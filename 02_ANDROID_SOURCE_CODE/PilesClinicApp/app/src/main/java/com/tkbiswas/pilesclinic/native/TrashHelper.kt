package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject

/**
 * TK-REQUESTED ADDITION (2026-07-18), Phase 1 of the "Delete mistaken
 * Enquiry/Registration" feature: a shared, safe way to move one record into
 * the EXISTING Trash Bin (same schema TrashRepository.kt already reads —
 * { id, table, record, deletedAt, deletedBy } — nothing about Trash Bin
 * itself is changed, so Restore there keeps working exactly as before).
 *
 * Phase 1 scope only: SAME-DAY delete. Whoever created the record (by
 * mobile) can delete it themselves on the day it was created — Master can
 * always delete regardless of day. Older records are NOT deletable through
 * this helper yet (guardCanDelete returns false) -- that is Phase 2/3
 * (Master-approval request flow), not built yet on purpose, to avoid
 * shipping a half-finished permission system.
 */
object TrashHelper {

    /**
     * 🔒🔒 খাতার সারি B165 (TK, 30.07.2026 — TK-এর ২ নম্বর সন্দেহ):
     * *"Delete মাঝপথে আটকে যেতে পারে... শেষের Delete ব্যর্থ হলে Trash তৈরি হয়ে
     *  যাবে, কিছু Follow-up Cancelled হয়ে যাবে, কিন্তু মূল Patient/Payment থেকে
     *  যেতে পারে।"* — **সত্যি ছিল।**
     *
     * **যে দোষটা এতে লুকিয়ে ছিল:** Trash-এর সারির আইডি **প্রতিবার নতুন করে
     * এলোমেলোভাবে** বানানো হত (`trash_` + random UUID)। তাই ডিলিট আধা-পথে
     * ব্যর্থ হলে স্টাফ আবার ডিলিট চাপলে **Trash-এ ওই একই রেকর্ডের দুটো (বা
     * তিনটে) আলাদা এন্ট্রি** তৈরি হয়ে যেত — মাস্টার Trash Bin খুলে বুঝতেই
     * পারতেন না কোনটা আসল, আর একটা Restore করলে বাকিগুলো পড়ে থাকত।
     *
     * **এখন আইডিটা রেকর্ড ধরে বাঁধা** — একই টেবিলের একই সারির জন্য **সবসময়
     * হুবহু একই আইডি**। তাই যতবারই চেষ্টা হোক, Trash-এ **একটাই এন্ট্রি** থাকে
     * (নতুন চেষ্টা পুরনোটার উপরেই লেখে)।
     *
     * ⛔ আইডিতে শুধু ইংরেজি অক্ষর · অঙ্ক · `_` · `-` রাখা হয় (বাকি সব `_`),
     *    কারণ এই আইডি পরে ওয়েব-ঠিকানায় বসে (Restore/Delete Forever)।
     * ⛔ শেষে রেকর্ডের নিজের নামের একটা ছোট হিসাব (hash) জোড়া হয়, তাই দুটো
     *    আলাদা রেকর্ড কখনো একই আইডি পেতে পারে না।
     * ⛔ **পুরনো Trash এন্ট্রি সম্পূর্ণ অক্ষত** — সেগুলোর আইডি বদলানো হয় না,
     *    Restore · Delete Forever আগের মতোই চলে।
     */
    fun trashIdFor(table: String, recordId: String): String {
        val raw = table.trim() + "_" + recordId.trim()
        val sb = StringBuilder()
        for (c in raw) {
            val keep = (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') ||
                c == '_' || c == '-'
            sb.append(if (keep) c else '_')
        }
        var safe = sb.toString()
        if (safe.length > 80) safe = safe.substring(safe.length - 80)
        return "trash_" + safe + "_" + Integer.toHexString(raw.hashCode())
    }

    private fun todayIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())

    /**
     * Phase 1 permission rule: Master can always delete. Anyone else only on
     * the SAME day the record was created, and only if they created it.
     *  - recordCreatedDate: the row's own date field (e.g. "date", "createdAt"
     *    truncated to yyyy-MM-dd) — pass in whatever this screen already uses.
     *  - recordCreatedByMobile: the row's receivedBy/createdBy mobile.
     */
    fun canDelete(user: NativeUser, recordCreatedDate: String, recordCreatedByMobile: String): Boolean {
        if (user.role == "master") return true
        val sameDay = recordCreatedDate.take(10) == todayIso()
        val sameMobile = recordCreatedByMobile.filter { it.isDigit() }.takeLast(10) ==
            user.mobile.filter { it.isDigit() }.takeLast(10)
        return sameDay && sameMobile
    }

    /** Moves the record into Trash, then removes it from its own table.
     *  Returns false (and leaves the original record untouched) on any
     *  failure, so a half-done delete never happens. */
    fun moveToTrash(table: String, record: JSONObject, deletedByMobile: String): Boolean {
        return try {
            val id = record.optString("id")
            if (id.isBlank()) return false
            // TK-REPORTED BUG FIX (2026-07-20): the trash table's "id" is a
            // NOT-NULL primary key, but this row never set one -- so every
            // insert was rejected by Supabase and surfaced as the misleading
            // "Could not delete — network too slow" toast. Give the trash row
            // its own unique id (the original record's id is preserved inside
            // "record", so Restore still rebuilds the exact original row).
            // 🔒 খাতার সারি B165: আইডি আর এলোমেলো নয় — রেকর্ড ধরে বাঁধা, তাই
            // ডিলিট আধা-পথে ব্যর্থ হয়ে আবার চেষ্টা হলেও Trash-এ **একটাই এন্ট্রি**
            // থাকে (আগে প্রতিবার নতুন এন্ট্রি তৈরি হত)।
            val trashRow = JSONObject()
                .put("id", trashIdFor(table, id))
                .put("table", table)
                .put("record", record)
                .put("deletedAt", isoNow())
                .put("deletedBy", deletedByMobile)
            if (!SupabaseClient.upsert("trash", trashRow)) return false
            val gone = SupabaseClient.deleteById(table, id)
            // 🚨🚨 TK-রিপোর্ট (03.08.2026) — এই ফাংশনেও (moveToTrashWithFollowup
            // Cascade-এর মতোই) একই গুরুতর ফাঁক ছিল: আসল রেকর্ডটা (table/id)
            // কখনো DeletedGuard-এ tombstone হতো না — শুধু ফোনের নিজের ক্যাশ
            // পরিষ্কার হতো। এই ফাংশন Payment-ডিলিটেও ব্যবহৃত হয় (দেখুন
            // DeletePermission.approveAndDelete) — তাই ডিলিট-করা পেমেন্টও একই
            // ঝুঁকিতে ছিল (কোনো ফোনের আটকে-থাকা সেভ পরে ফিরিয়ে দিতে পারত)।
            // এখন সরাসরি tombstone করা হচ্ছে।
            if (gone) { try { DeletedGuard.markDeleted(table, id, byMobile = deletedByMobile) } catch (_: Throwable) { } }
            // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B34): *"আমি
            // নিজে ডিলিট করে দিয়েছিলাম, তারপরে এখন সে কীভাবে চলে আসল?"*
            // মুছে ফেলা রেকর্ড ক্লাউড থেকে যেত, কিন্তু **ফোনের নিজের জমানো
            // তালিকা থেকে যেত না** — আর নিয়ম হলো "এই ফোনে যা সেভ হয়েছে তা সব
            // সময় দেখাবে", তাই কার্ডটা প্রতিবার ফিরে আসত (পুরনো/আধা তথ্য নিয়ে,
            // তাই রোগের নামও ফাঁকা দেখাত)। এখন ফোনের কপিটাও পরিষ্কার হয়।
            // ⛔ শুধু ক্লাউডে সত্যিই মোছা হয়ে যাওয়ার পরেই — তার আগে নয়।
            if (gone) forgetOnThisPhone(table, id, emptyList())
            gone
        } catch (e: Exception) {
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 🔒🔒 খাতার সারি B165 — ডিলিটের **আসল পুরনো অবস্থা** এই ফোনে জমা রাখা
    //
    // কেন দরকার: ডিলিট আধা-পথে ব্যর্থ হয়ে আবার চেষ্টা করলে সারিগুলো তখন
    // ইতিমধ্যেই `Cancelled` — তাই নতুন খোঁজায় সেগুলো ধরা পড়ে না। এখানে জমা
    // থাকা তালিকাটাই তখন আসল পুরনো অবস্থা মনে করিয়ে দেয়, যাতে Restore করলে
    // রোগীর কার্ডগুলো হুবহু আগের অবস্থায় ফিরে আসে।
    //
    // ⛔ এখানে **রোগীর কোনো তথ্য বা টাকা যায় না** — শুধু সারির আইডি, কোন
    //    টেবিলের, আর তার আগের অবস্থা (`Active`/`Incomplete` ইত্যাদি)।
    // ⛔ পুরোটাই ফোনের ভিতরে — একটাও নতুন ক্লাউড-কল নেই, নেট না থাকলেও চলে।
    // ⛔ তালিকা কখনো বড় হয় না (সর্বোচ্চ ২০০টি ডিলিট)।
    // ─────────────────────────────────────────────────────────────────
    private const val CASCADE_PREF = "piles_clinic_trash_cascade"
    private const val CASCADE_MAX = 200

    /** এই ডিলিটের স্ন্যাপশট ফোনে জমা রাখা। ব্যর্থ হলেও কিছু ভাঙে না। */
    private fun rememberCascade(trashId: String, arr: JSONArray) {
        try {
            val ctx = DeletedGuard.appContextOrNull() ?: return
            val p = ctx.getSharedPreferences(CASCADE_PREF, android.content.Context.MODE_PRIVATE)
            val all = try {
                JSONObject(p.getString("map", "{}") ?: "{}")
            } catch (_: Throwable) { JSONObject() }
            all.put(trashId, arr)
            // খুব পুরনোগুলো বাদ — তালিকা যেন কখনো বড় না হয়
            var out = all
            if (all.length() > CASCADE_MAX) {
                val keys = all.keys()
                val names = ArrayList<String>()
                while (keys.hasNext()) names.add(keys.next())
                val trimmed = JSONObject()
                val start = names.size - CASCADE_MAX
                for (i in start until names.size) {
                    val k = names[i]
                    trimmed.put(k, all.opt(k))
                }
                out = trimmed
            }
            p.edit().putString("map", out.toString()).apply()
        } catch (_: Throwable) { }
    }

    /** আগের চেষ্টায় জমা রাখা স্ন্যাপশট (না থাকলে `null`)। */
    private fun recallCascade(trashId: String): JSONArray? {
        return try {
            val ctx = DeletedGuard.appContextOrNull() ?: return null
            val p = ctx.getSharedPreferences(CASCADE_PREF, android.content.Context.MODE_PRIVATE)
            val all = JSONObject(p.getString("map", "{}") ?: "{}")
            all.optJSONArray(trashId)
        } catch (_: Throwable) { null }
    }

    /** পুরনো ও নতুন স্ন্যাপশট মেলানো — ⛔ **পুরনো অবস্থাটাই জেতে**, কারণ সেটাই
     *  ডিলিটের আগের আসল অবস্থা ছিল। */
    private fun mergeCascade(old: JSONArray?, fresh: JSONArray): JSONArray {
        val out = JSONArray()
        val seen = HashSet<String>()
        if (old != null) {
            for (i in 0 until old.length()) {
                val e = old.optJSONObject(i) ?: continue
                val cid = e.optString("id")
                if (cid.isBlank()) continue
                val ctable = e.optString("table", "followups").ifBlank { "followups" }
                if (!seen.add(ctable + "|" + cid)) continue
                out.put(e)
            }
        }
        for (i in 0 until fresh.length()) {
            val e = fresh.optJSONObject(i) ?: continue
            val cid = e.optString("id")
            if (cid.isBlank()) continue
            val ctable = e.optString("table", "followups").ifBlank { "followups" }
            if (!seen.add(ctable + "|" + cid)) continue
            out.put(e)
        }
        return out
    }

    /** মুছে ফেলা রেকর্ড ও তার সঙ্গে লুকিয়ে দেওয়া Follow-up সারিগুলো ফোনের
     *  নিজের জমানো তালিকা থেকেও সরিয়ে দেয়। কোনো কিছু না পাওয়া গেলেও কিছু
     *  ভাঙে না — শুধু চুপচাপ ফিরে আসে। */    private fun forgetOnThisPhone(table: String, id: String, cancelledFollowUpIds: List<String>) {
        try {
            val ctx = DeletedGuard.appContextOrNull() ?: return
            val store = LocalWorkflowStore(ctx)
            store.forgetRecord(table, id)
            if (cancelledFollowUpIds.isNotEmpty()) store.cancelFollowUpsLocally(cancelledFollowUpIds)
        } catch (_: Throwable) { }
    }

    /** TK-REQUESTED (2026-07-22): same as moveToTrash, but ALSO hides this
     *  patient/enquiry's Follow-up cards (Enquiry/Visit/Patient lists) so a
     *  delete actually removes them from view, not just from Trash --
     *  previously the followups row(s) were left untouched by delete, so the
     *  card stayed visible in the lists even though the record showed
     *  "Deleted". Each affected followups row's ORIGINAL status is
     *  snapshotted into this same trash row ("cascadedFollowups") so Restore
     *  can put them back exactly as they were: a patient who was already
     *  genuinely Rejected/Incomplete before this delete stays that way after
     *  Restore -- only the delete-caused hide is undone.
     *  moveToTrash() above is completely untouched by this addition, so
     *  nothing that already calls it is affected. */
    fun moveToTrashWithFollowupCascade(table: String, record: JSONObject, deletedByMobile: String, mobile: String): Boolean {
        return try {
            val id = record.optString("id")
            if (id.isBlank()) return false
            val digits = mobile.filter { it.isDigit() }.takeLast(10)
            val cascaded = JSONArray()
            if (digits.isNotEmpty()) {
                // TK-REPORTED-CLASS BUG AVOIDED (2026-07-22, caught in final
                // review): mobile is stored as "+91..." by this native app but
                // as bare 10 digits by the older WebView -- an exact "mobile=
                // eq.<digits>" filter would silently miss rows in the other
                // format. Reusing the same proven SupabaseClient.findByMobile()
                // (mobile=like.*<digits>) every other mobile lookup in this app
                // already uses, instead of a hand-rolled exact filter.
                // 🔴🔴🔒 V447 (TK-রিপোর্ট ১৮.০৮.২০২৬, চরম বিরক্তি + "Visit/Patient-ও
                // দেখুন" — সততার সাথে পুরো প্রজেক্ট আবার যাচাই করে ধরা পড়ল এই
                // Delete-পথেও **হুবহু একই বাগ**)। আগে এখানে ambiguous `findByMobile`
                // ব্যবহার হত — লুকআপ ব্যর্থ হলে চুপচাপ খালি লিস্ট আসত, `cascaded`
                // ফাঁকা থেকে যেত, কিন্তু মূল রেকর্ডটা তবু ডিলিট হয়ে যেত — অর্থাৎ
                // সহোদর followups/enquiries সারি কখনো বন্ধ না হয়েই আসল রেকর্ড মুছে
                // যেত, পরে সেই "Active" সারি থেকেই self-heal রোগীকে আবার ফিরিয়ে
                // আনত। **সমাধান:** `findByMobileOrNull` — লুকআপ সত্যিই ব্যর্থ হলে
                // (`null`) গোটা Delete-টাই honest ব্যর্থতা হিসেবে থামে (নিচে
                // cascadeLookupFailed গার্ড), স্টাফ "আবার চেষ্টা করুন" দেখেন —
                // কোনো আধা-ডিলিট (record মুছে গেছে কিন্তু সহোদর সারি বেঁচে) হয় না।
                var cascadeLookupFailed = false
                val rows = SupabaseClient.findByMobileOrNull("followups", mobile, "*", 200)
                if (rows == null) cascadeLookupFailed = true
                for (i in 0 until (rows?.length() ?: 0)) {
                    val row = rows!!.getJSONObject(i)
                    val status = row.s("status").ifBlank { "Active" }
                    // 🔒 খাতার সারি B110 (TK-এর সিদ্ধান্ত, 29.07.2026 বিকেল ৫.২০):
                    // *"Delete করলে সব টাকা বাদ যাবে"* — তাই **Incomplete সারিও**
                    // এখন ডিলিটের সঙ্গে বন্ধ হয়, নইলে আগে Incomplete করা রোগীকে
                    // ডিলিট করলে তাঁর টাকা হিসাবে থেকে যেত।
                    // ⛔ সারির **আসল পুরনো অবস্থা** স্ন্যাপশটে রাখা হয়, তাই Restore
                    //    করলে `Incomplete` আবার `Incomplete`-ই হয়ে ফিরে আসে —
                    //    কিছুই হারায় না।
                    // 🔒 V215 (§18, 31.07.2026 — TK-REPORTED, ছবিসহ · ROSHAN ARA):
                    // Visit-Reject/Reject তালিকায় থাকা রোগী Delete করলে "Moved to
                    // Trash Bin" দেখাত কিন্তু নামটা তালিকায় থেকেই যেত। **আসল কারণ:**
                    // Visit-Reject সারি নিজেই একটা already-`Cancelled` follow-up সারি,
                    // আর এখানে `if (status=="Cancelled") continue` ওটাকে বাদ দিত — তাই
                    // সারিটা কখনো tombstone/hide হত না, DraftRepository ওটাকে দেখেই
                    // রোগীকে তালিকায় রেখে দিত।
                    // **এখন:** Cancelled সারিও snapshot-এ ঢোকে; নিচের hide-loop-এ
                    // DeletedGuard দিয়ে tombstone হয় (DraftRepository সেই tombstone
                    // মেনে লুকায়)। Restore-এ tombstone সরে ও status হুবহু আগের
                    // (`Cancelled`) থেকে ফেরে — **কিছুই হারায় না**।
                    cascaded.put(JSONObject().put("id", row.optString("id")).put("status", status))
                }
                // 🚨🚨 TK-REPORTED, LIVE (29.07.2026 বিকেল ৩.৩৪ · খাতার সারি B108):
                // *"ডিলিট করলে আবার কেন চলে আসে?"*
                //
                // **আসল কারণ:** ডিলিট করলে `followups` সারিগুলো লুকানো হত ও আসল
                // রেকর্ডটা মোছা হত — কিন্তু ওই নম্বরের **`enquiries` সারিটা কখনো
                // ছোঁয়াই হত না**। আর এনকোয়ারি ট্যাবের জাল ঠিক ওই টেবিল থেকেই
                // কার্ড ফিরিয়ে আনে — তাই মুছে ফেলা নামটা বারবার ফিরে আসত।
                //
                // এখন এনকোয়ারি সারিগুলোও একই স্ন্যাপশটে ঢোকে (`table` চিহ্ন দিয়ে)
                // এবং একইভাবে লুকিয়ে দেওয়া হয়। **Restore করলে হুবহু আগের
                // অবস্থাতেই ফিরে আসে** — কারণ পুরনো status স্ন্যাপশটে রাখা থাকে।
                // ⛔ কোনো এনকোয়ারি সারি **মোছা হয় না** — শুধু `status` ঘরে দাগ।
                // ⛔ নতুন কোনো ঘর/টেবিল লাগেনি — পুরনো `cascadedFollowups`
                //    তালিকাটাই ব্যবহার হয়, তাই কোনো SQL লাগবে না।
                val enqRows = SupabaseClient.findByMobileOrNull("enquiries", mobile, "id,status", 200)
                if (enqRows == null) cascadeLookupFailed = true
                for (i in 0 until (enqRows?.length() ?: 0)) {
                    val row = enqRows!!.getJSONObject(i)
                    val status = row.s("status").ifBlank { "Active" }
                    // 🔒 V215 (§18): উপরের followups-এর মতোই — already-`Cancelled`
                    // enquiry সারিও এখন snapshot-এ থাকে ও tombstone হয় (আগে বাদ
                    // পড়ত), যাতে Reject তালিকা থেকেও সঙ্গে সঙ্গে সরে যায়। Restore-এ
                    // tombstone সরে, status হুবহু ফেরে।
                    val eid = row.optString("id")
                    if (eid.isBlank()) continue
                    cascaded.put(JSONObject().put("id", eid).put("status", status).put("table", "enquiries"))
                }
                // 🔴 V447 — লুকআপ সত্যিই ব্যর্থ হলে (নেট/সার্ভার-সমস্যা, "সত্যিই কোনো
                // সারি নেই"-এর সাথে গুলিয়ে ফেলা যাবে না) পুরো Delete থামে — আধা-ডিলিট
                // (রেকর্ড গেছে, সহোদর সারি বেঁচে) হওয়ার চেয়ে honest ব্যর্থতা ভালো।
                if (cascadeLookupFailed) return false
            }
            // 🔒 খাতার সারি B165 — এখানেও একই নিয়ম: আইডি রেকর্ড ধরে বাঁধা, তাই
            // বারবার চেষ্টা করলেও Trash-এ একটাই এন্ট্রি থাকবে।
            val trashId = trashIdFor(table, id)
            // 🔒🔒 খাতার সারি B165 — আইডি স্থায়ী করার সঙ্গে সঙ্গে একটা নতুন বিপদ
            // তৈরি হত, সেটা এখানেই বন্ধ করা হলো (কাজ করার সময় নিজে ধরা পড়েছে):
            //
            // প্রথম চেষ্টায় Follow-up/Enquiry সারিগুলো `Cancelled` হয়ে যায়, কিন্তু
            // শেষ ধাপে নেট কেটে যাওয়ায় মূল রেকর্ডটা মোছা গেল না। স্টাফ আবার
            // ডিলিট চাপলে উপরের খোঁজায় ওই সারিগুলো **আগে থেকেই Cancelled** বলে
            // বাদ পড়ে যেত — অর্থাৎ নতুন স্ন্যাপশট **ফাঁকা**। আগে আইডি এলোমেলো
            // ছিল বলে পুরনো এন্ট্রিটা টিকে থাকত; এখন একই আইডি বলে **নতুন ফাঁকা
            // স্ন্যাপশট পুরনোটার উপরে লিখে দিত — Restore করলে রোগীর কার্ডগুলো আর
            // ফিরে আসত না**।
            //
            // ⛔ তাই আসল পুরনো অবস্থাগুলো **এই ফোনেই আলাদা করে জমা থাকে**, আর
            //    প্রতিবার চেষ্টায় পুরনো ও নতুন মিলিয়ে নেওয়া হয় — **পুরনো
            //    অবস্থাটাই জেতে** (সেটাই রোগীর আসল অবস্থা ছিল)।
            // ⛔ এতে একটাও নতুন ক্লাউড-কল লাগেনি (পুরোটা ফোনের ভিতরে), আর নেট না
            //    থাকলেও কাজ করে।
            val cascadedFinal = mergeCascade(recallCascade(trashId), cascaded)
            if (cascadedFinal.length() > 0) rememberCascade(trashId, cascadedFinal)
            val trashRow = JSONObject()
                .put("id", trashId)
                .put("table", table)
                .put("record", record)
                .put("deletedAt", isoNow())
                .put("deletedBy", deletedByMobile)
            if (cascadedFinal.length() > 0) trashRow.put("cascadedFollowups", cascadedFinal)
            // TK-REPORTED FIX (2026-07-23): "Could not delete" on a Patient
            // (which cascades followups) despite a fast, stable connection.
            // The one thing that differs here from the SIMPLE moveToTrash()
            // above (which has never failed this way all session) is the
            // "cascadedFollowups" field -- the live "trash" table may not
            // have a matching column for it yet. The required "id" (NOT-
            // NULL, per the 2026-07-20 fix above) is kept in BOTH attempts.
            // If the first attempt (with cascadedFollowups) fails, retry
            // the EXACT same row minus that one field. If it's a genuine
            // network problem this retry fails too and failure is reported
            // exactly as before; if it was that field, the retry succeeds --
            // just without the cascaded-followups list saved in this
            // particular snapshot (Restore still works, it just won't
            // auto-restore those linked followup cards' status).
            val saved = SupabaseClient.upsert("trash", trashRow) ||
                (cascadedFinal.length() > 0 && SupabaseClient.upsert("trash", JSONObject()
                    .put("id", trashId)
                    .put("table", table)
                    .put("record", record)
                    .put("deletedAt", isoNow())
                    .put("deletedBy", deletedByMobile)))
            if (!saved) return false
            // Hide the followups rows only AFTER the trash snapshot is safely
            // saved -- so a mid-way failure never hides a card without a
            // saved way back to un-hide it.
            val hiddenIds = ArrayList<String>()
            for (i in 0 until cascadedFinal.length()) {
                val entry = cascadedFinal.getJSONObject(i)
                val cid = entry.optString("id")
                // 🚨 খাতার সারি B108: সারিটা কোন টেবিলের, সেটা এখন চিহ্ন দিয়ে লেখা
                // থাকে। পুরনো স্ন্যাপশটে চিহ্ন নেই — তখন আগের মতোই `followups`।
                val ctable = entry.optString("table", "followups").ifBlank { "followups" }
                val origStatus = entry.s("status")
                if (cid.isNotBlank()) {
                    // ⛔ V215 (§3 Free-plan): যেটা আগে থেকেই Cancelled তার জন্য আর
                    // একটা redundant cloud write পাঠানো হয় না — শুধু tombstone।
                    if (!origStatus.equals("Cancelled", true)) {
                        SupabaseClient.updateById(ctable, cid, JSONObject().put("status", "Cancelled"))
                    }
                    // 🔒 V215 (§18): সারিটা tombstone করা হয় যাতে DeletedGuard-মান্য
                    // যেকোনো তালিকা (DraftRepository-র Visit-Reject/Reject/Incomplete)
                    // এটা সঙ্গে সঙ্গে লুকায়। Restore-এ TrashRepository এটা unmark করে।
                    try { DeletedGuard.markDeleted(ctable, cid) } catch (_: Throwable) { }
                    if (ctable == "followups") hiddenIds.add(cid)
                }
            }
            val gone = SupabaseClient.deleteById(table, id)
            // 🚨🚨 TK-রিপোর্ট (03.08.2026, ছবিসহ — "DEMO" রোগী নিজে Reject+Delete
            // করার পরেও ফিরে এলো) — গভীর যাচাইয়ে ধরা পড়েছে: এতদিন উপরের লুপে
            // শুধু **cascade-এর (mobile-মিলিয়ে পাওয়া followups/enquiries)**
            // সারিগুলো DeletedGuard-এ tombstone হতো — কিন্তু **এই ফাংশনে যে আসল
            // রেকর্ডটা (table/id) মোছার জন্য দেওয়া হয়েছে সেটা নিজে কখনো
            // tombstone হতো না**, যদি না সেটা কাকতালীয়ভাবে ওই cascade-এও ধরা
            // পড়ে (শুধু "enquiries" টেবিলের বেলায় হতে পারত, "patients"-এর
            // বেলায় কখনোই না)। ফলে "patients" টেবিলের কোনো রেকর্ড Delete করলে
            // — কোনো ফোনের আগে থেকে জমে-থাকা/আটকে-থাকা সেভ (CloudWriteQueue)
            // পরে চেষ্টা করলে DeletedGuard.isDeleted() মিথ্যা বলত (কখনো মার্ক
            // হয়নি), আর রেকর্ডটা আবার ক্লাউডে ফিরে যেতে পারত। এখন আসল
            // রেকর্ডটাও (নিজের table/id) সরাসরি tombstone করা হচ্ছে — cascade
            // থাকুক বা না থাকুক।
            if (gone) { try { DeletedGuard.markDeleted(table, id, byMobile = deletedByMobile) } catch (_: Throwable) { } }
            // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B34): *"আমি
            // নিজে ডিলিট করে দিয়েছিলাম, তারপরে এখন সে কীভাবে চলে আসল?"*
            // মুছে ফেলা রেকর্ড ক্লাউড থেকে যেত, কিন্তু **ফোনের নিজের জমানো
            // তালিকা থেকে যেত না** — আর নিয়ম হলো "এই ফোনে যা সেভ হয়েছে তা সব
            // সময় দেখাবে", তাই কার্ডটা প্রতিবার ফিরে আসত (পুরনো/আধা তথ্য নিয়ে,
            // তাই রোগের নামও ফাঁকা দেখাত)। এখন ফোনের কপিটাও পরিষ্কার হয়।
            // ⛔ শুধু ক্লাউডে সত্যিই মোছা হয়ে যাওয়ার পরেই — তার আগে নয়।
            if (gone) forgetOnThisPhone(table, id, hiddenIds)
            gone
        } catch (e: Exception) {
            false
        }
    }
}
