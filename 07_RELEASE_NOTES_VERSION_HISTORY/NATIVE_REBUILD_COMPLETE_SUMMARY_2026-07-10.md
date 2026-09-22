# NATIVE REBUILD — SUMMARY (2026-07-10)

Dashboard now has native tiles for: Enquiry, Registration, Follow Up, Payment,
Doctor Visit, Doctor Queue, Briefing, Calendar, Users, Print (+ Menu fallback).

## Native now (built this session, on top of what already existed)
1. Print button wired to Dashboard (Print Center was unreachable before).
2. Doctor Queue — patients waiting for doctor; Check-up / Summary / Print.
3. Print Center completed — added Payment Receipt, Doctor Visit Print, and
   Blood Test / Investigation (16-test list ported from app.js). Now covers all
   WebView print types.
4. Briefing / Notice Board — post (master) / view / seen / reply.
5. Follow-up Calendar — month grid of due follow-ups; tap a date to list them.
6. Password Center (User management) — master views/changes user passwords
   (saved to Supabase usercredentials).

## New files (this session)
native/: DoctorQueue{Model,Repository,Adapter,Activity}, Briefing{Model,
Repository,Adapter,Activity}, FollowCalendarActivity, PasswordCenter{Repository,
Adapter,Activity}. print/: PrintMappersCloud. layouts: activity_doctorqueue,
item_queue_card, activity_briefing, item_briefing_card, activity_follow_calendar,
activity_password_center, item_credential_card. Plus edits to Dashboard
(layout+code), AndroidManifest, activity_print_center + PrintCenterActivity,
StaffDirectory (allAccounts accessor).

## Deliberately NOT ported (with reasons)
- Appointment: it is a PUBLIC WEBSITE booking form that creates an enquiry;
  those bookings already appear in native Enquiry. Belongs to the web build.
- User/patient PHOTO management: passwords are done; photo upload/attach is a
  smaller sub-feature not yet ported.
- The Dashboard "Menu" tile still opens the WebView as a fallback for anything
  not yet native.

## HONEST STATUS — READ THIS
- All of the above is written but has NOT been compiled or run even once here.
- On the FIRST Android Studio build, expect some red errors to fix — that is
  normal for new code of this size. Send any error text back and it gets fixed.
- Known things to verify live: the OLDER Print Center cards (Registration etc.)
  still read Room while live data is in Supabase; the Clinical hub hand-off from
  Doctor Queue (Check-up/Summary) uses RoleSession extras and should be checked
  on a real patient.

---
# UPDATE 2 — Patient Photo + Briefing Delete added
- Patient Photo: native/PatientPhoto{Repository,Activity} + layout + Dashboard
  "Patient Photo" tile. Search by mobile, pick gallery image, downscale+compress,
  save to Supabase patients.photo (+ mirror to followups).
- Briefing Delete/Hide: master soft-deletes for all; staff hides for self.
  (BriefingModel.buildMasterDelete/buildHideForUser + repository.deleteOrHide +
  card delete button.)

## Only optional item left
- Staff/doctor AVATAR photo. In the original this is device-local (localStorage),
  purely cosmetic (dashboard avatar), no clinic-data impact. Not built; can be
  added on request. Everything functional is now native.
