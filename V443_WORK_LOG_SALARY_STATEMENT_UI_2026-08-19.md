# V443 — Salary Statement Professional UI

**তারিখ:** 19.08.2026 · **সময়:** 10:33 AM IST  
**অনুমতি:** TK — “কোড বসান”  
**Base:** V442 / 4.42 → **V443 / 4.43**

## মালিকের অনুমোদিত কাজ

ফটো-প্রুফে অনুমোদিত Salary Statement design code-এ বসানো। বিশেষ শেষ নির্দেশ:
**HISTORICAL এবং Date আরও ডানদিকে, এবং প্রত্যেক সারিতে একই সোজা রেখা বরাবর।**

## যা করা হয়েছে

### Android
- `StaffProfileActivity.kt`-এর শুধু `showAllPayments()` presentation নতুন professional card design-এ সাজানো।
- Summary: Salary paid · Extra income paid · Extra income due · Grand total paid।
- All Entries-এর প্রতিটি row card করা।
- **Mode/HISTORICAL fixed-width right column** এবং **Date fixed-width right column** — সব salary row-এ একই x-position।
- Due row হালকা লাল, paid/history row সাদা/সবুজ professional styling।
- নীচে Total Entries · Period · Net Paid footer।
- কোনো emoji/icon যোগ করা হয়নি।

### Web
- একই Salary Statement/Payment History presentation `profile.js` + `styles.css`-এ বসানো।
- Mobile view-এ Android-এর একই fixed Mode/Date alignment; desktop-এ responsive wider columns।
- `index.html`-এ শুধু changed `styles.css` ও `profile.js` cache-bust query bump করা হয়েছে, যাতে পুরনো design browser cache-এ আটকে না থাকে।

## যা একদম ছোঁয়া হয়নি

- Salary হিসাব/মাস গণনা
- Salary payment save/edit
- Extra income calculation / DUE rule
- Supabase query/schema/RLS
- Login/session
- Patient/payment/medical workflow
- Doctor Queue / Global Search / Delete Guard
- অন্য কোনো screen-এর design

## হিসাব অপরিবর্তিত রাখার প্রমাণ

V442-এর total formula হুবহু রাখা হয়েছে:
- Salary row → `totSalary`
- EXTRA + PAID → `totExtra`
- EXTRA + DUE → `totDue`
- Grand total paid → `totSalary + totExtra`

Entry order-ও `pays` যেভাবে আগে আসত সেই order-এই render হয়; কোনো নতুন sorting/filtering নেই।

## Verification

- `tk_guard.py` — all machine checks PASS
- `node --check profile.js` — PASS
- Kotlin standalone parser check-এ নতুন অংশে syntax “expecting/unexpected” error নেই (Android dependency ছাড়া unresolved reference স্বাভাবিক)
- Android fixed columns: Mode = 78dp, Date = 86dp
- Web fixed columns: mobile 72px/82px, normal 78px/86px, desktop 100px/110px
- `HISTORICAL` + Date একই row-এ এবং একই straight vertical columns-এ রাখা হয়েছে
- Version parity: Android V443 / 4.43, Web version.json V443 / 4.43

## Android Gradle build honesty

এই execution environment-এ Gradle 8.5 distribution local cache-এ নেই এবং internet/DNS থেকে download করা যায় না। তাই actual Android Studio/Gradle build **PASS বলে দাবি করা হয়নি**। Machine guard ও source-level verification সম্পন্ন হয়েছে।

## Final release check

- `tk_guard.py --release` PASS ✅
- Unique release name reserved: `PILES_CLINIC_APP_V443_FINAL.zip`
- Actual Gradle attempt: wrapper could not resolve/download Gradle 8.5 from `services.gradle.org` (`UnknownHostException`); no project compile error was reported because Gradle itself could not start.
