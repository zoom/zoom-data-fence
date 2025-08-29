package us.zoom.data.dfence.test.fixtures;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class LifecycleWrapper implements LifecycleObject {
  private LifecycleObject wraps;
  private boolean performSetup = true;
  private boolean performTeardown = true;
  private boolean exists = false;

  public LifecycleWrapper(
      LifecycleObject lifecycleObject, Boolean performSetup, Boolean performTeardown) {
    this.wraps = lifecycleObject;
    this.performSetup = performSetup;
    this.performTeardown = performTeardown;
  }

  @Override
  public void setup() {
    if (!exists && performSetup) {
      wraps.setup();
      exists = true;
    }
  }

  @Override
  public void teardown() {
    if (exists && performTeardown) {
      wraps.teardown();
    }
  }
}
