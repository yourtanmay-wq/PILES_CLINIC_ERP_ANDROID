# NATIVE REBUILD — Briefing / Notice Board (2026-07-10)

## What this adds
Briefing / Notice Board is now a native screen (new "Briefing" tile on Dashboard).
Master posts notices to All Staff / a Branch / a Role; staff see notices that
target them, mark Seen, and Reply. Reads/writes the live Supabase "briefings"
table.

## Files added (5)
- native/BriefingModel.kt       — targeting/seen/reply logic, ported from app.js
- native/BriefingRepository.kt   — Supabase read/post/seen/reply
- native/BriefingAdapter.kt      — list adapter
- native/BriefingActivity.kt     — screen (list + post + seen + reply)
- res/layout/activity_briefing.xml, res/layout/item_briefing_card.xml

## Files wired (3)
- AndroidManifest.xml, activity_dashboard.xml, DashboardActivity.kt

## Scoped limitations (honest)
- Post targets = All Staff / Branch / Role (per-individual mobile picker later).
- Delete briefing / delete single reply not exposed here yet (view/seen/reply only).

## Note on "Appointment"
The WebView's Appointment (openAppt/saveAppt) is a PUBLIC WEBSITE booking form
that creates an enquiry (disease="Public Appointment", stage="Inquiry"). It is
not a separate staff screen — those bookings already land in the native Enquiry
list. So it belongs to the public web build, not the native staff dashboard.

## Still to port (genuine staff features)
- Follow-up Calendar (calendar view of follow-ups)
- User & photo management (passwords, user/patient photos)

## MUST DO before live: build in Android Studio; send back any red errors.
