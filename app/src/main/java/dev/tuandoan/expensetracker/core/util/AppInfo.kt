package dev.tuandoan.expensetracker.core.util

import android.content.Context
import android.content.pm.PackageManager
import dev.tuandoan.expensetracker.BuildConfig

/**
 * Utility object for accessing application information dynamically
 * Provides version info from build configuration in a centralized, maintainable way
 */
object AppInfo {
    /**
     * Gets the app version name from BuildConfig
     * This is the primary source of truth for version information
     *
     * @return Version name string (e.g., "1.0.0")
     */
    fun getVersionName(): String = BuildConfig.VERSION_NAME

    /**
     * Gets the app version code from BuildConfig
     * Useful for internal versioning and update checks
     *
     * @return Version code integer (e.g., 1)
     */
    fun getVersionCode(): Int = BuildConfig.VERSION_CODE

    /**
     * Gets formatted version info combining name and code
     * Useful for detailed version display in settings or about screens
     *
     * @return Formatted version string (e.g., "1.0.0 (1)")
     */
    fun getFormattedVersion(): String = "${getVersionName()} (${getVersionCode()})"

    /**
     * Gets build variant information
     * Useful for distinguishing between debug/release builds
     *
     * @return Build type string (e.g., "debug", "release")
     */
    fun getBuildType(): String = BuildConfig.BUILD_TYPE

    /**
     * Gets comprehensive app info for debugging or support
     * Combines version, build type, and other metadata
     *
     * @return Comprehensive info string
     */
    fun getFullVersionInfo(): String =
        buildString {
            append("Version: ${getVersionName()}")
            append(" (${getVersionCode()})")
            if (getBuildType() != "release") {
                append(" - ${getBuildType()}")
            }
        }

    /**
     * Fallback method using PackageManager if BuildConfig is unavailable
     * This provides redundancy in case of build configuration issues
     *
     * @param context Application context
     * @return Version name or "Unknown" if unavailable
     */
    fun getVersionNameFallback(context: Context): String =
        try {
            val packageInfo =
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0,
                )
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }

    /**
     * Gets the application ID/package name
     * Useful for debugging and support information
     *
     * @return Application ID string (e.g., "dev.tuandoan.expensetracker")
     */
    fun getApplicationId(): String = BuildConfig.APPLICATION_ID

    /**
     * Checks if this is a debug build
     * Useful for conditional behavior in development vs production
     *
     * @return true if debug build, false otherwise
     */
    fun isDebugBuild(): Boolean = BuildConfig.DEBUG
}
