package us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models;

import java.util.List;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models.ContainerPatternOptions;

/** Plan for creating grants. Standard for specific objects, Container for future/all grants. */
public sealed interface GrantsCreationData {
  String normalizedObjectName();

  SnowflakeObjectType objectType();

  List<GrantPrivilege> privileges();

  String roleName();

  record Standard(
      SnowflakeObjectType objectType,
      String normalizedObjectName,
      List<GrantPrivilege> privileges,
      String roleName)
      implements GrantsCreationData {}
  record Container(
      SnowflakeObjectType objectType,
      SnowflakeObjectType containerObjectType,
      ContainerPatternOptions options,
      String normalizedObjectName,
      boolean isSchemaObjectWithAllSchemas,
      List<GrantPrivilege> privileges,
      String roleName)
      implements GrantsCreationData {}
}
