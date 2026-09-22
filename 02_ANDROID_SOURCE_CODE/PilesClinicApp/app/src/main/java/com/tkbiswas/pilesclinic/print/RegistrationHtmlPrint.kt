package com.tkbiswas.pilesclinic.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * 🔵🔒 TK-নির্দেশ (10.08.2026): ফোনের Registration প্রিন্টও এখন **হুবহু ওয়েবের অনুমোদিত
 * ডিজাইনে** ছাপবে। আগে native structured print (ClinicPdfBuilder) দিয়ে হতো, তাই চেহারা
 * আলাদা আসত — TK প্রুফ দেখে যে মডেল পছন্দ করেছিলেন তা মিলত না। এখন ঠিক ওয়েবের
 * (wlv1RegA4Html) একই HTML+CSS একটা WebView-তে রেন্ডার করে Android-এর নিজস্ব
 * PrintManager দিয়ে ছাপা/PDF-সেভ/শেয়ার করা হয় — WebView ইঞ্জিন বলে ছাপা হুবহু ওই ডিজাইন।
 *
 * ⛔ Registration-ছাড়া অন্য কোনো প্রিন্ট (Prescription/Payment/etc.) ছোঁয়া হয়নি — সেগুলো
 * আগের native পথেই। ⛔ লোগো/ব্রাঞ্চ-ঠিকানা bundled assets/www + BranchCatalog থেকে।
 */
object RegistrationHtmlPrint {

    // WebView-টা প্রিন্ট-জব শেষ না হওয়া পর্যন্ত বাঁচিয়ে রাখতে হয় (নইলে খালি ছাপে)।
    @Suppress("StaticFieldLeak")
    private var keepAlive: WebView? = null

    fun print(activity: Activity, patient: JSONObject) {
        val html = RegistrationHtml.build(patient)
        val wv = WebView(activity)
        wv.settings.javaScriptEnabled = false
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Registration Form"
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

/** ওয়েবের wlv1RegA4Html + wlv1DocA4CSS-এর হুবহু Kotlin-পোর্ট (একই CSS ও গঠন)। */
object RegistrationHtml {

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** ওয়েবের wlv1AddrTwo — থানা-চিহ্নের আগে লাইন-ব্রেক, নইলে এক লাইনে। */
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

    /** "+919999999999" / "919999999999" → "9999999999"; ১০ সংখ্যার কম হলে যা আছে তাই। */
    private fun mob(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.length >= 10) d.takeLast(10) else raw
    }

    fun build(p: JSONObject): String {
        fun s(k: String) = p.optString(k, "")
        val info = BranchCatalog.byName(s("branch"))
        val pid = esc(s("patientId").ifBlank { "-" })
        val ageSex = esc(listOf(s("sex"), s("age")).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "-" })
        /* 🔴🔒 V506 (TK-নির্দেশ ২১.০৮.২০২৬, ফটো-প্রুফ দেখে অনুমোদিত):
           কাগজেও বার্তার হুবহু একই ধরন — "DATE: 21/08/2026 TIME: 10.48Am"।
           আগে ছিল "21/08/2026 · 10.48Am" (বিন্দু দিয়ে জোড়া)।
           ⛔ সময় জানা না থাকলে শুধু "DATE: …" যায় — আন্দাজে কিছু বসে না।
           ⛔ কাগজের বাকি সাজ · ঘর · লেখা কিছুই বদলায়নি। */
        val dateStr = run {
            val d = s("registrationDate").ifBlank { s("date") }
            val t = s("time")
            if (d.isBlank()) "" else if (t.isNotBlank()) "DATE: $d TIME: $t" else "DATE: $d"
        }
        val addressHtml = if (s("address").isNotBlank()) addr2(s("address")) else "-"

        fun rc(k: String, v: String, full: Boolean): String {
            val vv = if (v.isBlank()) "-" else v
            return "<div class=\"cell${if (full) " full" else ""}\"><span class=\"k\">${esc(k)}</span><span class=\"v\">${esc(vv)}</span></div>"
        }
        fun rb(k: String, full: Boolean): String =
            "<div class=\"cell${if (full) " full" else ""}\"><span class=\"k\">${esc(k)}</span><span class=\"vb\"></span></div>"

        // Referred By (থাকলে)
        var refRows = ""
        if (s("refBy").isNotBlank() || s("refDoctor").isNotBlank() || s("refDoctorMobile").isNotBlank()) {
            refRows = "<div class=\"sec\"><div class=\"sh\"><span>REFERRED BY</span></div><div class=\"g\">" +
                rc("Referred By", s("refBy"), false) +
                (if (s("refDoctor").isNotBlank()) rc("Doctor Name", s("refDoctor"), false) else "") +
                (if (s("refDoctorMobile").isNotBlank()) rc("Doctor Mobile", s("refDoctorMobile"), false) else "") +
                "</div></div>"
        }
        // Medical (থাকলে)
        var medRows = ""
        if (s("complaint").isNotBlank() || s("sinceWhen").isNotBlank() || s("previousTreatment").isNotBlank()) {
            medRows = "<div class=\"sec\"><div class=\"sh\"><span>MEDICAL DETAILS (at Registration)</span></div><div class=\"g\">" +
                (if (s("complaint").isNotBlank()) rc("Complaint", s("complaint"), true) else "") +
                (if (s("sinceWhen").isNotBlank()) rc("Since When", s("sinceWhen"), false) else "") +
                (if (s("previousTreatment").isNotBlank()) rc("Previous Treatment", s("previousTreatment"), false) else "") +
                "</div></div>"
        }
        // Fee (থাকলে)
        var feeRows = ""
        val regFeeClean = s("regFee").removeSuffix(".0")
        val regMode = s("regMode").ifBlank { s("payMode") }
        if ((regFeeClean.isNotBlank() && regFeeClean != "0") || regMode.isNotBlank()) {
            feeRows = "<div class=\"sec\"><div class=\"sh\"><span>REGISTRATION FEE</span></div><div class=\"g\">" +
                (if (regFeeClean.isNotBlank() && regFeeClean != "0") rc("Registration Fee", "₹$regFeeClean", false) else "") +
                (if (regMode.isNotBlank()) rc("Payment Mode", regMode, false) else "") +
                "</div></div>"
        }
        // Doctor's Check-up (ফাঁকা)
        val docRows = "<div class=\"sec docsec\"><div class=\"sh\"><span>DOCTOR'S CHECK-UP</span><span class=\"hint\">TO BE FILLED BY DOCTOR</span></div><div class=\"g\">" +
            rb("Examination / DRE", true) + rb("Grade", false) + rb("On Probing", false) +
            rb("Other Findings", true) + rb("Investigation Advised", true) + rb("Treatment Plan", true) +
            rb("Estimated Cost", false) + rb("Recovery Time", false) + rb("Doctor's Remarks / Advice", true) +
            "</div></div>"

        val css = """*{margin:0;padding:0;box-sizing:border-box;font-family:Georgia,'Noto Serif',serif;-webkit-print-color-adjust:exact;print-color-adjust:exact}
@page{size:A4;margin:0}body{background:#fff;color:#111}
.gold{height:6px;background:linear-gradient(90deg,#b8912f,#e6c65c,#b8912f)}.gbar{height:3px;background:#0f5132}
.lh{display:flex;align-items:center;gap:14px;padding:14px 22px 10px}.lh img{width:74px;height:74px;border-radius:50%}
.cn{font-size:23px;font-weight:800;color:#0f5132;line-height:1}.tag{font-size:11px;font-weight:700;color:#b8912f;letter-spacing:2px;margin-top:3px;text-transform:uppercase;font-family:Arial}
.addr{font-size:11.5px;color:#3b4650;margin-top:4px;font-family:Arial}.addr b{color:#0f5132}
.tb{background:#0f5132;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:8px 22px;font-family:Arial}.tb .t{font-size:14px;font-weight:800;letter-spacing:2px}.tb .r{font-size:10.5px;color:#cfe6d8;text-align:right;line-height:1.5}
.pi{display:flex;align-items:flex-start;gap:16px;padding:12px 22px;font-size:12px;font-family:Arial;background:#f7faf8;border-bottom:1.5px solid #e4ebe6}
.pphoto{width:84px;height:100px;border:2px solid #b8912f;border-radius:5px;background-size:cover;background-position:center;background-color:#eaf0f6;flex:0 0 auto;display:flex;align-items:center;justify-content:center;color:#9fb0a5;font-size:30px}
.pi .c{flex:1}.pi .r{padding:3px 0}.pi .r b{color:#0f5132;display:inline-block;min-width:82px;vertical-align:top}
.wrap{padding:8px 22px 14px;font-family:Arial}.sec{margin-top:12px;border:1px solid #d5ddd7;border-radius:5px;overflow:hidden}
.sh{background:#eef5f0;color:#0f5132;font-size:12px;font-weight:800;letter-spacing:1px;padding:8px 12px;border-left:4px solid #b8912f;display:flex;justify-content:space-between;align-items:center}
.sh .hint{color:#9a7b28;font-weight:700;font-size:10px;letter-spacing:.5px}
.g{display:flex;flex-wrap:wrap}.cell{width:50%;padding:9px 12px;font-size:13px;border-bottom:1px solid #f0f3f1;display:flex;gap:8px}.cell.full{width:100%}.cell .k{color:#6b7680;min-width:120px}.cell .v{color:#111;font-weight:700}.cell:nth-child(odd){border-right:1px solid #f0f3f1}
.docsec .cell{align-items:flex-end}.vb{flex:1;min-height:26px}
.note{margin-top:12px;font-size:11.5px;color:#5b6b81;font-family:Arial;background:#fbfdfc;border:1px dashed #cdd7d1;border-radius:5px;padding:9px 12px}
.foot{display:flex;justify-content:space-between;align-items:flex-end;padding:26px 22px 10px;font-family:Arial}.stamp{width:100px;height:100px;border:1.4px dashed #c3ccd6;border-radius:50%;display:flex;align-items:center;justify-content:center;color:#aeb8c2;font-size:10px;text-align:center}
.sign{text-align:center;font-size:12px}.sign .ln{width:200px;border-top:1.4px solid #333;margin-bottom:5px}.sign .dn{font-weight:800;color:#0f5132;font-size:14px;letter-spacing:.5px}.sign .rl{font-size:11px;color:#5b6b81}
.fn{border-top:1px solid #e4ebe6;text-align:center;font-size:10px;color:#8a949e;padding:9px 0 12px;font-family:Arial}"""

        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>$css</style></head><body>" +
            "<div class=\"gold\"></div><div class=\"lh\"><img src=\"${info.logoAssetPath}\"><div><div class=\"cn\">${esc(info.clinicName)}</div><div class=\"tag\">Ayurveda &amp; Anorectal Diseases</div>" +
            "<div class=\"addr\"><b>${esc(s("branch"))}:</b> ${esc(info.addressLine)} &nbsp;|&nbsp; <b>&#9742;</b> ${esc(info.phoneLine)} &nbsp;|&nbsp; <b>&#9742;</b> ${esc(BranchCatalog.HELPLINE)}</div></div></div><div class=\"gbar\"></div>" +
            "<div class=\"tb\"><span class=\"t\">PATIENT REGISTRATION FORM</span><span class=\"r\">Reg. No: $pid<br>${esc(dateStr)}</span></div>" +
            "<div class=\"pi\"><div class=\"pphoto\">🧑</div>" +
            "<div class=\"c\"><div class=\"r\"><b>Name</b> : ${esc(s("name").ifBlank { "-" }).uppercase()}</div><div class=\"r\"><b>Patient ID</b> : $pid</div><div class=\"r\"><b>Age / Sex</b> : $ageSex</div><div class=\"r\"><b>Occupation</b> : ${esc(s("occupation").ifBlank { "-" })}</div><div class=\"r\"><b>Branch</b> : ${esc(s("branch").ifBlank { "-" })}</div></div>" +
            "<div class=\"c\"><div class=\"r\"><b>Mobile</b> : ${esc(mob(s("mobile")).ifBlank { "-" })}</div><div class=\"r\"><b>Alt Mobile</b> : ${esc(mob(s("altMobile")).ifBlank { "-" })}</div><div class=\"r\"><b>Disease</b> : ${esc(s("disease").ifBlank { "-" })}</div><div class=\"r\"><b>Address</b> : <span style=\"display:inline-block;vertical-align:top\">$addressHtml</span></div></div></div>" +
            "<div class=\"wrap\">$refRows$medRows$feeRows$docRows" +
            "<div class=\"note\">📌 Please keep your Patient ID ($pid) safe for future visits and follow-ups.</div></div>" +
            "<div class=\"foot\"><div class=\"stamp\">Clinic Seal</div><div class=\"sign\"><div class=\"ln\"></div><div class=\"dn\">Doctor's Signature</div><div class=\"rl\">${esc(info.clinicName)}</div></div></div>" +
            "<div class=\"fn\">This is a computer-generated document from ${esc(info.clinicName)} &middot; No physical signature required.</div>" +
            "</body></html>"
    }
}
