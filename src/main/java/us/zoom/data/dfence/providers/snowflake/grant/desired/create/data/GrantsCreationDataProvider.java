package us.zoom.data.dfence.providers.snowflake.grant.desired.create.data;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.ContainerGrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.policies.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyGrant;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrantsCreationDataProvider {

  public static GrantsCreationData getGrantsCreationData(
          ResolvedPlaybookPattern target, PolicyGrant grant, String roleName) {
    SnowflakeObjectType objectType = grant.objectType();
    List<PolicyGrantPrivilege> privileges = grant.privileges();

    if (target instanceof ResolvedPlaybookPattern.Standard s) {
      String normalizedObjectName = normalizeObjectName(s);
      return new GrantsCreationData.Standard(
          objectType, normalizedObjectName, privileges, roleName);
    } else if (target instanceof ResolvedPlaybookPattern.Container c) {
      String normalizedObjectName = normalizeContainerName(c);

      boolean isSchemaObjectWithAllSchemas =
          c instanceof ResolvedPlaybookPattern.Container.SchemaObjectAllSchemas;

      return new GrantsCreationData.Container(
          new ContainerGrantsCreationData(
              objectType,
              c.containerObjectType(),
              normalizedObjectName,
              isSchemaObjectWithAllSchemas,
              privileges,
              roleName),
          c.containerPatternOptions());
    } else {
      throw new RbacDataError("Unknown resolved playbook pattern type: " + target.getClass());
    }
  }

  private static String normalizeObjectName(ResolvedPlaybookPattern.Standard target) {
    if (target instanceof ResolvedPlaybookPattern.Standard.Global) {
      return "";
    } else if (target instanceof ResolvedPlaybookPattern.Standard.AccountObject t) {
      return ObjectName.normalizeObjectName(t.objectName());
    } else if (target instanceof ResolvedPlaybookPattern.Standard.AccountObjectDatabase t) {
      return ObjectName.normalizeObjectName(t.databaseName());
    } else if (target instanceof ResolvedPlaybookPattern.Standard.Schema t) {
      return ObjectName.normalizeObjectName(
          String.join(".", List.of(t.databaseName(), t.schemaName())));
    } else if (target instanceof ResolvedPlaybookPattern.Standard.SchemaObject t) {
      return ObjectName.normalizeObjectName(
          String.join(".", List.of(t.databaseName(), t.schemaName(), t.objectName())));
    } else {
      throw new RbacDataError("Unknown standard pattern type: " + target.getClass());
    }
  }

  private static String normalizeContainerName(ResolvedPlaybookPattern.Container container) {
    if (container instanceof ResolvedPlaybookPattern.Container.AccountObjectDatabase t) {
      return ObjectName.normalizeObjectName(t.databaseName());
    } else if (container instanceof ResolvedPlaybookPattern.Container.Schema t) {
      return ObjectName.normalizeObjectName(
          String.join(".", List.of(t.databaseName(), t.schemaName())));
    } else if (container instanceof ResolvedPlaybookPattern.Container.SchemaObjectAllSchemas t) {
      return ObjectName.normalizeObjectName(t.databaseName());
    } else {
      throw new RbacDataError("Unknown container pattern type: " + container.getClass());
    }
  }
}
