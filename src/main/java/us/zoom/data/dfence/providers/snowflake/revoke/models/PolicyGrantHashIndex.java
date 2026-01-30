package us.zoom.data.dfence.providers.snowflake.revoke.models;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeObjectTypeAlias;

public record PolicyGrantHashIndex(
    ConcurrentHashMap<PolicyGrantPrivilege, Set<PolicyGrant>> privilegeIndex,
    ConcurrentHashMap<SnowflakeObjectType, Set<PolicyGrant>> snowflakeObjectTypeIndex,
    ConcurrentHashMap<SnowflakeObjectTypeAlias, Set<PolicyGrant>> snowflakeObjectTypeAliasIndex) {}
