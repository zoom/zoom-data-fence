package us.zoom.data.dfence.providers.snowflake.revoke.models;

import io.vavr.collection.List;

public sealed interface SnowflakeGrantType {

  List<String> parts();

  sealed interface Standard extends SnowflakeGrantType {
    record Global() implements Standard {
      @Override
      public List<String> parts() {
        return List.empty();
      }
    }
    record AccountObject(String objectName) implements Standard {
      @Override
      public List<String> parts() {
        return List.of(objectName);
      }
    }
    record Schema(String databaseName, String schemaName) implements Standard {
      @Override
      public List<String> parts() {
        return List.of(databaseName, schemaName);
      }
    }
    record SchemaObject(String databaseName, String schemaName, String objectName)
        implements Standard {
      @Override
      public List<String> parts() {
        return List.of(databaseName, schemaName, objectName);
      }
    }
  }

  sealed interface Container extends SnowflakeGrantType {
    // Grants returned by show grants only support FUTURE container grants
    record AccountObject(String objectName) implements Container {
      @Override
      public List<String> parts() {
        return List.of(objectName);
      }
    }
    record Schema(String databaseName, String schemaName) implements Container {
      @Override
      public List<String> parts() {
        return List.of(databaseName, schemaName);
      }
    }
  }
}
