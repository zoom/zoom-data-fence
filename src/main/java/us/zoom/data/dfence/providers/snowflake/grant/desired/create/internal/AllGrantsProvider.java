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
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyGrantPrivilege;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class AllGrantsProvider {

  private final SnowflakeObjectsService snowflakeObjectsService;

  public List<SnowflakeGrantModel> createGrants(ContainerGrantsCreationData data) {
    return expandAllGrants(
        data.containerObjectType(),
        data.objectType(),
        data.normalizedObjectName(),
        data.privileges().stream().map(PolicyGrantPrivilege::value).toList(),
        data.roleName(),
        false);
  }

  private List<SnowflakeGrantModel> expandAllGrants(
      SnowflakeObjectType containerObjectType,
      SnowflakeObjectType objectType,
      String containerName,
      List<String> privileges,
      String roleName,
      Boolean grantOption) {
    if (!snowflakeObjectsService.objectExists(containerName, containerObjectType)) {
      log.warn(
          "Container {} {} does not exist. Skipping grant expansion.",
          containerObjectType,
          containerName);
      return List.of();
    }
    List<String> objects =
        snowflakeObjectsService.getContainerObjectQualNames(
            containerObjectType, objectType, containerName);
    List<SnowflakeGrantModel> results = new ArrayList<>(objects.size() * privileges.size());
    for (String objectName : objects) {
      for (String privilege : privileges) {
        results.add(
            new SnowflakeGrantModel(
                privilege,
                objectType.getObjectType().replace(" ", "_"),
                objectName,
                "ROLE",
                roleName,
                grantOption,
                false,
                false));
      }
    }
    return results;
  }
}
