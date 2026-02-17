package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.validations.PolicyPatternValidations;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

import static org.junit.jupiter.api.Assertions.*;

class PolicyPatternValidationsTest {

  @Test
  void validateStandardPattern_shouldReturnAccountObject_whenQualLevelIs0() {
    PolicyPattern pattern = new PolicyPattern(Option.none(), Option.none(), Option.none());

    PolicyPatternValidations validations =
        new PolicyPatternValidations(
            pattern, new PolicyPatternOptions(false, false), SnowflakeObjectType.ACCOUNT);
    Validation<Seq<ValidationError>, PolicyType.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(PolicyType.Standard.Global.class, result.get());
  }

  @Test
  void validateStandardPattern_shouldReturnDatabaseLevel_whenQualLevelIs1AndDatabase() {
    PolicyPattern pattern =
        new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    PolicyPatternValidations validations =
        new PolicyPatternValidations(
            pattern, new PolicyPatternOptions(false, false), SnowflakeObjectType.DATABASE);
    Validation<Seq<ValidationError>, PolicyType.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(PolicyType.Standard.AccountObject.class, result.get());
    assertEquals(
        "MY_DB",
        ((PolicyType.Standard.AccountObject) result.get()).objectName());
  }

  @Test
  void validateStandardPattern_shouldReturnDatabaseAndName_whenQualLevelIs2AndDatabaseAndSchemaName() {
    PolicyPattern pattern =
        new PolicyPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    PolicyPatternValidations validations =
        new PolicyPatternValidations(
            pattern, new PolicyPatternOptions(false, false), SnowflakeObjectType.SCHEMA);
    Validation<Seq<ValidationError>, PolicyType.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(PolicyType.Standard.DatabaseObject.class, result.get());
    PolicyType.Standard.DatabaseObject dbAndName =
        (PolicyType.Standard.DatabaseObject) result.get();
    assertEquals("MY_DB", dbAndName.databaseName());
    assertEquals("MY_SCHEMA", dbAndName.name());
  }

  @Test
  void validateStandardPattern_shouldReturnDatabaseAndName_whenQualLevelIs2AndDatabaseAndObjectName() {
    PolicyPattern pattern =
        new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.some("MY_DATABASE_ROLE"));

    PolicyPatternValidations validations =
        new PolicyPatternValidations(
            pattern, new PolicyPatternOptions(false, false), SnowflakeObjectType.DATABASE_ROLE);
    Validation<Seq<ValidationError>, PolicyType.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(PolicyType.Standard.DatabaseObject.class, result.get());
    PolicyType.Standard.DatabaseObject dbAndName =
        (PolicyType.Standard.DatabaseObject) result.get();
    assertEquals("MY_DB", dbAndName.databaseName());
    assertEquals("MY_DATABASE_ROLE", dbAndName.name());
    assertEquals("MY_DB.MY_DATABASE_ROLE", dbAndName.qualifiedObjectName());
  }

  @Test
  void validateStandardPattern_shouldReturnObjectLevel_whenQualLevelIs3() {
    PolicyPattern pattern =
        new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    PolicyPatternValidations validations =
        new PolicyPatternValidations(
            pattern, new PolicyPatternOptions(false, false), SnowflakeObjectType.TABLE);
    Validation<Seq<ValidationError>, PolicyType.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(PolicyType.Standard.SchemaObject.class, result.get());
    PolicyType.Standard.SchemaObject schemaObject =
        (PolicyType.Standard.SchemaObject) result.get();
    assertEquals("MY_DB", schemaObject.databaseName());
    assertEquals("MY_SCHEMA", schemaObject.schemaName());
    assertEquals("MY_TABLE", schemaObject.objectName());
  }

  @Test
  void validateContainerPattern_shouldReturnInvalid_whenGrantTypeIsStandard() {
    PolicyPattern pattern =
        new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    PolicyPatternValidations validations =
        new PolicyPatternValidations(
            pattern, new PolicyPatternOptions(false, false), SnowflakeObjectType.TABLE);
    Validation<Seq<ValidationError>, PolicyType.Container> result =
        validations.validateContainerPattern();
    assertTrue(result.isInvalid());
  }
}
