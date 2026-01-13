package us.zoom.data.dfence.providers.snowflake.revoke.companions;

import com.google.common.collect.ImmutableList;
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

      ImmutableList<GrantPrivilege> privileges =
          ImmutableList.copyOf(
              grant.privileges().parallelStream()
                  .map(GrantPrivilege::new)
                  .collect(Collectors.toList()));

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
    if (includeFuture && includeAll) {
      return PlaybookGrantType.FUTURE_AND_ALL;
    } else if (includeFuture) {
      return PlaybookGrantType.FUTURE;
    } else if (includeAll) {
      return PlaybookGrantType.ALL;
    } else {
      return PlaybookGrantType.STANDARD;
    }
  }
}
