package us.zoom.data.dfence.consistency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakePermissionGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.DesiredGrantsCompiler;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GrantRevokeConsistencyChecker")
class GrantRevokeConsistencyCheckerTest {

  private static Map<String, SnowflakeGrantBuilder> compileDesiredGrantBuilders(
      List<PlaybookPrivilegeGrant> playbookGrants,
      DesiredGrantsCompiler desiredGrantsCompiler,
      String roleName) {
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    return playbookGrants.stream()
        .flatMap(g -> desiredGrantsCompiler.createFrom(g, roleName, options).stream())
        .collect(Collectors.toMap(SnowflakeGrantBuilder::getKey, x -> x, (x0, x1) -> x0));
  }

  @Test
  @DisplayName("check should succeed when desired grants match playbook grants")
  void check_succeedsWhenDesiredGrantsMatchPlaybook() {
    SnowflakeObjectsService objectsService = mock(SnowflakeObjectsService.class);
    when(objectsService.objectExists(anyString(), any(SnowflakeObjectType.class))).thenReturn(true);
    DesiredGrantsCompiler desiredGrantsCompiler = new DesiredGrantsCompiler(objectsService);

    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);
    List<PlaybookPrivilegeGrant> playbookGrants = List.of(playbookGrant);

    Map<String, SnowflakeGrantBuilder> desiredGrantBuilders =
        compileDesiredGrantBuilders(playbookGrants, desiredGrantsCompiler, "ROLE1");

    assertDoesNotThrow(
        () -> GrantRevokeConsistencyChecker.check(playbookGrants, desiredGrantBuilders, "ROLE1"));
  }

  @Test
  @DisplayName("check should succeed with empty inputs")
  void check_succeedsWithEmptyInputs() {
    assertDoesNotThrow(() -> GrantRevokeConsistencyChecker.check(List.of(), Map.of(), "ROLE1"));
  }

  @Test
  @DisplayName("check should throw when playbook grant cannot be indexed")
  void check_throwsWhenPlaybookGrantInvalid() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "invalid_type", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () -> GrantRevokeConsistencyChecker.check(List.of(playbookGrant), Map.of(), "ROLE1"));

    assertTrue(
        error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1"),
        () -> "Expected wrapper error message. Actual: " + error.getMessage());
  }

  @Test
  @DisplayName("check should throw when desired grant would be revoked")
  void check_throwsWhenDesiredGrantWouldBeRevoked() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

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
        new SnowflakePermissionGrantBuilder(mismatchedGrantModel, new SnowflakeGrantBuilderOptions());

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyChecker.check(
                    List.of(playbookGrant), Map.of("k", mismatchedBuilder), "ROLE1"));

    assertTrue(
        error.getMessage().contains("Inconsistency detected")
            || (error.getCause() != null && error.getCause().getMessage().contains("Inconsistency detected"))
            || error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1"),
        () -> "Expected inconsistency or wrapper error. Actual: " + error.getMessage());
  }

  @Test
  @DisplayName("check should wrap errors when desired grant model cannot be evaluated")
  void check_wrapsWhenDesiredGrantModelInvalid() {
    PlaybookPrivilegeGrant playbookGrant =
        new PlaybookPrivilegeGrant(
            "table", "MY_TABLE", "MY_SCHEMA", "MY_DB", List.of("SELECT"), false, false, true);

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

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () ->
                GrantRevokeConsistencyChecker.check(
                    List.of(playbookGrant), Map.of("k", invalidBuilder), "ROLE1"));

    assertTrue(
        error.getMessage().contains("Grant-revoke consistency check failed for role ROLE1"),
        () -> "Expected wrapper error message. Actual: " + error.getMessage());
  }
}

