package com.tkbiswas.pilesclinic.clinical

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * 🔵🔒 V558 (TK-অনুমোদিত) — **রোগের ছবির উপরে আঁকার পর্দা**
 *
 * TK-এর দুটো কথা এখানেই বাস্তব হয়:
 *   ১. *"যে কোন ফটোর উপর যেন সেই কাজটা করতে পারে"* → ছবিটা বাইরে থেকে
 *      বসানো হয় (`setPicture`), তাই বইয়ের ছবি বা চেম্বারের আসল ছবি —
 *      যেটাই হোক, একই ভাবে কাজ করে।
 *   ২. *"মাংসের উপরে আঙুল দিয়ে টান দিলে যেন মাংস বেড়ে যায়"* → V571 থেকে
 *      ছবির পিক্সেল ঠেলা হয় না; TK-এর পাঠানো ছবির মতো একটা **ফোঁটা-আকৃতির
 *      বেগুনি মাংসপিণ্ড আঁকা হয়** (`drawLump`) — গোড়া সরু, মাথা গোল, উপরে
 *      আলো, গায়ে দানা-দানা। টান যত বড়, মাংস তত বড়, আর যেদিকে টেনেছেন
 *      সেদিকেই বেরোয়।
 *
 * 📐 সব দাগ ছবির **শতকরা** মাপে জমা থাকে (`AnatomyModel`), পিক্সেলে নয় —
 *   তাই ছোট ফোন, বড় ফোন আর ওয়েবে দাগ একই জায়গায় বসে।
 *
 * ⛔ কোনো নতুন কলাম বা SQL লাগেনি — লেখাটা চেকআপের সাথেই জমা হয়।
 */
class AnatomyView(context: Context) : View(context) {

    /** কোন কাজটা এখন চলছে — উপরের বোতাম থেকে বদলায়। */
    /* 🔵 V585 (২৩.০৮.২০২৬, TK-নির্দেশ *"এটাতো অটোমেটিক্যালি হওয়ার কথা"*) —
       নতুন হাতিয়ার **CENTRE**: ছবির পায়ুপথের মাঝখানে একবার ছুঁয়ে দিলে ওই
       ছবির ঘড়ির কেন্দ্র জমা হয়। তারপর প্রতিটা চিহ্নের o'clock নিজে হিসাব হয়।
       ⛔ আগের সাতটা হাতিয়ারের একটাও বদলায়নি, শুধু একটা যোগ হলো। */
    /* 🔴🔒 V793 — FISSURE যোগ হলো (TK: "আঙুল দিয়ে যেখানে ঘষা দিব সেখানে
       যেন দাগ হয়ে যায়")। ⛔ পুরোনো সাতটা হাতিয়ার অপরিবর্তিত। */
    enum class Tool { BULGE, PILE, TRACT, RING, ARROW, PEN, ERASE, CENTRE, FISSURE }

    var tool: Tool = Tool.BULGE
    /* 🔵 V585 — আগে এখানে পপ-আপে বাছা লেখাটা জমা থাকত আর **প্রতিটা** চিহ্নে
       সেটাই বসত (TK: *"যেখানেই থাকবে চারটা কেন বাঁচবে"*)। ঘরটা **মোছা হয়নি** —
       পুরোনো কোনো ডাক থাকলে ভাঙবে না — কিন্তু এখন আর ব্যবহার হয় না। */
    var pileLabel: String = ""                 // ⛔ V585-এর পর ব্যবহার হয় না
    /** এই ছবির ঘড়ির কেন্দ্র (শতাংশে), জানা না থাকলে null — তখন o'clock লেখা পড়ে না। */
    var clockCentre: Pair<Double, Double>? = null
    /** ডাক্তার কেন্দ্র ছুঁয়ে দিলে ডাকা হয় (Activity সেটা জমা করে)। */
    var onCentreSet: ((Double, Double) -> Unit)? = null
    /** 🟢🔒 V626 — "ফোলান" ছবির ভুল জায়গায় (কেন্দ্রের অনেক দূরে) চাপা হলে
     *  একবার ডাকা হয়, যাতে Activity একটা মনে করিয়ে দেওয়ার Toast দেখাতে পারে। */
    var onBulgeBlocked: (() -> Unit)? = null

    /* 🔵🔒 V587 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) — **ক্ষারসূত্রের ধাপ
       রোগীকে দেখানোর মোড।** চালু থাকলে বাছা চিহ্নটা ধাপ অনুযায়ী আঁকা হয়
       (ইনজেকশন · ফোলা · সুতো · কেটে পড়া)।
       ⛔ এটা **শুধু দেখার** — `marks` তালিকায় কিছু যোগ/বাদ হয় না, কিছু সেভও
          হয় না। মোড বন্ধ করলেই ছবিটা হুবহু আগের মতো। */
    var ksOn = false
    /** 🔵 V898 — ছবির ফাঁকা জায়গায় এক টোকা (বোতাম লুকানো/ফেরানো)। */
    var onKsBlankTap: (() -> Unit)? = null
    /** 🔵 V898 — বোতাম লুকানো থাকলে সত্যি হয়; তখন ক্ষারসূত্রের মোডেও আঁকা যায়। */
    var ksDrawAllowed: Boolean = false
    var ksIndex = -1              // কোন চিহ্নের উপরে চলছে (marks-এর ক্রম)
    var ksStep = 0                // KsharSutraAnim-এর ধাপ
    var ksT = 0f                  // ওই ধাপের অগ্রগতি ০…১
    /** KS মোডে কোনো চিহ্ন ছুঁলে — Activity বেছে নেয়। */
    var onKsPick: ((Int) -> Unit)? = null
    /* 🟢🔒 V589 (২৩.০৮.২০২৬, TK-নির্দেশ) — *"ইনজেকশন — ডাক্তার যেখানে চাইবে
       সেখানে অ্যানিমেশনটা দেখাবে"* ও *"ক্ষার সূত্র যেখানে থাকবে সেখানে বেঁধে
       রাখবে"*। ইনজেকশনের ধাপে বা সুতোর ধাপে ছবিতে ছুঁলে জায়গাটা এখানে বসে।
       ⛔ মাপগুলো **মাংসের নিজের ভগ্নাংশে**, তাই জুম/ছোট-বড়তে সরে যায় না।
       ⛔ কিছুই সেভ হয় না — পর্দা বন্ধ করলে ডিফল্টেই ফেরে (গোড়ায় বাঁধা)। */
    var ksInjAlong = 0.55f
    var ksInjAcross = 0.10f
    var ksTieAt = KsharSutraAnim.TIE_AT_BASE
    /* 🔴🔒 V793 (TK-নির্দেশ) — *"কোন কোন পেশেন্টের তো চার সপ্তাহেও ঠিক হয়ে
       যেতে পারে"* ⇒ সপ্তাহের সংখ্যা ডাক্তার ➖ ➕ দিয়ে বসান। */
    var ksWeeks = KsharSutraAnim.WEEKS_DEFAULT
    /** ছুঁয়ে জায়গা বসানো হলে Activity-কে জানানো হয় (লেখাটা বদলায়)। */
    var onKsSpot: (() -> Unit)? = null
    var onChanged: (() -> Unit)? = null        // কিছু আঁকা হলেই ডাকা হয়
    /* 🔵 V600 (২৩.০৮.২০২৬, TK-অনুমোদিত "Option 1") — ছবি বসলেই/বদলালেই ডাকা
       হয়, নতুন ছবির আসল প্রস্থ/উচ্চতা দিয়ে। Activity এটা দিয়ে বাক্সের উচ্চতা
       ছবির নিজের অনুপাতে ঠিক করে — তাই কোনো ছবিতেই দুই পাশ ফাঁকা থাকে না,
       ছবিও বেঁকে যায় না, আর প্রতিটা ছবির আলাদা আকৃতিতেও ঠিক কাজ করে। */
    var onBaseImageSet: ((Bitmap?) -> Unit)? = null

    private val density = context.resources.displayMetrics.density
    private var base: Bitmap? = null           // আসল ছবি — কখনো বদলায় না

    private val marks = mutableListOf<AnatomyModel.Mark>()
    private var picKey: String = ""
    private var note: String = ""

    /** ছবির কোথায় আঁকা হচ্ছে — পর্দার ভিতরে ছবিটা যতটুকু জায়গা নেয়। */
    private val dst = RectF()

    /** 🟢🔒 V631 — ছবির উচ্চতা÷চওড়া (`tractCm()`-এর অনুপাতের হুবহু একই
     *  সংজ্ঞা) — দূরত্ব মাপার সময় x/y-কে সমান মাপে আনতে। ছবি এখনো
     *  বসানো/মাপা না হলে নিরাপদ ডিফল্ট ১.০ (বর্গাকার ধরে)। */
    private fun imgAspect(): Double =
        if (dst.width() > 0f) (dst.height() / dst.width()).toDouble() else 1.0

    /**
     * 🔵🔒 V569 (২২.০৮.২০২৬, TK-নির্দেশ) — *"যে ফটোটা আমি সিলেক্ট করব সেটা যেন
     * সম্পূর্ণ স্ক্রিন জুড়ে আসে ... ফটো সাইজ আরো বড় হবে ... বড় করে জুম করে"*।
     *
     * `fillScreen` — পুরো পর্দায় ছবিটা **গোটা পর্দা ভরে** বসে (দুই পাশ একটু
     *   কাটা যায়), ছোট বোর্ডে আগের মতোই পুরো ছবিটা ধরানো হয়।
     * `allowZoom` — দু'আঙুলে ছোট-বড় করা ও সরানো (শুধু পুরো পর্দায়)।
     *
     * ⛔ দাগ জমা থাকে ছবির **শতকরা** হিসেবেই, আর ছোঁয়ার হিসাব হয় `dst` ধরে —
     *    তাই জুম করলে বা সরালে দাগ ছবির ঠিক সেই জায়গাতেই বসে, আর জমা হওয়ার
     *    লেখা এক অক্ষরও বদলায় না।
     */
    var fillScreen: Boolean = false
    var allowZoom: Boolean = false

    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var twoFinger = false
    private var lastMidX = 0f
    private var lastMidY = 0f

    fun resetZoom() { zoom = 1f; panX = 0f; panY = 0f; invalidate() }

    /** ➕ / ➖ বোতাম — যাঁরা দু'আঙুল ব্যবহার করতে চান না তাঁদের জন্য। */
    fun zoomBy(k: Float) {
        if (!allowZoom) return
        zoom = (zoom * k).coerceIn(1f, 6f)
        if (zoom <= 1.001f) { panX = 0f; panY = 0f }
        clampPan(); invalidate()
    }

    private val scaleDetector = android.view.ScaleGestureDetector(context,
        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: android.view.ScaleGestureDetector): Boolean {
                if (!allowZoom) return false
                zoom = (zoom * d.scaleFactor).coerceIn(1f, 6f)
                if (zoom <= 1.001f) { panX = 0f; panY = 0f }
                clampPan(); invalidate(); return true
            }
        })

    /** দু'বার ছুঁলে আগের মাপে ফিরে যায় — হারিয়ে যাওয়ার ভয় থাকে না। */
    private val tapDetector = android.view.GestureDetector(context,
        object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!allowZoom) return false
                resetZoom(); return true
            }
        })

    /** ছবিটা যেন পর্দা ছেড়ে বেরিয়ে না যায়। */
    private fun clampPan() {
        val b = base ?: return
        val vw = width.toFloat(); val vh = height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val sc = baseScale(b, vw, vh) * zoom
        val w = b.width * sc; val h = b.height * sc
        val ox = Math.max(0f, (w - vw) / 2f)
        val oy = Math.max(0f, (h - vh) / 2f)
        panX = panX.coerceIn(-ox, ox)
        panY = panY.coerceIn(-oy, oy)
    }

    private fun baseScale(img: Bitmap, vw: Float, vh: Float): Float =
        if (fillScreen) Math.max(vw / img.width, vh / img.height)
        else Math.min(vw / img.width, vh / img.height)

    /** দু'আঙুল শুরু হলে চলতি আঁকাটা বাতিল — নইলে জুম করতে গিয়ে দাগ পড়ে যেত। */
    private fun cancelDraw() {
        val s = startPct
        if (s != null && marks.isNotEmpty()) {
            val last = marks[marks.size - 1]
            val sameStart = last.x == s[0].toDouble() && last.y == s[1].toDouble()
            val undoable = (tool == Tool.RING && last.kind == AnatomyModel.KIND_RING) ||
                           (tool == Tool.ARROW && last.kind == AnatomyModel.KIND_ARROW) ||
                           (tool == Tool.BULGE && last.kind == AnatomyModel.KIND_BULGE && sameStart)
            if (undoable) { marks.removeAt(marks.size - 1) }
        }
        startPct = null
        livePts.clear()
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    /* ═══════════════════════════════════════════════════════════════════════
       🔴🔴🔒 V793 (২৮.০৮.২০২৬, TK-নির্দেশ ও ডেমো-প্রুফ অনুমোদনের পরে) —
       **আসল রোগীর ছবিতে তার নিজের মাংসটাই ফুলবে।**

       TK-এর কথা (হুবহু): *"এখানে যে বাস্তব রোগীর বাস্তব ফটো তোলা হয়েছে,
       সেখানে মাংসটা টানলে যেন প্রকৃত এই রোগের মাংসটাই ফুলে যায়। আলাদা কোন
       অ্যানিমেশন ওখানে যাবে না। … আর যেগুলো হাতে আঁকা ছবি সেখানে
       অ্যানিমেশন টাইপের ফুলবে।"*

       ─── ইতিহাস (সৎভাবে) ────────────────────────────────────────────────
       V558-এ পিক্সেল-ফোলানো ছিল, কিন্তু V571-এ TK বলেছিলেন *"যথাযথ মিল
       খাচ্ছে না"* — তখন সেটা তুলে দিয়ে **আঁকা মাংস** (`drawLump`) বসানো
       হয়েছিল। এখন TK নিজেই আসল ছবির জন্য পিক্সেল-ফোলানো ফেরত চেয়েছেন,
       আর ডেমো-প্রুফ দেখে অনুমোদন করেছেন।

       ─── এখন কী হয় ─────────────────────────────────────────────────────
        • **আসল ছবি** (ডাক্তারের তোলা/গ্যালারির) ⇒ ছবির পিক্সেলই বাইরের দিকে
          ঠেলে দেওয়া হয় (`Canvas.drawBitmapMesh`) — আঁকা মাংস বসে **না**।
        • **অ্যাপের আঁকা ছবি** ⇒ আগের মতোই `drawLump()` — এক অক্ষরও বদলায়নি।

       ─── কেন এবার হালকা ও নিখুঁত ───────────────────────────────────────
       V558-এ প্রতিবার পুরো bitmap কপি করে পিক্সেল-লুপ চলত (ফোন আটকে যেত)।
       এখন **একটাও পিক্সেল ছোঁয়া হয় না** — শুধু ২৮×২৮ জালের কোণগুলো সরানো
       হয়, বাকিটা GPU করে। তাই টানার সময় আটকায় না, মেমরিও বাড়ে না।
       ⛔ কোনো জোড়া/চৌকো দাগ পড়ে না — জালটা মসৃণ।
       ⛔ ছবির বাইরে কিছু যায় না; ফোলার সীমা `AnatomyModel.BULGE_MAX`-ই।
       ═══════════════════════════════════════════════════════════════════ */
    private var baseIsPhoto = false
    private val meshN = 28                       // জালের ঘর (২৮×২৮)
    private val meshVerts = FloatArray((meshN + 1) * (meshN + 1) * 2)

    /** এই ছবিতে পিক্সেল-ফোলানো চলবে কি না। */
    private fun photoBulgeOn(): Boolean =
        baseIsPhoto && marks.any { it.kind == AnatomyModel.KIND_BULGE }

    /**
     * জালের প্রতিটা কোণ সরিয়ে ছবিটা আঁকা। প্রতিটা `bulge` চিহ্নের চারপাশে
     * ভিতরের বিন্দুগুলো কেন্দ্রের কাছ থেকে টেনে আনা হয় ⇒ ওই জায়গাটা ফুলে ওঠে।
     * ⛔ শুধু আঁকার সময়ের হিসাব — কোনো দাগ/সেভ বদলায় না।
     */
    private fun drawPhotoBulge(canvas: Canvas, img: Bitmap) {
        val w = dst.width(); val h = dst.height()
        var i = 0
        for (row in 0..meshN) {
            val fy = row.toFloat() / meshN
            for (col in 0..meshN) {
                val fx = col.toFloat() / meshN
                var x = fx * 100f          // শতকরা মাপে (দাগও একই মাপে)
                var y = fy * 100f
                for ((mi, m0) in marks.withIndex()) {
                    if (m0.kind != AnatomyModel.KIND_BULGE) continue
                    /* 🔴🔒 V793 — ধাপ চলার সময় **এই** মাংসটার ফোলা ধাপ অনুযায়ী
                       বাড়ে/কমে, তাই আসল ছবির চামড়াই ধাপে ধাপে ফুলে ওঠে
                       (TK: *"না সারালে … মাংস আরো বড়"*)। বাকিগুলো অপরিবর্তিত। */
                    val m = if (ksOn && mi == ksIndex) m0.copy(s = ksLumpStrength(m0)) else m0
                    val g = AnatomyModel.lumpGeom(m)
                    // ফোলার কেন্দ্র = মাংসের মাথা, ব্যাসার্ধ = তার দৈর্ঘ্যের একটু বেশি
                    val cx = (m.x + Math.cos(g.ang) * g.len * 0.55).toFloat()
                    val cy = (m.y + Math.sin(g.ang) * g.len * 0.55).toFloat()
                    val rad = (g.len * 1.35).toFloat()
                    if (rad <= 0.01f) continue
                    val dx = x - cx; val dy = y - cy
                    val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (d >= rad || d < 0.0001f) continue
                    val raw = if (m.s != 0.0) m.s else 0.45
                    val st = (if (raw < 0.0) 0.0
                              else if (raw > AnatomyModel.BULGE_MAX) AnatomyModel.BULGE_MAX
                              else raw).toFloat()
                    /* 🔴🔴🔒 V1256 (০৯.০৯.২০২৬, TK-রিপোর্ট ছবিসহ ও প্রুফ পাশ,
                       খাতার সারি ৩৭৯) — TK: *"ক্যামেরাতে তোলা ফটোতে পাইলসের
                       মাংস ফলানো যাচ্ছে না"*।

                       🔴 **আসল কারণ — অঙ্কটাই উল্টো বসানো ছিল (আমারই ভুল,
                          V793):** `meshVerts`-এ যে বিন্দুগুলো বসে সেগুলো ছবির
                          **কোথায় আঁকা হবে** (গন্তব্য), ছবির কোথা থেকে আসবে তা
                          নয়। গন্তব্যের বিন্দু কেন্দ্রের **দিকে** সরালে ওই
                          জায়গার ছবিটা চেপে যায় — অর্থাৎ মাংস **ছোট** হয়,
                          ফোলে না। উপরের পুরনো মন্তব্যটাই ভুল ছিল।
                          বেশি টানলে ব্যাসার্ধ ও জোর দুটোই বেড়ে গোটা ছবি
                          কেন্দ্রের দিকে ঘেঁটে যেত — TK-র ছবিতে ঠিক তাই।

                       ✅ **মিলিয়ে দেখা:** ক্লিনিকের একটা আসল ছবিতে এই একই
                          অঙ্ক চালিয়ে দেখা গেছে — টান যত বাড়ে মাংস তত **ছোট**
                          হয়, আর অনেক টানলে রেডিয়াল দাগে ঘেঁটে যায়। ঘাতটা
                          উল্টে (১/p) দিলে মাংস সত্যিই ফোলে, ছবি ঘাঁটে না।

                       ⇒ ঘাত এখন **১/p** — গন্তব্যের বিন্দু কেন্দ্র থেকে
                         **বাইরের দিকে** সরে, তাই কেন্দ্রের ছবিটা ছড়িয়ে বড়
                         হয় = আসল ফোলা।
                       ⛔ টানের সঙ্গে মাপ/জোরের হিসাব (`bulgeFromDrag` ·
                          `lumpGeom` · `BULGE_MAX`) এক অক্ষরও বদলায়নি।
                       ⛔ অ্যাপের আঁকা ছবির `drawLump()` পথ আলাদা, ছোঁয়া হয়নি। */
                    val p = 1f + st * 1.9f
                    val e = 1f / p
                    val f = Math.pow((d / rad).toDouble(), e.toDouble()).toFloat()
                    x = cx + dx * f; y = cy + dy * f
                }
                meshVerts[i++] = dst.left + x / 100f * w
                meshVerts[i++] = dst.top + y / 100f * h
            }
        }
        try {
            canvas.save()
            canvas.clipRect(dst)
            canvas.drawBitmapMesh(img, meshN, meshN, meshVerts, 0, null, 0, null)
            canvas.restore()
        } catch (_: Throwable) {
            canvas.drawBitmap(img, null, dst, null)   // গোলমাল হলে আগের মতোই
        }
    }

    init {
        /* 🔵 V571 — মাংসের নিচের নরম ছায়াটা (`setShadowLayer`) হার্ডওয়্যার
           আঁকায় পথের উপরে কাজ করে না, তাই এই পর্দাটা সফটওয়্যারে আঁকা হয়।
           ⚡ ভারী কিছু নয় — আগের পিক্সেল-ঠেলার হিসাবটাই বাদ গেছে, তাই
              সব মিলিয়ে এখন আগের চেয়ে হালকা। */
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    // ───────── বাইরে থেকে বসানো ও নেওয়া ─────────

    /** নতুন ছবি বসানো। আগের দাগ মুছে যায় — এক ছবির দাগ অন্য ছবিতে বসলে ভুল হত। */
    fun setPicture(key: String, resId: Int) {
        if (key == picKey && base != null) return
        picKey = key
        baseIsPhoto = false          // 🔴 V793 — অ্যাপের নিজের আঁকা ছবি
        clockCentre = AnatomyClock.centreOf(context, key)   // 🔵 V585
        marks.clear()
        resetZoom()
        base = try {
            val o = BitmapFactory.Options()
            o.inPreferredConfig = Bitmap.Config.ARGB_8888
            BitmapFactory.decodeResource(resources, resId, o)
        } catch (_: Throwable) { null } catch (_: OutOfMemoryError) { null }
        invalidate()
        onChanged?.invoke()
        onBaseImageSet?.invoke(base)
    }

    /**
     * 🔵 V573 — ডাক্তারের নিজের যোগ করা ছবি (ক্যামেরা/গ্যালারি) বসানো।
     * অ্যাপের ছবির মতোই কাজ করে; শুধু ছবিটা resource নয়, তৈরি করা bitmap।
     */
    fun setPictureBitmap(key: String, bmp: Bitmap?) {
        if (key == picKey && base != null) return
        picKey = key
        baseIsPhoto = true           // 🔴 V793 — ডাক্তারের তোলা আসল ছবি
        clockCentre = AnatomyClock.centreOf(context, key)   // 🔵 V585
        marks.clear()
        resetZoom()
        base = bmp
        invalidate()
        onChanged?.invoke()
        onBaseImageSet?.invoke(base)
    }

    /** ছবি বদলানো ছাড়াই শুধু ছবিটা বসানো — দাগ অক্ষত থাকে (রেকর্ড ফেরানোর সময়)। */
    fun setBaseBitmap(bmp: Bitmap?) {
        base = bmp
        invalidate()
        onBaseImageSet?.invoke(base)
    }

    fun load(saved: String?, resolve: (String) -> Int) {
        val b = AnatomyModel.parse(saved)
        note = b.note
        if (b.pic.isNotBlank()) {
            // ছবিটা ফোনে না থাকলেও নামটা ধরে রাখা হয় — নইলে পরের বার সেভ
            // করলে "কোন ছবির উপরে আঁকা হয়েছিল" সেই তথ্যটাই হারিয়ে যেত।
            picKey = b.pic
            clockCentre = AnatomyClock.centreOf(context, b.pic)   // 🔵 V585
            val id = resolve(b.pic)
            if (id != 0) {
                base = try { BitmapFactory.decodeResource(resources, id) } catch (_: Throwable) { null }
            }
        }
        marks.clear()
        marks.addAll(b.marks)
        invalidate()
        onBaseImageSet?.invoke(base)
    }

    /** 🔵 V585 — এখন কোন ছবির উপরে আঁকা হচ্ছে (কেন্দ্র জমা/পড়ার চাবি)। */
    fun picKeyNow(): String = picKey

    fun save(): String = AnatomyModel.format(AnatomyModel.Board(picKey, marks.toList(), note))

    fun setNote(t: String) { note = t }

    fun undo() {
        if (marks.isNotEmpty()) {
            marks.removeAt(marks.size - 1)
            invalidate(); onChanged?.invoke()
        }
    }

    fun clearMarks() {
        if (marks.isEmpty()) return
        marks.clear(); invalidate(); onChanged?.invoke()
    }

    fun hasPicture(): Boolean = base != null
    fun markCount(): Int = marks.size

    // ───────── আঙুল ─────────

    private var startPct: FloatArray? = null
    /** 🟢🔒 V626 — এই টানটা "ফোলান"-এর জন্য অনুমোদিত এলাকায় শুরু হয়েছে কি
     *  না, ACTION_DOWN-এই ঠিক হয়ে যায় (কেন্দ্র/শুরুর বিন্দু টানের মাঝপথে
     *  বদলায় না, তাই বারবার হিসাব করার দরকার নেই)। */
    private var bulgeAllowed: Boolean = true
    private var livePts = mutableListOf<Pair<Double, Double>>()

    /**
     * 🔵 V587 — KS মোডে ছোঁয়ার জায়গার সবচেয়ে কাছের চিহ্ন কোনটা।
     * শুধু **ফোলা ও নালী** ধরা হয় (এই দুটোতেই ক্ষারসূত্র হয়)। কিছু না পেলে −১।
     */
    fun ksNearestAt(xPct: Double, yPct: Double): Int {
        val aspect = imgAspect()   // 🟢🔒 V631 — একই aspect-সংশোধন এখানেও
        var best = -1; var bd = Double.MAX_VALUE
        for ((i, m) in marks.withIndex()) {
            val d: Double = when (m.kind) {
                AnatomyModel.KIND_BULGE -> {
                    val g = AnatomyModel.lumpGeom(m)
                    // গোড়া থেকে ডগা — মাঝ বরাবর সবচেয়ে কাছের দূরত্ব
                    val hx = m.x + Math.cos(g.ang) * g.len * 0.55
                    val hy = m.y + Math.sin(g.ang) * g.len * 0.55
                    Math.min(Math.hypot(xPct - m.x, (yPct - m.y) * aspect), Math.hypot(xPct - hx, (yPct - hy) * aspect))
                }
                // 🔴 V793 — ফাটলও ছুঁয়ে বাছা যায় (নালীর মতোই পথ ধরে)
                AnatomyModel.KIND_TRACT, AnatomyModel.KIND_FISSURE ->
                    m.pts.minOfOrNull { Math.hypot(xPct - it.first, (yPct - it.second) * aspect) } ?: Double.MAX_VALUE
                else -> Double.MAX_VALUE
            }
            if (d < bd) { bd = d; best = i }
        }
        return if (bd <= 18.0) best else -1
    }

    /**
     * 🟢🔒 V589 — ছোঁয়ার জায়গাটা **মাংসের নিজের মাপে** বদলে নেওয়া।
     * ইনজেকশনের ধাপ হলে সুচের ডগা, সুতোর ধাপ হলে বাঁধার জায়গা।
     * @return true হলে ছোঁয়াটা এখানেই কাজে লেগেছে (চিহ্ন বদলানো হবে না)।
     */
    private fun ksSetSpot(xPct: Double, yPct: Double): Boolean {
        val m = marks.getOrNull(ksIndex) ?: return false
        if (m.kind == AnatomyModel.KIND_TRACT) return false
        val inject = ksStep == KsharSutraAnim.LUMP_INJECT || ksStep == KsharSutraAnim.LUMP_SWELL
        val tie = ksStep == KsharSutraAnim.LUMP_TIE
        if (!inject && !tie) return false
        val g = AnatomyModel.lumpGeom(m)
        if (g.len <= 0.0 || g.wide <= 0.0) return false
        // ছবির শতাংশ → মাংসের নিজের অক্ষ (গোড়া শূন্য, ডগার দিকে ধনাত্মক)
        val dx = xPct - m.x
        val dy = yPct - m.y
        val along = dx * Math.cos(g.ang) + dy * Math.sin(g.ang)
        val across = -dx * Math.sin(g.ang) + dy * Math.cos(g.ang)
        if (inject) {
            ksInjAlong = KsharSutraAnim.clampInjAlong((along / g.len).toFloat())
            ksInjAcross = KsharSutraAnim.clampInjAcross((across / g.wide).toFloat())
        } else {
            ksTieAt = KsharSutraAnim.clampTie((along / g.len).toFloat())
        }
        invalidate()
        onKsSpot?.invoke()
        return true
    }

    /**
     * 🟢🔒 V589 (২৩.০৮.২০২৬, TK-নির্দেশ) — *"মাংসটা যখন কেটে পড়ে যাবে তখন
     * রিয়েল ফটোতেও যেন পরিষ্কার করে দেয়, যাতে পেশেন্ট সম্পূর্ণভাবে বুঝতে পারে
     * যে তার পাইলসের মাংসটা বেরিয়ে গেছে"*
     *
     * **কীভাবে:** আঁকা মাংসটার ঠিক বাইরের চারপাশ থেকে **আসল ছবির চামড়ার রং**
     * তুলে নেওয়া হয় (২৪টা জায়গা থেকে গড়), তারপর ওই রঙেরই একটা নরম ছোপ
     * মাংসের জায়গাটার উপরে বসানো হয় — কিনারায় মিলিয়ে যায়, তাই জোড়া বোঝা যায় না।
     *
     * ⛔ **আসল ছবিটা এক পিক্সেলও বদলায় না** — এটা শুধু পর্দায় আঁকা, উপরের
     *    স্তরে। মোড বন্ধ করলেই ছবিটা হুবহু আগের মতো, সেভও হয় না।
     * ⚠️ **সৎ কথা:** এটা বোঝানোর ছবি, অপারেশনের পরের আসল ফল নয়। যেখানে
     *    ডাক্তার মাংসটা এঁকেছেন ঠিক সেই জায়গাটাই পরিষ্কার দেখায় — তাই
     *    মাংসটা ছবির আসল মাংসের উপরেই আঁকতে হবে।
     * ⛔ ছবির রং তোলা না গেলে (কিছু ফোনে hardware bitmap) একটা হালকা চামড়ার
     *    রং ব্যবহার হয় — কখনো ভেঙে পড়ে না।
     */
    private fun ksSkinColour(cx: Double, cy: Double, rad: Double): Int {
        val b = base
        var r = 0L; var g = 0L; var bl = 0L; var n = 0
        if (b != null && b.width > 0 && b.height > 0) {
            for (i in 0 until 24) {
                val a = i * Math.PI * 2.0 / 24.0
                val sx = ((cx + Math.cos(a) * rad) / 100.0 * b.width).toInt()
                val sy = ((cy + Math.sin(a) * rad) / 100.0 * b.height).toInt()
                if (sx < 0 || sy < 0 || sx >= b.width || sy >= b.height) continue
                val c = try { b.getPixel(sx, sy) } catch (_: Throwable) { continue }
                r += Color.red(c); g += Color.green(c); bl += Color.blue(c); n++
            }
        }
        return if (n == 0) Color.parseColor("#B08A78")
               else Color.rgb((r / n).toInt(), (g / n).toInt(), (bl / n).toInt())
    }

    /** ছবির এক জায়গার রং (শতাংশে), না পেলে null। */
    private fun ksPixel(xPct: Double, yPct: Double): Int? {
        val b = base ?: return null
        if (b.width <= 0 || b.height <= 0) return null
        val sx = (xPct / 100.0 * b.width).toInt()
        val sy = (yPct / 100.0 * b.height).toInt()
        if (sx < 0 || sy < 0 || sx >= b.width || sy >= b.height) return null
        return try { b.getPixel(sx, sy) } catch (_: Throwable) { null }
    }

    /**
     * মাংসের চারপাশের ৮টা দিক দেখে **সবচেয়ে মসৃণ চামড়ার** টুকরোটা কোথায়।
     * ছবির ভিতরে থাকতে হবে, আর রঙের হেরফের সবচেয়ে কম হতে হবে — নইলে খাঁজ ·
     * চুল · ছায়া তুলে এনে বসাত।
     */
    private fun ksDonor(cx: Double, cy: Double, g: AnatomyModel.Lump, halfPct: Double): Pair<Double, Double>? {
        val off = Math.max(g.wide, g.len) * 1.15
        var best: Pair<Double, Double>? = null
        var bestVar = Double.MAX_VALUE
        for (k in 0 until 8) {
            val a = g.ang + Math.PI / 2 + k * Math.PI / 4
            val dx = cx + Math.cos(a) * off
            val dy = cy + Math.sin(a) * off
            if (dx - halfPct < 0 || dx + halfPct > 100 || dy - halfPct < 0 || dy + halfPct > 100) continue
            val vals = ArrayList<Double>()
            for (i in -1..1) for (j in -1..1) {
                val c = ksPixel(dx + i * halfPct * 0.55, dy + j * halfPct * 0.55) ?: continue
                vals.add((Color.red(c) + Color.green(c) + Color.blue(c)) / 3.0)
            }
            if (vals.size < 5) continue
            val mean = vals.sum() / vals.size
            val v = vals.sumOf { (it - mean) * (it - mean) } / vals.size
            if (v < bestVar) { bestVar = v; best = Pair(dx, dy) }
        }
        return best
    }

    /**
     * আঁকা মাংসটার জায়গায় **পাশের ভালো চামড়ার এক টুকরো আসল ছবি** বসানো —
     * কিনারা মিলিয়ে যায়, তাই দেখতে ঘষা দাগ নয়, সত্যিকারের চামড়ার মতো লাগে
     * (t = ০…১ ফুটে ওঠে)। ছবির রং পড়া না গেলে চারপাশের গড়-রঙের নরম ছোপ।
     */
    private fun ksHealLump(canvas: Canvas, m: AnatomyModel.Mark, t: Float) {
        if (t <= 0.01f) return
        val g = AnatomyModel.lumpGeom(m)
        if (g.len <= 0.0 || g.wide <= 0.0) return
        val cxP = m.x + Math.cos(g.ang) * g.len * 0.50
        val cyP = m.y + Math.sin(g.ang) * g.len * 0.50
        val sc = Math.min(dst.width(), dst.height()) / 100f
        // 🔴 V597 — ক্যানভাসের সব ঘর Float চায়। g.len / g.wide হলো Double, তাই
        // গুণফলটাও Double হয়ে যাচ্ছিল — সেই কারণেই Android Studio-তে
        // ১২টা "Type mismatch: Double but Float" ভুল আসছিল। হিসাব একই রইল,
        // শেষে শুধু Float-এ নামানো হলো।
        val r = Math.max(g.len * sc * 0.62f, g.wide * sc * 0.72f).toFloat()
        if (r <= 0.5f) return
        val halfPct = (r / sc).toDouble()
        val b = base
        val donor = if (b != null) ksDonor(cxP, cyP, g, halfPct) else null

        val alpha = (255f * t).toInt().coerceIn(0, 255)
        val save = canvas.save()
        canvas.translate(px(cxP), py(cyP))
        val outer = canvas.saveLayerAlpha(-r, -r, r, r, alpha)
        if (b != null && donor != null) {
            val sw = halfPct * 2.0 / 100.0 * b.width
            val sh = halfPct * 2.0 / 100.0 * b.height
            val sx = donor.first / 100.0 * b.width - sw / 2.0
            val sy = donor.second / 100.0 * b.height - sh / 2.0
            val src = Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
            if (src.left >= 0 && src.top >= 0 && src.right <= b.width && src.bottom <= b.height &&
                src.width() > 1 && src.height() > 1) {
                canvas.drawBitmap(b, src, RectF(-r, -r, r, r), null)
                // নরম কিনারা — মাঝখানে গাঢ়, ধারে মিলিয়ে যায়
                paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL
                paint.shader = android.graphics.RadialGradient(
                    0f, 0f, r,
                    intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.60f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                canvas.drawCircle(0f, 0f, r, paint)
                paint.xfermode = null; paint.shader = null
            }
        } else {
            val colour = ksSkinColour(cxP, cyP, Math.max(g.len, g.wide) * 0.85)
            paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL
            paint.shader = android.graphics.RadialGradient(
                0f, 0f, r,
                intArrayOf(colour, colour, Color.argb(0, Color.red(colour), Color.green(colour), Color.blue(colour))),
                floatArrayOf(0f, 0.58f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawCircle(0f, 0f, r, paint)
            paint.shader = null
        }
        canvas.restoreToCount(outer)
        canvas.restoreToCount(save)
        paint.reset(); paint.isAntiAlias = true
    }

    /** নালী কেটে যাওয়ার পরে নালী বরাবর আসল ছবির চামড়ার ছোপ। */
    private fun ksHealTract(canvas: Canvas, pts: List<Pair<Double, Double>>, s: Float, t: Float) {
        if (t <= 0.01f || pts.size < 2) return
        var cx = 0.0; var cy = 0.0
        for (q in pts) { cx += q.first; cy += q.second }
        cx /= pts.size; cy /= pts.size
        val colour = ksSkinColour(cx, cy, 6.0)
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        paint.color = Color.argb((255f * t).toInt().coerceIn(0, 255),
            Color.red(colour), Color.green(colour), Color.blue(colour))
        paint.strokeWidth = Math.max(3f, 3.2f * s)
        val path = Path()
        path.moveTo(px(pts[0].first), py(pts[0].second))
        for (i in 1 until pts.size) path.lineTo(px(pts[i].first), py(pts[i].second))
        canvas.drawPath(path, paint)
        paint.reset(); paint.isAntiAlias = true
    }

    /* 🔵🔒🔒 V898 (৩১.০৮.২০২৬, TK ডেমো প্রুফ দেখে **"হ্যাঁ পাশ, বসিয়ে দিন"**) —
       TK: *"ফটোতে একবার চাপ দিলে যেন শুধু ফটোটাই থাকে, বাকি সব হাইড হয়ে যায়;
       আবার চাপ দিলে পরের ধাপ · ইনজেকশন এগুলো যেন ফিরে আসে"* এবং
       *"ওগুলো হাইড হলেও যেন ছবি আঁকা যায়"*।

       ⇒ **ফাঁকা জায়গায় এক টোকা** = বোতাম লুকানো/ফেরানো (`onKsBlankTap`)।
       ⇒ **আঙুল টানলে** = আগের মতোই আঁকা — কিন্তু শুধু তখনই, যখন বোতাম লুকানো
         আছে (`ksDrawAllowed`)। বোতাম দেখা যাওয়া অবস্থায় V587-এর নিয়মই বহাল:
         ক্ষারসূত্রের মোডে ভুল করে দাগ পড়ার পথ নেই।
       ⛔ চিহ্ন **বেছে নেওয়া** ও ইনজেকশন/সুতোর **জায়গা দেখানো** — দুটোই আগের
          মতোই কাজ করে (চিহ্নের কাছে টোকা দিলে সেটাই আগে ধরা হয়)। */
    private var ksDownX = 0f
    private var ksDownY = 0f
    private var ksMoved = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (ksOn) {
            val slop = 10f * resources.displayMetrics.density
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    ksDownX = event.x; ksDownY = event.y; ksMoved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(event.x - ksDownX) > slop || Math.abs(event.y - ksDownY) > slop) ksMoved = true
                }
                MotionEvent.ACTION_UP -> {
                    if (!ksMoved) {
                        /* 🔵🔒 V898 — **লুকানো অবস্থায় এক টোকা মানে সবসময় বোতাম
                           ফিরে আসা**। (নইলে ইনজেকশন/সুতোর ধাপে টোকাটা "এইখানে
                           করুন" হিসেবে ধরা হতো, আর বোতাম আর ফিরত না — নিজের
                           যাচাইয়ে ধরা পড়েছে।) */
                        if (ksDrawAllowed) { onKsBlankTap?.invoke(); return true }
                        val p = toPercent(event.x, event.y)
                        if (p != null) {
                            /* 🟢 V589 — ইনজেকশনের ধাপে বা সুতোর ধাপে ছোঁয়া মানে
                               "এইখানে করুন"; বাকি ধাপে আগের মতোই চিহ্ন বেছে নেওয়া। */
                            if (!ksSetSpot(p[0].toDouble(), p[1].toDouble())) {
                                val i = ksNearestAt(p[0].toDouble(), p[1].toDouble())
                                if (i >= 0) onKsPick?.invoke(i)
                                else onKsBlankTap?.invoke()      // 🔵 V898
                            }
                        }
                        return true
                    }
                }
            }
            // 🔵 V898 — লুকানো অবস্থায় টান দিলে নিচের স্বাভাবিক আঁকার পথেই যায়।
            if (!ksDrawAllowed) return true
        }
        if (base == null) return false

        /* 🔵 V569 — দু'আঙুল হলে ছোট-বড় ও সরানো; তখন কিছু আঁকা হয় না।
           এক আঙুল হলে হুবহু আগের নিয়মে আঁকা হয়। */
        if (allowZoom) {
            scaleDetector.onTouchEvent(event)
            tapDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    cancelDraw(); twoFinger = true
                    lastMidX = midX(event); lastMidY = midY(event)
                    invalidate(); return true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount <= 2) { twoFinger = false; startPct = null }
                    return true
                }
            }
            if (twoFinger) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        if (!scaleDetector.isInProgress || event.pointerCount >= 2) {
                            val mx = midX(event); val my = midY(event)
                            panX += mx - lastMidX; panY += my - lastMidY
                            lastMidX = mx; lastMidY = my
                            clampPan(); invalidate()
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        twoFinger = false; startPct = null
                    }
                }
                return true
            }
        }

        val p = toPercent(event.x, event.y) ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                startPct = p
                livePts.clear()
                livePts.add(Pair(p[0].toDouble(), p[1].toDouble()))
                if (tool == Tool.ERASE) eraseNear(p[0].toDouble(), p[1].toDouble())
                /* 🟢🔒 V626 — "ফোলান" শুরু হওয়ার মুহূর্তেই ঠিক হয়ে যায় এই
                   টানটা অনুমোদিত এলাকায় (কেন্দ্রের কাছে) কি না। কেন্দ্র
                   জানা না থাকলেও (ছবিতে ⊕ এখনো ছোঁয়া হয়নি) নিরাপদ দিকেই
                   আটকানো হয় — ভুল জায়গায় ফোলা বসে যাওয়ার চেয়ে ভালো। */
                /* 🔴🔒 V678 (২৫.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট — "ওরকম কাজ বাদ
                   দিন, আমার যেখানে খুশি আমি সেখানে ফুলিয়ে নেব") — V626-এর
                   এলাকা-বাঁধন সম্পূর্ণ তুলে নেওয়া হলো। ⛔ কেন্দ্র (⊕) সেট
                   করার/o'clock হিসাবের বাকি সব কাজ অক্ষত — শুধু "ফোলান"
                   কোথায় শুরু করা যাবে তার বাঁধন নেই। */
                bulgeAllowed = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val s = startPct ?: return true
                when (tool) {
                    Tool.BULGE -> {
                        // টান যত বড়, ফোলা তত বড় — আঙুল নড়লেই আগেরটা সরিয়ে নতুনটা
                        // 🟢🔒 V626 — অনুমোদিত এলাকার বাইরে শুরু হওয়া টানে
                        // কিছুই আঁকা হয় না (bulgeAllowed ACTION_DOWN-এই ঠিক
                        // হয়ে গেছে)। আকৃতি/মাপের হিসাব এক অক্ষরও বদলায়নি।
                        if (bulgeAllowed) {
                            if (marks.isNotEmpty() && marks.last().kind == AnatomyModel.KIND_BULGE &&
                                marks.last().x == s[0].toDouble() && marks.last().y == s[1].toDouble()) {
                                marks.removeAt(marks.size - 1)
                            }
                            marks.add(AnatomyModel.bulgeFromDrag(
                                s[0].toDouble(), s[1].toDouble(), p[0].toDouble(), p[1].toDouble()))
                        }
                    }
                    Tool.TRACT, Tool.PEN, Tool.FISSURE -> {   // 🔴 V793 — ফিশারও পথ
                        val last = livePts.lastOrNull()
                        if (last == null || Math.abs(last.first - p[0]) + Math.abs(last.second - p[1]) > 0.6) {
                            livePts.add(Pair(p[0].toDouble(), p[1].toDouble()))
                        }
                    }
                    Tool.RING -> {
                        if (marks.isNotEmpty() && marks.last().kind == AnatomyModel.KIND_RING) marks.removeAt(marks.size - 1)
                        val dx = p[0] - s[0]; val dy = p[1] - s[1]
                        val r = Math.sqrt((dx * dx + dy * dy).toDouble()).coerceIn(2.0, 40.0)
                        marks.add(AnatomyModel.Mark(AnatomyModel.KIND_RING, x = s[0].toDouble(), y = s[1].toDouble(), r = r))
                    }
                    Tool.ARROW -> {
                        if (marks.isNotEmpty() && marks.last().kind == AnatomyModel.KIND_ARROW) marks.removeAt(marks.size - 1)
                        marks.add(AnatomyModel.Mark(AnatomyModel.KIND_ARROW,
                            x = s[0].toDouble(), y = s[1].toDouble(), x2 = p[0].toDouble(), y2 = p[1].toDouble()))
                    }
                    Tool.ERASE -> eraseNear(p[0].toDouble(), p[1].toDouble())
                    Tool.PILE, Tool.CENTRE -> { }   // 🔵 V585 — দুটোই শুধু ছোঁয়ায় কাজ করে
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                val s = startPct
                if (s != null) {
                    when (tool) {
                        /* 🔵 V585 — লেখাটা এখানেই হিসাব হয়ে `label`-এ বসে, অর্থাৎ
                           ঠিক সেই ঘরেই যেখানে আগে পপ-আপের বাছাই বসত। তাই A4
                           রিপোর্ট · 📜 History · প্রিন্ট · সেভ — নিচের কিছুই
                           বদলাতে হয়নি। */
                        Tool.PILE -> {
                            val c = clockCentre
                            val lab = if (c == null) "" else {
                                val h = AnatomyClock.hourAt(s[0].toDouble(), s[1].toDouble(), c.first, c.second)
                                if (h == 0) "" else "${h}টা"
                            }
                            marks.add(AnatomyModel.Mark(AnatomyModel.KIND_PILE,
                                x = s[0].toDouble(), y = s[1].toDouble(), label = lab))
                        }
                        /* কেন্দ্র বসানো — কোনো দাগ যোগ হয় না, শুধু মনে রাখা হয়। */
                        Tool.CENTRE -> {
                            clockCentre = Pair(s[0].toDouble(), s[1].toDouble())
                            onCentreSet?.invoke(s[0].toDouble(), s[1].toDouble())
                        }
                        Tool.TRACT, Tool.PEN, Tool.FISSURE -> {
                            if (livePts.size > 1) {
                                /* 🔴🔒 V793 — আঙুল যে পথে গেছে, ঠিক সেই পথই
                                   জমা হয় — তাই ফাটল/নালী বাঁকা হলেও হুবহু বসে। */
                                marks.add(AnatomyModel.Mark(
                                    when (tool) {
                                        Tool.TRACT -> AnatomyModel.KIND_TRACT
                                        Tool.FISSURE -> AnatomyModel.KIND_FISSURE
                                        else -> AnatomyModel.KIND_PEN
                                    },
                                    pts = livePts.toList()))
                            }
                        }
                        Tool.BULGE -> {
                            // শুধু ছুঁয়ে ছেড়ে দিলে ছোট একটা ফোলা — টানার দরকার নেই
                            // 🟢🔒 V626 — এখানেও একই অনুমোদিত-এলাকা পাহারা।
                            if (bulgeAllowed && (marks.isEmpty() || marks.last().kind != AnatomyModel.KIND_BULGE)) {
                                marks.add(AnatomyModel.bulgeFromDrag(
                                    s[0].toDouble(), s[1].toDouble(), s[0].toDouble(), s[1].toDouble()))
                                    }
                        }
                        else -> { }
                    }
                }
                startPct = null
                livePts.clear()
                invalidate()
                onChanged?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun midX(e: MotionEvent): Float =
        if (e.pointerCount >= 2) (e.getX(0) + e.getX(1)) / 2f else e.x
    private fun midY(e: MotionEvent): Float =
        if (e.pointerCount >= 2) (e.getY(0) + e.getY(1)) / 2f else e.y

    /** ছোঁয়ার জায়গার সবচেয়ে কাছের দাগটা মোছে। */
    /**
     * 🟢🔒 V631 (২৪.০৮.২০২৬, TK-নির্দেশ "ইরেজার মানে মোছার জন্য যেটুকু
     * মুছতে চাইবো সেটুকুই যেন মোছে") — একই aspect-ratio বাগ এখানেও ছিল
     * (ছবি বর্গাকার নয় বলে x/y-দূরত্ব সমান ধরে মাপাটা ভুল ছিল, তাই
     * কখনো কাছের দাগ বাদ যেত, কখনো দূরের দাগ মুছে যেত)। এখন `withinBulgeZone`-
     * এর সাথে হুবহু একই aspect-সংশোধন। ⛔ মোছার নিয়ম/সীমা (bestD=6.0)
     * এক অক্ষরও বদলায়নি — শুধু দূরত্ব মাপাটা এখন সঠিক।
     */
    private fun eraseNear(x: Double, y: Double) {
        val aspect = imgAspect()
        var best = -1; var bestD = 6.0
        for (i in marks.indices) {
            val m = marks[i]
            val d = when (m.kind) {
                AnatomyModel.KIND_TRACT, AnatomyModel.KIND_PEN, AnatomyModel.KIND_FISSURE ->
                    m.pts.minOfOrNull { Math.hypot(it.first - x, (it.second - y) * aspect) } ?: 999.0
                else -> Math.hypot(m.x - x, (m.y - y) * aspect)
            }
            if (d < bestD) { bestD = d; best = i }
        }
        if (best >= 0) {
            val gone = marks.removeAt(best)
            invalidate(); onChanged?.invoke()
        }
    }

    /** পর্দার ছোঁয়া → ছবির শতকরা জায়গা। ছবির বাইরে ছুঁলে null। */
    /**
     * 🟢🔒🔒 V651 (২৫.০৮.২০২৬, TK-রিপোর্ট, ছবি-প্রুফসহ যাচাই করে — "উপরের
     * ফটোতেই হচ্ছে, নিচের [পূর্ণ-পর্দার] ফটোতে হচ্ছে না") — **আসল কারণ
     * (কোড ধরে যাচাই):** টানটা ছবির নির্দিষ্ট আয়তক্ষেত্রের (`dst`) **এক
     * পিক্সেলও বাইরে** পড়লে এই ফাংশন `null` ফেরত দিত, আর `onTouchEvent`-এ
     * `?: return false`-এর কারণে **পুরো টানটাই চুপচাপ বাতিল** হয়ে যেত —
     * কোনো বার্তা (Toast) ছাড়াই, কারণ ওই বার্তা-দেখানোর কোডেই পৌঁছাত না।
     * পূর্ণ-পর্দায় (`fillScreen`) ছবি ইচ্ছাকৃতভাবে পর্দার চেয়ে বড় করে বসানো
     * হয় (পুরো পর্দা ভরাতে), তাই সেখানে কিনারার কাছে টানলে এই এক-পিক্সেল
     * গরমিল অনেক বেশি ঘটত — ছোট বোর্ডে (ছবি কখনো পর্দার বাইরে যায় না) এই
     * সমস্যাই ছিল না, ঠিক TK যা লক্ষ্য করেছেন।
     * **সমাধান:** ছবির বাইরে পড়লে বাতিল না করে, **ছবির সবচেয়ে কাছের
     * কিনারায়** টেনে আনা হয় (clamp) — তাই টান আর কখনো নিঃশব্দে হারায় না।
     * ⛔ ছবির লে-আউটই এখনো তৈরি না হলে (dst শূন্য) আগের মতোই `null` —
     *    সেই নিরাপত্তা অক্ষত। ⛔ এই ফাংশন সব হাতিয়ারই (Pile/Tract/Bulge/
     *    Ring/Pen/Erase) ব্যবহার করে, তাই সবকটাতেই একসাথে এই সুবিধা মিলল।
     */
    private fun toPercent(px: Float, py: Float): FloatArray? {
        if (dst.width() <= 0f || dst.height() <= 0f) return null
        val cx = px.coerceIn(dst.left, dst.right)
        val cy = py.coerceIn(dst.top, dst.bottom)
        return floatArrayOf(
            (cx - dst.left) / dst.width() * 100f,
            (cy - dst.top) / dst.height() * 100f
        )
    }

    // ───────── আঁকা ─────────

    override fun onDraw(canvas: Canvas) {
        val b = base
        if (b == null) {
            paint.color = Color.parseColor("#8A93A0")
            paint.textSize = 14f * density
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Select a picture from above", width / 2f, height / 2f, paint)
            paint.textAlign = Paint.Align.LEFT
            return
        }
        val img = b

        /* ছোট বোর্ডে ছবিটা পুরোটা ভিতরে ধরানো হয়। পুরো পর্দায় (V569) ছবিটা
           গোটা পর্দা **ভরে** বসে, তার উপরে ডাক্তারের জুম ও সরানো। */
        val vw = width.toFloat(); val vh = height.toFloat()
        val sc = baseScale(img, vw, vh) * (if (allowZoom) zoom else 1f)
        val w = img.width * sc; val h = img.height * sc
        val left = (vw - w) / 2f + (if (allowZoom) panX else 0f)
        val top = (vh - h) / 2f + (if (allowZoom) panY else 0f)
        dst.set(left, top, left + w, top + h)
        /* 🔴🔒 V793 — আসল ছবিতে ছবির পিক্সেলই ফোলে; আঁকা ছবিতে আগের মতোই। */
        if (photoBulgeOn()) drawPhotoBulge(canvas, img)
        else canvas.drawBitmap(img, null, dst, null)

        drawMarks(canvas)
    }

    /* 🔵🔒 V571 — আগে এখানে `rebuild()` ও `applyBulge()` ছিল: আসল ছবির পিক্সেল
       বাইরের দিকে ঠেলে "ফোলা" বানানো হত। TK ছবি পাঠিয়ে বললেন সেটা *"যথাযথ
       মিল খাচ্ছে না"* — এখন মাংসপিণ্ডটা `drawLump()` দিয়ে **আঁকা** হয়।
       ⚡ পাশাপাশি ফোনও হালকা হলো — আর কোনো বড় bitmap কপি বা পিক্সেল-লুপ নেই,
          তাই টানার সময় আটকায় না আর মেমরি-শেষ হওয়ার ভয়ও থাকে না। */

    /**
     * 🔵🔒 V571 — **পাইলসের মাংসপিণ্ড আঁকা** (TK-এর পাঠানো ছবির মতো)।
     * ⚠️ ওয়েবের `bulge()`/`lumpPath()`-এর হুবহু যমজ — একই আকার, একই রং, একই
     *    দানার হিসাব। তাই ফোন আর কম্পিউটারে মাংসটা এক দেখায়।
     */
    private val lumpPathObj = Path()

    /** ফোঁটার আকার — গোড়ায় (০,০) সরু ডগা, মাথায় গোল বল। */
    private fun buildLumpPath(len: Float, wide: Float) {
        val hw = wide / 2f
        val cx = len - hw
        lumpPathObj.reset()
        if (cx <= hw * 0.30f) {
            lumpPathObj.addCircle(len - hw, 0f, hw, Path.Direction.CW)
            return
        }
        val beta = Math.acos(Math.max(-1.0, Math.min(1.0, (hw / cx).toDouble())))
        val a1 = -(Math.PI - beta)
        val a2 = (Math.PI - beta)
        val p1x = (cx + hw * Math.cos(a1)).toFloat(); val p1y = (hw * Math.sin(a1)).toFloat()
        val p2x = (cx + hw * Math.cos(a2)).toFloat(); val p2y = (hw * Math.sin(a2)).toFloat()
        val tip = Math.min(hw * 0.22f, cx * 0.10f)
        lumpPathObj.moveTo(tip * 0.25f, -tip)
        lumpPathObj.quadTo(cx * 0.40f, p1y * 0.70f, p1x, p1y)
        // গোল মাথাটা — a1 থেকে a2 পর্যন্ত, দূরের দিক দিয়ে
        val oval = RectF(cx - hw, -hw, cx + hw, hw)
        val start = Math.toDegrees(a1).toFloat()
        val sweep = Math.toDegrees(a2 - a1).toFloat()
        lumpPathObj.arcTo(oval, start, sweep, false)
        lumpPathObj.quadTo(cx * 0.40f, p2y * 0.70f, tip * 0.25f, tip)
        lumpPathObj.quadTo(-tip * 0.45f, 0f, tip * 0.25f, -tip)
        lumpPathObj.close()
    }

    private fun drawLump(canvas: Canvas, m: AnatomyModel.Mark) {
        val g = AnatomyModel.lumpGeom(m)
        val sc = Math.min(dst.width(), dst.height()) / 100f
        val len = (g.len * sc).toFloat()
        val wide = (g.wide * sc).toFloat()
        if (len < 3f) return

        val save = canvas.save()
        canvas.translate(px(m.x), py(m.y))
        canvas.rotate(Math.toDegrees(g.ang).toFloat())

        buildLumpPath(len, wide)

        // মাটিতে পড়া ছায়া — মাংসটা ছবির উপরে বসে আছে মনে হয়
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#B3766C")
        paint.setShadowLayer(wide * 0.42f, 0f, wide * 0.10f, Color.parseColor("#66461E1A"))
        canvas.drawPath(lumpPathObj, paint)
        paint.clearShadowLayer()

        val save2 = canvas.save()
        canvas.clipPath(lumpPathObj)

        // গায়ের রং — মাথার উপর-বাঁয়ে আলো, কিনারায় গাঢ়
        val hx = len - wide * 0.5f; val hy = -wide * 0.26f
        paint.shader = android.graphics.RadialGradient(
            hx - wide * 0.22f, hy, Math.max(1f, wide * 1.25f),
            intArrayOf(Color.parseColor("#F7DCD4"), Color.parseColor("#EEC1B6"),
                       Color.parseColor("#DFA294"), Color.parseColor("#C8837A"),
                       Color.parseColor("#A75F58")),
            floatArrayOf(0f, 0.30f, 0.62f, 0.86f, 1f),
            android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(-len * 0.3f, -wide, len * 1.5f, wide, paint)
        paint.shader = null

        /* 🔵🔒 V582 (TK-নির্দেশ ২৩.০৮.২০২৬, আসল অপারেশনের ছবি দেখিয়ে):
           *"real লাগতে হবে অথবা AI animation-এর মতো হতে হবে"*।
           আগে সমান মাপের শক্ত-কিনারা গোল দাগ ছিল — কার্টুনের মতো লাগত।
           এখন দুই স্তর: বড় নরম **ছোপ**, তার উপরে ছোট **রক্তের ছিটে** —
           দুটোরই কিনারা মিলিয়ে যায়, তাই মাংসের মতো দেখায়।
           ⛔ এলোমেলো সংখ্যাগুলোর ক্রম **কম্পিউটারের অ্যাপের হুবহু এক**
              (`wlv1` bulge()-এ একই ক্রমে), তাই একই ফোলা দুই যন্ত্রে একই। */
        val state = longArrayOf(AnatomyModel.lumpSeed(m.x, m.y, g.len))
        paint.style = Paint.Style.FILL
        // ১. গায়ের ছোপ
        for (k in 0 until 4) {
            val mu = 0.18 + AnatomyModel.lumpNext(state) * 0.70
            val mv = (AnatomyModel.lumpNext(state) * 2 - 1) * 0.62
            val mr = (wide * (0.26 + AnatomyModel.lumpNext(state) * 0.24)).toFloat()
            val mx = (len * mu).toFloat()
            val my = ((wide / 2f) * mv).toFloat()
            if (mr <= 0f) continue
            paint.shader = android.graphics.RadialGradient(
                mx, my, mr,
                intArrayOf(Color.parseColor("#61CE7C74"), Color.parseColor("#33D6928A"),
                           Color.parseColor("#00DCA098")),
                floatArrayOf(0f, 0.60f, 1f), android.graphics.Shader.TileMode.CLAMP)
            canvas.drawCircle(mx, my, mr, paint)
            paint.shader = null
        }
        /* ২. সরু শিরা — TK: *"আরো রিয়েল মনে হয়"*। খুব হালকা তিনটে বাঁকা দাগ,
           গায়ের সমান-সমান ভাবটা ভেঙে দেয়। ⛔ এলোমেলো সংখ্যার ক্রম
           কম্পিউটারের অ্যাপের হুবহু এক (৪টে করে, তিনবার)। */
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (q in 0 until 3) {
            val au = 0.24 + AnatomyModel.lumpNext(state) * 0.48
            val av = (AnatomyModel.lumpNext(state) * 2 - 1) * 0.48
            val bu = Math.min(0.95, au + 0.10 + AnatomyModel.lumpNext(state) * 0.22)
            val bv = Math.max(-0.9, Math.min(0.9, av + (AnatomyModel.lumpNext(state) * 2 - 1) * 0.45))
            path.reset()
            path.moveTo((len * au).toFloat(), ((wide / 2f) * av).toFloat())
            path.quadTo((len * (au + bu) / 2).toFloat(),
                        ((wide / 2f) * (av + bv) / 2 - wide * 0.10f).toFloat(),
                        (len * bu).toFloat(), ((wide / 2f) * bv).toFloat())
            paint.color = Color.parseColor("#579E2226")
            paint.strokeWidth = Math.max(0.5f, wide * 0.032f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        // ৩. রক্তের ছিটে
        val n = Math.max(10, Math.min(46, Math.round(g.len * 1.9).toInt()))
        for (i in 0 until n) {
            val u = 0.10 + AnatomyModel.lumpNext(state) * 0.88
            val spread = Math.sin(Math.PI * Math.min(1.0, u * 1.02)) * 0.92
            val v = (AnatomyModel.lumpNext(state) * 2 - 1) * spread
            val ccx = (len * u).toFloat()
            val ccy = ((wide / 2f) * v).toFloat()
            val rr = (wide * (0.014 + AnatomyModel.lumpNext(state) * 0.050)).toFloat()
            if (rr <= 0f) continue
            paint.shader = android.graphics.RadialGradient(
                ccx, ccy, rr,
                intArrayOf(Color.parseColor("#EBA80C16"), Color.parseColor("#A8C41E26"),
                           Color.parseColor("#00D64044")),
                floatArrayOf(0f, 0.55f, 1f), android.graphics.Shader.TileMode.CLAMP)
            canvas.drawCircle(ccx, ccy, rr, paint)
            paint.shader = null
        }

        // কিনারার দিকে ভিতরে ছায়া — গোল ভাবটা বাড়ে
        paint.style = Paint.Style.FILL
        paint.shader = android.graphics.RadialGradient(
            len - wide * 0.5f, 0f, Math.max(1f, wide * 1.05f),
            intArrayOf(Color.parseColor("#0078342E"), Color.parseColor("#8078342E")),
            floatArrayOf(0.286f, 1f), android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(-len * 0.3f, -wide, len * 1.5f, wide, paint)
        paint.shader = null

        // ভেজা-চকচকে আলো
        paint.shader = android.graphics.RadialGradient(
            hx - wide * 0.26f, hy, Math.max(1f, wide * 0.52f),
            Color.parseColor("#6BFFF4F0"), Color.parseColor("#00FFF4F0"),
            android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(-len * 0.3f, -wide, len * 1.5f, wide, paint)
        paint.shader = null
        canvas.restoreToCount(save2)

        // চারদিকের গাঢ় কিনারা
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#C78E4A44")
        paint.strokeWidth = Math.max(0.8f, len * 0.030f)
        canvas.drawPath(lumpPathObj, paint)
        canvas.restoreToCount(save)
    }

    private fun drawMarks(canvas: Canvas) {
        val s = Math.min(dst.width(), dst.height()) / 100f
        paint.reset(); paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND

        /* 🔵 V587 (২৩.০৮.২০২৬, TK-নির্দেশ: *"ঘড়ি আঁকানো থাকবে না"*) —
           V585-এ কেন্দ্র জানা থাকলে হালকা সবুজ ঘড়ি ও মাঝের ফোঁটা আঁকা হত।
           TK চান ছবিতে ঘড়ির **কোনো চিহ্নই** না থাকুক, তাই আঁকাটা তুলে দেওয়া হলো।
           ⛔ **হিসাব অটুট** — কেন্দ্র আগের মতোই জমা থাকে (`AnatomyClock`), আর
              চিহ্ন বসালে o'clock আগের মতোই নিজে হিসাব হয়ে লেখা বসে।
              শুধু চোখে ঘড়িটা আর দেখা যায় না। */
        paint.reset(); paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND

        // এখন আঙুল যেটা টানছে সেটাও দেখা যাবে, ছাড়ার অপেক্ষা করতে হবে না
        if (livePts.size > 1 && (tool == Tool.TRACT || tool == Tool.PEN || tool == Tool.FISSURE)) {
            /* 🔴 V793 — ফিশার টানার সময়েও সঙ্গে সঙ্গে ফাটলের চেহারাতেই দেখা যায় */
            if (tool == Tool.FISSURE) {
                strokePts(canvas, livePts, s, "#6E1710", 3.6f, false)
                strokePts(canvas, livePts, s, "#F3E7DA", 1.15f, false)
            } else
            strokePts(canvas, livePts, s, if (tool == Tool.TRACT) "#F0A400" else "#111111",
                      if (tool == Tool.TRACT) 1.35f else 1.4f, tool == Tool.TRACT)
            // আঙুল টানার সময়েই মাপটা দেখা যায় — ছাড়ার অপেক্ষা করতে হয় না
            if (tool == Tool.TRACT) drawTractCm(canvas, livePts, s)
        }
        // মাংস আগে, দাগ পরে — ওয়েবের `draw()`-এর মতোই ক্রম
        /* 🔵 V587 — KS মোডে **বাছা চিহ্নটা** এখানে আঁকা হয় না; নিচে
           `drawKs()` তাকে ধাপ অনুযায়ী আঁকে (ফোলা · সুতো · কেটে পড়া)। */
        for ((i, m) in marks.withIndex()) {
            if (ksOn && i == ksIndex) continue
            // 🔴🔒 V793 — আসল ছবিতে আঁকা মাংস বসে না (ছবির পিক্সেলই ফুলেছে)
            if (m.kind == AnatomyModel.KIND_BULGE && !baseIsPhoto) drawLump(canvas, m)
        }
        paint.reset(); paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        for ((mi, m) in marks.withIndex()) {
            if (ksOn && mi == ksIndex && m.kind != AnatomyModel.KIND_BULGE) continue
            when (m.kind) {
                AnatomyModel.KIND_TRACT -> {
                    strokePts(canvas, m.pts, s, "#F0A400", 1.35f, true)
                    // 🔴 V564 (TK): নালী কত সেন্টিমিটার — শেষ মাথার পাশে
                    drawTractCm(canvas, m.pts, s)
                }
                AnatomyModel.KIND_PEN   -> strokePts(canvas, m.pts, s, "#111111", 1.4f, false)
                /* 🔴🔒 V793 — **ফিশারের ফাটল**, ডাক্তারের আঙুলের পথ ধরেই।
                   ভিতরে গাঢ় লাল খাঁজ, উপরে সরু সাদা রেখা (শক্ত কিনারা) —
                   তাই রোগী দেখেই বোঝেন "এই বরাবর ফাটল"। */
                AnatomyModel.KIND_FISSURE -> {
                    strokePts(canvas, m.pts, s, "#6E1710", 3.6f, false)
                    strokePts(canvas, m.pts, s, "#F3E7DA", 1.15f, false)
                }
                AnatomyModel.KIND_RING -> {
                    val cx = px(m.x); val cy = py(m.y); val r = (m.r / 100.0 * dst.width()).toFloat()
                    paint.style = Paint.Style.STROKE
                    paint.color = Color.argb(102, 0, 0, 0); paint.strokeWidth = 2.8f * s
                    canvas.drawOval(RectF(cx - r, cy - r * 0.82f, cx + r, cy + r * 0.82f), paint)
                    paint.color = Color.parseColor("#12A05A"); paint.strokeWidth = 1.6f * s
                    canvas.drawOval(RectF(cx - r, cy - r * 0.82f, cx + r, cy + r * 0.82f), paint)
                }
                AnatomyModel.KIND_ARROW -> drawArrow(canvas, px(m.x), py(m.y), px(m.x2), py(m.y2), s)
                AnatomyModel.KIND_PILE -> {
                    val cx = px(m.x); val cy = py(m.y)
                    paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#D81E3F")
                    canvas.drawCircle(cx, cy, 2.4f * s, paint)
                    paint.style = Paint.Style.STROKE; paint.color = Color.WHITE; paint.strokeWidth = 1.1f * s
                    canvas.drawCircle(cx, cy, 2.4f * s, paint)
                    if (m.label.isNotBlank()) chip(canvas, cx + 3.4f * s, cy, m.label, s)
                }
            }
        }

        // 🔵 V587 — ক্ষারসূত্রের ধাপ (বাছা চিহ্নের উপরে)
        if (ksOn) drawKs(canvas, s)
    }

    /**
     * 🔵🔒 V587 — বাছা চিহ্নটা ধাপ অনুযায়ী আঁকা।
     * ⛔ শুধু আঁকা — `marks` তালিকা ছোঁয়া হয় না, কিছু সেভ হয় না।
     */
    /**
     * 🔴🔒 V793 — চলতি ধাপে এই মাংসটা কতটা ফোলা।
     *  · **না সারালে** (WORSE) — ধাপে ধাপে বড় হতে থাকে
     *  · **চিকিৎসা** — আগের V587-এর হুবহু সেই হিসাব (`lumpStrength`)
     */
    private fun ksLumpStrength(m: AnatomyModel.Mark): Double {
        val b = if (m.s != 0.0) m.s else 0.45
        if (ksStep in KsharSutraAnim.WORSE_1..KsharSutraAnim.WORSE_3) {
            val gr = KsharSutraAnim.worseGrow(ksStep, ksT).toDouble()
            return b + (AnatomyModel.BULGE_MAX - b) * gr
        }
        return KsharSutraAnim.lumpStrength(b, ksStep, ksT)
    }

    /** পথের শুরু থেকে `u` ভগ্নাংশ পর্যন্ত অংশ (সাপ্তাহিক কাটার জন্য)। */
    private fun ptsHead(pts: List<Pair<Double, Double>>, u: Float): List<Pair<Double, Double>> {
        if (u <= 0f) return emptyList()
        val n = Math.max(2, Math.round(pts.size * u.coerceIn(0f, 1f)))
        return pts.take(Math.min(n, pts.size))
    }
    /** `u` ভগ্নাংশের পর বাকি অংশ। */
    private fun ptsTail(pts: List<Pair<Double, Double>>, u: Float): List<Pair<Double, Double>> {
        if (u >= 1f) return emptyList()
        val n = Math.round(pts.size * u.coerceIn(0f, 1f))
        return pts.drop(Math.max(0, Math.min(n, pts.size - 2)))
    }

    private fun drawKs(canvas: Canvas, s: Float) {
        val m = marks.getOrNull(ksIndex) ?: return
        val worse = ksStep in KsharSutraAnim.WORSE_1..KsharSutraAnim.WORSE_3

        /* ═══ 🔴🔒 V793 — **ফিশার** (ক্ষার-কর্ম; সুতো বাঁধা হয় না) ═══
           ফাটলটা ডাক্তারের আঙুলের পথ ধরেই, তাই বাঁকা হলেও ধাপগুলো সেই বাঁকেই। */
        if (m.kind == AnatomyModel.KIND_FISSURE) {
            val pts = m.pts
            if (pts.size < 2) return
            if (worse) {
                // না সারালে — ফাটল আরো গভীর ও লম্বা
                val gr = KsharSutraAnim.worseGrow(ksStep, ksT)
                strokePts(canvas, pts, s, "#6E1710", 3.6f + 3.4f * gr, false)
                strokePts(canvas, pts, s, "#F3E7DA", 1.15f + 1.1f * gr, false)
                if (gr > 0.05f) ksFisTag(canvas, pts, s, 1f + 1.4f * gr)
                return
            }
            when (ksStep) {
                KsharSutraAnim.FIS_NUMB -> {
                    strokePts(canvas, pts, s, "#6E1710", 3.6f, false)
                    strokePts(canvas, pts, s, "#F3E7DA", 1.15f, false)
                    ksFisTag(canvas, pts, s, 1f)
                    ksFisTool(canvas, pts, s, ksT, "#9FB6C8")   // অবশ করার সুচ
                }
                KsharSutraAnim.FIS_KSHAR -> {
                    strokePts(canvas, pts, s, "#6E1710", 3.6f, false)
                    ksFisTag(canvas, pts, s, 1f)
                    // ক্ষার লাগছে — ফাটল বরাবর গাঢ় বাদামি প্রলেপ
                    strokePts(canvas, ptsHead(pts, ksT), s, "#3B2A0C", 3.2f, false)
                    ksFisTool(canvas, pts, s, 1f, "#C9A96B")
                }
                KsharSutraAnim.FIS_WASH -> {
                    strokePts(canvas, pts, s, "#8E2A22", 3.2f, false)
                    strokePts(canvas, pts, s, "#C4564A", 1.5f, false)
                    val a = ((1f - ksT) * 255f).toInt().coerceIn(0, 255)
                    if (a > 4) {
                        val lay = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), a)
                        strokePts(canvas, pts, s, "#3B2A0C", 3.2f, false)
                        canvas.restoreToCount(lay)
                    }
                }
                KsharSutraAnim.FIS_HEAL -> {
                    ksHealTract(canvas, pts, s, ksT)            // আসল ছবির জায়গাটাও পরিষ্কার
                    val a = ((1f - ksT) * 255f).toInt().coerceIn(0, 255)
                    if (a > 4) {
                        val lay = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), a)
                        strokePts(canvas, pts, s, "#8E2A22", 3.2f, false)
                        canvas.restoreToCount(lay)
                    }
                    strokePts(canvas, pts, s, "#E7C9BD", 1.4f, false)
                }
                else -> {                                       // FIS_DRAWN
                    strokePts(canvas, pts, s, "#6E1710", 3.6f, false)
                    strokePts(canvas, pts, s, "#F3E7DA", 1.15f, false)
                    ksFisTag(canvas, pts, s, 1f)
                }
            }
            return
        }

        if (m.kind == AnatomyModel.KIND_TRACT) {
            val pts = m.pts
            if (pts.size < 2) return
            /* 🔴🔒 V793 — না সারালে: নালী মোটা হয়, শেষ মাথায় নতুন মুখ ফোটে */
            if (worse) {
                val gr = KsharSutraAnim.worseGrow(ksStep, ksT)
                strokePts(canvas, pts, s, "#F0A400", 1.35f + 1.5f * gr, true)
                if (gr > 0.35f) {
                    val e = pts.last()
                    paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL
                    paint.color = Color.argb(90, 227, 178, 60)
                    canvas.drawCircle(px(e.first), py(e.second), (6f + 10f * gr) * s, paint)
                    paint.color = Color.parseColor("#C62828")
                    canvas.drawCircle(px(e.first), py(e.second), 2.6f * s, paint)
                }
                return
            }
            /* 🔴🔒 V793 — **সপ্তাহে সপ্তাহে সুতো বদল** (TK: "ফুটবলের সাইজ ১০ →
               ৯ → ৮ → ৭")। কাটা অংশটা পিছনে ভরে যায়, তাই সুতোর গোলটা ছোট হয়। */
            if (ksStep > KsharSutraAnim.TRACT_WEEK || ksStep == KsharSutraAnim.TRACT_HEAL) {
                val u = KsharSutraAnim.weekHealed(ksStep, ksT, ksWeeks)
                ksHealTract(canvas, ptsHead(pts, u), s, 1f)      // ভরে যাওয়া অংশ
                val rest = ptsTail(pts, u)
                if (rest.size > 1) {
                    strokePts(canvas, rest, s, "#F0A400", 1.35f, true)
                    ksTractThread(canvas, s, rest, 1f, true)
                }
                strokePts(canvas, ptsHead(pts, u), s, "#9A7C77", 0.9f, true)
                return
            }
            when (ksStep) {
                KsharSutraAnim.TRACT_DRAWN ->
                    strokePts(canvas, pts, s, "#F0A400", 1.35f, true)
                KsharSutraAnim.TRACT_LACE -> {
                    strokePts(canvas, pts, s, "#F0A400", 1.35f, true)
                    ksTractThread(canvas, s, pts, ksT, false)
                }
                KsharSutraAnim.TRACT_TIE -> {
                    strokePts(canvas, pts, s, "#F0A400", 1.35f, true)
                    ksTractThread(canvas, s, pts, 1f, true)
                }
                KsharSutraAnim.TRACT_CUT -> {
                    // 🟢 V589 — নালী বরাবর আসল ছবির জায়গাটাও পরিষ্কার দেখায়
                    ksHealTract(canvas, pts, s, ksT)
                    // নালী ও সুতো মিলিয়ে যায়, সেরে ওঠার হালকা দাগ থাকে
                    val a = ((1f - ksT) * 255f).toInt().coerceIn(0, 255)
                    if (a > 4) {
                        val lay = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), a)
                        strokePts(canvas, pts, s, "#F0A400", 1.35f, true)
                        ksTractThread(canvas, s, pts, 1f, true)
                        canvas.restoreToCount(lay)
                    }
                    strokePts(canvas, pts, s, "#9A7C77", 0.9f, true)
                }
                else -> strokePts(canvas, pts, s, "#F0A400", 1.35f, true)
            }
            return
        }
        // ── মাংস ──
        val strength = KsharSutraAnim.lumpStrength(
            if (m.s != 0.0) m.s else 0.45, ksStep, ksT)
        val drop = KsharSutraAnim.lumpDrop(ksStep, ksT)
        val shown = m.copy(y = m.y + drop, y2 = m.y2 + drop, s = strength)
        val g = AnatomyModel.lumpGeom(shown)
        val sc = Math.min(dst.width(), dst.height()) / 100f
        val len = (g.len * sc).toFloat()
        val wide = (g.wide * sc).toFloat()

        // কেটে পড়ার পরে গোড়ার পরিষ্কার দাগ (আসল জায়গাতেই, নামে না)
        if (ksStep == KsharSutraAnim.LUMP_FALL) {
            /* 🟢🔒 V589 (TK-নির্দেশ) — *"রিয়েল ফটোতেও যেন পরিষ্কার করে দেয়"*।
               আঁকা মাংসের নিচে আসল ছবিতে যে মাংসটা দেখা যাচ্ছিল, সেটা এখন
               চারপাশের চামড়ার রঙে ঢাকা পড়ে — রোগী পরিষ্কার দেখতে পান।
               ⛔ আসল ছবি বদলায় না, শুধু পর্দায় উপরে আঁকা। */
            ksHealLump(canvas, m, ksT)
            val g0 = AnatomyModel.lumpGeom(m)
            val save0 = canvas.save()
            canvas.translate(px(m.x), py(m.y))
            canvas.rotate(Math.toDegrees(g0.ang).toFloat())
            KsharSutraAnim.drawCleanSpot(canvas, paint,
                (g0.len * sc).toFloat(), (g0.wide * sc).toFloat(), ksT)
            canvas.restoreToCount(save0)
        }

        val alpha = KsharSutraAnim.lumpAlpha(ksStep, ksT)
        val lay = if (alpha < 0.999f)
            canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(),
                (alpha * 255f).toInt().coerceIn(0, 255)) else -1
        drawLump(canvas, shown)
        val save = canvas.save()
        canvas.translate(px(shown.x), py(shown.y))
        canvas.rotate(Math.toDegrees(g.ang).toFloat())
        when (ksStep) {
            // 🟢 V589 — ডাক্তার যেখানে ছুঁয়ে দেখিয়েছেন ঠিক সেখানেই
            KsharSutraAnim.LUMP_INJECT ->
                KsharSutraAnim.drawNeedle(canvas, paint, len, wide, ksT, ksInjAlong, ksInjAcross)
            KsharSutraAnim.LUMP_TIE    ->
                KsharSutraAnim.drawThread(canvas, paint, len, wide, ksT, ksTieAt)
            KsharSutraAnim.LUMP_FALL   ->
                if (ksT < 0.80f) KsharSutraAnim.drawThread(canvas, paint, len, wide, 1f, ksTieAt)
            else -> { }
        }
        canvas.restoreToCount(save)
        if (lay >= 0) canvas.restoreToCount(lay)
        paint.reset(); paint.isAntiAlias = true
    }

    /** নালী বরাবর সুতো (grow = কতটা পরানো হলো)। */
    /** 🔴🔒 V793 — ফাটলের বাইরের ছোট মাংস (sentinel tag)। `k` = কত বড়। */
    private fun ksFisTag(canvas: Canvas, pts: List<Pair<Double, Double>>, s: Float, k: Float) {
        val q = pts.last()
        val cx = px(q.first); val cy = py(q.second)
        val r = 2.6f * s * k
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#D8A08C")
        canvas.drawOval(RectF(cx - r, cy - r * 1.25f, cx + r, cy + r * 1.25f), paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.7f * s
        paint.color = Color.parseColor("#B87B67")
        canvas.drawOval(RectF(cx - r, cy - r * 1.25f, cx + r, cy + r * 1.25f), paint)
    }

    /** 🔴🔒 V793 — অবশ করার সুচ / ক্ষার লাগানোর কাঠি — ফাটলের মাথায় এগিয়ে আসে। */
    private fun ksFisTool(canvas: Canvas, pts: List<Pair<Double, Double>>, s: Float,
                          prog: Float, colour: String) {
        val q = pts[pts.size / 2]
        val tx = px(q.first); val ty = py(q.second)
        val back = (1f - prog.coerceIn(0f, 1f)) * 22f * s
        val bx = tx + (16f * s + back); val by = ty + (14f * s + back)
        paint.reset(); paint.isAntiAlias = true; paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.parseColor(colour); paint.strokeWidth = 1.8f * s
        canvas.drawLine(bx, by, tx, ty, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#EFE3C9")
        canvas.drawCircle(tx, ty, 1.6f * s, paint)
    }

    private fun ksTractThread(canvas: Canvas, s: Float,
                              pts: List<Pair<Double, Double>>, grow: Float, knot: Boolean) {
        val n = Math.max(2, Math.round(pts.size * grow))
        val part = pts.take(n)
        strokePts(canvas, part, s, "#2F3A45", 1.9f, false)
        strokePts(canvas, part, s, "#6B7A87", 0.8f, false)
        if (knot) {
            paint.reset(); paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#2F3A45")
            for (q in listOf(part.first(), part.last()))
                canvas.drawCircle(px(q.first), py(q.second), Math.max(2.2f, 1.1f * s), paint)
        }
    }

    /**
     * 🔴 V564 (TK, লাইভ টেস্ট): *"ফিস্টুলার দাগ টানলে যেন কত সেন্টিমিটার সেটা
     * বোঝা যায় না"*। মাপটা ছবির চওড়া কত সেমি ধরে বার করা হয় — আমাদের আঁকা
     * ছবিতে সেটা সঠিক জানা, আসল ফটোয় আন্দাজি, তাই সেখানে "≈" বসে।
     */
    private fun drawTractCm(canvas: Canvas, pts: List<Pair<Double, Double>>, s: Float) {
        if (pts.size < 2) return
        val b = base ?: return
        val pic = AnatomyModel.pictureOf(picKey)
        val cmWide = pic?.cmWide ?: 10.0
        val exact = pic?.exactScale ?: false
        val cm = AnatomyModel.tractCm(pts, cmWide, b.height.toDouble() / b.width.toDouble())
        if (cm <= 0.0) return
        val last = pts[pts.size - 1]
        chip(canvas, px(last.first) + 3.4f * s, py(last.second), AnatomyModel.tractLabel(cm, exact), s)
    }

    private fun strokePts(canvas: Canvas, pts: List<Pair<Double, Double>>, s: Float,
                          color: String, w: Float, dashed: Boolean) {
        if (pts.size < 2) return
        path.reset()
        for (i in pts.indices) {
            val x = px(pts[i].first); val y = py(pts[i].second)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        paint.style = Paint.Style.STROKE; paint.pathEffect = null
        /* 🔵 V583 (TK-নির্দেশ): ফিস্টুলার দাগ সামান্য পাতলা — কালো ছায়াটাও
           সেই অনুপাতে, নইলে সরু দাগের চারপাশে মোটা ছায়া বেমানান লাগত।
           ⛔ রং · কাটা-কাটা ধরন কিছুই বদলায়নি। */
        paint.color = Color.argb(if (dashed) 87 else 115, 0, 0, 0)
        paint.strokeWidth = (w + (if (dashed) 0.7f else 1.6f)) * s
        canvas.drawPath(path, paint)
        paint.color = Color.parseColor(color); paint.strokeWidth = w * s
        if (dashed) paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(2.8f * s, 2.0f * s), 0f)
        canvas.drawPath(path, paint)
        paint.pathEffect = null
    }

    private fun drawArrow(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, s: Float) {
        val a = Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val hl = 4.2f * s
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(102, 0, 0, 0); paint.strokeWidth = 3.2f * s
        canvas.drawLine(x1, y1, x2, y2, paint)
        paint.color = Color.parseColor("#1B6FD8"); paint.strokeWidth = 1.8f * s
        canvas.drawLine(x1, y1, x2, y2, paint)
        path.reset()
        path.moveTo(x2, y2)
        path.lineTo(x2 - hl * Math.cos(a - 0.42).toFloat(), y2 - hl * Math.sin(a - 0.42).toFloat())
        path.lineTo(x2 - hl * Math.cos(a + 0.42).toFloat(), y2 - hl * Math.sin(a + 0.42).toFloat())
        path.close()
        paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#1B6FD8")
        canvas.drawPath(path, paint)
    }

    private fun chip(canvas: Canvas, x: Float, y: Float, txt: String, s: Float) {
        paint.style = Paint.Style.FILL
        paint.textSize = 3.1f * s
        val w = paint.measureText(txt) + 2.6f * s
        val h = 5.2f * s; val r = 2.6f * s
        val box = RectF(x, y - h / 2, x + w, y + h / 2)
        paint.color = Color.argb(240, 255, 255, 255)
        canvas.drawRoundRect(box, r, r, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.9f * s
        paint.color = Color.parseColor("#D81E3F")
        canvas.drawRoundRect(box, r, r, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText(txt, x + 1.3f * s, y + 1.1f * s, paint)
    }

    private fun px(v: Double) = dst.left + (v / 100.0 * dst.width()).toFloat()
    private fun py(v: Double) = dst.top + (v / 100.0 * dst.height()).toFloat()
}
