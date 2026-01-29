package us.zoom.data.dfence.policies.models;

public record PolicyGrantPrivilege(String value) {
  public PolicyGrantPrivilege {
    value = value.trim().toUpperCase();
  }
}
