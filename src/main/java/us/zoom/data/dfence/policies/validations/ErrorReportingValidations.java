package us.zoom.data.dfence.policies.validations;

import static us.zoom.data.dfence.policies.validations.BaseValidations.*;
import static us.zoom.data.dfence.policies.validations.Extensions.*;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;

/**
 * Validations that produce InvalidContainerPolicyPattern (deprecated container pattern) when
 * preconditions are met. Used as the fallback branch in container pattern validation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorReportingValidations {

  /**
   * Returns invalid container pattern validation for qual level 2 when database is present and
   * schema/object are empty (message: DB.* is expected for qual level 2 object-type).
   */
  public static Validation<Seq<ValidationError>, PolicyType.Container>
      reportInvalidContainerPatternQual2(PolicyPattern pattern) {
    return liftAndCast(
        database(pattern)
            .flatMap(db -> sch(pattern).empty().orElse(obj(pattern).empty()))
            .flatMap(
                sch -> invalidContainerPattern("DB.* is expected for qual level 2 object-type")),
        PolicyType.Container.class);
  }

  /**
   * Returns invalid container pattern validation for qual level 3 when database is present and
   * schema/object are not wildcard (message: DB.SCH.* or DB.*.OBJ or DB.*.* is expected for qual
   * level 3 object-type).
   */
  public static Validation<Seq<ValidationError>, PolicyType.Container>
      reportInvalidContainerPatternQual3(PolicyPattern pattern) {
    return liftAndCast(
        database(pattern)
            .flatMap(db -> sch(pattern).notWildcard())
            .flatMap(sch -> obj(pattern).notWildcard())
            .flatMap(
                obj ->
                    invalidContainerPattern(
                        "DB.SCH.* or DB.*.OBJ or DB.*.* is expected for qual level 3 object-type")),
        PolicyType.Container.class);
  }

  private static <I> Validation<ValidationError, I> invalidContainerPattern(String message) {
    return Validation.invalid(new ValidationError.InvalidContainerPolicyPattern(message));
  }
}
