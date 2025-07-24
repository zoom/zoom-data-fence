package us.zoom.data.dfence.providers.snowflake;

import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.data.dfence.providers.snowflake.grant.builder.GrantBuilderDiff;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeOwnedObjectFilterTest {

    @Test
    void keepFalseScaler() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                "mock_table",
                SnowflakeObjectType.TABLE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DATABASE.MOCK_SCHEMA.MOCK_TABLE",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueFromTroubleshooting() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "MOCK_DB_NAME",
                "MOCK_SCHEMA_NAME",
                "MOCK_TABLE_NAME",
                SnowflakeObjectType.TABLE,
                "ROLE_B");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.OTHER_TABLE_NAME",
                "ROLE",
                "ROLE_A",
                false,
                false,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepFalseDatabase() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                null,
                null,
                SnowflakeObjectType.DATABASE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "DATABASE",
                "MOCK_DATABASE",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepFalseSchema() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                null,
                SnowflakeObjectType.SCHEMA,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "SCHEMA",
                "MOCK_DATABASE.MOCK_SCHEMA",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepFalseSchemaWildcard() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "*",
                null,
                SnowflakeObjectType.SCHEMA,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "SCHEMA",
                "MOCK_DATABASE.MOCK_SCHEMA",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueSchemaSameRole() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                null,
                SnowflakeObjectType.SCHEMA,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "SCHEMA",
                "MOCK_DATABASE.MOCK_SCHEMA",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueDatabaseSameRole() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                null,
                null,
                SnowflakeObjectType.DATABASE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "DATABASE",
                "MOCK_DATABASE",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepFalseUserOtherRole() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                null,
                null,
                "mock_user",
                SnowflakeObjectType.USER,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "USER",
                "MOCK_USER",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueUserSameRole() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                null,
                null,
                "mock_user",
                SnowflakeObjectType.USER,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "USER",
                "MOCK_USER",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueAccount() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                "mock_table",
                SnowflakeObjectType.TABLE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "CREATE ROLE",
                "ACCOUNT",
                "abc123",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueFutureSchema() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                "mock_table",
                SnowflakeObjectType.TABLE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DATABASE.MOCK_SCHEMA.<TABLE>",
                "ROLE",
                "OTHER_ROLE",
                false,
                true,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueScalerSameRole() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                "mock_table",
                SnowflakeObjectType.TABLE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DATABASE.MOCK_SCHEMA.MOCK_TABLE",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepTrueScalerOtherObjectType() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                "mock_table",
                SnowflakeObjectType.VIEW,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DATABASE.MOCK_SCHEMA.MOCK_TABLE",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertTrue(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepFalseContainerSchema() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "mock_schema",
                "*",
                SnowflakeObjectType.TABLE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DATABASE.MOCK_SCHEMA.MOCK_TABLE",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepFalseContainerDatabase() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "mock_database",
                "*",
                "*",
                SnowflakeObjectType.TABLE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DATABASE.MOCK_SCHEMA.MOCK_TABLE",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void keepFalseScalerUnconventionalNames() {
        SnowflakeOwnedObjectFilter snowflakeOwnedObjectFilter = new SnowflakeOwnedObjectFilter(
                "\"mock_database!!$$\"",
                "\"mock_schema!!$$\"",
                "\"mock_table!!$$\"",
                SnowflakeObjectType.TABLE,
                "mock_role");
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "\"mock_database!!$$\".\"mock_schema!!$$\".\"mock_table!!$$\"",
                "ROLE",
                "OTHER_ROLE",
                false,
                false,
                false);
        assertFalse(snowflakeOwnedObjectFilter.keep(snowflakeGrantModel));
    }

    @Test
    void filterDiff() {
        GrantBuilderDiff grantBuilderDiff = new GrantBuilderDiff(
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "OWNERSHIP",
                        "TABLE",
                        "MOCK_DB_1.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE_1",
                        false,
                        false,
                        false))), List.of(
                SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "OWNERSHIP",
                        "TABLE",
                        "MOCK_DB_2.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE_1",
                        false,
                        false,
                        false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB_2.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE_1",
                        false,
                        false,
                        false))));
        GrantBuilderDiff grantBuilderDiffExpected = new GrantBuilderDiff(
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "OWNERSHIP",
                        "TABLE",
                        "MOCK_DB_1.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE_1",
                        false,
                        false,
                        false))),
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB_2.MOCK_SCHEMA.MOCK_TABLE",
                        "ROLE",
                        "MOCK_ROLE_1",
                        false,
                        false,
                        false))));
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "mock_role_1",
                new PlaybookRoleModel(
                        "MOCK_ROLE_1",
                        List.of(new PlaybookPrivilegeGrant(
                                "table",
                                "mock_table",
                                "mock_schema",
                                "mock_db_1",
                                List.of("ownership"),
                                true,
                                true))),
                "mock_role_2",
                new PlaybookRoleModel(
                        "MOCK_ROLE_2", List.of(new PlaybookPrivilegeGrant(
                        "table",
                        "mock_table",
                        "mock_schema",
                        "mock_db_2",
                        List.of("ownership", "select"),
                        true,
                        true)))));
        GrantBuilderDiff grantBuilderDiffActual = SnowflakeOwnedObjectFilter.filterDiff(
                grantBuilderDiff,
                playbookModel);
        assertEquals(grantBuilderDiffExpected, grantBuilderDiffActual);
    }

    @Test
    void filterDiffOther() {
        GrantBuilderDiff grantBuilderDiff = new GrantBuilderDiff(
                List.of(), List.of(
                SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "OWNERSHIP",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                        "ROLE",
                        "ROLE_A",
                        false,
                        false,
                        false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "OWNERSHIP",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.OTHER_TABLE_NAME",
                        "ROLE",
                        "ROLE_A",
                        false,
                        false,
                        false))));
        GrantBuilderDiff grantBuilderDiffExpected = new GrantBuilderDiff(
                List.of(), List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.OTHER_TABLE_NAME",
                "ROLE",
                "ROLE_A",
                false,
                false,
                false))));
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a", new PlaybookRoleModel("role_a", List.of()), "role-b", new PlaybookRoleModel(
                        "role_b", List.of(new PlaybookPrivilegeGrant(
                        "table",
                        "MOCK_TABLE_NAME",
                        "MOCK_SCHEMA_NAME",
                        "MOCK_DB_NAME",
                        List.of("OWNERSHIP"),
                        true,
                        true)))));
        GrantBuilderDiff grantBuilderDiffActual = SnowflakeOwnedObjectFilter.filterDiff(
                grantBuilderDiff,
                playbookModel);
        assertEquals(grantBuilderDiffExpected, grantBuilderDiffActual);
    }
}