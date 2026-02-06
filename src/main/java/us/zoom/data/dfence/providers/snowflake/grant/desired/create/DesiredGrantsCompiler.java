package us.zoom.data.dfence.providers.snowflake.grant.desired.create;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.policies.factories.PolicyGrantFactory;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOption;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.GrantsCreationDataFactory;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.ContainerGrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.AllGrantsCompiler;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.FutureGrantsCompiler;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.StandardGrantsCompiler;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class DesiredGrantsCompiler {

  private final FutureGrantsCompiler futureGrantsCompiler;
  private final AllGrantsCompiler allGrantsCompiler;

  public DesiredGrantsCompiler(SnowflakeObjectsService snowflakeObjectsService) {
    this.futureGrantsCompiler = new FutureGrantsCompiler(snowflakeObjectsService);
    this.allGrantsCompiler = new AllGrantsCompiler(snowflakeObjectsService);
  }

  /**
   * Converts a playbook grant to Snowflake grant builders. Handles standard grants and container
   * grants (future/all).
   */
  public List<SnowflakeGrantBuilder> compileGrants(
      PlaybookPrivilegeGrant playbookPrivilegeGrant,
      String roleName,
      SnowflakeGrantBuilderOptions options) {
    return Try.of(
            () -> {
              Option<PolicyGrant> grant = PolicyGrantFactory.createFrom(playbookPrivilegeGrant);
              return grant.fold(
                  List::<SnowflakeGrantBuilder>of,
                  g ->
                      compileGrants(g, roleName).stream()
                          .map(x -> SnowflakeGrantBuilder.fromGrant(x, options))
                          .filter(Objects::nonNull)
                          .toList());
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

  private List<SnowflakeGrantModel> compileGrants(PolicyGrant grant, String roleName) {
    GrantsCreationData data =
        GrantsCreationDataFactory.createFrom(grant.policyType(), grant, roleName);

    if (data instanceof GrantsCreationData.Standard s) {
      return StandardGrantsCompiler.compileGrants(s);
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
      case FUTURE -> futureGrantsCompiler.compileGrants(c);
      case ALL -> allGrantsCompiler.compileGrants(c);
    };
  }
}
