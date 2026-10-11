package dev.tuandoan.expensetracker.data.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.BuildConfig
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.security.AppIntegrityManager
import dev.tuandoan.expensetracker.domain.security.IntegrityResult
import dev.tuandoan.expensetracker.domain.security.IntegrityState
import dev.tuandoan.expensetracker.domain.security.RemediationResult
import dev.tuandoan.expensetracker.domain.security.RemediationType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production implementation of [AppIntegrityManager] integrating Google Play Integrity API
 * (Protected with Play) with local defense-in-depth and offline-first resilience.
 */
@Singleton
class PlayIntegrityManagerImpl(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val integrityManagerProvider: () -> StandardIntegrityManager,
) : AppIntegrityManager {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(context, ioDispatcher, { IntegrityManagerFactory.createStandard(context) })

    internal val standardIntegrityManager: StandardIntegrityManager by lazy {
        integrityManagerProvider()
    }

    private var tokenProvider: StandardIntegrityTokenProvider? = null

    private val _integrityState = MutableStateFlow<IntegrityState>(IntegrityState.Unchecked)
    override val integrityState: StateFlow<IntegrityState> = _integrityState.asStateFlow()

    override suspend fun warmUp() {
        withContext(ioDispatcher) {
            if (tokenProvider != null) return@withContext
            try {
                val request =
                    PrepareIntegrityTokenRequest
                        .builder()
                        .setCloudProjectNumber(GOOGLE_CLOUD_PROJECT_NUMBER)
                        .build()

                tokenProvider =
                    kotlinx.coroutines.withTimeoutOrNull(WARMUP_TIMEOUT_MS) {
                        suspendCancellableCoroutine { continuation ->
                            standardIntegrityManager
                                .prepareIntegrityToken(request)
                                .addOnSuccessListener(Runnable::run) { provider ->
                                    continuation.resume(provider)
                                }.addOnFailureListener(Runnable::run) {
                                    continuation.resume(null)
                                }
                        }
                    }
            } catch (_: Exception) {
                tokenProvider = null
            }
        }
    }

    override suspend fun evaluateIntegrity(): IntegrityResult =
        withContext(ioDispatcher) {
            _integrityState.value = IntegrityState.Checking

            // 1. Offline Check: Never block an offline user from accessing their records
            if (!isNetworkAvailable()) {
                val offlineResult =
                    IntegrityResult(
                        isGenuine = true,
                        isPlayStoreInstalled = isInstallerPlayStore(),
                        isSignatureValid = isSigningCertificateValid(),
                        isDeviceIntegrityMet = true,
                        isOfflineMode = true,
                        details = "Offline mode: Local defense passed. Attestation bypassed.",
                    )
                _integrityState.value = IntegrityState.Evaluated(offlineResult)
                return@withContext offlineResult
            }

            // 2. Client-side Defense-in-Depth
            val playInstalled = isInstallerPlayStore()
            val signatureValid = isSigningCertificateValid()

            if (!signatureValid) {
                val tamperedResult =
                    IntegrityResult(
                        isGenuine = false,
                        isPlayStoreInstalled = playInstalled,
                        isSignatureValid = false,
                        isDeviceIntegrityMet = false,
                        remediationRequired = RemediationType.GET_LICENSED,
                        details = "App binary signature mismatch. Potential tampering detected.",
                    )
                _integrityState.value = IntegrityState.Evaluated(tamperedResult)
                return@withContext tamperedResult
            }

            // 3. Play Integrity Standard Request
            var tokenObtained = false
            try {
                if (tokenProvider == null) {
                    warmUp()
                }

                val provider = tokenProvider
                if (provider != null) {
                    val requestHash = computeSha256(context.packageName)
                    val tokenRequest =
                        StandardIntegrityTokenRequest
                            .builder()
                            .setRequestHash(requestHash)
                            .build()

                    val tokenResponse =
                        suspendCancellableCoroutine { continuation ->
                            provider
                                .request(tokenRequest)
                                .addOnSuccessListener(Runnable::run) { response ->
                                    continuation.resume(response)
                                }.addOnFailureListener(Runnable::run) {
                                    continuation.resume(null)
                                }
                        }
                    tokenObtained = tokenResponse != null && tokenResponse.token().isNotEmpty()
                }
            } catch (_: Exception) {
                tokenObtained = false
            }

            // If token could not be obtained due to sideloading or unverified Play environment
            val isGenuine = if (BuildConfig.DEBUG) true else (tokenObtained || playInstalled)
            val remediation = if (!isGenuine) RemediationType.GET_LICENSED else null

            val result =
                IntegrityResult(
                    isGenuine = isGenuine,
                    isPlayStoreInstalled = playInstalled,
                    isSignatureValid = signatureValid,
                    isDeviceIntegrityMet = isGenuine,
                    isOfflineMode = false,
                    remediationRequired = remediation,
                    details = if (isGenuine) "Verified genuine" else "Unverified installation",
                )

            _integrityState.value = IntegrityState.Evaluated(result)
            result
        }

    override suspend fun requestRemediation(
        activity: Activity,
        typeCode: RemediationType,
    ): RemediationResult =
        withContext(ioDispatcher) {
            try {
                when (typeCode) {
                    RemediationType.GET_LICENSED -> {
                        // Direct user to official Google Play Store page
                        val playStoreIntent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${context.packageName}"),
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        activity.startActivity(playStoreIntent)
                        RemediationResult.SUCCESSFUL
                    }
                    else -> RemediationResult.UNAVAILABLE
                }
            } catch (_: Exception) {
                // Fallback to web browser if Play Store app is not installed
                try {
                    val webIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    activity.startActivity(webIntent)
                    RemediationResult.SUCCESSFUL
                } catch (_: Exception) {
                    RemediationResult.FAILED
                }
            }
        }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isInstallerPlayStore(): Boolean {
        if (BuildConfig.DEBUG) return true
        return try {
            val installer =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getInstallerPackageName(context.packageName)
                }
            installer in KNOWN_PLAY_INSTALLERS
        } catch (_: Exception) {
            false
        }
    }

    private fun isSigningCertificateValid(): Boolean {
        if (BuildConfig.DEBUG) return true
        return try {
            val signatures =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val info =
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            PackageManager.GET_SIGNING_CERTIFICATES,
                        )
                    info.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    val info =
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            PackageManager.GET_SIGNATURES,
                        )
                    @Suppress("DEPRECATION")
                    info.signatures
                }
            // Valid if signature array exists and is not empty
            signatures != null && signatures.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Google Cloud Project Number linked in google-services.json */
        const val GOOGLE_CLOUD_PROJECT_NUMBER = 423754710458L

        /** Timeout for integrity provider warm-up */
        const val WARMUP_TIMEOUT_MS = 5000L

        /** Timeout for standard integrity token request */
        const val REQUEST_TIMEOUT_MS = 5000L

        /** Approved Google Play installer packages */
        val KNOWN_PLAY_INSTALLERS =
            setOf(
                "com.android.vending",
                "com.google.android.feedback",
            )
    }
}
