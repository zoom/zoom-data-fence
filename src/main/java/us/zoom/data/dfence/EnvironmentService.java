package us.zoom.data.dfence;

import java.util.Map;

public class EnvironmentService {
  public Map<String, String> getEnv() {
    return System.getenv();
  }
}
