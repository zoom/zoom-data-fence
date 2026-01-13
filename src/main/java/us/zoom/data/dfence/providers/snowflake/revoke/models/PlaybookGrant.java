package us.zoom.data.dfence.providers.snowflake.revoke.models;

import com.google.common.collect.ImmutableList;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;

public record PlaybookGrant(
    SnowflakeObjectType objectType,
    PlaybookPattern pattern,
    ImmutableList<GrantPrivilege> privileges,
    PlaybookGrantType grantType,
    boolean enable) {}
