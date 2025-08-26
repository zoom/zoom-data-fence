package us.zoom.data.dfence.providers.snowflake.grant.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SnowflakeDefaultObjectServiceTypeTest {

  @Test
  void getQualLevel() {
    assertEquals(3, SnowflakeObjectType.TABLE.getQualLevel());
  }
}
