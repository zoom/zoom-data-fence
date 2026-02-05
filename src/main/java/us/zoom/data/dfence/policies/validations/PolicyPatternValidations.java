package us.zoom.data.dfence.policies.validations;

import static us.zoom.data.dfence.policies.validations.BaseValidations.*;

import io.vavr.Function3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import java.util.ArrayList;

import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOptions;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

public record PolicyPatternValidations(PolicyPattern pattern, SnowflakeObjectType objectType) {

  public Validation<Seq<ValidationError>, PolicyType.Standard>
      validateStandardPattern() {
    return switch (objectType.getQualLevel()) {
      case 0 -> Validation.valid(new PolicyType.Standard.Global());
      case 1 -> liftError(database(pattern).orElse(object(pattern)))
              .map(PolicyType.Standard.AccountObject::new);
      case 2 -> database(pattern)
          .combine(schema(pattern))
          .ap(PolicyType.Standard.Schema::new);
      case 3 -> database(pattern)
          .combine(schema(pattern))
          .combine(object(pattern))
          .ap(PolicyType.Standard.SchemaObject::new);
      default -> Validation.invalid(
          List.of(
                  new ValidationError.InvalidPolicyPattern(
                  String.format("Unknown qual level %s for grant object", objectType.getQualLevel()))
          ));
    };
  }

  public Validation<Seq<ValidationError>, PolicyType.Container>
      validateContainerPattern(PolicyPatternOptions patternOptions) {
    ArrayList<ContainerPolicyOption> options = new ArrayList<>();
    if (patternOptions.all()) {
      options.add(ContainerPolicyOption.ALL);
    }
    if (patternOptions.future()) {
      options.add(ContainerPolicyOption.FUTURE);
    }

    if (options.isEmpty()) {
      String message = "Both include-future and include-all cannot be false for container grants";
      return Validation.invalid((List.of(new ValidationError.InvalidPolicyPattern(message))));
    }

    ContainerPolicyOptions containerPolicyOptions = ContainerPolicyOptions.of(options);

      Validation<Seq<ValidationError>, PolicyType.Container> validContainerGrant =
              sch(pattern).wildcard().orElse(obj(pattern).wildcard())
                      .map(v -> (PolicyType.Container) null)
                      .mapError(err -> List.of((ValidationError) err));

    return switch (objectType.getQualLevel()) {
        case 2 -> {
            Validation<Seq<ValidationError>, PolicyType.Container> deprecatedPattern = database(pattern)
                    .flatMap(db -> sch(pattern).empty())
                    .flatMap(sch -> invalidContainerPattern("DB.* is expected for qual level 2 object"))
                    .map(value -> (PolicyType.Container) value)
                    .mapError(err -> List.of((ValidationError) err));


            Validation<Seq<ValidationError>, PolicyType.Container> validPattern = database(pattern)
                .combine(sch(pattern).wildcard())
                .ap(
                        (databaseName, unusedSchema) ->
                                new PolicyType.Container.AccountObject(
                                        databaseName, containerPolicyOptions, SnowflakeObjectType.DATABASE));

            yield validContainerGrant.flatMap(i -> validPattern).orElse(deprecatedPattern);
        }

      case 3 -> {

          Validation<Seq<ValidationError>, PolicyType.Container> deprecatedPattern = database(pattern)
                  .flatMap(db -> sch(pattern).empty().orElse(sch(pattern).validValueVoid()))
                  .flatMap(sch -> obj(pattern).empty().orElse(obj(pattern).validValueVoid()))
                  .flatMap(obj -> invalidContainerPattern("DB.SCH.* or DB.*.OBJ is expected for qual level 3 object"))
                  .map(value -> (PolicyType.Container) value)
                  .mapError(err -> List.of((ValidationError) err));


        Validation<Seq<ValidationError>, PolicyType.Container>
            objectLevelAllSchemasPattern =
                database(pattern)
                        .combine(sch(pattern).emptyOrWildcard())
                        .combine(obj(pattern).emptyOrWildcard())
                    .ap(
                        (Function3<String, Void, Void, PolicyType.Container>)
                            (databaseName, unusedSchema, unusedObject) ->
                                new PolicyType.Container.SchemaObjectAllSchemas(
                                    databaseName, containerPolicyOptions));

        Validation<Seq<ValidationError>, PolicyType.Container> schemaLevelPattern =
            database(pattern)
                .combine(schema(pattern))
                .combine(obj(pattern).emptyOrWildcard())
                .ap(
                    (Function3<String, String, Void, PolicyType.Container>)
                        (databaseName, schemaName, unusedObject) ->
                            new PolicyType.Container.Schema(
                                databaseName, schemaName, containerPolicyOptions));

        yield validContainerGrant.flatMap(i -> objectLevelAllSchemasPattern.orElse(schemaLevelPattern))
                .orElse(deprecatedPattern);
      }
      default -> Validation.invalid(
          List.of(
                  new ValidationError.InvalidPolicyPattern(
                  String.format(
                      "Unknown qual level %s for container grant object",
                      objectType.getQualLevel()))));
    };
  }

    private static Validation<ValidationError, PolicyType.Container> invalidContainerPattern(
            String message) {
        return Validation.invalid(new ValidationError.InvalidContainerPolicyPattern(message));
    }
}
