package com.tkbiswas.pilesclinic.clinical

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.PhotoUtils
import com.tkbiswas.pilesclinic.native.PremiumAlert
import com.tkbiswas.pilesclinic.native.SupabaseClient
import com.tkbiswas.pilesclinic.native.ZoomableImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/** TK APPROVED (2026-07-15): Blood Test / Investigation landing screen —
 *  category grid (Hematology / Bio-Chemistry / Immunology / Special Test /
 *  Urine / Stool / Semen / Imaging) transcribed test-by-test from TK's lab
 *  reference slip, plus two shortcut boxes above it — "Previous Patient
 *  Blood Test" (re-checks whatever was saved last time) and "Common Blood
 *  Test" (a fixed 7-test set TK defined, 01.08.2026) — both open the same
 *  tick/untick + add-extra dialog. Tapping a category opens
 *  InvestigationCategoryActivity for just that category's checklist. All
 *  selections still live in the same shared ClinicalRepository.currentInvestigations
 *  list as before, so Approve/Save/Print/Share keep working exactly as before. */
class InvestigationAdviceActivity : AppCompatActivity() {

    private lateinit var categoryContainer: LinearLayout

    /* 🩸🔒 V1464 (১৪.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — ব্লাড টেস্ট
       রিপোর্টের ছবি এই পর্দাতেই তোলা/জমা রাখা যায়, ঠিক PatientPhotoActivity-র
       প্রমাণিত ক্যামেরা/গ্যালারি ধাঁচেই (PhotoUtils.encodeResized — 600px/85%,
       ~৭০ KB প্রতিটা)। জমা হয় `medical.photos`-এ `{"reports":[...]}` আকারে —
       Doctor Checkup-এর আগে-পরের ছবির (`{"before":...}`) পাশাপাশি, একই ঘর
       শুধু আলাদা `type` (Investigation) বলে দুটো কখনো মেশে না।
       ⛔ ফাঁকা রাখলে (ছবি না যোগ করলে) আগের মতোই কিছুই বদলায় না। */
    private val reportPhotos = mutableListOf<String>()
    private var reportCameraUri: Uri? = null
    private lateinit var llReportPhotos: LinearLayout

    private val pickReportImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) addReportPhotoFromUri(uri)
    }
    private val requestReportCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openReportCamera()
            else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    private val takeReportPicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = reportCameraUri
            if (success && uri != null) addReportPhotoFromUri(uri)
        }

    private fun openReportCamera() {
        try {
            val dir = java.io.File(cacheDir, "images").apply { mkdirs() }
            val file = java.io.File(dir, "invreport_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            reportCameraUri = uri
            takeReportPicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera could not open", Toast.LENGTH_SHORT).show()
        }
    }

    /** ⛔ PatientPhotoActivity.showPhotoSourceDialog()-এর হুবহু একই পপ-আপ। */
    private fun showReportPhotoSourceDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Report Photo"))
            .setItems(arrayOf("📷 Camera", "🖼️ Gallery")) { _, which ->
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (which == 0) {
                    if (granted) openReportCamera() else requestReportCameraPermission.launch(android.Manifest.permission.CAMERA)
                } else pickReportImage.launch("image/*")
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun addReportPhotoFromUri(uri: Uri) {
        lifecycleScope.launch {
            val dataUrl = withContext(Dispatchers.IO) { PhotoUtils.encodeResized(this@InvestigationAdviceActivity, uri) }
            if (dataUrl == null) {
                Toast.makeText(this@InvestigationAdviceActivity, "Could not read image", Toast.LENGTH_SHORT).show()
                return@launch
            }
            reportPhotos.add(dataUrl)
            renderReportPhotoRow()
        }
    }

    /** থাম্বনেইল + একটা "+" বোতাম — চাপলে ক্যামেরা/গ্যালারি খোলে; কোনো থাম্বনেইলে
     *  চাপলে বড় করে দেখায় (zoom) সহ মোছার সুযোগ থাকে। */
    private fun renderReportPhotoRow() {
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        llReportPhotos.removeAllViews()
        reportPhotos.forEachIndexed { idx, dataUrl ->
            val bmp = PhotoUtils.decodeDataUrl(dataUrl)
            llReportPhotos.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { rightMargin = dp(8) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                if (bmp != null) setImageBitmap(bmp)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(1), android.graphics.Color.parseColor("#B9CBE0"))
                }
                clipToOutline = true
                setOnClickListener { showReportPhotoViewer(dataUrl, "Report Photo") { reportPhotos.removeAt(idx); renderReportPhotoRow() } }
            })
        }
        llReportPhotos.addView(TextView(this).apply {
            text = "+"
            gravity = android.view.Gravity.CENTER
            textSize = 20f
            setTextColor(android.graphics.Color.parseColor("#0B66D8"))
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1) * 2, android.graphics.Color.parseColor("#0B66D8"))
                color = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            }
            setOnClickListener { showReportPhotoSourceDialog() }
        })
    }

    /** ফুল-স্ক্রিন জুম-করা ভিউয়ার (ZoomableImageHelper, PatientPhotoActivity-র
     *  প্রমাণিত ধাঁচ)। [onDelete] ফাঁকা মানে মোছার বোতাম দেখাবে না (পুরনো
     *  রিপোর্ট দেখার সময় — সেগুলো এখান থেকে মোছা যায় না, ভুল করে ডেটা
     *  হারানোর ঝুঁকি এড়াতে)। */
    private fun showReportPhotoViewer(dataUrl: String, title: String, onDelete: (() -> Unit)? = null) {
        val bmp = PhotoUtils.decodeDataUrl(dataUrl) ?: return
        val iv = ImageView(this).apply {
            setImageBitmap(bmp)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        ZoomableImageHelper.attach(iv)
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, title))
            .setView(iv)
            .setPositiveButton("Close", null)
        if (onDelete != null) builder.setNegativeButton("Remove") { _, _ -> onDelete() }
        builder.show().also { PremiumAlert.paint(it) }
    }

    /** এই রোগীর আগে সেভ করা Investigation-এর রিপোর্ট ছবি — সস্তা প্রশ্ন
     *  (`select=id`, `photos=not.is.null`) দিয়ে আগে কোন সারিতে ছবি আছে তা
     *  জানা হয়, তারপর সেই কয়েকটা সারিরই `photos` আলাদা করে আনা হয় — বাকি
     *  পুরো তালিকার (MEDICAL_COLS, V794) ছবি-ছাড়া পড়া এক অক্ষরও বদলায় না। */
    private fun loadPreviousReports() {
        val pid = RoleSession.currentPatientId
        if (pid.isBlank()) return
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) {
                try {
                    val enc = java.net.URLEncoder.encode(pid, "UTF-8")
                    SupabaseClient.fetchListOrNull(
                        "medical", "patientId=eq.$enc&type=eq.Investigation&photos=not.is.null",
                        20, order = "createdAt.desc", select = "id,date,photos"
                    )
                } catch (_: Throwable) { null }
            } ?: return@launch
            if (isFinishing || isDestroyed) return@launch
            val lblPrev = findViewById<TextView>(R.id.tvPrevReportsLabel)
            val hsv = findViewById<HorizontalScrollView>(R.id.hsvPrevReports)
            val container = findViewById<LinearLayout>(R.id.llPrevReports)
            container.removeAllViews()
            val dens = resources.displayMetrics.density
            fun dp(v: Int) = (v * dens).toInt()
            var any = false
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val photosJson = try { org.json.JSONObject(row.optString("photos", "")) } catch (_: Throwable) { null } ?: continue
                val arr = photosJson.optJSONArray("reports") ?: continue
                val date = row.optString("date", "")
                for (j in 0 until arr.length()) {
                    val dataUrl = arr.optString(j, "")
                    if (dataUrl.isBlank()) continue
                    val bmp = PhotoUtils.decodeDataUrl(dataUrl) ?: continue
                    any = true
                    container.addView(ImageView(this@InvestigationAdviceActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { rightMargin = dp(8) }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageBitmap(bmp)
                        background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            setStroke(dp(1), android.graphics.Color.parseColor("#B9CBE0"))
                        }
                        clipToOutline = true
                        setOnClickListener { showReportPhotoViewer(dataUrl, "Report Photo · $date") }
                    })
                }
            }
            lblPrev.visibility = if (any) android.view.View.VISIBLE else android.view.View.GONE
            hsv.visibility = if (any) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    /* 🔴🔒 V786 (২৮.০৮.২০২৬, TK-রিপোর্ট: হেডারে "Patient / - / -") —
       ফোনে কল এলে বা মেমরি কম পড়লে Android অ্যাপের প্রসেস বন্ধ করে দেয়;
       পরে এই পর্দাটা আবার খোলে, কিন্তু মেমরির `RoleSession` ততক্ষণে ফাঁকা।
       তাই রোগীর পরিচয় এই পর্দার নিজের Bundle-এও রাখা হয় — Bundle প্রসেস
       মরলেও বাঁচে, আর V721-এর ৩০ মিনিটের সীমাও এতে লাগে না।
       ⛔ মেমরিতে রোগী থাকলে `restoreFrom()` কিচ্ছু করে না (RoleSession.kt)। */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        RoleSession.saveTo(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoleSession.restoreFrom(savedInstanceState)   // 🔴🔒 V786 — কল/মেমরির কারণে হারানো রোগী ফেরানো
        ClinicalRepository.attachInvestMemory(this)
        setContentView(R.layout.activity_investigation_advice)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        /* 🎨🔒 V767 (২৭.০৮.২০২৬, TK-অনুমোদিত **ডিজাইন B**, ডেমো ফটো দেখে বাছা)
           TK: *"পেশেন্ট ডিটেইলসের হেডারটা Check Up সেকশনের হেডারের সাথে মিলিয়ে দিন"*

           **আগে কী হত:** রোগীর নাম·ID·রোগ টুলবারের **সাবটাইটেলে** বসত। টুলবারে
           জায়গা কম, তাই তৃতীয় লাইনটা (COB-… · Piles, Fistula) **অর্ধেক কেটে**
           যেত — TK ছবিতে সেটাই দেখিয়েছেন।

           **এখন:** টুলবারটাই নেই (Check-Up পর্দার মতো), রোগীর তথ্য একটা
           পরিষ্কার কার্ডে — কাটার প্রশ্নই ওঠে না।
           ⛔ পিছনে ফেরা ফোনের নিজের Back-এ চলে (Check-Up-এ ঠিক যেভাবে চলে)।
           ⛔ নাম/ID/রোগ যেখান থেকে আসে (`RoleSession`) তা এক অক্ষরও বদলায়নি। */
        /* 🎨🔒 V1473 (১৪.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ, "প্রস্তাব ১ পাশ") —
           প্রফেশনাল কার্ড, এখনো এই একই তিনটে তথ্য (নাম·ID·রোগ) দিয়েই —
           `RoleSession`-এ যা আগে থেকেই আছে, নতুন কোনো নেটওয়ার্ক ডাক নেই। */
        run {
            // 🔒 খাতার সারি B175 — মানুষ-পড়া-যায় Patient ID।
            val name = RoleSession.currentPatientName.trim()
            val pid = RoleSession.displayId().trim()
            val disease = RoleSession.currentPatientDisease.trim()
            val branch = RoleSession.currentPatientBranch.trim()
            val card = findViewById<android.view.View>(R.id.llInvPatientCard)
            if (card != null && name.isNotBlank()) {
                findViewById<TextView>(R.id.tvInvPatientName).text = name
                findViewById<TextView>(R.id.tvInvAvatarInitials).text = name
                    .split(Regex("\\s+")).filter { it.isNotBlank() }
                    .take(2).joinToString("") { it.first().uppercaseChar().toString() }
                    .ifBlank { "?" }
                val sub = listOf(disease, branch).filter { it.isNotBlank() }.joinToString(" · ")
                val tvSub = findViewById<TextView>(R.id.tvInvPatientSub)
                tvSub.text = sub
                tvSub.visibility = if (sub.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
                val idChip = findViewById<android.view.View>(R.id.llInvIdChip)
                if (pid.isNotBlank()) {
                    findViewById<TextView>(R.id.tvInvPatientIdValue).text = pid
                    idChip.visibility = android.view.View.VISIBLE
                } else idChip.visibility = android.view.View.GONE
                card.visibility = android.view.View.VISIBLE
            }
        }

        categoryContainer = findViewById(R.id.llCategoryContainer)

        val btnSave = findViewById<MaterialButton>(R.id.btnSaveInvestigations)
        val btnSaveAndPrint = findViewById<MaterialButton>(R.id.btnSaveAndPrintInvestigations)
        // TK-REQUESTED REMOVAL (2026-07-16): "MARK SELECTED AS ADVISED
        // (DOCTOR)" and "SHARE AS TEXT" buttons removed — TK decided staff
        // can do this work without a separate Doctor-approval step, and
        // Save & Print already offers sharing, so a separate Share button
        // was redundant. The role notice banner is no longer needed either.
        findViewById<TextView>(R.id.tvRoleNotice).visibility = android.view.View.GONE

        btnSave.setOnClickListener { saveInvestigations(openPrintAfter = false) }
        btnSaveAndPrint.setOnClickListener { saveInvestigations(openPrintAfter = true) }

        // 🔒 TK-এর নির্দেশ (01.08.2026): Share বোতাম — Medicine Slip/Prescription-এর
        // হুবহু একই প্যাটার্ন (plain-text ACTION_SEND)। শুধু এখন যা টিক করা তাই যায়।
        findViewById<MaterialButton>(R.id.btnShareInvestigations).setOnClickListener { shareInvestigations() }

        // 🩸🔒 V1464 — এই সেশনে নতুন তোলা রিপোর্ট-ছবির সারি + আগে সেভ করা রিপোর্ট।
        llReportPhotos = findViewById(R.id.llReportPhotos)
        renderReportPhotoRow()
        loadPreviousReports()

        buildScreen()
    }

    /** 🔒 B277 (02.08.2026): আগে এই লজিকটা শুধু main-screen-এর Share বোতামের
     *  ভিতরেই লেখা ছিল — এখন আলাদা ফাংশনে বার করা হলো যাতে "Common Blood Test"
     *  পপ-আপের নতুন Share বোতামও (TK-এর নির্দেশে) ঠিক এই একই কোড ব্যবহার করে,
     *  আলাদা করে দ্বিতীয়বার লেখা লাগেনি। ⛔ ভিতরের লজিক এক অক্ষরও বদলায়নি। */
    /**
     * 📄🔒 V765 (২৭.০৮.২০২৬, TK-নির্দেশ ছবিসহ: *"এখানে share এ চাপলে Text কেন
     * যাবে, A4 Size এর PDF যেতে হবে"*)
     *
     * **আগে কী হত:** `Intent.ACTION_SEND` + `type = "text/plain"` — অর্থাৎ
     * শুধু কয়েক লাইন লেখা যেত, কোনো কাগজ নয়।
     *
     * **এখন:** প্রিন্টে যে **হুবহু একই A4 কাগজ** যায়
     * (`InvestigationHtmlPrint.build()`), সেটাই PDF বানিয়ে পাঠানো হয়
     * (`PrescriptionWhatsAppShare.shareHtml()` — প্রজেক্টের প্রমাণিত যন্ত্র,
     * প্রেসক্রিপশন ও Check-up History-তে বহুদিন ধরে চলছে)।
     * ⇒ **নতুন কোনো কাগজ বা PDF-যন্ত্র বানানো হয়নি** — যা ছিল তাই জোড়া হলো,
     *   তাই প্রিন্ট আর শেয়ারের কাগজ কখনো আলাদা হবে না।
     * ⛔ "কোনো টেস্ট বাছা হয়নি" পাহারা আগের মতোই আছে।
     */
    private fun shareInvestigations() {
        val chosen = ClinicalRepository.currentInvestigations.filter { it.isSelected }
        if (chosen.isEmpty()) {
            Toast.makeText(this, "No tests selected yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val html = com.tkbiswas.pilesclinic.print.InvestigationHtml.build(invRemarks())  // ⚠️ V769 — `build()` আছে `InvestigationHtml`-এ, `InvestigationHtmlPrint`-এ নয়
                //    (দুটোই একই ফাইলে — ফাইলের নাম ধরে লিখে ফেলাই ছিল ভুল)
            com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
                activity = this,
                html = html,
                documentTitle = "Blood Test Advice",
                patientName = RoleSession.currentPatientName
            )
        } catch (_: Throwable) {
            Toast.makeText(this, "Could not prepare the PDF. Please try Print instead.", Toast.LENGTH_LONG).show()
        }
    }

    /** TK APPROVED (2026-07-15): printing matters more than just saving — added
     *  a "Save & Print" option (same pattern as Prescription/Diet Chart) so the
     *  advice sheet can go straight to the print preview with one tap. */
    /** 🔴 V430 (TK-সিদ্ধান্ত ১৮.০৮.২০২৬) — পর্দার "Advice / Remarks" ঘরের লেখা।
     *  ঘরটা না থাকলেও (পুরনো লেআউট) কিছুই ভাঙে না — ফাঁকা লেখা ফিরে আসে। */
    private fun invRemarks(): String =
        try { findViewById<EditText>(R.id.etInvRemarks)?.text?.toString()?.trim().orEmpty() }
        catch (_: Throwable) { "" }

    private fun saveInvestigations(openPrintAfter: Boolean) {
        /* 🔴🔒 V786 — রোগী চেনা না গেলে (কল/মেমরির কারণে প্রসেস মরে পর্দা
           আবার খোলা) এখানেই থেমে যায়। আগে ফাঁকা আইডিতেও সেভ হয়ে যেত আর
           "saved" লেখা উঠত — ডাক্তারের লেখা চুপচাপ হারাত। */
        if (RoleSession.blockIfNoPatient(this)) return
        val requested = ClinicalRepository.currentInvestigations.filter { it.isSelected }
        if (requested.isEmpty()) {
            Toast.makeText(this, "No tests selected yet.", Toast.LENGTH_SHORT).show()
            return
        }
        // TK APPROVED (2026-07-15): this exact selection becomes the new
        // "Common Blood Test" default for next time.
        ClinicalRepository.saveCommonBloodTest(requested.map { it.name }.toSet())
        val summary = requested.joinToString(", ") { it.name }.take(80)
        ClinicalRepository.addVisit("Investigation Advice", summary, RoleSession.currentRole)
        val selectedStr = requested.joinToString(", ") { it.name }
        // 🔴 V430 — কম্পিউটারের মতোই লেখা মন্তব্যটাও সেভ হওয়া সারিতে থাকে।
        val invRem = invRemarks()
        val createdBy = NativeSession.current(this)?.mobile ?: ""
        val pid = RoleSession.currentPatientId
        val pname = RoleSession.currentPatientName
        // 🔒 TK-এর নিয়ম (28.07.2026): Save চাপার সঙ্গে সঙ্গে প্রিন্ট পর্দা
        // খুলবে — ক্লাউডে পাঠানো শেষ হওয়ার জন্য স্টাফকে বসিয়ে রাখা যাবে না।
        // প্রিন্টের সব তথ্য ফোনেই আছে, ক্লাউডের কিছু লাগে না। সেভটা আগে
        // ফোনেই লেখা হয়, তারপর পিছনে ক্লাউডে যায়; না গেলে অপেক্ষমাণ
        // তালিকায় জমা থেকে নিজে থেকেই আবার যায়, তাই কিছু হারায় না।
        val appCtx = applicationContext
        val detailsStr = if (invRem.isNotBlank()) invRem else summary
        /* 🟡🔒 V708 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত) — TK-এর ছবিতে
           ৩.১৭–৩.২১-এর মধ্যে **৫টা হুবহু এক Investigation**। কারণ: এই পর্দার
           `Save` ও `Save & Print` — দুটো বোতামই এই একই ফাংশন ডাকে, আর সেভের
           কোডে ডুপ্লিকেট যাচাই ছিল না। এখন আজকের হুবহু একই লেখা আগে থেকে
           থাকলে Warning আসে: **Cancel** = সেভ হবে না · **OK** = তবুও সেভ।
           ⛔ নেটের খরচ শূন্য (শুধু ফোনের জমা তালিকা দেখা হয়)।
           ⛔ Toast · প্রিন্ট · সেভ — তিনটেই আগের মতোই, শুধু সিদ্ধান্তের পরে। */
        // 🩸🔒 V1464 — এই সেশনে যোগ করা রিপোর্ট-ছবি (থাকলে) `{"reports":[...]}`
        //   আকারে যায়, Doctor Checkup-এর `{"before":...}`-এর পাশাপাশি একই ঘরে —
        //   `type` আলাদা (Investigation) বলে দুটো কখনো একে অন্যকে ছোঁয় না।
        //   ⛔ ফাঁকা থাকলে আগের মতোই কিছুই যায় না (photos = "")।
        val photosStr = if (reportPhotos.isNotEmpty())
            org.json.JSONObject().put("reports", org.json.JSONArray(reportPhotos)).toString() else ""
        DuplicateSaveGuard.run(this, pid, "Investigation", selectedStr, detailsStr) {
        Toast.makeText(this@InvestigationAdviceActivity, "Saved (${requested.size} test/s).", Toast.LENGTH_SHORT).show()
        com.tkbiswas.pilesclinic.native.BackgroundWork.run {
            ClinicalCloudRepository.saveMedical(appCtx, pid, pname, "Investigation", selectedStr,
                detailsStr, createdBy, photos = photosStr)
        }
        // ⛔ সেভের পরেও থাম্বনেইলগুলো এই পর্দাতেই দেখা যায় (মোছা হয় না) —
        // স্টাফ চোখেই দেখতে পান কোন ছবিগুলো এই মুহূর্তে সেভ হলো। পরের বার
        // পর্দাটা নতুন করে খুললে এগুলো "🗂 Previous Reports"-এ চলে আসবে
        // (`loadPreviousReports()`, `onCreate`-এ একবারই চলে)।
        if (openPrintAfter) {
            /* 🩸🔒 V596 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-ফটো দেখে): Blood Test এখন
               **অনুমোদিত A4 ডিজাইনে** ছাপে — ওয়েবের (`wlv1InvestigationA4`)
               হুবহু একই চেহারা, WebView + PrintManager দিয়ে। তাই ফোন ও
               কম্পিউটারের কাগজ এক। হুবহু যে পথে V390-তে Diet Chart গিয়েছিল।
               ⛔ `ClinicPdfBuilder` (OWNER LOCKED) ছোঁয়া হয়নি — বাকি সব প্রিন্ট
                  আগের পথেই। ⛔ বাছাই/সেভ/ক্লাউড — উপরের কোডে কিচ্ছু বদলায়নি। */
            com.tkbiswas.pilesclinic.print.InvestigationHtmlPrint.print(
                this@InvestigationAdviceActivity, invRemarks())
        }
        }   // 🟡 V708 — DuplicateSaveGuard.run ব্লকের শেষ
    }

    override fun onResume() {
        super.onResume()
        // Refresh counts in case selections changed inside a category screen.
        if (::categoryContainer.isInitialized && categoryContainer.childCount > 0) refreshCategoryGrid()
    }

    private fun buildScreen() {
        categoryContainer.removeAllViews()
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()

        // ---- 🔒 TK-এর নির্দেশ (01.08.2026): নাম বদলে "Previous Patient Blood
        // Test" — কাজ এক অক্ষরও বদলায়নি (applyCommonBloodTest(): শেষ সেভ করা
        // রোগীর টেস্ট আগে থেকে টিক করা, আনটিক/যোগ করা যায়)। শুধু উচ্চতা কমানো
        // হয়েছে (padding 16dp → 10dp/14dp)। ----
        val commonBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // TK-REPORTED BUG FIX (2026-07-16): the subtitle line was left-
            // aligned instead of centered (only visible because it's shorter
            // than the title line above it). gravity=CENTER_HORIZONTAL here
            // centers both wrap_content child lines within this card.
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                colors = intArrayOf(android.graphics.Color.parseColor("#0EA25F"), android.graphics.Color.parseColor("#0A3B20"))
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
            }
            isClickable = true; isFocusable = true
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10); layoutParams = lp
        }
        commonBtn.addView(TextView(this).apply {
            text = "⏰  Previous Patient Blood Test"
            textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE); gravity = android.view.Gravity.CENTER
        })
        commonBtn.addView(TextView(this).apply {
            text = "Re-checks whatever was saved last time"
            textSize = 10f; setTextColor(android.graphics.Color.parseColor("#CDEFDC")); gravity = android.view.Gravity.CENTER
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(2); layoutParams = p
        })
        commonBtn.setOnClickListener { applyCommonBloodTest() }
        categoryContainer.addView(commonBtn)

        // ---- 🔒 TK-এর নির্দেশ (01.08.2026): নতুন দ্বিতীয় বক্স — "Common Blood
        // Test", উপরেরটার সম্পূর্ণ আলাদা — রোগী বদলালেও এই ৭টা টেস্টই সবসময়
        // শুরুতে থাকে (আনটিক/যোগ করা যায়, শুধু এই রোগীর জন্য)। ----
        val fixedBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                colors = intArrayOf(android.graphics.Color.parseColor("#1E88C7"), android.graphics.Color.parseColor("#0D3E5C"))
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
            }
            isClickable = true; isFocusable = true
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(14); layoutParams = lp
        }
        fixedBtn.addView(TextView(this).apply {
            text = "⭐  Common Blood Test"
            textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE); gravity = android.view.Gravity.CENTER
        })
        fixedBtn.addView(TextView(this).apply {
            text = ClinicalRepository.commonBloodTestFixed.joinToString(" \u00b7 ")
            textSize = 10f; setTextColor(android.graphics.Color.parseColor("#D7ECFB")); gravity = android.view.Gravity.CENTER
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(2); layoutParams = p
        })
        fixedBtn.setOnClickListener { applyFixedCommonBloodTest() }
        categoryContainer.addView(fixedBtn)

        // ---- Category grid, 2 columns ----
        val palette = listOf(
            Triple("#FFE3E3", "#FFC9C9", "#7A1F1F"), Triple("#DCEBFF", "#B8D8FF", "#0F3A66"),
            Triple("#DFFBEA", "#B9F0CE", "#0B4F2A"), Triple("#FFF3D1", "#FCE2A1", "#7A5A0A"),
            Triple("#F0E2FF", "#DCC3FA", "#4B1F7A"), Triple("#D8F5F0", "#B3E9E0", "#0A5D50"),
            Triple("#E9EDF3", "#D2D9E4", "#374151"), Triple("#FDE7D6", "#FBCBA0", "#7A3B0A")
        )
        var row: LinearLayout? = null
        ClinicalRepository.investigationCategories.forEachIndexed { index, cat ->
            if (index % 2 == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = dp(10); layoutParams = lp
                }
                categoryContainer.addView(row)
            }
            val (c1, c2, textColor) = palette[index % palette.size]
            val selectedCount = ClinicalRepository.currentInvestigations.count { it.isSelected && cat.tests.contains(it.name) }
            // 🔒 TK-এর নির্দেশ (01.08.2026): "৮টা বক্সের সাইজ ছোট করুন, লেখা
            // বক্সের সাথে সামঞ্জস্যপূর্ণ" — padding ও লেখার মাপ কমানো হলো;
            // চাপ দিলে যা হতো (category screen খোলা) তা এক অক্ষরও বদলায়নি।
            // 🟢 V600 (২৩.০৮.২০২৬, TK-অনুমোদিত, ছবি-প্রুফ পাশ): "সাইজ আরো
            // ছোট করুন, আইকন থাকবে না, নামের লেখা একটু বড় করুন" — padding
            // 8/10dp → 6/6dp, ইমোজি আইকন সম্পূর্ণ বাদ, নামের সাইজ 11sp → 13sp।
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(dp(6), dp(6), dp(6), dp(6))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    colors = intArrayOf(android.graphics.Color.parseColor(c1), android.graphics.Color.parseColor(c2))
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(dp(4), 0, dp(4), 0); layoutParams = lp
                isClickable = true; isFocusable = true
            }
            card.addView(TextView(this).apply {
                text = cat.name; textSize = 13f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(textColor)); gravity = android.view.Gravity.CENTER
            })
            card.addView(TextView(this).apply {
                text = if (selectedCount > 0) "$selectedCount / ${cat.tests.size} selected" else "${cat.tests.size} tests"
                textSize = 9f; setTextColor(android.graphics.Color.parseColor(textColor)); gravity = android.view.Gravity.CENTER
            })
            card.setOnClickListener {
                startActivity(Intent(this, InvestigationCategoryActivity::class.java).apply {
                    putExtra("category_name", cat.name)
                })
            }
            row?.addView(card)
        }
        // if odd number of categories, balance the last row
        if (ClinicalRepository.investigationCategories.size % 2 == 1) {
            row?.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
            })
        }
    }

    private fun refreshCategoryGrid() = buildScreen()

    /** TK APPROVED (2026-07-15): "Apply Common Blood Test" (now labelled
     *  "Previous Patient Blood Test" on-screen — 01.08.2026) opens an
     *  interactive dialog instead of silently checking boxes in the background
     *  — shows the remembered test set with tick/untick checkboxes (all
     *  pre-checked, staff can uncheck any), plus a text box to type in extra
     *  tests not in the remembered set. Nothing is applied until "Apply" is
     *  tapped. Blank the first time (nothing saved yet) — never guesses. */
    private fun applyCommonBloodTest() {
        val remembered = ClinicalRepository.getCommonBloodTest()
        if (remembered.isEmpty()) {
            Toast.makeText(this, "No common set saved yet — select tests and Save once first.", Toast.LENGTH_SHORT).show()
            return
        }
        showBloodTestChecklistDialog("⏰ Previous Patient Blood Test", remembered.sorted())
    }

    /** 🔒 TK-এর নির্দেশ (01.08.2026): নতুন "Common Blood Test" বক্স — সবসময়
     *  TK-এর দেওয়া ৭টা ফিক্সড টেস্ট দিয়ে শুরু (রোগী বদলালেও বদলায় না),
     *  আনটিক/যোগ করা যায় — শেয়ার্ড ডায়ালগের হুবহু একই আচরণ (Previous
     *  Patient-এর মতোই), শুধু তালিকার উৎস আলাদা। ফাঁকা থাকার প্রশ্নই নেই
     *  (ফিক্সড লিস্ট), তাই "no set saved" চেক এখানে লাগে না। */
    private fun applyFixedCommonBloodTest() {
        showBloodTestChecklistDialog("⭐ Common Blood Test", ClinicalRepository.commonBloodTestFixed)
    }

    /** টিক/আনটিক + নিচে লিখে নতুন টেস্ট যোগ করার পপ-আপ — দুটো বক্স
     *  (Previous Patient · Common Blood Test) এই একই ফাংশন ডাকে, তাই
     *  আচরণ কখনো আলাদা হতে পারবে না। "Apply" চাপার আগ পর্যন্ত কিছুই সেভ হয় না।
     *  🔒 TK-এর নির্দেশ (02.08.2026, স্ক্রিনশটসহ — "সম্পূর্ণ প্রজেক্টে এই
     *  চেহারা এক থাকতে হবে"): এই তালিকার প্রতিটা সারিও এখন
     *  InvestigationCategoryActivity/PrintCenterActivity-এর হুবহু একই
     *  গোল টিক-বৃত্ত কার্ড ডিজাইনে, প্লেইন CheckBox নয় — একই স্ক্রিনের
     *  ভিতরেও যেন দুই রকম চেহারা না থাকে। */
    private fun showBloodTestChecklistDialog(title: String, names: List<String>) {
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        fun roundedBg(fill: String, stroke: String, radius: Int): android.graphics.drawable.GradientDrawable =
            android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = radius.toFloat()
                setColor(android.graphics.Color.parseColor(fill))
                setStroke(2, android.graphics.Color.parseColor(stroke))
            }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(4))
        }
        container.addView(TextView(this).apply {
            text = "Untick anything you don't need this time, or type more tests below."
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, 0, 0, dp(10))
        })

        // TK-এর নিয়ম মেনে "Apply" চাপার আগ পর্যন্ত কিছুই চূড়ান্ত হয় না —
        // তাই আগের CheckBox-দের বদলে `checkedNow` (স্থানীয় mutable set)
        // দিয়ে ট্র্যাক করা হয়, ঠিক Print Center-এর একই প্যাটার্নে।
        val checkedNow = names.toMutableSet()
        val rowNames = mutableListOf<String>()
        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun addRow(name: String) {
            rowNames.add(name)
            var isChecked = checkedNow.contains(name)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedBg(if (isChecked) "#EAFBF0" else "#FFFFFF", if (isChecked) "#0B4F2A" else "#E1E6ED", dp(12))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(8); layoutParams = lp
                isClickable = true; isFocusable = true
            }
            val circle = TextView(this).apply {
                text = if (isChecked) "✓" else ""
                textSize = 13f; gravity = android.view.Gravity.CENTER; setTextColor(android.graphics.Color.WHITE)
                background = roundedBg(if (isChecked) "#0B4F2A" else "#FFFFFF", "#B7C0CE", dp(20))
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            }
            val label = TextView(this).apply {
                text = name; textSize = 13.5f
                setTextColor(android.graphics.Color.parseColor(if (isChecked) "#0B4F2A" else "#10223A"))
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginStart = dp(12); layoutParams = lp
            }
            row.addView(circle); row.addView(label)
            row.setOnClickListener {
                isChecked = !isChecked
                if (isChecked) checkedNow.add(name) else checkedNow.remove(name)
                circle.text = if (isChecked) "✓" else ""
                circle.background = roundedBg(if (isChecked) "#0B4F2A" else "#FFFFFF", "#B7C0CE", dp(20))
                label.setTextColor(android.graphics.Color.parseColor(if (isChecked) "#0B4F2A" else "#10223A"))
                row.background = roundedBg(if (isChecked) "#EAFBF0" else "#FFFFFF", if (isChecked) "#0B4F2A" else "#E1E6ED", dp(12))
            }
            listContainer.addView(row)
        }
        names.forEach { addRow(it) }
        // Both the remembered checklist AND any newly-typed extra tests live in
        // this SAME bounded scroll area (one scroll region, not nested), so the
        // Add row below always stays reachable no matter how many are added.
        val scroll = android.widget.ScrollView(this).apply {
            addView(listContainer)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(260)
            )
        }
        container.addView(scroll)

        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(10); layoutParams = p
        }
        val addInput = android.widget.EditText(this).apply {
            hint = "Type another test name"
            // 🔴 TK-REQUESTED (02.08.2026, ছবিতে গোল করে দেখানো): এই বাক্সটা
            // উপরের টেস্ট-সারিগুলোর (HB/SUGAR/HIV ইত্যাদি) হুবহু একই সাইজ/
            // গোল-কোণা/বর্ডারে থাকবে, শুধু ভিতরটা সাদা (আনচেকড সারির রং)।
            // আগে `bg_input_field` (আলাদা ছোট, ভিন্ন প্যাডিং) ব্যবহার হত।
            background = roundedBg("#FFFFFF", "#E1E6ED", dp(12))
            textSize = 13.5f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.marginEnd = dp(8) }
        }
        val addBtn = MaterialButton(this).apply {
            text = "+ Add"
            setBackgroundResource(R.drawable.bg_btn_navy)
            setTextColor(android.graphics.Color.WHITE)
        }
        addRow.addView(addInput)
        addRow.addView(addBtn)
        container.addView(addRow)

        addBtn.setOnClickListener {
            val name = addInput.text.toString().trim()
            if (name.isBlank()) return@setOnClickListener
            if (rowNames.any { it.equals(name, ignoreCase = true) }) {
                Toast.makeText(this, "Already in the list.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkedNow.add(name)
            addRow(name)
            addInput.setText("")
        }

        UppercaseInputUtil.applyToAll(container)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        // 🔴 B277 (02.08.2026, TK-এর স্পষ্ট নির্দেশ, ছবিতে গোল করে দেখানো):
        // আগে এখানে শুধু "Cancel"/"Apply" ছিল — এখন মূল পাতার SAVE/SHARE/PRINT-এর
        // মতোই তিনটে বোতাম, প্রতিটা সত্যিকারের কাজ করে:
        //  · Share — টিক করা তালিকা সঙ্গে সঙ্গে Apply হয়ে যায়, তারপর ঠিক
        //    main-screen-এর Share বোতামের মতোই শেয়ার করে (shareInvestigations())।
        //  · Print — একইভাবে Apply হয়ে যায়, তারপর ঠিক main-screen-এর
        //    "Save & Print"-এর মতোই সেভ করে প্রিন্ট পর্দা খোলে (saveInvestigations
        //    (openPrintAfter = true))।
        //  · Cancel — আগের মতোই শুধু বন্ধ, কিছু Apply হয় না, পিছনের পাতায় ফেরে।
        // ⛔ "Apply"-এর ভিতরের আসল লজিক (টিক করা টেস্ট `currentInvestigations`-এ
        //    বসানো) এক অক্ষরও বদলায়নি — শুধু এখন একই লজিক দুইটা বোতাম থেকেই
        //    (Share ও Print) ডাকা হয়, তারপর যার যার নিজের কাজ।
        fun applyChosenNow(): Boolean {
            val chosen = rowNames.filter { checkedNow.contains(it) }
            if (chosen.isEmpty()) {
                Toast.makeText(this, "Nothing selected.", Toast.LENGTH_SHORT).show()
                return false
            }
            chosen.forEach { name ->
                val existing = ClinicalRepository.currentInvestigations.find { it.name == name }
                if (existing != null) {
                    existing.isSelected = true
                } else {
                    ClinicalRepository.currentInvestigations.add(InvestigationEntry(name = name, isSelected = true))
                }
            }
            refreshCategoryGrid()
            return true
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title))
            .setView(container)
            .setPositiveButton("Print") { _, _ ->
                if (applyChosenNow()) saveInvestigations(openPrintAfter = true)
            }
            .setNeutralButton("Share") { _, _ ->
                if (applyChosenNow()) shareInvestigations()
            }
            .setNegativeButton("Cancel", null)
            .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
    }
}
