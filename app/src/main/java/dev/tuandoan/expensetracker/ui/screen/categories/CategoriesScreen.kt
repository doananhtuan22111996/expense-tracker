package dev.tuandoan.expensetracker.ui.screen.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryWithCount?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryWithCount?>(null) }

    val categoryDeletedLabel = stringResource(R.string.category_deleted)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(uiState.pendingDeleteId) {
        if (uiState.pendingDeleteId != null) {
            val result =
                snackbarHostState.showSnackbar(
                    message = categoryDeletedLabel,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                SnackbarResult.Dismissed -> viewModel.confirmDelete()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Go back to settings"
                            },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Add new category"
                    },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            val selectedTabIndex =
                when (uiState.selectedTab) {
                    TransactionType.EXPENSE -> 0
                    TransactionType.INCOME -> 1
                }

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { viewModel.onTabSelected(TransactionType.EXPENSE) },
                    text = { Text(stringResource(R.string.expenses_tab)) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Show expense categories"
                        },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { viewModel.onTabSelected(TransactionType.INCOME) },
                    text = { Text(stringResource(R.string.income_tab)) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Show income categories"
                        },
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Loading categories"
                            },
                    )
                }
            } else {
                val categories =
                    when (uiState.selectedTab) {
                        TransactionType.EXPENSE -> uiState.visibleExpenseCategories
                        TransactionType.INCOME -> uiState.visibleIncomeCategories
                    }

                if (categories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_categories),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(DesignSystemSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                    ) {
                        items(
                            items = categories,
                            key = { it.category.id },
                        ) { categoryWithCount ->
                            CategoryRow(
                                categoryWithCount = categoryWithCount,
                                onEdit = { editingCategory = categoryWithCount },
                                onDelete = { deletingCategory = categoryWithCount },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEditCategoryDialog(
            isEditMode = false,
            initialName = "",
            initialColorIndex = 0,
            categoryType = uiState.selectedTab,
            onSave = { name, colorKey ->
                viewModel.createCategory(name, uiState.selectedTab, null, colorKey)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    editingCategory?.let { catWithCount ->
        val colorIndex =
            AVAILABLE_COLORS
                .indexOfFirst { it.first == catWithCount.category.colorKey }
                .coerceAtLeast(0)

        CreateEditCategoryDialog(
            isEditMode = true,
            initialName = catWithCount.category.name,
            initialColorIndex = colorIndex,
            categoryType = catWithCount.category.type,
            onSave = { name, colorKey ->
                viewModel.updateCategory(
                    catWithCount.category.id,
                    name,
                    catWithCount.category.iconKey,
                    colorKey,
                )
                editingCategory = null
            },
            onDismiss = { editingCategory = null },
        )
    }

    deletingCategory?.let { catWithCount ->
        DeleteCategoryDialog(
            categoryWithCount = catWithCount,
            onConfirm = {
                viewModel.requestDelete(catWithCount.category.id)
                deletingCategory = null
            },
            onDismiss = { deletingCategory = null },
        )
    }
}

@Composable
private fun CategoryRow(
    categoryWithCount: CategoryWithCount,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = categoryWithCount.category
    val colorValue =
        AVAILABLE_COLORS.firstOrNull { it.first == category.colorKey }?.second
            ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorValue),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = category.name.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        stringResource(
                            R.string.transactions_count,
                            categoryWithCount.transactionCount,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (category.isDefault) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.default_category),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                IconButton(
                    onClick = onEdit,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Edit ${category.name}"
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Delete ${category.name}"
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEditCategoryDialog(
    isEditMode: Boolean,
    initialName: String,
    initialColorIndex: Int,
    categoryType: TransactionType,
    onSave: (name: String, colorKey: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColorIndex by remember { mutableIntStateOf(initialColorIndex) }
    var nameError by remember { mutableStateOf(false) }

    val title =
        if (isEditMode) {
            stringResource(R.string.edit_category)
        } else {
            stringResource(R.string.add_category)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.category_name)) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text(stringResource(R.string.category_name_required))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.color_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                ) {
                    AVAILABLE_COLORS.forEachIndexed { index, (key, color) ->
                        val isSelected = index == selectedColorIndex
                        val colorName = colorKeyToName(key)
                        Box(
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 3.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ).clickable { selectedColorIndex = index }
                                    .semantics {
                                        contentDescription =
                                            if (isSelected) {
                                                "$colorName color, selected"
                                            } else {
                                                "Select $colorName color"
                                            }
                                    },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@TextButton
                    }
                    val colorKey = AVAILABLE_COLORS.getOrNull(selectedColorIndex)?.first
                    onSave(name.trim(), colorKey)
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun DeleteCategoryDialog(
    categoryWithCount: CategoryWithCount,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_category)) },
        text = {
            Column {
                Text(
                    stringResource(
                        R.string.delete_category_confirm,
                        categoryWithCount.category.name,
                    ),
                )
                if (categoryWithCount.transactionCount > 0) {
                    Text(
                        text =
                            stringResource(
                                R.string.delete_category_warning,
                                categoryWithCount.transactionCount,
                            ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = DesignSystemSpacing.small),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Maps a color key to its localized display name for accessibility.
 */
@Composable
private fun colorKeyToName(key: String): String =
    when (key) {
        "red" -> stringResource(R.string.color_red)
        "blue" -> stringResource(R.string.color_blue)
        "green" -> stringResource(R.string.color_green)
        "orange" -> stringResource(R.string.color_orange)
        "purple" -> stringResource(R.string.color_purple)
        "teal" -> stringResource(R.string.color_teal)
        "pink" -> stringResource(R.string.color_pink)
        "yellow" -> stringResource(R.string.color_yellow)
        else -> key.replaceFirstChar { it.uppercase() }
    }

val AVAILABLE_ICONS =
    listOf(
        "restaurant" to "Restaurant",
        "directions_car" to "Car",
        "shopping_cart" to "Shopping",
        "receipt" to "Bills",
        "local_hospital" to "Health",
        "movie" to "Entertainment",
        "work" to "Work",
        "school" to "School",
        "home" to "Home",
        "help_outline" to "Other",
    )

val AVAILABLE_COLORS =
    listOf(
        "red" to Color(0xFFE53935),
        "blue" to Color(0xFF1E88E5),
        "green" to Color(0xFF43A047),
        "orange" to Color(0xFFFB8C00),
        "purple" to Color(0xFF8E24AA),
        "teal" to Color(0xFF00897B),
        "pink" to Color(0xFFD81B60),
        "gray" to Color(0xFF757575),
    )
