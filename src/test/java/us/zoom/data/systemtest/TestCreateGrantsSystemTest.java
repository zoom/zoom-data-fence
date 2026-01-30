package us.zoom.data.systemtest;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import us.zoom.data.dfence.ChangesSummary;
import us.zoom.data.dfence.CompiledChanges;
import us.zoom.data.dfence.playbook.PlaybookService;
import us.zoom.data.dfence.playbook.PlaybookServiceBuilder;
import us.zoom.data.dfence.test.fixtures.TestRunFixture;
import us.zoom.data.dfence.test.fixtures.TestRunFixtureBuilder;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * System test for grants creator functionality.
 * Creates a playbook with different kinds of grants and ensures all grants are correctly created.
 * Uses unique naming to avoid collisions with other tests.
 */
@Slf4j
public class TestCreateGrantsSystemTest extends BaseGrantSystemTest {

    private TestRunFixture fixture;

    @BeforeGroups(groups = {"create"})
    public void beforeGroupsCreate() {
        fixture = new TestRunFixtureBuilder(
                sysadminSnowflakeConnectionProvider,
                securityadminSnowflakeConnectionProvider)
                .withDatabase()
                .withSchema()
                .withTable()
                .withRoles()
                .withProcedures()
                .build();
        log.info("Test fixture setup complete for {}", this.getClass().getSimpleName());
    }

    @AfterGroups(groups = {"create"})
    public void afterGroupsCreate() throws SQLException {
        if (fixture == null) {
            return; // Nothing to clean up
        }
        
        // Transfer ownership back to SYSADMIN before teardown
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            connection.createStatement().execute(
                    String.format("GRANT OWNERSHIP ON ALL TABLES IN DATABASE %s TO ROLE %S REVOKE CURRENT GRANTS",
                            fixture.databaseName(), snowflakeSysadminRole)
            );
            connection.createStatement().execute(
                    String.format("GRANT OWNERSHIP ON ALL PROCEDURES IN DATABASE %s TO ROLE %S REVOKE CURRENT GRANTS",
                            fixture.databaseName(), snowflakeSysadminRole)
            );
        }
        
        log.info("Tearing down lifecycleManager for {}", this.getClass().getSimpleName());
        fixture.lifecycleManager().teardown();
    }

    // Test state - stored as instance variables
    private ChangesSummary changes;
    private PlaybookService playbookService;
    private Map<String, String> variables;

    @Test(groups = {"create"})
    public void testCompile() throws URISyntaxException {
        // Grant permissions to SYSADMIN role for the test objects
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            connection.createStatement().execute(
                    String.format("GRANT USAGE, MONITOR ON DATABASE %s TO ROLE %s",
                            fixture.databaseName(), snowflakeSysadminRole)
            );
            connection.createStatement().execute(
                    String.format("GRANT USAGE, MONITOR ON ALL SCHEMAS IN DATABASE %s TO ROLE %s",
                            fixture.databaseName(), snowflakeSysadminRole)
            );
        } catch (SQLException e) {
            throw new RuntimeException("Unable to grant permissions on objects to SYSADMIN role.", e);
        }

        File rolesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/create-grants/roles.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());

        variables = createVariables();
        playbookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(rolesFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        changes = playbookService.compileChanges(false);

        // Verify that changes are generated
        Assert.assertFalse(changes.changes().isEmpty(), "Expected changes to be generated");
        
        // Validate that valid SQL statements are generated
        for (CompiledChanges change : changes.changes()) {
            // Check that grant statements are present in the changes
            boolean hasStatements = !change.ownershipGrantStatements().isEmpty() || 
                                   !change.roleGrantStatements().isEmpty() ||
                                   !change.roleCreationStatements().isEmpty();
            if (hasStatements) {
                log.info("LEFT OVER" + change.toString());
            }
            Assert.assertTrue(hasStatements, "Expected valid SQL statements to be generated");
            
            // Log the statements for debugging
            log.info("Changes for role {}: ownership statements: {}, role statements: {}, role creation: {}", 
                    change.roleName(), 
                    change.ownershipGrantStatements().size(),
                    change.roleGrantStatements().size(),
                    change.roleCreationStatements().size());
        }
    }

    private Map<String, String> createVariables() {
        Map<String, String> vars = new HashMap<>();
        vars.put("database", fixture.databaseName());
        vars.put("schema", fixture.schemaName());
        vars.put("table", fixture.tableName());
        vars.put("role", fixture.roleName());
        vars.put("role2", fixture.roleName2());
        vars.put("procedure", fixture.procedureName());
        vars.put("snowflake-user", snowflakeUser);
        vars.put("snowflake-account", snowflakeAccount);
        vars.put("snowflake-password", String.valueOf(snowflakePassword));
        vars.put("snowflake-authenticator", String.valueOf(snowflakeAuthenticator));
        vars.put("snowflake-sysadmin-role", snowflakeSysadminRole);
        vars.put("snowflake-securityadmin-role", snowflakeSecurityAdminRole);
        return vars;
    }

    @Test(dependsOnMethods = {"testCompile"}, groups = {"create"})
    public void testApply() throws SQLException {
        playbookService.applyChanges(changes.changes());

        // Ensure that there are no more changes.
        ChangesSummary newChanges = playbookService.compileChanges(false);
        Assert.assertTrue(newChanges.changes().isEmpty(), "Expected no remaining changes after apply");

        // Validate that the role has all the expected grants from the playbook.
        // The playbook includes database, schema (with create semantic view/agent), table, procedure
        List<Grant> grantsExpected = new ArrayList<>(List.of(
                new Grant("USAGE", "DATABASE", fixture.databaseName(), "ROLE", fixture.roleName()),
                new Grant("MONITOR", "DATABASE", fixture.databaseName(), "ROLE", fixture.roleName()),
                new Grant("USAGE", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("MONITOR", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("CREATE SEMANTIC VIEW", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("CREATE AGENT", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("SELECT", "TABLE", fixture.qualifiedTableName(), "ROLE", fixture.roleName()),
                new Grant("INSERT", "TABLE", fixture.qualifiedTableName(), "ROLE", fixture.roleName()),
                new Grant("USAGE", "PROCEDURE", fixture.procedureNameShowGrantsQual(), "ROLE", fixture.roleName()),
                new Grant("USAGE", "PROCEDURE", fixture.procedureNameNoArgsShowGrantsQual(), "ROLE", fixture.roleName())
        ));
        grantsExpected.sort(Comparator.comparing(x -> x.toString().hashCode()));

        List<Grant> grants = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
        assertGrantsMatch(grants, grantsExpected);
    }

}
