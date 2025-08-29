package us.zoom.data.dfence.test.fixtures;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SnowflakeConnectionProvider {
  private Properties connectionProperties;
  private String snowflakeAccount;

  public Connection getConnection() {
    try {
      String connectionString =
          String.format("jdbc:snowflake://%s.snowflakecomputing.com/", snowflakeAccount);
      return DriverManager.getConnection(connectionString, connectionProperties);
    } catch (SQLException e) {
      throw new RuntimeException("Unable to get Snowflake connection.", e);
    }
  }
}
