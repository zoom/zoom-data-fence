package us.zoom.data.dfence.providers.snowflake.revoke.models;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.collection.NonEmptyList;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;

public record PlaybookGrant(
    SnowflakeObjectType objectType,
    PlaybookPattern pattern,
    NonEmptyList<GrantPrivilege> privileges,
    NonEmptyList<SnowflakeGrantType> grantTypes,
    boolean enable) {}
