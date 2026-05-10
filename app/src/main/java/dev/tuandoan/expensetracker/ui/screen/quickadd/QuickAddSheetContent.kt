package dev.tuandoan.expensetracker.ui.screen.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.CurrencyAmountVisualTransformation
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.ui.theme.ChartColors
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Bottom-sheet UI for the widget quick-add flow (T3.4).
 *
 * Wires the `QuickAddViewModel` (PR #131) to a Material 3 [ModalBottomSheet]:
 * category header with color swatch, auto-focused amount field with currency
 * suffix + visual transformation, Save button disabled when invalid/saving,
 * Cancel button. A [LaunchedEffect] observes [QuickAddUiState.saved] and
 * triggers [onDismiss] on successful save so the hosting Activity can
 * `finish()`.
 *
 * The FR-07 race (pinned category deleted between widget snapshot and tap)
 * surfaces via [QuickAddUiState.categoryMissing] — the sheet renders a
 * single "Category no longer exists" message + Cancel button, no save
 * controls.
 *
 * VM is obtained via `hiltViewModel()`. The `categoryId` flows into its
 * `SavedStateHandle` automatically because the hosting Activity seeds
 * `intent.extras[EXTRA_CATEGORY_ID]`, which Hilt routes to the VM's
 * `SavedStateHandle` via the `SavedStateViewModelFactory` integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickAddSheetContent(
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // One-shot: dismiss the sheet (and Activity) after a successful save.
    LaunchedEffect(state.saved) {
        if (state.saved) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DesignSystemSpacing.large,
                        end = DesignSystemSpacing.large,
                        bottom = DesignSystemSpacing.large,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
        ) {
            when {
                state.categoryMissing -> CategoryMissingBody(onDismiss = onDismiss)
                state.category == null -> LoadingBody()
                else ->
                    QuickAddBody(
                        state = state,
                        onAmountChanged = viewModel::onAmountChanged,
                        onSave = viewModel::saveTransaction,
                        onDismiss = onDismiss,
                    )
            }
        }
    }
}

/**
 * Normal quick-add body: category header, amount field, action buttons.
 * Extracted so [QuickAddSheetContent]'s `when` branches stay readable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddBody(
    state: QuickAddUiState,
    onAmountChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val category = state.category ?: return
    val currency = remember(state.currencyCode) { SupportedCurrencies.byCode(state.currencyCode) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the amount field so the keyboard appears immediately and the
    // user can start typing without an extra tap. Keyed on the category to
    // re-request focus if a second tile tap swaps the category underneath.
    LaunchedEffect(category.id) {
        focusRequester.requestFocus()
    }

    val colorScheme = MaterialTheme.colorScheme
    val swatchColor = remember(category.colorKey) { ChartColors.categoryColor(category.colorKey, colorScheme) }

    CategoryHeader(name = category.name, swatchColor = swatchColor)

    OutlinedTextField(
        value = state.amountText,
        onValueChange = { input ->
            // Mirrors AddEditTransactionScreen's digit-only filter.
            val clean = input.replace("[^0-9]".toRegex(), "")
            onAmountChanged(clean)
        },
        label = {
            Text(stringResource(R.string.quick_add_amount_label, state.currencyCode))
        },
        suffix = {
            if (currency != null) {
                Text(
                    text = currency.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        visualTransformation =
            remember(state.currencyCode) {
                CurrencyAmountVisualTransformation(state.currencyCode)
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        isError = state.errorMessage != null,
        supportingText =
            state.errorMessage?.let { errorMessage ->
                { Text(text = errorMessage.asString()) }
            },
        singleLine = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
    )

    Spacer(modifier = Modifier.height(DesignSystemSpacing.small))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            enabled = !state.isSaving,
        ) {
            Text(text = stringResource(R.string.quick_add_cancel))
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = state.amountText.isNotBlank() && !state.isSaving,
        ) {
            Text(text = stringResource(R.string.quick_add_save))
        }
    }
}

/**
 * FR-07 race branch: the pinned category no longer exists. Replace the full
 * input surface with an explanation + Cancel button so users don't waste
 * keystrokes on a transaction that can't save.
 */
@Composable
private fun CategoryMissingBody(onDismiss: () -> Unit) {
    Text(
        text = stringResource(R.string.quick_add_category_missing),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedButton(onClick = onDismiss) {
            Text(text = stringResource(R.string.quick_add_cancel))
        }
    }
}

/**
 * Brief loading view for the ~1-frame window between Activity launch and the
 * VM's first state emission. Kept minimal so it doesn't flash visually; the
 * real body renders on the very next recomposition.
 */
@Composable
private fun LoadingBody() {
    Spacer(modifier = Modifier.height(DesignSystemSpacing.large))
}

@Composable
private fun CategoryHeader(
    name: String,
    swatchColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
    ) {
        Box(
            modifier =
                Modifier
                    .size(DesignSystemSpacing.medium)
                    .clip(CircleShape)
                    .background(swatchColor),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
