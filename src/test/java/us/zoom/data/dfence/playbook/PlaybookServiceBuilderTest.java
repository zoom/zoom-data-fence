package us.zoom.data.dfence.playbook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.data.dfence.EnvironmentService;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.data.dfence.providers.snowflake.*;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PlaybookServiceBuilderTest {

    File profilesFile;

    File rolesFile;

    File rolesDirectory;

    File variablesFile;

    @Mock
    EnvironmentService environmentService;
    AutoCloseable mocks;

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }


    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        ClassLoader classLoader = getClass().getClassLoader();
        profilesFile = new File(Objects.requireNonNull(classLoader.getResource("test-data/project/profiles.yml"))
                .getFile());
        rolesFile = new File(Objects.requireNonNull(classLoader.getResource("test-data/project/roles.yml")).getFile());
        rolesDirectory = new File(Objects.requireNonNull(classLoader.getResource("test-data/project/roles-directory/"))
                .getFile());
        variablesFile = new File(Objects.requireNonNull(classLoader.getResource("test-data/project/vars.yml"))
                .getFile());
        when(environmentService.getEnv()).thenReturn(Map.of("DFENCE_USER", "mock_user"));
    }

    @Test
    void build() {
        SnowflakeConnectionService snowflakeConnectionServiceExpected
                = new SnowflakeConnectionService(new SnowflakeProviderConfigModel() {{
            setConnectionString("jdbc:snowflake://mock-url.localhost:443");
            setConnectionProperties(new SnowflakeConnectionProperties() {{
                setAuthenticator("externalbrowser");
                setUser("mock_user");
            }});
        }});
        String db_name = "mock_db";
        SnowflakeObjectsService snowflakeObjectsService
                = new SnowflakeObjectsService(snowflakeConnectionServiceExpected);
        PlaybookService playbookServiceExpected = new PlaybookService(
                new SnowflakeProvider(
                        new SnowflakeStatementsService(snowflakeConnectionServiceExpected),
                        new SnowflakeGrantsService(snowflakeConnectionServiceExpected),
                        new SnowflakeObjectsService(snowflakeConnectionServiceExpected)), new PlaybookModel(Map.of(
                "rbac-example-role-1",
                new PlaybookRoleModel(
                        "rbac_example_1",
                        List.of(new PlaybookPrivilegeGrant(
                                "database",
                                null,
                                null,
                                db_name,
                                List.of("usage"),
                                true,
                                true))),
                "rbac-example-role-2",
                new PlaybookRoleModel(
                        "rbac_example_2",
                        List.of(new PlaybookPrivilegeGrant(
                                "role",
                                "rbac_example_1",
                                null,
                                null,
                                List.of("usage"),
                                true,
                                true))))));
        PlaybookServiceBuilder playbookServiceBuilder
                = new PlaybookServiceBuilder().setEnvironmentService(this.environmentService)
                .setPlaybookYamlStrings(this.rolesFile).setProfilesYamlString(profilesFile)
                .putAllVariablesFile(variablesFile).putAllVariablesFromEnvironment();
        PlaybookService playbookService = playbookServiceBuilder.build();
        assertEquals(playbookServiceExpected.getPlaybookModel(), playbookService.getPlaybookModel());
        assertEquals(playbookServiceExpected.getProvider(), playbookService.getProvider());
    }

    @Test
    void buildRolesDirectory() {
        SnowflakeConnectionService snowflakeConnectionServiceExpected
                = new SnowflakeConnectionService(new SnowflakeProviderConfigModel() {{
            setConnectionString("jdbc:snowflake://mock-url.localhost:443");
            setConnectionProperties(new SnowflakeConnectionProperties() {{
                setAuthenticator("externalbrowser");
                setUser("mock_user");
            }});
        }});
        String db_name = "mock_db";
        SnowflakeObjectsService snowflakeObjectsService
                = new SnowflakeObjectsService(snowflakeConnectionServiceExpected);
        PlaybookService playbookServiceExpected = new PlaybookService(
                new SnowflakeProvider(
                        new SnowflakeStatementsService(snowflakeConnectionServiceExpected),
                        new SnowflakeGrantsService(snowflakeConnectionServiceExpected),
                        new SnowflakeObjectsService(snowflakeConnectionServiceExpected)), new PlaybookModel(Map.of(
                "rbac-example-role-1",
                new PlaybookRoleModel(
                        "rbac_example_1",
                        List.of(new PlaybookPrivilegeGrant(
                                "database",
                                null,
                                null,
                                db_name,
                                List.of("usage"),
                                true,
                                true))),
                "rbac-example-role-2",
                new PlaybookRoleModel(
                        "rbac_example_2",
                        List.of(new PlaybookPrivilegeGrant(
                                "role",
                                "rbac_example_1",
                                null,
                                null,
                                List.of("usage"),
                                true,
                                true))))));
        PlaybookServiceBuilder playbookServiceBuilder
                = new PlaybookServiceBuilder().setEnvironmentService(this.environmentService)
                .setPlaybookYamlStrings(this.rolesDirectory).setProfilesYamlString(profilesFile)
                .putAllVariablesFile(variablesFile).putAllVariablesFromEnvironment();
        PlaybookService playbookService = playbookServiceBuilder.build();
        assertEquals(playbookServiceExpected.getPlaybookModel(), playbookService.getPlaybookModel());
        assertEquals(playbookServiceExpected.getProvider(), playbookService.getProvider());
    }

}