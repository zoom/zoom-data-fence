package us.zoom.data.dfence.test.fixtures;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Builder for creating TestRunFixture with all required test objects.
 * Encapsulates the complex setup logic that was previously in @BeforeGroups.
 */
@Slf4j
public class TestRunFixtureBuilder {
    private final SnowflakeConnectionProvider sysadminProvider;
    private final SnowflakeConnectionProvider securityadminProvider;
    private final LifecycleManager lifecycleManager = new LifecycleManager();

    private String databaseName;
    private String schemaName = "GROUP_A_SCHEMA";
    private String tableName = "GROUP_A_TABLE";
    private String roleName;
    private String roleName2;
    private String procedureNamePartial = "GROUP_A_PROCEDURE";

    public TestRunFixtureBuilder(
            SnowflakeConnectionProvider sysadminProvider,
            SnowflakeConnectionProvider securityadminProvider) {
        this.sysadminProvider = sysadminProvider;
        this.securityadminProvider = securityadminProvider;
        lifecycleManager.getLifecycleObjects().clear();
    }

    /**
     * Creates a test database with a unique name.
     */
    public TestRunFixtureBuilder withDatabase() {
        this.databaseName = String.format("TEST_DATABASE_%s", UUID.randomUUID().toString().replace("-", ""))
                .toUpperCase();
        lifecycleManager.getLifecycleObjects()
                .add(SnowflakeLifecycleObject.database(sysadminProvider, databaseName));
        return this;
    }

    /**
     * Creates a test schema in the database.
     */
    public TestRunFixtureBuilder withSchema() {
        if (databaseName == null) {
            throw new IllegalStateException("Database must be created before schema");
        }
        lifecycleManager.getLifecycleObjects()
                .add(SnowflakeLifecycleObject.schema(sysadminProvider, databaseName, schemaName));
        return this;
    }

    /**
     * Creates a test table in the schema.
     */
    public TestRunFixtureBuilder withTable() {
        if (databaseName == null || schemaName == null) {
            throw new IllegalStateException("Database and schema must be created before table");
        }
        lifecycleManager.getLifecycleObjects().add(SnowflakeLifecycleObject.table(
                sysadminProvider,
                databaseName,
                schemaName,
                tableName));
        return this;
    }

    /**
     * Creates test roles. Roles are created but not set up (they should already exist).
     */
    public TestRunFixtureBuilder withRoles() {
        this.roleName = String.format("TEST_ROLE_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
        this.roleName2 = String.format("TEST_ROLE2_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
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
     * Creates test procedures in the schema.
     */
    public TestRunFixtureBuilder withProcedures() {
        if (databaseName == null || schemaName == null) {
            throw new IllegalStateException("Database and schema must be created before procedures");
        }
        
        // Procedure with arguments
        lifecycleManager.getLifecycleObjects().add(new SnowflakeLifecycleObject(
                sysadminProvider,
                String.format(
                        """
                        CREATE OR REPLACE PROCEDURE "%s"."%s"."%s"(from_table STRING, to_table STRING, count INT)
                          RETURNS STRING
                          LANGUAGE PYTHON
                          RUNTIME_VERSION = '3.9'
                          PACKAGES = ('snowflake-snowpark-python')
                          HANDLER = 'run'
                        AS
                        $$
                        def run(session, from_table, to_table, count):
                          session.table(from_table).limit(count).write.save_as_table(to_table)
                          return "SUCCESS"
                        $$;
                        """, databaseName, schemaName, procedureNamePartial),
                String.format(
                        "DROP PROCEDURE IF EXISTS \"%s\".\"%s\".\"%s\"(STRING, STRING, INT)",
                        databaseName,
                        schemaName,
                        procedureNamePartial)
        ));

        // Procedure without arguments
        lifecycleManager.getLifecycleObjects().add(new SnowflakeLifecycleObject(
                sysadminProvider,
                String.format(
                        """
                        CREATE OR REPLACE PROCEDURE "%s"."%s"."%s"()
                          RETURNS STRING
                          LANGUAGE PYTHON
                          RUNTIME_VERSION = '3.9'
                          PACKAGES = ('snowflake-snowpark-python')
                          HANDLER = 'run'
                        AS
                        $$
                        def run(session):
                          return "SUCCESS"
                        $$;
                        """, databaseName, schemaName, procedureNamePartial),
                String.format(
                        "DROP PROCEDURE IF EXISTS \"%s\".\"%s\".\"%s\"()",
                        databaseName,
                        schemaName,
                        procedureNamePartial)
        ));
        return this;
    }

    /**
     * Builds the fixture and sets up all lifecycle objects.
     */
    public TestRunFixture build() {
        if (databaseName == null) {
            throw new IllegalStateException("Database must be created");
        }
        if (roleName == null || roleName2 == null) {
            throw new IllegalStateException("Roles must be created");
        }

        String procedureName = procedureNamePartial + "(STRING, STRING, NUMBER)";
        String procedureNameShowGrantsQual = String.format(
                "%s.%s.\"%s(FROM_TABLE VARCHAR, TO_TABLE VARCHAR, COUNT NUMBER):VARCHAR\"",
                databaseName,
                schemaName,
                procedureNamePartial);
        String procedureNameNoArgsShowGrantsQual = String.format(
                "%s.%s.\"%s():VARCHAR\"",
                databaseName,
                schemaName,
                procedureNamePartial);

        log.info("Setting up lifecycleManager for TestRunFixture");
        lifecycleManager.setup();

        return new TestRunFixture(
                databaseName,
                schemaName,
                tableName,
                roleName,
                roleName2,
                procedureNamePartial,
                procedureName,
                procedureNameShowGrantsQual,
                procedureNameNoArgsShowGrantsQual,
                lifecycleManager
        );
    }
}

