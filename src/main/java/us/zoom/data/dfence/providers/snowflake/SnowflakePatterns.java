package us.zoom.data.dfence.providers.snowflake;

import static java.util.regex.Pattern.compile;

import java.util.regex.Pattern;

public class SnowflakePatterns {
  private final Pattern informationSchemaPattern =
      compile("^[a-zA-Z0-9_\"]+.\"?INFORMATION_SCHEMA\"?(.[a-zA-Z0-9_\"]+)?");
}
