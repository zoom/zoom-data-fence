package us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.ContainerGrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.shared.models.GrantPrivilege;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class FutureGrantsProvider {

  private final SnowflakeObjectsService snowflakeObjectsService;

  /** Creates future grants on containers using <OBJECT_TYPE> syntax. */
  public List<SnowflakeGrantModel> createGrants(ContainerGrantsCreationData data) {
    if (data.isSchemaObjectWithAllSchemas()) {
      List<String> privileges = data.privileges().stream().map(GrantPrivilege::value).toList();
      List<SnowflakeGrantModel> grants = new ArrayList<>();
      grants.addAll(
          createFutureGrantsOnContainer(
              data.objectType(), data.normalizedObjectName(), privileges, data.roleName(), false));
      grants.addAll(
          createFutureSchemaObjectGrantsInAllSchemasInDatabase(
              data.normalizedObjectName(), data.objectType(), privileges, data.roleName(), false));
      return grants;
    } else {
      return createFutureGrantsOnContainer(
          data.objectType(),
          data.normalizedObjectName(),
          data.privileges().stream().map(GrantPrivilege::value).toList(),
          data.roleName(),
          false);
    }
  }

  private List<SnowflakeGrantModel> createFutureSchemaObjectGrantsInAllSchemasInDatabase(
      String databaseName,
      SnowflakeObjectType objectType,
      List<String> privileges,
      String roleName,
      Boolean grantOption) {
    if (!snowflakeObjectsService.objectExists(databaseName, SnowflakeObjectType.DATABASE)) {
      log.info("Database {} does not exist. Skipping future grants for schemas.", databaseName);
      return List.of();
    }
    List<String> schemas =
        snowflakeObjectsService.getContainerObjectQualNames(
            SnowflakeObjectType.DATABASE, SnowflakeObjectType.SCHEMA, databaseName);
    return schemas.stream()
        .flatMap(
            schema ->
                createFutureGrantsOnContainer(objectType, schema, privileges, roleName, grantOption)
                    .stream())
        .toList();
  }

  private List<SnowflakeGrantModel> createFutureGrantsOnContainer(
      SnowflakeObjectType objectType,
      String containerName,
      List<String> privileges,
      String roleName,
      Boolean grantOption) {
    String objectName = String.format("%s.<%s>", containerName, objectType.name());
    return privileges.stream()
        .map(
            p ->
                new SnowflakeGrantModel(
                    p,
                    objectType.getObjectType().replace(" ", "_"),
                    objectName,
                    "ROLE",
                    roleName,
                    grantOption,
                    true,
                    false))
        .toList();
  }
}
