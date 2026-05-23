package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    CalculatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class ButtonType {
    Number, Operator, Action
}

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onAction(CalculatorAction.ToggleHistory)
                    },
                    modifier = Modifier.testTag("history_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "تاریخچه",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "ماشین حساب",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onAction(CalculatorAction.Clear)
                    },
                    modifier = Modifier.testTag("all_clear_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "بازنشانی",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Expanded Display Work Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                // history panel
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.showHistory,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    HistoryPanel(
                        history = uiState.history,
                        onSelect = { item ->
                            viewModel.onAction(CalculatorAction.SelectHistoryItem(item))
                        },
                        onClear = {
                            viewModel.onAction(CalculatorAction.ClearHistory)
                        },
                        onClose = {
                            viewModel.onAction(CalculatorAction.ToggleHistory)
                        }
                    )
                }

                // Standard Calculator Screen display
                androidx.compose.animation.AnimatedVisibility(
                    visible = !uiState.showHistory,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        // Math Equation display
                        SelectionContainer {
                            Text(
                                text = if (uiState.expression.isEmpty()) "0" else uiState.expression,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 52.sp,
                                    textAlign = TextAlign.Right,
                                    textDirection = TextDirection.Ltr
                                ),
                                maxLines = 3,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expression_display")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Real-Time Result Preview
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.previewResult.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = uiState.previewResult,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Right,
                                    textDirection = TextDirection.Ltr
                                ),
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("preview_display")
                            )
                        }
                    }
                }
            }

            // Keypad rows configuration (using correct custom symbols)
            val buttons = listOf(
                listOf("AC" to ButtonType.Action, "⌫" to ButtonType.Action, "%" to ButtonType.Action, "÷" to ButtonType.Operator),
                listOf("7" to ButtonType.Number, "8" to ButtonType.Number, "9" to ButtonType.Number, "×" to ButtonType.Operator),
                listOf("4" to ButtonType.Number, "5" to ButtonType.Number, "6" to ButtonType.Number, "-" to ButtonType.Operator),
                listOf("1" to ButtonType.Number, "2" to ButtonType.Number, "3" to ButtonType.Number, "+" to ButtonType.Operator),
                listOf("±" to ButtonType.Number, "0" to ButtonType.Number, "." to ButtonType.Number, "=" to ButtonType.Operator)
            )

            // Grid Keypad layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keypad_container"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (symbol, type) ->
                            CalculatorGridButton(
                                text = symbol,
                                buttonType = type,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .testTag("button_${symbol_to_tag(symbol)}"),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    val action = when (symbol) {
                                        "AC" -> CalculatorAction.Clear
                                        "⌫" -> CalculatorAction.Delete
                                        "%" -> CalculatorAction.Percent
                                        "±" -> CalculatorAction.ToggleSign
                                        "=" -> CalculatorAction.Calculate
                                        "." -> CalculatorAction.Decimal
                                        "+", "-", "×", "÷" -> CalculatorAction.Operator(symbol)
                                        else -> CalculatorAction.Digit(symbol.toInt())
                                    }
                                    viewModel.onAction(action)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorGridButton(
    text: String,
    buttonType: ButtonType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = when (buttonType) {
        ButtonType.Operator -> if (text == "=") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
        ButtonType.Action -> MaterialTheme.colorScheme.secondaryContainer
        ButtonType.Number -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val contentColor = when (buttonType) {
        ButtonType.Operator -> if (text == "=") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
        ButtonType.Action -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonType.Number -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayFontSize = if (text.length > 1) {
        if (text == "AC") 20.sp else 18.sp
    } else {
        26.sp
    }

    val displayFontWeight = if (buttonType == ButtonType.Operator) FontWeight.Bold else FontWeight.Medium

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = displayFontSize,
                fontWeight = displayFontWeight,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HistoryPanel(
    history: List<HistoryItem>,
    onSelect: (HistoryItem) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "محاسبات اخیر",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "پاک کردن کل تاریخچه",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "بستن")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "بدون تاریخچه",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هنوز محاسباتی انجام نشده است",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(history) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelect(item) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = item.expression,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Right,
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "= ${item.result}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Right,
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun symbol_to_tag(symbol: String): String {
    return when (symbol) {
        "AC" -> "ac"
        "⌫" -> "backspace"
        "%" -> "percent"
        "÷" -> "divide"
        "×" -> "multiply"
        "-" -> "minus"
        "+" -> "plus"
        "±" -> "plus_minus"
        "." -> "dot"
        "=" -> "equals"
        else -> symbol
    }
}
