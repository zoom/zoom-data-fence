package us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models;

import java.util.List;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

/** Data for creating grants. Standard for specific objects, Container for future/all grants. */
public sealed interface GrantsCreationData {
  String normalizedObjectName();

  SnowflakeObjectType objectType();

  List<PolicyGrantPrivilege> privileges();

  String roleName();

  record Standard(
      SnowflakeObjectType objectType,
      String normalizedObjectName,
      List<PolicyGrantPrivilege> privileges,
      String roleName)
      implements GrantsCreationData {}
  record Container(
      ContainerGrantsCreationData data, ContainerPatternOptions containerPatternOptions)
      implements GrantsCreationData {

    @Override
    public String normalizedObjectName() {
      return data().normalizedObjectName();
    }

    @Override
    public SnowflakeObjectType objectType() {
      return data().objectType();
    }

    @Override
    public List<PolicyGrantPrivilege> privileges() {
      return data.privileges();
    }

    @Override
    public String roleName() {
      return data.roleName();
    }
  }
}
