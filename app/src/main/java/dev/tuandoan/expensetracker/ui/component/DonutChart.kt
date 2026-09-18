package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.ui.theme.ChartColors
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@Composable
fun DonutChart(
    categories: List<CategoryTotal>,
    modifier: Modifier = Modifier,
    selectedCategoryId: Long? = null,
    onCategoryClick: ((Category) -> Unit)? = null,
) {
    if (categories.isEmpty()) {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
        ) {
            Icon(
                imageVector = Icons.Outlined.DonutLarge,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Text(
                text = stringResource(R.string.no_expenses_this_period),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val total = categories.sumOf { it.total }.toFloat()
    if (total == 0f) return

    val sliceColors =
        ChartColors.resolveChartColors(
            categories.map { it.category.colorKey },
        )

    val chartDescription =
        stringResource(
            R.string.a11y_donut_chart_description,
            buildString {
                categories.forEachIndexed { _, ct ->
                    val pct = (ct.total / total * 100f).toInt()
                    append("${ct.category.name}: $pct%. ")
                }
            },
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = chartDescription
                    role = Role.Image
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val strokeWidth = 32.dp

        val canvasModifier =
            Modifier
                .size(200.dp)
                .then(
                    if (onCategoryClick != null) {
                        Modifier.pointerInput(categories, total) {
                            detectTapGestures { offset ->
                                val canvasSize = minOf(size.width, size.height).toFloat()
                                val strokePx = strokeWidth.toPx()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                val dist = kotlin.math.hypot(dx, dy)
                                val outerRadius = canvasSize / 2f
                                val innerRadius = outerRadius - strokePx
                                if (dist in innerRadius..outerRadius) {
                                    val angleRad = kotlin.math.atan2(dy, dx)
                                    val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                                    // Normalize angle clockwise starting from top (-90 deg):
                                    val normalizedAngle = (angleDeg + 90f + 360f) % 360f
                                    var accumulatedAngle = 0f
                                    for (catTotal in categories) {
                                        val sweepAngle = (catTotal.total / total) * 360f
                                        if (normalizedAngle >= accumulatedAngle &&
                                            normalizedAngle < accumulatedAngle + sweepAngle
                                        ) {
                                            onCategoryClick(catTotal.category)
                                            break
                                        }
                                        accumulatedAngle += sweepAngle
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                )

        Canvas(
            modifier = canvasModifier,
        ) {
            val canvasSize = size.minDimension
            val strokePx = strokeWidth.toPx()
            val radius = (canvasSize - strokePx) / 2f
            val topLeft =
                Offset(
                    (size.width - canvasSize + strokePx) / 2f,
                    (size.height - canvasSize + strokePx) / 2f,
                )
            val arcSize = Size(radius * 2f, radius * 2f)

            var startAngle = -90f
            categories.forEachIndexed { index, categoryTotal ->
                val sweepAngle = (categoryTotal.total / total) * 360f
                val isSelected = selectedCategoryId == categoryTotal.category.id
                val sliceColor =
                    if (selectedCategoryId != null && !isSelected) {
                        sliceColors[index].copy(alpha = 0.35f)
                    } else {
                        sliceColors[index]
                    }
                val currentStroke =
                    if (isSelected) {
                        Stroke(width = strokePx * 1.15f, cap = StrokeCap.Butt)
                    } else {
                        Stroke(width = strokePx, cap = StrokeCap.Butt)
                    }
                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = currentStroke,
                )
                startAngle += sweepAngle
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = DesignSystemSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
        ) {
            categories.forEachIndexed { index, categoryTotal ->
                val percentage = (categoryTotal.total / total * 100f).toInt()
                val isSelected = selectedCategoryId == categoryTotal.category.id
                LegendItem(
                    color = sliceColors[index],
                    label = categoryTotal.category.name,
                    percentage = percentage,
                    isSelected = isSelected,
                    isDimmed = selectedCategoryId != null && !isSelected,
                    onClick = onCategoryClick?.let { { it(categoryTotal.category) } },
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    percentage: Int,
    isSelected: Boolean = false,
    isDimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val alpha = if (isDimmed) 0.35f else 1f
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignSystemSpacing.xs))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(vertical = 2.dp, horizontal = DesignSystemSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color.copy(alpha = alpha))
        }
        Spacer(modifier = Modifier.width(DesignSystemSpacing.small))
        Text(
            text = label,
            style =
                if (isSelected) {
                    MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodySmall
                },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "$percentage%",
            style =
                if (isSelected) {
                    MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodySmall
                },
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}
