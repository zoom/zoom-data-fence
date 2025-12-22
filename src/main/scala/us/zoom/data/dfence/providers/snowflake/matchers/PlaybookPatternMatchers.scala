package us.zoom.data.dfence.providers.snowflake.matchers

import org.slf4j.LoggerFactory
import us.zoom.data.dfence.providers.snowflake.models.PlaybookPattern
import us.zoom.data.dfence.sql.ObjectName

class PlaybookPatternMatchers(pattern: PlaybookPattern):

  private val log = LoggerFactory.getLogger(getClass)

  object AccountLevelGrant:
    def unapply(value: String): Boolean =
      val result = value.trim == ""
      if !result then
        log.info(
          "Playbook pattern {} match failed for account-level-grant value {}",
          pattern,
          value
        )
      result

  object AccountLevelObject:
    def unapply(snowObj: String): Boolean =
      val result = pattern match
        case PlaybookPattern(None, _, Some("*"))                                                          => false
        case PlaybookPattern(None, _, Some(objName)) if ObjectName.equalObjectName(snowObj.trim, objName) => true
        case _                                                                                            => false
      if !result then
        log.info(
          "Playbook pattern {} match failed for account-level-object value {}",
          pattern,
          snowObj
        )
      result

  object Database:
    def unapply(snowDb: String): Boolean =
      val result = pattern.dbName match
        case Some("*")                                                            => false
        case Some(dbName) if ObjectName.equalObjectName(snowDb.trim, dbName.trim) => true
        case _                                                                    => false
      if !result then
        log.info(
          "Playbook pattern {} match failed for database value {}",
          pattern,
          snowDb
        )
      result

  object Schema:
    def unapply(snowSchema: String): Boolean =
      val result = pattern.schName match
        case Some("*")                                                                        => true
        case Some(schemaName) if ObjectName.equalObjectName(snowSchema.trim, schemaName.trim) => true
        case None                                                                             => true
        case _                                                                                => false
      if !result then
        log.info(
          "Playbook pattern {} match failed for schema value {}",
          pattern,
          snowSchema
        )
      result

  object Object:
    def unapply(snowObj: String): Boolean =
      val result = pattern.objName match
        case Some(objName) if objName == "*"                                         => true
        case Some(objName) if ObjectName.equalObjectName(snowObj.trim, objName.trim) => true
        case None                                                                    => true
        case _                                                                       => false
      if !result then
        log.info(
          "Playbook pattern {} match failed for object value {}",
          pattern,
          snowObj
        )
      result
