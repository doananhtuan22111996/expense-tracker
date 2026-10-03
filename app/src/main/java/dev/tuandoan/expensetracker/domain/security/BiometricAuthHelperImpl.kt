package dev.tuandoan.expensetracker.domain.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthHelperImpl
    @Inject
    constructor() : BiometricAuthHelper {
        override fun canAuthenticate(context: Context): BiometricStatus {
            val biometricManager = BiometricManager.from(context)
            val authenticators =
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            return when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                else -> BiometricStatus.UNAVAILABLE
            }
        }

        override fun authenticate(
            activity: FragmentActivity,
            title: String,
            subtitle: String?,
            onResult: (BiometricAuthResult) -> Unit,
        ) {
            val executor = ContextCompat.getMainExecutor(activity)
            val promptInfo =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(title)
                    .apply {
                        if (!subtitle.isNullOrBlank()) {
                            setSubtitle(subtitle)
                        }
                    }.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    ).build()

            val prompt =
                BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onResult(BiometricAuthResult.Success)
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onResult(BiometricAuthResult.Failed)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            onResult(BiometricAuthResult.Error(errorCode, errString))
                        }
                    },
                )
            prompt.authenticate(promptInfo)
        }
    }
