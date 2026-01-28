package us.zoom.data.dfence.providers.snowflake.grant.current.models;

public record LoadAllCurrentGrantsForRoleSettings(
    boolean roleExists, String roleName, boolean revokeOtherGrants, boolean ignoreUnknownGrants) {}
