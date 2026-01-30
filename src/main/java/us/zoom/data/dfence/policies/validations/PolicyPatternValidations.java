package us.zoom.data.dfence.policies.validations;

import static us.zoom.data.dfence.policies.validations.BaseValidations.*;

import io.vavr.Function3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import java.util.ArrayList;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOptions;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

public record PolicyPatternValidations(PolicyPattern pattern, SnowflakeObjectType objectType) {

  public Validation<Seq<ValidationError>, PolicyType.Standard>
      validateStandardPattern() {
    return switch (objectType.getQualLevel()) {
      case 0 -> Validation.valid(new PolicyType.Standard.Global());
      case 1 -> objectType == SnowflakeObjectType.DATABASE
          ? liftError(database(pattern))
              .map(PolicyType.Standard.AccountObjectDatabase::new)
          : liftError(object(pattern)).map(PolicyType.Standard.AccountObject::new);
      case 2 -> database(pattern)
          .combine(schema(pattern))
          .ap(PolicyType.Standard.Schema::new);
      case 3 -> database(pattern)
          .combine(schema(pattern))
          .combine(object(pattern))
          .ap(PolicyType.Standard.SchemaObject::new);
      default -> Validation.invalid(
          List.of(
              ValidationError.of(
                  String.format(
                      "Unknown qual level %s for grant object", objectType.getQualLevel()))));
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
      String err = "Both include-future and include-all cannot be false for container grants";
      return Validation.invalid((List.of(ValidationError.of(err))));
    }

    ContainerPolicyOptions containerPolicyOptions = ContainerPolicyOptions.of(options);

    return switch (objectType.getQualLevel()) {
      case 1 -> liftError(database(pattern))
          .map(
              databaseName ->
                  new PolicyType.Container.AccountObjectDatabase(
                      databaseName, containerPolicyOptions));
      case 2 -> database(pattern)
          .combine(sch(pattern).emptyOrWildcard("schema"))
          .ap(
              (databaseName, unusedSchema) ->
                  new PolicyType.Container.AccountObjectDatabase(
                      databaseName, containerPolicyOptions));
      case 3 -> {
        Validation<Seq<ValidationError>, PolicyType.Container>
            objectLevelAllSchemasPattern =
                database(pattern)
                    .combine(sch(pattern).emptyOrWildcard("schema"))
                    .combine(object(pattern))
                    .ap(
                        (Function3<String, Void, String, PolicyType.Container>)
                            (databaseName, unusedSchema, objectName) ->
                                new PolicyType.Container.SchemaObjectAllSchemas(
                                    databaseName, objectName, containerPolicyOptions));

        // Database-level container (empty/wildcard schema and empty/wildcard object)
        Validation<Seq<ValidationError>, PolicyType.Container> databaseLevelPattern =
            database(pattern)
                .combine(sch(pattern).emptyOrWildcard("schema"))
                .combine(obj(pattern).emptyOrWildcard("object"))
                .ap(
                    (Function3<String, Void, Void, PolicyType.Container>)
                        (databaseName, unusedSchema, unusedObject) ->
                            new PolicyType.Container.AccountObjectDatabase(
                                databaseName, containerPolicyOptions));

        // Schema-level container (specific schema, empty/wildcard object)
        Validation<Seq<ValidationError>, PolicyType.Container> schemaLevelPattern =
            database(pattern)
                .combine(schema(pattern))
                .combine(obj(pattern).emptyOrWildcard("object"))
                .ap(
                    (Function3<String, String, Void, PolicyType.Container>)
                        (databaseName, schemaName, unusedObject) ->
                            new PolicyType.Container.Schema(
                                databaseName, schemaName, containerPolicyOptions));

        yield objectLevelAllSchemasPattern.orElse(databaseLevelPattern).orElse(schemaLevelPattern);
      }
      default -> Validation.invalid(
          List.of(
              ValidationError.of(
                  String.format(
                      "Unknown qual level %s for container grant object",
                      objectType.getQualLevel()))));
    };
  }
}
