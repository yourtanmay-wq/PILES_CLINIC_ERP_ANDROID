# V229 — Changed files

**Base:** V228। **Build:** owner-এর মেশিনে; FINAL নয়।

## এই version-এর কাজ
Follow-up card-এর বাঁ কলামে Wi-Fi signal-এর নিচের **section নাম ও তারিখ সামান্য বড়** করা হলো (পরিষ্কার পড়ার জন্য) — তিন card-এই:
- Enquiry card: "Enquiry" লেখা 8.5→10sp; তারিখ 8→9.5sp।
- Visit/Patient card: "VISITED"/"PATIENT" section pill 10→11sp; নিচের তারিখ/Patient-ID 8→9.5sp (auto-fit 6–10sp, লম্বা ID এখনো এক লাইনে ঠিকঠাক বসে)।

শুধু text size বদলেছে। Card layout, spacing, margin, Wi-Fi signal, রং, বোতাম, অন্য লেখা ও workflow **অপরিবর্তিত**।

## পরিবর্তিত ফাইল (৪টি)
- `…/native/FollowUpActivity.kt` — ৫টি text-size সংখ্যা।
- `…/app/build.gradle.kts` — version 229 / 2.29।
- `assets/www/index.html` ও `03_NETLIFY_READY/index.html` — web `?v=v229`।

## Rollback
`ROLLBACK_V229/` — ৪টি ফাইলের সত্যিকারের pre-V229 (=V228) কপি (কার্যকর)।
