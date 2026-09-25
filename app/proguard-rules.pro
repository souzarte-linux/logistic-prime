# ==============================================================================
# ProGuard / R8 Rules - Central do Motorista (logistic-prime)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. General & Line Number Preservation for Crash Reports
# ------------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 2. Gson & Data Models / DTOs
# ------------------------------------------------------------------------------
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }

# Keep DTOs and Data Models to prevent Gson reflection serialization failures
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.fernando.centraldomotorista.data.model.** { *; }
-keep class com.fernando.centraldomotorista.data.remote.dto.** { *; }
-keep class com.fernando.centraldomotorista.data.billing.** { *; }

# ------------------------------------------------------------------------------
# 3. Retrofit 2 & OkHttp 3
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.fernando.centraldomotorista.data.remote.api.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ------------------------------------------------------------------------------
# 4. Supabase Kotlin & Ktor Engine
# ------------------------------------------------------------------------------
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# ------------------------------------------------------------------------------
# 5. Kotlin Coroutines
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------------------
# 6. AndroidX & Jetpack Components (Compose, Navigation, DataStore, Biometric)
# ------------------------------------------------------------------------------
-keep class androidx.biometric.** { *; }
-keep class androidx.datastore.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.lifecycle.** { *; }

# ------------------------------------------------------------------------------
# 7. Google Play Services, Credentials & ML Kit
# ------------------------------------------------------------------------------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class androidx.camera.** { *; }
