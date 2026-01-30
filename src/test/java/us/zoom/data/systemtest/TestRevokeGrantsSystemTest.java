package us.zoom.data.systemtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * System test for revoke functionality.
 * Creates extra grants not covered by playbook, runs revoke to remove only the extra grants.
 * Uses unique naming to avoid collisions with other tests.
 */
@Slf4j
public class TestRevokeGrantsSystemTest extends BaseGrantSystemTest {

    private TestRunFixture fixture;

    @BeforeGroups(groups = {"revoke"})
    public void beforeGroupsRevoke() {
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

    @AfterGroups(groups = {"revoke"})
    public void afterGroupsRevoke() throws SQLException {
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
    private ChangesSummary revokeChanges;
    private PlaybookService revokePlaybookService;

    @Test(groups = {"revoke"})
    public void testCompileInitialGrants() throws URISyntaxException {
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
                .getResource("test-data/system-test/revoke-grants/roles.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());

        variables = createVariables();
        playbookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(rolesFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        changes = playbookService.compileChanges(false);
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

    @Test(dependsOnMethods = {"testCompileInitialGrants"}, groups = {"revoke"})
    public void testApplyInitialGrants() throws SQLException {
        playbookService.applyChanges(changes.changes());

        // Ensure that there are no more changes.
        ChangesSummary newChanges = playbookService.compileChanges(false);
        Assert.assertTrue(newChanges.changes().isEmpty(), "Expected no remaining changes after apply");

        // Validate that the role has the expected grants from playbook.
        List<Grant> grantsExpected = new ArrayList<>(List.of(
                new Grant("USAGE", "DATABASE", fixture.databaseName(), "ROLE", fixture.roleName()),
                new Grant("SELECT", "TABLE", fixture.qualifiedTableName(), "ROLE", fixture.roleName())
        ));
        grantsExpected.sort(Comparator.comparing(x -> x.toString().hashCode()));

        List<Grant> grants = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
        assertGrantsMatch(grants, grantsExpected);
    }

    @Test(dependsOnMethods = {"testApplyInitialGrants"}, groups = {"revoke"})
    public void testCreateExtraGrants() throws SQLException {
        // Create extra grants NOT covered by the playbook
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            // Grant MONITOR on database (not in playbook)
            connection.createStatement().execute(
                    String.format("GRANT MONITOR ON DATABASE %s TO ROLE %s",
                            fixture.databaseName(), fixture.roleName())
            );
            // Grant USAGE on schema (not in playbook)
            connection.createStatement().execute(
                    String.format("GRANT USAGE ON SCHEMA \"%s\".\"%s\" TO ROLE %s",
                            fixture.databaseName(), fixture.schemaName(), fixture.roleName())
            );
            // Grant INSERT on table (not in playbook, only SELECT is in playbook)
            connection.createStatement().execute(
                    String.format("GRANT INSERT ON TABLE \"%s\".\"%s\".\"%s\" TO ROLE %s",
                            fixture.databaseName(), fixture.schemaName(), fixture.tableName(), fixture.roleName())
            );
        }

        // Verify extra grants are now present
        List<Grant> grantsAfterExtra = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
        Assert.assertTrue(grantsAfterExtra.size() > 2, "Expected extra grants to be present");
    }

    @Test(dependsOnMethods = {"testCreateExtraGrants"}, groups = {"revoke"})
    public void testCompileRevoke() throws SQLException, URISyntaxException {
        // Grant the test role to SYSADMIN so it can revoke grants
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            connection.createStatement().execute(
                    String.format("GRANT ROLE %s TO ROLE %s", fixture.roleName(), snowflakeSysadminRole)
            );
        }
        
        File rolesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/revoke-grants/roles.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());
        
        revokePlaybookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(rolesFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        revokeChanges = revokePlaybookService.compileChanges(false);

        // Verify that revoke changes are generated (should revoke the extra grants)
        Assert.assertFalse(revokeChanges.changes().isEmpty(), "Expected revoke changes to be generated");
        
        // Validate that SQL statements are generated for revoke
        for (CompiledChanges change : revokeChanges.changes()) {
            // Check that revoke statements are present in the changes
            // The revoke statements should be in the ownershipGrantStatements or roleGrantStatements
            boolean hasStatements = !change.ownershipGrantStatements().isEmpty() || 
                                   !change.roleGrantStatements().isEmpty();
            if (hasStatements) {
                log.info("LEFT OVER" + change.toString());
            }
            Assert.assertTrue(hasStatements, "Expected valid SQL statements to be generated for revoke");
            
            // Log the statements for debugging
            log.info("Revoke changes for role {}: ownership statements: {}, role statements: {}", 
                    change.roleName(), 
                    change.ownershipGrantStatements().size(),
                    change.roleGrantStatements().size());
        }
    }

    @Test(dependsOnMethods = {"testCompileRevoke"}, groups = {"revoke"})
    public void testApplyRevoke() throws SQLException, InterruptedException, JsonProcessingException {
        revokePlaybookService.applyChanges(revokeChanges.changes());

        // Validate that only playbook grants remain (extra grants should be revoked)
        List<Grant> grantsExpected = new ArrayList<>(List.of(
                new Grant("USAGE", "DATABASE", fixture.databaseName(), "ROLE", fixture.roleName()),
                new Grant("SELECT", "TABLE", fixture.qualifiedTableName(), "ROLE", fixture.roleName())
        ));
        grantsExpected.sort(Comparator.comparing(x -> x.toString().hashCode()));

        // Snowflake appears to have eventual consistency on this metadata after the revoke.
        // We need to wait for the metadata to catch up.
        int timeoutSeconds = 60;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxTime = now.plusSeconds(timeoutSeconds);
        boolean success = false;
        while (LocalDateTime.now().isBefore(maxTime.plusSeconds(10)) && !success) {
            try {
                List<Grant> grants = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
                assertGrantsMatch(grants, grantsExpected);
                success = true;
            } catch (AssertionError e) {
                if (LocalDateTime.now().isAfter(maxTime)) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    log.info("Changes: {}", objectMapper.writeValueAsString(revokeChanges));
                    throw e;
                }
                log.info("Test assertion failed on this iteration. Trying again.");
                Thread.sleep(100);
            }
        }
    }

}
