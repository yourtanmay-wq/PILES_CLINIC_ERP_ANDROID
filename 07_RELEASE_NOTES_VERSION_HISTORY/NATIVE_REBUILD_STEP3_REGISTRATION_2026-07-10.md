# Native Rebuild — Step 3: Registration (2026-07-10)

## What this is
Registration is now a real native Kotlin screen (`RegistrationActivity`),
reached from the native Dashboard's "Registration" tile. Enquiry (Step 2)
and Login/Dashboard (Step 1) are unchanged. Every other module (Follow-up,
Payment, Print, etc.) is still reached via the WebView as before.

## What was built
- `PatientIdGenerator.kt` — reproduces the WebView's exact Patient ID
  formula (`patientId()`/`lockedBranchCode()`/`lockedDateCode()`/
  `nextPatientSerial()`): branch code (KNE/JPE/COB/FLK/BIR) + reversed date
  (ddMMyyyy) + an incrementing 3-digit serial per branch+date, computed by
  querying Supabase for existing IDs with the same prefix.
- `PatientModel.kt` — builds the "patients" row, the matching Visit-stage
  "followups" row, and the Visit Fee "payments" row exactly like
  `savePatient()` does (including its address-building rule: only
  non-empty Vill/PO/PS/Dist/PIN parts, joined as "Label: value, Label:
  value").
- `RegistrationRepository.kt` — duplicate-patient check (patients table
  only — matching the WebView's rule that an existing *Enquiry* is a
  conversion path, not a block, only an existing *Patient* is), saves all
  3 rows, same offline pending-queue fallback pattern as Step 2.
- `RegistrationActivity.kt` + `activity_registration.xml` — the full form:
  Basic Info, Address, Other Info, Disease (checkboxes), Complaint
  (checkboxes + note), Medical History (checkboxes), Fees.

## Validation — matched exactly to savePatient()
| Rule | Native | WebView |
|---|---|---|
| First Visit Date cannot be in the future | ✅ | ✅ |
| Patient Name mandatory | ✅ | ✅ |
| Mobile must be a valid 10-digit number | ✅ | ✅ |
| Branch mandatory | ✅ | ✅ |
| Registration Fee mandatory (> 0) | ✅ | ✅ |

## Scoped limitations for this step (on purpose, disclosed clearly)
- **Patient Photo**: not captured on this screen yet. Registering here
  saves the patient with no photo — nothing is blocked, a photo can still
  be added afterward from the WebView's existing photo screens. Camera
  capture will be added as its own focused piece once the core form is
  confirmed working on a real device.
- **Existing-patient re-registration**: the WebView offers to update an
  existing Patient record in place when the same mobile registers again.
  This native version always creates a new record and just warns staff
  first — the WebView remains the tool for that specific "same patient,
  re-registering" correction case for now.
- **Enquiry → Registration pre-fill**: opening Registration from an
  existing Enquiry (auto-filling name/address/etc.) isn't wired yet — staff
  re-enter the details, same as a fresh walk-in. This will connect once
  Enquiry and Registration are linked in a later refinement.

## Verified in this sandbox
- All 73 Kotlin files: brace/parenthesis/string balance check passed.
- All 32 XML files: well-formed.
- Every `binding.xxx` reference in `RegistrationActivity.kt` — all ~20 of
  them — cross-checked one-by-one against `activity_registration.xml`'s
  actual `android:id`s: 100% match, no typos.
- Every `@style/...`, `@drawable/...`, `@color/...` used in the new layout
  cross-checked against what's actually defined: all exist.
- Full existing JS regression suite re-run — unaffected (this step touched
  no `app.js` code).

## NOT verified — same standing limitation as every step so far
Not compiled or run (no Android SDK/Gradle/emulator/device in this
sandbox). Please Gradle-sync and test on a device:
1. From the native Dashboard, tap "Registration".
2. Fill in all mandatory fields, try saving with the Fee left blank —
   confirm it's blocked with the right message.
3. Register a genuinely new patient with a real fee amount, confirm the
   Patient ID format looks right (e.g. `JPE-10072026-00X`) and the patient
   appears correctly in the WebView's Visit tab.
4. Register with a mobile number that already exists as a Patient —
   confirm the warning dialog shows the correct existing name/branch/ID.
5. Turn off connectivity, register a patient, confirm "will sync when
   online"; reconnect, reopen Registration, confirm it uploads silently.

## Next step
Follow-up, as previously discussed.
