# KkaloAI ProGuard rules
# Keep line numbers in crash reports (Crashlytics).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures — required by Gson + Retrofit for type tokens.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# --- Kotlin ---
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# --- Hilt / Dagger ---
# Hilt ships consumer-proguard rules for most things, but a few generated
# artifacts need explicit keep rules because they are referenced from
# superclasses generated in the same package as the app code.
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class com.kkaloai.app.**ViewModel { *; }
-keep class com.kkaloai.app.**_GeneratedInjector { *; }
-keep class com.kkaloai.app.KkaloAiApp_GeneratedInjector { *; }
-keep class com.kkaloai.app.MainActivity_GeneratedInjector { *; }
-dontwarn com.kkaloai.app.**_GeneratedInjector
-dontwarn dagger.hilt.**

# --- Gson ---
# Preserve @SerializedName fields and all data/model classes parsed by Gson.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.kkaloai.app.data.model.** { *; }
-keep class com.kkaloai.app.data.remote.OpenFoodFactsService$** { *; }
# Gson uses reflection on TypeToken subclasses.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# --- Firebase (Auth, Firestore, Crashlytics, Analytics) ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*

# --- ARCore & Sceneform ---
-keep class com.google.ar.** { *; }
-keep class com.google.ar.sceneform.** { *; }
-keep class com.gorisse.thomas.sceneform.** { *; }
-dontwarn com.google.ar.**
-dontwarn com.google.ar.sceneform.**
-dontwarn com.gorisse.thomas.sceneform.**

# --- Play Billing ---
-keep class com.android.billingclient.** { *; }

# --- Health Connect ---
-keep class androidx.health.connect.** { *; }
-dontwarn androidx.health.connect.**

# --- Compose ---
# Compose consumer rules already strip what's safe. Keep Preview/runtime
# classes that can be reflected on via tooling.
-keep class androidx.compose.runtime.** { *; }

# --- WorkManager (Hilt worker factory) ---
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }

# --- Keep our own Application + Activity entry points ---
-keep class com.kkaloai.app.KkaloAiApp { *; }
-keep class com.kkaloai.app.MainActivity { *; }
-keep class com.kkaloai.app.notif.** { *; }
-keep class com.kkaloai.app.worker.** { *; }
