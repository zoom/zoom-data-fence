package us.zoom.data.dfence.providers.snowflake.revoke.models;

import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeObjectTypeAlias;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record SnowflakeObjectTypeAndAliasIndex(
    ConcurrentHashMap<SnowflakeObjectType, Set<PolicyGrant>> snowflakeObjectTypeIndex,
    ConcurrentHashMap<SnowflakeObjectTypeAlias, Set<PolicyGrant>> snowflakeObjectAliasIndex) {}
