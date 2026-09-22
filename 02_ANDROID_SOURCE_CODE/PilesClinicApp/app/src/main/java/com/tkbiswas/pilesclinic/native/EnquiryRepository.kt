package com.tkbiswas.pilesclinic.native

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles saving a native Enquiry: duplicate-number check, upload to
 * Supabase (enquiries + followups, matching the WebView's saveEnq() +
 * ensureFollow() exactly), and a simple local pending-queue fallback if the
 * device is offline when Save is tapped -- retried the next time this
 * repository is used, so an enquiry entered without signal is never lost,
 * without needing the full offline-sync architecture the disconnected
 * Phase 5 Room/WorkManager stack was built for.
 */
class EnquiryRepository(private val context: Context) {

    companion object {
        // TK-REQUESTED FIX (2026-07-19): same shared-lock fix as
        // LocalWorkflowStore/RegistrationRepository -- a fresh
        // EnquiryRepository(context) is created almost everywhere this is
        // used, so this class's own pending queue needs a lock that is
        // shared across every instance, not just "this" one.
        private val LOCK = Any()
    }

    private val prefs = context.getSharedPreferences("piles_clinic_enquiry_pending", Context.MODE_PRIVATE)

    data class DuplicateResult(val found: Boolean, val name: String, val branch: String, val stage: String, val status: String = "")

    /** Checks both "enquiries" and "patients" tables for an existing row with
     * this mobile number, matching duplicate()'s check in app.js. Best-effort:
     * on any network failure this returns "not found" rather than blocking
     * the save, same as the WebView's own duplicate check degrades.
     *
     * 🔒🔒 খাতার সারি B189 (TK, 30.07.2026 সন্ধ্যা — "ইনকুইরি করার পরে
     * লোডিং" ও "অন্য ব্রাঞ্চের জন্য এনকুইরি করার পর লোডিং" রিপোর্টের অংশ):
     * **আসল কারণ (কোড ধরে):** Save চাপার পরে এই ফাংশনটাই সবার আগে চলে, আর
     * এটা ক্লাউডে **দুইবার, একটার পর একটা** যায় (আগে "enquiries" টেবিল,
     * তারপর "patients" টেবিল) — দুর্বল নেটওয়ার্কে (TK-এর ফোনে দেখা গেছে
     * 53 KB/S) প্রতিটা কল কয়েক সেকেন্ড লাগলে দুটো মিলিয়ে সেটাই "লোডিং" মনে
     * হয়। এনকোয়ারির ব্রাঞ্চ যেটাই হোক (নিজের বা অন্য ব্রাঞ্চ) — একই ফর্ম,
     * একই ফাংশন, তাই দুটো ক্ষেত্রেই একই কারণ, একই সমাধান।
     *
     * ⛔ **কী বদলাল:** দুটো চেক এখন **একসাথে (সমান্তরালে)** পাঠানো হয় —
     * একটার শেষের অপেক্ষা না করেই আরেকটা শুরু হয়ে যায়, তাই মোট সময় প্রায়
     * অর্ধেক। ⛔ **কোনো নিয়ম বদলায়নি** — "enquiries" আগে দেখা হয়, তারপর
     * "patients" (যদি enquiries-এ না পাওয়া যায়) — এই অগ্রাধিকার, ফলাফল,
     * "ONE MOBILE = ONE REGISTRATION" নিয়ম, ফোনের নিজের তালিকা মেলানো —
     * সবকিছু হুবহু আগের মতোই; শুধু দুটো কল একসাথে ছোঁড়া হচ্ছে।
     */
    // 🔴🔴🔴 খাতার সারি B203 (TK, 30.07.2026 রাত — Android Studio-তে
    // সত্যিকারের বিল্ড-এরর, ছবিসহ, TK তীব্র ক্ষুব্ধ): এই ফাইলেরই আসল বিল্ড
    // ব্যর্থ হয়েছিল "app:compileDebugKotlin 2 errors" — এই ফাংশনেই।
    //
    // **আসল কারণ (এবার ধরা পড়েছে, ১০০% নিশ্চিত হয়ে):** `async` Kotlin-এ
    // `CoroutineScope`-এর একটা **extension function** (`fun CoroutineScope.
    // async(...)`), সাধারণ কোনো টপ-লেভেল ফাংশন নয়। আগের কোডে
    // `kotlinx.coroutines.async(...)` লিখে **সরাসরি প্যাকেজ-নাম দিয়ে** এটা
    // ডাকার চেষ্টা করা হয়েছিল — কিন্তু Kotlin-এ extension function-কে এভাবে
    // (রিসিভার ছাড়া, শুধু প্যাকেজ-নাম জুড়ে) ডাকা যায় **না**; এটা `import`
    // করে সাধারণভাবে ডাকতে হয় (তখন যে জায়গায় ডাকা হচ্ছে সেখানকার implicit
    // `CoroutineScope` রিসিভার নিজে থেকেই যুক্ত হয়)। তাই কম্পাইলার
    // "Unresolved reference: async" বলে আটকে গিয়েছিল — এটা এই একই ফাংশনে
    // **দুইবার** (দুটো `async` কল) ঘটেছিল, ঠিক ছবিতে দেখা "2 errors"।
    // ⛔ `coroutineScope { ... }` নিজে টপ-লেভেল suspend ফাংশন (extension নয়),
    //    তাই ওটা ফাইল-নাম-জুড়ে ডাকলেও কাজ করত — বাগটা শুধু `async`-এই ছিল।
    //
    // **সমাধান:** ফাইলের উপরে `import kotlinx.coroutines.async` (ও
    // `coroutineScope`, `Dispatchers`) যোগ করে এখন সাধারণ, সঠিক Kotlin
    // সিনট্যাক্সে ডাকা হয়েছে। ⛔ **এই ফাংশনের বাইরের/আশেপাশের কোনো লজিক,
    // ডুপ্লিকেট-চেকের নিয়ম, অগ্রাধিকার — কিছুই বদলায়নি**, শুধু সিনট্যাক্স
    // ঠিক হলো।
    //
    // ⛔ **প্রজেক্টের অন্য কোথাও এই একই ভুল (fully-qualified extension
    // function call) আছে কিনা খুঁজে দেখা হয়েছে** — এই সেশনে যোগ করা বাকি সব
    // জায়গায় (`BackgroundWork.run{}` — এটা `object`-এর সাধারণ member
    // function, extension নয়, তাই এই সমস্যা নেই) আলাদা করে হাতে যাচাই করা
    // হয়েছে, আর কোথাও পাওয়া যায়নি।
    suspend fun checkDuplicate(mobileDigitsOnly: String): DuplicateResult {
        val normalized = EnquiryModel.normalizedMobile(mobileDigitsOnly)
        val (enq, pat) = coroutineScope {
            val enqCall = async(Dispatchers.IO) {
                SupabaseClient.findByMobile("enquiries", normalized, "name,branch,stage,status")
            }
            val patCall = async(Dispatchers.IO) {
                SupabaseClient.findByMobile("patients", normalized, "name,branch")
            }
            enqCall.await() to patCall.await()
        }
        if (enq.length() > 0) {
            val row = enq.getJSONObject(0)
            return DuplicateResult(true, row.s("name"), row.s("branch"), row.optString("stage", "Inquiry"), row.optString("status", ""))
        }
        if (pat.length() > 0) {
            val row = pat.getJSONObject(0)
            return DuplicateResult(true, row.s("name"), row.s("branch"), "Patient", "Patient")
        }
        // TK'S STANDING RULE (restated 2026-07-27): ONE MOBILE = ONE REGISTRATION.
        // Same hole as RegistrationRepository.checkDuplicatePatient: both cloud
        // lookups above return an EMPTY list on any network failure, and empty
        // was being read as "this number is new", so a dead line let the same
        // number be entered a second time. The phone's own saved list needs no
        // network. Patient is checked first so the popup shows the later stage.
        // If neither is there, the save goes ahead exactly as before.
        val store = LocalWorkflowStore(context)
        val localPat = store.findPatientByMobile(normalized)
        if (localPat != null) {
            return DuplicateResult(true, localPat.s("name"), localPat.s("branch"), "Patient", "Patient")
        }
        val localEnq = store.findEnquiryByMobile(normalized)
        if (localEnq != null) {
            return DuplicateResult(
                true,
                localEnq.s("name"),
                localEnq.s("branch"),
                localEnq.s("stage").ifBlank { "Inquiry" },
                localEnq.s("status")
            )
        }
        return DuplicateResult(false, "", "", "", "")
    }

    /**
     * 🚨🚨 TK-REPORTED, LIVE (29.07.2026 রাত ৮.০০ · খাতার সারি B131 · B132):
     * TK: *"সমস্ত staff-কে জিজ্ঞাসা করলাম, কেউ Restore করেনি। ডাক্তার অ্যাপ
     * ব্যবহারই করেনি। আমি মাস্টার, আমি করিনি।"* — তবু Reject করা নম্বর চালু
     * তালিকায় ফিরে আসছিল।
     *
     * **আসল কারণ (কোড ধরে):** পুরনো নম্বরে আবার কল এলে স্টাফ স্বাভাবিকভাবে
     * Enquiry ফর্ম ভরেন; তখন *"This number already exists"* বাক্সের বড় বোতাম
     * **Restore & Move** চাপলে পুরনো রেকর্ডটা চালু তালিকায় ফিরে আসে। বাক্সটা
     * কোথাও বলত না যে রেকর্ডটা **আগে Reject/Incomplete করা হয়েছিল**, তাই স্টাফ
     * ওটাকে "নতুন এনকোয়ারি সেভ করলাম" ভেবে চাপতেন — তাঁদের কাছে ওটা Restore নয়।
     *
     * এই ফাংশনটা ওই বাক্সে দেখানোর জন্য জানায় — রেকর্ডটা বন্ধ ছিল কিনা, কে
     * বন্ধ করেছিলেন, কবে, আর এখন Draft-এর কোন তালিকায় আছে।
     *
     * ⛔ **কোনো নিয়ম বদলায় না** — শুধু পড়া, কিছু লেখা হয় না।
     * ⛔ **রোজকার ব্যবহারে বাড়তি কোনো ক্লাউড-কল নয়** — এটা তখনই চলে যখন
     *    ডুপ্লিকেট নম্বরের বাক্সটা ওঠে (দিনে হাতে গোনা কয়েকবার), আর তখন
     *    মাত্র পাঁচটা ঘর নামে।
     * ⛔ খোঁজা ব্যর্থ হলে `closed = false` ফেরে — বাক্সটা তখন হুবহু আগের মতোই
     *    দেখাবে, কিছু আটকাবে না।
     */
    data class ClosedInfo(
        val closed: Boolean,
        val what: String = "",      // REJECTED / MARKED INCOMPLETE
        val byName: String = "",    // যিনি করেছিলেন
        val whenText: String = "",  // 29.07.2026 5.10 PM
        val listName: String = "",  // Draft → Enquiry Reject List
        // 🔒 খাতার সারি B133: Registration-এর বাক্সে লেখাটা আলাদা হয়
        //    ("REGISTRATION WAS CANCELLED"), তাই ধাপটাও জানানো হয়।
        val stage: String = "",
        // 🔒 B601 (10.08.2026, TK-অনুমোদিত): নতুন History-Warning পপ-আপে পুরনো
        //    রেকর্ডের নাম/ব্রাঞ্চ দেখাতে (additive — পুরনো কলার এগুলো ব্যবহার করে না)।
        val name: String = "",
        val branch: String = ""
    )

    fun closedInfo(mobileDigitsOnly: String): ClosedInfo {
        return try {
            val normalized = EnquiryModel.normalizedMobile(mobileDigitsOnly)
            val rows = SupabaseClient.findByMobile(
                "followups", normalized, "id,stage,status,history,updatedAt,name,branch", 20
            )
            var picked: JSONObject? = null
            for (i in 0 until rows.length()) {
                val r = rows.getJSONObject(i)
                val st = r.s("status")
                // V448: legacy rows can be Active today even though their append-only
                // history still ends with the old Reject. Use the same exact history
                // classifier as the Follow-up list so the duplicate warning cannot
                // silently treat a rejected number as a fresh active enquiry.
                if (st.equals("Cancelled", true) || st.equals("Incomplete", true) ||
                    st.equals("Rejected", true) || st.equals("Closed", true) ||
                    FollowUpRepository.inquiryHistoryEndsTerminal(r)) { picked = r; break }
            }
            val row = picked ?: return ClosedInfo(false)
            val status = row.s("status")
            val isIncomplete = status.equals("Incomplete", true)
            val stage = row.s("stage").ifBlank { "Inquiry" }
            val what = if (isIncomplete) "MARKED INCOMPLETE" else "REJECTED"
            val listName = when {
                isIncomplete -> "Draft \u2192 Incomplete Patient"
                stage.equals("Inquiry", true) -> "Draft \u2192 Enquiry Reject List"
                else -> "Draft \u2192 Visit Reject List"
            }
            // কে ও কবে — `history`-র শেষ যে সারিতে বন্ধ করার কথা লেখা আছে।
            var byName = ""
            var whenRaw = ""
            val hist = row.optJSONArray("history")
            if (hist != null) {
                for (i in hist.length() - 1 downTo 0) {
                    val h = hist.optJSONObject(i) ?: continue
                    val remark = h.s("remark")
                    val hit = remark.contains("Reject", true) ||
                        remark.contains("Cancelled", true) ||
                        remark.contains("Incomplete", true)
                    if (!hit) continue
                    byName = h.s("staff")
                    whenRaw = h.s("time").ifBlank { h.s("date") }
                    break
                }
            }
            if (whenRaw.isBlank()) whenRaw = row.s("updatedAt")
            ClosedInfo(true, what, byName, prettyWhen(whenRaw), listName, stage, row.s("name"), row.s("branch"))
        } catch (_: Throwable) { ClosedInfo(false) }
    }

    /** 🔒 গ্লোবাল রুল (খাতার সারি B76): তারিখ `29.07.2026`, সময় `5.10 PM`
     *  (AM/PM বড় হাতের অক্ষরে)। চেনা না গেলে যা আছে তাই ফেরে, কখনো ফাঁকা নয়। */
    private fun prettyWhen(raw: String): String {
        if (raw.isBlank()) return ""
        val date = DateUtil.display(raw)
        if (raw.length < 16 || !raw.contains("T")) return date
        return try {
            val src = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val parsed = src.parse(raw.substring(0, 19)) ?: return date
            val out = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US)
            "$date " + out.format(parsed)
        } catch (_: Exception) { date }
    }

    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())

    /**
     * Duplicate handling (locked with TK): never create a second record for a
     * number already in the system. Instead RESTORE the existing record and
     * MOVE it to the branch just selected on the form — keeping whichever
     * section (Enquiry / Visit / Patient) it was already in, and carrying its
     * full old history. Reactivates a Rejected/Draft record. Payments are NOT
     * touched (financial history stays intact).
     *
     * NOTE: build/test happens on the device — TK live-tests before locking.
     */
    /**
     * 🔵🔒 V534 (২২.০৮.২০২৬, TK-নির্দেশ) — **অন্য রোগীর রোগ/ব্রাঞ্চ আর বদলাবে না।**
     *
     * **আগে যা হত:** এই ফাংশন ওই নম্বরের **সব** followups · enquiries ·
     * patients সারিতে নতুন **ব্রাঞ্চ · status · রোগ · lastRemark** বসিয়ে দিত।
     * এক নম্বরে দু'জন আলাদা রোগী থাকলে **অন্যজনের রোগের নাম মুছে গিয়ে**
     * এঁর রোগ বসে যেত, আর তাঁর ব্রাঞ্চও বদলে যেত।
     *
     * **এখন:** `preferPatientCode` / `preferRowId` দিলে — নম্বরটা ভাগ করা হলে
     * **শুধু প্রমাণসহ সেই রোগীর সারিগুলোই** ছোঁয়া হয়।
     *
     * ⛔ দুটোই ফাঁকা, বা নম্বর ভাগ করা নয় (রোজকার ৯৯%) ⇒ **হুবহু আগের আচরণ**,
     *    এক অক্ষরও বদলায়নি।
     * ⛔ enquiries সারিতে রোগী চেনার কোনো ঘরই নেই — তাই নম্বর ভাগ করা থাকলে
     *    সেগুলো **ছোঁয়াই হয় না**।
     */
    fun restoreAndMove(
        mobileDigitsOnly: String, newBranch: String, newDisease: String,
        newRemark: String, newNextFollow: String, staffName: String,
        preferPatientCode: String = "", preferRowId: String = ""
    ): Boolean {
        val normalized = EnquiryModel.normalizedMobile(mobileDigitsOnly)
        val today = EnquiryModel.today()
        val now = isoNow()
        var ok = false

        // (1) followups rows (the card staff sees / calls from) — move branch,
        //    reactivate, keep stage/section, append history, keep old history.
        // TK-REPORTED CLASS OF BUG (fixed here 2026-07-27): findByMobile()
        // returns just ONE row unless a count is asked for. This loop is meant
        // to move EVERY follow-up row of this person to the new branch, but a
        // patient normally has more than one (Inquiry + Visit + Treatment) --
        // so only one row moved and the others stayed in the old branch. The
        // patient then showed in one screen/branch and not in another. Same
        // mistake was already found and fixed in MobileChangeSync (V134); it
        // was still here.
        val fus = SupabaseClient.findByMobile("followups", normalized, "*", 50)
        /* 🔵 V534: এই নম্বরে সত্যিই একাধিক আলাদা রোগী আছেন কিনা — একবারই দেখা হয়,
           আগে-আনা সারিগুলোর উপরেই (নতুন কোনো ক্লাউড-অনুরোধ নয়)। */
        val sharedNumber = PatientIdentity.isSharedNumber(fus, mobileDigitsOnly) &&
            (preferPatientCode.isNotBlank() || preferRowId.isNotBlank())
        for (i in 0 until fus.length()) {
            val row = fus.getJSONObject(i)
            val id = row.optString("id")
            if (id.isBlank()) continue
            if (sharedNumber && !PatientIdentity.rowBelongsTo(row, preferRowId, preferPatientCode)) continue
            val history = row.optJSONArray("history") ?: JSONArray()
            history.put(
                JSONObject()
                    .put("date", today)
                    .put("time", isoNow())   /* ⏰ V827 — সময়ও জমা হয় (TK: "LAST CALL তারিখের পরে যেন Time থাকে")। */
                    .put("remark", "Restored & moved to $newBranch (Enquiry form)")
                    .put("staff", staffName)
                    .put("status", "Active")
                    .put("decisionVersion", "V448")
            )
            val fields = JSONObject()
                .put("branch", newBranch)
                .put("status", "Active")
                .put("nextFollow", newNextFollow)
                .put("history", history)
                .put("updatedAt", now)
            if (newRemark.isNotBlank()) fields.put("lastRemark", newRemark)
            if (newDisease.isNotBlank()) fields.put("disease", newDisease)
            if (SupabaseClient.updateById("followups", id, fields)) ok = true
            // ⚡ 🔒 TK-এর স্থায়ী নিয়ম (খাতার সারি B21, 28.07.2026): *"আমি আমার
            // ফোনে যা যা কাজ করবো, সেটা যেন সাথে সাথেই দেখায়।"*
            //
            // 🚨 খাতার সারি B80 (TK, 29.07.2026 — স্টাফের অভিযোগ): এই ফাংশনটা
            // এতদিন **শুধু ক্লাউডে** লিখত, ফোনের নিজের জমানো তালিকায় কিছুই
            // বদলাত না। ফলে Restore করার পরেও স্টাফের ফোনে কার্ডটা **পুরনো
            // ব্রাঞ্চেই / পুরনো অবস্থাতেই** পড়ে থাকত, আর ধীর লাইনে ক্লাউডের
            // উত্তর আসতে কয়েক মিনিট লাগত — তাই *"এন্ট্রি করলাম, সঙ্গে সঙ্গে
            // দেখাল না, অনেক পরে এল"*।
            //
            // এখন ঠিক **একই ঘরগুলো** ফোনের সারিতেও বসে যায়। ⛔ নতুন কোনো সারি
            // তৈরি হয় না — ওই আইডির সারি ফোনে না থাকলে কিছুই হয় না (তখন আগের
            // মতোই ক্লাউড থেকে আসবে)। ⛔ কোনো নতুন ক্লাউড-কল নেই।
            try { LocalWorkflowStore(context).updateLocalFollowUp(id, fields) } catch (_: Throwable) { }
        }

        // (2) enquiries row — move branch + reactivate.
        // ⛔ V534: enquiries-এ রোগী চেনার ঘর নেই — নম্বর ভাগ করা থাকলে ছোঁয়া হয় না।
        val enqs = if (sharedNumber) JSONArray()
            else SupabaseClient.findByMobile("enquiries", normalized, "id,stage", 50)
        for (i in 0 until enqs.length()) {
            val erow = enqs.getJSONObject(i)
            val eid = erow.optString("id")
            if (eid.isBlank()) continue
            val f = JSONObject().put("branch", newBranch).put("status", "Active").put("updatedAt", now)
            val oldStage = erow.s("stage").trim().lowercase()
            if (oldStage in setOf("cancelled", "rejected", "closed")) f.put("stage", "Inquiry")
            if (newDisease.isNotBlank()) f.put("disease", newDisease)
            if (SupabaseClient.updateById("enquiries", eid, f)) ok = true
            // 🔒 খাতার সারি B80 — উপরের একই কারণ: ফোনের নিজের এনকোয়ারি সারিটাও
            // সঙ্গে সঙ্গে ঠিক হয়ে যায়, নইলে Draft-এ পুরনো ব্রাঞ্চ/অবস্থা
            // দেখাত। ⛔ সারিটা ফোনে না থাকলে কিছুই তৈরি হয় না।
            try {
                val store = LocalWorkflowStore(context)
                val localEnq = store.findEnquiryByMobile(normalized)
                if (localEnq != null && localEnq.optString("id") == eid) {
                    val keys = f.keys()
                    while (keys.hasNext()) { val k = keys.next(); localEnq.put(k, f.opt(k)) }
                    store.upsertEnquiry(localEnq, "SYNCED")
                }
            } catch (_: Throwable) { }
        }

        // (3) patients row — move branch for display only (payments untouched).
        val pats = SupabaseClient.findByMobile("patients", normalized, "id,patientId", 50)
        for (i in 0 until pats.length()) {
            val prow = pats.getJSONObject(i)
            val pid = prow.optString("id")
            if (pid.isBlank()) continue
            // 🔵 V534: নম্বর ভাগ করা হলে শুধু প্রমাণসহ এই রোগীর সারিটাই।
            if (sharedNumber && !PatientIdentity.rowBelongsTo(prow, preferRowId, preferPatientCode)) continue
            if (SupabaseClient.updateById("patients", pid, JSONObject().put("branch", newBranch).put("updatedAt", now))) ok = true
        }

        return ok
    }

    /** Saves the enquiry + its matching follow-up row. Returns true if it
     * reached Supabase immediately; false means it was queued locally and
     * will be retried by flushPending() on the next opportunity (the caller
     * should still show a "Saved (will sync when online)" message, not an
     * error, in that case -- see EnquiryActivity). */
    fun save(draft: EnquiryDraft, createdByMobile: String, staffName: String): Boolean {
        val enquiryRow = EnquiryModel.buildEnquiryRow(draft, createdByMobile)
        val followUpRow = EnquiryModel.buildFollowUpRow(enquiryRow, staffName)

        // OWNER-LOCK: save locally first and return immediately.
        LocalWorkflowStore(context).upsertFollowUp(followUpRow)
        // TK-REQUESTED ADDITION (2026-07-16): also cache the enquiry row
        // itself locally (same pattern as followUpRow above) so Draft /
        // Global Search / Full Journey can show it immediately too.
        LocalWorkflowStore(context).upsertEnquiry(enquiryRow)
        // TK-REPORTED (2026-07-27): "কখনো এনকোয়ারি করলে অন্য স্টাফ দেখতে পায় না।"
        // Same reasoning as Registration. These two rows leave the phone in
        // the order queued here, and the Enquiry tab is built from the
        // FOLLOWUPS row, so that one now goes first. If the line dies
        // half-way, the enquiry is already visible to every other staff
        // member, and the second row follows on the next retry.
        // Same two rows, same contents -- only which one leaves first.
        // TK-REQUESTED (2026-07-27), "এক রোগী = এক রেকর্ড" ধাপ ১-এর শেষ অংশ:
        // both rows belong to ONE enquiry, so they carry one shared group tag.
        // flushPending() uses it for a single purpose: if this enquiry is
        // deleted while part of it is still waiting here, BOTH rows are
        // dropped together instead of leaving a card in the cloud with no
        // enquiry behind it. Same two rows, same contents, same order.
        val batchId = "enq_" + java.util.UUID.randomUUID().toString().replace("-", "")
        queuePending("followups", followUpRow, batchId)
        queuePending("enquiries", enquiryRow, batchId)

        // One background attempt; no repeated write loop. Failed rows stay pending.
        Thread {
            try {
                flushPending()
                if (pendingCount() == 0) {
                    LocalWorkflowStore(context).upsertFollowUp(followUpRow, "SYNCED")
                    LocalWorkflowStore(context).upsertEnquiry(enquiryRow, "SYNCED")
                }
            } catch (_: Throwable) { }
        }.start()
        return true
    }

    /** Retries every locally-queued row (from a previous offline save).
     * Safe to call often (e.g. on Dashboard open) -- does nothing if the
     * queue is empty. */
    fun flushPending() {
        synchronized(LOCK) {
        val queue = loadPendingQueue()
        if (queue.length() == 0) return
        // TK-REQUESTED (2026-07-27): work out first which enquiry groups were
        // deleted in the meantime -- if either row of a group was deleted,
        // neither row may be pushed. Rows queued by an older version carry no
        // group tag and behave exactly as before.
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
        // আগে প্রতিটা সফল সারির জন্য আলাদা করে upsertEnquiry/upsertFollowUp
        // ডাকা হতো -- N সারি মানে N বার পুরো টেবিল পড়া+লেখা। এখন জমিয়ে
        // একবারেই batched upsert।
        val syncedEnquiries = ArrayList<JSONObject>()
        val syncedFollowUps = ArrayList<JSONObject>()
        for (i in 0 until queue.length()) {
            val entry = queue.getJSONObject(i)
            val table = entry.getString("table")
            val row = entry.getJSONObject("row")
            val batch = entry.optString("batch", "")
            if (batch.isNotBlank() && cancelledBatches.contains(batch)) continue
            // TK-REQUESTED (2026-07-26): a row deleted in the meantime must not
            // be pushed back into the cloud by this retry. Dropped from the
            // queue; every other row is handled exactly as before.
            if (DeletedGuard.isDeleted(table, row.optString("id", ""), context)) continue
            if (SupabaseClient.upsert(table, row)) {
                // TK-REPORTED BUG FIX (2026-07-16): this used to leave the
                // local cache row marked "_syncStatus=PENDING" forever even
                // after this retry succeeded -- LocalWorkflowStore's stale-
                // cloud-refresh guard (see LocalWorkflowStore.kt) then
                // treats this device as still having an un-synced change
                // for this exact record, so a LATER genuine update to the
                // same record (made by someone else, refreshed from the
                // cloud) could be wrongly rejected as "stale" on this one
                // device, forever, for this one record. Now confirmed
                // synced immediately after this retry succeeds, the same
                // way the very first successful save already does.
                if (table == "enquiries") syncedEnquiries.add(row)
                else if (table == "followups") syncedFollowUps.add(row)
            } else {
                stillPending.put(entry)
            }
        }
        if (syncedEnquiries.isNotEmpty() || syncedFollowUps.isNotEmpty()) {
            val store = LocalWorkflowStore(context)
            store.upsertEnquiries(syncedEnquiries, "SYNCED")
            store.upsertFollowUps(syncedFollowUps, "SYNCED")
        }
        savePendingQueue(stillPending)
        }
    }

    fun pendingCount(): Int = loadPendingQueue().length()

    // TK-FOUND RISK FIX (2026-07-18): if an Enquiry is deleted (Draft's
    // same-day Delete) before it ever synced to the cloud, it's still
    // sitting in THIS queue -- without removing it here too, the next
    // BottomNav retry would re-upload the very row that was just deleted.
    fun removePendingById(id: String) {
        if (id.isBlank()) return
        synchronized(LOCK) {
        val queue = loadPendingQueue()
        val kept = JSONArray()
        for (i in 0 until queue.length()) {
            val e = queue.getJSONObject(i)
            if (e.optJSONObject("row")?.optString("id") != id) kept.put(e)
        }
        savePendingQueue(kept)
        }
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
