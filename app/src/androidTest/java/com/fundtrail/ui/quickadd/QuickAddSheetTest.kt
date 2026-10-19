package com.fundtrail.ui.quickadd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fundtrail.ui.theme.FundtrailAppTheme
import org.junit.Rule
import org.junit.Test

class QuickAddSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quickAddSheet_initialFocusOnFirstKeypadButton() {
        val state = QuickAddUiState()

        composeTestRule.setContent {
            FundtrailAppTheme {
                QuickAddSheet(
                    state = state,
                    onDismiss = {},
                    onTransactionTypeSelected = {},
                    onKeypadDigit = {},
                    onKeypadDot = {},
                    onKeypadBackspace = {},
                    onCategorySelected = {},
                    onAccountSelected = {},
                    onToAccountSelected = {},
                    onIncomeSourceSelected = {},
                    onCryptoLossChange = {},
                    onDateChange = {},
                    onNoteChange = {},
                    onSpendKindSelected = {},
                    onToggleMoreOptions = {},
                    onSave = {},
                )
            }
        }

        // Verify that initial focus is requested on the first keypad control ("1") when QuickAddSheet opens
        composeTestRule.onNodeWithText("1").assertIsFocused()
    }

    @Test
    fun quickAddSheet_keypadInteraction_decimalLimit_and_minorUnitFormatting() {
        var state by mutableStateOf(QuickAddUiState())

        composeTestRule.setContent {
            FundtrailAppTheme {
                QuickAddSheet(
                    state = state,
                    onDismiss = {},
                    onTransactionTypeSelected = {},
                    onKeypadDigit = { digit ->
                        val current = state.amountText
                        state = state.copy(amountText = AmountInput.sanitize(current + digit))
                    },
                    onKeypadDot = {
                        val current = state.amountText
                        if (!current.contains('.')) {
                            state = state.copy(amountText = AmountInput.sanitize(if (current.isEmpty()) "0." else "$current."))
                        }
                    },
                    onKeypadBackspace = {
                        val current = state.amountText
                        if (current.isNotEmpty()) {
                            state = state.copy(amountText = current.dropLast(1))
                        }
                    },
                    onCategorySelected = {},
                    onAccountSelected = {},
                    onToAccountSelected = {},
                    onIncomeSourceSelected = {},
                    onCryptoLossChange = {},
                    onDateChange = {},
                    onNoteChange = {},
                    onSpendKindSelected = {},
                    onToggleMoreOptions = {},
                    onSave = {},
                )
            }
        }

        // Initially displays LKR 0.00
        composeTestRule.onNodeWithText("LKR 0.00").assertExists()

        // Tap 1, 2, ., 3, 4, 5
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText(".").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.onNodeWithText("5").performClick()

        // Sanitize limits 12.345 to 12.34, which is formatted as LKR 12.34 (1,234 minor units)
        composeTestRule.onNodeWithText("LKR 12.34").assertExists()
    }
}
