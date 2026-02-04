package us.zoom.data.dfence.policies.pattern.models;

public interface ValidationErr {
    String message();
    record InvalidContainerPolicyPattern(String message) implements ValidationErr {}
    record Error(String message) implements ValidationErr {}
}
