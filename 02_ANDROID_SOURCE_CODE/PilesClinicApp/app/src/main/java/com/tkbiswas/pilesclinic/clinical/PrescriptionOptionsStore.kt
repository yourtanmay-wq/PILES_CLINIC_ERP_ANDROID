package com.tkbiswas.pilesclinic.clinical

import android.content.Context
import com.tkbiswas.pilesclinic.native.SupabaseClient
import java.net.URLEncoder

/** Patient-bound Prescription print choices. No database/schema change. */
object PrescriptionOptionsStore {
    private const val PREF = "prescription_print_options_v1"
    val defaultFields = linkedSetOf("disease", "symptoms", "since", "complaint")
    val moreFields = linkedMapOf(
        "previousTreatment" to "Previous Treatment",
        "previousResult" to "Previous Result",
        "onset" to "Onset",
        "treatmentDuration" to "Treatment Duration"
    )

    private fun p(context: Context) = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun key(suffix: String) = "${RoleSession.currentPatientId}_$suffix"

    fun selectedFields(context: Context): MutableSet<String> =
        (p(context).getStringSet(key("fields"), defaultFields) ?: defaultFields).toMutableSet()

    fun saveSelectedFields(context: Context, fields: Set<String>) {
        p(context).edit().putStringSet(key("fields"), fields.toSet()).apply()
    }

    fun diet(context: Context): String = p(context).getString(key("diet"), "").orEmpty()
    fun saveDiet(context: Context, value: String) = p(context).edit().putString(key("diet"), value.trim()).apply()

    /* 🔵 V488 (20.08.2026, TK-নির্দেশ): "Sitz Bath — 2 Times" আগে সবসময় টিক দেওয়া
       থাকত, তোলা যেত না (isEnabled = false)। TK-এর সিদ্ধান্ত: তোলা যাবে, আর তুললে
       ছাপা কাগজ থেকেও ADVICE লাইনটা উঠে যাবে।
       ডিফল্ট **true** — অর্থাৎ কেউ হাত না দিলে আগের মতোই টিক দেওয়া, তাই পুরনো
       কোনো রোগীর কাগজ নিজে থেকে বদলাবে না। রোগী ধরে ধরে মনে রাখা হয় (key)। */
    fun sitzBath(context: Context): Boolean = p(context).getBoolean(key("sitzBath"), true)
    fun saveSitzBath(context: Context, on: Boolean) = p(context).edit().putBoolean(key("sitzBath"), on).apply()

    fun captureCheckup(context: Context, r: CheckupRecord) {
        p(context).edit()
            .putString(key("symptoms"), listOf(r.visual, r.visualOther).filter { it.isNotBlank() }.joinToString(", "))
            .putString(key("since"), r.duration.trim())
            .putString(key("complaint"), r.complaint.trim())
            .putString(key("previousTreatment"), r.prevTreatment.trim())
            .putString(key("previousResult"), r.prevResult.trim())
            .putString(key("onset"), r.acuteChronic.trim())
            .putString(key("treatmentDuration"), r.treatmentDuration.trim())
            .apply()
    }

    /** Refresh only this patient's last saved Doctor Check-up. A failed read
     * keeps the patient-bound phone copy unchanged; no guessed value is used. */
    fun refreshFromLatestCheckup(context: Context) {
        refreshFromCheckupRecord(context)
        fillBlanksFromRegistration(context)   // 🔵 V548
    }

    /* ════════════════════════════════════════════
       🔵 V548 (২২.০৮.২০২৬, TK-নির্দেশ ছাপা কাগজসহ — BACHU SARKAR):
       *"সমস্ত তথ্য থাকা সত্ত্বেও প্রেসক্রিপশন প্রিন্ট আউট হলে symptom · duration ·
         chief complaint ফাঁকা আসছে কেন?"*

       **আসল কারণ (কোড ধরে, আন্দাজ নয়):** উপরের ঘরগুলো **শুধু Doctor Check-up
       রেকর্ড** থেকে ভরা হত (`medical` টেবিল, `type=Doctor Checkup`)। এই রোগীর
       এখনো Check-up হয়নি (History-তে শুধু Registration · Advance · Prescription),
       তাই তিনটেই ফাঁকা যেত — অথচ **তথ্য তিনটেই রেজিস্ট্রেশনেই লেখা আছে**।

       **প্রমাণ:** `PatientModel.buildPatientRow` (native/PatientModel.kt:138-139,
       186) রোগীর সারিতে লেখে —
           complaint  = "<যে যে উপসর্গে টিক পড়েছে> | <স্টাফের টাইপ করা কথা>"
           sinceWhen  = "কতদিন ধরে"
       ওয়েবেও ঠিক এই তথ্যই ছাপায় (app.js: `since: p.sinceWhen…`,
       `complaint: n.complaint||p.complaint`) — অর্থাৎ ফোনটাই পিছিয়ে ছিল।

       ⛔ Check-up থাকলে তার মানই আগে — রেজিস্ট্রেশনের তথ্য **শুধু ফাঁকা ঘরেই** বসে।
       ⛔ কোনো ডেটা বদলানো/মোছা হয় না, একটাও নতুন কলাম লাগে না, SQL লাগে না।
       ⛔ একটাই ছোট পড়া (`patients` থেকে দুটো কলাম), আর সেটাও **শুধু তখনই** যখন
          অন্তত একটা ঘর ফাঁকা — তাই Supabase-এ বাড়তি চাপ প্রায় নেই।
       ════════════════════════════════════════ */
    private fun fillBlanksFromRegistration(context: Context) {
        val patientId = RoleSession.currentPatientId.trim()
        if (patientId.isBlank()) return
        val prefs = p(context)
        fun blank(field: String) = prefs.getString(key(field), "").orEmpty().isBlank()
        val needSymptoms = blank("symptoms")
        val needSince = blank("since")
        val needComplaint = blank("complaint")
        if (!needSymptoms && !needSince && !needComplaint) return
        val encodedId = URLEncoder.encode(patientId, "UTF-8")
        /* ➕🔒 V993 (০২.০৯.২০২৬, TK-এর কাগজসহ — SERINA BIBI):
           *"টিক দেওয়া সত্ত্বেও CHIEF COMPLAINT · DURATION · SYMPTOMS ফাঁকা ছাপা হলো কেন?"*

           **আসল কারণ (কোড ধরে, আন্দাজ নয়):** CHECK-UP Queue থেকে রোগী খুললে
           `DoctorQueueActivity` (লাইন 571) `EXTRA_PATIENT_ID`-তে **মানুষ-পড়া-যায়
           কোডটা** (যেমন "KNE-31082026-001") পাঠায়, raw আইডি ("pat_...") নয়।
           তাই এখানকার `patients?id=eq.<কোড>` খোঁজায় একটাও সারি পাওয়া যেত না,
           আর তিনটে ঘরই ফাঁকা ছাপা হত — যদিও রেজিস্ট্রেশনে লেখা আছে।

           **সমাধান:** দুরকম আইডিতেই খোঁজা হয় — ঠিক যেমন `DoctorCheckupActivity.
           markDoctorComplete()` আগে থেকেই করে। প্রথম খোঁজাতে পেলে দ্বিতীয়টা
           চলেই না, তাই Supabase-এ বাড়তি চাপ নেই।
           ⛔ কোনো তথ্য লেখা/মোছা হয় না, নতুন কলাম লাগে না, SQL লাগে না। */
        var rows = SupabaseClient.fetchListOrNull(
            table = "patients",
            filter = "id=eq.$encodedId",
            limit = 1,
            select = "complaint,sinceWhen"
        ) ?: return
        if (rows.length() == 0) {
            rows = SupabaseClient.fetchListOrNull(
                table = "patients",
                filter = "patientId=eq.$encodedId",
                limit = 1,
                select = "complaint,sinceWhen"
            ) ?: return
        }
        if (rows.length() == 0) return
        val row = rows.optJSONObject(0) ?: return
        val complaintRaw = row.optString("complaint").orEmpty().trim()
        val sinceRaw = row.optString("sinceWhen").orEmpty().trim()
        // রেজিস্ট্রেশন লেখে "<উপসর্গের তালিকা> | <টাইপ করা কথা>" — সেই একই ভাগ।
        val cut = complaintRaw.indexOf(" | ")
        val symptomsPart = if (cut >= 0) complaintRaw.substring(0, cut).trim() else complaintRaw
        val notePart = if (cut >= 0) complaintRaw.substring(cut + 3).trim() else ""
        val editor = prefs.edit()
        if (needSymptoms && symptomsPart.isNotBlank()) editor.putString(key("symptoms"), symptomsPart)
        if (needSince && sinceRaw.isNotBlank()) editor.putString(key("since"), sinceRaw)
        if (needComplaint && notePart.isNotBlank()) editor.putString(key("complaint"), notePart)
        editor.apply()
    }

    private fun refreshFromCheckupRecord(context: Context) {
        val patientId = RoleSession.currentPatientId.trim()
        if (patientId.isBlank()) return
        val encodedId = URLEncoder.encode(patientId, "UTF-8")
        val rows = SupabaseClient.fetchListOrNull(
            table = "medical",
            filter = "patientId=eq.$encodedId&type=eq.Doctor%20Checkup",
            limit = 1,
            order = "createdAt.desc.nullslast",
            select = "details"
        ) ?: return
        if (rows.length() == 0) return
        val details = rows.optJSONObject(0)?.optString("details").orEmpty()
        if (details.isBlank()) return
        val values = linkedMapOf<String, String>()
        details.split(';').forEach { part ->
            val i = part.indexOf(':')
            if (i > 0) values[part.substring(0, i).trim()] = part.substring(i + 1).trim()
        }
        p(context).edit()
            .putString(key("symptoms"), values["Visual"].orEmpty())
            .putString(key("since"), values["Duration"].orEmpty())
            .putString(key("complaint"), values["Complaint"].orEmpty())
            .putString(key("previousTreatment"), values["Prev Treatment"].orEmpty())
            .putString(key("previousResult"), values["Prev Result"].orEmpty())
            .putString(key("onset"), values["Onset"].orEmpty())
            .putString(key("treatmentDuration"), values["Treatment Duration"].orEmpty())
            .apply()
    }

    /**
     * 🖨️🔒 V833 (২৯.০৮.২০২৬, TK-নির্দেশ: *"প্রেসক্রিপশনে diseases · symptom ·
     * duration · chief complaint আছে, কিন্তু মেডিসিন স্লিপে নেই কেন?"*)
     *
     * Medicine Slip-এর জন্য **ঠিক ওই চারটে ঘরই সবসময়** — ডাক্তার
     * Prescription-এ কোন ঘরগুলো টিক দিয়েছেন তার উপর নির্ভর করে না।
     * ⛔ তাই দুটো কাগজ একে অপরকে টানে না; Prescription-এর `printLines()`
     *    এক অক্ষরও বদলায়নি।
     * ⛔ তথ্য না থাকলে ঘরটা ফাঁকাই যায় — V425-এর নিয়ম (শিরোনাম থাকে,
     *    নিচে হাতে লেখার জায়গা)।
     */
    fun printLinesForSlip(context: Context): List<String> {
        val prefs = p(context)
        fun value(field: String): String = when (field) {
            "disease" -> RoleSession.currentPatientDisease.trim()
            else -> prefs.getString(key(field), "").orEmpty().trim()
        }
        /* 🖨️🔒 V955 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK-এর ঠিক করা ক্রম:
           Chief Complaint → Duration → Symptoms → Provisional Diagnosis
           ("DISEASE" নামটা বদলে "PROVISIONAL DIAGNOSIS")।
           ⛔ কোন ঘর থেকে কোন তথ্য আসে — একটুও বদলায়নি, শুধু ক্রম ও নাম। */
        return listOf(
            "CHIEF COMPLAINT" to "complaint",
            "DURATION" to "since",
            "SYMPTOMS" to "symptoms",
            "PROVISIONAL DIAGNOSIS" to "disease"
        ).map { (label, field) -> "$label\n" + value(field) }
    }

    fun printLines(context: Context): List<String> {
        val prefs = p(context)
        val selected = selectedFields(context)
        fun value(field: String): String = when (field) {
            "disease" -> RoleSession.currentPatientDisease.trim()
            else -> prefs.getString(key(field), "").orEmpty().trim()
        }
        /* 🖨️🔒 V955 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK-এর ঠিক করা ক্রম ও
           নামকরণ (উপরের `printLinesForSlip`-এর হুবহু একই, যাতে দুই কাগজ এক দেখায়)।
           ⛔ কোন ঘর বাছা আছে সেই নিয়ম (`selected`) এক অক্ষরও বদলায়নি — শুধু
              তালিকার ক্রম বদলেছে, তাই বাছাই-পর্দাতেও কিছু ভাঙে না। */
        val labels = linkedMapOf(
            "complaint" to "CHIEF COMPLAINT", "since" to "DURATION",
            "symptoms" to "SYMPTOMS", "disease" to "PROVISIONAL DIAGNOSIS",
            "previousTreatment" to "PREVIOUS TREATMENT", "previousResult" to "PREVIOUS RESULT",
            "onset" to "ONSET", "treatmentDuration" to "TREATMENT DURATION"
        )
        return labels.mapNotNull { (field, label) ->
            if (field !in selected) return@mapNotNull null
            // 🔴 V425 (TK-নির্দেশ ১৭.০৮.২০২৬): *"Not Recorded লেখা থাকবে না ·
            //    প্রয়োজনে ফাকা প্রিন্ট হবে · যাতে প্রিন্ট আউট এর পরে লেখাও যায়"*
            //    ⇒ তথ্য না থাকলে ঘরটা ফাঁকাই যায়; শিরোনাম (SYMPTOMS ইত্যাদি)
            //    আগের মতোই থাকে, নিচে হাতে লেখার জায়গা ছাড়া হয় (rx_print.html)।
            val v = value(field)
            "$label\n$v"
        }
    }
}
