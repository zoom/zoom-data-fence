package us.zoom.data.dfence.policies.pattern.models;

import io.vavr.collection.List;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.sql.ObjectName;

// Check this page for terminology https://docs.snowflake.com/en/sql-reference/sql/grant-privilege
public sealed interface PolicyType {
  List<String> parts();

  String normalizeObjectName();

  sealed interface Standard extends PolicyType {
    record Global() implements Standard {
      @Override
      public List<String> parts() {
        return List.empty();
      }

      @Override
      public String normalizeObjectName() {
        return "";
      }
    }
    record AccountObject(String objectName) implements Standard {
      @Override
      public List<String> parts() {
        return List.of(objectName);
      }

      @Override
      public String normalizeObjectName() {
        return ObjectName.normalizeObjectName(objectName);
      }
    }
    record Schema(String databaseName, String schemaName) implements Standard {
      @Override
      public List<String> parts() {
        return List.of(databaseName, schemaName);
      }

      @Override
      public String normalizeObjectName() {
        return ObjectName.normalizeObjectName(String.join(".", List.of(databaseName, schemaName)));
      }
    }
    record SchemaObject(String databaseName, String schemaName, String objectName)
        implements Standard {
      @Override
      public List<String> parts() {
        return List.of(databaseName, schemaName, objectName);
      }

      @Override
      public String normalizeObjectName() {
        return ObjectName.normalizeObjectName(
            String.join(".", List.of(databaseName, schemaName, objectName)));
      }
    }
  }

  sealed interface Container extends PolicyType {
    ContainerPolicyOptions containerPolicyOptions();

    SnowflakeObjectType containerObjectType();

    record AccountObject(
        String objectName,
        ContainerPolicyOptions containerPolicyOptions,
        SnowflakeObjectType containerObjectType)
        implements Container {
      @Override
      public List<String> parts() {
        return List.of(objectName);
      }

      @Override
      public String normalizeObjectName() {
        return ObjectName.normalizeObjectName(objectName);
      }
    }
    record Schema(
        String databaseName, String schemaName, ContainerPolicyOptions containerPolicyOptions)
        implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.SCHEMA;
      }

      @Override
      public List<String> parts() {
        return List.of(databaseName, schemaName);
      }

      @Override
      public String normalizeObjectName() {
        return ObjectName.normalizeObjectName(String.join(".", List.of(databaseName, schemaName)));
      }
    }
    record SchemaObjectAllSchemas(
        String databaseName, ContainerPolicyOptions containerPolicyOptions) implements Container {
      @Override
      public SnowflakeObjectType containerObjectType() {
        return SnowflakeObjectType.DATABASE;
      }

      @Override
      public List<String> parts() {
        return List.of(databaseName);
      }

      @Override
      public String normalizeObjectName() {
        return ObjectName.normalizeObjectName(databaseName);
      }
    }
  }
}
