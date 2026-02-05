package us.zoom.data.dfence.policies.validations;

import static us.zoom.data.dfence.policies.validations.BaseValidations.*;
import static us.zoom.data.dfence.policies.validations.Extensions.*;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOptions;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

public record PolicyPatternValidations(
    PolicyPattern pattern, PolicyPatternOptions patternOptions, SnowflakeObjectType objectType) {

  public Validation<Seq<ValidationError>, PolicyType.Standard> validateStandardPattern() {
    return switch (objectType.getQualLevel()) {
      case 0 -> Validation.valid(new PolicyType.Standard.Global());
      case 1 -> liftError(database(pattern).orElse(object(pattern)))
          .map(PolicyType.Standard.AccountObject::new);
      case 2 -> database(pattern).combine(schema(pattern)).ap(PolicyType.Standard.Schema::new);
      case 3 -> database(pattern)
          .combine(schema(pattern))
          .combine(object(pattern))
          .ap(PolicyType.Standard.SchemaObject::new);
      default -> invalidPolicyPatternError(
          String.format("Unknown qual level %s for grant object", objectType.getQualLevel()));
    };
  }

  public Validation<Seq<ValidationError>, PolicyType.Container> validateContainerPattern() {

    if (getContainerPolicyOptions().options().isEmpty()) {
      return invalidPolicyPatternError(
          "Both include-future and include-all cannot be false for container grants");
    }

    // At least one wildcard is expected in schema or object position for container grants
    Validation<Seq<ValidationError>, PolicyType.Container> preconditionValidation =
        liftAndCast(
            sch(pattern).wildcard().orElse(obj(pattern).wildcard()), PolicyType.Container.class);

    return switch (objectType.getQualLevel()) {
      case 2 -> {
        Validation<Seq<ValidationError>, PolicyType.Container> accountObjectContainerValidation =
            database(pattern)
                .combine(sch(pattern).wildcard().orElse(obj(pattern).wildcard()))
                .ap(this::makeAccountObjectContainer);

        yield preconditionValidation
            .flatMap(i -> accountObjectContainerValidation)
            .orElse(ErrorReportingValidations.reportInvalidContainerPatternErrorQual2(pattern));
      }

      case 3 -> {
        Validation<Seq<ValidationError>, PolicyType.Container> allSchemasContainerValidation =
            database(pattern)
                .combine(sch(pattern).emptyOrWildcard())
                .combine(obj(pattern).emptyOrWildcard())
                .ap(this::makeAllSchemasContainer);

        Validation<Seq<ValidationError>, PolicyType.Container> schemaContainerValidation =
            database(pattern)
                .combine(schema(pattern))
                .combine(obj(pattern).emptyOrWildcard())
                .ap(this::makeSchemaContainer);

        yield preconditionValidation
            .flatMap(i -> allSchemasContainerValidation.orElse(schemaContainerValidation))
            .orElse(ErrorReportingValidations.reportInvalidContainerPatternErrorQual3(pattern));
      }
      default -> invalidPolicyPatternError(
          String.format(
              "Unknown qual level %s for container grant object", objectType.getQualLevel()));
    };
  }

  private PolicyType.Container makeAccountObjectContainer(String databaseName, Void unusedSchema) {
    return new PolicyType.Container.AccountObject(
        databaseName, getContainerPolicyOptions(), SnowflakeObjectType.DATABASE);
  }

  private PolicyType.Container makeAllSchemasContainer(
      String databaseName, Void unusedSchema, Void unusedObject) {
    return new PolicyType.Container.SchemaObjectAllSchemas(
        databaseName, getContainerPolicyOptions());
  }

  private PolicyType.Container makeSchemaContainer(
      String databaseName, String schema, Void unusedObject) {
    return new PolicyType.Container.Schema(databaseName, schema, getContainerPolicyOptions());
  }

  private ContainerPolicyOptions getContainerPolicyOptions() {
    List<ContainerPolicyOption> options = List.empty();
    if (patternOptions.all()) {
      options = options.append(ContainerPolicyOption.ALL);
    }
    if (patternOptions.future()) {
      options = options.append(ContainerPolicyOption.FUTURE);
    }
    return new ContainerPolicyOptions(options);
  }

  private static <I> Validation<Seq<ValidationError>, I> invalidPolicyPatternError(String message) {
    return Validation.invalid(List.of(new ValidationError.InvalidPolicyPattern(message)));
  }
}
