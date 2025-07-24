package us.zoom.data.dfence.providers.snowflake;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.DatabaseConnectionError;
import us.zoom.data.dfence.exception.RbacValueError;

import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@EqualsAndHashCode
public class SnowflakeConnectionService {

    private static final TypeReference<HashMap<String, Object>> genericTypeReference = new TypeReference<>() {
    };
    public static ObjectMapper connectionPropertyObjectMapper = new ObjectMapper();

    @Getter
    public final SnowflakeProviderConfigModel config;

    private final EnumMap<SnowflakeRoleType, HikariDataSource> dataSources  = new EnumMap<>(SnowflakeRoleType.class);

    public SnowflakeConnectionService(SnowflakeProviderConfigModel config) {
        this.config = config;
    }

    public SnowflakeConnectionService(SnowflakeProviderConfigModel config, EnumMap<SnowflakeRoleType, HikariDataSource> dataSources) {
        for (SnowflakeRoleType roleType : SnowflakeRoleType.values()) {
            if (!dataSources.containsKey(roleType)) {
                throw new RbacValueError("dataSources must contain keys for all values for SnowflakeRoleType.");
            }
        }
        this.config = config;
        this.dataSources.putAll(dataSources);
    }

    private synchronized void initializeDataSources() {
        if (!dataSources.isEmpty()) {
            return;
        }
        log.debug("Initializing Hikari data sources for Snowflake connections.");
        for (SnowflakeRoleType roleType : SnowflakeRoleType.values()) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getConnectionString());

            Properties props = connectionProperties(config.getConnectionProperties(), roleType);
            props.forEach((key, value) -> hikariConfig.addDataSourceProperty(key.toString(), value));

            // Configure connection pool
            hikariConfig.setMaximumPoolSize(config.getConnectionProperties().getMaxConnections());
            hikariConfig.setConnectionTimeout(config.getConnectionProperties().getLogInTimeout() * 1000L);
            hikariConfig.setPoolName(this.hashCode() + "HikariThreadPoolZoomDbRbac");
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            dataSources.put(roleType, dataSource);
        }
        log.debug("Successfully initialized Hikari data sources for Snowflake connections.");
    }

    public static Properties connectionProperties(
            SnowflakeConnectionProperties snowflakeConfigProperties,
            SnowflakeRoleType snowflakeRoleType) {
        Map<String, Object> propMap = connectionPropertyObjectMapper.convertValue(
                snowflakeConfigProperties,
                genericTypeReference);
        Properties properties = new Properties();
        properties.putAll(propMap);
        // Private key is a complex object that jackson cannot handle. We map it directly without Jackson.
        PrivateKey privateKey = snowflakeConfigProperties.getPrivateKey();
        if (snowflakeConfigProperties.getPrivateKey() != null) {
            properties.put("privateKey", privateKey);
        }
        // We use different roles in different contexts.
        String role;
        switch (snowflakeRoleType) {
            case SYSADMIN -> role = snowflakeConfigProperties.getSysAdminRole();
            case SECURITYADMIN -> role = snowflakeConfigProperties.getSecurityAdminRole();
            default -> role = snowflakeConfigProperties.getSecurityAdminRole();
        }
        properties.put("role", role);
        return properties;
    }

    public Connection connection(SnowflakeRoleType snowflakeRoleType) {
        initializeDataSources();
        log.debug("Obtaining snowflake connection from data source.");
        try {
            if (!dataSources.containsKey(snowflakeRoleType)) {
                throw new DatabaseConnectionError("No data source initialized for role type: " + snowflakeRoleType);
            }
            Connection connection = dataSources.get(snowflakeRoleType).getConnection();
            log.debug("Successfully obtained snowflake connection from data source.");
            return connection;
        } catch (SQLException e) {
            throw new DatabaseConnectionError("Unable to connect to snowflake.", e);
        }
    }

    public Connection connection() {
        return this.connection(SnowflakeRoleType.SECURITYADMIN);
    }

    /**
     * Closes all data sources and releases resources.
     * This method should be called when the service is no longer needed.
     */
    public void close() {
        log.debug("Closing Snowflake connection data sources.");
        dataSources.values().forEach(HikariDataSource::close);
        log.debug("Successfully closed Snowflake connection data sources.");
    }

    @Override
    public String toString() {
        return "SnowflakeConnectionService{" + "config=" + config + '}';
    }
}
