package com.tkbiswas.pilesclinic.native

import android.app.Activity
import androidx.appcompat.app.AlertDialog

/**
 * 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত) — একই দিনে দ্বিতীয়বার টাকা নেওয়ার আগে সতর্কবার্তা
 *
 * TK-এর কথা: *"কোন এক স্টাফ কোন এক পেশেন্টের পেমেন্ট নিতে ভুল করে ৩-৪ বার নিয়ে
 * নিয়েছে... হয়তো তখন ইন্টারনেট স্লো ছিল, বুঝতে পারেনি যে পেমেন্ট হয়ে গেছে...
 * প্রয়োজনে স্টাফকে সেইভাবে ওয়ার্নিং দেবে।"*
 *
 * TK-এর সিদ্ধান্ত (28.07.2026): টাকাটা **সোজা যোগ হবে না** — আগে স্টাফকে
 * জিজ্ঞাসা করা হবে, তিনি "নতুন টাকা" বললে তবেই যোগ হবে।
 *
 * 🔒 V452 (19.08.2026, TK-approved A): **কোনো দ্বিতীয় daily payment row তৈরি হবে না**।
 *    "Add to today's payment" বললে genuine new money ওই দিনের existing Treatment
 *    Payment-এ যোগ হবে; Cash/Online split আলাদা থাকবে। "No" বললে কিছুই হয় না।
 * ⛔ **আজ কিছু নেওয়া না হয়ে থাকলে কোনো পপ-আপই আসে না** — স্টাফের রোজকার
 *    কাজে একটাও বাড়তি ট্যাপ পড়ে না।
 * ⛔ **ক্লাউডে বাড়তি কোনো অনুরোধ নেই** — অঙ্কটা যে তালিকা আগেই নামানো হয়েছে
 *    তা থেকেই আসে (`PaymentRepository.paidOnDateFor`)।
 * ⛔ অ্যাপের নিয়ম মেনে লেখা **শুধু ইংরেজিতে**।
 */
object PaymentDayGuard {

    /**
     * @param alreadyPaid ওই রোগীর নামে আজ ইতিমধ্যে নেওয়া মোট টাকা (০ হলে সোজা [onProceed])।
     * @param patientName পর্দায় দেখানোর নাম।
     * @param todayLabel আজকের দিনটা কোন নম্বরে পড়ছে (`Advance` / `2nd Payment` …)।
     * @param onProceed স্টাফ "Add to today's payment" বললে existing daily payment-এ
     * new money যোগ করার একই save path চলবে।
     */
    /* 🔴🔒 V1504 (১৫.০৯.২০২৬, TK-রিপোর্ট — RIMPA ROY-র ₹2,000 দুবার, ছবিসহ) —
       নতুন ঐচ্ছিক `onCancel` (ডিফল্ট কিছুই করে না, তাই পুরনো কোনো ডাক এক
       অক্ষরও বদলায় না)। কল-করা পর্দাগুলো এখন "No, cancel"-এও Save-বোতামের
       তালা খুলে দিতে পারে — নইলে জিজ্ঞাসা করার মুহূর্তে (যেটা ক্লাউড-যাচাই
       শেষে আসে) বোতাম আটকে থাকত না, তাই ধীর নেটে বারবার চাপলে একাধিক
       প্রশ্ন একসাথে চলত আর প্রতিটায় "হ্যাঁ" চাপলে সত্যিকারের বাড়তি টাকা
       জমা হয়ে যেত। বিস্তারিত কারণ `confirmBeforeSave`-এর মন্তব্যে। */
    fun confirmIfAlreadyPaidToday(
        activity: Activity,
        alreadyPaid: Double,
        patientName: String,
        todayLabel: String,
        onProceed: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        if (alreadyPaid <= 0.0) { onProceed(); return }
        if (activity.isFinishing || activity.isDestroyed) { onCancel(); return }
        val who = patientName.ifBlank { "this patient" }
        // অ্যাপের সব জায়গার মতোই একই ধাঁচ — `₹10,000`।
        val amt = "₹" + "%,.0f".format(alreadyPaid)
        /* 🟡🔒 V814 (২৮.০৮.২০২৬, TK-নির্দেশ, ছবিসহ: *"এত বড় মেসেজ কেন?
           শর্টকাট দিলে ভালো হয়"*) — লেখাটা চার অনুচ্ছেদ থেকে **দুই লাইনে**।
           ⛔ যা জানার দরকার তার একটাও হারায়নি: কত টাকা · কার · কত নম্বর
              পেমেন্ট · আর প্রশ্নটা কী। বাকি ব্যাখ্যা (দ্বিতীয় সারি হবে না,
              CASH/ONLINE আলাদা গোনা) — ওটা নিয়মেই আছে, প্রতিবার পড়ার দরকার নেই।
           ⛔ কাজের নিয়ম এক অক্ষরও বদলায়নি, শুধু লেখা ছোট হলো। */
        val msg = "$amt already taken from $who TODAY ($todayLabel).\n\n" +
            "Add this as NEW money to today's payment?"
        AlertDialog.Builder(activity)
            .setCustomTitle(PremiumAlert.header(activity, "Already paid today"))
            .setMessage(msg)
            .setPositiveButton("Add to today's payment") { _, _ -> onProceed() }
            .setNegativeButton("No, cancel") { _, _ -> onCancel() }
            .setCancelable(false)
            .show().also { PremiumAlert.paint(it) }
    }

    /* ═══════════════════════════════════════════════════════════════════
       🔴🔒 V1106 (০৫.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — SADDAM: *"একই দিনে একই
       ধরনের পেমেন্ট দুইবার হয়ে গেছে তাও আটকালেন না কেন?"* · *"জিজ্ঞাসা করে
       নিশ্চিত করাবেন, একেবারে আটকাবেন না"*)

       উপরের B52-এর প্রশ্নটা ফোনের **জমানো** অঙ্ক দেখত, তাই অন্য ফোনে নেওয়া
       টাকা সে জানতই না (পুরো কারণ `PaymentRepository.todaysPaymentLike`-এ লেখা)।
       এখন সেভ চাপার মুহূর্তে **ক্লাউডকে** একবার জিজ্ঞাসা করা হয়।

       চারটে পথেই (Payment পর্দা · Follow-up Advance · Follow-up Nth · Chamber)
       এই একটাই ফাংশন ডাকা হয় ⇒ চার জায়গায় চার রকম আচরণ আর হতে পারে না।
       ⛔ **কিছুই আটকানো হয় না** — Cancel = কিছু হবে না · OK = জেনেশুনে তবুও।
       ⛔ হুবহু একই অঙ্ক না মিললে আচরণ **হুবহু আগের মতোই** (পুরনো B52 প্রশ্নটাই)।
       ⛔ নেট খারাপ/ব্যাকডেট হলে ক্লাউড-যাচাই বাদ, আগের পথই চলে।
       ═══════════════════════════════════════════════════════════════════ */
    fun confirmBeforeSave(
        activity: Activity,
        repo: PaymentRepository,
        patient: PatientBillInfo,
        amount: Double,
        alreadyPaid: Double,
        todayLabel: String,
        skipCloudCheck: Boolean = false,
        // 🔴🔒 V1152 (০৬.০৯.২০২৬, TK-নির্দেশ) — সতর্কবার্তা এখন **অঙ্ক + ধরন**
        // দুটোতেই মেলে (একই দিনে ১০০০ ক্যাশ ও ১০০০ অনলাইন = সতর্কতা নয়)।
        // ⛔ ফাঁকা পাঠালে আগের মতোই শুধু অঙ্ক ধরে মেলে, তাই পুরনো ডাক ভাঙে না।
        mode: String = "",
        /* 🔴🔒 V1272 (০৯.০৯.২০২৬, TK-নির্দেশ: *"ব্যাকডেট এর ফাঁকটাও বন্ধ করুন"*) —
           টাকাটা কোন দিনের জন্য বসছে। ফাঁকা = আজ (আগের আচরণ হুবহু অটুট)।
           ⛔ ব্যাকডেট হলে এখন **ওই দিনের** সারি দেখে প্রশ্ন করা হয় —
              আগে ব্যাকডেটে কোনো প্রশ্নই আসত না। */
        forDate: String = "",
        onProceed: () -> Unit,
        /* 🔴🔒 V1504 — ডিফল্ট কিছুই করে না (পুরনো ডাক অটুট)। ক্লাউড-যাচাই
           (Thread) চলাকালীন Save-বোতাম আটকে রাখার জন্য কল-করা পর্দা এই
           তালা এখানে-ওখানে (onProceed শেষে ও onCancel-এ) খুলে দেয়। */
        onCancel: () -> Unit = {}
    ) {
        if (skipCloudCheck || amount <= 0.0) {
            confirmIfAlreadyPaidToday(activity, alreadyPaid, patient.name, todayLabel, onProceed, onCancel); return
        }
        Thread {
            val dup = try { repo.todaysPaymentLike(patient, amount, mode, forDate) } catch (_: Throwable) { null }
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) { onCancel(); return@runOnUiThread }
                if (dup == null) {
                    confirmIfAlreadyPaidToday(activity, alreadyPaid, patient.name, todayLabel, onProceed, onCancel)
                    return@runOnUiThread
                }
                askSameAmount(activity, patient.name, amount, dup, onProceed, forDate, onCancel)
            }
        }.start()
    }

    /** 🔴 V1106 — "আজ হুবহু এই অঙ্কটা আগেই নেওয়া হয়েছে" প্রশ্নটা।
     *  Chamber-এর পথটা নিজের IO-থ্রেডেই যাচাই করে, তাই সে সরাসরি এটাই ডাকে —
     *  ⛔ ফলে চার জায়গায় লেখা ও আচরণ **হুবহু এক**, দুরকম হওয়ার পথ নেই। */
    fun askSameAmount(
        activity: Activity, patientName: String, amount: Double,
        dup: org.json.JSONObject, onProceed: () -> Unit,
        /* 🔴 V1272 — কোন দিনের টাকা। ফাঁকা/আজ হলে লেখা হুবহু আগের মতোই "TODAY"। */
        forDate: String = "",
        onCancel: () -> Unit = {}   // 🔴🔒 V1504 — ডিফল্ট কিছুই করে না
    ) {
        if (activity.isFinishing || activity.isDestroyed) { onCancel(); return }
        val who = patientName.ifBlank { "this patient" }
        val amt = "₹" + "%,.0f".format(amount)
        val day = DateUtil.iso(forDate).take(10)
        val isToday = day.isBlank() || day == PaymentModel.today()
        val whenTxt = if (isToday) "TODAY" else "on " + DateUtil.display(day)
        /* 🔴 V1272 — সময়টা তখনই দেখানো হয় যখন সেটা ওই দিনেরই। ব্যাকডেট করা
           সারিতে `createdAt` আসলে বসানোর দিনের ঘড়ি, তাই পুরনো তারিখের পাশে
           আজকের সময় বসলে ভুল বোঝাত (ওয়েবের `wlv1DayClock`-এর একই নিয়ম)। */
        val sameDay = dup.optString("createdAt", "").take(10).let { it.isNotBlank() && (isToday || it == day) }
        val prevTime = if (sameDay) PaymentModel.clockOf(dup.optString("createdAt", "")) else ""
        val prevMode = dup.optString("mode", "")
        val at = if (prevTime.isBlank()) "" else " at $prevTime"
        val md = if (prevMode.isBlank()) "" else " $prevMode"
        AlertDialog.Builder(activity)
            .setCustomTitle(PremiumAlert.header(activity,
                if (isToday) "⚠️ Same amount already today" else "⚠️ Same amount already on that date"))
            .setMessage(
                "$amt$md was already taken from $who $whenTxt$at.\n\n" +
                "Is this a SECOND, different payment?"
            )
            .setPositiveButton("Yes, take it again") { _, _ -> onProceed() }
            .setNegativeButton("No, cancel") { _, _ -> onCancel() }
            .setCancelable(false)
            .show().also { PremiumAlert.paint(it) }
    }
}
