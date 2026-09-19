import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val currentVersionName = "3.13.0"

/**
 * Baseline versionCode from Google Play Console production release (v3.12.0).
 * Google Play requires all subsequent releases to be strictly greater than this value.
 */
val productionBaseVersionCode = 1_778_932_594
val productionBaseSemverValue = 3_120_000
val googlePlayMaxVersionCode = 2_100_000_000

/**
 * Calculates a sustainable, monotonic versionCode from versionName.
 *
 * Starts from the production baseline (1,778,932,594 from v3.12.0) and increments
 * predictably using semantic versioning deltas:
 *   versionCode = PRODUCTION_BASE_VERSION_CODE + (semverValue - PRODUCTION_BASE_SEMVER_VALUE) + buildNumber
 *
 * Examples:
 *   "3.13.0" -> 1_778_942_594 (+10,000 delta for minor bump, leaving >320M headroom to 2.1B ceiling)
 *   "3.13.1" -> 1_778_942_694 (+100 delta for patch bump)
 *   "3.14.0" -> 1_778_952_594 (+10,000 delta for next minor)
 *
 * Supported overrides:
 *   - -PversionCode=<int> or env VERSION_CODE: explicit override (> PRODUCTION_BASE_VERSION_CODE)
 *   - -PbuildNumber=<int> or env BUILD_NUMBER: build / hotfix number (0..99)
 */
fun calculateVersionCode(
    versionName: String,
    project: Project,
): Int {
    val explicitCode =
        project.findProperty("versionCode")?.toString()?.toIntOrNull()
            ?: System.getenv("VERSION_CODE")?.toIntOrNull()
    if (explicitCode != null) {
        require(explicitCode > productionBaseVersionCode) {
            "versionCode ($explicitCode) must be greater than current production versionCode ($productionBaseVersionCode)"
        }
        require(explicitCode <= googlePlayMaxVersionCode) {
            "versionCode ($explicitCode) exceeds Google Play maximum limit of $googlePlayMaxVersionCode"
        }
        return explicitCode
    }

    val semverRegex = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?""")
    val match =
        semverRegex.find(versionName)
            ?: error("Invalid versionName '$versionName'. Expected semver format 'MAJOR.MINOR.PATCH'.")

    val major = match.groupValues[1].toInt()
    val minor = match.groupValues[2].toInt()
    val patch =
        match.groupValues
            .getOrNull(3)
            ?.takeIf { it.isNotEmpty() }
            ?.toInt() ?: 0

    val buildNumber =
        project.findProperty("buildNumber")?.toString()?.toIntOrNull()
            ?: System.getenv("BUILD_NUMBER")?.toIntOrNull()
            ?: 0

    val semverValue = major * 1_000_000 + minor * 10_000 + patch * 100
    val semverDelta = semverValue - productionBaseSemverValue

    val computed = productionBaseVersionCode + semverDelta + buildNumber

    require(computed > productionBaseVersionCode) {
        "Computed versionCode ($computed) must be greater than current production versionCode ($productionBaseVersionCode)"
    }
    require(computed <= googlePlayMaxVersionCode) {
        "Computed versionCode ($computed) exceeds Google Play maximum limit of $googlePlayMaxVersionCode"
    }
    return computed
}

android {
    namespace = "dev.tuandoan.expensetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.tuandoan.expensetracker"
        minSdk = 26
        targetSdk = 36
        versionCode = calculateVersionCode(currentVersionName, project)
        versionName = currentVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            fun getSecretPropertyFile(rootProject: Project): Properties {
                val signingKeyAlias = "SIGNING_KEY_ALIAS"
                val signingKeystorePassword = "SIGNING_KEYSTORE_PASSWORD"
                val signingKeyPassword = "SIGNING_KEY_PASSWORD"

                val secretPropertiesFile: File = rootProject.file("secret.properties")
                val secretProperties = Properties()
                if (secretPropertiesFile.exists()) {
                    secretProperties.load(FileInputStream(secretPropertiesFile))
                }
                System.getenv(signingKeyAlias)?.let {
                    secretProperties.setProperty(signingKeyAlias, it)
                }
                System.getenv(signingKeystorePassword)?.let {
                    secretProperties.setProperty(signingKeystorePassword, it)
                }
                System.getenv(signingKeyPassword)?.let {
                    secretProperties.setProperty(signingKeyPassword, it)
                }
                return secretProperties
            }

            val secretProperties = getSecretPropertyFile(rootProject)
            signingConfigs {
                create("release") {
                    keyAlias = "${secretProperties["SIGNING_KEY_ALIAS"]}"
                    keyPassword = "${secretProperties["SIGNING_KEY_PASSWORD"]}"
                    storePassword = "${secretProperties["SIGNING_KEYSTORE_PASSWORD"]}"
                    storeFile = File("$rootDir/keystore.jks")
                }
            }

            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

//     Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

//     WorkManager + Hilt-Work
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

//     Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // ViewModel and Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Play Review
    implementation(libs.play.review)

    // Glance (home screen widget)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Firebase — Crashlytics (v3.11.0). BoM pins transitive versions.
    // Both BoM and SDK are releaseImplementation: debug builds never see Firebase
    // classes in their classpath, enforcing ADR-010's "debug suppresses collection"
    // as a physical rather than a logical guarantee. Runtime collection is further
    // gated by AnalyticsPreferences + FirebaseCrashReporterImpl (Tasks 1.3–1.5);
    // manifest flag firebase_crashlytics_collection_enabled=false is the SDK-level
    // default for pre-consent crashes.
    releaseImplementation(platform(libs.firebase.bom))
    releaseImplementation(libs.firebase.crashlytics)
    // Analytics SDK is releaseImplementation only — same ADR-010 guarantee as
    // Crashlytics. Automatic events (screen_view, first_open, session_start)
    // are disabled via manifest flags in the release source set per FR-A8.
    releaseImplementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
