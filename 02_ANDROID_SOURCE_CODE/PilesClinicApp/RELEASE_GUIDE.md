# Release Guide — Signed APK / Play Store AAB

## Why there's no attached APK/AAB file
Building a signed APK or AAB requires actually running the Android Gradle
Plugin/Gradle with the real Android SDK, on a machine with internet access
to resolve dependencies (Room, Retrofit, WorkManager, ZXing, etc.), plus a
real keystore to sign it. **None of that exists in the sandbox this project
was built in** — there is no Android SDK, no Gradle, no network access, and
no keystore file. So instead of fabricating a fake "final APK", this guide
gives the exact steps to produce a real one yourself in Android Studio,
which only takes a few minutes once the project is open.

## 1. One-time: create a release keystore
In a terminal (keytool ships with any JDK, including the one bundled with
Android Studio):
```
keytool -genkeypair -v -keystore release-keystore.jks -alias pilesclinic \
  -keyalg RSA -keysize 2048 -validity 10000
```
Keep the resulting `release-keystore.jks` somewhere safe **outside** the
project folder (or make sure `.gitignore` excludes it if kept inside — this
project's `.gitignore` already ignores `*.keystore`, add `*.jks` too if you
keep it in-repo, though outside-repo is safer).

## 2. Configure signing (never hardcoded)
Add these four lines to `local.properties` (see `local.properties.example`):
```
RELEASE_KEYSTORE_PATH=/absolute/path/to/release-keystore.jks
RELEASE_KEYSTORE_PASSWORD=your-keystore-password
RELEASE_KEY_ALIAS=pilesclinic
RELEASE_KEY_PASSWORD=your-key-password
```
`app/build.gradle.kts` reads these automatically; the `release` build type
will be signed with them.

## 3. Build APK / AAB
- **From Android Studio**: Build → Generate Signed Bundle / APK → choose
  APK or Android App Bundle → it will auto-detect the signing config above
  (or let you point at the keystore interactively if you skipped step 2).
- **From the command line** (after `cd PilesClinicApp`):
  ```
  ./gradlew assembleRelease   # -> app/build/outputs/apk/release/app-release.apk
  ./gradlew bundleRelease     # -> app/build/outputs/bundle/release/app-release.aab
  ```

## 4. Before uploading to Play Console
- Fill in real Supabase credentials and branch details (see
  `SUPABASE_SETUP.md`, `print/BranchInfo.kt`) — this project ships with
  placeholders that must not go to production as-is.
- Walk through `FINAL_TEST_REPORT.md`'s manual regression checklist at least
  once on a real device/emulator, since none of it has been run yet.
- Confirm `applicationId` (`com.tkbiswas.pilesclinic`) and `versionCode` /
  `versionName` in `app/build.gradle.kts` are what you want for this release.
- Decide on `isMinifyEnabled` (currently `false`); the ProGuard rules for
  Gson/Retrofit/Room/ZXing are pre-staged in `proguard-rules.pro` if you turn
  it on, but re-test the app fully after doing so.

## 5. Everyday install (debug, for testing)
No signing needed for local testing:
```
./gradlew installDebug
```
or just press ▶ Run in Android Studio with a device/emulator connected.
