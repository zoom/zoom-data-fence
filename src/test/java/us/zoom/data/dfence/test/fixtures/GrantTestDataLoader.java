package us.zoom.data.dfence.test.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import us.zoom.data.dfence.Mappers;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
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
            
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlData = yamlMapper.readValue(resource, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tests = (List<Map<String, Object>>) yamlData.get("tests");
            
            if (tests == null) {
                throw new IllegalStateException("YAML file does not contain 'tests' key");
            }
            
            return tests.stream().map(GrantTestDataLoader::parseGrantRevokeTest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load grant-revoke-statements.yml", e);
        }
    }

    private static GrantRevokeStatementsTestData parseGrantRevokeTest(Map<String, Object> testData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> grantData = (Map<String, Object>) testData.get("grant");
        
        if (grantData == null) {
            throw new IllegalStateException("Test case missing 'grant' key: " + testData);
        }
        
        String privilege = (String) grantData.get("privilege");
        String objectType = (String) grantData.get("objectType");
        String objectName = (String) grantData.get("objectName");
        String role = (String) grantData.getOrDefault("role", "MOCK_ROLE_1");
        Boolean grantOption = (Boolean) grantData.getOrDefault("grantOption", false);
        Boolean future = (Boolean) grantData.getOrDefault("future", false);
        Boolean all = (Boolean) grantData.getOrDefault("all", false);
        
        SnowflakeGrantModel grantModel = new SnowflakeGrantModel(
                privilege,
                objectType,
                objectName != null ? objectName : "",
                "ROLE",
                role,
                grantOption != null ? grantOption : false,
                future != null ? future : false,
                all != null ? all : false
        );
        
        String expectedGrant = (String) testData.get("expectedGrantStatement");
        String expectedRevoke = (String) testData.get("expectedRevokeStatement");
        
        if (expectedGrant == null || expectedRevoke == null) {
            throw new IllegalStateException("Test case missing expected statements: " + testData.get("name"));
        }
        
        return new GrantRevokeStatementsTestData(
                (String) testData.get("name"),
                grantModel,
                List.of(expectedGrant),
                List.of(expectedRevoke)
        );
    }

    public record GrantRevokeStatementsTestData(
            String name,
            SnowflakeGrantModel grantModel,
            List<String> expectedGrantStatements,
            List<String> expectedRevokeStatements
    ) {
    }
}

