package us.zoom.data.dfence.providers.snowflake.policies.validations;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.policies.PlaybookWildcards;
import us.zoom.data.dfence.providers.snowflake.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyPattern;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseValidations {

  public static Validation<ValidationError, String> database(PolicyPattern pattern) {
    return db(pattern).validValue("database");
  }

  public static Validation<ValidationError, String> schema(PolicyPattern pattern) {
    return sch(pattern).validValue("schema");
  }

  public static Validation<ValidationError, String> object(PolicyPattern pattern) {
    return obj(pattern).validValue("object");
  }

  public static <E, A> Validation<Seq<E>, A> liftError(Validation<E, A> validation) {
    return validation.mapError(List::of);
  }

  public static SelectedFieldForValidation db(PolicyPattern pattern) {
    return new SelectedFieldForValidation(pattern.dbName());
  }

  public static SelectedFieldForValidation sch(PolicyPattern pattern) {
    return new SelectedFieldForValidation(pattern.schName());
  }

  public static SelectedFieldForValidation obj(PolicyPattern pattern) {
    return new SelectedFieldForValidation(pattern.objName());
  }
  public record SelectedFieldForValidation(Option<String> field) {

    public Validation<ValidationError, String> validValue(String fieldName) {
      return nonEmpty(fieldName)
          .flatMap(
              value ->
                  PlaybookWildcards.isWildcard(value)
                      ? Validation.invalid(
                          ValidationError.of(
                              String.format(
                                  "%s value is %s, non-empty and non-wildcard value is expected.",
                                  fieldName, value)))
                      : Validation.valid(ObjectName.normalizeObjectName(value)));
    }

    public Validation<ValidationError, Void> emptyOrWildcard(String fieldName) {
      return empty(fieldName).orElse(wildcard(fieldName));
    }

    public Validation<ValidationError, Void> empty(String fieldName) {
      return field
          .<Validation<ValidationError, Void>>map(
              x ->
                  Validation.invalid(
                      ValidationError.of(
                          String.format("%s is not empty, empty is expected.", fieldName))))
          .getOrElse(Validation.valid(null));
    }

    public Validation<ValidationError, String> nonEmpty(String fieldName) {
      return field
          .<Validation<ValidationError, String>>map(x -> Validation.valid(x.trim()))
          .getOrElse(
              Validation.invalid(
                  ValidationError.of(
                      String.format("%s is empty, non-empty value is expected.", fieldName))));
    }

    public Validation<ValidationError, Void> wildcard(String fieldName) {
      return nonEmpty(fieldName)
          .flatMap(
              value ->
                  PlaybookWildcards.isWildcard(value)
                      ? Validation.valid(null)
                      : Validation.invalid(
                          ValidationError.of(
                              String.format(
                                  "%s value is %s, wildcard is expected.", fieldName, value))));
    }
  }
}
