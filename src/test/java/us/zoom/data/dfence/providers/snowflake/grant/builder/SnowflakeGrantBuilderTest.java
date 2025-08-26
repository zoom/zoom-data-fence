package us.zoom.data.dfence.providers.snowflake.grant.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

class SnowflakeGrantBuilderTest {

  static Stream<GrantModelExpectedBuilder> fixtureGrants() {
    return Stream.of(
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT", "TABLE", "FOO.BAR.STAR", "ROLE", "MY_ROLE", false, false, false),
            SnowflakePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "\"foO\".\"bar-1@\".STAR",
                "ROLE",
                "MY_ROLE",
                false,
                false,
                false),
            SnowflakePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT", "TABLE", "\"foo\".BAR.STAR", "ROLE", "MY_ROLE", false, false, false),
            SnowflakePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "USAGE", "SCHEMA", "FOO.BAR", "ROLE", "MY_ROLE", false, false, false),
            SnowflakePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "USAGE", "DATABASE", "FOO", "ROLE", "MY_ROLE", false, false, false),
            SnowflakePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP", "SCHEMA", "FOO.BAR", "ROLE", "MY_ROLE", false, false, false),
            SnowflakeOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP", "DATABASE", "FOO", "ROLE", "MY_ROLE", false, false, false),
            SnowflakeOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP", "TABLE", "FOO.BAR.ZAR", "ROLE", "MY_ROLE", false, false, false),
            SnowflakeOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "EXTERNAL_TABLE",
                "FOO.BAR.ZAR",
                "ROLE",
                "MY_ROLE",
                false,
                false,
                false),
            SnowflakeOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT", "EXTERNAL_TABLE", "FOO.BAR.ZAR", "ROLE", "MY_ROLE", false, false, false),
            SnowflakePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT",
                "EXTERNAL_TABLE",
                "FOO.BAR.<EXTERNAL TABLE>",
                "ROLE",
                "MY_ROLE",
                false,
                true,
                false),
            SnowflakeFuturePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT",
                "EXTERNAL_TABLE",
                "FOO.<EXTERNAL TABLE>",
                "ROLE",
                "MY_ROLE",
                false,
                true,
                false),
            SnowflakeFuturePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP", "FILE_FORMAT", "FOO.BAR.ZAR", "ROLE", "MY_ROLE", true, false, false),
            SnowflakeOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "FILE_FORMAT",
                "FOO.BAR.<FILE_FORMAT>",
                "ROLE",
                "SYSADMIN",
                true,
                true,
                false),
            SnowflakeFutureOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "MATERIALIZED_VIEW",
                "FOO.BAR.<MATERIALIZED_VIEW>",
                "ROLE",
                "MY_ROLE",
                false,
                true,
                false),
            SnowflakeFutureOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "MATERIALIZED_VIEW",
                "FOO.BAR.ZAR",
                "ROLE",
                "MY_ROLE",
                false,
                false,
                false),
            SnowflakeOwnershipGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "APPLY PACKAGES POLICY",
                "ACCOUNT",
                "JUA64777",
                "ROLE",
                "SECURITYADMIN",
                true,
                false,
                false),
            SnowflakeAccountPermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT", "TABLE", "FOO.BAR.<TABLE>", "ROLE", "MY_ROLE", false, true, false),
            SnowflakeFuturePermissionGrantBuilder.class),
        new GrantModelExpectedBuilder(
            new SnowflakeGrantModel(
                "SELECT", "TABLE", "FOO.BAR.<TABLE>", "ROLE", "MY_ROLE", false, false, true),
            SnowflakeAllPermissionGrantBuilder.class));
  }

  static Stream<GrantRevokeStatementsParams> grantRevokeStatementsParamsStream() {
    return Stream.of(
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE_1",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MOCK_DB.MOCK_SCHEMA.\"mock_table_1\"",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"mock_table_1\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"mock_table_1\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MOCK_DB.MOCK_SCHEMA.\"MOCK_table_1?\"",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_table_1?\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_table_1?\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "EXTERNAL_TABLE",
                "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE_1",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT SELECT ON EXTERNAL TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON EXTERNAL TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MOCK_DB.MOCK_SCHEMA.<TABLE>",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                true,
                false),
            List.of(
                "GRANT SELECT ON FUTURE TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON FUTURE TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" FROM"
                    + " ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "\"Mock_db_1?.123\".MOCK_SCHEMA.<TABLE>",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                true,
                false),
            List.of(
                "GRANT SELECT ON FUTURE TABLES IN SCHEMA \"Mock_db_1?.123\".\"MOCK_SCHEMA\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON FUTURE TABLES IN SCHEMA \"Mock_db_1?.123\".\"MOCK_SCHEMA\" FROM"
                    + " ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "EXTERNAL_TABLE",
                "MOCK_DB.MOCK_SCHEMA.<EXTERNAL_TABLE>",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                true,
                false),
            List.of(
                "GRANT SELECT ON FUTURE EXTERNAL TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON FUTURE EXTERNAL TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE_1",
                "ROLE",
                "MOCK_ROLE_1",
                true,
                false,
                false),
            List.of(
                "GRANT SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" TO ROLE MOCK_ROLE_1 WITH GRANT OPTION;"),
            List.of(
                "REVOKE SELECT ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "CREATE DATABASE", "ACCOUNT", "", "ROLE", "MOCK_ROLE_1", false, false, false),
            List.of("GRANT CREATE DATABASE ON ACCOUNT TO ROLE MOCK_ROLE_1;"),
            List.of("REVOKE CREATE DATABASE ON ACCOUNT FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE_1",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT OWNERSHIP ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" TO ROLE MOCK_ROLE_1 COPY CURRENT GRANTS;"),
            List.of(
                "GRANT OWNERSHIP ON TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "EXTERNAL_TABLE",
                "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE_1",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT OWNERSHIP ON EXTERNAL TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" TO ROLE MOCK_ROLE_1 COPY CURRENT GRANTS;"),
            List.of(
                "GRANT OWNERSHIP ON EXTERNAL TABLE \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE_1\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "USAGE", "ROLE", "MOCK_ROLE_2", "ROLE", "MOCK_ROLE_1", false, false, false),
            List.of("GRANT ROLE \"MOCK_ROLE_2\" TO ROLE MOCK_ROLE_1;"),
            List.of("REVOKE ROLE \"MOCK_ROLE_2\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DB.MOCK_SCHEMA.<TABLE>",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                true,
                false),
            List.of(
                "GRANT OWNERSHIP ON FUTURE TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" TO ROLE MOCK_ROLE_1 COPY CURRENT GRANTS;"),
            List.of(
                "REVOKE OWNERSHIP ON FUTURE TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "EXTERNAL_TABLE",
                "MOCK_DB.MOCK_SCHEMA.<EXTERNAL_TABLE>",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                true,
                false),
            List.of(
                "GRANT OWNERSHIP ON FUTURE EXTERNAL TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" TO ROLE MOCK_ROLE_1 COPY CURRENT GRANTS;"),
            List.of(
                "REVOKE OWNERSHIP ON FUTURE EXTERNAL TABLES IN SCHEMA \"MOCK_DB\".\"MOCK_SCHEMA\" FROM ROLE MOCK_ROLE_1;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "OWNERSHIP",
                "SEMANTIC_VIEW",
                "MOCK_DB.MOCK_SCHEMA.MOCK_VIEW",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT OWNERSHIP ON SEMANTIC VIEW \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_VIEW\" TO ROLE MOCK_ROLE_1 COPY CURRENT GRANTS;"),
            List.of(
                "GRANT OWNERSHIP ON SEMANTIC VIEW \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_VIEW\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;")),
        new GrantRevokeStatementsParams(
            new SnowflakeGrantModel(
                "SELECT",
                "SEMANTIC_VIEW",
                "MOCK_DB.MOCK_SCHEMA.MOCK_VIEW",
                "ROLE",
                "MOCK_ROLE_1",
                false,
                false,
                false),
            List.of(
                "GRANT SELECT ON SEMANTIC VIEW \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_VIEW\" TO ROLE MOCK_ROLE_1;"),
            List.of(
                "REVOKE SELECT ON SEMANTIC VIEW \"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_VIEW\" FROM ROLE MOCK_ROLE_1;")));
  }

  public static Stream<PlaybookPrivilegeGrantTestParams> playbookPrivilegeGrantTestParamsStream() {
    return Stream.of(
        new PlaybookPrivilegeGrantTestParams(
            new SnowflakeGrantModel(
                "USAGE", "DATABASE", "MOCK_DB_A", "ROLE", "MOCK_ROLE_A", false, false, false),
            new PlaybookPrivilegeGrant(
                "database", null, null, "MOCK_DB_A", List.of("usage"), false, false)),
        new PlaybookPrivilegeGrantTestParams(
            new SnowflakeGrantModel(
                "USAGE", "SCHEMA", "MOCK_DB_A.<SCHEMA>", "ROLE", "MOCK_ROLE_A", false, true, false),
            new PlaybookPrivilegeGrant(
                "schema", null, "*", "MOCK_DB_A", List.of("usage"), true, false)),
        new PlaybookPrivilegeGrantTestParams(
            new SnowflakeGrantModel(
                "USAGE",
                "SCHEMA",
                "\"MOCK_DB_a\".<SCHEMA>",
                "ROLE",
                "MOCK_ROLE_A",
                false,
                true,
                false),
            new PlaybookPrivilegeGrant(
                "schema", null, "*", "\"MOCK_DB_a\"", List.of("usage"), true, false)),
        new PlaybookPrivilegeGrantTestParams(
            new SnowflakeGrantModel(
                "MONITOR", "ACCOUNT", "", "ROLE", "MOCK_ROLE_A", false, false, false),
            new PlaybookPrivilegeGrant(
                "account", null, null, null, List.of("monitor"), false, false)),
        new PlaybookPrivilegeGrantTestParams(
            new SnowflakeGrantModel(
                "USAGE", "ROLE", "MOCK_ROLE", "ROLE", "MOCK_ROLE_A", false, false, false),
            new PlaybookPrivilegeGrant(
                "role", "MOCK_ROLE", null, null, List.of("usage"), false, false)),
        new PlaybookPrivilegeGrantTestParams(
            new SnowflakeGrantModel(
                "OWNERSHIP", "USER", "MOCK_USER abc", "ROLE", "MOCK_ROLE_A", false, false, false),
            new PlaybookPrivilegeGrant(
                "user", "\"MOCK_USER abc\"", null, null, List.of("ownership"), false, false)));
  }

  @ParameterizedTest
  @MethodSource("fixtureGrants")
  void fromGrant(GrantModelExpectedBuilder params) throws RbacDataError {
    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(params.grant());
    assertInstanceOf(params.expected(), builder);
  }

  @Test
  void fromGrantNoGrantBuilderSuppressInvalidPrivilege() {
    SnowflakeGrantModel grantModel =
        new SnowflakeGrantModel(
            "INVALID PRIVILEGE", "TABLE", "FOO.BAR.ZAR", "ROLE", "MOCK_ROLE", false, false, false);
    SnowflakeGrantBuilder result = SnowflakeGrantBuilder.fromGrant(grantModel, true);
    assertNull(result);
  }

  @ParameterizedTest
  @MethodSource("grantRevokeStatementsParamsStream")
  void grantRevokeStatements(GrantRevokeStatementsParams params) {
    // This is not a true unit test. However, it buys a lot of cheap coverage.
    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(params.snowflakeGrantModel);
    List<String> grantStatements = builder.getGrantStatements();
    List<String> revokeStatements = builder.getRevokeStatements();
    assertEquals(params.grantStatementsExpected, grantStatements);
    assertEquals(params.revokeStatementsExpected, revokeStatements);
  }

  @ParameterizedTest
  @MethodSource("playbookPrivilegeGrantTestParamsStream")
  void playbookPrivilegeGrant(PlaybookPrivilegeGrantTestParams params) {
    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(params.snowflakeGrantModel);
    PlaybookPrivilegeGrant actual = builder.playbookPrivilegeGrant();
    assertEquals(params.expected, actual);
  }

  public record PlaybookPrivilegeGrantTestParams(
      SnowflakeGrantModel snowflakeGrantModel, PlaybookPrivilegeGrant expected) {}

  public record GrantModelExpectedBuilder(SnowflakeGrantModel grant, Class expected) {}

  public record GrantRevokeStatementsParams(
      SnowflakeGrantModel snowflakeGrantModel,
      List<String> grantStatementsExpected,
      List<String> revokeStatementsExpected) {}
}
