# V352 WORK LOG — ANDROID + WEB MEDICINE DEFAULTS

## 2026-08-13 00:43 IST (2026-08-12 19:13 UTC)

Owner approval: Web must receive the same permanent medicine Type/Dose/When/Days behaviour as Android, with one small Type box and without changing the remaining design.

Completed and verified:

- Android identity advanced from V331 / 3.31 to V352 / 3.52.
- Web cache identity advanced to `app.js?v=v352` and `styles.css?v=v352`.
- Web Prescription and Medicine Slip now show the same 12 owner-approved Type choices as Android.
- Type is mandatory before a medicine can be added.
- Saved/printed medicine defaults are kept locally first and copied to the existing `medicine_defaults` Supabase table.
- Dose and When stay in their existing combined Web field, but are split into the same cloud fields used by Android when the known `number-number-number + Before/After Food` format is used.
- Days are normalized between Web (`5`) and Android/cloud (`5 days`) so `days days` cannot appear.
- Print, share, saved history and selected-medicine preview include Type.
- Cloud refresh is local-first, does not block the screen, and runs at most once per 15 minutes during normal medicine-screen use. Manual Sync may explicitly refresh it once.
- Only `Arshakuthar Rasa = Tab / 1-0-1 / After Food / 5 days` has a built-in default because the owner explicitly confirmed it. No other medicine Type was guessed.
- No other screen design or workflow was intentionally changed.

Verification evidence:

- All 279 Android XML resources parsed successfully.
- Every JavaScript file in `03_NETLIFY_READY` passed `node --check`.
- Static parity checks passed for Type list, shared cloud table, 15-minute guard, mandatory Type, Save persistence and Print Type.
- Android Gradle build was attempted. It could not start because this controlled environment cannot reach `services.gradle.org` to download Gradle 8.5. No Android source/compiler error was reached. Therefore the delivery is honestly named `BUILD_PENDING`; Android Studio must perform the final Gradle Sync/Build.

Database instruction:

- The owner already ran `03_SUPABASE_SQL/V331_MEDICINE_DEFAULTS_ONE_TIME.sql` successfully.
- No new SQL is required for V352. Do not tell the owner to run or save another SQL for this work.

Permanent guard for future AI/developers:

- Never claim Android and Web are both complete without checking both source paths.
- Never reuse an old cache/version identity after changing Web JavaScript or CSS.
- Never guess a medicine Type, Dose, When or Days.
- Never change another design/workflow while implementing this feature without fresh owner permission.
- Never promise an Android build passed when Gradle compilation did not actually run.

