package com.tkbiswas.pilesclinic.native

import android.content.Context
import java.io.File

/**
 * 🔒 TK-APPROVED (30.07.2026 সন্ধ্যা, ফটো-প্রুফে "ঠিক আছে" · খাতার সারি B192)
 *
 * TK-এর কথা: *"শুধুমাত্র ইনকমপ্লিট পেশেন্ট এর ক্ষেত্রেই নয়, সমস্ত সেকশনের
 * ক্ষেত্রেই Google Sheets এ ডাউনলোড করার ব্যবস্থা রাখবেন।"*
 *
 * Draft-এর ছয়টা সেকশনই (My Enquiry · Enquiry Reject List · Visit Reject
 * List · Incomplete Patient · Complete Patient · Unexpected Time Calls)
 * একটাই পর্দা (DraftListActivity) দিয়ে খোলে — তাই এই একটা এক্সপোর্টার সব
 * কয়টাতেই কাজ করে, আলাদা করে ছয়বার বানাতে হয়নি।
 *
 * ⛔ প্যাটার্ন হুবহু `FollowUpSheetExporter`-এর (খাতার সারি B69, TK-approved)
 *    মতোই — পর্দায় **তখন যা দেখা যাচ্ছে ঠিক তা-ই**, **সেই একই ক্রমে** CSV
 *    হয়ে নামে। ফাইল CSV — Google Sheets ও Excel দুটোতেই সরাসরি খোলে, নতুন
 *    কোনো লাইব্রেরি লাগেনি (প্রজেক্টে আগে থেকেই থাকা `CsvExportHelper`/
 *    `FollowUpSheetExporter`-এর মতোই সাধারণ CSV লেখা)।
 * ⛔ **ডেটাবেসে কিছু লেখা হয় না, নতুন কোনো টেবিল/কলাম/SQL লাগে না।**
 * ⛔ **ক্লাউডে একটাও বাড়তি অনুরোধ নেই** — আগে থেকেই স্ক্রিনে-লোড-করা তালিকা
 *    থেকেই লেখা হয়, নতুন করে কিছু আনা হয় না।
 */
object DraftSheetExporter {

    /** এক ঘরের লেখা CSV-র নিয়ম মেনে নিরাপদ করা — FollowUpSheetExporter.cell()-এর হুবহু একই নিয়ম। */
    private fun cell(raw: String): String {
        val v = raw.replace("\r", " ").replace("\n", " ").trim()
        val needsQuote = v.contains(",") || v.contains("\"")
        val esc = v.replace("\"", "\"\"")
        return if (needsQuote) "\"$esc\"" else esc
    }

    /**
     * পর্দায় দেখা তালিকাটাকে CSV ফাইলে লিখে দেয় এবং ফাইলটা ফেরত দেয়।
     *
     * @param title  পর্দার হেডারে যা লেখা আছে (যেমন "Incomplete Patient") —
     *               ফাইলের নামে ও ভিতরের কলাম-বাছাইয়ে ব্যবহার হয়
     * @param items  ঠিক যে ক্রমে পর্দায় দেখা যাচ্ছে (position + 1 = সিরিয়াল,
     *               DraftCardAdapter-এর সিরিয়াল-নিয়মের হুবহু একই)
     */
    fun write(context: Context, title: String, items: List<DraftEntry>): File {
        // "My Enquiry" ছাড়া বাকি সব সেকশনে কার্ডে remark/status বাক্সটা
        // (e.extra বা e.lastRemark) দেখা যায় — দুটোই কলামে রাখা হলো, কোনটাই
        // বাদ পড়বে না।
        val header = listOf(
            "SL", "DATE", "NAME", "MOBILE", "BRANCH", "DISEASE",
            "PATIENT ID", "STATUS", "LAST REMARK"
        )
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { cell(it) }).append("\r\n")

        items.forEachIndexed { idx, e ->
            val mob = e.mobile.filter { it.isDigit() }.takeLast(10)
            val line = listOf(
                (idx + 1).toString(),
                if (e.recordDate.isBlank()) "" else FollowUpModel.displayDate(e.recordDate),
                e.name,
                mob,
                e.branch,
                e.disease,
                e.patientId,
                e.extra,
                e.lastRemark
            )
            sb.append(line.joinToString(",") { cell(it) }).append("\r\n")
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        // 🔴🔒 V1648 (২১.০৯.২০২৬, TK-ভিডিও-রিপোর্ট) — displayDate() "dd/MM/yyyy"
        // ফেরায় (স্ল্যাশ সহ), এখানে শুধু ডট পাল্টানো হতো — ফাইলের নামে আসল
        // স্ল্যাশ থেকে যেত ⇒ FileNotFoundException। একই ভুল আরও ৩ জায়গায় ছিল,
        // সবই ঠিক।
        val stamp = FollowUpModel.displayDate(FollowUpModel.today()).replace("/", "-")
        // ফাইলের নামে বাংলা/স্পেস/বিশেষ চিহ্ন না রাখাই ভালো (কিছু ফোনে শেয়ার
        // করার সময় সমস্যা করে) — তাই টাইটেল থেকে শুধু A-Z0-9 রাখা হলো।
        val safeTitle = title.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').ifBlank { "DRAFT" }
        val file = File(dir, "DRAFT_${safeTitle}_$stamp.csv")
        // Excel/Sheets যাতে বাংলা লেখা ঠিক দেখায় — শুরুতে UTF-8 চিহ্ন (BOM)।
        file.writeText("\uFEFF" + sb.toString(), Charsets.UTF_8)
        return file
    }
}
