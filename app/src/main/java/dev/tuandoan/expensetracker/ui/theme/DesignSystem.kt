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
    private fun fallbackChartColors(colorScheme: ColorScheme) =
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.error,
            colorScheme.inversePrimary,
            colorScheme.outline,
        )

    fun categoryColor(
        colorKey: String?,
        colorScheme: ColorScheme,
    ): Color =
        when (colorKey) {
            "red" -> colorScheme.error
            "blue" -> colorScheme.primary
            "green" -> colorScheme.onPrimaryContainer
            "orange" -> colorScheme.secondary
            "purple" -> colorScheme.inversePrimary
            "teal" -> colorScheme.tertiary
            "pink" -> colorScheme.onTertiaryContainer
            "gray" -> colorScheme.outline
            else -> colorScheme.primary
        }

    @Composable
    fun resolveChartColors(colorKeys: List<String?>): List<Color> {
        val colorScheme = MaterialTheme.colorScheme
        val fallbacks = fallbackChartColors(colorScheme)
        return colorKeys.mapIndexed { index, key ->
            if (key != null) {
                categoryColor(key, colorScheme)
            } else {
                fallbacks[index % fallbacks.size]
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
