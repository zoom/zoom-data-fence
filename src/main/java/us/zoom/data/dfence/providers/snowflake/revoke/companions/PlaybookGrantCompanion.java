package us.zoom.data.dfence.providers.snowflake.revoke.companions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaybookGrantCompanion {

  public static Optional<PlaybookGrant> toPlaybookGrant(PlaybookPrivilegeGrant grant) {
    try {
      PlaybookGrantType grantType = getPlaybookGrantType(grant.includeFuture(), grant.includeAll());

      Optional<String> dbName =
          Optional.ofNullable(grant.databaseName()).map(String::trim).filter(s -> !s.isEmpty());
      Optional<String> schName =
          Optional.ofNullable(grant.schemaName()).map(String::trim).filter(s -> !s.isEmpty());
      Optional<String> objName =
          Optional.ofNullable(grant.objectName()).map(String::trim).filter(s -> !s.isEmpty());

      List<GrantPrivilege> privileges =
          grant.privileges().parallelStream().map(GrantPrivilege::new).collect(Collectors.toList());

      return Optional.of(
          new PlaybookGrant(
              SnowflakeObjectType.fromString(grant.objectType()),
              new PlaybookPattern(dbName, schName, objName),
              privileges,
              grantType,
              grant.enable()));
    } catch (Exception err) {
      log.error("Conversion to playbook grant failed for playbook privilege grant {}", grant, err);
      return Optional.empty();
    }
  }

  private static PlaybookGrantType getPlaybookGrantType(boolean includeFuture, boolean includeAll) {
    if (includeFuture && includeAll) return PlaybookGrantType.FutureAndAll;
    if (includeFuture) return PlaybookGrantType.Future;
    if (includeAll) return PlaybookGrantType.All;
    return PlaybookGrantType.Standard;
  }
}
