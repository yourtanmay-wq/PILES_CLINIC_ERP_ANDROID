# V703 — ধাপ ২ "Clinical পরীক্ষা" বন্ধ অবস্থায় শুরু হবে
**তারিখ:** ২৬.০৮.২০২৬ · **ভার্সন:** V703 / 7.03

## TK কী বলেছেন
> "এখানেও Form টা ওপেন থাকবে না" (ছবি: ধাপ ২ পুরো খোলা)
> "শুধুমাত্র যেটুকু করতে বলা হলো সেটুকু করুন অর্থাৎ ক্লিনিক্যাল"

ডেমো প্রুফ (`V703_DEMO_CLINICAL_FOLD.png`) দেখিয়ে অনুমোদন নেওয়ার পরেই কাজ শুরু।

## কী করা হয়েছে
**শুধু ধাপ ২।** ধাপ ৩ · ৪ · ৫ এবং ধাপ ১ — একটাও ছোঁয়া হয়নি।

### ফোন (Android)
| ফাইল | কী বদলাল |
|---|---|
| `res/layout/activity_doctor_checkup.xml` | শিরোনামের সারিতে id `clinicalFoldHead` + ব্যাজ `clinicalFoldNum` + চিহ্ন `clinicalFoldChev`; বাকি সবটা `clinicalFoldBody` মোড়কে (`visibility="gone"`) |
| `clinical/DoctorCheckupActivity.kt` | নতুন `wireClinicalFold()` ও `refreshClinicalFold()`; `attachFold`-এ **ঐচ্ছিক** `onToggle` (default `null`) |

### কম্পিউটার (Web)
| ফাইল | কী বদলাল |
|---|---|
| `03_NETLIFY_READY/app.js` | ধাপ ২-এর `<details class="card" open>` থেকে শুধু `open` সরানো |

## যা ইচ্ছাকৃতভাবে করা হয়নি — এবং কেন
* **চেকবক্সে নতুন listener বসানো হয়নি।** `buildChecks()` প্রতিটা চেকবক্সে নিজের
  `setOnCheckedChangeListener` বসায় (সবুজ পিলের রং)। আরেকটা বসালে ওটা **মুছে যেত**
  এবং টিক দিলে আর সবুজ হত না। তাই ব্যাজের সংখ্যা বসে তিন জায়গায় —
  শুরুতে · পুরোনো রেকর্ড খোলার পরে · ভাঁজ খোলা-বন্ধ করার সময়
  (ব্যাজ তো বন্ধ অবস্থাতেই দেখা যায়)।
* **`attachFold` ভাঙা হয়নি।** নতুন প্যারামিটারটার default `null`, তাই আগের তিনটে
  ডাক (`sym` · `life` · `photo`) এক অক্ষরও বদলায়নি।
* **কোনো ঘর/id/টিক/সেভ বদলায়নি** — `visualGroup` · `dreGroup` · `etDreOther` ·
  `spGrade` · `etProctoscopy` · `etOnProbing` সব আগের জায়গাতেই, শুধু একটা মোড়কের
  ভিতরে। `findViewById` আগের মতোই কাজ করে (GONE হলেও View গাছেই থাকে), তাই
  সেভ/পড়া/পুরোনো রেকর্ড — সব অক্ষত।

## যাচাই
| যাচাই | ফল |
|---|---|
| XML গঠন (`xml.dom.minidom`) | ✅ valid |
| `verify_kotlin_compile.py` (kotlinc + android.jar) | ✅ **নতুন ভুল ০** |
| `app.js` JS syntax (`new Function`) | ✅ OK |
| `tk_guard.py --release` (২২টা মেশিন-যাচাই) | ✅ সব পাশ |

🔴 **নিজের ধরা একটা ভুল:** app.js-এর মন্তব্যে backtick (\`) লিখেছিলাম — ওটা template
literal-এর ভিতরে, তাই পুরো ফাইলের JS syntax ভেঙে গিয়েছিল। syntax যাচাইয়ে ধরা পড়ে
সঙ্গে সঙ্গে সরানো হয়েছে। **আন্দাজে ছাড়া হয়নি — চালিয়ে দেখা হয়েছে।**

⚠️ এখানে Android চালানোর উপায় নেই, তাই "ফোনে দেখতে ঠিক" বলা হচ্ছে না — TK দেখে বলবেন।
