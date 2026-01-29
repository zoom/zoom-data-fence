package us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models;

import java.util.List;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;

public record ContainerGrantsCreationData(
    SnowflakeObjectType objectType,
    SnowflakeObjectType containerObjectType,
    String normalizedObjectName,
    boolean isSchemaObjectWithAllSchemas,
    List<PolicyGrantPrivilege> privileges,
    String roleName) {}
