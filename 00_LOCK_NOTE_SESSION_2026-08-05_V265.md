# V265 LOCK NOTE (চূড়ান্ত — B469, বিল্ড-ভাঙা ভুল ঠিক করা)

Base: V264 FINAL (uploaded PILES_CLINIC_APP_V264_FINAL.zip) — যেটা TK-এর
Android Studio বিল্ডে ব্যর্থ হয়েছিল।

🔒 এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো
যাবে না, কোনো working flow খারাপ করা যাবে না।

## এই ডেলিভারিতে যা হয়েছে (B469)

**সমস্যা:** V264 Android Studio-তে বিল্ড ব্যর্থ — `WorkNotebookActivity.kt`-এ
২টা কম্পাইল এরর ("References to variables and parameters are unsupported")।

**আসল কারণ:** `askCheckOutReason()`-এ একটা লোকাল `lateinit var dlg` বসিয়ে
`::dlg.isInitialized` দিয়ে চেক করা হয়েছিল — Kotlin-এ `::name` (প্রপার্টি-
রেফারেন্স) শুধু ক্লাস-লেভেল/মেম্বার lateinit-এর জন্য কাজ করে, লোকাল
ভ্যারিয়েবলের জন্য না।

**সমাধান:** `lateinit var dlg` → `var dlg: AlertDialog? = null` (nullable),
`::dlg.isInitialized` → সহজ `dlg?.dismiss()`।

⛔ বাকি কোনো লজিক বদলায়নি — শুধু এই একটা প্যাটার্ন। প্রজেক্টের বাকি সব
জায়গায় খুঁজে দেখা হয়েছে — একই ধরনের ভুল (লোকাল ভ্যারিয়েবলে `::` রেফারেন্স)
আর কোথাও নেই।

## 🔴 TK-কে করতে হবে

1. `04_SUPABASE_DATABASE_SETUP/V264_DIALER_CALLS_2026-08-05.sql` Supabase-এ
   RUN (যদি আগে না করে থাকেন)।
2. V265 আবার বিল্ড করে দেখা — এবার এরর আসার কথা না।
3. বাকি সব লাইভ টেস্ট (V264 LOCK NOTE-এ তালিকা দেখুন)।
