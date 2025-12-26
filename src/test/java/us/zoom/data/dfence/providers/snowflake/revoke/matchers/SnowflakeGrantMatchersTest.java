package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.collection.NonEmptyList;
import us.zoom.data.dfence.providers.snowflake.revoke.models.*;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeGrantObjectName;

class SnowflakeGrantMatchersTest {

  // Mock PlaybookGrant for context
  private PlaybookGrant createPlaybookGrant(
      PlaybookPattern pattern,
      SnowflakeObjectType objectType,
      List<String> privileges,
      NonEmptyList<SnowflakeGrantType> grantTypes) {
    return new PlaybookGrant(
        objectType,
        pattern,
        NonEmptyList.from(
            ImmutableList.copyOf(privileges.stream().map(GrantPrivilege::new).toList())),
        grantTypes,
        true);
  }

  private PlaybookGrant createPlaybookGrant(PlaybookPattern pattern) {
    return createPlaybookGrant(
        pattern,
        SnowflakeObjectType.TABLE,
        List.of("SELECT"),
        NonEmptyList.of(SnowflakeGrantType.Standard));
  }

  @Test
  void grantObjectName_shouldMatch_whenThreePartNameMatchesExactPattern() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB.SCHEMA.OBJ");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Three-part object name should match when database, schema, and object all match");
  }

  @Test
  void grantObjectName_shouldMatch_whenObjectNameIsWildcardInPattern() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB.SCHEMA.ANY_OBJ");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should match any object when pattern has null object name (wildcard)");
  }

  @Test
  void grantObjectName_shouldMatch_whenSchemaIsWildcardButObjectMatches() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.empty(), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB.ANY_SCHEMA.OBJ");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should match when schema is wildcard (null) but object name matches");
  }

  @Test
  void grantObjectName_shouldMatch_whenSchemaAndObjectAreWildcards() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.empty(), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName =
        SnowflakeGrantObjectName.apply("DB.ANY_SCHEMA.ANY_OBJ");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should match any schema and object when both are wildcards (null) in pattern");
  }

  @Test
  void grantObjectName_shouldNotMatch_whenDatabaseNameDiffers() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("OTHER_DB.SCHEMA.OBJ");

    assertFalse(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should not match when database name differs even if schema and object match");
  }

  @Test
  void grantObjectName_shouldMatchCaseInsensitively() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("db.schema.obj");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Object name matching should be case-insensitive");
  }

  @Test
  void grantType_shouldMatchStandard() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Standard).apply(pg),
        "Should match Standard grant type");
  }

  @Test
  void grantType_shouldMatchFuture_whenPlaybookIsFuture() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Future));

    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Future).apply(pg),
        "Should match Future grant type");
  }

  @Test
  void grantType_shouldMatchStandardGrant_whenPlaybookIsFutureType() {
    // Future playbook grants accept both Future and Standard grants
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Future));

    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Standard).apply(pg),
        "Future playbook should accept Standard grants (Standard grants are compatible with Future)");
  }

  @Test
  void grantType_shouldMatchAll_whenPlaybookIsAll() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.All));

    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.All).apply(pg),
        "Should match All grant type");
  }

  @Test
  void grantType_shouldMatchStandardGrant_whenPlaybookIsAllType() {
    // All playbook grants accept both All and Standard grants
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.All));

    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Standard).apply(pg),
        "All playbook should accept Standard grants (Standard grants are compatible with All)");
  }

  @Test
  void grantType_shouldMatchAnyType_whenPlaybookIsFutureAndAll() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Future, SnowflakeGrantType.All));

    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Standard).apply(pg),
        "Should match any grant type when playbook contains both Future and All");
    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Future).apply(pg),
        "Should match Future grant type when playbook contains both Future and All");
    assertTrue(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.All).apply(pg),
        "Should match All grant type when playbook contains both Future and All");
  }

  @Test
  void grantType_shouldNotMatch_whenTypesAreIncompatible() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    assertFalse(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Future).apply(pg),
        "Should not match Future when playbook is Standard");
    assertFalse(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.All).apply(pg),
        "Should not match All when playbook is Standard");
  }

  @Test
  void grantObjectType_shouldNotMatch_whenTypesAreDifferentAndAliasesDontMatch() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    assertFalse(
        SnowflakeGrantMatchers.grantObjectType(SnowflakeObjectType.VIEW).apply(pg),
        "Should not match different object types");
  }

  @Test
  void grantObjectName_shouldMatchEmptyString_forAccountLevelGrants() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Empty object name should match empty pattern for account-level grants");
  }

  @Test
  void grantObjectType_shouldMatch_whenTypesAreEqual() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    assertTrue(
        SnowflakeGrantMatchers.grantObjectType(SnowflakeObjectType.TABLE).apply(pg),
        "Should match when object types are equal");
  }

  @Test
  void grantObjectType_shouldMatch_whenObjectTypesShareAlias() {
    // Critical: EXTERNAL_TABLE and TABLE share the same alias, so they should match
    // This tests the second branch of OR: when direct equality is false but alias equality is true
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    // EXTERNAL_TABLE != TABLE (first OR branch is false)
    // But EXTERNAL_TABLE.getAliasFor() == TABLE.getAliasFor() == "TABLE" (second OR branch is true)
    SnowflakeObjectType externalTable = SnowflakeObjectType.EXTERNAL_TABLE;
    assertTrue(
        SnowflakeGrantMatchers.grantObjectType(externalTable).apply(pg),
        "Should match when object types share the same alias (e.g., TABLE and EXTERNAL_TABLE)");
  }

  @Test
  void grantObjectType_shouldMatch_whenDirectEqualityIsTrue() {
    // Critical: Test the first branch of OR - when direct equality is true (short-circuits)
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    // Direct equality: TABLE == TABLE (first OR branch is true, short-circuits)
    assertTrue(
        SnowflakeGrantMatchers.grantObjectType(SnowflakeObjectType.TABLE).apply(pg),
        "Should match when object types are directly equal (first OR branch)");
  }

  @Test
  void grantObjectType_shouldMatch_whenPlaybookIsCortexAgentAndGrantIsCortexAgent() {
    // Critical: CORTEX_AGENT should match CORTEX_AGENT
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.CORTEX_AGENT,
            List.of("USAGE"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    assertTrue(
        SnowflakeGrantMatchers.grantObjectType(SnowflakeObjectType.CORTEX_AGENT).apply(pg),
        "Should match when both playbook and grant are CORTEX_AGENT");
  }

  @Test
  void grantObjectType_shouldMatch_whenPlaybookIsCortexAgentAndGrantIsAgent() {
    // Critical: AGENT string maps to CORTEX_AGENT enum, so they should match via alias
    // Note: This test verifies the alias matching logic works correctly
    // Since CORTEX_AGENT has aliasFor=null, getAliasFor() returns "CORTEX_AGENT"
    // And AGENT (when converted) becomes CORTEX_AGENT enum, so they match directly
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.CORTEX_AGENT,
            List.of("USAGE"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    // When AGENT string is converted via fromString(), it becomes CORTEX_AGENT enum
    // So this test verifies that CORTEX_AGENT matches CORTEX_AGENT
    assertTrue(
        SnowflakeGrantMatchers.grantObjectType(SnowflakeObjectType.CORTEX_AGENT).apply(pg),
        "Should match when playbook is CORTEX_AGENT and grant is CORTEX_AGENT (AGENT string maps to CORTEX_AGENT enum)");
  }

  @Test
  void grantPrivilege_shouldReturnTrue_whenPrivilegeMatches() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT", "UPDATE"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    assertTrue(
        SnowflakeGrantMatchers.grantPrivilege(new GrantPrivilege("SELECT")).apply(pg),
        "Should match when privilege is in list");
    assertTrue(
        SnowflakeGrantMatchers.grantPrivilege(new GrantPrivilege("UPDATE")).apply(pg),
        "Should match when privilege is in list");
  }

  @Test
  void grantPrivilege_shouldReturnFalse_whenPrivilegeDoesNotMatch() {
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Standard));

    assertFalse(
        SnowflakeGrantMatchers.grantPrivilege(new GrantPrivilege("UPDATE")).apply(pg),
        "Should not match when privilege is not in list");
  }

  @Test
  void grantObjectName_shouldMatch_whenSizeIsOneAndMatchesAccountLevel() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.of("ACCOUNT_OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("ACCOUNT_OBJ");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should match account level object");
  }

  @Test
  void grantObjectName_shouldMatch_whenSizeIsOneAndMatchesDatabase() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.empty(), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should match database level");
  }

  @Test
  void grantObjectName_shouldMatch_whenSizeIsOneAndAccountLevelGrantMatches() {
    // Critical: Test the second branch of OR chain - when accountLevelObject returns false but
    // accountLevelGrant returns true
    // Pattern: empty/empty/empty, part: "" (empty string)
    // accountLevelObject("") will return false (because objName is empty)
    // accountLevelGrant("") will return true (because value.trim().isEmpty())
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should match when accountLevelObject is false but accountLevelGrant is true (second OR branch)");
  }

  @Test
  void grantObjectName_shouldMatch_whenSizeIsOneAndOnlyDatabaseMatches() {
    // Critical: Test the third branch of OR chain - when both accountLevelObject and
    // accountLevelGrant return false, but database returns true
    // Pattern: db/empty/empty, part: "DB"
    // accountLevelObject("DB") will return false (because dbName is present)
    // accountLevelGrant("DB") will return false (because "DB".trim().isEmpty() is false)
    // database("DB") will return true
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.empty(), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB");

    assertTrue(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should match when accountLevelObject and accountLevelGrant are false but database is true (third OR branch)");
  }

  @Test
  void grantObjectName_shouldReturnFalse_whenSizeIsTwoButDatabaseDoesNotMatch() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("OTHER_DB.SCHEMA");

    assertFalse(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should not match when database doesn't match");
  }

  @Test
  void grantObjectName_shouldReturnFalse_whenSizeIsTwoButSchemaDoesNotMatch() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.empty());
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB.OTHER_SCHEMA");

    assertFalse(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should not match when schema doesn't match");
  }

  @Test
  void grantObjectName_shouldReturnFalse_whenSizeIsThreeButObjectDoesNotMatch() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB.SCHEMA.OTHER_OBJ");

    assertFalse(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should not match when object doesn't match");
  }

  @Test
  void grantObjectName_shouldNotMatch_whenSinglePartNameDoesNotMatchAnyPattern() {
    // Critical edge case: single-part name that doesn't match account-level object, account-level
    // grant, or database
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("UNMATCHED");

    assertFalse(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Single-part name should not match when it doesn't match account-level object, grant, or database pattern");
  }

  @Test
  void grantType_shouldNotMatch_whenFuturePlaybookReceivesAllGrant() {
    // Critical: Future playbook should NOT accept All grants
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.Future));

    assertFalse(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.All).apply(pg),
        "Future playbook should not accept All grants - only Future and Standard");
  }

  @Test
  void grantType_shouldNotMatch_whenAllPlaybookReceivesFutureGrant() {
    // Critical: All playbook should NOT accept Future grants
    PlaybookGrant pg =
        createPlaybookGrant(
            new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty()),
            SnowflakeObjectType.TABLE,
            List.of("SELECT"),
            NonEmptyList.of(SnowflakeGrantType.All));

    assertFalse(
        SnowflakeGrantMatchers.grantType(SnowflakeGrantType.Future).apply(pg),
        "All playbook should not accept Future grants - only All and Standard");
  }

  @Test
  void grantObjectName_shouldHandleException_whenObjectNameIsInvalid() {
    // Critical: Exception handling - when ObjectName.splitObjectName throws exception
    // This tests the exception path that can occur in grantObjectName
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    // Create an object name that will cause splitObjectName to throw
    // ObjectName.splitObjectName throws for names with more than 3 parts
    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB.SCHEMA.OBJ.SUBOBJ");

    // The exception will propagate, which is expected behavior
    assertThrows(
        us.zoom.data.dfence.exception.ObjectNameException.class,
        () -> SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should throw ObjectNameException for invalid object name format");
  }

  @Test
  void grantObjectName_shouldReturnFalse_whenSizeIsThreeButSchemaDoesNotMatch() {
    // Critical: Test the second AND branch in case 3 - when database matches but schema doesn't
    // This covers the missing branch: matchers.schema() returns false
    // Pattern: DB/SCHEMA/OBJ, Grant: DB/OTHER_SCHEMA/OBJ
    // database("DB") returns true (first AND branch is true)
    // schema("OTHER_SCHEMA") returns false (second AND branch is false, short-circuits)
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("DB.OTHER_SCHEMA.OBJ");

    assertFalse(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should return false when database matches but schema doesn't (second AND branch is false)");
  }

  @Test
  void grantObjectName_shouldReturnFalse_whenSizeIsThreeButDatabaseDoesNotMatch() {
    // Critical: Test the first AND branch in case 3 - when database doesn't match
    // This ensures we also test when the first AND branch is false (short-circuits)
    // Pattern: DB/SCHEMA/OBJ, Grant: OTHER_DB/SCHEMA/OBJ
    // database("OTHER_DB") returns false (first AND branch is false, short-circuits)
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("OBJ"));
    PlaybookGrant playbookGrant = createPlaybookGrant(pattern);

    SnowflakeGrantObjectName snowGrantName = SnowflakeGrantObjectName.apply("OTHER_DB.SCHEMA.OBJ");

    assertFalse(
        SnowflakeGrantMatchers.grantObjectName(snowGrantName).apply(playbookGrant),
        "Should return false when database doesn't match (first AND branch is false, short-circuits)");
  }
}
