package com.tkbiswas.pilesclinic.native

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CollectionRow(
    val source: String,
    val date: String,
    val name: String,
    val mobile: String,
    val branch: String,
    val mode: String,
    val amount: Double,
    // 🆔 TK-এর নিয়ম (28.07.2026): নাম ও মোবাইলের সঙ্গে Patient ID-ও দেখাতে হবে।
    // ডিফল্ট ফাঁকা — মেডিসিন/ওয়াক-ইন সারিতে রোগীর ID নাও থাকতে পারে, তখন
    // কিছুই বাড়তি দেখানো হয় না (খালি লেবেল কখনো নয়)।
    val patientId: String = "",
    // 🔒 B575 (08.08.2026, TK-অনুমোদিত প্রুফ): Payment Collection সারিতে নামের পাশে
    // রোগ (chip) ও নিচে ঠিকানা দেখাতে। ডিফল্ট ফাঁকা — না থাকলে দেখানো হয় না।
    // ⛔ শুধু দেখানোর তথ্য; টাকা/collection-এর কোনো লজিক এতে বদলায় না।
    val disease: String = "",
    val address: String = "",
    // 🔵 B612 (10.08.2026, TK-অনুমোদিত): পেমেন্ট কখন হয়েছে — কার্ডে ছোট করে
    // সময় (createdAt থেকে, "3:42 PM" ধাঁচে)। ডিফল্ট ফাঁকা — না থাকলে দেখায় না।
    val time: String = "",
    /* 🔴🔒 V565 (TK, ২২.০৮.২০২৬): *"কোন পেসেন্ট কোন টাইমে পয়সা দিলো সেটা আমার
       আগে দরকার · সকাল ১১টায় যে পেশেন্ট প্রথম টাকা দিয়েছে তার নাম যেন প্রথমে
       থাকে"* — উপরের `time` ঘরটা শুধু **দেখানোর লেখা** ("4:52 PM"), ওটা দিয়ে
       সাজানো যায় না (১১টা আর ৪টা-র লেখার ক্রম উল্টো আসে)। তাই টাকা জমার
       **আসল সময়টা** (createdAt) এখানে আলাদা করে রাখা হয়।
       ⛔ ঐচ্ছিক — পুরোনো/স্থানীয় সারিতে না থাকলে `time` লেখা থেকেই সময় বার
          করা হয় (`sortKeyOf`), তাই কোনো সারি হারায় না। */
    val paidAt: String = "",
    /* 🔴🔒 V566 (TK, ২২.০৮.২০২৬): *"এখানেও যদি কোন আরএমপির পেশেন্ট হয়ে থাকে
       অথবা আনএক্সপেক্টেড টাইমের পেশেন্ট হয়ে থাকে সেটাও যেন শো করে"* —
       রোগীর সারিতে তথ্যগুলো আগে থেকেই আছে (`refBy` · `refDoctor` · `timeType`),
       তাই নতুন কোনো কলাম লাগেনি; শুধু কার্ড পর্যন্ত আনা হল।
       ⛔ ফাঁকা হলে কার্ডে কিছুই বসে না। */
    val refBy: String = "",
    val refDoctor: String = "",
    val timeType: String = "",
    // 🔒 V452 WORKING (19.08.2026, TK-approved): এক রোগী + এক calendar day =
    // এক Treatment Payment। Cash/Online একই দিনের এক row-তে যোগ হলেও দুটো
    // হিসাব আলাদা থাকে। পুরনো row-এ এই ঘর না থাকলে mode+amount থেকেই safely
    // infer করা হয়; Medicine/Visit Fee-ও একই summary helper ব্যবহার করতে পারে।
    val cashAmount: Double = 0.0,
    val onlineAmount: Double = 0.0,
    // একই দিনের কতগুলো আসল money-event এই display row-এ জোড়া আছে। শুধু
    // History-তে ambiguous old 3-tap correction আটকাতে ব্যবহৃত; amount বদলায় না।
    val paymentEventCount: Int = 1,
    /* 🐞🔒 V1322 (TK-রিপোর্ট, ১১.০৯.২০২৬: *"পেমেন্ট টা কোন staff নিয়েছে ...
       সেটা জরুরী"*) — সারিতে `receivedBy` (স্টাফের মোবাইল) আগে থেকেই ছিল,
       শুধু নামে বদলে কার্ডে দেখানো হতো না। ডিফল্ট ফাঁকা — না পাওয়া গেলে
       আগের মতোই কিছু দেখায় না (patient/টাকার কোনো হিসাব বদলায়নি)। */
    val staff: String = ""
)

// TK-REQUESTED ADDITION (2026-07-24): Visit Fee visibility -- unlike the
// followups-row gaps (self-healed elsewhere, PatientTimelineRepository/
// FollowUpRepository), a missing Visit Fee payment's actual amount is
// genuinely unrecoverable if the original write never reached the cloud --
// guessing an amount would be dangerous, so this is surfaced for Master to
// check/re-collect manually instead of being silently self-healed.
data class MissingVisitFee(
    val patientRowId: String,
    val name: String,
    val mobile: String,
    val branch: String,
    val patientId: String,
    val registrationDate: String
)

data class PatientBillInfo(
    val id: String,
    val name: String,
    val mobile: String,
    val branch: String,
    val patientId: String,
    val bill: Double,
    val paid: Double,
    val billLocked: Boolean,
    // 🔵 TK-ORDER (07.08.2026): রোগী পাওয়া গেছে কিন্তু **payments পড়া ব্যর্থ** হলে
    // true — তখন `paid` (তাই Due-ও) ভরসাযোগ্য নয়, ₹0 দেখানো ভুল। ডিফল্ট false,
    // তাই এই ক্লাসের বাকি সব ব্যবহার এক অক্ষরও বদলায়নি। শুধু Add-Payment ফর্ম
    // এটা দেখে সতর্কবার্তা দেয়; টাকা/রিফান্ডের হিসাব অপরিবর্তিত (paid=0 হলে refund
    // এমনিতেই আটকে থাকে — fail-safe)।
    val paymentsUnverified: Boolean = false,
    // 🔒 B562 (08.08.2026, TK-অনুমোদিত প্রুফ): Add Treatment Payment কার্ডে
    // রোগের নাম (🩸 Piles ইত্যাদি) দেখানোর জন্য — রোগীর রেকর্ডের "disease" থেকে
    // আসে। ডিফল্ট ফাঁকা, তাই এই ক্লাসের বাকি সব ব্যবহার (যেখানে দেওয়া হয়নি)
    // এক অক্ষরও বদলায়নি; ফাঁকা হলে কার্ডে চিপটাই দেখায় না।
    val disease: String = "",
    // 🔒 B563 (08.08.2026, TK-নির্দেশ): কার্ডে ID+মোবাইলের নিচে রোগীর ঠিকানা
    // দেখাতে (গ্লোবাল দুই-লাইন নিয়ম B554)। ডিফল্ট ফাঁকা; ফাঁকা হলে দেখায় না।
    val address: String = "",
    // 🔴🔴 V509 (২১.০৮.২০২৬, TK-এর স্পষ্ট সিদ্ধান্ত — *"হ্যাঁ, সবাই পারবে —
    // Visit Fee-ও ফেরতের সীমায় ধরা হবে"*): এই রোগীর **Visit Fee / Registration
    // Fee**-এর যোগফল, ইতিমধ্যে ফেরত-হয়ে-যাওয়া অংশ বাদ দিয়ে।
    //
    // কেন আলাদা ঘরে, `paid`-এর সঙ্গে মিশিয়ে নয় (এটাই সবচেয়ে জরুরি) —
    //   `paid` দিয়ে **Due (বাকি টাকা)** হিসাব হয় (`Due = Bill − paid`), আর
    //   Visit Fee চিকিৎসার বিলের অংশ **নয়**। মিশিয়ে দিলে প্রত্যেক রোগীর Due
    //   Visit Fee-র সমান কমে যেত — অর্থাৎ টাকার হিসাব ভুল হয়ে যেত।
    // ⇒ তাই Visit Fee এখানে **আলাদা** থাকে; শুধু **Refund-এর সীমা** ঠিক করার
    //   সময় (`refundableTotal`) দুটো যোগ হয়।
    // ⚠️ সৎ কথা: Bill · Due · Collection · রিপোর্ট — কেউই এই ঘরটা পড়ে না, তাই
    //   এই ঘর যোগ করায় সেখানে কিছু বদলায়নি। **একটাই ব্যতিক্রম** আছে, সেটা
    //   `PaymentRepository`-র হিসাবের শেষে খোলাখুলি লেখা — পেমেন্ট-মুছে-ফেলা
    //   কিন্তু ফেরত-থেকে-যাওয়া রোগীর Due এখন শুধরে যায়।
    // ডিফল্ট 0.0, তাই এই ক্লাসের বাকি সব ব্যবহার এক অক্ষরও বদলায়নি।
    val visitFeePaid: Double = 0.0
) {
    /**
     * 🔴 V509 — **সর্বোচ্চ কত টাকা ফেরত দেওয়া যায়** (চিকিৎসার জমা + Visit Fee),
     * দুটোই ইতিমধ্যে অনুমোদিত ফেরত বাদ দেওয়া। শুধু Refund-এর পাহারায় ব্যবহার হয়।
     * ⛔ Due-র হিসাবে এটা কখনো ব্যবহার হয় না — সেখানে আগের মতোই শুধু `paid`।
     */
    val refundableTotal: Double get() = paid + visitFeePaid
}

/** V217 (§B216, 31.07.2026): saveRefund-এর সৎ ফলাফল — শুধু true/false-এর
 *  বদলে ব্যর্থতার আসল কারণটাও (নেট সমস্যা বনাম জমার চেয়ে বেশি) সঙ্গে যায়,
 *  যাতে UI-তে ভুল "Success" কখনো না দেখায়। */
data class RefundResult(val success: Boolean, val message: String)

// TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow -- a
// Staff picks a collection date other than today, and it needs Master's
// approval before it becomes a real payment. This holds one pending (or
// resolved) request; the real "payments" row only gets created once
// approved (approveBackdateRequest in PaymentRepository).
data class BackdateRequest(
    val id: String,
    val patientRowId: String,
    val patientCode: String,
    val mobile: String,
    val name: String,
    val branch: String,
    val billAmount: Double,
    val amount: Double,
    val mode: String,
    val remarks: String,
    val requestedDate: String,
    val requestedBy: String,
    val requestedByName: String,
    val requestedAt: String,
    val status: String,
    // 🔴🔒 V1442 (১৩.০৯.২০২৬, TK-নির্দেশ) — "treatment" (আগের সব ব্যাকডেট
    // পেমেন্ট, ডিফল্ট) বা "refund" (নতুন — Refund-এরও ব্যাকডেট চাওয়া গেলে)।
    // Master-এর Approve বোতাম এটা দেখেই ঠিক করে কোন সারিতে পরিণত হবে।
    val payType: String = "treatment"
)

// TK-REQUESTED ADDITION (2026-07-25): "Edit Payment Amount" approval
// workflow -- a payment can be corrected freely on its OWN date or the
// very next day; after that, editing needs Master's approval (same
// request -> approve/reject shape as BackdateRequest above, but this one
// corrects an EXISTING payment's amount instead of creating a new one).
data class PaymentEditRequest(
    val id: String,
    val paymentId: String,
    val patientRowId: String,
    val patientCode: String,
    val mobile: String,
    val name: String,
    val branch: String,
    val oldAmount: Double,
    val newAmount: Double,
    val mode: String,
    val paymentDate: String,
    val reason: String,
    val requestedBy: String,
    val requestedByName: String,
    val requestedAt: String,
    val status: String
)

object PaymentModel {

    // TK-REQUESTED (2026-07-25): shared by every screen that lets someone
    // edit an existing payment's amount (Report Card, Chamber Attendance
    // review, ...) so the "today or the next day" rule is defined ONCE and
    // can never drift between screens. True for the payment's own date and
    // exactly one day after it; false from the day after that onward.
    fun withinFreeEditWindow(paymentDateIso: String): Boolean {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val payDate = fmt.parse(paymentDateIso.take(10)) ?: return false
            val today = fmt.parse(today()) ?: return false
            val diffDays = ((today.time - payDate.time) / (24 * 60 * 60 * 1000))
            diffDays in 0..1
        } catch (_: Exception) { false }
    }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    /* 🕒🔒 V1106 (০৫.০৯.২০২৬, TK-নির্দেশ — SADDAM-এর একই দিনে দুবার
       পেমেন্ট): *"তারিখের পাশে সময় রাখতে হবে সমস্ত জায়গায়, তাহলে বোঝা যাবে যে
       অরিজিনাল না ডুপ্লিকেট"*।

       ⛔ **সময়টা যেভাবে জমা আছে ঠিক সেভাবেই পড়া হয়** — একটাও ঘণ্টা এদিক-ওদিক
          করা হয় না। কারণ উপরের `isoNow()` ফোনের **নিজের ঘড়ির** সময় লিখে শেষে
          শুধু `Z` অক্ষরটা বসায় (TK-এর লক করা নিয়ম, ছোঁয়া বারণ)। তাই ওটাকে
          UTC ধরে হিসাব করলে সময় **৫ ঘণ্টা ৩০ মিনিট এগিয়ে** দেখাত।
          🔴 ঠিক এই ভুলটাই মিশ্র পেমেন্টের ভাঙা-দেখার পপ-আপে ছিল — এখানেই সারানো।
       ⛔ ধাঁচ `5:40 PM` — Payment Collection কার্ডে (B612) **আগে থেকেই** এই ধাঁচেই
          সময় দেখাচ্ছে, তাই নতুন কোনো তৃতীয় ধাঁচ বানানো হয়নি; দুই পর্দায় দুরকম
          সময় দেখানোর ঝুঁকিটাই সবচেয়ে বড়। AM/PM বড় হাতে (TK-এর নিয়ম B76)।
       ⛔ সময় জানা না গেলে ফাঁকা ফেরে ⇒ ডাকনেওয়ালা শুধু তারিখটুকুই দেখায়,
          কখনো ভুল বা আন্দাজি সময় নয়। */
    fun clockOf(isoRaw: String?): String {
        val t = (isoRaw ?: "").trim()
        if (t.length < 16 || t[10] != 'T') return ""
        val hh = t.substring(11, 13).toIntOrNull() ?: return ""
        val mi = t.substring(14, 16)
        if (mi.toIntOrNull() == null) return ""
        val ap = if (hh < 12) "AM" else "PM"
        var h12 = hh % 12
        if (h12 == 0) h12 = 12
        return "$h12.$mi $ap"   // 🔴 V1158 — TK: "3.15 PM"
    }

    /** 🕒 V1106 — টাকার সারির "তারিখ + সময়"।
     *  ⛔ সময়টা **শুধু তখনই** জোড়া হয় যখন সেটা ওই তারিখেরই — নইলে ব্যাকডেট করা
     *     পেমেন্টে পুরনো তারিখের পাশে **আজকের** সময় বসে যেত, যা সম্পূর্ণ ভুল বার্তা।
     *  ⛔ সময় না থাকলে/না মিললে হুবহু আগের মতোই শুধু তারিখ। */
    fun dayAndClock(dateRaw: String?, isoRaw: String?): String {
        val day = DateUtil.display(dateRaw)
        val d10 = (dateRaw ?: "").take(10)
        val i10 = (isoRaw ?: "").take(10)
        val clock = if (d10.isNotBlank() && d10 == i10) clockOf(isoRaw) else ""
        return if (clock.isBlank()) day else "$day : $clock"   // 🔴 V1158 — "31/12/2026 : 3.15 PM"
    }

    fun normalizeMode(mode: String): String =
        if (mode.trim().uppercase().contains("UPI") || mode.trim().uppercase().contains("ONLINE")) "ONLINE" else "CASH"

    /** Matches collectionPaymentLabel() -- what to call this row in the list. */
    fun sourceLabel(payType: String, remarks: String): String {
        val t = payType.lowercase()
        // 🔴 V509 (২১.০৮.২০২৬) — `isVisitFeeRow`-এর ঠিক একই ফাঁদ এখানেও ছিল:
        // "Refund" যাচাইটা নিচে থাকায়, স্টাফ কারণ হিসেবে "VISIT FEE REFUND"
        // লিখলে সেই ফেরতের সারিটা তালিকায় **"Visit Fee"** নামে দেখাত।
        // ⛔ টাকার কোনো হিসাব এতে বদলাত না (শুধু নামের লেবেল), তবু ভুল নাম
        //    দেখানোও বিভ্রান্তি — তাই "Refund" যাচাইটা সবার আগে তোলা হলো।
        if (t == "refund") return "Refund"   // V216 (§13), V217-এ No-Bengali guard-এর জন্য ইংরেজি করা হলো
        val r = remarks.lowercase()
        if (t == "registration" || t == "visitfee" || t == "visit_fee" || r.contains("visit fee")) return "Visit Fee"
        if (t == "medicine") return "Medicine Payment"
        if (t == "bill_edit") return "Bill Edited"
        return "Treatment Payment"
    }

    // V216 (§13): এই সব refund-চেনার সহায়ক — এক জায়গায় নিয়ম, যাতে হিসাব ও
    // দেখানো কখনো আলাদা না হয়।
    const val REFUND_APPROVED = "approved"
    const val REFUND_PENDING = "pending"
    const val REFUND_REJECTED = "rejected"

    /** একটা payments row কি refund? */
    fun isRefundRow(row: JSONObject): Boolean =
        row.optString("payType", "").equals("refund", ignoreCase = true)

    /** refund কি সত্যিই approved (Master সরাসরি করলে, বা staff-request Master approve করলে)?
     *  ⛔ শুধু approved refund-ই collection/paid total থেকে কমবে। pending/rejected কমবে না। */
    fun isApprovedRefund(row: JSONObject): Boolean =
        isRefundRow(row) && row.optString("refundApprovalStatus", "").equals(REFUND_APPROVED, ignoreCase = true)

    /**
     * 🔴 V509 (২১.০৮.২০২৬, TK-সিদ্ধান্ত) — একটা payments সারি কি **Visit Fee /
     * Registration Fee**?
     *
     * ⛔ নতুন কোনো নিয়ম বানানো হয়নি — শর্তগুলো হুবহু সেই একই যেগুলো দিয়ে
     *    `isTreatmentPaymentRow()` (PaymentRepository) ও `isOrdinalTreatment
     *    Payment()` এই সারিগুলোকে **বাদ** দেয়। এক জায়গায় লেখা থাকল, তাই
     *    "কোনটা Visit Fee" নিয়ে দুই জায়গায় দুই উত্তর কখনো হবে না।
     */
    fun isVisitFeeRow(payType: String, remarks: String = ""): Boolean {
        val t = payType.lowercase()
        // 🔴🔴🔴 V509 নিজের যাচাইয়ে ধরা পড়েছে (২১.০৮.২০২৬) — **টাকা-নষ্ট করা বাগ,
        // পাঠানোর আগেই ধরা পড়ল।**
        //
        // নিচের `remarks`-এর শর্তটা লেখার ঘর দেখে — আর **Refund সারির `remarks`
        // হলো স্টাফের নিজের হাতে লেখা কারণ** (`buildRefundRow`: `.put("remarks",
        // reason.ifBlank { "Refund" })`)। তাই কোনো স্টাফ কারণ হিসেবে সবচেয়ে
        // স্বাভাবিক কথাটাই — **"VISIT FEE REFUND"** — লিখলে সেই ফেরতের সারিটাকেই
        // এই ফাংশন "Visit Fee জমা" বলে চিনত। ফল হত ভয়ানক:
        //   • ফেরতের টাকাটা **বিয়োগের বদলে যোগ** হয়ে যেত,
        //   • রোগীর ফেরতযোগ্য টাকা প্রতিবার **বেড়ে** যেত (৫০০ ফেরত দিলে সীমা ৫০০ বাড়ত),
        //   • Payment পর্দার Due কমে গিয়ে **আসল বাকি টাকা লুকিয়ে** যেত,
        //   • আর Reports/Timeline অন্য হিসাব দেখাত — দুই পর্দায় দুই সংখ্যা।
        //
        // ⇒ **Refund সারি কোনোদিনই "Visit Fee জমা" নয়** — টাকা আসছে না, যাচ্ছে।
        //   পাহারাটা এখানেই (এক জায়গায়) বসানো হলো, ডাকার জায়গায় নয় — যাতে
        //   ভবিষ্যতে কেউ নতুন জায়গা থেকে ডাকলেও একই ভুল আর কখনো না হয়।
        if (t == "refund") return false
        val r = remarks.lowercase()
        return t == "registration" || t == "visitfee" || t == "visit_fee" ||
            r.contains("visit fee") || r.contains("registration fee")
    }

    /** Treatment/daily-payment split. New rows carry cashAmount/onlineAmount;
     *  legacy rows are inferred from their old mode+amount without rewriting DB. */
    fun paymentSplit(row: JSONObject): Pair<Double, Double> {
        val total = row.optDouble("amount", 0.0)
        val hasCash = row.has("cashAmount") && !row.isNull("cashAmount")
        val hasOnline = row.has("onlineAmount") && !row.isNull("onlineAmount")
        if (hasCash || hasOnline) {
            val cash = row.optDouble("cashAmount", 0.0).coerceAtLeast(0.0)
            val online = row.optDouble("onlineAmount", 0.0).coerceAtLeast(0.0)
            // Old/partially-migrated row safety: if both split values are zero but
            // amount is positive, never hide money — infer from the old mode.
            if (cash <= 0.0 && online <= 0.0 && total > 0.0) {
                return if (normalizeMode(row.s("mode")) == "ONLINE") 0.0 to total else total to 0.0
            }
            return cash to online
        }
        return if (normalizeMode(row.s("mode")) == "ONLINE") 0.0 to total else total to 0.0
    }

    fun splitMode(cash: Double, online: Double): String = when {
        cash > 0.0 && online > 0.0 -> "MIXED"
        online > 0.0 -> "ONLINE"
        else -> "CASH"
    }

    fun paymentBreakdown(cash: Double, online: Double): String = when {
        cash > 0.0 && online > 0.0 -> "₹${"%,.0f".format(cash)} CASH + ₹${"%,.0f".format(online)} ONLINE"
        online > 0.0 -> "₹${"%,.0f".format(online)} ONLINE"
        else -> "₹${"%,.0f".format(cash)} CASH"
    }

    fun parsePaymentRow(row: JSONObject): CollectionRow {
        val split = paymentSplit(row)
        val eventCount = row.optJSONArray("dailyEvents")?.length()?.coerceAtLeast(1) ?: 1
        return CollectionRow(
            source = sourceLabel(row.s("payType").ifBlank { "treatment" }, row.s("remarks")),
            date = row.s("date"),
            name = row.s("name"),
            mobile = row.s("mobile"),
            branch = row.s("branch"),
            mode = splitMode(split.first, split.second),
            amount = row.optDouble("amount", 0.0),
            // "patientCode" = মানুষের পড়ার Patient ID (KNE-0012)।
            // ⚠️ "patientId" কলামটা নয় — ওটায় রোগীর সারির ভিতরের আইডি থাকে।
            patientId = row.s("patientCode"),
            // 🔵 B612: পেমেন্টের সময় (createdAt → "3:42 PM")।
            time = displayTime12(row.s("createdAt")),
            paidAt = row.s("createdAt"),                     // 🔵 V565 — সাজানোর জন্য
            cashAmount = split.first,
            onlineAmount = split.second,
            paymentEventCount = eventCount,
            // 🐞🔒 V1322 — receivedBy (না থাকলে createdBy) থেকে স্টাফের নাম।
            // StaffDirectory-তে না পেলে মোবাইল নম্বরটাই দেখায়, একদম ফাঁকা নয়।
            staff = row.s("receivedBy").ifBlank { row.s("createdBy") }.let { mob ->
                if (mob.isBlank()) "" else StaffDirectory.findAccount(mob)?.name?.takeIf { it.isNotBlank() } ?: mob
            }
        )
    }

    /* ═══════════════════════════════════════════════════════════════════════
       🔴🔴 V487 (20.08.2026, TK-রিপোর্ট ছবিসহ) — টাকা ফেরতের দিনে **Cash-এর ঘর
       ভুল সংখ্যা** দেখাচ্ছিল।

       প্রমাণ (২০.০৮.২০২৬, কিষাণগঞ্জ): মোট দেখাচ্ছিল ₹-23,600 (ঠিক), কিন্তু
       Cash-এর ঘরে ₹24,400 (ভুল) — অথচ ওই দিনের তিনটে সারি ছিল
       −14,000 · −10,000 · +400, অর্থাৎ ক্যাশও −23,600 হওয়ার কথা।

       আসল কারণ: refund সারিতে শুধু `amount`-কে বিয়োগ (−) করা হত, কিন্তু
       `cashAmount` / `onlineAmount` ঘর দুটো **যোগ (+) থেকেই যেত**। মোট হিসাব
       amount ধরে হয় বলে ঠিক থাকত, আর Cash/Online-এর ঘর cashAmount ধরে হয়
       বলে ফেরত-দেওয়া টাকা উল্টে **যোগ** হয়ে যেত।

       এখন তিনটে ঘরই একসাথে বিয়োগ হয় — তাই মোট আর Cash/Online সবসময় মিলবে।
       ⛔ ক্লাউডের কোনো সারি বদলায়নি; শুধু দেখানোর হিসাব ঠিক হল।
       ═══════════════════════════════════════════════════════════════════ */
    private fun negAmt(v: Double): Double = if (v == 0.0) 0.0 else -kotlin.math.abs(v)

    /** অনুমোদিত Refund সারি — amount · cashAmount · onlineAmount তিনটেই বিয়োগ। */
    fun parseApprovedRefundRow(row: JSONObject): CollectionRow {
        val base = parsePaymentRow(row)
        return base.copy(
            amount = negAmt(row.optDouble("amount", 0.0)),
            cashAmount = negAmt(base.cashAmount),
            onlineAmount = negAmt(base.onlineAmount)
        )
    }

    /**
     * V452: legacy same-day Treatment rows are grouped for DISPLAY/ACCOUNTING only.
     * No historical payment row is deleted or rewritten. Visit Fee, Medicine,
     * Refund and every non-treatment source stay exactly as separate rows.
     */
    fun mergeDailyTreatmentCollections(rows: List<CollectionRow>): List<CollectionRow> {
        if (rows.size < 2) return rows
        val out = mutableListOf<CollectionRow>()
        val grouped = LinkedHashMap<String, CollectionRow>()
        for (r in rows) {
            if (r.source != "Treatment Payment" || r.date.take(10).isBlank()) {
                out.add(r); continue
            }
            val mob = r.mobile.filter { it.isDigit() }.takeLast(10)
            val owner = if (r.patientId.isNotBlank()) "pid:${r.patientId}" else "mob:$mob|${r.branch.lowercase()}"
            val key = "$owner|${r.date.take(10)}"
            val old = grouped[key]
            if (old == null) {
                grouped[key] = r
            } else {
                val cash = old.cashAmount + r.cashAmount
                val online = old.onlineAmount + r.onlineAmount
                grouped[key] = old.copy(
                    amount = old.amount + r.amount,
                    mode = splitMode(cash, online),
                    cashAmount = cash,
                    onlineAmount = online,
                    paymentEventCount = old.paymentEventCount + r.paymentEventCount,
                    // Keep the latest visible time when old duplicate rows differ.
                    time = if (r.time.isNotBlank()) r.time else old.time,
                    /* 🔵 V565: এক রোগীর দুটো জমা জোড়া লাগলে **প্রথমবার কখন
                       দিয়েছেন** সেটাই ধরা হয় — TK-এর কথা "প্রথম টাকা দিয়েছে
                       তার নাম প্রথমে"। */
                    paidAt = earlierPaidAt(old.paidAt, r.paidAt)
                )
            }
        }
        out.addAll(grouped.values)
        return out
    }

    /** Same rule as [mergeDailyTreatmentCollections], but for raw payment JSON
     * used by the Payment-detail dialog. This is DISPLAY-only; original ids/rows
     * stay untouched in Supabase. `_displayEventCount` prevents an ambiguous old
     * one-amount/one-mode edit on a combined day. */
    fun mergeDailyTreatmentJsonForDisplay(rows: List<JSONObject>): List<JSONObject> {
        if (rows.size < 2) return rows
        val passthrough = mutableListOf<JSONObject>()
        val groups = LinkedHashMap<String, MutableList<JSONObject>>()
        for (r in rows) {
            if (!r.s("payType").equals("treatment", true) || r.s("date").take(10).isBlank()) {
                passthrough.add(r); continue
            }
            val mob = r.s("mobile").filter { it.isDigit() }.takeLast(10)
            val owner = r.s("patientId").ifBlank { r.s("patientCode") }.let {
                if (it.isNotBlank()) "pid:$it" else "mob:$mob|${r.s("branch").lowercase()}"
            }
            groups.getOrPut("$owner|${r.s("date").take(10)}") { mutableListOf() }.add(r)
        }
        val merged = groups.values.map { sameDay ->
            if (sameDay.size == 1 && (sameDay[0].optJSONArray("dailyEvents")?.length() ?: 1) <= 1) sameDay[0]
            else {
                val ordered = sameDay.sortedBy { it.s("createdAt").ifBlank { it.s("date") } }
                val latest = ordered.last()
                val copy = JSONObject(latest.toString())
                var total = 0.0; var cash = 0.0; var online = 0.0; var events = 0
                val remarks = mutableListOf<String>()
                for (r in ordered) {
                    total += r.optDouble("amount", 0.0)
                    val sp = paymentSplit(r); cash += sp.first; online += sp.second
                    events += (r.optJSONArray("dailyEvents")?.length() ?: 1).coerceAtLeast(1)
                    val stored = r.s("payLabel").ifBlank { r.s("paymentLabel") }
                    val human = typedPartOf(r.s("remarks"), stored).trim()
                    if (human.isNotBlank() && human !in remarks) remarks.add(human)
                }
                copy.put("amount", total)
                    .put("cashAmount", cash).put("onlineAmount", online)
                    .put("mode", splitMode(cash, online))
                    .put("remarks", remarks.joinToString(" | "))
                    .put("_displayEventCount", events.coerceAtLeast(1))
                copy
            }
        }
        // 🔴🔒 V1637 (TK-রিপোর্ট, ছবিসহ, ২০.০৯.২০২৬, CHAYAN ROY-র Payment
        // পর্দায় — "তারিখ উল্টোপাল্টা সাজানো কেন?") — কলার (PaymentActivity)
        // ঢোকার আগেই তালিকা সঠিকভাবে ভিজিটের `date` দিয়ে সাজিয়ে পাঠায়
        // (`list.sortByDescending { it.s("date") }`), কিন্তু এই ফাংশনের শেষ
        // লাইন সেটা আবার `createdAt` (এন্ট্রি করার তারিখ) দিয়ে re-sort করে
        // ফেলত — ঠিক Report Card-এর একই বাগ (V1636), এবার এখানে। ব্যাচ করে
        // পুরনো পেমেন্ট এন্ট্রি করলে ক্রম এলোমেলো দেখাত। ⛔ এই তালিকায় ততক্ষণে
        // marker-only সারি (chamber_expected ইত্যাদি, যাদের `date` ভবিষ্যতের
        // প্রত্যাশিত তারিখ হতে পারে) কলার আগেই বাদ দিয়ে দিয়েছে — তাই সরাসরি
        // `date` দিয়ে সাজানো নিরাপদ। `date` ফাঁকা থাকলেই (বিরল) `createdAt`
        // ফলব্যাক — আগের আচরণ অক্ষত।
        return (passthrough + merged).sortedByDescending { it.s("date").ifBlank { it.s("createdAt") } }
    }

    /** 🔵 B612 (10.08.2026): ISO createdAt ("...T15:42:10.123Z") থেকে ১২-ঘণ্টার
     *  সময় ("3:42 PM")। isoNow() ডিভাইসের নিজের সময়েই লেখে (IST), 'Z' শুধু
     *  অক্ষর — তাই পড়াও একই ধাঁচে, timezone-বদল লাগে না। ফাঁকা/ভুল হলে "" ফেরে। */
    /**
     * 🔴 V566: কার্ডে RMP-র চিপে কী লেখা হবে।
     * রেজিস্ট্রেশনে "Referred By" ডাক্তার/RMP হলে তবেই — Self/Online/Offline
     * ইত্যাদি হলে কিছুই নয়। ডাক্তারের নাম জানা থাকলে নামটাই দেখানো হয়,
     * নইলে শুধু "RMP"।
     */
    fun rmpTagOf(refBy: String, refDoctor: String): String {
        val by = refBy.trim()
        val isDoctor = by.equals("Dr. Visit", true) || by.equals("Dr Visit", true) ||
            by.equals("RMP", true) || by.contains("Doctor", true)
        if (!isDoctor && refDoctor.isBlank()) return ""
        val nm = refDoctor.trim()
        return if (nm.isNotBlank()) "RMP · " + nm.uppercase(Locale.US) else "RMP"
    }

    /** 🔴 V566: অফিসের সময়ের বাইরে আসা রোগী হলে চিপ। */
    fun unexpectedTagOf(timeType: String): String =
        if (timeType.trim().equals("Unexpected Time", true)) "অসময়" else ""

    /** দুটো সময়ের মধ্যে যেটা আগে। ফাঁকা হলে অন্যটা। */
    fun earlierPaidAt(a: String, b: String): String {
        if (a.isBlank()) return b
        if (b.isBlank()) return a
        return if (a <= b) a else b
    }

    /**
     * 🔴🔒 V565 (TK): সাজানোর চাবি — "yyyy-MM-dd HH:mm"।
     *
     * ১. আসল সময় (`paidAt`, যেমন "2026-08-22T11:05:20.000Z") থাকলে সেটাই।
     * ২. না থাকলে তারিখ + দেখানোর লেখা ("11:05 AM") থেকে ২৪-ঘণ্টার সময়।
     * ৩. কিছুই না থাকলে শুধু তারিখ, আর সময়ের জায়গায় "99:99" — যাতে সেই
     *    সারিগুলো ওই দিনের **শেষে** যায়, তালিকা থেকে হারিয়ে না যায়।
     */
    fun sortKeyOf(r: CollectionRow): String {
        val hm = hhmmOf(r)
        val day = if (r.paidAt.length >= 10) r.paidAt.substring(0, 10) else r.date
        return day + " " + hm
    }

    private fun hhmmOf(r: CollectionRow): String {
        if (r.paidAt.length >= 16 && r.paidAt[10] == 'T') return r.paidAt.substring(11, 16)
        val t = r.time.trim()
        if (t.isNotBlank()) {
            val m = Regex("^(\\d{1,2}):(\\d{2})\\s*([AaPp])").find(t)
            if (m != null) {
                var h = m.groupValues[1].toIntOrNull() ?: return "99:99"
                val mm = m.groupValues[2]
                val pm = m.groupValues[3].lowercase() == "p"
                if (h == 12) h = 0
                if (pm) h += 12
                return String.format(Locale.US, "%02d:%s", h, mm)
            }
        }
        return "99:99"
    }

    /**
     * 🔴 TK-এর নিয়ম: *"সকাল ১১টায় যে পেশেন্ট প্রথম টাকা দিয়েছে তার নাম যেন
     * প্রথমে থাকে"* ⇒ **আগের সময় উপরে, পরের সময় নিচে**।
     * একই সময়ে দুজন হলে নাম ধরে সাজানো হয়, যাতে ক্রম প্রতিবার এক থাকে।
     */
    fun sortByPaidTime(rows: List<CollectionRow>): List<CollectionRow> =
        rows.sortedWith(compareBy({ sortKeyOf(it) }, { it.name.uppercase() }))

    fun displayTime12(iso: String): String {
        if (iso.isBlank()) return ""
        /* 🕒 V1106 — এখন প্রথমেই একই একটাই নিয়ম (`clockOf`) দেখে, তাই Payment
           Collection কার্ড আর টাকার তালিকা কখনো দুরকম সময় দেখাতে পারে না।
           ⛔ না পড়তে পারলে হুবহু আগের দুটো পথই আগের মতো চলে — কিছু হারায়নি। */
        clockOf(iso).let { if (it.isNotBlank()) return it }
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(iso)
            SimpleDateFormat("h.mm a", Locale.US).format(parsed!!)
        } catch (_: Exception) {
            try {
                val hm = iso.substringAfter('T', "").take(5)   // "15:42"
                if (hm.length == 5) SimpleDateFormat("h.mm a", Locale.US).format(
                    SimpleDateFormat("HH:mm", Locale.US).parse(hm)!!
                ) else ""
            } catch (_: Exception) { "" }
        }
    }

    /** ⏰🔒 V835 (২৯.০৮.২০২৬, TK-নির্দেশ — *"LAST CALL 31/12/2026 : 3.15 PM"*):
     *  ঠিক উপরের `displayTime12()`-এরই লেখা, শুধু ঘণ্টা-মিনিটের মাঝে `:`-র
     *  বদলে `.` — অর্থাৎ `3:15 PM` → `3.15 PM`।
     *  ⛔ কেন আলাদা ফাংশন: `displayTime12()` অ্যাপের ২০ জায়গায় চলে (রসিদ ·
     *     পেমেন্ট · টাইমলাইন)। ওটায় হাত দিলে সব জায়গা বদলে যেত — তাই ছোঁয়া
     *     হয়নি। এটা **শুধু LAST CALL লাইনে** ব্যবহার হয়।
     *  ⛔ ধাঁচটা নতুন নয় — প্রজেক্টে `h.mm a` আগে থেকেই চলে
     *     (DoctorCheckupActivity V671 · PatientTimelineActivity · ওয়েবের
     *     `wlv1Ampm`), আর V543-এর নিজের নোটেই লেখা ছিল `12.30 PM`।
     *  ⛔ ফাঁকা ভিতরে = ফাঁকা বাইরে; `h:mm a`-তে একটাই `:` থাকে, তাই
     *     AM/PM বা অন্য কিছু নষ্ট হওয়ার সুযোগ নেই। */
    fun displayTime12Dot(iso: String): String = displayTime12(iso).replace(":", ".")

    // TK-REPORTED BUG FIX (2026-07-25): shared with PatientTimelineRepository
    // so it can recompute the true ordinal (Advance/2nd/3rd...) fresh from
    // the full payment history instead of trusting a possibly stale
    // save-time label. Same exact definition PaymentRepository's own
    // (private) isTreatmentPaymentRow already used to COUNT payments for
    // that save-time label -- kept identical so the two never disagree on
    // what counts as an ordinal treatment payment.
    /**
     * 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত) — **এক জায়গায় এক নিয়ম।**
     *
     * TK-এর নিয়ম: ১ম টাকা = `Advance`, তারপর `2nd Payment` · `3rd Payment` …
     * এবং **একই দিনে যতবারই টাকা নেওয়া হোক, নম্বর একটাই**।
     *
     * এই ফাংশনটা একটা রোগীর **টাকার সারির তালিকা** নিয়ে ঠিক করে দেয় কোন সারি
     * কোন নম্বরে পড়ে (`id` → নাম)। যে পর্দাগুলো রোগীর পুরো তালিকা আগে থেকেই
     * হাতে পায় (Payment History · Timeline · রসিদ ছাপা) তারা এটাই ব্যবহার করে,
     * তাই **পুরনো রেকর্ডেও নামটা নিজে থেকে ঠিক দেখায়**।
     *
     * ⛔ **ক্লাউডে একটাও বাড়তি অনুরোধ নেই** — যে তালিকা ওই পর্দা আগেই নামিয়েছে
     *    সেটাই ব্যবহার হয়।
     * ⛔ **ডেটাবেসে কিছু লেখা হয় না** — এটা শুধু দেখানোর সময়ের হিসাব।
     * ⛔ Registration/Visit Fee · Medicine · Marked Arrived — এগুলো নম্বরে ঢোকে
     *    না (`isOrdinalTreatmentPayment`-এর সেই একই পুরনো নিয়ম)।
     */
    fun dayBasedLabelById(rows: List<org.json.JSONObject>): Map<String, String> {
        val treat = rows.filter {
            isOrdinalTreatmentPayment(it.s("payType"), it.s("remarks"))
        }
        val days = treat.map { it.optString("date", "").take(10) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val out = HashMap<String, String>()
        for (r in treat) {
            val id = r.optString("id", "")
            if (id.isBlank()) continue
            val idx = days.indexOf(r.optString("date", "").take(10))
            if (idx >= 0) out[id] = ordinalPaymentLabel(idx + 1)
        }
        return out
    }

    /**
     * 🔵🔒 V523 (২২.০৮.২০২৬, TK-নির্দেশ) — **এটা কি সত্যিকারের টাকার সারি,
     * নাকি শুধু একটা চিহ্ন?**
     *
     * TK-এর কথা: *"যখন কেউ কোন পেমেন্টই করেনি, তাহলে এগুলো দেখানোর মানেটা কি?"*
     * (Reports → Today's Collection-এ `₹0 · Marked Expected` সারিগুলো)।
     *
     * `payments` টেবিলে কিছু সারি **টাকার সারি নয়** — শুধু একটা চিহ্ন:
     *   • `chamber_expected` — "আজ আসবেন" বলে দাগ দেওয়া (`amount = 0.0`,
     *     `PaymentModel.buildExpectedMarkRow`)
     *   • `attendance_mark`  — "রোগী এসেছেন" বলে দাগ দেওয়া
     *   • `bill_edit`        — শুধু বিলের অঙ্ক শুধরানোর নথি
     *
     * ⛔ এই তালিকাটা **নতুন কিছু নয়** — হুবহু একই তিনটে ধরন নিচের
     *    `isOrdinalTreatmentPayment()` আগে থেকেই বাদ দেয় (৪৭১ নং লাইন), আর
     *    `PatientTimelineRepository`-ও ঠিক এগুলোই বাদ দেয় (২৬.০৭.২০২৬-এর
     *    অডিটে ধরা বাগের ফিক্স)। তাই এটা প্রজেক্টের **চলতি নিয়মেরই** নাম দেওয়া।
     * ⛔ টাকার কোনো যোগফল এই ফাংশন বদলায় না — কোন সারি **দেখানো** হবে,
     *    শুধু সেটাই ঠিক করে।
     */
    fun isMarkerOnlyRow(payType: String): Boolean {
        val t = payType.trim().lowercase()
        return t == "chamber_expected" || t == "attendance_mark" || t == "bill_edit"
    }

    fun isOrdinalTreatmentPayment(payType: String, remarks: String = ""): Boolean {
        val t = payType.lowercase()
        val r = remarks.lowercase()
        if (t == "registration" || t == "visitfee" || t == "visit_fee" || r.contains("visit fee") || r.contains("registration fee")) return false
        if (t == "medicine" || t == "bill_edit" || t == "attendance_mark" || t == "chamber_expected") return false
        return t == "treatment" || t.isBlank()
    }

    /**
     * 🚨 TK-REPORTED, LIVE (27.07.2026, SADDAM / KNE-15072026-002 — TK's photo):
     * the Report Card's PROGRESS box was showing
     *     "Advance Payment — ₹10,000 · CASH · ₹1,000 · CASH | ₹400 · CASH"
     * -- three lines of the app's OWN payment text, which made that row far
     * taller than the others, while the next visit correctly showed "—".
     *
     * WHY IT HAPPENED: when no remark is typed, a payment stores its own label
     * as the remark, and the screens treat "remark == the label it was saved
     * with" as "nothing was written". But the Follow-up card's Advance saves the
     * words "Advance Payment" while the label itself is "Advance" -- the two did
     * not match, so it looked like a real staff-written treatment note and was
     * printed as Progress.
     *
     * THIS IS THE ONE PLACE that decides "is this remark just the app's own
     * automatic text?" -- the label it was saved with, any ordinal payment label
     * (Advance / 2nd Payment / 3rd Payment ...), and the fixed words the app
     * writes by itself. A remark a person actually typed is never matched here,
     * so no real Treatment Progress can ever be hidden by this.
     */
    fun isAutoPaymentRemark(remark: String, storedLabel: String): Boolean {
        val r = remark.trim()
        if (r.isBlank()) return true
        if (storedLabel.isNotBlank() && r.equals(storedLabel.trim(), ignoreCase = true)) return true
        val fixed = listOf(
            "Advance", "Advance Payment", "Visit Fee", "Registration Fee",
            "Treatment Payment", "Marked Arrived", "Marked Expected",
            // 🔴 B394-সংশোধন (04.08.2026, TK-নির্দেশে — "Close Chamber সতর্কতা"
            // ও "Report Card ছাপা" দুটোতেই এখন এই sentinel বাদ যাবে, TK-এর
            // দুটো সন্দেহই ঠিক ছিল): Registration/Visit তৈরির সময় সিস্টেম
            // নিজে থেকে বসানো ডিফল্ট স্টাব — স্টাফ নিজে কিছু লেখেননি।
            "Registered patient / Visit created"
        )
        if (fixed.any { r.equals(it, ignoreCase = true) }) return true
        // 🚨 TK-REPORTED, LIVE (29.07.2026, JONEKA BIBI / COB-28072026-002 —
        // TK-এর ছবি): Report Card-এর PROGRESS ঘরে ট্রিটমেন্টের বদলে লেখা ছিল
        //     "Advance Payment · Chamber ONLINE payment"
        // Chamber থেকে টাকা নিলে অ্যাপ নিজেই রিমার্কের ঘরে `Chamber CASH
        // payment` / `Chamber ONLINE payment` বসায় — কিন্তু উপরের তালিকায়
        // ওই কথাটা ছিল না, তাই সেটা মানুষের লেখা ভেবে ছাপা হয়ে যেত।
        if (Regex("^chamber( .+)? payment$", RegexOption.IGNORE_CASE).matches(r)) return true
        // অ্যাপের নিজের বসানো বাকি কথাগুলোও — টাকার অঙ্ক, টাকার ধরন,
        // বিল সংশোধনের লাইন, "(Chamber Attendance)" জাতীয় লেজ।
        if (Regex("^₹\\s?[\\d,]+(\\.\\d+)?$").matches(r)) return true
        if (Regex("^(cash|online|upi)$", RegexOption.IGNORE_CASE).matches(r)) return true
        /* 🔴🔒 V1451 (১৪.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — "Called via COB" Chamber
           Date-এর Treatment Progress-এ, TK: "এটা তো ট্রিটমেন্টের ঘর") — গভীরে
           দেখে পাওয়া গেল: এই ফাংশনই (V236, ০১.০৮.২০২৬) ঠিক এই কাজের জন্য
           বসানো হয়েছিল, কিন্তু পরে যোগ হওয়া অ্যাপ-নিজের-লেখা কথাগুলো (Dialer
           কল-লগ — B464/B602 · "আসার কথা" বাতিলের কারণ) এই তালিকায় কখনো
           যোগই হয়নি। সাথে এটাও ধরা পড়ল: নিচের পুরনো "bill corrected:" লাইনটা
           **আসল লেখার সাথেই মেলে না** (আসল লেখা `💰 Bill corrected ₹X → ₹Y
           by নাম` — কোলন নেই, ইমোজি আছে) — তাই বিল-সংশোধনের নোটও এতদিন এই
           পাহারা এড়িয়ে যেত। এখন তিনটেই ঠিক করা হলো। ⛔ মানুষের নিজের হাতে
           লেখা কোনো কথা এই প্যাটার্নে ভুল করে ধরা পড়বে না (প্রতিটা প্যাটার্নই
           অ্যাপের নিজের নির্দিষ্ট বাক্য-গঠন, কাকতালীয়ভাবে মেলার সম্ভাবনা নেই)। */
        if (Regex("^called via \\S+$", RegexOption.IGNORE_CASE).matches(r)) return true
        if (r.startsWith("আসার কথা বাতিল:")) return true
        // ⛔ শুরুর ইমোজি (💰) বাদ দিয়ে মেলানো — surrogate-pair-নির্ভর \p{So}
        // এড়ানো হলো, কারণ Java regex ডিফল্টে সেটা নির্ভরযোগ্যভাবে ধরে না।
        if (Regex("^bill corrected\\b.*$", RegexOption.IGNORE_CASE).matches(r.replaceFirst(Regex("^[^A-Za-z]+"), ""))) return true
        if (Regex("^marked (arrived|expected)\\b.*$", RegexOption.IGNORE_CASE).matches(r)) return true
        // "2nd Payment", "3rd Payment", "11th Payment" ... (with or without the
        // word Payment), exactly as ordinalPaymentLabel() builds them.
        return Regex("^\\d+(st|nd|rd|th)( payment)?$", RegexOption.IGNORE_CASE).matches(r)
    }

    /**
     * 🚨 TK-REPORTED, LIVE (29.07.2026, JONEKA BIBI — TK-এর ছবি) · খাতার সারি B59
     *
     * TK-এর নিয়ম: **Report Card-এর PROGRESS ঘরে কেবল "রোগীকে কী ট্রিটমেন্ট করা
     * হলো" তা-ই থাকবে** — অ্যাপের নিজের বসানো টাকার কথা কোনোদিন নয়।
     *
     * আগে ছাঁকনিটা **গোটা লেখাটা** একটা নামের তালিকার সঙ্গে মিলিয়ে দেখত। কিন্তু
     * অ্যাপের নিজের দুটো কথা যখন `·` দিয়ে জোড়া লেগে **একটাই লেখা** হয়ে যায়
     * (যেমন `Advance Payment · Chamber ONLINE payment`), তখন গোটা লেখাটা
     * তালিকার কোনো নামের সঙ্গেই মেলে না — তাই আস্ত লেখাটা ট্রিটমেন্ট সেজে
     * ছাপা হয়ে যেত।
     *
     * এখন লেখাটাকে `·` / `|` ধরে **টুকরো করে প্রতিটা টুকরো আলাদা করে** দেখা হয়:
     * অ্যাপের নিজের টুকরোগুলো বাদ যায়, **মানুষের হাতে লেখা টুকরোই কেবল থাকে**।
     * তাই ভবিষ্যতে অ্যাপ নতুন কোনো কথা বসালেও, বা কথাগুলো জোড়া লাগলেও,
     * ট্রিটমেন্টের ঘরে টাকার কথা আর ঢুকতে পারবে না।
     *
     * ⛔ মানুষের লেখা কোনো কথা এখানে কখনো বাদ পড়ে না — শুধু অ্যাপের নিজের
     *    বসানো টুকরোই বাদ যায়।
     * ⛔ ডেটাবেসে কিছু বদলানো হয় না — এটা শুধু দেখানোর সময়ের ছাঁকনি, তাই
     *    পুরনো সারিগুলোও নিজে থেকেই ঠিক দেখাবে।
     */
    fun typedPartOf(remark: String, storedLabel: String): String {
        val cleaned = remark.substringBefore("| Audit:").substringBefore("Audit:").trim()
        if (cleaned.isBlank()) return ""
        // `—` দিয়েও ভাগ করা হয়, কারণ অ্যাপ নিজে "লেখা — ₹১০,০০০ · CASH" এভাবেই
        // জোড়া দেয়; ওই লেখাটা কোনোভাবে রিমার্কের ঘরে ফিরে এলেও যেন টাকার
        // অংশটা ট্রিটমেন্ট সেজে না বসে (২৭.০৭.২০২৬-এ ঠিক এটাই হয়েছিল)।
        val parts = cleaned.split(" · ", " | ", " — ", "·").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        val human = parts.filter { !isAutoPaymentRemark(it, storedLabel) }
        return human.joinToString(" · ")
    }

    /** Matches ordinalPaymentLabel(): 1st = "Advance", then "2nd Payment", "3rd Payment"... */
    fun ordinalPaymentLabel(n: Int): String {
        if (n <= 1) return "Advance"
        val mod100 = n % 100
        val mod10 = n % 10
        val suffix = if (mod100 in 11..13) "th" else when (mod10) {
            1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th"
        }
        return "$n$suffix Payment"
    }

    fun buildTreatmentPaymentRow(
        patient: PatientBillInfo, amount: Double, mode: String, remarks: String,
        label: String, staffMobile: String,
        // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow.
        // All three default to the exact previous behaviour (today's date,
        // no backdate audit fields) -- the ChamberAttendanceActivity.kt
        // call site is completely unaffected.
        overrideDate: String? = null,
        backdateRequestedBy: String? = null,
        backdateApprovedBy: String? = null
    ): JSONObject {
        val now = isoNow()
        val paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "")
        val normalizedMode = normalizeMode(mode)
        val cashAmount = if (normalizedMode == "CASH") amount else 0.0
        val onlineAmount = if (normalizedMode == "ONLINE") amount else 0.0
        val event = JSONObject()
            .put("eventId", paymentId)
            .put("amount", amount)
            .put("mode", normalizedMode)
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("remarks", remarks.ifBlank { label })
        val row = JSONObject()
            .put("id", paymentId)
            .put("payType", "treatment")
            .put("payLabel", label)
            .put("paymentLabel", label)
            .put("patientId", patient.id)
            // 🆔 TK-এর নিয়ম (28.07.2026): মানুষের পড়ার Patient ID-ও সঙ্গে
            // রাখা হয়, যাতে টাকার তালিকায় আলাদা করে খুঁজতে না হয়।
            // ⛔ বাড়তি কোনো ক্লাউড-কল নয় — একই সারিতে একটা ঘর বেশি।
            .put("patientCode", patient.patientId)
            .put("mobile", patient.mobile)
            .put("branch", patient.branch)
            .put("name", patient.name)
            .put("date", overrideDate ?: today())
            .put("amount", amount)
            .put("mode", normalizedMode)
            .put("cashAmount", cashAmount)
            .put("onlineAmount", onlineAmount)
            .put("dailyEvents", org.json.JSONArray().put(event))
            .put("remarks", remarks.ifBlank { label })
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
        if (backdateRequestedBy != null) row.put("backdateRequestedBy", backdateRequestedBy)
        if (backdateApprovedBy != null) row.put("backdateApprovedBy", backdateApprovedBy)
        return row
    }

    /** TK APPROVED (2026-07-16): Chamber Attendance "Search & mark Arrived"
     *  button — for an existing patient who came in but has no Payment/
     *  Registration/Enquiry today (e.g. a free follow-up visit). Writes a
     *  ZERO-amount row into the SAME "payments" table (not a new table),
     *  so the existing arrived-detection in ChamberAttendanceRepository
     *  picks it up automatically. payType="attendance_mark" is specially
     *  recognised there so it never adds to any Fees/Payment total or
     *  shows as a "₹0 payment" anywhere else in the app. */
    fun buildAttendanceMarkRow(mobile: String, name: String, branch: String, staffMobile: String): JSONObject {
        val now = isoNow()
        /* 🔴🔒 V1490 (১৫.০৯.২০২৬, TK-রিপোর্ট ও অনুমতি) — আগে এখানে প্রতিবার
           নতুন random id (UUID) বসত, তাই একই পেশেন্টকে একদিনে দ্বিতীয়বার
           "এসেছেন" চাপলে (যেকোনো পর্দা থেকেই — Chamber board/Patient Card/
           Follow-up/Search, সবাই এই একই ফাংশনই ডাকে) দুটো আলাদা "Marked
           Arrived" সারি বসে যেত (Checkup History-তে হুবহু ডুপ্লিকেট দেখাত,
           TK ধরেছেন — PRENESWER ROY-র উদাহরণ)। এখন id মোবাইল+আজকের-তারিখ
           দিয়ে নির্দিষ্ট (`markExpected()`-এর একই প্রমাণিত পদ্ধতি, নিচেই
           দেখুন) — একই দিনে আবার চাপলে upsert শুধু পুরনো সারিটাই আপডেট করে,
           নতুন সারি বসে না। পরদিন তারিখ বদলে যায় বলে পরের দিনের আসল "এসেছেন"
           ঠিকই আলাদা সারি হিসেবে বসবে। */
        return JSONObject()
            .put("id", "pay_arr_" + mobile.filter { it.isDigit() }.takeLast(10) + "_" + today().replace("-", ""))
            .put("payType", "attendance_mark")
            .put("payLabel", "Marked Arrived")
            .put("paymentLabel", "Marked Arrived")
            .put("mobile", mobile)
            .put("branch", branch)
            .put("name", name)
            .put("date", today())
            .put("amount", 0.0)
            .put("mode", "CASH")
            .put("remarks", "Marked Arrived (Chamber Attendance)")
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
    }

    /* ═══════════════════════════════════════════════════════════════════
       📝🔒 V1116 (০৫.০৯.২০২৬, TK-রিপোর্ট, অনুমোদিত): *"চেম্বার বন্ধ করে
       তারপর যদি পুনরায় আবার চেম্বার খোলা হয়, সেই ক্ষেত্রে অনেক ট্রিটমেন্ট
       প্রগ্রেসের ঘর খালি দেখায়।"*

       ─── 🔴 আসল কারণ (কোড ধরে মেপে পাওয়া) ─────────────────────────────
       চেম্বারের Treatment Progress জমা হয় **ওই দিনের টাকার সারির `progress`
       ঘরে** (V687-এ প্রমাণিত, V938-এ ডাক্তারের পথও একই)। ⇒ যে রোগী এসেছেন,
       চিকিৎসা হয়েছে, কিন্তু **ওই দিনে এক টাকাও দেননি**, তাঁর একটাও টাকার
       সারি নেই — তাই লেখাটা **রাখার জায়গাই নেই**। বোর্ড খোলা থাকতে পর্দায়
       দেখায়, বন্ধ করে আবার খুললেই ফাঁকা।
       ⛔ `followups.history` থেকে ফিরিয়ে আনার ব্যবস্থাটা **শুধু পুরনো তারিখে**
          চলে, তাই একই দিনে বন্ধ-খোলায় কাজে লাগে না।

       ─── সমাধান ─────────────────────────────────────────────────────────
       তখন ওই দিনের জন্য একটা **শূন্য টাকার সারি** বসে, শুধু লেখাটা ধরে রাখতে।
       ⛔ ধরন `attendance_mark` — প্রকল্পে **আগে থেকেই থাকা** সেই একই শূন্য-সারি
          (V.. "Marked Arrived"), যেটা কোনো Fees/Payment যোগফলে **কখনো ধরা হয় না**
          আর টাকার তালিকাতেও দেখানো হয় না (V549)। ⇒ নতুন কোনো ধরন বানানো হয়নি,
          তাই কোনো হিসাব বা তালিকা বদলায় না।
       ⛔ আইডি **রোগীর নিজস্ব identity+তারিখ ধরে স্থির** (`prog_<patient>_<তারিখ>`) —
          তাই একই মোবাইলে দুই রোগী থাকলেও একে অন্যের সারি overwrite করে না,
          আর একই রোগীর একই দিনে বারবার লিখলেও **একটাই সারি** হয়।
       ⛔ রোগী সত্যিই এসেছিলেন বলেই সারিটা বসে, তাই "arrived" চিহ্নেও কোনো
          মিথ্যে যোগ হয় না।
       ═══════════════════════════════════════════════════════════════════ */
    fun buildProgressHolderRow(
        mobile: String, name: String, branch: String, patientRowId: String,
        dateKey: String, progress: String, staffMobile: String
    ): JSONObject {
        val now = isoNow()
        val d10 = mobile.filter { it.isDigit() }.takeLast(10)
        val day = dateKey.ifBlank { today() }
        return JSONObject()
            .put("id", "prog_" + patientRowId.trim().ifBlank { d10 }.replace(Regex("[^A-Za-z0-9_]"), "_") + "_" + day.replace("-", ""))
            .put("payType", "attendance_mark")
            .put("payLabel", "Marked Arrived")
            .put("paymentLabel", "Marked Arrived")
            .put("patientId", patientRowId)
            .put("mobile", mobile)
            .put("branch", branch)
            .put("name", name)
            .put("date", day)
            .put("amount", 0.0)
            .put("mode", "CASH")
            .put("remarks", "Marked Arrived (Chamber Attendance)")
            .put("progress", progress)
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
    }

    /** TK-REQUESTED (2026-07-22): audit trail for a Total Bill correction.
     *  Same zero-amount "payments" row trick as buildAttendanceMarkRow --
     *  payType "bill_edit" NEVER adds to any Fees/Payment total and does not
     *  touch Chamber "arrived"; it only records WHO changed the bill, WHEN, and
     *  from what to what, so the correction is visible in the payment history. */
    fun buildBillEditRow(mobile: String, name: String, branch: String, oldBill: Double, newBill: Double, staffMobile: String): JSONObject {
        val now = isoNow()
        return JSONObject()
            .put("id", "pay_" + UUID.randomUUID().toString().replace("-", ""))
            .put("payType", "bill_edit")
            .put("payLabel", "Bill Edited")
            .put("paymentLabel", "Bill Edited")
            .put("mobile", mobile)
            .put("branch", branch)
            .put("name", name)
            .put("date", today())
            .put("amount", 0.0)
            .put("mode", "CASH")
            .put("remarks", "Bill corrected: ₹${"%,.0f".format(oldBill)} → ₹${"%,.0f".format(newBill)}")
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
    }

    // same zero-amount "payments" row trick as buildAttendanceMarkRow above,
    // except the "date" is the FUTURE date the staff picked (when they'll
    // actually come), not today. ChamberAttendanceRepository reads this
    // payType to decide "Expected" instead of the old nextFollow-date
    // matching (TK's explicit 2026-07-19 request).
    fun buildExpectedMarkRow(mobile: String, name: String, branch: String, expectedDate: String, staffMobile: String): JSONObject {
        val now = isoNow()
        // TK-DECISION (2026-07-22): one person = exactly ONE "আসার কথা" entry.
        // A DETERMINISTIC id keyed only to the person (last-10 digits) means
        // marking the same person Expected again just OVERWRITES the same row
        // (upsert-by-id) instead of adding a duplicate -- and changing the
        // date simply updates that one row's date, so the entry "moves" to
        // the new date automatically with no leftover on the old date. No
        // extra Supabase query needed (quota-safe).
        val key = mobile.filter { it.isDigit() }.takeLast(10)
        return JSONObject()
            .put("id", "exp_$key")
            .put("payType", "chamber_expected")
            .put("payLabel", "Marked Expected")
            .put("paymentLabel", "Marked Expected")
            .put("mobile", mobile)
            .put("branch", branch)
            .put("name", name)
            .put("date", expectedDate)
            .put("amount", 0.0)
            .put("mode", "CASH")
            .put("remarks", "Marked Expected (Chamber Date)")
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
    }

    /**
     * V216 (§13, 31.07.2026) — Refund / টাকা ফেরত row.
     * ⛔ পুরোনো payment row কখনো edit/delete হয় না — refund সবসময় **আলাদা নতুন row**।
     * ⛔ zero-amount audit-row-এর মতো একই `payments` টেবিলেই লেখা হয় (নতুন টেবিল নেই)।
     * amount **ধনাত্মক** রাখা হয় (collection-এর `if(amount>0)` guard-এর সঙ্গে সামঞ্জস্য);
     * হিসাবের সময় approved refund আলাদা করে **বিয়োগ** করা হয় (PaymentRepository)।
     * approvalStatus: Master সরাসরি করলে "approved"; staff করলে "pending" → Master
     * approve করলে "approved", reject করলে "rejected"। শুধু approved refund total কমায়।
     */
    /**
     * V219 (§1, 31.07.2026) — Refund idempotency।
     * ⛔ আগে buildRefundRow প্রতিবার নতুন random UUID id দিত। তাই Refund cloud-এ
     *    না গিয়ে ব্যর্থ হলে স্টাফ আবার চাপলে **আলাদা id-র দ্বিতীয় Refund row**
     *    তৈরি হত (double refund)। এখন একই ইনপুটে (রোগী+টাকা+কারণ+আজকের তারিখ+
     *    যিনি করলেন) **হুবহু একই deterministic id** — তাই আবার চাপলে upsert
     *    পুরোনো row-টাই overwrite করে, নতুন row হয় না। এটা প্রজেক্টের নিজের
     *    `buildExpectedMarkRow` (exp_$key)-এর একই প্রমাণিত idempotency নিয়ম।
     * ⛔ id-এর সামনে মোবাইলের ১০ অঙ্ক থাকায় দুই আলাদা রোগীর id কখনো মিলবে না
     *    (hash মিললেও prefix আলাদা)।
     */
    // V220 (§4, 31.07.2026): `nonce` — Refund ফর্ম **একবার খোলা**র জন্য একটা ছোট
    // চিহ্ন। ঐ ফর্মের সব tap (cloud-fail → আবার চাপা = retry) একই nonce → একই id →
    // দ্বিতীয় Refund তৈরি হয় না (§1 idempotency অক্ষত)। **নতুন করে ফর্ম খুললে নতুন
    // nonce → আলাদা id**, তাই একই দিনে একই পরিমাণের দুটি *বৈধ* Refund আলাদা থাকে।
    // nonce ফাঁকা হলে আগের V219 আচরণ (backward-compatible)।
    // 🔒 V222 (§2, 31.07.2026): raw-এর সামনে **`patient.id` (আসল Record/Row id)** যোগ।
    // আগে id ছিল শুধু মোবাইল-ভিত্তিক; **এক মোবাইলে দুই আলাদা রোগী** থাকলে (patientId
    // কোড আলাদা, মোবাইল এক) একই টাকা+কারণ+দিন+staff-এ Refund id মিলে গিয়ে একটা
    // আরেকটাকে চাপা দিতে পারত। এখন patient.id আলাদা বলে দুই রোগীর id **কখনো মেলে না**।
    // ⛔ একই রোগীর retry আগের মতোই একই id (idempotency অক্ষত)। ⛔ web `wlv1RefundIdFor`-এর
    //    raw হুবহু এক ক্রমে (Java String.hashCode parity) রাখা হয়েছে।
    fun refundIdFor(patient: PatientBillInfo, amount: Double, reason: String, requestedBy: String, nonce: String = ""): String {
        val mob = patient.mobile.filter { it.isDigit() }.takeLast(10)
        val amtCents = Math.round(amount * 100)
        val dateCompact = today().replace("-", "")
        val req = requestedBy.filter { it.isDigit() }.takeLast(10)
        val raw = "${patient.id}|$mob|$amtCents|${reason.trim().lowercase()}|${today()}|$req|$nonce"
        val hex = Integer.toHexString(raw.hashCode())
        return "rfnd_${mob}_${amtCents}_${dateCompact}_$hex"
    }

    fun buildRefundRow(
        patient: PatientBillInfo, amount: Double, mode: String, reason: String,
        approvalStatus: String, requestedBy: String, approvedBy: String,
        staffMobile: String, refundOfPaymentId: String = "", nonce: String = "",
        // 🔴🔒 V1442 (১৩.০৯.২০২৬, TK-নির্দেশ) — "Backdate Payment"-এর হুবহু
        // একই তিনটে ঘর (PaymentModel.buildTreatmentPaymentRow-এর যমজ)।
        // সবক'টা ডিফল্ট আগের আচরণ (আজকের তারিখ, কোনো audit ঘর নেই) —
        // পুরনো কোনো ডাকার জায়গা (সাধারণ Refund) এক অক্ষরও বদলায় না।
        overrideDate: String? = null,
        backdateRequestedBy: String? = null,
        backdateApprovedBy: String? = null
    ): JSONObject {
        val now = isoNow()
        val row = JSONObject()
            // V219 (§1) + V220 (§4): deterministic id (nonce সহ) — retry-তে দ্বিতীয়
            // Refund হয় না, কিন্তু নতুন ফর্মে করা বৈধ দ্বিতীয় Refund আলাদা থাকে।
            .put("id", refundIdFor(patient, amount, reason, requestedBy, nonce))
            .put("payType", "refund")
            .put("payLabel", "Refund")
            .put("paymentLabel", "Refund")
            .put("patientId", patient.id)
            .put("patientCode", patient.patientId)
            .put("mobile", patient.mobile)
            .put("branch", patient.branch)
            .put("name", patient.name)
            .put("date", overrideDate ?: today())
            .put("amount", amount)
            .put("mode", normalizeMode(mode))
            .put("remarks", reason.ifBlank { "Refund" })
            .put("refundReason", reason)
            .put("refundApprovalStatus", approvalStatus)
            .put("refundRequestedBy", requestedBy)
            .put("refundApprovedBy", approvedBy)
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
        if (refundOfPaymentId.isNotBlank()) row.put("refundOfPaymentId", refundOfPaymentId)
        if (backdateRequestedBy != null) row.put("backdateRequestedBy", backdateRequestedBy)
        if (backdateApprovedBy != null) row.put("backdateApprovedBy", backdateApprovedBy)
        return row
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow --
    // builds the PENDING request row (separate "payment_backdate_requests"
    // table, never the real "payments" table until Master approves). Real
    // Due/Paid/Reports calculations never see this table at all, so this
    // is zero-risk to any existing money calculation anywhere.
    fun buildBackdateRequestRow(
        patient: PatientBillInfo, enteredBill: Double, amount: Double, mode: String,
        remarks: String, requestedDate: String, staffMobile: String, staffName: String,
        // 🔴🔒 V1442 (১৩.০৯.২০২৬, TK-নির্দেশ) — ডিফল্ট "treatment" মানে পুরনো
        // প্রতিটা ডাকার জায়গা (Treatment Payment backdate) এক অক্ষরও বদলায় না।
        payType: String = "treatment"
    ): JSONObject {
        val now = isoNow()
        return JSONObject()
            .put("id", "bdr_" + UUID.randomUUID().toString().replace("-", ""))
            .put("patientRowId", patient.id)
            .put("patientCode", patient.patientId)
            .put("mobile", patient.mobile)
            .put("name", patient.name)
            .put("branch", patient.branch)
            .put("billAmount", enteredBill)
            .put("amount", amount)
            .put("mode", mode)
            .put("remarks", remarks)
            .put("requestedDate", requestedDate)
            .put("requestedBy", staffMobile)
            .put("requestedByName", staffName)
            .put("requestedAt", now)
            .put("status", "pending")
            .put("payType", payType)
            .put("createdAt", now)
            .put("updatedAt", now)
    }
}
