package us.zoom.data.dfence.policies.pattern.factories;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.policies.pattern.models.ValidationErr;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.validations.PolicyPatternValidations;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyTypeFactory {

  public static Validation<Seq<ValidationErr>, PolicyType> createFrom(
          PolicyPattern pattern, SnowflakeObjectType objectType, PolicyPatternOptions options) {
    PolicyPatternValidations validators = new PolicyPatternValidations(pattern, objectType);

    Validation<Seq<ValidationErr>, PolicyType> validateStandardPattern =
        lift(validators.validateStandardPattern());
    Validation<Seq<ValidationErr>, PolicyType> validateContainerPattern =
        lift(validators.validateContainerPattern(options));

    return validateStandardPattern.orElse(validateContainerPattern);
  }

  private static <E extends PolicyType>
      Validation<Seq<ValidationErr>, PolicyType> lift(
          Validation<Seq<ValidationErr>, E> validation) {
    return validation.map(v -> (PolicyType) v);
  }
}
