package us.zoom.data.systemtest;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.ChangesSummary;
import us.zoom.data.dfence.playbook.PlaybookService;
import us.zoom.data.dfence.playbook.PlaybookServiceBuilder;
import us.zoom.data.dfence.test.fixtures.LifecycleManager;
import us.zoom.data.dfence.test.fixtures.LifecycleWrapper;
import us.zoom.data.dfence.test.fixtures.SnowflakeConnectionProvider;
import us.zoom.data.dfence.test.fixtures.SnowflakeLifecycleObject;

@Slf4j
public class TestRunSystemTest extends SnowflakeSysTestBase {

  public static List<Grant> getRoleGrants(
      String roleName, SnowflakeConnectionProvider connectionProvider) throws SQLException {
    try (Connection connection = connectionProvider.getConnection()) {
      Statement statement = connection.createStatement();
      statement.execute(String.format("SHOW GRANTS TO ROLE %s", roleName));
      ResultSet resultSet = statement.getResultSet();
      List<Grant> grants = new ArrayList<>();
      while (resultSet.next()) {
        grants.add(
            new Grant(
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
  public void beforeGroupsA(ITestContext ctx) {
    String databaseName =
        String.format("TEST_DATABASE_%s", UUID.randomUUID().toString().replace("-", ""))
            .toUpperCase();
    String roleName =
        String.format("TEST_ROLE_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
    String roleName2 =
        String.format("TEST_ROLE2_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
    String schemaName = "GROUP_A_SCHEMA";
    String tableName = "GROUP_A_TABLE";
    String procedureNamePartial = "GROUP_A_PROCEDURE";
    String procedureName = procedureNamePartial + "(STRING, STRING, NUMBER)";
    String procedureNameShowGrantsQual =
        String.format(
            "%s.%s.\"%s(FROM_TABLE VARCHAR, TO_TABLE VARCHAR, COUNT NUMBER):VARCHAR\"",
            databaseName, schemaName, procedureNamePartial);
    String procedureNameNoArgsShowGrantsQual =
        String.format("%s.%s.\"%s():VARCHAR\"", databaseName, schemaName, procedureNamePartial);
    LifecycleManager lifecycleManager = new LifecycleManager();
    ctx.setAttribute("database-name", databaseName);
    ctx.setAttribute("schema-name", schemaName);
    ctx.setAttribute("table-name", tableName);
    ctx.setAttribute("lifecycle-manager", lifecycleManager);
    ctx.setAttribute("role-name", roleName);
    ctx.setAttribute("role-name2", roleName2);
    ctx.setAttribute("procedure-name-show-grants-qual", procedureNameShowGrantsQual);
    ctx.setAttribute("procedure-name-no-args-show-grants-qual", procedureNameNoArgsShowGrantsQual);
    lifecycleManager.getLifecycleObjects().clear();
    lifecycleManager
        .getLifecycleObjects()
        .add(SnowflakeLifecycleObject.database(sysadminSnowflakeConnectionProvider, databaseName));
    lifecycleManager
        .getLifecycleObjects()
        .add(
            SnowflakeLifecycleObject.schema(
                sysadminSnowflakeConnectionProvider, databaseName, schemaName));
    lifecycleManager
        .getLifecycleObjects()
        .add(
            SnowflakeLifecycleObject.table(
                sysadminSnowflakeConnectionProvider, databaseName, schemaName, tableName));
    lifecycleManager
        .getLifecycleObjects()
        .add(
            LifecycleWrapper.builder()
                .performSetup(false)
                .exists(true)
                .wraps(
                    SnowflakeLifecycleObject.role(
                        securityadminSnowflakeConnectionProvider, roleName))
                .build());
    lifecycleManager
        .getLifecycleObjects()
        .add(
            LifecycleWrapper.builder()
                .performSetup(false)
                .exists(true)
                .wraps(
                    SnowflakeLifecycleObject.role(
                        securityadminSnowflakeConnectionProvider, roleName2))
                .build());
    lifecycleManager
        .getLifecycleObjects()
        .add(
            new SnowflakeLifecycleObject(
                sysadminSnowflakeConnectionProvider,
                String.format(
                    """
                        CREATE OR REPLACE PROCEDURE "%s"."%s"."%s"(from_table STRING, to_table STRING, count INT)
                          RETURNS STRING
                          LANGUAGE PYTHON
                          RUNTIME_VERSION = '3.9'
                          PACKAGES = ('snowflake-snowpark-python')
                          HANDLER = 'run'
                        AS
                        $$
                        def run(session, from_table, to_table, count):
                          session.table(from_table).limit(count).write.save_as_table(to_table)
                          return "SUCCESS"
                        $$;
                        """,
                    databaseName, schemaName, procedureNamePartial),
                String.format(
                    "DROP PROCEDURE IF EXISTS \"%s\".\"%s\".\"%s\"(STRING, STRING, INT)",
                    databaseName, schemaName, procedureNamePartial)));

    lifecycleManager
        .getLifecycleObjects()
        .add(
            new SnowflakeLifecycleObject(
                sysadminSnowflakeConnectionProvider,
                String.format(
                    """
                        CREATE OR REPLACE PROCEDURE "%s"."%s"."%s"()
                          RETURNS STRING
                          LANGUAGE PYTHON
                          RUNTIME_VERSION = '3.9'
                          PACKAGES = ('snowflake-snowpark-python')
                          HANDLER = 'run'
                        AS
                        $$
                        def run(session):
                          return "SUCCESS"
                        $$;
                        """,
                    databaseName, schemaName, procedureNamePartial),
                String.format(
                    "DROP PROCEDURE IF EXISTS \"%s\".\"%s\".\"%s\"()",
                    databaseName, schemaName, procedureNamePartial)));

    // Setup
    log.info("Setting up lifecycleManager for {}", this.getClass().getSimpleName());
    lifecycleManager.setup();
  }

  @AfterGroups(groups = {"a"})
  public void afterGroupsA(ITestContext ctx) throws SQLException {
    try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
      connection
          .createStatement()
          .execute(
              String.format(
                  "GRANT OWNERSHIP ON ALL TABLES IN DATABASE %s TO ROLE %S REVOKE CURRENT GRANTS",
                  ctx.getAttribute("database-name"), snowflakeSysadminRole));
      connection
          .createStatement()
          .execute(
              String.format(
                  "GRANT OWNERSHIP ON ALL PROCEDURES IN DATABASE %s TO ROLE %S REVOKE CURRENT GRANTS",
                  ctx.getAttribute("database-name"), snowflakeSysadminRole));
    }
    LifecycleManager lifecycleManager = (LifecycleManager) ctx.getAttribute("lifecycle-manager");
    log.info("Tearing down lifecycleManager for {}", this.getClass().getSimpleName());
    lifecycleManager.teardown();
  }

  @Test(groups = {"a"})
  public void testCompile(ITestContext ctx) throws URISyntaxException {
    String databaseName = (String) ctx.getAttribute("database-name");
    String schemaName = (String) ctx.getAttribute("schema-name");
    String tableName = (String) ctx.getAttribute("table-name");
    String roleName = (String) ctx.getAttribute("role-name");
    String roleName2 = (String) ctx.getAttribute("role-name2");
    String procedureName = (String) ctx.getAttribute("procedure-name");

    try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
      connection
          .createStatement()
          .execute(
              String.format(
                  "GRANT USAGE, MONITOR ON DATABASE %s TO ROLE %s",
                  databaseName, snowflakeSysadminRole));
      connection
          .createStatement()
          .execute(
              String.format(
                  "GRANT USAGE, MONITOR ON ALL SCHEMAS IN DATABASE %s TO ROLE %s",
                  databaseName, snowflakeSysadminRole));
    } catch (SQLException e) {
      throw new RuntimeException("Unable to grant permissions on objects to SYSADMIN role.", e);
    }

    File rolesFile =
        new File(
            Objects.requireNonNull(
                    this.getClass().getClassLoader().getResource("test-data/system-test/roles.yml"))
                .toURI());
    File profilesFile =
        new File(
            Objects.requireNonNull(
                    this.getClass()
                        .getClassLoader()
                        .getResource("test-data/system-test/profiles.yml"))
                .toURI());

    Map<String, String> variables = new HashMap<>();
    variables.put("database", databaseName);
    variables.put("schema", schemaName);
    variables.put("table", tableName);
    variables.put("role", roleName);
    variables.put("role2", roleName2);
    variables.put("procedure", procedureName);
    variables.put("snowflake-user", snowflakeUser);
    variables.put("snowflake-account", snowflakeAccount);
    variables.put("snowflake-password", String.valueOf(snowflakePassword));
    variables.put("snowflake-authenticator", String.valueOf(snowflakeAuthenticator));
    variables.put("snowflake-sysadmin-role", snowflakeSysadminRole);
    variables.put("snowflake-securityadmin-role", snowflakeSecurityAdminRole);
    PlaybookService playbookService =
        new PlaybookServiceBuilder()
            .setPlaybookYamlStrings(rolesFile)
            .putAllVariables(variables)
            .setProfilesYamlString(profilesFile)
            .build();
    ChangesSummary changes = playbookService.compileChanges(false);
    ctx.setAttribute("changes", changes);
    ctx.setAttribute("playbook-service", playbookService);
    ctx.setAttribute("variables", variables);
  }

  @Test(
      dependsOnMethods = {"testCompile"},
      groups = {"a"})
  public void testApply(ITestContext ctx) throws SQLException {
    ChangesSummary changes = (ChangesSummary) ctx.getAttribute("changes");
    PlaybookService playbookService = (PlaybookService) ctx.getAttribute("playbook-service");
    playbookService.applyChanges(changes.changes());
    String roleName = (String) Objects.requireNonNull(ctx.getAttribute("role-name"));
    String databaseName = (String) Objects.requireNonNull(ctx.getAttribute("database-name"));
    String schemaName = (String) Objects.requireNonNull(ctx.getAttribute("schema-name"));
    String tableName = (String) Objects.requireNonNull(ctx.getAttribute("table-name"));
    String procedureNameShowGrantsQual =
        (String) ctx.getAttribute("procedure-name-show-grants-qual");
    String procedureNameNoArgsShowGrantsQual =
        (String) ctx.getAttribute("procedure-name-no-args-show-grants-qual");

    // Ensure that there are no more changes.
    ChangesSummary newChanges = playbookService.compileChanges(false);
    assert newChanges.changes().isEmpty();

    // Validate that the role has the expected grants.
    List<Grant> grantsExpected =
        new ArrayList<>(
            List.of(
                new Grant("USAGE", "DATABASE", databaseName, "ROLE", roleName),
                new Grant("MONITOR", "DATABASE", databaseName, "ROLE", roleName),
                new Grant(
                    "USAGE",
                    "SCHEMA",
                    String.join(".", databaseName, schemaName),
                    "ROLE",
                    roleName),
                new Grant(
                    "MONITOR",
                    "SCHEMA",
                    String.join(".", databaseName, schemaName),
                    "ROLE",
                    roleName),
                new Grant(
                    "CREATE SEMANTIC VIEW",
                    "SCHEMA",
                    String.join(".", databaseName, schemaName),
                    "ROLE",
                    roleName),
                new Grant(
                    "SELECT",
                    "TABLE",
                    String.join(".", databaseName, schemaName, tableName),
                    "ROLE",
                    roleName),
                new Grant(
                    "OWNERSHIP",
                    "TABLE",
                    String.join(".", databaseName, schemaName, tableName),
                    "ROLE",
                    roleName),
                new Grant(
                    "USAGE", "APPLICATION_ROLE", "SNOWFLAKE.TRUST_CENTER_VIEWER", "ROLE", roleName),
                new Grant("USAGE", "PROCEDURE", procedureNameShowGrantsQual, "ROLE", roleName),
                new Grant(
                    "USAGE", "PROCEDURE", procedureNameNoArgsShowGrantsQual, "ROLE", roleName),
                new Grant("OWNERSHIP", "PROCEDURE", procedureNameShowGrantsQual, "ROLE", roleName),
                new Grant(
                    "OWNERSHIP",
                    "PROCEDURE",
                    procedureNameNoArgsShowGrantsQual,
                    "ROLE",
                    roleName)));
    grantsExpected.sort(Comparator.comparing(x -> x.toString().hashCode()));

    List<Grant> grants = getRoleGrants(roleName, securityadminSnowflakeConnectionProvider);
    grants.sort(Comparator.comparing(x -> x.toString().hashCode()));

    Assert.assertEquals(Set.copyOf(grants), Set.copyOf(grantsExpected));
  }

  @Test(
      dependsOnMethods = {"testApply"},
      groups = {"a"})
  public void testCompileRevoke(ITestContext ctx) throws SQLException, URISyntaxException {
    String roleName = (String) Objects.requireNonNull(ctx.getAttribute("role-name"));
    Map<String, String> variables =
        (Map<String, String>) Objects.requireNonNull(ctx.getAttribute("variables"));
    try (Connection connection = securityadminSnowflakeConnectionProvider.getConnection()) {
      connection
          .createStatement()
          .execute(String.format("GRANT ROLE %s TO ROLE %s", roleName, snowflakeSysadminRole));
    }
    File rolesFile =
        new File(
            Objects.requireNonNull(
                    this.getClass()
                        .getClassLoader()
                        .getResource("test-data/system-test/roles_no_grants.yml"))
                .toURI());
    File profilesFile =
        new File(
            Objects.requireNonNull(
                    this.getClass()
                        .getClassLoader()
                        .getResource("test-data/system-test/profiles.yml"))
                .toURI());
    PlaybookService playbookService =
        new PlaybookServiceBuilder()
            .setPlaybookYamlStrings(rolesFile)
            .putAllVariables(variables)
            .setProfilesYamlString(profilesFile)
            .build();
    ChangesSummary changes = playbookService.compileChanges(false);
    Assert.assertFalse(changes.changes().isEmpty(), "No changes found. Revokes are expected.");
    ctx.setAttribute("changes-revoke", changes);
    ctx.setAttribute("playbook-service-revoke", playbookService);
  }

  @Test(
      dependsOnMethods = {"testCompileRevoke"},
      groups = {"a"})
  public void testApplyRevoke(ITestContext ctx)
      throws SQLException, InterruptedException, JsonProcessingException {
    ChangesSummary changes = (ChangesSummary) ctx.getAttribute("changes-revoke");
    PlaybookService playbookService = (PlaybookService) ctx.getAttribute("playbook-service-revoke");
    playbookService.applyChanges(changes.changes());
    String roleName = (String) Objects.requireNonNull(ctx.getAttribute("role-name"));
    String roleName2 = (String) Objects.requireNonNull(ctx.getAttribute("role-name2"));
    String databaseName = (String) Objects.requireNonNull(ctx.getAttribute("database-name"));
    String schemaName = (String) Objects.requireNonNull(ctx.getAttribute("schema-name"));
    String tableName = (String) Objects.requireNonNull(ctx.getAttribute("table-name"));
    String procedureNameShowGrantsQual =
        (String) ctx.getAttribute("procedure-name-show-grants-qual");
    String procedureNameNoArgsShowGrantsQual =
        (String) ctx.getAttribute("procedure-name-no-args-show-grants-qual");

    // Validate that the role has the expected grants.
    List<Grant> grantsExpected = new ArrayList<>(List.of());
    grantsExpected.sort(Comparator.comparing(x -> x.toString().hashCode()));

    // Snowflake appears to have eventual consistency on this metadata after the ownership change.
    // We need to wait
    // for the metadata to catch up.
    int timeoutSeconds = 480;
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime maxTime = now.plusSeconds(timeoutSeconds);
    boolean success = false;
    while (LocalDateTime.now().isBefore(maxTime.plusSeconds(10)) && !success) {
      try {
        List<Grant> grants = getRoleGrants(roleName, securityadminSnowflakeConnectionProvider);
        grants.sort(Comparator.comparing(x -> x.toString().hashCode()));
        Assert.assertEquals(Set.copyOf(grants), Set.copyOf(grantsExpected));
        List<Grant> grants2 = getRoleGrants(roleName2, securityadminSnowflakeConnectionProvider);
        grants2.sort(Comparator.comparing(x -> x.toString().hashCode()));
        List<Grant> grantsExpected2 =
            new ArrayList<>(
                List.of(
                    new Grant(
                        "OWNERSHIP",
                        "TABLE",
                        String.join(".", databaseName, schemaName, tableName),
                        "ROLE",
                        roleName2),
                    new Grant(
                        "OWNERSHIP", "PROCEDURE", procedureNameShowGrantsQual, "ROLE", roleName2),
                    new Grant(
                        "OWNERSHIP",
                        "PROCEDURE",
                        procedureNameNoArgsShowGrantsQual,
                        "ROLE",
                        roleName2)));
        Assert.assertEquals(Set.copyOf(grants2), Set.copyOf(grantsExpected2));
        success = true;
      } catch (AssertionError e) {
        if (LocalDateTime.now().isAfter(maxTime)) {
          ObjectMapper objectMapper = new ObjectMapper();
          log.info("Changes: {}", objectMapper.writeValueAsString(changes));
          throw e;
        }
        log.info("Test assertion failed on this iteration. Trying again.");
        Thread.sleep(100);
      }
    }
  }

  public record Grant(
      String privilege, String grantedOn, String name, String grantedTo, String granteeName) {}
}
