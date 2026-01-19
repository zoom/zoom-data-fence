package us.zoom.data.dfence.providers.snowflake;

import lombok.extern.slf4j.Slf4j;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import us.zoom.data.dfence.CompiledChanges;
import us.zoom.data.dfence.Provider;
import us.zoom.data.dfence.exception.DatabaseError;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.data.dfence.providers.snowflake.grant.builder.GrantBuilderDiff;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.PartitionedGrantStatements;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SnowflakeProvider implements Provider {

    private final SnowflakeGrantsService snowflakeGrantsService;

    private final SnowflakeStatementsService snowflakeStatementsService;

    private final SnowflakeObjectsService snowflakeObjectsService;

    private final ForkJoinPool forkJoinPool;

    /**
     * Constructor that uses the common ForkJoinPool.
     * 
     * @param snowflakeStatementsService the statements service
     * @param snowflakeGrantsService the grants service
     * @param snowflakeObjectsService the objects service
     */
    public SnowflakeProvider(
            SnowflakeStatementsService snowflakeStatementsService,
            SnowflakeGrantsService snowflakeGrantsService,
            SnowflakeObjectsService snowflakeObjectsService) {
        this(snowflakeStatementsService, snowflakeGrantsService, snowflakeObjectsService, ForkJoinPool.commonPool());
    }

    /**
     * Constructor that accepts a custom ForkJoinPool.
     * 
     * @param snowflakeStatementsService the statements service
     * @param snowflakeGrantsService the grants service
     * @param snowflakeObjectsService the objects service
     * @param forkJoinPool the fork join pool to use for parallel operations
     */
    public SnowflakeProvider(
            SnowflakeStatementsService snowflakeStatementsService,
            SnowflakeGrantsService snowflakeGrantsService,
            SnowflakeObjectsService snowflakeObjectsService,
            ForkJoinPool forkJoinPool) {
        this.snowflakeStatementsService = snowflakeStatementsService;
        this.snowflakeGrantsService = snowflakeGrantsService;
        this.snowflakeObjectsService = snowflakeObjectsService;
        this.forkJoinPool = forkJoinPool;
    }

    public static String qualifiedAccountObjectName(String objectName, SnowflakeObjectType snowflakeObjectType) {
        if (objectName == null) {
            throw new RbacDataError("Object name may not be null.");
        }
        if (!snowflakeObjectType.getQualLevel().equals(1)) {
            throw new RbacDataError(String.format(
                    "Object type %s has a qualification level of %s instead of the expected level of 1.",
                    snowflakeObjectType.getObjectType(),
                    snowflakeObjectType.getQualLevel()));
        }
        return ObjectName.normalizeObjectName(objectName);
    }

    public static String qualifiedSchemaName(String databaseName, String schemaName) {
        if (schemaName == null || schemaName.equals("*")) {
            return null;
        }
        String normalizedObjectName = ObjectName.normalizeObjectName(String.join(
                ".",
                List.of(databaseName, schemaName)));
        return normalizedObjectName;
    }

    public static String qualifiedObjectName(
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
                String normalizedObjectName = ObjectName.normalizeObjectName(String.join(
                        ".",
                        List.of(databaseName, schemaName, objectName)));
                return normalizedObjectName;
            }
            default -> throw new RuntimeException(String.format("Invalid qualification level of %s.", qualLevel));
        }
    }

    @Override
    public List<CompiledChanges> compileChanges(PlaybookModel playbookModel, Boolean ignoreUnknownGrants) {
        Boolean consolidateWildcardGrantsToAll = false;
        log.debug("Compiling changes.");
        this.snowflakeObjectsService.clearCache();
        List<String> existingRoles = snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.ROLE,
                "");
        return forkJoinPool.submit(() -> 
            playbookModel.roles().keySet().parallelStream().map(k -> this.compileRoleChanges(
                    k,
                    playbookModel.roles().get(k),
                    existingRoles,
                    consolidateWildcardGrantsToAll,
                    playbookModel,
                    ignoreUnknownGrants)).filter(CompiledChanges::containsChanges)
            .sorted(Comparator.comparing(CompiledChanges::roleName)).toList()
        ).join();
    }

    @Override
    public void applyPrivilegeChanges(List<CompiledChanges> compiledChanges) {
        log.info("Applying privilege changes for {} roles.", compiledChanges.size());
        forkJoinPool.submit(() -> 
            compiledChanges.parallelStream().forEach(this::applyPrivilegeChangesToRole)
        ).join();
    }

    public void applyPrivilegeChangesToRole(CompiledChanges compiledChanges) {
        log.info("Applying {} ownership grants and {} non-ownership grants for role {}",
            compiledChanges.ownershipGrantStatements().size(),
            compiledChanges.roleGrantStatements().size(),
            compiledChanges.roleName());
        try {
            // First, run ownership grants in parallel
            if (!compiledChanges.ownershipGrantStatements().isEmpty()) {
                log.info("Applying {} ownership grants for role {}", 
                    compiledChanges.ownershipGrantStatements().size(), compiledChanges.roleName());
                forkJoinPool.submit(() -> 
                    compiledChanges.ownershipGrantStatements().parallelStream().forEach(snowflakeStatementsService::applyStatements)
                ).join();
            }
            
            // Then, run the rest of the grants in parallel
            if (!compiledChanges.roleGrantStatements().isEmpty()) {
                log.info("Applying {} non-ownership grants for role {}",
                    compiledChanges.roleGrantStatements().size(), compiledChanges.roleName());
                forkJoinPool.submit(() -> 
                    compiledChanges.roleGrantStatements().parallelStream().forEach(snowflakeStatementsService::applyStatements)
                ).join();
            }
        } catch (DatabaseError e) {
            throw new DatabaseError("Unable to apply privileges to role due to database error.", e);
        }
    }

    @Override
    public void applyRolesChanges(List<CompiledChanges> compiledChanges) {
        log.info("Applying {} role changes.", compiledChanges.size());
        forkJoinPool.submit(() -> 
            compiledChanges.parallelStream().forEach(this::applyRoleChanges)
        ).join();
    }

    @Override
    public List<PlaybookRoleModel> importRoles(List<String> roleNames) {
        for (String roleName : roleNames) {
            if (!snowflakeObjectsService.objectExists(roleName, SnowflakeObjectType.ROLE)) {
                throw new RbacDataError(String.format("Role %s not found", roleName));
            }
        }
        return forkJoinPool.submit(() -> 
            roleNames.parallelStream().map(this::importRole).sorted(Comparator.comparing(PlaybookRoleModel::name))
                .toList()
        ).join();
    }

    @Override
    public List<PlaybookRoleModel> importRolePatterns(List<Pattern> rolePatterns) {
        List<String> existingRoleNames = snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.ROLE,
                "");
        List<String> filteredRoles = existingRoleNames.stream().filter(s -> rolePatterns.stream()
                .anyMatch(p -> p.matcher(s.toLowerCase()).find() || p.matcher(s.toUpperCase()).find())).toList();
        return forkJoinPool.submit(() -> 
            filteredRoles.parallelStream().map(this::importRole).toList().stream()
                .sorted(Comparator.comparing(PlaybookRoleModel::name)).toList()
        ).join();
    }

    @Override
    public List<String> listRoles() {
        return snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.ACCOUNT,
                SnowflakeObjectType.ROLE,
                "");
    }

    public PlaybookRoleModel importRole(String roleName) {
        Map<String, SnowflakeGrantBuilder> grants = snowflakeGrantsService.getGrants(roleName);
        List<PlaybookPrivilegeGrant> playbookPrivilegeGrants = grants.values().stream()
                .map(SnowflakeGrantBuilder::playbookPrivilegeGrant).toList();
        return new PlaybookRoleModel(roleName.toLowerCase(), playbookPrivilegeGrants);
    }

    public void applyRoleChanges(CompiledChanges compiledChanges) {
        try {
            log.info("Applying role changes for role {}", compiledChanges.roleName());
            snowflakeStatementsService.applyStatements(compiledChanges.roleCreationStatements());
        } catch (DatabaseError e) {
            throw new DatabaseError(
                    String.format(
                            "Unable to create role with name %s and id %s due to database error.",
                            compiledChanges.roleName(),
                            compiledChanges.roleId()), e);
        }
    }

    public CompiledChanges compileRoleChanges(
            String roleId,
            PlaybookRoleModel role,
            List<String> existingRoles,
            Boolean consolidateWildcardsToAllGrants,
            PlaybookModel playbookModel,
            Boolean ignoreUnknownGrants) {
        log.info("Compiling changes for role {}", role.name());
        Boolean roleExists = existingRoles.contains(role.name().toUpperCase());
        List<String> roleCreationStatements = new ArrayList<>();
        Boolean roleWillExist = roleExists;
        if (!roleExists && role.create()) {
            log.debug("Role {} does not exist so we are going to add a create statement.", role.name());
            roleCreationStatements.add(String.format("CREATE ROLE IF NOT EXISTS %s;", role.name().toUpperCase()));
            roleWillExist = true;
            if (role.roleOwner() != null) {
                roleCreationStatements.add(String.format(
                        "GRANT OWNERSHIP ON ROLE %s TO ROLE %s COPY CURRENT GRANTS;",
                        role.name().toUpperCase(),
                        role.roleOwner()));
            }
        } else {
            log.debug("Skipping role creation for role {}.", role.name());
        }
        List<List<String>> ownershipGrantStatements = new ArrayList<>();
        List<List<String>> nonOwnershipGrantStatements = new ArrayList<>();
        if (roleWillExist) {
            log.debug("Compiling grants for role {}.", role.name());
            PartitionedGrantStatements partitionedGrantStatements = compilePlaybookPrivilegeGrants(
                    role.grants(),
                    role.name(),
                    roleExists,
                    role.revokeOtherGrants(),
                    consolidateWildcardsToAllGrants,
                    playbookModel,
                    ignoreUnknownGrants,
                    role.unsupportedRevokeBehavior());
            ownershipGrantStatements.addAll(partitionedGrantStatements.ownershipStatements());
            nonOwnershipGrantStatements.addAll(partitionedGrantStatements.nonOwnershipStatements());
        } else {
            log.debug(
                    "Skipping grant statement creation for role {} because it does not exist " + "and is not planned to be created.",
                    role.name());
        }
        
        log.debug("{} ownership statements and {} non-ownership statements planned for role {}",
            ownershipGrantStatements.size(), nonOwnershipGrantStatements.size(), role.name());
        return new CompiledChanges(roleId, role.name(), ownershipGrantStatements, roleCreationStatements, nonOwnershipGrantStatements);
    }

    PartitionedGrantStatements compilePlaybookPrivilegeGrants(
            List<PlaybookPrivilegeGrant> privilegeGrants,
            String roleName,
            Boolean roleExists,
            Boolean revokeOtherGrants,
            Boolean consolidateWildcardsToAllGrants,
            PlaybookModel playbookModel,
            Boolean ignoreUnknownGrants,
            UnsupportedRevokeBehavior unsupportedRevokeBehavior) {
        try {
            SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
            options.setSuppressErrors(ignoreUnknownGrants);
            options.setUnsupportedRevokeBehavior(unsupportedRevokeBehavior);
            Map<String, SnowflakeGrantBuilder> desiredGrantBuilders = forkJoinPool.submit(() -> 
                privilegeGrants.parallelStream()
                    .flatMap(x -> playbookGrantToSnowflakeGrants(x, roleName, options).stream())
                    .collect(Collectors.toMap(SnowflakeGrantBuilder::getKey, x -> x, (x0, x1) -> x0))
            ).join();
            Map<String, SnowflakeGrantBuilder> currentGrantBuilders = new HashMap<>();
            if (roleExists) {
                log.debug("Role exists so we are going to get the current grants.");
                currentGrantBuilders = this.snowflakeGrantsService.getGrants(
                        roleName,
                        !revokeOtherGrants || ignoreUnknownGrants);
                log.debug("Found {} existing grants.", currentGrantBuilders.size());
            } else {
                log.debug("Role does not exist. We will not look up existing roles.");
            }
            MapDifference<String, SnowflakeGrantBuilder> diff = Maps.difference(
                    currentGrantBuilders,
                    desiredGrantBuilders);
            List<SnowflakeGrantBuilder> revokeGrantBuilders = new ArrayList<>();
            if (revokeOtherGrants) {
                revokeGrantBuilders.addAll(diff.entriesOnlyOnLeft().values().stream()
                        .sorted(Comparator.comparing(SnowflakeGrantBuilder::getKey)).toList());
            }
            List<SnowflakeGrantBuilder> grantBuilders = diff.entriesOnlyOnRight().values().stream()
                    .sorted(Comparator.comparing(SnowflakeGrantBuilder::getKey)).toList();

            GrantBuilderDiff grantBuilderDiff = new GrantBuilderDiff(grantBuilders, revokeGrantBuilders);
            grantBuilderDiff = SnowflakeObjectExistsFilter.objectExistsGrantBuilderDiffFilter(
                    grantBuilderDiff,
                    snowflakeObjectsService,
                    playbookModel);
            if (consolidateWildcardsToAllGrants) {
                grantBuilderDiff = SnowflakeWildcardAllGrantFilter.consolidateWildcardAllGrantBuilders(
                        grantBuilderDiff,
                        privilegeGrants,
                        roleName);
            }
            grantBuilderDiff = SnowflakeOwnedObjectFilter.filterDiff(grantBuilderDiff, playbookModel);
            PartitionedGrantStatements partitionedGrantStatements = partitionGrantsByOwnership(grantBuilderDiff);
            log.debug("{} ownership grant changes and {} non-ownership changes planned for role {}",
                    partitionedGrantStatements.ownershipStatements().size(),
                    partitionedGrantStatements.nonOwnershipStatements().size(), roleName);
            return partitionedGrantStatements;
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format("Unable to compile role %s due to error %s", roleName, e), e);
        }
    }

    private PartitionedGrantStatements partitionGrantsByOwnership(GrantBuilderDiff grantBuilderDiff) {
        Map<Boolean, List<SnowflakeGrantBuilder>> partitionedGrantGrants = grantBuilderDiff.grant().stream()
                .collect(Collectors.partitioningBy(gb -> gb.getGrant().isOwnershipGrant()));
        Map<Boolean, List<SnowflakeGrantBuilder>> partitionedRevokeGrants = grantBuilderDiff.revoke().stream()
                .collect(Collectors.partitioningBy(gb -> gb.getGrant().isOwnershipGrant()));
        
        List<SnowflakeGrantBuilder> ownershipGrantBuilders = partitionedGrantGrants.get(true);
        List<SnowflakeGrantBuilder> nonOwnershipGrantBuilders = partitionedGrantGrants.get(false);
        List<SnowflakeGrantBuilder> ownershipRevokeBuilders = partitionedRevokeGrants.get(true);
        List<SnowflakeGrantBuilder> nonOwnershipRevokeBuilders = partitionedRevokeGrants.get(false);
        
        List<List<String>> ownershipStatements = new ArrayList<>();
        ownershipStatements.addAll(ownershipRevokeBuilders.stream()
                .map(SnowflakeGrantBuilder::getRevokeStatements)
                .toList());
        ownershipStatements.addAll(ownershipGrantBuilders.stream()
                .map(SnowflakeGrantBuilder::getGrantStatements)
                .toList());
        
        List<List<String>> nonOwnershipStatements = new ArrayList<>();
        nonOwnershipStatements.addAll(nonOwnershipRevokeBuilders.stream()
                .map(SnowflakeGrantBuilder::getRevokeStatements)
                .toList());
        nonOwnershipStatements.addAll(nonOwnershipGrantBuilders.stream()
                .map(SnowflakeGrantBuilder::getGrantStatements)
                .toList());
        
        return new PartitionedGrantStatements(ownershipStatements, nonOwnershipStatements);
    }

    public List<SnowflakeGrantBuilder> playbookGrantToSnowflakeGrants(
            PlaybookPrivilegeGrant playbookPrivilegeGrant,
            String roleName,
            SnowflakeGrantBuilderOptions options) {
        List<SnowflakeGrantModel> grants = new ArrayList<>();
        grants.addAll(standardGrants(playbookPrivilegeGrant, roleName));
        grants.addAll(containerGrants(playbookPrivilegeGrant, roleName));
        return grants.stream().map(x -> SnowflakeGrantBuilder.fromGrant(x, options))
                .filter(Objects::nonNull).toList();
    }

    public List<SnowflakeGrantModel> standardGrants(PlaybookPrivilegeGrant playbookPrivilegeGrant, String roleName) {
        try {
            if (!"*".equals(playbookPrivilegeGrant.objectName()) && !"*".equals(playbookPrivilegeGrant.schemaName())) {
                SnowflakeObjectType snowflakeObjectType
                        = SnowflakeObjectType.fromString(playbookPrivilegeGrant.objectType().toUpperCase());
                String objectName = qualifiedObjectName(
                        playbookPrivilegeGrant.databaseName(),
                        playbookPrivilegeGrant.schemaName(),
                        playbookPrivilegeGrant.objectName(),
                        snowflakeObjectType);
                if (objectName == null) {
                    log.info(String.format(
                            "Skipping grant %s for role %s due to one or more objects not found.",
                            playbookPrivilegeGrant,
                            roleName));
                    return List.of();
                }
                return playbookPrivilegeGrant.privileges().stream().map(p -> new SnowflakeGrantModel(
                        p, playbookPrivilegeGrant.objectType(),

                        objectName, "ROLE", roleName, false, false, false, false)).toList();
            } else {
                return List.of();
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(
                    String.format(
                            "Unable to generate grants for role %s grant %s",
                            roleName,
                            playbookPrivilegeGrant),
                    e);
        }
    }

    public List<SnowflakeGrantModel> containerGrants(PlaybookPrivilegeGrant playbookPrivilegeGrant, String roleName) {
        if ("*".equals(playbookPrivilegeGrant.objectName()) || "*".equals(playbookPrivilegeGrant.schemaName())) {
            if (playbookPrivilegeGrant.databaseName() == null || playbookPrivilegeGrant.databaseName().equals("*")) {
                throw new RbacDataError(String.format(
                        "Database name not provided along with wildcard for objectName in permissions for role %s.",
                        roleName));
            }
            SnowflakeObjectType containerObjectType;
            String containerName;
            if (playbookPrivilegeGrant.schemaName() == null || "*".equals(playbookPrivilegeGrant.schemaName())) {
                containerObjectType = SnowflakeObjectType.DATABASE;
                containerName = qualifiedAccountObjectName(
                        playbookPrivilegeGrant.databaseName(),
                        SnowflakeObjectType.DATABASE);
            } else {
                containerObjectType = SnowflakeObjectType.SCHEMA;
                containerName = qualifiedSchemaName(
                        playbookPrivilegeGrant.databaseName(),
                        playbookPrivilegeGrant.schemaName());
            }
            if (containerName == null) {
                log.warn(String.format(
                        "Skipping grant %s for role %s due to one or more objects not found.",
                        playbookPrivilegeGrant,
                        roleName));
                return List.of();
            }
            SnowflakeObjectType objectType = SnowflakeObjectType.fromString(playbookPrivilegeGrant.objectType()
                    .toUpperCase());
            List<SnowflakeGrantModel> snowflakeGrantModels = new ArrayList<>();
            if (playbookPrivilegeGrant.includeFuture()) {
                // Grant on future
                snowflakeGrantModels.addAll(createFutureGrants(
                        objectType,
                        containerName,
                        playbookPrivilegeGrant.privileges(),
                        roleName,
                        false));
                if (containerObjectType == SnowflakeObjectType.DATABASE && objectType.getQualLevel() > 2) {
                    snowflakeGrantModels.addAll(createFutureSchemaObjectGrantsInAllSchemasInDatabase(
                            containerName,
                            objectType,
                            playbookPrivilegeGrant.privileges(),
                            roleName,
                            false));
                }
            }
            if (playbookPrivilegeGrant.includeAll()) {
                // Grant on all
                snowflakeGrantModels.addAll(expandAllGrants(
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


    public List<SnowflakeGrantModel> createFutureSchemaObjectGrantsInAllSchemasInDatabase(
            String databaseName,
            SnowflakeObjectType objectType,
            List<String> privileges,
            String roleName,
            Boolean grantOption) {
        if (!snowflakeObjectsService.objectExists(databaseName, SnowflakeObjectType.DATABASE)) {
            log.info("Database {} does not exist. Skipping creation of future grants in schemas.", databaseName);
            return List.of();
        }
        List<String> schemas = snowflakeObjectsService.getContainerObjectQualNames(
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.SCHEMA,
                databaseName);
        return schemas.stream()
                .flatMap(x -> createFutureGrants(objectType, x, privileges, roleName, grantOption).stream()).toList();
    }

    public List<SnowflakeGrantModel> createFutureGrants(
            SnowflakeObjectType objectType,
            String containerName,
            List<String> privileges,
            String roleName,
            Boolean grantOption) {
        String objectName = String.format(
                "%s.<%s>",
                containerName,
                objectType.getObjectType().replace(" ", "_").toUpperCase());
        List<SnowflakeGrantModel> grants = privileges.stream().map(p -> new SnowflakeGrantModel(
                p,
                objectType.getObjectType().replace(" ", "_"),
                objectName,
                "ROLE",
                roleName,
                grantOption,
                true,
                false,
                false)).toList();
        return grants;
    }

    public List<SnowflakeGrantModel> expandAllGrants(
            SnowflakeObjectType containerObjectType,
            SnowflakeObjectType objectType,
            String containerName,
            List<String> privileges,
            String roleName,
            Boolean grantOption) {
        if (!snowflakeObjectsService.objectExists(containerName, containerObjectType)) {
            log.warn("{} {} does not exist. Skipping expanding grants.", containerObjectType, containerName);
            return List.of();
        }
        List<String> objects = snowflakeObjectsService.getContainerObjectQualNames(
                containerObjectType,
                objectType,
                containerName);
        List<SnowflakeGrantModel> results = new ArrayList<>();
        objects.forEach(objectName -> privileges.forEach(privilege -> results.add(new SnowflakeGrantModel(
                privilege,
                objectType.getObjectType().replace(" ", "_"),
                objectName,
                "role",
                roleName,
                grantOption,
                false,
                false,
                false))));
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SnowflakeProvider that))
            return false;

        if (!Objects.equals(snowflakeGrantsService, that.snowflakeGrantsService))
            return false;
        if (!Objects.equals(snowflakeStatementsService, that.snowflakeStatementsService))
            return false;
        return Objects.equals(snowflakeObjectsService, that.snowflakeObjectsService);
    }

    @Override
    public int hashCode() {
        int result = snowflakeGrantsService != null ? snowflakeGrantsService.hashCode() : 0;
        result = 31 * result + (snowflakeStatementsService != null ? snowflakeStatementsService.hashCode() : 0);
        result = 31 * result + (snowflakeObjectsService != null ? snowflakeObjectsService.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SnowflakeProvider{" + "snowflakeGrantsService=" + snowflakeGrantsService + ", snowflakeStatementsService=" + snowflakeStatementsService + ", snowflakeObjectsService=" + snowflakeObjectsService + '}';
    }
}
