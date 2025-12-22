package us.zoom.data.dfence.providers.snowflake.revoke

import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder
import us.zoom.data.dfence.providers.snowflake.revoke.evaluator.GrantRevocationEvaluator
import us.zoom.data.dfence.providers.snowflake.revoke.index.PlaybookGrantIndexBuilder

import java.util.{List as JList, Map as JMap}
import scala.collection.parallel.CollectionConverters.*
import scala.collection.parallel.mutable.ParSeq
import scala.jdk.CollectionConverters.*

class SnowflakeRevokeGrantsCompiler:

  def compileRevokeGrants(
    privilegeGrants: JList[PlaybookPrivilegeGrant],
    currentGrantBuilders: JMap[String, SnowflakeGrantBuilder]
  ): JList[SnowflakeGrantBuilder] =
    val playbookGrantsIndex =
      PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(privilegeGrants.asScala)
    val evaluator           = GrantRevocationEvaluator(playbookGrantsIndex)

    currentGrantBuilders
      .values()
      .asScala
      .par
      .to(ParSeq)
      .filter(grantBuilder => evaluator.needsRevoke(grantBuilder.getGrant))
      .toList
      .sortBy(builder => builder.getKey)
      .asJava
