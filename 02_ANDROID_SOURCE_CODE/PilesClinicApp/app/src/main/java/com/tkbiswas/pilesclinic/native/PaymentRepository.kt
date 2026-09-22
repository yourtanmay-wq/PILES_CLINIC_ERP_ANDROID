package com.tkbiswas.pilesclinic.native

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

import org.json.JSONObject

class PaymentRepository(private val context: Context? = null) {

    companion object {
        // TK-REQUESTED FIX (2026-07-19): same shared-lock fix as
        // LocalWorkflowStore/RegistrationRepository/EnquiryRepository.
        private val LOCK = Any()
        private const val CACHE_PREFS = "todays_collection_cache"
        // V1528: verified live data tops out at ₹3.5 lakh; allow a full 10x
        // headroom, but reject obvious extra-zero typo values before any local/cloud write.
        private const val BILL_TYPO_GUARD_MAX = 3_500_000.0

        /* 🔴🔒 V715 (২৬.০৮.২০২৬, TK-নির্দেশ — Supabase Egress-এর আসল কারণ,
           সার্ভারের লগ থেকে **মেপে** পাওয়া):

           গত ২৪ ঘণ্টায় `GET /rest/v1/followups` চলেছে **২০,৩১২ বার** — তার
           ~১১,৭০০ বার এই একটাই ফাংশন (`promoteFollowUpToTreatment`) থেকে,
           কারণ একটা পেমেন্ট ফোনের পেন্ডিং-তালিকায় আটকে আছে আর
           `flushPending()` **প্রতিবার যেকোনো পর্দা খোলার সময়** চলে।

           সবচেয়ে খারাপ দিকটা ছিল `select=*` — অর্থাৎ প্রতিবার রোগীর
           **base64 ছবিসহ** ১০০টা পর্যন্ত সারি নামত (একটা ছবি ~৫৫–১২০ KB)।
           এই একটা লাইনই দিনের egress-এর সিংহভাগ খেত।

           এই তিনটে তালিকা আসলে **মাত্র কয়েকটা ঘর** পড়ে (কোডে মিলিয়ে দেখা):
             · scan   → id · stage · status · lastRemark
             · inquiry→ id · name · patientId · refId  (PatientIdentity.provablyOtherPatient)
             · verify → শুধু গোনা হয় (`.length()`), একটাও ঘর পড়া হয় না

           ⛔ `fetchListSlim` ব্যবহার করা হয়েছে — সরু পড়া ব্যর্থ হলে সে
              **আগের মতোই পুরো সারি** নামায় (V405-এর প্রমাণিত fallback), তাই
              কোনো অবস্থাতেই কম তথ্য নিয়ে ভুল সিদ্ধান্ত হতে পারে না।
           ⛔ ছাঁকনি · limit · সাজানো · ফেরত আসা সারির সংখ্যা — কিছুই বদলায়নি। */
        private const val PROMOTE_COLS_SCAN    = "id,stage,status,lastRemark,mobile,updatedAt"
        private const val PROMOTE_COLS_INQUIRY = "id,name,patientId,refId,mobile,stage,status,updatedAt"
        private const val PROMOTE_COLS_VERIFY  = "id"

        // 🟢🔒 B662 (15.08.2026, TK-অনুমোদিত · Egress-৪): "Visit Fee Missing" গোনার জন্য
        //   `fetchMissingVisitFeePatients()` **পাঁচ ব্রাঞ্চের সব রোগী** (ছাঁকনি নেই, limit
        //   5000) + সব visit_fee টাকার সারি নামায়। মাস্টারের ঘণ্টা (BellCounter) এটা
        //   **ড্যাশবোর্ডে ফিরলেই প্রতিবার** ডাকত — দিনে বহুবার একই তথ্য বারবার।
        //   এখন উত্তরটা মনে রাখা হয়, আর নতুন করে গোনা হয় **শুধু তখনই** যখন
        //   patients/payments টেবিলে সত্যিই কিছু বদলেছে (LiveRefresh-এর প্রমাণিত
        //   HEAD count-only প্রশ্ন — একটাও সারি নামে না)।
        //   ⛔ হিসাবের নিয়ম এক অক্ষরও বদলায়নি — শুধু বারবার না গুনে মনে রাখা হয়।
        /* ═══════════════════════════════════════════════════════════════
           🟢🔒 V1121 (০৫.০৯.২০২৬, TK-অনুমোদিত — "হ্যাঁ করুন, সাবধানে")
           TK: *"Visit fee missing এখনো কেন এরকম?"* (৮ম বার)

           🔴 **কোডে মেপে পাওয়া কারণ:** V1101-এ যে ব্যবস্থা বসেছে সেটা
           **সেভের সময়ে** কাজ করে (`PendingVisitFeeStore`) — ওই দিনের পর
           কোনো ফি-র সারি আর হারাতে পারে না। কিন্তু **তার আগে** যাঁদের সারি
           হারিয়ে গেছে তাঁদের জন্য অ্যাপের কিছুই করার নেই, কারণ ফি-র
           **টাকার অঙ্কটা কেবল ওই টাকার সারিতেই** জমা থাকত — রোগীর সারিতে
           ফি-র কোনো ঘর নেই। ⇒ অঙ্কটা অ্যাপ জানেই না, আন্দাজে বসালে **ভুল
           টাকা খাতায় ঢুকত**। তাই ওই পুরনো নামগুলো তালিকায় রেখে মালিককে
           রোজ মনে করানোর কোনো মানে হয় না।

           ⇒ এই তারিখের **আগে** রেজিস্টার হওয়া রোগীর নাম আর তালিকায় ওঠে না।
           ⛔ এই তারিখ থেকে রেজিস্টার হওয়া কারো ফি-র সারি হারালে **আগের
              মতোই** তালিকায় উঠবে — পাহারাটা এক অক্ষরও দুর্বল হয়নি।
           ⛔ **কোনো টাকার হিসাব বদলায় না** · ডেটাবেসে কিছু লেখা/মোছা হয় না —
              শুধু এই একটা তালিকায় দেখানো বন্ধ।
           ⛔ তারিখটা ঠিক `yyyy-MM-dd` ধাঁচে না হলে (বা ফাঁকা হলে) কিছুই বাদ
              যায় না — না জেনে কাউকে লুকিয়ে ফেলা হয় না।
           ⚠️ TK-কে জানানো ঝুঁকি: এই চারজনের কেউ সত্যিই ফি না দিয়ে থাকলে সেই
              মনে-করানোটাও চলে যাবে।
           ═══════════════════════════════════════════════════════════════ */
        private const val FEE_GUARD_FROM = "2026-09-05"
        private val ISO_DAY = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        private var missingFeeCache: List<MissingVisitFee>? = null
        private var missingFeeAt: Long = 0L
        private val missingFeeWatch = LiveRefresh.Watch("patients", "payments")
        /** দুটো গোনার মধ্যে অন্তত এতটা সময় — এর ভিতরে ফিরে এলে জমানো উত্তরই। */
        private const val MISSING_FEE_MIN_GAP_MS = 3 * 60 * 1000L
    }

    // TK-REQUESTED ADDITION (2026-07-20): same "show what was already on the
    // phone instantly" pattern already added to Doctor Queue/Follow-up/
    // Chamber Attendance. fetchTodayCollection() below is unchanged except
    // for saving into this cache right before it returns.
    fun loadCachedTodayCollection(branchFilter: String?): List<CollectionRow>? {
        val ctx = context ?: return null
        val key = "cache_" + (branchFilter ?: "All") + "_" + PaymentModel.today()
        val json = ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).getString(key, null)
            // ⚡ জমানো তালিকা না থাকলেও ফোনের নিজের নেওয়া টাকা সঙ্গে সঙ্গে দেখাতে হবে।
            ?: return PaymentModel.mergeDailyTreatmentCollections(mergeOwnPhonePayments(branchFilter, emptyList())).ifEmpty { null }
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<CollectionRow>()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                list.add(
                    CollectionRow(
                        source = r.optString("source", ""), date = r.optString("date", ""),
                        name = r.optString("name", ""), mobile = r.optString("mobile", ""),
                        branch = r.s("branch"), mode = r.s("mode"),
                        amount = r.optDouble("amount", 0.0),
                        patientId = r.optString("patientId", ""),
                        time = r.optString("time", ""),
                        paidAt = r.optString("createdAt", ""),          // 🔵 V565
                        cashAmount = r.optDouble("cashAmount", if (r.s("mode") == "CASH") r.optDouble("amount", 0.0) else 0.0),
                        onlineAmount = r.optDouble("onlineAmount", if (r.s("mode") != "CASH") r.optDouble("amount", 0.0) else 0.0),
                        paymentEventCount = r.optInt("paymentEventCount", 1).coerceAtLeast(1)
                    )
                )
            }
            // ⚡ TK (28.07.2026): নিজের ফোনে নেওয়া টাকা সঙ্গে সঙ্গে দেখাতে হবে।
            PaymentModel.mergeDailyTreatmentCollections(mergeOwnPhonePayments(branchFilter, list))
        } catch (t: Throwable) { null }
    }

    /**
     * ⚡ TK-এর নির্দেশ (28.07.2026 ২.৩০ pm): **"আমি আমার ফোনে যা যা কাজ করবো,
     * সেটা যেন সাথে সাথেই দেখায়"** — শুধু রেজিস্ট্রেশন নয়, অ্যাডভান্স ও যে
     * কোনো পেমেন্টও।
     *
     * ক্লাউড থেকে আনার পথে ফোনের নিজের সদ্য নেওয়া টাকা আগেই মেশানো হত, কিন্তু
     * **জমানো তালিকা** দেখানোর পথে হত না — তাই ধীর লাইনে নিজের নেওয়া টাকা
     * তালিকায় দেরিতে আসত। এখন দুই পথেই মেশে।
     *
     * ⛔ কোনো নতুন ক্লাউড-কল নেই · কোনো সারি বাদ যায় না · কোনো হিসাব বদলায় না —
     * শুধু ফোনে জমে থাকা টাকার সারি যোগ হয়।
     */
    private fun mergeOwnPhonePayments(branchFilter: String?, cached: List<CollectionRow>): List<CollectionRow> {
        val ctx = context ?: return cached
        return try {
            val today = PaymentModel.today()
            val pending = LocalWorkflowStore(ctx).pendingPayments()
            if (pending.length() == 0) return cached
            val seen = HashSet<String>()
            // 🔒 খাতার সারি B73 (29.07.2026): নম্বর মেলানো হবে **শেষ ১০ সংখ্যা**
            // ধরে — অ্যাপের বাকি সব জায়গার মতোই (`takeLast(10)`)। আগে পুরো সংখ্যা
            // ধরে মেলানো হত, তাই এক দিকে "+91XXXXXXXXXX" আর অন্য দিকে
            // "XXXXXXXXXX" থাকলে একই টাকা দু'বার দেখানোর ঝুঁকি ছিল।
            // ⛔ টাকার কোনো অঙ্ক বদলায়নি — শুধু মেলানোর নিয়ম।
            for (c in cached) seen.add(c.date + "|" + c.mobile.filter { it.isDigit() }.takeLast(10) + "|" + c.amount + "|" + c.source)
            val extra = mutableListOf<CollectionRow>()
            for (i in 0 until pending.length()) {
                val row = pending.optJSONObject(i) ?: continue
                if (row.optString("date") != today) continue
                if (branchFilter != null && branchFilter != "All" &&
                    !row.s("branch").equals(branchFilter, ignoreCase = true)) continue
                /* 🔴🔒 V1311 (তালিকা ৪২৩): পুরো-পড়ার পথের (fetchTodayCollection) হুবহু একই নিয়ম —
                   চিহ্ন-সারি নয়; refund শুধু approved হলে, আর তখন **বিয়োগ** (আগে এখানে
                   pending/approved refund দুটোই **যোগ** হয়ে যেত — জমানো তালিকার পথে)। */
                if (PaymentModel.isMarkerOnlyRow(row.s("payType"))) continue
                val r = if (PaymentModel.isRefundRow(row)) {
                    if (!PaymentModel.isApprovedRefund(row)) continue
                    PaymentModel.parseApprovedRefundRow(row)
                } else {
                    if (row.optDouble("amount", 0.0) <= 0) continue
                    PaymentModel.parsePaymentRow(row)
                }
                // ⚠️ উপরের `seen` তৈরির নিয়মের সঙ্গে এটা **হুবহু এক** থাকতে হবে,
                // নইলে মেলানো ভেঙে যাবে (খাতার সারি B73)।
                val k = r.date + "|" + r.mobile.filter { it.isDigit() }.takeLast(10) + "|" + r.amount + "|" + r.source
                if (!seen.add(k)) continue
                extra.add(r)
            }
            if (extra.isEmpty()) cached else fillPatientIds(extra) + cached
        } catch (_: Throwable) { cached }
    }

    private fun saveCachedTodayCollection(branchFilter: String?, rows: List<CollectionRow>) {
        val ctx = context ?: return
        try {
            val arr = org.json.JSONArray()
            for (r in rows) {
                arr.put(
                    org.json.JSONObject()
                        .put("source", r.source).put("date", r.date).put("name", r.name)
                        .put("mobile", r.mobile).put("branch", r.branch).put("mode", r.mode)
                        .put("amount", r.amount).put("patientId", r.patientId).put("time", r.time)
                        .put("cashAmount", r.cashAmount).put("onlineAmount", r.onlineAmount)
                        .put("paymentEventCount", r.paymentEventCount)
                )
            }
            val key = "cache_" + (branchFilter ?: "All") + "_" + PaymentModel.today()
            ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit().putString(key, arr.toString()).apply()
        } catch (_: Throwable) { }
    }

    // Tracks how many treatment payments a patient already has, filled in by
    // findPatientByMobile() and read by nextLabelFor() -- used to compute the
    // correct "Advance" / "2nd Payment" / "3rd Payment" label before saving.
    private val treatmentPaymentCounts = mutableMapOf<String, Int>()

    // 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত) — "একদিনে একটাই পেমেন্ট"
    //
    // TK-এর কথা: *"একদিনে একটা পেমেন্টই এলাউ হবে... একই দিনে সেই পেশেন্ট যদি
    // আরো দু-তিনবারও পেমেন্ট করে তাহলে সেটা প্রতিবার অ্যাডভান্স পেমেন্টের
    // মধ্যেই অ্যাড হবে।"*
    //
    // আগে নম্বরটা বসত **পেমেন্টের সারি গুনে**, তাই একদিনে ৪ বার সেভ হলে
    // Advance · 2nd · 3rd · 4th হয়ে যেত (ইন্টারনেট ধীর থাকলে স্টাফ বুঝতে না
    // পেরে বারবার সেভ করলে ঠিক এটাই হত)। এখন নম্বরটা বসে **যত আলাদা দিনে টাকা
    // নেওয়া হয়েছে তা গুনে** — তাই একই দিনের সব টাকা একটাই নম্বরে থাকে।
    //
    // ⛔ টাকার অঙ্কে কোনো হাত পড়ে না — শুধু নম্বর/নাম কোনটা বসবে সেটা ঠিক হয়।
    private val treatmentPaymentDates = mutableMapOf<String, List<String>>()

    // ওই রোগীর নামে **আজ** ইতিমধ্যে কত টাকা নেওয়া হয়ে গেছে — সেভ করার আগে
    // স্টাফকে দেখানো সতর্কবার্তার জন্য (TK: *"প্রয়োজনে স্টাফকে সেইভাবে
    // ওয়ার্নিং দেবে"*)। ⛔ এর জন্য ক্লাউডে বাড়তি কোনো অনুরোধ নেই — যে তালিকা
    // findPatientByMobile() আগে থেকেই আনে, সেটা থেকেই গোনা হয়।
    private val treatmentPaidOnDate = mutableMapOf<String, Double>()

    /** Fetches today's combined collection (payments + medicine/product
     * payments), matching collectionRows().filter(date===today()). When
     * branchFilter is not "All", only that branch's rows are included --
     * same rule Follow-up applies (staff see only their branch, Master
     * sees everything). */
    fun fetchTodayCollection(branchFilter: String?): List<CollectionRow> {
        val today = PaymentModel.today()
        val branchPart = if (branchFilter != null && branchFilter != "All")
            "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""
        val rows = mutableListOf<CollectionRow>()

        // TK-REQUESTED FIX (2026-07-23): same root cause/fix as Follow-up's
        // Bill/Due bug -- fetchList() swallows a failed request into a
        // silent empty array, which used to get baked straight into
        // Today's Collection (and then CACHED at the end of this
        // function), making it look wrongly empty on a bad connection,
        // possibly stuck that way. Now uses fetchListOrNull(): a genuine
        // failure (null) on either fetch falls back to the last cached
        // collection instead of computing/caching a wrong one; if there's
        // no cache yet, falls through to empty exactly like before (no
        // behavior change for a first-ever load). A real empty result
        // (successful fetch, genuinely nothing collected yet today) is NOT
        // affected -- only a true failure is.
        // ⚡ TK-REPORTED (29.07.2026 বিকেল ৬.১০ · খাতার সারি B113 → B130):
        // *"লোডিং হতে এত বেশি সময় কেন লাগছে?"* — TK তিনটে পর্দার কথা বলেছিলেন:
        // Report Card · Patient Timeline · **Payment Collection**। প্রথম দুটো
        // সারি B114-এ ঠিক হয়েছিল, **এই তিন নম্বরটা আমার বাদ পড়ে গিয়েছিল**।
        //
        // এই পড়াটা এতদিন `select=*` করত — আজকের প্রতিটা টাকার সারির **সব ঘর**
        // নামত, অথচ নিচের কোড মাত্র ন'টা ঘর পড়ে।
        //
        // ⛔ **এক পয়সাও হিসাব বদলায়নি:** `PaymentModel.parsePaymentRow` যে ঘরগুলো
        //    পড়ে (payType · remarks · date · name · mobile · branch · mode ·
        //    amount · patientCode) আর এই ফাংশন যেটা পড়ে (id) — **সবকটাই নিচে
        //    চাওয়া হয়েছে**। মাসের তালিকায় (নিচের `fetchCollectionRange`) ঠিক
        //    এই একই তালিকাই আগে থেকে ব্যবহার হচ্ছে, তাই দুই পর্দা কখনো দুই
        //    হিসাব দেখাবে না।
        // 🔒 **সুরক্ষা:** কম-ঘরের অনুরোধ ব্যর্থ হলে `fetchListSlimOrNull` নিজেই
        //    আগের মতো সব ঘর চেয়ে নেয়; সত্যিকারের ব্যর্থতায় `null` ফেরে, তাই
        //    জমানো তালিকায় ফিরে যাওয়ার নিয়মটা (নিচের লাইন) হুবহু আগের মতোই।
        val paymentsRaw = SupabaseClient.fetchListSlimOrNull(
            "payments", "date=eq.$today$branchPart", 500,
            // V216 (§13): refundApprovalStatus যোগ — refund row চিনতে ও শুধু
            // approved refund collection থেকে বিয়োগ করতে দরকার।
            // 🐞🔒 V1322 (TK-নির্দেশ) — receivedBy যোগ, যাতে কার্ডে কোন স্টাফ
            // পেমেন্ট নিয়েছেন সেটা দেখানো যায়।
            "id,payType,remarks,date,name,mobile,branch,mode,amount,cashAmount,onlineAmount,dailyEvents,patientCode,refundApprovalStatus,updatedAt,createdAt,receivedBy"
        )
        if (paymentsRaw == null) loadCachedTodayCollection(branchFilter)?.let { return it }
        val payments = paymentsRaw ?: org.json.JSONArray()
        // 🔒🔒 খাতার সারি B110 (TK, 29.07.2026 বিকেল ৪.৫০ — স্থায়ী নিয়ম):
        // Reject / Delete / Registration Cancel হয়ে যাওয়া রেকর্ডের টাকা রোগীকে
        // ফেরত দেওয়া হয়েছে, তাই দিনের হিসাবে সেটা আর ধরা যাবে না — নইলে
        // ড্রয়ারের ক্যাশের সঙ্গে কোনোদিন মিলবে না।
        // ⛔ নিয়মটা একটাই জায়গায় লেখা (`RefundedRecords`), চেম্বার বোর্ডও
        //    ঠিক সেটাই ব্যবহার করে — তাই দুই পর্দা কখনো দুই হিসাব দেখাবে না।
        // ⛔ টাকার সারি মোছা হয় না — Payment History-তে সব আগের মতোই থাকে।
        // ⛔ খোঁজা ব্যর্থ হলে তালিকা ফাঁকা ফেরে, তখন কারও টাকা বাদ যায় না।
        val refundedMobiles = RefundedRecords.fetch(branchFilter)
        fun isRefunded(mob: String): Boolean {
            if (refundedMobiles.isEmpty()) return false
            val d = mob.filter { it.isDigit() }.takeLast(10)
            return d.length == 10 && d in refundedMobiles
        }
        val seenPaymentIds = HashSet<String>()
        for (i in 0 until payments.length()) {
            val row = payments.getJSONObject(i)
            seenPaymentIds.add(row.optString("id"))
            val amount = row.optDouble("amount", 0.0)
            if (isRefunded(row.s("mobile"))) continue
            // 🔒 V216 (§13, 31.07.2026): Refund row আলাদা করে সামলানো হয়।
            // approved refund দিনের collection থেকে **বিয়োগ** হয় (negative row,
            // তাই মোট যোগফল ঠিক কমে) — History-তে "Refund / টাকা ফেরত" দেখা যায়।
            // pending/rejected refund collection-এ কোনো প্রভাব ফেলে না।
            // ⛔ এই বিয়োগ শুধু refund amount-এ — Visit Fee/অন্য টাকা অক্ষত।
            if (PaymentModel.isRefundRow(row)) {
                if (PaymentModel.isApprovedRefund(row)) {
                    rows.add(PaymentModel.parseApprovedRefundRow(row))
                }
                continue
            }
            if (amount > 0) rows.add(PaymentModel.parsePaymentRow(row))
        }
        // TK-REQUESTED BUG FIX (2026-07-16): same fix as Follow-up/Doctor
        // Queue -- a just-taken treatment payment could be briefly missing
        // from Today's Collection because this always read straight from the
        // cloud, with no awareness of a payment still syncing in the
        // background (see PaymentRepository.saveTreatmentPayment). Any
        // locally-pending payment for today (and this branch) not yet in the
        // cloud result is now merged in too.
        context?.let { ctx ->
            LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("payments", payments)   // 🔴 V1311 (তালিকা ৪২৩)
            val pending = LocalWorkflowStore(ctx).pendingPayments()
            for (i in 0 until pending.length()) {
                val row = pending.getJSONObject(i)
                val id = row.optString("id")
                if (id.isBlank() || !seenPaymentIds.add(id)) continue
                if (row.optString("date") != today) continue
                if (branchFilter != null && branchFilter != "All" && !row.s("branch").equals(branchFilter, ignoreCase = true)) continue
                // 🔒 খাতার সারি B110 — ফোনে জমা থাকা সারিতেও একই নিয়ম।
                if (isRefunded(row.s("mobile"))) continue
                val amount = row.optDouble("amount", 0.0)
                // 🔒 V216 (§13): ফোনে জমা থাকা refund-ও সঙ্গে সঙ্গে collection থেকে
                // বিয়োগ দেখায় (§13 same-phone immediate) — cloud sync-এর আগেও।
                if (PaymentModel.isRefundRow(row)) {
                    if (PaymentModel.isApprovedRefund(row)) {
                        rows.add(PaymentModel.parseApprovedRefundRow(row))
                    }
                    continue
                }
                if (amount > 0) rows.add(PaymentModel.parsePaymentRow(row))
            }
        }

        // TK-REQUESTED FIX (2026-07-23): same failure/empty distinction as
        // the payments fetch above.
        // ⚡ খাতার সারি B130 — উপরের একই কারণ। নিচের লুপ যে ঘরগুলো পড়ে
        // (date · customer · mobile · branch · mode · deposit · total) ঠিক
        // সেগুলোই চাওয়া হলো; মাসের তালিকাতেও হুবহু এই তালিকা।
        // ⛔ `products` টেবিলে `name` নামের কোনো ঘর নেই — নিচের `ifBlank`
        //    শুধু নিরাপত্তার জন্য, তাই কিছু বাদ পড়ে না।
        val productsRaw = SupabaseClient.fetchListSlimOrNull(
            "products", "date=eq.$today$branchPart", 500,
            "id,date,customer,mobile,branch,mode,deposit,total,updatedAt,createdAt"
        )
        if (productsRaw == null) loadCachedTodayCollection(branchFilter)?.let { return it }
        val products = productsRaw ?: org.json.JSONArray()
        for (i in 0 until products.length()) {
            val row = products.getJSONObject(i)
            val deposit = row.optDouble("deposit", row.optDouble("total", 0.0))
            if (deposit > 0) {
                rows.add(
                    CollectionRow(
                        source = "Medicine Payment",
                        date = row.s("date"),
                        name = row.s("customer").ifBlank { row.s("name").ifBlank { "Walk-in" } },
                        mobile = row.s("mobile"),
                        branch = row.s("branch"),
                        mode = PaymentModel.normalizeMode(row.s("mode").ifBlank { "CASH" }),
                        amount = deposit,
                        time = PaymentModel.displayTime12(row.s("createdAt")),
                        paidAt = row.s("createdAt"),                    // 🔵 V565
                        cashAmount = if (PaymentModel.normalizeMode(row.s("mode").ifBlank { "CASH" }) == "CASH") deposit else 0.0,
                        onlineAmount = if (PaymentModel.normalizeMode(row.s("mode").ifBlank { "CASH" }) == "ONLINE") deposit else 0.0
                    )
                )
            }
        }
        // 🔒 V452: legacy/future Treatment rows from the same patient+day are
        // one collection item. Historical DB rows are not deleted; Visit Fee,
        // Medicine and Refund remain separate.
        val filled = fillPatientIds(PaymentModel.mergeDailyTreatmentCollections(rows))
        saveCachedTodayCollection(branchFilter, filled)
        return filled
    }

    /** TK-REQUESTED ADDITION (2026-07-25, Master only): the same collection
     *  rows as fetchTodayCollection(), but for a DATE RANGE instead of just
     *  today, so Master Admin can open "Monthly Collection" (one month) and
     *  "Collection History" (everything). Nothing about today's screen
     *  changes -- this is a separate read used only by CollectionListActivity.
     *
     *  Reads the SAME two tables the daily screen reads (payments +
     *  products), with the SAME row parsing, so an amount can never be
     *  counted differently here than it is on the daily screen.
     *
     *  Quota note: one tap = one payments read + one products read, capped at
     *  5000 rows each (same cap used elsewhere in the app), newest first.
     */
    /**
     * 🆔 TK-এর নিয়ম (28.07.2026): টাকার তালিকাতেও নাম ও মোবাইলের সঙ্গে
     * Patient ID দেখাতে হবে।
     *
     * নতুন পেমেন্টে ID সারির ভিতরেই থাকে (`patientCode`), তাই কিছু করতে হয় না।
     * পুরনো পেমেন্ট ও মেডিসিনের সারিতে ওটা নেই — সেগুলোর জন্য **ফোনের নিজের
     * জমা তালিকা** দেখা হয়।
     *
     * ⛔ **এখানে একটিও নতুন ক্লাউড-কল নেই** — TK-এর Supabase কোটা ও ধীর
     * লাইনের কথা ভেবে ইচ্ছাকৃতভাবে সব কাজ ফোনের ভিতরেই করা হয়েছে।
     * তাই তালিকা এক মুহূর্তও ধীর হয় না।
     *
     * ⛔ কোনো সারি বাদ যায় না, কোনো টাকার হিসাব বদলায় না — শুধু ID বসে।
     * ID না পাওয়া গেলে সারিটা আগের মতোই থাকে (খালি লেবেল কখনো নয়)।
     */
    // 🔒 B575 (08.08.2026): এখন শুধু patientId নয় — রোগীর **রোগ ও ঠিকানা**-ও ভরে
    // দেওয়া হয় (Payment Collection সারিতে নামের পাশে রোগ + নিচে ঠিকানা দেখাতে)।
    // ⛔ সবই ফোনের নিজের জমা তালিকা থেকে read-only lookup — নেট লাগে না, টাকার
    // কোনো হিসাব বদলায় না, ব্যর্থ হলে তালিকা আগের মতোই ফেরে (কখনো নষ্ট হয় না)।
    private fun fillPatientIds(rows: List<CollectionRow>): List<CollectionRow> {
        try {
            if (rows.isEmpty()) return rows
            val ctx = context ?: return rows
            fun key(m: String) = m.filter { it.isDigit() }.takeLast(10)
            val store = LocalWorkflowStore(ctx)
            val cache = HashMap<String, org.json.JSONObject?>()
            fun patientFor(m: String): org.json.JSONObject? {
                val k = key(m); if (k.length != 10) return null
                if (!cache.containsKey(k)) cache[k] = store.findPatientByMobile(k)
                return cache[k]
            }
            fun buildAddr(p: org.json.JSONObject): String {
                val single = p.s("address")
                if (single.isNotBlank()) return single
                return listOf(
                    "Vill" to p.s("village"), "PO" to p.s("po"), "PS" to p.s("ps"),
                    "Dist" to p.s("district"), "PIN" to p.s("pin")
                ).filter { it.second.isNotBlank() }.joinToString(", ") { "${it.first}: ${it.second}" }
            }
            return rows.map { r ->
                val p = patientFor(r.mobile) ?: return@map r
                r.copy(
                    patientId = if (r.patientId.isNotBlank()) r.patientId else p.s("patientId"),
                    disease = if (r.disease.isNotBlank()) r.disease else p.s("disease"),
                    address = if (r.address.isNotBlank()) r.address else buildAddr(p),
                    // 🔵 V566: RMP ও অসময়ের রোগী — রোগীর সারিতেই আছে
                    refBy = if (r.refBy.isNotBlank()) r.refBy else p.s("refBy"),
                    refDoctor = if (r.refDoctor.isNotBlank()) r.refDoctor else p.s("refDoctor"),
                    timeType = if (r.timeType.isNotBlank()) r.timeType else p.s("timeType")
                )
            }
        } catch (_: Throwable) {
            return rows   // যাই হোক, তালিকা কখনো নষ্ট হবে না
        }
    }

    // 🔵 TK-ORDER (07.08.2026): পড়া ব্যর্থ হলে আর ₹0/অসম্পূর্ণ টোটাল ফেরাবে না।
    // আগে ব্যর্থ payments/products পড়া `?: JSONArray()` দিয়ে **খালি** ধরা হত →
    // মাসিক/তারিখের Collection ₹0 বা কম দেখাত। এখন ব্যর্থ হলে **null** ফেরে
    // (fetchTodayCollection-এর মতোই), তাই ডাকা পর্দা শেষ-জানা তথ্য রাখে — ₹0 নয়।
    // ⛔ একই দুটো cloud-read (fetchListSlimOrNull নিজেই full-column fallback করে,
    //    তাই null মানে সত্যিকারের ব্যর্থতা) — Supabase free-plan-এ বাড়তি call নেই।
    // 🔁 পুরনো: রিটার্ন-টাইপ `List<CollectionRow>`, দুই জায়গায় `?: org.json.JSONArray()`।
    fun fetchCollectionRange(branchFilter: String?, fromDate: String, toDate: String): List<CollectionRow>? {
        val branchPart = if (branchFilter != null && branchFilter != "All")
            "&branch=eq.${java.net.URLEncoder.encode(branchFilter, "UTF-8")}" else ""
        val range = "date=gte.$fromDate&date=lte.$toDate$branchPart"
        val rows = mutableListOf<CollectionRow>()

        // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): the rows and
        // the date/branch filter are exactly as before -- only the columns this
        // function never reads (edit history, notes) are no longer downloaded.
        // ⛔ NOT ONE AMOUNT CHANGES: PaymentModel.parsePaymentRow reads payType,
        // remarks, date, name, mobile, branch, mode, amount and patientCode --
        // every one of them is asked for below. A failed narrowed read falls
        // back to every column by itself (fetchListSlimOrNull).
        val payments = SupabaseClient.fetchListSlimOrNull(
            "payments", range, 5000,
            // V216 (§13): refundApprovalStatus যোগ (refund চেনা ও বিয়োগের জন্য)।
            // 🐞🔒 V1322 (TK-নির্দেশ) — receivedBy যোগ, কোন স্টাফ পেমেন্ট নিয়েছেন
            // সেটা কার্ডে দেখানোর জন্য। শুধু দেখানোর তথ্য — টাকার হিসাব অপরিবর্তিত।
            "id,payType,remarks,date,name,mobile,branch,mode,amount,cashAmount,onlineAmount,dailyEvents,patientCode,refundApprovalStatus,updatedAt,createdAt,receivedBy"
        ) ?: return null   // 🔵 পড়া ব্যর্থ → null (₹0/অসম্পূর্ণ টোটাল নয়)
        for (i in 0 until payments.length()) {
            val row = payments.getJSONObject(i)
            val amount = row.optDouble("amount", 0.0)
            // 🔒 V216 (§13): Monthly/History collection-এও approved refund বিয়োগ
            // (negative row), pending/rejected বাদ — daily screen-এর হুবহু একই নিয়ম।
            if (PaymentModel.isRefundRow(row)) {
                if (PaymentModel.isApprovedRefund(row)) {
                    rows.add(PaymentModel.parseApprovedRefundRow(row))
                }
                continue
            }
            if (amount > 0) rows.add(PaymentModel.parsePaymentRow(row))
        }

        // Same as above: only what the loop below reads.
        val products = SupabaseClient.fetchListSlimOrNull(
            "products", range, 5000,
            "id,date,customer,mobile,branch,mode,deposit,total,updatedAt"   // (the products table has no "name" column at all)
        ) ?: return null   // 🔵 পড়া ব্যর্থ → null (₹0/অসম্পূর্ণ টোটাল নয়)
        for (i in 0 until products.length()) {
            val row = products.getJSONObject(i)
            val deposit = row.optDouble("deposit", row.optDouble("total", 0.0))
            if (deposit > 0) {
                rows.add(
                    CollectionRow(
                        source = "Medicine Payment",
                        date = row.s("date"),
                        name = row.s("customer").ifBlank { row.s("name").ifBlank { "Walk-in" } },
                        mobile = row.s("mobile"),
                        branch = row.s("branch"),
                        mode = PaymentModel.normalizeMode(row.s("mode").ifBlank { "CASH" }),
                        amount = deposit,
                        time = PaymentModel.displayTime12(row.s("createdAt")),
                        paidAt = row.s("createdAt"),                    // 🔵 V565
                        cashAmount = if (PaymentModel.normalizeMode(row.s("mode").ifBlank { "CASH" }) == "CASH") deposit else 0.0,
                        onlineAmount = if (PaymentModel.normalizeMode(row.s("mode").ifBlank { "CASH" }) == "ONLINE") deposit else 0.0
                    )
                )
            }
        }
        return fillPatientIds(PaymentModel.mergeDailyTreatmentCollections(rows)).sortedByDescending { it.date }
    }

    /** TK-REQUESTED ADDITION (2026-07-20): search patients by NAME or MOBILE
     *  (whichever the person typed) for the redesigned Search Patient dialog.
     *  Returns light-weight matches only (no bill/paid computed yet -- that
     *  still happens via findPatientByMobile() once a result is tapped, so
     *  the existing Add-Treatment-Payment flow is unchanged). Client-side
     *  filtering, same pattern already used by Global Search. */
    fun searchPatients(query: String): List<PatientBillInfo> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qDigits = q.filter { it.isDigit() }
        val qLower = q.lowercase()
        // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): Search Patient
        // used to pull the whole patients table with 38 columns just to show
        // name / mobile / branch / Patient ID. Only those are asked for now.
        // ⛔ THE SEARCH ITSELF IS UNCHANGED: same rows, same limit, same order,
        // and every matching line below is left word for word (bill and paid
        // were already set to 0.0 here -- they are worked out later, when a
        // result is actually tapped). A failed narrowed read falls back to
        // every column by itself.
        val rows = SupabaseClient.fetchListSlim(
            "patients", null, 5000,
            "id,name,mobile,branch,patientId,updatedAt"
        )
        val seenMobiles = HashSet<String>()
        val out = mutableListOf<PatientBillInfo>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val name = row.s("name")
            val mobile = row.s("mobile")
            val mobileDigits = mobile.filter { it.isDigit() }.takeLast(10)
            val nameMatch = qLower.isNotEmpty() && name.lowercase().contains(qLower)
            val mobileMatch = qDigits.length >= 3 && mobileDigits.contains(qDigits)
            if (!nameMatch && !mobileMatch) continue
            /* 🔵🔒 V520 (২২.০৮.২০২৬) — **এক মোবাইলে দুজন হলে দুটোই দেখাবে।**
               নিচের `seenMobiles` লাইনটা এক মোবাইলের **একটাই** সারি রাখে (ভুলে
               দুবার রেজিস্ট্রেশন হলে দুটো কার্ড দেখানো ঠিক নয় — সেটা অপরিবর্তিত)।
               কিন্তু স্টাফ যখন নিজে *"Different Patient — Same Mobile"* চেপে
               আলাদা রোগী বানিয়েছেন, তখন সেই সারিটা **আলাদা রোগী** — তাকে চাপা
               দেওয়া চলবে না, নইলে Payment-এ স্বামীকে খুঁজে স্ত্রীকেই পাওয়া যেত।
               ⛔ পুরোনো সব সারির আইডিতে ওই চিহ্ন নেই, তাই তাদের আচরণ হুবহু আগের। */
            val declaredSeparate = PatientModel.isDeclaredSeparateRowId(row.s("id"), mobileDigits)
            if (!declaredSeparate && !seenMobiles.add(mobileDigits.ifBlank { row.s("id") })) continue
            if (declaredSeparate) seenMobiles.add(row.s("id"))
            out.add(
                PatientBillInfo(
                    id = row.s("id"),
                    name = name,
                    mobile = mobile,
                    branch = row.s("branch"),
                    patientId = row.s("patientId"),
                    bill = 0.0,
                    paid = 0.0,
                    billLocked = false,
                    disease = row.s("disease"),   // 🔒 B562
                    address = row.s("address")    // 🔒 B563
                )
            )
            if (out.size >= 20) break
        }
        return out
    }

    /** Finds a registered patient by mobile, with their current bill/paid
     * totals -- used by Add Treatment Payment. Returns null if not found. */
    /**
     * TK-REPORTED BUG FIX (2026-07-26, from TK's two photo-proofs): Chamber
     * Attendance showed BUBUBU as COB-26072026-001 (Cooch Behar), while the
     * SAME mobile's Patient card showed branch JALPAIGURI -- and the ₹10,000
     * taken at the Cooch Behar chamber landed on that Jalpaiguri record, so
     * the Cooch Behar staff could not see their own patient at all.
     *
     * Root cause: this used to ask Supabase for ONE row matching the mobile
     * (`limit = 1`) with NO ordering at all. When the same mobile has more
     * than one "patients" row -- the same number used at two branches, or a
     * duplicate registration -- Postgres is free to return either one, and
     * which one came back could change from one call to the next. Whichever
     * row it happened to pick then received the payment and decided the
     * branch shown everywhere else.
     *
     * Fix: read the candidate rows instead of one, and choose deliberately --
     *   1. the row belonging to the branch the payment is actually being
     *      taken at (preferBranch), when the caller knows it;
     *   2. otherwise the row that already carries a real bill (the live
     *      treatment record) over an empty duplicate;
     *   3. otherwise the first row, exactly as before.
     * Callers that don't pass preferBranch behave as before apart from being
     * deterministic. No screen, design or flow is changed.
     */
    /**
     * 🔵🔒 V520 (২২.০৮.২০২৬, TK-অনুমোদিত) — `preferPatientCode` / `preferRowId`
     *
     * এক মোবাইলে একাধিক রোগী থাকতে পারেন (স্বামী/স্ত্রী)। ডাকার জায়গা যদি
     * **কোন রোগী** তা জানে (Chamber/Follow-up সারিতে Official Patient ID থাকে,
     * অথবা স্টাফ নিজে বেছে দিয়েছেন), তাহলে সেটাই ব্যবহার হয়।
     * ⛔ দুটোই ফাঁকা রাখলে **হুবহু আগের আচরণ** (`pickPatientRow`) — তাই
     *    পুরোনো কোনো ডাকার জায়গা বদলাতে হয়নি।
     */
    fun findPatientByMobile(
        mobileDigitsOnly: String,
        preferBranch: String = "",
        preferPatientCode: String = "",
        preferRowId: String = ""
    ): PatientBillInfo? {
        val normalized = PatientModel.normalizedMobile(mobileDigitsOnly)
        val patients = SupabaseClient.findByMobile("patients", normalized, "id,name,mobile,branch,patientId,bill", 20)
        if (patients.length() == 0) return null
        // TK-REQUESTED (2026-07-27), ধাপ ৩: this same rule now lives in ONE
        // place (PatientIdentity.pickPatientRow) and every other screen uses
        // it too, so no two screens can pick a different row for the same
        // person. The rule itself is unchanged, word for word.
        /* 🔵🔒 V520: ডাকার জায়গা কোন রোগী তা বলে দিলে ঠিক সেই সারিটাই।
           ⛔ না বললে, বা ওই আইডি এই নম্বরে না থাকলে — হুবহু আগের পথ। */
        var forced: org.json.JSONObject? = null
        if (preferRowId.isNotBlank() || preferPatientCode.isNotBlank()) {
            for (i in 0 until patients.length()) {
                val r = patients.optJSONObject(i) ?: continue
                if ((preferRowId.isNotBlank() && r.s("id") == preferRowId) ||
                    (preferPatientCode.isNotBlank() && r.s("patientId") == preferPatientCode)) { forced = r; break }
            }
        }
        val p = forced ?: PatientIdentity.pickPatientRow(patients, preferBranch) ?: patients.getJSONObject(0)
        val bill = p.optDouble("bill", 0.0)
        val patientId = p.optString("id")

        // 🚨 TK-REPORTED (28.07.2026, খাতার সারি B30): একই রোগীর একাধিক সারি
        // তৈরি হয়ে গিয়েছিল। নতুন সারি তৈরি হওয়া বন্ধ হয়েছে, কিন্তু **আগে যেগুলো
        // হয়ে গেছে** সেগুলোর টাকা যেন হারিয়ে না যায়।
        //
        // আগে টাকা গোনা হত **শুধু বেছে নেওয়া সারিটার** নামে। অন্য সারির নামে
        // জমা টাকা হিসাবেই আসত না — রোগীর বকেয়া বেশি দেখাত।
        //
        // এখন ওই মোবাইলের **সব ক'টা সারির** নামে জমা টাকা একসাথে গোনা হয়।
        // ⛔ দু'বার গোনা হওয়া অসম্ভব: একটাই অনুরোধে সব সারি আনা হয়, তাই একটা
        // পেমেন্টের সারি একবারই আসে।
        /* 🔴🔴🔒 V520 (২২.০৮.২০২৬) — **দুই রোগীর টাকা কখনো মিশবে না।**
           নিচের নিয়মটা (ওই মোবাইলের **সব** সারির টাকা একসাথে গোনা) বসানো
           হয়েছিল "এক মোবাইল = এক রোগী" ধরে নিয়ে — একই মানুষের ভুল করে
           দুটো সারি হলে টাকা যেন হারিয়ে না যায় (খাতার সারি B30)।
           V516-এর পরে এক নম্বরে **সত্যিই দুজন আলাদা রোগী** থাকতে পারেন;
           তখন এই যোগফল স্বামীর টাকা স্ত্রীর নামে দেখিয়ে দিত।
           ⇒ এই নম্বরে ঘোষিত আলাদা রোগী থাকলে হিসাব হয় **শুধু বেছে নেওয়া
             রোগীর নিজের আইডি ধরে**। ⛔ নইলে হুবহু আগের নিয়ম, B30 অটুট। */
        val mobDigits = mobileDigitsOnly.filter { it.isDigit() }.takeLast(10)
        var mixedMobile = false
        for (i in 0 until patients.length()) {
            val row = patients.optJSONObject(i) ?: continue
            if (PatientModel.isDeclaredSeparateRowId(row.s("id"), mobDigits)) { mixedMobile = true; break }
        }
        val allIds = LinkedHashSet<String>()
        if (mixedMobile) {
            p.optString("id").takeIf { it.isNotBlank() }?.let { allIds.add(it) }
            p.optString("patientId").takeIf { it.isNotBlank() }?.let { allIds.add(it) }
        } else {
            for (i in 0 until patients.length()) {
                val row = patients.optJSONObject(i) ?: continue
                row.optString("id").takeIf { it.isNotBlank() }?.let { allIds.add(it) }
                row.optString("patientId").takeIf { it.isNotBlank() }?.let { allIds.add(it) }
            }
        }
        val payFilter = if (allIds.size > 1)
            "patientId=in.(" + allIds.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") } + ")"
        else PatientIdentity.identityFilter(patientId, p.s("patientId"))
        // 🔵 TK-ORDER (07.08.2026): payments পড়া **ব্যর্থ** ও "সত্যিই কোনো পেমেন্ট
        // নেই" আলাদা করা হয় (fetchListOrNull)। ব্যর্থ হলে paid গোনা 0-ই থাকে (আগের
        // মতোই, fail-safe — Due বেশি দেখায়, refund আটকে থাকে), কিন্তু নিচে flag দিয়ে
        // Add-Payment ফর্মকে জানানো হয় যাতে সে ₹0-কে সত্যি না ধরে সতর্কবার্তা দেয়।
        // ⛔ একই একটাই cloud-read; টাকার হিসাব/লুপ এক অক্ষরও বদলায়নি।
        val paymentsRaw = if (payFilter != null) SupabaseClient.fetchListOrNull("payments", payFilter, 500)
            else SupabaseClient.fetchListOrNull("payments", "patientId=eq.$patientId")
        val paymentsFailed = paymentsRaw == null
        val paymentRows = paymentsRaw ?: org.json.JSONArray()
        var paid = 0.0
        // 🔴🔴 V509 (২১.০৮.২০২৬, TK-সিদ্ধান্ত — "Visit Fee-ও ফেরতের সীমায় ধরা হবে"):
        // Visit Fee-র যোগফল **আলাদা** ঘরে রাখা হয়, `paid`-এ মেশানো হয় না —
        // কারণ `paid` দিয়ে Due (Bill − paid) হিসাব হয়, আর Visit Fee বিলের অংশ নয়।
        // মিশিয়ে দিলে প্রত্যেক রোগীর Due ভুল হয়ে যেত। শুধু Refund-এর সীমা ঠিক
        // করার সময় (PatientBillInfo.refundableTotal) দুটো যোগ হয়।
        var visitFeePaid = 0.0
        var treatmentCount = 0
        // 🔒 খাতার সারি B52: টাকা নেওয়া হয়েছে এমন আলাদা দিনগুলো + আজকের মোট।
        val payDates = sortedSetOf<String>()
        var paidToday = 0.0
        val todayStr = PaymentModel.today()
        // 🔵 ক্লাউডে আসা প্রতিটা payment-এর id — ফোনে জমা একই সারি যেন দ্বিতীয়বার গোনা
        // না হয় (নিচের pending-merge এই set দেখে দুবার-গোনা আটকায়)। TK: "একই পেমেন্ট দুবার নয়।"
        val seenPayIds = HashSet<String>()
        for (i in 0 until paymentRows.length()) {
            val row = paymentRows.getJSONObject(i)
            row.optString("id").takeIf { it.isNotBlank() }?.let { seenPayIds.add(it) }
            if (isTreatmentPaymentRow(row.s("payType").ifBlank { "treatment" }, row.s("remarks"))) {
                paid += row.optDouble("amount", 0.0)
                treatmentCount++
                // 🔒 খাতার সারি B55 (যাচাইয়ে ধরা পড়েছে): তারিখটা সবসময়
                // প্রথম ১০ অক্ষর ধরেই দেখা হয় (`yyyy-MM-dd`) — কোনো সারিতে
                // সময় জুড়ে থাকলেও যেন সেটা আলাদা দিন হিসেবে গোনা না হয়।
                // দেখানোর ফাংশনেও (`PaymentModel.dayBasedLabelById`) ঠিক এই
                // একই কাটা, তাই সেভের সময় আর দেখানোর সময় কখনো আলাদা হবে না।
                val d = row.s("date").take(10)
                if (d.isNotBlank()) payDates.add(d)
                if (d == todayStr) paidToday += row.optDouble("amount", 0.0)
            } else if (!PaymentModel.isRefundRow(row) &&
                PaymentModel.isVisitFeeRow(row.s("payType"), row.s("remarks"))) {
                // 🔴 V509: Visit Fee — আলাদা ঘরে (Due-তে ঢোকে না, শুধু Refund-এর সীমায়)।
                visitFeePaid += row.optDouble("amount", 0.0)
            } else if (PaymentModel.isApprovedRefund(row)) {
                // 🔒 V216 (§13, 31.07.2026): approved Refund টাকা রোগীর paid total
                // থেকে **বিয়োগ** হয় (শুধু approved; pending/rejected কমায় না)।
                // ⛔ পুরোনো payment row কিছু বদলায় না — এটা আলাদা refund row-এর হিসাব।
                // 🔴 V509: আগে এখানে লেখা ছিল "refund-এ Visit Fee অক্ষত থাকে" — TK-এর
                //    ২১.০৮.২০২৬-এর সিদ্ধান্তে সেই নিয়ম বদলেছে। বিয়োগটা আগের মতোই
                //    `paid` থেকেই হয়; লুপের **পরে** (নিচে) দেখা হয় সেটা চিকিৎসার
                //    জমাকে ছাড়িয়ে গেছে কিনা — ছাড়ালে বাড়তিটুকু Visit Fee থেকে যায়।
                val amt = row.optDouble("amount", 0.0)
                paid -= amt
                val d = row.s("date").take(10)
                if (d == todayStr) paidToday -= amt
            }
        }
        // 🔵🔒 (09.08.2026, TK-অনুমোদিত — "সফল পরে বাকি" ফিক্স, শর্ত: একই পেমেন্ট দুবার নয়):
        // ফোনে জমা-থাকা (এখনো ক্লাউডে যায়নি) এই রোগীরই payment paid-এ ধরা হয়, নইলে sync-এর
        // আগ পর্যন্ত Due আবার "বাকি" দেখাত। "আজকের কালেকশন"-এর প্রমাণিত pending-merge প্যাটার্নের মিরর।
        // ⛔ দুবার-গোনা অসম্ভব: ক্লাউডে যে id আগেই এসেছে (seenPayIds) তা বাদ; শুধু এই রোগীর
        //    (patientId allIds-এ আছে) সারি; ধরন/refund হিসাব ক্লাউড-লুপের হুবহু একই। টাকার নিয়ম বদলায়নি।
        context?.let { ctx ->
            try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("payments", paymentRows) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
            val pend = try { LocalWorkflowStore(ctx).pendingPayments() } catch (_: Throwable) { org.json.JSONArray() }
            for (i in 0 until pend.length()) {
                val row = pend.getJSONObject(i)
                val pid = row.optString("id")
                if (pid.isBlank() || seenPayIds.contains(pid)) continue          // ইতিমধ্যে ক্লাউডে/গোনা — দুবার নয়
                val rPatient = row.s("patientId")
                if (rPatient.isBlank() || !allIds.contains(rPatient)) continue    // শুধু এই রোগীর টাকা
                seenPayIds.add(pid)
                if (isTreatmentPaymentRow(row.s("payType").ifBlank { "treatment" }, row.s("remarks"))) {
                    paid += row.optDouble("amount", 0.0)
                    treatmentCount++
                    val d = row.s("date").take(10)
                    if (d.isNotBlank()) payDates.add(d)
                    if (d == todayStr) paidToday += row.optDouble("amount", 0.0)
                } else if (!PaymentModel.isRefundRow(row) &&
                    PaymentModel.isVisitFeeRow(row.s("payType"), row.s("remarks"))) {
                    visitFeePaid += row.optDouble("amount", 0.0)   // 🔴 V509 — ক্লাউড-লুপের হুবহু একই নিয়ম
                } else if (PaymentModel.isApprovedRefund(row)) {
                    val amt = row.optDouble("amount", 0.0)
                    paid -= amt
                    val d = row.s("date").take(10)
                    if (d == todayStr) paidToday -= amt
                }
            }
        }
        // 🔴🔴 V509 (২১.০৮.২০২৬, TK-সিদ্ধান্ত) — **ফেরত কোন পকেট থেকে গেল।**
        //
        // নিয়ম: ফেরত আগে **চিকিৎসার জমা** থেকে যায়; সেটা ফুরিয়ে গেলে বাকিটুকু
        // **Visit Fee** থেকে। তাই এখানে `paid` কখনো ঋণাত্মক হয় না, আর Due
        // (Bill − paid) কখনো ফুলে ওঠে না।
        //
        // ⛔ যতটা সত্যিই Visit Fee-তে জমা আছে, ঠিক ততটাই নেওয়া হয় (`minOf`) —
        //    এক পয়সাও বেশি নয়। তাই কোথাও টাকা তৈরি হতে পারে না।
        //
        // ⚠️⚠️ **সৎ সীমাবদ্ধতা — নিজের যাচাইয়ে ধরা পড়েছে (২১.০৮.২০২৬), TK-কে
        //    জানানো হয়েছে:** প্রথমে লিখেছিলাম "পুরোনো কোনো রেকর্ড এটা ছুঁতেই
        //    পারে না" — **সেটা ভুল ছিল।** একটাই অবস্থায় পুরোনো রেকর্ডেও এটা
        //    কাজ করে: যদি কোনো রোগীর **পেমেন্টের সারি মুছে ফেলা হয়েছে অথচ তার
        //    ফেরতের সারিটা রয়ে গেছে** (তখন `paid` ঋণাত্মক হয়ে যায়)। ঐ রোগীর
        //    Due এতদিন **ফেরতের টাকাটা আবার "বাকি" হিসেবে দেখাত** (৫০০০ ফেরত
        //    দিলে Due ৫০০০ বেড়ে যেত) — সেটাই আসলে ভুল ছিল; এখন Visit Fee-তে
        //    যতটা আছে ততটা শুধরে যায়।
        //    ⛔ যে রোগীর Visit Fee-র সারিই নেই, তার হিসাব **অবিকল আগের মতোই**
        //       (নিচের `visitFeePaid > 0.0` শর্তটাই সেটা নিশ্চিত করে)।
        val over = (-paid).coerceAtLeast(0.0)
        if (over > 0.0 && visitFeePaid > 0.0) {
            val take = minOf(over, visitFeePaid)
            visitFeePaid -= take
            paid += take
        }
        treatmentPaymentCounts[patientId] = treatmentCount
        treatmentPaymentDates[patientId] = payDates.toList()
        treatmentPaidOnDate[patientId] = paidToday

        return PatientBillInfo(
            id = patientId,
            name = p.s("name"),
            mobile = p.s("mobile"),
            branch = p.s("branch"),
            patientId = p.s("patientId"),
            bill = bill,
            paid = paid,
            billLocked = bill > 0,
            paymentsUnverified = paymentsFailed,   // 🔵 payments পড়া ব্যর্থ হলে true
            disease = p.s("disease"),              // 🔒 B562: কার্ডে রোগের নাম দেখাতে
            // 🔒 B571 (08.08.2026, TK-নির্দেশ "ID-র নিচে ঠিকানা"): একক `address`
            // ঘর ফাঁকা থাকলে (গ্রাম/পোস্ট/থানা আলাদা ঘরে থাকলে) PatientModel.
            // buildAddress-এর হুবহু নিয়মে কম্পোনেন্ট জোড়া লাগিয়ে ঠিকানা বানানো হয়,
            // তাই Payment কার্ডেও ঠিকানা দেখায়। ⛔ কোনো তথ্য বদলায় না — শুধু দেখানো।
            address = p.s("address").ifBlank {
                listOf(
                    "Vill" to p.s("village"), "PO" to p.s("po"), "PS" to p.s("ps"),
                    "Dist" to p.s("district"), "PIN" to p.s("pin")
                ).filter { it.second.isNotBlank() }.joinToString(", ") { "${it.first}: ${it.second}" }
            },
            // 🔴 V509 (TK-সিদ্ধান্ত ২১.০৮.২০২৬): Refund-এর সীমায় Visit Fee-ও ধরা হবে।
            visitFeePaid = visitFeePaid
        )
    }

    /**
     * 🔵🔒 V520 (২২.০৮.২০২৬, TK-অনুমোদিত — **walk-in**) — এই নম্বরে কারা কারা আছেন।
     *
     * **কেন দরকার:** Payment পর্দায় স্টাফ শুধু **মোবাইল নম্বরটাই** টাইপ করেন —
     * আর কিছু জানা থাকে না। ওই নম্বরে যদি স্বামী ও স্ত্রী **দুজন আলাদা রোগী**
     * থাকেন, তাহলে কার টাকা নেওয়া হচ্ছে সেটা অ্যাপের পক্ষে আন্দাজ করা অসম্ভব।
     * তাই তালিকাটা ফিরিয়ে দেওয়া হয়, আর পর্দা স্টাফকে **নাম দেখিয়ে জিজ্ঞাসা**
     * করে নেয়।
     *
     * ⛔ **নতুন কোনো cloud-read নয়** — `findPatientByMobile()` ঠিক **এই একই**
     *    অনুরোধটাই করে (একই table · একই filter · একই কলাম · একই limit), তাই
     *    `CloudReadDedupe` ওটাকে দ্বিতীয়বার নেটে পাঠায় না। Free Plan-এর
     *    egress-এ **এক বাইটও** বাড়ে না।
     * ⛔ টাকার কোনো হিসাব এখানে হয় না (bill/paid শূন্য) — হিসাব আগের মতোই
     *    `findPatientByMobile()`-ই করে, স্টাফ একজনকে বেছে নেওয়ার পরে।
     * ⛔ একজনই থাকলে (রোজকার ৯৯% ক্ষেত্রে) তালিকায় একটাই নাম — ডাকার জায়গা
     *    তখন কিছুই জিজ্ঞাসা করে না, আচরণ **হুবহু আগের মতোই**।
     */
    fun identitiesOnMobile(mobileDigitsOnly: String, preferBranch: String = ""): List<PatientBillInfo> {
        val normalized = PatientModel.normalizedMobile(mobileDigitsOnly)
        val patients = SupabaseClient.findByMobile("patients", normalized, "id,name,mobile,branch,patientId,bill", 20)
        val mobDigits = mobileDigitsOnly.filter { it.isDigit() }.takeLast(10)
        fun info(row: org.json.JSONObject) = PatientBillInfo(
            id = row.s("id"), name = row.s("name"), mobile = row.s("mobile"),
            branch = row.s("branch"), patientId = row.s("patientId"),
            bill = row.optDouble("bill", 0.0), paid = 0.0, billLocked = false
        )
        val out = mutableListOf<PatientBillInfo>()
        // ১) ভুলে দুবার রেজিস্ট্রেশন হওয়া সারিগুলো **আগের মতোই এক রোগী** — তাই
        //    তাদের মধ্যে বাছাই হয় ঠিক সেই এক নিয়মেই যেটা নয়টা পর্দা মানে
        //    (`PatientIdentity.pickPatientRow`, V143 · খাতার সারি B30)।
        val ordinary = org.json.JSONArray()
        for (i in 0 until patients.length()) {
            val row = patients.optJSONObject(i) ?: continue
            if (!PatientModel.isDeclaredSeparateRowId(row.s("id"), mobDigits)) ordinary.put(row)
        }
        PatientIdentity.pickPatientRow(ordinary, preferBranch)?.let { out.add(info(it)) }
        // ২) স্টাফ নিজে ঘোষণা-করা প্রত্যেক আলাদা রোগী **নিজের নামে** আলাদা।
        for (i in 0 until patients.length()) {
            val row = patients.optJSONObject(i) ?: continue
            if (PatientModel.isDeclaredSeparateRowId(row.s("id"), mobDigits)) out.add(info(row))
        }
        return out
    }

    /** Web openVisitAdvancePayment(): patientForVisit(x) || makePatient(x).
     *  If no patients row exists yet for this mobile, create a minimal one from
     *  the follow-up card's data so Advance is never blocked.
     *
     *  🚨 TK'S RULE (28.07.2026, খাতার সারি B30): *"কোন প্রকার রোগীর যেন ডুপ্লিকেট
     *  না হয়। সিস্টেমে যদি আগে থেকে থাকে অবশ্যই ওয়ার্নিং দিতে হবে।"*
     *
     *  **এখানেই ডুপ্লিকেট তৈরি হত (TK-এর ফটোর ABDUL KAYAM — একই Patient ID
     *  `JPE-04072026-001`, দুটো সারি, তারিখ আলাদা):**
     *  আগে শুধু দেখা হত ক্লাউডে রোগী পাওয়া গেল কি না। কিন্তু **লাইন খারাপ হলেও
     *  "পাওয়া যায়নি" বলেই ধরা হত** — তাই অ্যাডভান্স নেওয়ার সময় **নতুন একটা সারি**
     *  তৈরি হয়ে যেত, আর তাতে **কার্ড থেকে আসা পুরনো Patient ID**-টাই বসে যেত।
     *  ফল: এক রোগীর দুটো সারি, একই Patient ID, আজকের তারিখ — কেউ কিছু জানতেও
     *  পারত না।
     *
     *  **এখন তিন ধাপে দেখা হয়, তারপরেই কেবল নতুন সারি:**
     *   ১. ক্লাউডে খোঁজা — পাওয়া গেলে ওটাই ব্যবহার হয় (আগের মতোই)।
     *   ২. **ক্লাউডে দেখাই না গেলে (লাইন খারাপ) — কিছুই তৈরি হয় না, `null`
     *      ফেরত যায়**, আর ডাকা পর্দা স্টাফকে ওয়ার্নিং দেখায়।
     *   ৩. ফোনের নিজের জমানো তালিকাতেও খোঁজা হয় — সেখানে থাকলেও নতুন সারি নয়।
     *
     *  ⛔ রোগী সত্যিই নতুন হলে আগের মতোই সারি তৈরি হয়, কিছুই আটকায় না।
     */
    fun findOrMakePatient(name: String, mobileDigits: String, branch: String, existingPatientId: String): PatientBillInfo? {
        /* 🔵🔒 V520: কার্ড থেকে আসা Patient ID (`existingPatientId`) **রোগী-প্রতি
           অনন্য**, তাই এক নম্বরে দুজন থাকলেও ঠিক রোগীটাই বেছে নেওয়া যায়।
           ⛔ ফাঁকা থাকলে বা ওই আইডি এই নম্বরে না থাকলে — হুবহু আগের পথ। */
        findPatientByMobile(mobileDigits, preferPatientCode = existingPatientId)?.let { return it }
        val normalized = PatientModel.normalizedMobile(mobileDigits)

        // ধাপ ২ — সত্যিই যাচাই করা গেল কি না। না গেলে কিছুই তৈরি হবে না।
        val verify = SupabaseClient.findByMobileOrNull("patients", normalized, "id", 1)
            ?: return null
        if (verify.length() > 0) {
            // এইমাত্র ক্লাউডে পাওয়া গেল (উপরের খোঁজাটা হয়তো ফসকেছিল) — নতুন নয়।
            findPatientByMobile(mobileDigits, preferPatientCode = existingPatientId)?.let { return it }
            return null
        }

        // ধাপ ৩ — এই ফোনেই আগে সেভ করা আছে কি না।
        context?.let { ctx ->
            val local = try { LocalWorkflowStore(ctx).findPatientByMobile(normalized) } catch (_: Throwable) { null }
            if (local != null) {
                return PatientBillInfo(
                    id = local.s("id"), name = local.s("name").ifBlank { name },
                    mobile = local.s("mobile").ifBlank { normalized },
                    branch = local.s("branch").ifBlank { branch },
                    patientId = local.s("patientId").ifBlank { existingPatientId },
                    bill = local.optDouble("bill", 0.0), paid = 0.0,
                    billLocked = local.optDouble("bill", 0.0) > 0,
                    disease = local.s("disease"),   // 🔒 B562
                    address = local.s("address")    // 🔒 B563
                )
            }
        }

        val today = PatientIdGenerator.todayIso()
        val pid = existingPatientId.ifBlank { PatientIdGenerator.generate(branch, today, context) }
        // 🔒 খাতার সারি B30 — একই নিয়ম: সারির আইডি মোবাইল থেকেই তৈরি, তাই একই
        // নম্বরে দ্বিতীয় সারি তৈরি হওয়া আর সম্ভব নয় (PatientModel.stableRowId)।
        val rowId = PatientModel.stableRowId(mobileDigits)
        val mob = normalized
        val row = org.json.JSONObject()
            .put("id", rowId).put("patientId", pid)
            .put("date", today).put("registrationDate", today).put("visitDate", today)
            .put("name", name).put("mobile", mob).put("branch", branch)
            .put("disease", "Piles").put("bill", 0)
            .put("stage", "Treatment Running")
            .put("createdAt", isoNow()).put("updatedAt", isoNow())
        SupabaseClient.upsert("patients", row)
        context?.let { try { LocalWorkflowStore(it).upsertPatient(row, "SYNCED") } catch (_: Throwable) { } }
        return PatientBillInfo(id = rowId, name = name, mobile = mob, branch = branch,
            patientId = pid, bill = 0.0, paid = 0.0, billLocked = false, disease = "Piles")
    }

    /**
     * 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত) — **নম্বর বাড়ে দিন ধরে, সেভ ধরে নয়।**
     *
     * TK-এর নিয়ম: ১ম টাকা = `Advance`, তারপর `2nd Payment` · `3rd Payment` …।
     * **একই দিনে যতবারই টাকা নেওয়া হোক, নম্বর একটাই থাকবে**; পরের নম্বর তখনই,
     * যখন রোগী **আবার নতুন দিনে** এসে টাকা দেবেন।
     *
     * ⛔ পুরনো নিয়মটা (সারি গুনে নম্বর) আর ব্যবহার হয় না, কিন্তু গোনাটা
     *    (`treatmentPaymentCounts`) মোছা হয়নি — দরকারে ফেরানো যাবে।
     *
     * @param forDate কোন দিনের জন্য নম্বর চাই (ফাঁকা = আজ)। ব্যাকডেট পেমেন্টে
     *        ওই তারিখটাই পাঠাতে হবে, নইলে ভুল নম্বর বসবে।
     */
    fun nextLabelFor(patientId: String, forDate: String = ""): String {
        val day = forDate.ifBlank { PaymentModel.today() }.take(10)
        val dates = treatmentPaymentDates[patientId]
        if (dates == null) {
            // এই রোগীর তালিকা এখনো পড়া হয়নি — আগের নিয়মেই চলুক, কিছু আটকাবে না।
            return PaymentModel.ordinalPaymentLabel((treatmentPaymentCounts[patientId] ?: 0) + 1)
        }
        val idx = dates.indexOf(day)
        // ওই দিনে আগেই টাকা নেওয়া হয়েছে → সেই দিনের নম্বরটাই ফেরত যায়।
        if (idx >= 0) return PaymentModel.ordinalPaymentLabel(idx + 1)
        // নতুন দিন → ওই দিনটা তালিকায় কোথায় বসত, সেই জায়গার নম্বর।
        val position = dates.count { it < day }
        return PaymentModel.ordinalPaymentLabel(position + 1)
    }

    /** 🔒 খাতার সারি B52: ওই রোগীর নামে **আজ** (বা দেওয়া তারিখে) ইতিমধ্যে কত
     *  টাকা নেওয়া হয়ে গেছে — ০-এর বেশি হলে সেভ করার আগে স্টাফকে সতর্কবার্তা
     *  দেখাতে হবে। ⛔ ক্লাউডে বাড়তি কোনো অনুরোধ নেই। */
    fun paidOnDateFor(patientId: String, forDate: String = ""): Double {
        val day = forDate.ifBlank { PaymentModel.today() }.take(10)
        if (day != PaymentModel.today()) return 0.0
        return treatmentPaidOnDate[patientId] ?: 0.0
    }

    /* ═══════════════════════════════════════════════════════════════════
       🔴🔒 V1106 (০৫.০৯.২০২৬, TK-রিপোর্ট, তিনটে ছবিসহ — SADDAM) —
       *"একই দিনে একই ধরনের পেমেন্ট দুইবার হয়ে গেছে তাও আটকালেন না কেন? …
       একটা বলছে আমি তো একবারই করেছিলাম … যেহেতু এটা কোন ডেমো প্রজেক্ট নয়,
       এটা একটা লাইভ ক্লিনিক, বিশেষ করে টাকা পয়সার হিসাব, এগুলো তো আরো
       কড়াকড়ি করতেই হবে"* · *"জিজ্ঞাসা করে নিশ্চিত করাবেন, একেবারে আটকাবেন না"*

       ─── 🔴 আসল ফাঁকটা কোথায় ছিল (মেপে দেখা, আন্দাজ নয়) ──────────────
       B52-এর প্রশ্নটা (`PaymentDayGuard`) আগে থেকেই চার জায়গাতেই বসানো ছিল,
       **কিন্তু সে অঙ্কটা নিত ফোনের নিজের জমানো তালিকা থেকে**
       (`paidOnDateFor` → `treatmentPaidOnDate`), যেটা ভরে **পর্দা খোলার সময়**।
       ⇒ অন্য ফোন বা অন্য স্টাফ একটু আগে টাকা নিয়ে থাকলে এই ফোন সেটা জানতই
         না, অঙ্ক ০ পেত, আর **কোনো প্রশ্নই আসত না**।
       ⇒ ঠিক এই কারণেই স্টাফ সৎভাবে বলতে পারেন *"আমি তো একবারই করেছিলাম"*।

       ─── এখন কী হয় ─────────────────────────────────────────────────────
       সেভ চাপার মুহূর্তে **ক্লাউডকে একবার জিজ্ঞাসা করা হয়** — আজ এই রোগীর
       নামে **হুবহু এই অঙ্কের** টাকা আগে থেকে বসানো আছে কি না। থাকলে কখন
       নেওয়া হয়েছিল সেটাসহ প্রশ্ন — **Cancel = কিছুই হবে না · OK = জেনেশুনে তবুও**
       (V708/V786-এ TK-এর পাশ করা হুবহু একই নিয়ম)।

       ⛔ **কিছুই আটকানো হয় না** — TK-এর স্পষ্ট নির্দেশ মেনে শুধু জিজ্ঞাসা।
       ⛔ **মিলিয়ে দেখা হয় প্রতিটা আলাদা এন্ট্রি ধরে** (`dailyEvents`), দিনের
          মোট ধরে নয় — নইলে ৩,০০০+২,০০০ = ৫,০০০ থাকা দিনে নতুন ৫,০০০ নিতে
          গেলে মিথ্যা সতর্কবার্তা আসত।
       ⛔ Egress (ফ্রি প্ল্যান): **শুধু সেভ চাপার সময়**, একটাই ছোট query, একজন
          রোগীর একটা দিনের সারি — তালিকা খোলার সময় নয়। দিনে ৩০-৬০টা পেমেন্ট
          হলেও কয়েক KB।
       ⛔ নেট খারাপ হলে চুপচাপ `null` — সৎ পেমেন্ট **কখনো** আটকায় না।
       ⛔ ব্যাকডেট করা পেমেন্টে এই প্রশ্ন আসে না (ওটা মাস্টারের নিজের সিদ্ধান্ত)।
       ═══════════════════════════════════════════════════════════════════ */
    /* 🔴🔒 V1152 (০৬.০৯.২০২৬, TK-নির্দেশ) — *"ক্যাশ অনলাইন সেটা কিন্তু আলাদা …
       একই দিনে ১০০০ ক্যাশ আর ১০০০ অনলাইন হলে সতর্কবার্তার কিছু নেই; কিন্তু সকাল
       দশটায় ১০০০ ক্যাশ আর দুপুর দুটোয় আবার ১০০০ ক্যাশ হলে অবশ্যই দেখাতে হবে"*।
       ⇒ মিল এখন **অঙ্ক + ধরন** দুটোতেই। `mode` ফাঁকা পাঠালে আগের মতোই শুধু
         অঙ্ক ধরে মেলে (পুরনো ডাক ভাঙে না)। */
    /* 🔴🔒 V1272 (০৯.০৯.২০২৬, TK-নির্দেশ: *"ব্যাকডেট এর ফাঁকটাও বন্ধ করুন"*) —
       আগে এই যাচাই **শুধু আজকের** সারি দেখত, তাই পুরনো তারিখে টাকা বসালে
       একই অঙ্ক দুবার বসলেও কোনো প্রশ্ন আসত না। এখন `forDate` পাঠালে **ওই
       দিনের** সারিই দেখা হয়।
       ⛔ `forDate` না পাঠালে আচরণ হুবহু আগের মতোই (আজকের দিন) — পুরনো ডাক ভাঙে না।
       ⛔ আটকানো এখনো হয় না, শুধু প্রশ্ন — TK-র পুরনো নিয়ম অটুট।
       ⛔ Egress বাড়ে না — সারির সংখ্যা একই, শুধু কোন দিনের সেটা বদলায়। */
    fun todaysPaymentLike(
        patient: PatientBillInfo, amount: Double, mode: String = "", forDate: String = ""
    ): org.json.JSONObject? {
        if (patient.id.isBlank() || amount <= 0.0) return null
        val wantMode = PaymentModel.normalizeMode(mode).trim()
        fun modeOk(m: String): Boolean =
            wantMode.isBlank() || PaymentModel.normalizeMode(m).equals(wantMode, ignoreCase = true)
        return try {
            val today = DateUtil.iso(forDate).take(10).ifBlank { PaymentModel.today() }
            val rows = SupabaseClient.fetchList(
                "payments",
                "patientId=eq.${patient.id}&payType=eq.treatment&date=eq.$today",
                50, select = "id,amount,mode,payLabel,createdAt,dailyEvents"
            )
            var hit: org.json.JSONObject? = null
            outer@ for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val evs = r.optJSONArray("dailyEvents")
                if (evs != null && evs.length() > 0) {
                    for (j in 0 until evs.length()) {
                        val e = evs.optJSONObject(j) ?: continue
                        if (kotlin.math.abs(e.optDouble("amount", 0.0) - amount) <= 0.5 &&
                            modeOk(e.optString("mode", r.optString("mode", "")))) {
                            hit = org.json.JSONObject()
                                .put("amount", e.optDouble("amount", 0.0))
                                .put("mode", e.optString("mode", r.optString("mode", "")))
                                .put("createdAt", e.optString("createdAt", r.optString("createdAt", "")))
                                .put("payLabel", r.optString("payLabel", ""))
                            break@outer
                        }
                    }
                } else if (kotlin.math.abs(r.optDouble("amount", 0.0) - amount) <= 0.5 &&
                           modeOk(r.optString("mode", ""))) {
                    hit = r; break@outer
                }
            }
            hit
        } catch (_: Throwable) { null }   // নেট খারাপ হলে চুপচাপ সরে দাঁড়ায় — সৎ পেমেন্ট কখনো আটকায় না
    }

    /**
     * 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত) — **ভুল করে নেওয়া টাকা স্টাফ নিজেই মুছতে পারবেন।**
     *
     * TK-এর কথা: *"কোন এক স্টাফ ভুল করে ৩-৪ বার পেমেন্ট নিয়ে নিয়েছে... তৎক্ষণাৎ
     * সেই স্টাফ কেন সেই পেমেন্ট ডিলিট করতে পারবে না?"*
     * TK-এর সিদ্ধান্ত: **স্টাফ নিজের ব্রাঞ্চের আজ ও গতকালের এন্ট্রি মুছতে পারবেন**
     * (মাস্টার যে কোনোটা), আর **মোছার খবর মাস্টারের ঘন্টায় যাবে**।
     *
     * ⛔ **কিছুই হারায় না** — সারিটা আগে Trash-এ যায় (`TrashHelper.moveToTrash`),
     *    তাই দরকারে ফেরানো যায়। Trash-এ তুলতে না পারলে **কিছুই মোছা হয় না**।
     * ⛔ **মুছে ফেলা টাকা আর ফিরে আসতে পারে না** — এই ফোনের জমানো কপি ও
     *    অপেক্ষমাণ তালিকা দুটো থেকেই সরে যায়, আর `DeletedGuard` চিহ্ন দিয়ে রাখে।
     * ⛔ **বিল (bill) ছোঁয়া হয় না** — শুধু ওই টাকার সারিটা ওঠে, তাই জমা/বকেয়া
     *    নিজে থেকেই ঠিক হয়ে যায়।
     * ⛔ **মাস্টারকে জানানোর জন্য নতুন কোনো টেবিল বা SQL লাগে না** — আগে থেকেই
     *    থাকা briefings ব্যবস্থাই ব্যবহার হয়, তাই ঘন্টায় সংখ্যা নিজে থেকেই বাড়ে।
     */
    fun deletePaymentEntry(row: org.json.JSONObject, byMobile: String, byName: String): Boolean {
        val ctx = context ?: return false
        val id = row.optString("id")
        if (id.isBlank()) return false
        // 🔒🔒 খাতার সারি B111 (TK, 29.07.2026 বিকেল ৫.৪০): *"staff ডিলিট করতে
        // পারবে না। Master-এর ঘন্টায় notification আসবে, মাস্টার অনুমতি দিলে
        // তবেই ডিলিট হবে।"*
        //
        // পর্দার বোতামেও পাহারা বসানো হয়েছে, কিন্তু নিয়মটা **এখানেও** থাকা
        // দরকার — নইলে ভবিষ্যতে অন্য কোনো পর্দা এই ফাংশনটা ডাকলে নিয়মটা
        // আবার ফাঁক হয়ে যাবে (ঠিক এভাবেই সারি B98-এর পরেও এই জায়গাটা বাদ
        // পড়ে গিয়েছিল)।
        // ⛔ মাস্টারের জন্য কিছুই বদলায়নি। ⛔ চেনা না গেলে মোছা হয় না — নিরাপদ দিক।
        // 🔒 খাতার সারি B112 (TK, 29.07.2026 বিকেল ৬.১০): স্টাফ **আজ ও গতকালের**
        // এন্ট্রি নিজে মুছতে পারবেন; **ওই দিনের চেম্বার বন্ধ হয়ে গেলে আর নয়**
        // (তখন মাস্টার হিসাব পেয়ে গেছেন)। মাস্টার সবসময় পারবেন।
        // ⛔ পর্দার বোতামেও এই একই যাচাই আছে — এটা দ্বিতীয় স্তরের পাহারা, যাতে
        //    ভবিষ্যতে অন্য কোনো পর্দা এই ফাংশন ডাকলেও নিয়মটা ফাঁক না হয়।
        val actor = try { StaffDirectory.findAccount(byMobile) } catch (_: Throwable) { null }
        val actorUser = actor?.let { NativeUser(it.mobile, it.name, it.branch, it.role) }
        val allowed = try {
            DeletePermission.canDeleteEntryNow(ctx, actorUser, row.s("date"), row.s("branch"), paid = true)
        } catch (_: Throwable) { false }
        // 🆕 B337 (03.08.2026, TK-নির্দেশ) — সাধারণ নিয়মে অনুমতি না থাকলেও,
        // এই স্টাফের ঠিক এই তারিখের জন্য Master-এর দেওয়া সাময়িক Grant থাকলে
        // ডিলিট করতে দেওয়া হয়। ⛔ শুধু এই PAYMENT-ডিলিট পথেই — Enquiry/Patient
        // ডিলিটের সাধারণ নিয়ম (`DeletePermission.canDeleteEntryNow`) অপরিবর্তিত।
        val allowedByGrant = if (!allowed) {
            try { BackdatePaymentGrant.isGrantedNow(byMobile, row.s("date")) } catch (_: Throwable) { false }
        } else false
        if (!allowed && !allowedByGrant) return false
        val moved = try { TrashHelper.moveToTrash("payments", row, byMobile) } catch (_: Throwable) { false }
        if (!moved) return false
        try { DeletedGuard.markDeleted("payments", id, ctx) } catch (_: Throwable) { }
        try { LocalWorkflowStore(ctx).removePayment(id) } catch (_: Throwable) { }
        removePaymentPending(id)
        try {
            val amount = "₹" + "%,.0f".format(row.optDouble("amount", 0.0))
            val label = row.s("payLabel").ifBlank { row.s("paymentLabel").ifBlank { row.s("payType").ifBlank { "Payment" } } }
            val who = byName.ifBlank { byMobile }
            // 🔴 B336 (03.08.2026, TK-রিপোর্ট — "এখানে পেশেন্ট ID ভুল কেন"):
            // `row.s("patientId")` আসলে ডেটাবেসের ভিতরের UUID/রেফারেন্স আইডি
            // (যেমন "pat_8317826054") — মানুষ-পড়া-যায় কোড না। প্রজেক্টের বাকি
            // সব জায়গায় (উদাহরণ: এই ফাইলেরই অন্যত্র, PaymentActivity.kt-এর
            // DeletePermission.sendRequest কল) দেখানোর জন্য সবসময় `patientCode`
            // ব্যবহার হয় (যেমন "KNE-03082026-001")। এই একটা জায়গাতেই ভুল
            // কলাম বসানো ছিল।
            val msg = "$amount ($label) deleted for ${row.s("name").ifBlank { row.s("mobile") }} " +
                "· Patient ID ${row.s("patientCode")} · Branch ${row.s("branch")} " +
                "· Payment date ${DateUtil.display(row.s("date"))} · Deleted by $who. " +
                "The entry is in Trash and can be restored."
            // 🚨 BUILD ERROR FIX (29.07.2026 দুপুর ১.০০, খাতার সারি B85 — TK-এর
            // Android Studio-র ছবি: `Unresolved reference: post`)।
            // **কারণ:** `BriefingRepository` একটা **class**, `object` নয় — তাই
            // ক্লাসের নাম দিয়ে সরাসরি `BriefingRepository.post(...)` ডাকা যায় না;
            // আগে একটা instance বানাতে হয়। প্রজেক্টের বাকি সব জায়গায়
            // (`BriefingActivity` ইত্যাদি) এভাবেই `BriefingRepository()` বানিয়ে
            // ডাকা হয় — এখানেই কেবল বাদ পড়েছিল।
            // ⛔ যা পাঠানো হচ্ছে তার একটি অক্ষরও বদলায়নি — শুধু ডাকার ধরন ঠিক করা।
            BriefingRepository().post(
                ctx, "Payment deleted", msg, "role",
                row.s("branch"), "master", byMobile
            )
        } catch (_: Throwable) { }
        return true
    }

    // =========================================================================
    // 🔴🔒 V471 (20.08.2026, TK-অনুমোদিত ফটো-প্রুফ — "আমার যে ইচ্ছা আমি
    // যেন সেটা করতে পারি"): মিশ্র (একই দিনে একাধিক এন্ট্রি একসাথে জমা হওয়া
    // — `dailyEvents`) পেমেন্ট-সারির **ভেতরের একটামাত্র এন্ট্রি** এডিট/
    // ডিলিট করা — শুধু Master, বাকি ৪টা (বা যত থাকুক) এন্ট্রি অক্ষত।
    // ⛔ উপরের `deletePaymentEntry()` (পুরো-সারি ডিলিট) এক অক্ষরও বদলানো
    //    হয়নি — শেষ এন্ট্রি সরালে **সেই একই প্রমাণিত ফাংশনই** ডাকা হয়,
    //    নতুন কোনো ডিলিট-লজিক তৈরি হয়নি।
    // ⛔ দ্বিতীয় স্তরের পাহারা (এই ফাংশনের ভেতরেই role চেক) — UI-এর
    //    বোতাম Master ছাড়া দেখাই যাবে না, কিন্তু এখানেও নিশ্চিত করা হলো।
    // =========================================================================

    /** মিশ্র পেমেন্টের ভেতরের একটা এন্ট্রি সরিয়ে দেওয়া (বাকিগুলো অক্ষত,
     *  মোট নতুন করে গণনা)। শেষ এন্ট্রি হলে পুরো সারিটাই (Trash-সহ)
     *  deletePaymentEntry()-এর প্রমাণিত পথেই মুছে যায়। */
    fun removeOneDailyEvent(row: JSONObject, eventId: String, byMobile: String, byName: String): Boolean {
        val ctx = context ?: return false
        // 🟢🔒 V618 (২৪.০৮.২০২৬, TK-নির্দেশ, সততার সাথে যাচাই করে) — আগে এখানে
        // সরাসরি "শুধু Master" লেখা ছিল (UI-এর গেট বদলালেও এই ভিতরের লকটা
        // অক্ষতই থেকে যেত — Save চাপলে নীরবে ব্যর্থ হতো)। এখন বাকি সব
        // পেমেন্ট-এডিটের মতোই একই নিয়ম: আজ/গতকাল স্টাফ নিজে পারবেন,
        // তার বেশি পুরনো হলে শুধু Master।
        val user = NativeSession.current(ctx)
        if (user?.role != "master" && !PaymentModel.withinFreeEditWindow(row.s("date"))) return false
        val id = row.optString("id")
        if (id.isBlank() || eventId.isBlank()) return false
        val events = row.optJSONArray("dailyEvents") ?: return false
        val remaining = org.json.JSONArray()
        var removed: JSONObject? = null
        for (i in 0 until events.length()) {
            val e = events.optJSONObject(i) ?: continue
            if (removed == null && e.optString("eventId") == eventId) { removed = e; continue }
            remaining.put(e)
        }
        val removedEvent = removed ?: return false
        if (remaining.length() == 0) {
            return deletePaymentEntry(row, byMobile, byName)
        }
        var newAmount = 0.0; var newCash = 0.0; var newOnline = 0.0
        for (i in 0 until remaining.length()) {
            val e = remaining.getJSONObject(i)
            val amt = e.optDouble("amount", 0.0)
            newAmount += amt
            if (e.optString("mode").equals("CASH", true)) newCash += amt else newOnline += amt
        }
        val fields = JSONObject()
            .put("dailyEvents", remaining)
            .put("amount", newAmount)
            .put("cashAmount", newCash)
            .put("onlineAmount", newOnline)
            .put("mode", if (newCash > 0 && newOnline > 0) "CASH" else if (newOnline > 0) "ONLINE" else "CASH")
            .put("updatedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
        val ok = try { SupabaseClient.updateById("payments", id, fields) } catch (_: Throwable) { false }
        if (!ok) return false
        /* 🔴🔒 V816 (২৯.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — GULGAR HOSSAIN, ₹1 CASH:
           *"পেমেন্ট ডিলিট করার পরেও থেকে যায়, অনেকক্ষণ পরে গিয়ে মোছে কেন?"*)

           **আসল কারণ (কোড ধরে প্রমাণিত, আন্দাজে নয়):**
           একই দিনের দ্বিতীয় টাকাটা আলাদা সারি হয়ে বসে না — সার্ভারের
           `tk_record_treatment_payment` ওটাকে ওই দিনের **একটাই** সারির
           `dailyEvents`-এ ঢুকিয়ে দেয়। কিন্তু টাকা সেভ করার সময় ফোন ওই
           পেমেন্টটা **অপেক্ষমাণ তালিকাতেও** (`piles_clinic_payment_pending`)
           রেখে দেয়, আর সেটা মুছে যায় **শুধু ক্লাউডে পাঠানো সফল হলে**।
           প্রথম চেষ্টা ব্যর্থ হলে (নেটের এক মুহূর্তের গোলমাল) এন্ট্রিটা
           তালিকায় থেকে যায়। এদিকে ভিতরের এন্ট্রিটা মুছে দিলেও **ওই
           অপেক্ষমাণ কপিটা কেউ মুছত না** — পরের বার `flushPending()` চললে
           সেটা আবার সার্ভারে যেত, আর সার্ভার সেই একই `eventId` ওই দিনের
           সারিতে **ফিরিয়ে বসাত**। ⇒ মোছা টাকা ফিরে আসত।

           ⛔ এখন মোছার সঙ্গে সঙ্গেই ওই এন্ট্রির অপেক্ষমাণ কপিটাও তুলে দেওয়া
              হয় — দুটো তালিকা থেকেই — তাই ফিরে আসার আর কোনো পথ নেই।
           ⛔ শুধু **যেটা মোছা হলো ঠিক সেই eventId**-টাই সরে; বাকি এন্ট্রি
              বা অন্য কারো টাকা এক পয়সাও ছোঁয়া হয় না। */
        try { removePaymentPending(eventId) } catch (_: Throwable) { }
        try { CloudWriteQueue.forget("UPSERT", "payments", eventId) } catch (_: Throwable) { }
        try {
            val updatedRow = JSONObject(row.toString())
            val it = fields.keys(); while (it.hasNext()) { val k = it.next(); updatedRow.put(k, fields.get(k)) }
            LocalWorkflowStore(ctx).upsertPayment(updatedRow, "SYNCED")
        } catch (_: Throwable) { }
        try {
            val amt = "₹" + "%,.0f".format(removedEvent.optDouble("amount", 0.0))
            val who = byName.ifBlank { byMobile }
            val msg = "$amt entry removed from a combined payment for ${row.s("name").ifBlank { row.s("mobile") }} " +
                "· Patient ID ${row.s("patientCode")} · Branch ${row.s("branch")} " +
                "· Payment date ${DateUtil.display(row.s("date"))} · Removed by $who. " +
                "Remaining total: ₹${"%,.0f".format(newAmount)}."
            BriefingRepository().post(ctx, "Payment entry removed", msg, "role", row.s("branch"), "master", byMobile)
        } catch (_: Throwable) { }
        return true
    }

    /** মিশ্র পেমেন্টের ভেতরের একটা নির্দিষ্ট এন্ট্রির Amount/Mode বদলানো
     *  (বাকিগুলো অক্ষত, মোট নতুন করে গণনা)। */
    fun editOneDailyEvent(row: JSONObject, eventId: String, newAmt: Double, newMode: String, byMobile: String, byName: String): Boolean {
        val ctx = context ?: return false
        // 🟢🔒 V618 — removeOneDailyEvent-এর হুবহু একই কারণ ও একই নিয়ম।
        val user = NativeSession.current(ctx)
        if (user?.role != "master" && !PaymentModel.withinFreeEditWindow(row.s("date"))) return false
        val id = row.optString("id")
        if (id.isBlank() || eventId.isBlank() || newAmt <= 0.0) return false
        val events = row.optJSONArray("dailyEvents") ?: return false
        val updated = org.json.JSONArray()
        var found = false
        for (i in 0 until events.length()) {
            val e = events.optJSONObject(i) ?: continue
            if (e.optString("eventId") == eventId) {
                found = true
                val e2 = JSONObject(e.toString())
                e2.put("amount", newAmt).put("mode", newMode)
                updated.put(e2)
            } else updated.put(e)
        }
        if (!found) return false
        var newAmount = 0.0; var newCash = 0.0; var newOnline = 0.0
        for (i in 0 until updated.length()) {
            val e = updated.getJSONObject(i)
            val amt = e.optDouble("amount", 0.0)
            newAmount += amt
            if (e.optString("mode").equals("CASH", true)) newCash += amt else newOnline += amt
        }
        val fields = JSONObject()
            .put("dailyEvents", updated)
            .put("amount", newAmount)
            .put("cashAmount", newCash)
            .put("onlineAmount", newOnline)
            .put("mode", if (newCash > 0 && newOnline > 0) "CASH" else if (newOnline > 0) "ONLINE" else "CASH")
            .put("updatedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
        val ok = try { SupabaseClient.updateById("payments", id, fields) } catch (_: Throwable) { false }
        if (!ok) return false
        try {
            val updatedRow = JSONObject(row.toString())
            val it = fields.keys(); while (it.hasNext()) { val k = it.next(); updatedRow.put(k, fields.get(k)) }
            LocalWorkflowStore(ctx).upsertPayment(updatedRow, "SYNCED")
        } catch (_: Throwable) { }
        try {
            val who = byName.ifBlank { byMobile }
            val msg = "One entry inside a combined payment was edited for ${row.s("name").ifBlank { row.s("mobile") }} " +
                "· Patient ID ${row.s("patientCode")} · Branch ${row.s("branch")} " +
                "· Payment date ${DateUtil.display(row.s("date"))} · Edited by $who. " +
                "New entry amount: ₹${"%,.0f".format(newAmt)} ($newMode). New total: ₹${"%,.0f".format(newAmount)}."
            BriefingRepository().post(ctx, "Payment entry edited", msg, "role", row.s("branch"), "master", byMobile)
        } catch (_: Throwable) { }
        return true
    }

    /** V471 — মিশ্র-পেমেন্ট breakdown পপ-আপ ভেতরের এন্ট্রি এডিট/ডিলিট করার পরে
     *  সবচেয়ে সাম্প্রতিক সারিটা আবার ফিরিয়ে আনার জন্য (তালিকা তাজা রাখতে)। */
    fun findPaymentById(id: String): JSONObject? {
        if (id.isBlank()) return null
        val rows = try { SupabaseClient.fetchListOrNull("payments", "id=eq.${java.net.URLEncoder.encode(id, "UTF-8")}", 1) } catch (_: Throwable) { null } ?: return null
        return if (rows.length() > 0) rows.optJSONObject(0) else null
    }

    private fun isTreatmentPaymentRow(payType: String, remarks: String): Boolean {
        val t = payType.lowercase()
        val r = remarks.lowercase()
        if (t == "registration" || t == "visitfee" || t == "visit_fee" || r.contains("visit fee") || r.contains("registration fee")) return false
        return t == "treatment" || t.isBlank()
    }

    /** V325: runs only after a treatment payment has reached cloud. It is
     * isolated from payment success/retry; any commission problem is only a
     * warning and can never put the payment back into Pending. */
    private fun activateRmpCommissionAfterCloud(patient: PatientBillInfo) {
        val ctx = context ?: return
        try {
            val commission = RmpCommissionActivation.checkAfterTreatmentPayment(ctx.applicationContext, patient.id)
            if (commission.state == RmpCommissionActivation.State.DEFAULT_MISSING) {
                val warningKey = "missing_${patient.id}_${PaymentModel.today()}"
                val warningPrefs = ctx.getSharedPreferences("rmp_commission_warnings", android.content.Context.MODE_PRIVATE)
                if (warningPrefs.getBoolean(warningKey, false)) return
                warningPrefs.edit().putBoolean(warningKey, true).apply()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(ctx.applicationContext,
                        "Payment saved. RMP commission is not set — please set it from Referral Income.",
                        android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (_: Throwable) { }
    }

    /** Saves a treatment payment: updates the patient's bill (only the first
     * time it's set) and adds the payment row, matching
     * saveTreatmentPayment() exactly. Returns true on success. */
    fun saveTreatmentPayment(
        patient: PatientBillInfo, enteredBill: Double, amount: Double, mode: String, remarks: String, staffMobile: String,
        // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow --
        // all three default to null, meaning every existing caller (normal
        // same-day payment) behaves EXACTLY as before. Only
        // approveBackdateRequest() (below) ever passes these.
        overrideDate: String? = null,
        backdateRequestedBy: String? = null,
        backdateApprovedBy: String? = null
    ): Boolean {
        // TK-REQUESTED (2026-07-25): a payment (Advance) can now be taken
        // even when NO Bill has been set yet for this patient -- Bill was
        // previously mandatory here (silently rejecting a real payment
        // with no clear reason why). effectiveBill can genuinely be 0 now;
        // only a real amount is still required.
        // 🔴🔴🔒 V663 (২৫.০৮.২০২৬, TK-কড়া-রিপোর্ট — কোচবিহার, "Bulan Roy"
        // রোগীর বিল সংশোধনের পরেও পুরনো বিলই দেখাচ্ছিল) — **আসল কারণ (কোড
        // ধরে চূড়ান্তভাবে ধরা পড়ল):** এই লাইন আগে ছিল
        // `if (patient.billLocked) patient.bill else enteredBill` — বিল
        // আগে থেকে "লক" থাকলে (billLocked = আগের বিল > 0), নতুন লেখা
        // বিলটাই **সম্পূর্ণ উপেক্ষা** হয়ে যেত, পুরনো `patient.bill`-ই
        // আবার বসত — **কিন্তু শুধু তখনই যখন বিল-সংশোধনের সাথে একটা
        // পেমেন্ট-এমাউন্টও একসাথে দেওয়া হতো** (শুধু-বিল-সংশোধন, কোনো
        // টাকা ছাড়া, PaymentActivity.kt-এর আলাদা billOnlyCorrection/
        // billChanged পথে ঠিকভাবে চলে যেত — তাই ওই ক্ষেত্রে কাজ করত)।
        // billLocked-এর আসল উদ্দেশ্য ছিল: ফাঁকা/শূন্য বিল যেন কখনো
        // দুর্ঘটনাক্রমে আগের আসল বিল মুছে না দেয় — ইচ্ছাকৃত সংশোধন আটকানো
        // নয়। **সমাধান:** নতুন লেখা বিল ফাঁকা/শূন্য (≤0) হলে তবেই পুরনো
        // লক করা বিল ব্যবহার হবে; সংখ্যা লেখা থাকলে (এমনকি আগেরটার থেকে
        // আলাদা হলেও) সেটাই এখন সবসময় গ্রহণ হয় — এটাই একটা ইচ্ছাকৃত
        // সংশোধন হিসেবে ধরা হয়।
        val effectiveBill = if (patient.billLocked && enteredBill <= 0.0) patient.bill else enteredBill
        if (effectiveBill > BILL_TYPO_GUARD_MAX) return false
        if (amount <= 0) return false
        // 🔒 TK'S LOCKED RULE (27.07.2026): "Bill · advance · any payment — যে
        // ব্রাঞ্চের স্টাফ তারাই করতে পারবে... সংশ্লিষ্ট ব্রাঞ্চের ডাক্তার করতে
        // পারবে, মাস্টার করতে পারবে।" Every Bill/Advance/2nd/3rd payment in the
        // app funnels through this one function (the Payment screen, the
        // Follow-up card's Advance, and the backdate approval all call it), so
        // the rule is enforced here as the final backstop -- the screens also
        // check first, purely so the staff sees a clear reason instead of a
        // silent failure. Master and the patient's own branch (staff or doctor)
        // are unaffected; a record with no branch at all is never blocked.
        context?.let { ctx ->
            if (!MoneyBranchGuard.canTakeMoney(ctx, patient.branch, patient.patientId)) return false
        }

        // TK-REQUESTED ADDITION (2026-07-16): label/paymentRow moved here
        // (out of the background Thread below) so the row can be cached
        // locally right away -- nextLabelFor() only reads the in-memory
        // treatmentPaymentCounts map (no network), so its value is identical
        // either way; this doesn't change what gets uploaded.
        // 🔒 খাতার সারি B52: ব্যাকডেট পেমেন্ট হলে **ওই তারিখের** নম্বর বসবে,
        // আজকের নয় — নইলে পুরনো দিনের টাকা ভুল নম্বর পেত।
        val payDay = (overrideDate ?: "").ifBlank { PaymentModel.today() }.take(10)
        val sameDayRepeat = treatmentPaymentDates[patient.id]?.contains(payDay) == true
        val label = nextLabelFor(patient.id, payDay)
        val paymentRow = PaymentModel.buildTreatmentPaymentRow(patient, amount, mode, remarks, label, staffMobile, overrideDate, backdateRequestedBy, backdateApprovedBy)
        // TK-REPORTED BUG FIX (2026-07-25): treatmentPaymentCounts was only
        // ever SET (in findPatientByMobile, when the Payment screen opens),
        // never incremented after an actual save. Taking a second payment
        // for the same patient in the same session -- without leaving and
        // reopening the screen -- reused the same stale count, so ordinal
        // labels (Advance/2nd/3rd...) could repeat or land out of order,
        // exactly what TK's photo-proof showed. Bumping it here means the
        // very next label in this same session is always one higher than
        // the one just used.
        treatmentPaymentCounts[patient.id] = (treatmentPaymentCounts[patient.id] ?: 0) + 1
        // 🔒 খাতার সারি B52: দিনটাকেও তালিকায় তুলে রাখা হয়, তাই **একই বসাতেই**
        // দ্বিতীয়বার টাকা নিলে নম্বর আর বাড়ে না — আগের সেই নম্বরটাই থাকে।
        // সঙ্গে আজকের মোটটাও বাড়ে, যাতে পরের বার সতর্কবার্তা ঠিক অঙ্ক দেখায়।
        treatmentPaymentDates[patient.id] =
            ((treatmentPaymentDates[patient.id] ?: emptyList()) + payDay).distinct().sorted()
        if (payDay == PaymentModel.today()) {
            treatmentPaidOnDate[patient.id] = (treatmentPaidOnDate[patient.id] ?: 0.0) + amount
        }

        // OWNER-LOCK: First Advance moves Visit -> Patient locally first.
        context?.let {
            val localStore = LocalWorkflowStore(it)
            localStore.promoteToTreatment(patient, effectiveBill, amount)
            // TK-REPORTED BUG FIX (2026-07-19): the "patients" row's own bill
            // field was never cached locally here at all -- only the
            // followups row was (via promoteToTreatment above). Follow-up's
            // Bill/Due join reads the "patients" table, so until the
            // separate cloud write below actually landed, Bill/Due kept
            // showing stale/₹0 even though the remark already said "Advance
            // Payment received". Now the local patients cache is updated
            // too, right away.
            // 🔴🔴🔒 V663 (২৫.০৮.২০২৬) — একই বাগের তৃতীয় অংশ (ফোনের নিজের
            // ক্যাশ): আগে `if (!patient.billLocked)` — locked থাকলে ফোনের
            // ক্যাশেও নতুন বিল বসত না, তাই ক্লাউড ফিক্সের পরেও ওই একই
            // ফোনে সাথে সাথে পুরনো বিল দেখাত (পরের রিফ্রেশ পর্যন্ত)। এখন
            // "বিল সত্যিই বদলেছে কিনা" শর্তে — বদলে থাকলে ক্যাশও সাথে
            // সাথে আপডেট হয়।
            if (effectiveBill != patient.bill) {
                localStore.upsertPatient(
                    JSONObject()
                        .put("id", patient.id)
                        .put("mobile", patient.mobile)
                        .put("name", patient.name)
                        .put("branch", patient.branch)
                        .put("patientId", patient.patientId)
                        .put("bill", effectiveBill)
                        .put("stage", "Treatment Running")
                        .put("updatedAt", isoNow())
                )
            }
            // TK-REPORTED BUG FIX (2026-07-16): a patient who reached Treatment
            // via a direct Advance payment (no Registration in between) kept
            // their old Inquiry-stage follow-up sitting "Active" forever, so
            // they still showed in the Enquiry tab even after being marked
            // Incomplete in Treatment. Registration already closes the Inquiry
            // row (closeInquiry); this does the same thing here, locally, so
            // the Enquiry tab updates immediately.
            localStore.closeInquiry(patient.mobile, patient.patientId)
            // TK-REQUESTED ADDITION (2026-07-16): cache the payment row
            // locally too, so Today's Collection can show it immediately.
            localStore.upsertPayment(paymentRow)
        }
        /* 🔁🔒 V839 (TK-নির্দেশ: *"Arrived নয় পেমেন্ট করলেও যেন কাজ হয়"*) —
           টাকা জমা হলে রোগী আবার CHECK-UP তালিকায় ফিরবেন।
           ⛔ নিজের ব্যাকগ্রাউন্ড থ্রেডে চলে, দিনে একবারের পাহারা ভিতরে;
              পেমেন্ট সেভের কাজ এক মুহূর্তও আটকায় না, ব্যর্থ হলেও কিছু ভাঙে না।
           ⛔ টাকার অঙ্ক · হিসাব · রসিদ — কিছুই ছোঁয়া হয়নি। */
        try { NextVisitQueue.reopenForToday(context, patient.mobile) } catch (_: Throwable) { }

        // TK-REPORTED BUG FIX (2026-07-16): this ALSO used to be a single
        // one-shot background attempt with no retry -- exactly the same
        // reliability problem already fixed for Enquiry/Registration (see
        // BottomNav.kt). A failed attempt here meant the Advance payment
        // (and the Visit->Patient promotion) stayed stuck on this one
        // device forever. Now the whole "what still needs to reach the
        // cloud" is queued FIRST, so BottomNav's retry (same as Enquiry/
        // Registration) can finish the job later if this first attempt
        // fails -- nothing about the instant local-save behavior changes.
        val pendingKey = queuePaymentPending(patient, effectiveBill, paymentRow, staffMobile, sameDayRepeat)

        // Cloud copy runs once in background. The staff workflow never waits for Supabase.
        Thread {
            try {
                val ok = pushPaymentToCloud(patient, effectiveBill, paymentRow, staffMobile, sameDayRepeat)
                if (ok) {
                    removePaymentPending(pendingKey)
                    // V325: every treatment-payment entry point (Payment,
                    // Follow-up Advance/Nth, approved backdate) reaches this
                    // same success point. Commission activation is isolated:
                    // payment is already in cloud, and any commission failure
                    // is swallowed so it can never damage payment/retry logic.
                    activateRmpCommissionAfterCloud(patient)
                }
            } catch (_: Throwable) { }
        }.start()
        return true
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow --
    // Staff side. Does NOT touch the "payments" table at all -- only writes
    // a pending row to the separate "payment_backdate_requests" table, so
    // Due/Paid/Reports/Today's Collection are completely unaffected until
    // Master approves (approveBackdateRequest below actually creates the
    // real payment).
    fun requestBackdatePayment(
        patient: PatientBillInfo, enteredBill: Double, amount: Double, mode: String,
        remarks: String, requestedDate: String, staffMobile: String, staffName: String,
        // 🔴🔒 V1442 (১৩.০৯.২০২৬, TK-নির্দেশ) — ডিফল্ট "treatment", পুরনো
        // ডাকার জায়গা (Add Treatment Payment) অপরিবর্তিত।
        payType: String = "treatment"
    ): Boolean {
        if (amount <= 0) return false
        val row = PaymentModel.buildBackdateRequestRow(patient, enteredBill, amount, mode, remarks, requestedDate, staffMobile, staffName, payType)
        return SupabaseClient.upsert("payment_backdate_requests", row)
    }

    // TK-REQUESTED ADDITION (2026-07-24): for the Dashboard bell -- cheap
    // count-only query (Master role only calls this), same pattern as
    // fetchCount() already used elsewhere.
    // 🔒 খাতার সারি B145 (30.07.2026): fetchCount এখন ব্যর্থ হলে -1 ("জানি না")
    // ফেরত দেয়। এই সংখ্যাটা সোজা ঘন্টার badge-এ বসে, তাই এখানে -1 কে 0 ধরা হয়
    // — নইলে পর্দায় উল্টোপাল্টা সংখ্যা দেখাত। আচরণ আগের মতোই থাকল।
    fun fetchPendingBackdateCount(): Int =
        SupabaseClient.fetchCount("payment_backdate_requests", "status=eq.pending")
            .coerceAtLeast(0)

    // TK-REQUESTED ADDITION (2026-07-24): for the Briefing screen's
    // Master-only "Pending Backdate Payment Requests" section.
    fun fetchPendingBackdateRequests(): List<BackdateRequest> {
        val rows = SupabaseClient.fetchList("payment_backdate_requests", "status=eq.pending", 200)
        val list = mutableListOf<BackdateRequest>()
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            list.add(
                BackdateRequest(
                    id = r.s("id"), patientRowId = r.s("patientRowId"), patientCode = r.s("patientCode"),
                    mobile = r.s("mobile"), name = r.s("name"), branch = r.s("branch"),
                    billAmount = r.optDouble("billAmount", 0.0), amount = r.optDouble("amount", 0.0),
                    mode = r.s("mode"), remarks = r.s("remarks"), requestedDate = r.s("requestedDate"),
                    requestedBy = r.s("requestedBy"), requestedByName = r.s("requestedByName"),
                    requestedAt = r.s("requestedAt"), status = r.s("status"),
                    // 🔴🔒 V1442 — পুরনো সারিতে ঘরটা নেই বলে খালি আসতে পারে;
                    // খালি হলে "treatment" ধরাই নিরাপদ (আগের সব সারির আচরণ)।
                    payType = r.s("payType").ifBlank { "treatment" }
                )
            )
        }
        return list
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow --
    // Master side. Re-fetches the patient's CURRENT bill/paid/lock state
    // fresh (never trusts anything stale from when the request was made,
    // in case the bill changed meanwhile), then reuses saveTreatmentPayment
    // itself (same local-cache + retry-queue reliability as every other
    // payment) with the requested date + full audit trail. Only marks the
    // request "approved" if the actual payment save succeeded.
    fun approveBackdateRequest(request: BackdateRequest, masterMobile: String): Boolean {
        // BUG CAUGHT DURING SELF-REVIEW (2026-07-24): findPatientByMobile()
        // expects DIGITS-ONLY (it prepends "+91" itself internally) --
        // request.mobile is stored already-normalized ("+91XXXXXXXXXX",
        // copied from PatientBillInfo.mobile). Passing that straight
        // through would double-prefix it ("+91+91XXXXXXXXXX") and always
        // fail to find the patient. Strip to digits-only first.
        val digitsOnly = request.mobile.filter { it.isDigit() }.takeLast(10)
        val patient = findPatientByMobile(digitsOnly) ?: PatientBillInfo(
            id = request.patientRowId, name = request.name, mobile = request.mobile,
            branch = request.branch, patientId = request.patientCode, bill = request.billAmount,
            paid = 0.0, billLocked = false
        )
        // 🔴🔒 V1442 (১৩.০৯.২০২৬, TK-নির্দেশ) — request.payType=="refund" হলে
        // এটা Treatment Payment নয়, ব্যাকডেট করা Refund-এর অনুরোধ। Master
        // Approve চাপাটাই এখানে refund-approval, তাই সরাসরি saveBackdatedRefund
        // (সবসময় APPROVED) — বাকি সব ডাকার জায়গা (treatment) অপরিবর্তিত।
        val ok = if (request.payType == "refund") {
            saveBackdatedRefund(
                patient, request.amount, request.mode, request.remarks,
                forDate = request.requestedDate,
                requestedBy = request.requestedBy,
                approvedBy = masterMobile,
                staffMobile = request.requestedBy
            ).success
        } else {
            saveTreatmentPayment(
                patient, request.billAmount, request.amount, request.mode, request.remarks, request.requestedBy,
                overrideDate = request.requestedDate,
                backdateRequestedBy = request.requestedBy,
                backdateApprovedBy = masterMobile
            )
        }
        if (ok) {
            val fields = JSONObject()
                .put("status", "approved")
                .put("approvedBy", masterMobile)
                .put("approvedAt", isoNow())
                .put("updatedAt", isoNow())
            SupabaseClient.updateById("payment_backdate_requests", request.id, fields)
        }
        return ok
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow --
    // Master rejects a pending request. No payment is ever created; just
    // marks the request rejected so it stops showing as pending.
    fun rejectBackdateRequest(requestId: String, masterMobile: String): Boolean {
        val fields = JSONObject()
            .put("status", "rejected")
            .put("approvedBy", masterMobile)
            .put("approvedAt", isoNow())
            .put("updatedAt", isoNow())
        return SupabaseClient.updateById("payment_backdate_requests", requestId, fields)
    }

    // ============================================================
    // V216 (§13, 31.07.2026) — Refund / টাকা ফেরত
    // ⛔ পুরোনো payment row কখনো edit/delete হয় না — refund সবসময় আলাদা নতুন
    //    row ("payments" টেবিলেই, payType="refund")। শুধু approved refund
    //    collection/paid total থেকে বিয়োগ হয় (উপরের হিসাব-লুপে করা হয়েছে)।
    // ⛔ নতুন কোনো টেবিল লাগে না; approval refund row-এর refundApprovalStatus-এই।
    // ============================================================

    /** V217 (§B216, 31.07.2026): জমা টাকার চেয়ে বেশি Refund যেন কিছুতেই না
     *  যায় — এই একটাই ফাংশন ব্যবহার হয় saveRefund-এর ভিতরে (UI-এর আলাদা
     *  চেক থাকলেও, ফাংশনের নিজের পাহারা আসল — কোনো ভবিষ্যতের পর্দা এড়িয়ে
     *  গেলেও এখানে আটকাবে)। ইতিমধ্যে pending (এখনো Master অনুমোদন করেননি)
     *  refund-ও যোগ করে ধরা হয় — নইলে স্টাফ একই টাকার উপর দুটো আলাদা
     *  pending request পাঠিয়ে দুটোই approve হয়ে গেলে জমার চেয়ে বেশি
     *  বেরিয়ে যেতে পারত।
     *  ⛔ ব্যর্থ হলে (নেট সমস্যা) রিফান্ড আটকানো হয় না — খালি তালিকা মানেই
     *     নিরাপদ ধরে নেওয়া হয় (আগের pending-sum পাওয়া গেল না মানেই শূন্য)।
     *  🔴 V217 self-audit fix (তৃতীয় দফা, আরও গভীর যাচাই): প্যারামিটারের নাম
     *     `patientId` হলেও এখানে **`patient.id` (আসল DB row-id) দিতে হবে,
     *     `patient.patientId` (মানুষ-পড়া কোড, যেমন KNE-31072026-001) নয়** —
     *     কারণ `buildRefundRow()`-এ payments টেবিলের "patientId" কলামে
     *     `patient.id`-ই লেখা হয় (মানুষ-পড়া কোড যায় আলাদা "patientCode"
     *     কলামে)। ভুল করে `patient.patientId` পাঠানো হলে এই ফাংশন কখনো কোনো
     *     সারি খুঁজেই পেত না (চুপচাপ সবসময় ০), তাই pending-stacking পাহারাটা
     *     আসলে কাজই করত না — এই বাগটা এখানেই ধরা পড়ে ঠিক করা হয়েছে। */
    private fun pendingRefundSumForPatient(patientId: String, excludeId: String = ""): Double {
        if (patientId.isBlank()) return 0.0
        return try {
            val rows = SupabaseClient.fetchList(
                "payments",
                "patientId=eq.$patientId&payType=eq.refund&refundApprovalStatus=eq.pending",
                50, select = "id,amount"
            )
            var sum = 0.0
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                // V219 (§1): একই Refund আবার চাপলে (deterministic id) সেটাকে দুবার
                // গোনা হয় না — নইলে idempotent retry ভুল করে "refundable-এর বেশি"
                // বলে আটকে যেতে পারত। ভিন্ন Refund হলে id আলাদা, তাই ঠিকই গোনা হয়।
                if (excludeId.isNotBlank() && r.optString("id") == excludeId) continue
                sum += r.optDouble("amount", 0.0)
            }
            sum
        } catch (_: Throwable) { 0.0 }
    }

    // 🔒🔒 V221 (§3, 31.07.2026): Refund nonce — শুধু memory-তে নয়, নিরাপদে persist।
    // আগে nonce ছিল ফর্ম-খোলার memory-তে (PaymentActivity)। App crash/restart-এ
    // হারিয়ে যেত, তখন একই অসম্পূর্ণ Refund আবার করলে **আলাদা id** হয়ে Duplicate
    // Refund হতে পারত। এখন nonce লোকালি (SharedPreferences) draft-key ধরে রাখা হয় —
    // draft-key = রোগী + টাকা + কারণ + আজকের তারিখ (refundIdFor-এর ইনপুটের সঙ্গে মিল)।
    //  • একই অসম্পূর্ণ Refund আবার (crash-এর পরেও) → একই persist-করা nonce → একই id
    //    → cloud upsert পুরোনো row overwrite করে, নতুন row হয় না। Duplicate বন্ধ।
    //  • cloud-এ **সত্যিই বসলে (ok)** তবেই nonce মুছে ফেলা হয় — তাই আগেরটা confirm
    //    হওয়ার পরে একই দিনে করা **বৈধ আলাদা Refund** নতুন nonce → আলাদা id পায়
    //    (একই দিনে দুটি বৈধ আলাদা Refund-এর বর্তমান সুবিধা অক্ষত)।
    // ⛔ পুরোপুরি লোকাল store — কোনো নতুন cloud read/write নয় (Free-plan অপরিবর্তিত)।
    // ⛔ Refund total/approval/Visit Fee/branch/payment হিসাব কিছুই বদলায়নি —
    //    শুধু id-র ভিতরের nonce কোথা থেকে আসে সেটুকু (memory → persist)।
    private val REFUND_NONCE_PREF = "refund_nonce_store"

    private fun refundNonceKey(patient: PatientBillInfo, amount: Double, reason: String): String {
        val mob = patient.mobile.filter { it.isDigit() }.takeLast(10)
        val amtCents = Math.round(amount * 100)
        // 🔒 V222 (§2): key-এর সামনে `patient.id` — এক মোবাইলে দুই আলাদা রোগীর
        // draft-key কখনো মেলে না, তাই তাদের nonce ও Refund id আলাদা থাকে।
        return "${patient.id}|$mob|$amtCents|${reason.trim().lowercase()}|${PaymentModel.today()}"
    }

    private fun getOrCreateRefundNonce(ctx: Context, key: String): String {
        return try {
            val p = ctx.getSharedPreferences(REFUND_NONCE_PREF, Context.MODE_PRIVATE)
            val existing = p.getString(key, "") ?: ""
            if (existing.isNotBlank()) existing
            else {
                val n = java.util.UUID.randomUUID().toString().replace("-", "").take(10)
                // commit() — cloud চেষ্টার আগেই ডিস্কে, তাই crash হলেও nonce টেকে।
                p.edit().putString(key, n).commit()
                n
            }
        } catch (_: Throwable) { "" }
    }

    /** 🔵🔒 V789 — জমা থাকা nonce **শুধু পড়ে** (না থাকলে ফাঁকা, নতুন বানায় না)।
     *  ⛔ `getOrCreateRefundNonce()` এক অক্ষরও বদলায়নি — সেভের পথ অটুট। */
    private fun peekRefundNonce(ctx: Context, key: String): String = try {
        ctx.getSharedPreferences(REFUND_NONCE_PREF, Context.MODE_PRIVATE).getString(key, "") ?: ""
    } catch (_: Throwable) { "" }

    private fun clearRefundNonce(ctx: Context, key: String) {
        try {
            val p = ctx.getSharedPreferences(REFUND_NONCE_PREF, Context.MODE_PRIVATE)
            if (p.contains(key)) p.edit().remove(key).apply()
        } catch (_: Throwable) { }
    }

    /** TK-নির্দেশ (02.08.2026, চূড়ান্ত): Refund/টাকা-ফেরত সংক্রান্ত যেকোনো
     *  ব্যাপারে (আজকের পেমেন্ট, বেশি বিল হয়ে যাওয়া, স্টাফের ভুল সংশোধন —
     *  টাকা যেদিনই জমা হোক না কেন) Staff নিজেই ঠিক করতে পারবেন, যতক্ষণ না
     *  সেই ব্রাঞ্চের আজকের Chamber বন্ধ (Close Chamber) হচ্ছে। Chamber বন্ধ
     *  হয়ে গেলে — তখন থেকে Master-এর অনুমতি ছাড়া Refund হবে না। কোনো
     *  সময়-ভিত্তিক ছাড় নেই (TK স্পষ্ট করেছেন — চেম্বার তাড়াতাড়ি বন্ধ হয়ে
     *  গেলেও, সেটা ভুল না হলে, আর খুলবে না; নতুন কোনো "Reopen" বোতামও
     *  বানানো হয়নি, TK-এর নির্দেশমতোই)। বিদ্যমান `ChamberCloseRepository`-ই
     *  একমাত্র উৎস (ফোনে জানা থাকলে নেট ছাড়াই, নইলে ক্লাউড চেক) — নতুন কোনো
     *  DB টেবিল/কলাম লাগেনি। */
    /** 🔴 নিরাপত্তা-সংশোধন (TK-নির্দেশ, 02.08.2026): অফলাইনে/নেট-সমস্যায় Chamber
     *  সত্যিই বন্ধ কিনা যাচাই করা না গেলে (আর এই ফোনেই বন্ধ করা হয়নি) — টাকার
     *  নিরাপত্তার স্বার্থে এখন **"বন্ধ" ধরে Master-এর অনুমতি চাওয়া হয়**, "খোলা"
     *  ধরে ছেড়ে দেওয়া হয় না (fail-safe)। আগের সংস্করণ `ChamberCloseRepository.
     *  isClosed()` ব্যবহার করত, যেটা নেট-ব্যর্থতায় নিজেই `false` (মানে "বন্ধ
     *  নয়") ফেরত দেয় — সেটা reminder-এর মতো জায়গায় ঠিক আচরণ, কিন্তু টাকার
     *  অনুমতির জন্য বিপজ্জনক। তাই এখানে সরাসরি ক্লাউড-কল করা হয়, যাতে
     *  "যাচাই করাই গেল না" (null) আর "সত্যিই বন্ধ হয়নি" (খালি তালিকা) — এই
     *  দুটো আলাদা করা যায়। ⛔ `ChamberCloseRepository.isClosed()`-এর নিজের
     *  আচরণ একটুও বদলানো হয়নি (প্রজেক্টের অন্য অনেক জায়গায় ব্যবহার হয় বলে
     *  সেটা ছোঁয়া ঝুঁকিপূর্ণ) — শুধু এখানে, Refund-অনুমতির জন্য, আলাদা করে
     *  fail-safe চেক লেখা হয়েছে। */
    fun chamberOpenToday(branch: String): Boolean {
        if (branch.isBlank()) return true // ব্রাঞ্চ অজানা হলে আটকানো হয় না (আগের আচরণের মতোই নিরাপদ ডিফল্ট)
        val today = PaymentModel.today()
        // ১. এই ফোনেই বন্ধ করা হয়ে থাকলে — নেট ছাড়াই নিশ্চিত বন্ধ, সাথে সাথে Master চাওয়া হয়।
        if (ChamberCloseRepository.isClosedLocally(context, branch, today)) return false
        // ২. ক্লাউডে সত্যিই যাচাই — null মানে যাচাই করাই যায়নি (অফলাইন/এরর): তখন
        //    নিরাপদ দিকেই ভুল করে "বন্ধ" ধরা হয় (Master চাওয়া হবে, auto-approve নয়)।
        //    খালি তালিকা (রো নেই) মানে সত্যিই কেউ বন্ধ করেনি — তখনই "খোলা"।
        val id = ChamberCloseRepository.idOf(branch, today)
        val key = try { java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20") } catch (_: Throwable) { id }
        val rows = SupabaseClient.fetchListOrNull("chamber_close", "id=eq.$key", 1) ?: return false
        return rows.length() == 0
    }

    /** আজ (স্থানীয় তারিখ) এই রোগী থেকে সত্যিই কত টাকা জমা পড়েছে (Refund
     *  বাদে) — সেটাই স্টাফের হাতে আজকের নগদে থাকতে পারে।
     *  🔴🔴🔒 খাতার সারি B445 (TK-নির্দেশ, 05.08.2026, উদাহরণ দিয়ে বুঝিয়ে —
     *  "গতকাল ৫০০০ টাকা এডভান্স হয়েছিল, সেটা তো আমার পকেটে চলে এসেছে,
     *  স্টাফ আজকে সেই টাকা ফেরত দেবে কোথা থেকে?")। এই ফাংশনটা ঠিক তার
     *  জন্যই — আজ এই রোগী থেকে সত্যিই কত এসেছে, সেটাই বের করে। */
    private fun paidTodayForPatient(patientRowId: String): Double {
        if (patientRowId.isBlank()) return 0.0
        return try {
            val today = PaymentModel.today()
            val rows = SupabaseClient.fetchList(
                "payments",
                "patientId=eq.$patientRowId&date=eq.$today&payType=neq.refund",
                200, select = "amount"
            )
            var sum = 0.0
            for (i in 0 until rows.length()) sum += (rows.optJSONObject(i)?.optDouble("amount", 0.0) ?: 0.0)
            sum
        } catch (_: Throwable) { 0.0 }
    }

    /** আজ এই রোগীর ইতিমধ্যে approved হওয়া Refund-এর যোগফল — আজকের জমা
     *  থেকে ইতিমধ্যে কতটা "খরচ" হয়ে গেছে তা বাদ দেওয়ার জন্য (নইলে একই
     *  দিনের ৫০০০-কে দুইবার-তিনবার রিফান্ডযোগ্য ধরে ফেলত)। */
    private fun refundedTodayForPatient(patientRowId: String, excludeId: String): Double {
        if (patientRowId.isBlank()) return 0.0
        return try {
            val today = PaymentModel.today()
            val rows = SupabaseClient.fetchList(
                "payments",
                "patientId=eq.$patientRowId&payType=eq.refund&refundApprovalStatus=eq.approved&date=eq.$today",
                200, select = "id,amount"
            )
            var sum = 0.0
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                if (excludeId.isNotBlank() && r.optString("id") == excludeId) continue
                sum += r.optDouble("amount", 0.0)
            }
            sum
        } catch (_: Throwable) { 0.0 }
    }

    /* ═══════════════════════════════════════════════════════════════════
       🟡🔒 V786 (২৮.০৮.২০২৬, TK-রিপোর্ট + ফটো-প্রমাণ) —
       **"Refund ২ বার হয়ে গেল, আটকালো না কেন? ডুপ্লিকেট pop up কেন এলো না?"**

       TK-এর ছবি (KABITA BANU · COB-26082026-001): ২৭.০৮.২০২৬ রাত ৯.২৫ ও
       ৯.২৬ — **দুটো হুবহু ₹4,001 CASH Refund**, এক মিনিটের ব্যবধানে।

       ─── কেন আটকায়নি (কোড ধরে যাচাই, আন্দাজ নয়) ─────────────────────────
       Refund-এ তিনটে পাহারা ছিল, কিন্তু **ডুপ্লিকেট ধরার পাহারা ছিল না**:
         ১. `refundSaving` — শুধু **একই ফর্মে** পরপর দুই চাপ আটকায়
         ২. `nonce` — ইচ্ছে করেই **ফর্ম নতুন করে খুললে নতুন**, যাতে একই দিনে
            একই টাকার **বৈধ** দ্বিতীয় ফেরত দেওয়া যায়
         ৩. `maxRefundable` — জমার চেয়ে বেশি ফেরত আটকায়। এখানে জমা ছিল
            ₹9,501 (₹5,500 + ₹4,001), তাই ₹4,001 × ২ = ₹8,002 **সীমার
            ভিতরেই** ছিল ⇒ অঙ্কের দিক থেকে কিছুই ভুল ছিল না, তাই আটকায়নি।
       ⇒ ফর্মটা দ্বিতীয়বার খোলা হয়েছিল, তাই ১ ও ২ কাজে লাগেনি; আর ৩ তো
         এটাকে ভুলই মনে করেনি।

       ─── এখন কী হয় ─────────────────────────────────────────────────────
       V708-এ Prescription/Investigation-এ TK যে নিয়ম পাশ করেছিলেন, টাকার
       পর্দাতেও ঠিক সেটাই — **Cancel = না · OK = জেনেশুনে তবুও**।
       আজকের দিনে এই রোগীর **একই পরিমাণ** ফেরত আগে থেকে থাকলে সতর্কবার্তা।

       ⛔ টাকার একটাও নিয়ম বদলায়নি — এটা শুধু **জিজ্ঞাসা** করে।
       ⛔ Egress: একটাই ছোট query (`select=id,amount,time,reason`), আর সেটাও
          শুধু Refund বোতাম চাপলে — তালিকা খোলার সময় নয়।
       ═══════════════════════════════════════════════════════════════════ */
    /* 🔴🔒 V1152 (০৬.০৯.২০২৬, TK-নির্দেশ) — ফেরতের সতর্কবার্তাতেও এখন
       **ধরন (CASH/ONLINE)** মেলানো হয়; আগে শুধু অঙ্ক দেখা হত, তাই ১০০০ ক্যাশের
       পরে ১০০০ অনলাইন ফেরত দিলেও অকারণে সতর্কতা উঠত।
       ⛔ `mode` ফাঁকা পাঠালে আগের মতোই শুধু অঙ্ক ধরে মেলে। */
    fun todaysRefundLike(
        patient: PatientBillInfo, amount: Double, reason: String = "", byMobile: String = "",
        mode: String = ""
    ): org.json.JSONObject? {
        if (patient.id.isBlank() || amount <= 0.0) return null
        /* 🔵🔒 V789 — **কম্পিউটারের সঙ্গে মিলিয়ে নেওয়া।** ওয়েবের
           `wlv1TodaysSameRefund()` চলতি এই Refund-এর **নিজের সারিটা** বাদ দেয়
           (`selfId`), ফোনে সেটা বাদ পড়েছিল। ফল: প্রথম চেষ্টা অর্ধেক সফল হয়ে
           নেট কেটে গেলে, আবার চাপলে নিজের সারিটাই "ডুপ্লিকেট" বলে দেখাত।
           ⛔ nonce এখানে **শুধু পড়া হয়, তৈরি করা হয় না** (`peekRefundNonce`) —
              তাই এই যাচাই কোনো কিছু বদলায় না, শুধু দেখে। */
        val selfId = try {
            val n = context?.let { peekRefundNonce(it, refundNonceKey(patient, amount, reason)) }.orEmpty()
            if (n.isBlank()) "" else PaymentModel.refundIdFor(patient, amount, reason, byMobile, n)
        } catch (_: Throwable) { "" }
        return try {
            val today = PaymentModel.today()
            val rows = SupabaseClient.fetchList(
                "payments",
                "patientId=eq.${patient.id}&payType=eq.refund&date=eq.$today",
                200, select = "id,amount,time,reason,mode,refundApprovalStatus"
            )
            var hit: org.json.JSONObject? = null
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                // বাতিল/না-মঞ্জুর সারি ডুপ্লিকেট নয়
                val st = r.optString("refundApprovalStatus", "")
                if (st.equals("rejected", true) || st.equals("cancelled", true)) continue
                if (selfId.isNotBlank() && r.optString("id") == selfId) continue   // 🔵 V789 — নিজের সারি নয়
                val wantMode = PaymentModel.normalizeMode(mode).trim()
                val modeOk = wantMode.isBlank() ||
                    PaymentModel.normalizeMode(r.optString("mode", "")).equals(wantMode, ignoreCase = true)
                if (kotlin.math.abs(r.optDouble("amount", 0.0) - amount) <= 0.5 && modeOk) { hit = r; break }
            }
            hit
        } catch (_: Throwable) { null }   // নেট খারাপ হলে চুপচাপ সরে দাঁড়ায় — সৎ ফেরত আটকানো যাবে না
    }

    /**
     * 🔴🔒 V1443 (১৩.০৯.২০২৬, TK-নির্দেশ — তালিকা ৫৬৭) — TK: *"টাকা রিটার্ন
     * যদি একবার হয়ে যায় সমপরিমাণ টাকা রিটার্ন করতে গেলে, অথবা সেই ব্যক্তির
     * টাকা রিটার্ন হয়ে গেছে পরবর্তীতে আবার যদি কিছু রিটার্ন করতে চায়, সেই
     * ক্ষেত্রে ওয়ার্নিং অবশ্যই দিবে।"*
     *
     * `todaysRefundLike()`-এর বড় বোন — তারিখ ও অঙ্ক দুটোতেই আটকে না থেকে,
     * এই রোগীর **যেকোনো দিনের** কোনো আগের Refund (rejected/cancelled বাদে)
     * থাকলেই ধরে। সমপরিমাণের সারি থাকলে সেটাই আগে ফেরত (বার্তায় নির্দিষ্ট
     * করে বলা যায়), নইলে সবচেয়ে সাম্প্রতিক সারি।
     * ⛔ শুধু **দেখে** — কিছুই আটকায় না, ঠিক আগের নিয়মেই "Cancel = না ·
     *    OK = তবুও"। নেট খারাপ হলে `null` — সৎ ফেরত কখনো আটকায় না।
     */
    fun priorRefundAnyDate(patient: PatientBillInfo, amount: Double = 0.0, excludeId: String = ""): org.json.JSONObject? {
        if (patient.id.isBlank()) return null
        return try {
            val rows = SupabaseClient.fetchList(
                "payments",
                "patientId=eq.${patient.id}&payType=eq.refund",
                200, select = "id,amount,date,reason,mode,refundApprovalStatus"
            )
            var sameAmtHit: org.json.JSONObject? = null
            var anyHit: org.json.JSONObject? = null
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val st = r.optString("refundApprovalStatus", "")
                if (st.equals("rejected", true) || st.equals("cancelled", true)) continue
                if (excludeId.isNotBlank() && r.optString("id") == excludeId) continue
                if (anyHit == null) anyHit = r
                if (amount > 0.0 && sameAmtHit == null && kotlin.math.abs(r.optDouble("amount", 0.0) - amount) <= 0.5) sameAmtHit = r
            }
            sameAmtHit ?: anyHit
        } catch (_: Throwable) { null }
    }

    /** V1443 — ফর্ম থেকে ডাকার সুবিধার্থে: `todaysRefundLike()`-এর মতোই
     *  নিজের চলতি ফর্মের (nonce-ধরা) সারিটা বাদ দিয়ে `priorRefundAnyDate()`
     *  ডাকে, যাতে retry নিজেকেই "আগের রিফান্ড" ভেবে সতর্ক না করে। */
    fun priorRefundLikeForForm(patient: PatientBillInfo, amount: Double, reason: String, byMobile: String): org.json.JSONObject? {
        val selfId = try {
            val n = context?.let { peekRefundNonce(it, refundNonceKey(patient, amount, reason)) }.orEmpty()
            if (n.isBlank()) "" else PaymentModel.refundIdFor(patient, amount, reason, byMobile, n)
        } catch (_: Throwable) { "" }
        return priorRefundAnyDate(patient, amount, selfId)
    }

    /** Refund সেভ। Master → সরাসরি approved। Staff → **শুধু তখনই** সরাসরি
     *  approved, যখন (ক) আজকের Chamber এখনো বন্ধ হয়নি, **আর** (খ) এই
     *  রোগী থেকে আজকেই এই রিফান্ডের সমান বা বেশি টাকা জমা পড়েছে (তাই
     *  স্টাফের হাতে সত্যিই সেই নগদ আছে)। ⛔ **সংশোধন — খাতার সারি B445
     *  (TK-নির্দেশ, 05.08.2026)।** আগে (02.08.2026) শুধু "চেম্বার খোলা
     *  আছে কিনা" দেখা হতো — টাকা কোন দিন জমা পড়েছে তা দেখা হতো না। TK
     *  উদাহরণ দিয়ে ধরিয়ে দিয়েছেন: গতকালের জমা ইতিমধ্যে Master-এর কাছে
     *  চলে গেছে (সন্ধ্যার হিসাব-বুঝিয়ে-দেওয়ায়), তাই আজ সেই টাকা স্টাফের
     *  হাতে নেই — গতকালের টাকা আজ রিফান্ড করতে হলে Master-এর অনুমতি
     *  লাগবেই, চেম্বার খোলা থাকলেও। শুধু **আজকের জমা, আজকেই ফেরত** —
     *  তখনই স্টাফ নিজে করতে পারবেন (সন্ধ্যায় হিসাব বোঝানোর সময় বাদ দিয়ে
     *  দেবেন)। ⛔ Master-এর নিজের Refund সবসময় সরাসরি, অপরিবর্তিত। */
    fun saveRefund(patient: PatientBillInfo, amount: Double, mode: String, reason: String, user: NativeUser, nonce: String = ""): RefundResult {
        if (amount <= 0) return RefundResult(false, "Enter a valid refund amount")
        context?.let { ctx ->
            if (!MoneyBranchGuard.canTakeMoney(ctx, patient.branch, patient.patientId)) return RefundResult(false, "Not allowed for this branch")
        }
        // 🔒 V217: জমার চেয়ে বেশি Refund সম্পূর্ণ বন্ধ। patient.paid ইতিমধ্যে
        // আগের approved refund বাদ দিয়েই আসে (PaymentRepository-এর paid-হিসাব
        // লুপ, §13) — তাই এখানে শুধু চলতি pending-ও যোগ করে ধরলেই যথেষ্ট।
        // V219 (§1): এই Refund-এর deterministic id বের করে pending-সমষ্টি থেকে বাদ
        // দেওয়া হয়, যাতে একই Refund আবার চাপলে (retry) ভুল করে না আটকায়।
        // V220 (§4): nonce সহ id — একই ফর্মের retry একই id (double নয়), নতুন ফর্মের
        // বৈধ দ্বিতীয় Refund আলাদা id। pending-সমষ্টি থেকে এই একই id বাদ (retry না আটকায়)।
        // 🔒 V221 (§3): persist-করা nonce — crash/restart-এও একই অসম্পূর্ণ Refund
        // একই id-তে retry হয় (Duplicate নয়)। context না থাকলে passed nonce (backward)।
        val nonceKey = refundNonceKey(patient, amount, reason)
        val effNonce = context?.let { getOrCreateRefundNonce(it, nonceKey) } ?: nonce
        val refundId = PaymentModel.refundIdFor(patient, amount, reason, user.mobile, effNonce)
        /* 🔴🔒 V1373 (১২.০৯.২০২৬, তালিকা ৪৬২-ঘ — TK-নির্দেশে গভীরে যাচাই করে):
           Refund সেভের আগে এই ফাংশন ৪-৫টা আলাদা ক্লাউড-পড়া একটার-পর-একটা
           (sequential) করত — অথচ কেউ কারো ফলাফলের উপর নির্ভর করে না (প্রতিটাই
           শুধু patient/refundId নিয়ে কাজ করে)। এখন প্রথম দুটো (pendingRefundSumFor
           Patient + findPatientByMobile) **একসাথে** আনা হয় — ঠিক
           ChamberAttendanceRepository.loadBoard()-এর একই TK-অনুমোদিত ধরন
           (async(Dispatchers.IO) + একসাথে অপেক্ষা)। ⛔ কোন সংখ্যা কীভাবে গোনা
           হয়, কোনটা কার উপর জেতে (V509/V217/B445) — এক অক্ষরও বদলায়নি, শুধু
           পড়াগুলো একসাথে চলে বলে মোট অপেক্ষার সময় কমে। */
        val (alreadyPending, liveByMobile) = runBlocking {
            val pendingDef = async(Dispatchers.IO) { pendingRefundSumForPatient(patient.id, refundId) }
            val liveDef = async(Dispatchers.IO) {
                try { findPatientByMobile(patient.mobile, patient.branch) } catch (_: Throwable) { null }
            }
            pendingDef.await() to liveDef.await()
        }
        // 🔴🔴 V509 (২১.০৮.২০২৬, TK-এর স্পষ্ট সিদ্ধান্ত — হুবহু: *"হ্যাঁ, সবাই
        // পারবে — Visit Fee-ও ফেরতের সীমায় ধরা হবে"*)। আগে এখানে শুধু
        // `patient.paid` ছিল, আর Visit Fee সেই হিসাবের বাইরে থাকত — তাই যে রোগী
        // শুধু Visit Fee দিয়েছেন (চিকিৎসার টাকা এখনো দেননি) তাঁর ফেরতযোগ্য ₹0
        // দেখাত, ফেরত দেওয়াই যেত না। এখন `refundableTotal` = চিকিৎসার জমা +
        // Visit Fee। ⛔ Bill/Due-র হিসাব আগের মতোই শুধু `paid` দেখে — বদলায়নি।
        //
        // 🔴🔴🔴 V509 নিজের যাচাইয়ে ধরা পড়া **দ্বিতীয় টাকা-বাগ** (২১.০৮.২০২৬) —
        // **একই টাকা দুবার ফেরত।**
        // এই ফাংশন এতদিন ডাকার সময় পাঠানো `patient`-এর সংখ্যাটাই বিশ্বাস করত।
        // কিন্তু একবার Refund সফল হওয়ার পরেও **পিছনের রোগী-কার্ডটা পুরোনো
        // সংখ্যা নিয়েই খোলা থাকে** (কার্ডটা নতুন করে আঁকা হয় না)। তাই DUE-তে
        // আবার চাপ দিলে পুরোনো "ফেরতযোগ্য ৫০০" নিয়েই ফর্ম খুলত, আর মাস্টারের
        // ক্ষেত্রে সেটা **সঙ্গে সঙ্গে অনুমোদিত** হয়ে যেত — ৫০০ টাকার জমায়
        // ১০০০ টাকা বেরিয়ে যেত। (`alreadyPending` এটা ধরতে পারে না — সে শুধু
        // *pending* ফেরত গোনে, আর প্রথমটা ততক্ষণে *approved*।)
        // ⚠️ ফাঁকটা আগে থেকেই ছিল, কিন্তু **V509-এই সেটা শুধু-Visit-Fee-দেওয়া
        //    রোগীদের নাগালে চলে আসত** (আগে তাঁদের ফর্মটাই খুলত না)। তাই এখানেই
        //    বন্ধ করা হলো।
        // ⇒ সেভ করার ঠিক আগে **ক্লাউড থেকে তাজা হিসাব** এনে দুটোর মধ্যে
        //   **ছোটটাই** নেওয়া হয় — কখনো বেশি নয়।
        // ⛔ নেট খারাপ / হিসাব আনা গেল না (`paymentsUnverified`) হলে আগের মতোই
        //   পাঠানো সংখ্যাই ব্যবহার হয় — নইলে সৎ রিফান্ডও আটকে যেত।
        val liveRefundable = try {
            val live = liveByMobile
            if (live != null && live.id == patient.id && !live.paymentsUnverified)
                minOf(live.refundableTotal, patient.refundableTotal)
            else patient.refundableTotal
        } catch (_: Throwable) { patient.refundableTotal }
        val maxRefundable = (liveRefundable - alreadyPending).coerceAtLeast(0.0)
        if (amount > maxRefundable + 0.5) { // 0.5 = paise-স্তরের রাউন্ডিং সহনশীলতা, টাকার নিয়ম বদলায় না
            return RefundResult(false, "Refund ₹${"%,.0f".format(amount)} is more than the refundable amount ₹${"%,.0f".format(maxRefundable)}")
        }
        val isMaster = user.role.equals("master", ignoreCase = true)
        // 🔴🔴🔒 B445 — আজকের জমা থেকে আজকের approved-refund বাদ দিয়ে
        // "স্টাফের হাতে এখনো কতটা আছে" বের করা হয়, রিফান্ড তার বেশি হলে
        // অটো-অ্যাপ্রুভ হবে না (চেম্বার খোলা থাকলেও)।
        // 🔴🔒 V1373 — isMaster হলে আগের মতোই এই তিনটে পড়াই স্কিপ (চেম্বার
        // যাচাই লাগে না)। নইলে তিনটে একসাথে (আগে একে একে হত)।
        val (availableFromToday, chamberOpen) = if (isMaster) Double.MAX_VALUE to true else runBlocking {
            val paidDef = async(Dispatchers.IO) { paidTodayForPatient(patient.id) }
            val refundedDef = async(Dispatchers.IO) { refundedTodayForPatient(patient.id, refundId) }
            val chamberDef = async(Dispatchers.IO) { chamberOpenToday(patient.branch) }
            val avail = (paidDef.await() - refundedDef.await()).coerceAtLeast(0.0)
            avail to chamberDef.await()
        }
        val autoApprove = isMaster || (chamberOpen && amount <= availableFromToday + 0.5)
        val status = if (autoApprove) PaymentModel.REFUND_APPROVED else PaymentModel.REFUND_PENDING
        val row = PaymentModel.buildRefundRow(
            patient, amount, mode, reason, status,
            requestedBy = user.mobile,
            approvedBy = if (autoApprove) user.mobile else "",
            staffMobile = user.mobile,
            nonce = effNonce
        )
        // এই ফোনে সঙ্গে সঙ্গে দেখানোর জন্য local cache (§13 same-phone immediate)।
        context?.let { try { LocalWorkflowStore(it).upsertPayment(row) } catch (_: Throwable) { } }
        // ব্যর্থ হলে SupabaseClient নিজেই CloudWriteQueue-তে জমা রাখে (retry হয়, কিছু হারায় না)।
        val ok = SupabaseClient.upsert("payments", row)
        // 🔴🔒 V1311 (তালিকা ৪২৩): ক্লাউডে বসলে ফোনের কপিও SYNCED — চিরকাল PENDING নয়।
        if (ok) context?.let { try { LocalWorkflowStore(it).upsertPayment(row, "SYNCED") } catch (_: Throwable) { } }
        // 🔒 V221 (§3): cloud-এ **সত্যিই বসলে তবেই** nonce মুছি — নেট-fail হলে রেখে
        // দিই, যাতে পরে একই Refund retry (crash-এর পরেও) একই id পায় (Duplicate নয়)।
        if (ok) context?.let { clearRefundNonce(it, nonceKey) }
        // 🔒🔒 V217 বাগ-ফিক্স (আগের কোড: `return ok || context != null` — context
        // প্রায় সবসময়ই non-null থাকে বলে cloud save ব্যর্থ হলেও এটা `true`
        // (Success) ফেরত দিত। TK-এর রিপোর্ট মতো এটাই আসল কারণ। এখন cloud-এ
        // সত্যিই সেভ হলো কিনা তার উপরেই ফেরত-মান নির্ভর করে — retry queue-তে
        // জমা থাকলেও UI-তে এখন সৎভাবে "ব্যর্থ" দেখানো হবে, "সফল" নয়।
        if (!autoApprove && ok) {
            // Staff refund request → Master-এর ঘন্টায় (শুধু cloud-এ সত্যিই বসলে,
            // নইলে Master-এর কাছে এমন অনুরোধ যেত যেটার রিফান্ড row-ই নেই)।
            context?.let { ctx ->
                try {
                    val amt = "₹" + "%,.0f".format(amount)
                    val msg = "$amt refund requested for ${patient.name.ifBlank { patient.mobile }} " +
                        "· Patient ID ${patient.patientId} · Branch ${patient.branch} " +
                        "· Reason: ${reason.ifBlank { "—" }} · by ${user.name.ifBlank { user.mobile }}. " +
                        "Approve/Reject from the bell."
                    BriefingRepository().post(ctx, "Refund request", msg, "role", patient.branch, "master", user.mobile)
                } catch (_: Throwable) { }
            }
        }
        // 🟢🔒 V676 (২৫.০৮.২০২৬, TK-নির্দেশ — "যেকোনো জায়গা থেকে Visit Fee
        // Return করলে যেন হয়ে যায়, আর Return-এর ট্যাগ (Draft-এর "Return
        // Visit" তালিকা) বসে")। আগে এই ট্যাগ শুধু Patient Timeline-এর
        // আলাদা "Return Fees" ডায়ালগেই বসত — Payment/DUE-এর সাধারণ Refund
        // দিয়ে করলে বসত না। এখন এই একই জায়গায় (সব Refund এখান দিয়েই যায়)
        // বসানো হলো, তাই আর কোনো স্ক্রিন আলাদা রাখতে হবে না।
        // ⛔ কোনো নতুন টাকা-হিসাব নেই — শুধু ইতিমধ্যে প্রমাণিত V509 নিয়ম
        //    (Refund আগে treatment paid থেকে, ছাড়ালে Visit Fee থেকে) ধরেই
        //    বোঝা হয় এই Refund Visit Fee ছুঁয়েছে কিনা (`amount > patient.paid`)।
        if (ok && autoApprove && amount > patient.paid + 0.5) {
            try {
                val digits = patient.mobile.filter { it.isDigit() }.takeLast(10)
                if (digits.length == 10) {
                    val fid = resolveBestFollowUpIdForReturn(digits)
                    if (!fid.isNullOrBlank()) {
                        SupabaseClient.updateById("followups", fid, JSONObject().put("status", "Returned"))
                    }
                }
            } catch (_: Throwable) { /* ⛔ ট্যাগ ব্যর্থ হলেও Refund সফল-ই থাকে */ }
        }
        return RefundResult(ok, if (ok) "" else "Could not save to cloud — check internet and try again")
    }

    /**
     * 🔴🔒 V1442 (১৩.০৯.২০২৬, TK-প্রশ্ন — "প্রকৃত টাকা ফেরত নেয়ার তারিখ এখানে
     * নেই কেন" — তালিকা ৫৬৬) — **ব্যাকডেট করা Refund।**
     *
     * TK-সিদ্ধান্ত: *"শুধুমাত্র মাস্টার করতে পারবে, অন্যান্যদের ক্ষেত্রে
     * মাস্টারের অনুমতি জরুরী।"* Treatment Payment-এর প্রমাণিত Backdate
     * ব্যবস্থার (BackdatePaymentGrant · payment_backdate_requests) হুবহু
     * একই যমজ নিয়ম — শুধু Refund-এর জন্য।
     *
     * ⛔ এখানে পৌঁছানো মানেই একজন Master ইতিমধ্যে এই তারিখ ও অঙ্কটা দেখে
     *    নিশ্চিত করেছেন (নিজে সরাসরি, বা staff-এর অনুরোধ Approve করে, বা
     *    আগে থেকে দেওয়া Grant দিয়ে) — তাই `saveRefund()`-এর "আজকের চেম্বার
     *    খোলা/আজকের হাতে-থাকা টাকা" প্রশ্নটা এখানে অর্থহীন ও **ইচ্ছে করেই
     *    বাদ**; approvalStatus সবসময় সরাসরি APPROVED।
     * ⛔ যে নিয়ম বাদ যায়নি — জমার চেয়ে বেশি Refund কখনো নয় (V217/V509-এর
     *    সেই একই তাজা-হিসাব cap), Visit Fee পর্যন্ত ধরা, ব্র্যাঞ্চ-গার্ড,
     *    ডাবল-ট্যাপ nonce, আর Visit-Fee ছোঁয়া হলে Draft-এর "Returned" ট্যাগ।
     */
    fun saveBackdatedRefund(
        patient: PatientBillInfo, amount: Double, mode: String, reason: String,
        forDate: String, requestedBy: String, approvedBy: String, staffMobile: String, nonce: String = ""
    ): RefundResult {
        if (amount <= 0) return RefundResult(false, "Enter a valid refund amount")
        if (forDate.isBlank()) return RefundResult(false, "Refund date missing")
        context?.let { ctx ->
            if (!MoneyBranchGuard.canTakeMoney(ctx, patient.branch, patient.patientId)) return RefundResult(false, "Not allowed for this branch")
        }
        val nonceKey = refundNonceKey(patient, amount, reason)
        val effNonce = context?.let { getOrCreateRefundNonce(it, nonceKey) } ?: nonce
        val refundId = PaymentModel.refundIdFor(patient, amount, reason, requestedBy, effNonce)
        // 🔒 saveRefund()-এর হুবহু একই cap-যাচাই (V217/V509) — শুধু তারিখটা বাদ,
        // কারণ এখানে "আজ কতটা হাতে আছে" প্রশ্নটাই ওঠে না।
        val (alreadyPending, liveByMobile) = runBlocking {
            val pendingDef = async(Dispatchers.IO) { pendingRefundSumForPatient(patient.id, refundId) }
            val liveDef = async(Dispatchers.IO) {
                try { findPatientByMobile(patient.mobile, patient.branch) } catch (_: Throwable) { null }
            }
            pendingDef.await() to liveDef.await()
        }
        val liveRefundable = try {
            val live = liveByMobile
            if (live != null && live.id == patient.id && !live.paymentsUnverified)
                minOf(live.refundableTotal, patient.refundableTotal)
            else patient.refundableTotal
        } catch (_: Throwable) { patient.refundableTotal }
        val maxRefundable = (liveRefundable - alreadyPending).coerceAtLeast(0.0)
        if (amount > maxRefundable + 0.5) {
            return RefundResult(false, "Refund ₹${"%,.0f".format(amount)} is more than the refundable amount ₹${"%,.0f".format(maxRefundable)}")
        }
        val row = PaymentModel.buildRefundRow(
            patient, amount, mode, reason, PaymentModel.REFUND_APPROVED,
            requestedBy = requestedBy, approvedBy = approvedBy, staffMobile = staffMobile,
            nonce = effNonce, overrideDate = forDate,
            backdateRequestedBy = requestedBy, backdateApprovedBy = approvedBy
        )
        context?.let { try { LocalWorkflowStore(it).upsertPayment(row) } catch (_: Throwable) { } }
        val ok = SupabaseClient.upsert("payments", row)
        if (ok) context?.let { try { LocalWorkflowStore(it).upsertPayment(row, "SYNCED") } catch (_: Throwable) { } }
        if (ok) context?.let { clearRefundNonce(it, nonceKey) }
        // 🟢🔒 V676-এর হুবহু একই ট্যাগ — saveRefund()-এ যা হয়, এখানেও তাই।
        if (ok && amount > patient.paid + 0.5) {
            try {
                val digits = patient.mobile.filter { it.isDigit() }.takeLast(10)
                if (digits.length == 10) {
                    val fid = resolveBestFollowUpIdForReturn(digits)
                    if (!fid.isNullOrBlank()) {
                        SupabaseClient.updateById("followups", fid, JSONObject().put("status", "Returned"))
                    }
                }
            } catch (_: Throwable) { }
        }
        return RefundResult(ok, if (ok) "" else "Could not save to cloud — check internet and try again")
    }

    /** V676 — Chamber Attendance-এর `resolveBestFollowUpId()`-এর হুবহু একই,
     *  প্রমাণিত stage-priority নিয়ম (Treatment > Patient > Inquiry, terminal
     *  স্ট্যাটাস বাদ) — শুধু এখানে blocking (এই ফাংশন এমনিতেই IO থ্রেডে চলে)। */
    private fun resolveBestFollowUpIdForReturn(digits: String): String? = try {
        fun stagePriority(s: String): Int = when {
            s.equals("Treatment", true) || s.equals("Treatment Running", true) -> 3
            s.equals("Patient", true) -> 2
            s.equals("Inquiry", true) -> 1
            else -> 0
        }
        val rows = SupabaseClient.findByMobileOrNull("followups", "+91$digits", "id,status,stage,updatedAt,createdAt", 50)
        rows?.let { r ->
            (0 until r.length()).map { r.getJSONObject(it) }
                .filter {
                    val st = it.optString("status", "Active")
                    !st.equals("Cancelled", true) && !st.equals("Incomplete", true) &&
                        !st.equals("Rejected", true) && !st.equals("Closed", true)
                }
                .maxWithOrNull(
                    compareBy<org.json.JSONObject> { stagePriority(it.optString("stage", "")) }
                        .thenBy { it.optString("updatedAt").ifBlank { it.optString("createdAt") } }
                )?.optString("id", "")
        }
    } catch (_: Throwable) { null }

    /** Master-এর ঘন্টার জন্য pending refund request-এর সস্তা count। */
    fun fetchPendingRefundCount(): Int =
        SupabaseClient.fetchCount("payments", "payType=eq.refund&refundApprovalStatus=eq.pending")
            .coerceAtLeast(0)

    /** Briefing পর্দার Master-only "Pending Refund Requests" তালিকা (payments row)। */
    fun fetchPendingRefundRequests(): List<JSONObject> {
        val rows = SupabaseClient.fetchList("payments", "payType=eq.refund&refundApprovalStatus=eq.pending", 200)
        return (0 until rows.length()).map { rows.getJSONObject(it) }
    }

    /** Master approve → refundApprovalStatus=approved (তখনই total থেকে কমবে)। */
    fun approveRefund(refundId: String, masterMobile: String): Boolean {
        if (refundId.isBlank()) return false
        val fields = JSONObject()
            .put("refundApprovalStatus", PaymentModel.REFUND_APPROVED)
            .put("refundApprovedBy", masterMobile)
            .put("updatedAt", isoNow())
        return SupabaseClient.updateById("payments", refundId, fields)
    }

    /** Master reject → refundApprovalStatus=rejected (total-এ কোনো প্রভাব নেই; row থাকে, history-তে দেখা যায়)। */
    fun rejectRefund(refundId: String, masterMobile: String): Boolean {
        if (refundId.isBlank()) return false
        val fields = JSONObject()
            .put("refundApprovalStatus", PaymentModel.REFUND_REJECTED)
            .put("refundApprovedBy", masterMobile)
            .put("updatedAt", isoNow())
        return SupabaseClient.updateById("payments", refundId, fields)
    }

    // TK-REQUESTED ADDITION (2026-07-25): "Edit Payment Amount" workflow --
    // Staff side, only reached once PaymentModel.withinFreeEditWindow()
    // returns false (i.e. the free same-day/next-day window has passed).
    // Does NOT touch the real payments row at all -- only writes a pending
    // row here, exactly like requestBackdatePayment above.
    fun requestPaymentEdit(
        paymentId: String, patient: PatientBillInfo, oldAmount: Double, newAmount: Double, mode: String,
        paymentDate: String, reason: String, staffMobile: String, staffName: String
    ): Boolean {
        if (newAmount <= 0) return false
        val row = JSONObject()
            .put("id", "editreq_" + java.util.UUID.randomUUID().toString().replace("-", ""))
            .put("paymentId", paymentId)
            .put("patientRowId", patient.id)
            .put("patientCode", patient.patientId)
            .put("mobile", patient.mobile)
            .put("name", patient.name)
            .put("branch", patient.branch)
            .put("oldAmount", oldAmount)
            .put("newAmount", newAmount)
            .put("mode", mode)
            .put("paymentDate", paymentDate)
            .put("reason", reason)
            .put("requestedBy", staffMobile)
            .put("requestedByName", staffName)
            .put("requestedAt", isoNow())
            .put("status", "pending")
            .put("createdAt", isoNow())
            .put("updatedAt", isoNow())
        return SupabaseClient.upsert("payment_edit_requests", row)
    }

    // TK-REQUESTED ADDITION (2026-07-25): for the SAME Dashboard bell the
    // Backdate count already uses -- Master sees ONE combined number for
    // "things waiting on me", not two separate bells to check.
    // 🔒 খাতার সারি B145 — উপরের একই কারণ, একই সুরক্ষা।
    fun fetchPendingEditCount(): Int =
        SupabaseClient.fetchCount("payment_edit_requests", "status=eq.pending")
            .coerceAtLeast(0)

    // TK-REQUESTED ADDITION (2026-07-25): for the Briefing screen's
    // Master-only pending-requests section, alongside Backdate requests.
    fun fetchPendingEditRequests(): List<PaymentEditRequest> {
        val rows = SupabaseClient.fetchList("payment_edit_requests", "status=eq.pending", 200)
        val list = mutableListOf<PaymentEditRequest>()
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            list.add(
                PaymentEditRequest(
                    id = r.s("id"), paymentId = r.s("paymentId"), patientRowId = r.s("patientRowId"),
                    patientCode = r.s("patientCode"), mobile = r.s("mobile"), name = r.s("name"), branch = r.s("branch"),
                    oldAmount = r.optDouble("oldAmount", 0.0), newAmount = r.optDouble("newAmount", 0.0),
                    mode = r.s("mode"), paymentDate = r.s("paymentDate"), reason = r.s("reason"),
                    requestedBy = r.s("requestedBy"), requestedByName = r.s("requestedByName"),
                    requestedAt = r.s("requestedAt"), status = r.s("status")
                )
            )
        }
        return list
    }

    // TK-REQUESTED ADDITION (2026-07-25): Master side -- actually changes
    // the real payment row's amount, only once approved. Reuses the same
    // audit-trail update pattern as an ordinary 3-tap edit elsewhere
    // (editHistory/editedAt/editedBy), so this shows up in Full Journey /
    // Report Card exactly like any other edit already does.
    fun approvePaymentEditRequest(request: PaymentEditRequest, masterMobile: String): Boolean {
        return try {
            // V452: never flatten a combined CASH+ONLINE day into one guessed
            // amount/mode. This one-row read happens only when Master approves
            // an old edit request; it is not a recurring Free-plan call.
            val current = SupabaseClient.fetchListSlim(
                "payments", "id=eq.${request.paymentId}", 1,
                cols = "id,payType,amount,mode,dailyEvents"
            ).optJSONObject(0) ?: return false
            val eventCount = current.optJSONArray("dailyEvents")?.length()?.coerceAtLeast(1) ?: 1
            if (current.s("payType").equals("treatment", true) && eventCount > 1) return false

            val fields = JSONObject()
                .put("amount", request.newAmount)
                .put("editedAt", isoNow())
                .put("editedBy", masterMobile)
                .put("editRequestedBy", request.requestedBy)
                .put("editApprovedBy", masterMobile)
                .put("updatedAt", isoNow())
            val ok = SupabaseClient.updateById("payments", request.paymentId, fields)
            if (ok) {
                SupabaseClient.updateById(
                    "payment_edit_requests", request.id,
                    JSONObject().put("status", "approved").put("approvedBy", masterMobile)
                        .put("approvedAt", isoNow()).put("updatedAt", isoNow())
                )
            }
            ok
        } catch (_: Throwable) { false }
    }

    // TK-REQUESTED ADDITION (2026-07-25): Master rejects -- the real
    // payment amount is never touched, just marks the request rejected.
    fun rejectPaymentEditRequest(requestId: String, masterMobile: String): Boolean {
        val fields = JSONObject()
            .put("status", "rejected")
            .put("approvedBy", masterMobile)
            .put("approvedAt", isoNow())
            .put("updatedAt", isoNow())
        return SupabaseClient.updateById("payment_edit_requests", requestId, fields)
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Visit Fee Missing" visibility --
    // every registered patient should have exactly one payType="visit_fee"
    // payment (buildVisitFeePaymentRow is always called at Registration,
    // "regFee > 0 always true" per its own comment). Cross-references
    // "patients" against "payments" to find any without one -- read-only,
    // zero risk to any existing data. The actual missing amount can't be
    // safely guessed back, so this is surfaced for Master to check/
    // re-collect manually rather than self-healed.
    /**
     * 🚨 TK-REPORTED (29.07.2026 দুপুর ২.১০, খাতার সারি B87): *"একবার পড়া হয়ে
     * গেছিল... প্রতিটা জিনিসে চাপ দিয়েছিলাম, সম্পূর্ণ শূন্য হয়ে গেছিল। নতুন
     * ফাইল বিল্ড করার পরে আবার কেন দেখাচ্ছে?"*
     *
     * **আসল কারণ:** \"পড়া হয়েছে\" চিহ্নটা এতদিন **শুধু ওই ফোনের ভিতরে** জমা
     * থাকত (SharedPreferences)। নতুন APK বসালে ফোনের ওই খাতা মুছে যায়, তাই
     * পুরনো সব নাম আবার ফিরে আসত।
     *
     * **এখন চিহ্নটা ক্লাউডেও জমা থাকে**, তাই APK বদলালেও থাকবে, আর মাস্টার
     * অন্য ফোনে দেখলেও একই থাকবে।
     *
     * 🔒 **কোনো নতুন টেবিল বা SQL লাগেনি** — আগে থেকেই থাকা `activity_logs`
     * টেবিলেই লেখা হয় (`module = "fee_missing"`)। ওই টেবিলটা শুধু হিসাব
     * রাখার জন্য, কোনো পর্দায় দেখানো হয় না, তাই কিছু এলোমেলো হবে না।
     *
     * ⛔ **টাকার কোনো হিসাব বদলায় না** — শুধু \"এই নামটা দেখা হয়ে গেছে\" এই
     * তথ্যটুকু জমা থাকে। ⛔ ব্যর্থ হলে কিছুই ভাঙে না — ফোনের পুরনো চিহ্নটা
     * আগের মতোই কাজ করে।
     */
    fun fetchFeeMissingSeenKeys(): Set<String> {
        return try {
            // ⚠️ `activity_logs` টেবিলে `updatedAt` ঘর **নেই** (দেখুন
            // `PILES_CLINIC_DB_SETUP.sql`) — তাই সাজানোর ঘরটা হাতে করে
            // `createdAt` দেওয়া হয়েছে। ডিফল্ট (`updatedAt`) রাখলে ক্লাউড
            // ভুল বলে ফেরত দিত আর চিহ্নগুলো কোনোদিন পড়াই যেত না।
            val rows = SupabaseClient.fetchListSlimOrNull(
                "activity_logs", "module=eq.fee_missing", 5000, "recordId",
                order = "createdAt.desc.nullslast"
            ) ?: return emptySet()
            val out = HashSet<String>()
            for (i in 0 until rows.length()) {
                val k = rows.optJSONObject(i)?.s("recordId") ?: ""
                if (k.isNotBlank()) out.add(k)
            }
            out
        } catch (_: Throwable) { emptySet() }
    }

    /** এক রোগীর নাম \"দেখা হয়েছে\" বলে ক্লাউডে লিখে রাখে। আইডিটা চাবির উপরেই
     *  বসানো, তাই একই নামে বারবার চাপ দিলেও **একটাই সারি** থাকে — টেবিল ভরে
     *  যায় না। ব্যর্থ হলে চুপচাপ ফিরে আসে, কিছু ভাঙে না। */
    fun markFeeMissingSeenCloud(key: String, byMobile: String, byName: String): Boolean {
        if (key.isBlank()) return false
        return try {
            val row = JSONObject()
                .put("id", "feeseen_" + key.filter { it.isLetterOrDigit() || it == '-' })
                .put("module", "fee_missing")
                .put("action", "seen")
                .put("recordId", key)
                .put("userMobile", byMobile)
                .put("userName", byName)
                .put("createdAt", isoNow())
            SupabaseClient.upsert("activity_logs", row)
        } catch (_: Throwable) { false }
    }

    /**
     * @param fresh `true` হলে "৩ মিনিটের ব্যবধান" নিয়মটা মানা হয় না — যে **পর্দা**
     *   এই তালিকাটা চোখের সামনে দেখায় (Briefing) সে এটা পাঠায়, যাতে ফি নেওয়ার
     *   সঙ্গে সঙ্গে নামটা তালিকা থেকে চলে যায়। ⛔ তখনও "কিছু বদলায়নি" হলে জমানো
     *   উত্তরই ফেরে (তাতে ভুল কিছু নেই — বদলায়নি মানে তালিকাও বদলায়নি)।
     */
    fun fetchMissingVisitFeePatients(fresh: Boolean = false): List<MissingVisitFee> {
        // 🟢🔒 B662 (15.08.2026, TK-অনুমোদিত · Egress-৪) — নিচের গোনার নিয়ম এক অক্ষরও
        //   বদলায়নি; শুধু **আগের উত্তরটা মনে রাখা** হয়, আর নতুন করে গোনা হয় তখনই যখন
        //   সত্যিই দরকার। তিনটে শর্তের যেকোনো একটাতেই জমানো উত্তর ফেরে:
        //     ১) রাত ১০টা – সকাল ৬টা (কেউ কাজ করেন না — LiveRefresh-এর পুরনো নিয়ম)
        //     ২) শেষ গোনার পরে ৩ মিনিটও হয়নি
        //     ৩) patients/payments টেবিলে গতবারের পরে **কিছুই বদলায়নি**
        //   ⛔ (৩)-এর প্রশ্নে একটাও সারি নামে না — শুধু একটা সংখ্যা আসে (HEAD)।
        //   ⛔ প্রথমবার সবসময় সত্যিকারের গোনা হয় (জমানো কিছু নেই), তাই ঘণ্টা কখনো ফাঁকা নয়।
        //   ⛔ উত্তর না এলে `changed()` false দেয় → জমানো উত্তরই থাকে, কিছু ভাঙে না।
        //   ⚠️ যেটুকু দাম: নতুন রোগীর ফি বাকি থাকলে মাস্টারের ঘণ্টায় সেটা **সর্বোচ্চ ৩ মিনিট
        //      দেরিতে** উঠতে পারে (TK-কে জানিয়ে অনুমতি নেওয়া হয়েছে)।
        val cachedNow = synchronized(LOCK) { missingFeeCache }
        if (cachedNow != null) {
            if (!LiveRefresh.awake()) return cachedNow
            val gap = System.currentTimeMillis() - synchronized(LOCK) { missingFeeAt }
            if (!fresh && gap < MISSING_FEE_MIN_GAP_MS) return cachedNow
            if (!missingFeeWatch.changed("missingfee", null)) {
                synchronized(LOCK) { missingFeeAt = System.currentTimeMillis() }
                return cachedNow
            }
        }
        // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): same two
        // reads, same rows, same filter -- only the six patient fields and the
        // one payment field this check actually looks at are downloaded now
        // (it used to bring 38 patient columns and every payment column).
        // ⛔ THE ANSWER CANNOT CHANGE: the comparison below is left word for
        // word, and a failed narrowed read falls back to every column by
        // itself, so this list can never come out wrongly long or short.
        /* 🔴🔴🔒 V1060 (০৪.০৯.২০২৬) — **এই তালিকার আসল গোড়া, অবশেষে।**
           TK: *"খাতায় ভালো করে দেখুন তো এই সমস্যার কথা কতবার আপনাকে বলা হয়েছে"* —
           খাতা মিলিয়ে দেখা গেল **পাঁচবার** (V147 · V151 · V154 · V901 · V958)।
           আগের প্রতিটা সমাধান ছিল **লেখার দিকে** (ফি-র সারিটা যেন তৈরি হয়);
           কিন্তু দোষটা ছিল **পড়ার দিকে**, তাই বারবার ফিরে এসেছে। দুটো দোষ:

           ① 🔢 **৫০০০-এর ছাঁট।** ফি-র সারি আনা হত `limit 5000`, আর ক্রম
              `updatedAt.desc` — অর্থাৎ **নতুন ৫০০০টা**। ক্লিনিক বড় হয়ে ৫০০০
              ছাড়াতেই সবচেয়ে **পুরনো** ফি-গুলো ছাঁটা পড়ত, তাই ওই পুরনো
              রোগীদেরই "ফি নেই" মনে হত। TK-এর ছবিতে ঠিক তাই — ৩০.০৩.২০২৬ ও
              ২৭.০৪.২০২৬-এ রেজিস্টার হওয়া দুজন, তালিকার সবচেয়ে পুরনো।
              ⇒ এখন **পাতা ধরে ধরে সবটা** আনা হয়, একটাও বাদ যায় না।

           ② 🏷️ **নামের বানান।** এখানে গোনা হত শুধু `visit_fee`, অথচ টাকার SQL
              (V418-এর `incentive_wanted()`) ধরে **তিনটে** — `visit_fee` ·
              `visitfee` · `registration`; কম্পিউটারের যাচাইও (app.js) দুটো ধরে।
              অর্থাৎ এক জিনিসের নিয়ম দুই জায়গায় দুরকম (নিয়ম ৭ক-এর ২)।
              ⇒ এখন তিনটেই ধরা হয়, ঠিক SQL-এর মতো।

           ⛔ গোনার বাকি নিয়ম এক অক্ষরও বদলায়নি — মোবাইল ধরে এক করা, "যে কোনো
              একটা সারিতে ফি থাকলেই হয়েছে" — সবই আগের মতোই।
           ⛔ পড়া ব্যর্থ হলে আগের মতোই ফাঁকা তালিকা ফেরে (ভুল করে লম্বা নয়)। */
        fun fetchAllPages(table: String, filter: String?, cols: String): org.json.JSONArray? {
            val out = org.json.JSONArray()
            var offset = 0
            val page = 1000
            while (true) {
                /* 🔒 V1060খ (নিজে যাচাই করে ধরা, দুটো আসল ঝুঁকি):
                   ① ক্রম **`id.asc`** — `updatedAt` একই/ফাঁকা হতে পারে, তাই ওটা
                      দিয়ে পাতা করলে পড়ার মাঝে সারি **এড়িয়ে যেতে পারত**; একটা
                      ফি-র সারি এড়ালেই ওই রোগী ভুল করে তালিকায় উঠত। `id`
                      অনন্য, তাই পাতাগুলো সবসময় একই ক্রমে আসে।
                   ② **যেকোনো** পাতা না এলে আধা-তালিকা নয়, `null` — অর্থাৎ
                      "জানি না"। আধা-তালিকা দিলে বাকি রোগীদের ফি "নেই" মনে হত।
                      পুরনো আচরণই: না জানলে তালিকা ফাঁকা, ভুল নাম ওঠে না। */
                val part = SupabaseClient.fetchListSlimOrNull(
                    table, filter, page, cols, order = "id.asc", offset = offset
                ) ?: return null
                for (i in 0 until part.length()) out.put(part.get(i))
                if (part.length() < page) break
                offset += page
                if (offset >= 200000) break                  // অসীম লুপের বিরুদ্ধে পাহারা
            }
            return out
        }
        val patients = fetchAllPages(
            "patients", null,
            "id,name,mobile,branch,patientId,bill,registrationDate,date,updatedAt"   // bill দরকার — কোন সারিটা আসল সেটা বাছতে
        ) ?: return emptyList()
        val feePayments = fetchAllPages(
            "payments", "payType=in.(visit_fee,visitfee,registration)", "id,patientId,updatedAt"
        ) ?: return emptyList()
        val patientIdsWithFee = HashSet<String>()
        for (i in 0 until feePayments.length()) {
            val pid = feePayments.getJSONObject(i).s("patientId")
            if (pid.isNotBlank()) patientIdsWithFee.add(pid)
        }
        // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B30): এই তালিকায়
        // একই রোগী **দু'বার/তিনবার** দেখাচ্ছিল (ABDUL KAYAM), কারণ আগে ভুল করে
        // তাঁর একাধিক সারি তৈরি হয়ে গিয়েছিল।
        //
        // নতুন সারি তৈরি হওয়া বন্ধ হয়েছে, কিন্তু পুরনোগুলো ডেটাবেসে রয়ে গেছে।
        // ⛔ পুরনো সারি মুছে ফেলা হয়নি (টাকা ও ইতিহাস ওতে থাকতে পারে) — বদলে
        // **এই তালিকাটাই মোবাইল ধরে এক করে দেখায়**, তাই একই মানুষ আর দু'বার
        // ওঠে না।
        //
        // 🔒 টাকার সুরক্ষা: রেজিস্ট্রেশন ফি রোগীর **যে কোনো একটা সারির** নামে
        // জমা থাকলেই ধরে নেওয়া হয় ফি নেওয়া হয়ে গেছে — তাই যাঁর ফি সত্যিই
        // নেওয়া আছে তিনি আর ভুল করে "Visit Fee Missing"-এ উঠবেন না।
        val byMobile = LinkedHashMap<String, MutableList<org.json.JSONObject>>()
        for (i in 0 until patients.length()) {
            val row = patients.optJSONObject(i) ?: continue
            val key = row.s("mobile").filter { it.isDigit() }.takeLast(10)
                .ifBlank { row.s("id") }
            byMobile.getOrPut(key) { mutableListOf() }.add(row)
        }
        val missing = mutableListOf<MissingVisitFee>()
        for ((_, rows) in byMobile) {
            // এই মানুষটার কোনো সারির নামে ফি জমা আছে কি না
            val anyHasFee = rows.any { r ->
                val rid = r.s("id")
                val code = r.s("patientId")
                (rid.isNotBlank() && patientIdsWithFee.contains(rid)) ||
                    (code.isNotBlank() && patientIdsWithFee.contains(code))
            }
            if (anyHasFee) continue
            val arr = org.json.JSONArray()
            for (r in rows) arr.put(r)
            val p = PatientIdentity.pickPatientRow(arr, "") ?: rows.first()
            val id = p.s("id")
            if (id.isBlank()) continue
            /* 🟢 V1121 — V1101-এর পাহারা বসার আগের রেজিস্ট্রেশন হলে তালিকায় নয়
               (উপরের বড় নোটে কারণ ও ঝুঁকি লেখা আছে)। */
            val regDay = p.s("registrationDate").ifBlank { p.s("date") }.take(10)
            if (ISO_DAY.matches(regDay) && regDay < FEE_GUARD_FROM) continue
            missing.add(
                MissingVisitFee(
                    patientRowId = id,
                    name = p.s("name"),
                    mobile = p.s("mobile"),
                    branch = p.s("branch"),
                    patientId = p.s("patientId"),
                    registrationDate = p.s("registrationDate").ifBlank { p.s("date") }
                )
            )
        }
        // 🟢 B662: সত্যিকারের গোনা শেষ — উত্তরটা মনে রাখা হলো (উপরের শর্তগুলোর জন্য)।
        synchronized(LOCK) {
            missingFeeCache = missing
            missingFeeAt = System.currentTimeMillis()
        }
        return missing
    }

    /** The actual cloud write for one Advance/Treatment payment -- bill
     * update (if not locked), the payment row itself, and promoting the
     * follow-up to Treatment. Used both by the first attempt above and by
     * flushPending()'s retry, so both paths do exactly the same thing. */
    private fun pushPaymentToCloud(
        patient: PatientBillInfo, effectiveBill: Double, paymentRow: JSONObject,
        staffMobile: String, sameDayRepeat: Boolean = false,
        /* 🟢🔒 V706 (২৬.০৮.২০২৬, TK-নির্দেশ) — TK: *"কোন পেশেন্ট এর পেমেন্ট আটকে
           রয়েছে সেটাই বা আমি জানবো কি করে"*। এখানে **কিছুই বদলায়নি** — শুধু
           কোন ধাপটা আটকাল সেটা ডাকনেওয়ালাকে জানানোর একটা ঐচ্ছিক বাক্স।
           ⛔ ডিফল্ট `null`, তাই আগের দুটো ডাক (প্রথম চেষ্টা ও flushPending)
              এক অক্ষরও না বদলেই চলে; টাকার হিসাব/return মান অপরিবর্তিত। */
        whyOut: StringBuilder? = null
    ): Boolean {
        // 🔴🔴🔒 V663 (২৫.০৮.২০২৬) — একই বাগের দ্বিতীয়, সমান গুরুত্বপূর্ণ অংশ:
        // আগে এখানে `if (!patient.billLocked)` — billLocked থাকলে ক্লাউডে
        // বিল **লেখাই হতো না**, `effectiveBill`-এ (উপরের ফিক্সের পরে) সঠিক
        // নতুন মান থাকলেও। এখন শর্তটা "বিল সত্যিই বদলেছে কিনা" — বদলে
        // থাকলে (locked হোক বা না হোক) ক্লাউডে লেখা হয়; না বদলালে আগের
        // মতোই বাড়তি write এড়ানো হয় (কোনো ঝুঁকি নেই, একই মান আবার লেখাই)।
        val billUpdateOk = if (effectiveBill != patient.bill) {
            val billFields = JSONObject()
                .put("bill", effectiveBill).put("stage", "Treatment Running").put("updatedAt", isoNow())
            val done = SupabaseClient.updateById("patients", patient.id, billFields)
            /* 🔴🔒 V1311 (তালিকা ৪২৩): বিল ক্লাউডে বসে গেলে ফোনের খাতার রোগী-কপিটাও SYNCED —
               আগে এই কপি (তারিখ-ঘর ছাড়া, stage-সহ) চিরকাল PENDING থেকে যেত (৪২২-এর একই জাত)। */
            if (done) context?.let { ctx ->
                try { LocalWorkflowStore(ctx).upsertPatient(JSONObject().put("id", patient.id).put("bill", effectiveBill).put("stage", "Treatment Running").put("updatedAt", billFields.optString("updatedAt")), "SYNCED") } catch (_: Throwable) { }
            }
            /* 🔴🔒 V1205 (০৮.০৯.২০২৬, TK-রিপোর্ট) — বিল আপডেট ব্যর্থ হলেও আগে
               পুরো পেমেন্ট "ব্যর্থ" ধরা হত ⇒ টাকা ক্লাউডে গিয়েও সারিটা চিরকাল
               অপেক্ষমাণ তালিকায় থাকত। এখন কাজটা `GenericUpdateQueue`-এ জমা থাকে
               (হারায় না, নেট ফিরলে নিজে থেকেই বসে), তাই আর আটকায় না।
               ⛔ টাকার কোনো অঙ্ক এতে বদলায় না। */
            if (!done) {
                var queued = false
                try { context?.let { GenericUpdateQueue.queue(it, "patients", patient.id, billFields); queued = true } }
                catch (_: Throwable) { }
                queued
            } else true
        } else true
        // V452: one cloud write returns the REAL daily owner row. This is
        // safer than trusting sameDayRepeat from local cache: after reinstall
        // this phone may not know another device already collected money today.
        val canonicalPayment = SupabaseClient.recordTreatmentPayment(paymentRow)
        val paymentOk = canonicalPayment != null
        context?.let { ctx ->
            if (paymentOk) {
                val store = LocalWorkflowStore(ctx)
                val eventId = paymentRow.optString("id", "")
                val canonicalId = canonicalPayment!!.optString("id", "")
                if (eventId.isNotBlank() && canonicalId != eventId) store.removePayment(eventId)
                store.upsertPayment(canonicalPayment, "SYNCED")
            }
        }
        // 🔴🔒 V1615 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ — টাকা জমা পড়ার পরেও রোগী
        // "Patient" তালিকায় ওঠেনি, Visit-এই আটকে ছিল) — এই কাজটা
        // (promoteFollowUpToTreatment) ব্যর্থ হলে (সাধারণত এক মুহূর্তের নেট-
        // হোঁচট) আগে **আর কখনো চেষ্টা হতো না** — টাকা ঠিকই জমা হতো, কিন্তু
        // কার্ডটা চিরকাল Visit-এ পড়ে থাকত (নিচের `whyOut`-এ "patient card
        // pending" লেখা হতো ঠিকই, কিন্তু সেটা কোথাও দেখানো/আবার চেষ্টা করা
        // হতো না)। এখন প্রথমবার ব্যর্থ হলে **সঙ্গে সঙ্গে একবার আবার** চেষ্টা
        // হয় — টাকা/বিলের হিসাবে কোনো হাত পড়ে না, শুধু কার্ড ওঠার এই একটা
        // ধাপই আবার চালানো হয় (`promoteFollowUpToTreatment` নিজে idempotent —
        // দুবার সফল হলেও একই সারিই আপডেট হয়, নতুন কিছু জমে না)।
        val promoted = if (billUpdateOk && paymentOk) {
            promoteFollowUpToTreatment(patient, staffMobile) || promoteFollowUpToTreatment(patient, staffMobile)
        } else false
        // 🟢🔒 V706 — শুধু লেখা হয়, কোনো সিদ্ধান্ত এর উপর নির্ভর করে না।
        //    ইংরেজি (TK-নির্দেশ: "বাংলা হবে না, শুধুমাত্র ইংরেজিতে করুন")।
        whyOut?.setLength(0)
        whyOut?.append(
            when {
                !paymentOk -> "Money row not sent yet"
                !billUpdateOk -> "Money sent - bill update pending"
                !promoted -> "Money sent - patient card pending"
                else -> ""
            }
        )
        return billUpdateOk && paymentOk && promoted
    }

    /** TK-REQUESTED (2026-07-22): correct a wrongly-entered Total Bill on its
     *  own -- no new payment required. Anyone may do it; an audit "bill_edit"
     *  row (zero amount, never affects totals or Chamber "arrived") records WHO
     *  changed the bill, WHEN, and from what to what, so it shows in the payment
     *  history. Local-first (instant, offline-safe), then best-effort cloud. */
    fun correctBill(patient: PatientBillInfo, newBill: Double, staffMobile: String): Boolean {
        if (newBill <= 0 || newBill > BILL_TYPO_GUARD_MAX) return false
        val auditRow = PaymentModel.buildBillEditRow(
            patient.mobile, patient.name, patient.branch, patient.bill, newBill, staffMobile
        )
        context?.let {
            val store = LocalWorkflowStore(it)
            store.upsertPatient(
                JSONObject()
                    .put("id", patient.id)
                    .put("mobile", patient.mobile)
                    .put("name", patient.name)
                    .put("branch", patient.branch)
                    .put("patientId", patient.patientId)
                    .put("bill", newBill)
                    .put("updatedAt", isoNow())
            )
            store.upsertPayment(auditRow)
        }
        val billOk = try {
            SupabaseClient.updateById("patients", patient.id, JSONObject().put("bill", newBill).put("updatedAt", isoNow()))
        } catch (_: Throwable) { false }
        if (!billOk) context?.let {
            GenericUpdateQueue.queue(it, "patients", patient.id, JSONObject().put("bill", newBill).put("updatedAt", isoNow()))
        }
        /* 🔴 V1310 (তালিকা ৪২২): ক্লাউডে বিল বসে গেলে ফোনের খাতার আধখানা কপিটাও "হয়ে গেছে" (SYNCED) —
           নইলে PENDING হয়ে চিরকাল থেকে যেত আর তালিকাগুলোয় ঢুকে পড়ত। */
        if (billOk) context?.let {
            try { LocalWorkflowStore(it).upsertPatient(JSONObject().put("id", patient.id).put("bill", newBill).put("updatedAt", isoNow()), "SYNCED") } catch (_: Throwable) { }
        }
        val auditOk = try { SupabaseClient.upsert("payments", auditRow) } catch (_: Throwable) { false }
        context?.let { if (auditOk) LocalWorkflowStore(it).upsertPayment(auditRow, "SYNCED") }
        return billOk
    }

    private val paymentPendingPrefs = context?.getSharedPreferences("piles_clinic_payment_pending", Context.MODE_PRIVATE)

    /** Queues everything needed to retry pushPaymentToCloud() later:
     * the patient's key fields (not the whole object -- only what
     * pushPaymentToCloud/promoteFollowUpToTreatment actually read),
     * the effective bill, the exact payment row, and who saved it.
     * Re-running this is always safe to repeat (idempotent) -- see the
     * comment on flushPending() below for why. */
    private fun queuePaymentPending(
        patient: PatientBillInfo, effectiveBill: Double, paymentRow: JSONObject,
        staffMobile: String, sameDayRepeat: Boolean
    ): String {
        val prefs = paymentPendingPrefs ?: return ""
        synchronized(LOCK) {
        val entry = JSONObject()
            .put("patientId", patient.id)
            .put("patientMobile", patient.mobile)
            .put("patientName", patient.name)
            .put("patientBranch", patient.branch)
            .put("patientCode", patient.patientId)
            .put("billLocked", patient.billLocked)
            .put("effectiveBill", effectiveBill)
            .put("staffMobile", staffMobile)
            .put("sameDayRepeat", sameDayRepeat)
            .put("paymentRow", paymentRow)
        // 🚨 TK-REPORTED, LIVE (2026-07-27): "কখনো পেমেন্ট হারিয়ে যায়, কখনো
        // পেশেন্ট হারিয়ে যায়..." ROOT CAUSE FOUND HERE.
        //
        // The retry loop that later pushes this row to the cloud SKIPS any row
        // whose id sits in the "deleted" list (DeletedGuard) . that guard
        // exists so a record deleted by staff cannot be resurrected by an old
        // queued save, which is right. BUT an id can legitimately come back:
        // "Update Existing" on the duplicate-mobile popup reuses the same row
        // id, a patient restored from Trash keeps their id, and a person can
        // be registered again after being deleted. In every one of those
        // cases the brand-new save was silently thrown away FOREVER . the
        // staff saw "saved", nothing stayed queued, and nothing ever reached
        // the cloud. That is a patient or a payment simply gone.
        //
        // FIX: a NEW save always beats an OLD delete mark. Clearing the mark
        // here, at the moment of saving, keeps the guard's real purpose
        // intact: if staff delete this record AFTER this save is queued, the
        // delete marks it again and the retry still correctly drops it.
        try { DeletedGuard.unmark("payments", paymentRow.optString("id", ""), context) } catch (_: Throwable) { }
        val queue = loadPaymentPendingQueue()
        queue.put(entry)
        prefs.edit().putString("queue", queue.toString()).commit()
        // TK-REQUESTED (2026-07-25): sync immediately, even if the staff
        // closes the app right after saving . WorkManager does the upload
        // in the background; the same proven flushPending() work, sooner.
        context?.let { c2 -> try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(c2) } catch (_: Throwable) { } }
        return paymentRow.optString("id")
        }
    }

    private fun removePaymentPending(paymentRowId: String) {
        val prefs = paymentPendingPrefs ?: return
        if (paymentRowId.isBlank()) return
        synchronized(LOCK) {
        val queue = loadPaymentPendingQueue()
        val kept = org.json.JSONArray()
        for (i in 0 until queue.length()) {
            val e = queue.optJSONObject(i) ?: continue
            if (e.optJSONObject("paymentRow")?.optString("id") != paymentRowId) kept.put(e)
        }
        prefs.edit().putString("queue", kept.toString()).commit()
        }
    }

    private fun loadPaymentPendingQueue(): org.json.JSONArray {
        val raw = paymentPendingPrefs?.getString("queue", "[]") ?: "[]"
        return try { org.json.JSONArray(raw) } catch (_: Exception) { org.json.JSONArray() }
    }

    /** TK-REPORTED BUG FIX (2026-07-16): retries every Advance/Treatment
     * payment still stuck on this device (see saveTreatmentPayment above).
     * Called from BottomNav.wire() on every screen open, same as Enquiry/
     * Registration's flushPending(). Safe to repeat even if part of a
     * previous attempt already succeeded: the bill update just re-sets the
     * same value, payments upsert is merge-duplicates by this row's own
     * fixed id (never creates a second row), and promoteFollowUpToTreatment
     * simply re-confirms stage="Treatment" if that already happened. Does
     * nothing (no network call at all) if nothing is pending. */
    /**
     * 🔴🔒 V715 (২৬.০৮.২০২৬) — `force` ঘরটা **নতুন, ঐচ্ছিক**, ডিফল্ট `false`।
     * তাই আগের প্রতিটা ডাক (`BottomNav.wire()` · `SyncWorker` · অন্য সব) এক
     * অক্ষরও না বদলেই চলে। মালিক/স্টাফ নিজে "Send" চাপলে (`PendingSyncStatus
     * .retryAllNow`) `force = true` যায় — তখন অপেক্ষা মানা হয় না, সঙ্গে সঙ্গে
     * চেষ্টা হয়, ঠিক আগের মতোই।
     */
    fun flushPending(force: Boolean = false) {
        val prefs = paymentPendingPrefs ?: return
        synchronized(LOCK) {
        val queue = loadPaymentPendingQueue()
        if (queue.length() == 0) return
        val stillPending = org.json.JSONArray()
        /* 🔵🔒 V717 — এই দফায় অপেক্ষার জন্য যাদের ছোঁয়া হলো না, তাদের তালিকা।
           নিচে দেখুন: এই দফায় অন্য কেউ সফল হলে (= নেট ভালো, প্রমাণিত) এদের
           অপেক্ষা মুছে দেওয়া হয়, যাতে নেট ফেরার সঙ্গে সঙ্গেই এরা সুযোগ পায়। */
        val skipped = mutableListOf<org.json.JSONObject>()
        var anySuccess = false
        for (i in 0 until queue.length()) {
            val e = queue.optJSONObject(i) ?: continue
            /* 🔴🔒 V715 — পরপর অনেকবার ব্যর্থ হওয়া সারিটা এই দফায় বাদ।
               ⛔ **সারিটা ফেলা হচ্ছে না** — হুবহু আগের মতোই `stillPending`-এ
                  রেখে দেওয়া হচ্ছে, শুধু এইবার নেটে হাত দেওয়া হচ্ছে না।
               ⛔ প্রথম দু'বার ব্যর্থতায় কোনো দেরি নেই (দুর্বল নেট অক্ষত)। */
            if (!PendingRetryBackoff.shouldTry(e, force)) { skipped.add(e); stillPending.put(e); continue }
            try {
                val paymentRow = e.optJSONObject("paymentRow") ?: continue
                // TK-REQUESTED (2026-07-26): if this payment was deleted in
                // the meantime, drop it instead of pushing it back into the
                // cloud. Falls through untouched when nothing was deleted.
                val rowId = paymentRow.optString("id", "")
                if (rowId.isNotBlank() && DeletedGuard.isDeleted("payments", rowId, context)) continue
                val patient = PatientBillInfo(
                    id = e.optString("patientId"),
                    name = e.optString("patientName"),
                    mobile = e.optString("patientMobile"),
                    branch = e.optString("patientBranch"),
                    patientId = e.optString("patientCode"),
                    bill = e.optDouble("effectiveBill", 0.0),
                    paid = 0.0,
                    billLocked = e.optBoolean("billLocked", false)
                )
                val why = StringBuilder()
                val ok = pushPaymentToCloud(
                    patient, e.optDouble("effectiveBill", 0.0), paymentRow,
                    e.optString("staffMobile"), e.optBoolean("sameDayRepeat", false), why
                )
                if (!ok) {
                    /* 🟢🔒 V706 — কেন আটকাল সেটা এই ফোনের তালিকার সারিতেই লিখে
                       রাখা, যাতে Dashboard-এর তালিকায় দেখা যায়।
                       ⛔ এটা শুধু ফোনের ভিতরের একটা বাড়তি ঘর — ক্লাউডে যায় না,
                          `pushPaymentToCloud` এই ঘরটা পড়েও না, তাই টাকার
                          হিসাবে বা পাঠানোর নিয়মে কোনো প্রভাব নেই। */
                    try { if (why.isNotEmpty()) e.put("lastWhy", why.toString()) } catch (_: Throwable) { }
                    // 🔴🔒 V715 — আবার ব্যর্থ; পরের চেষ্টার ফাঁক বাড়ানো হলো।
                    PendingRetryBackoff.noteFailure(e)
                    stillPending.put(e)
                } else { anySuccess = true; activateRmpCommissionAfterCloud(patient) }   // 🔵🔒 V717
            } catch (_: Throwable) {
                PendingRetryBackoff.noteFailure(e)   // 🔴🔒 V715
                stillPending.put(e)
            }
        }
        /* 🔵🔒 V717 — এই দফায় অন্তত একটা সারি সফল হয়েছে ⇒ **লাইন ভালো**।
           তাই যাদের শুধু অপেক্ষার জন্য ছোঁয়া হয়নি, তাদের অপেক্ষা মুছে দেওয়া
           হলো — পরের বারই তারা আবার চেষ্টা পাবে।
           ⛔ **এই দফায় যারা সত্যিই চেষ্টা করেও ব্যর্থ হয়েছে, তাদের অপেক্ষা
              মোছা হয় না** — নেট ভালো থাকা সত্ত্বেও ব্যর্থ মানে দোষটা নেটের নয়,
              তাই তাদের বারবার চেষ্টা করানোর কোনো মানে নেই (এটাই তো আসল সমস্যা
              ছিল — দিনে ~৪,০০০ বার)।
           ⛔ কোনো সারি ফেলা/বদলানো হয় না, শুধু অপেক্ষার হিসাব। */
        if (anySuccess) skipped.forEach { PendingRetryBackoff.clearWait(it) }
        prefs.edit().putString("queue", stillPending.toString()).commit()
        }
    }

    private fun promoteFollowUpToTreatment(patient: PatientBillInfo, staffMobile: String): Boolean {
        return try {
            val digits = patient.mobile.filter { it.isDigit() }.takeLast(10)
            // 🔴🔒 V715 — আগে `fetchList(...)` = `select=*` (রোগীর ছবিসহ ১০০ সারি)।
            // এই লুপ শুধু stage · status · id · lastRemark পড়ে — উপরের মন্তব্য দ্রষ্টব্য।
            /* 🧷🔒 V1286 (১০.০৯.২০২৬, TK-অনুমোদিত — তালিকা সারি ৪১১-④ দ্বিতীয় অংশ):
               SQL-এ মাপা ২৪ জোড়া followups সারির ৮টার আইডি `fu_pat_pat_<নম্বর>` —
               ঠিক নিচের ফলব্যাকের ছাঁচ। কারণ: এই খোঁজাটা নেট খারাপ হলে **ফাঁকা
               তালিকা** ফেরাত (`fetchListSlim`), অ্যাপ ভাবত "সারি নেই" আর নতুন
               সারি বানাত — অথচ আসল সারি ক্লাউডে ছিল ⇒ জোড়া।
               ⇒ এখন `…OrNull`: পড়া ব্যর্থ = `null` ⇒ **কিছুই বানানো হয় না**, `false`
                 ফেরে ⇒ টাকার সারি আগের মতোই "patient card pending" হয়ে জমা থাকে,
                 লাইন ফিরলে আবার চেষ্টা (`PatientTimelineRepository`-র প্রমাণিত নিয়ম)।
               ⛔ সফল পড়ায় (০টা সারিসহ) আচরণ এক অক্ষরও বদলায়নি। */
            val followups = SupabaseClient.fetchListSlimOrNull(
                "followups", "mobile=like.*$digits", 100, PROMOTE_COLS_SCAN) ?: return false
            var moved = 0
            for (i in 0 until followups.length()) {
                val row = followups.getJSONObject(i)
                val stage = row.s("stage")
                // Move any Visit(Patient)- or Treatment-stage follow-up for this
                // mobile forward. We match by mobile only (findByMobile already
                // scoped it); refId is NOT used to exclude, because a Visit
                // follow-up's refId often points to the enquiry/registration id,
                // not the patients-row id — the old refId guard silently skipped
                // it, so the bill/advance never reached the Patient card.
                if (stage != "Patient" && stage != "Treatment") continue
                // 🔒 TK-ORDER (30.07.2026 — পুরনো "রেকর্ড ফিরে আসা" বাগ-পরিবারের
                // পুনর্যাচাইয়ে পাওয়া): এই লুপ আগে status না দেখেই এগোত, তাই
                // আগে কখনো Reject/Incomplete করা একটা Patient/Treatment-stage
                // সারিও পেমেন্ট এলেই "Active" করে ফিরিয়ে আনতে পারত — ঠিক সেই
                // একই ধরনের বাগ (B34/B78/B96/B108/B141), শুধু নতুন একটা পথ।
                // ⛔ বন্ধ করা সারি বন্ধই থাকবে — moved==0 হলে নিচের ফলব্যাক
                // এমনিতেই একটা তাজা নতুন সারি বানিয়ে দেয়, তাই বিল/অ্যাডভান্স
                // হারাবে না।
                val rowStatus = row.s("status")
                if (rowStatus.equals("Cancelled", true) || rowStatus.equals("Incomplete", true) ||
                    rowStatus.equals("Rejected", true) || rowStatus.equals("Closed", true)) continue
                val id = row.s("id")
                if (id.isBlank()) continue
                // TK-REPORTED BUG FIX (2026-07-15): "date" was never updated on
                // repeat payments, so a patient's card stayed buried wherever
                // their FIRST advance payment date put them — scrolling to find
                // today's paying patients was hard. Every payment now bumps this
                // record's date to today, so it sorts to the very top (same
                // "newest first" rule the whole list already uses).
                // 🔒 V236 (TK, 01.08.2026 — সমস্যা-৩): টাকা নিলে এতদিন এই লাইনটা
                // রোগীর আসল Treatment Progress মুছে জোর করে "Treatment payment /
                // Advance received" বসিয়ে দিত — Chamber Date-এর Progress ঘরে ঠিক
                // ওটাই দেখাত, আর নতুন লেখা টাকা নিলেই আবার মুছে যেত। এখন: আগের
                // lastRemark মানুষের সত্যিকারের লেখা হলে সেটাই রাখা হয়; শুধু ফাঁকা
                // বা অ্যাপের নিজের auto-label হলে তখনই label বসে (PaymentModel.
                // isAutoPaymentRemark — Report Card/Timeline-ও একই নিয়ম মানে)।
                // ⛔ stage/status/date/টাকার অঙ্ক/বিল/advance/হিসাব কিছুই বদলায়নি —
                //    শুধু lastRemark আর জোর করে মোছে না (additive-safe)।
                val existingRemark = row.s("lastRemark")
                val fields = JSONObject()
                    .put("stage", "Treatment")
                    .put("status", "Active")
                    .put("date", PatientIdGenerator.todayIso())
                    .put("updatedAt", isoNow())
                if (PaymentModel.isAutoPaymentRemark(existingRemark, "")) {
                    fields.put("lastRemark", "Treatment payment / Advance received")
                }
                if (SupabaseClient.updateById("followups", id, fields)) moved++
            }
            // No Visit/Treatment follow-up existed yet (e.g. the card came from an
            // enquiry-derived / virtual row) — create the Patient-tab record so the
            // bill + advance still show up, mirroring web saveVisitAdvancePayment().
            if (moved == 0) {
                /* 🔴🔒🔒 V881 (৩০.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — SHAHARYA ও
                   GOURANGO BARMAN-এর কার্ডে Patient ID উধাও):
                   TK: *"ভবিষ্যতে যেন এরকম ফালতু কাজ অ্যাপ নিজে থেকে না করে"*

                   **আসল দোষ (কোড ধরে যাচাই, ডেটাবেসেও মিলিয়ে দেখা):** এই
                   ফলব্যাক সারিটার আইডি ছিল **এলোমেলো** (`fu_<random>`), আর
                   `patientId` ঘরটা বসানোই হতো না। ফলে —
                     · উপরের খোঁজা কোনো কারণে ফসকালে (লাইন খারাপ) **প্রতিবার
                       নতুন একটা সারি** জমত ⇒ একই ধাপে দুটো-তিনটে সারি
                       (৩০.০৮.২০২৬-এ মাপা: **৮৭ জনের** এমন হয়েছিল),
                     · আর ওই সারিতে আইডি না থাকায় কার্ডে আইডির বদলে তারিখ
                       দেখাত (TK-এর ছবি)।

                   **এখন:** আইডিটা **স্থির** — `fu_pat_<রোগীর সারির আইডি>`।
                   এটা প্রকল্পের নিজের প্রমাণিত নিয়ম (ওয়েবের B626/V406 ও
                   `PatientTimelineRepository` হুবহু এটাই ব্যবহার করে), তাই
                   যতবারই চলুক **একই সারিতেই বসে — নতুন সারি আর জমে না**।
                   সঙ্গে `patientId` ও `patientId`-এর তারিখও বসে, তাই কার্ডে
                   আইডি কখনো ফাঁকা যাবে না।
                   ⛔ টাকার হিসাব · বিল · advance — কিছুই বদলায়নি। */
                val fuId = "fu_pat_" + patient.id
                val tr = JSONObject()
                    .put("id", fuId).put("refId", patient.id)
                    .put("patientId", patient.patientId)
                    .put("mobile", patient.mobile).put("name", patient.name).put("branch", patient.branch)
                    .put("stage", "Treatment").put("status", "Active")
                    .put("date", PatientIdGenerator.todayIso())
                    .put("lastRemark", "Advance Payment received")
                    .put("createdAt", isoNow()).put("updatedAt", isoNow())
                if (!SupabaseClient.upsert("followups", tr)) return false
                moved = 1
            }

            // TK-REPORTED BUG FIX (2026-07-16): mirrors closeSourceEnquiry() in
            // RegistrationRepository -- close every lingering Inquiry-stage
            // follow-up for this mobile now that the patient has reached
            // Treatment. Without this, a patient who never went through the
            // Registration screen (advance taken directly) kept an old
            // "Active" Enquiry entry forever, even after being marked
            // Incomplete in Treatment.
            // FURTHER BUG FIX (2026-07-16): this step used to swallow its own
            // network failures silently WITHOUT that affecting the return
            // value below -- so if this step alone failed, the overall
            // payment would still be reported "fully successful" and removed
            // from the V78 retry queue, meaning this cleanup step could never
            // actually be retried. Now its success is required too, so a
            // failure here keeps the whole payment queued and this step gets
            // retried along with everything else next time.
            var closeOk = true
            try {
                // 🔴🔒 V715 — আগে `select=*` (ছবিসহ, ৫০০০ পর্যন্ত)। এই লুপ শুধু
                // id + PatientIdentity-র তিনটে ঘর (name · patientId · refId) পড়ে।
                /* 🔴🔒 V953 (০১.০৯.২০২৬, TK-নির্দেশ "খুব সাবধানে") — **আসল কারণ,
                   যাচাই করা:** এখানে এতদিন শুধু `stage=eq.Inquiry` সারিগুলো বন্ধ
                   হত। রোগী Advance দিয়ে Treatment-এ গেলেও তাঁর পুরনো
                   **`Patient`-stage সারিটা কখনো বন্ধ হত না** — তাই তিনি
                   Follow-up-এর "Visit" তালিকায় চিরকাল ভেসে থাকতেন
                   (TK-এর FAIZ ANWAR-এর অভিযোগ; ডেটাবেসে ১৮৯টা এমন সারি ছিল)।
                   **এখন:** Inquiry **ও** Patient — দুটোই বন্ধ হয়।
                   ⛔ কোনো সারি মোছা হয় না, শুধু `status=Closed` — ইতিহাস অক্ষত।
                   ⛔ নিচের `provablyOtherPatient` পাহারা আগের মতোই — এক নম্বরে
                      দু'জন থাকলে অন্যজনের সারিতে হাত পড়ে না।
                   ⛔ ব্যর্থ হলে আগের মতোই `closeOk=false` ⇒ পেমেন্ট সারিতে
                      থেকে যায় ও পরে আবার চেষ্টা হয়। */
                val inquiries = SupabaseClient.fetchListSlim(
                    "followups", "mobile=like.*$digits&stage=in.(Inquiry,Patient)&status=eq.Active", 5000,
                    PROMOTE_COLS_INQUIRY
                )
                for (i in 0 until inquiries.length()) {
                    val row = inquiries.getJSONObject(i)
                    val id = row.s("id")
                    if (id.isBlank()) continue
                    /* 🔵🔒 V536: এক নম্বরে দু'জন থাকলে **অন্যজনের Inquiry বন্ধ হবে না**।
                       ⛔ প্রমাণ না থাকলে আগের মতোই — কোনো সারি বাদ পড়ে না। */
                    if (PatientIdentity.provablyOtherPatient(
                            row, digits, patient.id, patient.patientId, patient.name)) continue
                    /* 🔴 V953 — Inquiry সারির ক্ষেত্রে আগের মতোই stage="Registered"
                       বসে (পুরনো আচরণ এক অক্ষরও বদলায়নি)। Patient সারির
                       stage **ছোঁয়াই হয় না** — শুধু বন্ধ করা হয়, নইলে
                       রোগীর নিজের ইতিহাসের ধাপটা হারিয়ে যেত। */
                    val wasInquiry = row.s("stage").equals("Inquiry", true)
                    val fields = JSONObject()
                        .put("status", "Closed")
                        .put("nextFollow", "")
                        .put("convertedPatientId", patient.patientId)
                        .put("lastRemark", "Converted to Patient / Treatment")
                        .put("updatedAt", isoNow())
                    if (wasInquiry) fields.put("stage", "Registered")
                    /* 🔴🔒 V1205 (০৮.০৯.২০২৬, TK-রিপোর্ট: *"১২টা পেমেন্ট বার বার সেন্ড
                       করছি কিন্তু সেন্ড কেনো হচ্ছে না"*) — **আসল দোষ, কোডে মেপে পাওয়া:**
                       এই পুরনো-সারি-বন্ধ করার ধাপটা ব্যর্থ হলে (একটা সারিও) পুরো
                       পেমেন্টটা "ব্যর্থ" ধরা হত ⇒ **টাকা ক্লাউডে পৌঁছে যাওয়ার পরেও**
                       সারিটা অপেক্ষমাণ তালিকা থেকে কোনোদিন উঠত না, "send" চাপলেও নয়।
                       ⇒ এখন ব্যর্থ হলে কাজটা **হারায় না** — প্রকল্পের নিজের প্রমাণিত
                         `GenericUpdateQueue`-এ জমা থাকে, নেট ফিরলে নিজে থেকেই বসে।
                       ⛔ ২০২৬-০৭-১৬-এর নিয়মের **উদ্দেশ্য অটুট** ("ধাপটা যেন আবার চেষ্টা
                          পায়") — শুধু টাকার সারিকে আর জিম্মি করে রাখে না। */
                    if (!SupabaseClient.updateById("followups", id, fields)) {
                        var queued = false
                        try {
                            context?.let { GenericUpdateQueue.queue(it, "followups", id, fields); queued = true }
                        } catch (_: Throwable) { }
                        if (!queued) closeOk = false
                    }
                }
            } catch (_: Exception) { closeOk = false }

            // Verify the Patient-tab record is actually readable before reporting success.
            // 🔴🔒 V715 — আগে `select=*` (ছবিসহ)। এখানে শুধু **গোনা** হয়
            // (`verify.length()`), একটাও ঘর পড়া হয় না — তাই `id`-ই যথেষ্ট।
            /* 🔴🔒 V1205 — এটা শুধু একটা **পড়া**। লাইন খারাপ হলে বা ছাঁকনি না
               মিললে ০ সারি ফেরে, অথচ উপরের **লেখাটা সফলই হয়েছে** (২০০ পেয়েছে)।
               আগে সেই ০ দেখে পুরো পেমেন্ট "ব্যর্থ" ধরা হত ⇒ চিরকাল আটকে থাকত।
               ⇒ এখন লেখা সফল (`moved > 0`) হলে পড়ার ব্যর্থতা আর আটকায় না।
               ⛔ লেখাই না হলে (`moved == 0`) আগের মতোই ব্যর্থ — টাকা কখনো
                  ভুল করে "হয়ে গেছে" ধরা হবে না। */
            moved > 0 && closeOk
        } catch (_: Exception) {
            false
        }
    }

    /** TK-REQUESTED (2026-07-22): correct the Total Bill on its own (no new
     *  payment). Anyone may do it, but WHO edited it is logged into the
     *  patient's Follow-up history (old -> new + staff name), so every bill
     *  change is traceable. Best-effort audit: the bill still updates even if
     *  the history note can't be written. */
    fun updateBillOnly(patient: PatientBillInfo, newBill: Double, oldBill: Double, staffMobile: String, staffName: String): Boolean {
        if (newBill <= 0 || newBill > BILL_TYPO_GUARD_MAX) return false
        // 🔒 TK'S LOCKED RULE (27.07.2026): "Bill" is named first in TK's rule --
        // only the patient's own branch (staff or doctor) or Master may set or
        // correct it. Same guard as saveTreatmentPayment.
        context?.let { ctx ->
            if (!MoneyBranchGuard.canTakeMoney(ctx, patient.branch, patient.patientId)) return false
        }
        val ok = SupabaseClient.updateById(
            "patients", patient.id,
            JSONObject().put("bill", newBill).put("updatedAt", isoNow())
        )
        // Update the local patients cache right away so Bill/Due reflect it.
        // 🔴🔒 V1311 (তালিকা ৪২৩): ক্লাউডে বসে গেলে SYNCED — নইলে PENDING (retry-র জন্য)।
        context?.let {
            LocalWorkflowStore(it).upsertPatient(
                JSONObject()
                    .put("id", patient.id)
                    .put("mobile", patient.mobile)
                    .put("name", patient.name)
                    .put("branch", patient.branch)
                    .put("patientId", patient.patientId)
                    .put("bill", newBill)
                    .put("updatedAt", isoNow()),
                if (ok) "SYNCED" else "PENDING"
            )
        }
        // Audit: log who corrected the bill into the Follow-up history.
        try {
            val digits = patient.mobile.filter { it.isDigit() }.takeLast(10)
            val fu = SupabaseClient.findByMobile("followups", "+91$digits", "id", 1)
            if (fu.length() > 0) {
                val fid = fu.getJSONObject(0).optString("id", "")
                if (fid.isNotBlank()) {
                    val note = "💰 Bill corrected ₹${"%,.0f".format(oldBill)} → ₹${"%,.0f".format(newBill)} by $staffName"
                    // 💰 V1403 — এটা অ্যাপের নোট, স্টাফের কল নয় (আগে কল ধরে "আজ কল করুন" বসত)
                    FollowUpRepository(context).updateRemark(fid, note, staffName, systemNote = true)
                }
            }
        } catch (_: Exception) { }
        return ok
    }

    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
}
