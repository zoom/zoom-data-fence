package us.zoom.data.systemtest;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.*;
import us.zoom.data.dfence.ChangesSummary;
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
 * System test for consistency check functionality.
 * Ensures consistency test is passing for revoke-grants playbook and create-grants playbook.
 * Uses unique naming to avoid collisions with other tests.
 */
@Slf4j
public class TestGrantConsistencySystemTest extends BaseGrantSystemTest {

    private TestRunFixture fixture;

    @BeforeGroups(groups = {"consistency"})
    public void beforeGroupsConsistency() {
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

    @AfterGroups(groups = {"consistency"})
    public void afterGroupsConsistency() throws SQLException {
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
    private Map<String, String> variables;

    @Test(groups = {"consistency"})
    public void testConsistencyCheckRevokeGrantsPlaybook() throws URISyntaxException {
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

        variables = createVariables();
        
        // Test consistency check for revoke-grants playbook
        File rolesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/revoke-grants/roles.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());

        PlaybookService playbookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(rolesFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        
        // Compile with consistency check enabled (default behavior)
        // This should pass if grant creation and revoke logic are consistent
        ChangesSummary changes = playbookService.compileChanges(false, true);
        
        // If we get here without exception, consistency check passed
        log.info("Consistency check passed for revoke-grants playbook. Generated {} changes.", changes.changes().size());
        Assert.assertTrue(true, "Consistency check should pass for revoke-grants playbook");
    }

    @Test(groups = {"consistency"})
    public void testConsistencyCheckCreateGrantsPlaybook() throws URISyntaxException {
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

        variables = createVariables();
        
        // Test consistency check for create-grants playbook
        File rolesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/create-grants/roles.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());

        PlaybookService playbookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(rolesFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        
        // Compile with consistency check enabled (default behavior)
        // This should pass if grant creation and revoke logic are consistent
        ChangesSummary changes = playbookService.compileChanges(false, true);
        
        // If we get here without exception, consistency check passed
        log.info("Consistency check passed for create-grants playbook. Generated {} changes.", changes.changes().size());
        Assert.assertTrue(true, "Consistency check should pass for create-grants playbook");
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
}
