package us.zoom.data.dfence.providers.snowflake.grant.builder;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

/**
 * Snowflake object types that can be granted on (databases, schemas, tables, agents, etc.).
 * <p>
 * Each type has a qualification level ({@link #getQualLevel()}) indicating how many name parts
 * it has: 0 = account-level, 1 = one part (e.g. database name), 2 = two parts (e.g. db.schema),
 * 3 = three parts (e.g. db.schema.object). The {@link #getObjectType()} and {@link
 * #getObjectTypePlural()} strings are used in SQL (e.g. "SHOW AGENTS IN DATABASE"). Use {@link
 * #getGrantNameObjectType()} when building grant names so desired state matches what Snowflake
 * returns.
 * <p>
 * {@link #getAliasFor()} is used only when building hash keys for grants (e.g. in {@code
 * SnowflakeGrantBuilder.getKey()} and the revoke index). It is not user-facing and does not
 * allow the user to specify a different name in the playbook. It exists to align on differences
 * that come from Snowflake: the same logical object type may appear under different names in
 * different Snowflake APIs (e.g. MATERIALIZED_VIEW in one place, VIEW in another). By having
 * multiple enum values return the same alias, they are treated as the same when calculating
 * which grants to add or remove. For playbook input names (e.g. "AGENT" instead of
 * "CORTEX_AGENT"), see {@link #overrideObjectTypes}.
 *
 * @see #fromString(String) for resolving playbook or Snowflake strings to this enum
 * @see #overrideObjectTypes for playbook input mapping (e.g. AGENT → CORTEX_AGENT)
 */
public enum SnowflakeObjectType {
    ACCOUNT(0, null),
    ALERT(3, null),
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
    MODEL(3, null),
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


    /** Number of name parts (0=account, 1=e.g. db, 2=e.g. db.schema, 3=e.g. db.schema.object). */
    @Getter
    private final Integer qualLevel;

    /** Singular form used in SQL (e.g. "AGENT" for SHOW AGENTS; "TABLE" for TABLE). */
    @Getter
    private final String objectType;

    /** Plural form used in SQL (e.g. "AGENTS", "TABLES"). */
    @Getter
    private final String objectTypePlural;

    /**
     * When non-null, used only in hash keys so that this type and another (e.g. VIEW) are
     * treated as the same when computing grants to add or remove. Aligns Snowflake variations
     * (e.g. MATERIALIZED_VIEW may appear as VIEW in some output). Not user-facing; see {@link
     * #overrideObjectTypes} for playbook input names.
     */
    @Getter
    private final String aliasFor;

    /**
     * Main constructor with all parameters.
     *
     * @param qualLevel       number of name parts (0=account, 1=e.g. db, 2=e.g. db.schema,
     *                        3=e.g. db.schema.object)
     * @param aliasFor        when non-null, value used in hash keys so this type matches another
     *                        (e.g. MATERIALIZED_VIEW uses "VIEW"); null if no alias
     * @param objectType      singular form for SQL (e.g. "AGENT", "TABLE"); null to infer from
     *                        enum name (underscores to spaces)
     * @param objectTypePlural plural form for SQL (e.g. "AGENTS", "TABLES"); null to infer from
     *                         objectType
     */
    SnowflakeObjectType(Integer qualLevel, String aliasFor, String objectType, String objectTypePlural) {
        this.qualLevel = qualLevel;
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
        this.aliasFor = Objects.requireNonNullElseGet(aliasFor, this::name);
    }

    /**
     * Constructor that infers objectType and objectTypePlural from enum name (e.g. TABLE → "TABLE",
     * "TABLES").
     *
     * @param qualLevel number of name parts (0=account, 1=e.g. db, 2=e.g. db.schema,
     *                 3=e.g. db.schema.object)
     * @param aliasFor when non-null, value used in hash keys so this type matches another; null
     *                 if no alias
     */
    SnowflakeObjectType(Integer qualLevel, String aliasFor) {
        this(qualLevel, aliasFor, null, null);
    }

    /**
     * Resolves a string (from playbook, Snowflake grant output, etc.) to this enum.
     * Applies {@link #overrideObjectTypes} so that e.g. "AGENT" maps to {@link #CORTEX_AGENT}.
     *
     * @param objectType the object type string (case-insensitive)
     * @return the corresponding SnowflakeObjectType
     * @throws IllegalArgumentException if the string does not match any enum and is not in the
     *         override map
     */
    public static SnowflakeObjectType fromString(String objectType) {
        String normalizedObjectType = objectType.toUpperCase();
        String overrideValue = overrideObjectTypes.get(normalizedObjectType);
        return SnowflakeObjectType.valueOf(Objects.requireNonNullElse(overrideValue, normalizedObjectType));
    }

    /**
     * Map of playbook (or Snowflake) input names to enum names. Used by {@link #fromString(String)}
     * so that e.g. "AGENT" in a playbook resolves to {@link #CORTEX_AGENT}. This is what allows
     * the user to specify a different name in the playbook; it is separate from {@link
     * #getAliasFor()}, which is only for hash keys.
     */
    public static Map<String, String> overrideObjectTypes = Map.of(
            "AGENT", "CORTEX_AGENT"
    );
}
