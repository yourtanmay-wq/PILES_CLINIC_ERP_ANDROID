# NATIVE PRINT — Payment Receipt added (2026-07-10)

## What this adds
The native Print Center now has a "Payment Receipt" card (was WebView-only).
Staff enters the patient's mobile; the app finds the patient in the live
Supabase "patients" table, fetches their payments, and prints the most recent
one as a receipt.

## Files added (1)
- print/PrintMappersCloud.kt  — builds a Payment Receipt PrintDocumentModel
                                 from a live Supabase "payments" row.

## Files changed (2)
- res/layout/activity_print_center.xml — added cardPaymentReceipt.
- print/PrintCenterActivity.kt          — mobile-search picker + fetch + preview.

## Why Supabase (not Room)
The native Payment screen saves to the Supabase "payments" table, so the receipt
reads from there — otherwise it would print blank. Confirmed from
PaymentRepository (SupabaseClient.upsert/fetchList "payments"), not assumed.

## Print types now in native Print Center (6 of 7)
Registration, Prescription, Medicine Slip, Investigation Advice, Diet Chart,
Payment Receipt.

## Still web-only (to port next)
- Doctor Visit Print
- Blood Test print (native has Investigation Advice, which is related but not
  the same as the WebView's dedicated Blood Test slip)

## IMPORTANT — live verification note
The OLDER Print Center cards (Registration, etc.) still read Room, while live
data is in Supabase. On a real device those may print blank until switched to
Supabase too. Flagged here so it can be checked and fixed when the app goes live.

## MUST DO
- Build in Android Studio; if any red error appears, send it back to be fixed.

---

# UPDATE — Print Center now complete (Doctor Visit Print + Blood Test added)

## Added on top of Payment Receipt
- print/PrintMappersCloud.kt: doctorVisitPrint(patient) and
  bloodTest(patient, selectedTests, remarks); BLOOD_TESTS list ported verbatim
  from app.js (16 investigations, same wording/order).
- activity_print_center.xml: cardDoctorVisitPrint, cardBloodTest.
- PrintCenterActivity.kt: mobile-search pickers for both; Blood Test also shows
  a tick-list of the 16 investigations + remarks before printing.

## Native Print Center now covers all WebView print types
Registration, Prescription, Medicine Slip, Investigation Advice, Diet Chart,
Payment Receipt, Doctor Visit Print, Blood Test / Investigation.

## Still to port (bigger feature areas, not print)
Briefing / notice board, Appointments, Follow-up Calendar, User & photo mgmt.
