package us.zoom.data.dfence.providers.snowflake.shared.models;

import io.vavr.control.Option;

public record PlaybookPattern(
    Option<String> dbName, Option<String> schName, Option<String> objName) {
  public static PlaybookPattern of(
      Option<String> dbName, Option<String> schName, Option<String> objName) {
    return new PlaybookPattern(dbName, schName, objName);
  }
}
