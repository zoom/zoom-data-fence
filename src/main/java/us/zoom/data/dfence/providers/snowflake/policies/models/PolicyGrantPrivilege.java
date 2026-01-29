package us.zoom.data.dfence.providers.snowflake.policies.models;

public record PolicyGrantPrivilege(String value) {
  public PolicyGrantPrivilege {
    value = value.trim().toUpperCase();
  }
}
