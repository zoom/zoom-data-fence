package us.zoom.data.dfence.providers.snowflake.grant.builder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.test.fixtures.GrantTestDataLoader;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

    /**
     * Tests that the correct grant builder class is selected for different grant types.
     * Test cases are defined in fixture-grants.yml
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureGrantsTestData")
    void fromGrant(GrantTestDataLoader.FixtureGrantTestData testData) throws RbacDataError {
        SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(testData.grantModel());
        assertInstanceOf(testData.expectedBuilderClass(), builder,
                "Expected builder class mismatch for: " + testData.name());
    }

    /**
     * Tests conversion from SnowflakeGrantModel to PlaybookPrivilegeGrant.
     * Test cases are defined in playbook-privilege-grants.yml
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("playbookPrivilegeGrantTestData")
    void playbookPrivilegeGrant(GrantTestDataLoader.PlaybookPrivilegeGrantTestData testData) {
        SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(testData.grantModel());
        PlaybookPrivilegeGrant actual = builder.playbookPrivilegeGrant();
        assertEquals(testData.expectedPlaybookGrant(), actual,
                "PlaybookPrivilegeGrant mismatch for: " + testData.name());
    }

    static Stream<GrantTestDataLoader.GrantRevokeStatementsTestData> grantRevokeStatementsTestData() {
        return GrantTestDataLoader.loadGrantRevokeStatements();
    }

    static Stream<GrantTestDataLoader.FixtureGrantTestData> fixtureGrantsTestData() {
        return GrantTestDataLoader.loadFixtureGrants();
    }

    static Stream<GrantTestDataLoader.PlaybookPrivilegeGrantTestData> playbookPrivilegeGrantTestData() {
        return GrantTestDataLoader.loadPlaybookPrivilegeGrants();
    }
}

