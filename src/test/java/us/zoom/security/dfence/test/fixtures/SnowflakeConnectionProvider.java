package us.zoom.security.dfence.test.fixtures;

import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@AllArgsConstructor
public class SnowflakeConnectionProvider {
    private Properties connectionProperties;
    private String snowflakeAccount;

    public Connection getConnection() {
        try {
            String connectionString = String.format("jdbc:snowflake://%s.snowflakecomputing.com/", snowflakeAccount);
            return DriverManager.getConnection(connectionString, connectionProperties);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to get Snowflake connection.", e);
        }
    }
}
