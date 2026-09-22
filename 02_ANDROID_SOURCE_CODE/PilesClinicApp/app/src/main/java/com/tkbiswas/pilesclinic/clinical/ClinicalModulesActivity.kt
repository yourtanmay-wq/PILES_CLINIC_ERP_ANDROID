package com.tkbiswas.pilesclinic.clinical

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/**
 * Phase 4 entry point / hub for the six clinical modules.
 *
 * Standalone for now (see project note): a future native Dashboard / Patient
 * Action flow can launch this Activity directly with:
 *
 *   val intent = Intent(context, ClinicalModulesActivity::class.java)
 *   intent.putExtra(RoleSession.EXTRA_ROLE, "DOCTOR")       // or "STAFF"
 *   intent.putExtra(RoleSession.EXTRA_PATIENT_NAME, patientName)
 *   intent.putExtra(RoleSession.EXTRA_PATIENT_ID, patientId)
 *   startActivity(intent)
 *
 * For manual testing right now (no Dashboard wired yet), launch it directly via adb:
 *   adb shell am start -n com.tkbiswas.pilesclinic/.clinical.ClinicalModulesActivity
 */
class ClinicalModulesActivity : AppCompatActivity() {

    companion object {
        // TK-REQUESTED (2026-07-17): lets a caller (Doctor Queue's Check-up /
        // Summary buttons) skip straight to one module instead of landing on
        // this list. Optional -- if absent, this screen behaves exactly as
        // before (shows the full list).
        const val EXTRA_AUTO_OPEN = "extra_auto_open"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clinical_modules)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        RoleSession.applyFrom(
            intent.getStringExtra(RoleSession.EXTRA_ROLE),
            intent.getStringExtra(RoleSession.EXTRA_PATIENT_NAME),
            intent.getStringExtra(RoleSession.EXTRA_PATIENT_ID),
            intent.getStringExtra(RoleSession.EXTRA_PATIENT_BRANCH),
            // 🔒 খাতার সারি B179 (TK, 30.07.2026): DoctorQueueActivity এখন
            // address/age/sex-ও পাঠায় (নতুন ক্লাউড-কল দিয়ে আনা) — আগে এই
            // তিনটে ঘরই null যেত।
            patientMobile = intent.getStringExtra(RoleSession.EXTRA_PATIENT_MOBILE),
            patientAddress = intent.getStringExtra(RoleSession.EXTRA_PATIENT_ADDRESS),
            patientAge = intent.getStringExtra(RoleSession.EXTRA_PATIENT_AGE),
            patientSex = intent.getStringExtra(RoleSession.EXTRA_PATIENT_SEX),
            patientDisease = intent.getStringExtra(RoleSession.EXTRA_PATIENT_DISEASE),
            // 🔒 খাতার সারি B175: মানুষ-পড়া-যায় Patient ID (থাকলে) আলাদা
            // extra দিয়ে আসছে — ছাপায় এখন সেটাই দেখাবে।
            patientDisplayId = intent.getStringExtra(RoleSession.EXTRA_PATIENT_DISPLAY_ID)
        )

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // 🔒 খাতার সারি B175 — মানুষ-পড়া-যায় Patient ID (`displayId()`)।
        findViewById<TextView>(R.id.tvPatientContext).text =
            "Patient: ${RoleSession.currentPatientName} (${RoleSession.displayId()})  •  Role: ${roleLabel()}"

        findViewById<TextView>(R.id.tvInvestigationSub).text =
            if (RoleSession.isDoctor())
                "Select tests and mark as advised"
            else
                "Staff can request; Doctor approves"

        findViewById<CardView>(R.id.cardCheckup).setOnClickListener {
            openOrExplainDoctorOnly(DoctorCheckupActivity::class.java, "Doctor Check-up")
        }
        findViewById<CardView>(R.id.cardPrescription).setOnClickListener {
            // Staff can view an already-written prescription (read-only) per rules;
            // editing requires Doctor role. The activity itself enforces this.
            startActivity(Intent(this, PrescriptionActivity::class.java))
        }
        findViewById<CardView>(R.id.cardMedicineSlip).setOnClickListener {
            startActivity(Intent(this, MedicineSlipActivity::class.java))
        }
        findViewById<CardView>(R.id.cardInvestigation).setOnClickListener {
            startActivity(Intent(this, InvestigationAdviceActivity::class.java))
        }
        findViewById<CardView>(R.id.cardDietChart).setOnClickListener {
            startActivity(Intent(this, DietChartActivity::class.java))
        }
        findViewById<CardView>(R.id.cardHistory).setOnClickListener {
            startActivity(Intent(this, PatientClinicalHistoryActivity::class.java))
        }

        // Auto-forward shortcut (see EXTRA_AUTO_OPEN above). Runs AFTER
        // RoleSession.applyFrom() above, so the target screen sees this
        // patient's correct data -- reuses the exact same click behavior
        // as the cards themselves (openOrExplainDoctorOnly / startActivity),
        // nothing new invented.
        // 🚨 TK-REPORTED (2026-07-28): CHECK-UP Queue → "Check-up" → Doctor Note,
        // তারপর Back চাপলে যেখান থেকে এসেছেন সেখানে না ফিরে এই Clinical Modules
        // পর্দাটা দেখাত। কারণ: এই পর্দাটা শুধু পথ দেখানোর জন্য খোলা হয়েছিল, কিন্তু
        // পিছনের সারিতে দাঁড়িয়ে থাকত।
        // ঠিক করা হলো: **শুধু এই সরাসরি-যাওয়ার ক্ষেত্রে** (autoOpen) পর্দাটা নিজেকে
        // বন্ধ করে দেয়, তাই Back চাপলে সোজা CHECK-UP Queue-তে ফেরে।
        // ⛔ স্টাফ যখন নিজে এই Clinical Modules পর্দায় আসেন (autoOpen ছাড়া), তখন
        // কিছুই বদলায় না — পর্দাটা আগের মতোই থাকে, সব বোতাম ও ডিজাইন অক্ষত।
        val autoOpen = intent.getStringExtra(EXTRA_AUTO_OPEN)
        when (autoOpen) {
            "CHECKUP" -> openOrExplainDoctorOnly(DoctorCheckupActivity::class.java, "Doctor Check-up")
            "SUMMARY" -> startActivity(Intent(this, PatientClinicalHistoryActivity::class.java))
        }
        if (autoOpen != null && RoleSession.canEditClinical()) finish()
    }

    private fun roleLabel(): String = if (RoleSession.isDoctor()) "Doctor" else "Staff"

    private fun openOrExplainDoctorOnly(target: Class<*>, moduleName: String) {
        if (RoleSession.canEditClinical()) {
            startActivity(Intent(this, target))
        } else {
            Toast.makeText(
                this,
                "$moduleName is Doctor-only. Staff can view the last saved record from Patient History.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
