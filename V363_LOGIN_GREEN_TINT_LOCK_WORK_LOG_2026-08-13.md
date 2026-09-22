# V363 Work Log — 2026-08-13 UTC

- Verified owner-approved Login button color is green `#07883F`, not navy blue.
- Root cause: Android's automatic Button background tint could cover the existing green drawable.
- Added `backgroundTint=null` only to the Login button, so the existing green drawable remains visible.
- Web Login button was already green and required no functional change.
- No Login logic, text, size, position, input, password, Forgot Password, header, logo, or other screen/design was changed.
- Supabase Free Plan: no database, storage, network, SQL, or paid-feature impact.
- Android version 3.63 (versionCode 363); Web cache/version stamp v363.
