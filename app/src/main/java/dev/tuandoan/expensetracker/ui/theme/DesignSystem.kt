package dev.tuandoan.expensetracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design system constants following Material 3 guidelines
 */
@Stable
object DesignSystemSpacing {
    val xs: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    // Screen-specific spacing
    val screenPadding: Dp = large
    val sectionSpacing: Dp = medium
    val componentSpacing: Dp = small
    val listItemSpacing: Dp = small
}

@Stable
object DesignSystemElevation {
    val none: Dp = 0.dp
    val low: Dp = 2.dp
    val medium: Dp = 4.dp
    val high: Dp = 8.dp
}

/**
 * Chart colors that respond to the current theme (light/dark).
 */
@Stable
object ChartColors {
    private val fallbackChartColors =
        listOf(
            Color(0xFF4CAF50),
            Color(0xFF2196F3),
            Color(0xFFF44336),
            Color(0xFFFF9800),
            Color(0xFF9C27B0),
            Color(0xFF607D8B),
        )

    fun categoryColor(
        colorKey: String?,
        colorScheme: ColorScheme,
    ): Color =
        when (colorKey) {
            "red" -> colorScheme.error
            "blue" -> colorScheme.primary
            "green" -> Color(0xFF43A047)
            "orange" -> colorScheme.secondary
            "purple" -> colorScheme.inversePrimary
            "teal" -> colorScheme.tertiary
            "pink" -> Color(0xFFD81B60)
            "gray" -> colorScheme.outline
            else -> colorScheme.primary
        }

    @Composable
    fun resolveChartColors(colorKeys: List<String?>): List<Color> {
        val colorScheme = MaterialTheme.colorScheme
        return colorKeys.mapIndexed { index, key ->
            if (key != null) {
                categoryColor(key, colorScheme)
            } else {
                fallbackChartColors[index % fallbackChartColors.size]
            }
        }
    }
}

/**
 * Semantic colors for financial data
 */
@Stable
object FinancialColors {
    @Composable
    fun incomeColor(): Color = MaterialTheme.colorScheme.primary

    @Composable
    fun expenseColor(): Color = MaterialTheme.colorScheme.error

    @Composable
    fun balanceColor(isPositive: Boolean): Color =
        if (isPositive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }
}
