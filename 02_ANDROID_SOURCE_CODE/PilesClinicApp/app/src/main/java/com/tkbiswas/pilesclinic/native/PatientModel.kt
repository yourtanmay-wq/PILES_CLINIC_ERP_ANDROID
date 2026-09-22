package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RegistrationDraft(
    val date: String,
    val name: String,
    val mobileDigitsOnly: String,
    // 🔒 V235 (TK verified 01.08.2026): Primary = mobileDigitsOnly। নতুন
    // altMobileDigitsOnly = Alternate/Enquiry নম্বর (default ফাঁকা, তাই পুরনো
    // কোনো caller ভাঙে না)। Enquiry থেকে Registration খুললে পুরনো Enquiry
    // নম্বর এখানে বসে; একই হলে ফাঁকা রাখা হয় (duplicate নয়)।
    val altMobileDigitsOnly: String = "",
    val branch: String,
    val age: String,
    val sex: String,
    val village: String,
    val po: String,
    val ps: String,
    val district: String,
    val pin: String,
    val occupation: String,
    val refBy: String,
    val diseases: List<String>,
    val symptoms: List<String>,
    val complaintNote: String,
    val medicalHistory: List<String>,
    // 🔴🆕🔒 খাতার সারি B452 (TK-নির্দেশ, 05.08.2026) — Registration-এর
    // সময় স্টাফের লেখা "কতদিন থেকে সমস্যা"/"আগের চিকিৎসা"। ⛔ ডিফল্ট
    // ফাঁকা — পুরনো কোনো caller ভাঙে না।
    val durationNote: String = "",
    val prevTreatmentNote: String = "",
    val regFee: Double,
    val payMode: String,
    val photo: String = "",
    val refDoctor: String = "",
    val refDoctorMobile: String = "",
    // TK-REQUESTED ADDITION (2026-07-24): same concept as Enquiry's
    // timeType ("Official Time"/"Unexpected Time") -- default preserves
    // every existing caller of this data class unchanged.
    val timeType: String = "Official Time"
)

object PatientModel {

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun normalizedMobile(digitsOnly: String): String = "+91$digitsOnly"

    /** এক মোবাইল = এক সারি। রোগীর সারির স্থায়ী আইডি, মোবাইল থেকেই তৈরি।
     *  নম্বরটা কোনো কারণে ১০ সংখ্যার না হলে আগের মতোই এলোমেলো আইডি হবে,
     *  যাতে কোনো সেভ কখনো আটকে না যায়। */
    fun stableRowId(mobileDigits: String): String {
        val d = mobileDigits.filter { it.isDigit() }.takeLast(10)
        return if (d.length == 10) "pat_$d"
        else "pat_" + UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * 🔵🔒 V516 (২২.০৮.২০২৬, TK-অনুমোদিত) — **এক মোবাইলে একাধিক রোগী।**
     *
     * TK-এর কথা: এক পরিবারে স্বামী ও স্ত্রী — দুজনেই রোগী, কিন্তু যোগাযোগের
     * মোবাইল একটাই। দুজনকে **সম্পূর্ণ আলাদা দুই রোগী** হিসেবে রাখতে হবে।
     *
     * **সমস্যা যেটা ছিল (কোডে প্রমাণিত):** উপরের `stableRowId()` সারির আইডি
     * **মোবাইল থেকেই** বানায় (`pat_<১০ সংখ্যা>`)। তাই একই নম্বরে দ্বিতীয়
     * রোগী সেভ করলে আইডি হুবহু এক হত, আর "একই আইডি = মিশিয়ে দাও" নিয়মে
     * **প্রথম রোগী চাপা পড়ে যেত**। শুধু পপ-আপে বোতাম বসালে এটা ঠিক হত না।
     *
     * **এখন:** স্টাফ যখন **নিজে বেছে** বলেন *"Different Patient — Same Mobile"*,
     * তখনই কেবল এই ফাংশন ডাকা হয় ও একটা **নতুন, অনন্য** আইডি তৈরি হয়।
     *
     * ⛔ **উপরের `stableRowId()` এক অক্ষরও বদলায়নি** — রোজকার রেজিস্ট্রেশন,
     *    পুরোনো সব রোগী ও খাতার সারি **B30**-এর সুরক্ষা ("একই রোগীর নামে দুটো
     *    আইডি নয়") হুবহু আগের মতোই। এটা শুধু **অতিরিক্ত** একটা পথ।
     * ⛔ **পুরোনো কোনো সারি ছোঁয়া হয় না** — কোনো migration লাগে না।
     * ⛔ আইডি এখনো `pat_` দিয়েই শুরু ও মোবাইলটা ভিতরেই থাকে, তাই যে কোড
     *    `startsWith("pat_")` দেখে (যেমন ওয়েবের `createPatientFromVisit`)
     *    সেটা আগের মতোই চলে। কেউ আইডি ভেঙে মোবাইল বের করে না — পুরো
     *    প্রজেক্ট খুঁজে যাচাই করা হয়েছে।
     * ⛔ আইডিটা **একবারই** তৈরি হয় (স্টাফ বোতাম চাপার মুহূর্তে) এবং সেভের
     *    সারিতে বসে যায়; retry queue ওই সারিটাই আবার পাঠায়। তাই নেট খারাপ
     *    হয়ে বারবার চেষ্টা হলেও **দুটো সারি তৈরি হয় না**।
     */
    fun newRowIdForSameMobile(mobileDigits: String): String {
        val d = mobileDigits.filter { it.isDigit() }.takeLast(10)
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return if (d.length == 10) "pat_${d}_$suffix"
        else "pat_" + UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * 🔵🔒 V520 (২২.০৮.২০২৬) — **"স্টাফ নিজে আলাদা রোগী বলেছেন" কি না, তা চেনার
     * একটাই সাধারণ নিয়ম।**
     *
     * উপরের `newRowIdForSameMobile()` **একমাত্র** জায়গা যেখানে
     * `pat_<১০ সংখ্যা>_<লেজ>` আকারের আইডি তৈরি হয় — আর সেটা তৈরি হয় কেবল
     * স্টাফ *"Different Patient — Same Mobile"* বোতাম চাপলে। তাই এই আকারটাই
     * নির্ভরযোগ্য চিহ্ন: **ইচ্ছে করে আলাদা করা রোগী** বনাম **ভুলে দুবার
     * রেজিস্ট্রেশন**।
     *
     * ⛔ পুরোনো সব সারির আইডি `pat_<১০ সংখ্যা>` (লেজ নেই) — তাই এই ফাংশন
     *    তাদের জন্য সবসময় `false` ফেরায়, অর্থাৎ **পুরোনো আচরণ অবিকল একই**।
     * ⛔ কোনো query, schema বা data ছোঁয়া হয় না — শুধু স্ট্রিং মিলিয়ে দেখা।
     *
     * একই নিয়ম আগে `GlobalSearchActivity` (V517) ও `FollowUpRepository`
     * (V518)-এ আলাদা করে লেখা ছিল; সেগুলো হুবহু এই নিয়মই মানে।
     */
    fun isDeclaredSeparateRowId(rowId: String, mobileDigits: String): Boolean {
        val d = mobileDigits.filter { it.isDigit() }.takeLast(10)
        if (d.length != 10) return false
        val prefix = "pat_${d}_"
        return rowId.startsWith(prefix) && rowId.length > prefix.length
    }

    /** Matches app.js's address-building logic exactly: only non-empty parts,
     * "Label: value" joined by ", ". */
    fun buildAddress(draft: RegistrationDraft): String {
        val parts = listOf(
            "Vill" to draft.village, "PO" to draft.po, "PS" to draft.ps,
            "Dist" to draft.district, "PIN" to draft.pin
        ).filter { it.second.isNotBlank() }.map { "${it.first}: ${it.second}" }
        return parts.joinToString(", ")
    }

    /** Builds the "patients" row, matching savePatient()'s field-for-field. */
    /* 🔴🔒🔒 V868 (৩০.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — RAJA MANDAL কার্ড):
       *"ওই স্টাফ নিজে ফরম ফিলাপ করেছিল, তখন দেখিয়েছিল তার আইডি —
       কিন্তু ভবিষ্যতে কেন বদলে গেল?"*

       **আসল দোষ (কোড ধরে যাচাই করা, আন্দাজ নয়):** রোগীর সারি **আবার সেভ**
       হলে (Update Existing · টাইপো সংশোধন) এই ফাংশনটা `createdBy`,
       `registeredBy` ও `createdAt`-এ **যে তখন লগইন আছে তার নম্বর ও এখনকার
       সময়** বসিয়ে দিত — আসল রেজিস্ট্রারের নাম ও আসল সময় মুছে যেত।

       **এখন:** সারিটা আগে থেকে থাকলে ওই তিনটে ঘরে **পুরোনো মানই** ফিরে বসে
       (`keep…` তিনটে)। ফাঁকা পাঠালে আগের মতোই এখনকার লগইন/সময় বসে, তাই
       **নতুন রেজিস্ট্রেশন এক অক্ষরও বদলায়নি**।
       ⛔ বাকি প্রতিটা ঘর হুবহু আগের মতোই। */
    fun buildPatientRow(
        draft: RegistrationDraft,
        patientId: String,
        createdByMobile: String,
        existingRowId: String = "",
        keepCreatedBy: String = "",
        keepRegisteredBy: String = "",
        keepCreatedAt: String = "",
        isExistingRow: Boolean = false
    ): JSONObject {
        val now = isoNow()
        val diagnosis = draft.diseases.joinToString(", ")
        val symptomsJoined = draft.symptoms.joinToString(", ")
        val complaint = listOf(symptomsJoined, draft.complaintNote).filter { it.isNotBlank() }.joinToString(" | ")
        return JSONObject()
            // 🔒 TK'S ORDER (28.07.2026, খাতার সারি B30): *"একই পেশেন্টের নামে দুটো
            // আইডি যেন চালু না হয় — সেটার ব্যবস্থা করুন।"*
            //
            // আগে নতুন রোগীর সারির আইডি ছিল **এলোমেলো** (`pat_<random>`)। তাই দুই
            // স্টাফ একই নম্বর একই সময়ে ভরলে — বা লাইন খারাপ থাকায় খোঁজাটা ফসকে
            // গেলে — **দুটো আলাদা সারি** তৈরি হয়ে যেত।
            //
            // এখন আইডিটা **মোবাইল নম্বর থেকেই তৈরি হয়** (`pat_<শেষ ১০ সংখ্যা>`)।
            // ক্লাউডে লেখা হয় "একই আইডি হলে মিশিয়ে দাও" নিয়মে — তাই একই নম্বরে
            // যতবারই সারি তৈরির চেষ্টা হোক, **সব একটাই সারিতে গিয়ে মেশে**।
            // দুটো সারি তৈরি হওয়া আর সম্ভব নয়।
            //
            // ⛔ পুরনো সারিগুলো ছোঁয়া হয়নি — "Update Existing"-এ আগের মতোই
            // existingRowId ব্যবহার হয়, তাই কোনো পুরনো রেকর্ড নড়বে না।
            // 🔒 এটা TK-এর নিজের নিয়মেরই সঙ্গে মেলে: "এক মোবাইল একবারই রেজিস্টার
            // হবে; নম্বর না থাকলে স্টাফ ডেমি নম্বর ব্যবহার করবে।"
            // (একই কৌশল আগে থেকেই চালু আছে — "আসার কথা" সারির আইডি `exp_<১০ সংখ্যা>`।)
            .put("id", existingRowId.ifBlank { stableRowId(draft.mobileDigitsOnly) })
            .put("patientId", patientId)
            .put("date", draft.date)
            .put("registrationDate", draft.date)
            .put("visitDate", draft.date)
            .put("name", draft.name)
            .put("mobile", normalizedMobile(draft.mobileDigitsOnly))
            // 🔒 V235 (TK, Primary/Alternate Mobile · safe-fallback): Alternate/Enquiry নম্বর।
            // ⚠️ SQL চালানোর আগেও যেন HTTP 400 না হয়: `altMobile` **শুধু তখনই** JSON-এ যায়
            // যখন সত্যিকারের একটা আলাদা নম্বর আছে (ফাঁকা হলে key-টাই পাঠানো হয় না)। তাই
            // সাধারণ registration-এ (alt নম্বর নেই) column না থাকলেও কোনো 400/queue-আটকা হয় না;
            // এবং merge-duplicates upsert-এ ফাঁকা "" পাঠিয়ে পুরনো altMobile মুছে যাওয়ার
            // ঝুঁকিও নেই। Primary-র সমান হলে বসে না (dedup — RegistrationActivity-তেও করা)।
            .apply {
                val alt = if (draft.altMobileDigitsOnly.isNotBlank()) normalizedMobile(draft.altMobileDigitsOnly) else ""
                if (alt.isNotBlank() && alt != normalizedMobile(draft.mobileDigitsOnly)) put("altMobile", alt)
            }
            .put("branch", draft.branch)
            .put("age", draft.age)
            .put("sex", draft.sex)
            .put("address", buildAddress(draft))
            .put("occupation", draft.occupation)
            .put("refBy", draft.refBy)
            .put("refDoctor", draft.refDoctor)
            .put("refDoctorMobile", draft.refDoctorMobile)
            .put("disease", diagnosis.ifBlank { "Piles" })
            .put("diagnosis", diagnosis)
            .put("sinceWhen", draft.durationNote)
            .put("complaint", complaint)
            .put("medicalHistory", draft.medicalHistory.joinToString(", "))
            // 🔴🆕🔒 B452 — DoctorCheckupActivity.kt-এর "History & Previous"
            // ধাপ `first("previousTreatment", "prevTreatment",
            // "medicalHistory")` দিয়ে প্রথমে এই কলামটাই খোঁজে, তাই এখানে
            // বসালেই ডাক্তার Checkup খোলার সময় স্বয়ংক্রিয়ভাবে প্রি-ফিল
            // পাবেন। ⛔ উপরের `medicalHistory` (চেকবক্স-তালিকা, ভিন্ন
            // জিনিস) ছোঁয়া হয়নি, শুধু নতুন এই একটা কলাম।
            .put("previousTreatment", draft.prevTreatmentNote)
            .put("photo", draft.photo)
            .put("createdBy", keepCreatedBy.ifBlank { createdByMobile })
            .put("registeredBy", keepRegisteredBy.ifBlank { createdByMobile })
            // TK-REQUESTED ADDITION (2026-07-24): same Official/Unexpected
            // Time concept Enquiry already has -- threaded through
            // patientRow the same way disease/address/age/sex already are,
            // so buildVisitFollowUpRow below can read it back.
            .put("timeType", draft.timeType)
            .put("createdAt", keepCreatedAt.ifBlank { now })
            .put("updatedAt", now)
            .also { row ->
                /* 🔴🔒🔒 V872 (৩০.০৮.২০২৬, TK-অনুমোদিত — *"লাইনে ফেরা বন্ধ
                   করে দিন"*): এই চারটে ঘর আগে **সব সময়** পাঠানো হতো। তাই
                   পুরোনো রোগীর তথ্য দ্বিতীয়বার সেভ করলেই (নামের বানান ঠিক
                   করা · Update Existing) —
                     · চেকআপ হয়ে যাওয়া রোগী আবার **ডাক্তারের লাইনে** ফিরত
                       (`stage`/`queue`/`doctorComplete`),
                     · আর তাঁর **বিলটা ০ হয়ে যেত** (`bill`)।
                   **এখন:** চারটেই শুধু **নতুন রোগীর সারিতে** বসে। পুরোনো সারিতে
                   ঘরগুলো পাঠানোই হয় না ⇒ ক্লাউডে ও ফোনে দুটোতেই অপরিবর্তিত
                   থাকে। ⛔ নতুন রেজিস্ট্রেশনে আচরণ এক অক্ষরও বদলায়নি
                   ("Different Patient"-ও নতুনই, তাই সেখানেও আগের মতোই)। */
                if (!isExistingRow) {
                    row.put("stage", "Doctor Queue")
                       .put("queue", true)
                       .put("doctorComplete", false)
                       /* 📅 V1013 — আজ তালিকায় উঠলেন, সেই দিনটাই এখানে বসে
                          (CHECK-UP Queue এখন এই ঘরটাই দেখে, `updatedAt` নয়)। */
                       .put("queuedAt", draft.date.ifBlank {
                           java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                       })
                       .put("bill", 0)
                }
            }
    }

    /** Builds the matching "followups" row (stage=Patient), matching
     * canonicalVisitFollowRow() exactly, so the patient shows up in the
     * Visit tab immediately, same as a WebView registration does. */
    /**
     * 🔴🔒 V399 (16.08.2026, TK-রিপোর্ট ছবিসহ: "অনেক জিনিস ২ বার ৩ বার হয়ে যাচ্ছে")।
     *
     * **আসল কারণ (কোড পড়ে নিশ্চিত):** এখানে প্রতিবার `"fu_" + UUID.randomUUID()`
     * বসত — অর্থাৎ একই রোগীকে দ্বিতীয়বার সেভ করলে (বা "Update Existing" চাপলে)
     * ক্লাউডে **নতুন একটা Follow-up সারি ঢুকে যেত**, পুরোনোটা আপডেট হত না।
     * প্রতিটা নতুন সারির নিজস্ব `history[0] = "Registered patient / Visit created"`
     * থাকায় রোগীর টাইমলাইনে ওই লেখাটা ২-৩ বার (আলাদা তারিখে) দেখা যেত।
     *
     * **অসমতাই প্রমাণ:** একই ফাংশনে রোগীর সারির আইডি স্থায়ী (`stableRowId`,
     * লাইন ৬১-৬৫) আর Visit Fee-তে রক্ষা আছে (`RegistrationRepository.kt:140`,
     * TK নিজে ২৫.০৭.২০২৬-এ ধরিয়েছিলেন) — শুধু Follow-up সারিটাই বাদ পড়েছিল।
     *
     * **সমাধান:** পুরোনো Follow-up সারি থাকলে তার **নিজের আইডিই** ব্যবহার হয়,
     * তাই নতুন সারি তৈরি হয় না — আপডেট হয়।
     * ⛔ **সবচেয়ে জরুরি সুরক্ষা:** পুরোনো সারি ব্যবহার করার সময় `history` ও
     *    `lastRemark` **পাঠানোই হয় না** — নইলে upsert পুরোনো সব কল-ইতিহাস
     *    মুছে দিত। ঘর না পাঠালে ওই ঘর অপরিবর্তিত থাকে (স্থানীয় স্টোরও
     *    `LocalWorkflowStore.upsertFollowUp` ঘর-ধরে-ঘর মেশায়, লাইন ৭২-৭৩)।
     * ⛔ পুরোনো সারি না পেলে (নতুন রোগী · নেট নেই) আগের হুবহু আচরণ — নতুন আইডি
     *    ও "Registered patient / Visit created" লেখা।
     */
    /* 🔴🔒 V868 — উপরের একই দোষ এখানেও ছিল: Follow-up (Visit) সারি
       আবার লেখা হলে `createdBy`/`createdAt`-এ নতুন লগইনের নাম ও সময় বসত।
       এখন পুরোনো সারি হলে পুরোনো মানই থাকে। */
    fun buildVisitFollowUpRow(
        patientRow: JSONObject,
        staffMobile: String,
        existingFollowUpRowId: String = "",
        keepCreatedBy: String = "",
        keepCreatedAt: String = ""
    ): JSONObject {
        val now = isoNow()
        val visitDate = patientRow.getString("visitDate")
        val reuse = existingFollowUpRowId.isNotBlank()
        val history = JSONArray().put(
            JSONObject()
                .put("date", patientRow.getString("date"))
                .put("time", isoNow())   /* ⏰ V827 — সময়ও জমা হয় (TK: "LAST CALL তারিখের পরে যেন Time থাকে")। */
                .put("remark", "Registered patient / Visit created")
                .put("staff", staffMobile)
        )
        val out = JSONObject()
            /* 🔗🔒 V1005 (০৩.০৯.২০২৬) — নতুন সারির id এখন **নির্দিষ্ট**
               (`fu_pat_<patient row id>`) — কম্পিউটারের self-heal ঠিক এই
               নিয়মেই বানায় (`app.js`, B626)। আগে এলোমেলো id বসত, তাই দুই
               যন্ত্র দুটো আলাদা সারি বানিয়ে ফেলতে পারত।
               ⛔ পুরনো সারি থাকলে আগের মতোই তার নিজের id-ই থাকে। */
            .put("id", if (reuse) existingFollowUpRowId else "fu_pat_" + patientRow.getString("id"))
            .put("refId", patientRow.getString("id"))
            .put("patientId", patientRow.getString("patientId"))
            .put("mobile", patientRow.getString("mobile"))
            .put("name", patientRow.getString("name"))
            .put("branch", patientRow.getString("branch"))
            .put("disease", patientRow.getString("disease"))
            .put("address", patientRow.getString("address"))
            .put("age", patientRow.s("age"))
            .put("sex", patientRow.s("sex"))
            .put("registrationDate", patientRow.getString("registrationDate"))
            .put("visitDate", visitDate)
            // TK-REQUESTED ADDITION (2026-07-24): same Official/Unexpected
            // Time badge concept the Enquiry-created followups row already
            // carries -- read back from patientRow (set in buildPatientRow
            // above). optString default keeps this safe even for any
            // existing/older patientRow that doesn't have it.
            .put("timeType", patientRow.s("timeType").ifBlank { "Official Time" })
            .put("createdBy", keepCreatedBy.ifBlank { staffMobile })
            .put("createdAt", keepCreatedAt.ifBlank { now })
            .put("updatedAt", now)
        if (!reuse) {
            /* 🔴🔒🔒 V871 (৩০.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — SHAMOL ROY):
               *"ইনি একজন পেশেন্ট, আগে দুই-একবার পেমেন্ট করেছে, তারপরও এখানে
               কেন তাকে ভিজিট কাটে দেখাচ্ছে"*

               **আসল দোষ (কোড ধরে যাচাই করা):** নিচের চারটে ঘর আগে **সব
               সময়** পাঠানো হতো — পুরোনো সারিতেও। তাই রোগীর তথ্য দ্বিতীয়বার
               সেভ হলেই —
                 · `stage` "Treatment" থেকে জোর করে "Patient" হয়ে যেত
                   ⇒ কার্ডে PATIENT-এর বদলে **VISITED** দেখাত, বিল/বকেয়া
                   উধাও, আর ADVANCE বোতাম ফিরে আসত,
                 · `nextFollow` (পরের কলের তারিখ) **মুছে** যেত,
                 · `status` জোর করে "Active" — বন্ধ করা সারিও ফিরে আসত,
                 · `date` আজকের তারিখে লাফিয়ে যেত।

               **এখন:** চারটেই শুধু **নতুন সারিতে** বসে। পুরোনো সারিতে ঘরগুলো
               পাঠানোই হয় না ⇒ ক্লাউডে ও স্থানীয় স্টোরে দুটোতেই অপরিবর্তিত
               থাকে (`history`/`lastRemark`-এর ক্ষেত্রে V399-এ প্রমাণিত একই
               নিয়ম)। ⛔ নতুন রেজিস্ট্রেশনে আচরণ এক অক্ষরও বদলায়নি। */
            out.put("stage", "Patient")
            out.put("date", visitDate)
            out.put("nextFollow", "")
            out.put("status", "Active")
            /* নতুন সারি — আগের মতোই ইতিহাস, লেখা ও গণনা বসে। */
            out.put("history", history)
            out.put("lastRemark", "Registered patient / Visit created")
            out.put("callCount", 0)
        }
        return out
    }

    /** Builds the Visit Fee payment row, matching savePatient()'s payment
     * add() call. Only called when regFee > 0 (always true here since the
     * fee is mandatory to reach Save at all). */
    fun buildVisitFeePaymentRow(patientRow: JSONObject, draft: RegistrationDraft, staffMobile: String): JSONObject {
        val now = isoNow()
        /* 🧾🔒 V1284 (১০.০৯.২০২৬, TK-অনুমোদিত প্ল্যান ② — তালিকা সারি ৪১০):
           TK: *"একই রকমের পেমেন্ট একই দিনে … ভবিষ্যতে যেন না হয়"*।
           🔴 **SQL-এ মাপা:** SANDIP BASAK (KNE-30012026-001) — রেজিস্ট্রেশনের ₹400
              ভিজিট-ফি **তিনবার, একই মিলিসেকেন্ডে** — কারণ এই সারিটা প্রতিবার
              **নতুন এলোমেলো আইডি** নিয়ে তৈরি হত (তিনটে আলাদা পথ থেকে), তাই
              যতবার লেখা, ততটা সারি।
           ⇒ এখন আইডি **রোগী + তারিখ ধরে স্থির** (`pay_vf_<রোগী-আইডি>_<তারিখ>`):
             একই রেজিস্ট্রেশনের ফি যতবারই লেখা হোক, ক্লাউডে (merge-duplicates)
             ও ফোনে **একটাই** সারি — জোড়া হওয়ার পথই থাকল না। কম্পিউটারে হুবহু
             একই ছাঁচ (`app.js` রেজিস্ট্রেশন)।
           ⛔ ঘরগুলো · অঙ্ক · তারিখ · কে নিল — এক অক্ষরও বদলায়নি; পুরনো সারি অছোঁয়া।
           ⛔ Payment পর্দা থেকে হাতে নেওয়া ভিজিট-ফি (পরের ভিজিটে) আগের মতোই
              নিজের আইডিতে — এখানে শুধু রেজিস্ট্রেশনের স্বয়ংক্রিয় সারিটা। */
        val vfDate = draft.date.trim().ifBlank { now.take(10) }
        return JSONObject()
            .put("id", "pay_vf_" + patientRow.getString("id") + "_" + vfDate)
            .put("payType", "visit_fee")
            .put("payLabel", "Visit Fee")
            .put("paymentLabel", "Visit Fee")
            .put("patientId", patientRow.getString("id"))
            // 🆔 TK-এর নিয়ম (28.07.2026): মানুষের পড়ার Patient ID-ও সঙ্গে থাকে,
            // যাতে টাকার তালিকায় নাম-মোবাইলের পাশে ID দেখানো যায়।
            // ⛔ বাড়তি কোনো ক্লাউড-কল নয় — একই সারিতে একটা ঘর বেশি।
            .put("patientCode", patientRow.s("patientId"))
            .put("mobile", patientRow.getString("mobile"))
            .put("branch", patientRow.getString("branch"))
            .put("name", patientRow.getString("name"))
            .put("date", draft.date)
            .put("amount", draft.regFee)
            .put("mode", draft.payMode)
            .put("remarks", "Visit Fee")
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
    }
}
