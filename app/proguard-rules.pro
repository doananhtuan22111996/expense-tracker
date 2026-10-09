# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# Jetpack libraries (Compose, Room, Hilt, Navigation, Coroutines) and AAPT2
# provide their own consumer ProGuard rules automatically. Avoid adding blanket
# -keep rules (e.g. -keep class androidx.compose.**) here as they disable R8
# code shrinking, obfuscation, and class inlining.

# ===== CRASHLYTICS / STACK TRACES =====
# Keep line numbers and source file names for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== NATIVE METHODS =====
# Keep JNI native methods
-keepclasseswithmembers class * {
    native <methods>;
}

# ===== KOTLINX SERIALIZATION =====
# Keep serializers and companions specifically for @Serializable backup DTO models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve serializers for backup models
-keep,includedescriptorclasses class dev.tuandoan.expensetracker.data.backup.model.**$$serializer { *; }
-keepclassmembers class dev.tuandoan.expensetracker.data.backup.model.** {
    *** Companion;
}

# ===== TOOLING WARNING SUPPRESSIONS =====
# Generated automatically by Android Gradle plugin
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
