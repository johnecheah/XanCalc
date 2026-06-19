package com.example

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CalculatorApp
import com.example.ui.CalculatorViewModel
import com.example.ui.CalculatorViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: CalculatorViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Log.e("CalculatorCrash", "CRASH DETECTED on thread ${thread.name}", throwable)
      System.err.println("UNCAUGHT EXCEPTION IN THREAD ${thread.name}:")
      throwable.printStackTrace()
      defaultHandler?.uncaughtException(thread, throwable)
    }

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val factory = CalculatorViewModelFactory(application)
    viewModel = ViewModelProvider(this, factory)[CalculatorViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          CalculatorApp(viewModel = viewModel)
        }
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    try {
      if (event.action == KeyEvent.ACTION_DOWN) {
        val keyChar = when (event.keyCode) {
          KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> "0"
          KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> "1"
          KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> "2"
          KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> "3"
          KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> "4"
          KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> "5"
          KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> "6"
          KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> "7"
          KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> "8"
          KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> "9"
          KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_NUMPAD_DOT -> "."
          KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> "+"
          KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> "-"
          KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> "×"
          KeyEvent.KEYCODE_SLASH, KeyEvent.KEYCODE_NUMPAD_DIVIDE -> "÷"
          KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_EQUALS -> {
            if (event.isShiftPressed && event.keyCode == KeyEvent.KEYCODE_EQUALS) {
              "+"
            } else {
              "="
            }
          }
          KeyEvent.KEYCODE_DEL -> "DEL"
          KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_ESCAPE -> "AC"
          else -> null
        }

        var finalChar = keyChar
        if (finalChar == null && event.isShiftPressed) {
          finalChar = when (event.keyCode) {
            KeyEvent.KEYCODE_8 -> "×"
            KeyEvent.KEYCODE_5 -> "%"
            else -> null
          }
        }

        if (finalChar != null) {
          if (::viewModel.isInitialized) {
            viewModel.handlePhysicalKeyPress(finalChar)
            return true
          }
        }
      }
    } catch (e: Exception) {
      Log.e("MainActivity", "Error handling key event: ${e.message}", e)
    }
    return super.dispatchKeyEvent(event)
  }
}

