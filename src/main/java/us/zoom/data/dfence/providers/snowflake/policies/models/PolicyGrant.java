package us.zoom.data.dfence.providers.snowflake.policies.models;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ResolvedPlaybookPattern;

public record PolicyGrant(
    SnowflakeObjectType objectType,
    PolicyPattern pattern,
    List<PolicyGrantPrivilege> privileges,
    ResolvedPlaybookPattern resolvedPattern,
    boolean enable) {
  public PolicyGrant {
    privileges =
        ImmutableList.copyOf(
            privileges.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }
}
