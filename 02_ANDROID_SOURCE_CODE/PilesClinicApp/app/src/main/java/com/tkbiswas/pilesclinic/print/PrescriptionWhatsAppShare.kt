package com.tkbiswas.pilesclinic.print

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * 📤🔒 V491 (২০.০৮.২০২৬, TK-নির্দেশ) — **প্রেসক্রিপশন WhatsApp-এ PDF হিসেবে পাঠানো।**
 *
 * TK-এর কথা: *"WhatsApp অপশন যেন থাকে, আর সেখানে চাপ দিলে WhatsApp / Business
 * জোর করে ওপেন হতে হবে, হোয়াটসঅ্যাপে যেন পিডিএফ শেয়ারও করতে পারি।"*
 *
 * ─── কেন আলাদা একটা ফাইল লাগল ───────────────────────────────────────────────
 * V409 থেকে Prescription ও Medicine Slip ছাপে **WebView → PrintManager** পথে,
 * যাতে ফোন আর কম্পিউটারের কাগজ হুবহু এক থাকে। ওই পথে Android নিজেই ছাপার পাতা
 * বানায় — অ্যাপের হাতে **কোনো PDF ফাইল আসে না**। তাই PrintPreviewActivity-তে
 * Prescription-এর "Share PDF" বোতামটা ইচ্ছে করেই লুকানো। WhatsApp-এ পাঠাতে
 * গেলে একটা সত্যিকারের ফাইল চাই।
 *
 * ─── 🔴 প্রথম চেষ্টায় যে ভুল হয়েছিল (২০.০৮.২০২৬ রাত ১০:৩১, TK-এর বিল্ড) ────
 * প্রথমে WebView-এর নিজের `PrintDocumentAdapter`-কে সরাসরি ফাইলে লিখতে বলা
 * হয়েছিল (`adapter.onLayout` → `adapter.onWrite`)। কিন্তু ওই দুটো ডাকের জন্য
 * `PrintDocumentAdapter.LayoutResultCallback` / `WriteResultCallback`-এর
 * object বানাতে হয়, আর ওদের constructor **package-private** — `android.print`
 * প্যাকেজের বাইরে থেকে বানানোই যায় না। তাই বিল্ড ভাঙল:
 *     Cannot access '<init>': it is package-private in 'LayoutResultCallback'
 *     Cannot access '<init>': it is package-private in 'WriteResultCallback'
 * ⇒ ওই পথ Android-এ **সোজাসুজি সম্ভবই নয়**। ঘুরপথে (নিজের ক্লাস `android.print`
 *   প্যাকেজে রেখে) করা যায়, কিন্তু সেটা কিছু ফোনে চলার সময় ভেঙে যেতে পারে —
 *   ক্লিনিকের অ্যাপে সেই ঝুঁকি নেওয়া হয়নি।
 *
 * ─── এখন যেভাবে করা হচ্ছে (শুধু স্বীকৃত, নিরাপদ API) ───────────────────────
 * **একই HTML** (`PrescriptionHtml.build` — অর্থাৎ হুবহু একই নকশা, একই উৎস)
 * একটা WebView-তে **A4-এর মাপে** (৭৯৪ × ১১২৩ px — ব্রাউজার A4 ছাপার সময় ঠিক
 * এই মাপই ধরে) বসিয়ে, সেটাকে Android-এর নিজের `PdfDocument`-এর পাতায় এঁকে
 * দেওয়া হয়। পাতা তৈরি হয় A4-এর আসল মাপে (৫৯৫ × ৮৪২ পয়েন্ট), আর আঁকার সময়
 * ঠিক ওই অনুপাতে ছোট করা হয় — ব্রাউজারও ছাপার সময় এই একই অনুপাত ব্যবহার করে।
 * লেখা ভেক্টর হিসেবেই বসে, তাই ঝাপসা হয় না। বড় প্রেসক্রিপশনে একাধিক পাতা
 * নিজে থেকেই হয়।
 *
 * ⛔ ছাপার পুরনো পথ (`PrescriptionHtmlPrint.print`) এক অক্ষরও বদলায়নি।
 * ⛔ কোন ওষুধ বাছা হল · কী সেভ হয় · ক্লাউডে কী যায় — কিছুই ছোঁয়া হয়নি।
 * 🔒 WhatsApp বাছাইয়ের নিয়ম V449-এর হুবহু একই — কখনো নিঃশব্দে একটা খোলে না,
 *    সবসময় দুটো অপশনই দেখানো হয় (TK-এর লক করা নিয়ম)।
 *
 * ⚠️ TK প্রথমবার মিলিয়ে দেখবেন: WhatsApp-এ যাওয়া PDF আর ছাপা কাগজ এক কিনা।
 */
object PrescriptionWhatsAppShare {

    private const val PKG_PERSONAL = "com.whatsapp"
    private const val PKG_BUSINESS = "com.whatsapp.w4b"

    /** A4 — ব্রাউজার ছাপার সময় যে CSS-পিক্সেল মাপ ধরে (৯৬ dpi)। */
    private const val A4_WIDTH_PX = 794
    private const val A4_HEIGHT_PX = 1123

    /** A4 — PDF-এর আসল মাপ (পয়েন্ট, ৭২ dpi)। */
    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842

    /** কাজ শেষ না হওয়া পর্যন্ত WebView বাঁচিয়ে রাখতে হয় (নইলে খালি পাতা)। */
    @Suppress("StaticFieldLeak")
    private var keepAlive: WebView? = null

    /**
     * 🟢🔒🔒 V639 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "Preparing PDF... এটা কি
     * হচ্ছে? Share তো হচ্ছেই না") — **আসল কারণ:** এই WebView-টা তৈরি করেই
     * সরাসরি measure()/layout() (হাতে-করা) ডাকা হত, কিন্তু কখনো পর্দার
     * আসল View-কাঠামোয় (window) যুক্তই করা হত না। কিছু ফোনের (বিশেষত
     * Xiaomi/MIUI-এর মতো কাস্টম WebView, এই ফাইলেরই উপরের মন্তব্যে আগে থেকে
     * স্বীকৃত সীমাবদ্ধতা) নিজস্ব WebView রেন্ডারার এমন "window-এ কখনো যুক্ত
     * হয়নি" WebView-এর onPageFinished/জাভাস্ক্রিপ্ট/আঁকা নির্ভরযোগ্যভাবে
     * চালায় না — তাই "Preparing PDF…" টোস্ট দেখানোর পরে কিছুই এগোত না, কোনো
     * error-ও আসত না (পুরো কাজটাই নিঃশব্দে থেমে যেত)।
     * **সমাধান:** WebView-টা এখন সত্যিই পর্দার (activity-র android.R.id.content)
     * সাথে যুক্ত থাকে — আকারে ১×১ পিক্সেল ও অদৃশ্য (INVISIBLE), তাই চোখে
     * কখনো দেখা যায় না, কিন্তু ফোনের আসল রেন্ডারিং-পথ দিয়েই চলে। কাজ শেষ
     * হলে (সফল/ব্যর্থ দুটোতেই) সাথে সাথে সরিয়ে ফেলা হয়।
     */
    private fun detachKeepAlive() {
        try {
            val wv = keepAlive
            (wv?.parent as? android.view.ViewGroup)?.removeView(wv)
        } catch (_: Throwable) { }
        keepAlive = null
    }

    // 🔴 V501: আগে এই তিনটে তথ্য `PrintDocumentModel`-এর ভিতর থেকে নেওয়া হতো।
    //    এখন যেকোনো HTML চলে বলে আলাদা করে রাখা হয় (একবারে একটাই শেয়ার চলে,
    //    তাই এতে গোলমালের সুযোগ নেই — WebView-ও একটাই `keepAlive`)।
    private var docTitle: String = "Document"
    private var docPatient: String = ""
    private var wantPrint: Boolean = false

    fun share(activity: Activity, model: PrintDocumentModel) {
        val html = try {
            PrescriptionHtml.build(activity, model)
        } catch (e: Throwable) {
            fail(activity, "Could not prepare the paper: ${e.message}"); return
        }
        shareHtml(activity, html, model.documentTitle, model.patientName)
    }

    /**
     * 🔴🔒 V501 (TK-নির্দেশ ২১.০৮.২০২৬, ফটো-প্রুফ অনুমোদিত) —
     * *"Back Save মাঝখানে Share রাখতে হবে… WhatsApp / business WhatsApp ওপেন হবে…
     *  A4 size pdf চাইলে প্রিন্টও করা যাবে।"*
     *
     * আগের `share()` শুধু **Prescription**-এর HTML বানাতে জানত। এখন যেকোনো
     * তৈরি HTML (যেমন `CheckupA4Report.html(...)`) একই প্রমাণিত পথে A4 PDF
     * হয়ে WhatsApp/Business/Print-এ যেতে পারে।
     *
     * ⛔ Prescription-এর পুরনো ডাক (`share(activity, model)`) হুবহু আগের মতোই
     *    চলে — সেটা এখন শুধু এই ফাংশনটাকেই ডাকে। কোনো আচরণ বদলায়নি।
     *
     * @param allowPrint `true` হলে বাছাইয়ের তালিকায় তৃতীয় একটা "Print / PDF"
     *        থাকে (প্রকল্পের নিজের প্রমাণিত `PdfPrintDocumentAdapter` দিয়ে,
     *        ঠিক `PrintPreviewActivity`-র মতোই)।
     */
    fun shareHtml(
        activity: Activity,
        html: String,
        documentTitle: String,
        patientName: String,
        allowPrint: Boolean = false
    ) {
        Toast.makeText(activity, "Preparing PDF…", Toast.LENGTH_SHORT).show()
        renderWidthPx = A4_WIDTH_PX   // 🔴🔒 V701 — আগের কাগজের মাপ যেন থেকে না যায়
        docTitle = documentTitle
        docPatient = patientName
        wantPrint = allowPrint

        /* ═══════════════════════════════════════════════════════════════════
           🔴🔴🔒 V795 (২৮.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — *"প্রেসক্রিপশন যখন
           হোয়াটসঅ্যাপে শেয়ার করতে যাই, ঘোড়ার ডিম এসেছে"*: PDF-এ শুধু বিশাল
           লোগো, ৪ পাতা)।

           ─── আসল কারণ (কোড ধরে প্রমাণিত, আন্দাজ নয়) ─────────────────────
           V698-এ ঠিক এই দোষটাই সারানো হয়েছিল — কিন্তু **শর্ত দিয়ে**: পাতার
           `<meta viewport>`-এ যদি একটা **সংখ্যা** থাকে (`width=794`) তবেই
           `useWideViewPort` চালু হয়। Check-up-এর কাগজে সংখ্যা আছে, তাই
           ওটা সেরে গিয়েছিল।
           কিন্তু **Prescription ও Medicine Slip**-এর টেমপ্লেট
           (`assets/www/rx_print.html` লাইন ৩) লেখা `width=device-width` —
           কোনো সংখ্যা নেই। তাই শর্তটা মেলে না, wide-viewport চালুই হয় না,
           আর ফোনের ঘনত্ব ধরে CSS চওড়া দাঁড়ায় ৭৯৪ ÷ ৩ ≈ ২৬৫px ⇒ ৭৮px-এর
           লোগোই পাতার তিন ভাগের এক ভাগ জুড়ে বসে, উচ্চতার মাপ ভুল হয়,
           বাড়তি পাতা তৈরি হয় — TK-এর ছবিতে ঠিক এটাই।
           ⚠️ V698-এর মন্তব্যে আমি নিজেই লিখেছিলাম *"Prescription-এ এক অক্ষরও
              বদলায়নি"* — সেটাই ছিল ফাঁক। তখন ওটাকে নিরাপত্তা ভেবেছিলাম।

           ─── সমাধান ─────────────────────────────────────────────────────
           **শুধু এই PDF বানানোর জন্য** পাতার viewport-টা A4-এর সংখ্যায়
           বদলে নেওয়া হয়। তাতে V698/V701-এর ইতিমধ্যে-প্রমাণিত পথটাই চালু
           হয়, আর কাগজ হুবহু A4 মাপে সাজে।
           ⛔ আসল টেমপ্লেট ফাইল **ছোঁয়া হয়নি** — বদলটা শুধু মেমরির এই কপিতে।
           ⛔ ছাপার পথ (PrintManager · `PdfPrintDocumentAdapter`) সম্পূর্ণ
              আলাদা, সেখানে হাত পড়েনি — তাই প্রিন্ট আগের মতোই।
           ═══════════════════════════════════════════════════════════════ */
        /* 🔴🔒 V1242 (০৮.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — *"কমন ব্লাড টেস্ট
           হোয়াটসঅ্যাপে শেয়ার করলাম, কেন এরকম ব্রেক হয়ে গেল"*, খাতার সারি ৩৬৩)।
           🔴 **আসল কারণ (কোডে মেপে — এবং দোষটা আমারই, V795-এ আধখানা কাজ):**
              উপরের V795-এর সমাধান দুটো অবস্থা সামলাত — viewport-এ সংখ্যা আছে,
              বা viewport-এর ট্যাগটা আছে (তখন বদলে দেওয়া হত)। কিন্তু কোনো কাগজে
              **viewport-এর ট্যাগটাই না থাকলে** নিচের `replace` কিছুই খুঁজে পেত না,
              তাই পাতাটা **অবিকল আগের মতোই** চলে যেত।
              Blood Test Advice-এর কাগজে (`InvestigationHtmlPrint`) ওই ট্যাগটা
              কোনোদিনই ছিল না ⇒ WebView ফোনের চওড়া (~৩৬০px) ধরে সাজাত, অথচ
              কাগজটা ২১০mm ≈ ৭৯৪px চওড়া ⇒ ডান দিকটা কেটে যেত, লোগো বিশাল
              দেখাত। TK-এর ছবিতে ঠিক তাই।
           ⇒ এখন **তিন নম্বর অবস্থাটাও** সামলানো হয়: ট্যাগ না থাকলে `<head>`-এর
             ঠিক পরেই A4-এর viewport বসিয়ে দেওয়া হয়।
           ⛔ আসল টেমপ্লেট ফাইল ছোঁয়া হয়নি — বদলটা শুধু মেমরির এই কপিতে।
           ⛔ যে কাগজগুলোতে viewport আগে থেকেই ছিল, সেগুলো **এক অক্ষরও বদলায়নি**।
           ⛔ ছাপার পথ আলাদা, সেখানে হাত পড়েনি। */
        val htmlForPdf = try {
            val hasNumber = Regex("name=[\"']viewport[\"'][^>]*content=[\"'][^\"']*width\\s*=\\s*\\d{3,4}",
                RegexOption.IGNORE_CASE).containsMatchIn(html)
            val hasTag = Regex("<meta[^>]*name=[\"']viewport[\"'][^>]*>", RegexOption.IGNORE_CASE)
                .containsMatchIn(html)
            when {
                hasNumber -> html
                hasTag -> Regex("<meta[^>]*name=[\"']viewport[\"'][^>]*>", RegexOption.IGNORE_CASE)
                    .replace(html, "<meta name=\"viewport\" content=\"width=$A4_WIDTH_PX\">")
                else -> {
                    // ⛔ `<head ...>`-এর নিজের লেখাটা অক্ষত রেখে ঠিক তার পরেই বসানো হয়।
                    val m = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(html)
                    if (m == null) html
                    else html.substring(0, m.range.last + 1) +
                        "<meta name=\"viewport\" content=\"width=$A4_WIDTH_PX\">" +
                        html.substring(m.range.last + 1)
                }
            }
        } catch (_: Throwable) { html }

        /* 🖨️🔴🔒 V1249 (০৯.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — *"প্রেসক্রিপশন হোয়াটসঅ্যাপে
           শেয়ার করলে এরকম কেন আসে … যেগুলো প্রিন্টার থেকে ছাপি, হোয়াটসঅ্যাপে
           পাঠালেও যেন হুবহু একই A4 সাইজের PDF যায়"*, খাতার সারি ৩৭১)।

           🔴 **আসল কারণ (ব্রাউজারে মেপে দেখা, আন্দাজ নয়):** Prescription ও
              Medicine Slip-এর কাগজ (`assets/www/rx_print.html`)-এর **পুরো A4
              সাজটাই `@media print { … }`-এর ভিতরে** লেখা — লোগোর মাপ, হেডারের
              গ্রিড, ২১০mm চওড়া, ২৯৭mm উঁচু পাতা, সবকিছু।
              • ছাপার পথ (PrintManager) কাগজটা **print মিডিয়ায়** আঁকে ⇒ সব নিয়ম
                লাগে ⇒ কাগজ ঠিক।
              • কিন্তু এই PDF বানানোর পথটা একটা সাধারণ WebView — সেটা **screen
                মিডিয়ায়** আঁকে ⇒ ওই নিয়মগুলো একটাও লাগে না।
              হুবহু একই কাগজে মেপে পাওয়া গেছে (৭৯৪×১১২৩-এ):
                screen → লোগো ৬৪০×৬৪০ px · পাতা ২২১৪ px উঁচু · ১০৫০ px চওড়া
                print  → লোগো  ৮০×৮০  px · পাতা ১১২৩ px উঁচু ·  ৭৯৪ px চওড়া
              ⇒ TK-এর ছবিতে ঠিক এটাই: বিশাল লোগো · ২ পাতা · ডান দিক কাটা।
              ⚠️ V795/V1242-এ আমি viewport-এর চওড়াটা সারিয়েছিলাম, কিন্তু
                 মিডিয়ার এই তফাতটা তখন ধরতে পারিনি — সেটা আমারই আধখানা কাজ।

           ⇒ **সমাধান:** এই PDF বানানোর সময় কাগজটাকে **ছাপার নিয়মেই** আঁকতে বলা
             হয় — `@media print` ⇒ সবসময় চালু, `@media screen` ⇒ বন্ধ।
             মেপে দেখা: এর পরে screen-এ আঁকা কাগজ **print-এর সঙ্গে হুবহু এক**
             (৭৯৩.৬৯ × ১১২২.৫২ px — এক পিক্সেলও তফাত নেই), ঠিক ১ পাতা A4।
           ⛔ **ডিজাইনের একটা অক্ষরও বদলায়নি** — TK-এর স্পষ্ট নির্দেশ; বরং
              ছাপা কাগজের **হুবহু সেই ডিজাইনটাই** এখন হোয়াটসঅ্যাপে যায়।
           ⛔ আসল টেমপ্লেট ফাইল ছোঁয়া হয়নি — বদলটা শুধু মেমরির এই কপিতে।
           ⛔ ছাপার পথ (PrintManager) সম্পূর্ণ আলাদা, সেখানে হাত পড়েনি।
           ⛔ বাকি কাগজগুলোতে (Blood Test Advice · Diet Chart · Check-up ·
              Estimate · Registration) `@media print` নিয়মই নেই — মেপে দেখা —
              তাই ওদের জন্য এই লাইনগুলো কিছুই করে না, ওরা অক্ষত থাকে। */
        val htmlPrintMedia = try {
            Regex("@media\\s+print\\s*\\{", RegexOption.IGNORE_CASE)
                .replace(htmlForPdf, "@media all{")
                .let { Regex("@media\\s+screen\\s*\\{", RegexOption.IGNORE_CASE).replace(it, "@media not all{") }
        } catch (_: Throwable) { htmlForPdf }

        val wv = WebView(activity)
        // ⚠️ JavaScript শুধু পাতার **উচ্চতা মাপার** জন্য। টেমপ্লেটে নিজের কোনো
        //    স্ক্রিপ্ট নেই ও বাইরের কিছু লোড হয় না, তাই এতে ঝুঁকি নেই।
        wv.settings.javaScriptEnabled = true
        /* 🔴🔒🔒 V698 (২৬.০৮.২০২৬, TK-রিপোর্ট, ৩টে ছবিসহ — "কাজ হচ্ছে না";
           Check-up History → Send on WhatsApp-এর PDF-এ লেখা সরু কলামে, লোগো
           বিশাল হয়ে কাটা, পাতাগুলো প্রায় ফাঁকা)।

           **আসল কারণ (কোড ধরে, আন্দাজ নয়):** এই WebView-টা হাতে করে ৭৯৪
           **ডিভাইস-পিক্সেল** চওড়ায় বসানো হয় (`layoutAt`)। কিন্তু
           `useWideViewPort` চালু না থাকায় WebView পাতার `<meta name=
           "viewport" content="width=794">` লাইনটা **পড়েই না** — তখন CSS-এর
           চওড়া হয় ৭৯৪ ÷ ফোনের ঘনত্ব, অর্থাৎ ৩x ফোনে মাত্র ~২৬৫ CSS px।
           ফলে A4-এর জন্য লেখা কাগজটা ২৬৫px-এর সরু কলামে সাজে, আর ৭৮px-এর
           লোগো ওই চওড়ার প্রায় এক-তৃতীয়াংশ জুড়ে বসে — TK-এর ছবিতে ঠিক
           এটাই। উচ্চতার মাপও (`scrollHeight`) তখন ভুল হয়, তাই বাড়তি
           ফাঁকা পাতা তৈরি হয়।

           **সমাধান:** পাতা নিজে যদি একটা নির্দিষ্ট চওড়া চায় (viewport-এ
           `width=<সংখ্যা>`), তবেই wide-viewport চালু — তখন CSS চওড়া হয়
           ঠিক ৭৯৪, আর ১ CSS px = ১ ডিভাইস px, অর্থাৎ হুবহু A4।

           ⛔ **Prescription ও Medicine Slip-এ এক অক্ষরও বদলায়নি** — ওদের
              টেমপ্লেটে (`www/rx_print.html`) লেখা আছে `width=device-width`,
              কোনো নির্দিষ্ট সংখ্যা নয়, তাই নিচের শর্তটা ওদের ক্ষেত্রে মেলে
              না ও আগের আচরণই বহাল থাকে। */
        try {
            val wantWidth = Regex(
                "name=[\"']viewport[\"'][^>]*content=[\"'][^\"']*width\\s*=\\s*(\\d{3,4})",
                RegexOption.IGNORE_CASE
            ).find(htmlPrintMedia)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (wantWidth != null && wantWidth >= 300) {
                wv.settings.useWideViewPort = true
                /* 🔴🔒🔒 V701 (২৬.০৮.২০২৬, TK-এর ৪টে ছবিতে ধরা — PDF-এ লোগো
                   বিশাল, "TK BISW…" ও "DOCTOR CHECK-UP RE…" ডানদিকে **কেটে
                   গেছে**)। **আসল কারণ — V698-এ আমারই ভুল লাইন:** এখানে
                   `loadWithOverviewMode = false` লেখা ছিল।

                   `useWideViewPort` চালু করায় পাতা ঠিকই ৭৯৪ **CSS**-পিক্সেলে
                   সাজে — কিন্তু overview বন্ধ থাকায় WebView সেটাকে পর্দার
                   মাপে **ছোট করে না**, ফোনের ঘনত্ব ধরে আঁকে। ৩x ফোনে
                   ৭৯৪ CSS px = **২৩৮২ ডিভাইস px** চওড়া, অথচ আমরা আঁকি মাত্র
                   ৭৯৪ px-এ ⇒ **ডান দিকের দুই-তৃতীয়াংশ কেটে যায়**।
                   V698-এর আগে ছিল উল্টো দোষ (সব ছোট, সরু কলাম); আমি এক
                   দোষ সারিয়ে আরেকটা বানিয়েছি।

                   ⇒ `true` করলে WebView ৭৯৪ CSS px-কে ঠিক ৭৯৪ ডিভাইস px-এ
                     বসায় (১ CSS px = ১ px) — এটাই আসল A4। */
                wv.settings.loadWithOverviewMode = true
            }
        } catch (_: Throwable) { /* ব্যর্থ হলে আগের আচরণই — কিছু ভাঙে না */ }
        wv.setBackgroundColor(Color.WHITE)
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                measureThenBuild(activity, view)
            }
        }
        keepAlive = wv
        // 🟢🔒 V639 — WebView-টা সত্যিই পর্দার সাথে যুক্ত থাকে (১×১ পিক্সেল,
        // অদৃশ্য) — নইলে কিছু ফোনে (MIUI-এর মতো) onPageFinished/আঁকা
        // নির্ভরযোগ্যভাবে চলে না। ব্যর্থ হলেও (যেমন content root না পাওয়া
        // গেলে) আগের মতোই detached ভাবে চলার চেষ্টা করে — কিছু ভাঙে না।
        try {
            val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            wv.visibility = View.INVISIBLE
            root.addView(wv, android.view.ViewGroup.LayoutParams(1, 1))
        } catch (_: Throwable) { }
        layoutAt(wv, A4_HEIGHT_PX)
        // baseURL = file:///android_asset/  → লোগোর ছবি রিজলভ হয় (ছাপার পথের মতোই)।
        wv.loadDataWithBaseURL("file:///android_asset/", htmlPrintMedia, "text/html", "UTF-8", null)
    }

    /* 🔴🔒 V701 — কাগজের আসল চওড়া। সাধারণত A4 (৭৯৪), কিন্তু কোনো ফোনে
       WebView যদি পাতাটা এর চেয়ে চওড়া করে সাজায়, তখন সেই চওড়াটাই ধরা হয় —
       নইলে ডান দিকটা কেটে যেত (TK-এর ছবির দোষ)। */
    private var renderWidthPx = A4_WIDTH_PX

    /** WebView-কে কাগজের চওড়ায় বসানো, যাতে CSS ঠিক ছাপার মতোই সাজে। */
    private fun layoutAt(view: WebView, heightPx: Int) {
        val h = max(heightPx, A4_HEIGHT_PX)
        val w = max(renderWidthPx, A4_WIDTH_PX)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, w, h)
    }

    private fun measureThenBuild(activity: Activity, view: WebView) {
        try {
            /* 🔴🔒 V701 (TK-এর ৪টে ছবিতে ধরা — PDF-এর ডান দিক কেটে যাচ্ছিল) —
               এখন **চওড়া ও উচ্চতা দুটোই** মাপা হয়। আগে শুধু উচ্চতা মাপা হত আর
               চওড়া ধরে নেওয়া হত ৭৯৪; পাতা তার চেয়ে চওড়া হয়ে গেলে বাকিটা
               চুপচাপ কেটে যেত। এখন যা মাপা যায় তাই বসে, আর নিচে সেই মাপ ধরেই
               A4-তে ছোট করা হয় — তাই **কোনো ফোনেই আর কাটা পড়বে না**।
               ⛔ মাপা না গেলে আগের মতোই ৭৯৪ ধরা হয় (আচরণ বদলায় না)। */
            /* 🔴🔒🔒 V702 (২৬.০৮.২০২৬, TK: *"আপনি সঠিকভাবে কাজটা করুন, আমি এত
               বারবার বিল্ড করতে পারবো না"*) — **আর ধরে নেওয়া নয়, মেপে নেওয়া।**

               আগের দুটো চেষ্টা (V698 · V701) ব্যর্থ হয়েছে কারণ দুবারই আমি
               **ধরে নিয়েছিলাম** WebView পাতাটা কত বড় করে আঁকবে। কখনো সেটা
               ৩ গুণ বড় করেছে (ডান দিক কেটে গেছে), কখনো ছোট (সরু কলাম)।

               এখন কিছুই ধরে নেওয়া হয় না — **WebView নিজে কত বড় করে আঁকছে,
               সেটাই মেপে নেওয়া হয়:**

                   বড় করার মাপ (scale) = ঘরের চওড়া (device px) ÷ window.innerWidth (CSS px)

               `window.innerWidth` = WebView পাতাটাকে যত CSS-পিক্সেল চওড়া
               ধরে সাজিয়েছে। ঘরের চওড়া আমরা নিজেরাই বসিয়েছি। দুটো ভাগ করলেই
               আসল scale — WebView যা-ই করুক, সংখ্যাটা সত্যি।

               তারপর কাগজের আসল মাপ = পাতার CSS মাপ × সেই scale — আর ওই
               মাপেই ছবি তোলা ও A4-এ বসানো হয়।
               ⇒ **কোনো ফোনে, কোনো ঘনত্বে আর কাটা পড়তে পারে না।**
               ⛔ মাপা না গেলে (বিরল) আগের A4-এর হিসাবই চলে, কিছু ভাঙে না। */
            view.evaluateJavascript(
                "(function(){var d=document.documentElement,b=document.body;" +
                "var w=Math.max(b.scrollWidth,d.scrollWidth,b.offsetWidth,d.offsetWidth);" +
                "var h=Math.max(b.scrollHeight,d.scrollHeight,b.offsetHeight,d.offsetHeight);" +
                "return w+'x'+h+'x'+(window.innerWidth||0)})()"
            ) { value ->
                val raw = value?.trim()?.trim('"').orEmpty()
                val parts = raw.split("x")
                val wCss = parts.getOrNull(0)?.toFloatOrNull() ?: A4_WIDTH_PX.toFloat()
                val hCss = parts.getOrNull(1)?.toFloatOrNull() ?: A4_HEIGHT_PX.toFloat()
                val innerW = parts.getOrNull(2)?.toFloatOrNull() ?: 0f

                // ── WebView সত্যিই কত বড় করে আঁকছে ──
                /* ⚠️ ঘরটা পর্দার সাথে যুক্ত (V639), তাই মাঝেমধ্যে বাইরের
                   লেআউট ওটাকে ছোট করে দিতে পারে। তখন মাপা scale মিথ্যে হত।
                   ⇒ ঘরটা অস্বাভাবিক ছোট হলে মাপায় ভরসা করা হয় না, আগের
                     A4-এর সোজা হিসাবেই ফেরত যাওয়া হয়। */
                val viewW = view.width
                val trustView = viewW >= A4_WIDTH_PX / 2
                val scale = if (trustView && innerW > 1f) viewW / innerW else 1f
                // অসম্ভব মান এলে ভরসা করা হয় না (০.১× – ৮× এর বাইরে)।
                val safeScale = if (scale in 0.1f..8f) scale else 1f

                val contentW = max((wCss * safeScale).toInt(), A4_WIDTH_PX)
                val contentH = max((hCss * safeScale).toInt(), A4_HEIGHT_PX)
                renderWidthPx =
                    if (trustView && contentW <= A4_WIDTH_PX * 6) contentW else A4_WIDTH_PX

                layoutAt(view, contentH)
                // আঁকার আগে একটু সময় — ছবি ও ফন্ট বসে যেতে দিন।
                view.postDelayed({ buildPdfThenShare(activity, view, contentH) }, 250L)
            }
        } catch (_: Throwable) {
            layoutAt(view, A4_HEIGHT_PX)
            view.postDelayed({ buildPdfThenShare(activity, view, A4_HEIGHT_PX) }, 250L)
        }
    }

    private fun buildPdfThenShare(
        activity: Activity, view: WebView, totalHeightPx: Int
    ) {
        val outFile: File
        try {
            val dir = File(activity.cacheDir, "pdfs").apply { mkdirs() }
            val safeTitle = docTitle.replace(Regex("[^A-Za-z0-9]+"), "_")
            val safeName = docPatient.trim().replace(Regex("[^A-Za-z0-9]+"), "_").take(24)
            // নাম আলাদা রাখা হয়, যাতে পুরনো ফাইল ভুল করে WhatsApp-এ না যায়।
            outFile = File(dir, "${safeTitle}_${safeName}_${System.currentTimeMillis()}.pdf")
        } catch (e: Throwable) {
            detachKeepAlive(); fail(activity, "Could not create the file: ${e.message}"); return
        }

        val document = PdfDocument()
        var built = false
        try {
            /* 🔴🔒 V701 — উপরে যে চওড়ায় সত্যিই সাজানো হয়েছে, সেই চওড়াটাকেই
               A4-এর ৫৯৫ পয়েন্টে বসানো হয়। আগে সবসময় ৭৯৪ ধরা হত, তাই পাতা
               চওড়া হলে ডান দিক কেটে যেত। */
            /* 🔴🔴🔒 V981 (০২.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — *"প্রিন্ট আউটে
               তো কিছুই আসলো না"*: Estimate-এর PDF ও প্রিন্ট **সম্পূর্ণ ফাঁকা**)।

               ─── আসল কারণ (কোড ধরে প্রমাণিত, আন্দাজ নয়) ─────────────────
               মাপার ঘরটা (WebView) V639 থেকে পর্দার সাথে **১×১ পিক্সেল**
               LayoutParams নিয়ে যুক্ত থাকে। আমরা নিজেরা ওটাকে হাতে করে
               `layoutAt()` দিয়ে A4 মাপে বসাই — কিন্তু সেটা শুধু ওই মুহূর্তের
               জন্য। এর পর অ্যাক্টিভিটির উইন্ডোতে **একটাও লেআউট-পাস** চললেই
               Android ঘরটাকে তার নিজের LayoutParams অনুযায়ী আবার **১×১**
               করে দেয়। তখন নিচের `view.draw(canvas)` এক পিক্সেলের ঘর আঁকে
               ⇒ **পুরো সাদা পাতা**।

               Estimate-এ ঠিক সেটাই ঘটছিল: "Print / Share" চাপলে আগে
               `onDone()` চলে, যা Estimated Cost ঘরে `setText()` করে —
               তাতেই অ্যাক্টিভিটির লেআউট-পাস চাপা পড়ে থাকে; তারপর পপ-আপটাও
               বন্ধ হয়। ওই ২৫০ms-এর ভিতরেই পাস চলে ও ঘরটা ১×১ হয়ে যায়।
               Check-up-এর শেয়ারে আগে কোনো `setText()` নেই, পপ-আপ আগেই বন্ধ
               হয় — তাই ওটা এত দিন ঠিক চলেছে।

               ─── সমাধান ─────────────────────────────────────────────────
               আঁকার **ঠিক আগের মুহূর্তে** ঘরটাকে আবার কাগজের মাপে বসানো
               হয়। মাঝখানে কে কী লেআউট চালাল তাতে আর কিছু যায় আসে না।
               ⛔ কোনো টেমপ্লেট · মাপার হিসাব · শেয়ার/প্রিন্টের পথ বদলায়নি —
                  শুধু একটা লাইন আগে সরানো হলো, তাই সব পুরনো কাগজ (Check-up ·
                  Prescription · Diet · Investigation) হুবহু আগের মতোই থাকে। */
            layoutAt(view, totalHeightPx)

            val drawWidthPx = max(renderWidthPx, A4_WIDTH_PX)
            val scale = A4_WIDTH_PT.toFloat() / drawWidthPx.toFloat()
            val pageHeightPx = A4_HEIGHT_PT / scale           // এক পাতায় কত px ধরে
            /* 🔴🔒 V1249 — **শেষ এক-দুই পিক্সেলের জন্য যেন বাড়তি ফাঁকা পাতা না
               আসে।** A4-এর উচ্চতা ২৯৭mm = ১১২২.৫২ px; WebView সেটা মেপে দেয়
               ১১২৩ (বা কোনো ফোনে ১১২৪) — আধ পিক্সেল বেশি হলেই আগের হিসাবে
               `ceil` দুই পাতা বানিয়ে ফেলত, দ্বিতীয়টা প্রায় ফাঁকা।
               ⇒ এক পাতার ২%-এর কম উপচে পড়াকে উপচানো ধরা হয় না।
               ⛔ সত্যিকারের বড় কাগজ (যেমন লম্বা Check-up রিপোর্ট) আগের
                  মতোই সব পাতা পায় — ২২ px-এর বেশি উপচালেই নতুন পাতা। */
            val slackPx = pageHeightPx * 0.02f
            val pageCount = max(1, ceil((totalHeightPx - slackPx) / pageHeightPx).toInt())
            for (i in 0 until pageCount) {
                val info = PdfDocument.PageInfo
                    .Builder(A4_WIDTH_PT, A4_HEIGHT_PT, i + 1)
                    .create()
                val page = document.startPage(info)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                canvas.save()
                canvas.scale(scale, scale)
                canvas.translate(0f, -(i * pageHeightPx))
                view.draw(canvas)
                canvas.restore()
                document.finishPage(page)
            }
            FileOutputStream(outFile).use { document.writeTo(it) }
            built = true
        } catch (e: Throwable) {
            fail(activity, "Could not build the PDF: ${e.message}")
        } finally {
            try { document.close() } catch (_: Throwable) { }
            detachKeepAlive()
        }
        if (!built) return

        if (!outFile.exists() || outFile.length() <= 0L) {
            fail(activity, "The PDF came out empty — please try again")
            return
        }
        chooseWhatsAppAndSend(activity, outFile)
    }

    /**
     * 🔒 V449-এর লক করা নিয়ম এখানেও: কখনো নিঃশব্দে Personal WhatsApp বেছে নেওয়া
     * হয় না — সবসময় দুটো অপশনই দেখানো হয়, ব্যবহারকারী বেছে দিলে তবেই খোলে।
     */
    private fun chooseWhatsAppAndSend(activity: Activity, file: File) {
        val uri = try {
            FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        } catch (e: Throwable) {
            fail(activity, "Could not share the file: ${e.message}"); return
        }
        val caption = buildString {
            append(docTitle.uppercase(Locale.US))
            if (docPatient.isNotBlank()) append(" — ").append(docPatient.trim())
        }
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, caption)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // 🔴 V501: দরকার হলে তৃতীয় পথ — A4 কাগজে ছাপা বা PDF সেভ।
        val options = if (wantPrint) arrayOf("WhatsApp", "WhatsApp Business", "Print / PDF")
                      else arrayOf("WhatsApp", "WhatsApp Business")
        val packages = arrayOf(PKG_PERSONAL, PKG_BUSINESS)
        try {
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Send PDF with")
                .setItems(options) { _, which ->
                    if (which >= packages.size) { printPdf(activity, file); return@setItems }
                    try {
                        activity.startActivity(Intent(base).setPackage(packages[which]))
                    } catch (_: android.content.ActivityNotFoundException) {
                        Toast.makeText(activity, "${options[which]} is not installed on this phone", Toast.LENGTH_LONG).show()
                    } catch (e: Throwable) {
                        Toast.makeText(activity, "Could not open ${options[which]}: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
        } catch (e: Throwable) {
            fail(activity, "Could not show the WhatsApp choice: ${e.message}")
        }
    }

    /**
     * A4 কাগজে ছাপা / PDF সেভ — প্রকল্পের **নিজের প্রমাণিত** পথেই
     * (`PdfPrintDocumentAdapter`, ঠিক `PrintPreviewActivity.printPdf()`-এর মতো)।
     * ⛔ নতুন কোনো Print-ব্যবস্থা বানানো হয়নি।
     */
    private fun printPdf(activity: Activity, file: File) {
        try {
            val pm = activity.getSystemService(android.content.Context.PRINT_SERVICE)
                as android.print.PrintManager
            val jobName = buildString {
                append(docTitle)
                if (docPatient.isNotBlank()) append(" - ").append(docPatient.trim())
            }
            pm.print(jobName, PdfPrintDocumentAdapter(jobName, file),
                com.tkbiswas.pilesclinic.native.PrintQuality.builder().build())
        } catch (e: Throwable) {
            Toast.makeText(activity, "Print failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun fail(activity: Activity, message: String) {
        detachKeepAlive()
        try { Toast.makeText(activity, message, Toast.LENGTH_LONG).show() } catch (_: Throwable) { }
    }
}
