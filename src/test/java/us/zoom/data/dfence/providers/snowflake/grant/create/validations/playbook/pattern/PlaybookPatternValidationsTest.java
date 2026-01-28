package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.PlaybookPatternValidations;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPatternOptions;

import static org.junit.jupiter.api.Assertions.*;

class PlaybookPatternValidationsTest {

  @Test
  void validateStandardPattern_shouldReturnAccountObject_whenQualLevelIs0() {
    PlaybookPattern pattern = new PlaybookPattern(Option.none(), Option.none(), Option.none());

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.ACCOUNT);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.Global.class, result.get());
  }

  @Test
  void validateStandardPattern_shouldReturnDatabaseLevel_whenQualLevelIs1AndDatabase() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.none(), Option.none());

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.DATABASE);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
        validations.validateStandardPattern();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.AccountObjectDatabase.class, result.get());
    assertEquals(
        "MY_DB",
        ((ResolvedPlaybookPattern.Standard.AccountObjectDatabase) result.get()).databaseName());
  }

  @Test
  void validateStandardPattern_shouldReturnSchemaLevel_whenQualLevelIs2() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.SCHEMA);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
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
    PlaybookPattern pattern =
        new PlaybookPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.TABLE);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
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
    PlaybookPattern pattern =
        new PlaybookPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.TABLE);
    Validation<Seq<String>, ResolvedPlaybookPattern.Container> result =
        validations.validateContainerPattern(new PlaybookPatternOptions(false, false));
    assertTrue(result.isInvalid());
  }
}
