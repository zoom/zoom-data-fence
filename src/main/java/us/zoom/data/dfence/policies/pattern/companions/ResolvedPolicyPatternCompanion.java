package us.zoom.data.dfence.policies.pattern.companions;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.validations.PolicyPatternValidations;
import us.zoom.data.dfence.policies.pattern.models.ResolvedPolicyPattern;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolvedPolicyPatternCompanion {

  public static Validation<Seq<ValidationError>, ResolvedPolicyPattern> from(
          PolicyPattern pattern, SnowflakeObjectType objectType, PolicyPatternOptions options) {
    PolicyPatternValidations validators = new PolicyPatternValidations(pattern, objectType);

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> validateStandardPattern =
        lift(validators.validateStandardPattern());
    Validation<Seq<ValidationError>, ResolvedPolicyPattern> validateContainerPattern =
        lift(validators.validateContainerPattern(options));

    return validateStandardPattern.orElse(validateContainerPattern);
  }

  private static <E extends ResolvedPolicyPattern>
      Validation<Seq<ValidationError>, ResolvedPolicyPattern> lift(
          Validation<Seq<ValidationError>, E> validation) {
    return validation.map(v -> (ResolvedPolicyPattern) v);
  }
}
