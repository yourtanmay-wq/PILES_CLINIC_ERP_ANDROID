package com.tkbiswas.pilesclinic.clinical

/**
 * 🔵🔒 V555 (২২.০৮.২০২৬) — TK-এর কাগজ "Patient History Sheet"-এর **ভাগ ৩**:
 * ডান দিকের চারটে "ইতিহাস" (রক্তপাত · ব্যথা · ফোলা/মাংসপিণ্ড · পুঁজ/জল)।
 *
 * TK-এর সিদ্ধান্ত (ডেমো দেখে):
 *   • টিক মারার জিনিস **পাশাপাশি চিপ** হয়ে বসবে;
 *   • লেখার জিনিস **টাইপ করার বক্স**;
 *   • **প্রতিটা প্রশ্নে একাধিক উত্তর** বাছা যাবে;
 *   • ব্যথার তীব্রতা **শুধু এখানেই** (মৃদু/মাঝারি/তীব্র) — ভাগ ২-এর তীব্র/মৃদু তুলে দেওয়া।
 *
 * ⛔ প্রতিটা শব্দ **কাগজ থেকে হুবহু** নেওয়া — একটাও নতুন লেখা বানানো হয়নি।
 * ⛔ **নতুন কলাম/টেবিল বা SQL লাগে না** — গোটা চেক-আপ আগে থেকেই একটাই লেখা
 *    হিসেবে `medical`-এ জমা হয় (V539/V554-এর মতোই)।
 * ⛔ এই ফাইলে Android-এর কিছুই নেই, তাই নিয়মটা আলাদা করে **চালিয়ে যাচাই করা যায়**।
 */
object HistoryDetailModel {

    data class Question(val key: String, val label: String, val options: List<String>)
    data class Group(val key: String, val title: String, val questions: List<Question>)

    val GROUPS = listOf(
        Group("bleed", "🩸 রক্তপাতের History", listOf(
            Question("color", "রঙ", listOf("টকটকে লাল", "কালচে")),
            Question("time", "সময়", listOf("মলের আগে", "মলের সাথে মিশে", "মলের পরে", "যখন তখন")),
            Question("amount", "পরিমাণ", listOf("অল্প", "অনেক"))
        )),
        Group("pain", "😣 ব্যথার History", listOf(
            Question("type", "ব্যথার ধরন", listOf("তীক্ষ্ণ কাটাকাটা", "দপদপ করা")),
            Question("time", "সময়", listOf(
                "মলত্যাগের সময় তীব্র হয় ও পরে কয়েক ঘন্টা থাকে",
                "সারাক্ষণ একটানা থাকে"
            )),
            Question("severity", "তীব্রতা", listOf("মৃদু", "মাঝারি", "তীব্র"))
        )),
        Group("lump", "🫃 ফোলা / মাংসপিণ্ডের History", listOf(
            Question("state", "", listOf(
                "নিজে থেকে ভেতরে চলে যায় (Spontaneous)",
                "ঠেলে ঢুকিয়ে দিতে হয় (Manual)",
                "সারাক্ষণ বাইরেই বের হয়ে থাকে (Irreducible)",
                "হঠাৎ তীব্র ব্যথাসহ শক্ত হয়ে ফুলে গেছে"
            ))
        )),
        Group("fluid", "💧 পুঁজ / জল পড়ার History", listOf(
            Question("type", "তরলের ধরন", listOf("শুধু পুঁজ", "রক্তযুক্ত পুঁজ", "পাতলা জল")),
            Question("smell", "গন্ধ", listOf("দুর্গন্ধযুক্ত", "স্বাভাবিক")),
            Question("opening", "পায়ুপথের কাছে ছোট ছিদ্র", listOf("দেখা যায়", "দেখা যায় না"))
        ))
    )

    /** জমা রাখার চাবি — কখনো বদলাবে না। */
    fun fieldKey(group: Group, q: Question): String = group.key + "." + q.key

    /** সব চাবি এক জায়গায় (পড়া/লেখার সময় ক্রম ঠিক রাখতে)। */
    fun allKeys(): List<String> = GROUPS.flatMap { g -> g.questions.map { fieldKey(g, it) } }

    private fun optionsOf(field: String): List<String> {
        for (g in GROUPS) for (q in g.questions) if (fieldKey(g, q) == field) return q.options
        return emptyList()
    }

    /**
     * চিপগুলো **পাশাপাশি** বসানোর নিয়ম (TK-এর নির্দেশ), প্রজেক্টের নিজের
     * `buildChecks`-এর প্রমাণিত ধরন ধরেই — **দুটো করে এক সারিতে**।
     * ⛔ শুধু লম্বা লেখা (২২ অক্ষরের বেশি) একাই পুরো সারি পায়, নইলে ফোনের
     *    পর্দায় কেটে যেত। এটাই একমাত্র ব্যতিক্রম, আর এটা মাপা নিয়ম — আন্দাজ নয়।
     */
    const val LONG_OPTION = 22

    fun rowsFor(options: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var pending: String? = null
        for (opt in options) {
            if (opt.length > LONG_OPTION) {
                pending?.let { rows.add(listOf(it)); pending = null }
                rows.add(listOf(opt))
            } else if (pending == null) {
                pending = opt
            } else {
                rows.add(listOf(pending!!, opt)); pending = null
            }
        }
        pending?.let { rows.add(listOf(it)) }
        return rows
    }

    /**
     * জমা রাখার লেখা:
     *   `bleed.color=টকটকে লাল | bleed.time=মলের পরে, যখন তখন | note=…`
     * ⛔ যে প্রশ্নে কিছু বাছা হয়নি সেটা লেখাই হয় না।
     */
    fun format(picked: Map<String, List<String>>, note: String): String {
        val parts = ArrayList<String>()
        for (field in allKeys()) {
            val known = optionsOf(field)
            val chosen = (picked[field] ?: emptyList()).filter { known.contains(it) }
            if (chosen.isEmpty()) continue
            parts.add(field + "=" + chosen.joinToString(", "))
        }
        val n = note.trim()
        if (n.isNotBlank()) parts.add("note=" + n.replace("|", "/"))
        return parts.joinToString(" | ")
    }

    /** উপরের লেখা আবার পড়া। ভাঙা/অচেনা লেখায় কখনো ক্র্যাশ করে না। */
    fun parse(text: String): Pair<MutableMap<String, MutableList<String>>, String> {
        val map = LinkedHashMap<String, MutableList<String>>()
        for (field in allKeys()) map[field] = ArrayList()
        var note = ""
        if (text.isBlank()) return Pair(map, note)
        for (raw in text.split("|")) {
            val part = raw.trim()
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val field = part.substring(0, eq).trim()
            val value = part.substring(eq + 1).trim()
            if (field.equals("note", true)) { note = value; continue }
            val list = map[field] ?: continue
            val known = optionsOf(field)
            for (v in value.split(",")) {
                val one = v.trim()
                if (one.isNotBlank() && known.contains(one) && !list.contains(one)) list.add(one)
            }
        }
        return Pair(map, note)
    }

    /** ছাপা/সারাংশে মানুষ-পড়া-যায় লেখা। ফাঁকা হলে ফাঁকাই। */
    fun readable(text: String): String {
        val (map, note) = parse(text)
        val out = ArrayList<String>()
        for (g in GROUPS) {
            val bits = ArrayList<String>()
            for (q in g.questions) {
                val chosen = map[fieldKey(g, q)] ?: continue
                if (chosen.isEmpty()) continue
                val label = if (q.label.isBlank()) "" else q.label + ": "
                bits.add(label + chosen.joinToString(", "))
            }
            if (bits.isNotEmpty()) out.add(g.title + " — " + bits.joinToString("; "))
        }
        if (note.isNotBlank()) out.add(note)
        return out.joinToString(" | ")
    }
}
