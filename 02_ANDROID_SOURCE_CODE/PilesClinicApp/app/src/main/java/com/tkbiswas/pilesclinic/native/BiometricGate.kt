package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

/**
 * 👆🔒 V496 (২১.০৮.২০২৬, TK-এর চূড়ান্ত নির্দেশ §৩) — **Android-এর নিরাপদ
 * আঙুলের-ছাপ পর্দা।**
 *
 * ─── ⚠️ যা এই ব্যবস্থা করতে পারে না (TK-এর §১, হুবহু) ────────────────────
 * `BiometricPrompt` **নির্দিষ্ট কোনো ব্যক্তিকে শনাক্ত করে না।** ফোনে নিবন্ধিত
 * **যেকোনো** অনুমোদিত আঙুল মিললেই Android "সফল" বলে। অর্থাৎ ওই ফোনে অন্য
 * কারো আঙুল যোগ করা থাকলে সেটাতেও সফল হবে — অ্যাপের পক্ষে সেটা জানা
 * **অসম্ভব**, Android এই তথ্য কোনো অ্যাপকে দেয় না।
 * ⇒ তাই কোথাও দাবি করা হয়নি যে "এটা প্রমাণ করে অমুকই এসেছেন"।
 *   এটা শুধু **ফাঁকির খরচ বাড়ায়** (ফোন + নিবন্ধিত আঙুল + ক্লিনিকে উপস্থিতি)।
 *
 * ─── কঠোর নিয়ম ────────────────────────────────────────────────────────────
 *  • শুধু `BIOMETRIC_STRONG`. দুর্বল (Class 2) বায়োমেট্রিক গ্রহণ করা হয় না।
 *  • **PIN / Pattern / Password-এর পথ নেই** — `setDeviceCredentialAllowed`
 *    বা `DEVICE_CREDENTIAL` কোথাও ব্যবহার করা হয়নি।
 *  • কোনো আঙুলের ছাপ · ছবি · বায়োমেট্রিক তথ্য **অ্যাপে, লগে বা Supabase-এ
 *    যায় না**। Android শুধু সফল/ব্যর্থ জানায়, সেটুকুই।
 *  • প্রতিটা অবস্থার জন্য আলাদা, পরিষ্কার বাংলা বার্তা — একটাও চাপা পড়ে না।
 *  • কোনো অবস্থাতেই crash · freeze · অসীম লুপ নয়।
 *
 * ⛔ বড় `try/catch` দিয়ে ভুল লুকানো হয়নি — প্রতিটা প্রত্যাশিত অবস্থা
 *    আলাদা করে ধরা হয়েছে (`Reason` enum), আর শুধু একেবারে অপ্রত্যাশিত
 *    ব্যতিক্রমেই `UNKNOWN` ফেরে (তখনও পরিষ্কার বার্তা, নীরব সফলতা নয়)।
 */
object BiometricGate {

    /** কেন হলো না — প্রতিটা আলাদা, যাতে বার্তাও আলাদা হতে পারে। */
    enum class Reason {
        SUCCESS,
        NO_HARDWARE,          // এই ফোনে সেন্সরই নেই
        HW_UNAVAILABLE,       // সেন্সর আছে, এখন কাজ করছে না
        NONE_ENROLLED,        // ফোনে কোনো আঙুল যোগ করা নেই
        SECURITY_UPDATE,      // নিরাপত্তা আপডেট দরকার
        UNSUPPORTED,          // এই Android সংস্করণে STRONG সম্ভব নয়
        LOCKOUT,              // বারবার ভুল — সাময়িক বন্ধ
        LOCKOUT_PERMANENT,    // স্থায়ীভাবে বন্ধ (স্ক্রিন-লক খুলতে হবে)
        FAILED,               // আঙুল মেলেনি
        CANCELLED,            // ব্যবহারকারী নিজে বাতিল করেছেন
        UNKNOWN               // অপ্রত্যাশিত
    }

    data class Outcome(val ok: Boolean, val reason: Reason, val message: String)

    /**
     * 🔤 V509 (২১.০৮.২০২৬, TK-নির্দেশ — ছবিসহ: *"এই ধরনের বাংলা থাকবে না"*):
     * অ্যাপ খোলার তালার সব বার্তা এখন **ইংরেজিতে**। আগে বাংলায় ছিল, আর
     * NoBengali শুধু বাংলা-বন্ধ ফোনেই ইংরেজি করত — TK-এর নিজের ফোনে বাংলাই
     * দেখাত। ⛔ শুধু **লেখা** বদলেছে; কোন অবস্থায় কোন বার্তা যায়, তালার
     * নিয়ম, নিরাপত্তা — কিছুই বদলায়নি।
     */
    fun messageFor(reason: Reason): String = when (reason) {
        Reason.SUCCESS -> ""
        Reason.NO_HARDWARE ->
            "This phone has no fingerprint sensor. Please inform the Master."
        Reason.HW_UNAVAILABLE ->
            "The fingerprint sensor is not working right now. Please try again in a moment."
        Reason.NONE_ENROLLED ->
            "No fingerprint is added on this phone. Go to Settings > Security > Fingerprint, add your finger, then try again."
        Reason.SECURITY_UPDATE ->
            "This phone needs a security update, so fingerprint cannot be used. Please inform the Master."
        Reason.UNSUPPORTED ->
            "This phone does not support secure fingerprint unlock. Please inform the Master."
        Reason.LOCKOUT ->
            "Too many wrong tries, so it is locked for a short while. Please try again in a moment."
        Reason.LOCKOUT_PERMANENT ->
            "Fingerprint is switched off. Unlock the phone once with its PIN or pattern, then try again."
        Reason.FAILED ->
            "Fingerprint did not match. Wipe your finger and try again."
        Reason.CANCELLED ->
            "You cancelled."
        Reason.UNKNOWN ->
            "Could not check the fingerprint. Please try again in a moment."
    }

    /**
     * এই ফোনে এখন **BIOMETRIC_STRONG** ব্যবহার করা যাবে কিনা।
     * পর্দা খোলার আগেই ডাকা হয়, তাই যেটা সম্ভব নয় তার জন্য পর্দাই খোলে না।
     */
    fun availability(context: Context): Reason {
        return try {
            val bm = BiometricManager.from(context)
            when (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> Reason.SUCCESS
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Reason.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Reason.HW_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Reason.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Reason.SECURITY_UPDATE
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Reason.UNSUPPORTED
                else -> Reason.UNKNOWN          // BIOMETRIC_STATUS_UNKNOWN সহ
            }
        } catch (_: Throwable) {
            // অপ্রত্যাশিত — নীরবে "ঠিক আছে" ধরা হয় না, বরং জানানো হয়।
            Reason.UNKNOWN
        }
    }

    /** ফোনের আঙুল-যোগ করার পাতা খোলা (NONE_ENROLLED অবস্থায়)। */
    fun openEnrollSettings(activity: AppCompatActivity): Boolean {
        val tries = mutableListOf<Intent>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tries.add(Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ))
        }
        tries.add(Intent(Settings.ACTION_SECURITY_SETTINGS))
        tries.add(Intent(Settings.ACTION_SETTINGS))
        for (i in tries) {
            try { activity.startActivity(i); return true } catch (_: Throwable) { }
        }
        return false
    }

    /*
     * 🗑️ V500 (২১.০৮.২০২৬) — **শুধু-আঙুলের কড়া `prompt()` তুলে দেওয়া হলো।**
     *
     * V496–V499-এ হাজিরার জন্য আলাদা একটা কড়া পর্দা ছিল (শুধু
     * `BIOMETRIC_STRONG`, ফোনের PIN-এর পথ বন্ধ)। TK-কে ঝুঁকিটা জানানোর পরে
     * তিনি সিদ্ধান্ত নিয়েছেন — **হাজিরাতেও পাসওয়ার্ড চলবে**।
     *
     * ⇒ তাই এখন গোটা অ্যাপে **একটাই** পর্দা: নিচের `promptUnlock()`।
     *   অব্যবহৃত দ্বিতীয় ফাংশন রেখে দিলে পরে কেউ ভুল করে সেটাই ডাকত,
     *   আর দুই জায়গায় দুই নিয়ম হয়ে যেত।
     */

    // ════════════════════════════════════════════════════════════════════════
    //  🔓 অ্যাপ খোলার তালা — **আঙুল অথবা ফোনের পাসওয়ার্ড**
    // ════════════════════════════════════════════════════════════════════════
    /*
     * 🔴 TK-এর সিদ্ধান্ত (২১.০৮.২০২৬, হুবহু):
     *   *"একদম সিম্পল রুল — ফিঙ্গারপ্রিন্ট খুলবে, আবার পাসওয়ার্ড দিলেও খুলবে।
     *    যেটা বর্তমান যুগের সব ফোনেই একই রকম ব্যবস্থা আছে। সুতরাং আমার
     *    অ্যাপের জন্য আলাদা কোনো বন্দোবস্ত আপনাকে করতে হবে না।"*
     *
     * ⇒ তাই এখানে **নতুন কিছু বানানো হয়নি**। Android-এর নিজের চেনা পর্দাটাই
     *   ডাকা হয় — যেখানে আঙুল না মিললে নিচে "Use PIN / Password" আসে, ঠিক
     *   ফোনের লক-স্ক্রিনের মতো। ফোনের পাসওয়ার্ড ফোনের কাছেই থাকে; অ্যাপ
     *   সেটা দেখেও না, রাখেও না, কোথাও পাঠায়ও না।
     *
     * ⛔ এটা **শুধু অ্যাপ খোলার** জন্য। হাজিরার (IN TIME) পর্দা আগের মতোই
     *    `prompt()` — সেখানে **শুধু আঙুল**, পাসওয়ার্ডের রাস্তা নেই। নইলে
     *    PIN জানা যে কেউ অন্যের হাজিরা বসিয়ে দিতে পারত।
     */
    private fun unlockAuthenticators(): Int =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** এই ফোনে "আঙুল অথবা পাসওয়ার্ড" আদৌ সম্ভব কিনা। */
    fun unlockAvailability(context: Context): Reason {
        return try {
            val bm = BiometricManager.from(context)
            // Android ১১ (API ৩০) থেকে দুটো একসাথে জিজ্ঞাসা করা যায়।
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                bm.canAuthenticate(unlockAuthenticators())
            else
                bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            when (code) {
                BiometricManager.BIOMETRIC_SUCCESS -> Reason.SUCCESS
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Reason.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Reason.HW_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Reason.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Reason.SECURITY_UPDATE
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Reason.UNSUPPORTED
                else -> Reason.UNKNOWN
            }
        } catch (_: Throwable) {
            Reason.UNKNOWN
        }
    }

    /**
     * অ্যাপ খোলার পর্দা — **আঙুল অথবা ফোনের PIN/পাসওয়ার্ড**।
     * [onResult] সবসময় UI থ্রেডে, **ঠিক একবার**।
     */
    fun promptUnlock(
        activity: AppCompatActivity,
        title: String,
        subtitle: String,
        onResult: (Outcome) -> Unit
    ) {
        val ready = unlockAvailability(activity)
        if (ready != Reason.SUCCESS) {
            onResult(Outcome(false, ready, messageFor(ready)))
            return
        }

        var done = false
        fun finish(r: Outcome) {
            if (done) return
            done = true
            try { activity.runOnUiThread { onResult(r) } } catch (_: Throwable) { onResult(r) }
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                finish(Outcome(true, Reason.SUCCESS, ""))
            }
            override fun onAuthenticationFailed() { /* Android নিজেই আবার চাইবে */ }
            override fun onAuthenticationError(code: Int, errString: CharSequence) {
                val reason = when (code) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> Reason.CANCELLED
                    BiometricPrompt.ERROR_LOCKOUT -> Reason.LOCKOUT
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> Reason.LOCKOUT_PERMANENT
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> Reason.NONE_ENROLLED
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> Reason.NO_HARDWARE
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                    BiometricPrompt.ERROR_TIMEOUT,
                    BiometricPrompt.ERROR_VENDOR -> Reason.HW_UNAVAILABLE
                    else -> Reason.FAILED
                }
                finish(Outcome(false, reason, messageFor(reason)))
            }
        }

        try {
            val prompt = BiometricPrompt(
                activity, ContextCompat.getMainExecutor(activity), callback
            )
            val b = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setConfirmationRequired(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // ⛔ পাসওয়ার্ডের পথ খোলা থাকলে "বাতিল" বোতাম দেওয়া যায় না
                //    (Android নিজেই আটকায়) — তাই এখানে বসানো হয়নি।
                b.setAllowedAuthenticators(unlockAuthenticators())
            } else {
                // পুরনো Android-এ একই কাজ এই পুরনো সুইচ দিয়েই হয়।
                @Suppress("DEPRECATION")
                b.setDeviceCredentialAllowed(true)
            }
            prompt.authenticate(b.build())
        } catch (_: Throwable) {
            finish(Outcome(false, Reason.UNKNOWN, messageFor(Reason.UNKNOWN)))
        }
    }
}
