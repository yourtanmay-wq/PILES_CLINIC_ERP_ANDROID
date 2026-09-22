# 🔒 LOCK NOTE — V135 (2026-07-26, Audit Fix Release)

**Base:** V134 · **নতুন:** V135 · **ZIP:** `PILES_CLINIC_APP_V135_FINAL.zip`
**versionCode / versionName:** 134 → **135 / V135**

> এই রিলিজে **শুধুমাত্র চারটি নিশ্চিত ফিক্স** আছে (TK-এর নির্দেশ, বাইরের Audit রিপোর্টের ভিত্তিতে)।
> অন্য কোনো কোড, ডিজাইন, ওয়ার্কফ্লো, পারমিশন, ডেটাবেস, SQL, ছবি, Sync, Delete, Patient ID বা RLS **স্পর্শ করা হয়নি**।

---

## ১) V134 → V135 — যে ফাইলগুলো বদলেছে (মোট ৫টি, নতুন ফাইল নেই, মোছা ফাইল নেই)

| # | ফাইল | কেন |
|---|---|---|
| 1 | `app/build.gradle.kts` | ভার্সন 134 → 135 |
| 2 | `native/FollowUpActivity.kt` | ফিক্স ১ — মোবাইল বদলে শুধু নির্দিষ্ট Patient ID-র সারি |
| 3 | `native/MobileChangeSync.kt` | ফিক্স ২ — `doctor_visits` সম্পূর্ণ বাদ |
| 4 | `native/PendingSyncStatus.kt` | ফিক্স ৩ — `closeQueue` গোনা |
| 5 | `native/DashboardActivity.kt` | ফিক্স ৪ — `v-NEW5` → `V135` |

---

## ২) Before / After (হুবহু কোড)

### ফিক্স ১ — মোবাইল বদল (`FollowUpActivity.kt`)
**আগে (V134) — বিপজ্জনক:**
```kotlin
val rows = SupabaseClient.findByMobile(table, "+91$oldMobile", "id", 50)
for (i in 0 until rows.length()) {
    val id = rows.getJSONObject(i).optString("id")
    if (id.isBlank()) continue
    SupabaseClient.updateById(table, id, patientFields)   // ← পুরনো নম্বরের প্রতিটি সারিতে
```
এতে `patientFields`-এ থাকে **name, branch, disease, mobile, age, sex, address** — অর্থাৎ এক নম্বরে পরিবারের অন্য সদস্যের রেকর্ডেও এই রোগীর তথ্য বসে যেত।

**এখন (V135) — শুধু Patient ID মিললে:**
```kotlin
val myPid = item.patientId.trim().uppercase()
if (myPid.isBlank()) {
    linkedUpdateSkipped = true                    // অনুমান করে কিছু বদলানো হয় না
} else {
    val rows = SupabaseClient.findByMobile(table, "+91$oldMobile", "id,patientId", 50)
    var touched = 0
    for (i in 0 until rows.length()) {
        val row = rows.getJSONObject(i)
        val id = row.optString("id")
        if (id.isBlank()) continue
        if (row.optString("patientId", "").trim().uppercase() != myPid) continue   // ← একমাত্র শর্ত
        touched++
        SupabaseClient.updateById(table, id, patientFields)
        ...
    }
    if (touched == 0) linkedUpdateSkipped = true
}
```
এবং না মিললে ব্যবহারকারীকে জানানো হয়:
```kotlin
if (linkedUpdateSkipped) {
    runOnUiThread {
        Toast.makeText(this@FollowUpActivity,
            "Saved. Linked Patient/Enquiry records were NOT changed (no matching Patient ID) - please check them.",
            Toast.LENGTH_LONG).show()
    }
}
```

### ফিক্স ২ — `MobileChangeSync.kt`
```diff
- private val TABLES = listOf("payments", "doctor_visits")
+ private val TABLES = listOf("payments")
```
কারণ: `doctor_visits` হলো ডাক্তার/RMP ডিরেক্টরি; ওখানকার `mobile` ডাক্তারের নম্বর, আর `patientId` কলামই নেই।

### ফিক্স ৩ — `PendingSyncStatus.kt`
```kotlin
// Registration prefs-এর দ্বিতীয় তালিকা "closeQueue" (রেজিস্ট্রেশনের পর
// পুরনো Enquiry বন্ধ করা) গোনা হতো না → "Synced" দেখাত অথচ আটকে থাকত।
val raw = context.getSharedPreferences("piles_clinic_registration_pending", Context.MODE_PRIVATE)
    .getString("closeQueue", "[]") ?: "[]"
val n = org.json.JSONArray(raw).length()
if (n > 0) { total += n; perLabel["এনকোয়ারি বন্ধ"] = n }
```

### ফিক্স ৪ — `DashboardActivity.kt`
```diff
- "⏳ ${s.total} to sync · v-NEW5"  /  "☁️ Synced · v-NEW5"
+ "⏳ ${s.total} to sync · V135"    /  "☁️ Synced · V135"
```

---

## ৩) প্রমাণ — নাম মিলিয়ে আপডেট সম্পূর্ণ সরানো হয়েছে

যন্ত্রে খোঁজা হয়েছে (`FollowUpActivity.kt`):

| খোঁজা হয়েছে | ফল |
|---|---|
| `myName` | **নেই** |
| `rowName` | **নেই** |
| `samePerson` | **নেই** |
| `row.optString("patientId", "").trim().uppercase() != myPid` (একমাত্র শর্ত) | **আছে** |

অর্থাৎ patients/enquiries-এ কোনো সারি নাম মিলিয়ে বা অনুমান করে আর বদলানো হয় না।
Payment-এর সিঙ্কেও (`MobileChangeSync`) নিয়ম অপরিবর্তিত: এক নম্বরে একাধিক Patient ID থাকলে শুধু ওই রোগীর নিজের সারি সরে।

---

## ৪) Static Check ফল (V135)

| পরীক্ষা | ফল |
|---|---|
| ১৫০টি Kotlin ফাইলে brace/paren/bracket | ✅ সব সমান |
| ২০৫টি XML well-formed | ✅ সব ঠিক |
| কোডের `R.id.x` লেআউটে আছে কিনা | ✅ একটাও নিখোঁজ নয় |
| versionCode / versionName | ✅ 135 / V135 |
| ওয়েব `node --check app.js` | ✅ পাশ |

---

## ৫) ⚠️ NOT TESTED (সৎ ঘোষণা)

- **Android Studio Build — NOT TESTED** (এই পরিবেশে Gradle/ইন্টারনেট নেই)
- Kotlin compile · APK install · অ্যাপ চালানো — **NOT TESTED**
- একাধিক ফোনে Live Sync · Offline→Online · Live Supabase টেবিল/RLS — **NOT TESTED**
- ZIP-এ কোনো APK/AAB নেই

উপরের সব যাচাই **Static (কোড-স্তরের)**। রানটাইম পরীক্ষা TK-কেই করতে হবে।

---

## ৬) ঘোষণা

**এই রিলিজে উপরের চারটি ফিক্স ছাড়া অন্য কোনো অনুমোদিত ডিজাইন, ওয়ার্কফ্লো, পারমিশন বা ডেটা-নিয়ম পরিবর্তিত হয়নি।**
V134-এ লক করা সবকিছু (Remark বক্স, Payment-এর চকলেট বোতাম, চেম্বারের ব্রাঞ্চ-বাছাই পপ-আপ, সতর্কবাতি, ওয়েব অ্যাপ) হুবহু অপরিবর্তিত আছে।
Database, SQL, RLS, ছবি সংরক্ষণ, Delete-guard, Patient ID তৈরির নিয়ম — **কিছুই ছোঁয়া হয়নি**।

## ৭) যেগুলো ইচ্ছাকৃতভাবে এই রিলিজে করা হয়নি (TK-এর নির্দেশে পরে আলাদা করে হবে)

1. Delete করা রেকর্ড **অন্য ফোন** থেকে ফিরে আসা (ক্লাউড-স্তরের tombstone দরকার)
2. ক্লাউডে পৌঁছানোর আগেই "সেভ হয়েছে" দেখানো
3. দুই ফোনে একসাথে রেজিস্ট্রেশনে Duplicate Patient ID (ডেটাবেসে unique নিয়ম দরকার)
4. ছবি base64-এ বারবার জমা হওয়া (Supabase Storage দরকার)
5. RLS বন্ধ থাকার নিরাপত্তা ঝুঁকি
6. সময়ে ভুল `Z` (টাইমজোন)
7. Trash/Delete সম্পূর্ণ transaction-safe নয়

---

**🔒 LOCK NOTE:** এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না, অ্যাপ স্লো করা যাবে না।
