package us.zoom.data.dfence.providers.snowflake.revoke.collection;

import com.google.common.collect.ImmutableList;

public record NonEmptyList<E>(E head, ImmutableList<E> tail) {

  @SafeVarargs
  public static <E> NonEmptyList<E> of(E head, E... tail) {
    return new NonEmptyList<>(head, ImmutableList.copyOf(tail));
  }

  public static <E> NonEmptyList<E> from(ImmutableList<E> values) {
    if (values.isEmpty())
      throw new IllegalArgumentException("NonEmptyList must contain at least one element");
    ImmutableList<E> tailList = values.subList(1, values.size());
    return new NonEmptyList<>(values.get(0), tailList);
  }

  public ImmutableList<E> asImmutableList() {
    return ImmutableList.<E>builder().add(head()).addAll(tail()).build();
  }
}
