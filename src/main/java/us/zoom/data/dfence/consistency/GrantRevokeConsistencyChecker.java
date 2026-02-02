package us.zoom.data.dfence.consistency;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.revoke.SnowflakeRevokeGrantsCompiler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrantRevokeConsistencyChecker {

  /**
   * Checks that grants compiled from playbook grants would not be selected for revocation.
   */
  public static void check(
          List<PlaybookPrivilegeGrant> playbookGrants,
          Map<String, SnowflakeGrantBuilder> desiredGrantBuilders, String roleName) {
    Try.of(
            () -> {
              log.info(
                  "Verifying grant-revoke consistency for role {} with {} playbook grants",
                  roleName,
                  playbookGrants.size());

              verifyNoRevokesAreCreatedForPlaybookGrants(playbookGrants, desiredGrantBuilders, roleName);

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
      Map<String, SnowflakeGrantBuilder> desiredGrantBuilders,
      String roleName) {

    List<SnowflakeGrantBuilder> revokeBuilders =
        SnowflakeRevokeGrantsCompiler.compileRevokeGrants(playbookGrants, desiredGrantBuilders);

    if (!revokeBuilders.isEmpty()) {
      String revokeDetails =
          revokeBuilders.stream()
              .map(b -> b.getGrant().toString())
              .collect(Collectors.joining("; "));
      String errorMessage =
          String.format(
              "Grant-revoke inconsistency: %d revoke(s) would be generated for playbook grants (role=%s). Revokes: %s",
              revokeBuilders.size(), roleName, revokeDetails);
      throw new RbacDataError(errorMessage);
    }
  }
}
