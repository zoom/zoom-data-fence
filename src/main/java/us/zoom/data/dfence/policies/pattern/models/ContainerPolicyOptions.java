package us.zoom.data.dfence.policies.pattern.models;

import io.vavr.collection.List;

public record ContainerPolicyOptions(List<ContainerPolicyOption> options) {
  public static ContainerPolicyOptions of(ContainerPolicyOption... options) {
    return new ContainerPolicyOptions(List.of(options));
  }

  public static ContainerPolicyOptions of(java.util.List<ContainerPolicyOption> options) {
    return new ContainerPolicyOptions(List.ofAll(options));
  }

  public boolean all() {
    return options.contains(ContainerPolicyOption.ALL);
  }

  public boolean future() {
    return options.contains(ContainerPolicyOption.FUTURE);
  }
}
