package us.zoom.data.dfence.providers.snowflake.revoke;

import io.vavr.control.Try;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.revoke.evaluator.GrantRevocationEvaluator;
import us.zoom.data.dfence.providers.snowflake.revoke.index.PolicyGrantHashIndexer;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnowflakeRevokeGrantsCompiler {

  /**
   * Compiles grants to revoke by comparing current grants with playbook grants. Returns grants that
   * don't match any playbook grant.
   */
  public static List<SnowflakeGrantBuilder> compileRevokeGrants(
      List<PlaybookPrivilegeGrant> playbookGrants,
      Map<String, SnowflakeGrantBuilder> currentGrantBuilders) {
    return Try.of(
            () -> {
              GrantRevocationEvaluator evaluator =
                  new GrantRevocationEvaluator(PolicyGrantHashIndexer.create(playbookGrants));

              return currentGrantBuilders.values().stream()
                  .filter(grantBuilder -> evaluator.needsRevoke(grantBuilder.getGrant()))
                  .sorted(Comparator.comparing(SnowflakeGrantBuilder::getKey))
                  .collect(Collectors.toList());
            })
        .getOrElseThrow(
            e -> {
              log.error(
                  "Unexpected error compiling revoke grants: {} playbook grants provided",
                  playbookGrants.size(),
                  e);
              return new RbacDataError("Failed to compile revoke grants: " + e.getMessage(), e);
            });
  }
}
