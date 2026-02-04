package us.zoom.data.dfence.policies.validations;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.policies.PolicyWildcards;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.pattern.models.ValidationErr;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseValidations {

    public static <E, A> Validation<Seq<E>, A> liftError(Validation<E, A> validation) {
        return validation.mapError(List::of);
    }


  public static Validation<ValidationErr, String> database(PolicyPattern pattern) {
    return db(pattern).validValue();
  }

  public static Validation<ValidationErr, String> schema(PolicyPattern pattern) {
    return sch(pattern).validValue();
  }

  public static Validation<ValidationErr, String> object(PolicyPattern pattern) {
    return obj(pattern).validValue();
  }

  public static SelectedFieldForValidation db(PolicyPattern pattern) {
    return new SelectedFieldForValidation(pattern.dbName(), "database");
  }

  public static SelectedFieldForValidation sch(PolicyPattern pattern) {
    return new SelectedFieldForValidation(pattern.schName(), "schema");
  }

  public static SelectedFieldForValidation obj(PolicyPattern pattern) {
    return new SelectedFieldForValidation(pattern.objName(), "object");
  }
  public record SelectedFieldForValidation(Option<String> fieldValue, String fieldName) {

      public Validation<ValidationErr, Void> anything() {
          return empty().orElse(wildcard()).orElse(validValueVoid());
      }

      public Validation<ValidationErr, Void> validValueVoid() {
          return validValue().map(v -> (Void) null);
      }


    public Validation<ValidationErr, String> validValue() {
      return nonEmpty()
          .flatMap(
              value ->
                  PolicyWildcards.isWildcard(value)
                      ? Validation.invalid(
                          new ValidationErr.Error(
                              String.format(
                                  "%s value is %s, non-empty and non-wildcard value is expected.",
                                  fieldName, value)))
                      : Validation.valid(ObjectName.normalizeObjectName(value)));
    }

    public Validation<ValidationErr, Void> emptyOrWildcard() {
      return empty().orElse(wildcard());
    }

    public Validation<ValidationErr, Void> empty() {
      return fieldValue
          .<Validation<ValidationErr, Void>>map(
              x ->
                  Validation.invalid(
                          new ValidationErr.Error(
                          String.format("%s is not empty, empty is expected.", fieldName))))
          .getOrElse(Validation.valid(null));
    }

      public Validation<ValidationErr, Void> wildcard() {
          return nonEmpty()
                  .flatMap(
                          value ->
                                  PolicyWildcards.isWildcard(value)
                                          ? Validation.valid(null)
                                          : Validation.invalid(
                                          new ValidationErr.Error(
                                          String.format(
                                                  "%s value is %s, wildcard is expected.", fieldName, value))));
      }

    public Validation<ValidationErr, String> nonEmpty() {
      return fieldValue
          .<Validation<ValidationErr, String>>map(x -> Validation.valid(x.trim()))
          .getOrElse(
              Validation.invalid(
                      new ValidationErr.Error(
                      String.format("%s is empty, non-empty value is expected.", fieldName))));
    }
  }
}
