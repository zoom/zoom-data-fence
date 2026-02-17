package us.zoom.data.systemtest;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import us.zoom.data.dfence.ChangesSummary;
import us.zoom.data.dfence.playbook.PlaybookService;
import us.zoom.data.dfence.playbook.PlaybookServiceBuilder;
import us.zoom.data.dfence.test.fixtures.DatabaseRoleTestFixture;
import us.zoom.data.dfence.test.fixtures.DatabaseRoleTestFixtureBuilder;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * System test for database role grant functionality.
 * Creates a database role, grants USAGE on it to a role via the playbook, and verifies the grant.
 */
@Slf4j
public class DatabaseRoleGrantSystemTest extends BaseGrantSystemTest {

    private DatabaseRoleTestFixture fixture;

    private ChangesSummary changes;
    private PlaybookService playbookService;
    private Map<String, String> variables;

    @BeforeGroups(groups = {"database-role"})
    public void beforeGroupsDatabaseRole() {
        fixture = new DatabaseRoleTestFixtureBuilder(
                sysadminSnowflakeConnectionProvider,
                securityadminSnowflakeConnectionProvider)
                .withDatabase()
                .withDatabaseRole()
                .withRoles()
                .build();
        log.info("Database role test fixture setup complete for {}", this.getClass().getSimpleName());
    }

    @AfterGroups(groups = {"database-role"})
    public void afterGroupsDatabaseRole() {
        if (fixture == null) {
            return;
        }
        log.info("Tearing down lifecycleManager for {}", this.getClass().getSimpleName());
        fixture.lifecycleManager().teardown();
    }

    @Test(groups = {"database-role"})
    public void testCompile() throws URISyntaxException {
        try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
            connection.createStatement().execute(
                    String.format("GRANT USAGE, MONITOR ON DATABASE %s TO ROLE %s",
                            fixture.databaseName(), snowflakeSysadminRole)
            );
        } catch (SQLException e) {
            throw new RuntimeException("Unable to grant permissions on database to SYSADMIN role.", e);
        }

        File rolesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/database-role-grants.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());

        variables = createVariables();
        playbookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(rolesFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        changes = playbookService.compileChanges(false);

        Assert.assertFalse(changes.changes().isEmpty(), "Expected changes to be generated");
    }

    private Map<String, String> createVariables() {
        Map<String, String> vars = new HashMap<>();
        vars.put("database", fixture.databaseName());
        vars.put("database-role", fixture.databaseRoleName());
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

    @Test(dependsOnMethods = {"testCompile"}, groups = {"database-role"})
    public void testApply() throws SQLException {
        playbookService.applyChanges(changes.changes());

        ChangesSummary newChanges = playbookService.compileChanges(false);
        Assert.assertTrue(newChanges.changes().isEmpty(), "Expected no remaining changes after apply");

        List<Grant> grantsExpected = new ArrayList<>(List.of(
                new Grant("USAGE", "DATABASE_ROLE", fixture.qualifiedDatabaseRoleName(), "ROLE", fixture.roleName())
        ));

        List<Grant> grants = getRoleGrants(fixture.roleName(), securityadminSnowflakeConnectionProvider);
        assertGrantsMatch(grants, grantsExpected);
    }
}
