package us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models;

import io.vavr.collection.List;

public record ContainerPatternOptions(List<ContainerPatternOption> options) {
  public static ContainerPatternOptions of(ContainerPatternOption... options) {
    return new ContainerPatternOptions(List.of(options));
  }
}
