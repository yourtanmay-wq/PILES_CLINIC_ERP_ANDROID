package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Native rebuild -- Doctor Queue (data access).
 *
 * Reads the same "patients" Supabase table the WebView reads, applies the
 * exact branch-scoping rule the other native screens use (Master / branch=="All"
 * see every branch; a branch staff user sees only their own branch), and then
 * filters/sorts client-side exactly like visitQueueRows() in app.js.
 *
 * Client-side filtering (rather than a complex PostgREST OR filter) is used on
 * purpose so the queue condition stays byte-for-byte identical to the WebView's,
 * which is what keeps the two front-ends showing the same patients.
 */
class DoctorQueueRepository(private val context: Context? = null) {

    companion object {
        private const val PREFS = "doctor_queue_cache"
        // 🔴🔒 V454 (20.08.2026, TK-অনুমোদিত পাইলট): শুধু auto-refresh পথের
        // জন্য delta-fetch-এর state — নিচের fetchQueueDelta() দেখুন।
        private const val DELTA_PREFS = "doctor_queue_delta_state"
        /** নিরাপত্তা-জাল ১: এত সময় পার হলে (বা কোনো since না থাকলে) জোর করে
         *  পূর্ণ-fetch — কিছু ভুলে বাদ পড়লেও নিজে থেকেই ঠিক হয়ে যাবে। */
        /* 🟢🔒 V1275 (০৯.০৯.২০২৬, TK-নির্দেশ) — **২ ঘণ্টা → ৩ ঘণ্টা**, যাতে
           প্রকল্পের চারটে জোর-করা পূর্ণ-পড়ার ঘড়িই এক মাপে থাকে (Follow-up ও
           Dashboard V1258-এ, Chamber V1275-এ — সবই ৩ ঘণ্টা)।
           ⛔ একই কারণ ও একই ঝুঁকি (শুধু hard-DELETE দেরিতে ধরা পড়ে); রোজকার
              বদল আগের মতোই delta-তেই আসে। ⛔ সারি · ছাঁকনি · হিসাব অটুট। */
        /* ⛔🔴 **আমার ভুল সংশোধন — V1277 (০৯.০৯.২০২৬):** V1275-এ Egress কমাতে
           এটা ২ → ৩ ঘণ্টা করেছিলাম। কিন্তু আজ ধরা পড়ল যে **আজকের টাকা-জমা
           রোগীরা কেবল এই পূর্ণ পড়ার পথেই** লাইনে ঢুকতেন ⇒ আমার ওই বদলে TK-র
           অভিযোগটা **আরও খারাপ** হত। উপরে delta পথেও কাজটা বসানো হলো, আর
           সাবধানতার জন্য সময়টাও **২ ঘণ্টায় ফিরিয়ে** আনা হলো।
           ⛔ Follow-up · Dashboard · Chamber-এর ৩ ঘণ্টা অটুট (ওদের এই নির্ভরতা নেই)। */
        private const val FULL_REFRESH_INTERVAL_MS = 2L * 60L * 60L * 1000L   // ২ ঘণ্টা (V1277-এ ফেরানো)
        private const val SAFETY_BACK_MS = 5_000L
    }

    // TK-REQUESTED ADDITION (2026-07-20): "at least show the data that was
    // already on the phone before" -- on a slow connection, the screen used
    // to show nothing but a spinner until the network call finished. Now
    // the last successfully fetched queue for this branch is saved on-device
    // and can be shown INSTANTLY while a fresh fetch happens in the
    // background, same pattern as the app's other silent-refresh screens.
    // This is a read-only display cache -- it never affects what gets
    // fetched/filtered/saved; fetchQueue() below is completely unchanged.
    /* ═══════════════════════════════════════════════════════════════
       🔍🔒 V1108 (০৫.০৯.২০২৬, TK-নির্দেশ: *"সার্চ বক্সে নাম টাইপ করলেই চলে
       আসতে হবে"*) — আজকের লাইনে না থাকলেও রোগী **নিজে থেকেই** উঠে আসে।

       🔴 আগে কী হত: এই ঘরটা শুধু **আজকের তালিকার ভিতরেই** ছাঁকত, তাই
          তালিকার বাইরের রোগী কোনোদিন আসতেন না (TK দুবার বলেছেন — সারি
          ১৭৯ · ২১০)।

       ⛔ **Egress (ফ্রি প্ল্যান) — প্রকল্পের আগে থেকেই TK-অনুমোদিত সেই একই
          সস্তা পথ** (`ChamberAttendanceRepository.searchPatients`-এর হুবহু
          নিয়ম): ছাঁকাটা **সার্ভারেই** হয় (`ilike`), তাই পুরো টেবিল নামে না —
          শুধু মিলে যাওয়া কয়েকটা সারি, আর তাও মাত্র ৭টা দরকারি ঘর।
       ⛔ **অন্তত ৩ অক্ষর** না লিখলে একটাও অনুরোধ যায় না।
       ⛔ ডাক্তার/স্টাফের বাছা ব্রাঞ্চের বাইরে খোঁজা হয় না (পর্দার নিজের নিয়ম)।
       ⛔ নেট খারাপ হলে চুপচাপ ফাঁকা তালিকা — পর্দা আগের মতোই চলে।
       ═══════════════════════════════════════════════════════════════ */
    fun searchAllPatients(query: String, branchFilter: String?): List<QueuePatient> {
        val q = query.trim()
        if (q.length < 3) return emptyList()
        return try {
            val pattern = java.net.URLEncoder.encode("*$q*", "UTF-8")
            val orFilter = "or=(name.ilike.$pattern,mobile.ilike.$pattern,patientId.ilike.$pattern)"
            val allBranch = branchFilter.isNullOrBlank() || branchFilter == "All"
            val filter = if (allBranch) orFilter
                else "branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}&$orFilter"
            val rows = SupabaseClient.fetchListSlim(
                "patients", filter, 25,
                "id,name,mobile,patientId,branch,disease,registrationDate"
            )
            val out = ArrayList<QueuePatient>()
            for (i in 0 until rows.length()) {
                val p = rows.optJSONObject(i) ?: continue
                val id = p.s("id").trim()
                if (id.isBlank()) continue
                out.add(
                    QueuePatient(
                        id = id,
                        patientId = p.s("patientId"),
                        name = p.s("name"),
                        mobile = p.s("mobile"),
                        disease = p.s("disease"),
                        branch = p.s("branch"),
                        photo = "",
                        updatedAt = "",
                        createdAt = "",
                        registrationDate = p.s("registrationDate")
                    )
                )
            }
            out
        } catch (_: Throwable) { emptyList() }   // নেট খারাপ হলে পর্দা আগের মতোই চলে
    }

    fun loadCachedQueue(branchFilter: String?): List<QueuePatient>? {
        val ctx = context ?: return null
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        /* 🔴 V1310 — পুরনো নিয়মে জমানো আজকের তালিকায় ভুল রোগী ঢুকে থাকতে পারে, আর দ্রুত-পড়া (delta)
           সেই তালিকাকেই ভিত্তি ধরে বলে ওঁরা সরতেন না। তাই এই ভার্সনে প্রথমবার: জমানো তালিকা ও
           delta-র "কবে থেকে" চিহ্ন মুছে পুরো নতুন পড়া। একবারই হয়। */
        if (prefs.getInt("_queue_rule_version", 0) < 1310) {
            try {
                prefs.edit().clear().putInt("_queue_rule_version", 1310).apply()
                ctx.getSharedPreferences(DELTA_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Throwable) { }
            return mergeOwnPhonePatients(branchFilter, emptyList()).ifEmpty { null }
        }
        val key = "cache_" + (branchFilter ?: "All")
        /* 📅 V1277 — অন্য দিনের জমানো তালিকা আর দেখানো হয় না (উপরে কারণ লেখা)।
           ⛔ তখনো ফোনের নিজের রেকর্ড থেকে যা পাওয়া যায় তা দেখায় — পর্দা
              কখনো ভুল রোগীর নাম দেখাবে না, বড়জোর একটু ফাঁকা থাকবে। */
        val cacheDay = prefs.getString("cacheDay_" + (branchFilter ?: "All"), "") ?: ""
        if (cacheDay.isNotBlank() && cacheDay != FollowUpModel.today())
            return mergeOwnPhonePatients(branchFilter, emptyList()).ifEmpty { null }
        val json = prefs.getString(key, null)
            // ⚡ জমানো তালিকা না থাকলেও ফোনের নিজের রেকর্ড সঙ্গে সঙ্গে দেখাতে হবে।
            ?: return mergeOwnPhonePatients(branchFilter, emptyList()).ifEmpty { null }
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<QueuePatient>()
            for (i in 0 until arr.length()) list.add(DoctorQueueModel.parse(arr.getJSONObject(i)))
            // ⚡ TK (28.07.2026): নিজের ফোনে করা রেজিস্ট্রেশন সঙ্গে সঙ্গে দেখাতে হবে।
            mergeOwnPhonePatients(branchFilter, list)
        } catch (t: Throwable) { null }
    }

    /**
     * ⚡ TK-এর নির্দেশ (28.07.2026 ২.৩০ pm): **"আমি আমার ফোনে যা যা কাজ করবো,
     * সেটা যেন সাথে সাথেই দেখায়।"**
     *
     * ক্লাউড থেকে আনার পথে ফোনের নিজের রোগী আগেই মেশানো হত, কিন্তু **জমানো
     * তালিকা** দেখানোর পথে হত না — তাই ধীর লাইনে সদ্য রেজিস্টার করা রোগী
     * CHECK-UP Queue-তে দেরিতে আসত। এখন দুই পথেই মেশে।
     *
     * ⛔ কোনো নতুন ক্লাউড-কল নেই · কোনো সারি বাদ যায় না — শুধু যোগ হয়।
     */
    private fun mergeOwnPhonePatients(branchFilter: String?, cached: List<QueuePatient>): List<QueuePatient> {
        val ctx = context ?: return cached
        return try {
            val pending = LocalWorkflowStore(ctx).pendingPatients()
            if (pending.length() == 0) return cached
            val seen = HashSet<String>()
            for (c in cached) if (c.id.isNotBlank()) seen.add(c.id)
            val extra = mutableListOf<QueuePatient>()
            for (i in 0 until pending.length()) {
                val p = pending.optJSONObject(i) ?: continue
                val pid = p.optString("id")
                if (pid.isBlank() || !seen.add(pid)) continue
                val branchOk = branchFilter == null || branchFilter == "All" ||
                    p.s("branch").equals(branchFilter, ignoreCase = true)
                if (!branchOk) continue
                /* 🔴🔴🔒 V1310 (১০.০৯.২০২৬, তালিকা ৪২২ — V1306-এর পরেও TK-র ফোনে সেই ৮ জন):
                   এখানে ফোনের নিজের খাতার সারি **লাইনের নিয়ম না মিলিয়েই** জুড়ে দেওয়া হত —
                   বিল/টাকা বদলের সময় লেখা আধখানা কপি (তারিখ নেই, PENDING চিরকাল) তাই দ্রুত-পড়া
                   ও জমানো-তালিকা — দুই পথেই লাইনে ঢুকে পড়ত (V1306 শুধু পুরো-পড়ার পথ ঠিক করেছিল —
                   রুল ৭ ভাঙা, আমার ভুল)। এখন তিন পথেই একই নিয়ম: `isInQueue` না মিললে জোড়া নয়।
                   ⛔ আজ এই ফোনে রেজিস্টার হওয়া রোগী (queue=true, আজকের তারিখ) আগের মতোই সঙ্গে সঙ্গে থাকেন। */
                if (!DoctorQueueModel.isInQueue(p)) continue
                extra.add(DoctorQueueModel.parse(p))
            }
            if (extra.isEmpty()) cached else extra + cached
        } catch (_: Throwable) { cached }
    }

    private fun saveCachedQueue(branchFilter: String?, queue: List<QueuePatient>, hadPhotos: Boolean = true) {
        val ctx = context ?: return
        try {
            val key = "cache_" + (branchFilter ?: "All")
            // 🟢 B625 (Egress ফিক্স, 11.08.2026): background pre-warm এখন **ছবি ছাড়া** টানে
            // (includePhoto=false) — তাই সেই ডাকে q.photo ফাঁকা আসে। কিন্তু আগের জমানো
            // তালিকায় ছবি থাকলে সেটা যেন **হারিয়ে না যায়** (অফলাইনে ছবি দেখা বন্ধ হত)।
            // তাই ছবি-ছাড়া ডাকে পুরনো ছবি রেখে দিই; আসল স্ক্রিন-ডাক (hadPhotos=true) হলে
            // যা এসেছে তাই লিখি — কেউ ছবি সরালে সেটাও সঠিকভাবে ফাঁকা হয়।
            val prevPhoto = HashMap<String, String>()
            if (!hadPhotos) {
                try {
                    val old = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
                    if (old != null) {
                        val oldArr = JSONArray(old)
                        for (i in 0 until oldArr.length()) {
                            val o = oldArr.getJSONObject(i)
                            val id = o.optString("id")
                            val ph = o.optString("photo", "")
                            if (id.isNotBlank() && ph.isNotBlank()) prevPhoto[id] = ph
                        }
                    }
                } catch (_: Throwable) {}
            }
            val arr = JSONArray()
            for (q in queue) {
                val photo = if (hadPhotos) q.photo
                            else if (q.photo.isNotBlank()) q.photo
                            else (prevPhoto[q.id] ?: "")
                arr.put(
                    org.json.JSONObject()
                        .put("id", q.id).put("patientId", q.patientId).put("name", q.name)
                        .put("mobile", q.mobile).put("disease", q.disease).put("branch", q.branch)
                        .put("photo", photo).put("updatedAt", q.updatedAt).put("createdAt", q.createdAt)
                        .put("bill", q.bill)
                        /* 🩺🔒 V839 — নেট না থাকলেও কার্ডে ট্যাগ ও OLD/NEW যেন
                           থাকে, তাই জমানো তালিকাতেও ঘরগুলো লেখা হয়।
                           ⛔ এটা শুধু ফোনের ভিতরের জমা — ক্লাউডে কিছু যায় না,
                              Egress-এ কোনো প্রভাব নেই। */
                        .put("registrationDate", q.registrationDate)
                        .put("nvpLine", q.nvpLine).put("nvpWhen", q.nvpWhen)
                        .put("nvpBy", q.nvpBy).put("nvpMedicine", q.nvpMedicine)
                        .put("nvpNote", q.nvpNote)
                        .put("nvpItems", q.nvpItems.joinToString(","))
                        /* ✅ V983 — "হয়ে গেছে" ভাগটা নেট ছাড়াও ঠিক থাকে। */
                        .put("done", q.done)
                        // queue/doctorComplete/stage aren't needed back -- isInQueue() isn't
                        // re-applied to cached data, it's only used for the raw fetch above.
                )
            }
            /* 📅🔒 V1277 (০৯.০৯.২০২৬, TK-রিপোর্ট হুবহু — তালিকা সারি ৩৯৯:
               *"Check up ওপেন করার সাথে সাথে এটা আসে — এই সমস্ত পেশেন্ট আজকে
               চেম্বারে কেউ আসে নাই · দু থেকে তিন সেকেন্ডের মধ্যে আবার পরিবর্তন
               হয়ে যায়"*)।
               🔴 **আসল কারণ (কোডে মেপে, দোষটা আমার):** জমানো তালিকার চাবিতে
               **তারিখ ছিল না** (`cache_<branch>`) ⇒ কাল/পরশুর লাইনটাই আজ পর্দা
               খুললেই দেখাত, তারপর ক্লাউডের আসল তালিকা এসে বদলে দিত।
               ⇒ এখন কোন দিনের তালিকা সেটাও লেখা থাকে; অন্য দিনের হলে আর
                 দেখানো হয় না (নিচে `loadCachedQueue`)। */
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(key, arr.toString())
                .putString("cacheDay_" + (branchFilter ?: "All"), FollowUpModel.today())
                .apply()
        } catch (_: Throwable) { }
    }

    fun fetchQueue(branchFilter: String?, includePhoto: Boolean = false): List<QueuePatient> {
        // 🔵🔒 (09.08.2026, TK-নির্দেশ "ডাক্তার রোগী খুঁজে পায় না" ফিক্স): আগে শুধু
        // branch=eq.<branch> ছিল — তাই যে রোগীর ব্রাঞ্চ ঘর এখনো খালি/বসেনি (সদ্য-তৈরি
        // বা পুরনো) সে ডাক্তারের লাইনে আসত না। এখন FollowUpRepository.branchScopeFilter-এর
        // হুবহু প্রমাণিত প্যাটার্ন: or=(branch.eq,branch.is.null) — খালি-ব্রাঞ্চ রোগীও দেখা যায়।
        // ⛔ staff নিজের ব্রাঞ্চ-locked (অন্য ব্রাঞ্চ বাছতেই পারে না), তাই অন্য ব্রাঞ্চ টানার
        // ঝুঁকি নেই; নিচের isInQueue/এক-মোবাইল-এক-কার্ড/সাজানো কিছু বদলায়নি — শুধু ফিল্টার চওড়া।
        val filter = if (branchFilter != null && branchFilter != "All") {
            "or=(branch.eq." + java.net.URLEncoder.encode(branchFilter, "UTF-8") + ",branch.is.null)"
        } else null

        // TK-REQUESTED SAFETY FIX (2026-07-16): this used the SupabaseClient
        // default limit (500), sorted by most-recently-updated. As the total
        // "patients" table grows past 500 rows (very plausible after months
        // of running 5 branches), an older patient still waiting for a
        // doctor checkup but not recently touched could silently fall
        // outside that window and vanish from the queue -- looking exactly
        // like "another staff/doctor can't see an entry". Explicit 5000
        // limit here, matching the same safe limit already used everywhere
        // else in this codebase (Follow-up, Export, Global Search, Backup).
        // TK-REQUESTED FIX (2026-07-23): same root cause/fix as Follow-up's
        // Bill/Due bug -- fetchList() swallows a failed request into a
        // silent empty array, which used to get baked straight into the
        // queue (and then CACHED at the end of fetchQueue()), making the
        // whole queue look wrongly empty on a bad connection, possibly
        // stuck that way. Now uses fetchListOrNull(): a genuine failure
        // (null) falls back to the last cached queue instead of computing/
        // caching a wrong (empty) one; if there's no cache yet, falls
        // through to empty exactly like before (no behavior change for a
        // first-ever load). A real empty result (successful fetch,
        // genuinely nobody waiting) is NOT affected -- only a true
        // failure is.
        // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): same table,
        // same filter, same rows, same limit, same order -- only the columns
        // this queue actually reads are downloaded now. It used to bring all
        // 39 patient columns: the full doctor's note, the medical history, the
        // complaint text, the previous-treatment notes -- none of which this
        // screen ever looks at.
        // ⛔ THE PHOTO STAYS. The queue card shows the patient's photo, so
        // "photo" is deliberately still asked for; dropping it would leave the
        // cards blank.
        // ⛔ NOTHING ELSE CHANGES: the list below is every field
        // DoctorQueueModel.parse and this file read, checked one by one
        // (branch, createdAt, createdBy, disease, doctorComplete, id, mobile,
        // name, patientId, photo, queue, registeredBy, stage, updatedAt), the
        // raw rows are never passed anywhere else, and a failed narrowed read
        // asks for every column again by itself.
        // 🔴 TK-নির্দেশ (04.08.2026): "bill" যোগ করা হলো -- Report Card
        // বোতাম দেখানো/লুকানোর জন্য (bill > 0 মানে Advance/বিল বসেছে,
        // Take Action/Patient Timeline-এর একই প্রমাণিত নিয়ম)। বাকি সব
        // কলাম অপরিবর্তিত।
        // 🟢🔒 B625 (Egress ফিক্স, 11.08.2026, TK-নির্দেশ): background pre-warm এখন
        //   includePhoto=false দিয়ে ডাকে — ছবি (base64) ছাড়া টানে। কারণ background প্রতি
        //   কয়েক মিনিটে প্রতিটা ফোনে এই queue টানত, ফলে **সব রোগীর ছবি বারবার নামত** —
        //   এটাই ছিল Supabase Free-plan egress (14.8GB) শেষ হওয়ার মূল কারণ (DB মাত্র ৮০MB)।
        //   ছবি এখন শুধু তখনই নামে যখন কেউ **সত্যিই Doctor Queue স্ক্রিন খোলে** (includePhoto=
        //   true দিলে) — তবে Doctor Queue screen এখন default slim path ব্যবহার করে; কার্ডের ছবি cache/missing-id batch থেকে পূরণ হয়।
        val cols = if (includePhoto)
            "id,name,mobile,branch,disease,patientId,photo,queue,stage,doctorComplete,createdBy,registeredBy,createdAt,updatedAt,bill,registrationDate,queuedAt"
        else
            "id,name,mobile,branch,disease,patientId,queue,stage,doctorComplete,createdBy,registeredBy,createdAt,updatedAt,bill,registrationDate,queuedAt"
        /* 💸🔒 V1282 (তালিকা সারি ৪০৫, ধাপ ২ক) — ছবি ছাড়া পথে আগে Follow-up-এর
           ভাগাভাগি রোগী-তালিকা (`sharedPatientsOrNull`) — একই ঘর, একই ছাঁকনি, একই
           limit; পাওয়া না গেলে **হুবহু আগের পড়া**। ছবিসহ পথ (includePhoto) অটুট। */
        val shared = if (!includePhoto) try { FollowUpRepository(context).sharedPatientsOrNull(branchFilter) } catch (_: Throwable) { null } else null
        val rowsRaw = shared ?: SupabaseClient.fetchListSlimOrNull("patients", filter, 5000, cols)
        if (rowsRaw == null) loadCachedQueue(branchFilter)?.let { return it }
        val rows = rowsRaw ?: JSONArray()

        // TK-REQUESTED BUG FIX (2026-07-16): same fix as Follow-up -- a
        // just-registered patient could be briefly missing from the queue
        // (delaying the doctor seeing them) because this always read straight
        // from the cloud, with no awareness of a registration still syncing
        // in the background. Any locally-pending patient not yet in the
        // cloud result is now merged in too. Filtering/sorting logic below
        // is completely unchanged.
        val merged = JSONArray()
        for (i in 0 until rows.length()) merged.put(rows.getJSONObject(i))
        context?.let { ctx ->
            try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("patients", rows) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
            val pending = LocalWorkflowStore(ctx).pendingPatients()
            val idPosition = HashMap<String, Int>()
            for (i in 0 until merged.length()) {
                val existingId = merged.getJSONObject(i).optString("id")
                if (existingId.isNotBlank()) idPosition[existingId] = i
            }
            for (i in 0 until pending.length()) {
                val p = pending.getJSONObject(i)
                val pid = p.optString("id")
                val branchOk = branchFilter == null || branchFilter == "All" ||
                    p.s("branch").equals(branchFilter, ignoreCase = true)
                if (!branchOk || pid.isBlank()) continue
                // TK-REPORTED BUG FIX (2026-07-20): same root cause as
                // Follow-up's remark bug (00_PROJECT_STATE_MASTER_NOTE.md
                // section 91) -- a locally-pending edit for a patient whose
                // id is already in the cloud result used to be silently
                // dropped instead of overriding the stale cloud row.
                val existingPos = idPosition[pid]
                if (existingPos != null) {
                    /* 🔴🔴🔒 V1306 (তালিকা ৪২২): ফোনের খাতার কপি দিয়ে ক্লাউডের **পুরো সারি বদলে
                       দেওয়া** হত — কপিটা আধখানা (বিল-বদল/পেমেন্টে শুধু id·name·bill·stage লেখা
                       হয়, তারিখের ঘর নেই) ⇒ queuedAt/registrationDate হারিয়ে "তারিখ জানি না" ⇒
                       পুরনো রোগী লাইনে থেকে যেত। এখন: ক্লাউডের সারিই ভিত্তি; ফোনের লেখা **নতুন
                       হলে তবেই** তার ঘরগুলো উপরে বসে (বাকি ঘর ক্লাউডেরই থাকে); পুরনো হলে ক্লাউডই।
                       V1305-এর (Follow-up) একই নিয়ম — রুল ৭। */
                    val base = merged.getJSONObject(existingPos)
                    val localStamp = p.optString("updatedAt", "")
                    val cloudStamp = base.optString("updatedAt", "")
                    if (localStamp.isNotBlank() && (cloudStamp.isBlank() || localStamp >= cloudStamp)) {
                        val overlaid = JSONObject(base.toString())
                        val keys = p.keys()
                        while (keys.hasNext()) { val k = keys.next(); overlaid.put(k, p.opt(k)) }
                        merged.put(existingPos, overlaid)
                    }
                } else {
                    idPosition[pid] = merged.length()
                    merged.put(p)
                }
            }
        }

        val queue = mutableListOf<QueuePatient>()
        for (i in 0 until merged.length()) {
            val row = merged.getJSONObject(i)
            if (DoctorQueueModel.isInQueue(row)) {
                queue.add(DoctorQueueModel.parse(row))
            }
        }
        // 🚨 TK-REPORTED (28.07.2026, খাতার সারি B30): ভুল করে আগে একই রোগীর
        // একাধিক সারি তৈরি হয়ে গিয়েছিল। নতুন সারি তৈরি হওয়া বন্ধ হয়েছে, কিন্তু
        // পুরনোগুলো ডেটাবেসে রয়ে গেছে — তাই এক মানুষ দু'বার লাইনে উঠতে পারত।
        // ⛔ পুরনো সারি মুছে ফেলা হয়নি — শুধু **এক মোবাইল = এক কার্ড** করে
        // দেখানো হয়, প্রথমটাই রাখা হয় (তালিকা আগে থেকেই নতুন-আগে সাজানো)।
        // মোবাইল না থাকলে আগের মতোই সবই দেখায়, কিছু হারায় না।
        val oncePerMobile = mutableListOf<QueuePatient>()
        val seenMobiles = HashSet<String>()
        for (q in queue) {
            val key = q.mobile.filter { it.isDigit() }.takeLast(10)
            if (key.length == 10 && !seenMobiles.add(key)) continue
            oncePerMobile.add(q)
        }
        val result0 = DoctorQueueModel.sortNewestFirst(oncePerMobile)
        // 🟢🔒 B659 (15.08.2026, TK-অনুমোদিত · Egress-২): ছবি-ছাড়া টানার পরে **পর্দায় যে
        //   তালিকা যায় সেটাতেও** জমানো ছবি বসানো হয়। B632/B625-এ ছবি শুধু জমানো ফাইলে
        //   রাখা হত, ফেরত-দেওয়া তালিকায় নয় — তাই ৩০ সেকেন্ডের অটো-রিফ্রেশ হলেই কার্ড
        //   থেকে ছবি উধাও হয়ে যেত (DoctorQueueAdapter ফাঁকা ছবি পেলে setImageDrawable(null))।
        //   ⛔ ছবিসহ টানার পথ (includePhoto=true) এক অক্ষরও বদলায়নি।
        val result1 = if (includePhoto) result0 else fillPhotosFromCache(branchFilter, result0)
        val result2 = fillNextVisitPlans(result1)   // 🩺 V839
        /* 🩺🔒 V951 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ): TK — *"OLD পেশেন্ট যারা
           ট্রিটমেন্ট করার জন্য টাকা জমা করেছে তাদেরকে কেন দেখাচ্ছে না?"*
           যাচাই করে দেখা গেছে লাইনে ঢোকার নিয়মে **শুধু** নতুন রেজিস্ট্রেশন ও
           NEXT VISIT PLAN ছিল — টাকা জমা দিলে কেউ লাইনে ঢুকত না। এখন ঢুকবে। */
        val result = addTodaysTreatmentPayers(branchFilter, merged, result2)
        saveCachedQueue(branchFilter, result, includePhoto)
        return result
    }

    /**
     * 🩺🔒 V951 — আজ **ট্রিটমেন্টের টাকা** জমা দেওয়া রোগীদের লাইনে আনা, আর
     * কার্ডের জন্য কত তম ভিজিট · আজ জমা · মোট জমা · গত ট্রিটমেন্ট বসানো।
     *
     * ### Egress — কেন এভাবে (আন্দাজে নয়, V839-এর প্রমাণিত ধাঁচেই)
     * প্রথমে **একটাই ছোট অনুরোধ**: আজকের তারিখের (ও এই ব্রাঞ্চের) পেমেন্ট সারি।
     * তাতে যে ক'জন পাওয়া যায় (সাধারণত ৫–২০ জন) **শুধু তাঁদের** জন্য দ্বিতীয়
     * অনুরোধ — `patientId=in.(…)`। সব রোগীর টাকা কখনো নামানো হয় না।
     *
     * ⛔ কেউ ব্যর্থ হলে (null) আগের তালিকাটাই হুবহু ফেরে — লাইন কখনো ফাঁকা হয় না।
     * ⛔ ইতিমধ্যে লাইনে থাকা রোগীর কার্ডও টাকার তথ্য পায়, কিন্তু **দুবার বসে না**।
     * ⛔ চেকআপ হয়ে যাওয়া (doctorComplete) রোগী আগের মতোই বাদ।
     */
    private fun addTodaysTreatmentPayers(
        branchFilter: String?,
        patientRows: JSONArray,
        current: List<QueuePatient>
    ): List<QueuePatient> {
        return try {
            val today = FollowUpModel.today()
            val brPart = if (branchFilter != null && branchFilter != "All")
                "&branch=eq." + java.net.URLEncoder.encode(branchFilter, "UTF-8") else ""
            val todayPays = SupabaseClient.fetchListSlimOrNull(
                "payments", "date=eq.$today$brPart", 2000,
                "id,patientId,mobile,amount,payType,date"
            ) ?: return current

            /* আজ কে কত **চিকিৎসার** টাকা দিলেন (ফি/হাজিরা/বিল-এডিট বাদ — চেম্বার
               বোর্ডের হুবহু একই তালিকা)। */
            fun isMoneyRow(t: String) =
                t != "bill_edit" && t != "chamber_expected" && t != "attendance_mark" &&
                t != "visit_fee" && t != "refund"
            val paidTodayBy = HashMap<String, Double>()
            for (i in 0 until todayPays.length()) {
                val p = todayPays.optJSONObject(i) ?: continue
                val t = p.optString("payType", "").lowercase()
                if (!isMoneyRow(t)) continue
                val amt = p.optDouble("amount", 0.0)
                if (amt <= 0.0) continue
                val owner = p.optString("patientId", "").trim()
                if (owner.isBlank()) continue
                paidTodayBy[owner] = (paidTodayBy[owner] ?: 0.0) + amt
            }
            if (paidTodayBy.isEmpty()) return current

            /* ওই ক'জনের **সব** টাকার সারি — মোট জমা, ভিজিট গোনা ও গত ট্রিটমেন্টের
               জন্য। একটাই অনুরোধ, ২০০ করে ভাগে (URL যেন লম্বা না হয়)। */
            val ids = paidTodayBy.keys.toList()
            val allPays = JSONArray()
            var readOk = true
            for (i in ids.indices step 200) {
                val part = ids.subList(i, minOf(i + 200, ids.size))
                val inList = part.joinToString(",") { "\"" + it.replace("\"", "") + "\"" }
                val got = SupabaseClient.fetchListSlimOrNull(
                    "payments", "patientId=in.($inList)", 5000,
                    "patientId,amount,payType,date,progress,createdAt"
                )
                if (got == null) { readOk = false; break }
                for (j in 0 until got.length()) allPays.put(got.getJSONObject(j))
            }
            if (!readOk) return current

            val paidTotalBy = HashMap<String, Double>()
            val visitDaysBy = HashMap<String, HashSet<String>>()
            val lastTreatBy = HashMap<String, Pair<String, String>>()   // id → (তারিখ, লেখা)
            val lastTreatTimeBy = HashMap<String, String>()              // id → সময় ("3.42 PM")
            for (i in 0 until allPays.length()) {
                val p = allPays.optJSONObject(i) ?: continue
                val owner = p.optString("patientId", "").trim()
                if (owner.isBlank()) continue
                val t = p.optString("payType", "").lowercase()
                val d = p.optString("date", "").take(10)
                if (isMoneyRow(t)) {
                    val amt = p.optDouble("amount", 0.0)
                    if (amt > 0.0) {
                        paidTotalBy[owner] = (paidTotalBy[owner] ?: 0.0) + amt
                    }
                }
                /* 🔢🔒 V976 (০২.০৯.২০২৬, TK-নির্দেশ) — *"চেম্বারে যতবার এসেছেন সব দিন
                   গুনবে; যেদিন ভিজিট ফি দিল সেটা প্রথম দিন"*।
                   ⇒ আগে শুধু **চিকিৎসার** টাকার দিন গোনা হত, তাই যাঁর প্রথম দিনে
                     শুধু ভিজিট ফি ছিল তাঁর একটা দিন বাদ পড়ত (TK-এর ছবিতে MERAJ
                     "1st Visit" দেখাচ্ছিল, হওয়ার কথা ছিল ২য়)।
                   ⛔ টাকার যোগ (`paidTotalBy`) আগের নিয়মেই — ভিজিট ফি সেখানে
                      ঢোকে না, তাই একটা পয়সাও এদিক-ওদিক হয়নি; শুধু **দিন গোনা**
                      বদলেছে। */
                val countsAsVisit = t == "visit_fee" || isMoneyRow(t)
                if (countsAsVisit && d.length == 10 && p.optDouble("amount", 0.0) > 0.0) {
                    visitDaysBy.getOrPut(owner) { HashSet() }.add(d)
                }
                /* গত ট্রিটমেন্ট = **আজকের আগের** সবচেয়ে নতুন দিনের চেম্বার-নোট।
                   🔴🔒 V976 (TK-রিপোর্ট ছবিসহ) — ক্লাউডে ঘরটা খালি থাকলে
                   `optString` **"null" লেখাটাই** ফেরত দিত, আর সেটাই কার্ডে
                   উঠত (TK-এর ছবিতে MERAJ-এর কার্ডে লাল "null")। এখন প্রকল্পের
                   নিজের `s()` দিয়ে পড়া হয় — খালি মানে খালিই।
                   ⛔ TK-নির্দেশ: *"লুকাতে হবে না, ব্লাংক থাকবে"* ⇒ তারিখের
                      বাক্সটা থাকে, শুধু নিচের লাইনটা ফাঁকা। */
                val prog = p.s("progress").trim()
                if (d.length == 10 && d < today) {
                    val prev = lastTreatBy[owner]
                    if (prev == null || d > prev.first) {
                        lastTreatBy[owner] = d to prog
                        // 🕐 V976 (TK-নির্দেশ: *"last treatment date and time লাগবে"*)
                        lastTreatTimeBy[owner] = PaymentModel.displayTime12(p.s("createdAt"))
                    }
                }
            }

            /* 🚨🔒 V1191 (০৭.০৯.২০২৬, TK-রিপোর্ট, হুবহু): *"last treatment
               03/08/2026 : 3.37PM এটা তো বুঝলাম, কিন্তু সেদিন তাকে কি চিকিৎসা করা
               হয়েছিল সেটা লেখা নেই কেন"*।

               🔴 **আসল কারণ (কোডে মেপে) — V1189-এর হুবহু একই দোষ, আরেক জায়গায়:**
                  উপরে "গত ট্রিটমেন্ট"-এর লেখাটা নেওয়া হয় **টাকার সারির**
                  `progress` ঘর থেকে। কিন্তু চেম্বার-বোর্ডে স্টাফ যা লেখেন সেটা
                  জমা হয় **রোগীর ফলো-আপ সারির** `history`-তে (তারিখ সহ)। দুটো
                  আলাদা জায়গা ⇒ ঘরটা ফাঁকাই থাকত।
               ⚠️ **আমার ব্যর্থতা:** V1189-এ Report Card-এ এই দোষটা সারানোর সময়
                  একই ধরনের বাকি জায়গা খুঁজে দেখিনি (নিয়ম ৭ ভেঙেছি)। TK ধরিয়ে
                  দেওয়ার পর এখানেও সারানো হলো।

               ⇒ তাই কুইউয়ের রোগীদের ফলো-আপ থেকে **তারিখ ধরে** চেম্বারের লেখাটা
                 আনা হয়; টাকার সারিতে লেখা থাকলে **সেটাই আগের মতো** থাকে।
               ⛔ একটাই সরু ব্যাচ-পড়া (শুধু `mobile,history`), আর কেবল আজকের
                  কুইউয়ের রোগীদের জন্য — খরচ নগণ্য (নিয়ম ১৩)।
               ⛔ পড়া ব্যর্থ হলে কিছুই ভাঙে না — ঘরটা আগের মতোই ফাঁকা থাকে। */
            val chamberNote = HashMap<String, HashMap<String, String>>()   // mobile10 → (তারিখ → লেখা)
            try {
                val mobs = current.mapNotNull { q ->
                    q.mobile.filter { c -> c.isDigit() }.takeLast(10).takeIf { it.length == 10 }
                }.distinct()
                for (i in mobs.indices step 100) {
                    val part = mobs.subList(i, minOf(i + 100, mobs.size))
                    val inList = part.joinToString(",")
                    val fu = SupabaseClient.fetchListSlimOrNull(
                        "followups", "mobile=in.($inList)", 2000, "mobile,history"
                    ) ?: continue
                    for (j in 0 until fu.length()) {
                        val f = fu.optJSONObject(j) ?: continue
                        val m10 = f.optString("mobile", "").filter { c -> c.isDigit() }.takeLast(10)
                        if (m10.length != 10) continue
                        val hist = f.optJSONArray("history") ?: continue
                        val byDate = chamberNote.getOrPut(m10) { HashMap() }
                        for (k in 0 until hist.length()) {
                            val h = hist.optJSONObject(k) ?: continue
                            val hd = h.optString("date", "").take(10)
                            val hr = h.optString("remark", "").trim()
                            if (hd.isBlank() || hr.isBlank()) continue
                            /* 🏷 V1192 — ফোন-কলের রিমার্ক চিকিৎসার জায়গায় কখনো নয়।
                               ⛔ পুরনো সারিতে চিহ্ন নেই ⇒ আগের মতোই দেখানো হয়। */
                            if (h.optString("src", "").equals("call", true)) continue
                            if (PaymentModel.isAutoPaymentRemark(hr, "")) continue
                            val human = PaymentModel.typedPartOf(hr, "").trim()
                            if (human.isBlank()) continue
                            val prevNote = byDate[hd].orEmpty()
                            byDate[hd] = when {
                                prevNote.isBlank() -> human
                                prevNote.contains(human, ignoreCase = true) -> prevNote
                                else -> prevNote + "  \u00b7  " + human
                            }
                        }
                    }
                }
            } catch (_: Throwable) { }

            /* রোগীর সারিগুলো id ধরে হাতের কাছে (নাম/ছবি/বিল বসানোর জন্য)। */
            val byId = HashMap<String, org.json.JSONObject>()
            for (i in 0 until patientRows.length()) {
                val r = patientRows.optJSONObject(i) ?: continue
                val rid = r.optString("id", "")
                if (rid.isNotBlank()) byId[rid] = r
            }
            /* 💰🔒 V1277 (০৯.০৯.২০২৬, TK-রিপোর্ট — তালিকা সারি ৩৯৯) — delta পথে
               `patientRows`-এ শুধু **বদলে-যাওয়া** সারি থাকে, আর টাকা জমা দিলে
               `patients` সারি বদলায় না (টাকা আলাদা টেবিলে) ⇒ নিচে
               `byId[pid] ?: continue`-তে আজ টাকা দেওয়া রোগী চুপচাপ বাদ পড়তেন।
               ⇒ এখন যাঁদের সারি হাতে নেই **শুধু তাঁদেরই** একবারে আনা হয়।
               ⛔ পূর্ণ পড়ার পথে সবার সারি আগে থেকেই থাকে ⇒ ওখানে একটাও
                  বাড়তি পড়া হয় না (মেপে দেখা)।
               ⛔ ছবি ছাড়া সরু পড়া (`PATIENT_COLS_NO_PHOTO`) — Egress নগণ্য।
               ⛔ ব্যর্থ হলে চুপচাপ এগিয়ে যায়; লাইন কখনো ফাঁকা হয় না। */
            val missing = ids.filter { it.isNotBlank() && !byId.containsKey(it) }
            for (i in missing.indices step 200) {
                val part = missing.subList(i, minOf(i + 200, missing.size))
                val inList = part.joinToString(",") { "\"" + it.replace("\"", "") + "\"" }
                val got = SupabaseClient.fetchListSlimOrNull(
                    "patients", "id=in.($inList)", 500, SupabaseClient.PATIENT_COLS_NO_PHOTO
                ) ?: continue
                for (j in 0 until got.length()) {
                    val r = got.optJSONObject(j) ?: continue
                    val rid = r.optString("id", "")
                    if (rid.isNotBlank()) byId[rid] = r
                }
            }

            /* 💰🔒 V976 (০২.০৯.২০২৬, TK-নির্দেশ) — *"সমস্ত পেশেন্টের ক্ষেত্রে
               দেখাবে"*। আগে **আজ টাকা না দিলে** এই ঘরগুলো একটাও বসত না
               (`?: return q`), তাই বিল · বাকি · ভিজিট-সংখ্যা · গত ট্রিটমেন্ট
               শুধু একজন-দুজনের কার্ডেই দেখা যেত। এখন সবার কার্ডেই বসে;
               আজ টাকা না দিলে "আজ জমা" ₹০। ⛔ টাকার হিসাব এক পয়সাও বদলায়নি। */
            fun decorate(q: QueuePatient): QueuePatient = q.copy(
                visitNo = visitDaysBy[q.id]?.size ?: 0,
                paidToday = paidTodayBy[q.id] ?: 0.0,
                paidTotal = paidTotalBy[q.id] ?: 0.0,
                lastTreatmentDate = lastTreatBy[q.id]?.first ?: "",
                /* 🚨 V1191 — টাকার সারিতে লেখা না থাকলে **ঐ দিনের** চেম্বারের লেখা। */
                lastTreatment = (lastTreatBy[q.id]?.second ?: "").ifBlank {
                    val d = lastTreatBy[q.id]?.first.orEmpty()
                    val m10 = q.mobile.filter { c -> c.isDigit() }.takeLast(10)
                    if (d.isBlank() || m10.length != 10) "" else chamberNote[m10]?.get(d).orEmpty()
                },
                lastTreatmentTime = lastTreatTimeBy[q.id] ?: ""
            )

            val out = current.map { decorate(it) }.toMutableList()
            val already = HashSet(out.map { it.id })
            val seenMobiles = HashSet(out.map { it.mobile.filter { c -> c.isDigit() }.takeLast(10) })
            for (pid in ids) {
                if (pid in already) continue
                val row = byId[pid] ?: continue
                /* ✅🔒 V983 (TK-নির্দেশ) — চেকআপ হয়ে যাওয়া রোগীও আজকের দিনটুকু
                   তালিকায় থাকেন ("DONE TODAY" ভাগে)। কে থাকবেন সেটা এখন
                   একটাই জায়গা ঠিক করে — `DoctorQueueModel.isInQueue()`। */
                if (!DoctorQueueModel.isInQueue(row)) continue
                val mob = row.s("mobile").filter { it.isDigit() }.takeLast(10)
                if (mob.length == 10 && !seenMobiles.add(mob)) continue // এক মোবাইল = এক কার্ড
                out.add(decorate(DoctorQueueModel.parse(row)))
            }
            out
        } catch (_: Throwable) { current }
    }

    /**
     * 🩺🔒 V839 (২৯.০৮.২০২৬, TK-নির্দেশ) — কার্ডে দেখানোর জন্য প্রতিটা
     * সারিতে **শেষ NEXT VISIT PLAN**-টা বসানো।
     *
     * ### 🚨 Egress — কেন এভাবে, আন্দাজে নয়
     * উপরের বড় পড়াটা **ব্রাঞ্চের সব রোগীর** সারি টানে (৫০০০ পর্যন্ত)।
     * সেখানে `nextVisitPlan` ঘরটা যোগ করলে **প্রত্যেক রোগীর পুরো
     * প্ল্যান-তালিকা** নামত — দিনে বহুবার, প্রতিটা ফোনে। সেটা করা হয়নি।
     *
     * বদলে: **ছাঁকনির পরে** যে ক'জন সত্যিই আজ তালিকায় আছেন (সাধারণত ৫–৩০ জন)
     * **শুধু তাঁদের** জন্য একটাই ছোট অনুরোধ — `id=in.(…)&select=id,nextVisitPlan`।
     * প্রকল্পের আগে থেকে থাকা প্রমাণিত ধাঁচ (`DraftRepository`/`ReportsActivity`-র
     * মতো একবারে-সবগুলো পড়া)।
     *
     * ⛔ ব্যর্থ হলে (নেট নেই / ঘরটা এখনো নেই) **তালিকা হুবহু আগের মতোই** ফেরে —
     *    ট্যাগ দেখায় না, কিছুই ভাঙে না।
     * ⛔ একসাথে সর্বোচ্চ ৬০টা id (URL খুব লম্বা হওয়া এড়াতে — `MAX_BATCH`-এর
     *    হুবহু একই সীমা)।
     */
    private fun fillNextVisitPlans(list: List<QueuePatient>): List<QueuePatient> {
        if (list.isEmpty()) return list
        return try {
            val ids = list.map { it.id }.filter { it.isNotBlank() }.distinct().take(60)
            if (ids.isEmpty()) return list
            val filter = "id=in.(" + ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") } + ")"
            val rows = SupabaseClient.fetchListSlimOrNull(
                "patients", filter, ids.size,
                "id," + com.tkbiswas.pilesclinic.clinical.NextVisitPlan.FIELD
            ) ?: return list
            val byId = HashMap<String, com.tkbiswas.pilesclinic.clinical.NextVisitPlan.Entry>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val rid = r.optString("id")
                if (rid.isBlank()) continue
                com.tkbiswas.pilesclinic.clinical.NextVisitPlan.latest(r)?.let { byId[rid] = it }
            }
            if (byId.isEmpty()) return list
            list.map { q ->
                val e = byId[q.id] ?: return@map q
                q.copy(
                    nvpLine = e.shortLine(),
                    nvpWhen = if (e.at.length >= 10) FollowUpModel.displayDate(e.at.take(10)) else "",
                    nvpBy = e.byName,
                    nvpItems = e.items,        // 🩺 V839 — পপ-আপের জন্য, একই পড়া
                    nvpMedicine = e.medicine,
                    nvpNote = e.note
                )
            }
        } catch (_: Throwable) { list }
    }

    /**
     * 🟢🔒 B659 (15.08.2026, TK-অনুমোদিত · Egress-২)
     *
     * ছবি-ছাড়া টানা তালিকার প্রতিটা রোগীর ছবি **ফোনে জমানো তালিকা থেকে** বসিয়ে দেয় —
     * কোনো নতুন ক্লাউড-কল ছাড়াই। ফলে কার্ডে ছবি অটুট থাকে।
     *
     * শুধু যাদের ছবি **কোথাওই নেই** (যেমন এইমাত্র রেজিস্টার হওয়া নতুন রোগী), তাদের
     * ক্ষেত্রে **শুধু ওই id-গুলোর `id,photo`** একবার আনা হয় — গোটা ব্রাঞ্চের সব ছবি নয়।
     * ⛔ ৫০ জন করে ভাগে আনা হয় (B660) — লম্বা URL-এর ঝুঁকি নেই; ৫০০ জনের বেশি হলে থামে।
     * ⛔ এখানে যাদের ছবি লাগে তাঁরা **শুধু লাইনে দাঁড়ানো রোগী** — ব্রাঞ্চের সবাই নয়
     *    (fetchQueue-এ isInQueue ছাঁকার **পরে** এই কাজটা হয়)।
     * ⛔ ব্যর্থ হলে কিছুই ভাঙে না — আগের মতোই ওই কার্ডে ছবি ফাঁকা থাকে, পরের ডাকে চলে আসে।
     */
    private fun fillPhotosFromCache(branchFilter: String?, list: List<QueuePatient>): List<QueuePatient> {
        val ctx = context ?: return list
        if (list.isEmpty()) return list
        return try {
            val map = HashMap<String, String>()
            val cachedStamp = HashMap<String, String>()
            val key = "cache_" + (branchFilter ?: "All")
            val old = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
            if (old != null) {
                val arr = JSONArray(old)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    val ph = o.optString("photo", "")
                    if (id.isNotBlank()) {
                        cachedStamp[id] = o.optString("updatedAt", "")
                        if (ph.isNotBlank()) map[id] = ph
                    }
                }
            }
            // 🔵🔒 EGRESS-SAFE (19.08.2026): পুরো branch-এর photo আর refresh-এ নামবে না।
            // Queue-তে থাকা যে রোগীর photo cache-এ নেই অথবা slim row-এর updatedAt বদলেছে,
            // শুধু তার id,photo আবার আনা হয়। updatedAt বদল মানেই photo বদল নয়, কিন্তু এই
            // ছোট bounded check-টাই অন্য ফোনে বদলানো/মুছে-ফেলা photo stale না রাখার নিরাপদ উপায়।
            // ব্যর্থ হলে পুরনো cache-ই থাকে; সফল response-এ blank photo এলে পুরনো photo সরানো হয়।
            val needPhoto = list.filter {
                it.photo.isBlank() && it.id.isNotBlank() &&
                it.id.matches(Regex("^[A-Za-z0-9_-]+$")) &&
                (map[it.id].isNullOrBlank() || cachedStamp[it.id].orEmpty() != it.updatedAt)
            }.map { it.id }.distinct()
            // ৫০ জন করে ভাগ; সর্বোচ্চ ১০ ভাগ (৫০০ queue patient) — URL/traffic bounded।
            val confirmedBlank = HashSet<String>()
            if (needPhoto.isNotEmpty()) {
                for ((ci, chunk) in needPhoto.chunked(50).withIndex()) {
                    if (ci >= 10) break
                    val extra = SupabaseClient.fetchListSlimOrNull(
                        "patients", "id=in.(" + chunk.joinToString(",") + ")", chunk.size, "id,photo"
                    )
                    if (extra != null) {
                        for (i in 0 until extra.length()) {
                            val o = extra.getJSONObject(i)
                            val id = o.optString("id")
                            val ph = o.optString("photo", "")
                            if (id.isNotBlank()) {
                                if (ph.isNotBlank()) {
                                    map[id] = ph
                                    confirmedBlank.remove(id)
                                } else {
                                    map.remove(id)
                                    confirmedBlank.add(id)
                                }
                            }
                        }
                    }
                }
            }
            // saveCachedQueue(hadPhotos=false) normally preserves an old cached photo.
            // If cloud explicitly confirmed that a changed queue row now has NO photo,
            // clear only that cached photo first so the old image cannot reappear.
            if (confirmedBlank.isNotEmpty() && old != null) {
                try {
                    val arr = JSONArray(old)
                    var touched = false
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        if (confirmedBlank.contains(o.optString("id"))) {
                            o.put("photo", "")
                            touched = true
                        }
                    }
                    if (touched) ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(key, arr.toString()).apply()
                } catch (_: Throwable) { }
            }
            list.map { if (it.photo.isBlank()) it.copy(photo = map[it.id] ?: "") else it }
        } catch (_: Throwable) { list }
    }

    // =========================================================================
    // 🔴🔒 V454 (20.08.2026, TK-নির্দেশ · পাইলট, শুধু Doctor Queue) — "শুধু
    // বদলানো অংশটুকু নামুক।"
    //
    // ⛔ উপরের `fetchQueue()` **এক অক্ষরও বদলানো হয়নি** — স্ক্রিন প্রথম খোলা/
    //    Resume/ব্রাঞ্চ-বদলের সময় এখনও সেটাই চলে (সবচেয়ে নিরাপদ, পূর্ণ তালিকা)।
    //    এই নতুন ফাংশন **শুধু ৩০-সেকেন্ডের auto-refresh পথে** (DoctorQueueActivity
    //    autoCheckForChanges()) ব্যবহারের জন্য — TK-এর অনুমতি নিয়ে, একটা টেস্ট
    //    ফোনে প্রথমে যাচাইয়ের জন্য।
    //
    // কীভাবে নিরাপদ রাখা হয়েছে (৩ স্তরের সুরক্ষা):
    //  ১. since (শেষ কতদূর দেখা হয়েছে) না থাকলে, বা শেষ পূর্ণ-fetch ২ ঘণ্টার
    //     বেশি আগে হলে — জোর করে পুরনো পূর্ণ fetchQueue()-ই চলে (self-heal)।
    //  ২. delta-র ক্লাউড-কল ব্যর্থ হলে (null) সাথে সাথে পূর্ণ fetchQueue()।
    //  ৩. delta-তে পাওয়া প্রতিটা রোগী isInQueue() দিয়ে যাচাই হয় — এখনও লাইনে
    //     থাকলে জমানো তালিকায় বসে/আপডেট হয়, লাইন থেকে বেরিয়ে গেলে (ডাক্তার
    //     দেখে ফেলেছেন) জমানো তালিকা থেকে **সরিয়ে দেওয়া হয়** — তাই "চেক-আপ
    //     হয়ে যাওয়া রোগী তালিকায় থেকে গেছে" এই ভুল হবে না।
    // ⛔ কোনো রেকর্ড ডেটাবেস থেকে সত্যিকারের DELETE হলে (updatedAt বদলায় না)
    //    সেটা delta-তে ধরা পড়বে না — কিন্তু ২ ঘণ্টার নিয়মিত পূর্ণ-fetch সেটাও
    //    নিজে থেকে ঠিক করে দেবে। এই সীমাবদ্ধতা TK-কে সততার সাথে জানানো হয়েছে।
    // =========================================================================
    fun fetchQueueDelta(branchFilter: String?, includePhoto: Boolean = false): List<QueuePatient> {
        val ctx = context ?: return fetchQueue(branchFilter, includePhoto)
        val key = branchFilter?.trim()?.takeIf { it.isNotEmpty() } ?: "All"
        val sp = ctx.getSharedPreferences(DELTA_PREFS, Context.MODE_PRIVATE)
        val since = sp.getString("since_$key", null)
        val lastFullAt = sp.getLong("fullAt_$key", 0L)
        val now = System.currentTimeMillis()

        if (since.isNullOrBlank() || (now - lastFullAt) > FULL_REFRESH_INTERVAL_MS) {
            val result = fetchQueue(branchFilter, includePhoto)
            markFullDone(sp, key, now)
            return result
        }

        val branchPart = if (branchFilter != null && !branchFilter.equals("All", ignoreCase = true))
            "&or=(branch.eq." + java.net.URLEncoder.encode(branchFilter, "UTF-8") + ",branch.is.null)"
        else ""
        val sinceEnc = try { java.net.URLEncoder.encode(since, "UTF-8") } catch (_: Throwable) { since }
        val filter = "updatedAt=gt.$sinceEnc$branchPart"
        val cols = if (includePhoto)
            "id,name,mobile,branch,disease,patientId,photo,queue,stage,doctorComplete,createdBy,registeredBy,createdAt,updatedAt,bill,registrationDate,queuedAt"
        else
            "id,name,mobile,branch,disease,patientId,queue,stage,doctorComplete,createdBy,registeredBy,createdAt,updatedAt,bill,registrationDate,queuedAt"

        val delta = try {
            SupabaseClient.fetchListSlimOrNull("patients", filter, 2000, cols)
        } catch (_: Throwable) { null }

        if (delta == null) {
            // নিরাপত্তা-জাল ২: ক্লাউড-কল ব্যর্থ হলে সাথে সাথে পূর্ণ fetch।
            val result = fetchQueue(branchFilter, includePhoto)
            markFullDone(sp, key, now)
            return result
        }

        // জমানো তালিকার উপর delta বসানো (id ধরে) — id-এর ক্রম রাখা হয় (LinkedHashMap)।
        val byId = LinkedHashMap<String, QueuePatient>()
        for (c in (loadCachedQueue(branchFilter) ?: emptyList())) if (c.id.isNotBlank()) byId[c.id] = c
        for (i in 0 until delta.length()) {
            val row = delta.getJSONObject(i)
            val id = row.optString("id")
            if (id.isBlank()) continue
            if (DoctorQueueModel.isInQueue(row)) byId[id] = DoctorQueueModel.parse(row)
            else byId.remove(id)   // লাইন থেকে বেরিয়ে গেছে (checkup সম্পন্ন ইত্যাদি)
        }

        context?.let { ctx -> try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("patients", delta) } catch (_: Throwable) { } }   // 🔴 V1311 (তালিকা ৪২৩)
        var merged = mergeOwnPhonePatients(branchFilter, byId.values.toList())
        val oncePerMobile = mutableListOf<QueuePatient>()
        val seenMobiles = HashSet<String>()
        for (q in merged) {
            val k = q.mobile.filter { it.isDigit() }.takeLast(10)
            if (k.length == 10 && !seenMobiles.add(k)) continue
            oncePerMobile.add(q)
        }
        val sorted = DoctorQueueModel.sortNewestFirst(oncePerMobile)
        val r1 = if (includePhoto) sorted else fillPhotosFromCache(branchFilter, sorted)
        /* 💰🔒 V1277 (০৯.০৯.২০২৬, TK-রিপোর্ট — তালিকা সারি ৩৯৯: *"যারা আজকে
           পেমেন্ট করেছে তাদের এখানে দেখায় না … আমি তো আপনাকে বলেছিলাম পেমেন্ট
           করলেই চেকআপ পর্দায় তাদেরকে দেখাইতে হবে"*)।
           🔴 **আসল কারণ (কোডে মেপে, দোষটা আমার):** V951-এ (তালিকা সারি ৩৭)
           টাকা-জমা রোগীদের লাইনে আনার কাজটা **শুধু পূর্ণ পড়ার পথে** বসানো
           হয়েছিল; এই delta পথে বসানো হয়নি। টাকা জমা দিলে `patients` সারি
           বদলায় না (টাকা আলাদা টেবিলে) ⇒ delta ওঁদের কোনোদিনই দেখতে পেত না,
           আর পূর্ণ পড়া হত অনেকক্ষণ পরপর। ⇒ এখন **দুই পথেই একই নিয়ম**।
           ⛔ V839-এর NEXT VISIT PLAN-ও একই কারণে বাদ পড়ত, সেটাও বসল (নিয়ম ৭)।
           ⛔ কোনো নতুন নিয়ম নয় — পূর্ণ পথের ঠিক এই দুটো ধাপই এখানে ডাকা হলো,
              তাই দুই পথে দুরকম উত্তর হওয়ার পথ আর নেই (নিয়ম ৭ক-এর ২)। */
        val r2 = fillNextVisitPlans(r1)
        val result = addTodaysTreatmentPayers(branchFilter, delta, r2)
        saveCachedQueue(branchFilter, result, includePhoto)

        sp.edit().putString("since_$key", stampNow()).apply()
        return result
    }

    private fun markFullDone(sp: android.content.SharedPreferences, key: String, now: Long) {
        try {
            sp.edit()
                .putString("since_$key", stampNow())
                .putLong("fullAt_$key", now)
                .apply()
        } catch (_: Throwable) { }
    }

    /** এখনকার সময়, ৫ সেকেন্ড পিছিয়ে (LiveRefresh.kt-এর একই প্রমাণিত মাপ) —
     *  ঘড়ি/নেটওয়ার্কের সামান্য গরমিলে কোনো বদল যেন বাদ না পড়ে। */
    private fun stampNow(): String = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(System.currentTimeMillis() - SAFETY_BACK_MS))
    } catch (_: Throwable) { "" }
}
