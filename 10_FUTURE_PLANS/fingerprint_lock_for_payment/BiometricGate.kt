package com.tkbiswas.pilesclinic.native

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * FUTURE PLAN -- NOT YET WIRED INTO THE APP (2026-07-16).
 *
 * TK approved this exact plan on 2026-07-16 but asked to hold off adding
 * it to the LIVE app until he has a laptop to test on a real device (he
 * only has a mobile right now). This file sits here, outside
 * app/src/main/..., so the current build is 100% unaffected until TK says
 * to switch it on. See 00_README_কীভাবে_চালু_করবেন.md in this same folder
 * for the exact activation steps.
 *
 * TK's approved plan, exactly:
 *  - Fingerprint is ADDED ON TOP OF the existing 3-tap unlock, not instead
 *    of it -- 3 taps first (unchanged), THEN a fingerprint check.
 *  - Only for 3 Payment-related sensitive spots (TK's choice, first phase):
 *      1. Total Bill Amount 3-tap unlock (PaymentActivity.kt)
 *      2. Advance/2nd Payment Amount 3-tap unlock (PaymentActivity.kt)
 *      3. "Marked Arrived" 3-tap Delete inside tryEditPayment()'s dialog
 *         (PaymentActivity.kt)
 *  - If the phone has no fingerprint sensor, or no fingerprint enrolled,
 *    this silently falls back to the existing 3-tap-only behavior --
 *    nothing is ever blocked just because a phone lacks the hardware.
 */
object BiometricGate {

    /** True if this device can actually show a fingerprint prompt right
     *  now (has the hardware AND at least one fingerprint enrolled).
     *  Callers must check this FIRST -- if false, skip straight to the
     *  existing 3-tap-only behavior (TK's explicit fallback instruction),
     *  do not call prompt() at all. */
    fun isAvailable(activity: FragmentActivity): Boolean {
        return try {
            val manager = BiometricManager.from(activity)
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    /** Shows the fingerprint prompt.
     *  - onSuccess() -- fingerprint matched, proceed with the sensitive action.
     *  - onFailOrCancel() -- person cancelled, or an error happened (or the
     *    prompt itself couldn't even open) -- caller should NOT proceed,
     *    same as if the 3-tap hadn't completed.
     *  A single non-matching finger (onAuthenticationFailed) does NOT call
     *  onFailOrCancel() -- the system prompt itself already lets the
     *  person try again without the caller needing to do anything. */
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailOrCancel: () -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailOrCancel()
                }
                override fun onAuthenticationFailed() {
                    // One wrong finger -- the system prompt itself stays open
                    // and lets the person try again; nothing to do here.
                }
            }
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .build()
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Throwable) {
            // If BiometricPrompt itself fails to even open (rare device
            // quirk), never leave the caller stuck -- treat it the same as
            // a cancel so the existing 3-tap-only flow can take over.
            onFailOrCancel()
        }
    }
}
