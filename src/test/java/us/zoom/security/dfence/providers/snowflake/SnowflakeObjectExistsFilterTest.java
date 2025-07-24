package us.zoom.security.dfence.providers.snowflake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.security.dfence.playbook.model.PlaybookModel;
import us.zoom.security.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.security.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.security.dfence.providers.snowflake.grant.builder.GrantBuilderDiff;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.security.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SnowflakeObjectExistsFilterTest {

    @Mock
    SnowflakeObjectsService snowflakeObjectsService;
    AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void objectExistsGrantBuilderDiffFilter() {
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB.MOCK_SCHEMA.TABLE_EXISTS",
                SnowflakeObjectType.TABLE)).thenReturn(true);
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB.MOCK_SCHEMA.TABLE_NOT_EXISTS",
                SnowflakeObjectType.TABLE)).thenReturn(false);

        GrantBuilderDiff grantBuilderDiffIn = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DB.MOCK_SCHEMA.TABLE_EXISTS",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DB.MOCK_SCHEMA.TABLE_NOT_EXISTS",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false))), List.of(
                SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA.TABLE_EXISTS",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA.TABLE_NOT_EXISTS",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        false))));
        GrantBuilderDiff grantBuilderDiffExpected = new GrantBuilderDiff(
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA.TABLE_EXISTS",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        false))),
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA.TABLE_EXISTS",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        false,
                        false))));
        GrantBuilderDiff grantBuilderDiffActual = SnowflakeObjectExistsFilter.objectExistsGrantBuilderDiffFilter(grantBuilderDiffIn,
                snowflakeObjectsService,
                new PlaybookModel(Map.of()));
        assertEquals(grantBuilderDiffExpected, grantBuilderDiffActual);
    }

    @Test
    void objectExistsGrantBuilderDiffFilterFutureSchema() {
        when(snowflakeObjectsService.objectExists("MOCK_DB.MOCK_SCHEMA_EXISTS", SnowflakeObjectType.SCHEMA)).thenReturn(
                true);
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB.MOCK_SCHEMA_NOT_EXISTS",
                SnowflakeObjectType.SCHEMA)).thenReturn(false);

        GrantBuilderDiff grantBuilderDiffIn = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DB.MOCK_SCHEMA_EXISTS.<TABLE>",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                true,
                                false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DB.MOCK_SCHEMA_NOT_EXISTS.<TABLE>",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                true,
                                false))), List.of(
                SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false)), SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB.SCHEMA_NOT_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false))));
        GrantBuilderDiff grantBuilderDiffExpected = new GrantBuilderDiff(
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false))),
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB.MOCK_SCHEMA_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false))));
        GrantBuilderDiff grantBuilderDiffActual = SnowflakeObjectExistsFilter.objectExistsGrantBuilderDiffFilter(grantBuilderDiffIn,
                snowflakeObjectsService,
                new PlaybookModel(Map.of()));
        assertEquals(grantBuilderDiffExpected, grantBuilderDiffActual);
    }

    @Test
    void objectExistsGrantBuilderDiffFilterFutureDatabase() {
        when(snowflakeObjectsService.objectExists("MOCK_DB_EXISTS", SnowflakeObjectType.DATABASE)).thenReturn(true);
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB_NOT_EXISTS",
                SnowflakeObjectType.DATABASE)).thenReturn(false);

        GrantBuilderDiff grantBuilderDiffIn = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DB_EXISTS.<TABLE>",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                true,
                                false)),
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "SELECT",
                                "TABLE",
                                "MOCK_DB_NOT_EXISTS.<TABLE>",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                true,
                                false))), List.of(
                SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false)),
                SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB_NOT_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false))));
        GrantBuilderDiff grantBuilderDiffExpected = new GrantBuilderDiff(
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false))),
                List.of(SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                        "DELETE",
                        "TABLE",
                        "MOCK_DB_EXISTS.<TABLE>",
                        "ROLE",
                        "MOCK_ROLE",
                        false,
                        true,
                        false))));
        GrantBuilderDiff grantBuilderDiffActual = SnowflakeObjectExistsFilter.objectExistsGrantBuilderDiffFilter(grantBuilderDiffIn,
                snowflakeObjectsService,
                new PlaybookModel(Map.of()));
        assertEquals(grantBuilderDiffExpected, grantBuilderDiffActual);
    }

    /*
    Make sure that we assume that roles that we plan to create will exist.
     */
    @Test
    void objectExistsGrantBuilderDiffFilterPlannedRoles() {
        when(snowflakeObjectsService.objectExists("MOCK_ROLE_EXISTS_GRANT", SnowflakeObjectType.ROLE)).thenReturn(true);
        when(snowflakeObjectsService.objectExists(
                "MOCK_ROLE_EXISTS_REVOKE",
                SnowflakeObjectType.ROLE)).thenReturn(true);
        when(snowflakeObjectsService.objectExists("MOCK_ROLE_NOT_EXIST", SnowflakeObjectType.TABLE)).thenReturn(false);
        when(snowflakeObjectsService.objectExists("MOCK_ROLE_WILL_EXIST", SnowflakeObjectType.TABLE)).thenReturn(false);
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "mock-role-exists", new PlaybookRoleModel("MOCK_ROLE_WILL_EXIST", List.of()),

                "mock-role", new PlaybookRoleModel(
                        "MOCK_ROLE", List.of(
                        new PlaybookPrivilegeGrant(
                                "ROLE",
                                "MOCK_ROLE_WILL_EXIST",
                                null,
                                null,
                                List.of("USAGE"),
                                false,
                                false),
                        new PlaybookPrivilegeGrant(
                                "ROLE",
                                "MOCK_ROLE_EXIST_EXISTS_GRANT",
                                null,
                                null,
                                List.of("USAGE"),
                                false,
                                false)))));

        GrantBuilderDiff grantBuilderDiffIn = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_EXISTS_GRANT",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)),
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_NOT_EXIST",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)),
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_WILL_EXIST",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false))),
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_EXISTS_REVOKE",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)),
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_NOT_EXIST",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)),
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_WILL_EXIST",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false))

                ));
        GrantBuilderDiff grantBuilderDiffExpected = new GrantBuilderDiff(
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_EXISTS_GRANT",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)),
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_WILL_EXIST",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false))),
                List.of(
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_EXISTS_REVOKE",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false)),
                        SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                                "USAGE",
                                "ROLE",
                                "MOCK_ROLE_WILL_EXIST",
                                "ROLE",
                                "MOCK_ROLE",
                                false,
                                false,
                                false))

                ));
        GrantBuilderDiff grantBuilderDiffActual = SnowflakeObjectExistsFilter.objectExistsGrantBuilderDiffFilter(grantBuilderDiffIn,
                snowflakeObjectsService,
                playbookModel);
        assertEquals(grantBuilderDiffExpected, grantBuilderDiffActual);
    }
}