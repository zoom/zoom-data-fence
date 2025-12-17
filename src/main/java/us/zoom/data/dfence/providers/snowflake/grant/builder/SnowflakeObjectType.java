package us.zoom.data.dfence.providers.snowflake.grant.builder;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

public enum SnowflakeObjectType {
    ACCOUNT(0, null),
    ALERT(3, null),
    APPLICATION_ROLE(1, null),
    CLASS(1, null),
    CORTEX_AGENT(3, null, "AGENT", "AGENTS"),
    DATABASE(1, null),
    DATABASE_ROLE(2, null),
    DIRECTORY_TABLE(3, null),
    EVENT_TABLE(3, null),
    EXTERNAL_TABLE(3, "TABLE"),
    FILE_FORMAT(3, null),
    FUNCTION(3, null),
    ICEBERG_TABLE(3, "TABLE"),
    INTEGRATION(1, null),
    INSTANCE(3, null),
    MASKING_POLICY(3, null),
    MATERIALIZED_VIEW(3, "VIEW"),
    NETWORK_POLICY(1, null),
    NETWORK_RULE(1, null),
    NOTEBOOK(3, null),
    PASSWORD_POLICY(3, null),
    PIPE(3, null),
    PROCEDURE(3, null),
    RESOURCE_MONITOR(1, null),
    ROLE(1, null),
    ROW_ACCESS_POLICY(3, null),
    SCHEMA(2, null),
    SECRET(3, null),
    SEQUENCE(3, null),
    SESSION_POLICY(3, null),
    SEMANTIC_VIEW(3, null),
    STAGE(3, null),
    STREAM(3, null),
    STREAMLIT(3, null),
    TABLE(3, null),
    TAG(3, null),
    TASK(3, null),
    USER(1, null),
    VIEW(3, null),
    VOLUME(1, null),
    WAREHOUSE(1, null),
    COMPUTE_POOL(1, null),
    IMAGE_REPOSITORY(3, null);


    @Getter
    private final Integer qualLevel;

    // objectType is used for SQL statements about the object.
    @Getter
    private final String objectType;

    // objectTypePlural is used for SQL statements about multiple objects.
    @Getter
    private final String objectTypePlural;

    // Alias for is used for considering grants on different object types equivalent for the sake of hashing and comparison.
    private final String aliasFor;

    // Main constructor with all parameters - contains the actual logic
    SnowflakeObjectType(Integer qualLevel, String aliasFor, String objectType, String objectTypePlural) {
        this.qualLevel = qualLevel;
        this.aliasFor = aliasFor;
        // If objectType is provided, use it; otherwise infer from enum name
        String computedObjectType = (objectType != null) ? objectType : this.name().replace("_", " ");
        this.objectType = computedObjectType;
        // If objectTypePlural is provided, use it; otherwise infer from objectType
        if (objectTypePlural != null) {
            this.objectTypePlural = objectTypePlural;
        } else {
            // Hooked on phonics works for me.
            if (computedObjectType.endsWith("Y")) {
                this.objectTypePlural = computedObjectType.substring(0, computedObjectType.length() - 1) + "IES";
            } else {
                this.objectTypePlural = computedObjectType + "S";
            }
        }
    }

    // Constructor that infers objectType and objectTypePlural from enum name
    SnowflakeObjectType(Integer qualLevel, String aliasFor) {
        this(qualLevel, aliasFor, null, null);
    }

    public String getAliasFor() {
        if (this.aliasFor == null) {
            return this.name();
        }
        return this.aliasFor;
    }

    public static SnowflakeObjectType fromString(String objectType) {
        String normalizedObjectType = objectType.toUpperCase();
        String overrideValue = overrideObjectTypes.get(normalizedObjectType);
        return SnowflakeObjectType.valueOf(Objects.requireNonNullElse(overrideValue, normalizedObjectType));
    }

    public static Map<String, String> overrideObjectTypes = Map.of(
            "AGENT", "CORTEX_AGENT"
    );
}
