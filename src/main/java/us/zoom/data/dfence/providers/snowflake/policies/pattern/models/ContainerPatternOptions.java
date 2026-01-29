package us.zoom.data.dfence.providers.snowflake.policies.pattern.models;

import io.vavr.collection.List;

public record ContainerPatternOptions(List<ContainerPatternOption> options) {
  public static ContainerPatternOptions of(ContainerPatternOption... options) {
    return new ContainerPatternOptions(List.of(options));
  }

  public static ContainerPatternOptions of(java.util.List<ContainerPatternOption> options) {
    return new ContainerPatternOptions(List.ofAll(options));
  }
}
