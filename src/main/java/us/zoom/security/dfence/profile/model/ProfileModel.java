package us.zoom.security.dfence.profile.model;

import lombok.Data;

@Data
public class ProfileModel {

    private ProviderName providerName = ProviderName.SNOWFLAKE;

    private ProfileProviderModel connection;

    public enum ProviderName {
        SNOWFLAKE
    }
}
