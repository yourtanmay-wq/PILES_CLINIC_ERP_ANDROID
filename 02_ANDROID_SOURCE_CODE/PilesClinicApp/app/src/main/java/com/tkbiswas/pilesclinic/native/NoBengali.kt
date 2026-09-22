package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.app.Dialog
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * 🔒🔒 Kishanganj Staff / Branch-number login-এ বাংলা লেখা একেবারে বন্ধ রাখার ব্যবস্থা।
 * ==========================================================================
 * **খাতার সারি B158** · TK-এর নির্দেশ (30.07.2026 সকাল ১১.১০):
 *
 * > *"কিশানগঞ্জের স্টাফ KNE-KISHAN5, মোবাইল +916207841890 — সে একদম বাংলা
 * >  বোঝে না এবং পড়তেও জানে না। সুতরাং তার বক্তব্য: স্যার আমি লগইন করার পরে
 * >  আমার ফোনে যেন কোন বাংলা ফন্ট না থাকে। প্রয়োজনে শুধু ইংলিশ, অথবা তার
 * >  সাথে হিন্দি — কিন্তু বাংলা একদম যেন না থাকে।"*
 *
 * **কীভাবে কাজ করে (তিন স্তরের সুরক্ষা):**
 *  ১. **Branch rule** — Kishanganj branch-এর Staff account লগইন করলেই চালু হয়;
 *     KNE-BRANCH (branch-number login)-ও Staff account, তাই একই নিয়মে পড়ে।
 *     পুরনো KNE-KISHAN5 mobile/code fallback নিরাপত্তা হিসেবে রাখা আছে।
 *     Doctor/Master/Field এবং অন্য branch-এর Staff-এর জন্য কিছুই বদলায় না।
 *  ২. **অনুবাদ** — পর্দার প্রতিটা বাংলা টুকরোর ইংরেজি নিচের তালিকায় আছে।
 *     লম্বা টুকরো আগে বদলানো হয়, তাই ছোট টুকরো বড়টার ভিতরে ঢুকে গোলমাল করে না।
 *  ৩. **শেষ জাল** — বদলানোর পরেও যদি একটাও বাংলা অক্ষর থেকে যায় (যেমন কেউ
 *     ভবিষ্যতে নতুন বাংলা লেখা যোগ করল), সেটা **মুছে দেওয়া হয়** — অর্থাৎ ওই
 *     স্টাফের পর্দায় বাংলা অক্ষর কোনোভাবেই আসতে পারে না। পাহারাদারের যাচাই
 *     ৯.১৪ আবার নিশ্চিত করে যে নতুন কোনো বাংলা লেখা অনুবাদ ছাড়া না থাকে।
 *
 * ⛔ **রোগীর কাছে যাওয়া লেখা ইচ্ছে করে ছোঁয়া হয়নি** — `PatientMessage.kt`,
 *    `DoctorMessage.kt`, `ClinicalRepository.kt` (ডায়েট/পরামর্শ), ছাপার লেখা।
 *    কারণ ওগুলো **রোগীর নিজের ভাষা**, TK-এর লক করা তিন-ভাষার নিয়ম (সারি B17)।
 *    ওই লেখা স্টাফের পর্দার অংশ নয় — WhatsApp/SMS-এ রোগীর কাছে যায়।
 * ⛔ **কোনো ডিজাইন · টাকার হিসাব · ব্রাঞ্চের নিয়ম · ডেটাবেস ছোঁয়া হয়নি।**
 */
object NoBengali {

    /** যাদের ফোনে বাংলা একেবারে বন্ধ — মোবাইলের শেষ ১০ অঙ্ক।
     *  ⛔ TK-এর অনুমতি ছাড়া এই তালিকা বদলানো যাবে না; নতুন কেউ যোগ করতে হলে
     *     TK বলবেন, তখন শুধু একটা নম্বর যোগ হবে। */
    private val NO_BENGALI_MOBILES = setOf(
        "6207841890",  // KNE-KISHAN5 (Kishanganj) — কাজ ছেড়ে দিয়েছেন (V453),
                       // সারিটা নিরীহ, তাই তোলা হয়নি
        // 🟢🔒 V735 (২৭.০৮.২০২৬, TK-এর সরাসরি নির্দেশ) — TK: *"বাংলা পড়তে পারে
        //    না"*। কিশানগঞ্জের নতুন স্টাফ KNE-KISHAN8 — তাই ওঁর পর্দাতেও বাংলা
        //    বন্ধ, ঠিক আগের নিয়মেই।
        "7321960416"   // KNE-KISHAN8 (Kishanganj)
    )

    /** অ্যাকাউন্টের কোড ধরেও মেলানো হয় — নম্বর কখনো বদলে গেলেও যেন কাজ চলে। */
    private val NO_BENGALI_CODES = setOf(
        "KNE-KISHAN5",
        "KNE-KISHAN8"   // 🟢 V735 — উপরের একই কারণ
    )

    @Volatile private var activeCache: Boolean = false

    /** কোন পর্দা/পপ-আপে পাহারা বসানো হয়ে গেছে — একবারই বসে।
     *  `WeakHashMap` বলে বন্ধ হয়ে যাওয়া পর্দা নিজে থেকেই ছেড়ে যায় (মেমরি ধরে
     *  রাখে না), আর কারও বসানো view-tag-এ হাত পড়ে না। */
    private val hooked = java.util.WeakHashMap<View, Boolean>()

    /** লগইন/পর্দা খোলার সময় ডাকা হয় — কে লগইন করা আছে দেখে নেয়। */
    fun refresh(context: Context?) {
        activeCache = try {
            val u = context?.let { NativeSession.current(it) }
            if (u == null) false else {
                val ten = u.mobile.filter { it.isDigit() }.takeLast(10)
                val account = try { StaffDirectory.findAccount(u.mobile) } catch (_: Throwable) { null }
                val code = account?.name ?: ""

                // 🔒 V451 (19.08.2026, TK-approved): Kishanganj-এর বাংলা-বন্ধ
                // নিয়ম আর কোনো নির্দিষ্ট ব্যক্তির নম্বরের ওপর নির্ভর করবে না।
                // Kishanganj branch-এর যেকোনো Staff account — এর মধ্যে KNE-BRANCH
                // (branch-number login)-ও পড়ে — English-only display পাবে।
                // ⛔ Doctor/Master/Field বা অন্য branch-এর Staff-এর display বদলাবে না।
                val role = (account?.role ?: u.role).trim()
                val branch = (account?.branch ?: u.branch).trim()
                val isKishanganjStaff = role.equals("staff", ignoreCase = true) &&
                    branch.equals("Kishanganj", ignoreCase = true)

                isKishanganjStaff ||
                    NO_BENGALI_MOBILES.contains(ten) ||
                    NO_BENGALI_CODES.contains(code.trim().uppercase())
            }
        } catch (_: Throwable) {
            false
        }
    }

    /** এখন যিনি লগইন করা, তাঁর জন্য বাংলা বন্ধ কি না। */
    fun active(): Boolean = activeCache

    // 🔴 B413 (04.08.2026, TK-নির্দেশে "সম্পূর্ণ প্রজেক্ট স্ক্রিন-ধরে-স্ক্রিন
    // যাচাই করুন" — Login স্ক্রিন অডিটে ধরা পড়েছে): `active()`/`refresh()`
    // দুটোই `NativeSession.current()` (মানে **লগইন হয়ে যাওয়ার পরের**
    // সেশন) লাগে। কিন্তু `LoginActivity`-র "Forgot Password" বার্তা
    // **লগইন হওয়ার আগেই** (পাসওয়ার্ড দেওয়ার আগে) দেখায় — তখন কোনো সেশনই
    // নেই, তাই KNE-KISHAN5 (মোবাইল নম্বর টাইপ করার পরেও) ওই বার্তায় কাঁচা
    // বাংলা দেখতেন, B158-এর লক করা নিয়ম ("তার ফোনে যেন কোনো বাংলা ফন্ট না
    // থাকে") ভেঙে যেত। এই নতুন ফাংশন **সেশন ছাড়াই**, সরাসরি মোবাইল নম্বর
    // দিয়ে যাচাই করে — Login স্ক্রিনের মতো pre-login জায়গায় ব্যবহারের জন্য।
    fun isNoBengaliMobile(mobile: String): Boolean {
        val ten = mobile.filter { it.isDigit() }.takeLast(10)
        val account = try { StaffDirectory.findAccount(mobile) } catch (_: Throwable) { null }

        // Login-এর আগেও branch জানা যায় StaffDirectory থেকে। তাই নতুন/অন্য
        // Kishanganj Staff ও KNE-BRANCH Forgot Password-এ বাংলা দেখবে না।
        if (account != null &&
            account.role.equals("staff", ignoreCase = true) &&
            account.branch.equals("Kishanganj", ignoreCase = true)
        ) return true

        if (NO_BENGALI_MOBILES.contains(ten)) return true
        val code = account?.name ?: ""
        return NO_BENGALI_CODES.contains(code.trim().uppercase())
    }

    private fun hasBengali(s: CharSequence): Boolean {
        for (ch in s) {
            if (ch.code in 0x0980..0x09FF) return true
        }
        return false
    }

    /** সবচেয়ে লম্বা টুকরো আগে — তাই একবারই সাজানো হয়। */
    private val ordered: List<Pair<String, String>> by lazy {
        MAP.entries.sortedByDescending { it.key.length }.map { it.key to it.value }
    }

    /** সবচেয়ে লম্বা টুকরো আগে — ইংরেজির তালিকার মতোই একই নিয়ম। */
    private val hindiOrdered: List<Pair<String, String>> by lazy {
        HINDI.entries.sortedByDescending { it.key.length }.map { it.key to it.value }
    }

    /* 🟢🔒 V591 (২৩.০৮.২০২৬) — **সংখ্যা বসা ঘর**। `"ফোলা $piles টা"` ·
       `"${h}টা"` ধাঁচের লেখায় সংখ্যাটা চলার সময়ে বসে, তাই তালিকায় রাখা
       যায় না — আর তালিকায় না থাকায় শেষ-জাল "টা"-টুকু **মুছে** দিত।
         · "3 টা" → "3 nos"      (কয়টা — গোনা)
         · "3টা"  → "3 o'clock"  (ঘড়ির কাঁটা)
       🔤 V730 (TK: *"হিন্দি লিখতে হবে না, শুধু ইংরেজিতে হবে"*) — আগে এখানে
          হিন্দি "नग"/"बजे" বসত, এখন ইংরেজি বসে। মেলানোর নিয়ম অপরিবর্তিত।
       ⛔ নিরাপত্তা: শুধু **সংখ্যার পরেই** চলে, আর "টা"-র পরে আর কোনো বাংলা
          অক্ষর থাকলে চলে না। তাই "টাকা" · "টাইম" · "টানুন" · "টাটকা" —
          একটাও কখনো ছোঁয়া হয় না। */
    private val NUM_COUNT  = Regex("([0-9]+) টা(?![\\u0980-\\u09FF])")
    private val NUM_OCLOCK = Regex("([0-9]+)টা(?![\\u0980-\\u09FF])")

    /**
     * একটা লেখাকে বাংলা-মুক্ত করে দেয়। বাংলা বন্ধ না থাকলে লেখাটা **হুবহু
     * অপরিবর্তিত** ফেরত যায় — তাই বাকি সব স্টাফের কিছুই বদলায় না।
     */
    fun fix(text: CharSequence?): CharSequence? {
        if (text == null) return null
        if (!activeCache) return text
        if (!hasBengali(text)) return text
        WHOLE[text.toString().trim()]?.let { return it }
        // 🔒 শুরুর ও শেষের ফাঁকা/নতুন-লাইন হুবহু রেখে দিতে হয় (30.07.2026-এ
        // নিজের কাজ আবার যাচাই করতে গিয়ে ধরা পড়েছে): অনেক বার্তায় লেখাটা
        // `"\n\n⏰ তারিখ: …"` ধাঁচে জোড়া হয়। শুধু `trim()` করলে ওই নতুন-লাইনটা
        // চলে যেত আর দুটো লাইন এক লাইনে জুড়ে যেত — চোখে দেখতে খারাপ লাগত।
        val whole = text.toString()
        val lead = whole.takeWhile { it.isWhitespace() }
        val tail = whole.takeLastWhile { it.isWhitespace() }
        var s = whole.trim()
        // 🔵 V575 (TK-নির্দেশ, ২৩.০৮.২০২৬): *"কিষানগঞ্জ all Staff এর কাছে যেন
        //    হিন্দি তে থাকে … আমি তো আপনাকে ডাক্তার চেকাপে হিন্দিতে রাখতে বললাম"*।
        //    তাই **আগে** হিন্দির তালিকা বসে, তারপর আগের ইংরেজির তালিকা।
        //    ⛔ হিন্দির তালিকায় শুধু ডাক্তার-চেক-আপের নিজস্ব লেখা আছে, তাই
        //       অন্য পর্দার একটাও লেখা বদলায় না — আগের মতোই ইংরেজি থাকে।
        for ((bn, hi) in hindiOrdered) {
            if (s.contains(bn)) s = s.replace(bn, hi)
        }
        for ((bn, en) in ordered) {
            if (s.contains(bn)) s = s.replace(bn, en)
        }
        // 🟢 V591 — সংখ্যা বসা ঘর (উপরে NUM_COUNT · NUM_OCLOCK দেখুন)।
        if (s.contains("টা")) {
            s = NUM_COUNT.replace(s) { m -> m.groupValues[1] + " nos" }
            s = NUM_OCLOCK.replace(s) { m -> m.groupValues[1] + " o'clock" }
        }
        if (hasBengali(s)) {
            // 🔒 শেষ জাল — অনুবাদে না থাকা বাংলা অক্ষর মুছে ফেলা হয়।
            val sb = StringBuilder(s.length)
            for (ch in s) {
                if (ch.code in 0x0980..0x09FF) continue
                sb.append(ch)
            }
            s = sb.toString()
        }
        // দুটো ফাঁকা জায়গা বা ঝুলে থাকা যোগচিহ্ন পরিষ্কার করা
        // বাংলা দাঁড়ি (।) বাংলা অক্ষরের ঘরে পড়ে না, তাই আলাদা করে সরাতে হয়
        s = s.replace("\u0964", ".")
        while (s.contains("  ")) s = s.replace("  ", " ")
        s = s.replace(" ·  ", " · ").replace("· ·", "·").replace(" .", ".").trim()
        while (s.endsWith("—") || s.endsWith("·") || s.endsWith(":")) s = s.dropLast(1).trim()
        return lead + s + tail
    }

    /** String দরকার হলে (যেমন Toast-এ) — একই কাজ। */
    fun s(text: String?): String = if (text == null) "" else (fix(text)?.toString() ?: "")

    /** একটা ভিউ-গাছের সব লেখা বাংলা-মুক্ত করা। */
    fun sweep(root: View?) {
        if (!activeCache || root == null) return
        try {
            walk(root)
        } catch (_: Throwable) { }
    }

    private fun walk(v: View) {
        if (v is TextView) {
            // ⛔⛔ **লেখার ঘরে (EditText) কখনো হাত দেওয়া হয় না।**
            // কারণ (নিজের কাজ আবার যাচাই করতে গিয়ে ধরা পড়েছে, 30.07.2026):
            // পুরনো কোনো রেকর্ডের Remark/নাম যদি বাংলায় লেখা থাকে, আর স্টাফ
            // সেটা খুলে Save চাপেন, তাহলে ঘরের বদলে যাওয়া লেখাটাই ডেটাবেসে
            // চলে যেত — অর্থাৎ **রোগীর আসল বাংলা তথ্য চিরতরে নষ্ট** হয়ে যেত।
            // তাই ইনপুট ঘরের **text ছোঁয়া হয় না**; শুধু hint (যেটা কোথাও
            // সেভ হয় না) বাংলা-মুক্ত করা হয়।
            val isInput = v is android.widget.EditText
            if (!isInput) {
                val t = v.text
                if (t != null && hasBengali(t)) {
                    val fixed = fix(t)
                    if (fixed != null && fixed.toString() != t.toString()) v.text = fixed
                }
            }
            val h = v.hint
            if (h != null && hasBengali(h)) {
                val fixed = fix(h)
                if (fixed != null && fixed.toString() != h.toString()) v.hint = fixed
            }
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
    }

    /** পর্দা (Activity) — প্রতিটা layout-এর পরে আবার মিলিয়ে দেখা হয়, তাই পরে
     *  বসানো লেখাও ধরা পড়ে (তালিকার সারি, টোস্ট ছাড়া সবকিছু)। */
    fun install(activity: Activity?) {
        if (!activeCache || activity == null) return
        try {
            val decor = activity.window?.decorView ?: return
            if (hooked.containsKey(decor)) {
                sweep(decor); return
            }
            hooked[decor] = true
            decor.viewTreeObserver.addOnGlobalLayoutListener { sweep(decor) }
            sweep(decor)
        } catch (_: Throwable) { }
    }

    /** পপ-আপ (Dialog) — এদের নিজের আলাদা উইন্ডো, তাই আলাদা করে দেখা হয়। */
    fun installDialog(dialog: Dialog?) {
        if (!activeCache || dialog == null) return
        try {
            val decor = dialog.window?.decorView ?: return
            if (!hooked.containsKey(decor)) {
                hooked[decor] = true
                decor.viewTreeObserver.addOnGlobalLayoutListener { sweep(decor) }
            }
            sweep(decor)
        } catch (_: Throwable) { }
    }

    /** পুরো অ্যাপে একবারই বসানো হয় (`PilesClinicApplication`) — তাই ৩৪টা
     *  পর্দার একটাতেও আলাদা করে কিছু লিখতে হয় না, আর ভবিষ্যতের নতুন পর্দাও
     *  নিজে থেকেই ঢেকে যায়। */
    fun hookApp(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) { refresh(a); install(a) }
            override fun onActivityStarted(a: Activity) { refresh(a); install(a) }
            override fun onActivityResumed(a: Activity) { refresh(a); install(a) }
            override fun onActivityPaused(a: Activity) { }
            override fun onActivityStopped(a: Activity) { }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) { }
            override fun onActivityDestroyed(a: Activity) { }
        })
    }


    /** 🔒 কয়েকটা লেখার হুবহু ইংরেজি — টুকরো ধরে বদলালে বাক্যের ক্রম উল্টো
     *  শোনাত (যেমন "Name দিন" → "Name day"), তাই এগুলো **আগে** মেলানো হয়।
     *  ⛔ TK-এর অনুমতি ছাড়া বদলানো যাবে না। */
    private val WHOLE: Map<String, String> = mapOf(
        /* 🔴🔒 V731 (২৭.০৮.২০২৬) — **নিজের কাজ যাচাই করতে গিয়ে ধরা পড়ল**
           (TK: *"সমস্ত কাজ যাচাই করে দেখুন কোথায় ভুল করেছেন"*)।
           দুটো লেখা "English + বাংলা" ধাঁচে লেখা, আর বাংলা অংশটার ইংরেজি
           **একই শব্দ**। তাই বাংলা-বন্ধ স্টাফের পর্দায় লেখাটা **দুবার** উঠত:
             · IncomeExpenseActivity → "Expense / Expense"
             · PaymentActivity       → "Reason (reason)"
           ⛔ এটা V730-এর কাজে হয়নি — **আগে থেকেই ছিল** (পুরনো হিন্দি নিয়মেও
              হুবহু একই ভুল উঠত, মিলিয়ে দেখা হয়েছে)।
           ⛔ WHOLE-এ রাখা হলো, MAP-এ নয় — WHOLE **গোটা লেখাটা হুবহু** মেলায়,
              তাই অন্য কোনো পর্দার একটা অক্ষরও ছোঁয়ার পথ নেই (সংঘর্ষ ০)। */
        // 🪟 V845 (৩০.০৮.২০২৬) — কল-স্ক্রিনের উপরের ভাসমান কার্ড ও
        // তার অনুমতি চাওয়ার পপ-আপের লেখা।
        "গত রিমার্ক" to "Last remark",
        "কল এলে রোগীর তথ্য পর্দায় দেখতে" to "See patient details when a call comes",
        "কল-স্ক্রিনের উপরে রোগীর তথ্য দেখাতে একটা অনুমতি লাগে" to
            "To show patient details over the call screen, one permission is needed",
        "পরের পর্দায় এই অ্যাপটা বেছে চালু করে দিন" to
            "On the next screen, pick this app and switch it on",
        "না দিলেও কিছু নষ্ট হবে না — আগের মতোই নোটিফিকেশন আসবে।" to
            "Nothing breaks if you skip it - the notification will come as before.",
        "চালু করি" to "Turn on",
        "এখন নয়" to "Not now",
        "Expense / ব্যায়" to "Expense",
        "Reason (কারণ)" to "Reason",
        /* 🟢🔒 V591 (২৩.০৮.২০২৬) — পরামর্শের এই লাইনগুলো **একাই সম্পূর্ণ**
           (কোথাও জোড়া লাগে না), তাই এখানে — বাংলা লাইনটা বাদ দিয়ে ইংরেজি ও
           হিন্দি দুটোই থাকে। ⛔ WHOLE সবার আগে ও হুবহু পুরো লেখা ধরে মেলায়। */
        "-এর নামে আছে।" to "'s name.\n\n",
        "(রোগীর ছবিসহ)। CSV কপি ওই একই Download থেকে তৈরি হবে।" to "(with patient photos). The CSV copy is made from that same download.\n\n",
        "Curd / Buttermilk\nদই বা ঘোল খান\nदही या छाछ लें" to "Curd / Buttermilk",
        "High fiber food\nআঁশযুক্ত খাবার খান\nफाइबरयुक्त भोजन करें" to "High fiber food",
        "Regular walking\nনিয়মিত হাঁটাহাঁটি করুন\nनियमित सैर करें" to "Regular walking",
        "Whole grains\nগোটা শস্য জাতীয় খাবার খান\nसाबुत अनाज खाएं" to "Whole grains",
        "Avoid constipation\nকোষ্ঠকাঠিন্য এড়িয়ে চলুন\nकब्ज़ से बचें" to "Avoid constipation",
        "Drink sufficient water\nপর্যাপ্ত জল পান করুন\nपर्याप्त पानी पिएं" to "Drink sufficient water",
        "Warm sitz bath\nগরম জলে সিটজ বাথ নিন\nगर्म पानी से सिट्ज़ बाथ लें" to "Warm sitz bath",
        "Adequate rest & sleep\nপর্যাপ্ত বিশ্রাম ও ঘুম\nपर्याप्त आराम और नींद लें" to "Adequate rest & sleep",
        "Fresh fruits & vegetables\nটাটকা ফল ও সবজি খান\nताज़े फल और सब्जियां खाएं" to "Fresh fruits & vegetables",
        "Avoid alcohol & smoking\nমদ্যপান ও ধূমপান এড়িয়ে চলুন\nशराब और धूम्रपान से बचें" to "Avoid alcohol & smoking",
        "Avoid prolonged sitting\nদীর্ঘক্ষণ বসে থাকা এড়িয়ে চলুন\nलंबे समय तक बैठने से बचें" to "Avoid prolonged sitting",
        "Avoid spicy/oily food\nঝাল ও তেলযুক্ত খাবার এড়িয়ে চলুন\nमसालेदार और तैलीय भोजन से बचें" to "Avoid spicy/oily food",
        "Avoid red meat / heavy non-veg\nলাল মাংস/ভারী আমিষ এড়িয়ে চলুন\nलाल मांस/भारी मांसाहार से बचें" to "Avoid red meat / heavy non-veg",
        "Avoid straining during bowel movement\nমলত্যাগের সময় জোর করবেন না\nमल त्याग के समय ज़ोर न लगाएं" to "Avoid straining during bowel movement",
        // 🔒 B568 (08.08.2026): Patient Timeline-এর নীল-কার্ড Note-এর সেকশন হেডার।
        // শুধু বাংলা-বন্ধ (Kishanganj) Staff-এর display translation; data/logic ছোঁয়া হয়নি।
        "📌 রিমার্ক" to "📌 Remark",
        "🧾 স্ট্যাটাস" to "🧾 Status",
        "🩺 ডাক্তার চেক-আপ" to "🩺 Doctor Check-up",
        "💰 পেমেন্ট" to "💰 Payment",
        // 🔒 B527 (07.08.2026, TK-এর অনুমোদন): Guard 9.14-এ ধরা পড়া
        // 07.08-এর নতুন failure/save বার্তাগুলোর English equivalent। শুধু
        // বাংলা-বন্ধ Staff-এর display translation; save/data/payment logic ছোঁয়া হয়নি।
        "লোড করা গেল না — একটু পরে আবার দেখুন" to "Could not load — please try again shortly",
        "⚠️ আগের Paid টাকা এখন লোড হয়নি — Paid/Due ভুল দেখাতে পারে। একটু পরে আবার খুলে দেখুন।" to "⚠️ Previous Paid amount could not be loaded — Paid/Due may be incorrect. Please reopen shortly.",
        "সেভ হয়নি — একটু পরে আবার Submit চাপুন" to "Not saved — please tap Submit again shortly",
        // 🆕 B438 (05.08.2026) — Work Notebook-এর নতুন multi-option quick-mark
        // ডায়ালগ (IN/OUT TIME রিমাইন্ডার থেকে খোলে); KNE-KISHAN5-এর ফোনে
        // ইংরেজি। ⛔ ইমোজি-প্রিফিক্স অক্ষত রাখা হলো (কোডে .startsWith() দিয়ে
        // চেনা হয়)।
        "আপনি কি চেম্বারে পৌঁছেছেন?" to "Have you reached the chamber?",
        "আপনি বাড়ি যেতে চান না" to "Don't you want to go home?",
        "✅ হ্যাঁ, এখনই IN TIME মার্ক করুন" to "✅ Yes, mark IN TIME now",
        "⏰ এখনো আসিনি — কখন আসব বলছি" to "⏰ Not there yet — tell when I'll arrive",
        // 🆕 (06.08.2026, B519) — চেম্বার-রিমাইন্ডার পুশ-নোটিফিকেশনের নতুন
        // ছোট বাংলা লেখা।
        "⏰ চেম্বারে পৌঁছেছেন?" to "⏰ Have you reached the chamber?",
        "চাপুন — IN TIME বসান।" to "Tap — mark IN TIME.",
        "⏰ চেম্বার থেকে বেরিয়েছেন?" to "⏰ Have you left the chamber?",
        "চাপুন — OUT TIME বসান।" to "Tap — mark OUT TIME.",
        // 🆕 (06.08.2026, B520) — Notes বক্সের নতুন হিন্ট-টেক্সট।
        "আজকের সারাদিনে কি কি কাজ করেছেন লিখুন (ঐচ্ছিক)..." to "What did you do today? (optional)",
        // 🆕 (06.08.2026, B523) — My Reports কার্ড-ডিজাইনের বাংলা লেখা
        // (ইতিমধ্যে isKishanganjStaff দিয়ে কোডে branch করা আছে, এখানে
        // পাহারাদারের জন্য বাড়তি নিরাপত্তা)।
        " — দৈনিক রিপোর্ট" to " — Daily Report",
        "— দৈনিক রিপোর্ট" to "— Daily Report",
        "N বার আপডেট হয়েছে (সর্বশেষ পাঠানো)" to "Updated N times (latest sent)",
        "একবার পাঠানো হয়েছে" to "Sent once",
        "জমা হয়েছে" to "Submitted",
        "🏖️ আজকে আমার ছুটি" to "🏖️ Today is my leave",
        "এই ফোনে ব্রাঞ্চের নম্বর কোন SIM?" to "Which SIM is the branch number on this phone?",
        "কতদিন থেকে সমস্যা" to "How long has the problem been there",
        "যেমন: 6 মাস / 2 বছর" to "e.g. 6 months / 2 years",
        "আগে কোথাও চিকিৎসা নিয়েছিলেন কিনা" to "Any treatment taken elsewhere before",
        "কোথায়/কী চিকিৎসা নিয়েছিলেন লিখুন, না নিয়ে থাকলে ফাঁকা রাখুন" to "Write where/what treatment was taken; leave blank if none",
        "✅ হ্যাঁ, এখনই OUT TIME মার্ক করুন" to "✅ Yes, mark OUT TIME now",
        "⏰ এখনো যাব না — কখন যাব বলছি" to "⏰ Not leaving yet — tell when I'll leave",
        "কিছু ঘর ফাঁকা আছে" to "Some fields are empty",
        // 🆕 (06.08.2026, B512) — প্রফেশনাল কার্ড-ডিজাইনে বুলেট (•) সরানো।
        "⚠️ কিছু ঘর ফাঁকা আছে" to "⚠️ Some fields are empty",
        "আজকে কতগুলি ফোন রিসিভ করেছেন — লেখা হয়নি" to "Today's calls received — not filled in",
        "Notes — কিছু লেখা হয়নি" to "Notes — nothing written",
        "অথবা নিজের কারণ লিখুন" to "Or write your own reason",
        "ভরে OUT TIME বসান" to "Fill & mark OUT TIME",
        "এড়িয়ে যান" to "Skip",
        "• আজকে কতগুলি ফোন রিসিভ করেছেন — লেখা হয়নি" to "• Today's calls received — not filled in",
        "• Notes — কিছু লেখা হয়নি" to "• Notes — nothing written",
        // 🆕 B419 (04.08.2026) — Chamber Reopen request feature-এর স্ট্যাটিক
        // (কোনো ভ্যারিয়েবল-বসানো নেই) বাক্যগুলো; KNE-KISHAN5-এর ফোনে ইংরেজি।
        "হ্যাঁ, পাঠান" to "Yes, Send",
        "অনুরোধ পাঠানো হয়েছে — Master অনুমোদন দিলে দিনটা আবার খুলবে" to "Request sent — the day will reopen once Master approves",
        "পাঠানো গেল না — নেট চেক করে আবার চেষ্টা করুন" to "Could not send — check your network and try again",
        // 🆕 V1609 (১৯.০৯.২০২৬) — একই দিনের জন্য বারবার Reopen-অনুরোধ আটকানোর সতর্কবার্তা।
        "Already requested — Master এখনো এটা দেখেননি, আবার পাঠানোর দরকার নেই" to "Already requested — Master has not seen it yet, no need to send again",
        "না" to "No",
        "এই অনুরোধ থেকে Branch/Date চেনা গেল না" to "Could not read Branch/Date from this request",
        // 🆕 B607 (10.08.2026, TK-নির্দেশ) — কেউ না এলে চেম্বার বন্ধের নিশ্চিতকরণ।
        "আজ কেউ আসেননি (Arrived 0)। তবুও চেম্বার বন্ধ করবেন?" to "Nobody arrived today (Arrived 0). Still close the chamber?",
        "হ্যাঁ, বন্ধ করুন" to "Yes, Close",
        // 🆕 B608 (10.08.2026, TK-নির্দেশ) — ভুল করে ছুটি দিলে বাতিলের নিশ্চিতকরণ।
        "ভুল করে ছুটি দিয়েছিলেন? ছুটি বাতিল করে আজকের হাজিরা আবার চালু করবেন?" to "Marked leave by mistake? Cancel the leave and resume today's attendance?",
        "হ্যাঁ, বাতিল করুন" to "Yes, Cancel",
        // 🆕 B615 (11.08.2026, TK-নির্দেশ) — দুপুর ১২টা পার হলে IN TIME বন্ধ।
        "আজকের IN TIME-এর সময় শেষ, না এলে ছুটি দিন" to "IN TIME window is over for today. Mark leave if absent.",
        // 🆕 B618 (11.08.2026, TK-নির্দেশ) — ছুটির আবেদন (আজ/অগ্রিম + মঞ্জুরি)।
        "🏖️ ছুটির আবেদন" to "🏖️ Leave Request",
        "কোন তারিখে ছুটি" to "Which date for leave",
        // 🏖️ V1336 (১১.০৯.২০২৬) — একাধিক দিনের ছুটি আবেদনে "From"/"To" লেবেল।
        "From (কোন তারিখ থেকে ছুটি)" to "From (leave starts)",
        "To (একদিনের ছুটি হলে From-এর মতোই রাখুন)" to "To (keep same as From for a single day)",
        "ছুটির আবেদন করুন" to "Apply for Leave",
        "ছুটির অনুরোধ পাঠানো হয়েছে — Pending" to "Leave request sent — Pending",
        // 🆕 B419 (04.08.2026) — ChamberAttendanceActivity-র নতুন "Reopen"
        // অনুরোধ-বোতাম; KNE-KISHAN5-এর ফোনে ইংরেজি।
        "🔓 Reopen-এর অনুরোধ পাঠান (Master-এর অনুমতি লাগবে)" to "🔓 Request Reopen (needs Master's approval)",
        // 🔴 V425 (TK-নির্দেশ ১৭.০৮.২০২৬: *"আমি মাস্টার আবার আমাকে কেন অনুমতি নিতে হবে"*)
        "🔓 চেম্বার আবার খুলুন" to "🔓 Reopen Chamber",
        "এই দিনের চেম্বার আবার খুলবেন?" to "Reopen this day's chamber?",
        "হ্যাঁ, খুলুন" to "Yes, reopen",
        "চেম্বার আবার খোলা হলো" to "Chamber reopened",
        "খোলা গেল না — নেট চেক করে আবার চেষ্টা করুন" to "Could not reopen — check the network and try again",
        // 🔴 B407 (04.08.2026) — WorkNotebookActivity.kt-এর "Outside Calls
        // Today" লেবেল এখন সাধারণত বাংলা, কিশানগঞ্জ ব্রাঞ্চে হিন্দি (কোডেই
        // branch-চেক দিয়ে ঠিক হয়) — এই এন্ট্রি শুধু বাড়তি সুরক্ষা-জাল।
        "আজকে কতগুলি ফোন আপনি রিসিভ করেছেন" to "How many calls did you receive today",
        // 🔴 B405 (04.08.2026) — WorkNotebookActivity.kt-এর "Mark as Leave"
        // বোতামের পাশে নতুন বাংলা ব্যাখ্যা-লাইন; KNE-KISHAN5-এর ফোনে ইংরেজি।
        "(আজকে আমার ছুটি — leave নেওয়ার জন্য চাপুন)" to "(Tap to mark today as leave)",
        // 🔒 ভাষা-বাছাইয়ের বোতাম: বাংলা বন্ধ থাকা স্টাফের কাছে যেন
        //    "English (Bengali)" গোছের উদ্ভট লেখা না দেখায় (30.07.2026-এ
        //    নিজের কাজ আবার যাচাই করতে গিয়ে ধরা পড়েছে)।
        "বাংলা  (Bengali)" to "Bengali",
        "বাংলা (Bengali)" to "Bengali",
        "Name দিন" to "Enter Name",
        "Remarks দিন" to "Enter Remarks",
        "Title দিন" to "Enter Title",
        "Message দিন" to "Enter Message",
        "Product দিন" to "Enter Product",
        "Doctor Name দিন" to "Enter Doctor Name",
        "Doctor নাম দিন" to "Enter Doctor Name",
        "Patient নাম দিন" to "Enter Patient Name",
        "সঠিক Bill দিন" to "Enter a correct Bill",
        "সঠিক Deposit দিন" to "Enter a correct Deposit",
        "সঠিক Amount দিন" to "Enter a correct Amount",
        "সঠিক 10 ডিজিট মোবাইল দিন" to "Enter a correct 10-digit mobile number",
        "Branch বাছুন" to "Select Branch",
        "তারিখ বাছুন" to "Pick a date",
        "Registration করুন" to "Do Registration",
        "এখন পরের Follow-up Call তারিখ দিন" to "Now enter the next Follow-up Call date",
        "পাইলস (অর্শ)" to "Piles",
        "ভগন্দর (ফিস্টুলা)" to "Fistula",
        "হাইড্রোসিল (একশিরা)" to "Hydrocele",
        "Approved — Deleted (Trash Bin-এ আছে)" to "Approved — Deleted (in Trash Bin)",
        "📋 সম্পূর্ণ ইতিহাস (Full History)" to "📋 Full History",
        "কোন পেমেন্ট Edit করবেন?" to "Which payment do you want to Edit?",
        "Complete Approve করবেন?" to "Approve Complete?",
        "Complete করার অনুরোধ পাঠাবেন?" to "Send the request to Complete?",
        "Delete request পাঠাবেন?" to "Send the Delete request?",
        "উপরে ব্রাঞ্চ বাছাই করুন RMP লিস্ট দেখতে" to "Select a branch above to see the RMP list",
        "🌿 উপসর্গ · Symptoms" to "🌿 Symptoms",
        "ℹ️ কারণ · Causes" to "ℹ️ Causes",
        "🛡️ সেরা চিকিৎসা · Best Treatment" to "🛡️ Best Treatment",
        "রোগের বিস্তারিত · Disease Info" to "Disease Info",
        "বিস্তারিত দেখুন › Learn more" to "Learn more ›",
        "এসেছেন Arrived" to "Arrived",
        "আসার কথা Expected" to "Expected",
        "আজকের তথ্য এখন আনা গেল না। OUT TIME নিরাপদে বসাতে আজকের তথ্যটা দরকার (নইলে আগের IN TIME মুছে যেতে পারত)। একবার আবার চেষ্টা করুন।" to "Today's information could not be loaded. It is required to save OUT TIME safely; otherwise the previous IN TIME could be lost. Please try again.",
        "থানা" to "PS",
        // ══════════════════════════════════════════════════════════════════
        // 🔴🔴🔒 V512 (২১.০৮.২০২৬) — **আগে এখানে লেখা ছিল `"বাংলা" to "English"`,
        //    সেটা ভুল ছিল।**
        //
        // ─── কারণ (প্রমাণ করে দেখা, আন্দাজে নয়) ──────────────────────────
        //   `res/layout/activity_disease_detail.xml`-এর ৪৩ নম্বর লাইনে ভাষা
        //   বাছার তিনটে বোতাম পাশাপাশি: **বাংলা · हिन्दी · English**।
        //   প্রথম বোতামটার লেখা হুবহু "বাংলা", তাই WHOLE-এর এই সারিটাই
        //   মিলত ⇒ বাংলা-বন্ধ স্টাফের পর্দায় বসত **English · हिन्दी ·
        //   English** — একই নামে দুটো বোতাম, আর "English"-এ চাপ দিলে
        //   বাংলা লেখা খুলত। ভাষা বাছাই কার্যত ভেঙে পড়েছিল।
        //   (একই চাবি MAP-এর ১০১০ নম্বর লাইনে ঠিকই "Bengali" ছিল — অর্থাৎ
        //    দুই জায়গায় দুই মানে, তাই লেখাটা কোথায় বসেছে তার উপর ফল
        //    বদলে যেত।)
        //
        // ⇒ এখন দুই জায়গাতেই এক — "Bengali"। বোতাম তিনটে হয়:
        //   **Bengali · हिन्दी · English**।
        // ⛔ কোনো সারি মোছা হয়নি (TK-এর নিয়ম), শুধু ভুল ইংরেজিটা ঠিক হলো।
        // ⛔ এটা **শুধু দেখানোর লেখা** — কোন ভাষার লেখা দেখাবে সেই কাজটা
        //    বোতামের `id` (`langBn`) ধরে হয়, লেখা ধরে নয়। তাই ভিতরের
        //    কাজে এক অক্ষরও বদলায়নি।
        // ⛔ বাংলা-বন্ধ **ছাড়া** সব স্টাফের কিছুই বদলায়নি (`fix()` তখন
        //    লেখাটা হুবহু ফেরত দেয়)।
        // ══════════════════════════════════════════════════════════════════
        "বাংলা" to "Bengali",
        // 🔴 V512 — ভাষা বাছার তালিকায় (DoctorVisitActivity) লেখাটা
        //    "বাংলা  (Bengali)" ধাঁচে থাকে। টুকরো ধরে বদলালে হতো
        //    "Bengali  (Bengali)" — তাই হুবহু-মিলের সারি। ওয়েব অ্যাপে
        //    (app.js — WLV1_NOBN_WHOLE) ঠিক এই দুটো সারিই আগে থেকেই আছে,
        //    এখন Android-ও এক হলো।
        "বাংলা  (Bengali)" to "Bengali",
        "বাংলা (Bengali)" to "Bengali",
        // 🔴 V512 — clinical/PrescriptionActivity.kt-এর দুটো পপ-আপের লেখা।
        //    এগুলো `native/`+`modules/`-এর বাইরে বলে পাহারাদার ৯.১৪ কখনো
        //    দেখেনি, আর পপ-আপ দুটো PremiumAlert দিয়ে আঁকা হয় না বলে
        //    বাংলা-বন্ধ স্টাফের পর্দাতেও বাংলাতেই থাকত।
        "এই রোগীর আজ একটি Prescription সেভ হয়েছে। আপনি কি আবার Prescription করতে চান?" to "A Prescription has already been saved for this patient today. Do you want to make another one?",
        "Prescription যাচাই করা যায়নি" to "Prescription could not be verified",
        "ইন্টারনেট সংযোগ পরীক্ষা করে আবার Save করুন। কোনো Prescription সেভ হয়নি।" to "Check the internet connection and Save again. No Prescription was saved.",
        // 🏍️🔒 V1471 (১৪.০৯.২০২৬) — WorkNotebookActivity.kt-এর নতুন ব্যাটারি-
        // সেটিংস পপ-আপ (RUPAM/ARMAN-এর কিলোমিটার যেন বাদ না পড়ে)।
        "কিলোমিটার যেন কখনো বাদ না পড়ে" to "So the km is never missed",
        "ফোনের নিজস্ব ব্যাটারি-সেভার মাঝেমধ্যে লোকেশন-সেবা বন্ধ করে দিতে পারে, তখন কিলোমিটার আর গোনা হয় না।" to "The phone's own battery saver can sometimes stop the location service — then the km stops counting.",
        "এখন এই অ্যাপের জন্য ফোনের Settings → Battery-তে গিয়ে \"No restriction\"/\"Don't optimize\" বেছে দিন, আর থাকলে Autostart-ও চালু করে দিন — এতে সারাদিনের কিলোমিটার সঠিক থাকবে।" to "Now go to phone Settings → Battery for this app and choose \"No restriction\"/\"Don't optimize\", and turn on Autostart too if it exists — this keeps the whole day's km correct.",
        "Settings খুলি" to "Open Settings",
    )

    /** পর্দার বাংলা লেখা → ইংরেজি।
     *  ⛔ TK-এর অনুমতি ছাড়া কোনো সারি মোছা যাবে না; নতুন বাংলা লেখা যোগ হলে
     *     এখানে তার ইংরেজিও যোগ করতে হবে (পাহারাদারের যাচাই ৯.১৪ ধরবে)। */
    private val MAP: Map<String, String> = mapOf(

        // 🎤🔒 V1415 (১৩.০৯.২০২৬) — Search-এর ভয়েস/টাইপ-প্রশ্ন চেনার শব্দ-ছাঁচ
        // (VoiceReportModel.kt)। এগুলো পর্দায় দেখানো হয় না (ফেরত-আসা label
        // সবসময় ইংরেজি) — শুধু ব্যবহারকারীর বাংলা-লেখা প্রশ্নের সাথে মেলানোর
        // জন্য .contains()-এ থাকে, তাই পাহারা ৯.১৪ ধরে; এখানেও যোগ করা হলো।
        // 🎤🔒 V1538 (১৮.০৯.২০২৬) — VoiceReportModel.kt-এর MONTH_MAP-এর মাসের
        // নাম — উপরের V1415-এর মতোই, শুধু প্রশ্ন চেনার জন্য, পর্দায় দেখানো
        // হয় না (উত্তরে সবসময় ইংরেজি মাসের নাম, MONTH_LABEL_EN থেকে)।
        "জানুয়ারি" to "January",
        "ফেব্রুয়ারি" to "February",
        "ফেব্রুয়ারী" to "February",
        "মার্চ" to "March",
        "এপ্রিল" to "April",
        "মে" to "May",
        "জুন" to "June",
        "জুলাই" to "July",
        "আগস্ট" to "August",
        "অগাস্ট" to "August",
        "সেপ্টেম্বর" to "September",
        "অক্টোবর" to "October",
        "নভেম্বর" to "November",
        "ডিসেম্বর" to "December",
        "গত মাস" to "last month",
        "পেসেন্ট" to "patient",   // 🎤 V1530 — "পেশেন্ট"-এর ভুল বানানও ধরা হয়
        "ডিসকাউন্ট" to "Discount",
        "ছবি" to "photo",
        "পাঠিয়েছে" to "sent",
        "পাঠানো" to "sent",
        "রেফার" to "refer",
        "কিশানগঞ্জ" to "Kishanganj",
        "জলপাইগুড়ি" to "Jalpaiguri",
        "কোচবিহার" to "Cooch Behar",
        "কুচবিহার" to "Cooch Behar",
        "ফালাকাটা" to "Falakata",
        "বীরপাড়া" to "Birpara",
        "গতকাল" to "Yesterday",
        "আজ" to "Today",
        "সাত দিন" to "seven days",
        "এক মাস" to "one month",
        "পেশেন্ট" to "patient",
        "রোগী" to "patient",
        "কালেকশন" to "collection",
        "জমা" to "deposit",
        "টাকা" to "money",
        "কতজন" to "how many",
        "কয়টা" to "how many",   // 🎤 V1447 (TK-রিপোর্ট: "কয়টা" প্রশ্ন ধরা পড়ছিল না)
        "কয়জন" to "how many",   // 🎤 V1447
        "এসেছিল" to "came",
        "এসেছে" to "has come",
        "কত" to "how much",
        "বিক্রি" to "sale",
        // 🎤🔒 V1416 (১৩.০৯.২০২৬) — মেডিসিন/স্যালাইন বিক্রির প্রশ্ন-ছাঁচ
        "স্যালাইন" to "Saline",
        "ওষুধ" to "medicine",
        // 🎤🔒 V1417 (১৩.০৯.২০২৬) — এনকোয়ারি-সংখ্যা/রিফান্ডের প্রশ্ন-ছাঁচ
        "রিফান্ড" to "refund",
        // 🎤🔒 V1418 (১৩.০৯.২০২৬) — ক্যাশ হ্যান্ডওভার/RMP-বাকির প্রশ্ন-ছাঁচ
        "হ্যান্ডওভার" to "handover",
        "কমিশন" to "commission",
        "বাকী" to "pending",
        // 🎤🔒 V1419 (১৩.০৯.২০২৬) — মেডিসিন-বাকি · অ্যাপ-কল · ট্র্যাশ · RMP-অগ্রিম · "এই মাসে"
        "এই মাস" to "this month",
        "কল" to "call",
        "অ্যাপ" to "app",
        "হয়েছে" to "done",
        "ফলো" to "follow",
        "ডিলিট" to "delete",
        "ট্র্যাশ" to "trash",
        "মোছা" to "deleted",
        "মুছে" to "deleted",
        "অগ্রিম" to "advance",
        "অ্যাডভান্স" to "advance",
        // 🎤🔒 V1420 (১৩.০৯.২০২৬) — অ্যাপয়েন্টমেন্ট · আসার কথা · হ্যান্ডওভার-বাকি · অনুরোধ · ছুটি · রিমাইন্ডার · ফি ফেরত
        "আগামীকাল" to "Tomorrow",
        "অ্যাপয়েন্টমেন্ট" to "appointment",
        "আসবে" to "will come",
        "হয়নি" to "not done",
        "রিকোয়েস্ট" to "request",
        "রেফারেল" to "referral",
        "ছুটি" to "leave",
        "রিমাইন্ডার" to "reminder",
        "ডাক্তার" to "doctor",
        "ফেরত" to "return",
        "ভিজিট" to "visit",
        "ফি" to "fee",
        // 🎤🔒 V1421 (১৩.০৯.২০২৬) — চেম্বার-বন্ধ-হয়নি · no-show · OUT-বাদ · WFH · ডুপ্লিকেট · ফি-জমা-পড়েনি · কল-বাকি · বার্তা
        "সপ্তাহ" to "week",
        "চেম্বার" to "chamber",
        "বন্ধ" to "closed",
        "আসেননি" to "did not come",
        "আসেনি" to "did not come",
        "আউট" to "out",
        "দেয়নি" to "not given",
        "দেননি" to "not given",
        "বাড়ি থেকে" to "from home",
        "ডুপ্লিকেট" to "duplicate",
        "ইনসেন্টিভ" to "incentive",   // 🎤🔒 V1578
        "জমা পড়েনি" to "not received",
        "জমা হয়নি" to "not received",
        "বার্তা" to "message",
        "মেসেজ" to "message",
        "হোয়াটসঅ্যাপ" to "WhatsApp",
        // 🎤🔒 V1422 (১৩.০৯.২০২৬) — নতুন রোগী · ফলো-আপ কল · রোগের নাম · RMP কল · ফিল্ড · ঘণ্টা · হাজির
        "শুরু হয়নি" to "not started",
        "নতুন" to "new",
        "আরএমপি" to "RMP",
        "কথা" to "supposed",
        "করতে হবে" to "to do",
        "ফিল্ড" to "field",
        "কিমি" to "km",
        "ঘুরে" to "travelled",
        "ঘণ্টা" to "hours",
        "ঘন্টা" to "hours",
        "হাজির" to "present",
        "উপস্থিত" to "present",
        "পাইলস" to "Piles",
        "ফিসার" to "Fissure",
        // 📊 V1426 — মাস-তুলনার প্রশ্ন-ছাঁচ
        "তুলনা" to "compare",
        "চেয়ে" to "than",
        "ফিস্টুলা" to "Fistula",
        "হাইড্রোসিল" to "Hydrocele",
        "গুপ্ত" to "Gupt",
        // 🎤 V1428 — কোন ব্রাঞ্চে সবচেয়ে বেশি/কম · RMP-কে দেওয়া কমিশন · IN-বাদ (প্রশ্ন-ছাঁচ)
        "কোন ব্রাঞ্চ" to "which branch",
        "কোন শাখা" to "which branch",
        "সবচেয়ে" to "most",
        "সবচেয়ে কম" to "least",
        "দেওয়া" to "given",
        "দেয়া" to "given",
        "দিয়েছি" to "gave",
        "পেয়েছে" to "received",
        "পেল" to "got",
        "ইন টাইম" to "IN time",
        "ইন-টাইম" to "IN time",

        // 🟢🔒 V629 (২৪.০৮.২০২৬) — Statement (স্টেটমেন্ট) পর্দার নতুন লেখা।
        "স্টেটমেন্ট" to "Statement",
        "\"From\" তারিখ \"To\"-এর পরে হতে পারে না।" to "\"From\" date cannot be after \"To\" date.",
        "চলতি ব্যালেন্স" to "Running Balance",
        "এই সময়ের মধ্যে এখনো কোনো এন্ট্রি নেই।" to "No entries yet in this period.",
        "আগে টাকার হিসাব পর্দায় একটি ব্রাঞ্চ বাছুন" to "Select a branch on the Income & Expense screen first.",
        "উপরে একটি ব্রাঞ্চ বাছুন" to "Select a branch above.",
        "ব্যয়" to "Expense",
        "Category বাছুন অথবা Paid To-তে নাম লিখুন" to "Select a Category or enter a name in Paid To.",

        // 🆕🔒 V496 (২১.০৮.২০২৬) — হাজিরা · আঙুলের ছাপ · অবস্থান · সেশনের নতুন লেখা।
        // খাতার সারি B158: বাংলা-বন্ধ স্টাফের পর্দায় যেন একটাও বাংলা না থাকে,
        // তাই প্রতিটা নতুন লেখার ইংরেজি এখানে যোগ করা হলো।
        // 🆕 V496 সংশোধন (TK §২ ও §৩, ২১.০৮.২০২৬) — বেতন শুধু staff-এর।
        "এই অ্যাকাউন্টের জন্য বেতনের হিসাব নেই। বেতন শুধু স্টাফদের জন্য।" to "This account has no salary record. Salary is only for staff.",
        "এই ফোনে আঙুলের ছাপের ব্যবস্থা নেই। মাস্টারকে জানান।" to "This phone has no fingerprint sensor. Please tell the master.",
        "আঙুলের ছাপের যন্ত্রটা এই মুহূর্তে কাজ করছে না। একটু পরে আবার চেষ্টা করুন।" to "The fingerprint sensor is not working right now. Please try again shortly.",
        "ফোনে আঙুলের ছাপ যোগ করা নেই। Settings → Security → Fingerprint-এ গিয়ে নিজের আঙুল যোগ করুন, তারপর আবার চেষ্টা করুন।" to "No fingerprint is set up on this phone. Go to Settings > Security > Fingerprint, add your finger, then try again.",
        "ফোনের নিরাপত্তা আপডেট বাকি আছে, তাই আঙুলের ছাপ ব্যবহার করা যাচ্ছে না। মাস্টারকে জানান।" to "A phone security update is pending, so fingerprint cannot be used. Please tell the master.",
        "এই ফোনে নিরাপদ আঙুলের ছাপের ব্যবস্থা নেই। মাস্টারকে জানান।" to "This phone does not support secure fingerprint. Please tell the master.",
        "কয়েকবার ভুল হয়েছে, তাই কিছুক্ষণের জন্য বন্ধ। একটু পরে আবার চেষ্টা করুন।" to "Too many wrong tries, so it is locked for a short while. Please try again shortly.",
        "আঙুলের ছাপ বন্ধ হয়ে গেছে। ফোনের স্ক্রিন-লক (PIN/প্যাটার্ন) দিয়ে একবার ফোন খুলুন, তারপর আবার চেষ্টা করুন।" to "Fingerprint is locked. Unlock the phone once with its screen lock, then try again.",
        "আঙুলের ছাপ মিলল না। আঙুল একটু মুছে আবার চেষ্টা করুন।" to "Fingerprint did not match. Wipe your finger and try again.",
        "আপনি বাতিল করেছেন।" to "You cancelled.",
        "আঙুলের ছাপ যাচাই করা গেল না। একটু পরে আবার চেষ্টা করুন।" to "Could not check the fingerprint. Please try again shortly.",
        "আপনি ক্লিনিকে নেই" to "You are not at the clinic",
        // 🔴 V497 বিল্ড-ফিক্স (২১.০৮.২০২৬): আগে এখানে `$distance` লেখা ছিল।
        //    Kotlin-এ `"…$নাম…"` মানে **চলকের মান বসানো** — লেখা নয়। তাই
        //    "unresolved reference: distance" এসে বিল্ড ভাঙত। তাছাড়া চলতি
        //    অ্যাপে ওখানে আসল সংখ্যা বসে (যেমন "৪২"), তাই এই সারি কখনো
        //    মিলতই না। এখন দু-টুকরো করে দেওয়া হলো — দুটোই মেলে।
        " (প্রায় " to " (about ",
        " মিটার দূরে)" to " metres away)",
        "। হাজিরা শুধু চেম্বারে এসে দেওয়া যায়।" to ". Attendance can only be marked at the chamber.",
        "}-এর অবস্থান এখনো অ্যাপে বসানো হয়নি। মাস্টারকে জানান — তিনি Fix Attendance দিয়ে হাজিরা বসিয়ে দিতে পারবেন।" to "} location is not set in the app yet. Tell the master - he can mark it with Fix Attendance.",
        "হাজিরার জন্য Location-এর অনুমতি দরকার। অনুমতি দিয়ে আবার চেষ্টা করুন।" to "Location permission is needed for attendance. Allow it and try again.",
        "ফোনের Location বন্ধ আছে। চালু করে আবার চেষ্টা করুন।" to "Phone Location is off. Turn it on and try again.",
        "অবস্থান পাওয়া গেল না। খোলা জায়গায় গিয়ে বা জানালার কাছে দাঁড়িয়ে আবার চেষ্টা করুন।" to "Could not get your location. Stand near a window or in the open and try again.",
        "অবস্থানটা যথেষ্ট নিশ্চিত নয়। একটু অপেক্ষা করে বা জানালার কাছে গিয়ে আবার চেষ্টা করুন।" to "The location is not accurate enough. Wait a moment or move near a window and try again.",
        "নকল অবস্থান ধরা পড়েছে, তাই হাজিরা নেওয়া হয়নি। মাস্টারকে জানানো হবে।" to "A fake location was detected, so attendance was not taken. The master will be informed.",
        "আপনার ব্রাঞ্চ চেনা গেল না। মাস্টারকে জানান।" to "Your branch could not be identified. Please tell the master.",
        "অবস্থান যাচাই করা গেল না। একটু পরে আবার চেষ্টা করুন।" to "Could not check your location. Please try again shortly.",
        "পুরনো Android-এ দরকার" to "Needed on older Android",
        "লগইন যাচাই করা গেল না। ইন্টারনেট দেখে আবার চেষ্টা করুন।" to "Could not verify your sign-in. Check the internet and try again.",
        "হাজিরা পাঠানো গেল না। ইন্টারনেট দেখে আবার চেষ্টা করুন।" to "Could not send the attendance. Check the internet and try again.",
        "হাজিরা এখন সেভ করা গেল না। ইন্টারনেট দেখে আবার চেষ্টা করুন।" to "Attendance could not be saved right now. Check the internet and try again.",
        "সার্ভার কিছু জানায়নি। আবার চেষ্টা করুন।" to "The server did not reply. Please try again.",
        "সার্ভারের উত্তর বোঝা গেল না। আবার চেষ্টা করুন।" to "Could not read the server reply. Please try again.",
        "ডাক্তারদের জন্য হাজিরার ব্যবস্থা নেই — আপনি যেকোনো সময় আসতে ও যেতে পারেন।" to "Doctors have no attendance - you may come and go at any time.",
        "অ্যাপ খুলুন" to "Open app",
        // 🆕 V499 (২১.০৮.২০২৬) — অ্যাপ খোলার তালা: আঙুল অথবা ফোনের পাসওয়ার্ড।
        "আঙুলের ছাপ দিন, নয়তো ফোনের পাসওয়ার্ড দিন" to "Give your fingerprint, or your phone password",
        "অ্যাপের তালা বন্ধ" to "App lock is off",
        "এই ফোনে আঙুলের ছাপ বা স্ক্রিন-লক কিছুই নেই, তাই অ্যাপ সরাসরি খুলল। নিরাপত্তার জন্য ফোনের Settings-এ গিয়ে স্ক্রিন-লক চালু করুন।" to "This phone has no fingerprint and no screen lock, so the app opened directly. For safety, turn on a screen lock in phone Settings.",
        "ঠিক আছে" to "OK",
        "নিরাপত্তার জন্য আঙুলের ছাপ দিন" to "Give your fingerprint for security",
        "অ্যাপ বন্ধ করুন" to "Close app",
        "Settings খুলুন" to "Open Settings",
        "আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে। মাস্টারকে জানান।" to "Your account has been closed. Please tell the master.",
        // 🔴 V497 বিল্ড-ফিক্স: এখানেও `$until` ছিল — একই কারণে বাদ। মাঝখানে
        //    আসল তারিখ বসে, তাই শুধু স্থির অংশটুকু অনুবাদ করা হলো।
        //    ⛔ শুধু "আপনি " বদলানো হয়নি — ওটা আরও অনেক বার্তার শুরুতে আছে,
        //       বদলালে সেগুলোর মানে উল্টে যেত। শেষ জাল বাকি বাংলা মুছে দেবে।
        " পর্যন্ত বন্ধ আছেন। মাস্টারকে জানান।" to " - you are suspended until this date. Please tell the master.",
        "অনেকদিন অ্যাপ ব্যবহার হয়নি, তাই নিরাপত্তার জন্য লগআউট করা হলো। মোবাইল ও পাসওয়ার্ড দিয়ে আবার লগইন করুন।" to "The app was unused for a long time, so you were logged out for safety. Log in again with mobile and password.",
        "ক্লিনিকে আছেন কিনা দেখা হচ্ছে…" to "Checking whether you are at the clinic...",
        "ক্লিনিকে আছেন কিনা" to "At the clinic?",
        "হাজিরা দিতে আঙুলের ছাপ দিন" to "Give your fingerprint to mark attendance",
        "হাজিরা সেভ হচ্ছে…" to "Saving attendance...",
        "হাজিরা হয়ে গেছে।" to "Attendance done.",
        "আঙুলের ছাপ" to "Fingerprint",
        "অ্যাকাউন্ট বন্ধ" to "Account closed",
        "এখন সেভ করা গেল না।" to "Could not save right now.",
        "আজ ছুটি" to "Leave today",
        "হাজিরা" to "Attendance",
        // 🔵🆕 V488 (২০.০৮.২০২৬, TK-নির্দেশ): ওষুধ খোঁজার ঘরের নতুন লেখা
        // ("🔍 Search" → "🔍 মেডিসিন")। KNE-KISHAN5-এর ফোনে বাংলা বন্ধ, তাই
        // অনুবাদ না থাকলে শব্দটা **মুছে** যেত (শেষ-জাল নিয়ম) আর ঘরটা ফাঁকা
        // দেখাত। এখন ওই ফোনে সেখানে "Medicine" বসবে।
        "মেডিসিন" to "Medicine",
        // 🔴🆕🔒 V438 (১৮.০৮.২০২৬, TK-নির্দেশ "৩ নম্বরটা করুন") — পাহারাদার ৯.১৪-এ
        //    ধরা পড়া **সব** বাকি বাংলা লেখার ইংরেজি। এতদিন এগুলোর অনুবাদ ছিল না,
        //    তাই বাংলা-বন্ধ স্টাফের (KNE-KISHAN5) পর্দায় ওগুলো হয় বাংলাতেই থাকত,
        //    নয়তো "শেষ জাল"-এ মুছে গিয়ে **ফাঁকা** দেখাত — দুটোই খারাপ।
        //    ⛔ বাংলা চালু থাকা বাকি সবার পর্দা এক চুলও বদলায় না।

        // — Branch বাছাইয়ের ফাঁকা-পর্দার বার্তা (BranchFilterStore.kt)
        "উপরে ডান দিকের Branch বাক্স থেকে একটি ব্রাঞ্চ বাছুন" to
            "Choose a branch from the Branch box at the top right",
        "একবার বাছলেই সব পর্দায় এটাই মনে থাকবে — বারবার বাছতে হবে না" to
            "Once you choose, every screen remembers it — no need to choose again",
        "সব ব্রাঞ্চ একসঙ্গে দেখতে চাইলে All বাছুন" to
            "Choose All to see every branch together",
        "উপরে ডান দিকে Branch বাছুন" to "Choose Branch at the top right",

        // — Chamber আবার খোলার প্রশ্ন (ChamberAttendanceActivity.kt)
        //   ⛔ "এই দিনের (" আগে থেকেই তালিকায় আছে — শুধু শেষ টুকরোটা ছিল না
        ") চেম্বার আবার খুলবেন?" to ")?",

        // — RMP পেমেন্ট (DoctorVisitActivity.kt)
        "শুধু নিজের ব্রাঞ্চের RMP-কে টাকা দেওয়া যাবে (এই RMP: " to
            "Payment is allowed only to an RMP of your own branch (this RMP: ",
        "এই বাড়তি টাকা শুধু Master অনুমোদন করতে পারেন" to
            "Only Master can approve this extra amount",

        // — Export Data-র সতর্কবার্তা (ExportDataActivity.kt)
        "এটা চাপলে বাছাই করা টেবিলের **সব তথ্য** ইন্টারনেট থেকে নামবে " to
            "Tapping this downloads **all the data** of the selected tables from the internet ",
        "(রোগীর ছবিসহ) — প্রায় পুরো ডেটাবেস" to
            "(with patient photos) — almost the whole database",
        "এতে মাসিক ডেটার হিসাব থেকে অনেকটা খরচ হয়। সত্যিই দরকার হলে " to
            "This uses up a large part of the monthly data allowance. Only if it is truly needed ",
        "তবেই চালান, বারবার নয়" to "should you run it — not again and again",
        "এখন নামাব?" to "Download now?",

        // — Income / Expense পর্দা (IncomeExpenseActivity.kt)
        "এই দিনে শুধু খরচ আছে — Add Expense পর্দা থেকে দেখুন" to
            "This day has expenses only — view them from the Add Expense screen",
        " খাতার সারিতেই লেখা — বদলাতে হলে ওই সারির তারিখ / Cash / Online ঘরে 3 বার চাপুন" to
            " below is written in the notebook row itself — to change it, tap 3 times on that row's Date / Cash / Online box",
        "খাতার সারিতে লেখা খরচ" to "Expense written in the notebook row",
        "এই খরচটি এখন বদলানো যাচ্ছে না — একবার ↻ চেপে আবার দেখুন" to
            "This expense cannot be edited right now — press ↻ once and try again",
        "✏️ খরচ বদলান" to "✏️ Edit Expense",
        "নাম লিখুন (শুধু সংখ্যা চলবে না)" to "enter a name (numbers alone will not do)",
        "খরচ বদলানো হয়েছে" to "Expense updated",
        "বদলানো গেল না (নেট?) — আবার চেষ্টা করুন" to
            "Could not update (internet?) — please try again",
        "এই খরচটি মুছবেন?" to "Delete this expense?",
        "এটি হিসাব থেকে বাদ যাবে" to "It will be taken out of the totals",
        "চিরতরে মুছবে না — দরকারে ফেরানো যাবে" to
            "It is not erased forever — it can be brought back if needed",
        "খরচটি মুছে ফেলা হয়েছে" to "The expense has been deleted",
        "মোছা গেল না (নেট?) — আবার চেষ্টা করুন" to
            "Could not delete (internet?) — please try again",
        "এই দিনে এই অঙ্ক আগেই জমা আছে — আর যোগ হয়নি" to
            "This amount is already recorded for this day — it was not added again",
        "টাকা: " to "Amount: ",
        "নিচের " to "The ",

        // V372: KNE-KISHAN5-এর English-only display-এর জন্য অনুমোদিত
        // Calendar/Follow-up/Doctor Check-up অনুবাদ। অন্য Staff-এর লেখা বদলায় না।
        "কাল আসার কথা" to "Expected Tomorrow",
        "আসবেন" to "will come",
        "ফোন" to "Call",
        "পরের ফোন কবে?" to "Next Call Date?",
        "পরের আসার দিন" to "Next Expected Date",
        "রোগী ফোনে কী বলল?" to "What did the patient say on the call?",
        "আসবে (চেম্বারে)" to "Will come (to chamber)",
        "চেম্বার-দিন বাছুন → একদিন আগে ফোন-রিমাইন্ডার" to "Choose chamber date → call reminder one day earlier",
        "শুধু ফোন করব" to "Call only",
        "যেকোনো দিন → ওইদিনই ফোনের তারিখ" to "Any date → same date for the call",
        // 📵🔒 V711 (২৬.০৮.২০২৬, TK-নির্দেশ) — "আর কল লাগবে না" ব্যবস্থার লেখাগুলো।
        "📵 আর কল লাগবে না" to "📵 No more calls needed",
        "চিকিৎসা চলছে — কল-তালিকা ও ব্যানার থেকে সরে যাবে" to "Treatment is running — will leave the call list and the banner",
        "আর ফোন করা হবে না — কল-তালিকা ও ব্যানার থেকে সরে যাবে" to "No more calls — will leave the call list and the banner",
        "ঠিক আছে — এঁকে আর কল-তালিকায় দেখাবে না" to "Done — this person will no longer show in the call list",
        "📵 কল বন্ধ" to "📵 NO CALLS",
        "Clinical পরীক্ষা" to "Clinical Examination",
        "চোখে দেখা পরীক্ষা" to "Visual Examination",
        // 📝 V1280 (তালিকা সারি ৪০৩) — চেকআপ ধাপ ৩-এর চুক্তি/পরিকল্পনার ঘর
        "চিকিৎসার চুক্তি ও পরিকল্পনা" to "Treatment Plan & Agreement",
        "রোগীর সাথে কী চুক্তি হল — কীভাবে চিকিৎসা, কতদিন, কিস্তি — বিস্তারিত এখানে লিখুন" to "What was agreed with the patient — how, how long, instalments — write in detail",
        "চিকিৎসা পরিকল্পনা" to "Treatment Plan",
        "এখানে কিছু লিখুন…" to "Write here…",
        "আয় ও ব্যয়" to "Income & Expense",
        "প্রকৃত জমার তারিখ" to "Actual Deposit Date",
        "টাকার খাতা" to "Cash Ledger",
        "ব্রাঞ্চ বাছুন" to "Select Branch",
        "মাস বাছুন" to "Select Month",
        "খরচ" to "Expense",
        "অবশিষ্ট টাকা" to "Remaining Balance",
        "এই দিনের খরচের কোনো বিবরণ লেখা নেই।" to "No expense details recorded for this day.",
        "মোট খরচ" to "Total Expense",
        /* 🔴🔒 V965 (০১.০৯.২০২৬, TK-এর স্পষ্ট নির্দেশ) — *"প্রয়োজনে পপ-আপ আরো বড়
           হবে, কিন্তু ইংলিশ এবং বাংলা দুটোই থাকবে"*। ওই বার্তায় ইংরেজি লাইনটা
           **আগে থেকেই আলাদা করে** বসানো আছে, আর বাংলাটা TK-এর নির্দেশে
           ইচ্ছাকৃতভাবে হুবহু দেখানো হয় (`NoBengali.s()` দিয়ে যায় না)।
           অনুবাদটা এখানে রাখা হলো যাতে প্রজেক্টের নিয়ম-খাতা সম্পূর্ণ থাকে। */
        "TK BISWAS যে কোনো সময় যাচাই করতে পারেন — সতর্ক থাকুন"
            to "TK BISWAS can verify this at any time — be careful",
        "খরচের বিবরণ" to "Expense Details",
        // 💰 V1211 — Ledger Entry-র নতুন জোড়া ঘর (TK-অনুমোদিত ফটো-প্রুফ)
        "কত টাকা খরচ করলাম" to "How much was spent",
        "কীভাবে ছাড় দেবেন" to "How to give the discount",   // 🏷️ V1213
        // 👥 V1215 — "এই টাকা কার কার কাছ থেকে" পপ-আপ
        "এই দিনের অ্যাপে তোলা কোনো পেমেন্ট পাওয়া গেল না (হাতে লেখা সংখ্যা হতে পারে)।"
            to "No app-entered payment found for this day (it may be a hand-written figure).",
        "✏️ এই সংখ্যাটা হাতে বদলান" to "✏️ Edit this number by hand",
        "কীসে খরচ করলাম" to "What it was spent on",
        "＋  আরও একটা খরচ" to "＋  Add another expense",
        "ব্যায়" to "Expense",
        "টাকার হিসাব" to "Accounts",
        "আজকের হিসাব" to "Today's Accounts",
        "আয়" to "Income",
        "ব্যয়" to "Expense",
        "অবশিষ্ট" to "Balance",
        "লোড হয়নি" to "Not Loaded",
        "এই মাসের হিসাব" to "This Month's Accounts",
        "পুরো খাতা" to "Full Ledger",
        "➕ নতুন দিন" to "➕ New Day",   /* ➕ V1174 */
        "অংশীদারি ভাগ" to "Partner Share",
        "উপরে ডানে ব্রাঞ্চ বাছুন" to "Select a branch at the top right",
        "নগদ বা অনলাইন — অন্তত একটা লিখুন" to "Enter at least one: Cash or Online",
        "আরেকটা যোগ করতে পারেন" to "You may add another",
        "Amount লিখুন" to "Enter Amount",
        "খরচের সংখ্যায় চাপ দিলে ওই দিন কিসে খরচ হয়েছিল দেখাবে" to "Tap the expense amount to view that day's expense details",
        "এই মাসে এখনো কোনো এন্ট্রি নেই।" to "No entries for this month yet.",
        "সব ব্রাঞ্চ" to "All Branches",
        "গত মাসের ব্যালেন্স" to "Previous Month's Balance",
        "WhatsApp-এ শেয়ার" to "Share on WhatsApp",
        "শেয়ার করা গেল না" to "Could not share",
        // 🟢🔒 V699 (২৬.০৮.২০২৬) — Doctor Note & Reminder পপ-আপের নতুন লেখা।
        "নোট" to "Note",
        "ইচ্ছেমতো" to "Optional",
        "তারিখ বাছলে তার আগের দিন মনে করানো হবে — শুধু আপনাকেই। সময় না বাছলে সন্ধ্যা 6টা।"
            to "If you pick a date, you will be reminded the day before — only you. Without a time, 6 PM.",
        /* 🩺 V1109 (TK-নির্দেশ) — ডাক্তার বাছার নতুন ঘরের লেখা। */
        "তারিখ বাছলে তার আগের দিন মনে করানো হবে — শুধু বাছা ডাক্তারকেই। সময় না বাছলে সন্ধ্যা 6টা।"
            to "If you pick a date, only the chosen doctor is reminded the day before. Without a time, 6 PM.",
        "কোন ডাক্তারকে মনে করাবে?" to "Which doctor to remind?",
        "ডাক্তার বাছুন" to "Pick a doctor",
        "সব ডাক্তার" to "All doctors",
        "কিছু লেখা বা বাছা হয়নি" to "Nothing written or picked",
        "রোগী পাওয়া যায়নি — নিচের SAVE চাপুন" to "Patient not found — use the SAVE below",
        "মনে করানোর নোট সেভ হয়েছে" to "Reminder note saved",
        "সেভ হয়নি — নিচের SAVE চাপুন" to "Not saved — use the SAVE below",
        // 🟢🔒 V693 (২৬.০৮.২০২৬) — মাসের হিসাবের "••• Options" মেনু।
        "আগে Show চাপুন।" to "Tap Show first.",
        "হিসাব আসছে — এক মুহূর্ত পরে আবার চাপুন।" to "Report is loading — please try again in a moment.",
        "হালনাগাদ হচ্ছে…" to "Updating…",
        "খুলছি... একটু অপেক্ষা করুন" to "Opening... please wait",
        "আজকের তথ্য আনছি..." to "Loading today's information...",
        "আবার চেষ্টা করুন" to "Try Again",
        "আজকের তথ্য এখন আনা গেল না। OUT TIME নিরাপদে বসাতে আজকের তথ্যটা দরকার" to "Today's information could not be loaded. It is required to save OUT TIME safely",
        "আবার চেষ্টা" to "Retry",
        "Net সমস্যা" to "Network problem",
        // 🆕 B438 (05.08.2026) — Work Notebook postpone-time toast-এ স্টাফের
        // বাছা সময় ইন্টারপোলেট থাকে বলে WHOLE-এ মিলবে না।
        "ঠিক আছে, " to "OK, ", "-এ আবার মনে করানো হবে" to " you'll be reminded again",
        // 🆕 B433 (05.08.2026) — MessageSentLog.warningText()-এর সতর্কতা-
        // লাইনে স্টাফের নাম/তারিখ ইন্টারপোলেট করা থাকে বলে WHOLE-এ পুরো
        // বাক্য মিলবে না — তাই এখানে Bengali অংশটুকু আলাদা করে অনুবাদ।
        "⚠️ এই বার্তা আগে " to "⚠️ Already sent by ",
        "-এ পাঠিয়েছেন।" to ".",
        "অন্য স্টাফ" to "another staff",
        // 🆕 B419 (04.08.2026) — Chamber Reopen-এর বাক্যগুলোতে তারিখ/ব্র্যাঞ্চ
        // ইন্টারপোলেট করা থাকে বলে WHOLE-এ পুরো বাক্য মিলবে না — তাই এখানে
        // Bengali অংশটুকু আলাদা করে (ইন্টারপোলেশনের আগে/পরে) অনুবাদ।
        "এই দিনের (" to "Reopen chamber for (",
        ") চেম্বার আবার খোলার অনুরোধ Master-এর কাছে পাঠাবেন?" to ") — send request to Master?",
        " আবার এডিটযোগ্য" to " editable again",
        "\nMaster: অনুমোদন দিলে এই দিনের চেম্বার আবার এডিটযোগ্য হয়ে যাবে।" to "\nMaster: approving will make this day's chamber editable again.",
        // 🆕 (03.08.2026, TK-অনুমোদিত) — Doctor Note স্ক্রিনে (activity_doctor_
        // checkup.xml + DoctorCheckupActivity.kt) ইংরেজির পাশে বাংলা যোগ হয়েছে।
        // Kishanganj-এ শুধু ইংরেজিটাই থাকুক, তাই "\nবাংলা" অংশটা পুরোপুরি বাদ।
        "\nমৌলিক বৃত্তান্ত" to "", "\nপ্রধান অভিযোগ" to "", "\nকতদিন থেকে" to "",
        "\nপেশা" to "", "\nপূর্ববর্তী চিকিৎসার বৃত্তান্ত" to "", "\nপূর্বে নেওয়া চিকিৎসা" to "",
        "\nপূর্বের ফলাফল" to "", "\nআগের খরচ" to "", "\nচিকিৎসার মেয়াদ" to "",
        "\nক্লিনিক্যাল ফলাফল" to "", "\nদৃশ্যমান পরীক্ষা" to "", "\nঅন্যান্য" to "",
        "\nডিআরই পরীক্ষা" to "", "\nপ্রক্টোস্কোপি গ্রেড" to "", "\nঅন্যান্য ফলাফল" to "",
        "\nপরীক্ষা-নিরীক্ষা" to "", "\nপরামর্শ ও উপদেশ" to "", "\nরোগের ব্যাখ্যা দেওয়া হয়েছে" to "",
        "\nআর্থিক আলোচনা" to "", "\nআনুমানিক খরচ" to "", "\nআনুমানিক সুস্থ হওয়ার সময়" to "",
        "\nঅগ্রিম আলোচনা" to "", "\nরোগীর সিদ্ধান্ত" to "", "\nঅন্যান্য / মন্তব্য" to "",
        "\nছবি ও নথি" to "", "\nআগের ছবি" to "", "\nচলাকালীন ছবি" to "", "\nপরের ছবি" to "",
        "\nনথি / রিপোর্ট নোট" to "", "\nদ্রুত কাজ" to "",
        // 🆕 (05.08.2026, TK-নির্দেশ, স্টেপ ৫ Photo & Video) — "Documents /
        // Reports Note" ও "Quick Actions" এখন " · " দিয়ে পাশাপাশি (আগে "\n" ছিল)।
        " · নথি / রিপোর্ট নোট" to "", " · দ্রুত কাজ" to "",
        // 🆕 (04.08.2026, TK-অনুমোদিত, "লক করুন") — Doctor Note স্ক্রিন নতুন
        // ডিজাইনে (হেডার + ৫ ধাপ + Treatment Plan)। নতুন/বদলানো বাংলা টুকরো।
        " · রোগীর বিবরণ" to "", "\nবর্তমান রোগের ইতিহাস ও পূর্ববর্তী চিকিৎসার বিবরণ" to "",
        "\nপ্রধান সমস্যা" to "", "\nহঠাৎ হয় নাকি ধীরে ধীরে বাড়ে" to "",
        "\nআগের চিকিৎসার বিবরণ" to "", "\nআগের চিকিৎসার ফলাফল" to "",
        "\nআগের চিকিৎসার খরচ" to "", "\nচিকিৎসা কত দিন চলেছিল" to "",
        // 🆕 (05.08.2026, TK-নির্দেশ) — স্টেপ ১-এর সেকশন-হেডার বাদ + ৬টা
        // লেবেল এখন "\nবাংলা"-র বদলে " · বাংলা" (এক লাইনে, পাশাপাশি) — তাই
        // এই নতুন ফরম্যাটের জন্য আলাদা এন্ট্রি; পুরনো "\n..." এন্ট্রিগুলো
        // (উপরে ও ৩৩২-৩৩৪ লাইনে) মোছা হয়নি, অন্য স্ক্রিনে/জায়গায় এখনো লাগে।
        " · প্রধান সমস্যা" to "", " · কতদিন থেকে" to "",
        " · হঠাৎ হয় নাকি ধীরে ধীরে বাড়ে" to "", " · পেশা" to "",
        " · আগের চিকিৎসার বিবরণ" to "", " · আগের চিকিৎসার ফলাফল" to "",
        // 🆕 (06.08.2026, TK-নির্দেশ, B482) — Grade/Other Findings এখন
        // " · " ফরম্যাটে (আগে "\n" ছিল, উপর-নিচে থেকে বক্স-ডিজাইনে টেক্সট
        // একলাইনে আনতে বদল)।
        " · প্রক্টোস্কোপি গ্রেড" to "", " · অন্যান্য ফলাফল" to "",
        // 🆕 (06.08.2026, TK-নির্দেশ, B484) — Dialer-এর নতুন "চেম্বার নম্বর
        // আছে কিনা" প্রশ্ন-পপ-আপ ও ফাঁকা-তালিকার বার্তা।
        "এই ফোনে কি চেম্বার/ব্রাঞ্চের নম্বর আছে?" to "Does this phone have the chamber/branch number?",
        "এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন — তাহলে Dialer-এ কোনো কল দেখানো হবে না।" to "Does any SIM on this phone have the clinic's chamber/branch number? If it's a personal number, choose \"No\" — Dialer will then show no calls.",
        // 🆕 (B488) — Work Notebook-এর একই প্রশ্ন, বার্তা সামান্য ছোট।
        "এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন।" to "Does any SIM on this phone have the clinic's chamber/branch number? If it's a personal number, choose \"No\".",
        "হ্যাঁ" to "Yes",
        "এই ফোনে চেম্বারের নম্বর নেই বলে জানানো আছে, তাই কল দেখানো হচ্ছে না।" to "This phone is marked as not having the chamber number, so no calls are shown.",
        // 🔴🔒 V474 (20.08.2026) — "কাল আসার কথা" পাতায় নাম/নম্বর long-press
        // করলে কপি হওয়ার নতুন Toast দুটো।
        "নাম কপি হয়েছে" to "Name copied",
        "নম্বর কপি হয়েছে" to "Number copied",
        // 🔴🔒 V481 (20.08.2026) — Chamber বন্ধ থাকা দিনে সম্পাদনা আটকানোর
        // নতুন সতর্কবার্তা।
        "এই দিনের চেম্বার বন্ধ (Close) করা হয়ে গেছে — আর কোনো পরিবর্তন করা যাবে না।" to
            "This day's chamber has already been closed — no more changes can be made.",
        // 🔴🔒 V478 (20.08.2026) — saveDay()-এর সৎ (silent-fail-বন্ধ) সতর্কবার্তা।
        "⚠️ এখনই ক্লাউডে সেভ হয়নি (ফোনে জমা আছে, নেট এলে নিজে বসে যাবে)। এখনই ইন্টারনেট/ওয়াইফাই চেক করে আবার বোতাম চাপুন।" to
            "⚠️ Not saved to the cloud yet (kept queued on this phone, will sync automatically once online). Please check internet/WiFi now and tap the button again.",
        // 🔴🔒 V468 (20.08.2026) — উপরের বাক্যের সাথে "আবার জিজ্ঞাসা করুন"
        // যোগ হওয়ায় নতুন সম্পূর্ণ বাক্যটার জন্য আলাদা এন্ট্রি — পুরনোটা
        // (উপরে) অন্য কোথাও ব্যবহৃত থাকতে পারে বলে অক্ষত রাখা হলো।
        "এই ফোনে চেম্বারের নম্বর নেই বলে জানানো আছে, তাই কল দেখানো হচ্ছে না।\n\n👉 এটা ভুল হলে — এখানে চাপুন আবার জিজ্ঞাসা করতে" to "This phone is marked as not having the chamber number, so no calls are shown.\n\n👉 If this is wrong — tap here to ask again",
        // 🆕 (06.08.2026, B504) — Income-Expense দিনের সারাংশ ডায়ালগ।
        "(শেষ জানা তথ্য — হালনাগাদ হচ্ছে…)" to "(Last known data — updating…)",
        // 🆕 (06.08.2026, TK-নির্দেশ, B473) — Cost/Duration বক্স এখন পূর্ণ-
        // চওড়া (উপর-নিচে), তাই আগের "\n" ফরম্যাটের বদলে বাকি লেবেলগুলোর
        // মতোই " · " দিয়ে এক লাইনে পাশাপাশি। পুরনো "\nআগের চিকিৎসার খরচ"
        // এন্ট্রি (৩৫০ লাইনে) মোছা হয়নি।
        " · আগের চিকিৎসার খরচ" to "", " · চিকিৎসা কত দিন চলেছিল" to "",
        // 🆕 (05.08.2026, TK-নির্দেশ, স্টেপ ২ Clinical Findings) — "Other" ·
        // "B. DRE..." · "D. On Probing" · "E. Investigations" এখন " · "
        // দিয়ে পাশাপাশি (আগে "\n" ছিল); আর "Fistula Opening"/"Bleeding"
        // চেকবক্সে বাংলা এখন নতুন লাইনে (আগে " · " ছিল) — তাই দুই দিকেরই
        // নতুন এন্ট্রি। পুরনো "\nঅন্যান্য"/" · ফিস্টুলার মুখ" ইত্যাদি এন্ট্রি
        // মোছা হয়নি (ক্ষতিহীন, আর কোথাও লাগলে কাজ করবে)।
        " · অন্যান্য" to "", " · ডিআরই পরীক্ষা" to "",
        " · প্রোব পরীক্ষা" to "", " · পরীক্ষা-নিরীক্ষা" to "",
        "\nফিস্টুলার মুখ" to "", "\nরক্ত পড়ে" to "",
        // 🆕 (06.08.2026, TK-নির্দেশ, B478) — Visual/DRE-এর বাকি সব
        // চেকবক্সও এখন "\n" ফরম্যাটে (আগে " · " ছিল, TK-এর ধরিয়ে দেওয়া
        // "প্রতিবার পাইলস লেখার পাশে ডট কেন" এর পরে সবগুলোতেই একই ফিক্স)।
        // পুরনো " · ..." এন্ট্রি (নিচে) মোছা হয়নি।
        "\nবাহ্যিক অর্শ" to "", "\nঅভ্যন্তরীণ অর্শ" to "", "\nফিসার" to "",
        "\nফোলাভাব" to "", "\nকোমলতা/ব্যথা" to "", "\nফিস্টুলা" to "",
        // 🆕 (06.08.2026, TK-নির্দেশ, B479) — Step 3 (Counsel)-এর চেকবক্স
        // লেবেল নতুন "English/ বাংলা" ফরম্যাটে (TK-এর নিজের দেওয়া লেখা)।
        "/ প্রতি অর্শ" to "", "/ ফিস্টুলা প্রতি ইঞ্চি" to "",
        "/ প্রতি ক্ষারসূত্র" to "", "/ মেশিনের মাধ্যমে চিকিৎসা" to "",
        "উপরে টিক মারার বাইরেও যদি রোগীকে অন্য কিছু বলে থাকেন তাহলে সেই কথা এখানে লিখুন, অন্যথায় " to "",
        // 🆕 (05.08.2026, TK-নির্দেশ, B465 — Work Notebook OUT TIME) —
        // "Why are you leaving?"-এর সাবটেক্সট ও "Office time over"-এর
        // পাশে বাংলা, আর নতুন কাস্টম-লেখার বক্সের hint।
        "আমি ক্লিনিক থেকে বেরোলাম" to "",
        " · অফিস টাইম শেষ" to "",
        // 🆕 (06.08.2026, B510) — "Why are you leaving?" কার্ড-ডিজাইনে
        // এখন আলাদা লাইনে (আগে " · " দিয়ে জোড়া ছিল)।
        "অফিস টাইম শেষ" to "Office time over",
        "ব্যক্তিগত কাজ — আগে চলে যাচ্ছেন" to "Personal work — leaving early",
        "কেন যাচ্ছেন এই সম্পর্কে কিছু লিখুন" to "Write something about why you're leaving",
        // 🆕 (05.08.2026, TK-নির্দেশ, B466 — Work Notebook OUT TIME redesign)
        "তুমি কেন চলে যেতে চাইছ" to "",
        "কেন বাড়ি যাচ্ছেন লিখুন" to "Write why you're going home",
        "কেন ব্যক্তিগত কাজে যাচ্ছেন?" to "Why are you leaving for personal work?",
        // 🔴 B466 — Work Notebook-এর নতুন মেশানো (English / বাংলা) লেবেলের
        // বাংলা অংশ — কোডে ইতিমধ্যেই KNE-KISHAN5-এর জন্য আলাদা করে শুধু
        // ইংরেজি টেক্সট বসানো আছে, তবু পাহারাদার (tk_guard.py 9.14) প্রতিটা
        // .kt লিটারেল স্ট্রিং স্ক্যান করে বলে এই এন্ট্রিগুলোও লাগে, নইলে
        // "অনুবাদ নেই" ধরে আটকে যায়।
        " / আজকের কাজ" to "",
        " / আজকের সারাদিনে কি কি কাজ করেছেন" to "",
        " / আজকে কি আপনার ছুটি" to "",
        // 🆕 (06.08.2026, B506) — Leave বোতামের হালনাগাদ লেখা।
        " / আজকে আমার ছুটি" to "",
        // 🩺🔒 V839 (২৯.০৮.২০২৬) — NEXT VISIT PLAN-এর লেখা।
        // ⛔ প্রকল্পের চলতি নিয়মেই: বাংলা অংশটা সরিয়ে দিলে ইংরেজি অর্ধেকটাই
        //    থেকে যায়, তাই বাংলা-বন্ধ স্টাফের পর্দা সম্পূর্ণ ইংরেজি হয়।
        " / ফলো আপ" to "", " / ড্রেসিং" to "", " / মেশিনের কাজ" to "",
        " / ভেতরে সুতো বাঁধা" to "", " / বাইরে সুতো বাঁধা" to "",
        " / সুতো চেঞ্জ" to "", " / সুতো টানা" to "", " / নতুন সুতো পরানো" to "",
        " / ঔষধ দিতে হবে" to "",
        " · কোন ঔষধ" to "", " · অন্য কিছু হলে এখানে লিখুন" to "",
        " · তারিখ (বাধ্যতামূলক নয়)" to "",
        " · পরের দিন বা পরের সপ্তাহে কী হবে" to "",
        /* 🔒 V996 — শিরোনামটা এখন দু লাইনে (ইংরেজি উপরে · বাংলা নিচে),
           তাই একই অনুবাদ নতুন রূপ দুটোতেও বসানো হলো — পর্দার লেখা
           (আসল newline) আর XML-এর কাঁচা লেখা (\\n) দুটোই। */
        "\nপরের দিন বা পরের সপ্তাহে কী হবে" to "",
        "\\nপরের দিন বা পরের সপ্তাহে কী হবে" to "",
        "\nপ্রোব পরীক্ষা" to "", "\nকীভাবে চিকিৎসা করা হবে" to "", " · কীভাবে চিকিৎসা করা হবে" to "",
        "\nআনুমানিক খরচ ও রোগীর সিদ্ধান্ত" to "", "\nআনুমানিক কতদিন বলা হল" to "",
        "\nঅগ্রিম কত টাকা জমা করতে চাইছে" to "", "\nরোগীর বা রোগী পক্ষের সিদ্ধান্ত" to "",
        // 🆕 (05.08.2026, TK-নির্দেশ, স্টেপ ৪ Estimate & Decision) — চারটা
        // লেবেল + Other/Remarks এখন " · " দিয়ে পাশাপাশি (আগে "\n" ছিল)।
        " · আনুমানিক খরচ" to "", " · আনুমানিক কতদিন বলা হল" to "",
        " · অগ্রিম কত টাকা জমা করতে চাইছে" to "", " · রোগীর বা রোগী পক্ষের সিদ্ধান্ত" to "",
        " · অন্যান্য / মন্তব্য" to "",
        "\nছবি ও ভিডিও" to "", "\nঅন্যান্য চিকিৎসার কথা (টাইপ করুন)" to "",
        // Treatment Plan-এর ৬টা Tick-বক্স — এগুলোয় বাংলা আগে/মাঝে থাকায়
        // শুধু সাফিক্স বাদ দিলে চলবে না, পুরো লেখাটাই পরিষ্কার ইংরেজিতে
        // বদলানো হলো (ঠিক DoctorCheckupActivity.kt-এর নতুন cb.tag-এর
        // মানের সাথে মিলিয়ে, যাতে সেভ-হওয়া মান ও দেখানো লেখা একই অর্থ বহন করে)।
        "Fistula Per ইঞ্চি" to "Fistula Per Inch",
        // 🔵 V541 (২২.০৮.২০২৬, TK-নির্দেশ): ইঞ্চি → সেমি। ⛔ উপরের পুরোনো
        //    লাইনটা **রাখা হলো**, নইলে পুরোনো রেকর্ডের লেখা ইংরেজিতে যেত না।
        "Fistula Per সেমি" to "Fistula Per CM",
        "ফিস্টুলা প্রতি সেমি" to "Fistula Per CM",
        // 🆕 (05.08.2026, TK-নির্দেশ) — "মেশিন দিয়ে কাজ হবে" ও "Machine
        // Treatment" এখন "\n"-এর বদলে " · " দিয়ে জোড়া (পাশাপাশি)।
        "মেশিন দিয়ে কাজ হবে · Machine Treatment" to "Machine Treatment",
        "Per ক্ষারসূত্র হিসাবে চিকিৎসা\nKshar Sutra" to "Kshar Sutra",
        "LIS-এর মাধ্যমে চিকিৎসা করা হবে" to "LIS Treatment",
        "Injection (Vaccination)-এর মাধ্যমে চিকিৎসা" to "Injection (Vaccination) Treatment",
        // 🆕 V501 (২১.০৮.২০২৬) — Treatment Plan-এর তিনটে লেখা এক লাইনে ধরাতে
        //    ছোট করা হয়েছে (TK-অনুমোদিত), তাই নতুন ইংরেজি এখানে যোগ করা হলো।
        //    ⛔ পুরনো সারি দুটো রেখে দেওয়া হলো — পুরোনো রেকর্ড/রিপোর্টে ওই
        //       লেখাগুলো এখনো থাকতে পারে।
        "Machine Treatment/ মেশিনে চিকিৎসা" to "Machine Treatment",
        "LIS/ এলআইএস চিকিৎসা" to "LIS Treatment",
        "Injection/ ইনজেকশন চিকিৎসা" to "Injection (Vaccination) Treatment",
        // 🆕 (05.08.2026, TK-নির্দেশ) — hint-এ "আপনি" যোগ হয়েছে, তাই এই
        // এন্ট্রিও নতুন লেখার সাথে মেলানো হলো (পুরনো লেখাটা এখন কোথাও
        // ব্যবহৃত নয়, তাও রাখা হলো, ক্ষতিহীন)।
        "রোগীকে কিভাবে চিকিৎসা করবেন বলেছেন সেই কথা এখানে লিখুন" to "Write here how the doctor will treat the patient",
        "রোগীকে আপনি কিভাবে চিকিৎসা করবেন বলেছেন সেই কথা এখানে লিখুন" to "Write here how you will treat the patient",
        // 🆕 (04.08.2026, Chamber Date-এর Treatment Progress কলাম) — ডিফল্ট
        // স্টাব রিমার্ককে "কিছু লেখা হয়নি" হিসেবে দেখানোর নতুন লেখা।
        "কিছু লেখা হয়নি — চাপুন" to "Nothing written — tap to add",
        // 🔴 V432 (TK-রিপোর্ট ১৮.০৮.২০২৬) — IN TIME/রিপোর্ট আবার পাঠানোর বোতাম
        "📤 IN TIME আবার WhatsApp-এ পাঠান" to "📤 Send IN TIME to WhatsApp again",
        "📤 রিপোর্ট আবার WhatsApp-এ পাঠান" to "📤 Send the report to WhatsApp again",
        // 🔴 V433 (TK-নির্দেশ ১৮.০৮.২০২৬) — পাঠানো হয়ে গেলে বোতাম লুকানোর ছোট প্রশ্ন
        "পাঠানো হয়েছে?" to "Sent?",
        "WhatsApp-এ পাঠানো হয়ে গেছে?" to "Has it been sent on WhatsApp?",
        "হ্যাঁ, পাঠানো হয়েছে" to "Yes, sent",
        "না, পাঠানো হয়নি" to "No, not sent",
        // 🔴 V434 (TK-রিপোর্ট ১৮.০৮.২০২৬ — "Remarks লেখার পর Save হচ্ছে না")
        "⏳ সেভ হচ্ছে…" to "⏳ Saving…",
        "লেখা রাখা হয়েছে — নেট এলে নিজে থেকেই যাবে। আবার লিখবেন না" to
            "Saved on this phone — it will go automatically when internet returns. Do not write it again",
        "নেট পাওয়া যায়নি — কিছুই সেভ হয়নি, একটু পরে আবার Save চাপুন" to
            "No internet — nothing was saved. Please press Save again in a moment",
        // 🔴 V436 (TK-রিপোর্ট ১৮.০৮.২০২৬ — পুরনো APK চুপচাপ চলছিল)
        "আপনার অ্যাপ পুরনো — নতুন ভার্সনটা বসান" to
            "Your app is out of date — please install the new version",
        // stepTitles (step-bar চিপ)
        " · বৃত্তান্ত" to "", " · পূর্ববর্তী" to "", " · ক্লিনিক্যাল" to "",
        " · পরামর্শ" to "", " · আর্থিক" to "", " · সিদ্ধান্ত" to "", " · ছবি" to "",
        // চেকবক্সের ডিসপ্লে-লেবেল (visualBn/dreBn/investigationBn, cb.text-এ)
        " · বাহ্যিক অর্শ" to "", " · ফিসার" to "", " · ফিস্টুলার মুখ" to "",
        " · রক্ত পড়ে" to "", " · ফোলাভাব" to "", " · কোমলতা/ব্যথা" to "",
        " · অভ্যন্তরীণ অর্শ" to "", " · ফিস্টুলা" to "", " · এমআরআই" to "",
        " · আল্ট্রাসাউন্ড" to "", " · কোলোনোস্কোপি" to "", " · ল্যাব রিপোর্ট" to "",
        // Grade/Patient Decision ড্রপডাউন (03.08.2026)
        " · গ্রেড I" to "", " · গ্রেড II" to "", " · গ্রেড III" to "", " · গ্রেড IV" to "",
        " · চিকিৎসায় রাজি" to "", " · রাজি নয়" to "", " · ভেবে দেখব" to "",
        " · পরিবারের সাথে আলোচনা" to "", " · আর্থিক সমস্যা" to "",
        // 🔒 B281 (02.08.2026): Refund request নোটিশ থেকে সরাসরি Approve/Reject।
        "এই নোটিশ থেকে Patient ID চেনা গেল না" to "Could not identify the Patient ID from this notice",
        "-এর জন্য এখন আর কোনো Pending Refund নেই — হয়তো আগেই Approve/Reject হয়ে গেছে, বা অন্য ফোন থেকে এখনো ক্লাউডে পৌঁছায়নি।" to
            " has no Pending Refund now — it may already be Approved/Rejected, or hasn't reached the cloud yet from another phone.",
        // 🔒 B274 (02.08.2026): "যায়নি" ঘরের এন্ট্রি স্থায়ীভাবে ছেড়ে দেওয়ার
        // নিশ্চিতকরণ পপ-আপ + Toast — নতুন যোগ, তাই অনুবাদ এখানে যোগ করা হলো।
        "স্থায়ীভাবে ছেড়ে দেবেন?" to "Give up permanently?",
        "টা তথ্য সার্ভারে খুঁজে পাওয়া যাচ্ছে না বলে কখনোই পাঠানো সম্ভব না। এগুলো আর দেখানো/চেষ্টা হবে না। রেকর্ড/টাকার হিসাব বদলাবে না।" to
            " item(s) can never be sent because the server can't find them anymore. They won't be shown or retried again. Records/money totals will not change.",
        "হ্যাঁ, ছেড়ে দিন" to "Yes, give up",
        "টা ছেড়ে দেওয়া হলো" to " item(s) given up",
        // 🔒 V221 (§1, 31.07.2026): খাতার সারি B158 — sync-স্ট্যাটাস/লাল সতর্কবার্তার
        // এই দুটো টুকরো (CloudWriteQueue.stuckDetail-এর "আরও N" ও
        // PendingSyncStatus-এর "আটকে: …") V219 §4-এ যোগ হয়েছিল কিন্তু অনুবাদ ছিল না,
        // তাই পাহারাদার ৯.১৪ ফেল করত (Bengali-off স্টাফের পর্দায় বাংলা থেকে যেত)।
        // ⛔ বাংলা চালু থাকা ব্যবহারকারীর কিছুই বদলায় না (fix() active না হলে হুবহু ফেরত)।
        "আটকে" to "stuck",
        "আরও" to "more",
        "পরে পাঠাব" to "Send later",
        "-এর অনুমতি লাগবে" to " permission is needed",
        "-এর অনুমোদন লাগবে" to " approval is needed",
        "-এর অনুমোদনের অপেক্ষায়" to " approval is awaited",
        "-এর কাছে অনুরোধ পাঠানো হয়েছে" to " has been sent the request",
        "-কে অনুরোধ পাঠানো হয়েছে" to " has been sent the request",
        "-এর ঘন্টায় অনুরোধ যাবে" to "'s bell will get the request",
        "-এর ঘন্টায় যাবে" to "'s bell will get it",
        "-এর থেকে বেশি হয়ে যাচ্ছে" to " limit is being exceeded",
        "-এ ঠিকই দেখাবে" to " will still show correctly",
        "-এ যাবে" to " will get it",
        "-এ চলে যাবে" to " it will move to",
        "-এ আছে" to " — inside",
        "-এ আছেন" to " branch",
        "নেট যাচাই করা যায়নি — এই নম্বর আগে থেকে রেজিস্টার আছে কিনা দেখা যাচ্ছে না। ডুপ্লিকেট রোগী এড়াতে একটু পরে আবার চেষ্টা করুন" to "Could not check the net — cannot tell if this number is already registered. Please try again in a moment so a duplicate patient is not created",
        "এ আছেন। একটা ব্রাঞ্চ বেছে নিন — প্রিন্টের উপরে ওই ব্রাঞ্চের ক্লিনিকের নাম বসবে এবং নিচে শুধু ওই ব্রাঞ্চের রোগীই থাকবে" to "branch. Pick one branch — that branch's clinic name will print on top and only that branch's patients will be listed below",
        "নেট ঠিকমতো কাজ করছে না, তাই এই নম্বরটা আগে থেকে রেজিস্টার করা আছে কিনা দেখা গেল না" to "The net is not working properly, so it could not be checked whether this number is already registered",
        "নেট যাচাই করা যায়নি — এই নম্বর আগে থেকে রেজিস্টার আছে কিনা দেখা যাচ্ছে না" to "Could not check the net — cannot tell if this number is already registered",
        "ডুপ্লিকেট রোগী তৈরি এড়াতে টাকা নেওয়া হয়নি। একটু পরে আবার চেষ্টা করুন" to "Money was not taken, to avoid creating a duplicate patient. Please try again in a moment",
        "এখনই সেভ করলে একই রোগীর দ্বিতীয় রেকর্ড তৈরি হয়ে যেতে পারে" to "Saving now may create a second record for the same patient",
        "উপরের ক্যালেন্ডারে চাপলে আজকের তালিকা আবার দেখতে পাবেন" to "Tap the calendar above to see today's list again",
        "এই মোবাইল নম্বরটি রেজিস্টার্ড নয়। সঠিক নম্বর দিন" to "This mobile number is not registered. Enter the correct number",
        "অনুমোদন দিলে তবেই ডিলিট হবে — এখনই কিছু মুছবে না" to "It will be deleted only after approval — nothing is removed now",
        "নিতে চাইছেন — এটা বিলের থেকে বেশি। তবুও এগোবেন" to "is being taken — this is more than the bill. Continue anyway",
        "হিসাব আনা গেল না — লাইন দেখে আবার চেষ্টা করুন" to "Could not load the figures — check the line and try again",
        "সব দিনের চেম্বার বন্ধ করা আছে — কিছু বাকি নেই" to "Every day's chamber is closed — nothing is pending",
        "এর ঘন্টায় যাবে — তিনি অনুমতি দিলে তবেই মুছবে" to "will go to the bell — it will be removed only after approval",
        "বিগত দিনের সেভ করা হিসাব — শুধু দেখা যাবে" to "Saved figures of a past day — view only",
        "নেওয়া যাবে — চাইলে নিচে বিল-ও বসিয়ে দিন" to "can be taken — you may also enter the bill below",
        "আজ কী হলো — নিজে লিখুন বা নিচের চিপ চাপুন" to "What happened today — write it yourself or tap a chip below",
        "নাম অথবা মোবাইল নম্বর — যেকোনো একটা লিখুন" to "Name or mobile number — write any one",
        "এই রোগীর আসার কথা ইতিমধ্যে দেওয়া হয়েছে" to "This patient already has an appointment date",
        "আজকের তারিখ (ডিফল্ট) — বদলাতে ট্যাপ করুন" to "Today's date (default) — tap to change",
        "অ্যাডমিন পাসওয়ার্ড রিসেটের জন্য ডিফল্ট" to "Default for admin password reset",
        "পেশেন্ট এসে আরো কি কি সমস্যার কথা বললেন" to "What other problems did the patient mention",
        "বিগত দিন — এই চেম্বার এখনো বন্ধ হয়নি" to "Past day — this chamber is not closed yet",
        "নতুন রেকর্ড হবে না — পুরনো রেকর্ডটাই" to "No new record will be created — the old record",
        "এই দিনে কেউ আসেননি — ছাপার কিছু নেই" to "Nobody came on this day — nothing to print",
        "কোন ব্রাঞ্চের রেজিস্টার প্রিন্ট হবে" to "Which branch's register will be printed",
        "কোন ব্রাঞ্চের চেম্বার বন্ধ করবেন?" to "Which branch's chamber will you close?",
        "আপনি এখন All Branch-এ আছেন। যে ব্রাঞ্চটি বন্ধ করবেন সেটি বেছে নিন — Review-তে শুধু ওই ব্রাঞ্চের রোগীরা থাকবেন।" to "You are viewing All Branches. Select the branch to close — the Review will show only that branch's patients.",
        "এর অনুমতি লাগবে। এখানে অনুরোধ পাঠান" to "permission is needed. Send the request here",
        "এই দিনগুলোর চেম্বার এখনো বন্ধ হয়নি" to "The chamber of these days is not closed yet",
        "এ ঠিকই দেখাবে) — শুধু এই পেশেন্ট আর" to "will still show correctly) — only this patient and",
        "যে সেকশনে ছিল সেখানেই থাকবে, পুরনো" to "will stay in the same section, the old",
        "এই অনুরোধ থেকে রেকর্ড চেনা গেল না" to "The record could not be identified from this request",
        "কোনো কল/রেফারেল/আয় এখনো লগ হয়নি" to "No call / referral / income logged yet",
        "প্রকৃত জমার তারিখ (যদি আজ না হয়" to "Actual payment date (if not today",
        "তিনি অনুমোদন দিলে তবেই ডিলিট হবে" to "It will be deleted only after approval",
        "পাঠানো যায়নি — আবার চেষ্টা করুন" to "Could not send — try again",
        "অনুমোদন দিলে শুধু এই পেশেন্ট আর" to "After approval, only this patient and",
        "চেম্বার বন্ধ করার আগে সেভ করুন" to "Save before closing the chamber",
        "বার ট্যাপ করুন — সম্পাদনা করতে" to "taps to edit",
        "আজকের চেম্বার বন্ধ করা হয়েছে" to "Today's chamber has been closed",
        "হিসাব এখনো আসেনি — এক মুহূর্ত" to "Figures have not arrived yet — one moment",
        "থেকে নতুন পাসওয়ার্ড সেট করুন" to "set a new password from",
        "পাসওয়ার্ড পাল্টে দিতে পারবেন" to "can change the password",
        "বিগত দিনের চেম্বার বন্ধ করুন" to "Close a past day's chamber",
        "এর কাছে অনুরোধ পাঠানো হয়েছে" to "request has been sent to",
        "পাঠানো গেল না — নেট চেক করুন" to "Could not send — check the net",
        "এ চলে যাবে (পরে ফেরানো যাবে" to "will move to (can be restored later",
        "কী কারণে বাতিল করছেন, লিখুন" to "Write why you are cancelling",
        "পেমেন্টের দিন পার হয়ে গেছে" to "The payment day has passed",
        "টি তথ্য এখনো ক্লাউডে যায়নি" to "items have not reached the cloud yet",
        "আপনার পাসওয়ার্ড রিসেট করতে" to "to reset your password",
        "কোনো আসার তারিখ ঠিক করা নেই" to "No appointment date is set",
        // 🟢 B631 (11.08.2026): বাল্ক Delete/Restore অগ্রগতি-ইঙ্গিতের নতুন লেখা
        // (DraftListActivity)। বাংলা-বন্ধ স্টাফের পর্দায় যাতে "Deleting 3/9" / "Restoring 3/9" দেখায়।
        "মুছে ফেলা হচ্ছে" to "Deleting",
        "ফেরানো হচ্ছে" to "Restoring",
        // 🔵 B620 (11.08.2026): Full Journey-তে "আসবে বলেছে" সারির নতুন লেখা
        // ("আসার তারিখ: 11.08.2026")। longest-first বলে উপরের বড় বাক্যগুলো আগেই
        // অনুবাদ হয়, তাই এই ছোট চাবি শুধু একা "আসার তারিখ"-এ লাগে (no-Bengali → "Appointment date")।
        "আসার তারিখ" to "Appointment date",
        // 🔴 TK-REPORTED FIX (01.08.2026), পার-হয়ে-যাওয়া তারিখের নতুন Toast
        // (PatientTimelineActivity.kt) — দুটো টুকরোয় ভাঙা, মাঝে আসল তারিখ বসে।
        "এই আসার তারিখ (" to "This appointment date (",
        ") পার হয়ে গেছে — আগে নতুন তারিখ ঠিক করুন" to ") has passed — please set a new date first",
        "আসার তারিখ পরিবর্তন করেছেন" to "has changed the appointment date",
        "বসানো হয়নি। বিল না বসালেও" to "is not entered. Even without a bill",
        "পাসওয়ার্ড দিয়ে লগইন করে" to "log in with the password and",
        "আসার তারিখ মনে করিয়ে দিন" to "Remind about the appointment date",
        "এর থেকে বেশি হয়ে যাচ্ছে" to "is becoming more than",
        "দ্রুত (চাপলে লেখায় বসবে" to "Quick (tap to put it in the text",
        "নতুন পেশেন্টের নাম লিখুন" to "Write the new patient's name",
        "কে অনুরোধ পাঠানো হয়েছে" to "request has been sent to",
        "উপরে ব্রাঞ্চ বাছাই করুন" to "Select a branch above",
        "চেম্বার বন্ধ হয়ে গেছে" to "The chamber is closed",
        "এর ঘন্টায় অনুরোধ যাবে" to "the request will go to the bell of",
        "অ্যাডমিনকে বলুন — তিনি" to "Tell the admin — he",
        "এর অনুমোদনের অপেক্ষায়" to "waiting for approval of",
        "কল/রিমাইন্ডারে আসবে না" to "will not come in calls / reminders",
        "মাস্টারের অনুমতি লাগবে" to "Master's permission is needed",
        "দিনের বেশি) — এখন থেকে" to "days) — from now",
        "ব্যর্থ — নেট চেক করুন" to "Failed — check the net",
        "অনুমোদন দিলে রেকর্ডটা" to "after approval the record",
        "ডিলিট করতে পারেন শুধু" to "can be deleted only by",
        "সব কাজ / পরবর্তী কাজ" to "All work / next work",
        "অনুরোধ পাঠানো হয়েছে" to "The request has been sent",
        "আসার কথা দেওয়া আছে" to "Appointment date is given",
        "এখনই কিছুই মুছবে না" to "Nothing will be deleted now",
        "পাঠানোর চেষ্টা চলছে" to "Trying to send",
        "অনুমোদনের অপেক্ষায়" to "Waiting for approval",
        "করার অনুরোধ পাঠাবেন" to "send the request to",
        "কোনো ঘরে ভুল থাকলে" to "if any field is wrong",
        "হাইড্রোসিল (একশিরা" to "Hydrocele",
        "অনুরোধ পাঠালে সেটা" to "if the request is sent then it",
        "আসার কথায় যোগ হলো" to "added to the appointment",
        "প্রিন্ট তৈরি হচ্ছে" to "The print is being made",
        "আজ কোনো লেনদেন নেই" to "No transaction today",
        "এই মাস বনাম গত মাস" to "This month vs last month",
        "এখনো এই পেশেন্টের" to "this patient still",
        "দিনের বেশি) — এখন" to "days) — now",
        "চেম্বার বন্ধ করুন" to "Close the chamber",
        "ডিজিট মোবাইল দিন" to "digit mobile number",
        "এর অনুমোদন লাগবে" to "approval is needed",
        "কেন বদলাতে হচ্ছে" to "Why it has to be changed",
        "এই রেকর্ডটা খুলে" to "opening this record",
        "ভগন্দর (ফিস্টুলা" to "Fistula",
        "ব্রাঞ্চে আনা হলো" to "moved to branch",
        "অপরিবর্তিত থাকবে" to "will stay unchanged",
        "লিস্টে দেখা যাবে" to "will be seen in the list",
        "বেশি হয়ে যাচ্ছে" to "is becoming more",
        "যাচাই করা গেল না" to "could not be checked",
        "কোনো এন্ট্রি নেই" to "No entry",
        "আজ কল বাকি দেখুন" to "See today's pending calls",
        "সম্পূর্ণ ইতিহাস" to "Full history",
        "ট্যাপ করে বদলান" to "tap to change",
        "ক্লিয়ার করা হল" to "cleared",
        "ঠিক করুন। তারপর" to "Fix it. Then",
        "এর অনুমতি লাগবে" to "permission is needed",
        "তৈরি করা যায়নি" to "could not be created",
        "জন দেখানো হচ্ছে" to "people shown",
        "আসলেই ডিলিট হবে" to "It will really be deleted",
        "লিস্টে চলে যাবে" to "will move to the list",
        "বিস্তারিত দেখুন" to "See details",
        "রোগের বিস্তারিত" to "Disease details",
        "আসতে পারবেন না" to "Cannot come",
        "আসার কথা বাতিল" to "Appointment cancelled",
        "করতে পাঠানো হল" to "sent for",
        "এনকোয়ারি বন্ধ" to "Enquiry closed",
        "পেশেন্ট খুঁজুন" to "Search patient",
        "এর কাজ করা হল" to "work done",
        "চেপে মুছে দিন" to "press to delete",
        "পাঠানো হয়েছে" to "has been sent",
        "বাতিল করা হলো" to "has been cancelled",
        "দেওয়া হয়েছে" to "has been given",
        "ব্রাঞ্চে আসবে" to "will come to branch",
        "একটি পেমেন্টে" to "one payment",
        "পাঠানো যায়নি" to "could not be sent",
        "তবুও সেভ করুন" to "Save anyway",
        "চেম্বারের দিন" to "Chamber day",
        "তালিকা ফাঁকা" to "The list is empty",
        "অনুরোধ পাঠান" to "Send request",
        "করানো হয়েছে" to "done",
        "দেওয়া হয়নি" to "not given",
        "লেখা হয়েছিল" to "was written",
        "বার্তা পাঠান" to "Send message",
        "রেজিস্ট্রেশন" to "Registration",
        "প্রেসক্রিপশন" to "Prescription",
        "লাইন ঠিক হলে" to "when the line is fine",
        "সেরা চিকিৎসা" to "best treatment",
        "তারিখ বাছুন" to "Pick a date",
        "জন এসেছিলেন" to "people came",
        "তারিখ বদলান" to "Change the date",
        "এখানে লিখুন" to "Write here",
        "হ্যাঁ, এগোন" to "Yes, continue",
        "পাইলস (অর্শ" to "Piles",
        "লিস্ট দেখতে" to "to see the list",
        "করানো হয়নি" to "not done",
        "বাধ্যতামূলক" to "Mandatory",
        "লিস্টে যাবে" to "will go to the list",
        "কোন পেমেন্ট" to "Which payment",
        "প্রকৃত জমা" to "Actual collection",
        // 🔴🔒 V1442 (১৩.০৯.২০২৬) — Refund-এর ব্যাকডেট ঘরের "প্রকৃত ফেরত: DD/MM/YYYY"।
        "প্রকৃত ফেরত" to "Actual refund",
        "নতুন তারিখ" to "New date",
        "কারণ লিখুন" to "Write the reason",
        "হিসাব আসছে" to "Figures are coming",
        "থেকে আপনার" to "from your",
        "বাতিল করুন" to "Cancel",
        "টি পেমেন্ট" to "payments",
        "আবার দেখুন" to "See again",
        "মোট বকেয়া" to "Total due",
        "বসাতে চান" to "want to enter",
        "অন্য কারণ" to "Other reason",
        "বাতিল হলো" to "cancelled",
        "এখনো বাকি" to "still pending",
        "দেওয়া হল" to "given",
        "বার চাপুন" to "taps",
        "বন্ধ করুন" to "Close",
        "লোড হচ্ছে" to "Loading",
        "তারিখ দিন" to "Enter the date",
        "থেকে যাবে" to "will remain",
        "এনকোয়ারি" to "Enquiry",
        "গুপ্ত রোগ" to "Gupt Rog",
        "সেভ করুন" to "Save",
        "আপনি এখন" to "You are now",
        "ইতিমধ্যে" to "already",
        "এখন পরের" to "now next",
        "আসার কথা" to "Appointment",
        "অন্য কাজ" to "other work",
        // 🔒 খাতার সারি B170 (30.07.2026): PendingSyncStatus-এর নতুন লেবেল।
        "চেম্বার ক্লোজ" to "Chamber Close",
        "করা হলো" to "done",
        "পাঠাবেন" to "will you send",
        "নাম দিন" to "Enter the name",
        "পেমেন্ট" to "Payment",
        "চেম্বার" to "Chamber",
        "ব্রিফিং" to "Briefing",
        "গত মাসে" to "last month",
        "অনুরোধ" to "request",
        "এসেছেন" to "arrived",
        "করা হল" to "done",
        "করেছেন" to "has done",
        "বদলাতে" to "to change",
        "এ যাবে" to "will go to",
        "অনুমতি" to "permission",
        "বার্তা" to "message",
        "ফলো-আপ" to "Follow-up",
        "সংশোধন" to "correction",
        "ভগন্দর" to "Fistula",
        "একশিরা" to "Hydrocele",
        "উপসর্গ" to "Symptoms",
        "বাছুন" to "select",
        "তারিখ" to "date",
        "এ আছে" to "is in",
        "ফিশার" to "Fissure",
        "পুরনো" to "old",
        "করবেন" to "will do",
        "ধ্বংস" to "destroy",
        "বাতিল" to "cancel",
        "চাপুন" to "tap",
        "পাঠান" to "send",
        "বাংলা" to "Bengali",
        "সঠিক" to "correct",
        "কারণ" to "reason",
        "আপনি" to "you",
        "থেকে" to "from",
        "বন্ধ" to "closed",
        "হয়ে" to "being",
        "অটুট" to "intact",
        "করুন" to "do",
        "বাকি" to "pending",
        "অর্শ" to "Piles",
        "দিন" to "day",
        "জমা" to "collected",
        "মোট" to "total",
        "মুছ" to "delete",
        "জন" to "people",
        "টি" to "items",
        "এর" to "of",
        "ভগন্দর (ফিস্টুলা) হলো মলদ্বারের ভেতরের অংশ ও তার পাশের চামড়ার মধ্যে তৈরি হওয়া একটি অস্বাভাবিক নালি। এটি সাধারণত মলদ্বারের কোনো ফোঁড়া (সংক্রমণের পকেট) সম্পূর্ণ না সারলে তৈরি হয়, যার ফলে একটি নালি থেকে যায় যার মধ্য দিয়ে পুঁজ বা তরল ক্রমাগত বের হতে থাকে। সময়ের সাথে সাথে এই নালি শাখা-প্রশাখায় জটিল হয়ে উঠতে পারে বলে, অর্শ বা ফিশারের চেয়ে ভগন্দরের চিকিৎসায় বেশি পরিকল্পিত পদ্ধতি লাগে — উপযুক্ত ক্ষেত্রে ঐতিহ্যবাহী আয়ুর্বেদিক ক্ষারসূত্র (ওষুধযুক্ত সুতো) পদ্ধতি ব্যবহার করা হয়" to "",
        "ফিশার হলো মলদ্বারের পাতলা, নরম আবরণে হওয়া একটি ছোট ছিঁড়ে যাওয়া বা ফাটল। এটি সাধারণত শক্ত বা বড় মল ত্যাগের কারণে হয়, যা মলদ্বারের নালিকে তার স্বাভাবিক ক্ষমতার চেয়ে বেশি টেনে ধরে। এই ফাটলের কারণে ভেতরের সংবেদনশীল টিস্যু ও মাংসপেশি উন্মুক্ত হয়ে যায়, ফলে মলত্যাগের সময় ও পরে তীব্র কাটা কাটা ব্যথা হয় যা কয়েক মিনিট থেকে কয়েক ঘণ্টা পর্যন্ত থাকতে পারে। দীর্ঘদিনের ফিশারে চারপাশের পেশিতে খিঁচুনি হতে পারে, যার ফলে সঠিক যত্ন ছাড়া সারতে দেরি হয়" to "",
        "অর্শ (পাইলস) হলো মলদ্বার ও মলদ্বারের নিচের অংশের শিরাগুলো ফুলে যাওয়া একটি সমস্যা। বারবার চাপ পড়া বা কোষ্ঠকাঠিন্যের কারণে এই শিরাগুলো ধীরে ধীরে ফুলে ওঠে। অর্শ দুই রকম হতে পারে — অভ্যন্তরীণ (ভেতরের দিকে, সাধারণত ব্যথাহীন কিন্তু রক্ত পড়তে পারে) এবং বাহ্যিক (মলদ্বারের চারপাশে চামড়ার নিচে, প্রায়ই ব্যথা ও চুলকানি সহ)। অবহেলা করলে ধীরে ধীরে সমস্যা বাড়তে থাকে, তাই প্রথম দিকেই আয়ুর্বেদিক চিকিৎসা নেওয়া জরুরি" to "",
        /* 🆕🔒 V566 (২২.০৮.২০২৬) — পাহারাদারের যাচাই ৯.১৪-এ ধরা পড়া বাকি সব
           বাংলা লেখার ইংরেজি। এর মধ্যে আছে V554–V558-এ যোগ হওয়া চেক-আপ পর্দার
           (কাগজের ভাগ ২–৬) লেখা, V562-এর RMP কমিশনের লাল লাইন, V566-এর "অসময়"
           চিহ্ন, আর হাজিরা/রেজিস্ট্রেশনের কয়েকটা পুরোনো বার্তা।
           ⛔ শুধু বাংলা-বন্ধ (Kishanganj) স্টাফের **পর্দায় কী দেখাবে** সেটুকুই —
              জমা হওয়া তথ্য, টাকার হিসাব, কোনো নিয়ম একটুও বদলায়নি (সারি B158)। */
        // — হাজিরা (AttendanceRepository-তে নিজস্ব তালিকাও আছে, এখানে একই ইংরেজি) —
        "আপনার প্রোফাইল পাওয়া যায়নি। মাস্টারকে জানান।" to "Your profile was not found. Please inform the Master.",
        "আজ আপনার ছুটি অনুমোদিত — হাজিরা লাগবে না।" to "Your leave for today is approved - no attendance needed.",
        "হাজিরা বসানো গেল না। আবার চেষ্টা করুন।" to "Attendance could not be marked. Please try again.",
        "এই অ্যাকাউন্টে হাজিরার ব্যবস্থা নেই।" to "Attendance is not used for this account.",
        "আজ আগেই হাজিরা হয়েছে — " to "Attendance was already marked today - ",
        "। দিনে একবারই দেওয়া যায়।" to ". It can only be given once a day.",
        // — রেজিস্ট্রেশনের একই-নম্বর সতর্কতা (মাঝখানে সংখ্যা/নাম বসে, তাই টুকরো ধরে) —
        "এই মোবাইল নম্বরটা ইতিমধ্যে " to "This mobile number is already registered to ",
        " জন রোগী আছেন:\n" to " more patients:\n",
        "নতুন আলাদা রোগী?" to "A new, separate patient?",
        "এই নম্বরে আরও " to "This number has ",
        "হ্যাঁ চাপলে সম্পূর্ণ নতুন একজন রোগী তৈরি হবে — পুরোনো রোগীর " to "Pressing Yes will create a completely new patient - the old patient's ",
        "কোনো তথ্য বদলাবে না, আর নতুন রোগীর নিজের Visit Fee কাটবে।" to "data will not change, and the new patient's own Visit Fee will apply.",
        "\" কি সত্যিই অন্য একজন রোগী (যেমন স্বামী / স্ত্রী / পরিবারের অন্য কেউ)?" to "\" - is this really a different patient (husband / wife / another family member)?",
        "-এর নামে আছে।\n\n" to " .\n\n",
        "হ্যাঁ, আলাদা রোগী" to "Yes, separate patient",
        "একজন রোগীর" to "another patient",
        // — V562: RMP কমিশন আলাদা লাল লাইনে · V566: অসময়ের রোগীর চিহ্ন —
        "দিতে হবে (মোট)" to "To be paid (total)",
        "অসময়" to "Off-hours",
        // — V605: কল-রিমার্কস (Incoming/Outgoing) —
        "কল নিয়ে যা বললেন লিখুন…" to "Write what was discussed on the call...",
        // — V621: Return Fees (Visit Card) —
        "লোড হচ্ছে…" to "Loading…",
        "ফেরতযোগ্য Fees নেই এই রোগীর" to "No refundable Fees for this patient",
        "আজকের চেম্বার বন্ধ হয়ে গেছে — এখন Master-এর অনুমতি লাগবে" to "Today's chamber is already closed — Master's permission is now needed",
        "⚠️ Return Fees — স্থায়ী" to "⚠️ Return Fees — permanent",
        "ফেরত দেওয়া হবে।\n" to " will be refunded.\n",
        "এই Visit \"Return Visit\" তালিকায় (Draft) সরে যাবে — Chamber Date-সহ সক্রিয় তালিকা থেকে বাদ পড়বে।\n\n" to "This Visit will move to the \"Return Visit\" list (Draft) — it will drop out of active lists including Chamber Date.\n\n",
        "সত্যিই এগোতে চান?" to "Are you sure you want to proceed?",
        "হ্যাঁ, Return করুন" to "Yes, Return it",
        "ব্যর্থ — আবার চেষ্টা করুন" to "Failed — please try again",
        "✅ Fees ফেরত হলো — Return Visit-এ সরানো হলো" to "✅ Fees refunded — moved to Return Visit",
        "Fees ফেরত হয়েছে, কিন্তু Return Visit-ট্যাগ ব্যর্থ — Draft-এ হাতে ঠিক করুন" to "Fees refunded, but tagging Return Visit failed — please fix manually in Draft",
        // 🟢🔒 V676 (২৫.০৮.২০২৬) — Doctor Checkup আজকের-এডিট পপ-আপ।
        "আজকের Check-up" to "Today's Check-up",
        // 🟢🔒 V687 (২৫.০৮.২০২৬) — Chamber বন্ধ করার আগে আজকের Treatment Progress বাধ্যতামূলক।
        "আজকের Treatment Progress লেখা হয়নি — না লিখলে চেম্বার বন্ধ করা যাবে না" to "Today's Treatment Progress hasn't been written — chamber can't be closed until it is",
        // 🔴🔒 V938 (০১.০৯.২০২৬) — পাহারাটা Review-এর Confirm-এ সরে যাওয়ায় নতুন দুটো লেখা।
        "আজকের Treatment Progress লেখা হয়নি — না লিখলে সেভ · শেয়ার · প্রিন্ট কিছুই হবে না" to "Today's Treatment Progress hasn't been written — nothing will be saved, shared or printed until it is",
        "আজকের চেম্বার বন্ধ করবেন? পরের পর্দায় সবটা দেখে নিতে পারবেন।" to "Close today's chamber? You can check everything on the next screen.",
        // 🔴🔒 V940 (০১.০৯.২০২৬) — একই নম্বর অন্য ব্রাঞ্চে থাকলে জিজ্ঞাসা।
        "এই নম্বর অন্য ব্রাঞ্চের জন্য আগেই সেভ করা আছে। এই ব্রাঞ্চের জন্যও আলাদাভাবে সেভ করতে চান?" to "This number is already saved for another branch. Save it separately for this branch too?",
        "আজকে কি TREATMENT করা হল · আজ এই রোগীর কী কাজ হলো" to "What treatment was done today for this patient",
        /* 🔒 V996 — শিরোনামটা এখন দু লাইনে (ইংরেজি উপরে · বাংলা নিচে),
           তাই একই অনুবাদ নতুন রূপ দুটোতেও বসানো হলো — পর্দার লেখা
           (আসল newline) আর XML-এর কাঁচা লেখা (\\n) দুটোই। */
        "আজকে কি TREATMENT করা হল\nআজ এই রোগীর কী কাজ হলো" to "What treatment was done today for this patient",
        "আজকে কি TREATMENT করা হল\\nআজ এই রোগীর কী কাজ হলো" to "What treatment was done today for this patient",
        "আজকের চেকআপ আগেই সেভ করা আছে — দেখবেন নাকি এডিট করবেন?" to "Today's check-up is already saved — view it or edit it?",
        // — V618: মিশ্র পেমেন্ট এডিটর (Chamber Date + Payment) —
        "মিশ্র পেমেন্ট — বিস্তারিত এডিটরে নিয়ে যাওয়া হচ্ছে…" to "Mixed payment — opening the detailed editor…",
        "এই দিনের মিশ্র পেমেন্ট বদলাতে এখন Master-এর অনুমতি লাগবে (আজ/গতকাল পার হয়ে গেছে)।" to "Changing this day's mixed payment now needs Master's permission (past today/yesterday).",
        // — V616: Change Branch (Master-only) —
        "খোঁজা হচ্ছে…" to "Searching…",
        "কোনো রেকর্ড পাওয়া যায়নি — নেট চেক করুন" to "No records found — check connection",
        "এখনকার ব্রাঞ্চ: " to "Current branch: ",
        "মোট সারি সরবে: " to "Total rows to move: ",
        "নতুন ব্রাঞ্চ বেছে নিন:" to "Select new branch:",
        "⚠️ এই ব্রাঞ্চ-বদল স্থায়ী" to "⚠️ This branch change is permanent",
        "টা সারি সরবে (Patient/Follow-up/Payment)।\n\n" to " row(s) will move (Patient/Follow-up/Payment).\n\n",
        "✅ Patient ID নতুন ব্রাঞ্চ অনুযায়ী নতুন করে বানানো হবে — " to "✅ A new Patient ID will be generated to match the new branch — ",
        "⛔ পুরনো ছাপা কাগজ/প্রেসক্রিপশন/রসিদে যে ID ছাপা ছিল, সেটা আর মিলবে না।\n\n" to "⛔ the ID printed on any earlier paper, prescription, or receipt will no longer match.\n\n",
        "নতুন ID: " to "New ID: ",
        "সত্যিই এগোতে চান?" to "Are you sure you want to proceed?",
        "হ্যাঁ, সরান" to "Yes, move it",
        "টা সারি " to " row(s) ",
        "-এ সরানো হলো" to " — moved",
        "টা সরেছে, " to " moved, ",
        "টা ব্যর্থ — আবার চেষ্টা করুন" to " failed — please try again",
        // — V606: কল-রিমাইন্ডার নোটিফিকেশন (বাংলায়, স্পষ্টতার জন্য) —
        " টা আরও আছে" to " more",
        "📞 বকেয়া কল আছে" to "📞 Pending Calls",
        "📞 আজকের কল বাকি" to "📞 Today's Pending Calls",
        " টা কল বাকি — " to " calls pending — ",
        " টা পুরনো। দেখতে চাপুন।" to " overdue. Tap to view.",
        " টা কল আজ বাকি — দেখতে চাপুন।" to " calls pending today — tap to view.",
        "📞 বকেয়া কল (" to "📞 Pending Calls (",
        " টা · " to " · ",
        " টা পুরনো)" to " overdue)",
        "📞 আজকের বাকি কল (" to "📞 Today's Pending Calls (",
        " টা)" to ")",
        "দেখতে চাপুন — " to "Tap to view — ",
        "ছবি দেখিয়ে রোগীকে যা বোঝালেন, দরকার হলে এখানে লিখুন" to "What you explained to the patient with the picture - write it here if needed",
        "এই ইতিহাস নিয়ে আর কিছু লেখার থাকলে এখানে লিখুন" to "If there is more to note about this history, write it here",
        "রোগের ছবি · রোগীকে দেখিয়ে বোঝানোর জন্য" to "Disease picture - to show and explain to the patient",
        "রোগী নিজে যা যা বললেন — এখানে টাইপ করুন" to "What the patient said in their own words - type here",
        "এছাড়া অন্য কিছু থাকলে এখানে লিখুন" to "If there is anything else, write it here",
        "আঙুল দিয়ে দেখে আর যা পাওয়া গেল" to "Other findings from the finger examination",
        "রোগী এসে প্রথমে কি কি বললেন?" to "What did the patient say first?",
        "Patient Said · রোগী যা বললেন" to "Patient Said",
        "অন্যান্য থাকলে এখানে লিখুন" to "Write anything else here",
        "B. আঙুল দিয়ে দেখে · DRE" to "B. Finger examination - DRE",
        "দৈনিক জল পানের পরিমাণ" to "Daily water intake",
        "কতদিন সময় চাওয়া হল?" to "How much time was asked for?",
        "রোগীর বলা ইতিহাস" to "History as told by the patient",
        "সম্ভাব্য কি রোগ?" to "Probable disease?",
        // 🟢🔒 V651 (২৫.০৮.২০২৬, TK-নির্দেশ) — Estimated Cost ঘর সরানোর পরে
        // Step 4-এর নতুন শিরোনাম, বাংলা-বন্ধ স্টাফের জন্য ইংরেজি অনুবাদ।
        // 🟢🔒 V656 (২৫.০৮.২০২৬) — Doctor Note & Reminder-এর ইংরেজি অনুবাদ।
        "পরের বার রোগীকে কী ওষুধ/কী কাজ করা হবে — এখানে লিখুন" to "Note what to do / which medicine next time",
        "কোন দিনের আগের দিন মনে করাবে?" to "Remind one day before which date?",
        "Probable Disease and Time Asked · সম্ভাব্য রোগ ও সময়" to "Probable Disease and Time Asked",
        /* 🔒 V996 — শিরোনামটা এখন দু লাইনে (ইংরেজি উপরে · বাংলা নিচে),
           তাই একই অনুবাদ নতুন রূপ দুটোতেও বসানো হলো — পর্দার লেখা
           (আসল newline) আর XML-এর কাঁচা লেখা (\\n) দুটোই। */
        "Probable Disease and Time Asked\nসম্ভাব্য রোগ ও সময়" to "Probable Disease and Time Asked",
        "Probable Disease and Time Asked\\nসম্ভাব্য রোগ ও সময়" to "Probable Disease and Time Asked",
        // 🟢🔒 V651 — Estimated Cost এখন Step 3-এ, নতুন লেবেল-টেক্সট।
        "Estimated Cost · আনুমানিক খরচ কত বলা হল" to "Estimated Cost",
        // 🟢🔒 V656 (২৫.০৮.২০২৬) — Doctor Reminder নোটিফিকেশনের অনুবাদ।
        "🔔 আগামীকালের রোগী-নোট" to "🔔 Tomorrow's Patient Note",
        // 🟢🔒 V671 (২৫.০৮.২০২৬) — সময়-বাছাইয়ের placeholder।
        "সময় বাছুন" to "Pick time",
        "এখানে টাইপ করুন" to "Type here",
        "রোগ ও অভ্যাস" to "Conditions and habits",
        "লিটার" to "litre",
        "হাইড্রোসিল হলো অণ্ডকোষের চারপাশের পাতলা থলিতে স্বচ্ছ তরল জমে ফোলা তৈরি হওয়ার একটি সমস্যা। প্রাপ্তবয়স্কদের ক্ষেত্রে এটি সাধারণত ব্যথাহীন হয়, তবে দিনের শেষে বা দীর্ঘক্ষণ দাঁড়িয়ে থাকার পরে ভারী লাগা বা টানটান অস্বস্তি অনুভূত হতে পারে। হাইড্রোসিল কোনো স্পষ্ট কারণ ছাড়াই হতে পারে, অথবা সামান্য আঘাত, সংক্রমণ বা প্রদাহের পরে দেখা দিতে পারে — সঠিক পরীক্ষা করলে অণ্ডকোষ ফোলার অন্য কারণগুলো বাদ দেওয়া যায়" to "",
        // 🔴🔒 V1649 (২১.০৯.২০২৬) — SuspendedActivity.kt (পূর্ণ-পর্দা suspend/removed বার্তা)।
        "আপনার অ্যাকাউন্ট মাস্টার বন্ধ করে দিয়েছেন। লগইন করা যাবে না।" to "Your account has been removed by the master. You cannot log in.",
        "আপনার অ্যাকাউন্ট মাস্টার সাময়িকভাবে বন্ধ করে দিয়েছেন। লগইন করা যাবে না।" to "Your account has been temporarily suspended by the master. You cannot log in.",
        "স্টাফ কোড: " to "Staff code: ",
        "বন্ধ থাকবে: " to "Suspended till: ",
        " পর্যন্ত" to "",
        "কারণ: " to "Reason: ",
        "কিছু লেখা হয়নি" to "No reason was written",
        "📞 সাহায্যের জন্য কল করুন" to "📞 Call for help",
        "ঠিক আছে, বুঝেছি" to "OK, understood",
    )

    /**
     * 🔤🔒 V730 (২৭.০৮.২০২৬, TK-নির্দেশ) — **এই তালিকা এখন আর হিন্দি নয়,
     *    ইংরেজি।**  TK: *"হিন্দি লিখতে হবে না, শুধু ইংরেজিতে হবে।"*
     * ------------------------------------------------------------------
     * নামটা (`HINDI`) ইচ্ছে করেই বদলানো হয়নি — নাম বদলালে ফাইলের আরও অনেক
     * জায়গা ছুঁতে হত, তাতে ঝুঁকি বাড়ত। **কাজের নিয়ম এক অক্ষরও বদলায়নি**:
     * এই তালিকা আগের মতোই MAP-এর **আগে** বসে, চাবিগুলো হুবহু আগেরটাই, ক্রমও
     * এক। শুধু **মান**গুলো হিন্দি থেকে ইংরেজি হয়েছে।
     *
     * কীভাবে প্রতিটা ইংরেজি বাছা হয়েছে (যন্ত্র দিয়ে, আন্দাজে নয়):
     *   ১. চাবিটা MAP-এও থাকলে → **MAP-এর মানই** বসানো হয়েছে (১৪১টা)।
     *      কারণ লেবেলগুলো "English · বাংলা" ধাঁচে লেখা, MAP সেখানে বাংলা
     *      অংশটা **মুছে** দেয়। নিজে অনুবাদ বসালে "Other · Other" ধাঁচে
     *      লেখা **দুবার** উঠত — সেই ফাঁদ এড়ানো হয়েছে।
     *   ২. রোগের চারটে বড় বিবরণ → `DiseaseCatalog`-এর নিজের `descEn`
     *      (৪টা)। MAP-এ ওগুলোর মান ফাঁকা "" — সেটা বসালে গোটা অনুচ্ছেদ
     *      **উধাও** হয়ে যেত।
     *   ৩. বাকিগুলোর নতুন ইংরেজি লেখা হয়েছে (১৯১টা)।
     *   ৪. যে ১৭টায় আগেই ইংরেজি ছিল — হুবহু রাখা হয়েছে।
     *
     * যাচাই (V730): প্রজেক্টের ১৩০৮টা বাংলা লেখা পুরনো ও নতুন — দুই নিয়মে
     * চালিয়ে মেলানো হয়েছে। **নতুন করে একটাও বাংলা অক্ষর মুছে যায় না**,
     * এই তালিকার একটা মানেও আর হিন্দি নেই।
     * ⚠️ পর্দার **উৎস লেখাতেই** যেখানে হিন্দি লেখা আছে (রোগের চিপ
     *    "অর্শ · बवासीर", ডায়েট-চার্টের HTML) সেগুলো ছোঁয়া হয়নি — ওটা
     *    ডিজাইনের অংশ, বদলাতে হলে TK-এর অনুমোদন লাগবে।
     *
     * ══════════════════ নিচে V575-এর পুরোনো নোট (ইতিহাস) ══════════════
     * 🔵 V575 — **ডাক্তার চেক-আপের লেখা হিন্দিতে** (TK-নির্দেশ ২৩.০৮.২০২৬)।
     * ------------------------------------------------------------------
     * TK: *"কিষানগঞ্জ all Staff এর কাছে যেন হিন্দি তে থাকে … আমি তো আপনাকে
     * ডাক্তার চেকাপে হিন্দিতে রাখতে বললাম"*। TK-এর বাছাই: **শুধু কিষানগঞ্জের
     * Staff-এর কাছে**, এবং **শুধু পর্দা** — ছাপার A4 কাগজ আগের মতোই থাকবে।
     *
     * কী বদলাল: চেক-আপের লেবেলগুলো কোডে লেখা আছে "English · বাংলা" ধাঁচে।
     * আগে কিষানগঞ্জ Staff-এর পর্দায় " · বাংলা" অংশটা **মুছে** দেওয়া হত
     * (নিচের MAP-এ ওগুলোর মান ফাঁকা "" ছিল)। এখন মোছার বদলে ওখানে **হিন্দি**
     * বসে — অর্থাৎ "Chief Complaint · मुख्य समस्या"।
     *
     * ⛔ এই তালিকায় **শুধু চেক-আপের নিজস্ব লেখা** — তাই অন্য কোনো পর্দার
     *    লেখা বদলায় না, আগের মতোই ইংরেজি থাকে (TK-এর বাছাই অনুযায়ী)।
     * ⛔ কে দেখবে তার নিয়ম (`refresh()`) এক অক্ষরও বদলায়নি।
     * ⛔ ডেটাবেস · টাকার হিসাব · ডিজাইন কিছুই ছোঁয়া হয়নি।
     * ⚠️ ওয়েবের `app.js` (WLV1_NOBN_HI) এই একই তালিকার যমজ — একটাই উৎস
     *    থেকে বানানো, তাই দুই জায়গায় দু-রকম হতে পারে না।
     */
    private val HINDI: Map<String, String> = mapOf(
        /* 🔤🔒 V729 (২৭.০৮.২০২৬, TK-নির্দেশ) — TK: *"হিন্দি লিখতে হবে না,
           শুধু ইংরেজিতে হবে।"*  V728-এ যোগ করা চারটে **ইংরেজি→হিন্দি** জোড়া
           (Clock position · All marks on the picture… · Picture name ·
           ⚠️ If you approve… Trash Bin…) এখান থেকে **তুলে নেওয়া হলো**।
           ⇒ এখন ওই চারটে লেখা কিশানগঞ্জেও **ইংরেজিই** থাকবে।
           ⛔ শুধু ওই চারটেই তোলা হয়েছে — চেক-আপের পুরোনো হিন্দি তালিকা
              (TK V575-অনুমোদিত) এক অক্ষরও ছোঁয়া হয়নি। */
        /* ═══════════════════════════════════════════════════════════════════
           🟢🔒 V591 (২৩.০৮.২০২৬, TK-অনুমোদিত) — কিশানগঞ্জের স্টাফের পর্দা থেকে
           **ভাঙা লেখা** সরানো। TK: *"কিশানগঞ্জের যেকোনো স্টাফের কাছে যেন বাংলা
           পর্দা কোথাও না থাকে — বাংলার পরিবর্তে হয় হিন্দি অথবা ইংলিশ।"*

           বাংলা অক্ষর আগেও পর্দায় উঠত না (শেষ জাল মুছে দেয়), কিন্তু **অনুবাদ না
           থাকলে লেখাটাই ভেঙে বা ফাঁকা** হয়ে যেত — "উচ্চ রক্তচাপ" হত পুরো ফাঁকা,
           স্টাফ কিছুই বুঝতেন না। চালিয়ে মেপে দেখা গেছে এমন **২০৬টা** লেখা
           সত্যিই পর্দায় আসে (বাকিগুলো ছাপার কাগজ/WhatsApp — সেখানে সুইপ চলে না)।

           ⛔ **কেন সবগুলো এই এক তালিকাতেই:** এই তালিকা সবার আগে চলে, আর ভিতরে
              লম্বা চাবি আগে মেলে। আলাদা তালিকায় রাখলে ছোট একটা শব্দ আগে বদলে
              গিয়ে লম্বা বাক্যটা ভেঙে দিত (চালিয়ে ধরা পড়েছে, আন্দাজ নয়)।
           ⛔ মান হিন্দি না ইংরেজি — পর্দা অনুযায়ী: চেক-আপ ও ক্লিনিক্যাল লেখা
              **হিন্দিতে** (TK-এর V575-এর নিজের নিয়ম), সাধারণ পর্দা (Settings ·
              Registration · Dialer · Export · হাজিরা) **ইংরেজিতে**।
           ⛔ **শুধু যোগ** — বাকি সব স্টাফের পর্দা এক অক্ষরও বদলায় না।
           ═══════════════════════════════════════════════════════════════════ */
        "গোল" to "Circle",
        "হার" to "Rate",
        "নালী" to "Tract",
        "ফিসার" to "Fissure",
        "চিহ্ন" to "Mark",
        /* 🟢 V591 — সংখ্যা বসা ঘরগুলোর সঙ্গী টুকরো (AnatomyModel.readable)।
           ⛔ "ফোলা " শেষে ফাঁকা রাখা হয়েছে, তাই "ফোলান" · "ফোলাভাব" · "ফোলার"
              কখনো এই নিয়মে পড়ে না। */
        "ফোলা " to "Swelling ",
        /* 🟢 V591 — সরানো ছবি ফেরানোর ঘর (DoctorCheckupActivity)। */
        "ফেরানো গেল না — ইন্টারনেট দেখে আবার চেষ্টা করুন" to
            "Could not restore — check the internet and try again",
        "এই ফোনে ফিরল — ক্লাউডে পরে যাবে" to "Restored on this phone — will go to the cloud later",
        "সরানো ছবি ফেরান" to "Restore removed picture",
        "ফেরানো হচ্ছে…" to "Restoring…",
        "ফিরে এসেছে" to "Restored",
        "ফেরান" to "Restore",
        /* 🟢 V591 — আরও চারটে পর্দার লেখা, নিজের কাজ আবার খুঁটিয়ে দেখে পাওয়া
           (আগের 206-এর তালিকায় ধরা পড়েনি, কিন্তু পর্দাতেই দেখায়)। */
        "অনেকদিন অ্যাপ ব্যবহার হয়নি, তাই নিরাপত্তার জন্য লগআউট করা হলো। মোবাইল ও পাসওয়ার্ড দিয়ে আবার লগইন করুন।" to
            "The app was unused for a long time, so you were logged out for safety. Log in again with mobile and password.",
        "ডাক্তারদের জন্য হাজিরার ব্যবস্থা নেই — আপনি যেকোনো সময় আসতে ও যেতে পারেন।" to
            "Doctors have no attendance - you may come and go at any time.",
        "ফেরানো হচ্ছে" to "Restoring",
        "অসময়" to "Off-hours",
        "নেট ঠিকমতো কাজ করছে না, তাই এই নম্বরটা আগে থেকে রেজিস্টার করা আছে কিনা দেখা গেল না।" to
            "The internet is not working properly, so it could not be checked whether this number is already registered.",
        /* 🟢 V591 — ওয়েবের ফেরানোর ঘরের নিজস্ব লেখা (app.js-এর যমজ)। */
        "কোন ছবিটা ফেরাবেন? নম্বর লিখুন" to "Which picture to restore? Enter the number",
        "এই ব্রাউজারে ফিরল — ক্লাউডে পরে যাবে" to "Restored in this browser — will go to the cloud later",
        "সরানো কোনো ছবি নেই" to "No removed picture",
        "ছবিটা পাওয়া গেল না" to "Picture not found",
        "নম্বরটা মিলল না" to "The number did not match",
        /* 🟢 V591 — "ফেরানো যাবে" কথাটা যে দুই বার্তায় আগে থেকেই ছিল, সেগুলোও
           পুরো অনুবাদ হলো (আগে অর্ধেক মুছে গিয়ে ভাঙা দেখাত)। */
        "অনুমোদন দিলে রেকর্ডটা Trash Bin-এ চলে যাবে (পরে ফেরানো যাবে)।" to
            "If you approve, the record goes to the Trash Bin (it can be restored later).",
        "এটি হিসাব থেকে বাদ যাবে। চিরতরে মুছবে না — দরকারে ফেরানো যাবে।" to
            "This will be left out of the accounts. It will not be deleted forever — it can be restored if needed.",
        "মাংস ফোলানো" to "Flesh swelling",
        "নালীর দাগ" to "Tract mark",
        "ফোলা:" to "Swelling:",
        "ক্ষার সূত্র" to "Kshar Sutra",
        "ফোলান" to "Swell",
        "মুছুন" to "Erase",
        "এমআরআই" to "MRI",
        "◀ আগের" to "◀ Previous",
        "ফোলাভাব" to "Swelling",
        "গ্রেড I" to "Grade I",
        "গ্রেড II" to "Grade II",
        "গ্রেড III" to "Grade III",
        "গ্রেড IV" to "Grade IV",
        "কেন্দ্র" to "Centre",
        "চিকিৎসা" to "Treatment",
        "ফিস্টুলা" to "Fistula",
        "রাজি নয়" to "Not willing",
        "ছবির নাম" to "Picture name",
        "সব মুছুন" to "Erase all",
        "রক্ত পড়ে" to "Bleeding",
        "ভেবে দেখব" to "Will think it over",
        "কবে থেকে?" to "Since when?",
        "একটা পিছনে" to "One step back",
        "পুরো পর্দা" to "Full screen",
        "＋\nছবি যোগ" to "＋\nAdd picture",
        "পরের ধাপ ▶" to "Next step ▶",
        "নাম ছাড়াই" to "Without a name",
        "কতদিন থেকে" to "Since when",
        "বাহ্যিক অর্শ" to "External piles",
        "কোমলতা/ব্যথা" to "Tenderness / pain",
        "কোলোনোস্কোপি" to "Colonoscopy",
        "আগের চিকিৎসা" to "Previous treatment",
        "ফিস্টুলার মুখ" to "Fistula opening",
        "আল্ট্রাসাউন্ড" to "Ultrasound",
        "ল্যাব রিপোর্ট" to "Lab report",
        "আর্থিক সমস্যা" to "Money problem",
        "চিকিৎসায় রাজি" to "Willing for treatment",
        "নিজের তোলা ছবি" to "Own photo",
        "অভ্যন্তরীণ অর্শ" to "Internal piles",
        "হঠাৎ শুরু হয়েছে" to "Started suddenly",
        "তালিকা থেকে সরান" to "Remove from the list",
        "ধীরে ধীরে বেড়েছে" to "Grew gradually",
        "পরিবারের সাথে আলোচনা" to "Will discuss with family",
        "কলম — আঙুল দিয়ে লিখুন" to "Pen — write with your finger",
        "WhatsApp-এ পাঠানো যাবে।" to "Can be sent on WhatsApp.",
        "ঘড়ির কাঁটা অনুযায়ী জায়গা" to "Clock position",
        "ছবির সব দাগ মুছে যাবে। মুছব?" to "All marks on the picture will be erased. Erase?",
        "ফোলান — মাংসের উপরে আঙুল টানুন" to "Swell — drag your finger over the flesh",
        "নালী — নালীর পথ ধরে আঙুল টানুন" to "Tract — drag your finger along the tract",
        "তীর — যেদিকে দেখাবেন সেদিকে টানুন" to "Arrow — drag towards the side you want to show",
        "গোল — যেটা ঘিরে দেখাবেন তার উপরে টানুন" to "Circle — drag over what you want to circle",
        "মুছুন — যে দাগটা তুলবেন তার উপরে ছুঁয়ে দিন" to "Erase — touch the mark you want to remove",
        "রিপোর্ট কোন ভাষায় বেরোবে — চাপ দিয়ে বদলান" to "Which language the report comes in — tap to change",
        "চিহ্ন — যেখানে চিহ্ন দেবেন সেখানে ছুঁয়ে দিন" to "Mark — touch where you want to put the mark",
        "কেন্দ্র — পায়ুপথের ঠিক মাঝখানে একবার ছুঁয়ে দিন" to "Centre — touch once at the exact middle of the anal canal",
        "এই রোগীর ডাক্তার চেক-আপ এখনো সম্পূর্ণ হয়নি।" to "This patient's doctor check-up is not complete yet.\n\n",
        "যে ফোলা বা নালীতে ক্ষারসূত্র দেখাবেন, সেটা ছুঁয়ে দিন" to "Touch the swelling or tract where you want to show Kshar Sutra",
        "চেক-আপ শেষ করে Save করলে এখান থেকে রিপোর্ট দেখা, A4 প্রিন্ট ও" to "After you finish the check-up and Save, from here you can view the report, print A4 and",
        "ছবিটা মুছে যাবে না — পুরোনো চেক-আপে এর উপরে আঁকা থাকলে সেটা আগের মতোই দেখা যাবে।" to "The picture will not be deleted — anything drawn on it in an older check-up will still show as before.",
        "\" ছবিটা তালিকা থেকে সরাব?" to "\" — remove this picture from the list?\n\n",
        "পেশা" to "Occupation",
        "পরামর্শ" to "Advice",
        "অন্যান্য" to "Other",
        "প্রধান সমস্যা" to "Main complaint",
        "রঙ" to "Colour",
        "সময়" to "Time",
        "অল্প" to "A little",
        "অনেক" to "A lot",
        "মৃদু" to "Mild",
        "কালচে" to "Blackish",
        "তীব্র" to "Severe",
        "পরিমাণ" to "Amount",
        "মাঝারি" to "Moderate",
        "তীব্রতা" to "Severity",
        "যখন তখন" to "Any time",
        "মলের আগে" to "Before stool",
        "মলের পরে" to "After stool",
        "দপদপ করা" to "Throbbing",
        "পাতলা জল" to "Thin watery",
        "তরলের ধরন" to "Type of discharge",
        "টকটকে লাল" to "Bright red",
        "শুধু পুঁজ" to "Pus only",
        "স্বাভাবিক" to "Normal",
        "দেখা যায়" to "Visible",
        "ব্যথার ধরন" to "Type of pain",
        "দেখা যায় না" to "Not visible",
        "দুর্গন্ধযুক্ত" to "Foul smelling",
        "মলের সাথে মিশে" to "Mixed with stool",
        "রক্তযুক্ত পুঁজ" to "Blood-stained pus",
        "😣 ব্যথার ইতিহাস" to "😣 Pain history",
        "তীক্ষ্ণ কাটাকাটা" to "Sharp cutting",
        // 🎨 V897 — দলের নাম বদলেছে ("ইতিহাস" → "History"); নতুন নামগুলোও
        //    যোগ, পুরোনোগুলো রইল (আগে সেভ হওয়া রেকর্ডের জন্য)।
        "🩸 রক্তপাতের History" to "🩸 Bleeding history",
        "😣 ব্যথার History" to "😣 Pain history",
        "🫃 ফোলা / মাংসপিণ্ডের History" to "🫃 Swelling / mass history",
        "💧 পুঁজ / জল পড়ার History" to "💧 Pus / discharge history",
        "🩸 রক্তপাতের ইতিহাস" to "🩸 Bleeding history",
        "সারাক্ষণ একটানা থাকে" to "Stays on continuously",
        "💧 পুঁজ / জল পড়ার ইতিহাস" to "💧 Pus / discharge history",
        "পায়ুপথের কাছে ছোট ছিদ্র" to "Small opening near the anus",
        "🫃 ফোলা / মাংসপিণ্ডের ইতিহাস" to "🫃 Swelling / mass history",
        "ঠেলে ঢুকিয়ে দিতে হয় (Manual)" to "Has to be pushed back in (Manual)",
        "নিজে থেকে ভেতরে চলে যায় (Spontaneous)" to "Goes back in on its own (Spontaneous)",
        "হঠাৎ তীব্র ব্যথাসহ শক্ত হয়ে ফুলে গেছে" to "Suddenly swollen and hard with severe pain",
        "সারাক্ষণ বাইরেই বের হয়ে থাকে (Irreducible)" to "Stays out all the time (Irreducible)",
        "মলত্যাগের সময় তীব্র হয় ও পরে কয়েক ঘন্টা থাকে" to "Severe during stool and stays for a few hours after",
        "কোষ্ঠকাঠিন্য" to "Constipation",
        "মলদ্বারে ব্যথা" to "Anal pain",
        "পায়ুপথে রক্তপাত" to "Anal bleeding",
        "পুঁজ / রক্ত / জল পড়া" to "Pus / blood / discharge",
        "চুলকানি / জ্বালাপোড়া" to "Itching / burning",
        "ফোলা / মাংসপিণ্ড বের হওয়া" to "Swelling / mass coming out",
        "কম" to "Low",
        "পর্যাপ্ত" to "Adequate",
        "ডায়াবেটিস" to "Diabetes",
        "উচ্চ রক্তচাপ" to "High blood pressure",
        "খাবারে ফাইবারের পরিমাণ" to "Fibre in the diet",
        "দীর্ঘমেয়াদী কোনো রোগ আছে কি না?" to "Any long-term illness?",
        "অতিরিক্ত কোঁথ (Straining) দিতে হয়?" to "Needs excessive straining (Straining)?",
        "টয়লেটে দীর্ঘক্ষণ বসে থাকার অভ্যাস আছে?" to "Habit of sitting long in the toilet?",
        "সেমি" to "cm",
        "ছোট মুখ" to "Small opening",
        "ছোট ফোলা" to "Small swelling",
        "একটা ফোলা" to "One swelling",
        "একটা ঢিবি" to "One lump",
        "রস গড়াচ্ছে" to "Oozing",
        "খুব বড় মাংস" to "Very large mass",
        "কয়েকটা ফোলা" to "A few swellings",
        "চিকিৎসার পরে" to "After treatment",
        "অনেকগুলো আঁচিল" to "Many tags",
        "অপারেশনের সময়" to "During the operation",
        "ফোলা · কাছ থেকে" to "Swelling · close up",
        "আঁচিলের মত মাংস" to "Tag-like mass",
        "ফিস্টুলা · নালী" to "Fistula · tract",
        "ফোলা · চওড়া ছবি" to "Swelling · wide view",
        "কাটা ছবি · নরম রং" to "Cross-section · soft colours",
        "লাল · বেরিয়ে আসা" to "Red · prolapsed",
        "কাটা ছবি · গাঢ় রং" to "Cross-section · dark colours",
        "অনেকটা বেরিয়ে আসা" to "Largely prolapsed",
        "হাতে আঁকা · খালি ছক" to "Hand drawn · blank chart",
        "বই · ফিস্টুলার নকশা" to "Book · fistula diagram",
        "ঘা হয়ে যাওয়া মাংস" to "Ulcerated mass",
        "বই · পাইলসের চার ধাপ" to "Book · four grades of piles",
        "বেরিয়ে আসা · বেগুনি" to "Prolapsed · purple",
        "3D মডেল · ঘড়ির কাঁটা" to "3D model · clock positions",
        "বই · ফিস্টুলার চার ধরন" to "Book · four types of fistula",
        "বই · ফোঁড়া কোথায় হয়" to "Book · where an abscess forms",
        "বই · পায়ুনালীর কাটা ছবি" to "Book · anal canal cross-section",
        "চারপাশ জুড়ে বেরিয়ে আসা" to "Prolapsed all around",
        "চারপাশ জুড়ে · চওড়া ছবি" to "All around · wide view",
        "ভিতরের পর্দা বেরিয়ে আসা" to "Inner lining prolapsed",
        "রোগ:" to "Disease:",
        "সময়:" to "Time:",
        "বলেছেন:" to "Said:",
        "আনুমানিক খরচ ₹" to "Estimated cost ₹",
        "1) ফিস্টুলার নালী" to "1) The fistula tract",
        "3) মাংস আরো ফুলে উঠল" to "3) The mass swelled further",
        "1) যেভাবে আঁকা হয়েছে" to "1) As it has been drawn",
        "5) কেটে পড়ল — জায়গা পরিষ্কার" to "5) It cut through and fell off — the area is clear",
        "4) নালী কেটে গেল — জায়গা পরিষ্কার" to "4) The tract cut through — the area is clear",
        "3) দুই মাথায় গিঁট — বেঁধে রাখা হলো" to "3) Knots at both ends — tied in place",
        "2) নালী বরাবর ক্ষারসূত্র পরানো হচ্ছে" to "2) Kshar Sutra being threaded along the tract",
        "2) ইনজেকশন দেওয়া হচ্ছে — ছুঁয়ে দেখান কোথায়" to "2) Injection being given — touch to show where",
        "4) গোড়ায় ক্ষারসূত্র বাঁধা হলো — ছুঁয়ে সরানো যায়" to "4) Kshar Sutra tied at the base — touch to move it",
        "উপর থেকে একটা ছবি বাছুন" to "Choose a picture from above",
        "গন্ধ" to "Smell",
        "এই নম্বরে আরও" to "This number already has",
        "জন রোগী আছেন:" to "more patient(s):\n",
        "এই মোবাইল নম্বরটা ইতিমধ্যে" to "This mobile number already belongs to",
        "হ্যাঁ চাপলে সম্পূর্ণ নতুন একজন রোগী তৈরি হবে — পুরোনো রোগীর" to "Tapping Yes creates a completely new patient — the old patient's",
        "তবেই নিন, বারবার নয়।\n\nএখন ব্যাকআপ নেব?" to "take it only then, not repeatedly.\n\nTake the backup now?",
        "ব্যাকআপ নিতে Cloud-এর 7টা Backup Table একবার নামবে" to "A backup downloads the 7 cloud backup tables once",
        "এতে মাসিক ডেটার হিসাব থেকে কিছুটা খরচ হয়। সত্যিই দরকার হলে" to "This uses some of the monthly data allowance. If you really need it,",
        "এতে মাসিক ডেটার হিসাব থেকে অনেকটা খরচ হয়। সত্যিই দরকার হলে" to "This uses a lot of the monthly data allowance. If you really need it,",
        "এটা চাপলে বাছাই করা টেবিলের **সব তথ্য** ইন্টারনেট থেকে নামবে" to "This downloads ALL data of the chosen tables from the internet",
        "টাকা:" to "Amount:",
        "⚠️ এই বার্তা আগে" to "⚠️ This message was already sent",
        "আজ আগেই হাজিরা হয়েছে —" to "Attendance is already marked today —",
        "পর্যন্ত বন্ধ আছেন। মাস্টারকে জানান।" to "is on hold until then. Please inform the Master.",
        "Master: অনুমোদন দিলে এই দিনের চেম্বার আবার এডিটযোগ্য হয়ে যাবে।" to "\nMaster: approving makes this day's chamber editable again.",
        "এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন।" to "Does any SIM on this phone have the clinic's chamber/branch number? If it's a personal number, choose \"No\".",
        "এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন — তাহলে Dialer-এ কোনো কল দেখানো হবে না।" to "Does any SIM on this phone have the clinic's chamber/branch number? If it's a personal number, choose \"No\" — Dialer will then show no calls.",
        "এই ফোনে চেম্বারের নম্বর নেই বলে জানানো আছে, তাই কল দেখানো হচ্ছে না।\n\n👉 এটা ভুল হলে — এখানে চাপুন আবার জিজ্ঞাসা করতে" to "This phone is marked as not having the chamber number, so no calls are shown.\n\n👉 If this is wrong — tap here to ask again",
        "After Photo\nপরের ছবি" to "After Photo",
        "Before Photo\nআগের ছবি" to "Before Photo",
        "During Photo\nচলাকালীন ছবি" to "During Photo",
        /* 🟢🔒 V590 (২৩.০৮.২০২৬, TK-নির্দেশ: *"কিশানগঞ্জের যেকোনো স্টাফের কাছে
           যেন বাংলা পর্দা কোথাও না থাকে · বাংলার পরিবর্তে হয় হিন্দি অথবা ইংলিশ"*)
           **যাচাই করে পাওয়া ফাঁক:** চেক-আপ পর্দার ১১টা Toast-এ কাঁচা বাংলা ছিল আর
           `NoBengali.s()` ছাড়াই দেখানো হত। Toast কোনো পর্দার ভিতরে থাকে না, তাই
           স্বয়ংক্রিয় সুইপ ওখানে পৌঁছায়ই না — কিশানগঞ্জের স্টাফ **বাংলাই** দেখতেন।
           ⇒ এখন প্রতিটা `NoBengali.s()`-এর ভিতরে, আর নিচে তাদের হিন্দি।
           ⛔ চেক-আপ পর্দার লেখা হিন্দিতে — TK-এর V575-এর নিজের নিয়ম মেনে।
           ⛔ বাকি সব স্টাফের পর্দায় এগুলো আগের মতোই বাংলা। */
        "ছবি যোগ হলো" to "Picture added",
        "কেন্দ্র বসানো হলো — এবার চিহ্ন দিলেই ঘড়ির সময় নিজে বসবে" to
            "Centre is set — now the clock time fills in by itself when you place a mark",
        "আগে ⊕ কেন্দ্র দিয়ে পায়ুপথের মাঝখানে একবার ছুঁয়ে দিন — তবেই ঘড়ির সময় নিজে বসবে" to
            "First touch the middle of the anal canal with ⊕ Centre — only then the clock time fills in by itself",
        "মোছার মত কিছু নেই" to "There is nothing to erase",
        "তালিকা থেকে সরানো হলো" to "Removed from the list",
        "আগে উপরের সারি থেকে একটা ছবি বাছুন" to "First choose a picture from the row above",
        "এখানে ক্ষারসূত্র দেখানো যায় না — ফোলা বা নালী ছুঁয়ে দিন" to
            "Kshar Sutra cannot be shown here — touch a swelling or a tract",
        "ইনজেকশনের ধাপ থাকবে" to "The injection step will be kept",
        "ইনজেকশনের ধাপ বাদ" to "The injection step is removed",
        "আগে ছবিতে ফোলা বা নালী আঁকুন — তারপর ক্ষারসূত্র দেখানো যাবে" to
            "First draw a swelling or a tract on the picture — then Kshar Sutra can be shown",
        "কাল আসার কথা" to "Expected Tomorrow",
        "\nমৌলিক বৃত্তান্ত" to "",
        "\nপ্রধান অভিযোগ" to "",
        "\nকতদিন থেকে" to "",
        "\nপেশা" to "",
        "\nপূর্ববর্তী চিকিৎসার বৃত্তান্ত" to "",
        "\nপূর্বে নেওয়া চিকিৎসা" to "",
        "\nপূর্বের ফলাফল" to "",
        "\nআগের খরচ" to "",
        "\nচিকিৎসার মেয়াদ" to "",
        "\nক্লিনিক্যাল ফলাফল" to "",
        "\nদৃশ্যমান পরীক্ষা" to "",
        "\nঅন্যান্য" to "",
        "\nডিআরই পরীক্ষা" to "",
        "\nপ্রক্টোস্কোপি গ্রেড" to "",
        "\nঅন্যান্য ফলাফল" to "",
        "\nপরীক্ষা-নিরীক্ষা" to "",
        "\nপরামর্শ ও উপদেশ" to "",
        "\nরোগের ব্যাখ্যা দেওয়া হয়েছে" to "",
        "\nআর্থিক আলোচনা" to "",
        "\nআনুমানিক খরচ" to "",
        "\nআনুমানিক সুস্থ হওয়ার সময়" to "",
        "\nঅগ্রিম আলোচনা" to "",
        "\nরোগীর সিদ্ধান্ত" to "",
        "\nঅন্যান্য / মন্তব্য" to "",
        "\nছবি ও নথি" to "",
        "\nআগের ছবি" to "",
        "\nচলাকালীন ছবি" to "",
        "\nপরের ছবি" to "",
        "\nনথি / রিপোর্ট নোট" to "",
        "\nদ্রুত কাজ" to "",
        " · নথি / রিপোর্ট নোট" to "",
        " · দ্রুত কাজ" to "",
        " · রোগীর বিবরণ" to "",
        "\nবর্তমান রোগের ইতিহাস ও পূর্ববর্তী চিকিৎসার বিবরণ" to "",
        "\nপ্রধান সমস্যা" to "",
        "\nহঠাৎ হয় নাকি ধীরে ধীরে বাড়ে" to "",
        "\nআগের চিকিৎসার বিবরণ" to "",
        "\nআগের চিকিৎসার ফলাফল" to "",
        "\nআগের চিকিৎসার খরচ" to "",
        "\nচিকিৎসা কত দিন চলেছিল" to "",
        " · প্রধান সমস্যা" to "",
        " · কতদিন থেকে" to "",
        " · হঠাৎ হয় নাকি ধীরে ধীরে বাড়ে" to "",
        " · পেশা" to "",
        " · আগের চিকিৎসার বিবরণ" to "",
        " · আগের চিকিৎসার ফলাফল" to "",
        " · প্রক্টোস্কোপি গ্রেড" to "",
        " · অন্যান্য ফলাফল" to "",
        " · আগের চিকিৎসার খরচ" to "",
        " · চিকিৎসা কত দিন চলেছিল" to "",
        " · অন্যান্য" to "",
        " · ডিআরই পরীক্ষা" to "",
        " · প্রোব পরীক্ষা" to "",
        " · পরীক্ষা-নিরীক্ষা" to "",
        "\nফিস্টুলার মুখ" to "",
        "\nরক্ত পড়ে" to "",
        "\nবাহ্যিক অর্শ" to "",
        "\nঅভ্যন্তরীণ অর্শ" to "",
        "\nফিসার" to "",
        "\nফোলাভাব" to "",
        "\nকোমলতা/ব্যথা" to "",
        "\nফিস্টুলা" to "",
        "/ প্রতি অর্শ" to "",
        "/ ফিস্টুলা প্রতি ইঞ্চি" to "",
        "/ প্রতি ক্ষারসূত্র" to "",
        "/ মেশিনের মাধ্যমে চিকিৎসা" to "",
        "\nপ্রোব পরীক্ষা" to "",
        "\nকীভাবে চিকিৎসা করা হবে" to "",
        " · কীভাবে চিকিৎসা করা হবে" to "",
        "\nআনুমানিক খরচ ও রোগীর সিদ্ধান্ত" to "",
        "\nআনুমানিক কতদিন বলা হল" to "",
        "\nঅগ্রিম কত টাকা জমা করতে চাইছে" to "",
        "\nরোগীর বা রোগী পক্ষের সিদ্ধান্ত" to "",
        " · আনুমানিক খরচ" to "",
        " · আনুমানিক কতদিন বলা হল" to "",
        " · অগ্রিম কত টাকা জমা করতে চাইছে" to "",
        " · রোগীর বা রোগী পক্ষের সিদ্ধান্ত" to "",
        " · অন্যান্য / মন্তব্য" to "",
        "\nছবি ও ভিডিও" to "",
        "\nঅন্যান্য চিকিৎসার কথা (টাইপ করুন)" to "",
        " · বৃত্তান্ত" to "",
        " · পূর্ববর্তী" to "",
        " · ক্লিনিক্যাল" to "",
        " · পরামর্শ" to "",
        " · আর্থিক" to "",
        " · সিদ্ধান্ত" to "",
        " · ছবি" to "",
        " · বাহ্যিক অর্শ" to "",
        " · ফিসার" to "",
        " · ফিস্টুলার মুখ" to "",
        " · রক্ত পড়ে" to "",
        " · ফোলাভাব" to "",
        " · কোমলতা/ব্যথা" to "",
        " · অভ্যন্তরীণ অর্শ" to "",
        " · ফিস্টুলা" to "",
        " · এমআরআই" to "",
        " · আল্ট্রাসাউন্ড" to "",
        " · কোলোনোস্কোপি" to "",
        " · ল্যাব রিপোর্ট" to "",
        " · গ্রেড I" to "",
        " · গ্রেড II" to "",
        " · গ্রেড III" to "",
        " · গ্রেড IV" to "",
        " · চিকিৎসায় রাজি" to "",
        " · রাজি নয়" to "",
        " · ভেবে দেখব" to "",
        " · পরিবারের সাথে আলোচনা" to "",
        " · আর্থিক সমস্যা" to "",
        "কল নিয়ে যা বললেন লিখুন…" to "Write what was discussed on the call...",
        "চোখে দেখা পরীক্ষা" to "Visual Examination",
        // 📝 V1280 (তালিকা সারি ৪০৩) — চেকআপ ধাপ ৩-এর চুক্তি/পরিকল্পনার ঘর
        "চিকিৎসার চুক্তি ও পরিকল্পনা" to "Treatment Plan & Agreement",
        "রোগীর সাথে কী চুক্তি হল — কীভাবে চিকিৎসা, কতদিন, কিস্তি — বিস্তারিত এখানে লিখুন" to "What was agreed with the patient — how, how long, instalments — write in detail",
        "চিকিৎসা পরিকল্পনা" to "Treatment Plan",
        "রোগের ছবি · রোগীকে দেখিয়ে বোঝানোর জন্য" to "Disease picture - to show and explain to the patient",
        "রোগী নিজে যা যা বললেন — এখানে টাইপ করুন" to "What the patient said in their own words - type here",
        "আঙুল দিয়ে দেখে আর যা পাওয়া গেল" to "Other findings from the finger examination",
        "রোগী এসে প্রথমে কি কি বললেন?" to "What did the patient say first?",
        "Patient Said · রোগী যা বললেন" to "Patient Said",
        "B. আঙুল দিয়ে দেখে · DRE" to "B. Finger examination - DRE",
        "দৈনিক জল পানের পরিমাণ" to "Daily water intake",
        "কতদিন সময় চাওয়া হল?" to "How much time was asked for?",
        "রোগীর বলা ইতিহাস" to "History as told by the patient",
        "সম্ভাব্য কি রোগ?" to "Probable disease?",
        "রোগ ও অভ্যাস" to "Conditions and habits",
        "এই ইতিহাস নিয়ে আর কিছু লেখার থাকলে এখানে লিখুন" to "If there is more to note about this history, write it here",
        "এছাড়া অন্য কিছু থাকলে এখানে লিখুন" to "If there is anything else, write it here",
        "অন্যান্য থাকলে এখানে লিখুন" to "Write anything else here",
        "Clinical পরীক্ষা" to "Clinical Examination",
        "Fistula Per ইঞ্চি" to "Fistula Per Inch",
        "ফিস্টুলা প্রতি সেমি" to "Fistula Per CM",
        "মেশিন দিয়ে কাজ হবে · Machine Treatment" to "Machine Treatment",
        "Machine Treatment/ মেশিনে চিকিৎসা" to "Machine Treatment",
        "LIS/ এলআইএস চিকিৎসা" to "LIS Treatment",
        "Injection/ ইনজেকশন চিকিৎসা" to "Injection (Vaccination) Treatment",
        "উপরে টিক মারার বাইরেও যদি রোগীকে অন্য কিছু বলে থাকেন তাহলে সেই কথা এখানে লিখুন, অন্যথায় " to "",
        "ভগন্দর (ফিস্টুলা) হলো মলদ্বারের ভেতরের অংশ ও তার পাশের চামড়ার মধ্যে তৈরি হওয়া একটি অস্বাভাবিক নালি। এটি সাধারণত মলদ্বারের কোনো ফোঁড়া (সংক্রমণের পকেট) সম্পূর্ণ না সারলে তৈরি হয়, যার ফলে একটি নালি থেকে যায় যার মধ্য দিয়ে পুঁজ বা তরল ক্রমাগত বের হতে থাকে। সময়ের সাথে সাথে এই নালি শাখা-প্রশাখায় জটিল হয়ে উঠতে পারে বলে, অর্শ বা ফিশারের চেয়ে ভগন্দরের চিকিৎসায় বেশি পরিকল্পিত পদ্ধতি লাগে — উপযুক্ত ক্ষেত্রে ঐতিহ্যবাহী আয়ুর্বেদিক ক্ষারসূত্র (ওষুধযুক্ত সুতো) পদ্ধতি ব্যবহার করা হয়" to "A fistula-in-ano is an abnormal tunnel that forms between the inside of the anal canal and the skin near the anus. It usually develops after an anal abscess (a pocket of infection) does not heal completely, leaving behind a channel through which pus or fluid continues to drain. Because the tunnel can branch and become complex over time, fistulas often need more structured treatment than piles or fissures, including the traditional Ayurvedic Ksharsutra (medicated thread) approach in appropriate cases",
        "ফিশার হলো মলদ্বারের পাতলা, নরম আবরণে হওয়া একটি ছোট ছিঁড়ে যাওয়া বা ফাটল। এটি সাধারণত শক্ত বা বড় মল ত্যাগের কারণে হয়, যা মলদ্বারের নালিকে তার স্বাভাবিক ক্ষমতার চেয়ে বেশি টেনে ধরে। এই ফাটলের কারণে ভেতরের সংবেদনশীল টিস্যু ও মাংসপেশি উন্মুক্ত হয়ে যায়, ফলে মলত্যাগের সময় ও পরে তীব্র কাটা কাটা ব্যথা হয় যা কয়েক মিনিট থেকে কয়েক ঘণ্টা পর্যন্ত থাকতে পারে। দীর্ঘদিনের ফিশারে চারপাশের পেশিতে খিঁচুনি হতে পারে, যার ফলে সঠিক যত্ন ছাড়া সারতে দেরি হয়" to "An anal fissure is a small tear or crack in the thin, moist lining of the anus. It most commonly results from passing hard or large stools, which stretches the anal canal beyond its capacity. The tear exposes sensitive tissue and underlying muscle, causing a sharp, cutting pain during and after bowel movements that can last minutes to hours. Chronic fissures may cause the surrounding muscle to spasm, making healing slower without proper care",
        "অর্শ (পাইলস) হলো মলদ্বার ও মলদ্বারের নিচের অংশের শিরাগুলো ফুলে যাওয়া একটি সমস্যা। বারবার চাপ পড়া বা কোষ্ঠকাঠিন্যের কারণে এই শিরাগুলো ধীরে ধীরে ফুলে ওঠে। অর্শ দুই রকম হতে পারে — অভ্যন্তরীণ (ভেতরের দিকে, সাধারণত ব্যথাহীন কিন্তু রক্ত পড়তে পারে) এবং বাহ্যিক (মলদ্বারের চারপাশে চামড়ার নিচে, প্রায়ই ব্যথা ও চুলকানি সহ)। অবহেলা করলে ধীরে ধীরে সমস্যা বাড়তে থাকে, তাই প্রথম দিকেই আয়ুর্বেদিক চিকিৎসা নেওয়া জরুরি" to "Piles (haemorrhoids) are swollen, inflamed veins in the lower rectum and anus. They form when the cushions of tissue that normally help control bowel movements become enlarged from repeated straining or pressure. Piles can be internal (inside the rectum, usually painless but may bleed) or external (under the skin around the anus, often painful and itchy). Left untreated, they tend to worsen gradually, so early Ayurvedic care matters",
        "হাইড্রোসিল হলো অণ্ডকোষের চারপাশের পাতলা থলিতে স্বচ্ছ তরল জমে ফোলা তৈরি হওয়ার একটি সমস্যা। প্রাপ্তবয়স্কদের ক্ষেত্রে এটি সাধারণত ব্যথাহীন হয়, তবে দিনের শেষে বা দীর্ঘক্ষণ দাঁড়িয়ে থাকার পরে ভারী লাগা বা টানটান অস্বস্তি অনুভূত হতে পারে। হাইড্রোসিল কোনো স্পষ্ট কারণ ছাড়াই হতে পারে, অথবা সামান্য আঘাত, সংক্রমণ বা প্রদাহের পরে দেখা দিতে পারে — সঠিক পরীক্ষা করলে অণ্ডকোষ ফোলার অন্য কারণগুলো বাদ দেওয়া যায়" to "A hydrocele is a build-up of clear fluid in the thin sac that surrounds a testicle, leading to swelling of the scrotum. It is usually painless in adults, though it can cause a feeling of heaviness or dragging discomfort, especially by the end of the day or after standing for long periods. Hydroceles can develop without any obvious cause, or follow minor injury, infection, or inflammation in the area — a proper examination helps rule out other causes of scrotal swelling",
    )

}
