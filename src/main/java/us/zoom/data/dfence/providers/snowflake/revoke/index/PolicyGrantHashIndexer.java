package us.zoom.data.dfence.providers.snowflake.revoke.index;

import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.policies.factories.PolicyGrantFactory;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PolicyGrantHashIndex;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyGrantHashIndexer {

  public static PolicyGrantHashIndex create(List<PlaybookPrivilegeGrant> policyGrants) {
    return Try.of(
            () -> {
              List<PolicyGrant> allGrants =
                  policyGrants.stream()
                      .map(PolicyGrantFactory::createFrom)
                      .filter(java.util.Objects::nonNull)
                      .collect(Collectors.toList());

              return createFromGrants(allGrants);
            })
        .getOrElseThrow(
            e -> {
              log.error(
                  "Unexpected error creating policy grant hash index: {} policy grants provided",
                  policyGrants.size(),
                  e);
              throw new RbacDataError(
                  "Failed to create policy grant hash index: " + e.getMessage(), e);
            });
  }

  public static PolicyGrantHashIndex createFromGrants(List<PolicyGrant> allPolicyGrants) {
      ConcurrentHashMap<String, ConcurrentHashMap<PolicyGrantPrivilege, Set<PolicyGrant>>> kv = new ConcurrentHashMap<>();

      for (PolicyGrant grant : allPolicyGrants) {
          String alias = grant.objectType().getAliasFor();
          kv.computeIfAbsent(alias, k -> new ConcurrentHashMap<>());
          for (PolicyGrantPrivilege privilege : grant.privileges()) {
              kv.get(alias).computeIfAbsent(privilege, k -> new HashSet<>()).add(grant);
          }
      }

      return new PolicyGrantHashIndex(kv);
  }
}
