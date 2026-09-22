package com.tkbiswas.pilesclinic.clinical

/**
 * 🔵🔒 V584 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) — Doctor Check-up-এর
 * **A4 রিপোর্ট দুই ভাষায়** (বাংলা / English) বার করার একমাত্র অভিধান ও
 * সারি-বানানোর জায়গা।
 *
 * TK-এর নির্দেশ (হুবহু):
 *   • *"এই ফর্মটা বাংলা এবং ইংরেজি দুটো যেন থাকে, আমরা যখন যেটা দেখতে চাইছি
 *     তখন সেটা যেন দেখতে পারি"*
 *   • *"হেডারে সম্পূর্ণ ডিটেইলস ইংরেজিতে থাকবে"* ⇒ ক্লিনিকের নাম/ঠিকানা,
 *     সবুজ পট্টি এবং রোগীর তথ্যের ঘর — দুই ভাষাতেই ইংরেজি। শুধু তার নিচের
 *     সেকশনগুলো ভাষা অনুযায়ী বদলায়।
 *   • TK-অনুমোদিত সীমা: **লেবেল ও তালিকা থেকে বাছা উত্তর** অনুবাদ হয়;
 *     স্টাফের নিজের হাতে টাইপ করা লেখা (Chief Complaint, Occupation,
 *     "অন্যান্য" ইত্যাদি) যেমন লেখা হয়েছে **হুবহু তেমনই** থাকে।
 *
 * ⛔ **ঝুঁকিহীন নকশা:** সম্পূর্ণ নতুন, আলাদা ফাইল। কোনো মডেল
 * (SymptomHistoryModel / HistoryDetailModel / LifestyleModel / AnatomyModel),
 * ফর্ম-ফিল্ড, সেভ-লজিক বা ডেটাবেস-কলামে এক অক্ষরও বদলায়নি — এখানে শুধু
 * তাদের **public `parse()`** ডেকে পড়া হয়। কোনো নেটওয়ার্ক/ক্লাউড কল নেই
 * (Supabase free-plan নিরাপদ)।
 *
 * ⚠️ অভিধানে না-থাকা কোনো লেখা এলে সেটা **যেমন আছে তেমনই** ফেরত যায় —
 *    কখনো ফাঁকা হয় না, তাই পুরোনো কোনো রেকর্ডও কিছু হারায় না।
 */
object CheckupA4Lang {

    const val BN = "bn"
    const val EN = "en"

    /** বাংলা → English। শুধু তালিকা/লেবেলের লেখা — টাইপ করা লেখা নয়। */
    private val EN_MAP: Map<String, String> = mapOf(
        // ── ভাগ ২ · উপসর্গের ছ'টা সারি (SymptomHistoryModel.LINES) ──
        "পায়ুপথে রক্তপাত" to "Bleeding per anus",
        "মলদ্বারে ব্যথা" to "Pain in anus",
        "ফোলা / মাংসপিণ্ড বের হওয়া" to "Mass / lump coming out",
        "পুঁজ / রক্ত / জল পড়া" to "Pus / blood / watery discharge",
        "চুলকানি / জ্বালাপোড়া" to "Itching / burning",
        "কোষ্ঠকাঠিন্য" to "Constipation",
        // "কবে থেকে?"-র একক (রেজিস্ট্রেশনের সেই একই তিনটে) — ইংরেজিতেই সেভ হয়,
        // তাই বাংলায় দেখানোর সময় উল্টো অনুবাদ লাগে (নিচের BN_MAP)।
        // ── ভাগ ৩ · ইতিহাসের চারটে দল (HistoryDetailModel.GROUPS) ──
        /* 🎨🔒 V897 (৩১.০৮.২০২৬, TK-নির্দেশ *"ইতিহাস না রেখে History করুন"*) —
           দলের নাম বদলেছে, তাই ছাপার ইংরেজি অনুবাদে **নতুন নামগুলোও** যোগ করা হলো।
           ⛔ পুরোনো চারটে লাইন রেখে দেওয়া হলো — আগে সেভ হয়ে যাওয়া রেকর্ডে
              বাংলা নামই লেখা আছে, ওগুলোর ছাপা যেন এক অক্ষরও না বদলায়। */
        "🩸 রক্তপাতের History" to "🩸 History of bleeding",
        "😣 ব্যথার History" to "😣 History of pain",
        "🫃 ফোলা / মাংসপিণ্ডের History" to "🫃 History of mass / lump",
        "💧 পুঁজ / জল পড়ার History" to "💧 History of discharge",
        "🩸 রক্তপাতের ইতিহাস" to "🩸 History of bleeding",
        "😣 ব্যথার ইতিহাস" to "😣 History of pain",
        "🫃 ফোলা / মাংসপিণ্ডের ইতিহাস" to "🫃 History of mass / lump",
        "💧 পুঁজ / জল পড়ার ইতিহাস" to "💧 History of discharge",
        "রঙ" to "Colour",
        "সময়" to "Time",
        "পরিমাণ" to "Amount",
        "ব্যথার ধরন" to "Type of pain",
        "তীব্রতা" to "Severity",
        "তরলের ধরন" to "Type of fluid",
        "গন্ধ" to "Smell",
        "পায়ুপথের কাছে ছোট ছিদ্র" to "Small opening near anus",
        "টকটকে লাল" to "Bright red",
        "কালচে" to "Dark",
        "মলের আগে" to "Before stool",
        "মলের সাথে মিশে" to "Mixed with stool",
        "মলের পরে" to "After stool",
        "যখন তখন" to "Any time",
        "অল্প" to "Little",
        "অনেক" to "Much",
        "তীক্ষ্ণ কাটাকাটা" to "Sharp cutting",
        "দপদপ করা" to "Throbbing",
        "মলত্যাগের সময় তীব্র হয় ও পরে কয়েক ঘন্টা থাকে" to "Severe during stool, lasts a few hours after",
        "সারাক্ষণ একটানা থাকে" to "Continuous all the time",
        "মৃদু" to "Mild",
        "মাঝারি" to "Moderate",
        "তীব্র" to "Severe",
        "নিজে থেকে ভেতরে চলে যায় (Spontaneous)" to "Goes back on its own (Spontaneous)",
        "ঠেলে ঢুকিয়ে দিতে হয় (Manual)" to "Has to be pushed back (Manual)",
        "সারাক্ষণ বাইরেই বের হয়ে থাকে (Irreducible)" to "Stays out all the time (Irreducible)",
        "হঠাৎ তীব্র ব্যথাসহ শক্ত হয়ে ফুলে গেছে" to "Suddenly hard and swollen with severe pain",
        "শুধু পুঁজ" to "Pus only",
        "রক্তযুক্ত পুঁজ" to "Blood-stained pus",
        "পাতলা জল" to "Watery",
        "দুর্গন্ধযুক্ত" to "Foul smelling",
        "স্বাভাবিক" to "Normal",
        "দেখা যায়" to "Visible",
        "দেখা যায় না" to "Not visible",
        // ── ভাগ ৪ · রোগ ও অভ্যাস (LifestyleModel.QUESTIONS) ──
        "দীর্ঘমেয়াদী কোনো রোগ আছে কি না?" to "Any chronic disease?",
        "খাবারে ফাইবারের পরিমাণ" to "Fibre in food",
        "টয়লেটে দীর্ঘক্ষণ বসে থাকার অভ্যাস আছে?" to "Sits long in toilet?",
        "অতিরিক্ত কোঁথ (Straining) দিতে হয়?" to "Has to strain?",
        "দৈনিক জল পানের পরিমাণ" to "Water per day",
        "লিটার" to "litre",
        "ডায়াবেটিস" to "Diabetes",
        "উচ্চ রক্তচাপ" to "High blood pressure",
        "পর্যাপ্ত" to "Adequate",
        "কম" to "Low",
        "হ্যাঁ" to "Yes",
        "না" to "No",
        // ── ভাগ ৬ · রোগের ছবিতে যা আঁকা হয়েছে (AnatomyModel.readable) ──
        "ফোলা" to "Piles",
        "মাংস ফোলানো" to "Lump raised",
        "নালীর দাগ" to "Fistula tract",
        "টা" to "no."
    )

    /** English → বাংলা (সেভ ইংরেজিতে হয় এমন কয়েকটা ঘর — যেমন সময়ের একক)। */
    private val BN_MAP: Map<String, String> = mapOf(
        "Days" to "দিন", "Months" to "মাস", "Years" to "বছর",
        "Yes" to "হ্যাঁ", "No" to "না"
    )

    /** এক টুকরো লেখা চাওয়া ভাষায়। না-চেনা লেখা হুবহু ফেরত যায়। */
    fun t(s: String, lang: String): String {
        val x = s.trim()
        if (x.isEmpty()) return x
        return if (lang == EN) EN_MAP[x] ?: x else BN_MAP[x] ?: x
    }

    /** ", " বা " / " দিয়ে জোড়া তালিকার প্রতিটা টুকরো আলাদা করে অনুবাদ। */
    fun tList(s: String, lang: String, sep: String = ", "): String =
        s.split(sep).joinToString(sep) { t(it, lang) }

    // ─────────────────────────────────────────────────────────────────────
    // সারি বানানো — মডেলের public parse() থেকেই, readable()-এর হুবহু একই ক্রমে
    // ─────────────────────────────────────────────────────────────────────

    /** ভাগ ২ — টিক দেওয়া উপসর্গ ও "কবে থেকে?"। */
    fun symptomRows(saved: String, lang: String): List<Pair<String, String>> {
        if (saved.isBlank()) return emptyList()
        val (map, other) = SymptomHistoryModel.parse(saved)
        val out = ArrayList<Pair<String, String>>()
        for (line in SymptomHistoryModel.LINES) {
            val e = map[line.key] ?: continue
            if (!e.ticked) continue
            val bits = ArrayList<String>()
            bits.add(t("হ্যাঁ", lang))
            if (e.severity.isNotBlank()) bits.add(t(e.severity, lang))
            if (e.amount.isNotBlank()) bits.add(e.amount + " " + t(e.unit, lang))
            out.add(t(line.label, lang) to bits.joinToString(" · "))
        }
        // "এছাড়া অন্য কিছু" — স্টাফের টাইপ করা লেখা, অনুবাদ হয় না
        if (other.isNotBlank()) out.add((if (lang == EN) "Other" else "অন্যান্য") to other)
        return out
    }

    /** ভাগ ৩ — চারটে দলের বাছাই, দলের শিরোনামই সারির নাম। */
    fun historyRows(saved: String, lang: String): List<Pair<String, String>> {
        if (saved.isBlank()) return emptyList()
        val (map, note) = HistoryDetailModel.parse(saved)
        val out = ArrayList<Pair<String, String>>()
        for (g in HistoryDetailModel.GROUPS) {
            val bits = ArrayList<String>()
            for (q in g.questions) {
                val chosen = map[HistoryDetailModel.fieldKey(g, q)] ?: continue
                if (chosen.isEmpty()) continue
                val vals = chosen.joinToString(", ") { t(it, lang) }
                bits.add(if (q.label.isBlank()) vals else t(q.label, lang) + ": " + vals)
            }
            if (bits.isNotEmpty()) out.add(t(g.title, lang) to bits.joinToString(" · "))
        }
        if (note.isNotBlank()) out.add((if (lang == EN) "Note" else "নোট") to note)
        return out
    }

    /** ভাগ ৪ — রোগ ও অভ্যাস। */
    fun habitRows(saved: String, lang: String): List<Pair<String, String>> {
        if (saved.isBlank()) return emptyList()
        val (map, water, other) = LifestyleModel.parse(saved)
        val out = ArrayList<Pair<String, String>>()
        for (q in LifestyleModel.QUESTIONS) {
            val chosen = map[q.key] ?: continue
            if (chosen.isEmpty()) continue
            out.add(t(q.label, lang) to chosen.joinToString(", ") { t(it, lang) })
        }
        if (water.isNotBlank())
            out.add(t(LifestyleModel.WATER_LABEL, lang) to (water + " " + t(LifestyleModel.WATER_UNIT, lang)))
        if (other.isNotBlank()) out.add((if (lang == EN) "Other" else "অন্যান্য") to other)
        return out
    }

    /**
     * ভাগ ৬ — ছবিতে যা চিহ্ন দেওয়া হয়েছে, ছোট ছোট লাইনে।
     * ⛔ `AnatomyModel.readable()` ছোঁয়া হয়নি — এখানে সেই একই গোনার নিয়ম
     *    আলাদা করে লেখা, শুধু দুই ভাষায় বার করার জন্য।
     */
    fun anatomyLines(saved: String?, lang: String): List<String> {
        val b = AnatomyModel.parse(saved)
        if (b.marks.isEmpty() && b.note.isBlank()) return emptyList()
        val out = ArrayList<String>()
        val piles = b.marks.filter { it.kind == AnatomyModel.KIND_PILE }
        val bulges = b.marks.count { it.kind == AnatomyModel.KIND_BULGE }
        val tracts = b.marks.count { it.kind == AnatomyModel.KIND_TRACT }
        for (m in piles) {
            val where = m.label.trim()
            out.add(
                if (where.isEmpty()) (if (lang == EN) "Piles mark" else "ফোলার চিহ্ন")
                else if (lang == EN) "Piles at $where" else "$where — ফোলা"
            )
        }
        if (bulges > 0) out.add(if (lang == EN) "Lump raised — $bulges no." else "মাংস ফোলানো — $bulges টা")
        if (tracts > 0) out.add(if (lang == EN) "Fistula tract — $tracts no." else "নালীর দাগ — $tracts টা")
        if (b.note.isNotBlank()) out.add(b.note)   // ডাক্তারের টাইপ করা নোট — হুবহু
        return out
    }

    // ─────────────── সেকশনের শিরোনাম ও লেবেল ───────────────

    fun s(key: String, lang: String): String = (if (lang == EN) TITLES_EN else TITLES_BN)[key] ?: key

    private val TITLES_EN: Map<String, String> = mapOf(
        "sec1" to "HISTORY &amp; PREVIOUS TREATMENT",
        "sec2" to "PATIENT'S OWN COMPLAINTS &middot; PART 2",
        "sec3" to "HISTORY AS TOLD BY THE PATIENT &middot; PART 3",
        "sec4" to "CONDITIONS &amp; HABITS &middot; PART 4",
        "sec5" to "CLINICAL FINDINGS",
        "sec6" to "DISEASE PICTURE &middot; PART 6",
        "sec7" to "TREATMENT PLAN &amp; COUNSELLING",
        "sec8" to "ESTIMATE &amp; DECISION",
        /* 📝🔒 V1276 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — তালিকা সারি ৩৯৮):
           ডাক্তারের মন্তব্যের ভাগটা এতদিন কাগজে **ছিলই না**। */
        "sec9" to "DOCTOR&#39;S REMARK",
        "complaint" to "Chief Complaint", "duration" to "Duration",
        "occupation" to "Occupation", "prevTreatment" to "Prev. Treatment",
        "patientSaid" to "Patient Said",
        "visual" to "Visual Exam", "grade" to "Proctoscopy Grade",
        "probable" to "Probable Disease",
        "onProbing" to "On Probing", "investigation" to "Investigations",
        "plan" to "Treatment Plan", "rate" to "Rate", "counselling" to "Counselling",
        "estCost" to "Estimated Cost", "recovery" to "Time Asked", "advance" to "Advance Paid",   // 🟢 V589
        "stamp" to "Clinic Stamp", "sign" to "Doctor's Signature"
    )

    private val TITLES_BN: Map<String, String> = mapOf(
        "sec1" to "ইতিহাস ও পূর্বের চিকিৎসা",
        "sec2" to "রোগী এসে প্রথমে কী কী বললেন &middot; ভাগ 2",
        "sec3" to "রোগীর বলা ইতিহাস &middot; ভাগ 3",
        "sec4" to "রোগ ও অভ্যাস &middot; ভাগ 4",
        "sec5" to "ডাক্তারি পরীক্ষা",
        "sec6" to "রোগের ছবি &middot; ভাগ 6",
        "sec7" to "চিকিৎসা পরিকল্পনা ও পরামর্শ",
        "sec8" to "খরচের হিসাব ও সিদ্ধান্ত",
        "sec9" to "ডাক্তারের মন্তব্য",   // 📝 V1276
        "complaint" to "প্রধান সমস্যা", "duration" to "কতদিন ধরে",
        "occupation" to "পেশা", "prevTreatment" to "পূর্বের চিকিৎসা",
        "patientSaid" to "রোগী যা বললেন",
        "visual" to "চোখে দেখে", "grade" to "গ্রেড",
        "probable" to "সম্ভাব্য রোগ",
        "onProbing" to "প্রোব করে", "investigation" to "পরীক্ষা-নিরীক্ষা",
        "plan" to "পরিকল্পনা", "rate" to "রেট", "counselling" to "পরামর্শ",
        "estCost" to "আনুমানিক খরচ", "recovery" to "কতদিন সময় চাওয়া হল", "advance" to "অগ্রিম",   // 🟢 V589
        "stamp" to "ক্লিনিক সিল", "sign" to "ডাক্তারের স্বাক্ষর"
    )
}
