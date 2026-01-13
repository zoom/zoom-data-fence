package us.zoom.data.dfence.providers.snowflake.revoke.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.PlaybookGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.models.*;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectTypeAlias;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaybookGrantHashIndexer {

  public static PlaybookGrantHashIndex create(List<PlaybookPrivilegeGrant> playbookGrants) {
    List<PlaybookGrant> enabledGrants =
        playbookGrants.parallelStream()
            .map(PlaybookGrantCompanion::toPlaybookGrant)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(PlaybookGrant::enable)
            .collect(Collectors.toList());

    return createFromGrants(enabledGrants);
  }

  public static PlaybookGrantHashIndex createFromGrants(List<PlaybookGrant> enabledPlaybookGrants) {
    ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> privilegeIndex =
        buildPrivilegeIndex(enabledPlaybookGrants);
    ObjectTypeIndexes objectTypeIndexes =
        buildObjectTypeAndObjectTypeAliasIndex(enabledPlaybookGrants);

    return new PlaybookGrantHashIndex(
        privilegeIndex, objectTypeIndexes.objectTypeIndex(), objectTypeIndexes.objectAliasIndex());
  }

  private static ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> buildPrivilegeIndex(
      List<PlaybookGrant> playbookGrants) {
    ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> index = new ConcurrentHashMap<>();
    for (PlaybookGrant grant : playbookGrants) {
      for (GrantPrivilege privilege : grant.privileges()) {
        index.computeIfAbsent(privilege, k -> ConcurrentHashMap.newKeySet()).add(grant);
      }
    }
    return index;
  }

  private static ObjectTypeIndexes buildObjectTypeAndObjectTypeAliasIndex(
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

    return new ObjectTypeIndexes(primaryIndex, aliasIndex);
  }
}
