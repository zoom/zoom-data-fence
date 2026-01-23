package us.zoom.data.dfence.providers.snowflake.revoke.index;

import static org.junit.jupiter.api.Assertions.*;

import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.PlaybookGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantHashIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectTypeAlias;
import us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models.ResolvedPlaybookPattern;

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
  void buildPlaybookGrantHashIndex_shouldNotFilterDisabledGrants() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT"), false)));

    // When
    PlaybookGrantHashIndex index = PlaybookGrantHashIndexer.create(grants);

    // Then
    PlaybookPrivilegeGrant expectedGrant =
        createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT"), false);

    assertTrue(
        index
            .privilegeIndex()
            .get(new GrantPrivilege("SELECT"))
            .contains(PlaybookGrantCompanion.from(expectedGrant)));
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldThrow_whenInvalidObjectTypes() {
    // Given
    List<PlaybookPrivilegeGrant> grants =
        new ArrayList<>(
            List.of(
                createPlaybookGrant(
                    "INVALID_OBJECT_TYPE", "DB", "SCHEMA", "T1", List.of("SELECT"))));

    // When/Then: Invalid object types throw RbacDataError during conversion
    assertThrows(
        RbacDataError.class,
        () -> PlaybookGrantHashIndexer.create(grants),
        "Should throw RbacDataError when object type is invalid");
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
    assertTrue(
        selectGrants.iterator().next().resolvedPattern()
            instanceof ResolvedPlaybookPattern.Standard,
        "Should create Standard pattern type when both future and all are false");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCreateFutureGrantType_whenOnlyFutureIsTrue() {
    // Critical: Future grant type when only includeFuture is true
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "*", // Wildcard object name for FUTURE grant
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
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertTrue(
        playbookGrant.resolvedPattern() instanceof ResolvedPlaybookPattern.Container,
        "Should create Container pattern type when only includeFuture is true");
    ResolvedPlaybookPattern.Container container =
        (ResolvedPlaybookPattern.Container) playbookGrant.resolvedPattern();
    assertTrue(container instanceof ResolvedPlaybookPattern.Container.Schema);
    ResolvedPlaybookPattern.Container.Schema schema =
        (ResolvedPlaybookPattern.Container.Schema) container;
    assertEquals(ContainerPatternOptions.FUTURE, schema.playbookContainerPatternOptions());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCreateAllGrantType_whenOnlyAllIsTrue() {
    // Critical: All grant type when only includeAll is true
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "*", // Wildcard object name for ALL grant
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
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertTrue(
        playbookGrant.resolvedPattern() instanceof ResolvedPlaybookPattern.Container,
        "Should create Container pattern type when only includeAll is true");
    ResolvedPlaybookPattern.Container container =
        (ResolvedPlaybookPattern.Container) playbookGrant.resolvedPattern();
    assertTrue(container instanceof ResolvedPlaybookPattern.Container.Schema);
    ResolvedPlaybookPattern.Container.Schema schema =
        (ResolvedPlaybookPattern.Container.Schema) container;
    assertEquals(ContainerPatternOptions.ALL, schema.playbookContainerPatternOptions());
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldCreateFutureAndAllGrantType_whenBothFutureAndAllAreTrue() {
    // Critical: FUTURE_AND_ALL grant type when both future and all are true
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE",
            "*", // Wildcard object name for FUTURE_AND_ALL grant
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
    PlaybookGrant playbookGrant = selectGrants.iterator().next();
    assertTrue(
        playbookGrant.resolvedPattern() instanceof ResolvedPlaybookPattern.Container,
        "Should create Container pattern type when both future and all are true");
    ResolvedPlaybookPattern.Container container =
        (ResolvedPlaybookPattern.Container) playbookGrant.resolvedPattern();
    assertTrue(container instanceof ResolvedPlaybookPattern.Container.Schema);
    ResolvedPlaybookPattern.Container.Schema schema =
        (ResolvedPlaybookPattern.Container.Schema) container;
    assertEquals(ContainerPatternOptions.FUTURE_AND_ALL, schema.playbookContainerPatternOptions());
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
        Option.some("DB"), playbookGrant.pattern().dbName(), "Database name should be trimmed");
    assertEquals(
        Option.some("SCHEMA"), playbookGrant.pattern().schName(), "Schema name should be trimmed");
    assertEquals(
        Option.some("T1"), playbookGrant.pattern().objName(), "Object name should be trimmed");
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
  void buildPlaybookGrantHashIndex_shouldThrow_whenConversionFails() {
    // Critical: Exception handling - when toPlaybookGrant throws exception, should throw
    // RbacDataError
    // Create a grant with invalid object type that will cause SnowflakeObjectType.fromString to
    // throw
    PlaybookPrivilegeGrant invalidGrant =
        new PlaybookPrivilegeGrant(
            "INVALID_TYPE_THAT_THROWS", "OBJ", "SCHEMA", "DB", List.of("SELECT"), true, true, true);

    // When/Then: Invalid grants throw RbacDataError during conversion
    assertThrows(
        RbacDataError.class,
        () -> PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(invalidGrant))),
        "Should throw RbacDataError when conversion fails");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldKeepNonEmptyTrimmedDatabaseName() {
    // Critical: Test the filter branch - when trimmed string is NOT empty (filter returns true)
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
        Option.some("DB"),
        playbookGrant.pattern().dbName(),
        "Non-empty trimmed database name should be kept (filter returns true branch)");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldKeepNonEmptyTrimmedSchemaName() {
    // Critical: Test the filter branch - when trimmed schema name is NOT empty (filter returns
    // true)
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
        Option.some("SCHEMA"),
        playbookGrant.pattern().schName(),
        "Non-empty trimmed schema name should be kept (filter returns true branch)");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldThrow_whenEmptyTrimmedDatabaseName() {
    // Critical: Test that grants with empty database names after trim throw RbacDataError
    // because pattern type determination requires a non-empty database name
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

    // When/Then: Empty database name causes pattern validation to fail, throwing RbacDataError
    assertThrows(
        RbacDataError.class,
        () -> PlaybookGrantHashIndexer.create(new ArrayList<>(List.of(grant))),
        "Should throw RbacDataError when database name is empty after trim");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldFilterOutEmptyTrimmedSchemaName() {
    // Critical: Test the filter branch - when trimmed schema name IS empty (filter returns false)
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
