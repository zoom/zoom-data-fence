package us.zoom.data.dfence.providers.snowflake;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.GrantBuilderDiff;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Slf4j
public class SnowflakeWildcardAllGrantFilter {

    public static String allGrantHash(SnowflakeGrantModel grant) {
        return String.join(
                "::", List.of(
                        grant.grantedOn(),
                        grant.granteeName(),
                        grant.privilege(),
                        ObjectName.containerName(grant.name()),
                        grant.grantOption().toString()));
    }


    /*
    Generate special all grants that are not individually part of the difference. These are not true grants because
    we cannot query them in snowflake. However, we can grant them with
    'grant <privilege> on all <object type> in <container name>;' This results in a
    collection of grants that we have to aggregate.
     */
    public static List<SnowflakeGrantBuilder> containerAllGrantBuilders(
            PlaybookPrivilegeGrant playbookPrivilegeGrant,
            String roleName) {
        SnowflakeObjectType objectType = SnowflakeObjectType.fromString(playbookPrivilegeGrant.objectType().toUpperCase());
        if ("*".equals(playbookPrivilegeGrant.databaseName())) {
            throw new RbacDataError("Database name may not be a wildcard.");
        }

        if ("*".equals(playbookPrivilegeGrant.objectName()) || "*".equals(playbookPrivilegeGrant.schemaName())) {
            if (playbookPrivilegeGrant.databaseName() == null || playbookPrivilegeGrant.databaseName().equals("*")) {
                throw new RbacDataError(String.format(
                        "Database name not provided along with wildcard for objectName in permissions for role %s.",
                        roleName));
            }
            String containerName;
            if (playbookPrivilegeGrant.schemaName() == null || "*".equals(playbookPrivilegeGrant.schemaName())) {
                containerName = ObjectName.normalizeObjectName(playbookPrivilegeGrant.databaseName());
            } else {
                containerName = ObjectName.normalizeObjectName(String.format(
                        "%s.%s",
                        playbookPrivilegeGrant.databaseName(),
                        playbookPrivilegeGrant.schemaName()));
            }
            String objectName = String.format("%s.<%s>", containerName, objectType.getObjectType().replace(" ", "_"));
            List<SnowflakeGrantModel> grants = playbookPrivilegeGrant.privileges().stream()
                    .map(p -> new SnowflakeGrantModel(
                            p,
                            objectType.getObjectType().replace(" ", "_"),
                            objectName,
                            "ROLE",
                            roleName,
                            false,
                            false,
                            true,
                            false)).toList();
            return grants.stream().map(SnowflakeGrantBuilder::fromGrant).toList();
        } else {
            return List.of();
        }
    }

    public static GrantBuilderDiff consolidateWildcardAllGrantBuilders(
            GrantBuilderDiff grantBuilderDiff,
            List<PlaybookPrivilegeGrant> playbookPrivilegeGrants,
            String roleName) {
        List<SnowflakeGrantBuilder> consolidatedGrants = new ArrayList<>();
        List<SnowflakeGrantBuilder> consolidatedRevokeGrants = new ArrayList<>();
        List<SnowflakeGrantBuilder> allGrants = playbookPrivilegeGrants.stream()
                .flatMap(x -> containerAllGrantBuilders(x, roleName).stream()).toList();
        Set<String> excludeContainerNames = allGrants.stream().map(x -> allGrantHash(x.getGrant()))
                .collect(Collectors.toSet());
        Set<String> includeContainerNamesGrants = grantBuilderDiff.grant().stream().map(x -> allGrantHash(x.getGrant()))
                .collect(Collectors.toSet());
        consolidatedGrants.addAll(grantBuilderDiff.grant().stream()
                .filter(x -> x.getGrant().future() || !excludeContainerNames.contains(allGrantHash(x.getGrant())))
                .toList());
        consolidatedRevokeGrants.addAll(grantBuilderDiff.revoke().stream()
                .filter(x -> x.getGrant().future() || !excludeContainerNames.contains(allGrantHash(x.getGrant())))
                .toList());
        // Only add the grants. We do not revoke all. There should be no all grants to revoke. Revokes will be
        // individual in other containers.
        log.debug("Adding {} all grants {}", allGrants.size());
        consolidatedGrants.addAll(allGrants.stream()
                .filter(x -> includeContainerNamesGrants.contains(allGrantHash(x.getGrant()))).toList());
        return new GrantBuilderDiff(consolidatedGrants, consolidatedRevokeGrants);
    }
}
