package us.zoom.data.dfence.test.fixtures;

/**
 * Type-safe fixture containing all test data for AgentGrantSystemTest.
 * Ensures test isolation with unique names.
 */
public record AgentTestFixture(
        String databaseName,
        String schemaName,
        String semanticViewName,
        String agentName,
        String roleName,
        String roleName2,
        LifecycleManager lifecycleManager
) {
    /**
     * Gets the fully qualified semantic view name.
     */
    public String qualifiedSemanticViewName() {
        return String.join(".", databaseName, schemaName, semanticViewName);
    }

    /**
     * Gets the fully qualified agent name.
     */
    public String qualifiedAgentName() {
        return String.join(".", databaseName, schemaName, agentName);
    }

    /**
     * Gets the fully qualified schema name.
     */
    public String qualifiedSchemaName() {
        return String.join(".", databaseName, schemaName);
    }
}

