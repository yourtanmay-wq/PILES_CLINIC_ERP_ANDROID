package com.tkbiswas.pilesclinic.clinical

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tkbiswas.pilesclinic.R
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.native.NativeSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

class PrescriptionActivity : AppCompatActivity() {

    private lateinit var adapter: PrescriptionAdapter
    private lateinit var tvEmptyState: TextView
    private var saveInProgress = false

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
        com.tkbiswas.pilesclinic.clinical.ClinicalRepository.attachDoseMemory(this)
        setContentView(R.layout.activity_prescription)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val editable = RoleSession.canEditClinical()
        tvEmptyState = findViewById(R.id.tvEmptyState)
        val rv = findViewById<RecyclerView>(R.id.rvMedicines)
        rv.layoutManager = LinearLayoutManager(this)

        /* 🔴🔵🔒 V537 (২২.০৮.২০২৬, TK-রিপোর্ট) — **দ্বিতীয় পাহারা।**
           কেন্দ্রীয় পাহারাটা `RoleSession.applyFrom()`-এ আছে; কোনো কারণে
           সেটা না চললেও (যেমন পর্দাটা সরাসরি খোলা হলে) এখানে আরেকবার
           দেখা হয় — তালিকাটা কি সত্যিই **এই** রোগীর?
           ⛔ একই রোগী হলে কিচ্ছু হয় না, চলতি কাজ কখনো মুছবে না। */
        ClinicalRepository.ensureListsBelongTo(RoleSession.currentPatientId)
        adapter = PrescriptionAdapter(ClinicalRepository.currentPrescription, editable) { position ->
            ClinicalRepository.currentPrescription.removeAt(position)
            adapter.notifyItemRemoved(position)
            refreshEmptyState()
        }
        rv.adapter = adapter
        refreshEmptyState()

        val btnAddFromReference = findViewById<MaterialButton>(R.id.btnAddFromReference)
        val btnAddBlank = findViewById<MaterialButton>(R.id.btnAddBlank)
        val btnSave = findViewById<MaterialButton>(R.id.btnSavePrescription)
        val btnSaveAndPrint = findViewById<MaterialButton>(R.id.btnSaveAndPrint)

        if (!editable) {
            findViewById<TextView>(R.id.tvReadOnlyNotice).visibility = android.view.View.VISIBLE
            btnAddFromReference.visibility = android.view.View.GONE
            btnAddBlank.visibility = android.view.View.GONE
            btnSave.isEnabled = false
            btnSave.text = "Doctor-only"
            btnSaveAndPrint.isEnabled = false
        }

        btnAddFromReference.setOnClickListener { showReferencePicker() }

        btnAddBlank.setOnClickListener { showCustomMedicineDialog() }

        btnSave.setOnClickListener { savePrescription(openPrintAfter = false) }

        btnSaveAndPrint.setOnClickListener { savePrescription(openPrintAfter = true) }


        // TK-REQUESTED CHANGE (2026-07-19): always jump straight into the
        // reference-list picker when editable — this used to only happen on
        // the very first (empty-list) open; now it also happens when adding
        // more medicines to an already-started Prescription, so the middle
        // review screen is skipped for both the new and the edit case.
        // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B38):
        // *"যেখান থেকে ঢুকেছিলাম, ব্যাক করলে আবার সেখানেই থাকতে হবে। মাঝপথে এই
        // দ্বিতীয় পর্দাটা কোথা থেকে আসছে? এটা আসা বন্ধ করুন।"*
        //
        // **আসল কারণ:** এই পর্দাটা খোলার সঙ্গে সঙ্গেই ওষুধের তালিকা দেখায়, তাই
        // ঢোকার সময় পর্দাটা চোখেই পড়ে না। কিন্তু তালিকা থেকে ব্যাক করলে **নিচের
        // ফাঁকা পর্দাটা বেরিয়ে পড়ত** — সেটাই TK-এর দ্বিতীয় ফটো।
        //
        // **এখন:** ঢোকার সময় নিজে থেকে খোলা তালিকা থেকে **কিছু না নিয়ে ব্যাক
        // করলে এই পর্দাটাও বন্ধ হয়ে যায়** — তাই যেখান থেকে এসেছিলেন সোজা
        // সেখানেই ফিরবেন।
        // ⛔ বোতাম চেপে নিজে তালিকা খুললে আগের মতোই — ব্যাক করলে এই পর্দাতেই
        // থাকবেন, কিছু হারাবে না।
        if (editable) {
            // 🔴 V743 — আগে এখানে `openedFromEntry = true` বসত, যেটা দিয়ে
            //    পিকার-বাতিলে পর্দা বন্ধ করা হতো (B38/B177)। TK-এর নতুন
            //    সিদ্ধান্তে ওই নিয়মটাই উঠে গেছে, তাই ঘরটারও দরকার নেই।
            // Read only this patient's last Doctor Check-up before the picker
            // opens. Failure safely falls back to the patient-bound phone copy.
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    PrescriptionOptionsStore.refreshFromLatestCheckup(applicationContext)
                }
                if (!isFinishing && !isDestroyed) showReferencePicker()
            }
        }
    }

    /** ROOT-CAUSE FIX (2026-07-15): "Generate Medicine Slip" button removed from
     *  Prescription screen (TK approved) — Prescription and Medicine Slip are
     *  separate documents with separate medicine lists now. Replaced with
     *  Save / Save & Print (prints the Prescription itself, not the Slip).
     *  @param finishAfter closes this screen only after the cloud save finishes,
     *  so the direct-add flow (showReferencePicker) never cuts the save short. */
    private fun savePrescription(openPrintAfter: Boolean, finishAfter: Boolean = false) {
        /* 🔴🔒 V786 — রোগী চেনা না গেলে (কল/মেমরির কারণে প্রসেস মরে পর্দা
           আবার খোলা) এখানেই থেমে যায়। আগে ফাঁকা আইডিতেও সেভ হয়ে যেত আর
           "saved" লেখা উঠত — ডাক্তারের লেখা চুপচাপ হারাত। */
        if (RoleSession.blockIfNoPatient(this)) return
        if (ClinicalRepository.currentPrescription.isEmpty()) {
            Toast.makeText(this, "Add at least one medicine before saving.", Toast.LENGTH_SHORT).show()
            return
        }
        // Prevent a fast double tap from starting two checks/two saves.
        if (saveInProgress) return
        saveInProgress = true
        val pid = RoleSession.currentPatientId
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                ClinicalCloudRepository.checkSameDayPrescription(applicationContext, pid)
            }
            when (result) {
                ClinicalCloudRepository.SameDayPrescriptionCheck.EXISTS -> {
                    AlertDialog.Builder(this@PrescriptionActivity)
                        .setTitle("Prescription already saved today")
                        .setMessage("A Prescription has already been saved for this patient today. Do you want to make another one?")   /* 🔤 V726 */
                        .setNegativeButton("No") { _, _ -> saveInProgress = false }
                        .setPositiveButton("Yes") { _, _ -> commitPrescription(openPrintAfter, finishAfter) }
                        .setOnCancelListener { saveInProgress = false }
                        // 🔴🔒 V512: বাংলা-বন্ধ স্টাফের (Kishanganj) পর্দায় এই পপ-আপটাও
                        //   ইংরেজিতে দেখাবে। এই পপ-আপ PremiumAlert দিয়ে আঁকা হয় না,
                        //   তাই এতদিন বাদ পড়ে যেত। ⛔ বাংলা-বন্ধ না থাকলে
                        //   `installDialog()` প্রথম লাইনেই ফিরে যায় — অর্থাৎ বাকি
                        //   সবার জন্য **এক অক্ষরও বদলায়নি**।
                        .show().also { com.tkbiswas.pilesclinic.native.NoBengali.installDialog(it); com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) }   // 🤫 V774
                }
                ClinicalCloudRepository.SameDayPrescriptionCheck.NONE ->
                    commitPrescription(openPrintAfter, finishAfter)
                ClinicalCloudRepository.SameDayPrescriptionCheck.UNVERIFIED -> {
                    saveInProgress = false
                    AlertDialog.Builder(this@PrescriptionActivity)
                        .setTitle("Prescription could not be verified")   /* 🔤 V726 */
                        .setMessage("Check the internet connection and Save again. No Prescription was saved.")   /* 🔤 V726 */
                        .setPositiveButton("OK", null)
                        // 🔴🔒 V512: উপরেরটার হুবহু একই কারণ ও একই নিরাপত্তা।
                        .show().also { com.tkbiswas.pilesclinic.native.NoBengali.installDialog(it); com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) }   // 🤫 V774
                }
            }
        }
    }

    /** Writes only after the same-day safety check (or explicit YES). */
    private fun commitPrescription(openPrintAfter: Boolean, finishAfter: Boolean) {
        // Owner decision (12.08.2026): the values used while saving become
        // that medicine's default everywhere until the owner changes them.
        ClinicalRepository.currentPrescription.forEach { medicine ->
            ClinicalRepository.rememberPermanentDefault(
                applicationContext,
                medicine.name,
                medicine.medicineType,
                medicine.dosage,
                medicine.frequency,
                medicine.duration
            )
        }
        val names = ClinicalRepository.currentPrescription.joinToString(", ") { it.name.ifBlank { "(unnamed)" } }
        ClinicalRepository.addVisit("Prescription", names.take(80), RoleSession.currentRole)
        // TK APPROVED (2026-07-15): this exact medicine set becomes the new
        // "Common Prescription" default for next time (same as Blood Test).
        ClinicalRepository.saveCommonPrescription(ClinicalRepository.currentPrescription.map { it.name }.toSet())
        // ROOT-CAUSE FIX: persist permanently to Supabase "medical" table.
        val details = ClinicalRepository.currentPrescription.joinToString("; ") {
            listOf(it.name.ifBlank { "(unnamed)" }, it.dosage, it.frequency, it.duration).filter { s -> s.isNotBlank() }.joinToString(" · ")
        }
        val createdBy = NativeSession.current(this)?.mobile ?: ""
        val pid = RoleSession.currentPatientId
        val pname = RoleSession.currentPatientName
        val count = ClinicalRepository.currentPrescription.size
        // 🔒 TK-এর নিয়ম (28.07.2026): Save চাপার সঙ্গে সঙ্গে প্রিন্ট পর্দা
        // খুলবে — ক্লাউডে পাঠানো শেষ হওয়ার জন্য স্টাফকে বসিয়ে রাখা যাবে না।
        // প্রিন্টের সব তথ্য ফোনেই আছে, ক্লাউডের কিছু লাগে না। সেভটা আগে
        // ফোনেই লেখা হয়, তারপর পিছনে ক্লাউডে যায়; না গেলে অপেক্ষমাণ
        // তালিকায় জমা থেকে নিজে থেকেই আবার যায়, তাই কিছু হারায় না।
        Toast.makeText(this@PrescriptionActivity, "Prescription saved ($count item/s).", Toast.LENGTH_SHORT).show()
        val appCtx = applicationContext
        com.tkbiswas.pilesclinic.native.BackgroundWork.run {
            ClinicalCloudRepository.saveMedical(appCtx, pid, pname, "Prescription", names, details, createdBy)
        }
        if (openPrintAfter) {
            com.tkbiswas.pilesclinic.print.PrintDataHolder.pendingModel =
                com.tkbiswas.pilesclinic.print.PrintMappers.prescription(applicationContext)
            startActivity(Intent(this@PrescriptionActivity, com.tkbiswas.pilesclinic.print.PrintPreviewActivity::class.java))
        }
        saveInProgress = false
        if (finishAfter) finish()
    }

    /** TK APPROVED (2026-07-15): shared gentle "premium pulse" -- one soft
     *  scale-up-and-back, not a repeating loop, used only on the new Apply
     *  Common buttons. */
    private fun animatePulse(view: android.view.View) {
        val anim = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            view,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.045f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.045f)
        )
        anim.duration = 650
        anim.repeatMode = android.animation.ValueAnimator.REVERSE
        anim.repeatCount = 1
        anim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        anim.start()
    }

    private fun refreshEmptyState() {
        tvEmptyState.visibility =
            if (ClinicalRepository.currentPrescription.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    /** TK APPROVED (2026-07-15): plain text share, same pattern as Medicine
     *  Slip's "Share as Text" — sends via WhatsApp/SMS/any share target.
     *
     *  🔴 TK-নির্দেশ (২৭.০৮.২০২৬): *"share As Text থাকবে না"* — বোতামটা
     *  পর্দা থেকে তুলে নেওয়া হয়েছে, তাই এই ফাংশনটা এখন **আর ডাকা হয় না**।
     *  ⛔ মুছে ফেলা হয়নি — TK কখনো ফেরত চাইলে শুধু একটা লাইন
     *     (`btnShare.setOnClickListener { sharePrescription() }`) বসালেই
     *     আগের মতো কাজ করবে; কোড আবার নতুন করে লিখতে হবে না। */
    @Suppress("unused")
    private fun sharePrescription() {
        val medicines = ClinicalRepository.currentPrescription
        if (medicines.isEmpty()) {
            Toast.makeText(this, "Add at least one medicine first.", Toast.LENGTH_SHORT).show()
            return
        }
        val sb = StringBuilder()
        // 🔒 TK-এর লক করা নিয়ম (খাতার সারি B72, 29.07.2026): কিশনগঞ্জ →
        // TK BISWAS PILES CLINIC, বাকি সব ব্রাঞ্চ → MAA AYURVED PILES CLINIC।
        // আগে এখানে সব ব্রাঞ্চেই জোর করে "TK Biswas Piles Clinic" বসানো ছিল,
        // তাই JPE/COB/FLK/BIR-এর রোগী WhatsApp-এ ভুল নাম পেতেন। নামটা এখন
        // একটাই জায়গা BranchCatalog থেকে আসে — হাতে লেখা হয় না।
        sb.append(com.tkbiswas.pilesclinic.print.BranchCatalog.byName(RoleSession.currentPatientBranch).clinicName).append("\n")
        // 🔒 খাতার সারি B175 — শেয়ার টেক্সটেও মানুষ-পড়া-যায় আইডি।
        sb.append("Patient: ${RoleSession.currentPatientName} (${RoleSession.displayId()})\n\n")
        sb.append("Prescription (Rx)\n")
        medicines.forEachIndexed { index, m ->
            val parts = listOfNotNull(
                m.dosage.takeIf { it.isNotBlank() },
                m.frequency.takeIf { it.isNotBlank() },
                m.duration.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            sb.append("${index + 1}. ${m.name.ifBlank { "(unnamed)" }}\n")
            if (parts.isNotBlank()) sb.append("   $parts\n")
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Prescription - ${RoleSession.currentPatientName}")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(sendIntent, "Share Prescription"))
    }

    /** TK-REQUESTED CHANGE (2026-07-19): Days and Instructions are now set
     *  right inside the Add Medicine dialog itself (showExtraFields = true),
     *  and this picker now ALWAYS opens directly — whether starting a fresh
     *  Prescription or adding more medicines to one already in progress — so
     *  the separate review screen (with its own Dose/Days/Instructions boxes)
     *  is never seen in the normal flow. Tapping Add still directly saves,
     *  prints, and closes the screen. */
    private fun showReferencePicker() {
        MedicinePickerDialog.showPicker(
            activity = this,
            listType = "ayurvedic",
            baseList = ClinicalRepository.commonMedicines,
            title = "Add from Reference List",
            subtitle = "Ayurvedic medicines · Prescription",
            accent = MedicinePickerDialog.GREEN_AYURVEDIC,
            showExtraFields = true,
            commonProvider = { ClinicalRepository.getCommonPrescription() },
            onCancelled = {
                // 🔒🔒 খাতার সারি B177 (TK, 30.07.2026 — একই কথা আগেও একবার বলা
                // হয়েছিল, সারি B38-এ, 28.07.2026)। **আসল কারণ (কোড ধরে,
                // আন্দাজ নয়):** B38-এর সমাধানে শর্ত ছিল "তালিকা ফাঁকা থাকলে
                // তবেই বন্ধ হবে" (`currentPrescription.isEmpty()`) — এই ধারণায়
                // যে খালি তালিকায় দেখানোর কিছু নেই। কিন্তু NOOR ALAM-এর মতো
                // যাঁর প্রেসক্রিপশনে **আগে থেকেই ওষুধ আছে** (এই সেশনেই বা
                // আগের কোনো সেভ থেকে), সেই তালিকা আর ফাঁকা থাকে না — তাই
                // finish() হতই না, আর নিচের (এখন আর-খালি-নয়) রিভিউ পর্দাটা
                // বেরিয়ে পড়ত। **TK-এর স্পষ্ট চূড়ান্ত সিদ্ধান্ত (ছবি-প্রুফে):**
                // "শুধুমাত্র এই ফটোটাই থাকবে" — পিকার থেকে কিছু না করে বেরোলে
                // তালিকা খালি থাক বা ভরা, **সবসময়ই** সরাসরি Take Action-এ
                // ফিরতে হবে। তাই এখন `isEmpty()` শর্তটা তুলে দেওয়া হলো।
                /* 🔴🔒 V743 (২৭.০৮.২০২৬) — **TK-এর নতুন সিদ্ধান্ত, পুরনোটা তুলে দেওয়া।**
                   TK ছবি দিয়ে দেখালেন: ওষুধ বাছার পর্দা → ব্যাক → সোজা Follow-up
                   তালিকায় চলে আসে, Prescription পর্দাটা দেখাই যায় না। প্রশ্ন করলেন
                   *"৩ থেকে ব্যাকে আবার ১ এ কেন আসে"*, আর তিনটে বিকল্প দেখানোর পর
                   স্পষ্ট বললেন: **"২ করুন"** = B177 তুলে দিতে হবে।

                   ⛔ **উপরের B38/B177-এর লেখা মুছিনি ইচ্ছে করেই** — ওটা ইতিহাস,
                      কেন একদিন উল্টো নিয়ম ছিল সেটা যেন পরে বোঝা যায়।
                   ⇒ এখন পিকার থেকে কিছু না নিয়ে বেরোলেও পর্দাটা **বন্ধ হয় না**,
                     Prescription পর্দাই দেখায়; সেখান থেকে ব্যাক = Follow-up।
                   ⚠️ তাই কিছু না নিয়ে বেরোলে **ফাঁকা Prescription পর্দা** দেখা
                      যাবে — ঠিক যেটা B38-এ TK পছন্দ করেননি। TK-কে জানানো হয়েছে। */
            },
            /* 🔵 V488 (20.08.2026, TK-নির্দেশ): নিচের বারের নতুন "WhatsApp" বোতাম।
               Save-এর মতোই সব সেভ হয় (আগে ফোনে, পিছনে ক্লাউডে), শুধু ছাপার পর্দা
               খোলার বদলে — ছাপার **হুবহু একই কাগজ** PDF করে WhatsApp / WhatsApp
               Business-এ পাঠানোর ব্যবস্থা খোলে।
               ⛔ Save-এর নিজের পথ (নিচের ব্লক) এক অক্ষরও বদলায়নি।
               ⛔ পর্দা এখানে finish() করা হয় না — WhatsApp বেছে নেওয়ার বাক্সটা
                  এই পর্দার উপরেই দাঁড়ায়; আগেই বন্ধ করে দিলে বাক্সটাও ভেঙে যেত
                  (ঠিক B359-এ ধরা পড়া সেই একই কারণ)। */
            onWhatsApp = {
                adapter.notifyDataSetChanged()
                refreshEmptyState()
                savePrescription(openPrintAfter = false, finishAfter = false)
                com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.share(
                    this,
                    com.tkbiswas.pilesclinic.print.PrintMappers.prescription(applicationContext)
                )
            }
        ) {
            adapter.notifyDataSetChanged()
            refreshEmptyState()
            // 🔴🔴🔒 V669 (২৫.০৮.২০২৬, TK-কড়া-রিপোর্ট, দ্বিতীয়বার — "এই সমস্যার
            // কথাও তো আগে বলেছিলাম কেন ঠিক করেন নাই") — **স্বীকারোক্তি:**
            // V662-এ শুধু "Outside List" (custom ওষুধ লেখার) পথ ঠিক করেছিলাম,
            // কিন্তু এই "Reference List" (তালিকা থেকে টিক দিয়ে বাছার) পথের
            // নিজের "Save"-এও একই বাগ ছিল — মিস হয়ে গিয়েছিল, TK-এর ছবিতেই
            // ধরা পড়ল (KANKAYAN VATI ARSHA বেছে "Save (1)" চাপতেই সরাসরি
            // Print Preview খুলেছিল)। এখন এখানেও ঠিক করা হলো — আর সরাসরি
            // প্রিন্টে যাবে না, ডাক্তার প্রেসক্রিপশন পাতাতেই ফিরে থাকবেন।
        }
    }

    private fun showCustomMedicineDialog() {
        // 🔴🔴🔒 V662 (২৫.০৮.২০২৬, TK-কড়া-রিপোর্ট) — আগে এখানে একটা ওষুধ
        // Add করলেই সরাসরি `savePrescription(openPrintAfter = true,
        // finishAfter = true)` ডাকা হতো — অর্থাৎ সাথে সাথে সেভ+প্রিন্ট+
        // পর্দা বন্ধ, ডাক্তারকে কোনো সুযোগ না দিয়ে (এটাই TK-এর দুটো
        // অভিযোগেরই আসল কারণ — একটার বেশি ওষুধ লেখা যেত না, আর সরাসরি
        // প্রিন্টে চলে যেত)। এটা ছিল TK-এরই ১৯.০৭.২০২৬-এর আগের নির্দেশ,
        // যেটা এখন TK স্পষ্টভাবে বদলাতে বলেছেন।
        // ⛔ এখন শুধু তালিকা রিফ্রেশ হয় — ডাক্তার Prescription পাতাতেই
        //   ফিরে থাকেন, নিজে থেকে Save/Save & Print বেছে নেবেন (নিচের
        //   btnSave/btnSaveAndPrint — এক অক্ষরও বদলায়নি)।
        MedicinePickerDialog.showOutsideDialog(this, "ayurvedic", MedicinePickerDialog.GREEN_AYURVEDIC) {
            adapter.notifyDataSetChanged()
            refreshEmptyState()
        }
    }

}
