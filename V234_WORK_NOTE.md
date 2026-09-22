# V234 — Work Note (কোড পরিবর্তনের আগে · Demo-অনুমোদনের অপেক্ষায়)

**Base:** V233 (সর্বশেষ working project — এটাই একমাত্র base)।
**তারিখ ও সময় (শুরু):** 01.08.2026, 12:40 PM IST।
**অবস্থা:** 🛑 এখনো **কোনো source file বদলানো হয়নি**। আগে Demo Proof, TK-এর অনুমোদনের পরেই কোড।

---

## ১. SMS option কেন অনুপস্থিত হয়েছে
Send Message popup-এ SMS বোতামটা একটা শর্তের ভিতরে লুকানো — `whatsAppOnly`/`waOnly` **true** হলে SMS দেখানো হয় না, শুধু WhatsApp + Later/পরে-পাঠাব থাকে। যেসব বার্তা লম্বা/WhatsApp-সাজানো (যেমন Enquiry template, First Visit, Receipt, Document), সেগুলো `whatsAppOnly = true` দিয়ে খোলে — তাই ওই popup-গুলোতে SMS উঠে যায়। এটাই বর্তমান "শুধু WhatsApp ও Later" দেখার কারণ।

## ২. কোন কোন Message Popup-এ সমস্যা / যেগুলো একই নিয়মে আনতে হবে
- **Android App —** `…/native/PatientMessage.kt` → `presentSendBox(...)` (একটাই কেন্দ্রীয় popup; সব বার্তা এখান দিয়েই যায় — Registration/Bill/Payment/VISIT_DATE/Enquiry/First Visit/Receipt/Document সব)। বর্তমান ক্রম: **WhatsApp · SMS(শুধু whatsAppOnly=false হলে) · Later**।
- **Web App —** `…/03_NETLIFY_READY/app.js` (এবং mirror `…/assets/www/app.js`) → `wlv1AskSend(...)` popup। বর্তমান ক্রম: **WhatsApp · SMS(waOnly হলে লুকানো) · [🖼️ ছবি নামান] · পরে পাঠাব**।
- **Website —** Public Site-এ রোগীকে-বার্তা-পাঠানোর এমন কোনো popup নেই (শুধু Call/WhatsApp সরাসরি লিংক)। তাই এখানে এই popup প্রযোজ্য নয় — যাচাই করে জানানো হলো।

## ৩. প্রস্তাবিত পরিবর্তন (অনুমোদনের পরে)
প্রতিটি Send Message popup-এ **একই সারিতে নির্দিষ্ট ক্রম:**

> **WhatsApp (প্রথমে) → Later/পরে পাঠাব (মাঝখানে) → SMS (শেষে)**

- SMS আর `whatsAppOnly`/`waOnly` দিয়ে লুকানো হবে না — **সব popup-এ তিনটি বোতামই** থাকবে।
- WhatsApp ও SMS চাপলে বর্তমান Message + সঠিক ১০-সংখ্যার Mobile নিয়ে সংশ্লিষ্ট flow (`sendWhatsApp`/`sendSms`, web-এ `wa.me`/`sms:`) খুলবে — এই flow **আগের মতোই**।
- Later/পরে-পাঠাব-এর behavior **অপরিবর্তিত** (`finishOnce()` / `closeModal()`)।
- রঙ: WhatsApp সবুজ `#0C9E33` · Later ধূসর `#E4E8EE` · SMS নীল `#1E5AB4` — বর্তমান professional/balanced combination-ই।

## ৪. কোন File পরিবর্তন করতে হবে (অনুমোদনের পরে — সর্বোচ্চ ২–৩টি)
1. `…/native/PatientMessage.kt` — শুধু `presentSendBox`-এর বোতাম-সারির **ক্রম ও SMS-শর্ত** (design/preview/header অপরিবর্তিত)।
2. `…/03_NETLIFY_READY/app.js` — `wlv1AskSend`-এর বোতাম-ক্রম ও SMS-শর্ত।
3. `…/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/assets/www/app.js` — উপরেরটির mirror (Android WebView একই কোড ব্যবহার করে)।

## ৫. কোন PASS/LOCKED কাজ পরিবর্তন করা যাবে না
- Final-Locked Message text ও Language (Enquiry/First Visit/VISIT_DATE ইত্যাদি এক অক্ষরও নয়)।
- Enquiry/Visit/Patient-এর আলাদা Message logic; First Visit vs Next Visit-এর আলাদা নিয়ম।
- WhatsApp ও Later-এর existing কার্যকর behavior।
- Popup-এর approved design/size/shape/spacing/alignment/visual identity; header, "To patient" লাইন, message preview box।
- Payment, Registration, Follow-up, Trash/Restore, permission, database, sync; আগের V231/V232/V233 কাজ; অন্য কোনো popup/screen/workflow।

**পরিকল্পনা:** শুধু বোতাম-ক্রম (WhatsApp→Later→SMS) ও সব popup-এ SMS ফিরিয়ে আনা। কোনো broad redesign/refactor/cleanup/optimization নয়।

---

## ৬. অনুমোদনের পরে সম্পন্ন
**তারিখ ও সময় (শেষ):** 01.08.2026, 12:55 PM IST। TK Demo Proof দেখে **"done" (অনুমোদন)** দিয়েছেন — তারপর কোড।

### পরিবর্তিত File-এর সঠিক তালিকা (৩টি)
- `…/native/PatientMessage.kt` — `presentSendBox`-এর বোতাম-সারি: ক্রম **WhatsApp → Later → SMS**; `if (!whatsAppOnly)` গার্ড তুলে SMS সব popup-এ।
- `…/03_NETLIFY_READY/app.js` — `wlv1AskSend`: এক সারিতে WhatsApp → পরে পাঠাব → SMS, SMS সবসময়, approved রঙ; "🖼️ ছবি নামান" নিচে বহাল।
- `…/02_ANDROID_SOURCE_CODE/…/assets/www/app.js` — উপরেরটির হুবহু mirror (Android WebView)।

### কোন কোন Popup সংশোধন হয়েছে
- Android: কেন্দ্রীয় `presentSendBox` — অর্থাৎ **সব বার্তা** (Registration/Bill/Payment/Next Visit/Enquiry/First Visit/Receipt/Document) এখন একই ক্রমে ও তিন বোতামেই।
- Web App: `wlv1AskSend` popup (Advance/Payment ইত্যাদি রোগী-বার্তা)।
- Website (Public Site): রোগী-বার্তা popup নেই — অপরিবর্তিত।

### WhatsApp · Later · SMS — Test result (owner Android Studio/Browser-এ যাচাই করবেন)
- **WhatsApp:** চাপলে আগের মতোই বর্তমান Message + ১০-সংখ্যার নম্বর নিয়ে WhatsApp খোলে (`sendWhatsApp`/`wa.me`)। ✓
- **SMS:** এখন সব popup-এ দেখা যায়; চাপলে বর্তমান Message + নম্বর নিয়ে Message অ্যাপ খোলে (`sendSms`/`sms:`)। ✓
- **Later/পরে পাঠাব:** অপরিবর্তিত — কিছুই পাঠায় না, popup বন্ধ (`finishOnce()`/`closeModal()`)। ✓
- ⚠️ এই cloud-এ SDK নেই তাই device-run হয়নি; static যাচাই + JS `node --check` OK + স্বতন্ত্র review-তে **BUILD-SAFE**।

### Final order: **WhatsApp → Later → SMS** (তিন platform-এ এক)।

### approved Demo-র design ও color combination রক্ষা
WhatsApp সবুজ `#0C9E33` · Later ধূসর `#E4E8EE` · SMS নীল `#1E5AB4` — Demo-তে যা অনুমোদন হয়েছে ঠিক তাই। header/preview/size/shape/spacing অপরিবর্তিত।

### Declaration
শুধু বোতামের ক্রম ও SMS-এর দৃশ্যমানতা (+ web-এ approved রঙ) বদলেছে। **এক অক্ষরও বদলায়নি:** Message text/language, Enquiry/Visit/First-Visit/Next-Visit logic, WhatsApp/Later behavior, Payment/Registration/Follow-up/Trash-Restore, permission/database/sync, আগের V231/V232/V233 কাজ, অন্য কোনো popup/screen/UI। কোনো broad redesign/refactor/cleanup/optimization হয়নি। "Build/Test Pass" দাবি নয় — owner build ও যাচাই করবেন।
