package us.zoom.data.dfence.providers.snowflake.shared.models;

public record GrantPrivilege(String value) {
  public GrantPrivilege {
    value = value.trim().toUpperCase();
  }
}
