package us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models;

import java.util.List;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.shared.models.GrantPrivilege;

public record ContainerGrantsCreationData(
    SnowflakeObjectType objectType,
    SnowflakeObjectType containerObjectType,
    String normalizedObjectName,
    boolean isSchemaObjectWithAllSchemas,
    List<GrantPrivilege> privileges,
    String roleName) {}
