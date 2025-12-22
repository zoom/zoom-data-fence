package us.zoom.data.dfence.providers.snowflake.matchers

import org.slf4j.LoggerFactory
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.models.{
  PlaybookGrant,
  PlaybookGrantType,
  PlaybookPattern,
  SnowflakeGrantType
}
import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, SnowflakeGrantObjectName}
import us.zoom.data.dfence.sql.ObjectName

import scala.jdk.CollectionConverters.*

class SnowflakeGrantMatchers(playbookGrant: PlaybookGrant):

  private val log = LoggerFactory.getLogger(getClass)

  object GrantObjectType:
    def unapply(snowflakeGrantObjectType: SnowflakeObjectType): Boolean =
      val result = (snowflakeGrantObjectType, playbookGrant.objectType) match
        // Example: GRANT SELECT ON TABLE sales_db.analytics.customers matches playbook with objectType=TABLE => true
        case (sgot, pgot) if sgot == pgot                         => true
        // Example: GRANT SELECT ON EXTERNAL_TABLE sales_db.analytics.external_data matches playbook with objectType=TABLE => true
        // (EXTERNAL_TABLE is an alias for TABLE)
        case (sgot, pgot) if sgot.getAliasFor == pgot.getAliasFor => true
        // Example: GRANT SELECT ON VIEW sales_db.analytics.report matches playbook with objectType=TABLE => false
        case (_, _)                                               => false
      if !result then
        log.info(
          "Playbook grant {} match failed for snowflake grant grant-object-type {}",
          playbookGrant,
          snowflakeGrantObjectType
        )
      result

  object GrantPrivilege:
    def unapply(privilege: GrantPrivilege): Boolean =
      val result = playbookGrant.privileges.contains(privilege)
      if !result then
        log.info(
          "Playbook grant {} match failed for snowflake grant grant-privilege {}",
          playbookGrant,
          privilege
        )
      result

//  def foo(): Unit =
//    case class Employee(name: String, age: Option[Int])
//    Employee("foo", Some(30)) match
//      case Employee(name, Some(age)) if age >= 18 => true
//      case Employee(name, None)                   => true

  object GrantType:
    def unapply(snowflakeGrantType: SnowflakeGrantType): Boolean =
      val result = (snowflakeGrantType, playbookGrant.grantType) match
        // Example: Any grant type (Standard/Future/All) matches playbook with FutureAndAll => true
        // GRANT SELECT ON TABLE sales_db.analytics.customers matches playbook with FutureAndAll => true
        // GRANT SELECT ON FUTURE TABLES IN SCHEMA sales_db.analytics matches playbook with FutureAndAll => true
        case (
              _,
              PlaybookGrantType.FutureAndAll
            ) => true
        // Example: GRANT SELECT ON FUTURE TABLES IN SCHEMA sales_db.analytics matches playbook with Future => true
        // Example: GRANT SELECT ON TABLE sales_db.analytics.customers matches playbook with Future => true
        // (Standard grants are valid when playbook specifies Future grants)
        case (SnowflakeGrantType.Future | SnowflakeGrantType.Standard, PlaybookGrantType.Future) => true
        // Example: GRANT SELECT ON ALL TABLES IN SCHEMA sales_db.analytics matches playbook with All => true
        // Example: GRANT SELECT ON TABLE sales_db.analytics.customers matches playbook with All => true
        // (Standard grants are valid when playbook specifies All grants)
        case (SnowflakeGrantType.All | SnowflakeGrantType.Standard, PlaybookGrantType.All)       => true
        // Example: GRANT SELECT ON TABLE sales_db.analytics.customers matches playbook with Standard => true
        case (SnowflakeGrantType.Standard, PlaybookGrantType.Standard)                           => true
        // Example: GRANT SELECT ON FUTURE TABLES IN SCHEMA sales_db.analytics matches playbook with Standard => false
        // Example: GRANT SELECT ON ALL TABLES IN SCHEMA sales_db.analytics matches playbook with Standard => false
        case (_, _)                                                                              => false
      if !result then
        log.info(
          "Playbook grant {} match failed for snowflake grant grant-type {}",
          playbookGrant,
          snowflakeGrantType
        )
      result

  object GrantObjectName:
    def unapply(snowflakeGrantObjectName: SnowflakeGrantObjectName): Boolean =
      val grantObjectNameParts = ObjectName.splitObjectName(snowflakeGrantObjectName.toStr).asScala.toList
      val matchers             = PlaybookPatternMatchers(playbookGrant.pattern)
      import matchers.*

      val result = grantObjectNameParts match
        case List(AccountLevelObject() | AccountLevelGrant()) => true
        case List(Database())                                 => true
        case List(Database(), Schema())                       => true
        case List(Database(), Schema(), Object())             => true
        case _                                                => false

      if !result then
        log.info(
          "Playbook grant {} match failed for snowflake grant grant-object-name {}",
          playbookGrant,
          snowflakeGrantObjectName
        )
      result
