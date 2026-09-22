package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🚨 TK'S ORDER (2026-07-28): "আমি আমার ফোনে যা করলাম তা যেন হারিয়ে না যায়... একই
 * ইন্টারনেটে UPI চলে, ফ্লিপকার্ট চলে, তাহলে আমাদের অ্যাপে কেন সমস্যা?"
 *
 * WHAT THE AUDIT FOUND (28.07.2026, counted -- not guessed): the app writes to
 * the cloud in 106 places. 55 of them already put a failed write into their own
 * retry list. THE OTHER 51 DID NOT: a medicine sale, a bill correction, an
 * approval, a Trash restore, a doctor entry, a password change -- if the line
 * dropped at that exact moment, the work was simply gone, and nobody was told.
 *
 * WHAT THIS IS: one safety net underneath ALL of them. Every write in the app
 * already passes through SupabaseClient, so a failure is remembered here and
 * sent again later, by itself:
 *   . when the app is opened,
 *   . every hour in the background while there is internet, even with the app
 *     closed.
 *
 * WHY IT IS SAFE
 *  - Nothing about any screen changes. This only runs AFTER a write has
 *    already failed, where previously the work was dropped.
 *  - Every retry is by the row's own id (create-or-update / update / delete),
 *    so sending the same thing twice can never make a second copy or take
 *    money twice.
 *  - A row that was deleted on purpose is never brought back: the same
 *    DeletedGuard check the older queues use is applied here.
 *  - The repositories that already had their own retry keep it; a write simply
 *    gets a second chance instead of none.
 *  - Anything unusually large (a photo, for instance) is left to the queue that
 *    already handles it, so this list stays small and quick.
 */
object CloudWriteQueue {

    private const val PREF = "cloud_write_queue"
    private const val KEY = "pending"

    /**
     * 🚨 খাতার সারি B145 (TK, 30.07.2026): আগে ছিল **৪০০** — তালিকা এর বেশি হলে
     * সবচেয়ে পুরনো কাজগুলো **নীরবে বাদ** পড়ত, আর ৫০ বার ব্যর্থ হওয়া কাজও নীরবে
     * বাদ পড়ত। TK-এর কথা: *"এটি নীরবে হওয়া উচিত নয়।"*
     * এখন — (১) সীমা **১০০০** (এখানে শুধু লেখা জমে, ফোনের উপর ভার নগণ্য),
     * (২) যা সত্যিই আর পাঠানো যাচ্ছে না তা **মুছে ফেলা হয় না** — নিচের
     * "যায়নি" ঘরে (`KEY_FAILED`) তুলে রাখা হয়, লাল সতর্কবাতিতে সংখ্যাটা
     * দেখা যায় এবং "পাঠান" চাপলে আবার চেষ্টা করা যায়।
     */
    private const val MAX_ENTRIES = 1000
    private const val MAX_PAYLOAD_CHARS = 200_000
    private const val MAX_TRIES = 50

    /**
     * 🔒🔒 খাতার সারি B145-এর যাচাইয়ে নিজে ধরা পড়েছে (30.07.2026): সীমা ৪০০ থেকে
     * ১০০০ করার পরে **হিসাবে** এই তালিকাটা অনেক বড় হয়ে যেতে পারত (১০০০ × বড়
     * তথ্য)। এই তালিকা ফোনের স্মৃতিতে **পুরোটা একসাথে** পড়া হয়, তাই খুব বড় হলে
     * অ্যাপ ধীর বা হ্যাং হতে পারত — ঠিক যেটা খাতার সারি B27-এ TK-এর স্টাফদের
     * হয়েছিল। তাই সংখ্যার পাশাপাশি **মাপেরও সীমা** — সবমিলিয়ে ২ MB-র বেশি হলে
     * সবচেয়ে পুরনোগুলো "যায়নি" ঘরে সরে যায় (মোছা হয় না)।
     */
    private const val MAX_TOTAL_CHARS = 2_000_000

    /** যেগুলো আর পাঠানো যাচ্ছে না — শুধু জানানোর জন্য রাখা হয়, মোছা হয় না। */
    private const val KEY_FAILED = "failed"
    private const val MAX_FAILED = 300

    /**
     * 🔒🔒 খাতার সারি B164 (TK, 30.07.2026 সকাল ১১.৩৯ — TK-এর ১ নম্বর সন্দেহ):
     * *"দুর্বল নেটে দুটি Save একসঙ্গে ব্যর্থ হলে একটি হারাতে পারে।"* — **সত্যি ছিল।**
     *
     * এই তালিকাটা রাখা হয় এক টুকরো লেখায় (SharedPreferences)। লেখার নিয়ম ছিল
     * তিন ধাপে — **পড়া → বদলানো → লেখা**। কোনো তালা না থাকায় দুটো কাজ একই
     * মুহূর্তে ব্যর্থ হলে **দুজনেই পুরনো তালিকাটা পড়ত**, তারপর একজনের লেখা
     * অন্যজনের লেখাকে চাপা দিয়ে দিত — একটা কাজ নিঃশব্দে হারিয়ে যেত।
     *
     * এখন এই একটাই তালা — যে-ই তালিকাটা ছোঁবে (`remember` · `flush` ·
     * `retryFailed`), একজনের কাজ শেষ হলে তবেই পরেরজন ঢুকবে।
     * ⛔ **নেটের কাজ তালার বাইরে** — তাই পাঠানো চলার সময়েও নতুন সেভ আটকে
     *    থাকে না, এক পলকও দেরি হয় না।
     */
    private val LOCK = Any()

    /** এক সারিকে চেনার নাম — কাজের ধরন | টেবিল | আইডি। */
    private fun keyOf(e: JSONObject): String =
        e.optString("kind") + "|" + e.optString("table") + "|" + e.optString("id")

    // 🔒 V220 (§2, 31.07.2026): body-র একটা ছোট হাতছাপ — server যা "কখনো নেবে না"
    // (permanent 4xx) সেই সারির data বদলেছে কিনা চিনতে। একই hash = একই ভুল data।
    private fun bodyHash(body: String): String = Integer.toHexString(body.hashCode())

    /** খাতার সারি B447 — শরীরের JSON-এ পুরনো (এখন সংঘর্ষে-পড়া) patientId-র
     *  জায়গায় এই মুহূর্তে সত্যিই ফাঁকা একটা নতুন Patient ID বসায় (একই
     *  branch+date থেকেই, `PatientIdGenerator`-এর নিজস্ব ক্লাউড-যাচাই-সহ
     *  প্রমাণিত নিয়মে)। ⛔ নাম/মোবাইল/টাকা/বাকি কোনো তথ্য ছোঁয়া হয় না —
     *  শুধু একটা ঘর। ব্রাঞ্চ/তারিখ না পাওয়া গেলে বা নতুন ID বানানো না গেলে
     *  `null` (তখন এই এন্ট্রি আগের মতোই আটকে থাকবে, জোর করে ভুল কিছু
     *  পাঠানো হয় না)। */
    private fun regeneratePatientIdInBody(context: Context, bodyStr: String): String? {
        if (bodyStr.isBlank()) return null
        val obj = try { JSONObject(bodyStr) } catch (_: Throwable) { return null }
        val branch = obj.optString("branch", "")
        val date = obj.optString("date", "").ifBlank { obj.optString("registrationDate", "") }
        if (branch.isBlank() || date.isBlank()) return null
        val newId = try { PatientIdGenerator.generate(branch, date, context) } catch (_: Throwable) { null }
        if (newId.isNullOrBlank()) return null
        obj.put("patientId", newId)
        return obj.toString()
    }

    /**
     * 🔒 খাতার সারি B166: এই ফোনে অপেক্ষমাণ কোনো **Delete** আছে কি না।
     * `null` = এখনো জানি না (একবার পড়ে দেখা হবে) · `false` = নেই · `true` = আছে।
     *
     * ⛔ **কেন দরকার:** `forget()` ডাকা হয় প্রতিটা সফল সেভের সঙ্গে
     * (`DeletedGuard.unmark` থেকে)। প্রতিবার ফাইল পড়লে সেটা হত **রোজকার
     * কাজের গায়ে বাড়তি ভার** — ঠিক খাতার সারি B27 · B31-এ TK-এর স্টাফদের যা
     * হয়েছিল (পর্দা ধীর হয়ে যাওয়া)। তাই সাধারণ অবস্থায় (কোনো Delete আটকে নেই)
     * একটাও ফাইল ছোঁয়া হয় না।
     */
    @Volatile private var hasDeletes: Boolean? = null

    /** অপেক্ষমাণ তালিকা দেখে উপরের চিহ্নটা ঠিক করা (তালার ভিতরে ডাকতে হয়)। */
    private fun recountDeletes(pending: JSONArray) {
        var found = false
        for (i in 0 until pending.length()) {
            val e = pending.optJSONObject(i) ?: continue
            if (e.optString("kind") == "DELETE") { found = true; break }
        }
        hasDeletes = found
    }

    /**
     * 🔒 V221 (§2, 31.07.2026): সারিতে (pending বা failed) কিছু জমা আছে কিনা।
     * `null` = এখনো জানি না · `false` = দুটোই ফাঁকা · `true` = আছে।
     * ⛔ কেন দরকার: `clearConfirmed()` ডাকা হয় **প্রতিটা সফল cloud-লেখার সঙ্গে**
     *    (SupabaseClient upsert/updateById)। খাতার সারি B27/B31-এর মতো রোজকার
     *    সেভে বাড়তি ভার এড়াতে — সারি ফাঁকা থাকলে (স্বাভাবিক অবস্থা) একটাও ফাইল
     *    পড়া/পার্স করা হয় না, ঠিক `hasDeletes`-এর মতো। */
    @Volatile private var hasQueue: Boolean? = null

    /** দুই তালিকা দেখে উপরের চিহ্নটা ঠিক করা (তালার ভিতরে, `p` হাতে থাকলে)। */
    private fun recountQueue(p: android.content.SharedPreferences) {
        val a = try { JSONArray(p.getString(KEY, "[]") ?: "[]").length() } catch (_: Throwable) { 0 }
        val b = try { JSONArray(p.getString(KEY_FAILED, "[]") ?: "[]").length() } catch (_: Throwable) { 0 }
        hasQueue = (a > 0 || b > 0)
    }

    /**
     * 🔒🔒 V221 (§2) + V222 (§1, 31.07.2026): একটা row সত্যিই cloud-এ **সফলভাবে
     * বসার** পরে (SupabaseClient upsert `ok` / updateById `changed`) ঐ **একই
     * Table+Record**-এর আটকে থাকা **পুরোনো/সম্পন্ন** UPSERT/UPDATE কাজ মুছে দেওয়া
     * হয় — যাতে সংশোধিত record সফল হওয়ার পরেও পুরোনো HTTP 400 entry ও লাল
     * সতর্কবাতি থেকে না যায়।
     *
     * 🔒🔒 V222 (§1) — **নতুন Pending কাজ কখনো হারাবে না** (দুই পাহারা):
     *  ১. **সময়-পাহারা (`writeStart`):** শুধু এই সফল লেখা **শুরুর আগে জমা** (at ≤
     *     writeStart) কাজই মোছা হয়। এই লেখা চলাকালীন বা পরে জমা **নতুন** কাজ (নতুন
     *     Remark/Date/Payment/Follow-up — at > writeStart) কখনো ছোঁয়া হয় না।
     *  ২. **supersede-পাহারা:**
     *     • সফল লেখা **UPSERT** (পুরো row) হলে — পুরো অবস্থা বহন করে বলে ঐ id-র
     *       পুরোনো UPSERT/UPDATE বাতিল ধরা নিরাপদ।
     *     • সফল লেখা **UPDATE** (আংশিক patch) হলে — শুধু এমন পুরোনো UPDATE মোছা হয়
     *       যার **সব ঘর এই সফল patch-এও আছে** (subset)। **আলাদা-ঘরের** পুরোনো
     *       UPDATE (যেমন Pending Remark, আর সফল হলো Date) কখনো মোছা হয় না, আর
     *       পুরোনো পুরো-row UPSERT-ও UPDATE দিয়ে মোছা হয় না।
     *
     * ⛔ অন্য কোনো Record (আলাদা id) কখনো ছোঁয়া হয় না। DELETE ছোঁয়া হয় না।
     * ⛔ id ফাঁকা হলে কিছুই করে না। সারি ফাঁকা হলে (`hasQueue == false`) একটাও ফাইল
     *    ছোঁয়া হয় না — রোজকার সেভ এক পলকও ধীর হয় না।
     *
     * @param confirmedKind সফল লেখাটা "UPSERT" না "UPDATE"।
     * @param confirmedBody UPDATE হলে সফল patch-এর ঘরগুলো (subset যাচাইয়ে); UPSERT-এ null।
     * @param writeStart সফল লেখার নেট-কল **শুরুর আগে** ধরা সময় (ms)।
     */
    fun clearConfirmed(confirmedKind: String, table: String, id: String, confirmedBody: JSONObject?, writeStart: Long) {
        val ctx = appContext ?: return
        if (table.isBlank() || id.isBlank()) return
        if (hasQueue == false) return   // 🔒 সস্তা পাহারা — সারি ফাঁকা হলে ফাইল ছোঁয়া হয় না
        try {
            synchronized(LOCK) {
                val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                // UPDATE হলে সফল patch-এর ঘরগুলো (subset যাচাইয়ের জন্য)। UPSERT হলে null।
                val confKeys: HashSet<String>? =
                    if (confirmedKind == "UPDATE" && confirmedBody != null) {
                        val s = HashSet<String>()
                        val it = confirmedBody.keys()
                        while (it.hasNext()) s.add(it.next())
                        s
                    } else null
                var touched = false
                val ed = p.edit()
                for (key in listOf(KEY, KEY_FAILED)) {
                    val cur = try {
                        JSONArray(p.getString(key, "[]") ?: "[]")
                    } catch (_: Throwable) { JSONArray() }
                    if (cur.length() == 0) continue
                    val kept = JSONArray()
                    for (i in 0 until cur.length()) {
                        val e = cur.optJSONObject(i) ?: continue
                        val k = e.optString("kind")
                        val sameRow = (k == "UPSERT" || k == "UPDATE") &&
                            e.optString("table") == table && e.optString("id") == id
                        if (!sameRow) { kept.put(e); continue }
                        // 🔒 সময়-পাহারা: এই সফল লেখা শুরুর পরে জমা নতুন কাজ কখনো নয়।
                        val at = e.optLong("at", 0L)
                        if (at > writeStart) { kept.put(e); continue }
                        // 🔒 supersede-পাহারা।
                        val supersede = if (confirmedKind == "UPSERT") {
                            true
                        } else {
                            // confirmedKind == "UPDATE": শুধু subset পুরোনো UPDATE।
                            if (k != "UPDATE" || confKeys == null) {
                                false
                            } else {
                                val storedKeys = try {
                                    val b = JSONObject(e.optString("body", "{}"))
                                    val s = HashSet<String>()
                                    val it = b.keys()
                                    while (it.hasNext()) s.add(it.next())
                                    s
                                } catch (_: Throwable) { null }
                                storedKeys != null && storedKeys.isNotEmpty() && confKeys.containsAll(storedKeys)
                            }
                        }
                        if (supersede) touched = true else kept.put(e)
                    }
                    if (kept.length() != cur.length()) ed.putString(key, kept.toString())
                }
                if (touched) ed.commit()
                // চিহ্ন হালনাগাদ — ফাঁকা হলে false → পরের বার সস্তা।
                recountQueue(p)
                if (touched) {
                    val after = try {
                        JSONArray(p.getString(KEY, "[]") ?: "[]")
                    } catch (_: Throwable) { JSONArray() }
                    recountDeletes(after)
                }
            }
        } catch (_: Throwable) { }
    }

    @Volatile private var appContext: Context? = null

    /** Called once when the app starts, so any part of the app can be covered. */
    fun attach(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    /** "যায়নি" ঘরে একটা নোট তোলা। ⛔ রোগীর তথ্য নয় — শুধু টেবিলের নাম, সারির
     *  আইডি, কেন যায়নি ও কখন। তালিকা কখনো বড় হয় না (সর্বোচ্চ ৩০০)। */
    private fun withFailedAdded(
        p: android.content.SharedPreferences,
        items: List<JSONObject>,
        why: String
    ): JSONArray {
        val cur = try {
            JSONArray(p.getString(KEY_FAILED, "[]") ?: "[]")
        } catch (_: Throwable) { JSONArray() }
        for (e in items) {
            try {
                cur.put(
                    JSONObject()
                        .put("kind", e.optString("kind"))
                        .put("table", e.optString("table"))
                        .put("id", e.optString("id"))
                        .put("body", e.optString("body"))
                        // 🔒 V219 (§4, 31.07.2026): per-entry why থাকলে সেটাই, নইলে
                        // পাঠানো default। আগে সবার জন্য "tried_50_times" বসত।
                        .put("why", e.optString("why").ifBlank { why })
                        // 🔒 V219 (§4): আসল HTTP কারণ (যেমন "HTTP 400") এখন এখানেও
                        // রাখা হয় — আগে failed-এ সরানোর সময় `lastError` মুছে যেত,
                        // তাই যে সারিগুলো সবচেয়ে বেশি ব্যাখ্যা দরকার সেগুলোরই কারণ
                        // হারিয়ে যেত। এখন Table+Record-এর সঙ্গে সহজ কারণও দেখানো যায়।
                        .put("lastError", e.optString("lastError"))
                        // 🔒 V220 (§2): permanent (server-rejected) চিহ্ন ও body-hash-ও
                        // রাখা হয়, যাতে হুবহু একই ভুল data আর আপনা থেকে না পাঠানো হয়।
                        .put("permanent", e.optBoolean("permanent", false))
                        .put("permBodyHash", e.optString("permBodyHash"))
                        // 🔒 V222 (§1): মূল enqueue-সময় `at`-ও রাখা হয়, যাতে failed-ঘরের
                        // এন্ট্রিতেও clearConfirmed-এর সময়-পাহারা (at > writeStart হলে রাখো)
                        // হুবহু একইভাবে কাজ করে — কোনো নতুন কাজ যেন কখনো না হারায়।
                        .put("at", e.optLong("at", 0L))
                        .put("failedAt", System.currentTimeMillis())
                )
            } catch (_: Throwable) { }
        }
        if (cur.length() <= MAX_FAILED) return cur
        val start = cur.length() - MAX_FAILED
        val t = JSONArray()
        for (i in start until cur.length()) t.put(cur.get(i))
        return t
    }

    /**
     * 🔒🔒 খাতার সারি B194 (TK, 30.07.2026 রাত — "fast Wifi-র মধ্যেও কেন এরকম
     * হচ্ছে?"): TK দেখেছেন "১টি তথ্য ক্লাউডে যায়নি" বার্তা, "পাঠান" চাপার পরেও
     * "নেটওয়ার্ক ফিরলে আবার চেষ্টা করুন" বলে — কিন্তু TK ফাস্ট WiFi-তে ছিলেন,
     * তাই আসল কারণ জানতে চান।
     *
     * **আগে যা ছিল:** একটা সেভ ব্যর্থ হলে **শুধু "ব্যর্থ হয়েছে" মনে রাখা হত** —
     * কেন ব্যর্থ হয়েছে (নেট আসলেই খারাপ, নাকি সার্ভার কোনো নির্দিষ্ট কারণে
     * প্রত্যাখ্যান করেছে) তা কোথাও লেখা থাকত না, তাই বার্তাটা সবসময় সন্দেহাতীতভাবে
     * "নেটওয়ার্ক" বলত — যদিও প্রকৃত কারণ ভিন্ন হতে পারত।
     *
     * **এখন:** নতুন ঐচ্ছিক `reason` প্যারামিটার (ডিফল্ট ফাঁকা স্ট্রিং — তাই
     * পুরনো কোনো ডাক এক অক্ষরও বদলায়নি) — ব্যর্থতার সংক্ষিপ্ত আসল কারণ
     * (`SupabaseClient`-এ ধরা, যেমন "HTTP 401" বা "Timeout") এন্ট্রির নিজের
     * JSON-এ `lastError` হিসেবে বসে। ⛔ শুধু **দেখানোর জন্য** — কোনো পাঠানো/
     * retry/tries-গোনার নিয়ম এক লাইনও বদলায়নি, dedup-এর পুরনো আচরণও অক্ষত।
     */
    /* 📱🔒 V1291 (১০.০৯.২০২৬, তালিকা ৪১১-⑨ খ, TK: *"খ করুন, সাবধানে"*) —
       **সারি এখনো সার্ভারে পৌঁছয়নি, অথচ তার এডিট এসে গেছে।**
       ঘটনা: নেট ছাড়া নতুন সারি (UPSERT) এখানে "পাঠানো বাকি"-তে জমা; নেট ফিরলে
       জমা কাজ পাঠানোর **আগেই** স্টাফ সেই সারি এডিট করলে সরাসরি PATCH যায় → সার্ভারে
       সারি নেই → B593-নিয়মে (TK-অনুমোদিত) কাজটা চুপচাপ বাদ পড়ত → পরে জমা UPSERT
       পুরনো কপি বসাত ⇒ এডিট হারাত, স্টাফ "Saved" দেখতেন।
       এখন: PATCH-এ ০ সারি ফিরলে আগে এখানে ওই টেবিল+id-র জমা UPSERT খোঁজা হয়;
       থাকলে এডিটের ঘরগুলো তার ভিতরে বসিয়ে দেওয়া হয় (`updatedAt` সহ) — জমা কাজটা
       গেলে এডিট সমেত যাবে। ⛔ জমা UPSERT না থাকলে (সারি সত্যিই মুছে গেছে/কখনো ছিল না)
       B593-নিয়ম হুবহু আগের মতোই — কিছু বদলায় না। ⛔ "যায়নি" (failed) ঘর ছোঁয়া হয় না।
       @return true = জুড়ে দেওয়া গেছে। */
    fun mergeIntoPendingUpsert(table: String, id: String, fields: JSONObject?): Boolean {
        val ctx = appContext ?: return false
        if (table.isBlank() || id.isBlank() || fields == null || fields.length() == 0) return false
        return try {
            synchronized(LOCK) {
                val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                val list = JSONArray(p.getString(KEY, "[]") ?: "[]")
                var merged = false
                for (i in 0 until list.length()) {
                    val e = list.optJSONObject(i) ?: continue
                    if (e.optString("kind") != "UPSERT" || e.optString("table") != table || e.optString("id") != id) continue
                    val body = try { JSONObject(e.optString("body", "{}")) } catch (_: Throwable) { continue }
                    if (body.length() == 0) continue
                    val keys = fields.keys()
                    while (keys.hasNext()) { val k = keys.next(); body.put(k, fields.get(k)) }
                    e.put("body", body.toString())
                    merged = true
                }
                if (merged) {
                    p.edit().putString(KEY, list.toString()).commit()
                    hasQueue = true
                }
                merged
            }
        } catch (_: Throwable) { false }
    }

    fun remember(kind: String, table: String, id: String, payload: JSONObject?, reason: String = "") {
        val ctx = appContext ?: return
        if (table.isBlank()) return
        // 🔒🔒 খাতার সারি B145-এর যাচাইয়ে নিজে ধরা পড়েছে (30.07.2026):
        // **আইডি ছাড়া UPSERT কখনো আবার পাঠানো যাবে না।** কারণ Supabase আইডি
        // দেখেই বোঝে "এটা পুরনো সারিটাই" — আইডি না থাকলে সে **নতুন সারি বানিয়ে
        // ফেলে**। নেট কেটে যাওয়ার সময় সারিটা সার্ভারে হয়তো আগেই বসে গেছে, শুধু
        // উত্তরটা আসেনি; তখন আইডিহীন কাজ আবার পাঠালে **একই টাকা দুবার** বসে
        // যেতে পারত।
        // ✅ প্রজেক্টের ৪১টা upsert-এর প্রতিটা হাতে মিলিয়ে দেখা হয়েছে — সবগুলোই
        //    নিজের আইডি বসায় (`pay_…`, `dv_…`, `bdr_…`, `trash_…` ইত্যাদি), তাই
        //    আজ এই অবস্থা আসেই না। এটা ভবিষ্যতের জন্য পাহারা।
        if (kind == "UPSERT" && id.isBlank()) return
        if ((kind == "UPDATE" || kind == "DELETE") && id.isBlank()) return
        try {
            val body = payload?.toString() ?: ""
            // 🔒🔒 খাতার সারি B164: **পড়া → বদলানো → লেখা** — পুরো তিন ধাপ একটাই
            // তালার ভিতরে। নইলে একই মুহূর্তে দুটো কাজ ব্যর্থ হলে একজনের লেখা
            // অন্যজনের লেখাকে চাপা দিয়ে দিত (একটা কাজ নিঃশব্দে হারাত)।
            synchronized(LOCK) {
            val p0 = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            // 🚨 খাতার সারি B145 (৪ নম্বর আলোচনা): সীমার চেয়ে বড় তথ্য (যেমন খুব
            // বড় ছবি) এখানে রাখা হয় না — কিন্তু আগে সেটা **নীরবে** ফিরে যেত।
            // এখন অন্তত একটা ছোট নোট রেখে যায়, যাতে TK জানতে পারেন।
            // ⛔ ছবির নিজের queue (`GenericUpdateQueue`) আগের মতোই কাজ করে, তাই
            //    কাজটা আসলে হারায় না — এটা শুধু জানানোর জন্য।
            if (body.length > MAX_PAYLOAD_CHARS) {
                try {
                    val note = JSONObject()
                        .put("kind", kind).put("table", table).put("id", id).put("body", "")
                    p0.edit().putString(
                        KEY_FAILED, withFailedAdded(p0, listOf(note), "too_large").toString()
                    ).commit()
                    hasQueue = true   // 🔒 V221 (§2): failed-ঘরে নোট জমল
                } catch (_: Throwable) { }
                return
            }
            val p = p0
            val list = JSONArray(p.getString(KEY, "[]") ?: "[]")
            // the same row waiting twice is pointless -- keep the newer one
            val cleaned = JSONArray()
            for (i in 0 until list.length()) {
                val e = list.optJSONObject(i) ?: continue
                val same = e.optString("table") == table && e.optString("id") == id &&
                    e.optString("kind") == kind
                if (!same) cleaned.put(e)
            }
            // 🔒 V220 (§2, 31.07.2026): "যায়নি" ঘরে এই সারির একটা **permanent**
            // (server-rejected 4xx) এন্ট্রি আছে কিনা দেখা হয় —
            //  • হুবহু একই body (একই ভুল data) হলে → pending-এ যোগ করা হয় **না**,
            //    parked-ই থাকে। তাই একই ভুল অ্যাপ নিজে থেকে আর বারবার পাঠায় না।
            //    (⛔ Retry বন্ধ নয় — "পাঠান" চাপলে retryFailed এখনো এটাকে সুযোগ দেয়,
            //     আর নেট-ব্যর্থ/সংশোধিত সারি আগের মতোই যায়।)
            //  • body বদলালে (record ঠিক করা হয়েছে) → parked এন্ট্রি মুছে ফেলা হয়,
            //    নতুন data স্বাভাবিকভাবে আবার retry-তে যায়।
            run {
                val want = kind + "|" + table + "|" + id
                val newHash = bodyHash(body)
                val failedArr = try { JSONArray(p.getString(KEY_FAILED, "[]") ?: "[]") } catch (_: Throwable) { JSONArray() }
                var permanentSameBody = false
                var failedChanged = false
                val keptFailed = JSONArray()
                for (i in 0 until failedArr.length()) {
                    val fe = failedArr.optJSONObject(i) ?: continue
                    if (keyOf(fe) == want && fe.optBoolean("permanent", false)) {
                        if (fe.optString("permBodyHash") == newHash) { permanentSameBody = true; keptFailed.put(fe) }
                        else failedChanged = true   // corrected — এই permanent এন্ট্রি বাদ
                    } else keptFailed.put(fe)
                }
                if (permanentSameBody) {
                    // একই ভুল data — শুধু pending থেকে stale dup সরিয়ে commit, নতুন যোগ নয়।
                    p.edit().putString(KEY, cleaned.toString()).commit()
                    recountQueue(p)   // 🔒 V221 (§2): তালিকা বদলাল — চিহ্ন ঠিক করা
                    return
                }
                if (failedChanged) {
                    p.edit().putString(KEY_FAILED, keptFailed.toString()).commit()
                }
            }
            cleaned.put(
                JSONObject()
                    .put("kind", kind).put("table", table).put("id", id)
                    .put("body", body).put("tries", 0)
                    .put("at", System.currentTimeMillis())
                    // 🔒 খাতার সারি B194: ফাঁকা হলে ঘরটাই বসে না (পুরনো এন্ট্রির
                    // আকারে কোনো বাড়তি চিহ্ন থাকে না)।
                    .let { if (reason.isNotBlank()) it.put("lastError", reason.take(200)) else it }
            )
            // keep the newest entries only, so this list can never grow forever
            // 🚨 খাতার সারি B145: বাদ পড়া পুরনো কাজগুলো এখন **নীরবে হারায় না** —
            // "যায়নি" ঘরে তুলে রাখা হয়।
            var failedJson: String? = null
            val dropped = ArrayList<JSONObject>()
            var trimmed = if (cleaned.length() > MAX_ENTRIES) {
                val start = cleaned.length() - MAX_ENTRIES
                for (i in 0 until start) {
                    cleaned.optJSONObject(i)?.let { dropped.add(it) }
                }
                val t = JSONArray()
                for (i in start until cleaned.length()) t.put(cleaned.get(i))
                t
            } else cleaned
            // মাপের সীমা — সংখ্যা কম হলেও তথ্য বড় হতে পারে, তাই এটাও দেখা হয়।
            while (trimmed.length() > 1 && trimmed.toString().length > MAX_TOTAL_CHARS) {
                trimmed.optJSONObject(0)?.let { dropped.add(it) }
                val t = JSONArray()
                for (i in 1 until trimmed.length()) t.put(trimmed.get(i))
                trimmed = t
            }
            if (dropped.isNotEmpty()) {
                failedJson = withFailedAdded(p, dropped, "list_full").toString()
            }
            val ed = p.edit().putString(KEY, trimmed.toString())
            if (failedJson != null) ed.putString(KEY_FAILED, failedJson)
            ed.commit()
            hasQueue = true   // 🔒 V221 (§2): pending-এ নতুন কাজ জমল
            // 🔒 খাতার সারি B166: অপেক্ষমাণ Delete আছে কি না, চিহ্নটা ঠিক করা।
            if (kind == "DELETE") hasDeletes = true
            }   // 🔒 খাতার সারি B164 — তালা শেষ
        } catch (_: Throwable) {}
    }

    /**
     * 🔒🔒 খাতার সারি B166 (TK, 30.07.2026 — TK-এর ৩ নম্বর সন্দেহ):
     * ব্যর্থ **Delete** এখন এই তালিকায় ওঠে, তাই নেট ফিরলে নিজে থেকেই শেষ হয়।
     * **কিন্তু তাতে একটা বিপদ তৈরি হত, আর এটাই সেই পাহারা:**
     *
     * ডিলিট ব্যর্থ হলো → তালিকায় "মুছে ফেলো" জমা রইল → এর মধ্যে মাস্টার
     * **Trash থেকে রেকর্ডটা Restore করলেন** → পরে তালিকাটা পাঠানোর সময় ওই
     * পুরনো "মুছে ফেলো" চলে যেত এবং **ফেরানো রেকর্ডটা আবার মুছে যেত**।
     *
     * তাই যেই মুহূর্তে একটা সারি **ফিরে আসে বা নতুন করে লেখা হয়**, ওই সারির
     * অপেক্ষমাণ "মুছে ফেলো" এখানে **মুছে ফেলা হয়** — সবচেয়ে নতুন ইচ্ছেটাই জেতে।
     *
     * ⛔ শুধু ওই একটা সারির, ওই একটা ধরনের কাজই সরানো হয় — বাকি কিছুই ছোঁয়া হয় না।
     */
    fun forget(kind: String, table: String, id: String) {
        val ctx = appContext ?: return
        if (table.isBlank() || id.isBlank()) return
        val want = kind + "|" + table + "|" + id
        // 🔒 খাতার সারি B166: অপেক্ষমাণ কোনো Delete নেই — তাহলে দেখারও কিছু নেই,
        // একটাও ফাইল ছোঁয়া হয় না (রোজকার সেভ যেন এক পলকও ধীর না হয়)।
        if (kind == "DELETE" && hasDeletes == false) return
        try {
            synchronized(LOCK) {
                val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                var touched = false
                val ed = p.edit()
                for (key in listOf(KEY, KEY_FAILED)) {
                    val cur = try {
                        JSONArray(p.getString(key, "[]") ?: "[]")
                    } catch (_: Throwable) { JSONArray() }
                    if (cur.length() == 0) continue
                    val kept = JSONArray()
                    for (i in 0 until cur.length()) {
                        val e = cur.optJSONObject(i) ?: continue
                        if (keyOf(e) == want) { touched = true; continue }
                        kept.put(e)
                    }
                    if (kept.length() != cur.length()) ed.putString(key, kept.toString())
                }
                if (touched) ed.commit()
                if (touched) {
                    val after = try {
                        JSONArray(p.getString(KEY, "[]") ?: "[]")
                    } catch (_: Throwable) { JSONArray() }
                    recountDeletes(after)   // 🔒 খাতার সারি B166
                    recountQueue(p)         // 🔒 V221 (§2)
                }
            }
        } catch (_: Throwable) { }
    }

    /** V378: surgically removes obsolete fields from queued UPDATE bodies for
     * one row, preserving every unrelated pending field. Used when a nested
     * Referral Income entry is intentionally edited/deleted. */
    fun discardUpdateFields(table: String, id: String, fieldNames: Set<String>) {
        val ctx = appContext ?: return
        if (table.isBlank() || id.isBlank() || fieldNames.isEmpty()) return
        try {
            synchronized(LOCK) {
                val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                val ed = p.edit()
                for (key in listOf(KEY, KEY_FAILED)) {
                    val cur = try { JSONArray(p.getString(key, "[]") ?: "[]") } catch (_: Throwable) { JSONArray() }
                    val kept = JSONArray()
                    for (i in 0 until cur.length()) {
                        val e = cur.optJSONObject(i) ?: continue
                        if (e.optString("kind") != "UPDATE" || e.optString("table") != table || e.optString("id") != id) {
                            kept.put(e); continue
                        }
                        val body = try { JSONObject(e.optString("body", "{}")) } catch (_: Throwable) { JSONObject() }
                        fieldNames.forEach { body.remove(it) }
                        if (body.length() > 0) {
                            e.put("body", body.toString()).put("tries", 0).put("lastError", "")
                            e.remove("permanent"); e.remove("permBodyHash"); e.remove("why")
                            kept.put(e)
                        }
                    }
                    ed.putString(key, kept.toString())
                }
                ed.commit()
            }
        } catch (_: Throwable) { }
    }

    /** How many pieces of work are still waiting (for a status line if ever needed). */
    fun pendingCount(context: Context): Int = try {
        JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]").length()
    } catch (_: Throwable) { 0 }

    /** 🚨 খাতার সারি B145: যেগুলো আর পাঠানো যায়নি — লাল সতর্কবাতিতে দেখানোর জন্য। */
    fun failedCount(context: Context): Int = try {
        JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_FAILED, "[]") ?: "[]").length()
    } catch (_: Throwable) { 0 }

    /**
     * 🔒🔒 B274 (02.08.2026, TK-অনুমোদিত — "হ্যাঁ করুন"): "যায়নি" ঘরের এন্ট্রি
     * **স্থায়ীভাবে ছেড়ে দেওয়ার** ব্যবস্থা। TK-এর ২টা `followups` এন্ট্রি
     * (`row_not_matched` — সার্ভারে ওই সারিটাই আর নেই) কোনোদিন সফল হবে না,
     * TK নিজে জেনেশুনে "ছেড়ে দিতে" বলেছেন।
     *
     * ⛔⛔ **শুধু "যায়নি" (`KEY_FAILED`) ঘর ছোঁয়া হয় — সচল "পাঠানো বাকি"
     * (`KEY`, pending) ঘর কখনো এটা ছুঁতে পারবে না।** তাই এখনো-সফল-হতে-পারা
     * কোনো কাজ কখনো এই ফাংশনে হারায় না।
     * ⛔ কোনো ডেটাবেস কল নেই — শুধু ফোনের নিজের অপেক্ষার তালিকা খালি হয়।
     * ⛔ যে রেকর্ড/টাকার সারি নিয়ে এই এন্ট্রি ছিল, সেটা Payment History/Patient
     *    Timeline-এ যা ছিল তাই থাকে — এটা শুধু "আবার পাঠানোর চেষ্টা" বন্ধ করে।
     *
     * @return কতগুলো এন্ট্রি ছেড়ে দেওয়া হলো
     */
    fun clearFailed(context: Context): Int {
        return try {
            synchronized(LOCK) {
                val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                val n = try { JSONArray(p.getString(KEY_FAILED, "[]") ?: "[]").length() } catch (_: Throwable) { 0 }
                if (n > 0) {
                    p.edit().putString(KEY_FAILED, "[]").commit()
                    recountQueue(p)
                }
                n
            }
        } catch (_: Throwable) { 0 }
    }

    /**
     * 🔒🔒 খাতার সারি B194 (TK, 30.07.2026 রাত): "fast Wifi-তেও কেন ব্যর্থ হচ্ছে"
     * প্রশ্নের উত্তর দিতে — অপেক্ষমাণ/যায়নি দুই তালিকা মিলিয়ে সবচেয়ে সাম্প্রতিক
     * আসল কারণ (`lastError`) খুঁজে বার করে। ⛔ **শুধু পড়ে**, কিছুই লেখে না বা
     * বদলায় না — Dashboard-এর সতর্কবাতির লেখায় দেখানোর জন্য একটা ছোট বাক্য।
     * পাওয়া না গেলে ফাঁকা স্ট্রিং (পুরনো বার্তাই দেখা যাবে, কিছু ভাঙে না)।
     */
    fun peekLastError(context: Context): String {
        return try {
            var best: String = ""
            var bestAt: Long = -1
            fun scan(json: String) {
                val arr = try { JSONArray(json) } catch (_: Throwable) { return }
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    val err = e.optString("lastError", "")
                    if (err.isBlank()) continue
                    val at = e.optLong("at", 0L)
                    if (at >= bestAt) { bestAt = at; best = err }
                }
            }
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            scan(p.getString(KEY, "[]") ?: "[]")
            scan(p.getString(KEY_FAILED, "[]") ?: "[]")
            best
        } catch (_: Throwable) { "" }
    }

    /**
     * 🔒 V219 (§4, 31.07.2026): আটকে থাকা প্রতিটা সারির **Table · Record · সহজ কারণ**
     * এক নজরে — শুধু যেগুলোর একটা আসল কারণ (`lastError`/`why`) আছে (অর্থাৎ সত্যিই
     * সমস্যা, নিছক নেট-অপেক্ষা নয়)। ⛔ শুধু পড়ে, কিছুই লেখে না। রোগীর কোনো তথ্য নয় —
     * শুধু টেবিলের নাম, সারির আইডি ও কারণ। সর্বোচ্চ `max`টা দেখানো হয়।
     * উদাহরণ: "payments · …a1b2c3 — HTTP 400 · followups · …d4e5 — HTTP 400 · আরও ২"
     */
    fun stuckDetail(context: Context, max: Int = 3): String {
        return try {
            data class S(val table: String, val id: String, val reason: String)
            val out = LinkedHashMap<String, S>()
            fun scan(json: String) {
                val arr = try { JSONArray(json) } catch (_: Throwable) { return }
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    val table = e.optString("table"); if (table.isBlank()) continue
                    val id = e.optString("id")
                    val reason = e.optString("lastError").ifBlank { e.optString("why") }
                    if (reason.isBlank()) continue   // নিছক অপেক্ষমাণ (কোনো error নেই) — বাদ
                    out["$table|$id"] = S(table, id, reason)
                }
            }
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            scan(p.getString(KEY_FAILED, "[]") ?: "[]")   // "যায়নি" আগে দেখানো হয়
            scan(p.getString(KEY, "[]") ?: "[]")
            if (out.isEmpty()) return ""
            val items = out.values.toList()
            val shown = items.take(max).joinToString(" · ") { s ->
                val shortId = if (s.id.length > 8) "…" + s.id.takeLast(8) else s.id
                "${s.table} · $shortId — ${s.reason}"
            }
            if (items.size > max) "$shown · আরও ${items.size - max}" else shown
        } catch (_: Throwable) { "" }
    }

    /**
     * 🚨 খাতার সারি B145: "পাঠান" বোতাম চাপলে "যায়নি" ঘরের কাজগুলোকে আবার
     * অপেক্ষমাণ তালিকায় ফেরানো হয় (গোনা আবার ০ থেকে), তারপর `flush()` চলে।
     * ⛔ যেগুলোর তথ্যই রাখা যায়নি (খুব বড় ছবি) সেগুলো পাঠানো সম্ভব নয়, তাই
     *    ওগুলোর নোট এখানে **মুছে দেওয়া হয়** — নইলে হোম পেজের লাল বার চিরকাল
     *    থেকে যেত ও "পাঠান" চেপেও সংখ্যা কমত না। ⛔ কাজটা তাতে হারায় না:
     *    ছবির নিজের queue (`GenericUpdateQueue`) ওটা পাঠায় এবং সেটা
     *    সতর্কবাতিতে আলাদা করে গোনা হয়।
     */
    fun retryFailed(context: Context) {
        try {
            // 🔒 খাতার সারি B164 — এই ঘরটাও একই তালার নিচে, নইলে "পাঠান" চাপার
            //    ঠিক সেই মুহূর্তে কোনো সেভ ব্যর্থ হলে একটা লেখা চাপা পড়ে যেত।
            synchronized(LOCK) {
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val failed = try {
                JSONArray(p.getString(KEY_FAILED, "[]") ?: "[]")
            } catch (_: Throwable) { JSONArray() }
            if (failed.length() == 0) return
            val pending = try {
                JSONArray(p.getString(KEY, "[]") ?: "[]")
            } catch (_: Throwable) { JSONArray() }
            val stillFailed = JSONArray()
            for (i in 0 until failed.length()) {
                val e = failed.optJSONObject(i) ?: continue
                val body = e.optString("body")
                if (body.isBlank() || e.optString("table").isBlank()) {
                    // 🔒 যাচাইয়ে ধরা পড়েছে (30.07.2026): যেগুলোর তথ্যই রাখা যায়নি
                    // (খুব বড় ছবি) সেগুলো কোনোদিন পাঠানো সম্ভব নয়। ওগুলো ঘরে
                    // রেখে দিলে হোম পেজের **লাল বার চিরকাল দেখাত** ও "পাঠান"
                    // চেপেও সংখ্যা কমত না — TK সেটাকে নতুন বাগ হিসেবে দেখতেন।
                    // ⛔ ওই কাজটা আসলে হারায় না — ছবির নিজের queue
                    //    (`GenericUpdateQueue`) ওটা পাঠায়, আর সেটা সতর্কবাতিতে
                    //    আলাদা করে গোনাও হয়। তাই "পাঠান" চাপলে এই নোটটা উঠে যায়।
                    continue
                }
                pending.put(
                    JSONObject()
                        .put("kind", e.optString("kind")).put("table", e.optString("table"))
                        .put("id", e.optString("id")).put("body", body)
                        .put("tries", 0).put("at", System.currentTimeMillis())
                )
            }
            p.edit()
                .putString(KEY, pending.toString())
                .putString(KEY_FAILED, stillFailed.toString())
                .commit()
            recountDeletes(pending)   // 🔒 খাতার সারি B166
            recountQueue(p)           // 🔒 V221 (§2)
            }   // 🔒 খাতার সারি B164 — তালা শেষ
        } catch (_: Throwable) { }
    }

    /** Sends everything that is still waiting. Anything that fails stays for next time. */
    fun flush(context: Context) {
        try {
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            // 🔒🔒 খাতার সারি B164: তালিকাটা **তালার ভিতরে** পড়া হয়, কিন্তু
            // ⛔ **নেটের কাজ তালার বাইরে** — নইলে পাঠানো চলার পুরো সময়টা নতুন
            //    সেভ আটকে থাকত (দুর্বল লাইনে সেটা অনেকক্ষণ হতে পারত)।
            val list = synchronized(LOCK) {
                try { JSONArray(p.getString(KEY, "[]") ?: "[]") } catch (_: Throwable) { JSONArray() }
            }
            if (list.length() == 0) return
            // 🔒 খাতার সারি B164: আগে এটা একটা সাধারণ তালিকা ছিল; এখন সারির
            // নাম ধরে রাখা হয়, যাতে একই সারির দুটো কপি কখনো পাশাপাশি না থাকে।
            val left = LinkedHashMap<String, JSONObject>()
            // 🚨 খাতার সারি B145: ৫০ বার চেষ্টার পরেও না গেলে আগে **নীরবে বাদ**
            // পড়ত। এখন "যায়নি" ঘরে তুলে রাখা হয়।
            val giveUp = ArrayList<JSONObject>()
            for (i in 0 until list.length()) {
                val e = list.optJSONObject(i) ?: continue
                val kind = e.optString("kind")
                val table = e.optString("table")
                val id = e.optString("id")
                val tries = e.optInt("tries", 0)
                if (table.isBlank()) continue
                if (tries >= MAX_TRIES) { giveUp.add(e); continue }
                // 🔒 V219 (§4): নিরাপদ retry — HTTP 400/404/422 মানে server ঐ
                // সারিটা যেমন আছে তেমন **কখনোই** নেবে না (হুবহু একই body আবার
                // পাঠালে আবার একই 400)। তাই ৫০ বার বৃথা চেষ্টা না করে, ২ বার চেষ্টার
                // পরে সঙ্গে সঙ্গে "যায়নি" ঘরে (আসল কারণসহ) সরিয়ে দেওয়া হয় — Free-plan
                // কোটাও বাঁচে, আর TK Table+Record+কারণ দেখে ঠিক করতে পারেন।
                // ⛔ 401/403/409/429 (auth/conflict/rate) permanent নয় — সেগুলো আগের
                //    মতোই retry হয় (এখানে ধরা হয়নি)।
                val le = e.optString("lastError", "")
                // 🔒 B274 (02.08.2026, TK-অনুরোধ — ঝুঁকিহীন): `row_not_matched`
                // মানে সার্ভারে ওই id-র সারিটাই নেই (খুঁজে না পাওয়ায় ০টা সারি
                // ফেরত এসেছে) — HTTP 400/404/422-এর মতোই এটাও **কখনো নিজে থেকে
                // ঠিক হবে না** (রেকর্ডটা হঠাৎ সার্ভারে চলে আসবে না)। আগে এটা
                // সাধারণ ব্যর্থতা হিসেবে ৫০ বার (MAX_TRIES) চেষ্টা করে তবেই
                // "যায়নি" ঘরে যেত — শুধু Supabase ফ্রি-প্ল্যানের কোটা নষ্ট হত,
                // কখনো সফল হওয়ার সুযোগ ছিল না। এখন এটাও ২ বারের পরেই সরাসরি
                // "যায়নি" ঘরে (আসল কারণসহ) সরে যায় — ঠিক নিচের HTTP
                // 400/404/422-এর নিয়মেই। ⛔ তথ্য হারায় না — "যায়নি" ঘরে থাকে,
                // "পাঠান" চাপলে আবার চেষ্টা হয়।
                val permanentHttp = le.contains("HTTP 400") || le.contains("HTTP 404") || le.contains("HTTP 422")
                // 🆕 (03.08.2026, TK-নির্দেশ — "এটা বারবার দেখে বিভ্রান্ত হয়ে
                // যাচ্ছি, ব্যবস্থা করুন") — `row_not_matched` মানে ১০০% নিশ্চিত
                // সার্ভারে ওই সারিটাই নেই (অন্য কোথাও ইতিমধ্যে ডিলিট হয়ে গেছে) —
                // তাই এখানে কিছু "হারানোর" নেই, শুধু একটা অসম্ভব চেষ্টা। আগে এটা
                // ২ বার চেষ্টার পর "যায়নি" ঘরে গিয়ে বসে থাকত আর TK-কে কারিগরি
                // শব্দ ("row_not_matched") দেখিয়ে বিভ্রান্ত করত, যতক্ষণ না তিনি
                // নিজে লাল বাক্সে দীর্ঘ-চাপ দিয়ে সেটা সরাতেন। এখন এটা ২ বার
                // চেষ্টার পরে **সরাসরি নিঃশব্দে বাদ** — "যায়নি" ঘরেও যায় না,
                // তাই বিভ্রান্তিকর কোনো বার্তা আর দেখাবে না। ⛔ অন্য কোনো ধরনের
                // ব্যর্থতা (নেটওয়ার্ক/টাইমআউট/৪০০-৪০৪-৪২২ ইত্যাদি) আগের মতোই
                // "যায়নি" ঘরে যায়, TK চাইলে "পাঠান"/দীর্ঘ-চাপ দিয়ে দেখতে/সরাতে
                // পারবেন — শুধু row_not_matched-এর ক্ষেত্রেই এই ছাড়, কারণ এটার
                // কোনোদিনও সফল হওয়ার সম্ভাবনা নেই এটা ১০০% নিশ্চিত।
                // 🔒🔒 TK-APPROVED (2026-08-07): `row_not_matched` মানে সার্ভারে
                // ওই সারিটাই নেই — UPDATE কখনো সফল হবে না (উপরের নোটেও তাই)।
                // আগে এটা `tries >= 2`-এ নিঃশব্দে `continue` করত, কিন্তু
                // `remember()` প্রতিবার এন্ট্রিটা tries=0 করে আবার বসায় (নিচে
                // দ্রষ্টব্য), তাই সংখ্যাটা কখনো ২ ছুঁত না — ফলে এন্ট্রিটা
                // "পাঠানো বাকি" (pending) ঘরে **চিরকাল আটকে** থাকত, লং-প্রেসেও
                // (যেটা শুধু "যায়নি" ঘর সরায়) ধরা পড়ত না। এখন HTTP 400/404/422-এর
                // প্রমাণিত নিয়মেই সরাসরি "যায়নি" (failed/permanent) ঘরে সরানো হয়:
                //   (১) "to sync" গোনা থেকে বেরিয়ে যায়,
                //   (২) লাল বাক্সে দীর্ঘ-চাপ দিয়ে "ছেড়ে দিন" করা যায়,
                //   (৩) permanent + body-hash রাখায় `remember()`-এর same-body
                //       parking ভবিষ্যতে আর pending-এ re-queue করে না (লুপ বন্ধ)।
                // ⛔ কিছু হারায় না — নেই-থাকা সারিতে UPDATE এমনিতেও কিছুই করে না;
                //    দরকারে "পাঠান" (retryFailed) দিয়ে আবার সুযোগও থাকে।
                if (le.contains("row_not_matched")) {
                    // 🔒🔒 B593 (10.08.2026, TK-অনুমোদিত — "সার্ভারে-নেই কাজ চুপচাপ
                    // পাকাপাকি বাদ, লাল বার্তা আর ফিরবে না, আসল তথ্য মুছবে না"): আগে এটা
                    // "যায়নি" ঘরে (permanent) রেখে দিত — তাই লাল সতর্কবার্তায় সংখ্যাটা
                    // থেকেই যেত ও TK-কে বারবার রিপোর্ট করতে হত। row_not_matched মানে
                    // সার্ভারে ঐ সারিটাই নেই → UPDATE কোনোদিন সফল হবে না। তাই "যায়নি"
                    // ঘরেও না রেখে **সরাসরি নিঃশব্দে বাদ** — তালিকা থেকে মুছে গেল, আর
                    // updateById-ও (B593) এটাকে নতুন করে মনে রাখে না, তাই আর ফিরবে না।
                    // ⛔ তথ্য হারায় না (নেই-সারিতে UPDATE এমনিতেও no-op; followup পুরো
                    // সারি আলাদা upsert-heal পথে ক্লাউডে নিরাপদ) — SupabaseClient.updateById-এর
                    // B593 নোট দ্রষ্টব্য।
                    continue
                }
                // 🆕 B421 (05.08.2026, TK-রিপোর্ট — "cascadedFollowups column"
                // দেখিয়ে বারবার আটকে থাকা, ছবিসহ): ঠিক row_not_matched-এর
                // মতোই একই ক্লাস — PGRST204 ("Could not find the '...' column
                // of '...' in the schema cache") মানে ১০০% নিশ্চিত সেই
                // কলামটাই ডেটাবেসে নেই, তাই বারবার পাঠালেও কখনো সফল হবে না।
                // **আসল তথ্য হারানোর ভয় নেই:** এই নির্দিষ্ট এন্ট্রি
                // (`trash` টেবিলের `cascadedFollowups`) — `TrashHelper.
                // moveToTrash()`-এর নিজস্ব ২-ধাপের ব্যবস্থা (আগে থেকেই আছে)
                // এই একই লেখা কলামটা বাদ দিয়ে **আলাদাভাবে দ্বিতীয়বার
                // পাঠিয়ে ইতিমধ্যেই সফল করে ফেলেছে** — Trash-এ রেকর্ডটা
                // ঠিকই আছে। এখানে যেটা আটকে ছিল সেটা শুধু **প্রথম (ব্যর্থ)
                // চেষ্টার** একটা ভূতুড়ে প্রতিলিপি, যেটা প্রতিবারই একই
                // কলাম-নেই কারণে আটকাবে। তাই ২ বার চেষ্টার পরে row_not_
                // matched-এর মতোই **সরাসরি নিঃশব্দে বাদ**। ⛔ অন্য কোনো
                // কলাম ভবিষ্যতে সত্যিই দরকার হলে (নতুন ফিচার) সেটা তখন
                // নতুন SQL দিয়ে যোগ করতে হবে — এটা শুধু পুরনো, ইতিমধ্যে-
                // সফল-হওয়া এন্ট্রির ভূতুড়ে অনুলিপি সাফ করে।
                if (le.contains("PGRST204") && tries >= 2) continue
                // 🔴🔴🔴 খাতার সারি B447 (TK-রিপোর্ট, ছবিসহ — Patient ID
                // ডুপ্লিকেট (HTTP 409 · code=23505 ·
                // patients_officialid_unique_idx) দেখিয়ে চিরকাল আটকে
                // থাকা রেজিস্ট্রেশন)। **আসল কারণ:** এই এন্ট্রিটা বহুদিন
                // অফলাইনে আটকে ছিল (এই ফোনে সেভ হয়েছিল, ক্লাউডে যায়নি) —
                // ততদিনে সেই একই Patient ID (যেমন JPE-14072026-004)
                // অন্য কেউ (অন্য ফোন/সময়ে এই ফোনই) নিয়ে নিয়েছে। একই ID
                // দিয়ে আবার পাঠালে **প্রতিবারই** এই একই সংঘর্ষ হবে —
                // কিন্তু row_not_matched/PGRST204-এর মতো এটা নিঃশব্দে বাদ
                // দেওয়া যাবে না, এটা আসল, না-সিঙ্ক-হওয়া রোগীর রেজিস্ট্রেশন,
                // হারালে চলবে না। **সমাধান:** ২ বার চেষ্টার পরে এখন একটা
                // নতুন, এই মুহূর্তে সত্যিই ফাঁকা Patient ID বসিয়ে (বাকি
                // সব তথ্য — নাম/মোবাইল/ঠিকানা/টাকা — অক্ষত রেখে) আবার
                // চেষ্টা করা হয়, চেষ্টার গোনা ০ থেকে আবার শুরু হয়।
                if (table == "patients" && le.contains("23505") && le.contains("patientId") && tries >= 2) {
                    val fixedBody = try { regeneratePatientIdInBody(context, e.optString("body")) } catch (_: Throwable) { null }
                    if (fixedBody != null) {
                        left[keyOf(e)] = e.put("body", fixedBody).put("tries", 0).put("lastError", "")
                        continue
                    }
                }
                // 🔴🔴🔴🔴 V479 (20.08.2026, TK-রিপোর্ট, ছবিসহ — পুরনো ফোনে
                // "22 items have not reached the cloud yet", payments_pkey
                // duplicate দেখিয়ে চিরকাল আটকে থাকা)। TK-এর প্রশ্ন: "এটা তো
                // আগে ভালোই ছিল, কবে থেকে খারাপ হলো?" — **আজ সকালের সেই
                // একই JWT/reAuth বাগের (V465-এ ঠিক করা) সাথেই যুক্ত**:
                // আসল লেখাটা সার্ভারে **ঠিকই সফল** হয়েছিল, কিন্তু ঠিক তার
                // পরের মুহূর্তে (টোকেন মেয়াদ ফুরিয়ে) queue থেকে "সফল, সরিয়ে
                // ফেলো" নিশ্চিত করার ধাপটাই ব্যর্থ হয়ে গিয়েছিল — তাই এই
                // ফোন চিরকাল ধরে নিয়েছিল কাজটা এখনো বাকি, বারবার একই ডেটা
                // পাঠানোর চেষ্টা করে গেছে, প্রতিবারই "already exists" পেয়েছে।
                // **নিরাপত্তা (নিশ্চিত, আন্দাজ নয়):** `_pkey` (primary key)-এর
                // উপর duplicate মানেই **নিজের এই একই ID-র সারিটা সত্যিই
                // ইতিমধ্যে ওখানে বসে আছে** — অন্য কারো ডেটা মোছা/চাপা পড়া
                // নয়, ঠিক এই এন্ট্রিরই একটা পুরনো, ইতিমধ্যে-সফল অনুলিপি।
                // তাই patients-এর ভিন্ন-ID-দিয়ে-আবার-চেষ্টার (উপরের ব্লক)
                // মতো নয় — এখানে নতুন ID লাগে না, শুধু নিশ্চিত-হওয়া সাফল্য
                // হিসেবে ধরে সরিয়ে ফেলা (row_not_matched/PGRST204-এর একই
                // নিরাপদ প্যাটার্ন)।
                /* 🔴🔒🔒 V903 (৩১.০৮.২০২৬, TK-এর দেখানো লগ থেকে প্রমাণিত) —
                   Supabase-এর লগে ঘণ্টায় **শত শত** একই ভুল:
                   `duplicate key value violates unique constraint "payments_pkey"`।
                   কারণ: নিচের শর্তে **"already exists"** শব্দ দুটোও চাওয়া হতো,
                   কিন্তু সার্ভার সবসময় ওই বিস্তারিত অংশটা পাঠায় না — শুধু
                   উপরের বাক্যটা আসে। ⇒ শর্ত মিলত না, সারিটা তালিকা থেকে
                   **কোনোদিন মুছত না**, আর ফোন চিরকাল একই টাকার সারি আবার
                   পাঠাত (অকারণ Egress ও ভুলের পাহাড়)।
                   **এখন:** `23505` + `<টেবিল>_pkey` — এই দুটোই যথেষ্ট। নিজের ওই
                   একই আইডির সারি ওখানে সত্যিই বসে আছে বলেই তো এই ভুলটা আসে,
                   তাই "কাজ শেষ" ধরে সরিয়ে দেওয়াই ঠিক।
                   ⛔ কোনো টাকা মোছা/বদলানো হয় না — শুধু ফোনের অপেক্ষমাণ
                      তালিকা থেকে ইতিমধ্যে-বসে-যাওয়া সারিটা বাদ যায়। */
                if (le.contains("23505") && le.contains("${table}_pkey")) {
                    continue
                }
                val permanent = permanentHttp
                if (permanent && tries >= 2) {
                    // 🔒 V220 (§2): permanent চিহ্ন + body-hash রাখা হয়, যাতে remember()
                    // পরে হুবহু একই ভুল data আবার না পাঠায় (data বদলালে আবার retry)।
                    giveUp.add(e.put("why", "server rejected (won't retry): $le")
                        .put("permanent", true)
                        .put("permBodyHash", bodyHash(e.optString("body"))))
                    continue
                }
                // never resurrect something that was deleted on purpose
                val deleted = try { DeletedGuard.isDeleted(table, id) } catch (_: Throwable) { false }
                if (deleted && kind != "DELETE") continue
                val body = e.optString("body")
                val done = try {
                    when (kind) {
                        "UPSERT" -> if (body.isBlank()) true else SupabaseClient.upsert(table, JSONObject(body))
                        "UPDATE" -> if (body.isBlank() || id.isBlank()) true
                            else SupabaseClient.updateById(table, id, JSONObject(body))
                        "DELETE" -> if (id.isBlank()) true else SupabaseClient.deleteById(table, id)
                        else -> true
                    }
                } catch (_: Throwable) { false }
                if (!done) left[keyOf(e)] = e.put("tries", tries + 1)
            }
            // Something may have failed and been remembered WHILE this was
            // running (a save the staff made a second ago). Merge those in
            // instead of overwriting them, so nothing can slip through.
            // 🔒🔒 খাতার সারি B164: মেলানো ও লেখা — দুটোই এখন **এক তালার ভিতরে**,
            // নইলে ঠিক এই মুহূর্তে ব্যর্থ হওয়া কোনো কাজ চাপা পড়ে যেতে পারত।
            synchronized(LOCK) {
                val now = try {
                    JSONArray(p.getString(KEY, "[]") ?: "[]")
                } catch (_: Throwable) { JSONArray() }
                // এই দফায় যে সারিগুলো হাতে নেওয়া হয়েছিল, তাদের **সময়** মনে রাখা।
                val handledAt = HashMap<String, Long>()
                // 🔒 খাতার সারি B166: চেষ্টার সংখ্যাটাও মনে রাখা হয় — নিচে দেখুন কেন।
                val handledTries = HashMap<String, Int>()
                for (i in 0 until list.length()) {
                    val o = list.optJSONObject(i) ?: continue
                    handledAt[keyOf(o)] = o.optLong("at", 0L)
                    handledTries[keyOf(o)] = o.optInt("tries", 0)
                }
                for (i in 0 until now.length()) {
                    val e = now.optJSONObject(i) ?: continue
                    val k = keyOf(e)
                    val old = handledAt[k]
                    if (old == null) {
                        // এই দফায় ছিলই না — অর্থাৎ এর মধ্যেই নতুন করে ব্যর্থ
                        // হয়েছে। ⛔ কখনো ফেলে দেওয়া যাবে না।
                        if (!left.containsKey(k)) left[k] = e
                    } else if (e.optLong("at", 0L) > old) {
                        // 🔒🔒 খাতার সারি B164 — এই ফাঁকটাও একই সঙ্গে বন্ধ হলো:
                        // পাঠানো চলার সময় ওই **একই সারি আবার সেভ** হলে (স্টাফ
                        // এক সেকেন্ড আগে রিমার্ক/টাকা বদলেছেন) আগে সেটাকে
                        // "এই দফাতেই ছিল" ধরে **বাদ দিয়ে দেওয়া হত — নতুন লেখাটা
                        // হারিয়ে যেত**। এখন নতুনটাই জেতে।
                        // 🔒 খাতার সারি B166: কিন্তু **চেষ্টার সংখ্যা পিছিয়ে দেওয়া
                        // যাবে না**। কারণ ব্যর্থ হওয়ার মুহূর্তেই কাজটা আবার এই
                        // তালিকায় ওঠে (তখন গোনা ০ থেকে শুরু) — তাই সংখ্যাটা
                        // প্রতিবার শূন্য হয়ে গেলে **৫০ বারের সীমাটা কোনোদিন
                        // ছোঁয়াই যেত না**, আর যে কাজ কোনোদিনই যাবে না সেটা
                        // চিরকাল চেষ্টা করে যেত। এখন দুটোর মধ্যে **বড় সংখ্যাটাই**
                        // রাখা হয়, তাই সীমাটা ঠিকঠাক কাজ করে।
                        val before = (handledTries[k] ?: 0) + 1
                        val fresh = e.optInt("tries", 0)
                        left[k] = e.put("tries", if (before > fresh) before else fresh)
                    }
                }
                val out = JSONArray()
                for (v in left.values) out.put(v)
                val ed = p.edit().putString(KEY, out.toString())
                if (giveUp.isNotEmpty()) {
                    ed.putString(KEY_FAILED, withFailedAdded(p, giveUp, "tried_50_times").toString())
                }
                ed.commit()
                recountDeletes(out)   // 🔒 খাতার সারি B166
                recountQueue(p)       // 🔒 V221 (§2)
            }
        } catch (_: Throwable) {}
    }
}
