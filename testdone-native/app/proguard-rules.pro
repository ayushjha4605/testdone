# Kotlin serialization — official R8 rules (kotlinx.serialization README §"R8 and ProGuard"),
# applied to ALL @Serializable classes (Retrofit's kotlinx converter resolves serializers
# reflectively at runtime; these keep the generated $serializer + Companion.serializer()).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature
-dontnote kotlinx.serialization.**

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }

# Belt & braces: app models' companions survive even without the annotation check above
-keepclassmembers class com.testdone.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.testdone.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Supabase REST error bodies carry generic types
-keepattributes EnclosingMethod, SourceFile, LineNumberTable

# ── Razorpay Checkout (v2.3.16) ────────────────────────────────────────────────
# Official Razorpay R8 guidance: keep the SDK's public surface; its AAR ships
# consumer rules for the common cases, these belt-and-braces keeps cover the
# reflective lookup of PaymentResultListener on the Activity.
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-keepclassmembers class * implements com.razorpay.PaymentResultListener {
    public void onPaymentSuccess(java.lang.String);
    public void onPaymentError(int, java.lang.String);
}
-keepclassmembers class com.testdone.app.MainActivity {
    public void onPaymentSuccess(java.lang.String);
    public void onPaymentError(int, java.lang.String);
}
