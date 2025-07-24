package us.zoom.security.dfence.providers.snowflake.informationschema;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import us.zoom.security.dfence.exception.DatabaseError;
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

@EqualsAndHashCode
@AllArgsConstructor
@Slf4j
public class SnowflakeStageObjectsService {

    private final SnowflakeConnectionService snowflakeConnectionService;

    public List<String> getContainerExternalStages(
            @NotNull String containerName,
            @NotNull SnowflakeObjectType containerObjectType) {

        List<String> containerParts = ObjectName.splitObjectName(containerName);
        if (containerParts.size() != containerObjectType.getQualLevel()) {
            throw new RuntimeException(String.format(
                    "Container name %s contains %s parts. Expected %s for object type %s.",
                    containerName,
                    containerParts.size(),
                    containerObjectType.getQualLevel(),
                    containerObjectType)


            );
        }
        String query;
        switch (containerObjectType) {
            case DATABASE -> {
                String databaseName = containerParts.get(0);
                if (databaseName == null || databaseName.isEmpty()) {
                    throw new RuntimeException(String.format(
                            "Missing database name in container name %s",
                            containerName));
                }
                query = String.format(
                        "select stage_catalog, stage_schema, stage_name, stage_type from %s.INFORMATION_SCHEMA.STAGES where stage_type = 'External Named';",
                        databaseName);
            }
            case SCHEMA -> {
                String databaseName = containerParts.get(0);
                String schemaName = containerParts.get(1);
                if (databaseName == null || databaseName.isEmpty()) {
                    throw new RuntimeException(String.format(
                            "Missing database name in container name %s",
                            containerName));
                }
                if (schemaName == null || schemaName.isEmpty()) {
                    throw new RuntimeException(String.format(
                            "Missing database name in container name %s",
                            containerName));
                }
                query = String.format(
                        "select stage_catalog, stage_schema, stage_name, stage_type from %s.INFORMATION_SCHEMA.STAGES where stage_type = 'External Named' and stage_schema = '%s';",
                        databaseName,
                        schemaName);
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
                        resultSet.getString("STAGE_CATALOG"),
                        resultSet.getString("STAGE_SCHEMA"),
                        resultSet.getString("STAGE_NAME")

                )));
            }
            return objectNames;
        } catch (SQLException e) {
            throw new DatabaseError(
                    String.format(
                            "Unable to find stages in %s %s due to database error.",
                            containerObjectType,
                            containerName), e);
        }
    }
}
