package com.tkbiswas.pilesclinic.native

/**
 * 🔵🔒 V493 (২০.০৮.২০২৬, TK-নির্দেশ — Supabase Egress) — **একই অনুরোধ দুবার নয়।**
 *
 * TK-এর রিপোর্ট (Supabase লাইভ লগ থেকে): *"patients, followups, payments ও
 * enquiries টেবিলের বড় তালিকা কাছাকাছি সময়ে বারবার — কখনো একই অনুরোধ দুবার —
 * নামার প্রমাণ পাওয়া গেছে।"*
 *
 * ─── কেন হত (কোড ধরে যাচাই করা) ────────────────────────────────────────────
 * একই তথ্য চাওয়ার জায়গা অ্যাপে অনেক, আর তারা একে অপরের কথা জানে না:
 *   • `onCreate` তালিকা আনে, সঙ্গে সঙ্গে `onResume`-ও আনে
 *   • `LiveRefresh` ঠিক সেই মুহূর্তে বদল খুঁজতে গিয়ে আবার আনে
 *   • `BackgroundRefreshWorker` পিছনে চলতে চলতে আবার আনে
 *   • এক পর্দায় দুটো Repository পাশাপাশি (`async { }`) একই টেবিল আনে
 * ফলে **হুবহু একই URL** কয়েক সেকেন্ডের মধ্যে ২–৪ বার সার্ভারে যেত, আর
 * প্রতিবারই পুরো তালিকা নেমে আসত।
 *
 * `CloudReadCache` আগে থেকেই ছিল, কিন্তু সেটা মাত্র ১৩ জায়গায় হাতে-বসানো —
 * বাকি ২৪৯টা পড়া সরাসরি নেটে চলে যেত।
 *
 * ─── এখন কী হয় ─────────────────────────────────────────────────────────────
 * এই পাহারাটা বসেছে **একদম নিচের স্তরে** (`SupabaseClient.fetchListOrNull`),
 * তাই অ্যাপের **সব** পড়া এর ভিতর দিয়ে যায় — কোনো পর্দার কোড বদলাতে হয়নি।
 *
 *  ১. **একসঙ্গে (in-flight):** হুবহু একই URL এখনই নেটে গেছে — দ্বিতীয়জন
 *     নতুন অনুরোধ না করে ওই একটারই উত্তরের জন্য অপেক্ষা করে।
 *     ⇒ এখানে বাসি তথ্যের প্রশ্নই নেই, কারণ দুটো অনুরোধ একই মুহূর্তের।
 *  ২. **সদ্য শেষ (TTL):** একই URL {@link #TTL_MS} মিলিসেকেন্ডের ভিতরে আবার
 *     চাওয়া হলে সদ্য পাওয়া উত্তরটাই দেওয়া হয়।
 *
 * ─── 🔒 নিরাপত্তা (প্রতিটা ইচ্ছে করে বসানো) ────────────────────────────────
 *  • **ব্যর্থ পড়া কখনো মনে রাখা হয় না।** `null` এলে কিছুই জমা হয় না, তাই
 *    পরের চেষ্টা আগের মতোই নেটে যায়। ⇒ খাতার সারি **B446** ("নেট আটকালে
 *    ₹0 দেখাত") ফিরে আসার কোনো পথ নেই।
 *  • **লেখামাত্র সব ভুলে যাওয়া।** `SupabaseClient`-এর প্রতিটা upsert /
 *    update / delete-এর পরে `clear()` ডাকা হয় (ঠিক যেখানে আগে থেকেই
 *    `CloudReadCache.clear()` ডাকা হত, সেই একই ছয় জায়গায়)। ⇒ নিজে কিছু
 *    সেভ করার পর কখনো পুরনো তালিকা দেখা যাবে না।
 *  • **প্রত্যেকে নিজের আলাদা কপি পায়।** জমা থাকে সার্ভারের কাঁচা লেখা
 *    (text), আর প্রত্যেক ডাকার জায়গা নিজে সেটা থেকে নিজের `JSONArray`
 *    বানায়। ⇒ একজন তালিকা বদলালে অন্যজনের তালিকা বদলে যাওয়ার ঝুঁকি
 *    **শূন্য** — আচরণ হুবহু আগের মতোই।
 *  • **মেমরির সীমা।** সব মিলিয়ে {@link #MAX_BYTES}-এর বেশি জমে না; বেশি
 *    হলে সবচেয়ে পুরনোটা আগে সরে যায়।
 *  • **যেকোনো গোলমালে চুপচাপ সরে দাঁড়ায়।** এখানে কিছু ভুল হলে ডাকার
 *    জায়গা তার নিজের পড়াটা আগের মতোই পায় — এই ফাইল কখনো কোনো পর্দা
 *    আটকাতে বা ভাঙতে পারে না।
 *
 * ⛔ কোনো তথ্য · হিসাব · ডিজাইন · ব্রাঞ্চ-নিয়ম · অনুমতি কিছুই ছোঁয়া হয়নি।
 *    এটা শুধু **একই প্রশ্ন দুবার না করার** ব্যবস্থা।
 */
object CloudReadDedupe {

    /** সদ্য পাওয়া উত্তর কতক্ষণ ব্যবহারযোগ্য।
     *
     *  ৬০ সেকেন্ড বেছে নেওয়ার কারণ: `onCreate → onResume → LiveRefresh →
     *  Worker` — এই ঝাঁকটা সাধারণত কয়েক সেকেন্ডের মধ্যে ঘটে, আর মানুষ এক
     *  পর্দা থেকে বেরিয়ে ফিরে এসে আবার খোলে প্রায় এক মিনিটের মধ্যেই।
     *  নিজের সেভের পরে যেহেতু সঙ্গে সঙ্গেই সব ভুলে যাওয়া হয়, তাই এতে
     *  নিজের কাজ কখনো পুরনো দেখাবে না। */
    private const val TTL_MS = 60_000L

    /** সব মিলিয়ে জমা রাখার সর্বোচ্চ মাপ (৮ MB)। */
    private const val MAX_BYTES = 8L * 1024L * 1024L

    private class Entry(val at: Long, val body: String)

    private val lock = Any()
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)   // true = কম-ব্যবহৃত আগে সরে
    private var bytes = 0L

    /** 🔴🔒 V494 (২১.০৮.২০২৬, TK-যাচাই ১) — **মেমরি লিক সারানো।**
     *
     *  V493-এ এটা ছিল `HashMap<String, Any>` আর চাবি যোগ হত কিন্তু **কখনো
     *  সরত না**। প্রতিটা নতুন URL (রোগী-ধরে-রোগী পড়া, তারিখ-ধরে পড়া…)
     *  চিরকাল জমে থাকত — অ্যাপ যত বেশিক্ষণ চলত, ম্যাপটা তত বড় হত।
     *
     *  এখন প্রতিটা চাবির সঙ্গে **কতজন এখন ব্যবহার করছে** তা গোনা হয়, আর
     *  শেষজন সরে গেলেই (`finally`-তে, সফল · ব্যর্থ · exception — সব
     *  অবস্থাতেই) চাবিটা ম্যাপ থেকে মুছে যায়।
     *  ⛔ একই URL-এর চলমান অনুরোধ আটকানোর ক্ষমতা এক চুলও কমেনি — কারণ
     *     গোনাটা `synchronized(gate)`-এ ঢোকার **আগেই** বাড়ে, তাই দ্বিতীয়জন
     *     এসে ঠিক সেই একই তালাই পায়। */
    private class Gate {
        var users = 0
    }

    private val inFlight = HashMap<String, Gate>()

    /** জমানো উত্তর (থাকলে) — না থাকলে null। */
    private fun peek(key: String): String? = synchronized(lock) {
        val e = entries[key] ?: return null
        if (System.currentTimeMillis() - e.at > TTL_MS) {
            entries.remove(key); bytes -= e.body.length.toLong(); return null
        }
        e.body
    }

    private fun put(key: String, body: String) = synchronized(lock) {
        entries.remove(key)?.let { bytes -= it.body.length.toLong() }
        entries[key] = Entry(System.currentTimeMillis(), body)
        bytes += body.length.toLong()
        val it = entries.entries.iterator()
        while (bytes > MAX_BYTES && it.hasNext()) {
            val e = it.next(); bytes -= e.value.body.length.toLong(); it.remove()
        }
    }

    /** চাবিটা ধরা — গোনা এক বাড়ে। `synchronized(gate)`-এ ঢোকার আগেই ডাকতে হয়। */
    private fun acquireGate(key: String): Gate = synchronized(lock) {
        val g = inFlight.getOrPut(key) { Gate() }
        g.users++
        g
    }

    /** চাবি ছাড়া — শেষজন সরলে ম্যাপ থেকেও মুছে যায় (V494: মেমরি লিক বন্ধ)। */
    private fun releaseGate(key: String, g: Gate) = synchronized(lock) {
        g.users--
        if (g.users <= 0) inFlight.remove(key)
    }

    /**
     * [key] (হুবহু URL) ধরে একবারই [load] চালায়।
     *
     * @param load সার্ভার থেকে কাঁচা লেখা আনে; ব্যর্থ হলে null।
     * @return কাঁচা লেখা, বা ব্যর্থ হলে null। **ডাকার জায়গা নিজে এটা থেকে
     *         নিজের JSONArray বানাবে** — তাই কেউ কারো তথ্য ছুঁতে পারে না।
     */
    fun body(rawKey: String, load: () -> String?): String? {
        // 🔐 V494 (TK-যাচাই ২): চাবির সঙ্গে **কে লগইন করা আছে** সেটাও জোড়া হয়।
        val key = keyFor(rawKey)
        try {
            peek(key)?.let { return it }
            val g = acquireGate(key)
            try {
                synchronized(g) {
                    // অপেক্ষা করতে করতে অন্য কেউ এনে ফেলে থাকতে পারে।
                    peek(key)?.let { return it }
                    val fresh = load()
                    if (fresh != null) put(key, fresh)
                    return fresh
                }
            } finally {
                // 🔴 V494: সফল · ব্যর্থ · exception — সব অবস্থাতেই চাবি ছাড়া হয়।
                releaseGate(key, g)
            }
        } catch (_: Throwable) {
            // শেষ উপায় — এই ফাইলটা যেন কখনো কোনো পড়া আটকাতে না পারে।
            return try { load() } catch (_: Throwable) { null }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔐🔒 V494 (২১.০৮.২০২৬, TK-যাচাই ২) — **লগইন/লগআউট নিরাপত্তা।**
    //
    // V493-এ চাবি ছিল শুধু URL। এক ফোনে একজন লগআউট করে অন্যজন লগইন করলে,
    // ৬০ সেকেন্ডের ভিতরে আগের ব্যবহারকারীর জমানো তালিকা নতুন জনের কাছে
    // চলে যেতে পারত — ভুল ব্রাঞ্চের রোগী/টাকা দেখানোর ঝুঁকি।
    //
    // এখন **দুই স্তরের সুরক্ষা** (একটা ব্যর্থ হলেও অন্যটা ধরে):
    //  ১. চাবির সামনে বর্তমান পরিচয় জোড়া — `"<পরিচয়>|<URL>"`. তাই দুজনের
    //     চাবি কখনো এক হতে পারে না, ভুল করে clear() বাদ পড়লেও নয়।
    //  ২. পরিচয় বদলালে `setSession()` নিজে থেকেই সব জমানো তথ্য মুছে দেয়।
    //
    // ⛔ পরিচয় হিসেবে **মোবাইলের শেষ ১০ অঙ্ক** ব্যবহার হয় (প্রজেক্টের সব
    //    জায়গার `mob()` নিয়মের মতোই)। কোনো পাসওয়ার্ড/টোকেন এখানে আসে না।
    // ═══════════════════════════════════════════════════════════════════════
    @Volatile private var sessionTag: String = ""

    /** কে এখন লগইন করা — লগইন · লগআউট · ব্যবহারকারী বদল, তিন পথেই ডাকা হয়।
     *  পরিচয় বদলালে সব জমানো তথ্য সঙ্গে সঙ্গে মুছে যায়। */
    fun setSession(identity: String?) {
        val tag = (identity ?: "").filter { it.isDigit() }.takeLast(10)
        if (tag == sessionTag) return
        sessionTag = tag
        clear()
    }

    private fun keyFor(rawKey: String): String = sessionTag + "|" + rawKey

    /** সব ভুলে যাওয়া। প্রতিটা লেখার (upsert/update/delete) পরে ডাকা হয়। */
    fun clear() {
        synchronized(lock) { entries.clear(); bytes = 0L }
    }

    /* 🟠🔒 V1461 (১৪.০৯.২০২৬, TK-অনুমোদিত — তালিকা ৪৬২-ক, `CloudListRevalidate`-এর
       হুবহু একই টেবিল-বাছাই যুক্তি এখানেও)। `body()`-তে চাবি সবসময় url-ই থাকে
       (`SupabaseClient`-এর `/rest/v1/<table>?...`-ছাঁচ), তাই এই মিল নির্ভরযোগ্য।
       table না দিলে আগের মতোই সব মুছে যায় — পুরনো কোনো ডাক বদলায়নি। */
    fun clear(table: String) {
        val needle = "/rest/v1/$table?"
        synchronized(lock) {
            val it = entries.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                if (e.key.contains(needle)) { bytes -= e.value.body.length.toLong(); it.remove() }
            }
        }
    }

    /** শুধু পরীক্ষার জন্য — এখন কতগুলো উত্তর জমা আছে। */
    fun debugSize(): Int = synchronized(lock) { entries.size }

    /** শুধু পরীক্ষার জন্য (V494, TK-যাচাই ১) — চলমান-অনুরোধের ম্যাপে এখন
     *  কতগুলো চাবি আছে। সব কাজ শেষ হলে এটা **০** হওয়া বাধ্যতামূলক;
     *  না হলে মেমরি লিক ফিরে এসেছে। */
    fun debugInFlightSize(): Int = synchronized(lock) { inFlight.size }
}
