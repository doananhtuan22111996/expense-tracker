package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.Month
import java.time.YearMonth

@Composable
private fun monthLabels(): List<String> =
    listOf(
        stringResource(R.string.month_jan),
        stringResource(R.string.month_feb),
        stringResource(R.string.month_mar),
        stringResource(R.string.month_apr),
        stringResource(R.string.month_may),
        stringResource(R.string.month_jun),
        stringResource(R.string.month_jul),
        stringResource(R.string.month_aug),
        stringResource(R.string.month_sep),
        stringResource(R.string.month_oct),
        stringResource(R.string.month_nov),
        stringResource(R.string.month_dec),
    )

@Composable
fun MonthYearPickerDialog(
    currentSelection: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerYear by remember { mutableIntStateOf(currentSelection.year) }
    val labels = monthLabels()
    val selectMonthTitle = stringResource(R.string.select_month_title)
    val selectMonthDialogDesc = stringResource(R.string.a11y_select_month_dialog)
    val previousYearDesc = stringResource(R.string.a11y_previous_year)
    val yearDesc = stringResource(R.string.a11y_year, pickerYear)
    val nextYearDesc = stringResource(R.string.a11y_next_year)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = selectMonthTitle,
                modifier =
                    Modifier.semantics {
                        contentDescription = selectMonthDialogDesc
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
                                contentDescription = previousYearDesc
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
                                contentDescription = yearDesc
                            },
                    )

                    IconButton(
                        onClick = { pickerYear++ },
                        modifier =
                            Modifier.semantics {
                                contentDescription = nextYearDesc
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
                    items(labels.size) { index ->
                        val label = labels[index]
                        val month = Month.of(index + 1)
                        val ym = YearMonth.of(pickerYear, month)
                        val isSelected = ym == currentSelection

                        if (isSelected) {
                            val selectedDesc =
                                stringResource(R.string.a11y_month_selected, label, pickerYear)
                            FilledTonalButton(
                                onClick = {
                                    onMonthSelected(ym)
                                    onDismiss()
                                },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = selectedDesc
                                    },
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            val unselectedDesc =
                                stringResource(R.string.a11y_month_unselected, label, pickerYear)
                            OutlinedButton(
                                onClick = {
                                    onMonthSelected(ym)
                                    onDismiss()
                                },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = unselectedDesc
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
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
