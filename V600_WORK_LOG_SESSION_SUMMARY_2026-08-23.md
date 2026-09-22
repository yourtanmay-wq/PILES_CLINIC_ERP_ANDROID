# V600 — এই সেশনের সব কাজ
**তারিখ:** ২৩.০৮.২০২৬ · **ভার্সন:** V600 / 6.00

## এই সেশনে যা হয়েছে (V597 থেকে V600 পর্যন্ত)

1. **DoctorCheckupActivity.kt import বাগ** (V598→V599) — `NoBengali` ও JSON
   `.s()` extension দুটো আলাদা জিনিস, দুটো import-ই দরকার ছিল।
2. **রোগের ছবির বাক্স** — ছবির নিজের অনুপাতে বাক্সের উচ্চতা এখন নিজে থেকে
   ঠিক হয় (আগে fixed 300dp ছিল, লম্বা ছবিতে দুই পাশ ফাঁকা থাকত)।
3. **History-এর ফ্রি-টেক্সট বাক্স বাদ** — "এই ইতিহাস নিয়ে আর কিছু থাকলে
   লিখুন" সরানো হয়েছে। **Patient Said** বাক্স এখন অন্য বাক্সের মতোই ছোট শুরু
   হয়, টাইপ করলে বড় হয়।
4. **Blood Test A4-এ ভুতুড়ে ২য় (ফাঁকা) পাতা** — `.sheet`-এ `overflow:hidden`
   ছিল না + উচ্চতা ভুল ছিল (296mm, আসল A4=297mm)। ঠিক করা হলো।
5. **সম্ভাব্য রোগ + সময়ের ঘর** সরানো হলো Estimate সেকশনের মাথায়; সেকশনের
   নাম "Estimate & Decision" → শুধু "Estimated Cost · আনুমানিক খরচ"।
6. **Photo & Video সেকশন** এখন fold করা (চাপলে খোলে), আগের মতো সবসময়
   খোলা থাকত না।
7. **হ্যাঁ/না প্রশ্নে দুটোই বাছা যেত** (Lifestyle: টয়লেট + কোঁথ প্রশ্ন) —
   এখন single-select (radio), Android + Web দুটোতেই।
8. **Investigation Advice স্ক্রিন** — হেডার "Blood Test / Investigation
   Advice" → "Test / Investigation" (16sp); ৮টা ক্যাটাগরি-বক্স ছোট (padding
   6dp/6dp), আইকন বাদ, নামের লেখা বড় (11sp→13sp) — Android + Web দুটোতেই।
9. **🔴 আসল কাজ — Add Payment খুললেই বারবার লোডিং:**
   - **কারণ:** `SupabaseClient.findByMobile()` ও `findByMobileOrNull()`
     (৪০+ জায়গায় ব্যবহৃত — Payment, Doctor Visit, Chamber Attendance,
     Registration, Print Center, Enquiry, Follow-up...) কখনোই
     `CloudReadDedupe` (V493-এর প্রমাণিত ৬০-সেকেন্ড cache) ব্যবহার করত না,
     যদিও `fetchListOrNull()` অনেক আগে থেকেই করে।
   - **সমাধান:** দুটো ফাংশনই এখন সেই একই cache-এর ভিতর দিয়ে যায় — URL,
     filter, limit, ব্যর্থতার আচরণ **এক অক্ষরও বদলায়নি**।
   - **নিরাপত্তা:** `CloudReadDedupe` প্রতিটা payment/patient সেভের পরেই
     (`recordTreatmentPayment` · `upsert` · `updateById` · `deleteById`)
     নিজে থেকে খালি হয়ে যায় (৭ জায়গায় যাচাই করা হয়েছে) — তাই সেভের পরে
     কখনো পুরনো Paid/Due দেখানোর ঝুঁকি নেই।
   - **ফল:** একই রোগীর "Add Payment" ৬০ সেকেন্ডের মধ্যে আবার খুললে
     সঙ্গে সঙ্গে খোলে, নতুন নেট-কল হয় না — লোডিং কমবে, Supabase egress-ও
     কমবে (Payment ছাড়াও Doctor Visit, Chamber, Print Center-সহ ৪০+
     জায়গায় একসাথে উপকার)।
   - ওয়েবে এই সমস্যা নেই (ডেটা আগে থেকেই লোকাল লোড থাকে)।

## যাচাই
| পরীক্ষা | ফল |
|---|---|
| `tk_guard.py --release` (সব যাচাই) | ✅ সম্পূর্ণ পাশ |
| `verify_version_json.py` | ✅ V600/6.00 মিলেছে |
| `node --check app.js` | ✅ পাশ |
| XML সব ফাইল | ✅ valid |
| `CloudReadDedupe.clear()` ৭ জায়গায় (সব লেখার পথে) | ✅ যাচাই করা |

**সৎ সীমা:** এই পরিবেশে Android SDK/Gradle নেই, তাই সত্যিকারের build এখানে
চালানো যায়নি — TK Android Studio-তে বিল্ড করে নিশ্চিত করবেন।
