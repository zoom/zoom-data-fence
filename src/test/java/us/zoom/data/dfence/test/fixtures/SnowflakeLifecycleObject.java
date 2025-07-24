package us.zoom.data.dfence.test.fixtures;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

@AllArgsConstructor
@Slf4j
public class SnowflakeLifecycleObject implements LifecycleObject {
    SnowflakeConnectionProvider snowflakeConnectionProvider;
    String setupSql;
    String teardownSql;

    public static LifecycleObject table(
            SnowflakeConnectionProvider snowflakeConnectionProvider,
            String databaseName,
            String schemaName,
            String tableName) {
        String qualName = String.format("\"%s\".\"%s\".\"%s\"", databaseName, schemaName, tableName);
        return new SnowflakeLifecycleObject(
                snowflakeConnectionProvider,
                String.format("CREATE OR REPLACE TABLE %s (COL_1 VARCHAR)", qualName),
                String.format("DROP TABLE IF EXISTS %s", qualName));
    }

    public static LifecycleObject schema(
            SnowflakeConnectionProvider snowflakeConnectionProvider,
            String databaseName,
            String schemaName) {
        String qualName = String.format("\"%s\".\"%s\"", databaseName, schemaName);
        return new SnowflakeLifecycleObject(
                snowflakeConnectionProvider,
                String.format("CREATE OR REPLACE SCHEMA %S", qualName),
                String.format("DROP SCHEMA IF EXISTS %s", qualName));
    }

    public static LifecycleObject database(
            SnowflakeConnectionProvider snowflakeConnectionProvider,
            String databaseName) {
        String qualName = String.format("\"%s\"", databaseName);
        return new SnowflakeLifecycleObject(
                snowflakeConnectionProvider,
                String.format("CREATE OR REPLACE DATABASE %s", qualName),
                String.format("DROP DATABASE IF EXISTS %s", qualName));
    }

    public static LifecycleObject role(SnowflakeConnectionProvider snowflakeConnectionProvider, String roleName) {
        return new SnowflakeLifecycleObject(
                snowflakeConnectionProvider,
                String.format("CREATE OR REPLACE ROLE %s", roleName),
                String.format("DROP ROLE IF EXISTS %s", roleName));
    }

    public static LifecycleObject grantRole(SnowflakeConnectionProvider snowflakeConnectionProvider, String parentRole, String childRole) {
        return new SnowflakeLifecycleObject(
                snowflakeConnectionProvider,
                String.format("GRANT ROLE %s TO ROLE %S", childRole, parentRole),
                String.format("REVOKE ROLE %s FROM ROLE %s", childRole, parentRole)
        );
    }

    public static LifecycleObject user(SnowflakeConnectionProvider snowflakeConnectionProvider, String userName) {
        return new SnowflakeLifecycleObject(
                snowflakeConnectionProvider,
                String.format("CREATE USER %s", userName),
                String.format("DROP USER IF EXISTS %s", userName)
        );
    }

    public static LifecycleObject user(
            SnowflakeConnectionProvider snowflakeConnectionProvider, 
            String userName, 
            String networkPolicy, 
            String password, 
            String rsaPublicKey) {
        return new SnowflakeLifecycleObject(
                snowflakeConnectionProvider,
                String.format(
                        "CREATE USER %s NETWORK_POLICY = '%s' PASSWORD = '%s' RSA_PUBLIC_KEY = '%s'",
                        userName, networkPolicy, password, rsaPublicKey),
                String.format("DROP USER IF EXISTS %s", userName)
        );
    }

    @Override
    public void setup() {
        try (Connection snowflakeConnection = snowflakeConnectionProvider.getConnection()) {
            log.info("Executing Setup SQL {}", setupSql);
            snowflakeConnection.createStatement().execute(setupSql);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error performing setup SQL statement %s.", setupSql), e);
        }

    }

    @Override
    public void teardown() {
        try (Connection snowflakeConnection = snowflakeConnectionProvider.getConnection()) {
            log.info("Executing Teardown SQL {}", teardownSql);
            snowflakeConnection.createStatement().execute(teardownSql);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error performing teardown SQL statement %s.", teardownSql), e);
        }
    }
}
