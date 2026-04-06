package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@Composable
fun MonthSelector(
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onMonthLabelClick: (() -> Unit)? = null,
    periodType: String = "month",
) {
    val previousPeriodDesc = stringResource(R.string.a11y_previous_period, periodType)
    val nextPeriodDesc = stringResource(R.string.a11y_next_period, periodType)
    val selectedPeriodDesc =
        if (onMonthLabelClick != null) {
            stringResource(R.string.a11y_selected_period_tap, periodType, monthLabel, periodType)
        } else {
            stringResource(R.string.a11y_selected_period, periodType, monthLabel)
        }

    val alpha = if (enabled) 1f else DISABLED_ALPHA

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = DesignSystemSpacing.large)
                .alpha(alpha),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPreviousMonth,
            enabled = enabled,
            modifier =
                Modifier.semantics {
                    contentDescription = previousPeriodDesc
                },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
            )
        }

        Text(
            text = monthLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier
                    .let { mod ->
                        if (onMonthLabelClick != null) {
                            mod.clickable(role = Role.Button, onClick = onMonthLabelClick)
                        } else {
                            mod
                        }
                    }.semantics {
                        heading()
                        contentDescription = selectedPeriodDesc
                    },
        )

        IconButton(
            onClick = onNextMonth,
            enabled = enabled,
            modifier =
                Modifier.semantics {
                    contentDescription = nextPeriodDesc
                },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
