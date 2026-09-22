package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import java.io.File

/**
 * 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B69)
 *
 * TK-এর কথা: *"সিরিয়াল নাম্বারের পাশে আরো একটা জিনিস রাখতে হবে — গুগল সিট,
 * অর্থাৎ আমি ডাউনলোড করলে যেন গুগল সিটে দেখতে পাই। সেখানে থাকবে সমস্ত
 * ডিটেলস — DATE (FIRST ENTRY DATE / FIRST REGISTRATION DATE / FIRST ADVANCE
 * DATE), NAME, MOB, BRANCH, ENQUIRY FOR/DISEASES, PATIENT ADDRESS।"*
 *
 * কী নামে: পর্দায় **তখন যা দেখা যাচ্ছে ঠিক তা-ই** — যে ট্যাব খোলা, যে ব্রাঞ্চ
 * বাছা, যে ছাঁকনি চালু, খোঁজার লেখা সহ; আর **যে ক্রমে দেখা যাচ্ছে সেই ক্রমেই**।
 *
 * ঘরগুলো (TK-এর তালিকা + আলোচনায় পাশ করা বাড়তি):
 *   SL · DATE · NAME · MOBILE · BRANCH · DISEASE · ADDRESS · PATIENT ID ·
 *   LAST CALL · NEXT CALL · LAST REMARK      — আর Patient-এ বাড়তি: BILL · PAID · DUE
 *
 * DATE-এর মানে ভাগ অনুযায়ী:
 *   • Enquiry  → প্রথম এনকোয়ারির তারিখ
 *   • Visit    → রেজিস্ট্রেশনের তারিখ
 *   • Patient  → **প্রথম অ্যাডভান্সের তারিখ** (টাকার তালিকা থেকে বার করা)
 *
 * ⛔ **ফাইল CSV** — Google Sheets ও Excel দুটোতেই সরাসরি খোলে, অ্যাপে নতুন কোনো
 *    লাইব্রেরি লাগে না (প্রজেক্টে আগে থেকেই এই একই পথ ব্যবহার হচ্ছে —
 *    `CsvExportHelper` / `ExportDataActivity`)।
 * ⛔ **ডেটাবেসে কিছু লেখা হয় না, নতুন কোনো টেবিল/কলাম লাগে না, SQL লাগে না।**
 * ⛔ Patient ছাড়া অন্য ভাগে **ক্লাউডে একটাও বাড়তি অনুরোধ নেই**; Patient-এ শুধু
 *    **একবার** টাকার তালিকা আনা হয় (প্রথম অ্যাডভান্সের তারিখের জন্য)।
 */
object FollowUpSheetExporter {

    /** এক ঘরের লেখা CSV-র নিয়ম মেনে নিরাপদ করা। */
    private fun cell(raw: String): String {
        val v = raw.replace("\r", " ").replace("\n", " ").trim()
        val needsQuote = v.contains(",") || v.contains("\"")
        val esc = v.replace("\"", "\"\"")
        return if (needsQuote) "\"$esc\"" else esc
    }

    private fun money(v: Double): String = if (v <= 0.0) "0" else "%.0f".format(v)

    private fun tabName(stage: String): String = when (stage) {
        "Inquiry" -> "ENQUIRY"
        "Patient" -> "VISIT"
        "Treatment" -> "PATIENT"
        else -> "FOLLOWUP"
    }

    /**
     * টাকার তালিকা থেকে প্রতিটি মোবাইলের **প্রথম** চিকিৎসা-পেমেন্টের তারিখ।
     * ⛔ শুধু Patient ভাগের জন্য ডাকা হয়, আর ডাকা হয় **একবারই**।
     */
    fun firstAdvanceDates(rows: JSONArray): Map<String, String> {
        val out = HashMap<String, String>()
        for (i in 0 until rows.length()) {
            val r = rows.optJSONObject(i) ?: continue
            val isTreat = PaymentModel.isOrdinalTreatmentPayment(
                r.s("payType"), r.s("remarks")
            )
            if (!isTreat) continue
            val d = r.optString("date", "").take(10)
            if (d.isBlank()) continue
            // ⛔ **আগে Patient ID ধরে**, না থাকলে মোবাইল ধরে — কারণ এক পরিবারে
            //    একটাই মোবাইল থাকলে দুজনের তারিখ গুলিয়ে যেতে পারত।
            //    (পুরনো টাকার সারিতে Patient ID লেখা থাকে না, তাই মোবাইলটাও রাখা।)
            val code = r.optString("patientCode", "").trim().uppercase()
            if (code.isNotBlank()) {
                val old = out["C:" + code]
                if (old == null || d < old) out["C:" + code] = d
            }
            val mob = r.optString("mobile", "").filter { it.isDigit() }.takeLast(10)
            if (mob.length == 10) {
                val old = out["M:" + mob]
                if (old == null || d < old) out["M:" + mob] = d
            }
        }
        return out
    }

    /**
     * পর্দায় দেখা তালিকাটাকে CSV ফাইলে লিখে দেয় এবং ফাইলটা ফেরত দেয়।
     *
     * @param stage কোন ভাগ — "Inquiry" / "Patient" (Visit) / "Treatment" (Patient)
     * @param items ঠিক যে ক্রমে পর্দায় দেখা যাচ্ছে
     * @param serials কার্ডের আইডি → সিরিয়াল নম্বর
     * @param firstAdvance মোবাইল(১০ অঙ্ক) → প্রথম অ্যাডভান্সের তারিখ (শুধু Patient-এ)
     */
    fun write(
        context: Context,
        stage: String,
        items: List<FollowUpItem>,
        serials: Map<String, Int>,
        firstAdvance: Map<String, String>
    ): File {
        val isPatientTab = stage == "Treatment"
        val header = mutableListOf(
            "SL", "DATE", "NAME", "MOBILE", "BRANCH", "DISEASE",
            "ADDRESS", "PATIENT ID", "LAST CALL", "NEXT CALL", "LAST REMARK"
        )
        if (isPatientTab) { header.add("BILL"); header.add("PAID"); header.add("DUE") }

        val sb = StringBuilder()
        sb.append(header.joinToString(",") { cell(it) }).append("\r\n")

        for (item in items) {
            val mob = item.mobile.filter { it.isDigit() }.takeLast(10)
            val rawDate = if (isPatientTab) {
                val byCode = firstAdvance["C:" + item.patientId.trim().uppercase()].orEmpty()
                byCode.ifBlank { firstAdvance["M:" + mob].orEmpty() }.ifBlank { item.recordDate }
            } else item.recordDate
            val line = mutableListOf(
                (serials[item.id] ?: 0).toString(),
                FollowUpModel.displayDate(rawDate),
                item.name,
                mob,
                item.branch,
                item.disease,
                item.address,
                item.patientId,
                if (item.lastCallDate.isBlank()) "" else FollowUpModel.displayDate(item.lastCallDate),
                if (item.nextFollow.isBlank()) "" else FollowUpModel.displayDate(item.nextFollow),
                item.lastRemark
            )
            if (isPatientTab) {
                val due = (item.bill - item.paid).let { if (it < 0.0) 0.0 else it }
                line.add(money(item.bill)); line.add(money(item.paid)); line.add(money(due))
            }
            sb.append(line.joinToString(",") { cell(it) }).append("\r\n")
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        // 🔴🔒 V1648 (২১.০৯.২০২৬, TK-ভিডিও-রিপোর্ট) — displayDate() "dd/MM/yyyy"
        // ফেরায় (স্ল্যাশ সহ), এখানে শুধু ডট পাল্টানো হতো — ফাইলের নামে আসল
        // স্ল্যাশ থেকে যেত ⇒ FileNotFoundException। একই ভুল আরও ৩ জায়গায় ছিল,
        // সবই ঠিক।
        val stamp = FollowUpModel.displayDate(FollowUpModel.today()).replace("/", "-")
        val file = File(dir, "FOLLOWUP_${tabName(stage)}_$stamp.csv")
        // Excel/Sheets যাতে বাংলা লেখা ঠিক দেখায় — শুরুতে UTF-8 চিহ্ন (BOM)।
        file.writeText("\uFEFF" + sb.toString(), Charsets.UTF_8)
        return file
    }
}
