package us.zoom.data.dfence.providers.snowflake.shared.companions;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.companions.ResolvedPlaybookPatternCompanion;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.shared.models.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPatternOptions;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaybookGrantCompanion {

  public static PlaybookGrant from(PlaybookPrivilegeGrant grant) {
    return getPlaybookGrant(grant)
        .getOrElseThrow(
            err -> {
              log.error(
                  "Failed to convert playbook privilege grant to playbook grant: {}", grant, err);
              return new RbacDataError(
                  "Failed to convert playbook privilege grant: " + err.getMessage(), err);
            });
  }

  private static Try<PlaybookGrant> getPlaybookGrant(PlaybookPrivilegeGrant grant) {
    return getSnowflakeObjectType(grant.objectType())
        .flatMap(snowflakeObjectType -> getPlaybookGrant(snowflakeObjectType, grant));
  }

  private static Try<PlaybookGrant> getPlaybookGrant(
      SnowflakeObjectType snowflakeObjectType, PlaybookPrivilegeGrant grant) {
    return Try.of(
        () -> {
          Option<String> dbName =
              Option.of(grant.databaseName()).map(String::trim).filter(s -> !s.isEmpty());
          Option<String> schName =
              Option.of(grant.schemaName()).map(String::trim).filter(s -> !s.isEmpty());
          Option<String> objName =
              Option.of(grant.objectName()).map(String::trim).filter(s -> !s.isEmpty());

          PlaybookPattern pattern = PlaybookPattern.of(dbName, schName, objName);
          PlaybookPatternOptions options =
              new PlaybookPatternOptions(grant.includeFuture(), grant.includeAll());
          ResolvedPlaybookPattern resolvedPlaybookPattern =
              of(pattern, snowflakeObjectType, options);

          List<GrantPrivilege> privileges =
              grant.privileges().stream().map(GrantPrivilege::new).collect(Collectors.toList());

          return new PlaybookGrant(
              snowflakeObjectType, pattern, privileges, resolvedPlaybookPattern, grant.enable());
        });
  }

  private static Try<SnowflakeObjectType> getSnowflakeObjectType(String grantObjectType) {
    return Try.of(() -> SnowflakeObjectType.fromString(grantObjectType));
  }

  private static ResolvedPlaybookPattern of(
      PlaybookPattern pattern,
      SnowflakeObjectType snowflakeObjectType,
      PlaybookPatternOptions options) {
    return Try.of(
            () -> ResolvedPlaybookPatternCompanion.from(pattern, snowflakeObjectType, options))
        .flatMap(
            validatedPattern ->
                validatedPattern.fold(
                    errors ->
                        Try.failure(
                            new RbacDataError(
                                "Playbook pattern validation failed: "
                                    + errors
                                        .map(ValidationError::message)
                                        .mkString("[", ", ", "]"))),
                    Try::success))
        .getOrElseThrow(e -> new RbacDataError("Pattern validation failed: " + e.getMessage(), e));
  }
}
