package us.zoom.data.dfence.policies.pattern.models;

public sealed interface ValidationError {
  String message();

  record InvalidContainerPolicyPattern(String message) implements ValidationError {}
  record InvalidPolicyPattern(String message) implements ValidationError {}
}
