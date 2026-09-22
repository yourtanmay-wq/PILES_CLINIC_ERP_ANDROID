package com.tkbiswas.pilesclinic.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * 🗓️🔒 V1199 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **STAFF ATTENDANCE SHEET, A4**।
 *
 * TK-এর কথা (হুবহু):
 *  · *"date in time out time … তারপরের কলমে কত ঘন্টা হল"*
 *  · *"ছুটিতে থাকলেও ছুটি দেখাবে, ওয়ার্ক ফ্রম হোম দিলে ওয়ার্ক ফ্রম হোম দেখাবে,
 *     অন্য ব্রাঞ্চে গিয়ে ডিউটি করলে সেটাও শো করবে"*
 *  · *"গুগল শিটের মতন চারি সাইডে বক্স আকার থাকবে"*
 *  · *"অন্যান্য সবকিছুতে যেমন হেডার থাকে ঠিক এখানেও … বাঁদিকে ক্লিনিকের লোগো,
 *     তারপর ক্লিনিকের নাম, তার নিচে ঠিকানা"*
 *  · *"এটার নাম হবে স্টাফ অ্যাটেনডেন্স শিট … স্টাফের সম্পূর্ণ নাম, মোবাইল নাম্বার
 *     এবং তার ঠিকানা থাকতে হবে"*
 *  · *"আমি এক পেজের মধ্যেই প্রিন্ট আউট চাই"*
 *
 * ⛔ `SalaryStatementHtmlPrint.kt`-এর **হুবহু একই প্রমাণিত পথ** — HTML একটা
 *    WebView-তে এঁকে Android-এর নিজের PrintManager দিয়ে A4-এ ছাপা/PDF।
 * ⛔ এখানে কোনো হিসাব কষা হয় না — যে সারিগুলো পর্দায় দেখানো হয়েছে, হুবহু
 *    সেগুলোই কাগজে যায়, তাই পর্দা ও কাগজ কখনো আলাদা হতে পারে না।
 * ⛔ ৩১ দিনের মাসেও **এক পাতাতেই** ধরে — মাপ মেপে ছোট করা হয়েছে।
 */
object AttendanceSheetHtmlPrint {

    /** পর্দার এক-একটা দিনের সারি — যা দেখানো হয়েছে, ঠিক তাই। */
    data class Row(
        val date: String,        // "01/09/2026"
        val inTime: String,      // "10.02 AM" · "—"
        val outTime: String,     // "5.10 PM" · "—" · "" (missing হলে ফাঁকা)
        val outMissing: Boolean,
        val hours: String,       // "7h 08m"
        val tag: String,         // "LEAVE (Fever)" · "WORK FROM HOME" · "COOCH BEHAR" · ""
        val tagKind: String      // "lv" · "wf" · "br" · ""
    )

    /** 📊 V1204 — মাসের পারফরম্যান্স (যা Monthly Report-এ দেখায়, হুবহু সেই ঘরগুলো)। */
    data class Perf(
        val enquiries: String, val registrations: String,
        val appCalls: String, val outsideCalls: String,
        val totalCalls: String, val leaveDays: String
    )

    @Suppress("StaticFieldLeak")
    private var keepAlive: WebView? = null

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun build(
        branchName: String, staffName: String, staffCode: String, mobile: String,
        address: String, monthLabel: String, printedOn: String,
        rows: List<Row>, totalHours: String,
        monthHoursText: String, salaryText: String, rateText: String, payableText: String,
        /* 📊🔒 V1204 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"ইন টাইম আউট
           টাইম এবং কত ঘন্টা কাজ করেছে, সারা মাসে কতগুলি Enquiry · Registration ·
           App Call · Outside call — অর্থাৎ স্টাফের সম্পূর্ণ পারফরমেন্স সিট আমি যেন পাই"*।
           ⇒ হাজিরার শিটেই উপরে একটা "MONTHLY PERFORMANCE" বাক্স। ঘরগুলো ঐচ্ছিক —
             না পাঠালে (বা পড়া ব্যর্থ হলে "…") শিট আগের মতোই ছাপা হয়।
           ⛔ কোনো হিসাব এখানে কষা হয় না — যা পাঠানো হয়, হুবহু তাই কাগজে যায়। */
        perf: Perf? = null
    ): String {
        val info = BranchCatalog.byName(branchName)
        val body = StringBuilder()
        for (r in rows) {
            val outCell = if (r.outMissing) "<td class='ms'>MISSING</td>" else "<td>" + esc(r.outTime) + "</td>"
            val tagHtml = if (r.tag.isBlank()) "" else "<small>&middot; " + esc(r.tag) + "</small>"
            body.append("<tr><td class='d'>").append(esc(r.date)).append("</td>")
                .append("<td>").append(esc(r.inTime)).append("</td>")
                .append(outCell)
                .append("<td class='").append(r.tagKind).append("'>").append(esc(r.hours)).append(tagHtml).append("</td></tr>")
        }
        if (rows.isEmpty()) body.append("<tr><td colspan='4' class='mut'>No attendance in this month.</td></tr>")
        // 📊 V1204 — পারফরম্যান্সের বাক্স (না পাঠালে কিছুই বসে না)
        val perfHtml = if (perf == null) "" else {
            fun c(label: String, v: String) =
                "<td><span class='pl'>" + label + "</span>" + esc(v.ifBlank { "-" }) + "</td>"
            "<div class='blk'><div class='h'>MONTHLY PERFORMANCE &nbsp;&middot;&nbsp; " +
                esc(monthLabel).uppercase() + "</div><table class='perf'><tr>" +
                c("NEW ENQUIRY", perf.enquiries) + c("REGISTRATION", perf.registrations) +
                c("APP CALLS", perf.appCalls) + c("OUTSIDE CALLS", perf.outsideCalls) +
                c("TOTAL CALLS", perf.totalCalls) + c("LEAVE DAYS", perf.leaveDays) +
                "</tr></table></div>"
        }
        return """
<!doctype html><html><head><meta charset="utf-8">
<style>
 *{-webkit-print-color-adjust:exact;print-color-adjust:exact}
 @page{size:A4;margin:8mm}
 body{font-family:sans-serif;color:#1C2A33;margin:0}
 .gold{height:5px;background:linear-gradient(90deg,#b8912f,#e6c65c,#b8912f)}
 .gbar{height:3px;background:#0f5132}
 .lh{display:flex;align-items:center;gap:10px;padding:7px 10px 5px}
 .lh img{width:46px;height:46px;border-radius:50%}
 .cn{font-size:16px;font-weight:800;color:#0f5132;line-height:1}
 .tag{font-size:8.5px;font-weight:700;color:#b8912f;letter-spacing:2px;margin-top:2px;text-transform:uppercase}
 .addr{font-size:9px;color:#3b4650;margin-top:2px}.addr b{color:#0f5132}
 .tb{background:#0f5132;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:5px 10px}
 .tb .t{font-size:11.5px;font-weight:800;letter-spacing:2px}
 .tb .r{font-size:8.5px;color:#cfe6d8;text-align:right;line-height:1.4}
 .pi{display:flex;gap:16px;padding:6px 10px 4px;font-size:9.5px}
 .pi .c{flex:1}.pi .r{padding:1px 0}.pi b{color:#0f5132}
 table{width:100%;border-collapse:collapse;font-size:9.5px}
 th{background:#0B4F2A;color:#fff;padding:4px 6px;font-size:8.5px;border:1px solid #0B4F2A}
 td{padding:2.5px 6px;border:1px solid #D6DEE6;text-align:center;line-height:1.25}
 td.d{text-align:left;font-weight:700}
 td small{font-size:8px;font-weight:700;padding-left:5px}
 .lv small{color:#123E8C}.wf small{color:#8A5A00}.br small{color:#0A7C3F}
 .ms{color:#C62828;font-weight:700}
 .mut{color:#8B98A9}
 tfoot td{border-top:2px solid #0B4F2A;font-weight:800;background:#F4F9F6}
 .blk{margin-top:7px;border:1px solid #D6DEE6;border-radius:5px;overflow:hidden;margin-bottom:7px}
 .blk .h{background:#0B4F2A;color:#fff;padding:4px 9px;font-size:8.5px;font-weight:800;letter-spacing:1px}
 .perf td{text-align:center;font-weight:800;font-size:13px;color:#0B2B59;padding:5px 4px}
 .perf td .pl{display:block;font-size:8px;font-weight:700;color:#6B7280;letter-spacing:.6px}
 .calc{margin-top:7px;border:1px solid #D6DEE6;border-radius:5px;overflow:hidden}
 .calc .h{background:#0B4F2A;color:#fff;padding:4px 9px;font-size:8.5px;font-weight:800;letter-spacing:1px}
 .calc td{text-align:center}.calc td.d{width:60%}
 .fn{border-top:1px solid #e4ebe6;text-align:center;font-size:8px;color:#8a949e;padding:5px 0 4px;margin-top:7px}
 .wrap{padding:2px 10px 0}
</style></head><body>
<div class="gold"></div>
<div class="lh"><img src="${info.logoAssetPath}">
 <div><div class="cn">${esc(info.clinicName)}</div>
 <div class="tag">Ayurveda &amp; Anorectal Diseases</div>
 <div class="addr"><b>${esc(info.displayName)}:</b> ${esc(info.addressLine)} &nbsp;|&nbsp; &#9742; ${esc(info.phoneLine)} &nbsp;|&nbsp; &#9742; ${esc(BranchCatalog.HELPLINE)}</div></div></div>
<div class="gbar"></div>
<div class="tb"><span class="t">STAFF PERFORMANCE SHEET</span><span class="r">${esc(monthLabel)}<br>Printed ${esc(printedOn)}</span></div>
<div class="pi">
 <div class="c"><div class="r"><b>Staff Name</b> : ${esc(staffName)}</div>
  <div class="r"><b>Staff Code</b> : ${esc(staffCode)}</div>
  <div class="r"><b>Branch</b> : ${esc(branchName)}</div></div>
 <div class="c"><div class="r"><b>Mobile</b> : ${esc(mobile.ifBlank { "-" })}</div>
  <div class="r"><b>Address</b> : ${esc(address.ifBlank { "-" })}</div></div>
</div>
<div class="wrap">
$perfHtml
<table>
 <thead><tr><th>DATE</th><th>IN TIME</th><th>OUT TIME</th><th>HOURS</th></tr></thead>
 <tbody>$body</tbody>
 <tfoot><tr><td colspan="3">TOTAL HOURS WORKED</td><td>${esc(totalHours)}</td></tr></tfoot>
</table>
<div class="calc"><div class="h">SALARY CALCULATION &nbsp;&middot;&nbsp; ${esc(monthLabel).uppercase()}</div>
<table>
 <tr><td class="d">Month hours</td><td>${esc(monthHoursText)}</td></tr>
 <tr><td class="d">Monthly salary (set)</td><td>${esc(salaryText)}</td></tr>
 <tr><td class="d">Rate per hour</td><td>${esc(rateText)}</td></tr>
 <tr><td class="d">Hours worked</td><td>${esc(totalHours)}</td></tr>
 <tr style="background:#F4F9F6"><td class="d"><b>SALARY FOR THIS MONTH</b></td>
     <td style="font-weight:800;color:#0F5132">${esc(payableText)}</td></tr>
</table></div>
<div class="fn">This is a computer-generated document from ${esc(info.clinicName)} &middot; No physical signature required.</div>
</div>
</body></html>"""
    }

    fun print(activity: Activity, staffCode: String, monthLabel: String, html: String) {
        val wv = WebView(activity)
        wv.settings.javaScriptEnabled = false
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Performance Sheet - $staffCode - $monthLabel"
                    pm.print(
                        jobName, view.createPrintDocumentAdapter(jobName),
                        com.tkbiswas.pilesclinic.native.PrintQuality.builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                    )
                } catch (_: Throwable) {
                    android.widget.Toast.makeText(
                        activity, "Could not open print — please try again",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        keepAlive = wv
        /* ⛔ baseURL = file:///android_asset/ — লোগোর ছবিটা এখান থেকেই আসে
           (RegistrationHtmlPrint-এর হুবহু একই পথ)। */
        wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
    }

    /** WhatsApp-এ পাঠানোর লেখা — টেবিল নয়, সাজানো লাইন (WhatsApp টেবিল বোঝে না)। */
    fun whatsAppText(
        staffName: String, staffCode: String, branchName: String, monthLabel: String,
        rows: List<Row>, totalHours: String, payableText: String,
        perf: Perf? = null                      // 📊 V1204 — কাগজ ও WhatsApp একই সংখ্যা
    ): String {
        val sb = StringBuilder()
        sb.append("*STAFF PERFORMANCE SHEET*\n")
        sb.append(staffName).append("  ·  ").append(staffCode).append("\n")
        sb.append(branchName).append("  ·  ").append(monthLabel).append("\n")
        sb.append("--------------------------------\n")
        if (perf != null) {
            sb.append("New Enquiry: ").append(perf.enquiries)
                .append("  |  Registration: ").append(perf.registrations).append("\n")
                .append("App Calls: ").append(perf.appCalls)
                .append("  |  Outside Calls: ").append(perf.outsideCalls)
                .append("  |  Total: ").append(perf.totalCalls).append("\n")
                .append("Leave Days: ").append(perf.leaveDays).append("\n")
                .append("--------------------------------\n")
        }
        for (r in rows) {
            sb.append(r.date.take(5)).append("  ")
                .append(if (r.outMissing) r.inTime + " → MISSING" else r.inTime + " → " + r.outTime)
                .append("  ").append(r.hours)
            if (r.tag.isNotBlank()) sb.append("  (").append(r.tag).append(")")
            sb.append("\n")
        }
        sb.append("--------------------------------\n")
        sb.append("*Total hours* : ").append(totalHours).append("\n")
        sb.append("*Salary this month* : ").append(payableText)
        return sb.toString()
    }
}
