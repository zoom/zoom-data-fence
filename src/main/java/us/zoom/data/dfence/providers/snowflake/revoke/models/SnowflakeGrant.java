package us.zoom.data.dfence.providers.snowflake.revoke.models;

import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record SnowflakeGrant(
        SnowflakeObjectType snowflakeObjectType,
        PolicyGrantPrivilege privilege,
        SnowflakeGrantType type) {
}
