package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONObject

/**
 * 🔴🔴🔒 V938 (৩১.০৮.২০২৬, TK-নির্দেশ · ডেমো-প্রুফে অনুমোদিত: *"হ্যাঁ পাশ"*)
 *
 * TK: *"উক্ত পেশেন্টের আজকে কি চিকিৎসা করা হলো — এটা যদি ডাক্তার চেকআপের সময়
 * করে দেয়, সেক্ষেত্রে সেখান থেকে যেন অটোমেটিক এখানে (চেম্বারে) চলে আসে।"*
 *
 * ### এটা নতুন কোনো পথ নয়
 * চেম্বার বোর্ডে Treatment Progress লেখা হলে ঠিক **দুটো** জায়গায় বসে
 * (`ChamberAttendanceActivity.writeTreatment` + `syncProgressToReportCard`):
 *   ১. রোগীর `followups` সারির `lastRemark` + `lastRemarkAt` (+ history)
 *   ২. ওই দিনের `payments` সারির `progress` ঘর — চেম্বার বোর্ডের **আসল উৎস**
 *      (V687-এ প্রমাণিত)
 * এই ফাইলটা হুবহু সেই দুটো কাজই করে, যাতে ডাক্তারের লেখা চেম্বার Date ·
 * Report Card · ছাপা রেজিস্টার — সব জায়গায় এক লেখা দেখায়।
 *
 * ⛔ লেখা ফাঁকা হলে কিছুই করে না — পুরনো Progress কখনো মুছে যায় না।
 * ⛔ পুরো কাজটা best-effort: ব্যর্থ হলেও চেকআপ সেভ আটকায় না
 *    (ডাকা হয় `BackgroundWork.run{}`-এর ভিতর থেকে)।
 * ⛔ `payments.remarks` ছোঁয়াই হয় না (V533-এর নিয়ম) — শুধু `progress`।
 */
object TodayTreatmentSync {

    /* 🔵🔒 V947 — লাস্ট রিমার্কে দুটো লেখা আলাদা করে চেনার জন্য। ইংরেজি রাখা
       হলো কারণ স্টাফের পর্দায় সব লেখা ইংরেজি (TK-এর নিয়ম ৯)। */
    private const val DOC_PREFIX = "Doctor: "
    private const val TREAT_PREFIX = "Today: "

    /**
     * @param patientRowId রোগীর নিজের সারির আইডি (এক নম্বরে দুজন রোগী থাকলে
     *        ঠিক তাঁরই সারিতে বসানোর জন্য — V536-এর একই নিয়ম)। জানা না থাকলে ফাঁকা।
     */
    /**
     * 🔵🔒 V947 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — `doctorRemark` যোগ হলো।
     * TK-এর স্পষ্ট নিয়ম:
     *   • **আজকের চিকিৎসা** (`text`) → আগের মতোই **দুই** জায়গায় — followups-এর
     *     `lastRemark` **এবং** ওই দিনের `payments.progress` (চেম্বার বোর্ড ও
     *     Report Card ওখান থেকেই পড়ে)। এক অক্ষরও বদলায়নি।
     *   • **ডাক্তারের মন্তব্য** (`doctorRemark`) → **শুধু** `lastRemark`-এ, যাতে
     *     স্টাফ পরে কল করার সময় দেখতে পান। ⛔ `payments.progress`-এ **কখনো নয়**
     *     — তাই চেম্বার বোর্ড ও Report Card-এ এটা দেখাবে না।
     * দুটোই থাকলে লাস্ট রিমার্কে একসাথে বসে (TK: *"দুটো একসাথে দেখাবে"*)।
     */
    fun push(
        context: Context?,
        mobile: String,
        patientRowId: String,
        text: String,
        staffName: String,
        dateKey: String = FollowUpModel.today(),
        doctorRemark: String = ""
    ) {
        val note = text.trim()
        val docNote = doctorRemark.trim()
        /* 🔵 V947 — দুটোর একটাও না থাকলে আগের মতোই কিছুই করা হয় না
           (পুরনো Progress/Remark কখনো মুছে যায় না)। */
        if (note.isBlank() && docNote.isBlank()) return
        /* 🔵 V947 — লাস্ট রিমার্কে যা বসবে: দুটোই থাকলে একসাথে, নইলে যেটা আছে।
           ⛔ নিচের `payments.progress` কিন্তু **শুধু `note`**-ই পায়। */
        val remarkText = when {
            docNote.isNotBlank() && note.isNotBlank() -> "$DOC_PREFIX$docNote · $TREAT_PREFIX$note"
            docNote.isNotBlank() -> DOC_PREFIX + docNote
            else -> note
        }
        var digits = mobile.filter { it.isDigit() }.takeLast(10)
        /* 🔴🔒 V939 (নিজে ধরা) — চেকআপ পর্দা কোন পথে খোলা হয়েছে তার উপরে
           `RoleSession.currentPatientMobile` নির্ভর করে; ফাঁকা থাকলে আগে
           নিঃশব্দে কিছুই হত না। এখন রোগীর সারি থেকে একবার নম্বরটা তুলে নেওয়া
           হয় — শুধু তখনই, আর শুধু ডাক্তার কিছু বাছলে (উপরে ফাঁকা হলে ফিরে গেছে)। */
        if (digits.length != 10 && patientRowId.isNotBlank()) {
            try {
                /* 🟢🔒 V1275 (TK-নির্দেশ, Egress) — আগে এখানে রোগীর **সব ঘর**
                   নামত, অর্থাৎ base64 `photo`-ও (৫০–১০০ KB), অথচ নিচের লাইনে
                   পড়া হয় শুধু `mobile`। ⇒ এখন শুধু দুটো ঘর।
                   ⛔ কাজ · সারি · ছাঁকনি — এক অক্ষরও বদলায়নি। */
                val rows = SupabaseClient.fetchList("patients", "id=eq.$patientRowId", 1, select = "id,mobile")
                if (rows.length() > 0) {
                    digits = rows.getJSONObject(0).optString("mobile", "").filter { it.isDigit() }.takeLast(10)
                }
            } catch (_: Throwable) { }
        }
        if (digits.length != 10) return
        val day = dateKey.ifBlank { FollowUpModel.today() }

        // ── ১. রোগীর Follow-up সারিতে (চেম্বারের `writeTreatment`-এর একই কল) ──
        try {
            val rows = SupabaseClient.findByMobileOrNull(
                "followups", digits, "id,mobile,refId,stage,status", 20
            )
            var bestId = ""
            var bestPr = -1
            if (rows != null) {
                for (i in 0 until rows.length()) {
                    val r = rows.optJSONObject(i) ?: continue
                    val st = r.optString("status", "")
                    if (st.equals("Cancelled", true) || st.equals("Rejected", true) ||
                        st.equals("Closed", true) || st.equals("Incomplete", true)) continue
                    val rid = r.optString("id", "")
                    if (rid.isBlank()) continue
                    // ঘোষিত আলাদা রোগী হলে ঠিক তাঁরই সারি (V536)
                    if (patientRowId.isNotBlank() && r.optString("refId", "") != patientRowId) continue
                    val pr = when (r.optString("stage", "")) {
                        "Treatment" -> 3; "Patient" -> 2; "Visit" -> 1; else -> 0
                    }
                    if (pr > bestPr) { bestPr = pr; bestId = rid }
                }
            }
            if (bestId.isNotBlank()) {
                FollowUpRepository(context).updateRemark(bestId, remarkText, staffName, source = "treat", eventDate = day)   // 🏷 V1192
            }
        } catch (_: Throwable) { }

        // ── ২. ওই দিনের টাকার সারির `progress` ঘরে (চেম্বার বোর্ডের আসল উৎস) ──
        /* 🔵🔒 V947 — এই ধাপে **শুধু আজকের চিকিৎসা** যায়। ডাক্তারের মন্তব্য
           এখানে কখনো লেখা হয় না (TK: *"ডাক্তারের মন্তব্য শুধু স্টাফরা কল করলে
           লাস্ট রিমার্ক হিসেবেই শো করবে"*)। শুধু মন্তব্য থাকলে এই ধাপ বাদ। */
        if (note.isBlank()) return
        try {
            val pays = SupabaseClient.fetchList("payments", "mobile=like.*$digits&date=eq.$day", 20)
            var wrote = false   // 📝 V1116 — ওই দিনে সত্যিই কোথাও বসল কিনা
            for (i in 0 until pays.length()) {
                val p = pays.optJSONObject(i) ?: continue
                val payType = p.optString("payType", "")
                if (payType == "bill_edit" || payType == "chamber_expected") continue
                val owner = p.optString("patientId", "").trim()
                val keep = if (patientRowId.isNotBlank()) owner == patientRowId
                    else !PatientModel.isDeclaredSeparateRowId(owner, digits)
                if (!keep) continue
                val pid = p.optString("id", "")
                if (pid.isBlank()) continue
                val fields = JSONObject()
                    .put("progress", note)
                    .put("updatedAt", SupabaseClient.localWriteStamp())
                val ok = try { SupabaseClient.updateById("payments", pid, fields) } catch (_: Throwable) { false }
                if (!ok && context != null) GenericUpdateQueue.queue(context, "payments", pid, fields)
                wrote = true
            }
            /* 📝🔒 V1116 (TK-রিপোর্ট, অনুমোদিত) — **টাকা না দেওয়া রোগীর লেখাটাও
               এখন থেকে যায়।** ওই দিনে একটাও টাকার সারি না থাকলে লেখাটা রাখার
               জায়গা ছিল না, তাই চেম্বার বন্ধ করে খুললে ফাঁকা দেখাত।
               ⇒ তখন একটা শূন্য টাকার সারি বসে শুধু লেখাটা ধরে রাখতে
                 (`PaymentModel.buildProgressHolderRow` — বিস্তারিত ওখানে)।
               ⛔ টাকার সারি থাকলে এই ধাপটা চলেই না — আচরণ হুবহু আগের মতোই।
               ⛔ চেম্বার পর্দার পথেও (`syncProgressToReportCard`) হুবহু একই কাজ,
                  আর আইডি স্থির বলে দুই পথে দুটো সারি হওয়ার পথ নেই। */
            if (!wrote) {
                val holder = PaymentModel.buildProgressHolderRow(
                    mobile = digits, name = "", branch = "",
                    patientRowId = patientRowId, dateKey = day,
                    progress = note, staffMobile = staffName
                )
                val ok = try { SupabaseClient.upsert("payments", holder) } catch (_: Throwable) { false }
                if (!ok && context != null) GenericUpdateQueue.queue(
                    context, "payments", holder.optString("id"), JSONObject().put("progress", note))
            }
        } catch (_: Throwable) { }
    }
}
