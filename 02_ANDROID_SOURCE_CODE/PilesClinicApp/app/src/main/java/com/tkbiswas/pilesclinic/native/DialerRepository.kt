package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONObject

/**
 * 🆕 B464 (05.08.2026, TK-নির্দেশ) — "Dialer": অন্য ব্রাঞ্চের ফরওয়ার্ড-করা
 * এনকোয়ারি নম্বরে ফিরতি কল করলে সেটা অ্যাপে "গোনা" যায় না, কারণ স্টাফ
 * WhatsApp থেকে নম্বর কপি করে ফোনের নিজের ডায়ালারে কল করতেন — অ্যাপ কিছুই
 * জানত না। এই নতুন `DialerActivity`-এর "Call" বোতাম থেকে কল করলে:
 *   ১) `CallChooser.open()` (আগে থেকেই প্রজেক্ট-জোড়া ব্যবহৃত) — ডায়াল করে
 *      **এবং** `call_taps`-এ লগ করে, যেটা Work Notebook-এর Daily Report-এর
 *      "App Calls (auto)"/"Total call (auto)"-তে এমনিতেই যোগ হয়ে যায় —
 *      এখানে নতুন কিছু করতে হয়নি।
 *   ২) এই ফাইলের `logDialedCall()` — নম্বরটা `followups` টেবিলে (যেকোনো
 *      ব্রাঞ্চের Enquiry/Visit/Patient) মিললে সেই রেকর্ডেই কল-গোনা বাড়ে
 *      (প্রমাণিত `FollowUpRepository.updateRemark(..., incrementCall=true)`
 *      পুনর্ব্যবহার — এই ফাংশনের ভিতরে এক অক্ষরও ছোঁয়া হয়নি)। না মিললে
 *      নতুন, আলাদা `dialer_calls` টেবিলে জমা থাকে (TK-এর নির্দেশ: "দুটোই —
 *      Enquiry থাকলে তাতে যোগ হবে, না থাকলে আলাদা লগে")।
 * ⛔ কোনো পুরনো টেবিল/কলাম/ফাংশনের ভিতরে হাত দেওয়া হয়নি — সবটাই নতুন,
 *    স্বাধীন সংযোজন। 🔴 `dialer_calls` নতুন টেবিল — TK-কে SQL চালাতে হবে।
 */
object DialerRepository {

    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())

    /** ডায়াল করার সাথে সাথে (ব্যাকগ্রাউন্ডে) ডাকা হয় — কল করাটা কখনো এর
     *  জন্য অপেক্ষা করে না, ব্যর্থ হলেও নিঃশব্দে বাদ (কল করাই আসল কাজ)। */
    /* 🔴🔒 V1540 (১৯.০৯.২০২৬, TK-নির্দেশ) — TK: *"স্টাফ তো কলই করে নাই, তাহলে
       এটা কেন আসলো... Remarks না লিখলে কল গণ্য হবে না এটা তো আগেই বলা ছিল
       (তালিকা ২৩৭, ০৫.০৯.২০২৬)।"* আগে এই ফাংশন (`CallChooser.open()`-এর
       মাধ্যমে অ্যাপের **যেকোনো** 📞 বোতাম চাপলেই, শুধু Dialer পর্দা থেকে নয়)
       নিজে থেকেই "Called via COB"-এর মতো একটা লেখা `remark` হিসেবে বসিয়ে
       রোগীর ইতিহাসে সারি বসাত ও কল-গোনা বাড়াত — এটা আসলে স্টাফের নিজের
       লেখা কোনো মন্তব্য নয়, অ্যাপের নিজের বসানো কথা। TK-র "শুধু রিমার্ক
       লিখলে তবেই কল গণ্য" নিয়মের সাথে সরাসরি সাংঘর্ষিক ছিল।
       ⇒ এখন followups-এর remark/history/callCount কিছুই ছোঁয়া হয় না —
       শুধু `dialer_calls` টেবিলে (আগে শুধু না-মেলা নম্বরের জন্য ব্যবহৃত)
       ডায়াল করা হয়েছে এইটুকু তথ্য জমা থাকে, রোগীর পাতায় দেখা যায় না।
       ⛔ `ModuleAuth.logCallTap()` (Work Notebook-এর "App Calls (auto)")
       আলাদা, ওটা ছোঁয়া হয়নি — শুধু বোতাম-চাপ গোনে, ভুয়া রিমার্ক লেখে না। */
    fun logDialedCall(context: Context, dialedNumber: String, staffMobile: String, staffName: String, branch: String) {
        Thread {
            try {
                val digits = dialedNumber.filter { it.isDigit() }.takeLast(10)
                if (digits.length != 10) return@Thread
                val row = JSONObject()
                    .put("id", "dc_" + System.currentTimeMillis() + "_" + (0..999).random())
                    .put("staffMobile", staffMobile.filter { it.isDigit() }.takeLast(10))
                    .put("staffName", staffName)
                    .put("branch", branch)
                    .put("dialedNumber", digits)
                    .put("calledAt", isoNow())
                SupabaseClient.upsert("dialer_calls", row)
            } catch (_: Throwable) { /* কল করাটা কখনো এর জন্য থামবে না */ }
        }.start()
    }

    /** আজ এই স্টাফের করা Dialer কল যেগুলো কোনো Enquiry/রোগীর রেকর্ডে মেলেনি
     *  (তাই আলাদা লগে গেছে) — স্ক্রিনে ছোট তালিকা দেখানোর জন্য। */
    fun fetchTodayUnmatched(staffMobile: String): List<JSONObject> {
        return try {
            val digits = staffMobile.filter { it.isDigit() }.takeLast(10)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val rows = SupabaseClient.fetchList(
                "dialer_calls", "staffMobile=eq.$digits&calledAt=gte.${today}T00:00:00", 50, order = "calledAt.desc"
            )
            (0 until rows.length()).map { rows.getJSONObject(it) }
        } catch (_: Throwable) { emptyList() }
    }

    /* 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ) — Incoming/Outgoing দুই ধরনের
       কলেই রিমার্কস। নতুন, স্বাধীন `call_remarks` টেবিল (V605 SQL, TK
       নিজে চালাবেন) — কোনো পুরনো টেবিল/ফাংশন ছোঁয়া হয়নি। একই নম্বরে
       একাধিকবার রিমার্কস লিখলে প্রতিটাই আলাদা সারি হয়ে জমা থাকে (ইতিহাস
       হারায় না), স্ক্রিনে সবচেয়ে নতুনটা দেখানো হয়।
     *
     * 🟢🔒🔒 V634 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "২ বার কল করা হয়েছে
     * তাও Wifi signal কেন ১ টা দেখাচ্ছে") — **আসল কারণ (কোড ধরে যাচাই):**
     * এই ফাংশনটা শুধু নতুন `call_remarks` টেবিলে লেখে — এনকোয়ারি
     * তালিকার Wifi-সিগন্যাল আইকন যে `followups.callCount` ঘর পড়ে
     * (`FollowUpAdapter.kt`), সেটা এখানে কখনো বাড়ানোই হত না। ফলে
     * ইতিহাসের টেবিলে (দুই উৎস মিলিয়ে) ২টা কল দেখালেও, সিগন্যাল আইকন
     * পুরনো, একবারই-বাড়া সংখ্যা দেখাত।
     * **সমাধান:** এই একই ব্যাকগ্রাউন্ড থ্রেডেই, `followupId` জানা থাকলে
     * (RMP-মিলে না), প্রজেক্টের আগে থেকে প্রমাণিত
     * `FollowUpRepository.logEnquiryCall()` (দিনে-একবার নিয়ম, ওয়েবের
     * `signalTripleTap()`-এর সাথে হুবহু মেলানো) ডাকা হয় — নতুন কোনো
     * হিসাব-নিয়ম বানানো হয়নি, প্রমাণিত পথই পুনর্ব্যবহার।
     * ⛔ `call_remarks` সেভের নিয়ম এক অক্ষরও বদলায়নি — শুধু বাড়তি এই
     *    একটা ধাপ যোগ হলো। */
    /**
     * 🟢🔒 V836 (২৯.০৮.২০২৬, TK-নির্দেশ, ডেমো-ফটো পাশ) — নোটিফিকেশনে লেখা
     * রিমার্ক এখন **নিজের সেকশনেও** বসে।
     *
     * TK: *"Enquiry, Visit, Patient, RMP — যদি আমাদের Save Data হয়ে থাকে
     * তাহলে যেন Remarks লেখা যায়। আর এখানে Remarks লিখলে উক্ত সেকশনে যেন
     * অটোমেটিক Update হয়ে যায়।"*
     *
     * আগে কী হত: লেখাটা **শুধু** `call_remarks` টেবিলে জমা হত। Follow-up-এর
     * Enquiry/Visit/Patient কার্ডে বা RMP সেকশনে কখনো দেখা যেত না — শুধু
     * কল-গোনাটা বাড়ত (V634)। TK-এর অভিযোগ ঠিক এটাই।
     *
     * এখন কী হয় (তিনটেই, একের পর এক — একটা ব্যর্থ হলেও বাকিগুলো চলে):
     *   ১. `call_remarks` — **আগের মতোই**, একটুও বদলায়নি।
     *   ২. `followupId` থাকলে → `updateRemark()` — Follow-up কার্ডের
     *      `lastRemark` + `history`। ⛔ গোনা বাড়ানোর কাজটা নিচের
     *      `logEnquiryCall()`-ই করে (দিনে একবার, B53), তাই এখানে
     *      `incrementCall = false` — **দু'বার গোনা হবে না**।
     *   ৩. `rmpId` থাকলে → `logCallKeepingDates()` — RMP সেকশনের
     *      `remarks` + `callHistory`। ⛔ আগের Next Call ও Expected Patient
     *      তারিখ **অক্ষত** থাকে (স্টাফ নিজে নতুন তারিখ বাছলে তবেই বদলায়)।
     *
     * ⛔ একটা নম্বর একই সাথে followups ও RMP — দুটোতেই থাকতে পারে না
     *    (`matchNumbersBatch` আগে followups দেখে, না পেলে তবেই RMP), তাই
     *    দুটো একসাথে লেখা হওয়ার সুযোগ নেই।
     * ⛔ পুরো কাজটা আগের মতোই ব্যাকগ্রাউন্ড থ্রেডে, try/catch-এর ভিতরে —
     *    ব্যর্থ হলেও কল বা অ্যাপ কখনো আটকাবে না।
     */
    fun saveCallRemark(
        ctx: Context?, mobile: String, direction: String, remark: String, patientId: String,
        staffMobile: String, staffName: String, branch: String, calledAtIso: String,
        followupId: String = "", rmpId: String = "", nextCallDate: String = ""
    ) {
        Thread {
            try {
                val digits = mobile.filter { it.isDigit() }.takeLast(10)
                if (digits.length != 10 || remark.isBlank()) return@Thread
                val now = isoNow()
                val row = JSONObject()
                    .put("id", "cr_" + System.currentTimeMillis() + "_" + (0..999).random())
                    .put("mobile", digits)
                    .put("direction", direction)
                    .put("remark", remark.trim())
                    .put("patientId", patientId)
                    .put("staffMobile", staffMobile.filter { it.isDigit() }.takeLast(10))
                    .put("staffName", staffName)
                    .put("branch", branch)
                    .put("calledAt", calledAtIso.ifBlank { now })
                    .put("createdAt", now)
                    .put("updatedAt", now)
                SupabaseClient.upsert("call_remarks", row)
                // 🟢🔒 V634 — একই থ্রেডেই, ব্যর্থ হলেও রিমার্কস-সেভে প্রভাব
                // পড়বে না (try/catch এই বাইরের ব্লকেই আছে)। আসল Context
                // পাঠানো হয় — নেট না থাকলে অফলাইন-সারিতে যেন জমা থাকে
                // (offline queue-এর জন্য context লাগে)।
                if (followupId.isNotBlank()) {
                    // ⏺️ V836 — আগে রিমার্কটা সারিতে বসাই, তারপর গোনা বাড়াই।
                    //    দুটো আলাদা লেখা, আলাদা ঘরে — একটা অন্যটাকে মোছে না।
                    try {
                        FollowUpRepository(ctx).updateRemark(
                            followupId, remark.trim(), staffName,
                            incrementCall = false, stampCallDate = false
                        )
                    } catch (_: Throwable) { }
                    try { FollowUpRepository(ctx).logEnquiryCall(followupId) } catch (_: Throwable) { }
                    /* 🔴🔒🔒 V873 (৩০.০৮.২০২৬, TK-রিপোর্ট + অনুমতি —
                       BHUDEV CHANDRA ROY): কল হয়ে গেলেও নাম "টুডে পেন্ডিং"-এ
                       আটকে থাকত, কারণ **এই পথে `nextFollow` কখনো বদলাতই না** —
                       `nextCallDate` শুধু RMP-র ঘরে যেত, রোগীর ঘরে নয়।
                       এখন স্টাফের বাছা তারিখটা রোগীর সারিতেও বসে ⇒ নামটা
                       পেন্ডিং থেকে সরে গিয়ে ঠিক দিনে ফিরে আসে।
                       ⛔ ফাঁকা হলে কিছুই বদলায় না — পুরোনো তারিখ অক্ষত
                          (প্রমাণিত `updateNextFollow`-ই ব্যবহার, নতুন কিছু নয়)। */
                    if (nextCallDate.isNotBlank()) {
                        try { FollowUpRepository(ctx).updateNextFollow(followupId, nextCallDate) } catch (_: Throwable) { }
                    }
                }
                // 🩺 V836 — RMP হলে RMP সেকশনেই বসে।
                if (rmpId.isNotBlank()) {
                    try {
                        DoctorVisitRepository().logCallKeepingDates(
                            id = rmpId, note = remark.trim(), staffMobile = staffMobile,
                            nextCallDate = nextCallDate, context = ctx
                        )
                    } catch (_: Throwable) { }
                }
            } catch (_: Throwable) { /* রিমার্কস সেভ ব্যর্থ হলেও অ্যাপ আটকাবে না */ }
        }.start()
    }

    /** এই নম্বরের সবচেয়ে সাম্প্রতিক রিমার্ক — কল-লগ তালিকার নিচে ছোট
     *  করে দেখানোর জন্য। না থাকলে ফাঁকা স্ট্রিং (নীরবে)। */
    /* 🆕🔒 V856 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — TK: *"LAST CALL-এর
       তারিখ এবং সময় লাগবে · missed call, incoming call নাকি outgoing call
       এগুলি থাকতে হবে"*।
       ⛔ **Egress এক বাইটও বাড়ে না** — নিচের `fetchLatestRemark()` ইতিমধ্যেই
          ঠিক এই একটাই সারি পড়ে (`call_remarks`, limit 1, calledAt.desc)।
          এখানে শুধু **একই সারির আরও তিনটে ছোট ঘর** চাওয়া হলো, নতুন কোনো
          অনুরোধ নয়। (যাচাই করে দেখা: টেবিলে ঘরগুলো আছে — V605-এর SQL।)
       ⚠️ **সীমা (TK-কে জানানো ও তিনি মেনেছেন):** `call_remarks`-এ সারি বসে
          **শুধু তখনই যখন স্টাফ রিমার্ক লেখেন**। তাই "LAST CALL" মানে
          *শেষ যে কলে রিমার্ক লেখা হয়েছিল*। প্রিমিয়াম প্ল্যান নিলে TK
          "সব কল" দেখাতে বলবেন (খাতায় লেখা আছে)। */
    data class LastCallInfo(
        val remark: String = "",
        val calledAt: String = "",
        val staffName: String = "",
        val direction: String = ""
    )

    fun fetchLatestCallInfo(mobile: String): LastCallInfo {
        return try {
            val digits = mobile.filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10) return LastCallInfo()
            val rows = SupabaseClient.fetchListSlimOrNull(
                "call_remarks", "mobile=eq.$digits", 1,
                "remark,calledAt,staffName,direction", order = "calledAt.desc"
            ) ?: return LastCallInfo()
            if (rows.length() == 0) return LastCallInfo()
            val r = rows.getJSONObject(0)
            LastCallInfo(r.s("remark"), r.s("calledAt"), r.s("staffName"), r.s("direction"))
        } catch (_: Throwable) { LastCallInfo() }
    }

    fun fetchLatestRemark(mobile: String): String {
        return try {
            val digits = mobile.filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10) return ""
            val rows = SupabaseClient.fetchListSlimOrNull(
                "call_remarks", "mobile=eq.$digits", 1, "remark", order = "calledAt.desc"
            ) ?: return ""
            if (rows.length() == 0) "" else rows.getJSONObject(0).s("remark")   // 🔴🔒 V696
        } catch (_: Throwable) { "" }
    }

    /** একাধিক নম্বরের সাম্প্রতিক রিমার্কস একসাথে (কল-লগ তালিকা খোলার
     *  সময়) — `matchNumbersBatch`-এর হুবহু একই "একবারে সবগুলো" নিয়ম,
     *  যাতে ২০-৩০টা কল-লগ সারির জন্য ২০-৩০টা আলাদা অনুরোধ না যায়। */
    fun fetchLatestRemarksBatch(numbers: List<String>): Map<String, String> {
        val digits = numbers.map { it.filter { d -> d.isDigit() }.takeLast(10) }
            .filter { it.length == 10 }.distinct().take(60)
        if (digits.isEmpty()) return emptyMap()
        return try {
            val filter = "or=(" + digits.joinToString(",") { "mobile.like.*$it" } + ")"
            val rows = SupabaseClient.fetchListSlimOrNull(
                "call_remarks", filter, digits.size * 5, "mobile,remark,calledAt", order = "calledAt.desc"
            ) ?: return emptyMap()
            val out = LinkedHashMap<String, String>()   // প্রথমবার পাওয়াটাই সবচেয়ে নতুন (calledAt.desc)
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val m = r.optString("mobile")
                if (m.isNotBlank() && !out.containsKey(m)) out[m] = r.s("remark")   // 🔴🔒 V696
            }
            out
        } catch (_: Throwable) { emptyMap() }
    }

    /**
     * 🆕🔒 খাতার সারি — Dialer পুনর্গঠন (TK-নির্দেশ, 05.08.2026 — "Android
     * ফোনের নিজস্ব Dialer-এর মতন হবে, Call Log + Contacts")।
     *
     * 🚨 Supabase ফ্রি-প্ল্যান ঝুঁকি (TK নিজে জিজ্ঞাসা করেছেন, "ভালো করে
     * যাচাই করে বলবেন"): আজকের Call Log-এ অনেকগুলো নম্বর থাকতে পারে — একটা
     * একটা করে (`findByMobile`) ক্লাউডে গেলে দিনে বহুবার অনুরোধ যেত। তাই
     * এখানে **সবগুলো নম্বর একসাথে একটাই অনুরোধে** (`or=(mobile.like.*N1,
     * mobile.like.*N2,...)` — প্রজেক্টের আগে থেকে থাকা প্রমাণিত প্যাটার্ন,
     * `DraftRepository.kt`/`ReportsActivity.kt`-এ একই সিনট্যাক্স ব্যবহার
     * হয়) মেলানো হয়, আর `CloudReadCache`-এ ২০ সেকেন্ড মনে থাকে — তাই একই
     * পর্দায় All ও Missed দুই ট্যাব একসাথে খুললেও দ্বিতীয়বার আর ক্লাউডে
     * যেতে হয় না।
     */
    data class MatchedContact(
        val id: String, val name: String, val mobile: String,
        val branch: String, val disease: String, val stage: String, val patientId: String,
        val address: String = "",   // 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ)
        val isRmp: Boolean = false  // 🟢🔒 V632 (২৪.০৮.২০২৬) — RMP (রেফারিং ডাক্তার) মিলেছে কি না
    )

    /** সর্বোচ্চ এই কয়টা নম্বর একবারে মেলানো হয় (URL খুব লম্বা হওয়া এড়াতে,
     *  বাস্তবে একদিনে এত নম্বর সাধারণত হয় না)। */
    private const val MAX_BATCH = 60

    fun matchNumbersBatch(numbers: List<String>): Map<String, MatchedContact> {
        val digits = numbers.map { it.filter { d -> d.isDigit() }.takeLast(10) }
            .filter { it.length == 10 }.distinct().take(MAX_BATCH)
        if (digits.isEmpty()) return emptyMap()
        return try {
            val filter = "or=(" + digits.joinToString(",") { "mobile.like.*$it" } + ")"
            val cacheKey = "dialer:match:" + digits.sorted().joinToString(",")
            val rows = CloudReadCache.get(cacheKey) {
                SupabaseClient.fetchListSlimOrNull(
                    "followups", filter, digits.size * 3, "id,name,mobile,branch,disease,stage,patientId,address"
                )
            } ?: return emptyMap()
            // একই নম্বরে একাধিক সারি মিললে (Enquiry+Visit+Patient) — সবচেয়ে
            // এগিয়ে থাকা ধাপটাই দেখানো হয় (Treatment > Patient > Inquiry)।
            fun rank(stage: String): Int = when (stage) {
                "Treatment" -> 3; "Patient" -> 2; "Inquiry" -> 1; else -> 0
            }
            val byNumber = HashMap<String, MatchedContact>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val mobile = r.optString("mobile")
                val key = mobile.filter { it.isDigit() }.takeLast(10)
                if (key.length != 10) continue
                val candidate = MatchedContact(
                    id = r.optString("id"), name = r.optString("name"), mobile = mobile,
                    branch = r.optString("branch"), disease = r.optString("disease"),
                    stage = r.optString("stage"), patientId = r.optString("patientId"),
                    address = r.optString("address")
                )
                val existing = byNumber[key]
                if (existing == null || rank(candidate.stage) > rank(existing.stage)) byNumber[key] = candidate
            }
            /* 🟢🔒 V632 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "এই নম্বর তো App-এ
               আছে, ইনি Cooch Behar-এর RMP, তাহলে 'Not saved anywhere' কেন
               দেখাচ্ছে?") — **আসল কারণ (কোড ধরে যাচাই):** এই ফাংশন এতদিন
               শুধু `followups` (রোগী/এনকোয়ারি) টেবিল দেখত — RMP (রেফারিং
               ডাক্তার)-দের নিজস্ব টেবিল `doctor_visits` কখনো দেখাই হত না।
               তাই কোনো RMP-র নম্বরে কল এলে, সেই RMP সত্যিই App-এ সেভ করা
               থাকলেও, এই মিলানোর কোড তা খুঁজেই পেত না।
               সমাধান: রোগী-মিল না পেলে (রোগীই অগ্রাধিকার পান, নম্বর একই
               হলেও) একই batch-নিয়মে `doctor_visits`-এও খোঁজা হয় — নাম শুধু
               `mobile` না, `altMobiles`-এও (RMP-দের বিকল্প নম্বর থাকতে
               পারে)। ⛔ রোগীর মিলানোর নিয়ম/উপরের কোড এক অক্ষরও বদলায়নি —
               শুধু "না পেলে" এই দ্বিতীয় ধাপ যোগ হলো। */
            val stillMissing = digits.filter { !byNumber.containsKey(it) }
            if (stillMissing.isNotEmpty()) {
                try {
                    val rmpFilter = "or=(" + stillMissing.joinToString(",") { "mobile.like.*$it,altMobiles.like.*$it" } + ")"
                    val rmpCacheKey = "dialer:rmpmatch:" + stillMissing.sorted().joinToString(",")
                    val rmpRows = CloudReadCache.get(rmpCacheKey) {
                        SupabaseClient.fetchListSlimOrNull(
                            "doctor_visits", rmpFilter, stillMissing.size * 3, "id,name,mobile,altMobiles,branch,area"
                        )
                    }
                    if (rmpRows != null) {
                        for (i in 0 until rmpRows.length()) {
                            val r = rmpRows.optJSONObject(i) ?: continue
                            val mainMobile = r.optString("mobile").filter { it.isDigit() }.takeLast(10)
                            val altMobiles = r.optString("altMobiles").split(",", ";", " ")
                                .map { it.filter { c -> c.isDigit() }.takeLast(10) }.filter { it.length == 10 }
                            val ownKeys = (listOf(mainMobile) + altMobiles).filter { it.isNotBlank() }
                            for (key in ownKeys) {
                                if (key !in stillMissing || byNumber.containsKey(key)) continue
                                byNumber[key] = MatchedContact(
                                    id = r.optString("id"), name = r.optString("name"), mobile = r.optString("mobile"),
                                    branch = r.optString("branch"), disease = "", stage = "", patientId = "RMP",
                                    address = r.optString("area"), isRmp = true
                                )
                            }
                        }
                    }
                } catch (_: Throwable) { }
            }
            byNumber
        } catch (_: Throwable) { emptyMap() }
    }

    /**
     * Contacts ট্যাব — Enquiry + Visit + Patient (TK-নির্দেশ)। প্রজেক্টের
     * আগে থেকে থাকা `FollowUpRepository.fetchTab()` তিনবার (Inquiry/
     * Patient/Treatment — এই ফাইলগুলোর ভিতরের আসল নাম, UI-তে যথাক্রমে
     * Enquiry/Visit/Patient) — নতুন কোনো টেবিল/কলাম লাগেনি। ⛔ ব্রাঞ্চ-
     * ছাঁকনি প্রজেক্টের সব জায়গার একই নিয়ম (Master সব, বাকিরা নিজের
     * ব্রাঞ্চ) — এখানেও তাই।
     */
    fun fetchContacts(context: Context, branchFilter: String?): List<FollowUpItem> {
        val repo = FollowUpRepository(context)
        val enquiry = try { repo.fetchTab("Inquiry", branchFilter) } catch (_: Throwable) { emptyList() }
        val visit = try { repo.fetchTab("Patient", branchFilter) } catch (_: Throwable) { emptyList() }
        val patient = try { repo.fetchTab("Treatment", branchFilter) } catch (_: Throwable) { emptyList() }
        return mergeContactStages(enquiry, visit, patient)
    }

    // 🔴🔒 B502 (06.08.2026, TK-নির্দেশ — "সাথে সাথে দেখাতে হবে, সব
    // ব্রাঞ্চে সমানভাবে") — Contacts ট্যাব আগে সরাসরি ক্লাউডের উত্তরের
    // অপেক্ষা করত। এখন `FollowUpActivity.kt`-এর মতোই `loadCachedTab()`
    // (ফোনের জমানো তথ্য, তাৎক্ষণিক) দিয়ে তিনটে ধাপই আনা হয়, তারপর ঠিক
    // উপরের `fetchContacts()`-এর সেই একই মিলানোর নিয়মে (এখন
    // `mergeContactStages()`-এ আলাদা করা হয়েছে, নকল কোড এড়াতে) একত্র
    // করা হয়। ⛔ মিলানোর/সাজানোর নিয়ম একটুও বদলায়নি — শুধু পুনর্ব্যবহার।
    fun fetchCachedContacts(context: Context, branchFilter: String?): List<FollowUpItem> {
        val repo = FollowUpRepository(context)
        val enquiry = try { repo.loadCachedTab("Inquiry", branchFilter) } catch (_: Throwable) { null } ?: emptyList()
        val visit = try { repo.loadCachedTab("Patient", branchFilter) } catch (_: Throwable) { null } ?: emptyList()
        val patient = try { repo.loadCachedTab("Treatment", branchFilter) } catch (_: Throwable) { null } ?: emptyList()
        return mergeContactStages(enquiry, visit, patient)
    }

    private fun mergeContactStages(enquiry: List<FollowUpItem>, visit: List<FollowUpItem>, patient: List<FollowUpItem>): List<FollowUpItem> {
        val byMobile = LinkedHashMap<String, FollowUpItem>()
        // ⛔ ক্রম গুরুত্বপূর্ণ: পরে বসানো এন্ট্রিই জেতে, তাই সবচেয়ে এগিয়ে
        // থাকা ধাপ (Treatment) সবার শেষে বসানো হলো।
        for (item in enquiry) {
            val key = item.mobile.filter { it.isDigit() }.takeLast(10)
            if (key.length == 10) byMobile[key] = item
        }
        for (item in visit) {
            val key = item.mobile.filter { it.isDigit() }.takeLast(10)
            if (key.length == 10) byMobile[key] = item
        }
        for (item in patient) {
            val key = item.mobile.filter { it.isDigit() }.takeLast(10)
            if (key.length == 10) byMobile[key] = item
        }
        return byMobile.values.sortedBy { it.name.uppercase() }
    }
}
