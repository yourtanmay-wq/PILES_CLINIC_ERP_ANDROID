# V224 — Changed Files (2026-08-01)

## Source code (build-verified — BUILD SUCCESSFUL, 0 error)
1. `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/FollowUpActivity.kt`
   - Item 4: card tag "UNEXPECTED TIME" → "UNEXPECTED" (display label only; data condition unchanged).
   - Item 8/20: four bottom action buttons height 34dp → 27dp (−20%); "Next Call" button switched from purple `bg_action_next` to the same blue `bg_action_view` as "View All" (Call/WhatsApp already green, View already blue). Same function renders Enquiry/Visit/Patient cards → item 20 satisfied.
2. `02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/ReportsRepository.kt`
   - Item 87: `conversionRate` clamped to 0..100 (a conversion rate is ≤100% by definition; removes the impossible 116%). Deeper same-window/eligible recalculation left for owner's definition + live data.

## Database (separate verified SQL — run in Supabase)
3. `04_SUPABASE_DATABASE_SETUP/V224_2026-08-01_official_patient_id_unique.sql`
   - Item 82/83: partial UNIQUE index on non-blank `patients."patientId"` (Official Patient ID) to block duplicates across devices; orphans (blank/null) and Free Plan untouched; includes a duplicate pre-check SELECT.

## Build enablement (machine-specific — not for git)
4. `02_ANDROID_SOURCE_CODE/PilesClinicApp/local.properties`
   - `sdk.dir=...` so Gradle can find the Android SDK. Machine-specific; Android Studio recreates it. Safe to delete/ignore.

## Docs
5. `00_MASTER_WORK_NOTE_2026-08-01_V224.md` — full 92-item honest triage + 5× verification.
6. `V224_TEST_REPORT.md`, `V224_FINAL_DECLARATION.md`, this file.

## Rollback
- `ROLLBACK_V224/FollowUpActivity.kt.bak`, `ROLLBACK_V224/ReportsRepository.kt.bak`
- SQL rollback: `drop index if exists public.patients_officialid_unique_idx;`

## NOT changed
No design/workflow/permission/branch-rule/payment/print/login/asset/other feature was changed beyond the four items above.
