package us.zoom.security.dfence.profile.model;

import lombok.Data;
import us.zoom.security.dfence.providers.snowflake.SnowflakeProviderConfigModel;

@Data
public class ProfileProviderModel {
    private SnowflakeProviderConfigModel snowflake;
}
