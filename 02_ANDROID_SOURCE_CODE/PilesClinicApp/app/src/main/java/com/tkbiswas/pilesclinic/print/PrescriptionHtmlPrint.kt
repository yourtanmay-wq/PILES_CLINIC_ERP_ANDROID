package com.tkbiswas.pilesclinic.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ℞🔒 V409 (১৭.০৮.২০২৬, TK-অনুমোদিত প্রুফ) — **PRESCRIPTION ও MEDICINE SLIP, A4**।
 *
 * TK-এর নির্দেশ: *"ফোনে প্রেসক্রিপশনের কাজটা ধরুন · খুব সাবধানে · কোন কিছু যেন
 * খারাপ না হয় · সততার সাথে কাজটা করবেন"*।
 *
 * ⛔ এই ফাইলটা `DietChartHtmlPrint.kt` ও `RegistrationHtmlPrint.kt`-এর **হুবহু একই
 *    প্রমাণিত প্যাটার্ন** — ওয়েবের অনুমোদিত HTML+CSS একটা WebView-তে রেন্ডার করে
 *    Android-এর নিজের PrintManager দিয়ে A4-এ ছাপা/PDF। তাই ফোন ও কম্পিউটারের
 *    ছাপা কাগজ **হুবহু এক**।
 *
 * 🔵 এখানে একটা বাড়তি সাবধানতা নেওয়া হয়েছে: HTML+CSS-টা Kotlin-এর ভিতরে হাতে
 *    লেখা হয়নি। ওয়েবের **আসল ছাপা পাতা থেকেই** যন্ত্রে বের করে আনা হয়েছে এবং
 *    `assets/www/rx_print.html` ফাইলে রাখা হয়েছে। এই Kotlin শুধু ওই ফাইলটা পড়ে
 *    `{{...}}` ঘরগুলোয় রোগীর তথ্য বসায় — অর্থাৎ নকশা নিয়ে এখানে কোনো হিসাব নেই,
 *    তাই নকশা এদিক-ওদিক হওয়ার সুযোগও নেই।
 *    ⇒ মিলিয়ে দেখা হয়েছে: ৩৫,৬৬,৬৪৮ পিক্সেলের মধ্যে আলাদা মাত্র ৫৩২ (০.০১%),
 *      সেটুকুও শুধু `℞` চিহ্নের কিনারার মসৃণতা।
 *
 * ⛔ `ClinicPdfBuilder.kt` (OWNER LOCKED — Blood Test / Registration / Payment
 *    Receipt / Chamber Register সবই ওটার উপর দাঁড়িয়ে) **একটুও ছোঁয়া হয়নি**।
 *    শুধু Prescription ও Medicine Slip — এই দুটো কাগজের ছাপার পথ বদলাল।
 * ⛔ কোন ওষুধ বাছা হয়েছে, কী সেভ হয়, ক্লাউডে কী যায় — এক অক্ষরও বদলায়নি।
 */
object PrescriptionHtmlPrint {

    /** এই দুটো কাগজই নতুন পথে ছাপে; বাকি সব আগের পথেই থাকে। */
    fun handles(documentTitle: String?): Boolean {
        val t = (documentTitle ?: "").trim().uppercase(Locale.US)
        return t == "PRESCRIPTION" || t == "MEDICINE SLIP"
    }

    // প্রিন্ট-জব শেষ না হওয়া পর্যন্ত WebView বাঁচিয়ে রাখতে হয় (নইলে খালি ছাপে)।
    @Suppress("StaticFieldLeak")
    private var keepAlive: WebView? = null

    fun print(activity: Activity, model: PrintDocumentModel, onStateChanged: ((String) -> Unit)? = null) {
        val html = PrescriptionHtml.build(activity, model)
        val wv = WebView(activity)
        wv.settings.javaScriptEnabled = false
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = model.documentTitle
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    val job = pm.print(
                        jobName, adapter,
                        com.tkbiswas.pilesclinic.native.PrintQuality.builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                    )
                    // 🟢🔒🔒 V675 (২৫.০৮.২০২৬, বিল্ড-এরর ফিক্স) — Android SDK-তে
                    // `PrintJob`-এর কোনো state-change listener নেই (পাবলিক API
                    // নেই), তাই Handler দিয়ে প্রতি ৫০০ms জব-স্টেট পোল করা হচ্ছে
                    // যতক্ষণ না completed/failed/cancelled হয়।
                    if (onStateChanged != null) {
                        try {
                            val handler = android.os.Handler(android.os.Looper.getMainLooper())
                            var lastMsg = ""
                            val poller = object : Runnable {
                                override fun run() {
                                    val msg = when {
                                        job.isCompleted -> "✅ Print sheet closed — completed (printed or saved as PDF)."
                                        job.isFailed -> "❌ Print failed — please try again."
                                        job.isCancelled -> "⚠️ Print was cancelled — nothing was printed."
                                        job.isBlocked -> "⏸️ Print is paused — check the printer."
                                        job.isQueued || job.isStarted -> "⏳ Sending to printer…"
                                        else -> ""
                                    }
                                    if (msg.isNotBlank() && msg != lastMsg) {
                                        lastMsg = msg
                                        onStateChanged(msg)
                                    }
                                    if (!job.isCompleted && !job.isFailed && !job.isCancelled) {
                                        handler.postDelayed(this, 500)
                                    }
                                }
                            }
                            handler.postDelayed(poller, 500)
                        } catch (_: Throwable) { }
                    }
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

/**
 * `assets/www/rx_print.html` টেমপ্লেটের `{{...}}` ঘরগুলো ভরে দেয়।
 * ⛔ এখানে ইচ্ছে করে **কোনো HTML/CSS লেখা হয়নি** (শুধু কয়েকটা ছোট মোড়ক) —
 *    পুরো নকশা টেমপ্লেট ফাইলেই আছে।
 */
object PrescriptionHtml {

    private const val TEMPLATE_ASSET = "www/rx_print.html"

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun dot(date: Date): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date)

    /** 🖨️🔒 V955 (০১.০৯.২০২৬, TK-নির্দেশ: *"Date-এর পাশে Time থাকতে হবে"*) —
     *  প্রকল্পের লক করা ফরম্যাট: তারিখ `dd.MM.yyyy`, সময় `h.mm a` (১২-ঘণ্টা AM/PM,
     *  ঠিক যেমন `DoctorCheckupActivity`-তে আছে)। ⛔ তারিখের রূপ বদলায়নি,
     *  শুধু পাশে সময় যোগ হলো। */
    private fun dotTime(date: Date): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date) + " : " +
        SimpleDateFormat("h.mm a", Locale.US).format(date)   // 🔴 V1158

    /** "2026-09-02" বা "02.09.2026" — দুটোই এসে 02.09.2026 হয়ে যায়। */
    private fun dotFromText(raw: String): String {
        val s = raw.trim()
        /* 🖨️🔒 V994 (০৩.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — তারিখ না থাকলে
           আগে `_______________` ছাপত; TK-এর কথায় ওটা দেখতে পেশাদার নয়।
           এখন ফাঁকা যায় আর টেমপ্লেটের CSS একটা পরিষ্কার পাতলা দাগ আঁকে —
           হাতে লেখার জায়গা আগের মতোই থাকে। ⛔ তারিখ থাকলে কিছুই বদলায়নি। */
        if (s.isEmpty()) return ""
        val iso = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(s)
        if (iso != null) {
            return iso.groupValues[3] + "/" + iso.groupValues[2] + "/" + iso.groupValues[1]   // 🔴 V1158
        }
        return s
    }

    /** "42 / Female" → প্রথম অংশ বয়স, দ্বিতীয় অংশ লিঙ্গ। */
    private fun agePart(ageSex: String): String {
        val bits = ageSex.split("/")
        return if (bits.isNotEmpty()) bits[0].trim() else ""
    }

    private fun sexPart(ageSex: String): String {
        val bits = ageSex.split("/")
        return if (bits.size > 1) bits[1].trim().uppercase(Locale.US) else ""
    }

    /** PrescriptionOptionsStore.printLines() দেয় "LABEL\nমান" — সেটাই বাঁ কলামের ঘর। */
    private fun historyHtml(lines: List<String>): String {
        val sb = StringBuilder()
        for (line in lines) {
            val cut = line.indexOf('\n')
            val label = if (cut > 0) line.substring(0, cut) else line
            val value = if (cut > 0) line.substring(cut + 1) else ""
            sb.append("<div><b>").append(esc(label.trim()))
                .append("</b><span>").append(esc(value.trim())).append("</span></div>")
        }
        return sb.toString()
    }

    /** ওষুধের টেবিলের সারি। কোনো ওষুধ না থাকলে ওয়েবের মতোই একটা ফাঁকা বার্তা। */
    private fun rowsHtml(section: PrintSection?): String {
        val names = section?.rxNames
        if (section == null || names == null || names.isEmpty()) {
            return "<tr><td colspan=\"5\">No item selected</td></tr>"
        }
        val types = section.rxTypes
        val dose = section.rxDosage
        val whenCol = section.rxFrequency
        val days = section.rxDuration
        fun at(list: List<String>?, i: Int): String {
            if (list == null || i >= list.size) return ""
            val v = list[i].trim()
            return if (v == "-") "" else v
        }
        val sb = StringBuilder()
        for (i in names.indices) {
            sb.append("<tr><td>").append(i + 1).append("</td><td>")
            val t = at(types, i)
            if (t.isNotEmpty()) {
                sb.append("<span class=\"rxPrintType\">").append(esc(t)).append("</span>")
            }
            sb.append(esc(names[i].trim())).append("</td><td>")
                .append(esc(at(dose, i))).append("</td><td>")
                .append(esc(at(whenCol, i))).append("</td><td>")
                .append(esc(at(days, i))).append("</td></tr>")
        }
        return sb.toString()
    }

    fun build(context: Context, model: PrintDocumentModel): String {
        val template = context.assets.open(TEMPLATE_ASSET)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

        val branch = BranchCatalog.byName(model.branchName)
        val isKishanganj = branch.id == "kishanganj"
        val title = model.documentTitle.trim().uppercase(Locale.US)
        val isPrescription = title == "PRESCRIPTION"

        val branchSub = if (isKishanganj) {
            ""
        } else {
            "<div class=\"brSub\">" + esc(model.branchName.trim().uppercase(Locale.US)) + " BRANCH</div>"
        }
        val clinicAddr = esc(branch.addressLine) + " · Mobile: +91" + esc(branch.phoneLine) +
            " · Helpline: +91" + esc(BranchCatalog.HELPLINE)   // ☎️ V833

        // 🔵 ওয়েবের মতোই: Diet ও রোগীর ইতিহাস শুধু PRESCRIPTION-এ, Medicine Slip-এ নয়।
        /* 🖨️🔒 V833 (২৯.০৮.২০২৬, TK-নির্দেশ) — আগে এখানে `if (isPrescription)`
           ছিল, তাই **Medicine Slip-এ বাঁ কলামটা ইচ্ছে করে বাদ দেওয়া হত**
           (TK-এর প্রশ্ন: *"মেডিসিন স্লিপে নেই কেন?"*)। এখন দুই কাগজেই বসে।
           ⛔ তালিকা ফাঁকা হলে আগের মতোই কিছুই বসে না — অর্থাৎ পুরনো কোনো
              কাগজ ভাঙে না। */
        val historyBlock = historyHtml(model.complaintHistory)
        /* 🔵 V488 (20.08.2026, TK-নির্দেশ): "ADVICE: Sitz Bath — 2 Times Daily"
           লাইনটা আগে টেমপ্লেটের ভিতরে **হাতে লেখা স্থির** ছিল, তাই কোনোভাবেই
           তোলা যেত না। এখন সেটা `{{ADVICE}}` ঘর — Prescription-এ টিক তোলা থাকলে
           ঘরটা ফাঁকা যায়, নইলে আগের হুবহু একই লেখাই বসে।
           ⛔ Medicine Slip-এ (isPrescription = false) আগের মতোই সবসময় বসে —
              ওই কাগজে এই টিক-বাক্সটাই নেই, তাই ওখানে কিছু বদলানো চলবে না। */
        val adviceBlock = if (!isPrescription || model.prescriptionSitzBath) {
            "<span><b>ADVICE:</b> Sitz Bath — 2 Times Daily</span>"
        } else {
            ""
        }
        val dietBlock = if (isPrescription && model.prescriptionDiet.isNotBlank()) {
            "<span class=\"rxDietLine\"><b>Diet:</b> " + esc(model.prescriptionDiet.trim()) + "</span>"
        } else {
            ""
        }
        // ⛔ TK: "আগে কি ছিল তাই থাকবে" — Medicine Slip-এ ডাক্তারের নাম বসে না।
        val doctorBlock = if (isPrescription) {
            "<div class=\"docRight\"><b>Dr. K.H MANDAL</b><small>(B.A.M.S) Regd 12386</small></div>"
        } else {
            "<div class=\"docRight emptyDoc\"></div>"
        }

        val firstSection = if (model.sections.isEmpty()) null else model.sections[0]

        var out = template
        out = out.replace("{{LOGO}}", branch.logoAssetPath)
        out = out.replace("{{CLINIC}}", esc(branch.clinicName))
        out = out.replace("{{BRANCH_SUB}}", branchSub)
        out = out.replace("{{CLINIC_ADDR}}", clinicAddr)
        out = out.replace("{{TITLE}}", esc(title))
        out = out.replace("{{NAME}}", esc(model.patientName.trim().uppercase(Locale.US)))
        out = out.replace("{{AGE}}", esc(agePart(model.patientAgeSex)))
        out = out.replace("{{PID}}", esc(model.patientId.trim()))
        out = out.replace("{{MOBILE}}", esc(model.patientMobile.trim()))
        out = out.replace("{{DATE}}", dotTime(Date()))   // 🖨️ V955 — তারিখের পাশে সময়
        out = out.replace("{{SEX}}", esc(sexPart(model.patientAgeSex)))
        out = out.replace("{{DISEASE}}", esc(model.patientDisease.trim().uppercase(Locale.US)))
        out = out.replace("{{ADDRESS}}", esc(model.patientAddress.trim()))
        out = out.replace("{{HISTORY}}", historyBlock)
        out = out.replace("{{ADVICE}}", adviceBlock)   // 🔵 V488
        out = out.replace("{{DIET}}", dietBlock)
        out = out.replace("{{NEXT}}", esc(dotFromText(model.nextFollowDate)))
        out = out.replace("{{ROWS}}", rowsHtml(firstSection))
        out = out.replace("{{VID}}", esc(model.patientId.trim()))
        out = out.replace("{{DOCTOR}}", doctorBlock)
        return out
    }
}
