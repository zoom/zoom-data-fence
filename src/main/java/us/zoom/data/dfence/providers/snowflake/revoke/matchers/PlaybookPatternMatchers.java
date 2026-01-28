package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import io.vavr.control.Option;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaybookPatternMatchers {

  public static Predicate<PlaybookPattern> global() {
    return p -> true;
  }

  public static Predicate<PlaybookPattern> accountObject(String value) {
    return object(value);
  }

  public static Predicate<PlaybookPattern> database(String value) {
    return database().matches(value);
  }

  public static Predicate<PlaybookPattern> schema(String value) {
    return sch().matches(value);
  }

  public static Predicate<PlaybookPattern> object(String value) {
    return obj().matches(value);
  }

  public static Predicate<PlaybookPattern> noMatch() {
    return p -> false;
  }

  public static SelectedField database() {
    return new SelectedField(PlaybookPattern::dbName);
  }

  public static SelectedField sch() {
    return new SelectedField(PlaybookPattern::schName);
  }

  public static SelectedField obj() {
    return new SelectedField(PlaybookPattern::objName);
  }
  public record SelectedField(Function<PlaybookPattern, Option<String>> value) {

    public Predicate<PlaybookPattern> isEmptyOrWildcard() {
      return isEmpty().or(isWildcard());
    }

    public Predicate<PlaybookPattern> isWildcard() {
      return p -> value.apply(p).map(s -> "*".equals(s.trim())).getOrElse(false);
    }

    public Predicate<PlaybookPattern> isEmpty() {
      return p -> value.apply(p).isEmpty();
    }

    public Predicate<PlaybookPattern> isNotEmpty() {
      return isEmpty().negate();
    }

    public Predicate<PlaybookPattern> isNotWildcard() {
      return isWildcard().negate();
    }

    public Predicate<PlaybookPattern> matches(String currentValue) {
      Predicate<PlaybookPattern> matchesValue =
          p ->
              value
                  .apply(p)
                  .map(s -> ObjectName.equalObjectName(s.trim(), currentValue.trim()))
                  .getOrElse(false);
      return isNotEmpty().and(isNotWildcard()).and(matchesValue);
    }
  }
}
