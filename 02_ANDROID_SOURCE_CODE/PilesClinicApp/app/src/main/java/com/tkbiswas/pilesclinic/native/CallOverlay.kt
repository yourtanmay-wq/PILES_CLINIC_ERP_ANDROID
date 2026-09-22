package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 🪟🔒🔒 V845 (৩০.০৮.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) —
 * **কল বাজার সময়** কল-স্ক্রিনের উপরে ভাসমান কার্ড।
 *
 * TK-রিপোর্ট: *"ফোন কল কাটার পরে কেন এটা আসবে… আমি চাইছি ডিউরিং ফোন কলে,
 * অর্থাৎ ফোন কল যখন আসবে তখনই যেন স্ক্রিনের ডিসপ্লেতে দেখা যায়।"*
 *
 * 🔬 আসল কারণ (কোড ধরে যাচাই, আন্দাজ নয়): `CallNotifyManager.onRinging()`
 * নোটিফিকেশনটা **কল বাজার সাথে সাথেই** পাঠায় — ওখানে কোনো ভুল ছিল না।
 * কিন্তু ওই মুহূর্তে ফোনের নিজের কল-স্ক্রিন পুরো পর্দা দখল করে রাখে, তাই
 * সাধারণ নোটিফিকেশন পিছনে চলে যায়। কল কাটলে পর্দা সরে ⇒ তখন চোখে পড়ে।
 * ⇒ উপরে দেখাতে হলে **overlay উইন্ডো** ছাড়া উপায় নেই।
 *
 * ⛔ যা কখনো হবে না:
 * · অনুমতি না থাকলে **চুপচাপ কিছুই করে না** — নোটিফিকেশন আগের মতোই চলে।
 * · কোনো ব্যতিক্রম হলেও **কল বা অ্যাপ কখনো আটকাবে না** (সব try/catch)।
 * · কোনো ক্লাউড-পড়া নেই — যা দেখানো হয় সব `CallNotifyManager`-এর
 *   ইতিমধ্যে আনা তথ্য থেকে। **Egress শূন্য।**
 * · কল কেটে গেলে বা স্টাফ ✕ চাপলে কার্ড সরে যায়; কখনো আটকে থাকে না।
 */
/* 🆕🔒🔒 V856 (৩০.০৮.২০২৬, TK-নির্দেশ: *"Card-এর যেকোনো জায়গায় চাপ দিয়ে যেন
   উপর নিচে সরানো যায়"*) — কার্ডের **সব জায়গা** থেকে টানা যায়, এমনকি
   Open / 📝 Remark / ✕ বোতামের উপর থেকেও।

   কীভাবে (Android-এর নিজের প্রমাণিত নিয়ম, নিজে বানানো কিছু নয়):
   · আঙুল নামলে কিছুই কেড়ে নেওয়া হয় না ⇒ বোতামে **চাপ** আগের মতোই কাজ করে।
   · আঙুল `scaledTouchSlop`-এর বেশি নড়লেই বাইরের এই বাক্সটা টাচটা **কেড়ে নেয়**
     (`onInterceptTouchEvent`) ⇒ তখন সেটা **টানা**, বোতাম আর চাপ খায় না।
   ⇒ তাই বোতামের উপরে আঙুল রেখে টানলেও কার্ড সরে, আর ভুল করে বোতাম চাপে না। */
private class DragCard(ctx: Context) : LinearLayout(ctx) {
    private val slop = android.view.ViewConfiguration.get(ctx).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    var onDragBegin: (() -> Unit)? = null
    var onDragMove: ((Float) -> Unit)? = null
    var onDragDone: (() -> Unit)? = null
    var onCardTap: (() -> Unit)? = null

    private fun moved(ev: android.view.MotionEvent): Boolean =
        kotlin.math.abs(ev.rawY - downY) > slop || kotlin.math.abs(ev.rawX - downX) > slop

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX; downY = ev.rawY; dragging = false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (!dragging && moved(ev)) {
                    dragging = true
                    onDragBegin?.invoke()
                    return true   // এখান থেকে টাচটা আমরা নিই ⇒ বোতাম আর চাপ খাবে না
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX; downY = ev.rawY; dragging = false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (!dragging && moved(ev)) { dragging = true; onDragBegin?.invoke() }
                if (dragging) onDragMove?.invoke(ev.rawY - downY)
            }
            android.view.MotionEvent.ACTION_UP -> {
                if (dragging) onDragDone?.invoke() else onCardTap?.invoke()
                dragging = false
            }
            android.view.MotionEvent.ACTION_CANCEL -> {
                if (dragging) onDragDone?.invoke()
                dragging = false
            }
        }
        return true
    }
}

object CallOverlay {

    private var view: View? = null
    private var wm: WindowManager? = null

    /** অনুমতি আছে কি না — না থাকলে কোথাও কিছু করা হয় না। */
    fun allowed(ctx: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            android.provider.Settings.canDrawOverlays(ctx) else true
    } catch (_: Throwable) { false }

    /** ফোনের Settings-এ "Display over other apps"-এর পাতা খোলে। */
    fun openPermissionScreen(ctx: Context) {
        try {
            val i = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + ctx.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (_: Throwable) { }
    }

    private fun dp(ctx: Context, v: Int): Int =
        (v * ctx.resources.displayMetrics.density).toInt()

    /**
     * কার্ডটা দেখানো (বা আগেরটা থাকলে নতুন তথ্যে বদলে দেওয়া)।
     * ⛔ সবসময় মূল থ্রেড থেকে ডাকতে হবে — WindowManager-এর নিয়ম।
     */
    /** ☎️ V1434 — "LAST CALL" বক্সের তথ্য। type: INCOMING · OUTGOING · MISSED;
     *  whenTxt: "Today 10:15 AM" ইত্যাদি; staff: কে কথা বলেছিল (রেকর্ড থাকলে);
     *  remark: শেষ রিমার্ক; first = true হলে বাকি সব উপেক্ষা করে "FIRST CALL"। */
    data class LastBox(
        val type: String = "",
        val whenTxt: String = "",
        val staff: String = "",
        val remark: String = "",
        val first: Boolean = false
    )

    fun show(
        ctx: Context,
        number: String,
        name: String,
        lines: List<String>,
        saved: Boolean,
        callType: String = "INCOMING",       // INCOMING · OUTGOING · MISSED (🆕 V856)
        autoHide: Boolean = false,           // Missed হলে ৬০ সেকেন্ড পরে নিজে সরে যায়
        /* ☎️🔒 V1434 (১৩.০৯.২০২৬, TK-নির্দেশ, ডেমো V9 পাশ) — Follow-up-এর "LAST CALL"
           বক্সের মতো **একটাই** বক্স: শেষ কল কী ছিল (Incoming/Outgoing/Missed) · কখন
           (Today/Yesterday/তারিখ + সময়) · কে কথা বলেছিল (রেকর্ড থাকলে) · নিচে শেষ রিমার্ক।
           রেকর্ড/কল কিছুই না থাকলে (first = true) হলুদ "FIRST CALL" বক্স।
           V1427-এর তিন লাইনের ইতিহাস ও "Last remark" লাইন — এই বক্সেই মিশে গেল
           (TK: *"টাইম ২ বার কেন?"*)। null হলে বক্স বসে না (তথ্য এখনো আসেনি)। */
        box: LastBox? = null,
        onOpen: () -> Unit,
        onRemark: () -> Unit
    ) {
        if (!allowed(ctx)) return
        try {
            hide(ctx)   // পুরনোটা থাকলে আগে সরাই — দুটো কার্ড কখনো নয়

            val root = DragCard(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx, 13), dp(ctx, 12), dp(ctx, 13), dp(ctx, 12))
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx, 16).toFloat()
                    setColor(Color.WHITE)
                }
                elevation = dp(ctx, 8).toFloat()
            }

            /* 🔄🔒 V856 — TK: *"TK BISWAS PILES CLINIC লেখা এখানে রাখতে চাই না"*
               ⇒ ওই লাইনটা বাদ। বদলে উপরে একটাই সারি: বাঁয়ে কলের ধরন,
               মাঝে টানার হাতল, ডানে ✕ (TK: *"× চিহ্নটা উপরে ডান দিকে থাকবে"*)। */
            val topRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(TextView(ctx).apply {
                text = callType
                textSize = 9f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2))
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx, 10).toFloat()
                    setColor(Color.parseColor(when (callType) {
                        "MISSED" -> "#E5484D"
                        "OUTGOING" -> "#1167D8"
                        else -> "#0C9E33"
                    }))
                }
            })
            // মাঝের ধূসর দাগ = "আমাকে ধরে টানা যায়" — চেনা ইঙ্গিত।
            topRow.addView(View(ctx).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(ctx, 3).toFloat()
                    setColor(Color.parseColor("#D3DCE6"))
                }
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 42), dp(ctx, 4), 1f)
                    .apply { marginStart = dp(ctx, 8); marginEnd = dp(ctx, 8) }
            })
            topRow.addView(TextView(ctx).apply {
                text = "✕"
                textSize = 15f
                setTextColor(Color.parseColor("#8A96A3"))
                setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 2), dp(ctx, 2))
                isClickable = true
                setOnClickListener { hide(ctx) }
            })
            root.addView(topRow)

            /* ☎️🔒 V1434 — TK: *"নাম এবং মোবাইল নম্বর পাশাপাশি রাখুন"* — এক সারিতে
               নাম (কালো, মোটা) + নম্বর (নীল); নাম না থাকলে শুধু নম্বর। */
            val nameRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(ctx, 6), 0, 0)
            }
            if (name.isNotBlank()) {
                nameRow.addView(TextView(ctx).apply {
                    text = name
                    textSize = 16f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(Color.parseColor("#1A1A1A"))
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    // weight 1 = নম্বর আগে নিজের জায়গা পায়, লম্বা নাম "…" হয়ে ছোট হয় — নম্বর কখনো চাপা পড়ে না।
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { marginEnd = dp(ctx, 8) }
                })
            }
            nameRow.addView(TextView(ctx).apply {
                text = number
                textSize = if (name.isNotBlank()) 14f else 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor(if (name.isNotBlank()) "#1167D8" else "#1A1A1A"))
                maxLines = 1
            })
            root.addView(nameRow)
            for (ln in lines) {
                if (ln.isBlank()) continue
                root.addView(TextView(ctx).apply {
                    text = ln
                    textSize = 12f
                    setTextColor(Color.parseColor("#5B7089"))
                    setPadding(0, dp(ctx, 3), 0, 0)
                })
            }
            /* ☎️🔒 V1434 — "LAST CALL" বক্স (Follow-up পর্দার ড্যাশ-সবুজ বক্সের মতো)। */
            if (box != null) {
                val first = box.first
                val boxView = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(ctx, 10), dp(ctx, 7), dp(ctx, 10), dp(ctx, 7))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(ctx, 10).toFloat()
                        setColor(Color.parseColor(if (first) "#FFF8E6" else "#EEF8F2"))
                        setStroke(dp(ctx, 1), Color.parseColor(if (first) "#E0B04A" else "#5FB08A"),
                            dp(ctx, 5).toFloat(), dp(ctx, 4).toFloat())
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(ctx, 9) }
                }
                boxView.addView(TextView(ctx).apply {
                    text = if (first) "✨ FIRST CALL · no earlier call or record" else run {
                        val color = when (box.type) {
                            "MISSED" -> "#E5484D"
                            "OUTGOING" -> "#1167D8"
                            else -> "#0C9E33"
                        }
                        val arrow = if (box.type == "OUTGOING") "↗ " else "↙ "
                        val sp = android.text.SpannableStringBuilder()
                        val head = arrow + box.type + " CALL"
                        sp.append(head)
                        sp.setSpan(android.text.style.ForegroundColorSpan(Color.parseColor(color)), 0, head.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sp.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, head.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sp.append(" " + box.whenTxt)
                        if (box.staff.isNotBlank()) {
                            val st = sp.length
                            sp.append(" (" + box.staff + ")")
                            sp.setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#B07A10")), st, sp.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        sp
                    }
                    textSize = 11.5f
                    setTextColor(Color.parseColor("#1A2A3A"))
                    maxLines = 2
                })
                if (!first && box.remark.isNotBlank()) {
                    boxView.addView(View(ctx).apply {
                        setBackgroundColor(Color.parseColor("#CFE6D8"))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1)
                        ).apply { topMargin = dp(ctx, 5); bottomMargin = dp(ctx, 5) }
                    })
                    boxView.addView(TextView(ctx).apply {
                        text = box.remark
                        textSize = 13f
                        setTextColor(Color.parseColor("#0F2438"))
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                }
                root.addView(boxView)
            }

            fun pill(label: String, bg: String, fg: String, w: Float, click: () -> Unit) =
                TextView(ctx).apply {
                    text = label
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor(fg))
                    setPadding(dp(ctx, 6), dp(ctx, 8), dp(ctx, 6), dp(ctx, 8))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(ctx, 9).toFloat()
                        setColor(Color.parseColor(bg))
                    }
                    layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, w
                    ).apply { marginStart = dp(ctx, 3); marginEnd = dp(ctx, 3) }
                    isClickable = true
                    setOnClickListener { click() }
                }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(ctx, 10) }
            }
            row.addView(pill(if (saved) "Open" else "+ New Enquiry", "#7A3FF2", "#FFFFFF", 1.3f) {
                hide(ctx); onOpen()
            })
            row.addView(pill(NoBengali.s("📝 Remark"), "#1777F2", "#FFFFFF", 1.2f) {
                hide(ctx); onRemark()
            })
            // 🔄 V856 — ✕ এখন উপরে ডানদিকে (TK-নির্দেশ), তাই এই সারি থেকে বাদ।
            root.addView(row)

            // কার্ডের গায়ে চাপ = Open (TK-নির্দেশ V844-এর একই নিয়ম)
            root.isClickable = true

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                /* ⛔ NOT_FOCUSABLE — কার্ডটা যেন কল ধরার/কাটার বোতাম কেড়ে
                   না নেয়। স্টাফ আগের মতোই কল ধরতে পারবেন। */
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                // 🆕 V856 — আগে সরিয়ে থাকলে সেই জায়গাতেই, নইলে আগের মতোই ১২০dp।
                y = savedPosY(ctx, dp(ctx, 120))
                x = 0
                horizontalMargin = 0.03f
            }

            /* 🆕🔒🔒 V856 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) —
               TK: *"কল যখন আসে সেটা স্টিল থাকে, অর্থাৎ ওটা আমরা উপরে বা নিচে
               সরাতে পারি না"*। ⇒ কার্ডটা এখন আঙুল দিয়ে টেনে সরানো যায়।

               ─── কীভাবে চাপ আর টানা আলাদা করা হয় (সবচেয়ে জরুরি) ──────────
               আঙুল **ViewConfiguration-এর নিজের `scaledTouchSlop`**-এর চেয়ে কম
               নড়লে ⇒ এটা **চাপ**, তাই আগের মতোই Open খোলে। বেশি নড়লে ⇒ **টানা**,
               তখন Open খোলে না — শুধু কার্ড সরে। এটা Android-এর নিজের মাপ,
               নিজে বানানো কোনো সংখ্যা নয়, তাই সব ফোনেই ঠিকভাবে কাজ করে।
               ⛔ Open · 📝 Remark · ✕ — তিনটেই নিজেরা চাপ ধরে (child view),
                  তাই ওগুলোর কাজ এক অক্ষরও বদলায়নি।
               ⛔ জায়গাটা ফোনেই মনে থাকে (`call_overlay_pos`), পরের কলে ওখানেই
                  বসে। পর্দার বাইরে কখনো যেতে পারে না (নিচে clamp)।
               ⛔ কোনো ক্লাউড-পড়া/লেখা নেই — Egress শূন্য। */
            val screenH = ctx.resources.displayMetrics.heightPixels
            var startY = 0
            root.onDragBegin = { startY = lp.y }
            root.onDragMove = { dy ->
                // পর্দার বাইরে যেন কখনো না যায় — উপরে ০, নিচে পর্দার শেষ।
                val maxY = (screenH - dp(ctx, 140)).coerceAtLeast(0)
                lp.y = (startY + dy.toInt()).coerceIn(0, maxY)
                try { wm?.updateViewLayout(root, lp) } catch (_: Throwable) { }
            }
            root.onDragDone = { savePosY(ctx, lp.y) }   // সরানো ⇒ জায়গাটা মনে রাখি
            /* 🔴🔒 V867 (৩০.০৮.২০২৬, TK-নির্দেশ): *"Card এ চাপ দিলে
               অটোমেটিক ইনকয়ারি ফর্ম কেন খুলবে"* ⇒ কার্ডের গায়ে শুধু চাপ দিলে
               **আর কিছুই খোলে না**। খুলতে হলে নিচের বোতামেই চাপতে হবে
               (Open / + New Enquiry · 📝 Remark), বন্ধ করতে উপরে ডানের ✕।
               ⛔ তিনটে বোতামের কাজ এক অক্ষরও বদলায়নি। */
            root.onCardTap = null

            val manager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            manager.addView(root, lp)
            wm = manager
            view = root

            /* 🆕 V856 — Missed কলের কার্ড ৬০ সেকেন্ড পরে নিজে থেকেই সরে যায়,
               যাতে পর্দায় কখনো আটকে না থাকে। ⛔ অন্য কোনো কলে এটা চলে না। */
            if (autoHide) {
                val token = root
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (view === token) hide(ctx)
                }, 60_000L)
            }
        } catch (_: Throwable) {
            // ⛔ কোনো কারণে না পারলে চুপচাপ ছেড়ে দিই — নোটিফিকেশন তো আছেই।
            view = null
        }
    }

    /* 🆕 V856 — কার্ডের জায়গা ফোনেই জমা থাকে (ক্লাউডে কিছু যায় না)। */
    private const val POS_PREFS = "call_overlay_pos"
    private const val POS_KEY = "y"

    private fun savedPosY(ctx: Context, fallback: Int): Int = try {
        val v = ctx.getSharedPreferences(POS_PREFS, Context.MODE_PRIVATE).getInt(POS_KEY, -1)
        if (v < 0) fallback else v
    } catch (_: Throwable) { fallback }

    private fun savePosY(ctx: Context, y: Int) {
        try {
            ctx.getSharedPreferences(POS_PREFS, Context.MODE_PRIVATE)
                .edit().putInt(POS_KEY, y).apply()
        } catch (_: Throwable) { }
    }

    /** কার্ড সরানো — বারবার ডাকলেও ক্ষতি নেই। */
    fun hide(ctx: Context) {
        try {
            val v = view ?: return
            (wm ?: ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (_: Throwable) {
        } finally {
            view = null
        }
    }
}
