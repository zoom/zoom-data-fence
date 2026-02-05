package us.zoom.data.dfence.policies.validations;

import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.policies.PolicyWildcards;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseValidations {

  public static Validation<ValidationError, String> database(PolicyPattern pattern) {
    return db(pattern).validValue();
  }

  public static Validation<ValidationError, String> schema(PolicyPattern pattern) {
    return sch(pattern).validValue();
  }

  public static Validation<ValidationError, String> object(PolicyPattern pattern) {
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

    public Validation<ValidationError, Void> notWildcard() {
      return emptyOrValidValue();
    }

    public Validation<ValidationError, Void> emptyOrValidValue() {
      return empty().orElse(validValueVoid());
    }

    public Validation<ValidationError, Void> validValueVoid() {
      return validValue().map(v -> (Void) null);
    }

    public Validation<ValidationError, String> validValue() {
      return nonEmpty()
          .flatMap(
              value ->
                  PolicyWildcards.isWildcard(value)
                      ? invalidPolicyPatternError(
                          String.format(
                              "%s value is %s, non-empty and non-wildcard value is expected.",
                              fieldName, value))
                      : Validation.valid(ObjectName.normalizeObjectName(value)));
    }

    public Validation<ValidationError, Void> emptyOrWildcard() {
      return empty().orElse(wildcard());
    }

    public Validation<ValidationError, Void> empty() {
      return fieldValue
          .<Validation<ValidationError, Void>>map(
              x ->
                  invalidPolicyPatternError(
                      String.format("%s is not empty, empty is expected.", fieldName)))
          .getOrElse(Validation.valid(null));
    }

    public Validation<ValidationError, Void> wildcard() {
      return nonEmpty()
          .flatMap(
              value ->
                  PolicyWildcards.isWildcard(value)
                      ? Validation.valid(null)
                      : invalidPolicyPatternError(
                          String.format(
                              "%s value is %s, wildcard is expected.", fieldName, value)));
    }

    public Validation<ValidationError, String> nonEmpty() {
      return fieldValue
          .<Validation<ValidationError, String>>map(x -> Validation.valid(x.trim()))
          .getOrElse(
              invalidPolicyPatternError(
                  String.format("%s is empty, non-empty value is expected.", fieldName)));
    }
  }

  public static <I> Validation<ValidationError, I> invalidPolicyPatternError(String message) {
    return Validation.invalid(new ValidationError.InvalidPolicyPattern(message));
  }
}
