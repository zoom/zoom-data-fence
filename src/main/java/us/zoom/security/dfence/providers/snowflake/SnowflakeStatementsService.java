package us.zoom.security.dfence.providers.snowflake;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import us.zoom.security.dfence.exception.DatabaseError;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Slf4j
@EqualsAndHashCode
public class SnowflakeStatementsService {
    private final SnowflakeConnectionService snowflakeConnectionService;

    public SnowflakeStatementsService(
            SnowflakeConnectionService snowflakeConnectionService) {
        this.snowflakeConnectionService = snowflakeConnectionService;
    }

    @Override
    public String toString() {
        return "SnowflakeStatementsService{" + "snowflakeConnectionService=" + snowflakeConnectionService + '}';
    }

    public void applyStatements(List<String> statements) {
        try (Connection connection = snowflakeConnectionService.connection()) {
            statements.forEach(statementString -> {
                try {
                    log.debug("Executing Statement: {}", statementString);
                    Statement statement = connection.createStatement();
                    statement.execute(statementString);
                } catch (SQLException e) {
                    throw new DatabaseError(String.format("Unable to execute statement %s", statementString), e);
                }
            });
        } catch (SQLException e) {
            throw new DatabaseError("Unable to connect to Snowflake.", e);
        }

    }
}
