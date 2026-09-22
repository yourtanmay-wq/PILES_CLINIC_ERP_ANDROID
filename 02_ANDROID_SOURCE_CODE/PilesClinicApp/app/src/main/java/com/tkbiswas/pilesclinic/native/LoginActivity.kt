package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native rebuild Step 1 -- Login.
 *
 * Authenticates against the same accounts the existing WebView login()
 * function uses (see StaffDirectory.kt), checking a live Supabase password
 * override first and falling back to the bundled role default password,
 * exactly mirroring the JS login() priority order. On success, hands off to
 * the native Dashboard. All screens are now native; the WebView has been
 * removed. The bundled assets/www now holds only the branch logo/icon
 * images used for printing -- the unused old app.js/config.js/styles.css/
 * index.html/manifest.json web-copy was deleted (B271, 02.08.2026, TK
 * approved); the live website (03_NETLIFY_READY) is the reference now.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // V367 live-photo proof: some phones re-applied the theme's navy tint
        // over the green XML drawable. Pin this one button to the approved
        // green at runtime; no other button or screen is affected.
        binding.btnLogin.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#07883F")
        )
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        // Already logged in from a previous app open -- skip straight to Dashboard.
        val existing = NativeSession.current(this)
        if (existing != null) {
            openDashboard()
            return
        }

        binding.btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            binding.etPassword.inputType = if (passwordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvForgot.setOnClickListener { showForgotPassword() }
        setupFingerprintLogin()
    }

    /* ══════════════════════════════════════════════════════════════════════
       🔒🔒 V527 (২২.০৮.২০২৬, TK-নির্দেশ) — **"পাসওয়ার্ড অথবা Fingerprint,
       তবে Fingerprint-এর আগে থাকবে।"**

       ⚠️ **একটা জরুরি সত্য:** আঙুল দিয়ে শুধু বোঝা যায় *"এটা এই ফোনের
       মালিক"* — **কে লগইন করছেন সেটা আঙুল বলতে পারে না**। তাই আঙুল কাজ
       করে কেবল তখনই, যখন **এই ফোনে আগে কেউ পাসওয়ার্ড দিয়ে লগইন করেছেন** —
       তখন সেই নম্বরটাই মনে রাখা থাকে, আর আঙুল দিলে তাঁকেই আবার ঢোকানো হয়।
       (ব্যাংকের অ্যাপ ঠিক এভাবেই কাজ করে।)

       ⛔ **কোনো পাসওয়ার্ড কখনো ফোনে জমা হয় না** — শুধু ১০ সংখ্যার নম্বর।
       ⛔ আঙুল দিয়ে ঢুকলেও **suspend/removed যাচাই হুবহু আগের মতোই** হয়
          (`finishLogin`) — তাই বাদ-দেওয়া কেউ আঙুল দিয়েও ঢুকতে পারবেন না।
       ⛔ ফোনে আঙুল/স্ক্রিন-লক না থাকলে বা আগে কেউ লগইন না করলে বোতামটাই
          দেখা যায় না — পর্দা **অবিকল আগের মতোই**, পাসওয়ার্ড দিয়েই চলবে।
       ══════════════════════════════════════════════════════════════════════ */
    private fun lastMobilePrefs() =
        getSharedPreferences("piles_clinic_login_v1", MODE_PRIVATE)

    private fun rememberLastMobile(mobile: String) {
        try { lastMobilePrefs().edit().putString("lastMobile", mobile).apply() } catch (_: Throwable) { }
    }

    private fun setupFingerprintLogin() {
        val last = try { lastMobilePrefs().getString("lastMobile", "") ?: "" } catch (_: Throwable) { "" }
        if (last.length != 10) return
        if (BiometricGate.unlockAvailability(this) != BiometricGate.Reason.SUCCESS) return
        // নম্বরটা আগে থেকে বসিয়ে রাখা — পাসওয়ার্ড দিতে চাইলেও সুবিধা হয়।
        if (binding.etMobile.text.isNullOrBlank()) binding.etMobile.setText(last)
        binding.btnFingerprint.visibility = View.VISIBLE
        binding.btnFingerprint.setOnClickListener { fingerprintLogin(last) }
        // TK: *"Fingerprint-এর আগে থাকবে"* — পর্দা খুলেই একবার নিজে থেকে চায়।
        // ⛔ বাতিল করলে কিছুই হয় না, পাসওয়ার্ডের ঘর আগের মতোই খোলা থাকে।
        binding.btnFingerprint.post { fingerprintLogin(last) }
    }

    private fun fingerprintLogin(mobile: String) {
        val account = StaffDirectory.findAccount(mobile)
        if (account == null) {
            showError("Mobile number not found")
            return
        }
        BiometricGate.promptUnlock(
            this, "Login", "Use your fingerprint, or your phone password"
        ) { res ->
            if (res.ok) { hideError(); finishLogin(account, mobile) }
            // ⛔ না মিললে কিছুই আটকায় না — পাসওয়ার্ড দিয়ে ঢোকা যাবে।
        }
    }

    private fun attemptLogin() {
        val mobileRaw = binding.etMobile.text.toString()
        val password = binding.etPassword.text.toString().trim()
        hideError()

        val mobile = StaffDirectory.normalizeMobile(mobileRaw)
        if (mobile.length != 10) {
            showError("Enter a valid 10-digit mobile number")
            return
        }
        if (password.isEmpty()) {
            showError("Password required")
            return
        }

        /* 👥🔒 V746 (২৭.০৮.২০২৬, TK-অনুমোদিত) — **অ্যাপ থেকে যোগ করা লোকজন**।
           ⚠️ **পুরনো পথে এক অক্ষরও হাত পড়েনি।** বাঁধা তালিকাটা (২৩ জন) আগের
              মতোই **প্রথমে** দেখা হয়; পেলে নিচের সব কিছু হুবহু আগের মতোই চলে —
              কোনো বাড়তি নেট-কল নেই, এক মুহূর্তও দেরি নেই।
           ⇒ মেঘ তখনই দেখা হয় যখন তালিকায় নম্বরটা **নেই** — অর্থাৎ আজ যেখানে
             লগইন এমনিতেই "Mobile number not found" বলে থেমে যেত।
           ⇒ তাই **আজকের চেয়ে খারাপ হওয়ার কোনো পথ নেই।**
           ⛔ 🧵 মেঘে যাওয়া আলাদা থ্রেডে — মূল থ্রেডে নয়, নইলে অ্যাপ থামত। */
        /* ⛔🔒 V890 (৩০.০৮.২০২৬, TK-নির্দেশ) — বাদ দেওয়া স্টাফ কোনোভাবেই
           ঢুকতে পারবেন না। তালিকা: `BlockedStaff`। এটা **সবার আগে** বসে,
           তাই কোড-তালিকা বা ক্লাউড — কোনো পথেই ফাঁক থাকে না। */
        if (BlockedStaff.isBlockedMobile(mobile)) { showError("Mobile number not found"); return }
        val builtIn = StaffDirectory.findAccount(mobile)
        if (builtIn == null) {
            setLoading(true)
            lifecycleScope.launch {
                val fromCloud = withContext(Dispatchers.IO) {
                    try { CloudStaffDirectory.findAccount(applicationContext, mobile) }
                    catch (_: Throwable) { null }
                }
                setLoading(false)
                if (fromCloud == null) {
                    showError("Mobile number not found")
                } else {
                    continueLogin(fromCloud, mobile, password)
                }
            }
            return
        }
        continueLogin(builtIn, mobile, password)
    }

    /** 🔒 V746 — পাসওয়ার্ড যাচাই থেকে পর্দা খোলা পর্যন্ত **পুরনো কোডটাই**,
     *  হুবহু, এক অক্ষরও না বদলে — শুধু আলাদা ফাংশনে সরানো হয়েছে যাতে
     *  বাঁধা-তালিকা ও মেঘ — দুই পথেই ঠিক একই যাচাই হয়। */
    private fun continueLogin(account: StaffAccount, mobile: String, password: String) {
        setLoading(true)
        lifecycleScope.launch {
            // TK-REQUESTED SECURITY FIX (2026-07-23): three clearly-separated
            // cases now (was: any null -> accept default, which let a network
            // glitch OR a set custom password both be bypassed by the old
            // default):
            //   HasCustom -> ONLY that custom password works (default rejected).
            //   NoCustom  -> the role default works (server confirmed no custom).
            //   Failed    -> could not reach the server: do NOT silently accept
            //                the default; show an error and let the user retry.
            // MASTER AUDIT 22.09.2026: V2 asks only whether the typed password matches.
            // Stored password/hash is not returned to the phone. If V2 is unavailable,
            // the proven old path below remains as compatibility fallback.
            val v2 = withContext(Dispatchers.IO) {
                CloudPasswordCheck.verifyPasswordV2(mobile, password)
            }

            val ok = when (v2) {
                CloudPasswordCheck.VerifyV2.OK -> true
                CloudPasswordCheck.VerifyV2.WRONG -> false
                CloudPasswordCheck.VerifyV2.NO_CUSTOM ->
                    password == StaffDirectory.defaultPasswordFor(account.role)
                CloudPasswordCheck.VerifyV2.ERROR -> {
                    val state = withContext(Dispatchers.IO) {
                        CloudPasswordCheck.fetchOverridePasswordState(mobile)
                    }
                    when (state) {
                        is CloudPasswordCheck.PasswordState.HasCustom -> {
                            if (PasswordHasher.isHash(state.passwordHash)) {
                                PasswordHasher.verify(password, state.passwordHash)
                            } else {
                                val plainOk = state.password.isNotBlank() && password == state.password
                                if (plainOk) {
                                    val toStore = password
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try { CloudPasswordCheck.storePasswordHash(mobile, PasswordHasher.hash(toStore)) } catch (_: Throwable) { }
                                    }
                                }
                                plainOk
                            }
                        }
                        is CloudPasswordCheck.PasswordState.NoCustom ->
                            password == StaffDirectory.defaultPasswordFor(account.role)
                        is CloudPasswordCheck.PasswordState.Failed -> {
                            showError("Network problem — could not verify password. Please check your internet and try again.")
                            return@launch
                        }
                    }
                }
            }
            setLoading(false)

            if (ok) {
                finishLogin(account, mobile)
            } else {
                showError("Wrong password")
            }
        }
    }

    /**
     * 🔒🔒 V527 (২২.০৮.২০২৬, TK-নির্দেশ) — **পাসওয়ার্ড ও আঙুল, দুটোই এই একই
     * দরজা দিয়ে ঢোকে।**
     *
     * পাসওয়ার্ড মেলার **পরে** যে যাচাইগুলো হত (suspend/removed, session সেভ),
     * সেগুলো এখানে সরিয়ে আনা হলো — এক অক্ষরও বদলায়নি। আঙুল দিয়ে ঢুকলেও
     * **হুবহু একই যাচাই** হয়, তাই সাসপেন্ড/বাদ-দেওয়া কেউ আঙুল দিয়েও ঢুকতে
     * পারবেন না।
     * ⛔ এটাই সবচেয়ে জরুরি — নইলে আঙুলের পথে একটা ফাঁক তৈরি হত।
     */
    private fun finishLogin(account: StaffAccount, mobile: String) {
        lifecycleScope.launch {
                // 🔵🔒 B618 (11.08.2026, TK-নির্দেশ): সাসপেন্ড-চেক (শুধু master ছাড়া)।
                // ⛔ fail-open — নেট/ত্রুটিতে fetchSuspendedUntil null ফেরে, তখন কখনো
                // আটকাবে না (কেউ যেন ভুলে লক না হয়); শুধু স্পষ্ট suspended_until >= আজ
                // হলে আটকাবে। master কখনো সাসপেন্ড নয়।
                if (account.role != "master") {
                    val suspUntil = withContext(Dispatchers.IO) { CloudPasswordCheck.fetchSuspendedUntil(mobile) }
                    val todayIst = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }.format(java.util.Date())
                    // 🔴 V404 (16.08.2026): বাদ-দেওয়া কর্মীর জন্য SQL এখন
                    //    2999-12-31 ফেরায় (`suspended_until_for`)। তখন
                    //    "suspended until 2999-12-31" লেখা বিভ্রান্তিকর — তাই
                    //    আলাদা, পরিষ্কার বার্তা। ⛔ সাধারণ সাসপেন্ডের বার্তা ও
                    //    আচরণ হুবহু আগের মতোই, fail-open-ও অটুট।
                    if (suspUntil != null && suspUntil >= todayIst) {
                        // 🔴🔒 V1649 (TK-নির্দেশ, "সম্পূর্ণ ডিসপ্লে জুড়ে") — আগে
                        // লগইন-পর্দার ছোট্ট লাল লেখা, মিস হয়ে যেত। এখন পূর্ণ-পর্দা,
                        // কারণসহ। এখনো লগইন হয়নি (সেশন সেভ হয়নি), তাই
                        // alreadyLoggedOut=false — শুধু "Back" চাপলেই এখানে ফেরা যায়।
                        val removed = suspUntil >= "2900-01-01"
                        val reason = try {
                            withContext(Dispatchers.IO) { CloudPasswordCheck.fetchSuspendReason(mobile) } ?: ""
                        } catch (_: Throwable) { "" }
                        SuspendedActivity.launch(
                            this@LoginActivity, account.name, suspUntil, removed, reason,
                            alreadyLoggedOut = false
                        )
                        return@launch
                    }
                }
                val user = NativeUser(account.mobile, account.name, account.branch, NativeUser.permissionRole(account.role), account.role)
                NativeSession.save(this@LoginActivity, user)
                /* 🔴🔒 V1512 (RLS-প্রস্তুতি, ১৭.০৯.২০২৬, TK-অনুমোদিত) — এতদিন
                   আসল Supabase পরিচয়-প্রমাণ (ModuleAuth.signInCurrentSession)
                   শুধু Staff Profile/বেতনের পর্দা খুললে বসত। এখন প্রতিটা
                   লগইনেই সাথে সাথে (ব্যাকগ্রাউন্ডে, নিঃশব্দে) বসানো হচ্ছে —
                   ভবিষ্যতে patients/payments-এও এই পরিচয় ব্যবহারের প্রস্তুতি।
                   ⛔ raw Thread — lifecycleScope নয়, কারণ এই মুহূর্তেই
                   openDashboard()-এ finish() ডাকা হবে, যা lifecycleScope
                   বাতিল করে দিত (DoctorLocation.captureIfPossible-এর একই
                   প্রমাণিত ধাঁচ)। ⛔ ব্যর্থ/ধীর হলেও ড্যাশবোর্ড খুলতে এক
                   মুহূর্তও দেরি হয় না, আর আজকের patients/payments-এর কাজে
                   এখনো এক অক্ষরও হাত পড়েনি — শুধু প্রস্তুতি। */
                Thread {
                    try { com.tkbiswas.pilesclinic.modules.ModuleAuth.signInCurrentSession(this@LoginActivity) } catch (_: Throwable) { }
                }.start()
                // 🔒 V527: পরের বার আঙুল দিয়ে ঢোকার জন্য কোন নম্বরটা মনে রাখতে হবে।
                //    ⛔ শুধু নম্বর — কোনো পাসওয়ার্ড কখনো ফোনে জমা হয় না।
                rememberLastMobile(mobile)
                openDashboard()
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.alpha = if (loading) 0.6f else 1f
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    /**
     * Native port of forgotPasswordFlow(): looks up the entered mobile and gives
     * the correct recovery path — Master resets via their own account, everyone
     * else is reset by the Master from the Password Center.
     */
    private fun showForgotPassword() {
        val digits = binding.etMobile.text.toString().filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            android.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Forgot Password"))
                .setMessage("Enter your registered mobile number first, then tap 'Forgot Password?'.")
                .setPositiveButton("OK", null).show().also { PremiumAlert.paint(it) }
            return
        }
        val acc = StaffDirectory.findAccount(digits)
        // 🔴 B413 (04.08.2026) — লগইন হওয়ার আগে কোনো সেশন থাকে না, তাই
        // NoBengali.active() এখানে কখনো কাজ করত না। এখন সরাসরি মোবাইল নম্বর
        // দিয়ে যাচাই করা হয় — KNE-KISHAN5 এখন এখানেও শুধু ইংরেজি দেখবেন।
        val noBengali = NoBengali.isNoBengaliMobile(digits)
        val msg = when {
            acc == null -> if (noBengali) "This mobile number is not registered. Enter the correct number."
                else "এই মোবাইল নম্বরটি রেজিস্টার্ড নয়। সঠিক নম্বর দিন।"
            acc.role == "master" -> if (noBengali) "To reset the Master admin password, log in with the default master password and set a new one from Password Center."
                else "Master অ্যাডমিন পাসওয়ার্ড রিসেটের জন্য ডিফল্ট master পাসওয়ার্ড দিয়ে লগইন করে Password Center থেকে নতুন পাসওয়ার্ড সেট করুন।"
            else -> if (noBengali) "To reset your password, ask the Master admin — they can change your (${acc.name}) password from Password Center."
                else "আপনার পাসওয়ার্ড রিসেট করতে Master অ্যাডমিনকে বলুন — তিনি Password Center থেকে আপনার (${acc.name}) পাসওয়ার্ড পাল্টে দিতে পারবেন।"
        }
        android.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Forgot Password"))
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show().also { PremiumAlert.paint(it) }
    }
}
