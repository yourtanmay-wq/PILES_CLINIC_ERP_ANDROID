package com.tkbiswas.pilesclinic.native

import android.content.Context
import java.io.File

/**
 * 📊🔒 V1208 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **Yearly Registration
 * শিট (CSV)**।
 *
 * TK: *"এটা আমি গুগল শিট ডাউনলোড করতে চাই, কেন অপশন নেই?"*
 *
 * ⛔ `DraftSheetExporter.kt`-এর **হুবহু একই প্রমাণিত পথ** — একই ফোল্ডার
 *    (`cacheDir/exports`), একই UTF-8 BOM (নইলে Excel/Sheets-এ বাংলা ভাঙে),
 *    একই নিরাপদ ফাইলনাম। নতুন কোনো ধরন বানানো হয়নি।
 * ⛔ এখানে কোনো হিসাব কষা হয় না — পর্দায় যে সারিগুলো দেখানো হচ্ছে (চালু
 *    ফিল্টার সহ), হুবহু সেগুলোই কাগজে যায়, তাই পর্দা ও শিট কখনো আলাদা হয় না।
 */
object YearlyRegistrationSheetExporter {

    private fun cell(raw: String): String {
        val v = raw.replace("\r", " ").replace("\n", " ").trim()
        return if (v.contains(',') || v.contains('"')) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }

    /** 🧾 V1210 — `dd/MM/yyyy=টাকা|…` ভেঙে (তারিখ, টাকা) জোড়ার তালিকা। */
    private fun splitPays(raw: String): List<Pair<String, String>> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { seg ->
            val t = seg.trim()
            if (t.isBlank()) return@mapNotNull null
            val i = t.lastIndexOf('=')
            if (i <= 0) return@mapNotNull null
            t.substring(0, i) to t.substring(i + 1)
        }
    }

    /** 🧾 V1210 — 1ST · 2ND · 3RD · 4TH … (TK-এর নমুনার হুবহু লেখা)। */
    private fun ord(n: Int): String {
        val suffix = when {
            n % 100 in 11..13 -> "TH"
            n % 10 == 1 -> "ST"
            n % 10 == 2 -> "ND"
            n % 10 == 3 -> "RD"
            else -> "TH"
        }
        return "$n$suffix"
    }

    /** টাকার অঙ্ক — শিটে যোগ করা যায় এমন সাদামাটা সংখ্যা (কমা/চিহ্ন ছাড়া)। */
    private fun num(v: Double): String =
        if (v == 0.0) "0" else String.format(java.util.Locale.US, "%.0f", v)

    /** পর্দার সারির অবস্থা — ঠিক যা কার্ডে দেখানো হয়। */
    private fun statusOf(e: DraftEntry): String = when {
        e.extra == YearlyRegistration.SKIP_MARK -> "Removed"
        e.regTag.isNotBlank() -> e.regTag
        else -> "Counted"
    }

    fun write(context: Context, branch: String, year: String, items: List<DraftEntry>): File {
        /* 💰🔒 V1209 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"সেই রুগীর ঠিকানা লাগবে ·
           কত বিল · কত জমা · কত বাকি · কত কত তারিখে কত কত জমা করেছে"*।
           ⛔ পাঁচটাই আগে থেকে ফোনে নেমে আসা তথ্য — নতুন কোনো ক্লাউড-পড়া নেই। */
        /* 🧾🔒 V1210 (০৮.০৯.২০২৬, TK-নির্দেশ ও নমুনা-শিট, হুবহু): *"একটা কলমের
           তারিখ আর তারপরের কলমে সেই তারিখে কত জমা করেছে — সেরকম হতে হবে,
           আপনি তো একটা কলমেই সমস্ত পেমেন্ট তুলে দিয়েছেন"* · *"তাছাড়া Ref by লাগবে"*।
           ⇒ প্রতিটা জমা এখন **দুটো আলাদা কলমে** — 1ST DATE · 1ST PAYMENT ·
             2ND DATE · 2ND PAYMENT … যত জমা সবচেয়ে বেশি কারো আছে, ততগুলো জোড়া।
           ⛔ কারো কম জমা থাকলে বাকি ঘরগুলো ফাঁকা থাকে — কিছু গায়েব হয় না। */
        val pays = items.map { splitPays(it.payHistory) }
        val maxPay = pays.maxOfOrNull { it.size } ?: 0
        val header = ArrayList(listOf(
            "SL", "DATE", "NAME", "MOBILE", "BRANCH", "DISEASE",
            "PATIENT ID", "STATUS", "REGISTERED BY", "REF BY",
            "ADDRESS", "BILL", "PAID", "DUE"
        ))
        for (i in 1..maxPay) { header.add(ord(i) + " DATE"); header.add(ord(i) + " PAYMENT") }
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { cell(it) }).append("\r\n")
        items.forEachIndexed { idx, e ->
            val mob = e.mobile.filter { it.isDigit() }.takeLast(10)
            val line = ArrayList(listOf(
                (idx + 1).toString(),
                if (e.recordDate.isBlank()) "" else DateUtil.display(e.recordDate),
                e.name,
                mob,
                e.branch.ifBlank { branch },
                e.disease,
                e.patientId,
                statusOf(e),
                e.regBy,
                e.refByText,
                e.address,
                num(e.bill),
                num(e.paid),
                num(e.bill - e.paid)
            ))
            val mine = pays[idx]
            for (i in 0 until maxPay) {
                line.add(mine.getOrNull(i)?.first ?: "")
                line.add(mine.getOrNull(i)?.second ?: "")
            }
            sb.append(line.joinToString(",") { cell(it) }).append("\r\n")
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        // 🔴🔒 V1648 (২১.০৯.২০২৬, TK-ভিডিও-রিপোর্ট, RMP Advance-এর "Google Sheet"
        // বোতামে FileNotFoundException ধরা পড়ল) — displayDate() "dd/MM/yyyy"
        // ফেরায় (স্ল্যাশ দিয়ে), কিন্তু এখানে শুধু ডট (".") পাল্টানো হতো — ফাইলের
        // নামে আসল স্ল্যাশ থেকে যেত, যেটা ফোল্ডার-বিভাজক হিসেবে ধরা পড়ে ⇒ না-থাকা
        // সাব-ফোল্ডারে লিখতে গিয়ে ব্যর্থ হতো। একই ভুল প্রজেক্টের চারটে জায়গাতেই
        // (Yearly Registration, RMP Advance, Draft, Follow-up) ছিল — সবগুলোই ঠিক
        // করা হলো (নিয়ম ৭: একটা দোষ ধরলে সব জায়গায়)।
        val stamp = FollowUpModel.displayDate(FollowUpModel.today()).replace("/", "-")
        val safeBranch = branch.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').ifBlank { "ALL" }
        val file = File(dir, "YEARLY_REGISTRATION_${safeBranch}_${year}_$stamp.csv")
        // Excel/Sheets যাতে বাংলা ঠিক দেখায় — শুরুতে UTF-8 চিহ্ন (BOM)।
        file.writeText("﻿" + sb.toString(), Charsets.UTF_8)
        return file
    }
}
