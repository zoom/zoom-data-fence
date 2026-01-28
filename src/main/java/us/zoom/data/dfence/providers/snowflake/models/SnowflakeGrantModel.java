package us.zoom.data.dfence.providers.snowflake.models;

import io.vavr.collection.List;
import us.zoom.data.dfence.exception.ObjectNameException;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SnowflakeGrantModel(
        String privilege,
        String grantedOn,
        String name,
        String grantedTo,
        String granteeName,
        Boolean grantOption,
        Boolean future,
        Boolean all) {
    public SnowflakeGrantModel {
        if (!name.isEmpty()) {
            // Patch for user name not quoted in Snowflake. This looks like a bug in Snowflake where they handle the
            // user name totally different than the way they handle other objects.
            if (grantedOn.equalsIgnoreCase("USER") && !name.startsWith("\"")) {
                name = "\"" + name + "\"";
            }
            try {
                name = ObjectName.normalizeObjectName(name);
            } catch (ObjectNameException e) {
                throw new ObjectNameException(String.format("Unable to normalize name %s for grant.", name), e);
            }
        }
        privilege = privilege.toUpperCase();
        grantedOn = grantedOn.toUpperCase();
        grantedTo = grantedTo.toUpperCase();
        if (!granteeName.isEmpty()) {
            try {
                granteeName = ObjectName.normalizeObjectName(granteeName);
            } catch (ObjectNameException e) {
                throw new ObjectNameException(
                        String.format(
                                "Unable to normalize grantee name %s for grant.",
                                granteeName), e);
            }
        }

        name = normalizeFutureGrantName(name, grantedOn, future);
    }

    public Boolean isOwnershipGrant() {
        return "OWNERSHIP".equalsIgnoreCase(privilege.trim());
    }

    public String getEscapedName() {
        try {
            return ObjectName.quotedObjectName(name);
        } catch (ObjectNameException e) {
            throw new ObjectNameException(String.format("Unable to generate escaped name for grant %s", this, e));
        }
    }

    /**
     * Normalizes the object-name part of a future grant so current and desired keys match.
     * For example for cortex agents, SHOW FUTURE GRANTS returns {@code <CORTEX_AGENT>}.
     * But if the future SnowflakeGrantModel is built in code with <AGENT>, then equality check fails
     * even though both grants are same during actual grants creation during Maps.difference.
     *
     * This method rewrites the last segment to {@code <SnowflakeObjectType.name()>} so both sides align
     * and no spurious drift is reported.
     */
    public static String normalizeFutureGrantName(String name, String grantedOn, boolean isFuture) {
        if (!isFuture || name == null || name.isEmpty() ) {
            return name;
        }
        List<String> parts = List.ofAll(ObjectName.splitObjectName(name));
        List<String> allPartsExceptLast = parts.dropRight(1);
        SnowflakeObjectType objectType = SnowflakeObjectType.fromString(grantedOn);
        String newLast = String.format("<%s>", objectType.name());
        return allPartsExceptLast.append(newLast).mkString(".");
    }
}
