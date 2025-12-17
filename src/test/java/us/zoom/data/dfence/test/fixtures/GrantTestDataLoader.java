package us.zoom.data.dfence.test.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import us.zoom.data.dfence.Mappers;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.*;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.test.fixtures.model.GrantTestDataModels;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads grant test data from YAML files.
 * YAML files are organized to match Snowflake documentation structure.
 * Reference: https://docs.snowflake.com/en/sql-reference/sql/grant-privilege
 */
public class GrantTestDataLoader {

    private static final ObjectMapper yamlMapper = Mappers.yamlObjectMapper();

    /**
     * Loads grant/revoke statement test cases from YAML.
     */
    public static Stream<GrantRevokeStatementsTestData> loadGrantRevokeStatements() {
        try {
            InputStream resource = GrantTestDataLoader.class.getClassLoader()
                    .getResourceAsStream("test-data/grant-builder/grant-revoke-statements.yml");
            if (resource == null) {
                throw new IllegalStateException("Could not find grant-revoke-statements.yml");
            }
            
            GrantTestDataModels.GrantRevokeStatementsTestFile yamlData = 
                    yamlMapper.readValue(resource, GrantTestDataModels.GrantRevokeStatementsTestFile.class);
            
            if (yamlData.tests() == null) {
                throw new IllegalStateException("YAML file does not contain 'tests' key");
            }
            
            return yamlData.tests().stream().map(GrantTestDataLoader::parseGrantRevokeTest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load grant-revoke-statements.yml", e);
        }
    }

    private static GrantRevokeStatementsTestData parseGrantRevokeTest(GrantTestDataModels.GrantRevokeStatementsTest test) {
        SnowflakeGrantModel grantModel = createGrantModel(test.grant(), "MOCK_ROLE_1");
        
        if (test.expectedGrantStatement() == null || test.expectedRevokeStatement() == null) {
            throw new IllegalStateException("Test case missing expected statements: " + test.name());
        }
        
        return new GrantRevokeStatementsTestData(
                test.name(),
                grantModel,
                List.of(test.expectedGrantStatement()),
                List.of(test.expectedRevokeStatement())
        );
    }

    /**
     * Loads fixture grant test cases from YAML.
     * Tests that the correct builder class is selected.
     */
    public static Stream<FixtureGrantTestData> loadFixtureGrants() {
        try {
            InputStream resource = GrantTestDataLoader.class.getClassLoader()
                    .getResourceAsStream("test-data/grant-builder/fixture-grants.yml");
            if (resource == null) {
                throw new IllegalStateException("Could not find fixture-grants.yml");
            }
            
            GrantTestDataModels.FixtureGrantsTestFile yamlData = 
                    yamlMapper.readValue(resource, GrantTestDataModels.FixtureGrantsTestFile.class);
            
            if (yamlData.tests() == null) {
                throw new IllegalStateException("YAML file does not contain 'tests' key");
            }
            
            return yamlData.tests().stream().map(GrantTestDataLoader::parseFixtureGrantTest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load fixture-grants.yml", e);
        }
    }

    private static FixtureGrantTestData parseFixtureGrantTest(GrantTestDataModels.FixtureGrantsTest test) {
        SnowflakeGrantModel grantModel = createGrantModel(test.grant(), "MY_ROLE");
        
        if (test.expectedBuilder() == null) {
            throw new IllegalStateException("Test case missing 'expectedBuilder': " + test.name());
        }
        
        Class<? extends SnowflakeGrantBuilder> builderClass = resolveBuilderClass(test.expectedBuilder());
        
        return new FixtureGrantTestData(
                test.name(),
                grantModel,
                builderClass
        );
    }

    private static SnowflakeGrantModel createGrantModel(GrantTestDataModels.GrantData grantData, String defaultRole) {
        String role = grantData.role() != null ? grantData.role() : defaultRole;
        return new SnowflakeGrantModel(
                grantData.privilege(),
                grantData.objectType(),
                grantData.objectName() != null ? grantData.objectName() : "",
                "ROLE",
                role,
                grantData.grantOption() != null ? grantData.grantOption() : false,
                grantData.future() != null ? grantData.future() : false,
                grantData.all() != null ? grantData.all() : false
        );
    }

    private static Class<? extends SnowflakeGrantBuilder> resolveBuilderClass(String className) {
        String fullClassName = "us.zoom.data.dfence.providers.snowflake.grant.builder." + className;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends SnowflakeGrantBuilder> clazz = 
                    (Class<? extends SnowflakeGrantBuilder>) Class.forName(fullClassName);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not find builder class: " + fullClassName, e);
        }
    }

    /**
     * Loads playbook privilege grant test cases from YAML.
     * Tests conversion from SnowflakeGrantModel to PlaybookPrivilegeGrant.
     */
    public static Stream<PlaybookPrivilegeGrantTestData> loadPlaybookPrivilegeGrants() {
        try {
            InputStream resource = GrantTestDataLoader.class.getClassLoader()
                    .getResourceAsStream("test-data/grant-builder/playbook-privilege-grants.yml");
            if (resource == null) {
                throw new IllegalStateException("Could not find playbook-privilege-grants.yml");
            }
            
            GrantTestDataModels.PlaybookPrivilegeGrantTestFile yamlData = 
                    yamlMapper.readValue(resource, GrantTestDataModels.PlaybookPrivilegeGrantTestFile.class);
            
            if (yamlData.tests() == null) {
                throw new IllegalStateException("YAML file does not contain 'tests' key");
            }
            
            return yamlData.tests().stream().map(GrantTestDataLoader::parsePlaybookPrivilegeGrantTest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load playbook-privilege-grants.yml", e);
        }
    }

    private static PlaybookPrivilegeGrantTestData parsePlaybookPrivilegeGrantTest(GrantTestDataModels.PlaybookPrivilegeGrantTest test) {
        SnowflakeGrantModel grantModel = createGrantModel(test.grant(), "MOCK_ROLE_A");
        
        if (test.expectedPlaybookGrant() == null) {
            throw new IllegalStateException("Test case missing 'expectedPlaybookGrant': " + test.name());
        }
        
        GrantTestDataModels.ExpectedPlaybookGrantData expected = test.expectedPlaybookGrant();
        
        PlaybookPrivilegeGrant expectedPlaybookGrant = new PlaybookPrivilegeGrant(
                expected.objectType(),
                convertNullString(expected.objectName()),
                convertNullString(expected.schemaName()),
                convertNullString(expected.databaseName()),
                expected.privileges(),
                expected.includeFuture() != null ? expected.includeFuture() : false,
                expected.includeAll() != null ? expected.includeAll() : false
        );
        
        return new PlaybookPrivilegeGrantTestData(
                test.name(),
                grantModel,
                expectedPlaybookGrant
        );
    }

    public record GrantRevokeStatementsTestData(
            String name,
            SnowflakeGrantModel grantModel,
            List<String> expectedGrantStatements,
            List<String> expectedRevokeStatements
    ) {
    }

    public record FixtureGrantTestData(
            String name,
            SnowflakeGrantModel grantModel,
            Class<? extends SnowflakeGrantBuilder> expectedBuilderClass
    ) {
    }

    private static String convertNullString(String value) {
        // YAML "null" string should be converted to Java null
        if (value == null || "null".equals(value)) {
            return null;
        }
        return value;
    }

    public record PlaybookPrivilegeGrantTestData(
            String name,
            SnowflakeGrantModel grantModel,
            PlaybookPrivilegeGrant expectedPlaybookGrant
    ) {
    }
}

