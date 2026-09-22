package com.tkbiswas.pilesclinic.native

/**
 * 🟢 B628 (11.08.2026, TK-নির্দেশ) — Referral Income এন্ট্রি এডিট/ডিলিটের
 * মাস্টার-অনুমোদন অনুরোধ। ঠিক PaymentEditRequest-এর মতোই: দিন পেরিয়ে গেলে
 * স্টাফ/ডাক্তার সরাসরি বদলাতে পারে না — একটা pending অনুরোধ তৈরি হয়
 * (`referral_edit_requests` টেবিল), মাস্টার Approve করলে তবেই আসল
 * doctor_visits.referralPayments বদলায়। মাস্টার ও একই-দিনের স্টাফ/ডাক্তার
 * সরাসরি বদলায় (কোনো অনুরোধ লাগে না)।
 */
data class ReferralEditRequest(
    val id: String,
    val docId: String,
    val entryId: String,
    val docName: String,
    val docMobile: String,
    val branch: String,
    val patient: String,
    val patientMobile: String,
    val oldAmount: Double,
    val newAmount: Double,
    val oldStatus: String,
    val newStatus: String,
    val isDelete: Boolean,
    val reason: String,
    val requestedBy: String,
    val requestedByName: String,
    val requestedAt: String,
    val status: String
)
