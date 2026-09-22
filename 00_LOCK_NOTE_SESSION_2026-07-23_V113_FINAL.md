# Session Lock Note — → V113 (2026-07-23)

এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।

**⚠️ TK-এর স্থায়ী নিয়ম:** TK শুধু ফটো-প্রুফ দেখে ডিজাইন ফাইনাল করেন, টেকনিক্যাল/কোড যাচাইয়ের সম্পূর্ণ দায় Claude-এর। ছবিতে যা দেখে "ঠিক আছে" বলেন, Android Studio-তে build করার পরেও ঠিক তাই দেখাতে/কাজ করতে হবে।

## V112 base ধরে ঠিক ৩টি কাজ (আর কিছু ছোঁয়া হয়নি)

**১. Version bump → V113**
`app/build.gradle.kts`: versionCode 93 → 113, versionName "V93" → "V113"।

**২. পাসওয়ার্ড নিরাপত্তা**
আগে: cloud পাসওয়ার্ড আনতে ব্যর্থ হলে (নেট সমস্যা) বা custom না থাকলে — দুটোই null ধরে পুরোনো default পাসওয়ার্ড গ্রহণ করত। এমনকি custom সেট থাকলেও default দিয়ে ঢোকা যেত।
এখন ৩টি আলাদা অবস্থা (`CloudPasswordCheck.PasswordState`):
- **HasCustom** → শুধু সেই custom পাসওয়ার্ডই কাজ করবে (default আর গ্রহণ হবে না)।
- **NoCustom** → server নিশ্চিত করেছে custom নেই → তখনই শুধু default কাজ করবে।
- **Failed** → server-এ পৌঁছানো যায়নি → "Network problem" বার্তা, default দিয়ে ঢুকতে দেওয়া হবে না।
পুরোনো `fetchOverridePassword()` অপরিবর্তিত (অন্য কিছু ভাঙেনি); নতুন `fetchOverridePasswordState()` যোগ করে LoginActivity সেটা ব্যবহার করে।

**৩. Enquiry Follow-up Card-এ Time Type badge**
- `EnquiryModel.buildFollowUpRow`: followups রেকর্ডেও `timeType` লেখা হয় (enquiry থেকে কপি)।
- `FollowUpModel`: `timeType` ফিল্ড যোগ + parse-এ পড়া।
- `item_followup_card.xml`: Enquiry কলামে ছোট `tvTimeType` ব্যাজ (default gone)।
- `FollowUpAdapter`: Enquiry কার্ডে সবুজ "🕘 Official Time" / বেগুনি "🌙 Unexpected Time"; timeType ফাঁকা হলে লুকানো; Visit/Treatment কার্ডে recycling-safe ভাবে লুকানো।
- Migration: `04_SUPABASE_DATABASE_SETUP/PATCH_2026-07-23_followups_timeType.sql` — শুধু `add column if not exists "timeType"` + পুরোনো followups-এ linked enquiry থেকে safe backfill। **কোনো drop/rename/delete নেই।**

## কঠোর নিয়ম যা মানা হয়েছে
- Branch filter অপরিবর্তিত। কোনো redesign নেই (শুধু ছোট time badge)। Working flow / permission / login design / dashboard / patient card / payment / prescription / medicine slip / blood test / diet chart / RMP — কিছু বদলায়নি। Doctor ও Field Officer-এর staff-like permission অক্ষত। Auto-logout চালু করা হয়নি। পুরোনো ZIP / Web / .git / extra files মোছা হয়নি।

## যাচাই (গভীরভাবে, ফাইল দেওয়ার আগে)
- ৫টা Kotlin ফাইল: brace/paren balanced, ব্লক-কমেন্ট `/* */` জোড়া মিলছে।
- XML well-formed, "--" নেই, tvTimeType id ↔ adapter মিলছে।
- `.s()` হেল্পার আছে (EnquiryModel-এ ব্যবহৃত)।
- SQL-এ কোনো drop/rename/delete নেই (শুধু কমেন্টে উল্লেখ যে এগুলো করা হয় না)।
- V112 base-এর সাথে তুলনা: শুধু এই ৩ কাজের ফাইলই বদলেছে, স্কোপের বাইরে কিছু না।

**পরিবর্তিত ফাইল:** build.gradle.kts, CloudPasswordCheck.kt, LoginActivity.kt, EnquiryModel.kt, FollowUpModel.kt, FollowUpAdapter.kt, item_followup_card.xml + নতুন SQL patch।
