package com.tkbiswas.pilesclinic.clinical

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/**
 * 🔵🔒 V570 (২২.০৮.২০২৬, TK-অনুমোদিত "প্রস্তাব ক") — আঁকার একটা বোতামের আইকন।
 *
 * TK: *"এগুলো থাকা আপনার কাছে কি মনে হয় যে এটা দেখতে ভালো লাগছে? আমার কাছে
 * তো প্রফেশনালহীন মনে ... আরো ভালোভাবে কিভাবে তৈরি করা যায় সেটা আমাকে দেখান"*।
 *
 * বোতাম একটাও বাদ যায়নি — সংখ্যা সমস্যা ছিল না, চেহারা ছিল। ইমোজি + বাংলা
 * লেখা মিলিয়ে আটটা চওড়া বোতাম এক সারিতে ধরত না, তাই ২–৩ সারিতে ভেঙে ছবির
 * অনেকটা ঢেকে দিত। এখন পরিষ্কার আঁকা আইকন — আটটাই **এক সারিতে**।
 *
 * ⛔ **নতুন কোনো drawable/XML ফাইল যোগ করা হয়নি** — আগে ছবির নাম নিয়ে
 *    aapt2-তে বিল্ড ভেঙেছিল (V559), তাই এখানে আইকনগুলো **কোডেই আঁকা** হয়।
 * ⚠️ পথের লেখা (`path data`) ওয়েবের `WLV1_ANAT_ICONS`-এর **হুবহু একই**, তাই
 *    ফোন আর কম্পিউটারে আইকন দুটো এক দেখায়।
 */
class AnatToolIcon(context: Context, private val kind: String) : View(context) {

    /** এই হাতিয়ারটাই কি এখন চলছে? */
    var on: Boolean = false
        set(v) { field = v; invalidate() }

    /** কালো পটভূমিতে (পুরো পর্দা) না সাদা ঘরে (ছোট বোর্ড) বসছে। */
    var darkBar: Boolean = false
        set(v) { field = v; invalidate() }

    /** লাল রঙের কাজ (সব মুছুন)। */
    var danger: Boolean = false
        set(v) { field = v; invalidate() }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val src: Path = buildPath(kind)
    private val out = Path()
    private val m = Matrix()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val r = Math.min(w, h) / 2f

        if (on) {
            fill.color = Color.parseColor(if (danger) "#C1382B" else "#12A05A")
            canvas.drawCircle(w / 2f, h / 2f, r, fill)
        }
        val ink = when {
            on -> "#FFFFFF"
            danger -> if (darkBar) "#FF8A80" else "#C1382B"
            darkBar -> "#CBD7E6"
            else -> "#5B6B81"
        }
        stroke.color = Color.parseColor(ink)

        // আইকনটা বোতামের মাঝখানে, বোতামের ~৫৮% জায়গা জুড়ে
        val side = r * 2f * ICON_FRACTION
        val sc = side / 24f
        stroke.strokeWidth = 1.9f * sc
        m.reset()
        m.setScale(sc, sc)
        m.postTranslate((w - side) / 2f, (h - side) / 2f)
        out.reset(); src.transform(m, out)
        canvas.drawPath(out, stroke)
    }

    companion object {
        private const val ICON_FRACTION = 0.58f

        /** ওয়েবের `WLV1_ANAT_ICONS`-এর হুবহু একই পথ, ২৪×২৪ ঘরে আঁকা। */
        fun pathData(kind: String): String = when (kind) {
            "pile"  -> "M12 21s6.2-6.1 6.2-10.4A6.2 6.2 0 0 0 5.8 10.6C5.8 14.9 12 21 12 21z" +
                       "M14.1 10.5a2.1 2.1 0 1 1-4.2 0 2.1 2.1 0 0 1 4.2 0z"
            // 🟢🔒🔒 V673 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — "ফোলান"-এর
            // নিজস্ব আইকন এতদিন ছিলই না (else-শাখায় পড়ে জেনেরিক সূর্য-চিহ্ন
            // দেখাত)। এখন একটা গোলাকার, ছোপ-ছোপ দাগ-দেওয়া "মাংসপিণ্ড"-এর
            // মতো আইকন — ওয়েবের WLV1_ANAT_ICONS-এও এই একই পথ যোগ করা হলো
            // (দুই প্ল্যাটফর্মে এক দেখানোর নিয়ম অক্ষত রাখতে)।
            "bulge" -> "M8.5 20.5c-3-2-4.5-5-3.7-8.3C5.7 8.6 9 6 12.5 6.3c3 .3 5 2.8 4.7 5.8" +
                       "-.3 3-3 5.8-6.2 7.4-.8.4-1.7.5-2.5 1z" +
                       "M9.5 12a0.9 0.9 0 1 1 0-1.8 0.9 0.9 0 0 1 0 1.8z" +
                       "M12.8 15a0.9 0.9 0 1 1 0-1.8 0.9 0.9 0 0 1 0 1.8z" +
                       "M13.5 10.5a0.7 0.7 0 1 1 0-1.4 0.7 0.7 0 0 1 0 1.4z"
            "tract" -> "M3 15c2.6 0 2.6-6 5.2-6s2.6 6 5.2 6 2.6-6 5.2-6"
            /* 🔴🔒 V793 — "ফাটল" (Fissure): একটা লম্বা চেরা দাগ, দু'পাশে
               শক্ত কিনারার ছোট দুটো টান। ⚠️ ওয়েবের একই পথ। */
            "fis"   -> "M12 3.5c-1.6 3.4 1.6 4.4 0 7.6-1.6 3.2 1.6 4.2 0 9.4" +
                       "M8.6 8.2 6.8 6.6M15.4 8.2l1.8-1.6M8.4 16l-1.8 1.6M15.6 16l1.8 1.6"
            "ring"  -> "M19.6 12a7.6 6.3 0 1 1-15.2 0 7.6 6.3 0 0 1 15.2 0z"
            "arrow" -> "M4 12h14M13 7l5 5-5 5"
            "erase" -> "M8.5 19.5 4 15a2 2 0 0 1 0-2.8l7.2-7.2a2 2 0 0 1 2.8 0l4.6 4.6a2 2 0 0 1 0 2.8l-7 7zM9 20h10"
            "undo"  -> "M4 10h9a5 5 0 1 1 0 10H8M8 6 4 10l4 4"
            "trash" -> "M4.5 7h15M9.5 7V5.2A1.2 1.2 0 0 1 10.7 4h2.6a1.2 1.2 0 0 1 1.2 1.2V7" +
                       "M6.4 7l.9 12.1A1.6 1.6 0 0 0 8.9 20.6h6.2a1.6 1.6 0 0 0 1.6-1.5L17.6 7"
            "full"  -> "M9 4H4v5M15 4h5v5M9 20H4v-5M15 20h5v-5"
            /* 🔵 V585 — "কেন্দ্র" হাতিয়ার: ঘড়ির মত গোল, ভিতরে ক্রসহেয়ার।
               ⚠️ ওয়েবের `WLV1_ANAT_ICONS.centre`-এর হুবহু একই পথ। */
            "centre" -> "M20 12a8 8 0 1 1-16 0 8 8 0 0 1 16 0z" +
                       "M12 2.6v3.2M12 18.2v3.2M2.6 12h3.2M18.2 12h3.2" +
                       "M13.4 12a1.4 1.4 0 1 1-2.8 0 1.4 1.4 0 0 1 2.8 0z"
            else    -> "M16.2 12a4.2 4.2 0 1 1-8.4 0 4.2 4.2 0 0 1 8.4 0z" +
                       "M12 3.2v2.4M12 18.4v2.4M3.2 12h2.4M18.4 12h2.4" +
                       "M5.8 5.8l1.7 1.7M16.5 16.5l1.7 1.7M18.2 5.8l-1.7 1.7M7.5 16.5l-1.7 1.7"
        }

        /** পথের লেখা → আঁকার পথ। ভাঙা লেখাতেও কখনো ক্র্যাশ করে না। */
        private fun buildPath(kind: String): Path = try {
            androidx.core.graphics.PathParser.createPathFromPathData(pathData(kind))
        } catch (_: Throwable) { Path() }
    }
}
