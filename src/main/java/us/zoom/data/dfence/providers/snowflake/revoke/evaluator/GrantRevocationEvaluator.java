package us.zoom.data.dfence.providers.snowflake.revoke.evaluator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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
  private final PlaybookGrantMatcher matcher = PlaybookGrantMatcher.apply();

  public GrantRevocationEvaluator(PlaybookGrantHashIndex index) {
    this.index = index;
  }

  public boolean needsRevoke(SnowflakeGrantModel grant) {
    try {
      SnowflakeGrant snowflakeGrant =
          SnowflakeGrantCompanion.from(grant)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          String.format(
                              "Unable to create snow grant from snowflake grant model %s", grant)));

      Set<PlaybookGrant> candidates =
          getCandidates(snowflakeGrant.snowflakeObjectType(), snowflakeGrant.privilege());

      boolean matches =
          candidates.parallelStream()
              .anyMatch(
                  playbookGrant ->
                      matcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowflakeGrant));

      if (!matches) {
        log.info("Snowflake grant {} invalid, needs to be revoked", grant);
      } else {
        log.info("Snowflake grant {} is valid", grant);
      }
      return !matches;
    } catch (Exception err) {
      log.error("Error occurred while checking revocation of grants on model: {}", grant, err);
      return false;
    }
  }

  private Set<PlaybookGrant> getCandidates(
      SnowflakeObjectType grantObjectType, GrantPrivilege grantPrivilege) {
    Set<PlaybookGrant> objectTypeGrants =
        getObjectTypeAndObjectTypeAliasMatchedGrants(grantObjectType);
    Set<PlaybookGrant> privilegeGrants = getPrivilegeMatchedGrants(grantPrivilege);

    Set<PlaybookGrant> intersection = new HashSet<>(objectTypeGrants);
    intersection.retainAll(privilegeGrants);
    return intersection;
  }

  private Set<PlaybookGrant> getObjectTypeAndObjectTypeAliasMatchedGrants(
      SnowflakeObjectType grantObjectType) {
    Set<PlaybookGrant> baseGrants =
        index.objectTypeIndex().getOrDefault(ObjectType.apply(grantObjectType), Set.of());

    Set<PlaybookGrant> specificGrants =
        index
            .objectAliasIndex()
            .getOrDefault(ObjectTypeAlias.apply(grantObjectType), Set.of())
            .parallelStream()
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
