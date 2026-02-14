import java.io.FileInputStream
import java.time.Instant
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.tuandoan.expensetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.tuandoan.expensetracker"
        minSdk = 26
        targetSdk = 36
        versionCode = Instant.now().epochSecond.toInt() // Epoch seconds: safe until 2038, always increasing
        versionName = "1.2.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
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

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
