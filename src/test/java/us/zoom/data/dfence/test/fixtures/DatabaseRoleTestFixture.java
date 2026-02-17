package us.zoom.data.dfence.test.fixtures;

/**
 * Type-safe fixture containing all test data for DatabaseRoleGrantSystemTest.
 * Uses unique naming to avoid collisions with other tests.
 */
public record DatabaseRoleTestFixture(
        String databaseName,
        String databaseRoleName,
        String roleName,
        String roleName2,
        LifecycleManager lifecycleManager
) {
    /**
     * Gets the fully qualified database role name (database.databaseRoleName).
     */
    public String qualifiedDatabaseRoleName() {
        return String.join(".", databaseName, databaseRoleName);
    }
}
