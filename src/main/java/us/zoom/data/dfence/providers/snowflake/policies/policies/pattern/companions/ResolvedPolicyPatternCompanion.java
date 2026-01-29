package us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.companions;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.policies.policies.PolicyPatternValidations;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyPattern;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyPatternOptions;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolvedPolicyPatternCompanion {

  public static Validation<Seq<ValidationError>, ResolvedPlaybookPattern> from(
          PolicyPattern pattern, SnowflakeObjectType objectType, PolicyPatternOptions options) {
    PolicyPatternValidations validators = new PolicyPatternValidations(pattern, objectType);

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> validateStandardPattern =
        lift(validators.validateStandardPattern());
    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> validateContainerPattern =
        lift(validators.validateContainerPattern(options));

    return validateStandardPattern.orElse(validateContainerPattern);
  }

  private static <E extends ResolvedPlaybookPattern>
      Validation<Seq<ValidationError>, ResolvedPlaybookPattern> lift(
          Validation<Seq<ValidationError>, E> validation) {
    return validation.map(v -> (ResolvedPlaybookPattern) v);
  }
}
