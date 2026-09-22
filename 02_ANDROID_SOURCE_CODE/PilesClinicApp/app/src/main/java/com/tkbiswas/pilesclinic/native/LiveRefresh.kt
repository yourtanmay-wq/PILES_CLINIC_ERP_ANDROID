package com.tkbiswas.pilesclinic.native

/**
 * 🔔🔒 খাতার সারি B151 (TK, 30.07.2026) — **"রিফ্রেশ না টেনেও নিজে থেকে নতুন হবে"**
 * ══════════════════════════════════════════════════════════════════
 *
 * TK-এর কথা (হুবহু):
 *   *"যে এন্ট্রি করবে তার ফোনে যেন সাথে সাথে দেখায়, বিন্দুমাত্র সময়ও যেন দেরি না
 *    করে... আমার ফোন থেকে কাকে কাকে পাঠানোর কথা সেটা যেন অটোমেটিক ব্যাকগ্রাউন্ডে
 *    হয়ে থাকে... যাদের দেখার কথা তারা যখন অ্যাপ খুলবে তখন দেখালেই হবে... বিশেষ
 *    করে অফিশিয়াল টাইমে রিফ্রেস না টানলেও যেন ডাক্তার চেকআপ · চেম্বার তারিখ ·
 *    ভিজিট সেকশন · পেশেন্ট সেকশন — এ সমস্ত জায়গায় কার্যকরী হয়।"*
 *   *"রাত দশটা থেকে সকাল ৬টা পর্যন্ত কেউ অ্যাপসে কাজ করে না।"*
 *   *"সমস্ত কিছু যাচাই করে করবেন... যে কাজ করলে আমাদের সমস্যার সম্মুখীন হতে হবে না
 *    এবং ফ্রী প্লানেও কাজ চলবে।"*
 *
 * ─── কেন এই ফাইলটা আলাদা ─────────────────────────────────────────
 * চারটে পর্দায় চার রকম নিয়ম লিখলে ভবিষ্যতে একটা বদলালে বাকিগুলো পিছিয়ে থাকত
 * (প্রজেক্টে এই ভুল আগে অনেকবার হয়েছে — খাতার সারি B55 · B60 · B140)। তাই
 * নিয়মটা **এক জায়গায়**, চারটে পর্দা এই একটাই ফাংশন ব্যবহার করে।
 *
 * ─── ফ্রি প্ল্যান কীভাবে বাঁচে (TK-এর প্রধান শর্ত) ─────────────────
 * ৩০ সেকেন্ড পরপর **পুরো তালিকা নামানো হয় না**। শুধু একটা **ছোট প্রশ্ন** যায়:
 *   *"গতবার দেখার পরে এই টেবিলে কিছু বদলেছে? কতটা?"*
 * এই প্রশ্নে **একটাও সারি নামে না** — শুধু একটা সংখ্যা আসে (HEAD অনুরোধ,
 * কয়েকশো বাইট)। উত্তর ০ হলে সেখানেই শেষ। **সংখ্যা ০-র বেশি হলে তবেই** আসল
 * তালিকা নামে — অর্থাৎ যেটুকু এমনিতেই নামানো লাগত।
 * 👉 তাই Follow-up-এ **খরচ আসলে কমছে**: আগে ৩ মিনিট পরপর **পুরো তালিকা** নামত
 *    (কিছু না বদলালেও); এখন শুধু বদলালে নামে।
 *
 * ─── ঘুমের সময় (TK-এর নিয়ম) ──────────────────────────────────────
 * **রাত ১০টা → সকাল ৬টা** কোনো প্রশ্নই যায় না। ওই সময়ে কেউ অ্যাপে কাজ করেন না,
 * তাই খোঁজার কিছু নেই — পুরো ৮ ঘণ্টার খরচ শূন্য।
 *
 * ─── ⛔ যা এই ফাইল কখনো করে না ────────────────────────────────────
 *  · কোনো তথ্য লেখে না, মোছে না, বদলায় না — শুধু **গোনে**।
 *  · উত্তর না এলে (`-1`) **কিছুই করে না** — আগের মতোই চলে।
 *  · প্রথম উত্তরটা শুধু মিলিয়ে রাখে, তাতে কোনো রিফ্রেশ হয় না।
 *  · টাকার হিসাব · ব্রাঞ্চের ছাঁকনি · ডিজাইন — কিছুতেই হাত দেয় না।
 */
/**
 * 🔒 খাতার সারি B171 (TK, 30.07.2026 — TK-এর ৮ নম্বর সন্দেহ) — **"Automatic Refresh সব
 * প্রয়োজনীয় পরিবর্তন দেখছে না"**
 * ══════════════════════════════════════════════════════════════════
 *
 * TK-এর কথা (হুবহু):
 *   *"Chamber screen শুধু Payment table দেখছে, অথচ তথ্য আসে Patient, Enquiry ও
 *    Follow-up থেকেও। Follow-up screen শুধু Follow-up table দেখছে, অথচ Patient,
 *    Payment ও Enquiry-ও প্রয়োজন। একই পরিবর্তনের কারণে প্রায় দুই মিনিট পর্যন্ত
 *    কয়েকবার Full Refresh হতে পারে।"*
 *
 * **দুটো আলাদা দোষ, কোড ধরে দুটোই সত্যি পাওয়া গেছে:**
 *
 * **(১) `stampNow()` সবসময় ২ মিনিট পিছিয়ে দিত — প্রতিটা ট্যাগেই, নতুন করে।**
 * এই ২ মিনিটের পিছিয়ে-যাওয়াটা ধার করা হয়েছিল `BackgroundRefreshWorker`-এর
 * ৫-মিনিট-পিছানো থেকে, কিন্তু ওখানে সেটা নিরাপদ কারণ ওই worker **১২ মিনিটে
 * একবারই** চলে। এখানে `changed()` চলে **৩০ সেকেন্ডে একবার** — তাই ২ মিনিট
 * পিছিয়ে রাখলে একই আসল পরিবর্তন **পরের চার-পাঁচটা ট্যাগেই** আবার "নতুন কিছু
 * বদলেছে" বলে ধরা পড়ত, আর প্রতিবার পুরো Full Refresh চলত — ঠিক TK-এর দেখা
 * উপসর্গ। **সমাধান:** পিছানোর মাপ ২ মিনিট থেকে কমিয়ে **৫ সেকেন্ড** করা হলো
 * (শুধু নেটওয়ার্ক/ঘড়ির সামান্য গরমিল ঢাকতে, বারবার ধরা পড়া বন্ধ করতে)।
 *
 * **(২) এক টেবিল দেখা, অথচ পর্দা কয়েকটা টেবিলের যোগফল দেখায় — সত্যি।**
 * `ChamberAttendanceRepository.loadBoard()` পড়ে **payments · patients ·
 * enquiries · followups** চারটে টেবিল থেকেই (কোড মিলিয়ে দেখা)।
 * `FollowUpRepository.fetchTab()` ট্যাব অনুযায়ী পড়ে **followups** সবসময়, আর
 * Inquiry ট্যাবে **enquiries**, Patient/Treatment ট্যাবে **patients ·
 * payments**-ও। কিন্তু `Watch` আগে **একটাই** টেবিল দেখত — তাই অন্য টেবিলে
 * বদল হলে (যেমন অন্য ফোনে টাকা জমা) এই স্ক্রিন সেটা কখনো জানতই না, শুধু
 * ম্যানুয়াল টেনে রিফ্রেশ করলে বা পর্দা আবার খুললে দেখা যেত।
 * **সমাধান:** `Watch` এখন **একাধিক টেবিল** নিতে পারে (`vararg`), কোনোটায়
 * বদল হলেই পুরো রিফ্রেশ চলে। ⛔ `DoctorQueueActivity`-র নিজের ডেটা শুধু
 * `patients` টেবিল থেকেই আসে (মিলিয়ে দেখা হয়েছে) — তাই ওটা অপরিবর্তিত।
 *
 * ⚡ **খরচ (TK-কে আগেই জানানো হয়েছে, TK "ঝুঁকি ন্যূনতম হলে করুন" বলেছেন):**
 * প্রতিটা টেবিলের জন্য আলাদা HEAD অনুরোধ (একটাও সারি নামে না), তাই Chamber-এ
 * ১টার বদলে ৪টা আর Follow-up-এ ১টার বদলে সর্বোচ্চ ৩টা প্রশ্ন যায় প্রতি ৩০
 * সেকেন্ডে — কিন্তু বদল না থাকলে **আসল তালিকা তখনও নামে না**, তাই সবচেয়ে
 * বেশি খরচ এই ছোট প্রশ্নগুলোতেই, যেগুলো এমনিতেই খুব হালকা।
 */
object LiveRefresh {

    /** কত সময় পরপর ওই ছোট প্রশ্নটা যাবে। ⛔ একটাই সংখ্যা — বড় করলেই ধীর, আর
     *  খুব বড় করলে কার্যত বন্ধ। কিছু ভাঙতে হয় না। */
    const val TICK_MS = 30_000L

    /** ⛔ TK-এর নিয়ম (30.07.2026): কাজ হয় **সকাল ৬টা – রাত ১০টা**। */
    private const val WAKE_HOUR = 6
    private const val SLEEP_HOUR = 22

    /**
     * 🔒 খাতার সারি B171: নেটওয়ার্ক/ঘড়ির সামান্য গরমিল ঢাকতে যতটুকু পিছিয়ে
     * রাখা দরকার, তার বেশি এক সেকেন্ডও নয় — নইলে একই পরিবর্তন বারবার ধরা
     * পড়ে (আগে ছিল ২ মিনিট, যা এই ৩০-সেকেন্ডের টিকে বারবার ধরা পড়ার কারণ
     * ছিল)।
     */
    private const val SAFETY_BACK_MS = 5_000L

    /* 🔴🔒 V1522 (18.09.2026, Supabase Free-plan egress audit): V1519 true
       Realtime চালু হওয়ার পরেও পুরনো 30-second count fallback একই হারে চলছিল।
       Realtime server-confirmed healthy থাকলে ওই fallback এখন 5 মিনিটে একবার
       safety check মাত্র। Socket healthy না থাকলে পুরনো 30-second fallback
       হুবহু আগের মতোই চলে — তাই Realtime ব্যর্থ হলেও freshness হারায় না। */
    private const val REALTIME_SAFETY_POLL_MS = 5L * 60L * 1000L

    /** এখন কি জেগে থাকার সময়? রাত ১০টা – সকাল ৬টা হলে সব বন্ধ। */
    fun awake(): Boolean {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return h >= WAKE_HOUR && h < SLEEP_HOUR
    }

    /** ব্রাঞ্চের নাম ছাঁকনিতে বসানোর নিরাপদ রূপ (নামে ফাঁকা জায়গা থাকে,
     *  যেমন "Cooch Behar" — তাই encode করা বাধ্যতামূলক)। */
    private fun enc(v: String): String =
        try { java.net.URLEncoder.encode(v, "UTF-8") } catch (_: Throwable) { v }

    /**
     * একটা পর্দার জন্য পাহারাদার। প্রতিটা পর্দা নিজের একটা `Watch` রাখে।
     * 🔒 খাতার সারি B171: এখন **একাধিক টেবিল** দেওয়া যায় — পর্দার আসল তথ্য
     * যতগুলো টেবিল থেকে আসে, ততগুলোই এখানে দিতে হবে (আগে ভুলটা এখানেই
     * ছিল — এক টেবিল দেওয়া হত, অথচ পর্দা কয়েকটা টেবিলের যোগফল দেখাত)।
     *
     * ⚠️ `changed()` **কখনো মেইন থ্রেডে ডাকা যাবে না** — এটা ক্লাউডে প্রশ্ন করে।
     */
    class Watch(vararg watchTables: String) {

        // ⛔ হাতে-নিরাপদ ধরন — vararg প্যারামিটার সরাসরি property করার বদলে
        // একটা সাধারণ, নিশ্চিত-ভাবে-কম্পাইল-হওয়া তালিকায় কপি করা হলো।
        private val tables: List<String> = watchTables.toList()

        /** কোন তালিকা দেখা হচ্ছে (তারিখ + ব্রাঞ্চ + ট্যাব) — বদলালে আবার শুরু। */
        private var key: String = ""
        /** সর্বশেষ কোন সময় পর্যন্ত দেখা হয়েছে (UTC, Supabase-এর ধাঁচে)। */
        private var since: String = ""
        /** 🔴 V1360 — একই মুহূর্ত, ফোনের ঘড়িতে (ফোন থেকে লেখা সারির ধাঁচে)। */
        private var sinceLocal: String = ""
        private var lastFallbackPollAt: Long = 0L

        /* 🟢🔒 V1519 — true Realtime is only a wake-up signal. The old 30-second
           count check below stays untouched as fallback. Callback is debounced so a
           save that changes two related rows causes one screen reload, not two. */
        private val realtimeHandler = android.os.Handler(android.os.Looper.getMainLooper())
        private var realtimeToken: Long = 0L
        private var realtimePending: Runnable? = null

        fun startRealtime(
            branchProvider: () -> String? = { null },
            rowFilter: ((RealtimeRefresh.Change) -> Boolean)? = null,
            onChanged: () -> Unit
        ) {
            stopRealtime()
            realtimeToken = RealtimeRefresh.subscribe(tables) { change ->
                val wanted = try { branchProvider()?.trim().orEmpty() } catch (_: Throwable) { "" }
                val got = change.branch?.trim().orEmpty()
                if (wanted.isNotBlank() && !wanted.equals("All", true) &&
                    got.isNotBlank() && !wanted.equals(got, true)
                ) return@subscribe
                if (rowFilter != null && try { !rowFilter(change) } catch (_: Throwable) { true }) return@subscribe

                realtimePending?.let { realtimeHandler.removeCallbacks(it) }
                val task = Runnable {
                    realtimePending = null
                    if (LiveRefresh.awake()) try { onChanged() } catch (_: Throwable) { }
                }
                realtimePending = task
                realtimeHandler.postDelayed(task, 350L)
            }
        }

        fun stopRealtime() {
            realtimePending?.let { realtimeHandler.removeCallbacks(it) }
            realtimePending = null
            if (realtimeToken != 0L) RealtimeRefresh.unsubscribe(realtimeToken)
            realtimeToken = 0L
        }

        /**
         * @return গতবারের পরে সত্যিই কিছু বদলেছে কি না (দেওয়া টেবিলগুলোর
         *         **যেকোনো একটাতে**)। `false` মানে — কিছু বদলায়নি, **অথবা**
         *         উত্তর আসেনি (দুই ক্ষেত্রেই পর্দা আগের মতোই থাকবে, কিছু
         *         ভাঙবে না)।
         */
        fun changed(newKey: String, branch: String?): Boolean {
            try {
                RealtimeRefresh.poke()
                // তালিকা বদলে গেলে (অন্য তারিখ/ব্রাঞ্চ/ট্যাব) নতুন করে শুরু।
                if (key != newKey) {
                    key = newKey
                    since = stampNow()
                    sinceLocal = localStampNow()
                    lastFallbackPollAt = 0L
                    return false
                }
                if (since.isBlank() || sinceLocal.isBlank()) {
                    since = stampNow()
                    sinceLocal = localStampNow()
                    return false
                }
                // V1522: true Realtime is the fast path. While Supabase has
                // acknowledged the socket, keep this old count path only as a
                // 5-minute safety net. If the socket is not healthy, do NOT skip
                // anything — the original 30-second fallback remains intact.
                val pollNow = System.currentTimeMillis()
                if (RealtimeRefresh.healthy() &&
                    lastFallbackPollAt > 0L &&
                    pollNow - lastFallbackPollAt < REALTIME_SAFETY_POLL_MS
                ) return false
                lastFallbackPollAt = pollNow

                val branchPart =
                    if (branch != null && branch.isNotBlank() && branch != "All") {
                        "&branch=eq." + enc(branch)
                    } else {
                        ""
                    }
                // 🔒 খাতার সারি B171: প্রতিটা টেবিল আলাদা করে দেখা হয় — যেকোনো
                // একটায় সত্যিকারের বদল পেলেই পুরো রিফ্রেশ চলবে। কোনো একটা
                // টেবিলের প্রশ্ন ব্যর্থ হলে (-1) শুধু সেটাই বাদ, বাকিগুলো
                // ঠিকঠাক দেখা হয় — একটা টেবিলের নেট-সমস্যায় বাকি টেবিলের
                // আসল বদল যেন হারিয়ে না যায়।
                var anyChanged = false
                var anyKnown = false
                /* 🔴🔒 V1360 (১১.০৯.২০২৬, পুরো প্রজেক্ট যাচাইয়ে ধরা — Payment/Follow-up
                   পর্দা "সেভের পরে ধীর"): ফোন থেকে লেখা সারির `updatedAt` আর এই
                   পাহারাদারের `since` — দুটো **দু'রকম ঘড়িতে** লেখা (ফোনেরটা ভারতের
                   সময়, এখানেরটা UTC — ৫.৫ ঘণ্টা এগিয়ে)। তাই এই ফোনে যেকোনো সেভের
                   পরে **৫.৫ ঘণ্টা ধরে প্রতি ৩০ সেকেন্ডে** "নতুন কিছু বদলেছে" ধরা পড়ত
                   ⇒ প্রতিবার পুরো তালিকা আবার নামত (নেট খরচ + ধীর)।
                   ⇒ এখন দু'রকম সারি দু'রকম মাপে দেখা হয়, একই ছোট প্রশ্নে:
                      · কম্পিউটারের লেখা (UTC): since-এর পরে **কিন্তু এখনকার UTC-র
                        মধ্যে** — ফোনের সারি এই সীমায় পড়েই না (৫.৫ ঘণ্টা এগিয়ে)।
                      · ফোনের লেখা (ভারতের সময়): ফোনের ঘড়ির since-এর পরে।
                   ⛔ একটাও সারি নামে না (আগের মতোই শুধু গোনা) · সময়-ঘরের লেখা
                      কোথাও বদলানো হয়নি · বদল সত্যিই হলে আগের মতোই ধরা পড়ে। */
                val upperUtc = utcStampPlus(60_000L)
                for (table in tables) {
                    val n = try {
                        SupabaseClient.fetchCount(table,
                            "or=(and(updatedAt.gt." + enc(since) + ",updatedAt.lte." + enc(upperUtc) + ")," +
                                "updatedAt.gt." + enc(sinceLocal) + ")" + branchPart)
                    } catch (_: Throwable) { -1 }
                    if (n < 0) continue
                    anyKnown = true
                    if (n > 0) anyChanged = true
                }
                // একটাও টেবিলের উত্তর না এলে (সব -1) — আগের মতোই কিছু করা
                // হয় না, সময়ের হিসাবও পিছিয়ে যায় না।
                if (!anyKnown) return false
                if (!anyChanged) return false
                // সত্যিই বদলেছে — এবার থেকে এই সময়ের পরের বদলগুলোই খোঁজা হবে।
                since = stampNow()
                sinceLocal = localStampNow()
                return true
            } catch (_: Throwable) {
                return false
            }
        }

        /** এখনকার সময়, সামান্য পিছিয়ে — যাতে ঠিক ওই মুহূর্তের কোনো বদল বাদ না
         *  পড়ে। 🔒 খাতার সারি B171: আগে এখানে ২ মিনিট পিছানো হত, যা ৩০-সেকেন্ডের
         *  টিকে **একই বদল বারবার** ধরে ফেলত (TK-এর দেখা "দুই মিনিট পর্যন্ত
         *  কয়েকবার Full Refresh" এখানেই ছিল)। এখন মাত্র ৫ সেকেন্ড — শুধু
         *  ঘড়ি/নেটওয়ার্কের সামান্য গরমিল ঢাকতে যথেষ্ট, বারবার ধরার মতো বড় নয়। */
        private fun stampNow(): String = try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date(System.currentTimeMillis() - SAFETY_BACK_MS))
        } catch (_: Throwable) { "" }

        /** 🔴 V1360 — ফোনের ঘড়িতে একই মুহূর্ত (ফোন থেকে লেখা সারির ধাঁচে; কেবল তুলনার জন্য)। */
        private fun localStampNow(): String = try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(System.currentTimeMillis() - SAFETY_BACK_MS))
        } catch (_: Throwable) { "" }

        /** 🔴 V1360 — কম্পিউটারের (UTC) লেখার উপরের সীমা: এখনকার UTC + সামান্য। */
        private fun utcStampPlus(aheadMs: Long): String = try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date(System.currentTimeMillis() + aheadMs))
        } catch (_: Throwable) { "" }
    }
}
