package us.zoom.data.dfence.policies.models;

import io.vavr.control.Option;

public record PolicyPattern(Option<String> dbName, Option<String> schName, Option<String> objName) {
  public static PolicyPattern of(
      Option<String> dbName, Option<String> schName, Option<String> objName) {
    return new PolicyPattern(dbName, schName, objName);
  }
}
