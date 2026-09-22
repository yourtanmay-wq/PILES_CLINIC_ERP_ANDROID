package com.tkbiswas.pilesclinic.clinical

import java.util.UUID

/**
 * Phase 4 — Clinical Modules data models.
 *
 * These are plain in-memory data classes. They intentionally do not talk to any
 * database / Supabase table yet — persistence + real patient linkage is pending
 * backend wiring (tracked as a Phase 6 item, see ClinicalRepository header note).
 */

enum class UserRole { DOCTOR, STAFF }

/** One row inside a Prescription. */
data class MedicineEntry(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var dosage: String = "",
    var frequency: String = "",
    var duration: String = "",
    var instructions: String = "",
    var medicineType: String = ""
)

/** One requested / advised investigation (blood test etc.). */
data class InvestigationEntry(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var isSelected: Boolean = false,
    var isDoctorApproved: Boolean = false,
    val isCustom: Boolean = false,
    // 🟢🔒 V624 (২৪.০৮.২০২৬, TK-নির্দেশ) — কোন ক্যাটাগরি-পর্দা থেকে টাইপ করে
    // যোগ করা হয়েছিল, তা মনে রাখা (শুধু isCustom=true হলে ব্যবহৃত)। এই
    // ক্যাটাগরি-পর্দায় আবার এলে নিজের যোগ করা টেস্টটাই যেন দেখা যায় — অন্য
    // কোনো ক্যাটাগরির নিচে ভুল করে না বসে। ⛔ পুরনো কোনো কল-সাইট ভাঙে না
    // (ডিফল্ট ""), Save/Share/Print শুধু isSelected দেখেই কাজ করে — এই ঘরটার
    // উপর নির্ভর করে না।
    var customCategory: String = ""
)

/** One diet-chart guideline line (either "Allowed" or "Avoid" category). */
data class DietEntry(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var category: String = "Allowed", // "Allowed" or "Avoid"
    var isSelected: Boolean = false
)

/** Doctor Check-up form contents. */
data class CheckupRecord(
    // 1. History & Previous Treatment (merged, TK-নির্দেশ 04.08.2026)
    var complaint: String = "",
    var duration: String = "",
    // 🆕 TK-নির্দেশ (04.08.2026): হঠাৎ হয় নাকি ধীরে ধীরে বাড়ে।
    var acuteChronic: String = "",
    var occupation: String = "",
    var prevTreatment: String = "",
    var prevResult: String = "",
    var prevCost: String = "",
    var treatmentDuration: String = "",
    // 2. Clinical Findings
    var visual: String = "",         // comma-joined checkboxes
    var visualOther: String = "",
    var dre: String = "",            // comma-joined checkboxes
    var dreOther: String = "",
    /* 🔵🔒 V539 (২২.০৮.২০২৬, TK-নির্দেশ) — TK-এর নিজের উত্তর: *"হ্যাঁ, একই —
       পুরোনো ঘরটাই ব্যবহার করুন।"* ⇒ `grade` এখন **Internal Piles-এর Grade**
       ধরে রাখে, আর মানগুলো হুবহু আগের ("Grade I"…"Grade IV")।
       ⛔ তাই **পুরোনো প্রতিটা রেকর্ড আগের মতোই ঠিক দেখাবে** — কিছু হারায় না। */
    var grade: String = "",
    /* 🔵🔒 V539: Proctoscopy-র ঘরটা এখন **টাইপ করার বক্স** (তালিকা নয়) —
       তাই তার লেখা এই নতুন ঘরে। ⛔ নতুন কোনো ডেটাবেস-কলাম লাগে না: এই
       পর্দার পুরো রেকর্ডটা `buildDetails()` দিয়ে **একটাই লেখা** হিসেবে
       `medical` টেবিলে জমা হয় (কোড ধরে যাচাই করা)। */
    var proctoscopy: String = "",
    /* 🔵🔒 V539 (TK-নির্দেশ): *"Previous Treatment-এর নিচে আরেকটা বক্স থাকবে,
       রোগী এসে তার সমস্যার কথা আর কী কী বললেন সেটা টাইপ করব।"* */
    var patientSaid: String = "",
    /* 🔵🔒 V554 (২২.০৮.২০২৬, TK-অনুমোদিত): কাগজের *"রোগী এসে প্রথমে কি কি সমস্যার
       কথা বললেন?"* — ছ'টা টিক, প্রতিটার নিজের "কবে থেকে?" (সংখ্যা + Days/Months/Years),
       ব্যথার তীব্র/মৃদু, আর শেষে "এছাড়া অন্য কিছু"।
       সবটা **একটাই লেখায়** জমা থাকে (`SymptomHistoryModel.format`), তাই
       V539-এর মতোই **নতুন কোনো কলাম বা SQL লাগেনি**। */
    var symptomHistory: String = "",
    /* 🔵🔒 V555 (TK-অনুমোদিত): কাগজের ভাগ ৩ — চারটে "ইতিহাস"-এর বাছাই ও
       ডাক্তারের নিজের লেখা, সবটা একটাই লেখায় (`HistoryDetailModel.format`)।
       ⛔ নতুন কলাম বা SQL লাগেনি। */
    var historyDetail: String = "",
    /* 🔵🔒 V556: কাগজের ভাগ ৪-এর নতুন অংশ — দীর্ঘমেয়াদী রোগ · ফাইবার · জল ·
       টয়লেটের অভ্যাস · কোঁথ (`LifestyleModel.format`)। ⛔ নতুন কলাম/SQL লাগেনি। */
    var lifestyle: String = "",
    /* 🔵🔒 V557 (TK-অনুমোদিত): কাগজের ভাগ ৫ — "সম্ভাব্য কি রোগ?" ও
       "কতদিন সময় চাওয়া হল?" ("15 Days" ধরনে, রেজিস্ট্রেশনের মতোই)।
       ⛔ নতুন কলাম/SQL লাগেনি। */
    var probableDisease: String = "",
    /* 🔵🔒 V947 — ডাক্তারের মন্তব্য (শুধু স্টাফের কল-পর্দার Last Remark-এ যায়;
       চেম্বার বোর্ড/Report Card-এ কখনো নয়)। */
    var doctorRemark: String = "",
    var timeAsked: String = "",
    /* 🔵🔒 V558 (TK-অনুমোদিত): কাগজের হাতে-আঁকা ছবির জায়গায় "রোগের ছবি" —
       ডাক্তার ২৫টা ছবির যেটা খুশি বেছে তার উপরেই ফোলা/নালী দেখান, আর
       ফোলার উপরে আঙুল টানলে ছবির মাংসটাই বেড়ে যায়।
       সবটা একটাই লেখায় (`AnatomyModel.format`) — ⛔ নতুন কলাম/SQL লাগেনি। */
    var anatomy: String = "",
    // 🆕 TK-নির্দেশ (04.08.2026): নতুন সেকশন — C (Proctoscopy Grade) আর
    // পুরনো D (Investigations, এখন E) এর মাঝে। প্রোব ঢোকানোর পরে নালি
    // কোন দিকে গেল, কতটা গেল — ডাক্তার টাইপ করে লেখেন।
    var onProbing: String = "",
    var investigation: String = "",  // comma-joined checkboxes
    var otherFindings: String = "",
    // 3. Counselling & Advice
    // 🆕 TK-নির্দেশ (04.08.2026): "কীভাবে চিকিৎসা করা হবে" — Tick-বক্স
    // (comma-joined, visual/dre/investigation-এর হুবহু একই প্যাটার্ন) +
    // তিনটে অপশনের পাশে এডিটেবল টাকা।
    var treatmentPlan: String = "",
    var amtPerPiles: String = "8000",
    var amtFistulaPerInch: String = "11000",
    var amtKsharSutra: String = "6000",
    // ⛔ TK-নির্দেশ (04.08.2026): "Disease Explanation Given" বক্সটাই থেকে
    // যাচ্ছে (কোনো নতুন ঘর/কলাম না) — শুধু লেবেল/হিন্ট বদলে "অন্যান্য
    // চিকিৎসার কথা" হলো। ভ্যারিয়েবলের নাম (counselling) ইচ্ছাকৃতভাবে
    // অপরিবর্তিত রাখা হলো, যাতে পুরনো কোনো রেকর্ড/প্রিন্ট/প্রিন্টম্যাপার
    // এই ঘর খুঁজতে ব্যর্থ না হয়।
    var counselling: String = "",
    // 4. Estimate & Decision (merged, TK-নির্দেশ 04.08.2026)
    var estimatedCost: String = "",
    /* 💰🔒 V971 (TK-অনুমোদিত) — এস্টিমেটের ভাঙা হিসাব, JSON লেখা হিসেবে।
       ⛔ চেকআপের **আগে থেকেই থাকা** নোট-JSON-এর ভিতরেই যায় ⇒ নতুন কলাম/SQL নেই।
       ⛔ ফাঁকা থাকলে সব আগের মতোই — পুরনো চেকআপে কিছু ভাঙে না। */
    var estimateJson: String = "",
    var recoveryTime: String = "",
    var advanceDiscussed: String = "",
    var patientDecision: String = "",
    var decisionRemark: String = "",
    // 5. Photo & Video (treatment photos, base64 data URLs)
    var beforePhoto: String = "",
    var duringPhoto: String = "",
    var afterPhoto: String = "",
    var documents: String = "",
    var savedAt: Long = 0L,
    var savedByRole: UserRole = UserRole.DOCTOR
)

// 🟢🔒 V676 (২৫.০৮.২০২৬, TK-নির্দেশ — "আজকের Doctor Checkup সম্পূর্ণ এডিট
// করতে পারব")। ⛔ ঝুঁকিহীন: `details`-এর টেক্সট-ব্লব রিভার্স-পার্স করার
// বদলে (TK-এর নিজের ঝুঁকি-সতর্কতা, খাতার সারি — "ভুল রিভার্স-পার্স হলে
// ডাক্তারের লেখা তথ্য হারাতে/এলোমেলো হতে পারে") — প্রতিটা ঘর এখানে **সরাসরি,
// এক এক করে হাতে** JSON-এ লেখা/পড়া হয়, কোনো অনুমান/regex/স্প্লিট নেই। তাই
// একটাই ঝুঁকি: নতুন কোনো ঘর `CheckupRecord`-এ যোগ হলে এই দুটো ফাংশনেও
// যোগ করতে হবে (নইলে সেই একটা ঘর শুধু এডিটে ফাঁকা আসবে, বাকি সব ঠিক থাকবে)।
fun CheckupRecord.toJsonString(): String {
    val o = org.json.JSONObject()
    o.put("complaint", complaint).put("duration", duration).put("acuteChronic", acuteChronic)
        .put("occupation", occupation).put("prevTreatment", prevTreatment).put("prevResult", prevResult)
        .put("prevCost", prevCost).put("treatmentDuration", treatmentDuration)
        .put("visual", visual).put("visualOther", visualOther).put("dre", dre).put("dreOther", dreOther)
        .put("grade", grade).put("proctoscopy", proctoscopy).put("patientSaid", patientSaid)
        .put("symptomHistory", symptomHistory).put("historyDetail", historyDetail).put("lifestyle", lifestyle)
        .put("probableDisease", probableDisease).put("doctorRemark", doctorRemark).put("timeAsked", timeAsked).put("anatomy", anatomy)
        .put("onProbing", onProbing).put("investigation", investigation).put("otherFindings", otherFindings)
        .put("treatmentPlan", treatmentPlan).put("amtPerPiles", amtPerPiles)
        .put("amtFistulaPerInch", amtFistulaPerInch).put("amtKsharSutra", amtKsharSutra)
        .put("counselling", counselling).put("estimatedCost", estimatedCost)
        .put("estimate", estimateJson).put("recoveryTime", recoveryTime)
        .put("advanceDiscussed", advanceDiscussed).put("patientDecision", patientDecision)
        .put("decisionRemark", decisionRemark).put("beforePhoto", beforePhoto).put("duringPhoto", duringPhoto)
        .put("afterPhoto", afterPhoto).put("documents", documents)
    return o.toString()
}

/** null হলে বোঝা যায় এই সারিটা পুরনো (blob-only) — caller তখন এডিট নয়, শুধু
 *  আগের মতো A4 রিপোর্ট-দেখা পথে যাবে (⛔ কোনো ডেটা হারানোর ঝুঁকি নেই)। */
fun checkupRecordFromJsonStringOrNull(json: String): CheckupRecord? {
    if (json.isBlank()) return null
    return try {
        val o = org.json.JSONObject(json)
        // এই key-টাই নিশ্চিত করে এটা সত্যিই আমাদের structured JSON, অন্য কোনো
        // পুরনো "selected" টেক্সট (Prescription/Diet-এর মতো, যদি ভুলবশত এখানে
        // চলে আসে) নয়।
        if (!o.has("complaint")) return null
        CheckupRecord(
            complaint = o.optString("complaint"), duration = o.optString("duration"),
            acuteChronic = o.optString("acuteChronic"), occupation = o.optString("occupation"),
            prevTreatment = o.optString("prevTreatment"), prevResult = o.optString("prevResult"),
            prevCost = o.optString("prevCost"), treatmentDuration = o.optString("treatmentDuration"),
            visual = o.optString("visual"), visualOther = o.optString("visualOther"),
            dre = o.optString("dre"), dreOther = o.optString("dreOther"), grade = o.optString("grade"),
            proctoscopy = o.optString("proctoscopy"), patientSaid = o.optString("patientSaid"),
            symptomHistory = o.optString("symptomHistory"), historyDetail = o.optString("historyDetail"),
            lifestyle = o.optString("lifestyle"), probableDisease = o.optString("probableDisease"),
            timeAsked = o.optString("timeAsked"), anatomy = o.optString("anatomy"),
            onProbing = o.optString("onProbing"), investigation = o.optString("investigation"),
            otherFindings = o.optString("otherFindings"), treatmentPlan = o.optString("treatmentPlan"),
            amtPerPiles = o.optString("amtPerPiles").ifBlank { "8000" },
            amtFistulaPerInch = o.optString("amtFistulaPerInch").ifBlank { "11000" },
            amtKsharSutra = o.optString("amtKsharSutra").ifBlank { "6000" },
            counselling = o.optString("counselling"), estimatedCost = o.optString("estimatedCost"),
            estimateJson = o.optString("estimate"),
            recoveryTime = o.optString("recoveryTime"), advanceDiscussed = o.optString("advanceDiscussed"),
            patientDecision = o.optString("patientDecision"), decisionRemark = o.optString("decisionRemark"),
            beforePhoto = o.optString("beforePhoto"), duringPhoto = o.optString("duringPhoto"),
            afterPhoto = o.optString("afterPhoto"), documents = o.optString("documents")
        )
    } catch (_: Throwable) { null }
}

/** One entry in the patient's clinical timeline / history view. */
data class ClinicalVisit(
    val id: String = UUID.randomUUID().toString(),
    val patientName: String,
    val type: String, // "Check-up" | "Prescription" | "Investigation Advice" | "Diet Chart"
    val summary: String,
    val timestamp: Long,
    val doneByRole: UserRole
)
