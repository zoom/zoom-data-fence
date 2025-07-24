package us.zoom.security.dfence.providers.snowflake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.security.dfence.CompiledChanges;
import us.zoom.security.dfence.playbook.model.PlaybookModel;
import us.zoom.security.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.security.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeApplicationRoleGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakePermissionGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


class SnowflakeProviderTest {

    @Mock
    SnowflakeGrantsService snowflakeGrantsService;

    @Mock
    SnowflakeStatementsService snowflakeStatementsService;

    @Mock
    SnowflakeObjectsService snowflakeObjectsService;

    SnowflakeProvider snowflakeProvider;

    AutoCloseable mocks;

    List<String> databasesInAccount = List.of("MOCK_DB_NAME");

    String databaseName = "MOCK_DB_NAME";
    List<String> schemasInDatabase = List.of("MOCK_DB_NAME.MOCK_SCHEMA_NAME");

    String schemaName = "MOCK_SCHEMA_NAME";
    List<String> tablesInSchema = List.of(
            "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
            "MOCK_DB_NAME.MOCK_SCHEMA_NAME.\"mock_table_name_2\"");

    String tableName = "MOCK_TABLE_NAME";

    public static Stream<CompileRoleChangesTestParams> compileRoleChangesTestParamsStream() {
        List<PlaybookPrivilegeGrant> playbookPrivilegeGrants = List.of(
                new PlaybookPrivilegeGrant(
                        "table",
                        "\"mock_table_name_2\"",
                        "mock_schema_name",
                        "mock_db_name",
                        List.of("select"),
                        true,
                        true),
                new PlaybookPrivilegeGrant(
                        "table",
                        "mock_table_name",
                        "mock_schema_name",
                        "mock_db_name",
                        List.of("select", "update"),
                        true,
                        true),
                new PlaybookPrivilegeGrant("database", null, null, "mock_db_name", List.of("usage"), true, true),
                new PlaybookPrivilegeGrant("view", "*", "*", "mock_db_name", List.of("select"), true, true),
                new PlaybookPrivilegeGrant("account", null, null, null, List.of("monitor"), false, false));
        String roleName = "mock_role_name";
        String roleId = "mock-role-id";
        List<List<String>> expectedRoleGrantStatements = List.of(
                List.of("GRANT MONITOR ON ACCOUNT TO ROLE MOCK_ROLE_NAME;"),
                List.of("GRANT SELECT ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"mock_table_name_2\" TO ROLE MOCK_ROLE_NAME;"),
                List.of("GRANT SELECT ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE MOCK_ROLE_NAME;"),
                List.of("GRANT SELECT ON FUTURE VIEWS IN DATABASE \"MOCK_DB_NAME\" TO ROLE MOCK_ROLE_NAME;"),
                List.of("GRANT SELECT ON FUTURE VIEWS IN SCHEMA \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\" TO ROLE MOCK_ROLE_NAME;"),
                List.of("GRANT UPDATE ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE MOCK_ROLE_NAME;"),
                List.of("GRANT USAGE ON DATABASE \"MOCK_DB_NAME\" TO ROLE MOCK_ROLE_NAME;"));
        List<String> expectedRoleCreationStatements = List.of("CREATE ROLE IF NOT EXISTS MOCK_ROLE_NAME;");
        SnowflakeGrantBuilder extraGrant = new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                "create table",
                "SCHEMA",
                "OTHER_DB.OTHER_SCHEMA",
                "ROLE",
                roleName,
                false,
                false,
                false));
        return Stream.of(
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants),
                        roleId,
                        false,
                        List.of("SOME_OTHER_ROLE"),
                        new HashMap<>(),
                        new CompiledChanges(
                                roleId,
                                roleName,
                                expectedRoleCreationStatements,
                                expectedRoleGrantStatements)),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants),
                        roleId,
                        true,
                        List.of("SOME_OTHER_ROLE", roleName.toUpperCase()),
                        new HashMap<>(),
                        new CompiledChanges(roleId, roleName, List.of(), expectedRoleGrantStatements)),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants),
                        roleId,
                        true,
                        List.of("SOME_OTHER_ROLE", roleName.toUpperCase()),
                        Map.of(extraGrant.getKey(), extraGrant),
                        new CompiledChanges(
                                roleId, roleName, List.of(), new ArrayList<>() {{
                            add(List.of(
                                    "REVOKE CREATE TABLE ON SCHEMA \"OTHER_DB\".\"OTHER_SCHEMA\" FROM ROLE MOCK_ROLE_NAME;"));
                            addAll(expectedRoleGrantStatements);
                        }})),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants, true, false, true, null),
                        roleId,
                        true,
                        List.of("SOME_OTHER_ROLE", roleName.toUpperCase()),
                        Map.of(extraGrant.getKey(), extraGrant),
                        new CompiledChanges(
                                roleId, roleName, List.of(), new ArrayList<>() {{
                            addAll(expectedRoleGrantStatements);
                        }})),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(
                                roleName, new ArrayList<>() {{
                            add(new PlaybookPrivilegeGrant(
                                    "table",
                                    "table_that_does_not_exist",
                                    "schema_that_does_not_exist",
                                    "db_that_does_not_exist",
                                    List.of("select"),
                                    true,
                                    true));
                            addAll(playbookPrivilegeGrants);
                        }}),
                        roleId,
                        true,
                        List.of(roleName.toUpperCase()),
                        Map.of(),
                        new CompiledChanges(roleId, roleName, List.of(), expectedRoleGrantStatements)),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants, false, true, true, null),
                        roleId,
                        false,
                        List.of("SOME_OTHER_ROLE"),
                        new HashMap<>(),
                        new CompiledChanges(roleId, roleName, List.of(), List.of())));
    }

    public static Stream<CompilePlaybookPrivilegeGrantsTestParams> compilePlaybookPrivilegeGrantsTestParamsStream() {
        return compileRoleChangesTestParamsStream().filter(p -> p.playbookRoleModel().create() || p.roleExists)
                .map(p -> new CompilePlaybookPrivilegeGrantsTestParams(
                        p.playbookRoleModel.grants(),
                        p.playbookRoleModel.name(),
                        p.roleExists,
                        p.mockGetGrantsResult,
                        p.expected.roleGrantStatements(),
                        p.playbookRoleModel.revokeOtherGrants()));
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        // Create a new SnowflakeProvider with the mock services and a ForkJoinPool
        snowflakeProvider = new SnowflakeProvider(snowflakeStatementsService, snowflakeGrantsService, snowflakeObjectsService, ForkJoinPool.commonPool());
        snowflakeObjectsService.clearCache();
        when(snowflakeObjectsService.objectExists(
                anyString(),
                eq(SnowflakeObjectType.DATABASE))).thenAnswer(invocation -> databasesInAccount.contains(invocation.getArgument(
                0)));
        when(snowflakeObjectsService.objectExists(
                anyString(),
                eq(SnowflakeObjectType.SCHEMA))).thenAnswer(invocation -> schemasInDatabase.contains(invocation.getArgument(
                0)));
        when(snowflakeObjectsService.objectExists(
                anyString(),
                eq(SnowflakeObjectType.TABLE))).thenAnswer(invocation -> tablesInSchema.contains(invocation.getArgument(
                0)));
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.DATABASE,
                "")).thenReturn(databasesInAccount);
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.SCHEMA,
                databaseName)).thenReturn(schemasInDatabase);
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.SCHEMA,
                SnowflakeObjectType.TABLE,
                databaseName + "." + schemaName)).thenReturn(tablesInSchema);
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.TABLE,
                databaseName)).thenReturn(tablesInSchema);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @ParameterizedTest
    @MethodSource("compileRoleChangesTestParamsStream")
    void compileChanges(CompileRoleChangesTestParams params) {
        when(snowflakeGrantsService.getGrants(
                params.playbookRoleModel.name(),
                false)).thenReturn(params.mockGetGrantsResult);
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(params.existingRoles);
        when(snowflakeObjectsService.objectExists(anyString(), eq(SnowflakeObjectType.ACCOUNT))).thenReturn(true);
        when(snowflakeObjectsService.objectExists(
                "OTHER_DB.OTHER_SCHEMA",
                SnowflakeObjectType.SCHEMA)).thenReturn(true);
        PlaybookModel playbookModel = new PlaybookModel(Map.of(params.roleId, params.playbookRoleModel));
        List<CompiledChanges> compiledChangesExpected = params.expected.containsChanges() ? List.of(params.expected) :
                List.of();
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false);
        assertEquals(compiledChangesExpected, compiledChangesActual);
    }

    @Test
    void compileChangesOwnershipFilter() {
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.OTHER_TABLE_NAME",
                SnowflakeObjectType.TABLE)).thenReturn(true);
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a", new PlaybookRoleModel("role_a", List.of()), "role-b", new PlaybookRoleModel(
                        "role_b",
                        List.of(new PlaybookPrivilegeGrant(
                                "table",
                                tableName,
                                schemaName,
                                databaseName,
                                List.of("OWNERSHIP"),
                                true,
                                true)))));
        SnowflakeGrantBuilder extraOwnerGrantSilence = SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                "ROLE",
                "ROLE_A",
                false,
                false,
                false));
        SnowflakeGrantBuilder extraOwnerGrantRevoke = SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.OTHER_TABLE_NAME",
                "ROLE",
                "ROLE_A",
                false,
                false,
                false));
        when(snowflakeGrantsService.getGrants("role_a", false)).thenReturn(Map.of(
                extraOwnerGrantSilence.getKey(),
                extraOwnerGrantSilence,
                extraOwnerGrantRevoke.getKey(),
                extraOwnerGrantRevoke));
        when(snowflakeGrantsService.getGrants("role_b", false)).thenReturn(Map.of());
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(List.of("ROLE_A", "ROLE_B"));
        List<CompiledChanges> compiledChangesExpected = List.of(
                new CompiledChanges(
                        "role-a",
                        "role_a",
                        List.of(),
                        List.of(List.of(
                                "GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"OTHER_TABLE_NAME\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;"))),
                new CompiledChanges(
                        "role-b",
                        "role_b",
                        List.of(),
                        List.of(List.of(
                                "GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE ROLE_B COPY CURRENT GRANTS;"))));
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false);
        assertEquals(compiledChangesExpected, compiledChangesActual);
    }

    @Test
    void compileChangesOwnershipFilterContainer() {
        when(snowflakeObjectsService.objectExists(
                "OTHER_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                SnowflakeObjectType.TABLE)).thenReturn(true);
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a", new PlaybookRoleModel("role_a", List.of()), "role-b", new PlaybookRoleModel(
                        "role_b",
                        List.of(new PlaybookPrivilegeGrant(
                                "table",
                                "*",
                                "*",
                                databaseName,
                                List.of("OWNERSHIP"),
                                false,
                                true)))));
        SnowflakeGrantBuilder extraOwnerGrantSilence = SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                "ROLE",
                "ROLE_A",
                false,
                false,
                false));
        SnowflakeGrantBuilder extraOwnerGrantRevoke = SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "OWNERSHIP",
                "TABLE",
                "OTHER_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                "ROLE",
                "ROLE_A",
                false,
                false,
                false));
        when(snowflakeGrantsService.getGrants("role_a", false)).thenReturn(Map.of(
                extraOwnerGrantSilence.getKey(),
                extraOwnerGrantSilence,
                extraOwnerGrantRevoke.getKey(),
                extraOwnerGrantRevoke));
        when(snowflakeGrantsService.getGrants("role_b", false)).thenReturn(Map.of());
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(List.of("ROLE_A", "ROLE_B"));
        List<CompiledChanges> compiledChangesExpected = List.of(
                new CompiledChanges(
                        "role-a",
                        "role_a",
                        List.of(),
                        List.of(List.of(
                                "GRANT OWNERSHIP ON TABLE \"OTHER_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;"))),
                new CompiledChanges(
                        "role-b", "role_b", List.of(), List.of(
                        List.of("GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"mock_table_name_2\" TO ROLE ROLE_B COPY CURRENT GRANTS;"),
                        List.of("GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE ROLE_B COPY CURRENT GRANTS;"))));
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false);
        assertEquals(compiledChangesExpected, compiledChangesActual);
    }

    @Test
    void compileChangesRoleCreationOwner() {
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a",
                new PlaybookRoleModel("role_a", List.of(), true, true, true, "SECURITYADMIN")));
        when(snowflakeGrantsService.getGrants("role_a")).thenReturn(Map.of());
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(List.of());
        List<CompiledChanges> compiledChangesExpected = List.of(new CompiledChanges(
                "role-a", "role_a", List.of(
                "CREATE ROLE IF NOT EXISTS ROLE_A;",
                "GRANT OWNERSHIP ON ROLE ROLE_A TO ROLE SECURITYADMIN COPY CURRENT GRANTS;"), List.of()));
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false);
        assertEquals(compiledChangesExpected, compiledChangesActual);
    }

    @Test
    void applyPrivilegeChanges() {
        List<CompiledChanges> compiledChanges = List.of(compileRoleChangesTestParamsStream().findFirst()
                .get().expected);
        snowflakeProvider.applyPrivilegeChanges(compiledChanges);
        compiledChanges.forEach(x -> x.roleGrantStatements()
                .forEach(y -> verify(snowflakeStatementsService, times(1)).applyStatements(y)));
    }

    @Test
    void applyPrivilegeChangesToRole() {
        CompiledChanges compiledChanges = compileRoleChangesTestParamsStream().findFirst().get().expected;
        snowflakeProvider.applyPrivilegeChangesToRole(compiledChanges);
        compiledChanges.roleGrantStatements()
                .forEach(x -> verify(snowflakeStatementsService, times(1)).applyStatements(x));
    }

    @Test
    void applyRolesChanges() {
        List<CompiledChanges> compiledChanges = List.of(compileRoleChangesTestParamsStream().findFirst()
                .get().expected);
        snowflakeProvider.applyRolesChanges(compiledChanges);
        compiledChanges.forEach(x -> verify(
                snowflakeStatementsService,
                times(1)).applyStatements(x.roleCreationStatements()));
    }

    @Test
    void applyRoleChanges() {
        CompiledChanges compiledChanges = compileRoleChangesTestParamsStream().findFirst().get().expected;
        snowflakeProvider.applyRoleChanges(compiledChanges);
        verify(snowflakeStatementsService, times(1)).applyStatements(compiledChanges.roleCreationStatements());
    }

    @ParameterizedTest
    @MethodSource("compileRoleChangesTestParamsStream")
    void compileRoleChanges(CompileRoleChangesTestParams params) {
        when(snowflakeGrantsService.getGrants(
                params.playbookRoleModel.name(),
                false)).thenReturn(params.mockGetGrantsResult);
        when(snowflakeObjectsService.objectExists(anyString(), eq(SnowflakeObjectType.ACCOUNT))).thenReturn(true);
        when(snowflakeObjectsService.objectExists(
                "OTHER_DB.OTHER_SCHEMA",
                SnowflakeObjectType.SCHEMA)).thenReturn(true);
        CompiledChanges compiledChangesActual = snowflakeProvider.compileRoleChanges(
                params.roleId,
                params.playbookRoleModel,
                params.existingRoles,
                false,
                new PlaybookModel(Map.of("foo", params.playbookRoleModel)),
                false);
        assertEquals(params.expected, compiledChangesActual);
    }

    @ParameterizedTest
    @MethodSource("compilePlaybookPrivilegeGrantsTestParamsStream")
    void compilePlaybookPrivilegeGrants(CompilePlaybookPrivilegeGrantsTestParams params) {
        when(snowflakeObjectsService.objectExists(anyString(), eq(SnowflakeObjectType.ACCOUNT))).thenReturn(true);
        when(snowflakeObjectsService.objectExists(
                "OTHER_DB.OTHER_SCHEMA",
                SnowflakeObjectType.SCHEMA)).thenReturn(true);
        when(snowflakeGrantsService.getGrants(params.roleName(), false)).thenReturn(params.mockGetGrantsResult);
        List<List<String>> actualStatements = this.snowflakeProvider.compilePlaybookPrivilegeGrants(
                params.playbookPrivilegeGrants,
                params.roleName,
                params.roleExists,
                params.revokeCurrentGrants,
                false,
                new PlaybookModel(Map.of()),
                false);
        try {
            assertEquals(
                    params.expected.stream().flatMap(Collection::stream).toList(),
                    actualStatements.stream().flatMap(Collection::stream).toList());
        } catch (AssertionError e) {
            throw e;
        }
    }

    @Test
    void compilePlaybookPrivilegeGrantsFuture() {
        String roleName = "FOO_BAR";
        when(snowflakeGrantsService.getGrants(roleName)).thenReturn(Map.of());
        List<PlaybookPrivilegeGrant> playbookPrivilegeGrants = List.of(new PlaybookPrivilegeGrant(
                "view",
                "*",
                "*",
                databaseName.toLowerCase(),
                List.of("select", "update"),
                true,
                true,
                true));
        List<List<String>> expectedStatements = List.of(
                List.of("GRANT SELECT ON FUTURE VIEWS IN DATABASE \"MOCK_DB_NAME\" TO ROLE FOO_BAR;"),
                List.of("GRANT SELECT ON FUTURE VIEWS IN SCHEMA \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\" TO ROLE FOO_BAR;"),
                List.of("GRANT UPDATE ON FUTURE VIEWS IN DATABASE \"MOCK_DB_NAME\" TO ROLE FOO_BAR;"),
                List.of("GRANT UPDATE ON FUTURE VIEWS IN SCHEMA \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\" TO ROLE FOO_BAR;"));
        List<List<String>> actualStatements = this.snowflakeProvider.compilePlaybookPrivilegeGrants(playbookPrivilegeGrants,
                roleName,
                true,
                true,
                false,
                new PlaybookModel(Map.of()),
                false);
        assertEquals(expectedStatements, actualStatements);

    }

    @Test
    void playbookGrantToSnowflakeGrants() {
        PlaybookPrivilegeGrant playbookPrivilegeGrant = new PlaybookPrivilegeGrant(
                "table",
                "mock_table_name",
                "mock_schema_name",
                "mock_db_name",
                List.of("select", "update"),
                true,
                true);
        String roleName = "mock_role_name";
        List<SnowflakeGrantBuilder> expectedBuilders = List.of(
                new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                        "ROLE",
                        "MOCK_ROLE_NAME",
                        false,
                        false,
                        false)), new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                        "UPDATE",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                        "ROLE",
                        "MOCK_ROLE_NAME",
                        false,
                        false,
                        false)));
        List<SnowflakeGrantBuilder> actualBuilders = this.snowflakeProvider.playbookGrantToSnowflakeGrants(playbookPrivilegeGrant,
                roleName,
                false);
        assertEquals(expectedBuilders, actualBuilders);
    }

    @Test
    void playbookGrantToSnowflakeGrantsApplicationRole() {
        PlaybookPrivilegeGrant playbookPrivilegeGrant = new PlaybookPrivilegeGrant(
                "application_role",
                "snowflake.trust_center_viewer",
                null,
                null,
                List.of("usage"),
                true,
                true);
        String roleName = "mock_role_name";
        List<SnowflakeGrantBuilder> expectedBuilders = List.of(
                new SnowflakeApplicationRoleGrantBuilder(new SnowflakeGrantModel(
                        "USAGE",
                        "APPLICATION_ROLE",
                        "SNOWFLAKE.TRUST_CENTER_VIEWER",
                        "ROLE",
                        "MOCK_ROLE_NAME",
                        false,
                        false,
                        false)));
        List<SnowflakeGrantBuilder> actualBuilders = this.snowflakeProvider.playbookGrantToSnowflakeGrants(playbookPrivilegeGrant,
                roleName,
                false);
        assertEquals(expectedBuilders.get(0).getKey(), actualBuilders.get(0).getKey());
        assertEquals(expectedBuilders, actualBuilders);
    }

    @Test
    void standardGrants() {
        PlaybookPrivilegeGrant playbookPrivilegeGrant = new PlaybookPrivilegeGrant(
                "table",
                "mock_table_name",
                "mock_schema_name",
                "mock_db_name",
                List.of("select", "update"),
                true,
                true);
        String roleName = "mock_role_name";
        List<SnowflakeGrantModel> expectedGrants = List.of(
                new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                        "ROLE",
                        "MOCK_ROLE_NAME",
                        false,
                        false,
                        false), new SnowflakeGrantModel(
                        "UPDATE",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                        "ROLE",
                        "MOCK_ROLE_NAME",
                        false,
                        false,
                        false));
        List<SnowflakeGrantModel> actualGrants = this.snowflakeProvider.standardGrants(
                playbookPrivilegeGrant,
                roleName);
        assertEquals(expectedGrants, actualGrants);
    }

    @Test
    void standardGrantsWildcard() {
        PlaybookPrivilegeGrant playbookPrivilegeGrant = new PlaybookPrivilegeGrant(
                "table",
                "*",
                "mock_schema_name",
                "mock_db_name",
                List.of("select", "update"),
                true,
                true);
        String roleName = "mock_role_name";
        List<SnowflakeGrantModel> expectedGrants = List.of();
        List<SnowflakeGrantModel> actualGrants = this.snowflakeProvider.standardGrants(
                playbookPrivilegeGrant,
                roleName);
        assertEquals(expectedGrants, actualGrants);
    }

    @Test
    void containerGrants() {
        PlaybookPrivilegeGrant playbookPrivilegeGrant = new PlaybookPrivilegeGrant(
                "table",
                "*",
                "mock_schema_name",
                "mock_db_name",
                List.of("select", "update"),
                true,
                true);
        List<String> tablesInDatabase = List.of(
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_1",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.\"mock_table_2\"");
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.DATABASE,
                "")).thenReturn(List.of("MOCK_DB_NAME"));
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.SCHEMA,
                "MOCK_DB_NAME")).thenReturn(List.of("MOCK_DB_NAME.MOCK_SCHEMA_NAME"));
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.SCHEMA,
                SnowflakeObjectType.TABLE,
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME")).thenReturn(tablesInDatabase);
        String roleName = "mock_role_name";
        List<SnowflakeGrantModel> expectedGrants = new ArrayList<>();
        expectedGrants.add(new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.<TABLE>",
                "ROLE",
                "MOCK_ROLE_NAME",
                false,
                true,
                false));
        expectedGrants.add(new SnowflakeGrantModel(
                "UPDATE",
                "TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.<TABLE>",
                "ROLE",
                "MOCK_ROLE_NAME",
                false,
                true,
                false));
        tablesInDatabase.forEach(expectedObjectName -> {
            expectedGrants.add(new SnowflakeGrantModel(
                    "SELECT",
                    "TABLE",
                    expectedObjectName,
                    "ROLE",
                    "MOCK_ROLE_NAME",
                    false,
                    false,
                    false));
            expectedGrants.add(new SnowflakeGrantModel(
                    "UPDATE",
                    "TABLE",
                    expectedObjectName,
                    "ROLE",
                    "MOCK_ROLE_NAME",
                    false,
                    false,
                    false));

        });
        List<SnowflakeGrantModel> actualGrants = this.snowflakeProvider.containerGrants(
                playbookPrivilegeGrant,
                roleName);
        assertEquals(expectedGrants, actualGrants);
    }

    @Test
    void containerGrantsMultipleWordObjectName() {
        PlaybookPrivilegeGrant playbookPrivilegeGrant = new PlaybookPrivilegeGrant(
                "external_table",
                "*",
                "mock_schema_name",
                "mock_db_name",
                List.of("select"),
                true,
                true);
        List<String> tablesInDatabase = List.of(
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_1",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.\"mock_table_2\"");
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.DATABASE,
                "")).thenReturn(List.of("MOCK_DB_NAME"));
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.SCHEMA,
                "MOCK_DB_NAME")).thenReturn(List.of("MOCK_DB_NAME.MOCK_SCHEMA_NAME"));
        when(snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.SCHEMA,
                SnowflakeObjectType.EXTERNAL_TABLE,
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME")).thenReturn(tablesInDatabase);
        String roleName = "mock_role_name";
        List<SnowflakeGrantModel> expectedGrants = new ArrayList<>();
        expectedGrants.add(new SnowflakeGrantModel(
                "SELECT",
                "EXTERNAL_TABLE",
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.<EXTERNAL_TABLE>",
                "ROLE",
                "MOCK_ROLE_NAME",
                false,
                true,
                false));
        tablesInDatabase.forEach(expectedObjectName -> {
            expectedGrants.add(new SnowflakeGrantModel(
                    "SELECT",
                    "EXTERNAL_TABLE",
                    expectedObjectName,
                    "ROLE",
                    "MOCK_ROLE_NAME",
                    false,
                    false,
                    false));
        });
        List<SnowflakeGrantModel> actualGrants = this.snowflakeProvider.containerGrants(
                playbookPrivilegeGrant,
                roleName);
        assertEquals(expectedGrants, actualGrants);
    }

    @Test
    void createFutureGrants() {
        List<SnowflakeGrantModel> expected = List.of(new SnowflakeGrantModel(
                "SELECT",
                "EXTERNAL_TABLE",
                "FOO_DB.<EXTERNAL_TABLE>",
                "ROLE",
                "BAR_ROLE",
                false,
                true,
                false));
        List<SnowflakeGrantModel> actual = snowflakeProvider.createFutureGrants(
                SnowflakeObjectType.EXTERNAL_TABLE,
                "FOO_DB",
                List.of("SELECT"),
                "BAR_ROLE",
                false);
        assertEquals(expected, actual);

    }

    public record CompilePlaybookPrivilegeGrantsTestParams(
            List<PlaybookPrivilegeGrant> playbookPrivilegeGrants,
            String roleName,
            Boolean roleExists,
            Map<String, SnowflakeGrantBuilder> mockGetGrantsResult,
            List<List<String>> expected,

            Boolean revokeCurrentGrants) {
    }

    public record CompileRoleChangesTestParams(

            PlaybookRoleModel playbookRoleModel,

            String roleId, Boolean roleExists,

            List<String> existingRoles, Map<String, SnowflakeGrantBuilder> mockGetGrantsResult,

            CompiledChanges expected) {
    }
}
