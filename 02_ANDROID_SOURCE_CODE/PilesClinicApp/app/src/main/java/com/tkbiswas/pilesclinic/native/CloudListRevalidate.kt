package com.tkbiswas.pilesclinic.native

/**
 * 🔵🔒 V513 (২২.০৮.২০২৬, TK-নির্দেশ — Supabase Egress) —
 * **"বদলেছে কি না জিজ্ঞেস করো, পুরোটা আবার নামিও না।"**
 *
 * TK-এর রিপোর্ট: *"patients / followups / payments / enquiries টেবিল থেকে
 * ২০০০–৫০০০ সারি বারবার নামছে।"*
 *
 * ─── কেন এখনো হচ্ছিল (কোডে চোখে দেখে যাচাই করা) ───────────────────────────
 * V493-এর `CloudReadDedupe` **৬০ সেকেন্ডের** ভিতরে একই URL দুবার নামা বন্ধ
 * করেছে — ওটা `onCreate → onResume → LiveRefresh → Worker` ঝাঁকটা ধরে ফেলে।
 * কিন্তু ৬০ সেকেন্ড পেরোলেই আবার **পুরো তালিকা** নামে। আর এই বোর্ডগুলোতে
 * তালিকা মানে পুরো ব্রাঞ্চের ইতিহাস, তারিখের কোনো সীমা নেই:
 *
 *   • `DraftRepository`          — enquiries + followups + patients + payments, ৫০০০ করে
 *   • `ReportsRepository`        — enquiries + patients + payments + doctor_visits, ৫০০০ করে
 *   • `FollowUpRepository`       — patients + payments, ৫০০০ করে
 *   • `ChamberAttendanceRepository`, `DoctorQueueRepository`, `DoctorVisitActivity` — একই ধরন
 *
 * একজন স্টাফ দিনে ২০–৩০ বার এই পর্দাগুলো খোলেন। প্রতিবারই **পুরো তালিকা**।
 * অথচ বেশিরভাগ বারেই টেবিলে **একটা সারিও বদলায়নি** — একই তথ্য বারবার নামে।
 *
 * ─── এখন কী হয় ─────────────────────────────────────────────────────────────
 * বড় তালিকা নামানোর আগে **একটা ছোট প্রশ্ন** করা হয় (উত্তর কয়েকশো বাইট,
 * তালিকার একটাও সারি নামে না) — দুটো তথ্য একসঙ্গে:
 *
 *   • **কতগুলো সারি আছে?** — `Prefer: count=exact` বললে PostgREST মোট সংখ্যাটা
 *     `Content-Range` হেডারে পাঠায়।
 *   • **সবচেয়ে নতুন `updatedAt` কোনটা?** — `limit=1`, একটাই ঘর। **এক সারি।**
 *
 * দুটোই **একই অনুরোধে** পাওয়া যায় (`SupabaseClient.fetchListFingerprintOrNull`)
 * — তাই টেবিলপ্রতি খরচ **একটাই ছোট অনুরোধ**।
 * এই দুটো মিলিয়ে টেবিলের একটা "সই" (fingerprint)। গতবার যে সই ছিল সেটার
 * সঙ্গে হুবহু মিলে গেলে ⇒ **একটা সারিও বদলায়নি** ⇒ গতবারের জমানো উত্তরটাই
 * ফেরত দেওয়া হয়। এক ফোঁটাও নতুন ডেটা নামে না।
 *
 * সই না মিললে (কেউ কিছু যোগ/বদল/মুছেছে) ⇒ **আগের মতোই পুরো তালিকা নামে**।
 *
 * ─── কেন এই সই সত্যিই নির্ভরযোগ্য (আন্দাজ নয়, কোডে মিলিয়ে দেখা) ──────────
 *  • **নতুন সারি** ⇒ সংখ্যা বাড়ে ⇒ ধরা পড়ে।
 *  • **মুছে ফেলা** ⇒ সংখ্যা কমে ⇒ ধরা পড়ে। (এই অ্যাপে ডিলিট সত্যিকারের
 *    `DELETE` — `SupabaseClient.deleteById` — তাই সংখ্যা নিশ্চিত বদলায়।)
 *  • **বদল (edit)** ⇒ `updatedAt` নতুন হয় ⇒ সবচেয়ে-নতুন সময় এগোয় ⇒ ধরা পড়ে।
 *    প্রতিটা লেখায় অ্যাপ নিজে `updatedAt` বসায় — কোডে ৮৫ জায়গায় `isoNow()`
 *    দিয়ে, আর V223-এর ডেটাবেস-trigger ওই ঘরটাই পাহারা দেয়।
 *
 * ─── 🔒 নিরাপত্তা (প্রতিটা ইচ্ছে করে বসানো) ────────────────────────────────
 *  • **শুধু বড় তালিকায়।** {@link #MIN_BODY_BYTES}-এর ছোট উত্তর কখনো জমা হয়
 *    না। অর্থাৎ একজন রোগী · এক দিনের পেমেন্ট · এক id ধরে পড়া — এদের আচরণ
 *    **এক অক্ষরও বদলায়নি**। যে বড় পড়াগুলোর কথা TK বলেছেন, শুধু সেগুলোই।
 *  • **সন্দেহ হলেই পুরোটা নামে।** সই আনতে ব্যর্থ · সংখ্যা `-1` (= জানি না) ·
 *    `updatedAt` ঘরটাই নেই · আগে কখনো নামেনি — **সব ক্ষেত্রেই আগের মতোই
 *    পুরো তালিকা**। এই ফাইল কখনো "কম তথ্য" ফেরাতে পারে না।
 *  • **ব্যর্থ পড়া কখনো জমা হয় না** (`null` ⇒ কিছুই মনে রাখা হয় না)।
 *    ⇒ খাতার সারি **B446** ("নেট আটকালে খালি তালিকা / Collection ₹0")
 *    ফিরে আসার কোনো পথ নেই।
 *  • **নিজে কিছু সেভ করলেই সব ভুলে যাওয়া।** `SupabaseClient`-এর প্রতিটা
 *    upsert / update / delete-এর পরে `clear()` ডাকা হয় — ঠিক সেই সাতটা
 *    জায়গায় যেখানে আগে থেকেই `CloudReadDedupe.clear()` ডাকা হত।
 *  • **সর্বোচ্চ বয়স।** {@link #MAX_AGE_MS}-এর বেশি পুরনো জমানো উত্তর কখনো
 *    ব্যবহার হয় না — সই মিললেও নয়। কোনো ফোনের ঘড়ি ভুল থাকলেও তাই
 *    সর্বোচ্চ ওইটুকু সময়ের মধ্যে সব মিলে যায়।
 *  • **মেমরির সীমা** {@link #MAX_BYTES}; বেশি হলে সবচেয়ে কম-ব্যবহৃতটা আগে সরে।
 *  • **যেকোনো গোলমালে চুপচাপ সরে দাঁড়ায়** — ডাকার জায়গা তার নিজের পড়াটা
 *    আগের মতোই পায়। এই ফাইল কোনো পর্দা আটকাতে বা ভাঙতে পারে না।
 *
 * ⛔ কোনো তথ্য · হিসাব · ডিজাইন · ব্রাঞ্চ-নিয়ম · অনুমতি · পর্দার কোড কিছুই
 *    ছোঁয়া হয়নি। সার্ভারের দিকে তালিকার URL-ও এক অক্ষর বদলায়নি।
 */
object CloudListRevalidate {

    /** এর চেয়ে ছোট উত্তর জমা হয় না — ছোট পড়ার আচরণ হুবহু আগের মতোই থাকে। */
    private const val MIN_BODY_BYTES = 32 * 1024

    /**
     * জমানো উত্তরের সর্বোচ্চ বয়স। এর পরে সই মিললেও আর ব্যবহার হয় না।
     *
     * 🔴🔒 **কেন একটা সীমা রাখতেই হবে — সৎভাবে (TK-কে জানানো হয়েছে):**
     * সই-এর `updatedAt` অ্যাপ নিজে বসায়, **ফোনের ঘড়ি থেকে**। ডেটাবেসের
     * V223 পাহারা (`_rk_guard_no_older_overwrite`) নিশ্চিত করে যে কোনো
     * সারির নতুন লেখা তার **নিজের** আগের সময়ের চেয়ে পুরনো হতে পারে না।
     * কিন্তু কোনো ফোনের ঘড়ি **পিছিয়ে** থাকলে তার লেখা সময়টা টেবিলের
     * **সবচেয়ে নতুন** সময়ের নিচে পড়তে পারে — আর তখন (যদি ঠিক ওই সময়ে
     * টেবিলে আর কোনো যোগ/মোছাও না হয়) সই বদলাবে না।
     *
     * এই সরু ফাঁকটা যাতে কখনো দীর্ঘ না হয়, তাই ১০ মিনিট পেরোলে জমানো
     * উত্তর নিঃশর্তে বাতিল — সই মিলুক বা না মিলুক, পুরো তালিকা আবার নামে।
     * ⇒ সবচেয়ে খারাপ অবস্থাতেও পার্থক্য ১০ মিনিটের বেশি টিকতে পারে না,
     *   আর সেটা শুধু ওই এক অবস্থাতেই (ভুল ঘড়ি + টেবিলে আর কোনো বদল নেই)।
     * ⇒ নিজের ফোনে কিছু সেভ করলে সঙ্গে সঙ্গেই সব ভুলে যাওয়া হয় (`clear()`),
     *   তাই নিজের কাজ কখনো পুরনো দেখায় না।
     */
    private const val MAX_AGE_MS = 10L * 60L * 1000L

    /** সব মিলিয়ে এর বেশি মেমরি নেয় না। */
    private const val MAX_BYTES = 12L * 1024L * 1024L

    /**
     * 🔵🔒 V515 (২২.০৮.২০২৬ — Egress অডিট): **একটা তালিকা একাই জায়গাটা
     * দখল করতে পারবে না।**
     *
     * কেন: খাতার সারি V512-তে ঠিক এই ভুলটাই ধরা পড়েছিল — `trash`-এর একটা
     * সারিতেই মুছে ফেলা পুরো রেকর্ড (ছবিসহ) থাকে, আর সেরকম কয়েকটা সারি
     * জমা রাখলেই বাকি সব পর্দার জমানো উত্তর ছিটকে যেত; ফলে সেগুলো আবার
     * নতুন করে নামত — অর্থাৎ Egress **বাড়ত**।
     *
     * তাই এর চেয়ে বড় কোনো উত্তর জমা হয় না। জমা না হলে ক্ষতি নেই — ওই
     * পড়াটা আগের মতোই প্রতিবার নেট থেকে আসে (হুবহু পুরনো আচরণ), শুধু
     * অন্যদের জায়গা কেড়ে নেয় না।
     */
    private const val MAX_ONE_BYTES = 2L * 1024L * 1024L

    /** একই টেবিলের সই পরপর কয়েকটা পড়ার জন্য একবারই আনা হয় (ঝাঁক ধরার জন্য)। */
    private const val PROBE_TTL_MS = 15_000L

    private class Entry(val at: Long, val body: String, val count: Int, val maxStamp: String)

    private class Probe(val at: Long, val count: Int, val maxStamp: String)

    private val lock = Any()

    /** true = সবচেয়ে কম-ব্যবহৃত এন্ট্রি আগে সরে (LRU)। */
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private var bytes = 0L

    private val probes = HashMap<String, Probe>()

    /**
     * 🔒 যে URL অন্তত একবার **বড়** উত্তর দিয়েছে, শুধু তার আগেই সই আনা হয়।
     *
     * কেন এই তালিকাটা দরকার: সই আনতে দুটো ছোট অনুরোধ লাগে। অ্যাপের বেশিরভাগ
     * পড়াই ছোট (এক রোগী · এক id · এক দিন) — সেগুলোর আগে সই আনলে **অনুরোধের
     * সংখ্যা উল্টে বেড়ে যেত**, অথচ জমানোর মতো কিছুই থাকত না। তাই প্রথমবার
     * কোনো URL আগের মতোই সোজা নামে; বড় বলে প্রমাণিত হলে তবেই পরের বার থেকে
     * তার আগে সই দেখা হয়।
     */
    private val bigUrls = java.util.Collections.synchronizedSet(HashSet<String>())

    /**
     * বড় তালিকা পড়ার একমাত্র দরজা।
     *
     * @param table   টেবিলের নাম (সই আনার জন্য)
     * @param filter  ঠিক সেই ছাঁকনি যেটা তালিকার অনুরোধে গেছে (নইলে সই মিলবে না)
     * @param url     পুরো URL — জমানো উত্তরের চাবি
     * @param load    আসল পড়া; সই না মিললে বা সন্দেহ হলে এটাই চলে
     */
    fun body(table: String, filter: String?, url: String, load: () -> String?): String? {
        return try {
            // এই URL আগে কখনো বড় উত্তর দেয়নি ⇒ হুবহু আগের আচরণ, কোনো সই নয়।
            if (!bigUrls.contains(url)) {
                val first = load()
                if (first != null && first.length >= MIN_BODY_BYTES) bigUrls.add(url)
                return first
            }

            /* 🔴🔒 সই **পড়ার আগে** নেওয়া হয় — পরে নয়।
               কারণ: পড়া চলাকালীন কেউ কিছু বদলালে, পরে নেওয়া সই ওই বদলটাও
               ধরে ফেলত; তখন জমানো তালিকাটা (যাতে বদলটা নেই) "তাজা" বলে
               গণ্য হত — অর্থাৎ **বাসি তথ্য**। আগে নিলে উল্টোটা হয়: বদলটা
               পরের বারের সইয়ে ধরা পড়ে ও পুরো তালিকা নতুন করে নামে।
               বড়জোর একবার বাড়তি পড়া — কখনো বাসি তথ্য নয়। */
            val before = fingerprint(table, filter)

            val cached = peekIfUnchanged(url, before)
            if (cached != null) return cached

            val loaded = load()
            if (loaded != null) remember(url, loaded, before)
            loaded
        } catch (_: Throwable) {
            // শেষ ভরসা: এই ফাইলটা যেন কখনো কারো পথ আটকাতে না পারে।
            try { load() } catch (_: Throwable) { null }
        }
    }

    /** জমানো উত্তর তখনই ফেরে, যখন সই **হুবহু** মেলে ও বয়স সীমার মধ্যে। */
    private fun peekIfUnchanged(url: String, now: Probe?): String? {
        if (now == null) return null                            // সন্দেহ ⇒ পুরোটা নামুক
        val e = synchronized(lock) { entries[url] } ?: return null
        if (System.currentTimeMillis() - e.at > MAX_AGE_MS) { forget(url); return null }
        if (now.count != e.count) return null                   // যোগ/মোছা হয়েছে
        if (now.maxStamp != e.maxStamp) return null             // কিছু বদলেছে
        return e.body                                           // ✅ কিছুই বদলায়নি
    }

    /**
     * টেবিলের সই = (সারির সংখ্যা, সবচেয়ে নতুন `updatedAt`)।
     * দুটোই ছোট অনুরোধ — একটাতেও তালিকার সারি নামে না।
     */
    private fun fingerprint(table: String, filter: String?): Probe? {
        val key = table + "|" + (filter ?: "")
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val cached = probes[key]
            if (cached != null && now - cached.at <= PROBE_TTL_MS) return cached
        }
        val fp = try { SupabaseClient.fetchListFingerprintOrNull(table, filter) } catch (_: Throwable) { null }
        val count = fp?.first ?: -1
        val stamp = fp?.second
        /* 🔴🔒 খাতার সারি B446-এর শিক্ষা এখানেও মানা হয়েছে: **"একবার ব্যর্থ হলে
           আর কোনোদিন চেষ্টা কোরো না"** — এই ধরনের শর্টকাট এই প্রজেক্টে আগে
           একবার আসল বাগ তৈরি করেছিল। তাই এখানে টেবিলটাকে স্থায়ীভাবে বাদ দেওয়া
           হয় **না**। এবারের মতো সই পাওয়া গেল না ⇒ এবার পুরো তালিকাই নামবে
           (আগের আচরণ), আর পরের বার আবার চেষ্টা হবে।
           খরচ: `updatedAt` ঘর নেই এমন টেবিলে প্রতিবার একটা ছোট অনুরোধ — নগণ্য। */
        if (stamp == null || count < 0) return null
        val p = Probe(now, count, stamp)
        synchronized(lock) { probes[key] = p }
        return p
    }

    /** সফল ও যথেষ্ট বড় পড়া জমা হয়, **পড়ার আগে নেওয়া** সই-সহ। */
    private fun remember(url: String, body: String, before: Probe?) {
        if (before == null) return
        if (body.length < MIN_BODY_BYTES) return
        val size = body.length.toLong()
        if (size > MAX_ONE_BYTES) return
        synchronized(lock) {
            entries.remove(url)?.let { bytes -= it.body.length.toLong() }
            entries[url] = Entry(System.currentTimeMillis(), body, before.count, before.maxStamp)
            bytes += size
            val it = entries.entries.iterator()
            while (bytes > MAX_BYTES && it.hasNext()) {
                val old = it.next()
                bytes -= old.value.body.length.toLong()
                it.remove()
            }
        }
    }

    private fun forget(url: String) = synchronized(lock) {
        entries.remove(url)?.let { bytes -= it.body.length.toLong() }
        Unit
    }

    /** নিজে কিছু লেখার পরে সব ভুলে যাওয়া হয়, যাতে পরের পড়া তাজা হয়। */
    fun clear() = synchronized(lock) {
        entries.clear()
        probes.clear()
        bytes = 0L
    }

    /* 🟠🔒 V1461 (১৪.০৯.২০২৬, TK-অনুমোদিত — তালিকা ৪৬২-ক): **শুধু যে টেবিলে
       লেখা হলো, তারই জমানো সই/তালিকা মুছে যায় — বাকি টেবিলগুলোর ১০-মিনিটের
       জমানো উত্তর অক্ষত থাকে।**
       আগে যেকোনো একটা টেবিলে (যেমন payments) সেভ করলে `clear()` (উপরের,
       প্যারামিটার-হীন) followups/patients/enquiries — সব টেবিলের জমানো সই
       একসাথে মুছে দিত, যদিও তাদের একটা সারিও বদলায়নি। ফলে পরের যে-কোনো
       পর্দা খুললেই ওই না-বদলানো টেবিলগুলোও আবার পুরো তালিকা নামাত।
       ⛔ যেটা বদলেছে (table প্যারামিটার) তার জমানো সব মুছে যাওয়া আগের মতোই
          অটুট — বাসি তথ্য দেখানোর কোনো ঝুঁকি নতুন করে তৈরি হয়নি।
       ⛔ url সবসময় `/rest/v1/<table>?...`-এর হুবহু একই ছাঁচে বসে
          (`SupabaseClient`-এর প্রতিটা তালিকা-পড়ায়) — তাই এই মিলটা নির্ভরযোগ্য।
       ⛔ table না দিলে (পুরনো ডাক) আগের মতোই সব মুছে যায় — কোনো আচরণ বদলায়নি। */
    fun clear(table: String) = synchronized(lock) {
        val needle = "/rest/v1/$table?"
        val prefix = "$table|"
        val eIt = entries.entries.iterator()
        while (eIt.hasNext()) {
            val e = eIt.next()
            if (e.key.contains(needle)) { bytes -= e.value.body.length.toLong(); eIt.remove() }
        }
        val pIt = probes.keys.iterator()
        while (pIt.hasNext()) { if (pIt.next().startsWith(prefix)) pIt.remove() }
    }

    /** পরীক্ষার জন্য (কোনো পর্দা এটা ব্যবহার করে না)। */
    fun debugSize(): Int = synchronized(lock) { entries.size }
}
