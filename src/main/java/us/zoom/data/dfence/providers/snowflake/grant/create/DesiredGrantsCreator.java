package us.zoom.data.dfence.providers.snowflake.grant.create;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.sql.ObjectName;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class DesiredGrantsCreator {

  private final SnowflakeObjectsService snowflakeObjectsService;

  // Converts a playbook privilege grant to a list of Snowflake grant builders
  public List<SnowflakeGrantBuilder> playbookGrantToSnowflakeGrants(
      PlaybookPrivilegeGrant playbookPrivilegeGrant,
      String roleName,
      SnowflakeGrantBuilderOptions options) {
    List<SnowflakeGrantModel> grants = new ArrayList<>();
    grants.addAll(standardGrants(playbookPrivilegeGrant, roleName));
    grants.addAll(containerGrants(playbookPrivilegeGrant, roleName));
    return grants.stream()
        .map(x -> SnowflakeGrantBuilder.fromGrant(x, options))
        .filter(Objects::nonNull)
        .toList();
  }

  // Creates standard grants (non-wildcard, non-container grants)
  public List<SnowflakeGrantModel> standardGrants(
      PlaybookPrivilegeGrant playbookPrivilegeGrant, String roleName) {
    try {
      if (!"*".equals(playbookPrivilegeGrant.objectName())
          && !"*".equals(playbookPrivilegeGrant.schemaName())) {
        SnowflakeObjectType snowflakeObjectType =
            SnowflakeObjectType.fromString(playbookPrivilegeGrant.objectType().toUpperCase());
        String objectName =
            qualifiedObjectName(
                playbookPrivilegeGrant.databaseName(),
                playbookPrivilegeGrant.schemaName(),
                playbookPrivilegeGrant.objectName(),
                snowflakeObjectType);
        if (objectName == null) {
          log.info(
              "Skipping grant {} for role {} due to one or more objects not found.",
              playbookPrivilegeGrant,
              roleName);
          return List.of();
        }
        return playbookPrivilegeGrant.privileges().stream()
            .map(
                p ->
                    new SnowflakeGrantModel(
                        p,
                        playbookPrivilegeGrant.objectType(),
                        objectName,
                        "ROLE",
                        roleName,
                        false,
                        false,
                        false))
            .toList();
      } else {
        return List.of();
      }
    } catch (RuntimeException e) {
      throw new RuntimeException(
          String.format(
              "Unable to generate grants for role %s grant %s", roleName, playbookPrivilegeGrant),
          e);
    }
  }

  // Creates container grants (wildcard grants for future/all objects)
  public List<SnowflakeGrantModel> containerGrants(
      PlaybookPrivilegeGrant playbookPrivilegeGrant, String roleName) {
    if ("*".equals(playbookPrivilegeGrant.objectName())
        || "*".equals(playbookPrivilegeGrant.schemaName())) {
      if (playbookPrivilegeGrant.databaseName() == null
          || playbookPrivilegeGrant.databaseName().equals("*")) {
        throw new RbacDataError(
            String.format(
                "Database name not provided along with wildcard for objectName in permissions for role %s.",
                roleName));
      }
      SnowflakeObjectType containerObjectType;
      String containerName;
      if (playbookPrivilegeGrant.schemaName() == null
          || "*".equals(playbookPrivilegeGrant.schemaName())) {
        containerObjectType = SnowflakeObjectType.DATABASE;
        containerName =
            qualifiedAccountObjectName(
                playbookPrivilegeGrant.databaseName(), SnowflakeObjectType.DATABASE);
      } else {
        containerObjectType = SnowflakeObjectType.SCHEMA;
        containerName =
            qualifiedSchemaName(
                playbookPrivilegeGrant.databaseName(), playbookPrivilegeGrant.schemaName());
      }
      if (containerName == null) {
        log.warn(
            "Skipping grant {} for role {} due to one or more objects not found.",
            playbookPrivilegeGrant,
            roleName);
        return List.of();
      }
      SnowflakeObjectType objectType =
          SnowflakeObjectType.fromString(playbookPrivilegeGrant.objectType().toUpperCase());
      List<SnowflakeGrantModel> snowflakeGrantModels = new ArrayList<>();
      if (playbookPrivilegeGrant.includeFuture()) {
        // Grant on future
        snowflakeGrantModels.addAll(
            createFutureGrants(
                objectType, containerName, playbookPrivilegeGrant.privileges(), roleName, false));
        if (containerObjectType == SnowflakeObjectType.DATABASE && objectType.getQualLevel() > 2) {
          snowflakeGrantModels.addAll(
              createFutureSchemaObjectGrantsInAllSchemasInDatabase(
                  containerName, objectType, playbookPrivilegeGrant.privileges(), roleName, false));
        }
      }
      if (playbookPrivilegeGrant.includeAll()) {
        // Grant on all
        snowflakeGrantModels.addAll(
            expandAllGrants(
                containerObjectType,
                objectType,
                containerName,
                playbookPrivilegeGrant.privileges(),
                roleName,
                false));
      }
      return List.copyOf(snowflakeGrantModels);
    } else {
      return List.of();
    }
  }

  // Creates future grants for objects in a container
  public List<SnowflakeGrantModel> createFutureGrants(
      SnowflakeObjectType objectType,
      String containerName,
      List<String> privileges,
      String roleName,
      Boolean grantOption) {
    String objectName =
        String.format(
            "%s.<%s>", containerName, objectType.getObjectType().replace(" ", "_").toUpperCase());
    List<SnowflakeGrantModel> grants =
        privileges.stream()
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
    return grants;
  }

  // Creates future grants for schema objects in all schemas within a database
  public List<SnowflakeGrantModel> createFutureSchemaObjectGrantsInAllSchemasInDatabase(
      String databaseName,
      SnowflakeObjectType objectType,
      List<String> privileges,
      String roleName,
      Boolean grantOption) {
    if (!snowflakeObjectsService.objectExists(databaseName, SnowflakeObjectType.DATABASE)) {
      log.info(
          "Database {} does not exist. Skipping creation of future grants in schemas.",
          databaseName);
      return List.of();
    }
    List<String> schemas =
        snowflakeObjectsService.getContainerObjectQualNames(
            SnowflakeObjectType.DATABASE, SnowflakeObjectType.SCHEMA, databaseName);
    return schemas.stream()
        .flatMap(x -> createFutureGrants(objectType, x, privileges, roleName, grantOption).stream())
        .toList();
  }

  // Expands "ALL" grants to individual grants for all objects in a container
  public List<SnowflakeGrantModel> expandAllGrants(
      SnowflakeObjectType containerObjectType,
      SnowflakeObjectType objectType,
      String containerName,
      List<String> privileges,
      String roleName,
      Boolean grantOption) {
    if (!snowflakeObjectsService.objectExists(containerName, containerObjectType)) {
      log.warn(
          "{} {} does not exist. Skipping expanding grants.", containerObjectType, containerName);
      return List.of();
    }
    List<String> objects =
        snowflakeObjectsService.getContainerObjectQualNames(
            containerObjectType, objectType, containerName);
    List<SnowflakeGrantModel> results = new ArrayList<>();
    objects.forEach(
        objectName ->
            privileges.forEach(
                privilege ->
                    results.add(
                        new SnowflakeGrantModel(
                            privilege,
                            objectType.getObjectType().replace(" ", "_"),
                            objectName,
                            "ROLE",
                            roleName,
                            grantOption,
                            false,
                            false))));
    return results;
  }

  // Static helper methods for object name qualification

  private static String qualifiedAccountObjectName(
      String objectName, SnowflakeObjectType snowflakeObjectType) {
    if (objectName == null) {
      throw new RbacDataError("Object name may not be null.");
    }
    if (!snowflakeObjectType.getQualLevel().equals(1)) {
      throw new RbacDataError(
          String.format(
              "Object type %s has a qualification level of %s instead of the expected level of 1.",
              snowflakeObjectType.getObjectType(), snowflakeObjectType.getQualLevel()));
    }
    return ObjectName.normalizeObjectName(objectName);
  }

  private static String qualifiedSchemaName(String databaseName, String schemaName) {
    if (schemaName == null || schemaName.equals("*")) {
      return null;
    }
    String normalizedObjectName =
        ObjectName.normalizeObjectName(String.join(".", List.of(databaseName, schemaName)));
    return normalizedObjectName;
  }

  private static String qualifiedObjectName(
      String databaseName,
      String schemaName,
      String objectName,
      SnowflakeObjectType snowflakeObjectType) {
    Integer qualLevel = snowflakeObjectType.getQualLevel();
    switch (qualLevel) {
      case 0 -> {
        return "";
      }
      case 1 -> {
        if (snowflakeObjectType.equals(SnowflakeObjectType.DATABASE)) {
          return qualifiedAccountObjectName(databaseName, SnowflakeObjectType.DATABASE);
        } else {
          return qualifiedAccountObjectName(objectName, snowflakeObjectType);
        }
      }
      case 2 -> {
        return qualifiedSchemaName(databaseName, schemaName);
      }
      case 3 -> {
        if (objectName == null || objectName.equals("*")) {
          return null;
        }
        String normalizedObjectName =
            ObjectName.normalizeObjectName(
                String.join(".", List.of(databaseName, schemaName, objectName)));
        return normalizedObjectName;
      }
      default ->
          throw new RuntimeException(
              String.format("Invalid qualification level of %s.", qualLevel));
    }
  }
}
