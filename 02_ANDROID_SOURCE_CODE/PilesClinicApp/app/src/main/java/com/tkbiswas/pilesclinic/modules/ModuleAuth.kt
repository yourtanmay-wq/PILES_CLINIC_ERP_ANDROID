/* =====================================================================
   V245 — ModuleAuth : shared, separate authenticated gateway for the three
   new modules (Profile & Salary · Work Notebook · Income & Expense).

   It gives per-person privacy WITHOUT touching the existing app login and
   WITHOUT any new Gradle dependency — it uses the SAME OkHttp the app ships
   with, calling Supabase Auth (token) + PostgREST (schema hr/wn/fin via the
   Accept-Profile / Content-Profile header). The JWT makes Supabase enforce
   the RLS from V245_MODULES_HR_WN_FIN_2026-08-02.sql: Master sees all, each
   staff sees only their own — exactly like the website.

   No personal email: the login email is synthetic, built from the Staff Code
   (e.g. kne-laxmi@staff.piles). Master sets the passwords.
   All comments/UI are English (Guard rule 9.14). Data access here never uses
   fetchListSlim, so Guard rule 9.7 is unaffected.
   ===================================================================== */
package com.tkbiswas.pilesclinic.modules

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.tkbiswas.pilesclinic.data.remote.SupabaseConfig
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.StaffDirectory

object ModuleAuth {

    private const val EMAIL_DOMAIN = "staff.piles"
    /* 🔴🔴🔒 V803 (২৮.০৮.২০২৬) — **"Staff Profile তো খুলছেই না"** (TK-রিপোর্ট, ফটো সহ:
       সাদা ফাঁকা পর্দা, নিচে "Opening..." লেখা আটকে আছে)।
       ─── আসল কারণ (কোড ধরে প্রমাণিত) ────────────────────────────────────────
       এখানে লেখা ছিল খালি `OkHttpClient()` — **একটাও timeout বসানো ছিল না**।
       OkHttp-র নিজের ডিফল্টে `callTimeout = 0`, মানে **কোনো সময়সীমা নেই**।
       নেট আধমরা হয়ে উত্তরটা ফোঁটা-ফোঁটা করে এলে `readTimeout` প্রতিবার নতুন
       করে শুরু হয় ⇒ ডাকটা **কোনোদিনই শেষ হয় না**। আর Staff Profile পর্দা
       ঠিক এই ডাকটার উত্তরের জন্যই অপেক্ষা করে (`ModuleUi.ensureSignedIn`),
       তাই পর্দা সাদা থেকে যায়, কোনো ভুলের বার্তাও আসে না।
       ⛔ এটা নতুন কোনো আবিষ্কার নয় — প্রজেক্টেই আগে ধরা পড়েছিল ও সারানো
          হয়েছিল: `SupabaseClient.kt:19-29`-এ হুবহু এই কারণটা লেখা আছে
          ("...loading spinner just span forever. callTimeout caps the TOTAL time")।
          শুধু **এই ফাইলটায় সেটা বসানো হয়নি** — খাতার নিয়ম ৬.২ অনুযায়ী এখন বসল।
       ─── সারানো ─────────────────────────────────────────────────────────────
       মূল অ্যাপের প্রমাণিত মাপগুলোই: connect ৮s · read ৮s · **callTimeout ২৫s**।
       ⇒ সবচেয়ে খারাপ অবস্থাতেও ২৫ সেকেন্ডে ডাক শেষ হয়ে "Could not open"
       বার্তা আসে — পর্দা আর চিরকাল সাদা হয়ে বসে থাকে না। */
    /* 🔴🔒 V808 (২৮.০৮.২০২৬) — TK: "staff Profile খুলছে না তো, কি কাজ করেছেন আপনি?"
       V803-এ সময়সীমা বসিয়ে সাদা-পর্দা থামিয়েছিলাম — কিন্তু ওটা ছিল **উপসর্গ**
       সারানো, রোগ নয়। পর্দা এখন "Could not open — timeout" দেখায়, খোলে না।
       ─── "timeout" লেখাটা কেন অকেজো ──────────────────────────────────────
       OkHttp ডিফল্টে **নিজে থেকেই বারবার অন্য রাস্তায় চেষ্টা করে**
       (`retryOnConnectionFailure`)। তাই আসল ভুলটা (যেমন "connect হলো না" বা
       HTTP ৪২৯) চাপা পড়ে যেত, আর শেষে শুধু `callTimeout`-এর নিরর্থক
       "timeout" বেরিয়ে আসত — যা দিয়ে কারণ বোঝার উপায় নেই।
       ─── এখন ────────────────────────────────────────────────────────────
       বারবার চেষ্টা বন্ধ ⇒ আসল ভুলটা **তাড়াতাড়ি ও নিজের নামেই** আসে।
       সময়সীমা একটু বাড়ানো হলো (দুর্বল নেটে যেন অকারণে না কাটে)। */
    /* 🔴🔴🔴🔒 V809 (২৮.০৮.২০২৬) — **আমার নিজের করা ক্ষতি ফিরিয়ে নেওয়া হলো।**
       TK: *"এক জলপাইগুড়ির স্টাফ পাঠিয়েছে — timeout, কাজ হচ্ছে না। কেন এরকম
       ফাজলামো করলেন আপনি আমার সাথে?"*
       ─── git-এর প্রমাণ ─────────────────────────────────────────────────────
       V793 (যেটা এতদিন সবার ফোনে চলছিল) — `private val http = OkHttpClient()`
         ⇒ OkHttp-র ডিফল্ট: connect ১০s · read ১০s · **মোট সময়ের কোনো সীমা নেই**
       আমি V803-এ বসিয়েছিলাম — connect ৮s · read ৮s · **মোট ২৫s**
       V808-এ আরও কড়া করেছিলাম — `retryOnConnectionFailure(false)`
       ⇒ **তিনটে দিকেই আমি আগের চেয়ে কড়া করে দিয়েছিলাম।**
       দুর্বল নেটে (স্টাফের ফোনে ৫ KB/s) যে ডাকটা আগে ধীরে হলেও **শেষ হত**,
       সেটা এখন ২৫ সেকেন্ডে **জোর করে কেটে** যেত ⇒ "timeout"।
       অর্থাৎ আগে পর্দাটা **ধীর** ছিল, আমি সেটাকে **ভাঙা** বানিয়ে ফেলেছি।
       ─── এখন যা করা হলো ───────────────────────────────────────────────────
       প্রতিটা মাপ V793-এর চেয়ে **উদার** — তাই আগে যা চলত, সবই চলবে:
         connect ১০s → **২০s** · read ১০s → **৪০s** · বারবার চেষ্টা **আবার চালু**
       শুধু একটা **শেষ ভরসার** সীমা (১২০s) রাখা হলো, যাতে V803-এর আসল সমস্যাটা
       (পর্দা চিরকাল সাদা হয়ে বসে থাকা) ফিরে না আসে। ১২০ সেকেন্ড এত বড় যে
       সত্যিকারের কোনো কাজ এতে কাটা পড়বে না। */
    private val http = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(40, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json".toMediaType()

    @Volatile var accessToken: String? = null; private set
    @Volatile var personCode: String? = null; private set
    @Volatile var isMaster: Boolean = false; private set
    // 🔴🔒 V1512 (RLS-প্রস্তুতি, ১৭.০৯.২০২৬, যাচাই করে ধরা পড়া দোষ) —
    // `isSignedIn` শুধু accessToken ফাঁকা কিনা দেখে, মেয়াদ শেষ হয়েছে কিনা
    // নয়। এতদিন এটা ক্ষতি করেনি কারণ Staff Profile/বেতনের পর্দা মাঝেমধ্যে
    // খোলা হয়, ১ ঘণ্টার বেশি একটানা চললেই তবে সমস্যা হত (কম দেখা যেত)।
    // patients/payments-এর মতো সবসময় ব্যবহৃত পথে এই একই টোকেন বসালে,
    // মেয়াদ ফুরোনোর পর (~১ ঘণ্টা) থেকে **পুরো শিফট জুড়ে** সব কল ব্যর্থ
    // হত। তাই এখন মেয়াদও আলাদাভাবে RAM-এ রাখা হচ্ছে (নিচের tokenFresh())।
    @Volatile private var accessTokenExpiresAt: Long = 0L
    // 🔵 সর্বশেষ সফল সাইন-ইনের applicationContext — টোকেন মেয়াদ শেষ হলে গোপনে আবার
    // লগইন করতে লাগে (নিচের reAuth দেখুন)। শুধু ব্যর্থ-পথে ব্যবহার হয়।
    @Volatile private var appCtx: Context? = null

    // 🔴🔒 V453 (20.08.2026, TK-অনুমোদিত · Free Plan Egress/Session fix —
    // আগের V440 লগে "বাকি — TK-এর সিদ্ধান্ত লাগবে" হিসেবে চিহ্নিত ছিল)।
    //
    // **আসল সমস্যা:** `accessToken` শুধু RAM-এ (@Volatile var) থাকত। Android
    // ব্যাকগ্রাউন্ড অ্যাপ প্রক্রিয়া মেরে দিলে (যা খুবই সাধারণ ঘটনা), এই
    // object আবার শূন্য থেকে শুরু হয় — WorkManager-এর প্রতিটা দফা
    // (BackgroundRefreshWorker · AttendanceReminderWorker · SalaryReminder
    // ইত্যাদি) `signInCurrentSession()` ডাকলেই একটা **সম্পূর্ণ নতুন
    // email+password লগইন** (Supabase Auth-এ নতুন session) হতো — টোকেন
    // তখনও হয়তো মেয়াদ-উত্তীর্ণ হয়ইনি। ১৫ জন স্টাফের জন্য এভাবেই বহু হাজার
    // অপ্রয়োজনীয় session জমেছিল।
    //
    // **সমাধান:** টোকেন + মেয়াদ (expiresAt) এখন SharedPreferences-এ জমা
    // থাকে। প্রক্রিয়া নতুন করে শুরু হলেও, মেয়াদ ফুরোয়নি এমন টোকেন থাকলে
    // — কোনো নতুন নেটওয়ার্ক-কল ছাড়াই সেটা আবার ব্যবহার হয়। মেয়াদ ফুরোলে
    // তবেই আগের মতো লগইন হয়। ⛔ লগইনের নিয়ম/পাসওয়ার্ড/RLS কিছুই বদলায়নি —
    // শুধু অপ্রয়োজনীয় বারবার-লগইন বন্ধ হলো।
    private const val PREF = "module_auth_state"
    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun loadPersisted(context: Context, expectedCode: String) {
        try {
            val p = prefs(context)
            val savedCode = p.getString("code", null) ?: return
            if (savedCode != expectedCode) return
            val expiresAt = p.getLong("expiresAt", 0L)
            if (expiresAt <= System.currentTimeMillis()) return
            val tok = p.getString("token", null) ?: return
            accessToken = tok
            personCode = p.getString("personCode", savedCode)
            isMaster = p.getBoolean("isMaster", false)
            accessTokenExpiresAt = expiresAt   // 🔴🔒 V1512
        } catch (_: Throwable) { }
    }

    private fun savePersisted(context: Context, code: String, expiresInSeconds: Int) {
        try {
            val safeSeconds = if (expiresInSeconds > 0) expiresInSeconds else 3600
            // ৬০ সেকেন্ড আগেই "মেয়াদ শেষ" ধরা হয় — ঘড়ির সামান্য গরমিল ঢাকতে,
            // যাতে কখনো মেয়াদ-উত্তীর্ণ টোকেন দিয়ে read না যায়।
            val expiresAt = System.currentTimeMillis() + (safeSeconds - 60L).coerceAtLeast(60L) * 1000L
            accessTokenExpiresAt = expiresAt   // 🔴🔒 V1512 — RAM-এও রাখা, tokenFresh() এটাই দেখে
            prefs(context).edit()
                .putString("code", code)
                .putString("token", accessToken)
                .putString("personCode", personCode)
                .putBoolean("isMaster", isMaster)
                .putLong("expiresAt", expiresAt)
                .apply()
        } catch (_: Throwable) { }
    }

    private fun clearPersisted(context: Context?) {
        try { context?.let { prefs(it).edit().clear().apply() } } catch (_: Throwable) { }
    }

    // 🔴🔒 V1512 — শুধু টোকেন "আছে" কিনা নয়, মেয়াদও তাজা কিনা দেখে। এই একটা
    // জায়গা ঠিক হওয়ায় গোটা কোডবেসে `.isSignedIn` ব্যবহার-করা প্রতিটা জায়গাই
    // (WorkNotebook, Worker-গুলো, DoctorVisit ইত্যাদি) এখন থেকে মেয়াদ-উত্তীর্ণ
    // টোকেনকে "সাইন-ইন করা আছে" ভুল করে ধরবে না।
    val isSignedIn: Boolean
        get() = !accessToken.isNullOrBlank() && System.currentTimeMillis() < accessTokenExpiresAt

    /** 🔴🔒 V429 (TK-নির্দেশ ১৭.০৮.২০২৬ — *"আমি সাধারণ ব্যবহারকারী, আমার সামনে
     *  যেন কোনো সমস্যা না আসে; এই সিদ্ধান্ত আপনাকে নিতে হবে"*)।
     *
     *  **যে সমস্যাটা হচ্ছিল:** কোনো পর্দা ক্লাউডে ডাক পাঠালে টোকেন লাগে। টোকেন
     *  না থাকলে নিচের `reAuth()` গোপনে আবার লগইন করে নেয় — কিন্তু তার জন্য
     *  `appCtx` দরকার, আর সেটা বসত **প্রথম সফল লগইনের পরে**। ফলে যে পর্দা
     *  সবার আগে ডাক পাঠাত, তার ডাকটা চুপচাপ ব্যর্থ হত, কোনো এররও দেখাত না —
     *  ব্যবহারকারীর মনে হতো জিনিসটা "নেই" (চেম্বারের RMP কমিশন ঠিক তাই হয়েছিল)।
     *
     *  **সমাধান:** অ্যাপ চালু হওয়ার সময়েই একবার context ধরে রাখা হয়। তাতে
     *  **যে কোনো পর্দার** প্রথম ডাকেও গোপন লগইনটা কাজ করে। এক জায়গায় ঠিক করায়
     *  ভবিষ্যতে নতুন পর্দা যোগ হলেও এই ভুল আর ফিরবে না।
     *  ⛔ লগইনের নিয়ম/পাসওয়ার্ড কিছুই বদলায়নি — শুধু context আগে থেকে রাখা। */
    fun attachContext(context: Context) {
        try { if (appCtx == null) appCtx = context.applicationContext } catch (_: Throwable) { }
    }

    // 🔵🔒 (09.08.2026, TK-রিপোর্ট — নেট ফাস্ট তবু "weak internet"): Supabase লগইন-টোকেন
    // ~১ ঘণ্টা পর মেয়াদ শেষ; isSignedIn শুধু টোকেন **আছে** কিনা দেখে, মেয়াদ নয়। তাই
    // মেয়াদ-শেষ টোকেন দিয়ে read গেলে 401 আসত, UI ভুল করে "দুর্বল নেট" বলত। এই ফাংশন
    // একবার গোপনে (কোনো পপ-আপ নেই) আবার লগইন করে নতুন টোকেন আনে। সফল হলে true।
    private fun reAuth(): Boolean {
        val c = appCtx ?: return false
        return try {
            // 🔴🔒 V464 (20.08.2026, TK-রিপোর্ট ছবিসহ — "JWT expired" আটকে
            // যাচ্ছিল, RMP Default Commission স্ক্রিনে)।
            //
            // **আসল কারণ (নিজের আজকের ভুল, খুঁজে বার করা হয়েছে):** `reAuth()`
            // ঠিক তখনই ডাকা হয় যখন বর্তমান টোকেন ইতিমধ্যে 401 দিয়েছে। কিন্তু
            // V453-এ যোগ করা `signInCurrentSession()`-এর "ইতিমধ্যে isSignedIn
            // থাকলে আবার লগইন না করা" শর্টকাট এখানে ভুল প্রয়োগ হচ্ছিল —
            // `isSignedIn` তখনও `true` থাকত (পুরনো, ব্যর্থ-হওয়া টোকেনটাই
            // মেমোরিতে বসা), তাই `reAuth()` **আসলে নতুন কোনো লগইন না করেই**
            // "সফল" বলে ফিরে যেত। ফলে retry-ও সেই একই খারাপ টোকেন দিয়ে আবার
            // ব্যর্থ হত — এটাই ব্যবহারকারীর দেখা "JWT expired" আটকে থাকার
            // আসল কারণ।
            //
            // **সমাধান:** এখানে আগে `signOut()` (মেমোরি + SharedPreferences
            // দুটোই সাফ) করে, তারপর `signInCurrentSession()` ডাকা হয় — তাই
            // এখন নিশ্চিতভাবে **সত্যিকারের নতুন লগইন** হয়, পুরনো ব্যর্থ
            // টোকেন কখনো পুনর্ব্যবহার হয় না। ⛔ V453-এর মূল সাশ্রয় (প্রক্রিয়া
            // নতুন শুরু হলে valid persisted token থাকলে পুনর্ব্যবহার) অক্ষত —
            // এই ফিক্স শুধু "টোকেন ইতিমধ্যে ব্যর্থ হয়েছে" এই একটা নির্দিষ্ট
            // পথেই প্রযোজ্য।
            signOut(c)
            signInCurrentSession(c) == null
        } catch (_: Exception) { false }
    }

    fun codeToEmail(code: String): String =
        code.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-") + "@" + EMAIL_DOMAIN

    /**
     * V247: The owner has already authenticated at the main app login, so the
     * private modules must not ask for a second visible password.  Derive the
     * module identity from that existing session and authenticate silently.
     */
    // 🔴 B317 (03.08.2026, TK-নির্দেশ — এক ব্যবহারকারীর পর অন্য একজন লগইন করলে
    // পুরনো Module পরিচয় যেন কখনো থেকে না যায়): কোড→পাসওয়ার্ড ঠিক করার
    // অংশটা এখানে অক্ষত রইল, শুধু কোড বার করার অংশ `expectedCode()`-এ আলাদা
    // করা হলো — যাতে `ModuleUi.ensureSignedIn()` কল না করেই আগে থেকে জেনে
    // নিতে পারে "এখন আসলে কার Module-এ সাইন-ইন থাকা উচিত", আর সেটার সাথে
    // মিলিয়ে দেখতে পারে বর্তমান cached সেশন সত্যিই ওই একই ব্যক্তির কিনা।
    fun expectedCode(context: Context): String? {
        val user = NativeSession.current(context) ?: return null
        val mobile = user.mobile.filter { it.isDigit() }.takeLast(10)
        return when (mobile) {
            "8001080080" -> "MASTER-TK"
            "7980993652" -> "DR-KH-MANDAL"
            "8001800148" -> "DR-JAY-BANIK"
            "9046366596" -> "DR-AMIT-GOLDAR"
            "6297625447" -> "DR-PK-ROY"
            "9002003540" -> "FIELD-OFFICER"
            // 🔵 V308 (১০.০৮): ৪ অংশীদার-ডাক্তারের মোবাইল→DR-কোড (V308 SQL-এর dr-…@staff.piles-এর মিল)।
            "7479173399" -> "DR-JH-MANDAL"
            "9002610352" -> "DR-GOKUL"
            "7810907954" -> "DR-SAIKAT-ROY"
            "9242009205" -> "DR-PRANAB-BISWAS"
            // 🔑 V749 (২৭.০৮.২০২৬, TK: *"KNE-LAXMI — এত মানুষ"*) —
            //    অ্যাপ থেকে যোগ করা লোকের **কোড** এখন জমানো তালিকা থেকে আসে,
            //    নাম থেকে নয়। তাই পর্দায় আসল নাম দেখানো যায়, আর মডিউলের
            //    auth-ইমেল (`<কোড>@staff.piles`) ঠিকঠাক মেলে।
            //
            //    ⚠️⚠️ **শর্তটা খেয়াল করুন — বাঁধা তালিকায় থাকলে এখানে ঢোকাই হয় না।**
            //      অর্থাৎ আজকের ২৩ জনের ক্ষেত্রে আগের নিয়মই (নাম→কোড) অটুট,
            //      এক অক্ষরও বদল নেই। মেঘের তালিকা না পেলেও আগের নিয়মেই ফেরে।
            //    ⛔ `cachedCodeFor` কখনো নেটে যায় না — শুধু ফোনে জমানোটা পড়ে।
            else -> {
                var out = user.name.trim().uppercase()
                if (StaffDirectory.findAccount(mobile) == null) {
                    val fromCloud = try {
                        com.tkbiswas.pilesclinic.native.CloudStaffDirectory
                            .cachedCodeFor(context, mobile)
                    } catch (_: Throwable) { null }
                    if (!fromCloud.isNullOrBlank()) out = fromCloud
                }
                out
            }
        }
    }

    /* 🔴🔒 V1581 (১৭.০৯.২০২৬, TK-রিপোর্ট — Staff Profile খুলতে মাঝে মাঝে
       ৪০+ সেকেন্ড আটকে "Could not open" আসছিল, অথচ নেট ভালো) — **সন্দেহভাজন
       কারণ:** এই ফাংশনটা প্রজেক্টের অনেক জায়গা থেকে ডাকা হয় (মূল অ্যাপে
       লগইনের সময়ও, আজকের V1512-এর নতুন ব্যাকগ্রাউন্ড সাইন-ইন সহ) — আর
       আজকের `isSignedIn`-মেয়াদ-ফিক্সের পরে এই ডাকটা **আগের চেয়ে বেশিবার**
       সত্যিকারের নেট-কল (`signIn()`) পর্যন্ত পৌঁছায় (উপরের মন্তব্যেই লেখা)।
       কোনো লক না থাকায়, দুটো জায়গা থেকে (যেমন লগইনের ব্যাকগ্রাউন্ড কল +
       তার কিছুক্ষণ পরেই স্টাফ Staff Profile খুললে) **একই মুহূর্তে দুটো
       আলাদা সাইন-ইন নেট-কল** একসাথে Supabase Auth-এ যেতে পারত — সেটা যদি
       ধীরতা/সংঘাতের একটা কারণ হয়ে থাকে।
       ⛔ ১০০% নিশ্চিত প্রমাণ করা যায়নি (ফোনে সরাসরি দেখার উপায় নেই), কিন্তু
       এই বদলটা **সম্পূর্ণ ঝুঁকিহীন**: এখন একই মুহূর্তে সর্বোচ্চ **একটাই**
       আসল সাইন-ইন নেট-কল চলে — অন্য যে কেউ একই সময়ে ডাকলে সে শুধু সেই
       একই ফলের অপেক্ষা করবে (আবার আলাদা কল করবে না), তারপর একই উত্তর পাবে।
       সফল/ব্যর্থ হওয়ার নিয়ম, পাসওয়ার্ড, ক্যাশ — কিছুই বদলায়নি। */
    private val signInLock = Any()

    fun signInCurrentSession(context: Context): String? {
        appCtx = context.applicationContext
        val user = NativeSession.current(context) ?: return "Main app login required"
        val mobile = user.mobile.filter { it.isDigit() }.takeLast(10)
        val code = expectedCode(context) ?: return "Main app login required"

        // 🔴🔒 V453: প্রক্রিয়া নতুন করে শুরু হলে RAM-এ কিছু থাকে না, কিন্তু আগের
        // মেয়াদ-অক্ষত টোকেন SharedPreferences-এ থাকতে পারে — থাকলে সেটাই
        // ব্যবহার হয়, নতুন কোনো লগইন-কল লাগে না।
        if (!isSignedIn) loadPersisted(context, code)
        // 🔴🔒 V1512 — `isSignedIn` এখন মেয়াদও দেখে (উপরে দেখুন), তাই এখানে
        // আলাদা কোনো "tokenFresh" চেক লাগে না — মেয়াদ ফুরিয়ে থাকলে
        // `isSignedIn` নিজেই false হবে আর নিচের কোড আবার সত্যিকারের সাইন-ইন করবে।
        if (isSignedIn && personCode == code) return null

        // 🔒 V1581: আসল নেট-কলটা এখন লকের ভিতরে — একসাথে দুটো চলবে না।
        synchronized(signInLock) {
            // লক পাওয়ার মধ্যেই অন্য কোনো থ্রেড ইতিমধ্যে সাইন-ইন করে ফেলে
            // থাকতে পারে (ঠিক এই কোডটাই চালিয়ে) — সেটা হলে আর নতুন কল না
            // করে সেই ফলটাই ব্যবহার করা হয়।
            if (isSignedIn && personCode == code) return null

            // Reuse only the project's long-standing role passwords. No new
            // password is created and no second password is shown anywhere.
            val originalRole = StaffDirectory.findAccount(mobile)?.role ?: user.displayRole
            val password = when (originalRole) {
                "master" -> "admin123"
                "doctor" -> "doctor123"
                "field" -> "field123"
                else -> "staff123"
            }
            val err = signIn(code, password)
            if (err == null) savePersisted(context, code, lastExpiresIn)
            return err
        }
    }

    private fun baseUrl(): String = SupabaseConfig.url.trimEnd('/')
    private fun anonKey(): String = SupabaseConfig.anonKey

    // 🔴🔒 V453: শেষ সফল সাইন-ইনের expires_in (সেকেন্ড) — savePersisted()-এ
    // ব্যবহারের জন্য। Supabase সাধারণত 3600 পাঠায়; না পেলে 3600 ধরে নেওয়া হয়
    // (savePersisted()-এর নিজস্ব safe default)।
    @Volatile private var lastExpiresIn: Int = 0

    // 🔴🔒 V1570 — শেষ সফল সাইন-ইনের নিজের auth.uid() (Supabase লগইন-উত্তরের
    // `user.id`)। নিচের identity-খোঁজায় `uid=eq.` ফিল্টার দিতে ব্যবহার হয় —
    // দেখুন signIn()-এর বড় মন্তব্য।
    @Volatile private var myUid: String = ""

    /** Sign in with Staff Code + module password. Returns null on success, else error text.
     *  Blocking network call — run it on a background thread. */
    /* 🔴🔒 V808 — এখন ভুলের বার্তায় **ঠিক কী হয়েছে** লেখা থাকে: কোন ধাপে
       আটকেছে · সার্ভার কী কোড পাঠিয়েছে · কত সেকেন্ড লেগেছে · কোন ধরনের গোলমাল।
       আগে শুধু "timeout" আসত — ওটা দিয়ে কারণ বোঝার কোনো উপায় ছিল না, তাই
       সমস্যাটা ধরাই যাচ্ছিল না। ⛔ সফল পথে এক অক্ষরও বদলায়নি। */
    fun signIn(code: String, password: String): String? {
        val t0 = System.currentTimeMillis()
        fun secs() = "%.1f".format((System.currentTimeMillis() - t0) / 1000.0)
        try {
            val body = JSONObject()
                .put("email", codeToEmail(code))
                .put("password", password)
                .toString().toRequestBody(JSON)
            /* 🔴🔴🔴🔒 V811 (২৮.০৮.২০২৬) — **আসল কারণ পাওয়া গেল।**
               TK-এর নেট মেপে দেখা: 10.4 Mbps ↓ · 42.3 Mbps ↑ · latency ৫৮ ms —
               অর্থাৎ নেট দ্রুত, "ধীর নেট" আমার আগের অনুমান **ভুল** ছিল।
               ─── যেভাবে ধরা পড়ল (কাজ করা vs আটকে যাওয়া ডাক মিলিয়ে) ──────────
               এই অ্যাপের **যত ডাক কাজ করে** (`SupabaseClient.kt:333-334, 464-465`)
               সবগুলোই **দুটো** হেডার পাঠায়:
                     apikey: <key>   ও   Authorization: Bearer <key>
               ওয়েবের Supabase SDK-ও (`createClient`) দুটোই পাঠায় — ওয়েবে তাই
               মডিউল-লগইন চলে।
               কিন্তু **এই একটামাত্র ডাক** পাঠাত **শুধু `apikey`** — `Authorization`
               হেডারটাই ছিল না। নতুন ধরনের চাবিতে (`sb_publishable_…`) Supabase-এর
               গেটওয়ে দুটোই চায়; একটা না পেলে ডাকটা সাড়াই দেয় না ⇒ অ্যাপ
               অপেক্ষা করতেই থাকে ⇒ V793-এ সাদা পর্দা, V803-এর পরে "timeout"।
               ─── প্রমাণ যে সময়সীমা দোষী নয় ─────────────────────────────────
               `SupabaseClient`-এর নিজের মাপও connect ৮s · read ৮s · call ২৫s —
               **হুবহু একই**, আর ওগুলো দিব্যি কাজ করে। তাই ২৫ সেকেন্ড কম ছিল না।
               ⇒ আমি আগে যে বলেছিলাম "সময়সীমা কমিয়ে আমি ভেঙেছি" — **সেটাও ভুল
                 ছিল**। দোষটা এই অনুপস্থিত হেডার, প্রথম দিন থেকেই।
               ⛔ সারানো: বাকি সব ডাকের মতোই দুটো হেডারই পাঠানো হয়। */
            val req = Request.Builder()
                .url(baseUrl() + "/auth/v1/token?grant_type=password")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + anonKey())
                .addHeader("Content-Type", "application/json")
                .post(body).build()
            http.newCall(req).execute().use { resp ->
                val txt = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    val why = try { JSONObject(txt).optString("error_description", "").ifBlank {
                        JSONObject(txt).optString("msg", "")
                    } } catch (_: Exception) { "" }
                    return "Step 1 (login) — server said HTTP ${resp.code}" +
                        (if (why.isNotBlank()) ": $why" else "") +
                        "\n\nCode: $code · ${secs()}s"
                }
                val json = JSONObject(txt)
                accessToken = json.optString("access_token", "")
                lastExpiresIn = json.optInt("expires_in", 3600)
                if (accessToken.isNullOrBlank()) return "Step 1 (login) — no token came back. ${secs()}s"
                myUid = json.optJSONObject("user")?.optString("id", "").orEmpty()
            }
            // 🔴🔴🔴🔒 V1570 (১৭.০৯.২০২৬, TK-ভিডিওতে ধরা পড়া — মাস্টারের ফোনে
            // "Staff Profiles" বারবার একজন নির্দিষ্ট স্টাফের (COB-4) নিজের
            // পাতা দেখাচ্ছিল, মাস্টারের তালিকা নয়) — **আসল কারণ, কোডে যাচাই
            // করে ধরা:** এই লাইনটা "আমার নিজের পরিচয়" আনতে `uid=eq.<নিজের>`
            // ফিল্টার **ছাড়াই** `select=...&limit=1` চালাত, শুধু RLS-এর
            // "নিজেরটাই দেখা যায়" নিয়মের উপর ভরসা করে। কিন্তু মাস্টারের RLS
            // **সবার** সারি দেখতে দেয় — তাই `limit=1` (কোনো ORDER BY নেই)
            // মাস্টারের বেলায় হর্তা-কর্তা RLS-এর জন্য **এলোমেলো যেকোনো একটা
            // সারি** ফিরিয়ে দিত (এখানে সবসময় COB-4-এর, কারণ ২৩ জনের সবার
            // `created_at` হুবহু এক সেকেন্ডে বসেছিল — V246, তাই সাজানোর কোনো
            // স্বাভাবিক ক্রম নেই)। ফল: মাস্টারের `personCode`/`isMaster`
            // চুপচাপ COB-4-এর মানে বদলে যেত, তাই "Staff Profiles" তাঁকে
            // মাস্টার না ভেবে COB-4 ভেবে তাঁরই পাতা দেখাত।
            // ⛔ এই বাগ **আজকের কাজে তৈরি হয়নি, আগে থেকেই ছিল** — কিন্তু
            //   আগে `isSignedIn` মেয়াদ দেখত না বলে একবার (ভাগ্যক্রমে ঠিক
            //   বা ভুল) কোনো একটা পরিচয় ক্যাশ হয়ে গেলে সেটাই চিরকাল
            //   ব্যবহার হতো, তাই এই ভুল ডাকটা কমই আবার চলত। আজ `isSignedIn`
            //   ঠিক করার পর টোকেন সত্যিই ফুরোলে/প্রতি লগইনে ডাকটা আবার
            //   চলছে, তাই ভুলটা এখন বারবার চোখে পড়ছে।
            // ✅ সমাধান: Supabase-এর লগইন-উত্তরেই থাকা নিজের `uid` (auth.uid())
            //   দিয়ে সরাসরি ফিল্টার করা হলো — RLS যতই চওড়া হোক, `uid=eq.`
            //   দিয়ে সবসময় **শুধু নিজের** সারিটাই আসবে, মাস্টার হোক বা স্টাফ।
            val uidFilter = if (myUid.isNotBlank()) "&uid=eq.$myUid" else ""
            val id = getRows("hr", "app_identity", "select=person_code,role_kind,is_master$uidFilter&limit=1")
            if (id.length() > 0) {
                personCode = id.getJSONObject(0).optString("person_code", code)
                isMaster = id.getJSONObject(0).optBoolean("is_master", false)
            } else { personCode = code; isMaster = false }
            return null
        } catch (e: Exception) {
            val kind = e.javaClass.simpleName
            val msg = e.message.orEmpty()
            val plain = when {
                kind.contains("UnknownHost") -> "Could not find the server — check the internet connection."
                kind.contains("SSL") || kind.contains("Certificate") -> "Secure connection failed."
                msg.contains("timeout", true) || kind.contains("Timeout") ->
                    "The server did not answer in time."
                kind.contains("ConnectException") || kind.contains("SocketException") ->
                    "Could not reach the server."
                else -> "Could not sign in."
            }
            return "Step 1 (login) — $plain\n\n$kind" +
                (if (msg.isNotBlank()) ": $msg" else "") + " · ${secs()}s"
        }
    }

    // 🔴🔒 V453: context ঐচ্ছিক — পুরনো ৫+ ব্যবহারের জায়গা (identity-switch
    // পথ, `signOut()` কোনো আর্গুমেন্ট ছাড়াই) অপরিবর্তিত কাজ করবে, শুধু RAM-এর
    // টোকেন সাফ হবে (আগের মতোই)। যেখানে context দেওয়া হবে (যেমন মূল অ্যাপ
    // Logout) — সেখানে SharedPreferences-এর জমানো টোকেনও মুছে যাবে, যাতে এই
    // ফোনে পরের ব্যক্তি লগইন করলে আগের কারো cached session কখনো ফিরে না আসে।
    fun signOut(context: Context? = null) {
        // 🔐🔒 V494 (TK-যাচাই ২): জোর করে সাইন-আউট হলেও জমানো cloud-পড়া মুছবে।
        try { com.tkbiswas.pilesclinic.native.CloudReadDedupe.clear() } catch (_: Throwable) { }
        clearPersisted(context ?: appCtx)
        accessToken = null; personCode = null; isMaster = false
    }

    private fun authGet(url: String, schema: String): Request =
        Request.Builder().url(url)
            .addHeader("apikey", anonKey())
            .addHeader("Authorization", "Bearer " + (accessToken ?: ""))
            .addHeader("Accept-Profile", schema)
            .get().build()

    /** SELECT from a schema-qualified table. `query` is a raw PostgREST query. */
    fun getRows(schema: String, table: String, query: String): JSONArray {
        return try {
            http.newCall(authGet(baseUrl() + "/rest/v1/" + table + "?" + query, schema)).execute().use { resp ->
                val txt = resp.body?.string() ?: "[]"
                if (resp.isSuccessful) JSONArray(txt) else JSONArray()
            }
        } catch (e: Exception) { JSONArray() }
    }

    // 🔴 B316 (03.08.2026, TK-নির্দেশ — Staff Profile Save ফাঁকা ডেটা দিয়ে
    // ওভাররাইট করার ঝুঁকি): উপরের `getRows()` সফল-খালি বনাম ব্যর্থ — এই দুটো
    // আলাদা করে না (দুটোতেই খালি JSONArray দেয়), তাই "লোড ব্যর্থ হলে Save
    // বন্ধ রাখা" নিরাপদভাবে করা যাচ্ছিল না। এই নতুন ফাংশনটা **শুধু যোগ**
    // হয়েছে — পুরনো `getRows()` ও তার ৫০+ ব্যবহারের জায়গা এক অক্ষরও বদলায়নি,
    // তাই বাকি প্রজেক্টে কোনো ঝুঁকি নেই। শুধু Staff Profile-এর Save-সুরক্ষায়
    // ব্যবহার হবে।
    data class RowsResult(val ok: Boolean, val rows: JSONArray)
    // একটাই read-চেষ্টা: (সফল?, সারি, HTTP-কোড)। কোড -1 = নেটওয়ার্ক/এক্সসেপশন।
    private fun rawRows(schema: String, table: String, query: String): Triple<Boolean, JSONArray, Int> {
        return try {
            http.newCall(authGet(baseUrl() + "/rest/v1/" + table + "?" + query, schema)).execute().use { resp ->
                val txt = resp.body?.string() ?: "[]"
                if (resp.isSuccessful) Triple(true, JSONArray(txt), resp.code)
                else Triple(false, JSONArray(), resp.code)
            }
        } catch (e: Exception) { Triple(false, JSONArray(), -1) }
    }
    fun getRowsChecked(schema: String, table: String, query: String): RowsResult {
        val first = rawRows(schema, table, query)
        if (first.first) return RowsResult(true, first.second)
        // 🔵 401 = টোকেন মেয়াদ শেষ (নেটের দোষ নয়)। একবার গোপনে re-login করে আবার চেষ্টা।
        // ⛔ additive — সফল read-এ কিছু বদলায় না, শুধু 401-পথে সেল্ফ-হিল।
        if (first.third == 401 && reAuth()) {
            val second = rawRows(schema, table, query)
            if (second.first) return RowsResult(true, second.second)
        }
        return RowsResult(false, JSONArray())
    }

    /** UPSERT one row (id-keyed) into a schema-qualified table. */
    /* 🔴🔒 V1432 (১৩.০৯.২০২৬, তালিকা ৫৫০, নিয়ম ৭) — read (`getRowsChecked`) · rpc · delete আগে থেকেই
       টোকেনের মেয়াদ শেষ হলে (401) একবার গোপনে re-login করে আবার চেষ্টা করত, কিন্তু
       **লেখা** (upsert · upsertOnConflict · insert · update) করত না ⇒ এক ঘণ্টা অ্যাপ খোলা থাকলে
       Save-এ "Retry"। এখন সব লেখা-পথে একই সেল্ফ-হিল। ⛔ সফল লেখায় কিছু বদলায় না। */
    private fun postOnce(url: String, schema: String, prefer: String, body: String): Pair<Boolean, Int> = try {
        val req = Request.Builder().url(url)
            .addHeader("apikey", anonKey())
            .addHeader("Authorization", "Bearer " + (accessToken ?: ""))
            .addHeader("Content-Profile", schema)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", prefer)
            .post(body.toRequestBody(JSON)).build()
        http.newCall(req).execute().use { resp -> Pair(resp.isSuccessful, resp.code) }
    } catch (_: Exception) { Pair(false, -1) }

    private fun postWithReauth(url: String, schema: String, prefer: String, body: String): Boolean {
        var r = postOnce(url, schema, prefer, body)
        if (!r.first && r.second == 401 && reAuth()) r = postOnce(url, schema, prefer, body)
        return r.first
    }

    fun upsert(schema: String, table: String, row: JSONObject): Boolean =
        postWithReauth(baseUrl() + "/rest/v1/" + table, schema, "resolution=merge-duplicates,return=minimal", JSONArray().put(row).toString())

    // 🔵 TK-ORDER (07.08.2026): উপরের upsert() `merge-duplicates` করে **PK (id)**
    // ধরে — কিন্তু IN TIME-এর `day`-তে id থাকে না, তাই একই দিনের সারিতে না বসে
    // (staff_code, work_date) unique-constraint-এ ধাক্কা খেয়ে **ব্যর্থ** হতে পারত।
    // এই মেথড `?on_conflict=<cols>` দিয়ে ঠিক ওই unique-key ধরে বসায় — সারি না
    // থাকলে নতুন বসে, থাকলে আপডেট হয় — কখনো ব্যর্থ হয় না, ডুপ্লিকেটও হয় না।
    // ⛔ পুরনো upsert() এক অক্ষরও বদলায়নি (অন্য জায়গায় অক্ষত)। শুধু payload-এ
    //    থাকা কলামগুলোই আপডেট হয়, বাকি ঘর (যেমন আগের check_out) অক্ষত থাকে।
    fun upsertOnConflict(schema: String, table: String, row: JSONObject, onConflict: String): Boolean =
        postWithReauth(baseUrl() + "/rest/v1/" + table + "?on_conflict=" + onConflict, schema, "resolution=merge-duplicates,return=minimal", JSONArray().put(row).toString())   // V1432 — 401 সেল্ফ-হিল

    /** INSERT one row (outside_calls / call_taps / work_reports / payments). */
    fun insert(schema: String, table: String, row: JSONObject): Boolean =
        insertChecked(schema, table, row).ok

    /** 🔴🔒 V418 (TK-নির্দেশ, ১৭.০৮.২০২৬: *"ভবিষ্যতে ডুপ্লিকেট এন্ট্রির জন্য
     *  আটকে দেয়"*)
     *
     *  পুরনো `insert()` শুধু হ্যাঁ/না বলত। তাতে **"নেট নেই"** আর **"ডেটাবেস
     *  ডুপ্লিকেট বলে আটকে দিয়েছে"** — দুটোই এক দেখাত, আর পর্দায় ভুল করে
     *  "Saved offline / retry" উঠত। টাকার হিসাবে ওটা বিপজ্জনক: জমা হয়নি, অথচ
     *  ব্যবহারকারী ভাবতেন জমা হয়েছে।
     *
     *  এখন সাড়ার সংকেতটাও ফেরে, তাই পর্দায় **সৎ** কথা বলা যায়।
     *  `duplicate = true` তখনই, যখন ডেটাবেস নিজের unique-নিয়মে আটকায়
     *  (HTTP 409 / Postgres কোড 23505)।
     *  ⛔ পুরনো `insert()` আগের মতোই হ্যাঁ/না ফেরায় — কোনো ডাকার জায়গা বদলায়নি। */
    data class InsertResult(val ok: Boolean, val code: Int, val duplicate: Boolean, val message: String)

    fun insertChecked(schema: String, table: String, row: JSONObject): InsertResult {
        val first = insertOnce(schema, table, row)
        if (!first.ok && first.code == 401 && reAuth()) return insertOnce(schema, table, row)   // V1432 — 401 সেল্ফ-হিল
        return first
    }
    private fun insertOnce(schema: String, table: String, row: JSONObject): InsertResult {
        return try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table)
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: ""))
                .addHeader("Content-Profile", schema)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(JSONArray().put(row).toString().toRequestBody(JSON)).build()
            http.newCall(req).execute().use { resp ->
                val body = try { resp.body?.string() ?: "" } catch (_: Exception) { "" }
                val dup = !resp.isSuccessful &&
                    (resp.code == 409 || body.contains("23505") ||
                     body.contains("duplicate key", true))
                InsertResult(resp.isSuccessful, resp.code, dup, body)
            }
        } catch (e: Exception) { InsertResult(false, -1, false, "") }
    }

    /** V325: call a schema-scoped, authenticated database function.
     * Commission payment uses this instead of writing Expense directly: the
     * database validates role/date/overpayment and writes commission + expense
     * atomically. A failed call writes neither row. Existing CRUD is untouched. */
    data class RpcResult(val ok: Boolean, val body: String, val message: String)
    fun rpc(schema: String, function: String, args: JSONObject): RpcResult {
        fun once(): Triple<Boolean, String, Int> = try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/rpc/" + function)
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: ""))
                .addHeader("Accept-Profile", schema)
                .addHeader("Content-Profile", schema)
                .addHeader("Content-Type", "application/json")
                .post(args.toString().toRequestBody(JSON)).build()
            http.newCall(req).execute().use { resp ->
                Triple(resp.isSuccessful, resp.body?.string() ?: "", resp.code)
            }
        } catch (_: Exception) { Triple(false, "", -1) }
        var result = once()
        if (!result.first && result.third == 401 && reAuth()) result = once()
        val message = if (result.first) "" else try {
            JSONObject(result.second).optString("message", "Request failed")
        } catch (_: Exception) { "Request failed" }
        return RpcResult(result.first, result.second, message)
    }

    /** PATCH: update rows in a schema-qualified table matching `filter`. */
    fun update(schema: String, table: String, filter: String, patch: JSONObject): Boolean {
        fun once(): Pair<Boolean, Int> = try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table + "?" + filter)
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: ""))
                .addHeader("Content-Profile", schema)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(patch.toString().toRequestBody(JSON)).build()
            http.newCall(req).execute().use { resp -> Pair(resp.isSuccessful, resp.code) }
        } catch (e: Exception) { Pair(false, -1) }
        var r = once()
        if (!r.first && r.second == 401 && reAuth()) r = once()   // V1432 — 401 সেল্ফ-হিল
        return r.first
    }

    /** 📊 V824 — DELETE from a schema-qualified table (raw PostgREST filter).
     *  ⛔ শুধু নতুন `fin.registration_count_excluded` ("গোনায় ধরব না" দাগ)
     *     সরাতে ব্যবহার হয় — রোগী · টাকা · Follow-up কোনো টেবিলে এটা ডাকা
     *     হয় না। RLS সার্ভারেই ঠিক করে কে মুছতে পারে (শুধু মাস্টার)।
     *  ⛔ পুরনো কোনো ফাংশন ছোঁয়া হয়নি — এটা সম্পূর্ণ নতুন ও আলাদা। */
    fun deleteRows(schema: String, table: String, filter: String): Boolean {
        fun once(): Pair<Boolean, Int> = try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table + "?" + filter)
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: ""))
                .addHeader("Content-Profile", schema)
                .addHeader("Accept-Profile", schema)
                .addHeader("Prefer", "return=minimal")
                .delete().build()
            http.newCall(req).execute().use { resp -> Pair(resp.isSuccessful, resp.code) }
        } catch (_: Exception) { Pair(false, -1) }
        var result = once()
        if (!result.first && result.second == 401 && reAuth()) result = once()
        return result.first
    }

    /** PATCH করে সত্যিই অন্তত একটি সারি বদলেছে কি না যাচাই করে।
     *  PostgREST `return=minimal`-এ ০ সারিও HTTP-success হওয়ায় Remove-এর মতো
     *  গুরুত্বপূর্ণ কাজে সেই পুরনো Boolean যথেষ্ট নয়। */
    fun updateAtLeastOne(schema: String, table: String, filter: String, patch: JSONObject): Boolean {
        fun once(): Pair<Boolean, Int> = try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table + "?" + filter)
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: ""))
                .addHeader("Content-Profile", schema)
                .addHeader("Accept-Profile", schema)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(patch.toString().toRequestBody(JSON)).build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: "[]"
                Pair(resp.isSuccessful && try { JSONArray(body).length() > 0 } catch (_: Throwable) { false }, resp.code)
            }
        } catch (_: Exception) { Pair(false, -1) }
        var result = once()
        if (!result.first && result.second == 401 && reAuth()) result = once()
        return result.first
    }

    /** Count rows in the EXISTING public tables (read-only; those tables have
     *  no RLS). Used for the Notebook's automatic statistics. Never writes. */
    fun countPublic(table: String, query: String): Int {
        return try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table + "?" + query + "&select=id")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: anonKey()))
                .addHeader("Prefer", "count=exact")
                .get().build()
            http.newCall(req).execute().use { resp ->
                val cr = resp.header("Content-Range") ?: ""
                val total = cr.substringAfter('/', "").toIntOrNull()
                if (total != null) total else JSONArray(resp.body?.string() ?: "[]").length()
            }
        } catch (e: Exception) { 0 }
    }

    // 🔴🔒 B496 (06.08.2026, TK-এর স্টাফের রিপোর্ট — "এনকোয়ারি ছিল, এখন
    // জিরো দেখাচ্ছে") — আসল `countPublic()` নেটওয়ার্ক-ব্যর্থতা আর
    // সত্যিকারের-শূন্য আলাদা করে না (দুটোতেই 0), তাই সাময়িক দুর্বল
    // সিগন্যালেই আজকের আসল সংখ্যা "মুছে" গেছে বলে মনে হতো। পুরনো
    // `countPublic()` (৫+ জায়গায় ব্যবহৃত) এক অক্ষরও বদলানো হয়নি — এই
    // নতুন ফাংশনটাই শুধু যোগ, ব্যর্থ হলে `ok=false` জানায়।
    data class CountResult(val ok: Boolean, val count: Int)
    fun countPublicChecked(table: String, query: String): CountResult {
        return try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table + "?" + query + "&select=id")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: anonKey()))
                .addHeader("Prefer", "count=exact")
                .get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return CountResult(false, 0)
                val cr = resp.header("Content-Range") ?: ""
                val total = cr.substringAfter('/', "").toIntOrNull()
                CountResult(true, total ?: JSONArray(resp.body?.string() ?: "[]").length())
            }
        } catch (e: Exception) { CountResult(false, 0) }
    }

    /** Sum a numeric column in public.payments for this staff in a range. */
    fun sumPublic(table: String, query: String, col: String): Double {
        return try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table + "?" + query + "&select=" + col)
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: anonKey()))
                .get().build()
            http.newCall(req).execute().use { resp ->
                val arr = JSONArray(resp.body?.string() ?: "[]")
                var s = 0.0
                for (i in 0 until arr.length()) s += arr.getJSONObject(i).optDouble(col, 0.0)
                s
            }
        } catch (e: Exception) { 0.0 }
    }

    // 🔴🔒 AUDIT FIX (2026-08-06, TK approved — same family as B496/B316):
    // the original `sumPublic()` returns 0.0 both on genuine-zero AND on a
    // network failure, so a weak signal could make today's real Collection
    // look like ₹0. The old `sumPublic()` (and its existing callers) is left
    // untouched — this is an ADD-ONLY checked variant that reports ok=false on
    // failure, so callers can show "…" instead of a misleading ₹0.
    data class SumResult(val ok: Boolean, val sum: Double)
    fun sumPublicChecked(table: String, query: String, col: String): SumResult {
        return try {
            val req = Request.Builder().url(baseUrl() + "/rest/v1/" + table + "?" + query + "&select=" + col)
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer " + (accessToken ?: anonKey()))
                .get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return SumResult(false, 0.0)
                val arr = JSONArray(resp.body?.string() ?: "[]")
                var s = 0.0
                for (i in 0 until arr.length()) s += arr.getJSONObject(i).optDouble(col, 0.0)
                SumResult(true, s)
            }
        } catch (e: Exception) { SumResult(false, 0.0) }
    }

    /** Log an in-app call-button press (owner rule 8). Records ONLY the press —
     *  never claims the call connected, never a duration. Silent on failure.
     *
     *  🔴 B406 (04.08.2026, TK-রিপোর্ট, Laxmi/Kishanganj — "৩ বার কল করলাম,
     *  App Calls-এ ২ দেখাচ্ছে"): আসল কারণ — এই ফাংশন আগে `if (!isSignedIn)
     *  return` করেই থেমে যেত, চুপচাপ, কোনো রেকর্ড না রেখে। ModuleAuth-এর নিজের
     *  সাইন-ইন (Work Notebook/Staff Profile/Income-Expense খুললে তবেই হয়)
     *  আর মূল অ্যাপের কল-বোতাম চাপা — দুটো সম্পূর্ণ স্বাধীন। তাই স্টাফ যদি
     *  আজ এখনো Work Notebook না খুলে থাকেন (বা অ্যাপ সদ্য চালু হয়ে থাকে),
     *  প্রথম কল-চাপাটা লগ-ই হতো না — ঠিক এইভাবেই সংখ্যা কম দেখাত। এখন সাইন-ইন
     *  না থাকলে **প্রথমে নিঃশব্দে সাইন-ইন করার চেষ্টা** হয়, তারপরই লগ করা হয়
     *  (ডায়ালার খোলায় কোনো দেরি হয় না, এটা আলাদা ব্যাকগ্রাউন্ড থ্রেডে)। ⛔
     *  ব্যর্থ হলেও (নেট না থাকলে) আগের মতোই নিঃশব্দে বাদ — কল-করাটা কখনো
     *  আটকায় না, শুধু গোনাটা মিস হতে পারে।
     *
     *  🔴 নিজে-ধরা সংশোধন (একই B406 কমিটে, ডেলিভারির আগে দ্বিতীয়বার
     *  যাচাই করতে গিয়ে ধরা পড়েছে): শুধু `!isSignedIn` চেক করলে B317-এর
     *  ঠিক করা ঝুঁকিই (এক ব্যবহারকারীর পর অন্যজন লগইন করলে আগের ব্যক্তির
     *  Module-পরিচয় থেকে যাওয়া) এই একটা জায়গায় ফিরে আসত — যদি আগের
     *  ব্যবহারকারী A কোনো Module স্ক্রিন খুলে ModuleAuth-এ সাইন-ইন করে
     *  রেখে যান, তারপর B লগইন করে সরাসরি কল-বোতাম চাপেন (কোনো Module
     *  স্ক্রিন না খুলেই), `isSignedIn` তখনো true (A-এর) — কল-ট্যাপ ভুল
     *  করে A-এর নামে লগ হতো। এখন `ModuleUi.ensureSignedIn()`-এর same
     *  identity-check এখানেও — `personCode == expectedCode(context)`
     *  না মিললে আগে `signOut()` করে তারপর বর্তমান ব্যবহারকারী হিসেবে
     *  নতুন করে সাইন-ইন করা হয়। */
    // 🔴🔒 B503 (06.08.2026, TK-নির্দেশ — "সাথে সাথে দেখাতে হবে") — এই
    // ফোনেই কতগুলো কল-বোতাম আজ চাপা হয়েছে তার একটা তাৎক্ষণিক, স্থানীয়
    // গণনা রাখা হয় (নেটওয়ার্ক-ছাড়া) — Work Notebook-এর "App Calls
    // (auto)" ঘরটা এখন প্রথমে এই সংখ্যা দেখাতে পারবে, পরে ক্লাউড থেকে
    // মিলিয়ে/সংশোধন করে নেবে। ⛔ আসল লগ (`call_taps` টেবিলে insert)
    // এতটুকুও বদলায়নি — এটা শুধু একটা বাড়তি, স্থানীয় গোনা।
    private fun localCallTapPrefs(context: Context) = context.getSharedPreferences("wn_local_counts", Context.MODE_PRIVATE)
    fun bumpLocalCallTapCount(context: Context, staffCode: String, dateIso: String) {
        try {
            val key = "calltaps_${staffCode}_$dateIso"
            val p = localCallTapPrefs(context)
            p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
        } catch (_: Throwable) { }
    }
    fun localCallTapCount(context: Context, staffCode: String, dateIso: String): Int {
        return try { localCallTapPrefs(context).getInt("calltaps_${staffCode}_$dateIso", 0) } catch (_: Throwable) { 0 }
    }

    // 🔴🔴🔒 V1338 (১১.০৯.২০২৬, TK-নির্দেশ, উদাহরণ JPE-CRP ১৭-বনাম-৪) — call_taps
    // এতদিন **পুরোপুরি fire-and-forget** ছিল: প্রতিবার Call বোতাম চাপলে একটা
    // ব্যাকগ্রাউন্ড Thread ইনসার্ট চেষ্টা করত, ঠিক তার পরেই ফোনের ডায়ালার
    // খুলতে গিয়ে অ্যাপ ব্যাকগ্রাউন্ডে চলে যেত — কম-RAM ফোনে Android মাঝেমধ্যে
    // ওই Thread শেষ হওয়ার আগেই প্রসেস থামিয়ে দিতে পারে, আর তাতে কোনো
    // retry/queue না থাকায় সেই কলটা চিরকালের জন্য গোনা থেকে বাদ পড়ে যেত —
    // স্টাফ চাপলেন, তবু সংখ্যা বাড়ল না, অথচ পর্দায় কোনো ভুল বার্তাও দেখায়নি।
    // ⛔ পুরনো ৫৫+ জায়গায় ব্যবহৃত CloudWriteQueue `public` স্কিমার জন্য বানানো
    //    (Content-Profile হেডার নেই) — এই মুহূর্তে সেটা `wn` স্কিমা পর্যন্ত
    //    বাড়ানো ঝুঁকিপূর্ণ (এত বড় শেয়ার্ড কোড, আসল ZIP পাঠানোর ঠিক আগে)।
    //    তাই call_taps-এর জন্য এই ছোট, আলাদা, শুধু এই একটা কাজের queue —
    //    পুরনো CloudWriteQueue-র একটি লাইনও ছোঁয়া হয়নি।
    // কাজ করে যেভাবে: প্রতিটা tap-এর নিজস্ব client-তৈরি UUID (`id`) থাকে, তাই
    // পরে আবার পাঠালেও ডুপ্লিকেট হয় না (on_conflict=id)। পাঠানোর **আগেই**
    // (নেট শুরু হওয়ার আগে, মূল থ্রেডে) SharedPreferences-এ ছোট্ট JSON-এ
    // সেভ হয়ে যায় — অ্যাপ পরমুহূর্তে মারা গেলেও এই এক-লাইনের সেভটুকু থাকে।
    // সফল হলে সেখান থেকে মুছে যায়; ব্যর্থ হলে পরে অ্যাপ খোলার সময়
    // (`flushPendingCallTaps`, BottomNav.kt-এর অন্য সবগুলোর মতোই) আবার
    // চেষ্টা হয়।
    private fun pendingCallTapPrefs(context: Context) = context.getSharedPreferences("wn_pending_call_taps", Context.MODE_PRIVATE)

    private fun savePendingCallTap(context: Context, id: String, row: JSONObject) {
        try {
            val p = pendingCallTapPrefs(context)
            p.edit().putString(id, row.toString()).commit()   // ⛔ commit() ইচ্ছে করেই — এর পরেই অ্যাপ ব্যাকগ্রাউন্ডে চলে যায়
        } catch (_: Throwable) { }
    }
    private fun clearPendingCallTap(context: Context, id: String) {
        try { pendingCallTapPrefs(context).edit().remove(id).apply() } catch (_: Throwable) { }
    }

    /** ⛔ শুধু call_taps-এর নিজস্ব ছোট queue — বাকি সব app-এর retry (CloudWriteQueue)
     * এতটুকুও বদলায়নি। BottomNav.kt-এ বাকি সব flushPending()-এর পাশে বসানো, প্রতি
     * পর্দা খোলার সময় একবার — একজন স্টাফের একদিনে সর্বোচ্চ কয়েক ডজন tap, তাই খরচ নগণ্য। */
    fun flushPendingCallTaps(context: Context) {
        try {
            val p = pendingCallTapPrefs(context)
            val all = p.all
            if (all.isEmpty()) return
            if (!isSignedIn) { try { signInCurrentSession(context) } catch (_: Throwable) { } }
            if (!isSignedIn) return
            for ((id, raw) in all) {
                try {
                    val row = JSONObject(raw as? String ?: continue)
                    if (upsertOnConflict("wn", "call_taps", row, "id")) {
                        clearPendingCallTap(context, id)
                    }
                } catch (_: Throwable) { }
            }
        } catch (_: Throwable) { }
    }

    fun logCallTap(mobileDigits: String, context: Context) {
        try {
            // 🔴 V452 (19.08.2026, TK-অনুমোদিত): Staff Performance-এ Master যেন
            // ভবিষ্যতের App Call-এ ঠিক কোন নম্বরে dial করা হয়েছিল সেটা দেখতে পারেন।
            // পুরনো row-তে full number ছিল না — সেগুলো আন্দাজ করে পূরণ করা হবে না।
            // এখানে শুধু caller যে digits-এ চাপ দিয়েছেন সেটাই রাখা হয়; call connected
            // হয়েছে এমন দাবি করা হয় না। Existing masked field-ও backward compatibility-এর
            // জন্য আগের মতোই রাখা হচ্ছে।
            val fullMobile = mobileDigits.filter { it.isDigit() }
            val masked = if (fullMobile.length >= 4) "••••••" + fullMobile.takeLast(4) else "••••"
            // স্থানীয় গোনা সাথে সাথেই বাড়ানো হয় (ক্লাউড-লেখা ব্যর্থ হলেও
            // অন্তত এই ফোনে "আমি চাপলাম" এটা হারায় না)।
            // 🔴🔴🔴🔒 B515 (06.08.2026, TK-এর Android Studio-তে ধরা পড়া
            // বিল্ড-এরর — "Type mismatch: inferred type is String? but
            // String was expected") — `expectedCode(context)` নাল-হতে-পারা
            // (`String?`), কিন্তু `bumpLocalCallTapCount()`-এর `staffCode`
            // প্যারামিটার নন-নাল `String` চায় — সরাসরি পাঠানোয় কম্পাইল
            // ব্যর্থ হতো। এখন নাল হলে (`expectedCode` জানা না থাকলে)
            // স্থানীয় গণনাটাই বাদ দেওয়া হয় (null-check দিয়ে) — বাকি
            // সব যুক্তি অক্ষত।
            try {
                val staffCodeNow = expectedCode(context)
                if (staffCodeNow != null) {
                    // 🔴 V592 — এখানেও ভারতীয় সময়, নইলে ফোনের ঘড়ি অন্য টাইমজোনে
                    //    থাকলে জমা-গোনা আর পড়া-গোনা আলাদা দিনে পড়ে যেত
                    //    (পড়ার দিকে `todayIso()` আগে থেকেই Asia/Kolkata)।
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
                        .format(java.util.Date())
                    bumpLocalCallTapCount(context, staffCodeNow, today)
                }
            } catch (_: Throwable) { }
            // 🔴🔒 V1338 — id **এখানে, মূল থ্রেডেই** তৈরি ও SharedPreferences-এ
            // সেভ হয় (নেট পাঠানোর চেষ্টার আগেই) — অ্যাপ ব্যাকগ্রাউন্ডে যাওয়ার
            // পরপরই প্রসেস মারা গেলেও এই তথ্যটুকু হারায় না।
            // ⛔ `personCode` (লগইন-থ্রেডে বসে) এখানে নির্ভরযোগ্য নাও হতে পারে —
            // `expectedCode()`-ই ব্যবহার করা হচ্ছে, ঠিক যেভাবে উপরের local-count
            // বাড়ানোর সময় হয় (নেট ছাড়াই, session থেকে সরাসরি জানা যায়)।
            val tapId = java.util.UUID.randomUUID().toString()
            val staffForTap = try { expectedCode(context) } catch (_: Throwable) { null }
            val pendingRow: JSONObject? = if (staffForTap != null) {
                JSONObject().put("id", tapId).put("staff_code", staffForTap).put("target_mobile_mask", masked)
                    .also { if (fullMobile.isNotBlank()) it.put("target_mobile", fullMobile) }
            } else null
            if (pendingRow != null) { try { savePendingCallTap(context, tapId, pendingRow) } catch (_: Throwable) { } }
            Thread {
                try {
                    val expected = expectedCode(context)
                    if (isSignedIn && personCode != expected) signOut()
                    if (!isSignedIn) { try { signInCurrentSession(context) } catch (_: Throwable) {} }
                    if (isSignedIn) {
                        /* 🔵🔒 V592 (২৩.০৮.২০২৬) — **`call_date` এখান থেকে পাঠানো
                           হয় না, ইচ্ছে করেই।**

                           প্রথমে ভেবেছিলাম না-পাঠানোটাই গোনা-০-এর একটা কারণ, কিন্তু
                           TK ডেটাবেসে চালিয়ে দেখালেন — সত্যি নয়। ঘরটার ডিফল্ট
                           আগে থেকেই ঠিক আছে, আর ভারতীয় সময়েই:
                               call_date  date  DEFAULT ((now() AT TIME ZONE
                                                'Asia/Kolkata'::text))::date  NOT NULL
                           ৭২৩টা সারির একটারও তারিখ ফাঁকা নয় (যাচাই করা)।

                           ⇒ তাই তারিখটা **সার্ভারই** বসাবে, ফোন নয়। কারণ সার্ভারের
                             ঘড়ি সবসময় ঠিক, কিন্তু কোনো স্টাফের ফোনের ঘড়ি/টাইমজোন
                             ভুল থাকলে ফোন থেকে পাঠানো তারিখ ভুল দিনে বসিয়ে দিত —
                             অর্থাৎ যা ঠিক আছে তাকে খারাপ করা হত।
                           ⚠️ ওয়েব (`module_core.js`) নিজে থেকে তারিখ পাঠায়, কিন্তু
                              সেটা একই ডিফল্টের সঙ্গেই মেলে, তাই কোনো অমিল হয় না। */
                        if (pendingRow != null) {
                            // 🔴🔒 V1338 — plain insert()-এর বদলে id-ধরা upsertOnConflict:
                            // ব্যর্থ হলে pending-এ থেকেই যায় (পরে flushPendingCallTaps
                            // আবার চেষ্টা করবে); সফল হলে সঙ্গে সঙ্গে pending থেকে মুছে যায়।
                            if (upsertOnConflict("wn", "call_taps", pendingRow, "id")) {
                                clearPendingCallTap(context, tapId)
                            }
                        } else {
                            // ⛔ খুব বিরল edge-case (তখনো session/staffCode জানাই ছিল না)
                            // — আগের মতোই একবার সরাসরি insert, pending-queue ছাড়া।
                            val row = JSONObject()
                                .put("staff_code", personCode)
                                .put("target_mobile_mask", masked)
                            if (fullMobile.isNotBlank()) row.put("target_mobile", fullMobile)
                            insert("wn", "call_taps", row)
                        }
                    }
                } catch (_: Throwable) { /* logging must never affect the call */ }
            }.start()
        } catch (e: Exception) { /* logging must never affect the call */ }
    }
}
