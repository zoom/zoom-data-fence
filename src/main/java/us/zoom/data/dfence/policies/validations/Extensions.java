package us.zoom.data.dfence.policies.validations;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Static utilities for Validation: lift error to Seq, map success and lift error. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Extensions {

  /**
   * Converts Validation<E, A> to Validation<Seq<E>, A> by wrapping the error in a single-element
   * list.
   */
  public static <E, A> Validation<Seq<E>, A> liftError(Validation<E, A> validation) {
    return validation.fold(err -> Validation.invalid(List.of(err)), Validation::valid);
  }

  /**
   * Folds the validation with a cast on the valid branch. Equivalent to fold(validation,
   * targetClass::cast).
   */
  public static <E, A, B> Validation<Seq<E>, B> fold(
      Validation<E, A> validation, Class<B> targetClass) {
    return validation.fold(
        err -> Validation.invalid(List.of(err)),
        value -> Validation.valid(targetClass.cast(value)));
  }
}
