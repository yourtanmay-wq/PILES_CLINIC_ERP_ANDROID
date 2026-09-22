# V220_FINAL_DECLARATION — সৎ ঘোষণা

**Base:** V219 (219/2.19) → **V220 (220/2.20)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।

### কোন কোন File — `V220_CHANGED_FILES.md`।

### সত্যিই কোড-এ যা হয়েছে (static-verified; device/build/live Pending)
- **§১** HTTP 400-এর PostgREST error body পড়ে **আসল ভুল Field/কারণ** সতর্কবার্তায় (Table·Record-এর সঙ্গে)। নতুন request নেই।
- **§২** permanent 4xx + body-hash — অ্যাপ **নিজে থেকে একই ভুল data আর বারবার পাঠায় না**; retry বন্ধ নয় (নেট-ব্যর্থ/সংশোধিত/"পাঠান" আগের মতোই)।
- **§৪** Refund per-dialog nonce — একই ফর্মের retry double নয়, **নতুন ফর্মে বৈধ দ্বিতীয় Refund আলাদা** (Android+web)।

### §৩ — শুধু পুনরায় সঠিক Audit (কোড-এ করা হয়নি)
`V220_SECTION3_BACKUP_RESTORE_REAUDIT.md`: মূল ফাঁক Trash-Restore (Android/web) + stale queued-UPSERT; সমাধান = DB `updatedAt`-trigger, **কিন্তু আগে NULL-updatedAt, createdAt-as-updatedAt backfill (৪ লাইন), legacy SyncManager সামলাতে হবে** — তাই আলাদা version-এ live-backup+এক-টেবিল টেস্ট করে। আপনার অনুমতির পরে V221-এ।

### Test — শুধু static Pass; সব device/build/live **Pending** (`V220_TEST_REPORT.md`)। signed APK দাবি করা হয়নি।

### Design/Workflow/Permission/Branch/Payment/Diet Chart — কিচ্ছু বদলানো হয়নি
§১ শুধু সতর্কবার্তার লেখা সমৃদ্ধ; §২ queue-র ভেতরের নিয়ম; §৪ id-তে শুধু nonce (totals/approval/branch-money-rule/Master-অনুমোদন অপরিবর্তিত)। §৫ web parity অটুট (assets/www = Netlify identical)।

### Patient/Payment Data — কিছু Delete/drop হয়নি; RLS live-এ চালানো হয়নি।

### Free-plan — §১ শুধু আসা-body পড়ে (নতুন request নেই); §২ resend *কমায়*; §৪ কোনো নতুন read/write নয়। ঝুঁকি নেই/কম।

**⛔ "সব শেষ ও tested" নয়। ১/২/৪ কোড-এ (static-verified), device/live Pending; ৩ শুধু re-audit। কোনো untested জিনিসকে Pass বলা হয়নি।**
