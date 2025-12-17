package us.zoom.data.dfence.providers.snowflake.grant.builder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import us.zoom.data.dfence.test.fixtures.GrantTestDataLoader;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * YAML-driven parameterized tests for SnowflakeGrantBuilder.
 * 
 * Test data is defined in YAML files in src/test/resources/test-data/grant-builder/
 * The YAML structure is organized to match Snowflake documentation:
 * https://docs.snowflake.com/en/sql-reference/sql/grant-privilege
 * 
 * To add new tests:
 * 1. Open the appropriate YAML file (e.g., grant-revoke-statements.yml)
 * 2. Add a new test case following the existing pattern
 * 3. Reference the Snowflake documentation for the correct privilege and object type
 * 4. Run the tests - they will automatically pick up the new test case
 */
class SnowflakeGrantBuilderYamlTest {

    /**
     * Tests that grant and revoke statements are generated correctly.
     * Test cases are defined in grant-revoke-statements.yml
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("grantRevokeStatementsTestData")
    void grantRevokeStatements(GrantTestDataLoader.GrantRevokeStatementsTestData testData) {
        SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(testData.grantModel());
        List<String> grantStatements = builder.getGrantStatements();
        List<String> revokeStatements = builder.getRevokeStatements();
        
        assertEquals(testData.expectedGrantStatements(), grantStatements,
                "Grant statement mismatch for: " + testData.name());
        assertEquals(testData.expectedRevokeStatements(), revokeStatements,
                "Revoke statement mismatch for: " + testData.name());
    }

    static Stream<GrantTestDataLoader.GrantRevokeStatementsTestData> grantRevokeStatementsTestData() {
        return GrantTestDataLoader.loadGrantRevokeStatements();
    }
}

