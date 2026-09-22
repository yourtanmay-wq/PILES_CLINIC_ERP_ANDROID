# Add project specific ProGuard rules here.
# minifyEnabled is currently false (see build.gradle.kts) since this app grew
# from a WebView shell into a native app across Phases 4-9 without ever being
# regression-tested with R8 minification on. The rules below are pre-staged
# so that turning isMinifyEnabled = true later (after a full regression pass)
# doesn't immediately break Gson/Retrofit reflection or Room.

# Gson (Phase 5: Supabase DTOs are reflectively (de)serialized)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.tkbiswas.pilesclinic.data.remote.** { *; }
-dontwarn com.google.gson.**

# Retrofit / OkHttp (Phase 5)
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-keepattributes Exceptions

# Room (Phase 5) — entities are used via reflection by generated DAOs
-keep class com.tkbiswas.pilesclinic.data.local.** { *; }

# ZXing (Phase 6)
-dontwarn com.google.zxing.**
