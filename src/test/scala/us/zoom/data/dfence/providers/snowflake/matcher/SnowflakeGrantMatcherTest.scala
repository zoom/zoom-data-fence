package us.zoom.data.dfence.providers.snowflake.matcher

import munit.FunSuite
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.matchers.PlaybookGrantMatcher
import us.zoom.data.dfence.providers.snowflake.models.{
  PlaybookGrant,
  PlaybookGrantType,
  PlaybookPattern,
  SnowflakeGrant
}
import us.zoom.data.dfence.providers.snowflake.models.opaque.GrantPrivilege
import us.zoom.data.dfence.providers.snowflake.models.*

class SnowflakeGrantMatcherTest extends FunSuite:

  test("matchesPlaybookGrant should return true when grants match exactly") {
    // Given
    val snowGrant     = createSnowGrant(
      privilege = "SELECT",
      objectType = "TABLE",
      objectName = "DB.SCHEMA.TABLE"
    )
    val playbookGrant = createPlaybookGrant(
      privilege = "SELECT",
      objectType = SnowflakeObjectType.TABLE,
      db = Some("DB"),
      schema = Some("SCHEMA"),
      obj = Some("TABLE")
    )
    val matcher       = PlaybookGrantMatcher(snowGrant)

    // When
    val actualMatch = matcher.matchesPlaybookGrant(playbookGrant)

    // Then
    assert(actualMatch, "Grant should match when all fields are identical")
  }

  test("matchesPlaybookGrant should return false when privileges mismatch") {
    // Given
    val snowGrant     = createSnowGrant("UPDATE", "TABLE", "DB.SCHEMA.TABLE")
    val playbookGrant =
      createPlaybookGrant(
        "SELECT",
        SnowflakeObjectType.TABLE,
        Some("DB"),
        Some("SCHEMA"),
        Some("TABLE")
      )
    val matcher       = PlaybookGrantMatcher(snowGrant)

    // When
    val actualMatch = matcher.matchesPlaybookGrant(playbookGrant)

    // Then
    assert(!actualMatch, "Grant should not match when privileges differ")
  }

  test("matchesPlaybookGrant should return true for wildcard object name") {
    // Given
    val snowGrant     = createSnowGrant("SELECT", "TABLE", "DB.SCHEMA.ANY_TABLE")
    val playbookGrant = createPlaybookGrant(
      privilege = "SELECT",
      objectType = SnowflakeObjectType.TABLE,
      db = Some("DB"),
      schema = Some("SCHEMA"),
      obj = None // Wildcard
    )
    val matcher       = PlaybookGrantMatcher(snowGrant)

    // When
    val actualMatch = matcher.matchesPlaybookGrant(playbookGrant)

    // Then
    assert(actualMatch, "Grant should match when playbook has wildcard object name")
  }

  test("matchesPlaybookGrant should return true for aliased object type") {
    // Given: EXTERNAL_TABLE is an alias for TABLE in Snowflake logic
    val snowGrant     = createSnowGrant("SELECT", "EXTERNAL_TABLE", "DB.SCHEMA.EXT_TABLE")
    val playbookGrant =
      createPlaybookGrant(
        "SELECT",
        SnowflakeObjectType.TABLE,
        Some("DB"),
        Some("SCHEMA"),
        Some("EXT_TABLE")
      )
    val matcher       = PlaybookGrantMatcher(snowGrant)

    // When
    val actualMatch = matcher.matchesPlaybookGrant(playbookGrant)

    // Then
    assert(
      actualMatch,
      "Grant should match when object types are aliases (TABLE matching EXTERNAL_TABLE)"
    )
  }

  // Helper Functions
  private def createSnowGrant(
    privilege: String,
    objectType: String,
    objectName: String,
    future: Boolean = false,
    all: Boolean = false
  ): SnowflakeGrant =
    val model = SnowflakeGrantModel(
      privilege,
      objectType,
      objectName,
      "ROLE",
      "GRANTEE",
      false,
      future,
      all
    )
    SnowflakeGrant.from(model).getOrElse(fail(s"Unable to build snow grant from snowflake grant model $model"))

  private def createPlaybookGrant(
    privilege: String,
    objectType: SnowflakeObjectType,
    db: Option[String],
    schema: Option[String],
    obj: Option[String],
    grantType: PlaybookGrantType = PlaybookGrantType.Standard
  ): PlaybookGrant =
    PlaybookGrant(
      objectType,
      PlaybookPattern(db, schema, obj),
      List(GrantPrivilege(privilege)),
      grantType,
      enable = true
    )
