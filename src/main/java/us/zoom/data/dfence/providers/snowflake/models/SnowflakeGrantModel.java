package us.zoom.data.dfence.providers.snowflake.models;

import us.zoom.data.dfence.exception.ObjectNameException;
import us.zoom.data.dfence.sql.ObjectName;

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
    }

    public Boolean isOwnershipGrant() {
        return computeIsOwnershipGrant(privilege);
    }

    private static Boolean computeIsOwnershipGrant(String privilege) {
        return "OWNERSHIP".equalsIgnoreCase(privilege.trim());
    }

    public String getEscapedName() {
        try {
            return ObjectName.quotedObjectName(name);
        } catch (ObjectNameException e) {
            throw new ObjectNameException(String.format("Unable to generate escaped name for grant %s", this, e));
        }
    }
}
