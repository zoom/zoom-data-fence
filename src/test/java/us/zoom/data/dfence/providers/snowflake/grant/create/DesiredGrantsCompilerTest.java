package us.zoom.data.dfence.providers.snowflake.grant.create;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.DesiredGrantsCompiler;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DesiredGrantsCompiler")
class DesiredGrantsCompilerTest {

  private SnowflakeObjectsService mockObjectsService;
  private DesiredGrantsCompiler desiredGrantsCompiler;
  private SnowflakeGrantBuilderOptions options;

  @BeforeEach
  void setUp() {
    mockObjectsService = mock(SnowflakeObjectsService.class);
    desiredGrantsCompiler = new DesiredGrantsCompiler(mockObjectsService);
    options = new SnowflakeGrantBuilderOptions();
  }

  @Test
  @DisplayName("compileGrants creates grant for specific table")
  void compileGrants_createsGrantForSpecificTable() {
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"));

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(1, actualBuilders.size());
    SnowflakeGrantBuilder builder = actualBuilders.get(0);
    assertTrue(builder.getKey().contains("MY_DB.MY_SCHEMA.MY_TABLE"));
    assertTrue(builder.getKey().contains("SELECT"));
  }

  @Test
  @DisplayName(
      "compileGrants normalizes object names using ObjectName.normalize")
  void compileGrants_normalizesObjectNames() {
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("table", "my_table", "my_schema", "my_db", List.of("SELECT"));

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(1, actualBuilders.size());
    String key = actualBuilders.get(0).getKey();
    assertTrue(key.contains("MY_DB.MY_SCHEMA.MY_TABLE"));
  }

  @Test
  @DisplayName(
      "compileGrants creates multiple grants for multiple privileges")
  void compileGrants_createsMultipleGrantsForMultiplePrivileges() {
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT", "INSERT", "UPDATE"));

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(3, actualBuilders.size());
  }

  @Test
  @DisplayName(
      "compileGrants throws when wildcard is used with standard grant type")
  void compileGrants_throwsWhenWildcardUsedWithStandardGrant() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    assertThrows(
        Exception.class,
        () -> desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options));
  }

  @Test
  @DisplayName(
      "compileGrants uses database name when objectName is null for qualLevel 1")
  void compileGrants_usesDatabaseNameWhenObjectNameIsNullForQualLevel1() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "database", null, null, "MY_DB", List.of("USAGE"), false, false, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(1, actualBuilders.size());
    assertTrue(actualBuilders.get(0).getKey().contains("MY_DB"));
  }

  @Test
  @DisplayName("compileGrants throws when validation fails")
  void compileGrants_throwsWhenValidationFails() {
    // Create a grant with missing schema name for a table (qualLevel 3 requires schema)
    PlaybookPrivilegeGrant playbookGrant =
        createPlaybookGrant("table", "MY_TABLE", null, "MY_DB", List.of("SELECT"));

    Exception actualException =
        assertThrows(
            Exception.class,
            () -> desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options),
            "Should throw when object name validation fails");

    assertTrue(
        actualException.getMessage().contains("Unable to generate grants")
            || actualException.getClass().getSimpleName().contains("RbacData"),
        "Should throw error about generating grants");
  }

  @Test
  @DisplayName("compileGrants creates future grants for database container")
  void compileGrants_createsFutureGrantsForDatabaseContainer() {
    when(mockObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class)))
        .thenReturn(true);
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "*", "MY_DB", List.of("SELECT"), true, false, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(1, actualBuilders.size());
    assertTrue(actualBuilders.get(0).getKey().contains("MY_DB.<TABLE>"));
  }

  @Test
  @DisplayName("compileGrants creates all grants when includeAll is true")
  void compileGrants_createsAllGrantsWhenIncludeAllIsTrue() {
    when(mockObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class)))
        .thenReturn(true);
    when(mockObjectsService.getContainerObjectQualNames(
            any(SnowflakeObjectType.class), any(SnowflakeObjectType.class), anyString()))
        .thenReturn(List.of("MY_DB.MY_SCHEMA.TABLE1", "MY_DB.MY_SCHEMA.TABLE2"));
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, true, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(2, actualBuilders.size());
    assertTrue(actualBuilders.stream().anyMatch(g -> g.getKey().contains("TABLE1")));
    assertTrue(actualBuilders.stream().anyMatch(g -> g.getKey().contains("TABLE2")));
  }

  @Test
  @DisplayName("compileGrants creates at least future and all grants for database container")
  void compileGrants_createsAtLeastFutureAndAllGrants_forDatabaseContainer() {
    when(mockObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class)))
        .thenReturn(true);
    when(mockObjectsService.getContainerObjectQualNames(
            eq(SnowflakeObjectType.DATABASE), eq(SnowflakeObjectType.SCHEMA), anyString()))
        .thenReturn(List.of("MY_DB.SCHEMA1"));
    when(mockObjectsService.getContainerObjectQualNames(
            eq(SnowflakeObjectType.DATABASE), eq(SnowflakeObjectType.TABLE), anyString()))
        .thenReturn(List.of("MY_DB.TABLE1"));
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant("table", "*", "*", "MY_DB", List.of("SELECT"), true, true, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertTrue(actualBuilders.size() >= 2, "Should contain at least future and all grants");
    assertTrue(
        actualBuilders.stream().anyMatch(b -> b.getKey().contains("<TABLE>")),
        "Should contain future grant");
    assertTrue(
        actualBuilders.stream().anyMatch(b -> b.getKey().contains("TABLE1")),
        "Should contain all grant");
  }

  @Test
  @DisplayName("compileGrants throws when database name is wildcard")
  void compileGrants_throwsWhenDatabaseNameIsWildcard() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant("table", "*", "*", "*", List.of("SELECT"), true, false, true);

    Exception actualException =
        assertThrows(
            Exception.class,
            () -> desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options));

    assertTrue(
        actualException.getMessage().contains("non-empty and non-wildcard value is expected")
            || actualException.getMessage().contains("Unable to generate grants")
            || actualException.getMessage().contains("Unable to convert playbook grant")
            || actualException.getMessage().contains("Unable to determine pattern type")
            || actualException
                .getMessage()
                .contains("Failed to convert playbook grant to Snowflake grants"),
        "Should throw error about database name validation");
  }

  @Test
  @DisplayName("compileGrants creates future grants with future flag")
  void compileGrants_createsFutureGrantsWithFutureFlag() {
    when(mockObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class)))
        .thenReturn(true);
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "*", "MY_DB", List.of("SELECT"), true, false, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(1, actualBuilders.size());
    SnowflakeGrantModel grant = actualBuilders.get(0).getGrant();
    assertEquals("SELECT", grant.privilege());
    assertTrue(grant.name().contains("MY_DB.<TABLE>"));
    assertEquals(true, grant.future());
    assertEquals(false, grant.all());
  }

  @Test
  @DisplayName(
      "compileGrants handles future grants for object types with spaces")
  void compileGrants_handlesFutureGrantsForObjectTypesWithSpaces() {
    when(mockObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class)))
        .thenReturn(true);
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "external_table", "*", "*", "MY_DB", List.of("SELECT"), true, false, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(1, actualBuilders.size());
    assertTrue(actualBuilders.get(0).getGrant().name().contains("MY_DB.<EXTERNAL_TABLE>"));
  }

  @Test
  @DisplayName(
      "compileGrants returns empty or single database-level future grant when database does not exist")
  void compileGrants_returnsEmptyOrSingleDatabaseLevelFutureGrant_whenDatabaseDoesNotExist() {
    when(mockObjectsService.objectExists("\"MY_DB\"", SnowflakeObjectType.DATABASE))
        .thenReturn(false);
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "*", "MY_DB", List.of("SELECT"), true, false, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertTrue(actualBuilders.isEmpty() || (actualBuilders.size() == 1 && actualBuilders.get(0).getGrant().name().contains("MY_DB.<TABLE>")),
        "When database does not exist, expect empty or single database-level future grant");
  }

  @Test
  @DisplayName(
      "compileGrants creates future grants per schema when database container and schemas exist")
  void compileGrants_createsFutureGrantsPerSchema_whenDatabaseContainerAndSchemasExist() {
    when(mockObjectsService.objectExists(anyString(), eq(SnowflakeObjectType.DATABASE)))
        .thenReturn(true);
    when(mockObjectsService.getContainerObjectQualNames(
            eq(SnowflakeObjectType.DATABASE), eq(SnowflakeObjectType.SCHEMA), anyString()))
        .thenReturn(List.of("MY_DB.SCHEMA1", "MY_DB.SCHEMA2"));
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "*", "MY_DB", List.of("SELECT"), true, false, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertTrue(actualBuilders.size() >= 1);
    assertTrue(
        actualBuilders.stream()
            .anyMatch(
                b ->
                    b.getGrant().name().contains("SCHEMA1")
                        && b.getGrant().name().contains("<TABLE>")));
    assertTrue(
        actualBuilders.stream()
            .anyMatch(
                b ->
                    b.getGrant().name().contains("SCHEMA2")
                        && b.getGrant().name().contains("<TABLE>")));
  }

  @Test
  @DisplayName(
      "compileGrants returns empty when container does not exist for all grants")
  void compileGrants_returnsEmptyWhenContainerDoesNotExistForAllGrants() {
    when(mockObjectsService.objectExists("\"MY_DB\".\"MY_SCHEMA\"", SnowflakeObjectType.SCHEMA))
        .thenReturn(false);
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, true, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(0, actualBuilders.size());
  }

  @Test
  @DisplayName("compileGrants creates grants for all objects in container")
  void compileGrants_createsGrantsForAllObjectsInContainer() {
    when(mockObjectsService.objectExists(anyString(), eq(SnowflakeObjectType.SCHEMA)))
        .thenReturn(true);
    when(mockObjectsService.getContainerObjectQualNames(
            eq(SnowflakeObjectType.SCHEMA), eq(SnowflakeObjectType.TABLE), anyString()))
        .thenReturn(List.of("MY_DB.MY_SCHEMA.TABLE1", "MY_DB.MY_SCHEMA.TABLE2"));
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", List.of("SELECT", "INSERT"), false, true, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(4, actualBuilders.size());
    assertTrue(
        actualBuilders.stream()
            .anyMatch(
                b ->
                    b.getGrant().name().equals("MY_DB.MY_SCHEMA.TABLE1")
                        && b.getGrant().privilege().equals("SELECT")));
    assertTrue(
        actualBuilders.stream()
            .anyMatch(
                b ->
                    b.getGrant().name().equals("MY_DB.MY_SCHEMA.TABLE1")
                        && b.getGrant().privilege().equals("INSERT")));
    assertTrue(
        actualBuilders.stream()
            .anyMatch(
                b ->
                    b.getGrant().name().equals("MY_DB.MY_SCHEMA.TABLE2")
                        && b.getGrant().privilege().equals("SELECT")));
  }

  @Test
  @DisplayName("compileGrants handles schema-level grants with normalized names")
  void compileGrants_handlesSchemaLevelGrantsWithNormalizedNames() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "schema", null, "my_schema", "my_db", List.of("USAGE"), false, false, true);

    List<SnowflakeGrantBuilder> actualBuilders =
        desiredGrantsCompiler.compileGrants(playbookGrant, "ROLE1", options);

    assertEquals(1, actualBuilders.size());
    String key = actualBuilders.get(0).getKey();
    assertTrue(key.contains("MY_DB.MY_SCHEMA"));
  }

  private PlaybookPrivilegeGrant createPlaybookGrant(
      String objectType,
      String objectName,
      String schemaName,
      String databaseName,
      List<String> privileges) {
    return new PlaybookPrivilegeGrant(
        objectType, objectName, schemaName, databaseName, privileges, false, false, true);
  }
}
