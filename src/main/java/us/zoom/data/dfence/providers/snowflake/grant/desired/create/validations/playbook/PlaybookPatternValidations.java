package us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook;

import static us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.BaseValidations.*;

import io.vavr.Function3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import java.util.function.Function;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPatternOptions;

public record PlaybookPatternValidations(PlaybookPattern pattern, SnowflakeObjectType objectType) {

  public Validation<Seq<String>, ResolvedPlaybookPattern.Standard> validateStandardPattern() {
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
              String.format("Unknown qual level %s for grant object", objectType.getQualLevel())));
    };
  }

  public Validation<Seq<String>, ResolvedPlaybookPattern.Container> validateContainerPattern(
      PlaybookPatternOptions playbookPatternOptions) {
    Validation<Seq<String>, ContainerPatternOptions> validatedContainerOptions;

    if (!playbookPatternOptions.future() && !playbookPatternOptions.all()) {
      String err = "Both include-future and include-all cannot be false for container grants";
      validatedContainerOptions = Validation.invalid(List.of(err));
    } else if (playbookPatternOptions.future() && playbookPatternOptions.all()) {
      validatedContainerOptions =
          Validation.valid(
              ContainerPatternOptions.of(
                  ContainerPatternOption.ALL, ContainerPatternOption.FUTURE));
    } else if (playbookPatternOptions.all()) {
      validatedContainerOptions =
          Validation.valid(ContainerPatternOptions.of(ContainerPatternOption.ALL));
    } else {
      validatedContainerOptions =
          Validation.valid(ContainerPatternOptions.of(ContainerPatternOption.FUTURE));
    }

    Function<ContainerPatternOptions, Validation<Seq<String>, ResolvedPlaybookPattern.Container>>
        createContainerPattern =
            containerOptions ->
                switch (objectType.getQualLevel()) {
                  case 1 -> liftError(database(pattern))
                      .map(
                          databaseName ->
                              new ResolvedPlaybookPattern.Container.AccountObjectDatabase(
                                  databaseName, containerOptions));
                  case 2 -> database(pattern)
                      .combine(sch().emptyOrWildcard("schema").apply(pattern))
                      .ap(
                          (databaseName, unusedSchema) ->
                              new ResolvedPlaybookPattern.Container.AccountObjectDatabase(
                                  databaseName, containerOptions));
                  case 3 -> {
                    Validation<Seq<String>, ResolvedPlaybookPattern.Container>
                        objectLevelAllSchemasPattern =
                            database(pattern)
                                .combine(sch().emptyOrWildcard("schema").apply(pattern))
                                .combine(object(pattern))
                                .ap(
                                    (Function3<
                                            String,
                                            Void,
                                            String,
                                            ResolvedPlaybookPattern.Container>)
                                        (databaseName, unusedSchema, objectName) ->
                                            new ResolvedPlaybookPattern.Container
                                                .SchemaObjectAllSchemas(
                                                databaseName, objectName, containerOptions));

                    // Database-level container (empty/wildcard schema and empty/wildcard object)
                    Validation<Seq<String>, ResolvedPlaybookPattern.Container>
                        databaseLevelPattern =
                            database(pattern)
                                .combine(sch().emptyOrWildcard("schema").apply(pattern))
                                .combine(obj().emptyOrWildcard("object").apply(pattern))
                                .ap(
                                    (Function3<
                                            String, Void, Void, ResolvedPlaybookPattern.Container>)
                                        (databaseName, unusedSchema, unusedObject) ->
                                            new ResolvedPlaybookPattern.Container
                                                .AccountObjectDatabase(
                                                databaseName, containerOptions));

                    // Schema-level container (specific schema, empty/wildcard object)
                    Validation<Seq<String>, ResolvedPlaybookPattern.Container> schemaLevelPattern =
                        database(pattern)
                            .combine(schema(pattern))
                            .combine(obj().emptyOrWildcard("object").apply(pattern))
                            .ap(
                                (Function3<String, String, Void, ResolvedPlaybookPattern.Container>)
                                    (databaseName, schemaName, unusedObject) ->
                                        new ResolvedPlaybookPattern.Container.Schema(
                                            databaseName, schemaName, containerOptions));

                    yield objectLevelAllSchemasPattern
                        .orElse(databaseLevelPattern)
                        .orElse(schemaLevelPattern);
                  }
                  default -> Validation.invalid(
                      List.of(
                          String.format(
                              "Unknown qual level %s for container grant object",
                              objectType.getQualLevel())));
                };

    return validatedContainerOptions.flatMap(createContainerPattern);
  }
}
