package us.zoom.data.dfence.test.fixtures;

import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

public class DirectoryLifecycleObject implements LifecycleObject {

  private Path directoryName;

  public DirectoryLifecycleObject(Path directoryName) {
    this.directoryName = directoryName;
  }

  @Override
  public void setup() {
    directoryName.toFile().mkdirs();
  }

  @Override
  public void teardown() {
    try {
      FileUtils.deleteDirectory(directoryName.toFile());
    } catch (java.io.IOException e) {
      throw new RuntimeException("Unable to delete directory.", e);
    }
  }
}
