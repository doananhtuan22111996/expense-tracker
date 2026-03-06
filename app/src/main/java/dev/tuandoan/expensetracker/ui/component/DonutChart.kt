package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

private val chartColors =
    listOf(
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFF607D8B),
    )

@Composable
fun DonutChart(
    categories: List<CategoryTotal>,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
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

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Expense distribution chart" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val strokeWidth = 32.dp

        Canvas(
            modifier = Modifier.size(200.dp),
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
                val color = chartColors[index % chartColors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt),
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
                val color = chartColors[index % chartColors.size]
                LegendItem(
                    color = color,
                    label = categoryTotal.category.name,
                    percentage = percentage,
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(DesignSystemSpacing.small))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
