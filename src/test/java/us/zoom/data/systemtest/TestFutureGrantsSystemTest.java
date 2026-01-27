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

/** System test for cortex agent future grants ({@code <AGENT>} vs {@code <CORTEX_AGENT>} name alignment). */
@Slf4j
public class TestFutureGrantsSystemTest extends BaseGrantSystemTest {

    private TestRunFixture fixture;
    private ChangesSummary changes;
    private PlaybookService playbookService;
    private Map<String, String> variables;

    @BeforeGroups(groups = {"future"})
    public void beforeGroupsFuture() {
        fixture = new TestRunFixtureBuilder(
                sysadminSnowflakeConnectionProvider,
                securityadminSnowflakeConnectionProvider)
                .withDatabase()
                .withSchema()
                .withRoles()
                .build();
        log.info("Future grants test fixture setup complete for {}", this.getClass().getSimpleName());
    }

    @AfterGroups(groups = {"future"})
    public void afterGroupsFuture() throws SQLException {
        if (fixture == null) {
            return;
        }
        log.info("Tearing down lifecycleManager for {}", this.getClass().getSimpleName());
        fixture.lifecycleManager().teardown();
    }

    @Test(groups = {"future"})
    public void testCompile() throws URISyntaxException {
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
            throw new RuntimeException("Unable to grant permissions to SYSADMIN.", e);
        }

        File playbookFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/future-grant-playbook.yml")).toURI());
        File profilesFile = new File(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("test-data/system-test/profiles.yml")).toURI());

        variables = new HashMap<>();
        variables.put("database", fixture.databaseName());
        variables.put("schema", fixture.schemaName());
        variables.put("role", fixture.roleName());
        variables.put("snowflake-user", snowflakeUser);
        variables.put("snowflake-account", snowflakeAccount);
        variables.put("snowflake-password", String.valueOf(snowflakePassword));
        variables.put("snowflake-authenticator", String.valueOf(snowflakeAuthenticator));
        variables.put("snowflake-sysadmin-role", snowflakeSysadminRole);
        variables.put("snowflake-securityadmin-role", snowflakeSecurityAdminRole);

        playbookService = new PlaybookServiceBuilder()
                .setPlaybookYamlStrings(playbookFile)
                .putAllVariables(variables)
                .setProfilesYamlString(profilesFile)
                .build();
        changes = playbookService.compileChanges(false);
        Assert.assertFalse(changes.changes().isEmpty(), "FUTURE grant changes");
    }

    @Test(dependsOnMethods = {"testCompile"}, groups = {"future"})
    public void testApply() throws SQLException {
        playbookService.applyChanges(changes.changes());

        ChangesSummary newChanges = playbookService.compileChanges(false);
        Assert.assertTrue(newChanges.changes().isEmpty(), "No changes after apply");
    }
}
