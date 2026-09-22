package com.tkbiswas.pilesclinic.native

import org.json.JSONArray

/**
 * Native rebuild -- Briefing / Notice Board (data access).
 *
 * Reads/writes the live Supabase "briefings" table via SupabaseClient, the same
 * table the WebView uses. Listing rules match app.js:
 *  - Master sees every non-deleted briefing (management view).
 *  - Other users see today's briefings that target them and they haven't hidden.
 * (Unlike activeBriefings(), the list here does NOT drop already-seen items, so
 *  staff can still re-read and reply; "seen" is used only for the unread badge.)
 */
class BriefingRepository {

    companion object {
        // TK-REQUESTED FIX (2026-07-19): same shared-lock fix as the other
        // repositories, for consistency (this is a fresh instance per call
        // site too).
        private val LOCK = Any()
    }

    fun fetchForUser(user: NativeUser): List<Briefing> = parseForUser(fetchRaw(), user)

    // TK-REQUESTED (2026-07-24): split out so BriefingActivity can cache the
    // raw rows (same SharedPreferences pattern as Trash/Payment/Doctor
    // Visit) and show them instantly next time -- fetchForUser() above still
    // behaves exactly as before for any other caller.
    fun fetchRaw(): JSONArray {
        // TK-REQUESTED SAFETY FIX (2026-07-16): same fix as Doctor Queue/Visit/
        // Trash -- explicit high limit so older briefings (Master's management
        // view especially) never silently fall outside the default 500-row
        // window.
        // 🔒 V219 (§7, 31.07.2026 — Supabase Free Plan): এই পুরো `briefings` টেবিল
        // Briefing পর্দা খোলা + ঘন্টার গোনা — প্রতিবার নামত। এখন প্রজেক্টের নিজের
        // `CloudReadCache` (২০ সেকেন্ড) দিয়ে মোড়া — একই মুহূর্তের একাধিক ডাক
        // (onCreate + onResume + bell) একটাই download শেয়ার করে, তাই কোটা বাঁচে।
        // ⛔ **কোনো সারি বাদ যায় না** — একই সম্পূর্ণ তালিকাই ফেরে, শুধু ২০ সেকেন্ডে
        //    একবার আনা হয়। ⛔ ফাঁকা/ব্যর্থ ফল **কখনো cache হয় না** (নিচে null
        //    ফেরানো হয়), তাই তালিকা কখনো ভুল করে অসম্পূর্ণ দেখাবে না — তখন সরাসরি
        //    আরেকবার আনা হয়। কোনো নতুন write নেই, শুধু read কম।
        /* 🔵🔒 V515 (২২.০৮.২০২৬, TK-নির্দেশ — Egress অডিট): `fetchList` →
           `fetchListGuarded`। **অনুরোধ হুবহু আগেরটাই** (একই টেবিল, ছাঁকনি নেই,
           limit 5000, সব ঘর) — শুধু এখন V513/V514-এর পাহারার ভিতর দিয়ে যায়,
           তাই টেবিলে কিছু না বদলালে সারিগুলো আর নামে না।
           ⛔ ফেরত আসা তালিকা এক অক্ষরও বদলায়নি — নোটিশের পুরো লেখা ও সব উত্তর
              আগের মতোই আসে, তাই পর্দা কখনো ফাঁকা দেখাবে না।
           ⛔ ব্যর্থ হলে আগের মতোই খালি তালিকা, আর নিচের ২০ সেকেন্ডের
              `CloudReadCache`-এর নিয়ম ("খালি ফল কখনো জমা হয় না") অটুট। */
        val cached = try {
            CloudReadCache.get("briefings:all") {
                val r = SupabaseClient.fetchListGuarded("briefings", BRIEF_FILTER, 5000)
                if (r.length() == 0) null else r
            }
        } catch (_: Throwable) { null }
        return cached ?: SupabaseClient.fetchListGuarded("briefings", BRIEF_FILTER, 5000)
    }

    /* 🌿🔒 V1072 (০৪.০৯.২০২৬, TK-অনুমতি — Egress কমানো) — মুছে ফেলা নোটিশ
       আগে **নামত, তারপর ফোনেই বাদ** যেত (`BriefingModel:154,208`)। এখন
       সার্ভারেই বাদ যায়, তাই ওগুলোর জন্য এক বাইটও খরচ হয় না।
       ⛔ পর্দায় কিছুই বদলায় না — ওই সারিগুলো আগেও দেখা যেত না।
       ⛔ ঘরটা ফাঁকা (`is.null`) হলেও ধরা পড়ে, তাই পুরনো সারি হারায় না। */
    /* ⛔ **দুটোই ধরা হয়** — ঘরটা NULL, আর কোথাও ফাঁকা লেখা (`''`) থাকলেও।
       একটামাত্র রূপ ধরলে ফাঁকা-লেখা কোনো সারি চুপচাপ হারিয়ে যেত। */
    private val BRIEF_FILTER = "or=(deletedAt.is.null,deletedAt.eq.)"

    /**
     * 🟢🔒 V997 (০৩.০৯.২০২৬, TK-নির্দেশ — Egress তালিকার ১ নম্বর) —
     * **নোটিশ বোর্ড খুললে আর সবসময় পুরো টেবিল নামে না।**
     *
     * **আসল কারণ (কোড ধরে, আন্দাজ নয়):** `fetchRaw()` ব্যবহার করে
     * `SupabaseClient.fetchListGuarded()`, যার নামে "পাহারা" থাকলেও ভিতরে
     * (SupabaseClient.kt:870-872) সেটা শুধু `fetchListOrNull(...) ?: JSONArray()` —
     * **কোনো বদল-যাচাই নেই**। তাই পর্দা খুললেই ৫০০০ সারি, নোটিশের পুরো
     * লেখা (`message`) ও সব উত্তর (`replies`) সহ আবার নামত।
     *
     * **এখন:** প্রথমে একটা **অতি-ছোট** পড়া (`fetchListFingerprintOrNull` —
     * শুধু গোনা ও সবচেয়ে নতুন `updatedAt`)। ফোনে জমা কপির সঙ্গে মিলে গেলে
     * **একটা সারিও নামে না**; না মিললে শুধু `updatedAt` বড় সারিগুলো নামে
     * আর জমা কপির উপরে বসে।
     *
     * ⛔ এটা `DoctorVisitRepository.fetchListRawSmartOrNull()`-এর **হুবহু একই
     *    প্রমাণিত ধাঁচ** — নতুন কোনো নিয়ম বানানো হয়নি।
     * ⛔ যেকোনো ধাপ ব্যর্থ হলে (জমা কপি নেই · fingerprint আসেনি · গোনা মেলেনি ·
     *    delta ব্যর্থ) **আগের হুবহু পুরো পড়াটাই** চলে — তাই নোটিশ কখনো
     *    হারাবে না বা অসম্পূর্ণ দেখাবে না।
     * ⛔ সারি যোগ বা মোছা হলে গোনা মেলে না ⇒ পুরোটাই নামে, তাই মুছে যাওয়া
     *    নোটিশ পর্দায় পড়ে থাকতে পারে না।
     * ⛔ ফেরত তালিকার ক্রম আগের মতোই (`updatedAt` নতুন আগে)।
     */
    fun fetchRawSmart(cached: JSONArray?): JSONArray {
        val full = { fetchRaw() }
        if (cached == null || cached.length() == 0) return full()

        val fp = SupabaseClient.fetchListFingerprintOrNull("briefings", null) ?: return full()
        if (fp.first != cached.length()) return full()          // যোগ/মোছা হয়েছে
        if (fp.second.isBlank()) return full()

        var localStamp = ""
        for (i in 0 until cached.length()) {
            val u = cached.optJSONObject(i)?.optString("updatedAt").orEmpty()
            if (u > localStamp) localStamp = u
        }
        if (localStamp.isBlank()) return full()
        if (fp.second == localStamp) return cached              // ✅ কিছুই বদলায়নি

        val enc = java.net.URLEncoder.encode(localStamp, "UTF-8")
        val delta = SupabaseClient.fetchListOrNull(
            "briefings", "updatedAt=gt.$enc", 5000) ?: return full()

        val byId = LinkedHashMap<String, org.json.JSONObject>()
        for (i in 0 until cached.length()) {
            val o = cached.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        for (i in 0 until delta.length()) {
            val o = delta.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        // সার্ভারের ক্রম হুবহু রাখা — নতুন `updatedAt` আগে
        val sorted = byId.values.sortedByDescending { it.optString("updatedAt").orEmpty() }
        val out = JSONArray()
        for (v in sorted) out.put(v)
        return out
    }

    /**
     * 🔵 V405 (16.08.2026, TK-অনুমোদিত — Egress) — **শুধু ঘণ্টার সংখ্যা গোনার
     * জন্য** সরু পড়া। গোনায় নোটিশের **পুরো লেখা (`message`) ও সব উত্তর
     * (`replies`)** কখনোই লাগে না — অথচ `fetchRaw()` সেগুলোসহ পুরো টেবিল নামাত,
     * আর ব্যাকগ্রাউন্ড রিমাইন্ডার সেটাই বারবার ডাকত।
     *
     * নিচের ঘরগুলো `unseenCount()`-এর পথে সত্যিই পড়া হয় — কোড ধরে মিলিয়ে দেখা:
     *   `deletedAt` · `hiddenFor` (isDeletedForMe) · `title` (needsMasterApproval) ·
     *   `date` (আজকের কিনা) · `targets` (targetsHit) · `seen` (hasSeen) ·
     *   `id`/`branch`/`updatedAt` (মেলানো ও সাজানো)।
     * ⛔ `message` ও `replies` — গোনার পথে একবারও ছোঁয়া হয় না, তাই বাদ।
     * ⛔ পর্দার তালিকা (`fetchRaw`) এক অক্ষরও বদলায়নি — সেখানে সব ঘরই আসে।
     * ⛔ সরু পড়া ব্যর্থ হলে `fetchListSlim` নিজেই আগের মতো সব ঘর চেয়ে নেয়
     *    (B446-এর নিয়ম) — তাই সংখ্যা কখনো ভুল করে ০ দেখাবে না।
     * ⛔ একই ২০ সেকেন্ডের `CloudReadCache`, কিন্তু **আলাদা চাবি** — নইলে সরু ফল
     *    পর্দার তালিকায় গিয়ে বসত ও নোটিশের লেখা ফাঁকা দেখাত।
     */
    /* 🌐💰🔒 V492 (20.08.2026, TK-নির্দেশ — Supabase Egress ৯২%): এখন থেকে
       **শুধু বদলে যাওয়া সারিই** নামে (`BriefingCloudCache`), পুরো টেবিল নয়।
       এই ফাংশনটাই সবচেয়ে বেশি চলে — প্রতি ১৫ মিনিটে প্রতিটা ফোনে
       (BackgroundRefreshWorker → BellCounter), তাই এখানেই সবচেয়ে বড় সাশ্রয়।

       ⛔ `ctx` না দিলে আচরণ **হুবহু আগের মতোই** (পুরো পড়া) — তাই পুরনো কোনো
          ডাক ভাঙার সুযোগ নেই।
       ⛔ delta-পড়া ব্যর্থ বা খালি হলেও নিচের পুরনো পথটাই চালু থাকে, তাই
          ঘন্টার সংখ্যা কখনো ভুল করে ০ দেখাবে না।
       ⛔ গোনার নিয়ম (`unseenCount` · `visibleForUser`) এক অক্ষরও বদলায়নি। */
    fun fetchRawForCount(ctx: android.content.Context? = null): JSONArray {
        val cols = BriefingCloudCache.COLS
        if (ctx != null) {
            // ⚠️ `length() > 0` দিয়ে পরীক্ষা করা **চলবে না** — যেদিন সত্যিই
            //    একটাও নোটিশ নেই, সেদিন খালি তালিকাটাই ঠিক উত্তর। তখন যদি
            //    পুরো পড়ায় ফিরে যেতাম, শান্ত দিনে সাশ্রয়টাই উবে যেত।
            //    তাই শুধু `null` (অর্থাৎ পড়াই যায়নি) হলে পুরনো পথ।
            val delta = try { BriefingCloudCache.rowsForCount(ctx) } catch (_: Throwable) { null }
            if (delta != null) return delta
        }
        val cached = try {
            CloudReadCache.get("briefings:count") {
                val r = SupabaseClient.fetchListSlim("briefings", null, 5000, cols)
                if (r.length() == 0) null else r
            }
        } catch (_: Throwable) { null }
        return cached ?: SupabaseClient.fetchListSlim("briefings", null, 5000, cols)
    }

    /**
     * 🔒 খাতার সারি B60 (TK, 29.07.2026 — TK-এর ছবি: ঘন্টায় ৫ দেখাচ্ছে, ভিতরে
     * ফাঁকা): **এক নোটিশ দেখা যাবে কিনা — সেই সিদ্ধান্ত এখন একটাই জায়গায়।**
     *
     * আগে পর্দার তালিকা আর ঘন্টার গোনা **দুই আলাদা নিয়মে** চলত। পর্দা
     * "আমার জন্য মোছা" নোটিশ বাদ দিত, ঘন্টা দিত না — তাই TK নিজের পর্দা থেকে
     * নোটিশ মুছে দিলে সেগুলো তালিকায় আর দেখাত না, অথচ ঘন্টা সেগুলো গুনেই
     * যেত; দেখাই যেত না বলে "দেখা হয়েছে" চিহ্নও বসত না — সংখ্যাটা চিরকাল
     * আটকে থাকত। এখন দুটোই এই একটাই নিয়ম মেনে চলে, তাই আর আলাদা হতে পারবে না।
     */
    fun visibleForUser(row: org.json.JSONObject, user: NativeUser): Boolean {
        if (BriefingModel.isDeletedForMe(row, user.mobile)) return false
        // 🔵 খাতার সারি (TK-নির্দেশ, 09.08.2026): "অনুমতি লাগে এমন" (Refund/Delete/
        // Reopen request) — আগের নিয়মেই থাকল: Master পুরনো হলেও দেখেন (নইলে
        // pending অনুমোদন হারিয়ে যেত)। ⛔ এই শাখা হুবহু আগের আচরণ।
        if (BriefingModel.needsMasterApproval(row.optString("title"))) {
            return user.role == "master" ||
                (row.optString("date") == BriefingModel.today() &&
                    BriefingModel.targetsHit(row, user.mobile, user.role, user.branch))
        }
        // 🔴🔒 V1518 (17.09.2026, TK-নির্দেশ): সব সাধারণ নোটিশ — হাতে লেখা হোক
        // বা New Enquiry / New Registration / Advance-এর auto notice — শুধু
        // নোটিশের নিজের দিনেই দেখা যাবে। রাত ১২টার পর আগের দিনের সাধারণ নোটিশ
        // নিজে থেকেই তালিকা/ঘন্টার গোনা থেকে বাদ যাবে।
        // ⛔ Master approval লাগে এমন Refund/Delete/Reopen/Leave request উপরের
        // শাখায় অপরিবর্তিত: সিদ্ধান্ত না হওয়া পর্যন্ত পুরনো হলেও Master দেখবেন।
        if (row.optString("date") != BriefingModel.today()) return false
        return user.role == "master" ||
            BriefingModel.targetsHit(row, user.mobile, user.role, user.branch)
    }

    /** 🔒 খাতার সারি B60: ঘন্টায় ঠিক ততগুলোই গোনা হয়, পর্দায় যতগুলো দেখা
     *  যাবে ও এখনো "দেখা হয়েছে" বলা হয়নি।
     *
     *  ⚠️ **গোনায় `targetsHit`-ও রাখা হয়েছে — ইচ্ছে করে।** মাস্টার পর্দায় সব
     *  নোটিশই দেখতে পান (নিজের লেখাগুলো সহ), কিন্তু সেগুলো ঘন্টায় গুনলে সংখ্যাটা
     *  হঠাৎ অনেক বড় হয়ে যেত ও অকারণে শব্দ বাজত। তাই ঘন্টা গোনে **কেবল তাঁর
     *  উদ্দেশে পাঠানো, না-মোছা, না-দেখা** নোটিশ — আর এগুলো প্রত্যেকটাই তালিকায়
     *  নিশ্চিতভাবে দেখা যায়। ফলে "সংখ্যা আছে কিন্তু ভিতরে ফাঁকা" আর কখনো হতে
     *  পারবে না, অথচ সংখ্যাটাও অকারণে ফুলে যায় না। */
    fun unseenCount(rows: JSONArray, user: NativeUser): Int {
        var c = 0
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (!visibleForUser(row, user)) continue
            if (!BriefingModel.targetsHit(row, user.mobile, user.role, user.branch)) continue
            if (!BriefingModel.hasSeen(row, user.mobile)) c++
        }
        return c
    }

    /** 🔴🆕🔒 V433 (TK-নির্দেশ ১৮.০৮.২০২৬, স্ক্রিনশটসহ — *"সাধারণ নোটিফিকেশন,
     *  এত হাইলাইট করে দেখানোর কিছু নেই"*): **শুধু ১০-মিনিট-পর-পর আসা
     *  রিমাইন্ডার-অ্যালার্মের** জন্য আলাদা গোনা — "Staff IN TIME"/"Staff OUT
     *  TIME" শুধু তথ্য জানানোর কার্ড, ওর জন্য বারবার অ্যালার্ম বাজার দরকার নেই।
     *
     *  ⛔ উপরের `unseenCount()` (ঘন্টার সংখ্যা · Notifications পাতা) এক অক্ষরও
     *     বদলায়নি — সেখানে আগের মতোই **সব** নোটিশ গোনা হয়, তাই কোনো তথ্য
     *     হারায় না বা লুকিয়ে যায় না; শুধু বারবার বাজাটা বন্ধ হলো।
     *  ⛔ অনুমতি লাগে এমন নোটিশ (Delete/Refund/Reopen অনুরোধ) আগের মতোই গোনা
     *     হয়, তাই সেগুলোর অ্যালার্ম কখনো বন্ধ হবে না। */
    fun unseenCountForReminder(rows: JSONArray, user: NativeUser): Int {
        var c = 0
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (!visibleForUser(row, user)) continue
            if (!BriefingModel.targetsHit(row, user.mobile, user.role, user.branch)) continue
            if (BriefingModel.hasSeen(row, user.mobile)) continue
            val t = row.optString("title")
            if (t.equals("Staff IN TIME", ignoreCase = true) ||
                t.equals("Staff OUT TIME", ignoreCase = true)) continue
            // 🔔🔒 V490 (20.08.2026, TK-নির্দেশ — *"নোটিফিকেশনের জন্য বারবার
            // যেন অ্যালার্ম না বাজে"*): নতুন Enquiry · Registration · Advance-এর
            // স্বয়ংক্রিয় নোটিশ এখন Seen না করা পর্যন্ত ৭ দিন থাকে। এই ১০-মিনিট-
            // পর-পর চলা রিমাইন্ডারে ওগুলো গুনলে সারা দিন-রাত অ্যালার্ম বাজতেই
            // থাকত। তাই ঠিক V433-এর (Staff IN/OUT TIME) একই নিয়মে বাদ।
            // ⛔ ঘন্টার সংখ্যা (unseenCount) অপরিবর্তিত — নোটিশ আসার সময়
            //    BellNotifier একবার শব্দসহ জানায়, তারপর আর নয়।
            if (BriefingModel.isAutoNotice(t)) continue
            c++
        }
        return c
    }

    fun parseForUser(rows: JSONArray, user: NativeUser): List<Briefing> {
        val result = mutableListOf<Briefing>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (visibleForUser(row, user)) result.add(BriefingModel.parse(row))
        }
        return result
    }

    // TK-REQUESTED ADDITION (2026-07-18): the point of a Briefing is that it
    // reaches EVERYONE else -- not a local preview for the Master who wrote
    // it. So this doesn't need the local-cache/merge pattern used for
    // Registration etc.; it just needs to not be silently lost on a weak
    // connection. Queue + retry via BottomNav.wire(), same proven pattern.
    fun post(
        context: android.content.Context,
        title: String, message: String, target: String,
        branch: String, role: String, createdByMobile: String,
        targetMobile: String = "",
        forcedId: String = ""   // 🔁 V1271 — ঐচ্ছিক; না দিলে আগের মতোই এলোমেলো আইডি
    ): Boolean {
        val row = BriefingModel.buildNewBriefing(title, message, target, branch, role, createdByMobile, targetMobile, forcedId)
        val ok = try { SupabaseClient.upsert("briefings", row) } catch (_: Throwable) { false }
        if (!ok) {
            synchronized(LOCK) {
            val prefs = context.getSharedPreferences("piles_clinic_briefing_pending", android.content.Context.MODE_PRIVATE)
            val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
            queue.put(row)
            prefs.edit().putString("queue", queue.toString()).commit()
            }
            // 🚨 TK'S ORDER (27.07.2026): same as the other queues -- ask
            // WorkManager to upload straight away, which keeps working after
            // the staff leaves the app. This queue was the other one still
            // waiting for the next screen-open.
            try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(context) } catch (_: Throwable) { }
        }
        return true // queued if needed -- the notice will reach everyone once synced
    }

    /** Retries every Briefing still stuck on this device. Called from
     *  BottomNav.wire() on every screen open, same as everything else. */
    fun flushPending(context: android.content.Context) {
        synchronized(LOCK) {
        val prefs = context.getSharedPreferences("piles_clinic_briefing_pending", android.content.Context.MODE_PRIVATE)
        val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        if (queue.length() == 0) return
        val stillPending = org.json.JSONArray()
        for (i in 0 until queue.length()) {
            val row = queue.optJSONObject(i) ?: continue
            // TK-REQUESTED (2026-07-26): never re-create a deleted briefing.
            if (DeletedGuard.isDeleted("briefings", row.optString("id", ""), context)) continue
            val ok = try { SupabaseClient.upsert("briefings", row) } catch (_: Throwable) { false }
            if (!ok) stillPending.put(row)
        }
        prefs.edit().putString("queue", stillPending.toString()).commit()
        }
    }

    /** Old signature, kept in case anything else references it. */
    fun post(
        title: String, message: String, target: String,
        branch: String, role: String, createdByMobile: String,
        targetMobile: String = "",
        forcedId: String = ""   // 🔁 V1271
    ): Boolean {
        val row = BriefingModel.buildNewBriefing(title, message, target, branch, role, createdByMobile, targetMobile, forcedId)
        return SupabaseClient.upsert("briefings", row)
    }

    /**
     * ⚡🔒 V756 (২৭.০৮.২০২৬) — **আগে থেকে জানা সারি দিয়ে "seen" লেখা।**
     *
     * ⚠️ কেন লাগল: `markSeen()` প্রতিবার আগে সারিটা **মেঘ থেকে টেনে আনে**,
     *    তারপর লেখে — অর্থাৎ প্রতি নোটিশে **দুটো** নেট-কল। V753-এ মাস্টারের
     *    জন্য সব নোটিশ "seen" হতে শুরু করায় এটা বিপজ্জনক হয়ে দাঁড়াত
     *    (Supabase free প্ল্যানে অকারণ খরচ)। নিজের কাজ যাচাই করতে গিয়ে ধরা।
     *
     * এখানে সারিটা **হাতেই আছে** (পর্দা আঁকার জন্য যেটা আনা হয়েছে), তাই
     * টানার দরকারই নেই ⇒ **অর্ধেক নেট-কল**।
     * ⛔ যা লেখা হয় তা হুবহু একই (`buildSeenUpdate`), তাই ফল অপরিবর্তিত।
     */
    fun markSeenWithRow(id: String, existingSeen: JSONArray, userMobile: String): Boolean {
        if (id.isBlank()) return false
        return try {
            SupabaseClient.updateById(
                "briefings", id, BriefingModel.buildSeenUpdate(existingSeen, userMobile))
        } catch (_: Throwable) { false }
    }

    fun markSeen(id: String, userMobile: String): Boolean {
        val existing = SupabaseClient.fetchList("briefings", "id=eq.$id", 1)
        if (existing.length() == 0) return false
        val current = existing.getJSONObject(0).optJSONArray("seen") ?: JSONArray()
        // Skip if already seen, to avoid duplicate entries.
        for (i in 0 until current.length()) {
            if (BriefingModel.mob(current.optString(i)) == BriefingModel.mob(userMobile)) return true
        }
        val fields = BriefingModel.buildSeenUpdate(current, userMobile)
        return SupabaseClient.updateById("briefings", id, fields)
    }

    /**
     * 🟢🔒 V642 (২৪.০৮.২০২৬, TK-নির্দেশ — "Reply করলে সে যেন Notification
     * পায়") — **আসল কারণ:** রিপ্লাই এতদিন শুধু এই নোটিশের নিজের `replies`
     * ঘরে জমা হতো — যিনি আসল অনুরোধ পাঠিয়েছিলেন (যেমন Uttama), তাঁর কাছে
     * নতুন কোনো ঘন্টা-নোটিশ যেত না; নিজে থেকে আবার এই নোটিশটা না খুললে
     * রিপ্লাই দেখতেই পেতেন না।
     * **সমাধান:** রিপ্লাই সফল হলে, যদি রিপ্লাইকারী **আসল অনুরোধকারী নিজে
     * না হন** (`createdBy` ভিন্ন), তাহলে তাঁকে (createdBy মোবাইলে) একটা
     * নতুন, ছোট, ব্যক্তিগত (individual-target) ঘন্টা-নোটিশ যায় — "💬 Reply
     * on: <শিরোনাম>" — তাতে আসল রিপ্লাই টেক্সটটাও থাকে। এই একই ফাংশন
     * প্রজেক্টের **সাতটা জায়গা থেকেই** ডাকা হয় (Approve/Reject/Reopen
     * বোতাম-সহ), তাই সব জায়গাতেই এই সুবিধা একসাথে চালু হলো — আলাদা করে
     * প্রতিটা কল-সাইট বদলাতে হয়নি।
     * ⛔ রিপ্লাই সেভের পুরনো নিয়ম এক অক্ষরও বদলায়নি — নোটিফিকেশন-পাঠানো
     *    ব্যর্থ হলেও (যেমন `context` না থাকলে) রিপ্লাই সেভ হওয়া আটকায় না।
     * ⛔ রিপ্লাইকারী নিজেই আসল অনুরোধকারী হলে (নিজের লেখায় নিজে রিপ্লাই)
     *    কোনো বাড়তি নোটিশ যায় না — নিজেকে নিজে জানানোর দরকার নেই।
     */
    fun addReply(context: android.content.Context?, id: String, text: String, userMobile: String): Boolean {
        val existing = SupabaseClient.fetchList("briefings", "id=eq.$id", 1)
        if (existing.length() == 0) return false
        val row = existing.getJSONObject(0)
        val current = row.optJSONArray("replies") ?: JSONArray()
        val fields = BriefingModel.buildReplyUpdate(current, text, userMobile)
        val ok = SupabaseClient.updateById("briefings", id, fields)
        if (ok && context != null) {
            try {
                val creator = row.optString("createdBy", "")
                if (BriefingModel.mob(creator).isNotBlank() && BriefingModel.mob(creator) != BriefingModel.mob(userMobile)) {
                    val title = row.optString("title", "Notice")
                    val branch = row.optString("branch", "")
                    post(context, "\uD83D\uDCAC Reply on: $title", text, "individual", branch, "", userMobile, creator)
                }
            } catch (_: Throwable) { /* নোটিফিকেশন ব্যর্থ হলেও রিপ্লাই সেভ অক্ষত */ }
        }
        return ok
    }

    /** Master deletes for everyone; a non-master only hides it for themselves,
     * matching deleteBriefing(). */
    /**
     * 🔴🔴🔒 V666 (২৫.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "অনুমতি দেওয়ার পরেও কেন
     * থেকে যাচ্ছে?") — **আসল কারণ (কোড ধরে যাচাই):** এই ফাংশনের ফলাফল
     * (সফল/ব্যর্থ) কোথাও যাচাই হতো না — নেটওয়ার্ক সমস্যায় ক্লাউডে ডিলিট-চিহ্ন
     * বসাতে ব্যর্থ হলেও কোড ধরে নিত সব ঠিক আছে (`try { ... } catch { }`
     * দিয়ে ফলাফল উপেক্ষা করা হতো)। "✅ Approved" রিপ্লাই ঠিকই যোগ হতো
     * (আলাদা, সফল কল), কিন্তু আসল ডিলিট-চিহ্নটা কখনো বসত না — তাই পরের
     * লোডে নোটিশ আবার ফিরে আসত।
     * **সমাধান:** ব্যর্থ হলে এখন প্রমাণিত `GenericUpdateQueue`-তে জমা
     * থাকে (context দেওয়া থাকলে), যতক্ষণ না সত্যিই ক্লাউডে বসে —
     * অ্যাপের অন্য সব জায়গার (Payment/Trash ইত্যাদি) একই প্রমাণিত
     * রিট্রাই-প্যাটার্ন। ⛔ `context` না দিলে (পুরনো caller) আগের মতোই
     * আচরণ — নতুন কোনো ঝুঁকি নেই।
     */
    fun deleteOrHide(id: String, user: NativeUser, context: android.content.Context? = null): Boolean {
        // Master: আগের মতোই global delete (কোনো read নেই — অপরিবর্তিত)।
        if (user.role == "master") {
            val fields = BriefingModel.buildMasterDelete(user.mobile)
            val ok = SupabaseClient.updateById("briefings", id, fields)
            if (!ok) context?.let { GenericUpdateQueue.queue(it, "briefings", id, fields) }
            return ok
        }
        // 🔵 TK-ORDER (07.08.2026): non-master hide — existing row পড়া **ব্যর্থ** হলে
        // আন্দাজে খালি hiddenFor দিয়ে PATCH করব না (আগে করত → অন্যদের hide মুছে
        // যেত)। fetchListOrNull; ব্যর্থ/সারি-নেই হলে false। ⛔ একই একটাই cloud-read।
        val existing = SupabaseClient.fetchListOrNull("briefings", "id=eq.$id", 1) ?: return false
        if (existing.length() == 0) return false
        val hidden = existing.getJSONObject(0).optJSONArray("hiddenFor") ?: JSONArray()
        val fields = BriefingModel.buildHideForUser(hidden, user.mobile)
        val ok = SupabaseClient.updateById("briefings", id, fields)
        if (!ok) context?.let { GenericUpdateQueue.queue(it, "briefings", id, fields) }
        return ok
    }

    /**
     * 🔒 TK-ORDER (2026-08-06): hide a briefing from ONLY this person's list
     * (per-user hide, NEVER a global delete) — used to auto-clear an ordinary
     * notice from someone's board once they press "Seen".
     *
     * Unlike deleteOrHide(), this is the SAME per-user hide for everyone,
     * including Master — so one person seeing a notice can never remove it from
     * anyone else's board. Others keep seeing it until they too press Seen; once
     * everyone has, it naturally disappears for all. Approval/permission
     * notices (Refund / Delete / Chamber-reopen request) are excluded by the
     * caller and are never auto-hidden.
     */
    fun hideForMe(id: String, userMobile: String): Boolean {
        // 🔵 TK-ORDER (07.08.2026): existing row পড়া **ব্যর্থ** হলে আর আন্দাজে খালি
        // hiddenFor দিয়ে PATCH করব না। আগে fetchList() ব্যর্থে খালি ফেরাত → hiddenFor
        // শুধু এই ব্যবহারকারী দিয়ে বসিয়ে দিত → **অন্যদের আগের hide মুছে যেত** (তাদের
        // লুকোনো নোটিশ ফিরে আসত)। এখন fetchListOrNull; ব্যর্থ/সারি-নেই হলে false
        // (কিছু লেখা হয় না, পরে আবার চেষ্টা হবে)। ⛔ একই একটাই cloud-read।
        val existing = SupabaseClient.fetchListOrNull("briefings", "id=eq.$id", 1) ?: return false
        if (existing.length() == 0) return false
        val hidden = existing.getJSONObject(0).optJSONArray("hiddenFor") ?: JSONArray()
        val fields = BriefingModel.buildHideForUser(hidden, userMobile)
        return SupabaseClient.updateById("briefings", id, fields)
    }
}
