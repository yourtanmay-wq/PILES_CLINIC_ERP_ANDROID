# V234 — Changed files

**Base:** V233। **Build:** owner-এর Android Studio-তে; এই cloud-এ SDK নেই তাই **FINAL নয়**।
**তারিখ:** 01.08.2026, 12:55 PM IST। **Demo Proof TK-অনুমোদিত ("done")।**

## এই version-এর কাজ (TK verified + demo-approved)
**সমস্যা:** Send Message popup-এ SMS option উধাও — শুধু WhatsApp ও Later দেখাচ্ছিল। কারণ SMS বোতাম `whatsAppOnly`/`waOnly` শর্তে লুকানো ছিল।

**GLOBAL RULE প্রয়োগ:** সম্পূর্ণ project-এর প্রত্যেক Send Message popup-এ **একই সারিতে একই ক্রম — WhatsApp → Later → SMS**, তিনটি বোতামই সবসময়। WhatsApp/SMS চাপলে বর্তমান Message + সঠিক ১০-সংখ্যার নম্বর নিয়ে flow খোলে; Later অপরিবর্তিত।

## পরিবর্তিত ফাইল (৩টি)
- `…/native/PatientMessage.kt` — `presentSendBox`: ক্রম WhatsApp→Later→SMS; SMS-এর `whatsAppOnly` গার্ড তুলে দেওয়া (সব popup-এ SMS)।
- `…/03_NETLIFY_READY/app.js` — `wlv1AskSend`: এক সারিতে WhatsApp→পরে পাঠাব→SMS, SMS সবসময়, approved রঙ; ছবি-নামানোর বোতাম নিচে বহাল।
- `…/02_ANDROID_SOURCE_CODE/…/assets/www/app.js` — উপরেরটির mirror (Android WebView)।

## রঙ (approved demo অনুযায়ী)
WhatsApp সবুজ `#0C9E33` · Later ধূসর `#E4E8EE` · SMS নীল `#1E5AB4` — professional/balanced, header/preview/size/shape/spacing অপরিবর্তিত।

## যা ছোঁয়া হয়নি (যাচাইকৃত)
- Final-Locked Message text ও Language; Enquiry/Visit/Patient ও First-Visit/Next-Visit-এর আলাদা logic।
- WhatsApp ও Later/পরে-পাঠাব-এর existing behavior (`sendWhatsApp`/`wa.me`, `finishOnce()`/`closeModal()`)।
- Popup header, "To patient" লাইন, message preview box, design/size/spacing।
- Payment/Registration/Follow-up/Trash-Restore/permission/database/sync; আগের V231/V232/V233; Website-এর Call/WhatsApp লিংক; অন্য কোনো popup/screen।
- Website (Public Site)-এ রোগী-বার্তা popup নেই — অপরিবর্তিত।

## যা এই cloud-এ করা যায়নি (সৎ)
- **Compile/Build/APK — হয়নি** (SDK নেই)। JS `node --check` OK; Kotlin structure যাচাই; একটি স্বতন্ত্র review-তে **BUILD-SAFE ও scope clean**। "Build/Test Pass" দাবি করা হচ্ছে না।

## Rollback
`ROLLBACK_V234/` — তিন ফাইলের সত্যিকারের **pre-V234 (=V233)** কপি: `native/PatientMessage.kt`, `netlify/app.js`, `assets_www/app.js`। ফেরত চাইলে ওগুলো বদলে দিলেই V233।

## Owner-এর যাচাই
1. Android: যেকোনো বার্তা (Next Visit / Enquiry / First Visit / Registration ইত্যাদি) → popup-এ **তিনটি বোতাম** এক সারিতে **WhatsApp · Later · SMS** ক্রমে; তিনটিই পরিষ্কার, কাটা/overlap নয়।
2. WhatsApp চাপলে WhatsApp, SMS চাপলে Message অ্যাপ — বর্তমান লেখা ও সঠিক নম্বরসহ। Later কিছু পাঠায় না, popup বন্ধ।
3. Web App-এও একই ক্রম ও তিন বোতাম।
