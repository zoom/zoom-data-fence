package us.zoom.data.dfence.providers.snowflake.policies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlaybookWildcardsTest {

  @Test
  void isWildcard_shouldReturnTrue_whenValueIsWildcard() {
    assertTrue(PlaybookWildcards.isWildcard("*"));
  }

  @Test
  void isWildcard_shouldReturnFalse_whenValueIsNull() {
    assertFalse(PlaybookWildcards.isWildcard(null));
  }

  @Test
  void isWildcard_shouldReturnFalse_whenValueIsNotWildcard() {
    assertFalse(PlaybookWildcards.isWildcard("foo"));
  }
}
