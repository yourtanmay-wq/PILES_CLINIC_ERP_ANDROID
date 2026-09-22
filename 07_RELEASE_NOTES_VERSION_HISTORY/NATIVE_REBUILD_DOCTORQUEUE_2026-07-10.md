# NATIVE REBUILD — Doctor Queue (2026-07-10)

## What this adds
The Visit / Doctor Queue is now a real native screen (was WebView-only before).
Reached from a new "Doctor Queue" tile on the native Dashboard.

## Files added (6)
- native/DoctorQueueModel.kt      — queue filter/sort, mirrors visitQueueRows()
- native/DoctorQueueRepository.kt  — reads "patients" table via SupabaseClient
- native/DoctorQueueAdapter.kt     — RecyclerView adapter
- native/DoctorQueueActivity.kt    — screen (mirrors DoctorVisitActivity)
- res/layout/activity_doctorqueue.xml
- res/layout/item_queue_card.xml

## Files wired (3)
- AndroidManifest.xml              — registered DoctorQueueActivity
- res/layout/activity_dashboard.xml — added tileDoctorQueue
- native/DashboardActivity.kt      — tile opens DoctorQueueActivity

## Behaviour (matches WebView doctorQueue())
- Shows patients where queue==true OR stage in (Doctor Queue, Visit), not yet
  doctorComplete, branch-scoped (Master/All = all branches; staff = own branch),
  newest first.
- Per patient: Check-up + Summary open the existing native Clinical hub for that
  patient (via RoleSession extras); Print opens the native Print Center.
- Role gate: master / doctor / staff only.

## Scoped limitations (honest)
- Seed-record filter is a conservative approximation of app.js isSeededRecord()
  (real patients always carry clinic metadata, so none are ever hidden).
- The WebView's "float last-opened patient to top" nicety is not reproduced.

## MUST DO
- Build in Android Studio and test on a real phone before release.
- If Android Studio shows any red error, copy it back so it can be fixed.
