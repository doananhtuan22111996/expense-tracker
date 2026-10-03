package dev.tuandoan.expensetracker.domain.security

import android.content.Context
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE,
}

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult

    data object Failed : BiometricAuthResult

    data class Error(
        val errorCode: Int,
        val errString: CharSequence,
    ) : BiometricAuthResult
}

/**
 * Abstraction over AndroidX BiometricPrompt and BiometricManager (v3.16.0, ADR-018).
 */
interface BiometricAuthHelper {
    fun canAuthenticate(context: Context): BiometricStatus

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        onResult: (BiometricAuthResult) -> Unit,
    )
}
