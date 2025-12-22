package us.zoom.data.dfence.extensions

import munit.FunSuite
import scala.collection.mutable

class MapExtensionsTest extends FunSuite:

  test("mergeHashMaps should merge when map1 is smaller") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(4),
      "key2" -> mutable.HashSet(5, 6),
      "key3" -> mutable.HashSet(7),
      "key4" -> mutable.HashSet(8)
    )

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 4)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2, 4))
    assertEquals(actualResult("key2"), mutable.HashSet(3, 5, 6))
    assertEquals(actualResult("key3"), mutable.HashSet(7))
    assertEquals(actualResult("key4"), mutable.HashSet(8))
    // Should return map2 (the larger map)
    assert(actualResult eq map2, "Should return the larger map (map2)")
  }

  test("mergeHashMaps should merge when map2 is smaller") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3),
      "key3" -> mutable.HashSet(4),
      "key4" -> mutable.HashSet(5)
    )
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(6),
      "key2" -> mutable.HashSet(7, 8)
    )

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 4)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2, 6))
    assertEquals(actualResult("key2"), mutable.HashSet(3, 7, 8))
    assertEquals(actualResult("key3"), mutable.HashSet(4))
    assertEquals(actualResult("key4"), mutable.HashSet(5))
    // Should return map1 (the larger map)
    assert(actualResult eq map1, "Should return the larger map (map1)")
  }

  test("mergeHashMaps should merge when maps have equal size") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(4),
      "key2" -> mutable.HashSet(5, 6)
    )

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 2)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2, 4))
    assertEquals(actualResult("key2"), mutable.HashSet(3, 5, 6))
    // When equal size, should return map2 (map2.mergeTo(map1))
    assert(actualResult eq map1, "Should return map1 when sizes are equal")
  }

  test("mergeHashMaps should handle empty map1") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]]()
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 2)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2))
    assertEquals(actualResult("key2"), mutable.HashSet(3))
    assert(actualResult eq map2, "Should return map2 when map1 is empty")
  }

  test("mergeHashMaps should handle empty map2") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]]()

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 2)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2))
    assertEquals(actualResult("key2"), mutable.HashSet(3))
    assert(actualResult eq map1, "Should return map1 when map2 is empty")
  }

  test("mergeHashMaps should handle both maps empty") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]]()
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]]()

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 0)
    // When equal size, returns map1 (map2.mergeTo(map1) returns map1)
    assert(actualResult eq map1, "Should return map1 when both are empty and equal size")
  }

  test("mergeHashMaps should merge disjoint keys") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key3" -> mutable.HashSet(4),
      "key4" -> mutable.HashSet(5, 6)
    )

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 4)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2))
    assertEquals(actualResult("key2"), mutable.HashSet(3))
    assertEquals(actualResult("key3"), mutable.HashSet(4))
    assertEquals(actualResult("key4"), mutable.HashSet(5, 6))
  }

  test("mergeHashMaps should merge overlapping keys by combining sets") {
    // Given
    val map1 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2, 3),
      "key2" -> mutable.HashSet(4)
    )
    val map2 = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(3, 4, 5), // 3 overlaps
      "key2" -> mutable.HashSet(6, 7)
    )

    // When
    val actualResult = map1.mergeHashMaps(map2)

    // Then
    assertEquals(actualResult.size, 2)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2, 3, 4, 5))
    assertEquals(actualResult("key2"), mutable.HashSet(4, 6, 7))
  }

  // Tests for mergeTo extension method
  test("mergeTo should merge source map into destination map") {
    // Given
    val sourceMap = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )
    val destMap   = mutable.HashMap[String, mutable.HashSet[Int]](
      "key2" -> mutable.HashSet(4),
      "key3" -> mutable.HashSet(5)
    )

    // When
    val actualResult = sourceMap.mergeTo(destMap)

    // Then
    assertEquals(actualResult.size, 3)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2))
    assertEquals(actualResult("key2"), mutable.HashSet(3, 4))
    assertEquals(actualResult("key3"), mutable.HashSet(5))
    assert(actualResult eq destMap, "Should return the destination map")
  }

  test("mergeTo should combine sets for overlapping keys") {
    // Given
    val sourceMap = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2, 3),
      "key2" -> mutable.HashSet(4)
    )
    val destMap   = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(3, 4, 5), // 3 overlaps
      "key2" -> mutable.HashSet(6, 7)
    )

    // When
    val actualResult = sourceMap.mergeTo(destMap)

    // Then
    assertEquals(actualResult.size, 2)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2, 3, 4, 5))
    assertEquals(actualResult("key2"), mutable.HashSet(4, 6, 7))
    assert(actualResult eq destMap, "Should return the destination map")
  }

  test("mergeTo should add new keys to empty destination map") {
    // Given
    val sourceMap = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )
    val destMap   = mutable.HashMap[String, mutable.HashSet[Int]]()

    // When
    val actualResult = sourceMap.mergeTo(destMap)

    // Then
    assertEquals(actualResult.size, 2)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2))
    assertEquals(actualResult("key2"), mutable.HashSet(3))
    assert(actualResult eq destMap, "Should return the destination map")
  }

  test("mergeTo should leave destination unchanged when source is empty") {
    // Given
    val sourceMap = mutable.HashMap[String, mutable.HashSet[Int]]()
    val destMap   = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )

    // When
    val actualResult = sourceMap.mergeTo(destMap)

    // Then
    assertEquals(actualResult.size, 2)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2))
    assertEquals(actualResult("key2"), mutable.HashSet(3))
    assert(actualResult eq destMap, "Should return the destination map")
  }

  test("mergeTo should handle both maps empty") {
    // Given
    val sourceMap = mutable.HashMap[String, mutable.HashSet[Int]]()
    val destMap   = mutable.HashMap[String, mutable.HashSet[Int]]()

    // When
    val actualResult = sourceMap.mergeTo(destMap)

    // Then
    assertEquals(actualResult.size, 0)
    assert(actualResult eq destMap, "Should return the destination map")
  }

  test("mergeTo should mutate the destination map") {
    // Given
    val sourceMap = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2)
    )
    val destMap   = mutable.HashMap[String, mutable.HashSet[Int]](
      "key2" -> mutable.HashSet(3)
    )

    // When
    sourceMap.mergeTo(destMap)

    // Then: destMap should be mutated
    assertEquals(destMap.size, 2)
    assertEquals(destMap("key1"), mutable.HashSet(1, 2))
    assertEquals(destMap("key2"), mutable.HashSet(3))
  }

  test("mergeTo should handle disjoint keys") {
    // Given
    val sourceMap = mutable.HashMap[String, mutable.HashSet[Int]](
      "key1" -> mutable.HashSet(1, 2),
      "key2" -> mutable.HashSet(3)
    )
    val destMap   = mutable.HashMap[String, mutable.HashSet[Int]](
      "key3" -> mutable.HashSet(4),
      "key4" -> mutable.HashSet(5, 6)
    )

    // When
    val actualResult = sourceMap.mergeTo(destMap)

    // Then
    assertEquals(actualResult.size, 4)
    assertEquals(actualResult("key1"), mutable.HashSet(1, 2))
    assertEquals(actualResult("key2"), mutable.HashSet(3))
    assertEquals(actualResult("key3"), mutable.HashSet(4))
    assertEquals(actualResult("key4"), mutable.HashSet(5, 6))
  }
