package us.zoom.data.dfence.providers.snowflake.policies.policies;

import static us.zoom.data.dfence.providers.snowflake.policies.policies.BaseValidations.*;

import io.vavr.Function3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import java.util.ArrayList;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyPattern;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyPatternOptions;

public record PolicyPatternValidations(PolicyPattern pattern, SnowflakeObjectType objectType) {

  public Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Standard>
      validateStandardPattern() {
    return switch (objectType.getQualLevel()) {
      case 0 -> Validation.valid(new ResolvedPlaybookPattern.Standard.Global());
      case 1 -> objectType == SnowflakeObjectType.DATABASE
          ? liftError(database(pattern))
              .map(ResolvedPlaybookPattern.Standard.AccountObjectDatabase::new)
          : liftError(object(pattern)).map(ResolvedPlaybookPattern.Standard.AccountObject::new);
      case 2 -> database(pattern)
          .combine(schema(pattern))
          .ap(ResolvedPlaybookPattern.Standard.Schema::new);
      case 3 -> database(pattern)
          .combine(schema(pattern))
          .combine(object(pattern))
          .ap(ResolvedPlaybookPattern.Standard.SchemaObject::new);
      default -> Validation.invalid(
          List.of(
              ValidationError.of(
                  String.format(
                      "Unknown qual level %s for grant object", objectType.getQualLevel()))));
    };
  }

  public Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Container>
      validateContainerPattern(PolicyPatternOptions playbookPatternOptions) {
    ArrayList<ContainerPatternOption> options = new ArrayList<>();
    if (playbookPatternOptions.all()) {
      options.add(ContainerPatternOption.ALL);
    }
    if (playbookPatternOptions.future()) {
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
                  new ResolvedPlaybookPattern.Container.AccountObjectDatabase(
                      databaseName, containerOptions));
      case 2 -> database(pattern)
          .combine(sch(pattern).emptyOrWildcard("schema"))
          .ap(
              (databaseName, unusedSchema) ->
                  new ResolvedPlaybookPattern.Container.AccountObjectDatabase(
                      databaseName, containerOptions));
      case 3 -> {
        Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Container>
            objectLevelAllSchemasPattern =
                database(pattern)
                    .combine(sch(pattern).emptyOrWildcard("schema"))
                    .combine(object(pattern))
                    .ap(
                        (Function3<String, Void, String, ResolvedPlaybookPattern.Container>)
                            (databaseName, unusedSchema, objectName) ->
                                new ResolvedPlaybookPattern.Container.SchemaObjectAllSchemas(
                                    databaseName, objectName, containerOptions));

        // Database-level container (empty/wildcard schema and empty/wildcard object)
        Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Container> databaseLevelPattern =
            database(pattern)
                .combine(sch(pattern).emptyOrWildcard("schema"))
                .combine(obj(pattern).emptyOrWildcard("object"))
                .ap(
                    (Function3<String, Void, Void, ResolvedPlaybookPattern.Container>)
                        (databaseName, unusedSchema, unusedObject) ->
                            new ResolvedPlaybookPattern.Container.AccountObjectDatabase(
                                databaseName, containerOptions));

        // Schema-level container (specific schema, empty/wildcard object)
        Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Container> schemaLevelPattern =
            database(pattern)
                .combine(schema(pattern))
                .combine(obj(pattern).emptyOrWildcard("object"))
                .ap(
                    (Function3<String, String, Void, ResolvedPlaybookPattern.Container>)
                        (databaseName, schemaName, unusedObject) ->
                            new ResolvedPlaybookPattern.Container.Schema(
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
