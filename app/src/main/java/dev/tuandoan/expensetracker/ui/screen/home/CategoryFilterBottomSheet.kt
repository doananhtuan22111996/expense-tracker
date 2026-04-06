package dev.tuandoan.expensetracker.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.ui.theme.ChartColors
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterBottomSheet(
    sheetState: SheetState,
    expenseCategories: List<Category>,
    incomeCategories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DesignSystemSpacing.large,
                        end = DesignSystemSpacing.large,
                        bottom = DesignSystemSpacing.xl,
                    ),
        ) {
            Text(
                text = stringResource(R.string.filter_category_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignSystemSpacing.large),
            )

            LazyColumn(
                modifier = Modifier.height(SHEET_MAX_HEIGHT),
            ) {
                // All Categories option
                item {
                    CategoryFilterItem(
                        name = stringResource(R.string.filter_category_all),
                        colorKey = null,
                        isSelected = selectedCategoryId == null,
                        onClick = {
                            onCategorySelected(null)
                            onDismiss()
                        },
                    )
                }

                // Expense categories
                if (expenseCategories.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.filter_expenses),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    top = DesignSystemSpacing.medium,
                                    bottom = DesignSystemSpacing.xs,
                                ),
                        )
                    }
                    items(expenseCategories, key = { it.id }) { category ->
                        CategoryFilterItem(
                            name = category.name,
                            colorKey = category.colorKey,
                            isSelected = selectedCategoryId == category.id,
                            onClick = {
                                onCategorySelected(category.id)
                                onDismiss()
                            },
                        )
                    }
                }

                // Income categories
                if (incomeCategories.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.filter_income),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    top = DesignSystemSpacing.medium,
                                    bottom = DesignSystemSpacing.xs,
                                ),
                        )
                    }
                    items(incomeCategories, key = { it.id }) { category ->
                        CategoryFilterItem(
                            name = category.name,
                            colorKey = category.colorKey,
                            isSelected = selectedCategoryId == category.id,
                            onClick = {
                                onCategorySelected(category.id)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterItem(
    name: String,
    colorKey: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val desc =
        if (isSelected) {
            "$name, ${stringResource(R.string.a11y_selected)}"
        } else {
            name
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = DesignSystemSpacing.medium)
                .semantics { contentDescription = desc },
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null, // handled by row click
        )
        Spacer(modifier = Modifier.width(DesignSystemSpacing.medium))
        if (colorKey != null) {
            val color = ChartColors.categoryColor(colorKey, MaterialTheme.colorScheme)
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(COLOR_SWATCH_SIZE),
            ) {}
            Spacer(modifier = Modifier.width(DesignSystemSpacing.small))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val SHEET_MAX_HEIGHT = 400.dp
private val COLOR_SWATCH_SIZE = 12.dp
