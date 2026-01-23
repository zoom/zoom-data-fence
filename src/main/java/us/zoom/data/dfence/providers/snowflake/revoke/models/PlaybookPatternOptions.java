package us.zoom.data.dfence.providers.snowflake.revoke.models;

public enum PlaybookPatternOptions {
  STANDARD,
  FUTURE,
  ALL,
  FUTURE_AND_ALL;

  public static PlaybookPatternOptions of(boolean includeFuture, boolean includeAll) {
    if (includeFuture && includeAll) return FUTURE_AND_ALL;
    else if (includeFuture) return FUTURE;
    else if (includeAll) return ALL;
    else return STANDARD;
  }
}
