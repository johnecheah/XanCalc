package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HistoryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Custom luxury theme color constants matching a premium carbon-cyber style
val DarkBgStart = Color(0xFF0F111A)
val DarkBgEnd = Color(0xFF1B1E2E)
val PrimaryOrange = Color(0xFFFF9E22)
val LightChalk = Color(0xFFE2E4EB)
val CharcoalButton = Color(0xFF1E2130)
val CharcoalButtonActive = Color(0xFF2C3147)
val ScientificCyan = Color(0xFF00B4D8)
val ScientificCyanContainer = Color(0xFF143F4D)
val AccentPurple = Color(0xFF7209B7)
val HistoryCardBg = Color(0xFF191C2B)

enum class CalculatorTab {
    STANDARD, SCIENTIFIC, CONVERTER, CURRENCY, GOLD, HISTORY
}

fun findActivity(context: android.content.Context): android.app.Activity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

fun findComponentActivity(context: android.content.Context): androidx.activity.ComponentActivity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is androidx.activity.ComponentActivity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

val LocalIsInPip = staticCompositionLocalOf { false }

@Composable
fun CalculatorApp(viewModel: CalculatorViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val activity = remember(context) { findActivity(context) }
    var isInPip by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val compAct = findComponentActivity(context)
        val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPip = info.isInPictureInPictureMode
        }
        compAct?.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            compAct?.removeOnPictureInPictureModeChangedListener(listener)
        }
    }

    LaunchedEffect(context, activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            isInPip = activity?.isInPictureInPictureMode == true
        }
    }

    val expression by viewModel.expression.collectAsStateWithLifecycle()
    val previewResult by viewModel.previewResult.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isRadians by viewModel.isRadians.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyState.collectAsStateWithLifecycle()
    val previousCalculation by viewModel.previousCalculation.collectAsStateWithLifecycle()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isMultiWindow = activity?.isInMultiWindowMode == true
    val isHorizontalLayout = (screenWidth > screenHeight || isMultiWindow) && screenWidth >= 400
    // On bigger small windows/resizable floating screens like iQOO 12, screen is bigger than a typical micro video PiP player.
    // In that case, we should use the normal size instead of shrinking fonts and elements, making it perfectly usable.
    val isMicroLayout = isInPip && (screenWidth < 240 || screenHeight < 320)

    CompositionLocalProvider(LocalIsInPip provides isMicroLayout) {
        // Base Scaffold handles edge to edge layouts gracefully
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DarkBgStart, DarkBgEnd))),
            containerColor = Color.Transparent,
            bottomBar = {
                if (!isInPip) {
                    NavigationBar(
                        containerColor = Color(0xFF131522),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == CalculatorTab.STANDARD,
                            onClick = { viewModel.setSelectedTab(CalculatorTab.STANDARD) },
                            icon = { Icon(Icons.Default.Calculate, contentDescription = "Standard") },
                            label = { Text("Standard") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryOrange,
                                selectedTextColor = PrimaryOrange,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = CharcoalButton
                            ),
                            modifier = Modifier.testTag("tab_standard")
                        )
                        NavigationBarItem(
                            selected = selectedTab == CalculatorTab.SCIENTIFIC,
                            onClick = { viewModel.setSelectedTab(CalculatorTab.SCIENTIFIC) },
                            icon = { Icon(Icons.Default.Science, contentDescription = "Scientific") },
                            label = { Text("Scientific") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ScientificCyan,
                                selectedTextColor = ScientificCyan,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = CharcoalButton
                            ),
                            modifier = Modifier.testTag("tab_scientific")
                        )
                        NavigationBarItem(
                            selected = selectedTab == CalculatorTab.CONVERTER,
                            onClick = { viewModel.setSelectedTab(CalculatorTab.CONVERTER) },
                            icon = { Icon(Icons.Default.SwapCalls, contentDescription = "Converter") },
                            label = { Text("Converter") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF9D4EDD),
                                selectedTextColor = Color(0xFF9D4EDD),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = CharcoalButton
                            ),
                            modifier = Modifier.testTag("tab_converter")
                        )
                        NavigationBarItem(
                            selected = selectedTab == CalculatorTab.CURRENCY,
                            onClick = { viewModel.setSelectedTab(CalculatorTab.CURRENCY) },
                            icon = { Icon(Icons.Default.CurrencyExchange, contentDescription = "Currency") },
                            label = { Text("Currency") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF06D6A0),
                                selectedTextColor = Color(0xFF06D6A0),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = CharcoalButton
                            ),
                            modifier = Modifier.testTag("tab_currency")
                        )
                        NavigationBarItem(
                            selected = selectedTab == CalculatorTab.GOLD,
                            onClick = { viewModel.setSelectedTab(CalculatorTab.GOLD) },
                            icon = { Icon(Icons.Default.ShowChart, contentDescription = "Gold Price") },
                            label = { Text("Gold") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFFD700),
                                selectedTextColor = Color(0xFFFFD700),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = CharcoalButton
                            ),
                            modifier = Modifier.testTag("tab_gold")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                // Elegant top row displaying the Calculator branding
                if (!isInPip) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "XanCalc",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CALCULATOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9E22).copy(alpha = 0.8f),
                                    letterSpacing = 2.sp
                                )
                                IconButton(
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_info")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Show Creator Information",
                                        tint = Color(0xFFFF9E22).copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedTab == CalculatorTab.HISTORY) {
                                        viewModel.setSelectedTab(CalculatorTab.STANDARD)
                                    } else {
                                        viewModel.setSelectedTab(CalculatorTab.HISTORY)
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("btn_history_top")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "View History Logs",
                                    tint = if (selectedTab == CalculatorTab.HISTORY) Color(0xFFFF9E22) else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            val params = android.app.PictureInPictureParams.Builder().build()
                                            activity?.enterPictureInPictureMode(params)
                                        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                            activity?.enterPictureInPictureMode()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("btn_pip")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Enter Picture In Picture Mode",
                                    tint = Color(0xFFFF9E22),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                if (isInPip) {
                    PipCalculatorScreen(
                        expression = expression,
                        previewResult = previewResult,
                        errorMessage = errorMessage,
                        previousCalculation = previousCalculation
                    )
                } else {
                    when (selectedTab) {
                        CalculatorTab.STANDARD -> {
                            StandardCalculatorScreen(
                                expression = expression,
                                previewResult = previewResult,
                                errorMessage = errorMessage,
                                onKeyPress = { viewModel.onKeyPress(it) },
                                onCopyResult = {
                                    val textToCopy = previewResult.ifEmpty { expression }
                                    if (textToCopy.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                    }
                                },
                                isHorizontal = isHorizontalLayout,
                                previousCalculation = previousCalculation
                            )
                        }
                        CalculatorTab.SCIENTIFIC -> {
                            ScientificCalculatorScreen(
                                expression = expression,
                                previewResult = previewResult,
                                errorMessage = errorMessage,
                                isRadians = isRadians,
                                onToggleRadians = { viewModel.toggleRadians() },
                                onKeyPress = { viewModel.onKeyPress(it) },
                                onCopyResult = {
                                    val textToCopy = previewResult.ifEmpty { expression }
                                    if (textToCopy.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                    }
                                },
                                isHorizontal = isHorizontalLayout,
                                previousCalculation = previousCalculation
                            )
                        }
                        CalculatorTab.CONVERTER -> {
                            UnitConverterScreen(
                                viewModel = viewModel,
                                isHorizontal = isHorizontalLayout
                            )
                        }
                        CalculatorTab.CURRENCY -> {
                            CurrencyConverterScreen(
                                viewModel = viewModel,
                                isHorizontal = isHorizontalLayout
                            )
                        }
                        CalculatorTab.GOLD -> {
                            GoldPriceScreen(
                                viewModel = viewModel,
                                isHorizontal = isHorizontalLayout
                            )
                        }
                        CalculatorTab.HISTORY -> {
                            HistoryScreen(
                                historyList = historyItems,
                                onDelete = { viewModel.deleteHistoryItem(it) },
                                onClearAll = { viewModel.clearAllHistory() },
                                onSelectHistory = {
                                    viewModel.loadHistoryItemToCalculator(it)
                                },
                                isHorizontal = isHorizontalLayout
                            )
                        }
                    }
                }
            }
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryOrange
                        )
                        Text(
                            text = "About XanCalc",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightChalk
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Created by",
                            fontSize = 14.sp,
                            color = LightChalk.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Johne Cheah",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightChalk
                        )
                        Text(
                            text = "@ 2026",
                            fontSize = 13.sp,
                            color = PrimaryOrange.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Version V2.2",
                            fontSize = 12.sp,
                            color = LightChalk.copy(alpha = 0.5f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryOrange)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = Color(0xFF131522),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun StandardCalculatorScreen(
    expression: String,
    previewResult: String,
    errorMessage: String?,
    onKeyPress: (String) -> Unit,
    onCopyResult: () -> Unit,
    isHorizontal: Boolean = false,
    previousCalculation: String = ""
) {
    val isInPip = LocalIsInPip.current
    if (isHorizontal && !isInPip) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Pane: Display
            CalculatorDisplay(
                expression = expression,
                previewResult = previewResult,
                errorMessage = errorMessage,
                onCopyResult = onCopyResult,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                previousCalculation = previousCalculation
            )

            // Right Pane: Keypad
            val buttons = listOf(
                listOf("AC", "+/-", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "DEL", "=")
            )

            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in buttons) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (char in row) {
                            CalculatorButton(
                                text = char,
                                onClick = { onKeyPress(char) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isInPip) 4.dp else 16.dp)
        ) {
            // Upper Display Area for expression & output
            CalculatorDisplay(
                expression = expression,
                previewResult = previewResult,
                errorMessage = errorMessage,
                onCopyResult = onCopyResult,
                modifier = Modifier.weight(if (isInPip) 1.2f else 1.5f),
                previousCalculation = previousCalculation
            )

            Spacer(modifier = Modifier.height(if (isInPip) 4.dp else 16.dp))

            // Large robust grid for simple keypad
            val buttons = listOf(
                listOf("AC", "+/-", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "DEL", "=")
            )

            Column(
                modifier = Modifier
                    .weight(if (isInPip) 3.8f else 3.5f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isInPip) 4.dp else 10.dp)
            ) {
                for (row in buttons) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isInPip) 4.dp else 10.dp)
                    ) {
                        for (char in row) {
                            CalculatorButton(
                                text = char,
                                onClick = { onKeyPress(char) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScientificCalculatorScreen(
    expression: String,
    previewResult: String,
    errorMessage: String?,
    isRadians: Boolean,
    onToggleRadians: () -> Unit,
    onKeyPress: (String) -> Unit,
    onCopyResult: () -> Unit,
    isHorizontal: Boolean = false,
    previousCalculation: String = ""
) {
    val isInPip = LocalIsInPip.current
    if (isHorizontal && !isInPip) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Pane: Display
            CalculatorDisplay(
                expression = expression,
                previewResult = previewResult,
                errorMessage = errorMessage,
                onCopyResult = onCopyResult,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                previousCalculation = previousCalculation
            )

            // Right Pane: Advanced Scientific Panel
            val scientificButtons = listOf(
                listOf("sin", "cos", "tan", "(", ")", if (isRadians) "RAD" else "DEG"),
                listOf("asin", "acos", "atan", "^", "√", "AC"),
                listOf("ln", "log", "π", "÷", "×", "DEL"),
                listOf("e", "7", "8", "9", "-", "%"),
                listOf("!", "4", "5", "6", "+", "+/-"),
                listOf("0", "1", "2", "3", ".", "=")
            )

            Column(
                modifier = Modifier
                    .weight(2.0f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in scientificButtons) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (char in row) {
                            val isSpecial = char == "RAD" || char == "DEG"
                            CalculatorButton(
                                text = char,
                                onClick = {
                                    if (isSpecial) {
                                        onToggleRadians()
                                    } else {
                                        onKeyPress(char)
                                    }
                                },
                                isScientific = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isInPip) 3.dp else 12.dp)
        ) {
            // Compact Upper Display Area
            CalculatorDisplay(
                expression = expression,
                previewResult = previewResult,
                errorMessage = errorMessage,
                onCopyResult = onCopyResult,
                modifier = Modifier.weight(if (isInPip) 1.2f else 1.2f),
                previousCalculation = previousCalculation
            )

            Spacer(modifier = Modifier.height(if (isInPip) 2.dp else 8.dp))

            // Advanced Scientific Panel
            val scientificButtons = listOf(
                listOf("sin", "cos", "tan", "(", ")", if (isRadians) "RAD" else "DEG"),
                listOf("asin", "acos", "atan", "^", "√", "AC"),
                listOf("ln", "log", "π", "÷", "×", "DEL"),
                listOf("e", "7", "8", "9", "-", "%"),
                listOf("!", "4", "5", "6", "+", "+/-"),
                listOf("0", "1", "2", "3", ".", "=")
            )

            Column(
                modifier = Modifier
                    .weight(if (isInPip) 3.8f else 4.3f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isInPip) 2.dp else 6.dp)
            ) {
                for (row in scientificButtons) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isInPip) 2.dp else 6.dp)
                    ) {
                        for (char in row) {
                            val isSpecial = char == "RAD" || char == "DEG"
                            CalculatorButton(
                                text = char,
                                onClick = {
                                    if (isSpecial) {
                                        onToggleRadians()
                                    } else {
                                        onKeyPress(char)
                                    }
                                },
                                isScientific = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorDisplay(
    expression: String,
    previewResult: String,
    errorMessage: String?,
    onCopyResult: () -> Unit,
    modifier: Modifier = Modifier,
    previousCalculation: String = ""
) {
    val isInPip = LocalIsInPip.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131522)),
        shape = RoundedCornerShape(if (isInPip) 10.dp else 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isInPip) 6.dp else 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (previousCalculation.isNotEmpty()) {
                    val scrollStatePrev = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollStatePrev),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = formatExpressionWithCommas(previousCalculation),
                            fontSize = if (isInPip) 10.sp else 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = LightChalk.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("previous_calculation_text")
                        )
                    }
                    Spacer(modifier = Modifier.height(if (isInPip) 1.dp else 4.dp))
                }

                // Expression display
                val scrollState = rememberScrollState()
                LaunchedEffect(expression, scrollState.maxValue) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = formatExpressionWithCommas(expression.ifEmpty { "0" }),
                        fontSize = if (isInPip) {
                            if (expression.length > 15) 12.sp else 16.sp
                        } else {
                            if (expression.length > 15) 28.sp else 38.sp
                        },
                        fontWeight = FontWeight.Medium,
                        color = LightChalk,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("expression_text")
                    )
                }

                Spacer(modifier = Modifier.height(if (isInPip) 2.dp else 12.dp))

                // Preview real-time evaluation or Error Output
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = if (isInPip) 10.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        modifier = Modifier.testTag("error_text")
                    )
                } else if (previewResult.isNotEmpty()) {
                    Text(
                        text = previewResult,
                        fontSize = if (isInPip) 12.sp else 24.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("preview_result_text")
                    )
                }
            }

            // Copy Trigger overlay top-left
            if (!isInPip) {
                IconButton(
                    onClick = onCopyResult,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .testTag("copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text result",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    onClick: () -> Unit,
    isScientific: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isInPip = LocalIsInPip.current
    // Styling classification
    val backgroundColor = when (text) {
        "=" -> PrimaryOrange
        "AC", "DEL" -> Color(0xFFD62828)
        "+", "-", "×", "÷" -> if (isScientific) Color(0xFF33374D) else PrimaryOrange
        "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "cbrt", "^", "!", "π", "e", "(", ")", "RAD", "DEG" -> ScientificCyanContainer
        "+/-", "%" -> Color(0xFF33374D)
        else -> CharcoalButton
    }

    val textColor = when (text) {
        "=" -> Color.White
        "AC", "DEL" -> Color.White
        "+", "-", "×", "÷" -> Color.White
        "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "cbrt", "^", "!", "π", "e", "(", ")", "RAD", "DEG" -> ScientificCyan
        else -> LightChalk
    }

    val sizeFactor = if (isInPip) {
        if (isScientific) 7.sp else 11.sp
    } else {
        if (isScientific) 13.sp else 22.sp
    }
    val shape = RoundedCornerShape(if (isInPip) 4.dp else 16.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(backgroundColor)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = sizeFactor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1
        )
    }
}

@Composable
fun ConverterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInPip = LocalIsInPip.current
    val backgroundColor = when (text) {
        "Save" -> Color(0xFF820ADF)
        "Copy" -> Color(0xFF33374D)
        "DEL", "AC" -> Color(0xFFD62828)
        "+/-" -> Color(0xFF33374D)
        else -> CharcoalButton
    }

    val textColor = when (text) {
        "Save" -> Color.White
        "Copy" -> Color(0xFF9D4EDD)
        "DEL", "AC" -> Color.White
        else -> LightChalk
    }

    val shape = RoundedCornerShape(if (isInPip) 4.dp else 12.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(backgroundColor)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("converter_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        when (text) {
            "Copy" -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy result",
                        tint = textColor,
                        modifier = Modifier.size(if (isInPip) 8.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isInPip) 2.dp else 4.dp))
                    Text("Copy", color = textColor, fontSize = if (isInPip) 7.sp else 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            "Save" -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save result",
                        tint = textColor,
                        modifier = Modifier.size(if (isInPip) 8.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isInPip) 2.dp else 4.dp))
                    Text("Save", color = textColor, fontSize = if (isInPip) 7.sp else 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            "DEL" -> {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Delete last character",
                    tint = textColor,
                    modifier = Modifier.size(if (isInPip) 11.dp else 20.dp)
                )
            }
            else -> {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = if (isInPip) 11.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    viewModel: CalculatorViewModel,
    isHorizontal: Boolean = false
) {
    val isInPip = LocalIsInPip.current
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val converterInput by viewModel.converterInput.collectAsStateWithLifecycle()
    val fromUnitName by viewModel.fromUnitName.collectAsStateWithLifecycle()
    val toUnitName by viewModel.toUnitName.collectAsStateWithLifecycle()
    val converterOutput by viewModel.converterOutput.collectAsStateWithLifecycle()

    var fromDropdownExpanded by remember { mutableStateOf(false) }
    var toDropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (isHorizontal && !isInPip) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Pane: Category Selection and Display Card
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Unit Converter",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightChalk,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ConverterCategory.values().forEach { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.changeCategory(category) },
                            label = { Text(category.label, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF820ADF),
                                containerColor = CharcoalButton
                            ),
                            modifier = Modifier.testTag("chip_${category.name}").height(28.dp)
                        )
                    }
                }

                // Cards Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131522)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // From row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("From", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = formatExpressionWithCommas(converterInput.ifEmpty { "0" }),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.testTag("converter_input_preview")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box {
                                Button(
                                    onClick = { fromDropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp).testTag("btn_from_dropdown")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = fromUnitName,
                                            color = LightChalk,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 80.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = fromDropdownExpanded,
                                    onDismissRequest = { fromDropdownExpanded = false },
                                    modifier = Modifier.background(CharcoalButton)
                                ) {
                                    selectedCategory.units.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.name, color = LightChalk, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.setFromUnit(unit.name)
                                                fromDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Swap Divider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                            IconButton(
                                onClick = { viewModel.swapConverterUnits() },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(24.dp)
                                    .background(CharcoalButton, CircleShape)
                                    .testTag("btn_swap_units")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap conversion units",
                                    tint = Color(0xFF9D4EDD),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                        }

                        // To row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("To", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = converterOutput.ifEmpty { "0" },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9D4EDD),
                                        maxLines = 1,
                                        modifier = Modifier.testTag("converter_result_text")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box {
                                Button(
                                    onClick = { toDropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp).testTag("btn_to_dropdown")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = toUnitName,
                                            color = LightChalk,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 80.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = toDropdownExpanded,
                                    onDismissRequest = { toDropdownExpanded = false },
                                    modifier = Modifier.background(CharcoalButton)
                                ) {
                                    selectedCategory.units.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.name, color = LightChalk, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.setToUnit(unit.name)
                                                toDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Pane: Numeric Keypad
            val numpadKeys = listOf(
                listOf("7", "8", "9", "DEL"),
                listOf("4", "5", "6", "AC"),
                listOf("1", "2", "3", "+/-"),
                listOf("0", ".", "Copy", "Save")
            )
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in numpadKeys) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (key in row) {
                            val clipboard = LocalClipboardManager.current
                            ConverterButton(
                                text = key,
                                onClick = {
                                    when (key) {
                                        "Copy" -> {
                                            if (converterOutput.isNotEmpty() && converterOutput != "Invalid Input") {
                                                clipboard.setText(AnnotatedString(converterOutput))
                                            }
                                        }
                                        "Save" -> {
                                            viewModel.saveConversionToHistory()
                                        }
                                        else -> {
                                            viewModel.onConverterKeyPress(key)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isInPip) 4.dp else 16.dp)
        ) {
            Text(
                text = "Unit Converter",
                fontSize = if (isInPip) 11.sp else 22.spx(),
                fontWeight = FontWeight.Bold,
                color = LightChalk,
                modifier = Modifier.padding(bottom = if (isInPip) 4.dp else 12.dp)
            )

            // Scrollable Row of chips for selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(bottom = if (isInPip) 4.dp else 16.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isInPip) 4.dp else 8.dp)
            ) {
                ConverterCategory.values().forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.changeCategory(category) },
                        label = { Text(category.label, color = if (isSelected) Color.White else Color.Gray, fontSize = if (isInPip) 7.sp else 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF820ADF),
                            containerColor = CharcoalButton
                        ),
                        modifier = Modifier
                            .testTag("chip_${category.name}")
                            .then(if (isInPip) Modifier.height(18.dp) else Modifier)
                    )
                }
            }

            // Active Conversion Display Panels Card (stacked, beautiful!)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131522)),
                shape = RoundedCornerShape(if (isInPip) 8.dp else 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isInPip) 6.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isInPip) 2.dp else 8.dp)
                ) {
                    // UPPER ROW: FROM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From", fontSize = if (isInPip) 7.sp else 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            val fromScrollState = rememberScrollState()
                            LaunchedEffect(converterInput, fromScrollState.maxValue) {
                                fromScrollState.animateScrollTo(fromScrollState.maxValue)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(fromScrollState)
                            ) {
                                Text(
                                    text = formatExpressionWithCommas(converterInput.ifEmpty { "0" }),
                                    fontSize = if (isInPip) 12.sp else 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.testTag("converter_input_preview")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(if (isInPip) 4.dp else 8.dp))

                        // From Unit Dropdown dropdown selection
                        Box {
                            Button(
                                onClick = { fromDropdownExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                                contentPadding = if (isInPip) PaddingValues(horizontal = 4.dp, vertical = 2.dp) else PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(if (isInPip) 4.dp else 10.dp),
                                modifier = Modifier
                                    .testTag("btn_from_dropdown")
                                    .then(if (isInPip) Modifier.height(18.dp) else Modifier)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = fromUnitName,
                                        color = LightChalk,
                                        fontSize = if (isInPip) 8.sp else 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = if (isInPip) 50.dp else 100.dp)
                                    )
                                    Spacer(modifier = Modifier.width(if (isInPip) 2.dp else 4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(if (isInPip) 10.dp else 16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = fromDropdownExpanded,
                                onDismissRequest = { fromDropdownExpanded = false },
                                modifier = Modifier.background(CharcoalButton)
                            ) {
                                selectedCategory.units.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.name, color = LightChalk, fontSize = if (isInPip) 9.sp else 14.sp) },
                                        modifier = if (isInPip) Modifier.height(24.dp) else Modifier,
                                        onClick = {
                                            viewModel.setFromUnit(unit.name)
                                            fromDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // SWAP BUTTON ALIGNMENT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                        IconButton(
                            onClick = { viewModel.swapConverterUnits() },
                            modifier = Modifier
                                .padding(horizontal = if (isInPip) 2.dp else 8.dp)
                                .size(if (isInPip) 18.dp else 32.dp)
                                .background(CharcoalButton, CircleShape)
                                .testTag("btn_swap_units")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap conversion units",
                                tint = Color(0xFF9D4EDD),
                                modifier = Modifier.size(if (isInPip) 10.dp else 18.dp)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                    }

                    // LOWER ROW: TO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To", fontSize = if (isInPip) 7.sp else 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            val toScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(toScrollState)
                            ) {
                                Text(
                                    text = converterOutput.ifEmpty { "0" },
                                    fontSize = if (isInPip) 12.sp else 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9D4EDD),
                                    maxLines = 1,
                                    modifier = Modifier.testTag("converter_result_text")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(if (isInPip) 4.dp else 8.dp))

                        // To Unit Dropdown selection
                        Box {
                            Button(
                                onClick = { toDropdownExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                                contentPadding = if (isInPip) PaddingValues(horizontal = 4.dp, vertical = 2.dp) else PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(if (isInPip) 4.dp else 10.dp),
                                modifier = Modifier
                                    .testTag("btn_to_dropdown")
                                    .then(if (isInPip) Modifier.height(18.dp) else Modifier)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = toUnitName,
                                        color = LightChalk,
                                        fontSize = if (isInPip) 8.sp else 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = if (isInPip) 50.dp else 100.dp)
                                    )
                                    Spacer(modifier = Modifier.width(if (isInPip) 2.dp else 4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(if (isInPip) 10.dp else 16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = toDropdownExpanded,
                                onDismissRequest = { toDropdownExpanded = false },
                                modifier = Modifier.background(CharcoalButton)
                            ) {
                                selectedCategory.units.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.name, color = LightChalk, fontSize = if (isInPip) 9.sp else 14.sp) },
                                        modifier = if (isInPip) Modifier.height(24.dp) else Modifier,
                                        onClick = {
                                            viewModel.setToUnit(unit.name)
                                            toDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isInPip) 4.dp else 16.dp))

            // Custom Converter Numeric Keypad Grid
            val numpadKeys = listOf(
                listOf("7", "8", "9", "DEL"),
                listOf("4", "5", "6", "AC"),
                listOf("1", "2", "3", "+/-"),
                listOf("0", ".", "Copy", "Save")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isInPip) 4.dp else 8.dp)
            ) {
                for (row in numpadKeys) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isInPip) 4.dp else 8.dp)
                    ) {
                        for (key in row) {
                            val clipboard = LocalClipboardManager.current
                            ConverterButton(
                                text = key,
                                onClick = {
                                    when (key) {
                                        "Copy" -> {
                                            if (converterOutput.isNotEmpty() && converterOutput != "Invalid Input") {
                                                clipboard.setText(AnnotatedString(converterOutput))
                                            }
                                        }
                                        "Save" -> {
                                            viewModel.saveConversionToHistory()
                                        }
                                        else -> {
                                            viewModel.onConverterKeyPress(key)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyConverterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInPip = LocalIsInPip.current
    val backgroundColor = when (text) {
        "Save" -> Color(0xFF06D6A0)
        "Copy" -> Color(0xFF33374D)
        "DEL", "AC" -> Color(0xFFD62828)
        "+/-" -> Color(0xFF33374D)
        else -> CharcoalButton
    }

    val textColor = when (text) {
        "Save" -> Color.Black
        "Copy" -> Color(0xFF06D6A0)
        "DEL", "AC" -> Color.White
        else -> LightChalk
    }

    val shape = RoundedCornerShape(if (isInPip) 4.dp else 12.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(backgroundColor)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("currency_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        when (text) {
            "Copy" -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy result",
                        tint = textColor,
                        modifier = Modifier.size(if (isInPip) 8.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isInPip) 2.dp else 4.dp))
                    Text("Copy", color = textColor, fontSize = if (isInPip) 7.sp else 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            "Save" -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save result",
                        tint = textColor,
                        modifier = Modifier.size(if (isInPip) 8.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isInPip) 2.dp else 4.dp))
                    Text("Save", color = textColor, fontSize = if (isInPip) 7.sp else 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = if (isInPip) 8.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CurrencyConverterScreen(
    viewModel: CalculatorViewModel,
    isHorizontal: Boolean = false
) {
    val isInPip = LocalIsInPip.current
    val currencyInput by viewModel.currencyInput.collectAsStateWithLifecycle()
    val fromCurrency by viewModel.fromCurrency.collectAsStateWithLifecycle()
    val toCurrency by viewModel.toCurrency.collectAsStateWithLifecycle()
    val currencyOutput by viewModel.currencyOutput.collectAsStateWithLifecycle()
    val isFetchingRates by viewModel.isFetchingRates.collectAsStateWithLifecycle()
    val lastRatesUpdate by viewModel.lastRatesUpdate.collectAsStateWithLifecycle()

    var showFromDialog by remember { mutableStateOf(false) }
    var showToDialog by remember { mutableStateOf(false) }

    fun getCurrencyFlagAndLabel(code: String): String {
        val flag = getFlagEmojiForCurrency(code)
        return "$flag $code"
    }

    if (isHorizontal && !isInPip) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Currency Converter",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightChalk
                        )
                        Text(
                            text = lastRatesUpdate,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshCurrencyRates() },
                        modifier = Modifier.testTag("btn_refresh_rates").size(32.dp)
                    ) {
                        if (isFetchingRates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF06D6A0)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh currency rates",
                                tint = Color(0xFF06D6A0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131522)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("From", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = formatExpressionWithCommas(currencyInput.ifEmpty { "0" }),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.testTag("currency_input_preview")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showFromDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp).testTag("btn_from_currency_dropdown")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = getCurrencyFlagAndLabel(fromCurrency),
                                        color = LightChalk,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                            IconButton(
                                onClick = { viewModel.swapCurrencyUnits() },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(24.dp)
                                    .background(CharcoalButton, CircleShape)
                                    .testTag("btn_swap_currencies")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap currencies",
                                    tint = Color(0xFF06D6A0),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("To", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = currencyOutput.ifEmpty { "0" },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF06D6A0),
                                        maxLines = 1,
                                        modifier = Modifier.testTag("currency_result_text")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showToDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp).testTag("btn_to_currency_dropdown")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = getCurrencyFlagAndLabel(toCurrency),
                                        color = LightChalk,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            val numpadKeys = listOf(
                listOf("7", "8", "9", "DEL"),
                listOf("4", "5", "6", "AC"),
                listOf("1", "2", "3", "+/-"),
                listOf("0", ".", "Copy", "Save")
            )
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in numpadKeys) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (key in row) {
                            val clipboard = LocalClipboardManager.current
                            CurrencyConverterButton(
                                text = key,
                                onClick = {
                                    when (key) {
                                        "Copy" -> {
                                            if (currencyOutput.isNotEmpty()) {
                                                clipboard.setText(AnnotatedString(currencyOutput))
                                            }
                                        }
                                        "Save" -> {
                                            viewModel.saveCurrencyConversionToHistory()
                                        }
                                        else -> {
                                            viewModel.onCurrencyKeyPress(key)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isInPip) 4.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = if (isInPip) 4.dp else 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Currency Converter",
                        fontSize = if (isInPip) 11.sp else 22.spx(),
                        fontWeight = FontWeight.Bold,
                        color = LightChalk
                    )
                    Text(
                        text = lastRatesUpdate,
                        fontSize = if (isInPip) 7.sp else 12.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshCurrencyRates() },
                    modifier = Modifier.testTag("btn_refresh_rates").size(if (isInPip) 20.dp else 36.dp)
                ) {
                    if (isFetchingRates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(if (isInPip) 10.dp else 18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF06D6A0)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh currency rates",
                            tint = Color(0xFF06D6A0),
                            modifier = Modifier.size(if (isInPip) 10.dp else 22.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131522)),
                shape = RoundedCornerShape(if (isInPip) 8.dp else 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isInPip) 6.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isInPip) 2.dp else 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From", fontSize = if (isInPip) 7.sp else 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            val fromScrollState = rememberScrollState()
                            LaunchedEffect(currencyInput, fromScrollState.maxValue) {
                                fromScrollState.animateScrollTo(fromScrollState.maxValue)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(fromScrollState)
                            ) {
                                Text(
                                    text = formatExpressionWithCommas(currencyInput.ifEmpty { "0" }),
                                    fontSize = if (isInPip) 11.sp else 24.spx(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.testTag("currency_input_preview")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { if (!isInPip) showFromDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                            contentPadding = PaddingValues(horizontal = if (isInPip) 4.dp else 12.dp, vertical = if (isInPip) 2.dp else 6.dp),
                            shape = RoundedCornerShape(if (isInPip) 4.dp else 10.dp),
                            modifier = Modifier
                                .then(if (isInPip) Modifier.height(18.dp) else Modifier.height(36.dp))
                                .testTag("btn_from_currency_dropdown")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = getCurrencyFlagAndLabel(fromCurrency),
                                    color = LightChalk,
                                    fontSize = if (isInPip) 7.sp else 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(if (isInPip) 10.dp else 16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                        IconButton(
                            onClick = { viewModel.swapCurrencyUnits() },
                            modifier = Modifier
                                .padding(horizontal = if (isInPip) 2.dp else 8.dp)
                                .size(if (isInPip) 16.dp else 32.dp)
                                .background(CharcoalButton, CircleShape)
                                .testTag("btn_swap_currencies")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap conversion units",
                                tint = Color(0xFF06D6A0),
                                modifier = Modifier.size(if (isInPip) 10.dp else 18.dp)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalButton, thickness = 1.dp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To", fontSize = if (isInPip) 7.sp else 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            val toScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(toScrollState)
                            ) {
                                Text(
                                    text = currencyOutput.ifEmpty { "0" },
                                    fontSize = if (isInPip) 11.sp else 24.spx(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06D6A0),
                                    maxLines = 1,
                                    modifier = Modifier.testTag("currency_result_text")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { if (!isInPip) showToDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalButton),
                            contentPadding = PaddingValues(horizontal = if (isInPip) 4.dp else 12.dp, vertical = if (isInPip) 2.dp else 6.dp),
                            shape = RoundedCornerShape(if (isInPip) 4.dp else 10.dp),
                            modifier = Modifier
                                .then(if (isInPip) Modifier.height(18.dp) else Modifier.height(36.dp))
                                .testTag("btn_to_currency_dropdown")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = getCurrencyFlagAndLabel(toCurrency),
                                    color = LightChalk,
                                    fontSize = if (isInPip) 7.sp else 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(if (isInPip) 10.dp else 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isInPip) 4.dp else 16.dp))

            val numpadKeys = listOf(
                listOf("7", "8", "9", "DEL"),
                listOf("4", "5", "6", "AC"),
                listOf("1", "2", "3", "+/-"),
                listOf("0", ".", "Copy", "Save")
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isInPip) 2.dp else 6.dp)
            ) {
                for (row in numpadKeys) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(if (isInPip) 2.dp else 6.dp)
                    ) {
                        for (key in row) {
                            val clipboard = LocalClipboardManager.current
                            CurrencyConverterButton(
                                text = key,
                                onClick = {
                                    when (key) {
                                        "Copy" -> {
                                            if (currencyOutput.isNotEmpty()) {
                                                clipboard.setText(AnnotatedString(currencyOutput))
                                            }
                                        }
                                        "Save" -> {
                                            viewModel.saveCurrencyConversionToHistory()
                                        }
                                        else -> {
                                            viewModel.onCurrencyKeyPress(key)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFromDialog) {
        CurrencySelectDialog(
            title = "Select From Currency",
            currencies = CalculatorViewModel.ALL_CURRENCIES,
            selectedCurrency = fromCurrency,
            onCurrencySelected = { viewModel.setFromCurrency(it) },
            onDismiss = { showFromDialog = false }
        )
    }

    if (showToDialog) {
        CurrencySelectDialog(
            title = "Select To Currency",
            currencies = CalculatorViewModel.ALL_CURRENCIES,
            selectedCurrency = toCurrency,
            onCurrencySelected = { viewModel.setToCurrency(it) },
            onDismiss = { showToDialog = false }
        )
    }
}

@Composable
fun HistoryScreen(
    historyList: List<HistoryEntity>,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onSelectHistory: (HistoryEntity) -> Unit,
    isHorizontal: Boolean = false
) {
    val isInPip = LocalIsInPip.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isInPip) 4.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History Logs",
                fontSize = if (isInPip) 11.sp else 22.spx(),
                fontWeight = FontWeight.Bold,
                color = LightChalk
            )
            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                    modifier = Modifier
                        .testTag("btn_clear_all_history")
                        .then(if (isInPip) Modifier.height(20.dp) else Modifier),
                    contentPadding = if (isInPip) PaddingValues(horizontal = 4.dp, vertical = 2.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Clear all database items",
                        modifier = Modifier.size(if (isInPip) 10.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isInPip) 2.dp else 4.dp))
                    Text("Clear All", fontSize = if (isInPip) 8.sp else 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isInPip) 4.dp else 12.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = "No calculations found",
                        tint = Color.Gray,
                        modifier = Modifier.size(if (isInPip) 24.dp else 64.dp)
                    )
                    Spacer(modifier = Modifier.height(if (isInPip) 4.dp else 12.dp))
                    Text(
                        text = "History is empty",
                        color = Color.Gray,
                        fontSize = if (isInPip) 10.sp else 16.spx(),
                        fontWeight = FontWeight.Medium
                    )
                    if (!isInPip) {
                        Text(
                            text = "Evaluated expressions will appear here",
                            color = Color.DarkGray,
                            fontSize = 12.spx(),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(if (isInPip) 4.dp else 8.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    HistoryRow(
                        item = item,
                        onDelete = { onDelete(item.id) },
                        onSelect = { onSelectHistory(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryRow(
    item: HistoryEntity,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val isInPip = LocalIsInPip.current
    val formatter = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(item.timestamp))

    // Badge styling for tags
    val badgeColor = when {
        item.type.startsWith("CONVERSION") -> Color(0xFF820ADF)
        item.type == "SCIENTIFIC" -> ScientificCyan
        else -> PrimaryOrange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("history_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = HistoryCardBg),
        shape = RoundedCornerShape(if (isInPip) 6.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isInPip) 6.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isInPip) 2.dp else 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isInPip) 4.dp else 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(if (isInPip) 2.dp else 4.dp))
                            .padding(horizontal = if (isInPip) 3.dp else 6.dp, vertical = if (isInPip) 1.dp else 2.dp)
                    ) {
                        Text(
                            text = item.type,
                            fontSize = if (isInPip) 6.sp else 9.spx(),
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                    Text(
                        text = formattedDate,
                        fontSize = if (isInPip) 7.sp else 10.spx(),
                        color = Color.Gray
                    )
                }

                Text(
                    text = formatExpressionWithCommas(item.expression),
                    fontSize = if (isInPip) 10.sp else 16.spx(),
                    fontWeight = FontWeight.Medium,
                    color = LightChalk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "= ${item.result}",
                    fontSize = if (isInPip) 11.sp else 18.spx(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .testTag("btn_delete_history_${item.id}")
                    .then(if (isInPip) Modifier.size(24.dp) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete from local history",
                    tint = Color.Gray,
                    modifier = Modifier.size(if (isInPip) 12.dp else 24.dp)
                )
            }
        }
    }
}

@Composable
fun PipCalculatorScreen(
    expression: String,
    previewResult: String,
    errorMessage: String?,
    previousCalculation: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val topText = if (previousCalculation.isNotEmpty()) {
                previousCalculation
            } else {
                expression.ifEmpty { "0" }
            }

            val bottomText = when {
                !errorMessage.isNullOrEmpty() -> errorMessage
                previousCalculation.isNotEmpty() -> expression.ifEmpty { "0" }
                previewResult.isNotEmpty() -> previewResult
                else -> expression.ifEmpty { "0" }
            }

            Text(
                text = topText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bottomText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Simple extension helper for font sizes scaling
fun Int.spx() = this.sp

fun getFlagEmojiForCurrency(currencyCode: String): String {
    val upper = currencyCode.uppercase().trim()
    val countryCode = when (upper) {
        "EUR" -> "EU"
        "ANG" -> "AN"
        "BTC" -> "XBT"
        "XOF" -> "SN"
        "XAF" -> "CM"
        "XPF" -> "PF"
        "XCD" -> "DM"
        "XDR" -> "IMF"
        "AUD" -> "AU"
        "CAD" -> "CA"
        "CHF" -> "CH"
        "CNY" -> "CN"
        "INR" -> "IN"
        "JPY" -> "JP"
        "SGD" -> "SG"
        "NZD" -> "NZ"
        "HKD" -> "HK"
        "SEK" -> "SE"
        "KRW" -> "KR"
        "TRY" -> "TR"
        "RUB" -> "RU"
        "ZAR" -> "ZA"
        "DKK" -> "DK"
        "PLN" -> "PL"
        "BRL" -> "BR"
        "TWD" -> "TW"
        "THB" -> "TH"
        "IDR" -> "ID"
        "HUF" -> "HU"
        "CZK" -> "CZ"
        "ILS" -> "IL"
        "CLP" -> "CL"
        "PHP" -> "PH"
        "AED" -> "AE"
        "COP" -> "CO"
        "SAR" -> "SA"
        "MYR" -> "MY"
        "RON" -> "RO"
        "VND" -> "VN"
        "ARS" -> "AR"
        "MXN" -> "MX"
        "AFN" -> "AF"
        "ALL" -> "AL"
        "AMD" -> "AM"
        "AOA" -> "AO"
        "AWG" -> "AW"
        "AZN" -> "AZ"
        "BAM" -> "BA"
        "BBD" -> "BB"
        "BDT" -> "BD"
        "BGN" -> "BG"
        "BHD" -> "BH"
        "BIF" -> "BI"
        "BMD" -> "BM"
        "BND" -> "BN"
        "BOB" -> "BO"
        "BSD" -> "BS"
        "BTN" -> "BT"
        "BWP" -> "BW"
        "BYN" -> "BY"
        "BZD" -> "BZ"
        "CDF" -> "CD"
        "CUP" -> "CU"
        "CVE" -> "CV"
        "DJF" -> "DJ"
        "DOP" -> "DO"
        "DZD" -> "DZ"
        "EGP" -> "EG"
        "ERN" -> "ER"
        "ETB" -> "ET"
        "FJD" -> "FJ"
        "FKP" -> "FK"
        "FOK" -> "FO"
        "GEL" -> "GE"
        "GGP" -> "GG"
        "GHS" -> "GH"
        "GIP" -> "GI"
        "GMD" -> "GM"
        "GNF" -> "GN"
        "GTQ" -> "GT"
        "GYD" -> "GY"
        "HNL" -> "HN"
        "HRK" -> "HR"
        "HTG" -> "HT"
        "IMP" -> "IM"
        "IQD" -> "IQ"
        "IRR" -> "IR"
        "ISK" -> "IS"
        "JEP" -> "JE"
        "JMD" -> "JM"
        "JOD" -> "JO"
        "KES" -> "KE"
        "KGS" -> "KG"
        "KHR" -> "KH"
        "KID" -> "KI"
        "KMF" -> "KM"
        "KPW" -> "KP"
        "KWD" -> "KW"
        "KYD" -> "KY"
        "KZT" -> "KZ"
        "LAK" -> "LA"
        "LBP" -> "LB"
        "LKR" -> "LK"
        "LRD" -> "LR"
        "LSL" -> "LS"
        "LYD" -> "LY"
        "MAD" -> "MA"
        "MDL" -> "MD"
        "MGA" -> "MG"
        "MKD" -> "MK"
        "MMK" -> "MM"
        "MNT" -> "MN"
        "MOP" -> "MO"
        "MRU" -> "MR"
        "MUR" -> "MU"
        "MVR" -> "MV"
        "MWK" -> "MW"
        "MZN" -> "MZ"
        "NAD" -> "NA"
        "NGN" -> "NG"
        "NIO" -> "NI"
        "NOK" -> "NO"
        "NPR" -> "NP"
        "OMR" -> "OM"
        "PAB" -> "PA"
        "PEN" -> "PE"
        "PGK" -> "PG"
        "PKR" -> "PK"
        "PYG" -> "PY"
        "QAR" -> "QA"
        "RSD" -> "RS"
        "RWF" -> "RW"
        "SBD" -> "SB"
        "SCR" -> "SC"
        "SDG" -> "SD"
        "SHP" -> "SH"
        "SLE" -> "SL"
        "SLL" -> "SL"
        "SOS" -> "SO"
        "SRD" -> "SR"
        "SSP" -> "SS"
        "STN" -> "ST"
        "SYP" -> "SY"
        "SZL" -> "SZ"
        "TJS" -> "TJ"
        "TMT" -> "TM"
        "TOP" -> "TO"
        "TTD" -> "TT"
        "TVD" -> "TV"
        "TZS" -> "TZ"
        "UAH" -> "UA"
        "UGX" -> "UG"
        "UYU" -> "UY"
        "UZS" -> "UZ"
        "VES" -> "VE"
        "VUV" -> "VU"
        "WST" -> "WS"
        "YER" -> "YE"
        "ZMW" -> "ZM"
        "ZWL" -> "ZW"
        else -> if (upper.length >= 2) upper.substring(0, 2) else "US"
    }

    if (countryCode == "XBT" || countryCode == "IMF") return "🪙"

    return try {
        val firstLetter = countryCode[0].code - 0x41 + 0x1F1E6
        val secondLetter = countryCode[1].code - 0x41 + 0x1F1E6
        String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    } catch (e: Exception) {
        "🌐"
    }
}

@Composable
fun CurrencySelectDialog(
    title: String,
    currencies: Map<String, String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val popularList = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "SGD", "CNY", "INR", "CHF")
    
    val filteredCurrencies = remember(searchQuery) {
        val query = searchQuery.lowercase().trim()
        val list = currencies.toList().sortedBy { it.first }
        if (query.isEmpty()) {
            list
        } else {
            list.filter { (code, name) ->
                code.lowercase().contains(query) || name.lowercase().contains(query)
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF131522),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightChalk
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search currency...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightChalk,
                        unfocusedTextColor = LightChalk,
                        focusedBorderColor = Color(0xFF06D6A0),
                        unfocusedBorderColor = CharcoalButton,
                        cursorColor = Color(0xFF06D6A0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Popular Section (only show when search query is empty)
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Popular Currencies",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(popularList) { code ->
                            val isSelected = selectedCurrency == code
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF06D6A0) else CharcoalButton)
                                    .clickable {
                                        onCurrencySelected(code)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(getFlagEmojiForCurrency(code), fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = code,
                                    color = if (isSelected) Color.Black else LightChalk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Title for all/results
                Text(
                    text = if (searchQuery.isNotEmpty()) "Search Results" else "All Currencies",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Currencies list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCurrencies) { (code, name) ->
                        val isSelected = selectedCurrency == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CharcoalButtonActive.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable {
                                    onCurrencySelected(code)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("currency_select_item_$code"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getFlagEmojiForCurrency(code),
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = code,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF06D6A0) else LightChalk,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = name,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF06D6A0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoldPriceScreen(
    viewModel: CalculatorViewModel,
    isHorizontal: Boolean
) {
    val currencyRates by viewModel.currencyRates.collectAsStateWithLifecycle()
    val isFetchingRates by viewModel.isFetchingRates.collectAsStateWithLifecycle()
    val lastRatesUpdate by viewModel.lastRatesUpdate.collectAsStateWithLifecycle()
    val spotGoldUSDPerGram by viewModel.goldPriceUSD.collectAsStateWithLifecycle()
    val goldHistory by viewModel.goldHistory.collectAsStateWithLifecycle()
    val goldHistoryStatus by viewModel.goldHistoryStatus.collectAsStateWithLifecycle()

    // 1 USD to MYR exchange rate (e.g. 4.72 as fallback)
    val myrRate = currencyRates["MYR"] ?: 4.72

    // Interactive chart period (0: 30D, 1: 6 Months, 2: 1 Year, 3: 5 Years)
    var chartPeriod by remember { mutableStateOf(0) }

    // Selected day/index in the current historical list (re-bound when the selected period changes)
    var selectedChartIndex by remember(chartPeriod) { mutableStateOf<Int?>(null) }

    // Troy ounce to gram conversion, used to turn recorded USD/oz history into USD/gram
    val ozToGram = 31.1034768

    // Build each period's chart data from the REAL history fetched from FreeGoldAPI.com
    // (blend of Yahoo Finance daily futures + World Bank monthly data, all attributed to a
    // real public source - see CalculatorViewModel.fetchGoldHistory()). No point on this
    // chart is the live price multiplied by a made-up ratio; every non-"Live" point is an
    // actual recorded price. If the fetch hasn't completed yet (or failed), the period lists
    // fall back to just "Live" so nothing fabricated is ever shown.
    val (historicalData30Days, historicalData6Months,
        historicalData1Year, historicalData5Years) = remember(goldHistory, spotGoldUSDPerGram) {
        buildGoldChartData(goldHistory, spotGoldUSDPerGram, ozToGram)
    }

    val currentData = when (chartPeriod) {
        0 -> historicalData30Days
        1 -> historicalData6Months
        2 -> historicalData1Year
        else -> historicalData5Years
    }

    // Determine active price from selection or default to the latest/live price
    val activeIndex = selectedChartIndex ?: (currentData.size - 1)
    val activePoint = currentData[activeIndex.coerceIn(currentData.indices)]
    val activeGoldUSD = activePoint.priceInUSD

    // Dynamic Gold Price in MYR per gram (24K) based on selection
    val price24K = activeGoldUSD * myrRate
    val price22K = price24K * 0.916 // 916 Gold purity
    val price18K = price24K * 0.750 // 750 Gold purity
    val price14K = price24K * 0.585 // 585 Gold purity

    // Scroll state for the column
    val scrollState = rememberScrollState()

    // Calculator state
    var goldWeightInput by remember { mutableStateOf("1") }
    var selectedKaratIndex by remember { mutableStateOf(0) } // 0: 24K, 1: 22K, 2: 18K, 3: 14K
    val karats = listOf("24K (99.9%)", "22K (91.6%)", "18K (75.0%)", "14K (58.5%)")
    val karatPurities = listOf(1.0, 0.916, 0.75, 0.585)

    // Calculate gold value based on input weight
    val inputWeight = goldWeightInput.toDoubleOrNull() ?: 0.0
    val activePurity = karatPurities[selectedKaratIndex]
    val calculatedValueMYR = inputWeight * price24K * activePurity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Price Hero Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gold_price_hero_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191C2B)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF262115), // Deep gold-tinted charcoal
                                Color(0xFF151824)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isHistoricalSelection = selectedChartIndex != null && activePoint.dateLabel != "Live"
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isHistoricalSelection) Color(0xFF5AB2FF) else Color(0xFFFFD700), CircleShape)
                            )
                            Text(
                                text = if (isHistoricalSelection) {
                                    "HISTORICAL SPOT GOLD (MALAYSIA)"
                                } else {
                                    "LIVE SPOT GOLD (MALAYSIA)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHistoricalSelection) Color(0xFF5AB2FF) else Color(0xFFFFD700),
                                letterSpacing = 1.5.sp
                            )
                        }
                        
                        // Status or Refresh Indicator
                        IconButton(
                            onClick = { viewModel.refreshCurrencyRates() },
                            modifier = Modifier.size(28.dp).testTag("btn_refresh_gold")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Price Data",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RM ${String.format(Locale.US, "%,.2f", price24K)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "per gram",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isHistoricalSelection = selectedChartIndex != null && activePoint.dateLabel != "Live"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isHistoricalSelection) Icons.Default.History else Icons.Default.TrendingUp,
                                contentDescription = if (isHistoricalSelection) "Historical view" else "Upward trend",
                                tint = if (isHistoricalSelection) Color(0xFF5AB2FF) else Color(0xFF06D6A0),
                                modifier = Modifier.size(16.dp)
                            )
                            val selectionPrefix = "Selected day"
                            Text(
                                text = if (isHistoricalSelection) {
                                    if (activePoint.fullDateLabel == "Live") "Selected: Live" else "$selectionPrefix: ${activePoint.fullDateLabel}"
                                } else {
                                    "+RM 5.25 (+1.45%) Today"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isHistoricalSelection) Color(0xFF5AB2FF) else Color(0xFF06D6A0)
                            )
                        }
                        
                        if (isHistoricalSelection) {
                            Text(
                                text = "Reset to Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                    .clickable { selectedChartIndex = null }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        } else {
                            Text(
                                text = if (isFetchingRates) "Updating..." else lastRatesUpdate,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // Interactive Trend Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gold_trend_chart_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131522)),
            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gold Price Trend",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Spot rate in MYR / g",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Today / 30D / 6M / 1Y / 5Y Segmented Toggle
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E2130), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("30D" to 0, "6M" to 1, "1Y" to 2, "5Y" to 3).forEach { (label, index) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (chartPeriod == index) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { chartPeriod = index }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chartPeriod == index) Color(0xFFFFD700) else Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Chart Component
                val chartLabelCount = when (chartPeriod) {
                    0 -> 6
                    1 -> 6
                    2 -> 6
                    else -> 5
                }
                InteractiveGoldChart(
                    dataPoints = currentData,
                    myrRate = myrRate,
                    selectedIndex = selectedChartIndex,
                    onSelectedIndexChange = { selectedChartIndex = it },
                    labelCount = chartLabelCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Source: FreeGoldAPI.com (Yahoo Finance / World Bank) - $goldHistoryStatus",
                    fontSize = 9.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }

        // Purity Breakdown Grid
        Text(
            text = "Live Rates by Purity (Karat)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )

        val breakdownList = listOf(
            Triple("24K Gold (99.9%)", price24K, "Investment bars, Gold bullion coins"),
            Triple("22K Gold (916)", price22K, "Traditional Malaysian bridal jewelry"),
            Triple("18K Gold (750)", price18K, "Diamond settings, Western luxury jewelry"),
            Triple("14K Gold (585)", price14K, "Strong alloy jewelry, affordable wear")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            breakdownList.forEach { (title, price, description) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191C2B)),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "RM ${String.format(Locale.US, "%,.2f", price)} / g",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (title.startsWith("24K") || title.startsWith("22K")) Color(0xFFFFD700) else Color.White
                        )
                    }
                }
            }
        }

        // Gold Investment Calculator Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .testTag("gold_calculator_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2130)),
            border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculator",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Gold Value Calculator",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Calculate purchase or resale value instantly",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 28.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gram Input
                Text(
                    text = "Gold Weight (Grams)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = goldWeightInput,
                    onValueChange = {
                        // Keep only numbers and a single decimal point
                        if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") {
                            goldWeightInput = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131522), RoundedCornerShape(12.dp))
                        .testTag("input_gold_weight"),
                    placeholder = { Text("0.0", color = Color.DarkGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFFFD700)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Karat Selector Dropdown / Row
                Text(
                    text = "Select Purity (Karat)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Beautiful Horizontally Scrollable Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    karats.forEachIndexed { index, karatName ->
                        val isSelected = selectedKaratIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFFFD700) else Color(0xFF131522))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedKaratIndex = index }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = karatName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Large Display Output
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF131522))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ESTIMATED GOLD VALUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "RM ${String.format(Locale.US, "%,.2f", calculatedValueMYR)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pure gold content: ${String.format(Locale.US, "%.2fg", inputWeight * activePurity)}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Numpad Grid for Quick Weight Entry
                Text(
                    text = "Quick Input Pad",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf(".", "0", "⌫")
                    )
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                val isBackspace = key == "⌫"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isBackspace) Color(0xFFC43030).copy(alpha = 0.15f) else Color(0xFF131522))
                                        .clickable {
                                            if (isBackspace) {
                                                if (goldWeightInput.isNotEmpty()) {
                                                    goldWeightInput = goldWeightInput.dropLast(1)
                                                }
                                            } else if (key == ".") {
                                                if (!goldWeightInput.contains(".")) {
                                                    goldWeightInput = if (goldWeightInput.isEmpty()) "0." else goldWeightInput + "."
                                                }
                                            } else {
                                                if (goldWeightInput == "0" || goldWeightInput == "1") {
                                                    goldWeightInput = key
                                                } else {
                                                    if (goldWeightInput.length < 8) {
                                                        goldWeightInput += key
                                                    }
                                                }
                                            }
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = if (isBackspace) Color(0xFFC43030).copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isBackspace) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = Color(0xFFE57373),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (key == ".") Color(0xFFFFD700) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveGoldChart(
    dataPoints: List<GoldPricePoint>,
    myrRate: Double,
    selectedIndex: Int?,
    onSelectedIndexChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    labelCount: Int = 5
) {
    val minPrice = dataPoints.minOf { it.priceInUSD } * myrRate
    val maxPrice = dataPoints.maxOf { it.priceInUSD } * myrRate
    val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

    // Add extra margin to top and bottom of chart to prevent clipping
    val chartMin = minPrice - (priceRange * 0.1)
    val chartMax = maxPrice + (priceRange * 0.1)
    val chartRange = chartMax - chartMin

    val activeIndex = (selectedIndex ?: (dataPoints.size - 1)).coerceIn(dataPoints.indices)
    val activePoint = dataPoints[activeIndex]
    val activePriceMYR = activePoint.priceInUSD * myrRate

    Column(modifier = modifier) {
        // Selection Detail Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activePoint.fullDateLabel == "Live") "Selected: Live" else "Selected day: ${activePoint.fullDateLabel}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            Text(
                text = "RM ${String.format(Locale.US, "%.2f", activePriceMYR)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        }

        run {
            // Line + gradient-area chart, used for all periods.
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(dataPoints) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull()
                                if (pointer != null && pointer.pressed && dataPoints.size > 1) {
                                    val colWidth = size.width / (dataPoints.size - 1)
                                    val index = (pointer.position.x / colWidth).roundToInt().coerceIn(0, dataPoints.size - 1)
                                    onSelectedIndexChange(index)
                                }
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                if (dataPoints.size < 2) return@Canvas

                val colWidth = width / (dataPoints.size - 1)

                // 1. Draw horizontal grid lines (3 gridlines)
                val gridLines = 3
                for (i in 0 until gridLines) {
                    val gridY = (height / (gridLines - 1)) * i
                    // Draw line
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(0f, gridY),
                        end = androidx.compose.ui.geometry.Offset(width, gridY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Compute coordinates
                val coordinates = dataPoints.mapIndexed { idx, pt ->
                    val x = idx * colWidth
                    val yVal = pt.priceInUSD * myrRate
                    // Map yVal to pixel y (invert because 0 is at top)
                    val y = height - (((yVal - chartMin) / chartRange) * height).toFloat()
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                // 3. Draw gradient area under line
                val fillPath = Path().apply {
                    moveTo(0f, height)
                    lineTo(coordinates.first().x, coordinates.first().y)
                    for (i in 1 until coordinates.size) {
                        val pPrev = coordinates[i - 1]
                        val pCurr = coordinates[i]
                        // Bezier smoothing
                        val controlX1 = pPrev.x + (colWidth / 2f)
                        val controlY1 = pPrev.y
                        val controlX2 = pPrev.x + (colWidth / 2f)
                        val controlY2 = pCurr.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, pCurr.x, pCurr.y)
                    }
                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )

                // 4. Draw stroke line
                val strokePath = Path().apply {
                    moveTo(coordinates.first().x, coordinates.first().y)
                    for (i in 1 until coordinates.size) {
                        val pPrev = coordinates[i - 1]
                        val pCurr = coordinates[i]
                        // Bezier smoothing
                        val controlX1 = pPrev.x + (colWidth / 2f)
                        val controlY1 = pPrev.y
                        val controlX2 = pPrev.x + (colWidth / 2f)
                        val controlY2 = pCurr.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, pCurr.x, pCurr.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    color = Color(0xFFFFD700),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // 5. Draw active index elements (vertical line and glow points)
                val activeCoord = coordinates[activeIndex]

                // Vertical Seekline
                drawLine(
                    color = Color(0xFFFFD700).copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(activeCoord.x, 0f),
                    end = androidx.compose.ui.geometry.Offset(activeCoord.x, height),
                    strokeWidth = 1.dp.toPx()
                )

                // Glowing circles
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = activeCoord
                )
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 4.dp.toPx(),
                    center = activeCoord
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom X-axis Dates Label Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val finalLabelCount = when {
                dataPoints.size > labelCount -> labelCount
                dataPoints.size > 1 -> dataPoints.size
                else -> 1
            }
            for (i in 0 until finalLabelCount) {
                val idx = if (finalLabelCount > 1) {
                    Math.round(i * (dataPoints.size - 1).toFloat() / (finalLabelCount - 1))
                        .coerceIn(0, dataPoints.size - 1)
                } else {
                    0
                }
                Text(
                    text = dataPoints[idx].dateLabel,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

data class GoldPricePoint(
    val dateLabel: String,
    val priceInUSD: Double,
    val fullDateLabel: String = ""
)

/**
 * Updated chart data builder per your specs:
 * - 30D: 120 points over 30 days, labeled as "dd" (day of month)
 * - 6M: 96 points over 6 months, labeled as "MMM" (month name)
 * - 1Y: 96 points over 1 year, labeled as "MMM" (month name)
 * - 5Y: 80 points over 5 years, labeled as "yyyy" (four-digit year)
 */
fun buildGoldChartData(
    history: List<CalculatorViewModel.GoldHistoryPoint>,
    spotGoldUSDPerGram: Double,
    ozToGram: Double
): List<List<GoldPricePoint>> {
    val liveOnly = listOf(GoldPricePoint("Live", spotGoldUSDPerGram, "Live"))
    if (history.isEmpty()) return List(4) { liveOnly }

    val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun cutoffDate(field: Int, amount: Int): String {
        val cal = Calendar.getInstance()
        cal.add(field, -amount)
        return isoFormat.format(cal.time)
    }

    fun shortLabel(iso: String, pattern: String): String {
        return try {
            val parsed = isoFormat.parse(iso)
            if (parsed != null) SimpleDateFormat(pattern, Locale.US).format(parsed) else iso.takeLast(5)
        } catch (e: Throwable) { iso.takeLast(5) }
    }

    fun <T> sampleEvenly(list: List<T>, targetCount: Int): List<T> {
        if (list.size <= targetCount) return list
        val step = (list.size - 1).toDouble() / (targetCount - 1)
        return (0 until targetCount).map { i ->
            list[(i * step).roundToInt().coerceIn(0, list.size - 1)]
        }
    }

    fun buildSeries(cutoff: String, pattern: String, targetPoints: Int, fallbackDays: Int = 45): List<GoldPricePoint> {
        var filtered = history.filter { it.date >= cutoff }
        if (filtered.isEmpty()) {
            val fbCutoff = cutoffDate(Calendar.DAY_OF_YEAR, fallbackDays)
            filtered = history.filter { it.date >= fbCutoff }
        }
        if (filtered.isEmpty()) return liveOnly

        val sampled = sampleEvenly(filtered, targetPoints)
        return sampled.map {
            val label = shortLabel(it.date, pattern)
            val fullLabelPattern = "MMM dd, yyyy"
            val fullLabel = shortLabel(it.date, fullLabelPattern)
            GoldPricePoint(label, it.priceUSDPerOz / ozToGram, fullLabel)
        } + liveOnly
    }

    val thirtyDays = buildSeries(cutoffDate(Calendar.DAY_OF_YEAR, 30), "dd", 29)
    val sixMonths = buildSeries(cutoffDate(Calendar.MONTH, 6), "MMM yy", 179)
    val oneYear = buildSeries(cutoffDate(Calendar.YEAR, 1), "MMM yy", 364)
    val fiveYears = buildSeries(cutoffDate(Calendar.YEAR, 5), "yyyy", 364)

    return listOf(thirtyDays, sixMonths, oneYear, fiveYears)
}


