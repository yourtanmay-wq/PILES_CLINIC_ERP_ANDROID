package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🔒 TK'S PERMANENT RULE (28.07.2026): "আমি আমার ফোনে যা যা কাজ করবো, সেটা যেন
 * সাথে সাথেই দেখায়" -- whatever THIS phone saved is always shown in the list
 * on THIS phone. The cloud may only ADD to that list, never take away from it.
 *
 * WHAT WAS STILL MISSING (khata row B25, part 1)
 * Seven lists in the app show a stored (cached) copy first, then refresh from
 * the cloud. Four of them already merge this phone's own work back in
 * (Follow-up, Today's Collection, CHECK-UP Queue, Draft). THREE DID NOT:
 * Doctor/RMP, Chamber Board and Medicine History. On a weak line the stored
 * list is exactly what stays on screen -- so a doctor just added, a call
 * remark just written, or a medicine sale just taken was simply not there,
 * and the staff went looking for it: TK's own words, "ডাক্তারের নাম খুঁজে
 * পাচ্ছি না", "রিমার্ক লিখছি হয়ে গেছে দেখায়, পরে পুরনোটাই থাকে".
 *
 * WHAT THIS IS
 * One small note-book of the rows THIS phone wrote, per table. A list asks
 * for it with overlay() and gets its own rows back with this phone's writing
 * put on top. Nothing else about any screen changes.
 *
 * WHY IT IS SAFE
 *  - Display only. Not one save path, amount or rule is touched here. The
 *    actual sending to the cloud is unchanged and still handled where it
 *    always was (CloudWriteQueue retries a failed write by itself).
 *  - An entry retires itself. As soon as the cloud copy of that row is
 *    equally new or newer (updatedAt), the note is dropped -- so another
 *    branch's newer edit is never held back by an old note on this phone.
 *  - Anything older than seven days is dropped no matter what.
 *  - Only the fields this phone actually wrote are put on top; every other
 *    field (money included) stays exactly as the list already had it, so a
 *    Bill/Paid figure can never be blanked or zeroed by this.
 *  - If anything at all goes wrong in here, the caller gets its original
 *    list back untouched -- this can never block or empty a screen.
 */
object MyPhoneWrites {

    private const val PREF = "my_phone_writes_v1"
    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    private const val MAX_ROWS = 200

    /** Same reason as LocalWorkflowStore's shared lock: read-modify-write. */
    private val LOCK = Any()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun load(context: Context, table: String): JSONArray =
        try { JSONArray(prefs(context).getString(table, "[]") ?: "[]") } catch (_: Throwable) { JSONArray() }

    private fun store(context: Context, table: String, rows: JSONArray) {
        // 🚨 TK-REPORTED, চালু ক্লিনিক (28.07.2026, খাতার সারি B27): overlay()
        // পর্দা থেকেই (মেইন থ্রেড) ডাকা হয়, তাই এখানে ডিস্কে লেখা শেষ হওয়ার
        // জন্য অপেক্ষা করা যাবে না — ওটাই পর্দা আটকে দেয়।
        // `apply()` লেখাটা **সঙ্গে সঙ্গে মেমরিতে** বসায় (তাই পরের পড়াগুলো
        // এখনই নতুন লেখাটাই পায়, কিছুই বদলায় না) আর ডিস্কের কাজটা পিছনে করে।
        // ⛔ এটা শুধু দেখানোর নোটবই — আসল সেভ `LocalWorkflowStore`-এ যায়, সেখানে
        // ডিস্কে লেখার অপেক্ষা আগের মতোই অক্ষত, তাই কিছু হারানোর প্রশ্নই নেই।
        try { prefs(context).edit().putString(table, rows.toString()).apply() } catch (_: Throwable) { }
    }

    /**
     * Remembers that this phone wrote [fields] onto row [id] of [table].
     * Called after a save. Writing the same row again merges field by field,
     * so an earlier write is never lost by a later, smaller one.
     */
    fun remember(context: Context?, table: String, id: String, fields: JSONObject) {
        val ctx = context ?: return
        if (table.isBlank() || id.isBlank()) return
        try {
            synchronized(LOCK) {
                val rows = load(ctx, table)
                val now = System.currentTimeMillis()
                val kept = JSONArray()
                var entry: JSONObject? = null
                for (i in 0 until rows.length()) {
                    val e = rows.optJSONObject(i) ?: continue
                    if (e.optString("_id") == id) { entry = e; continue }
                    if (now - e.optLong("_at", 0L) > MAX_AGE_MS) continue
                    kept.put(e)
                }
                val merged = entry ?: JSONObject()
                val keys = fields.keys()
                while (keys.hasNext()) { val k = keys.next(); merged.put(k, fields.opt(k)) }
                merged.put("_id", id).put("_at", now)
                kept.put(merged)
                val trimmed = if (kept.length() > MAX_ROWS) {
                    val t = JSONArray()
                    for (i in (kept.length() - MAX_ROWS) until kept.length()) t.put(kept.get(i))
                    t
                } else kept
                store(ctx, table, trimmed)
            }
        } catch (_: Throwable) { }
    }

    /**
     * Returns [rows] with this phone's own writing put on top: a row already
     * in the list gets this phone's fields applied, a row not in the list at
     * all is added. [idField] is the column that holds the row id. Set
     * [addNewAtTop] for a newest-first list, so a brand-new row of this
     * phone's own does not land at the bottom of it.
     */
    fun overlay(context: Context?, table: String, rows: JSONArray, idField: String = "id", addNewAtTop: Boolean = false): JSONArray {
        val ctx = context ?: return rows
        return try {
            synchronized(LOCK) {
                val mine = load(ctx, table)
                if (mine.length() == 0) return rows
                val out = JSONArray()
                val added = JSONArray()
                val position = HashMap<String, Int>()
                for (i in 0 until rows.length()) {
                    val r = rows.optJSONObject(i) ?: continue
                    val rid = r.optString(idField)
                    if (rid.isNotBlank()) position[rid] = out.length()
                    out.put(r)
                }
                val keep = JSONArray()
                val now = System.currentTimeMillis()
                var dropped = false
                for (i in 0 until mine.length()) {
                    val e = mine.optJSONObject(i) ?: continue
                    val id = e.optString("_id")
                    if (id.isBlank() || now - e.optLong("_at", 0L) > MAX_AGE_MS) { dropped = true; continue }
                    val at = position[id]
                    if (at != null) {
                        val cloud = out.getJSONObject(at)
                        // The cloud has caught up (or is ahead) -- this note has
                        // done its job and must not hold an older text on screen.
                        val cloudAt = cloud.s("updatedAt")
                        val mineAt = e.s("updatedAt")
                        if (cloudAt.isNotBlank() && mineAt.isNotBlank() && cloudAt >= mineAt) { dropped = true; continue }
                        val copy = JSONObject(cloud.toString())
                        val keys = e.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            if (k == "_id" || k == "_at") continue
                            copy.put(k, e.opt(k))
                        }
                        out.put(at, copy)
                    } else {
                        val copy = JSONObject(e.toString())
                        copy.remove("_id")
                        copy.remove("_at")
                        if (copy.optString(idField).isBlank()) copy.put(idField, id)
                        added.put(copy)
                    }
                    keep.put(e)
                }
                if (dropped) store(ctx, table, keep)
                if (added.length() == 0) return out
                val ordered = JSONArray()
                if (addNewAtTop) {
                    for (i in added.length() - 1 downTo 0) ordered.put(added.get(i))
                    for (i in 0 until out.length()) ordered.put(out.get(i))
                } else {
                    for (i in 0 until out.length()) ordered.put(out.get(i))
                    for (i in 0 until added.length()) ordered.put(added.get(i))
                }
                ordered
            }
        } catch (_: Throwable) { rows }
    }

    /** Drops the note for one row (used when that row is deleted on purpose). */
    fun forget(context: Context?, table: String, id: String) {
        val ctx = context ?: return
        if (table.isBlank() || id.isBlank()) return
        try {
            synchronized(LOCK) {
                val rows = load(ctx, table)
                val kept = JSONArray()
                for (i in 0 until rows.length()) {
                    val e = rows.optJSONObject(i) ?: continue
                    if (e.optString("_id") == id) continue
                    kept.put(e)
                }
                store(ctx, table, kept)
            }
        } catch (_: Throwable) { }
    }
}
