package us.zoom.data.dfence.providers.snowflake.revoke.models;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeGrantObjectName;

public record SnowflakeGrant(
    SnowflakeObjectType snowflakeObjectType,
    GrantPrivilege privilege,
    SnowflakeGrantType grantType,
    SnowflakeGrantObjectName name) {}
