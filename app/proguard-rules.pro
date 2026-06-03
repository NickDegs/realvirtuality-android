-keepattributes *Annotation*
-keepclassmembers class app.realvirtuality.data.** { *; }
-keep class kotlinx.serialization.** { *; }

# Tink / security-crypto errorprone annotations are compile-time only
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
