package us.zoom.security.dfence.providers.snowflake;

import lombok.Data;

@Data
public class SnowflakeProviderConfigModel {
    private String connectionString;
    private SnowflakeConnectionProperties connectionProperties;
}
