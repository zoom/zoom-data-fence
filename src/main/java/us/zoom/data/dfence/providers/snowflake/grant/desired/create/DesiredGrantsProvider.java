package us.zoom.data.dfence.providers.snowflake.grant.desired.create;

import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.GrantsCreationDataProvider;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.ContainerGrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.AllGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.FutureGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.StandardGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.policies.companions.PlaybookGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyGrant;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class DesiredGrantsProvider {

  private final FutureGrantsProvider futureGrantsProvider;
  private final AllGrantsProvider allGrantsProvider;

  public DesiredGrantsProvider(SnowflakeObjectsService snowflakeObjectsService) {
    this.futureGrantsProvider = new FutureGrantsProvider(snowflakeObjectsService);
    this.allGrantsProvider = new AllGrantsProvider(snowflakeObjectsService);
  }

  /**
   * Converts a playbook grant to Snowflake grant builders. Handles standard grants and container
   * grants (future/all).
   */
  public List<SnowflakeGrantBuilder> playbookGrantToSnowflakeGrants(
      PlaybookPrivilegeGrant playbookPrivilegeGrant,
      String roleName,
      SnowflakeGrantBuilderOptions options) {
    return Try.of(
            () -> {
              PolicyGrant grant = PlaybookGrantCompanion.from(playbookPrivilegeGrant);
              return getGrants(grant, roleName).stream()
                  .map(x -> SnowflakeGrantBuilder.fromGrant(x, options))
                  .filter(Objects::nonNull)
                  .toList();
            })
        .getOrElseThrow(
            e -> {
              log.error(
                  "Failed to convert playbook grant to Snowflake grants for role {}: {}",
                  roleName,
                  playbookPrivilegeGrant,
                  e);
              return new RbacDataError(
                  "Failed to convert playbook grant to Snowflake grants for role " + roleName, e);
            });
  }

  private List<SnowflakeGrantModel> getGrants(PolicyGrant grant, String roleName) {
    GrantsCreationData data =
        GrantsCreationDataProvider.getGrantsCreationData(grant.resolvedPattern(), grant, roleName);

    if (data instanceof GrantsCreationData.Standard s) {
      return StandardGrantsProvider.createGrants(s);
    } else if (data instanceof GrantsCreationData.Container c) {
      return createContainerGrants(c);
    } else {
      log.error("Unknown grant creation data type: {} for role {}", data.getClass(), roleName);
      throw new RbacDataError("Unknown grant creation data type: " + data.getClass());
    }
  }

  private List<SnowflakeGrantModel> createContainerGrants(GrantsCreationData.Container container) {
    return container
        .containerPatternOptions()
        .options()
        .foldLeft(
            new ArrayList<>(),
            (allGrants, option) -> {
              allGrants.addAll(getContainerGrantsForOption(container.data(), option));
              return allGrants;
            });
  }

  private List<SnowflakeGrantModel> getContainerGrantsForOption(
      ContainerGrantsCreationData c, ContainerPatternOption option) {
    return switch (option) {
      case FUTURE -> futureGrantsProvider.createGrants(c);
      case ALL -> allGrantsProvider.createGrants(c);
    };
  }
}
