package us.zoom.data.dfence.providers.snowflake.grant.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnowflakeDefaultObjectServiceTypeTest {

    @Test
    void getQualLevel() {
        assertEquals(3, SnowflakeObjectType.TABLE.getQualLevel());
    }
}