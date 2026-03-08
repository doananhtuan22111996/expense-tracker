package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tuandoan.expensetracker.domain.model.MonthlyBarPoint

private val monthLabels =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@Composable
fun MonthlyBarChart(
    points: List<MonthlyBarPoint>,
    emptyLabel: String,
    modifier: Modifier = Modifier,
    onSurfaceVariantColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onMonthTapped: ((month: Int) -> Unit)? = null,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = onSurfaceVariantColor
    val labelSizeSp = 10.sp
    val labelSizePx = with(LocalDensity.current) { labelSizeSp.toPx() }
    val barSpacingState = remember { mutableFloatStateOf(0f) }
    val leftPaddingState = remember { mutableFloatStateOf(0f) }

    val tapModifier =
        if (onMonthTapped != null) {
            Modifier.pointerInput(onMonthTapped) {
                detectTapGestures { offset ->
                    val barSpacing = barSpacingState.floatValue
                    val leftPadding = leftPaddingState.floatValue
                    if (barSpacing > 0f) {
                        val barIndex = ((offset.x - leftPadding) / barSpacing).toInt()
                        if (barIndex in 0..11) {
                            onMonthTapped(barIndex + 1)
                        }
                    }
                }
            }
        } else {
            Modifier
        }

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = "Monthly expenses bar chart" }
                .then(tapModifier),
    ) {
        val maxValue = points.maxOfOrNull { it.totalExpense } ?: 0L
        if (maxValue == 0L) {
            // Draw centered empty-state text when all data is zero
            drawContext.canvas.nativeCanvas.apply {
                val paint =
                    android.graphics.Paint().apply {
                        color = onSurfaceVariant.toArgb()
                        textSize = labelSizePx * 1.2f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                drawText(emptyLabel, size.width / 2f, size.height / 2f, paint)
            }
            return@Canvas
        }

        val bottomPadding = labelSizePx * 2.5f
        val topPadding = labelSizePx * 2f
        val leftPadding = 0f
        val rightPadding = 0f
        val chartHeight = size.height - bottomPadding - topPadding
        val chartWidth = size.width - leftPadding - rightPadding
        val barWidth = chartWidth / 12f * 0.6f
        val barSpacing = chartWidth / 12f

        barSpacingState.floatValue = barSpacing
        leftPaddingState.floatValue = leftPadding

        // Draw max value label
        val maxLabel = compactFormat(maxValue)
        drawContext.canvas.nativeCanvas.apply {
            val paint =
                android.graphics.Paint().apply {
                    color = onSurfaceVariant.toArgb()
                    textSize = labelSizePx
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
            drawText(maxLabel, leftPadding, topPadding - labelSizePx * 0.3f, paint)
        }

        // Draw bars
        points.forEachIndexed { index, point ->
            val barHeight =
                if (maxValue > 0L) {
                    (point.totalExpense.toFloat() / maxValue) * chartHeight
                } else {
                    0f
                }
            val x = leftPadding + barSpacing * index + (barSpacing - barWidth) / 2f
            val y = topPadding + chartHeight - barHeight

            if (barHeight > 0f) {
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                )
            }

            // Draw month label
            drawContext.canvas.nativeCanvas.apply {
                val paint =
                    android.graphics.Paint().apply {
                        color = onSurfaceVariant.toArgb()
                        textSize = labelSizePx
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                drawText(
                    monthLabels[index],
                    x + barWidth / 2f,
                    size.height - labelSizePx * 0.3f,
                    paint,
                )
            }
        }
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

internal fun compactFormat(value: Long): String =
    when {
        value >= 1_000_000_000L -> {
            val formatted = value / 1_000_000_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}B"
            } else {
                "${"%.1f".format(formatted).trimEnd('0').trimEnd('.')}B"
            }
        }
        value >= 1_000_000L -> {
            val formatted = value / 1_000_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}M"
            } else {
                "${"%.1f".format(formatted).trimEnd('0').trimEnd('.')}M"
            }
        }
        value >= 1_000L -> {
            val formatted = value / 1_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}K"
            } else {
                "${"%.1f".format(formatted).trimEnd('0').trimEnd('.')}K"
            }
        }
        else -> value.toString()
    }
