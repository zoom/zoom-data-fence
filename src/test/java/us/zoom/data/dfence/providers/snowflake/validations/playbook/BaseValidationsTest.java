package us.zoom.data.dfence.providers.snowflake.validations.playbook;

import static org.junit.jupiter.api.Assertions.*;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Test;

class BaseValidationsTest {

  @Test
  void liftError_shouldConvertSingleErrorToSequence() {
    Validation<String, String> validation = Validation.invalid("Error message");

    Validation<Seq<String>, String> result = BaseValidations.liftError(validation);

    assertTrue(result.isInvalid());
    assertEquals(1, result.getError().size());
    assertEquals("Error message", result.getError().head());
  }

  @Test
  void liftError_shouldPreserveValidValue() {
    Validation<String, String> validation = Validation.valid("Success");

    Validation<Seq<String>, String> result = BaseValidations.liftError(validation);

    assertTrue(result.isValid());
    assertEquals("Success", result.get());
  }
}
