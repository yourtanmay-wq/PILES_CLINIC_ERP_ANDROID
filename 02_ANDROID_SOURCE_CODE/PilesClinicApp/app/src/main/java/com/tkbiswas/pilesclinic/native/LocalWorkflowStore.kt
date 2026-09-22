package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * OWNER-LOCKED local-first workflow cache.
 *
 * Purpose only:
 *  Enquiry save -> Enquiry tab immediately
 *  Registration (registration fee is the same action) -> Visit tab immediately
 *  First Advance -> Patient tab immediately
 *
 * Supabase remains the cloud copy. This cache never changes any business rule;
 * it only guarantees that the UI moves locally before network sync finishes.
 */
class LocalWorkflowStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "piles_clinic_local_workflow_v1", Context.MODE_PRIVATE
    )

    companion object {
        // TK-REQUESTED FIX (2026-07-19): every @Synchronized method below used
        // to lock on `this` -- but a NEW LocalWorkflowStore(context) instance is
        // created on almost every call site, so two different instances writing
        // to the SAME underlying SharedPreferences file at the same moment (e.g.
        // BottomNav's background retry running while a fresh save is happening)
        // were never actually blocked by each other -- a classic lost-update race
        // (read old list, modify, write back -- the second writer silently wipes
        // out the first writer's change). All read-modify-write methods now lock
        // on this single shared object instead, so they are truly serialized
        // across every instance/thread in the app.
        private val LOCK = Any()

        /** The exact text last written for each list, shared by every instance
         *  in this app. See load()/save() below for why it exists. */
        private val snapshot = java.util.concurrent.ConcurrentHashMap<String, String>()
    }

    /** ROOT-CAUSE FIX (2026-07-15): this used to fully REPLACE the old row with
     *  whatever the new one had, so if anything ever called this with a row
     *  missing a field (e.g. "branch"), that field silently got wiped even
     *  though it was correct before — this is how a patient's branch could
     *  end up blank without branch ever being skippable at entry. Now it
     *  MERGES field-by-field (same safe pattern as updateLocalFollowUp below),
     *  so an existing field is only ever changed if the new data actually has it. */
    /**
     * 🔵🔒 V520 (২২.০৮.২০২৬, TK-অনুমোদিত — **offline**) — **দুই রোগীর সারি
     * কখনো এক করে ফেলা যাবে না।**
     *
     * নিচের দুটো `upsertFollowUp*()` পুরোনো সারি খুঁজে পায় দু'ভাবে: আইডি মিললে,
     * **অথবা** "একই যাত্রা" — মানে *একই মোবাইল + একই stage*। এক নম্বরে স্বামী ও
     * স্ত্রী দুজন আলাদা রোগী থাকলে দুজনেরই stage "Patient" হতে পারে, তখন
     * দ্বিতীয়জনের সারি প্রথমজনের সারির উপরেই বসে যেত — **দুজনের রেকর্ড এক
     * হয়ে যেত** (TK-এর ৭ নম্বর নিয়মের সরাসরি লঙ্ঘন)।
     *
     * এই ফাংশন **কেবল তখনই** `true` বলে, যখন কোড থেকে **প্রমাণ** করা যায় যে
     * সারি দুটো আলাদা রোগীর:
     *   ১. দুটো সারিতেই `refId` আছে এবং **আলাদা** → নিশ্চিতভাবে দুই রোগী।
     *   ২. একটায় `refId` আছে ও সেটা *"Different Patient — Same Mobile"*-এর
     *      চিহ্নবহ (`pat_<১০ সংখ্যা>_<লেজ>`), অন্যটায় `refId` **নেই** →
     *      যার `refId` নেই সেটা পুরোনো সাধারণ সারি, তাই দুজন আলাদা।
     *      (কারণ ঘোষিত-আলাদা রোগীর সারি **সবসময়** `refId` নিয়েই তৈরি হয় —
     *      `PatientModel.buildVisitFollowUpRow` এক অক্ষরও ছাড়ে না।)
     *
     * ⛔ এর বাইরে সব ক্ষেত্রে `false` — অর্থাৎ **আচরণ হুবহু আগের মতোই**।
     *    পুরোনো (refId-হীন) সারিগুলোর মিলে-যাওয়া একটুও বদলায়নি।
     */
    private fun provablyDifferentPatients(old: JSONObject, neu: JSONObject, mobile: String): Boolean {
        val a = old.optString("refId")
        val b = neu.optString("refId")
        if (a.isNotBlank() && b.isNotBlank()) return a != b
        val known = if (a.isNotBlank()) a else b
        return known.isNotBlank() && PatientModel.isDeclaredSeparateRowId(known, mobile)
    }

    fun upsertFollowUp(row: JSONObject, syncStatus: String = "PENDING") {
        synchronized(LOCK) {
        val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
        val rows = load("followups")
        val id = copy.optString("id")
        val mobile = digits(copy.optString("mobile"))
        val stage = copy.optString("stage")
        var replaced = false
        for (i in 0 until rows.length()) {
            val old = rows.getJSONObject(i)
            val sameId = id.isNotBlank() && old.optString("id") == id
            val sameJourney = mobile.isNotBlank() && digits(old.optString("mobile")) == mobile &&
                old.optString("stage") == stage && !provablyDifferentPatients(old, copy, mobile)
            if (sameId || sameJourney) {
                // STALE-CLOUD-OVERWRITE GUARD (TK-reported bug, 2026-07-16): a
                // routine cloud refresh (syncStatus="SYNCED", e.g. just opening a
                // Follow-up tab) used to blindly overwrite a row that still had a
                // newer, not-yet-uploaded local change (e.g. Advance payment just
                // promoted Visit -> Patient card) with the OLDER cloud data --
                // silently undoing the promotion and stamping it "SYNCED" so it
                // could never be re-sent as pending. Root cause of "card
                // disappears / doesn't move right after Save or Advance". Fix:
                // a "SYNCED" refresh never overwrites a row that is still
                // "PENDING" locally with an equal-or-newer updatedAt.
                if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                val keys = copy.keys()
                while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                rows.put(i, old); replaced = true; break
            }
        }
        if (!replaced) rows.put(copy)
        save("followups", rows)
            }
    }


    /**
     * 🚨 TK-REPORTED, চালু ক্লিনিক (28.07.2026, খাতার সারি B27): স্টাফদের ফোনে
     * Follow-up পর্দায় **"TK Biswas Piles Clinic isn't responding"** আসছিল।
     *
     * আসল কারণ (কোড দেখে, আন্দাজ নয়): ক্লাউড থেকে তালিকা আসার পরে প্রতিটা সারি
     * **এক এক করে** এখানে লেখা হত। প্রতিটা সারির জন্য আলাদা করে —
     *   (১) তালা নেওয়া, (২) পুরো জমানো তালিকাটা পড়া, (৩) আবার পুরোটা লেখা,
     *   (৪) ডিস্কে লেখা শেষ হওয়া পর্যন্ত অপেক্ষা।
     * ৪৭টা সারি মানে এই ভারী কাজটা ৪৭ বার, আর পুরোটা সময় **তালাটা আটকে থাকত**।
     * ঠিক তখনই পর্দা (মেইন থ্রেড) ওই একই তালা চাইত জমানো তালিকা দেখানোর জন্য —
     * তাই পর্দা কয়েক সেকেন্ড আটকে যেত আর Android "isn't responding" দেখাত।
     *
     * এখন: **একবার তালা · একবার পড়া · একবার লেখা** — সারি যতগুলোই হোক।
     * ⛔ প্রতিটা সারির সিদ্ধান্তের নিয়ম (মেলানো · পুরনো-ক্লাউড আটকানো · ঘর-ধরে-ঘর
     * মেশানো) `upsertFollowUp`-এর সঙ্গে **হুবহু এক** — এক অক্ষরও বদলানো হয়নি,
     * তাই তালিকায় যা দেখাত ঠিক তাই দেখাবে, শুধু পর্দা আর আটকাবে না।
     */
    fun upsertFollowUps(rows: List<JSONObject>, syncStatus: String = "PENDING") {
        if (rows.isEmpty()) return
        synchronized(LOCK) {
            val stored = load("followups")
            for (row in rows) {
                val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
                val id = copy.optString("id")
                val mobile = digits(copy.optString("mobile"))
                val stage = copy.optString("stage")
                var replaced = false
                for (i in 0 until stored.length()) {
                    val old = stored.getJSONObject(i)
                    val sameId = id.isNotBlank() && old.optString("id") == id
                    val sameJourney = mobile.isNotBlank() && digits(old.optString("mobile")) == mobile &&
                        old.optString("stage") == stage && !provablyDifferentPatients(old, copy, mobile)
                    if (sameId || sameJourney) {
                        if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                        val keys = copy.keys()
                        while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                        stored.put(i, old); replaced = true; break
                    }
                }
                if (!replaced) stored.put(copy)
            }
            save("followups", stored)
        }
    }


    /** True if `incoming` is a plain cloud refresh ("SYNCED") that would
     *  clobber a row still awaiting upload ("PENDING") with data that is not
     *  newer than what's already cached. Shared guard used by every upsert*
     *  below so a background refresh can never quietly undo a just-made
     *  local change (see upsertFollowUp above for the full story). */
    private fun isStaleCloudRefresh(old: JSONObject, incomingSyncStatus: String, incoming: JSONObject): Boolean {
        if (incomingSyncStatus != "SYNCED") return false
        if (old.optString("_syncStatus") != "PENDING") return false
        return old.optString("updatedAt") > incoming.optString("updatedAt")
    }

    // TK-REQUESTED ADDITION (2026-07-16): local cache for "patients" rows,
    // same safe merge-by-id pattern as upsertFollowUp above. Added so Doctor
    // Queue (and anything else reading "patients") can show a just-saved
    // patient immediately, the same way Follow-up already does for
    // enquiries/visits/treatments. Nothing about upsertFollowUp/rowsForStage
    // above is touched.
    fun upsertPatient(row: JSONObject, syncStatus: String = "PENDING") {
        synchronized(LOCK) {
        val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
        val rows = load("patients")
        val id = copy.optString("id")
        var replaced = false
        for (i in 0 until rows.length()) {
            val old = rows.getJSONObject(i)
            if (id.isNotBlank() && old.optString("id") == id) {
                if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                val keys = copy.keys()
                while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                rows.put(i, old); replaced = true; break
            }
        }
        if (!replaced) rows.put(copy)
        save("patients", rows)
            }
    }


    /** V1320 (তালিকা ৪২৪, একই দোষ আরেক জায়গায় — নিয়ম ৭): batched সংস্করণ,
     *  ঠিক upsertFollowUps-এর প্যাটার্নে। RegistrationRepository.flushPending()
     *  আগে প্রতিটা সফল-sync হওয়া সারির জন্য আলাদা করে upsertPatient() ডাকত —
     *  N সারি মানে N বার পুরো টেবিল পড়া+লেখা। এখন একবারেই। */
    fun upsertPatients(rows: List<JSONObject>, syncStatus: String = "PENDING") {
        if (rows.isEmpty()) return
        synchronized(LOCK) {
            val stored = load("patients")
            for (row in rows) {
                val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
                val id = copy.optString("id")
                var replaced = false
                for (i in 0 until stored.length()) {
                    val old = stored.getJSONObject(i)
                    if (id.isNotBlank() && old.optString("id") == id) {
                        if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                        val keys = copy.keys()
                        while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                        stored.put(i, old); replaced = true; break
                    }
                }
                if (!replaced) stored.put(copy)
            }
            save("patients", stored)
        }
    }


    /** Every locally-cached "patients" row not yet confirmed synced. */
    fun pendingPatients(): JSONArray {
        val out = JSONArray()
        val rows = load("patients")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("_syncStatus") == "PENDING") out.put(JSONObject(row.toString()))
        }
        return out
    }


    // TK-REQUESTED ADDITION (2026-07-16): same pattern again, this time for
    // "payments" rows, so Today's Collection can show a just-taken payment
    // immediately (see PaymentRepository.saveTreatmentPayment).
    fun upsertPayment(row: JSONObject, syncStatus: String = "PENDING") {
        synchronized(LOCK) {
        val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
        val rows = load("payments")
        val id = copy.optString("id")
        var replaced = false
        for (i in 0 until rows.length()) {
            val old = rows.getJSONObject(i)
            if (id.isNotBlank() && old.optString("id") == id) {
                if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                val keys = copy.keys()
                while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                rows.put(i, old); replaced = true; break
            }
        }
        if (!replaced) rows.put(copy)
        save("payments", rows)
            }
    }


    /** V1320 (তালিকা ৪২৪, নিয়ম ৭) — উপরের upsertPatients-এর মতোই, "payments"-এর
     *  জন্য batched সংস্করণ। */
    fun upsertPayments(rows: List<JSONObject>, syncStatus: String = "PENDING") {
        if (rows.isEmpty()) return
        synchronized(LOCK) {
            val stored = load("payments")
            for (row in rows) {
                val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
                val id = copy.optString("id")
                var replaced = false
                for (i in 0 until stored.length()) {
                    val old = stored.getJSONObject(i)
                    if (id.isNotBlank() && old.optString("id") == id) {
                        if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                        val keys = copy.keys()
                        while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                        stored.put(i, old); replaced = true; break
                    }
                }
                if (!replaced) stored.put(copy)
            }
            save("payments", stored)
        }
    }


    // TK-FOUND RISK FIX (2026-07-18): Chamber Attendance's "Undo Arrived"
    // only ever deleted the CLOUD payment row -- if that mark hadn't synced
    // yet (still local-only, offline), Undo would fail AND the mark would
    // keep reappearing on this device (still cached locally as pending).
    // This removes it from the local cache too, so Undo is complete either
    // way.
    fun removePayment(id: String) {
        synchronized(LOCK) {
        if (id.isBlank()) return
        val rows = load("payments")
        val kept = JSONArray()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("id") != id) kept.put(row)
        }
        save("payments", kept)
            }
    }


    /** Every locally-cached "payments" row not yet confirmed synced. */
    fun pendingPayments(): JSONArray {
        val out = JSONArray()
        val rows = load("payments")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("_syncStatus") == "PENDING") out.put(JSONObject(row.toString()))
        }
        return out
    }


    // TK-REQUESTED ADDITION (2026-07-16): same pattern once more, for
    // "enquiries" rows, completing coverage for Draft / Global Search /
    // Full Journey (see EnquiryRepository.save()).
    fun upsertEnquiry(row: JSONObject, syncStatus: String = "PENDING") {
        synchronized(LOCK) {
        val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
        val rows = load("enquiries")
        val id = copy.optString("id")
        var replaced = false
        for (i in 0 until rows.length()) {
            val old = rows.getJSONObject(i)
            if (id.isNotBlank() && old.optString("id") == id) {
                if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                val keys = copy.keys()
                while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                rows.put(i, old); replaced = true; break
            }
        }
        if (!replaced) rows.put(copy)
        save("enquiries", rows)
            }
    }


    /** V1320 (তালিকা ৪২৪, নিয়ম ৭) — উপরের upsertPatients-এর মতোই, "enquiries"-এর
     *  জন্য batched সংস্করণ। EnquiryRepository.flushPending()-এ ব্যবহৃত। */
    fun upsertEnquiries(rows: List<JSONObject>, syncStatus: String = "PENDING") {
        if (rows.isEmpty()) return
        synchronized(LOCK) {
            val stored = load("enquiries")
            for (row in rows) {
                val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
                val id = copy.optString("id")
                var replaced = false
                for (i in 0 until stored.length()) {
                    val old = stored.getJSONObject(i)
                    if (id.isNotBlank() && old.optString("id") == id) {
                        if (isStaleCloudRefresh(old, syncStatus, copy)) { replaced = true; break }
                        val keys = copy.keys()
                        while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                        stored.put(i, old); replaced = true; break
                    }
                }
                if (!replaced) stored.put(copy)
            }
            save("enquiries", stored)
        }
    }


    // TK-FOUND RISK FIX (2026-07-18): same class of gap as removePayment
    // above -- deleting an Enquiry (Draft's same-day Delete) only ever
    // removed the CLOUD row. If that Enquiry hadn't synced to the cloud
    // yet (still local-only), the delete would find nothing to remove and
    // the entry would keep reappearing on this device. This clears the
    // local cache copy too.
    fun removeEnquiry(id: String) {
        synchronized(LOCK) {
        if (id.isBlank()) return
        val rows = load("enquiries")
        val kept = JSONArray()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("id") != id) kept.put(row)
        }
        save("enquiries", kept)
            }
    }


    /** Every locally-cached "enquiries" row not yet confirmed synced. */
    fun pendingEnquiries(): JSONArray {
        val out = JSONArray()
        val rows = load("enquiries")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("_syncStatus") == "PENDING") out.put(JSONObject(row.toString()))
        }
        return out
    }


    // TK-FOUND RISK FIX (2026-07-18): Doctor Checkup / Prescription / Diet
    // Chart / Investigation Advice (ClinicalCloudRepository.saveMedical)
    // had ZERO offline protection -- a single direct network call, no
    // local cache, no retry. A weak connection meant the clinical record
    // was just silently lost. Same proven local-first + retry-queue
    // pattern as everything else in this file.
    fun upsertMedical(row: JSONObject, syncStatus: String = "PENDING") {
        synchronized(LOCK) {
        val copy = JSONObject(row.toString()).put("_syncStatus", syncStatus)
        val rows = load("medical")
        val id = copy.optString("id")
        var replaced = false
        for (i in 0 until rows.length()) {
            val old = rows.getJSONObject(i)
            if (id.isNotBlank() && old.optString("id") == id) {
                val keys = copy.keys()
                while (keys.hasNext()) { val k = keys.next(); old.put(k, copy.opt(k)) }
                rows.put(i, old); replaced = true; break
            }
        }
        if (!replaced) rows.put(copy)
        save("medical", rows)
            }
    }


    fun pendingMedical(): JSONArray {
        val out = JSONArray()
        val rows = load("medical")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("_syncStatus") == "PENDING") out.put(JSONObject(row.toString()))
        }
        return out
    }


    fun medicalForPatient(patientId: String): JSONArray {
        val out = JSONArray()
        val rows = load("medical")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("patientId") == patientId) out.put(JSONObject(row.toString()))
        }
        return out
    }


    /**
     * 🚨 TK-REPORTED, চালু ক্লিনিক (28.07.2026, খাতার সারি B27): পর্দাটা (মেইন
     * থ্রেড) জমানো তালিকা দেখানোর জন্য **এই** ফাংশনটাই ডাকে। আগে এটা ওই একই
     * তালা চাইত যেটা পিছনের কাজ ধরে রাখত — তাই পর্দা কয়েক সেকেন্ড আটকে যেত
     * আর Android "isn't responding" দেখাত।
     *
     * এখানে তালার দরকারই নেই: এই ফাংশন **কিছুই বদলায় না**, শুধু পড়ে। যে লেখা
     * থেকে পড়া হয় সেটা একটা অবিকৃত টেক্সট (load দেখুন), আর প্রতিটা সারির
     * নতুন কপি বানিয়ে ফেরত দেওয়া হয়। তাই দুটো কাজ একসাথে চললেও কিছু ভাঙে না।
     * ⛔ ছাঁকনির নিয়ম (stage মেলানো · Cancelled/Incomplete/Closed বাদ) এক
     * অক্ষরও বদলানো হয়নি — তালিকায় ঠিক আগের জিনিসই আসবে।
     */
    /**
     * 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B34): *"আমি নিজে
     * ডিলিট করে দিয়েছিলাম, তারপরে এখন সে কীভাবে চলে আসল?"*
     *
     * **আসল কারণ:** মুছে ফেললে রেকর্ডটা **ক্লাউড থেকে** যেত, কিন্তু **ফোনের
     * নিজের জমানো তালিকা থেকে যেত না**। আর নিয়ম হলো "এই ফোনে যা সেভ হয়েছে তা
     * সব সময় দেখাবে" — তাই মুছে ফেলা রেকর্ডটা ফোনের তালিকা থেকে **প্রতিবার
     * ফিরে আসত**, আর পুরনো/আধা তথ্য নিয়ে আসত বলে রোগের নামও ফাঁকা দেখাত।
     *
     * এই দুটো ফাংশন মুছে ফেলার সময় ফোনের কপিটাও পরিষ্কার করে।
     */
    /** 🔴🔒 V1311 (তালিকা ৪২৩, রুল ৭): ক্লাউড থেকে আসা সারি ফোনের **PENDING** কপির সমান বা নতুন
     *  (updatedAt) হলে কপিটা SYNCED হয়ে যায় — "চিরকাল PENDING" আর হয় না, তাই তালিকাগুলোয়
     *  আধখানা/পুরনো কপি ঢুকে পড়ার পথ বন্ধ। শুধু একই id · দুটোতেই updatedAt আছে · ক্লাউড >= ফোন —
     *  তবেই। ক্লাউড পুরনো হলে (ফোনের লেখা এখনো ওঠেনি) কিছুই ছোঁয় না — sync-এর কাজ sync করে। */
    fun markSyncedWhereCloudCaughtUp(table: String, cloudRows: JSONArray?) {
        if (cloudRows == null || cloudRows.length() == 0) return
        val list = ArrayList<JSONObject>(cloudRows.length())
        for (i in 0 until cloudRows.length()) cloudRows.optJSONObject(i)?.let { list.add(it) }
        markSyncedWhereCloudCaughtUp(table, list)
    }

    fun markSyncedWhereCloudCaughtUp(table: String, cloudRows: List<JSONObject>) {
        if (table !in listOf("patients", "payments", "followups", "enquiries", "medical")) return
        if (cloudRows.isEmpty()) return
        try {
            val cloudStamp = HashMap<String, String>()
            for (r in cloudRows) {
                val id = r.optString("id"); val u = r.optString("updatedAt")
                if (id.isNotBlank() && u.isNotBlank()) cloudStamp[id] = u
            }
            if (cloudStamp.isEmpty()) return
            synchronized(LOCK) {
                val rows = load(table)
                var changed = false
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    if (row.optString("_syncStatus") != "PENDING") continue
                    val c = cloudStamp[row.optString("id")] ?: continue
                    val l = row.optString("updatedAt")
                    if (l.isNotBlank() && c >= l) { row.put("_syncStatus", "SYNCED"); rows.put(i, row); changed = true }
                }
                if (changed) save(table, rows)
            }
        } catch (_: Throwable) { }
    }

    /* 🔴🔒 V1587 (১৭.০৯.২০২৬, TK-রিপোর্ট — COB-UTTAMA: "আমার এনকোয়ারিতে
       ওভারডিউ ছিল না, আজ কেন দেখাচ্ছে") — একবার-নিশ্চিত-সেভ-হওয়া সারি কখনো
       হারায় না, এই নিয়মটাই (উপরের `markSyncedWhereCloudCaughtUp()`-এর
       "PENDING হলে সবসময় দেখাও" নিয়ম) কোনো ভাবেই বদলানো হয়নি — সেটা টাকা/
       রোগীর তথ্য হারানো ঠেকানোর জন্যই বসানো, ছোঁয়া ঝুঁকিপূর্ণ।

       **আসল ফাঁকটা:** `markSyncedWhereCloudCaughtUp()` একটা ট্যাবের
       নিজের-ধাপের তাজা ক্লাউড-ফলের সাথেই মেলায়। কোনো সারির ধাপ বদলে গেলে
       (যেমন Inquiry → Registered, পরে পেশেন্ট হয়ে যাওয়ায়) সেটা আর কোনো
       ট্যাবের ওই ধাপের ফলেই দেখা যায় না — তাই এই তুলনা কখনো মেলে না,
       সারিটা "PENDING" থেকে যায়, আর পুরনো রিমার্ক/তারিখ নিয়ে চিরকাল
       ফিরে আসতে থাকে (Enquiry ট্যাবে "ওভারডিউ" হয়ে), যদিও আসল রোগী/টাকার
       তথ্যে কোনো ক্ষতি নেই।

       **এই ফাংশন যা করে:** একটামাত্র সারির জন্য, `id` ধরে সরাসরি — cloud-এর
       `updatedAt` local-এর সমান/নতুন হলে তবেই SYNCED করা হয় (উপরের ফাংশনের
       হুবহু একই তুলনা, শুধু উৎস আলাদা — একগুচ্ছ তাজা সারির বদলে একটা
       নির্দিষ্ট id-র সরাসরি উত্তর)। ⛔ কখনো সারি মোছে না, কখনো ঘর বদলায়
       না — শুধু `_syncStatus` পতাকাটা। ব্যর্থ/না-মেলা হলে কিচ্ছু বদলায় না,
       সারিটা আগের মতোই "PENDING" থেকে যায় (নিরাপদ দিক)। */
    fun markRowSyncedIfCloudCaughtUp(table: String, id: String, cloudUpdatedAt: String) {
        if (table !in listOf("patients", "payments", "followups", "enquiries", "medical")) return
        if (id.isBlank() || cloudUpdatedAt.isBlank()) return
        try {
            synchronized(LOCK) {
                val rows = load(table)
                var changed = false
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    if (row.optString("id") != id) continue
                    if (row.optString("_syncStatus") != "PENDING") continue
                    val l = row.optString("updatedAt")
                    if (l.isNotBlank() && cloudUpdatedAt >= l) { row.put("_syncStatus", "SYNCED"); rows.put(i, row); changed = true }
                    break
                }
                if (changed) save(table, rows)
            }
        } catch (_: Throwable) { }
    }

    fun forgetRecord(table: String, id: String) {
        if (table.isBlank() || id.isBlank()) return
        synchronized(LOCK) {
            val rows = load(table)
            val kept = JSONArray()
            var dropped = false
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (row.optString("id") == id) { dropped = true; continue }
                kept.put(row)
            }
            if (dropped) save(table, kept)
        }
    }

    /** 🔴🔴🔒 V1319 (১১.০৯.২০২৬, তালিকা ৪২৪ — আসল কারণ): V1305-এ `mergeOwnPhoneRows()`-এর
     *  একটা লুপে **প্রতিটা পুরনো সারির জন্য আলাদা করে** `forgetRecord()` ডাকা হত — মানে
     *  পুরো জমানো টেবিল (হাজার হাজার সারি হতে পারে, মাসের পর মাস আসল ব্যবহারে) **প্রতিটা
     *  পুরনো সারির জন্য আলাদা করে** আবার পড়া-ছাঁকা-লেখা হত। ২০-৩০টা পুরনো সারি থাকলেই
     *  এটা তত বারই পুরো টেবিল ছুঁয়ে ফেলত — ঠিক এই এলোমেলো ধীরগতি/আটকে যাওয়ার কারণ,
     *  আর V1305-এর আগে এই লুপই ছিল না বলে সমস্যাটা আজই প্রথম তৈরি হয়েছে (TK ঠিক ধরেছেন)।
     *  **সমাধান:** একসাথে অনেক id ভুলে যাওয়ার জন্য — টেবিল **একবারই** পড়া-ছাঁকা-লেখা হয়। */
    fun forgetRecords(table: String, ids: Collection<String>) {
        if (table.isBlank() || ids.isEmpty()) return
        val idSet = ids.filter { it.isNotBlank() }.toHashSet()
        if (idSet.isEmpty()) return
        synchronized(LOCK) {
            val rows = load(table)
            val kept = JSONArray()
            var dropped = false
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (idSet.contains(row.optString("id"))) { dropped = true; continue }
                kept.put(row)
            }
            if (dropped) save(table, kept)
        }
    }

    /** মুছে ফেলার সঙ্গে যে Follow-up সারিগুলো লুকিয়ে দেওয়া হয়, ফোনের কপিতেও
     *  সেগুলো একই ভাবে লুকিয়ে দেয় — নইলে কার্ডটা তালিকায় থেকে যেত। */
    fun cancelFollowUpsLocally(ids: Collection<String>) {
        if (ids.isEmpty()) return
        synchronized(LOCK) {
            val rows = load("followups")
            var changed = false
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (ids.contains(row.optString("id"))) {
                    row.put("status", "Cancelled")
                    rows.put(i, row); changed = true
                }
            }
            if (changed) save("followups", rows)
        }
    }


    fun rowsForStage(stage: String): JSONArray {
        val out = JSONArray()
        val rows = load("followups")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val statusOpen = row.optString("status", "Active") !in listOf("Cancelled", "Incomplete", "Rejected", "Closed")
            val legacyHistoryClosed = stage == "Inquiry" && FollowUpRepository.inquiryHistoryEndsTerminal(row)
            if (row.optString("stage") == stage && statusOpen && !legacyHistoryClosed) {
                out.put(JSONObject(row.toString()))
            }
        }
        return out
    }

    /** TK-REPORTED BUG FIX (2026-07-22): the opposite of rowsForStage -- local
     *  "followups" rows for a stage that ARE Cancelled/Incomplete. Used only to
     *  let the Visit/Patient-tab patients-table fallback safety nets know a
     *  Reject/Incomplete action immediately, even before it has finished
     *  syncing to the cloud (previously those fallbacks only checked the
     *  cloud, so a just-rejected patient could be resurrected until the sync
     *  caught up). Read-only, no existing behaviour changed. */
    fun rejectedRowsForStage(stage: String): JSONArray {
        val out = JSONArray()
        val rows = load("followups")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val terminalStatus = row.optString("status", "Active") in listOf("Cancelled", "Incomplete", "Rejected", "Closed")
            val legacyHistoryClosed = stage == "Inquiry" && FollowUpRepository.inquiryHistoryEndsTerminal(row)
            if (row.optString("stage") == stage && (terminalStatus || legacyHistoryClosed)) {
                out.put(JSONObject(row.toString()))
            }
        }
        return out
    }


    /** Every locally-cached "followups" row not yet confirmed synced,
     *  regardless of stage/status -- used by Full Journey, which (unlike
     *  the Follow-up tabs) shows a patient's complete history. */
    fun pendingFollowUps(): JSONArray {
        val out = JSONArray()
        val rows = load("followups")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("_syncStatus") == "PENDING") out.put(JSONObject(row.toString()))
        }
        return out
    }


    fun closeInquiry(mobileRaw: String, patientId: String) {
        synchronized(LOCK) {
        val mobile = digits(mobileRaw)
        val rows = load("followups")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (digits(row.optString("mobile")) == mobile && row.optString("stage") == "Inquiry") {
                row.put("stage", "Registered").put("status", "Closed")
                    .put("convertedPatientId", patientId).put("updatedAt", isoNow())
                    // TK-REPORTED BUG FIX (2026-07-25, found from TK's live
                    // report -- "same number shows in Enquiry/Visit/
                    // Registration at once", worse on slow internet): this
                    // local close used to leave _syncStatus untouched, so
                    // the existing stale-cloud-overwrite guard (which only
                    // protects rows marked "PENDING") never protected it --
                    // a routine cloud refresh (e.g. just opening the
                    // Enquiry tab) BEFORE the real cloud-side close
                    // (closeSourceEnquiry, queued separately) finished
                    // could silently revive this Enquiry back to visible/
                    // Active, exactly the multi-stage-at-once symptom TK
                    // saw -- more likely the slower the network, since that
                    // widens the race window. Marking it "PENDING" here
                    // means the guard now protects it until the real cloud
                    // close actually lands and re-marks it "SYNCED".
                    .put("_syncStatus", "PENDING")
                rows.put(i, row)
            }
        }
        save("followups", rows)
            }
    }


    fun promoteToTreatment(patient: PatientBillInfo, effectiveBill: Double, amount: Double) {
        synchronized(LOCK) {
        val mobile = digits(patient.mobile)
        val rows = load("followups")
        var found = false
        /* 🔵🔒 V520 (২২.০৮.২০২৬, TK-অনুমোদিত — **offline**): নিচের লুপ ওই
           মোবাইলের **সব** Patient/Treatment সারিতে টাকা বসিয়ে দিত। এক নম্বরে
           স্বামী ও স্ত্রী দুজন থাকলে একজনের টাকা **দুজনের নামেই** বসে যেত।
           এখন আগে দেখা হয় — এই রোগীর **নিজের আইডি** ধরে চেনা কোনো সারি আছে
           কিনা (`refId`/`patientId`)। থাকলে **কেবল সেগুলোই** বদলায়।
           ⛔ পুরোনো (আইডি-হীন) সারির ক্ষেত্রে একটাও মিলবে না, তখন আচরণ
              **হুবহু আগের মতোই** — কোনো পুরোনো ফোনের জমা তথ্য ভাঙে না। */
        val ownerId = patient.id
        fun isOwn(row: JSONObject) =
            ownerId.isNotBlank() &&
                (row.optString("refId") == ownerId || row.optString("patientId") == ownerId)
        /* কোড থেকে **প্রমাণ** করা যায় যে সারিটা অন্য রোগীর — কেবল তখনই বাদ। */
        fun isSomeoneElse(row: JSONObject): Boolean {
            if (ownerId.isBlank() || isOwn(row)) return false
            val rid = row.optString("refId")
            if (PatientModel.isDeclaredSeparateRowId(ownerId, mobile)) return true
            return rid.isNotBlank() && PatientModel.isDeclaredSeparateRowId(rid, mobile)
        }
        var ownedFound = false
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            if (digits(row.optString("mobile")) != mobile) continue
            if (row.optString("stage") !in listOf("Patient", "Treatment")) continue
            if (isOwn(row)) { ownedFound = true; break }
        }
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (ownedFound && !isOwn(row)) continue
            if (isSomeoneElse(row)) continue
            if (digits(row.optString("mobile")) == mobile && row.optString("stage") in listOf("Patient", "Treatment")) {
                val priorPaid = row.optDouble("paid", 0.0)
                // TK-REPORTED BUG FIX (2026-07-15): bump "date" on every payment
                // (not just record creation) so this patient sorts to the very
                // top today — same fix as the cloud side in PaymentRepository.
                row.put("stage", "Treatment").put("status", "Active")
                    .put("bill", effectiveBill).put("paid", priorPaid + amount)
                    .put("lastRemark", "Treatment payment / Advance received")
                    .put("date", PatientIdGenerator.todayIso())
                    .put("updatedAt", isoNow()).put("_syncStatus", "PENDING")
                rows.put(i, row); found = true
            }
        }
        if (!found) {
            rows.put(JSONObject()
                .put("id", "local_fu_" + java.util.UUID.randomUUID().toString().replace("-", ""))
                .put("refId", patient.id).put("patientId", patient.id)
                .put("mobile", patient.mobile).put("name", patient.name).put("branch", patient.branch)
                .put("stage", "Treatment").put("status", "Active")
                .put("date", PatientIdGenerator.todayIso())
                .put("bill", effectiveBill).put("paid", amount)
                .put("lastRemark", "Treatment payment / Advance received")
                .put("createdAt", isoNow()).put("updatedAt", isoNow()).put("_syncStatus", "PENDING"))
        }
        save("followups", rows)
            }
    }



    fun findFollowUp(id: String): JSONObject? {
        val rows = load("followups")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("id") == id) return JSONObject(row.toString())
        }
        return null
    }


    /** Same as findFollowUp, but for several ids in ONE pass. Added so a
     *  screen with many rows (the Chamber board) does not read and re-parse
     *  the whole stored list once per row on a slow phone. Returns only the
     *  ids that were actually found. */
    fun findFollowUps(ids: Set<String>): Map<String, JSONObject> {
        val out = HashMap<String, JSONObject>()
        if (ids.isEmpty()) return out
        val rows = load("followups")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val id = row.optString("id")
            if (id.isNotBlank() && ids.contains(id) && !out.containsKey(id)) {
                out[id] = JSONObject(row.toString())
            }
        }
        return out
    }


    fun updateLocalFollowUp(id: String, fields: JSONObject): Boolean {
        synchronized(LOCK) {
        val rows = load("followups")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            if (row.optString("id") == id) {
                val keys = fields.keys()
                while (keys.hasNext()) { val k = keys.next(); row.put(k, fields.opt(k)) }
                row.put("_syncStatus", "PENDING")
                rows.put(i, row); save("followups", rows); return true
            }
        }
        return false
            }
    }


    /**
     * TK'S STANDING RULE (restated 2026-07-27): ONE MOBILE = ONE REGISTRATION.
     * There is no such thing as two people on one number -- staff use a dummy
     * number when a patient has none.
     *
     * The duplicate check before saving a Registration/Enquiry only ever asked
     * the cloud, and SupabaseClient.findByMobile() returns an EMPTY list on any
     * network failure (by design, so a hiccup never blocks a save). On TK's
     * line (often 0.16-2.0 KB/s) that empty answer was read as "this number is
     * new" -- so the same mobile got a SECOND patient row and a SECOND Patient
     * ID. That is one of the real ways one person ends up as two records.
     *
     * These two lookups read the phone's OWN saved list first, which needs no
     * network at all, so a number registered on this phone can never be entered
     * a second time even with the line down. Returns a copy; null when not
     * found (caller then behaves exactly as it did before).
     */
    fun findPatientByMobile(mobileRaw: String): JSONObject? = findByMobileIn("patients", mobileRaw)

    /**
     * 🔵🔒 V520 (২২.০৮.২০২৬, TK-অনুমোদিত — **offline**) — এই ফোনে জমা থাকা
     * ওই নম্বরের **সব** রোগী, শুধু প্রথমজন নয়।
     *
     * **কেন:** নেট না থাকলে ডুপ্লিকেট-চেক ক্লাউডে কিছুই দেখতে পায় না, তখন
     * ফোনের নিজের তালিকাই ভরসা। উপরের ফাংশনটা **প্রথম** মিলটাই ফেরায় — এক
     * নম্বরে স্বামী ও স্ত্রী দুজন থাকলে পপ-আপে একজনই দেখা যেত, আর স্টাফ
     * ভুল জনকে *"Update Existing"* করে ফেলতে পারতেন।
     *
     * ⛔ উপরের `findPatientByMobile()` **এক অক্ষরও বদলায়নি** — তার সব পুরোনো
     *    ডাকার জায়গা হুবহু আগের মতোই চলে। এটা শুধু **অতিরিক্ত** একটা পথ।
     * ⛔ নেট লাগে না · কোনো cloud-read নেই · কিছু লেখা হয় না (read-only)।
     * ⛔ একজন থাকলে তালিকায় একটাই — আচরণ আগের মতোই।
     */
    fun findPatientsByMobile(mobileRaw: String): List<JSONObject> {
        val want = digits(mobileRaw)
        if (want.length != 10) return emptyList()
        val rows = load("patients")
        val out = mutableListOf<JSONObject>()
        val seen = HashSet<String>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            if (digits(row.optString("mobile")) != want) continue
            val rid = row.optString("id")
            if (rid.isNotBlank() && !seen.add(rid)) continue
            out.add(JSONObject(row.toString()))
        }
        return out
    }

    fun findEnquiryByMobile(mobileRaw: String): JSONObject? = findByMobileIn("enquiries", mobileRaw)

    /** 🔒 খাতার সারি B78 (TK, 29.07.2026): উপরেরটার মতোই, কিন্তু `followups`-এ।
     *  দরকার হয় যখন ক্লাউডে ওই নম্বরের সারি নেই কিন্তু **এই ফোনে জমা আছে**
     *  (সেভ হয়েছিল, ক্লাউডে ওঠেনি)। তখন নতুন সারি না বানিয়ে **ফোনের সারিটার
     *  আইডিই** ব্যবহার করতে হয় — নইলে ফোনের তালিকায় পুরনো সারিটা Active থেকে
     *  যেত আর Reject করা কার্ড আবার ফিরে আসত। */
    fun findFollowUpByMobile(mobileRaw: String): JSONObject? = findByMobileIn("followups", mobileRaw)

    private fun findByMobileIn(key: String, mobileRaw: String): JSONObject? {
            val want = digits(mobileRaw)
            if (want.length != 10) return null
            val rows = load(key)
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                if (digits(row.optString("mobile")) == want) return JSONObject(row.toString())
            }
            return null
    }


    /**
     * 🚨 TK-REPORTED, চালু ক্লিনিক (28.07.2026, খাতার সারি B27 — দ্বিতীয় অংশ):
     * জমানো তালিকাটা লেখার সময় যে লেখাটা ডিস্কে যায়, সেটাই মনে রাখা হয়।
     * তাই পড়ার সময় আর ডিস্কের জন্য অপেক্ষা করতে হয় না।
     * ⛔ কোনো তথ্য বদলায় না — ডিস্কে ঠিক এই একই লেখাটাই জমা হয়, আগের মতোই
     * (`commit()` অক্ষত, তাই সেভ করা কিছু কখনো হারাবে না)।
     */
    private fun load(key: String): JSONArray = try {
        val raw = snapshot[key] ?: (prefs.getString(key, "[]") ?: "[]").also { snapshot[key] = it }
        JSONArray(raw)
    } catch (_: Exception) { JSONArray() }

    private fun save(key: String, rows: JSONArray) {
        // 🔴🔴🔒 V1317 (১১.০৯.২০২৬, TK-রিপোর্ট — Follow-up ও Payment পর্দা কালো হয়ে
        // আটকে যাচ্ছিল, একাধিক ভিডিওতে প্রমাণিত, রেকর্ডিং/রোটেশন/নেট-স্পিড কোনোটাই
        // কারণ নয় বলে TK নিজে ধরিয়ে দিয়েছেন): আসল কারণ — এই `.commit()` ডিস্কে
        // লেখা শেষ না হওয়া পর্যন্ত থেমে থাকে (synchronous)। V1311-এ আমারই যোগ করা
        // `markSyncedWhereCloudCaughtUp()` প্রতিবার পর্দা খোলার সময় এই সেভ-টা
        // ডাকতে শুরু করে (১৭টা জায়গায়, Follow-up ও Payment-সহ) — ফোনে মাসের পর
        // মাস জমা হওয়া বড় স্থানীয় খাতা (followups/payments, বিশেষত Master-এর
        // All Branches-এ) প্রতিবার পুরোটাই আবার ডিস্কে লেখার চেষ্টা করত, আটকে
        // দিত। ⛔ এই বাগটা আমারই ভুল (V1311), আগে থেকে ছিল না। **সমাধান:**
        // `MyPhoneWrites.kt`-এর প্রমাণিত একই কৌশল — `.apply()` (সঙ্গে সঙ্গে
        // মেমরিতে বসে, তাই এখনই পড়া/পরের কাজ ঠিক আগের মতোই পায়; ডিস্কের কাজ
        // পিছনে চলে) — পর্দা আর আটকাবে না।
        val text = rows.toString()
        snapshot[key] = text
        prefs.edit().putString(key, text).apply()
    }
    private fun digits(v: String): String = v.filter(Char::isDigit).takeLast(10)
    private fun isoNow(): String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
}
