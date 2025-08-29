package us.zoom.data.dfence.providers.snowflake.grant.builder.options;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UnsupportedRevokeBehaviorTest {

  @Test
  void testEnumValues() {
    UnsupportedRevokeBehavior[] values = UnsupportedRevokeBehavior.values();
    assertEquals(2, values.length);

    assertTrue(containsValue(values, UnsupportedRevokeBehavior.IGNORE));
    assertTrue(containsValue(values, UnsupportedRevokeBehavior.DROP));
  }

  @Test
  void testValueOf() {
    assertEquals(UnsupportedRevokeBehavior.IGNORE, UnsupportedRevokeBehavior.valueOf("IGNORE"));
    assertEquals(UnsupportedRevokeBehavior.DROP, UnsupportedRevokeBehavior.valueOf("DROP"));
  }

  @Test
  void testValueOfInvalidValue() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          UnsupportedRevokeBehavior.valueOf("INVALID");
        });
  }

  @Test
  void testEnumEquality() {
    UnsupportedRevokeBehavior ignore1 = UnsupportedRevokeBehavior.IGNORE;
    UnsupportedRevokeBehavior ignore2 = UnsupportedRevokeBehavior.IGNORE;
    UnsupportedRevokeBehavior drop = UnsupportedRevokeBehavior.DROP;

    assertEquals(ignore1, ignore2);
    assertNotEquals(ignore1, drop);
    assertNotEquals(ignore2, drop);
  }

  @Test
  void testEnumHashCode() {
    UnsupportedRevokeBehavior ignore1 = UnsupportedRevokeBehavior.IGNORE;
    UnsupportedRevokeBehavior ignore2 = UnsupportedRevokeBehavior.IGNORE;
    UnsupportedRevokeBehavior drop = UnsupportedRevokeBehavior.DROP;

    assertEquals(ignore1.hashCode(), ignore2.hashCode());
    assertNotEquals(ignore1.hashCode(), drop.hashCode());
  }

  @Test
  void testEnumToString() {
    assertEquals("IGNORE", UnsupportedRevokeBehavior.IGNORE.toString());
    assertEquals("DROP", UnsupportedRevokeBehavior.DROP.toString());
  }

  @Test
  void testEnumOrdinal() {
    assertEquals(0, UnsupportedRevokeBehavior.IGNORE.ordinal());
    assertEquals(1, UnsupportedRevokeBehavior.DROP.ordinal());
  }

  private boolean containsValue(
      UnsupportedRevokeBehavior[] values, UnsupportedRevokeBehavior target) {
    for (UnsupportedRevokeBehavior value : values) {
      if (value == target) {
        return true;
      }
    }
    return false;
  }
}
