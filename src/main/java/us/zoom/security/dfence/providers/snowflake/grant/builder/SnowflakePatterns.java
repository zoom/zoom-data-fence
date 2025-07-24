package us.zoom.security.dfence.providers.snowflake.grant.builder;

import lombok.Getter;

import java.util.regex.Pattern;

public class SnowflakePatterns {


    @Getter
    private static final Pattern futureElementPattern = Pattern.compile("^<[A-Z0-9_ ]+>$");

}
