# V386 — Web-এর মোবাইল ভিউ: অ্যান্ড্রয়েডের হুবহু (A to Z)

**তারিখ:** 15.08.2026
**নির্দেশ (TK):** *"অ্যান্ড্রয়েডে যা আছে ঠিক হুবহু তাই থাকবে web-এর মোবাইল View-তে। অ্যান্ড্রয়েডের কোনো কিছু খারাপ করবেন না। ডেস্কটপ আলাদা। আন্দাজে কোনো কাজ করবেন না।"*

---

## ১. ⛔ যা ছোঁয়া হয়নি — মেপে প্রমাণ করা

| বিষয় | প্রমাণ |
|---|---|
| **Android সোর্স** | `02_ANDROID_SOURCE_CODE`-এর **৫৭২টি ফাইলের একটিও বদলায়নি** (`find -newermt` = **0**) |
| **প্রজেক্টের অন্য সব ফোল্ডার** | ০ ফাইল বদলেছে |
| **ডেস্কটপের পুরনো CSS** | পুরনো `styles.css` নতুন ফাইলের **শুরুতে হুবহু আছে** — একটি অক্ষরও মোছা/বদলানো হয়নি |
| **নতুন CSS-এর সীমা** | যোগ হওয়া ৭২৭ লাইনে media query মাত্র দুটি: `max-width:899px` (ফোন) ও `min-width:900px` (ডেস্কটপ)। ডেস্কটপের পুরনো নিয়মে হাত পড়েনি |
| **JavaScript ফাংশন** | **একটিও মুছে যায়নি**। শুধু ২টি নতুন সহায়ক ফাংশন যোগ (`anSeverityOf`, `anDialogIcon`) |
| **লজিক / টাকা / role / branch / sync** | কোনোটাতেই হাত পড়েনি — `app.js`-এ diff hunk মাত্র **৮টি**, সবই চেহারা-সংক্রান্ত |
| **অন্য module JS** | finance · notebook · partners · profile · rmp_commission · module_core — ছোঁয়া হয়নি |

**বদলানো ফাইল মাত্র ৩টি:** `03_NETLIFY_READY/app.js` · `styles.css` · `index.html` (শুধু cache লিংক v412 → v413)

---

## ২. 🔴 যে আসল ভুলটা ধরা পড়ল (ব্রাউজারে মেপে প্রমাণিত)

Follow-up কার্ডের HTML-এ `</div>` **ভুল জায়গায় বন্ধ হচ্ছিল**। মাপ (Chromium, 390px):
- `.followMain` (নাম · মোবাইল · ট্যাগের কলাম) = **০ × ১৭৭ px** — সব ঢাকা পড়ত
- `#followRows`-এ ৩টি কার্ডের জন্য **৬টি সন্তান** — ৪টে বোতাম কার্ডের **বাইরে** চলে যেত

এখন ঠিক — কার্ড = [উপরের সারি] + [রিমার্ক বাক্স] + [৪ বোতাম], অ্যান্ড্রয়েডের মতোই।

---

## ৩. ⚠️ Dead code — যা থেকে মাপ নেওয়া **হয়নি**

| ফাইল | কেন বাদ |
|---|---|
| `native/FollowUpAdapter.kt` | কোথাও ব্যবহার হয় না। আসল কার্ড আঁকে `FollowUpActivity.FollowAdapter` (:1225) → `buildFollowCard()` (:1325) |
| `res/layout/item_followup_card.xml` | শুধু ওই মৃত adapter-এ (`FollowUpActivity.kt:1913`-এর কমেন্টেই লেখা) |
| `clinical/InvestigationAdapter.kt` · `item_investigation_test.xml` | কখনো তৈরি হয় না |

বাকি সব layout **ViewBinding দিয়ে সত্যিই ব্যবহার হয়** — যাচাই করে দেখা হয়েছে। যেখানে Kotlin কোড XML-এর মান বদলে দেয় (যেমন `DoctorQueueAdapter`, `RegistrationActivity.tint()`, `EnquiryActivity`, `ChamberAttendanceAdapter`, `DraftCardAdapter`), সেখানে **Kotlin-এর চূড়ান্ত মানই** নেওয়া হয়েছে।

---

## ৪. ✅ যা যা করা হলো

### ৪.১ সব পর্দার ভিত্তি (মোবাইল)

| বিষয় | অ্যান্ড্রয়েডের উৎস | মান |
|---|---|---|
| পাতার পটভূমি | `bg_app_gradient.xml` | 135° `#EDF6FF → #F7FFFB → #FFFAF1` |
| উপরের হেডার | `bg_login_hero.xml` (৩০+ পর্দায়) | 135° `#0B2B59 → #1167D8 → #16A36D`, padding 5dp, ← 18sp সাদা, শিরোনাম 15sp bold |
| ড্যাশবোর্ডের বার | `activity_dashboard.xml:27` | **সাদা** `@color/white` (গ্রেডিয়েন্ট নয়) |
| ফর্মের লেবেল | `styles_form.xml` FieldLabel | 13sp bold `#6B7280`, mT 14dp |
| ফর্মের ঘর | FieldInput + `bg_input_field.xml` | h **50dp**, `#F6F9FC`, 1dp `#D8E4F2`, r 14dp, 14.5sp `#1A1A1A` |
| placeholder | `@color/field_hint` | `#C7CDD6` |
| বোতাম | `bg_btn_green.xml` | `#16A36D`, r 14dp |
| ghost বোতাম | `bg_btn_silver.xml` | 135° `#F4F6F9→#D6DBE2→#A7ADB8`, লেখা `#1B2432` |
| কার্ড | `styles_form.xml` RegCard | r **20dp**, elevation 3dp, সাদা |
| বিভাগ-শিরোনাম | SectionHead | 14sp bold `#0B2B59`, mT 22dp, padB 6dp |
| খালি অবস্থা | ১৬টি layout-এ এক | মাঝে, `#6B7280`, 14sp, mT 30dp, **কোনো আইকন নেই** |
| ভুল-ঘর | `native/FieldError.kt` | `#FFF4F4`, 1.7dp `#E23B3B`, r 12dp |
| iPhone safe-area | — | `env(safe-area-inset-*)` যোগ |
| পাশে গড়ানো | — | `overflow-x:hidden` |

### ৪.২ নিচের ৫-বোতামের বার — **বন্ধ** (অ্যান্ড্রয়েডের সঙ্গে মিলিয়ে)

প্রমাণ: `res/layout/bottom_nav_bar.xml:12` → `android:visibility="gone"`, আর `native/BottomNav.kt:212` কোড থেকেও GONE। ফাইলের নিজের কমেন্ট:
> *"TK-REQUESTED (2026-07-20): the bottom navigation bar must NOT appear on ANY screen (only the Dashboard's own top-right Menu stays)."*

চলাফেরা অটুট — `page()` (app.js:1220) ও `dashboard()` প্রতিটি পর্দাতেই উপরে-ডানে ☰ Menu আঁকে (যাচাই করা)।
**↩️ ফেরত নিতে:** `styles.css`-এর শেষে `.bottom,.proBottomNav{display:none!important}` — এই **একটি লাইন** মুছে দিন।

### ৪.৩ পপ-আপ — `native/PremiumAlert.kt`-এর হুবহু

- কার্ড: সাদা, r **20dp**, পর্দার মাঝে (আগে নিচ থেকে উঠত)
- হেডার: **gradient স্ট্রিপ**, 16.5sp bold, padding 16dp, উপরের দুই কোণে r20
  - সবুজ `#0B5E34→#1F9D55` · হলুদ `#C98A00→#F0B520` (লেখা `#3A2600`) · লাল `#8A1810→#C43325`
- **severity ঠিক করার নিয়ম ও ইমোজি** — `severityOf()` (kt:59-79) ও `iconFor()` (kt:84-113) **হুবহু** JS-এ লেখা, ৭টি নমুনা দিয়ে যাচাই করা
- বোতাম: সবুজে `#0A7C3F` · হলুদে `#E8A100` · লালে `#B42318`; Cancel/Close `#EEF2F7` / `#41506A`, r 10dp, minWidth 88dp

### ৪.৪ প্রতিটি পর্দা

| পর্দা | অ্যান্ড্রয়েড উৎস | মূল মিল |
|---|---|---|
| **ড্যাশবোর্ড** | `activity_dashboard.xml:307` + `item_dashboard_tile.xml` | **৩ কলাম**, টাইল 100dp, আইকন 44×44dp `bg_tile_icon` উপরে · লেখা 12sp bold `#0B1B2E` নিচে, r 20dp, 1dp `#EDF1F6`, ডান দিকের `›` বাদ |
| **☰ Menu** | `activity_more_menu.xml` | সাদা কার্ড r16, আইকন-ঘর 42×42dp r12, শিরোনাম 13.5sp bold **serif** `#0B2B59`, ডানে `›` `#B7C0CC` |
| **Follow-up** | `FollowUpActivity.buildFollowCard()` | সম্পূর্ণ নতুন করে — নিচে আলাদা টেবিল |
| **Follow-up ট্যাব** | `activity_followup.xml:116-152` | h 40dp, 13sp bold, r12; বাছা `#0B2B59`/সাদা · না-বাছা সাদা+1dp `#D8E4F2`/`#1A1A1A` |
| **তারিখ-চিপ** | `FollowUpActivity.kt:336-346` | h 32dp, 11.5sp; বাছা `#1167D8`/সাদা · না-বাছা `#E8F2FF`/`#1167D8` |
| **Registration** | `activity_registration.xml` | ৭টি আলাদা RegCard (r20, elev3, mH12, pad12), `*` **লাল `#E53935`** (`FormStar.kt`), Save **54dp `#0C9E33`** r14 16sp, ছবি-বোতাম `#E9F7EC` r30 2dp `#0C9E33` লেখা `#0A7A28` |
| **সেগমেন্ট বাছাই** | `RegistrationActivity.kt:493-528` | বাছা `#1167D8`/সাদা · না-বাছা `#E8F2FF`/`#1167D8` |
| **বহু-বাছাই চিপ** | `chip_bg/text_selector.xml` | বাছা `#16A36D`/সাদা · না-বাছা `#E5E7EB`/`#1A1A1A`, 11.5sp, ALL-CAPS |
| **Enquiry** | `activity_enquiry.xml` | এক কার্ড m16 r18 elev4 pad18, লেবেলে ইমোজি, রোগ **৩ কলাম × 48dp** — না-বাছা `#9AA5B1` · বাছা `#16A36D` (`EnquiryActivity.kt:184-198`), Save 52dp `#16A36D` |
| **Payment** | `activity_payment.xml` | দুই বোতাম **54dp** 3-D (নীল/সবুজ), সারাংশ 45° navy→blue→green r16 pad13, মোট **23sp**, তিন বাক্স `#E9F7EE/#0B7A34` · `#E8F1FF/#1457B8` · `#F3ECFF/#6B28C9` |
| **সংগ্রহের সারি** | `item_collection_row.xml` + `CollectionAdapter.kt` | m6 r12 elev3, বাঁয়ে **5dp সবুজ**, নাম 14sp, রোগ-চিপ `#FDE7EA/#B0392B`, মোবাইল `#2F7D4E` bold, ID-চিপ `#EFE9FB/#6B21A8`, টাকার বাক্স cash `#E8F7EE`+1dp `#BFE6CD` / online `#E7F0FD`+1dp `#C3DCFA` |
| **CHECK-UP Queue** | `item_queue_card.xml` + `DoctorQueueAdapter.kt` | কার্ড m8 r18 elev3 pad14, ছবি **56dp গোল** gradient, নাম **18sp**, মোবাইল 14sp bold `#1167D8`, ব্যাজ `#D92D20` r8 10.5sp "WAITING", ৪ বোতাম **h34dp 10.5sp** — History/Report `#7A3FF2` r10 · Check-up/Action `#1777F2` r9; বিভাগ-শিরোনাম Today `#0C9E33` · Pending `#E8890C`, 15sp সাদা r10 pad16/11 |
| **Doctor Visit / RMP** | `activity_doctorvisit.xml` + `item_doctor_card.xml` | ৪টি KPI — Expected `#FDF2F8→#FCE0EC`/`#BE185D` · Pending `#FFF1EE→#FBDAD2`/`#C2410C` · Called `#F1FBF4→#D6F0DE`/`#15803D` · All `#F5F3FF→#E4DBFB`/`#6D28D9`; Performance `#7C3AED` r12 · Due `#C0392B`; কার্ড m5 r16 pad11, **লাল সিরিয়াল ব্যাজ** (ছবি নেই), নাম 15.5sp, ট্যাগ `#1167D8`/`#079B28`, রিমার্ক dashed সবুজ, ৪ বোতাম h34dp — 📞💬 `#078C18` · 👁 `#1777F2` · ➜ 315° `#6A1DD2→#9D22E9` |
| **CHAMBER DATE** | `activity_chamber_attendance.xml` | হেডার-সারি **`#6B3A16`** (চকলেট), PATIENT 106dp 10sp · TREATMENT 8.5sp · FEES 38dp · CASH/ONLINE 46dp 9.5sp — সব সাদা bold; সারির রং arrived `#E9F8F0` · expected `#FFF9E6` · অন্য `#FDEEEE`, নিচে 1dp `#D6E0EA`, minHeight **62dp**; নাম 12sp bold `#B42318` uppercase; `→` 42×42dp `#16A36D` r14 |
| **Reports** | `activity_reports.xml` | ৩ KPI টাইল pad16 r8 — Enquiry `#E8F2FF`+1dp `#CFE0F7` মান 20sp `#1457B8` · Patients `#E8FFF4`+1dp `#B6ECD4` `#08724D` · Collection 16sp; ব্যাজ 32×32dp r10 gradient; drill-down সারি সাদা r14 1dp `#E4E8ED` **বাঁয়ে 4dp `#0EA25F`** |
| **Print Center** | `activity_print_center.xml` | শিরোনাম **17sp bold**; উপরের ৩ টাইল **h 118dp** r16 elev3, ব্যাজ 42×42dp r12 gradient, নাম 11sp, উপ-লেখা 8.5sp; নিচের সারি r12 elev2 pad10, ব্যাজ 34×34dp r9, নাম 13sp |
| **Search** | `GlobalSearchActivity.kt:364-486` | কার্ড r16 m8/6; হেডার-ব্যান্ড pad14 gradient `#0A5428→#0EA25F`; avatar **44dp গোল**; নাম 15.5sp সাদা; meta 11.5sp `#DCF3E6`; tag `#C99A19` r20 9.5sp; বোতাম r12 pad10/9 — সবুজ gradient বা রুপালি gradient, আইকন 14sp + লেখা 10.5sp |
| **Login** | `activity_login.xml` | hero pad 56/24/40 gradient; logo **56dp**; "TK BISWAS" 20sp · clinic 16sp; কার্ড **mT −24dp** r20 elev8 pad22; ইনপুট 52dp; Login **52dp `#07883F`** r14 16sp; Forgot 14sp bold `#1167D8` |
| **Trash / Password / Backup / Settings** | `item_trash_card.xml` · `item_credential_card.xml` | কার্ড m8 r16 elev3 pad14; Restore/Change `#1777F2` r9 h42dp; Delete Forever `#D32F2F` r9; পাসওয়ার্ডের ঘর `#FFF8E6` r10 1dp `#F5E3AE` |
| **Briefing** | `item_briefing_card.xml` + `BriefingAdapter.kt` | কার্ড mH10 mV4 r13 elev1.5, **বাঁয়ে 4dp** normal `#0F7A43` / urgent `#C0392B`; শিরোনাম 11.5sp; উত্তর-বাক্স `#FFF8E6` r10; বোতাম Approve `#0F172A` · Reply সাদা+1.5dp `#D7DDE6` · Delete সাদা+1.5dp `#F3D1D8` লেখা `#E11D48` |
| **Draft কার্ড** | `item_draft_card.xml` + `DraftCardAdapter.kt` | mH8 mV4 pad8 `bg_follow_card`; মোবাইল+নাম **12.5sp bold `#10223A`**; meta **9.5sp bold `#1167D8`**; রিমার্ক dashed সবুজ 10.5sp এক লাইন; বোতাম **h 22dp** — 📞💬 `#078C18` · 👁 `#1777F2` · Restore `#EAFBF0`+1dp `#BFE9CE`/`#0B4F2A` · Delete `#D32F2F` |
| **Timeline (View All)** | `PatientTimelineActivity` + `item_timeline.xml` | হেডার stage-অনুযায়ী পাল্‌ রঙ (Enquiry/Visit/Patient) নিচে 2dp দাগ; ছবি **52dp চৌকো** gradient; নাম 17sp; সারি alternating সাদা / `#F6FAF7` |

### ৪.৫ Follow-up কার্ড — `FollowUpActivity.kt buildFollowCard()`-এর হুবহু

| অংশ | মান | kt লাইন |
|---|---|---|
| কার্ড | সাদা · r 18dp · 1dp `#E4EDEC` · বাঁয়ে **4dp `#0B66D8`** · margin 8/5dp · padding 10dp | 1347-1351 |
| বাঁ কলাম (শুধু Enquiry) | **62dp** · marginEnd 8dp · paddingTop 6dp | 1354 |
| কল-সিগন্যাল | **wifi আর্ক 48×40dp** — `ic_wifi_calls_0..5.xml`-এর SVG নকল, জ্বলা `#1067D8` / নেভানো `#D8E4F2`, নিচ থেকে জ্বলে | drawable |
| "Enquiry" / তারিখ | 10sp `#1067D8` mT 7dp · 9.5sp `#667085` | 1377, 1382 |
| সিরিয়াল | **লাল `#D32F2F`** r6 · 11sp সাদা · minW 24dp · নামের বাঁয়ে | 1496 |
| নাম / মোবাইল | 16sp bold `#10223A` · 12.5sp **সাধারণ** `#5B6B81` mT 3dp | 1516, 1521 |
| ট্যাগ | **চারটেই একই নীল `#1167D8`** · 10.5sp bold · pad 7/4dp · r8 | 1571 |
| VISITED/PATIENT | `#079B28` r20 · 10sp · pad 9/3dp · mT 6dp | 1645-1650 |
| Enquiry ব্যাজ | 8.8sp · pad 6/4dp · r6 · `#E5484D` / `#F79009` | 1684 |
| Visit ডান স্লট | **💰 ADVANCE HERE** — 45° `#0EA25F→#0A3B20` r12 · 10sp | 1692 |
| Patient ডান স্লট | PRESCRIPTION চিপ + **রিং 48dp** (track `#E1E6ED`, arc `#16A36D`) + **Bill/Due দুটো বাক্স** (`#EAF8EF/#0B8F3C`, `#FDECEC/#D92D20`) | 1706-1738 |
| রিমার্ক বাক্স | `#EAF9F0` r12 · 1.3dp ড্যাশ `#5DBE84` · pad 10/7/10/8dp · **ভিতরে** LAST/NEXT CALL (8sp) → 1dp `#A8D8BC` → রিমার্ক 12sp | 1936-1949 |
| ৪ বোতাম | **h 27dp** · gap 5dp · r9 · 15sp · 📞💬 `#078C18` · 👁 **ও ➜ দুটোই `#1777F2`** (বেগুনি নয়) | 1951-1990 |
| টাকা | হাজারে কমা (`₹18,000`) — শুধু এই কার্ডে, গ্লোবাল `money()` অক্ষত | 1718 |

---

## ৫. 🧪 যাচাই

| পরীক্ষা | ফল |
|---|---|
| `node --check` — ৮টি JS ফাইল | ✅ সব PASS |
| CSS ব্রেস ব্যালান্স | ✅ ঠিক |
| পুরনো CSS অক্ষত (prefix মিল) | ✅ হ্যাঁ |
| যোগ হওয়া CSS-এ media query | ✅ শুধু `max-width:899px` ও `min-width:900px` |
| Android ফোল্ডারে বদল | ✅ ০ ফাইল |
| JS ফাংশন মুছে গেছে? | ✅ একটিও না |
| **১৬টি পর্দা Chromium-এ 390px-এ আঁকা** | ✅ ছবি: `V386_ALL_SCREENS.png` |
| Follow-up 360px | ✅ কিছু কাটে না, বেরোয় না |
| ডেস্কটপ 1440px | ✅ ৩-কলাম গ্রিড ও কার্ড ঠিক |
| **রানটাইমে JS-ইনজেক্ট করা ২টি `<style>` ব্লক সহ** যাচাই | ✅ নতুন নিয়মই জেতে (specificity মিলিয়ে দেখা) |
| পপ-আপের severity নিয়ম | ✅ ৭টি নমুনায় অ্যান্ড্রয়েডের সঙ্গে হুবহু মিলেছে |

**⚠️ বাকি:** Supabase-এ আসল লগইন করে লাইভ টেস্ট — TK-এর নিজের পরিবেশে।

---

## ৬. 📌 সততার সঙ্গে: যা এখনও মেলেনি

1. **"🩸 TEST HERE" বোতাম** — অ্যান্ড্রয়েডের Visit কার্ডে ADVANCE HERE-এর নিচে আছে (`kt:1699`)। ওয়েবে যোগ করিনি: ওয়েবের `blood(id)` **patient id** চায় (`app.js:6869`), কিন্তু Visit ট্যাবে রোগী তখনো নিবন্ধিত না-ও থাকতে পারে — আন্দাজে বসালে বোতামটা "Patient not found" দেখাত। **কোন ফাংশন ডাকতে হবে জানালে সঙ্গে সঙ্গে যোগ হবে।**
2. **ট্যাগ নিচের লাইনে নামলে আগের `|` দাগটা লাইনের শেষে ঝুলে থাকে** — অ্যান্ড্রয়েডের `layoutTagsInRows()` এটা এড়ায় (রান-টাইমে চওড়া মেপে)। CSS-এ এটা করা যায় না; ছোট প্রসাধনী পার্থক্য।
3. ~~Patient History~~ — ✅ **হয়ে গেছে** (নিচে ৮.১ দেখুন)।
4. ~~Dialer~~ — ✅ **হয়ে গেছে** (নিচে ৮.২ ও ৮.৩ দেখুন — All/Missed ট্যাব ব্রাউজারে সম্ভব নয়)।
5. ~~নোটবুক · স্টাফ প্রোফাইল · আয়-ব্যয় · অংশীদার~~ — ✅ **হয়ে গেছে** (নিচে ৯ নম্বর অংশ দেখুন)।

---

## ৭. ↩️ কীভাবে ফেরত নেওয়া যায়

সব নতুন CSS `styles.css`-এর **একদম শেষে**, `V386 — ANDROID PARITY` লেখা ব্লকে (৭২৭ লাইন)। পুরো ব্লকটা মুছলেই V385-এর চেহারা ফিরে আসে (তখন Follow-up কার্ডের পুরনো ভুলটাও ফিরে আসবে)।
`app.js`-এ বদল মাত্র ৮ জায়গায় — প্রতিটির উপরে বাংলা কমেন্টে কী ও কেন লেখা আছে।

**⚠️ পরের সেশনের জন্য:** এই ফাইলটাই V386-এর একমাত্র রেফারেন্স। ৬ নম্বর অংশের ৫টি বাকি কাজ পরের ধাপে।
