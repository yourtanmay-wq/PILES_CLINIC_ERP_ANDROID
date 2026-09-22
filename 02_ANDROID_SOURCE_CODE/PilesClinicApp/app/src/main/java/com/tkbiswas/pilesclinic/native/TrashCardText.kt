package com.tkbiswas.pilesclinic.native



/**
 * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — **Trash Bin-এর কার্ড ও 👁 View-এর লেখা।**
 *
 * TK-এর কথা: *"এই কার্ডেই পেশেন্ট এর সম্পূর্ণ ডিটেলস ছোট আকারে দেখানো থাকবে...
 * ভিউ একটা অপশন থাকার দরকার ছিল, যাতে সেখানে চাপ দিলে অন্তত আমি একবার দেখে
 * নিতে পারি যে এনাকে আমি রিস্টোর করবো না ডিলিট ফরএভার করব।"*
 *
 * ─── কেন আলাদা ফাইল ─────────────────────────────────────────────────────
 * একই লেখা দু-জায়গায় লাগে — ছোট কার্ডে আর 👁 View পপ-আপে। দুই জায়গায় আলাদা
 * করে লিখলে ভবিষ্যতে একটা বদলালে অন্যটা পিছিয়ে থাকত (এই প্রকল্পে এই ভুল আগে
 * অনেকবার হয়েছে)। তাই নিয়মটা **এক জায়গায়**।
 *
 * ⛔ এই ফাইল কোনো তথ্য পড়ে না, লেখে না, ক্লাউডে যায় না — Trash সারির ভিতরে
 *    **যে JSON আগে থেকেই আছে**, শুধু সেটাকে পড়ার মতো করে সাজায়।
 * ⛔ সব লেখা **ইংরেজি** (TK: *"বাংলা থাকবে না"*)।
 */
object TrashCardText {

    /** ISO তারিখ (yyyy-MM-dd, বা তার সাথে সময়/অক্ষর জোড়া থাকলেও প্রথম ১০ অক্ষর
     *  ধরে) → DOT ফরম্যাট (dd.MM.yyyy) — প্রজেক্টের সর্বত্র ব্যবহৃত একই নিয়ম।
     *  ⛔ পার্স ব্যর্থ হলে (ফাঁকা/অচেনা ফরম্যাট) আসল লেখাটাই অক্ষত ফেরত যায় —
     *     কখনো কিছু হারায় না, শুধু বদলায় না।
     */
    private fun dotDate(raw: String): String {
        val v = raw.trim()
        if (v.isBlank()) return v
        return try {
            val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(v.take(10))
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(d!!)
        } catch (_: Throwable) { v }
    }

    /** কোন টেবিল থেকে মোছা — চিপে দেখানোর নাম। */
    fun sourceLabel(table: String): String = when (table.trim().lowercase()) {
        "followups" -> "FOLLOW-UP"
        "payments" -> "PAYMENT"
        "enquiries" -> "ENQUIRY"
        "patients" -> "PATIENT"
        "doctor_visits" -> "DR. VISIT"
        "medical" -> "MEDICINE"
        else -> table.uppercase().ifBlank { "RECORD" }
    }

    /** কবে মোছা হয়েছে — কার্ডের ডানদিকের ছোট লেখা।
     *  ⛔ `deletedAt` ফোনের নিজের ঘড়িতে লেখা হয় (শেষে শুধু 'Z' বসানো), তাই
     *     UTC ধরে পড়লে সময় ৫:৩০ ঘণ্টা এগিয়ে যেত — ২৫.০৭.২০২৬-এ ধরা পড়া
     *     সেই ফিক্সটাই এখানেও হুবহু রাখা হলো। */
    fun whenText(item: TrashItem): String {
        if (item.deletedAt.isBlank()) return ""
        return try {
            val d = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .parse(item.deletedAt) ?: return ""
            /* 🔴 V1162 (০৭.০৯.২০২৬, TK: *"২ আর ৩ করুন"*) — তারিখ ও সময় এখন
               **এক লাইনে**, প্রকল্পের লক করা চেহারায়: `31/12/2026 : 3.15 PM`।
               ⛔ আগে মাঝে লাইন-ভাঙা ছিল, আর ডাকার জায়গায় সেটাকে ", " বা "  "
                  দিয়ে বদলানো হত — তাই তিন জায়গায় তিন রকম দেখাত। */
            java.text.SimpleDateFormat("dd/MM/yyyy : h.mm a", java.util.Locale.US).format(d)
        } catch (_: Throwable) { "" }
    }

    /** কে মুছেছেন — মোবাইল নম্বরের বদলে স্টাফের নাম (২২.০৭.২০২৬-এর নিয়ম)। */
    fun deletedByName(item: TrashItem): String {
        if (item.deletedBy.isBlank()) return ""
        return try {
            StaffDirectory.findAccount(item.deletedBy)?.name ?: item.deletedBy
        } catch (_: Throwable) { item.deletedBy }
    }

    /**
     * 🟢🔒🔒 V660 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — "কবে মুছেছে" আর
     * "কে মুছেছে" এখন **একই লাইনে পাশাপাশি** (আগে দুটো আলাদা জায়গায় ছিল —
     * একটা কার্ডের ডান-উপরে, আরেকটা নিচে আলাদা সারিতে, যা বাড়তি জায়গা নিত)।
     * ⛔ দুটো তথ্যই অক্ষত, কোনো ঘর/হিসাব বদলায়নি — শুধু দেখানোর জায়গা।
     */
    fun whenAndBy(item: TrashItem): String {
        val w = whenText(item)   // 🔴 V1162 — লাইন-ভাঙা আর নেই
        val by = deletedByName(item)
        return when {
            w.isNotBlank() && by.isNotBlank() -> "🗑 $w · Deleted by $by"
            w.isNotBlank() -> "🗑 $w"
            by.isNotBlank() -> "🗑 Deleted by $by"
            else -> ""
        }
    }

    /** তৃতীয় চিপ — টাকার অঙ্ক (payments) বা রোগের নাম (বাকিদের)। */
    fun extraChip(item: TrashItem): String {
        val r = item.record
        val amt = r.s("amount").trim()
        if (amt.isNotBlank() && amt != "0") {
            val n = amt.replace(Regex("[^0-9.]"), "")
            if (n.isNotBlank()) return "₹" + n.trimEnd('.').ifBlank { n }
        }
        val disease = r.s("disease").trim()
        if (disease.isNotBlank()) return disease.uppercase()
        return ""
    }

    /**
     * কার্ডের ১ম লাইন — মোবাইল ও পেশেন্ট আইডি।
     * 🟢🔒 V660 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — আইডি না থাকলে
     * (যেমন Enquiry-তে, যার কোনো Patient ID হয় না) এখন এই একই লাইনেই
     * রেকর্ডের নিজের তারিখ বসে (dot-ফরম্যাটে) — আগে সেটা আলাদা লাইনে
     * (line2) একাই থাকত, বাড়তি জায়গা নিত।
     */
    fun line1(item: TrashItem): String {
        val r = item.record
        val parts = mutableListOf<String>()
        val mob = r.s("mobile").trim()
        if (mob.isNotBlank()) parts.add("📞 $mob")
        /* 🔴🔒 V762 (২৭.০৮.২০২৬, TK-রিপোর্ট ছবিসহ: *"Patient ID ভুল কেন?"*)
           **আসল কারণ (কোড ধরে যাচাই):** টেবিল-ভেদে ঘর দুটোর মানে **উল্টো**—
             · `patients` সারিতে  → `patientId` = আসল আইডি (COB-28072026-003)
             · `payments` সারিতে  → `patientId` = **ভিতরের সারি-আইডি**
               (`pat_7f6251fdac…`), আর আসল আইডি থাকে `patientCode`-এ
               (PaymentModel: `.put("patientId", patient.id)` ও
                `.put("patientCode", patient.patientId)`)
           আগে সবসময় `patientId` আগে দেখা হতো, তাই পেমেন্টের কার্ডে মানুষের
           বদলে যন্ত্রের আইডি দেখাত।
           ⇒ এখন **যেটা মানুষের আইডির মতো দেখতে সেটাই** বাছা হয়। ভিতরের
             আইডিতে সবসময় `_` থাকে (pat_ · enq_ · rfnd_ · brief_), আর আসল
             আইডিতে কখনো থাকে না (COB-28072026-003) — এটাই পার্থক্যের চিহ্ন।
           ⛔ কোনো তথ্য বদলানো হয়নি — শুধু কোনটা **দেখানো** হবে সেটা ঠিক করা। */
        fun looksHuman(v: String) = v.isNotBlank() && !v.contains("_")
        val pid = listOf(r.s("patientCode").trim(), r.s("patientId").trim())
            .firstOrNull { looksHuman(it) }.orEmpty()
        if (pid.isNotBlank()) {
            parts.add(pid)
        } else {
            // 🟢🔒 V660 — আইডি নেই (Enquiry) — তাই তারিখটাই এখানে পাশাপাশি বসে।
            val d = r.s("date").trim()
            if (d.isNotBlank()) parts.add(dotDate(d))
        }
        return parts.joinToString("  ·  ")
    }

    /**
     * কার্ডের ২য় লাইন — টেবিল অনুযায়ী সবচেয়ে দরকারি ঘরগুলো।
     * 🟢🔒 V660 — সব তারিখ এখন dot-ফরম্যাটে (আগে raw ISO "2026-08-20" দেখাত)।
     * ⛔ Enquiry-র ক্ষেত্রে তারিখ এখন line1-এই বসে (উপরে) — তাই এখানে আর
     *    আলাদা করে বসে না, খালি হলে লাইনটাই দেখা যায় না (কার্ড আরও ছোট)।
     */
    fun line2(item: TrashItem): String {
        val r = item.record
        val parts = mutableListOf<String>()
        when (item.table.trim().lowercase()) {
            "payments" -> {
                addIf(parts, r.s("payType").trim())
                addDateIf(parts, r.s("date").trim())
                addIf(parts, r.s("mode").trim())
                // 🔴🔒 V1439 (১৩.০৯.২০২৬, TK-প্রশ্ন — "By Mobile number কেন?")
                // আসল কারণ: `receivedBy`-তে স্টাফের **মোবাইল** জমা থাকে (কোডেই
                // তাই লেখা, PaymentModel.kt) — নিচেই "Deleted by" ঠিক এই একই
                // ঘর থেকে StaffDirectory দিয়ে নাম বার করে, কিন্তু এই লাইনটা
                // raw মোবাইলই দেখাত। এখন একই নিয়ম — নাম না পেলে মোবাইলই থাকে।
                val byMobile = r.s("receivedBy").trim()
                val by = if (byMobile.isNotBlank())
                    (try { StaffDirectory.findAccount(byMobile)?.name } catch (_: Throwable) { null }) ?: byMobile
                else ""
                if (by.isNotBlank()) parts.add("by $by")
            }
            "followups" -> {
                addIf(parts, r.s("stage").trim())
                val st = r.s("status").trim()
                if (st.isNotBlank()) parts.add(st)
                val nf = r.s("nextFollow").trim()
                if (nf.isNotBlank()) parts.add("Next call " + dotDate(nf))
            }
            "patients" -> {
                addDateIf(parts, r.s("registrationDate").trim())
                val bill = r.s("bill").trim()
                if (bill.isNotBlank() && bill != "0") parts.add("Bill ₹$bill")
                addIf(parts, r.s("stage").trim())
            }
            "enquiries" -> {
                // 🟢🔒 V660 — তারিখ এখন line1-এই (আইডি না থাকলে সেখানে বসে),
                // তাই এখানে শুধু refBy — ডুপ্লিকেট তারিখ নেই।
                addIf(parts, r.s("refBy").trim())
            }
            else -> {
                addDateIf(parts, r.s("date").trim())
                addIf(parts, r.s("stage").trim())
            }
        }
        return parts.joinToString("  ·  ")
    }

    private fun addIf(list: MutableList<String>, v: String) {
        if (v.isNotBlank()) list.add(v)
    }

    /** 🟢🔒 V660 — addIf-এর তারিখ-সংস্করণ, dot-ফরম্যাটে বসায়। */
    private fun addDateIf(list: MutableList<String>, v: String) {
        if (v.isNotBlank()) list.add(dotDate(v))
    }

    /**
     * 👁 View পপ-আপের সব ঘর — যে টেবিল থেকে মোছা হয়েছে, সেই অনুযায়ী।
     * ⛔ `photo`-র মতো বিশাল ঘর ইচ্ছে করেই বাদ (পপ-আপ ভরে যেত)।
     * ⛔ যে ঘর ফাঁকা, সেটা দেখানোই হয় না — পপ-আপ ছোট থাকে।
     */
    fun viewFields(item: TrashItem): List<Pair<String, String>> {
        val r = item.record
        val out = mutableListOf<Pair<String, String>>()
        fun add(label: String, key: String) {
            val v = r.s(key).trim()
            if (v.isNotBlank()) out.add(label to v)
        }
        // 🟢🔒 V660 (২৫.০৮.২০২৬) — তারিখ-ঘরের জন্য, dot-ফরম্যাটে বসায়।
        fun addDate(label: String, key: String) {
            val v = r.s(key).trim()
            if (v.isNotBlank()) out.add(label to dotDate(v))
        }
        out.add("From" to sourceLabel(item.table))
        add("Name", "name")
        add("Mobile", "mobile")
        add("Alt. mobile", "altMobile")
        add("Patient ID", "patientId")
        add("Patient code", "patientCode")
        add("Branch", "branch")
        when (item.table.trim().lowercase()) {
            "payments" -> {
                add("Pay type", "payType")
                add("Amount", "amount")
                add("Mode", "mode")
                addDate("Date", "date")
                add("Received by", "receivedBy")
                add("Remarks", "remarks")
            }
            "followups" -> {
                add("Stage", "stage")
                add("Status", "status")
                add("Disease", "disease")
                add("Address", "address")
                add("Last remark", "lastRemark")
                addDate("Next call", "nextFollow")
                add("Call count", "callCount")
                addDate("Record date", "date")
            }
            "patients" -> {
                add("Age", "age")
                add("Sex", "sex")
                add("Address", "address")
                add("Disease", "disease")
                add("Diagnosis", "diagnosis")
                add("Stage", "stage")
                add("Bill", "bill")
                add("Discount", "discount")
                add("Referred by", "refBy")
                addDate("Registration date", "registrationDate")
            }
            "enquiries" -> {
                add("Disease", "disease")
                add("Address", "address")
                add("Referred by", "refBy")
                addDate("Date", "date")
                add("Remarks", "remarks")
            }
            else -> {
                add("Stage", "stage")
                add("Status", "status")
                addDate("Date", "date")
                add("Remarks", "remarks")
            }
        }
        add("Created by", "createdBy")
        val by = deletedByName(item)
        if (by.isNotBlank()) out.add("Deleted by" to by)
        val w = whenText(item)   // 🔴 V1162 — লাইন-ভাঙা আর নেই
        if (w.isNotBlank()) out.add("Deleted at" to w)
        val rid = r.s("id").trim()
        if (rid.isNotBlank()) out.add("Record ID" to rid)
        return out
    }
}
