package us.zoom.data.dfence.providers.snowflake.revoke.evaluator;

import io.vavr.control.Try;
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
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeObjectTypeAlias;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
                          SnowflakeGrantMatchers.matchesSnowflakeGrant().test(policyGrant, snowflakeGrant)
                  );
            })
        .getOrElseThrow(
            e -> {
              log.error("Failed to evaluate grant revocation for grant: {}", grant, e);
              return new RbacDataError("Failed to evaluate grant revocation for grant " + grant, e);
            });
  }

  private Set<PolicyGrant> getCandidates(
      SnowflakeObjectType grantObjectType, PolicyGrantPrivilege grantPrivilege) {
    Set<PolicyGrant> objectTypeGrants =
        getObjectTypeAndObjectTypeAliasMatchedGrants(grantObjectType);
    Set<PolicyGrant> privilegeGrants = getPrivilegeMatchedGrants(grantPrivilege);

    return objectTypeGrants.stream().filter(privilegeGrants::contains).collect(Collectors.toSet());
  }

  private Set<PolicyGrant> getObjectTypeAndObjectTypeAliasMatchedGrants(
      SnowflakeObjectType grantObjectType) {
    Set<PolicyGrant> baseGrants =
        index.snowflakeObjectTypeIndex().getOrDefault(grantObjectType, Set.of());

    Set<PolicyGrant> specificGrants =
        index
            .snowflakeObjectTypeAliasIndex()
            .getOrDefault(SnowflakeObjectTypeAlias.of(grantObjectType), Set.of())
            .stream()
            .filter(grant -> grant.objectType() == grantObjectType)
            .collect(Collectors.toSet());

    Set<PolicyGrant> result = new HashSet<>(baseGrants);
    result.addAll(specificGrants);
    return result;
  }

  private Set<PolicyGrant> getPrivilegeMatchedGrants(PolicyGrantPrivilege grantPrivilege) {
    return index.privilegeIndex().getOrDefault(grantPrivilege, Set.of());
  }
}
