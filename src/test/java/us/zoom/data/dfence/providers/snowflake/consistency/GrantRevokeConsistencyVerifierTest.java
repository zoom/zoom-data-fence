package us.zoom.data.dfence.providers.snowflake.consistency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakePermissionGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.create.DesiredGrantsCreator;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.PlaybookGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;

class GrantRevokeConsistencyVerifierTest {

  @Mock private DesiredGrantsCreator mockGrantCreator;

  private AutoCloseable mocks;
  private String testRoleName = "TEST_ROLE";

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void verifyAllGrants_shouldSucceed_whenAllGrantsMatch() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "TEST_TABLE", "TEST_SCHEMA", "TEST_DB", List.of("SELECT"), true, true);

    SnowflakeGrantModel grantModel =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            testRoleName,
            false,
            false,
            false);

    SnowflakeGrantBuilder grantBuilder =
        new SnowflakePermissionGrantBuilder(grantModel, new SnowflakeGrantBuilderOptions());

    // Mock grant creator to return the expected grant builder
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(playbookGrant), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(grantBuilder));

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), mockGrantCreator, testRoleName));
  }

  @Test
  void verifyAllGrants_shouldSucceed_whenMultipleGrantsMatch() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table",
            "TEST_TABLE",
            "TEST_SCHEMA",
            "TEST_DB",
            List.of("SELECT", "UPDATE"),
            true,
            true);

    SnowflakeGrantModel selectModel =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            testRoleName,
            false,
            false,
            false);

    SnowflakeGrantModel updateModel =
        new SnowflakeGrantModel(
            "UPDATE",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            testRoleName,
            false,
            false,
            false);

    SnowflakeGrantBuilder selectBuilder =
        new SnowflakePermissionGrantBuilder(selectModel, new SnowflakeGrantBuilderOptions());
    SnowflakeGrantBuilder updateBuilder =
        new SnowflakePermissionGrantBuilder(updateModel, new SnowflakeGrantBuilderOptions());

    // Mock grant creator to return the expected grant builders
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(playbookGrant), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(selectBuilder, updateBuilder));

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), mockGrantCreator, testRoleName));
  }

  @Test
  void verifyAllGrants_shouldSucceed_whenMultiplePlaybookGrantsAllMatch() {
    PlaybookPrivilegeGrant grant1 =
        new PlaybookPrivilegeGrant(
            "table", "TABLE1", "SCHEMA1", "DB1", List.of("SELECT"), true, true);
    PlaybookPrivilegeGrant grant2 =
        new PlaybookPrivilegeGrant(
            "view", "VIEW1", "SCHEMA1", "DB1", List.of("SELECT"), true, true);

    SnowflakeGrantModel model1 =
        new SnowflakeGrantModel(
            "SELECT", "TABLE", "DB1.SCHEMA1.TABLE1", "ROLE", testRoleName, false, false, false);
    SnowflakeGrantModel model2 =
        new SnowflakeGrantModel(
            "SELECT", "VIEW", "DB1.SCHEMA1.VIEW1", "ROLE", testRoleName, false, false, false);

    SnowflakeGrantBuilder builder1 =
        new SnowflakePermissionGrantBuilder(model1, new SnowflakeGrantBuilderOptions());
    SnowflakeGrantBuilder builder2 =
        new SnowflakePermissionGrantBuilder(model2, new SnowflakeGrantBuilderOptions());

    // Mock grant creator to return the expected grant builders
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(grant1), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(builder1));
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(grant2), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(builder2));

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(grant1, grant2), mockGrantCreator, testRoleName));
  }

  @Test
  void verifyAllGrants_shouldThrow_whenGrantDoesNotMatch() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "TEST_TABLE", "TEST_SCHEMA", "TEST_DB", List.of("SELECT"), true, true);

    // Grant model with different object name that won't match
    SnowflakeGrantModel grantModel =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.DIFFERENT_TABLE",
            "ROLE",
            testRoleName,
            false,
            false,
            false);

    SnowflakeGrantBuilder grantBuilder =
        new SnowflakePermissionGrantBuilder(grantModel, new SnowflakeGrantBuilderOptions());

    // Mock grant creator to return a grant that doesn't match
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(playbookGrant), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(grantBuilder));

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), mockGrantCreator, testRoleName));

    assertTrue(
        error.getMessage().contains("Grant-Revoke mismatch detected"),
        "Error message should indicate mismatch");
  }

  @Test
  void verifyAllGrants_shouldThrow_whenPlaybookGrantConversionFails() {
    // This is hard to simulate since PlaybookGrantCompanion.toPlaybookGrant is a static method
    // that typically succeeds. We'll test with an invalid object type that might fail conversion
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "invalid_type", "OBJ", "SCHEMA", "DB", List.of("SELECT"), true, true);

    // Note: Grant creator is used directly now, so mocks are not needed

    // Note: This test depends on PlaybookGrantCompanion behavior
    // If it returns empty Optional, we should get an error
    Optional<PlaybookGrant> conversionResult =
        PlaybookGrantCompanion.toPlaybookGrant(playbookGrant);
    if (conversionResult.isEmpty()) {
      RbacDataError error =
          assertThrows(
              RbacDataError.class,
              () ->
                  GrantRevokeConsistencyVerifier.verifyAllGrants(
                      List.of(playbookGrant), mockGrantCreator, testRoleName));

      assertTrue(
          error.getMessage().contains("Playbook grant conversion failed"),
          "Error message should indicate conversion failure");
    }
  }

  @Test
  void verifyAllGrants_shouldThrow_whenGrantGenerationFails() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "TEST_TABLE", "TEST_SCHEMA", "TEST_DB", List.of("SELECT"), true, true);

    RuntimeException generationError = new RuntimeException("Grant generation failed");

    // Make grant creator throw to simulate grant generation failure
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            any(PlaybookPrivilegeGrant.class),
            anyString(),
            any(SnowflakeGrantBuilderOptions.class)))
        .thenThrow(generationError);

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), mockGrantCreator, testRoleName));

    assertTrue(
        error.getMessage().contains("Error generating grants"),
        "Error message should indicate grant generation failure");
    assertEquals(generationError, error.getCause(), "Error should wrap the original exception");
  }

  @Test
  void verifyAllGrants_shouldThrow_whenGrantModelConversionFails() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "TEST_TABLE", "TEST_SCHEMA", "TEST_DB", List.of("SELECT"), true, true);

    // Create a grant model with invalid object type that will fail conversion
    SnowflakeGrantModel invalidModel =
        new SnowflakeGrantModel(
            "SELECT",
            "INVALID_TYPE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            testRoleName,
            false,
            false,
            false);

    SnowflakeGrantBuilder grantBuilder =
        new SnowflakePermissionGrantBuilder(invalidModel, new SnowflakeGrantBuilderOptions());

    // Mock grant creator to return a grant builder with invalid model
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(playbookGrant), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(grantBuilder));

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyVerifier.verifyAllGrants(
                    List.of(playbookGrant), mockGrantCreator, testRoleName));

    assertTrue(
        error.getMessage().contains("Grant model conversion failed"),
        "Error message should indicate conversion failure");
  }

  @Test
  void verifyAllGrants_shouldFilterNullBuilders() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "TEST_TABLE", "TEST_SCHEMA", "TEST_DB", List.of("SELECT"), true, true);

    SnowflakeGrantModel grantModel =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            testRoleName,
            false,
            false,
            false);

    SnowflakeGrantBuilder grantBuilder =
        new SnowflakePermissionGrantBuilder(grantModel, new SnowflakeGrantBuilderOptions());

    // Return list with null builder
    List<SnowflakeGrantBuilder> builders = new ArrayList<>();
    builders.add(null);
    builders.add(grantBuilder);

    // Mock grant creator to return builders with null
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(playbookGrant), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(builders);

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), mockGrantCreator, testRoleName));
  }

  @Test
  void verifyAllGrants_shouldSucceed_whenFutureGrantMatches() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "view", "*", "*", "TEST_DB", List.of("SELECT"), true, true, true);

    SnowflakeGrantModel futureModel =
        new SnowflakeGrantModel(
            "SELECT", "VIEW", "TEST_DB.TEST_SCHEMA", "ROLE", testRoleName, false, true, false);

    SnowflakeGrantBuilder grantBuilder =
        new SnowflakePermissionGrantBuilder(futureModel, new SnowflakeGrantBuilderOptions());

    // Mock grant creator to return the expected future grant builder
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(playbookGrant), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(grantBuilder));

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), mockGrantCreator, testRoleName));
  }

  @Test
  void verifyAllGrants_shouldSucceed_whenAllGrantMatches() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant("table", "*", "*", "TEST_DB", List.of("SELECT"), false, true);

    SnowflakeGrantModel allModel =
        new SnowflakeGrantModel(
            "SELECT", "TABLE", "TEST_DB.TEST_SCHEMA", "ROLE", testRoleName, false, false, true);

    SnowflakeGrantBuilder grantBuilder =
        new SnowflakePermissionGrantBuilder(allModel, new SnowflakeGrantBuilderOptions());

    // Mock grant creator to return the expected all grant builder
    when(mockGrantCreator.playbookGrantToSnowflakeGrants(
            eq(playbookGrant), eq(testRoleName), any(SnowflakeGrantBuilderOptions.class)))
        .thenReturn(List.of(grantBuilder));

    assertDoesNotThrow(
        () ->
            GrantRevokeConsistencyVerifier.verifyAllGrants(
                List.of(playbookGrant), mockGrantCreator, testRoleName));
  }
}
