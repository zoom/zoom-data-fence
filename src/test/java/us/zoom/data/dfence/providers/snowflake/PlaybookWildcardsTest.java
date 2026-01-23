package us.zoom.data.dfence.providers.snowflake;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlaybookWildcardsTest {

  @Test
  void isWildcardOrNull_shouldReturnTrue_whenValueIsNull() {
    assertTrue(PlaybookWildcards.isWildcardOrNull(null));
  }

  @Test
  void isWildcardOrNull_shouldReturnTrue_whenValueIsWildcard() {
    assertTrue(PlaybookWildcards.isWildcardOrNull("*"));
  }

  @Test
  void isWildcardOrNull_shouldReturnFalse_whenValueIsNotWildcard() {
    assertFalse(PlaybookWildcards.isWildcardOrNull("foo"));
  }

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
