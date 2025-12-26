package us.zoom.data.dfence.providers.snowflake.revoke.companions;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.collection.NonEmptyList;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaybookGrantCompanion {

  public static Optional<PlaybookGrant> toPlaybookGrant(PlaybookPrivilegeGrant grant) {
    try {
      NonEmptyList<SnowflakeGrantType> grantTypes =
          getSnowflakeGrantTypes(grant.includeFuture(), grant.includeAll());

      Optional<String> dbName =
          Optional.ofNullable(grant.databaseName()).map(String::trim).filter(s -> !s.isEmpty());
      Optional<String> schName =
          Optional.ofNullable(grant.schemaName()).map(String::trim).filter(s -> !s.isEmpty());
      Optional<String> objName =
          Optional.ofNullable(grant.objectName()).map(String::trim).filter(s -> !s.isEmpty());

      NonEmptyList<GrantPrivilege> privileges =
          NonEmptyList.from(
              ImmutableList.copyOf(
                  grant.privileges().parallelStream()
                      .map(GrantPrivilege::new)
                      .collect(Collectors.toList())));

      return Optional.of(
          new PlaybookGrant(
              SnowflakeObjectType.fromString(grant.objectType()),
              new PlaybookPattern(dbName, schName, objName),
              privileges,
              grantTypes,
              grant.enable()));
    } catch (Exception err) {
      log.error("Conversion to playbook grant failed for playbook privilege grant {}", grant, err);
      return Optional.empty();
    }
  }

  private static NonEmptyList<SnowflakeGrantType> getSnowflakeGrantTypes(
      boolean includeFuture, boolean includeAll) {
    if (includeFuture && includeAll) {
      return NonEmptyList.of(SnowflakeGrantType.Future, SnowflakeGrantType.All);
    } else if (includeFuture) {
      return NonEmptyList.of(SnowflakeGrantType.Future);
    } else if (includeAll) {
      return NonEmptyList.of(SnowflakeGrantType.All);
    } else {
      return NonEmptyList.of(SnowflakeGrantType.Standard);
    }
  }
}
