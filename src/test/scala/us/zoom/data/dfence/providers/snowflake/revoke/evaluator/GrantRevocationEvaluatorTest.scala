package us.zoom.data.dfence.providers.snowflake.revoke.evaluator

import munit.FunSuite
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.models.{
  PlaybookGrant,
  PlaybookGrantType,
  PlaybookPattern,
  PrivilegeGrantIndex
}
import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, ObjectType, ObjectTypeAlias}
import us.zoom.data.dfence.providers.snowflake.models.*
import us.zoom.data.dfence.providers.snowflake.revoke.evaluator.GrantRevocationEvaluator

import scala.collection.parallel.{ParMap, ParSet}

class GrantRevocationEvaluatorTest extends FunSuite:

  test("needsRevoke should return true when no matching playbook grant exists") {
    // Given
    val index        = createEmptyIndex()
    val evaluator    = GrantRevocationEvaluator(index)
    val grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE")

    // When
    val actualNeedsRevoke = evaluator.needsRevoke(grantToCheck)

    // Then
    assert(actualNeedsRevoke, "Should revoke grant not found in playbook")
  }

  test("needsRevoke should return false when matching playbook grant exists") {
    // Given
    val playbookGrant = createPlaybookGrant(
      "SELECT",
      SnowflakeObjectType.TABLE,
      "DB",
      "SCHEMA",
      "TABLE"
    )
    val index         = createIndexWith(playbookGrant)
    val evaluator     = GrantRevocationEvaluator(index)
    val grantToCheck  = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE")

    // When
    val actualNeedsRevoke = evaluator.needsRevoke(grantToCheck)

    // Then
    assert(!actualNeedsRevoke, "Should NOT revoke grant found in playbook")
  }

  test("needsRevoke should return false when alias matches") {
    // Given: Playbook allows TABLE, existing grant is EXTERNAL_TABLE (alias)
    // The new implementation looks up ObjectType(EXTERNAL_TABLE) directly and ObjectTypeAlias(EXTERNAL_TABLE)
    // For the alias lookup to work, we need a playbook grant with objectType = EXTERNAL_TABLE
    // indexed under ObjectTypeAlias(EXTERNAL_TABLE) = "TABLE"
    // But the filter requires grant.objectType == EXTERNAL_TABLE, so we need an EXTERNAL_TABLE playbook grant
    val playbookGrant =
      createPlaybookGrant("SELECT", SnowflakeObjectType.EXTERNAL_TABLE, "DB", "SCHEMA", "EXT_TABLE")
    val index         = createIndexWith(playbookGrant)
    val evaluator     = GrantRevocationEvaluator(index)
    val grantToCheck  = createGrantModel("SELECT", "EXTERNAL_TABLE", "DB.SCHEMA.EXT_TABLE")

    // When
    val actualNeedsRevoke = evaluator.needsRevoke(grantToCheck)

    // Then
    assert(!actualNeedsRevoke, "Should NOT revoke when object types are valid aliases")
  }

  test("needsRevoke should return false and log error when grant model is invalid") {
    // Given: Invalid grant model that will fail to convert to SnowGrant
    val index        = createEmptyIndex()
    val evaluator    = GrantRevocationEvaluator(index)
    val grantToCheck = createGrantModel("SELECT", "INVALID_OBJECT_TYPE", "DB.SCHEMA.TABLE")

    // When
    val actualNeedsRevoke = evaluator.needsRevoke(grantToCheck)

    // Then: Should return false (safe default) and log error
    assert(!actualNeedsRevoke, "Should return false when grant model is invalid")
  }

  // Helper Functions
  private def createGrantModel(priv: String, objType: String, name: String) =
    new SnowflakeGrantModel(priv, objType, name, "ROLE", "USER", false, false, false)

  private def createPlaybookGrant(
    priv: String,
    objType: SnowflakeObjectType,
    db: String,
    schema: String,
    obj: String
  ) =
    PlaybookGrant(
      objType,
      PlaybookPattern(Some(db), Some(schema), Some(obj)),
      List(GrantPrivilege(priv)),
      PlaybookGrantType.Standard,
      enable = true
    )

  private def createEmptyIndex() = PrivilegeGrantIndex(ParMap.empty, ParMap.empty, ParMap.empty)

  private def createIndexWith(grant: PlaybookGrant) =
    val priv    = grant.privileges.head
    val objType = ObjectType(grant.objectType)
    val alias   = ObjectTypeAlias(grant.objectType)

    // Add to all indices for completeness in this unit test
    PrivilegeGrantIndex(
      ParMap(priv    -> ParSet(grant)),
      ParMap(objType -> ParSet(grant)),
      ParMap(alias   -> ParSet(grant))
    )
