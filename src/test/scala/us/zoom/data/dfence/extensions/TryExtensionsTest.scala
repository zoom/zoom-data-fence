package us.zoom.data.dfence.extensions

import munit.FunSuite
import scala.util.{Failure, Success, Try}

class TryExtensionsTest extends FunSuite:

  test("getOrThrow should return value when Try is Success") {
    // Given
    val tryValue = Success(42)

    // When
    val actualValue = tryValue.getOrThrow(err => new RuntimeException("Should not be called", err))

    // Then
    assertEquals(actualValue, 42)
  }

  test("getOrThrow should call function with error when Try is Failure") {
    // Given
    val originalError = new IllegalArgumentException("Original error")
    val tryValue      = Failure(originalError)

    // When & Then
    val customError = intercept[RuntimeException] {
      tryValue.getOrThrow(err => throw new RuntimeException(s"Custom: ${err.getMessage}", err))
    }

    assertEquals(customError.getMessage, "Custom: Original error")
    assertEquals(customError.getCause, originalError)
  }

  test("getOrThrow should allow function to return value instead of throwing") {
    // Given
    val originalError = new IllegalArgumentException("Original")
    val tryValue      = Failure(originalError)

    // When
    val defaultValue = tryValue.getOrThrow(_ => 0)

    // Then
    assertEquals(defaultValue, 0)
  }

  test("getOrThrow should preserve exception type when function throws") {
    // Given
    val originalError = new IllegalArgumentException("Original")
    val tryValue      = Failure(originalError)

    // When & Then
    val customError = intercept[IllegalStateException] {
      tryValue.getOrThrow(_ => throw new IllegalStateException("Custom state error"))
    }

    assertEquals(customError.getMessage, "Custom state error")
  }
