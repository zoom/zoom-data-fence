package us.zoom.data.dfence.policies.validations;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Static helpers for {@link io.vavr.control.Validation}: lift error to Seq, lift and cast. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Extensions {

  /** Converts Validation&lt;E, A&gt; to Validation&lt;Seq&lt;E&gt;, A&gt; by wrapping the error in a list. */
  public static <E, A> Validation<Seq<E>, A> liftError(Validation<E, A> validation) {
    return validation.fold(err -> Validation.invalid(List.of(err)), Validation::valid);
  }

  /** Lifts a single error to Seq and casts the valid value to {@code targetClass}. */
  public static <E, A, B> Validation<Seq<E>, B> liftAndCast(
      Validation<E, A> validation, Class<B> targetClass) {
    return validation.fold(
        err -> Validation.invalid(List.of(err)),
        value -> Validation.valid(targetClass.cast(value)));
  }
}
