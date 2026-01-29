package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.policies.policies.PolicyPatternValidations;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.policies.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyPattern;
import us.zoom.data.dfence.providers.snowflake.policies.models.PolicyPatternOptions;

import static org.junit.jupiter.api.Assertions.*;

class PlaybookPatternValidationsTest {

  @Test
  void validateStandardPattern_shouldReturnAccountObject_whenQualLevelIs0() {
    PolicyPattern pattern = new PolicyPattern(Option.none(), Option.none(), Option.none());

    PolicyPatternValidations validations =
        new PolicyPatternValidations(pattern, SnowflakeObjectType.ACCOUNT);
    Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.Global.class, result.get());
  }

  @Test
  void validateStandardPattern_shouldReturnDatabaseLevel_whenQualLevelIs1AndDatabase() {
    PolicyPattern pattern =
        new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    PolicyPatternValidations validations =
        new PolicyPatternValidations(pattern, SnowflakeObjectType.DATABASE);
    Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.AccountObjectDatabase.class, result.get());
    assertEquals(
        "MY_DB",
        ((ResolvedPlaybookPattern.Standard.AccountObjectDatabase) result.get()).databaseName());
  }

  @Test
  void validateStandardPattern_shouldReturnSchemaLevel_whenQualLevelIs2() {
    PolicyPattern pattern =
        new PolicyPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    PolicyPatternValidations validations =
        new PolicyPatternValidations(pattern, SnowflakeObjectType.SCHEMA);
    Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.Schema.class, result.get());
    ResolvedPlaybookPattern.Standard.Schema schema =
        (ResolvedPlaybookPattern.Standard.Schema) result.get();
    assertEquals("MY_DB", schema.databaseName());
    assertEquals("MY_SCHEMA", schema.schemaName());
  }

  @Test
  void validateStandardPattern_shouldReturnObjectLevel_whenQualLevelIs3() {
    PolicyPattern pattern =
        new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    PolicyPatternValidations validations =
        new PolicyPatternValidations(pattern, SnowflakeObjectType.TABLE);
    Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.SchemaObject.class, result.get());
    ResolvedPlaybookPattern.Standard.SchemaObject schemaObject =
        (ResolvedPlaybookPattern.Standard.SchemaObject) result.get();
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
        new PolicyPatternValidations(pattern, SnowflakeObjectType.TABLE);
    Validation<Seq<ValidationError>, ResolvedPlaybookPattern.Container> result =
        validations.validateContainerPattern(new PolicyPatternOptions(false, false));
    assertTrue(result.isInvalid());
  }
}
