# PILES CLINIC ERP — MASTER READY PACK V1
## Source of Truth / Future-Proof Lock Book
Generated: 2026-07-09 12:05

Project Code: **মুখে রাখো সুপার হাসি**  
Workflow Code: **FUTURE PLAN**  
All-in-one Package Code: **MASTER READY PACK**

---

# 1. MASTER READY PACK PURPOSE
This package is the future-preservation ZIP for PILES CLINIC ERP. It is intended to keep together:
- Android full source project
- Netlify upload-ready web package
- Supabase setup notes
- Master Lock Book / Source of Truth
- Test checklists
- Release notes / version history
- Original uploaded base ZIPs for reference

Important honesty note:
- APK and AAB are NOT prebuilt inside this pack unless later built in Android Studio.
- This pack contains Android source/build notes so APK/AAB can be generated from the included source.

---

# 2. FUTURE PLAN WORKFLOW LOCK
No audit-on-audit and no premature final/master/ready claims.

Workflow:
1. User uploads/uses the file.
2. Test in real Netlify / Android / mobile use.
3. User shares screenshot/video/problem.
4. Create one clean issue list only.
5. Fix only listed issues.
6. Re-test.
7. Only when user says **সব section OK**, call it Master / Netlify Ready / APK Ready / AAB Ready / Release Candidate.

---

# 3. CURRENT LOCKED ISSUE LIST

## Issue #1 — Mobile Number Input
All Mobile Number fields in every form must support:
- Copy/Paste
- Normal typing
- Backspace/Delete
- Existing 10-digit/+91 validation/formatting

Do not touch other fields, forms, UI, workflow, structure, or modules.

## Issue #2 — Branch-wise Collection / Today Collection
Remove:
- extra blank search/input area
- unnecessary blank/loading card

Keep:
- Cash / Online / Total summary
- payment list
- existing collection logic and calculations unchanged.

## Issue #3 — Global Touch / Click Stability
Buttons, cards, menus, and options must work reliably on first touch.

Root cause areas to check:
- invisible overlay
- loading state
- touch event blocking
- z-index / pointer-events
- state/render issue
- performance lag

No per-button workaround. Fix root cause only. Preserve UI/workflow/calculations/modules.

---

# 4. FOLLOW-UP ENQUIRY CARD FINAL LOCK
Approved mobile card layout:
- Left side: Call Progress Signal / Call Count + Enquiry Date
- Patient Name
- Mobile Number shown once only
- Branch Badge + Disease Badge
- Last Remark
- Status Badge if needed (Today Due / Overdue / etc.)
- Actions: Call / WhatsApp / View / Next
- NO separate Payment Progress text/bar on Enquiry card
- NO duplicate mobile number

Reason:
Enquiry is before Registration/Treatment/Payment, so separate Payment Progress is not valid.

---

# 5. FOLLOW-UP VISIT CARD FINAL LOCK
Approved mobile Visit card layout:
- Left side: Visit Date only
- Do NOT show “Today Visit / আজ Visit” at top-left
- Branch + Disease
- Last Remark
- Do NOT show Treatment Started below remarks
- Do NOT show Treatment Progress below remarks
- Top-right badge/button: **Advanced**
- Tapping Advanced opens **Bill & Advanced Window**
- Preserve rest of approved Visit card layout.

---

# 6. FOLLOW-UP PATIENT CARD FINAL LOCK
Approved mobile Patient card layout:
- Left column: Patient Photo
- Under Patient Photo: Patient ID only
- NO Registration Date under photo
- Center: Name, Mobile, Branch Badge, Disease Badge, Last Remark, Next Follow-up Date, Action Buttons
- Do NOT show Treatment Progress text/bar under remarks
- Do NOT show Payment Progress text/bar under remarks
- Right column: **Prescription** label
- Under Prescription: circular percentage indicator
- Under circle: Bill / Due amount, e.g. `20000 / 15000`
- Do not redesign without user approval.

---

# 7. GLOBAL MOBILE CARD UX GOAL
The target is not only beauty. The goal is production speed:
- Staff can identify patient/enquiry in 1 second.
- Staff can understand stage/status in 1 second.
- Staff can act in 1 touch: Call / WhatsApp / View / Advanced / Next.
- Less scrolling.
- Less staff confusion.
- Mobile-first card design suitable for large patient volumes.

---

# 8. PRINT / PRESCRIPTION LOCK REMINDER
Prescription Print final correction:
- TK BISWAS = Founder Consultant
- Dr. K.H. Mandal = Doctor
- Do not wrongly show TK BISWAS as the doctor with Regd No. 12386.

Registration Print:
- Patient Photo field exists in registration form.
- Printed documents do not include patient photo unless separately changed.
- Large photos must be compressed/optimized so registration never fails due to photo size.

---

# 9. OFFLINE / SYNC LOCK REMINDER
ERP/Android must support offline working:
- Registration / Payment / Follow-up should not stop during internet outage.
- Save locally as Pending.
- Auto-sync with Supabase when internet returns.
- Show sync status such as Pending / Synced / Failed where applicable.

---

# 10. CHANGE CONTROL
Before any code/database/file change:
1. Explain issue.
2. Explain fix plan.
3. State what will NOT be touched.
4. Get user permission.
5. Fix only approved issue.
6. Provide test report with Pass/Pending.

---

# 11. CONTENTS OF THIS PACK
- `02_ANDROID_SOURCE_CODE/` — Android source project from uploaded master base.
- `03_NETLIFY_READY/` — Netlify upload-ready package.
- `04_SUPABASE_DATABASE_SETUP/` — Supabase setup notes if present.
- `05_APK_AAB_BUILD_NOTES/` — Android Studio build notes.
- `06_TEST_CHECKLISTS/` — Real mobile/Netlify/Supabase test checklist.
- `07_RELEASE_NOTES_VERSION_HISTORY/` — Version notes.
- `09_ORIGINAL_UPLOADED_FILES/` — original uploaded ZIPs/report for backup.

---

# 12. READY STATUS
Current pack status:
- Netlify package included: YES
- Android source included: YES
- Supabase setup note included: YES if source contained it
- APK included: NO, must build in Android Studio
- AAB included: NO, must build in Android Studio
- Real Android test: PENDING
- Live Supabase login/save/sync test: PENDING

Only after real test + user confirmation **সব section OK**, this can be promoted to final release.
