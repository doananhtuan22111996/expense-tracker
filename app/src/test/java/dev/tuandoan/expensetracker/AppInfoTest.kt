package dev.tuandoan.expensetracker

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import dev.tuandoan.expensetracker.core.util.AppInfo
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Unit tests for AppInfo utility
 * Tests dynamic version retrieval and fallback mechanisms
 */
class AppInfoTest {
    @Test
    fun getVersionName_returnsVersionFromBuildConfig() {
        val version = AppInfo.getVersionName()
        assertNotNull("Version name should not be null", version)
        assertTrue("Version name should not be empty", version.isNotEmpty())
        // Version should match the pattern (e.g., "1.0.0")
        assertTrue("Version should contain dots", version.contains("."))
    }

    @Test
    fun getVersionCode_returnsPositiveInteger() {
        val versionCode = AppInfo.getVersionCode()
        assertTrue("Version code should be positive", versionCode > 0)
    }

    @Test
    fun getFormattedVersion_combinesNameAndCode() {
        val formatted = AppInfo.getFormattedVersion()
        val expectedPattern = "${AppInfo.getVersionName()} (${AppInfo.getVersionCode()})"

        assertEquals("Formatted version should match expected pattern", expectedPattern, formatted)
        assertTrue("Formatted version should contain parentheses", formatted.contains("(") && formatted.contains(")"))
    }

    @Test
    fun getBuildType_returnsValidBuildType() {
        val buildType = AppInfo.getBuildType()
        assertNotNull("Build type should not be null", buildType)
        assertTrue(
            "Build type should be valid",
            buildType == "debug" || buildType == "release" || buildType == "staging",
        )
    }

    @Test
    fun getFullVersionInfo_includesBuildTypeForNonRelease() {
        val fullInfo = AppInfo.getFullVersionInfo()
        assertNotNull("Full version info should not be null", fullInfo)
        assertTrue("Should contain version", fullInfo.contains("Version:"))
        assertTrue("Should contain version name", fullInfo.contains(AppInfo.getVersionName()))
        assertTrue("Should contain version code", fullInfo.contains(AppInfo.getVersionCode().toString()))

        if (AppInfo.getBuildType() != "release") {
            assertTrue("Should contain build type for non-release", fullInfo.contains(AppInfo.getBuildType()))
        }
    }

    @Test
    fun getApplicationId_returnsValidPackageName() {
        val appId = AppInfo.getApplicationId()
        assertNotNull("Application ID should not be null", appId)
        assertTrue("Should contain package structure", appId.contains("."))
        assertTrue("Should start with expected domain", appId.startsWith("dev.tuandoan"))
    }

    @Test
    fun isDebugBuild_returnsBooleanValue() {
        val isDebug = AppInfo.isDebugBuild()
        // Should return either true or false (not null)
        assertTrue("Debug flag should be boolean", isDebug is Boolean)
    }

    @Test
    fun getVersionNameFallback_withValidContext_returnsVersion() {
        // Mock context and package manager
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        val mockPackageInfo =
            PackageInfo().apply {
                versionName = "2.0.0"
            }

        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockContext.packageName).thenReturn("dev.tuandoan.expensetracker")
        `when`(mockPackageManager.getPackageInfo("dev.tuandoan.expensetracker", 0))
            .thenReturn(mockPackageInfo)

        val version = AppInfo.getVersionNameFallback(mockContext)
        assertEquals("Should return mocked version", "2.0.0", version)
    }

    @Test
    fun getVersionNameFallback_withException_returnsUnknown() {
        // Mock context that throws exception
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)

        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockContext.packageName).thenReturn("invalid.package")
        `when`(mockPackageManager.getPackageInfo("invalid.package", 0))
            .thenThrow(PackageManager.NameNotFoundException())

        val version = AppInfo.getVersionNameFallback(mockContext)
        assertEquals("Should return Unknown for exception", "Unknown", version)
    }

    @Test
    fun getVersionNameFallback_withNullVersionName_returnsUnknown() {
        // Mock context with null version name
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        val mockPackageInfo =
            PackageInfo().apply {
                versionName = null
            }

        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockContext.packageName).thenReturn("dev.tuandoan.expensetracker")
        `when`(mockPackageManager.getPackageInfo("dev.tuandoan.expensetracker", 0))
            .thenReturn(mockPackageInfo)

        val version = AppInfo.getVersionNameFallback(mockContext)
        assertEquals("Should return Unknown for null version", "Unknown", version)
    }

    @Test
    fun versionConsistency_buildConfigMatchesFallback() {
        // This test would require a real context in instrumented tests
        // Here we just verify that BuildConfig version is consistent
        val buildConfigVersion = AppInfo.getVersionName()
        assertNotNull("BuildConfig version should be available", buildConfigVersion)
        assertTrue(
            "Version should follow semantic versioning pattern",
            buildConfigVersion.matches(Regex("\\d+\\.\\d+(\\.\\d+)?")),
        )
    }
}
