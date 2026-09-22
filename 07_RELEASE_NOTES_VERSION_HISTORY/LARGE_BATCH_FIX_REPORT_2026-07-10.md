# Large Batch Fix Report — 2026-07-10 (evening session)

Scope: everything requested in the single long message covering mobile
copy-paste, calendar, camera, 3-tap payment protection, View All redesign,
print document formatting, and Collection list click-through.

## 1. Mobile number copy-paste (all sections)
**Root cause**: `eMob` and `pMob` inputs had `maxlength="10"` — a CHARACTER
limit, not a digit limit. Pasting `+918001080080` (13 characters) got
truncated by the browser to the first 10 *characters* before our JS ever
ran, corrupting the number.
**Fix**: removed `maxlength` from both fields; digit-counting is now done
entirely in JS. Also fixed `sanitizeMobileInput()` to detect a 12-digit
paste starting with the `91` country code and strip it correctly (previously
it just kept the first 10 raw digits, which kept `91` + 8 real digits and
dropped the last 2). Extended the same protection to the public appointment
form (`apMob`) and direct-print form (`dpMobile`), which previously had no
sanitization at all.
**Verified**: pasting `+918001080080`, `918001080080`, `8001080080`, and
`+91 80010 80080` (with spaces) all now correctly produce `8001080080`. A
genuine 10-digit number that happens to start with `91` (e.g.
`9198765432`) is left untouched (not mistaken for a country-code paste).

## 2. Enquiry mobile auto-fills into Registration
Tested directly against the live `finalContinueRegistration` code path —
name, mobile, and branch all filled correctly. This was already working;
no change needed here.

## 3. Calendar "Set" option hidden
This is a **native OS/WebView date-picker dialog**, not something rendered
by our HTML/CSS — Android draws it outside the page. I cannot reproduce or
directly control its own button layout without a real device. As a
precaution, added extra bottom padding to the bottom-sheet modal in case an
in-page picker variant was getting its confirm button clipped by the
modal's scroll area. **This one still needs confirmation on a real device.**

## 4. Camera not opening (Registration photo)
**Root cause**: the native Android file-chooser handler only used
`fileChooserParams.createIntent()`, which is a generic document/gallery
picker — it does not reliably launch the camera even though the HTML
button used `capture="environment"`.
**Fix**: `MainActivity.kt` now builds a real camera intent
(`MediaStore.ACTION_IMAGE_CAPTURE`) writing to a FileProvider Uri (the
FileProvider was already configured in the manifest, just unused) when the
web page's "Open Camera" button is pressed, and offers it via
`Intent.createChooser` alongside the normal picker. The result handler
returns that photo's Uri back to the WebView.
**Photo compression**: already handled correctly on the JS side
(`fileData()` resizes to max 520px and iterates JPEG quality down until
under ~160KB) — this applies automatically to camera photos too, no native
changes needed for compression.
**Verification status**: static-checked (imports, FileProvider config,
manifest) — camera intents cannot be exercised without a real device or
emulator, which I don't have access to. Needs real-device confirmation.

## 5. Three-tap-to-edit rule (Bill / Advance Payment)
- **Bill field**: tested directly — already correctly protected on both the
  Visit card's Advance Payment screen and the Patient/Treatment card's
  payment-circle screen (readonly until exactly 3 taps within 1.2s). No
  change needed; this was already working.
- **Advance Payment amount field**: previously had **no** protection at
  all — freely editable always. Added the same tap-lock pattern: once an
  amount is typed and the field loses focus (blur), it locks; 3 taps
  within 1.2 seconds unlocks it again. Applied to both the Visit card's
  Advance Payment and the Patient card's Treatment Payment amount fields.
  Verified with a direct test: locks after blur, stays locked after 2
  taps, unlocks on the 3rd.

## 6. "View All" timeline redesign
- Patient identity header is now a premium gradient card (matches the
  branch/blue-green palette used elsewhere) with the current stage shown
  as a highlighted pill, instead of a plain white box.
- **Order reversed**: most recent update now shows first (was oldest-first
  before), so staff immediately see "what happened last" — e.g. why
  treatment hasn't started after registration — without scrolling through
  months of old history first.
- Each entry now has a type icon and colour accent (Enquiry/Visit/
  Payment/Medical) for faster scanning.
- Long histories now show the 5 most recent entries with a "View More (N)"
  button to reveal the rest, instead of dumping everything at once.
- Verified with a 7-entry test history: dates render newest-to-oldest, and
  "View More (2)" appears correctly for the 2 entries beyond the first 5.

## 7. Print document formatting (Prescription / Medicine Slip / Diet
   Chart / Blood Test)
**Root cause**: all four screens share one function, `patientDetailsPanel()`
— it crammed Name/Age/Mobile/First Visit/Address/Disease/Branch into a
cramped 2-column grid at 11.5px font, so anything long (especially
Address) wrapped awkwardly mid-word.
**Fix**: redesigned once (fixes all four screens at once): patient name is
now a prominent header line, short fields sit in a clean 2-column
label-above-value grid, and Address gets its own full-width row below a
divider so it never gets squeezed. Verified with a real long address
("Vill: Kanji vada , PO: panjipara") on all four screens (Prescription,
Medicine Slip, Diet Chart, Blood Test) — no overflow, no clipping.

## 8. Today Collection list — rows didn't open anything
**Root cause**: both the Dashboard's "Today Collection" list and the
Cash/Online/Monthly/History collection list screens rendered each row as a
plain `<div>` with no `onclick` at all — a dead click, by design oversight,
not a bug that broke something that used to work.
**Fix**: added a new `showCollectionRowDetails(mobile)` function; tapping
any row on either screen now opens a "Payment Details" modal showing every
payment matching that row's mobile number (name, branch, total, and each
individual payment's date/amount/mode), not just the one row that was
tapped. Verified: tapping a seeded row opens the modal with the correct
amount shown.

## Verified across the whole batch
Full existing regression suite (mobile input, duplicate-mobile handling,
role/branch visibility, full patient journey, Issue #2 collection search
bar) re-run after every change in this batch — all identical to baseline,
zero new JS errors. All 60 Kotlin files and 21 XML files re-verified with
the same static checks used throughout this project (comment/brace/string
balance, resource cross-references, manifest registration) — zero issues.

## Still needs real-device confirmation (cannot be verified in this sandbox)
- Calendar "Set" button visibility (native OS dialog)
- Camera capture actually opening and returning a usable photo
