package us.zoom.data.dfence.test.fixtures;

/**
 * Type-safe fixture containing all test data for TestRunSystemTest.
 * Replaces the use of ITestContext string-based attributes.
 */
public record TestRunFixture(
        String databaseName,
        String schemaName,
        String tableName,
        String roleName,
        String roleName2,
        String procedureNamePartial,
        String procedureName,
        String procedureNameShowGrantsQual,
        String procedureNameNoArgsShowGrantsQual,
        LifecycleManager lifecycleManager
) {
    /**
     * Gets the fully qualified table name.
     */
    public String qualifiedTableName() {
        return String.join(".", databaseName, schemaName, tableName);
    }

    /**
     * Gets the fully qualified schema name.
     */
    public String qualifiedSchemaName() {
        return String.join(".", databaseName, schemaName);
    }
}

