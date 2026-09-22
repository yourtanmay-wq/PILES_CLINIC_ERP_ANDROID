package com.tkbiswas.pilesclinic.native

import android.content.Context
import com.tkbiswas.pilesclinic.modules.ModuleAuth

/**
 * V325 — post-payment commission activation.
 * This is deliberately called only AFTER saveTreatmentPayment returned true.
 * It can never block, cancel, rewrite or roll back a patient payment.
 */
object RmpCommissionActivation {
    enum class State { NO_RMP, ALREADY_SET, DEFAULT_APPLIED, DEFAULT_MISSING, CHECK_FAILED }
    data class ActivationResult(val state: State, val rmpName: String = "")

    fun checkAfterTreatmentPayment(context: Context, patientRowId: String): ActivationResult {
        return try {
            if (patientRowId.isBlank()) return ActivationResult(State.CHECK_FAILED)

            // Free-plan guard: the normal case after the first payment is one
            // tiny indexed lookup only. Do not re-download Patient/RMP rows
            // after a commission snapshot already exists.
            val expected = ModuleAuth.expectedCode(context)
            if (ModuleAuth.isSignedIn && ModuleAuth.personCode != expected) ModuleAuth.signOut()
            if (!ModuleAuth.isSignedIn) {
                val err = ModuleAuth.signInCurrentSession(context.applicationContext)
                if (err != null) return ActivationResult(State.CHECK_FAILED)
            }
            val existing = RmpCommissionRepository.getPatientCommission(patientRowId)
            if (!existing.ok) return ActivationResult(State.CHECK_FAILED)
            if (existing.value != null) return ActivationResult(State.ALREADY_SET, existing.value.rmpName)

            val patients = SupabaseClient.fetchList("patients", "id=eq.${java.net.URLEncoder.encode(patientRowId, "UTF-8")}",
                1, select = "id,branch,refDoctor,refDoctorMobile")
            if (patients.length() == 0) return ActivationResult(State.CHECK_FAILED)
            val p = patients.getJSONObject(0)
            val refName = p.optString("refDoctor", "").trim()
            val refMobile = p.optString("refDoctorMobile", "").trim()
            if (refName.isBlank() && refMobile.isBlank()) return ActivationResult(State.NO_RMP)
            // 🔴🔒 V1395 — নিজের ব্রাঞ্চ দেওয়া হলো, নইলে একই মোবাইল/নাম
            // একাধিক ব্রাঞ্চে থাকলে ভুল ব্রাঞ্চের RMP বেছে নেওয়ার ঝুঁকি (দেখুন
            // DoctorVisitRepository.findReferringDoctor-এর মন্তব্য)।
            val rmp = DoctorVisitRepository().findReferringDoctor(refName, refMobile, p.optString("branch", "").trim())
                ?: return ActivationResult(State.CHECK_FAILED, refName)

            val default = RmpCommissionRepository.getDefault(rmp.optString("id"))
            if (!default.ok) return ActivationResult(State.CHECK_FAILED, refName)
            if (default.value == null) return ActivationResult(State.DEFAULT_MISSING, refName)
            val saved = RmpCommissionRepository.setPatientCommission(patientRowId, rmp.optString("id"))
            if (saved.ok) ActivationResult(State.DEFAULT_APPLIED, default.value.rmpName.ifBlank { refName })
            else ActivationResult(State.CHECK_FAILED, refName)
        } catch (_: Throwable) { ActivationResult(State.CHECK_FAILED) }
    }
}
