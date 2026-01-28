package us.zoom.data.dfence.providers.snowflake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.data.dfence.CompiledChanges;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakePermissionGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.DesiredGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.PartitionedGrantStatements;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.InOrder;


class SnowflakeProviderTest {

    @Mock
    SnowflakeGrantsService snowflakeGrantsService;

    @Mock
    SnowflakeStatementsService snowflakeStatementsService;

    @Mock
    SnowflakeObjectsService snowflakeObjectsService;

    SnowflakeProvider snowflakeProvider;

    DesiredGrantsProvider desiredGrantsProvider;

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
                List.of("GRANT UPDATE ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE MOCK_ROLE_NAME;"),
                List.of("GRANT USAGE ON DATABASE \"MOCK_DB_NAME\" TO ROLE MOCK_ROLE_NAME;"));
        List<String> expectedRoleCreationStatements = List.of("CREATE ROLE IF NOT EXISTS MOCK_ROLE_NAME;");
        SnowflakeGrantBuilderOptions extraGrantOptions = new SnowflakeGrantBuilderOptions();
        extraGrantOptions.setSuppressErrors(false);
        SnowflakeGrantBuilder extraGrant = new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                "create table",
                "SCHEMA",
                "OTHER_DB.OTHER_SCHEMA",
                "ROLE",
                roleName,
                false,
                false,
                false), extraGrantOptions);
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
                                List.of(),
                                expectedRoleCreationStatements,
                                expectedRoleGrantStatements)),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants),
                        roleId,
                        true,
                        List.of("SOME_OTHER_ROLE", roleName.toUpperCase()),
                        new HashMap<>(),
                        new CompiledChanges(roleId, roleName, List.of(), List.of(), expectedRoleGrantStatements)),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants),
                        roleId,
                        true,
                        List.of("SOME_OTHER_ROLE", roleName.toUpperCase()),
                        Map.of(extraGrant.getKey(), extraGrant),
                        new CompiledChanges(
                                roleId, roleName, List.of(), List.of(), new ArrayList<>() {{
                            add(List.of(
                                    "REVOKE CREATE TABLE ON SCHEMA \"OTHER_DB\".\"OTHER_SCHEMA\" FROM ROLE MOCK_ROLE_NAME;"));
                            addAll(expectedRoleGrantStatements);
                        }})),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants, true, false, true, null, UnsupportedRevokeBehavior.IGNORE),
                        roleId,
                        true,
                        List.of("SOME_OTHER_ROLE", roleName.toUpperCase()),
                        Map.of(extraGrant.getKey(), extraGrant),
                        new CompiledChanges(
                                roleId, roleName, List.of(), List.of(), new ArrayList<>() {{
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
                        new CompiledChanges(roleId, roleName, List.of(), List.of(), expectedRoleGrantStatements)),
                new CompileRoleChangesTestParams(
                        new PlaybookRoleModel(roleName, playbookPrivilegeGrants, false, true, true, null, UnsupportedRevokeBehavior.IGNORE),
                        roleId,
                        false,
                        List.of("SOME_OTHER_ROLE"),
                        new HashMap<>(),
                        new CompiledChanges(roleId, roleName, List.of(), List.of(), List.of())));
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
        desiredGrantsProvider = new DesiredGrantsProvider(snowflakeObjectsService);
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
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false, false);
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
                        List.of(List.of(
                                "GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"OTHER_TABLE_NAME\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;")),
                        List.of(),
                        List.of()),
                new CompiledChanges(
                        "role-b",
                        "role_b",
                        List.of(List.of(
                                "GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE ROLE_B COPY CURRENT GRANTS;")),
                        List.of(),
                        List.of()));
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false, false);
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
                        List.of(List.of(
                                "GRANT OWNERSHIP ON TABLE \"OTHER_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;")),
                        List.of(),
                        List.of()),
                new CompiledChanges(
                        "role-b", "role_b", 
                        List.of(
                        List.of("GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"mock_table_name_2\" TO ROLE ROLE_B COPY CURRENT GRANTS;"),
                        List.of("GRANT OWNERSHIP ON TABLE \"MOCK_DB_NAME\".\"MOCK_SCHEMA_NAME\".\"MOCK_TABLE_NAME\" TO ROLE ROLE_B COPY CURRENT GRANTS;")),
                        List.of(),
                        List.of()));
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false, false);
        assertEquals(compiledChangesExpected, compiledChangesActual);
    }

    @Test
    void compileChangesRoleCreationOwner() {
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a",
                new PlaybookRoleModel("role_a", List.of(), true, true, true, "SECURITYADMIN", UnsupportedRevokeBehavior.IGNORE)));
        when(snowflakeGrantsService.getGrants("role_a")).thenReturn(Map.of());
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(List.of());
        List<CompiledChanges> compiledChangesExpected = List.of(new CompiledChanges(
                "role-a", "role_a", List.of(),
                List.of(
                "CREATE ROLE IF NOT EXISTS ROLE_A;",
                "GRANT OWNERSHIP ON ROLE ROLE_A TO ROLE SECURITYADMIN COPY CURRENT GRANTS;"), List.of()));
        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false, false);
        assertEquals(compiledChangesExpected, compiledChangesActual);
    }

    @Test
    void applyPrivilegeChanges() {
        List<CompiledChanges> compiledChanges = List.of(compileRoleChangesTestParamsStream().findFirst()
                .get().expected);
        snowflakeProvider.applyPrivilegeChanges(compiledChanges);
        compiledChanges.forEach(x -> {
            x.ownershipGrantStatements()
                .forEach(y -> verify(snowflakeStatementsService, times(1)).applyStatements(y));
            x.roleGrantStatements()
                .forEach(y -> verify(snowflakeStatementsService, times(1)).applyStatements(y));
        });
    }

    @Test
    void applyPrivilegeChangesToRole() {
        CompiledChanges compiledChanges = compileRoleChangesTestParamsStream().findFirst().get().expected;
        snowflakeProvider.applyPrivilegeChangesToRole(compiledChanges);
        compiledChanges.ownershipGrantStatements()
                .forEach(x -> verify(snowflakeStatementsService, times(1)).applyStatements(x));
        compiledChanges.roleGrantStatements()
                .forEach(x -> verify(snowflakeStatementsService, times(1)).applyStatements(x));
    }

    @Test
    void compileChangesSeparatesOwnershipAndRegularGrants() {
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                SnowflakeObjectType.TABLE)).thenReturn(true);
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a", new PlaybookRoleModel("role_a", List.of(
                        new PlaybookPrivilegeGrant(
                                "table",
                                "mock_table_name",
                                "mock_schema_name",
                                "mock_db_name",
                                List.of("OWNERSHIP", "SELECT", "UPDATE"),
                                true,
                                true)))));
        when(snowflakeGrantsService.getGrants("role_a", false)).thenReturn(Map.of());
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(List.of("ROLE_A"));

        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false, false);

        assertEquals(1, compiledChangesActual.size());
        CompiledChanges changes = compiledChangesActual.get(0);

        // Verify ownership grants are in ownershipGrantStatements
        assertEquals(1, changes.ownershipGrantStatements().size());
        assertTrue(changes.ownershipGrantStatements().get(0).stream()
                .anyMatch(stmt -> stmt.contains("GRANT OWNERSHIP")));

        // Verify regular grants are in roleGrantStatements
        assertEquals(2, changes.roleGrantStatements().size());
        assertTrue(changes.roleGrantStatements().stream()
                .anyMatch(stmts -> stmts.stream().anyMatch(stmt -> stmt.contains("GRANT SELECT"))));
        assertTrue(changes.roleGrantStatements().stream()
                .anyMatch(stmts -> stmts.stream().anyMatch(stmt -> stmt.contains("GRANT UPDATE"))));

        // Verify ownership grants are NOT in roleGrantStatements
        assertFalse(changes.roleGrantStatements().stream()
                .anyMatch(stmts -> stmts.stream().anyMatch(stmt -> stmt.contains("OWNERSHIP"))));

        // Verify regular grants are NOT in ownershipGrantStatements
        assertFalse(changes.ownershipGrantStatements().stream()
                .anyMatch(stmts -> stmts.stream().anyMatch(stmt ->
                        stmt.contains("SELECT") || stmt.contains("UPDATE"))));
    }

    @Test
    void applyPrivilegeChangesToRoleAppliesOwnershipGrantsBeforeRegularGrants() {
        // Create CompiledChanges with both ownership and regular grants
        List<List<String>> ownershipGrants = List.of(
                List.of("GRANT OWNERSHIP ON TABLE \"DB\".\"SCHEMA\".\"TABLE1\" TO ROLE ROLE_A COPY CURRENT GRANTS;"));
        List<List<String>> regularGrants = List.of(
                List.of("GRANT SELECT ON TABLE \"DB\".\"SCHEMA\".\"TABLE1\" TO ROLE ROLE_A;"),
                List.of("GRANT UPDATE ON TABLE \"DB\".\"SCHEMA\".\"TABLE1\" TO ROLE ROLE_A;"));

        CompiledChanges compiledChanges = new CompiledChanges(
                "role-a", "role_a", ownershipGrants, List.of(), regularGrants);

        snowflakeProvider.applyPrivilegeChangesToRole(compiledChanges);

        // Use InOrder to verify ownership grants are applied before regular grants
        // Note: Regular grants may execute in parallel, so we verify all ownership grants
        // complete before any regular grants start
        InOrder inOrder = inOrder(snowflakeStatementsService);

        // Verify ownership grants are applied first (all of them complete)
        inOrder.verify(snowflakeStatementsService, times(1))
                .applyStatements(ownershipGrants.get(0));

        // Verify regular grants are applied after ownership grants complete
        // We verify at least one regular grant is called after ownership
        // (exact order within regular grants may vary due to parallel execution)
        inOrder.verify(snowflakeStatementsService, atLeastOnce())
                .applyStatements(anyList());

        // Verify all calls were made (order within regular grants doesn't matter)
        verify(snowflakeStatementsService, times(1)).applyStatements(ownershipGrants.get(0));
        verify(snowflakeStatementsService, times(1)).applyStatements(regularGrants.get(0));
        verify(snowflakeStatementsService, times(1)).applyStatements(regularGrants.get(1));
        verify(snowflakeStatementsService, times(3)).applyStatements(anyList());
    }

    @Test
    void compileChangesHandlesNoOwnershipGrants() {
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a", new PlaybookRoleModel("role_a", List.of(
                        new PlaybookPrivilegeGrant(
                                "table",
                                "mock_table_name",
                                "mock_schema_name",
                                "mock_db_name",
                                List.of("SELECT", "UPDATE"),
                                true,
                                true)))));
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                SnowflakeObjectType.TABLE)).thenReturn(true);
        when(snowflakeGrantsService.getGrants("role_a", false)).thenReturn(Map.of());
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(List.of("ROLE_A"));

        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false, false);

        assertEquals(1, compiledChangesActual.size());
        CompiledChanges changes = compiledChangesActual.get(0);

        // Verify ownershipGrantStatements is empty
        assertTrue(changes.ownershipGrantStatements().isEmpty());

        // Verify regular grants are in roleGrantStatements
        assertFalse(changes.roleGrantStatements().isEmpty());
        assertEquals(2, changes.roleGrantStatements().size());
    }

    @Test
    void compileChangesHandlesOnlyOwnershipGrants() {
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a", new PlaybookRoleModel("role_a", List.of(
                        new PlaybookPrivilegeGrant(
                                "table",
                                "mock_table_name",
                                "mock_schema_name",
                                "mock_db_name",
                                List.of("OWNERSHIP"),
                                true,
                                true)))));
        when(snowflakeObjectsService.objectExists(
                "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                SnowflakeObjectType.TABLE)).thenReturn(true);
        when(snowflakeGrantsService.getGrants("role_a", false)).thenReturn(Map.of());
        when(snowflakeObjectsService.getContainerObjectQualNames(
                eq(SnowflakeObjectType.ACCOUNT),
                eq(SnowflakeObjectType.ROLE),
                anyString())).thenReturn(List.of("ROLE_A"));

        List<CompiledChanges> compiledChangesActual = snowflakeProvider.compileChanges(playbookModel, false, false);

        assertEquals(1, compiledChangesActual.size());
        CompiledChanges changes = compiledChangesActual.get(0);

        // Verify ownershipGrantStatements has grants
        assertFalse(changes.ownershipGrantStatements().isEmpty());
        assertEquals(1, changes.ownershipGrantStatements().size());

        // Verify roleGrantStatements is empty
        assertTrue(changes.roleGrantStatements().isEmpty());
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
                false,
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
        PartitionedGrantStatements partitionedStatements = this.snowflakeProvider.compilePlaybookPrivilegeGrants(
                params.playbookPrivilegeGrants,
                params.roleName,
                params.roleExists,
                params.revokeCurrentGrants,
                false,
                new PlaybookModel(Map.of()),
                false,
                UnsupportedRevokeBehavior.IGNORE);
        List<List<String>> actualStatements = new ArrayList<>();
        actualStatements.addAll(partitionedStatements.ownershipStatements());
        actualStatements.addAll(partitionedStatements.nonOwnershipStatements());
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
                List.of("GRANT UPDATE ON FUTURE VIEWS IN DATABASE \"MOCK_DB_NAME\" TO ROLE FOO_BAR;"));
        PartitionedGrantStatements partitionedStatements = this.snowflakeProvider.compilePlaybookPrivilegeGrants(playbookPrivilegeGrants,
                roleName,
                true,
                true,
                false,
                new PlaybookModel(Map.of()),
                false,
                UnsupportedRevokeBehavior.IGNORE);
        List<List<String>> actualStatements = new ArrayList<>();
        actualStatements.addAll(partitionedStatements.ownershipStatements());
        actualStatements.addAll(partitionedStatements.nonOwnershipStatements());
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
        SnowflakeGrantBuilderOptions expectedOptions = new SnowflakeGrantBuilderOptions();
        expectedOptions.setSuppressErrors(false);
        List<SnowflakeGrantBuilder> expectedBuilders = List.of(
                new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                        "SELECT",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                        "ROLE",
                        "MOCK_ROLE_NAME",
                        false,
                        false,
                        false), expectedOptions), new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                        "UPDATE",
                        "TABLE",
                        "MOCK_DB_NAME.MOCK_SCHEMA_NAME.MOCK_TABLE_NAME",
                        "ROLE",
                        "MOCK_ROLE_NAME",
                        false,
                        false,
                        false), expectedOptions));
        SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
        options.setSuppressErrors(false);
        List<SnowflakeGrantBuilder> actualBuilders = desiredGrantsProvider.playbookGrantToSnowflakeGrants(playbookPrivilegeGrant,
                roleName,
                options);
        assertEquals(expectedBuilders, actualBuilders);
    }


    // Note: The following tests have been removed as the methods standardGrants, containerGrants, 
    // and createFutureGrants have been refactored into internal classes (StandardGrantsProvider, 
    // AllGrantsProvider, FutureGrantsProvider) and are no longer directly accessible from SnowflakeProvider.
    // The functionality is now tested through the playbookGrantToSnowflakeGrants method in DesiredGrantsProvider.

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

    @Test
    void applyPrivilegeChangesToRole_executesOwnershipFirst() {
        List<String> ownershipGrant = List.of("GRANT OWNERSHIP ON TABLE T1 TO ROLE R1");
        List<String> standardGrant = List.of("GRANT SELECT ON TABLE T1 TO ROLE R1");

        CompiledChanges changes = new CompiledChanges(
                "role-id",
                "ROLE1",
                List.of(ownershipGrant),
                List.of(),
                List.of(standardGrant)
        );

        snowflakeProvider.applyPrivilegeChangesToRole(changes);

        InOrder inOrder = inOrder(snowflakeStatementsService);
        inOrder.verify(snowflakeStatementsService).applyStatements(ownershipGrant);
        inOrder.verify(snowflakeStatementsService).applyStatements(standardGrant);
    }

    @Test
    void compileChanges_partitionsMixedGrants() {
        // Setup existing grants: One OWNERSHIP, One STANDARD
        SnowflakeGrantBuilder existingOwnership = SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "OWNERSHIP", "TABLE", "DB.SCHEMA.T1", "ROLE", "ROLE_A", false, false, false));
        SnowflakeGrantBuilder existingStandard = SnowflakeGrantBuilder.fromGrant(new SnowflakeGrantModel(
                "SELECT", "TABLE", "DB.SCHEMA.T1", "ROLE", "ROLE_A", false, false, false));

        when(snowflakeGrantsService.getGrants("role_a", false)).thenReturn(Map.of(
                existingOwnership.getKey(), existingOwnership,
                existingStandard.getKey(), existingStandard));

        when(snowflakeObjectsService.getContainerObjectQualNames(eq(SnowflakeObjectType.ACCOUNT), eq(SnowflakeObjectType.ROLE), anyString()))
                .thenReturn(List.of("ROLE_A"));
        when(snowflakeObjectsService.objectExists(anyString(), any(SnowflakeObjectType.class))).thenReturn(true);

        // Playbook: Revoke everything (empty grants list, revokeOtherGrants=true)
        PlaybookModel playbookModel = new PlaybookModel(Map.of(
                "role-a", new PlaybookRoleModel("role_a", List.of(), true, true, true, null, UnsupportedRevokeBehavior.IGNORE)));

        List<CompiledChanges> changes = snowflakeProvider.compileChanges(playbookModel, false, false);

        assertEquals(1, changes.size());
        CompiledChanges roleChanges = changes.get(0);

        // Check ownership grants (should contain revoke of OWNERSHIP)
        assertEquals(1, roleChanges.ownershipGrantStatements().size(), "Should have 1 ownership revoke statement");
        assertEquals("GRANT OWNERSHIP ON TABLE \"DB\".\"SCHEMA\".\"T1\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;",
                roleChanges.ownershipGrantStatements().get(0).get(0));

        // Check non-ownership grants (should contain revoke of SELECT)
        assertEquals(1, roleChanges.roleGrantStatements().size(), "Should have 1 non-ownership revoke statement");
        assertEquals("REVOKE SELECT ON TABLE \"DB\".\"SCHEMA\".\"T1\" FROM ROLE ROLE_A;",
                roleChanges.roleGrantStatements().get(0).get(0));
    }
}
