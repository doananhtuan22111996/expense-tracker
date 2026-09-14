package dev.tuandoan.expensetracker.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R

/**
 * TopAppBar action toggle button for excluding/including trip transactions
 * from month totals, donut charts, bar charts, and budget calculations (v3.13.0, T5.4).
 *
 * When [isExcluded] is false (default), shows [Icons.Default.FlightTakeoff] in standard
 * action icon tint.
 * When [isExcluded] is true, shows [Icons.Default.FlightTakeoff] with a diagonal strike-through
 * line in the error color palette to indicate trips are filtered out.
 */
@Composable
fun ExcludeTripsToggleButton(
    isExcluded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description =
        if (isExcluded) {
            stringResource(R.string.a11y_exclude_trips_disable)
        } else {
            stringResource(R.string.a11y_exclude_trips_enable)
        }

    IconButton(
        onClick = onToggle,
        modifier =
            modifier.semantics {
                contentDescription = description
                role = Role.Switch
            },
    ) {
        if (isExcluded) {
            val activeColor = MaterialTheme.colorScheme.error
            Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.drawSlash(activeColor),
            )
        } else {
            Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Modifier.drawSlash(color: Color): Modifier =
    drawWithContent {
        drawContent()
        drawLine(
            color = color,
            start = Offset(x = size.width * 0.15f, y = size.height * 0.15f),
            end = Offset(x = size.width * 0.85f, y = size.height * 0.85f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
