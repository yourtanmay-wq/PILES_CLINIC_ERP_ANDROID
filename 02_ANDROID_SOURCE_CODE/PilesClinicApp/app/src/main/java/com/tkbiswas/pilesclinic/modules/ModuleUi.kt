/* =====================================================================
   V245 — ModuleUi : tiny programmatic-UI + login-gate helpers shared by the
   three new module Activities. Programmatic views keep the new screens fully
   self-contained (no new layout XML / no view-binding), so they can never
   disturb any existing screen or the guarded design. English only.
   ===================================================================== */
package com.tkbiswas.pilesclinic.modules

import android.app.Activity
import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object ModuleUi {

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    /* 🔴🆕🔒 V438 (১৮.০৮.২০২৬) — খাতার সারি B158-এর ফাঁক: Module-এর
       **সব** Toast এই একটা ফাংশন দিয়ে যায়, অথচ এখানে NoBengali বসানো ছিল না
       ⇒ বাংলা-বন্ধ স্টাফের পর্দায় ওই বার্তাগুলো বাংলাতেই আসত। এখন এক
       জায়গাতেই ঢাকা পড়ল — বাংলা চালু থাকা সবার জন্য লেখা হুবহু আগের মতোই
       (NoBengali.s() চালু না থাকলে একই লেখা ফেরত দেয়)। */
    fun toast(ctx: Context, msg: String) =
        Toast.makeText(ctx, com.tkbiswas.pilesclinic.native.NoBengali.s(msg), Toast.LENGTH_SHORT).show()

    /** A scrollable vertical column; returns the inner LinearLayout to add to. */
    fun screen(activity: Activity, title: String): LinearLayout {
        val scroll = ScrollView(activity).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
        }
        val col = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 24))
        }
        scroll.addView(col)
        activity.setContentView(scroll)
        if (title.isNotBlank()) {
            val t = TextView(activity).apply {
                text = title
                textSize = 19f
                setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                setPadding(0, 0, 0, dp(activity, 10))
            }
            col.addView(t)
        }
        return col
    }

    fun heading(ctx: Context, text: String): TextView =
        TextView(ctx).apply {
            this.text = text; textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            setPadding(0, dp(ctx, 12), 0, dp(ctx, 4))
        }

    fun body(ctx: Context, text: String): TextView =
        TextView(ctx).apply { this.text = text; textSize = 14f; setPadding(0, dp(ctx, 2), 0, dp(ctx, 2)) }

    fun label(ctx: Context, text: String): TextView =
        TextView(ctx).apply {
            this.text = text; textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#5B6B82"))
            setPadding(0, dp(ctx, 8), 0, dp(ctx, 2))
        }

    fun input(ctx: Context, hint: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText =
        EditText(ctx).apply {
            this.hint = hint; inputType = type; textSize = 15f
            // ⌨️🔒 V756 — ফোনের নিজের সাজেশন এখানেই বন্ধ। এই এক লাইনেই
            //    **সব Module পর্দার** ঘর ঢেকে যায় (ওগুলো পর্দা খোলার পরে
            //    বানানো হয় বলে `NoAutofill.scrub()` পৌঁছাত না)।
            try { com.tkbiswas.pilesclinic.native.NoAutofill.harden(this) } catch (_: Throwable) { }
        }

    // 🔴 B409 (04.08.2026, TK-নির্দেশ — অনেক স্টাফের ফোনে সংখ্যা-ঘরে চাপ
    // দিলে কীবোর্ড আসত না, "Notes" (TYPE_CLASS_TEXT) জাতীয় ঘরে সমস্যা ছিল
    // না): কিছু ফোনের কাস্টম কীবোর্ড একা-`TYPE_CLASS_NUMBER`-এ tap করলে
    // numeric keypad খুলতে ব্যর্থ হয় (পরিচিত OEM-keyboard সীমাবদ্ধতা)।
    // এখন `TYPE_CLASS_TEXT` + `DigitsKeyListener` (শুধু সংখ্যা/দশমিক টাইপ
    // করা যায়, বাকি সব ফোনেই নির্ভরযোগ্যভাবে কীবোর্ড খোলে) + click-এ জোর
    // করে ফোকাস/কীবোর্ড — এই একটাই শেয়ার্ড হেল্পার, তিনটে মডিউলেই
    // (WorkNotebook/StaffProfile/Income-Expense) পুনর্ব্যবহার হবে যাতে
    // ভবিষ্যতে নতুন কোনো সংখ্যা-ঘরে এই একই বাগ আর ফিরে না আসে।
    fun numberInput(ctx: Context, hint: String, allowDecimal: Boolean = false): EditText {
        val f = input(ctx, hint, InputType.TYPE_CLASS_TEXT)
        f.keyListener = android.text.method.DigitsKeyListener.getInstance(if (allowDecimal) "0123456789." else "0123456789")
        f.isFocusableInTouchMode = true
        f.setOnClickListener {
            f.requestFocus()
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(f, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        return f
    }

    // 🔴 B342 (03.08.2026, TK-নির্দেশ — Work Notebook একটাই ফর্ম, AUTO ঘরগুলো
    // স্টাফ বদলাতে পারবে না): শুধু দেখানোর জন্য (non-editable) হালকা ধূসর
    // বাক্স — যে সংখ্যাগুলো অ্যাপ নিজে গণনা করে (New Enquiry/Registration/
    // App Calls/Total call), সেগুলোতেই শুধু ব্যবহার হবে। এটা EditText না —
    // TextView, তাই কখনো টাইপ/এডিট করা যায় না।
    fun autoValue(ctx: Context, value: String): TextView =
        TextView(ctx).apply {
            text = value; textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#6E7A75"))
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 8).toFloat()
                setColor(android.graphics.Color.parseColor("#F0F2F4"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(ctx, 4); bottomMargin = dp(ctx, 6) }
        }

    /* 🔴🔒 V418 (TK-রিপোর্ট, ১৭.০৮.২০২৬ — *"এত ডুপ্লিকেট কেন হবে"*):
       ১৩/০৩/২০২৬-এর একই এন্ট্রি (৯,৫০০ / ৫,০০০) **তিনবার** বসে গিয়েছিল —
       ডেটাবেসের সময় বলছে তিনটেই **০৮:২২:৫১ থেকে ০৮:২২:৫২**, অর্থাৎ এক সেকেন্ডের
       মধ্যে। "Save"-এ পরপর কয়েকবার চাপ পড়েছিল।
       ডুপ্লিকেট-সতর্কতা আগে থেকেই ছিল, কিন্তু ওটা আগে ক্লাউডে জিজ্ঞাসা করে —
       তিনটে চাপই উত্তর আসার **আগেই** বেরিয়ে গিয়েছিল, তাই তিনটেই "নতুন" মনে হয়েছিল।
       এখন **প্রথম চাপের পর ১ সেকেন্ড** কোনো বোতাম দ্বিতীয় চাপ নেয় না।
       ⛔ এক জায়গায় বসানো ⇒ সব মডিউল-পর্দার সব বোতামেই খাটে।
       ⛔ ইচ্ছে করে আবার চাপলে (১ সেকেন্ড পরে) আগের মতোই কাজ করে — কিছুই আটকায় না। */
    private const val TAP_GAP_MS = 1000L

    fun button(ctx: Context, text: String, onClick: () -> Unit): Button =
        Button(ctx).apply {
            this.text = text
            setTextColor(android.graphics.Color.WHITE)
            isAllCaps = false
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 10).toFloat()
                setColor(android.graphics.Color.parseColor("#0B8A3E"))
            }
            /* পাহারাটা **প্রতিটা বোতামের নিজস্ব** — একই বোতামে পরপর চাপ আটকায়,
               কিন্তু পাশের অন্য বোতাম (যেমন Back) সঙ্গে সঙ্গেই কাজ করে। */
            var lastTapAt = 0L
            setOnClickListener {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastTapAt < TAP_GAP_MS) return@setOnClickListener
                lastTapAt = now
                onClick()
            }
        }

    /**
     * 🟢🔒 V590 (২৩.০৮.২০২৬, TK-নির্দেশ) — *"ইন টাইম যদি একবার পাঠিয়ে দিয়ে
     * থাকে, তাহলে ওটা একটু উজ্জ্বলতা কম থাকবে।"*
     *
     * "আবার পাঠান" ধরনের **দ্বিতীয় সারির** বোতাম — কাজ হুবহু উপরের
     * `button()`-এর মতোই (একই পরপর-চাপ পাহারা, একই মাপ), শুধু রংটা হালকা।
     * তাই দিনের **আসল কাজ** (OUT TIME) চোখে আগে পড়ে, আর এই বোতামটা পাশে
     * চুপচাপ থাকে।
     *
     * ⛔ কী কাজ করে, কখন দেখা যায় — কিছুই বদলায় না, শুধু রং।
     */
    fun buttonSoft(ctx: Context, text: String, onClick: () -> Unit): Button =
        button(ctx, text, onClick).apply {
            setTextColor(android.graphics.Color.parseColor("#2F6B45"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 10).toFloat()
                setColor(android.graphics.Color.parseColor("#E7F4EC"))
                setStroke(dp(ctx, 1), android.graphics.Color.parseColor("#BFE0CC"))
            }
        }

    fun card(ctx: Context): LinearLayout {
        val l = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 10).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(ctx, 1), android.graphics.Color.parseColor("#CFE9D8"))
            }
        }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(ctx, 8) }
        l.layoutParams = lp
        return l
    }

    /** V247: reuse the existing app login; never show a second password dialog.
     *  🔴 B317 (03.08.2026, TK-নির্দেশ): এখন প্রতিবার আগে যাচাই করে দেখা হয় যে
     *  cached Module-সেশন সত্যিই *এখনকার* main-app ব্যবহারকারীরই কিনা —
     *  একজনের পর অন্য একজন লগইন করলে আগের ব্যক্তির Module-অ্যাক্সেস যেন
     *  পরের ব্যক্তি কখনো না পান। না মিললে পুরনো সেশন সাইন-আউট করে চুপচাপ
     *  বর্তমান ব্যবহারকারী হিসেবেই আবার সাইন-ইন হয় — কোনো দ্বিতীয় পাসওয়ার্ড
     *  স্ক্রিন দেখানো হয় না। */
    fun ensureSignedIn(activity: Activity, prefillCode: String, onReady: () -> Unit) {
        /* 🔎🔒 V932 — কোন ধাপে আটকাচ্ছে সেটা পর্দাতেই দেখা যাবে (OpenTrace দেখুন)।
           ⛔ শুধু একটা ছোট লেখা — কোনো নিয়ম · হিসাব · ডাক কিছুই বদলায়নি। */
        val stuckBefore = OpenTrace.lastStuckStep(activity)
        OpenTrace.step(activity, "1. checking login")
        val expected = ModuleAuth.expectedCode(activity)
        OpenTrace.step(activity, "2. login checked")
        if (ModuleAuth.isSignedIn && ModuleAuth.personCode == expected) {
            OpenTrace.done(activity); onReady(); return
        }
        if (ModuleAuth.isSignedIn) { OpenTrace.step(activity, "3. closing old session"); ModuleAuth.signOut() }
        OpenTrace.step(activity, "4. drawing screen")
        toast(activity, "Opening...")
        /* 🔴🔒 V803 (২৮.০৮.২০২৬) — TK: "Staff Profile তো খুলছেই না?" (ফটো: সাদা
           ফাঁকা পর্দা)। আসল দোষ ছিল timeout না থাকা (ModuleAuth.kt দেখুন), সেটা
           সারানো হয়েছে। কিন্তু এখানে **দ্বিতীয় দোষটাও** ছিল: উত্তর আসার আগে
           পর্দায় **কিচ্ছু আঁকা হত না** — তাই মানুষ শুধু সাদা কাগজ দেখত, বুঝতেই
           পারত না কিছু চলছে কিনা। এখন একটা স্পষ্ট "Opening…" পর্দা বসে।
           ⛔ সফল হলে নিচের `onReady()` আগের মতোই পুরো পর্দা এঁকে দেয় (এই
              অস্থায়ী লেখাটা তখন নিজে থেকেই চাপা পড়ে যায়) — কোনো আচরণ বদলায়নি। */
        /* 🕑🔒 V902 (৩১.০৮.২০২৬, TK-নির্দেশ — *"হ্যাঁ করুন, কত সেকেন্ড লাগল
           দেখান"*): TK-এর ফোনে এই পর্দাটা প্রায় ১ মিনিট আটকে ছিল, অথচ
           চেম্বারে দ্রুত নেট। কারণ ধরতে হলে আগে **সময়টা চোখে দেখা** দরকার —
           তাই এখন পর্দাতেই সেকেন্ড গোনা হয়, আর খোলার পরে কত লাগল সেটা
           একটা ছোট বার্তায় দেখায়।
           ⛔ শুধু দেখানো — লগইনের নিয়ম · সময়সীমা · কোনো ডাক কিছুই বদলায়নি। */
        val t0 = android.os.SystemClock.elapsedRealtime()
        fun secsSoFar(): String = "%.1f".format((android.os.SystemClock.elapsedRealtime() - t0) / 1000.0)
        val tick = android.os.Handler(android.os.Looper.getMainLooper())
        /* 🔴 V1006 — অস্থায়ী লেখাটার হাতল বাইরেও দরকার: পর্দা সত্যিই আঁকা
           হয়ে গেছে কিনা এটা দিয়েই বোঝা যায় (তখন এটা আর পর্দায় থাকে না)। */
        var waitView: android.widget.TextView? = null
        try {
            /* 🔎🔒 V932 — গতবার যদি খোলা শেষ না হয়ে থাকে, উপরে সেই ধাপটা
               দেখানো হয় — TK যেন ছবি তুলে পাঠাতে পারেন। */
            val prevLine = if (stuckBefore.isNotBlank()) "Last time it stopped at:  " + stuckBefore + "\n\n" else ""
            val wait = android.widget.TextView(activity).apply {
                text = prevLine + "Opening…\n\nPlease wait a moment."
                textSize = 15f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#33404F"))
                setPadding(48, 220, 48, 48)
            }
            activity.setContentView(wait)
            waitView = wait
            val ticker = object : Runnable {
                override fun run() {
                    try {
                        if (activity.isFinishing) return
                        /* 🔴 V1006 — আসল পর্দা এঁকে গেলে গোনা নিজে থেকেই থামে। */
                        if (!wait.isAttachedToWindow) {
                            try { OpenTrace.onStep = null } catch (_: Throwable) { }
                            return
                        }
                        /* ⛔ ধাপের নামটাও দেখানো হয় — জমে গেলে **শেষ ধাপটাই
                           পর্দায় থেকে যাবে**, তখন ঠিক জায়গাটা ধরা যাবে। */
                        wait.text = prevLine + "Opening…\n\nPlease wait a moment.\n\n" +
                            secsSoFar() + " s\n\nStep:  " + OpenTrace.current()
                        tick.postDelayed(this, 500)
                    } catch (_: Throwable) { }
                }
            }
            tick.postDelayed(ticker, 500)
            /* ⛔ ধাপ বদলালে সঙ্গে সঙ্গেও লেখাটা বদলায় (৫০০ ms-এর অপেক্ষা নয়)। */
            OpenTrace.onStep = { st ->
                try {
                    activity.runOnUiThread {
                        try {
                            if (!activity.isFinishing) wait.text = prevLine +
                                "Opening…\n\nPlease wait a moment.\n\n" + secsSoFar() + " s\n\nStep:  " + st
                        } catch (_: Throwable) { }
                    }
                } catch (_: Throwable) { }
            }
        } catch (_: Throwable) { }
        OpenTrace.step(activity, "5. signing in")
        Thread {
            val err = ModuleAuth.signInCurrentSession(activity.applicationContext)
            activity.runOnUiThread {
                if (err == null) {
                    /* 🔴🔒 V1006 (০৩.০৯.২০২৬, TK-নির্দেশ — খাতার সারি ৭ শেষ করা)
                       **আসল কারণ (কোড ধরে, আন্দাজ নয়):** লগইন সফল হওয়ার সাথে
                       সাথেই এখানে সেকেন্ড-গোনা **থামিয়ে** দেওয়া হত এবং ধাপের
                       লেখাও বন্ধ করে দেওয়া হত। তারপর `onReady()` যে কাজটা শুরু
                       করে (নোটবুকে: জমে-থাকা মার্ক ক্লাউডে বসানো, তারপর আজকের
                       দিন আনা) সেটা শেষ না হওয়া পর্যন্ত পর্দায় **পুরনো
                       "Opening…" লেখাটাই** দাঁড়িয়ে থাকত — সেকেন্ড বাড়ত না,
                       কোনো ভুল-বার্তাও ছিল না (২৫ সেকেন্ডের সময়সীমা শুধু
                       লগইনের জন্য, তার পরের অংশে কোনো সময়সীমাই ছিল না)।
                       ⇒ TK-এর দেখা ছবিটার তিনটে লক্ষণই এতে মেলে: সেকেন্ড নেই ·
                         "Could not open" আসেনি · পর্দা বদলায় না।

                       **সমাধান:** লগইনের পরেও গোনা ও ধাপের লেখা **চলতে থাকে**,
                       আর পর্দা সত্যিই আঁকা হলে (এই অস্থায়ী লেখাটা যখন আর
                       পর্দায় থাকে না) নিজে থেকেই থেমে যায়। তার উপরে একটা
                       **৪৫ সেকেন্ডের পাহারা** — এতক্ষণেও না খুললে "Could not
                       open" বার্তা আসে (Try again / Back), তাই কেউ আর চিরকাল
                       আটকে থাকবেন না।
                       ⛔ সফল ও দ্রুত পথে এক অক্ষরও বদলায়নি — পর্দা আঁকা হলেই
                          গোনা থেমে যায়, আর পাহারাটাও কিছু করে না। */
                    OpenTrace.step(activity, "6. opening the screen")
                    toast(activity, "Opened in " + secsSoFar() + " s")
                    val stopWatch = Runnable {
                        try { tick.removeCallbacksAndMessages(null) } catch (_: Throwable) { }
                        try { OpenTrace.onStep = null } catch (_: Throwable) { }
                    }
                    tick.postDelayed({
                        try {
                            if (activity.isFinishing) { stopWatch.run(); return@postDelayed }
                            if (waitView?.isAttachedToWindow != true) { stopWatch.run(); return@postDelayed }
                            stopWatch.run()
                            AlertDialog.Builder(activity)
                                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(activity, "Could not open"))
                                .setMessage("Still opening after " + secsSoFar() + " s.\n\nStep:  " + OpenTrace.current() +
                                    "\n\nPlease check the internet and try again.")
                                .setPositiveButton("Try again") { d, _ ->
                                    try { d.dismiss() } catch (_: Throwable) { }
                                    ensureSignedIn(activity, prefillCode, onReady)
                                }
                                .setNegativeButton("Back") { _, _ -> activity.finish() }
                                .setCancelable(false)
                                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
                        } catch (_: Throwable) { }
                    }, 45000L)
                    onReady()
                    OpenTrace.done(activity)
                }
                else {
                    try { tick.removeCallbacksAndMessages(null) } catch (_: Throwable) { }
                    try { OpenTrace.onStep = null } catch (_: Throwable) { }
                    /* 🔴🔒 V808 (২৮.০৮.২০২৬) — TK: "staff Profile খুলছে না তো"।
                       আগে একটাই বোতাম ছিল ("Back") — অর্থাৎ নেট এক সেকেন্ডের জন্য
                       খারাপ হলেও পর্দা থেকে বেরিয়ে গিয়ে আবার সব শুরু করতে হত।
                       এখন **Try again** — একই জায়গা থেকে আবার চেষ্টা। */
                    AlertDialog.Builder(activity)
                        // 🎨 TK-APPROVED (2026-08-06, দল ২): রঙিন হেডার + রাউন্ডেড কার্ড।
                        .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(activity, "Could not open"))
                        .setMessage(err)
                        .setPositiveButton("Try again") { d, _ ->
                            try { d.dismiss() } catch (_: Throwable) { }
                            ensureSignedIn(activity, prefillCode, onReady)
                        }
                        .setNegativeButton("Back") { _, _ -> activity.finish() }
                        .setCancelable(false)
                        .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
                }
            }
        }.start()
    }

    fun maskMobile(m: String?): String {
        val d = (m ?: "").filter { it.isDigit() }
        return if (d.length >= 4) "••••••" + d.takeLast(4) else "••••"
    }
    /**
     * 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — **নম্বর আর লুকানো হয় না।**
     *
     * TK-এর কথা: *"নাম্বার এত হাই সিকিউরিটি ভাবে দেখানোর কিছু নেই। আমার কোন
     * বড় বিলাসবহুল কোম্পানি নয়, ছোটখাটো একটা ক্লিনিক। সুতরাং নাম্বারটা
     * সম্পূর্ণভাবে দেখাবে।"*
     *
     * ⛔ পুরোনো `maskMobile()` **মোছা হয়নি** — সরকারি আইডির মতো যেসব জায়গায়
     *    সত্যিই লুকানো দরকার, সেগুলো যেন না ভাঙে। এটা শুধু **দেখানোর** জন্য
     *    একটা নতুন, আলাদা ফাংশন।
     * ⛔ কোনো তথ্য বদলায় না · database ছোঁয়া হয় না।
     */
    fun fullMobile(m: String?): String {
        val raw = (m ?: "").trim()
        val d = raw.filter { it.isDigit() }
        if (d.isEmpty()) return raw
        return if (raw.startsWith("+")) raw else if (d.length > 10) "+$d" else d
    }

    fun maskIdLast4(last4: String?): String =
        if (!last4.isNullOrBlank()) "XXXX XXXX " + last4 else "—"

    /** ছবি-এর মতো "pill" বোতাম — আইকন বাক্স + শিরোনাম + উপ-লেখা।
     *  🔴 TK-নির্দেশ (02.08.2026): কোনো লেখা কাটবে না/ব্রেক হবে না/একটার উপর
     *  আরেকটা বসবে না — তাই এখানে ইচ্ছাকৃতভাবে কোনো maxLines/ellipsize/singleLine
     *  বসানো হয়নি (TextView-এর ডিফল্ট আচরণই স্বাভাবিক multi-line wrap)। পুরো সারি
     *  MATCH_PARENT প্রস্থ নেয় আর টেক্সট-কলাম weight=1 দিয়ে বাকি জায়গা পায়, তাই
     *  লম্বা লেখা স্ক্রিনের প্রস্থেই wrap করে নিচে নামে, কখনো পাশে উপচে পড়ে না। */
    fun pillButton(
        ctx: Context, icon: String, title: String, subtitle: String?,
        bgColor: String, iconBg: String, textColor: String, borderColor: String?,
        onClick: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 14), dp(ctx, 13), dp(ctx, 14), dp(ctx, 13))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 14).toFloat()
                setColor(android.graphics.Color.parseColor(bgColor))
                if (borderColor != null) setStroke(dp(ctx, 1), android.graphics.Color.parseColor(borderColor))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(ctx, 6); bottomMargin = dp(ctx, 6) }
        }
        val iconBox = TextView(ctx).apply {
            text = icon; textSize = 17f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 34), dp(ctx, 34))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 9).toFloat(); setColor(android.graphics.Color.parseColor(iconBg))
            }
        }
        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(ctx, 12) }
        }
        val titleTv = TextView(ctx).apply {
            text = title; textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(textColor))
        }
        textCol.addView(titleTv)
        if (!subtitle.isNullOrBlank()) {
            val subTv = TextView(ctx).apply {
                text = subtitle; textSize = 11f
                setTextColor(android.graphics.Color.parseColor(textColor)); alpha = 0.75f
                setPadding(0, dp(ctx, 1), 0, 0)
            }
            textCol.addView(subTv)
        }
        row.addView(iconBox); row.addView(textCol)
        return row
    }

    /** ছোট বর্গাকার আইকন-বোতাম — হেডারের ডানদিকে (যেমন ক্যালেন্ডার)। */
    fun iconButton(ctx: Context, icon: String, onClick: () -> Unit): TextView =
        TextView(ctx).apply {
            text = icon; textSize = 17f; gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 38), dp(ctx, 38))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 10).toFloat()
                setColor(android.graphics.Color.parseColor("#EAF6EE"))
                setStroke(dp(ctx, 1), android.graphics.Color.parseColor("#CFE9D8"))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }

    // 🔴 B313 (03.08.2026, TK-রিপোর্ট — হেডারের ক্যালেন্ডার-আইকনে সবসময় "July
    // 17" দেখাচ্ছিল) — এই একই বাগ প্রজেক্টে আগেও কয়েকবার ধরা পড়েছে ও ঠিক করা
    // হয়েছে (DoctorVisitActivity.kt-এর V233 "EXPECTED" কার্ড, FollowUpActivity.kt,
    // FollowUpAdapter.kt) — একলা "⏰"/"⏰" ইমোজি নিজের বাক্সে বসালে কিছু Android
    // emoji-ফন্ট (Noto ইত্যাদি) গ্লিফের ভিতরেই একটা বানানো/স্থির তারিখ ("Jul 17")
    // এঁকে দেয়, যেটা আসল আজকের তারিখ নয় আর কখনো বদলায় না। সমাধান একই প্রমাণিত
    // প্যাটার্নে — ইমোজির বদলে `Calendar.getInstance()` থেকে সত্যিকারের
    // মাস+দিন লেখা ২-লাইনের ছোট ব্যাজ, পর্দা খোলার সময় বসে বলে প্রতিদিন নিজে
    // থেকেই ঠিক থাকে। এই ফাংশনটা নতুন — Work Notebook/অন্য কোনো মডিউল ভবিষ্যতে
    // "আজকের তারিখ" আইকন লাগলে এটাই পুনর্ব্যবহার করবে, নতুন করে একই ভুল যেন
    // না হয়।
    fun liveDateIconButton(ctx: Context, onClick: () -> Unit): LinearLayout {
        val cal = java.util.Calendar.getInstance()
        val mon = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time)
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 38), dp(ctx, 38))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 10).toFloat()
                setColor(android.graphics.Color.parseColor("#EAF6EE"))
                setStroke(dp(ctx, 1), android.graphics.Color.parseColor("#CFE9D8"))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
            addView(TextView(ctx).apply {
                text = mon; textSize = 8.5f; gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#0B8A3E"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(ctx).apply {
                text = day; textSize = 13f; gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
        }
    }

    /** ছবি দেখানোর জন্য সাধারণ ImageView — বর্গাকার, হালকা ধূসর বর্ডার। */
    fun image(ctx: Context): android.widget.ImageView =
        android.widget.ImageView(ctx).apply {
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 96), dp(ctx, 96))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(ctx, 8).toFloat()
                setColor(android.graphics.Color.parseColor("#F0F2F6"))
                setStroke(dp(ctx, 1), android.graphics.Color.parseColor("#D8DEE8"))
            }
        }
}
