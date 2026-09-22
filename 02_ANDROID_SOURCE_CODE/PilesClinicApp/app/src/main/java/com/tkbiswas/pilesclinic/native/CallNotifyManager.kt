package com.tkbiswas.pilesclinic.native

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tkbiswas.pilesclinic.R

/**
 * 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ ৪ বার) — "Truecaller-এর
 * মতো" ইনকামিং/আউটগোয়িং কলে রোগীর পরিচয় + রিমার্কস/New Enquiry।
 *
 * TK-এর নির্দেশ (হুবহু):
 *   ১. "যদি Incoming নম্বর আমাদের app-এ আগে থেকে save না থাকে" — তাহলে
 *      শুধু নম্বর দেখাবে, "New Enquiry" অপশন থাকবে।
 *   ২. "নম্বর যদি save থেকে থাকে — কল কাটার পর Remarks লেখার option।"
 *   ৩. "কল চলাকালীন ও কল কাটার পরে দুটোতেই Remarks/New Enquiry Fillup।"
 *   ৪. "শুধু যেসব ফোনে চেম্বারের SIM আছে বলে জানানো আছে, সেখানেই।"
 *
 * 🔒 নিরাপত্তার সিদ্ধান্ত (TK-কে আগেই জানানো হয়েছে) — সত্যিকারের "কল
 * চলাকালীন ভেসে-থাকা উইন্ডো" SYSTEM_ALERT_WINDOW অনুমতি চায়, যেটা কিছু
 * ফোনে (Xiaomi/Vivo/Oppo) অস্থির/অনির্ভরযোগ্য। তাই এখানে **নোটিফিকেশন**
 * ব্যবহার হয়েছে — একই কার্যকারিতা (দেখা যায়, বোতাম চাপা যায়, কল চলা
 * অবস্থাতেই), কিন্তু প্রতিটা ফোনে নিরাপদ ও প্রমাণিত পথ।
 *
 * ⛔ শুধু একটাই বাড়তি অনুমতি (READ_PHONE_STATE) — এটা এই প্রজেক্টে আগে
 *    থেকেই আছে (B441, দুই-SIম আলাদা করতে)। নতুন কোনো অনুমতি চাওয়া হয়নি।
 * ⛔ আসল কল করা/ধরা/কাটা — সবই ফোনের নিজের সিস্টেমেই থাকে, এই অ্যাপ
 *    সেখানে হাতই দেয় না।
 */
object CallNotifyManager {

    private const val CHANNEL_ID = "call_id_v605"
    const val NOTIF_ID = 90501

    @Volatile private var activeNumber: String = ""
    @Volatile private var activeDirection: String = ""
    @Volatile private var activeCalledAt: String = ""
    @Volatile private var activeMatch: DialerRepository.MatchedContact? = null
    // 🟢🔒🔒 V637 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "প্রথমবার Remarks-এ
    // 'কাল আসবে' লিখেছিলাম, পরেরবার কল এলে সেটা কেন দেখাচ্ছে না") — আগের
    // লেখা রিমার্কস এখানে মনে রাখা হয়, `post()`-এ নোটিফিকেশনের বোতামের
    // সাথে পাঠানো হয়।
    @Volatile private var activeExistingRemark: String = ""

    /* 🆕🔒 V856 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — ভাসমান কার্ডে
       **LAST CALL-এর তারিখ · সময় · কে করেছিল**, আর কলটা
       **INCOMING / OUTGOING / MISSED** কোনটা।
       ⛔ Egress এক বাইটও বাড়েনি — যে একটাই সারি আগে থেকেই পড়া হত
          (`call_remarks`), সেটারই আরও তিনটে ছোট ঘর চাওয়া হলো। */
    @Volatile private var activeLastCall: DialerRepository.LastCallInfo =
        DialerRepository.LastCallInfo()

    /** এই কলটা ধরা হয়েছিল কি না — না ধরলে (incoming) সেটাই **Missed**। */
    @Volatile private var answeredThisCall: Boolean = false
    @Volatile private var activeMissed: Boolean = false

    /* 🔴🔒🔒 V896 (৩১.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — *"ফোন আসে বা যায় এক
       নাম্বার দিয়ে, আর এটা ওঠে আরেক নাম্বার"*):

       **আসল দোষ (কোড ধরে যাচাই, আন্দাজ নয়):** কল কেটে গেলেও (`onIdle`)
       আগের নম্বরটা মনে থেকে যেত — ইচ্ছে করেই, যাতে স্টাফ পরে Remark
       লিখতে পারেন। কিন্তু তারপর **ফোনের নিজের ডায়ালার** থেকে কল করলে/ধরলে
       Android অ্যাপকে নতুন নম্বর দেয় না, শুধু "কথা হচ্ছে" (OFFHOOK) জানায়।
       `onOffhook()` তখন **ওই পুরোনো নম্বর নিয়েই** কার্ডটা আবার "চলছে"
       অবস্থায় দেখিয়ে দিত — TK-এর ছবিতে ঠিক সেটাই (স্ত্রীর কল চলাকালীন
       আগের কলের নম্বর)।

       **এখন:** কল শেষ হলে এই চিহ্নটা বসে। পরের OFFHOOK-এ যদি চিহ্নটা বসানো
       থাকে, বোঝা যায় এটা **নতুন একটা কল যার নম্বর অ্যাপ জানে না** — তখন
       ভাসমান কার্ডটা সরিয়ে দেওয়া হয়, ভুল নম্বর আর দেখানো হয় না।
       ⛔ আগের কলের নোটিফিকেশনটা ট্রে-তে অক্ষত থাকে (নম্বরটা ওখানে ঠিকই
          আছে), তাই পরে Remark লেখার পথ এক অক্ষরও বন্ধ হয়নি। */
    @Volatile private var activeEnded: Boolean = false
    /* ☎️🔒 V1427 (১৩.০৯.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — এই নম্বরের শেষ
       Missed · Outgoing · Incoming (ফোনের নিজের Call Log থেকে, ব্যাকগ্রাউন্ডে
       একবার পড়া) — ব্যানার ও নোটিফিকেশন দুটোতেই দেখানো হয়। */
    @Volatile private var activeHistory: BranchSimHelper.CallHistory = BranchSimHelper.CallHistory()
    /* ☎️🔒 V1434 — দুটো খোঁজ (ফোনের কল-লগ · ক্লাউডের call_remarks) শেষ হয়েছে কি না।
       দুটোই শেষ, তবু কিছু নেই ⇒ তবেই "FIRST CALL" বক্স (আগে-ভাগে ভুল "FIRST CALL" নয়)। */
    @Volatile private var activeLogDone: Boolean = false
    @Volatile private var activeCloudDone: Boolean = false

    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())

    /** 🔒 TK-নির্দেশ ৪ (২৪.০৮.২০২৬, পরে সংশোধিত V619) — শুধু যে ফোন আগে
     *  সত্যিই "হ্যাঁ, চেম্বারের SIM আছে" বলেছে, সেখানেই এই পুরো ফিচার চলে।
     *  ⛔ প্রথমবার `hasGenuinelyChosenSim()` ব্যবহার করেছিলাম (ভুল —
     *  সেটা Dialer-এর কল-লগ দেখানোর জন্য ইচ্ছাকৃতভাবে নরম, পুরনো এক-সিম
     *  ফোনকে প্রশ্ন ছাড়াই grandfather করে)। TK নিজের/Master-এর ফোনে এই
     *  ফাঁক ধরেছেন — এখন `hasExplicitlyConfirmedChamberSim()` (নতুন,
     *  কড়া, কোনো ছাড় নেই) ব্যবহার হচ্ছে।
     *
     *  🟢🔒🔒 V633 (২৪.০৮.২০২৬, TK-রিপোর্ট + স্পষ্ট নির্দেশ — "ব্রাঞ্চের
     *  নম্বর ছাড়া যেন এই কাজ কারো ফোনে না হয়") — **আসল কারণ ধরা পড়ল:**
     *  `tryAutoDetectChamberNumber()` ফোনের নিজের সিম-নম্বর পড়ে ব্রাঞ্চের
     *  নম্বরের সাথে মিলিয়ে **চুপচাপ, কাউকে না জিজ্ঞাসা করেই** "হ্যাঁ"
     *  ধরে নিত — আর Android-এর নিজের `line1Number` (বিশেষত ভারতীয় সিমে)
     *  প্রায়ই ভুল/পুরনো নম্বর ফেরত দেয় (এটা Android/অপারেটরেরই স্বীকৃত
     *  সীমাবদ্ধতা), তাই ভুল ফোনেও চালু হয়ে যেতে পারত।
     *  **সমাধান (TK-এর স্পষ্ট নির্দেশ অনুযায়ী):** পুরনো SIM-শনাক্তকরণ
     *  নিয়মের **উপরে** এখন একটা কড়া দ্বিতীয় পাহারা — যে মোবাইল দিয়ে
     *  App-এ লগইন করা আছে, সেটা **এই চারটে নির্দিষ্ট নম্বরের একটা** না
     *  হলে কল-শনাক্তকরণ ফিচারটাই সম্পূর্ণ বন্ধ থাকবে, ফোনে যাই থাকুক না
     *  কেন। দুটো শর্তই (পুরনো SIM-নিশ্চিতকরণ + নতুন লগইন-হোয়াইটলিস্ট)
     *  সত্যি হতে হবে — কোনোটা একা যথেষ্ট না।
     *  ⛔ Dialer-এর কল-লগ **দেখানোর** নিয়ম (`hasGenuinelyChosenSim`,
     *     `hasChamberNumber` — WorkNotebook/DialerActivity-তে ব্যবহৃত)
     *     এক অক্ষরও বদলায়নি — এই হোয়াইটলিস্ট শুধু **কল-শনাক্তকরণ
     *     নোটিফিকেশন**-এর (এই ফাইল) জন্য প্রযোজ্য।
     */
    /* 🔴🔒🔒 V869 (৩০.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — JPE-CRP-এর ফোন,
       Jalpaiguri, "Display over other apps = Allowed", তবু ব্যানার আসে না):
       *"ব্রাঞ্চের নাম্বার যে সমস্ত ফোনে লাগানো আছে সে সমস্ত ফোনেও যেন একই
       কাজ হতে হয় … এটা তো আগেই আপনাকে বলা হয়েছিল"*।

       **আমার দোষ (স্বীকার করছি):** V633-এ আমি লগইন-নম্বরের একটা **চারজনের
       তালিকা** বসিয়েছিলাম (৩টে ব্রাঞ্চ + মাস্টার)। ফলে —
         · স্টাফের নিজের আইডিতে (যেমন JPE-CRP) ব্যানার **কখনোই** আসত না,
         · Falakata ও Birpara-র নম্বর তালিকাতেই ছিল না।
       TK-এর নির্দেশ ছিল **ফোনে ব্রাঞ্চের নম্বর থাকলেই** চলবে — লগইন কার,
       সেটা শর্ত ছিল না। তালিকাটা আমার নিজের বাড়তি শর্ত, TK-এর নয়।

       **এখন:** শর্ত একটাই — ফোনে সত্যিই চেম্বার/ব্রাঞ্চের SIM আছে কিনা
       (`hasExplicitlyConfirmedChamberSim`)। এটাই TK-এর আসল নিয়ম, আর এটা
       V633-এর কড়া যাচাই — কোনো ছাড় নেই, তাই ব্রাঞ্চের নম্বর ছাড়া কারো
       ফোনে এখনো চালু হবে না।
       ⛔ Dialer-এর কল-লগ দেখানোর নিয়ম এক অক্ষরও বদলায়নি। */
    private fun allowed(ctx: Context): Boolean =
        BranchSimHelper.hasExplicitlyConfirmedChamberSim(ctx)

    /** ফোন বাজছে — নম্বর মিলিয়ে সঙ্গে সঙ্গে দেখানো। */
    fun onRinging(ctx: Context, rawNumber: String) {
        if (!allowed(ctx)) return
        val digits = rawNumber.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return
        activeNumber = digits
        activeDirection = "incoming"
        activeCalledAt = isoNow()
        activeMatch = null
        activeExistingRemark = ""   // 🟢🔒 V637
        activeLastCall = DialerRepository.LastCallInfo()   // 🆕 V856
        answeredThisCall = false
        activeMissed = false
        activeEnded = false   // 🔴 V896 — নতুন কল, নম্বর জানা
        activeHistory = BranchSimHelper.CallHistory()   // ☎️ V1427
        activeLogDone = false; activeCloudDone = false   // ☎️ V1434
        post(ctx, ringing = true, ended = false)
        // পিছনে গিয়ে মেলানো — পাওয়া গেলে নোটিফিকেশন আপডেট হবে।
        Thread {
            try {
                // ☎️ V1427 — আগে ফোনের নিজের কল-লগ (ক্লাউড নয়, তাই দ্রুত), তারপর ক্লাউড-মেলানো।
                val hist = try { BranchSimHelper.lastCallsByType(ctx, digits) } catch (_: Throwable) { BranchSimHelper.CallHistory() }
                if (activeNumber == digits) {
                    activeHistory = hist
                    // ☎️ V1434 — কল-লগ পড়ার অনুমতি না থাকলে "খোঁজ শেষ" ধরা হয় না, নইলে ভুল "FIRST CALL" দেখাত।
                    activeLogDone = BranchSimHelper.hasCallLogPermission(ctx)
                    if (hist.missedMs > 0L || hist.outgoingMs > 0L || hist.incomingMs > 0L) post(ctx, ringing = true, ended = false)
                }
                val m = DialerRepository.matchNumbersBatch(listOf(digits))[digits]
                // 🟢🔒🔒 V637 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ) — আসল কারণ: এই
                // নোটিফিকেশনের "📝 Add Remark" বোতাম আগের কোনো লেখা রিমার্কস
                // কখনো নিয়ে আসতই না — `DialerRepository.fetchLatestRemark()`
                // ফাংশনটা আগে থেকেই ছিল (Dialer-এর নিজের তালিকায় ব্যবহৃত),
                // কিন্তু এই কল-শনাক্তকরণ ফ্লো থেকে কখনো ডাকাই হত না। এখন
                // একই ব্যাকগ্রাউন্ড থ্রেডেই আগের রিমার্কও আনা হয়।
                /* 🆕 V856 — আগের মতোই **একটাই** অনুরোধ, শুধু একই সারির
                   আরও তিনটে ঘর সঙ্গে আসে (তারিখ · সময় · কে · কোন দিকের কল)। */
                val info = try { DialerRepository.fetchLatestCallInfo(digits) } catch (_: Throwable) { DialerRepository.LastCallInfo() }
                if (activeNumber == digits) {
                    activeMatch = m; activeExistingRemark = info.remark
                    activeLastCall = info
                    activeCloudDone = true   // ☎️ V1434
                    post(ctx, ringing = true, ended = false)
                }
            } catch (_: Throwable) {
                // ☎️ V1434 — খোঁজ ভেঙে গেলেও "শেষ" ধরা হয়, নইলে বক্স কখনো আসত না।
                if (activeNumber == digits) { activeLogDone = BranchSimHelper.hasCallLogPermission(ctx); activeCloudDone = true; post(ctx, ringing = true, ended = false) }
            }
        }.start()
    }

    /** কল ধরা হলো (incoming) বা চলছে (outgoing) — একই নোটিফিকেশন, এখন
     *  বোতাম যোগ হয় (কল চলাকালীনও Remark/Enquiry লেখা যাবে, TK-নির্দেশ ৩)। */
    fun onOffhook(ctx: Context) {
        if (!allowed(ctx)) return
        if (activeNumber.isBlank()) return   // outgoing হলে notifyOutgoingDialed() থেকেই শুরু হয়
        /* 🔴🔒🔒 V896 — আগের কল ইতিমধ্যে শেষ, অথচ "কথা হচ্ছে" এলো ⇒ এটা
           **নতুন একটা কল, যার নম্বর অ্যাপ জানে না** (ফোনের নিজের ডায়ালার)।
           পুরোনো নম্বরের কার্ড আর দেখানো হবে না — সরিয়ে দেওয়া হয়।
           ⛔ নোটিফিকেশন ছোঁয়া হয় না, তাই আগের কলের Remark লেখা যাবে। */
        if (activeEnded) {
            try { CallOverlay.hide(ctx) } catch (_: Throwable) { }
            return
        }
        answeredThisCall = true   // 🆕 V856 — কল ধরা হলো ⇒ এটা আর Missed নয়
        post(ctx, ringing = false, ended = false)
    }

    /** কল কেটে গেল — একই তথ্য থাকবে, "Call ended" দেখাবে, বোতাম থাকবে। */
    fun onIdle(ctx: Context) {
        if (!allowed(ctx)) return
        if (activeNumber.isBlank()) return
        /* 🔴🔒 V896 — আগের কল আগেই শেষ হয়েছিল ⇒ এখন যেটা শেষ হলো সেটা
           **অন্য একটা কল, যার নম্বর অ্যাপ জানে না**। পুরোনো নম্বরের কার্ড বা
           নোটিফিকেশন আর নতুন করে দেখানো হবে না। */
        if (activeEnded) return
        /* 🆕🔒 V856 — **Missed** কল: এসেছিল (incoming), কিন্তু কেউ ধরেনি
           (OFFHOOK কখনো আসেনি)। ⛔ নিজের করা কল (outgoing) কখনো Missed নয়। */
        activeMissed = (activeDirection == "incoming" && !answeredThisCall)
        post(ctx, ringing = false, ended = true)
        activeEnded = true   // 🔴 V896 — এই নম্বরের কল শেষ
        // পরের কলের জন্য প্রস্তুত — কিন্তু নোটিফিকেশনটা (ended অবস্থায়)
        // থেকে যায়, স্টাফ চাপলে Remark/Enquiry খুলবে। নতুন RINGING/
        // notifyOutgoingDialed() এলে এই ভেরিয়েবলগুলো নতুন করে বসবে।
    }

    /** 🟢 TK-নির্দেশ ৩ (Outgoing-ও) — অ্যাপের নিজের Call বোতাম থেকে ডায়াল
     *  করার মুহূর্তেই ডাকা হয় (DialerActivity-তে জোড়া, নম্বর তখনই জানা)। */
    fun notifyOutgoingDialed(ctx: Context, rawNumber: String, matched: DialerRepository.MatchedContact?) {
        if (!allowed(ctx)) return
        val digits = rawNumber.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return
        activeNumber = digits
        activeDirection = "outgoing"
        activeCalledAt = isoNow()
        activeMatch = matched
        activeExistingRemark = ""   // 🟢🔒 V637 — নিচে ব্যাকগ্রাউন্ডে আনা হচ্ছে
        activeLastCall = DialerRepository.LastCallInfo()   // 🆕 V856
        answeredThisCall = true      // নিজে ডায়াল করা কল কখনো "Missed" নয়
        activeMissed = false
        activeEnded = false   // 🔴 V896 — নতুন কল, নম্বর জানা
        activeHistory = BranchSimHelper.CallHistory()   // ☎️ V1427
        activeLogDone = false; activeCloudDone = false   // ☎️ V1434
        post(ctx, ringing = false, ended = false)
        // 🟢🔒 V637 — outgoing কলেও আগের রিমার্কস আনা হয় (incoming-এর
        // `onRinging()`-এর হুবহু একই প্যাটার্ন)।
        Thread {
            try {
                val hist = try { BranchSimHelper.lastCallsByType(ctx, digits) } catch (_: Throwable) { BranchSimHelper.CallHistory() }   // ☎️ V1427
                val info = try { DialerRepository.fetchLatestCallInfo(digits) } catch (_: Throwable) { DialerRepository.LastCallInfo() }   // 🆕 V856
                if (activeNumber == digits) {
                    activeHistory = hist
                    activeExistingRemark = info.remark; activeLastCall = info
                    activeLogDone = BranchSimHelper.hasCallLogPermission(ctx); activeCloudDone = true   // ☎️ V1434
                    post(ctx, ringing = false, ended = false)
                }
            } catch (_: Throwable) {
                if (activeNumber == digits) { activeLogDone = BranchSimHelper.hasCallLogPermission(ctx); activeCloudDone = true; post(ctx, ringing = false, ended = false) }
            }
        }.start()
    }

    /** ☎️ V1434 — কল-ব্যানারে "কে" লাইনটা (TK-র কথা, গ্রামার ঠিক করে)। */
    private fun stageLine(m: DialerRepository.MatchedContact): String {
        fun cln(v: String?): String {
            val t = v?.trim().orEmpty()
            return if (t.equals("null", true) || t.equals("undefined", true)) "" else t
        }
        val br = cln(m.branch).let { if (it.isNotBlank()) "$it Branch" else "" }
        val dis = cln(m.disease)
        if (m.isRmp) return "🩺 RMP" + (if (br.isNotBlank()) " of $br" else "")
        return when (cln(m.stage)) {
            "Inquiry" -> "❓ " + (if (dis.isNotBlank()) "$dis " else "") + "Enquiry" + (if (br.isNotBlank()) " for $br" else "")
            "Patient" -> "🏥 Visited" + (if (br.isNotBlank()) " $br" else "") + (if (dis.isNotBlank()) " for $dis" else "")
            "Treatment" -> "💊 " + (if (dis.isNotBlank()) "$dis " else "") + "Patient" + (if (br.isNotBlank()) " of $br" else "")
            else -> "👤 " + listOf(dis, br).filter { it.isNotBlank() }.joinToString(" \u00b7 ").ifBlank { "Saved in the app" }
        }
    }

    /** ☎️ V1434 — স্টাফ-কোড "KNE-LAXMI" → "LAXMI" (TK: *"সহজ সরল করুন"*)। */
    private fun shortStaff(raw: String): String {
        val t = raw.trim()
        val m = Regex("^[A-Za-z]{2,4}-(.+)$").find(t)
        return (m?.groupValues?.get(1) ?: t).trim()
    }

    /** ☎️🔒 V1434 — "LAST CALL" বক্সের তথ্য। নিয়ম (ডেমো পাশ):
     *  · রেকর্ড (call_remarks) ও ফোনের কল-লগ — যেটা **নতুন**, সেটাই "শেষ কল" (ধরন + কখন);
     *    রেকর্ডটা নতুন হলে সাথে কে কথা বলেছিল। শেষ রিমার্ক থাকলে নিচে।
     *  · কিছুই না থাকলে, দুটো খোঁজ শেষ হওয়ার পরেই "FIRST CALL"; তার আগে null (বক্স নেই)। */
    private fun buildLastBox(): CallOverlay.LastBox? {
        val info = activeLastCall
        val log = activeHistory.latest()
        val recMs = BranchSimHelper.isoToMillis(info.calledAt)
        val logMs = log?.second ?: 0L
        val remark = info.remark.trim().let { if (it.equals("null", true)) "" else it }
        return when {
            recMs > 0L && recMs >= logMs -> CallOverlay.LastBox(
                // রিমার্ক Missed কলেও "incoming" নামে জমা হয় (CallRemarkActivity) — ফোনের লগে
                // একই সময়ের (২ মিনিটের মধ্যে) Missed থাকলে সেটাই সত্যি, তাই MISSED দেখানো হয়।
                type = when {
                    info.direction.trim().equals("outgoing", true) -> "OUTGOING"
                    log != null && log.first == "Missed" && kotlin.math.abs(recMs - logMs) < 120_000L -> "MISSED"
                    else -> "INCOMING"
                },
                whenTxt = BranchSimHelper.whenText(recMs),
                staff = shortStaff(info.staffName),
                remark = remark
            )
            log != null -> CallOverlay.LastBox(
                type = log.first.uppercase(java.util.Locale.US),
                whenTxt = BranchSimHelper.whenText(logMs),
                remark = remark
            )
            activeLogDone && activeCloudDone -> CallOverlay.LastBox(first = true)
            else -> null
        }
    }

    private fun post(ctx: Context, ringing: Boolean, ended: Boolean) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NoticeChannels.ensure(ctx, CHANNEL_ID, "Call Identification", "Shows patient info during calls")

            val number = activeNumber
            val direction = activeDirection
            val calledAt = activeCalledAt
            val match = activeMatch
            val existingRemark = activeExistingRemark   // 🟢🔒 V637

            /* 🔴🔒 V812 (২৮.০৮.২০২৬) — TK-রিপোর্ট (ফটো সহ): কল-নোটিফিকেশনে
               **"null · Kishanganj"** লেখা উঠছিল। TK: *"এটা কি প্রফেশনাল লুক?"*
               কারণ: ঘরটা সত্যিই ফাঁকা নয় — তাতে **"null" শব্দটাই লেখা** থাকে
               (org.json-এর পুরনো ফাঁদ: SQL NULL পড়লে `optString` আক্ষরিক "null"
               ফেরায়)। তাই `ifBlank {}` ছাঁকনি ওটাকে ফাঁকা ধরতে পারত না।
               ⛔ এই সাফাইটা **নোটিফিকেশনের প্রতিটা ঘরেই** বসানো হলো (নিয়ম ৬.২),
                  যাতে অন্য কোনো ঘরেও কোনোদিন "null" ছাপা না হয়। */
            fun cln(v: String?): String {
                val t = v?.trim().orEmpty()
                return if (t.equals("null", ignoreCase = true) || t.equals("undefined", ignoreCase = true)) "" else t
            }
            /* ☎️🔒 V1434 (১৩.০৯.২০২৬, TK-নির্দেশ, ডেমো V9 পাশ) — নাম ও নম্বর পাশাপাশি;
               ধরন-ভেদে আইকন ও কথা: 🩺 RMP of X Branch · ❓ Piles Enquiry for X Branch ·
               🏥 Visited X Branch for Piles · 💊 Piles Patient of X Branch; তারপর ঠিকানা।
               সেভ না থাকলে "🆕 New number · not saved in the app"। নিচে একটাই
               "LAST CALL" তথ্য (buildLastBox) — V1427-এর তিন লাইন ও আলাদা রিমার্ক-লাইন বাদ। */
            val callerName = cln(match?.name)
            val title = "📞 " + (if (callerName.isNotBlank()) "$callerName · $number" else number)
            val infoLines = ArrayList<String>()
            if (match != null) {
                infoLines.add(stageLine(match))
                if (cln(match.address).isNotBlank()) infoLines.add(cln(match.address))
            } else {
                infoLines.add("🆕 New number · not saved in the app")
            }
            val box = buildLastBox()
            val boxLines = ArrayList<String>()
            if (box != null) {
                if (box.first) boxLines.add("✨ FIRST CALL · no earlier call or record")
                else {
                    boxLines.add((if (box.type == "OUTGOING") "↗ " else "↙ ") + box.type + " CALL " + box.whenTxt +
                        (if (box.staff.isNotBlank()) " (" + box.staff + ")" else ""))
                    if (box.remark.isNotBlank()) boxLines.add("📝 " + box.remark)
                }
            }
            val lines = ArrayList<String>(infoLines + boxLines)
            if (ended) lines.add(0, "Call ended")

            val style = NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n"))

            val builder = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(lines.firstOrNull().orEmpty())
                .setStyle(style)
                // 🟢🔒 V619 (২৪.০৮.২০২৬, TK-রিপোর্ট — "ভুল নম্বর হলে Cancel
                // থাকতে হত") — আগে কল চলাকালীন `setOngoing(true)` দিয়ে
                // সোয়াইপ করেও সরানো যেত না, আর কোনো Dismiss বোতামও ছিল
                // না। এখন সবসময় স্বাভাবিকভাবে সরানো যায় (সোয়াইপ), আর
                // নিচে একটা স্পষ্ট "✕ Dismiss" বোতামও থাকছে।
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)

            /* 👆🔒 V844 (৩০.০৮.২০২৬, TK-রিপোর্ট ছবিসহ: *"যে নাম্বার থেকে কল
               এসেছিল সেই কার্ডে চাপ দিলে সেই নাম্বারের পেজে যেতে হবে…
               বারবার চাপা সত্ত্বেও কেন যাচ্ছে না"*)

               ❗ আসল কারণ — **আমারই বাদ পড়া**: এই নোটিফিকেশনে কখনো
               `setContentIntent(...)` বসানোই হয়নি। তাই নিচের বোতাম দুটো
               কাজ করত, কিন্তু **কার্ডের গায়ে চাপ দিলে কিচ্ছু হত না**।

               ✅ এখন চাপ দিলে ওই নম্বরের নিজের পাতা খোলে:
                 · RMP হলে → RMP-র তালিকা, ওই নম্বর খোঁজা অবস্থায়
                   (`searchMobile` — পর্দাটা আগে থেকেই এই extra পড়ে)
                 · রোগী হলে → তার Full Journey (`PatientTimelineActivity`,
                   `mobile` extra — এটাও আগে থেকেই আছে)
                 · কোথাও সেভ না থাকলে → নতুন Enquiry ফর্ম, নম্বর ভরা
                   (নিচের "➕ New Enquiry" বোতামের হুবহু একই কাজ)
               ⛔ নতুন কোনো পর্দা বা extra বানানো হয়নি — সবই প্রমাণিত।
               ⛔ নিচের তিনটে বোতাম · লেখা · রং — কিছুই বদলায়নি। */
            val tapIntent = when {
                match == null -> Intent(ctx, EnquiryActivity::class.java)
                    .putExtra("prefillMobile", number)
                match.isRmp -> Intent(ctx, DoctorVisitActivity::class.java)
                    .putExtra("searchMobile", number)
                    .putExtra("searchBranch", match.branch)
                else -> Intent(ctx, PatientTimelineActivity::class.java)
                    .putExtra("mobile", number)
                    .putExtra("preName", match.name)
                    .putExtra("preBranch", match.branch)
                    .putExtra("prePatientId", match.patientId)
            }.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            builder.setContentIntent(pendingActivity(ctx, 4, tapIntent))

            // ✕ Dismiss — ভুল নম্বর/দরকার নেই হলে সরাসরি সরানোর জন্য।
            val dismissIntent = Intent(ctx, CallDismissReceiver::class.java)
            val dismissPi = android.app.PendingIntent.getBroadcast(
                ctx, 3, dismissIntent,
                (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                else android.app.PendingIntent.FLAG_UPDATE_CURRENT)
            )
            builder.addAction(0, "✕ Dismiss", dismissPi)

            // 📝 Add/Edit Remark — কল চলাকালীন ও কল-শেষে দুটোতেই (TK-নির্দেশ ৩)।
            val remarkIntent = Intent(ctx, CallRemarkActivity::class.java)
                .putExtra("mobile", number).putExtra("direction", direction)
                .putExtra("patientId", match?.patientId.orEmpty())
                .putExtra("patientName", match?.name.orEmpty())
                .putExtra("branch", match?.branch.orEmpty())
                .putExtra("calledAt", calledAt)
                // 🟢🔒 V634 (২৪.০৮.২০২৬) — followups সারির আসল id, যাতে
                // রিমার্কস সেভের সময় Wifi-সিগন্যাল আইকনের callCount ঠিকভাবে
                // বাড়ে। RMP মিললে (match.isRmp) এটা ইচ্ছাকৃতভাবে ফাঁকা —
                // RMP-দের নিজস্ব followups সারি থাকে না।
                .putExtra("followupId", if (match != null && !match.isRmp) match.id else "")
                // 🩺🔒 V836 (২৯.০৮.২০২৬, TK-নির্দেশ) — RMP মিললে তার নিজের
                // `doctor_visits` সারির id, যাতে লেখা রিমার্ক **RMP সেকশনেই**
                // বসে। ⛔ RMP না হলে ফাঁকা, তাই পুরনো আচরণ হুবহু অক্ষত।
                .putExtra("rmpId", if (match != null && match.isRmp) match.id else "")
                // 🟢🔒🔒 V637 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "প্রথমবার
                // 'কাল আসবে' লিখেছিলাম, পরেরবার কল এলে সেটা দেখাচ্ছে না
                // কেন") — আসল কারণ: এই বোতাম আগে কখনো আগের রিমার্কস আনতই
                // না — `DialerRepository.fetchLatestRemark()` আগে থেকেই
                // ছিল (Dialer-এর নিজের তালিকায় ব্যবহৃত), কিন্তু এই
                // কল-শনাক্তকরণ ফ্লো থেকে কখনো ডাকা হত না। এখন `onRinging()`/
                // `notifyOutgoingDialed()`-এ একই ব্যাকগ্রাউন্ড থ্রেডে আনা
                // আগের রিমার্কস এখানে পাঠানো হয় — `CallRemarkActivity`
                // আগে থেকেই এই extra পড়ে বাক্সে বসায় (existingRemark),
                // শুধু এতদিন কেউ পাঠাতই না।
                .putExtra("existingRemark", existingRemark)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val remarkPi = pendingActivity(ctx, 1, remarkIntent)
            builder.addAction(0, "📝 Add Remark", remarkPi)

            if (match == null) {
                // 🔴 TK-নির্দেশ ১ — সেভ না থাকলে সরাসরি Enquiry ফর্মে (নম্বর ভরা)।
                val enquiryIntent = Intent(ctx, EnquiryActivity::class.java)
                    .putExtra("prefillMobile", number)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val enquiryPi = pendingActivity(ctx, 2, enquiryIntent)
                builder.addAction(0, "➕ New Enquiry", enquiryPi)
            }

            nm.notify(NOTIF_ID, builder.build())

            /* 🪟🔒 V845 (৩০.০৮.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) —
               নোটিফিকেশনের **ঠিক পাশাপাশি** কল-স্ক্রিনের উপরে ভাসমান কার্ড।
               TK: *"ফোন কল যখন আসবে তখনই যেন স্ক্রিনের ডিসপ্লেতে দেখা যায়।"*

               ⛔ **নোটিফিকেশনের একটা অক্ষরও বদলায়নি** — ওটা আগের মতোই যায়।
                  কার্ডটা শুধু বাড়তি; অনুমতি না থাকলে চুপচাপ কিছুই হয় না।
               ⛔ **কল বাজা ও কল চলা** — এই দুই অবস্থাতেই দেখায়; কল কেটে গেলে
                  (`ended`) সরে যায়, কারণ তখন পর্দা এমনিতেই খালি, আর
                  নোটিফিকেশন তো থেকেই যাচ্ছে।
               ⛔ **কোনো বাড়তি ক্লাউড-পড়া নেই** — উপরে তৈরি হওয়া `title` ·
                  `lines` · `existingRemark`-ই ব্যবহার হয়। Egress শূন্য।
               ⛔ কার্ড কল ধরার/কাটার বোতাম কেড়ে নেয় না (NOT_FOCUSABLE)। */
            try {
                /* 🆕🔒 V856 — কল কেটে গেলে কার্ড সরে যায় (আগের মতোই), **শুধু
                   Missed হলে** কার্ডটা থেকে যায় যাতে স্টাফ দেখে সঙ্গে সঙ্গে
                   Remark লিখতে বা ফিরতি কল করতে পারেন। ⛔ সেটাও ৬০ সেকেন্ড
                   পরে নিজে থেকেই সরে যায় — পর্দায় কখনো আটকে থাকবে না। */
                if (ended && !activeMissed) {
                    CallOverlay.hide(ctx)
                } else if (CallOverlay.allowed(ctx)) {
                    val ovNumber = number
                    val ovName = cln(match?.name)
                    // ☎️ V1434 — কার্ডে LAST CALL আলাদা বক্সে (নিচে `box`), তাই শুধু পরিচয়-লাইনগুলো।
                    val ovLines = infoLines
                    val ovBox = box
                    val ovRemark = existingRemark
                    val ovSaved = match != null
                    val ovIsRmp = match?.isRmp == true
                    val ovBranch = cln(match?.branch)
                    val ovPatientId = cln(match?.patientId)
                    // 🆕 V856 — কলের ধরন ও শেষ কলের তথ্য।
                    val ovType = when {
                        activeMissed -> "MISSED"
                        direction == "outgoing" -> "OUTGOING"
                        else -> "INCOMING"
                    }
                    val ovAutoHide = activeMissed
                    val app = ctx.applicationContext
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        CallOverlay.show(
                            ctx = app,
                            number = ovNumber,
                            name = ovName,
                            lines = ovLines,
                            saved = ovSaved,
                            callType = ovType,               // 🆕 V856
                            autoHide = ovAutoHide,
                            box = ovBox,                     // ☎️ V1434
                            onOpen = {
                                /* ⛔ V844-এর হুবহু একই গন্তব্য — নতুন নিয়ম নয়। */
                                val i = when {
                                    !ovSaved -> Intent(app, EnquiryActivity::class.java)
                                        .putExtra("prefillMobile", ovNumber)
                                    ovIsRmp -> Intent(app, DoctorVisitActivity::class.java)
                                        .putExtra("searchMobile", ovNumber)
                                        .putExtra("searchBranch", ovBranch)
                                    else -> Intent(app, PatientTimelineActivity::class.java)
                                        .putExtra("mobile", ovNumber)
                                        .putExtra("preName", ovName)
                                        .putExtra("preBranch", ovBranch)
                                        .putExtra("prePatientId", ovPatientId)
                                }.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                try { app.startActivity(i) } catch (_: Throwable) { }
                            },
                            onRemark = {
                                /* ⛔ "📝 Add Remark" বোতামের হুবহু একই পর্দা ও extras। */
                                val i = Intent(app, CallRemarkActivity::class.java)
                                    .putExtra("mobile", ovNumber)
                                    .putExtra("direction", direction)
                                    .putExtra("patientId", match?.patientId.orEmpty())
                                    .putExtra("patientName", match?.name.orEmpty())
                                    .putExtra("branch", match?.branch.orEmpty())
                                    .putExtra("calledAt", calledAt)
                                    .putExtra("followupId", if (match != null && !match.isRmp) match.id else "")
                                    .putExtra("rmpId", if (match != null && match.isRmp) match.id else "")
                                    .putExtra("existingRemark", ovRemark)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                try { app.startActivity(i) } catch (_: Throwable) { }
                            }
                        )
                    }
                }
            } catch (_: Throwable) { /* কার্ড না দেখানো গেলেও কল/অ্যাপ কখনো আটকাবে না */ }
        } catch (_: Throwable) {
            // নোটিফিকেশন ব্যর্থ হলেও কল/অ্যাপ কখনো আটকাবে না।
        }
    }

    private fun pendingActivity(ctx: Context, code: Int, intent: Intent): PendingIntent {
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(ctx, code, intent, flags)
    }
}
