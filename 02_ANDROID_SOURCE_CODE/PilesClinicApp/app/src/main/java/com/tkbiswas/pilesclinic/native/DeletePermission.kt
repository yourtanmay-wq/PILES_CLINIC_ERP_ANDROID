package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🔒 TK-এর নিয়ম (29.07.2026 রাত ১০.১০ · খাতার সারি B98):
 *
 *   *"মাস্টার ছাড়া কেউ ডিলিট করবে না। staff-রা করতে চাইলে Master admin-এর কাছে
 *    অনুমতি নিতে হবে। Master-এর ঘন্টাতে যাবে। মাস্টার অনুমতি দিলে ডিলিট হবে,
 *    অন্যথায় হবে না।"*
 *
 * ─────────────────────────────────────────────────────────────
 * **এটা যা করে**
 *  • `canDeleteNow()` — এই মানুষটা **এখনই** ডিলিট করতে পারবেন কি না।
 *    শুধু **মাস্টার** পারবেন। স্টাফ/ডাক্তার/ফিল্ড — কেউ না।
 *  • `sendRequest()` — স্টাফ ডিলিট চাইলে **মাস্টারের ঘন্টায়** একটা অনুরোধ যায়।
 *    ⛔ এই মুহূর্তে **কিছুই মোছে না** — মাস্টার নিজে দেখে তবেই মুছবেন।
 *
 * ─────────────────────────────────────────────────────────────
 * **কেন এভাবে (ডেটাবেসে নতুন ঘর ছাড়া)**
 *  অনুরোধটা যায় আগে থেকেই থাকা **`briefings`** টেবিলে, `role = master` লক্ষ্য
 *  করে — অর্থাৎ ঠিক সেই ব্যবস্থাটাই যা দিয়ে মাস্টারের **ঘন্টা** কাজ করে।
 *  ⛔ **কোনো নতুন টেবিল বা ঘর লাগেনি, তাই TK-কে কোনো SQL চালাতে হবে না।**
 *  ⛔ অনুরোধ পাঠাতে না পারলেও কিছু মোছে না — শুধু "পাঠানো গেল না" বলে।
 *
 * ⛔ **পুরনো `TrashHelper.canDelete()` ছোঁয়া হয়নি** — ডাক্তার মডিউল ও অন্য
 *    জায়গা আগের নিয়মেই চলে; এই ফাইলটা শুধু রোগীর রেকর্ড মোছার পথে ব্যবহার হয়।
 */
object DeletePermission {

    /** শুধু মাস্টারই এখনই মুছতে পারেন। */
    fun canDeleteNow(user: NativeUser?): Boolean = user != null && user.role == "master"

    private fun todayIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    private fun yesterdayIso(): String {
        val c = java.util.Calendar.getInstance()
        c.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(c.time)
    }

    /**
     * 🔒🔒 খাতার সারি B112 (29.07.2026) — **পরবর্তীতে B467-এ (06.08.2026)
     * TK-নির্দেশে পুরোপুরি হালনাগাদ, এটাই এখন ফাইনাল নিয়ম।**
     *
     * TK-এর কথা (06.08.2026, খাতার সারি B467): *"যতক্ষণ অ্যাডভান্স না করে
     * ততক্ষণ সে পেশেন্ট হয় না — যদি কারো এডভান্স বা বিল তৈরি হয়ে যায় এবং
     * কিছু টাকা এডভান্স করে [তবেই অনুমতির নিয়ম প্রযোজ্য]।"* আর টাকা জমা
     * থাকা এন্ট্রির জন্য: *"একই দিন, OUT TIME অথবা চেম্বার বন্ধের আগ পর্যন্ত
     * ফ্রি, তারপর অনুমতি লাগবে।"* (আগের B443-এর "Treatment-stage-এ সবসময়
     * অনুমতি লাগবে, আজ হলেও ছাড় নেই" — এই কড়া নিয়মটা আজ TK নিজেই বদলে
     * দিয়েছেন, এটাই এখন চূড়ান্ত।)
     *
     * ### নিয়ম (এই ক্রমেই যাচাই হয়)
     * ১. **মাস্টার** → সবসময় পারবেন।
     * ২. **টাকা জমা পড়েনি** (`paid = false` — Enquiry বা এখনো-Advance-না-হওয়া
     *    Visit) → স্টাফ **যেকোনো দিন, কোনো অনুমতি ছাড়াই** পারবেন।
     * ৩. টাকা জমা পড়েছে (`paid = true`) কিন্তু এন্ট্রিটা **আজকের নয়** →
     *    পারবেন না (অনুমতি লাগবে)। ⛔ আগে আজ+গতকাল দুদিন চলত, এখন শুধু আজ।
     * ৪. আজকেরই, কিন্তু ওই দিনের **চেম্বার বন্ধ হয়ে গেছে** → পারবেন না।
     * ৫. আজকেরই, চেম্বারও খোলা, কিন্তু স্টাফ নিজে আজকের **OUT TIME সেভ করে
     *    ফেলেছেন** → পারবেন না (নতুন শর্ত, B467)।
     * ৬. উপরের কোনোটাই না হলে → পারবেন।
     *
     * ⛔ ব্রাঞ্চের নিয়ম আগের মতোই — স্টাফ নিজের ব্রাঞ্চের বাইরে এমনিতেই কিছু
     *    করতে পারেন না (খাতার সারি B52-এর টাকার নিয়ম)।
     * ⛔ **কিছু বুঝতে না পারলে "না"** — তারিখ/ব্রাঞ্চ ফাঁকা হলে, বা OUT TIME
     *    সত্যিই আছে কিনা ক্লাউড থেকে জানা না গেলে (নেট ব্যর্থ), অনুমতি
     *    লাগবে ধরে নেওয়া হয় — টাকার ব্যাপারে সন্দেহ হলে আটকানোই নিরাপদ।
     * ⚠️ **এই ফাংশন ক্লাউড ছুঁতে পারে** (চেম্বার বন্ধ কিনা, OUT TIME হয়েছে
     *    কিনা দেখতে) — তাই সবসময় `Dispatchers.IO`-তে ডাকতে হবে।
     *
     * @param paid টাকা (Advance/Bill) জমা পড়া এন্ট্রি কিনা — কল-সাইট নিজে
     *    ঠিক করে পাঠায় (কোনো ডিফল্ট নেই, ভুল করে "ফ্রি" ধরে যাওয়ার ঝুঁকি
     *    এড়াতে); Payment/টাকার সারি ডিলিটে সবসময় `true`, খাঁটি Enquiry
     *    ডিলিটে সবসময় `false`, রোগীর রেকর্ডে `isTreatmentStage` (Advance
     *    হয়েছে কিনা)।
     */
    fun canDeleteEntryNow(
        context: Context?,
        user: NativeUser?,
        entryDate: String,
        branch: String,
        paid: Boolean
    ): Boolean {
        if (user == null) return false
        if (user.role == "master") return true
        // 🆕 B467 (06.08.2026): টাকা জমাই না পড়ে থাকলে (Enquiry/Visit,
        // এখনো Advance হয়নি) — যেকোনো দিন, কোনো অনুমতি ছাড়াই।
        if (!paid) return true
        val d = entryDate.take(10)
        if (d.isBlank() || branch.isBlank()) return false
        // 🆕 B467 (06.08.2026): আগে আজ+গতকাল দুদিন চলত, এখন শুধু আজকের এন্ট্রি।
        if (d != todayIso()) return false
        // চেম্বার বন্ধ হয়ে গেলে মাস্টার হিসাব পেয়ে গেছেন — স্টাফ আর মুছতে পারবেন না।
        if (try { ChamberCloseRepository.isClosed(context, branch, d) } catch (_: Throwable) { false }) return false
        // 🆕 B467 (06.08.2026, TK-নির্দেশ): নিজের আজকের OUT TIME সেভ হয়ে
        // গেলে আর ফ্রি না — এটা বা চেম্বার বন্ধ, যেটা আগে হয়। ক্লাউড থেকে
        // সত্যিই জানা না গেলে (নেট ব্যর্থ) নিরাপদ দিক ধরে "হ্যাঁ, OUT TIME
        // হয়ে গেছে" ধরে নেওয়া হয় (অনুমতি লাগবে) — সন্দেহে আটকানোই নিরাপদ।
        if (hasOutTimeToday(context, user)) return false
        return true
    }

    /**
     * B467-এর OUT TIME-চেক — `wn.notebook_days`-এ আজকের সারিতে `check_out`
     * ভরা আছে কিনা দেখে।
     *
     * 🔴🔴🔒 V1148 (০৬.০৯.২০২৬, TK-রিপোর্ট — BASANTI ROY, খাতার সারি ২৪৩):
     * TK: *"আজকে পেমেন্ট নিয়েছে, ভুল করে কম-বেশি করে ফেলেছে, ডিলিট করতে
     * চাইছে; তাহলে কেন মাস্টারের অনুমতি লাগবে?"* — আর পরে: *"স্টাফ তখন
     * আমার সামনেই বসে ছিল"* (অর্থাৎ OUT TIME হয়ইনি, চেম্বারও খোলা)।
     *
     * 🔴 **ফাঁকটা এখানেই ছিল, আর সেটা আমারই:** এই পড়াটা **মডিউল-লগইনের**
     *    টোকেন দিয়ে হয়, অথচ পড়ার আগে `signInCurrentSession()` **কখনো ডাকা
     *    হত না** (প্রজেক্টের অন্য জায়গায় ডাকা হয় — যেমন `SalaryReminder`)।
     *    স্টাফ ওই সেশনে Work Notebook/Profile না খুলে থাকলে টোকেনই থাকত না
     *    ⇒ পড়া ব্যর্থ ⇒ নিচের "নিরাপদ দিক" নিয়মে **OUT TIME হয়ে গেছে** ধরে
     *    নেওয়া হত ⇒ *Master's approval needed*। ঠিক TK যা দেখেছেন।
     *
     * ⇒ এখন দুটো বদল:
     *   ① পড়ার আগে দরকার হলে মডিউল-লগইনটা করানো হয় (একবারই, প্রমাণিত পথে)
     *   ② **জানা না গেলে আর আটকায় না** (TK-এর সিদ্ধান্ত: *"জানা না গেলেও
     *      মুছতে দিন, চেম্বার খোলা থাকলে"*)। চেম্বার-বন্ধের পাহারাটা এর
     *      **আগেই** চলে (`canDeleteEntryNow`), তাই চেম্বার বন্ধ হয়ে গেলে
     *      স্টাফ এমনিতেই মুছতে পারেন না — সেই সুরক্ষা অটুট।
     * ⛔ সত্যিই OUT TIME সেভ থাকলে আগের মতোই আটকাবে — নিয়মটা বদলায়নি।
     */
    private fun hasOutTimeToday(context: Context?, user: NativeUser): Boolean {
        if (context == null) return false   // 🔴 V1148 — জানা না গেলে আটকানো নয়
        return try {
            // 🔴 V1148 ① — টোকেন না থাকলে আগে মডিউল-লগইন, নইলে পড়াটাই ব্যর্থ হয়।
            try {
                if (!com.tkbiswas.pilesclinic.modules.ModuleAuth.isSignedIn) {
                    com.tkbiswas.pilesclinic.modules.ModuleAuth.signInCurrentSession(context)
                }
            } catch (_: Throwable) { }
            val staffCode = user.name.ifBlank { user.mobile }
            val result = com.tkbiswas.pilesclinic.modules.ModuleAuth.getRowsChecked(
                "wn", "notebook_days",
                "select=check_out&staff_code=eq.${java.net.URLEncoder.encode(staffCode, "UTF-8")}&work_date=eq.${todayIso()}&limit=1"
            )
            if (!result.ok) return false // 🔴 V1148 ② — জানা গেল না ⇒ আটকাব না
            if (result.rows.length() == 0) return false // আজ এখনো কোনো সারিই নেই — OUT TIME হয়নি
            result.rows.getJSONObject(0).optString("check_out").isNotBlank()
        } catch (_: Throwable) { false }
    }

    /**
     * মাস্টারের ঘন্টায় অনুরোধ পাঠায়। কিছুই মোছে না।
     * @param what কী মোছার কথা — যেমন "Enquiry" / "Patient" / "Visit"
     */
    /* ═══════════════════════════════════════════════════════════════════
       🔁🔒 V1176 (০৭.০৯.২০২৬, TK-নির্দেশ) — **একই অনুরোধ দুবার যাবে না।**

       TK, ছবিসহ: *"একই রিকোয়েস্ট যখন আমার কাছে আগে চলে এসেছে, তাহলে স্টাফকে
       কেন দেখাবে না যে অটোমেটিক চলে গেছে, আপনাকে আর পাঠাইতে হবে না — এরকম তো
       একটা Pop up আসার কথা"* (KANAK LAL MONDAL-এর একই Delete Payment অনুরোধ
       ১২.১৪ PM ও ২.৫৩ PM — দুবার)।

       **কারণ (কোডে মেপে দেখা):** `sendRequest()` কোনো যাচাই ছাড়াই প্রতিবার
       নতুন নোটিশ পাঠাত — আগে একটা অপেক্ষায় আছে কিনা কেউ দেখত না, আর স্টাফও
       কোনো ইঙ্গিত পেতেন না।

       **এখন:** অনুরোধ পাঠানোর সঙ্গে সঙ্গে ওই সারির চাবিটা **ফোনেই** মনে রাখা
       হয়। **একই দিনে** আবার পাঠাতে গেলে নতুন নোটিশ যায় না — বদলে স্টাফ দেখেন
       *"এই অনুরোধ আগেই পাঠানো হয়েছে (২.৫৩ PM) — আবার পাঠাতে হবে না"*।

       ⛔ **কোনো ক্লাউড-পড়া নেই** (ফ্রি প্ল্যানে বাড়তি খরচ নেই), কোনো নতুন ঘর
          বা SQL নেই — শুধু ফোনের নিজের জমানো তালিকা।
       ⛔ **পরদিন আবার পাঠানো যায়** — মাস্টার সিদ্ধান্ত না নিলে স্টাফ যেন
          চিরতরে আটকে না যান।
       ⚠️ **সৎ সীমা (TK-কে আগেই জানানো):** এটা ওই স্টাফের **নিজের ফোনে** কাজ
          করে। অন্য স্টাফ একই অনুরোধ পাঠালে ধরা পড়বে না — তার জন্য মাস্টারের
          নোটিশ ক্লাউড থেকে পড়তে হত, আর স্টাফের ওই পড়ার অনুমতি আছে কিনা
          নিশ্চিত নয়।
       ═══════════════════════════════════════════════════════════════════ */
    private const val SENT_PREF = "piles_delete_request_sent"

    /** কোন জিনিসের অনুরোধ — সারির নিজের আইডি থাকলে সেটাই, নইলে নম্বর। */
    private fun sentKey(what: String, rowId: String, mobile: String): String =
        what.trim().lowercase() + "|" + rowId.trim().ifBlank { mobile.filter { it.isDigit() }.takeLast(10) }

    private fun sentPrefs(context: Context?) =
        context?.applicationContext?.getSharedPreferences(SENT_PREF, Context.MODE_PRIVATE)

    /** "2.53 PM" ধাঁচে (TK-র লক করা ঘড়ির চেহারা)। */
    private fun clockOf(millis: Long): String = try {
        java.text.SimpleDateFormat("h.mm a", java.util.Locale.US).format(java.util.Date(millis))
    } catch (_: Throwable) { "" }

    /**
     * আজ এই অনুরোধ আগেই পাঠানো হয়েছে কি? হ্যাঁ হলে **কখন** (যেমন `2.53 PM`),
     * নইলে `null`। ⛔ কখনো নেটে যায় না, কখনো ব্যতিক্রম ছোড়ে না।
     */
    fun alreadySentToday(context: Context?, what: String, rowId: String, mobile: String): String? {
        return try {
            val p = sentPrefs(context) ?: return null
            val v = p.getString(sentKey(what, rowId, mobile), "") ?: ""
            if (v.isBlank()) return null
            val parts = v.split("|")
            if (parts.size != 2 || parts[0] != todayIso()) return null
            clockOf(parts[1].toLongOrNull() ?: return null).ifBlank { null }
        } catch (_: Throwable) { null }
    }

    /* 🔁🔒 V1271 (০৯.০৯.২০২৬, TK-রিপোর্ট ছবিসহ: ঘণ্টায় **হুবহু একই** Delete
       Payment অনুরোধ **দুটো কার্ডে**, দুটোই ৩.১০ PM, একই স্টাফ)।

       🔬 **আসল কারণ (কোডে মেপে):** V1176-এর "আজ একবারই" পাহারাটা ঠিকই আছে,
          কিন্তু চিহ্নটা বসত **নোটিশ পাঠানোর পরে**। মাঝের সময়টা একটা নেট-কল
          (দুর্বল লাইনে কয়েক সেকেন্ড) — ওই ফাঁকে দ্বিতীয়বার চাপলে পাহারা
          এখনো কিছুই দেখতে পেত না ⇒ দুটো নোটিশ চলে যেত।
          আর প্রতিটা নোটিশের আইডি **এলোমেলো** ছিল, তাই দুটো আলাদা কার্ড হত।

       ⇒ **দুটো পাহারা বসল:**
         ① চিহ্নটা এখন **পাঠানোর আগেই** বসে (ব্যর্থ হলে তুলে নেওয়া হয়, তাই
            স্টাফ আবার চেষ্টা করতে পারেন) — একই ফোনে দুবার চাপলেও একটাই যাবে।
         ② নোটিশের আইডি এখন **নির্দিষ্ট** (কী + সারির আইডি + আজকের তারিখ) —
            তাই **দুটো আলাদা ফোন** থেকেও একই অনুরোধ গেলে নতুন কার্ড না হয়ে
            আগেরটাই আবার লেখা হয়, ঘণ্টায় একটাই কার্ড থাকে।
       ⛔ কোনো ক্লাউড-পড়া যোগ হয়নি (ফ্রি প্ল্যান নিরাপদ)।
       ⛔ অন্য কোনো নোটিশের আইডি বদলায়নি — `forcedId` ঘরটা ঐচ্ছিক, শুধু
          ডিলিটের অনুরোধই সেটা পাঠায়। */
    private fun requestBriefId(what: String, rowId: String, mobile: String): String {
        val key = rowId.trim().ifBlank { mobile.filter { it.isDigit() }.takeLast(10) }
        val safe = (what.trim().lowercase() + "_" + key)
            .map { if (it.isLetterOrDigit() || it == '_') it else '_' }.joinToString("")
        return "brief_del_" + safe + "_" + todayIso()
    }

    private fun clearSentToday(context: Context?, what: String, rowId: String, mobile: String) {
        try { sentPrefs(context)?.edit()?.remove(sentKey(what, rowId, mobile))?.apply() } catch (_: Throwable) { }
    }

    private fun markSentToday(context: Context?, what: String, rowId: String, mobile: String) {
        try {
            sentPrefs(context)?.edit()
                ?.putString(sentKey(what, rowId, mobile), todayIso() + "|" + System.currentTimeMillis())
                ?.apply()
        } catch (_: Throwable) { }
    }

    /**
     * শেষ চেষ্টার ফল — স্টাফকে যা দেখানো হবে। `sendRequest()` প্রতিবার এটা
     * বসিয়ে দেয়, তাই ডাকার জায়গাগুলো শুধু এটাই দেখালেই সঠিক কথা যায়।
     * ⛔ শুধু **দেখানোর লেখা** — কোনো সিদ্ধান্ত এর উপর নির্ভর করে না।
     */
    @Volatile private var lastMsg: String = ""
    fun lastMessage(): String = lastMsg.ifBlank { "Request sent to Master" }

    /* 📄🔒 V1177 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — TK, ছবিসহ:
       *"এগুলি প্রফেশনাল লুক আনতে হবে · এত বেশি বিস্তারিত রাখা যাবে না ·
       যেটুকু প্রয়োজন সেটুকুই রাখুন"*।

       "Approve & Delete?" পপ-আপে **পুরো কাঁচা বার্তাটা** বসত — Row ID ·
       Patient ID · Mobile-এর মতো যন্ত্রের লেখা সহ, ৯ লাইন। মাস্টারের
       সিদ্ধান্ত নিতে ওগুলোর দরকার নেই।

       এখন তিন লাইন: **নাম** · **তারিখ ও টাকা** · **ব্রাঞ্চ · কে চাইল**।
       ⛔ সেভ করা `item.message` এক অক্ষরও বদলায় না — শুধু **দেখানোর** লেখা;
          তাই Approve-এর পথ (`approveAndDelete` ওই বার্তা থেকেই Row ID পড়ে)
          হুবহু আগের মতোই কাজ করে।
       ⛔ কোনো ঘর চেনা না গেলে সেই লাইনটা শুধু বসে না — কিছু ভাঙে না। */
    fun shortSummary(message: String): String {
        return try {
            fun f(key: String): String = Regex(
                "(?im)^[^A-Za-z]*" + Regex.escape(key) + "\\s*:\\s*(.+)$"
            ).find(message)?.groupValues?.get(1)?.trim().orEmpty()
            val type = f("Type").ifBlank { "Record" }
            val what = f(type).ifBlank { f("Reason") }          // "Payment : 07.09.2026 · ₹1,000 …"
            val by = f("Requested by")
            val branch = f("Branch")
            val lines = ArrayList<String>()
            f("Name").takeIf { it.isNotBlank() }?.let { lines.add(it) }
            if (what.isNotBlank()) lines.add(what)
            val third = listOf(branch, if (by.isNotBlank()) "By $by" else "")
                .filter { it.isNotBlank() }.joinToString(" \u00b7 ")
            if (third.isNotBlank()) lines.add(third)
            if (lines.isEmpty()) message else lines.joinToString("\n")
        } catch (_: Throwable) { message }
    }

    fun sendRequest(
        context: Context,
        user: NativeUser,
        what: String,
        name: String,
        mobile: String,
        patientId: String,
        branch: String,
        reason: String = "",
        // 🔒 খাতার সারি B111: টাকার সারির ক্ষেত্রে **ঠিক কোন সারিটা** মুছতে হবে
        // সেটা নম্বর দিয়ে বোঝা যায় না (একজনের অনেক পেমেন্ট থাকে), তাই সারির
        // নিজের আইডিটাও অনুরোধে পাঠানো হয়। ⛔ অন্য সব ক্ষেত্রে এটা ফাঁকা থাকে,
        // তাই আগের কোনো অনুরোধের চেহারা বদলায় না।
        rowId: String = "",
        // 🟢🔒 V641 (২৪.০৮.২০২৬, TK-রিপোর্ট — "আমি কেন বুঝব না এটা কিসের
        // পেশেন্ট ছিল, Patient ID দেখে কি বুঝব?") — এখন রোগ (Disease)-ও
        // অনুরোধে যায়, যাতে Master এক নজরেই বুঝতে পারেন কোন রোগী। ডিফল্ট
        // ফাঁকা, তাই disease না পাঠানো পুরনো caller-দের কোনো ক্ষতি হয় না —
        // ফাঁকা হলে সেই লাইনটা শুধু বসেই না।
        disease: String = "",
        /* 🟢🔒 V1134 (০৬.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK: *"কত তারিখের
           পেমেন্ট ডিলিট করতে চাইছে সেটা তো দেখাতে হবে"*। সারিটার নিজের তারিখ
           (yyyy-MM-dd)। ফাঁকা হলে লাইনটা আগের মতোই "Reason :" থাকে, তাই পুরনো
           caller-দের কিছু বদলায় না। */
        entryDate: String = ""
    ): Boolean {
        /* 🔁 V1176 — আজ এই অনুরোধ আগেই গেছে? তাহলে নতুন নোটিশ যাবে না। */
        val already = alreadySentToday(context, what, rowId, mobile)
        if (already != null) {
            lastMsg = "Already sent at " + already + " — no need to send again"
            return false
        }
        return try {
            val who = StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
            val sb = StringBuilder()
            sb.append("Delete permission request\n")
            sb.append("Type : ").append(what).append("\n")
            sb.append("Name : ").append(name.ifBlank { mobile }).append("\n")
            if (disease.isNotBlank()) sb.append("Disease : ").append(disease).append("\n")
            sb.append("Mobile : ").append(mobile).append("\n")
            if (patientId.isNotBlank()) sb.append("Patient ID : ").append(patientId).append("\n")
            if (branch.isNotBlank()) sb.append("Branch : ").append(branch).append("\n")
            if (rowId.isNotBlank()) sb.append("Row ID : ").append(rowId).append("\n")
            sb.append("Requested by : ").append(who).append("\n")
            /* 🟢 V1134 — টাকার সারির ক্ষেত্রে **তারিখ ও অঙ্ক এক লাইনে**।
               ⛔ তারিখ না পাঠালে আগের "Reason :" লাইনই হুবহু থাকে। */
            if (reason.isNotBlank()) {
                if (entryDate.isNotBlank())
                    sb.append(what.ifBlank { "Entry" }).append(" : ")
                        .append(DateUtil.display(entryDate)).append(" \u00b7 ").append(reason).append("\n")
                else sb.append("Reason : ").append(reason).append("\n")
            }
            /* 🔁 V1271 ① — চিহ্নটা **পাঠানোর আগেই**; নিচে ব্যর্থ হলে তুলে নেওয়া হয়। */
            markSentToday(context, what, rowId, mobile)
            BriefingRepository().post(
                context,
                /* 🟢🔒 V1134 (TK-রিপোর্ট): *"Delete request — BASANTI ROY —
                   এখানে মনে হচ্ছে পেশেন্টটাকে ডিলিট করবে, কিন্তু এখানে তো পেমেন্ট
                   ডিলিট করার কথা"*। ⇒ শিরোনামেই এখন **কী মোছা হবে** লেখা থাকে
                   ("Payment delete request — …" · "Patient delete request — …")।
                   ⛔ "delete request" শব্দ দুটো অটুট — Briefing-এর চেনার নিয়ম
                      (`title.contains("Delete request", ignoreCase = true)`) ও
                      একসাথে-অনুমোদনের ছাঁকনি আগের মতোই কাজ করে। */
                "🗑️ " + what.ifBlank { "Record" } + " delete request — " + name.ifBlank { mobile },
                sb.toString(),
                "role",
                branch,
                "master",
                user.mobile,
                /* 🔁 V1271 ② — নির্দিষ্ট আইডি, তাই দ্বিতীয় অনুরোধে নতুন কার্ড হয় না */
                forcedId = requestBriefId(what, rowId, mobile)
            ).also { ok ->
                /* 🔁 V1176/V1271 — ব্যর্থ হলে চিহ্নটা তুলে নেওয়া হয়, নইলে নেট
                   ফিরলে স্টাফ আর পাঠাতেই পারতেন না। */
                lastMsg = if (ok) "Request sent to Master" else "Failed — check the network"
                if (!ok) clearSentToday(context, what, rowId, mobile)
            }
        } catch (_: Throwable) {
            clearSentToday(context, what, rowId, mobile)   // 🔁 V1271
            lastMsg = "Failed — check the network"
            false
        }
    }

    /**
     * 🔒 খাতার সারি B100 (TK, 29.07.2026 রাত ১১.১০): *"হ্যাঁ করে দিন"* —
     * মাস্টার ঘন্টার নোটিশ থেকেই **এক চাপে** অনুরোধটা অনুমোদন করে ডিলিট করবেন।
     *
     * অনুরোধের লেখাটা এই ফাইলেরই `sendRequest()` বানায়, তাই ধাঁচটা নিশ্চিত —
     * সেখান থেকেই **Type** ও **Mobile** পড়ে নেওয়া হয়।
     *
     * ⛔ ডিলিটটা হয় অ্যাপের **সেই পুরনো পথেই** (`TrashHelper`) — Trash-এ যায়,
     *    ফেরানো যায়, টাকার ইতিহাস অক্ষত থাকে। নতুন কোনো নিয়ম বানানো হয়নি।
     * ⛔ সারি খুঁজে না পেলে বা লেখা পড়া না গেলে **কিছুই মোছে না**, সাফ বার্তা যায়।
     */
    /**
     * 🔴🔒 V1350 (১১.০৯.২০২৬, TK-রিপোর্ট — "অ্যাডভান্স ডিলিট করতে চাইলাম,
     * মাস্টার রিকুয়েস্ট গেল না কেন", তারপর "হ্যাঁ ঠিক করে দিন") — একক
     * পুরনো পেমেন্টে `sendRequest()` দিয়ে মাস্টারের ঘণ্টায় অনুরোধ যায়;
     * "মিশ্র" (dailyEvents, একই দিনে একাধিক এন্ট্রি জোড়া) পুরনো পেমেন্টে
     * আগে এটাই বাদ ছিল — শুধু "Master-এর অনুমতি লাগবে" Toast দেখিয়ে
     * থেমে যেত, কোনো নোটিশই যেত না।
     *
     * ⛔ এখানে ইচ্ছে করেই "Approve & Delete" এক-চাপ বোতাম নেই — `sendRequest()`
     *    থেকে আলাদা টাইপ ("CombinedPayment") ও শিরোনামে "Delete request"
     *    শব্দ দুটো নেই, তাই `BriefingAdapter`-এর অটো-অনুমোদন বোতাম দেখা
     *    যায় না। কারণ: এই সারিতে একাধিক আলাদা এন্ট্রি (dailyEvents) থাকে,
     *    ঠিক কোনটা মুছতে হবে এক-চাপে নিশ্চিতভাবে বোঝা যায় না — ভুল করে
     *    এক-চাপে পুরো দিনের সব টাকা মুছে যাওয়ার ঝুঁকি এড়াতে শুধু জানানো;
     *    মাস্টার নিজে Payment স্ক্রিনে খুলে (Master হিসেবে বিভাজন সবসময়
     *    খোলে, `canOpenBreakdown`) দেখেশুনে ঠিক এন্ট্রিটা বেছে নেবেন।
     * ⛔ দিনে একবারই — `sendRequest()`-এর হুবহু একই dedup-প্যাটার্ন (V1176/V1271)।
     */
    fun sendCombinedPaymentReviewRequest(
        context: Context,
        user: NativeUser,
        row: org.json.JSONObject,
        reason: String = ""
    ): Boolean {
        val rowId = row.optString("id")
        val mobile = row.optString("mobile")
        val already = alreadySentToday(context, "CombinedPayment", rowId, mobile)
        if (already != null) {
            lastMsg = "Already sent at " + already + " — no need to send again"
            return false
        }
        return try {
            val who = StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
            val eventCount = row.optJSONArray("dailyEvents")?.length() ?: 0
            val name = row.optString("name").ifBlank { mobile }
            val sb = StringBuilder()
            sb.append("Combined payment review request\n")
            sb.append("Name : ").append(name).append("\n")
            sb.append("Mobile : ").append(mobile).append("\n")
            if (row.optString("patientCode").isNotBlank()) sb.append("Patient ID : ").append(row.optString("patientCode")).append("\n")
            if (row.optString("branch").isNotBlank()) sb.append("Branch : ").append(row.optString("branch")).append("\n")
            sb.append("Payment date : ").append(DateUtil.display(row.optString("date"))).append(" · ").append(eventCount).append(" entries combined\n")
            sb.append("Requested by : ").append(who).append("\n")
            if (reason.isNotBlank()) sb.append("Reason : ").append(reason).append("\n")
            sb.append("⚠ Open Payment screen as Master to review and edit/delete the specific entry.")
            markSentToday(context, "CombinedPayment", rowId, mobile)
            BriefingRepository().post(
                context,
                "🗑️ Combined payment review requested — " + name,
                sb.toString(),
                "role",
                row.optString("branch"),
                "master",
                user.mobile,
                forcedId = requestBriefId("CombinedPayment", rowId, mobile)
            ).also { ok ->
                lastMsg = if (ok) "Request sent to Master" else "Failed — check the network"
                if (!ok) clearSentToday(context, "CombinedPayment", rowId, mobile)
            }
        } catch (_: Throwable) {
            clearSentToday(context, "CombinedPayment", rowId, mobile)
            lastMsg = "Failed — check the network"
            false
        }
    }

    fun approveAndDelete(message: String, masterMobile: String): String {
        return try {
            fun field(key: String): String {
                for (line in message.split("\n")) {
                    val t = line.trim()
                    if (t.startsWith("$key :")) return t.substringAfter("$key :").trim()
                }
                return ""
            }
            val type = field("Type")
            val mobile = field("Mobile").filter { it.isDigit() }.takeLast(10)
            if (mobile.length != 10) return "BAD_REQUEST"

            // 🔒 খাতার সারি B111 (TK, 29.07.2026 বিকেল ৫.৪০): *"staff ডিলিট করতে
            // পারবে না, মাস্টারের ঘন্টায় notification আসবে, মাস্টার অনুমতি দিলে
            // তবেই ডিলিট হবে।"* — টাকার সারির অনুরোধও এখন এখান দিয়েই যায়।
            // ⛔ টাকার সারি **আইডি ধরে** মোছা হয় (নম্বর ধরে নয়) — নইলে ভুল
            //    পেমেন্ট মুছে যেতে পারত। আইডি না থাকলে **কিছুই মোছে না**।
            if (type.equals("Payment", true)) {
                val rowId = field("Row ID")
                if (rowId.isBlank()) return "BAD_REQUEST"
                val payRows = SupabaseClient.fetchList("payments", "id=eq.$rowId", 1)
                if (payRows.length() == 0) return "NOT_FOUND"
                return if (TrashHelper.moveToTrash("payments", payRows.getJSONObject(0), masterMobile)) "OK" else "NETWORK"
            }
            // এনকোয়ারি হলে `enquiries`, রেজিস্ট্রেশন/রোগী হলে `patients`।
            val table = if (type.equals("Enquiry", true)) "enquiries" else "patients"
            val rows = SupabaseClient.findByMobile(table, mobile, "*", 1)
            if (rows.length() == 0) return "NOT_FOUND"
            val ok = TrashHelper.moveToTrashWithFollowupCascade(
                table, rows.getJSONObject(0), masterMobile, mobile
            )
            if (ok) "OK" else "NETWORK"
        } catch (_: Throwable) { "NETWORK" }
    }
}
