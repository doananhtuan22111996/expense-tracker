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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@Composable
fun MonthSelector(
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    onMonthLabelClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = DesignSystemSpacing.large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier =
                Modifier.semantics {
                    contentDescription = "Go to previous month"
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
                        contentDescription =
                            if (onMonthLabelClick != null) {
                                "Selected month: $monthLabel. Tap to pick month"
                            } else {
                                "Selected month: $monthLabel"
                            }
                    },
        )

        IconButton(
            onClick = onNextMonth,
            modifier =
                Modifier.semantics {
                    contentDescription = "Go to next month"
                },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}
