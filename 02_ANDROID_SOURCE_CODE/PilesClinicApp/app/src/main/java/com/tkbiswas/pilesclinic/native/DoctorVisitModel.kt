package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DoctorVisitItem(
    val id: String,
    val name: String,
    val mobile: String,
    // 🟢 B630 (11.08.2026, TK-নির্দেশ): একই ডাক্তারের বাড়তি নম্বর (কমা-আলাদা "+91..." CSV)।
    //   ⛔ মূল `mobile` অটুট (পুরনো কল/খোঁজা/referral সব আগের মতোই); এটা শুধু বাড়তি নম্বর
    //      সেভ ও ডুপ্লিকেট-চেকে ব্যবহার হয়।
    val altMobiles: String = "",
    val area: String,
    /* 🚓🔒 V1034 (০৪.০৯.২০২৬, TK-নির্দেশ: *"আরএমপি ঠিকানা লেখা আছে, আমি চাইছি
       সেই আরএমপি কোন থানায়"*) — ডাক্তার কোন থানার অধীনে।
       ⛔ ডিফল্ট ফাঁকা, তাই ডেটাবেসে ঘরটা না থাকলেও পুরনো কিছু ভাঙে না। */
    val policeStation: String = "",
    val branch: String,
    val remarks: String,
    val lastCallDate: String,
    val nextCallDate: String,
    val callStatus: String = "",
    val callCount: Int = 0,
    // 🔒 খাতার সারি B123 (TK, 29.07.2026): Follow-up কার্ডের মতো কার্ডে
    // `LAST CALL <তারিখ> (<স্টাফ>)` দেখাতে হলে **কে কল করেছিলেন** সেটা লাগে।
    // ঘরটা `callHistory`-র সবচেয়ে নতুন সারির `by` থেকে আসে — ⛔ ডেটাবেসে
    // নতুন কোনো ঘর লাগেনি, যা আগে থেকেই লেখা হচ্ছিল সেটাই পড়া হলো।
    val lastCallBy: String = "",
    /* 🔵🔒 V543 (২২.০৮.২০২৬, TK-নির্দেশ: *"তারিখের সাথে সময় থাকবে"*)
       শেষ কলের সময় — `callHistory`-র সবচেয়ে নতুন সারির `createdAt` থেকে।
       ⛔ ওই ঘরটা প্রতিটা কলে আগে থেকেই জমা হয় (`buildCallUpdateFields`),
          তাই **নতুন কোনো কলাম বা ক্লাউড-অনুরোধ লাগেনি**।
       ⛔ না থাকলে ফাঁকা ⇒ কার্ডে আগের মতোই শুধু তারিখ। */
    val lastCallTime: String = "",
    // TK-REQUESTED ADDITION (2026-07-23): Delete-Approval for Doctor/RMP —
    // Master deletes immediately, anyone else only requests (these two
    // fields get set); blank means no request pending. `raw` keeps the
    // full original row so an approved delete can move the COMPLETE record
    // into Trash Bin (same as every other delete in the app), not just the
    // few fields this app already parses out of it.
    val deleteRequestedBy: String = "",
    val deleteRequestedAt: String = "",
    // TK-REQUESTED ADDITION (2026-07-23): shown on the card (see
    // tvReferralSummary). referredCount is NOT filled in by parse() below —
    // it needs a cross-check against the whole "patients" table, which
    // DoctorVisitActivity.loadList() computes once for the whole list and
    // fills in via .copy(referredCount = ...); parse() alone has no way to
    // know it. referralPaid IS a plain field already stored on this
    // doctor's own row, so parse() fills it in directly, no extra fetch.
    val referredCount: Int = 0,
    val referralPaid: Double = 0.0,
    // 🔒 খাতার সারি B193 (TK, 30.07.2026 রাত): "EXPECTED" ঘরের জন্য —
    // ডাক্তার যদি বলেন "এই তারিখে একটা পেশেন্ট পাঠাতে পারি", Log Call
    // ফর্মে সেই তারিখ এখানে বসে। ⛔ ফাঁকা মানে কোনো প্রত্যাশিত তারিখ নেই।
    val expectedPatientDate: String = "",
    val raw: JSONObject = JSONObject()
)

object DoctorVisitModel {

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    /** Matches doctorDefaultNextDate(): today + 30 days, used when staff
     * leaves the Next Call Date blank. */
    fun defaultNextDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 30)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    /** Matches doctorDue(): no next-call date set, or it's today/in the past. */
    fun isDue(nextCallDate: String): Boolean = nextCallDate.isBlank() || nextCallDate <= today()

    // TK-REQUESTED ADDITION (2026-07-23): "Overdue" = a next-call date that is
    // strictly BEFORE today (a call that was missed). This is intentionally
    // NOT the same as isDue() above, which also counts today's calls and
    // blank dates. Kept separate so isDue()'s existing callers (sorting,
    // etc.) are completely unaffected. Blank dates are NOT overdue here.
    fun isOverdue(nextCallDate: String): Boolean =
        nextCallDate.isNotBlank() && nextCallDate < today()

    /**
     * 🔒 খাতার সারি B193 (TK, 30.07.2026 রাত): "PENDING"/"CALLED" ঘরের
     * ভিত্তি — একটা তারিখ কি **এই ইংরেজি ক্যালেন্ডার মাসে** (১ তারিখ থেকে
     * মাসের শেষ তারিখ পর্যন্ত) পড়ে কিনা। তারিখ ISO "yyyy-MM-dd" ফরম্যাটে
     * থাকে বলে প্রথম ৭ অক্ষর ("yyyy-MM") মিলিয়ে দেখাই যথেষ্ট — মাসের
     * শুরু/শেষ আলাদা করে হিসাব করতে হয় না।
     */
    fun isThisMonth(dateStr: String): Boolean =
        dateStr.isNotBlank() && dateStr.take(7) == today().take(7)

    fun parse(row: JSONObject): DoctorVisitItem = DoctorVisitItem(
        id = row.s("id"),
        name = row.s("name"),
        mobile = row.s("mobile"),
        altMobiles = row.s("altMobiles"),
        area = row.s("area"),
        policeStation = row.s("policeStation"),
        branch = row.s("branch"),
        remarks = row.s("remarks"),
        lastCallDate = row.s("lastCallDate"),
        nextCallDate = row.s("nextCallDate"),
        callStatus = row.s("callStatus"),
        callCount = row.optJSONArray("callHistory")?.length() ?: 0,
        // সবচেয়ে নতুন কল-সারিটা তালিকার **প্রথমে** থাকে (buildCallUpdateFields
        // unshift করে), তাই ০ নম্বর সারির `by`-ই শেষ কলের স্টাফ।
        lastCallBy = row.optJSONArray("callHistory")?.optJSONObject(0)?.s("by").orEmpty(),
        lastCallTime = row.optJSONArray("callHistory")?.optJSONObject(0)?.s("createdAt").orEmpty(),   // 🔵 V543
        deleteRequestedBy = row.s("deleteRequestedBy"),
        deleteRequestedAt = row.s("deleteRequestedAt"),
        referralPaid = referralPaidFrom(row),   // 🔵 V551
        expectedPatientDate = row.s("expectedPatientDate"),
        raw = row
    )

    /* 🔵🔒 V551 (২২.০৮.২০২৬, FALAKATA staff-এর রিপোর্ট, ছবিসহ):
       *"আমি তো ২০০০০-এর পরে ৯০০০ দিয়েছি, ওখানে কেন ২০০০০ দেখাচ্ছে?"*

       **আসল কারণ (কোড ধরে, আন্দাজ নয়):** RMP কার্ডের "₹… INCOME" লেখাটা
       `doctor_visits` সারিতে **জমানো** `referralPaid` ঘরটা সরাসরি দেখাত।
       ওই জমানো সংখ্যাটা **পুরোনো হয়ে যেতে পারে** — এটা প্রজেক্টের নিজের কোডেই
       আগে থেকে লেখা আছে: `DoctorVisitActivity.kt:2188-2190` —
       *"The old scalar referralPaid/referralDue fields can be stale (for example,
         history contains a Paid ₹1,500 row while the scalar still says ₹0)"*।
       তাই ওই একই পর্দার **View All** (👁) তালিকা অনেক আগেই (V381) জমানো
       সংখ্যাটা বাদ দিয়ে **এন্ট্রির তালিকা থেকে নিজে যোগ করে** দেখায় — কিন্তু
       বাইরের কার্ডটা পুরোনো সংখ্যাই দেখাত। তাই দুই জায়গায় দুই রকম।

       **এখন:** কার্ডও সেই **এন্ট্রির তালিকা** (`referralPayments`) থেকেই যোগ করে —
       "Paid" চিহ্ন দেওয়া সবগুলোর যোগফল। ⇒ 👁-এ যা, কার্ডেও ঠিক তা।

       ⛔ **Supabase-এ একটাও বাড়তি query নেই** — `referralPayments` ঘরটা এই
          তালিকা এমনিতেই আনে (`SafeWideColumns.kt:67`)।
       ⛔ তালিকাটা না এলে (শুধু শেষ-চেষ্টার সরু পড়ায় বাদ যায়) আগের মতোই জমানো
          সংখ্যাটাই দেখায় — কোনো পথ ভাঙে না।
       ⛔ কোনো টাকা/এন্ট্রি বদলানো বা মোছা হয় না — শুধু যোগফলটা এখন সত্যি। */
    fun referralPaidFrom(row: org.json.JSONObject): Double {
        val arr = row.optJSONArray("referralPayments") ?: return row.optDouble("referralPaid", 0.0)
        if (arr.length() == 0) return row.optDouble("referralPaid", 0.0)
        var paid = 0.0
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            if (e.optString("status", "Unpaid").equals("Paid", true)) paid += e.optDouble("amount", 0.0)
        }
        return paid
    }

    fun buildNewDoctorRow(name: String, mobileDigitsOnly: String, branch: String, area: String, remarks: String, nextCallDate: String, staffMobile: String, altMobiles: String = "", policeStation: String = ""): JSONObject {
        val now = isoNow()
        val row = JSONObject()
            .put("id", "dv_" + UUID.randomUUID().toString().replace("-", ""))
            .put("name", name)
            .put("mobile", "+91$mobileDigitsOnly")
            .put("area", area)
            .put("remarks", remarks)
            .put("date", today())
            .put("branch", branch)
            .put("lastCallDate", "")
            .put("nextCallDate", nextCallDate)
            .put("callHistory", JSONArray())
            .put("referralPayments", JSONArray())
            .put("referralPaid", 0)
            .put("referralDue", 0)
            .put("callStatus", "Pending")
            .put("status", "Active")
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
        // 🟢 B630: বাড়তি নম্বর থাকলে তবেই `altMobiles` লেখা হয় — খালি হলে নয়।
        //   এতে SQL (কলাম) ভুলে বাদ পড়লেও সাধারণ ডাক্তার-সেভ ব্যর্থ হয় না।
        if (altMobiles.isNotBlank()) row.put("altMobiles", altMobiles)
        // 🚓 V1034 — থানা লেখা থাকলে তবেই বসে (ঘর না থাকলেও সেভ ব্যর্থ হয় না)।
        if (policeStation.isNotBlank()) row.put("policeStation", policeStation)
        return row
    }

    /** Fields to PATCH for a call-log update, matching saveDoctorCall()
     * exactly: appends to callHistory, updates lastCallDate/nextCallDate/
     * remarks/callStatus. existingHistory should be whatever callHistory
     * array the row already has (empty JSONArray if none).
     *
     * 🔒 খাতার সারি B193 (TK, 30.07.2026 রাত): নতুন `expectedPatientDate`
     * প্যারামিটার — ডিফল্ট ফাঁকা স্ট্রিং, তাই পুরনো কোনো কলের আচরণ বদলায়নি।
     * ⛔ কল করা হলে (যেকোনো path) এই মানটাই পাঠানো হবে — তাই কলকারীর
     *    দায়িত্ব হলো "না ছুঁলে আগের মানই আবার পাঠানো" (item.expectedPatientDate),
     *    নইলে অজান্তেই মুছে যেতে পারে।
     */
    fun buildCallUpdateFields(existingHistory: JSONArray, note: String, nextCallDate: String, staffMobile: String, expectedPatientDate: String = ""): JSONObject {
        val entry = JSONObject()
            .put("date", today())
            .put("note", note)
            .put("nextCallDate", nextCallDate)
            .put("by", staffMobile)
            .put("createdAt", isoNow())
        // unshift: newest call first, matching hist.unshift(...) in app.js
        val newHistory = JSONArray()
        newHistory.put(entry)
        for (i in 0 until existingHistory.length()) newHistory.put(existingHistory.get(i))

        return JSONObject()
            .put("lastCallDate", today())
            .put("nextCallDate", nextCallDate)
            .put("callStatus", "Called")
            .put("remarks", note)
            .put("callHistory", newHistory)
            .put("expectedPatientDate", expectedPatientDate)
            .put("updatedAt", isoNow())
    }
}
