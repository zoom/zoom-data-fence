package us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.shared.PlaybookWildcards;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseValidations {

  public static Validation<String, String> database(PlaybookPattern pattern) {
    return db(pattern).validValue("database");
  }

  public static Validation<String, String> schema(PlaybookPattern pattern) {
    return sch(pattern).validValue("schema");
  }

  public static Validation<String, String> object(PlaybookPattern pattern) {
    return obj(pattern).validValue("object");
  }

  public static <E, A> Validation<Seq<E>, A> liftError(Validation<E, A> validation) {
    return validation.mapError(List::of);
  }

  public static SelectedFieldForValidation db(PlaybookPattern pattern) {
    return new SelectedFieldForValidation(pattern.dbName());
  }

  public static SelectedFieldForValidation sch(PlaybookPattern pattern) {
    return new SelectedFieldForValidation(pattern.schName());
  }

  public static SelectedFieldForValidation obj(PlaybookPattern pattern) {
    return new SelectedFieldForValidation(pattern.objName());
  }
  public record SelectedFieldForValidation(Option<String> field) {

    public Validation<String, String> validValue(String fieldName) {
      return nonEmpty(fieldName)
          .flatMap(
              value ->
                  PlaybookWildcards.isWildcard(value)
                      ? Validation.invalid(
                          String.format(
                              "%s value is %s, non-empty and non-wildcard value is expected.",
                              fieldName, value))
                      : Validation.valid(ObjectName.normalizeObjectName(value)));
    }

    public Validation<String, Void> emptyOrWildcard(String fieldName) {
      return empty(fieldName).orElse(wildcard(fieldName));
    }

    public Validation<String, Void> empty(String fieldName) {
      return field
          .<Validation<String, Void>>map(
              x ->
                  Validation.invalid(
                      String.format("%s is not empty, empty is expected.", fieldName)))
          .getOrElse(Validation.valid(null));
    }

    public Validation<String, String> nonEmpty(String fieldName) {
      return field
          .<Validation<String, String>>map(x -> Validation.valid(x.trim()))
          .getOrElse(
              Validation.invalid(
                  String.format("%s is empty, non-empty value is expected.", fieldName)));
    }

    public Validation<String, Void> wildcard(String fieldName) {
      return nonEmpty(fieldName)
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
