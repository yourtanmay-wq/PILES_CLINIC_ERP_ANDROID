package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONObject

/**
 * TK-REQUESTED (2026-07-27): "প্রতিটা স্টাফ চেম্বার বন্ধ করে বাড়ি চলে গেছে কিন্তু
 * এখনো এখানে এগুলো শো করছে" — so from 7 PM a staff is reminded until that
 * branch's chamber is closed.
 *
 * Until now nothing recorded that a chamber HAD been closed (Close Chamber only
 * printed the register), so with TK's permission a small table was added:
 * 04_SUPABASE_DATABASE_SETUP/PATCH_2026-07-27_chamber_close.sql
 * One row per branch per day, id = "BRANCH|yyyy-MM-dd".
 *
 * Two things are deliberate here:
 *  1. The mark is ALSO remembered on the phone that closed it. So the reminder
 *     stops at once even if the line is down, and it can never start ringing
 *     again just because a cloud read failed.
 *  2. A failed write is retried from this class's own list -- NOT from the
 *     generic retry queue, because that queue replays with a PATCH and a PATCH
 *     on a row that does not exist yet reports success while writing nothing;
 *     the mark would have been lost silently and the phone would ring all night.
 */
object ChamberCloseRepository {

    private const val TABLE = "chamber_close"
    private const val PREF = "chamber_close_local"
    private const val KEY_DONE = "closed_ids"
    private const val KEY_PENDING = "pending_rows"

    fun idOf(branch: String, date: String): String =
        branch.trim().uppercase() + "|" + date.trim()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /**
     * 🚨🚨 খাতার সারি B170 (TK, 30.07.2026 — TK-এর ৭ নম্বর সন্দেহ):
     * *"Chamber Close-এর pending কাজ বর্তমান সতর্কবাতির হিসাবে পুরোপুরি ধরা
     *  হচ্ছে না। কিছু worker কেন্দ্রীয় pending তালিকাও পরীক্ষা করে না। ফল: App
     *  সবুজ Synced দেখাতে পারে, অথচ ভিতরে একটি কাজ আটকে থাকতে পারে।"*
     *
     * **সত্যি ছিল।** এই তালিকাটা (`chamber_close_local`-এর `pending_rows`)
     * `PendingSyncStatus.summary()`-এ কখনো গোনা হত না — তাই হোম পেজের লাল
     * বাতিতে এই কাজ আটকে থাকলেও সংখ্যাটা ধরা পড়ত না, "পাঠান" বোতামও এটা
     * পাঠাত না, আর `BottomNav.retryStuckSaves`-ও এটা ছুঁতোই না
     * (`BackgroundRefreshWorker`-এই শুধু ছিল)।
     *
     * এই ফাংশনটা শুধু **গোনে** — কিছু লেখে না, বদলায় না।
     */
    fun pendingCount(context: Context): Int = try {
        JSONObject(prefs(context).getString(KEY_PENDING, "{}") ?: "{}").length()
    } catch (_: Throwable) { 0 }

    /** Marks this branch's chamber closed for this date (cloud + this phone). */
    fun markClosed(context: Context?, branch: String, date: String, user: NativeUser?): Boolean {
        if (branch.isBlank() || date.isBlank()) return false
        val id = idOf(branch, date)
        val now = try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
        } catch (_: Exception) { "" }
        val row = JSONObject()
            .put("id", id)
            .put("branch", branch.trim().uppercase())
            .put("date", date.trim())
            .put("closedBy", user?.mobile ?: "")
            .put("closedByName", user?.name ?: "")
            .put("closedAt", now)
            .put("createdAt", now)
            .put("updatedAt", now)

        // remember it on this phone first, so the reminder stops immediately
        if (context != null) rememberLocally(context, id)

        val ok = try { SupabaseClient.upsert(TABLE, row) } catch (_: Exception) { false }
        if (!ok && context != null) keepForRetry(context, id, row)
        return ok
    }

    /**
     * শুধু এই ফোনের জানা তথ্য — **কোনো নেট লাগে না।**
     * (নতুন, 28.07.2026 · খাতার সারি B46: "চেম্বার বন্ধ করুন" তালিকায় প্রতিটা
     * দিনের জন্য আলাদা করে ক্লাউডে যাওয়া চলবে না, তাই এই সস্তা যাচাইটা লাগে।)
     */
    fun isClosedLocally(context: Context?, branch: String, date: String): Boolean {
        if (context == null || branch.isBlank() || date.isBlank()) return false
        return knownLocally(context, idOf(branch, date))
    }

    /** True if this phone already knows the chamber was closed, or the cloud says so. */
    fun isClosed(context: Context?, branch: String, date: String): Boolean {
        if (branch.isBlank() || date.isBlank()) return false
        val id = idOf(branch, date)
        if (context != null && knownLocally(context, id)) return true
        return try {
            val key = java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20")
            val rows = SupabaseClient.fetchListOrNull(TABLE, "id=eq.$key", 1) ?: return false
            val found = rows.length() > 0
            if (found && context != null) rememberLocally(context, id)
            found
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 🆕🔴 TK-নির্দেশ (07.08.2026) — **লোকাল ক্যাশ উপেক্ষা করে সরাসরি ক্লাউডে**
     * যাচাই করে এই দিনটা সত্যিই বন্ধ কি না।
     *
     * **কেন দরকার:** উপরের `isClosed()` ফোনের নিজস্ব "বন্ধ" চিহ্ন পেলেই সঙ্গে
     * সঙ্গে `true` ফেরত দেয়, ক্লাউডে জিজ্ঞাসাই করে না (খরচ বাঁচাতে)। ফলে
     * Master অনুমোদন দিয়ে দিনটা আবার খুলে দিলেও, যে ফোন বন্ধ করেছিল সেখানে
     * বড়জোর ৩০ মিনিট (`CACHE_TTL_MS`) পর্যন্ত "বন্ধ"-ই দেখাত — অথচ রোগী তখন
     * সামনে দাঁড়িয়ে। এখন ব্যবহারকারী নিজে তালিকা নিচে টেনে refresh করলে এই
     * ফাংশনটা একবার ডাকা হয়।
     *
     * ⛔ **নিরাপদ:** নেট ব্যর্থ হলে `null` ফেরত — তখন কল-সাইট আগের অবস্থাই ধরে
     *    রাখে (ভুল করে "খোলা" দেখিয়ে দেয় না)। ক্লাউড "খোলা" বললে তবেই ফোনের
     *    চিহ্ন মোছা হয়, "বন্ধ" বললে চিহ্ন আবার বসে।
     * ⛔ খরচ: শুধু হাতে-টানা refresh-এ একবার — নিজে থেকে কখনো নয়।
     */
    fun isClosedFromCloud(context: Context?, branch: String, date: String): Boolean? {
        if (branch.isBlank() || date.isBlank()) return null
        val id = idOf(branch, date)
        return try {
            val key = java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20")
            val rows = SupabaseClient.fetchListOrNull(TABLE, "id=eq.$key", 1) ?: return null
            val found = rows.length() > 0
            if (context != null) {
                if (found) rememberLocally(context, id) else forgetLocally(context, id)
            }
            found
        } catch (_: Exception) { null }
    }

    // 🆕 B419 (04.08.2026, TK-নির্দেশ — "ভুল করে বন্ধ হলে আবার খোলা যাবে?")
    // — চেম্বার আবার খুলে দেয়: ক্লাউডের `chamber_close` সারিটা মুছে দেওয়া হয়
    // ও **এই ফোনের** নিজস্ব ক্যাশ থেকেও সরানো হয় (তাই যে ফোনে Approve করা
    // হলো সেই ফোনেই সঙ্গে সঙ্গে "খোলা" দেখাবে)।
    // ⛔ **অন্য ফোনগুলোর (যেমন যে স্টাফ বন্ধ করেছিলেন) নিজস্ব ক্যাশ** এখান
    // থেকে সরাসরি সরানো সম্ভব না (প্রতিটা ফোনের নিজস্ব স্থানীয় সংরক্ষণ,
    // দূর থেকে ছোঁয়া যায় না) — তাই নিচের `rememberLocally()`/`knownLocally()`
    // এখন থেকে **সময়-সহ** মনে রাখে; ৩০ মিনিটের পুরনো ক্যাশ আর বিশ্বাস করা
    // হয় না, ক্লাউডে আবার যাচাই হয় — তাই reopen হলে অন্য ফোনেও বড়জোর
    // ৩০ মিনিটের মধ্যেই "খোলা" ধরা পড়বে (সঙ্গে সঙ্গে না হলেও, নিজে থেকেই)।
    fun reopen(context: Context?, branch: String, date: String): Boolean {
        if (branch.isBlank() || date.isBlank()) return false
        val id = idOf(branch, date)
        val key = try { java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20") } catch (_: Throwable) { id }
        val ok = try { SupabaseClient.deleteById(TABLE, key) } catch (_: Throwable) { false }
        if (context != null) forgetLocally(context, id)
        return ok
    }


    /** Retries any close-mark that could not reach the cloud earlier. */
    fun flushPending(context: Context) {
        try {
            val p = prefs(context)
            val raw = p.getString(KEY_PENDING, "{}") ?: "{}"
            val obj = JSONObject(raw)
            if (obj.length() == 0) return
            val left = JSONObject()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val row = obj.optJSONObject(k) ?: continue
                val ok = try { SupabaseClient.upsert(TABLE, row) } catch (_: Exception) { false }
                if (!ok) left.put(k, row)
            }
            p.edit().putString(KEY_PENDING, left.toString()).apply()
        } catch (_: Exception) {}
    }

    // 🆕 B419 (04.08.2026) — এই দুটো ফাংশন আগে শুধু id-র একটা Set রাখত
    // (চিরস্থায়ী ক্যাশ, কখনো নিজে থেকে পুরনো হতো না)। এখন id→সময় (JSON)
    // রাখা হয় — যাতে reopen-এর পরে অন্য ফোনগুলোও একটা সময়ের মধ্যে
    // (৩০ মিনিট) নিজে থেকে আবার ক্লাউডে যাচাই করে সত্যিটা জানতে পারে।
    // ⛔ পুরনো ফরম্যাটের ডেটা (আগের ভার্সনের ফোনে) পড়া না গেলে খালি সেট
    //    ধরে নেওয়া হয় (নিরাপদ ডিফল্ট — বড়জোর একবার বাড়তি ক্লাউড-কল হবে,
    //    কোনো ভুল তথ্য দেখাবে না)।
    private const val CACHE_TTL_MS = 30 * 60 * 1000L

    private fun rememberLocally(ctx: Context, id: String) {
        try {
            val p = prefs(ctx)
            val obj = try { JSONObject(p.getString(KEY_DONE, "{}") ?: "{}") } catch (_: Throwable) { JSONObject() }
            obj.put(id, System.currentTimeMillis())
            // keep it small: only the last 60 marks are of any use
            if (obj.length() > 60) {
                val keys = obj.keys().asSequence().toList()
                    .sortedByDescending { obj.optLong(it, 0L) }.take(60)
                val trimmed = JSONObject()
                for (k in keys) trimmed.put(k, obj.optLong(k, 0L))
                p.edit().putString(KEY_DONE, trimmed.toString()).apply()
            } else {
                p.edit().putString(KEY_DONE, obj.toString()).apply()
            }
        } catch (_: Exception) {}
    }

    private fun knownLocally(ctx: Context, id: String): Boolean = try {
        val obj = JSONObject(prefs(ctx).getString(KEY_DONE, "{}") ?: "{}")
        val at = if (obj.has(id)) obj.optLong(id, 0L) else -1L
        at > 0 && (System.currentTimeMillis() - at) < CACHE_TTL_MS
    } catch (_: Exception) { false }

    private fun forgetLocally(ctx: Context, id: String) {
        try {
            val p = prefs(ctx)
            val obj = try { JSONObject(p.getString(KEY_DONE, "{}") ?: "{}") } catch (_: Throwable) { JSONObject() }
            obj.remove(id)
            p.edit().putString(KEY_DONE, obj.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun keepForRetry(ctx: Context, id: String, row: JSONObject) {
        try {
            val p = prefs(ctx)
            val obj = JSONObject(p.getString(KEY_PENDING, "{}") ?: "{}")
            obj.put(id, row)
            p.edit().putString(KEY_PENDING, obj.toString()).apply()
        } catch (_: Exception) {}
    }
}
