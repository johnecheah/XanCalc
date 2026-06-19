package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testExpressionEvaluatorSqrt() {
    // √9 is mapped to sqrt(9) under our preprocessing, which equals 3.0
    val result = com.example.math.ExpressionEvaluator.evaluate("√9")
    assertEquals(3.0, result, 1e-9)
  }
}
