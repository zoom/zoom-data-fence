package us.zoom.data.dfence.providers.snowflake.consistency;

import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.DesiredGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.revoke.SnowflakeRevokeGrantsCompiler;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrantRevokeConsistencyVerifier {

  /**
   * Verifies grant creation and revocation paths are consistent. Ensures grants created from
   * playbook grants would not be revoked.
   */
  public static void verifyAllGrants(
      List<PlaybookPrivilegeGrant> playbookGrants,
      DesiredGrantsProvider grantsProvider,
      String roleName) {
    Try.of(
            () -> {
              log.info(
                  "Verifying grant-revoke consistency for role {} with {} playbook grants",
                  roleName,
                  playbookGrants.size());

              verifyNoRevokesAreCreatedForPlaybookGrants(playbookGrants, grantsProvider, roleName);

              log.info("Grant-revoke consistency verification passed for role {}", roleName);
              return null;
            })
        .getOrElseThrow(
            e -> {
              log.error(
                  "Grant-revoke consistency check failed for role {}: {}",
                  roleName,
                  e.getMessage(),
                  e);
              return new RbacDataError(
                  "Grant-revoke consistency check failed for role " + roleName, e);
            });
  }

  private static void verifyNoRevokesAreCreatedForPlaybookGrants(
      List<PlaybookPrivilegeGrant> playbookGrants,
      DesiredGrantsProvider grantCreator,
      String roleName) {
    List<SnowflakeGrantBuilder> generatedGrantBuilders =
        playbookGrants.stream()
            .flatMap(
                playbookGrant ->
                    grantCreator
                        .playbookGrantToSnowflakeGrants(
                            playbookGrant, roleName, new SnowflakeGrantBuilderOptions())
                        .stream())
            .filter(Objects::nonNull)
            .toList();

    Map<String, SnowflakeGrantBuilder> currentGrantBuilders =
        generatedGrantBuilders.stream()
            .collect(
                Collectors.toMap(
                    SnowflakeGrantBuilder::getKey,
                    Function.identity(),
                    (existing, replacement) -> existing));

    List<SnowflakeGrantBuilder> revokeBuilders =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(playbookGrants, currentGrantBuilders);

    if (!revokeBuilders.isEmpty()) {
      String revokeDetails =
          revokeBuilders.stream()
              .map(b -> b.getGrant().toString())
              .collect(Collectors.joining("; "));
      String errorMessage =
          String.format(
              "Inconsistency detected: %d revoke(s) would be generated for playbook grants. Role: %s. Revokes: %s",
              revokeBuilders.size(), roleName, revokeDetails);
      log.error("Grant-revoke inconsistency detected for role {}: {}", roleName, revokeDetails);
      throw new RbacDataError(errorMessage);
    }
  }
}
