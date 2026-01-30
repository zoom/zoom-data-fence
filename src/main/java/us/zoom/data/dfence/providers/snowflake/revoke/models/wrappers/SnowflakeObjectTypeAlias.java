package us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record SnowflakeObjectTypeAlias(String value) {
  public static SnowflakeObjectTypeAlias of(SnowflakeObjectType snowflakeObjectType) {
    return new SnowflakeObjectTypeAlias(snowflakeObjectType.getAliasFor());
  }
}
