package com.tkbiswas.pilesclinic.native

import android.content.Context
import com.tkbiswas.pilesclinic.modules.ModuleAuth
import org.json.JSONArray
import org.json.JSONObject

/**
 * V1660 — Safe cross-branch RMP calling access.
 *
 * This path intentionally exposes only the RMP calling directory and the
 * atomic call+remark RPC. It never reads patients, payments, commission,
 * referral income or edit/delete data for the remote branch.
 */
object RmpCallingAccessRepository {

    data class AccessRow(val branch: String, val enabled: Boolean)
    data class Result<T>(val ok: Boolean, val value: T? = null, val message: String = "")

    private fun ensureAuth(context: Context): Boolean {
        val expected = ModuleAuth.expectedCode(context)
        if (ModuleAuth.isSignedIn && ModuleAuth.personCode != expected) ModuleAuth.signOut()
        if (!ModuleAuth.isSignedIn) {
            try { ModuleAuth.signInCurrentSession(context.applicationContext) } catch (_: Throwable) { }
        }
        return ModuleAuth.isSignedIn && (expected == null || ModuleAuth.personCode == expected)
    }

    fun allowedBranches(context: Context): Result<List<String>> {
        if (!ensureAuth(context)) return Result(false, message = "Secure sign-in not ready")
        val r = ModuleAuth.rpc("hr", "rmp_call_allowed_branches", JSONObject())
        if (!r.ok) return Result(false, message = r.message)
        return try {
            val a = JSONArray(r.body)
            val out = ArrayList<String>()
            for (i in 0 until a.length()) {
                val b = a.optJSONObject(i)?.optString("branch", "")?.trim().orEmpty()
                if (b.isNotBlank() && b !in out) out.add(b)
            }
            Result(true, out)
        } catch (_: Throwable) { Result(false, message = "Could not read calling access") }
    }

    fun adminAccessList(context: Context, personCode: String): Result<List<AccessRow>> {
        if (!ensureAuth(context)) return Result(false, message = "Secure sign-in not ready")
        val r = ModuleAuth.rpc("hr", "admin_rmp_call_access_list", JSONObject().put("p_person_code", personCode))
        if (!r.ok) return Result(false, message = r.message)
        return try {
            val a = JSONArray(r.body)
            val out = ArrayList<AccessRow>()
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val b = o.optString("branch", "").trim()
                if (b.isNotBlank()) out.add(AccessRow(b, o.optBoolean("enabled", false)))
            }
            Result(true, out)
        } catch (_: Throwable) { Result(false, message = "Could not read access") }
    }

    fun adminSetAccess(context: Context, personCode: String, branch: String, enabled: Boolean, remark: String): Result<Unit> {
        if (!ensureAuth(context)) return Result(false, message = "Secure sign-in not ready")
        val r = ModuleAuth.rpc("hr", "admin_set_rmp_call_access", JSONObject()
            .put("p_person_code", personCode)
            .put("p_branch", branch)
            .put("p_enabled", enabled)
            .put("p_remark", remark.trim()))
        if (!r.ok) return Result(false, message = r.message)
        return try {
            val body = r.body.trim()
            val o = when {
                body.startsWith("[") -> JSONArray(body).optJSONObject(0)
                body.startsWith("{") -> JSONObject(body)
                else -> null
            }
            if (o != null && !o.optBoolean("ok", false)) Result(false, message = o.optString("message", "Request rejected"))
            else Result(true, Unit, o?.optString("message", "Saved") ?: "Saved")
        } catch (_: Throwable) { Result(true, Unit, "Saved") }
    }

    fun directory(context: Context, branch: String): Result<List<DoctorVisitItem>> {
        if (!ensureAuth(context)) return Result(false, message = "Secure sign-in not ready")
        val r = ModuleAuth.rpc("hr", "rmp_call_directory", JSONObject().put("p_branch", branch))
        if (!r.ok) return Result(false, message = r.message)
        return try {
            val a = JSONArray(r.body)
            val out = ArrayList<DoctorVisitItem>(a.length())
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                out.add(DoctorVisitItem(
                    id = o.optString("id", ""),
                    name = o.optString("name", ""),
                    mobile = o.optString("mobile", ""),
                    altMobiles = o.optString("alt_mobiles", ""),
                    area = o.optString("area", ""),
                    policeStation = o.optString("police_station", ""),
                    branch = o.optString("branch", branch),
                    remarks = o.optString("remarks", ""),
                    lastCallDate = o.optString("last_call_date", ""),
                    nextCallDate = o.optString("next_call_date", ""),
                    callStatus = o.optString("call_status", ""),
                    callCount = o.optInt("call_count", 0),
                    lastCallBy = o.optString("last_call_by", ""),
                    lastCallTime = o.optString("last_call_time", ""),
                    expectedPatientDate = o.optString("expected_patient_date", ""),
                    raw = JSONObject()
                ))
            }
            Result(true, out)
        } catch (_: Throwable) { Result(false, message = "Could not read RMP list") }
    }

    fun logCall(context: Context, rmpId: String, remark: String, nextCallDate: String, expectedPatientDate: String): Result<Unit> {
        if (!ensureAuth(context)) return Result(false, message = "Secure sign-in not ready")
        val r = ModuleAuth.rpc("hr", "rmp_log_cross_branch_call", JSONObject()
            .put("p_rmp_id", rmpId)
            .put("p_note", remark.trim())
            .put("p_next_call_date", nextCallDate.trim())
            .put("p_expected_patient_date", expectedPatientDate.trim()))
        if (!r.ok) return Result(false, message = r.message)
        return try {
            val body = r.body.trim()
            val o = when {
                body.startsWith("[") -> JSONArray(body).optJSONObject(0)
                body.startsWith("{") -> JSONObject(body)
                else -> null
            }
            if (o != null && !o.optBoolean("ok", false)) Result(false, message = o.optString("message", "Save rejected"))
            else Result(true, Unit, o?.optString("message", "Doctor call updated") ?: "Doctor call updated")
        } catch (_: Throwable) { Result(true, Unit, "Doctor call updated") }
    }
}
