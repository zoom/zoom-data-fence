package us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPatternOptions;

// Check this page for terminology https://docs.snowflake.com/en/sql-reference/sql/grant-privilege
public sealed interface ResolvedPlaybookPattern {

  PlaybookPatternOptions playbookPatternOptions();

  sealed interface Standard extends ResolvedPlaybookPattern {
    @Override
    default PlaybookPatternOptions playbookPatternOptions() {
      return PlaybookPatternOptions.STANDARD;
    }
    record Global() implements Standard {}
    record AccountObject(String objectName) implements Standard {}
    record AccountObjectDatabase(String databaseName) implements Standard {}
    record Schema(String databaseName, String schemaName) implements Standard {}
    record SchemaObject(String databaseName, String schemaName, String objectName)
        implements Standard {}
  }

  sealed interface Container extends ResolvedPlaybookPattern {
    ContainerPatternOptions playbookContainerPatternOptions();

    SnowflakeObjectType containerObjectType();

    @Override
    default PlaybookPatternOptions playbookPatternOptions() {
      return switch (playbookContainerPatternOptions()) {
        case FUTURE_AND_ALL -> PlaybookPatternOptions.FUTURE_AND_ALL;
        case FUTURE -> PlaybookPatternOptions.FUTURE;
        case ALL -> PlaybookPatternOptions.ALL;
      };
    }
    record AccountObjectDatabase(
        String databaseName, ContainerPatternOptions playbookContainerPatternOptions)
        implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.DATABASE;
      }
    }
    record Schema(
        String databaseName,
        String schemaName,
        ContainerPatternOptions playbookContainerPatternOptions)
        implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.SCHEMA;
      }
    }
    record SchemaObjectAllSchemas(
        String databaseName,
        String objectName,
        ContainerPatternOptions playbookContainerPatternOptions)
        implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.DATABASE;
      }
    }
  }
}
