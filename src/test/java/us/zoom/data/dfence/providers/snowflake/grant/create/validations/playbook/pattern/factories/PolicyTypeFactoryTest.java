package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern.factories;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.factories.PolicyTypeFactory;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyTypeFactoryTest {

  @Test
  @DisplayName("Standard pattern - Account level (qualLevel 0)")
  void from_StandardAccountLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.none(), Option.none(), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.ACCOUNT, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.Global);
  }

  @Test
  @DisplayName("Standard pattern - Database level (qualLevel 1)")
  void from_StandardDatabaseLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.DATABASE, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.AccountObject);
  }

  @Test
  @DisplayName("Standard pattern - Schema level (qualLevel 2)")
  void from_StandardSchemaLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.SCHEMA, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.Schema);
  }

  @Test
  @DisplayName("Standard pattern - Object level (qualLevel 3)")
  void from_StandardObjectLevel() {
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.SchemaObject);
  }

  @Test
  @DisplayName("Container pattern - Database level (qualLevel 2 with wildcard schema)")
  void from_ContainerDatabaseLevel() {
    // FUTURE SCHEMAS IN DATABASE MY_DB (wildcard schema required for valid pattern)
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.some("*"), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.SCHEMA, new PolicyPatternOptions(true, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Container.AccountObject);
  }

  @Test
  @DisplayName("Container pattern - Schema level without wildcard yields InvalidContainerPolicyPattern (qualLevel 3)")
  void from_ContainerSchemaLevel() {
    // DB.SCH with empty object: no schema/object wildcard -> deprecated pattern, invalid
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isInvalid());
    assertTrue(
        result.getError().exists(err -> err instanceof ValidationError.InvalidContainerPolicyPattern
            && err.message().contains("DB.SCH.* or DB.*.OBJ or DB.*.* is expected for qual level 3 object")));
  }

  @Test
  @DisplayName("Container pattern - empty schema and object without wildcard yields InvalidContainerPolicyPattern (qualLevel 3)")
  void from_ContainerQual3_emptySchemaAndObjectNoWildcard_yieldsInvalidContainerPolicyPattern() {
    // DB with empty schema and object: no wildcard -> deprecated pattern, invalid
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isInvalid());
    assertTrue(
        result.getError().exists(err -> err instanceof ValidationError.InvalidContainerPolicyPattern
            && err.message().contains("DB.SCH.* or DB.*.OBJ or DB.*.* is expected for qual level 3 object")));
  }

  @Test
  @DisplayName("Container pattern - Schema level with includeAll, no wildcard yields InvalidContainerPolicyPattern (qualLevel 3)")
  void from_ContainerSchemaLevel_withIncludeAll_yieldsInvalidContainerPolicyPattern() {
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isInvalid());
    assertTrue(
        result.getError().exists(err -> err instanceof ValidationError.InvalidContainerPolicyPattern
            && err.message().contains("DB.SCH.* or DB.*.OBJ or DB.*.* is expected for qual level 3 object")));
  }
}
