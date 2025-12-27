package us.zoom.data.dfence.providers.snowflake.revoke.models;

import java.util.List;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;

public record PlaybookGrant(
    SnowflakeObjectType objectType,
    PlaybookPattern pattern,
    List<GrantPrivilege> privileges,
    PlaybookGrantType grantType,
    boolean enable) {}
