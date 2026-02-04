package us.zoom.data.dfence.providers.snowflake.informationschema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.SnowflakeConnectionService;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class SnowflakeObjectsService {

    private final Map<String, List<String>> getContainerObjectQualNamesCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> objectExistsCache = new ConcurrentHashMap<>();

    private final SnowflakeDefaultObjectService snowflakeDefaultObjectService;

    private final SnowflakeTableObjectService snowflakeTableObjectService;

    private final SnowflakeStageObjectsService snowflakeStageObjectsService;

    private final SnowflakeProcedureObjectService snowflakeProcedureObjectService;

    private final SnowflakeFunctionObjectService snowflakeFunctionObjectService;

    private final SnowflakeVolumeService snowflakeVolumeService;

    public SnowflakeObjectsService(
            SnowflakeConnectionService snowflakeConnectionService) {
        this.snowflakeDefaultObjectService = new SnowflakeDefaultObjectService(snowflakeConnectionService);
        this.snowflakeTableObjectService = new SnowflakeTableObjectService(snowflakeConnectionService);
        this.snowflakeStageObjectsService = new SnowflakeStageObjectsService(snowflakeConnectionService);
        this.snowflakeProcedureObjectService = new SnowflakeProcedureObjectService(snowflakeConnectionService);
        this.snowflakeFunctionObjectService = new SnowflakeFunctionObjectService(snowflakeConnectionService);
        this.snowflakeVolumeService = new SnowflakeVolumeService(snowflakeConnectionService);
    }

    public List<String> getContainerObjectQualNames(
            SnowflakeObjectType containerObjectType,
            SnowflakeObjectType objectType,
            String containerName) {
        String key = String.join(
                "::",
                List.of(containerObjectType.getSqlQueryObjectType(), objectType.getSqlQueryObjectType(), containerName));
        if (getContainerObjectQualNamesCache.containsKey(key)) {
            return getContainerObjectQualNamesCache.get(key);
        } else {
            List<String> result = getContainerObjectQualNamesRaw(containerObjectType, objectType, containerName);
            getContainerObjectQualNamesCache.put(key, result);
            return result;
        }
    }

    public void clearCache() {
        log.debug("Clearing cache.");
        this.getContainerObjectQualNamesCache.clear();
        this.objectExistsCache.clear();
    }

    /*
    Cached version of function. This uses a function that is also cached that does the I/O. However, we still
    cache again here because this function is used so much and we want to avoid repeated lookups through loops of loops.
    However, this cache will be pretty big since it includes all of the objects we are managing so it will be saving
    some speed but using some memory.
     */
    public Boolean objectExists(String objectName, SnowflakeObjectType objectType) {
        String key = String.join("::", List.of(objectName, objectType.toString()));
        if (objectExistsCache.containsKey(key)) {
            return objectExistsCache.get(key);
        } else {
            Boolean result = objectExistsRaw(objectName, objectType);
            objectExistsCache.put(key, result);
            return result;
        }
    }

    /*
    Uncached version of function to determine if an object exists.
     */
    public Boolean objectExistsRaw(String objectName, SnowflakeObjectType objectType) {
        if (objectType == SnowflakeObjectType.ACCOUNT) {
            return true;
        }
        String normalizedObjectName = ObjectName.quotedObjectName(objectName);
        String containerName = ObjectName.containerName(normalizedObjectName);
        SnowflakeObjectType containerObjectType;
        switch (objectType.getQualLevel()) {
            case 0 -> {
                // The object is the account. It has to exist if we are talking to it.
                return true;
            }
            case 1 -> {
                // Object type is database, role etc. It's parent is account.
                containerObjectType = SnowflakeObjectType.ACCOUNT;
            }
            case 2 -> {
                // Object type can only be schema. It's parent can only be database.
                containerObjectType = SnowflakeObjectType.DATABASE;
            }
            case 3 -> {
                // Object type is a table, view, etc. inside of a schema.
                containerObjectType = SnowflakeObjectType.SCHEMA;
            }
            default -> throw new RuntimeException(String.format(
                    "Invalid qualification level of %s.",
                    objectType.getQualLevel()));
        }
        if (containerObjectType.getQualLevel() > 0) {
            // Check if the parent exists first.
            if (!objectExists(containerName, containerObjectType)) {
                return false;
            }
        }
        List<String> containerObjectQualNames = getContainerObjectQualNames(
                containerObjectType,
                objectType,
                containerName);
        for (String name : containerObjectQualNames) {
            if (ObjectName.equalObjectName(name, objectName)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getContainerObjectQualNamesRaw(
            @NotNull SnowflakeObjectType containerObjectType,
            @NotNull SnowflakeObjectType objectType,
            @NotEmpty String containerName) {
        log.info(
                "Finding existing {} in {} {}.",
                objectType.getSqlQueryObjectTypePlural(),
                containerObjectType.getSqlQueryObjectType(),
                containerName);
        switch (objectType) {
            case TABLE, VIEW, MATERIALIZED_VIEW, EXTERNAL_TABLE, EVENT_TABLE -> {
                return snowflakeTableObjectService.getContainerTables(containerName, containerObjectType, objectType);
            }
            case VOLUME -> {
                return snowflakeVolumeService.getExternalVolumesInAccount();
            }
            // We will only consider external stages for now as a patch because internal and external stages allow different types of grants.
            case STAGE -> {
                return snowflakeStageObjectsService.getContainerExternalStages(containerName, containerObjectType);
            }
            case PROCEDURE -> {
                return snowflakeProcedureObjectService.getContainerProcedures(containerName, containerObjectType);
            }
            case FUNCTION -> {
                return snowflakeFunctionObjectService.getContainerProcedures(containerName, containerObjectType);
            }
            default -> {
                return snowflakeDefaultObjectService.getContainerObjectQualNamesDefault(
                        containerObjectType,
                        objectType,
                        containerName);
            }
        }
    }
}
