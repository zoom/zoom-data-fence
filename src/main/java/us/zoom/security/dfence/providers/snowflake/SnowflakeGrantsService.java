package us.zoom.security.dfence.providers.snowflake;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import us.zoom.security.dfence.exception.DatabaseError;
import us.zoom.security.dfence.exception.ObjectNameException;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.security.dfence.sql.ObjectName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@EqualsAndHashCode
@ToString
public class SnowflakeGrantsService {
    private final SnowflakeConnectionService snowflakeConnectionService;

    private static final List<String> IGNORE_GRANTED_ON_TYPES = List.of("DIRECTORY_TABLE", "SEMANTIC_ENTITY", "SEMANTIC_CALCULATION", "SEMANTIC_RELATIONSHIP");

    public SnowflakeGrantsService(SnowflakeConnectionService snowflakeConnectionService) {
        this.snowflakeConnectionService = snowflakeConnectionService;
    }

    public static String preprocessObjectName(String objectName, String objectType, Boolean isFuture) {
        if (List.of("PROCEDURE", "FUNCTION").contains(objectType) && !isFuture) {
            return ObjectName.procedureGrantNameToObjectName(objectName);
        }
        return objectName;
    }

    public static List<SnowflakeGrantModel> resultSetToGrant(ResultSet resultSet, Boolean isFuture, Boolean ignoreUnknownGrantTypes)
            throws SQLException {
        List<SnowflakeGrantModel> snowflakeGrantModels = new ArrayList<>();
        String grantTerm = "granted";
        if (isFuture) {
            grantTerm = "grant";
        }
        while (resultSet.next()) {
                String grantedOn = resultSet.getString(grantTerm + "_on");
                String objectName = resultSet.getString("name");
                SnowflakeGrantModel grantModel = null;
                if (!IGNORE_GRANTED_ON_TYPES.contains(grantedOn)) {
                    try {
                        String objectNameProcessed = preprocessObjectName(objectName, grantedOn, isFuture);

                        grantModel = new SnowflakeGrantModel(
                                resultSet.getString("privilege"),
                                grantedOn,
                                objectNameProcessed,
                                resultSet.getString(grantTerm + "_to"),
                                resultSet.getString("grantee_name"),
                                resultSet.getBoolean("grant_option"),
                                isFuture,
                                false);
                    } catch (ObjectNameException e) {
                        if (ignoreUnknownGrantTypes) {
                            log.info(
                                    String.format(
                                            "Ignoring existing grant for object %s of type %s because of failure to parse object name.",
                                            objectName,
                                            grantedOn), e);
                        } else {
                            throw new ObjectNameException(
                                    String.format(
                                            "Unable to parse result set for object %s of type %s",
                                            objectName,
                                            grantedOn), e);
                        }
                    }
                    if (grantModel != null) {
                        snowflakeGrantModels.add(grantModel);
                    }
                }
        }
        return List.copyOf(snowflakeGrantModels);
    }

    public Map<String, SnowflakeGrantBuilder> getGrants(String roleName) {
        return getGrants(roleName, false);
    }

    public Map<String, SnowflakeGrantBuilder> getGrants(String roleName, Boolean skipUnknownGrantTypes) {
        log.info("Finding existing grants for role {}", roleName);
        List<SnowflakeGrantModel> snowflakeGrantModels = new ArrayList<>();
        List<Boolean> isFutures = List.of(false, true);
        List<String> queries = List.of(
                String.format("show grants to role %s;", roleName).toLowerCase(),
                String.format("show future grants to role %s;", roleName).toLowerCase());
        try (Connection connection = snowflakeConnectionService.connection()) {
            for (int i = 0; i < 2; i++) {
                String query = queries.get(i);
                Boolean isFuture = isFutures.get(i);
                log.debug("Getting grants for role {} with statement \"{}\"", roleName, query);
                Statement statement = connection.createStatement();
                statement.execute(query);
                ResultSet resultSet = statement.getResultSet();
                snowflakeGrantModels.addAll(resultSetToGrant(resultSet, isFuture, skipUnknownGrantTypes));
            }
            return snowflakeGrantModels.stream()
                    .map(x -> SnowflakeGrantBuilder.fromGrant(x, skipUnknownGrantTypes))
                    .filter(Objects::nonNull).collect(Collectors.toMap(
                            SnowflakeGrantBuilder::getKey,
                            Function.identity(),
                            (first, second) -> second));
        } catch (SQLException e) {
            throw new DatabaseError(String.format("Failed to retrieve current grant state for role %s.", roleName), e);
        }
    }
}
