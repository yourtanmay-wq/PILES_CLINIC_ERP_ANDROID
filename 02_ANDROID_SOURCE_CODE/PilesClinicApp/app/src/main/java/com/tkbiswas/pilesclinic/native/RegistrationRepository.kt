package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles native Registration save: generates the correct Patient ID,
 * checks for an existing Patient with the same mobile (matching
 * savePatient()'s duplicate rule -- an existing Enquiry is NOT a block, only
 * an existing Patient is), then saves the patient + its Visit-stage
 * follow-up + Visit Fee payment, with the same offline pending-queue
 * fallback pattern as EnquiryRepository.
 *
 * SCOPED LIMITATION (by design, for this step): when a patient with this
 * mobile already exists, the WebView asks whether to update that existing
 * record in place ("same patient re-registering"). This native version
 * currently always creates a new record and asks the staff to confirm --
 * merging into the existing record will be added once this becomes a real
 * reported need, to keep this step's scope reviewable.
 */
class RegistrationRepository(private val context: Context) {

    companion object {
        // TK-REQUESTED FIX (2026-07-19): same reasoning as LocalWorkflowStore's
        // new companion LOCK -- a fresh RegistrationRepository(context) is
        // created almost everywhere this is used, so without a shared lock,
        // BottomNav's background flushPending() and a live save() happening
        // at the same moment could race on this class's own pending/close
        // queues and silently drop one of them.
        private val LOCK = Any()
    }

    private val prefs = context.getSharedPreferences("piles_clinic_registration_pending", Context.MODE_PRIVATE)

    /**
     * 🚨 TK'S RULE (28.07.2026, খাতার সারি B30): *"কোন প্রকার রোগীর যেন ডুপ্লিকেট
     * না হয়। সিস্টেমে যদি আগে থেকে থাকে অবশ্যই ওয়ার্নিং দিতে হবে।"*
     * [verified] = যাচাইটা সত্যিই করা গেছে কিনা। লাইন খারাপ থাকলে ক্লাউডে দেখাই
     * যায় না — তখন `found = false` মানে **"নতুন"** নয়, মানে **"জানা যায়নি"**।
     * ওই অবস্থায় স্টাফকে ওয়ার্নিং দেখাতে হবে, চুপচাপ নতুন রোগী বানানো যাবে না।
     */
    /**
     * 🔵🔒 V516 (২২.০৮.২০২৬, TK-অনুমোদিত): একই মোবাইলে একাধিক রোগী থাকতে পারে,
     * তাই এখন **সবগুলো** মিল ফেরে (`matches`), শুধু প্রথমটা নয়।
     * ⛔ পুরোনো ঘরগুলো (`name`/`branch`/`patientId`/`rowId`) **হুবহু আগের মতোই**
     *    প্রথম মিলটাই ধরে রাখে — তাই এই ক্লাস ব্যবহার করা পুরোনো কোনো কোড
     *    এক অক্ষরও বদলাতে হয়নি।
     */
    data class Match(
        val rowId: String, val name: String, val branch: String, val patientId: String
    )

    data class DuplicatePatient(
        val found: Boolean, val name: String, val branch: String,
        val patientId: String, val rowId: String = "", val verified: Boolean = true,
        val matches: List<Match> = emptyList()
    )

    /* 🔴🔒 V1269 (০৯.০৯.২০২৬ — TK-রিপোর্ট ছবিসহ ও তাঁর নিজের SQL-এ প্রমাণিত:
       *"yearly registration এ কিছু কিছু নাম ডুপ্লিকেট কেন এসেছে"*)।

       🔬 **আসল ফাঁক (TK-র SQL-ফলে মেপে, আন্দাজ নয়):** ডুপ্লিকেট-পাহারা এতদিন
          **শুধু মোবাইল নম্বর** ধরে দেখত। কিন্তু ফলে পাওয়া গেল তিন জোড়া সারি
          যেখানে **একই দিনে একই নাম**, অথচ নম্বরে **ঠিক এক ঘর টাইপ-ভুল** —
          SAGAR KUMAR SHING (748204**69**66 ↔ 748204**64**66) ·
          DHARMENDRA SAH (896**7**577917 ↔ 896**9**577917) ·
          SAHA ALAM (993264**82**77 ↔ 993264**85**77)।
          নম্বর আলাদা বলে পাহারা এগুলো **দেখতেই পায়নি**, তাই দ্বিতীয় রেকর্ড
          তৈরি হয়ে গেছে।

       ⇒ এখন নম্বরে মিল না পেলে **নাম + ব্রাঞ্চ** ধরেও একবার দেখা হয়।
       ⛔ **কিছুই আটকানো হয় না** — শুধু স্টাফকে দেখানো হয় "এই নামে আগে
          রেজিস্ট্রেশন আছে"; তিনি চাইলে আগের মতোই এগোতে পারবেন (নামের মিল
          সবসময় ডুপ্লিকেট নয় — TK-র ওই একই SQL-এ ১৩ জোড়া আলাদা মানুষ ছিল)।
       ⛔ পড়াটা **খুব ছোট** ও শুধু তখনই হয় যখন নম্বরে কোনো মিল পাওয়া যায়নি —
          ৪টে ঘর, সর্বোচ্চ ২০ সারি ⇒ Egress-এ প্রভাব নগণ্য (নিয়ম ১৩)।
       ⛔ নেট খারাপ হলে ফাঁকা ফেরে ⇒ আগের মতোই কিছুই দেখানো হয় না, সেভ আটকায় না। */
    fun checkSameNamePatient(name: String, branch: String): DuplicatePatient {
        val n = name.trim()
        val b = branch.trim()
        if (n.length < 3 || b.isBlank()) return DuplicatePatient(false, "", "", "")
        val enc = { v: String ->
            try { java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20") } catch (_: Throwable) { v }
        }
        val rows = try {
            SupabaseClient.fetchList(
                "patients", "name=ilike.${enc(n)}&branch=eq.${enc(b)}", 20,
                select = "id,name,branch,patientId,mobile,registrationDate"
            )
        } catch (_: Throwable) { org.json.JSONArray() }
        val all = LinkedHashMap<String, Match>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val rid = row.s("id")
            if (rid.isNotBlank()) all[rid] = Match(rid, row.s("name"), row.s("branch"), row.s("patientId"))
        }
        if (all.isEmpty()) return DuplicatePatient(false, "", "", "")
        val first = all.values.first()
        return DuplicatePatient(true, first.name, first.branch, first.patientId, first.rowId, matches = all.values.toList())
    }

    fun checkDuplicatePatient(mobileDigitsOnly: String): DuplicatePatient {
        val normalized = PatientModel.normalizedMobile(mobileDigitsOnly)
        // খাতার সারি B30: ব্যর্থ হলে `null` — "নতুন" আর "দেখতেই পারলাম না" আর এক নয়।
        /* 🔵🔒 V516: আগে limit ছিল ১ (শুধু প্রথম মিল)। এখন ২০ — একই নম্বরে
           যদি সত্যিই একাধিক রোগী থাকেন, স্টাফকে **সবাইকে** দেখাতে হবে।
           ⛔ এক নম্বরের কয়েকটা সরু সারি — Egress-এ প্রভাব নগণ্য।
           ⛔ ছাঁকনি · ঘর · ব্যর্থতার নিয়ম (`null` = জানা যায়নি) সব আগের মতোই। */
        val cloud = SupabaseClient.findByMobileOrNull("patients", normalized, "id,name,branch,patientId", 20)
        val rows = cloud ?: org.json.JSONArray()
        val all = LinkedHashMap<String, Match>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val rid = row.s("id")
            if (rid.isNotBlank()) all[rid] = Match(rid, row.s("name"), row.s("branch"), row.s("patientId"))
        }
        // 🔒 V235 (TK verified 01.08.2026): Duplicate check এখন Alternate নম্বরেও।
        // Primary-তে না মিললে দেখা হয় কোনো রোগীর `altMobile` এই নম্বর কিনা।
        // ⛔ সম্পূর্ণ additive — আগের Primary-match একটুও বদলায়নি। altMobile column
        //    এখনো যোগ না হলে query খালি ফেরে (কিছু ভাঙে না)।
        // 🔵 V516 (TK-অনুমোদিত): Alternate নম্বরেও একই নিয়ম — মিলগুলো একই
        //    তালিকায় যোগ হয় (একই সারি দুবার এলে `id` ধরে একবারই থাকে)।
        val altRows = try {
            SupabaseClient.fetchList("patients", "altMobile=eq.$normalized", 20, select = "id,name,branch,patientId")
        } catch (_: Throwable) { org.json.JSONArray() }
        for (i in 0 until altRows.length()) {
            val row = altRows.getJSONObject(i)
            val rid = row.s("id")
            if (rid.isNotBlank()) all.putIfAbsent(rid, Match(rid, row.s("name"), row.s("branch"), row.s("patientId")))
        }
        if (all.isNotEmpty()) {
            val first = all.values.first()
            return DuplicatePatient(
                true, first.name, first.branch, first.patientId, first.rowId,
                matches = all.values.toList()
            )
        }
        // TK'S STANDING RULE (restated 2026-07-27): ONE MOBILE = ONE REGISTRATION.
        // The cloud lookup above returns an EMPTY list on any network failure --
        // by design, so a hiccup never blocks a save -- and an empty list was
        // being read as "this number is new". On a slow/dead line that quietly
        // created a SECOND patient row and a SECOND Patient ID for a number that
        // was already registered. The phone's own saved list needs no network,
        // so ask it before concluding the number is new. Nothing is blocked: if
        // it is not there either, the save goes ahead exactly as before.
        /* 🔵🔒 V520 (২২.০৮.২০২৬, TK-অনুমোদিত — **offline**): আগে ফোনের তালিকা
           থেকে **একটাই** মিল ফিরত। নেট না থাকা অবস্থায় এক নম্বরে স্বামী ও স্ত্রী
           দুজন জমা থাকলে পপ-আপে একজনই দেখা যেত — স্টাফ ভুল জনকে *"Update
           Existing"* করে ফেলতে পারতেন। এখন **সবাই** দেখা যায়, ঠিক যেমন উপরের
           ক্লাউড-পথে হয়।
           ⛔ একজন থাকলে হুবহু আগের মতোই একটাই মিল, একই ঘরে, একই আচরণ।
           ⛔ নতুন কোনো cloud-read নেই — এটা ফোনেরই জমা তালিকা। */
        val locals = LocalWorkflowStore(context).findPatientsByMobile(normalized)
        if (locals.isNotEmpty()) {
            val ms = locals.map { Match(it.s("id"), it.s("name"), it.s("branch"), it.s("patientId")) }
            val first = ms.first()
            return DuplicatePatient(
                true, first.name, first.branch, first.patientId, first.rowId, matches = ms
            )
        }
        // ক্লাউডে দেখাই গেল না, ফোনেও নেই — তাই "নতুন" বলা যাচ্ছে না, শুধু
        // "জানা যায়নি"। ডাকা পর্দা এটা দেখে স্টাফকে ওয়ার্নিং দেবে।
        return DuplicatePatient(false, "", "", "", "", verified = cloud != null)
    }

    /** Returns the generated Patient ID on success (network reachable and
     * all 3 rows saved), or null if it had to be queued for later -- the
     * caller should still treat null as a successful save from the staff's
     * point of view (see RegistrationActivity), just not yet synced. */
    /**
     * 🔵🔒 V516 (২২.০৮.২০২৬, TK-অনুমোদিত) — `forceNewPatientRowId`
     *
     * স্টাফ পপ-আপে **নিজে বেছে** *"Different Patient — Same Mobile"* চাপলে
     * সেখানে তৈরি হওয়া নতুন অনন্য সারি-আইডি এখানে আসে। ফাঁকা হলে (রোজকার
     * সব সেভ) **কিছুই বদলায় না — আচরণ হুবহু আগের মতোই**।
     */
    fun save(draft: RegistrationDraft, staffMobile: String, existingPatientId: String = "", existingRowId: String = "", forceNewPatientRowId: String = ""): String? {
        // 🔴🔴🔴 খাতার সারি B455 (TK-রিপোর্ট, ছবিসহ — একই রোগীর (GOURANGO
        // BARMAN) দুইবার Registration, দুইবার Visit Fee কাটা)। **আসল
        // কারণ যতটা কোড পড়ে বোঝা গেছে:** Visit Fee কাটার সিদ্ধান্ত
        // সম্পূর্ণ নির্ভর করে `existingRowId`-এর উপর, আর `existingRowId`
        // আসে শুধু উপরের স্ক্রিনের মোবাইল-দিয়ে-খোঁজা ডুপ্লিকেট-চেক থেকে
        // (checkDuplicatePatient) — সেই চেক ব্যর্থ হলে (পুরনো রেকর্ডের
        // মোবাইল অন্যভাবে লেখা ছিল, নেট সমস্যা, বা স্টাফ "তবুও সেভ করুন"
        // চাপলে) `existingRowId` ফাঁকাই থেকে যায়, আর দ্বিতীয়বার Visit Fee
        // কাটে। **অতিরিক্ত সুরক্ষা (এখন যোগ করা হলো):** এখানে, লেখার
        // ঠিক আগে, যে id-তে আসল সেভ (upsert) হতে চলেছে (মোবাইল থেকে
        // তৈরি স্থায়ী id) সেই id-তে ইতিমধ্যে কোনো সারি আছে কিনা সরাসরি
        // আরেকবার যাচাই করা হয় — উপরের স্ক্রিনের মোবাইল-টেক্সট-খোঁজার
        // উপর নির্ভর না করে, সরাসরি সেই id ধরে। থাকলে সেটাকেই
        // "existingRowId" ধরে নেওয়া হয় (Visit Fee কাটে না, patientId-ও
        // পুরনোটাই থাকে) — এমনকি প্রথম চেক মিস করলেও। ⛔ এই নতুন চেক
        // ব্যর্থ হলে (নেট সমস্যা) আগের আচরণই চলে — নতুন কোনো ব্লক নেই।
        var effectiveRowId = existingRowId
        var effectivePatientId = existingPatientId
        /* 🔴🔒🔒 V868 (TK-রিপোর্ট, RAJA MANDAL কার্ড) — আসল রেজিস্ট্রারের নাম
           ও আসল সময় ধরে রাখার তিনটে ঘর। সারিটা আগে থেকে থাকলে নিচে এগুলো
           ভরা হয়, আর `buildPatientRow`/`buildVisitFollowUpRow` পুরোনো মানই
           বসায় — দ্বিতীয়বার সেভেও নাম বদলায় না। */
        var keepCreatedBy = ""
        var keepRegisteredBy = ""
        var keepCreatedAt = ""
        /* 🔴🔵🔒 V516 (TK-অনুমোদিত): নিচের B455-পাহারাটা মোবাইল থেকে তৈরি স্থায়ী
           আইডি (`pat_<মোবাইল>`) ধরে খোঁজে — অর্থাৎ **ওই নম্বরের প্রথম রোগীকে**।
           রোজকার সেভে ওটাই ঠিক (একই রোগী দুবার সেভ হলে Visit Fee দুবার কাটে না)।
           কিন্তু স্টাফ যখন স্পষ্ট করে বলেছেন *"ইনি আলাদা একজন রোগী"*, তখন ওই
           পাহারা চললে **দ্বিতীয় রোগী প্রথম রোগীর আপডেট হয়ে যেত** — ঠিক যে
           সমস্যাটা সারাতে বসেছি সেটাই ফিরে আসত।
           ⛔ তাই শুধু ওই স্পষ্ট বাছাইয়ের সময়ই পাহারাটা এড়ানো হয়।
           ⛔ `forceNewPatientRowId` ফাঁকা = রোজকার সেভ ⇒ B455 হুবহু আগের মতোই চলে। */
        if (effectiveRowId.isBlank() && forceNewPatientRowId.isBlank()) {
            try {
                val stableId = PatientModel.stableRowId(draft.mobileDigitsOnly)
                // 🟢 V868 — তিনটে ঘর **এই একই অনুরোধেই** আসে, নতুন কোনো পড়া নয়।
                val existing = SupabaseClient.fetchListOrNull(
                    "patients", "id=eq.$stableId", 1,
                    select = "id,patientId,createdBy,registeredBy,createdAt")
                if (existing != null && existing.length() > 0) {
                    val row = existing.getJSONObject(0)
                    effectiveRowId = row.s("id")
                    effectivePatientId = row.s("patientId").ifBlank { existingPatientId }
                    keepCreatedBy = row.s("createdBy")
                    keepRegisteredBy = row.s("registeredBy")
                    keepCreatedAt = row.s("createdAt")
                }
            } catch (_: Throwable) { }
        }
        /* 🟢 V868 — "Update Existing"-এর পথে উপরের যাচাইটা চলে না (আইডি
           আগে থেকেই জানা)। তাই ঠিক তখনই, শুধু ওই একটা সারির তিনটে ছোট ঘর
           একবার আনা হয়। ⛔ নতুন রেজিস্ট্রেশনে এটা চলে না ⇒ রোজকার কাজে
           Egress এক বাইটও বাড়ে না। ⛔ না আনতে পারলে আগের আচরণই চলে। */
        if (effectiveRowId.isNotBlank() && keepCreatedBy.isBlank() && keepRegisteredBy.isBlank()) {
            try {
                val own = SupabaseClient.fetchListOrNull(
                    "patients", "id=eq.$effectiveRowId", 1,
                    select = "createdBy,registeredBy,createdAt")
                if (own != null && own.length() > 0) {
                    val row = own.getJSONObject(0)
                    keepCreatedBy = row.s("createdBy")
                    keepRegisteredBy = row.s("registeredBy")
                    keepCreatedAt = row.s("createdAt")
                }
            } catch (_: Throwable) { }
        }
        val existingRowIdSafe = effectiveRowId
        val existingPatientIdSafe = effectivePatientId
        /* ═══════════════════════════════════════════════════════════════
           🔴🔒 V1110 (০৫.০৯.২০২৬, TK-রিপোর্ট — KASHAB MANDAL,
           COB-05092026-001 অথচ branch = Jalpaiguri)।

           ─── 🔴 আসল কারণ (TK-এর CSV + কোড, মেপে পাওয়া) ────────────────
           সারিটা আগে থেকে থাকলে এখানে **পুরনো আইডিটাই রেখে দেওয়া হত**
           (`existingPatientIdSafe`), আর **ব্রাঞ্চ বদলেছে কিনা কখনো দেখা হত না**।
           ⇒ প্রথমবার ভুল করে Cooch Behar-এ সেভ হয়ে `COB-…` আইডি বসেছিল;
             পরে ব্রাঞ্চ ঠিক করে (Jalpaiguri) আবার সেভ হয়েছে, কিন্তু আইডিটা
             চিরকালের মতো `COB-` থেকে গেছে।

           ─── এখন কী হয় (TK-এর স্থায়ী নিয়ম) ─────────────────────────────
           TK: *"রেজিস্ট্রেশন কোন ব্রাঞ্চে হলো সেটাই ম্যাটার করে … টাকা পয়সা
           সমস্ত হিসাব সেই ব্রাঞ্চের নামেই হবে"*।
           ⇒ পুরনো আইডির ব্রাঞ্চ-সংকেত যদি **আজ যে ব্রাঞ্চে রেজিস্ট্রেশন হচ্ছে**
             তার সঙ্গে না মেলে, তাহলে ঠিক ব্রাঞ্চের নতুন আইডি বসে।

           ⛔ **শুধু তখনই, যখন আইডির তারিখটাও আজকেরই** — অর্থাৎ ভুলটা আজই
              হয়েছে। পুরনো দিনের আইডি (রোগীর হাতে ছাপা কাগজ আছে) **কখনো**
              বদলানো হয় না, নইলে পুরনো রসিদের সঙ্গে মিলত না।
           ⛔ আইডি বদলালে টাকার সারির `patientCode`-ও একই সঙ্গে ঠিক করা হয়
              (নিচে), তাই দুই জায়গায় দুরকম আইডি থেকে যেতে পারে না।
           ⛔ নতুন আইডি বানাতে না পারলে (নেট নেই) পুরনোটাই থাকে — কিছুই ভাঙে না।
           ═══════════════════════════════════════════════════════════════ */
        val keptId = existingPatientIdSafe
        val wantCode = PatientIdGenerator.branchCode(draft.branch)
        val keptCode = keptId.substringBefore("-", "")
        val keptDate = keptId.split("-").getOrNull(1).orEmpty()
        val todayCode = PatientIdGenerator.dateCode(draft.date)
        val mustRecode = keptId.isNotBlank() && keptCode.isNotBlank() &&
            !keptCode.equals(wantCode, ignoreCase = true) && keptDate == todayCode
        val recodedId = if (mustRecode)
            (try { PatientIdGenerator.generate(draft.branch, draft.date, context) } catch (_: Throwable) { "" })
        else ""
        val patientIdRaw = when {
            recodedId.isNotBlank() -> recodedId
            keptId.isNotBlank()    -> keptId
            else -> PatientIdGenerator.generate(draft.branch, draft.date, context)
        }
        /* ═══════════════════════════════════════════════════════════════
           🛡️🔒 V1111 (০৫.০৯.২০২৬) — **শেষ পাহারা: আইডির ব্রাঞ্চ-সংকেত আর
           সারির ব্রাঞ্চ কখনোই আলাদা হতে পারবে না।**

           🔴 কেন দরকার (সৎ কথা): KASHAB MANDAL-এর সারিতে `createdAt` আর
              `updatedAt` **হুবহু এক** (11:27:50.808) — অর্থাৎ সারিটা **একবারই**
              লেখা হয়েছে, তবু `branch=Jalpaiguri` আর আইডি `COB-…`। উপরের সব
              পথ একই `draft.branch` ব্যবহার করে, তাই **কোন পথে এটা হলো তা আমি
              এখনো প্রমাণ করতে পারিনি** — TK-কে সেটা জানানো হয়েছে।
           ⇒ তাই কারণ খোঁজার উপর ভরসা না করে **ফলটাই আটকে দেওয়া হলো**: লেখার
             ঠিক আগে আইডির প্রথম তিন অক্ষর মিলিয়ে দেখা হয়; না মিললে ওই
             ব্রাঞ্চের ঠিক আইডি বসে।
           ⛔ শুধু **আজকের তারিখের** আইডিতে (রোগীর হাতে ছাপা পুরনো কাগজ অক্ষত)।
           ⛔ নতুন আইডি বানাতে না পারলে (নেট নেই) আগেরটাই থাকে — সেভ আটকায় না।
           ═══════════════════════════════════════════════════════════════ */
        val patientId = run {
            val code = patientIdRaw.substringBefore("-", "")
            val dpart = patientIdRaw.split("-").getOrNull(1).orEmpty()
            if (code.isNotBlank() && !code.equals(wantCode, ignoreCase = true) && dpart == todayCode) {
                val fixed = try { PatientIdGenerator.generate(draft.branch, draft.date, context) } catch (_: Throwable) { "" }
                if (fixed.isNotBlank()) fixed else patientIdRaw
            } else patientIdRaw
        }
        /* 🔴 V1110 — আইডি বদলে থাকলে ওই রোগীর টাকার সারিগুলোর `patientCode`-ও
           ঠিক করে দেওয়া হয়। ⛔ টাকার অঙ্ক · তারিখ · ব্রাঞ্চ কিছুই ছোঁয়া হয় না,
           শুধু মানুষের-পড়ার আইডির ঘরটা। ⛔ ব্যর্থ হলে চুপচাপ — সেভ আটকায় না। */
        if (recodedId.isNotBlank() && keptId.isNotBlank() && effectiveRowId.isNotBlank()) {
            try {
                Thread {
                    try {
                        val rows = SupabaseClient.fetchListSlimOrNull(
                            "payments", "patientId=eq.$effectiveRowId&patientCode=eq.$keptId", 100, "id")
                        if (rows != null) for (i in 0 until rows.length()) {
                            val rid = rows.optJSONObject(i)?.s("id").orEmpty()
                            if (rid.isNotBlank()) SupabaseClient.updateById(
                                "payments", rid, org.json.JSONObject().put("patientCode", recodedId))
                        }
                    } catch (_: Throwable) { }
                }.start()
            } catch (_: Throwable) { }
        }
        /* 🔵🔒 V516: সারির আইডি —
             · রোজকার সেভ ⇒ `existingRowIdSafe` (ফাঁকা হলে ভিতরে `stableRowId`) — আগের মতোই
             · "Different Patient" ⇒ স্টাফের বাছাইয়ে তৈরি নতুন অনন্য আইডি
           ⛔ `existingRowIdSafe` ফাঁকাই থাকে, তাই **Visit Fee আগের নিয়মেই কাটে**
              (নিচের `paymentRow` দেখুন) — নতুন রোগীর নিজের ভিজিট ফি, ঠিক যেমন হওয়া উচিত।
           ⛔ Follow-up (Visit) সারিও এই নতুন আইডি ধরেই তৈরি হয়, তাই প্রথম
              রোগীর Follow-up-এ হাত পড়ে না। */
        val rowIdForSave = existingRowIdSafe.ifBlank { forceNewPatientRowId }
        /* 🔴🔒 V872 — শেষ শর্তটা: এটা কি আগে থেকে থাকা রোগীর সারি?
           ⛔ ঠিক যে শর্তে Visit Fee কাটা হয় না, হুবহু সেই শর্তই (প্রমাণিত)।
              "Different Patient" নতুন রোগী ⇒ এখানে `false`, আগের মতোই। */
        val patientRow = PatientModel.buildPatientRow(
            draft, patientId, staffMobile, rowIdForSave,
            keepCreatedBy, keepRegisteredBy, keepCreatedAt,
            isExistingRow = existingRowIdSafe.isNotBlank())
        /* 🔴🔒 V399 (16.08.2026, TK-রিপোর্ট ছবিসহ — "২ বার ৩ বার হয়ে যাচ্ছে"):
           এই রোগীর Follow-up (Visit) সারি ক্লাউডে আগে থেকেই আছে কিনা দেখা হয় —
           থাকলে **সেটার আইডিই** ব্যবহার হয়, তাই নতুন সারি আর তৈরি হয় না।
           ⛔ ঠিক B455-এর (Visit Fee) প্রমাণিত প্যাটার্ন — লেখার ঠিক আগে একবার যাচাই।
           ⛔ যাচাই ব্যর্থ হলে (নেট নেই) আগের হুবহু আচরণ — কোনো নতুন বাধা নেই।
           ⛔ স্থানীয় স্টোরে আগে থেকেই (মোবাইল+stage) মিলিয়ে আপডেট হয়
              (`LocalWorkflowStore.upsertFollowUp`), তাই সমস্যাটা শুধু ক্লাউডেই ছিল। */
        var existingFollowUpRowId = ""
        var keepFuCreatedBy = ""
        var keepFuCreatedAt = ""
        try {
            val refForFu = patientRow.s("id")
            if (refForFu.isNotBlank()) {
                // 🟢 V868 — createdBy/createdAt **এই একই অনুরোধেই**, নতুন পড়া নয়।
                val fu = SupabaseClient.fetchListOrNull(
                    "followups", "refId=eq.$refForFu&stage=eq.Patient", 1,
                    select = "id,createdBy,createdAt")
                if (fu != null && fu.length() > 0) {
                    val row = fu.getJSONObject(0)
                    existingFollowUpRowId = row.s("id")
                    keepFuCreatedBy = row.s("createdBy")
                    keepFuCreatedAt = row.s("createdAt")
                }
            }
        } catch (_: Throwable) { }
        val visitFollowUpRow = PatientModel.buildVisitFollowUpRow(
            patientRow, staffMobile, existingFollowUpRowId, keepFuCreatedBy, keepFuCreatedAt)
        // TK-REPORTED BUG FIX (2026-07-25): this used to build+queue a brand
        // new Visit Fee payment row EVERY time save() ran, even via "Update
        // Existing" on the duplicate-mobile popup -- so re-saving an
        // already-registered patient (e.g. correcting a typo, or the staff
        // choosing Update Existing again) silently charged ANOTHER ₹Visit
        // Fee each time, with no dedupe, exactly the repeated "Fees-400/-
        // Cash" rows TK's photo-proof caught. A Visit Fee is only real for
        // a genuinely NEW registration (existingRowId blank); "Update
        // Existing" (existingRowId set) now only updates the patient/
        // followup record, no new fee.
        /* 🔴🔒🔒 V901 (৩১.০৮.২০২৬, TK-রিপোর্ট — *"Visit Fee তো বাধ্যতামূলক,
           তাহলে Missing কেন হবে?"*):

           **আসল দোষ (কোড ধরে যাচাই):** ফর্মে ফি না লিখলে Save-ই হয় না, কিন্তু
           ডুপ্লিকেট-পপ-আপে **"Update Existing"** চাপলে ফি-র টাকার সারিটা
           **কোথাও লেখা হতো না** — স্টাফের নেওয়া টাকাটা হারিয়ে যেত, আর
           Briefing-এ ওই নামটা "Visit Fee Missing"-এ উঠত।

           **এখন (TK-অনুমোদিত নিয়ম):** "Update Existing"-এও ফি লেখা থাকলে সারিটা
           বসে — **কিন্তু শুধু তখনই, যদি ওই রোগীর ভিজিট ফি আগে কখনো নেওয়া
           না হয়ে থাকে**। তাই দুবার ফি কাটার পুরোনো সমস্যা (যেটার জন্য এই
           নিয়মটা বসানো হয়েছিল) ফিরে আসার পথ নেই।
           ⛔ যাচাই করতে না পারলে (নেট নেই) **কিছুই লেখা হয় না** — ভুল করে
              দ্বিতীয়বার কাটার চেয়ে না-লেখাই নিরাপদ।
           ⛔ নতুন রেজিস্ট্রেশনের পথ এক অক্ষরও বদলায়নি। */
        /* 🟥🔒 V958 (০১.০৯.২০২৬, TK-নির্দেশ *"হ্যাঁ, বন্ধ করুন"*) — উপরের V901-এ
           একটা দরজা খোলা ছিল: **যাচাই করা না গেলে** (নেট নেই / পড়া ব্যর্থ) ফি-র
           সারিটা একেবারেই লেখা হত না, তাই স্টাফের নেওয়া টাকা হারিয়ে যেত আর নামটা
           "Visit Fee Missing"-এ উঠত। এখন "জানি না" মানে **বাদ নয়, অপেক্ষা** —
           সারিটা তৈরি হয়ে জমা থাকে, লাইন ফিরলে আবার যাচাই করে তবেই বসে।
           ⛔ দুবার কাটার পথ নেই: সারিটা একবারই তৈরি, নিজের আইডি নিয়ে, আর
              বসানোর ঠিক আগে প্রতিবার আবার যাচাই হয় (PendingVisitFeeStore)। */
        val paymentRow = if (existingRowIdSafe.isBlank()) {
            PatientModel.buildVisitFeePaymentRow(patientRow, draft, staffMobile)
        } else if (draft.regFee > 0.0) {
            when (PendingVisitFeeStore.visitFeeStatus(patientRow.s("id"), patientRow.s("patientId"))) {
                PendingVisitFeeStore.FEE_NOT_TAKEN ->
                    PatientModel.buildVisitFeePaymentRow(patientRow, draft, staffMobile)
                PendingVisitFeeStore.FEE_TAKEN -> null
                else -> {
                    // যাচাই করা গেল না — সারিটা জমা থাক, লাইন ফিরলে বসবে।
                    try {
                        PendingVisitFeeStore.hold(
                            context, PatientModel.buildVisitFeePaymentRow(patientRow, draft, staffMobile)
                        )
                    } catch (_: Throwable) { }
                    null
                }
            }
        } else null

        // OWNER-LOCK: Registration and Registration Fee are one action.
        // Move Enquiry -> Visit locally first and return without waiting for network.
        val localStore = LocalWorkflowStore(context)
        localStore.upsertFollowUp(visitFollowUpRow)
        // TK-REQUESTED ADDITION (2026-07-16): also cache the patient row
        // locally (same pattern as visitFollowUpRow above) so Doctor Queue
        // can show this patient immediately, before the cloud sync below
        // finishes.
        localStore.upsertPatient(patientRow)
        // TK-REQUESTED (2026-07-27), same step: the Visit Fee row was the ONE
        // row of a registration that was not also kept on the phone -- so an
        // offline registration's fee was invisible in Today's Collection until
        // the line came back. Every other payment in the app (Chamber, Advance,
        // Treatment) already caches its row exactly like this, and the readers
        // skip a local row once the cloud row with the same id is seen, so a
        // fee can never be counted twice.
        if (paymentRow != null) localStore.upsertPayment(paymentRow)
        /* 🔴🔵🔒 V516 (TK-অনুমোদিত) — **"Different Patient" হলে Enquiry বন্ধ করা হয় না।**
           `closeInquiry()` ওই **মোবাইলের সব** Inquiry-সারি বন্ধ করে দেয়। এক
           নম্বরে দুজন রোগী থাকলে খোলা Enquiry-টা **কার** তা কোড থেকে জানার
           কোনো উপায় নেই। ভুল করে বন্ধ করলে অন্যজনের চালু Enquiry তালিকা থেকে
           হারিয়ে যেত — সেটা সত্যিকারের ক্ষতি। না-বন্ধ করলে বড়জোর একটা Enquiry
           তালিকায় থেকে যায়, স্টাফ নিজে বন্ধ করতে পারেন — কিছুই হারায় না।
           তাই আন্দাজ না করে **নিরাপদ দিকটাই** বেছে নেওয়া হলো।
           ⛔ রোজকার রেজিস্ট্রেশনে (`forceNewPatientRowId` ফাঁকা) আচরণ হুবহু আগের মতোই। */
        if (forceNewPatientRowId.isBlank()) localStore.closeInquiry(draft.mobileDigitsOnly, patientId)
        // TK-REPORTED (2026-07-27): these three rows go to the cloud one after
        // another, and the retry sends them in exactly the order they are
        // queued here. If the line dies half-way, whichever rows were sent
        // first are the ones that exist.
        //
        // The order used to be patients -> followups -> payments. The Visit
        // tab reads the FOLLOWUPS row, while Chamber Attendance reads the
        // PATIENTS row -- so a half-finished send produced exactly the
        // complaint TK keeps getting: "the patient is in Chamber but missing
        // from the Visit card". Sending the followups row FIRST makes the
        // patient appear where the staff actually looks for them, and the
        // remaining rows follow on the next retry.
        //
        // Nothing about WHAT is saved changes -- same three rows, same
        // contents, same retry. Only which one leaves the phone first.
        // TK-REQUESTED (2026-07-27), "এক রোগী = এক রেকর্ড" ধাপ ১-এর শেষ অংশ:
        // these rows belong to ONE registration, so they now carry one shared
        // batch tag. flushPending() below uses it for a single purpose: if the
        // record is deleted while part of it is still waiting here, the WHOLE
        // group is dropped together. Before this, only the row whose own id was
        // marked deleted was dropped and the rest were still pushed -- leaving
        // a follow-up card or a payment in the cloud with no patient behind it
        // (exactly "চেম্বারে আছে, ভিজিট কার্ডে নেই"). Nothing else changes:
        // same three rows, same contents, same order, same retry.
        val batchId = "reg_" + java.util.UUID.randomUUID().toString().replace("-", "")
        queuePending("followups", visitFollowUpRow, batchId)
        queuePending("patients", patientRow, batchId)
        if (paymentRow != null) {
            queuePending("payments", paymentRow, batchId)
        }
        // TK-REPORTED BUG FIX (2026-07-16): closeSourceEnquiry() (below) used
        // to run ONLY inline here, once, and ONLY if flushPending() emptied
        // the queue on this very first attempt. If that first attempt
        // failed, the OLD Enquiry-stage row on the cloud stayed "Active"
        // forever -- not because anyone could still see it (the Enquiry tab
        // already hides it once the Visit-stage record exists, so this
        // wasn't a visible duplicate-tab bug), but the raw enquiry/followup
        // row itself never got cleaned up on the cloud, which could throw
        // off anything else that reads "enquiries" directly (Reports,
        // Global Search, Draft, CSV Export). Queuing this the same way as
        // the rows above means BottomNav's retry (V77 fix) now finishes
        // this step too, not just the Patient/Visit/Payment rows.
        // 🔵 V516 — উপরের একই কারণ (ক্লাউডের দিকটাও একইভাবে বাদ)।
        if (forceNewPatientRowId.isBlank()) queueCloseIntent(draft.mobileDigitsOnly, patientId)

        Thread {
            try {
                /* ═══════════════════════════════════════════════════════════
                   🔴🔒 V1122 (০৫.০৯.২০২৬, TK-অনুমোদিত — "হ্যাঁ করুন, সাবধানে")
                   TK: *"ভিজিট ফি যখন বাধ্যতামূলক, তখন আমি এই ধরনের নোটিফিকেশন
                   কেন মেনে নেব"* — কথাটা ঠিক, তাই এবার ফাঁকটাই বন্ধ।

                   🔴 প্রমাণিত কারণ: উপরের তিনটে সারি **আলাদা আলাদা তিনটে
                   অনুরোধে** যেত (payments সবার শেষে)। মাঝপথে লাইন কাটলে প্রথম
                   দুটো বসে যেত আর ফি-র সারিটা শুধু **এই ফোনের** জমা-ঘরে পড়ে
                   থাকত — ওই ফোন পরে ফ্লাশ না করলে সারিটা চিরতরে হারাত, অথচ
                   রোগী ক্লাউডে বসেই থাকত। (কোডের নিজের পুরনো মন্তব্যেই লেখা
                   ছিল: *"If the line dies half-way, whichever rows were sent
                   first are the ones that exist."*)

                   ⇒ নতুন রেজিস্ট্রেশনে এখন **একটাই অনুরোধে** তিনটে সারি একসাথে
                     বসে (সার্ভারের নিজের লেনদেন) — একটায় গোলমাল হলে তিনটেরই
                     কিছু বসে না।

                   ⛔ **এটা কেবল বাড়তি একটা চেষ্টা** — না চললে (SQL ফাংশনটা এখনো
                      চালানো হয়নি · নেট নেই · সারিটা আগে থেকেই আছে) নিচের
                      পুরনো, প্রমাণিত পথটাই আগের মতোই চলে। কিচ্ছু ভাঙে না।
                   ⛔ শুধু **নতুন** রেজিস্ট্রেশনে (`existingRowIdSafe` ফাঁকা) —
                      "Update Existing"-এ পুরনো পথই, তাই দুবার ফি কাটার পথ নেই।
                   ⛔ সফল হলে ওই তিনটে সারি জমা-ঘর থেকে তুলে নেওয়া হয়, নইলে
                      পরের ফ্লাশে আবার পাঠানো হত (একই আইডি, তাই ক্ষতি হত না)।
                   ═══════════════════════════════════════════════════════════ */
                var atomicOk = false
                if (existingRowIdSafe.isBlank()) {
                    atomicOk = try {
                        SupabaseClient.registerAtomic(patientRow, visitFollowUpRow, paymentRow)
                    } catch (_: Throwable) { false }
                    if (atomicOk) {
                        try { dropBatchFromQueue(batchId) } catch (_: Throwable) { }
                        try {
                            localStore.upsertFollowUp(visitFollowUpRow, "SYNCED")
                            localStore.upsertPatient(patientRow, "SYNCED")
                            if (paymentRow != null) localStore.upsertPayment(paymentRow, "SYNCED")
                        } catch (_: Throwable) { }
                    }
                }
                flushPending()
                if (loadPendingQueue().length() == 0) {
                    localStore.upsertFollowUp(visitFollowUpRow, "SYNCED")
                    localStore.upsertPatient(patientRow, "SYNCED")
                }
                /* ═══════════════════════════════════════════════════════════
                   🔴🔒 V1101 (০৫.০৯.২০২৬) — TK: *"রেজিস্ট্রেশন নেওয়ার সময়
                   ভিজিট ফি বাধ্যতামূলক, তাহলে আমার কাছে Visit Fee Missing
                   নোটিফিকেশন আসবেই বা কেন"* — কথাটা ঠিক। সারানোর কাজটা
                   মালিকের নয়, **অ্যাপের**।

                   ⇒ সেভের পরে অ্যাপ নিজেই মিলিয়ে দেখে ফি-র সারিটা সত্যিই
                     ক্লাউডে বসেছে কিনা। না বসলে সারিটা জমা-ঘরে (PendingVisitFeeStore)
                     রেখে দেওয়া হয় — লাইন ফিরলে নিজে থেকেই বসে যায়।

                   ⛔ **দুবার কাটার পথ নেই:** বসানোর ঠিক আগে প্রতিবার আবার
                      যাচাই হয়; আগে থেকে ফি থাকলে সারিটা চুপচাপ বাদ যায়।
                      সারির আইডিও একটাই, তাই একই আইডিতেই বসে (upsert)।
                   ⛔ যাচাই করা না গেলে (নেট নেই) সারিটা জমাই থাকে — হারায় না।
                   ⛔ টাকার অঙ্ক কখনো আন্দাজে বানানো হয় না — স্টাফের লেখা
                      অঙ্কটাই, সেভের মুহূর্তের সেই একই সারি।
                   ═══════════════════════════════════════════════════════════ */
                if (paymentRow != null) try {
                    val pid = patientRow.s("id")
                    val code = patientRow.s("patientId")
                    if (PendingVisitFeeStore.visitFeeStatus(pid, code) != PendingVisitFeeStore.FEE_TAKEN) {
                        PendingVisitFeeStore.hold(context, paymentRow)
                    }
                } catch (_: Throwable) { }
            } catch (_: Throwable) { }
        }.start()
        return patientId
    }

    /**
     * Native port of app.js closeEnquiryAfterRegistration(): flips the source
     * enquiry row to Registered (recording the new patientId) and moves its
     * Inquiry-stage follow-up row to Registered/Closed, so a converted enquiry
     * no longer shows as an open Inquiry.
     * TK-REPORTED BUG FIX (2026-07-16): this used to swallow every network
     * error internally and never tell the caller anything went wrong -- so
     * flushCloseIntents() below (which retries this on failure) could never
     * actually detect a failure and would just drop the retry after one
     * attempt, exactly the same "silently claims success" bug already fixed
     * elsewhere in this file. Now returns whether every update it attempted
     * actually succeeded, so a real failure gets retried and a real success
     * removes it from the queue -- still "best-effort" in the sense that it
     * never throws/blocks the original save either way. */
    /* 🔴🔒 V901-এর `visitFeeAlreadyTaken()` V958-এ সরানো হলো — ওটা "যাচাই করা
       গেল না"-কেও "ফি নেওয়া আছে" বলত, আর ওখান থেকেই টাকা হারাত। এখন তিন রকম
       উত্তর দেয় `PendingVisitFeeStore.visitFeeStatus()`। */

    private fun closeSourceEnquiry(mobileDigitsOnly: String, patientId: String): Boolean {
        val digits = mobileDigitsOnly.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return true // nothing meaningful to retry
        val now = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).format(java.util.Date())
        var allOk = true

        try {
            // Update every matching Enquiry row, not only the first match.
            // 🔵🔒 V494 (২১.০৮.২০২৬, TK-যাচাই ৫): নিচে এই তালিকা থেকে **শুধু
            // `id`** পড়া হয় (:260), অথচ আগে `select=*` দিয়ে প্রতিটা সারির সব
            // ঘর নামত। ⛔ সারির সংখ্যা · ছাঁকনি · limit কিছুই বদলায়নি।
            // ⛔ সরু পড়া ব্যর্থ হলে fetchListSlim নিজেই পুরনো পথে ফিরে যায়।
            val enquiries = SupabaseClient.fetchListSlim(
                "enquiries", "mobile=like.*$digits", 5000, "id"
            )
            for (i in 0 until enquiries.length()) {
                val id = enquiries.getJSONObject(i).s("id")
                if (id.isBlank()) continue
                val fields = JSONObject()
                    .put("stage", "Registered")
                    .put("status", "Registered")
                    .put("nextFollow", "")
                    .put("convertedPatientId", patientId)
                    .put("convertedAt", now)
                    .put("updatedAt", now)
                if (!SupabaseClient.updateById("enquiries", id, fields)) allOk = false
            }
        } catch (_: Exception) { allOk = false }

        try {
            // Close every Inquiry-stage follow-up for this mobile. Only columns
            // that actually exist in the followups table are patched, so the
            // whole update cannot fail because of unknown fields.
            // 🔵🔒 V494 (২১.০৮.২০২৬, TK-যাচাই ৫): নিচে এই তালিকা থেকে **শুধু
            // `id` ও `history`** পড়া হয় (:282, :284)। আগে `select=*` মানে
            // followups সারির **রোগীর base64 ছবিও** নামত — আর এটা চলে
            // **প্রতিটা রেজিস্ট্রেশনে**। ⛔ সারি · ছাঁকনি · limit অপরিবর্তিত।
            /* 🔵🔒 V536 (২২.০৮.২০২৬, TK-নির্দেশ) — এক নম্বরে দু'জন আলাদা রোগী
               থাকলে **অন্যজনের খোলা Inquiry আর বন্ধ হয়ে যাবে না**।
               ⛔ প্রমাণ না থাকলে আগের মতোই বন্ধ হয় — একটাও সারি বাদ পড়ে না।
               ⛔ বাছাই করতে যে তিনটে ঘর লাগে (`refId,patientId,name`) সেগুলোই
                  শুধু যোগ করা হলো — ছোট ঘর, আর সারি এমনিতেই হাতে গোনা। */
            val followups = SupabaseClient.fetchListSlim(
                "followups", "mobile=like.*$digits&stage=eq.Inquiry", 5000, "id,history,refId,patientId,name"
            )
            for (i in 0 until followups.length()) {
                val row = followups.getJSONObject(i)
                val id = row.s("id")
                if (id.isBlank()) continue
                if (PatientIdentity.provablyOtherPatient(row, digits, patientId, patientId)) continue
                val history = row.optJSONArray("history") ?: JSONArray()
                history.put(
                    JSONObject()
                        .put("date", PatientModel.today())
                        .put("time", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()))   /* ⏰ V827 — সময়ও জমা হয় (TK: "LAST CALL তারিখের পরে যেন Time থাকে")। */
                        .put("remark", "Converted to Patient Registration")
                        .put("staff", "Registration")
                )
                val fields = JSONObject()
                    .put("stage", "Registered")
                    .put("status", "Closed")
                    .put("nextFollow", "")
                    .put("lastRemark", "Converted to Patient Registration")
                    .put("history", history)
                    .put("updatedAt", now)
                if (!SupabaseClient.updateById("followups", id, fields)) allOk = false
            }
        } catch (_: Exception) { allOk = false }
        return allOk
    }

    fun flushPending() {
        synchronized(LOCK) {
        val queue = loadPendingQueue()
        if (queue.length() > 0) {
            // TK-REQUESTED (2026-07-27): first work out which registration
            // groups have been deleted in the meantime. If ANY row of a group
            // was deleted, none of that group's rows may be pushed -- half a
            // patient in the cloud is worse than none. Rows queued by an older
            // version carry no group tag and behave exactly as before.
            val cancelledBatches = HashSet<String>()
            for (i in 0 until queue.length()) {
                val e = queue.optJSONObject(i) ?: continue
                val b = e.optString("batch", "")
                if (b.isBlank()) continue
                val rid = e.optJSONObject("row")?.optString("id", "") ?: ""
                if (DeletedGuard.isDeleted(e.optString("table"), rid, context)) cancelledBatches.add(b)
            }
            val stillPending = JSONArray()
            // V1320 (তালিকা ৪২৪, নিয়ম ৭ — Follow-up-এ ধরা পড়া একই দোষ এখানেও ছিল):
            // আগে প্রতিটা সফল সারির জন্য আলাদা করে upsertPatient/upsertFollowUp/
            // upsertPayment ডাকা হতো -- N সারি মানে N বার পুরো টেবিল পড়া+লেখা,
            // ঠিক V1319-এ ধরা পড়া মূল সমস্যার মতোই (শুধু কম-ঘটা পথে, নেট বন্ধ
            // থাকার পর অনেক সারি জমলে)। এখন জমিয়ে একবারেই batched upsert।
            val syncedPatients = ArrayList<JSONObject>()
            val syncedFollowUps = ArrayList<JSONObject>()
            val syncedPayments = ArrayList<JSONObject>()
            for (i in 0 until queue.length()) {
                val entry = queue.getJSONObject(i)
                val table = entry.getString("table")
                val row = entry.getJSONObject("row")
                val batch = entry.optString("batch", "")
                if (batch.isNotBlank() && cancelledBatches.contains(batch)) continue
                // TK-REQUESTED (2026-07-26): a row deleted in the meantime must
                // not be pushed back into the cloud by this retry. Dropped from
                // the queue; every other row is handled exactly as before.
                if (DeletedGuard.isDeleted(table, row.optString("id", ""), context)) continue
                if (SupabaseClient.upsert(table, row)) {
                    // TK-REPORTED BUG FIX (2026-07-16): same fix as
                    // EnquiryRepository.flushPending() -- confirm the local
                    // cache row as SYNCED right when this retry succeeds,
                    // so LocalWorkflowStore's stale-cloud-refresh guard
                    // doesn't keep treating this record as having an
                    // un-synced local change forever after it has, in
                    // fact, already reached the cloud.
                    when (table) {
                        "patients" -> syncedPatients.add(row)
                        "followups" -> syncedFollowUps.add(row)
                        "payments" -> syncedPayments.add(row)
                    }
                } else {
                    stillPending.put(entry)
                }
            }
            if (syncedPatients.isNotEmpty() || syncedFollowUps.isNotEmpty() || syncedPayments.isNotEmpty()) {
                val store = LocalWorkflowStore(context)
                store.upsertPatients(syncedPatients, "SYNCED")
                store.upsertFollowUps(syncedFollowUps, "SYNCED")
                store.upsertPayments(syncedPayments, "SYNCED")
            }
            savePendingQueue(stillPending)
        }
        // TK-REPORTED BUG FIX (2026-07-16): retry any "close the source
        // Enquiry" step that didn't finish on a previous attempt too --
        // checked independently every time (BottomNav.wire() calls this
        // often), not skipped just because the Patient/Visit/Payment rows
        // above already finished syncing.
        flushCloseIntents()
        // 🟥 V958: যাচাই না হওয়ায় যে ভিজিট-ফি সারিগুলো অপেক্ষায় আছে, লাইন
        // ফিরলে সেগুলোও আবার যাচাই করে বসিয়ে দেওয়া হয়।
        try { PendingVisitFeeStore.flush(context) } catch (_: Throwable) { }
        }
    }

    /** Queues "close the source Enquiry for this mobile" so it can be
     * retried later if the very first attempt (in save() above) doesn't
     * reach the cloud. Safe to run more than once -- closeSourceEnquiry()
     * just re-writes the same "Registered/Closed" state either way. */
    /** 🟢 V1122 — এক লেনদেনে বসে যাওয়া সারিগুলো জমা-ঘর থেকে তুলে নেওয়া।
     *  ⛔ ব্যাচের চিহ্ন ধরে, তাই ওই রেজিস্ট্রেশনের সারিগুলোই ওঠে, আর কারো নয়। */
    private fun dropBatchFromQueue(batch: String) {
        if (batch.isBlank()) return
        synchronized(LOCK) {
            val queue = loadPendingQueue()
            val next = org.json.JSONArray()
            for (i in 0 until queue.length()) {
                val e = queue.optJSONObject(i) ?: continue
                if (e.optString("batch", "") != batch) next.put(e)
            }
            savePendingQueue(next)
        }
    }

    private fun queueCloseIntent(mobileDigitsOnly: String, patientId: String) {
        synchronized(LOCK) {
        val queue = loadCloseQueue()
        val next = JSONArray()
        for (i in 0 until queue.length()) {
            val e = queue.getJSONObject(i)
            if (e.optString("mobile") != mobileDigitsOnly) next.put(e)
        }
        next.put(JSONObject().put("mobile", mobileDigitsOnly).put("patientId", patientId))
        prefs.edit().putString("closeQueue", next.toString()).commit()
        }
    }

    private fun flushCloseIntents() {
        synchronized(LOCK) {
        val queue = loadCloseQueue()
        if (queue.length() == 0) return
        val stillPending = JSONArray()
        for (i in 0 until queue.length()) {
            val e = queue.optJSONObject(i) ?: continue
            try {
                val ok = closeSourceEnquiry(e.optString("mobile"), e.optString("patientId"))
                if (!ok) stillPending.put(e)
            } catch (_: Throwable) {
                stillPending.put(e)
            }
        }
        prefs.edit().putString("closeQueue", stillPending.toString()).commit()
        }
    }

    private fun loadCloseQueue(): JSONArray {
        val raw = prefs.getString("closeQueue", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    private fun queuePending(table: String, row: JSONObject, batch: String = "") {
        synchronized(LOCK) {
        // 🚨 TK-REPORTED, LIVE (2026-07-27): "কখনো পেমেন্ট হারিয়ে যায়, কখনো
        // পেশেন্ট হারিয়ে যায়..." ROOT CAUSE FOUND HERE.
        //
        // The retry loop that later pushes this row to the cloud SKIPS any row
        // whose id sits in the "deleted" list (DeletedGuard) . that guard
        // exists so a record deleted by staff cannot be resurrected by an old
        // queued save, which is right. BUT an id can legitimately come back:
        // "Update Existing" on the duplicate-mobile popup reuses the same row
        // id, a patient restored from Trash keeps their id, and a person can
        // be registered again after being deleted. In every one of those
        // cases the brand-new save was silently thrown away FOREVER . the
        // staff saw "saved", nothing stayed queued, and nothing ever reached
        // the cloud. That is a patient or a payment simply gone.
        //
        // FIX: a NEW save always beats an OLD delete mark. Clearing the mark
        // here, at the moment of saving, keeps the guard's real purpose
        // intact: if staff delete this record AFTER this save is queued, the
        // delete marks it again and the retry still correctly drops it.
        try { DeletedGuard.unmark(table, row.optString("id", ""), context) } catch (_: Throwable) { }
        val queue = loadPendingQueue()
        val id = row.optString("id")
        val next = JSONArray()
        for (i in 0 until queue.length()) {
            val e = queue.getJSONObject(i)
            val same = e.optString("table") == table && id.isNotBlank() && e.optJSONObject("row")?.optString("id") == id
            if (!same) next.put(e)
        }
        val newEntry = JSONObject().put("table", table).put("row", row)
        if (batch.isNotBlank()) newEntry.put("batch", batch)
        next.put(newEntry)
        savePendingQueue(next)
        // TK-REQUESTED (2026-07-25): the moment anything is queued, ask
        // WorkManager to sync right away. WorkManager runs even when the
        // app is closed or the staff switched to another app, so a save
        // no longer waits for the next screen-open or the 15-minute
        // backstop . it reaches the cloud within seconds of the network
        // being available. Nothing else changes; the same proven
        // flushPending() work runs, just sooner.
        try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(context) } catch (_: Throwable) { }

        }
    }

    private fun loadPendingQueue(): JSONArray {
        val raw = prefs.getString("queue", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    private fun savePendingQueue(queue: JSONArray) {
        prefs.edit().putString("queue", queue.toString()).commit()
    }
}
