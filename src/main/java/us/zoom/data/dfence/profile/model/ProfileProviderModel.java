package us.zoom.data.dfence.profile.model;

import lombok.Data;
import us.zoom.data.dfence.providers.snowflake.SnowflakeProviderConfigModel;

@Data
public class ProfileProviderModel {
    private SnowflakeProviderConfigModel snowflake;
}
