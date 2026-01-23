package us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers;

public record GrantPrivilege(String value) {
  public GrantPrivilege {
    value = value.trim().toUpperCase();
  }
}
