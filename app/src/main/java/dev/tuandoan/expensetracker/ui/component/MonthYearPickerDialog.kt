package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.Month
import java.time.YearMonth

private val MONTH_LABELS =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@Composable
fun MonthYearPickerDialog(
    currentSelection: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerYear by rememberSaveable { mutableIntStateOf(currentSelection.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "Select month",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Select month dialog"
                    },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium)) {
                // Year stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { pickerYear-- },
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Previous year"
                            },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                        )
                    }

                    Text(
                        text = pickerYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Year $pickerYear"
                            },
                    )

                    IconButton(
                        onClick = { pickerYear++ },
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Next year"
                            },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    }
                }

                // 4x3 month grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                ) {
                    items(MONTH_LABELS) { label ->
                        val monthIndex = MONTH_LABELS.indexOf(label) + 1
                        val month = Month.of(monthIndex)
                        val ym = YearMonth.of(pickerYear, month)
                        val isSelected = ym == currentSelection

                        if (isSelected) {
                            FilledTonalButton(
                                onClick = {
                                    onMonthSelected(ym)
                                    onDismiss()
                                },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "$label $pickerYear, selected"
                                    },
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    onMonthSelected(ym)
                                    onDismiss()
                                },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "$label $pickerYear"
                                    },
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
