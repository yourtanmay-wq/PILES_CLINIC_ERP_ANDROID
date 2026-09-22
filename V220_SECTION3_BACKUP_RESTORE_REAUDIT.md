# V220 — §3 (পুরোনো Backup Restore করলেও নতুন Cloud Data অক্ষত) — পুনরায় সঠিক Audit

**তারিখ:** 31.07.2026 IST। **এই version-এ §3 কোড-এ করা হয়নি** — শুধু আপনার নির্দেশমতো আবার সঠিকভাবে audit। নিচের ঝুঁকি ও ঝুঁকিহীন plan দেখে আপনি আলাদা অনুমতি দেবেন।

## মূল কারণ (নিশ্চিত)
প্রতিটা cloud লেখা `SupabaseClient.upsert` → `Prefer: resolution=merge-duplicates` (SupabaseClient.kt:181) — অর্থাৎ **একই id হলে পুরো row overwrite**, কোনো `updatedAt` তুলনা নেই। Web-এও একই (`app.js` `cloudUpsertAdaptive` → `upsert({onConflict:'id'})`)। **Android-এ কোথাও `updatedAt` মিলিয়ে overwrite ঠেকানো নেই।**

## কোন কোন পথে পুরোনো data নতুনকে চাপা দিতে পারে (file:line)

| # | পথ | file:line | ঝুঁকি |
|---|----|-----------|-------|
| A | Trash **Restore** (Android) — পুরোনো snapshot upsert | `TrashRepository.kt:77` `upsert(item.table, item.record)` | **হ্যাঁ** — record মুছে ফেলার পর ঐ id অন্য ফোনে নতুন করে edit/তৈরি হলে, Master Restore করলে পুরোনো snapshot নতুনকে চাপা দিতে পারে |
| B | Trash **Restore** (web) | `app.js` `wlv1RestoreTrash` (~7857) `directCloudUpsertRow(x.table,rec)` | **হ্যাঁ** — একই দৃশ্য; raw upsert, merge নেই |
| C | আটকে থাকা **queued UPSERT** পরে replay | `CloudWriteQueue.flush` (UPSERT শাখা) | **হ্যাঁ (ফাঁক)** — একটা সারি UPSERT queue-তে জমা, এর মধ্যে ঐ id অন্যত্র edit হয়ে cloud-এ **সফল** হলো; পরে flush পুরোনো body পাঠিয়ে নতুনকে চাপা দেয়। `forget()` শুধু **DELETE**-এর জন্য ডাকা হয়, UPSERT-এর জন্য নয় |
| D | Web bulk backup restore | `app.js` `applyBackupPayload`(~4027)→`cloudPush`→`mergeForCloudPush` | **কম** — web-এ push-পথে `updatedAt`-ভিত্তিক merge আছে (নতুন জেতে); তবে backup row-এ `updatedAt` ফাঁকা/জাল হলে ফাঁক থাকে |
| E | Legacy `SyncManager.push*` (Room stack) | `data/repository/SyncManager.kt:113/130/147/164` | **অনিশ্চিত** — pending-flagged row unconditional upsert, PULL-এ `updatedAt` তুলনা আছে কিন্তু PUSH-এ নেই। এই stack এখনো active কিনা যাচাই দরকার |

**ভালো দিক (যা আগেই সুরক্ষিত):** DELETE-vs-Restore দৌড় ঠিক আছে — Restore-এ `DeletedGuard.unmark` → `CloudWriteQueue.forget("DELETE",...)` আটকে থাকা DELETE মুছে দেয় (TrashRepository.kt:76)। web `restoreDraftEntry` fresh `updatedAt` বসিয়ে current row edit করে (পুরোনো snapshot নয়) — নিরাপদ।

## প্রস্তাবিত সমাধান ও তার আসল ঝুঁকি (এই re-audit-এর মূল কথা)

**DB-side `BEFORE UPDATE` trigger** (`NEW."updatedAt" < OLD."updatedAt"` হলে পুরোনো লেখা বাতিল) A/B/C তিনটি বড় ফাঁকই বন্ধ করে — কারণ ঐ তিন পথের সারিতেই আসল পুরোনো `updatedAt` থাকে। **কিন্তু re-audit-এ ধরা পড়েছে এটা "একদম নিরঝুঁকি নয়" — নিচের ৩টা জিনিস আগে সামলাতে হবে:**

1. **NULL `updatedAt`:** কোনো row-এ `updatedAt` NULL হলে `NULL < OLD` তুলনা মিথ্যা → overwrite হয়ে যায় (trigger ফসকে যায়)। তাই trigger-এ NULL/ফাঁকা `NEW.updatedAt`-কে আলাদাভাবে সামলাতে (reject বা now বসানো) হবে।
2. **backfill upsert যেখানে `updatedAt = createdAt` বসানো হয়** (heal/পুরনো সারি মেরামত): `FollowUpRepository.kt:935,1355,1471`, `PatientTimelineRepository.kt:443` — এগুলো ইচ্ছাকৃতভাবে পুরোনো `updatedAt` বসায়। trigger চালু হলে এগুলো **ভুল করে reject** হতে পারে (মেরামতের সারি বসবে না)। তাই এদের আলাদা করে দেখতে হবে (হয়তো এই লেখাগুলো `updatedAt=now` করা, বা trigger-এ ছাড় দেওয়া)।
3. **subset PATCH যেখানে `updatedAt` পাঠানো হয় না** (যেমন `TrashRepository.kt:102` cascade `status` only): এগুলো trigger-এ "সমান timestamp" ধরে পাস করে — বেশিরভাগ নিরীহ, কিন্তু জানা থাকা ভালো।
4. **Legacy `SyncManager` DTO**-তে `updatedAt` যায় কিনা (`Mappers.kt`) — active হলে যাচাই দরকার; না গেলে NULL-ফাঁক।

**অর্থাৎ trigger-ই সঠিক পথ, কিন্তু "চালিয়ে দিলেই হলো" নয়** — আগে (ক) trigger-এ NULL-guard, (খ) createdAt-as-updatedAt backfill ৪টা লাইন যাচাই/ঠিক, (গ) SyncManager active কিনা — এই তিনটে সামলে, live-এ backup নিয়ে, একটা টেবিলে টেস্ট করে, তারপর সব টেবিলে। এটাই ঝুঁকিহীন করার আসল ধাপ।

## ঝুঁকিহীন Fix Plan (§3 — অনুমতি পেলে V221-এ)
1. **DB trigger (মূল সমাধান, app-code নয়):** প্রতিটা মূল টেবিলে `BEFORE UPDATE` trigger — `IF NEW."updatedAt" IS NULL OR NEW."updatedAt" >= OLD."updatedAt" THEN allow ELSE RETURN OLD (skip)`। NULL হলে থামায় না (পুরোনো subset-PATCH ভাঙে না)। আলাদা copy-paste SQL, live-এ backup+এক-টেবিল-টেস্টসহ।
2. **createdAt-as-updatedAt backfill (৪ লাইন):** `FollowUpRepository.kt:935,1355,1471`, `PatientTimelineRepository.kt:443` — এগুলো heal-লেখা; trigger-এর সঙ্গে যাতে সংঘর্ষ না হয় তা যাচাই করে দরকার হলে ঠিক করা (ঝুঁকি: heal আচরণ — তাই আলাদা করে TK-কে দেখিয়ে)।
3. **App-side (ঐচ্ছিক, ২য় স্তর):** `TrashRepository.restore` Restore-এর আগে cloud row-এর `updatedAt` মিলিয়ে নেওয়া (cloud নবীন হলে overwrite নয়) — এক জায়গার ছোট পাহারা; কিন্তু DB-trigger-ই সব পথ ঢাকে, তাই এটা secondary।
4. **CloudWriteQueue stale-UPSERT:** সফল লেখার পর ঐ (table,id)-এর আটকে থাকা পুরোনো UPSERT queue থেকে `forget` করা — DB-trigger থাকলে এমনিতেই ক্ষতি হয় না, তবু কোটা/পরিচ্ছন্নতায় ভালো।

**ঝুঁকি সারাংশ:** trigger নিজে **কোনো design/workflow/permission/branch/payment rule বদলায় না** — শুধু পুরোনো লেখা চাপা দেওয়া আটকায়। একমাত্র সত্যিকারের সতর্কতা: উপরের ১-৩ নম্বর (NULL, createdAt-backfill) না সামলে চালালে কিছু heal/subset-লেখা আটকে যেতে পারে। তাই এটা **আলাদা version-এ, live backup + এক-টেবিল টেস্ট করে** করা উচিত — এই কারণেই এখন কোড-এ করা হয়নি, শুধু re-audit দেওয়া হলো।
