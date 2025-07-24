package us.zoom.security.dfence.providers.snowflake.models;

import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record ContainerGrantParts(
        SnowflakeObjectType containerObjectType, String objectName, SnowflakeObjectType objectType) {
}
