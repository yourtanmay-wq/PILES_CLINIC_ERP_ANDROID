package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 🚨 TK'S ORDER (2026-07-27): "দিনে একবার যখন অ্যাপ্লিকেশন খুলব, অটোমেটিক যেন
 * ব্যাকগ্রাউন্ডে লোডিং-এর কাজ, আপডেটের কাজ... অ্যাপ্লিকেশন বন্ধ থাকলেও চলতে থাকে —
 * ইন্টারনেট কানেকশন অন থাকলেই।"
 *
 * WHAT WAS MISSING: the background job this app already had only ever pushed
 * things UP (unsent enquiries, registrations, payments). Nothing ever pulled the
 * lists DOWN, so the Follow-up tabs were only ever fetched at the moment a tab
 * was opened -- on a 0.16-9 KB/s line that is exactly the "Loading..." TK sees.
 *
 * WHAT THIS DOES: while the phone has internet -- app open or closed -- it
 * quietly fetches the three Follow-up tabs for the logged-in person's branch.
 * fetchTab() already writes its result into the same on-phone cache the screen
 * reads first, so by the time TK opens the screen the list is already there and
 * appears at once.
 *
 * Deliberately careful:
 *  - runs only when someone is logged in and the phone has a connection;
 *  - skips the run if the lists were already refreshed less than 45 minutes ago,
 *    so a phone that is being used all day does not download the same thing
 *    twice (this also protects the Supabase free quota);
 *  - reuses the SAME fetch the screen uses -- no new query, no new rule, and
 *    nothing on screen changes;
 *  - can never crash: every step is wrapped.
 */
class BackgroundRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val ctx = applicationContext
            val user = NativeSession.current(ctx) ?: return Result.success()
            val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val last = sp.getLong(KEY_LAST, 0L)
            val now = System.currentTimeMillis()

            // 🚨🚨 খাতার সারি B145 (TK, 30.07.2026): আটকে থাকা কাজ পাঠানোটা
            // **এই ১২ মিনিটের বিরতির উপরেও** থাকতে হবে। নিচের বিরতিটা বানানো
            // হয়েছিল **তালিকা নামানোর** খরচ বাঁচাতে, কিন্তু ওটা পাঠানোকেও
            // আটকে দিত — অথচ পাঠানোয় তালিকা খালি থাকলে খরচ শূন্য।
            flushEverythingWaiting(ctx)

            // 🌙🔒 খাতার সারি B151 (TK, 30.07.2026): *"রাত দশটা থেকে সকাল ৬টা
            // পর্যন্ত কেউ অ্যাপসে কাজ করে না।"* — তাই ওই ৮ ঘণ্টা **খবর আনা বন্ধ**,
            // ফ্রি প্ল্যানের খরচ ওই সময়টুকু শূন্য।
            // ⛔ কিন্তু উপরের **পাঠানোটা বন্ধ নয়** — এই ফোনে কিছু আটকে থাকলে সেটা
            //    রাতেও ক্লাউডে চলে যাবে, তাই কোনো তথ্য কখনো হারাবে না।
            if (!LiveRefresh.awake()) return Result.success()

            if (now - last < MIN_GAP_MS) return Result.success()

            // 🚨 TK'S ORDER (28.07.2026, খাতার সারি B41): *"Supabase free plan-এ
            // চলতে হবে এবং অ্যাপ্লিকেশন ফাস্ট চলতে হবে — তার ব্যবস্থা করে রাখবেন।"*
            //
            // **সমস্যা:** ১৫ মিনিট পরপর পুরো তালিকা নামালে ফ্রি প্ল্যানের কোটা
            // দ্রুত শেষ হয়ে যাবে।
            //
            // **সমাধান — আগে সস্তা প্রশ্ন, তারপর দামি কাজ:**
            // প্রথমে শুধু **দুটো ছোট প্রশ্ন** করা হয় — "গতবারের পর কি নতুন কিছু
            // হয়েছে?" এই প্রশ্নে **একটাও সারি নামে না**, শুধু একটা সংখ্যা আসে
            // (HEAD অনুরোধ)। কিছু না বদলালে সেখানেই থেমে যায়।
            // 👉 তাই সারাদিনে বেশিরভাগ দফায় প্রায় **শূন্য খরচ**, আর সত্যিই নতুন
            // কিছু হলে তবেই পুরো তালিকা নামে।
            // ⛔ ফল একই — নতুন কিছু হলে ফোনে সঙ্গে সঙ্গে চলে আসে।
            // 🚨🚨 খাতার সারি B145 (TK, 30.07.2026 — TK নিজে ধরিয়ে দিয়েছেন):
            // **আটকে থাকা কাজ পাঠানো এখন সবার আগে।** আগে পাঠানোর লাইনগুলো
            // নিচের "নতুন কিছু হয়েছে?" প্রশ্নের **পরে** ছিল, আর অন্য কেউ কিছু না
            // বদলালে ওই প্রশ্নেই ফিরে যাওয়া হত — অর্থাৎ **পিছনের পাঠানোটা
            // কোনোদিন চলত না**, এই ফোনে কাজ আটকে থাকলেও। বাঁচার পথ ছিল একটাই:
            // স্টাফ কোনো পর্দা খুললে (`BottomNav`)।
            // ⛔ কোনো নতুন ক্লাউড-কল নয় — তালিকা খালি থাকলে প্রতিটা flush নিজেই
            //    সাথে সাথে ফিরে যায়, তাই খরচ শূন্য।
            //    (উপরে একবার চালানো হয়ে গেছে — এখানে আর দরকার নেই।)

            val since = sp.getString(KEY_SINCE, "") ?: ""
            if (since.isNotBlank()) {
                // 🔒 খাতার সারি B145: `fetchCount` এখন ব্যর্থ হলে -1 ("জানি না")
                // দেয়। দুটোর একটাও -1 হলে আর ঝুঁকি নেওয়া হয় না — আগের মতোই
                // পুরো তালিকা নামিয়ে নেওয়া হয়। আগে ব্যর্থ হলে 0 আসত, তাই
                // "নতুন কিছু নেই" ধরে নেওয়া হত ও তথ্য আটকে থাকত।
                // 🔴💸🔒 V440 (19.08.2026 — Egress অডিটে ধরা পড়া সবচেয়ে বড় ফুটো):
                //   এই সস্তা প্রশ্ন দুটোতে **ব্রাঞ্চের নাম ছিল না**। ফল: কিশানগঞ্জের
                //   একজন স্টাফ একটা ফলোআপ লিখলেই পাঁচ ব্রাঞ্চের **সব ফোনে** দরজা
                //   খুলে যেত, আর প্রত্যেকে নিজের ব্রাঞ্চের পুরো patients + payments +
                //   followups আবার নামাত। পাঁচ ব্রাঞ্চে সারাদিন কাজ চলে বলে দরজাটা
                //   প্রায় প্রতিবারই খুলত (দিনে ~১৬ বার × ১৩ ফোন)।
                //   এখন স্টাফ/ডাক্তারের প্রশ্নে শুধু **নিজের ব্রাঞ্চ** — তাই অন্য
                //   ব্রাঞ্চের কাজে এই ফোন আর অকারণে তালিকা নামাবে না।
                // ⛔ যা এক অক্ষরও বদলায়নি:
                //   · প্যাটার্নটা প্রজেক্টের **প্রমাণিত** `branchScopeFilter()`-এর হুবহু
                //     একই (FollowUpRepository.kt:115-120, B531-এ পরীক্ষিত)।
                //   · `branch.is.null`-ও ধরা আছে — সদ্য তৈরি যে সারিতে branch এখনো
                //     বসেনি, সেটাও গোনায় আসে, তাই কিছু বাদ পড়ার ভয় নেই।
                //   · master / "All" হলে আগের মতোই **সব ব্রাঞ্চ** (কোনো ছাঁকনি নেই)।
                //   · ব্যর্থ হলে আগের মতোই -1 ⇒ সন্দেহ হলে পুরো তালিকা নামে (B145 অটুট)।
                //   · briefings-এ ইচ্ছে করে **হাত দিইনি** — নোটিশ/Approval যেন কোনো
                //     অবস্থাতেই মিস না হয় (নিচে দেখুন)।
                val bScope = try {
                    val bb = user.branch.trim()
                    if (bb.isEmpty() || bb.equals("All", ignoreCase = true)) ""
                    else "&or=(branch.eq." + java.net.URLEncoder.encode(bb, "UTF-8") + ",branch.is.null)"
                } catch (_: Throwable) { "" }
                val a = try { SupabaseClient.fetchCount(
                    "followups", "updatedAt=gt." + java.net.URLEncoder.encode(since, "UTF-8") + bScope
                ) } catch (_: Throwable) { -1 }
                val b = try { SupabaseClient.fetchCount(
                    "payments", "updatedAt=gt." + java.net.URLEncoder.encode(since, "UTF-8") + bScope
                ) } catch (_: Throwable) { -1 }
                // 🔔 V215 (§15, 31.07.2026): briefings-ও এই সস্তা HEAD-count-এ যোগ।
                // নতুন Staff Request / Approval / Correction / Refund Request /
                // Briefing এলে সেটাও "নতুন কিছু হয়েছে" হিসেবে ধরা পড়ে, তাই worker
                // early-return করে না — নিচে ঘন্টার সংখ্যা হিসাব হয়ে সাউন্ড আসে।
                // ⛔ এটাও count-only (একটাও সারি নামে না), Free-plan নিরাপদ।
                val bn = try { SupabaseClient.fetchCount(
                    "briefings", "updatedAt=gt." + java.net.URLEncoder.encode(since, "UTF-8")
                ) } catch (_: Throwable) { -1 }
                val changed = if (a < 0 || b < 0 || bn < 0) -1 else a + b + bn
                if (changed == 0) return Result.success()
            }

            // 🔔 V215 (§15): এতদূর এলে মানে সস্তা check বলছে নতুন কিছু হয়েছে (বা
            // প্রথমবার/অজানা)। এখন এই ব্যবহারকারীর ঘন্টার মোট সংখ্যা হিসাব করে —
            // সংখ্যা **আগের চেয়ে বাড়লে** BellNotifier ফোনের স্বাভাবিক সাউন্ড +
            // ভাইব্রেশনসহ একটা background notification দেয় (App বন্ধ থাকলেও,
            // WorkManager চালায়)। BellNotifier নিজেই de-dup করে — একই request
            // দুবার বাজে না, সংখ্যা কমলে চুপ থাকে। আগে এটা শুধু দিনে ৩ বার
            // (CallReminderWorker) হত; এখন প্রতিটা background refresh-এই near-
            // realtime। ⛔ কোনো নতুন টেবিল/পোলিং নয় — Free-plan মাথায় রেখে শুধু
            // পরিবর্তন ধরা পড়লেই এই হিসাব চলে।
            try {
                val bell = BellCounter.count(ctx, user)
                BellNotifier.onCount(ctx, user, bell)
            } catch (_: Throwable) { }

            val repo = FollowUpRepository(ctx)
            var ok = false
            for (stage in listOf("Inquiry", "Patient", "Treatment")) {
                try {
                    // 🔴🔒 V457 (20.08.2026, TK-অনুমোদিত ছোট কাজ): এই prewarm
                    // প্রতি ৬০ মিনিটে চলে (উপরের MIN_GAP_MS), আর ইতিমধ্যেই
                    // এর আগে একটা সস্তা "কিছু বদলেছে কিনা" চেক পার হয়ে এসেছে।
                    // তাই এখন `fetchTabDelta()` ব্যবহার করা নিরাপদ ও যুক্তিসঙ্গত
                    // — আসল ডেটা টানাও কমবে, অথচ ফোনের local cache আগের মতোই
                    // পুরো/সঠিক থাকে (fetchTabDelta ব্যর্থ/প্রথমবার হলে নিজে
                    // থেকেই পূর্ণ fetchTab()-এ ফেরত যায়)।
                    repo.fetchTabDelta(stage, user.branch, user.name, user.mobile)
                    ok = true
                } catch (_: Throwable) {
                    // one tab failing must not stop the others
                }
            }
            // 🚨 TK'S RULE (2026-07-28): "সে যখন অ্যাপ্লিকেশন চালু করবে, সাথে সাথে
            // যেন তার ফোনে চলে যায় -- এমন না হয় যে খুলে আবার লোডিং করতে সময় নেবে।"
            // So the other daily screens are made ready here too, exactly the
            // same way: each one's OWN normal load is run, which fills that
            // screen's own saved copy on the phone. Nothing new is fetched that
            // the screen would not have fetched itself.
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            // 🔴🔴 V509 (২১.০৮.২০২৬) — **এখানেও হাত দেওয়া হয়নি, ইচ্ছে করেই।**
            //   Egress কমাতে এটাকে "দিনে একবার" করা হয়েছিল (Draft-এর B664-এর
            //   মতো)। যাচাই করে দেখা গেল **এখানে ঐ ধাঁচটা নিরাপদ নয়** —
            //   `loadBoard()`-এর চারটে পড়াই **আজকের তারিখ দিয়ে ছাঁকা**
            //   (`date=eq.$date`), তাই এটা দিনে কয়েক সারির খুব ছোট পড়া;
            //   বাঁচত প্রায় কিছুই।
            //   অথচ ক্ষতি হত সত্যিকারের: `WorkNotebookActivity`-র OUT TIME
            //   পর্দা ও Daily Report এই **জমানো** বোর্ড থেকেই "Today Patient"
            //   ঘরটা আগে থেকে ভরে দেয় (ঐ ফাইলের ১৮৮৫ ও ২১৯৯ নম্বর লাইন,
            //   নেটওয়ার্ক-ফলব্যাক ছাড়া)। দিনে একবার নামালে ঐ জমানো সংখ্যাটা
            //   হত **সকাল ৬-৭টার**, অর্থাৎ প্রায় শূন্য — আর ঘরটা ফাঁকা থাকলে
            //   সাবমিটে `.ifBlank { "0" }` চলে যায়। স্টাফ টের না পেয়েই
            //   **ভুল রোগী-সংখ্যা** পাঠিয়ে দিতেন। তাই আগের মতোই রাখা হলো।
            try { ChamberAttendanceRepository.loadBoard(today, user.branch, ctx) } catch (_: Throwable) {}
            // 🟢 B625 (Egress ফিক্স): background pre-warm ছবি ছাড়া টানে (includePhoto=false) —
            //   ছবি শুধু কেউ Doctor Queue স্ক্রিন খুললে নামবে। এটাই egress-এর মূল সাশ্রয়।
            try { DoctorQueueRepository(ctx).fetchQueue(user.branch, includePhoto = false) } catch (_: Throwable) {}
            // 🟢🔒 B664 (15.08.2026, TK-অনুমোদিত · Egress-৬): Draft (Reject/Delete-এর
            //   তালিকা) আগে থেকে তৈরি রাখাটা **প্রতি দফায়** চলত — অথচ `DraftRepository.load`
            //   একাই **৫টা বড় পড়া** করে (enquiries · followups · patients · payments ·
            //   নিজের সারি), প্রতিটা 5000 পর্যন্ত। এই দফা দিনে ~১৬ বার, প্রতিটা ফোনে —
            //   অথচ Draft পর্দাটা কেউ প্রায় খোলেই না। এটাই ছিল সবচেয়ে বড় "না-দেখা" খরচ।
            //   এখন **দিনে একবার** (এই ফোনে দিনের প্রথম দফায়) আগে থেকে তৈরি রাখা হয়।
            //   ⛔ TK-এর "খুললেই সাথে সাথে দেখাবে" নিয়ম অক্ষত — পর্দাটা খুললে আগের মতোই
            //     জমানো তালিকা **সঙ্গে সঙ্গে** দেখায়, আর তার নিজের তাজা পড়াটাও আগের মতোই
            //     চলে। অর্থাৎ পর্দার আচরণ এক অক্ষরও বদলায়নি।
            //   ⛔ ব্যর্থ হলে তারিখ লেখা হয় না — পরের দফায় আবার চেষ্টা হয়।
            try {
                val draftDay = sp.getString(KEY_DRAFT_DAY, "") ?: ""
                if (draftDay != today) {
                    DraftRepository(ctx).load(user.branch)
                    sp.edit().putString(KEY_DRAFT_DAY, today).apply()
                }
            } catch (_: Throwable) {}
            try { PaymentRepository(ctx).fetchTodayCollection(user.branch) } catch (_: Throwable) {}
            if (ok) {
                // পরেরবার এই সময়ের পরের বদলগুলোই শুধু খোঁজা হবে।
                val stamp = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                        .format(java.util.Date(now - 5L * 60L * 1000L))   // ৫ মিনিট পিছিয়ে, যাতে কিছু বাদ না পড়ে
                } catch (_: Throwable) { "" }
                sp.edit().putLong(KEY_LAST, now).putString(KEY_SINCE, stamp).apply()
            }
        } catch (_: Throwable) {
        }
        return Result.success()
    }

    /**
     * 🚨 খাতার সারি B145 (TK, 30.07.2026): এই ফোনে আটকে থাকা সব কাজ ক্লাউডে
     * পাঠানোর চেষ্টা — ঠিক `BottomNav.retryStuckSaves`-এর সেই একই তালিকা, একই
     * ক্রমে, একই ফাংশনগুলো। নতুন কোনো নিয়ম বানানো হয়নি।
     * ⛔ প্রতিটা তালিকা খালি থাকলে নিজেই সাথে সাথে ফিরে যায়, তাই খরচ শূন্য।
     * ⛔ প্রতিটা ধাপ আলাদা `try`-তে, তাই একটা ব্যর্থ হলে বাকিগুলো থামে না।
     */
    private fun flushEverythingWaiting(ctx: Context) {
        // 🔒 খাতার সারি B169 (TK-এর ৬ নম্বর সন্দেহ): এই কাজটা আগে `BottomNav`-এর
        // তালার কথা জানত না, তাই পর্দা খোলার দফার সঙ্গে **একই সময়ে** চলতে
        // পারত (অকারণ ইন্টারনেট ও ব্যাটারি খরচ)। এখন চারটে জায়গাই একটাই দরজা।
        // ⛔ কেউ ভিতরে থাকলে চুপচাপ ফিরে যায় — ওই দফাটাই তো এই কাজগুলোই করছে।
        SyncGate.tryRun {
        try { CloudWriteQueue.attach(ctx) } catch (_: Throwable) {}
        try { DeletedGuard.syncFromCloud(ctx) } catch (_: Throwable) {}
        try { CloudWriteQueue.flush(ctx) } catch (_: Throwable) {}
        try { EnquiryRepository(ctx).flushPending() } catch (_: Throwable) {}
        try { RegistrationRepository(ctx).flushPending() } catch (_: Throwable) {}
        try { PaymentRepository(ctx).flushPending() } catch (_: Throwable) {}
        try { FollowUpRepository(ctx).flushPending() } catch (_: Throwable) {}
        try { ChamberAttendanceRepository.flushPending(ctx) } catch (_: Throwable) {}
        try { ChamberCloseRepository.flushPending(ctx) } catch (_: Throwable) {}
        try {
            com.tkbiswas.pilesclinic.clinical.ClinicalCloudRepository.flushPending(ctx)
        } catch (_: Throwable) {}
        try { BriefingRepository().flushPending(ctx) } catch (_: Throwable) {}
        try { GenericUpdateQueue.flushPending(ctx) } catch (_: Throwable) {}
        // 🔴🔒 V457 (১৮.০৮.২০২৬) — মোবাইল-বদলের সময় payment খুঁজতে ব্যর্থ
        // হলে (নেট-সমস্যা) এখানে জমা থাকা কাজ আবার চেষ্টা হয়।
        try { MobileChangeSync.flushPending(ctx) } catch (_: Throwable) {}
        }   // 🔒 খাতার সারি B169 — দরজা শেষ
    }

    companion object {
        private const val PREF = "background_refresh_state"
        private const val KEY_LAST = "last_ok"
        /** শেষ সফল দফার সময় — এর পরে কিছু বদলেছে কিনা সেটাই সস্তায় দেখা হয়। */
        private const val KEY_SINCE = "last_seen_stamp"
        /**
         * 🟢🔒 B664 (15.08.2026, TK-অনুমোদিত · Egress-৬): Draft পর্দা কোন দিন শেষবার
         * আগে থেকে তৈরি রাখা হয়েছে (yyyy-MM-dd)। দিনে একবারই — কারণ ওই একটা কাজ
         * ৫টা বড় পড়া করে, অথচ পর্দাটা কেউ প্রায় খোলেই না।
         */
        private const val KEY_DRAFT_DAY = "draft_prewarm_day"
        /**
         * দুটো দফার মধ্যে সবচেয়ে কম ফাঁক।
         * 🚨 TK (28.07.2026, খাতার সারি B41): আগে ছিল **৪৫ মিনিট** — তাই ১৫
         * মিনিট পরপর ডাক পড়লেও কাজ হত ঘণ্টায় একবার। এখন **১২ মিনিট**, যাতে
         * প্রতিটা ডাকেই সত্যিই নতুন তথ্য নামে।
         * ⚠️ এটা Supabase-এর কোটা একটু বেশি খরচ করবে — TK-কে জানানো হয়েছে।
         * দরকার হলে এই একটা সংখ্যা বাড়ালেই আগের মতো হয়ে যাবে।
         */
        // 🟢 B625 (Egress ফিক্স, 11.08.2026): 12 → 30 মিনিট। background pre-warm অর্ধেকেরও
        //   কম বার চলবে, তাই সব background টানা কমবে (স্ক্রিন খুললে/realtime-এ তাজা ডেটা
        //   আগের মতোই আসে, তাই কেউ পার্থক্য টের পাবে না)।
        // 🟢 B632 (Egress ফিক্স, 11.08.2026): 30 → 60 মিনিট। Free-plan egress আরও কমাতে
        //   background pre-warm আরও কম বার চলবে। ⛔ স্ক্রিন খুললে নিজেই তাজা ডেটা টানে +
        //   খোলা থাকলে ৩০ সেকেন্ডে live-refresh — তাই ব্যবহারকারী কোনো পার্থক্য টের পাবে না।
        private const val MIN_GAP_MS = 60L * 60L * 1000L
    }
}
