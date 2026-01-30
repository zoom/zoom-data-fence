package us.zoom.data.dfence.providers.snowflake.revoke.models;

public sealed interface SnowflakeGrantType {

  sealed interface Standard extends SnowflakeGrantType {
    record Global() implements Standard {}
    record AccountObject(String objectName) implements Standard {}
    record AccountObjectDatabase(String databaseName) implements Standard {}
    record Schema(String databaseName, String schemaName) implements Standard {}
    record SchemaObject(String databaseName, String schemaName, String objectName)
        implements Standard {}
  }

  sealed interface Container extends SnowflakeGrantType {
    // Grants returned by show grants only support FUTURE container grants
    record AccountObject(String objectName) implements Container {}
    record Schema(String databaseName, String schemaName) implements Container {}
  }
}
