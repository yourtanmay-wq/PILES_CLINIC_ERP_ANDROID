package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🌐💰🔒 V492 (২০.০৮.২০২৬, TK-নির্দেশ) — **ঘন্টার গোনায় আর পুরো টেবিল নামে না।**
 *
 * ─── TK-এর প্রমাণ (Supabase-এর ছবি, ২০.০৮.২০২৬) ────────────────────────────
 *   Egress            : ৪.৫৮৫ / ৫ GB  (৯২%)  🔴
 *   Database Size     : ০.০৬৬ / ০.৫ GB (১৩%)
 *   রোজকার ডাউনলোড    : ৩৮০ – ৭০০ MB
 *
 * অর্থাৎ **পুরো ডেটাবেস মাত্র ৬৬ MB, অথচ রোজ প্রায় ৯–১০ বার করে নামছে।**
 * তথ্য বেশি নয় — একই তথ্য বারবার নামছে।
 *
 * ─── আসল অপরাধী (কোড ধরে খুঁজে বের করা, আন্দাজ নয়) ────────────────────────
 * `PilesClinicApplication` প্রতি **১৫ মিনিটে** `BackgroundRefreshWorker`
 * চালায় → সেটা `BellCounter.count()` ডাকে → সেটা
 * `BriefingRepository.fetchRawForCount()` ডাকে → সেটা **পুরো `briefings`
 * টেবিল** (৫০০০ সারি পর্যন্ত) নামাত।
 *
 * ১৬টা ফোন × দিনে ৯৬ বার × পুরো টেবিল — এটাই Egress-এর সবচেয়ে বড় অংশ।
 * (V490-এর নতুন স্বয়ংক্রিয় নোটিশ যোগ হওয়ায় টেবিলটা এখন রোজ বাড়ে, তাই
 *  আগে যে "কিছু বদলায়নি ⇒ থেমে যাও" রক্ষাকবচ ছিল সেটাও আর কাজে লাগছিল না।)
 *
 * ─── এখন কী হয় ────────────────────────────────────────────────────────────
 * প্রথমবার একবার পুরোটা নামে। তারপর থেকে শুধু **`updatedAt` বদলেছে এমন
 * সারিগুলোই** নামে — সাধারণত ০ থেকে কয়েকটা। বাকিটা ফোনেই জমানো থাকে।
 *
 * ⚠️ এটা প্রজেক্টের **নতুন কোনো কৌশল নয়** — Follow-up · Doctor Queue ·
 *    Chamber-এ ঠিক এই একই `updatedAt=gt.` পদ্ধতি আগে থেকেই চলছে ও প্রমাণিত।
 *
 * ─── 🔒 কেন এটা নিরাপদ (প্রতিটা যাচাই করে দেখা) ───────────────────────────
 *  ১. **গোনার নিয়ম এক অক্ষরও বদলায়নি।** এই ফাইল শুধু *সারিগুলো জোগাড়* করে;
 *     কে দেখবে · কী গোনা হবে — সেই সিদ্ধান্ত আগের মতোই `visibleForUser()` ও
 *     `unseenCount()`-এর হাতে। খাতার সারি B60-এর নিয়ম অটুট।
 *  ২. **ঘরগুলো হুবহু একই** (`COLS`) — যা আগে নামত, তাই নামে।
 *  ৩. **সারি সত্যিকারের মুছে ফেলা হয় না।** কোড ধরে মিলিয়ে দেখা হয়েছে:
 *     ফোন ও ওয়েব — কোথাও `briefings` থেকে হার্ড-ডিলিট নেই। Master মুছলে
 *     `deletedAt` বসে (সেটা একটা *আপডেট*, তাই delta-তে ধরা পড়ে), আর
 *     স্টাফ মুছলে `hiddenFor`-এ নাম বসে (সেটাও আপডেট)।
 *     একমাত্র হার্ড-ডিলিট V490-এর ৭ দিনের পুরনো স্বয়ংক্রিয় নোটিশ পরিষ্কার —
 *     আর নিচের `prune()` ঠিক সেই একই ৭ দিনের নিয়মেই ফোনের কপি ছাঁটে।
 *  ৪. **তবু অন্ধ বিশ্বাস নয়** — প্রতি ৬ ঘণ্টায় একবার আবার পুরোটা নামিয়ে
 *     মিলিয়ে নেওয়া হয় (`FULL_EVERY_MS`)। কোনো কারণে ফোনের কপি সার্ভারের
 *     সাথে না মিললে, সর্বোচ্চ ৬ ঘণ্টার মধ্যেই নিজে থেকে ঠিক হয়ে যায়।
 *     এটা প্রজেক্টের আগের delta-গুলোরই একই রক্ষাকবচ।
 *  ৫. **Master-এর অনুমোদন কখনো হারায় না** — Refund/Delete/Reopen অনুরোধের
 *     নোটিশ যত পুরনোই হোক, ছাঁটা হয় না (`needsMasterApproval`)।
 *  ৬. **নেট খারাপ হলে সংখ্যা ভুল হবে না** — delta-পড়া ব্যর্থ হলে ফোনের
 *     আগের জমানো তালিকাই ফেরে (ঘন্টা আগের সংখ্যাই দেখায়), আর জমানো কিছু
 *     না থাকলে আগের মতোই সরাসরি পুরো পড়া হয়।
 *
 * ⛔ পর্দার তালিকা (`fetchRaw`) ইচ্ছে করে **ছোঁয়া হয়নি** — মানুষ দিনে দু-একবার
 *    নোটিশ পর্দা খোলে, ওটা Egress-এর সমস্যা নয়; আর ওখানে হাত দিলে নোটিশের
 *    লেখা/উত্তর ফাঁকা দেখানোর ঝুঁকি থাকত।
 */
object BriefingCloudCache {

    /** ঘন্টার গোনায় যে ঘরগুলো সত্যিই পড়া হয় — আগের মতোই হুবহু। */
    const val COLS = "id,date,title,targets,seen,hiddenFor,deletedAt,branch,updatedAt"

    private const val PREF = "briefing_cloud_delta_v1"
    private const val K_ROWS = "rows"
    private const val K_SINCE = "since"
    private const val K_FULL_AT = "lastFullAt"

    /** ৬ ঘণ্টা পরপর একবার পুরোটা মিলিয়ে নেওয়া (ভুল জমে থাকা ঠেকাতে)। */
    private const val FULL_EVERY_MS = 6L * 60L * 60L * 1000L

    /** ফোনে জমানো সারি কত দিন রাখা হবে — V490-এর সার্ভার-নিয়মের হুবহু একই। */
    private const val KEEP_DAYS = 7

    /** টেবিল একেবারে ফাঁকা থাকলে `since`-এর শুরুর মান (সবকিছুর আগের সময়)। */
    private const val EPOCH = "1970-01-01T00:00:00.000Z"

    private val lock = Any()

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /**
     * ঘন্টার গোনার জন্য সারিগুলো। ব্যর্থ হলে `null` — ডাকা জায়গা তখন আগের
     * মতোই নিজে পুরো পড়া চালাবে (কিছু ভাঙে না)।
     */
    fun rowsForCount(ctx: Context?): JSONArray? {
        val c = ctx?.applicationContext ?: return null
        synchronized(lock) {
            return try { load(c) } catch (_: Throwable) { null }
        }
    }

    private fun load(ctx: Context): JSONArray? {
        val sp = prefs(ctx)
        val cached = readCached(sp)
        val since = sp.getString(K_SINCE, "").orEmpty()
        val lastFull = sp.getLong(K_FULL_AT, 0L)
        val now = System.currentTimeMillis()

        // ⚠️ ইচ্ছে করে `cached.length() == 0` **দেখা হয় না**। শান্ত সপ্তাহে
        //    ছাঁটার পর তালিকা খালি হতেই পারে — সেটা "জমানো নেই" নয়, বরং
        //    "দেখানোর মতো কিছু নেই"। ওটা দেখলে ঠিক শান্ত দিনগুলোতেই প্রতিবার
        //    পুরো টেবিল নামত, অর্থাৎ সাশ্রয়টাই উল্টে যেত।
        //    "একবারও পুরো পড়া হয়নি" বোঝায় `since` ফাঁকা থাকা।
        val needFull = cached == null || since.isBlank() ||
            (now - lastFull) > FULL_EVERY_MS

        if (needFull) {
            val full = try {
                SupabaseClient.fetchListSlimOrNull("briefings", null, 5000, COLS)
            } catch (_: Throwable) { null }
            // পুরো পড়া ব্যর্থ — জমানো কিছু থাকলে সেটাই ফেরে, নইলে null।
            if (full == null) return cached
            val kept = prune(full)
            // টেবিল একেবারে ফাঁকা হলে `maxUpdatedAt` ফাঁকাই থাকত, আর তখন
            // পরের বারও "একবারও পড়া হয়নি" ধরে নিয়ে আবার পুরোটা নামত।
            // তাই ফাঁকা হলে একটা শুরুর তারিখ বসিয়ে দেওয়া হয়।
            val nextSince = maxUpdatedAt(full, since).ifBlank { EPOCH }
            save(sp, kept, nextSince, now)
            return kept
        }

        val delta = try {
            val f = "updatedAt=gt." + java.net.URLEncoder.encode(since, "UTF-8")
            SupabaseClient.fetchListSlimOrNull("briefings", f, 5000, COLS)
        } catch (_: Throwable) { null }
        // delta ব্যর্থ — ফোনের আগের তালিকাই ফেরে (ঘন্টা আগের সংখ্যা দেখায়)।
        if (delta == null) return cached

        val merged = mergeById(cached ?: JSONArray(), delta)
        val kept = prune(merged)
        save(sp, kept, maxUpdatedAt(delta, since), lastFull)
        return kept
    }

    // ── সাহায্যকারী ────────────────────────────────────────────────────────

    private fun readCached(sp: android.content.SharedPreferences): JSONArray? = try {
        val raw = sp.getString(K_ROWS, null)
        if (raw.isNullOrBlank()) null else JSONArray(raw)
    } catch (_: Throwable) { null }

    private fun save(sp: android.content.SharedPreferences, rows: JSONArray, since: String, fullAt: Long) {
        try {
            sp.edit()
                .putString(K_ROWS, rows.toString())
                .putString(K_SINCE, since)
                .putLong(K_FULL_AT, fullAt)
                .apply()
        } catch (_: Throwable) { }
    }

    /** id ধরে বসানো — নতুন সারি যোগ, বদলানো সারি প্রতিস্থাপন। ক্রম অটুট। */
    private fun mergeById(old: JSONArray, fresh: JSONArray): JSONArray {
        val byId = LinkedHashMap<String, JSONObject>()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        for (i in 0 until fresh.length()) {
            val o = fresh.optJSONObject(i) ?: continue
            val id = o.optString("id"); if (id.isNotBlank()) byId[id] = o
        }
        val out = JSONArray()
        for (v in byId.values) out.put(v)
        return out
    }

    /**
     * ফোনের কপি ছোট রাখা — V490-এর সার্ভার-নিয়মের হুবহু একই ৭ দিন।
     * ⛔ Master-এর অনুমোদন লাগে এমন নোটিশ যত পুরনোই হোক, কখনো ছাঁটা হয় না।
     */
    private fun prune(rows: JSONArray): JSONArray {
        val out = JSONArray()
        for (i in 0 until rows.length()) {
            val o = rows.optJSONObject(i) ?: continue
            val title = o.optString("title")
            if (BriefingModel.needsMasterApproval(title)) { out.put(o); continue }
            val d = o.optString("date").trim()
            // ⚠️ `withinDays` ভবিষ্যতের তারিখকে "না" বলে। কোনো ফোনের ঘড়ি সামান্য
            //    এগিয়ে থাকলে সেই ফোনের লেখা নোটিশের তারিখ কালকের হতে পারে —
            //    সেটা যেন ভুল করে বাদ না পড়ে, তাই আজ বা তার পরের তারিখও রাখা হয়।
            if (d >= BriefingModel.today() || BriefingModel.withinDays(d, KEEP_DAYS)) out.put(o)
        }
        return out
    }

    /** সবচেয়ে নতুন `updatedAt` — পরের বার এর পর থেকেই চাওয়া হবে। */
    private fun maxUpdatedAt(rows: JSONArray, current: String): String {
        var best = current
        for (i in 0 until rows.length()) {
            val u = rows.optJSONObject(i)?.optString("updatedAt").orEmpty().trim()
            if (u.isNotBlank() && u > best) best = u
        }
        return best
    }
}
