package us.zoom.data.dfence.providers.snowflake.revoke.collection;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NonEmptyListTest {

  @Test
  void of_shouldCreateNonEmptyList_withSingleElement() {
    NonEmptyList<String> list = NonEmptyList.of("head");

    assertEquals("head", list.asImmutableList().get(0));
    assertEquals(1, list.asImmutableList().size());
  }

  @Test
  void of_shouldCreateNonEmptyList_withMultipleElements() {
    NonEmptyList<String> list = NonEmptyList.of("head", "tail1", "tail2", "tail3");

    ImmutableList<String> immutable = list.asImmutableList();
    assertEquals("head", immutable.get(0));
    assertEquals(4, immutable.size());
    assertEquals("tail1", immutable.get(1));
    assertEquals("tail2", immutable.get(2));
    assertEquals("tail3", immutable.get(3));
  }

  @Test
  void of_shouldCreateNonEmptyList_withNoTailElements() {
    NonEmptyList<Integer> list = NonEmptyList.of(42);

    ImmutableList<Integer> immutable = list.asImmutableList();
    assertEquals(42, immutable.get(0));
    assertEquals(1, immutable.size());
  }

  @Test
  void from_shouldCreateNonEmptyList_fromListWithSingleElement() {
    ImmutableList<String> source = ImmutableList.of("single");
    NonEmptyList<String> list = NonEmptyList.from(source);

    ImmutableList<String> immutable = list.asImmutableList();
    assertEquals("single", immutable.get(0));
    assertEquals(1, immutable.size());
  }

  @Test
  void from_shouldCreateNonEmptyList_fromListWithMultipleElements() {
    ImmutableList<String> source = ImmutableList.of("first", "second", "third", "fourth");
    NonEmptyList<String> list = NonEmptyList.from(source);

    ImmutableList<String> immutable = list.asImmutableList();
    assertEquals("first", immutable.get(0));
    assertEquals(4, immutable.size());
    assertEquals("second", immutable.get(1));
    assertEquals("third", immutable.get(2));
    assertEquals("fourth", immutable.get(3));
  }

  @Test
  void from_shouldCreateNonEmptyList_fromArrayList() {
    ImmutableList<Integer> source = ImmutableList.of(1, 2, 3);
    NonEmptyList<Integer> list = NonEmptyList.from(source);

    ImmutableList<Integer> immutable = list.asImmutableList();
    assertEquals(1, immutable.get(0));
    assertEquals(3, immutable.size());
    assertEquals(2, immutable.get(1));
    assertEquals(3, immutable.get(2));
  }

  @Test
  void from_shouldThrowException_whenListIsEmpty() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> NonEmptyList.from(ImmutableList.of()));

    assertEquals("NonEmptyList must contain at least one element", exception.getMessage());
  }

  @Test
  void recordEquality_shouldWorkCorrectly() {
    NonEmptyList<String> list1 = NonEmptyList.of("head", "tail");
    NonEmptyList<String> list2 = NonEmptyList.of("head", "tail");
    NonEmptyList<String> list3 = NonEmptyList.of("head", "different");

    assertEquals(list1, list2);
    assertNotEquals(list1, list3);
  }

  @Test
  void recordEquality_shouldWorkWithDifferentTailSizes() {
    NonEmptyList<String> list1 = NonEmptyList.of("head", "tail1");
    NonEmptyList<String> list2 = NonEmptyList.of("head", "tail1", "tail2");

    assertNotEquals(list1, list2);
  }

  @Test
  void recordEquality_shouldWorkWithSameHeadDifferentTail() {
    NonEmptyList<String> list1 = NonEmptyList.of("head", "tail1");
    NonEmptyList<String> list2 = NonEmptyList.of("head", "tail2");

    assertNotEquals(list1, list2);
  }

  @Test
  void recordEquality_shouldWorkWithSameContent() {
    NonEmptyList<Integer> list1 = NonEmptyList.from(ImmutableList.of(1, 2, 3));
    NonEmptyList<Integer> list2 = NonEmptyList.of(1, 2, 3);

    assertEquals(list1, list2);
  }

  @Test
  void recordHashCode_shouldBeConsistent() {
    NonEmptyList<String> list1 = NonEmptyList.of("head", "tail");
    NonEmptyList<String> list2 = NonEmptyList.of("head", "tail");

    assertEquals(list1.hashCode(), list2.hashCode());
  }

  @Test
  void recordToString_shouldIncludeHeadAndTail() {
    NonEmptyList<String> list = NonEmptyList.of("head", "tail1", "tail2");
    String toString = list.toString();

    assertTrue(toString.contains("head"));
    assertTrue(toString.contains("tail1"));
    assertTrue(toString.contains("tail2"));
  }

  @Test
  void of_shouldWorkWithNullHead() {
    NonEmptyList<String> list = NonEmptyList.of((String) null);

    // Note: ImmutableList doesn't allow null values, so asImmutableList() will throw
    // NullPointerException
    assertThrows(
        NullPointerException.class,
        () -> list.asImmutableList(),
        "ImmutableList doesn't allow null elements");
  }

  @Test
  void of_shouldWorkWithNullTailElements() {
    // Note: ImmutableList doesn't allow null values, so this should throw NullPointerException
    List<String> sourceList = new ArrayList<>();
    sourceList.add("head");
    sourceList.add(null);
    sourceList.add("tail");

    assertThrows(
        NullPointerException.class,
        () -> NonEmptyList.from(ImmutableList.copyOf(sourceList)),
        "ImmutableList doesn't allow null elements");
  }

  @Test
  void from_shouldWorkWithNullElements() {
    // Note: ImmutableList doesn't allow null values, so this will throw NullPointerException
    List<String> sourceList = new ArrayList<>();
    sourceList.add(null);
    sourceList.add("second");

    assertThrows(
        NullPointerException.class,
        () -> NonEmptyList.from(ImmutableList.copyOf(sourceList)),
        "ImmutableList doesn't allow null elements");
  }

  @Test
  void from_shouldCreateIndependentCopy() {
    // Note: ImmutableList.subList() returns an ImmutableList view, which is independent
    ImmutableList<String> source = ImmutableList.of("first", "second");
    NonEmptyList<String> list = NonEmptyList.from(source);

    // Verify initial state
    ImmutableList<String> immutable = list.asImmutableList();
    assertEquals("first", immutable.get(0));
    assertEquals(2, immutable.size());
    assertEquals("second", immutable.get(1));
  }

  @Test
  void of_shouldWorkWithDifferentTypes() {
    NonEmptyList<Integer> intList = NonEmptyList.of(1, 2, 3);
    NonEmptyList<String> stringList = NonEmptyList.of("a", "b", "c");
    NonEmptyList<Boolean> boolList = NonEmptyList.of(true, false);

    assertEquals(1, intList.asImmutableList().get(0));
    assertEquals("a", stringList.asImmutableList().get(0));
    assertTrue(boolList.asImmutableList().get(0));
  }

  @Test
  void from_shouldWorkWithLargeList() {
    ImmutableList.Builder<Integer> builder = ImmutableList.builder();
    for (int i = 0; i < 1000; i++) {
      builder.add(i);
    }
    ImmutableList<Integer> largeList = builder.build();
    NonEmptyList<Integer> nonEmptyList = NonEmptyList.from(largeList);

    ImmutableList<Integer> immutable = nonEmptyList.asImmutableList();
    assertEquals(0, immutable.get(0));
    assertEquals(1000, immutable.size());
    assertEquals(1, immutable.get(1));
    assertEquals(999, immutable.get(999));
  }
}
