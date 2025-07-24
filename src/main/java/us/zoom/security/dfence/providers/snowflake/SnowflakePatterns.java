package us.zoom.security.dfence.providers.snowflake;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class SnowflakePatterns {
    private final Pattern informationSchemaPattern = compile(
            "^[a-zA-Z0-9_\"]+.\"?INFORMATION_SCHEMA\"?(.[a-zA-Z0-9_\"]+)?");
}
