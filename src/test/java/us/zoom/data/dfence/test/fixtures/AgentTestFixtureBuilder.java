package us.zoom.data.dfence.test.fixtures;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Builder for creating AgentTestFixture with all required test objects.
 * Uses unique names prefixed with "AGENT_TEST_" to avoid collisions with other tests.
 */
@Slf4j
public class AgentTestFixtureBuilder {
    private final SnowflakeConnectionProvider sysadminProvider;
    private final SnowflakeConnectionProvider securityadminProvider;
    private final LifecycleManager lifecycleManager = new LifecycleManager();

    private String databaseName;
    private String schemaName = "AGENT_TEST_SCHEMA";
    private String tableName = "AGENT_TEST_TABLE";
    private String semanticViewName = "AGENT_TEST_SEMANTIC_VIEW";
    private String agentName = "AGENT_TEST_AGENT";
    private String roleName;
    private String roleName2;

    public AgentTestFixtureBuilder(
            SnowflakeConnectionProvider sysadminProvider,
            SnowflakeConnectionProvider securityadminProvider) {
        this.sysadminProvider = sysadminProvider;
        this.securityadminProvider = securityadminProvider;
        lifecycleManager.getLifecycleObjects().clear();
    }

    /**
     * Creates a test database with a unique name prefixed with "AGENT_TEST_".
     */
    public AgentTestFixtureBuilder withDatabase() {
        this.databaseName = String.format("AGENT_TEST_DATABASE_%s", UUID.randomUUID().toString().replace("-", ""))
                .toUpperCase();
        lifecycleManager.getLifecycleObjects()
                .add(SnowflakeLifecycleObject.database(sysadminProvider, databaseName));
        return this;
    }

    /**
     * Creates a test schema in the database.
     */
    public AgentTestFixtureBuilder withSchema() {
        if (databaseName == null) {
            throw new IllegalStateException("Database must be created before schema");
        }
        lifecycleManager.getLifecycleObjects()
                .add(SnowflakeLifecycleObject.schema(sysadminProvider, databaseName, schemaName));
        return this;
    }

    /**
     * Creates a table in the schema (required for semantic view).
     */
    public AgentTestFixtureBuilder withTable() {
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
     * Creates a semantic view in the schema (required for agent creation).
     * Requires a table to be created first.
     */
    public AgentTestFixtureBuilder withSemanticView() {
        if (databaseName == null || schemaName == null || tableName == null) {
            throw new IllegalStateException("Database, schema, and table must be created before semantic view");
        }
        lifecycleManager.getLifecycleObjects().add(SnowflakeLifecycleObject.semanticView(
                sysadminProvider,
                databaseName,
                schemaName,
                semanticViewName,
                tableName));
        return this;
    }

    /**
     * Creates an agent in the schema with the semantic view as a tool.
     */
    public AgentTestFixtureBuilder withAgent() {
        if (databaseName == null || schemaName == null || semanticViewName == null) {
            throw new IllegalStateException("Database, schema, and semantic view must be created before agent");
        }
        lifecycleManager.getLifecycleObjects().add(SnowflakeLifecycleObject.agent(
                sysadminProvider,
                databaseName,
                schemaName,
                agentName,
                semanticViewName));
        return this;
    }

    /**
     * Creates test roles. Roles are created but not set up (they should already exist).
     */
    public AgentTestFixtureBuilder withRoles() {
        this.roleName = String.format("AGENT_TEST_ROLE_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
        this.roleName2 = String.format("AGENT_TEST_ROLE2_%s", UUID.randomUUID().toString().replace("-", "")).toUpperCase();
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
    public AgentTestFixture build() {
        if (databaseName == null) {
            throw new IllegalStateException("Database must be created");
        }
        if (roleName == null || roleName2 == null) {
            throw new IllegalStateException("Roles must be created");
        }
        if (tableName == null) {
            throw new IllegalStateException("Table must be created");
        }
        if (semanticViewName == null) {
            throw new IllegalStateException("Semantic view must be created");
        }
        if (agentName == null) {
            throw new IllegalStateException("Agent must be created");
        }

        log.info("Setting up lifecycleManager for AgentTestFixture");
        lifecycleManager.setup();

        return new AgentTestFixture(
                databaseName,
                schemaName,
                semanticViewName,
                agentName,
                roleName,
                roleName2,
                lifecycleManager
        );
    }
}

