package us.zoom.data.dfence.providers.snowflake.informationschema;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.DatabaseError;
import us.zoom.data.dfence.providers.snowflake.SnowflakeConnectionService;
import us.zoom.data.dfence.providers.snowflake.SnowflakeRoleType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.sql.ObjectName;

@AllArgsConstructor
@Slf4j
@EqualsAndHashCode
public class SnowflakeFunctionObjectService {
  private final SnowflakeConnectionService snowflakeConnectionService;

  public List<String> getContainerProcedures(
      @NotEmpty String containerName, @NotNull SnowflakeObjectType containerObjectType) {
    List<String> containerParts = ObjectName.splitObjectName(containerName);
    if (containerParts.size() != containerObjectType.getQualLevel()) {
      throw new RuntimeException(
          String.format(
              "Container name %s contains %s parts. Expected %s for object type %s.",
              containerName,
              containerParts.size(),
              containerObjectType.getQualLevel(),
              containerObjectType));
    }
    String query;
    switch (containerObjectType) {
      case DATABASE -> {
        String databaseName = containerParts.get(0);
        if (databaseName == null || databaseName.isEmpty()) {
          throw new RuntimeException(
              String.format("Missing database name in container name %s", containerName));
        }
        query =
            String.format(
                "select function_catalog, function_schema, function_name, argument_signature, from %s.INFORMATION_SCHEMA.FUNCTIONS",
                databaseName);
      }
      case SCHEMA -> {
        String databaseName = containerParts.get(0);
        String schemaName = containerParts.get(1);
        if (databaseName == null || databaseName.isEmpty()) {
          throw new RuntimeException(
              String.format("Missing database name in container name %s", containerName));
        }
        if (schemaName == null || schemaName.isEmpty()) {
          throw new RuntimeException(
              String.format("Missing database name in container name %s", containerName));
        }
        query =
            String.format(
                "select function_catalog, function_schema, function_name, argument_signature from %s.INFORMATION_SCHEMA.FUNCTIONS where function_schema = '%s'",
                databaseName, schemaName);
      }
      default -> {
        throw new RuntimeException(
            String.format("Container object type %s not supported.", containerObjectType));
      }
    }
    log.debug("Finding objects with query: \"{}\"", query);
    try (Connection connection =
        snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)) {
      Statement statement = connection.createStatement();
      statement.execute(query);
      ResultSet resultSet = statement.getResultSet();
      List<String> objectNames = new ArrayList<>();
      while (resultSet.next()) {
        objectNames.add(
            ObjectName.normalizeObjectName(
                String.format(
                    "\"%s\".\"%s\".\"%s\"(%s)",
                    resultSet.getString("FUNCTION_CATALOG"),
                    resultSet.getString("FUNCTION_SCHEMA"),
                    resultSet.getString("FUNCTION_NAME"),
                    ObjectName.procedureArgumentsToTypes(
                        resultSet.getString("ARGUMENT_SIGNATURE")))));
      }
      return objectNames;
    } catch (SQLException e) {
      log.error("SQL Failure while executing query: {}", query);
      throw new DatabaseError(
          String.format(
              "Unable to find tables in %s %s due to database error.",
              containerObjectType, containerName),
          e);
    }
  }
}
