package com.tkbiswas.pilesclinic.print

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.tkbiswas.pilesclinic.R
import java.io.File
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/**
 * Renders the PDF built by [ClinicPdfBuilder] natively (via PdfRenderer ->
 * Bitmap) for on-screen preview, then offers Save PDF / Share PDF / Print.
 * No WebView anywhere in this screen.
 */
class PrintPreviewActivity : AppCompatActivity() {

    private var pdfFile: File? = null
    private var currentPageIndex = 0
    private var pageCount = 1
    private var documentModel: PrintDocumentModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print_preview)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        /* 📱🔒 V1495 (১৫.০৯.২০২৬, TK-রিপোর্ট) — Report Card-এ নিচের Print/
           Share/Save/Close বার কাটা দেখাচ্ছিল (TK নিশ্চিত করলেন: শুধু Report
           Card-এই, প্রতিবার, স্ক্রল করে দেখারও উপায় নেই — মানে ছবিটাই এত লম্বা
           যে বোতাম-বারটা একদম নিচের কিনারায় গিয়ে ঠেকে, আর কিছু ফোনের নিজের
           নিচের জেসচার-বার সেই কিনারাটুকু ঢেকে দেয়)। অন্য কাগজে ছবি ছোট বলে
           এত নিচ অবধি যায় না, তাই ওখানে সমস্যাটা দেখা যায়নি। ⛔ কোনো নতুন
           edge-to-edge/থিম বসানো হয়নি — শুধু ফোনের নিজের নিচের বার যতটুকু
           জায়গা নেয়, ততটুকু বাড়তি প্যাডিং এই বোতাম-বারে যোগ হলো, যাতে
           বোতাম কখনো তার নিচে ঢাকা না পড়ে। বাকি সব পর্দায় (যেখানে এমনিতেই
           ফাঁকা জায়গা ছিল) এতে দৃশ্যত কিছু বদলাবে না। */
        try {
            val actionBar = findViewById<android.view.View>(R.id.actionButtonBar)
            val basePadBottom = actionBar.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(actionBar) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, basePadBottom + bars.bottom)
                insets
            }
        } catch (_: Throwable) { }

        // TK-REQUESTED (2026-07-25): a screen that already built its own
        // PDF (Chamber Register) hands it off here directly -- same Save/
        // Share/Print buttons below, no ClinicPdfBuilder involved at all.
        // Existing callers (Prescription, Registration, etc.) never set
        // this, so they fall straight through to the unchanged code below.
        val prebuilt = PrintDataHolder.prebuiltFile
        if (prebuilt != null) {
            pdfFile = prebuilt
            toolbar.title = PrintDataHolder.prebuiltTitle ?: "Document"
            PrintDataHolder.prebuiltFile = null
            PrintDataHolder.prebuiltTitle = null
            try {
                renderPage(0)
                setupPageNav()
                setupActionButtons()
                /* 💰🔒 V984 (TK-নির্দেশ: *"টাকা বুঝিয়ে দেয়ার সিস্টেমটা এই পর্দাতে
                   রাখুন"*) — শুধু Chamber Register-এর সময় ঘরটা ভরে; বাকি প্রতিটা
                   কাগজে ওটা লুকানোই থাকে, তাই কোথাও কিছু বদলায়নি। */
                try {
                    com.tkbiswas.pilesclinic.print.MoneyHandoverCard.attach(
                        this, findViewById(R.id.handoverSlot)
                    )
                } catch (_: Throwable) { }
            } catch (e: Exception) {
                showError("Could not render the PDF preview: ${e.message}")
            }
            return
        }

        val model = PrintDataHolder.pendingModel
        if (model == null) {
            showError("Nothing to preview. Go back and choose a document to print first.")
            return
        }
        documentModel = model
        toolbar.title = model.documentTitle

        // 🔵🔒 V409 (TK-অনুমোদিত প্রুফ, ১৭.০৮.২০২৬) — **Prescription ও Medicine Slip**
        //    এখন ওয়েবের অনুমোদিত ডিজাইনেই ছাপে (assets/www/rx_print.html →
        //    WebView → PrintManager), ঠিক যেভাবে Diet Chart (V390) ও Registration
        //    (B601) ছাপে। তাই ফোন ও কম্পিউটারের কাগজ হুবহু এক।
        //    ⛔ বাকি প্রতিটা কাগজ (Blood Test · Payment Receipt · Doctor Visit ·
        //       Check-up · Registration · Chamber Register) আগের ClinicPdfBuilder
        //       পথেই — নিচের কোডের একটা অক্ষরও বদলায়নি।
        //    ⛔ এই পর্দাটা **বন্ধ করা হয় না** (finish() ডাকা হয়নি) — WebView-টা
        //       এই Activity-র উপর দাঁড়িয়ে, ছাপার কাজ শেষ না হওয়া পর্যন্ত ওটাকে
        //       বাঁচিয়ে রাখতে হয়। Diet Chart-এও ঠিক এই নিয়মই মানা হয়।
        //    ℹ️ Save PDF / Share PDF বোতাম দুটো এই দুই কাগজে লুকানো — কারণ ওগুলো
        //       পুরনো নকশার PDF বানাত, তাহলে ছাপা আর শেয়ার করা কাগজ আলাদা হয়ে যেত।
        //       ফোনের নিজের প্রিন্ট-শিটেই "Save as PDF" আছে, তাই কিছু হারায় না।
        if (PrescriptionHtmlPrint.handles(model.documentTitle)) {
            findViewById<android.view.View>(R.id.pageNavRow).visibility = android.view.View.GONE
            findViewById<ImageView>(R.id.ivPreview).visibility = android.view.View.GONE
            // 🟢🔒🔒 V670 (২৫.০৮.২০২৬, TK-নির্দেশ, তিনটে অনুরোধ) —
            // ১) "প্রিন্ট হলো কিনা বোঝার উপায় নেই" — Android-এর PrintJob
            //    state-listener দিয়ে সত্যিকারের ফলাফল দেখানো হয় (নিচে)।
            // ২) "যেটা প্রিন্ট আউট হলো এটা যেন দেখা যায়" — একই HTML এখন
            //    সরাসরি WebView-তে (wvRxPreview) দেখানো হয়।
            // ৩) "এখান থেকে WhatsApp-এ শেয়ার করা যাবে" — আগে থেকেই থাকা
            //    বোতাম (btnSharePdf, অন্য কাগজে ব্যবহৃত) এখানে দেখানো হলো,
            //    PrescriptionWhatsAppShare-এর প্রমাণিত পথে জোড়া হলো।
            val wvPreview = findViewById<android.webkit.WebView>(R.id.wvRxPreview)
            try {
                wvPreview.settings.javaScriptEnabled = false
                /* 🔵🔒 V954 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK: *"কী প্রিন্ট
                   আউট হবে সেটা যেন এখানে দেখতে পারি, এবং জুম করলে যেন বড়-ছোট হয়"*।
                     · `loadWithOverviewMode` + `useWideViewPort` ⇒ খোলার সাথে সাথেই
                       **পুরো কাগজটা** এঁটে দেখায় (আগে শুধু লোগোর টুকরো দেখাত)।
                     · `setSupportZoom` + `builtInZoomControls` ⇒ **চিমটি দিয়ে জুম**।
                       `displayZoomControls=false` ⇒ পর্দায় পুরনো ধাঁচের +/− বোতাম
                       ভেসে ওঠে না, চেহারা পরিষ্কার থাকে।
                   ⛔ ছাপার HTML এক অক্ষরও বদলায়নি — শুধু দেখানোর ধরন। */
                wvPreview.settings.loadWithOverviewMode = true
                wvPreview.settings.useWideViewPort = true
                wvPreview.settings.setSupportZoom(true)
                wvPreview.settings.builtInZoomControls = true
                wvPreview.settings.displayZoomControls = false
                /* 🖼️🔒 V1012 (০৩.০৯.২০২৬, TK-নির্দেশ: *"প্রিন্ট করার সময় যেটা
                   প্রিন্ট করব আমার ডিসপ্লেতে সেটুকুই দেখাবে"*)।
                   **আসল কারণ (কোড পড়ে):** ছাপার HTML-এ কোনো `viewport` লেখা
                   নেই — ওটা দরকারও নেই, কারণ ছাপার সময় কাগজের মাপই (A4)
                   বিবেচ্য। কিন্তু WebView-এ **পর্দায়** দেখাতে গেলে viewport
                   ছাড়া সে নিজের ডিফল্ট প্রস্থ ধরে নেয়, তাই V954-এর
                   `loadWithOverviewMode`/`useWideViewPort` কিছুই এঁটে দেখাতে
                   পারত না — ফলে শুধু লোগোর বিশাল টুকরো দেখাত।
                   ⇒ এখন **শুধু পর্দায় দেখানোর কপিটাতে** A4-এর প্রস্থ
                     (৭৯৪ px = ২১০mm @96dpi) viewport হিসেবে বসানো হয়, তাই
                     পুরো কাগজটা এঁটে দেখায় — যা ছাপবে ঠিক তাই।
                   ⛔ **ছাপার HTML এক অক্ষরও বদলায়নি** — নিচের ছাপা/শেয়ার
                      দুটোই আগের হুবহু একই `PrescriptionHtml.build()` ব্যবহার
                      করে; এই viewport লাইনটা শুধু এই প্রিভিউ-কপিতে। */
                val previewHtml = com.tkbiswas.pilesclinic.print.PrescriptionHtml.build(this, model)
                val previewFitted = if (previewHtml.contains("name=\"viewport\"", ignoreCase = true)) previewHtml
                    else previewHtml.replaceFirst(
                        "<head>",
                        "<head><meta name=\"viewport\" content=\"width=794, initial-scale=1\">",
                        ignoreCase = true)
                wvPreview.loadDataWithBaseURL("file:///android_asset/", previewFitted, "text/html", "UTF-8", null)
                /* 🔵 V954 — WebView এখন আলাদা কার্ডে, পুরো জায়গা জুড়ে; তাই ছবির
                   ScrollView-টা লুকিয়ে দেওয়া হয় (নইলে দুটো একসাথে জায়গা নিত)। */
                findViewById<android.view.View>(R.id.cardRxPreview).visibility = android.view.View.VISIBLE
                findViewById<android.view.View>(R.id.scrollImgPreview).visibility = android.view.View.GONE
            } catch (_: Throwable) { }
            findViewById<MaterialButton>(R.id.btnSavePdf).visibility = android.view.View.GONE
            findViewById<MaterialButton>(R.id.btnSharePdf).visibility = android.view.View.VISIBLE
            findViewById<MaterialButton>(R.id.btnSharePdf).setOnClickListener {
                com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.share(this, model)
            }
            val tvStatus = findViewById<TextView>(R.id.tvError)
            tvStatus.apply {
                visibility = android.view.View.VISIBLE
                text = "Opening the print sheet for this ${model.documentTitle}.\n\n" +
                    "Your phone's print sheet can print it or save it as a PDF."
            }
            findViewById<MaterialButton>(R.id.btnPrint).setOnClickListener {
                tvStatus.text = "Opening the print sheet for this ${model.documentTitle}…"
                PrescriptionHtmlPrint.print(this, model) { statusMsg -> runOnUiThread { tvStatus.text = statusMsg } }
            }
            findViewById<MaterialButton>(R.id.btnClosePreview)?.setOnClickListener { finish() }
            PrescriptionHtmlPrint.print(this, model) { statusMsg -> runOnUiThread { tvStatus.text = statusMsg } }
            return
        }

        try {
            val outDir = File(cacheDir, "pdfs").apply { mkdirs() }
            val safeName = model.documentTitle.replace(Regex("[^A-Za-z0-9]+"), "_")
            val fileName = "${safeName}_${System.currentTimeMillis()}.pdf"
            val file = File(outDir, fileName)
            // Auto-select the letterhead + watermark from the patient's own branch
            // (web uses branch(p.branch)), so a Kishanganj record prints as
            // "TK BISWAS PILES CLINIC" with the Biswas logo watermark.
            val branchForPrint = if (model.branchName.isNotBlank())
                BranchCatalog.byName(model.branchName) else BranchSession.current
            ClinicPdfBuilder(this).build(model, branchForPrint, file)
            pdfFile = file
            renderPage(0)
            setupPageNav()
        } catch (e: Exception) {
            showError("Could not generate the PDF: ${e.message}")
            return
        }

        setupActionButtons()
    }

    private fun showError(message: String) {
        findViewById<TextView>(R.id.tvError).apply {
            visibility = android.view.View.VISIBLE
            text = message
        }
        findViewById<android.view.View>(R.id.pageNavRow).visibility = android.view.View.GONE
        findViewById<ImageView>(R.id.ivPreview).visibility = android.view.View.GONE
    }

    private fun setupPageNav() {
        findViewById<MaterialButton>(R.id.btnPrevPage).setOnClickListener {
            if (currentPageIndex > 0) renderPage(currentPageIndex - 1)
        }
        findViewById<MaterialButton>(R.id.btnNextPage).setOnClickListener {
            if (currentPageIndex < pageCount - 1) renderPage(currentPageIndex + 1)
        }
    }

    private fun renderPage(index: Int) {
        val file = pdfFile ?: return
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount
                    val safeIndex = index.coerceIn(0, pageCount - 1)
                    renderer.openPage(safeIndex).use { page ->
                        val scale = 2 // render at 2x for a crisper on-screen preview
                        val bitmap = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        findViewById<ImageView>(R.id.ivPreview).setImageBitmap(bitmap)
                    }
                    currentPageIndex = safeIndex
                    findViewById<TextView>(R.id.tvPageIndicator).text = "Page ${safeIndex + 1} of $pageCount"
                    findViewById<MaterialButton>(R.id.btnPrevPage).isEnabled = safeIndex > 0
                    findViewById<MaterialButton>(R.id.btnNextPage).isEnabled = safeIndex < pageCount - 1
                }
            }
        } catch (e: Exception) {
            showError("Could not render the PDF preview: ${e.message}")
        }
    }

    private fun setupActionButtons() {
        findViewById<MaterialButton>(R.id.btnSavePdf).setOnClickListener { savePdf() }
        findViewById<MaterialButton>(R.id.btnSharePdf).setOnClickListener { sharePdf() }
        findViewById<MaterialButton>(R.id.btnPrint).setOnClickListener { printPdf() }
        // 🔒 B601 (TK-নির্দেশ): ওয়েবের মতো ৪র্থ বোতাম "Close" — পর্দা বন্ধ করে ফেরে।
        findViewById<MaterialButton>(R.id.btnClosePreview)?.setOnClickListener { finish() }
    }

    private fun savePdf() {
        val file = pdfFile ?: return
        try {
            val saveDir = getExternalFilesDir("PDFs") ?: filesDir
            saveDir.mkdirs()
            val destination = File(saveDir, file.name)
            file.copyTo(destination, overwrite = true)
            Toast.makeText(this, "Saved: ${destination.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdf() {
        val file = pdfFile ?: return
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val base = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // 🔒 V449 (TK-approved 19.08.2026): Print Preview → WhatsApp Share must
            // NEVER silently prefer Personal WhatsApp. Always show both choices so the
            // user explicitly selects WhatsApp or WhatsApp Business. This changes only
            // the Print/PDF share route; PDF contents, Save and Print remain untouched.
            val options = arrayOf("WhatsApp", "WhatsApp Business")
            val packages = arrayOf("com.whatsapp", "com.whatsapp.w4b")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Share PDF with")
                .setItems(options) { _, which ->
                    try {
                        startActivity(Intent(base).setPackage(packages[which]))
                    } catch (_: android.content.ActivityNotFoundException) {
                        Toast.makeText(this, "${options[which]} is not installed on this phone", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not open ${options[which]}: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
        } catch (e: Exception) {
            Toast.makeText(this, "Share failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun printPdf() {
        val file = pdfFile ?: return
        try {
            val printManager = getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
            // TK-REQUESTED (2026-07-25): documentModel is null on the
            // prebuilt-file path (Chamber Register) -- fall back to the
            // toolbar's own title instead of requiring the model, so
            // Print still works there exactly like it already does for
            // every model-based document.
            val model = documentModel
            val jobName = if (model != null) "${model.documentTitle} - ${model.patientName}" else (supportActionBar?.title?.toString() ?: "Document")
            val adapter = PdfPrintDocumentAdapter(jobName, file)
            printManager.print(jobName, adapter, com.tkbiswas.pilesclinic.native.PrintQuality.builder().build())
        } catch (e: Exception) {
            Toast.makeText(this, "Print failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
