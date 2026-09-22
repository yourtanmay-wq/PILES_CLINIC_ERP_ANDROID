package com.tkbiswas.pilesclinic.clinical

/**
 * 🔵🔒 V554 (২২.০৮.২০২৬) — TK-এর কাগজ "Patient History Sheet"-এর **ভাগ ২**:
 * *"রোগী এসে প্রথমে কি কি সমস্যার কথা বললেন?"* — ছ'টা উপসর্গ, প্রতিটার নিজের
 * **"কবে থেকে?"**, আর সবার শেষে **"এছাড়া অন্য কিছু"** লেখার বাক্স।
 * TK-এর বাছাই: **সাজ "গ"** (কাগজের মতো সারি) + **শুরুতে টিক** +
 * ব্যথার সারিতে **তীব্র/মৃদু এক লাইনেই**।
 *
 * ⛔ **"কবে থেকে?"-র ঘর দুটো নতুন বানানো হয়নি** — রেজিস্ট্রেশনের সেই একই জোড়া
 *    (সংখ্যার ঘর + Days/Months/Years বাছাই, `RegistrationActivity.kt:58,633`),
 *    আর সেভও হয় সেই একই ধরনে ("2 Years")।
 * ⛔ **নতুন কোনো কলাম/টেবিল বা SQL লাগে না** — গোটা চেক-আপ রেকর্ডটা আগে থেকেই
 *    `buildDetails()` দিয়ে **একটাই লেখা** হিসেবে `medical` টেবিলে জমা হয়
 *    (V539-এ `proctoscopy`/`patientSaid`-ও ঠিক এভাবেই যোগ হয়েছিল)।
 * ⛔ এই ফাইলে **Android-এর কিছুই নেই** (শুধু সাধারণ Kotlin), তাই লেখা-পড়ার
 *    নিয়মটা আলাদা করে **চালিয়ে যাচাই করা যায়** — আন্দাজে ছাড়া হয়নি।
 */
object SymptomHistoryModel {

    /** রেজিস্ট্রেশনের হুবহু একই তিনটে একক (RegistrationActivity.durationUnits)। */
    val UNITS = listOf("Days", "Months", "Years")

    /** ব্যথার সারির দুটো বাছাই — কাগজে যেমন লেখা ("তীব্র / মৃদু")। */
    val SEVERITY = listOf("তীব্র", "মৃদু")

    /** কাগজের ছ'টা লাইন, কাগজের ক্রমেই। `key` কখনো বদলাবে না (জমা থাকা লেখা পড়ার চাবি)। */
    data class Line(val key: String, val label: String, val severity: Boolean = false)

    val LINES = listOf(
        Line("bleeding", "পায়ুপথে রক্তপাত"),
        /* 🔵 V555 (TK-এর সিদ্ধান্ত): *"ভাগ ২-এর তীব্র/মৃদু তুলে দিয়ে শুধু ভাগ ৩-এ
           মৃদু/মাঝারি/তীব্র রাখব"* — একই জিনিস দুবার ছিল।
           ⛔ `severity` ঘরটা **মোছা হয়নি**: আগে যাঁদের রেকর্ডে "(তীব্র)" লেখা আছে
              সেটা আগের মতোই পড়া যায়, শুধু নতুন করে আর লেখা/দেখানো হয় না। */
        Line("pain", "মলদ্বারে ব্যথা"),
        Line("prolapse", "ফোলা / মাংসপিণ্ড বের হওয়া"),
        Line("discharge", "পুঁজ / রক্ত / জল পড়া"),
        Line("itching", "চুলকানি / জ্বালাপোড়া"),
        Line("constipation", "কোষ্ঠকাঠিন্য")
    )

    data class Entry(
        var ticked: Boolean = false,
        var amount: String = "",
        var unit: String = "Days",
        var severity: String = ""
    )

    private fun cleanUnit(u: String): String =
        UNITS.firstOrNull { it.equals(u.trim(), true) } ?: "Days"

    private fun cleanAmount(a: String): String = a.filter { it.isDigit() }

    /**
     * জমা রাখার লেখা — টিক দেওয়া সারিগুলোই কেবল লেখা হয়:
     *   `bleeding=2 Years | pain=6 Months(তীব্র) | other=...`
     * ⛔ টিক না দেওয়া সারি লেখাই হয় না, তাই পুরোনো রেকর্ড বড় হয় না।
     */
    fun format(entries: Map<String, Entry>, other: String): String {
        val parts = ArrayList<String>()
        for (line in LINES) {
            val e = entries[line.key] ?: continue
            if (!e.ticked) continue
            val amt = cleanAmount(e.amount)
            val sev = if (line.severity && e.severity.isNotBlank()) "(${e.severity.trim()})" else ""
            val whenPart = if (amt.isNotBlank()) "$amt ${cleanUnit(e.unit)}" else ""
            parts.add(line.key + "=" + whenPart + sev)
        }
        val o = other.trim()
        if (o.isNotBlank()) parts.add("other=" + o.replace("|", "/"))
        return parts.joinToString(" | ")
    }

    /** উপরের লেখাটা আবার পড়া। ভাঙা/অচেনা লেখায় কখনো ক্র্যাশ করে না। */
    fun parse(text: String): Pair<MutableMap<String, Entry>, String> {
        val map = LinkedHashMap<String, Entry>()
        var other = ""
        for (line in LINES) map[line.key] = Entry()
        if (text.isBlank()) return Pair(map, other)
        for (raw in text.split("|")) {
            val part = raw.trim()
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val key = part.substring(0, eq).trim()
            var value = part.substring(eq + 1).trim()
            if (key.equals("other", true)) { other = value; continue }
            val e = map[key] ?: continue
            var sev = ""
            val open = value.indexOf('(')
            val close = value.lastIndexOf(')')
            if (open >= 0 && close > open) {
                sev = value.substring(open + 1, close).trim()
                value = value.substring(0, open).trim()
            }
            val bits = value.split(" ").filter { it.isNotBlank() }
            e.ticked = true
            e.amount = cleanAmount(bits.getOrElse(0) { "" })
            e.unit = cleanUnit(bits.getOrElse(1) { "" })
            e.severity = SEVERITY.firstOrNull { it == sev } ?: ""
        }
        return Pair(map, other)
    }

    /**
     * রেজিস্ট্রেশনে যে উপসর্গে টিক দেওয়া ছিল সেগুলো **নিজে থেকেই** টিক হয়ে আসে,
     * যাতে ডাক্তারকে আবার একই কাজ করতে না হয়।
     * চেনা হয় Chief Complaint-এর লেখা থেকেই (RegistrationActivity.symptomOptions —
     * Pain · Itching · Burning · Bleeding · Pus Discharge · Fluid Discharge ·
     * Massa Bara Hua → "Prolapsed Lump")। ⛔ কোনো নতুন নাম বানানো হয়নি।
     */
    fun ticksFromComplaint(complaint: String): Set<String> {
        val t = complaint.lowercase()
        val out = LinkedHashSet<String>()
        if (t.contains("bleed")) out.add("bleeding")
        if (t.contains("pain")) out.add("pain")
        if (t.contains("prolaps") || t.contains("massa")) out.add("prolapse")
        if (t.contains("pus") || t.contains("discharge") || t.contains("fluid")) out.add("discharge")
        if (t.contains("itch") || t.contains("burn")) out.add("itching")
        if (t.contains("constipat")) out.add("constipation")
        return out
    }

    /** ছাপা/সারাংশে মানুষ-পড়া-যায় লেখা। ফাঁকা হলে ফাঁকাই। */
    fun readable(text: String): String {
        val (map, other) = parse(text)
        val parts = ArrayList<String>()
        for (line in LINES) {
            val e = map[line.key] ?: continue
            if (!e.ticked) continue
            val sev = if (e.severity.isNotBlank()) " (${e.severity})" else ""
            val whenPart = if (e.amount.isNotBlank()) " — ${e.amount} ${e.unit}" else ""
            parts.add(line.label + sev + whenPart)
        }
        if (other.isNotBlank()) parts.add(other)
        return parts.joinToString("; ")
    }
}
