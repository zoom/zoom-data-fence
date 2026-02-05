package us.zoom.data.dfence.policies.models;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record PolicyGrant(
    SnowflakeObjectType objectType,
    List<PolicyGrantPrivilege> privileges,
    PolicyType policyType,
    boolean enable) {
  public PolicyGrant {
    privileges =
        ImmutableList.copyOf(
            privileges.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }
}
