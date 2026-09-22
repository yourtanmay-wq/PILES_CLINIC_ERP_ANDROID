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
            findViewById<MaterialButton>(R.id.btnSavePdf).visibility = android.view.View.GONE
            findViewById<MaterialButton>(R.id.btnSharePdf).visibility = android.view.View.GONE
            findViewById<TextView>(R.id.tvError).apply {
                visibility = android.view.View.VISIBLE
                text = "Opening the print sheet for this ${model.documentTitle}.\n\n" +
                    "Your phone's print sheet can print it or save it as a PDF."
            }
            findViewById<MaterialButton>(R.id.btnPrint).setOnClickListener {
                PrescriptionHtmlPrint.print(this, model)
            }
            findViewById<MaterialButton>(R.id.btnClosePreview)?.setOnClickListener { finish() }
            PrescriptionHtmlPrint.print(this, model)
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
            // 🔒 B601 (TK-নির্দেশ 09.08.2026): "WhatsApp" বোতাম — PDF সরাসরি WhatsApp-এ
            // শেয়ার করে (রোগী বেছে পাঠাতে হয় — WhatsApp-এর নিজের সীমা)। WhatsApp/
            // WhatsApp Business না থাকলে সাধারণ Share তালিকা খোলে — কাজ আটকায় না।
            try {
                startActivity(Intent(base).setPackage("com.whatsapp"))
            } catch (_: android.content.ActivityNotFoundException) {
                try {
                    startActivity(Intent(base).setPackage("com.whatsapp.w4b"))
                } catch (_: android.content.ActivityNotFoundException) {
                    startActivity(Intent.createChooser(base, "Share PDF"))
                }
            }
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
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Toast.makeText(this, "Print failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
