package us.zoom.security.dfence.test.fixtures;

import net.snowflake.client.jdbc.internal.apache.commons.io.FileUtils;

import java.nio.file.Path;

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
