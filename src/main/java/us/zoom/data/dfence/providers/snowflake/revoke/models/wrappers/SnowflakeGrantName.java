package us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers;

public record SnowflakeGrantName(String value) {
  public static SnowflakeGrantName apply(String value) {
    return new SnowflakeGrantName(value.trim());
  }
}
