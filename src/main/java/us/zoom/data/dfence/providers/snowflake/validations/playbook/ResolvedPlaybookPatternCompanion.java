package us.zoom.data.dfence.providers.snowflake.validations.playbook;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPatternOptions;
import us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models.ResolvedPlaybookPattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolvedPlaybookPatternCompanion {

  public static Validation<Seq<String>, ResolvedPlaybookPattern> from(
      PlaybookPattern pattern, SnowflakeObjectType objectType, PlaybookPatternOptions options) {
    PlaybookPatternValidations validators = new PlaybookPatternValidations(pattern, objectType);
    return validators
        .toStandardTarget()
        .map(target -> (ResolvedPlaybookPattern) target)
        .orElse(validators.toContainerTarget(options));
  }
}
