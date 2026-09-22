package com.tkbiswas.pilesclinic.clinical

/**
 * 🔵🔒 V558 (২২.০৮.২০২৬, TK-অনুমোদিত) — **রোগের ছবি**
 *
 * TK-এর কথা (হুবহু):
 *   • *"ডাক্তার যখন ডেমো হিসেবে পেসেন্টকে সমস্যার কথা খুলে বলবে, ডাক্তার
 *     চাইলে যে কোন ফটোর উপর যেন সেই কাজটা করতে পারে"*
 *   • *"শুধু দাগ বলে কথা নয় — কোন পেশেন্টের পাইলস কতটা বেড়ে গেছে, সেই
 *     মাংসের উপরে আঙুল দিয়ে টান দিলে যেন মাংস বেড়ে যায়"*
 *
 * তাই এখানে দু'রকম কাজ:
 *   ১. **দাগ** — ফোলার চিহ্ন · নালীর রেখা · গোল দাগ · তীর · হাতের আঁকা
 *   ২. **ফোলানো (bulge)** — ছবির ওই জায়গার পিক্সেলগুলোই বাইরের দিকে ঠেলে
 *      দেওয়া হয়, তাই সত্যিকারের ফোলার মত দেখায় (দাগ আঁকা নয়)
 *
 * 🧷 জমা হওয়ার নিয়ম — V539/V554/V555/V556-এর মতোই **একটাই লেখা**, যা
 * চেকআপের `buildDetails()`-এর ভিতরে চলে যায়। তাই:
 *   ⛔ নতুন কোনো কলাম লাগেনি · ⛔ কোনো SQL চালাতে হয়নি ·
 *   ⛔ Supabase-এ বাড়তি query/egress বাড়েনি।
 *
 * 📐 সব মাপ **ছবির শতকরা** হিসেবে (০–১০০), পিক্সেল নয়। তাই ছোট ফোন,
 * বড় ফোন আর ওয়েব — তিন জায়গাতেই দাগ ঠিক একই জায়গায় বসে।
 *
 * ⚠️ ওয়েবের যমজ কোড: `03_NETLIFY_READY/app.js` → `wlv1Anat*`.
 *    দুটোর লেখা **হুবহু এক** হতে হবে — পরীক্ষায় সেটাই মিলিয়ে দেখা হয়।
 */
object AnatomyModel {

    /** এক-একটা দাগ। শতকরা মাপে। */
    data class Mark(
        val kind: String,                 // pile · tract · pen · ring · arrow · bulge
        val x: Double = 0.0,
        val y: Double = 0.0,
        val x2: Double = 0.0,             // arrow-এর শেষ মাথা
        val y2: Double = 0.0,
        val r: Double = 0.0,              // ring / bulge-এর ব্যাসার্ধ
        val s: Double = 0.0,              // bulge কতটা জোরে (−০.৮৫ … ০.৮৫)
        val label: String = "",           // ফোলার নাম (৩টা / ডান পাশ …)
        /* 🔵 V571 — bulge-এর দিক জানা আছে কি না। পুরোনো রেকর্ডে দিক লেখা নেই,
           তখন ছবির মাঝখান থেকে বাইরের দিকে ধরা হয়। */
        val hasDir: Boolean = false,
        val pts: List<Pair<Double, Double>> = emptyList()
    )

    /** পুরো ছবিটার অবস্থা: কোন ছবি, কী কী দাগ, ডাক্তারের নিজের কথা। */
    data class Board(
        val pic: String = "",
        val marks: List<Mark> = emptyList(),
        val note: String = ""
    )

    const val KIND_PILE  = "pile"
    const val KIND_TRACT = "tract"
    const val KIND_PEN   = "pen"
    const val KIND_RING  = "ring"
    const val KIND_ARROW = "arrow"
    const val KIND_BULGE = "bulge"
    /* 🔴🔒 V793 (২৮.০৮.২০২৬, TK-নির্দেশ) — **ফিশারের ফাটল।**
       TK: *"ফিসারের দাগটা যেন আমি বেকা আঁকতে পারি … আঙুল দিয়ে যেখানে ঘষা
       দিব সেখানে যেন দাগ হয়ে যায়, যাতে বোঝা যায় এই বরাবর আপনার ফিসার
       হয়েছে।"* ⇒ আঙুলের পুরো পথটা `pts`-এ জমা হয় (নালীর মতোই), তাই দাগ
       ঠিক সেই বাঁক ধরেই বসে। ⛔ পুরোনো কোনো ধরন বদলায়নি — এটা নতুন। */
    const val KIND_FISSURE = "fis"

    /** ফোলানোর সীমা — এর বেশি টানলে ছবি ভেঙে যায়, তাই আটকানো। */
    const val BULGE_MAX = 0.92
    const val RADIUS_MAX = 42.0
    const val RADIUS_MIN = 3.0

    /**
     * 🟢🔒 V626 (২৪.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ অনুমোদিত) — *"পাইলস তো
     * আর সব জায়গায় হয় না... মলদ্বারে মাংসটা খুলতে হবে, ছবির অন্যান্য
     * জায়গায় চাপলে যেন না ফোলে"*।
     *
     * ⛔ মাংসের **আকৃতি/মাপ** (`drawLump()`, `bulgeFromDrag()`) এক অক্ষরও
     *    বদলানো হয়নি — TK স্পষ্ট বলেছেন *"বর্তমান কোডে যেমন হয় ঠিক তেমনি
     *    থাকতে হবে"*। এখানে শুধু **কোথায়** কাজ করবে সেটা আটকানো হলো।
     *
     * কেন্দ্র (⊕, `AnatomyClock.centreOf`) থেকে এই দূরত্বের (শতকরা মাপে)
     * মধ্যে ছুঁলে তবেই ফোলা যায় — নইলে কিছুই আঁকা হয় না।
     * ⚠️ ওয়েবের `WLV1_ANAT_BULGE_ZONE`-এর হুবহু একই সংখ্যা।
     */
    const val BULGE_ZONE_RADIUS = 25.0

    /**
     * 🟢🔒 V631 (২৪.০৮.২০২৬, TK-রিপোর্ট "পাইলসের মাংস তো এবার ফোলানি যাচ্ছে
     * না") — **আসল কারণ ধরা পড়ল:** ছবি বর্গাকার নয় (যেমন anat26 — ৪০৮×৬২৮,
     * লম্বায় বেশি), অথচ V626-এ দূরত্ব x ও y-কে সমান ধরে মাপা হয়েছিল। ফলে
     * "২৫% ব্যাসার্ধ" আসলে একটা **উপবৃত্ত** (ellipse) হয়ে গিয়েছিল, বৃত্তাকার
     * নয় — তাই বৃত্তের চারপাশে অনেক বাস্তবসম্মত জায়গাতেও (১০টা, ১১টা...)
     * "না" বলছিল।
     *
     * সমাধান: `tractCm()`-এর (V564) **প্রমাণিত একই কৌশল** — y-দূরত্বকে
     * ছবির অনুপাতে (aspect = height÷width) গুণ করে x-এর একই মাপে আনা হয়,
     * তারপর দূরত্ব মাপা হয়। এখন সীমাটা সত্যিকারের বৃত্তাকার এলাকা।
     * ⛔ মাংসের আকৃতি/মাপ (drawLump/bulgeFromDrag) এখনো এক অক্ষরও বদলায়নি।
     * ⚠️ ওয়েবের `wlv1AnatWithinBulgeZone()`-এর হুবহু একই সংশোধন।
     */
    fun withinBulgeZone(x: Double, y: Double, cx: Double, cy: Double, aspect: Double): Boolean {
        val dx = x - cx
        val dy = (y - cy) * aspect
        return Math.hypot(dx, dy) <= BULGE_ZONE_RADIUS
    }

    /* 🔵🔒 V571 (২২.০৮.২০২৬, TK-নির্দেশ) — আগে এখানে `pushFactor()` ছিল, যেটা
       ছবির পিক্সেল বাইরের দিকে ঠেলে "ফোলা" বানাত। TK ছবি পাঠিয়ে বললেন সেটা
       *"যথাযথ মিল খাচ্ছে না"* — এখন মাংসপিণ্ডটা **আঁকা** হয় (`AnatomyView`-এ),
       ছবি ঠেলা হয় না। তাই ওই অঙ্কটার আর দরকার নেই।

       নিচের দুটো হিসাব ফোন ও ওয়েবে **হুবহু এক** — মাংসের দানাগুলো যাতে
       এলোমেলো না হয়, একই মাংস বারবার আঁকলে হুবহু এক দেখায়। */

    /** দানার বীজ — মাংসের নিজের জায়গা থেকেই, এলোমেলো নয়। */
    fun lumpSeed(x: Double, y: Double, len: Double): Long {
        val v = Math.floor(x * 131 + y * 977 + len * 17).toLong() % 2147483647L
        val w = if (v < 0) v + 2147483647L else v
        return if (w == 0L) 12345L else w
    }

    /** ওয়েবের `lumpNext()`-এর হুবহু একই সংখ্যা-শৃঙ্খল। */
    fun lumpNext(state: LongArray): Double {
        /* Park–Miller — ইচ্ছে করে ছোট গুণ (১৬৮০৭), কারণ ওয়েবে (জাভাস্ক্রিপ্টে)
           বড় গুণফল নির্ভুল থাকে না। এতে দুই জায়গার সংখ্যা হুবহু মেলে। */
        var v = (state[0] * 16807L) % 2147483647L
        if (v <= 0) v += 2147483646L
        state[0] = v
        return v.toDouble() / 2147483647.0
    }

    /** মাংসটা কোন দিকে, কত লম্বা, কত চওড়া — ওয়েবের `lumpGeom()`-এর যমজ। */
    data class Lump(val len: Double, val ang: Double, val wide: Double)

    fun lumpGeom(m: Mark): Lump {
        val len: Double
        val ang: Double
        val dx = if (m.hasDir) m.x2 - m.x else 0.0
        val dy = if (m.hasDir) m.y2 - m.y else 0.0
        val pull = Math.sqrt(dx * dx + dy * dy)
        if (m.hasDir && pull > 0.8) {
            ang = Math.atan2(dy, dx); len = Math.max(4.0, pull * 1.15)
        } else {
            val ox = m.x - 50.0; val oy = m.y - 50.0
            ang = if (Math.abs(ox) + Math.abs(oy) < 0.5) (Math.PI / 2) else Math.atan2(oy, ox)
            len = Math.max(4.0, (if (m.r > 0) m.r else 8.0) * 0.95)
        }
        val st = clamp(if (m.s != 0.0) m.s else 0.45, 0.2, 0.95)
        return Lump(len, ang, len * (0.46 + 0.30 * st))
    }



    // ─────────── ছবির তালিকা ───────────

    /** এক-একটা ছবি: `key` জমা হয়, `label` ডাক্তার পর্দায় দেখেন। */
    /**
     * @param cmWide ছবিটার **চওড়া কত সেন্টিমিটার** ধরা হবে — নালীর লম্বা
     *   মাপার জন্য এটাই মাপকাঠি।
     *   • আমাদের নিজের আঁকা ছবিগুলোয় (anat26–anat29) মাপটা **সঠিক জানা**,
     *     কারণ ছবিগুলো আমরাই সেন্টিমিটার হিসেবে এঁকেছি।
     *   • বইয়ের ছবি ও চেম্বারের আসল ফটোয় মাপটা **আন্দাজি** — ক্যামেরা কত
     *     দূর থেকে তোলা হয়েছে তা জানার উপায় নেই। তাই সেখানে সেমি-র আগে
     *     "≈" চিহ্ন দেখানো হয়, যেন ডাক্তার বুঝতে পারেন এটা মোটামুটি হিসাব।
     *   TK চাইলে যেকোনো ছবির আসল চওড়া বলে দিলে সেটাই বসানো হবে।
     */
    data class Picture(val key: String, val label: String,
                       val cmWide: Double = 10.0, val exactScale: Boolean = false,
                       /* 🔵 V573 — ডাক্তারের নিজের যোগ করা ছবি হলে ছবিটা এখানেই
                          (ছোট করা JPEG, data URL)। অ্যাপের সাথে আসা ছবিতে ফাঁকা। */
                       val photo: String = "")

    /** ক্লাউডের `anatomy_pictures` টেবিলের একটা সারি। */
    data class PicRow(val id: String, val picKey: String = "", val label: String = "",
                      val photo: String = "", val hidden: Boolean = false,
                      val sortOrder: Long = 0L, val createdAt: String = "")

    /** যোগ করা ছবির নাম — অ্যাপের ছবির নামের সঙ্গে যেন কখনো না মেলে। */
    const val CLOUD_PREFIX = "cloud:"
    fun isCloudKey(key: String): Boolean = key.startsWith(CLOUD_PREFIX)

    /**
     * 🔵🔒 V573 (২২.০৮.২০২৬, TK-অনুমোদিত) — অ্যাপের ছবি + ক্লাউডে যোগ করা ছবি
     * মিলিয়ে **পর্দায় যে তালিকাটা দেখাবে** সেটা বানায়।
     *
     * নিয়ম:
     *   ১. ডাক্তারের নিজের যোগ করা ছবি **আগে** (নতুনটা সবার আগে) — এইমাত্র
     *      তোলা ছবিটাই তো তখন দরকার।
     *   ২. তারপর অ্যাপের সাথে আসা ছবি, আগের সেই ক্রমেই।
     *   ৩. যে ছবিগুলো তালিকা থেকে সরানো হয়েছে সেগুলো বাদ।
     *
     * ⛔ সরানো মানে **মোছা নয়** — পুরোনো চেক-আপে ওই ছবির উপরে আঁকা থাকলে
     *    সেটা আগের মতোই ঠিক দেখাবে; শুধু নতুন করে আর বাছা যাবে না।
     * ⚠️ ওয়েবের `wlv1AnatMergePics()`-এর হুবহু যমজ।
     */
    fun mergePictures(builtIn: List<Picture>, rows: List<PicRow>): List<Picture> {
        val hiddenKeys = HashSet<String>()
        for (r in rows) if (r.hidden && r.picKey.isNotBlank()) hiddenKeys.add(r.picKey)
        val added = rows
            .filter { it.photo.isNotBlank() && !it.hidden }
            .sortedWith(compareByDescending<PicRow> { it.sortOrder }.thenByDescending { it.createdAt }
                .thenByDescending { it.id })
            .map { Picture(CLOUD_PREFIX + it.id,
                           if (it.label.isNotBlank()) it.label else "নিজের তোলা ছবি",
                           photo = it.photo) }
        val keep = builtIn.filter { !hiddenKeys.contains(it.key) }
        return added + keep
    }


    /**
     * TK-এর পাঠানো ছবি + আমাদের নিজের আঁকা ছবি — মোট ২৯টা।
     *
     * 🔴 সাজানোর নিয়ম (TK, ২২.০৮.২০২৬): *"হাতে আঁকা বা এয়াই মডেল বা থ্রিডি
     *    মডেল যে ফটোগুলো আগে থাকবে, আর প্রকৃত ফটোগুলো তারপরে থাকবে"* ⇒
     *    ১-৪ আঁকা/৩ডি · ৫-৭ বইয়ের ছবি · তারপর চেম্বারের আসল ছবি।
     * ⛔ এই তালিকার **ক্রমটাই** পর্দায় ছবি বাছার সারির ক্রম।
     * ⚠️ `key` কখনো বদলাবেন না — পুরনো রোগীর জমা লেখায় ওই নামই আছে,
     *    বদলালে তাদের ছবি আর খুঁজে পাওয়া যাবে না।
     * ফোনে ছবিটা `<key>` (যেমন anat01), ওয়েবে `img/anatomy/<key>.jpg`.
     */
    val PICTURES: List<Picture> = listOf(
        Picture("anat26", "হাতে আঁকা · খালি ছক", cmWide = 9.0, exactScale = true),
        Picture("anat27", "3D মডেল · ঘড়ির কাঁটা", cmWide = 16.5, exactScale = true),
        Picture("anat28", "কাটা ছবি · নরম রং", cmWide = 11.3, exactScale = true),
        Picture("anat29", "কাটা ছবি · গাঢ় রং", cmWide = 11.3, exactScale = true),
        Picture("anat01", "বই · পায়ুনালীর কাটা ছবি"),
        Picture("anat02", "বই · ফিস্টুলার চার ধরন"),
        Picture("anat03", "বই · ফিস্টুলার নকশা"),
        /* 🔵 V571 (২২.০৮.২০২৬) — TK নিজে পাঠানো আরও দুটো বইয়ের ছবি। */
        Picture("anat30", "বই · ফোঁড়া কোথায় হয়"),
        Picture("anat31", "বই · পাইলসের চার ধাপ"),
        Picture("anat04", "ফোলা · কাছ থেকে"),
        Picture("anat05", "একটা ফোলা"),
        Picture("anat06", "একটা ঢিবি"),
        Picture("anat07", "ছোট ফোলা"),
        Picture("anat08", "ফোলা · চওড়া ছবি"),
        Picture("anat09", "লাল · বেরিয়ে আসা"),
        Picture("anat10", "বেরিয়ে আসা · বেগুনি"),
        Picture("anat11", "চারপাশ জুড়ে বেরিয়ে আসা"),
        Picture("anat12", "চারপাশ জুড়ে · চওড়া ছবি"),
        Picture("anat13", "ভিতরের পর্দা বেরিয়ে আসা"),
        Picture("anat14", "অনেকটা বেরিয়ে আসা"),
        Picture("anat15", "খুব বড় মাংস"),
        Picture("anat16", "ঘা হয়ে যাওয়া মাংস"),
        Picture("anat17", "আঁচিলের মত মাংস"),
        Picture("anat18", "অনেকগুলো আঁচিল"),
        Picture("anat19", "কয়েকটা ফোলা"),
        Picture("anat20", "ফিস্টুলার মুখ"),
        Picture("anat21", "ফিস্টুলা · নালী"),
        Picture("anat22", "ছোট মুখ"),
        Picture("anat23", "রস গড়াচ্ছে"),
        Picture("anat24", "চিকিৎসার পরে"),
        Picture("anat25", "অপারেশনের সময়"),
    )

    fun labelOf(key: String): String = PICTURES.firstOrNull { it.key == key }?.label ?: key
    fun pictureOf(key: String): Picture? = PICTURES.firstOrNull { it.key == key }

    // ─────────── জমা করা ও পড়া ───────────

    fun format(board: Board): String {
        val out = StringBuilder()
        if (board.pic.isNotBlank()) out.append("pic=").append(board.pic)
        for (m in board.marks) {
            if (out.isNotEmpty()) out.append('|')
            when (m.kind) {
                /* 🔴🔒 V793 — ফিশারও আঙুলের **পুরো পথ** হিসেবেই জমা হয়
                   (নালী/কলমের মতোই), তাই বাঁকটা হুবহু ফেরে। */
                KIND_TRACT, KIND_PEN, KIND_FISSURE -> {
                    out.append(m.kind).append(':')
                    out.append(m.pts.joinToString(";") { "${n1(it.first)},${n1(it.second)}" })
                }
                KIND_RING  -> out.append("ring:").append(n1(m.x)).append(',').append(n1(m.y)).append(',').append(n1(m.r))
                KIND_ARROW -> out.append("arrow:").append(n1(m.x)).append(',').append(n1(m.y))
                    .append(',').append(n1(m.x2)).append(',').append(n1(m.y2))
                KIND_BULGE -> {
                    out.append("bulge:").append(n1(m.x)).append(',').append(n1(m.y))
                        .append(',').append(n1(m.r)).append(',').append(n2(m.s))
                    /* 🔵 V571 — দিক জানা থাকলে শেষ বিন্দুও। পুরোনো চার-সংখ্যার
                       লেখা আগের মতোই পড়া যায়, তাই কোনো রেকর্ড নষ্ট হয় না। */
                    if (m.hasDir) out.append(',').append(n1(m.x2)).append(',').append(n1(m.y2))
                }
                else -> out.append("pile:").append(n1(m.x)).append(',').append(n1(m.y)).append(',').append(m.label)
            }
        }
        if (board.note.isNotBlank()) {
            if (out.isNotEmpty()) out.append('|')
            out.append("note=").append(board.note.replace("|", "/"))
        }
        return out.toString()
    }

    fun parse(saved: String?): Board {
        if (saved.isNullOrBlank()) return Board()
        var pic = ""; var note = ""
        val marks = mutableListOf<Mark>()
        for (raw in saved.split("|")) {
            val t = raw.trim()
            if (t.isEmpty()) continue
            if (t.startsWith("pic=")) { pic = t.substring(4); continue }
            if (t.startsWith("note=")) { note = t.substring(5); continue }
            val c = t.indexOf(':')
            if (c < 0) continue
            val kind = t.substring(0, c)
            val body = t.substring(c + 1)
            when (kind) {
                KIND_TRACT, KIND_PEN, KIND_FISSURE -> {
                    val pts = body.split(";").mapNotNull { p ->
                        val a = p.split(",")
                        if (a.size < 2) null else {
                            val x = a[0].trim().toDoubleOrNull(); val y = a[1].trim().toDoubleOrNull()
                            if (x == null || y == null) null else Pair(x, y)
                        }
                    }
                    if (pts.size > 1) marks.add(Mark(kind = kind, pts = pts))
                }
                KIND_RING -> {
                    val a = body.split(",")
                    if (a.size >= 3) marks.add(Mark(KIND_RING, x = d(a[0]), y = d(a[1]), r = d(a[2])))
                }
                KIND_ARROW -> {
                    val a = body.split(",")
                    if (a.size >= 4) marks.add(Mark(KIND_ARROW, x = d(a[0]), y = d(a[1]), x2 = d(a[2]), y2 = d(a[3])))
                }
                KIND_BULGE -> {
                    val a = body.split(",")
                    if (a.size >= 6) marks.add(Mark(KIND_BULGE, x = d(a[0]), y = d(a[1]),
                        r = d(a[2]), s = d(a[3]), x2 = d(a[4]), y2 = d(a[5]), hasDir = true))
                    else if (a.size >= 4) marks.add(Mark(KIND_BULGE, x = d(a[0]), y = d(a[1]), r = d(a[2]), s = d(a[3])))
                }
                KIND_PILE -> {
                    val a = body.split(",")
                    if (a.size >= 2) marks.add(
                        Mark(KIND_PILE, x = d(a[0]), y = d(a[1]),
                             label = if (a.size > 2) a.drop(2).joinToString(",") else "")
                    )
                }
            }
        }
        return Board(pic, marks, note)
    }

    // ─────────── আঙুলের টান → ফোলা কত বড় ───────────

    /**
     * ডাক্তার যেখানে আঙুল রাখলেন (startX,startY) আর যেখানে টেনে নিলেন
     * (nowX,nowY) — সেই দূরত্ব থেকেই ফোলার মাপ। যত টান, তত বড়।
     */
    fun bulgeFromDrag(startX: Double, startY: Double, nowX: Double, nowY: Double): Mark {
        val dx = nowX - startX; val dy = nowY - startY
        val pull = Math.sqrt(dx * dx + dy * dy)
        return Mark(
            kind = KIND_BULGE, x = startX, y = startY,
            // 🔵 V571 — শেষ বিন্দুও রাখা হয়, তাই মাংসটা ডাক্তার যেদিকে টেনেছেন
            // ঠিক সেদিকেই বেরোয় (আগে দিকটা হারিয়ে যেত)।
            x2 = nowX, y2 = nowY, hasDir = true,
            // V564: একই টানে আগের চেয়ে অনেক বড় ফোলা — TK লাইভ টেস্টে বললেন
            // আগেরটায় "মাংস বড় হচ্ছে না"।
            r = clamp(5.0 + pull * 2.30, RADIUS_MIN, RADIUS_MAX),
            s = clamp(0.30 + pull * 0.075, 0.22, BULGE_MAX)
        )
    }

    /**
     * 🔴 V564 (TK, লাইভ টেস্ট): *"ফিস্টুলার দাগ টানলে যেন কত সেন্টিমিটার সেটা
     * বোঝা যায় না"* — মাপটা দেখানোই হত না। এখন দেখানো হয়।
     *
     * ⚠️ আগের হিসাবে একটা **সত্যিকারের ভুল** ছিল: x আর y দুটোকেই শতকরা ধরে
     * একসাথে যোগ করা হত, অথচ ছবি চৌকো না হলে ১% চওড়া আর ১% লম্বা এক জিনিস নয়।
     * লম্বাটে ছবিতে তাই মাপ ভুল আসত। এখন ছবির আকার (উচ্চতা ÷ চওড়া) ধরে
     * y-টাকে আগে চওড়ার হিসাবে আনা হয়।
     *
     * @param cmWide ছবির চওড়া কত সেন্টিমিটার
     * @param aspect ছবির উচ্চতা ÷ চওড়া
     */
    fun tractCm(pts: List<Pair<Double, Double>>, cmWide: Double, aspect: Double): Double {
        var sum = 0.0
        for (i in 1 until pts.size) {
            val dx = pts[i].first - pts[i - 1].first
            val dy = (pts[i].second - pts[i - 1].second) * aspect   // লম্বাটাকে চওড়ার মাপে
            sum += Math.sqrt(dx * dx + dy * dy)
        }
        return Math.round(sum * cmWide / 100.0 * 10.0) / 10.0
    }

    /** পর্দায় যা লেখা হবে — জানা মাপ হলে "২.৪ সেমি", আন্দাজি হলে "≈ ২.৪ সেমি"। */
    fun tractLabel(cm: Double, exact: Boolean): String =
        (if (exact) "" else "≈ ") + trimZero(cm) + " সেমি"

    private fun trimZero(v: Double): String {
        val r = Math.round(v * 10.0) / 10.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString()
    }

    // ─────────── মানুষ-পড়া-যায় লেখা (প্রিন্ট ও হিস্ট্রির জন্য) ───────────

    fun readable(saved: String?): String {
        val b = parse(saved)
        if (b.pic.isBlank() && b.marks.isEmpty() && b.note.isBlank()) return ""
        val bits = mutableListOf<String>()
        val piles  = b.marks.count { it.kind == KIND_PILE }
        val bulges = b.marks.count { it.kind == KIND_BULGE }
        val tracts = b.marks.count { it.kind == KIND_TRACT }
        if (piles > 0) {
            val names = b.marks.filter { it.kind == KIND_PILE && it.label.isNotBlank() }.map { it.label }
            bits.add(if (names.isEmpty()) "ফোলা $piles টা" else "ফোলা: ${names.joinToString(", ")}")
        }
        if (bulges > 0) bits.add("মাংস ফোলানো $bulges টা")
        if (tracts > 0) bits.add("নালীর দাগ $tracts টা")
        if (b.note.isNotBlank()) bits.add(b.note)
        return bits.joinToString(" · ")
    }

    /** ছবিতে কিছু আঁকা হয়েছে কি না — খালি হলে প্রিন্টে কিছুই যাবে না। */
    fun isEmpty(saved: String?): Boolean = parse(saved).marks.isEmpty()

    // ─────────── ছোট সাহায্যকারী ───────────

    private fun d(s: String): Double = s.trim().toDoubleOrNull() ?: 0.0
    private fun clamp(v: Double, lo: Double, hi: Double) = if (v < lo) lo else if (v > hi) hi else v

    /** ফোলার জোর দুই দশমিক ঘরে — এক ঘরে লিখলে ০.৮৫ হয়ে যেত ০.৯,
        সীমা ছাড়িয়ে যেত আর ডাক্তার যা টেনেছিলেন তার চেয়ে বেশি ফুলত। */
    fun n2(v: Double): String {
        val r = Math.round(v * 100.0) / 100.0
        return if (r == Math.floor(r) && !r.isInfinite()) r.toLong().toString() else r.toString()
    }

    /** এক দশমিক ঘর — ওয়েবের `n1()`-এর হুবহু একই আচরণ। */
    fun n1(v: Double): String {
        val r = Math.round(v * 10.0) / 10.0
        return if (r == Math.floor(r) && !r.isInfinite()) r.toLong().toString() else r.toString()
    }
}
