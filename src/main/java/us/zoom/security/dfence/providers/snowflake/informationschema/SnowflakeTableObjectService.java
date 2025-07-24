package us.zoom.security.dfence.providers.snowflake.informationschema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import us.zoom.security.dfence.exception.DatabaseError;
import us.zoom.security.dfence.exception.RbacDataError;
import us.zoom.security.dfence.providers.snowflake.SnowflakeConnectionService;
import us.zoom.security.dfence.providers.snowflake.SnowflakeRoleType;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.security.dfence.sql.ObjectName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@EqualsAndHashCode
@Slf4j
public class SnowflakeTableObjectService {

    private final SnowflakeConnectionService snowflakeConnectionService;


    public List<String> getContainerTables(
            @NotEmpty String containerName,
            @NotNull SnowflakeObjectType containerObjectType,
            @NotNull SnowflakeObjectType objectType) {
        List<String> tableTypes;
        switch (objectType) {
            case TABLE -> {
                tableTypes = List.of("BASE TABLE", "TEMPORARY TABLE");
            }
            case EXTERNAL_TABLE -> {
                tableTypes = List.of("EXTERNAL TABLE");
            }
            case EVENT_TABLE -> {
                tableTypes = List.of("EVENT TABLE");
            }
            case VIEW -> {
                tableTypes = List.of("VIEW");
            }
            case MATERIALIZED_VIEW -> {
                tableTypes = List.of("MATERIALIZED VIEW");
            }
            default -> {
                throw new RuntimeException(String.format("Object type %s cannot be queried in INFORMATION_SCHEMA.TABLES.",
                        objectType));
            }
        }
        String tableTypesSql = String.join(", ", tableTypes.stream().map(x -> String.format("'%s'", x)).toList());
        String query;
        switch (containerObjectType) {
            case DATABASE -> {
                query = String.format(
                        "select table_catalog, table_name, table_schema from %s.information_schema.tables where table_schema != 'INFORMATION_SCHEMA' and table_type in ( %s );",
                        ObjectName.normalizeObjectNamePart(containerName),
                        tableTypesSql);
            }
            case SCHEMA -> {
                List<String> containerParts = ObjectName.splitObjectName(containerName);
                if (containerParts.size() != 2 || containerParts.get(0).isEmpty() || containerParts.get(1).isEmpty()) {
                    throw new RuntimeException(String.format(
                            "2 container parts expected, received %s.",
                            containerParts.size()));
                }
                if ("INFORMATION_SCHEMA".equals(containerParts.get(1))) {
                    throw new RbacDataError("Cannot grant privileges directly on INFORMATION_SCHEMA.");
                }
                query = String.format(
                        "select table_catalog, table_name, table_schema from %s.information_schema.tables where table_schema = '%s' and table_type in ( %s );",
                        containerParts.get(0),
                        ObjectName.unquotedObjectNamePart(containerParts.get(1)),
                        tableTypesSql);

            }
            default -> {
                throw new RuntimeException(String.format(
                        "Container object type %s not supported.",
                        containerObjectType));
            }
        }
        log.debug("Finding objects with query: \"{}\"", query);
        try (Connection connection = snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)) {
            Statement statement = connection.createStatement();
            statement.execute(query);
            ResultSet resultSet = statement.getResultSet();
            List<String> objectNames = new ArrayList<>();
            while (resultSet.next()) {
                objectNames.add(ObjectName.normalizeObjectName(String.format(
                        "\"%s\".\"%s\".\"%s\"",
                        resultSet.getString("TABLE_CATALOG"),
                        resultSet.getString("TABLE_SCHEMA"),
                        resultSet.getString("TABLE_NAME")

                )));
            }
            return objectNames;
        } catch (SQLException e) {
            throw new DatabaseError(
                    String.format(
                            "Unable to find tables in %s %s due to database error.",
                            containerObjectType,
                            containerName), e);
        }
    }
}
