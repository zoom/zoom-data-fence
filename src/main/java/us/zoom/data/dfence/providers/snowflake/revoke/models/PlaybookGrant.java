package us.zoom.data.dfence.providers.snowflake.revoke.models;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models.ResolvedPlaybookPattern;

public record PlaybookGrant(
    SnowflakeObjectType objectType,
    PlaybookPattern pattern,
    List<GrantPrivilege> privileges,
    ResolvedPlaybookPattern resolvedPattern,
    boolean enable) {
  public PlaybookGrant {
    privileges =
        ImmutableList.copyOf(
            privileges.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }
}
