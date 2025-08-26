package us.zoom.data.dfence.playbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;

import us.zoom.data.dfence.Provider;
import us.zoom.data.dfence.exception.VariableNotFoundException;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;

class PlaybookTest {

  static final PlaybookModel playbook =
      new PlaybookModel(
          Map.of(
              "rbac-example-role-1",
              new PlaybookRoleModel(
                  "rbac_example_1",
                  List.of(
                      new PlaybookPrivilegeGrant(
                          "database", null, null, "my_db", List.of("usage"), true, true),
                      new PlaybookPrivilegeGrant(
                          "schema", null, "my_schema", "my_db", List.of("usage"), true, true),
                      new PlaybookPrivilegeGrant(
                          "table", "my_table", "my_schema", "my_db", List.of("select"), true, true),
                      new PlaybookPrivilegeGrant(
                          "external table",
                          "my_table_2",
                          "my_schema",
                          "my_db",
                          List.of("select"),
                          true,
                          true))),
              "rbac-example-role-2",
              new PlaybookRoleModel(
                  "rbac_example_2",
                  List.of(
                      new PlaybookPrivilegeGrant(
                          "role", "rbac_example_1", null, null, List.of("usage"), true, true),
                      new PlaybookPrivilegeGrant(
                          "account", null, null, null, List.of("monitor"), true, true)))));
  String rolesFileName = "test-data/roles.yml";
  String rolesWithEnvVarFileName = "test-data/roles-with-env-var.yml";
  File rolesFile;
  File rolesWithEnvVarFile;
  String playbookYaml;
  AutoCloseable mocks;

  @Mock Provider provider;

  public static Stream<FilterPlaybookParams> filterPlaybookParamsStream() {
    return Stream.of(
        new FilterPlaybookParams(
            new PlaybookModel(
                Map.of(
                    "mock-role-enabled",
                    new PlaybookRoleModel("mock_role_enabled", List.of()),
                    "mock-role-disabled",
                    new PlaybookRoleModel(
                        "mock_role_disabled",
                        List.of(),
                        true,
                        true,
                        false,
                        null,
                        UnsupportedRevokeBehavior.IGNORE))),
            new PlaybookModel(
                Map.of(
                    "mock-role-enabled", new PlaybookRoleModel("mock_role_enabled", List.of())))),
        new FilterPlaybookParams(
            new PlaybookModel(
                Map.of(
                    "mock-role-enabled",
                    new PlaybookRoleModel(
                        "mock_role_enabled",
                        List.of(
                            new PlaybookPrivilegeGrant(
                                "table-enabled",
                                "mock_table",
                                "mock_schema",
                                "mock_database",
                                List.of("SELECT"),
                                true,
                                true,
                                true),
                            new PlaybookPrivilegeGrant(
                                "table-disabled",
                                "mock_table",
                                "mock_schema",
                                "mock_database",
                                List.of("SELECT"),
                                true,
                                true,
                                false))))),
            new PlaybookModel(
                Map.of(
                    "mock-role-enabled",
                    new PlaybookRoleModel(
                        "mock_role_enabled",
                        List.of(
                            new PlaybookPrivilegeGrant(
                                "table-enabled",
                                "mock_table",
                                "mock_schema",
                                "mock_database",
                                List.of("SELECT"),
                                true,
                                true,
                                true)))))));
  }

  @BeforeEach
  void setUp() throws IOException {
    mocks = MockitoAnnotations.openMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();
    rolesFile = new File(Objects.requireNonNull(classLoader.getResource(rolesFileName)).getFile());
    rolesWithEnvVarFile =
        new File(Objects.requireNonNull(classLoader.getResource(rolesFileName)).getFile());
    playbookYaml = Files.readString(rolesFile.toPath());
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void parse() throws VariableNotFoundException, JsonProcessingException {
    PlaybookModel playbookActual = Playbook.parse(playbookYaml, new HashMap<>());
    assertEquals(playbook, playbookActual);
  }

  @Test
  void parseWithEnvVars() throws VariableNotFoundException, JsonProcessingException {
    PlaybookModel playbookActual = Playbook.parse(playbookYaml, Map.of("db-name", "my_db"));
    assertEquals(playbook, playbookActual);
  }

  @Test
  void serializeParseRoundTrip() throws JsonProcessingException, VariableNotFoundException {
    String playbookYamlActual = Playbook.serialize(playbook);
    PlaybookModel playbookModelActual = Playbook.parse(playbookYamlActual, new HashMap<>());
    assertEquals(playbook, playbookModelActual);
  }

  @Test
  void consolidatePrivileges() {
    List<PlaybookPrivilegeGrant> grants =
        List.of(
            new PlaybookPrivilegeGrant(
                "table",
                "mock_table_a",
                "mock_schema_a",
                "mock_db_a",
                List.of("select"),
                true,
                true),
            new PlaybookPrivilegeGrant(
                "table",
                "mock_table_a",
                "mock_schema_a",
                "mock_db_a",
                List.of("update"),
                true,
                true),
            new PlaybookPrivilegeGrant(
                "table",
                "mock_table_b",
                "mock_schema_a",
                "mock_db_a",
                List.of("delete"),
                true,
                true));
    List<PlaybookPrivilegeGrant> expected =
        List.of(
            new PlaybookPrivilegeGrant(
                "table",
                "mock_table_a",
                "mock_schema_a",
                "mock_db_a",
                List.of("select", "update"),
                true,
                true),
            new PlaybookPrivilegeGrant(
                "table",
                "mock_table_b",
                "mock_schema_a",
                "mock_db_a",
                List.of("delete"),
                true,
                true));
    List<PlaybookPrivilegeGrant> actual = Playbook.consolidatePrivileges(grants);
    assertEquals(Set.copyOf(expected), Set.copyOf(actual));
  }

  @Test
  void importRoles() {
    List<PlaybookRoleModel> rolesProvider =
        List.of(
            new PlaybookRoleModel(
                "role_a",
                List.of(
                    new PlaybookPrivilegeGrant(
                        "table",
                        "mock_table_a",
                        "mock_schema_a",
                        "mock_db_a",
                        List.of("select"),
                        true,
                        true),
                    new PlaybookPrivilegeGrant(
                        "table",
                        "mock_table_a",
                        "mock_schema_a",
                        "mock_db_a",
                        List.of("update"),
                        true,
                        true))),
            new PlaybookRoleModel(
                "role_b",
                List.of(
                    new PlaybookPrivilegeGrant(
                        "table",
                        "mock_table_a",
                        "mock_schema_a",
                        "mock_db_a",
                        List.of("select"),
                        true,
                        true))));
    List<String> roleNames = rolesProvider.stream().map(PlaybookRoleModel::name).toList();
    Map<String, PlaybookRoleModel> rolesExpected =
        Map.of(
            "role-a",
            new PlaybookRoleModel(
                "role_a",
                List.of(
                    new PlaybookPrivilegeGrant(
                        "table",
                        "mock_table_a",
                        "mock_schema_a",
                        "mock_db_a",
                        List.of("select", "update"),
                        true,
                        true))),
            "role-b",
            new PlaybookRoleModel(
                "role_b",
                List.of(
                    new PlaybookPrivilegeGrant(
                        "table",
                        "mock_table_a",
                        "mock_schema_a",
                        "mock_db_a",
                        List.of("select"),
                        true,
                        true))));
    PlaybookModel playbookModelExpected = new PlaybookModel(rolesExpected);
    when(provider.importRoles(roleNames)).thenReturn(rolesProvider);
    PlaybookModel playbookModelActual = PlaybookImport.importRoles(roleNames, provider);
    assertEquals(playbookModelExpected, playbookModelActual);
  }

  @ParameterizedTest
  @MethodSource("filterPlaybookParamsStream")
  void filterPlaybook(FilterPlaybookParams params) {
    PlaybookModel actual = Playbook.filterPlaybook(params.playbookModel);
    assertEquals(params.expectedPlaybookModel, actual);
  }

  @Test
  void propagateDefaults() {
    PlaybookModel playbookModel =
        new PlaybookModel(
            Map.of(
                "role-a",
                new PlaybookRoleModel(
                    "role_a", List.of(), true, true, true, null, UnsupportedRevokeBehavior.IGNORE)),
            "SECURITYADMIN");
    PlaybookModel playbookModelExpected =
        new PlaybookModel(
            Map.of(
                "role-a",
                new PlaybookRoleModel(
                    "role_a",
                    List.of(),
                    true,
                    true,
                    true,
                    "SECURITYADMIN",
                    UnsupportedRevokeBehavior.IGNORE)),
            "SECURITYADMIN");
    assertEquals(playbookModelExpected, Playbook.propagateDefaults(playbookModel));
  }

  public record FilterPlaybookParams(
      PlaybookModel playbookModel, PlaybookModel expectedPlaybookModel) {}
}
