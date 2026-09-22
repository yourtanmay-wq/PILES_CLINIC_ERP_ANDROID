# V233 — Changed files

**Base:** V232। **Build:** owner-এর Android Studio-তে; এই cloud-এ SDK নেই তাই **FINAL নয়**।
**তারিখ:** 01.08.2026, 12:40 PM IST।

## এই version-এর কাজ (একটাই, TK verified live-date fix)
**সমস্যা:** 1 August হলেও একটি Calendar icon-এ fixed **"July 17"** দেখাচ্ছিল — কারণ ওটা raw `📅` ইমোজি, যার নিজের আঁকা ছবিতেই "Jul 17" বসানো (আসল তারিখ নয়)।

**পুরো-project AUDIT:** এই বাগ শুধু **একটি** জায়গায় বাকি ছিল — **Doctor Visit পর্দার "EXPECTED" stat-কার্ড**-এর একলা `📅` ইমোজি। Follow-up ও Chamber পর্দায় আগেই live 2-line badge বসানো; বাকি সব 📅 live তারিখের পাশে শুধু-সাজানো; Web/Website-এ কোনো fixed-তারিখ ক্যালেন্ডার নেই।

**সমাধান:** ওই একলা ইমোজিকে **Follow-up/Chamber-এর হুবহু একই ২-লাইনের live calendar badge** দিয়ে বদলানো হলো — উপরে মাস (`bg_cal_badge_top`), নিচে দিন (`bg_cal_badge_bottom`) — যা আজকের (device-local = IST) তারিখ দেখায় (যেমন **"Aug / 1"**), প্রতিদিন নিজে থেকে বদলায়। Calendar-style design/জায়গা/মাপ এক; ইমোজি "সরানো" নয়, বরং সঠিক live calendar-এ রূপান্তর।

## পরিবর্তিত ফাইল (২টি)
- `…/res/layout/activity_doctorvisit.xml` — "EXPECTED" কার্ডের `📅` TextView → live 2-line badge (`tvDvCalMonth` + `tvDvCalDay`)।
- `…/native/DoctorVisitActivity.kt` — `onCreate`-এ badge-এ আজকের IST মাস/দিন সেট (Follow-up পর্দার একই কোড)।

## যা ছোঁয়া হয়নি (যাচাইকৃত)
- EXPECTED কার্ডের **count (`tvTodayCount`) / filter (`"expected"`) / click** — অপরিবর্তিত।
- অন্য সব 📅 ইমোজি (Next Visit Date / Visit Reminder / Custom Date / Calendar বোতাম / web আইকন — সব live তারিখের পাশে) — সরানো/বদলানো হয়নি।
- Follow-up ও Chamber-এর existing live badge; UI design/layout/color/spacing/buttons; date calculation/filter/payment/collection/enquiry/follow-up/database/sync; Registration/Trash-Restore/permission; আগের V231 (delete/stale) ও V232 (First Visit বার্তা)।

## যা এই cloud-এ করা যায়নি (সৎ)
- **Compile/Build/APK — হয়নি** (SDK নেই)। XML valid, resource/id/binding সব যাচাই + একটি স্বতন্ত্র review-তে **BUILD-SAFE**, scope clean। "Build/Test Pass" দাবি করা হচ্ছে না।

## Rollback
`ROLLBACK_V233/layout/activity_doctorvisit.xml` ও `ROLLBACK_V233/native/DoctorVisitActivity.kt` — সত্যিকারের **pre-V233 (=V232)** কপি (uploaded zip থেকে)। ফেরত চাইলে ওই দুটি বদলে দিলেই V232।

## Owner-এর যাচাই (Android Studio-তে build করে)
1. Doctor Visit পর্দা খুলুন → "EXPECTED" কার্ডে আজকের তারিখ (**"Aug / 1"** ধাঁচে) দেখাচ্ছে কি না — fixed "Jul 17" আর নেই।
2. Follow-up ও Chamber পর্দার ক্যালেন্ডার badge আগের মতোই ঠিক আছে কি না (অপরিবর্তিত)।
3. EXPECTED কার্ডে চাপলে আগের মতোই "expected" filter কাজ করছে কি না, count ঠিক আছে কি না।
