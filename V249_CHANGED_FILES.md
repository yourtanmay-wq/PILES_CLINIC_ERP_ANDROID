# V249 CHANGED FILES — 02.08.2026, 05:42:06 PM IST

- `ModuleAuth.kt`: silent module authentication uses only the original role password.
- `ModuleUi.kt`: no visible Module Sign-in/password dialog.
- `V249_KEEP_EXISTING_PASSWORDS_ONE_RUN.sql`: aligns private module accounts
  to the same original role passwords; it creates no new password scheme.
- `app/build.gradle.kts`: versionCode 249, versionName 2.49.

All unrelated behavior remains unchanged.
