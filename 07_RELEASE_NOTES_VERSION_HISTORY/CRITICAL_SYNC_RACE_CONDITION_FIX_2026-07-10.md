# Critical Fix — Cloud Sync Data-Loss Race Condition (2026-07-10)

## The problem
Right after real Supabase tables were created and sync started actually
working for the first time, a new class of bug became possible: saving a
record locally (Enquiry, Patient Registration, Payment, Follow-up) and a
cloud pull/realtime update landing moments later — before that record's own
upload to Supabase had finished — could cause the just-saved record to
silently vanish from the local list. This matched exactly what was
reported: "Registration Success" toast appears, but the new patient then
shows nowhere in the Visit or Patient list.

This could never have happened before today, because Supabase never had
any tables — every pull silently failed, so the (already-present) "treat
cloud as authoritative" merge logic never actually ran for real. Today's
schema setup made pulls succeed for the first time, which is what exposed
this.

## The fix — done once, at the root, not patched file-by-file
Rather than hunting down and patching every individual Enquiry/Registration/
Payment/Follow-up save function one at a time (which risks missing one, and
is exactly the kind of repeat-report situation asked to be avoided), the fix
was made inside the **one shared `save()` function that every single save in
the entire app goes through**, no matter which screen or table:

- Any row saved through a genuine local write (not a save that's itself just
  writing pulled/merged cloud data back to storage) is automatically marked
  "protected" for the next few seconds if it was just touched (based on its
  own `updatedAt`/`createdAt` timestamp).
- A cloud pull or realtime update landing during that window can no longer
  silently overwrite it — the local copy wins until its own upload has had
  time to complete.
- This is timestamp-scoped, not "protect everything forever" — a full table
  re-save (e.g. registration re-saves the whole patients array, not just the
  one new patient) only protects the row(s) actually touched in the last few
  seconds, so this stays correct and fast even with hundreds of existing
  records.

## Verified
- Enquiry save, Patient Registration save, Treatment Payment save, and
  Follow-up save were all tested directly against the exact race condition
  (a cloud pull with the new record *not yet present*, landing right after
  the local save) — the record survives in all four cases, with **zero**
  changes needed to those four functions individually; the fix in `save()`
  covers all of them automatically.
- Re-tested with 80 pre-existing patients already in the table to confirm
  the newly-registered patient doesn't get pushed out of the protected list
  by the old ones (only recently-touched rows are protected).
- Full existing regression suite re-run: mobile input, duplicate-mobile
  handling, patient journey, and — importantly — **branch-based visibility**
  (Master sees all branches; each staff member sees only their own branch;
  a staff member's own created record stays visible to them even outside
  their current branch view) all still pass exactly as before. Branch
  scoping is a separate, intentional access-control feature and was not
  touched by this fix.

## What this means going forward
Because the fix lives in the one shared save path, any future feature that
saves any kind of record — Enquiry, Registration, Payment, Follow-up,
Medical record, Doctor/RMP entry, Staff briefing — is automatically covered
by this protection without needing a separate fix each time.
