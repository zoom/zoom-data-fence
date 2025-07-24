package us.zoom.security.dfence.providers.snowflake;

import org.junit.jupiter.api.Test;
import us.zoom.security.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.security.dfence.providers.snowflake.grant.builder.GrantBuilderDiff;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnowflakeWildcardAllGrantFilterTest {

    @Test
    void containerAllGrantBuilders() {
        PlaybookPrivilegeGrant playbookPrivilegeGrant = new PlaybookPrivilegeGrant(
                "TABLE",
                "*",
                "MOCK_SCHEMA",
                "MOCK_DATABASE",
                List.of("SELECT", "UPDATE", "DELETE"),
                true,
                true,
                true);
        String roleName = "MOCK_ROLE";
        List<SnowflakeGrantBuilder> expected = playbookPrivilegeGrant.privileges().stream()
                .map(privilege -> SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        privilege,
                        "TABLE",
                        "MOCK_DATABASE.MOCK_SCHEMA.<TABLE>",
                        "ROLE",
                        roleName,
                        false,
                        false,
                        true))).toList();
        List<SnowflakeGrantBuilder> actual = SnowflakeWildcardAllGrantFilter.containerAllGrantBuilders(playbookPrivilegeGrant,
                roleName);
        assertEquals(actual, expected);
    }

    @Test
    void consolidateWildcardAllGrantBuilders() {
        List<PlaybookPrivilegeGrant> playbookPrivilegeGrants = List.of(
                new PlaybookPrivilegeGrant(
                        "table",
                        "*",
                        "mock_schema",
                        "mock_database_1",
                        List.of("SELECT"),
                        true,
                        true,
                        true),
                new PlaybookPrivilegeGrant(
                        "table",
                        "mock_database_2",
                        "mock_schema",
                        "mock_table_name_1",
                        List.of("SELECT"),
                        true,
                        true,
                        true));
        GrantBuilderDiff grantBuilderDiff = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DATABASE_1.MOCK_SCHEMA.MOCK_TABLE_NAME_1",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DATABASE_1.MOCK_SCHEMA.MOCK_TABLE_NAME_2",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DATABASE_2.MOCK_SCHEMA.MOCK_TABLE_NAME_1",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false))), List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MOCK_DATABASE_3.MOCK_SCHEMA.MOCK_TABLE_NAME_1",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false))));
        GrantBuilderDiff expected = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DATABASE_2.MOCK_SCHEMA.MOCK_TABLE_NAME_1",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DATABASE_1.MOCK_SCHEMA.<TABLE>",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                true))), grantBuilderDiff.revoke());
        GrantBuilderDiff actual = SnowflakeWildcardAllGrantFilter.consolidateWildcardAllGrantBuilders(
                grantBuilderDiff,
                playbookPrivilegeGrants,
                "MOCK_ROLE");
        assertEquals(expected, actual);
    }

    @Test
    void consolidateWildcardAllGrantBuildersOnlyChangesInDiff() {
        List<PlaybookPrivilegeGrant> playbookPrivilegeGrants = List.of(
                new PlaybookPrivilegeGrant(
                        "table",
                        "*",
                        "mock_schema",
                        "mock_database_1",
                        List.of("SELECT"),
                        false,
                        true,
                        true),
                new PlaybookPrivilegeGrant(
                        "table",
                        "*",
                        "mock_schema",
                        "mock_database_2",
                        List.of("SELECT"),
                        false,
                        true,
                        true));
        GrantBuilderDiff grantBuilderDiff = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DATABASE_1.MOCK_SCHEMA.MOCK_TABLE_NAME_1",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DATABASE_1.MOCK_SCHEMA.MOCK_TABLE_NAME_2",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false))), List.of());
        GrantBuilderDiff expected = new GrantBuilderDiff(
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DATABASE_1.MOCK_SCHEMA.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        true))), List.of());
        GrantBuilderDiff actual = SnowflakeWildcardAllGrantFilter.consolidateWildcardAllGrantBuilders(
                grantBuilderDiff,
                playbookPrivilegeGrants,
                "MOCK_ROLE");
        assertEquals(expected, actual);
    }
}