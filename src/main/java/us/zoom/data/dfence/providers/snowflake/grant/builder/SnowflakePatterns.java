package us.zoom.data.dfence.providers.snowflake.grant.builder;

import java.util.regex.Pattern;

import lombok.Getter;

public class SnowflakePatterns {

  @Getter private static final Pattern futureElementPattern = Pattern.compile("^<[A-Z0-9_ ]+>$");
}
