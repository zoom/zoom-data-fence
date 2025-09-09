package us.zoom.data.dfence.providers.snowflake.informationschema;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.DatabaseError;
import us.zoom.data.dfence.providers.snowflake.SnowflakeConnectionService;
import us.zoom.data.dfence.providers.snowflake.SnowflakeRoleType;
import us.zoom.data.dfence.sql.ObjectName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@EqualsAndHashCode
public class SnowflakeVolumeService {

    private final SnowflakeConnectionService snowflakeConnectionService;

    SnowflakeVolumeService(SnowflakeConnectionService snowflakeConnectionService) {
        this.snowflakeConnectionService = snowflakeConnectionService;
    }

    List<String> getExternalVolumesInAccount() {

        String query = "SHOW EXTERNAL VOLUMES IN ACCOUNT";

        log.debug("Finding objects with query: \"{}\"", query);

        try (Connection connection = snowflakeConnectionService.connection(SnowflakeRoleType.SYSADMIN)) {
            Statement statement = connection.createStatement();
            statement.execute(query);
            ResultSet resultSet = statement.getResultSet();
            List<String> objectNames = new ArrayList<>();
            while (resultSet.next()) {
                objectNames.add(ObjectName.normalizeObjectName(
                        resultSet.getString("name")
                ));
            }
            return objectNames;
        } catch (SQLException e) {
            throw new DatabaseError("Unable to find external volumes in account due to database error.", e);
        }
    }
}
