package us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.companions;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.PlaybookPatternValidations;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPatternOptions;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolvedPlaybookPatternCompanion {

  public static Validation<Seq<String>, ResolvedPlaybookPattern> from(
      PlaybookPattern pattern, SnowflakeObjectType objectType, PlaybookPatternOptions options) {
    PlaybookPatternValidations validators = new PlaybookPatternValidations(pattern, objectType);

    Validation<Seq<String>, ResolvedPlaybookPattern> validateStandardPattern =
        lift(validators.validateStandardPattern());
    Validation<Seq<String>, ResolvedPlaybookPattern> validateContainerPattern =
        lift(validators.validateContainerPattern(options));

    return validateStandardPattern.orElse(validateContainerPattern);
  }

  private static <E extends ResolvedPlaybookPattern>
      Validation<Seq<String>, ResolvedPlaybookPattern> lift(Validation<Seq<String>, E> validation) {
    return validation.map(v -> (ResolvedPlaybookPattern) v);
  }
}
