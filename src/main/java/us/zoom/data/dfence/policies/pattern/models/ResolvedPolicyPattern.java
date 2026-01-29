package us.zoom.data.dfence.policies.pattern.models;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

// Check this page for terminology https://docs.snowflake.com/en/sql-reference/sql/grant-privilege
public sealed interface ResolvedPolicyPattern {

  sealed interface Standard extends ResolvedPolicyPattern {
    record Global() implements Standard {}
    record AccountObject(String objectName) implements Standard {}
    record AccountObjectDatabase(String databaseName) implements Standard {}
    record Schema(String databaseName, String schemaName) implements Standard {}
    record SchemaObject(String databaseName, String schemaName, String objectName)
        implements Standard {}
  }

  sealed interface Container extends ResolvedPolicyPattern {
    ContainerPatternOptions containerPatternOptions();

    SnowflakeObjectType containerObjectType();

    record AccountObjectDatabase(
        String databaseName, ContainerPatternOptions containerPatternOptions) implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.DATABASE;
      }
    }
    record Schema(
        String databaseName, String schemaName, ContainerPatternOptions containerPatternOptions)
        implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.SCHEMA;
      }
    }
    record SchemaObjectAllSchemas(
        String databaseName, String objectName, ContainerPatternOptions containerPatternOptions)
        implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.DATABASE;
      }
    }
  }
}
