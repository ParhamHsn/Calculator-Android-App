package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class HistoryItem(
    val id: Long,
    val expression: String,
    val result: String
)

data class CalculatorUiState(
    val expression: String = "",
    val previewResult: String = "",
    val history: List<HistoryItem> = emptyList(),
    val showHistory: Boolean = false
)

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private val decimalFormatter = DecimalFormat("#,###.######").apply {
        decimalFormatSymbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
    }

    private fun isOperator(char: Char): Boolean {
        return char == '+' || char == '-' || char == '×' || char == '÷'
    }

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Digit -> enterDigit(action.digit)
            is CalculatorAction.Decimal -> enterDecimal()
            is CalculatorAction.Clear -> clearExpression()
            is CalculatorAction.Delete -> deleteLastChar()
            is CalculatorAction.Operator -> enterOperator(action.operator)
            is CalculatorAction.Calculate -> performCalculation()
            is CalculatorAction.ToggleSign -> toggleSign()
            is CalculatorAction.Percent -> applyPercent()
            is CalculatorAction.ToggleHistory -> toggleHistoryPanel()
            is CalculatorAction.ClearHistory -> clearAllHistory()
            is CalculatorAction.SelectHistoryItem -> recallHistory(action.item)
        }
    }

    private fun enterDigit(digit: Int) {
        _uiState.update { currentState ->
            val newExpr = if (currentState.expression == "0" && digit != 0) {
                digit.toString()
            } else if (currentState.expression == "0" && digit == 0) {
                currentState.expression
            } else {
                currentState.expression + digit
            }
            currentState.copy(
                expression = newExpr,
                previewResult = calculatePreview(newExpr)
            )
        }
    }

    private fun enterDecimal() {
        _uiState.update { currentState ->
            val expr = currentState.expression
            if (expr.isEmpty()) {
                val newExpr = "0."
                return@update currentState.copy(
                    expression = newExpr,
                    previewResult = calculatePreview(newExpr)
                )
            }

            val lastChar = expr.last()
            if (isOperator(lastChar)) {
                val newExpr = expr + "0."
                return@update currentState.copy(
                    expression = newExpr,
                    previewResult = calculatePreview(newExpr)
                )
            }

            // check if the current last number token already contains a decimal
            val tokens = expr.split("+", "-", "×", "÷")
            val lastToken = tokens.lastOrNull() ?: ""
            if (!lastToken.contains(".")) {
                val newExpr = expr + "."
                currentState.copy(
                    expression = newExpr,
                    previewResult = calculatePreview(newExpr)
                )
            } else {
                currentState
            }
        }
    }

    private fun enterOperator(op: String) {
        _uiState.update { currentState ->
            val expr = currentState.expression
            if (expr.isEmpty()) {
                if (op == "-") {
                    return@update currentState.copy(expression = "-")
                }
                return@update currentState
            }

            val lastChar = expr.last()
            val newExpr = if (isOperator(lastChar)) {
                // Replace last operator with the new one
                expr.dropLast(1) + op
            } else {
                expr + op
            }

            currentState.copy(
                expression = newExpr,
                previewResult = calculatePreview(newExpr)
            )
        }
    }

    private fun clearExpression() {
        _uiState.update { currentState ->
            currentState.copy(
                expression = "",
                previewResult = ""
            )
        }
    }

    private fun deleteLastChar() {
        _uiState.update { currentState ->
            val expr = currentState.expression
            if (expr.isEmpty()) return@update currentState

            val newExpr = expr.dropLast(1)
            currentState.copy(
                expression = newExpr,
                previewResult = calculatePreview(newExpr)
            )
        }
    }

    private fun toggleSign() {
        _uiState.update { currentState ->
            val expr = currentState.expression
            if (expr.isEmpty()) return@update currentState

            // Find the last number token in the expression
            val operatorIndices = expr.mapIndexedNotNull { index, char ->
                if (isOperator(char) && index != 0 && !isOperator(expr[index - 1])) index else null
            }

            val lastOperatorIndex = operatorIndices.lastOrNull()

            val newExpr = if (lastOperatorIndex == null) {
                // Simple single token operation
                if (expr.startsWith("-")) {
                    expr.removePrefix("-")
                } else {
                    "-$expr"
                }
            } else {
                val prefix = expr.substring(0, lastOperatorIndex + 1)
                val lastToken = expr.substring(lastOperatorIndex + 1)
                
                if (lastToken.startsWith("-")) {
                    prefix + lastToken.removePrefix("-")
                } else {
                    prefix + "-$lastToken"
                }
            }

            currentState.copy(
                expression = newExpr,
                previewResult = calculatePreview(newExpr)
            )
        }
    }

    private fun applyPercent() {
        _uiState.update { currentState ->
            val expr = currentState.expression
            if (expr.isEmpty() || isOperator(expr.last())) return@update currentState

            // Parse percentage: treat the last number as divided by 100
            val operatorIndices = expr.mapIndexedNotNull { index, char ->
                if (isOperator(char) && index != 0 && !isOperator(expr[index - 1])) index else null
            }
            val lastOperatorIndex = operatorIndices.lastOrNull()

            val newExpr = if (lastOperatorIndex == null) {
                val num = expr.toDoubleOrNull()
                if (num != null) {
                    formatValue(num / 100.0)
                } else expr
            } else {
                val prefix = expr.substring(0, lastOperatorIndex + 1)
                val lastToken = expr.substring(lastOperatorIndex + 1)
                val num = lastToken.toDoubleOrNull()
                if (num != null) {
                    prefix + formatValue(num / 100.0)
                } else expr
            }

            currentState.copy(
                expression = newExpr,
                previewResult = calculatePreview(newExpr)
            )
        }
    }

    private fun performCalculation() {
        val currentState = _uiState.value
        val expr = currentState.expression
        if (expr.isEmpty()) return

        val rawResult = parseAndEvaluate(expr)
        if (rawResult.isNaN()) {
            _uiState.update {
                it.copy(previewResult = "خطا")
            }
            return
        }

        val formattedResult = formatValue(rawResult)
        
        _uiState.update { state ->
            val newHistoryItem = HistoryItem(
                id = System.currentTimeMillis(),
                expression = expr,
                result = formattedResult
            )
            state.copy(
                expression = formattedResult,
                previewResult = "",
                history = listOf(newHistoryItem) + state.history
            )
        }
    }

    private fun recallHistory(item: HistoryItem) {
        _uiState.update { state ->
            state.copy(
                expression = item.expression,
                previewResult = calculatePreview(item.expression),
                showHistory = false
            )
        }
    }

    private fun toggleHistoryPanel() {
        _uiState.update { state ->
            state.copy(showHistory = !state.showHistory)
        }
    }

    private fun clearAllHistory() {
        _uiState.update { state ->
            state.copy(history = emptyList())
        }
    }

    private fun calculatePreview(expr: String): String {
        if (expr.isEmpty()) return ""
        val lastChar = expr.last()
        // If it's just a number, we don't display a preview
        val hasOperators = expr.any { isOperator(it) && it != '-' } || (expr.startsWith('-') && expr.substring(1).any { isOperator(it) })
        if (!hasOperators) return ""

        val workingExpr = if (isOperator(lastChar)) expr.dropLast(1) else expr
        val rawResult = parseAndEvaluate(workingExpr)
        
        return if (rawResult.isNaN()) {
            ""
        } else {
            "= ${formatValue(rawResult)}"
        }
    }

    private fun formatValue(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            decimalFormatter.format(value)
        }
    }

    private fun parseAndEvaluate(expression: String): Double {
        try {
            val tokens = tokenizeExpression(expression)
            if (tokens.isEmpty()) return Double.NaN
            
            // First pass: perform multiplication and division
            val multiplierTokens = mutableListOf<String>()
            var idx = 0
            while (idx < tokens.size) {
                val token = tokens[idx]
                if (token == "×" || token == "÷") {
                    if (multiplierTokens.isEmpty()) return Double.NaN
                    val leftStr = multiplierTokens.removeAt(multiplierTokens.lastIndex)
                    val left = leftStr.toDoubleOrNull() ?: return Double.NaN
                    if (idx + 1 >= tokens.size) return Double.NaN
                    val rightStr = tokens[idx + 1]
                    val right = rightStr.toDoubleOrNull() ?: return Double.NaN
                    
                    val res = if (token == "×") {
                        left * right
                    } else {
                        if (right == 0.0) return Double.NaN
                        left / right
                    }
                    multiplierTokens.add(res.toString())
                    idx += 2
                } else {
                    multiplierTokens.add(token)
                    idx++
                }
            }

            // Second pass: perform addition and subtraction
            if (multiplierTokens.isEmpty()) return Double.NaN
            var finalResult = multiplierTokens[0].toDoubleOrNull() ?: return Double.NaN
            var j = 1
            while (j < multiplierTokens.size) {
                val op = multiplierTokens[j]
                if (j + 1 >= multiplierTokens.size) return Double.NaN
                val nextVal = multiplierTokens[j + 1].toDoubleOrNull() ?: return Double.NaN
                if (op == "+") {
                    finalResult += nextVal
                } else if (op == "-") {
                    finalResult -= nextVal
                } else {
                    return Double.NaN
                }
                j += 2
            }
            return finalResult
        } catch (e: Exception) {
            return Double.NaN
        }
    }

    private fun tokenizeExpression(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var currentToken = StringBuilder()
        var idx = 0
        while (idx < expr.length) {
            val char = expr[idx]
            if (char.isDigit() || char == '.') {
                currentToken.append(char)
            } else if (char == '-' && (idx == 0 || isOperator(expr[idx - 1]))) {
                currentToken.append(char)
            } else if (isOperator(char)) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken = StringBuilder()
                }
                tokens.add(char.toString())
            }
            idx++
        }
        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString())
        }
        return tokens
    }
}

sealed interface CalculatorAction {
    data class Digit(val digit: Int) : CalculatorAction
    object Decimal : CalculatorAction
    object Clear : CalculatorAction
    object Delete : CalculatorAction
    data class Operator(val operator: String) : CalculatorAction
    object Calculate : CalculatorAction
    object ToggleSign : CalculatorAction
    object Percent : CalculatorAction
    object ToggleHistory : CalculatorAction
    object ClearHistory : CalculatorAction
    data class SelectHistoryItem(val item: HistoryItem) : CalculatorAction
}
