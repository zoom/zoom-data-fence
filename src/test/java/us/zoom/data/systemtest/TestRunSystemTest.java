package us.zoom.data.systemtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.*;
import us.zoom.data.dfence.ChangesSummary;
import us.zoom.data.dfence.playbook.PlaybookService;
import us.zoom.data.dfence.playbook.PlaybookServiceBuilder;
import us.zoom.data.dfence.test.fixtures.SnowflakeConnectionProvider;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class TestRunSystemTest extends SnowflakeSysTestBase {

    private TestRunFixture fixture;

    public static List<Grant> getRoleGrants(String roleName, SnowflakeConnectionProvider connectionProvider)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute(String.format("SHOW GRANTS TO ROLE %s", roleName));
            ResultSet resultSet = statement.getResultSet();
            List<Grant> grants = new ArrayList<>();
            while (resultSet.next()) {
                grants.add(new Grant(
                        resultSet.getString("privilege"),
                        resultSet.getString("granted_on"),
                        resultSet.getString("name"),
                        resultSet.getString("granted_to"),
                        resultSet.getString("grantee_name")));
            }
            return grants;
        }
    }

    @BeforeGroups(groups = {"a"})
    public void beforeGroupsA() {
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

    @AfterGroups(groups = {"a"})
    public void afterGroupsA() throws SQLException {
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


    // Test state - stored as instance variables instead of ITestContext
    private ChangesSummary changes;
    private PlaybookService playbookService;
    private Map<String, String> variables;
    private ChangesSummary revokeChanges;
    private PlaybookService revokePlaybookService;

    @Test(groups = {"a"})
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
                .getResource("test-data/system-test/roles.yml")).toURI());
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

    @Test(dependsOnMethods = {"testCompile"}, groups = {"a"})
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
                new Grant("SELECT", "TABLE", fixture.qualifiedTableName(), "ROLE", fixture.roleName()),
                new Grant("OWNERSHIP", "TABLE", fixture.qualifiedTableName(), "ROLE", fixture.roleName()),
                new Grant("USAGE", "APPLICATION_ROLE", "SNOWFLAKE.TRUST_CENTER_VIEWER", "ROLE", fixture.roleName()),
                new Grant("USAGE", "PROCEDURE", fixture.procedureNameShowGrantsQual(), "ROLE", fixture.roleName()),
                new Grant("USAGE", "PROCEDURE", fixture.procedureNameNoArgsShowGrantsQual(), "ROLE", fixture.roleName()),
                new Grant("OWNERSHIP", "PROCEDURE", fixture.procedureNameShowGrantsQual(), "ROLE", fixture.roleName()),
                new Grant("OWNERSHIP", "PROCEDURE", fixture.procedureNameNoArgsShowGrantsQual(), "ROLE", fixture.roleName())
        ));
        grantsExpected.sort(Comparator.comparing(x -> x.toString().hashCode()));

        List<Grant> grants = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
        grants.sort(Comparator.comparing(x -> x.toString().hashCode()));

        Assert.assertEquals(Set.copyOf(grants), Set.copyOf(grantsExpected));
    }

    @Test(dependsOnMethods = {"testApply"}, groups = {"a"})
    public void testCompileRevoke() throws SQLException, URISyntaxException {
        // Grant the test role to SYSADMIN so it can revoke grants
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            connection.createStatement().execute(
                    String.format("GRANT ROLE %s TO ROLE %s", fixture.roleName(), snowflakeSysadminRole)
            );
        }
        
        File rolesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/roles_no_grants.yml")).toURI());
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

    @Test(dependsOnMethods = {"testCompileRevoke"}, groups = {"a"})
    public void testApplyRevoke() throws SQLException, InterruptedException, JsonProcessingException {
        revokePlaybookService.applyChanges(revokeChanges.changes());

        // Validate that role1 has no grants (all revoked)
        List<Grant> grantsExpectedRole1 = new ArrayList<>(List.of());
        grantsExpectedRole1.sort(Comparator.comparing(x -> x.toString().hashCode()));

        // Validate that role2 has ownership grants (transferred from role1)
        List<Grant> grantsExpectedRole2 = new ArrayList<>(List.of(
                new Grant("OWNERSHIP", "TABLE", fixture.qualifiedTableName(), "ROLE", fixture.roleName2()),
                new Grant("OWNERSHIP", "PROCEDURE", fixture.procedureNameShowGrantsQual(), "ROLE", fixture.roleName2()),
                new Grant("OWNERSHIP", "PROCEDURE", fixture.procedureNameNoArgsShowGrantsQual(), "ROLE", fixture.roleName2())
        ));
        grantsExpectedRole2.sort(Comparator.comparing(x -> x.toString().hashCode()));

        // Snowflake appears to have eventual consistency on this metadata after the ownership change.
        // We need to wait for the metadata to catch up.
        int timeoutSeconds = 480;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxTime = now.plusSeconds(timeoutSeconds);
        boolean success = false;
        while (LocalDateTime.now().isBefore(maxTime.plusSeconds(10)) && !success) {
            try {
                // Verify role1 has no grants
                List<Grant> grantsRole1 = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
                grantsRole1.sort(Comparator.comparing(x -> x.toString().hashCode()));
                Assert.assertEquals(Set.copyOf(grantsRole1), Set.copyOf(grantsExpectedRole1),
                        "Role1 should have no grants after revoke");

                // Verify role2 has ownership grants
                List<Grant> grantsRole2 = getRoleGrants(fixture.roleName2(), securityadminSnowflakeConnectionProvider);
                grantsRole2.sort(Comparator.comparing(x -> x.toString().hashCode()));
                Assert.assertEquals(Set.copyOf(grantsRole2), Set.copyOf(grantsExpectedRole2),
                        "Role2 should have ownership grants");
                
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



    public record Grant(String privilege, String grantedOn, String name, String grantedTo, String granteeName) {
    }

}
