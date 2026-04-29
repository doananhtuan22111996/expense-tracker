package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * 3-row shimmer placeholder matching the final [InsightRowItem] heights so
 * the section doesn't visibly jump when the engine emits its first populated
 * result (PRD FR-18).
 */
@Composable
internal fun InsightsShimmer(modifier: Modifier = Modifier) {
    val alpha by rememberShimmerAlpha()
    val loadingDesc = stringResource(R.string.a11y_insights_loading)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = DesignSystemSpacing.small)
                .semantics { contentDescription = loadingDesc },
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        repeat(3) {
            ShimmerRow(alpha = alpha)
        }
    }
}

@Composable
private fun ShimmerRow(alpha: Float) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignSystemSpacing.large,
                    vertical = DesignSystemSpacing.medium,
                ),
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shimmerColor =
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * alpha + 0.08f)

        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .background(color = shimmerColor, shape = CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(
                        color = shimmerColor,
                        shape = RoundedCornerShape(DesignSystemSpacing.xs),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .width(48.dp)
                    .height(24.dp)
                    .background(
                        color = shimmerColor,
                        shape = RoundedCornerShape(DesignSystemSpacing.small),
                    ),
        )
    }
}

@Composable
private fun rememberShimmerAlpha() =
    rememberInfiniteTransition(label = "insights_shimmer")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                InfiniteRepeatableSpec(
                    animation = tween(durationMillis = 900),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "insights_shimmer_alpha",
        )
