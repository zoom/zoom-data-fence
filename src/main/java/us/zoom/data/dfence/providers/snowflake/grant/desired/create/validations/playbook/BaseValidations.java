package us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.shared.PlaybookWildcards;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseValidations {

  public static Validation<String, String> database(PlaybookPattern pattern) {
    return db().validValue("database").apply(pattern);
  }

  public static Validation<String, String> schema(PlaybookPattern pattern) {
    return sch().validValue("schema").apply(pattern);
  }

  public static Validation<String, String> object(PlaybookPattern pattern) {
    return obj().validValue("object").apply(pattern);
  }

  public static <E, A> Validation<Seq<E>, A> liftError(Validation<E, A> validation) {
    return validation.mapError(List::of);
  }

  public static SelectedFieldForValidation db() {
    return new SelectedFieldForValidation(PlaybookPattern::dbName);
  }

  public static SelectedFieldForValidation sch() {
    return new SelectedFieldForValidation(PlaybookPattern::schName);
  }

  public static SelectedFieldForValidation obj() {
    return new SelectedFieldForValidation(PlaybookPattern::objName);
  }
  public record SelectedFieldForValidation(Function<PlaybookPattern, Option<String>> field) {

    public Function<PlaybookPattern, Validation<String, String>> validValue(String fieldName) {
      return p ->
          nonEmpty(fieldName)
              .apply(p)
              .flatMap(
                  value ->
                      PlaybookWildcards.isWildcard(value)
                          ? Validation.invalid(
                              String.format(
                                  "%s value is %s, non-empty and non-wildcard value is expected.",
                                  fieldName, value))
                          : Validation.valid(ObjectName.normalizeObjectName(value)));
    }

    public Function<PlaybookPattern, Validation<String, Void>> emptyOrWildcard(String fieldName) {
      return p -> empty(fieldName).apply(p).orElse(wildcard(fieldName).apply(p));
    }

    public Function<PlaybookPattern, Validation<String, Void>> empty(String fieldName) {
      return p ->
          field
              .apply(p)
              .<Validation<String, Void>>map(
                  x ->
                      Validation.invalid(
                          String.format("%s is not empty, empty is expected.", fieldName)))
              .getOrElse(Validation.valid(null));
    }

    public Function<PlaybookPattern, Validation<String, String>> nonEmpty(String fieldName) {
      return p ->
          field
              .apply(p)
              .<Validation<String, String>>map(x -> Validation.valid(x.trim()))
              .getOrElse(
                  Validation.invalid(
                      String.format("%s is empty, non-empty value is expected.", fieldName)));
    }

    public Function<PlaybookPattern, Validation<String, Void>> wildcard(String fieldName) {
      return p ->
          nonEmpty(fieldName)
              .apply(p)
              .flatMap(
                  value ->
                      PlaybookWildcards.isWildcard(value)
                          ? Validation.valid(null)
                          : Validation.invalid(
                              String.format(
                                  "%s value is %s, wildcard is expected.", fieldName, value)));
    }
  }
}
