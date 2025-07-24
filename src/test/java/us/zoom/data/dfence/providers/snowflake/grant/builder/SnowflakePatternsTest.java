package us.zoom.data.dfence.providers.snowflake.grant.builder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakePatternsTest {

    @ParameterizedTest
    @ValueSource(strings = {"<TABLE>", "<SCHEMA>", "<MATERIALIZED VIEW>"})
    void getFutureElementPattern(String value) {
        assertTrue(SnowflakePatterns.getFutureElementPattern().matcher(value).find());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOO", "foo", "foo bar", "FOO_BAR", "", "<>"})
    void getFutureElementPatternFalse(String value) {
        assertFalse(SnowflakePatterns.getFutureElementPattern().matcher(value).find());
    }
}