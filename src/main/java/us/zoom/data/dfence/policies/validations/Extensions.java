package us.zoom.data.dfence.policies.validations;

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Extensions {

  public static <E, A> Validation<Seq<E>, A> liftError(Validation<E, A> validation) {
    return validation.fold(err -> Validation.invalid(List.of(err)), Validation::valid);
  }

  public static <E, A, B> Validation<Seq<E>, B> liftAndCast(
      Validation<E, A> validation, Class<B> targetClass) {
    return validation.fold(
        err -> Validation.invalid(List.of(err)),
        value -> Validation.valid(targetClass.cast(value)));
  }
}
