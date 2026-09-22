# V359 Work Log — 2026-08-13 UTC

- User report verified: Android Doctor/RMP call-return form contained only date controls; its separate Remarks input had been removed.
- Restored a required, multiline Remarks field in the Android call form.
- Save Call now writes the entered discussion to the RMP remarks and newest call-history entry while preserving Next Call Date and Expected Patient Date behavior.
- Web verified: its Doctor Call Remarks form already has a required Remarks field and saves it into call history; no web code change was needed.
- Supabase Free Plan impact: no new table, column, storage bucket, function, or paid feature; the existing `doctor_visits` fields are reused.
- Safety scope: no unrelated screen, design, branch filter, count, salary, photo, or login workflow changed.
- Android version: 3.59 (versionCode 359).
- Web delivery cache/version stamp: v359.
- Delivery name: `PILES_CLINIC_APP_V359_COMPLETE_PROJECT_BUILD_PENDING.zip`.
- Build status is stated honestly as pending because this workspace could not download Gradle 8.5; no false zero-error build claim is made.
