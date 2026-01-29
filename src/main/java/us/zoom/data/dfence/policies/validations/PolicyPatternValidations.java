package us.zoom.data.dfence.policies.validations;

import static us.zoom.data.dfence.policies.validations.BaseValidations.*;

import io.vavr.Function3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import java.util.ArrayList;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.policies.pattern.models.ResolvedPolicyPattern;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

public record PolicyPatternValidations(PolicyPattern pattern, SnowflakeObjectType objectType) {

  public Validation<Seq<ValidationError>, ResolvedPolicyPattern.Standard>
      validateStandardPattern() {
    return switch (objectType.getQualLevel()) {
      case 0 -> Validation.valid(new ResolvedPolicyPattern.Standard.Global());
      case 1 -> objectType == SnowflakeObjectType.DATABASE
          ? liftError(database(pattern))
              .map(ResolvedPolicyPattern.Standard.AccountObjectDatabase::new)
          : liftError(object(pattern)).map(ResolvedPolicyPattern.Standard.AccountObject::new);
      case 2 -> database(pattern)
          .combine(schema(pattern))
          .ap(ResolvedPolicyPattern.Standard.Schema::new);
      case 3 -> database(pattern)
          .combine(schema(pattern))
          .combine(object(pattern))
          .ap(ResolvedPolicyPattern.Standard.SchemaObject::new);
      default -> Validation.invalid(
          List.of(
              ValidationError.of(
                  String.format(
                      "Unknown qual level %s for grant object", objectType.getQualLevel()))));
    };
  }

  public Validation<Seq<ValidationError>, ResolvedPolicyPattern.Container>
      validateContainerPattern(PolicyPatternOptions patternOptions) {
    ArrayList<ContainerPatternOption> options = new ArrayList<>();
    if (patternOptions.all()) {
      options.add(ContainerPatternOption.ALL);
    }
    if (patternOptions.future()) {
      options.add(ContainerPatternOption.FUTURE);
    }

    if (options.isEmpty()) {
      String err = "Both include-future and include-all cannot be false for container grants";
      return Validation.invalid((List.of(ValidationError.of(err))));
    }

    ContainerPatternOptions containerOptions = ContainerPatternOptions.of(options);

    return switch (objectType.getQualLevel()) {
      case 1 -> liftError(database(pattern))
          .map(
              databaseName ->
                  new ResolvedPolicyPattern.Container.AccountObjectDatabase(
                      databaseName, containerOptions));
      case 2 -> database(pattern)
          .combine(sch(pattern).emptyOrWildcard("schema"))
          .ap(
              (databaseName, unusedSchema) ->
                  new ResolvedPolicyPattern.Container.AccountObjectDatabase(
                      databaseName, containerOptions));
      case 3 -> {
        Validation<Seq<ValidationError>, ResolvedPolicyPattern.Container>
            objectLevelAllSchemasPattern =
                database(pattern)
                    .combine(sch(pattern).emptyOrWildcard("schema"))
                    .combine(object(pattern))
                    .ap(
                        (Function3<String, Void, String, ResolvedPolicyPattern.Container>)
                            (databaseName, unusedSchema, objectName) ->
                                new ResolvedPolicyPattern.Container.SchemaObjectAllSchemas(
                                    databaseName, objectName, containerOptions));

        // Database-level container (empty/wildcard schema and empty/wildcard object)
        Validation<Seq<ValidationError>, ResolvedPolicyPattern.Container> databaseLevelPattern =
            database(pattern)
                .combine(sch(pattern).emptyOrWildcard("schema"))
                .combine(obj(pattern).emptyOrWildcard("object"))
                .ap(
                    (Function3<String, Void, Void, ResolvedPolicyPattern.Container>)
                        (databaseName, unusedSchema, unusedObject) ->
                            new ResolvedPolicyPattern.Container.AccountObjectDatabase(
                                databaseName, containerOptions));

        // Schema-level container (specific schema, empty/wildcard object)
        Validation<Seq<ValidationError>, ResolvedPolicyPattern.Container> schemaLevelPattern =
            database(pattern)
                .combine(schema(pattern))
                .combine(obj(pattern).emptyOrWildcard("object"))
                .ap(
                    (Function3<String, String, Void, ResolvedPolicyPattern.Container>)
                        (databaseName, schemaName, unusedObject) ->
                            new ResolvedPolicyPattern.Container.Schema(
                                databaseName, schemaName, containerOptions));

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
