package com.tkbiswas.pilesclinic.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * TK APPROVED (2026-07-15): the Patient card's payment badge used to be a
 * solid, fully-filled circle with the percentage written on top — it always
 * looked "100% full" no matter the real percentage. This draws a real
 * progress ring: a light background track plus a green arc that fills only
 * as much of the circle as the percentage paid, with the number centered —
 * same tap target, same size, only how it's drawn changed.
 *
 * 🔴🔒 V688 (২৫.০৮.২০২৬, নিজের যাচাইয়ে ধরা পড়া বাগ — V683-এ এই ক্লাসটাই
 * প্রথমবার XML-এ ব্যবহার হয়েছে, `item_followup_card.xml`-এ) — আগে শুধু
 * `(context: Context)` কনস্ট্রাক্টর ছিল, যা শুধু কোড-দিয়ে-বানানোর জন্য
 * চলে (FollowUpActivity.buildFollowCard()-এর মতো)। XML থেকে inflate করতে
 * Android-এর `(Context, AttributeSet?)` কনস্ট্রাক্টর **লাগেই** — না থাকলে
 * এই কার্ড বাঁধার সময় সঙ্গে সঙ্গে ক্র্যাশ (InflateException) করত। এখন
 * standard তিনটে View-কনস্ট্রাক্টরই আছে — পুরনো এক-আর্গুমেন্ট কল
 * (কোড-দিয়ে-বানানো, buildFollowCard()) অক্ষত, এক অক্ষরও বদলায়নি।
 */
class PaymentRingView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var percent: Int = 0
        set(value) { field = value.coerceIn(0, 100); invalidate() }

    private val density = context.resources.displayMetrics.density
    private val strokeWidth = 5f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@PaymentRingView.strokeWidth
        color = Color.parseColor("#E1E6ED")
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@PaymentRingView.strokeWidth
        color = Color.parseColor("#16A36D")
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10223A")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val arcRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = strokeWidth / 2f + 1f
        arcRect.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)
        if (percent > 0) {
            val sweep = 360f * (percent / 100f)
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)
        }
        /* 🟢🔒 V823 (২৯.০৮.২০২৬, TK-নির্দেশ ও অনুমোদিত ফটো-প্রুফ:
           *"গোল আকৃতির পার্সেন্টেজের মধ্যে 100% লেখাটার ফন্ট একটু ছোট করুন,
           যাতে গোলাকৃতিতে ঘেঁষে না যায়"*)।
           আগে ছিল `0.32f` — তাতে **"100%"** (চার অক্ষর) দু'পাশে বেড়ে ছুঁয়ে
           যেত। TK ফটো-প্রুফে ০.২৪ বেছেছেন।
           ⛔ বৃত্তের আকার · রং · বেড়ের মোটা · লেখার জায়গা — কিছুই বদলায়নি,
              শুধু লেখার মাপ। ⛔ কম শতাংশে (যেমন "0%") আরও বেশি ফাঁক থাকবে,
              তাই কোথাও কেটে যাওয়ার ভয় নেই। */
        textPaint.textSize = height * 0.24f
        val fm = textPaint.fontMetrics
        val textY = height / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText("$percent%", width / 2f, textY, textPaint)
    }
}
