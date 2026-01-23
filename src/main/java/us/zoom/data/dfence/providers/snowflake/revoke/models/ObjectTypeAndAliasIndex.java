package us.zoom.data.dfence.providers.snowflake.revoke.models;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectTypeAlias;

public record ObjectTypeAndAliasIndex(
    ConcurrentHashMap<ObjectType, Set<PlaybookGrant>> objectTypeIndex,
    ConcurrentHashMap<ObjectTypeAlias, Set<PlaybookGrant>> objectAliasIndex) {}
