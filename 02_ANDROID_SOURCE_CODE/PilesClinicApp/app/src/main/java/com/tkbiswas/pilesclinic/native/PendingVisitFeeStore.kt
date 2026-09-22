package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🟥🔒 V958 (০১.০৯.২০২৬, TK-নির্দেশ: *"হ্যাঁ, বন্ধ করুন"*)
 *
 * ─── কী দোষ ছিল (খাতা + কোড, দুটোতেই মিলিয়ে দেখা) ────────────────────────
 * খাতার সারি V901 (৩১.০৮.২০২৬) "Visit Fee Missing"-এর **একটা** কারণ সেরেছিল —
 * "Update Existing" চাপলে ফি-র টাকার সারি লেখা হত না। কিন্তু ওই সমাধানেই
 * একটা দরজা **ইচ্ছে করে খোলা** রাখা হয়েছিল:
 *
 *     ফি আগে নেওয়া হয়েছে কিনা যাচাই করতে ক্লাউডে একটা পড়া লাগে। নেট না
 *     থাকলে বা ওই পড়াটা ব্যর্থ হলে `visitFeeAlreadyTaken()` **"হ্যাঁ নেওয়া
 *     আছে"** ধরে নিত (দুবার কাটার চেয়ে না-লেখা নিরাপদ) ⇒ স্টাফের হাতে নেওয়া
 *     টাকাটা **কোথাও উঠত না**, আর নামটা Briefing-এ "Visit Fee Missing"-এ
 *     চলে যেত। ওয়েবেও হুবহু একই দরজা ছিল (`wlv1FeeTakenBefore=true`)।
 *
 * ─── এখন কী হয় ───────────────────────────────────────────────────────────
 * যাচাই করা **গেল না** মানে আর "বাদ" নয় — **অপেক্ষা**। ফি-র সারিটা হুবহু
 * তৈরি হয়ে এই ঘরে জমা থাকে। পরে যখনই লাইন ফেরে (অ্যাপ খুললেই এটা চলে),
 * আবার যাচাই হয় —
 *   · ফি সত্যিই নেওয়া **হয়নি**  ⇒ সারিটা তখন বসে যায় (টাকা আর হারায় না)
 *   · ফি আগে **নেওয়া আছে**      ⇒ সারিটা বাদ (দুবার কাটার পথ নেই)
 *   · এখনো যাচাই করা গেল না    ⇒ চুপচাপ অপেক্ষায় থাকে, পরের বার আবার
 *
 * 🔒 দুবার কাটা কেন অসম্ভব: সারিটা **একবারই** তৈরি হয়, নিজের আইডি নিয়ে।
 *    বারবার চেষ্টা হলেও একই আইডি-তেই লেখা হয় (create-or-update), তাই
 *    দ্বিতীয় সারি তৈরি হওয়ার কোনো পথ নেই। আর বসানোর ঠিক আগে **প্রতিবার**
 *    আবার যাচাই হয়।
 * ⛔ নতুন রেজিস্ট্রেশনের পথ এক অক্ষরও বদলায়নি — সেখানে ফি আগের মতোই
 *    সঙ্গে সঙ্গে বসে, কোনো যাচাই লাগে না।
 * ⛔ নতুন কোনো টেবিল/কলাম/SQL লাগেনি — জমা থাকে শুধু এই ফোনে।
 */
object PendingVisitFeeStore {

    private const val PREF = "piles_clinic_pending_visit_fee"
    private const val KEY = "held"
    private const val MAX_ENTRIES = 300
    private val LOCK = Any()

    /** যাচাইয়ের তিন রকম উত্তর। */
    const val FEE_NOT_TAKEN = 0
    const val FEE_TAKEN = 1
    const val FEE_UNKNOWN = 2

    /**
     * এই রোগীর ভিজিট ফি আগে নেওয়া হয়েছে কিনা — **তিন রকম** উত্তর।
     * পুরনো `visitFeeAlreadyTaken()` "জানি না"-কেও "নেওয়া আছে" বলত; এখানে
     * "জানি না" আলাদা করে বলা হয়, যাতে সারিটা বাদ না গিয়ে অপেক্ষায় থাকে।
     */
    fun visitFeeStatus(patientRowId: String, patientCode: String = ""): Int {
        if (patientRowId.isBlank() && patientCode.isBlank()) return FEE_TAKEN
        /* 🔎🔒 V973 (নিজে ধরা, গভীরে যাচাই করতে গিয়ে) — **আগে দ্বিতীয় খোঁজাটা
           অকেজো ছিল।** রোগীর সংকেত (যেমন COB-02092026-004) `payments`-এর
           `patientCode` ঘরে বসে, `patientId`-তে নয় (`buildVisitFeePaymentRow`
           দেখুন) — অথচ দুটোতেই `patientId=eq.…` দিয়ে খোঁজা হচ্ছিল, তাই সংকেত
           ধরে কখনোই কিছু পাওয়া যেত না।
           ⛔ এতে ভুল করে "টাকা নেওয়া হয়ে গেছে" হত না (তাতে টাকা হারাত),
              কিন্তু পাহারাটা অর্ধেকই কাজ করত। এখন প্রতিটা ঘর নিজের নামে খোঁজে। */
        val lookups = listOfNotNull(
            if (patientRowId.isNotBlank()) "patientId=eq.$patientRowId" else null,
            if (patientCode.isNotBlank()) "patientCode=eq.$patientCode" else null
        )
        var sawAnswer = false
        for (filter in lookups) {
            val rows = try {
                SupabaseClient.fetchListOrNull(
                    /* 🏷️🔒 V1060 (০৪.০৯.২০২৬) — নিয়ম ৭: একই দোষ এখানেও ছিল।
                       ফি খোঁজা হত শুধু `visit_fee` নামে, অথচ টাকার SQL
                       (V418) ও কম্পিউটার `visitfee` · `registration`-ও ধরে।
                       ⛔ এর ফল আরও খারাপ হত — ফি **আগেই নেওয়া থাকলেও** খুঁজে
                          পাওয়া যেত না, তাই সারিটা বসে গিয়ে **একই ফি দুবার**
                          উঠতে পারত। এখন তিনটে নামই দেখা হয়। */
                    "payments", "$filter&payType=in.(visit_fee,visitfee,registration)", 1, select = "id"
                )
            } catch (_: Throwable) { null } ?: continue
            sawAnswer = true
            if (rows.length() > 0) return FEE_TAKEN
        }
        return if (sawAnswer) FEE_NOT_TAKEN else FEE_UNKNOWN
    }

    /** যাচাই করা যায়নি — সারিটা হুবহু জমা রাখা হলো, পরে আবার দেখা হবে। */
    fun hold(context: Context, paymentRow: JSONObject) {
        val id = paymentRow.optString("id", "")
        if (id.isBlank()) return
        synchronized(LOCK) {
            val list = load(context)
            val next = JSONArray()
            for (i in 0 until list.length()) {
                val e = list.optJSONObject(i) ?: continue
                if (e.optString("id", "") == id) continue
                next.put(e)
            }
            next.put(paymentRow)
            save(context, trim(next))
        }
    }

    fun pendingCount(context: Context): Int =
        synchronized(LOCK) { load(context).length() }

    /**
     * লাইন ফিরলে চালানো হয় (BottomNav → RegistrationRepository.flushPending)।
     * প্রতিটা জমা সারির জন্য **আবার** যাচাই — তারপরেই বসে বা বাদ যায়।
     */
    fun flush(context: Context) {
        val list = synchronized(LOCK) { load(context) }
        if (list.length() == 0) return
        val stillHeld = JSONArray()
        for (i in 0 until list.length()) {
            val row = list.optJSONObject(i) ?: continue
            val rowId = row.optString("id", "")
            // ইচ্ছে করে মুছে ফেলা সারি কখনো ফিরিয়ে আনা হয় না।
            if (rowId.isNotBlank() && DeletedGuard.isDeleted("payments", rowId, context)) continue
            when (visitFeeStatus(row.optString("patientId", ""), row.optString("patientCode", ""))) {
                FEE_TAKEN -> { /* আগেই নেওয়া আছে — বাদ, দুবার কাটা হবে না */ }
                FEE_NOT_TAKEN -> {
                    if (SupabaseClient.upsert("payments", row)) {
                        try { LocalWorkflowStore(context).upsertPayment(row, "SYNCED") } catch (_: Throwable) { }
                    } else {
                        // ক্লাউডে পৌঁছয়নি — ফোনে দেখা যাক, আর জমাও থাকুক
                        try { LocalWorkflowStore(context).upsertPayment(row) } catch (_: Throwable) { }
                        stillHeld.put(row)
                    }
                }
                else -> stillHeld.put(row)   // এখনো জানা গেল না — অপেক্ষা
            }
        }
        synchronized(LOCK) { save(context, stillHeld) }
    }

    private fun trim(list: JSONArray): JSONArray {
        if (list.length() <= MAX_ENTRIES) return list
        val out = JSONArray()
        for (i in (list.length() - MAX_ENTRIES) until list.length()) out.put(list.opt(i))
        return out
    }

    private fun load(context: Context): JSONArray = try {
        JSONArray(
            context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY, "[]") ?: "[]"
        )
    } catch (_: Throwable) { JSONArray() }

    private fun save(context: Context, list: JSONArray) {
        try {
            context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY, list.toString()).commit()
        } catch (_: Throwable) { }
    }
}
