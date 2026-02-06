package us.zoom.data.dfence.providers.snowflake.revoke;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

class SnowflakeRevokeGrantsCompilerTest {

  @Test
  void compileRevokeGrants_whenGrantMatchesPlaybook_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenGrantNotInPlaybook_shouldRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant("UPDATE", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    List<String> privileges = actualRevokes.stream().map(b -> b.getGrant().privilege()).toList();
    assertEquals(List.of("UPDATE"), privileges);
  }

  @Test
  void compileRevokeGrants_whenWildcardMatches_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "*", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.ANY_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenAliasMatches_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "EXTERNAL_TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant(
            "SELECT", "EXTERNAL_TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenFutureGrantCompatible_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "TABLE", "TEST_DB", "TEST_SCHEMA", "*", List.of("SELECT"), true, true, true);
    SnowflakeGrantModel currentGrant =
        createFutureGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.<TABLE>", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);
    System.out.println(actualRevokes);
    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenFutureGrantNotCompatible_shouldRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "TABLE", "TEST_DB", "TEST_SCHEMA", "*", List.of("SELECT"), false, true, true);
    SnowflakeGrantModel currentGrant =
        createFutureGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("SELECT", revokedGrant.privilege());
    assertEquals("TABLE", revokedGrant.grantedOn());
    assertEquals("TEST_DB.TEST_SCHEMA", revokedGrant.name());
    assertEquals("ROLE", revokedGrant.grantedTo());
    assertEquals("TEST_ROLE", revokedGrant.granteeName());
    assertTrue(revokedGrant.future());
    assertFalse(revokedGrant.all());
  }

  @Test
  void compileRevokeGrants_whenAllGrantCompatible_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "TABLE", "TEST_DB", "TEST_SCHEMA", "*", List.of("SELECT"), true, true, true);
    SnowflakeGrantModel currentGrant =
        createAllGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When

    assertThrows(
        Exception.class,
        () ->
            SnowflakeRevokeGrantsCompiler.compileRevokeGrants(
                List.of(playbookGrant), currentGrants));
  }

  @Test
  void compileRevokeGrants_whenRoleGrantMatches_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrantForRole("TEST_ROLE", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "ROLE", "TEST_ROLE", "ROLE", "ANOTHER_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenDatabaseGrantMatches_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("DATABASE", "TEST_DB", null, null, List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "DATABASE", "TEST_DB", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenSchemaGrantMatches_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("SCHEMA", "TEST_DB", "TEST_SCHEMA", null, List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "SCHEMA", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenEmptyPlaybook_shouldRevokeAllGrants() {
    // Given
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(), currentGrants);

    // Then
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("SELECT", revokedGrant.privilege());
    assertEquals("TABLE", revokedGrant.grantedOn());
    assertEquals("TEST_DB.TEST_SCHEMA.TEST_TABLE", revokedGrant.name());
    assertEquals("ROLE", revokedGrant.grantedTo());
    assertEquals("TEST_ROLE", revokedGrant.granteeName());
  }

  @Test
  void compileRevokeGrants_whenEmptyCurrentGrants_shouldReturnEmpty() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List.of("SELECT"));
    Map<String, SnowflakeGrantBuilder> currentGrants = new HashMap<>();

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenDisabledPlaybookGrant_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List.of("SELECT"), true, true, false);
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertEquals(0, actualRevokes.size());
  }

  @Test
  void compileRevokeGrants_whenMultipleGrants_shouldHaveMixedResults() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "*", List.of("SELECT"));
    SnowflakeGrantModel allowedGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TABLE1", "ROLE", "TEST_ROLE");
    SnowflakeGrantModel revokedGrant1 =
        createGrant("UPDATE", "TABLE", "TEST_DB.TEST_SCHEMA.TABLE2", "ROLE", "TEST_ROLE");
    SnowflakeGrantModel revokedGrant2 =
        createGrant("DELETE", "TABLE", "TEST_DB.TEST_SCHEMA.TABLE3", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants =
        createCurrentGrants(allowedGrant, revokedGrant1, revokedGrant2);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    List<String> privileges =
        actualRevokes.stream().map(b -> b.getGrant().privilege()).sorted().toList();
    assertTrue(privileges.contains("UPDATE"));
    assertTrue(privileges.contains("DELETE"));
    assertEquals(2, privileges.size());
  }

  @Test
  void compileRevokeGrants_whenSchemaWildcardMatches_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("SCHEMA", "TEST_DB", "*", null, List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "SCHEMA", "TEST_DB.ANY_SCHEMA", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenDifferentObjectType_shouldRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "VIEW", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("SELECT", revokedGrant.privilege());
    assertEquals("VIEW", revokedGrant.grantedOn());
    assertEquals("TEST_DB.TEST_SCHEMA.TEST_TABLE", revokedGrant.name());
    assertEquals("ROLE", revokedGrant.grantedTo());
    assertEquals("TEST_ROLE", revokedGrant.granteeName());
  }

  @Test
  void compileRevokeGrants_whenEmptyPrivilegesList_shouldNotIndexPrivilege() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "TABLE", "TEST_TABLE", "TEST_SCHEMA", "TEST_DB", List.of(), true, true, true);
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("SELECT", revokedGrant.privilege());
    assertEquals("TABLE", revokedGrant.grantedOn());
    assertEquals("TEST_DB.TEST_SCHEMA.TEST_TABLE", revokedGrant.name());
    assertEquals("ROLE", revokedGrant.grantedTo());
    assertEquals("TEST_ROLE", revokedGrant.granteeName());
  }

  @Test
  void compileRevokeGrants_whenInvalidObjectTypeInPlaybook_shouldThrow() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "INVALID_TYPE",
            "TEST_TABLE",
            "TEST_SCHEMA",
            "TEST_DB",
            List.of("SELECT"),
            true,
            true,
            true);
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When/Then: Invalid object types in playbook grants throw RbacDataError
    assertThrows(
        Exception.class,
        () ->
            SnowflakeRevokeGrantsCompiler.compileRevokeGrants(
                List.of(playbookGrant), currentGrants));
  }

  @Test
  void compileRevokeGrants_whenDatabaseAndSchemaWildcards_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "TEST_DB.ANY_SCHEMA.ANY_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenDatabaseAndSchemaWildcardsDifferentSchema_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant(
            "SELECT", "TABLE", "TEST_DB.DIFFERENT_SCHEMA.DIFFERENT_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenDatabaseAndSchemaWildcardsWrongDatabase_shouldRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant("SELECT", "TABLE", "OTHER_DB.ANY_SCHEMA.ANY_TABLE", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("SELECT", revokedGrant.privilege());
    assertEquals("TABLE", revokedGrant.grantedOn());
    assertEquals("OTHER_DB.ANY_SCHEMA.ANY_TABLE", revokedGrant.name());
    assertEquals("ROLE", revokedGrant.grantedTo());
    assertEquals("TEST_ROLE", revokedGrant.granteeName());
  }

  @Test
  void
      compileRevokeGrants_whenDatabaseLevelPlaybookMatchesDatabaseLevelFutureGrant_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", null, "*", List.of("SELECT"), true, true, true);
    SnowflakeGrantModel futureGrant =
        createFutureGrant("SELECT", "TABLE", "TEST_DB.<SCHEMA>", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(futureGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenDatabaseLevelPlaybookDoesNotMatchWrongDatabase_shouldRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", null, "*", List.of("SELECT"), true, true, true);
    SnowflakeGrantModel futureGrant =
        createFutureGrant("SELECT", "TABLE", "OTHER_DB.SCHEMA.<TABLE>", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(futureGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("SELECT", revokedGrant.privilege());
    assertEquals("TABLE", revokedGrant.grantedOn());
    assertEquals("OTHER_DB.SCHEMA.<TABLE>", revokedGrant.name());
    assertTrue(revokedGrant.future());
  }

  @Test
  void
      compileRevokeGrants_whenPlaybookWithWildcardSchemaMatchesDatabaseLevelFutureGrant_shouldNotRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List.of("SELECT"), true, true, true);
    SnowflakeGrantModel futureGrant =
        createFutureGrant("SELECT", "TABLE", "TEST_DB.X.<TABLE>", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(futureGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    assertTrue(actualRevokes.isEmpty());
  }

  @Test
  void compileRevokeGrants_whenDatabaseFutureGrantMatchesPlaybook_shouldRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "SCHEMA", "TEST_DB", "TEST_SCHEMA", null, List.of("USAGE"), true, true, true);
    SnowflakeGrantModel futureGrant =
        createFutureGrant("USAGE", "SCHEMA", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(futureGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    // When both includeFuture and includeAll are true, the grant type determination depends on
    // whether the playbook pattern is a valid standard target. In this test, the pattern
    // (TEST_DB, TEST_SCHEMA) is a valid standard target for SCHEMA (qualLevel 2 requires both
    // database and schema), so the grant type is STANDARD (not FUTURE_AND_ALL).
    // STANDARD grants only match STANDARD grants, so the FUTURE grant should be revoked
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("USAGE", revokedGrant.privilege());
    assertEquals("SCHEMA", revokedGrant.grantedOn());
    assertEquals("TEST_DB.TEST_SCHEMA", revokedGrant.name());
    assertTrue(revokedGrant.future());
  }

  @Test
  void compileRevokeGrants_whenDatabaseFutureGrantDoesNotMatchPlaybook_shouldRevoke() {
    // Given
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "SCHEMA", "TEST_DB", "TEST_SCHEMA", null, List.of("USAGE"), true, true, true);
    SnowflakeGrantModel futureGrant =
        createFutureGrant("USAGE", "SCHEMA", "OTHER_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(futureGrant);

    // When
    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    // Then
    // When both includeFuture and includeAll are true, the grant type determination depends on
    // whether the playbook pattern is a valid standard target. In this test, the pattern
    // (TEST_DB, TEST_SCHEMA) is a valid standard target for SCHEMA (qualLevel 2 requires both
    // database and schema), so the grant type is STANDARD (not FUTURE_AND_ALL).
    // STANDARD grants only match STANDARD grants, so the FUTURE grant should be revoked.
    // Additionally, the grant is on OTHER_DB.TEST_SCHEMA which doesn't match the playbook pattern
    // TEST_DB.TEST_SCHEMA, so it would be revoked for pattern mismatch as well.
    assertEquals(1, actualRevokes.size());
    SnowflakeGrantModel revokedGrant = actualRevokes.get(0).getGrant();
    assertEquals("USAGE", revokedGrant.privilege());
    assertEquals("SCHEMA", revokedGrant.grantedOn());
    assertEquals("OTHER_DB.TEST_SCHEMA", revokedGrant.name());
    assertTrue(revokedGrant.future());
  }

  @Test
  void compileRevokeGrants_shouldReturnResultsSortedByKey() {
    // Given
    SnowflakeGrantModel grantB =
        createGrant("SELECT", "TABLE", "DB.SCHEMA.TABLE_B", "ROLE", "ROLE_1");
    SnowflakeGrantModel grantA =
        createGrant("SELECT", "TABLE", "DB.SCHEMA.TABLE_A", "ROLE", "ROLE_1");

    List<PlaybookPrivilegeGrant> playbookGrants = List.of();

    Map<String, SnowflakeGrantBuilder> currentGrants = new HashMap<>();
    SnowflakeGrantBuilder builderB = SnowflakeGrantBuilder.fromGrant(grantB);
    SnowflakeGrantBuilder builderA = SnowflakeGrantBuilder.fromGrant(grantA);

    if (builderB != null) currentGrants.put(builderB.getKey(), builderB);
    if (builderA != null) currentGrants.put(builderA.getKey(), builderA);

    // When
    List<SnowflakeGrantBuilder> result =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(playbookGrants, currentGrants);

    // Then
    assertEquals(2, result.size());
    assertEquals(builderA.getKey(), result.get(0).getKey());
    assertEquals(builderB.getKey(), result.get(1).getKey());
  }

  // Helper methods for creating test data
  private PlaybookPrivilegeGrant createPlaybookGrant(
      String objectType,
      String databaseName,
      String schemaName,
      String objectName,
      List<String> privileges) {
    return createPlaybookGrant(
        objectType, databaseName, schemaName, objectName, privileges, true, true, true);
  }

  private PlaybookPrivilegeGrant createPlaybookGrant(
      String objectType,
      String databaseName,
      String schemaName,
      String objectName,
      List<String> privileges,
      boolean includeFuture,
      boolean includeAll,
      boolean enable) {
    return new PlaybookPrivilegeGrant(
        objectType,
        objectName,
        schemaName,
        databaseName,
        privileges,
        includeFuture,
        includeAll,
        enable);
  }

  private PlaybookPrivilegeGrant createPlaybookGrantForRole(
      String roleName, List<String> privileges) {
    return new PlaybookPrivilegeGrant("ROLE", roleName, null, null, privileges, true, true, true);
  }

  private SnowflakeGrantModel createGrant(
      String privilege, String grantedOn, String name, String grantedTo, String granteeName) {
    return new SnowflakeGrantModel(
        privilege, grantedOn, name, grantedTo, granteeName, false, false, false);
  }

  private SnowflakeGrantModel createFutureGrant(
      String privilege, String grantedOn, String name, String grantedTo, String granteeName) {
    return new SnowflakeGrantModel(
        privilege, grantedOn, name, grantedTo, granteeName, false, true, false);
  }

  private SnowflakeGrantModel createAllGrant(
      String privilege, String grantedOn, String name, String grantedTo, String granteeName) {
    return new SnowflakeGrantModel(
        privilege, grantedOn, name, grantedTo, granteeName, false, false, true);
  }

  private Map<String, SnowflakeGrantBuilder> createCurrentGrants(SnowflakeGrantModel... grants) {
    Map<String, SnowflakeGrantBuilder> currentGrants = new HashMap<>();
    for (SnowflakeGrantModel grant : grants) {
      SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant);
      if (builder != null) {
        currentGrants.put(builder.getKey(), builder);
      }
    }
    return currentGrants;
  }

  @Test
  void compileRevokeGrants_whenPlaybookHasAgentAndCurrentGrantIsCortexAgent_shouldNotRevoke() {
    // Critical: AGENT string in playbook should map to CORTEX_AGENT enum, matching CORTEX_AGENT
    // grants
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("AGENT", "TEST_DB", "TEST_SCHEMA", "TEST_AGENT", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "CORTEX_AGENT", "TEST_DB.TEST_SCHEMA.TEST_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertTrue(
        actualRevokes.isEmpty(),
        "Should not revoke when playbook has AGENT (mapped to CORTEX_AGENT) and current grant is CORTEX_AGENT");
  }

  @Test
  void compileRevokeGrants_whenPlaybookHasCortexAgentAndCurrentGrantIsAgent_shouldNotRevoke() {
    // Critical: Current grant with "AGENT" string should map to CORTEX_AGENT enum, matching
    // CORTEX_AGENT playbook grants
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "CORTEX_AGENT", "TEST_DB", "TEST_SCHEMA", "TEST_AGENT", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "AGENT", "TEST_DB.TEST_SCHEMA.TEST_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertTrue(
        actualRevokes.isEmpty(),
        "Should not revoke when playbook has CORTEX_AGENT and current grant is AGENT (mapped to CORTEX_AGENT)");
  }

  @Test
  void compileRevokeGrants_whenPlaybookHasAgentAndCurrentGrantIsAgent_shouldNotRevoke() {
    // Critical: Both AGENT strings should map to CORTEX_AGENT enum and match
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("AGENT", "TEST_DB", "TEST_SCHEMA", "TEST_AGENT", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "AGENT", "TEST_DB.TEST_SCHEMA.TEST_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertTrue(
        actualRevokes.isEmpty(),
        "Should not revoke when both playbook and current grant use AGENT (both map to CORTEX_AGENT)");
  }

  @Test
  void
      compileRevokeGrants_whenPlaybookHasCortexAgentAndCurrentGrantIsCortexAgent_shouldNotRevoke() {
    // Critical: Both CORTEX_AGENT should match directly
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "CORTEX_AGENT", "TEST_DB", "TEST_SCHEMA", "TEST_AGENT", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "CORTEX_AGENT", "TEST_DB.TEST_SCHEMA.TEST_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertTrue(
        actualRevokes.isEmpty(),
        "Should not revoke when both playbook and current grant use CORTEX_AGENT");
  }

  @Test
  void compileRevokeGrants_whenPlaybookHasAgentButPrivilegeDiffers_shouldRevoke() {
    // Critical: Even if object types match, privilege mismatch should cause revocation
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("AGENT", "TEST_DB", "TEST_SCHEMA", "TEST_AGENT", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant(
            "OWNERSHIP", "CORTEX_AGENT", "TEST_DB.TEST_SCHEMA.TEST_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertEquals(
        1, actualRevokes.size(), "Should revoke when object types match but privilege differs");
    assertEquals("OWNERSHIP", actualRevokes.get(0).getGrant().privilege());
  }

  @Test
  void compileRevokeGrants_whenPlaybookHasAgentButObjectNameDiffers_shouldRevoke() {
    // Critical: Object name mismatch should cause revocation even if object types match
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("AGENT", "TEST_DB", "TEST_SCHEMA", "TEST_AGENT", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant(
            "USAGE", "CORTEX_AGENT", "TEST_DB.TEST_SCHEMA.OTHER_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertEquals(
        1, actualRevokes.size(), "Should revoke when object types match but object name differs");
    assertEquals("TEST_DB.TEST_SCHEMA.OTHER_AGENT", actualRevokes.get(0).getGrant().name());
  }

  @Test
  void compileRevokeGrants_whenPlaybookDoesNotHaveAgentButCurrentGrantIsAgent_shouldRevoke() {
    // Critical: Current grant with AGENT should be revoked if not in playbook
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List.of("SELECT"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "AGENT", "TEST_DB.TEST_SCHEMA.TEST_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertEquals(1, actualRevokes.size(), "Should revoke AGENT grant when it's not in playbook");
    assertEquals(
        "CORTEX_AGENT",
        actualRevokes.get(0).getGrant().grantedOn(),
        "Revoked grant should have CORTEX_AGENT as object type (AGENT maps to CORTEX_AGENT)");
  }

  @Test
  void compileRevokeGrants_whenPlaybookHasAgentWithWildcard_shouldNotRevoke() {
    // Critical: Wildcard object name should match any agent name
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("AGENT", "TEST_DB", "TEST_SCHEMA", "*", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "CORTEX_AGENT", "TEST_DB.TEST_SCHEMA.ANY_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertTrue(
        actualRevokes.isEmpty(),
        "Should not revoke when playbook has AGENT with wildcard object name");
  }

  @Test
  void compileRevokeGrantsFuture_whenPlaybookHasAgentWithWildcard_shouldNotRevoke() {
    // Critical: Wildcard object name should match any agent name
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("AGENT", "TEST_DB", "TEST_SCHEMA", "*", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        new SnowflakeGrantModel(
            "USAGE",
            "CORTEX_AGENT",
            "TEST_DB.TEST_SCHEMA.<CORTEX_AGENT>",
            "ROLE",
            "TEST_ROLE",
            false,
            true,
            false);
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertTrue(
        actualRevokes.isEmpty(),
        "Should not revoke when playbook has AGENT with wildcard object name");
  }

  @Test
  void compileRevokeGrants_whenPlaybookHasAgentCaseInsensitive_shouldNotRevoke() {
    // Critical: Case should not matter for AGENT override
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("agent", "TEST_DB", "TEST_SCHEMA", "TEST_AGENT", List.of("USAGE"));
    SnowflakeGrantModel currentGrant =
        createGrant("USAGE", "CORTEX_AGENT", "TEST_DB.TEST_SCHEMA.TEST_AGENT", "ROLE", "TEST_ROLE");
    Map<String, SnowflakeGrantBuilder> currentGrants = createCurrentGrants(currentGrant);

    List<SnowflakeGrantBuilder> actualRevokes =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(List.of(playbookGrant), currentGrants);

    assertTrue(
        actualRevokes.isEmpty(),
        "Should not revoke when playbook has lowercase 'agent' (maps to CORTEX_AGENT)");
  }
}
