package us.zoom.data.dfence.providers.snowflake.grant.desired.create.data;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.ContainerGrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrantsCreationDataFactory {

  public static GrantsCreationData createFrom(
      PolicyType target, PolicyGrant grant, String roleName) {
    SnowflakeObjectType objectType = grant.objectType();
    List<PolicyGrantPrivilege> privileges = grant.privileges();

    if (target instanceof PolicyType.Standard s) {
      return new GrantsCreationData.Standard(
          objectType, s.normalizeObjectName(), privileges, roleName);
    } else if (target instanceof PolicyType.Container c) {
      boolean isSchemaObjectWithAllSchemas =
          c instanceof PolicyType.Container.SchemaObjectAllSchemas;

      return new GrantsCreationData.Container(
          new ContainerGrantsCreationData(
              objectType,
              c.containerObjectType(),
              c.normalizeObjectName(),
              isSchemaObjectWithAllSchemas,
              privileges,
              roleName),
          c.containerPolicyOptions());
    } else {
      throw new RbacDataError("Unknown resolved playbook pattern type: " + target.getClass());
    }
  }
}
