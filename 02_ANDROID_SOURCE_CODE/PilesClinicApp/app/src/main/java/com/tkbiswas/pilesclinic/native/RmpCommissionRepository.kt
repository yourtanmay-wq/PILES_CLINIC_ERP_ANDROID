package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.modules.ModuleAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** V325 authenticated gateway for the new commission tables/functions.
 * It never writes the old doctor_visits.referralPayments JSON, so old records
 * stay readable while the new workflow is tested separately. */
object RmpCommissionRepository {
    data class Default(
        val rmpId: String, val rmpName: String, val rmpMobile: String,
        val mode: RmpCommissionModel.Mode, val value: Double
    )
    data class PatientCommission(
        val id: String, val patientRowId: String, val rmpId: String,
        val rmpName: String, val mode: RmpCommissionModel.Mode, val value: Double,
        val branch: String, val setOn: String
    )

    data class RepoResult<T>(val ok: Boolean, val value: T? = null, val message: String = "")
    data class ApprovalRequest(
        val id: String, val type: String, val patientRowId: String,
        val payload: JSONObject, val reason: String, val requestedBy: String,
        val requestedAt: String
    )
    data class CommissionPayment(
        val id: String, val rmpName: String, val paidOn: String, val amount: Double, val mode: String,
        val referenceNo: String, val hiddenFromNonMaster: Boolean, val recordedBy: String
    )
    data class RmpSummary(
        val patientCount: Int, val earned: Double, val paid: Double, val previousRmpPaid: Double,
        val due: Double, val overpaid: Double
    )
    data class AdvancePayment(
        val id: String, val paidOn: String, val amount: Double, val allocated: Double,
        val legacyCovered: Double, val mode: String, val referenceNo: String,
        /**
         * 🟢🔒🔒 V661 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ — "তারিখ এবং সময়
         * লাগবে") — `recorded_at` ঘরটা টেবিলে **আগে থেকেই** ছিল (নিচের
         * `order=paid_on.desc,recorded_at.desc`-এই প্রমাণিত — শুধু sort-এর
         * জন্য পড়া হচ্ছিল, দেখানো হচ্ছিল না)। তাই কোনো নতুন SQL/কলাম লাগেনি —
         * শুধু এই একই ঘরটা এখন `select`-এও যোগ করে display-এ আনা হলো।
         */
        val recordedAt: String = ""
    ) { val available: Double get() = amount - allocated }
    data class LegacyViewAllPatient(
        val id: String, val patientCode: String, val name: String, val mobile: String,
        val referralDate: String, val bill: Double, val paid: Double, val disease: String
    )
    data class LegacyPerformanceMetric(
        val rmpId: String, val thisMonthCount: Int, val allTimeCount: Int,
        val referralPaid: Double, val mostRecentDate: String
    )

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun mode(value: String): RmpCommissionModel.Mode =
        if (value.equals("AMOUNT", true)) RmpCommissionModel.Mode.AMOUNT else RmpCommissionModel.Mode.PERCENT

    /** 🔴🔒 V426 (TK-নির্দেশ ১৭.০৮.২০২৬) — Chamber Review পর্দার জন্য **এক দিনের**
     *  RMP কমিশন, **এক ডাকে**। সার্ভারের `fin.rmp_day_commission()` হিসাবটা করে,
     *  তাই ফোন ও কম্পিউটারে সংখ্যা কখনো আলাদা হতে পারে না, আর রোগীপ্রতি আলাদা
     *  ডাক না যাওয়ায় Egress-এ চাপ পড়ে না।
     *  ⛔ শুধু পড়া — একটাও সারি লেখা/বদলানো হয় না।
     *  ⛔ ব্যর্থ হলে খালি তালিকা ফেরে; ডাকা জায়গায় তখন কমিশনের অংশটুকু দেখানো
     *     হয় না, বাকি পর্দা আগের মতোই চলে (কিছু ভাঙে না)। */
    data class DayCommissionRow(
        val patientRowId: String, val patientMobile: String, val patientCode: String,
        val patientName: String, val rmpId: String, val rmpName: String,
        val rmpMobile: String, val paidToday: Double, val commissionToday: Double,
        /** 🔵 V1083 — রোগীর মোট বিল (পপ-আপে দেখানোর জন্য)। পুরনো সার্ভারে
         *  ঘরটা না থাকলে ০ থাকে, তাতে কিছু ভাঙে না। */
        val finalBill: Double = 0.0
    )

    fun dayCommission(branch: String, date: String): RepoResult<List<DayCommissionRow>> {
        val rpc = ModuleAuth.rpc("fin", "rmp_day_commission",
            JSONObject().put("p_branch", branch).put("p_date", date))
        if (!rpc.ok) return RepoResult(false, emptyList(), rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            val out = ArrayList<DayCommissionRow>(rows.length())
            for (i in 0 until rows.length()) {
                val x = rows.optJSONObject(i) ?: continue
                out.add(DayCommissionRow(
                    x.optString("patient_row_id", ""), x.optString("patient_mobile", ""),
                    x.optString("patient_code", ""), x.optString("patient_name", ""),
                    x.optString("rmp_id", ""), x.optString("rmp_name", ""),
                    x.optString("rmp_mobile", ""),
                    x.optDouble("paid_today", 0.0), x.optDouble("commission_today", 0.0),
                    x.optDouble("final_bill", 0.0)))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, emptyList(), "Invalid RMP day commission result") }
    }

    /** 🔴🔒 V427 (TK-নির্দেশ ১৭.০৮.২০২৬: *"আলাদা লাইনে 'আজ কত দিলাম'ও রাখুন"*) —
     *  আজ কোন RMP-কে সত্যিই কত টাকা দেওয়া হয়েছে (রোগীভিত্তিক কমিশন + অ্যাডভান্স)।
     *  ⛔ এই সংখ্যাটা **মোট থেকে বাদ যায় না** — শুধু জানার জন্য। বাদ যায় কেবল
     *     V426-এর *আজকের প্রাপ্য* কমিশন (TK-এর সিদ্ধান্ত "ক")। */
    data class DayPaidRow(
        val rmpId: String, val rmpName: String,
        val commissionPaid: Double, val advancePaid: Double, val totalPaid: Double
    )

    /* 🔴🔒 V1078 (০৪.০৯.২০২৬, TK: *"হ্যাঁ করুন, তবে সাবধানে"*) — রোগীর ঘরে
       RMP-র নাম লেখা আছে অথচ কমিশন বাঁধা নেই, এমন রোগীদের **নিজে থেকে**
       ওই RMP-র বাঁধা হারে জুড়ে দেওয়া।
       🔴 যে দোষটা এতে সারে: পুরনো মেলানোর নিয়ম রোগীর `refBy` ঘরটাকে RMP-র
          নামের সঙ্গে মেলাত, কিন্তু আজকের অ্যাপে ওই ঘরে থাকে শুধু ধরন
          ("Dr. Visit") — নামটা থাকে `refDoctor`-এ। তাই নাম লিখে নম্বর না
          লিখলে রোগীটা কমিশনের হিসাব থেকে চুপচাপ বাদ পড়ত (TK-এর BULAN ROY)।
       ⛔ সব হিসাব সার্ভারেই (`fin.rmp_autolink_refdoctor`) — ফোন ও কম্পিউটারে
          এক নিয়ম, আর আগে থেকে বাঁধা কোনো কমিশন কখনো বদলায় না (শুধু নতুন জোড়া)।
       ⛔ ব্যর্থ হলে খালি তালিকা ফেরে — ডাকা পর্দা আগের মতোই চলে, কিছু ভাঙে না। */
    data class AutoLinkRow(
        val patientRowId: String, val patientCode: String,
        val patientName: String, val rmpName: String, val action: String
    )

    fun autolinkRefDoctor(branch: String, dryRun: Boolean = false): RepoResult<List<AutoLinkRow>> {
        val rpc = ModuleAuth.rpc("fin", "rmp_autolink_refdoctor",
            JSONObject().put("p_branch", branch).put("p_dry_run", dryRun))
        if (!rpc.ok) return RepoResult(false, emptyList(), rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            val out = ArrayList<AutoLinkRow>(rows.length())
            for (i in 0 until rows.length()) {
                val x = rows.optJSONObject(i) ?: continue
                out.add(AutoLinkRow(
                    x.optString("patient_row_id", ""), x.optString("patient_code", ""),
                    x.optString("patient_name", ""), x.optString("rmp_name", ""),
                    x.optString("action", "")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, emptyList(), "Invalid RMP auto-link result") }
    }

    fun dayPaid(branch: String, date: String): RepoResult<List<DayPaidRow>> {
        val rpc = ModuleAuth.rpc("fin", "rmp_day_paid",
            JSONObject().put("p_branch", branch).put("p_date", date))
        if (!rpc.ok) return RepoResult(false, emptyList(), rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            val out = ArrayList<DayPaidRow>(rows.length())
            for (i in 0 until rows.length()) {
                val x = rows.optJSONObject(i) ?: continue
                out.add(DayPaidRow(x.optString("rmp_id", ""), x.optString("rmp_name", ""),
                    x.optDouble("commission_paid", 0.0), x.optDouble("advance_paid", 0.0),
                    x.optDouble("total_paid", 0.0)))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, emptyList(), "Invalid RMP day paid result") }
    }

    fun getDefault(rmpId: String): RepoResult<Default?> {
        val got = ModuleAuth.getRowsChecked("fin", "rmp_commission_defaults",
            "select=rmp_id,rmp_name,rmp_mobile,commission_mode,commission_value&rmp_id=eq.${enc(rmpId)}&limit=1")
        if (!got.ok) return RepoResult(false, message = "Could not load RMP default")
        if (got.rows.length() == 0) return RepoResult(true, null)
        val x = got.rows.getJSONObject(0)
        return RepoResult(true, Default(x.optString("rmp_id"), x.optString("rmp_name"),
            x.optString("rmp_mobile"), mode(x.optString("commission_mode")), x.optDouble("commission_value")))
    }

    fun setDefault(rmpId: String, name: String, mobile: String,
                   commissionMode: RmpCommissionModel.Mode, value: Double): RepoResult<Unit> {
        val rpc = ModuleAuth.rpc("fin", "rmp_set_default", JSONObject()
            .put("p_rmp_id", rmpId).put("p_rmp_name", name).put("p_rmp_mobile", mobile)
            .put("p_mode", commissionMode.name).put("p_value", value))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        // V379: never report a setting as saved until the same Cloud row can
        // be read back with the selected mode/value.
        val verify = getDefault(rmpId)
        val saved = verify.value
        val landed = verify.ok && saved != null && saved.mode == commissionMode &&
            kotlin.math.abs(saved.value - value) < 0.001
        return if (landed) RepoResult(true, Unit)
        else RepoResult(false, message = "Default was not verified — please retry")
    }

    // =========================================================================
    // 🔴🔒 V470 (20.08.2026, TK-অনুমোদিত — ব্রাঞ্চ-ভিত্তিক আলাদা Default %)
    // ⛔ উপরের getDefault()/setDefault() (বৈশ্বিক Default) এক অক্ষরও
    //    বদলানো হয়নি — এই দুটো নতুন ফাংশন সম্পূর্ণ পাশাপাশি, নতুন SQL
    //    টেবিল/RPC ব্যবহার করে।
    // =========================================================================
    data class BranchDefault(val mode: RmpCommissionModel.Mode, val value: Double, val isBranchSpecific: Boolean)

    /** এই RMP-র জন্য, এই ব্রাঞ্চের নিজস্ব Default থাকলে সেটাই; না থাকলে
     *  বৈশ্বিক Default (আগের getDefault()-এর মতোই মান) — সার্ভারই ঠিক করে। */
    fun getBranchDefault(rmpId: String, branch: String): RepoResult<BranchDefault?> {
        val rpc = ModuleAuth.rpc("fin", "rmp_get_branch_default", JSONObject()
            .put("p_rmp_id", rmpId).put("p_branch", branch))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            if (rows.length() == 0) RepoResult(true, null)
            else {
                val x = rows.getJSONObject(0)
                RepoResult(true, BranchDefault(mode(x.optString("commission_mode")),
                    x.optDouble("commission_value"), x.optBoolean("is_branch_specific")))
            }
        } catch (_: Exception) { RepoResult(false, message = "Invalid RMP branch default result") }
    }

    /** শুধু এই একটা ব্রাঞ্চের জন্য Default % ওভাররাইড সেট করেন — বাকি
     *  ব্রাঞ্চ/বৈশ্বিক Default অক্ষত থাকে। */
    fun setBranchDefault(rmpId: String, name: String, mobile: String, branch: String,
                          commissionMode: RmpCommissionModel.Mode, value: Double): RepoResult<Unit> {
        val rpc = ModuleAuth.rpc("fin", "rmp_set_branch_default", JSONObject()
            .put("p_rmp_id", rmpId).put("p_rmp_name", name).put("p_rmp_mobile", mobile)
            .put("p_branch", branch).put("p_mode", commissionMode.name).put("p_value", value))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val verify = getBranchDefault(rmpId, branch)
        val saved = verify.value
        val landed = verify.ok && saved != null && saved.isBranchSpecific &&
            saved.mode == commissionMode && kotlin.math.abs(saved.value - value) < 0.001
        return if (landed) RepoResult(true, Unit)
        else RepoResult(false, message = "Branch default was not verified — please retry")
    }

    fun getPatientCommission(patientRowId: String): RepoResult<PatientCommission?> {
        val got = ModuleAuth.getRowsChecked("fin", "rmp_patient_commissions",
            "select=id,patient_row_id,rmp_id,rmp_name,commission_mode,commission_value,treatment_branch,set_on" +
                "&patient_row_id=eq.${enc(patientRowId)}&limit=1")
        if (!got.ok) return RepoResult(false, message = "Could not load patient commission")
        if (got.rows.length() == 0) return RepoResult(true, null)
        val x = got.rows.getJSONObject(0)
        return RepoResult(true, PatientCommission(x.optString("id"), x.optString("patient_row_id"),
            x.optString("rmp_id"), x.optString("rmp_name"), mode(x.optString("commission_mode")),
            x.optDouble("commission_value"), x.optString("treatment_branch"), x.optString("set_on")))
    }

    /** mode/value null means: copy and save this RMP's current Default. */
    fun setPatientCommission(patientRowId: String, rmpId: String,
                             commissionMode: RmpCommissionModel.Mode? = null,
                             value: Double? = null, setOn: String? = null): RepoResult<String> {
        val args = JSONObject().put("p_patient_row_id", patientRowId).put("p_rmp_id", rmpId)
        args.put("p_mode", commissionMode?.name ?: JSONObject.NULL)
        args.put("p_value", value ?: JSONObject.NULL)
        args.put("p_set_on", setOn ?: JSONObject.NULL)
        val rpc = ModuleAuth.rpc("fin", "rmp_set_patient_commission", args)
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val verify = getPatientCommission(patientRowId)
        val saved = verify.value
        val landed = verify.ok && saved != null && saved.rmpId == rmpId &&
            (commissionMode == null || saved.mode == commissionMode) &&
            (value == null || kotlin.math.abs(saved.value - value) < 0.001)
        return if (landed) RepoResult(true, rpc.body.trim().trim('"'))
        else RepoResult(false, message = "Patient commission was not verified — please retry")
    }

    fun summary(patientRowId: String): RepoResult<RmpCommissionModel.Summary> {
        val rpc = ModuleAuth.rpc("fin", "rmp_summary", JSONObject().put("p_patient_row_id", patientRowId))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val x = if (arr.length() > 0) arr.getJSONObject(0) else JSONObject()
            RepoResult(true, RmpCommissionModel.Summary(
                x.optDouble("final_bill"), x.optDouble("net_treatment_paid"), x.optDouble("earned"),
                x.optDouble("paid"), x.optDouble("due"), x.optDouble("overpaid")))
        } catch (_: Exception) { RepoResult(false, message = "Invalid commission summary") }
    }

    fun rmpSummary(rmpId: String): RepoResult<RmpSummary> {
        val rpc = ModuleAuth.rpc("fin", "rmp_rmp_summary", JSONObject().put("p_rmp_id", rmpId))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body); val x = if (arr.length() > 0) arr.getJSONObject(0) else JSONObject()
            RepoResult(true, RmpSummary(x.optInt("patient_count"), x.optDouble("earned"), x.optDouble("paid_to_this_rmp"),
                x.optDouble("previous_rmp_paid"), x.optDouble("due"), x.optDouble("overpaid")))
        } catch (_: Exception) { RepoResult(false, message = "Invalid RMP commission summary") }
    }


    /* ═══════════════════════════════════════════════════════════════════════
       📒🔒 V1252 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, খাতার সারি ৩৭৩) —
       TK: *"কত তারিখে কোন RMP কে কত কমিশন দেওয়া হল সেটা আমি Google Sheet-এর
       মতো দেখতে চাই"* · পরে: *"কোন রোগীর জন্য দিলাম"*।

       ⛔ **নতুন কোনো টেবিল · কলাম · SQL লাগেনি** — টাকা দেওয়ার সব সারি আগে
          থেকেই দুটো টেবিলে জমা: `fin.rmp_commission_payments` (রোগীর নামে
          দেওয়া কমিশন) ও `fin.rmp_advance_payments` (আগাম দেওয়া টাকা)।
          এতদিন শুধু **এক রোগীর** বা **এক RMP-র** সারি আলাদা করে দেখা যেত;
          তারিখ ধরে সবার একসাথে দেখার কোনো পথ ছিল না।
       ⛔ **শুধু পড়া** — একটাও সারি লেখা/বদলানো হয় না।
       ⛔ Egress-এর হিসাব (ফ্রি প্ল্যান): এক মাসে **দুটো ছোট পড়া**, আর
          রোগীর নামটা প্রথম পড়ার ভিতরেই embed হয়ে আসে (আলাদা ডাক নয়)।
          embed কোনো কারণে না চললে **নামহীন হয়ে হলেও তালিকাটা আসে** —
          পর্দা কখনো ফাঁকা যায় না।
       ⛔ গোপনীয়তা: `hidden_from_non_master` সারি Master ছাড়া কেউ দেখেন না —
          এটা ডেটাবেসের নিজের RLS নিয়মেই আটকানো (V325), অ্যাপের হাতে নয়।
       ═══════════════════════════════════════════════════════════════════ */
    /** 📒 V1309 — RMP-পেমেন্ট কোন রোগীর জন্য (হাতে adjust = allocated · পুরনো বকেয়া আগে = fifo)। */
    data class Cover(val name: String, val rowId: String, val mobile: String, val amount: Double, val kind: String)
    data class SheetRow(
        val paidOn: String, val rmpName: String, val patientName: String,
        val isAdvance: Boolean, val amount: Double, val mode: String,
        val branch: String, val referenceNo: String, val recordedBy: String,
        val recordedAt: String,
        val id: String = "", val rmpId: String = "",                 // 📒 V1309
        val patientRowId: String = "", val patientMobile: String = "",
        val legacyCovered: Double = 0.0, val covers: List<Cover> = emptyList()
    )

    /** একটা তারিখ-সীমার সব কমিশন ও আগাম টাকা, নতুন তারিখ আগে। */
    fun commissionSheet(fromIso: String, toIso: String, branch: String): RepoResult<List<SheetRow>> {
        val range = "&paid_on=gte.${enc(fromIso)}&paid_on=lte.${enc(toIso)}"
        val out = ArrayList<SheetRow>()
        var anyOk = false

        // ── ১. রোগীর নামে দেওয়া কমিশন ──────────────────────────────────
        val payCols = "id,rmp_id,rmp_name,treatment_branch,paid_on,amount,mode,reference_no,recorded_by,recorded_at"   // 📒 V1309 +rmp_id
        val payBranch = if (branch.isBlank()) "" else "&treatment_branch=eq.${enc(branch)}"
        /* 🔵 রোগীর নামটা একই পড়াতেই আসে (foreign key ধরে) — আলাদা ডাক নয়,
           তাই ফ্রি প্ল্যানে বাড়তি চাপ পড়ে না। */
        var payR = ModuleAuth.getRowsChecked("fin", "rmp_commission_payments",
            "select=$payCols,rmp_patient_commissions(patient_name,patient_row_id,patient_mobile)$range$payBranch&order=paid_on.desc,recorded_at.desc&limit=1000")   // 📒 V1309
        /* ⛔ শেষ-ভরসা: embed কোনো কারণে না চললে নাম ছাড়াই পড়া হয় —
           তালিকাটা তখনো আসে, শুধু PATIENT ঘরটা ফাঁকা থাকে। */
        if (!payR.ok) {
            payR = ModuleAuth.getRowsChecked("fin", "rmp_commission_payments",
                "select=$payCols$range$payBranch&order=paid_on.desc,recorded_at.desc&limit=1000")
        }
        if (payR.ok) {
            anyOk = true
            for (i in 0 until payR.rows.length()) {
                val x = payR.rows.optJSONObject(i) ?: continue
                val pc = x.optJSONObject("rmp_patient_commissions")
                out.add(SheetRow(
                    x.optString("paid_on", ""), x.optString("rmp_name", ""),
                    pc?.optString("patient_name", "") ?: "",
                    false, x.optDouble("amount", 0.0), x.optString("mode", ""),
                    x.optString("treatment_branch", ""), x.s("reference_no"),   // 🔴🔒 V1313 (TK-প্রশ্ন: "REFERENCE null মানে কী?") — optString() JSON-null-কে লেখা "null" বানিয়ে ফেলত; ফাঁকা ঘরে (Cash-এ রেফারেন্স লাগেই না) এখন সত্যিই ফাঁকা
                    x.optString("recorded_by", ""), x.optString("recorded_at", ""),
                    id = x.optString("id", ""), rmpId = x.optString("rmp_id", ""),
                    patientRowId = pc?.optString("patient_row_id", "") ?: "",
                    patientMobile = pc?.optString("patient_mobile", "") ?: ""))
            }
        }

        // ── ২. আগাম দেওয়া টাকা (কোনো রোগীর সঙ্গে বাঁধা নয়) ─────────────
        val advBranch = if (branch.isBlank()) "" else "&branch=eq.${enc(branch)}"
        val advR = ModuleAuth.getRowsChecked("fin", "rmp_advance_payments",
            "select=id,rmp_id,rmp_name,branch,paid_on,amount,mode,reference_no,recorded_by,recorded_at,allocated_amount,legacy_covered_amount" +
                "$range$advBranch&order=paid_on.desc,recorded_at.desc&limit=1000")   // 📒 V1309
        if (advR.ok) {
            anyOk = true
            for (i in 0 until advR.rows.length()) {
                val x = advR.rows.optJSONObject(i) ?: continue
                out.add(SheetRow(
                    x.optString("paid_on", ""), x.optString("rmp_name", ""), "",
                    true, x.optDouble("amount", 0.0), x.optString("mode", ""),
                    x.optString("branch", ""), x.s("reference_no"),   // 🔴🔒 V1313 — উপরের একই কারণ/ফিক্স
                    x.optString("recorded_by", ""), x.optString("recorded_at", ""),
                    id = x.optString("id", ""), rmpId = x.optString("rmp_id", ""),
                    legacyCovered = x.optDouble("legacy_covered_amount", 0.0)))
            }
        }

        if (!anyOk) return RepoResult(false, message = "Could not load the commission sheet")
        /* 📒🔒 V1309 (১০.০৯.২০২৬, তালিকা ৪১৮ — TK: *"পেশেন্টের ঘরে এডভান্স লেখা কেন… নাম থাকতে হবে"*):
           ① RMP-কে দেওয়া টাকা (rmp_advance_payments — "RMP Payment" বোতাম এটাতেই লেখে) কোন রোগীর
              জন্য: সার্ভারের এক ডাকে (fin.rmp_sheet_cover) — হাতে adjust করা থাকলে সেটা, বাকিটা ওই
              RMP-র রোগীদের পুরনো বকেয়া আগে (FIFO)। ② adjust করলে যে দ্বিতীয় commission-সারি তৈরি হয়
              (একই টাকা), সেটা আলাদা সারি হিসেবে আর নয় — শুধু RMP-পেমেন্টের নিচে রোগীর নাম হিসেবে ⇒
              TOTAL = সত্যিই হাতে দেওয়া টাকা, দুবার নয়। ⛔ কোনো ডাক ব্যর্থ হলে তালিকা আগের মতোই আসে
              (নাম ছাড়া) — কিছু হারায় না। */
        try {
            val advIds = out.filter { it.isAdvance && it.id.isNotBlank() }.map { it.id }
            if (advIds.isNotEmpty()) {
                val allocR = ModuleAuth.getRowsChecked("fin", "rmp_advance_allocations",
                    "select=commission_payment_id,advance_id&advance_id=in.(" + advIds.joinToString(",") { enc(it) } + ")&limit=1000")
                if (allocR.ok) {
                    val allocPayIds = HashSet<String>()
                    for (i in 0 until allocR.rows.length()) {
                        val a = allocR.rows.optJSONObject(i) ?: continue
                        val cp = a.optString("commission_payment_id", ""); if (cp.isNotBlank()) allocPayIds.add(cp)
                    }
                    if (allocPayIds.isNotEmpty()) out.removeAll { !it.isAdvance && allocPayIds.contains(it.id) }
                }
                val rpc = ModuleAuth.rpc("fin", "rmp_sheet_cover", JSONObject()
                    .put("p_from", fromIso).put("p_to", toIso)
                    .put("p_branch", if (branch.isBlank()) JSONObject.NULL else branch))
                if (rpc.ok) {
                    val arr = try { JSONArray(rpc.body) } catch (_: Throwable) { JSONArray() }
                    val byAdv = HashMap<String, MutableList<Cover>>()
                    for (i in 0 until arr.length()) {
                        val c = arr.optJSONObject(i) ?: continue
                        val aid = c.optString("advance_id", ""); if (aid.isBlank()) continue
                        byAdv.getOrPut(aid) { mutableListOf() }.add(Cover(
                            c.optString("patient_name", ""), c.optString("patient_row_id", ""),
                            c.optString("patient_mobile", ""), c.optDouble("amount", 0.0), c.optString("kind", "")))
                    }
                    for (i in out.indices) {
                        val r = out[i]
                        if (r.isAdvance) byAdv[r.id]?.let { out[i] = r.copy(covers = it) }
                    }
                }
            }
        } catch (_: Throwable) { }
        // নতুন তারিখ আগে; একই তারিখে যেটা পরে বসানো হয়েছে সেটা আগে।
        out.sortWith(compareByDescending<SheetRow> { it.paidOn }.thenByDescending { it.recordedAt })
        return RepoResult(true, out)
    }

    fun advancePayments(rmpId: String): RepoResult<List<AdvancePayment>> {
        val got = ModuleAuth.getRowsChecked("fin", "rmp_advance_payments",
            "select=id,paid_on,amount,allocated_amount,legacy_covered_amount,mode,reference_no,recorded_at&rmp_id=eq.${enc(rmpId)}&order=paid_on.desc,recorded_at.desc&limit=200")
        if (!got.ok) return RepoResult(false, message = "Could not load RMP advance payments")
        val out = mutableListOf<AdvancePayment>()
        for (i in 0 until got.rows.length()) {
            val x = got.rows.getJSONObject(i)
            out.add(AdvancePayment(x.optString("id"), x.optString("paid_on"), x.optDouble("amount"),
                x.optDouble("allocated_amount"), x.optDouble("legacy_covered_amount"), x.optString("mode"), x.optString("reference_no"),
                x.optString("recorded_at")))
        }
        return RepoResult(true, out)
    }

    fun recordAdvance(rmpId: String, amount: Double, paidOn: String,
                      paymentMode: String, referenceNo: String): RepoResult<String> {
        val rpc = ModuleAuth.rpc("fin", "rmp_record_advance", JSONObject()
            .put("p_rmp_id", rmpId).put("p_amount", amount).put("p_paid_on", paidOn)
            .put("p_mode", paymentMode.uppercase())
            .put("p_reference_no", referenceNo.ifBlank { JSONObject.NULL }))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val id = rpc.body.trim().trim('"')
        if (id.isBlank()) return RepoResult(false, message = "Cloud response was not verified — do not enter again; open History")
        val verify = ModuleAuth.getRowsChecked("fin", "rmp_advance_payments",
            "select=id,rmp_id,paid_on,amount,mode&id=eq.${enc(id)}&limit=1")
        if (!verify.ok || verify.rows.length() != 1)
            return RepoResult(false, message = "Cloud accepted the payment but verification is unavailable — do not enter again; open History")
        val row = verify.rows.getJSONObject(0)
        val landed = row.optString("id") == id && row.optString("rmp_id") == rmpId &&
            row.optString("paid_on") == paidOn && row.optString("mode").equals(paymentMode, true) &&
            kotlin.math.abs(row.optDouble("amount") - amount) < 0.001
        return if (landed) RepoResult(true, id)
        else RepoResult(false, message = "Cloud payment did not match — do not enter again; open History")
    }

    fun allocateAdvance(advanceId: String, patientRowId: String, amount: Double, allowOverDue: Boolean): RepoResult<String> {
        val rpc = ModuleAuth.rpc("fin", "rmp_allocate_advance", JSONObject()
            .put("p_advance_id", advanceId).put("p_patient_row_id", patientRowId).put("p_amount", amount)
            .put("p_allow_over_due", allowOverDue))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val paymentId = rpc.body.trim().trim('"')
        val verify = ModuleAuth.getRowsChecked("fin", "rmp_commission_payments",
            "select=id,patient_commission_id,amount&id=eq.${enc(paymentId)}&limit=1")
        val landed = verify.ok && verify.rows.length() == 1 &&
            kotlin.math.abs(verify.rows.getJSONObject(0).optDouble("amount") - amount) < 0.001
        return if (landed) RepoResult(true, paymentId)
        else RepoResult(false, message = "Adjustment was not verified — do not repeat; reopen Advance History")
    }

    /** V328: tiny authenticated replacement for the RMP-card count download.
     * Returns only RMP id -> count. It never writes data and is deliberately
     * separate from the new commission summary/matching rules. */
    fun legacyCardCounts(): RepoResult<Map<String, Int>> {
        val rpc = ModuleAuth.rpc("fin", "rmp_legacy_card_counts", JSONObject())
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            val out = HashMap<String, Int>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val id = row.optString("rmp_id", "")
                val count = row.optInt("referred_count", -1)
                if (id.isBlank() || count < 0 || out.containsKey(id))
                    return RepoResult(false, message = "Invalid RMP count summary")
                out[id] = count
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid RMP count summary") }
    }

    /** V328: selected RMP's legacy View All patient rows, with the current
     * mobile-grouped Paid and Refund rule calculated by the protected server
     * function. Read-only; caller keeps the full old download as fallback. */
    fun legacyViewAll(rmpId: String): RepoResult<List<LegacyViewAllPatient>> {
        val rpc = ModuleAuth.rpc("fin", "rmp_legacy_view_all_v2", JSONObject().put("p_rmp_id", rmpId))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            val out = ArrayList<LegacyViewAllPatient>(rows.length())
            val ids = HashSet<String>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: return RepoResult(false, message = "Invalid RMP View All result")
                val id = row.optString("patient_row_id", "")
                val bill = row.optDouble("bill", Double.NaN)
                val paid = row.optDouble("paid", Double.NaN)
                if (id.isBlank() || !ids.add(id) || !bill.isFinite() || bill < 0.0 || !paid.isFinite())
                    return RepoResult(false, message = "Invalid RMP View All result")
                out.add(LegacyViewAllPatient(id, row.optString("patient_code", ""),
                    row.optString("patient_name", ""), row.optString("patient_mobile", ""),
                    row.optString("referral_date", ""), bill, paid,
                    row.s("disease").ifBlank { row.s("diagnosis") }))   // 🔴 V819 — `optString` SQL NULL-এ আক্ষরিক "null" ফেরায় (V696/V812-এর ফাঁদ); `s()` সেটা ফাঁকা ধরে
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid RMP View All result") }
    }

    /* 🔴🔒 V411 (TK-রিপোর্ট, ছবিসহ, ১৭.০৮.২০২৬) — **RMP Due List অন্ধ ছিল।**
       TK দেখালেন: Due List বলছে "NOBODY IS PENDING · ₹0", অথচ PK-র কার্ডে
       "Ref. Due ₹41,750"। লাইভ ডেটায় যাচাই করে পাওয়া গেল — পাঁচ ব্রাঞ্চ
       মিলিয়ে আসলে **₹২,১০,৮৫০** বাকি, যার একটাকাও ওই তালিকায় উঠত না।

       **কারণ:** Due List শুধু **পুরনো** পদ্ধতি (`doctor_visits.referralPayments`)
       দেখে তালিকা বানাত; তারপর সেই তালিকার প্রথম ২৫ জনকে নতুন পদ্ধতিতে
       মিলিয়ে নিত। ফলে যে RMP-র **পুরনো** পদ্ধতিতে কিছু বাকি নেই অথচ **নতুন**
       পদ্ধতিতে (রোগীর বিল × %) টাকা পাওনা, তিনি তালিকায় **ঢুকতেই পারতেন না**।

       **সমাধান:** নতুন সার্ভার-ফাংশন `fin.rmp_branch_due(p_branch)` — **এক ডাকেই**
       ওই ব্রাঞ্চের সব RMP-র পাওনা। ৪১২ জনের জন্য ৪১২ বার ডাকতে হয় না, তাই
       Egress-এও চাপ পড়ে না। ⛔ ফাংশনটা অন্য সব RMP-ফাংশনের মতোই পাহারা-দেওয়া
       (`fin.rmp_can_use()` + ব্রাঞ্চ-পরীক্ষা) এবং **শুধু পড়ে**, কিছু বদলায় না।
       ⛔ ফল উদ্ভট/ব্যর্থ হলে পুরোটাই বাতিল — তখন আগের পুরনো হিসাবটাই দেখায়। */
    data class BranchDueRow(
        val rmpId: String, val name: String, val mobile: String, val branch: String,
        val patientCount: Int, val earned: Double, val paid: Double, val due: Double
    )

    fun branchDue(branch: String): RepoResult<List<BranchDueRow>> {
        if (branch.isBlank()) return RepoResult(false, message = "Branch required")
        val rpc = ModuleAuth.rpc("fin", "rmp_branch_due", JSONObject().put("p_branch", branch))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            val out = ArrayList<BranchDueRow>(rows.length())
            val seen = HashSet<String>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i)
                    ?: return RepoResult(false, message = "Invalid RMP due result")
                val id = r.optString("rmp_id", "")
                val due = r.optDouble("due", Double.NaN)
                val paid = r.optDouble("paid", Double.NaN)
                val earned = r.optDouble("earned", Double.NaN)
                if (id.isBlank() || !seen.add(id) || !due.isFinite() || !paid.isFinite() ||
                    !earned.isFinite() || due < 0 || paid < 0 || earned < 0)
                    return RepoResult(false, message = "Invalid RMP due result")
                out.add(
                    BranchDueRow(
                        id, r.optString("rmp_name", ""), r.optString("rmp_mobile", ""),
                        r.optString("branch", ""), r.optInt("patient_count", 0), earned, paid, due
                    )
                )
            }
            RepoResult(true, out)
        } catch (_: Exception) {
            RepoResult(false, message = "Invalid RMP due result")
        }
    }

    /** V328: Master-only Performance metrics from the protected server
     * function. It returns no patient ledger. Any malformed/failed response
     * is rejected so the caller can run the unchanged old calculation. */
    fun legacyPerformance(branch: String?): RepoResult<List<LegacyPerformanceMetric>> {
        val args = JSONObject().put("p_branch", branch?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        val rpc = ModuleAuth.rpc("fin", "rmp_legacy_performance", args)
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val rows = JSONArray(rpc.body)
            val out = ArrayList<LegacyPerformanceMetric>(rows.length())
            val ids = HashSet<String>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i)
                    ?: return RepoResult(false, message = "Invalid RMP Performance result")
                val id = row.optString("rmp_id", "")
                val month = row.optInt("this_month_count", -1)
                val all = row.optInt("all_time_count", -1)
                val paid = row.optDouble("referral_paid", Double.NaN)
                if (id.isBlank() || !ids.add(id) || month < 0 || all < 1 || month > all ||
                    all > 5000 || !paid.isFinite())
                    return RepoResult(false, message = "Invalid RMP Performance result")
                out.add(LegacyPerformanceMetric(id, month, all, paid,
                    row.optString("most_recent_date", "")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid RMP Performance result") }
    }

    fun recordPayment(patientRowId: String, amount: Double, paidOn: String,
                      paymentMode: String, referenceNo: String): RepoResult<String> {
        val rpc = ModuleAuth.rpc("fin", "rmp_record_payment", JSONObject()
            .put("p_patient_row_id", patientRowId).put("p_amount", amount).put("p_paid_on", paidOn)
            .put("p_mode", paymentMode.uppercase()).put("p_reference_no", referenceNo.ifBlank { JSONObject.NULL }))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val paymentId = rpc.body.trim().trim('"')
        if (paymentId.isBlank()) return RepoResult(false, message = "Cloud response was not verified — do not enter the payment again; open History")
        // V381: an HTTP-success alone is no longer shown as "Payment saved".
        // Read the exact new row back from Cloud and match every money field.
        val verify = ModuleAuth.getRowsChecked("fin", "rmp_commission_payments",
            "select=id,patient_commission_id,paid_on,amount,mode&id=eq.${enc(paymentId)}&limit=1")
        if (!verify.ok || verify.rows.length() != 1)
            return RepoResult(false, message = "Cloud accepted the request but verification is unavailable — do not enter it again; open History")
        val row = verify.rows.getJSONObject(0)
        val landed = row.optString("id") == paymentId && row.optString("paid_on") == paidOn &&
            row.optString("mode").equals(paymentMode, true) && kotlin.math.abs(row.optDouble("amount") - amount) < 0.001
        return if (landed) RepoResult(true, paymentId)
        else RepoResult(false, message = "Cloud payment did not match — do not enter it again; open History")
    }

    fun requestBackdatePayment(patientRowId: String, amount: Double, paidOn: String,
                               paymentMode: String, referenceNo: String, reason: String): RepoResult<String> {
        val payload = JSONObject().put("amount", amount).put("paid_on", paidOn)
            .put("mode", paymentMode.uppercase()).put("reference_no", referenceNo)
        val rpc = ModuleAuth.rpc("fin", "rmp_request_approval", JSONObject()
            .put("p_request_type", "BACKDATE_PAYMENT").put("p_patient_row_id", patientRowId)
            .put("p_payload", payload).put("p_reason", reason.ifBlank { JSONObject.NULL }))
        return if (rpc.ok) RepoResult(true, rpc.body.trim().trim('"')) else RepoResult(false, message = rpc.message)
    }

    fun pendingRequests(): RepoResult<List<ApprovalRequest>> {
        val got = ModuleAuth.getRowsChecked("fin", "rmp_commission_requests",
            "select=id,request_type,patient_row_id,payload,reason,requested_by,requested_at" +
                "&status=eq.PENDING&order=requested_at.asc&limit=200")
        if (!got.ok) return RepoResult(false, message = "Could not load commission requests")
        val out = mutableListOf<ApprovalRequest>()
        for (i in 0 until got.rows.length()) {
            val x = got.rows.getJSONObject(i)
            out.add(ApprovalRequest(x.optString("id"), x.optString("request_type"),
                x.optString("patient_row_id"), x.optJSONObject("payload") ?: JSONObject(),
                x.optString("reason"), x.optString("requested_by"), x.optString("requested_at")))
        }
        return RepoResult(true, out)
    }

    fun decideRequest(requestId: String, approve: Boolean): RepoResult<Unit> {
        val rpc = ModuleAuth.rpc("fin", "rmp_decide_request",
            JSONObject().put("p_request_id", requestId).put("p_approve", approve))
        return if (rpc.ok) RepoResult(true, Unit) else RepoResult(false, message = rpc.message)
    }

    fun paymentHistory(patientCommissionId: String): RepoResult<List<CommissionPayment>> {
        val got = ModuleAuth.getRowsChecked("fin", "rmp_commission_payments",
            "select=id,rmp_name,paid_on,amount,mode,reference_no,hidden_from_non_master,recorded_by" +
                "&patient_commission_id=eq.${enc(patientCommissionId)}&order=paid_on.desc,recorded_at.desc&limit=500")
        if (!got.ok) return RepoResult(false, message = "Could not load commission payment history")
        val out = mutableListOf<CommissionPayment>()
        for (i in 0 until got.rows.length()) {
            val x = got.rows.getJSONObject(i)
            out.add(CommissionPayment(x.optString("id"), x.optString("rmp_name"), x.optString("paid_on"), x.optDouble("amount"),
                x.optString("mode"), x.optString("reference_no"), x.optBoolean("hidden_from_non_master"),
                x.optString("recorded_by")))
        }
        return RepoResult(true, out)
    }

    fun editPayment(paymentId: String, amount: Double, paidOn: String, paymentMode: String,
                    referenceNo: String, masterPrivate: Boolean, reason: String): RepoResult<Unit> {
        val rpc = ModuleAuth.rpc("fin", "rmp_edit_payment", JSONObject()
            .put("p_payment_id", paymentId).put("p_amount", amount).put("p_paid_on", paidOn)
            .put("p_mode", paymentMode.uppercase()).put("p_reference_no", referenceNo.ifBlank { JSONObject.NULL })
            .put("p_master_private", masterPrivate).put("p_reason", reason.ifBlank { JSONObject.NULL }))
        return if (rpc.ok) RepoResult(true, Unit) else RepoResult(false, message = rpc.message)
    }

    fun deletePayment(paymentId: String, reason: String): RepoResult<Unit> {
        val rpc = ModuleAuth.rpc("fin", "rmp_delete_payment", JSONObject()
            .put("p_payment_id", paymentId)
            .put("p_reason", reason.ifBlank { JSONObject.NULL }))
        return if (rpc.ok) RepoResult(true, Unit) else RepoResult(false, message = rpc.message)
    }

    fun requestPaymentEdit(patientRowId: String, paymentId: String, amount: Double, paidOn: String,
                           paymentMode: String, referenceNo: String, reason: String): RepoResult<String> {
        val payload = JSONObject().put("payment_id", paymentId).put("amount", amount)
            .put("paid_on", paidOn).put("mode", paymentMode.uppercase()).put("reference_no", referenceNo)
        val rpc = ModuleAuth.rpc("fin", "rmp_request_approval", JSONObject()
            .put("p_request_type", "PAYMENT_EDIT").put("p_patient_row_id", patientRowId)
            .put("p_payload", payload).put("p_reason", reason.ifBlank { JSONObject.NULL }))
        return if (rpc.ok) RepoResult(true, rpc.body.trim().trim('"')) else RepoResult(false, message = rpc.message)
    }

    fun reassignPatient(patientRowId: String, newRmpId: String): RepoResult<Unit> {
        val rpc = ModuleAuth.rpc("fin", "rmp_reassign_patient",
            JSONObject().put("p_patient_row_id", patientRowId).put("p_new_rmp_id", newRmpId))
        return if (rpc.ok) RepoResult(true, Unit) else RepoResult(false, message = rpc.message)
    }

    fun requestReassignment(patientRowId: String, oldRmpId: String, newRmpId: String, reason: String): RepoResult<String> {
        val rpc = ModuleAuth.rpc("fin", "rmp_request_approval", JSONObject()
            .put("p_request_type", "RMP_REASSIGNMENT").put("p_patient_row_id", patientRowId)
            .put("p_payload", JSONObject().put("old_rmp_id", oldRmpId).put("new_rmp_id", newRmpId))
            .put("p_reason", reason.ifBlank { JSONObject.NULL }))
        return if (rpc.ok) RepoResult(true, rpc.body.trim().trim('"')) else RepoResult(false, message = rpc.message)
    }

    fun requestPastCommissionChange(patientRowId: String, rmpId: String,
                                    commissionMode: RmpCommissionModel.Mode, value: Double,
                                    originalMode: RmpCommissionModel.Mode, originalValue: Double,
                                    originalSetOn: String, reason: String): RepoResult<String> {
        val payload = JSONObject().put("rmp_id", rmpId).put("mode", commissionMode.name)
            .put("value", value).put("set_on", originalSetOn)
            .put("old_mode", originalMode.name).put("old_value", originalValue)
        val rpc = ModuleAuth.rpc("fin", "rmp_request_approval", JSONObject()
            .put("p_request_type", "PAST_COMMISSION_CHANGE").put("p_patient_row_id", patientRowId)
            .put("p_payload", payload).put("p_reason", reason.ifBlank { JSONObject.NULL }))
        return if (rpc.ok) RepoResult(true, rpc.body.trim().trim('"')) else RepoResult(false, message = rpc.message)
    }

    /* ═══════════════════════════════════════════════════════════════════════
       📒🔒 V1404 (১২.০৯.২০২৬, TK-নির্দেশ, ডেমো পাশ, খাতার সারি ৫১১) — "একটাই
       খাতা": RMP-র পর্দার Earned · Paid · Due আর রোগী-ধরে ভাঙা হিসাব সবই
       ডেটাবেসের **একই** নিয়ম (`fin.rmp_patient_breakdown`) থেকে আসে — অ্যাপে
       আলাদা করে কোনো টাকা যোগ-বিয়োগ করা হয় না, তাই দুই পর্দায় দুরকম সংখ্যা
       আসার পথ বন্ধ। Master-এর "এখানেই বন্ধ" (cap) `fin.rmp_cap_patient`।
       ═══════════════════════════════════════════════════════════════════ */
    data class PatientBreakdownRow(
        val patientRowId: String, val patientCode: String, val name: String, val mobile: String,
        val branch: String, val mode: String, val value: Double, val setOn: String,
        val netPaid: Double, val computed: Double, val legacyPaid: Double, val legacyDue: Double,
        val cappedAmount: Double?, val earned: Double, val paid: Double, val due: Double, val source: String
    )

    fun patientBreakdown(rmpId: String): RepoResult<List<PatientBreakdownRow>> {
        val rpc = ModuleAuth.rpc("fin", "rmp_patient_breakdown", JSONObject().put("p_rmp_id", rmpId))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<PatientBreakdownRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                val cap = if (x.isNull("capped_amount")) null else x.optDouble("capped_amount")
                out.add(PatientBreakdownRow(
                    x.optString("patient_row_id"), x.optString("patient_code"), x.optString("patient_name"),
                    x.optString("patient_mobile"), x.optString("treatment_branch"), x.optString("commission_mode"),
                    x.optDouble("commission_value", 0.0), x.optString("set_on"),
                    x.optDouble("net_paid", 0.0), x.optDouble("computed", 0.0), x.optDouble("legacy_paid", 0.0),
                    x.optDouble("legacy_due", 0.0), cap, x.optDouble("earned", 0.0), x.optDouble("paid", 0.0),
                    x.optDouble("due", 0.0), x.optString("source")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid RMP breakdown") }
    }

    /** V1404 — Master-only: cap == null → আবার চালু; cap ≥ 0 → এই টাকার উপরে আর কমিশন তৈরি হবে না। */
    fun capPatient(patientRowId: String, rmpId: String, cap: Double?): RepoResult<Unit> {
        val rpc = ModuleAuth.rpc("fin", "rmp_cap_patient", JSONObject()
            .put("p_patient_row_id", patientRowId).put("p_rmp_id", rmpId)
            .put("p_cap", cap ?: JSONObject.NULL))
        return if (rpc.ok) RepoResult(true, Unit) else RepoResult(false, message = rpc.message)
    }
}
