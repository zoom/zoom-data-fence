package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern.companions;

import com.google.common.collect.ImmutableList;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.companions.ResolvedPolicyPatternCompanion;
import us.zoom.data.dfence.policies.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.policies.pattern.models.ResolvedPolicyPattern;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolvedPlaybookPatternCompanionTest {

  @Test
  @DisplayName("Standard Target - Account Level (qualLevel 0)")
  void from_StandardAccountLevel() {
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.ACCOUNT,
            Option.none(),
            Option.none(),
            Option.none(),
            new ResolvedPolicyPattern.Standard.Global());

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Standard.Global);
  }

  @Test
  @DisplayName("Standard Target - Database Level (qualLevel 1)")
  void from_StandardDatabaseLevel() {
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.DATABASE,
            Option.some("MY_DB"),
            Option.none(),
            Option.none(),
            new ResolvedPolicyPattern.Standard.AccountObjectDatabase("MY_DB"));

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Standard.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Standard Target - Schema Level (qualLevel 2)")
  void from_StandardSchemaLevel() {
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.SCHEMA,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.none(),
            new ResolvedPolicyPattern.Standard.Schema("MY_DB", "MY_SCHEMA"));

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Standard.Schema);
  }

  @Test
  @DisplayName("Standard Target - Object Level (qualLevel 3)")
  void from_StandardObjectLevel() {
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.some("MY_TABLE"),
            new ResolvedPolicyPattern.Standard.SchemaObject("MY_DB", "MY_SCHEMA", "MY_TABLE"));

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Standard.SchemaObject);
  }

  @Test
  @DisplayName("Container Target - Database Level (qualLevel 2 with wildcard schema)")
  void from_ContainerDatabaseLevel() {
    // FUTURE SCHEMAS IN DATABASE MY_DB
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.SCHEMA,
            Option.some("MY_DB"),
            Option.none(), // Missing/wildcard schema implies database level for container
            Option.none(),
            new ResolvedPolicyPattern.Container.AccountObjectDatabase(
                "MY_DB", ContainerPatternOptions.of(ContainerPatternOption.FUTURE)));

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(true, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Container.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Container Target - Schema Level (qualLevel 3)")
  void from_ContainerSchemaLevel() {
    // ALL TABLES IN SCHEMA MY_DB.MY_SCHEMA
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.none(),
            new ResolvedPolicyPattern.Container.Schema(
                "MY_DB", "MY_SCHEMA", ContainerPatternOptions.of(ContainerPatternOption.ALL)));

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Container.Schema);
  }

  @Test
  @DisplayName("Container Target - Database Level (qualLevel 3 with wildcard schema)")
  void from_ContainerDatabaseLevelForAll() {
    // ALL TABLES IN DATABASE MY_DB
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.none(),
            Option.none(),
            new ResolvedPolicyPattern.Container.Schema(
                "MY_DB", "MY_SCHEMA", ContainerPatternOptions.of(ContainerPatternOption.ALL)));

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Container.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Container Target - Schema Level (qualLevel 3 for FUTURE_AND_ALL)")
  void from_ContainerSchemaLevelForFutureAndAll() {
    PolicyGrant grant =
        createGrant(
            SnowflakeObjectType.TABLE,
            Option.some("MY_DB"),
            Option.some("MY_SCHEMA"),
            Option.none(),
            new ResolvedPolicyPattern.Container.Schema(
                "MY_DB", "MY_SCHEMA", ContainerPatternOptions.of(ContainerPatternOption.ALL)));

    Validation<Seq<ValidationError>, ResolvedPolicyPattern> result =
        ResolvedPolicyPatternCompanion.from(
            grant.pattern(), grant.objectType(), new PolicyPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof ResolvedPolicyPattern.Container.Schema);
  }

  private PolicyGrant createGrant(
      SnowflakeObjectType objectType,
      Option<String> dbName,
      Option<String> schName,
      Option<String> objName,
      ResolvedPolicyPattern patternType) {
    return new PolicyGrant(
        objectType,
        new PolicyPattern(dbName, schName, objName),
        ImmutableList.of(new PolicyGrantPrivilege("SELECT")),
        patternType,
        true);
  }
}
