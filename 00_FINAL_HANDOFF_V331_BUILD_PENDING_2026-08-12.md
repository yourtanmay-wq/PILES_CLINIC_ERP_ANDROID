# PILES CLINIC APP V331 — COMPLETE PROJECT HANDOFF (BUILD PENDING)

Date: 12.08.2026 (IST)

- Root folder for delivery: `PILES_CLINIC_APP_V331_BUILD_PENDING`
- Android versionCode: `331`
- Android versionName: `3.31`
- One-time SQL: `03_SUPABASE_SQL/V331_MEDICINE_DEFAULTS_ONE_TIME.sql`
- SQL status: owner screenshot on 12.08.2026 showed `Success. No rows returned`.

## V331 owner-approved result

The existing medicine-picker design is unchanged. When a Prescription or
Medicine Slip is saved, Medicine Name + Type + Dose + When + Days are kept on
the phone for instant/offline use and in the small Supabase
`medicine_defaults` table for reinstall/other-device recovery. A later owner
change becomes the new default. An older delayed retry cannot overwrite a
newer change. `Arshakuthar Rasa = Tab` is owner-confirmed; no other Type was
guessed.

## Locked protection

No AI/person may change this medicine-default rule, any design, patient/finance/
RMP/Branch/role logic or another existing feature without TK Biswas's explicit
permission. Every future version must preserve this note and the dated work log.

## Verification truth

- Source connections, Save paths, Version identity, SQL/RLS/trigger file and
  unchanged Android resource/design files were checked.
- Supabase SQL execution was confirmed by the owner's screenshot.
- A real Gradle/Android build was not possible in this workspace because the
  Gradle 8.5 distribution was not cached and its download was blocked.
- Therefore this package is honestly named `BUILD_PENDING`, not `FINAL` or
  `BUILD_PASSED`. Android Studio must perform the real build before live use.

Full details: `V331_WORK_LOG_MEDICINE_DEFAULTS_2026-08-12.md`.
