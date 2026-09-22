package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🆕🔒 খাতার সারি — Dialer (TK-নির্দেশ, 05.08.2026 — "শুধুমাত্র ব্রাঞ্চের
 * সিমের কল")। "কোন SIM ব্রাঞ্চের নম্বর" — এই প্রশ্নটা প্রথম Work Notebook-এ
 * (`WorkNotebookActivity.kt`, খাতার সারি B440/B441) বসানো হয়েছিল, একই
 * `SharedPreferences("wn_prefs")` ঘর `"branch_sim_slot"`-এ জমা থাকে।
 *
 * এই ফাইলটা **সেই একই ঘর পড়ে** — তাই স্টাফকে দ্বিতীয়বার (Dialer খুললে
 * আবার) এই প্রশ্ন করা হবে না, Work Notebook-এ একবার বেছে দিলেই দুই
 * জায়গাতেই কাজ করবে। ⛔ `WorkNotebookActivity.kt`-এর একটা অক্ষরও ছোঁয়া
 * হয়নি — এই ফাইলটা শুধু সেই একই preference **পড়ে**, লেখে না (Work
 * Notebook-এর নিজের বাছাই-প্রশ্নের UI-ই একমাত্র জায়গা যেখানে এই মান
 * সেভ হয়)।
 *
 * Work Notebook স্ক্রিন কখনো খোলা না হলে (নতুন ইনস্টল, বা স্টাফ ওই মডিউল
 * ব্যবহারই করেন না) এই ঘরে কিছু থাকবে না — তখন Dialer নিজেই একবার একই
 * প্রশ্ন জিজ্ঞাসা করে সেই একই ঘরে সেভ করে (তাই পরে Work Notebook খুললে
 * সেটাও আর জিজ্ঞাসা করবে না — দুই দিকেই শেয়ার হয়)।
 */
object BranchSimHelper {

    private fun prefs(context: Context) = context.getSharedPreferences("wn_prefs", Context.MODE_PRIVATE)

    fun hasChosen(context: Context): Boolean = prefs(context).contains("branch_sim_slot")

    // 🔴🔴🔒 B509 (06.08.2026, TK-রিপোর্ট — "না বলেছে, তারপরও কাউন্টিং
    // করছে") — আসল কারণ: এক-সিম ফোনে পুরনো (আজকের চেম্বার-নম্বর প্রশ্ন
    // তৈরি হওয়ার আগের) কোডে **কোনো প্রশ্ন ছাড়াই** নিঃশব্দে `save(-1)`
    // বসে যেত ("পুরো ফোনটাই চেম্বারের" ধরে নিয়ে) — এই পুরনো, কখনো
    // সত্যিই-জিজ্ঞাসা-না-করা মান এখনো ফোনে থেকে গেলে `hasChosen()`
    // true দেখাত, আর নতুন চেম্বার-নম্বর প্রশ্নটা (grandfather-ক্লজে)
    // চিরকালের জন্য আর কখনো জিজ্ঞাসাই হতো না — স্টাফ "না" বললেও সেটা
    // আসলে জিজ্ঞাসাই করা হয়নি। **এখন শুধু সত্যিকারের, দুই-বা-তার বেশি
    // SIM-এর মধ্যে থেকে হাতে-বাছা সিদ্ধান্তকেই (`savedSlot() >= 0`)
    // grandfather করা হয় — এক-সিম ফোনের পুরনো নিঃশব্দ -1 আর গণ্য হবে
    // না, তাদের জন্য নতুন প্রশ্নটা এখন সত্যিই একবার জিজ্ঞাসা করা হবে।**
    // 🔴🔴🔴🔒 B517 (06.08.2026, TK-এর ফোনে সত্যিকারের ক্র্যাশ —
    // StackOverflowError, Google Crash-লগ প্রুফে ধরা পড়েছে) — B509-এর
    // ফিক্সে একক-সিম ফোনের জন্য নতুন, গুরুতর বাগ ঢুকে গিয়েছিল: একক-সিম
    // ফোনে `askWhichSimSlot()` লেজিটিমেটভাবেই `save(-1)` করে (কোনো
    // ফিল্টার লাগবে না মানে), কিন্তু `hasGenuinelyChosenSim()` শুধু
    // `savedSlot() >= 0` চেক করত — তাই -1 কখনো "genuinely chosen" ধরা
    // হতো না, আর `maybeAskWhichSimIsBranch()` প্রতিবার আবার
    // `askWhichSimSlot()` ডেকে ফেলত → অসীম লুপ (loadCallLog ↔
    // maybeAskWhichSimIsBranch ↔ askWhichSimSlot)। **এখন সঠিক নিয়ম:**
    // সঞ্চিত সিদ্ধান্ত বিশ্বাসযোগ্য যদি — (ক) সত্যিই দুই-বা-তার-বেশি
    // SIM থেকে হাতে বাছা হয় (`savedSlot() >= 0`), **অথবা** (খ) নতুন
    // চেম্বার-নম্বর প্রশ্নের ফ্লো ইতিমধ্যে সম্পূর্ণ হয়ে গেছে
    // (`hasChamberAnswer()` true) — এই দ্বিতীয় শর্তটাই একক-সিম ফোনের
    // বৈধ `-1` রেজোলিউশনকে সঠিকভাবে "সম্পন্ন" ধরবে, লুপ ভাঙবে। B489-এর
    // মূল উদ্দেশ্য (পুরনো, কখনো-প্রশ্ন-না-করা `-1` গ্রহণযোগ্য না) অক্ষত
    // থাকে, কারণ সেই পুরনো ডেটায় `hasChamberAnswer()` মিথ্যা থাকে।
    fun hasGenuinelyChosenSim(context: Context): Boolean =
        hasChosen(context) && (savedSlot(context) >= 0 || hasChamberAnswer(context))

    fun savedSlot(context: Context): Int = prefs(context).getInt("branch_sim_slot", -1)

    fun save(context: Context, slot: Int) {
        prefs(context).edit().putInt("branch_sim_slot", slot).apply()
    }

    // 🆕🔒 TK-নির্দেশ (06.08.2026, খাতার সারি B484 — "যে ফোনে চেম্বারের
    // নম্বর থাকবে, শুধু সেই ফোনের কল লিস্ট দেখাবে, স্টাফ-মাস্টার সবার
    // ক্ষেত্রেই")। **আসল কারণ যেটা ঠিক করা হচ্ছে:** এক-সিম ফোনে আগে কখনো
    // প্রশ্নই করা হতো না — ধরে নেওয়া হতো পুরো ফোনটাই চেম্বারের (স্লট -1,
    // সব কল দেখাত)। এটা ডেডিকেটেড ব্রাঞ্চ-ফোনে ঠিক, কিন্তু TK-এর মতো
    // ব্যক্তিগত/মিশ্র-ব্যবহারের ফোনে ভুল — ব্যক্তিগত কলও (যেমন পরিবারের
    // নম্বর) দেখা যেত। এখন **সিম কটা তা না দেখেই প্রথমে জিজ্ঞাসা করা হয়**
    // — "এই ফোনে কি চেম্বারের নম্বর আছে?" — হ্যাঁ হলে তবেই এক-সিম/দুই-সিম
    // বাছাই এগোয়, না হলে Dialer কখনো কোনো কল দেখাবে না (ব্যক্তিগত কল
    // ফাঁস হওয়ার ঝুঁকি নেই)।
    // ⛔ **আগে থেকে সিম বেছে রাখা ফোন (`hasChosen()==true`) স্পর্শ করা
    // হয়নি** — তাঁরা আগেই এই প্রশ্নের উত্তর কার্যত দিয়ে দিয়েছেন (সিম
    // বেছেছেন মানে "হ্যাঁ, আছে"), তাই আবার জিজ্ঞাসা করা হবে না, চলতি
    // স্টাফদের কাজ থামবে না।
    fun hasChamberAnswer(context: Context): Boolean = prefs(context).contains("has_chamber_number")

    /* 🔴🔒 V908 (৩১.০৮.২০২৬, JPE-CRP-এর রিপোর্ট — TK: *"চেম্বারের ফোনে
       ক্লিনিকের সিম আছে"*)। **এখানেই আসল দোষ ছিল:** উপরের grandfather-লাইনটা
       স্টাফের **নিজের সাফ উত্তরের আগে** বসত। তাই যে ফোনে পুরনো (নিঃশব্দে বসা)
       সিম-বাছাই জমা ছিল, সেখানে স্টাফ *"না, এই ফোনে চেম্বারের নম্বর নেই"*
       বললেও এই ঘর **হ্যাঁ**-ই বলত ⇒ ব্যক্তিগত ফোনের নিজের কলও
       "Superfone/Clinic Number Call"-এ গোনা হয়ে যেত (B509-এ TK ঠিক এই কথাই
       বলেছিলেন — *"না বলেছে, তারপরও কাউন্টিং করছে"*)।
       **এখন নিয়ম:** স্টাফ একবার উত্তর দিয়ে থাকলে **সেই উত্তরই চূড়ান্ত**;
       grandfather শুধু তখনই, যখন কোনো উত্তরই দেওয়া হয়নি।
       ⛔ যে ফোনে "হ্যাঁ" আছে, তার আচরণ এক অক্ষরও বদলায়নি। */
    fun hasChamberNumber(context: Context): Boolean {
        val p = prefs(context)
        if (p.contains("has_chamber_number")) return p.getBoolean("has_chamber_number", false)
        if (hasGenuinelyChosenSim(context)) return true // grandfathered — সত্যিই একাধিক SIM থেকে হাতে বেছেছেন
        return false
    }

    fun saveHasChamberNumber(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("has_chamber_number", value).apply()
    }

    /** 🟢🔒 V619 (২৪.০৮.২০২৬, TK-রিপোর্ট — নিজের/Master-এর ফোনে, যেখানে
     *  চেম্বারের নম্বর আছে বলে কখনো জানানোই হয়নি, তাও কল-নোটিফিকেশন
     *  দেখাচ্ছিল) — `hasGenuinelyChosenSim()`/`hasChamberNumber()`
     *  Dialer-এর কল-লগ **দেখানোর** জন্য ইচ্ছাকৃতভাবে নরম (পুরনো এক-সিম
     *  ফোনে নিঃশব্দে grandfather করে, `hasChamberAnswer()` থাকলেই যথেষ্ট
     *  ধরে — আসল উত্তর হ্যাঁ না না তা না দেখেই)। এটা Dialer-এর জন্য ঠিক
     *  ছিল, কিন্তু কল-শনাক্তকরণ/নোটিফিকেশনের জন্য **অনেক বেশি নরম** —
     *  TK স্পষ্ট বলেছিলেন "শুধু যারা আগেই হ্যাঁ বলেছে"। এই ফাংশনটা তাই
     *  সরাসরি **আসল সংরক্ষিত হ্যাঁ/না** পড়ে, grandfather-ছাড় ছাড়াই —
     *  শুধু (ক) সত্যিই একাধিক SIM থেকে হাতে-বাছা (`savedSlot >= 0`,
     *  এটা নিজেই দ্ব্যর্থহীন "হ্যাঁ"), অথবা (খ) প্রশ্নের উত্তরে সত্যিই
     *  "হ্যাঁ" বলা হয়েছে (raw বুলিয়ান, কোনো ছাড় ছাড়া) — এই দুটোতেই
     *  শুধু true। ⛔ Dialer-এর কল-লগ দেখানোর নিয়ম এক অক্ষরও বদলায়নি —
     *  এই নতুন ফাংশনটা শুধু নতুনভাবে যোগ হলো, কেউ ডাকছে না মানে কিছু
     *  বদলায় না। */
    fun hasExplicitlyConfirmedChamberSim(context: Context): Boolean {
        if (savedSlot(context) >= 0) return true
        return hasChamberAnswer(context) && prefs(context).getBoolean("has_chamber_number", false)
    }

    /* 🔴🔒 V1429 (১৩.০৯.২০২৬, যাচাইকারীর ধরা ফাঁক) — V1427 শুধু **ভবিষ্যতের** চুপচাপ "না"
       বন্ধ করেছিল; যে ফোনে পুরনো auto-detect ইতিমধ্যে "না" লিখে রেখেছে (TK-র রিপোর্টের
       ঠিক সেই ফোনগুলো) সেখানে ব্যানার তবু বন্ধই থাকত। ফোনে "সত্যিকারের না" আর "চুপচাপ না"
       আলাদা করে রাখা নেই — তাই **একবারই** (এই বিল্ডে প্রথম Home খোলার সময়): উত্তর "না" অথচ
       কোনো SIM-স্লট বাছা নেই ⇒ উত্তরটা মুছে দেওয়া হয়, স্টাফকে আবার একবার প্রশ্ন করা হবে
       (Dialer/Work Notebook/Call ID Banner সেটআপ — যেটা আগে খোলে)। সত্যিই "না" হলে আবার
       "না" বললেই শেষ। ⛔ "হ্যাঁ" বা হাতে-বাছা স্লট ছোঁয়া হয় না। ⛔ দ্বিতীয়বার আর চলে না। */
    fun resetSilentNoOnce(context: Context) {
        try {
            val p = prefs(context)
            if (p.getBoolean("v1429_silent_no_reset", false)) return
            p.edit().putBoolean("v1429_silent_no_reset", true).apply()
            if (p.contains("has_chamber_number") && !p.getBoolean("has_chamber_number", false) && savedSlot(context) < 0) {
                p.edit().remove("has_chamber_number").apply()
            }
        } catch (_: Throwable) { }
    }

    fun clearChamberAnswer(context: Context) {
        prefs(context).edit().remove("has_chamber_number").apply()
    }

    // 🆕🔒 B485 (06.08.2026, TK-নির্দেশ) — প্রশ্ন করার আগে অ্যাপ নিজে একবার
    // চেষ্টা করে দেখে ফোনের নিজের নম্বরটা পড়ে, প্রজেক্টের আগে-থেকে-থাকা
    // ব্রাঞ্চ-নম্বরের তালিকার (`BranchCatalog.all`) সাথে মিলিয়ে। মিললে/না
    // মিললে জানা যায় (`true`/`false`) — TK-কে আর প্রশ্ন করতে হয় না। ⚠️
    // **সৎ সীমাবদ্ধতা:** ভারতীয় সিমে (Jio/Airtel/Vi/BSNL) Android প্রায়ই
    // নিজের নম্বর ফেরত দিতে পারে না (ফোন/অনুমতি ঠিক থাকলেও) — এটা
    // Android/অপারেটরের নিজস্ব সীমাবদ্ধতা, কোড দিয়ে ঠিক করা যায় না। তাই
    // পড়া না গেলে (`null`) — আগের মতোই প্রশ্ন করা হয়, কিছু ভাঙে না।
    fun tryAutoDetectChamberNumber(context: Context): Boolean? {
        if (!hasPhoneStatePermission(context)) return null
        return try {
            @Suppress("DEPRECATION")
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val myNumber = tm?.line1Number?.filter { it.isDigit() }?.takeLast(10)
            if (myNumber.isNullOrBlank() || myNumber.length < 10) return null
            val branchNumbers = com.tkbiswas.pilesclinic.print.BranchCatalog.all
                .map { it.phoneLine.filter { c -> c.isDigit() }.takeLast(10) }
                .toSet()
            branchNumbers.contains(myNumber)
        } catch (_: Throwable) { null }
    }

    fun hasPhoneStatePermission(context: Context): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasCallLogPermission(context: Context): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CALL_LOG
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** একাধিক SIM থাকলে (নাম, স্লট) তালিকা — একটাই থাকলে খালি (প্রশ্নের
     *  দরকারই নেই)। ⛔ WorkNotebookActivity.kt-এর `maybeAskWhichSimIsBranch()`-
     *  এর হুবহু একই যুক্তি, শুধু এখানে পুনর্ব্যবহারযোগ্য করে রাখা হলো। */
    fun activeSimSlots(context: Context): List<Pair<Int, String>> {
        return try {
            if (!hasPhoneStatePermission(context)) emptyList()
            else {
                val sm = context.getSystemService(android.telephony.SubscriptionManager::class.java)
                sm?.activeSubscriptionInfoList?.map {
                    it.simSlotIndex to (it.displayName?.toString() ?: "SIM ${it.simSlotIndex + 1}")
                } ?: emptyList()
            }
        } catch (_: Throwable) { emptyList() }
    }

    /** বাছা স্লটের subscriptionId-গুলো (PHONE_ACCOUNT_ID মেলানোর জন্য)। খালি
     *  ফেরত মানে "মেলাতে পারিনি" — কল-করা কোডকে **সবগুলো SIM** গোনার
     *  ফলব্যাকে যেতে হবে (WorkNotebookActivity-র একই সৎ সীমাবদ্ধতা)। */
    private fun branchSubIds(context: Context): Set<String> {
        val slot = savedSlot(context)
        if (slot < 0 || !hasPhoneStatePermission(context)) return emptySet()
        return try {
            val sm = context.getSystemService(android.telephony.SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList
                ?.filter { it.simSlotIndex == slot }
                ?.map { it.subscriptionId.toString() }
                ?.toSet() ?: emptySet()
        } catch (_: Throwable) { emptySet() }
    }

    data class CallLogRow(
        val number: String,
        val type: Int,
        val dateMs: Long,
        // 🆕 B470 (06.08.2026, TK-নির্দেশ) — কত সেকেন্ড কথা হয়েছিল। ডিফল্ট
        // 0L রাখা হয়েছে যাতে এই ডেটা ক্লাসের আগে-থেকে-থাকা তিন-প্যারামিটার
        // ব্যবহারগুলো (positional args) একটাও না ভাঙে।
        val durationSec: Long = 0L
    )

    /** আজ মধ্যরাত থেকে এখন পর্যন্ত — ব্রাঞ্চের SIM বাছা থাকলে শুধু সেই
     *  SIM-এর কল, না মেলাতে পারলে সব SIM (WorkNotebook-এর একই ফলব্যাক
     *  নিয়ম)। ⛔ কোনো ক্লাউড-কল নেই, পুরোটাই ফোনের নিজের Call Log পড়া। */
    fun fetchTodayCallLog(context: Context): List<CallLogRow> {
        val out = ArrayList<CallLogRow>()
        if (!hasCallLogPermission(context)) return out
        // 🆕🔒 B484 (06.08.2026) — এই ফোনে চেম্বারের নম্বর নেই বলে জানানো
        // থাকলে (ও আগে কখনো সিম বাছেননি), কোনো কলই দেখানো হয় না —
        // ব্যক্তিগত কল ফাঁস হওয়ার ঝুঁকি এড়াতে।
        // 🔴🔴🔒 B489 (06.08.2026, TK-রিপোর্ট — Dialer ফ্রিজ হয়ে যাচ্ছিল) —
        // B484-এর এই গেট সাময়িকভাবে বন্ধ (DialerActivity.kt-এর একই
        // মন্তব্য দেখুন, পুরো ব্যাখ্যা ওখানে)। এই ফাংশন Dialer ও Work
        // Notebook দুটোতেই শেয়ার হয় (B488)।
        // 🔴🔒 B491 (06.08.2026) — আসল কারণ Call Log অনুমতি ছিল, এই গেট
        // না — TK লাইভ টেস্টে নিশ্চিত করার পরে আবার চালু।
        /* 🔴🔒 V908 — সাফ "না" এখন সিম-বাছাইকেও হারায় (উপরের টীকা দেখুন)।
           ⇒ ব্যক্তিগত ফোনের কল আর ক্লিনিকের গোনায় ঢোকে না। */
        if (hasChamberAnswer(context) && !hasChamberNumber(context)) return out // 🔴🔒 B509 · V908
        try {
            val midnight = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val branchSubIdsSet = branchSubIds(context)
            context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.TYPE,
                    android.provider.CallLog.Calls.DATE,
                    android.provider.CallLog.Calls.PHONE_ACCOUNT_ID,
                    android.provider.CallLog.Calls.DURATION
                ),
                "${android.provider.CallLog.Calls.DATE} >= ?",
                arrayOf(midnight.toString()),
                "${android.provider.CallLog.Calls.DATE} DESC"
            )?.use { c ->
                val numIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                val typeIdx = c.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val accIdx = c.getColumnIndex(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID)
                val durIdx = c.getColumnIndex(android.provider.CallLog.Calls.DURATION)
                while (c.moveToNext()) {
                    if (branchSubIdsSet.isNotEmpty()) {
                        val acc = if (accIdx >= 0) c.getString(accIdx) else null
                        if (acc != null && branchSubIdsSet.none { acc.contains(it) }) continue
                    }
                    val num = (if (numIdx >= 0) c.getString(numIdx) else null) ?: continue
                    val type = if (typeIdx >= 0) c.getInt(typeIdx) else 0
                    val dateMs = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                    val durationSec = if (durIdx >= 0) c.getLong(durIdx) else 0L
                    out.add(CallLogRow(num, type, dateMs, durationSec))
                }
            }
        } catch (_: Throwable) { }
        return out
    }

    /* ═══════════════════════════════════════════════════════════════════
       ☎️🔒 V963 (০১.০৯.২০২৬, TK-এর লক করা নির্দেশ) — TK: *"চেম্বার এর ফোনে যখন
       ফোন আসবে তখন স্টাফ এর কোন হাত থাকবে না, এটা অটোমেটিক অ্যাপ counting
       করবে"* · *"প্রথম কলটাই ধরবেন"* · *"দিনের দিন, বড়জোর তারপরের দিন"*।

       এই ঘরটা একটা নম্বরের **সবচেয়ে প্রথম কলের সময়** ফেরত দেয় — ব্রাঞ্চের
       SIM-এর কল-লগ থেকে, গত ৩ দিনের মধ্যে (ইনকামিং · আউটগোয়িং · মিসড
       তিনটেই, কারণ TK-এর নিয়মে আউটগোয়িং কলও গোনা হয়)।
       ⇒ রাত ১০টার কল পরের দিন দুপুরে ফর্মে তুললেও **ওই কলের সময়টাই** ধরা হয়।

       ⛔ ক্লাউডে একটাও অনুরোধ নেই — পুরোটাই ফোনের নিজের Call Log পড়া।
       ⛔ উপরের `fetchTodayCallLog()`-এর **হুবহু একই** নিরাপত্তা-গেট: অনুমতি
          নেই ⇒ কিছু নয় · এই ফোনে চেম্বারের নম্বর নেই বলে জানানো থাকলে ⇒ কিছু
          নয় (ব্যক্তিগত কল কখনো গোনায় ঢুকবে না) · ব্রাঞ্চের SIM বাছা থাকলে
          শুধু সেই SIM-এর কল।
       ⛔ না পাওয়া গেলে `null` — তখন ডাকা জায়গাটা আগের নিয়মেই চলে।
       ═══════════════════════════════════════════════════════════════════ */
    private const val CALL_LOOKBACK_DAYS = 3L

    fun firstCallTimeMs(context: Context, mobile: String): Long? {
        if (!hasCallLogPermission(context)) return null
        if (hasChamberAnswer(context) && !hasChamberNumber(context)) return null
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return null
        return try {
            val since = System.currentTimeMillis() - CALL_LOOKBACK_DAYS * 24L * 60L * 60L * 1000L
            val branchSubIdsSet = branchSubIds(context)
            var earliest: Long? = null
            context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.DATE,
                    android.provider.CallLog.Calls.PHONE_ACCOUNT_ID
                ),
                "${android.provider.CallLog.Calls.DATE} >= ?",
                arrayOf(since.toString()),
                "${android.provider.CallLog.Calls.DATE} ASC"
            )?.use { c ->
                val numIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val accIdx = c.getColumnIndex(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID)
                while (c.moveToNext()) {
                    if (branchSubIdsSet.isNotEmpty()) {
                        val acc = if (accIdx >= 0) c.getString(accIdx) else null
                        if (acc != null && branchSubIdsSet.none { acc.contains(it) }) continue
                    }
                    val num = (if (numIdx >= 0) c.getString(numIdx) else null) ?: continue
                    if (num.filter { ch -> ch.isDigit() }.takeLast(10) != digits) continue
                    val dateMs = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                    if (dateMs > 0L) { earliest = dateMs; break }   // ASC — প্রথমটাই সবচেয়ে পুরনো
                }
            }
            earliest
        } catch (_: Throwable) { null }
    }

    /**
     * 🆕🔒 খাতার সারি — Dialer → Missed ট্যাব (TK-নির্দেশ, 05.08.2026 —
     * "বেল-নোটিফিকেশন হবে, ফিরতি কল বাকি মনে করাবে")। একটা মিসড কল
     * "বাকি" থাকে যতক্ষণ না **সেই একই নম্বরে পরে** একটা Outgoing কল করা
     * হয়েছে (ফিরতি কল দেওয়া হয়ে গেছে) — এটা ফোনের নিজের Call Log থেকেই
     * বোঝা যায়, তাই আলাদা কোনো "handled" চিহ্ন সেভ করে রাখতে হয়নি, নতুন
     * কোনো SQL/কলাম লাগেনি। ⛔ শুধু এই ফোনের স্থানীয় গণনা — ক্লাউডে কিছু
     * যায় না, তাই `BellCounter.count()`-এ যোগ করলেও কোটার উপর কোনো চাপ
     * পড়ে না।
     */
    /* ☎️🔒 V1427 (১৩.০৯.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — Truecaller-এর মতো:
       কল এলে ব্যানারে এই নম্বরের **শেষ Missed · শেষ Outgoing · শেষ Incoming**
       কখন হয়েছিল ("10 min ago" · "1 day ago" · তারিখ)।
       ⛔ পুরোটাই **এই ফোনের নিজের Call Log** থেকে — ক্লাউডে একটাও অনুরোধ নেই
          (ফ্রি-প্ল্যানে শূন্য চাপ), Truecaller-ও ঠিক এভাবেই দেখায়।
       ⛔ উপরের `fetchTodayCallLog()`-এর **হুবহু একই** নিরাপত্তা-গেট: অনুমতি
          নেই ⇒ ফাঁকা · এই ফোনে চেম্বারের নম্বর নেই বলা থাকলে ⇒ ফাঁকা ·
          ব্রাঞ্চের SIM বাছা থাকলে শুধু সেই SIM-এর কল।
       ⛔ কল বাজার সময় ব্যাকগ্রাউন্ড থ্রেডে একবারই পড়া হয় (CallNotifyManager),
          মূল থ্রেড কখনো আটকায় না; সর্বোচ্চ ৩০০ সারি দেখা হয়। */
    data class CallHistory(
        val missedMs: Long = 0L,
        val outgoingMs: Long = 0L,
        val incomingMs: Long = 0L
    ) {
        /** সবচেয়ে নতুনটা আগে — (লেবেল, কখন) জোড়া; না থাকলে ফাঁকা তালিকা। */
        fun rows(now: Long = System.currentTimeMillis()): List<Pair<String, String>> =
            listOf("Missed" to missedMs, "Outgoing" to outgoingMs, "Incoming" to incomingMs)
                .filter { it.second > 0L }
                .sortedByDescending { it.second }
                .map { it.first to agoText(it.second, now) }

        /** ☎️🔒 V1434 (১৩.০৯.২০২৬, TK-নির্দেশ, ডেমো পাশ) — **শুধু শেষ কলটা**
         *  (যে ধরনেরই হোক): (লেবেল, কখন-ms); কিছু না থাকলে null। */
        fun latest(): Pair<String, Long>? =
            listOf("Missed" to missedMs, "Outgoing" to outgoingMs, "Incoming" to incomingMs)
                .filter { it.second > 0L }
                .maxByOrNull { it.second }
    }

    /** ☎️🔒 V1434 — TK-নির্দেশ: *"আজকে হলে Today তার পাশে সময়, গতকাল হলে
     *  Yesterday তার পাশে সময়, গতকালের আগে হলে তারিখ এবং সময়"*।
     *  → "Today 10:15 AM" · "Yesterday 4:32 PM" · "27/08/2026 6:05 PM"। */
    fun whenText(ms: Long, now: Long = System.currentTimeMillis()): String {
        val tf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
        val time = tf.format(java.util.Date(ms))
        val dayKey = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
        val d = dayKey.format(java.util.Date(ms))
        val today = dayKey.format(java.util.Date(now))
        val yesterday = dayKey.format(java.util.Date(now - 24L * 3_600_000L))
        return when (d) {
            today -> "Today $time"
            yesterday -> "Yesterday $time"
            else -> java.text.SimpleDateFormat(DateUtil.DISPLAY_DATE, java.util.Locale.US).format(java.util.Date(ms)) + " " + time
        }
    }

    /** ☎️ V1434 — ISO লেখা (call_remarks.calledAt, UTC 'Z' বা টাইমজোন-ছাড়া) → ms; না পারলে 0। */
    fun isoToMillis(iso: String): Long {
        // মাইক্রোসেকেন্ড (.123456) থাকলে ৩ অঙ্কে ছাঁটা — নইলে SSS ওটাকে ১২৩৪৫৬ ms ধরে মিনিট বাড়িয়ে দেয়।
        val raw = iso.trim().replace(Regex("(\\.\\d{3})\\d+"), "$1")
        if (raw.isBlank()) return 0L
        val fmts = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to true, "yyyy-MM-dd'T'HH:mm:ss'Z'" to true,
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" to false, "yyyy-MM-dd'T'HH:mm:ssXXX" to false,
            "yyyy-MM-dd'T'HH:mm:ss" to false, "yyyy-MM-dd HH:mm:ss" to false
        )
        for ((pat, utc) in fmts) {
            try {
                val f = java.text.SimpleDateFormat(pat, java.util.Locale.US)
                if (utc) f.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val t = f.parse(raw)?.time ?: continue
                if (t > 0L) return t
            } catch (_: Throwable) { }
        }
        return 0L
    }

    fun lastCallsByType(context: Context, mobile: String): CallHistory {
        if (!hasCallLogPermission(context)) return CallHistory()
        if (hasChamberAnswer(context) && !hasChamberNumber(context)) return CallHistory()
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return CallHistory()
        return try {
            val branchSubIdsSet = branchSubIds(context)
            var missed = 0L; var outgoing = 0L; var incoming = 0L
            var seen = 0
            context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.TYPE,
                    android.provider.CallLog.Calls.DATE,
                    android.provider.CallLog.Calls.PHONE_ACCOUNT_ID
                ),
                "${android.provider.CallLog.Calls.NUMBER} LIKE ?",
                arrayOf("%$digits"),
                "${android.provider.CallLog.Calls.DATE} DESC"
            )?.use { c ->
                val numIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                val typeIdx = c.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val accIdx = c.getColumnIndex(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID)
                while (c.moveToNext() && seen < 300) {
                    seen++
                    if (branchSubIdsSet.isNotEmpty()) {
                        val acc = if (accIdx >= 0) c.getString(accIdx) else null
                        if (acc != null && branchSubIdsSet.none { acc.contains(it) }) continue
                    }
                    val num = (if (numIdx >= 0) c.getString(numIdx) else null) ?: continue
                    if (num.filter { ch -> ch.isDigit() }.takeLast(10) != digits) continue
                    val type = if (typeIdx >= 0) c.getInt(typeIdx) else 0
                    val dateMs = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                    if (dateMs <= 0L) continue
                    when (type) {
                        android.provider.CallLog.Calls.MISSED_TYPE,
                        android.provider.CallLog.Calls.REJECTED_TYPE -> if (missed == 0L) missed = dateMs
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> if (outgoing == 0L) outgoing = dateMs
                        android.provider.CallLog.Calls.INCOMING_TYPE -> if (incoming == 0L) incoming = dateMs
                    }
                    if (missed > 0L && outgoing > 0L && incoming > 0L) break   // তিনটেই পাওয়া গেছে
                }
            }
            CallHistory(missed, outgoing, incoming)
        } catch (_: Throwable) { CallHistory() }
    }

    /** "Just now" · "10 min ago" · "3 hr ago" · "1 day ago" · তার বেশি হলে তারিখ (dd/MM/yyyy)। */
    fun agoText(ms: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - ms
        if (diff < 0L) return DateUtil.displayWithTime(java.util.Date(ms))
        val min = diff / 60_000L
        val hr = diff / 3_600_000L
        return when {
            min < 1L -> "Just now"
            min < 60L -> "$min min ago"
            hr < 24L -> "$hr hr ago"
            hr < 48L -> "1 day ago"
            else -> java.text.SimpleDateFormat(DateUtil.DISPLAY_DATE, java.util.Locale.US).format(java.util.Date(ms))
        }
    }

    fun countPendingMissedCallbacks(context: Context): Int {
        return try {
            pendingMissedCallbackNumbers(context).size
        } catch (_: Throwable) { 0 }
    }

    // 🆕 (06.08.2026, নতুন Notifications পাতার জন্য — TK-অনুমোদনে) —
    // countPendingMissedCallbacks()-এর হুবহু একই বাছাই-নিয়ম, কিন্তু নম্বর ও
    // সময়সহ পুরো তালিকা দেয় (শুধু গোনা নয়)। countPendingMissedCallbacks()
    // এখন এটাই ডাকে, তাই দুটো কখনো আলাদা হতে পারবে না। ⛔ ক্লাউডে কিছু
    // যায় না, আগের মতোই ফোনের নিজের Call Log।
    fun pendingMissedCallbackNumbers(context: Context): List<CallLogRow> {
        val rows = fetchTodayCallLog(context)
        if (rows.isEmpty()) return emptyList()
        val byNumber = rows.groupBy { it.number.filter { d -> d.isDigit() }.takeLast(10) }
        val out = ArrayList<CallLogRow>()
        for ((_, calls) in byNumber) {
            val lastMissed = calls.filter { it.type == android.provider.CallLog.Calls.MISSED_TYPE }
                .maxByOrNull { it.dateMs } ?: continue
            val calledBackAfter = calls.any {
                it.type == android.provider.CallLog.Calls.OUTGOING_TYPE && it.dateMs > lastMissed.dateMs
            }
            if (!calledBackAfter) out.add(lastMissed)
        }
        out.sortByDescending { it.dateMs }
        return out
    }
}
