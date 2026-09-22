package com.tkbiswas.pilesclinic.clinical

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.native.NativeSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

class DietChartActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_diet_chart)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        // TK-DECISION (2026-07-22): show the current patient under the title so
        // a Diet Chart can't be made for the wrong patient by mistake.
        run {
            // 🔒 খাতার সারি B175 (TK, 30.07.2026 — "Patient ID তো ফাইনাল ছিল,
            // প্রেসক্রিপশনে/সিস্টেমে নেই কেন?")। SET-এর দিকটা (patientDisplayId
            // পাঠানো) আগে থেকেই ঠিক ছিল, কিন্তু এই READ-এর দিকটা তখনও raw
            // `currentPatientId` পড়ত — তাই মানুষ-পড়া-যায় কোড থাকা সত্ত্বেও
            // পর্দায় raw আইডিই দেখাত। এখন `displayId()` (আছে থাকলে সেটাই,
            // নইলে আগের মতো raw)।
            val line = listOf(RoleSession.currentPatientName, RoleSession.displayId(), RoleSession.currentPatientDisease)
                .map { it.trim() }.filter { it.isNotBlank() }.joinToString(" · ")
            if (line.isNotBlank()) {
                supportActionBar?.subtitle = "👤 $line"
                toolbar.setSubtitleTextColor(android.graphics.Color.parseColor("#DDE7F2"))
            }
        }

        // Seed reference guidelines the first time, so the list is never blank.
        /* 🟢🔒 V595 (২৩.০৮.২০২৬, TK-নির্দেশ, ছবিসহ): *"সবগুলো ঠিক মারা থাকবে ·
           আমি চাইলে untick করতে পারি · তারপর ফাইনাল প্রিন্ট আউট হবে"*

           আগে প্রতিটা ঘর **ফাঁকা** এসে বসত (`DietEntry.isSelected`-এর ডিফল্ট
           `false`), তাই ডাক্তারকে প্রতিবার একটা একটা করে সবগুলোয় টিক দিতে হত —
           অথচ প্রায় সবসময়ই সবগুলোই লাগে।
           ⇒ এখন **সবগুলোয় আগে থেকেই টিক** থাকে; যেটা লাগবে না সেটায় চাপ দিয়ে
             তুলে দিলেই হলো।

           ⛔ ছাপা/সেভ/শেয়ার — তিনটেই আগের মতোই **শুধু টিক-দেওয়া** ঘরগুলোই নেয়
              (`filter { it.isSelected }`), তাই তুলে দেওয়া ঘর কাগজে যাবে না।
           ⛔ রোগী বদলালে তালিকাটা মুছে আবার নতুন করে বসে
              (`ClinicalRepository.resetForNewPatient()`), তাই আগের রোগীর বাছাই
              পরের রোগীর কাগজে কখনো যাবে না।
           ⛔ `DietEntry`-র ডিফল্ট (`isSelected = false`) ছোঁয়া হয়নি — অন্য কোথাও
              এই ক্লাসটা ব্যবহার হলে তার আচরণ এক অক্ষরও বদলায়নি। */
        if (ClinicalRepository.currentDiet.isEmpty()) {
            ClinicalRepository.dietAllowed.forEach {
                ClinicalRepository.currentDiet.add(
                    DietEntry(name = it, category = "Allowed", isSelected = true))
            }
            ClinicalRepository.dietAvoid.forEach {
                ClinicalRepository.currentDiet.add(
                    DietEntry(name = it, category = "Avoid", isSelected = true))
            }
        }

        val editable = RoleSession.canEditClinical()
        val llAllowed = findViewById<LinearLayout>(R.id.llAllowed)
        val llAvoid = findViewById<LinearLayout>(R.id.llAvoid)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveDiet)
        val btnSaveAndPrint = findViewById<MaterialButton>(R.id.btnSaveAndPrintDiet)
        val btnShare = findViewById<MaterialButton>(R.id.btnShareDiet)

        if (!editable) {
            findViewById<TextView>(R.id.tvReadOnlyNotice).visibility = android.view.View.VISIBLE
            btnSave.isEnabled = false
            btnSave.text = "Doctor-only"
            btnSaveAndPrint.isEnabled = false
        }

        ClinicalRepository.currentDiet.forEach { entry ->
            val cb = CheckBox(this)
            cb.text = entry.name
            cb.isChecked = entry.isSelected
            cb.isEnabled = editable
            // TK FIX (2026-07-15): items were touching each other with no gap --
            // made worse once each item became 3 lines (English/বাংলা/हिन्दी).
            // Added bottom margin between items and extra line spacing within
            // an item so the three languages don't press against each other
            // or against the next item.
            val d = resources.displayMetrics.density
            cb.setLineSpacing((3 * d), 1.05f)
            cb.setPadding(cb.paddingLeft, (6 * d).toInt(), cb.paddingRight, (6 * d).toInt())
            cb.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (10 * d).toInt() }
            cb.setOnCheckedChangeListener { _, isChecked -> entry.isSelected = isChecked }
            val target = if (entry.category == "Allowed") llAllowed else llAvoid
            target.addView(cb)
        }

        btnSave.setOnClickListener { saveDietChart(openPrintAfter = false) }
        btnSaveAndPrint.setOnClickListener { saveDietChart(openPrintAfter = true) }
        btnShare.setOnClickListener { shareDietChart() }
    }

    /** TK APPROVED (2026-07-15): plain text share, same pattern as Medicine
     *  Slip's "Share as Text" — sends via WhatsApp/SMS/any share target. */
    private fun shareDietChart() {
        val selected = ClinicalRepository.currentDiet.filter { it.isSelected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one diet guideline first.", Toast.LENGTH_SHORT).show()
            return
        }
        val allowed = selected.filter { it.category == "Allowed" }.map { it.name }
        val avoid = selected.filter { it.category == "Avoid" }.map { it.name }
        val sb = StringBuilder()
        // 🔒 TK-এর লক করা নিয়ম (খাতার সারি B72, 29.07.2026) — নাম BranchCatalog
        // থেকেই আসবে, হাতে লেখা নয়।
        sb.append(com.tkbiswas.pilesclinic.print.BranchCatalog.byName(RoleSession.currentPatientBranch).clinicName).append("\n")
        // 🔒 খাতার সারি B175 — শেয়ার/হোয়াটসঅ্যাপ টেক্সটেও মানুষ-পড়া-যায় আইডি।
        sb.append("Patient: ${RoleSession.currentPatientName} (${RoleSession.displayId()})\n\n")
        sb.append("Diet Chart\n")
        if (allowed.isNotEmpty()) sb.append("\n✓ Allowed:\n").append(allowed.joinToString("\n") { "  - $it" })
        if (avoid.isNotEmpty()) sb.append("\n\n✗ Avoid:\n").append(avoid.joinToString("\n") { "  - $it" })
        // 🔴 V430 — লেখা থাকলে "Extra Advice"-ও সঙ্গে যায় (কম্পিউটারের মতোই)।
        val dRem = dietRemarks()
        if (dRem.isNotBlank()) sb.append("\n\n").append(dRem)
        /* 📄🔒 V773 — TK: "share করলে PDF যেতে হবে"। উপরের `sb` (টেক্সট) আর
           পাঠানো হয় না; একই তথ্য এখন **A4 PDF** হয়ে যায় — ছাপার সময় যে
           কাগজটা তৈরি হয় (`DietChartHtml.build`) ঠিক সেটাই, তাই ছাপা ও শেয়ার
           করা কাগজ হুবহু এক। allowPrint = true ⇒ চাইলে সরাসরি ছাপাও যায়।
           ⛔ বাছাই · সেভ · ছাপা — কিছুই বদলায়নি, শুধু পাঠানোর ধরন। */
        com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
            activity = this,
            html = com.tkbiswas.pilesclinic.print.DietChartHtml.build(dietRemarks()),
            documentTitle = "Diet Chart",
            patientName = RoleSession.currentPatientName
        )   // ⛔ allowPrint দেওয়া হয়নি — এই পর্দায় নিজের "Save & Print" আগে
            //    থেকেই আছে (Investigation-এর মতোই), তাই ডুপ্লিকেট পথ নয়।
    }

    /** TK APPROVED (2026-07-15): printing matters more than just saving — added
     *  a "Save & Print" option (matching Prescription's pattern) so the chart
     *  can go straight to the print preview, same data Save persists. */
    /** 🔴 V430 — "Extra Advice" ঘরের লেখা। ঘরটা না থাকলেও কিছু ভাঙে না। */
    private fun dietRemarks(): String =
        try { findViewById<android.widget.EditText>(R.id.etDietRemarks)?.text?.toString()?.trim().orEmpty() }
        catch (_: Throwable) { "" }

    private fun saveDietChart(openPrintAfter: Boolean) {
        /* 🔴🔒 V786 — রোগী চেনা না গেলে (কল/মেমরির কারণে প্রসেস মরে পর্দা
           আবার খোলা) এখানেই থেমে যায়। আগে ফাঁকা আইডিতেও সেভ হয়ে যেত আর
           "saved" লেখা উঠত — ডাক্তারের লেখা চুপচাপ হারাত। */
        if (RoleSession.blockIfNoPatient(this)) return
        val selected = ClinicalRepository.currentDiet.filter { it.isSelected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Select at least one diet guideline before saving.", Toast.LENGTH_SHORT).show()
            return
        }
        val summary = selected.joinToString(", ") { it.name }.take(80)
        ClinicalRepository.addVisit("Diet Chart", summary, RoleSession.currentRole)
        // ROOT-CAUSE FIX: persist permanently to Supabase "medical" table.
        val selectedStr = selected.joinToString(", ") { "${it.category}: ${it.name}" }
        val createdBy = NativeSession.current(this)?.mobile ?: ""
        val pid = RoleSession.currentPatientId
        val pname = RoleSession.currentPatientName
        // 🔒 TK-এর নিয়ম (28.07.2026): Save চাপার সঙ্গে সঙ্গে প্রিন্ট পর্দা
        // খুলবে — ক্লাউডে পাঠানো শেষ হওয়ার জন্য স্টাফকে বসিয়ে রাখা যাবে না।
        // প্রিন্টের সব তথ্য ফোনেই আছে, ক্লাউডের কিছু লাগে না। সেভটা আগে
        // ফোনেই লেখা হয়, তারপর পিছনে ক্লাউডে যায়; না গেলে অপেক্ষমাণ
        // তালিকায় জমা থেকে নিজে থেকেই আবার যায়, তাই কিছু হারায় না।
        val appCtx = applicationContext
        // 🟡🔒 V708 — Investigation-এর হুবহু একই পাহারা (উপরের ফাইলের ব্যাখ্যা দেখুন)।
        DuplicateSaveGuard.run(this, pid, "Diet Chart", selectedStr, summary) {
        Toast.makeText(this@DietChartActivity, "Diet chart saved (${selected.size} item/s).", Toast.LENGTH_SHORT).show()
        com.tkbiswas.pilesclinic.native.BackgroundWork.run {
            ClinicalCloudRepository.saveMedical(appCtx, pid, pname, "Diet Chart", selectedStr, summary, createdBy)
        }
        if (openPrintAfter) {
            // 🥗🔒 V390 (১৬.০৮.২০২৬, TK-অনুমোদিত প্রুফ): Diet Chart এখন **অনুমোদিত A4
            // ডিজাইনে** ছাপে — ওয়েবের (`wlv1DietA4Html`) হুবহু একই HTML+CSS, WebView +
            // PrintManager দিয়ে। তাই ফোন ও কম্পিউটারের কাগজ এক।
            // ⛔ `ClinicPdfBuilder` (OWNER LOCKED) ছোঁয়া হয়নি — বাকি সব প্রিন্ট আগের পথেই।
            // ⛔ বাছাই/সেভ/ক্লাউড — উপরের কোডে কিচ্ছু বদলায়নি।
            // 🔴 V430 — পর্দার "Extra Advice" ঘরের লেখা কাগজে যায় (কম্পিউটারের মতোই)।
            //    ঘরটা ফাঁকা থাকলে কাগজ হুবহু আগের মতোই ছাপে।
            com.tkbiswas.pilesclinic.print.DietChartHtmlPrint.print(this@DietChartActivity, dietRemarks())
        }
        }   // 🟡 V708 — DuplicateSaveGuard.run ব্লকের শেষ
    }
}
