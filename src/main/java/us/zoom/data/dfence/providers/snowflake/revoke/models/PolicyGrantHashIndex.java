package us.zoom.data.dfence.providers.snowflake.revoke.models;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;

public record PolicyGrantHashIndex(
    ConcurrentHashMap<String, ConcurrentHashMap<PolicyGrantPrivilege, Set<PolicyGrant>>> kv) {}
