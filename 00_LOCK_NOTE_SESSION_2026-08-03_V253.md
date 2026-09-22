# V253 LOCK NOTE (B339 → B346)

Base: V252 FINAL (uploaded PILES_CLINIC_APP_V252_FINAL_71.zip).

এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না,
কোনো working flow খারাপ করা যাবে না, অ্যাপ স্লো করা যাবে না।

## এই ডেলিভারিতে যা হয়েছে (খাতায় B339–B346-এ বিস্তারিত)

1. **B339 — Staff Dashboard-এ Draft/Work Notebook:** More মেনু থেকে Draft ও
   Work Notebook বোতাম দুটোই সরিয়ে Dashboard-এ আনা হলো (Draft এমনিতেই
   Dashboard-এ ছিল, শুধু ডুপ্লিকেট মেনু-বোতাম সরানো হলো; Work Notebook
   নতুন `tileWorkNotebook`, শুধু Staff role)।

2. **B340 — Collection বাদ:** Work Notebook-এর WhatsApp-শেয়ার রিপোর্ট
   (Daily + Monthly) থেকে "Collection" লাইন বাদ — TK: গ্রুপে কালেকশনের
   টাকা অন্য কাউকে দেখাতে চান না। হোম-স্ক্রিনের নিজস্ব "Auto from App
   records" কার্ড অপরিবর্তিত (শুধু স্টাফ নিজে দেখেন)।

3. **B341 — অকেজো ঘর বাদ:** Carry-forward/Problem-Help ঘর দুটো — কোডে
   যাচাই করে দেখা গেছে কোথাও ব্যবহারই হতো না — বাদ দেওয়া হলো। Outside
   Calls Today অক্ষত।

4. **B342 — Work Notebook একটাই ফর্ম:** পুরনো আলাদা কার্ড (Check-in/
   Auto-stats/Work-Entries/Outside-Calls/Reports/Home) মিশিয়ে একটাই
   ফর্ম — IN TIME/OUT TIME (অটো বোতাম) → Mark as Leave → New Enquiry/
   Registration/App Calls/Total call (AUTO, ধূসর, অ-এডিটযোগ্য) → Today
   Patient/Outside Calls Today (এডিটযোগ্য) → Notes (একটাই বাক্স) →
   **✔ Submit Report to Master** (Save+Submit+WhatsApp Share একসাথে)।
   Home বাদ। Monthly Report/My Reports মোছা হয়নি, ছোট লিংক করে রাখা হলো।

5. **B343/B344 — WhatsApp ফিক্স:** WhatsApp Business-এ চাপলে কিছু হতো না
   (https://wa.me় লিংক App-Link যাচাই লাগে, Business সাধারণত পায় না) —
   এখন `whatsapp://send` স্কিম (যাচাই লাগে না)। আর WhatsApp/WhatsApp
   Business — দুটোই সবসময় চেক-লিস্টে দেখানো হয় (আগে `isInstalled()`
   ভুলভাবে একটাকে বাদ দিত)। একটাই জায়গায় ফিক্স (`WhatsAppMessageChooser.kt`)
   বলে প্রজেক্টের সব ৮টা জায়গায় (Follow-up/Doctor Visit/Briefing/Draft/
   Search ইত্যাদি) একসাথে প্রয়োগ হয়ে গেছে।

6. **B345 — 🚨 More মেনুর বিশাল-বার বাগ (গুরুত্বপূর্ণ, নিজের-করা ভুল ধরে
   ঠিক করা):** "My Profile"/"Staff Profiles"/"Income & Expense" বোতাম
   আগে সরাসরি More মেনুর ৩-কলাম আইকন-`GridLayout`-এর ভিতরে (কোনো
   GridLayout.LayoutParams ছাড়াই) ঢোকানো হতো — এতে GridLayout-এর সারি/
   কলাম হিসাব ভেঙে বিশাল, স্ক্রিনের-বাইরে-যাওয়া বার তৈরি হতো। এখন এই
   বোতাম তিনটে GridLayout-এর **বাইরে** (তার parent-এ, গ্রিডের ঠিক পরে)
   পূর্ণ-প্রস্থ আলাদা বোতাম হিসেবে বসে — GridLayout-এর ভিতরের ১০টা
   আইকন-বোতাম অক্ষত।

7. **B346 — বাংলা-টেক্সট ফিক্স:** Work Notebook-এ (B342-এর সময়) ভুলবশত
   দুটো বাংলা লাইন ঢুকে গিয়েছিল (guard-এর স্ক্যানার `modules/` ফোল্ডার
   কভার করে না বলে ধরা পড়েনি) — হাতে ধরে ইংরেজি করা হলো।

## অপরিবর্তিত (নিশ্চিত)

- Draft/Work Notebook/Monthly Report/My Reports-এর আসল সেভ/পারমিশন লজিক
  এক অক্ষরও বদলায়নি।
- Staff Profiles/Income & Expense (Master-only মেনু বোতাম) — অপরিবর্তিত,
  শুধু GridLayout-বাগ ফিক্সের সাথে জায়গা বদলেছে (এখনো মেনুতেই)।
- Chamber/Payment/Follow-up/Timeline-এর কোনো লজিক ছোঁয়া হয়নি।
- ওয়েব অ্যাপ (`app.js`) এই ডেলিভারিতে ছোঁয়া হয়নি — সবকটা বদল Android-only।

## ফাইল বদলেছে

`DashboardActivity.kt` · `activity_dashboard.xml` · `MoreMenuActivity.kt` ·
`WorkNotebookActivity.kt` · `ModuleUi.kt` (নতুন `autoValue()`) ·
`WhatsAppMessageChooser.kt` · `build.gradle.kts` (versionCode 252→253,
versionName 2.52→2.53)।

⛔ কোনো SQL লাগেনি এই ডেলিভারিতে।

## যাচাই

ব্র্যাকেট-প্যারেন গোনা (প্রতিটা বদলানো ফাইলে) ✅ পাশ · প্রতিটা নতুন/বদলানো
ফাংশন-কলের আর্গুমেন্ট-সংখ্যা হাতে মিলিয়ে দেখা হয়েছে · `00_GUARD/tk_guard.py
--release` **সব ✅ পাশ**।

🔴 **TK-এর লাইভ টেস্ট বাকি:**
- Staff লগইন করে Dashboard-এ Draft ও Work Notebook বাটন দুটোই দেখা যাচ্ছে
  কিনা, More মেনুতে আর নেই কিনা।
- Work Notebook খুলে IN TIME/OUT TIME/Mark as Leave/Today Patient/Outside
  Calls/Notes ভরে Submit চেপে — WhatsApp শেয়ার-শিট খুলছে কিনা, টেক্সট ঠিক
  ফরম্যাটে (Collection ছাড়া) আছে কিনা, Master-এর Work Report-এ জমা হচ্ছে
  কিনা।
- 💬 (WhatsApp) বোতাম যেকোনো স্ক্রিন থেকে চেপে WhatsApp ও WhatsApp Business
  দুটোই তালিকায় আসছে কিনা, দুটোতেই ঠিক খুলছে কিনা।
- **সবচেয়ে জরুরি:** Master ও Staff/Doctor — দুই role দিয়েই More মেনু খুলে
  সব বোতাম (আইকন-গ্রিড + Staff Profiles/Income & Expense/My Profile) ঠিক
  ছোট আকারে, লেখা-সহ, স্ক্রিনের মধ্যেই দেখাচ্ছে কিনা (B345 বাগ সত্যিই সেরেছে
  কিনা)।

📌 **পরের সেশনের জন্য (TK-এর সিদ্ধান্ত বাকি, খাতার B346-এ বিস্তারিত):**
`IncomeExpenseActivity.kt`-এ ১০টা পুরনো বাংলা লেখা পাওয়া গেছে (Master-only
স্ক্রিন, তাৎক্ষণিক ঝুঁকি নেই) — TK বললে পরের সেশনে ঠিক করা হবে, সাথে
guard-এর স্ক্যানার স্থায়ীভাবে `modules/` ফোল্ডারও কভার করবে।
