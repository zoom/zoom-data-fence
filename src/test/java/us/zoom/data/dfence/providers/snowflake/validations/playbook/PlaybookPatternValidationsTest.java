package us.zoom.data.dfence.providers.snowflake.validations.playbook;

import static org.junit.jupiter.api.Assertions.*;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPatternOptions;
import us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models.ResolvedPlaybookPattern;

class PlaybookPatternValidationsTest {

  @Test
  void database_shouldReturnValidValue_whenDatabaseNameIsPresent() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<String, String> result = BaseValidations.database(pattern);

    assertTrue(result.isValid());
    assertEquals("MY_DB", result.get());
  }

  @Test
  void database_shouldReturnInvalid_whenDatabaseNameIsEmpty() {
    PlaybookPattern pattern = new PlaybookPattern(Option.none(), Option.none(), Option.none());

    Validation<String, String> result = BaseValidations.database(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().contains("database is empty"));
  }

  @Test
  void database_shouldReturnInvalid_whenDatabaseNameIsWildcard() {
    PlaybookPattern pattern = new PlaybookPattern(Option.some("*"), Option.none(), Option.none());

    Validation<String, String> result = BaseValidations.database(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().contains("non-empty and non-wildcard value is expected"));
  }

  @Test
  void schema_shouldReturnValidValue_whenSchemaNameIsPresent() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<String, String> result = BaseValidations.schema(pattern);

    assertTrue(result.isValid());
    assertEquals("MY_SCHEMA", result.get());
  }

  @Test
  void schema_shouldReturnInvalid_whenSchemaNameIsEmpty() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<String, String> result = BaseValidations.schema(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().contains("schema is empty"));
  }

  @Test
  void object_shouldReturnValidValue_whenObjectNameIsPresent() {
    PlaybookPattern pattern =
        new PlaybookPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    Validation<String, String> result = BaseValidations.object(pattern);

    assertTrue(result.isValid());
    assertEquals("MY_TABLE", result.get());
  }

  @Test
  void object_shouldReturnInvalid_whenObjectNameIsEmpty() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<String, String> result = BaseValidations.object(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().contains("object is empty"));
  }

  @Test
  void toStandardTarget_shouldReturnAccountObject_whenQualLevelIs0() {
    PlaybookPattern pattern = new PlaybookPattern(Option.none(), Option.none(), Option.none());

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.ACCOUNT);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
        validations.toStandardTarget();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.Global.class, result.get());
  }

  @Test
  void toStandardTarget_shouldReturnDatabaseLevel_whenQualLevelIs1AndDatabase() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.none(), Option.none());

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.DATABASE);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
        validations.toStandardTarget();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.AccountObjectDatabase.class, result.get());
    assertEquals(
        "MY_DB",
        ((ResolvedPlaybookPattern.Standard.AccountObjectDatabase) result.get()).databaseName());
  }

  @Test
  void toStandardTarget_shouldReturnSchemaLevel_whenQualLevelIs2() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.SCHEMA);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
        validations.toStandardTarget();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.Schema.class, result.get());
    ResolvedPlaybookPattern.Standard.Schema schema =
        (ResolvedPlaybookPattern.Standard.Schema) result.get();
    assertEquals("MY_DB", schema.databaseName());
    assertEquals("MY_SCHEMA", schema.schemaName());
  }

  @Test
  void toStandardTarget_shouldReturnObjectLevel_whenQualLevelIs3() {
    PlaybookPattern pattern =
        new PlaybookPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.TABLE);
    Validation<Seq<String>, ResolvedPlaybookPattern.Standard> result =
        validations.toStandardTarget();

    assertTrue(result.isValid());
    assertInstanceOf(ResolvedPlaybookPattern.Standard.SchemaObject.class, result.get());
    ResolvedPlaybookPattern.Standard.SchemaObject schemaObject =
        (ResolvedPlaybookPattern.Standard.SchemaObject) result.get();
    assertEquals("MY_DB", schemaObject.databaseName());
    assertEquals("MY_SCHEMA", schemaObject.schemaName());
    assertEquals("MY_TABLE", schemaObject.objectName());
  }

  @Test
  void toContainerTarget_shouldReturnInvalid_whenGrantTypeIsStandard() {
    PlaybookPattern pattern =
        new PlaybookPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    PlaybookPatternValidations validations =
        new PlaybookPatternValidations(pattern, SnowflakeObjectType.TABLE);
    Validation<Seq<String>, ResolvedPlaybookPattern.Container> result =
        validations.toContainerTarget(PlaybookPatternOptions.STANDARD);
    assertTrue(result.isInvalid());
  }
}
