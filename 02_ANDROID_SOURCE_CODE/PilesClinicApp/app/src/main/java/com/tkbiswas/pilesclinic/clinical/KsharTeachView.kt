package com.tkbiswas.pilesclinic.clinical

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🎓🔒 V783 (২৮.০৮.২০২৬, TK-নির্দেশ ও ডেমো-ফটো অনুমোদনের পরে) —
 * **রোগীকে বোঝানোর পর্দা: পাইলস · ফিশার · ফিস্টুলা।**
 *
 * TK-এর কথা (হুবহু): *"আমি যেন বেছে নিতে পারি — পাইলস ফিসার ফিস্টুলা। যে
 * রোগীর যে সমস্যা আমি সেই অনুসারে যেন বোঝাতে পারি পেশেন্টকে।"* ·
 * *"এটা লিখে রাখার কিছু নেই"* · *"একই পেশেন্টের তিন রকম রোগও তো থাকতে পারে।"*
 *
 * ═════════════════════════════════════════════════════════════════════
 * ⛔ **এই পর্দা কিচ্ছু সেভ করে না** — ডেটাবেস · প্রিন্ট · A4 · রিপোর্ট কোথাও
 *    যায় না। শুধু আঁকে। তাই কোনো ঝুঁকি নেই।
 * ⛔ ডাক্তারের নিজের আঁকা ছবি (`AnatomyView`) **এক বিন্দুও ছোঁয়া হয় না** —
 *    এটা সম্পূর্ণ আলাদা একটা View, আলাদা পর্দায় খোলে।
 * ⛔ তিনটে রোগ সবসময় পাশাপাশি থাকে, ইচ্ছেমতো বদলানো যায় — একই রোগীর তিন
 *    রোগ থাকলে একটার পর একটা দেখানো যাবে (TK-এর শর্ত)।
 * ⛔ কোনো ছবি-ফাইল বা ভিডিও নেই — সবটাই gradient/shadow দিয়ে আঁকা, তাই
 *    অ্যাপের সাইজ বাড়ে না, নেট লাগে না, অফলাইনেও চলে।
 * ═════════════════════════════════════════════════════════════════════
 *
 * **চিকিৎসার ধাপ — ওয়েবে যাচাই করে নেওয়া (আন্দাজ নয়):**
 * · পাইলস — ইনজেকশন · ফুলে ওঠা · **গোড়ায় ক্ষারসূত্র** · কেটে পড়া
 * · ফিশার — **ক্ষার-কর্ম**: অপামার্গ ক্ষার লাগিয়ে পরে ধুয়ে ফেলা, সপ্তাহে
 *   একবার, 3-4 বারে সারে (সুতো বাঁধা হয় **না**) — PMC7685256
 * · ফিস্টুলা — প্রোব · সুতো টেনে আনা · **দুই মাথায় গিঁট (লুপ)** · সাপ্তাহিক
 *   বদল · কেটে ভরে যাওয়া — ClinicalTrials NCT01880398
 */
class KsharTeachView(ctx: Context) : View(ctx) {

    companion object {
        const val PILES = 0
        const val FISSURE = 1
        const val FISTULA = 2

        /** আঁকার কল্পিত মাপ — পর্দা যত বড়ই হোক, সব হিসাব এই মাপেই। */
        private const val VW = 900f
        private const val VH = 640f
        private const val CANAL_X = 548f
        private const val SKIN_Y = 470f
        private const val TOP_Y = 40f
        private const val EXT_X = 272f
        private const val EXT_Y = 472f
        private const val INT_X = 500f
        private const val INT_Y = 296f
        /** ছবি ২ (পাশ থেকে কাটা) — নালীর কেন্দ্র ও ভিতরের মুখ। */
        private const val S_CX = 450f
        private const val S_INT_X = 432f
        private const val S_INT_Y = 330f

        fun stepCount(disease: Int): Int = when (disease) {
            PILES -> 5
            FISSURE -> 5
            else -> 6
        }

        /** পর্দায় দেখানো লেখা। ⛔ সব বাংলা — এটা **রোগীকে** বোঝানোর পর্দা। */
        fun caption(disease: Int, step: Int): String = when (disease) {
            PILES -> when (step) {
                0 -> "1) Piles — ফোলা মাংস"
                1 -> "2) ইনজেকশন দেওয়া হচ্ছে"
                2 -> "3) মাংস আরো ফুলে উঠল"
                3 -> "4) গোড়ায় ক্ষারসূত্র বাঁধা হলো"
                else -> "5) কেটে পড়ল — জায়গা পরিষ্কার"
            }
            FISSURE -> when (step) {
                0 -> "1) Fissure — শক্ত সাদা কিনারা, বাইরে ছোট মাংস"
                1 -> "2) জায়গাটা অবশ করা হচ্ছে"
                2 -> "3) ফাটলের উপর ক্ষার লাগানো হচ্ছে"
                3 -> "4) ক্ষার ধুয়ে ফেলা — শক্ত কিনারা গলে গেল"
                else -> "5) ঘা ভরে উঠছে — সপ্তাহে একবার, 3-4 বারে সারে"
            }
            else -> when (step) {
                0 -> "1) Fistula — নালী, বাইরের ও ভিতরের মুখ"
                1 -> "2) প্রোব ঢোকানো হচ্ছে"
                2 -> "3) প্রোবের ফুটোয় ক্ষারসূত্র বেঁধে টেনে আনা"
                3 -> "4) দুই মাথা বাইরে এনে গিঁট — সুতো বাঁধা থাকে"
                4 -> "5) সপ্তাহে একবার সুতো বদল — একটু একটু কেটে ভরাট"
                else -> "6) নালী সম্পূর্ণ কেটে গেল — ঘা ভরে গেল"
            }
        }
    }

    var disease: Int = PILES
    var step: Int = 0
    var t: Float = 1f

    /** 🖼️🔒 V783 — কোন ভিত্তি-ছবি (TK-এর বাছাই: ১ = সামনে থেকে কাটা,
     *  ২ = পাশ থেকে কাটা)। বদলালেই নিশানার জায়গা ওই ছবির ডিফল্টে ফেরে। */
    var base: Int = 0
        set(v) { field = v.coerceIn(0, 1); resetSpots() }

    /* 👆 ডাক্তার ছুঁয়ে নিশানা যেখানে দেবেন — TK: *"মাংস ফোলাবো বা ফিস্টুলার
       নিশানা দিব অথবা ফিশারের দাগ"*। তিন রোগের তিনটে আলাদা জায়গা, তাই
       একটা বদলালে অন্যটা নড়ে না। ⛔ কিছুই সেভ হয় না। */
    private var pileX = 0f; private var pileY = 0f      // পাইলসের মাংস
    private var extX = 0f;  private var extY = 0f       // ফিস্টুলার বাইরের মুখ
    private var fisX = 0f;  private var fisY = 0f       // ফিশারের নিচের মাথা

    /** ভিত্তি-ছবি বদলালে নিশানাগুলো ওই ছবির স্বাভাবিক জায়গায় ফেরে। */
    fun resetSpots() {
        if (base == 0) {
            pileX = CANAL_X - 46f; pileY = INT_Y - 38f
            extX = EXT_X; extY = EXT_Y
            fisX = CANAL_X - 24f; fisY = SKIN_Y - 2f
        } else {
            pileX = S_CX - 20f; pileY = 300f
            extX = 300f; extY = SKIN_Y + 2f
            fisX = S_CX - 12f; fisY = SKIN_Y - 4f
        }
        invalidate()
    }

    init { resetSpots(); isClickable = true }

    /** 👆 ছুঁয়ে/টেনে নিশানা সরানো — যে রোগ চালু আছে সেটারই সরে। */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: android.view.MotionEvent): Boolean {
        if (sc <= 0f) return false
        val x = (e.x - ox) / sc
        val y = (e.y - oy) / sc
        when (e.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN,
            android.view.MotionEvent.ACTION_MOVE -> {
                when (disease) {
                    PILES -> { pileX = x.coerceIn(60f, VW - 60f); pileY = y.coerceIn(TOP_Y + 60f, SKIN_Y - 20f) }
                    FISSURE -> { fisX = x.coerceIn(60f, VW - 60f); fisY = y.coerceIn(TOP_Y + 120f, SKIN_Y + 10f) }
                    else -> { extX = x.coerceIn(40f, VW - 40f); extY = y.coerceIn(TOP_Y + 80f, SKIN_Y + 20f) }
                }
                invalidate(); return true
            }
        }
        return true
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sc = 1f
    private var ox = 0f
    private var oy = 0f

    /** চর্বির কোষ — একবারই তৈরি, তাই প্রতিবার আঁকায় নড়ে না। */
    private val fatCells: List<FloatArray> = run {
        val r = java.util.Random(11)
        (0 until 190).map {
            floatArrayOf(
                6f + r.nextFloat() * (VW - 12f),
                14f + r.nextFloat() * (SKIN_Y - 78f),
                4f + r.nextFloat() * 7f,
                0.55f + r.nextFloat() * 0.3f
            )
        }
    }
    /** শ্লেষ্মার রক্তনালী — একবারই তৈরি। */
    private val vessels: List<FloatArray> = run {
        val r = java.util.Random(5)
        (0 until 20).map {
            val x = CANAL_X - 88f + r.nextFloat() * 176f
            val y = TOP_Y + 40f + r.nextFloat() * (SKIN_Y - TOP_Y - 60f)
            val len = 30f + r.nextFloat() * 50f
            val a = (-1.9 + r.nextFloat() * 0.7).toFloat()
            floatArrayOf(x, y, x + cos(a) * len, y + sin(a) * len, 0.9f + r.nextFloat() * 1.2f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val vw = width.toFloat(); val vh = height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        sc = minOf(vw / VW, vh / VH)
        ox = (vw - VW * sc) / 2f
        oy = (vh - VH * sc) / 2f
        val save = canvas.save()
        canvas.translate(ox, oy)
        canvas.scale(sc, sc)
        try {
            if (base == 0) drawBody(canvas) else drawBodySide(canvas)
            when (disease) {
                PILES -> drawPiles(canvas)
                FISSURE -> drawFissure(canvas)
                else -> drawFistula(canvas)
            }
        } catch (_: Throwable) { }
        canvas.restoreToCount(save)
    }

    // ─────────────────────────── শরীর ───────────────────────────
    private fun canalPath(wTop: Float, wBot: Float, y0: Float, y1: Float): Path {
        val path = Path()
        path.moveTo(CANAL_X - wTop, y0)
        path.quadTo(CANAL_X - wTop - 16f, y0 + 170f, CANAL_X - wBot - 22f, y0 + 290f)
        path.lineTo(CANAL_X - wBot, y1)
        path.lineTo(CANAL_X + wBot, y1)
        path.lineTo(CANAL_X + wBot + 22f, y0 + 290f)
        path.quadTo(CANAL_X + wTop + 16f, y0 + 170f, CANAL_X + wTop, y0)
        path.close()
        return path
    }

    private fun vgrad(x0: Float, y0: Float, x1: Float, y1: Float, vararg c: Int): Shader =
        LinearGradient(x0, y0, x1, y1, c, null, Shader.TileMode.CLAMP)

    private fun drawBody(c: Canvas) {
        // পটভূমি (শরীরের বাইরে)
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.color = Color.parseColor("#EFE6E0")
        c.drawRect(0f, 0f, VW, VH, p)

        // শরীর (চর্বি)
        val bodyP = Path().apply {
            moveTo(0f, 0f); lineTo(VW, 0f); lineTo(VW, SKIN_Y - 16f)
            quadTo(CANAL_X + 160f, SKIN_Y + 2f, CANAL_X + 30f, SKIN_Y + 6f)
            lineTo(CANAL_X - 30f, SKIN_Y + 6f)
            quadTo(CANAL_X - 160f, SKIN_Y + 2f, 0f, SKIN_Y - 16f); close()
        }
        p.shader = vgrad(VW * 0.2f, 0f, VW * 0.8f, SKIN_Y,
            Color.parseColor("#FBEEE0"), Color.parseColor("#F2DCC6"), Color.parseColor("#E4C6A9"))
        c.drawPath(bodyP, p); p.shader = null

        // চর্বির কোষ
        val sv = c.save(); c.clipPath(bodyP)
        for (f in fatCells) {
            p.style = Paint.Style.FILL; p.color = Color.parseColor("#FFF7EC"); p.alpha = 34
            c.drawOval(RectF(f[0] - f[2], f[1] - f[2] * f[3], f[0] + f[2], f[1] + f[2] * f[3]), p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f
            p.color = Color.parseColor("#CDA687"); p.alpha = 40
            c.drawOval(RectF(f[0] - f[2], f[1] - f[2] * 0.7f, f[0] + f[2], f[1] + f[2] * 0.7f), p)
        }
        p.alpha = 255
        c.restoreToCount(sv)

        // চামড়ার স্তর
        val skinP = Path().apply {
            moveTo(0f, SKIN_Y - 56f); lineTo(VW, SKIN_Y - 56f); lineTo(VW, SKIN_Y - 16f)
            quadTo(CANAL_X + 160f, SKIN_Y + 2f, CANAL_X + 30f, SKIN_Y + 6f)
            lineTo(CANAL_X - 30f, SKIN_Y + 6f)
            quadTo(CANAL_X - 160f, SKIN_Y + 2f, 0f, SKIN_Y - 16f); close()
        }
        p.style = Paint.Style.FILL
        p.shader = vgrad(0f, SKIN_Y - 56f, 0f, SKIN_Y + 6f,
            Color.parseColor("#F6DFD2"), Color.parseColor("#EBC7B5"), Color.parseColor("#D9A18E"))
        c.drawPath(skinP, p); p.shader = null

        // চামড়ার ভাঁজ (মলদ্বার ঘিরে)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f
        p.color = Color.parseColor("#B67F6C"); p.alpha = 115
        for (k in 0 until 16) {
            val a = Math.PI * (0.06 + 0.88 * k / 15.0)
            val x0 = CANAL_X + (cos(a) * 38).toFloat(); val x1 = CANAL_X + (cos(a) * 150).toFloat()
            val y0 = SKIN_Y + 4f - (sin(a) * 7).toFloat(); val y1 = SKIN_Y - 14f - (sin(a) * 20).toFloat()
            val pp = Path().apply { moveTo(x0, y0); quadTo((x0 + x1) / 2, (y0 + y1) / 2 - 6f, x1, y1) }
            c.drawPath(pp, p)
        }
        p.alpha = 255

        // বাইরের পেশি + ডোরা
        val ex = canalPath(186f, 58f, TOP_Y + 12f, SKIN_Y + 6f)
        p.style = Paint.Style.FILL
        p.shader = vgrad(CANAL_X - 186f, 0f, CANAL_X + 186f, 0f,
            Color.parseColor("#7E3330"), Color.parseColor("#C06A62"), Color.parseColor("#79302D"))
        c.drawPath(ex, p); p.shader = null
        val s2 = c.save(); c.clipPath(ex)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f
        p.color = Color.parseColor("#5F2321"); p.alpha = 90
        var y = TOP_Y + 20f
        while (y < SKIN_Y + 6f) {
            val pp = Path().apply { moveTo(CANAL_X - 200f, y); quadTo(CANAL_X, y + 4f, CANAL_X + 200f, y) }
            c.drawPath(pp, p); y += 9f
        }
        p.alpha = 255
        c.restoreToCount(s2)

        // ভিতরের পেশি
        val inn = canalPath(152f, 38f, TOP_Y + 20f, SKIN_Y + 6f)
        p.style = Paint.Style.FILL
        p.shader = vgrad(CANAL_X - 152f, 0f, CANAL_X + 152f, 0f,
            Color.parseColor("#A8534D"), Color.parseColor("#E4A79F"), Color.parseColor("#9E4A45"))
        c.drawPath(inn, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f
        p.color = Color.parseColor("#7C2F2C"); p.alpha = 115; c.drawPath(inn, p); p.alpha = 255

        // শ্লেষ্মা + রক্তনালী + ভাঁজ
        val mu = canalPath(120f, 22f, TOP_Y + 28f, SKIN_Y + 6f)
        p.style = Paint.Style.FILL
        p.shader = vgrad(CANAL_X - 120f, 0f, CANAL_X + 120f, 0f,
            Color.parseColor("#9E2F35"), Color.parseColor("#E06C68"), Color.parseColor("#96292F"))
        c.drawPath(mu, p); p.shader = null
        val s3 = c.save(); c.clipPath(mu)
        p.style = Paint.Style.STROKE; p.color = Color.parseColor("#7E1F25"); p.alpha = 128
        for (v in vessels) { p.strokeWidth = v[4]; c.drawLine(v[0], v[1], v[2], v[3], p) }
        p.alpha = 255
        for (dx in intArrayOf(-66, -44, -22, 0, 22, 44, 66)) {
            p.color = Color.parseColor("#7E2126"); p.strokeWidth = 3.2f; p.alpha = 107
            val pp = Path().apply {
                moveTo(CANAL_X + dx, TOP_Y + 40f)
                quadTo(CANAL_X + dx * 1.25f, TOP_Y + 220f, CANAL_X + dx * 0.22f, SKIN_Y)
            }
            c.drawPath(pp, p)
            p.color = Color.parseColor("#F0938C"); p.strokeWidth = 1.4f; p.alpha = 90
            c.drawPath(pp, p)
        }
        p.alpha = 255
        c.restoreToCount(s3)

        // ফাঁকা নালী
        val lu = canalPath(84f, 10f, TOP_Y + 34f, SKIN_Y + 8f)
        p.style = Paint.Style.FILL
        p.shader = vgrad(0f, TOP_Y, 0f, SKIN_Y,
            Color.parseColor("#75302C"), Color.parseColor("#4A1B19"), Color.parseColor("#240C0B"))
        c.drawPath(lu, p); p.shader = null

        // ভেজা আভা
        blurOval(c, CANAL_X - 40f, TOP_Y + 150f, 26f, 90f, Color.parseColor("#FFD9CF"), 40, 18f)
        blurOval(c, CANAL_X + 52f, TOP_Y + 120f, 16f, 70f, Color.WHITE, 26, 18f)

        // dentate line
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f
        p.color = Color.parseColor("#6E2024"); p.alpha = 140
        p.pathEffect = android.graphics.DashPathEffect(floatArrayOf(9f, 7f), 0f)
        val dl = Path().apply {
            moveTo(CANAL_X - 58f, INT_Y + 2f); quadTo(CANAL_X, INT_Y + 10f, CANAL_X + 58f, INT_Y + 2f)
        }
        c.drawPath(dl, p); p.pathEffect = null; p.alpha = 255

        // শরীরের বাইরের অংশ (চামড়ার নিচে)
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#E7DAD2")
        c.drawRect(0f, SKIN_Y + 6f, VW, VH, p)
        blurRect(c, 0f, SKIN_Y + 6f, VW, SKIN_Y + 32f, Color.parseColor("#B99A8B"), 71, 10f)

        // মলদ্বারের মুখ
        blurOval(c, CANAL_X, SKIN_Y + 8f, 40f, 13f, Color.parseColor("#3B1A18"), 128, 8f)
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#22090A")
        c.drawOval(RectF(CANAL_X - 18f, SKIN_Y - 1f, CANAL_X + 18f, SKIN_Y + 11f), p)

        // উপরের নরম আলো
        blurOval(c, VW * 0.32f, 60f, 380f, 150f, Color.WHITE, 36, 26f)
    }

    /**
     * 🖼️🔒 V783 — **ছবি ২: পাশ থেকে কাটা** (TK-এর বাছাই "১ অথবা ২")।
     * মলাশয় উপরে, নিচে বেঁকে মলদ্বার-নালী, দু'পাশে হলুদ চর্বি।
     * ⛔ ছবি ১-এর কোড এক অক্ষরও ছোঁয়া হয়নি — এটা আলাদা ফাংশন।
     */
    private fun drawBodySide(c: Canvas) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.color = Color.parseColor("#FDF6EA"); c.drawRect(0f, 0f, VW, VH, p)
        // চর্বির পটভূমি
        p.shader = vgrad(0f, 60f, 0f, SKIN_Y + 6f,
            Color.parseColor("#FBEFC0"), Color.parseColor("#F0DE9C"))
        c.drawRect(0f, 60f, VW, SKIN_Y + 6f, p); p.shader = null
        // চর্বির কোষ (দু'পাশে)
        for (f in fatCells) {
            val x = f[0]; val y = f[1]
            if (x > 300f && x < 600f) continue
            p.style = Paint.Style.FILL
            p.shader = RadialGradient(x - f[2] * 0.3f, y - f[2] * 0.3f, f[2] * 1.6f,
                intArrayOf(Color.parseColor("#FFF3B8"), Color.parseColor("#E8CB63")), null, Shader.TileMode.CLAMP)
            c.drawOval(RectF(x - f[2] * 1.6f, y - f[2] * 1.2f, x + f[2] * 1.6f, y + f[2] * 1.2f), p)
            p.shader = null
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.1f
            p.color = Color.parseColor("#D9B93F"); p.alpha = 200
            c.drawOval(RectF(x - f[2] * 1.6f, y - f[2] * 1.2f, x + f[2] * 1.6f, y + f[2] * 1.2f), p)
            p.alpha = 255
        }
        // রক্তনালী
        p.style = Paint.Style.STROKE; p.color = Color.parseColor("#4C79C8"); p.alpha = 190
        for (v in vessels) {
            if (v[0] > 300f && v[0] < 600f) continue
            p.strokeWidth = v[4] + 1.4f; c.drawLine(v[0], v[1], v[2], v[3], p)
        }
        p.alpha = 255

        fun tube(pts: FloatArray): Path {
            val path = Path()
            path.moveTo(pts[0], pts[1])
            path.cubicTo(pts[2], pts[3], pts[4], pts[5], pts[6], pts[7])
            path.lineTo(pts[6], SKIN_Y + 6f)
            path.lineTo(pts[8], SKIN_Y + 6f)
            path.lineTo(pts[8], pts[7])
            path.cubicTo(pts[10], pts[5], pts[10], pts[3], pts[12], pts[1])
            path.close(); return path
        }
        // বাইরের পেশি
        p.style = Paint.Style.FILL
        p.shader = vgrad(300f, 0f, 600f, 0f,
            Color.parseColor("#A94545"), Color.parseColor("#D07C77"), Color.parseColor("#A54242"))
        c.drawPath(tube(floatArrayOf(330f, 40f, 300f, 180f, 300f, 286f, 372f, 368f, 528f, 0f, 600f, 0f, 570f)), p)
        p.shader = null
        // ভিতরের পেশি
        p.shader = vgrad(340f, 0f, 560f, 0f,
            Color.parseColor("#C0605F"), Color.parseColor("#F0AFA6"), Color.parseColor("#BC5A59"))
        c.drawPath(tube(floatArrayOf(366f, 52f, 340f, 182f, 340f, 288f, 400f, 370f, 500f, 0f, 560f, 0f, 534f)), p)
        p.shader = null
        // শ্লেষ্মা
        p.shader = vgrad(378f, 0f, 522f, 0f,
            Color.parseColor("#C6555B"), Color.parseColor("#F2A8A6"), Color.parseColor("#C25258"))
        c.drawPath(tube(floatArrayOf(398f, 64f, 378f, 188f, 378f, 292f, 424f, 372f, 476f, 0f, 522f, 0f, 502f)), p)
        p.shader = null
        // ফাঁকা নালী
        p.shader = vgrad(0f, 74f, 0f, SKIN_Y,
            Color.parseColor("#8E4340"), Color.parseColor("#5A2523"))
        c.drawPath(tube(floatArrayOf(424f, 74f, 408f, 192f, 408f, 296f, 442f, 374f, 458f, 0f, 492f, 0f, 476f)), p)
        p.shader = null
        // dentate line
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f
        p.color = Color.parseColor("#8E3438"); p.alpha = 150
        p.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 8f), 0f)
        c.drawLine(424f, 352f, 476f, 352f, p); p.pathEffect = null; p.alpha = 255
        // চামড়া
        p.style = Paint.Style.FILL
        p.shader = vgrad(0f, SKIN_Y - 30f, 0f, SKIN_Y + 6f,
            Color.parseColor("#F8E3D6"), Color.parseColor("#D9A38E"))
        c.drawRect(0f, SKIN_Y - 30f, 372f, SKIN_Y + 6f, p)
        c.drawRect(528f, SKIN_Y - 30f, VW, SKIN_Y + 6f, p)
        p.shader = null
        p.color = Color.parseColor("#EFE4DA")
        c.drawRect(0f, SKIN_Y + 6f, VW, VH, p)
        blurOval(c, S_CX, SKIN_Y + 8f, 44f, 13f, Color.parseColor("#3B1A18"), 115, 8f)
        blurOval(c, VW * 0.3f, 70f, 340f, 130f, Color.WHITE, 30, 24f)
    }

    private fun blurOval(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, col: Int, alpha: Int, blur: Float) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.color = col; p.alpha = alpha
        try { p.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL) } catch (_: Throwable) { }
        c.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), p)
        p.maskFilter = null; p.alpha = 255
    }

    private fun blurRect(c: Canvas, l: Float, t0: Float, r: Float, b: Float, col: Int, alpha: Int, blur: Float) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.color = col; p.alpha = alpha
        try { p.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL) } catch (_: Throwable) { }
        c.drawRect(l, t0, r, b, p)
        p.maskFilter = null; p.alpha = 255
    }

    // ─────────────────────────── সুতো ───────────────────────────
    /** আসল ক্ষারসূত্রের মতো পাকানো সুতো — হলুদ-বাদামি, সবুজ ওষুধের আভা। */
    private fun rope(c: Canvas, path: Path, w: Float = 10f, glow: Boolean = true) {
        if (glow) {
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = w * 2.8f
            p.color = Color.parseColor("#5BE58C"); p.alpha = 77
            try { p.maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL) } catch (_: Throwable) { }
            c.drawPath(path, p); p.maskFilter = null; p.alpha = 255
        }
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = w + 5f; p.color = Color.parseColor("#4A2E08"); p.alpha = 102
        c.drawPath(path, p); p.alpha = 255
        p.strokeWidth = w
        p.shader = vgrad(0f, SKIN_Y - 200f, 0f, SKIN_Y + 120f,
            Color.parseColor("#F2CC63"), Color.parseColor("#C08820"), Color.parseColor("#8B5D10"))
        c.drawPath(path, p); p.shader = null
        // পাকানো দাগ
        p.strokeWidth = w * 0.92f; p.strokeCap = Paint.Cap.BUTT
        p.color = Color.parseColor("#7A5210"); p.alpha = 140
        p.pathEffect = android.graphics.DashPathEffect(floatArrayOf(2.5f, 6f), 0f)
        c.drawPath(path, p)
        p.strokeWidth = w * 0.30f; p.color = Color.parseColor("#FFF0BC"); p.alpha = 190
        p.pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 7f), 2f)
        c.drawPath(path, p)
        p.pathEffect = null; p.alpha = 255
    }

    private fun knot(c: Canvas, kx: Float, ky: Float) {
        blurOval(c, kx, ky + 4f, 17f, 11f, Color.parseColor("#2A1B04"), 90, 6f)
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.shader = vgrad(kx - 13f, ky - 13f, kx + 13f, ky + 13f,
            Color.parseColor("#F2CC63"), Color.parseColor("#B37D18"))
        c.drawCircle(kx, ky, 13f, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f; p.color = Color.parseColor("#6E4A0C")
        c.drawCircle(kx, ky, 13f, p)
        val tail = Path().apply {
            moveTo(kx, ky); rQuadTo(-30f, 24f, -62f, 17f)
            moveTo(kx, ky); rQuadTo(-7f, 32f, -32f, 45f)
        }
        p.strokeWidth = 7.5f; p.strokeCap = Paint.Cap.ROUND
        p.shader = vgrad(kx - 60f, ky, kx + 10f, ky + 60f,
            Color.parseColor("#E8B23A"), Color.parseColor("#A96E18"))
        c.drawPath(tail, p); p.shader = null
    }

    private fun probe(c: Canvas, prog: Float) {
        val px = extX + (iX() - extX) * prog
        val py = extY + (iY() - extY) * prog
        val path = Path().apply { moveTo(extX - 64f, extY + 34f); lineTo(extX, extY); lineTo(px, py) }
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 15f; p.color = Color.parseColor("#2E3A46"); p.alpha = 90
        c.drawPath(path, p); p.alpha = 255
        p.strokeWidth = 10f
        p.shader = vgrad(0f, extY - 200f, 0f, extY + 40f,
            Color.WHITE, Color.parseColor("#96A5B4"), Color.parseColor("#5E6C7A"))
        c.drawPath(path, p); p.shader = null
        p.strokeWidth = 2.6f; p.color = Color.WHITE; p.alpha = 165; c.drawPath(path, p); p.alpha = 255
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#E8EEF4"); c.drawCircle(px, py, 6f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = Color.parseColor("#B9C4CF")
        c.drawOval(RectF(extX - 75f, extY + 26f, extX - 53f, extY + 42f), p)
    }

    /** ভিতরের মুখ — কোন ভিত্তি-ছবি চলছে তার উপর নির্ভর করে। */
    private fun iX() = if (base == 0) INT_X else S_INT_X
    private fun iY() = if (base == 0) INT_Y else S_INT_Y
    /** নালীর কেন্দ্র — ছবি অনুযায়ী। */
    private fun cX() = if (base == 0) CANAL_X else S_CX

    // ─────────────────────────── নালী ───────────────────────────
    private fun tract(c: Canvas, healed: Float) {
        val ix = iX(); val iy = iY()
        val hx = extX + (ix - extX) * healed
        val hy = extY + (iy - extY) * healed
        if (healed < 1f) {
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 30f; p.color = Color.parseColor("#42160F"); p.alpha = 140
            try { p.maskFilter = BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL) } catch (_: Throwable) { }
            c.drawLine(hx, hy, ix, iy, p); p.maskFilter = null; p.alpha = 255
            p.strokeWidth = 21f; p.color = Color.parseColor("#5E241B"); c.drawLine(hx, hy, ix, iy, p)
            p.strokeWidth = 17f
            p.shader = RadialGradient((hx + ix) / 2, (hy + iy) / 2, 120f,
                intArrayOf(Color.parseColor("#8E3A2E"), Color.parseColor("#C97F70")), null, Shader.TileMode.CLAMP)
            c.drawLine(hx, hy, ix, iy, p); p.shader = null
        }
        if (healed > 0f) {
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 10f; p.color = Color.parseColor("#C08A7C"); c.drawLine(extX, extY, hx, hy, p)
            p.strokeWidth = 4f; p.color = Color.parseColor("#EFCDC2"); p.alpha = 205
            c.drawLine(extX, extY, hx, hy, p); p.alpha = 255
        }
    }

    private fun loopPath(fromX: Float, fromY: Float, kx: Float, ky: Float): Path = Path().apply {
        moveTo(fromX, fromY); lineTo(iX(), iY())
        quadTo(cX() - 16f, iY() + 26f, cX(), iY() + 52f)
        lineTo(cX(), SKIN_Y + 10f)
        quadTo(cX() - 12f, SKIN_Y + 90f, kx, ky)
        quadTo((fromX + kx) / 2 - 18f, ky - 8f, fromX - 12f, fromY + 52f)
        lineTo(fromX, fromY)
    }

    private fun drawFistula(c: Canvas) {
        when (step) {
            0 -> { tract(c, 0f); dot(c, extX, extY, "#C62828"); dot(c, iX(), iY(), "#7A3FD0") }
            1 -> { tract(c, 0f); probe(c, t); dot(c, extX, extY, "#C62828"); dot(c, iX(), iY(), "#7A3FD0") }
            2 -> {
                tract(c, 0f); probe(c, 1f)
                val d = Path().apply {
                    moveTo(extX - 52f + 104f * t, extY + 30f - 16f * t); lineTo(extX, extY)
                    lineTo(extX + (iX() - extX) * t, extY + (iY() - extY) * t)
                }
                rope(c, d)
            }
            3 -> {
                tract(c, 0f)
                val kx = (extX + cX()) / 2 - 56f; val ky = SKIN_Y + 114f
                rope(c, loopPath(extX, extY, kx, ky)); knot(c, kx, ky)
            }
            4 -> {
                val h = 0.12f + 0.55f * t
                val hx = extX + (iX() - extX) * h; val hy = extY + (iY() - extY) * h
                val kx = (hx + cX()) / 2 - 56f; val ky = SKIN_Y + 112f
                tract(c, h); rope(c, loopPath(hx, hy, kx, ky)); knot(c, kx, ky)
            }
            else -> { tract(c, 1f); dot(c, extX, extY, "#E9C9C4") }
        }
    }

    // ─────────────────────────── ফিশার ───────────────────────────
    /** ফাটলের নিচের মাথা ডাক্তার ছুঁয়ে বসান; উপরের মাথা তার থেকেই হিসেব হয়। */
    private val fisTopX get() = fisX - 42f
    private val fisTopY get() = (fisY - 174f).coerceAtLeast(TOP_Y + 80f)
    private val fisBotX get() = fisX
    private val fisBotY get() = fisY
    private fun fisPath(): Path = Path().apply {
        moveTo(fisTopX, fisTopY)
        quadTo((fisTopX + fisBotX) / 2 - 8f, (fisTopY + fisBotY) / 2, fisBotX, fisBotY)
    }

    private fun drawFissure(c: Canvas) {
        val d = fisPath()
        // sentinel tag (বাইরের ছোট মাংস)
        if (step < 4) {
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
            p.color = Color.parseColor("#D8A08C")
            val tag = Path().apply {
                moveTo(fisBotX - 6f, SKIN_Y + 4f); rQuadTo(-14f, 14f, -4f, 26f)
                rQuadTo(12f, 10f, 22f, -2f); rQuadTo(6f, -12f, -4f, -24f); close()
            }
            c.drawPath(tag, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = Color.parseColor("#B87B67")
            c.drawPath(tag, p)
        }
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        when (step) {
            0 -> {
                p.strokeWidth = 15f; p.color = Color.parseColor("#3A100D"); p.alpha = 128
                try { p.maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL) } catch (_: Throwable) { }
                c.drawPath(d, p); p.maskFilter = null; p.alpha = 255
                p.strokeWidth = 12f; p.color = Color.parseColor("#7E1A12"); c.drawPath(d, p)
                p.strokeWidth = 4.6f; p.color = Color.parseColor("#FBEFE2"); p.alpha = 215
                c.drawPath(d, p); p.alpha = 255
                dot(c, fisTopX, fisTopY, "#C62828")
            }
            1 -> {
                p.strokeWidth = 12f; p.color = Color.parseColor("#7E1A12"); c.drawPath(d, p)
                p.strokeWidth = 4.6f; p.color = Color.parseColor("#FBEFE2"); c.drawPath(d, p)
                val px = fisBotX + 70f - 70f * t; val py = fisBotY + 70f - 58f * t
                p.style = Paint.Style.STROKE; p.strokeWidth = 7f
                p.shader = vgrad(px, py, px + 60f, py + 50f, Color.WHITE, Color.parseColor("#7C8B99"))
                c.drawLine(px + 52f, py + 42f, px, py, p); p.shader = null
                if (t > 0.85f) blurOval(c, fisBotX - 4f, fisBotY - 14f, 26f, 20f,
                    Color.parseColor("#BFD8E8"), 90, 8f)
            }
            2 -> {
                p.strokeWidth = 12f; p.color = Color.parseColor("#7E1A12"); c.drawPath(d, p)
                val cx = fisTopX + 40f - 30f * t; val cy = fisTopY + 60f - 50f * t
                p.strokeWidth = 6f; p.color = Color.parseColor("#C9A96B")
                c.drawLine(cx + 92f, cy + 78f, cx + 10f, cy + 8f, p)
                p.style = Paint.Style.FILL; p.color = Color.parseColor("#EFE3C9")
                c.drawOval(RectF(cx - 13f, cy - 10f, cx + 13f, cy + 10f), p)
                p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = 12f * t; p.color = Color.parseColor("#3B2A0C"); c.drawPath(d, p)
                p.strokeWidth = 5f * t; p.color = Color.parseColor("#6B4E12"); p.alpha = 205
                c.drawPath(d, p); p.alpha = 255
            }
            3 -> {
                p.strokeWidth = 12f; p.color = Color.parseColor("#8E2A22"); c.drawPath(d, p)
                p.strokeWidth = 5.5f; p.color = Color.parseColor("#C4564A"); c.drawPath(d, p)
                p.strokeWidth = 12f * (1f - t); p.color = Color.parseColor("#3B2A0C"); p.alpha = 215
                c.drawPath(d, p); p.alpha = 255
                p.style = Paint.Style.FILL; p.color = Color.parseColor("#BFE2F0"); p.alpha = 205
                for (k in 0 until 4) {
                    val yy = fisTopY + 40f + k * 34f + t * 40f
                    c.drawOval(RectF(fisTopX + 12f + k * 4f, yy - 7f, fisTopX + 21f + k * 4f, yy + 7f), p)
                }
                p.alpha = 255
            }
            else -> {
                p.strokeWidth = 12f - 5f * t; p.color = Color.parseColor("#C9847A"); c.drawPath(d, p)
                p.strokeWidth = 5f - 2.5f * t; p.color = Color.parseColor("#EFCEC4"); c.drawPath(d, p)
            }
        }
    }

    // ─────────────────────────── পাইলস ───────────────────────────

    private fun drawPiles(c: Canvas) {
        val swell = when (step) { 0, 1 -> 0.45f; 2 -> 0.45f + 0.55f * t; else -> 1f }
        val fallen = if (step == 4) t else 0f
        val cy = pileY + 70f * fallen * fallen
        val rx = 26f + 16f * swell; val ry = 34f + 18f * swell
        val op = ((1f - fallen) * 255).toInt().coerceIn(0, 255)
        if (op > 6) {
            blurOval(c, pileX + 6f, cy + 6f, rx, ry, Color.parseColor("#3A0E13"), (op * 0.45f).toInt(), 8f)
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
            p.color = Color.parseColor("#8E2340"); p.alpha = op
            c.drawOval(RectF(pileX - rx, cy - ry, pileX + rx, cy + ry), p)
            p.color = Color.parseColor("#B23A55"); p.alpha = (op * 0.85f).toInt()
            c.drawOval(RectF(pileX - rx * 0.82f, cy - ry * 0.82f, pileX + rx * 0.82f, cy + ry * 0.82f), p)
            blurOval(c, pileX - rx * 0.3f, cy - ry * 0.34f, rx * 0.34f, ry * 0.28f,
                Color.parseColor("#FFD3D8"), (op * 0.35f).toInt(), 7f)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f
            p.color = Color.parseColor("#66101F"); p.alpha = (op * 0.45f).toInt()
            for (k in 0 until 6) {
                val x = pileX - rx * 0.7f + k * rx * 0.28f
                val pp = Path().apply { moveTo(x, cy - ry * 0.7f); rQuadTo(6f, ry * 0.7f, 0f, ry * 1.3f) }
                c.drawPath(pp, p)
            }
            p.alpha = 255
        } else {
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
            p.color = Color.parseColor("#C08A7C"); p.alpha = 180
            c.drawOval(RectF(pileX - 6f, pileY - 28f, pileX + 22f, pileY - 12f), p); p.alpha = 255
        }
        if (step == 1) {
            // ইনজেকশনের সুচ
            val px = pileX + 130f - 120f * t; val py = cy + 120f - 110f * t
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 6f
            p.shader = vgrad(px, py, px + 90f, py + 80f, Color.WHITE, Color.parseColor("#7C8B99"))
            c.drawLine(px + 86f, py + 76f, px, py, p); p.shader = null
            p.style = Paint.Style.FILL; p.color = Color.parseColor("#DCEAF6"); p.alpha = 235
            c.drawRect(px + 84f, py + 66f, px + 150f, py + 96f, p); p.alpha = 255
        }
        if (step >= 3 && op > 6) {
            val by = cy + ry * 0.92f
            p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE
            p.strokeWidth = 9f; p.color = Color.parseColor("#4A2E08"); p.alpha = 115
            c.drawOval(RectF(pileX - rx * 0.72f, by - 7f, pileX + rx * 0.72f, by + 7f), p); p.alpha = 255
            p.strokeWidth = 6.5f
            p.shader = vgrad(0f, by - 20f, 0f, by + 20f,
                Color.parseColor("#F2CC63"), Color.parseColor("#A96E18"))
            c.drawOval(RectF(pileX - rx * 0.72f, by - 7f, pileX + rx * 0.72f, by + 7f), p); p.shader = null
            knot(c, pileX + rx * 0.72f, by)
        }
    }

    private fun dot(c: Canvas, x: Float, y: Float, hex: String) {
        blurOval(c, x, y, 12f, 12f, Color.BLACK, 56, 5f)
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        p.color = Color.parseColor(hex); c.drawCircle(x, y, 9f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3.2f; p.color = Color.WHITE
        c.drawCircle(x, y, 9f, p)
    }
}
