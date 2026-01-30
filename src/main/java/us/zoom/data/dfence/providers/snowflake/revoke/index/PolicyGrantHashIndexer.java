package us.zoom.data.dfence.providers.snowflake.revoke.index;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.policies.factories.PolicyGrantFactory;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PolicyGrantHashIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeObjectTypeAndAliasIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeObjectTypeAlias;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyGrantHashIndexer {

  public static PolicyGrantHashIndex create(List<PlaybookPrivilegeGrant> PolicyGrants) {
    return Try.of(
            () -> {
              List<PolicyGrant> allGrants =
                  PolicyGrants.stream()
                      .map(PolicyGrantFactory::createFrom)
                      .filter(java.util.Objects::nonNull)
                      .collect(Collectors.toList());

              return createFromGrants(allGrants);
            })
        .getOrElseThrow(
            e -> {
              log.error(
                  "Unexpected error creating policy grant hash index: {} policy grants provided",
                  PolicyGrants.size(),
                  e);
              throw new RbacDataError(
                  "Failed to create policy grant hash index: " + e.getMessage(), e);
            });
  }

  public static PolicyGrantHashIndex createFromGrants(List<PolicyGrant> enabledPolicyGrants) {
    ConcurrentHashMap<PolicyGrantPrivilege, Set<PolicyGrant>> privilegeIndex =
        buildPrivilegeIndex(enabledPolicyGrants);
    SnowflakeObjectTypeAndAliasIndex objectTypeAndAliasIndex =
        buildObjectTypeAndObjectTypeAliasIndex(enabledPolicyGrants);

    return new PolicyGrantHashIndex(
        privilegeIndex,
        objectTypeAndAliasIndex.snowflakeObjectTypeIndex(),
        objectTypeAndAliasIndex.snowflakeObjectAliasIndex());
  }

  private static ConcurrentHashMap<PolicyGrantPrivilege, Set<PolicyGrant>> buildPrivilegeIndex(
      List<PolicyGrant> policyGrants) {
    ConcurrentHashMap<PolicyGrantPrivilege, Set<PolicyGrant>> index = new ConcurrentHashMap<>();
    for (PolicyGrant grant : policyGrants) {
      List<PolicyGrantPrivilege> privileges = grant.privileges();
      for (PolicyGrantPrivilege privilege : privileges) {
        index.computeIfAbsent(privilege, k -> ConcurrentHashMap.newKeySet()).add(grant);
      }
    }
    return index;
  }

  private static SnowflakeObjectTypeAndAliasIndex buildObjectTypeAndObjectTypeAliasIndex(
      List<PolicyGrant> PolicyGrants) {
    List<PolicyGrant> primaryObjectTypeGrants = new ArrayList<>();
    List<PolicyGrant> aliasObjectTypeGrants = new ArrayList<>();

    for (PolicyGrant grant : PolicyGrants) {
      if (grant.objectType().getObjectType().equals(grant.objectType().getAliasFor())) {
        primaryObjectTypeGrants.add(grant);
      } else {
        aliasObjectTypeGrants.add(grant);
      }
    }

    ConcurrentHashMap<SnowflakeObjectType, Set<PolicyGrant>> primaryIndex =
        new ConcurrentHashMap<>();
    for (PolicyGrant grant : primaryObjectTypeGrants) {
      SnowflakeObjectType key = grant.objectType();
      primaryIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(grant);
    }

    ConcurrentHashMap<SnowflakeObjectTypeAlias, Set<PolicyGrant>> aliasIndex =
        new ConcurrentHashMap<>();
    for (PolicyGrant grant : aliasObjectTypeGrants) {
      SnowflakeObjectTypeAlias key = SnowflakeObjectTypeAlias.of(grant.objectType());
      aliasIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(grant);
    }

    return new SnowflakeObjectTypeAndAliasIndex(primaryIndex, aliasIndex);
  }
}
