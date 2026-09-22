# PILES CLINIC APP V330 — COMPLETE PROJECT HANDOFF

## Identity

- Android versionCode: 330
- Android versionName: 3.30
- Status: FINAL_BUILD_PENDING

## Owner-approved work

- A second Prescription for the same patient on the same day requires an explicit Yes/No confirmation.
- Fast repeated taps cannot start two saves.
- A failed verification cannot silently create another Prescription.
- Full Journey displays the real saved time for Prescription and Registration records and sorts same-day events by that time.
- Registration, Visit and its first genuine Visit Fee are shown as the owner's one action; database/payment/audit rows remain intact.
- Any extra accidental Visit Fee remains visible and is not concealed.

## Scope lock

- No SQL, table, financial calculation, role rule, Branch rule, layout or unrelated workflow was changed.
- Missing historical times are never invented.

## Honest build status

- Source/static checks must pass before packaging.
- Full Gradle build cannot be claimed in this environment until the Gradle distribution is available.
