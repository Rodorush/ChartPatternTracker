# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserva número de linha nos stack traces do Crashlytics (release).
-keepattributes SourceFile,LineNumberTable
# Assinaturas genéricas e anotações — exigidas por Gson/Retrofit (ex.: List<HistoricalData>).
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

# --- DTOs Gson da Brapi (Retrofit) — desserializados por reflexão (@SerializedName) ---
-keep class br.com.rodorush.chartpatterntracker.api.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Modelo mapeado via Firestore toObject() — precisa de campos + construtor sem-arg ---
-keep class br.com.rodorush.chartpatterntracker.model.PatternItem { *; }