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
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOption;
import us.zoom.data.dfence.policies.factories.PolicyGrantFactory;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.GrantsCreationDataFactory;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.ContainerGrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.AllGrantsFactory;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.FutureGrantsFactory;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.StandardGrantsFactory;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class DesiredGrantsFactory {

  private final FutureGrantsFactory futureGrantsFactory;
  private final AllGrantsFactory allGrantsFactory;

  public DesiredGrantsFactory(SnowflakeObjectsService snowflakeObjectsService) {
    this.futureGrantsFactory = new FutureGrantsFactory(snowflakeObjectsService);
    this.allGrantsFactory = new AllGrantsFactory(snowflakeObjectsService);
  }

  /**
   * Converts a playbook grant to Snowflake grant builders. Handles standard grants and container
   * grants (future/all).
   */
  public List<SnowflakeGrantBuilder> createFrom(
      PlaybookPrivilegeGrant playbookPrivilegeGrant,
      String roleName,
      SnowflakeGrantBuilderOptions options) {
    return Try.of(
            () -> {
              PolicyGrant grant = PolicyGrantFactory.createFrom(playbookPrivilegeGrant);
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
        GrantsCreationDataFactory.createFrom(grant.policyType(), grant, roleName);

    if (data instanceof GrantsCreationData.Standard s) {
      return StandardGrantsFactory.createFrom(s);
    } else if (data instanceof GrantsCreationData.Container c) {
      return createContainerGrants(c);
    } else {
      log.error("Unknown grant creation data type: {} for role {}", data.getClass(), roleName);
      throw new RbacDataError("Unknown grant creation data type: " + data.getClass());
    }
  }

  private List<SnowflakeGrantModel> createContainerGrants(GrantsCreationData.Container container) {
    return container
        .containerPolicyOptions()
        .options()
        .foldLeft(
            new ArrayList<>(),
            (allGrants, option) -> {
              allGrants.addAll(getContainerGrantsForOption(container.data(), option));
              return allGrants;
            });
  }

  private List<SnowflakeGrantModel> getContainerGrantsForOption(
      ContainerGrantsCreationData c, ContainerPolicyOption option) {
    return switch (option) {
      case FUTURE -> futureGrantsFactory.createFrom(c);
      case ALL -> allGrantsFactory.createFrom(c);
    };
  }
}
