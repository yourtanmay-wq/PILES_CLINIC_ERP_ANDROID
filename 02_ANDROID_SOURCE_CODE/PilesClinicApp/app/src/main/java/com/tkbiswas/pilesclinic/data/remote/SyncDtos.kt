package com.tkbiswas.pilesclinic.data.remote

/**
 * These mirror the Room entities but deliberately exclude local-only fields
 * (syncStatus, lastSyncAttemptAt, syncError) since Supabase doesn't need them.
 * Gson is configured with FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES (see
 * RetrofitProvider), so e.g. `patientName` <-> Postgres column `patient_name`
 * automatically — no manual @SerializedName bookkeeping required.
 */

data class EnquiryDto(
    val id: String,
    val patientName: String,
    val phone: String,
    val address: String?,
    val enquiryDate: Long,
    val source: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

data class RegistrationDto(
    val id: String,
    val enquiryId: String?,
    val regNo: String,
    val patientName: String,
    val age: Int?,
    val gender: String?,
    val phone: String,
    val address: String?,
    val registrationDate: Long,
    val referredBy: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

data class FollowUpDto(
    val id: String,
    val patientId: String,
    val followUpDate: Long,
    val notes: String?,
    val nextVisitDate: Long?,
    val doneByRole: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

data class PaymentDto(
    val id: String,
    val patientId: String,
    val amount: Double,
    val paymentDate: Long,
    val mode: String,
    val purpose: String?,
    val receivedByRole: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)
