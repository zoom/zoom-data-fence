package us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models;

// Wrapper around validation error message for typesafety. Doesn't need to implement throwable.
public record ValidationError(String message) {
  public static ValidationError of(String message) {
    return new ValidationError(message);
  }
}
