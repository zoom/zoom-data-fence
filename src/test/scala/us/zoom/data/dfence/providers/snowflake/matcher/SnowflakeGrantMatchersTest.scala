package us.zoom.data.dfence.providers.snowflake.matcher

import munit.FunSuite
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.matchers.SnowflakeGrantMatchers
import us.zoom.data.dfence.providers.snowflake.models.{
  PlaybookGrant,
  PlaybookGrantType,
  PlaybookPattern,
  SnowflakeGrantType
}
import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, SnowflakeGrantObjectName}
import us.zoom.data.dfence.providers.snowflake.models.*

class SnowflakeGrantMatchersTest extends FunSuite:

  // Mock PlaybookGrant for context
  def createPlaybookGrant(
    pattern: PlaybookPattern,
    objectType: SnowflakeObjectType = SnowflakeObjectType.TABLE,
    privileges: List[String] = List("SELECT"),
    grantType: PlaybookGrantType = PlaybookGrantType.Standard
  ): PlaybookGrant =
    PlaybookGrant(
      objectType,
      pattern,
      privileges.map(GrantPrivilege(_)),
      grantType,
      enable = true
    )

  test("GrantObjectName should match exact hierarchy DB.SCHEMA.OBJECT") {
    val pattern  = PlaybookPattern(Some("DB"), Some("SCHEMA"), Some("OBJ"))
    val matchers = SnowflakeGrantMatchers(createPlaybookGrant(pattern))

    val snowGrantName = SnowflakeGrantObjectName("DB.SCHEMA.OBJ")

    snowGrantName match
      case matchers.GrantObjectName() => // Success
      case _                          => fail("Should have matched exact hierarchy")
  }

  test("GrantObjectName should match DB.SCHEMA.*") {
    val pattern  =
      PlaybookPattern(Some("DB"), Some("SCHEMA"), None) // None implies Wildcard in matcher logic
    val matchers = SnowflakeGrantMatchers(createPlaybookGrant(pattern))

    val snowGrantName = SnowflakeGrantObjectName("DB.SCHEMA.ANY_OBJ")

    snowGrantName match
      case matchers.GrantObjectName() => // Success
      case _                          => fail("Should have matched wildcard object")
  }

  test("GrantObjectName should match DB.*.OBJ") {
    val pattern  = PlaybookPattern(Some("DB"), None, Some("OBJ"))
    val matchers = SnowflakeGrantMatchers(createPlaybookGrant(pattern))

    val snowGrantName = SnowflakeGrantObjectName("DB.ANY_SCHEMA.OBJ")

    snowGrantName match
      case matchers.GrantObjectName() => // Success
      case _                          => fail("Should have matched wildcard schema with specific object")
  }

  test("GrantObjectName should match DB.*.*") {
    val pattern  = PlaybookPattern(Some("DB"), None, None)
    val matchers = SnowflakeGrantMatchers(createPlaybookGrant(pattern))

    val snowGrantName = SnowflakeGrantObjectName("DB.ANY_SCHEMA.ANY_OBJ")

    snowGrantName match
      case matchers.GrantObjectName() => // Success
      case _                          => fail("Should have matched wildcard schema and object")
  }

  test("GrantObjectName should fail when DB does not match") {
    val pattern  = PlaybookPattern(Some("DB"), Some("SCHEMA"), Some("OBJ"))
    val matchers = SnowflakeGrantMatchers(createPlaybookGrant(pattern))

    val snowGrantName = SnowflakeGrantObjectName("OTHER_DB.SCHEMA.OBJ")

    snowGrantName match
      case matchers.GrantObjectName() => fail("Should not match different DB")
      case _                          => // Success
  }

  test("GrantObjectName should match case insensitive") {
    val pattern  = PlaybookPattern(Some("DB"), Some("SCHEMA"), Some("OBJ"))
    val matchers = SnowflakeGrantMatchers(createPlaybookGrant(pattern))

    val snowGrantName = SnowflakeGrantObjectName("db.schema.obj")

    snowGrantName match
      case matchers.GrantObjectName() => // Success
      case _                          => fail("Should match case insensitive")
  }

  test("GrantType should match Standard") {
    val pg       =
      createPlaybookGrant(PlaybookPattern(None, None, None), grantType = PlaybookGrantType.Standard)
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeGrantType.Standard match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match Standard grant type")
  }

  test("GrantType should match Future when playbook is Future") {
    val pg       =
      createPlaybookGrant(PlaybookPattern(None, None, None), grantType = PlaybookGrantType.Future)
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeGrantType.Future match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match Future grant type")
  }

  test("GrantType should match Standard when playbook is Future (as per logic)") {
    // Logic: case (SnowGrantType.Future | SnowGrantType.Standard, PlaybookGrantType.Future) => true
    val pg       =
      createPlaybookGrant(PlaybookPattern(None, None, None), grantType = PlaybookGrantType.Future)
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeGrantType.Standard match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match Standard grant type against Future playbook")
  }

  test("GrantType should match All when playbook is All") {
    val pg       =
      createPlaybookGrant(PlaybookPattern(None, None, None), grantType = PlaybookGrantType.All)
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeGrantType.All match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match All grant type")
  }

  test("GrantType should match Standard when playbook is All (as per logic)") {
    // Logic: case (SnowGrantType.All | SnowGrantType.Standard, PlaybookGrantType.All) => true
    val pg       =
      createPlaybookGrant(PlaybookPattern(None, None, None), grantType = PlaybookGrantType.All)
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeGrantType.Standard match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match Standard grant type against All playbook")
  }

  test("GrantType should match any type when playbook is FutureAndAll") {
    val pg       = createPlaybookGrant(
      PlaybookPattern(None, None, None),
      grantType = PlaybookGrantType.FutureAndAll
    )
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeGrantType.Standard match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match any grant type when playbook is FutureAndAll")

    SnowflakeGrantType.Future match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match Future grant type when playbook is FutureAndAll")

    SnowflakeGrantType.All match
      case matchers.GrantType() => // Success
      case _                    => fail("Should match All grant type when playbook is FutureAndAll")
  }

  test("GrantType should not match when types are incompatible") {
    val pg       =
      createPlaybookGrant(PlaybookPattern(None, None, None), grantType = PlaybookGrantType.Standard)
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeGrantType.Future match
      case matchers.GrantType() => fail("Should not match Future when playbook is Standard")
      case _                    => // Success
    SnowflakeGrantType.All match
      case matchers.GrantType() => fail("Should not match All when playbook is Standard")
      case _                    => // Success
  }

  test("GrantObjectType should not match when types are different and aliases don't match") {
    val pg       =
      createPlaybookGrant(PlaybookPattern(None, None, None), objectType = SnowflakeObjectType.TABLE)
    val matchers = SnowflakeGrantMatchers(pg)

    SnowflakeObjectType.VIEW match
      case matchers.GrantObjectType() => fail("Should not match different object types")
      case _                          => // Success
  }

  test("GrantObjectName should match empty string with empty pattern (account-level grants)") {
    // Given: Account-level grant with no database/schema/object
    // ObjectName.splitObjectName("") returns List.of("") (list with one empty string)
    val pattern  = PlaybookPattern(None, None, None)
    val matchers = SnowflakeGrantMatchers(createPlaybookGrant(pattern))

    val snowGrantName = SnowflakeGrantObjectName("") // Empty name for account-level grants

    // When & Then
    snowGrantName match
      case matchers.GrantObjectName() => // Success - should match List("") with empty pattern
      case _                          =>
        fail("Should match empty string (List(\"\")) with empty pattern for account-level grants")
  }
