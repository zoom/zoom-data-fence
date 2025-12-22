package us.zoom.data.dfence.providers.snowflake.revoke.evaluator

import org.slf4j.LoggerFactory
import us.zoom.data.dfence.extensions.*
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.matchers.PlaybookGrantMatcher
import us.zoom.data.dfence.providers.snowflake.models.{
  PlaybookGrant,
  PrivilegeGrantIndex,
  SnowflakeGrant,
  SnowflakeGrantModel
}
import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, ObjectType, ObjectTypeAlias}

import scala.collection.parallel.ParSet
import scala.util.{Failure, Success, Try}

class GrantRevocationEvaluator(index: PrivilegeGrantIndex):

  private val log = LoggerFactory.getLogger(getClass)

  def needsRevoke(grant: SnowflakeGrantModel): Boolean =
    Try {
      val snowflakeGrant = SnowflakeGrant
        .from(grant)
        .getOrThrow(err =>
          throw IllegalArgumentException(
            s"Unable to create snow grant from snowflake grant model $grant",
            err
          )
        )

      val matcher = PlaybookGrantMatcher(snowflakeGrant)

      val matches = getCandidates(
        grantObjectType = snowflakeGrant.snowflakeObjectType,
        grantPrivilege = snowflakeGrant.privilege
      ).exists(matcher.matchesPlaybookGrant)

      !matches
    } match
      case Success(needsRevoke) =>
        if needsRevoke then log.info("Snowflake grant {} invalid, needs to be revoked", grant)
        else log.info("Snowflake grant {} is valid", grant)
        needsRevoke
      case Failure(err)         =>
        log.error("Error occurred while checking revocation of grants on model: {}", grant, err)
        false

  private def getCandidates(
    grantObjectType: SnowflakeObjectType,
    grantPrivilege: GrantPrivilege
  ): ParSet[PlaybookGrant] =
    getObjectTypeAndObjectTypeAliasMatchedGrants(
      grantObjectType
    ) & getPrivilegeMatchedGrants(grantPrivilege)

  private def getObjectTypeAndObjectTypeAliasMatchedGrants(
    grantObjectType: SnowflakeObjectType
  ): ParSet[PlaybookGrant] =
    val baseGrants = index
      .objectTypeIndex
      .getOrElse(
        ObjectType(grantObjectType),
        ParSet.empty
      )

    val specificGrants = index
      .objectAliasIndex
      .getOrElse(ObjectTypeAlias(grantObjectType), ParSet.empty)
      .filter(_.objectType == grantObjectType)

    baseGrants ++ specificGrants

  private def getPrivilegeMatchedGrants(
    grantPrivilege: GrantPrivilege
  ): ParSet[PlaybookGrant] =
    index.privilegeIndex.getOrElse(grantPrivilege, ParSet.empty)
