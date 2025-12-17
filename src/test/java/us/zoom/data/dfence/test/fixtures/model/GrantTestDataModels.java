package us.zoom.data.dfence.test.fixtures.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Type-safe models for YAML test data.
 * These records eliminate the need for @SuppressWarnings("unchecked") annotations
 * by providing proper type information to Jackson.
 */
public class GrantTestDataModels {

    // Root structure for grant-revoke-statements.yml
    public record GrantRevokeStatementsTestFile(List<GrantRevokeStatementsTest> tests) {}
    
    // Root structure for fixture-grants.yml  
    public record FixtureGrantsTestFile(List<FixtureGrantsTest> tests) {}
    
    // Root structure for playbook-privilege-grants.yml
    public record PlaybookPrivilegeGrantTestFile(List<PlaybookPrivilegeGrantTest> tests) {}

    // Grant/Revoke Statements Test
    public record GrantRevokeStatementsTest(
            String name,
            GrantData grant,
            @JsonProperty("expectedGrantStatement") String expectedGrantStatement,
            @JsonProperty("expectedRevokeStatement") String expectedRevokeStatement
    ) {}

    // Fixture Grants Test
    public record FixtureGrantsTest(
            String name,
            GrantData grant,
            @JsonProperty("expectedBuilder") String expectedBuilder
    ) {}

    // Playbook Privilege Grant Test
    public record PlaybookPrivilegeGrantTest(
            String name,
            GrantData grant,
            @JsonProperty("expectedPlaybookGrant") ExpectedPlaybookGrantData expectedPlaybookGrant
    ) {}

    // Common grant data structure
    public record GrantData(
            String privilege,
            @JsonProperty("objectType") String objectType,
            @JsonProperty("objectName") String objectName,
            String role,
            @JsonProperty("grantOption") Boolean grantOption,
            Boolean future,
            Boolean all
    ) {}

    // Expected playbook grant data
    public record ExpectedPlaybookGrantData(
            @JsonProperty("objectType") String objectType,
            @JsonProperty("objectName") String objectName,
            @JsonProperty("schemaName") String schemaName,
            @JsonProperty("databaseName") String databaseName,
            List<String> privileges,
            @JsonProperty("includeFuture") Boolean includeFuture,
            @JsonProperty("includeAll") Boolean includeAll
    ) {}
}

