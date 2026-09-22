package com.tkbiswas.pilesclinic.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * 🧾🔒 V1055 (০৪.০৯.২০২৬, TK-নির্দেশ) — **স্যালারি ও এক্সট্রা ইনকাম স্টেটমেন্ট, A4**।
 *
 * TK-এর কথা: *"বছরের শেষে যেন আমি স্টেটমেন্ট তুলতে পারি — স্টাফ কোন মাসে কত
 * স্যালারি পেয়েছে, এক্সট্রা ইনকাম কত পেয়েছে… পিডিএফ ফরমে যেন দেখানো যায়"*,
 * তারপর *"শুধু সারা বছর কেন, আমি যতদিন থেকে যতদিন খুশি… ব্যাংকের স্টেটমেন্ট
 * তুলতে গেলে চুজ করা যায় তারিখ থেকে তারিখ"*।
 *
 * ⛔ এটা `DietChartHtmlPrint.kt`-এর **হুবহু একই প্রমাণিত প্যাটার্ন** — ওয়েবের
 *    অনুমোদিত টেবিলের HTML+CSS একটা WebView-তে রেন্ডার করে Android-এর নিজের
 *    PrintManager দিয়ে A4-এ ছাপা/PDF। তাই **ফোন ও কম্পিউটারের কাগজ হুবহু এক**।
 * ⛔ `ClinicPdfBuilder.kt` (OWNER LOCKED) একটুও ছোঁয়া হয়নি।
 * ⛔ এখানে কোনো হিসাব কষা হয় না — যে সারিগুলো পর্দায় দেখানো হয়েছে, সেগুলোই
 *    হুবহু কাগজে যায়। তাই পর্দা আর কাগজের সংখ্যা কখনো আলাদা হবে না।
 */
object SalaryStatementHtmlPrint {

    /** পর্দার এক-একটা মাসের সারি — যা দেখানো হয়েছে, ঠিক তাই। */
    data class Row(
        val month: String,      // "Sep-26"
        val salary: Double,
        val extraPaid: Double,
        val extraDue: Double
    )

    @Suppress("StaticFieldLeak")
    private var keepAlive: WebView? = null

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun money(n: Double): String =
        "&#8377;" + com.tkbiswas.pilesclinic.native.MoneyFormat.inr(n)

    fun build(code: String, fromLabel: String, toLabel: String, rows: List<Row>): String {
        var tS = 0.0; var tP = 0.0; var tD = 0.0
        val body = StringBuilder()
        for (r in rows) {
            tS += r.salary; tP += r.extraPaid; tD += r.extraDue
            body.append("<tr><td>").append(esc(r.month)).append("</td>")
                .append("<td class='n'>").append(money(r.salary)).append("</td>")
                .append("<td class='n'>").append(money(r.extraPaid)).append("</td>")
                .append("<td class='n").append(if (r.extraDue > 0) " d" else "").append("'>")
                .append(money(r.extraDue)).append("</td>")
                .append("<td class='n'><b>").append(money(r.salary + r.extraPaid)).append("</b></td></tr>")
        }
        if (rows.isEmpty()) body.append("<tr><td colspan='5' class='mut'>No payments in this period.</td></tr>")
        return """
<!doctype html><html><head><meta charset="utf-8">
<style>
 *{-webkit-print-color-adjust:exact;print-color-adjust:exact}
 @page{size:A4;margin:14mm}
 body{font-family:sans-serif;color:#1C2A33;margin:0}
 .t{text-align:center;font-weight:800;font-size:16px;color:#0F5132;letter-spacing:.4px}
 .s{text-align:center;color:#5B6B81;font-size:12px;margin-top:3px}
 table{width:100%;border-collapse:collapse;margin-top:16px;font-size:12.5px}
 th{background:#0B4F2A;color:#fff;padding:8px 9px;text-align:left;font-size:11.5px}
 td{padding:8px 9px;border-bottom:1px solid #EDF1F5}
 .n{text-align:right;white-space:nowrap}
 .d{color:#C62828;font-weight:700}
 .mut{color:#8B98A9}
 tfoot td{border-top:2px solid #0B4F2A;border-bottom:0;font-weight:800;background:#F4F9F6}
</style></head><body>
<div class="t">SALARY &amp; EXTRA INCOME STATEMENT</div>
<div class="s">${esc(code)} &nbsp;&middot;&nbsp; ${esc(fromLabel)} &nbsp;to&nbsp; ${esc(toLabel)}</div>
<table>
 <thead><tr><th>Month</th><th class="n">Salary</th><th class="n">Extra</th>
 <th class="n">Due</th><th class="n">Total</th></tr></thead>
 <tbody>$body</tbody>
 <tfoot><tr><td>TOTAL</td><td class="n">${money(tS)}</td><td class="n">${money(tP)}</td>
 <td class="n${if (tD > 0) " d" else ""}">${money(tD)}</td>
 <td class="n"><b>${money(tS + tP)}</b></td></tr></tfoot>
</table>
</body></html>"""
    }

    fun print(activity: Activity, code: String, fromLabel: String, toLabel: String, rows: List<Row>) {
        val html = build(code, fromLabel, toLabel, rows)
        val wv = WebView(activity)
        wv.settings.javaScriptEnabled = false
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Salary Statement - $code"
                    pm.print(
                        jobName, view.createPrintDocumentAdapter(jobName),
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
        wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
