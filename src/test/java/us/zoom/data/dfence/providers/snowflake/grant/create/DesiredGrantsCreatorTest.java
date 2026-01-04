package us.zoom.data.dfence.providers.snowflake.grant.create;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

class DesiredGrantsCreatorTest {

  @Mock private SnowflakeObjectsService mockSnowflakeObjectsService;

  private DesiredGrantsCreator desiredGrantsCreator;
  private AutoCloseable mocks;
  private String testRoleName = "TEST_ROLE";

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    desiredGrantsCreator = new DesiredGrantsCreator(mockSnowflakeObjectsService);
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void standardGrants_shouldReturnGrants_whenValidGrant() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table",
            "TEST_TABLE",
            "TEST_SCHEMA",
            "TEST_DB",
            List.of("SELECT", "UPDATE"),
            true,
            true);

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.standardGrants(playbookGrant, testRoleName);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("SELECT", result.get(0).privilege());
    assertEquals("UPDATE", result.get(1).privilege());
  }

  @Test
  void standardGrants_shouldReturnEmpty_whenWildcardObjectName() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "*", "TEST_SCHEMA", "TEST_DB", List.of("SELECT"), true, true);

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.standardGrants(playbookGrant, testRoleName);

    assertTrue(result.isEmpty());
  }

  @Test
  void containerGrants_shouldReturnFutureGrants_whenIncludeFuture() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "view", "*", "*", "TEST_DB", List.of("SELECT"), true, true, true);

    when(mockSnowflakeObjectsService.objectExists("TEST_DB", SnowflakeObjectType.DATABASE))
        .thenReturn(true);

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.containerGrants(playbookGrant, testRoleName);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.stream().anyMatch(g -> g.future()));
  }

  @Test
  void containerGrants_shouldReturnAllGrants_whenIncludeAll() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant("table", "*", "*", "TEST_DB", List.of("SELECT"), false, true);

    when(mockSnowflakeObjectsService.objectExists("TEST_DB", SnowflakeObjectType.DATABASE))
        .thenReturn(true);
    when(mockSnowflakeObjectsService.getContainerObjectQualNames(
            eq(SnowflakeObjectType.DATABASE), eq(SnowflakeObjectType.TABLE), eq("TEST_DB")))
        .thenReturn(List.of("TEST_DB.SCHEMA1.TABLE1", "TEST_DB.SCHEMA1.TABLE2"));

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.containerGrants(playbookGrant, testRoleName);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(2, result.size()); // 2 tables * 1 privilege
    assertFalse(result.stream().anyMatch(g -> g.all())); // expandAllGrants sets all to false
  }

  @Test
  void containerGrants_shouldThrow_whenDatabaseNameMissing() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant("table", "*", "*", null, List.of("SELECT"), true, true);

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () -> desiredGrantsCreator.containerGrants(playbookGrant, testRoleName));

    assertTrue(
        error.getMessage().contains("Database name not provided"),
        "Error message should indicate missing database name");
  }

  @Test
  void containerGrants_shouldReturnEmpty_whenExpandAllGrantsContainerNotFound() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant("view", "*", "*", "TEST_DB", List.of("SELECT"), false, true);

    // qualifiedAccountObjectName will return normalized name, but expandAllGrants checks existence
    when(mockSnowflakeObjectsService.objectExists("TEST_DB", SnowflakeObjectType.DATABASE))
        .thenReturn(false);

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.containerGrants(playbookGrant, testRoleName);

    assertTrue(result.isEmpty());
  }

  @Test
  void createFutureGrants_shouldReturnFutureGrants() {
    SnowflakeObjectType objectType = SnowflakeObjectType.TABLE;
    String containerName = "TEST_DB.TEST_SCHEMA";
    List<String> privileges = List.of("SELECT", "INSERT");
    Boolean grantOption = false;

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.createFutureGrants(
            objectType, containerName, privileges, testRoleName, grantOption);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(g -> g.future()));
    assertEquals("SELECT", result.get(0).privilege());
    assertEquals("INSERT", result.get(1).privilege());
  }

  @Test
  void
      createFutureSchemaObjectGrantsInAllSchemasInDatabase_shouldReturnGrants_whenDatabaseExists() {
    String databaseName = "TEST_DB";
    SnowflakeObjectType objectType = SnowflakeObjectType.TABLE;
    List<String> privileges = List.of("SELECT");
    Boolean grantOption = false;

    when(mockSnowflakeObjectsService.objectExists(databaseName, SnowflakeObjectType.DATABASE))
        .thenReturn(true);
    when(mockSnowflakeObjectsService.getContainerObjectQualNames(
            eq(SnowflakeObjectType.DATABASE), eq(SnowflakeObjectType.SCHEMA), eq(databaseName)))
        .thenReturn(List.of("TEST_DB.SCHEMA1", "TEST_DB.SCHEMA2"));

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.createFutureSchemaObjectGrantsInAllSchemasInDatabase(
            databaseName, objectType, privileges, testRoleName, grantOption);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(g -> g.future()));
  }

  @Test
  void
      createFutureSchemaObjectGrantsInAllSchemasInDatabase_shouldReturnEmpty_whenDatabaseNotExists() {
    String databaseName = "NON_EXISTENT_DB";
    SnowflakeObjectType objectType = SnowflakeObjectType.TABLE;
    List<String> privileges = List.of("SELECT");
    Boolean grantOption = false;

    when(mockSnowflakeObjectsService.objectExists(databaseName, SnowflakeObjectType.DATABASE))
        .thenReturn(false);

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.createFutureSchemaObjectGrantsInAllSchemasInDatabase(
            databaseName, objectType, privileges, testRoleName, grantOption);

    assertTrue(result.isEmpty());
  }

  @Test
  void expandAllGrants_shouldReturnGrants_whenContainerExists() {
    SnowflakeObjectType containerObjectType = SnowflakeObjectType.DATABASE;
    SnowflakeObjectType objectType = SnowflakeObjectType.TABLE;
    String containerName = "TEST_DB";
    List<String> privileges = List.of("SELECT", "UPDATE");
    Boolean grantOption = false;

    when(mockSnowflakeObjectsService.objectExists(containerName, containerObjectType))
        .thenReturn(true);
    when(mockSnowflakeObjectsService.getContainerObjectQualNames(
            eq(containerObjectType), eq(objectType), eq(containerName)))
        .thenReturn(List.of("TEST_DB.SCHEMA1.TABLE1", "TEST_DB.SCHEMA1.TABLE2"));

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.expandAllGrants(
            containerObjectType, objectType, containerName, privileges, testRoleName, grantOption);

    assertEquals(4, result.size()); // 2 objects * 2 privileges
    assertFalse(result.stream().anyMatch(g -> g.future()));
    assertFalse(result.stream().anyMatch(g -> g.all()));
  }

  @Test
  void expandAllGrants_shouldReturnEmpty_whenContainerNotExists() {
    SnowflakeObjectType containerObjectType = SnowflakeObjectType.DATABASE;
    SnowflakeObjectType objectType = SnowflakeObjectType.TABLE;
    String containerName = "NON_EXISTENT_DB";
    List<String> privileges = List.of("SELECT");
    Boolean grantOption = false;

    when(mockSnowflakeObjectsService.objectExists(containerName, containerObjectType))
        .thenReturn(false);

    List<SnowflakeGrantModel> result =
        desiredGrantsCreator.expandAllGrants(
            containerObjectType, objectType, containerName, privileges, testRoleName, grantOption);

    assertTrue(result.isEmpty());
  }

  @Test
  void standardGrants_shouldHandleRuntimeException() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "invalid_type", "OBJ", "SCHEMA", "DB", List.of("SELECT"), true, true);

    RuntimeException error =
        assertThrows(
            RuntimeException.class,
            () -> desiredGrantsCreator.standardGrants(playbookGrant, testRoleName));

    assertTrue(
        error.getMessage().contains("Unable to generate grants"),
        "Error message should indicate grant generation failure");
  }
}
