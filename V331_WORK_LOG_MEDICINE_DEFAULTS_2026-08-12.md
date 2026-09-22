# V331 WORK LOG — PERMANENT MEDICINE DEFAULTS

Date: 12.08.2026 (IST)  
Owner approval: TK Biswas  
Base: V330  
New identity: versionCode 331 / versionName 3.31

## Locked owner decision

When a Prescription or Medicine Slip is saved, that medicine's Name, Type,
Dose, When and Days become its future default. They remain unchanged until the
owner/user deliberately changes and saves them again. The visible design must
remain exactly the same.

## Verified old defect

- Type/Dose/Days were stored only in Android SharedPreferences.
- Reinstall, app-data clear or a different phone could lose those defaults.
- Historical `medical` Prescription rows did not contain a structured medicine
  Type, so they could not safely rebuild it.
- `Arshakuthar Rasa` had a hard-coded dose but no hard-coded Type; therefore the
  picker truthfully showed the generic `Type` badge.

## Changes made

1. Added a small `medicine_defaults` cloud table setup file.
2. Kept SharedPreferences as the instant/offline cache; no screen waits for cloud.
3. Added a maximum one small refresh per 15 minutes while medicine features are used.
4. Saving a Prescription/Slip now stores Type + Dose + When + Days together.
5. A delayed older write is blocked from overwriting a newer saved default.
6. Type selection is not made permanent merely by tapping; it becomes permanent
   only when the medicine/Prescription is actually saved.
7. Owner-confirmed `Arshakuthar Rasa = Tab` is seeded. No other Type was guessed.

## Scope protection

No layout/XML/design, patient, payment, refund, RMP, role, Branch, print design,
or existing history row was changed. The new table contains no patient, money,
photo or medical-history record.

## Files changed

- `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/clinical/MedicineDefaultsCloudRepository.kt` (new)
- `.../clinical/ClinicalRepository.kt`
- `.../clinical/MedicinePickerDialog.kt`
- `.../clinical/PrescriptionActivity.kt`
- `.../clinical/MedicineSlipActivity.kt`
- `.../PilesClinicApplication.kt`
- `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts`
- `03_SUPABASE_SQL/V331_MEDICINE_DEFAULTS_ONE_TIME.sql` (new, one-time)

## Honest verification limit

Static source/connection checks were performed. A real Gradle build could not be
completed in this workspace because Gradle 8.5 was not already cached and the
required download was blocked. Android Studio build and live Supabase test remain
required before a FINAL claim.
