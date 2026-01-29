package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.BaseValidations;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ValidationError;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookPattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseValidationsTest {

  @Test
  void liftError_shouldConvertSingleErrorToSequence() {
    Validation<ValidationError, String> validation = Validation.invalid(ValidationError.of("Error message"));

    Validation<Seq<ValidationError>, String> result = BaseValidations.liftError(validation);

    assertTrue(result.isInvalid());
    assertEquals(1, result.getError().size());
    assertEquals("Error message", result.getError().head().message());
  }

  @Test
  void liftError_shouldPreserveValidValue() {
    Validation<ValidationError, String> validation = Validation.valid("Success");

    Validation<Seq<ValidationError>, String> result = BaseValidations.liftError(validation);

    assertTrue(result.isValid());
    assertEquals("Success", result.get());
  }

  @Test
  void database_shouldReturnValidValue_whenDatabaseNameIsPresent() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<ValidationError, String> result = BaseValidations.database(pattern);

    assertTrue(result.isValid());
    assertEquals("MY_DB", result.get());
  }

  @Test
  void database_shouldReturnInvalid_whenDatabaseNameIsEmpty() {
    PlaybookPattern pattern = new PlaybookPattern(Option.none(), Option.none(), Option.none());

    Validation<ValidationError, String> result = BaseValidations.database(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().message().contains("database is empty"));
  }

  @Test
  void database_shouldReturnInvalid_whenDatabaseNameIsWildcard() {
    PlaybookPattern pattern = new PlaybookPattern(Option.some("*"), Option.none(), Option.none());

    Validation<ValidationError, String> result = BaseValidations.database(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().message().contains("non-empty and non-wildcard value is expected"));
  }

  @Test
  void schema_shouldReturnValidValue_whenSchemaNameIsPresent() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<ValidationError, String> result = BaseValidations.schema(pattern);

    assertTrue(result.isValid());
    assertEquals("MY_SCHEMA", result.get());
  }

  @Test
  void schema_shouldReturnInvalid_whenSchemaNameIsEmpty() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<ValidationError, String> result = BaseValidations.schema(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().message().contains("schema is empty"));
  }

  @Test
  void object_shouldReturnValidValue_whenObjectNameIsPresent() {
    PlaybookPattern pattern =
        new PlaybookPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    Validation<ValidationError, String> result = BaseValidations.object(pattern);

    assertTrue(result.isValid());
    assertEquals("MY_TABLE", result.get());
  }

  @Test
  void object_shouldReturnInvalid_whenObjectNameIsEmpty() {
    PlaybookPattern pattern =
        new PlaybookPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<ValidationError, String> result = BaseValidations.object(pattern);

    assertTrue(result.isInvalid());
    assertTrue(result.getError().message().contains("object is empty"));
  }
}
