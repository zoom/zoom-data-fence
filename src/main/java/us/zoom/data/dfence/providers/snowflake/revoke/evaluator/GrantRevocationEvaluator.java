package us.zoom.data.dfence.providers.snowflake.revoke.evaluator;

import io.vavr.control.Try;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.factories.SnowflakeGrantFactory;
import us.zoom.data.dfence.providers.snowflake.revoke.matchers.SnowflakeGrantMatchers;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PolicyGrantHashIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class GrantRevocationEvaluator {
  private final PolicyGrantHashIndex index;

  /**
   * Determines if a grant should be revoked. Returns true if the grant doesn't match any playbook
   * grant.
   */
  public boolean needsRevoke(SnowflakeGrantModel grant) {
    return Try.of(
            () -> {
              SnowflakeGrant snowflakeGrant = SnowflakeGrantFactory.createFrom(grant);
              return getCandidates(snowflakeGrant.snowflakeObjectType(), snowflakeGrant.privilege())
                  .stream()
                  .noneMatch(
                      policyGrant ->
                          SnowflakeGrantMatchers.matchesSnowflakeGrant()
                              .test(policyGrant, snowflakeGrant));
            })
        .getOrElseThrow(
            e -> {
              log.error("Failed to evaluate grant revocation for grant: {}", grant, e);
              return new RbacDataError("Failed to evaluate grant revocation for grant " + grant, e);
            });
  }

  private Set<PolicyGrant> getCandidates(
      SnowflakeObjectType grantObjectType, PolicyGrantPrivilege grantPrivilege) {
    return index
        .kv()
        .getOrDefault(grantObjectType.getAliasFor(), new ConcurrentHashMap<>())
        .getOrDefault(grantPrivilege, Set.of());
  }
}
