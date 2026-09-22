package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject

/**
 * 🔒🔒 খাতার সারি B172 (TK, 30.07.2026) — Enquiry/Visit/Patient কার্ডে রোগীর
 * ঠিকানার একটা ছোট ট্যাগ (গ্রাম/পোস্ট/থানা/জেলা থেকে বেছে নেওয়া, বা স্টাফ
 * নিজে লেখা) — **এক পেশেন্টের জন্য সব কার্ডে এক থাকবে, মোবাইল ধরে।**
 *
 * TK-এর কথা (হুবহু): *"পেশেন্টের ছোটখাট ঠিকানা যোগ হবে, যেটা স্টাফ চাইলে
 * যেকোনো সময় এডিট করতে পারে... গ্রাম চাঁদা, পোস্ট পাঞ্চিতা, থানা বনগাঁ,
 * জেলা উত্তর ২৪ পরগনা — শুধুমাত্র বনগাঁ লেখাটা যেন আমার কার্ডে থাকে।"*
 * এবং: *"প্রথমবার রেজিস্ট্রেশনের ঠিকানা থেকে auto বসবে"* · *"এক জায়গায়
 * বদলালে সব জায়গায় বদলাবে।"*
 *
 * ── কেন এটা আলাদা টেবিলে (নতুন `address_tags`, patients/enquiries/
 *    followups-এ হাত পড়েনি) ──
 * পুরনো টেবিলে ঘর যোগ করলে তিনটে জায়গায় (enquiries · patients · followups)
 * একসঙ্গে সিঙ্ক রাখতে হত। এখানে **মোবাইল ধরে একটাই সারি** — তাই Enquiry
 * কার্ড, Visit কার্ড, Patient কার্ড — কোনটা থেকে বদলানো হোক না কেন, একটাই
 * জায়গায় লেখা হয়, তাই তিন কার্ডেই এক দেখাবে (TK-এর ঠিক এই শর্তটাই)।
 *
 * ── "auto" ঠিকানা কীভাবে আসে (⚡ বাড়তি ক্লাউড-কল ছাড়াই) ──
 * `patients`/`followups`-এর নিজের `address` ঘরেই "Vill: Chada, PO: Panchita,
 * PS: Bongaon, Dist: North 24 Parganas" — এই combined লেখাটা আগে থেকেই আছে
 * (PatientModel.buildAddress()) । স্টাফ নিজে কখনো এখানে কিছু না বসালে, এই
 * লেখাটা থেকে **থানা (PS)** অংশটা বেছে **শুধু পর্দায় দেখানো হয়** — এটা
 * কোনো ক্লাউড-কল নয়, যে তথ্য এমনিতেই তালিকার সঙ্গে নেমেছে তারই একটা
 * টুকরো পড়া। ⛔ **নিজে থেকে `address_tags`-এ কিছু লিখে রাখা হয় না** — স্টাফ
 * ট্যাপ করে "Save" না চাপা পর্যন্ত ওই টেবিলে কোনো সারি তৈরি হয় না, তাই এই
 * ফিচার চালু হওয়ার সঙ্গে সঙ্গে হাজার হাজার সারির জন্য হুট করে লেখার হুড়োহুড়ি
 * (bulk-write) হয় না — Supabase-এর কোটার উপর কোনো চাপ নেই।
 * ⛔ Enquiry-স্তরে (এখনো রেজিস্ট্রেশন হয়নি) কোনো গ্রাম/পোস্ট/থানা/জেলা আলাদা
 *    করে থাকেই না (শুধু একটা মুক্ত-লেখা address) — তাই ওই কার্ডে auto কিছু
 *    বসে না, স্টাফ চাইলে নিজে হাতে লিখতে পারবেন (এডিট পপ-আপে "নিজে লিখুন")।
 */
object AddressTagRepository {

    private const val TABLE = "address_tags"

    /** মোবাইলের শেষ ১০ সংখ্যা — এই টেবিলের আইডি ও ব্যাচ-খোঁজার চাবি। */
    fun keyFor(mobile: String): String =
        mobile.filter { it.isDigit() }.takeLast(10)

    /**
     * "Vill: Chada, PO: Panchita, PS: Bongaon, Dist: North 24 Parganas, PIN: 743235"
     * — এই combined লেখাটা টুকরো টুকরো করে {ঘর-নাম -> মান} বানানো।
     * `PatientModel.buildAddress()`-এর ঠিক উল্টো কাজ। ⛔ ফাঁকা/না-মেলা অংশ
     * বাদ যায়, কিছু ভাঙে না।
     */
    fun parseAddress(address: String): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        if (address.isBlank()) return out
        for (chunk in address.split(",")) {
            val piece = chunk.trim()
            val idx = piece.indexOf(":")
            if (idx <= 0) continue
            val label = piece.substring(0, idx).trim()
            val value = piece.substring(idx + 1).trim()
            if (label.isBlank() || value.isBlank()) continue
            val key = when (label.uppercase()) {
                "VILL", "VILLAGE" -> "VILLAGE"
                "PO", "POST" -> "POST"
                "PS", "THANA" -> "THANA"
                "DIST", "DISTRICT" -> "DISTRICT"
                "PIN" -> "PIN"
                else -> null
            } ?: continue
            out[key] = value
        }
        return out
    }

    /** পর্দায় দেখানোর ডিফল্ট — TK-এর সিদ্ধান্ত: **থানা**। না থাকলে যা আছে
     *  তার প্রথমটা (গ্রাম/পোস্ট/জেলা), সবই ফাঁকা হলে খালি স্ট্রিং।
     *
     * 🔴🔒 V680 (২৫.০৮.২০২৬, TK-লাইভ-টেস্ট রিপোর্ট — "Follow-up কার্ডে
     * JALPAIGURI | PILES-এর পাশে ঠিকানা কেন নেই") — আসল কারণ: Enquiry-র
     * ঠিকানা প্রায়ই সাধারণ এক-লাইনের লেখা (যেমন "RANI HAAT"), কোনো
     * "Vill: X, PO: Y" গঠন থাকে না — তাই `parseAddress()` কিছুই খুঁজে
     * পেত না, ট্যাগ ফাঁকা থেকে যেত (অথচ Timeline-এর ঠিকানা ঠিকই দেখাত,
     * কারণ ওখানে raw address সরাসরি দেখানো হয়, parse করা হয় না)।
     * সমাধান: গঠন-করা ঠিকানা না পাওয়া গেলে, raw ঠিকানাটাই (ছোট করে,
     * প্রথম অংশ) ট্যাগ হিসেবে ব্যবহার হয় — তাই কখনো নীরবে হারায় না। */
    fun defaultTagFromAddress(address: String): String {
        val parts = parseAddress(address)
        val structured = parts["THANA"] ?: parts["VILLAGE"] ?: parts["POST"] ?: parts["DISTRICT"]
        if (structured != null) return structured
        val raw = address.trim()
        if (raw.isBlank()) return ""
        // কমা দিয়ে ভাগ করা থাকলে প্রথম অংশটাই যথেষ্ট (ছোট, পরিষ্কার ট্যাগ)।
        return raw.split(",").first().trim()
    }

    /**
     * একাধিক মোবাইলের জন্য একসাথে সেভ-করা ট্যাগ আনা (একটাই ছোট অনুরোধ)।
     * ⛔ ব্যর্থ হলে খালি ম্যাপ ফেরত — তখন প্রতিটা কার্ড নিজের auto-ডিফল্টে
     *    ফিরে যাবে, কিছু ভাঙে না।
     */
    fun fetchSavedTags(mobiles: List<String>): Map<String, String> {
        val keys = mobiles.map { keyFor(it) }.filter { it.length == 10 }.distinct()
        if (keys.isEmpty()) return emptyMap()
        return try {
            val filter = "id=in.(" + keys.joinToString(",") + ")"
            val rows = SupabaseClient.fetchListSlimOrNull(TABLE, filter, keys.size, "id,value") ?: return emptyMap()
            val out = HashMap<String, String>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val id = row.optString("id")
                val value = row.optString("value")
                if (id.isNotBlank()) out[id] = value
            }
            out
        } catch (_: Throwable) { emptyMap() }
    }

    /**
     * স্টাফের বেছে নেওয়া/লেখা ট্যাগ সেভ করা। ⛔ ব্যর্থ হলে `SupabaseClient.
     * upsert()`-এর নিজের নিয়মেই `CloudWriteQueue`-তে জমা থাকে ও পরে আবার
     * চেষ্টা হয় (খাতার সারি B145/B164-এর সেই একই পাহারা) — এখানে আলাদা করে
     * কিছু বসাতে হয়নি।
     */
    fun saveTag(mobile: String, value: String, updatedByMobile: String): Boolean {
        val id = keyFor(mobile)
        if (id.isBlank()) return false
        val row = JSONObject()
            .put("id", id)
            .put("mobile", mobile)
            .put("value", value.trim())
            .put("updatedBy", updatedByMobile)
            .put("updatedAt", java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
            ).format(java.util.Date()))
        return try { SupabaseClient.upsert(TABLE, row) } catch (_: Throwable) { false }
    }

    /**
     * 🔒🔒 খাতার সারি B179 (TK, 30.07.2026 — B174/B175-এর বাকি অংশ, TK-এর
     * স্পষ্ট অনুমতিতে): Chamber/Global Search/Doctor Queue থেকে Prescription
     * ইত্যাদি খুললে ঠিকানা/বয়স/লিঙ্গ ফাঁকা থাকত, কারণ এই তিন জায়গার নিজের
     * তালিকায় (ChamberAttendanceRow/SearchHit/QueuePatient) এই তথ্যই নেই।
     * এই ফাংশনটা মোবাইল ধরে **একটাই ছোট, সরু (slim) অনুরোধে** সেই তিনটে ঘর
     * এনে দেয়। ⛔ TK-কে আগেই জানানো হয়েছে — এটা প্রতিটা ব্যবহারেই **একটা
     * নতুন ক্লাউড-কল**, TK নিজে অনুমতি দিয়েছেন ("জায়গাতেও ঠিক করতে চাই")।
     * ব্যর্থ হলে খালি মান ফেরত — কোনো পর্দা ভাঙে না, শুধু ওই তিনটে ঘর
     * আগের মতোই ফাঁকা থাকে।
     */
    // 🔒 V217 (§B216, Master Fix Order §14, item 6 "CHECK-UP Loading কমান"):
    // CHECK-UP খোলার আগে যে address/age/sex আনতে হয় (B179, নিচে অপরিবর্তিত
    // fetchDemographics), সেটা **একই রোগীর জন্য বারবার** না আনতে একটা ছোট,
    // ৫ মিনিটের in-memory cache — অ্যাপ বন্ধ হলে এমনিতেই খালি হয়ে যায়।
    // ⛔ B179-এর নিয়ম অক্ষত: প্রথমবার/৫ মিনিট পর সবসময় আসল ক্লাউড থেকেই
    //    আনা হয়, তাই ছাপায় ঠিকানা ভুল/পুরনো যাওয়ার ঝুঁকি নেই — এটা শুধু
    //    একই সেশনে বারবার একই রোগীর CHECK-UP খুললে ফালতু নেট-কল বাঁচায়।
    // 🔴 V217 self-audit fix (31.07.2026): plain HashMap thread-safe নয় —
    // এটা IO-thread থেকে ডাকা হয়, দুটো CHECK-UP প্রায় একসাথে খুললে ভেতরে
    // ভুল ডেটা/ক্র্যাশের ঝুঁকি ছিল। ConcurrentHashMap দিয়ে নিরাপদ করা হলো।
    private val demoCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Triple<String, String, String>>>()
    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    // 🔵 TK-ORDER (07.08.2026): fetchDemographics-এর মতোই, তবে পড়া **ব্যর্থ হলে null**
    // (findByMobileOrNull) — "রেকর্ড নেই" আর "পড়া হলোই না" আলাদা বোঝা যায়।
    // ⛔ একই একটাই cloud-read; পুরনো fetchDemographics (GlobalSearch ব্যবহার করে) অক্ষত।
    private fun fetchDemographicsOrNull(
        mobile: String,
        preferRowId: String = ""
    ): Triple<String, String, String>? {
        return try {
            /* 🔵 V531: রোগীর নিজের সারির আইডি জানা থাকলে ঠিক সেটাই — একটাই,
               আরও ছোট অনুরোধ। ⛔ ফাঁকা হলে নিচের লাইনটাই আগের মতো চলে। */
            var rows: JSONArray? = null
            if (preferRowId.isNotBlank()) {
                rows = SupabaseClient.fetchListOrNull(
                    "patients", "id=eq.$preferRowId", 1, select = "address,age,sex"
                )
                /* ⛔ **কখনোই আগের চেয়ে খারাপ নয়:** আইডিটা সার্ভারে না থাকলে
                   (যেমন ফোনে-বানানো সারি যা এখনো ওঠেনি) পুরোনো মোবাইল-পথেই
                   ফিরে যাওয়া হয় — আগে যা হত, হুবহু তাই। */
                if (rows != null && rows.length() == 0) rows = null
                if (rows == null) rows = SupabaseClient.findByMobileOrNull("patients", mobile, "address,age,sex", 1)
            } else {
                rows = SupabaseClient.findByMobileOrNull("patients", mobile, "address,age,sex", 1)
            }
            if (rows == null) return null
            if (rows.length() == 0) return Triple("", "", "")   // সত্যিই কোনো সারি নেই
            val row = rows.getJSONObject(0)
            Triple(row.s("address"), row.s("age"), row.s("sex"))
        } catch (_: Throwable) { null }
    }

    /**
     * 🔵🔒 V531 (২২.০৮.২০২৬, TK-নির্দেশ) — **এক নম্বরে দু'জন হলে ঠিক জনেরই
     * ঠিকানা/বয়স/লিঙ্গ।**
     *
     * **আগে যা হত:** শুধু মোবাইল ধরে খুঁজে **প্রথম সারিটাই** নেওয়া হত। এক
     * নম্বরে স্বামী-স্ত্রী দু'জন আলাদা রোগী থাকলে Doctor Queue ও Global Search
     * থেকে ক্লিনিক্যাল পর্দায় **অন্যজনের বয়স-লিঙ্গ** বসে যেতে পারত।
     *
     * **এখন:** ডাকার জায়গা রোগীর নিজের সারির আইডি (`preferRowId`) জানলে
     * সরাসরি **সেই আইডি ধরেই** আনা হয় — মোবাইল ধরে খোঁজাই হয় না।
     *
     * ⛔ `preferRowId` ফাঁকা ⇒ **হুবহু আগের সেই এক ডাক**, এক অক্ষরও বদলায়নি।
     * ⛔ **ক্লাউড-অনুরোধ বাড়েনি** — একটাই অনুরোধ, আগের মতোই। বরং আইডি ধরে
     *    খোঁজা মোবাইল ধরে খোঁজার চেয়ে **ছোট ও সঠিক**।
     * ⛔ জমানো ফলের (cache) চাবিতে আইডিটাও যোগ হয়েছে — নইলে দু'জনের
     *    ঠিকানা একে অপরের ঘরে ঢুকে যেত। পুরোনো চাবি (শুধু মোবাইল) অক্ষত
     *    থাকে যখন আইডি ফাঁকা, তাই আগের জমানো ফল নষ্ট হয় না।
     */
    fun fetchDemographicsCached(
        mobile: String,
        preferRowId: String = ""
    ): Triple<String, String, String> {
        val cacheKey = if (preferRowId.isBlank()) mobile else mobile + "|" + preferRowId
        val now = System.currentTimeMillis()
        demoCache[cacheKey]?.let { (ts, value) -> if (now - ts < CACHE_TTL_MS) return value }
        // 🔵 TK-ORDER (07.08.2026): আগে ব্যর্থ পড়ার খালি (`"","",""`)-ও ৫ মিনিট cache
        // হয়ে যেত → checkup/print-এ ঠিকানা/বয়স/লিঙ্গ ৫ মিনিট ফাঁকা থাকত। এখন **শুধু
        // সফল পড়াই** cache হয়; ব্যর্থ হলে এবারের মতো খালি ফেরে কিন্তু cache হয় না
        // (পরের বার আবার আসল ক্লাউড থেকে আনা হয়)।
        val fresh = fetchDemographicsOrNull(mobile, preferRowId)
        if (fresh != null) { demoCache[cacheKey] = now to fresh; return fresh }
        return Triple("", "", "")
    }

    /** 🔵🔒 V531: উপরের `fetchDemographicsCached`-এর মতোই — `preferRowId` দিলে
     *  ঠিক সেই সারিটাই, ফাঁকা দিলে **হুবহু আগের আচরণ**। */
    fun fetchDemographics(
        mobile: String,
        preferRowId: String = ""
    ): Triple<String, String, String> {
        return try {
            var rows = if (preferRowId.isNotBlank())
                SupabaseClient.fetchList("patients", "id=eq.$preferRowId", 1, select = "address,age,sex")
            else SupabaseClient.findByMobile("patients", mobile, "address,age,sex", 1)
            // ⛔ V531: আইডিতে না পেলে পুরোনো মোবাইল-পথ — আগের চেয়ে খারাপ কখনো নয়।
            if (preferRowId.isNotBlank() && rows.length() == 0)
                rows = SupabaseClient.findByMobile("patients", mobile, "address,age,sex", 1)
            if (rows.length() == 0) return Triple("", "", "")
            val row = rows.getJSONObject(0)
            Triple(row.s("address"), row.s("age"), row.s("sex"))
        } catch (_: Throwable) { Triple("", "", "") }
    }
}
