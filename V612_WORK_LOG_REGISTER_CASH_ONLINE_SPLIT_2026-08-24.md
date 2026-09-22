# V612 — Chamber Register প্রিন্ট: Fees/Total/Medicine Cash-Online ভাগ
**তারিখ:** ২৪.০৮.২০২৬ · **ভার্সন:** V612 / 6.12

## TK-এর নির্দেশ
"Fees সব এক জায়গায় করেছেন কেন — Cash/Online আলাদা করতে হবে। তারপর Total
Cash (Fees+Treatment) / Total Online (Fees+Treatment)। Medicine বিক্রি
হলে সেটাও Cash/Online আলাদা। খুব নিরাপদে করবেন।"

## সমাধান
### ডেটা (সম্পূর্ণ additive, পুরনো হিসাব অক্ষত)
- `ChamberAttendanceRow`/`ChamberAttendanceTotals`-এ নতুন
  `medicineCash`/`medicineOnline` (ডিফল্ট 0.0) — পেমেন্ট-লুপে যখন
  `srcLabel == "Medicine Payment"` তখন **বাড়তি** যোগ হয়, পুরনো
  `paymentCash`/`paymentOnline`-এর হিসাব এক অক্ষরও বদলায়নি (ওষুধের টাকা
  আগের মতোই সেখানেও থাকে — দ্বিগুণ গোনা এড়াতে সাবধানে করা হয়েছে)।
- cache read/write (SharedPreferences) দুই জায়গাতেই যোগ, নইলে দ্রুত
  cache-দেখানোর সময় হারিয়ে যেত।

### প্রিন্ট (ChamberRegisterPdfBuilder.kt)
- ওপরের TOTAL টেবিল-সারি (Fees|Cash|Online কলাম) **অক্ষত**।
- নিচের এক-লাইন সারাংশ এখন:
  `Fees: Cash ₹X · Online ₹Y   ·   Total Cash ₹P · Total Online ₹Q   ·   TOTAL ₹Z`
  যেখানে Total Cash/Online = Fees + Treatment (Treatment-এর ভিতরেই
  Medicine ধরা আছে, তাই দ্বিগুণ গোনা হয় না)।
- Medicine বিক্রি হলে (>0) তার উপরে আলাদা লাইন:
  `Medicine: Cash ₹A · Online ₹B` — বিক্রি না হলে লাইনটাই ছাপা হয় না।
- GRAND TOTAL অঙ্ক এক পয়সাও বদলায়নি — শুধু বিভাজন স্পষ্ট হলো।

## নিরাপত্তা
- সব নতুন ফিল্ড ডিফল্ট 0.0/কনস্ট্রাক্টরের শেষে — পুরনো কোনো কল-সাইট ভাঙেনি।
- RMP Commission/Paid-today লাইনের অবস্থান/রং/নিয়ম অক্ষত।

## যাচাই
| পরীক্ষা | ফল |
|---|---|
| `tk_guard.py --release` | ✅ পাশ |
| `verify_version_json.py` | ✅ V612/6.12 |
| ছবি-প্রুফ (সারাংশ-লাইন) | ✅ দেখানো হয়েছে |

## ওয়েব
এই সেশনে শুধু Android-এ করা হলো (Chamber Register প্রিন্ট Android-only
ফিচার) — ওয়েবে সমতুল্য প্রিন্ট থাকলে জানালে আলাদা করে করা হবে।
