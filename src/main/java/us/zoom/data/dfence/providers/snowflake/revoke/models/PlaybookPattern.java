package us.zoom.data.dfence.providers.snowflake.revoke.models;

import java.util.Optional;

public record PlaybookPattern(
    Optional<String> dbName, Optional<String> schName, Optional<String> objName) {}
