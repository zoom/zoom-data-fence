package us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers;

public record SnowflakeGrantObjectName(String value) {
  public static SnowflakeGrantObjectName of(String value) {
    return new SnowflakeGrantObjectName(value.trim());
  }
}
