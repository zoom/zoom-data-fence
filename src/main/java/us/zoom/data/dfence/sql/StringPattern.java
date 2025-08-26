package us.zoom.data.dfence.sql;

import java.util.*;

public class StringPattern {

  public static List<String> splitIgnore(
      String input, char delimiter, char ignoreStart, char ignoreEnd) {
    if (input == null || input.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> result = new ArrayList<>();
    StringBuilder currentSegment = new StringBuilder();
    Deque<Character> stack = new ArrayDeque<>();
    boolean inQuotes = false;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (c == '"') {
        inQuotes = !inQuotes;
      }

      if (!inQuotes) {
        if (c == ignoreStart) {
          stack.push(c);
        } else if (c == ignoreEnd && !stack.isEmpty() && stack.peek() == ignoreStart) {
          stack.pop();
        }
      }

      if (c == delimiter && stack.isEmpty() && !inQuotes) {
        result.add(currentSegment.toString().trim());
        currentSegment.setLength(0);
      } else {
        currentSegment.append(c);
      }
    }

    if (currentSegment.length() > 0) {
      result.add(currentSegment.toString().trim());
    }

    return result;
  }
}
