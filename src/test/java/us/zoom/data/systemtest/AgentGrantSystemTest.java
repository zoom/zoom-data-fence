package us.zoom.data.systemtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.*;
import us.zoom.data.dfence.ChangesSummary;
import us.zoom.data.dfence.playbook.PlaybookService;
import us.zoom.data.dfence.playbook.PlaybookServiceBuilder;
import us.zoom.data.dfence.test.fixtures.AgentTestFixture;
import us.zoom.data.dfence.test.fixtures.AgentTestFixtureBuilder;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * System test for agent and semantic view grants.
 * Uses unique naming (AGENT_TEST_*) to avoid collisions with other tests.
 */
@Slf4j
public class AgentGrantSystemTest extends BaseGrantSystemTest {

    private AgentTestFixture fixture;

    // Test state - stored as instance variables instead of ITestContext
    private ChangesSummary changes;
    private PlaybookService playbookService;
    private Map<String, String> variables;
    private ChangesSummary revokeChanges;
    private PlaybookService revokePlaybookService;

    @BeforeGroups(groups = {"agent"})
    public void beforeGroupsAgent() {
        fixture = new AgentTestFixtureBuilder(
                sysadminSnowflakeConnectionProvider,
                securityadminSnowflakeConnectionProvider)
                .withDatabase()
                .withSchema()
                .withTable()
                .withSemanticView()
                .withAgent()
                .withRoles()
                .build();
        log.info("Agent test fixture setup complete for {}", this.getClass().getSimpleName());
    }

    @AfterGroups(groups = {"agent"})
    public void afterGroupsAgent() throws SQLException {
        if (fixture == null) {
            return; // Nothing to clean up
        }

        // Transfer ownership back to SYSADMIN before teardown
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            connection.createStatement().execute(
                    String.format("GRANT OWNERSHIP ON ALL AGENTS IN DATABASE %s TO ROLE %S REVOKE CURRENT GRANTS",
                            fixture.databaseName(), snowflakeSysadminRole)
            );
            connection.createStatement().execute(
                    String.format("GRANT OWNERSHIP ON ALL SEMANTIC VIEWS IN DATABASE %s TO ROLE %S REVOKE CURRENT GRANTS",
                            fixture.databaseName(), snowflakeSysadminRole)
            );
        }

        log.info("Tearing down lifecycleManager for {}", this.getClass().getSimpleName());
        fixture.lifecycleManager().teardown();
    }

    @Test(groups = {"agent"})
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
                .getResource("test-data/system-test/agent-grants.yml")).toURI());
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
        vars.put("semantic-view", fixture.semanticViewName());
        vars.put("agent", fixture.agentName());
        vars.put("role", fixture.roleName());
        vars.put("role2", fixture.roleName2());
        vars.put("snowflake-user", snowflakeUser);
        vars.put("snowflake-account", snowflakeAccount);
        vars.put("snowflake-password", String.valueOf(snowflakePassword));
        vars.put("snowflake-authenticator", String.valueOf(snowflakeAuthenticator));
        vars.put("snowflake-sysadmin-role", snowflakeSysadminRole);
        vars.put("snowflake-securityadmin-role", snowflakeSecurityAdminRole);
        return vars;
    }

    @Test(dependsOnMethods = {"testCompile"}, groups = {"agent"})
    public void testApply() throws SQLException {
        playbookService.applyChanges(changes.changes());

        // Ensure that there are no more changes.
        ChangesSummary newChanges = playbookService.compileChanges(false);
        Assert.assertTrue(newChanges.changes().isEmpty(), "Expected no remaining changes after apply");

        // Validate that the role has the expected grants.
        List<Grant> grantsExpected = new ArrayList<>(List.of(
                new Grant("USAGE", "DATABASE", fixture.databaseName(), "ROLE", fixture.roleName()),
                new Grant("MONITOR", "DATABASE", fixture.databaseName(), "ROLE", fixture.roleName()),
                new Grant("USAGE", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("MONITOR", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("CREATE SEMANTIC VIEW", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("CREATE AGENT", "SCHEMA", fixture.qualifiedSchemaName(), "ROLE", fixture.roleName()),
                new Grant("SELECT", "SEMANTIC VIEW", fixture.qualifiedSemanticViewName(), "ROLE", fixture.roleName()),
                new Grant("REFERENCES", "SEMANTIC VIEW", fixture.qualifiedSemanticViewName(), "ROLE", fixture.roleName()),
                new Grant("USAGE", "CORTEX_AGENT", fixture.qualifiedAgentName(), "ROLE", fixture.roleName()),
                new Grant("MODIFY", "CORTEX_AGENT", fixture.qualifiedAgentName(), "ROLE", fixture.roleName()),
                new Grant("MONITOR", "CORTEX_AGENT", fixture.qualifiedAgentName(), "ROLE", fixture.roleName()),
                new Grant("OWNERSHIP", "CORTEX_AGENT", fixture.qualifiedAgentName(), "ROLE", fixture.roleName())
        ));

        List<Grant> grants = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
        assertGrantsMatch(grants, grantsExpected);
    }

    @Test(dependsOnMethods = {"testApply"}, groups = {"agent"})
    public void testCompileRevoke() throws SQLException, URISyntaxException {
        // Grant the test role to SYSADMIN so it can revoke grants
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            connection.createStatement().execute(
                    String.format("GRANT ROLE %s TO ROLE %s", fixture.roleName(), snowflakeSysadminRole)
            );
        }

        File rolesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/agent-grants-no-grants.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());

        revokePlaybookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(rolesFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        revokeChanges = revokePlaybookService.compileChanges(false);
        Assert.assertFalse(revokeChanges.changes().isEmpty(), "No changes found. Revokes are expected.");
    }

    @Test(dependsOnMethods = {"testCompileRevoke"}, groups = {"agent"})
    public void testApplyRevoke() throws SQLException, InterruptedException, JsonProcessingException {
        revokePlaybookService.applyChanges(revokeChanges.changes());

        // Validate that role1 has no grants (all revoked)
        // Note: Ownership will be transferred back to the original owner (SYSADMIN) or revoked
        List<Grant> grantsExpectedRole1 = new ArrayList<>(List.of());

        // Snowflake appears to have eventual consistency on this metadata after the ownership change.
        // We need to wait for the metadata to catch up.
        int timeoutSeconds = 480;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxTime = now.plusSeconds(timeoutSeconds);
        boolean success = false;
        while (LocalDateTime.now().isBefore(maxTime.plusSeconds(10)) && !success) {
            try {
                // Verify role1 has no grants (all revoked)
                List<Grant> grantsRole1 = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
                assertGrantsMatch(grantsRole1, grantsExpectedRole1);

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

