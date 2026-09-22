package com.tkbiswas.pilesclinic.native

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native rebuild -- Patient Photo management.
 *
 * Stores the photo as a data-URL string in the live Supabase "patients".photo
 * field. That is the ONE place the photo lives.
 *
 * 🔴 TK-অনুমোদিত পরিবর্তন (02.08.2026, স্ক্রিনশটে দাগ দিয়ে আলোচনা করে):
 * আগে এই ফাংশন patients.photo সেভ করার পর সেই একই ছবি প্রতিটা মিলে-যাওয়া
 * followups সারিতেও কপি করে বসাত ("Mirror onto followups")। TK নিজে
 * নিশ্চিত করেছেন Follow-up-এর কোনো কার্ডেই ছবি দেখানো হয় না (২৭.০৭.২০২৬-এর
 * পর থেকে) — তাই সেই কপিটা কোথাও ব্যবহারই হতো না, শুধু ডেটাবেসের জায়গা
 * (প্রতি রোগীতে একাধিক কপি) ও Supabase-এর Egress অকারণে খরচ করত। মিরর করা
 * বন্ধ করা হলো — patients.photo-ই এখন একমাত্র সত্য উৎস (single source of
 * truth), ঠিক TK-এর "টাকার হিসাব এক জায়গায়" নিয়মের মতোই ছবির বেলাতেও।
 * ⛔ রোগীর নিজের পাতা/Payment/Report Card-এ ছবি দেখানো এক অক্ষরও বদলায়নি —
 * ওগুলো সবসময় patients.photo থেকেই সরাসরি পড়ে, followups থেকে নয়।
 */
class PatientPhotoRepository {

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    // 🆔 TK-এর নিয়ম (28.07.2026): নাম ও মোবাইলের সঙ্গে Patient ID-ও দেখাতে হবে।
    // ডিফল্ট ফাঁকা রাখা হয়েছে, তাই পুরনো কোনো ডাক ভাঙে না।
    // 🚦🔒 V1469 (১৪.০৯.২০২৬, তালিকা ৫৮১ — DURDARS PAL) — রোগীর নিজের `branch`ও
    //   এখানে যোগ হলো, ডিফল্ট ফাঁকা (পুরনো কোনো caller ভাঙে না)। কারণ:
    //   Patient Commission পর্দায় রোগীর ব্রাঞ্চ আর RMP-কার্ডের ব্রাঞ্চ মিলিয়ে
    //   দেখার জন্য এই ঘরটাই দরকার হলো (`DoctorVisitActivity.showPatientCommission`)।
    data class PatientRef(val id: String, val name: String, val mobile: String, val photo: String, val patientId: String = "", val branch: String = "")

    private fun refOf(r: org.json.JSONObject) = PatientRef(
        id = r.s("id"),
        name = r.s("name"),
        mobile = r.s("mobile"),
        photo = r.s("photo"),
        patientId = r.s("patientId"),
        branch = r.s("branch")
    )

    /**
     * 🔵🔒 V530 (২২.০৮.২০২৬, TK-নির্দেশ) — **ছবি এখন ঠিক রোগীটিরই।**
     *
     * **আগে যা হত:** এই ফাংশন নম্বর দিয়ে খুঁজে **প্রথম সারিটাই** নিত
     * (`rows.getJSONObject(0)`)। এক নম্বরে স্বামী-স্ত্রী দু'জন আলাদা রোগী থাকলে
     * দু'জনেরই কার্ডে **একই ছবি** উঠত, আর একজনের ছবি বদলালে অন্যজনেরটাও।
     *
     * **এখন:** ডাকার জায়গা যদি জানে কোন রোগী (`preferRowId` / `preferPatientCode`),
     * ঠিক সেই সারিটাই ফেরে।
     *
     * ⛔ **দুটোই ফাঁকা রাখলে আচরণ অক্ষরে অক্ষরে আগের মতোই** (প্রথম সারি) —
     *    তাই পুরোনো কোনো ডাকার জায়গা ভাঙে না, একটাও।
     * ⛔ নতুন কোনো ক্লাউড-অনুরোধ নেই — সেই একই একটাই query, শুধু limit ১ থেকে
     *    ২০ (query-র সংখ্যা বাড়েনি; একই নম্বরের সারি ক'টা, তা এমনিতেই হাতে
     *    গোনা — B30-এর পরে সাধারণত ১টাই)।
     */
    fun findByMobile(
        mobileDigits: String,
        preferRowId: String = "",
        preferPatientCode: String = ""
    ): PatientRef? {
        val rows = SupabaseClient.findByMobile(
            "patients", "+91$mobileDigits", "id,name,mobile,photo,patientId,branch", 20
        )
        if (rows.length() == 0) return null
        if (preferRowId.isNotBlank() || preferPatientCode.isNotBlank()) {
            PatientIdentity.chooseRow(rows, "", preferRowId, preferPatientCode)
                ?.let { return refOf(it) }
        }
        // ⛔ পছন্দ না বলা থাকলে — হুবহু আগের সেই প্রথম সারিটাই।
        return refOf(rows.getJSONObject(0))
    }

    /**
     * 🔵🔒 V530: **এই নম্বরে ক'জন আলাদা রোগী** — পর্দা জিজ্ঞাসা করবে কি না তা
     * ঠিক করতে। তালিকার আকার ১ হলে (রোজকার ৯৯%) পর্দা কিছুই জিজ্ঞাসা করে না।
     * ⛔ প্রজেক্টের সেই একটাই শেয়ার-করা নিয়ম (`PatientIdentity.separateIdentities`)।
     */
    fun identitiesByMobile(mobileDigits: String): List<PatientRef> {
        val rows = SupabaseClient.findByMobile(
            "patients", "+91$mobileDigits", "id,name,mobile,photo,patientId,branch,bill", 20
        )
        return PatientIdentity.separateIdentities(rows, mobileDigits).map { refOf(it) }
    }

    /** Saves (or clears, if photoData is blank) the patient photo on the
     * patients row. Returns true if the patient row updated.
     * (No longer mirrors to followups -- see class doc-comment above.) */
    fun savePhoto(patient: PatientRef, photoData: String, context: android.content.Context? = null): Boolean {
        val now = isoNow()
        val patFields = JSONObject().put("photo", photoData).put("updatedAt", now)
        val ok = SupabaseClient.updateById("patients", patient.id, patFields)
        if (!ok) {
            // TK-REQUESTED (2026-07-27): retry later instead of losing it.
            if (context != null) {
                try { GenericUpdateQueue.queue(context, "patients", patient.id, patFields) } catch (_: Throwable) { }
            }
            return false
        }
        return true
    }
}
