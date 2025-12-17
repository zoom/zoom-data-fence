package us.zoom.data.dfence.providers.snowflake;

import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.ObjectNameException;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.providers.snowflake.grant.builder.GrantBuilderDiff;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.List;

@Slf4j
public class SnowflakeObjectExistsFilter {
    public static GrantBuilderDiff objectExistsGrantBuilderDiffFilter(
            GrantBuilderDiff grantBuilderDiff,
            SnowflakeObjectsService snowflakeObjectsService,
            PlaybookModel playbookModel) {
        List<String> playbookRoleNames = playbookModel.roles().values().stream()
                .map(x -> ObjectName.normalizeObjectName(x.name())).toList();
        return new GrantBuilderDiff(
                grantBuilderDiff.grant().stream()
                        .filter(grantBuilder -> objectExistsGrantFilter(
                                grantBuilder.getGrant(),
                                snowflakeObjectsService,
                                playbookRoleNames)).toList(),
                grantBuilderDiff.revoke().stream()
                        .filter(grantBuilder -> objectExistsGrantFilter(
                                grantBuilder.getGrant(),
                                snowflakeObjectsService,
                                playbookRoleNames)).toList());
    }

    public static Boolean objectExistsGrantFilter(
            SnowflakeGrantModel snowflakeGrantModel,
            SnowflakeObjectsService snowflakeObjectsService,
            List<String> playbookRoleNames) {
        if (
                snowflakeGrantModel.grantedOn().equals("ROLE") && playbookRoleNames.contains(ObjectName.normalizeObjectName(
                snowflakeGrantModel.name()))) {
            return true;
        }
        if (snowflakeGrantModel.grantedOn().equals("APPLICATION_ROLE")) {
            return true;
        }
        String objectName;
        SnowflakeObjectType snowflakeObjectType;
        if (snowflakeGrantModel.all() || snowflakeGrantModel.future()) {
            objectName = ObjectName.containerName(snowflakeGrantModel.name());
            List<String> objectNameParts = ObjectName.splitObjectName(objectName);
            switch (objectNameParts.size()) {
                case (1) -> {
                    snowflakeObjectType = SnowflakeObjectType.DATABASE;
                }
                case (2) -> {
                    snowflakeObjectType = SnowflakeObjectType.SCHEMA;
                }
                default -> {
                    throw new RbacDataError(String.format(
                            "Invalid object name %s for future or all grant.",
                            snowflakeGrantModel.name()));
                }
            }
        } else {
            objectName = ObjectName.normalizeObjectName(snowflakeGrantModel.name());
            snowflakeObjectType = SnowflakeObjectType.fromString(snowflakeGrantModel.grantedOn());
        }
        Boolean ret;
        try {
            ret = snowflakeObjectsService.objectExists(objectName, snowflakeObjectType);
        } catch (ObjectNameException e) {
            throw new ObjectNameException(
                    String.format("Unable to check if %s %s exists due to object name exception.", snowflakeObjectType, objectName)
                    , e);
        }
        if (!ret) {
            log.info("{} {} not found. Skipping.", snowflakeObjectType.toString().toLowerCase(), objectName);
        }
        return ret;
    }
}
