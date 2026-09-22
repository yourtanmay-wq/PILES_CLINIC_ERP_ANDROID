package com.tkbiswas.pilesclinic.clinical

import android.content.Context

/**
 * 🔵🔒 V585 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) — **ঘড়ির কাঁটা নিজে
 * থেকে হিসাব** করার একমাত্র জায়গা।
 *
 * TK-নির্দেশ (হুবহু): *"যেখানেই থাকবে চারটা কেন বাঁচবে — এটাতো অটোমেটিক্যালি
 * হওয়ার কথা"*।
 *
 * আগে কী হত: "চিহ্ন" হাতিয়ার বাছার সময় **একবার** ঘড়ির তালিকা আসত; ডাক্তার
 * যেটা বাছতেন সেই লেখাটাই এরপর **প্রতিটা চিহ্নে** বসে যেত। তাই দশটা চিহ্নেই
 * "4টা" লেখা পড়ত।
 *
 * এখন: চিহ্নটা ছবির যেখানে বসল, **সেখান থেকেই** o'clock হিসাব হয় —
 * কেন্দ্র থেকে কোণ মেপে। TK-এর বাছাই: **12 = ছবির একদম উপর**, ঘড়ির কাঁটার দিকে।
 *
 * কেন্দ্র কোথায়:
 *   • হাতে আঁকা ছক (anat26) ও 3D মডেল (anat27) — ছবির ভিতরেই বৃত্ত/পুচ্ছ আছে,
 *     তাই কেন্দ্র **পিক্সেল মেপে** বার করে এখানে বসানো (চোখে আন্দাজ নয়)।
 *   • বাকি সব ছবি (আসল রোগীর ফটো, বইয়ের ছবি, ডাক্তারের নিজের তোলা ছবি) —
 *     ডাক্তার **প্রথমবার একবার ছুঁয়ে** দেবেন, তারপর ওই ছবির জন্য মনে থাকে।
 *     ⚠️ সৎ কারণ: ওই ছবিগুলোয় মাংস বেরিয়ে থাকায় পায়ুপথের মুখ প্রায়ই ঢাকা আর
 *        ক্যামেরার কোণও একেক রকম — কেন্দ্র একটু সরে গেলেই প্রতিটা o'clock ভুল
 *        হয়ে রেকর্ডে বসে যেত। ডাক্তারের আঙুলই সবচেয়ে নিখুঁত।
 *
 * ⛔ **ঝুঁকিহীন:** o'clock-এর লেখাটা ঠিক সেখানেই বসে যেখানে আগে পপ-আপের বাছাই
 *    বসত (`Mark.label`) — তাই A4 রিপোর্ট · 📜 History · প্রিন্ট · সেভ, নিচের
 *    কিছুই বদলাতে হয়নি। কোনো নেটওয়ার্ক/ক্লাউড কল নেই (কেন্দ্র ফোনেই জমা)।
 * ⚠️ ওয়েবের `wlv1AnatCentre*` / `wlv1AnatHourOf()`-এর হুবহু যমজ।
 */
object AnatomyClock {

    private const val PREFS = "v585_anat_centre"

    /** ছবির ভিতরের বৃত্ত/পুচ্ছ মেপে পাওয়া কেন্দ্র (শতাংশে)। */
    private val BUILT_IN: Map<String, Pair<Double, Double>> = mapOf(
        // anat26 — হাতে আঁকা ছকের কালো বৃত্তটার বাক্স মেপে: x 32.8→66.7, y 11.9→45.5
        "anat26" to Pair(49.8, 28.7),
        // anat27 — 3D মডেলের পুচ্ছের কেন্দ্র (ছবিতে ছাপা 12/3/6/9 নম্বরের সাথে মিলিয়ে দেখা)
        "anat27" to Pair(50.0, 52.0)
    )

    /** এই ছবির কেন্দ্র জানা আছে কি না; না থাকলে `null`। */
    fun centreOf(ctx: Context, picKey: String): Pair<Double, Double>? {
        if (picKey.isBlank()) return null
        try {
            val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(picKey, "")
            if (!raw.isNullOrBlank()) {
                val p = raw.split(",")
                if (p.size == 2) {
                    val x = p[0].trim().toDoubleOrNull(); val y = p[1].trim().toDoubleOrNull()
                    if (x != null && y != null) return Pair(x, y)
                }
            }
        } catch (_: Throwable) { }
        return BUILT_IN[picKey]
    }

    /** ডাক্তার ছুঁয়ে দেওয়া কেন্দ্র মনে রাখা (ওই ছবির জন্য, চিরকাল)। */
    fun setCentre(ctx: Context, picKey: String, x: Double, y: Double) {
        if (picKey.isBlank()) return
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(picKey, "${AnatomyModel.n2(x)},${AnatomyModel.n2(y)}").apply()
        } catch (_: Throwable) { }
    }

    /**
     * কেন্দ্র থেকে (x, y) কোন o'clock-এ পড়ে।
     * 12 = সোজা উপর, তারপর ঘড়ির কাঁটার দিকে। ১ থেকে ১২ ফেরে।
     * ⛔ কেন্দ্রের **একদম উপরে** ছুঁলে (দূরত্ব প্রায় শূন্য) দিক বলা যায় না —
     *    তখন 0 ফেরে, আর কোনো লেখা বসে না।
     */
    fun hourAt(x: Double, y: Double, cx: Double, cy: Double): Int {
        val dx = x - cx
        val dy = y - cy
        if (Math.hypot(dx, dy) < 1.5) return 0
        var ang = Math.toDegrees(Math.atan2(dx, -dy))
        if (ang < 0) ang += 360.0
        val h = Math.round(ang / 30.0).toInt() % 12
        return if (h == 0) 12 else h
    }

    /** চিহ্নের পাশে যে লেখাটা বসবে ("7টা")। কেন্দ্র না জানা থাকলে ফাঁকা। */
    fun labelAt(ctx: Context, picKey: String, x: Double, y: Double): String {
        val c = centreOf(ctx, picKey) ?: return ""
        val h = hourAt(x, y, c.first, c.second)
        return if (h == 0) "" else "${h}টা"
    }
}
