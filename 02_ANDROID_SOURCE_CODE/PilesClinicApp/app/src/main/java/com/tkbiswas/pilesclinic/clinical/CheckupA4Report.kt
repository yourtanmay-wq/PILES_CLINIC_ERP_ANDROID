package com.tkbiswas.pilesclinic.clinical

import com.tkbiswas.pilesclinic.print.BranchCatalog

/**
 * 🆕🔒 (07.08.2026, TK-অনুমোদিত ফটো-প্রুফের পরে) — সেভ-করা Doctor Check-up-এর
 * **A4 রিপোর্ট** তৈরির একমাত্র জায়গা (ক্লিনিক-লেটারহেড + রোগীর তথ্য + ৫টা
 * সেকশন + Before/During/After ছবি + সিল/সই)।
 *
 * দুই জায়গা থেকেই এই একই টেমপ্লেট ব্যবহার হয়, তাই চেহারা কখনো আলাদা হবে না:
 *   ১) `DoctorCheckupActivity` — Save চাপার পর পর্দায় (ও 🖨 Print-এ)।
 *   ২) `PatientTimelineActivity` — 📜 History-তে পুরনো "Doctor Checkup"
 *      সারিতে চাপলে (পুরনো রেকর্ড দেখা/ছাপা)।
 *
 * ⛔ **ঝুঁকিহীন নকশা:** এটা সম্পূর্ণ নতুন, আলাদা ফাইল — কোনো ফর্ম-ফিল্ড,
 * সেভ-লজিক, ডেটাবেস-কলাম বা পুরনো ক্লাসে এক অক্ষরও বদলায় না। শুধু টেক্সট
 * থেকে HTML বানায় (কোনো নেটওয়ার্ক/ক্লাউড কল নেই — Supabase free-plan নিরাপদ)।
 */
object CheckupA4Report {

    /** রিপোর্টের উপরের অংশে যা বসে (রোগী + ব্রাঞ্চ + তারিখ)। */
    data class Info(
        val name: String = "",
        val patientId: String = "",
        val age: String = "",
        val sex: String = "",
        val mobile: String = "",
        val disease: String = "",
        val address: String = "",
        val branch: String = "",
        val date: String = "",
        val photo: String = ""   // 🔒 B551 — রোগীর ছবি (data URL), থাকলে ডিটেলসের বাঁ পাশে বসে
    )

    /** ভিতরের সব লেখা — কোনো ঘর ফাঁকা থাকলে রিপোর্টে "—" বসে। */
    data class Fields(
        val complaint: String = "", val duration: String = "", val onset: String = "",
        val occupation: String = "", val prevTreatment: String = "", val prevResult: String = "",
        val prevCost: String = "", val treatmentDuration: String = "",
        val visual: String = "", val dre: String = "", val grade: String = "",
        val onProbing: String = "", val investigation: String = "", val otherFindings: String = "",
        val treatmentPlan: String = "", val rate: String = "", val counselling: String = "",
        val estCost: String = "", val recovery: String = "", val advance: String = "",
        val decision: String = "", val remarks: String = "",
        val beforePhoto: String = "", val duringPhoto: String = "", val afterPhoto: String = "",
        /* 🔵 V584 (২৩.০৮.২০২৬, TK-নির্দেশ *"যে সমস্ত জিনিস মিসিং আছে সেগুলো
           যুক্ত করবেন"*) — কাগজের ভাগ ২ · ভাগ ৩ · ভাগ ৪ ও রোগের ছবি (ভাগ ৬)
           এতদিন প্রিন্টে যেত না, অথচ ফর্মে ভরা হত। ঘরগুলোর ডিফল্ট ফাঁকা,
           তাই পুরোনো কোনো ডাক (V584-এর আগের) এক অক্ষরও ভাঙে না। */
        val patientSaid: String = "",
        val symptomHistory: String = "",   // ভাগ ২ — SymptomHistoryModel-এর সেভ করা লেখা
        val historyDetail: String = "",    // ভাগ ৩ — HistoryDetailModel
        val lifestyle: String = "",        // ভাগ ৪ — LifestyleModel
        val anatomy: String = "",          // ভাগ ৬ — AnatomyModel (দাগগুলো)
        val anatomyImage: String = "",     // ভাগ ৬ — আঁকা ছবিটার data URL (CheckupAnatomyImage)
        val probableDisease: String = "",
        /* ⚠️ সৎ সীমাবদ্ধতা: `medical` টেবিলের পুরোনো এক-লাইনের লেখায় (📜 History)
           ভাগ ২/৩/৪-এর **মূল কাঁচা লেখাটা** থাকে না, থাকে মানুষ-পড়া-যায় রূপটা।
           সেই রেকর্ডে উপরের ঘরগুলো ফাঁকা থাকে আর নিচেরগুলো ভরে — তখন সেকশনটা
           সারি-সারি না হয়ে **এক লাইনে** বসে। কিছু হারায় না, শুধু সাজ আলাদা। */
        val symptomText: String = "", val historyText: String = "",
        val habitText: String = "", val pictureText: String = "",
        /* 📝🔒 V1276 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — তালিকা সারি ৩৯৮):
           TK: *"আমি এখানে এটা লিখলাম … কিন্তু পরের ফটোতে দেখুন এই লেখাটা কোথাও নেই"*।
           **কারণ (মেপে দেখা):** এই `Fields`-এ `doctorRemark` ঘরটাই ছিল না, আর
           `parseDetails` কোনোদিন "Doctor Remark" লেবেল খুঁজতই না ⇒ কাগজে কখনো
           বসেনি। লেখাটা হারায়নি (রেকর্ডে ও Follow-up-এর last remark-এ ছিল)।
           ⛔ ফাঁকা থাকলে ভাগটাই বসে না — পুরনো কাগজ হুবহু আগের মতোই। */
        val doctorRemark: String = ""
    )

    fun today(): String {
        // 🔴🔒 V936 (TK-নির্দেশ — এক ফরম্যাট): হাইফেন ছিল, এখন প্রজেক্টের বিন্দু।
        val f = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    // 🔒 B552 (08.08.2026, TK-নির্দেশ — "গ্লোবাল রুলস"): ঠিকানা দু'লাইনে —
    // থানা-চিহ্নের (PS/P.S/P/S/Thana/থানা/Police Station) ঠিক আগে লাইন-ব্রেক,
    // তাই ১ম লাইনে গ্রাম+পোস্ট, ২য় লাইনে থানা+জেলা। চিহ্ন না পেলে এক লাইনেই
    // থাকে (কিছু ভাঙে না)। DoctorCheckupActivity.formatAddressTwoLines-এর
    // হুবহু একই প্রমাণিত নিয়ম, শুধু এখানে HTML `<br>` দিয়ে। সেভ-হওয়া মান
    // বদলায় না — শুধু দেখানোর সময় ভাঙা হয়।
    private fun addrTwoLines(rawIn: String): String {
        val raw = rawIn.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ: "সমস্ত জায়গায় ক্যাপিটাল লেটারই করবেন") — শুধু **দেখানোর** সময় বড় হাতে; ডেটাবেসে যা লেখা আছে তা এক অক্ষরও বদলায় না।
        val markers = listOf("PS:", "P.S", "P/S", "Thana", "থানা", "Police Station")
        var idx = -1
        for (m in markers) {
            val i = raw.indexOf(m, ignoreCase = true)
            if (i > 0 && (idx == -1 || i < idx)) idx = i
        }
        if (idx <= 0) return esc(raw)
        val first = raw.substring(0, idx).trim().trimEnd(',').trim()
        val second = raw.substring(idx).trim()
        if (first.isBlank() || second.isBlank()) return esc(raw)
        return esc(first) + "<br>" + esc(second)
    }

    /**
     * 📜 History-র জন্য: `DoctorCheckupActivity.buildDetails()` যে টেক্সট
     * সেভ করে ("Complaint: x; Duration: y; ...") সেটাকেই আবার ঘরে ভাগ করে।
     * ⛔ লেবেলগুলো হুবহু buildDetails()-এর সাথে মিলিয়ে রাখা হয়েছে।
     * ⚠️ সৎ সীমাবদ্ধতা: History-র সারিতে ছবি (before/during/after) থাকে না
     * (ওগুলো আলাদা কলামে সেভ হয়), তাই পুরনো রেকর্ডের রিপোর্টে ছবির ঘর ফাঁকা।
     */
    /* 🔵🔒 V586 (২৩.০৮.২০২৬, TK-অনুমোদিত: *"পড়ার কোড দু'রকম বুঝুক"*) —
       ফোন ও কম্পিউটার **আলাদা চিহ্ন দিয়ে** ঘর জোড়ে:
         · ফোন  (`DoctorCheckupActivity.buildDetails`) → `"; "`
         · ওয়েব (`app.js`, saveDoctor)                → `" | "`
       আগে এখানে শুধু `"; "` খোঁজা হত, তাই **ওয়েবে সেভ করা চেক-আপ ফোনের
       📜 History-তে খুললে প্রথম ঘরটাই আসত**, বাকিগুলো ফাঁকা দেখাত।
       এখন দুটো চিহ্নই চেনা হয়। ⛔ সেভের নিয়মে এক অক্ষরও হাত পড়েনি, তাই
       পুরনো কোনো রেকর্ড নষ্ট হওয়ার পথ নেই — শুধু পড়া ভালো হলো। */
    private val SEPS = listOf("; ", " | ")

    /* 🔵🔒 V586 — **ঘর কোথায় শেষ, সেটা চেনার আসল নিয়ম।**
       শুধু `"; "` বা `" | "` দেখে থেমে গেলে ভুল হত, কারণ **ঘরের ভিতরের
       লেখাতেও ওই চিহ্নগুলো থাকে**:
         · `SymptomHistoryModel.readable()` উপসর্গগুলো `"; "` দিয়ে জোড়ে
         · `HistoryDetailModel.readable()` দলগুলো `" | "` দিয়ে জোড়ে
         · `LifestyleModel.readable()`-ও `"; "` দিয়ে জোড়ে
       ফলে ভাগ ২ · ৩ · ৪-এর লেখা **প্রথম চিহ্নেই কেটে** যেত — ফোনের নিজের
       সেভ করা রেকর্ডেও (চালিয়ে দেখা হয়েছে)।
       ⇒ এখন নিয়ম: বিভাজকের **ঠিক পরে যদি চেনা কোনো লেবেল + ": "** থাকে,
         তবেই সেটা সত্যিকারের ঘরের শেষ; নইলে ওটা লেখারই অংশ।
       ⛔ সেভের নিয়মে হাত পড়েনি — পুরনো সব রেকর্ড এখন **আরও ভালো** পড়বে। */
    private val KNOWN_LABELS = listOf(
        "Complaint", "Duration", "Onset", "Occupation", "Prev Treatment",
        "Patient Said", "Patient Reported", "History Detail", "Habits",
        "Probable Disease", "Time Asked", "Disease Picture",
        "Prev Result", "Prev Cost", "Treatment Duration",
        "Visual", "DRE", "Internal Piles Grade", "Grade", "Proctoscopy",
        "On Probing", "Investigation", "Investigations", "Other Findings",
        "Treatment Plan", "Other Treatment Note",
        "Est Cost", "Financial", "Recovery", "Advance",
        "Decision", "Remarks", "Documents"
    )

    fun parseDetails(note: String): Fields {
        /** এই জায়গা থেকে **সত্যিকারের** ঘরের শেষ কোথায়; না পেলে লেখার শেষ। */
        fun nextSep(from: Int): Int {
            var at = from
            while (at < note.length) {
                var best = -1; var bestLen = 0
                for (sep in SEPS) {
                    val i = note.indexOf(sep, at)
                    if (i >= 0 && (best == -1 || i < best)) { best = i; bestLen = sep.length }
                }
                if (best < 0) return note.length
                val after = best + bestLen
                if (KNOWN_LABELS.any { note.startsWith("$it: ", after) }) return best
                at = after            // চিহ্নটা লেখারই অংশ — এগিয়ে যাও
            }
            return note.length
        }
        fun one(label: String): String {
            val marker = "$label: "
            var searchFrom = 0
            var idx = -1
            while (true) {
                val found = note.indexOf(marker, searchFrom)
                if (found < 0) break
                // শুরুতে, নয়তো ঠিক আগে কোনো বিভাজক থাকলে তবেই আসল লেবেল ধরা হয় —
                // নইলে "Treatment Duration"-এর ভিতরের "Duration"-ও মিলে যেত
                // (PatientTimelineActivity-তে ধরা পড়া পুরনো বাগ, একই সুরক্ষা)।
                val ok = found == 0 || SEPS.any { found >= it.length && note.startsWith(it, found - it.length) }
                if (ok) { idx = found; break }
                searchFrom = found + 1
            }
            if (idx < 0) return ""
            val from = idx + marker.length
            return note.substring(from, nextSep(from)).trim()
        }
        /* 🔵 V586 — একই ঘর দু'জায়গায় দু'নামে লেখা হয়। যেটা আগে মেলে সেটাই
           নেওয়া হয়, তাই ফোনের ও ওয়েবের দুই ধরনের লেখাই পড়া যায়।
           ⚠️ এর মধ্যে একটা **পুরনো দোষও** সারল: ফোন লেখে "Internal Piles
              Grade", অথচ এখানে খোঁজা হত শুধু "Grade" — আর "Grade"-এর ঠিক
              আগে বিভাজক না থাকায় কখনোই মিলত না, ফলে 📜 History-র A4-এ
              **গ্রেড কোনোদিনই দেখাত না**। */
        fun field(vararg labels: String): String {
            for (l in labels) {
                val v = one(l)
                if (v.isNotBlank()) return v
            }
            return ""
        }
        val plan = field("Treatment Plan")
        // "Per Piles (Per Piles ₹8000)" — বন্ধনীর ভিতরের টাকাটাই "হার"।
        val rate = plan.substringAfter('(', "").substringBeforeLast(')', "").trim()
        val planOnly = if (plan.contains('(')) plan.substringBefore('(').trim() else plan
        return Fields(
            complaint = field("Complaint"), duration = field("Duration"), onset = field("Onset"),
            occupation = field("Occupation"), prevTreatment = field("Prev Treatment"),
            prevResult = field("Prev Result"), prevCost = field("Prev Cost"),
            treatmentDuration = field("Treatment Duration"),
            visual = field("Visual"), dre = field("DRE"),
            grade = field("Internal Piles Grade", "Grade"),
            onProbing = field("On Probing"),
            investigation = field("Investigation", "Investigations"),
            otherFindings = field("Other Findings"),
            treatmentPlan = planOnly, rate = rate, counselling = field("Other Treatment Note"),
            estCost = field("Est Cost", "Financial"),
            /* 🟢🔒 V589 (২৩.০৮.২০২৬) — এই ঘরটা এখন ভাগ ৩-এর "কতদিন সময় চাওয়া
               হল?" থেকে আসে, যেটা নোটে "Time Asked: 15 Days" নামে লেখা হয়।
               ⛔ পুরনো রেকর্ডে ওটা না থাকলে আগের "Recovery"-ই পড়া হয়, তাই
                  পুরনো কোনো কাগজ ফাঁকা হয়ে যায় না। */
            recovery = field("Time Asked", "Recovery"), advance = field("Advance"),
            decision = field("Decision"), remarks = field("Remarks"),
            // 🔵 V584 — পুরোনো লেখা থেকে যতটুকু পাওয়া যায় ততটুকুই (মানুষ-পড়া-যায় রূপ)
            patientSaid = field("Patient Said"),
            probableDisease = field("Probable Disease"),
            symptomText = field("Patient Reported"),
            historyText = field("History Detail"),
            habitText = field("Habits"),
            pictureText = field("Disease Picture"),
            // 📝 V1276 — পুরনো এক-লাইনের লেখাতেও লেবেলটা থাকলে তুলে আনা হয়
            doctorRemark = field("Doctor Remark", "Doctor's Remark")
        )
    }


    /**
     * 🔵🔒 V584 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) — সম্পূর্ণ A4
     * রিপোর্টের HTML, **এক পাতায়** ও **দুই ভাষায়**।
     *
     * TK-এর নির্দেশ, ধাপে ধাপে:
     *   • *"যেগুলো পাশাপাশি রাখা যাবে সেগুলি পাশাপাশি রেখে যাতে একটা পেজেই
     *     প্রিন্ট আউট করা যায়"* ⇒ ভাগ ২ ও ভাগ ৪ পাশাপাশি, আর নিচে রোগের ছবির
     *     পাশে ডাক্তারি পরীক্ষা + পরিকল্পনা + হিসাব।
     *   • *"ক্লিনিক্যাল ফটোগ্রাফ এটা যদি এই ফর্মে না রাখা হয় ... তাহলে এফোর
     *     সাইজে এক পেজে প্রিন্ট আউট করা যেতে পারে"* ⇒ Before/During/After-এর
     *     ঘরটা এখান থেকে বাদ (TK-অনুমোদিত)।
     *   • *"যে সমস্ত জিনিস মিসিং আছে সেগুলো যুক্ত করবেন"* ⇒ ভাগ ২ · ভাগ ৩ ·
     *     ভাগ ৪ ও রোগের ছবি (ভাগ ৬) যোগ হলো।
     *   • *"হেডারে সম্পূর্ণ ডিটেইলস ইংরেজিতে থাকবে"* ⇒ লোগো-লাইন, সবুজ পট্টি
     *     ও রোগীর তথ্যের ঘর দুই ভাষাতেই ইংরেজি।
     *
     * ⛔ **পুরোনো ডাক ভাঙে না:** `lang`-এর ডিফল্ট **English** — অর্থাৎ যে দুটো
     *    জায়গা আগে থেকে `html(info, f)` ডাকে (সেভের পরের পর্দা ও 📜 History),
     *    তারা হুবহু আগের ইংরেজি রিপোর্টই পায়। ভাষা বাছার সুযোগটা নতুন
     *    Check-up History পপ-আপে।
     * ⛔ কোনো ফর্ম-ফিল্ড/সেভ-লজিক/ডেটাবেস-কলাম ছোঁয়া হয়নি; নেটওয়ার্ক কলও নেই।
     *
     * ছবি দেখাতে WebView-এ baseURL `file:///android_asset/` দিতে হয়
     * (ক্লিনিক-লোগো assets-এ থাকে)।
     */
    @JvmOverloads
    fun html(info: Info, f: Fields, lang: String = CheckupA4Lang.EN): String {
        val b = BranchCatalog.byName(info.branch)
        val date = info.date.ifBlank { today() }
        val name = esc(info.name.ifBlank { "-" })
        val pid = esc(info.patientId.ifBlank { "-" })
        val ageSex = esc(listOf(info.age, info.sex).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "-" })
        val disease = esc(info.disease.ifBlank { "-" })
        val addr2 = addrTwoLines(info.address.ifBlank { "-" })
        val mobile = esc(info.mobile.ifBlank { "-" })

        fun t(k: String) = CheckupA4Lang.s(k, lang)
        fun v(x: String) = esc(x.ifBlank { "—" })
        /* 🔵🔒 V948 (০১.০৯.২০২৬, TK-রিপোর্ট ছবিসহ, ফটো-প্রুফ পাশ) — TK: *"মন্তব্য
           একটা সোজা লাইনে থাকার দরকার ছিল, উপর-নিচে ভেঙে আসছে"*।
           কারণ: লম্বা লেখাও অর্ধেক-চওড়া ঘরে বসত, তাই ভেঙে যেত।
           এখন লেখা লম্বা হলে ঘরটা **নিজে থেকেই** পুরো চওড়া নেয়।
           ⛔ ছোট লেখা আগের মতোই পাশাপাশি — কোনো তথ্য বাদ যায় না।
           ⛔ কাগজের প্রতিটা ভাগেই এক নিয়ম (এখানে একটাই জায়গা)। */
        fun cell(k: String, value: String, full: Boolean = false) =
            """<div class="cell${if (full || value.trim().length > 42) " full" else ""}"><span class="k">$k</span><span class="v">${v(value)}</span></div>"""
        /** সব ঘর ফাঁকা হলে সেকশনটাই বসে না — নইলে পাতায় শুধু "—" ভরা ঘর পড়ে থাকত। */
        fun sec(title: String, cells: List<String>, one: Boolean = false): String {
            if (cells.isEmpty()) return ""
            return """<div class="sec"><div class="sh">$title</div><div class="g${if (one) " one" else ""}">${cells.joinToString("")}</div></div>"""
        }
        fun rowCells(rows: List<Pair<String, String>>, full: Boolean = false): List<String> =
            rows.map { cell(esc(it.first), it.second, full) }

        // ── কাগজের ভাগ ২ / ৩ / ৪ — কাঁচা লেখা থাকলে সারি-সারি, নইলে এক লাইনে ──
        val symCells =
            if (f.symptomHistory.isNotBlank()) rowCells(CheckupA4Lang.symptomRows(f.symptomHistory, lang))
            else if (f.symptomText.isNotBlank()) listOf(cell("", f.symptomText, true)) else emptyList()
        val hisCells =
            if (f.historyDetail.isNotBlank()) rowCells(CheckupA4Lang.historyRows(f.historyDetail, lang), true)
            else if (f.historyText.isNotBlank()) listOf(cell("", f.historyText, true)) else emptyList()
        val habCells =
            if (f.lifestyle.isNotBlank()) rowCells(CheckupA4Lang.habitRows(f.lifestyle, lang))
            else if (f.habitText.isNotBlank()) listOf(cell("", f.habitText, true)) else emptyList()

        // ── ভাগ ৬ · রোগের ছবি ──
        val picLines =
            if (f.anatomy.isNotBlank()) CheckupA4Lang.anatomyLines(f.anatomy, lang)
            else if (f.pictureText.isNotBlank()) listOf(f.pictureText) else emptyList()
        val picMarks =
            if (picLines.isEmpty()) ""
            else """<div class="mk">${picLines.joinToString("<br>") { "&middot; " + esc(it) }}</div>"""
        val picBox =
            if (f.anatomyImage.isNotBlank())
                """<div class="pic"><div class="pbox"><img src="${f.anatomyImage}"></div></div>"""
            else ""
        val picSection =
            if (picBox.isBlank() && picMarks.isBlank()) ""
            else """<div class="sec tall"><div class="sh">${t("sec6")}</div>$picBox$picMarks</div>"""

        // ── ডান কলাম — ডাক্তারি পরীক্ষা · পরিকল্পনা · হিসাব ──
        val findCells = ArrayList<String>()
        if (f.visual.isNotBlank()) findCells.add(cell(t("visual"), f.visual, true))
        if (f.probableDisease.isNotBlank()) findCells.add(cell(t("probable"), f.probableDisease, true))
        if (f.grade.isNotBlank()) findCells.add(cell(t("grade"), f.grade, true))
        if (f.onProbing.isNotBlank()) findCells.add(cell(t("onProbing"), f.onProbing, true))
        if (f.investigation.isNotBlank()) findCells.add(cell(t("investigation"), f.investigation, true))

        val planCells = ArrayList<String>()
        if (f.treatmentPlan.isNotBlank()) planCells.add(cell(t("plan"), f.treatmentPlan, true))
        if (f.rate.isNotBlank()) planCells.add(cell(t("rate"), f.rate, true))
        if (f.counselling.isNotBlank()) planCells.add(cell(t("counselling"), f.counselling, true))

        val estCells = ArrayList<String>()
        if (f.estCost.isNotBlank()) estCells.add(cell(t("estCost"), f.estCost))
        if (f.recovery.isNotBlank()) estCells.add(cell(t("recovery"), f.recovery))
        if (f.advance.isNotBlank()) estCells.add(cell(t("advance"), f.advance))

        val rightCol = sec(t("sec5"), findCells, true) + sec(t("sec7"), planCells, true) + sec(t("sec8"), estCells)

        // ── ধাপ ১ ──
        val step1 = ArrayList<String>()
        if (f.complaint.isNotBlank()) step1.add(cell(t("complaint"), f.complaint))
        if (f.duration.isNotBlank()) step1.add(cell(t("duration"), f.duration))
        if (f.occupation.isNotBlank()) step1.add(cell(t("occupation"), f.occupation))
        if (f.patientSaid.isNotBlank()) step1.add(cell(t("patientSaid"), f.patientSaid))
        if (f.prevTreatment.isNotBlank()) step1.add(cell(t("prevTreatment"), f.prevTreatment, true))

        // ── পাশাপাশি সাজ — একদিক ফাঁকা হলে অন্যদিক পুরো চওড়া নেয় ──
        fun two(left: String, right: String): String = when {
            left.isBlank() && right.isBlank() -> ""
            left.isBlank() -> right
            right.isBlank() -> left
            else -> """<div class="two"><div>$left</div><div>$right</div></div>"""
        }
        /* 📝🔒 V1276 — ডাক্তারের মন্তব্য: কাগজের সবার নিচে, দস্তখতের ঠিক আগে,
           পুরো চওড়ায় — বাকি ভাগগুলোর হুবহু একই সাজ (`sec`)। লেখাটা কয়েক
           লাইনের হতে পারে, তাই লাইন-ভাঙা রাখা হয়েছে (`<br>`)।
           ⛔ ফাঁকা হলে একটাও ট্যাগ বসে না ⇒ পুরনো কাগজ এক চুলও বদলায়নি। */
        val dremSection =
            if (f.doctorRemark.isBlank()) ""
            else {
                /* ⛔ **আগে esc, তারপর লাইন-ভাঙা** — উল্টো করলে `<br>`-ও পালিয়ে
                   গিয়ে কাগজে হুবহু "&lt;br&gt;" ছাপা হত (`cell()` ভিতরে esc করে,
                   তাই এখানে ওটা ব্যবহার করা যায়নি — সাজ হুবহু একই রাখা হলো)। */
                val body = esc(f.doctorRemark.trim())
                    .replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>")
                """<div class="sec"><div class="sh">${t("sec9")}</div>""" +
                """<div class="g one"><div class="cell full"><span class="v">$body</span></div></div></div>"""
            }
        val midRow = two(sec(t("sec2"), symCells), sec(t("sec4"), habCells))
        val btmRow = when {
            picSection.isBlank() -> rightCol
            rightCol.isBlank() -> picSection
            else -> """<div class="two btm"><div>$picSection</div><div>$rightCol</div></div>"""
        }
        val photoCell = if (info.photo.isNotBlank())
            """<div class="pphoto" style="background-image:url('${info.photo}')"></div>""" else ""

        return """<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=794">
<style>
*{margin:0;padding:0;box-sizing:border-box;font-family:Georgia,'Noto Serif',serif;-webkit-print-color-adjust:exact;print-color-adjust:exact;}
body{background:#fff;color:#111;position:relative;min-height:1123px;display:flex;flex-direction:column;}
/* 🔵🔒 V948 (TK-নির্দেশ) — ক্লিনিকের জল-ছবি, ডায়েট/প্রেসক্রিপশন প্রিন্টের
   হুবহু একই নিয়ম (`DietChartHtmlPrint.wm`), শুধু TK-এর পাশ-করা মাপ:
   বড় (দুই পাশ ছোঁয়া) · আরও হালকা (৩%) · একটু উপরে। */
.wm{position:absolute;left:50%;top:45%;transform:translate(-50%,-50%);width:700px;opacity:.03;z-index:0;pointer-events:none;}
.pi,.wrap,.foot,.fn{position:relative;z-index:1;}
.gold{height:5px;background:linear-gradient(90deg,#b8912f,#e6c65c,#b8912f);}
.gbar{height:3px;background:#0f5132;}
.lh{display:flex;align-items:center;gap:16px;padding:8px 20px 6px;}
.lh img{width:78px;height:78px;object-fit:contain;flex:0 0 auto;}
.cn{font-size:23px;font-weight:800;color:#0f5132;line-height:1;}
.tag{font-size:11px;font-weight:700;color:#b8912f;letter-spacing:2px;margin-top:2px;text-transform:uppercase;font-family:Arial;}
.addr{font-size:11.5px;color:#3b4650;margin-top:3px;font-family:Arial;}
.addr b{color:#0f5132;}
.tb{background:#0f5132;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:7px 20px;font-family:Arial;}
.tb .t{font-size:13px;font-weight:800;letter-spacing:2px;}
.tb .r{font-size:10.5px;color:#cfe6d8;}
.pi{display:flex;gap:18px;padding:9px 20px;font-size:12px;font-family:Arial;background:#f7faf8;border-bottom:1.5px solid #e4ebe6;}
.pphoto{width:78px;height:94px;border:2px solid #b8912f;border-radius:4px;background-size:cover;background-position:center;background-color:#eaf0f6;flex:0 0 auto;}
.pi .c{flex:1;}
.pi .r{padding:2.5px 0;}
.pi .r b{color:#0f5132;display:inline-block;min-width:74px;}
.wrap{padding:6px 20px 10px;font-family:Arial;flex:1;}
.two{display:flex;gap:10px;}
.two>*{flex:1;min-width:0;}
.two.btm{align-items:stretch;}
.sec{margin-top:5px;border:1px solid #d5ddd7;border-radius:4px;overflow:hidden;}
.sec.tall{display:flex;flex-direction:column;height:100%;}
.sh{background:#eef5f0;color:#0f5132;font-size:11px;font-weight:800;letter-spacing:1px;padding:5.5px 12px;border-left:4px solid #b8912f;}
.g{display:flex;flex-wrap:wrap;}
/* 🔴🔒 V988 (০৩.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — "Pus / blood / watery discharge"-এর
   উত্তরটা কেটে গিয়ে শুধু "Y" দেখাচ্ছিল)। **আসল কারণ:** ঘরটা পাতার আধা চওড়ার
   ভিতরে আবার ৫০%, আর লেখাটা তার চেয়ে লম্বা — উত্তরটা ঘরের বাইরে বেরিয়ে গিয়ে
   কেটে যেত। ⇒ জায়গায় না ধরলে উত্তরটা এখন নিচের লাইনে নামে, কিছুই কাটে না।
   ⛔ যেখানে জায়গা আছে সেখানে হুবহু আগের মতোই এক লাইনে বসে। */
.cell{width:50%;padding:4.5px 12px;font-size:11.5px;border-bottom:1px solid #f0f3f1;display:flex;flex-wrap:wrap;gap:6px;line-height:1.35;}
.cell.full{width:100%;}
/* 🔴🔒 V1529 (১৮.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — লেবেলের রং আগে
   ছিল #6b7680 (হালকা ধূসর), TK-এর পড়তে কষ্ট হচ্ছিল। এখন গাঢ় #333333।
   ⛔ শুধু রং বদলেছে — ফন্ট/মাপ/গঠন/সেভ-লজিক কিছুই বদলায়নি। */
.cell .k{color:#333333;min-width:94px;flex:0 0 auto;}
.cell .v{color:#111;font-weight:700;min-width:0;overflow-wrap:anywhere;}
.cell:nth-child(odd){border-right:1px solid #f0f3f1;}
.one .cell{width:100%;border-right:0;}
.pic{display:flex;gap:14px;padding:7px 10px 3px;flex:1;}
.pbox{flex:1;min-height:170px;border:1px solid #d5ddd7;border-radius:4px;background:#fff;display:flex;align-items:center;justify-content:center;overflow:hidden;}
.pbox img{max-width:100%;max-height:225px;}
.mk{padding:4px 12px 8px;font-size:11.5px;line-height:1.7;color:#111;}
/* 🔵🔒 V948 (TK-নির্দেশ, ফটো-প্রুফ পাশ) — সই-সারি এখন ডায়েট ও প্রেসক্রিপশন
   প্রিন্টের **হুবহু একই** ধাঁচে (`DietChartHtmlPrint`-এর `.sign`/`.vfy`):
   বাঁয়ে TK BISWAS · মাঝে বারকোড · ডানে ডাক্তার, তিনটে দাগ এক সমান্তরাল লাইনে।
   ⛔ পুরনো গোল "Clinic Stamp" বাদ (TK: আর দরকার নেই)।
   ⛔ বারকোডের নিচে পেশেন্ট আইডি বসে না — উপরে Rec. No-তে আগে থেকেই আছে। */
.foot{margin-top:auto;padding:9px 20px 4px;font-family:Arial;}
.sign{display:grid;grid-template-columns:1fr auto 1fr;align-items:start;gap:22px;}
.sign .ln{border-top:.9px solid #15231C;text-align:center;padding-top:5px;}
.sign .ln b{display:block;font-size:11.2px;font-weight:900;color:#15231C;letter-spacing:.2px;}
.sign .ln small{display:block;font-size:9px;color:#54615A;margin-top:1px;}
.vfy{text-align:center;border-top:.9px solid #15231C;padding-top:5px;}
.vfy .bar{height:24px;width:121px;margin:0 auto 1.5px;background:repeating-linear-gradient(90deg,#15231C 0 1.9px,#fff 1.9px 4px);}
.vfy .vl{margin-top:2px;white-space:nowrap;}
.vfy b{display:inline;font-size:8.6px;color:#0A5428;}
.vfy small{display:inline;font-size:8.2px;color:#54615A;}
.fn{border-top:1px solid #e4ebe6;text-align:center;font-size:9.5px;color:#8a949e;padding:5px 0 6px;font-family:Arial;}
</style></head><body>
<div class="gold"></div>
<div class="lh"><img src="${b.logoAssetPath}">
<div><div class="cn">${esc(b.clinicName)}</div><div class="tag">Ayurveda &amp; Anorectal Diseases</div>
<div class="addr"><b>${esc(b.displayName)}:</b> ${esc(b.addressLine)} &nbsp;|&nbsp; <b>&#9742;</b> ${esc(b.phoneLine)} &nbsp;|&nbsp; <b>&#9742;</b> ${esc(com.tkbiswas.pilesclinic.print.BranchCatalog.HELPLINE)}</div></div></div>
<div class="gbar"></div>
<div class="tb"><span class="t">DOCTOR CHECK-UP RECORD</span><span class="r">Rec. No: $pid &nbsp;&middot;&nbsp; Date: $date</span></div>
<img class="wm" src="${b.logoAssetPath}">
<div class="pi">
$photoCell<div class="c"><div class="r"><b>Name</b> : $name</div><div class="r"><b>Patient ID</b> : $pid</div><div class="r"><b>Age / Sex</b> : $ageSex</div><div class="r"><b>Mobile</b> : $mobile</div></div>
<div class="c"><div class="r"><b>Disease</b> : $disease</div><div class="r"><b>Branch</b> : ${esc(b.displayName)}</div><div class="r"><b>Visit Date</b> : $date</div><div class="r"><b>Address</b> : <span style="display:inline-block;vertical-align:top">$addr2</span></div></div>
</div>
<div class="wrap">
${sec(t("sec1"), step1)}
$midRow
${sec(t("sec3"), hisCells, true)}
$btmRow
$dremSection
</div>
<div class="foot"><div class="sign">
<div class="ln"><b>TK BISWAS</b><small>Founder &amp; Consultant</small></div>
<div class="vfy"><div class="bar"></div>
<div class="vl"><b>Document Digitally Verified</b> &middot; <small>No Physical Signature Required</small></div></div>
<div class="ln"><b>Dr. K.H MANDAL</b><small>(B.A.M.S) Regd 12386</small></div>
</div></div>
<div class="fn">Computer-generated check-up record &middot; ${esc(b.clinicName)} &middot; Ayurveda &amp; Anorectal Diseases</div>
</body></html>"""
    }
}
