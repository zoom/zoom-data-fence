package us.zoom.data.dfence.providers.snowflake;

import lombok.extern.slf4j.Slf4j;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import us.zoom.data.dfence.CompiledChanges;
import us.zoom.data.dfence.Provider;
import us.zoom.data.dfence.consistency.GrantRevokeConsistencyChecker;
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
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.DesiredGrantsCompiler;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.PartitionedGrantStatements;
import us.zoom.data.dfence.providers.snowflake.revoke.SnowflakeRevokeGrantsCompiler;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SnowflakeProvider implements Provider {

    private final SnowflakeGrantsService snowflakeGrantsService;

    private final SnowflakeStatementsService snowflakeStatementsService;

    private final SnowflakeObjectsService snowflakeObjectsService;

    private final DesiredGrantsCompiler desiredGrantsCompiler;

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
        this.desiredGrantsCompiler = new DesiredGrantsCompiler(snowflakeObjectsService);
        this.forkJoinPool = forkJoinPool;
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

    public PartitionedGrantStatements compilePlaybookPrivilegeGrants(
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
                    .flatMap(x -> desiredGrantsCompiler.compileGrants(x, roleName, options).stream())
                    .collect(Collectors.toMap(SnowflakeGrantBuilder::getKey, x -> x, (x0, x1) -> x0))
            ).join();
            try {
                GrantRevokeConsistencyChecker.check(
                        privilegeGrants,
                        desiredGrantBuilders,
                        roleName);
            } catch (RbacDataError e) {
                throw new RbacDataError(
                        String.format(
                                "Grant-Revoke consistency check failed for role %s. "
                                        + "Application aborted to prevent data inconsistency. "
                                        + "Please fix the matching logic divergence before proceeding. "
                                        + "%s",
                                roleName,
                                e.getMessage()),
                        e);
            }
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
                // Use SnowflakeRevokeGrantsCompiler to identify grants that should be revoked.
                // This compares current grants against playbook privilege grants to find grants
                // that are not allowed by the playbook.
                List<SnowflakeGrantBuilder> grantsToRevoke = SnowflakeRevokeGrantsCompiler
                        .compileRevokeGrants(privilegeGrants, currentGrantBuilders).stream().toList();
                revokeGrantBuilders.addAll(grantsToRevoke);
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
