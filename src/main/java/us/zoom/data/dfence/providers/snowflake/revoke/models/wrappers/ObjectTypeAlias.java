package us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record ObjectTypeAlias(String value) {
  public static ObjectTypeAlias apply(SnowflakeObjectType snowflakeObjectType) {
    return new ObjectTypeAlias(snowflakeObjectType.getAliasFor());
  }
}
