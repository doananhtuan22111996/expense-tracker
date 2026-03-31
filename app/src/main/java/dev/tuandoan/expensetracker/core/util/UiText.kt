package dev.tuandoan.expensetracker.core.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Wrapper that allows ViewModels to reference string resources
 * without depending on Android Context.
 */
sealed class UiText {
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText()

    data class DynamicString(
        val value: String,
    ) : UiText()

    @Composable
    fun asString(): String =
        when (this) {
            is StringResource -> stringResource(resId, *args.toTypedArray())
            is DynamicString -> value
        }

    fun asString(context: Context): String =
        when (this) {
            is StringResource -> context.getString(resId, *args.toTypedArray())
            is DynamicString -> value
        }
}
