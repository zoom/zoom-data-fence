package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern.factories;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.factories.PolicyTypeFactory;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationErr;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyTypeFactoryTest {

  @Test
  @DisplayName("Standard Target - Account Level (qualLevel 0)")
  void from_StandardAccountLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.none(), Option.none(), Option.none());

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.ACCOUNT, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.Global);
  }

  @Test
  @DisplayName("Standard Target - Database Level (qualLevel 1)")
  void from_StandardDatabaseLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.DATABASE, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.AccountObject);
  }

  @Test
  @DisplayName("Standard Target - Schema Level (qualLevel 2)")
  void from_StandardSchemaLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.SCHEMA, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.Schema);
  }

  @Test
  @DisplayName("Standard Target - Object Level (qualLevel 3)")
  void from_StandardObjectLevel() {
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.SchemaObject);
  }

  @Test
  @DisplayName("Container Target - Database Level (qualLevel 2 with wildcard schema)")
  void from_ContainerDatabaseLevel() {
    // FUTURE SCHEMAS IN DATABASE MY_DB (wildcard schema required for valid pattern)
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.some("*"), Option.none());

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.SCHEMA, new PolicyPatternOptions(true, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Container.AccountObject);
  }

  @Test
  @DisplayName("Container Target - Schema Level without wildcard yields InvalidContainerPolicyPattern (qualLevel 3)")
  void from_ContainerSchemaLevel() {
    // DB.SCH with empty object: no schema/object wildcard -> deprecated pattern, invalid
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isInvalid());
    assertTrue(
        result.getError().exists(err -> err instanceof ValidationErr.InvalidContainerPolicyPattern
            && err.message().contains("DB.SCH.* or DB.*.OBJ is expected for qual level 3 object")));
  }

  @Test
  @DisplayName("Container Target - empty schema and object without wildcard yields InvalidContainerPolicyPattern (qualLevel 3)")
  void from_ContainerSchemaObjectAllSchemas_whenEmptySchemaAndObject() {
    // DB with empty schema and object: no wildcard -> deprecated pattern, invalid
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isInvalid());
    assertTrue(
        result.getError().exists(err -> err instanceof ValidationErr.InvalidContainerPolicyPattern
            && err.message().contains("DB.SCH.* or DB.*.OBJ is expected for qual level 3 object")));
  }

  @Test
  @DisplayName("Container Target - Schema Level without wildcard yields InvalidContainerPolicyPattern (qualLevel 3 for FUTURE_AND_ALL)")
  void from_ContainerSchemaLevelForFutureAndAll() {
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationErr>, PolicyType> result =
        PolicyTypeFactory.createFrom(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isInvalid());
    assertTrue(
        result.getError().exists(err -> err instanceof ValidationErr.InvalidContainerPolicyPattern
            && err.message().contains("DB.SCH.* or DB.*.OBJ is expected for qual level 3 object")));
  }
}
