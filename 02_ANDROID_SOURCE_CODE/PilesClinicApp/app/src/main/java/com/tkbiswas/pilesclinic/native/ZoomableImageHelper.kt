package com.tkbiswas.pilesclinic.native

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

/**
 * 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B64)
 *
 * TK-এর কথা: *"অন্যান্য অ্যাপ্লিকেশনের ফটো জুম করা যায়, এখানে আমি কেন জুম
 * করতে পারছি না?"*
 *
 * এই ছোট সাহায্যকারীটা যে কোনো `ImageView`-কে জুম-করার যোগ্য করে দেয় —
 *   • দুই আঙুলে **পিঞ্চ** করলে বড়/ছোট হয়,
 *   • **দুবার ট্যাপ** করলে একবারে বড় হয়, আবার দুবার ট্যাপে আগের মাপে ফেরে,
 *   • বড় অবস্থায় **আঙুল দিয়ে টেনে** ছবির যে কোনো দিকে যাওয়া যায়,
 *   • ছবি কখনো পর্দার বাইরে হারিয়ে যায় না (নিজে থেকেই ধরে রাখে),
 *   • ছোট করতে করতে **আসল মাপের চেয়ে ছোট হয় না**, বড় হয় সর্বোচ্চ ৫ গুণ।
 *
 * ⛔ **কোনো বাইরের লাইব্রেরি যোগ করা হয়নি** — Android-এর নিজের ব্যবস্থাতেই লেখা,
 *    তাই build-এ নতুন কোনো ঝুঁকি নেই।
 * ⛔ ছবিটা **পুরোটা দেখানো হয়** (`fit`) — আগের মতো চৌকো করে কেটে নয়।
 * ⛔ এটা নতুন ফাইল, তাই অন্য কোনো পর্দায় এর কোনো প্রভাব নেই।
 */
object ZoomableImageHelper {

    /** সর্বোচ্চ কত গুণ বড় করা যাবে (আসল মাপের তুলনায়)। */
    private const val MAX_TIMES = 5f

    /** দুবার ট্যাপ করলে কত গুণ বড় হবে। */
    private const val DOUBLE_TAP_TIMES = 2.5f

    @SuppressLint("ClickableViewAccessibility")
    fun attach(image: ImageView) {
        image.scaleType = ImageView.ScaleType.MATRIX

        val fit = Matrix()      // ছবিটা পুরো ধরে যাওয়ার মাপ (শুরুর অবস্থা)
        val live = Matrix()     // এখন যা দেখা যাচ্ছে
        var ready = false
        var lastX = 0f
        var lastY = 0f
        var dragging = false

        fun scaleOf(m: Matrix): Float {
            val v = FloatArray(9)
            m.getValues(v)
            return v[Matrix.MSCALE_X]
        }

        fun apply() {
            image.imageMatrix = live
            image.invalidate()
        }

        fun computeFit() {
            val d = image.drawable ?: return
            val vw = image.width.toFloat()
            val vh = image.height.toFloat()
            val dw = d.intrinsicWidth.toFloat()
            val dh = d.intrinsicHeight.toFloat()
            if (vw <= 0f || vh <= 0f || dw <= 0f || dh <= 0f) return
            val s = if (vw / dw < vh / dh) vw / dw else vh / dh
            fit.reset()
            fit.setScale(s, s)
            fit.postTranslate((vw - dw * s) / 2f, (vh - dh * s) / 2f)
            live.set(fit)
            ready = true
            apply()
        }

        fun clamp() {
            val d = image.drawable ?: return
            val r = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
            live.mapRect(r)
            val vw = image.width.toFloat()
            val vh = image.height.toFloat()
            var dx = 0f
            var dy = 0f
            if (r.width() <= vw) {
                dx = (vw - r.width()) / 2f - r.left
            } else {
                if (r.left > 0f) dx = -r.left
                if (r.right < vw) dx = vw - r.right
            }
            if (r.height() <= vh) {
                dy = (vh - r.height()) / 2f - r.top
            } else {
                if (r.top > 0f) dy = -r.top
                if (r.bottom < vh) dy = vh - r.bottom
            }
            if (dx != 0f || dy != 0f) live.postTranslate(dx, dy)
            apply()
        }

        val scaleDetector = ScaleGestureDetector(
            image.context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (!ready) return true
                    val base = scaleOf(fit)
                    if (base <= 0f) return true
                    val now = scaleOf(live)
                    if (now <= 0f) return true
                    var factor = detector.scaleFactor
                    val target = now * factor
                    val min = base
                    val max = base * MAX_TIMES
                    if (target < min) factor = min / now
                    if (target > max) factor = max / now
                    live.postScale(factor, factor, detector.focusX, detector.focusY)
                    clamp()
                    return true
                }
            }
        )

        val tapDetector = GestureDetector(
            image.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (!ready) return true
                    val base = scaleOf(fit)
                    if (base <= 0f) return true
                    if (scaleOf(live) > base * 1.05f) {
                        live.set(fit)
                    } else {
                        live.postScale(DOUBLE_TAP_TIMES, DOUBLE_TAP_TIMES, e.x, e.y)
                    }
                    clamp()
                    return true
                }
            }
        )

        image.isClickable = true
        image.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> computeFit() }

        image.setOnTouchListener { v, event ->
            if (!ready) computeFit()
            scaleDetector.onTouchEvent(event)
            tapDetector.onTouchEvent(event)
            val zoomed = ready && scaleOf(live) > scaleOf(fit) * 1.02f
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    dragging = zoomed
                    if (dragging) v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                    // আঙুলের সংখ্যা বদলালে টানার হিসাব নতুন করে শুরু হয়, নইলে
                    // ছবিটা হঠাৎ লাফ দিত।
                    lastX = event.x
                    lastY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (dragging && zoomed && event.pointerCount == 1 && !scaleDetector.isInProgress) {
                        live.postTranslate(event.x - lastX, event.y - lastY)
                        lastX = event.x
                        lastY = event.y
                        clamp()
                    } else {
                        lastX = event.x
                        lastY = event.y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            true
        }
    }

    /** নতুন ছবি বসানোর পরে আগের জুম মুছে আবার শুরুর মাপে ফেরায়। */
    fun reset(image: ImageView) {
        image.post {
            val d = image.drawable ?: return@post
            val vw = image.width.toFloat()
            val vh = image.height.toFloat()
            val dw = d.intrinsicWidth.toFloat()
            val dh = d.intrinsicHeight.toFloat()
            if (vw <= 0f || vh <= 0f || dw <= 0f || dh <= 0f) return@post
            val s = if (vw / dw < vh / dh) vw / dw else vh / dh
            val m = Matrix()
            m.setScale(s, s)
            m.postTranslate((vw - dw * s) / 2f, (vh - dh * s) / 2f)
            image.imageMatrix = m
            image.invalidate()
        }
    }
}
