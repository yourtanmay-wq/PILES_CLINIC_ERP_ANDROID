# CRITICAL Performance Fix Report — 2026-07-10

## What was reported
- Touch/buttons appearing dead or extremely delayed
- Switching Enquiry <-> Visit tabs taking 20+ seconds, sometimes more
- The app closing by itself
- Enquiry -> Registration auto-fill sometimes not working
- Enquiry created by one staff member not visible to others (separately
  explained as a Supabase configuration/setup matter, not a code bug)

## Root cause found (confirmed with real timing measurements, not guesswork)

A function called `repairAtoZWorkflowFormula()` — meant to silently repair
any patient missing their "Visit" stage follow-up record — was wired to run
in full, for EVERY patient in the clinic's records, every single time:
- the Home/Dashboard screen was opened, AND
- the Enquiry/Visit/Patient follow-up tabs were opened or switched.

For each patient, it reloaded and re-saved the entire followups list (and
often the entire enquiries list) from scratch, and attempted a cloud sync —
all repeated per patient. With only a few records this is invisible. With
~400 real patient records (roughly what a multi-branch clinic accumulates
over a month or two), this measured out to **10,000–14,000 milliseconds**
of the app being completely frozen, every single time a tab was opened —
exactly matching "20+ seconds to switch tabs" and severe enough that Android
can and does force-close an app whose screen stops responding for that long.

This single root cause is the most likely explanation for the touch
freezing, the 20+ second tab switches, and the app auto-closing, all at
once. It also explains the Enquiry -> Registration auto-fill complaint: the
auto-fill code itself was tested directly and found to be correct (see
below) — it was very likely just badly delayed by this same freeze,
making it look broken when a screen was simply still catching up.

## Fix
1. The repair now runs at most **once per app session** instead of on every
   single screen open (it's a repair for historical gaps; re-running it
   again moments later fixes nothing new, so repeating it endlessly was
   pure wasted work).
2. The repair itself was rewritten to read each list once, fix every
   patient in memory, and save once at the end — instead of a fresh
   reload-and-save cycle per patient. This cut even the first, one-time run
   from 10-14 seconds down to well under 100ms for 400 records.
3. Three other functions with the exact same "reload the whole list and
   re-scan it for every row" pattern (`v266PatientByFollow`,
   `v267PatientByFollow`, `v266TreatmentPaidForPatient`, `mergeFollow`,
   `patientForMobile`) were fixed to share one lookup table built once per
   screen instead of rebuilding it per row.

## Verification (headless Chromium, Playwright, real app.js/config.js)
Seeded 400 realistic patients/enquiries/followups/payments (simulating
real accumulated clinic data):

| Scenario | Before | After |
|---|---|---|
| Open Enquiry tab (1st time this session) | 10,000-14,000 ms | ~85 ms |
| Switch to Visit tab | 10,000+ ms | ~17 ms |
| Switch back to Enquiry tab | 1,100+ ms | ~17-22 ms |
| Switch to Visit tab again | 10,000+ ms | ~15 ms |

Correctness re-verified after the rewrite (all pass):
- A patient with a matching active Enquiry gets a Visit-stage follow-up
  record created, its Inquiry-stage row correctly closed, and its Enquiry
  correctly marked Registered.
- Running the repair a second time does not create duplicate records
  (idempotent).
- A patient whose Visit-stage record already existed correctly is left
  alone — no duplicate, existing history preserved.
- The live "Continue Registration" auto-fill path
  (`finalContinueRegistration`) was tested directly: name, mobile, and
  branch all filled correctly, immediately.
- Full existing regression suite re-run (mobile input, duplicate-mobile
  handling, role/branch visibility, full patient journey, Issue #2 collection
  search bar) — all identical to baseline, no new errors.

## Still open / not yet done in this pass
Code-reading also found that "Continue Registration", "Enquiry Mobile
Check", and "Registration Mobile Check" each exist as 3 separate,
overlapping versions in the file (leftover from repeated past patches).
Only ONE version of each is actually wired to a real button today (the
others are dead code), and the live versions were directly tested and found
correct — so this was not the cause of any reported bug. It is still a
genuine maintainability risk (confusing to read, easy to edit the wrong
copy by mistake) and worth cleaning up in a future pass if wanted.

## What was NOT touched
No business rule, calculation, workflow, screen, or button changed — every
fix in this report only changes *how many times* the same lookup/repair
work gets redone, not *what* it decides or *what data results*.
