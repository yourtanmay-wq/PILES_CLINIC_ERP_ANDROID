package com.tkbiswas.pilesclinic.clinical

/**
 * 🔵🔒 V556 (২২.০৮.২০২৬) — TK-এর কাগজ "Patient History Sheet"-এর **ভাগ ৪**-এর
 * সেই অংশটুকু যা অ্যাপে **ছিলই না**:
 *   • দীর্ঘমেয়াদী কোনো রোগ আছে কি না? (ডায়াবেটিস / উচ্চ রক্তচাপ / IBD / অন্যান্য)
 *   • খাবারে ফাইবারের পরিমাণ (পর্যাপ্ত / কম)
 *   • দৈনিক জল পানের পরিমাণ ( —— লিটার )
 *   • টয়লেটে দীর্ঘক্ষণ বসে থাকার অভ্যাস আছে? (হ্যাঁ / না)
 *   • অতিরিক্ত কোঁথ (Straining) দিতে হয়? (হ্যাঁ / না)
 *
 * TK-এর নিয়ম (ভাগ ৩-এর মতোই): টিকের জিনিস **পাশাপাশি চিপ**, লেখার জিনিস **বক্স**,
 * আর **একাধিক উত্তর** বাছা যাবে।
 * ⛔ কাগজের প্রতিটা শব্দ **হুবহু** — নতুন কোনো লেখা বানানো হয়নি।
 * ⛔ **নতুন কলাম/টেবিল বা SQL লাগে না** — চেক-আপের সেই একটাই লেখায় জমা হয়।
 * ⛔ এই ফাইলে Android-এর কিছুই নেই, তাই নিয়মটা **চালিয়ে যাচাই করা যায়**।
 */
object LifestyleModel {

    /* 🔴 V600 (২৩.০৮.২০২৬, TK-ধরা বাগ, ছবি-প্রুফ): "হ্যাঁ/না" প্রশ্নে আগে
       একসাথে দুটোই বাছা যেত (V556-এর "একাধিক উত্তর" নিয়মটা ভুল করে সব
       প্রশ্নেই বসানো হয়েছিল)। এখন যে প্রশ্নের উত্তর সত্যিই একটাই হতে পারে
       (হ্যাঁ *অথবা* না — দুটো একসাথে নয়), সেখানে `singleSelect=true`। */
    data class Question(val key: String, val label: String, val options: List<String>,
                         val singleSelect: Boolean = false)

    val QUESTIONS = listOf(
        Question("chronic", "দীর্ঘমেয়াদী কোনো রোগ আছে কি না?", listOf("ডায়াবেটিস", "উচ্চ রক্তচাপ", "IBD")),
        Question("fiber", "খাবারে ফাইবারের পরিমাণ", listOf("পর্যাপ্ত", "কম")),
        Question("toilet", "টয়লেটে দীর্ঘক্ষণ বসে থাকার অভ্যাস আছে?", listOf("হ্যাঁ", "না"), singleSelect = true),
        Question("strain", "অতিরিক্ত কোঁথ (Straining) দিতে হয়?", listOf("হ্যাঁ", "না"), singleSelect = true)
    )

    /** কাগজে: "দৈনিক জল পানের পরিমাণ: ———— লিটার।" */
    const val WATER_LABEL = "দৈনিক জল পানের পরিমাণ"
    const val WATER_UNIT = "লিটার"

    /** "অন্যান্য:" — কাগজে দীর্ঘমেয়াদী রোগের ঠিক নিচেই। */
    const val OTHER_LABEL = "অন্যান্য থাকলে এখানে লিখুন"

    private fun optionsOf(key: String): List<String> =
        QUESTIONS.firstOrNull { it.key == key }?.options ?: emptyList()

    /** জল-এর ঘরে শুধু সংখ্যাই (দশমিক সহ, যেমন "2.5")। */
    fun cleanWater(v: String): String {
        val kept = v.filter { it.isDigit() || it == '.' }
        val firstDot = kept.indexOf('.')
        if (firstDot < 0) return kept
        return kept.substring(0, firstDot + 1) + kept.substring(firstDot + 1).replace(".", "")
    }

    /**
     * জমা রাখার লেখা:
     *   `chronic=ডায়াবেটিস, IBD | fiber=কম | water=2 | toilet=হ্যাঁ | other=…`
     * ⛔ যেটায় কিছু বাছা/লেখা হয়নি সেটা লেখাই হয় না।
     */
    fun format(picked: Map<String, List<String>>, water: String, other: String): String {
        val parts = ArrayList<String>()
        for (q in QUESTIONS) {
            val known = q.options
            val chosen = (picked[q.key] ?: emptyList()).filter { known.contains(it) }
            if (chosen.isEmpty()) continue
            parts.add(q.key + "=" + chosen.joinToString(", "))
        }
        val w = cleanWater(water)
        if (w.isNotBlank()) parts.add("water=$w")
        val o = other.trim()
        if (o.isNotBlank()) parts.add("other=" + o.replace("|", "/"))
        return parts.joinToString(" | ")
    }

    /** উপরের লেখা আবার পড়া। ভাঙা/অচেনা লেখায় কখনো ক্র্যাশ করে না। */
    fun parse(text: String): Triple<MutableMap<String, MutableList<String>>, String, String> {
        val map = LinkedHashMap<String, MutableList<String>>()
        for (q in QUESTIONS) map[q.key] = ArrayList()
        var water = ""
        var other = ""
        if (text.isBlank()) return Triple(map, water, other)
        for (raw in text.split("|")) {
            val part = raw.trim()
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val key = part.substring(0, eq).trim()
            val value = part.substring(eq + 1).trim()
            when {
                key.equals("water", true) -> water = cleanWater(value)
                key.equals("other", true) -> other = value
                else -> {
                    val list = map[key] ?: continue
                    val known = optionsOf(key)
                    for (v in value.split(",")) {
                        val one = v.trim()
                        if (one.isNotBlank() && known.contains(one) && !list.contains(one)) list.add(one)
                    }
                }
            }
        }
        return Triple(map, water, other)
    }

    /** ছাপা/সারাংশে মানুষ-পড়া-যায় লেখা। ফাঁকা হলে ফাঁকাই। */
    fun readable(text: String): String {
        val (map, water, other) = parse(text)
        val out = ArrayList<String>()
        for (q in QUESTIONS) {
            val chosen = map[q.key] ?: continue
            if (chosen.isEmpty()) continue
            out.add(q.label + " " + chosen.joinToString(", "))
        }
        if (water.isNotBlank()) out.add("$WATER_LABEL $water $WATER_UNIT")
        if (other.isNotBlank()) out.add(other)
        return out.joinToString("; ")
    }
}
