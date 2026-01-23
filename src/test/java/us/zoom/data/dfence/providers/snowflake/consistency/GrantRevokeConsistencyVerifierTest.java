package us.zoom.data.dfence.providers.snowflake.consistency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakePermissionGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.DesiredGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

@DisplayName("GrantRevokeConsistencyVerifier")
class GrantRevokeConsistencyVerifierTest {

  private DesiredGrantsProvider grantProvider;
  private SnowflakeObjectsService mockObjectsService;

  @BeforeEach
  void setUp() {
    mockObjectsService = mock(SnowflakeObjectsService.class);
    grantProvider = new DesiredGrantsProvider(mockObjectsService);
    when(mockObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class)))
        .thenReturn(true);
  }

  @Test
  @DisplayName("verifyAllGrants should succeed when all grants match")
  void verifyAllGrants_succeedsWhenGrantsMatch() {
    when(mockObjectsService.objectExists("\"MY_DB\"", SnowflakeObjectType.DATABASE))
        .thenReturn(true);
    when(mockObjectsService.objectExists("\"MY_DB\".\"MY_SCHEMA\"", SnowflakeObjectType.SCHEMA))
        .thenReturn(true);
    when(mockObjectsService.objectExists(
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"", SnowflakeObjectType.TABLE))
        .thenReturn(true);

    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), grantProvider, "ROLE1"));
  }

  @Test
  @DisplayName("verifyAllGrants should throw when grant conversion fails")
  void verifyAllGrants_throwsWhenConversionFails() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "invalid_type",
            "MY_TABLE",
            "MY_SCHEMA",
            "MY_DB",
            List.of("SELECT"),
            false,
            false,
            true);

    // When conversion fails, DesiredGrantsProvider throws RbacDataError
    // which gets wrapped by the verifier
    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), grantProvider, "ROLE1"));

    assertTrue(
        error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1")
            || error.getMessage().contains("Failed to convert playbook privilege grant")
            || error.getMessage().contains("Failed to convert playbook grant to Snowflake grants"),
        () -> "Error message should indicate conversion failure. Actual: " + error.getMessage());
  }

  @Test
  @DisplayName("verifyAllGrants should handle empty grant list")
  void verifyAllGrants_handlesEmptyList() {
    assertDoesNotThrow(
        () -> GrantRevokeConsistencyVerifier.verifyAllGrants(List.of(), grantProvider, "ROLE1"));
  }

  @Test
  @DisplayName("verifyAllGrants should throw when grant model has invalid object type")
  void verifyAllGrants_throwsWhenGrantModelConversionFails() {
    when(mockObjectsService.objectExists("\"MY_DB\"", SnowflakeObjectType.DATABASE))
        .thenReturn(true);
    when(mockObjectsService.objectExists("\"MY_DB\".\"MY_SCHEMA\"", SnowflakeObjectType.SCHEMA))
        .thenReturn(true);
    when(mockObjectsService.objectExists(
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"", SnowflakeObjectType.TABLE))
        .thenReturn(true);

    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    DesiredGrantsProvider mockGrantProvider = mock(DesiredGrantsProvider.class);
    SnowflakeGrantModel invalidGrantModel =
        new SnowflakeGrantModel(
            "SELECT",
            "INVALID_OBJECT_TYPE",
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"",
            "ROLE",
            "ROLE1",
            false,
            false,
            false);
    SnowflakeGrantBuilder invalidBuilder =
        new SnowflakePermissionGrantBuilder(invalidGrantModel, new SnowflakeGrantBuilderOptions());

    when(mockGrantProvider.playbookGrantToSnowflakeGrants(any(), anyString(), any()))
        .thenReturn(List.of(invalidBuilder));

    // Invalid grant models cause getKey() to throw when creating the map,
    // which gets wrapped in RbacDataError by the verifier
    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), mockGrantProvider, "ROLE1"));

    assertTrue(
        error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1"),
        () ->
            "Error message should indicate grant verification failure. Actual: "
                + error.getMessage());
  }

  @Test
  @DisplayName("verifyAllGrants should throw when grant does not match playbook grant")
  void verifyAllGrants_throwsWhenGrantDoesNotMatch() {
    when(mockObjectsService.objectExists("\"MY_DB\"", SnowflakeObjectType.DATABASE))
        .thenReturn(true);
    when(mockObjectsService.objectExists("\"MY_DB\".\"MY_SCHEMA\"", SnowflakeObjectType.SCHEMA))
        .thenReturn(true);
    when(mockObjectsService.objectExists(
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"", SnowflakeObjectType.TABLE))
        .thenReturn(true);

    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    DesiredGrantsProvider mockGrantProvider = mock(DesiredGrantsProvider.class);
    SnowflakeGrantModel mismatchedGrantModel =
        new SnowflakeGrantModel(
            "INSERT",
            "TABLE",
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"",
            "ROLE",
            "ROLE1",
            false,
            false,
            false);
    SnowflakeGrantBuilder mismatchedBuilder =
        new SnowflakePermissionGrantBuilder(
            mismatchedGrantModel, new SnowflakeGrantBuilderOptions());

    when(mockGrantProvider.playbookGrantToSnowflakeGrants(any(), anyString(), any()))
        .thenReturn(List.of(mismatchedBuilder));

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), mockGrantProvider, "ROLE1"));

    assertTrue(
        error.getMessage().contains("Inconsistency detected")
            || (error.getCause() != null
                && error.getCause().getMessage().contains("Inconsistency detected"))
            || error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1"),
        () ->
            "Error message should contain 'Inconsistency detected' or consistency check failure. Actual: "
                + error.getMessage());
  }

  @Test
  @DisplayName("verifyAllGrants should filter null grant builders")
  void verifyAllGrants_filtersNullGrantBuilders() {
    when(mockObjectsService.objectExists("\"MY_DB\"", SnowflakeObjectType.DATABASE))
        .thenReturn(true);
    when(mockObjectsService.objectExists("\"MY_DB\".\"MY_SCHEMA\"", SnowflakeObjectType.SCHEMA))
        .thenReturn(true);
    when(mockObjectsService.objectExists(
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"", SnowflakeObjectType.TABLE))
        .thenReturn(true);

    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    DesiredGrantsProvider mockGrantProvider = mock(DesiredGrantsProvider.class);
    SnowflakeGrantModel validGrantModel =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"",
            "ROLE",
            "ROLE1",
            false,
            false,
            false);
    SnowflakeGrantBuilder validBuilder =
        new SnowflakePermissionGrantBuilder(validGrantModel, new SnowflakeGrantBuilderOptions());

    List<SnowflakeGrantBuilder> buildersWithNull = new ArrayList<>();
    buildersWithNull.add(validBuilder);
    buildersWithNull.add(null);

    when(mockGrantProvider.playbookGrantToSnowflakeGrants(any(), anyString(), any()))
        .thenReturn(buildersWithNull);

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), mockGrantProvider, "ROLE1"));
  }

  @Test
  @DisplayName("verifyAllGrants should wrap generic exceptions")
  void verifyAllGrants_wrapsGenericExceptions() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    DesiredGrantsProvider mockGrantProvider = mock(DesiredGrantsProvider.class);
    when(mockGrantProvider.playbookGrantToSnowflakeGrants(any(), anyString(), any()))
        .thenThrow(new RuntimeException("Unexpected error"));

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), mockGrantProvider, "ROLE1"));

    assertTrue(
        error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1")
            || error.getMessage().contains("Grant verification failed for role ROLE1"));
    assertTrue(
        error.getMessage().contains("Unexpected error")
            || (error.getCause() != null
                && error.getCause().getMessage().contains("Unexpected error")));
  }

  @Test
  @DisplayName("verifyAllGrants should verify no revoke grants are generated for playbook grants")
  void verifyAllGrants_verifiesNoRevokeGrantsGenerated() {
    when(mockObjectsService.objectExists("\"MY_DB\"", SnowflakeObjectType.DATABASE))
        .thenReturn(true);
    when(mockObjectsService.objectExists("\"MY_DB\".\"MY_SCHEMA\"", SnowflakeObjectType.SCHEMA))
        .thenReturn(true);
    when(mockObjectsService.objectExists(
            "\"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"", SnowflakeObjectType.TABLE))
        .thenReturn(true);

    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), grantProvider, "ROLE1"),
        "Should not throw when generated grants match playbook grants and won't be revoked");
  }

  @Test
  @DisplayName("verifyAllGrants should throw when generated grant would be revoked")
  void verifyAllGrants_throwsWhenGeneratedGrantWouldBeRevoked() {
    when(mockObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class)))
        .thenReturn(true);

    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    DesiredGrantsProvider mockGrantProvider = mock(DesiredGrantsProvider.class);
    SnowflakeGrantModel mismatchedGrantModel =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "\"MY_DB\".\"MY_SCHEMA\".\"OTHER_TABLE\"",
            "ROLE",
            "ROLE1",
            false,
            false,
            false);
    SnowflakeGrantBuilder mismatchedBuilder =
        new SnowflakePermissionGrantBuilder(
            mismatchedGrantModel, new SnowflakeGrantBuilderOptions());

    when(mockGrantProvider.playbookGrantToSnowflakeGrants(any(), anyString(), any()))
        .thenReturn(List.of(mismatchedBuilder));

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), mockGrantProvider, "ROLE1"),
            "Should throw when generated grant would be revoked");

    assertTrue(
        error.getMessage().contains("Inconsistency detected")
            || (error.getCause() != null
                && error.getCause().getMessage().contains("Inconsistency detected"))
            || error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1"),
        () ->
            "Error message should contain 'Inconsistency detected' or consistency check failure. Actual: "
                + error.getMessage());
  }
}
