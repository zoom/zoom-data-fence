package us.zoom.data.dfence.test.fixtures;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Builder for creating DatabaseRoleTestFixture with all required test objects.
 * Uses unique names prefixed with "DBROLE_TEST_" to avoid collisions with other tests.
 */
@Slf4j
public class DatabaseRoleTestFixtureBuilder {
    private final SnowflakeConnectionProvider sysadminProvider;
    private final SnowflakeConnectionProvider securityadminProvider;
    private final LifecycleManager lifecycleManager = new LifecycleManager();

    private String databaseName;
    private String databaseRoleName = "DBROLE_TEST_DATABASE_ROLE";
    private String roleName;
    private String roleName2;

    public DatabaseRoleTestFixtureBuilder(
            SnowflakeConnectionProvider sysadminProvider,
            SnowflakeConnectionProvider securityadminProvider) {
        this.sysadminProvider = sysadminProvider;
        this.securityadminProvider = securityadminProvider;
        lifecycleManager.getLifecycleObjects().clear();
    }

    /**
     * Creates a test database with a unique name prefixed with "DBROLE_TEST_".
     */
    public DatabaseRoleTestFixtureBuilder withDatabase() {
        this.databaseName = String.format("DBROLE_TEST_DATABASE_%s", UUID.randomUUID().toString().replace("-", ""))
                .toUpperCase();
        lifecycleManager.getLifecycleObjects()
                .add(SnowflakeLifecycleObject.database(sysadminProvider, databaseName));
        return this;
    }

    /**
     * Creates a database role in the database.
     */
    public DatabaseRoleTestFixtureBuilder withDatabaseRole() {
        if (databaseName == null) {
            throw new IllegalStateException("Database must be created before database role");
        }
        lifecycleManager.getLifecycleObjects().add(SnowflakeLifecycleObject.databaseRole(
                sysadminProvider,
                databaseName,
                databaseRoleName));
        return this;
    }

    /**
     * Creates test roles. Roles are created but not set up (they should already exist).
     */
    public DatabaseRoleTestFixtureBuilder withRoles() {
        this.roleName = String.format("DBROLE_TEST_ROLE_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
        this.roleName2 = String.format("DBROLE_TEST_ROLE2_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
        lifecycleManager.getLifecycleObjects().add(LifecycleWrapper.builder()
                .performSetup(false)
                .exists(true)
                .wraps(SnowflakeLifecycleObject.role(securityadminProvider, roleName))
                .build());
        lifecycleManager.getLifecycleObjects().add(LifecycleWrapper.builder()
                .performSetup(false)
                .exists(true)
                .wraps(SnowflakeLifecycleObject.role(securityadminProvider, roleName2))
                .build());
        return this;
    }

    /**
     * Builds the fixture and sets up all lifecycle objects.
     */
    public DatabaseRoleTestFixture build() {
        if (databaseName == null) {
            throw new IllegalStateException("Database must be created");
        }
        if (roleName == null || roleName2 == null) {
            throw new IllegalStateException("Roles must be created");
        }
        if (databaseRoleName == null) {
            throw new IllegalStateException("Database role must be created");
        }

        log.info("Setting up lifecycleManager for DatabaseRoleTestFixture");
        lifecycleManager.setup();

        return new DatabaseRoleTestFixture(
                databaseName,
                databaseRoleName,
                roleName,
                roleName2,
                lifecycleManager
        );
    }
}
