package us.zoom.data.dfence.policies.pattern.models;

import io.vavr.collection.List;

public record ContainerPolicyOptions(List<ContainerPolicyOption> options) {
  public boolean all() {
    return options.contains(ContainerPolicyOption.ALL);
  }

  public boolean future() {
    return options.contains(ContainerPolicyOption.FUTURE);
  }
}
