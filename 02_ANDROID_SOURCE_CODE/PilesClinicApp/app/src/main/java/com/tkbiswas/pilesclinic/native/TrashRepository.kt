package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * Native rebuild -- Trash Bin (Master only).
 *
 * Reads the live Supabase "trash" table. Each trash row wraps a deleted record:
 *   { id, table, record (the original row), deletedAt, deletedBy }.
 * Restore re-inserts the original record into its own table and removes the
 * trash entry, matching restoreTrash() in app.js.
 */
data class TrashItem(
    val id: String,
    val table: String,
    val label: String,
    val record: JSONObject,
    // TK-REQUESTED (2026-07-21): every trash row already records who deleted
    // it and when (moveToTrash writes deletedBy/deletedAt) -- these were being
    // fetched then silently dropped, never shown to Master. Added here with
    // safe defaults so nothing else that constructs/uses TrashItem breaks.
    val deletedAt: String = "",
    val deletedBy: String = "",
    // TK-REQUESTED (2026-07-22): follow-up rows hidden as part of this
    // delete (see TrashHelper.moveToTrashWithFollowupCascade), so Restore
    // can put them back. Null/empty for ordinary trash rows -- unaffected.
    val cascadedFollowups: org.json.JSONArray? = null,
    /**
     * 🔴🔒 V515 (২২.০৮.২০২৬, TK-অনুমোদিত — Egress): `true` মানে এই `record`-এ
     * শুধু **পর্দায় দেখানোর ঘরগুলো** আছে, পুরো মুছে ফেলা রেকর্ড নয়।
     * ⛔ এই অবস্থায় `restore()` **কখনো** এই আংশিক record দিয়ে লিখবে না —
     *    আগে ক্লাউড থেকে আসল পুরো record এনে নেবে (নিচে দেখুন)।
     * ⛔ পুরনো জমানো কপি (SharedPreferences) বা fallback পথে পুরো `record`
     *    থাকে ⇒ তখন `false`, আচরণ হুবহু আগের মতোই।
     */
    val recordIsPartial: Boolean = false
)

class TrashRepository {

    companion object {
        /**
         * 🔴🔒 V515 (২২.০৮.২০২৬, TK-অনুমোদিত — Egress) — **Trash Bin আর ছবি নামায় না।**
         *
         * সমস্যা যেটা ছিল: `trash` টেবিলের প্রতিটা সারির `record` ঘরে **মুছে ফেলা
         * পুরো রেকর্ডটাই** থাকে — রোগীর base64 ছবিসহ। Trash Bin খুললেই ৫০০০ পর্যন্ত
         * এমন সারি নামত। অ্যাপের সবচেয়ে ভারী পড়া ছিল এটাই।
         *
         * কিন্তু পর্দা `record`-এর ভিতর থেকে সত্যিই কী কী পড়ে? কোড ধরে **চারটে
         * ফাইলের প্রতিটা লাইন মিলিয়ে** দেখা হয়েছে — TrashCardText · TrashAdapter ·
         * TrashBinActivity · TrashRepository — মোট **১৯টা ঘর**, নিচের তালিকাটাই।
         * ছবি ওদের একটাও নয়।
         *
         * তাই তালিকায় এখন `record`-এর ওই ১৯টা ঘরই আলাদা করে চাওয়া হয়
         * (`record->>name` — PostgREST-এর jsonb পথ; `record` ঘরটা `jsonb`,
         * DB_SETUP লাইন ২০৫-এ প্রমাণ)। ছবি আর নামেই না।
         *
         * ⛔ পর্দায় দেখানো কিছুই বদলায়নি — একই লেবেল, একই কার্ডের লেখা, একই
         *    ব্রাঞ্চ-ছাঁকনি, একই খোঁজা।
         * ⛔ **Restore আগের মতোই পুরো আসল রেকর্ড দিয়েই হয়** — শুধু সেটা এখন
         *    Restore চাপার মুহূর্তে ওই এক সারির জন্য আনা হয় (নিচে `restore()`)।
         * ⛔ সরু পড়া ব্যর্থ হলে **হুবহু আগের পুরো পড়াটাই** চলে (V512-এর পথ),
         *    তাই পর্দা কখনো ফাঁকা বা অসম্পূর্ণ দেখাবে না।
         */
        val RECORD_LIST_FIELDS = listOf(
            "altMobile", "amount", "bill", "branch", "date", "disease", "id", "mobile",
            "mode", "name", "nextFollow", "patientCode", "patientId", "payType",
            "receivedBy", "refBy", "registrationDate", "stage", "status"
        )

        /** তালিকার সরু কলাম — `record` পুরোটা নয়, শুধু উপরের ঘরগুলো। */
        val TRASH_LIST_COLS: String =
            (listOf("id", "table", "deletedAt", "deletedBy", "cascadedFollowups") +
                RECORD_LIST_FIELDS.map { "rk_$it:record->>$it" }).joinToString(",")

        /**
         * সারি থেকে `record` বানায়।
         *  · পুরো `record` থাকলে (fallback পথ · পুরনো জমানো কপি) সেটাই ফেরে —
         *    আচরণ হুবহু আগের মতোই।
         *  · না থাকলে উপরের ঘরগুলো জুড়ে **আংশিক** record বানানো হয় (শুধু দেখানোর
         *    জন্য); তখন `recordIsPartial = true`।
         */
        fun recordFrom(row: JSONObject): Pair<JSONObject, Boolean> {
            row.optJSONObject("record")?.let { return Pair(it, false) }
            val o = JSONObject()
            for (k in RECORD_LIST_FIELDS) {
                val v = row.opt("rk_$k")
                if (v != null && v != JSONObject.NULL) o.put(k, v)
            }
            return Pair(o, true)
        }
    }

    fun fetchTrash(): List<TrashItem> = parseTrash(fetchTrashRaw())

    // TK-REQUESTED (2026-07-24): split out so TrashBinActivity can cache the
    // raw rows (SharedPreferences, same pattern as Payment/Doctor Visit/
    // Follow-up) and show them instantly next time, without touching this
    // parsing logic at all -- fetchTrash() above still behaves exactly as
    // before for any other caller.
    fun fetchTrashRaw(): org.json.JSONArray {
        // TK-REQUESTED SAFETY FIX (2026-07-16): same fix as Doctor Queue/Visit
        // -- explicit high limit so older trashed rows never silently fall
        // outside the default 500-row window (Master would think a delete
        // never happened / can't restore it).
        // 🔴🔴🔴 V509 (২১.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — *"trash bin তো সারাজীবন
        // ফাঁকাই দেখলাম, মাস্টার হিসাবে"*)। **কারণ প্রমাণ করে দেখা হয়েছে:**
        //
        // `SupabaseClient.fetchList()`-এর ডিফল্ট সাজানোর নিয়ম
        // `order=updatedAt.desc.nullslast`। কিন্তু **`trash` টেবিলে `updatedAt`
        // ঘরটাই নেই** — তার ঘর মাত্র ছয়টা: id · table · record · deletedAt ·
        // deletedBy · cascadedFollowups (DB_SETUP লাইন ২০২–২০৮ ও V257 প্যাচ)।
        // নেই-ঘর ধরে সাজাতে বললে ডেটাবেস সরাসরি ভুল ধরিয়ে দেয় —
        //     ERROR: column "updatedAt" does not exist
        // অনুরোধটাই বাতিল হয়, `fetchList` ফাঁকা তালিকা ফেরায়, পর্দায় বসে
        // **"Trash empty"**। তাই এটা ছাঁকনির সমস্যা নয় — **Trash Bin কোনোদিনই
        // একটাও সারি দেখাতে পারেনি**, ব্রাঞ্চ যাই বাছা হোক, কে দেখুক না কেন।
        //
        // ⇒ এখন `deletedAt` ধরে সাজানো হয় — এই টেবিলে ঘরটা সত্যিই আছে, আর
        //   অর্থও ঠিক: **সবচেয়ে সাম্প্রতিক ডিলিট সবার উপরে**।
        //
        // ⛔ আর কিছুই বদলায়নি — একই টেবিল, একই ৫০০০ সীমা, কোনো ছাঁকনি যোগ
        //    হয়নি, Restore/Delete-এর নিয়মও অক্ষত। শুধু সাজানোর ঘরটা বদলাল,
        //    তাই অনুরোধ আর বাতিল হয় না।
        // ⚠️ মুছে যাওয়া কোনো রেকর্ড হারায়নি — সেগুলো `trash` টেবিলে এতদিন ঠিকই
        //    জমা ছিল, শুধু পর্দায় দেখা যাচ্ছিল না।
        /* 🟢🔒 V997 (০৩.০৯.২০২৬, TK-নির্দেশ — Egress তালিকার ২ নম্বর):
           `trash`-এর প্রতিটা সারিতে `record` ঘরে **মুছে ফেলা পুরো সারিটাই**
           জমা থাকে — এই টেবিলের সবচেয়ে ভারী জিনিস। অথচ তালিকার কার্ডে
           ওই পুরো লেখাটার দরকারই পড়ে না; দরকার পড়ে **Restore চাপলে**, আর
           সেই এক-সারির পড়াটা `restore()`-এ আগে থেকেই আছে (লাইন ২৪৭-২৫৪)।

           উপরের `TRASH_LIST_COLS` (ও `recordFrom()`) এই কাজটার জন্যই লেখা
           হয়েছিল, কিন্তু **কোথাও ব্যবহার হয়নি** — তাই তালিকা এখনো পুরো
           `record` সহ নামত। এখন সেটাই ব্যবহার হচ্ছে।

           ⛔ সরু পড়া ব্যর্থ হলে (পুরনো ডেটাবেস · ঘর নেই · নেট) **হুবহু
              আগের পুরো পড়াটাই** চলে — V509/V512-এর সমাধান দুটোই অটুট।
           ⛔ সাজানো আগের মতোই `deletedAt.desc` — V509-এর কারণ ছোঁয়া হয়নি।
           ⛔ Restore · Delete · ব্রাঞ্চ-ছাঁকনি · কার্ডের লেখা — কিছুই বদলায়নি
              (কার্ড যে ঘরগুলো পড়ে সেগুলো `RECORD_LIST_FIELDS`-এ আছে)। */
        val slim = SupabaseClient.fetchListSlimOrNull(
            "trash", null, 5000, TRASH_LIST_COLS, order = "deletedAt.desc.nullslast")
        if (slim != null) return slim
        return SupabaseClient.fetchList("trash", null, 5000, order = "deletedAt.desc.nullslast")
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔴🔴🔒 V512 (২১.০৮.২০২৬) — **"নেট খারাপ হলে Trash Bin ফাঁকা দেখাত"**
    //
    // ─── কারণ (কোড ধরে প্রমাণিত, আন্দাজ নয়) ───────────────────────────────
    //   উপরের `fetchTrashRaw()` ব্যবহার করে `SupabaseClient.fetchList()`,
    //   আর সেটা **ব্যর্থ হলেও ফাঁকা তালিকা `[]` ফেরায়**
    //   (SupabaseClient.kt — `if (!response.isSuccessful) return JSONArray()`
    //    এবং `catch (e: Exception) { JSONArray() }`)।
    //   ফলে TrashBinActivity-র `if (rawRows == null)` পাহারাটা **কখনোই**
    //   চালু হতো না; বদলে —
    //     (১) ফোনের জমানো কপির উপরে `"[]"` লিখে ফেলত (পরের বারও ফাঁকা), আর
    //     (২) পর্দায় **"Trash empty"** বসিয়ে দিত।
    //   অর্থাৎ কিছুই মোছা হয়নি, অথচ দেখে মনে হতো সব চলে গেছে।
    //
    // ⇒ এই নতুন ফাংশনটা `fetchListOrNull()` ব্যবহার করে — সেটা **সত্যিকারের
    //   ব্যর্থতায় `null`** ফেরায়, আর সার্ভারে সত্যিই শূন্য সারি থাকলে ফাঁকা
    //   তালিকা। দুটো অবস্থা আলাদা করা যায় বলেই আর ভুল বার্তা ওঠে না।
    //   (এটা নতুন কোনো কৌশল নয় — Chamber Attendance · Draft · Payment ও
    //    Doctor Visit পর্দায় ঠিক এই একই সমাধান আগে থেকেই চলছে।)
    //
    // ⚠️🔒 V798 — এই `fetchTrashRaw()` **কোথাও ডাকা হয় না** (প্রজেক্ট খুঁজে যাচাই
    //    করা, ২৮.০৮.২০২৬)। এটা `select=*` দিয়ে পড়ে, আর `trash.record`-এ রোগীর
    //    ছবিসহ পুরো সারি থাকে ⇒ কেউ ভবিষ্যতে এটাকে ডাকলে egress হু-হু করে বাড়বে।
    //    ⛔ নতুন কোনো কোডে এটা ব্যবহার করবেন না — `fetchTrashRawOrNull()` ব্যবহার
    //       করুন, ওটা সরু কলামে পড়ে।
    // ⛔ URL · টেবিল · সাজানোর নিয়ম · ৫০০০ সীমা — এক অক্ষরও বদলায়নি।
    // ⛔ উপরের `fetchTrashRaw()` **হুবহু আগের মতোই** রাখা হলো, তাই
    //    `fetchTrash()` বা অন্য কোনো ডাকার জায়গার আচরণ বদলায়নি।
    // ⛔ Restore / Delete Forever-এর নিয়মে হাত পড়েনি।
    // ══════════════════════════════════════════════════════════════════════
    // ⛔ V512 (সংশোধিত) — `fetchListOrNull` নয়, `fetchListOrNullDirect`।
    //    কারণ: `trash`-এর সারিতে মুছে ফেলা রেকর্ডের পুরোটা (রোগীর ছবিসহ) থাকে;
    //    সেটা `CloudReadDedupe`-এ ৬০ সেকেন্ড ধরে রাখলে অন্য পর্দার জমানো
    //    উত্তর ছিটকে যেত ⇒ Egress বাড়ত। এই যমজ ফাংশনটা dedupe ছোঁয় না, তাই
    //    সফল অবস্থায় পুরোনো `fetchList()`-এর **হুবহু** সমান — শুধু ব্যর্থতায়
    //    `null` ফেরায়। (SupabaseClient.kt-এ কারণটা পুরো লেখা আছে।)
    fun fetchTrashRawOrNull(): org.json.JSONArray? {
        /* 🔴🔒 V515 — আগে ছবিসহ পুরো `record` নামত; এখন শুধু পর্দার ঘরগুলো।
           ⛔ `fetchListOrNullDirect` **ইচ্ছে করেই** রাখা হলো (V512-এর কারণ অটুট):
              এই পড়া `CloudReadDedupe` ছোঁয় না, তাই অন্য পর্দার জমানো উত্তর
              ছিটকে যাওয়ার আগের সমস্যাটা ফিরতে পারে না।
           ⛔ সরু পড়া ব্যর্থ হলে (ঘরের নাম/সিনট্যাক্স যা-ই হোক) নিচে **হুবহু
              আগের পুরো পড়াটাই** চলে — অর্থাৎ সবচেয়ে খারাপ অবস্থাতেও আচরণ
              V512-এর সমান, পর্দা কখনো ফাঁকা দেখাবে না। */
        val narrow = SupabaseClient.fetchListOrNullDirect(
            "trash", null, 5000, order = "deletedAt.desc.nullslast", select = TRASH_LIST_COLS
        )
        if (narrow != null) return narrow
        /* 🔴🔒 V798 (২৮.০৮.২০২৬) — TK-নির্দেশ: "Supabase egress-এর ঝুঁকি সব সারান"।
           ─── সমস্যা যেটা ছিল ────────────────────────────────────────────────
           উপরের সরু পড়াটা PostgREST-এর `record->>field` লেখা ব্যবহার করে।
           কোনো কারণে ওটা ব্যর্থ হলে সোজা `select=*` চলত — আর `trash.record`
           ঘরে **মুছে ফেলা পুরো সারিটা, রোগীর ছবিসহ** থাকে। ৫০০০ সারি × ছবি
           = একবারেই বহু-শত MB। free প্ল্যানের জন্য এটাই সবচেয়ে বড় গর্ত ছিল।
           ─── সারানো ───────────────────────────────────────────────────────
           মাঝখানে আর একটা ধাপ বসল: **শুধু আসল, সাধারণ ঘরগুলো** — কোনো JSON
           লেখা নেই, তাই সিনট্যাক্স-ভুলে ব্যর্থ হওয়ার পথ নেই। এতে পর্দা ভরে
           ওঠে (নাম না পেলে id দেখায়), ছবি নামে না।
           ⛔ একদম নিচের `select=*` পথটা **হুবহু আগের মতোই** রয়ে গেল, শুধু
              এখন সেখানে পৌঁছতে হলে **দুটো** পড়াকেই ব্যর্থ হতে হবে ⇒ V512-এর
              প্রতিশ্রুতি ("Trash Bin কখনো ফাঁকা দেখাবে না") অক্ষত। */
        val plain = SupabaseClient.fetchListOrNullDirect(
            "trash", null, 5000, order = "deletedAt.desc.nullslast",
            select = "id,table,deletedAt,deletedBy,cascadedFollowups"
        )
        if (plain != null) return plain
        return SupabaseClient.fetchListOrNullDirect("trash", null, 5000, order = "deletedAt.desc.nullslast")
    }

    fun parseTrash(rows: org.json.JSONArray): List<TrashItem> {
        val list = mutableListOf<TrashItem>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            /* 🔴🔒 V515: পুরো `record` থাকলে সেটাই (পুরনো জমানো কপি ও fallback পথ),
               নইলে সরু ঘরগুলো জুড়ে আংশিক record — লেবেল/কার্ড/ছাঁকনি সবই একই ঘর
               থেকে আসে, তাই পর্দায় কোনো পার্থক্য নেই। */
            val (record, partial) = recordFrom(row)
            val label = record.s("name").ifBlank {
                record.s("mobile").ifBlank { record.s("id").ifBlank { row.s("id") } }
            }
            list.add(
                TrashItem(
                    id = row.s("id"),
                    table = row.s("table"),
                    label = label,
                    record = record,
                    deletedAt = row.s("deletedAt"),
                    deletedBy = row.s("deletedBy"),
                    cascadedFollowups = row.optJSONArray("cascadedFollowups"),
                    recordIsPartial = partial
                )
            )
        }
        return list
    }

    /** Re-inserts the original record into its table, then deletes the trash row. */
    fun restore(item: TrashItem, context: android.content.Context? = null): Boolean {
        if (item.table.isBlank()) return false

        /* 🔴🔴🔒 V515 (২২.০৮.২০২৬, TK-অনুমোদিত — Egress) — **Restore সবসময় আসল
           পুরো রেকর্ড দিয়েই হয়।**

           তালিকাটা এখন হালকা করে আনা হয় (ছবি বাদ, শুধু দেখানোর ঘর) — তাই ওই
           আংশিক record দিয়ে ভুল করেও লেখা যাবে না; লিখলে **মুছে ফেলা রেকর্ডের
           বাকি সব তথ্য (ছবিসহ) চিরতরে হারিয়ে যেত**। তাই Restore চাপার ঠিক
           মুহূর্তে ওই **একটি** সারির পুরো `record` ক্লাউড থেকে আনা হয়।

           ⛔ আনতে না পারলে (নেট নেই / সারি নেই / ফাঁকা record) — **কিছুই লেখা
              হয় না**, `false` ফেরে, trash সারিটা অক্ষত থাকে, পরে আবার চেষ্টা
              করা যায়। ঠিক V223 §C1-এর নিয়ম, এক চুলও আলাদা নয়।
           ⛔ পুরনো জমানো কপি বা fallback পথে record আগে থেকেই পুরো
              (`recordIsPartial == false`) — তখন **একটাও বাড়তি অনুরোধ হয় না**,
              আচরণ হুবহু আগের মতোই।
           ⛔ একটামাত্র সারির জন্য একটামাত্র অনুরোধ, আর সেটা শুধু Restore
              চাপলেই — তালিকা খোলায় নয়। */
        val fullRecord: JSONObject = if (!item.recordIsPartial) item.record else {
            val one = try {
                SupabaseClient.fetchListOrNullDirect(
                    "trash",
                    "id=eq." + java.net.URLEncoder.encode(item.id, "UTF-8"),
                    1, order = "deletedAt.desc.nullslast", select = "record"
                )
            } catch (_: Throwable) { null } ?: return false
            val rec = one.optJSONObject(0)?.optJSONObject("record") ?: return false
            if (rec.length() == 0) return false
            rec
        }

        // TK-REQUESTED (2026-07-26): this row was tombstoned when it was
        // deleted . clear that first, otherwise its normal cloud sync would
        // keep being skipped after the restore.
        try { DeletedGuard.unmark(item.table, fullRecord.optString("id", "")) } catch (_: Throwable) { }
        // 🔒 V223 (§C1, 01.08.2026): পুরোনো snapshot বসানোর আগে cloud-এর updatedAt
        // **নিশ্চিতভাবে** পড়ে মেলানো হয়:
        //   · cloud নবীন হলে (KEPT_NEWER) পুরোনো snapshot চাপা দেওয়া হয় না — record
        //     cloud-এ নবীন রূপে আছে, তাই restore সফল ধরা হয়।
        //   · cloud পড়া ব্যর্থ / তুলনা অসম্ভব / লেখা ব্যর্থ (BLOCKED) হলে **কিছুই লেখা
        //     হয় না** ও false ফেরে — trash সারি অক্ষত থাকে (পরে আবার চেষ্টা করা যায়),
        //     পুরোনো data কখনো আন্দাজে লেখা হয় না। DB trigger দ্বিতীয় পাহারা।
        val outcome = SupabaseClient.upsertRestoreSafe(item.table, fullRecord)
        if (outcome == SupabaseClient.RestoreOutcome.BLOCKED) return false
        // TK-REQUESTED (2026-07-22): put back any Follow-up rows that were
        // hidden as part of this delete (see moveToTrashWithFollowupCascade),
        // so the card reappears in the Enquiry/Visit/Patient lists exactly
        // as it was before delete. Done before removing the trash row so a
        // failure here leaves the trash entry in place for a retry.
        item.cascadedFollowups?.let { arr ->
            for (i in 0 until arr.length()) {
                val entry = arr.getJSONObject(i)
                val fid = entry.optString("id")
                val status = entry.s("status")
                // 🚨 খাতার সারি B108: স্ন্যাপশটে এখন এনকোয়ারি সারিও থাকতে পারে —
                // চিহ্ন দেখে ঠিক টেবিলেই ফেরানো হয়। পুরনো স্ন্যাপশটে চিহ্ন নেই,
                // তখন আগের মতোই `followups` — **পুরনো Trash এন্ট্রি অক্ষত**।
                val ctable = entry.optString("table", "followups").ifBlank { "followups" }
                if (fid.isNotBlank() && status.isNotBlank()) {
                    // 🔒 V215 (§18/§14.7): delete-এর সময় এই সারিটা tombstone করা
                    // হয়েছিল (TrashHelper) — Restore করলে আগে tombstone সরাতে হয়,
                    // নইলে status ফিরলেও DeletedGuard-মান্য তালিকা এটা লুকিয়েই রাখত।
                    // tombstone না থাকলে unmark নিরাপদ no-op।
                    try { DeletedGuard.unmark(ctable, fid) } catch (_: Throwable) { }
                    // TK-REQUESTED (2026-07-27): a failure here used to be
                    // silent -- the record came back but its Follow-up card
                    // stayed hidden for ever. Now it is retried later.
                    val fields = JSONObject().put("status", status)
                    // 🔴🔴🔒 V510 (২১.০৮.২০২৬, TK-রিপোর্ট — KAMAL ROY-এর ফলো-আপ
                    // দুটো Restore-এর পরেও `Cancelled`-ই থেকে যাচ্ছিল)।
                    //
                    // **কারণ (লাইভ ডেটাবেস থেকে হুবহু বার করা, আন্দাজ নয়):**
                    // `followups` টেবিলে `tk_terminal_no_return` নামে একটা ট্রিগার
                    // আছে (`public.tk_followup_terminal_guard`)। তার নিয়ম —
                    // Cancelled/Incomplete/Rejected/Closed সারিকে Active করা যাবে
                    // **শুধু তখনই**, যখন `history`-র **শেষ** এন্ট্রিতে থাকে
                    //     status = "Active"  আর  remark শুরু "Restored"/"Continue" দিয়ে।
                    // নইলে ট্রিগার চুপচাপ `return old` করে — অর্থাৎ **কোনো ভুল না
                    // দেখিয়ে** পুরোনো অবস্থাই রেখে দেয়।
                    // আমরা এতদিন শুধু `status` পাঠাতাম, `history` নয় — তাই Restore
                    // বোতাম রোগীকে ফেরাত ঠিকই, কিন্তু তাঁর ফলো-আপ কার্ড চিরকাল
                    // লুকানো থেকে যেত (TK ২১.০৮.২০২৬-এ লাইভে ধরেছেন)।
                    //
                    // ⛔ পুরোনো `history` মোছা হয় না — সার্ভার থেকে পড়ে নিয়ে তার
                    //    **শেষে একটা লাইন যোগ** করা হয় (কে · কবে · কেন)।
                    // ⛔ পড়া না গেলে অন্তত একটা লাইন পাঠানো হয়, যাতে ট্রিগারের শর্ত
                    //    মেটে — সবচেয়ে খারাপ ক্ষেত্রেও শুধু history-র পুরোনো লেখা
                    //    কমে, কোনো রেকর্ড/টাকা/রোগীর তথ্য নষ্ট হয় না।
                    // ⛔ `enquiries`-এ এই ট্রিগার নেই, তাই সেখানে আগের মতোই শুধু status।
                    if (ctable == "followups" && status.equals("Active", ignoreCase = true)) {
                        val line = JSONObject()
                            /* ⏰🔒 V888 (৩০.০৮.২০২৬, TK-নির্দেশ — *"তারিখ এবং সময়
                               সমস্ত জায়গায় লাগবে"*): এই একটাই লাইন `date`/`time`
                               ছাড়া লেখা হতো, তাই টাইমলাইনে তারিখ-সময় দুটোই ফাঁকা
                               দেখাত। এখন প্রকল্পের বাকি সব history-লেখার হুবহু
                               একই দুটো ঘর বসে। ⛔ পুরোনো `at`/`by` ঘর দুটো অটুট। */
                            .put("date", com.tkbiswas.pilesclinic.native.FollowUpModel.today())
                            .put("time", trashIsoNow())
                            .put("status", "Active")
                            .put("remark", "Restored from Trash")
                            .put("at", trashIsoNow())
                            .put("by", try {
                                if (context != null) NativeSession.current(context)?.mobile ?: "" else ""
                            } catch (_: Throwable) { "" })
                        val hist = try {
                            val enc = java.net.URLEncoder.encode(fid, "UTF-8")
                            val rows = SupabaseClient.fetchListSlimOrNull(
                                "followups", "id=eq.$enc", 1, "id,history"
                            )
                            rows?.optJSONObject(0)?.optJSONArray("history") ?: org.json.JSONArray()
                        } catch (_: Throwable) { org.json.JSONArray() }
                        hist.put(line)
                        fields.put("history", hist)
                    }
                    val okRow = SupabaseClient.updateById(ctable, fid, fields)
                    if (!okRow && context != null) {
                        try { GenericUpdateQueue.queue(context, ctable, fid, fields) } catch (_: Throwable) { }
                    }
                }
            }
        }
        return SupabaseClient.deleteById("trash", item.id)
    }

    /** TK APPROVED (2026-07-20): permanently removes a trash entry from the
     *  cloud. This is the ONLY place in the whole app where a record is
     *  destroyed for good -- after this there is NO way to get it back.
     *  Master-only: the caller (TrashBinActivity) only ever shows the Delete
     *  Forever button to a Master, and confirms with a "Sure?" popup first. */
    fun permanentDelete(item: TrashItem): Boolean {
        if (item.id.isBlank()) return false
        return SupabaseClient.deleteById("trash", item.id)
    }

    /** 🔴 V510 — `history` লাইনের সময়, প্রকল্পের সব জায়গার একই ছাঁচে
     *  (UTC · `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`)। ⛔ Locale.US — নইলে কোনো ফোনে
     *  বাংলা সংখ্যা বসে যেত (V509-এ ঠিক এই ভুলটা একবার ধরা পড়েছিল)। */
    private fun trashIsoNow(): String = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())
    } catch (_: Throwable) { "" }
}
