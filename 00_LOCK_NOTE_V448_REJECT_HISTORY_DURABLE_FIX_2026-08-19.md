# V448 — Reject history durable fix — 19.08.2026 · 12:13 PM IST

## TK live proof
V447 was installed and running, but old rejected Inquiry numbers (e.g. Neha / Shyam / UNKNOWN shown in TK's live screenshot) still appeared in Follow-up. Therefore V447 was not accepted as a complete fix.

## Verified code root cause (V447)
1. Follow-up visibility still ultimately trusted the current `status` for old rows.
2. The already-run V407 duplicate-merge SQL deliberately merged every duplicate row's `history`, but its `jsonb_agg(distinct ...)` had no ORDER BY and the UPDATE did not merge/derive `status`. Therefore a kept duplicate could remain `Active` even though a sibling had been genuinely Rejected/Cancelled; the Reject evidence survived inside history, and history order was no longer guaranteed.
3. Independently, Web `ensureFollow()` was a generic create/heal helper but, when it found an existing follow-up, it merged a new row containing `status:'Active'`. That was another path that could overwrite a real old terminal status while retaining history.
4. V447 therefore could no longer recognize those legacy rows from status alone.
5. Web Enquiry Reject Restore also assumed the Enquiry row id was a Follow-up id; those ids are not guaranteed to match.

## V448 exact fix
- Android + Web classify explicit Inquiry history actions by their saved date/time; they do **not** trust array position, because V407 history merge order was unspecified.
- The newest explicit Reject/Cancelled/Incomplete/Closed action wins.
- A provably newer explicit Restore/Continue action wins over the old Reject; ambiguous old same-day/no-time history stays safely in Reject until the user explicitly Restores it once.
- Ordinary free-text remarks are ignored; a sentence containing the word “rejected” is not enough to hide a record.
- Web `ensureFollow()` can no longer reactivate an existing terminal row. Generic view/heal is not Restore.
- Explicit Reject List Restore/Continue appends a durable `status=Active` history marker and reactivates same-mobile Inquiry siblings/enquiry rows.
- Android Draft → Enquiry Reject also includes a legacy row whose current status was corrupted Active but whose last explicit history action is Reject, so the record remains recoverable through the normal Restore route.
- Local/self-heal terminal guards now also treat `Rejected` as terminal.
- Android clears only the old derived Follow-up display cache once on first V448 use, because V447 cached cards do not contain status/history and could otherwise briefly re-show a legacy Reject before the fresh read. No clinical/offline-pending row is cleared.

## Not changed
No patient/payment/salary/medical/RMP calculation, UI design, branch rule, call-count rule, Supabase schema, or delete logic was changed. No existing record is deleted by this fix.

## Verification required before release
- JS syntax check.
- 7 history-state fixtures (Reject, accidental Active overwrite, Restore, Continue, new Reject after Restore, free text).
- Generic `ensureFollow()` resurrection test.
- TK project guard.
- Android Studio live build remains the final device-side compile check if this environment cannot obtain Gradle.
