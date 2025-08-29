package us.zoom.data.dfence.providers.snowflake.models;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record ContainerGrantParts(
    SnowflakeObjectType containerObjectType, String objectName, SnowflakeObjectType objectType) {}
