package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * TK-REQUESTED (2026-07-26): "Delete করা রেকর্ড ফিরে আসা বন্ধ".
 *
 * Every save is local first and then pushed to Supabase through a retry queue.
 * If a record was deleted while a copy of it was still sitting in a phone's
 * retry queue, the next flush pushed it back into the cloud. Every delete now
 * writes the row id here, and each queue checks this list before pushing.
 *
 * URGENT PERFORMANCE FIX (2026-07-26 . TK reported the Follow-up Visit and
 * Patient tabs stuck on "Loading..." for hours):
 * The first version opened SharedPreferences, parsed the whole JSON list and
 * did a SYNCHRONOUS commit() on EVERY successful upsert. Those two tabs
 * self-heal rows while they load (dozens of upserts), so the screen was doing
 * dozens of blocking disk writes and could look frozen.
 * Now the list is kept in memory, disk is read only ONCE, the normal path
 * (nothing to clear) does NO disk work at all, and writes use apply()
 * (background) instead of commit().
 *
 * SAFETY: this only ever ADDS a skip check. If anything here fails,
 * isDeleted() returns false and every queue behaves exactly as before.
 * Stores ids only . no patient data, no amounts.
 *
 * 🔵🔒 V442 (19.08.2026, TK-approved): the old 3000-item cap was removed. Once
 * cloud tombstones crossed 3000, older delete markers could be forgotten and a
 * stale phone could re-upload a deleted row. The guard now keeps every known id
 * and cloud sync reads the table page-by-page.
 */
object DeletedGuard {

    private const val PREFS = "piles_clinic_deleted_guard"
    private const val KEY = "ids"
    private const val CLOUD_PAGE_SIZE = 1000

    @Volatile
    private var appContext: Context? = null

    /** In-memory copy; disk is read only once per app start. */
    private var cache: LinkedHashSet<String>? = null

    /**
     * 🔒🔒 খাতার সারি B167 (TK, 30.07.2026 — TK-এর ৪ নম্বর সন্দেহ):
     * *"Restore করার পরে অন্য ফোনের পুরনো Deleted চিহ্ন নিজে থেকে উঠছে না।"*
     *
     * নিচের `syncFromCloud()` এখন ক্লাউডে **আর নেই** এমন চিহ্ন তুলে দেয়, যাতে
     * এক ফোনে Restore করলে অন্য ফোনও জেনে যায়। কিন্তু তাতে **একটা মারাত্মক
     * ঝুঁকি** ছিল, আর এই তালিকাটাই সেই ঝুঁকির পাহারা:
     *
     * এই ফোনে নেট ছাড়া কিছু ডিলিট করা হলে চিহ্নটা ক্লাউডে **এখনো পৌঁছায়নি**।
     * তখন ক্লাউডের তালিকায় সেটা না পেয়ে যদি চিহ্নটা তুলে দেওয়া হত, তাহলে
     * **এই ফোনের নিজের ডিলিট করা রেকর্ড আবার ফিরে আসতে পারত** — TK-এর
     * চিরকালের নিয়ম *"ডিলিট করলে যেন আর ফিরে না আসে"* (সারি B138) ভেঙে যেত।
     *
     * তাই যে চিহ্নগুলো **এই ফোনে বসানো হয়েছে কিন্তু ক্লাউডে পৌঁছেছে কি না
     * নিশ্চিত নয়**, সেগুলো এখানে আলাদা করে রাখা হয় — ⛔ **ওগুলো কখনো তোলা
     * হয় না**। ক্লাউডে পৌঁছানো নিশ্চিত হলে তবেই এই তালিকা থেকে নাম কাটা যায়।
     */
    private const val KEY_LOCAL = "localOnly"
    private var localCache: LinkedHashSet<String>? = null

    /** Called once from PilesClinicApplication.onCreate. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** 🚨 খাতার সারি B34: মুছে ফেলার সময় ফোনের নিজের জমানো তালিকাও পরিষ্কার
     *  করতে হয়, আর সেখানে একটা Context লাগে। অ্যাপ চালু হওয়ার সময় যেটা এখানে
     *  রাখা হয়, সেটাই দেওয়া হয় — নতুন কিছু তৈরি করা হয় না। */
    fun appContextOrNull(): Context? = appContext

    private fun prefs(context: Context?): android.content.SharedPreferences? {
        val ctx = context?.applicationContext ?: appContext ?: return null
        return try { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) } catch (_: Throwable) { null }
    }

    @Synchronized
    private fun load(context: Context?): LinkedHashSet<String>? {
        val existing = cache
        if (existing != null) return existing
        val p = prefs(context) ?: return null
        val set = LinkedHashSet<String>()
        try {
            val arr = org.json.JSONArray(p.getString(KEY, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "")
                if (v.isNotBlank()) set.add(v)
            }
        } catch (_: Throwable) { }
        cache = set
        return set
    }

    @Synchronized
    private fun persist(context: Context?) {
        val p = prefs(context) ?: return
        val set = cache ?: return
        try {
            val arr = org.json.JSONArray()
            for (v in set) arr.put(v)
            p.edit().putString(KEY, arr.toString()).apply()
        } catch (_: Throwable) { }
    }

    private fun tag(table: String, id: String): String = table + "|" + id

    // ── 🔒 খাতার সারি B167 — "এই ফোনের নিজের, এখনো ক্লাউডে পৌঁছায়নি" তালিকা ──

    @Synchronized
    private fun loadLocal(context: Context?): LinkedHashSet<String>? {
        val existing = localCache
        if (existing != null) return existing
        val p = prefs(context) ?: return null
        val set = LinkedHashSet<String>()
        try {
            val arr = org.json.JSONArray(p.getString(KEY_LOCAL, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "")
                if (v.isNotBlank()) set.add(v)
            }
        } catch (_: Throwable) { }
        localCache = set
        return set
    }

    @Synchronized
    private fun persistLocal(context: Context?) {
        val p = prefs(context) ?: return
        val set = localCache ?: return
        try {
            val arr = org.json.JSONArray()
            for (v in set) arr.put(v)
            p.edit().putString(KEY_LOCAL, arr.toString()).apply()
        } catch (_: Throwable) { }
    }

    /** এই চিহ্নটা ক্লাউডে পৌঁছেছে — তাই আর "শুধু এই ফোনের" নয়। */
    @Synchronized
    private fun markPushed(tagValue: String) {
        try {
            val set = loadLocal(null) ?: return
            if (set.remove(tagValue)) persistLocal(null)
        } catch (_: Throwable) { }
    }

    /** Remembers that this row was deleted, so no queue may push it again. */
    @Synchronized
    fun markDeleted(table: String, id: String, context: Context? = null, byMobile: String = "") {
        if (table.isBlank() || id.isBlank()) return
        // 🔒 খাতার সারি B138: Restore করলে ক্লাউডের চিহ্নটা মোছা হয় — আর সেই
        // মোছার কাজটাও `deleteById` দিয়েই হয়, তাই এখানে না আটকালে "চিহ্ন
        // মোছার চিহ্ন" তৈরি হয়ে যেত (নিজের লেজ নিজে খাওয়া)। তালিকার নিজের
        // টেবিলটাকে কখনো চিহ্ন দেওয়া হয় না।
        if (table == TABLE) return
        try {
            val set = load(context) ?: return
            if (!set.add(tag(table, id))) return
            persist(context)
            // 🔒 খাতার সারি B167: চিহ্নটা ক্লাউডে পৌঁছেছে কি না এখনো জানা নেই,
            // তাই "শুধু এই ফোনের" তালিকাতেও নাম তোলা হয়। ⛔ ক্লাউডের তালিকায়
            // না পেলেও এই চিহ্নটা কখনো তোলা হবে না — নইলে নেট ছাড়া করা
            // ডিলিট আবার ফিরে আসতে পারত।
            try {
                val local = loadLocal(context)
                if (local != null && local.add(tag(table, id))) {
                    persistLocal(context)
                }
            } catch (_: Throwable) { }
            // 🔒 খাতার সারি B138: চিহ্নটা ক্লাউডেও পাঠানো হয়, যাতে অন্য ফোনও
            // জেনে যায় এবং কেউ সারিটা আর ফেরত পাঠাতে না পারে। পিছনে চলে,
            // ব্যর্থ হলেও ফোনের নিজের সুরক্ষা আগের মতোই থাকে।
            pushDeletedToCloud(table, id, byMobile)
        } catch (_: Throwable) { }
    }

    /**
     * Clears a tombstone . used when a record is restored from the Trash Bin,
     * and when a row with a re-usable id (e.g. "exp_<10 digits>") is created
     * again. Touches NO disk when the id is not in the list, which is the
     * normal case for almost every save.
     */
    @Synchronized
    fun unmark(table: String, id: String, context: Context? = null) {
        if (table.isBlank() || id.isBlank()) return
        // 🔒🔒 খাতার সারি B166 (TK-এর ৩ নম্বর সন্দেহ): ব্যর্থ Delete এখন কেন্দ্রীয়
        // তালিকায় জমা থাকে ও পরে নিজে থেকে পাঠানো হয়। **তাই একটা বিপদ তৈরি হত:**
        // ডিলিট ব্যর্থ হলো → তালিকায় "মুছে ফেলো" জমা রইল → এর মধ্যে মাস্টার
        // Trash থেকে রেকর্ডটা **Restore করলেন** → পরে ওই পুরনো "মুছে ফেলো" চলে
        // যেত এবং **ফেরানো রেকর্ডটা আবার মুছে যেত**।
        // ⛔ এই লাইনটা ঠিক সেটাই আটকায় — সারিটা ফিরে আসামাত্র তার অপেক্ষমাণ
        //    "মুছে ফেলো" তালিকা থেকে উঠে যায়। সবচেয়ে নতুন ইচ্ছেটাই জেতে।
        // ⛔ এটা নিচের `set.remove()`-এর **আগে**, কারণ যে ডিলিট ব্যর্থ হয়েছিল
        //    তার চিহ্ন এই তালিকায় কখনো বসেইনি (চিহ্ন বসে শুধু সফল ডিলিটে) —
        //    তাই নিচে গিয়ে ফাংশনটা আগেই ফিরে যেত, আর পাহারাটা চলত না।
        // ⛔ সাধারণ অবস্থায় (কোনো Delete আটকে নেই) `forget()` একটাও ফাইল
        //    ছোঁয় না, তাই রোজকার সেভ এক পলকও ধীর হয় না।
        try { CloudWriteQueue.forget("DELETE", table, id) } catch (_: Throwable) { }
        try {
            val set = load(context) ?: return
            if (set.isEmpty()) return
            if (!set.remove(tag(table, id))) return
            persist(context)
            // 🔒 খাতার সারি B167: Restore হয়ে গেছে — তাই "শুধু এই ফোনের"
            // তালিকা থেকেও নামটা তুলে দেওয়া হয়, নইলে ওটা চিরকাল পড়ে থাকত।
            try {
                val local = loadLocal(context)
                if (local != null && local.remove(tag(table, id))) persistLocal(context)
            } catch (_: Throwable) { }
            // 🔒 খাতার সারি B138: Restore করা হলে ক্লাউডের চিহ্নটাও তুলে দেওয়া হয়,
            // নইলে রেকর্ডটা ফিরে এলেও অন্য ফোন সেটাকে "মুছে ফেলা" ভেবে আটকে দিত।
            removeDeletedFromCloud(table, id)
        } catch (_: Throwable) { }
    }

    /** True only when this exact row is known to have been deleted. */
    @Synchronized
    fun isDeleted(table: String, id: String, context: Context? = null): Boolean {
        if (table.isBlank() || id.isBlank()) return false
        return try {
            val set = load(context) ?: return false
            if (set.isEmpty()) false else set.contains(tag(table, id))
        } catch (_: Throwable) {
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 🔒🔒 খাতার সারি B138 (TK, 29.07.2026 রাত ১০.২০):
    //     *"ডিলিট করলে যেন সেটা আর ফিরে না আসে — তার জন্য যেটা ব্যবস্থা
    //       করতে হবে আপনি সেটা করুন।"*
    //
    // **এতদিন যে ফাঁকটা ছিল:** উপরের তালিকাটা **শুধু ওই ফোনের ভিতরে** থাকত।
    // TK যে ফোনে ডিলিট করেছেন সেই ফোনই কেবল জানত। **অন্য কোনো স্টাফের ফোনে**
    // ওই সারির পুরনো কপি জমা বা কিউতে থেকে গেলে, পরে সেটা আবার ক্লাউডে উঠে
    // যেতে পারত — অর্থাৎ মুছে ফেলা জিনিস ফিরে আসার একটা রাস্তা খোলা ছিল।
    //
    // **এখন:** ডিলিটের চিহ্নটা **ক্লাউডেও** (`deleted_records` টেবিল) লেখা হয়,
    // আর প্রতিটা ফোন সেই তালিকাটা নামিয়ে নিজের তালিকার সঙ্গে মিলিয়ে নেয়।
    // তাই **যে ফোনেই ডিলিট হোক, সব ফোন জেনে যায়** এবং কেউ আর সেটাকে ক্লাউডে
    // ফেরত পাঠাতে পারে না।
    //
    // ⛔ **কোনো রোগীর তথ্য বা টাকা এখানে যায় না** — শুধু টেবিলের নাম ও সারির
    //    আইডি, কে ও কবে মুছেছেন।
    // ⛔ **Trash · Restore আগের মতোই** — Restore করলে চিহ্নটা ক্লাউড থেকেও
    //    উঠে যায় (`unmark`), তাই রেকর্ড আবার স্বাভাবিকভাবে ফিরে আসে।
    // ⛔ **সব কাজ best-effort** — নেট না থাকলে বা টেবিলটা না থাকলে কিছুই
    //    ভাঙবে না, অ্যাপ ঠিক আগের মতোই চলবে (শুধু আগের সুরক্ষাটুকু থাকবে)।
    // ⚡ **কোটা:** নামানোর কাজটা অ্যাপ খোলার পরে **৬ ঘণ্টায় একবারের বেশি নয়**,
    //    আর তাতে মাত্র একটা ঘর (`id`) নামে।
    // ─────────────────────────────────────────────────────────────────

    private const val TABLE = "deleted_records"
    private const val KEY_SYNC_AT = "cloudSyncAt"
    /* 📱🔒 V1292 (১০.০৯.২০২৬, তালিকা ৪১১-⑪ ক, TK: *"ক করুন, সাবধানে"*) — **ক্লাউডে নতুন
       কিছু মোছা না হলে পুরো তালিকা আর নামানো হয় না।** শেষ *সম্পূর্ণ* পড়ার সময়কার
       মোট-গোনা + সবচেয়ে নতুন id মনে রাখা হয়; পরের ঘণ্টায় শুধু ওই দুটো (২টা ছোট
       ডাক) মিলিয়ে দেখা — এক থাকলে পাতা-ধরে টানা বাদ। নতুন চিহ্ন ⇒ newest বদলায়,
       তোলা চিহ্ন ⇒ গোনা বদলায়; দুটোই একসাথে বদলে অমিল লুকোনোর তাত্ত্বিক সম্ভাবনা
       ঢাকতে **দিনে অন্তত একবার** পুরো পড়া হয়ই। ⛔ চিহ্ন যোগ/তোলার নিয়ম অছোঁয়া। */
    private const val KEY_LAST_FULL_COUNT = "lastFullCount"
    private const val KEY_LAST_FULL_HEAD = "lastFullHead"
    private const val KEY_LAST_FULL_AT = "lastFullAt"
    private const val FULL_READ_MAX_GAP_MS = 24L * 60L * 60L * 1000L
    // 🚨 খাতার সারি B145 (TK, 30.07.2026): আগে ছিল **৬ ঘণ্টা**। ওই ৬ ঘণ্টার
    // ফাঁকে অন্য ফোনের পুরনো অপেক্ষমাণ কপি মুছে ফেলা সারিটাকে আবার ক্লাউডে
    // তুলে দিতে পারত। এখন **১ ঘণ্টা** — ফাঁকটা ৬ গুণ ছোট।
    // ⚡ কোটার উপর প্রভাব প্রায় শূন্য: নামে শুধু একটা ঘর (`id`), আর দিনে
    //    সর্বোচ্চ ২৪ বার।
    private const val SYNC_GAP_MS = 1L * 60L * 60L * 1000L

    /** ডিলিটের চিহ্ন ক্লাউডে পাঠানো (পিছনে, কাউকে অপেক্ষা করায় না)। */
    fun pushDeletedToCloud(table: String, id: String, byMobile: String) {
        if (table.isBlank() || id.isBlank()) return
        if (table == TABLE) return   // 🔒 খাতার সারি B138 — নিজের টেবিল নয়
        // 🔒 খাতার সারি B166: এই চিহ্নটা আগে একবার Restore-এর সময় মোছা হয়ে
        // থাকতে পারে, আর সেই "মুছে ফেলো" কাজটা তালিকায় আটকে থাকতে পারে।
        // এখন যেহেতু চিহ্নটা **নতুন করে বসানো হচ্ছে**, ওই পুরনো "মুছে ফেলো"
        // তালিকা থেকে তুলে দিতে হবে — নইলে পরে সেটা চলে গিয়ে **নতুন চিহ্নটাই
        // মুছে দিত** এবং অন্য ফোনগুলো ডিলিটের খবর পেত না।
        try { CloudWriteQueue.forget("DELETE", TABLE, tag(table, id)) } catch (_: Throwable) { }
        try {
            Thread {
                try {
                    val row = org.json.JSONObject()
                        .put("id", tag(table, id))
                        .put("tableName", table)
                        .put("rowId", id)
                        .put("deletedBy", byMobile)
                        .put("deletedAt", java.text.SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US
                        ).format(java.util.Date()))
                    val ok = SupabaseClient.upsert(TABLE, row)
                    // 🔒 খাতার সারি B167: ক্লাউডে সত্যিই পৌঁছেছে — এখন থেকে এটা
                    // আর "শুধু এই ফোনের" চিহ্ন নয়। পরে কেউ Restore করলে
                    // ক্লাউডের তালিকা দেখেই এই ফোন সেটা জেনে যাবে।
                    // ⛔ ব্যর্থ হলে নাম তালিকায় থেকেই যায়, তাই চিহ্নটা কখনো
                    //    ভুল করে তোলা হবে না।
                    if (ok) markPushed(tag(table, id))
                } catch (_: Throwable) { }
            }.start()
        } catch (_: Throwable) { }
    }

    /** Restore করলে চিহ্নটা ক্লাউড থেকেও তুলে দেওয়া হয়। */
    fun removeDeletedFromCloud(table: String, id: String) {
        if (table.isBlank() || id.isBlank()) return
        try {
            Thread {
                try { SupabaseClient.deleteById(TABLE, tag(table, id)) } catch (_: Throwable) { }
            }.start()
        } catch (_: Throwable) { }
    }

    /**
     * ক্লাউডের তালিকাটা নামিয়ে নিজের তালিকার সঙ্গে মিলিয়ে নেওয়া।
     *
     * 🔒🔒 খাতার সারি B167 (TK, 30.07.2026 — TK-এর ৪ নম্বর সন্দেহ):
     * *"Restore করার পরে অন্য ফোনের পুরনো Deleted চিহ্ন নিজে থেকে উঠছে না। ফল:
     *  কোনো ফোনে restored record দেখা গেলেও অন্য ফোন সেটিকে Deleted ধরে আটকে
     *  রাখতে পারে।"* — **সত্যি ছিল।** আগে এখানে চিহ্ন শুধু **যোগ** হত, কখনো
     * বাদ যেত না। তাই এক ফোনে Restore করলে সেই খবর অন্য ফোনে কোনোদিন পৌঁছাত না।
     *
     * **এখন:** ক্লাউডের তালিকায় **আর নেই** এমন চিহ্ন এই ফোন থেকেও উঠে যায়।
     *
     * ⛔ **তিনটে পাহারা, নইলে মুছে ফেলা রেকর্ড ফিরে আসতে পারত:**
     *  ১. এই ফোনে বসানো যে চিহ্ন **ক্লাউডে পৌঁছেছে কি না নিশ্চিত নয়**, সেটা
     *     কখনো তোলা হয় না (উপরের `KEY_LOCAL` তালিকা)।
     *  ২. ক্লাউডের সব page পড়ে শুরু/শেষের exact count + newest id মিলিয়ে
     *     উত্তর **সম্পূর্ণ ও স্থির** কি না প্রমাণ করা হয়। প্রমাণ না হলে **একটাও
     *     চিহ্ন তোলা হয় না** (শুধু পাওয়া tombstone যোগ হয়)।
     *  ৩. উত্তর না এলে আগের মতোই চুপচাপ ফিরে যাওয়া হয়, কিছুই বদলায় না।
     */
    fun syncFromCloud(context: Context?) {
        try {
            val p = prefs(context) ?: return
            val now = System.currentTimeMillis()
            val last = try { p.getLong(KEY_SYNC_AT, 0L) } catch (_: Throwable) { 0L }
            if (last > 0L && now - last < SYNC_GAP_MS) return
            // 🔴🔴🔒 V451 (TK-নির্দেশ ১৮.০৮.২০২৬, Supabase Logs ধরে যাচাই —
            // "কেন 400 বারবার আসছে")। **আসল কারণ:** `fetchListSlimOrNull`-এর
            // ডিফল্ট sort ঘর `updatedAt` — কিন্তু `deleted_records` টেবিলে এই
            // ঘরটাই নেই (আছে শুধু id/tableName/rowId/deletedBy/deletedAt),
            // তাই প্রতিটা ডাক 400 (Bad Request) দিয়ে ব্যর্থ হতো, চুপচাপ।
            // **এর মানে:** এতদিন এই cross-device tombstone-sync আসলে কখনো
            // সফল হয়নি — এক ফোনে Delete করলে অন্য ফোন সেই খবর ক্লাউড থেকে
            // কখনো পায়নি (আজকের B638-এর "ভুতুড়ে ফিরে আসা" আলোচনার সাথে
            // সরাসরি সম্পর্কিত)। **সমাধান:** আসল কলাম `deletedAt` দিয়ে sort।
            // 🔵🔵🔒 V442 (19.08.2026, TK-approved — Free Plan safety):
            // OLD RISK: cloud read stopped at 3000, and the phone cache also dropped
            // older markers after 3000. A stale/reinstalled phone could therefore
            // forget an intentional delete and push that row back.
            //
            // NOW: read only `id`, 1000 rows at a time until the real end. Before
            // removing any local marker (Restore propagation), prove that the cloud
            // stayed stable during the multi-page read: exact count and newest id
            // must be identical before/after, and unique ids read must equal count.
            // If network/cloud changes mid-read, NOTHING is unmarked; discovered
            // tombstones are only added. This deliberately fails closed.
            val countBefore = SupabaseClient.fetchCount(TABLE)
            val headBefore = SupabaseClient.fetchListSlimOrNull(
                TABLE, null, 1, "id", order = "deletedAt.desc.nullslast,id.asc"
            ) ?: return
            val newestBefore = headBefore.optJSONObject(0)?.optString("id", "").orEmpty()

            // 📱 V1292 (⑪ ক): শেষ সম্পূর্ণ পড়ার পরে ক্লাউডে কিছু বদলায়নি (গোনা + newest এক)
            // এবং ২৪ ঘণ্টা পার হয়নি ⇒ পুরো তালিকা নামানো বাদ। চিহ্ন-তালিকা যেমন ছিল তেমনই।
            try {
                val lastCount = p.getInt(KEY_LAST_FULL_COUNT, -1)
                val lastHead = p.getString(KEY_LAST_FULL_HEAD, null)
                val lastFullAt = p.getLong(KEY_LAST_FULL_AT, 0L)
                if (countBefore >= 0 && lastCount >= 0 && lastHead != null &&
                    countBefore == lastCount && newestBefore == lastHead &&
                    lastFullAt > 0L && now - lastFullAt < FULL_READ_MAX_GAP_MS) {
                    p.edit().putLong(KEY_SYNC_AT, now).apply()
                    return
                }
            } catch (_: Throwable) { }

            val fromCloud = HashSet<String>()
            var offset = 0
            var reachedEnd = false
            while (true) {
                val page = SupabaseClient.fetchListSlimOrNull(
                    TABLE, null, CLOUD_PAGE_SIZE, "id",
                    order = "deletedAt.desc.nullslast,id.asc", offset = offset
                ) ?: return
                var pageNewIds = 0
                for (i in 0 until page.length()) {
                    val v = page.optJSONObject(i)?.optString("id", "").orEmpty()
                    if (v.isNotBlank() && fromCloud.add(v)) pageNewIds++
                }
                // If a server/proxy ever ignored offset and repeated the same page,
                // stop fail-closed instead of looping forever. No marker is unmarked.
                if (offset > 0 && page.length() > 0 && pageNewIds == 0) break
                // Do not assume a short page means "the end": PostgREST/Supabase can
                // impose a server-side row cap lower than our requested page size.
                // An actual empty page proves there is no next offset page.
                if (page.length() == 0) {
                    reachedEnd = true
                    break
                }
                // Defensive overflow guard: if this ever happened, do not unmark anything.
                if (offset > Int.MAX_VALUE - page.length()) break
                offset += page.length()
            }

            val countAfter = SupabaseClient.fetchCount(TABLE)
            val headAfter = SupabaseClient.fetchListSlimOrNull(
                TABLE, null, 1, "id", order = "deletedAt.desc.nullslast,id.asc"
            ) ?: return
            val newestAfter = headAfter.optJSONObject(0)?.optString("id", "").orEmpty()

            val complete = reachedEnd && countBefore >= 0 && countAfter >= 0 &&
                countBefore == countAfter && fromCloud.size == countAfter &&
                newestBefore == newestAfter

            synchronized(this) {
                val set = load(context) ?: return
                var added = false
                for (v in fromCloud) {
                    if (set.add(v)) added = true
                }
                var removed = false
                if (complete) {
                    // 🔒 পাহারা ১ — এই ফোনের নিজের, এখনো না-পৌঁছানো চিহ্নগুলো বাদ।
                    val mine = loadLocal(context) ?: LinkedHashSet<String>()
                    val it = set.iterator()
                    while (it.hasNext()) {
                        val v = it.next()
                        if (fromCloud.contains(v)) continue
                        if (mine.contains(v)) continue
                        it.remove()
                        removed = true
                    }
                }
                // V442: no size-pruning here. Every known tombstone remains guarded.
                if (added || removed) persist(context)
            }
            // 📱 V1292: শুধু *সম্পূর্ণ* পড়া হলে তার ছাপ মনে রাখা (নইলে পরের বার আবার পুরো পড়া)।
            try {
                if (complete) p.edit().putInt(KEY_LAST_FULL_COUNT, countAfter).putString(KEY_LAST_FULL_HEAD, newestAfter).putLong(KEY_LAST_FULL_AT, now).apply()
                else p.edit().remove(KEY_LAST_FULL_COUNT).remove(KEY_LAST_FULL_HEAD).remove(KEY_LAST_FULL_AT).apply()
            } catch (_: Throwable) { }
            try { p.edit().putLong(KEY_SYNC_AT, now).apply() } catch (_: Throwable) { }
        } catch (_: Throwable) { }
    }
}
