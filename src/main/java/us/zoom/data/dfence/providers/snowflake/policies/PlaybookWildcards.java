package us.zoom.data.dfence.providers.snowflake.policies;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaybookWildcards {
  public static Boolean isWildcard(String value) {
    return "*".equalsIgnoreCase(value);
  }
}
