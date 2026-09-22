package com.tkbiswas.pilesclinic.native

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native rebuild -- Draft (Master/Staff). Mirrors web draffHome()'s five
 * categories, computed from live Supabase data:
 *   received     : all enquiries (My Enquiry, All Branch)
 *   enqReject    : Inquiry-stage follow-ups marked Cancelled (Reject)
 *   visitReject  : Visit-stage (stage "Patient") follow-ups marked Cancelled
 *   notComplete  : Patient-stage (stage "Treatment") follow-ups marked Incomplete
 *   complete     : patients with a bill fully paid (bill>0, due==0)
 *
 * Each entry carries its follow-up id + origin tab so a Restore returns it to
 * the exact section it came from (web restoreDraftEntry), and its date so the
 * Custom-date / Monthly filter can narrow the list.
 */
data class DraftEntry(
    val name: String,
    val mobile: String,
    val extra: String,
    val branch: String = "",
    val disease: String = "",
    val stage: String = "",
    val patientId: String = "",
    val id: String = "",
    val tab: String = "",
    val recordDate: String = "",
    val nextFollow: String = "",
    val lastRemark: String = "",
    // TK-REQUESTED ADDITION (2026-07-18): who received/created this record —
    // needed for the same-day self-delete permission check (TrashHelper).
    // Blank for any entry type that doesn't need it; nothing else affected.
    val createdByMobile: String = "",
    // 🟢🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ — "একই কার্ড সব জায়গায়, ঝুঁকি ছাড়া")
    // — My Enquiry/Unexpected Time/Incomplete/Complete/Running Patient
    // এখন Follow-up-এর কার্ডের মতোই দেখাবে (FollowUpAdapter পুনর্ব্যবহার,
    // FollowUpActivity-এর নিজস্ব কোড ছোঁয়া হয়নি)। এই ঘরগুলো সেই কার্ডের
    // জন্যই লাগে — ডিফল্ট ফাঁকা/০, তাই পুরনো buckets (Enquiry Reject ইত্যাদি,
    // যেগুলো বদলায়নি) এক অক্ষরও প্রভাবিত হয় না।
    val status: String = "",
    val bill: Double = 0.0,
    val paid: Double = 0.0,
    // 🏷️🔒 V1508 (১৬.০৯.২০২৬, TK-নির্দেশ — MD ANARUL HOWK): আসল বিল (ছাড়ের
    // আগে), শুধু কার্ডের "Bill" চিপে দেখানোর জন্য। 0.0 ⇒ কখনো ছাড় দেওয়া হয়নি,
    // চিপ তখন আগের মতোই `bill` দেখায়। ⛔ Due/% হিসাব `bill`-ই ব্যবহার করে, অপরিবর্তিত।
    val billBeforeDiscount: Double = 0.0,
    val callCount: Int = 0,
    val timeType: String = "",
    val address: String = "",
    val age: String = "",
    val sex: String = "",
    val refId: String = "",
    /* 🆕🔒 V850 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — TK: "Last call
       Date & Time staff নাম এখানে কেন আসে না"।
       **আসল কারণ (কোড ধরে যাচাই, আন্দাজ নয়):** Draft-এর পাঁচটা "জীবন্ত"
       তালিকা কার্ডে তথ্য পাঠায় এই DraftEntry বাক্সে ভরে (`toFollowUpItem`)।
       এই বাক্সে **LAST CALL-এর তিনটে ঘরই ছিল না**, তাই ডেটাবেসে যা-ই থাক
       কার্ডে সব সময় `LAST CALL —` দেখাত।
       ⛔ তথ্যগুলো আগে থেকেই ফোনে নেমে আসে (`FOLLOWUP_COLS_NO_PHOTO`-এ
          `history` ও `lastCallDate`, `PATIENT_COLS_NO_PHOTO`-এ
          `registrationDate` ও `registeredBy`) — **একটাও নতুন ক্লাউড-পড়া নেই**। */
    val lastCallDate: String = "",
    val lastCallBy: String = "",
    val lastCallTime: String = "",
    /* TK: "যেগুলো রেজিস্ট্রেশন করা হয়েছে সেখানে লিখতে হবে কত তারিখে
       রেজিস্ট্রেশন হয়েছে এবং কে রেজিস্ট্রেশন করেছিল"। */
    val regDate: String = "",
    val regBy: String = "",
    /* 🆕🔒 V852 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — TK: "return visit /
       refund এরকম কোনো ব্যাপার থাকলে ওই ব্যক্তির পাশে ফার্স্ট ব্র্যাকেটের
       মধ্যে মেনশন থাকবে" · "তালিকায় যা দেখাবে গোনাও যেন একই হয়"।
       মান: "" · "Return Visit" · "Refund"। */
    val regTag: String = "",
    /* 💰🔒 V1209 (০৮.০৯.২০২৬, TK-নির্দেশ): *"কত কত তারিখে কত কত জমা করেছে
       সেগুলিও লাগবে"*। এক লাইনে সাজানো — "02/01/2026 ₹5,000 | 10/02/2026 ₹3,000"।
       ⛔ ডিফল্ট ফাঁকা ⇒ যে তালিকা এটা ভরে না, সেখানে কিছুই বদলায় না।
       ⛔ **নতুন কোনো ক্লাউড-পড়া লাগেনি** — টাকার সারিগুলো এই ফাংশনেই
          আগে থেকে নেমে আসে (`paidByMobile`-এর একই লুপ)। */
    val payHistory: String = "",
    /* 🩺🔒 V1210 (০৮.০৯.২০২৬, TK-নির্দেশ): *"তাছাড়া Ref by লাগবে"*।
       রোগীর সারিতেই আছে (`refBy` · `refDoctor`) — নতুন কোনো ক্লাউড-পড়া নেই। */
    val refByText: String = ""
) : java.io.Serializable

/**
 * 🟢🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ — "একই কার্ড সব জায়গায়, ঝুঁকি ছাড়া") —
 * Draft-এর My Enquiry/Unexpected Time/Incomplete/Complete/Running Patient
 * এখন Follow-up-এর কার্ডের মতোই দেখায় ও কাজ করে — কিন্তু FollowUpActivity-এর
 * নিজের কোড (`buildFollowCard`) একদম ছোঁয়া হয়নি (ঝুঁকি নেওয়া হয়নি, TK-এর
 * স্পষ্ট নির্দেশে)। বদলে, প্রজেক্টে আগে থেকেই থাকা, স্বাধীন `FollowUpAdapter`
 * (item_followup_card.xml, এখন শুধু Trash-প্রিভিউতে ব্যবহৃত) পুনর্ব্যবহার
 * করা হচ্ছে — এই ফাংশনটা তারই জন্য DraftEntry-কে FollowUpItem-এ রূপান্তর করে।
 * ⛔ ছবি (photo) ইচ্ছাকৃতভাবে বাদ — ফ্রি-প্ল্যানে বাড়তি খরচ এড়াতে (TK-অনুমোদিত)।
 */
fun DraftEntry.toFollowUpItem(): FollowUpItem = FollowUpItem(
    id = id,
    name = name,
    mobile = mobile,
    branch = branch,
    disease = disease,
    stage = stage,
    lastRemark = lastRemark,
    nextFollow = nextFollow,
    recordDate = recordDate,
    callCount = callCount,
    bill = bill,
    paid = paid,
    billBeforeDiscount = billBeforeDiscount,   // 🏷️ V1508
    patientId = patientId,
    address = address,
    age = age,
    sex = sex,
    photo = "",
    timeType = timeType,
    refId = refId,
    // 🆕 V850 — এই পাঁচটা ঘর না পাঠালে কার্ডের লাইনটা চিরকাল `—` থাকত।
    lastCallDate = lastCallDate,
    lastCallBy = lastCallBy,
    lastCallTime = lastCallTime,
    regDate = regDate,
    regBy = regBy
)

data class DraftBuckets(
    val received: List<DraftEntry>,
    val enqReject: List<DraftEntry>,
    val visitReject: List<DraftEntry>,
    val notComplete: List<DraftEntry>,
    val complete: List<DraftEntry>,
    // TK-REQUESTED ADDITION (2026-07-18): every Enquiry saved with Call
    // Timing = "Unexpected Time" (staff took the call outside official
    // hours), so TK can see at month-end how many such calls came in and
    // how many of those numbers actually converted to a real patient/
    // treatment -- to decide staff incentive pay. Purely additive; the
    // other 5 buckets and their screens are untouched.
    val unexpectedTime: List<DraftEntry>,
    // 🔴 TK-নির্দেশ (02.08.2026): Advance/Treatment টাকা সম্পূর্ণ Refund হয়ে
    // গেলে (Approved Refund আছে ও নেট জমা ঠিক ₹0) সেই রোগী আর Enquiry/Visit/
    // Patient কোনো কার্ডেই থাকে না — এই নতুন ঘরে (রেকর্ড অক্ষত, শুধু কার্ড
    // সরে)। পরে আবার টাকা জমা দিলে (নেট জমা আর ০ থাকবে না) স্বয়ংক্রিয়ভাবেই
    // এই ঘর থেকে সরে আবার Patient কার্ডে ফিরে যাবে (স্থির-সংরক্ষিত কোনো
    // status নয়, প্রতিবার লাইভ হিসাব করে বার করা হয়)।
    val refunded: List<DraftEntry> = emptyList(),
    // 🟢🔒 V621 (২৪.০৮.২০২৬, TK-নির্দেশ) — Fees Return করা Visit-এর জন্য
    // সম্পূর্ণ নতুন, আলাদা তালিকা। ⛔ ডিফল্ট খালি লিস্ট — যেখানেই
    // `DraftBuckets(...)` আগে থেকে বানানো হতো (এই ঘর ছাড়াই), সেগুলো
    // এখনো ঠিকভাবে কম্পাইল/চলে, শুধু এই ঘর খালি থাকবে।
    val returnVisit: List<DraftEntry> = emptyList(),
    // 🟢🔒 V644 (২৫.০৮.২০২৬, TK-নির্দেশ, দুই দফা প্রশ্ন করে নিশ্চিত হওয়া) —
    // "Running Patient" — যাদের চিকিৎসা এখন চলমান (stage == "Treatment",
    // স্বাভাবিক active অবস্থা — Incomplete/Cancelled/Returned নয়, ওগুলো
    // আগে থেকেই আলাদা bucket-এ ধরা পড়ে)। PATIENT সেকশনে সবচেয়ে উপরে,
    // Incomplete Patient-এর ঠিক আগে বসে (TK-নির্দিষ্ট ক্রম)। ⛔ ডিফল্ট
    // খালি লিস্ট — পুরনো কোনো `DraftBuckets(...)` কল এই ঘর ছাড়াই ঠিকভাবে
    // চলে, শুধু এই ঘর খালি থাকবে।
    val runningTreatment: List<DraftEntry> = emptyList(),
    // 📊🔒 V824 (২৯.০৮.২০২৬, TK-নির্দেশ) — "Yearly Registration": চলতি বছরে
    // (০১ জানুয়ারি → ৩১ ডিসেম্বর) **বাছা ব্রাঞ্চে** যত জনের সত্যিকারের
    // রেজিস্ট্রেশন হয়েছে। ⛔ শুধু মাস্টারের পর্দায় দেখা যায়।
    // ⛔ Draft-এর উপরের All/This Month/Custom ছাঁকনি এই ঘরে **লাগে না** —
    //    এটা সবসময় পুরো বছরের হিসাব (TK-এর স্পষ্ট নির্দেশ)।
    // ⛔ ডিফল্ট খালি — পুরনো কোনো `DraftBuckets(...)` কল ভাঙে না।
    val yearlyReg: List<DraftEntry> = emptyList(),
    /* 🆕🔒 V852 — Yearly Registration-এ যারা **তালিকাতেও নেই, গোনাতেও নেই**:
       নামে DEMO/TEST · রেজিস্ট্রেশনের তারিখ নেই। পর্দার উপরে ছোট করে দেখানো
       হয় যাতে TK নিজেই বুঝতে পারেন কেউ চুপচাপ বাদ পড়ছে কিনা। */
    val yearlyOutDemo: Int = 0,
    val yearlyOutNoDate: Int = 0,
    /* 🆕🔒 V-YEARSW (১৬.০৯.২০২৬, TK-অনুমোদিত) — Yearly Registration পর্দায়
       বছর-বদলের বোতামে কোন কোন বছর দেখানো যাবে (আসলেই তথ্য আছে এমন বছর,
       নতুন থেকে পুরনো সাজানো, চলতি বছর সবসময় থাকে)। ⛔ ডিফল্ট খালি —
       পুরনো কোনো `DraftBuckets(...)` কল ভাঙে না। */
    val yearlyAvailableYears: List<String> = emptyList()
)

class DraftRepository(private val context: Context? = null) {

    companion object {
        /* 💸 V1281 — Draft-এর delta-ঘড়ি: পূর্ণ পড়া ৩ ঘণ্টায় (Follow-up · Chamber · Dashboard-এর একই মাপ)। */
        private const val DRAFT_DELTA_PREFS = "draft_delta_state"
        private const val DRAFT_FULL_REFRESH_INTERVAL_MS = 3L * 60L * 60L * 1000L
        private const val DRAFT_SAFETY_BACK_MS = 5_000L
    }

    // TK-REQUESTED ADDITION (2026-07-20): same "show what was already on the
    // phone instantly" pattern added to the other screens today. load()
    // below (fetch/merge/bucket logic) is completely unchanged except for
    // saving into this cache right before it returns.
    private val CACHE_PREFS = "draft_buckets_cache"

    private fun cacheKey(branchFilter: String?, from: String?, to: String?) =
        "cache_${branchFilter ?: "All"}_${from ?: "-"}_${to ?: "-"}"

    private fun serializeEntries(list: List<DraftEntry>): org.json.JSONArray {
        val arr = org.json.JSONArray()
        for (e in list) {
            arr.put(
                org.json.JSONObject()
                    .put("name", e.name).put("mobile", e.mobile).put("extra", e.extra)
                    .put("branch", e.branch).put("disease", e.disease).put("stage", e.stage)
                    .put("patientId", e.patientId).put("id", e.id).put("tab", e.tab)
                    .put("recordDate", e.recordDate).put("nextFollow", e.nextFollow)
                    .put("lastRemark", e.lastRemark).put("createdByMobile", e.createdByMobile)
                    /* 🔴🔒 V741 (২৭.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — "বিল ক্লিয়ার হয়ে
                       গেছে তাহলে এখানে 0% কেন দেখাচ্ছে, বিলও লেখা নাই")।
                       **আসল কারণ (কোড ধরে, আন্দাজ নয়):** V646-এ কার্ডের জন্য
                       ৯টা নতুন ঘর যোগ হয়েছিল (bill · paid · refId ইত্যাদি),
                       কিন্তু **এই জমা-করার তালিকায় সেগুলো যোগ করা হয়নি**।
                       ফলে ফোনে জমানো তালিকা থেকে দেখালে টাকার ঘর ফাঁকা (০)
                       আসত ⇒ Bill ₹0 · Due ₹0 · 0%। "Paid 30600" লেখাটা টিকে
                       থাকত, কারণ ওটা `extra` ঘরে — সেটা জমা হতো।
                       ⛔ শুধু **যোগ** করা হলো; পুরনো একটা ঘরও বদলায়নি।
                       ⛔ পুরনো জমানো তালিকায় এই ঘরগুলো নেই — তখন আগের মতোই
                          ০ আসবে, তারপর মেঘ থেকে এলেই ঠিক হয়ে যাবে (খারাপ হয় না)। */
                    .put("status", e.status).put("bill", e.bill).put("paid", e.paid)
                    .put("callCount", e.callCount).put("timeType", e.timeType)
                    .put("address", e.address).put("age", e.age).put("sex", e.sex)
                    .put("refId", e.refId)
                    /* 🆕 V850 — জমানো তালিকাতেও এই পাঁচটা ঘর লিখতে হবে, নইলে
                       পর্দা খোলার সঙ্গে সঙ্গে (ক্যাশ থেকে) লাইনটা আবার ফাঁকা
                       দেখাত, আর লাইন খারাপ থাকলে চিরকালই। (পাহারা §৯.২৩) */
                    .put("lastCallDate", e.lastCallDate)
                    .put("lastCallBy", e.lastCallBy)
                    .put("lastCallTime", e.lastCallTime)
                    .put("regDate", e.regDate)
                    .put("regBy", e.regBy)
                    .put("payHistory", e.payHistory)   // 💰 V1209 (পাহারা §৯.২৩)
                    .put("refByText", e.refByText)     // 🩺 V1210 (পাহারা §৯.২৩)
                    .put("regTag", e.regTag)   // 🆕 V852 (পাহারা §৯.২৩)
                    .put("billBeforeDiscount", e.billBeforeDiscount)   // 🏷️ V1508 (পাহারা §৯.২৩)
            )
        }
        return arr
    }

    private fun deserializeEntries(arr: org.json.JSONArray): List<DraftEntry> {
        val list = mutableListOf<DraftEntry>()
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            list.add(
                DraftEntry(
                    name = r.optString("name", ""), mobile = r.optString("mobile", ""),
                    extra = r.optString("extra", ""), branch = r.optString("branch", ""),
                    disease = r.optString("disease", ""), stage = r.optString("stage", ""),
                    patientId = r.optString("patientId", ""), id = r.optString("id", ""),
                    tab = r.optString("tab", ""), recordDate = r.optString("recordDate", ""),
                    nextFollow = r.s("nextFollow"), lastRemark = r.s("lastRemark"),   // 🔴🔒 V696
                    createdByMobile = r.optString("createdByMobile", ""),
                    // 🔴🔒 V741 — উপরের `serializeEntries`-এর জোড়া। এগুলো না
                    // পড়লে কার্ডে টাকা ও ➡️ বোতামের রোগী-আইডি (`refId`) হারাত।
                    status = r.optString("status", ""),
                    bill = r.optDouble("bill", 0.0),
                    paid = r.optDouble("paid", 0.0),
                    callCount = r.optInt("callCount", 0),
                    timeType = r.optString("timeType", ""),
                    address = r.optString("address", ""),
                    age = r.optString("age", ""),
                    sex = r.optString("sex", ""),
                    refId = r.optString("refId", ""),
                    // 🆕 V850 — উপরের `serializeEntries`-এর জোড়া (পাহারা §৯.২৩)।
                    lastCallDate = r.optString("lastCallDate", ""),
                    lastCallBy = r.optString("lastCallBy", ""),
                    lastCallTime = r.optString("lastCallTime", ""),
                    payHistory = r.optString("payHistory", ""),   // 💰 V1209 (পাহারা §৯.২৩)
                    refByText = r.optString("refByText", ""),     // 🩺 V1210 (পাহারা §৯.২৩)
                    regDate = r.optString("regDate", ""),
                    regBy = r.optString("regBy", ""),
                    regTag = r.optString("regTag", ""),   // 🆕 V852
                    billBeforeDiscount = r.optDouble("billBeforeDiscount", 0.0)   // 🏷️ V1508 (পাহারা §৯.২৩)
                )
            )
        }
        return list
    }

    // myMobile ডিফল্ট ফাঁকা — তাই পুরনো কোনো ডাক ভাঙে না; ফাঁকা হলে
    // মেশানোর ধাপটা এমনিতেই বাদ যায় (আগের ব্যবহার হুবহু এক)।
    fun loadCachedBuckets(branchFilter: String?, from: String?, to: String?, myMobile: String = ""): DraftBuckets? {
        val ctx = context ?: return null
        val json = ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).getString(cacheKey(branchFilter, from, to), null) ?: return null
        return try {
            val obj = org.json.JSONObject(json)
            // 🔒 V215 (§18): জমানো (cache) তালিকা আগে থেকেই bucket করা থাকে, তাই
            // সদ্য-Delete-করা রোগীর tombstoned সারি instant-open-এ ক্ষণিকের জন্য
            // ফিরে আসতে পারত। এখানেও DeletedGuard মেনে বাদ দেওয়া হয় — fresh load()
            // যেমন বাদ দেয় ঠিক তেমনই, যাতে flash-back না হয়।
            fun dropDeleted(list: List<DraftEntry>): List<DraftEntry> =
                list.filter { e ->
                    try { !DeletedGuard.isDeleted("followups", e.id, ctx) } catch (_: Throwable) { true }
                }
            DraftBuckets(
                received = deserializeEntries(obj.getJSONArray("received")),
                enqReject = dropDeleted(deserializeEntries(obj.getJSONArray("enqReject"))),
                visitReject = dropDeleted(deserializeEntries(obj.getJSONArray("visitReject"))),
                notComplete = dropDeleted(deserializeEntries(obj.getJSONArray("notComplete"))),
                complete = deserializeEntries(obj.getJSONArray("complete")),
                unexpectedTime = deserializeEntries(obj.getJSONArray("unexpectedTime")),
                // পুরনো cache-এ এই চাবি নাও থাকতে পারে (নতুন ফিচার) — optJSONArray
                // দিয়ে নিরাপদে ফাঁকা তালিকা ধরা হয়, পুরনো cache ভেঙে যায় না।
                refunded = deserializeEntries(obj.optJSONArray("refunded") ?: org.json.JSONArray()),
                // 🟢🔒 V621 — একই নিরাপদ ধরন, পুরনো cache-এ এই চাবি নেই তো খালি।
                returnVisit = deserializeEntries(obj.optJSONArray("returnVisit") ?: org.json.JSONArray()),
                // 🟢🔒 V644 — একই নিরাপদ ধরন, পুরনো cache-এ এই চাবি নেই তো খালি।
                runningTreatment = dropDeleted(deserializeEntries(obj.optJSONArray("runningTreatment") ?: org.json.JSONArray())),
                // 📊🔒 V824 — একই নিরাপদ ধরন, পুরনো cache-এ এই চাবি নেই তো খালি।
                yearlyReg = deserializeEntries(obj.optJSONArray("yearlyReg") ?: org.json.JSONArray()),
                yearlyOutDemo = obj.optInt("yearlyOutDemo", 0),    // 🆕 V852
                yearlyOutNoDate = obj.optInt("yearlyOutNoDate", 0),
                // 🆕🔒 V-YEARSW — পুরনো cache-এ এই চাবি নেই তো শুধু চলতি বছর থাকবে।
                yearlyAvailableYears = (obj.optJSONArray("yearlyAvailableYears") ?: org.json.JSONArray())
                    .let { arr -> (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() } }
                    .ifEmpty { listOf(YearlyRegistration.currentYear()) }
            ).let { if (myMobile.isBlank()) it else mergeOwnPhoneEnquiries(it, myMobile) }
        } catch (t: Throwable) { null }
    }

    /**
     * ⚡ TK-এর স্থায়ী নিয়ম (28.07.2026 ২.৩০ pm): **"আমি আমার ফোনে যা যা কাজ
     * করবো, সেটা যেন সাথে সাথেই দেখায়।"**
     *
     * ক্লাউড থেকে আনার পথে এই ফোনের নিজের এনকোয়ারি আগেই মেশানো হত, কিন্তু
     * **জমানো তালিকা** দেখানোর পথে হত না — তাই ধীর লাইনে নিজের করা এনকোয়ারি
     * "My Enquiry (All Branch)"-তে দেরিতে আসত। এখন দুই পথেই মেশে।
     *
     * ⛔ কোনো নতুন ক্লাউড-কল নেই · কোনো সারি বাদ যায় না — শুধু যোগ হয়।
     * "My Enquiry" ঘরটাই ব্যবহারকারীর নিজের ঘর, তাই এখানে ব্রাঞ্চ দেখা হয় না
     * (আগের নিয়মের সঙ্গে হুবহু মিল — উপরের ক্লাউড-পথেও ঠিক তাই)।
     */
    private fun mergeOwnPhoneEnquiries(cached: DraftBuckets, myMobile: String): DraftBuckets {
        val ctx = context ?: return cached
        return try {
            val myDigits = myMobile.filter { it.isDigit() }.takeLast(10)
            if (myDigits.length != 10) return cached
            val local = LocalWorkflowStore(ctx).pendingEnquiries()
            if (local.length() == 0) return cached
            val seen = HashSet<String>()
            for (e in cached.received) if (e.id.isNotBlank()) seen.add(e.id)
            val extra = mutableListOf<DraftEntry>()
            for (i in 0 until local.length()) {
                val row = local.optJSONObject(i) ?: continue
                // 🔒 খাতার সারি B81 (29.07.2026): মেলানোর নিয়ম উপরের ক্লাউড-পথের
                // সঙ্গে **হুবহু এক** — `createdBy` (যিনি ফর্ম ভরেছেন) **অথবা**
                // `receivedBy` (যিনি কল ধরেছেন)। আগে এখানে শুধু `receivedBy`
                // দেখা হত, তাই তিন-চাপ দিয়ে অন্য কারো নাম বসালে **যিনি ফর্মটা
                // ভরেছেন তাঁর নিজের তালিকা থেকেই এন্ট্রিটা হারিয়ে যেত**।
                val byC = row.s("createdBy").filter { it.isDigit() }.takeLast(10)
                val byR = row.s("receivedBy").filter { it.isDigit() }.takeLast(10)
                if (byC != myDigits && byR != myDigits) continue
                val id = row.s("id")
                if (id.isBlank() || !seen.add(id)) continue
                extra.add(entry(row, "received"))
            }
            if (extra.isEmpty()) cached else cached.copy(received = extra + cached.received)
        } catch (_: Throwable) { cached }
    }

    private fun saveCachedBuckets(branchFilter: String?, from: String?, to: String?, buckets: DraftBuckets) {
        val ctx = context ?: return
        try {
            val obj = org.json.JSONObject()
                .put("received", serializeEntries(buckets.received))
                .put("enqReject", serializeEntries(buckets.enqReject))
                .put("visitReject", serializeEntries(buckets.visitReject))
                .put("notComplete", serializeEntries(buckets.notComplete))
                .put("complete", serializeEntries(buckets.complete))
                .put("unexpectedTime", serializeEntries(buckets.unexpectedTime))
                .put("refunded", serializeEntries(buckets.refunded))
                .put("returnVisit", serializeEntries(buckets.returnVisit))
                .put("runningTreatment", serializeEntries(buckets.runningTreatment))
                .put("yearlyReg", serializeEntries(buckets.yearlyReg))
                .put("yearlyOutDemo", buckets.yearlyOutDemo)       // 🆕 V852
                .put("yearlyOutNoDate", buckets.yearlyOutNoDate)
                // 🆕🔒 V-YEARSW — ছোট্ট তালিকা (বছরের সংখ্যা মাত্র), সাইজ নগণ্য।
                .put("yearlyAvailableYears", org.json.JSONArray(buckets.yearlyAvailableYears))
            ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit().putString(cacheKey(branchFilter, from, to), obj.toString()).apply()
        } catch (_: Throwable) { }
    }

    /**
     * 🔒🔒 খাতার সারি B224 (TK verified live-test, 01.08.2026 — ছবিসহ):
     * *"DEMO TEST ও TK BISWAS-কে Delete করলে 'Record not found' আসছে, অথচ ওই
     *  দুই রেকর্ড এখনও Incomplete Patient তালিকা ও পুরোনো Detail Screen-এ থেকে
     *  যাচ্ছে, Action-ও খোলা যাচ্ছে।"*
     *
     * database-এ **আর নেই** এমন সারি Delete চাপলে (deleteEnquiry / Timeline-এর
     * দুই Delete পথ থেকে "NOT_FOUND") আর error দেখিয়ে রেখে দেওয়া হয় না — এই
     * ফাংশন সেই সারির **এই ফোনের জমানো (display-cache) ছায়া-কপি** মুছে দেয়,
     * যাতে তালিকায় ফিরে এলে সেটা আর দেখা না যায় ও count ঠিক থাকে।
     *
     * ⛔ **শুধু এই ফোনের display-cache** — কোনো cloud লেখা হয় না, `DeletedGuard`
     *    tombstone-ও বসানো হয় না। তাই অন্য ফোনের **এখনো sync না-হওয়া আসল
     *    রেকর্ড** কেবল "not found" পাওয়ার কারণে স্থায়ীভাবে মুছবে না — পরে sync
     *    হলে স্বাভাবিকভাবেই ফিরে আসবে।
     * ⛔ pending-sync queue (`LocalWorkflowStore`) ছোঁয়া হয় না, আর নিজের ফোনের
     *    এখনো না-ওঠা enquiry `mergeOwnPhoneEnquiries` দিয়ে load-এর সময় আবার
     *    মিশে যায় — তাই নিজের আসল কাজও নিরাপদ।
     *
     * id মিললে ঠিক সেই সারিটাই বাদ যায়; নইলে mobile (শেষ ১০ অঙ্ক) মিললে ওই
     * রোগীর draft ছায়া-সারি বাদ যায় (রোগী cloud-এ id ও mobile দুভাবেই সত্যিই
     * না-পাওয়ার পরেই তবে এটা ডাকা হয়, তাই over-remove হয় না)।
     */
    fun purgeGhostFromCache(id: String?, mobile: String? = null) {
        val ctx = context ?: return
        val wantId = id?.trim().orEmpty()
        val wantMob = mobile?.filter { it.isDigit() }?.takeLast(10).orEmpty()
        if (wantId.isBlank() && wantMob.length != 10) return
        // process চলাকালীন সব draft-পর্দাও যেন সঙ্গে সঙ্গে জানে (নিচের GhostHide)।
        GhostHide.hide(wantId, wantMob)
        try {
            val prefs = ctx.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            val keys = prefs.all.keys.toList()   // সব (branch/date) cache-ঘর
            val editor = prefs.edit()
            var changedAny = false
            val bucketNames = listOf("received", "enqReject", "visitReject", "notComplete", "complete", "unexpectedTime", "refunded", "returnVisit")
            fun isGhost(e: DraftEntry): Boolean {
                if (wantId.isNotBlank() && e.id == wantId) return true
                if (wantMob.length == 10 && e.mobile.filter { it.isDigit() }.takeLast(10) == wantMob) return true
                return false
            }
            for (key in keys) {
                val json = prefs.getString(key, null) ?: continue
                val obj = try { org.json.JSONObject(json) } catch (_: Throwable) { continue }
                var keyChanged = false
                for (n in bucketNames) {
                    val arr = obj.optJSONArray(n) ?: continue
                    val kept = deserializeEntries(arr).filterNot { isGhost(it) }
                    if (kept.size != arr.length()) {
                        obj.put(n, serializeEntries(kept))
                        keyChanged = true
                    }
                }
                if (keyChanged) { editor.putString(key, obj.toString()); changedAny = true }
            }
            if (changedAny) editor.apply()
        } catch (_: Throwable) { }
    }

    /**
     * 🔒 খাতার সারি B224 — উপরের ঘটনার in-memory অংশ। এক পর্দায় (Detail Screen)
     * "not found" সারি সরালে অন্য পর্দা (Incomplete তালিকা) যেটা আগেই মেমরিতে
     * লোড হয়ে আছে সেটাও যেন সঙ্গে সঙ্গে জানে — তাই এই process-জোড়া তালিকা।
     *
     * ⛔ **শুধু মেমরি, শুধু এই ফোন** — disk-tombstone নয়, cloud নয়,
     *    DeletedGuard নয়। app বন্ধ হলে মুছে যায় (তখন cloud-ই একমাত্র সত্য),
     *    তাই অন্য ফোনের আসল রেকর্ড কখনো স্থায়ীভাবে হারায় না।
     */
    object GhostHide {
        private val hiddenIds = java.util.Collections.synchronizedSet(HashSet<String>())
        private val hiddenMobiles = java.util.Collections.synchronizedSet(HashSet<String>())

        fun hide(id: String?, mobile: String?) {
            id?.trim()?.takeIf { it.isNotBlank() }?.let { hiddenIds.add(it) }
            mobile?.filter { it.isDigit() }?.takeLast(10)?.takeIf { it.length == 10 }?.let { hiddenMobiles.add(it) }
        }

        fun isHidden(id: String?, mobile: String?): Boolean {
            val i = id?.trim().orEmpty()
            if (i.isNotBlank() && hiddenIds.contains(i)) return true
            val m = mobile?.filter { it.isDigit() }?.takeLast(10).orEmpty()
            return m.length == 10 && hiddenMobiles.contains(m)
        }
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /* 🆕🔒 V850 — কার্ডের `LAST CALL` / `REGISTERED` লাইনের তথ্য।
       ⛔ সবটাই **আগে থেকে নামানো** সারি থেকে — নতুন কোনো ক্লাউড-পড়া নেই।
       `pat` = এই মানুষের `patients` সারি (থাকলে); ওখান থেকেই রেজিস্ট্রেশনের
       তারিখ ও কে রেজিস্টার করেছিলেন। */
    // ═══════════════════════════════════════════════════════════════════════
    // 💸🔒 V1281 — Draft-এর "শুধু বদল" পড়া (Follow-up V1258 / Chamber-এর একই ছাঁচ)
    // ═══════════════════════════════════════════════════════════════════════
    private fun draftDeltaPrefs() = context?.getSharedPreferences(DRAFT_DELTA_PREFS, Context.MODE_PRIVATE)
    private fun draftStampNow(): String = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(Date(System.currentTimeMillis() - DRAFT_SAFETY_BACK_MS))
    } catch (_: Throwable) { "" }
    private fun draftLoadCached(key: String): JSONArray? {
        val sp = draftDeltaPrefs() ?: return null
        return try { sp.getString("arr_$key", null)?.let { JSONArray(it) } } catch (_: Throwable) { null }
    }
    private fun draftSaveCached(key: String, arr: JSONArray) {
        val sp = draftDeltaPrefs() ?: return
        try { sp.edit().putString("arr_$key", arr.toString()).apply() } catch (_: Throwable) { }
    }
    /** পূর্ণ পড়া ৩ ঘণ্টায় একবার; মাঝে শুধু `updatedAt` বদলানো সারি জমানো তালিকার
     *  উপরে বসে (upsert-only)। ⛔ delta ব্যর্থ/জমানো তালিকা না থাকলে **আগের পূর্ণ
     *  পড়াই** চলে — ফলাফল কখনো ফাঁকা হয় না। ⛔ ছাঁকনি · limit · ঘর হুবহু আগের। */
    private fun draftDeltaOrFull(table: String, branchPart: String?, cols: String, key: String): JSONArray? {
        val sp = draftDeltaPrefs()
        val since = sp?.getString("since_$key", null)
        val lastFullAt = sp?.getLong("fullAt_$key", 0L) ?: 0L
        val now = System.currentTimeMillis()
        val cached = if (since.isNullOrBlank()) null else draftLoadCached(key)
        if (sp != null && !since.isNullOrBlank() && cached != null && (now - lastFullAt) <= DRAFT_FULL_REFRESH_INTERVAL_MS) {
            val sinceEnc = try { java.net.URLEncoder.encode(since, "UTF-8") } catch (_: Throwable) { since }
            val filter = "updatedAt=gt.$sinceEnc" + (if (branchPart.isNullOrBlank()) "" else "&$branchPart")
            val delta = try { SupabaseClient.fetchListSlimOrNull(table, filter, 2000, cols) } catch (_: Throwable) { null }
            if (delta != null) {
                val byId = LinkedHashMap<String, JSONObject>()
                for (i in 0 until cached.length()) { val o = cached.optJSONObject(i) ?: continue; val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o }
                for (i in 0 until delta.length()) { val o = delta.optJSONObject(i) ?: continue; val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o }
                val merged = JSONArray(); for (v in byId.values) merged.put(v)
                draftSaveCached(key, merged)
                try { sp.edit().putString("since_$key", draftStampNow()).apply() } catch (_: Throwable) { }
                return merged
            }
        }
        // পূর্ণ পড়া — হুবহু আগের ডাক
        val full = SupabaseClient.fetchListSlimOrNull(table, branchPart, 5000, cols) ?: return null
        if (sp != null) {
            draftSaveCached(key, full)
            try { sp.edit().putString("since_$key", draftStampNow()).putLong("fullAt_$key", now).apply() } catch (_: Throwable) { }
        }
        return full
    }

    private fun histLast(row: JSONObject, key: String): String = try {
        val arr = row.optJSONArray("history")
        if (arr == null || arr.length() == 0) "" else {
            var found = ""
            for (i in arr.length() - 1 downTo 0) {
                val e = arr.optJSONObject(i) ?: continue
                val d = if (e.isNull("date")) "" else e.optString("date", "")
                if (d.isNotBlank()) { found = if (e.isNull(key)) "" else e.optString(key, ""); break }
            }
            found
        }
    } catch (_: Throwable) { "" }

    private fun entry(
        row: JSONObject, tab: String, extra: String = "", bill: Double = 0.0, paid: Double = 0.0,
        pat: JSONObject? = null,   // 🆕 V850
        billBeforeDiscount: Double = 0.0   // 🏷️ V1508
    ): DraftEntry =
        DraftEntry(
            // 🔴🔴🔴 TK-REPORTED (31.07.2026): "Patient Name-এর জায়গায় Mobile
            // দুবার দেখানো"। আসল কারণ এইখানে পাওয়া গেল — এই সারিতেই আগে থেকে
            // name-এ mobile বসিয়ে দেওয়া হত, তাই DraftCardAdapter.kt-এ পরে
            // "UNKNOWN" ফিক্স বসালেও কখনো কাজ করত না (name কখনো
            // ফাঁকা পেত না, ইতিমধ্যেই mobile দিয়ে ভরা থাকত)। এখন name সত্যিই
            // ফাঁকা থাকলে ফাঁকাই থাকে — "UNKNOWN" adapter-এই বসে।
            name = row.s("name"),
            mobile = row.s("mobile"),
            extra = extra,
            branch = row.s("branch"),
            /* 🔒 V853 (নিজের যাচাইয়ে ধরা) — কিছু `patients` সারিতে রোগের নাম
               `disease`-এ নয়, `diagnosis`-এ থাকে (প্রজেক্টের অন্য জায়গায়
               আগে থেকেই এই fallback আছে — FollowUpRepository)। ⛔ ফাঁকা ঘর
               ভরে, ভরা ঘর কখনো বদলায় না। */
            disease = row.s("disease").ifBlank { row.s("diagnosis") },
            stage = row.s("stage"),
            // 🟢🔒 V646 (২৫.০৮.২০২৬, TK-রিপোর্ট — "আইডি কখনো দেখাই যায় না") —
            // followups সারিতে patientId নামেই ঘর আছে (আগের মতোই)। কিন্তু
            // enquiries সারিতে সেটা নেই — রেজিস্ট্রেশনের সময় patientId
            // লেখা হয় "convertedPatientId" নামের ঘরে (`RegistrationRepository.
            // kt`)। এতদিন এখানে শুধু "patientId" খোঁজা হত, তাই এনকোয়ারি-
            // সারি থেকে বানানো কার্ডে আইডি কখনোই দেখাত না। এখন দুটোই
            // চেষ্টা করা হয় — কোথাও কিছু হারায়নি, শুধু সঠিক ঘরটা এখন পড়া হয়।
            patientId = row.s("patientId").ifBlank { row.s("convertedPatientId") },
            id = row.s("id"),
            tab = tab,
            recordDate = row.s("date").ifBlank { row.s("visitDate").ifBlank { row.s("registrationDate") } }.take(10),
            nextFollow = row.s("nextFollow"),
            lastRemark = row.s("lastRemark").ifBlank { row.s("remarks").ifBlank { extra } },
            createdByMobile = row.s("receivedBy").ifBlank { row.s("createdBy") },
            // 🟢🔒 V646 — Follow-up-কার্ডের জন্য বাড়তি ঘর, সরাসরি এই একই
            // সারি থেকে (কোনো নতুন কল লাগেনি — followups row-এই এসব আছে)।
            status = row.s("status"),
            bill = bill,
            paid = paid,
            billBeforeDiscount = billBeforeDiscount,   // 🏷️ V1508
            callCount = row.optInt("callCount", 0),
            timeType = row.s("timeType"),
            address = row.s("address"),
            age = row.s("age"),
            sex = row.s("sex"),
            refId = row.s("refId"),
            /* 🆕 V850 — LAST CALL: আগে `lastCallDate` ঘর, না থাকলে `history`-র
               শেষ সারির তারিখ। সময় ও স্টাফের নাম **শুধু `history`-তেই** থাকে
               (ডেটাবেসে ওদের আলাদা ঘরই নেই — যাচাই করে দেখা)। */
            lastCallDate = row.s("lastCallDate").ifBlank { histLast(row, "date").take(10) },
            lastCallBy = FollowUpModel.prettyStaff(histLast(row, "staff")),
            lastCallTime = histLast(row, "time"),
            /* 🆕 V850 — REGISTERED: রোগীর নিজের সারি থাকলে সেখান থেকে; না
               থাকলে (নিছক এনকোয়ারি) ফাঁকা ⇒ কার্ডে আগের মতোই LAST CALL। */
            regDate = (pat ?: row.takeIf { it.has("registeredBy") })
                ?.let { it.s("registrationDate").ifBlank { it.s("date") } }.orEmpty().take(10),
            regBy = FollowUpModel.prettyStaff(
                (pat ?: row.takeIf { it.has("registeredBy") })
                    ?.let { it.s("registeredBy").ifBlank { it.s("createdBy") } }.orEmpty()
            )
        )

    /** Merges any locally-pending rows (not yet synced) matching this branch
     *  filter into a cloud result set, avoiding duplicates by id.
     *  TK-REQUESTED ADDITION (2026-07-16): same fix as Follow-up/Doctor
     *  Queue/Today's Collection/Global Search/Full Journey -- Draft always
     *  read straight from the cloud with no awareness of a save still
     *  syncing in the background. */
    private fun mergeWithPending(cloud: JSONArray, pending: JSONArray, branchFilter: String?): JSONArray {
        val merged = JSONArray()
        val seenIds = HashSet<String>()
        for (i in 0 until cloud.length()) {
            val row = cloud.getJSONObject(i)
            seenIds.add(row.optString("id"))
            merged.put(row)
        }
        val allBranch = branchFilter == null || branchFilter == "All"
        for (i in 0 until pending.length()) {
            val row = pending.getJSONObject(i)
            val id = row.optString("id")
            val branchOk = allBranch || row.s("branch").equals(branchFilter, ignoreCase = true)
            if (id.isNotBlank() && branchOk && seenIds.add(id)) merged.put(row)
        }
        return merged
    }

    /** from/to are inclusive yyyy-MM-dd bounds; pass null for no date limit. */
    fun load(branchFilter: String?, from: String? = null, to: String? = null): DraftBuckets {
        val branchPart = if (branchFilter != null && branchFilter != "All")
            "branch=eq." + java.net.URLEncoder.encode(branchFilter, "UTF-8") else null

        // TK-REQUESTED FIX (2026-07-23): same root cause/fix as Follow-up's
        // Bill/Due bug and Chamber Attendance's board -- fetchList()
        // swallows a failed request into a silent empty array, which used
        // to get baked straight into the Draft lists (and then CACHED at
        // the end of load()), making a bucket look wrongly empty on a bad
        // connection, possibly stuck that way. Now uses fetchListOrNull():
        // a genuine failure (null) on ANY of these 4 falls back to the
        // last cached buckets instead of computing/caching wrong ones; if
        // there's no cache yet, falls through to empty exactly like before
        // (no behavior change for a first-ever load). A real empty result
        // (successful fetch, genuinely nothing in that bucket) is NOT
        // affected -- only a true failure is.
        // PERFORMANCE FIX (2026-07-25, TK-requested proactive sweep): the four
        // reads the Draft screen needs do not depend on each other, so they are
        // started together instead of one after the other (the screen used to
        // wait for the SUM of four round trips). Same four queries, same
        // filters/limits, same null-checks below . only the waiting is shorter.
        // TK-REQUESTED (2026-07-27): the "My Enquiry (All Branch)" bucket needs
        // THIS staff's own enquiries from every branch (see below). That read is
        // started HERE, together with the other four, so the screen does not wait
        // one extra round trip for it on a slow line -- the total waiting time is
        // unchanged. Master is excluded: he sees exactly the branch he selected.
        val myOwnDigits = run {
            val me = context?.let { NativeSession.current(it) }
            if (branchFilter == null || branchFilter == "All") "" 
            else if (me == null || me.role == "master") ""
            else me.mobile.filter { it.isDigit() }.takeLast(10)
        }
        val preDraft = runBlocking {
            // 🟢🔒🔒 V647 (২৫.০৮.২০২৬, TK-নির্দেশ, "Supabase Egress কমানোর কোনো
            // উপায় আছে কিনা যাচাই করুন") — **আসল কারণ, খুঁজে বার করা:**
            // Follow-up স্ক্রিন প্রতিটা বড় পড়ায় (`followups`/`patients`/
            // `payments`) `CloudReadCache` (২০-সেকেন্ডের হালকা ক্যাশ, প্রমাণিত,
            // ইতিমধ্যে FollowUpRepository.kt-এ ১১ বার ব্যবহৃত) দিয়ে মোড়া —
            // কিন্তু Draft স্ক্রিন **কখনোই** এই ক্যাশ ব্যবহার করত না। ফলে
            // Follow-up আর Draft কাছাকাছি সময়ে খোলা হলে, বা Draft নিজেই
            // বারবার (Back-Forward) খোলা হলে, প্রতিবারই সম্পূর্ণ নতুন করে
            // চারটে বড় টেবিল (৫০০০ সারি পর্যন্ত) টানত।
            // **সমাধান:** এই একই, প্রমাণিত ক্যাশ এখানেও — নিজস্ব, আলাদা
            // cache-key ("draft:...") ব্যবহার করা হয়েছে (Follow-up-এর
            // cache-key-র সাথে গুলিয়ে না যায়, তাই দুই স্ক্রিনের ভিন্ন
            // column-সেট কখনো একে অপরের জায়গায় ভুল করে বসবে না — নিরাপদ,
            // স্বাধীন ক্যাশ)। ⛔ ছাঁকনি/কলাম/লিমিট/লজিক কিচ্ছু বদলায়নি —
            // শুধু ২০ সেকেন্ডের মধ্যে একই অনুরোধ দ্বিতীয়বার এলে ক্লাউডে না
            // গিয়ে এই হালকা মেমোরি থেকেই উত্তর দেয়। ব্যর্থ (null) ফলাফল
            // কখনো মনে রাখা হয় না (CloudReadCache-এর নিজস্ব, প্রমাণিত নিয়ম),
            // তাই আসল ব্যর্থতা লুকোয় না।
            // 🔴🔴🔒 V650 (২৫.০৮.২০২৬, TK-রিপোর্ট — Android Studio বিল্ড-এরর
            // "Only safe (?.) or non-null asserted (!!.) calls are allowed
            // on a nullable receiver") — **আসল কারণ (কোড ধরে যাচাই):**
            // `branchPart` (উপরে, ৪১৬ নং লাইন) `String?` (nullable) —
            // "All"/মাস্টার-সব-ব্রাঞ্চ হলে এটা `null` হয়। V647-এ এখানে
            // সরাসরি `.ifBlank` কল করা হয়েছিল, যেটা শুধু non-null String-এই
            // চলে — তাই কম্পাইলই হয়নি। এখন নিরাপদ `?:` (null হলে "all")
            // দিয়ে ঠিক করা হলো — একই ফলাফল (branchPart কখনো ফাঁকা স্ট্রিং
            // হয় না, শুধু null অথবা "branch=eq...." — তাই `.ifBlank`-এর
            // দরকারই ছিল না)।
            val draftCacheKey = branchPart ?: "all"
            // 🔵🔒 V441 (19.08.2026, TK-অনুমোদিত — Free Plan): Draft এই enquiry
            // তালিকা থেকে শুধু card/bucket বানাতে লাগা ঘরগুলোই পড়ে। Same rows, same
            // branch filter, same 5000 limit; narrow read ব্যর্থ হলে shared safety-net
            // আগের মতো full row দিয়ে retry করে — তালিকা ফাঁকা হয়ে যাবে না।
            val a = async(Dispatchers.IO) {
                CloudReadCache.get("draft:enq:$draftCacheKey") {
                    draftDeltaOrFull("enquiries", branchPart, SupabaseClient.ENQUIRY_COLS_DRAFT, "enq_$draftCacheKey")   // 💸 V1281
                }
            }
            // 🔒 কোটা/গতি (29.07.2026, খাতার সারি B105): এই পর্দার `patients`
            // পড়াটা আগেই ছবি বাদ দিয়ে করা হয়েছিল, কিন্তু `followups`-এও
            // **ঠিক একই `photo` ঘর** আছে — সেটা তখন বাদ পড়েনি, তাই রোগীর ছবি
            // দু'বারের বদলে একবার নামত মাত্র। Draft পর্দায় কোথাও ছবি দেখানো
            // হয় না (কোডে মিলিয়ে দেখা হয়েছে — এই ফাইলে `photo` একবারও পড়া হয় না)।
            // ⛔ শুধু `photo` বাদ — বাকি প্রতিটা ঘর আগের মতোই আসে; ছাঁকনি ·
            //    limit · সাজানো কিছুই বদলায়নি, আর সরু পড়া ব্যর্থ হলে
            //    `fetchListSlimOrNull` নিজেই সব ঘর চেয়ে নেয়, তাই আসল ব্যর্থতা
            //    আগের মতোই `null` হয়ে ফেরে (নিচের ক্যাশ-ফলব্যাক ওটাই আশা করে)।
            /* 💸🔒 V1281 (০৯.০৯.২০২৬, TK-র অনুমতি — তালিকা সারি ৪০৫, ধাপ ১; TK:
               *"এক এক করে করবেন … কোনো ভালো কাজ যেন খারাপ না হয় … আন্দাজে কিছু
               করবেন না"*)। **আগে:** Draft খুললেই চারটে টেবিলের পুরো ব্রাঞ্চ নামত
               (ভাগাভাগি-ক্যাশ মাত্র ২০ সেকেন্ড), followups-এ `history`-সহ — TK-র মাপে
               মাস্টার ফোনে প্রতিবার ~৯ MB। **এখন:** Follow-up (V1258) ও Chamber-এর
               প্রমাণিত নিয়ম — পূর্ণ পড়া **৩ ঘণ্টায় একবার**, মাঝে শুধু `updatedAt`
               বদলানো সারি (`draftDeltaOrFull`), আর followups-এ `history` নামে না
               (`FOLLOWUP_COLS_DRAFT`)।
               ⛔ চারটে টেবিলের ছাঁকনি · limit · ঘর (history ছাড়া) · নিচের সব হিসাব —
                  এক অক্ষরও বদলায়নি; delta ব্যর্থ হলে আগের পূর্ণ পড়াই চলে।
               ⛔ ঝুঁকি (TK মেনেছেন): অন্য ফোনে চিরতরে মোছা সারি ৩ ঘণ্টা পর্যন্ত
                  Draft-এ থাকতে পারে; ৩ ঘণ্টার পূর্ণ পড়া সেটা নিজে ঠিক করে। */
            val b = async(Dispatchers.IO) {
                CloudReadCache.get("draft:followups:$draftCacheKey") {
                    draftDeltaOrFull("followups", branchPart, SupabaseClient.FOLLOWUP_COLS_DRAFT, "fu_$draftCacheKey")
                }
            }
            // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): this one
            // read used to bring down EVERY patient's PHOTO -- a full image
            // stored inside each row -- on a screen that shows no photo at all.
            // On TK's line that alone was most of the Draft screen's wait.
            // ⛔ NOTHING CHANGES: PATIENT_COLS_NO_PHOTO is every column of the
            // patients table EXCEPT "photo" (the same proven list Global
            // Search, Payment and Doctor already use), the filter/limit/order
            // are untouched, and fetchListSlimOrNull falls back to every column
            // by itself if the narrowed read ever fails -- so a genuine failure
            // is still reported as null exactly as before.
            val c2 = async(Dispatchers.IO) {
                CloudReadCache.get("draft:patients:$draftCacheKey") {
                    draftDeltaOrFull("patients", branchPart, SupabaseClient.PATIENT_COLS_NO_PHOTO, "pat_$draftCacheKey")   // 💸 V1281
                }
            }
            // ⚡ খাতার সারি B135 (TK "হ্যাঁ", 29.07.2026 রাত ৯.৩০): এই পড়াটাও
            // `payments`-এর সব ঘর চাইত। Draft যে ঘরগুলো পড়ে (amount · date ·
            // branch · mobile · payType · receivedBy · createdBy …) সবই নিচের
            // তালিকায় আছে — প্রজেক্টে আগে থেকেই ব্যবহার হওয়া `PAYMENT_COLS_LIST`।
            // ⛔ কোনো হিসাব · ছাঁকনি · নিয়ম বদলায়নি; সরু পড়া ব্যর্থ হলে অ্যাপ
            //    নিজেই সব ঘর চেয়ে নেয়, আসল ব্যর্থতা আগের মতোই `null`।
            val d = async(Dispatchers.IO) {
                CloudReadCache.get("draft:payments:$draftCacheKey") {
                    draftDeltaOrFull("payments", branchPart, SupabaseClient.PAYMENT_COLS_LIST, "pay_$draftCacheKey")   // 💸 V1281
                }
            }
            val e2 = async(Dispatchers.IO) {
                if (myOwnDigits.length == 10)
                    SupabaseClient.fetchListSlimOrNull(
                        "enquiries",
                        "or=(createdBy.like.*$myOwnDigits,receivedBy.like.*$myOwnDigits)",
                        2000,
                        SupabaseClient.ENQUIRY_COLS_DRAFT
                    )
                else null
            }
            listOf(a.await(), b.await(), c2.await(), d.await(), e2.await())
        }
        val myOwnEnquiries = preDraft[4]
        val enqRaw = preDraft[0]
        val followRaw = preDraft[1]
        val patientsRaw = preDraft[2]
        val paymentsRaw = preDraft[3]
        if (enqRaw == null || followRaw == null || patientsRaw == null || paymentsRaw == null) {
            loadCachedBuckets(branchFilter, from, to)?.let { return it }
        }
        var enq = enqRaw ?: org.json.JSONArray()
        var follow = followRaw ?: org.json.JSONArray()
        var patients = patientsRaw ?: org.json.JSONArray()
        var payments = paymentsRaw ?: org.json.JSONArray()
        context?.let { ctx ->
            val store = LocalWorkflowStore(ctx)
            // 🔴 V1311 (তালিকা ৪২৩): ক্লাউড ধরে ফেললে ফোনের PENDING কপি SYNCED — আগে চিরকাল থেকে যেত
            try { store.markSyncedWhereCloudCaughtUp("enquiries", enq); store.markSyncedWhereCloudCaughtUp("followups", follow)
                  store.markSyncedWhereCloudCaughtUp("patients", patients); store.markSyncedWhereCloudCaughtUp("payments", payments) } catch (_: Throwable) { }
            enq = mergeWithPending(enq, store.pendingEnquiries(), branchFilter)
            follow = mergeWithPending(follow, store.pendingFollowUps(), branchFilter)
            patients = mergeWithPending(patients, store.pendingPatients(), branchFilter)
            payments = mergeWithPending(payments, store.pendingPayments(), branchFilter)
        }

        val paidByMobile = HashMap<String, Double>()
        // 💰 V1209 — মোবাইল ধরে "তারিখ → টাকা" (একই payments থেকেই)।
        val payHistByMobile = HashMap<String, ArrayList<Pair<String, Double>>>()
        // 🔴 TK-নির্দেশ (02.08.2026): কার Approved Refund আছে সেটাও এই একই
        // লুপে ধরে রাখা হচ্ছে (নিচে "Refunded" ঘর বানাতে লাগবে) — নতুন কোনো
        // নেট-কল লাগেনি, একই payments থেকেই।
        val hasApprovedRefundByMobile = HashSet<String>()
        // 🔴🔴 TK-অডিট-অনুরোধ (01.08.2026, Follow-up-এর Due-ভুল ধরার পরে সম্পূর্ণ
        // প্রজেক্ট যাচাই): এখানে Refund সারিও প্লেইন পজিটিভ Payment হিসেবে
        // যোগ হচ্ছিল — FollowUpRepository-র V238 `paidEffect` নিয়ম (approved
        // refund বিয়োগ, pending/rejected কোনো প্রভাব নেই) এখানে কখনো বসানো হয়নি।
        // ফল: Refund পাওয়া রোগীর "paid" ভুলভাবে বেশি দেখাত, তাই Due ভুলভাবে ০
        // বা কম হয়ে যেত — এই ফাইলেই নিচে Due==0 হলে রোগীকে "Complete Patient"
        // তালিকায় ফেলা হয় ও "✅ Treatment complete" লেখা হয়, তাই বাকি Due থাকা
        // সত্ত্বেও ভুল বাকেটে/ভুল স্ট্যাটাসে চলে যেতে পারত। এখন একই নিয়ম বসানো হলো।
        for (i in 0 until payments.length()) {
            val p = payments.getJSONObject(i)
            val payType = p.optString("payType", "")
            if (payType == "visit_fee" || payType == "attendance_mark" || PaymentModel.isMarkerOnlyRow(payType)) continue   /* 🔴🔒 V1301 (তালিকা ৪১৩): চিহ্ন-সারি (bill_edit · chamber_expected · attendance_mark) কখনো টাকা নয় */
            val paidEffect = when {
                PaymentModel.isApprovedRefund(p) -> -p.optDouble("amount", 0.0)
                PaymentModel.isRefundRow(p) -> 0.0
                else -> p.optDouble("amount", 0.0)
            }
            val pm = p.s("mobile").filter { it.isDigit() }.takeLast(10)
            if (pm.isNotBlank()) paidByMobile[pm] = (paidByMobile[pm] ?: 0.0) + paidEffect
            /* 💰 V1209 — একই লুপে "কত তারিখে কত" জমা হয় (নতুন কল নয়)।
               ⛔ Refund-এর সারি বাদ (উপরের `paidEffect`-এর মতোই হিসাব),
                  তাই টাকার যোগফল ও এই তালিকা কখনো আলাদা হয় না। */
            if (pm.isNotBlank() && paidEffect != 0.0) {
                val d = p.s("date").take(10)
                payHistByMobile.getOrPut(pm) { ArrayList() }.add(d to paidEffect)
            }
            if (PaymentModel.isApprovedRefund(p) && pm.isNotBlank()) hasApprovedRefundByMobile.add(pm)
        }

        val received = mutableListOf<DraftEntry>()
        // 🚨 TK'S LOCKED RULE (told long before today, restated 27.07.2026):
        //   "জলপাইগুড়ির স্টাফ কিশানগঞ্জের জন্য এনকোয়ারি করলে সেই নম্বর কিশানগঞ্জের
        //    সব স্টাফ ও মাস্টার দেখবে। আর যে স্টাফ এন্ট্রিটা করেছে, সে সেটা দেখবে
        //    Draft-এর 'My Enquiry (All Branch)'-তে।"
        //
        // WHAT WAS BROKEN: this screen's four reads are all filtered to the
        // staff's OWN branch (branchPart, above). So the bucket labelled
        // "All Branch" was in fact showing one branch only -- an enquiry a
        // Jalpaiguri staff took FOR Kishanganj was filtered out by the very
        // first query, and the person who entered it had nowhere left in the
        // app to see it. The rule was right; this screen simply never asked
        // for those rows.
        //
        // FIX: one extra, narrow read -- enquiries whose createdBy/receivedBy is
        // THIS staff, with no branch limit -- merged into this ONE bucket only.
        // Nothing else on the screen changes: the other buckets, and Master's
        // All-Branch view (which already fetches everything), are untouched, and
        // no other staff's cross-branch row is fetched, so this cannot re-open
        // the cross-branch leak TK closed on 25.07.2026.
        val receivedRows = org.json.JSONArray()
        val receivedIds = HashSet<String>()
        // 🚨 TK-REPORTED, LIVE (29.07.2026 দুপুর ১.৪০, খাতার সারি B81 — স্টাফের
        // অভিযোগ: *"এই এন্ট্রিটা তো আমি করেছিলাম, সেখানে অন্যজনের নাম কেন
        // দেখাচ্ছে?"*)
        //
        // **আসল দোষ:** এই বাক্সটার নাম **"My Enquiry (All Branch)"**, অর্থাৎ
        // TK-এর নিয়ম অনুযায়ী এখানে **শুধু নিজের করা এন্ট্রি** থাকার কথা
        // (খাতার \"এক নম্বরে সব কল\" নিয়মের ৩ নম্বর ধারা: *\"যিনি ফর্মটা ফিলাপ
        // করেছিলেন, তিনি নম্বরটা দেখবেন নিজের Draft → My Enquiry-তে\"*)।
        // কিন্তু এখানে **ব্রাঞ্চের প্রতিটা এনকোয়ারি** ঢেলে দেওয়া হত — কে
        // করেছে তা দেখাই হত না। তাই স্টাফ নিজের তালিকায় **অন্য স্টাফের ও
        // মাস্টারের করা এন্ট্রি** দেখতেন, আর তাতে অন্যজনের নামই উঠত।
        //
        // এখন স্টাফ/ডাক্তার/ফিল্ড-এর ক্ষেত্রে **শুধু নিজের সারিই** ঢোকে —
        // মেলানো হয় ঠিক সেই একই নিয়মে যা উপরের নিজস্ব ক্লাউড-খোঁজায় ব্যবহার
        // হয় (`createdBy` **অথবা** `receivedBy`), তাই দুই পথে ফল কখনো আলাদা
        // হবে না, আর কারো প্রাপ্য এন্ট্রি হারাবে না।
        //
        // ⛔ **মাস্টারের দেখায় এক অক্ষরও বদলায়নি** — TK-এর ২৭.০৭.২০২৬-এর
        // নিয়ম: *"মাস্টার যা বেছেছেন ঠিক তাই দেখাবে"*; তাঁর জন্য `myOwnDigits`
        // ফাঁকা থাকে, তাই আগের মতোই সব সারি ঢোকে।
        // ⛔ বাড়তি কোনো ক্লাউড-কল নেই · অন্য কোনো বাক্স ছোঁয়া হয়নি।
        // ⚠️ এখানে `myOwnDigits` ব্যবহার করা হয়নি ইচ্ছে করেই — ওটা ব্রাঞ্চ
        // "All" হলে ফাঁকা থাকে (ওটা শুধু বাড়তি ক্লাউড-খোঁজা চালানোর শর্ত)।
        // ফিল্ড অফিসারের ব্রাঞ্চ "All", তাই ওটা ধরলে তাঁর তালিকাতেও সবার
        // এন্ট্রি ঢুকে পড়ত। তাই পরিচয়টা সরাসরি লগইন থেকে নেওয়া হয়।
        // ⛔ মাস্টার বাদ — তাঁর দেখা আগের মতোই থাকে। বাড়তি কোনো ক্লাউড-কল নেই।
        val myDigitsForBucket = run {
            val me = context?.let { NativeSession.current(it) }
            if (me == null || me.role == "master") "" else me.mobile.filter { c -> c.isDigit() }.takeLast(10)
        }
        for (i in 0 until enq.length()) {
            val row = enq.getJSONObject(i)
            if (myDigitsForBucket.length == 10) {
                val by = row.s("createdBy").filter { c -> c.isDigit() }.takeLast(10)
                val recv = row.s("receivedBy").filter { c -> c.isDigit() }.takeLast(10)
                if (by != myDigitsForBucket && recv != myDigitsForBucket) continue
            }
            if (receivedIds.add(row.s("id"))) receivedRows.put(row)
        }
        // TK'S ANSWER (27.07.2026): Master looks at ONE branch he selects, or All
        // Branch -- what Master sees must follow that choice exactly and must never
        // be widened by his own entries. So this own-rows addition is for
        // staff/doctor only (myOwnDigits is blank for Master, see above).
        run {
            val myDigits = myOwnDigits
            if (myDigits.length == 10) {
                val mineOrNull = myOwnEnquiries
                if (mineOrNull != null) {
                    for (i in 0 until mineOrNull.length()) {
                        val row = mineOrNull.getJSONObject(i)
                        if (receivedIds.add(row.s("id"))) receivedRows.put(row)
                    }
                }
                // The same enquiry may still be waiting on this very phone (not
                // yet uploaded). mergeWithPending() above drops it because its
                // branch is another branch's -- so add this staff's own pending
                // rows here, where branch must not matter.
                context?.let { ctx ->
                    val localEnq = LocalWorkflowStore(ctx).pendingEnquiries()
                    for (i in 0 until localEnq.length()) {
                        val row = localEnq.optJSONObject(i) ?: continue
                        // 🔒 খাতার সারি B81: উপরের ও ক্যাশের পথের সঙ্গে **হুবহু
                        // এক নিয়ম** — `createdBy` অথবা `receivedBy`। আগে শুধু
                        // `receivedBy` দেখা হত, তাই তিন-চাপ দিয়ে অন্য কারো নাম
                        // বসালে যিনি ফর্ম ভরেছেন তাঁর তালিকা থেকে এন্ট্রিটা
                        // হারিয়ে যেত।
                        val byC = row.s("createdBy").filter { c -> c.isDigit() }.takeLast(10)
                        val byR = row.s("receivedBy").filter { c -> c.isDigit() }.takeLast(10)
                        if (byC != myDigits && byR != myDigits) continue
                        if (receivedIds.add(row.s("id"))) receivedRows.put(row)
                    }
                }
            }
        }
        // 🟢🔒 V646 — এই তালিকা এখন মূল লুপ শেষ হওয়ার পরে বানানো হয় (নিচে),
        // যাতে প্রতিটা মানুষের **সবচেয়ে সাম্প্রতিক অবস্থার** (followByMobile)
        // সাথে জোড়া যায় — My Enquiry-র কার্ড তাই পুরনো স্ন্যাপশটে আটকে থাকে
        // না, বর্তমান Enquiry/Visit/Patient অবস্থা অনুযায়ী বদলায়।

        // TK-REQUESTED ADDITION (2026-07-18): Unexpected Time calls, with a
        // one-glance conversion status per number so TK can tell at month-
        // end how many of these actually became a real patient/treatment,
        // for staff incentive decisions. Reuses the same patients+payments
        // data already fetched above -- no new network calls.
        // TK-REQUESTED (2026-07-27), ধাপ ৩খ — Draft পর্দা: this map kept
        // whichever patients row came LAST for a mobile, while the money screen,
        // Patient Details and the Report Card all use the ONE shared rule
        // (current branch -> the row with a real bill -> the first row). On a
        // duplicate registration that meant Draft could read the empty row and
        // report "Enquiry only / no bill yet" for a patient who is in fact under
        // treatment. Same rule everywhere now; with a single row (the normal
        // case) this map is exactly what it was before.
        val patientRowsByMobile = HashMap<String, org.json.JSONArray>()
        for (i in 0 until patients.length()) {
            val row = patients.getJSONObject(i)
            val m = row.s("mobile").filter { it.isDigit() }.takeLast(10)
            if (m.isNotBlank()) patientRowsByMobile.getOrPut(m) { org.json.JSONArray() }.put(row)
        }
        val patientByMobile = HashMap<String, JSONObject>()
        for ((m, rows) in patientRowsByMobile) {
            val chosen = PatientIdentity.pickPatientRow(rows, branchFilter.orEmpty())
            if (chosen != null) patientByMobile[m] = chosen
        }

        // 🔴 TK-REPORTED (04.08.2026, ছবিসহ — Enquiry Reject List-এ একই মানুষ
        // দুইবার, যেমন MD SHARIF UDDIN): Patient Timeline-এ "View" চেপে
        // যাচাই করে TK নিশ্চিত হয়েছেন — ওই নম্বরে আসলে ডাটাবেসে **একটাই**
        // followups সারি (একই স্টাফ COB-UTTAMA-র ৩টা কল, সবই একই সারির
        // history-তে জমা)। তাহলে এই তিনটে তালিকায় (Enquiry/Visit Reject,
        // Incomplete) একই মোবাইল একাধিকবার আসছিল কেন — সেটা ডাটাবেসের
        // সমস্যা না, **এই স্ক্রিন বানানোর সময়ই একই নম্বর দুইবার তালিকায়
        // ঢুকে যাচ্ছিল** (ফোনে আটকে-থাকা পুরনো একটা লোকাল কপি + আসল
        // ক্লাউড কপি — দুটোরই id আলাদা বলে আগের mergeWithPending()-এর
        // id-ধরে-dedup সেটা ধরতে পারেনি)।
        //
        // ⛔ সমাধান শুধু এই তিনটে তালিকা **দেখানোর** সময় — ডাটাবেসে কিছু
        // মোছা/বদলানো হয় না, Restore/Delete/Save কোনো লজিক ছোঁয়া হয়নি।
        // একই মোবাইল একই বাকেটে দ্বিতীয়বার এলে, দুটোর মধ্যে যেটার history
        // বড় (মানে বেশি তথ্য জমা আছে) সেটাই রাখা হয়; সমান হলে যেটার
        // updatedAt নতুন সেটা রাখা হয়। ⛔ ভিন্ন মোবাইল/ভিন্ন বাকেট কখনো
        // একসাথে মেলানো হয় না।
        fun dedupByMobile(rows: List<JSONObject>): List<JSONObject> {
            val best = LinkedHashMap<String, JSONObject>()
            for (row in rows) {
                val key = row.s("mobile").filter { it.isDigit() }.takeLast(10)
                if (key.isBlank()) { best[row.s("id").ifBlank { java.util.UUID.randomUUID().toString() }] = row; continue }
                val existing = best[key]
                if (existing == null) { best[key] = row; continue }
                /* 💸 V1281 — `history` আর নামে না; কোন সারিটা "বেশি চলেছে" সেটা
                   এখন `callCount` দিয়ে (একই মানে: কতবার কথা হয়েছে), সমান হলে
                   আগের মতোই updatedAt। */
                val existingHist = existing.optInt("callCount", existing.optJSONArray("history")?.length() ?: 0)
                val newHist = row.optInt("callCount", row.optJSONArray("history")?.length() ?: 0)
                if (newHist > existingHist) { best[key] = row; continue }
                if (newHist == existingHist && row.s("updatedAt") > existing.s("updatedAt")) best[key] = row
            }
            return best.values.toList()
        }

        val enqRejectRows = mutableListOf<JSONObject>()
        val visitRejectRows = mutableListOf<JSONObject>()
        val notCompleteRows = mutableListOf<JSONObject>()
        // 🟢🔒🔒 V645 (২৫.০৮.২০২৬, TK-নির্দেশ — "৬০ দিন বা তার ঊর্ধ্বে যদি কোন
        // পেসেন্ট টাকা জমা না করে সে অটোমেটিক ইনকমপ্লিট পেশেন্টে যাবে, এটা
        // দুই জায়গাতেই একই নিয়ম থাকা উচিত") — **আসল কারণ, খুঁজে বার করা:**
        // Android-এ "Incomplete Patient" এতদিন শুধু স্টাফের **হাতে-চাপা**
        // বোতাম থেকে আসত (stage=="Treatment" && status=="incomplete") —
        // কোনো তারিখ-ভিত্তিক স্বয়ংক্রিয় নিয়ম ছিলই না। ওয়েবে উল্টো — শুধু
        // স্বয়ংক্রিয় (stage=="Patient" && টাকা জমা নেই && ৬০+ দিন পুরনো),
        // হাতে-চাপার কোনো উপায় নেই। **সমাধান:** এখন Android-এও ওয়েবের
        // ঠিক একই স্বয়ংক্রিয় নিয়ম যোগ হলো (একই ৬০-দিন, একই "কোনো টাকা
        // জমা নেই" শর্ত) — দুই প্ল্যাটফর্মে এখন একই নিয়ম।
        // ⛔ স্টাফের হাতে-চাপা পুরনো বোতামটা (PatientTimelineActivity-র
        //    "⏳ Incomplete Patient" অ্যাকশন) সরানো হয়নি — কেউ যদি এখনো
        //    সেটা ব্যবহার করে থাকেন, তাঁর কাজ নষ্ট হবে না। তাই কোনো রোগী
        //    **হয় হাতে-চাপা, নয়তো ৬০-দিন-নিয়মে** — যেকোনো একটাতে মিললেই
        //    এই তালিকায় আসেন (দুটো নিয়মের মিলন, কোনোটাই বাদ যায় না)।
        val today = java.util.Calendar.getInstance()
        fun daysSince(dateStr: String): Int {
            if (dateStr.isBlank()) return 0
            return try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr.take(10)) ?: return 0
                val diffMs = today.timeInMillis - d.time
                (diffMs / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
            } catch (_: Throwable) { 0 }
        }
        // 🟢🔒 V621 (২৪.০৮.২০২৬, TK-নির্দেশ) — Fees Return করা Visit-এর জন্য
        // সম্পূর্ণ নতুন, আলাদা তালিকা (Visit Reject থেকে ইচ্ছাকৃতভাবে আলাদা)।
        val returnVisitRows = mutableListOf<JSONObject>()
        // 🟢🔒 V644 (২৫.০৮.২০২৬, TK-নির্দেশ) — "Running Patient": stage ==
        // "Treatment" ও স্বাভাবিক active অবস্থা (Incomplete/Cancelled/
        // Rejected/Closed/Returned নয় — ওগুলো আগে থেকেই আলাদা bucket-এ)।
        val runningTreatmentRows = mutableListOf<JSONObject>()
        // 🟢🔒🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ — "এনকোয়ারি ভিজিট/পেশেন্ট হলে
        // My Enquiry কার্ডেই সেই বর্তমান অবস্থা দেখাতে হবে") — **আসল কারণ:**
        // My Enquiry এতদিন `enquiries` টেবিলের **স্থির স্ন্যাপশট** থেকে
        // বানানো হত — ওই সারি রেজিস্ট্রেশনের পরে (Visit) আর কখনো আপডেট হয়
        // না, ট্রিটমেন্ট শুরু হলে তো নয়ই। তাই My Enquiry-র কার্ড সবসময়
        // প্রথম দিনের অবস্থাতেই আটকে থাকত। **সমাধান:** প্রতিটা মোবাইলের
        // **সবচেয়ে সাম্প্রতিক অবস্থার** `followups` সারিটা এখানে ধরে রাখা
        // হচ্ছে (একই লুপে, বাড়তি কোনো Supabase কল নেই) — V638-এর একই
        // প্রমাণিত টাই-ব্রেকার (সমান stage হলে সবচেয়ে নতুনটা জেতে)। My
        // Enquiry বানানোর সময় (নিচে) এই লাইভ ডেটা দিয়ে কার্ড বসে, তাই
        // Visit/Treatment হয়ে গেলে কার্ডও সাথে সাথে বদলে যায়।
        val followByMobile = HashMap<String, JSONObject>()
        val followByMobilePriority = HashMap<String, Int>()
        val followByMobileUpdatedAt = HashMap<String, String>()
        fun followStagePriority(s: String): Int = when {
            s.equals("Treatment", true) -> 3
            s.equals("Patient", true) -> 2
            s.equals("Inquiry", true) -> 1
            else -> 0
        }
        // 🟢🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ — "এখান থেকে কল করুক আর Follow Up
        // থেকে কল করুক, জিনিস একই থাকতে হবে") — "Unexpected Time Calls" আগে
        // `enquiries` টেবিল থেকে আসত (আলাদা, স্থির স্ন্যাপশট)। এখন সরাসরি
        // `followups` টেবিল থেকে (stage=="Inquiry", timeType=="Unexpected
        // Time") — Follow-up-এর Enquiry ট্যাব ঠিক এই একই সারি থেকেই বানায়,
        // তাই Draft থেকে কল করা আর Follow-up থেকে কল করা এখন **একই জীবন্ত
        // রেকর্ড**।
        val unexpectedTimeRows = mutableListOf<JSONObject>()
        for (i in 0 until follow.length()) {
            val row = follow.getJSONObject(i)
            // 🔒 V215 (§18, 31.07.2026): এই তালিকাগুলো cancelled/incomplete follow-up
            // সারি থেকেই বানানো হয়, তাই পুরো রোগী Delete করলেও (যেখানে ঐ সারিটা
            // tombstone হয় কিন্তু history-র জন্য cloud-এ Cancelled-ই থাকে) আগে সারিটা
            // এখানে ফিরে আসত। এখন DeletedGuard-এ tombstone থাকলে বাদ — ঠিক যেমন
            // FollowUpRepository/RegistrationRepository আগে থেকেই করে। এতে Delete-এর
            // পর নাম সঙ্গে সঙ্গে সরে যায় ও Back/Refresh-এ আর ফিরে আসে না। Restore
            // করলে tombstone সরে বলে আবার দেখা যায়।
            try {
                if (DeletedGuard.isDeleted("followups", row.optString("id", ""), context)) continue
            } catch (_: Throwable) { }
            val stage = row.s("stage")
            val status = row.s("status").lowercase()
            // 🟢🔒 V646 — Unexpected Time collector, একই লুপে (বাড়তি কোনো
            // Supabase কল/পাস লাগেনি)। ⛔ Enquiry Reject-এ ধরা পড়া (terminal
            // status) সারি বাদ, যাতে দুই বাকেটে একই নম্বর দ্বিতীয়বার না আসে।
            if (stage == "Inquiry" && row.s("timeType").equals("Unexpected Time", ignoreCase = true) &&
                status !in setOf("cancelled", "incomplete", "rejected", "closed")) {
                unexpectedTimeRows.add(row)
            }
            // 🟢🔒 V646 — My Enquiry-র জন্য প্রতি মোবাইলে সবচেয়ে সাম্প্রতিক
            // অবস্থার followups সারি ধরে রাখা (উপরের মন্তব্যে ব্যাখ্যা করা)।
            run {
                val fm = row.s("mobile").filter { c -> c.isDigit() }.takeLast(10)
                if (fm.isNotBlank()) {
                    val pr = followStagePriority(stage)
                    val ua = row.s("updatedAt").ifBlank { row.s("createdAt") }
                    val curPr = followByMobilePriority[fm] ?: -1
                    if (pr > curPr || (pr == curPr && ua > (followByMobileUpdatedAt[fm] ?: ""))) {
                        followByMobilePriority[fm] = pr
                        followByMobileUpdatedAt[fm] = ua
                        followByMobile[fm] = row
                    }
                }
            }
            // 🟢🔒 V645 — ৬০-দিন-নিয়মের জন্য: কোনো টাকা জমা আছে কিনা,
            // paidByMobile থেকে (এই একই ফাংশনে উপরেই বানানো, নতুন কোনো
            // Supabase কল লাগেনি)।
            val mobKeyForAge = row.s("mobile").filter { it.isDigit() }.takeLast(10)
            val paidSoFar = paidByMobile[mobKeyForAge] ?: 0.0
            val ageDays = daysSince(row.s("date").ifBlank { row.s("visitDate").ifBlank { row.s("registrationDate") } })
            val autoIncomplete60Day = stage == "Patient" && paidSoFar <= 0.0 && ageDays >= 60
            when {
                stage == "Inquiry" && (status in setOf("cancelled", "incomplete", "rejected", "closed") ||
                    FollowUpRepository.inquiryHistoryEndsTerminal(row)) -> enqRejectRows.add(row)
                stage == "Patient" && status == "returned" -> returnVisitRows.add(row)
                stage == "Patient" && status == "cancelled" -> visitRejectRows.add(row)
                /* 🔴🔴🔒 V716 (২৬.০৮.২০২৬, TK-নির্দেশ — লাইভে ধরা পড়া, ছবি ও
                   ডেটাবেসের প্রমাণসহ; TK: *"হ্যাঁ তিনি ট্রিটমেন্ট করাচ্ছেন"*):

                   **আসল সমস্যা (আন্দাজ নয়, প্রমাণিত):** `stage = "Treatment"` +
                   `status = "cancelled"` — এই অবস্থার কার্ড **অ্যাপের কোনো
                   তালিকাতেই দেখা যেত না**। উপরের নিয়ম দুটো ছিল শুধু
                   `Patient + cancelled` (Visit Reject) আর
                   `Treatment + incomplete` (Incomplete) — মাঝের এই ঘরটা ফাঁকা।
                   ফলে কার্ডটা Follow-up-এও নেই (বাতিল বলে বাদ), Draft-এর কোনো
                   তালিকাতেও নেই ⇒ স্টাফ চাইলেও **ফেরানোর কোনো বোতাম পেত না**।

                   লাইভ প্রমাণ: SADDAM HUSSAIN (কিশনগঞ্জ, …9547006061) — টাকা
                   নেওয়া হয়েছে ও ক্লাউডে পৌঁছেছে, কিন্তু কার্ড বাতিল অবস্থায়
                   আটকে ১৬ জুলাই থেকে। সেই আটকে থাকা পেমেন্টই দিনে ~৪,০০০ বার
                   অকারণে চেষ্টা করত (V715-এর তদন্ত দ্রষ্টব্য)। ডেটাবেসে এমন
                   কার্ড মোট **২৪টা** (TK-এর সামনে গোনা)।

                   **সমাধান:** এরা এখন সত্যিকারের বাতিল-করা Visit-এর মতোই
                   **"Visit Reject"** তালিকায় দেখাবে, তাই স্টাফ চেনা
                   **Restore** বোতামেই ফেরাতে পারবেন। Restore-এর কোড
                   (`restore()` → "visitreject") আগে থেকেই `history`-তে
                   `status="Active"` + `"Restored…"` লেখা বসায় — ডেটাবেসের
                   `tk_terminal_no_return` পাহারাদার ঠিক এটাই চায়। অর্থাৎ
                   **পাহারাদারকে পাশ কাটানো হচ্ছে না**, তার নিজের নিয়মেই ফেরানো।

                   ⛔ কোড **নিজে থেকে কারো বাতিল করা কার্ড খোলে না** — শুধু
                      লুকিয়ে থাকা কার্ডটা মানুষের চোখের সামনে আনে; ফেরানোর
                      সিদ্ধান্ত আগের মতোই মানুষের।
                   ⛔ কোনো সারি তৈরি/মোছা/বদল হচ্ছে না — এটা শুধু **কোন তালিকায়
                      দেখাবে** তার নিয়ম।
                   ⛔ নিচের `Treatment` (স্বাভাবিক active) নিয়মে হাত পড়েনি —
                      সেখানে `cancelled` আগে থেকেই বাদ ছিল, তাই একই সারি
                      দু'জায়গায় যেতে পারে না। */
                /* 🔵🔒 V717 (নিজে গভীরে যাচাই করে ধরা, TK-নির্দেশ "কোন ভাল কাজ
                   যেন খারাপ না হয়"): যাঁর টাকা **ফেরত (Refund) অনুমোদিত** হয়ে
                   গেছে, তিনি আগে থেকেই আলাদা **"Refunded"** তালিকায় আছেন।
                   তাঁকে এখানেও দেখালে **একই মানুষ দুই তালিকায়** পড়তেন, আর
                   কেউ ভুল করে Refund-হওয়া রোগীকে Restore করে ফেলতে পারতেন।
                   ⛔ তাই শুধু এঁরাই বাদ; বাকি সব লুকানো কার্ড আগের মতোই দেখাবে। */
                stage == "Treatment" && status == "cancelled" &&
                    !hasApprovedRefundByMobile.contains(mobKeyForAge) -> visitRejectRows.add(row)
                stage == "Treatment" && status == "incomplete" -> notCompleteRows.add(row)
                // 🔴🔒 V681 (২৫.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট যুক্তি — "Advance দিলেই
                // অটোমেটিক Patient(Treatment) হয়ে যায়, তাহলে যে কখনো Advance-ই
                // দেয়নি সে আবার 'Incomplete Patient'-এ যাবে কেন") — আগে (V645)
                // এই ৬০-দিন-নিয়মের সারি "Incomplete Patient"-এ যেত, যদিও এরা
                // stage="Patient" (Visit) — কখনো Treatment-এ প্রমোটই হয়নি (Advance
                // ছাড়া promoteFollowUpToTreatment() কখনো ডাকা হয় না)। এখন এরা
                // Visit Reject-এ যায় (TK-এর স্পষ্ট পছন্দ) — সত্যিকারের বাতিল করা
                // Visit-এর মতোই ধরা হয়। ⛔ Treatment-Incomplete-এর আসল নিয়ম
                // (উপরের লাইন) অপরিবর্তিত।
                autoIncomplete60Day -> visitRejectRows.add(row)
                // 🟢🔒 V644 — বাকি সব "Treatment" সারি (স্বাভাবিক active) —
                // এই একই `when`-এ, তাই ওপরের কোনো শর্তে ইতিমধ্যে ধরা পড়া সারি
                // (Incomplete) এখানে দ্বিতীয়বার আসে না।
                stage == "Treatment" && status !in setOf("cancelled", "incomplete", "rejected", "closed") ->
                    runningTreatmentRows.add(row)
            }
        }
        val enqReject = dedupByMobile(enqRejectRows).map { entry(it, "enqreject", "Rejected") }.toMutableList()
        val visitReject = dedupByMobile(visitRejectRows).map { entry(it, "visitreject", "Visit Reject") }.toMutableList()
        val notComplete = dedupByMobile(notCompleteRows).map {
            val m = it.s("mobile").filter { c -> c.isDigit() }.takeLast(10)
            val b = patientByMobile[m]?.optDouble("bill", 0.0) ?: 0.0
            entry(it, "notcomplete", "Incomplete", bill = b, paid = paidByMobile[m] ?: 0.0,
                  pat = patientByMobile[m],   // 🆕 V850
                  billBeforeDiscount = patientByMobile[m]?.optDouble("billBeforeDiscount", 0.0) ?: 0.0)   // 🏷️ V1508
        }.toMutableList()
        // 🟢🔒 V621 — "cancelled" bucket-এর ঠিক পাশে, সম্পূর্ণ আলাদা tab id
        // ("returnvisit") ও লেবেল ("Return Visit")।
        val returnVisit = dedupByMobile(returnVisitRows).map { entry(it, "returnvisit", "Return Visit") }.toMutableList()
        // 🔴🔒 এই স্ন্যাপশট নিচের নতুন (হাল-নাগাদ) সংকেত-ভিত্তিক Return Visit
        // এন্ট্রি যোগ হওয়ার **আগেই** নেওয়া — My Enquiry-র deadMobiles নিয়ম
        // (নিচে, TK-এর আগের ইচ্ছাকৃত সিদ্ধান্ত: "Refunded" থাকলেও My Enquiry-
        // তে দেখাবে) যেন নতুন যোগ হওয়া Refunded-ভিত্তিক Return Visit সারির
        // কারণে ভেঙে না যায়।
        val returnVisitTagOnlyMobiles = returnVisit.mapNotNull {
            it.mobile.filter { c -> c.isDigit() }.takeLast(10).ifBlank { null }
        }.toHashSet()
        // 🟢🔒 V644 — নতুন, স্বাধীন tab id ("runningtreatment")।
        /* 🔴🔒 V1508 (১৬.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — MD ANARUL HOWK, KNE-
           07082026-001, Due ₹0 · history-তে "Treatment Complete" তবু
           "Running Patient (223)"-এ দেখাচ্ছিল): বিল বসানো থাকলে ও বাকি
           ইতিমধ্যে ₹0 (পুরো টাকা জমা/ছাড়ে মিটে গেছে) — এই রোগী আর "Running"
           নন, তিনি নিচের `complete` bucket-এই আছেন (একই ফাইলে, একই
           bill−paid নিয়মে, ~লাইন ১২১৮)। এখানে দ্বিতীয়বার দেখালে Draft-এ
           ডবল-লিস্টিং হয় (TK-এর প্রথম অভিযোগ)।
           ⛔ বিল এখনো বসানো হয়নি এমন (bill<=0) রোগী বাদ পড়েন না — `complete`
              bucket-এরই `bill > 0.0` পাহারা এখানে হুবহু ব্যবহার হলো, যাতে
              বিল-না-বসানো মাঝ-চিকিৎসার রোগী ভুল করে Running থেকে বাদ না যান। */
        val runningTreatment = dedupByMobile(runningTreatmentRows).mapNotNull {
            val m = it.s("mobile").filter { c -> c.isDigit() }.takeLast(10)
            val pat = patientByMobile[m]
            val b = pat?.optDouble("bill", 0.0) ?: 0.0
            val p = paidByMobile[m] ?: 0.0
            val due = (b - p).coerceAtLeast(0.0)
            if (b > 0.0 && due <= 0.0) return@mapNotNull null
            entry(it, "runningtreatment", "Running Treatment", bill = b, paid = p,
                  pat = pat,   // 🆕 V850
                  billBeforeDiscount = pat?.optDouble("billBeforeDiscount", 0.0) ?: 0.0)   // 🏷️ V1508
        }.toMutableList()

        // 🟢🔒 V646 — Unexpected Time Calls এখন `unexpectedTimeRows`
        // (followups টেবিল থেকে, মূল লুপে জমা করা) থেকে বানানো হয় —
        // dedupByMobile-এর একই প্রমাণিত নিয়মে ডুপ্লিকেট এড়ানো হয়।
        val unexpectedTime = dedupByMobile(unexpectedTimeRows).map { row ->
            val m = row.s("mobile").filter { it.isDigit() }.takeLast(10)
            val pat = patientByMobile[m]
            val bill = pat?.optDouble("bill", 0.0) ?: 0.0
            val paid = paidByMobile[m] ?: 0.0
            val status = if (pat == null) {
                "Enquiry only — not registered"
            } else {
                val due = (bill - paid).coerceAtLeast(0.0)
                when {
                    bill <= 0.0 -> "Registered — no bill yet"
                    due <= 0.0 -> "✅ Treatment complete"
                    else -> "Registered — treatment ongoing"
                }
            }
            val by = row.s("receivedBy").ifBlank { row.s("createdBy") }
            // 🔒 TK-এর লক করা নিয়ম (খাতার সারি B96-এ আবার ধরা পড়ল, ছবিসহ):
            // *"By: ঘরে সবসময় স্টাফের নাম দেখাবে, কখনো কাঁচা মোবাইল নম্বর নয়।"*
            val byName = FollowUpModel.prettyStaff(by)
            entry(row, "unexpected", "$status${if (byName.isNotBlank()) " · by $byName" else ""}",
                  bill = bill, paid = paid, pat = pat,   // 🆕 V850
                  billBeforeDiscount = pat?.optDouble("billBeforeDiscount", 0.0) ?: 0.0)   // 🏷️ V1508
        }.toMutableList()

        val complete = mutableListOf<DraftEntry>()
        // TK-REQUESTED (2026-07-27), ধাপ ৩খ — Draft পর্দা: this walked EVERY
        // patients row, so a person with a duplicate registration could be
        // listed twice here (and be judged on the wrong row's bill). It now uses
        // the same single row per person the rest of the app uses, chosen by the
        // ONE shared rule above. With a single row nothing changes.
        for ((mobKey, chosenRow) in patientByMobile) {
            val row = chosenRow
            val bill = row.optDouble("bill", 0.0)
            if (bill <= 0.0) continue
            val mob = mobKey
            val paid = paidByMobile[mob] ?: 0.0
            val due = (bill - paid).coerceAtLeast(0.0)
            // TK-REQUESTED ADDITION (2026-07-24): a patient with real Due
            // left CAN still land here if Master has approved a "Complete
            // despite Due" request (completeApprovedBy set) -- the Due
            // itself is NEVER changed/zeroed by this, it's shown honestly
            // in the label so Reports/collections stay accurate; this only
            // affects which Draft bucket the patient is grouped into.
            val approvedDespiteDue = row.s("completeApprovedBy").isNotBlank()
            val billBefore = row.optDouble("billBeforeDiscount", 0.0)   // 🏷️ V1508
            if (due == 0.0) {
                complete.add(entry(row, "complete", "Paid ${paid.toLong()}", bill = bill, paid = paid, billBeforeDiscount = billBefore))
            } else if (approvedDespiteDue) {
                complete.add(entry(row, "complete", "Paid ${paid.toLong()} · Due ${due.toLong()} (Master-approved)", bill = bill, paid = paid, billBeforeDiscount = billBefore))
            }
        }

        // 🔴 TK-নির্দেশ (02.08.2026): Advance/Treatment টাকা সম্পূর্ণ Refund হয়ে
        // গেলে (Approved Refund আছে ও নেট জমা ঠিক ₹0) — এখানে দেখানো হয়, Enquiry/
        // Visit/Patient কোনো কার্ডেই আর না (FollowUpRepository.fetchTab()-এর
        // stage=="Treatment" ফিল্টারে একই শর্তে বাদ)। ⛔ `complete`-এর মতো
        // bill>0 শর্ত এখানে নেই — Bill না বসিয়েই Advance নিয়ে থাকলেও (এখনো
        // Refund হয়নি) এই শর্ত মেলে না (হয় Refund নেই, নয়তো নেট জমা ০ নয়),
        // তাই সেই রোগী ভুল করে এখানে ধরা পড়বে না।
        val refunded = mutableListOf<DraftEntry>()
        for ((mobKey, chosenRow) in patientByMobile) {
            if (!hasApprovedRefundByMobile.contains(mobKey)) continue
            val row = chosenRow
            // 🔴 B302.1 (02.08.2026): TK হাতে করে ফিরিয়ে আনলে (`refundRestoredBy`
            // সেট থাকে, Complete-despite-Due-এর হুবহু একই প্যাটার্ন) — তখন এই
            // Draft ঘরে আর দেখানো হয় না (রোগী ততক্ষণে Patient কার্ডে ফিরে গেছে)।
            if (row.s("refundRestoredBy").isNotBlank()) continue
            val paid = paidByMobile[mobKey] ?: 0.0
            if (paid > 0.5) continue // পুরোপুরি ০ না হলে (এখনো কিছু জমা আছে) — বাদ, রোগী Patient কার্ডেই থাকবে
            refunded.add(entry(row, "refunded", "Refunded"))
        }

        // 🔴🔒 TK-নির্দেশ (১১.০৯.২০২৬, স্পষ্ট — "রিটার্ন করেছে সুতরাং সব জায়গা
        // থেকে রিটার্ন হতে হবে"): আসল দোষ — Return Visit তালিকা এতদিন শুধু
        // followups.status=="Returned" ট্যাগ দেখেই বানানো হত (উপরে লাইন
        // ~1073), আর সেই ট্যাগ বসে শুধু Patient Timeline-এর "Return Fees"
        // বোতাম থেকে, তাও `currentFollowupId` ফাঁকা থাকলে চুপচাপ ব্যর্থ হয়ে
        // যেত। ফল: টাকা সত্যিই ফেরত (Refund Approved, নেট জমা ₹0) হয়ে গেলেও
        // ট্যাগ না বসলে Return Visit-এ শূন্যই থেকে যেত।
        // ⛔ সমাধান: "Refunded" ঘরের এই একই বিশ্বস্ত সংকেত (hasApprovedRefund
        // ByMobile + নেট জমা ₹0, সরাসরি payments টেবিল থেকে) দিয়ে Return
        // Visit-ও বানানো হচ্ছে — কোন স্ক্রিন থেকে রিটার্ন হয়েছে তার ওপর আর
        // নির্ভর করে না। পুরনো ট্যাগ-ভিত্তিক সারি (উপরে) বাদ যায়নি, শুধু একই
        // মোবাইল দ্বিতীয়বার না ঢোকার জন্য চেক করা হচ্ছে।
        run {
            val returnVisitMobiles = returnVisit.mapNotNull {
                it.mobile.filter { c -> c.isDigit() }.takeLast(10).ifBlank { null }
            }.toHashSet()
            for ((mobKey, chosenRow) in patientByMobile) {
                if (!hasApprovedRefundByMobile.contains(mobKey)) continue
                if (mobKey in returnVisitMobiles) continue
                val row = chosenRow
                if (row.s("refundRestoredBy").isNotBlank()) continue
                val paid = paidByMobile[mobKey] ?: 0.0
                if (paid > 0.5) continue
                returnVisit.add(entry(row, "returnvisit", "Return Visit"))
                returnVisitMobiles.add(mobKey)
            }
        }

        /* 📊🔒🔒 V824 (২৯.০৮.২০২৬, TK-নির্দেশ) — "Yearly Registration"
           (শুধু মাস্টার, শুধু বাছা ব্রাঞ্চ, ০১ জানু → ৩১ ডিসে চলতি বছর)।

           TK-এর শর্ত হুবহু, একটাও নিজের মনগড়া নয়:
             ১) **শুধু বাছা ব্রাঞ্চ** — উপরের `branchPart` দিয়েই এই `patients`
                তালিকা আনা হয়েছে, তাই আলাদা কিছু করার নেই।
             ২) **এক নম্বরে একজন** — `patientByMobile` (প্রজেক্টের প্রমাণিত,
                একটাই শেয়ার-করা নিয়ম), নতুন কোনো নিয়ম বানানো হয়নি।
             ৩) **Refund ও Return Visit বাদ** — ঠিক উপরে তৈরি হওয়া সেই দুটো
                তালিকা থেকেই মোবাইল নেওয়া হয় (আলাদা করে নতুন শর্ত লেখা হয়নি,
                তাই দুই জায়গার হিসাব কখনো আলাদা হতে পারে না)।
             ৪) **নামে DEMO বা TEST থাকলে বাদ**।
             ৫) **মাস্টার নিজে হাতে বাদ দিলে বাদ** — সেই তালিকা এই ফোনে জমানো
                (`YearlyRegistration.cachedExcludedIds`), তাই এখানে **একটাও
                নতুন ক্লাউড-কল হয় না** (Egress এক বাইটও বাড়ে না)।

           ⛔ উপরের All / This Month / Custom Date ছাঁকনি এই ঘরে ইচ্ছাকৃতভাবে
              লাগানো হয় না (নিচে `filt()`-এর বাইরে রাখা) — TK চেয়েছেন সবসময়
              পুরো বছরের হিসাব।
           ⛔ কোনো রেকর্ড তৈরি/বদল/মোছা হয় না — শুধু গোনা। */
        // 🆕 V852 — "কতজন বাদ পড়ল ও কেন" (নিচের run-ব্লক ভরে দেয়)।
        var yearlyOutDemo = 0
        var yearlyOutNoDate = 0
        // 🆕🔒 V-YEARSW (১৬.০৯.২০২৬) — নিচের গোটা হিসাবটা **বছর-নিরপেক্ষ**
        // (declaredSeparate/pickPatientRow/demo/no-date/tag/skip-mark কোনোটাই
        // `year`-এর উপর নির্ভর করে না, শুধু শেষে `regDate`-এর বছর অনুযায়ী
        // সাজানো হয়) — তাই সব বছরের হিসাব **একবারেই** বানিয়ে ফেলা যায়, পরে
        // Yearly Registration পর্দায় বছর বদলালে নতুন করে গোনার দরকার নেই।
        val yearlyRegByYear = LinkedHashMap<String, MutableList<DraftEntry>>()
        val yearlyOutDemoByYear = HashMap<String, Int>()
        run {
            /* 🔄🔒 V852 (৩০.০৮.২০২৬, TK-অনুমোদিত) — TK: *"এটা কী করে সম্ভব
               তালিকায় দেখাবে আবার গুনবে না? তালিকায় যা দেখাবে গোনা যেন একই হয়।
               প্রয়োজনে আমি তালিকা থেকে স্কিপ করে দেব, তখন গোনা ঠিক হয়ে যাবে।"*
               ⇒ Return Visit ও Refund আর **আপনা-আপনি বাদ যায় না**; তালিকায়
                 থাকে, নামের পাশে `(Return Visit)` / `(Refund)` লেখা, আর
                 **গোনাতেও ধরা হয়**। মাস্টার চাইলে Skip করবেন। */
            val returnMobiles = HashSet<String>()
            for (e in returnVisit) {
                val m = e.mobile.filter { it.isDigit() }.takeLast(10)
                if (m.isNotBlank()) returnMobiles.add(m)
            }
            val refundMobiles = HashSet<String>()
            for (e in refunded) {
                val m = e.mobile.filter { it.isDigit() }.takeLast(10)
                if (m.isNotBlank()) refundMobiles.add(m)
            }
            val excludedIds = YearlyRegistration.cachedExcludedIds(context)

            /* 🔴🔒 V852 — TK: *"২ নম্বর নিয়মটা ঠিক নয়, এক নম্বরে দুজন থাকতে পারে"*।
               আগে `patientByMobile` ধরে **প্রতি নম্বরে একজনই** গোনা হত, তাই এক
               নম্বরে সত্যিকারের দুজন থাকলে একজন হারিয়ে যেতেন।
               এখন প্রজেক্টের **প্রমাণিত চিহ্ন** ব্যবহার হয়: স্টাফ নিজে
               "Different Patient — Same Mobile" চাপলে রোগীর সারির আইডি হয়
               `pat_<১০ সংখ্যা>_…` ধাঁচের (V516/V518)। সেই সারিগুলো আলাদা মানুষ
               হিসেবে গোনা হয়। ⛔ অন্য কোনো পথে এই ধাঁচ তৈরি হয় না, তাই ভুল করে
               দুবার রেজিস্টার হওয়া সারি এতে দুবার গোনা হবে না। */
            fun declaredSeparate(rowId: String, mobileDigits: String): Boolean {
                if (mobileDigits.length != 10) return false
                val prefix = "pat_" + mobileDigits + "_"
                return rowId.startsWith(prefix) && rowId.length > prefix.length
            }

            /* 🔴🔒 V853 (নিজের যাচাইয়ে ধরা পড়া দোষ, ৩০.০৮.২০২৬) — প্রথমে
               `patientByMobile` (যেটা `pickPatientRow` দিয়ে বাছা) থেকে শুরু
               করেছিলাম। কিন্তু `pickPatientRow` "আলাদা রোগী" চিহ্নটা চেনে না —
               তাই এক নম্বরে [সাধারণ সারি + আলাদা-ঘোষিত সারি] থাকলে সে
               আলাদা-ঘোষিত সারিটাকেই বেছে নিতে পারত, আর তখন **সাধারণ মানুষটাই
               গোনা থেকে বাদ পড়ে যেতেন** (একজন বাড়ার বদলে একজন হারাত)।
               এখন দুটো ভাগ আলাদা করা হয়: প্রতিটা আলাদা-ঘোষিত সারি নিজে গোনা
               হয়, আর বাকি সাধারণ সারিগুলো থেকে প্রজেক্টের প্রমাণিত নিয়মেই
               (`pickPatientRow`) একজন। ⇒ কেউ হারায় না, কেউ দুবারও গোনা হয় না। */
            val counted = ArrayList<JSONObject>()
            for ((mobKey, rowsForMobile) in patientRowsByMobile) {
                val plain = JSONArray()
                for (i in 0 until rowsForMobile.length()) {
                    val r = rowsForMobile.optJSONObject(i) ?: continue
                    if (declaredSeparate(r.s("id"), mobKey)) counted.add(r) else plain.put(r)
                }
                PatientIdentity.pickPatientRow(plain, branchFilter.orEmpty())?.let { counted.add(it) }
            }

            /* 🆕 V852 — TK: *"কতজন বাদ পড়ল ও কেন, সেটা দেখতে চাই"*।
               তালিকাতেও নেই, গোনাতেও নেই — শুধু এই দুটো কারণে।
               🆕🔒 V-YEARSW — "তারিখ নেই" রোগীর কোনো বছরই নেই (তাই বছর-
               নিরপেক্ষ, একটাই মোট); "ডেমো/টেস্ট" যে বছরের তারিখেই থাকুক
               সেই বছরের নিচে গোনা হয় (`yearlyOutDemoByYear`)। */
            for (row in counted) {
                val mobKey = row.s("mobile").filter { it.isDigit() }.takeLast(10)
                val regDate = YearlyRegistration.regDateOf(row)
                if (regDate.length < 4) { yearlyOutNoDate++; continue }
                val rowYear = regDate.take(4)
                if (YearlyRegistration.isDemoName(row.s("name"))) {
                    yearlyOutDemoByYear[rowYear] = (yearlyOutDemoByYear[rowYear] ?: 0) + 1
                    continue
                }
                // মাস্টার নিজে বাদ দিয়ে থাকলে সারিটা **তালিকা থেকে সরে যায় না** —
                // দাগ দেওয়া থাকে (`extra = SKIPPED`), যাতে কাটা দাগে দেখা যায় ও
                // "Undo" চেপে ফেরানো যায়। গোনায় ধরা হয় শুধু দাগ-ছাড়া সারিগুলো।
                val marked = if (excludedIds.contains(row.s("id"))) YearlyRegistration.SKIP_MARK else ""
                val tag = when {
                    refundMobiles.contains(mobKey) -> YearlyRegistration.TAG_REFUND
                    returnMobiles.contains(mobKey) -> YearlyRegistration.TAG_RETURN
                    else -> ""
                }
                /* 💰 V1209 (TK: *"কত বিল · কত জমা · কত বাকি · কত কত তারিখে কত কত
                   জমা"*) — তিনটেই এই ফাংশনে **আগে থেকেই** আছে: বিল রোগীর নিজের
                   সারিতে, জমা ও জমার তালিকা উপরের `payments` লুপে।
                   ⛔ একটাও নতুন ক্লাউড-পড়া লাগেনি (Egress এক বাইটও বাড়ে না)। */
                /* 🧾 V1210 (TK-নির্দেশ: *"একটা কলমের তারিখ আর তারপরের কলমে সেই
                   তারিখে কত জমা করেছে"*) — তাই এখানে **যন্ত্র-পাঠযোগ্য** ধাঁচ:
                   `dd/MM/yyyy=টাকা|dd/MM/yyyy=টাকা`। শিট বানানোর সময় এটাই
                   ভেঙে আলাদা আলাদা কলমে বসে। ⛔ এই লেখাটা কোনো পর্দায় দেখানো
                   হয় না, শুধু শিটের জন্য। */
                val hist = payHistByMobile[mobKey].orEmpty()
                    .sortedBy { it.first }
                    .joinToString("|") { (d, amt) ->
                        (if (d.isBlank()) "-" else DateUtil.display(d)) + "=" +
                            String.format(java.util.Locale.US, "%.0f", amt)
                    }
                val refTxt = listOf(row.s("refBy"), row.s("refDoctor"))
                    .map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(" — ")
                yearlyRegByYear.getOrPut(rowYear) { mutableListOf() }.add(
                    entry(row, "yearlyreg", marked,
                        bill = row.optDouble("bill", 0.0),
                        paid = paidByMobile[mobKey] ?: 0.0)
                    .copy(recordDate = regDate, lastRemark = "", regTag = tag,
                        payHistory = hist, refByText = refTxt))
            }
            for (k in yearlyRegByYear.keys.toList()) {
                yearlyRegByYear[k] = yearlyRegByYear[k]!!
                    .sortedWith(compareByDescending<DraftEntry> { it.recordDate }.thenBy { it.name })
                    .toMutableList()
            }
        }
        val yearlyCurrentYear = YearlyRegistration.currentYear()
        val yearlyReg: List<DraftEntry> = yearlyRegByYear[yearlyCurrentYear] ?: emptyList()
        yearlyOutDemo = yearlyOutDemoByYear[yearlyCurrentYear] ?: 0
        // 🆕🔒 V-YEARSW — সব বছরের হিসাব এই ফোনের মেমোরিতে জমা (YearlyRegistration.kt-এর
        // মন্তব্য দ্রষ্টব্য) যাতে বিস্তারিত পর্দায় বছর বদলালে নতুন করে গোনার/পড়ার দরকার না হয়।
        val yearlySnapshotByYear = yearlyRegByYear.mapValues { (yr, yrRows) ->
            YearlyRegistration.YearSnapshot(yrRows, yearlyOutDemoByYear[yr] ?: 0, yearlyOutNoDate)
        }.toMutableMap()
        if (!yearlySnapshotByYear.containsKey(yearlyCurrentYear)) {
            yearlySnapshotByYear[yearlyCurrentYear] = YearlyRegistration.YearSnapshot(emptyList(), 0, yearlyOutNoDate)
        }
        YearlyRegistration.cacheAllYears(branchFilter.orEmpty().ifBlank { "All" }, yearlySnapshotByYear)
        val yearlyAvailableYears = yearlySnapshotByYear.keys.sortedDescending()

        // 🟢🔒🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ) — My Enquiry এখন সব বাকেট
        // তৈরি হওয়ার পরে বানানো হয়, দুইটা কারণে:
        //  ১) `followByMobile`-এর সাথে জোড়া হয়ে **বর্তমান অবস্থা** দেখায়
        //     (Enquiry/Visit/Patient কার্ড — যা-ই এখন সত্যি, তাই দেখায়)।
        //  ২) TK-এর নির্দেশ: "মৃত-ঘোষিত" (Enquiry Reject/Visit Reject/
        //     Return Visit/Incomplete Patient/Complete Patient) তালিকায়
        //     ইতিমধ্যে থাকা মানুষ My Enquiry থেকে বাদ যাবেন। ⛔ Refunded/
        //     Running Treatment TK এই তালিকায় রাখেননি (নির্দেশে ছিল না),
        //     তাই সেগুলোয় থাকলেও My Enquiry-তেও দেখাবেন — এটা ইচ্ছাকৃত।
        val deadMobiles = HashSet<String>()
        for (e in enqReject) if (e.mobile.isNotBlank()) deadMobiles.add(e.mobile.filter { it.isDigit() }.takeLast(10))
        for (e in visitReject) if (e.mobile.isNotBlank()) deadMobiles.add(e.mobile.filter { it.isDigit() }.takeLast(10))
        deadMobiles.addAll(returnVisitTagOnlyMobiles)
        for (e in notComplete) if (e.mobile.isNotBlank()) deadMobiles.add(e.mobile.filter { it.isDigit() }.takeLast(10))
        for (e in complete) if (e.mobile.isNotBlank()) deadMobiles.add(e.mobile.filter { it.isDigit() }.takeLast(10))
        for (i in 0 until receivedRows.length()) {
            val enqRow = receivedRows.getJSONObject(i)
            val m = enqRow.s("mobile").filter { it.isDigit() }.takeLast(10)
            if (m.isNotBlank() && deadMobiles.contains(m)) continue
            val live = followByMobile[m]
            if (live != null) {
                val bill = patientByMobile[m]?.optDouble("bill", 0.0) ?: 0.0
                val paid = paidByMobile[m] ?: 0.0
                received.add(entry(live, "received", bill = bill, paid = paid,
                                   pat = patientByMobile[m]))   // 🆕 V850
            } else {
                received.add(entry(enqRow, "received"))
            }
        }

        fun filt(list: List<DraftEntry>): List<DraftEntry> {
            if (from == null && to == null) return list
            return list.filter {
                val d = it.recordDate
                if (d.isBlank()) return@filter false
                (from == null || d >= from) && (to == null || d <= to)
            }
        }
        // ⛔ `yearlyReg` ইচ্ছাকৃতভাবে `filt()`-এর বাইরে — উপরের তারিখ-ছাঁকনি
        //    এই ঘরে লাগে না (TK: সবসময় ০১ জানু → ৩১ ডিসে, পুরো বছর)।
        val result = DraftBuckets(filt(received), filt(enqReject), filt(visitReject), filt(notComplete), filt(complete), filt(unexpectedTime), filt(refunded), filt(returnVisit), filt(runningTreatment), yearlyReg, yearlyOutDemo, yearlyOutNoDate, yearlyAvailableYears)   // 🆕 V852 / V-YEARSW
        saveCachedBuckets(branchFilter, from, to, result)
        return result
    }

    /** 🔴 TK-নির্দেশ (02.08.2026, B302.1): "Refunded" ঘর থেকে রোগীর রেকর্ড
     *  Delete — `PatientTimelineActivity.showDeletePatientDialog()`-এর হুবহু
     *  একই অনুমতি-নিয়ম (Master সরাসরি; Staff শুধু আজ/গতকালের রেকর্ড, তাও
     *  Chamber বন্ধ না হলে; নইলে Master-এর অনুমতি লাগবে) ও একই মোছার পথ
     *  (Trash Bin-এ, Follow-up cascade সহ) — আলাদা কোনো নতুন লজিক নয়, শুধু
     *  Draft-এর এই তালিকা থেকেও একই কাজটা করা যায়। কল-সাইট (DraftListActivity)
     *  অনুমতি না থাকলে "PERMISSION" ফেরত পেয়ে Master-কে অনুরোধ পাঠানোর
     *  পপ-আপ দেখাবে (deleteEnquiry-র প্যাটার্নেই)। */
    fun deletePatientRecord(e: DraftEntry, user: NativeUser): String {
        if (!DeletePermission.canDeleteEntryNow(context, user, e.recordDate, e.branch, paid = true)) return "PERMISSION"
        if (e.id.isBlank() && e.mobile.isBlank()) return "NOT_FOUND"
        val rows = if (e.id.isNotBlank()) {
            val byId = SupabaseClient.fetchListSlim("patients", "id=eq.${e.id}", 1,
                    SupabaseClient.PATIENT_NO_PHOTO_COLS)   // 🔴 V794 — ছবি ছাড়া
            if (byId.length() > 0) byId else SupabaseClient.findByMobile("patients", e.mobile, "*", 1)
        } else {
            SupabaseClient.findByMobile("patients", e.mobile, "*", 1)
        }
        if (rows.length() == 0) return "NOT_FOUND"
        val ok = TrashHelper.moveToTrashWithFollowupCascade("patients", rows.getJSONObject(0), user.mobile, e.mobile)
        return if (ok) "OK" else "NETWORK"
    }

    /** TK-REQUESTED ADDITION (2026-07-18), Phase 1: same-day self-delete for
     *  the "My Enquiry" list. Permission is re-checked here too (not just in
     *  the adapter) so this can never be called successfully out of turn.
     *  Moves the record into the existing Trash Bin (Master can Restore it
     *  from there exactly like any other trashed record) instead of a hard
     *  delete — nothing is ever unrecoverable.
     *
     *  TK-REQUESTED (2026-07-19): returns a specific reason instead of a
     *  plain true/false, so the screen can tell the user the REAL cause
     *  (no permission / record already gone / network problem) instead of
     *  one generic "check connection or permission" message every time. */
    fun deleteEnquiry(e: DraftEntry, user: NativeUser, bypassPermission: Boolean = false): String {
        // 🆕 TK-নির্দেশ (03.08.2026) — Enquiry/Visit Reject তালিকায় (কোনো টাকা
        // জড়িত নয়) স্টাফ যেকোনো সময় নিজেই ডিলিট করতে পারবেন — কল-সাইট
        // (DraftListActivity) শুধু enqreject/visitreject তালিকাতেই এই ছাড়
        // পাঠায়, অন্য কোনো তালিকার (notcomplete/complete) আচরণ বদলায়নি।
        if (!bypassPermission && !TrashHelper.canDelete(user, e.recordDate, e.createdByMobile)) return "PERMISSION"
        if (e.id.isBlank()) return "NOT_FOUND"
        val rows = SupabaseClient.fetchList("enquiries", "id=eq.${e.id}", 1)
        if (rows.length() > 0) {
            val record = rows.getJSONObject(0)
            // 🔴 TK-REPORTED (01.08.2026, Enquiry Reject List — "ডিলিট করলে
            // পরে আবার ফিরে আসছে"): এই পথটা এতদিন plain moveToTrash()
            // ব্যবহার করত, যা শুধু enquiries সারিটা মুছত — এই নম্বরের
            // Cancelled/Incomplete followups সারি (যেখান থেকে Reject/
            // Visit-Reject/Incomplete তালিকা তৈরি হয়) কখনো ছোঁয়া হত না,
            // তাই নামটা ওই তালিকায় থেকেই যেত। এখন B108/V215-এর প্রমাণিত
            // cascade ফাংশন ব্যবহার হচ্ছে (Timeline-এর Delete যেটা আগে
            // থেকেই ব্যবহার করে) — একই নম্বরের সব followups+enquiries সারি
            // একসাথে Trash-এ যায় ও tombstone হয়, তাই আর কোনো তালিকাতেই
            // ফিরে আসবে না। Restore করলে সব হুবহু আগের অবস্থায় ফেরে।
            val ok = TrashHelper.moveToTrashWithFollowupCascade("enquiries", record, user.mobile, e.mobile)
            // Also clear this device's local cache/pending-queue copy (if
            // any) so a slightly-stale local view can't bring it back.
            if (ok) {
                context?.let { ctx ->
                    LocalWorkflowStore(ctx).removeEnquiry(e.id)
                    EnquiryRepository(ctx).removePendingById(e.id)
                }
            }
            return if (ok) "OK" else "NETWORK"
        }
        // TK-FOUND RISK FIX (2026-07-18): the enquiry hadn't reached the
        // cloud yet (still local-only, e.g. saved offline moments ago) --
        // the cloud fetch above finds nothing, so Delete used to just fail
        // here even though the record genuinely exists on this device.
        // Fall back to the local cache: move that copy to Trash instead,
        // then clear it from both the local cache and the pending-sync
        // queue so BottomNav's next retry can't re-upload something that
        // was just deleted.
        val ctx = context ?: return "NETWORK"
        val local = LocalWorkflowStore(ctx).pendingEnquiries()
        for (i in 0 until local.length()) {
            val row = local.getJSONObject(i)
            if (row.optString("id") == e.id) {
                val ok = TrashHelper.moveToTrashWithFollowupCascade("enquiries", row, user.mobile, e.mobile)
                if (ok) {
                    LocalWorkflowStore(ctx).removeEnquiry(e.id)
                    EnquiryRepository(ctx).removePendingById(e.id)
                }
                return if (ok) "OK" else "NETWORK"
            }
        }
        // 🔵 B609 (10.08.2026, TK-রিপোর্ট "Visit Reject delete করলে ভুতুড়ে ফিরে
        // আসে"): এই তালিকাগুলো (Visit Reject/Reject) followups টেবিলের Cancelled
        // সারি থেকে তৈরি — কার্ডের id হলো followups-এর id, যা enquiries টেবিলে
        // থাকে না (বিশেষত "Registered patient/Visit created", যাদের আলাদা enquiries
        // সারি নেই)। আগে তখন NOT_FOUND হয়ে শুধু এই ফোনে লুকাত (আসল cloud-delete/
        // tombstone নয়), তাই পরের auto-refresh সারিটা আবার টেনে এনে "ফিরিয়ে" দিত।
        // এখন enquiries না পেলে সরাসরি followups সারিটা ধরে **একই প্রমাণিত cascade**
        // (উপরের enquiries/patients-পথের হুবহু একই ফাংশন): ঐ মোবাইলের সব followups+
        // enquiries Trash-এ + DeletedGuard tombstone, আসল সারি snapshot-সহ Trash-এ
        // যায়; Master Restore করলে upsertRestoreSafe দিয়ে হুবহু আগের অবস্থায় ফেরে
        // (table="followups" restore-এ সমর্থিত — যাচাই করা)। ⛔ নতুন কোনো মোছার
        // লজিক নয়; over-delete-এর ঝুঁকিও নতুন নয় (mobile-cascade আগের enquiries-
        // পথেই ছিল, এক-নম্বর-এক-সেকশন নিয়মে সুরক্ষিত)।
        val fRows = try { SupabaseClient.fetchList("followups", "id=eq.${e.id}", 1) }
            catch (_: Throwable) { org.json.JSONArray() }
        if (fRows.length() > 0) {
            val frecord = fRows.getJSONObject(0)
            val ok = TrashHelper.moveToTrashWithFollowupCascade("followups", frecord, user.mobile, e.mobile)
            if (ok) {
                context?.let { c ->
                    LocalWorkflowStore(c).removeEnquiry(e.id)
                    EnquiryRepository(c).removePendingById(e.id)
                }
            }
            return if (ok) "OK" else "NETWORK"
        }
        return "NOT_FOUND"
    }

    /** Restore an entry back to the exact section it came from. For the three
     *  follow-up lists we simply clear the Cancelled/Incomplete status back to
     *  Active, so the record reappears in its Follow-up tab (web
     *  restoreDraftEntry). For a Complete patient we re-activate the matching
     *  Treatment follow-up. Returns true on success. */
    fun restore(e: DraftEntry, user: NativeUser? = null): Boolean {
        return try {
            when (e.tab) {
                "enqreject" -> {
                    // 🔴🔒 V448 (19.08.2026): the Inquiry live-list now also honours
                    // the append-only Reject/Restore history. Therefore a *real* Restore
                    // must leave an equally durable Active marker and must reactivate
                    // every same-mobile Inquiry sibling + the matching enquiry row.
                    // Otherwise one old Cancelled sibling/enquiry could immediately hide
                    // the restored card again. No row is created or deleted here.
                    val d = e.mobile.filter { it.isDigit() }.takeLast(10)
                    if (d.length != 10) return false
                    val rows = SupabaseClient.findByMobileOrNull(
                        "followups", d, "id,mobile,stage,status,history", 50
                    ) ?: return false
                    val now = isoNow()
                    val who = user?.let { it.mobile.ifBlank { it.name } }.orEmpty()
                    var touched = false
                    var allOk = true
                    for (i in 0 until rows.length()) {
                        val row = rows.getJSONObject(i)
                        if (!row.s("stage").equals("Inquiry", true)) continue
                        val id = row.s("id")
                        if (id.isBlank()) continue
                        val history = row.optJSONArray("history") ?: JSONArray()
                        history.put(JSONObject()
                            .put("date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                            .put("time", now)
                            .put("remark", "Restored from Reject List")
                            .put("staff", who)
                            .put("status", "Active")
                            .put("decisionVersion", "V448"))
                        val fields = JSONObject().put("status", "Active").put("history", history).put("updatedAt", now)
                        val ok = SupabaseClient.updateById("followups", id, fields)
                        if (!ok) context?.let { GenericUpdateQueue.queue(it, "followups", id, fields) }
                        touched = true
                        allOk = allOk && ok
                    }
                    // Old Reject logic also marked enquiries.status. Clear that durable
                    // marker only as part of this explicit Restore action.
                    val enqs = SupabaseClient.findByMobileOrNull("enquiries", d, "id,status,stage", 50) ?: return false
                    for (i in 0 until enqs.length()) {
                        val erow = enqs.getJSONObject(i)
                        val id = erow.s("id")
                        if (id.isBlank()) continue
                        val fields = JSONObject().put("status", "Active").put("updatedAt", now)
                        val oldStage = erow.s("stage").trim().lowercase()
                        if (oldStage in setOf("cancelled", "rejected", "closed")) fields.put("stage", "Inquiry")
                        val ok = SupabaseClient.updateById("enquiries", id, fields)
                        if (!ok) context?.let { GenericUpdateQueue.queue(it, "enquiries", id, fields) }
                        allOk = allOk && ok
                    }
                    touched && allOk
                }
                "visitreject", "notcomplete", "returnvisit" -> {
                    if (e.id.isBlank()) return false
                    // V452: a real Restore must carry a durable Active decision in
                    // history.  The cloud terminal guard uses this marker to tell an
                    // intentional Restore apart from an old/stale device trying to
                    // overwrite Cancelled/Incomplete back to Active.
                    val now = isoNow()
                    val current = try { SupabaseClient.fetchList("followups", "id=eq.${e.id}", 1, select = "id,history") }
                        catch (_: Throwable) { JSONArray() }
                    if (current.length() == 0) return false
                    val row = current.getJSONObject(0)
                    val history = row.optJSONArray("history") ?: JSONArray()
                    val who = user?.let { it.mobile.ifBlank { it.name } }.orEmpty()
                    history.put(JSONObject()
                        .put("date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                        .put("time", now)
                        .put("remark", when (e.tab) {
                            "visitreject" -> "Restored from Visit Reject List"
                            "returnvisit" -> "Restored from Return Visit List"
                            else -> "Restored from Incomplete List"
                        })
                        .put("staff", who)
                        .put("status", "Active")
                        .put("decisionVersion", "V452"))
                    val fields = JSONObject().put("status", "Active").put("history", history).put("updatedAt", now)
                    val ok = SupabaseClient.updateById("followups", e.id, fields)
                    if (!ok) context?.let { GenericUpdateQueue.queue(it, "followups", e.id, fields) }
                    ok
                }
                "complete" -> {
                    val mob = e.mobile.filter { it.isDigit() }.takeLast(10)
                    // TK-REPORTED CLASS OF BUG (fixed 2026-07-27): this asked
                    // for the patient's follow-up rows but findByMobile()
                    // returns only ONE unless a count is given -- and that one
                    // row is often the wrong stage. Restoring a Completed
                    // patient then said "Restored" while the card never came
                    // back. Now every row of this person is re-activated, the
                    // same as the other three Draft sections already do.
                    /* 🔵🔒 V536: Restore-এ এক নম্বরের **সব** সারি Active/Treatment হয়ে
                       যেত — এক নম্বরে দু'জন থাকলে অন্যজনের কার্ডও। এখন প্রমাণসহ
                       অন্যের সারি বাদ। ⛔ প্রমাণ না থাকলে হুবহু আগের আচরণ।
                       ⛔ বাছাইয়ের তিনটে ঘরই শুধু যোগ হলো (ছোট, সারিও কম)। */
                    val rows = SupabaseClient.findByMobile("followups", mob, "id,stage,refId,patientId,name", 50)
                    var ok = false
                    for (i in 0 until rows.length()) {
                        val fr = rows.getJSONObject(i)
                        val id = fr.s("id")
                        if (id.isBlank()) continue
                        if (PatientIdentity.provablyOtherPatient(fr, mob, e.id, e.patientId, e.name)) continue
                        val fields = JSONObject().put("status", "Active").put("stage", "Treatment").put("updatedAt", isoNow())
                        val rowOk = SupabaseClient.updateById("followups", id, fields)
                        if (!rowOk) context?.let { GenericUpdateQueue.queue(it, "followups", id, fields) }
                        ok = rowOk || ok
                    }
                    ok
                }
                // 🔴 TK-নির্দেশ (02.08.2026, B302.1): "Refunded" ঘর থেকে হাতে
                // ফিরিয়ে আনা — completeApprovedBy-এর হুবহু একই প্যাটার্ন
                // (PatientTimelineActivity.approveCompleteDespiteDue দ্রষ্টব্য)।
                // ⛔ কোনো টাকার হিসাব/Refund রেকর্ড ছোঁয়া হয় না — শুধু এই একটা
                // ফ্ল্যাগ, যেটা FollowUpRepository-র exclusion উল্টে দেয়।
                "refunded" -> {
                    if (e.id.isBlank()) return false
                    val who = user?.mobile?.ifBlank { "unknown" } ?: "unknown"
                    val fields = JSONObject().put("refundRestoredBy", who).put("updatedAt", isoNow())
                    val ok = SupabaseClient.updateById("patients", e.id, fields)
                    if (!ok) context?.let { GenericUpdateQueue.queue(it, "patients", e.id, fields) }
                    ok
                }
                else -> false
            }
        } catch (e: Exception) { false }
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
}
