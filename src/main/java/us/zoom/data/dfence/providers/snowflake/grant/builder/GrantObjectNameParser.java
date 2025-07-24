package us.zoom.data.dfence.providers.snowflake.grant.builder;

import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.models.ContainerGrantParts;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.List;
import java.util.regex.Pattern;

public class GrantObjectNameParser {

    private static final Pattern objectPartPattern = Pattern.compile("^<[A-Z0-9_]+>$");

    public static ContainerGrantParts futureGrantObjectNameParts(String objectName) {
        List<String> parts = ObjectName.splitObjectName(objectName);
        List<String> shortenedObjectNameParts = parts.subList(0, parts.size() - 1).stream().toList();
        String shortenedObjectName = String.join(
                ".",
                shortenedObjectNameParts.stream().map(ObjectName::quotedObjectNamePart).toList());
        String objectPart = parts.get(parts.size() - 1);
        SnowflakeObjectType containerObjectTypePlural;
        if (parts.size() == 3) {
            containerObjectTypePlural = SnowflakeObjectType.SCHEMA;
        } else if (parts.size() == 2) {
            containerObjectTypePlural = SnowflakeObjectType.DATABASE;
        } else {
            throw new RbacDataError(String.format(
                    "Invalid number of parts, %s, for objectName %s.",
                    parts.size(),
                    objectName));
        }
        if (!objectPartPattern.matcher(objectPart).find()) {
            throw new RbacDataError(String.format(
                    "objectPart %s is does not match the pattern %s",
                    objectPart,
                    objectPartPattern.pattern()));
        }
        String objectTypeString = objectPart.substring(1, objectPart.length() - 1);
        SnowflakeObjectType objectType = SnowflakeObjectType.valueOf(objectTypeString);
        return new ContainerGrantParts(containerObjectTypePlural, shortenedObjectName, objectType);
    }
}
