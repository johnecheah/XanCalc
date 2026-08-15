package com.example

import android.app.Application
import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.ui.CalculatorViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("XanCalc", appName)
  }

  @Test
  fun testRepeatPlusEquals() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = CalculatorViewModel(app)
    viewModel.onKeyPress("5")
    viewModel.onKeyPress("+")
    viewModel.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("5", viewModel.expression.value)

    viewModel.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("10", viewModel.expression.value)

    viewModel.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("15", viewModel.expression.value)
  }

  @Test
  fun testMainActivityLaunch() {
    val activityController = Robolectric.buildActivity(MainActivity::class.java)
    activityController.setup()
    val activity = activityController.get()
    assertEquals(false, activity == null)
  }

  @Test
  fun testPhysicalKeyEvents() {
    val activityController = Robolectric.buildActivity(MainActivity::class.java)
    activityController.setup()
    val activity = activityController.get()

    // Send ACTION_DOWN key event for digit '5'
    val event5 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_5)
    val consumed5 = activity.dispatchKeyEvent(event5)
    assertEquals(true, consumed5)

    // Send ACTION_DOWN key event for '+'
    val eventAdd = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PLUS)
    val consumedAdd = activity.dispatchKeyEvent(eventAdd)
    assertEquals(true, consumedAdd)

    // Send ACTION_DOWN key event for '7'
    val event7 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_7)
    val consumed7 = activity.dispatchKeyEvent(event7)
    assertEquals(true, consumed7)

    // Send ACTION_DOWN key event for '='
    val eventEquals = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_EQUALS)
    val consumedEquals = activity.dispatchKeyEvent(eventEquals)
    assertEquals(true, consumedEquals)
  }

  @Test
  fun testPrefixOperatorsShowZero() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    
    // Test division /9=
    val viewModelDiv = CalculatorViewModel(app)
    viewModelDiv.onKeyPress("÷")
    viewModelDiv.onKeyPress("9")
    viewModelDiv.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("0", viewModelDiv.expression.value)

    // Test multiplication *9=
    val viewModelMul = CalculatorViewModel(app)
    viewModelMul.onKeyPress("×")
    viewModelMul.onKeyPress("9")
    viewModelMul.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("0", viewModelMul.expression.value)

    // Test +=
    val viewModelPlusEquals = CalculatorViewModel(app)
    viewModelPlusEquals.onKeyPress("+")
    viewModelPlusEquals.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("", viewModelPlusEquals.expression.value)

    // Test -=
    val viewModelMinusEquals = CalculatorViewModel(app)
    viewModelMinusEquals.onKeyPress("-")
    viewModelMinusEquals.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("", viewModelMinusEquals.expression.value)

    // Test *=
    val viewModelTimesEquals = CalculatorViewModel(app)
    viewModelTimesEquals.onKeyPress("×")
    viewModelTimesEquals.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("", viewModelTimesEquals.expression.value)

    // Test /=
    val viewModelDivideEquals = CalculatorViewModel(app)
    viewModelDivideEquals.onKeyPress("÷")
    viewModelDivideEquals.onKeyPress("=")
    ShadowLooper.idleMainLooper()
    assertEquals("", viewModelDivideEquals.expression.value)
  }

  @Test
  fun testCurrencyConverter() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = CalculatorViewModel(app)

    // Check default currency states
    assertEquals("0", viewModel.currencyInput.value)
    assertEquals("SGD", viewModel.fromCurrency.value)
    assertEquals("MYR", viewModel.toCurrency.value)

    // Set currency from USD to EUR
    viewModel.setFromCurrency("USD")
    viewModel.setToCurrency("EUR")
    viewModel.onCurrencyKeyPress("1")
    viewModel.onCurrencyKeyPress("0")
    viewModel.onCurrencyKeyPress("0")
    assertEquals("100", viewModel.currencyInput.value)

    // Conversion output should be non-empty and a valid positive number
    val outputValue = viewModel.currencyOutput.value.toDoubleOrNull()
    assertEquals(true, outputValue != null && outputValue > 0.0)

    // Swap units
    viewModel.swapCurrencyUnits()
    assertEquals("EUR", viewModel.fromCurrency.value)
    assertEquals("USD", viewModel.toCurrency.value)

    // Switch TARGET/FROM selections
    viewModel.setFromCurrency("GBP")
    viewModel.setToCurrency("USD")
    val gbpToUsd = viewModel.currencyOutput.value.toDoubleOrNull()
    assertEquals(true, gbpToUsd != null && gbpToUsd > 0.0)

    // Test clear (AC)
    viewModel.onCurrencyKeyPress("AC")
    assertEquals("0", viewModel.currencyInput.value)
  }
}

