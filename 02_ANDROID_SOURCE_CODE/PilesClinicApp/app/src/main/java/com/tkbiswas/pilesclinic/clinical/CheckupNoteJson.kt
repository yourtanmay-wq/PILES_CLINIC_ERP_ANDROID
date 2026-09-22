package com.tkbiswas.pilesclinic.clinical

/**
 * 🔵🔒 V559 (২২.০৮.২০২৬, TK-অনুমোদিত: *"হ্যাঁ"*) —
 * **ফোনেও সেভ করা চেকআপ ফিরিয়ে আনা**
 *
 * ### সমস্যাটা কী ছিল
 * ফোনের CHECK-UP পর্দা আগে সেভ করা চেকআপ কখনো ফর্মে ফেরাত না। কারণ
 * `populate()`-এর ডাক তুলে দেওয়া হয়েছিল — B437-এ এক রোগীর তথ্য অন্য রোগীর
 * ঘরে বসে গিয়েছিল, তাই। ফলে V554–V558-এর কোনো ঘরই ফিরে আসত না, অথচ
 * ওয়েবে ফিরে আসত।
 *
 * ### এখন কীভাবে
 * ওয়েব অনেক দিন ধরেই পুরো চেকআপটা `patients.doctorFullNote`-এ একটা JSON
 * হিসেবে রাখে। ফোনও এখন **ঠিক সেই একই জায়গায়, সেই একই নামে** রাখবে ও
 * পড়বে। ফলে —
 *   • ⛔ নতুন কোনো কলাম বা SQL লাগে না (কলামটা আগে থেকেই আছে),
 *   • ⛔ বাড়তি কোনো query লাগে না — রোগীর সারিটা পর্দা খোলার সময় এমনিতেই
 *     একবার আনা হয়, তার ভিতরেই ঘরটা থাকে,
 *   • ডাক্তার ফোনে লিখে ওয়েবে দেখতে পাবেন, ওয়েবে লিখে ফোনে — দুই দিকেই।
 *
 * ### B437 আবার যেন না ফেরে
 * এই ফাইলটা শুধু নাম-মেলানোর কাজ করে; **কোন রোগীর** তথ্য, সেটা মেলানোর
 * দায়িত্ব ডাকার জায়গার (`DoctorCheckupActivity`) — সেখানে রোগীর id মিলিয়ে
 * তবেই `populate()` ডাকা হয়।
 *
 * ⚠️ চাবির নামগুলো ওয়েবের `saveDoctor()`-এর সাথে **হুবহু** এক রাখতে হবে,
 *    নইলে এক পাশে লেখা অন্য পাশে পড়া যাবে না।
 */
object CheckupNoteJson {

    /** যে ঘরগুলো ওয়েবে **তালিকা** হিসেবে থাকে (ফোনে কমা দিয়ে জোড়া লেখা)। */
    val LIST_KEYS = setOf("visual", "dre", "investigations", "treatmentPlan")

    private const val SEP = ", "

    /** CheckupRecord → ওয়েবের নামে লেখা। তালিকার ঘরগুলো কমা দিয়ে জোড়া থাকে। */
    fun toMap(r: CheckupRecord): Map<String, String> = linkedMapOf(
        "complaint" to r.complaint,
        "duration" to r.duration,
        "acuteChronic" to r.acuteChronic,
        "occupation" to r.occupation,
        "previousTreatment" to r.prevTreatment,
        "previousResult" to r.prevResult,
        "previousCost" to r.prevCost,
        "treatmentDuration" to r.treatmentDuration,
        "patientSaid" to r.patientSaid,
        "symptomHistory" to r.symptomHistory,
        "historyDetail" to r.historyDetail,
        "lifestyle" to r.lifestyle,
        "probableDisease" to r.probableDisease,
        "doctorRemark" to r.doctorRemark,   // 🔵 V947
        "timeAsked" to r.timeAsked,
        "anatomy" to r.anatomy,
        "visual" to r.visual,
        "visualOther" to r.visualOther,
        "dre" to r.dre,
        "dreOther" to r.dreOther,
        "grade" to r.grade,
        "proctoscopy" to r.proctoscopy,
        "onProbing" to r.onProbing,
        "investigations" to r.investigation,
        "otherFindings" to r.otherFindings,
        "treatmentPlan" to r.treatmentPlan,
        "amtPerPiles" to r.amtPerPiles,
        "amtFistulaPerInch" to r.amtFistulaPerInch,
        "amtKsharSutra" to r.amtKsharSutra,
        "counselling" to r.counselling,
        "estimatedCost" to r.estimatedCost,
        // 💰 V971 — ভাঙা হিসাব; ওয়েবেও ঠিক এই "estimate" নামেই বসে।
        "estimate" to r.estimateJson,
        "recoveryTime" to r.recoveryTime,
        "advanceDiscussed" to r.advanceDiscussed,
        "patientDecision" to r.patientDecision,
        "decisionRemark" to r.decisionRemark,
        "documents" to r.documents
    )

    /** ওয়েবের নামে লেখা → CheckupRecord। অচেনা/না-থাকা ঘর ফাঁকা থাকে। */
    fun fromMap(m: Map<String, String>): CheckupRecord {
        fun g(k: String) = m[k].orEmpty()
        return CheckupRecord(
            complaint = g("complaint"),
            duration = g("duration"),
            acuteChronic = g("acuteChronic"),
            occupation = g("occupation"),
            prevTreatment = g("previousTreatment"),
            prevResult = g("previousResult"),
            prevCost = g("previousCost"),
            treatmentDuration = g("treatmentDuration"),
            patientSaid = g("patientSaid"),
            symptomHistory = g("symptomHistory"),
            historyDetail = g("historyDetail"),
            lifestyle = g("lifestyle"),
            probableDisease = g("probableDisease"),
            doctorRemark = g("doctorRemark"),   // 🔵 V947
            timeAsked = g("timeAsked"),
            anatomy = g("anatomy"),
            visual = g("visual"),
            visualOther = g("visualOther"),
            dre = g("dre"),
            dreOther = g("dreOther"),
            grade = g("grade"),
            proctoscopy = g("proctoscopy"),
            onProbing = g("onProbing"),
            investigation = g("investigations"),
            otherFindings = g("otherFindings"),
            treatmentPlan = g("treatmentPlan"),
            amtPerPiles = g("amtPerPiles"),
            amtFistulaPerInch = g("amtFistulaPerInch"),
            amtKsharSutra = g("amtKsharSutra"),
            counselling = g("counselling"),
            estimatedCost = g("estimatedCost"),
            estimateJson = g("estimate"),
            recoveryTime = g("recoveryTime"),
            advanceDiscussed = g("advanceDiscussed"),
            patientDecision = g("patientDecision"),
            decisionRemark = g("decisionRemark"),
            documents = g("documents")
        )
    }

    /** কমা দিয়ে জোড়া লেখা → তালিকা (ওয়েবের জন্য)। */
    fun splitList(v: String): List<String> =
        v.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun joinList(items: List<String>): String = items.filter { it.isNotBlank() }.joinToString(SEP)

    /**
     * ফোনের পর্দায় **সত্যিই যে ঘরগুলো আছে**। বাকিগুলো (যেমন Advance
     * Payment, Patient Decision, Previous Result — এগুলোর ঘর ফোন থেকে
     * আগেই তুলে দেওয়া হয়েছিল) ফোন পড়েও না, দেখায়ও না।
     *
     * ⚠️ এই তালিকাটাই সবচেয়ে জরুরি বেড়া: ফোন থেকে সেভ করলে যেন **শুধু
     *    এই ঘরগুলোই** বদলায়। নইলে ডাক্তার ওয়েবে যা লিখেছিলেন, ফোন থেকে
     *    একবার সেভ করলেই সেগুলো ফাঁকা হয়ে যেত — ফোনে ঘরই নেই বলে।
     */
    val PHONE_KEYS = setOf(
        "complaint", "duration", "occupation", "previousTreatment", "patientSaid",
        "symptomHistory", "historyDetail", "lifestyle", "probableDisease", "doctorRemark", "timeAsked",
        "anatomy", "visual", "dre", "dreOther", "grade", "proctoscopy", "onProbing",
        "investigations", "treatmentPlan", "amtPerPiles", "amtFistulaPerInch",
        "amtKsharSutra", "counselling", "estimatedCost", "estimate", "recoveryTime"
    )

    /**
     * সেভ করার আগে: ওয়েব যা লিখে রেখেছিল তার উপরে **শুধু ফোনের ঘরগুলো**
     * বসানো হয়। ফোনে যে ঘর নেই, ওয়েবের সেই লেখা হুবহু থেকে যায়।
     */
    fun merge(old: Map<String, String>, r: CheckupRecord): Map<String, String> {
        val out = LinkedHashMap(old)
        for ((k, v) in toMap(r)) {
            if (!PHONE_KEYS.contains(k)) continue
            /* 💰🔒 V973 (নিজে ধরা, গভীরে যাচাই করতে গিয়ে) — **টাকার হিসাব যেন
               কখনো ফাঁকা দিয়ে মুছে না যায়।** এস্টিমেটের ভাঙা হিসাব ফর্মের কোনো
               ঘরে দেখা যায় না; কোনো কারণে ফর্মে না বসলে (যেমন ভুল রোগীর তথ্য
               ঠেকাতে `populate()` বাদ পড়লে) সেভের সময় ফাঁকা লেখা গিয়ে
               ওয়েবে বানানো হিসাবটা মুছে দিতে পারত।
               ⇒ ফাঁকা হলে আগেরটাই থাকে; হিসাব থাকলে তবেই বদলায়।
               ⛔ বাকি সব ঘরের আচরণ এক অক্ষরও বদলায়নি। */
            if (k == "estimate" && v.isBlank()) continue
            out[k] = v
        }
        return out
    }
}
