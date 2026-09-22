# PILES CLINIC APP — V35 CHANGELOG & PERMANENT LOCK

## Base file
`PILES_CLINIC_APP_V31_GRADLE_FIXED.zip`

## এই Version-এ বাস্তবে পরিবর্তিত কাজ
1. Enquiry status triple-tap popup-এ `Continue` ও `Reject` স্পষ্ট, আলাদা রঙের এবং clickable button করা হয়েছে।
2. Enquiry লেখার নিচের Date সামান্য ডান দিকে নেওয়া হয়েছে।
3. Follow-up/Visit/Patient card-এর Name, Mobile, Branch, Disease ও Next Follow-up অংশের main content সামান্য ডান দিকে নেওয়া হয়েছে।
4. Visit Card-এর `VISITED` badge-এ triple-tap করলে Patient Photo editor খুলবে। Patient ID নিচে আগের মতো থাকবে।
5. Visit Card-এর `Visit Advance` আগের Advance Payment popup-ই খুলবে।
6. Patient Card-এর গোলাকার Payment % চাপলে Advance popup নয়, Patient Payment screen খুলবে—সেখান থেকে 2nd, 3rd, 4th... unlimited payment entry/history ব্যবহার করা যাবে।
7. Prescription-এর Ayurvedic reference list ও Last Selected Dose memory অক্ষত রাখা হয়েছে।
8. Prescription-এ `Add Medicine (Outside List)` dialog যোগ করা হয়েছে—Medicine Name, Dose, When/Frequency, Days, Instruction দিয়ে print list-এ যোগ হবে।
9. Medicine Slip-এ existing Allopathic Medicine List থেকে direct multi-select এবং `Add Outside List` যোগ করা হয়েছে।
10. Clinical Document popup-এর 4টি option (Prescription, Medicine Slip, Blood Test, Diet Chart) এবং তাদের existing direct action অপরিবর্তিত রাখা হয়েছে।

## পরিবর্তিত Source Files
- `FollowUpActivity.kt`
- `PrescriptionActivity.kt`
- `MedicineSlipActivity.kt`
- `activity_prescription.xml`
- `activity_medicine_slip.xml`
- `FILE_MANIFEST_SHA256.json` (package integrity update)

## PERMANENT DO-NOT-CHANGE RULE
পরবর্তী Developer/AI প্রথমে এই নোট পড়বে। User স্পষ্টভাবে না বললে:
- অন্য কোনো UI/Design/Layout বদলানো যাবে না।
- Login, Supabase, Database, Sync, Print Formula বা Business Workflow বদলানো যাবে না।
- Existing working feature remove/rename/rebuild করা যাবে না।
- শুধু User যে নির্দিষ্ট Bug/Update বলবেন, শুধু সেটুকুই patch করতে হবে।
- প্রতিবার নতুন sequential Version name ব্যবহার করতে হবে: V36, V37, V38...
- প্রতিটি returned ZIP-এ নতুন CHANGELOG_LOCK রাখতে হবে।

## Verification performed here
- সমস্ত Android resource XML parse: PASS
- পরিবর্তিত Kotlin files brace/parenthesis structural check: PASS
- Gradle wrapper start: PASS
- Full compile: NOT COMPLETED in this environment because Gradle distribution download requires internet. Android Studio-তে APK build test আবশ্যক।
