package us.zoom.security.dfence.providers.snowflake.grant.builder;

import java.util.List;

public record GrantBuilderDiff(
        List<SnowflakeGrantBuilder> grant, List<SnowflakeGrantBuilder> revoke) {
}
