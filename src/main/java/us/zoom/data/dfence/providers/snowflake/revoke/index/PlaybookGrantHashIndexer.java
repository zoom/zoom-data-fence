package us.zoom.data.dfence.providers.snowflake.revoke.index;

import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.PlaybookGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.models.*;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectTypeAlias;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaybookGrantHashIndexer {

  public static PlaybookGrantHashIndex create(List<PlaybookPrivilegeGrant> playbookGrants) {
    return Try.of(
            () -> {
              List<PlaybookGrant> allGrants =
                  playbookGrants.stream()
                      .map(PlaybookGrantCompanion::from)
                      .filter(java.util.Objects::nonNull)
                      .collect(Collectors.toList());

              return createFromGrants(allGrants);
            })
        .getOrElseThrow(
            e -> {
              log.error(
                  "Unexpected error creating playbook grant hash index: {} playbook grants provided",
                  playbookGrants.size(),
                  e);
              throw new RbacDataError(
                  "Failed to create playbook grant hash index: " + e.getMessage(), e);
            });
  }

  public static PlaybookGrantHashIndex createFromGrants(List<PlaybookGrant> enabledPlaybookGrants) {
    ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> privilegeIndex =
        buildPrivilegeIndex(enabledPlaybookGrants);
    ObjectTypeAndAliasIndex objectTypeAndAliasIndex =
        buildObjectTypeAndObjectTypeAliasIndex(enabledPlaybookGrants);

    return new PlaybookGrantHashIndex(
        privilegeIndex,
        objectTypeAndAliasIndex.objectTypeIndex(),
        objectTypeAndAliasIndex.objectAliasIndex());
  }

  private static ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> buildPrivilegeIndex(
      List<PlaybookGrant> playbookGrants) {
    ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> index = new ConcurrentHashMap<>();
    for (PlaybookGrant grant : playbookGrants) {
      List<GrantPrivilege> privileges = grant.privileges();
      for (GrantPrivilege privilege : privileges) {
        index.computeIfAbsent(privilege, k -> ConcurrentHashMap.newKeySet()).add(grant);
      }
    }
    return index;
  }

  private static ObjectTypeAndAliasIndex buildObjectTypeAndObjectTypeAliasIndex(
      List<PlaybookGrant> playbookGrants) {
    List<PlaybookGrant> primaryObjectTypeGrants = new ArrayList<>();
    List<PlaybookGrant> aliasObjectTypeGrants = new ArrayList<>();

    for (PlaybookGrant grant : playbookGrants) {
      if (grant.objectType().getObjectType().equals(grant.objectType().getAliasFor())) {
        primaryObjectTypeGrants.add(grant);
      } else {
        aliasObjectTypeGrants.add(grant);
      }
    }

    ConcurrentHashMap<ObjectType, Set<PlaybookGrant>> primaryIndex = new ConcurrentHashMap<>();
    for (PlaybookGrant grant : primaryObjectTypeGrants) {
      ObjectType key = ObjectType.apply(grant.objectType());
      primaryIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(grant);
    }

    ConcurrentHashMap<ObjectTypeAlias, Set<PlaybookGrant>> aliasIndex = new ConcurrentHashMap<>();
    for (PlaybookGrant grant : aliasObjectTypeGrants) {
      ObjectTypeAlias key = ObjectTypeAlias.apply(grant.objectType());
      aliasIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(grant);
    }

    return new ObjectTypeAndAliasIndex(primaryIndex, aliasIndex);
  }
}
