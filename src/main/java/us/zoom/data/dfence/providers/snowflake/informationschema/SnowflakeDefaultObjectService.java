package us.zoom.data.dfence.providers.snowflake.informationschema;

import static java.util.regex.Pattern.compile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.DatabaseError;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.SnowflakeConnectionService;
import us.zoom.data.dfence.providers.snowflake.SnowflakeRoleType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.sql.ObjectName;

@Slf4j
@AllArgsConstructor
@EqualsAndHashCode
public class SnowflakeDefaultObjectService {

  private final SnowflakeConnectionService snowflakeConnectionService;

  /*
  This is a general purpose function that should work with any object. However, it uses SHOW so it is not
  optimized and can come under limits for the number of results allowed.
   */
  public List<String> getContainerObjectQualNamesDefault(
      @NotNull SnowflakeObjectType containerObjectType,
      @NotNull SnowflakeObjectType objectType,
      @NotEmpty String containerName) {
    if (!List.of(
            SnowflakeObjectType.ACCOUNT, SnowflakeObjectType.DATABASE, SnowflakeObjectType.SCHEMA)
        .contains(containerObjectType)) {
      throw new RbacDataError(
          String.format("Invalid container object type %s", containerObjectType));
    }

    String query;
    if (containerObjectType == SnowflakeObjectType.ACCOUNT) {
      if (objectType.getQualLevel() != 1) {
        throw new RbacDataError(
            String.format(
                "Object type %s cannot be qualified under the account level. Qualification level of 1 is "
                    + "expected. Actual qualification level is %s.",
                objectType.getObjectType(), objectType.getQualLevel()));
      }
      query = String.format("show %s;", objectType.getObjectTypePlural().toLowerCase());
    } else {
      query =
          String.format(
              "show %s in %s %s;",
              objectType.getObjectTypePlural().toLowerCase(),
              containerObjectType.getObjectType().toLowerCase(),
              containerName);
    }
    log.debug("Finding objects with query: \"{}\"", query);
    Pattern informationSchemaPattern =
        compile("^[a-zA-Z0-9_\"]+.\"?INFORMATION_SCHEMA\"?(.[a-zA-Z0-9_\"]+)?");
    try (Connection connection =
        snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)) {
      Statement statement = connection.createStatement();
      statement.execute(query);
      ResultSet resultSet = statement.getResultSet();
      List<String> objectNames = new ArrayList<>();
      while (resultSet.next()) {
        String objectName = resultSet.getString("name");
        List<String> objectQualNameParts = new ArrayList<>();
        if (containerObjectType.getQualLevel() > 0) {
          // We ignore account level containers because it is not part of the qualified name. The
          // container name itself may also contain periods. For example, the container name may be
          // MY_DATABASE.MY_SCHEMA.
          objectQualNameParts.add(containerName);
        }
        if (containerObjectType == SnowflakeObjectType.DATABASE && objectType.getQualLevel() == 3) {
          // This is a special case where we are using a wildcard schema name, and we are
          // using a 3rd level qualified object. This is basically the
          // GRANT SELECT ON ALL TABLES IN DATABASE MY_DATABASE use case.
          String schemaName = resultSet.getString("schema_name");
          objectQualNameParts.add("\"" + schemaName + "\"");
        }
        objectQualNameParts.add("\"" + objectName + "\"");
        String objectQualName =
            ObjectName.normalizeObjectName(String.join(".", objectQualNameParts));
        objectNames.add(objectQualName);
      }
      return objectNames.stream().filter(x -> !informationSchemaPattern.matcher(x).find()).toList();
    } catch (SQLException e) {
      throw new DatabaseError(
          String.format(
              "Unable to find objects of type %s in container %s of type %s using query \"%s\"",
              objectType.getObjectType(),
              containerName,
              containerObjectType.getObjectType(),
              query),
          e);
    }
  }
}
