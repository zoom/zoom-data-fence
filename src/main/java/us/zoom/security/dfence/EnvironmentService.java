package us.zoom.security.dfence;

import java.util.Map;

public class EnvironmentService {
    public Map<String, String> getEnv() {
        return System.getenv();
    }
}
