package dev.tuandoan.expensetracker.ui.screen.widgetcategories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.widget.PinnedCategorySlot
import dev.tuandoan.expensetracker.ui.theme.ChartColors
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Settings → Widget Categories screen (T6.3). Lets the user pick up to 3
 * EXPENSE categories to pin as quick-add tiles on the home-screen widget.
 *
 * Layout:
 * 1. 3-slot preview strip mirroring the widget layout (Filled / Empty).
 * 2. "N of 3 pinned" count label.
 * 3. LazyColumn of every EXPENSE category. Pinned rows expose up/down arrows
 *    for reordering; unpinned rows expose a "pin" action (greyed when at cap).
 * 4. Empty state with CTA when the user has no EXPENSE categories yet.
 *
 * Reorder gesture: up/down arrow buttons rather than drag. The list maxes at
 * 3 items, so arrows cover every reachable reorder cheaper than a drag lib
 * and with better screen-reader UX. `onMove(from, to)` on the VM stays
 * drag-compatible if a future PR wants to add the gesture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCategoriesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    viewModel: WidgetCategoriesViewModel,
    modifier: Modifier = Modifier,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.overLimitMessage) {
        uiState.overLimitMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onOverLimitMessageShown()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_navigate_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        WidgetCategoriesBody(
            uiState = uiState,
            onTogglePin = viewModel::onTogglePin,
            onMove = viewModel::onMove,
            onNavigateToCategories = onNavigateToCategories,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            bottomContentPadding = bottomContentPadding,
        )
    }
}

@Composable
private fun WidgetCategoriesBody(
    uiState: WidgetCategoriesUiState,
    onTogglePin: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    // isLoading: first frame before the combine() emits. Render nothing
    // rather than a half-configured strip + empty list (pinnedSlots default
    // is emptyList(), which would render a zero-tile strip). Upstream emits
    // within a single Main-dispatcher turn, so the loading frame is
    // short-lived and not worth a skeleton.
    if (uiState.isLoading) {
        Box(modifier = modifier)
        return
    }

    if (uiState.availableCategories.isEmpty()) {
        EmptyCategoriesState(onNavigateToCategories = onNavigateToCategories, modifier = modifier)
        return
    }

    // Live-ordered pin list for arrow-reorder indexing. Matches the VM's
    // onMove(fromIndex, toIndex) contract: indices over the LIVE list only.
    val livePinnedIds =
        uiState.pinnedSlots.filterIsInstance<PinnedCategorySlot.Filled>().map { it.category.id }

    LazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = DesignSystemSpacing.large,
                end = DesignSystemSpacing.large,
                top = DesignSystemSpacing.medium,
                bottom = DesignSystemSpacing.large + bottomContentPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
    ) {
        item {
            Text(
                text = stringResource(R.string.widget_categories_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            PreviewStrip(slots = uiState.pinnedSlots)
        }
        item {
            Text(
                text = stringResource(R.string.widget_categories_count_label, uiState.pinnedCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                text = stringResource(R.string.widget_categories_list_header),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = DesignSystemSpacing.small),
            )
        }
        items(
            items = uiState.availableCategories,
            key = { it.id },
        ) { category ->
            val livePinIndex = livePinnedIds.indexOf(category.id)
            val isPinned = livePinIndex >= 0
            CategoryPinRow(
                category = category,
                isPinned = isPinned,
                isAtMaxPins = uiState.isAtMaxPins,
                canMoveUp = isPinned && livePinIndex > 0,
                canMoveDown = isPinned && livePinIndex in 0 until (livePinnedIds.size - 1),
                onTogglePin = { onTogglePin(category.id) },
                onMoveUp = { onMove(livePinIndex, livePinIndex - 1) },
                onMoveDown = { onMove(livePinIndex, livePinIndex + 1) },
            )
        }
    }
}

@Composable
private fun PreviewStrip(
    slots: List<PinnedCategorySlot>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
    ) {
        slots.forEach { slot ->
            PreviewTile(
                slot = slot,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewTile(
    slot: PinnedCategorySlot,
    modifier: Modifier = Modifier,
) {
    when (slot) {
        is PinnedCategorySlot.Filled -> {
            val color = ChartColors.categoryColor(slot.category.colorKey, MaterialTheme.colorScheme)
            val a11y =
                stringResource(
                    R.string.a11y_widget_categories_preview_filled,
                    slot.index,
                    slot.category.name,
                )
            Box(
                modifier =
                    modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .semantics { contentDescription = a11y },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = slot.category.name,
                    style = MaterialTheme.typography.labelLarge,
                    // ChartColors.categoryColor returns 8 different scheme tokens
                    // (primary, error, secondary, tertiary, inversePrimary, outline,
                    // onPrimaryContainer, onTertiaryContainer) — no single `onX`
                    // color is readable on all of them. Pick black or white by
                    // luminance so contrast stays above WCAG AA regardless of
                    // which colorKey the category carries.
                    color = contrastingTextColor(color),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = DesignSystemSpacing.small),
                )
            }
        }
        is PinnedCategorySlot.Empty -> {
            val a11y =
                stringResource(R.string.a11y_widget_categories_preview_empty, slot.index)
            Box(
                modifier =
                    modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .semantics { contentDescription = a11y },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.widget_categories_empty_slot),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryPinRow(
    category: Category,
    isPinned: Boolean,
    isAtMaxPins: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTogglePin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = ChartColors.categoryColor(category.colorKey, MaterialTheme.colorScheme)

    Card(
        // Intentionally NOT using semantics(mergeDescendants = true). Merging
        // collapses the up/down/pin IconButtons into a single row-level a11y
        // node, which makes the per-action buttons unreachable for TalkBack
        // users. Each IconButton already carries its own contentDescription,
        // so leaving the row unmerged preserves discoverability of the
        // reorder + pin/unpin actions.
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystemSpacing.large, vertical = DesignSystemSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clearAndSetSemantics {},
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isPinned) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.widget_categories_move_up),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.widget_categories_move_down),
                    )
                }
                IconButton(onClick = onTogglePin) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.widget_categories_unpin),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                IconButton(
                    onClick = onTogglePin,
                    enabled = !isAtMaxPins,
                ) {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = stringResource(R.string.widget_categories_pin),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCategoriesState(
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(DesignSystemSpacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.widget_categories_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.widget_categories_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = DesignSystemSpacing.small),
        )
        Button(
            onClick = onNavigateToCategories,
            modifier = Modifier.padding(top = DesignSystemSpacing.large),
        ) {
            Text(stringResource(R.string.widget_categories_empty_cta))
        }
    }
}

/**
 * Returns black or white depending on the background's relative luminance
 * so the label stays above WCAG AA contrast regardless of which scheme
 * token [ChartColors.categoryColor] happens to map to. Threshold 0.5 is
 * the standard Material heuristic for "pick the contrasting monochrome".
 */
private fun contrastingTextColor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White
