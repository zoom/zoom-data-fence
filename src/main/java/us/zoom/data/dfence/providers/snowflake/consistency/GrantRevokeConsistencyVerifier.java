package us.zoom.data.dfence.providers.snowflake.consistency;

import io.vavr.collection.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.create.DesiredGrantsCreator;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.PlaybookGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.SnowflakeGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.matchers.playbook.PlaybookGrantMatcher;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrantRevokeConsistencyVerifier {

  // Verifies that grants generated via grant path match the playbook grant using revoke path
  // matching logic
  public static void verifyAllGrants(
      java.util.List<PlaybookPrivilegeGrant> playbookGrants,
      DesiredGrantsCreator grantCreator,
      String roleName) {
    try {
      log.info(
          "Starting grant-revoke consistency verification for role {} ({} playbook grants)",
          roleName,
          playbookGrants.size());

      for (PlaybookPrivilegeGrant playbookGrant : List.ofAll(playbookGrants)) {
        verifyPlaybookGrant(playbookGrant, grantCreator, roleName);
      }

      log.info(
          "Grant-revoke consistency verification completed successfully for role {}", roleName);
    } catch (RbacDataError e) {
      throw e;
    } catch (Exception e) {
      log.error("Error generating grants for role {}: {}", roleName, e.getMessage(), e);
      throw new RbacDataError(
          String.format("Error generating grants for role %s: %s", roleName, e.getMessage()), e);
    }
  }

  private static void verifyPlaybookGrant(
      PlaybookPrivilegeGrant playbookGrant, DesiredGrantsCreator grantCreator, String roleName) {
    // Convert playbook grant to revoke path model
    PlaybookGrant playbookGrantForRevoke =
        PlaybookGrantCompanion.toPlaybookGrant(playbookGrant)
            .orElseThrow(
                () ->
                    new RbacDataError(
                        String.format(
                            "Playbook grant conversion failed for role %s: %s",
                            roleName, playbookGrant)));

    // Generate grants via grant path using injected grant creator
    List<SnowflakeGrantBuilder> grantBuilders =
        List.ofAll(
            grantCreator.playbookGrantToSnowflakeGrants(
                playbookGrant, roleName, new SnowflakeGrantBuilderOptions()));

    // Verify each generated grant matches the playbook grant that generated it
    grantBuilders
        .filter(Objects::nonNull)
        .forEach(builder -> verifyGrant(builder, playbookGrantForRevoke, playbookGrant, roleName));
  }

  // Verifies a single grant and throws RbacDataError if it doesn't match the playbook grant
  private static void verifyGrant(
      SnowflakeGrantBuilder builder,
      PlaybookGrant playbookGrantForRevoke,
      PlaybookPrivilegeGrant sourcePlaybookGrant,
      String roleName) {
    SnowflakeGrantModel grantModel = builder.getGrant();

    SnowflakeGrant snowflakeGrant =
        SnowflakeGrantCompanion.from(grantModel)
            .orElseThrow(
                () ->
                    new RbacDataError(
                        String.format(
                            "Grant model conversion failed for role %s: %s",
                            roleName, grantModel)));

    // Verify the generated grant matches the playbook grant that generated it
    boolean matches =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook()
            .apply(playbookGrantForRevoke, snowflakeGrant);

    if (!matches) {
      throw new RbacDataError(
          String.format(
              "Grant-Revoke mismatch detected: grant %s does not match playbook grant %s",
              grantModel, sourcePlaybookGrant));
    }
  }
}
