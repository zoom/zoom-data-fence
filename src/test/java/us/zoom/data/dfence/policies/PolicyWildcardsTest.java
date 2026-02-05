package us.zoom.data.dfence.policies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolicyWildcardsTest {

  @Test
  void isWildcard_shouldReturnTrue_whenValueIsWildcard() {
    assertTrue(PolicyWildcards.isWildcard("*"));
  }

  @Test
  void isWildcard_shouldReturnFalse_whenValueIsNull() {
    assertFalse(PolicyWildcards.isWildcard(null));
  }

  @Test
  void isWildcard_shouldReturnFalse_whenValueIsNotWildcard() {
    assertFalse(PolicyWildcards.isWildcard("foo"));
  }
}
