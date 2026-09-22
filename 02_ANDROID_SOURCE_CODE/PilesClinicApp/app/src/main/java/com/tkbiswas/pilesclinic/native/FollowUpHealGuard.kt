package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * Safety gate used ONLY by background/self-heal follow-up creation.
 *
 * A cached patient/enquiry can outlive a real delete on another device.  A
 * self-heal must therefore prove that its live source still exists before it
 * creates a new Active follow-up row.  Network uncertainty is treated as
 * "do not create"; the normal screen can try again later.
 *
 * Normal Registration, Enquiry and Payment saves do not use this gate, so no
 * approved workflow is changed and no extra request is added to ordinary
 * work.  The one small read happens only when code is about to repair a
 * supposedly missing follow-up row.
 */
object FollowUpHealGuard {
    fun liveSourceStillExists(row: JSONObject): Boolean {
        val stage = row.optString("stage", "")
        val mobile = row.optString("mobile", "")
        if (mobile.filter { it.isDigit() }.takeLast(10).length != 10) return false

        return try {
            when {
                stage.equals("Inquiry", true) -> {
                    val expectedId = row.optString("refId", "")
                    val live = SupabaseClient.findByMobileOrNull(
                        "enquiries", mobile, "id,status", 20
                    ) ?: return false
                    (0 until live.length()).any { i ->
                        val source = live.optJSONObject(i) ?: return@any false
                        val status = source.optString("status", "Active")
                        (expectedId.isBlank() || source.optString("id") == expectedId) &&
                            !status.equals("Cancelled", true) &&
                            !status.equals("Incomplete", true) &&
                            !status.equals("Rejected", true) &&
                            !status.equals("Closed", true)
                    }
                }

                stage.equals("Patient", true) || stage.equals("Treatment", true) -> {
                    val expectedRowId = row.optString("refId", "")
                    val expectedPatientId = row.optString("patientId", "")
                    val live = SupabaseClient.findByMobileOrNull(
                        "patients", mobile, "id,patientId", 20
                    ) ?: return false
                    (0 until live.length()).any { i ->
                        val source = live.optJSONObject(i) ?: return@any false
                        val idMatches = expectedRowId.isBlank() || source.optString("id") == expectedRowId
                        val patientIdMatches = expectedPatientId.isBlank() ||
                            source.optString("patientId") == expectedPatientId
                        idMatches && patientIdMatches
                    }
                }

                else -> false
            }
        } catch (_: Throwable) {
            false
        }
    }
}
