package us.zoom.data.dfence.providers.snowflake.grant.builder.options;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SnowflakeGrantBuilderOptionsTest {

  @Test
  void testDefaultValues() {
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();

    assertEquals(UnsupportedRevokeBehavior.IGNORE, options.getUnsupportedRevokeBehavior());
    assertFalse(options.getSuppressErrors());
  }

  @Test
  void testSetUnsupportedRevokeBehavior() {
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();

    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);
    assertEquals(UnsupportedRevokeBehavior.DROP, options.getUnsupportedRevokeBehavior());

    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.IGNORE);
    assertEquals(UnsupportedRevokeBehavior.IGNORE, options.getUnsupportedRevokeBehavior());
  }

  @Test
  void testSetSuppressErrors() {
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();

    options.setSuppressErrors(true);
    assertTrue(options.getSuppressErrors());

    options.setSuppressErrors(false);
    assertFalse(options.getSuppressErrors());
  }

  @Test
  void testSetNullUnsupportedRevokeBehavior() {
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();

    options.setUnsupportedRevokeBehavior(null);
    assertNull(options.getUnsupportedRevokeBehavior());
  }

  @Test
  void testSetNullSuppressErrors() {
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();

    options.setSuppressErrors(null);
    assertNull(options.getSuppressErrors());
  }

  @Test
  void testMultipleOptionsInstances() {
    SnowflakeGrantBuilderOptions options1 = new SnowflakeGrantBuilderOptions();
    SnowflakeGrantBuilderOptions options2 = new SnowflakeGrantBuilderOptions();

    // Set different values on each instance
    options1.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);
    options1.setSuppressErrors(true);

    options2.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.IGNORE);
    options2.setSuppressErrors(false);

    // Verify they are independent
    assertEquals(UnsupportedRevokeBehavior.DROP, options1.getUnsupportedRevokeBehavior());
    assertTrue(options1.getSuppressErrors());

    assertEquals(UnsupportedRevokeBehavior.IGNORE, options2.getUnsupportedRevokeBehavior());
    assertFalse(options2.getSuppressErrors());
  }

  @Test
  void testEqualsAndHashCode() {
    SnowflakeGrantBuilderOptions options1 = new SnowflakeGrantBuilderOptions();
    SnowflakeGrantBuilderOptions options2 = new SnowflakeGrantBuilderOptions();
    SnowflakeGrantBuilderOptions options3 = new SnowflakeGrantBuilderOptions();

    // Set same values
    options1.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);
    options1.setSuppressErrors(true);

    options2.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);
    options2.setSuppressErrors(true);

    // Set different values
    options3.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.IGNORE);
    options3.setSuppressErrors(false);

    assertEquals(options1, options2);
    assertNotEquals(options1, options3);
    assertNotEquals(options2, options3);

    assertEquals(options1.hashCode(), options2.hashCode());
    assertNotEquals(options1.hashCode(), options3.hashCode());
  }

  @Test
  void testToString() {
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);
    options.setSuppressErrors(true);

    String toString = options.toString();

    assertTrue(toString.contains("unsupportedRevokeBehavior=DROP"));
    assertTrue(toString.contains("suppressErrors=true"));
  }
}
