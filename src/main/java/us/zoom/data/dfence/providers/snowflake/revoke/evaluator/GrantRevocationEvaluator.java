package us.zoom.data.dfence.providers.snowflake.revoke.evaluator;

import io.vavr.control.Try;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.SnowflakeGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.matchers.playbook.PlaybookGrantMatcher;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantHashIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectTypeAlias;

@Slf4j
public class GrantRevocationEvaluator {
  private final PlaybookGrantHashIndex index;

  public GrantRevocationEvaluator(PlaybookGrantHashIndex index) {
    this.index = index;
  }

  /**
   * Determines if a grant should be revoked. Returns true if the grant doesn't match any playbook
   * grant.
   */
  public boolean needsRevoke(SnowflakeGrantModel grant) {
    return Try.of(
            () -> {
              SnowflakeGrant snowflakeGrant = SnowflakeGrantCompanion.from(grant);
              return !getCandidates(
                      snowflakeGrant.snowflakeObjectType(), snowflakeGrant.privilege())
                  .stream()
                  .anyMatch(
                      playbookGrant ->
                          PlaybookGrantMatcher.matchGrantAgainstPlaybook()
                              .test(playbookGrant, snowflakeGrant));
            })
        .getOrElseThrow(
            e -> {
              log.error("Failed to evaluate grant revocation for grant: {}", grant, e);
              return new RbacDataError("Failed to evaluate grant revocation for grant " + grant, e);
            });
  }

  private Set<PlaybookGrant> getCandidates(
      SnowflakeObjectType grantObjectType, GrantPrivilege grantPrivilege) {
    Set<PlaybookGrant> objectTypeGrants =
        getObjectTypeAndObjectTypeAliasMatchedGrants(grantObjectType);
    Set<PlaybookGrant> privilegeGrants = getPrivilegeMatchedGrants(grantPrivilege);

    return objectTypeGrants.stream().filter(privilegeGrants::contains).collect(Collectors.toSet());
  }

  private Set<PlaybookGrant> getObjectTypeAndObjectTypeAliasMatchedGrants(
      SnowflakeObjectType grantObjectType) {
    Set<PlaybookGrant> baseGrants =
        index.objectTypeIndex().getOrDefault(ObjectType.apply(grantObjectType), Set.of());

    Set<PlaybookGrant> specificGrants =
        index
            .objectAliasIndex()
            .getOrDefault(ObjectTypeAlias.apply(grantObjectType), Set.of())
            .stream()
            .filter(grant -> grant.objectType() == grantObjectType)
            .collect(Collectors.toSet());

    Set<PlaybookGrant> result = new HashSet<>(baseGrants);
    result.addAll(specificGrants);
    return result;
  }

  private Set<PlaybookGrant> getPrivilegeMatchedGrants(GrantPrivilege grantPrivilege) {
    return index.privilegeIndex().getOrDefault(grantPrivilege, Set.of());
  }
}
