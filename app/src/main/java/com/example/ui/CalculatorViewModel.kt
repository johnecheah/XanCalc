package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalculatorDatabase
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import com.example.math.ExpressionEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.coroutines.Dispatchers

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository

    init {
        val database = CalculatorDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
    }

    val historyState: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Primary expression text (Visible code layout)
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    // Holds the previous calculation expression (e.g. "5 + 5 ="), only visible after pressing "="
    private val _previousCalculation = MutableStateFlow("")
    val previousCalculation: StateFlow<String> = _previousCalculation.asStateFlow()

    private var didJustEvaluateValue = false
    private var lastOperator: String? = null
    private var lastOperand: String? = null
    private var isRepeatFunctionActive = false

    // Preview evaluation result, displayed beneath primary formula
    private val _previewResult = MutableStateFlow("")
    val previewResult: StateFlow<String> = _previewResult.asStateFlow()

    // Error state to feedback user
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Radian vs Degree controls (scientific mode)
    private val _isRadians = MutableStateFlow(true)
    val isRadians: StateFlow<Boolean> = _isRadians.asStateFlow()

    // Active tab state hoisted to ViewModel
    private val _selectedTab = MutableStateFlow(CalculatorTab.STANDARD)
    val selectedTab: StateFlow<CalculatorTab> = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: CalculatorTab) {
        _selectedTab.value = tab
        // Reset inputs to default values on tab switch
        _expression.value = ""
        _previewResult.value = ""
        _errorMessage.value = null
        _converterInput.value = "0"
        _previousCalculation.value = ""
        lastOperator = null
        lastOperand = null
        isRepeatFunctionActive = false
        performConversion()
        if (tab == CalculatorTab.CURRENCY) {
            refreshCurrencyRates()
        }
    }

    fun handlePhysicalKeyPress(key: String) {
        try {
            when (_selectedTab.value) {
                CalculatorTab.STANDARD, CalculatorTab.SCIENTIFIC -> {
                    onKeyPress(key)
                }
                CalculatorTab.CONVERTER -> {
                    if (key != "+" && key != "-" && key != "×" && key != "÷" && key != "=") {
                        onConverterKeyPress(key)
                    }
                }
                CalculatorTab.CURRENCY -> {
                    if (key != "+" && key != "-" && key != "×" && key != "÷" && key != "=") {
                        onCurrencyKeyPress(key)
                    }
                }
                CalculatorTab.HISTORY -> {
                    // Ignore keypad keys in history
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("CalculatorVM", "Error in handlePhysicalKeyPress for key: $key", t)
        }
    }

    // --- Converter States ---
    private val sharedPrefs = application.getSharedPreferences("unit_converter_prefs", android.content.Context.MODE_PRIVATE)

    private val categoryUnitSelections = mutableMapOf<ConverterCategory, Pair<String, String>>().apply {
        ConverterCategory.values().forEach { category ->
            val units = category.units
            if (units.isNotEmpty()) {
                val fromUnit = units[0].name
                val toUnit = if (units.size > 1) units[1].name else units[0].name
                put(category, Pair(fromUnit, toUnit))
            }
        }
    }

    private val _selectedCategory = MutableStateFlow(
        run {
            val savedCatName = sharedPrefs.getString("selected_category", null)
            if (savedCatName != null) {
                try {
                    ConverterCategory.valueOf(savedCatName)
                } catch (e: Exception) {
                    ConverterCategory.AREA
                }
            } else {
                ConverterCategory.AREA
            }
        }
    )
    val selectedCategory: StateFlow<ConverterCategory> = _selectedCategory.asStateFlow()

    private val _converterInput = MutableStateFlow("0")
    val converterInput: StateFlow<String> = _converterInput.asStateFlow()

    private val _fromUnitName = MutableStateFlow(
        categoryUnitSelections[_selectedCategory.value]?.first ?: "Square Foot"
    )
    val fromUnitName: StateFlow<String> = _fromUnitName.asStateFlow()

    private val _toUnitName = MutableStateFlow(
        categoryUnitSelections[_selectedCategory.value]?.second ?: "Square Meter"
    )
    val toUnitName: StateFlow<String> = _toUnitName.asStateFlow()

    private val _converterOutput = MutableStateFlow("0")
    val converterOutput: StateFlow<String> = _converterOutput.asStateFlow()

    // --- Currency Converter States ---
    private val currencyPrefs = application.getSharedPreferences("currency_prefs", android.content.Context.MODE_PRIVATE)

    private val _currencyInput = MutableStateFlow("1")
    val currencyInput: StateFlow<String> = _currencyInput.asStateFlow()

    private val _fromCurrency = MutableStateFlow("SGD")
    val fromCurrency: StateFlow<String> = _fromCurrency.asStateFlow()

    private val _toCurrency = MutableStateFlow("MYR")
    val toCurrency: StateFlow<String> = _toCurrency.asStateFlow()

    private val _currencyOutput = MutableStateFlow("0")
    val currencyOutput: StateFlow<String> = _currencyOutput.asStateFlow()

    private val _isFetchingRates = MutableStateFlow(false)
    val isFetchingRates: StateFlow<Boolean> = _isFetchingRates.asStateFlow()

    private val _lastRatesUpdate = MutableStateFlow("Tap refresh to load live rates")
    val lastRatesUpdate: StateFlow<String> = _lastRatesUpdate.asStateFlow()

    private val _currencyRates = MutableStateFlow(
        ALL_CURRENCIES.keys.associateWith { code ->
            when (code) {
                "USD" -> 1.0
                "EUR" -> 0.92
                "GBP" -> 0.78
                "JPY" -> 155.0
                "CAD" -> 1.36
                "AUD" -> 1.50
                "SGD" -> 1.35
                "CNY" -> 7.25
                "INR" -> 83.5
                "CHF" -> 0.90
                "NZD" -> 1.63
                "HKD" -> 7.82
                "SEK" -> 10.50
                "KRW" -> 1380.0
                "AED" -> 3.67
                else -> 1.0
            }
        }
    )
    val currencyRates: StateFlow<Map<String, Double>> = _currencyRates.asStateFlow()

    init {
        // Automatically default units when category changes or load saved selections
        viewModelScope.launch {
            _selectedCategory.collect { category ->
                val saved = categoryUnitSelections[category]
                if (saved != null) {
                    _fromUnitName.value = saved.first
                    _toUnitName.value = saved.second
                } else {
                    val units = category.units
                    if (units.isNotEmpty()) {
                        _fromUnitName.value = units[0].name
                        _toUnitName.value = if (units.size > 1) units[1].name else units[0].name
                    }
                }
                performConversion()
            }
        }
        
        // Initialize currency conversion & fetch rates
        performCurrencyConversion()
        refreshCurrencyRates()
    }

    private fun extractLastOperation(expression: String): Pair<String, String>? {
        val trimmed = expression.trim()
        val operators = listOf('+', '-', '×', '÷', '^')
        for (i in trimmed.length - 1 downTo 0) {
            val char = trimmed[i]
            if (char in operators) {
                // Check if '-' is unary (preceded by another operator or open bracket or start)
                if (char == '-') {
                    var beforeIndex = i - 1
                    while (beforeIndex >= 0 && trimmed[beforeIndex] == ' ') {
                        beforeIndex--
                    }
                    if (beforeIndex < 0 || trimmed[beforeIndex] in listOf('+', '-', '×', '÷', '^', '(')) {
                        continue // unary minus
                    }
                }
                
                val op = char.toString()
                val rightSide = trimmed.substring(i + 1).trim()
                if (rightSide.isNotEmpty()) {
                    return Pair(op, rightSide)
                }
            }
        }
        return null
    }

    fun onKeyPress(key: String) {
        _errorMessage.value = null
        val isConsecutiveEquals = (key == "=" && didJustEvaluateValue)

        if (key != "=") {
            _previousCalculation.value = ""
        }

        if (didJustEvaluateValue) {
            val isContinuingKey = key in listOf("+", "-", "×", "÷", "^", "%", "!", "+/-", "DEL", "=")
            if (!isContinuingKey && key != "AC") {
                _expression.value = ""
                _previewResult.value = ""
            }
            didJustEvaluateValue = false
        }

        val current = _expression.value

        when (key) {
            "AC" -> {
                _expression.value = ""
                _previewResult.value = ""
                _previousCalculation.value = ""
                lastOperator = null
                lastOperand = null
                isRepeatFunctionActive = false
            }
            "DEL" -> {
                if (current.isNotEmpty()) {
                    // Check if deleting a system function name block
                    val isFunctionEnding = functionNames.any { current.endsWith("$it(") }
                    if (isFunctionEnding) {
                        val matchingFunc = functionNames.first { current.endsWith("$it(") }
                        _expression.value = current.substring(0, current.length - "$matchingFunc(".length)
                    } else {
                        _expression.value = current.substring(0, current.length - 1)
                    }
                }
                updatePreview()
            }
            "=" -> {
                var currentExpr = _expression.value
                val operators = setOf("+", "-", "×", "÷", "*", "/", "^")
                if (currentExpr.trim() in operators) {
                    // Similar to AC (clear button)
                    _expression.value = ""
                    _previewResult.value = ""
                    _previousCalculation.value = ""
                    lastOperator = null
                    lastOperand = null
                    isRepeatFunctionActive = false
                    return
                }
                val trimmed = currentExpr.trim()
                if (trimmed.isEmpty()) return

                val hasOperand = trimmed.any { it.isDigit() || it == 'π' || it == 'e' }
                if (!hasOperand) {
                    _expression.value = "0"
                    _previewResult.value = ""
                    _errorMessage.value = null
                    didJustEvaluateValue = true
                    return
                }

                if (!isConsecutiveEquals && !isCalculation(currentExpr)) {
                    _previewResult.value = ""
                    _errorMessage.value = null
                    didJustEvaluateValue = true
                    return
                }

                var saveToHistory = true
                var updatePreviousCalculation = true

                if (isConsecutiveEquals) {
                    saveToHistory = false
                    if (isRepeatFunctionActive) {
                        val op = lastOperator
                        val operand = lastOperand
                        if (op != null && operand != null) {
                            _expression.value = "$currentExpr $op $operand"
                        }
                    } else {
                        updatePreviousCalculation = false
                    }
                } else {
                    val trimmed = currentExpr.trim()
                    val operators = setOf('+', '-', '×', '÷', '^')
                    val endsWithOperator = trimmed.isNotEmpty() && trimmed.last() in operators

                    if (endsWithOperator) {
                        isRepeatFunctionActive = true
                        saveToHistory = false
                        val op = trimmed.last().toString()
                        val rawOperand = trimTrailingOperators(trimmed.substring(0, trimmed.length - 1).trim())
                        val evaluatedOperand = try {
                            val evalResult = ExpressionEvaluator.evaluate(rawOperand, _isRadians.value)
                            formatResult(evalResult)
                        } catch (e: Throwable) {
                            rawOperand
                        }
                        lastOperator = op
                        lastOperand = evaluatedOperand
                        _expression.value = if (rawOperand.isNotEmpty()) rawOperand else "0"
                    } else {
                        isRepeatFunctionActive = false
                        lastOperator = null
                        lastOperand = null
                    }
                }

                evaluateExpression(
                    expressionToEvaluate = _expression.value,
                    isFinal = true,
                    saveToHistory = saveToHistory,
                    updatePreviousCalculation = updatePreviousCalculation
                )
                didJustEvaluateValue = true
            }
            "+/-" -> {
                toggleSign()
            }
            "%" -> {
                applyPercentage()
            }
            else -> {
                // If appending functions, open bracket immediately
                val keyToAppend = if (key in functionNamesWithoutParenthesis) "$key(" else key
                val isDigitOrFuncOrConst = key.any { it.isDigit() } || 
                                          key in functionNamesWithoutParenthesis || 
                                          key == "π" || key == "e" || key == "(" || key == ")"
                if (current == "0" && isDigitOrFuncOrConst) {
                    _expression.value = keyToAppend
                } else {
                    _expression.value = current + keyToAppend
                }
                updatePreview()
            }
        }
    }

    private fun toggleSign() {
        val current = _expression.value
        if (current.isEmpty()) {
            _expression.value = "-"
            return
        }

        // Simplistic sign toggle: wrapping or prepending '-' or removing it
        if (current.startsWith("-") && !current.contains(Regex("[+×÷]"))) {
            _expression.value = current.substring(1)
        } else if (!current.contains(Regex("[+×÷-]"))) {
            _expression.value = "-$current"
        } else {
            // Complex expressions: wrap the whole expression in parentheses and prepend negative
            if (current.startsWith("-(") && current.endsWith(")")) {
                _expression.value = current.substring(2, current.length - 1)
            } else {
                _expression.value = "-($current)"
            }
        }
        updatePreview()
    }

    private fun applyPercentage() {
        val current = _expression.value
        if (current.isEmpty()) return
        
        // Append percentage sign so Parser parses postfix percent
        _expression.value = current + "%"
        updatePreview()
    }

    private fun updatePreview() {
        val current = _expression.value
        if (current.trim().isEmpty()) {
            _previewResult.value = ""
            return
        }
        evaluateExpression(expressionToEvaluate = current, isFinal = false)
    }

    private fun trimTrailingOperators(expression: String): String {
        var expr = expression.trim()
        val operators = setOf('+', '-', '×', '÷', '*', '/', '^')
        while (expr.isNotEmpty() && expr.last() in operators) {
            expr = expr.substring(0, expr.length - 1).trim()
        }
        return expr
    }

    private fun evaluateExpression(
        expressionToEvaluate: String,
        isFinal: Boolean,
        saveToHistory: Boolean = true,
        updatePreviousCalculation: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                var expr = expressionToEvaluate
                expr = trimTrailingOperators(expr)
                if (expr.isEmpty()) {
                    if (isFinal) {
                        _errorMessage.value = "Invalid Expression"
                    } else {
                        _previewResult.value = ""
                    }
                    return@launch
                }

                val trimmed = expr.trim()
                if (trimmed.startsWith("÷") || trimmed.startsWith("×") || trimmed.startsWith("^") || trimmed.startsWith("/") || trimmed.startsWith("*")) {
                    expr = "0$expr"
                }

                // Balance parenthesis automatically for dynamic evaluation preview
                val openBrackets = expr.count { it == '(' }
                val closeBrackets = expr.count { it == ')' }
                if (openBrackets > closeBrackets) {
                    expr += ")".repeat(openBrackets - closeBrackets)
                }

                val evalResult = ExpressionEvaluator.evaluate(expr, _isRadians.value)
                val formatted = formatResult(evalResult)

                if (isFinal) {
                    if (updatePreviousCalculation) {
                        _previousCalculation.value = expr
                    }
                    _expression.value = formatted
                    _previewResult.value = ""

                    if (saveToHistory) {
                        viewModelScope.launch {
                            try {
                                val type = if (isScientificExpression(expressionToEvaluate)) "SCIENTIFIC" else "STANDARD"
                                repository.insert(
                                    HistoryEntity(
                                        expression = expr,
                                        result = formatted,
                                        type = type
                                    )
                                )
                            } catch (e: Throwable) {
                                // Background save error fallback
                            }
                        }
                    }
                } else {
                    _previewResult.value = formatted
                }
            } catch (e: Throwable) {
                if (isFinal) {
                    _errorMessage.value = e.message ?: "Invalid Expression"
                    if (updatePreviousCalculation) {
                        _previousCalculation.value = ""
                    }
                } else {
                    _previewResult.value = ""
                }
            }
        }
    }

    private fun isCalculation(expr: String): Boolean {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return false

        // Check if there are any operands (digits, constants)
        val hasOperand = trimmed.any { it.isDigit() || it == 'π' || it == 'e' }
        if (!hasOperand) return false

        // Operators
        val operators = listOf("+", "-", "×", "÷", "^", "%", "!", "√")
        val hasOperator = operators.any { trimmed.contains(it) }
        val hasFunction = functionNames.any { trimmed.contains(it) }

        if (hasOperator || hasFunction) {
            // If the only operator is a leading negative, it's just a negative number (like -5), which is not a calculation.
            if (trimmed.startsWith("-")) {
                val remaining = trimmed.substring(1)
                val hasOtherOperator = operators.any { remaining.contains(it) }
                val hasOtherFunction = functionNames.any { remaining.contains(it) }
                return hasOtherOperator || hasOtherFunction
            }
            return true
        }
        return false
    }

    private fun isScientificExpression(expr: String): Boolean {
        return functionNames.any { expr.contains(it) } || expr.contains("^") || expr.contains("√") || expr.contains("π") || expr.contains("e")
    }

    fun toggleRadians() {
        _isRadians.value = !_isRadians.value
        updatePreview()
    }

    // --- Converter Functions ---
    private fun saveCategoryUnits(category: ConverterCategory, from: String, to: String) {
        categoryUnitSelections[category] = Pair(from, to)
    }

    fun changeCategory(category: ConverterCategory) {
        _selectedCategory.value = category
        sharedPrefs.edit().putString("selected_category", category.name).apply()
    }

    fun setConverterInput(input: String) {
        _converterInput.value = input
        performConversion()
    }

    fun onConverterKeyPress(key: String) {
        val current = _converterInput.value
        when (key) {
            "AC" -> {
                _converterInput.value = "0"
            }
            "DEL" -> {
                if (current.isNotEmpty()) {
                    val next = current.substring(0, current.length - 1)
                    _converterInput.value = next.ifEmpty { "0" }
                } else {
                    _converterInput.value = "0"
                }
            }
            "+/-" -> {
                if (current != "0" && current.isNotEmpty()) {
                    _converterInput.value = if (current.startsWith("-")) {
                        current.substring(1)
                    } else {
                        "-$current"
                    }
                }
            }
            "." -> {
                if (!current.contains(".")) {
                    _converterInput.value = current + "."
                }
            }
            else -> {
                // It is a digit: "0"-"9"
                if (current == "0") {
                    _converterInput.value = key
                } else {
                    _converterInput.value = current + key
                }
            }
        }
        performConversion()
    }

    fun setFromUnit(unitName: String) {
        _fromUnitName.value = unitName
        saveCategoryUnits(_selectedCategory.value, unitName, _toUnitName.value)
        performConversion()
    }

    fun setToUnit(unitName: String) {
        _toUnitName.value = unitName
        saveCategoryUnits(_selectedCategory.value, _fromUnitName.value, unitName)
        performConversion()
    }

    fun swapConverterUnits() {
        val temp = _fromUnitName.value
        _fromUnitName.value = _toUnitName.value
        _toUnitName.value = temp
        saveCategoryUnits(_selectedCategory.value, _fromUnitName.value, _toUnitName.value)
        performConversion()
    }

    fun performConversion() {
        val inputStr = _converterInput.value
        if (inputStr.isEmpty()) {
            _converterOutput.value = ""
            return
        }

        val parsedInput = inputStr.toDoubleOrNull()
        if (parsedInput == null) {
            _converterOutput.value = "Invalid Input"
            return
        }

        val category = _selectedCategory.value
        val fromUnit = category.units.firstOrNull { it.name == _fromUnitName.value }
        val toUnit = category.units.firstOrNull { it.name == _toUnitName.value }

        if (fromUnit == null || toUnit == null) {
            _converterOutput.value = ""
            return
        }

        val result = if (category == ConverterCategory.TEMPERATURE) {
            convertTemperature(parsedInput, fromUnit.name, toUnit.name)
        } else {
            // Standard conversion via base ratios
            // val ratioToMeter = fromUnit.multiplier
            // val valueInBase = parsedInput * ratioToMeter
            // val valueInTarget = valueInBase / toUnit.multiplier
            val valueInBase = parsedInput * fromUnit.multiplier
            valueInBase / toUnit.multiplier
        }

        _converterOutput.value = formatResult(result)
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        val inCelsius = when (from) {
            "Celsius" -> value
            "Fahrenheit" -> (value - 32.0) * (5.0 / 9.0)
            "Kelvin" -> value - 273.15
            else -> value
        }

        return when (to) {
            "Celsius" -> inCelsius
            "Fahrenheit" -> inCelsius * (9.0 / 5.0) + 32.0
            "Kelvin" -> inCelsius + 273.15
            else -> inCelsius
        }
    }

    fun saveConversionToHistory() {
        val category = _selectedCategory.value.label
        val from = _fromUnitName.value
        val to = _toUnitName.value
        val input = _converterInput.value
        val output = _converterOutput.value

        if (input.isNotEmpty() && output.isNotEmpty() && output != "Invalid Input") {
            viewModelScope.launch {
                repository.insert(
                    HistoryEntity(
                        expression = "$input [$from]",
                        result = "$output [$to]",
                        type = "CONVERSION: $category"
                    )
                )
            }
        }
    }

    // --- Currency Converter Functions ---
    fun onCurrencyKeyPress(key: String) {
        val current = _currencyInput.value
        when (key) {
            "AC" -> {
                _currencyInput.value = "1"
            }
            "DEL" -> {
                if (current.isNotEmpty()) {
                    val next = current.substring(0, current.length - 1)
                    _currencyInput.value = next.ifEmpty { "1" }
                } else {
                    _currencyInput.value = "1"
                }
            }
            "+/-" -> {
                if (current != "0" && current != "1" && current.isNotEmpty()) {
                    _currencyInput.value = if (current.startsWith("-")) {
                        current.substring(1)
                    } else {
                        "-$current"
                    }
                }
            }
            "." -> {
                if (!current.contains(".")) {
                    _currencyInput.value = current + "."
                }
            }
            else -> {
                if (current == "0" || current == "1") {
                    _currencyInput.value = key
                } else {
                    _currencyInput.value = current + key
                }
            }
        }
        performCurrencyConversion()
    }

    fun setFromCurrency(code: String) {
        _fromCurrency.value = code
        performCurrencyConversion()
    }

    fun setToCurrency(code: String) {
        _toCurrency.value = code
        performCurrencyConversion()
    }

    fun swapCurrencyUnits() {
        val temp = _fromCurrency.value
        _fromCurrency.value = _toCurrency.value
        _toCurrency.value = temp
        performCurrencyConversion()
    }

    fun performCurrencyConversion() {
        val inputStr = _currencyInput.value
        val inputVal = inputStr.toDoubleOrNull() ?: 0.0
        val fromCode = _fromCurrency.value
        val toCode = _toCurrency.value

        val rates = _currencyRates.value
        val fromRate = rates[fromCode] ?: 1.0
        val toRate = rates[toCode] ?: 1.0

        val valueInUsd = inputVal / fromRate
        val convertedValue = valueInUsd * toRate
        _currencyOutput.value = formatCurrencyResult(convertedValue)
    }

    private fun formatCurrencyResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        return if (value % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%.0f", value)
        } else {
            val formatted = String.format(java.util.Locale.US, "%.4f", value)
            if (formatted.contains(".")) {
                var cleaned = formatted.dropLastWhile { it == '0' }
                if (cleaned.endsWith(".")) {
                    cleaned = cleaned.dropLast(1)
                }
                cleaned
            } else {
                formatted
            }
        }
    }

    fun saveCurrencyConversionToHistory() {
        val from = _fromCurrency.value
        val to = _toCurrency.value
        val input = _currencyInput.value
        val output = _currencyOutput.value

        if (input.isNotEmpty() && output.isNotEmpty() && output != "Invalid Input") {
            viewModelScope.launch {
                repository.insert(
                    HistoryEntity(
                        expression = "$input $from",
                        result = "$output $to",
                        type = "CURRENCY"
                    )
                )
            }
        }
    }

    fun refreshCurrencyRates() {
        if (_isFetchingRates.value) return
        _isFetchingRates.value = true
        _lastRatesUpdate.value = "Fetching live rates..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://open.er-api.com/v6/latest/USD")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                val code = connection.responseCode
                if (code == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    val rateRegex = """"([A-Z]{3})"\s*:\s*([0-9.]+)""".toRegex()
                    val matches = rateRegex.findAll(text)
                    val newRates = _currencyRates.value.toMutableMap()
                    for (match in matches) {
                        val currencyCode = match.groupValues[1]
                        val rate = match.groupValues[2].toDoubleOrNull()
                        if (rate != null) {
                            newRates[currencyCode] = rate
                        }
                    }
                    _currencyRates.value = newRates
                    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    val timeStr = formatter.format(java.util.Date())
                    _lastRatesUpdate.value = "Live rates updated at $timeStr"
                } else {
                    _lastRatesUpdate.value = "Failed to fetch live rates (HTTP $code)"
                }
            } catch (e: java.net.UnknownHostException) {
                _lastRatesUpdate.value = "Offline mode - local fallback rates active"
            } catch (e: Throwable) {
                _lastRatesUpdate.value = "Local fallback rates active"
            } finally {
                _isFetchingRates.value = false
                performCurrencyConversion()
            }
        }
    }

    // --- History Functions ---
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun loadHistoryItemToCalculator(history: HistoryEntity) {
        if (history.type.startsWith("CONVERSION")) {
            // If conversion item, extract parameters and set to converter
            // Format is "input [from]" and "output [to]"
            val categoryLabel = history.type.removePrefix("CONVERSION: ")
            val matchedCategory = ConverterCategory.values().firstOrNull { it.label == categoryLabel }
            if (matchedCategory != null) {
                // Extract input
                val fromRegex = Regex("^(.*) \\[(.*)\\]$")
                val fromMatch = fromRegex.matchEntire(history.expression)
                val toMatch = fromRegex.matchEntire(history.result)
                val fromVal = fromMatch?.groupValues?.get(2) ?: matchedCategory.units.getOrNull(0)?.name ?: ""
                val toVal = toMatch?.groupValues?.get(2) ?: matchedCategory.units.getOrNull(1)?.name ?: fromVal

                saveCategoryUnits(matchedCategory, fromVal, toVal)
                _selectedCategory.value = matchedCategory
                sharedPrefs.edit().putString("selected_category", matchedCategory.name).apply()
                
                if (fromMatch != null) {
                    _converterInput.value = fromMatch.groupValues[1]
                    _fromUnitName.value = fromMatch.groupValues[2]
                }
                
                if (toMatch != null) {
                    _toUnitName.value = toMatch.groupValues[2]
                }
                performConversion()
            }
            _selectedTab.value = CalculatorTab.CONVERTER
        } else if (history.type == "CURRENCY") {
            // e.g. "100 USD" and "92 EUR"
            val partsFrom = history.expression.split(" ")
            val partsTo = history.result.split(" ")
            if (partsFrom.size >= 2) {
                _currencyInput.value = partsFrom[0]
                _fromCurrency.value = partsFrom[1]
            }
            if (partsTo.size >= 2) {
                _toCurrency.value = partsTo[1]
            }
            performCurrencyConversion()
            _selectedTab.value = CalculatorTab.CURRENCY
            refreshCurrencyRates()
        } else {
            // calculator standard / scientific expression loaded
            _expression.value = history.expression
            updatePreview()
            _selectedTab.value = if (_isRadians.value && history.type == "SCIENTIFIC") {
                CalculatorTab.SCIENTIFIC
            } else {
                CalculatorTab.STANDARD
            }
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        if (value % 1.0 == 0.0) {
            if (value >= Long.MAX_VALUE.toDouble() || value <= Long.MIN_VALUE.toDouble()) {
                return value.toString()
            }
            return value.toLong().toString()
        }
        val formatted = String.format(Locale.US, "%.8f", value)
            .replace(Regex("0+$"), "")
            .replace(Regex("\\.$"), "")
        return if (formatted == "-0") "0" else formatted
    }

    companion object {
        val functionNamesWithoutParenthesis = listOf(
            "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "cbrt"
        )
        val functionNames = functionNamesWithoutParenthesis.map { "$it(" }

        val ALL_CURRENCIES = mapOf(
            "USD" to "US Dollar",
            "EUR" to "Euro",
            "GBP" to "British Pound",
            "JPY" to "Japanese Yen",
            "AUD" to "Australian Dollar",
            "CAD" to "Canadian Dollar",
            "CHF" to "Swiss Franc",
            "CNY" to "Chinese Yuan",
            "INR" to "Indian Rupee",
            "SGD" to "Singapore Dollar",
            "NZD" to "New Zealand Dollar",
            "HKD" to "Hong Kong Dollar",
            "SEK" to "Swedish Krona",
            "KRW" to "South Korean Won",
            "TRY" to "Turkish Lira",
            "RUB" to "Russian Ruble",
            "ZAR" to "South African Rand",
            "DKK" to "Danish Krone",
            "PLN" to "Polish Zloty",
            "BRL" to "Brazilian Real",
            "TWD" to "New Taiwan Dollar",
            "THB" to "Thai Baht",
            "IDR" to "Indonesian Rupiah",
            "HUF" to "Hungarian Forint",
            "CZK" to "Czech Koruna",
            "ILS" to "Israeli New Shekel",
            "CLP" to "Chilean Peso",
            "PHP" to "Philippine Peso",
            "AED" to "UAE Dirham",
            "COP" to "Colombian Peso",
            "SAR" to "Saudi Riyal",
            "MYR" to "Malaysian Ringgit",
            "RON" to "Romanian Leu",
            "VND" to "Vietnamese Dong",
            "ARS" to "Argentine Peso",
            "IQD" to "Iraqi Dinar",
            "EGP" to "Egyptian Pound",
            "KWD" to "Kuwaiti Dinar",
            "QAR" to "Qatari Riyal",
            "PKR" to "Pakistani Rupee",
            "UAH" to "Ukrainian Hryvnia",
            "MAD" to "Moroccan Dirham",
            "KZT" to "Kazakhstani Tenge",
            "OMR" to "Omani Rial",
            "LKR" to "Sri Lankan Rupee",
            "BHD" to "Bahraini Dinar",
            "BGN" to "Bulgarian Lev",
            "JOD" to "Jordanian Dinar",
            "GEL" to "Georgian Lari",
            "UZS" to "Uzbekistani Som",
            "DOP" to "Dominican Peso",
            "CRC" to "Costa Rican Colon",
            "ALL" to "Albanian Lek",
            "AZN" to "Azerbaijani Manat",
            "BND" to "Brunei Dollar",
            "GTQ" to "Guatemalan Quetzal",
            "BYN" to "Belarusian Ruble",
            "HNL" to "Honduran Lempira",
            "MDL" to "Moldovan Leu",
            "PAB" to "Panamanian Balboa",
            "PEN" to "Peruvian Sol",
            "RSD" to "Serbian Dinar",
            "TND" to "Tunisian Dinar",
            "UYU" to "Uruguayan Peso",
            "BOB" to "Bolivian Boliviano",
            "AFN" to "Afghan Afghani",
            "AMD" to "Armenian Dram",
            "ANG" to "Netherlands Antillean Guilder",
            "AOA" to "Angolan Kwanza",
            "AWG" to "Aruban Florin",
            "BAM" to "Bosnia Convertible Mark",
            "BBD" to "Barbadian Dollar",
            "BDT" to "Bangladeshi Taka",
            "BIF" to "Burundian Franc",
            "BMD" to "Bermudian Dollar",
            "BSD" to "Bahamian Dollar",
            "BTN" to "Bhutanese Ngultrum",
            "BWP" to "Botswanan Pula",
            "BZD" to "Belize Dollar",
            "CDF" to "Congolese Franc",
            "CUP" to "Cuban Peso",
            "CVE" to "Cape Verdean Escudo",
            "DJF" to "Djiboutian Franc",
            "ERN" to "Eritrean Nakfa",
            "ETB" to "Ethiopian Birr",
            "FJD" to "Fijian Dollar",
            "FKP" to "Falkland Islands Pound",
            "FOK" to "Faroese Króna",
            "GGP" to "Guernsey Pound",
            "GHS" to "Ghanaian Cedi",
            "GIP" to "Gibraltar Pound",
            "GMD" to "Gambian Dalasi",
            "GNF" to "Guinean Franc",
            "GYD" to "Guyanese Dollar",
            "HTG" to "Haitian Gourde",
            "IMP" to "Manx Pound",
            "IRR" to "Iranian Rial",
            "ISK" to "Icelandic Krona",
            "JEP" to "Jersey Pound",
            "JMD" to "Jamaican Dollar",
            "KES" to "Kenyan Shilling",
            "KGS" to "Kyrgystani Som",
            "KHR" to "Cambodian Riel",
            "KID" to "Kiribati Dollar",
            "KMF" to "Comorian Franc",
            "KPW" to "North Korean Won",
            "KYD" to "Cayman Islands Dollar",
            "LAK" to "Laotian Kip",
            "LBP" to "Lebanese Pound",
            "LRD" to "Liberian Dollar",
            "LSL" to "Lesotho Loti",
            "LYD" to "Libyan Dinar",
            "MGA" to "Malagasy Ariary",
            "MKD" to "Macedonian Denar",
            "MMK" to "Myanmar Kyat",
            "MNT" to "Mongolian Tugrik",
            "MOP" to "Macanese Pataca",
            "MRU" to "Mauritanian Ouguiya",
            "MUR" to "Mauritian Rupee",
            "MVR" to "Maldivian Rufiyaa",
            "MWK" to "Malawian Kwacha",
            "MZN" to "Mozambican Metical",
            "NAD" to "Namibian Dollar",
            "NGN" to "Nigerian Naira",
            "NIO" to "Nicaraguan Cordoba",
            "NPR" to "Nepalese Rupee",
            "PGK" to "Papua New Guinean Kina",
            "PYG" to "Paraguayan Guarani",
            "RWF" to "Rwandan Franc",
            "SBD" to "Solomon Islands Dollar",
            "SCR" to "Seychellois Rupee",
            "SDG" to "Sudanese Pound",
            "SHP" to "Saint Helena Pound",
            "SLE" to "Sierra Leonean Leone",
            "SOS" to "Somali Shilling",
            "SRD" to "Surinamese Dollar",
            "SSP" to "South Sudanese Pound",
            "STN" to "São Tomé Dobra",
            "SYP" to "Syrian Pound",
            "SZL" to "Swazi Lilangeni",
            "TJS" to "Tajikistani Somoni",
            "TMT" to "Turkmenistani Manat",
            "TOP" to "Tongan Paʻanga",
            "TTD" to "Trinidad and Tobago Dollar",
            "TVD" to "Tuvaluan Dollar",
            "TZS" to "Tanzanian Shilling",
            "UGX" to "Ugandan Shilling",
            "UYU" to "Uruguayan Peso",
            "VES" to "Venezuelan Bolívar",
            "VUV" to "Vanuatu Vatu",
            "WST" to "Samoan Tala",
            "XAF" to "Central African CFA Franc",
            "XCD" to "East Caribbean Dollar",
            "XDR" to "Special Drawing Rights",
            "XOF" to "West African CFA Franc",
            "XPF" to "CFP Franc",
            "YER" to "Yemeni Rial",
            "ZMW" to "Zambian Kwacha",
            "ZWL" to "Zimbabwean Dollar"
        )
    }
}

class CalculatorViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- Enum classes matching conversion schema ---
enum class ConverterCategory(val label: String, val units: List<ConversionUnit>) {
    AREA("Area", listOf(
        ConversionUnit("Square Foot", 0.09290304),
        ConversionUnit("Square Meter", 1.0),
        ConversionUnit("Square Kilometer", 1000000.0),
        ConversionUnit("Acre", 4046.8564224),
        ConversionUnit("Square Mile", 2589988.110336)
    )),
    LENGTH("Length", listOf(
        ConversionUnit("Meter", 1.0),
        ConversionUnit("Millimeter", 0.001),
        ConversionUnit("Centimeter", 0.01),
        ConversionUnit("Kilometer", 1000.0),
        ConversionUnit("Inch", 0.0254),
        ConversionUnit("Foot", 0.3048),
        ConversionUnit("Yard", 0.9144),
        ConversionUnit("Mile", 1609.344)
    )),
    WEIGHT("Weight/Mass", listOf(
        ConversionUnit("Gram", 1.0),
        ConversionUnit("Milligram", 0.001),
        ConversionUnit("Kilogram", 1000.0),
        ConversionUnit("Pound", 453.59237),
        ConversionUnit("Ounce", 28.349523125)
    )),
    TEMPERATURE("Temperature", listOf(
        ConversionUnit("Celsius", 1.0),
        ConversionUnit("Fahrenheit", 1.0),
        ConversionUnit("Kelvin", 1.0)
    )),
    VOLUME("Volume", listOf(
        ConversionUnit("Liter", 1.0),
        ConversionUnit("Milliliter", 0.001),
        ConversionUnit("Cubic Meter", 1000.0),
        ConversionUnit("Gallon", 3.785411784),
        ConversionUnit("Cup", 0.2365882365),
        ConversionUnit("Fluid Ounce", 0.02957352956)
    )),
    SPEED("Speed", listOf(
        ConversionUnit("Meter/Second", 1.0),
        ConversionUnit("Kilometer/Hour", 0.2777777778),
        ConversionUnit("Mile/Hour", 0.44704),
        ConversionUnit("Knot", 0.5144444444)
    ))
}

data class ConversionUnit(val name: String, val multiplier: Double)
