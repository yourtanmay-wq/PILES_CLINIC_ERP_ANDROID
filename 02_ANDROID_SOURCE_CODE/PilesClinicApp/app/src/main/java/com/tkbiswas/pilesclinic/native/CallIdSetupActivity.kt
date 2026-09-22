package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * ☎️🔒 V1427 (১৩.০৯.২০২৬, TK-রিপোর্ট + ছবি-প্রুফ পাশ) — **Call ID Banner সেটআপ**।
 *
 * TK: *"চেম্বারের নম্বরে ফোন আসে কিন্তু True Caller-এর মতন ব্যানার আসে না — শুধু
 * মাস্টারের ফোনেই আসে, কোনো স্টাফের ফোনে আসে না … গভীরে গিয়ে যাচাই করুন।"*
 *
 * **কোডে যাচাই করে পাওয়া কারণ (আন্দাজ নয়):** ব্যানার চলতে একটা ফোনে ৪-৫টা জিনিস
 * লাগে — READ_PHONE_STATE (ব্রডকাস্ট পেতে) · READ_CALL_LOG (Android 9+ এ নম্বরটা
 * পেতে) · "এই ফোনে চেম্বারের সিম আছে = হ্যাঁ" উত্তর (`hasExplicitlyConfirmedChamberSim`)
 * · Display over other apps · (Android 13+) নোটিফিকেশন অনুমতি। এগুলো এতদিন শুধু
 * Dialer / Work Notebook খুললে ছড়ানো-ছিটানোভাবে চাওয়া হতো; একটাও বাদ থাকলে
 * ব্যানার **চুপচাপ** আসত না, কেউ জানতেও পারত না কোনটা বাকি। মাস্টারের ফোনে
 * সবগুলো আছে, তাই সেখানে আসে।
 *
 * **এখন:** More মেনুতে "Call ID Banner — ON/OFF" ঘর (সব রোলে); চাপলে এই পর্দা —
 * প্রতিটা ধাপ ✔/✘ দেখায়, CONTINUE চাপলে যেটা বাকি সেটাই একে একে চায়।
 * ⛔ Home-এ কিছু বসেনি (TK: *"ডিসপ্লের উপর রেখে বিভ্রান্ত কেন করবেন"*)।
 * ⛔ ব্যানার দেখানোর নিয়ম (CallNotifyManager/BranchSimHelper) এক অক্ষরও নরম হয়নি —
 *    এই পর্দা শুধু **যা বাকি সেটা চেয়ে নেয়**।
 * ⛔ পুরো পর্দাটা কোডে আঁকা (নতুন XML নেই) — resource-ভুলের ঝুঁকি শূন্য।
 */
object CallIdSetup {
    data class Status(
        val phone: Boolean,
        val callLog: Boolean,
        val simAnswered: Boolean,
        val simYes: Boolean,
        val overlay: Boolean,
        val notif: Boolean
    ) {
        val ready: Boolean get() = phone && callLog && simYes && overlay && notif
        val simNo: Boolean get() = simAnswered && !simYes
    }

    fun status(ctx: Context): Status {
        val simYes = try { BranchSimHelper.hasExplicitlyConfirmedChamberSim(ctx) } catch (_: Throwable) { false }
        val simAnswered = simYes || try { BranchSimHelper.hasChamberAnswer(ctx) } catch (_: Throwable) { false }
        val notif = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        else true
        return Status(
            phone = BranchSimHelper.hasPhoneStatePermission(ctx),
            callLog = BranchSimHelper.hasCallLogPermission(ctx),
            simAnswered = simAnswered,
            simYes = simYes,
            overlay = CallOverlay.allowed(ctx),
            notif = notif
        )
    }

    /** More মেনুর ঘরের নিচের লেখা। */
    fun summary(ctx: Context): String {
        val st = status(ctx)
        return when {
            st.ready -> "ON — shows patient info when a call comes"
            st.simNo -> "OFF — this phone: no chamber SIM (tap to change)"
            else -> "OFF — tap to set up"
        }
    }

    fun isOn(ctx: Context): Boolean = try { status(ctx).ready } catch (_: Throwable) { false }
}

class CallIdSetupActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var btnContinue: TextView
    private var pendingAfterPermission = false

    private val requestPhonePerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            render()
            // অনুমতি না দিলে/আর জিজ্ঞাসা করা যাবে না হলে ⇒ ফোনের Settings-এর অ্যাপ-পাতা
            val st = CallIdSetup.status(this)
            if (!st.phone || !st.callLog) {
                val canAsk = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.READ_CALL_LOG
                ) || androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.READ_PHONE_STATE
                )
                if (!canAsk) openAppSettings("Allow Phone and Call logs under Permissions")
            } else if (pendingAfterPermission) {
                pendingAfterPermission = false
                runNextStep()
            }
        }

    private val requestNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EDF6FF"))
        }
        // হেডার — More-এর সবুজ গ্র্যাডিয়েন্টের মতো
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0B5C36"), Color.parseColor("#3FBE7A"))
            ).apply { cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 22 * d, 22 * d, 22 * d, 22 * d) }
        }
        header.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(Color.WHITE)
            setPadding(dp(4), 0, dp(14), 0)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Call ID Banner"; textSize = 20f; setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(header)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        body.addView(TextView(this).apply {
            text = "When a call comes on this phone, a card shows who is calling " +
                "(patient / RMP / not saved) with the last calls and last remark. " +
                "Every step below must be ✔ on THIS phone."
            textSize = 13f; setTextColor(Color.parseColor("#344054"))
            setPadding(dp(4), 0, dp(4), dp(10))
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(list)
        scroll.addView(body)
        root.addView(scroll)

        btnContinue = TextView(this).apply {
            textSize = 15f; setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = 12 * d; setColor(Color.parseColor("#0A5C33"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(12), dp(6), dp(12), dp(14)) }
            setOnClickListener { runNextStep() }
        }
        root.addView(btnContinue)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ── তালিকা আঁকা ─────────────────────────────────────────────────────
    private fun render() {
        try {
            val st = CallIdSetup.status(this)
            list.removeAllViews()
            row(st.phone, "Phone permission", if (st.phone) "Allowed" else "Needed to know when a call comes") { askPhonePerms() }
            row(st.callLog, "Call log permission", if (st.callLog) "Allowed" else "Needed to read the caller's number") { askPhonePerms() }
            row(
                st.simYes, "Chamber SIM in this phone",
                when {
                    st.simYes -> "Yes — banner allowed on this phone"
                    st.simNo -> "Answered NO — banner stays off on this phone (tap to change)"
                    else -> "Not answered yet"
                }
            ) { askChamberSim() }
            row(st.overlay, "Display over other apps", if (st.overlay) "Allowed" else "Needed to show the card on the call screen") { CallOverlay.openPermissionScreen(this) }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                row(st.notif, "Notification permission", if (st.notif) "Allowed" else "Needed for the call notification") { askNotif() }
            }
            btnContinue.text = if (st.ready) "ALL SET — BANNER IS ON" else "CONTINUE"
            btnContinue.background = GradientDrawable().apply {
                cornerRadius = 12 * resources.displayMetrics.density
                setColor(Color.parseColor(if (st.ready) "#0EA25F" else "#0A5C33"))
            }
        } catch (_: Throwable) { }
    }

    private fun row(ok: Boolean, title: String, sub: String, fix: () -> Unit) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = GradientDrawable().apply { cornerRadius = 16 * d; setColor(Color.WHITE) }
            elevation = 2 * d
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            isClickable = true
            setOnClickListener { if (!ok) fix() }
        }
        card.addView(TextView(this).apply {
            text = if (ok) "✔" else "✘"
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor(if (ok) "#0B6B3A" else "#C62828"))
            setPadding(0, 0, dp(14), 0)
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(this).apply {
            text = title; textSize = 14.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#0B2545"))
        })
        col.addView(TextView(this).apply {
            text = sub; textSize = 12f
            setTextColor(Color.parseColor(if (ok) "#5B7089" else "#C2410C"))
        })
        card.addView(col)
        if (!ok) card.addView(TextView(this).apply {
            text = "›"; textSize = 20f; setTextColor(Color.parseColor("#9AA8B7"))
        })
        list.addView(card)
    }

    // ── যেটা বাকি সেটাই একে একে ──────────────────────────────────────────
    private fun runNextStep() {
        val st = CallIdSetup.status(this)
        when {
            !st.phone || !st.callLog -> { pendingAfterPermission = true; askPhonePerms() }
            !st.simYes -> askChamberSim()
            !st.overlay -> {
                AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, "Display over other apps"))
                    .setMessage("On the next screen choose this app and turn the switch ON, then come back.")
                    .setPositiveButton("Open") { _, _ -> CallOverlay.openPermissionScreen(this) }
                    .setNegativeButton("Later", null)
                    .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
            }
            !st.notif -> askNotif()
            else -> finish()
        }
    }

    private fun askPhonePerms() {
        requestPhonePerms.launch(arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CALL_LOG
        ))
    }

    private fun askNotif() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val canAsk = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            )
            val asked = getSharedPreferences("call_id_setup", MODE_PRIVATE).getBoolean("notif_asked", false)
            if (asked && !canAsk) { openAppSettings("Allow Notifications for this app"); return }
            getSharedPreferences("call_id_setup", MODE_PRIVATE).edit().putBoolean("notif_asked", true).apply()
            requestNotif.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openAppSettings(hint: String) {
        try {
            android.widget.Toast.makeText(this, hint, android.widget.Toast.LENGTH_LONG).show()
            startActivity(Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:$packageName")
            ))
        } catch (_: Throwable) { }
    }

    /** DialerActivity-র চেম্বার-সিম প্রশ্নের **হুবহু একই** দুই ধাপ (হ্যাঁ/না → কোন SIM),
     *  একই SharedPreferences ঘরে সেভ — তাই Dialer/Work Notebook আর জিজ্ঞাসা করবে না। */
    private fun askChamberSim() {
        val st = CallIdSetup.status(this)
        if (!st.phone) { pendingAfterPermission = true; askPhonePerms(); return }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Does this phone have the chamber/branch number?"))
            .setMessage("Is the clinic's chamber/branch SIM in this phone? If this is a personal number, choose NO — the banner and Dialer call list stay off on this phone.")
            .setPositiveButton("Yes") { _, _ ->
                BranchSimHelper.saveHasChamberNumber(this, true)
                askWhichSimSlot()
            }
            .setNegativeButton("No") { _, _ ->
                BranchSimHelper.saveHasChamberNumber(this, false)   // Dialer-এর "No"-র হুবহু একই সেভ
                render()
            }
            .setNeutralButton("Cancel", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun askWhichSimSlot() {
        val slots = BranchSimHelper.activeSimSlots(this)
        if (slots.size < 2) {
            BranchSimHelper.save(this, -1)
            render(); return
        }
        val labels = slots.map { "${it.second} (SIM ${it.first + 1})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Which SIM is the branch number on this phone?"))
            .setItems(labels) { _, which ->
                BranchSimHelper.save(this, slots[which].first)
                render()
            }
            /* 🔴 V1429 (যাচাইকারীর ধরা) — Dialer-এর হুবহু নিয়ম: Back/বাইরে-চাপলে "হ্যাঁ"
               উত্তরটা মুছে যায়, নইলে দুই-সিম ফোনে স্লট না বেছেই ব্যানার ON হয়ে থাকত। */
            .setNegativeButton("Back") { _, _ -> BranchSimHelper.clearChamberAnswer(this); askChamberSim() }
            .setOnCancelListener { BranchSimHelper.clearChamberAnswer(this); render() }
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }
}
