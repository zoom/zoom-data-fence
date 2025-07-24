package us.zoom.security.dfence.providers.snowflake;

import lombok.Data;
import us.zoom.security.dfence.exception.ObjectNameException;
import us.zoom.security.dfence.exception.RbacDataError;
import us.zoom.security.dfence.playbook.model.PlaybookModel;
import us.zoom.security.dfence.providers.snowflake.grant.builder.GrantBuilderDiff;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.security.dfence.sql.ObjectName;

import java.util.List;

/*
Filter class used for filtering revoke ownership grants out for revokes on the same object or objects within the same container.
 */
@Data
public class SnowflakeOwnedObjectFilter {
    private final String databaseName;
    private final String schemaName;
    private final String objectName;
    private final SnowflakeObjectType snowflakeObjectType;
    private final String roleName;

    public SnowflakeOwnedObjectFilter(
            String databaseName,
            String schemaName,
            String objectName,
            SnowflakeObjectType snowflakeObjectType,
            String roleName) {
        this.databaseName = databaseName == null ? null : ObjectName.normalizeObjectNamePart(databaseName);
        this.schemaName = schemaName == null || "*".equals(schemaName) ? schemaName :
                ObjectName.normalizeObjectNamePart(schemaName);
        this.objectName = objectName == null || "*".equals(objectName) ? objectName :
                ObjectName.normalizeObjectNamePart(objectName);
        this.snowflakeObjectType = snowflakeObjectType;
        this.roleName = ObjectName.normalizeObjectNamePart(roleName);
    }

    public static List<SnowflakeOwnedObjectFilter> filtersFromPlaybookModel(PlaybookModel playbook) {
        return playbook.roles().values().stream().flatMap(role -> role.grants().stream()
                .filter(grant -> grant.privileges().stream().anyMatch("OWNERSHIP"::equalsIgnoreCase))
                .map(grant -> new SnowflakeOwnedObjectFilter(
                        grant.databaseName(),
                        grant.schemaName(),
                        grant.objectName(),
                        SnowflakeObjectType.valueOf(grant.objectType().toUpperCase()),
                        role.name()))).toList();
    }

    public static GrantBuilderDiff filterDiff(GrantBuilderDiff grantBuilderDiff, PlaybookModel playbookModel) {
        List<SnowflakeOwnedObjectFilter> filters = SnowflakeOwnedObjectFilter.filtersFromPlaybookModel(playbookModel);
        List<SnowflakeGrantBuilder> revokeGrants = grantBuilderDiff.revoke().stream().filter(gb -> {
            Boolean k = filters.stream().allMatch(f -> {
                Boolean y = f.keep(gb.getGrant());
                return y;
            });
            return k;
        }).toList();
        return new GrantBuilderDiff(grantBuilderDiff.grant(), revokeGrants);
    }


    Boolean keep(SnowflakeGrantModel snowflakeGrantModel) {
        // We do this in separate steps rather than one big boolean in order to return early before instantiating anything else.
        if (!"OWNERSHIP".equals(snowflakeGrantModel.privilege()) || ObjectName.equalObjectName(
                snowflakeGrantModel.granteeName(),
                roleName) || snowflakeGrantModel.future() || snowflakeGrantModel.all()) {
            return true;
        }
        SnowflakeObjectType grantObjectType = SnowflakeObjectType.valueOf(snowflakeGrantModel.grantedOn());
        if (!grantObjectType.equals(snowflakeObjectType)) {
            return true;
        }

        // Now we assume that this is an ownership grant to some other role.
        List<String> grantObjectNameParts;
        try {
            grantObjectNameParts = ObjectName.splitObjectName(snowflakeGrantModel.name());
        } catch (ObjectNameException e) {
            throw new ObjectNameException(
                    String.format(
                            "Unable to process snowflake grant model %s due to object name exception for name %s",
                            snowflakeGrantModel,
                            snowflakeGrantModel.name()), e);
        }
        switch (grantObjectType.getQualLevel()) {
            case 1 -> {
                if (!(grantObjectNameParts.size() == 1)) {
                    throw new RbacDataError(String.format(
                            "Invalid object name %s for object type %s",
                            snowflakeGrantModel.granteeName(),
                            grantObjectType));
                }
                if (grantObjectType.equals(SnowflakeObjectType.DATABASE)) {
                    return !ObjectName.equalObjectName(databaseName, grantObjectNameParts.get(0));
                }
                return !ObjectName.equalObjectName(objectName, grantObjectNameParts.get(0));
            }
            case 2 -> {
                if (!(grantObjectNameParts.size() == 2)) {
                    throw new RbacDataError(String.format(
                            "Invalid object name %s for object type %s",
                            snowflakeGrantModel.granteeName(),
                            grantObjectType));
                }
                return !(ObjectName.equalObjectName(
                        databaseName,
                        grantObjectNameParts.get(0)) && ("*".equals(schemaName) || ObjectName.equalObjectName(
                        schemaName,
                        grantObjectNameParts.get(1))

                ));
            }
            case 3 -> {
                // We don't have to worry about future grants with a qual level of three and two grant parts because we are already ignoring those.
                if (!(grantObjectNameParts.size() == 3)) {
                    throw new RbacDataError(String.format(
                            "Invalid object name %s for object type %s",
                            snowflakeGrantModel.granteeName(),
                            grantObjectType));
                }
                return !(ObjectName.equalObjectName(databaseName, grantObjectNameParts.get(0)) && (("*".equals(
                        objectName) && "*".equals(schemaName)) || ("*".equals(objectName) && ObjectName.equalObjectName(schemaName,
                        grantObjectNameParts.get(1))) || (ObjectName.equalObjectName(
                        schemaName,
                        grantObjectNameParts.get(1)) && ObjectName.equalObjectName(
                        objectName,
                        grantObjectNameParts.get(2))

                )

                ));
            }
            default -> {
                throw new RbacDataError(String.format(
                        "Invalid qualifaction level of %s for ownership grant.",
                        grantObjectType.getQualLevel()));
            }
        }

    }
}
