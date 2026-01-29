package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern.companions;

import com.google.common.collect.ImmutableList;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.companions.ResolvedPlaybookPatternCompanion;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.shared.models.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPatternOptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolvedPlaybookPatternCompanionTest {

  @Test
  @DisplayName("Standard Target - Account Level (qualLevel 0)")
  void from_StandardAccountLevel() {
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.ACCOUNT,
            Option.none(),
            Option.none(),
            Option.none(),
            new ResolvedPlaybookPattern.Standard.Global());

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Standard.Global);
  }

  @Test
  @DisplayName("Standard Target - Database Level (qualLevel 1)")
  void from_StandardDatabaseLevel() {
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.DATABASE,
            Option.some("MY_DB"),
            Option.none(),
            Option.none(),
            new ResolvedPlaybookPattern.Standard.AccountObjectDatabase("MY_DB"));

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Standard.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Standard Target - Schema Level (qualLevel 2)")
  void from_StandardSchemaLevel() {
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.SCHEMA,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.none(),
            new ResolvedPlaybookPattern.Standard.Schema("MY_DB", "MY_SCHEMA"));

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Standard.Schema);
  }

  @Test
  @DisplayName("Standard Target - Object Level (qualLevel 3)")
  void from_StandardObjectLevel() {
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.some("MY_TABLE"),
            new ResolvedPlaybookPattern.Standard.SchemaObject("MY_DB", "MY_SCHEMA", "MY_TABLE"));

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Standard.SchemaObject);
  }

  @Test
  @DisplayName("Container Target - Database Level (qualLevel 2 with wildcard schema)")
  void from_ContainerDatabaseLevel() {
    // FUTURE SCHEMAS IN DATABASE MY_DB
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.SCHEMA,
            Option.some("MY_DB"),
            Option.none(), // Missing/wildcard schema implies database level for container
            Option.none(),
            new ResolvedPlaybookPattern.Container.AccountObjectDatabase(
                "MY_DB", ContainerPatternOptions.of(ContainerPatternOption.FUTURE)));

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(true, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Container.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Container Target - Schema Level (qualLevel 3)")
  void from_ContainerSchemaLevel() {
    // ALL TABLES IN SCHEMA MY_DB.MY_SCHEMA
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.none(),
            new ResolvedPlaybookPattern.Container.Schema(
                "MY_DB", "MY_SCHEMA", ContainerPatternOptions.of(ContainerPatternOption.ALL)));

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Container.Schema);
  }

  @Test
  @DisplayName("Container Target - Database Level (qualLevel 3 with wildcard schema)")
  void from_ContainerDatabaseLevelForAll() {
    // ALL TABLES IN DATABASE MY_DB
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.none(),
            Option.none(),
            new ResolvedPlaybookPattern.Container.Schema(
                "MY_DB", "MY_SCHEMA", ContainerPatternOptions.of(ContainerPatternOption.ALL)));

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Container.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Container Target - Schema Level (qualLevel 3 for FUTURE_AND_ALL)")
  void from_ContainerSchemaLevelForFutureAndAll() {
    PlaybookGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.none(),
            new ResolvedPlaybookPattern.Container.Schema(
                "MY_DB", "MY_SCHEMA", ContainerPatternOptions.of(ContainerPatternOption.ALL)));

    Validation<Seq<ValidationError>, ResolvedPlaybookPattern> result =
        ResolvedPlaybookPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PlaybookPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPlaybookPattern.Container.Schema);
  }

  private PlaybookGrant createGrant(
      SnowflakeObjectType objectType,
      Option<String> dbName,
      Option<String> schName,
      Option<String> objName,
      ResolvedPlaybookPattern patternType) {
    return new PlaybookGrant(
        objectType,
        new PlaybookPattern(dbName, schName, objName),
        ImmutableList.of(new GrantPrivilege("SELECT")),
        patternType,
        true);
  }
}
