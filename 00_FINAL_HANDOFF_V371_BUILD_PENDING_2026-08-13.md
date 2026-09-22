# V371 FINAL HANDOFF — BUILD PENDING

- Android version: `371 / 3.71`
- Web cache version: `v371`
- Changed work: Dialer and Work Notebook SIM selection now provide safe Back/Cancel navigation.
- Back from SIM list returns to the previous chamber-number question.
- Cancel/device Back from the first question returns to the previous screen without saving a choice.
- Chamber Date was investigated and proofed only; no Chamber Date change was applied.
- No database, Supabase, call counting, design, role, payment, patient, prescription, or other workflow was changed.

## Verification

- Android/Web version alignment: PASS
- Web JavaScript syntax: PASS
- Changed-file scope comparison: PASS
- ZIP integrity: to be checked after packaging
- Android Gradle build: NOT COMPLETED in this workspace because the required Gradle download was blocked by the network. Build must be run in Android Studio.
