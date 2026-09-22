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
 * 🩸🔒 V596 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-ফটো দেখে) —
 * **BLOOD TEST / INVESTIGATION ADVICE, A4**।
 *
 * TK-এর কথা (ধাপে ধাপে, প্রতিটাতেই ডেমো দেখিয়ে অনুমোদন নেওয়া):
 *   · *"এটাকে প্রফেশনাল লুক বানাতে হবে · বড় হসপিটালে কেমন দেখতে হয় ঠিক সেরকম"*
 *   · *"হেডার ও একদম নিচের পাঠ প্রেসক্রিপশনের মতোই থাকবে"*
 *   · *"জল ছবি টা পুরো আসতে হবে"*
 *   · *"দাগ গুলি এত ক্লিয়ার থাকবে না, হাল্কা দাগ থাকতে হবে"*
 *   · *"এক্সেল এর দাগগুলো খুব উজ্জ্বল লাগছে, একটু হালকা করুন"*
 *
 * ⛔ এই ফাইলটা `DietChartHtmlPrint.kt`-এর **হুবহু একই প্রমাণিত প্যাটার্ন**
 *    (V390, TK-অনুমোদিত): অনুমোদিত HTML+CSS একটা WebView-তে এঁকে Android-এর
 *    নিজের PrintManager দিয়ে A4-এ ছাপা/PDF। তাই ফোন ও কম্পিউটারের কাগজ এক।
 * ⛔ `ClinicPdfBuilder.kt` (OWNER LOCKED) **একটুও ছোঁয়া হয়নি** — Prescription ·
 *    Medicine Slip · Registration · Payment Receipt সব আগের পথেই ছাপে।
 * ⛔ কোন পরীক্ষা বাছা হলো, কী সেভ হয়, ক্লাউডে কী যায় — এক অক্ষরও বদলায়নি;
 *    শুধু কাগজের চেহারা।
 */
object InvestigationHtmlPrint {

    // প্রিন্ট-জব শেষ না হওয়া পর্যন্ত WebView বাঁচিয়ে রাখতে হয় (নইলে খালি ছাপে)।
    @Suppress("StaticFieldLeak")
    private var keepAlive: WebView? = null

    fun print(activity: Activity, remarks: String = "") {
        val html = InvestigationHtml.build(remarks)
        val wv = WebView(activity)
        wv.settings.javaScriptEnabled = false
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Blood Test - Investigation Advice"
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

/** ওয়েবের `wlv1InvestigationA4()`-এর হুবহু চেহারা, নিজের CSS-এ (WebView-এ
 *  `styles.css` থাকে না, তাই ডায়েট চার্টের মতোই CSS এখানেই লেখা)। */
object InvestigationHtml {

    /** ফাঁকা ছাপা লাইন — মোট এতগুলো সারি অন্তত থাকবে (হাতে লেখার জায়গা)। */
    private const val MIN_ROWS = 13

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

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
        return if (d.length >= 10) d.takeLast(10) else raw
    }

    private fun today(): String =
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
            .format(java.util.Date())

    private fun css(): String = """
@page{size:A4;margin:0}
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:Arial,"Noto Sans",sans-serif;color:#15231C;background:#fff}
.sheet{width:210mm;height:297mm;padding:6mm 7mm 5mm;position:relative;overflow:hidden;display:flex;flex-direction:column}
.head{display:grid;grid-template-columns:21mm 1fr 21mm;gap:4mm;align-items:center;padding-bottom:1.6mm}
.head img{width:21mm;height:21mm;object-fit:contain;border-radius:50%}
.hc{text-align:center}
.hc h1{font-size:19.5pt;line-height:1;color:#0A5428;letter-spacing:.2px;font-weight:900}
.hc .br{font-size:8.5pt;font-weight:800;color:#0A5428;letter-spacing:1.6px;margin-top:1.2mm}
.hc .ad{font-size:8.6pt;color:#1B2A22;font-weight:bold;margin-top:.9mm}
.sep{height:1.3px;background:#0A5428;margin:1.6mm 0 0}
.tag{margin:2mm 0 0;border:1px solid #0A5428;border-radius:2mm;text-align:center;padding:1.2mm 3mm}
.tag b{display:block;font-size:8.2pt;color:#0A5428}
.tag span{display:block;font-size:6.9pt;font-weight:bold;color:#3A4A40;letter-spacing:.3px}
.title{margin-top:2mm;background:#EEF6F1;border-top:1px solid #0A5428;border-bottom:1px solid #0A5428;
  text-align:center;padding:1.5mm 0}
.title b{font-size:13pt;letter-spacing:.9px;color:#0A5428}
.pt{margin-top:2mm;border:.8px solid #C7D3CB;border-radius:1.8mm;display:grid;
  grid-template-columns:1fr 1fr;gap:0 6mm;padding:2mm 3mm;font-size:9pt}
.pt .c{display:grid;grid-template-columns:26mm 3mm 1fr;row-gap:1.1mm;align-items:start}
.pt .c:first-child{border-right:.8px solid #C7D3CB;padding-right:5mm}
.pt b{color:#15231C;font-weight:bold}
.box{margin-top:2mm;border:.9px solid #DDE5DF;border-radius:2mm;padding:4mm 4mm 3mm;
  position:relative;flex:1 1 auto;display:flex;flex-direction:column;min-height:0}
.wm{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);
  width:140mm;height:140mm;opacity:.022;z-index:0;pointer-events:none}
.wm img{width:140mm;height:140mm;object-fit:contain;filter:grayscale(1);border-radius:50%}
.box>*:not(.wm){position:relative;z-index:1}
.bt{text-align:center;color:#0A5428;font-size:12.5pt;letter-spacing:.6px;font-weight:bold;
  border-bottom:1px solid #D4B15F;padding-bottom:2mm;margin-bottom:4mm}
table.inv{width:100%;border-collapse:collapse;border:1px solid #DDE5DF;background:transparent}
table.inv th{background:#0A5428;color:#fff;letter-spacing:.9px;font-size:8.4pt;text-align:left;
  padding:2.2mm 3mm;border:0;-webkit-print-color-adjust:exact;print-color-adjust:exact}
table.inv th.sl,table.inv td.sl{width:13mm;text-align:center}
table.inv th.sl{border-right:1px solid rgba(255,255,255,.28)}
table.inv td{padding:2.4mm 3mm;font-size:9.6pt;font-weight:bold;color:#15231C;background:transparent;
  border:0;border-bottom:1px solid #E8EEEA}
table.inv td.sl{color:#0A5428;border-right:1px solid #E8EEEA}
table.inv tr.alt td{background:rgba(10,84,40,.030);-webkit-print-color-adjust:exact;print-color-adjust:exact}
table.inv tr.blank td{height:8mm;padding:0}
table.inv tr.tot td{background:rgba(10,84,40,.07);letter-spacing:.6px;font-size:8pt;color:#22352C;
  border:0;border-top:1px solid #DDE5DF;padding:2mm 3mm;
  -webkit-print-color-adjust:exact;print-color-adjust:exact}
.rem{margin-top:3.5mm;font-size:9.2pt;line-height:1.35}
.rem b{color:#15231C}
.dates{margin-top:3.5mm;display:grid;grid-template-columns:1fr 1fr;gap:6mm;
  padding:2.5mm 3mm;border:1px solid #C7D3CB;border-radius:2mm;font-size:9pt}
.dates b{color:#0A5428}
.grow{flex:1 1 auto}
.sign{margin-top:3mm;display:grid;grid-template-columns:1fr auto 1fr;gap:6mm;align-items:end}
.sign .ln{border-top:1px solid #15231C;text-align:center;padding-top:1.4mm}
.sign .ln b{display:block;font-size:9pt;font-weight:bold;color:#15231C}
.sign .ln small{display:block;font-size:7.2pt;color:#54615A;margin-top:.4mm}
.vfy{text-align:center}
.vfy .bar{height:7mm;width:34mm;margin:0 auto .8mm;
  background:repeating-linear-gradient(90deg,#15231C 0 .5mm,#fff .5mm 1.05mm);
  -webkit-print-color-adjust:exact;print-color-adjust:exact}
.vfy b{display:block;font-size:7.6pt;color:#0A5428}
.strip{margin-top:2.5mm;background:#0A5428;color:#fff;text-align:center;font-size:7.8pt;
  letter-spacing:.4px;padding:1.6mm 0;-webkit-print-color-adjust:exact;print-color-adjust:exact}
"""

    fun build(remarks: String = ""): String {
        val branch = BranchCatalog.byName(RoleSession.currentPatientBranch)
        val isKishanganj = branch.id == "kishanganj"
        val logo = branch.logoAssetPath
        val tests = ClinicalRepository.currentInvestigations.filter { it.isSelected }.map { it.name }

        val sb = StringBuilder()
        for ((i, t) in tests.withIndex()) {
            sb.append("<tr").append(if (i % 2 == 1) " class=\"alt\"" else "").append(">")
                .append("<td class=\"sl\">").append(String.format(java.util.Locale.US, "%02d", i + 1)).append("</td>")
                .append("<td>").append(esc(t)).append("</td></tr>")
        }
        for (i in tests.size until MIN_ROWS) {
            sb.append("<tr class=\"blank").append(if (i % 2 == 1) " alt" else "").append("\">")
                .append("<td class=\"sl\"></td><td></td></tr>")
        }

        val brLine = if (isKishanganj) "" else
            "<div class=\"br\">" + esc(RoleSession.currentPatientBranch.uppercase()) + " BRANCH</div>"
        val pid = RoleSession.displayId().ifBlank { "-" }
        // ⛔ বয়স ও লিঙ্গ প্রকল্পের নিজের দুটো ঘর থেকেই (PrintMappers-এর মতোই)
        val age = RoleSession.currentPatientAge.trim().ifBlank { "-" }
        val sex = RoleSession.currentPatientSex.trim().ifBlank { "-" }

        return """<!DOCTYPE html><html><head><meta charset="utf-8"><style>${css()}</style></head><body>
<div class="sheet">
  <div class="head">
    <img src="$logo">
    <div class="hc"><h1>${esc(branch.clinicName)}</h1>$brLine
      <div class="ad">${esc(branch.addressLine)} &nbsp;·&nbsp; Mobile: ${esc(branch.phoneLine)} &nbsp;·&nbsp; Helpline: ${esc(BranchCatalog.HELPLINE)}</div></div>
    <img src="$logo" style="visibility:hidden">
  </div>
  <div class="sep"></div>
  <div class="tag"><b>WE PROVIDE AYURVEDA KSHAR SUTRA THERAPY IN PILES, FISSURE &amp; FISTULA</b>
    <span>MOST SUCCESSFUL TREATMENT WITH HIGH SUCCESS RATE</span></div>
  <div class="title"><b>BLOOD TEST / INVESTIGATION ADVICE</b></div>
  <div class="pt">
    <div class="c">
      <b>Patient Name</b><span>:</span><span>${esc(RoleSession.currentPatientName.uppercase())}</span>
      <b>Age</b><span>:</span><span>${esc(age)}</span>
      <b>Patient ID</b><span>:</span><span>${esc(pid)}</span>
      <b>Mobile</b><span>:</span><span>${esc(mob(RoleSession.currentPatientMobile))}</span>
    </div>
    <div class="c">
      <b>Date</b><span>:</span><span>${today()}</span>
      <b>Sex</b><span>:</span><span>${esc(sex.uppercase())}</span>
      <b>Diseases</b><span>:</span><span>${esc(RoleSession.currentPatientDisease)}</span>
      <b>Address</b><span>:</span><span>${addr2(RoleSession.currentPatientAddress)}</span>
    </div>
  </div>
  <div class="box">
    <div class="wm"><img src="$logo"></div>
    <div class="bt">BLOOD TEST / INVESTIGATION ADVICE</div>
    <table class="inv">
      <tr><th class="sl">SL.</th><th>INVESTIGATION ADVISED</th></tr>
      $sb
      <tr class="tot"><td colspan="2">TOTAL ${tests.size} INVESTIGATION(S) ADVISED</td></tr>
    </table>
    ${if (remarks.isNotBlank()) "<div class=\"rem\"><b>Advice / Remarks:</b> " + esc(remarks) + "</div>" else ""}
    <div class="dates"><span><b>Report Collection Date</b> : ______________</span>
      <span><b>Next Follow-up Date</b> : ______________</span></div>
    <div class="grow"></div>
  </div>
  <div class="sign">
    <div class="ln"><b>TK BISWAS</b><small>Founder &amp; Consultant</small></div>
    <div class="vfy"><div class="bar"></div><b>Document Digitally Verified</b></div>
    <div class="ln"><b>Dr. K.H MANDAL</b><small>(B.A.M.S) Regd 12386</small></div>
  </div>
  <div class="strip">All treatments are Ayurvedic &amp; Natural &nbsp;|&nbsp; Bring this prescription for next visit</div>
</div></body></html>"""
    }
}
