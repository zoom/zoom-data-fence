package us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record ObjectType(String value) {
  public static ObjectType apply(SnowflakeObjectType snowflakeObjectType) {
    return new ObjectType(snowflakeObjectType.getObjectType());
  }
}
