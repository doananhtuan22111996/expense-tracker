package dev.tuandoan.expensetracker.ui.screen.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Scaffold body for the quick-add bottom sheet (T3.1). Renders a Material 3
 * [ModalBottomSheet] with a placeholder header and a dismiss-only Cancel
 * button; the real amount field + Save path arrive in T3.2/T3.4.
 *
 * Showing "Category #<id>" instead of the real category name is a deliberate
 * T3.1 limitation — name lookup is a `QuickAddViewModel` responsibility that
 * T3.2 introduces. Keeping this scaffold VM-free lets the Activity + manifest
 * wiring land as a separate reviewable chunk; a user that somehow reached
 * this path before T3.2 merges would see the placeholder and cancel out.
 *
 * Dismissing via any mechanism — Cancel tap, scrim tap, predictive back —
 * flows through [onDismiss] so the hosting Activity can call `finish()`
 * uniformly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickAddSheetContent(
    categoryId: Long,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
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
            // T3.2 will swap this for the real category name resolved via VM.
            Text(text = "Category #$categoryId")
            Spacer(modifier = Modifier.height(DesignSystemSpacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.quick_add_cancel))
                }
                // T3.2/T3.4 replace this with a real Save button bound to VM.save().
                // Until then the Save control is intentionally absent so users can
                // only cancel — prevents a confusing no-op tap from a half-shipped
                // scaffold.
            }
        }
    }
}
