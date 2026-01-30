package us.zoom.data.dfence.providers.snowflake.revoke.index;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.policies.factories.PolicyGrantFactory;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PolicyGrantHashIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeObjectTypeAlias;

class PolicyGrantHashIndexerTest {

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
    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(grants);

    // Then
    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
    var updateGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("UPDATE"));

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
    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(grants);

    // Then
    var tableGrants = index.snowflakeObjectTypeIndex().get(SnowflakeObjectType.TABLE);
    var viewGrants = index.snowflakeObjectTypeIndex().get(SnowflakeObjectType.VIEW);

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
    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(grants);

    // Then - primary object type should be in snowflakeObjectTypeIndex
    var primaryGrants = index.snowflakeObjectTypeIndex().get(SnowflakeObjectType.TABLE);
    assertNotNull(
        primaryGrants, "Primary object type (TABLE) should be in snowflakeObjectTypeIndex");
    assertEquals(1, primaryGrants.size());

    // Then - alias object type should be in snowflakeObjectAliasIndex
    var aliasGrants =
        index
            .snowflakeObjectTypeAliasIndex()
            .get(SnowflakeObjectTypeAlias.of(SnowflakeObjectType.EXTERNAL_TABLE));
    assertNotNull(
        aliasGrants, "Alias object type (EXTERNAL_TABLE) should be in snowflakeObjectAliasIndex");
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
    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(grants);

    // Then
    var aliasGrants =
        index
            .snowflakeObjectTypeAliasIndex()
            .get(SnowflakeObjectTypeAlias.of(SnowflakeObjectType.EXTERNAL_TABLE));
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
    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(grants);

    // Then
    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
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
    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(grants);

    // Then
    PlaybookPrivilegeGrant expectedGrant =
        createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List.of("SELECT"), false);

    assertTrue(
        index
            .privilegeIndex()
            .get(new PolicyGrantPrivilege("SELECT"))
            .contains(PolicyGrantFactory.createFrom(expectedGrant)));
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
        () -> PolicyGrantHashIndexer.create(grants),
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

    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    assertTrue(
        selectGrants.iterator().next().policyType() instanceof PolicyType.Standard,
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

    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    PolicyGrant policyGrant = selectGrants.iterator().next();
    assertTrue(
        policyGrant.policyType() instanceof PolicyType.Container,
        "Should create Container pattern type when only includeFuture is true");
    PolicyType.Container container = (PolicyType.Container) policyGrant.policyType();
    assertTrue(container instanceof PolicyType.Container.Schema);
    PolicyType.Container.Schema schema = (PolicyType.Container.Schema) container;
    assertTrue(schema.containerPolicyOptions().future());
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

    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    PolicyGrant policyGrant = selectGrants.iterator().next();
    assertTrue(
        policyGrant.policyType() instanceof PolicyType.Container,
        "Should create Container pattern type when only includeAll is true");
    PolicyType.Container container = (PolicyType.Container) policyGrant.policyType();
    assertTrue(container instanceof PolicyType.Container.Schema);
    PolicyType.Container.Schema schema = (PolicyType.Container.Schema) container;
    assertTrue(schema.containerPolicyOptions().all());
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

    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    assertEquals(1, selectGrants.size());
    PolicyGrant policyGrant = selectGrants.iterator().next();
    assertTrue(
        policyGrant.policyType() instanceof PolicyType.Container,
        "Should create Container pattern type when both future and all are true");
    PolicyType.Container container = (PolicyType.Container) policyGrant.policyType();
    assertTrue(container instanceof PolicyType.Container.Schema);
    PolicyType.Container.Schema schema = (PolicyType.Container.Schema) container;
    assertTrue(schema.containerPolicyOptions().future());
    assertTrue(schema.containerPolicyOptions().all());
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

    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
    var updateGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("UPDATE"));
    assertNotNull(selectGrants, "SELECT privilege should be indexed (normalized from lowercase)");
    assertNotNull(updateGrants, "UPDATE privilege should be indexed (normalized from lowercase)");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldTrimWhitespaceFromDatabaseSchemaAndObjectNames() {
    // Critical: Names should be trimmed
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "TABLE", "  T1  ", "  SCHEMA  ", "  DB  ", List.of("SELECT"), true, true, true);

    PolicyGrantHashIndex index = PolicyGrantHashIndexer.create(new ArrayList<>(List.of(grant)));

    var selectGrants = index.privilegeIndex().get(new PolicyGrantPrivilege("SELECT"));
    assertNotNull(selectGrants);
    PolicyGrant policyGrant = selectGrants.iterator().next();
    // Assuming PolicyType.Standard.SchemaObject
    assertTrue(policyGrant.policyType() instanceof PolicyType.Standard.SchemaObject);
    PolicyType.Standard.SchemaObject schemaObject =
        (PolicyType.Standard.SchemaObject) policyGrant.policyType();

    assertEquals("DB", schemaObject.databaseName(), "Database name should be trimmed");
    assertEquals("SCHEMA", schemaObject.schemaName(), "Schema name should be trimmed");
    assertEquals("T1", schemaObject.objectName(), "Object name should be trimmed");
  }

  @Test
  void buildPlaybookGrantHashIndex_shouldThrow_whenConversionFails() {
    // Critical: Exception handling - when toPlaybookGrant throws exception, should throw
    // RbacDataError
    PlaybookPrivilegeGrant invalidGrant =
        new PlaybookPrivilegeGrant(
            "INVALID_TYPE_THAT_THROWS", "OBJ", "SCHEMA", "DB", List.of("SELECT"), true, true, true);

    // When/Then: Invalid grants throw RbacDataError during conversion
    assertThrows(
        RbacDataError.class,
        () -> PolicyGrantHashIndexer.create(new ArrayList<>(List.of(invalidGrant))),
        "Should throw RbacDataError when conversion fails");
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
