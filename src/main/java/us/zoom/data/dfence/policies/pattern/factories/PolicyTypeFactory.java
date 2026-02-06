package us.zoom.data.dfence.policies.pattern.factories;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.validations.PolicyPatternValidations;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

/** Builds a {@link PolicyType} (standard or container) from a policy pattern and object type. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyTypeFactory {

  /** Tries standard pattern first, then container pattern; returns the first valid result. */
  public static Validation<Seq<ValidationError>, PolicyType> createFrom(
      PolicyPattern pattern, SnowflakeObjectType objectType, PolicyPatternOptions options) {
    PolicyPatternValidations validators =
        new PolicyPatternValidations(pattern, options, objectType);

    return validators
        .validateStandardPattern()
        .map(p -> (PolicyType) p)
        .orElse(validators.validateContainerPattern());
  }
}
