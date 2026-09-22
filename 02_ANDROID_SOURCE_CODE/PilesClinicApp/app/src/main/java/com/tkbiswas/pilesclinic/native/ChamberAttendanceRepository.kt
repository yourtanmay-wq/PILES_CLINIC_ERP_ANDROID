package com.tkbiswas.pilesclinic.native

// PERFORMANCE FIX (2026-07-25, TK-approved): the four independent board
// fetches below are now run at the same time instead of one after another,
// so these are the only imports this file needs.
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
// 🔴🔒 V466 (20.08.2026, TK-এর build-error ধরিয়ে দেওয়ায় ঠিক করা হলো):
// V462-এ যোগ করা delta-fetch কোড bare `JSONArray`/`JSONObject` ব্যবহার
// করেছিল, কিন্তু এই ফাইলে কখনো import করা ছিল না (পুরনো কোড সবসময়
// `org.json.JSONArray` পুরো-নাম দিয়ে লিখত)। তাই "Unresolved reference"
// বিল্ড-এরর হচ্ছিল। এখন import যোগ করা হলো — পুরনো fully-qualified
// ব্যবহারগুলো (org.json.JSONArray/JSONObject) অক্ষত থাকে, দুটোই একসাথে
// চলবে, কোনো সংঘর্ষ নেই।
import org.json.JSONArray
import org.json.JSONObject

/**
 * TK APPROVED FEATURE, Step 2+ (2026-07-16) -- "Chamber Attendance".
 *
 * WHAT THIS IS (agreed with TK before writing any of this): a daily,
 * branch-wise board mirroring TK's old paper register -- who was expected
 * today, who actually showed up, their Fees (Registration Fee) / Payment
 * (Advance-Treatment Payment), and what happened.
 *
 * THE ONE RULE THAT SHAPES EVERYTHING HERE (TK's explicit requirement,
 * repeated several times): this screen must NEVER be a second place where
 * the same information is typed twice. So this repository ONLY READS from
 * the exact same tables Enquiry/Registration/Payment/Follow-up already
 * write to ("enquiries", "patients", "payments", "followups") -- it does
 * not add any new table or duplicate field. Every action a staff takes from
 * this screen (adding a payment, updating a remark) opens the SAME existing
 * screens/functions (PaymentActivity, FollowUpRepository.updateRemark) --
 * nothing here reimplements a save path.
 *
 * "Expected" = TK-REQUESTED CHANGE (2026-07-19): used to be any Follow-up
 * whose nextFollow date matched the selected date -- TK's explicit
 * complaint: nextFollow is a general reminder field (used for "call them
 * back" as much as "they'll visit"), so it kept marking people Expected who
 * were only ever going to get a phone call, never actually visit chamber.
 * Now Expected comes ONLY from a deliberate "Mark Expected" action (see
 * markExpected() below, same zero-amount "payments" row trick as
 * markArrived already uses) -- nextFollow date is no longer read for this
 * at all.
 * "Arrived" = for the selected date, this mobile has a new Enquiry, a new
 * Registration, or a Payment -- i.e. something real actually happened for
 * them that day. Also inferred from Follow-up's own history log entries.
 */

data class ChamberAttendanceRow(
    val mobile: String,
    val name: String,
    val branch: String,
    val disease: String,
    val expected: Boolean,
    val arrived: Boolean,
    val feesCash: Double,
    val feesOnline: Double,
    val paymentCash: Double,
    val paymentOnline: Double,
    /* 🔴🔒 V709 (২৬.০৮.২০২৬, TK-রিপোর্ট, ডেমো-প্রুফে অনুমোদিত) — TK: *"আজকে
       কিষানগঞ্জের চেম্বার থেকে একজন পেশেন্টের টাকা রিফান্ড করা হলো, কিন্তু
       চেম্বারের তারিখে কেন দেখাচ্ছে না"*।
       আজ **অনুমোদিত (approved)** রিফান্ডে কত টাকা ফেরত গেছে — Cash ও Online
       আলাদা করে। `paymentCash`/`paymentOnline` থেকে ওটা আগেই **বিয়োগ** হয়ে
       আছে (নিচে, V709-এর অনেক আগের নিয়ম); এই দুটো ঘর সেই বিয়োগ হওয়া
       অঙ্কটাই **আলাদা করে মনে রাখে**, যাতে Review পর্দায় "Refund" নামে
       নিজের লাইন দেখানো যায়।
       ⛔ ডিফল্ট 0.0 ⇒ যেখানে বসানো হয়নি (পুরোনো cache) সেখানে আচরণ
          **হুবহু আগের মতোই**। ⛔ কোনো হিসাব এখানে বদলায়নি — শুধু মনে রাখা। */
    val refundCash: Double = 0.0,
    val refundOnline: Double = 0.0,
    val remark: String,
    val whatHappened: List<String>,
    // TK's explicit requirement: writing a remark from this board must
    // update the SAME Follow-up record the Visit/Patient card already
    // shows -- this id is what FollowUpRepository.updateRemark() needs.
    // Blank if this mobile has no Follow-up record yet (pure new Enquiry
    // still being processed) -- the Remark quick-action is hidden then.
    val followUpId: String = "",
    /**
     * 🔵🔒 V526 (২২.০৮.২০২৬, TK-নির্দেশ) — এই সারিটা **কোন রোগীর** (patients
     * টেবিলের সারির আইডি)। এক নম্বরে দুজন আলাদা রোগী থাকলে Payment · History ·
     * Report Card ঠিক এই রোগীতেই যাবে, আন্দাজে নয়।
     * ⛔ ডিফল্ট ফাঁকা — তাই এই ক্লাস তৈরি করা পুরোনো কোনো জায়গা বদলাতে হয়নি,
     *    আর ফাঁকা থাকলে আচরণ **হুবহু আগের মতোই**।
     */
    val patientRowId: String = "",
    // TK-REQUESTED ADDITION (2026-07-19): needed for the A4 register print
    // (ChamberRegisterPdfBuilder) to show Patient ID alongside Name/Mobile.
    // Blank for a pure Enquiry/Expected entry that isn't Registered yet.
    val patientId: String = "",
    // TK-REQUESTED ADDITION (2026-07-24): itemized payment display lines
    // for the live-screen "Payment" box (see section 160 of master note).
    // Display-only -- feesCash/feesOnline/paymentCash/paymentOnline above
    // are UNCHANGED and remain the source of truth for Save/Print/Report
    // Card.
    val paymentLines: List<String> = emptyList(),
    // TK-REQUESTED ADDITION (2026-07-19): earliest real timestamp for this
    // row today, used to print patients in actual chronological order
    // (not alphabetical). Blank if nothing with a timestamp happened yet.
    val arrivedAt: String = "",
    // 🔴🔒 V471 (20.08.2026, TK-অনুমোদিত) — রোগীর নামের নিচে রেফারিং RMP-র
    // নাম (থাকলে) দেখানোর জন্য। ⛔ Display-only, বাকি কোনো হিসাব/সংখ্যা
    // এই ঘরের উপর নির্ভর করে না।
    val refDoctor: String = "",
    /* 🔴🔒 V933 (৩১.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "কোন পেশেন্ট যদি RMP পাঠায়
       সে ক্ষেত্রে পেশেন্ট নামের পাশে আরএমপি কেন লেখা থাকে না?")। V684-এ
       শুধু RMP-র **নাম** (`refDoctor`) দেখানো হত — কিন্তু রেজিস্ট্রেশনে
       "Referred By = Dr. Visit" বাছার পরেও নামের ঘরটা **ফাঁকা রাখা যায়**,
       তখন বোর্ডে কিছুই বসত না। Today's Collection-এর চিপ (`PaymentModel.
       rmpTagOf`) কিন্তু ওই অবস্থাতেও "RMP" লেখে — সেই একই নিয়ম এখন এখানেও।
       ⛔ Display-only, কোনো হিসাব এই ঘরের উপর নির্ভর করে না। */
    val refBy: String = "",
    // 🟢🔒 V612 (২৪.০৮.২০২৬, TK-নির্দেশ, "খুব নিরাপদে") — ওষুধ-বিক্রির
    // Cash/Online আলাদা করে ধরার জন্য। ⛔ ডিফল্ট 0.0 — paymentCash/
    // paymentOnline-এর হিসাব এক অক্ষরও বদলায়নি (ওষুধের টাকা এখনো তাতেও
    // যোগ হয়, এটা শুধু বাড়তি, আলাদা করে গোনা)।
    val medicineCash: Double = 0.0,
    val medicineOnline: Double = 0.0,
    // 🟢🔒🔒 V654 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবিসহ — "গত দিনের ট্রিটমেন্ট
    // প্রগ্রেস যেন হাইড থাকে, আজকেরটা উজ্জ্বল থাকে") — `remark` ঘরের
    // লেখাটা **কবে** লেখা হয়েছিল (YYYY-MM-DD), যাতে UI নিজে বুঝতে পারে
    // এটা আজকের নাকি আগের দিনের লেখা। ⛔ ডিফল্ট ফাঁকা — পুরোনো কোনো
    // ব্যবহার ভাঙে না।
    val remarkUpdatedAt: String = ""
)

data class ChamberAttendanceTotals(
    val feesCash: Double,
    val feesOnline: Double,
    val paymentCash: Double,
    val paymentOnline: Double,
    val expectedCount: Int,
    val arrivedCount: Int,
    val noShowCount: Int,
    // 🟢🔒 V612 — উপরের ChamberAttendanceRow-এর মতোই কারণ। ⛔ ডিফল্ট 0.0।
    val medicineCash: Double = 0.0,
    val medicineOnline: Double = 0.0
)

data class ChamberAttendanceBoard(
    val rows: List<ChamberAttendanceRow>,
    val totals: ChamberAttendanceTotals
)

object ChamberAttendanceRepository {

    // TK-REQUESTED ADDITION (2026-07-20): same "show what was already on the
    // phone instantly" pattern already added to Doctor Queue and Follow-up.
    // Read-only display cache of the last successfully loaded board for a
    // given date+branch; loadBoard() below is completely unchanged except
    // for saving into this cache right before it returns.
    private const val CACHE_PREFS = "chamber_board_cache"

    // =========================================================================
    // 🔴🔒 V462 (20.08.2026, TK-নির্দেশ · ধাপ ১, শুধু আজকের/খোলা বোর্ড):
    // "শুধু বদলানো অংশটুকু নামুক" — Chamber-এর জন্য।
    //
    // ⛔ `loadBoard()`-এর ৪টা override বাদে বাকি সব কোড অক্ষত। এই ৪টা
    //    override-ও কেবল তখনই ব্যবহার হয় যখন `fetchBoardDelta()` (নিচে)
    //    থেকে ডাকা হয় — সেটা আবার শুধু `ChamberAttendanceActivity`-র
    //    ৩০-সেকেন্ড auto-refresh পথেই ব্যবহৃত (স্ক্রিন প্রথম খোলা/তারিখ
    //    বদল/ব্রাঞ্চ বদল সবসময়ই পুরনো নিরাপদ `loadBoard()`)।
    //
    // কেন removal-লজিক লাগে না (কোড পড়ে নিশ্চিত করা, আন্দাজ নয়):
    //  - payments/patients/enquiries: চিরকাল যোগ-হওয়া তালিকা (ledger-জাতীয়),
    //    কখনো "সরানো" লাগে না — Follow-up-এর patients/payments-এর একই
    //    প্রমাণিত যুক্তি এখানেও খাটে।
    //  - followups: এই বোর্ডের read **status দিয়ে ছাঁকা হয় না** (উপরের
    //    B110 মন্তব্য — "status-এর ছাঁকনি ক্লাউড থেকে তুলে নিচে কোডে
    //    বসানো হলো")। অর্থাৎ Cancelled/Rejected-সহ **সব status**-ই এমনিতেই
    //    পড়া হয় — তাই এখানেও শুধু upsert (নতুন/বদলানো বসানো) যথেষ্ট।
    // ⛔ তাই চারটে read-ই upsert-only — Doctor Queue-র মতো জটিল "সরানো"
    //    লজিক এখানে দরকারই নেই, যা এই কাজকে তুলনামূলক নিরাপদ করেছে।
    // ⛔ সত্যিকারের hard-DELETE ধরা পড়বে না — ৩ ঘণ্টার নিয়মিত পূর্ণ-fetch (V1275; আগে ৩০ মিনিট)
    //    সেটা স্বয়ংক্রিয়ভাবে ঠিক করে দেবে (আগের সব delta-কাজের একই সীমা)।
    // =========================================================================
    private const val CHAMBER_DELTA_PREFS = "chamber_delta_state"
    /* 🟢🔒 V1275 (০৯.০৯.২০২৬, TK-নির্দেশ: *"Egress কমানোর আর কোন উপায় আছে কিনা …
       তবে সাবধানে, কোন প্রকার ভালো কাজ যেন খারাপ না হয়"*) — **৩০ মিনিট → ৩ ঘণ্টা**।
       **কেন নিরাপদ (মেপে দেখা):** এই জোর-করা পূর্ণ-পড়াটার একমাত্র কাজ **hard-DELETE
       ধরা** (delta শুধু `updatedAt` দেখে, তাই মুছে ফেলা সারি সে দেখতে পায় না) —
       রোজকার সব বদল আগের মতোই delta-তেই আসে, এক সেকেন্ডও দেরি হয় না।
       ⇒ ওই এক পড়ায় ব্রাঞ্চের **সব followups** (তারিখের ছাঁকনি নেই, limit ৫০০০) +
         ওই দিনের payments · patients · enquiries নামে। ৩ ঘণ্টা করলে জোর-করা
         পড়া **৬ ভাগের ১ ভাগ**।
       ⚠️ TK-কে আগে জানানো ঝুঁকি: অন্য ফোনে কেউ সারি **চিরতরে মুছলে** এই ফোনের
          বোর্ডে সেটা ৩ ঘণ্টা পর্যন্ত দেখা নাও যেতে পারে — TK মেনেছেন।
       ⛔ এটা V1258-এর হুবহু একই বদল (`FU_FULL_REFRESH_INTERVAL_MS` ও
          `BANNER_FULL_GAP_MS` দুটোই ৩ ঘণ্টা) ⇒ প্রকল্পের তিনটে ঘড়িই এখন এক।
       ⛔ টাকার হিসাব · বোর্ডের সারি · ছাঁকনি · limit — কিচ্ছু বদলায়নি। */
    private const val CHAMBER_FULL_REFRESH_INTERVAL_MS = 3L * 60L * 60L * 1000L   // ৩ ঘণ্টা (V1275; আগে ৩০ মিনিট)
    private const val CHAMBER_SAFETY_BACK_MS = 5_000L

    private fun chamberDeltaPrefs(context: android.content.Context?) =
        context?.getSharedPreferences(CHAMBER_DELTA_PREFS, android.content.Context.MODE_PRIVATE)

    private fun chamberStampNow(): String = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(System.currentTimeMillis() - CHAMBER_SAFETY_BACK_MS))
    } catch (_: Throwable) { "" }

    private fun chamberLoadCachedArray(context: android.content.Context?, key: String): JSONArray {
        val sp = chamberDeltaPrefs(context) ?: return JSONArray()
        return try {
            val raw = sp.getString(key, null) ?: return JSONArray()
            JSONArray(raw)
        } catch (_: Throwable) { JSONArray() }
    }

    private fun chamberSaveCachedArray(context: android.content.Context?, key: String, arr: JSONArray) {
        val sp = chamberDeltaPrefs(context) ?: return
        try { sp.edit().putString(key, arr.toString()).apply() } catch (_: Throwable) { }
    }

    /** upsert-only delta+merge — payments/enquiries/patients/followups চারটেতেই একই যুক্তি। */
    private fun chamberDeltaUpsertOrNull(
        context: android.content.Context?, table: String, dateFilterKey: String, date: String,
        branchPart: String, cols: String?, cacheKey: String, since: String
    ): JSONArray? {
        val sinceEnc = try { java.net.URLEncoder.encode(since, "UTF-8") } catch (_: Throwable) { since }
        val filter = "$dateFilterKey=eq.$date$branchPart&updatedAt=gt.$sinceEnc"
        val delta = try {
            if (cols != null) SupabaseClient.fetchListSlimOrNull(table, filter, 2000, cols)
            else SupabaseClient.fetchListOrNull(table, filter, 2000)
        } catch (_: Throwable) { null } ?: return null

        val cached = chamberLoadCachedArray(context, cacheKey)
        val byId = LinkedHashMap<String, JSONObject>()
        for (i in 0 until cached.length()) {
            val o = cached.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        for (i in 0 until delta.length()) {
            val row = delta.getJSONObject(i)
            val id = row.optString("id"); if (id.isBlank()) continue
            byId[id] = row   // upsert-only
        }
        val merged = JSONArray(); for (v in byId.values) merged.put(v)
        chamberSaveCachedArray(context, cacheKey, merged)
        return merged
    }

    /** আজকের বোর্ডের জন্য delta — সবগুলো একসাথে, অথবা কোনোটাই না (নিরাপত্তা)। */
    fun fetchBoardDelta(date: String, branchFilter: String?, context: android.content.Context?): ChamberAttendanceBoard {
        if (context == null) return loadBoard(date, branchFilter, context)
        val allBranch = branchFilter == null || branchFilter == "All"
        val branchPart = if (!allBranch) "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""
        val branchKey = branchFilter?.trim()?.takeIf { it.isNotBlank() && !it.equals("All", true) } ?: "all"
        val stateKey = "${date}_$branchKey"
        val sp = chamberDeltaPrefs(context)!!
        val since = sp.getString("since_$stateKey", null)
        val lastFullAt = sp.getLong("fullAt_$stateKey", 0L)
        val now = System.currentTimeMillis()

        if (since.isNullOrBlank() || (now - lastFullAt) > CHAMBER_FULL_REFRESH_INTERVAL_MS) {
            val result = loadBoard(date, branchFilter, context)
            // পূর্ণ-fetch হলে ৪টা raw cache-ও আবার ভরে রাখা হয়, যাতে পরের delta-কল এগোতে পারে।
            try {
                val p = SupabaseClient.fetchListSlimOrNull("payments", "date=eq.$date$branchPart", 5000, SupabaseClient.PAYMENT_COLS_LIST)
                val e = SupabaseClient.fetchListOrNull("enquiries", "date=eq.$date$branchPart", 5000)
                val pt = SupabaseClient.fetchListSlimOrNull("patients", "registrationDate=eq.$date$branchPart", 5000, SupabaseClient.PATIENT_COLS_NO_PHOTO)
                /* 🔴🔒 V577 (২৩.০৮.২০২৬ — TK-নির্দেশে পুরো প্রজেক্টের Egress অডিট):
                   এখানে `followups`-এর **সব ঘর** নামত — অর্থাৎ **রোগীর `photo`
                   ঘরটাও** (base64 ছবি), ব্রাঞ্চের ৫০০০ সারি পর্যন্ত, প্রতি ৩০
                   মিনিটে প্রতিটা ফোনে। অথচ এই তালিকাটা যায় `loadBoard()`-এ, আর
                   সেখানে (এই ফাইলেরই ৪৮৫ নম্বর লাইনে, খাতার সারি B661) প্রমাণ
                   করা আছে যে বোর্ড এই টেবিল থেকে মাত্র **৭টা ঘর** পড়ে।
                   ⇒ তাই ঠিক ওই একই তালিকাটাই (`FOLLOWUP_COLS_CHAMBER_BOARD`)
                     এখানে বসানো হলো। ⛔ সারি · ছাঁকনি · limit · সাজানো — কিছুই
                     বদলায়নি, বোর্ডের হিসাবও হুবহু এক; শুধু কম ঘর নামে। */
                val f = SupabaseClient.fetchListSlimOrNull("followups", branchPart.removePrefix("&").ifBlank { null }, 5000, SupabaseClient.FOLLOWUP_COLS_CHAMBER_BOARD)
                if (p != null) chamberSaveCachedArray(context, "payments_$stateKey", p)
                if (e != null) chamberSaveCachedArray(context, "enquiries_$stateKey", e)
                if (pt != null) chamberSaveCachedArray(context, "patients_$stateKey", pt)
                if (f != null) chamberSaveCachedArray(context, "followups_$stateKey", f)
                sp.edit().putString("since_$stateKey", chamberStampNow()).putLong("fullAt_$stateKey", now).apply()
            } catch (_: Throwable) { }
            return result
        }

        val payments = chamberDeltaUpsertOrNull(context, "payments", "date", date, branchPart, SupabaseClient.PAYMENT_COLS_LIST, "payments_$stateKey", since)
            ?: return loadBoard(date, branchFilter, context)
        val enquiries = chamberDeltaUpsertOrNull(context, "enquiries", "date", date, branchPart, null, "enquiries_$stateKey", since)
            ?: return loadBoard(date, branchFilter, context)
        val patients = chamberDeltaUpsertOrNull(context, "patients", "registrationDate", date, branchPart, SupabaseClient.PATIENT_COLS_NO_PHOTO, "patients_$stateKey", since)
            ?: return loadBoard(date, branchFilter, context)
        // followups-এর মূল filter branch-ভিত্তিক (তারিখ না) — উপরের মতোই, শুধু delta since যোগ।
        val fuBranchFilter = branchPart.removePrefix("&").ifBlank { null }
        val fuFilterCombined = if (fuBranchFilter != null) "$fuBranchFilter&updatedAt=gt.${java.net.URLEncoder.encode(since, "UTF-8")}"
            else "updatedAt=gt.${java.net.URLEncoder.encode(since, "UTF-8")}"
        // 🔴🔒 V577 — উপরের একই কারণ: delta-তেও `photo` সহ সব ঘর নামত। বোর্ড
        //    ওই ৭টা ঘরই পড়ে, আর নিচে এই সারিগুলো ঠিক ওই জমানো তালিকাটার
        //    সঙ্গেই মেশানো হয় — তাই দুই দিকের ঘর এক রাখা **জরুরিও** ছিল।
        val followUps = try { SupabaseClient.fetchListSlimOrNull("followups", fuFilterCombined, 2000, SupabaseClient.FOLLOWUP_COLS_CHAMBER_BOARD) } catch (_: Throwable) { null }
            ?: return loadBoard(date, branchFilter, context)
        run {
            val cached = chamberLoadCachedArray(context, "followups_$stateKey")
            val byId = LinkedHashMap<String, JSONObject>()
            for (i in 0 until cached.length()) { val o = cached.optJSONObject(i) ?: continue; val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o }
            for (i in 0 until followUps.length()) { val row = followUps.getJSONObject(i); val id = row.optString("id"); if (id.isNotBlank()) byId[id] = row }
            val merged = JSONArray(); for (v in byId.values) merged.put(v)
            chamberSaveCachedArray(context, "followups_$stateKey", merged)
        }
        val followUpsMerged = chamberLoadCachedArray(context, "followups_$stateKey")

        sp.edit().putString("since_$stateKey", chamberStampNow()).apply()
        return loadBoard(
            date, branchFilter, context,
            paymentsOverride = payments, enquiriesOverride = enquiries,
            patientsOverride = patients, followUpsOverride = followUpsMerged
        )
    }

    fun loadCachedBoard(context: android.content.Context?, date: String, branchFilter: String?): ChamberAttendanceBoard? {
        val ctx = context ?: return null
        val key = "cache_${date}_${branchFilter ?: "All"}"
        val json = ctx.getSharedPreferences(CACHE_PREFS, android.content.Context.MODE_PRIVATE).getString(key, null) ?: return null
        return try {
            val obj = org.json.JSONObject(json)
            val rowsArr = obj.getJSONArray("rows")
            val rows = mutableListOf<ChamberAttendanceRow>()
            for (i in 0 until rowsArr.length()) {
                val r = rowsArr.getJSONObject(i)
                rows.add(
                    ChamberAttendanceRow(
                        mobile = r.optString("mobile", ""), name = r.optString("name", ""),
                        branch = r.optString("branch", ""), disease = r.optString("disease", ""),
                        expected = r.optBoolean("expected", false), arrived = r.optBoolean("arrived", false),
                        feesCash = r.optDouble("feesCash", 0.0), feesOnline = r.optDouble("feesOnline", 0.0),
                        paymentCash = r.optDouble("paymentCash", 0.0), paymentOnline = r.optDouble("paymentOnline", 0.0),
                        medicineCash = r.optDouble("medicineCash", 0.0), medicineOnline = r.optDouble("medicineOnline", 0.0),   // 🟢🔒 V612 (পুরোনো cache-এ নেই ⇒ 0.0)
                        refundCash = r.optDouble("refundCash", 0.0), refundOnline = r.optDouble("refundOnline", 0.0),   // 🔴🔒 V709 (পুরোনো cache-এ নেই ⇒ 0.0)
                        remark = r.s("remark"),   // 🔴🔒 V696 — জমানো cache-এ "null" ঢুকে থাকলেও পরের বার নিজেই সেরে যায়
                        whatHappened = r.optJSONArray("whatHappened")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
                        followUpId = r.optString("followUpId", ""), patientId = r.optString("patientId", ""),
                        arrivedAt = r.optString("arrivedAt", ""), refDoctor = r.optString("refDoctor", ""),
                        refBy = r.optString("refBy", ""),   // 🔴🔒 V933 (পুরোনো cache-এ নেই ⇒ ফাঁকা, আগের আচরণ)
                        patientRowId = r.optString("patientRowId", ""),
                        remarkUpdatedAt = r.optString("remarkUpdatedAt", "")   // 🟢🔒 V654 (পুরোনো cache-এ নেই ⇒ ফাঁকা)   // 🔵 V526 (পুরোনো cache-এ নেই ⇒ ফাঁকা, আগের আচরণ)
                    )
                )
            }
            // TK-REPORTED BUG FIX (2026-07-21): drop any ghost (no name AND no
            // mobile) row that an older build may have left in this cache, and
            // recompute the counts from the cleaned rows so the stat cards
            // never show a No-show/Expected that isn't actually in the list.
            // 🔒 TK'S PERMANENT RULE (28.07.2026, khata row B25): what THIS
            // phone wrote must show on THIS phone. This stored board is exactly
            // what stays on screen when the line is too weak to load a fresh
            // one -- and a Treatment Progress / remark just typed here used to
            // be missing from it, so the staff typed it again: TK's own words,
            // "রিমার্ক লিখছি হয়ে গেছে দেখায়, পরে পুরনোটাই থাকে."
            // The remark is read back from the phone's own Follow-up store (the
            // same place the remark was saved into), matched by that row's own
            // followUpId. ⛔ MONEY IS NEVER TOUCHED: only the remark text is
            // replaced, so Fees/Payment figures stay exactly as the stored
            // board had them and can never be blanked or zeroed by this.
            val withMine = try { applyMyRemarks(ctx, dropGhostRows(rows)) } catch (_: Throwable) { dropGhostRows(rows) }
            ChamberAttendanceBoard(withMine, totalsOf(withMine))
        } catch (e: Throwable) { null }
    }

    /** Replaces a stored row's remark with what this phone last wrote for the
     *  same Follow-up record, and nothing else. Returns the rows untouched if
     *  this phone has nothing newer of its own. */
    private fun applyMyRemarks(context: android.content.Context, rows: List<ChamberAttendanceRow>): List<ChamberAttendanceRow> {
        val ids = rows.mapNotNull { it.followUpId.takeIf { id -> id.isNotBlank() } }.toSet()
        if (ids.isEmpty()) return rows
        val mine = try { LocalWorkflowStore(context).findFollowUps(ids) } catch (_: Throwable) { emptyMap<String, org.json.JSONObject>() }
        if (mine.isEmpty()) return rows
        return rows.map { row ->
            val local = mine[row.followUpId] ?: return@map row
            val text = local.s("lastRemark")
            if (text.isBlank()) return@map row
            /* 🔴🔴🔒 V933 (৩১.০৮.২০২৬, TK-রিপোর্ট — "চেম্বার বন্ধ করার পরে আবার
               যখন ওপেন করা হয় সেক্ষেত্রে আবার কেন Treatment Progress লিখতে?")
               ─── আসল কারণ (কোড ধরে, আন্দাজ নয়) ───────────────────────────
               এই ফাংশনটা জমানো বোর্ডে **শুধু লেখাটাই** ফিরিয়ে আনত, লেখাটা
               **কবে লেখা হয়েছিল** সেই ঘরটা (`remarkUpdatedAt`) নয়। V810-এর
               চেম্বার-বন্ধের পাহারা ঠিক ওই তারিখটাই মেলায় — জমানো বোর্ডে ঘরটা
               ফাঁকা/পুরনো থাকলে পাহারা ভাবত "আজ কিছু লেখা হয়নি", আর স্টাফকে
               একই লেখা আবার লিখতে হত। চেম্বার আবার খোলার পরেই এটা সবচেয়ে বেশি
               চোখে পড়ত, কারণ তখন পর্দা প্রথমে জমানো বোর্ডটাই দেখায়।
               **সমাধান:** লেখাটার সাথে তার নিজের তারিখটাও ফেরে।
               ⛔ টাকার কোনো ঘরে হাত পড়ে না (আগের মতোই) — শুধু লেখা ও তার তারিখ।
               ⛔ ফোনের নিজের লেখাটা পুরনো হলে জমানো বোর্ডেরটাই থাকে। */
            val at = local.s("lastRemarkAt")
            if (text == row.remark) {
                if (at.isNotBlank() && at > row.remarkUpdatedAt) row.copy(remarkUpdatedAt = at) else row
            } else {
                row.copy(remark = text, remarkUpdatedAt = at.ifBlank { row.remarkUpdatedAt })
            }
        }
    }

    /** 🔴🔒 V1371 (১২.০৯.২০২৬, তালিকা ৪৬২-ঞ — TK-নির্দেশে গভীরে যাচাই করে):
     *  প্রতিদিন প্রতি ব্রাঞ্চের জন্য এই cache-এ একটা নতুন চাবি (`cache_<date>_
     *  <branch>`) জমা হয়, কিন্তু কোনো চাবি কখনো মোছা হত না — মাস-বছরের পর
     *  পাঁচ ব্রাঞ্চ মিলিয়ে এই ফাইল অনির্দিষ্টকাল বড় হতেই থাকত (নিয়ম ৫খ)।
     *  এখন প্রতি অ্যাপ-চালুতে **একবার** (থ্রটল করা, প্রতি সেভেই নয়) ৬০
     *  দিনের বেশি পুরনো চাবি মুছে দেওয়া হয় — Calendar দিয়ে সাম্প্রতিক
     *  পুরনো দিন দেখার সুবিধা (৬০ দিন পর্যন্ত) অক্ষত থাকে, কিন্তু ফাইল
     *  অনির্দিষ্টকাল বড় হয় না। ⛔ এটা শুধু on-device display cache — আসল
     *  তথ্য সার্ভারেই থাকে, তাই ৬০ দিনের বেশি পুরনো দিন খুললে সামান্য একটু
     *  ধীরে (নেট থেকে) লোড হবে, কিছু হারাবে না। */
    @Volatile private var boardsPrunedThisSession = false
    private fun pruneOldBoardsOnce(context: android.content.Context) {
        if (boardsPrunedThisSession) return
        boardsPrunedThisSession = true
        try {
            val cutoffCal = java.util.Calendar.getInstance()
            cutoffCal.add(java.util.Calendar.DAY_OF_YEAR, -60)
            val cutoff = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cutoffCal.time)
            val prefs = context.getSharedPreferences(CACHE_PREFS, android.content.Context.MODE_PRIVATE)
            val editor = prefs.edit()
            var changed = false
            for (k in prefs.all.keys) {
                if (!k.startsWith("cache_")) continue
                val datePart = k.removePrefix("cache_").take(10)
                if (datePart.length == 10 && datePart < cutoff) { editor.remove(k); changed = true }
            }
            if (changed) editor.apply()
        } catch (_: Throwable) { }
    }

    private fun saveCachedBoard(context: android.content.Context?, date: String, branchFilter: String?, board: ChamberAttendanceBoard) {
        val ctx = context ?: return
        try { pruneOldBoardsOnce(ctx) } catch (_: Throwable) { }
        try {
            val rowsArr = org.json.JSONArray()
            for (row in board.rows) {
                rowsArr.put(
                    org.json.JSONObject()
                        .put("mobile", row.mobile).put("name", row.name).put("branch", row.branch)
                        .put("disease", row.disease).put("expected", row.expected).put("arrived", row.arrived)
                        .put("feesCash", row.feesCash).put("feesOnline", row.feesOnline)
                        .put("paymentCash", row.paymentCash).put("paymentOnline", row.paymentOnline)
                        .put("medicineCash", row.medicineCash).put("medicineOnline", row.medicineOnline)   // 🟢🔒 V612
                        .put("refundCash", row.refundCash).put("refundOnline", row.refundOnline)   // 🔴🔒 V709
                        .put("remark", row.remark).put("whatHappened", org.json.JSONArray(row.whatHappened))
                        .put("followUpId", row.followUpId).put("patientId", row.patientId).put("arrivedAt", row.arrivedAt)
                        .put("refDoctor", row.refDoctor)
                        .put("refBy", row.refBy)   // 🔴🔒 V933
                        .put("patientRowId", row.patientRowId)   // 🔵 V526
                        .put("remarkUpdatedAt", row.remarkUpdatedAt)   // 🟢🔒 V654
                )
            }
            val totalsObj = org.json.JSONObject()
                .put("feesCash", board.totals.feesCash).put("feesOnline", board.totals.feesOnline)
                .put("paymentCash", board.totals.paymentCash).put("paymentOnline", board.totals.paymentOnline)
                .put("medicineCash", board.totals.medicineCash).put("medicineOnline", board.totals.medicineOnline)   // 🟢🔒 V612
                .put("expectedCount", board.totals.expectedCount).put("arrivedCount", board.totals.arrivedCount)
                .put("noShowCount", board.totals.noShowCount)
            val out = org.json.JSONObject().put("rows", rowsArr).put("totals", totalsObj)
            val key = "cache_${date}_${branchFilter ?: "All"}"
            ctx.getSharedPreferences(CACHE_PREFS, android.content.Context.MODE_PRIVATE).edit().putString(key, out.toString()).apply()
        } catch (_: Throwable) { }
    }

    // TK-REQUESTED FIX (2026-07-19): this is already a singleton (Kotlin
    // `object`), so a shared lock genuinely protects every caller -- used
    // for the same-class race-condition fix as the other repositories
    // (Mark Arrived / Undo / retry could otherwise race on the same
    // pending queue and silently drop each other's change).
    private val LOCK = Any()

    /* 🔴🔒 V933 (৩১.০৮.২০২৬, TK-রিপোর্ট) — চেম্বার বোর্ডের "Ref By" লাইনটা
       কী লেখা হবে, তার **একটাই নিয়ম** (বোর্ডের সরু ও চওড়া সারি, আর Close
       Chamber-এর Review — তিন জায়গাই এটাই ডাকে, যাতে তিন রকম না হয়)।
       ⛔ নিয়মটা Today's Collection-এর RMP-চিপের (`PaymentModel.rmpTagOf`)
          হুবহু একই: নাম থাকলে নাম, নাম না থাকলেও "Referred By = Dr. Visit"
          হলে অন্তত "RMP" — আগে এই দ্বিতীয় ক্ষেত্রে কিছুই দেখাত না।
       ⛔ কিছুই না থাকলে ফাঁকা ফেরে ⇒ লাইনটা আগের মতোই বসে না। */
    fun refByLabel(row: ChamberAttendanceRow): String {
        val name = row.refDoctor.trim()
        if (name.isNotBlank()) return "Ref By: $name"
        val by = row.refBy.trim().lowercase()
        val isDoc = Regex("^dr\\.? ?visit$").matches(by) || by == "rmp" || by.contains("doctor")
        return if (isDoc) "Ref By: RMP" else ""
    }

    private fun digits(v: String): String = v.filter { it.isDigit() }.takeLast(10)

    private fun isFeeRow(payType: String): Boolean {
        val t = payType.lowercase()
        return t == "registration" || t == "visitfee" || t == "visit_fee"
    }

    /** True if this "mode" text should count under the Cash column vs the
     *  Online/UPI column -- same normalization PaymentModel already uses,
     *  reused here (not reimplemented) so a payment always lands in the
     *  same bucket everywhere in the app. */
    private fun isCash(mode: String): Boolean = PaymentModel.normalizeMode(mode) != "ONLINE"

    /** 🔒 V236 (TK, 01.08.2026 — সমস্যা-৩): Chamber Date-এর "TREATMENT PROGRESS"
     *  ঘরে অ্যাপের নিজের বসানো payment/auto-label (যেমন "Treatment payment /
     *  Advance received", "Advance Payment received") কখনো রোগীর progress সেজে
     *  দেখাবে না — শুধু মানুষের সত্যিকারের লেখা। Report Card/Timeline যে
     *  PaymentModel.isAutoPaymentRemark ব্যবহার করে সেটাই এখানে, সঙ্গে followups-
     *  এর কয়েকটা fixed app-label যা ওই ফাংশনের তালিকায় নেই। ⛔ মানুষের লেখা
     *  কোনো remark এতে ধরা পড়ে না, তাই আসল progress কখনো লুকোবে না। */
    private fun isAppAutoRemark(remark: String): Boolean {
        val r = remark.trim()
        if (r.isEmpty()) return true
        if (PaymentModel.isAutoPaymentRemark(r, "")) return true
        val fixed = listOf(
            "Treatment payment / Advance received",
            "Advance Payment received",
            "Converted to Patient / Treatment"
        )
        return fixed.any { r.equals(it, ignoreCase = true) }
    }

    /** TK-REPORTED BUG FIX (2026-07-21): a stale/corrupt "ghost" row with NO
     *  name AND NO mobile (left over in the display cache from an older
     *  build) was showing as a blank No-show row carrying an old
     *  "Treatment payment / Advance received" remark, splitting one real
     *  patient into two rows and inflating the Expected/No-show counts. An
     *  identity-less row can never be a real patient -- every real Enquiry/
     *  Registration/Mark-Expected always carries at least a mobile -- so it
     *  is dropped everywhere a board is produced (fresh AND cached). */
    private fun dropGhostRows(rows: List<ChamberAttendanceRow>): List<ChamberAttendanceRow> =
        rows.filter { it.name.isNotBlank() || it.mobile.isNotBlank() }

    /** Recomputes the four money totals and the Expected/Arrived/No-show
     *  counts straight from the given rows, so the numbers on the stat cards
     *  always match exactly the rows actually shown (used after ghost rows
     *  are dropped -- never trust a cached count that may have included one). */
    private fun totalsOf(rows: List<ChamberAttendanceRow>): ChamberAttendanceTotals =
        ChamberAttendanceTotals(
            feesCash = rows.sumOf { it.feesCash },
            feesOnline = rows.sumOf { it.feesOnline },
            paymentCash = rows.sumOf { it.paymentCash },
            paymentOnline = rows.sumOf { it.paymentOnline },
            medicineCash = rows.sumOf { it.medicineCash },     // 🟢🔒 V612
            medicineOnline = rows.sumOf { it.medicineOnline }, // 🟢🔒 V612
            expectedCount = rows.count { it.expected && !it.arrived },
            arrivedCount = rows.count { it.arrived },
            noShowCount = rows.count { it.expected && !it.arrived }
        )

    // 🔴🔒 V462 (20.08.2026, TK-অনুমোদিত · ধাপ ১, শুধু আজকের/খোলা বোর্ড):
    // "শুধু বদলানো অংশটুকু নামুক" — Chamber-এর জন্য। ⛔ নিচের `loadBoard()`
    // **এক অক্ষরও বদলানো হয়নি** — শুধু ৪টা নতুন ঐচ্ছিক override প্যারামিটার
    // যোগ হয়েছে (ডিফল্ট `null`), তাই বাকি সব কল-সাইট আগের মতোই কাজ করে।
    // Override দিলে সংশ্লিষ্ট ক্লাউড-কলটা বাদ যায়, কিন্তু তার পরের সব জটিল
    // যুক্তি (local-pending merge, ব্রাঞ্চ/স্ট্যাটাস হিসাব, বোর্ড তৈরি)
    // হুবহু আগের মতোই সেই override-করা ডেটা নিয়ে কাজ করে।
    fun loadBoard(
        date: String, branchFilter: String?, context: android.content.Context? = null,
        paymentsOverride: JSONArray? = null, enquiriesOverride: JSONArray? = null,
        patientsOverride: JSONArray? = null, followUpsOverride: JSONArray? = null
    ): ChamberAttendanceBoard {
        val allBranch = branchFilter == null || branchFilter == "All"
        val branchPart = if (!allBranch) "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""

        // PERFORMANCE FIX (2026-07-25, TK-approved — no design/behaviour
        // change): these four reads do not depend on each other, so they are
        // now started together and awaited together instead of one after the
        // other. The board used to wait for the SUM of the four times; it now
        // waits only for the slowest one. Exactly the same four queries with
        // exactly the same filters/limits are sent (no extra load on the
        // Supabase quota), and every null-check / local-pending merge below
        // still happens in exactly the same order as before.
        val fetched = runBlocking {
            val paymentsDef = async(Dispatchers.IO) {
                if (paymentsOverride != null) paymentsOverride else
                // ⚡ খাতার সারি B135 (TK "হ্যাঁ", 29.07.2026 রাত ৯.৩০): এই পড়াটা
                // এতদিন `payments`-এর **সব ঘর** চাইত। বোর্ড যে ঘরগুলো পড়ে সব
                // কটাই নিচের তালিকায় আছে (প্রজেক্টে আগে থেকেই ব্যবহার হওয়া
                // `PAYMENT_COLS_LIST`, মাসের তালিকা ও Today's Collection-ও এটাই
                // ব্যবহার করে) — তাই কোনো হিসাব বা নিয়ম বদলায়নি, শুধু কম ঘর নামে।
                // 🔒 সরু পড়া ব্যর্থ হলে অ্যাপ নিজেই আগের মতো সব ঘর চেয়ে নেয়;
                //    সত্যিকারের ব্যর্থতায় `null` ফেরে, তাই জমানো বোর্ডে ফিরে
                //    যাওয়ার নিয়মটা (নিচে) হুবহু আগের মতোই কাজ করে।
                SupabaseClient.fetchListSlimOrNull("payments", "date=eq.$date$branchPart", 5000, SupabaseClient.PAYMENT_COLS_LIST)
            }
            val enquiriesDef = async(Dispatchers.IO) {
                if (enquiriesOverride != null) enquiriesOverride else
                SupabaseClient.fetchListOrNull("enquiries", "date=eq.$date$branchPart", 5000)
            }
            val patientsDef = async(Dispatchers.IO) {
                if (patientsOverride != null) patientsOverride else
                // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): the
                // board shows no patient photo anywhere, yet every row's photo
                // was being downloaded. PATIENT_COLS_NO_PHOTO is every column
                // of the table EXCEPT "photo", the filter/limit/order are
                // untouched, and a failed narrowed read falls back to every
                // column by itself -- so a genuine failure is still null,
                // exactly as the cached-board fallback below expects.
                SupabaseClient.fetchListSlimOrNull("patients", "registrationDate=eq.$date$branchPart", 5000, SupabaseClient.PATIENT_COLS_NO_PHOTO)
            }
            val followUpsDef = async(Dispatchers.IO) {
                if (followUpsOverride != null) followUpsOverride else
                // 🔒 কোটা/গতি (29.07.2026, খাতার সারি B105): উপরের `patients`
                // পড়াটা আগেই ছবি বাদ দিয়ে করা হয়েছিল, কিন্তু `followups`-এও
                // **ঠিক একই `photo` ঘর** আছে — সেটা তখন বাদ পড়েনি। এই বোর্ডে
                // কোথাও ছবি দেখানো হয় না (উপরের মন্তব্যেই লেখা আছে)।
                // ⛔ শুধু `photo` বাদ — বাকি সব ঘর, ছাঁকনি, limit অপরিবর্তিত;
                //    সরু পড়া ব্যর্থ হলে নিজে থেকেই সব ঘর চেয়ে নেয়, তাই আসল
                //    ব্যর্থতা আগের মতোই `null` — ক্যাশ-বোর্ডের ফলব্যাক অক্ষত।
                //
                // 🔒 খাতার সারি B110 (TK, 29.07.2026 বিকেল ৪.৫০): *"Reject ·
                // Delete · Registration Cancel হলে টাকার পরিমাণও যেন না দেখায় —
                // টাকা তো ফেরত দিতে হয়েছে, নইলে দিনের শেষে হিসাব মিলবে না।"*
                // সেটা বুঝতে হলে **বাতিল হওয়া সারিগুলোও** জানা দরকার। তাই
                // `status`-এর ছাঁকনিটা ক্লাউড থেকে তুলে নিয়ে **নিচে কোডে** বসানো
                // হলো — ⛔ **অনুরোধের সংখ্যা ঠিক আগের মতোই একটাই**, শুধু কয়েকটা
                // বাতিল সারি বেশি নামে। পুরনো ব্যবহারের জায়গাগুলো নিচে হুবহু
                // আগের মতোই শুধু Active সারি পায় (`allFollowUpsList`)।
                // 🟢🔒 B661 (15.08.2026, TK-অনুমোদিত · Egress-৩): এই তালিকা থেকে কোডে মাত্র
                //   ৭টা ঘর পড়া হয় (status · id · nextFollow · mobile · stage · lastRemark ·
                //   branch), অথচ ২৫টা ঘর নামত — সবচেয়ে লম্বা `history` লেখাটাও। এখন শুধু
                //   দরকারি ঘরগুলোই নামে (FOLLOWUP_COLS_CHAMBER_BOARD)।
                //   ⛔ সারি · ছাঁকনি · limit · সাজানো কিছুই বদলায়নি — বোর্ডের হিসাব হুবহু এক।
                SupabaseClient.fetchListSlimOrNull("followups", branchPart.removePrefix("&").ifBlank { null }, 5000, SupabaseClient.FOLLOWUP_COLS_CHAMBER_BOARD)
            }
            listOf(paymentsDef.await(), enquiriesDef.await(), patientsDef.await(), followUpsDef.await())
        }

        // Step 1: Money that moved today -- both Fees (visit_fee/registration) and
        //    Payment (treatment) live in the SAME "payments" table, already.
        // TK-REQUESTED FIX (2026-07-23): same root cause/fix as Follow-up's
        // Bill/Due bug -- fetchList() swallows a failed request into a
        // silent empty array, which used to get baked straight into
        // today's board (and then CACHED at the end of this function),
        // making the board look wrongly empty on a bad connection, possibly
        // stuck that way. Now uses fetchListOrNull(): a genuine failure
        // (null) falls back to the last cached board instead of computing/
        // caching a wrong one; if there's no cache yet, falls through to
        // empty exactly like before (no behavior change for a first-ever
        // load). A real empty result (successful fetch, genuinely no
        // payments yet today) is NOT affected -- only a true failure is.
        val paymentsRaw = fetched[0]
        if (paymentsRaw == null) loadCachedBoard(context, date, branchFilter)?.let { return it }
        var payments = paymentsRaw ?: org.json.JSONArray()
        // TK-REQUESTED ADDITION (2026-07-18): merge in any locally-pending
        // payment (most importantly a just-tapped "Mark Arrived" that
        // hasn't reached the cloud yet) for TODAY, so this device's own
        // board never looks like the tap "did nothing" while offline.
        context?.let { ctx ->
            try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("payments", payments) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
            val pending = LocalWorkflowStore(ctx).pendingPayments()
            val idPosition = HashMap<String, Int>()
            for (i in 0 until payments.length()) {
                val existingId = payments.getJSONObject(i).optString("id")
                if (existingId.isNotBlank()) idPosition[existingId] = i
            }
            val merged = org.json.JSONArray()
            for (i in 0 until payments.length()) merged.put(payments.getJSONObject(i))
            for (i in 0 until pending.length()) {
                val p = pending.getJSONObject(i)
                val id = p.optString("id")
                val sameDay = p.optString("date") == date
                val branchOk = allBranch || p.s("branch").equals(branchFilter, ignoreCase = true)
                if (!sameDay || !branchOk || id.isBlank()) continue
                // TK-REPORTED BUG FIX (2026-07-20): same root cause as
                // Follow-up's remark bug -- a pending edit to a payment
                // already present in the cloud result (e.g. Edit Payment)
                // used to be dropped instead of replacing the stale row.
                val existingPos = idPosition[id]
                if (existingPos != null) {
                    // 🔴🔒 V1306 (তালিকা ৪২২, রুল ৭): ফোনের কপি নতুন হলে তবেই তার ঘরগুলো ক্লাউডের সারির উপরে; পুরো সারি বদলানো নয়।
                    val base = merged.getJSONObject(existingPos)
                    val ls = p.optString("updatedAt", ""); val cs = base.optString("updatedAt", "")
                    if (ls.isNotBlank() && (cs.isBlank() || ls >= cs)) {
                        val ov = JSONObject(base.toString()); val ks = p.keys()
                        while (ks.hasNext()) { val k = ks.next(); ov.put(k, p.opt(k)) }
                        merged.put(existingPos, ov)
                    }
                }
                else { idPosition[id] = merged.length(); merged.put(p) }
            }
            payments = merged
        }

        // Step 2: New enquiries / new registrations dated today -- "arrived" signal
        //    for someone who hasn't necessarily paid anything yet.
        // TK-REQUESTED FIX (2026-07-23): same failure/empty distinction as
        // the payments fetch above.
        val enquiriesRaw = fetched[1]
        if (enquiriesRaw == null) loadCachedBoard(context, date, branchFilter)?.let { return it }
        var enquiries = enquiriesRaw ?: org.json.JSONArray()
        // TK-REQUESTED FIX (2026-07-19): same reasoning as the payments merge
        // above -- a just-created Enquiry that hasn't synced to the cloud yet
        // (offline/slow network) used to be invisible on today's board until
        // it synced. Now any locally-pending Enquiry for this date/branch is
        // merged in too, so it never has to wait on the network to show up.
        context?.let { ctx ->
            val pending = LocalWorkflowStore(ctx).pendingEnquiries()
            val idPosition = HashMap<String, Int>()
            for (i in 0 until enquiries.length()) {
                val existingId = enquiries.getJSONObject(i).optString("id")
                if (existingId.isNotBlank()) idPosition[existingId] = i
            }
            val merged = org.json.JSONArray()
            for (i in 0 until enquiries.length()) merged.put(enquiries.getJSONObject(i))
            for (i in 0 until pending.length()) {
                val p = pending.getJSONObject(i)
                val id = p.optString("id")
                val sameDay = p.optString("date").take(10) == date
                val branchOk = allBranch || p.s("branch").equals(branchFilter, ignoreCase = true)
                if (!sameDay || !branchOk || id.isBlank()) continue
                val existingPos = idPosition[id]
                if (existingPos != null) {
                    // 🔴🔒 V1306 (তালিকা ৪২২, রুল ৭): ফোনের কপি নতুন হলে তবেই তার ঘরগুলো ক্লাউডের সারির উপরে; পুরো সারি বদলানো নয়।
                    val base = merged.getJSONObject(existingPos)
                    val ls = p.optString("updatedAt", ""); val cs = base.optString("updatedAt", "")
                    if (ls.isNotBlank() && (cs.isBlank() || ls >= cs)) {
                        val ov = JSONObject(base.toString()); val ks = p.keys()
                        while (ks.hasNext()) { val k = ks.next(); ov.put(k, p.opt(k)) }
                        merged.put(existingPos, ov)
                    }
                }
                else { idPosition[id] = merged.length(); merged.put(p) }
            }
            enquiries = merged
        }
        // TK-REQUESTED FIX (2026-07-23): same failure/empty distinction as above.
        val patientsRaw = fetched[2]
        if (patientsRaw == null) loadCachedBoard(context, date, branchFilter)?.let { return it }
        var patients = patientsRaw ?: org.json.JSONArray()
        // TK-REQUESTED FIX (2026-07-19): same fix as above, for a just-saved
        // Registration that hasn't synced yet.
        context?.let { ctx ->
            try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("patients", patients) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
            val pending = LocalWorkflowStore(ctx).pendingPatients()
            val idPosition = HashMap<String, Int>()
            for (i in 0 until patients.length()) {
                val existingId = patients.getJSONObject(i).optString("id")
                if (existingId.isNotBlank()) idPosition[existingId] = i
            }
            val merged = org.json.JSONArray()
            for (i in 0 until patients.length()) merged.put(patients.getJSONObject(i))
            for (i in 0 until pending.length()) {
                val p = pending.getJSONObject(i)
                val id = p.optString("id")
                val sameDay = p.optString("registrationDate").take(10) == date
                val branchOk = allBranch || p.s("branch").equals(branchFilter, ignoreCase = true)
                if (!sameDay || !branchOk || id.isBlank()) continue
                val existingPos = idPosition[id]
                if (existingPos != null) {
                    // 🔴🔒 V1306 (তালিকা ৪২২, রুল ৭): ফোনের কপি নতুন হলে তবেই তার ঘরগুলো ক্লাউডের সারির উপরে; পুরো সারি বদলানো নয়।
                    val base = merged.getJSONObject(existingPos)
                    val ls = p.optString("updatedAt", ""); val cs = base.optString("updatedAt", "")
                    if (ls.isNotBlank() && (cs.isBlank() || ls >= cs)) {
                        val ov = JSONObject(base.toString()); val ks = p.keys()
                        while (ks.hasNext()) { val k = ks.next(); ov.put(k, p.opt(k)) }
                        merged.put(existingPos, ov)
                    }
                }
                else { idPosition[id] = merged.length(); merged.put(p) }
            }
            patients = merged
        }

        // TK-REPORTED BUG FIX (2026-07-25): the query above only ever
        // fetched patients REGISTERED today, so a returning patient (paid/
        // arrived today, registered on an earlier day) never had a row
        // here -- their PATIENT box showed Name+Mobile only, no ID, even
        // though they have a real one. Fixed with ONE extra batched query
        // (never per-patient -- same quota caution TK asked for on this
        // board) using the patientId UUIDs already sitting on today's
        // payments rows, for whichever of those aren't already covered by
        // the fetch above.
        run {
            val knownIds = HashSet<String>()
            for (i in 0 until patients.length()) {
                val id = patients.getJSONObject(i).optString("id")
                if (id.isNotBlank()) knownIds.add(id)
            }
            val missingIds = HashSet<String>()
            for (i in 0 until payments.length()) {
                val pid = payments.getJSONObject(i).optString("patientId")
                if (pid.isNotBlank() && pid !in knownIds) missingIds.add(pid)
            }
            if (missingIds.isNotEmpty()) {
                val idList = missingIds.joinToString(",")
                // Same as above -- no photo is ever shown on this board.
                val extra = SupabaseClient.fetchListSlim("patients", "id=in.($idList)", missingIds.size, SupabaseClient.PATIENT_COLS_NO_PHOTO)
                if (extra.length() > 0) {
                    val merged2 = org.json.JSONArray()
                    for (i in 0 until patients.length()) merged2.put(patients.getJSONObject(i))
                    for (i in 0 until extra.length()) merged2.put(extra.getJSONObject(i))
                    patients = merged2
                }
            }
        }
        //    for followUpId (so the Treatment/remark button works) and Last
        //    Remark below. TK-REQUESTED CHANGE (2026-07-19): this used to ALSO
        //    be the source of "Expected" (nextFollow date match); that is now
        //    driven entirely by the deliberate "Mark Expected" button instead
        //    (see markExpected() and the payments loop below) -- nextFollow
        //    is no longer read for Expected at all.
        //
        //    DEEP-AUDIT FIX (2026-07-16): this used to call
        //    FollowUpRepository.fetchTab() three times (once per stage).
        //    fetchTab() is built for the Follow-up screen's own needs and
        //    does much more work than this board needs per call -- a
        //    higher-stage-exclusion check (its own extra Supabase query)
        //    every time, and for the Patient/Treatment stages, a full
        //    "patients" + "payments" join (two MORE Supabase queries) just
        //    to compute a bill/paid figure this board never uses. Calling
        //    it three times meant roughly a dozen Supabase requests for a
        //    single board load -- a real, avoidable load on the free-tier
        //    quota TK asked to be careful about. Replaced with ONE direct,
        //    read-only "followups" query instead (branch-scoped the same
        //    way, active-status-only the same way) -- same information,
        //    4 total Supabase calls for the whole board instead of ~12.
        // TK-REQUESTED FIX (2026-07-23): same failure/empty distinction as
        // the other 3 fetches above.
        val allFollowUpsRaw = fetched[3]
        if (allFollowUpsRaw == null) loadCachedBoard(context, date, branchFilter)?.let { return it }
        val allFollowUpsFetched = allFollowUpsRaw ?: org.json.JSONArray()
        // 🔒 খাতার সারি B110: উপরের পড়াটা এখন **বাতিল সারিগুলোও** নিয়ে আসে
        // (অনুরোধ একটাই, আগের মতোই)। নিচের পুরনো সব কাজ যাতে **এক চুলও না
        // বদলায়**, তাই এখানেই আগের সেই ছাঁকনিটা বসানো হলো — `allFollowUpsList`
        // ঠিক আগের মতোই শুধু Active সারি পায়।
        val allFollowUps = org.json.JSONArray()
        for (i in 0 until allFollowUpsFetched.length()) {
            val r = allFollowUpsFetched.optJSONObject(i) ?: continue
            val st = r.s("status").ifBlank { "Active" }
            if (!st.equals("Cancelled", true) && !st.equals("Incomplete", true) &&
                !st.equals("Rejected", true) && !st.equals("Closed", true)) allFollowUps.put(r)
        }
        // 🔒🔒 খাতার সারি B110 (TK, 29.07.2026 বিকেল ৪.৫০ — স্থায়ী নিয়ম):
        // *"Reject হোক · Delete হোক · Registration Cancel হোক — এটা করেছে মানে
        //  পেশেন্টকে টাকাটা ফেরত দিতে হয়েছে। তাহলে সেই টাকা এখানে কেন দেখাবে?
        //  নইলে দিনের শেষে হিসাব মিলবে না।"*
        //
        // **কাকে \"ফেরত দেওয়া\" ধরা হবে (TK-এর সিদ্ধান্ত, বিকেল ৫.২০):** যে নম্বরের
        // অন্তত একটা `Cancelled` সারি আছে, আর `Cancelled` ছাড়া অন্য কোনো সারিই নেই।
        // ⛔ **Reject / Registration Cancel / Delete** → টাকা বাদ।
        // ⛔ **Treatment \"Incomplete\" → টাকা আগের মতোই গোনা হয়** (TK: চিকিৎসা
        //    অসম্পূর্ণ হলেও টাকা ফেরত দেওয়া হয় না, ক্লিনিকেই থাকে)।
        // ⛔ Reject-এর পরে আবার রেজিস্টার হলে টাকা আগের মতোই গোনা হয়।
        // ⛔ **কোনো টাকার সারি মোছা হয় না** (TK-এর সিদ্ধান্ত) — সারিটা ডেটাবেসে
        //    ও Payment History-তে থাকে, শুধু এই বোর্ডের FEES/CASH/ONLINE ঘরে ও
        //    দিনের মোট হিসাবে ধরা হয় না।
        // ⛔ **পুরনো দিনেও একই নিয়ম** (TK-এর সিদ্ধান্ত) — হিসাবটা প্রতিবার
        //    সারির অবস্থা দেখে হয়, তাই কোনো SQL বা এককালীন সংশোধন লাগে না।
        // ⛔ বাড়তি ক্লাউড-কল নেই — এই তালিকাটা উপরের ওই একটাই পড়া থেকেই আসে।
        // ⛔ হিসাবটা একটাই জায়গায় লেখা (`RefundedRecords`), যাতে চেম্বার বোর্ড ও
        //    Today's Collection কখনো দুই রকম হিসাব না দেখায়।
        // 🔵🔒 B621 (11.08.2026, TK-নির্দেশ): আগে এখানে fromRows(base) ব্যবহার হতো, তাই
        // Reject-এর পরে আবার রেজিস্টার-করা রোগীর আসল টাকা চেম্বার বোর্ডেও লুকিয়ে যেত —
        // অথচ Today Collection-এ (fetch) ঠিক দেখাত (দুই পর্দা দুই হিসাব, নিয়মভঙ্গ)। এখন
        // এখানেও **হুবহু একই** RefundedRecords.fetch ব্যবহার হয় (patients-যাচাই সহ) — তাই
        // চেম্বার বোর্ড ও Today's Collection সবসময় এক হিসাব দেখায়। fetch fail-safe (ব্যর্থ
        // হলে ফাঁকা → কারও টাকা বাদ যায় না)।
        val refundedMobiles = RefundedRecords.fetch(branchFilter)
        val allFollowUpsList = (0 until allFollowUps.length()).mapNotNull { i -> allFollowUps.optJSONObject(i) }.toMutableList()
        // TK-REQUESTED FIX (2026-07-19): same fix as Step 1/2 above -- merge in
        // any locally-pending followups row not yet synced, so a just-saved
        // Enquiry/Registration/Advance's OWN followups row (needed for
        // followUpId/Last Remark above) doesn't have to wait on the network.
        context?.let { ctx ->
            try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("followups", allFollowUpsList) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
            val pending = LocalWorkflowStore(ctx).pendingFollowUps()
            val idPosition = HashMap<String, Int>()
            for (i in allFollowUpsList.indices) {
                val existingId = allFollowUpsList[i].optString("id")
                if (existingId.isNotBlank()) idPosition[existingId] = i
            }
            for (i in 0 until pending.length()) {
                val p = pending.getJSONObject(i)
                val id = p.optString("id")
                val status = p.s("status").ifBlank { "Active" }
                if (status.equals("Cancelled", true) || status.equals("Incomplete", true) ||
                    status.equals("Rejected", true) || status.equals("Closed", true)) continue
                val branchOk = allBranch || p.s("branch").equals(branchFilter, ignoreCase = true)
                if (!branchOk || id.isBlank()) continue
                val existingPos = idPosition[id]
                if (existingPos != null) allFollowUpsList[existingPos] = p
                else { idPosition[id] = allFollowUpsList.size; allFollowUpsList.add(p) }
            }
        }
        // TK-REQUESTED CHANGE (2026-07-19): this list is no longer used to
        // decide "expected" (see markExpected()/payments loop below) -- kept
        // only as a source for followUpId + Last Remark below, which still
        // needs today's Follow-up rows regardless of the new Expected logic.
        val todaysFollowUps = allFollowUpsList.filter { it.s("nextFollow").take(10) == date }

        /* ══════════════════════════════════════════════════════════════════
           🔴🔴🔒 V526 (২২.০৮.২০২৬, TK-নির্দেশ) — **এক নম্বরে দুজন রোগী থাকলে
           বোর্ডেও দুটো আলাদা সারি।**

           **সমস্যা যেটা ছিল (কোডে প্রমাণিত):** এই বোর্ড সারি বানাত
           **শুধু মোবাইল নম্বর ধরে** (`byMobile`)। V516-এর পরে এক নম্বরে
           স্বামী ও স্ত্রী **দুজন আলাদা রোগী** থাকতে পারেন — তখন দুজনের
           নাম · Patient ID · টাকা · রিমার্ক সব **একটাই সারিতে মিশে** যেত।

           **সমাধান — "পরিচয়ের চাবি" (V518/V520-এর হুবহু একই প্রমাণিত নিয়ম):**
           স্টাফ নিজে *"Different Patient — Same Mobile"* চাপলে তবেই রোগীর
           সারির আইডি হয় `pat_<১০ সংখ্যা>_<লেজ>` ধাঁচের — অন্য কোনো পথে এই
           ধাঁচ তৈরি হয় না (`PatientModel.newRowIdForSameMobile`)।
             · এই ধাঁচের রোগী ⇒ চাবি হবে **তাঁর নিজের আইডি** (আলাদা সারি)
             · বাকি সবাই ⇒ চাবি **আগের মতোই মোবাইল** (এক অক্ষরও বদলায়নি)

           ⛔ **যে নম্বরে ঘোষিত আলাদা রোগী নেই, সেখানে প্রতিটা চাবি হুবহু
              মোবাইলই থাকে — অর্থাৎ পুরো বোর্ড অবিকল আগের মতোই চলে।**
              (পরীক্ষায় হাতে-কলমে প্রমাণিত।)
           ⛔ **বাড়তি একটাও cloud-read নেই** — যে তালিকাগুলো এমনিতেই আনা হয়
              (`patients`) সেগুলো থেকেই চিহ্নটা বের করা হয়।
           ⛔ Enquiry-র সারিতে রোগীর আইডি থাকেই না, তাই সেগুলো আগের মতোই
              মোবাইল ধরে বসে — ঠিকই আছে, এনকোয়ারি তো এখনো রোগীই হননি।
           ══════════════════════════════════════════════════════════════════ */
        val declaredSeparateIds = HashSet<String>()
        for (i in 0 until patients.length()) {
            val row = patients.optJSONObject(i) ?: continue
            val rid = row.s("id")
            if (rid.isNotBlank() && PatientModel.isDeclaredSeparateRowId(rid, digits(row.s("mobile")))) {
                declaredSeparateIds.add(rid)
            }
        }

        /** এই সারিটা কার — ঘোষিত আলাদা রোগী হলে তাঁর আইডি, নইলে মোবাইল। */
        fun keyFor(mobile: String, patientRowId: String): String {
            val m = digits(mobile)
            return if (patientRowId.isNotBlank() && declaredSeparateIds.contains(patientRowId)) patientRowId else m
        }

        // Build one row per unique patient across all of the above.
        val byMobile = LinkedHashMap<String, MutableMap<String, Any?>>()

        fun ensure(key: String, mobile: String, name: String, branch: String, disease: String) {
            val m = digits(mobile)
            if (m.isBlank() || key.isBlank()) return
            byMobile.getOrPut(key) {
                mutableMapOf(
                    // 🔵 V526: চাবি আর মোবাইল এক নাও হতে পারে (ঘোষিত আলাদা রোগী),
                    //    তাই আসল ১০-সংখ্যার নম্বরটা এখানেই আলাদা করে রাখা হয় —
                    //    নিচে সারি বানানোর সময় **এটাই** ব্যবহার হয়, চাবি নয়।
                    "mobile10" to m,
                    // 🔵 V526: এই সারিটা কোন রোগীর (ঘোষিত আলাদা হলে তাঁর আইডি)।
                    "patientRowId" to "",
                    "name" to name, "mobile" to mobile, "branch" to branch, "disease" to disease,
                    "expected" to false, "arrived" to false,
                    "feesCash" to 0.0, "feesOnline" to 0.0, "paymentCash" to 0.0, "paymentOnline" to 0.0,
                    "medicineCash" to 0.0, "medicineOnline" to 0.0,   // 🟢🔒 V612
                    "refundCash" to 0.0, "refundOnline" to 0.0,   // 🔴🔒 V709
                    "remark" to "", "happened" to mutableListOf<String>(), "followUpId" to "", "patientId" to "",
                    "arrivedAt" to "", "refDoctor" to "", "refBy" to "",   /* 🔴🔒 V933 */
                    // TK-REQUESTED (2026-07-24): itemized payment lines for
                    // the new live-screen "Payment" box (e.g. "Fees-400/-
                    // Cash", "2000/- Cash", "1000/- Medicine") -- purely
                    // additive alongside the existing summed feesCash/
                    // paymentCash/etc totals below, which are UNCHANGED and
                    // still what Save/Print/Report Card use.
                    "paymentLines" to mutableListOf<String>()
                )
            }
            // Fill in a better name/branch/disease if this call has one and
            // the stored row doesn't yet (first-writer keeps priority
            // otherwise, so nothing already-shown ever gets blanked).
            val row = byMobile[key]!!
            if ((row["name"] as String).isBlank() && name.isNotBlank()) row["name"] = name
            if ((row["branch"] as String).isBlank() && branch.isNotBlank()) row["branch"] = branch
            if ((row["disease"] as String).isBlank() && disease.isNotBlank()) row["disease"] = disease
        }

        // TK-DECISION (2026-07-22): nextFollow date no longer makes anyone
        // "expected" here. TK's reasoning: an Enquiry is uncertain (out of
        // 1000 enquiries maybe 15 actually come), so a nextFollow reminder
        // must never auto-fill the আসার কথা list; and re-reading followups on
        // every board open is a Supabase free-plan quota risk. "Expected" now
        // comes ONLY from a deliberate "আসার কথা"/Mark-Expected action, which
        // writes a chamber_expected row ONCE (handled in the payments loop
        // below). For real patients (Visit/Patient/Treatment), that write is
        // made automatically the moment their nextFollow is SAVED (see
        // FollowUpActivity.pickNextFollow / PatientTimeline nextFollow save)
        // -- so it still feels automatic for them, but costs zero extra board
        // reads. todaysFollowUps is kept below only for followUpId + Last
        // Remark, exactly as before.

        for (i in 0 until enquiries.length()) {
            val row = enquiries.optJSONObject(i) ?: continue
            /* 🔵 V526: এনকোয়ারির সারিতে রোগীর আইডি থাকেই না, তাই চাবি
               **আগের মতোই মোবাইল** — এই অংশ এক অক্ষরও বদলায়নি। */
            val m = keyFor(row.s("mobile"), "")
            ensure(m, row.s("mobile"), row.s("name"), row.s("branch"), row.s("disease"))
            (byMobile[m]?.get("happened") as? MutableList<String>)?.add("New Enquiry")
        }

        // TK-REQUESTED (2026-07-27), ধাপ ৩খ — চেম্বার পর্দা: when the same person
        // has more than one patients row, the Patient ID printed on the chamber
        // register was simply the first row's, while the money screen, Patient
        // Details, the Report Card and Draft all resolve the person's real row
        // with the ONE shared rule. The register is a paper document the patient
        // keeps, so it must not carry a different Patient ID than the app shows.
        // Same rule here now; with a single row nothing changes.
        val chamberChosenByMobile = HashMap<String, org.json.JSONObject>()
        run {
            val grouped = HashMap<String, org.json.JSONArray>()
            for (i in 0 until patients.length()) {
                val row = patients.optJSONObject(i) ?: continue
                val m = digits(row.s("mobile"))
                if (m.isBlank()) continue
                /* 🔵🔒 V526: এই দলটার কাজ — "ভুলে দুবার রেজিস্ট্রেশন হলে কোন
                   সারিটা আসল" সেটা ঠিক করা (V143-এর নিয়ম)। ঘোষিত আলাদা রোগী
                   ওই দলের কেউ নন — তিনি নিজেই আলাদা মানুষ। তাঁকে দলে ঢোকালে
                   তাঁর Patient ID অন্যজনের সারিতে বসে যেতে পারত।
                   ⛔ ঘোষিত আলাদা রোগী না থাকলে এই লাইন কিছুই করে না। */
                if (declaredSeparateIds.contains(row.s("id"))) continue
                grouped.getOrPut(m) { org.json.JSONArray() }.put(row)
            }
            for ((m, rows) in grouped) {
                val chosen = PatientIdentity.pickPatientRow(rows, branchFilter.orEmpty())
                if (chosen != null) chamberChosenByMobile[m] = chosen
            }
        }

        for (i in 0 until patients.length()) {
            val row = patients.optJSONObject(i) ?: continue
            /* 🔵🔒 V526: রোগীর সারিতে আইডি আছে — ঘোষিত আলাদা রোগী হলে
               চাবি তাঁর নিজের আইডি, নইলে আগের মতোই মোবাইল। */
            val rid = row.s("id")
            val m = keyFor(row.s("mobile"), rid)
            ensure(m, row.s("mobile"), row.s("name"), row.s("branch"), row.s("disease"))
            byMobile[m]?.set("arrived", true)
            // 🔵 V526: এই সারিটা কার — Payment/History/Report Card ঠিক রোগীতে
            //    যাওয়ার জন্য (first-writer-wins, বোর্ডের চিরকালের নিয়ম)।
            if ((byMobile[m]?.get("patientRowId") as? String).isNullOrBlank() && rid.isNotBlank()) {
                byMobile[m]?.set("patientRowId", rid)
            }
            // TK-REQUESTED ADDITION (2026-07-19): Patient ID, needed for the
            // A4 register print (ChamberRegisterPdfBuilder) alongside
            // Name/Mobile.
            /* 🔵🔒 V526: ঘোষিত আলাদা রোগী হলে "দলে জেতা সারি" নয় — **তাঁর
               নিজের সারিই**, নইলে তাঁর কার্ডে অন্যজনের Patient ID বসে যেত।
               ⛔ সাধারণ রোগীর ক্ষেত্রে হুবহু আগের পথ। */
            val chosenRow = if (declaredSeparateIds.contains(rid)) row
                else chamberChosenByMobile[digits(row.s("mobile"))]
            val pid = (chosenRow?.s("patientId") ?: "").ifBlank { row.s("patientId") }
            if (pid.isNotBlank() && (byMobile[m]?.get("patientId") as? String).isNullOrBlank()) {
                byMobile[m]?.set("patientId", pid)
            }
            // 🔴🔒 V471 (20.08.2026, TK-অনুমোদিত) — রেফারিং RMP-র নাম, একই
            // "জিতে যাওয়া সারি" (chamberChosenByMobile) থেকে — যাতে Money
            // স্ক্রিন/Patient Details-এর সাথে সবসময় একই নাম দেখায়।
            val refDoc = (chosenRow?.s("refDoctor") ?: "").ifBlank { row.s("refDoctor") }
            if (refDoc.isNotBlank() && (byMobile[m]?.get("refDoctor") as? String).isNullOrBlank()) {
                byMobile[m]?.set("refDoctor", refDoc)
            }
            /* 🔴🔒 V933 — RMP-র নাম না লেখা থাকলেও "Referred By" ঘরটা বলে দেয়
               রোগীকে RMP/ডাক্তার পাঠিয়েছেন। হুবহু একই "জিতে যাওয়া সারি" থেকে,
               কোনো বাড়তি cloud-কল নেই। */
            val refByVal = (chosenRow?.s("refBy") ?: "").ifBlank { row.s("refBy") }
            if (refByVal.isNotBlank() && (byMobile[m]?.get("refBy") as? String).isNullOrBlank()) {
                byMobile[m]?.set("refBy", refByVal)
            }
            // TK-REQUESTED ADDITION (2026-07-19): earliest real timestamp
            // for this row, used to print in actual chronological order
            // (not alphabetical) -- first-writer-wins so the true earliest
            // event of the day always sticks.
            // 🔴🔴🔒 V654 (২৫.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "নামের নিচের
            // তারিখ/সময় আজকেরটা না, প্রথম-ভিজিটের পুরনো তারিখ দেখাচ্ছে")
            // — **আসল কারণ (কোড ধরে যাচাই, Sukanta Roy-র Timeline দিয়ে
            // প্রমাণিত):** এই লুপ `patients` টেবিলের **প্রতিটা** সারিতে
            // চলে — শুধু আজ নতুন-রেজিস্ট্রেশন নয়, আজ চেম্বারে থাকা **সব**
            // (পুরনো) রোগীও এতে থাকেন। আগে এখানে `row.s("createdAt")`
            // (রোগীর সারি **কবে তৈরি হয়েছিল**, অর্থাৎ তাঁর **প্রথম**
            // রেজিস্ট্রেশনের সময়) নিঃশর্তে `arrivedAt`-এ বসত — পুরনো
            // রোগীর জন্য এটা সপ্তাহ/মাস আগের তারিখ! আর যেহেতু এই লুপ
            // Payment-লুপের **আগে** চলে (first-writer-wins), এই ভুল পুরনো
            // তারিখটাই "জিতে" যেত, আজকের আসল পেমেন্ট-সময়কে কখনো বসতে
            // দিত না — ঠিক TK-এর স্ক্রিনশটে যা দেখা গেছে।
            // **সমাধান:** `createdAt` তখনই fallback হিসেবে ব্যবহার হবে
            // যখন এই সারিটা **সত্যিই আজই তৈরি হয়েছে** (`createdAt`-এর
            // তারিখ অংশ আজকের `date`-এর সাথে মেলে) — পুরনো রোগীর জন্য
            // এই fallback আর কখনো ভুল পুরনো তারিখ বসাবে না, Payment-লুপের
            // সত্যিকারের আজকের timestamp-ই এখন ঠিকভাবে বসবে।
            // ⛔ নতুন রেজিস্ট্রেশনের (আজই তৈরি) ক্ষেত্রে আচরণ অপরিবর্তিত।
            val createdAt = row.s("createdAt")
            if (createdAt.isNotBlank() && createdAt.take(10) == date &&
                (byMobile[m]?.get("arrivedAt") as? String).isNullOrBlank()) {
                byMobile[m]?.set("arrivedAt", createdAt)
            }
            (byMobile[m]?.get("happened") as? MutableList<String>)?.add("New Registration")
        }

        // 🔴🔒 V687 (২৫.০৮.২০২৬, TK-নির্দেশ, গভীরে যাচাই করে — "Treatment
        // Progress ঘরে আজকের লেখা নয়, আগের কল/ভিজিটের রিমার্ক দেখাচ্ছে")।
        // **আসল কারণ:** এই বোর্ডের "remark" এতদিন `followups.lastRemark`
        // থেকে আসত — যে কলামে ফোন-কল লগও (Follow-up screen থেকে "CALL NOT
        // RECEIVED" ইত্যাদি) সেভ হয়, একই কলামে। যেটা সবচেয়ে শেষে সেভ
        // হয়েছে সেটাই দেখাত — চেম্বারের ট্রিটমেন্ট নোট হোক বা ফোন-কলের
        // নোট, তারিখ মিললেই আলাদা করা যেত না।
        // **প্রমাণিত সঠিক উৎস:** `payments.progress` — V590-এ ইতিমধ্যে
        // বানানো, শুধু চেম্বার-ট্রিটমেন্ট Save হলেই বসে (`syncProgressToReportCard`),
        // ফোন-কল কখনো এই ঘর ছোঁয় না। এই একই ঘর Report Card পড়ে, তাই এখন
        // বোর্ড ও Report Card **একই সোর্স** থেকে আসছে — কখনো আলাদা হবে না।
        // ⛔ কোনো নতুন cloud-কল লাগেনি — নিচের payments-লুপেই (আগে থেকে
        //    আনা) `progress` ঘরটা পড়া হচ্ছে।
        val hasProgressToday = HashSet<String>()

        for (i in 0 until payments.length()) {
            val row = payments.optJSONObject(i) ?: continue
            if (digits(row.s("mobile")).isBlank()) continue
            /* 🔴🔵🔒 V526: টাকার সারিতে মালিকের আইডি **সবসময়** লেখা থাকে
               (`payments.patientId` = রোগীর সারির আইডি — V520-এ কোডে প্রমাণিত)।
               তাই টাকাটা ঠিক যাঁর, তাঁর সারিতেই বসে — স্বামীর টাকা আর
               স্ত্রীর সারিতে যাবে না।
               ⛔ ঘোষিত আলাদা রোগী না থাকলে চাবি হুবহু মোবাইলই — টাকার
                  যোগফল ও প্রতিটা সারি অবিকল আগের মতোই। */
            val m = keyFor(row.s("mobile"), row.s("patientId"))
            ensure(m, row.s("mobile"), row.s("name"), row.s("branch"), "")
            val entry = byMobile[m] ?: continue
            // 🔴🔒 V687 — এই সারি (আজকের/এই বোর্ডের দিনের payments) যদি
            // `progress` নিয়ে আসে (writeTreatment-এর syncProgressToReportCard
            // এটা বসায়), সেটাই এই বোর্ডের "remark" — ফোন-কল কখনো এই ঘর
            // লেখে না, তাই এখানে এলে সত্যিই আজকের চেম্বার-নোট।
            // 🔴🔒 V696 (২৬.০৮.২০২৬, TK-এর ছবিতে ধরা — SERINA KHATTON-এর সারিতে
            //   TREATMENT PROGRESS-এ লেখা ছিল **"null"**)। আসল কারণ:
            //   `payments.progress` ঘরটা ফাঁকা (SQL NULL) হলে org.json-এর
            //   `optString()` **"null" লেখাটাই** ফেরত দেয় — আর সেটা
            //   `isNotBlank()` পাশ করে যাওয়ায় আসল লেখা ভেবে বসে যেত।
            //   ⛔ এই ফাঁদের জন্যই `JsonExt.s()` বানানো ছিল, এখানে ব্যবহার
            //      হয়নি। ঠিক উপরের লাইনেই `row.s("mobile")` আছে।
            val progressToday = row.s("progress")
            if (progressToday.isNotBlank()) {
                entry["remark"] = progressToday
                // 🔴🔒 V687 — এই payments সারিই এই বোর্ডের নিজের `date`-এর
                // (তাই "আজকের", V654-এর সবুজ রং ঠিকভাবে দেখানোর জন্য)।
                entry["remarkUpdatedAt"] = date + "T00:00:00.000Z"
                hasProgressToday.add(m)
            }
            // 🚨 TK-REPORTED, LIVE (29.07.2026 বিকেল ৪.২১, ছবিসহ · খাতার সারি B109
            //     — MANISH PASWAN · 7258092776): *"Fees 400/- দিয়েছে দেখাচ্ছে,
            //     তাহলে Patient ID নেই কেন?"*
            //
            // **আসল কারণ (কোড ধরে, আন্দাজ নয়):** এই বোর্ডে Patient ID বসত
            // **শুধুমাত্র `patients` টেবিলের সারি থেকে** (উপরের লুপ)। ওই সারিটা
            // যদি না পাওয়া যায় — রেজিস্ট্রেশন বাতিল/ডিলিট হয়ে গেছে, অথবা
            // টাকার সারিতে `patientId` (UUID) নেই বলে উপরের ব্যাচ-খোঁজায় ধরা
            // পড়েনি — তখন ঘরটা **ফাঁকা** থেকে যেত, যদিও টাকার সারিতেই রোগীর
            // আসল ID (`patientCode`) বসানো আছে।
            //
            // **ওষুধ:** `patients` থেকে ID না পাওয়া গেলে **টাকার সারির নিজের
            // `patientCode`** ব্যবহার হবে। এটাই ওই টাকা নেওয়ার সময় লেখা হয়েছিল,
            // তাই এটা আন্দাজ নয় — রসিদে যা ছাপা হয়েছিল ঠিক তাই।
            // ⛔ `patients` থেকে ID পাওয়া গেলে **সেটাই আগের মতো জেতে** — এই
            //    লাইন তখন কিছুই করে না (first-writer-wins, বোর্ডের চিরকালের নিয়ম)।
            // ⛔ কোনো নতুন ক্লাউড-কল নেই · কোনো টাকার হিসাব বদলায় না ·
            //    ঘরটা ফাঁকা থাকলে আগের মতোই ফাঁকা থাকে।
            val payCode = row.s("patientCode")
            if (payCode.isNotBlank() && (entry["patientId"] as? String).isNullOrBlank()) {
                entry["patientId"] = payCode
            }
            val payType = row.optString("payType", "")
            // TK-REQUESTED ADDITION (2026-07-19): "Mark Expected" writes a
            // zero-amount payments row with this payType, dated to the
            // FUTURE date the staff picked -- this is now the ONLY source of
            // "expected". It must NEVER flip arrived or touch any Fees/
            // Payment total, same treatment as attendance_mark just below.
            if (payType == "chamber_expected") {
                entry["expected"] = true
                (entry["happened"] as? MutableList<String>)?.add("Marked Expected")
                continue
            }
            // TK-REQUESTED (2026-07-22): a "bill_edit" audit row is NOT the
            // patient arriving -- it must never flip Chamber "arrived" or touch
            // any total. Skip it entirely here (same as chamber_expected).
            if (payType == "bill_edit") continue
            // 🔴🔴🔴 TK-অডিট-অনুরোধ (01.08.2026, Follow-up-এর Due-ভুল ধরার পরে
            // সম্পূর্ণ প্রজেক্ট যাচাই): এই ফোনের Chamber Attendance বোর্ডে Refund
            // সারি (payType="refund") এতদিন সাধারণ Payment-এর মতোই Cash/Online-এ
            // যোগ হচ্ছিল, আর রোগীকে ভুলভাবে "Arrived"ও দেখাত — ঠিক যে বাগ ওয়েব
            // অ্যাপে (B250/B251) আগেই ধরা পড়ে ঠিক হয়েছিল, কিন্তু ফোনের এই বোর্ডে
            // তখন করা হয়নি। এখন একই নিয়ম: approved refund সত্যিই Cash/Online
            // থেকে বিয়োগ হয়, pending/rejected কোনো প্রভাব ফেলে না, আর refund
            // কখনো নিজে "Arrived" গণনা করে না। সারিটা Payment History-তে
            // অক্ষত থাকে — শুধু এই বোর্ডের যোগফলে গোনা হয় না/সঠিকভাবে বিয়োগ হয়।
            if (payType == "refund") {
                if (PaymentModel.isApprovedRefund(row)) {
                    val refundAmt = row.optDouble("amount", 0.0)
                    val refundCash = isCash(row.s("mode").ifBlank { "CASH" })
                    if (refundCash) entry["paymentCash"] = (entry["paymentCash"] as Double) - refundAmt
                    else entry["paymentOnline"] = (entry["paymentOnline"] as Double) - refundAmt
                    // 🔴🔒 V709 — উপরের বিয়োগটা **এক অক্ষরও বদলায়নি**; শুধু কত
                    //    বিয়োগ হলো সেটা আলাদা করে মনে রাখা হচ্ছে (Review-র
                    //    "Refund" লাইনের জন্য)।
                    if (refundCash) entry["refundCash"] = (entry["refundCash"] as? Double ?: 0.0) + refundAmt
                    else entry["refundOnline"] = (entry["refundOnline"] as? Double ?: 0.0) + refundAmt
                    (entry["happened"] as? MutableList<String>)?.add("Refunded ₹${"%,.0f".format(refundAmt)}")
                    (entry["paymentLines"] as? MutableList<String>)?.add("Refund -${"%,.0f".format(refundAmt)}/- ${if (refundCash) "Cash" else "Online"}")
                }
                continue
            }
            entry["arrived"] = true
            // TK-REQUESTED ADDITION (2026-07-19): earliest real timestamp
            // for this row (covers OLD patients whose Registration wasn't
            // today -- the first payment/mark-arrived of the day is their
            // only real "arrived" timestamp).
            val createdAtP = row.s("createdAt")
            if (createdAtP.isNotBlank() && (entry["arrivedAt"] as? String).isNullOrBlank()) {
                entry["arrivedAt"] = createdAtP
            }
            // TK APPROVED (2026-07-16): "Search & mark Arrived" writes a
            // zero-amount payments row with this payType -- it must NEVER
            // add to any Fees/Payment total or show as "Payment ₹0"; it only
            // flips arrived=true, same as any other real payment would.
            if (payType == "attendance_mark") {
                (entry["happened"] as? MutableList<String>)?.add("Marked Arrived")
                continue
            }
            val amount = row.optDouble("amount", 0.0)
            val split = PaymentModel.paymentSplit(row)
            val cash = split.second <= 0.0
            // 🔒🔒 খাতার সারি B110: এই মানুষটার রেকর্ড Reject/Delete/Registration
            // Cancel হয়ে গেছে — মানে টাকাটা তাঁকে ফেরত দেওয়া হয়েছে। তাই এই
            // সারির অঙ্ক **পর্দাতেও দেখাবে না, দিনের হিসাবেও যোগ হবে না**।
            // ⛔ সারিটা ডেটাবেসে ও Payment History-তে **অক্ষত থাকে** — শুধু এখানে
            //    গোনা হয় না (TK-এর সিদ্ধান্ত: "থাকবে, কিন্তু হিসাবে ধরবে না")।
            // ⛔ `arrived` ইতিমধ্যে উপরে বসানো হয়ে গেছে, তাই মানুষটা তালিকায়
            //    আগের মতোই থাকেন — শুধু টাকার ঘরগুলো `—` দেখায়।
            if (m in refundedMobiles) {
                (entry["happened"] as? MutableList<String>)?.add("Refunded (record cancelled)")
                continue
            }
            // TK-REQUESTED (2026-07-24): one line per real payment for the
            // live-screen "Payment" box -- "Fees-400/- Cash" for the
            // Registration/Visit fee, "1000/- Medicine" for a medicine
            // payment, plain "2000/- Cash" for everything else (Advance/
            // Treatment). Uses the SAME payType classification PaymentModel.
            // sourceLabel() already uses elsewhere (Payment Collection list),
            // so this always agrees with how the rest of the app labels a
            // payment. Purely a display string -- doesn't touch amount/
            // feesCash/paymentCash below, which stay exactly as before.
            val modeLabel = if (split.first > 0.0 && split.second > 0.0) "Cash + Online" else if (cash) "Cash" else "Online"
            val amtStr = "%,.0f".format(amount)
            val srcLabel = PaymentModel.sourceLabel(payType, row.s("remarks"))
            val line = when {
                isFeeRow(payType) -> "Fees-$amtStr/- $modeLabel"
                srcLabel == "Medicine Payment" -> "$amtStr/- Medicine"
                split.first > 0.0 && split.second > 0.0 -> PaymentModel.paymentBreakdown(split.first, split.second)
                else -> "$amtStr/- $modeLabel"
            }
            (entry["paymentLines"] as? MutableList<String>)?.add(line)
            if (isFeeRow(payType)) {
                // Visit/Registration fee is still a single-mode row.
                if (cash) entry["feesCash"] = (entry["feesCash"] as Double) + amount
                else entry["feesOnline"] = (entry["feesOnline"] as Double) + amount
            } else {
                entry["paymentCash"] = (entry["paymentCash"] as Double) + split.first
                entry["paymentOnline"] = (entry["paymentOnline"] as Double) + split.second
                // 🟢🔒 V612 (২৪.০৮.২০২৬, TK-নির্দেশ) — একই টাকা paymentCash/
                // paymentOnline-এও যোগ হলো (উপরের লাইন দুটো অক্ষত) — শুধু
                // ওষুধ হলে **বাড়তি** আলাদা করে ধরে রাখা হচ্ছে, প্রিন্টে
                // Medicine-এর নিজস্ব Cash/Online লাইন দেখানোর জন্য।
                if (srcLabel == "Medicine Payment") {
                    entry["medicineCash"] = (entry["medicineCash"] as Double) + split.first
                    entry["medicineOnline"] = (entry["medicineOnline"] as Double) + split.second
                }
            }
            val label = row.s("payLabel").ifBlank { row.s("paymentLabel").ifBlank { "Payment" } }
            (entry["happened"] as? MutableList<String>)?.add("$label ₹${"%,.0f".format(amount)}")
        }

        // Last Remark for today, if this mobile has a Follow-up record --
        // read-only mirror of the SAME field Visit/Patient cards show.
        // TK-REPORTED BUG FIX (2026-07-22): this loop used to set ONLY
        // "remark", never "followUpId" -- so a patient whose remark WAS
        // showing on screen (e.g. "KTA করা হল") could still fail the
        // writeTreatment()/editRemarkInReview() guard with "No Follow-up
        // record yet", because followUpId depended entirely on a SEPARATE
        // fallback pass below finding the record again. Now the id is taken
        // straight from the very same record that supplied the remark, so
        // the two can never disagree.
        // TK-REPORTED BUG FIX (2026-07-25, from TK's live report -- "remark
        // written in one place doesn't auto-update everywhere"): a patient
        // with multiple followups rows (one per stage -- see the identical
        // root cause already fixed in PatientTimelineRepository.kt today)
        // used to lock the board's followUpId/remark onto whichever row was
        // found FIRST while looping, not necessarily the row for this
        // patient's actual CURRENT stage. Writing a remark from THIS board
        // could then land on a different row than Report Card/Full Journey
        // read from -- looking like the update "didn't sync" even though
        // it saved correctly, just to the wrong row. Same stage-priority
        // fix as PatientTimelineRepository.kt, applied here too so every
        // screen converges on the SAME row for the same patient.
        fun stagePriority(s: String): Int = when {
            s.equals("Treatment", true) || s.equals("Treatment Running", true) -> 3
            s.equals("Patient", true) -> 2
            s.equals("Inquiry", true) -> 1
            else -> 0
        }
        val bestStageByMobile = HashMap<String, Int>()
        // 🔴🔒 V473 (20.08.2026, TK-রিপোর্ট — "Treatment Progress লিখলে
        // পেমেন্ট নেওয়ার পরে অটোমেটিক সরে যায়/ফাঁকা হয়ে যায়"): **আসল কারণ
        // (কোড ধরে যাচাই করা, আন্দাজ নয়)** — নিচের নিয়মে remark ও followUpId
        // দুটোই **একই** `bestStageByMobile` গেট দিয়ে আটকানো ছিল। পেমেন্ট
        // নিলে (PaymentRepository.kt) কখনো কখনো একটা **নতুন, higher-stage
        // কিন্তু ফাঁকা/auto-remark** followup সারি তৈরি হয় ("Advance Payment
        // received")। এই নতুন সারিটা যদি (Supabase-এর নিজস্ব ক্রম অনুযায়ী)
        // **আগে** processed হয়, তাহলে `bestStageByMobile[m]` তখনই সর্বোচ্চ
        // হয়ে যায় — তারপর আসল, স্টাফের হাতে-লেখা remark-সহ (কিন্তু কম
        // stage-এর) পুরনো সারিটা processed হলেও `pr >= bestStageByMobile[m]`
        // শর্তই মেলে না, তাই সেই আসল remark **কখনো পড়াই হয় না**। এটা একটা
        // ক্রম-নির্ভর (order-dependent) বাগ — তাই "মাঝে মাঝে" হতো, সবসময় না।
        // **সমাধান:** remark-এর জন্য আলাদা, নিজস্ব priority-ট্র্যাকার
        // (`bestRemarkPriority`) — stage/followUpId-এর গেট থেকে সম্পূর্ণ
        // স্বাধীন। এখন সর্বোচ্চ-stage-এর **আসল** (auto নয়) remark-ই সবসময়
        // দেখাবে, প্রসেস-করার ক্রম যাই হোক না কেন। ⛔ followUpId/bestStage-এর
        // পুরনো আচরণ (কোন সারিতে future writes যাবে) এক অক্ষরও বদলায়নি।
        val bestRemarkPriority = HashMap<String, Int>()
        /* 🟢🔒🔒 V638 (২৪.০৮.২০২৬, TK-রিপোর্ট — "KAPIL DAS ৩য় ভিজিট, তাও
           Treatment Progress আবার ফাঁকা দেখাচ্ছে") — একাধিকবার আসা রোগীর
           নামে একই stage-এর একাধিক `followups` সারি থাকতে পারে (প্রতি
           ভিজিটে একটা করে)। আগে সমান stage-এ **যেটা পরে processed হতো
           সেটাই** (`>=`) জিততো — ক্রম-নির্ভর, তাই কখনো আজকের ভিজিটের
           সারিটা জিততো, কখনো পুরনো ভিজিটেরটা। এখন সমান stage-এ
           **সবচেয়ে সাম্প্রতিক** (updatedAt/createdAt) সারিটাই জেতে —
           ঠিক ChamberAttendanceActivity ফাইলের resolveBestFollowUpId
           ফাংশনের একই তারিখ-ভিত্তিক টাই-ব্রেকার, দুই জায়গাতেই এখন এক
           নিয়ম, তাই save আর board — দুটোই সবসময় একই সারি বেছে নেয়। */
        val bestStageUpdatedAt = HashMap<String, String>()
        val bestRemarkUpdatedAt = HashMap<String, String>()
        for (fu in todaysFollowUps) {
            /* 🔵🔒 V526: Follow-up সারিতে `refId` = রোগীর সারির আইডি
               (V518-এ কোডে প্রমাণিত)। তাই রিমার্ক ঠিক রোগীর সারিতেই বসে। */
            val m = keyFor(fu.s("mobile"), fu.s("refId"))
            val pr = stagePriority(fu.s("stage"))
            val ua = fu.s("updatedAt").ifBlank { fu.s("createdAt") }
            /* 🔴🔒 V814 (২৮.০৮.২০২৬, TK-রিপোর্ট "ASBEN এখনো কেন?") — রিমার্কের
               লেখাটা **কবে লেখা হলো** সেটাই এখন রিমার্কের তারিখ। আগে সারির
               `updatedAt` ধরা হত, কিন্তু ওটা `updateNextFollow()`-এর মতো
               অন্য কাজেও আজকের হয়ে যায় — ফলে পুরনো লেখা আজকের সেজে
               চেম্বার-বন্ধের পাহারা পার হয়ে যেত।
               ⛔ পুরনো সারিতে ঘরটা ফাঁকা ⇒ আগের নিয়মেই (`ua`) চলে। */
            val ra = fu.s("lastRemarkAt").ifBlank { ua }
            if (pr > (bestStageByMobile[m] ?: -1) || (pr == bestStageByMobile[m] && ua > (bestStageUpdatedAt[m] ?: ""))) {
                bestStageByMobile[m] = pr
                bestStageUpdatedAt[m] = ua
                val fid = fu.s("id")
                if (fid.isNotBlank()) byMobile[m]?.set("followUpId", fid)
            }
            val remark = fu.s("lastRemark")
            // 🔒 V236 (TK — সমস্যা-৩): অ্যাপের নিজের auto-label progress সেজে দেখাবে না।
            // 🔴🔒 V687 — `m !in hasProgressToday`: আজকের আসল চেম্বার-নোট
            // (payments.progress) ইতিমধ্যে বসে থাকলে ফোন-কল/পুরনো
            // followups.lastRemark সেটা ওভাররাইট করবে না।
            if (m !in hasProgressToday && remark.isNotBlank() && !isAppAutoRemark(remark) &&
                (pr > (bestRemarkPriority[m] ?: -1) || (pr == bestRemarkPriority[m] && ua > (bestRemarkUpdatedAt[m] ?: "")))) {
                bestRemarkPriority[m] = pr
                bestRemarkUpdatedAt[m] = ua
                byMobile[m]?.set("remark", remark)
                byMobile[m]?.set("remarkUpdatedAt", ra)   // 🟢🔒 V654 · 🔴🔒 V814
            }
        }
        // Also pick up remarks/followUpId for mobiles that arrived/expected
        // but weren't in todaysFollowUps -- look them up directly, from the
        // SAME "followups" list already fetched above (no extra Supabase
        // query).
        val stillMissing = byMobile.filterValues {
            (it["remark"] as String).isBlank() || (it["followUpId"] as String).isBlank()
        }.keys
        if (stillMissing.isNotEmpty()) {
            for (fu in allFollowUpsList) {
                val m = keyFor(fu.s("mobile"), fu.s("refId"))   // 🔵 V526: একই চাবি
                if (m !in stillMissing) continue
                val pr = stagePriority(fu.s("stage"))
                val ua = fu.s("updatedAt").ifBlank { fu.s("createdAt") }   // 🟢🔒 V638
                val ra = fu.s("lastRemarkAt").ifBlank { ua }               // 🔴🔒 V814
                if (pr > (bestStageByMobile[m] ?: -1) || (pr == bestStageByMobile[m] && ua > (bestStageUpdatedAt[m] ?: ""))) {
                    bestStageByMobile[m] = pr
                    bestStageUpdatedAt[m] = ua
                    val fid = fu.s("id")
                    if (fid.isNotBlank()) byMobile[m]?.set("followUpId", fid)
                }
                val remark = fu.s("lastRemark")
                // 🔒 V236 (TK — সমস্যা-৩): অ্যাপের নিজের auto-label progress সেজে দেখাবে না।
                if (remark.isNotBlank() && !isAppAutoRemark(remark) &&
                    (pr > (bestRemarkPriority[m] ?: -1) || (pr == bestRemarkPriority[m] && ua > (bestRemarkUpdatedAt[m] ?: "")))) {
                    bestRemarkPriority[m] = pr
                    bestRemarkUpdatedAt[m] = ua
                    byMobile[m]?.set("remark", remark)
                    byMobile[m]?.set("remarkUpdatedAt", ra)   // 🟢🔒 V654 · 🔴🔒 V814
                }
            }
        }

        /**
         * 🔵🔒 V535 (২২.০৮.২০২৬, TK-রিপোর্ট: *"গতকাল সব কিছু ফিলাপ করেছিল
         * কিন্তু এখন অনেকগুলি ফাঁকা কেন?"*)
         *
         * **আসল কারণ (কোড ধরে প্রমাণিত):** উপরের `lastRemark` হলো ওই রোগীর
         * সারিতে **এই মুহূর্তে** যে লেখাটা আছে — সেদিন কী লেখা ছিল তা নয়।
         * (followups তারিখ ধরে আনাই হয় না, শুধু ব্রাঞ্চ ধরে।) রোগী আবার
         * এলে বা টাকা দিলে `lastRemark` বদলে যায়; নতুন লেখাটা অ্যাপের নিজের
         * অটো-লেখা হলে বোর্ড সেটা লুকায় (`isAppAutoRemark`) ⇒ **"—"**।
         *
         * **তথ্য হারায়নি:** `FollowUpRepository.updateRemark()` প্রতিবার
         * `history`-তে **তারিখ সমেত** লেখাটা জমা রাখে (ওই ফাইলের ২৬১০ নং লাইন)।
         *
         * **সমাধান:** পুরোনো দিনের বোর্ডে যে সারিগুলো ফাঁকা, **শুধু সেগুলোর**
         * `history` এনে ওই তারিখের নিজের লেখাটা বসানো হয়।
         *
         * ⛔ **আজকের বোর্ডে একটাও বাড়তি অনুরোধ নয়** — শর্তেই আটকে যায়।
         * ⛔ পুরোনো দিনেও ফাঁকা কিছু না থাকলে কোনো অনুরোধ হয় না।
         * ⛔ থাকলে **একটাই ছোট অনুরোধ**, শুধু ওই কয়েকটা সারির `id,history`
         *    (৫০০০ সারির বড় তালিকা নয়) — TK-এর Supabase কোটার কথা মাথায় রেখে।
         * ⛔ ভরা সারিতে হাত পড়ে না; যা আছে তাই থাকে।
         */
        try {
            if (date.isNotBlank() && date < FollowUpModel.today()) {
                /* 🕓🔒 V1157 (০৭.০৯.২০২৬, TK-রিপোর্ট) — আগে **শুধু ফাঁকা** সারির
                   জন্য খাতা দেখা হত। কিন্তু ঘরে অন্য দিনের একটা পুরনো লেখা বসে
                   থাকলে সারিটা "ফাঁকা নয়" ধরে বাদ পড়ত — অথচ ওই দিনের নিজের
                   লেখাটা খাতায় ছিল। ⇒ এখন **যার লেখা এই দিনের নয়**, তার জন্যও
                   খাতা দেখা হয়। ⛔ ওই দিনের লেখা ইতিমধ্যে থাকলে বাদ, তাই
                   বাড়তি অনুরোধ শুধু সত্যিই দরকার হলেই। */
                fun needsDayRemark(v: MutableMap<String, Any?>): Boolean {
                    if (((v["remark"] as? String).orEmpty()).isBlank()) return true
                    return ((v["remarkUpdatedAt"] as? String).orEmpty()).take(10) != date
                }
                val needIds = byMobile.values
                    .filter { needsDayRemark(it) }
                    .mapNotNull { (it["followUpId"] as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
                    .distinct()
                if (needIds.isNotEmpty()) {
                    val filter = "id=in.(" + needIds.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") } + ")"
                    val hist = SupabaseClient.fetchListSlimOrNull(
                        "followups", filter, needIds.size, "id,history"
                    )
                    if (hist != null) {
                        val dayRemarkById = HashMap<String, String>()
                        for (i in 0 until hist.length()) {
                            val r = hist.optJSONObject(i) ?: continue
                            val rid = r.s("id")
                            if (rid.isBlank()) continue
                            val h = r.optJSONArray("history") ?: continue
                            // ওই দিনের **শেষ** সত্যিকারের লেখাটাই নেওয়া হয়।
                            for (j in 0 until h.length()) {
                                val item = h.optJSONObject(j) ?: continue
                                if (item.s("date").take(10) != date) continue
                                val rem = item.s("remark").trim()
                                if (rem.isNotBlank() && !isAppAutoRemark(rem)) dayRemarkById[rid] = rem
                            }
                        }
                        if (dayRemarkById.isNotEmpty()) {
                            for (v in byMobile.values) {
                                if (!needsDayRemark(v)) continue
                                val fid = (v["followUpId"] as? String)?.trim().orEmpty()
                                dayRemarkById[fid]?.let {
                                    v["remark"] = it
                                    /* 🔴🔒 V1157 (০৭.০৯.২০২৬, TK-রিপোর্ট) — লেখাটা
                                       **ঠিক এই দিনেরই**, তাই তারিখটাও এখানে বসাতেই
                                       হয়। নইলে চেম্বার-বন্ধের পাহারা (এখন পুরনো
                                       দিনেও তারিখ মেলায়) সত্যিকারের লেখাকেই
                                       "নেই" ধরে নিত। */
                                    v["remarkUpdatedAt"] = date + "T00:00:00.000Z"
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) { /* বাড়তি সুবিধা — ব্যর্থ হলে বোর্ড আগের মতোই চলে */ }

        val rows = byMobile.entries.map { (_, v) ->
            ChamberAttendanceRow(
                /* 🔵🔒 V526: আগে **চাবিটাই** মোবাইল হিসেবে বসত। চাবি এখন
                   ঘোষিত আলাদা রোগীর ক্ষেত্রে তাঁর আইডি, তাই আসল নম্বরটা
                   আলাদা ঘর থেকে নেওয়া হয়। ⛔ সাধারণ রোগীর ক্ষেত্রে দুটো
                   হুবহু এক (দুটোই ১০ সংখ্যার নম্বর) — কিছুই বদলায় না। */
                mobile = v["mobile10"] as? String ?: "",
                name = v["name"] as String,
                branch = v["branch"] as String,
                disease = v["disease"] as String,
                expected = v["expected"] as Boolean,
                arrived = v["arrived"] as Boolean,
                feesCash = v["feesCash"] as Double,
                feesOnline = v["feesOnline"] as Double,
                paymentCash = v["paymentCash"] as Double,
                paymentOnline = v["paymentOnline"] as Double,
                medicineCash = v["medicineCash"] as? Double ?: 0.0,     // 🟢🔒 V612
                medicineOnline = v["medicineOnline"] as? Double ?: 0.0, // 🟢🔒 V612
                refundCash = v["refundCash"] as? Double ?: 0.0,         // 🔴🔒 V709
                refundOnline = v["refundOnline"] as? Double ?: 0.0,     // 🔴🔒 V709
                remark = v["remark"] as String,
                whatHappened = (v["happened"] as? MutableList<String>) ?: emptyList(),
                followUpId = v["followUpId"] as? String ?: "",
                patientId = v["patientId"] as? String ?: "",
                paymentLines = (v["paymentLines"] as? MutableList<String>) ?: emptyList(),
                arrivedAt = v["arrivedAt"] as? String ?: "",
                refDoctor = v["refDoctor"] as? String ?: "",
                refBy = v["refBy"] as? String ?: "",   // 🔴🔒 V933
                patientRowId = v["patientRowId"] as? String ?: "",   // 🔵 V526
                remarkUpdatedAt = v["remarkUpdatedAt"] as? String ?: ""   // 🟢🔒 V654
            )
        }.sortedWith(
            // Arrived-but-not-expected (walk-ins) and expected-and-arrived
            // first, then expected-but-not-arrived (no-shows) at the bottom
            // -- matches how TK's paper register reads top to bottom.
            /* 🟢🔒 V588 (23.08.2026, TK-নির্দেশ) — *"একই দিনে যখন প্রচুর পেশেন্ট
               হবে তখন সর্বপ্রথম কে এসেছিল টাইমিং অনুসারে সাজাবে"*।
               **সত্যিটা যাচাই করে:** ছাপা কাগজ (`finalizeAndShare`) আর Review
               পর্দা (`showCloseReview`) ১৯.০৭.২০২৬ থেকেই `arrivedAt` ধরে আসার
               ক্রমে সাজে — ওই দুটো ঠিকই ছিল। **কিন্তু পর্দার এই বোর্ডটা নামের
               অক্ষর-ক্রমে ছিল**, TK ঠিকই ধরেছেন। এখন তিনটে জায়গাতেই এক নিয়ম:
                 ১) আগে "এসেছেন", তারপর "আসার কথা" (আগের মতোই),
                 ২) তার মধ্যে **যিনি আগে এসেছেন তিনি আগে**,
                 ৩) সময় জানা না থাকলে (পুরনো সারি) সবার শেষে, তখন নাম ধরে —
                    ছাপা কাগজেও ঠিক এই "9999" নিয়মই চলে।
               ⛔ কোনো সারি বাদ যায় না · কোনো অঙ্ক বদলায় না — শুধু ক্রম। */
            compareByDescending<ChamberAttendanceRow> { it.arrived }
                .thenBy { it.arrivedAt.ifBlank { "9999" } }
                .thenBy { it.name }
        ).let { dropGhostRows(it) }.let { list ->
            // 🟢🔒 V621 (২৪.০৮.২০২৬, TK-নির্দেশ) — "Fees Return" করা রোগী
            // Chamber Date থেকে **সম্পূর্ণ বাদ** (সারিসহ, শুধু টাকা লুকানো
            // না — Cancelled-এর পুরনো নিয়ম থেকে ইচ্ছাকৃতভাবে আলাদা)।
            // ⛔ ব্যর্থ হলে (নেট/এরর) খালি সেট ফেরে — কারো সারি ভুলবশত বাদ যায় না।
            val returned = try { RefundedRecords.fetchReturnedVisits(branchFilter) } catch (_: Throwable) { HashSet() }
            if (returned.isEmpty()) list else list.filter { it.mobile.filter { c -> c.isDigit() }.takeLast(10) !in returned }
        }
            // TK-DECISION (2026-07-22, option "ক"): a pure Enquiry (only
            // enquired today, has NOT arrived and was NOT deliberately Marked
            // Expected) must NOT appear on the Chamber board at all -- so a
            // row that is neither arrived nor expected is dropped. This also
            // guarantees every row shown is counted in exactly one stat card
            // (Arrived, or Expected/No-show), so the counts can never read 0
            // while a row is visible.
            .filter { it.arrived || it.expected }
        // TK-REPORTED BUG FIX (2026-07-21): ghost (no name AND no mobile) rows
        // dropped above; totals computed from the SAME cleaned list so the
        // stat-card counts always match the rows actually shown.
        val totals = totalsOf(rows)

        val board = ChamberAttendanceBoard(rows, totals)
        saveCachedBoard(context, date, branchFilter, board)
        return board
    }

    /** 🩺 V763 (২৭.০৮.২০২৬, TK-নির্দেশ: *"পেশেন্টের নাম, রোগের নামও দেখাক"*) —
     *  `disease` ঘরটা যোগ করা হলো। ⛔ ডিফল্ট খালি, তাই পুরনো কোনো কল ভাঙে না। */
    data class PatientSearchResult(
        val name: String, val mobile: String, val patientId: String,
        val branch: String, val disease: String = ""
    )

    /** TK APPROVED (2026-07-16): "Search & Add existing patient" -- Name /
     *  Mobile / Patient ID. TK-REQUESTED OPTIMIZATION (2026-07-16, to keep
     *  Supabase free-quota usage low): this used to download the WHOLE
     *  "patients" table (up to 5000 rows) on every search and filter it on
     *  the phone. Now the filtering happens on the SERVER (a single
     *  ilike/or query, same "patients" table, same SupabaseClient.fetchList
     *  every other screen already uses) -- only the small number of
     *  matching rows are downloaded. A minimum 3-character query is
     *  required so an accidental single keystroke can't trigger a request. */
    fun searchPatients(query: String, branchFilter: String?): List<PatientSearchResult> {
        val q = query.trim()
        if (q.length < 3) return emptyList()
        val pattern = java.net.URLEncoder.encode("*$q*", "UTF-8")
        // TK-REQUESTED ADDITION (2026-07-24): search now also matches
        // Disease, Address, and Registration Date -- was Name/Mobile/
        // Patient ID only before. Same server-side "or=(...)" filter,
        // just more ilike conditions on fields already in "patients".
        val orFilter = "or=(name.ilike.$pattern,mobile.ilike.$pattern,patientId.ilike.$pattern,disease.ilike.$pattern,address.ilike.$pattern,registrationDate.ilike.$pattern)"
        val allBranch = branchFilter == null || branchFilter == "All"
        val filter = if (!allBranch) {
            "branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}&$orFilter"
        } else orFilter
        // 🔴 TK-ORDER (31.07.2026, Loading Speed Audit): এই ফাংশন শুধু
        // name/mobile/patientId/branch ব্যবহার করে, কিন্তু আগে পুরো row
        // (ছবিসহ) আনত — সর্বোচ্চ ২০ জনের ছবি অকারণে দুর্বল লাইনে নামত।
        // এখন শুধু ৪টা দরকারি কলামই আনা হয়। ⛔ ফলাফল/ফিল্টার/সংখ্যা কিছুই
        // বদলায়নি — শুধু ছবির বোঝাটা বাদ।
        // 🩺 V763 — `disease` ও `diagnosis` যোগ (TK: রোগের নামও দেখাতে হবে)।
        //    ⛔ মাত্র দুটো ছোট লেখার ঘর — Egress-এ প্রভাব নগণ্য, ছবি আগের মতোই বাদ।
        val patients = SupabaseClient.fetchListSlim(
            "patients", filter, 20, "name,mobile,patientId,branch,disease,diagnosis")
        val results = mutableListOf<PatientSearchResult>()
        for (i in 0 until patients.length()) {
            val p = patients.optJSONObject(i) ?: continue
            // ⛔ `disease` খালি থাকলে `diagnosis` — রেজিস্ট্রেশনে দুটোর যেকোনোটায় বসে।
            val dis = p.s("disease").trim().ifBlank { p.s("diagnosis").trim() }
            results.add(PatientSearchResult(
                p.s("name"), p.s("mobile"), p.s("patientId"), p.s("branch"), dis))
        }
        return results
    }

    /** TK APPROVED (2026-07-16): the actual write for "✅ Arrived" -- a
     *  single zero-amount row into the SAME "payments" table via the SAME
     *  SupabaseClient.upsert every other payment save already uses. See
     *  PaymentModel.buildAttendanceMarkRow for exactly what this writes.
     *  Returns the new row's id (needed for the 3-tap Undo below) or null
     *  on failure. */
    // TK-REQUESTED ADDITION (2026-07-18): "Mark Arrived" used to be a single
    // direct network call with NO offline safety net -- unlike every other
    // save in this app (Registration/Enquiry/Payment/Follow-up), a weak
    // connection meant the mark was just lost, no retry, and the staff's
    // OWN phone didn't even show it had happened. Now: save locally FIRST
    // (so it shows immediately on this device regardless of network),
    // then try the cloud; if that fails, queue it for retry (same
    // SharedPreferences-queue + BottomNav.wire() pattern already proven
    // for the other repositories) instead of silently dropping it.
    private fun markPendingPrefs(context: android.content.Context) =
        context.getSharedPreferences("piles_clinic_chamber_pending", android.content.Context.MODE_PRIVATE)

    /** TK-REPORTED BUG FIX (2026-07-22): the Chamber "Advance/Due Payment"
     *  dialog (ChamberAttendanceActivity.takePaymentPopup) saved locally
     *  first (safe) but, unlike Mark Arrived/Mark Expected right above, never
     *  queued the row for retry if the cloud push failed -- so a payment
     *  taken during a brief signal drop could stay stuck on that one device
     *  forever, invisible to Master/other branches, with nothing to retry
     *  it. This reuses the EXACT SAME queue + flushPending() below (already
     *  payType-agnostic) -- just exposes a way to add to it from outside
     *  this file. Nothing about markArrived/markExpected changes. */
    fun queuePendingPayment(context: android.content.Context, row: org.json.JSONObject) {
        synchronized(LOCK) {
            val prefs = markPendingPrefs(context)
            val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
            queue.put(row)
            prefs.edit().putString("queue", queue.toString()).commit()
        }
        // TK-REQUESTED (2026-07-25): sync immediately, even if the staff
        // closes the app right after saving . WorkManager does the upload
        // in the background; the same proven flushPending() work, sooner.
        try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(context) } catch (_: Throwable) { }
    }

    /**
     * 🔒🔒 খাতার সারি B196 (TK, 30.07.2026 রাত — "Chamber Attendance-এর লোডিং
     * ফিক্স, সাবধানে, ধাপে ধাপে" · ধাপ ১: Mark Arrived):
     *
     * **আসল কারণ (কোড ধরে):** এই ফাংশনটা **আগে থেকেই** অফলাইন-ফার্স্ট ছিল —
     * ফোনে আগে সেভ (`LocalWorkflowStore.upsertPayment`), তারপর ক্লাউডে
     * পাঠানোর চেষ্টা, ব্যর্থ হলে retry-queue। কিন্তু ক্লাউডে পাঠানোর অংশটা
     * **এই ফাংশনের ভিতরেই ব্লক করে বসেছিল** — তাই যে কেউ এটা ডাকতেন
     * (Chamber Attendance, Follow-up, Global Search, Patient Timeline —
     * পাঁচটা জায়গা) তাঁর "Marked Arrived ✅" বার্তা/পর্দা রিফ্রেশ **ফোনে সেভ
     * হয়ে যাওয়ার অনেক পরে**, ক্লাউড-কল শেষ হলে তবেই দেখাত — যদিও ডেটা ততক্ষণে
     * আসলে নিরাপদ। এটাই "ভিজিট নেওয়ার পর লোডিং"-এর মূল কারণ।
     *
     * **সমাধান:** ফোনে সেভ হওয়ার সঙ্গে সঙ্গেই আইডি ফেরত যায় (আগের মতোই) —
     * ক্লাউডে পাঠানো ও retry-queue-এ তোলা এখন **পিছনে** (`BackgroundWork.run{}`)
     * চলে। ⛔ **যা এক অক্ষরও বদলায়নি:** ফাংশনের প্যারামিটার/return type
     * (এখনও `String`, সেই একই আইডি, একই সময়ে গণনা করা) · লোকাল সেভের নিয়ম ·
     * ব্যর্থ হলে retry-queue-এ তোলার নিয়ম · **পাঁচটা কল-সাইটের একটাও ছোঁয়া
     * হয়নি** (কেউই ক্লাউড-কল শেষ হওয়ার অপেক্ষা করে এমন কিছুর উপর নির্ভর করে
     * না — সবাই শুধু ফেরত-আসা আইডি বা "ok" বুলিয়ান ব্যবহার করে, যেটা আগে
     * থেকেই সবসময় ফোনের সেভ সফল হলে true/আইডি থাকত)।
     * ⛔ **কনটেক্সট-লিক এড়াতে** পিছনের কাজে `context.applicationContext`
     * ব্যবহার হয়েছে, Activity-র রেফারেন্স ধরে রাখা হয়নি।
     */
    fun markArrived(context: android.content.Context, mobile: String, name: String, branch: String, staffMobile: String): String {
        val row = PaymentModel.buildAttendanceMarkRow(mobile, name, branch, staffMobile)
        LocalWorkflowStore(context).upsertPayment(row) // PENDING by default -- visible on this device now
        /* 🔁🔒 V839 (TK-নির্দেশ) — চেম্বারের আজকের তালিকায় নাম উঠলে রোগী
           আবার CHECK-UP তালিকায় ফিরবেন। ⛔ নিজের ব্যাকগ্রাউন্ড থ্রেডে চলে,
           দিনে একবারের পাহারা ভিতরে; এখানকার কাজ এক মুহূর্তও আটকায় না। */
        NextVisitQueue.reopenForToday(context, mobile)
        val appCtx = context.applicationContext
        BackgroundWork.run {
            val ok = try { SupabaseClient.upsert("payments", row) } catch (_: Throwable) { false }
            if (ok) {
                LocalWorkflowStore(appCtx).upsertPayment(row, "SYNCED")
            } else {
                synchronized(LOCK) {
                val prefs = markPendingPrefs(appCtx)
                val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
                queue.put(row)
                prefs.edit().putString("queue", queue.toString()).commit()
                }
            }
        }
        return row.s("id")
    }

    /** TK-REQUESTED ADDITION (2026-07-19): "Mark Expected (Future Date)" --
     *  identical save pattern to markArrived above (local-first, then cloud,
     *  queued on failure, retried by the SAME flushPending() below since it
     *  just replays whatever is in this queue regardless of payType) --
     *  only difference is the row itself is dated to the FUTURE date the
     *  staff picked, and carries payType="chamber_expected" instead of
     *  "attendance_mark", so it flips Expected (not Arrived) when that date
     *  is viewed. Works whether the mobile is a known Enquiry, a known
     *  Patient, or has no record at all yet (pure walk-in name/mobile). */
    /**
     * 🔒🔒 খাতার সারি B201 (TK, 30.07.2026 রাত — "ঝুঁকি না থাকলে করুন"):
     * markArrived()-এর (খাতার সারি B196) হুবহু একই সমস্যা ও একই সমাধান —
     * এখানে কোনো রোগী-খোঁজা/day-guard-এর মতো জটিলতা নেই (Payment-এর মতো
     * নয়), শুধু একটা রেকর্ড বসানো, তাই ঝুঁকি markArrived()-এর সমান — কম।
     * ⛔ **যা এক অক্ষরও বদলায়নি:** ফাংশনের প্যারামিটার/return type (এখনও
     * `String`, একই আইডি, একই সময়ে গণনা) · লোকাল সেভ ও DeletedGuard.unmark
     * (দুটোই লোকাল-শুধু, নেটওয়ার্ক নেই — যাচাই করা হয়েছে) · ব্যর্থ হলে
     * retry-queue-এ তোলার নিয়ম। **৭টা কল-সাইটের একটাও ছোঁয়া হয়নি** — কেউই
     * ফেরত-আসা আইডির উপর নির্ভর করে না (markArrived()-এর Undo-ফিচারের মতো
     * কিছু এখানে নেই), সবাই শুধু Toast দেখায়/board রিফ্রেশ করে।
     */
    fun markExpected(context: android.content.Context, mobile: String, name: String, branch: String, expectedDate: String, staffMobile: String): String {
        val row = PaymentModel.buildExpectedMarkRow(mobile, name, branch, expectedDate, staffMobile)
        // TK-REQUESTED SAFETY (2026-07-26): the "আসার কথা" row always uses the
        // SAME id for a person ("exp_<last 10 digits>"). If this person's
        // Expected was cancelled earlier, that id is in the deleted list . this
        // is a brand new mark, so clear it FIRST. Without this, a mark made
        // while offline would sit in the retry queue and be dropped forever.
        try { DeletedGuard.unmark("payments", row.optString("id", ""), context) } catch (_: Throwable) { }
        LocalWorkflowStore(context).upsertPayment(row) // PENDING by default -- visible on this device now
        val appCtx = context.applicationContext
        BackgroundWork.run {
            val ok = try { SupabaseClient.upsert("payments", row) } catch (_: Throwable) { false }
            if (ok) {
                LocalWorkflowStore(appCtx).upsertPayment(row, "SYNCED")
            } else {
                synchronized(LOCK) {
                val prefs = markPendingPrefs(appCtx)
                val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
                queue.put(row)
                prefs.edit().putString("queue", queue.toString()).commit()
                }
            }
        }
        return row.s("id")
    }

    /** TK-DECISION (2026-07-22): cancel a person's "আসার কথা" (Expected).
     *  Because every Expected entry uses the deterministic id "exp_<last10>"
     *  (buildExpectedMarkRow), one person has at most one such row, so this
     *  removes it everywhere: local cache first (instant on this device),
     *  the retry queue (so it isn't re-uploaded), and a best-effort cloud
     *  delete guarded to payType=="chamber_expected" (can never delete a
     *  real payment/fee/attendance row even if a wrong id were passed). The
     *  optional reason is recorded on the person's Follow-up remark by the
     *  caller (updateRemark) -- kept out of here so this stays a pure delete. */
    fun cancelExpected(context: android.content.Context, mobile: String): Boolean {
        val id = "exp_" + mobile.filter { it.isDigit() }.takeLast(10)
        LocalWorkflowStore(context).removePayment(id)
        synchronized(LOCK) {
            val prefs = markPendingPrefs(context)
            val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
            val kept = org.json.JSONArray()
            for (i in 0 until queue.length()) {
                val row = queue.optJSONObject(i) ?: continue
                if (row.optString("id") != id) kept.put(row)
            }
            prefs.edit().putString("queue", kept.toString()).commit()
        }
        val rows = try { SupabaseClient.fetchList("payments", "id=eq.$id", 1) } catch (_: Throwable) { org.json.JSONArray() }
        if (rows.length() > 0) {
            val row = rows.optJSONObject(0)
            if (row != null && row.optString("payType", "") == "chamber_expected") {
                SupabaseClient.deleteById("payments", id)
            }
        }
        return true
    }

    /** TK-REQUESTED (2026-07-27): reads back a person's existing "আসার কথা"
     *  date, so the app can WARN before overwriting it instead of silently
     *  replacing it. Returns the stored yyyy-MM-dd date, or null when this
     *  person has none.
     *
     *  Looks at this device's own copy FIRST (so a date just set offline is
     *  found instantly and with no network at all), then the cloud. On any
     *  failure it returns null, which the callers treat as "not known" and
     *  simply open the date picker as before -- a bad line can therefore
     *  never block the staff from setting a date. */
    fun findExpectedDate(context: android.content.Context, mobile: String): String? {
        val id = "exp_" + mobile.filter { it.isDigit() }.takeLast(10)
        try {
            val local = LocalWorkflowStore(context).pendingPayments()
            for (i in 0 until local.length()) {
                val row = local.optJSONObject(i) ?: continue
                if (row.optString("id") == id && row.optString("payType", "") == "chamber_expected") {
                    val d = row.optString("date", "")
                    if (d.isNotBlank()) return d
                }
            }
        } catch (_: Throwable) { }
        return try {
            val rows = SupabaseClient.fetchList("payments", "id=eq.$id", 1)
            val row = if (rows.length() > 0) rows.optJSONObject(0) else null
            if (row != null && row.optString("payType", "") == "chamber_expected") {
                row.optString("date", "").ifBlank { null }
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    /** Retries every "Mark Arrived" still stuck on this device. Called from
     *  BottomNav.wire() on every screen open, same as the other repositories.
     *  Upsert-by-id is idempotent, so re-sending an already-synced mark is
     *  harmless (Supabase free-plan-friendly: this only runs when the local
     *  queue is non-empty, i.e. does nothing -- no network call at all --
     *  once everything is caught up). */
    fun flushPending(context: android.content.Context) {
        synchronized(LOCK) {
        val prefs = markPendingPrefs(context)
        val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        if (queue.length() == 0) return
        val stillPending = org.json.JSONArray()
        for (i in 0 until queue.length()) {
            val row = queue.optJSONObject(i) ?: continue
            // TK-REQUESTED (2026-07-26): a mark that was deleted/undone in the
            // meantime must not be pushed back into the cloud by this retry.
            if (DeletedGuard.isDeleted("payments", row.optString("id", ""), context)) continue
            val ok = try { SupabaseClient.upsert("payments", row) } catch (_: Throwable) { false }
            if (ok) {
                LocalWorkflowStore(context).upsertPayment(row, "SYNCED")
            } else {
                stillPending.put(row)
            }
        }
        prefs.edit().putString("queue", stillPending.toString()).commit()
        }
    }

    fun markArrived(mobile: String, name: String, branch: String, staffMobile: String): String? {
        val row = PaymentModel.buildAttendanceMarkRow(mobile, name, branch, staffMobile)
        val ok = SupabaseClient.upsert("payments", row)
        return if (ok) row.s("id") else null
    }

    /** TK APPROVED (2026-07-16): "Undo" for an accidental "✅ Arrived" mark
     *  -- 3-tap protected in the UI (ChamberAttendanceActivity). SAFETY
     *  CHECK: only ever deletes a row that is STILL payType="attendance_mark"
     *  (re-checked here, not assumed) -- this can never delete a real
     *  Payment/Registration Fee row even if the wrong id were ever passed
     *  in. Uses the SAME SupabaseClient.deleteById the Trash Bin already
     *  uses elsewhere. */
    fun undoAttendanceMark(context: android.content.Context, paymentRowId: String): Boolean {
        if (paymentRowId.isBlank()) return false
        // Always clear the local cache first (this device's own view),
        // regardless of whether the mark ever reached the cloud.
        LocalWorkflowStore(context).removePayment(paymentRowId)
        // Also remove it from the retry queue if it hadn't synced yet --
        // otherwise BottomNav's next retry would re-upload a mark the
        // staff just undid.
        synchronized(LOCK) {
        val prefs = markPendingPrefs(context)
        val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        val kept = org.json.JSONArray()
        for (i in 0 until queue.length()) {
            val row = queue.optJSONObject(i) ?: continue
            if (row.optString("id") != paymentRowId) kept.put(row)
        }
        prefs.edit().putString("queue", kept.toString()).commit()
        }
        // Best-effort cloud delete too, in case it had already synced.
        // Not finding it on the cloud (e.g. it was only ever local) is not
        // a failure -- the local+queue cleanup above already did the job.
        val rows = try { SupabaseClient.fetchList("payments", "id=eq.$paymentRowId", 1) } catch (_: Throwable) { org.json.JSONArray() }
        if (rows.length() > 0) {
            val row = rows.optJSONObject(0)
            if (row != null && row.optString("payType", "") == "attendance_mark") {
                SupabaseClient.deleteById("payments", paymentRowId)
            }
        }
        return true
    }

    /* ↩️🔒 V1459 (১৪.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, তালিকা ৫৭৯) — বোর্ডেই
       সরাসরি "Undo Arrived"। আগে এই Undo শুধু "Patient Search" ডায়ালগে,
       ঠিক মার্ক-করার মুহূর্তেই থাকত (`markedRowId` ওই ডায়ালগেরই স্থানীয় চলক);
       বোর্ডের সারিতে (ChamberAttendanceRow) কোনো নির্দিষ্ট payments-সারির id
       ধরে রাখা হয় না (একই মোবাইলের একাধিক সারি যোগ হয়ে বসে), তাই এখানে
       mobile+branch+date+payType দিয়ে খুঁজে বার করে তারপর ওই একই প্রমাণিত
       `undoAttendanceMark(context, id)` ডাকা হয় — নতুন কোনো ডিলিট-নিয়ম নয়।
       ⛔ TK-এর স্পষ্ট শর্ত: "যে পেমেন্ট করবে তাকে আনডো করা যাবে না... পেমেন্টের
       ঘর জিরো হলে তবেই আনডু হবে" — এই যাচাইটা UI-তে (ChamberAttendanceActivity)
       আগেই হয়ে যায় (row-এর fees/payment/medicine সব ০ কিনা), এখানে আসারই কথা
       নয় যদি টাকা থাকে; তবু এখানেও কখনো কোনো টাকার সারি ছোঁয়া হয় না —
       `payType == "attendance_mark"` ছাড়া কিছুই ডিলিট হয় না (আগের নিয়মই)। */
    fun undoArrivedFromBoard(context: android.content.Context, mobile: String, branch: String, date: String): Boolean {
        return try {
            val filter = "mobile=eq.${java.net.URLEncoder.encode(mobile, "UTF-8")}" +
                "&branch=eq.${java.net.URLEncoder.encode(branch, "UTF-8")}" +
                "&date=eq.$date&payType=eq.attendance_mark"
            val rows = SupabaseClient.fetchList("payments", filter, 1)
            if (rows.length() == 0) return false
            val id = rows.optJSONObject(0)?.optString("id", "").orEmpty()
            if (id.isBlank()) return false
            undoAttendanceMark(context, id)
        } catch (_: Throwable) { false }
    }

    /** Old signature, kept in case anything else references it -- same
     *  cloud-only behavior as before, no local/queue cleanup. */
    fun undoAttendanceMark(paymentRowId: String): Boolean {
        if (paymentRowId.isBlank()) return false
        val rows = SupabaseClient.fetchList("payments", "id=eq.$paymentRowId", 1)
        if (rows.length() == 0) return false
        val row = rows.optJSONObject(0) ?: return false
        if (row.optString("payType", "") != "attendance_mark") return false
        return SupabaseClient.deleteById("payments", paymentRowId)
    }

    /* 🆕🔒 V805 (২৮.০৮.২০২৬, TK-অনুমোদিত) — চেম্বার-প্রিন্টে দেখানোর জন্য ওই
       দিনের **ওষুধ ও স্যালাইন বিক্রির মোট** টাকা।
       ─── কেন আলাদা করে পড়তে হচ্ছে ────────────────────────────────────────
       ওষুধ/স্যালাইন বিক্রি জমা হয় `products` টেবিলে, আর চেম্বার রেজিস্টার
       এতদিন পড়ত শুধু `payments` — দুটো আলাদা জায়গা। সেই কারণেই কাগজে
       "MEDICINE SALES" লাইনটা **কোনোদিন ছাপাই হয়নি** (সবসময় ₹0 হয়ে যেত)।
       ─── egress ───────────────────────────────────────────────────────────
       একটাই সরু পড়া: **শুধু ওই এক দিনের, ওই এক ব্রাঞ্চের** সারি, আর মাত্র
       ৫টা ঘর (`kind,mode,deposit,date,branch`) — কয়েক KB-র বেশি নয়।
       ─── সুরক্ষা ──────────────────────────────────────────────────────────
       ⛔ ব্যর্থ হলে সব শূন্য ফেরে ⇒ লাইনদুটো ছাপা হয় না, কাগজ হুবহু আগের মতোই।
       ⛔ FEES / TREATMENT / GRAND TOTAL-এর হিসাবে **একটুও হাত পড়েনি** —
          এই টাকা কোনো মোটে যোগ হয় না (TK-এর নিজের সিদ্ধান্ত)।
       ⛔ যে টাকা **সত্যিই জমা পড়েছে** সেটাই গোনা হয় (`deposit`), বিল নয় —
          বাকি থাকলে সেটা ড্রয়ারে আসেনি। */
    fun saleTotals(date: String, branch: String?): com.tkbiswas.pilesclinic.print.ChamberRegisterPdfBuilder.SaleTotals {
        val Z = com.tkbiswas.pilesclinic.print.ChamberRegisterPdfBuilder.SaleTotals()
        return try {
            val filter = StringBuilder("date=eq.").append(date)
                .append("&kind=in.(medicinePayment,salinePayment)")
            if (!branch.isNullOrBlank() && !branch.equals("All", ignoreCase = true)) {
                filter.append("&branch=eq.").append(java.net.URLEncoder.encode(branch, "UTF-8"))
            }
            val rows = SupabaseClient.fetchListSlimOrNull(
                "products", filter.toString(), 2000, "kind,mode,deposit,date,branch"
            ) ?: return Z
            var mc = 0.0; var mo = 0.0; var sc = 0.0; var so = 0.0
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val amt = r.s("deposit").replace(",", "").trim().toDoubleOrNull() ?: 0.0
                if (amt <= 0.0) continue
                val online = r.s("mode").equals("ONLINE", ignoreCase = true) ||
                    r.s("mode").equals("UPI", ignoreCase = true)
                if (r.s("kind") == "salinePayment") { if (online) so += amt else sc += amt }
                else { if (online) mo += amt else mc += amt }
            }
            com.tkbiswas.pilesclinic.print.ChamberRegisterPdfBuilder.SaleTotals(mc, mo, sc, so)
        } catch (_: Throwable) { Z }
    }

}
