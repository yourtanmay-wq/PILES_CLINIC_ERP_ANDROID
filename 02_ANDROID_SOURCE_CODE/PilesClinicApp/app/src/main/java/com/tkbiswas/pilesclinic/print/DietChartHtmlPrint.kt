package com.tkbiswas.pilesclinic.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tkbiswas.pilesclinic.clinical.ClinicalRepository
import com.tkbiswas.pilesclinic.clinical.RoleSession

/**
 * 🥗🔒 V390 (১৬.০৮.২০২৬, TK-অনুমোদিত প্রুফ) — **DIET & LIFESTYLE CHART, A4**।
 *
 * TK-এর নির্দেশ ছিল: "সুন্দর এবং প্রফেশনাল ভাবে আগে তৈরি করুন, তারপর প্রুফ দেখান",
 * "Piles Fissure & Fistula রোগীদের জন্য", "আন্দাজে কোন কাজ করবেন না", এবং শেষে
 * "অ্যান্ড্রয়েড ও ওয়েব দুই জায়গাতেই বসান"।
 *
 * ⛔ এই ফাইলটা `RegistrationHtmlPrint.kt`-এর **হুবহু একই প্রমাণিত প্যাটার্ন** —
 *    ওয়েবের অনুমোদিত HTML+CSS একটা WebView-তে রেন্ডার করে Android-এর নিজের
 *    PrintManager দিয়ে A4-এ ছাপা/PDF। তাই ফোন ও কম্পিউটারের ছাপা কাগজ **হুবহু এক**।
 *
 * ⛔ `ClinicPdfBuilder.kt` (OWNER LOCKED — Prescription/Slip/Registration/Blood Test
 *    সবই ওটার উপর দাঁড়িয়ে) **একটুও ছোঁয়া হয়নি**। শুধু Diet Chart-এর ছাপার পথ বদলাল।
 * ⛔ কোন আইটেম বাছা হয়েছে, কী সেভ হয়, ক্লাউডে কী যায় — এক অক্ষরও বদলায়নি।
 *
 * আগের ছাপায় যে তিনটে সত্যিকারের ভুল ছিল, তিনটেই এখানে সারানো:
 *   ১) "Avoid" লেখাগুলোতেও টিক-চিহ্ন বসত → রোগী উল্টো বুঝতে পারতেন।
 *      এখন **✓ EAT (সবুজ)** আর **✗ AVOID (লাল)** সম্পূর্ণ আলাদা দুই কলামে।
 *   ২) তিনটে ভাষা এক লাইনে জোড়া লেগে যেত → এখন প্রতিটা ভাষা নিজের লাইনে।
 *   ৩) ট্যাগলাইন বাক্সটা থাকত না → এখন আছে।
 *
 * 📚 দৈনিক লক্ষ্যের সংখ্যা আন্দাজে লেখা নয়, প্রকাশিত রোগী-নির্দেশিকা থেকে:
 *    · আঁশ ২৫–৩০ গ্রাম/দিন, গরম সিটজ বাথ ~১০ মিনিট দিনে কয়েকবার — UCSF Health
 *    · জল ৬–৮ গ্লাস/দিন, পায়ের নিচে টুল (হাঁটু কোমরের উপরে) — Cambridge University Hospitals NHS
 *    · আঁশ ধীরে ধীরে বাড়ানো, সঙ্গে বেশি জল — NIDDK, NIH
 *    কাগজেই সূত্র ও "ডাক্তারের নির্দেশের বিকল্প নয়" কথাটা ছাপা থাকে।
 */
object DietChartHtmlPrint {

    // প্রিন্ট-জব শেষ না হওয়া পর্যন্ত WebView বাঁচিয়ে রাখতে হয় (নইলে খালি ছাপে)।
    @Suppress("StaticFieldLeak")
    private var keepAlive: WebView? = null

    fun print(activity: Activity, remarks: String = "") {
        val html = DietChartHtml.build(remarks)
        val wv = WebView(activity)
        wv.settings.javaScriptEnabled = false
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Diet Chart"
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    pm.print(
                        jobName, adapter,
                        com.tkbiswas.pilesclinic.native.PrintQuality.builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                    )
                } catch (e: Throwable) {
                    android.widget.Toast.makeText(
                        activity, "Could not open print — please try again",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        keepAlive = wv
        // baseURL = file:///android_asset/  → img src "www/assets/...-logo.jpg" রিজলভ হয়।
        wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
    }
}

/** ওয়েবের `wlv1DietA4Css()` + `wlv1DietA4Html()`-এর হুবহু Kotlin-পোর্ট (একই CSS, একই গঠন)। */
object DietChartHtml {

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** ওয়েবের `wlv1AddrTwo` — থানা-চিহ্নের আগে লাইন-ব্রেক, নইলে এক লাইনে। */
    private fun addr2(aRaw: String): String {
        val a = aRaw.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ: "সমস্ত জায়গায় ক্যাপিটাল লেটারই করবেন") — শুধু **দেখানোর** সময় বড় হাতে; ডেটাবেসে যা লেখা আছে তা এক অক্ষরও বদলায় না।
        if (a.isBlank()) return "-"
        val u = a.uppercase()
        // 🟢🔒 V1135 (০৬.০৯.২০২৬ — TK: *"অ্যান্ড্রয়েড থেকে যত ধরনের প্রিন্ট
        // আউট হয়, একই জিনিস একই মডেল কম্পিউটার থেকেও হতে হবে"* — মিলিয়ে দেখতে
        // গিয়ে ধরা পড়ল): ওয়েবের `wlv1AddrTwo` বাংলা **"থানা"** শব্দেও লাইন ভাঙে,
        // অথচ এই চারটে ছাপার কাগজে সেটা বাদ ছিল ⇒ একই ঠিকানা কম্পিউটারে দুই
        // লাইনে, ফোনে এক লাইনে ছাপত। অ্যাপের বাকি আটটা জায়গায় শব্দটা আগে থেকেই আছে।
        // ⛔ ইংরেজি চিহ্নগুলো বড় হাতেই থাকল (উপরে `u` বড় হাতে করা), তাই আগের
        //    আচরণ এক অক্ষরও বদলায়নি — শুধু বাংলা শব্দটা যোগ হলো।
        val markers = listOf("PS:", "P.S", "P/S", "THANA", "POLICE STATION", "থানা")
        var idx = -1
        for (m in markers) { val k = u.indexOf(m); if (k > 0 && (idx == -1 || k < idx)) idx = k }
        if (idx <= 0) return esc(a)
        val first = a.substring(0, idx).trimEnd(',', ' ').trim()
        val second = a.substring(idx).trim()
        if (first.isBlank() || second.isBlank()) return esc(a)
        return esc(first) + "<br>" + esc(second)
    }

    private fun mob(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.length >= 10) "+91 " + d.takeLast(10) else raw
    }

    /** ওয়েবের `wlv1DietA4Css()`-এর হুবহু নকল। */
    private fun css(): String = """
@page{size:A4;margin:0}
*{margin:0;padding:0;box-sizing:border-box;-webkit-print-color-adjust:exact;print-color-adjust:exact}
body{font-family:Arial,"Noto Sans Bengali","Noto Sans Devanagari",sans-serif;color:#15231C;background:#fff}
.sheet{width:210mm;height:297mm;padding:7mm 8mm 6mm;position:relative;overflow:hidden;display:flex;flex-direction:column}
.wm{position:absolute;left:50%;top:58%;transform:translate(-50%,-50%);width:105mm;opacity:.05;z-index:0;pointer-events:none}
.sheet>*:not(.wm){position:relative;z-index:1}
.head{display:grid;grid-template-columns:21mm 1fr;gap:5mm;align-items:center;padding-bottom:2mm}
.head img{width:21mm;height:21mm;object-fit:contain}
.hc{text-align:center}
.hc h1{font-size:19.5pt;line-height:1;color:#0A5428;letter-spacing:.2px;font-weight:900}
.hc .br{font-size:8.5pt;font-weight:800;color:#0A5428;letter-spacing:1.6px;margin-top:1.2mm}
.rule{height:1.1px;background:#C99A19;margin:1.2mm 12mm}
.hc .ad{font-size:9pt;color:#1B2A22}
.tag{margin:2mm 0 0;border:1px solid #0A5428;border-radius:2mm;text-align:center;padding:1.1mm 3mm}
.tag b{display:block;font-size:8.2pt;color:#0A5428}
.tag span{display:block;font-size:6.9pt;font-weight:bold;color:#3A4A40;letter-spacing:.3px}
.sep{height:1.3px;background:#0A5428;margin:2.6mm 0 0}
.title{margin-top:2.2mm;background:#0A5428;color:#fff;text-align:center;border-radius:1.5mm;padding:1.6mm 0}
.title b{font-size:12pt;letter-spacing:1.4px}
.title span{display:block;font-size:7.4pt;letter-spacing:2.6px;color:#CDEFDC;margin-top:.4mm}
.pt{margin-top:2mm;border:.8px solid #C7D3CB;border-radius:1.5mm;display:grid;grid-template-columns:1fr 1fr 1fr;gap:.3mm 4mm;padding:1.3mm 3mm;font-size:8.2pt}
.pt div b{color:#0A5428}.pt .full{grid-column:1/4}
.tg{margin-top:1.9mm;display:grid;grid-template-columns:repeat(4,1fr);gap:2.2mm}
.tg>div{border:.9px solid #BFE9CE;background:#F2FBF5;border-radius:1.8mm;text-align:center;padding:1.1mm 1mm}
.tg .ic{font-size:11pt;line-height:1}
.tg .vl{font-size:11.5pt;font-weight:900;color:#0A5428;line-height:1.05;margin-top:.6mm}
.tg .lb{font-size:6.8pt;font-weight:bold;color:#3A4A40;letter-spacing:.3px;margin-top:.3mm}
.cols{margin-top:2.1mm;display:grid;grid-template-columns:1fr 1fr;gap:3mm;flex:0 0 auto}
.box{border:.9px solid #D5DED8;border-radius:1.8mm;overflow:hidden;display:flex;flex-direction:column}
.box>h3{font-size:9pt;letter-spacing:.8px;padding:1.5mm 3mm;color:#fff}
.ok>h3{background:#0A7C3F}.no>h3{background:#B42318}
.box ul{list-style:none;padding:1mm 2.4mm}
.box li{display:grid;grid-template-columns:4mm 1fr;gap:1.2mm;padding:1.15mm 0;font-size:7.6pt;line-height:1.2;border-bottom:.6px dotted #DDE5DF}
.box li:last-child{border-bottom:0}
.mk{font-size:8.6pt;font-weight:900;line-height:1.2}
.ok .mk{color:#0A7C3F}.no .mk{color:#B42318}
.en{display:block;font-size:8.6pt;font-weight:bold;color:#15231C;line-height:1.2}
.bn{display:block;font-size:7.6pt;color:#33463D;line-height:1.28;margin-top:.3mm}
.hi{display:block;font-size:7.6pt;color:#33463D;line-height:1.28}
.gold{margin-top:1.8mm;border:.9px solid #E2C77A;background:#FFFBEF;border-radius:1.8mm;padding:1.4mm 3mm}
.gold h4{font-size:8.2pt;color:#8A6116;letter-spacing:.6px;margin-bottom:.8mm}
.gold ol{list-style:none;display:grid;grid-template-columns:1fr 1fr;gap:.3mm 4mm}
.gold li{font-size:7.2pt;color:#2A3630;line-height:1.2;padding-left:4mm;position:relative}
.gold li b{color:#8A6116;position:absolute;left:0}.gold li i{font-style:normal;color:#54615A}
.warn{margin-top:1.6mm;border:.9px solid #F0B7B0;background:#FDECEA;border-radius:1.8mm;padding:1.2mm 3mm;font-size:7.3pt;color:#8E2A1E;line-height:1.25}
.warn.rem{border-color:#C7D3CB;background:#F7FAF8;color:#15231C}
.sign{margin-top:1.6mm;display:grid;grid-template-columns:1fr auto 1fr;align-items:end;gap:6mm}
.sign .ln{border-top:.9px solid #15231C;text-align:center;padding-top:1.2mm}
.sign .ln b{display:block;font-size:8.4pt;font-weight:900;color:#15231C;letter-spacing:.2px}
.sign .ln small{display:block;font-size:6.8pt;color:#54615A;margin-top:.3mm}
.vfy{text-align:center}.vfy b{display:block;font-size:7.8pt;color:#0A5428}
.vfy small{display:block;font-size:6.4pt;color:#54615A}
.vfy .bar{height:7mm;width:32mm;margin:.8mm auto .4mm;background:repeating-linear-gradient(90deg,#15231C 0 .5mm,#fff .5mm 1.05mm)}
.vfy .id{font-size:6.4pt;letter-spacing:.6px}
.next{margin-top:1.6mm;border:.8px solid #C7D3CB;border-radius:1.5mm;padding:1.1mm 3mm;font-size:8pt;display:flex;justify-content:space-between}
.thanks{margin-top:1.6mm;background:#0A5428;color:#fff;text-align:center;font-size:7.4pt;letter-spacing:.5px;padding:1.4mm 0;border-radius:1.2mm}
.src{margin-top:1.4mm;font-size:5.9pt;color:#7A857F;text-align:center;line-height:1.35}
""".trimIndent()

    /** একটা সারি — ইংরেজি/বাংলা/হিন্দি তিন লাইনে (পর্দার CheckBox-এর মতোই)। */
    private fun row(name: String, mark: String): String {
        val q = name.split("\n")
        val sb = StringBuilder("<li><span class=\"mk\">$mark</span><span>")
        sb.append("<span class=\"en\">").append(esc(q.getOrElse(0) { "" })).append("</span>")
        if (q.size > 1 && q[1].isNotBlank()) sb.append("<span class=\"bn\">").append(esc(q[1])).append("</span>")
        if (q.size > 2 && q[2].isNotBlank()) sb.append("<span class=\"hi\">").append(esc(q[2])).append("</span>")
        return sb.append("</span></li>").toString()
    }

    fun build(remarks: String): String {
        val info = BranchCatalog.byName(RoleSession.currentPatientBranch)
        // ⛔ বাছাই পর্দাতেই হয়ে আছে — এখানে শুধু পড়া হচ্ছে, কিছু বদলানো হচ্ছে না।
        val selected = ClinicalRepository.currentDiet.filter { it.isSelected }
        val okRows = selected.filter { it.category == "Allowed" }.joinToString("") { row(it.name, "✓") }
            .ifBlank { "<li><span class=\"mk\"></span><span><span class=\"en\">—</span></span></li>" }
        val noRows = selected.filter { it.category == "Avoid" }.joinToString("") { row(it.name, "✗") }
            .ifBlank { "<li><span class=\"mk\"></span><span><span class=\"en\">—</span></span></li>" }

        val isKish = info.id == "kishanganj"
        val brLine = if (isKish) "" else
            "<div class=\"br\">${esc(info.displayName.uppercase())} BRANCH</div>"
        val pid = esc(RoleSession.displayId().ifBlank { "-" })
        // PrintMappers.patientAgeSex()-এর হুবহু একই নিয়ম (Age / Sex)
        val ageSex = listOf(RoleSession.currentPatientAge.trim(), RoleSession.currentPatientSex.trim())
            .filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "-" }
        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            .format(java.util.Date())

        val remarkBlock = if (remarks.isBlank()) "" else
            "<div class=\"warn rem\"><b>Advice / Remarks:</b> ${esc(remarks)}</div>"

        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>${css()}</style></head><body><div class=\"sheet\">" +
            "<img class=\"wm\" src=\"${info.logoAssetPath}\">" +
            "<div class=\"head\"><img src=\"${info.logoAssetPath}\"><div class=\"hc\">" +
            "<h1>${esc(info.clinicName)}</h1>$brLine<div class=\"rule\"></div>" +
            "<div class=\"ad\">${esc(info.addressLine)} &nbsp;|&nbsp; Mob: ${esc(info.phoneLine)} &nbsp;|&nbsp; Helpline: ${esc(BranchCatalog.HELPLINE)}</div>" +
            "<div class=\"tag\"><b>WE PROVIDE AYURVEDA KSHAR SUTRA THERAPY IN PILES, FISSURE &amp; FISTULA</b>" +
            "<span>MOST SUCCESSFUL TREATMENT WITH HIGH SUCCESS RATE</span></div></div></div><div class=\"sep\"></div>" +
            "<div class=\"title\"><b>DIET &amp; LIFESTYLE CHART</b><span>PILES &nbsp;·&nbsp; FISSURE &nbsp;·&nbsp; FISTULA</span></div>" +
            "<div class=\"pt\">" +
            "<div><b>Patient Name</b> : ${esc(RoleSession.currentPatientName.uppercase())}</div>" +
            "<div><b>Age / Sex</b> : ${esc(ageSex)}</div>" +
            "<div><b>Date</b> : ${esc(dateStr)}</div>" +
            "<div><b>Patient ID</b> : $pid</div>" +
            "<div><b>Mobile</b> : ${esc(mob(RoleSession.currentPatientMobile))}</div>" +
            "<div><b>Branch</b> : ${esc(info.displayName)}</div>" +
            "<div class=\"full\"><b>Address</b> : ${addr2(RoleSession.currentPatientAddress)}</div></div>" +
            "<div class=\"tg\">" +
            "<div><div class=\"ic\">🌾</div><div class=\"vl\">25–30 g</div><div class=\"lb\">FIBRE / DAY<br>আঁশ · फाइबर</div></div>" +
            "<div><div class=\"ic\">💧</div><div class=\"vl\">6–8 glasses</div><div class=\"lb\">WATER / DAY<br>জল · पानी</div></div>" +
            "<div><div class=\"ic\">🛁</div><div class=\"vl\">10 min × 2–3</div><div class=\"lb\">WARM SITZ BATH<br>সিটজ বাথ · सिट्ज़ बाथ</div></div>" +
            "<div><div class=\"ic\">🚶</div><div class=\"vl\">30 min</div><div class=\"lb\">WALK / DAY<br>হাঁটা · सैर</div></div></div>" +
            "<div class=\"cols\">" +
            "<div class=\"box ok\"><h3>✓ &nbsp;EAT / DO &nbsp;— &nbsp;খাবেন ও করবেন</h3><ul>$okRows</ul></div>" +
            "<div class=\"box no\"><h3>✗ &nbsp;AVOID &nbsp;— &nbsp;এড়িয়ে চলুন</h3><ul>$noRows</ul></div></div>" +
            "<div class=\"gold\"><h4>★ GOLDEN RULES &nbsp;— &nbsp;সোনালি নিয়ম</h4><ol>" +
            "<li><b>1</b>Go to the toilet as soon as you feel the urge — never hold it. <i>বেগ এলেই যাবেন, চেপে রাখবেন না।</i></li>" +
            "<li><b>2</b>Do not strain or push. Do not sit longer than needed. <i>জোর করবেন না, বেশিক্ষণ বসে থাকবেন না।</i></li>" +
            "<li><b>3</b>Rest your feet on a low stool so knees stay above hips. <i>পায়ের নিচে টুল রাখুন, হাঁটু কোমরের উপরে।</i></li>" +
            "<li><b>4</b>Increase fibre slowly over 1–2 weeks, with extra water. <i>আঁশ ধীরে ধীরে বাড়ান, সঙ্গে বেশি জল।</i></li>" +
            "<li><b>5</b>Keep the area clean and dry; pat, do not rub. <i>জায়গাটা পরিষ্কার ও শুকনো রাখুন, ঘষবেন না।</i></li>" +
            "<li><b>6</b>Take medicines exactly as written on the prescription. <i>ওষুধ প্রেসক্রিপশন মতোই খাবেন।</i></li>" +
            "</ol></div>" +
            remarkBlock +
            "<div class=\"warn\"><b>⚠ Report at once</b> if there is heavy bleeding, fever, increasing pain, pus discharge, or you cannot pass stool or urine. &nbsp;" +
            "<b>এখনই জানান</b> — বেশি রক্তপাত, জ্বর, ব্যথা বাড়া, পুঁজ, বা পায়খানা-প্রস্রাব বন্ধ হলে।</div>" +
            // 🔒 V391 (TK-নির্দেশ, ১৬.০৮.২০২৬): বাঁয়ে TK BISWAS, মাঝে বারকোড,
            // ডানে Dr. K.H MANDAL — Prescription প্রিন্টেও ঠিক এই দুজনই বসে।
            "<div class=\"sign\"><div class=\"ln\"><b>TK BISWAS</b><small>Founder &amp; Consultant</small></div>" +
            "<div class=\"vfy\"><b>Document Digitally Verified</b><small>No Physical Signature Required</small>" +
            "<div class=\"bar\"></div><div class=\"id\">$pid</div></div>" +
            "<div class=\"ln\"><b>Dr. K.H MANDAL</b><small>(B.A.M.S) Regd 12386</small></div></div>" +
            "<div class=\"next\"><span>Next Follow-up Date : <b>_____________________</b></span><span>Chart valid for : <b>30 days</b></span></div>" +
            "<div class=\"thanks\">All treatments are Ayurvedic &amp; Natural &nbsp;|&nbsp; Bring this chart on your next visit</div>" +
            "<div class=\"src\">Daily targets follow published patient guidance — fibre 25–30 g/day and warm sitz bath ~10 minutes several times daily (UCSF Health); fluid 6–8 glasses/day and footstool position (Cambridge University Hospitals NHS); increase fibre gradually with fluids (NIDDK, NIH). This chart is general advice and does not replace your doctor’s instructions.</div>" +
            "</div></body></html>"
    }
}
