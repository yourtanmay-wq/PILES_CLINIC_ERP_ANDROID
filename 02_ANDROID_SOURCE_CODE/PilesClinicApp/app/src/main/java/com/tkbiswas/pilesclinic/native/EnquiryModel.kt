package com.tkbiswas.pilesclinic.native

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Native rebuild Step 2 -- Enquiry.
 *
 * Matches the exact columns of the "enquiries" and "followups" Supabase
 * tables (see 04_SUPABASE_DATABASE_SETUP/SUPABASE_SETUP.sql and app.js's
 * saveEnq()/ensureFollow()), so an enquiry saved from this native screen is
 * fully interchangeable with one saved from the WebView -- it shows up in
 * the same Follow-up lists, Global Search, everywhere, with no special
 * casing needed anywhere else in the app.
 */
data class EnquiryDraft(
    val date: String,
    val branch: String,
    val name: String,
    val mobileDigitsOnly: String,
    val disease: String,
    val address: String,
    val remarks: String,
    val nextFollow: String,
    val timeType: String,
    /* 🕐🔒 V1042 (TK-নির্দেশ) — সময়টা অ্যাপ **নিজে** কল-তালিকা দেখে বুঝেছে
       ("auto"), না স্টাফ **হাতে** বেছে দিয়েছে ("hand")। কলটা চেম্বারের ফোনে
       এলে অ্যাপ জানতে পারে; স্টাফের নিজের ফোনে এলে জানার উপায়ই নেই।
       ⛔ ফাঁকা থাকলে আগের মতোই — পুরনো সারিতে কিছু বসানো হয়নি। */
    val timeSource: String = "",
    /* 🩺🔒 V1070 (TK-নির্দেশ) — কোন RMP রোগী পাঠিয়েছেন। Registration-এর হুবহু
       একই ঘর, তাই পরে রেজিস্ট্রেশনে মানটা নিজে থেকেই যেতে পারে।
       ⛔ ফাঁকা হলে আগের মতোই — কোনো ঘর বাধ্যতামূলক নয়। */
    val refBy: String = "",
    val refDoctor: String = "",
    val refDoctorMobile: String = "",
    val receivedByMobile: String
)

object EnquiryModel {

    private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun today(): String = todayIso()

    fun normalizedMobile(digitsOnly: String): String = "+91$digitsOnly"

    /** Builds the "enquiries" row exactly like saveEnq() does, ready to upsert. */
    fun buildEnquiryRow(draft: EnquiryDraft, createdByMobile: String): JSONObject {
        val now = isoNow()
        return JSONObject()
            .put("id", "enq_" + UUID.randomUUID().toString().replace("-", ""))
            .put("date", draft.date)
            .put("branch", draft.branch)
            .put("name", draft.name)
            .put("mobile", normalizedMobile(draft.mobileDigitsOnly))
            .put("disease", draft.disease)
            .put("address", draft.address)
            .put("remarks", draft.remarks)
            .put("nextFollow", draft.nextFollow)
            .put("timeType", draft.timeType)
            .put("timeSource", draft.timeSource)   // 🕐 V1042
            .put("refBy", draft.refBy)                       // 🩺 V1070
            .put("refDoctor", draft.refDoctor)
            .put("refDoctorMobile", draft.refDoctorMobile)
            .put("receivedBy", draft.receivedByMobile)
            .put("stage", "Inquiry")
            .put("status", "Active")
            .put("callCount", 0)
            .put("createdBy", createdByMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
    }

    /** Builds the matching "followups" row exactly like ensureFollow() does,
     * so this enquiry immediately shows up in the Follow-up (Enquiry tab)
     * list the same way a WebView-saved one does. */
    fun buildFollowUpRow(enquiryRow: JSONObject, staffName: String): JSONObject {
        val now = isoNow()
        val history = org.json.JSONArray().put(
            JSONObject()
                .put("date", enquiryRow.getString("date"))
                .put("time", isoNow())   /* ⏰ V827 — সময়ও জমা হয় (TK: "LAST CALL তারিখের পরে যেন Time থাকে")। */
                .put("remark", enquiryRow.getString("remarks"))
                .put("staff", staffName)
        )
        /* 🔗🔒 V1005 (০৩.০৯.২০২৬, TK-নির্দেশ · প্রমাণসহ ধরা) — আগে এখানে
           **এলোমেলো** id বসত (`fu_<uuid>`), কিন্তু কম্পিউটারের self-heal একই
           এনকোয়ারির জন্য **নির্দিষ্ট** id বানায় (`fu_inq_<enquiry id>`,
           `app.js` B626)। দুটো id কখনো মিলত না, তাই কম্পিউটারের কপিতে ফোনের
           সারিটা না পৌঁছালে সে **দ্বিতীয় একটা সারি** বানিয়ে ফেলত —
           HABIBOR RAHAMAN-এর ক্ষেত্রে ঠিক সেটাই হয়েছিল (৩০.০৮ রাত ৮:০১-এ
           ফোনের সারি, ৩১.০৮-এ কম্পিউটারের আরেকটা সারি)।
           এখন ফোনও **হুবহু একই নিয়মে** id বানায়, তাই দুটো যন্ত্র একই সারিতেই
           মেলে — আর ডুপ্লিকেট হয় না।
           ⛔ id-টা এখনো "fu_" দিয়েই শুরু, তাই পুরনো কোনো নিয়ম ভাঙে না।
           ⛔ এনকোয়ারির id এইমাত্র তৈরি হয়েছে, তাই এই id কখনো আগে থেকে
              থাকতে পারে না — সেভ আটকানোর ঝুঁকি নেই। */
        return JSONObject()
            .put("id", "fu_inq_" + enquiryRow.getString("id"))
            .put("refId", enquiryRow.getString("id"))
            .put("mobile", enquiryRow.getString("mobile"))
            .put("name", enquiryRow.getString("name"))
            .put("branch", enquiryRow.getString("branch"))
            .put("disease", enquiryRow.getString("disease"))
            .put("address", enquiryRow.getString("address"))
            .put("stage", "Inquiry")
            .put("date", enquiryRow.getString("date"))
            .put("registrationDate", enquiryRow.getString("date"))
            .put("visitDate", enquiryRow.getString("date"))
            .put("lastRemark", enquiryRow.getString("remarks"))
            .put("nextFollow", enquiryRow.s("nextFollow"))
            // TK-REQUESTED ADDITION (2026-07-23): carry the Official/Unexpected
            // Time flag onto the followups row too, so the Follow-up (Enquiry)
            // card can show a small time-type badge without re-joining the
            // enquiries table. Copied from the enquiry row; blank if absent.
            .put("timeType", enquiryRow.s("timeType"))
            .put("timeSource", enquiryRow.s("timeSource"))   // 🕐 V1042
            // 🔴🔴 TK-REPORTED (31.07.2026 — "Enquiry card-এ Wifi Signal কাজ
            // করছে না, কারণ Enquiry Form Save হওয়ার সাথে সাথেই তো একটা
            // signal হতে হতো, কারণ কল এসেছে সেজন্যই তো Form টা Save করা
            // হয়েছে")। আসল কারণ (গভীরে যাচাই করে): উপরে `history`-তে ঠিক
            // এই মুহূর্তেই একটা এন্ট্রি (তারিখ+রিমার্ক+স্টাফ) যোগ হচ্ছে —
            // অর্থাৎ এই প্রথম যোগাযোগটাই একটা "কল", কিন্তু `callCount`
            // পুরনো কোডে ভুল করে ০ বসানো হত। এখন **১** — যাতে RMP/Follow-up
            // কার্ডের 📶 সিগন্যাল-বার আইকন (`ic_wifi_calls_1`) সঙ্গে সঙ্গেই
            // দেখায়, ০ বার (কোনো সিগন্যাল নেই) নয়। ⛔ `callCount == 0`-কে
            // বিশেষভাবে চেক করে এমন কোনো লজিক প্রজেক্টে কোথাও নেই (মিলিয়ে
            // দেখা হয়েছে), তাই এই বদলে অন্য কোনো working flow প্রভাবিত হয় না।
            .put("callCount", 1)
            .put("status", "Active")
            .put("history", history)
            .put("createdAt", now)
            .put("updatedAt", now)
    }

    fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
}
