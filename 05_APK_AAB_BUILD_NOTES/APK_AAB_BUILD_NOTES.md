# APK / AAB BUILD NOTES

APK and AAB are not prebuilt in this environment.

To build later:
1. Open Android Studio.
2. Open folder: `02_ANDROID_SOURCE_CODE/PilesClinicApp/`
3. Let Android Studio sync Gradle.
4. If Gradle wrapper is missing, let Android Studio regenerate/sync or add wrapper.
5. Build APK: Build > Build Bundle(s) / APK(s) > Build APK(s)
6. Build AAB: Build > Generate Signed Bundle / APK > Android App Bundle

Before calling it APK Ready / AAB Ready:
- Install APK on real Android phone.
- Test login, forms, touch, copy-paste mobile, cards, print bridge, call/WhatsApp intents.
- Test Supabase online/offline sync.
