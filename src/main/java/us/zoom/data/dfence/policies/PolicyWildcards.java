package us.zoom.data.dfence.policies;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyWildcards {
  public static Boolean isWildcard(String value) {
    return "*".equalsIgnoreCase(value);
  }
}
