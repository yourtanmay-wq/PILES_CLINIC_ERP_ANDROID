# V224 — Android / Web App / Website পরিবর্তন-সমতা (Parity) নোট

তিনটি surface একই সর্বশেষ অবস্থায় আছে কিনা — item ধরে যাচাই:

| V224 পরিবর্তন | Native Android | Web App + Website (`app.js` — assets/www ও 03_NETLIFY_READY, দুটি হুবহু এক) |
|----------------|----------------|------------------|
| **Item 4** — কার্ড ট্যাগ "UNEXPECTED TIME"→"UNEXPECTED" | ✅ প্রয়োগ (`FollowUpActivity.kt`) | **প্রযোজ্য নয় (কিছু পিছিয়ে নেই)** — web-এ ঐ ট্যাগ কার্ডে দেখানো হয় না; "Unexpected Time" কেবল ফর্মের time-picker মান ও "Unexpected Time Calls" list-নাম (item 42 — রাখা বাধ্যতামূলক) হিসেবে আছে। তাই বদলানোর মতো ট্যাগ web-এ নেই। |
| **Item 87** — Conversion ≤100% | ✅ clamp যোগ (`ReportsRepository.kt`) | ✅ **web-এ আগে থেকেই সঠিক ও উন্নত** (`reportConversion`, V195): আসল converted-enquiry হিসাব + `Math.min(100,…)`. Native এখন web-এর সঙ্গে সমান। |
| **Item 8/20** — কার্ড বোতাম উচ্চতা −20% + Next=নীল | ✅ প্রয়োগ (`FollowUpActivity.kt`) | **native-কার্ড-নির্দিষ্ট** — web কার্ড আলাদা CSS ডিজাইন; ঐ dp/drawable web-এ নেই। working web design না ভাঙতে জোর করে বদলানো হয়নি (নিয়ম ৮/১০)। |
| **Item 82/83** — Official ID duplicate DB-guard (SQL) | Backend | Backend — একই Supabase, তাই **তিন surface-এই সমান প্রযোজ্য**। |

**সিদ্ধান্ত:** এই V224-এর পরিবর্তনগুলোর জন্য Web App ও Website **পিছিয়ে নেই** —
হয় web-এ আগে থেকেই সঠিক (conversion), নয়তো web-এ ঐ উপাদানটাই নেই (native-কার্ড
ট্যাগ/বোতাম)। assets/www ও 03_NETLIFY_READY দুটি কপি byte-for-byte এক (যাচাই করা)।
Supabase guard তিনটিতেই সমান।

*(2026-08-01)*
