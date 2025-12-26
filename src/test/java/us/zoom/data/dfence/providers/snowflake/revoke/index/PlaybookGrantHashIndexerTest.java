package us.zoom.data.dfence.providers.snowflake.revoke.index;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantHashIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectTypeAlias;

class PlaybookGrantHashIndexerTest {

  @Test
  void buildPrivilegeGrantIndex_shouldCorrectlyIndexGrantsByPlaybook() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(
                createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT")),
                createPlaybookGrant("TABLE", "DB", "SCHEMA", "T2", List.of("SELECT", "UPDATE")),
                createPlaybookGrant("VIEW", "DB", "SCHEMA", "V1", List.of("SELECT"))));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then
    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    var updateGrants = index.privilegeIndex().get(new GrantPrivilege("UPDATE"));

    assertNotNull(selectGrants);
    assertNotNull(updateGrants);
    assertEquals(3, selectGrants.size());
    assertEquals(1, updateGrants.size());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCorrectlyIndexGrantsByObjectType() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(
                createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT")),
                createPlaybookGrant("VIEW", "DB", "SCHEMA", "V1", List.of("SELECT"))));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then
    var tableGrants = index.objectTypeIndex().get(ObjectType.apply(SnowflakeObjectType.TABLE));
    var viewGrants = index.objectTypeIndex().get(ObjectType.apply(SnowflakeObjectType.VIEW));

    assertNotNull(tableGrants);
    assertNotNull(viewGrants);
    assertEquals(1, tableGrants.size());
    assertEquals(1, viewGrants.size());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldIndexBothPrimaryAndAliasObjectTypes() {
    // Critical: Test both branches of the if/else in buildObjectTypeAndObjectTypeAliasIndex
    // Primary object type: TABLE (getObjectType().equals(getAliasFor()) is true)
    // Alias object type: EXTERNAL_TABLE (getObjectType().equals(getAliasFor()) is false)
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(
                createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT")), // Primary
                createPlaybookGrant(
                    "EXTERNAL_TABLE", "DB", "SCHEMA", "ET1", List.of("SELECT")) // Alias
                ));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then - primary object type should be in objectTypeIndex
    var primaryGrants = index.objectTypeIndex().get(ObjectType.apply(SnowflakeObjectType.TABLE));
    assertNotNull(primaryGrants, "Primary object type (TABLE) should be in objectTypeIndex");
    assertEquals(1, primaryGrants.size());

    // Then - alias object type should be in objectAliasIndex
    var aliasGrants =
        index.objectAliasIndex().get(ObjectTypeAlias.apply(SnowflakeObjectType.EXTERNAL_TABLE));
    assertNotNull(aliasGrants, "Alias object type (EXTERNAL_TABLE) should be in objectAliasIndex");
    assertEquals(1, aliasGrants.size());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCorrectlyIndexGrantsByAliasedObjectType() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(
                createPlaybookGrant("EXTERNAL_TABLE", "DB", "SCHEMA", "ET1", List.of("SELECT"))));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then
    var aliasGrants =
        index.objectAliasIndex().get(ObjectTypeAlias.apply(SnowflakeObjectType.EXTERNAL_TABLE));
    assertNotNull(aliasGrants);
    assertEquals(1, aliasGrants.size());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldMergeDuplicateKeysCorrectly() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(
                createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT")),
                createPlaybookGrant("TABLE", "DB", "SCHEMA", "T2", List.of("SELECT"))));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then
    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(2, selectGrants.size());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldFilterDisabledGrants() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT"), false)));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then
    assertTrue(index.privilegeIndex().isEmpty());
    assertTrue(index.objectTypeIndex().isEmpty());
    assertTrue(index.objectAliasIndex().isEmpty());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldHandleInvalidObjectTypesGracefully() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(
                createPlaybookGrant(
                    "INVALID_OBJECT_TYPE", "DB", "SCHEMA", "T1", List.of("SELECT"))));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then
    assertTrue(index.privilegeIndex().isEmpty(), "Invalid grants should be filtered out");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCreateStandardGrantType_whenBothFutureAndAllAreFalse() {
    // Critical: Standard grant type when neither future nor all is enabled
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "SCHEMA",
            "DB",
            List.of("SELECT"),
            false, // includeFuture = false
            false, // includeAll = false
            true // enable
            );

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    assertEquals(
        PlaybookGrantType.Standard,
        selectGrants.iterator().next().grantType(),
        "Should create Standard grant type when both future and all are false");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCreateFutureGrantType_whenOnlyFutureIsTrue() {
    // Critical: Future grant type when only includeFuture is true
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "SCHEMA",
            "DB",
            List.of("SELECT"),
            true, // includeFuture = true
            false, // includeAll = false
            true // enable
            );

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    assertEquals(
        PlaybookGrantType.Future,
        selectGrants.iterator().next().grantType(),
        "Should create Future grant type when only includeFuture is true");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCreateAllGrantType_whenOnlyAllIsTrue() {
    // Critical: All grant type when only includeAll is true
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "SCHEMA",
            "DB",
            List.of("SELECT"),
            false, // includeFuture = false
            true, // includeAll = true
            true // enable
            );

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    assertEquals(
        PlaybookGrantType.All,
        selectGrants.iterator().next().grantType(),
        "Should create All grant type when only includeAll is true");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCreateFutureAndAllGrantType_whenBothFutureAndAllAreTrue() {
    // Critical: FutureAndAll grant type when both are true
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "SCHEMA",
            "DB",
            List.of("SELECT"),
            true, // includeFuture = true
            true, // includeAll = true
            true // enable
            );

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    assertEquals(
        PlaybookGrantType.FutureAndAll,
        selectGrants.iterator().next().grantType(),
        "Should create FutureAndAll grant type when both future and all are true");
  }

  @Test
  void buildPrivilegeGrantIndex_shouldNormalizePlaybookNamesToUppercase() {
    // Critical: Privileges should be normalized to uppercase
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "SCHEMA",
            "DB",
            List.of("select", "update"), // Lowercase
            true,
            true,
            true);

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    var updateGrants = index.privilegeIndex().get(new GrantPrivilege("UPDATE"));
    assertNotNull(selectGrants, "SELECT privilege should be indexed (normalized from lowercase)");
    assertNotNull(updateGrants, "UPDATE privilege should be indexed (normalized from lowercase)");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldTrimWhitespaceFromDatabaseSchemaAndObjectNames() {
    // Critical: Names should be trimmed
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE", "  T1  ", "  SCHEMA  ", "  DB  ", List.of("SELECT"), true, true, true);

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertEquals(
        Optional.of("DB"), playbookGrant.pattern().dbName(), "Database name should be trimmed");
    assertEquals(
        Optional.of("SCHEMA"), playbookGrant.pattern().schName(), "Schema name should be trimmed");
    assertEquals(
        Optional.of("T1"), playbookGrant.pattern().objName(), "Object name should be trimmed");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldHandleEmptyOrWhitespaceNamesAsNull() {
    // Critical: Empty/whitespace names should become null
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant("TABLE", "  ", "", "DB", List.of("SELECT"), true, true, true);

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertTrue(
        playbookGrant.pattern().schName().isEmpty(),
        "Empty schema name should become Optional.empty()");
    assertTrue(
        playbookGrant.pattern().objName().isEmpty(),
        "Whitespace object name should become Optional.empty()");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldHandleExceptionGracefully_whenConversionFails() {
    // Critical: Exception handling branch - when toPlaybookGrant throws exception, should return
    // empty Optional
    // Create a grant with invalid object type that will cause SnowflakeObjectType.fromString to
    // throw
    PlaybookPrivilegeGrant invalidGrant =
        new PlaybookPrivilegeGrant(
            "INVALID_TYPE_THAT_THROWS", "OBJ", "SCHEMA", "DB", List.of("SELECT"), true, true, true);

    PlaybookGrantHashIndex index =
        PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(invalidGrant)));

    // Should handle gracefully and return empty index (invalid grant filtered out)
    assertTrue(
        index.privilegeIndex().isEmpty(),
        "Invalid grants should be filtered out when conversion fails");
    assertTrue(
        index.objectTypeIndex().isEmpty(),
        "Invalid grants should be filtered out when conversion fails");
    assertTrue(
        index.objectAliasIndex().isEmpty(),
        "Invalid grants should be filtered out when conversion fails");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldKeepNonEmptyTrimmedDatabaseName() {
    // Critical: Test the filter branch at line 89 - when trimmed string is NOT empty (filter
    // returns true)
    // This covers the missing branch: s -> !s.isEmpty() when s is NOT empty
    // We need a case where databaseName() is not null, and after trim() it's not empty
    // This tests the branch where the filter keeps the value (filter returns true)
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "SCHEMA",
            "  DB  ", // Has whitespace but not empty after trim
            List.of("SELECT"),
            true,
            true,
            true);

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertEquals(
        Optional.of("DB"),
        playbookGrant.pattern().dbName(),
        "Non-empty trimmed database name should be kept (filter returns true branch)");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldKeepNonEmptyTrimmedSchemaName() {
    // Critical: Test the filter branch at line 92 - when trimmed schema name is NOT empty (filter
    // returns true)
    // This tests the branch where the filter keeps the value (filter returns true) for schName
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "  SCHEMA  ", // Has whitespace but not empty after trim
            "DB",
            List.of("SELECT"),
            true,
            true,
            true);

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertEquals(
        Optional.of("SCHEMA"),
        playbookGrant.pattern().schName(),
        "Non-empty trimmed schema name should be kept (filter returns true branch)");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldFilterOutEmptyTrimmedDatabaseName() {
    // Critical: Test the filter branch at line 89 - when trimmed database name IS empty (filter
    // returns false)
    // This tests the branch where the filter removes the value (filter returns false) for dbName
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "SCHEMA",
            "   ", // Whitespace only, becomes empty after trim
            List.of("SELECT"),
            true,
            true,
            true);

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertTrue(
        playbookGrant.pattern().dbName().isEmpty(),
        "Empty trimmed database name should be filtered out (filter returns false branch)");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldFilterOutEmptyTrimmedSchemaName() {
    // Critical: Test the filter branch at line 92 - when trimmed schema name IS empty (filter
    // returns false)
    // This tests the branch where the filter removes the value (filter returns false) for schName
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "T1",
            "   ", // Whitespace only, becomes empty after trim
            "DB",
            List.of("SELECT"),
            true,
            true,
            true);

    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new GrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertTrue(
        playbookGrant.pattern().schName().isEmpty(),
        "Empty trimmed schema name should be filtered out (filter returns false branch)");
  }

  private PlaybookPrivilegeGrant createPlaybookGrant(
      String objectType,
      String databaseName,
      String schemaName,
      String objectName,
      List<String> privileges,
      boolean enable) {
    return new PlaybookPrivilegeGrant(
        objectType,
        objectName,
        schemaName,
        databaseName,
        privileges,
        true, // includeFuture
        true, // includeAll
        enable);
  }

  private PlaybookPrivilegeGrant createPlaybookGrant(
      String objectType,
      String databaseName,
      String schemaName,
      String objectName,
      List<String> privileges) {
    return createPlaybookGrant(objectType, databaseName, schemaName, objectName, privileges, true);
  }
}
