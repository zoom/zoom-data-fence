package us.zoom.data.dfence.providers.snowflake.revoke.index

import org.slf4j.LoggerFactory
import us.zoom.data.dfence.extensions.mergeHashMaps
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.models.{
  PlaybookGrant,
  PlaybookGrantType,
  PlaybookPattern,
  PrivilegeGrantIndex
}
import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, ObjectType, ObjectTypeAlias}

import scala.collection.mutable
import scala.collection.parallel.CollectionConverters.*
import scala.collection.parallel.mutable.ParSeq
import scala.collection.parallel.{ParMap, ParSet}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

object PlaybookGrantIndexBuilder:

  private val log = LoggerFactory.getLogger(getClass)

  def buildPrivilegeGrantIndex(
    playbookGrants: mutable.Buffer[PlaybookPrivilegeGrant]
  ): PrivilegeGrantIndex =
    val enabledGrants  = playbookGrants.par.flatMap(toPlaybookGrant).to(ParSeq).filter(_.enable)
    val privilegeIndex = buildPrivilegeIndex(enabledGrants)
    val (objectTypeIndex, objectTypeAliasIndex) =
      buildObjectTypeAndObjectTypeAliasIndex(enabledGrants)
    PrivilegeGrantIndex(privilegeIndex, objectTypeIndex, objectTypeAliasIndex)

  private def buildPrivilegeIndex(
    privilegeGrants: ParSeq[PlaybookGrant]
  ): ParMap[GrantPrivilege, ParSet[PlaybookGrant]] =
    privilegeGrants.flatMap { grant =>
      grant.privileges.map(privilege => privilege -> grant)
    }.aggregate(mutable.HashMap[GrantPrivilege, mutable.HashSet[PlaybookGrant]]())(
      (map, entry) =>
        map.getOrElseUpdate(entry._1, mutable.HashSet()).add(entry._2)
        map
      ,
      (a, b) => a.mergeHashMaps(b)
    ).par
      .map { case (k, v) => k -> v.to(ParSet) }
      .to(ParMap)

  private def buildObjectTypeAndObjectTypeAliasIndex(
    privilegeGrants: ParSeq[PlaybookGrant]
  ): (
    ParMap[ObjectType, ParSet[PlaybookGrant]],
    ParMap[ObjectTypeAlias, ParSet[PlaybookGrant]]
  ) =
    val (primaryObjectTypeGrants, aliasObjectTypeGrants) =
      privilegeGrants.partition(grant => grant.objectType.getObjectType() == grant.objectType.getAliasFor())

    val primaryIndex =
      primaryObjectTypeGrants
        .aggregate(mutable.HashMap[ObjectType, mutable.HashSet[PlaybookGrant]]())(
          (map, grant) =>
            val key = ObjectType(grant.objectType)
            map.getOrElseUpdate(key, mutable.HashSet()).add(grant)
            map
          ,
          (a, b) => a.mergeHashMaps(b)
        )
        .par
        .map { case (k, v) => k -> v.to(ParSet) }
        .to(ParMap)

    val aliasIndex =
      aliasObjectTypeGrants
        .aggregate(mutable.HashMap[ObjectTypeAlias, mutable.HashSet[PlaybookGrant]]())(
          (map, grant) =>
            val key = ObjectTypeAlias(grant.objectType)
            map.getOrElseUpdate(key, mutable.HashSet()).add(grant)
            map
          ,
          (a, b) => a.mergeHashMaps(b)
        )
        .par
        .map { case (k, v) => k -> v.to(ParSet) }
        .to(ParMap)

    (primaryIndex, aliasIndex)

  private def toPlaybookGrant(grant: PlaybookPrivilegeGrant): Option[PlaybookGrant] =
    Try {
      PlaybookGrant(
        objectType = SnowflakeObjectType.valueOf(grant.objectType()),
        pattern = PlaybookPattern(
          dbName = Option(grant.databaseName()).map(_.trim).filter(_.nonEmpty),
          schName = Option(grant.schemaName()).map(_.trim).filter(_.nonEmpty),
          objName = Option(grant.objectName()).map(_.trim).filter(_.nonEmpty)
        ),
        privileges =
          grant.privileges().asScala.toList.map(privilege => GrantPrivilege(privilege.toUpperCase)),
        grantType = (grant.includeFuture, grant.includeAll) match
          case (true, true)  => PlaybookGrantType.FutureAndAll
          case (true, false) => PlaybookGrantType.Future
          case (false, true) => PlaybookGrantType.All
          case (_, _)        => PlaybookGrantType.Standard,
        enable = grant.enable()
      )
    } match
      case Success(value) => Some(value)
      case Failure(err)   =>
        log.error("Conversion to playbook grant failed for playbook privilege grant {}", grant, err)
        None
