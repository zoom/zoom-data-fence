package us.zoom.data.dfence.policies.pattern.models;

public interface ValidationError {
    String message();
    record InvalidContainerPolicyPattern(String message) implements ValidationError {}
    record InvalidPolicyPattern(String message) implements ValidationError {}

    static ValidationError invalidPolicyPattern(String message) {
        return new InvalidPolicyPattern(message);
    }
}
