package com.tkbiswas.pilesclinic.print

import com.tkbiswas.pilesclinic.native.BottomNav

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.clinical.ClinicalRepository
import com.tkbiswas.pilesclinic.clinical.RoleSession
import com.tkbiswas.pilesclinic.native.SupabaseClient
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.s
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume   // 🔵 V530
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/**
 * Phase 6 entry point / hub for the 5 print types. Standalone for now, same
 * reasoning as ClinicalModulesActivity (Phase 4) and SyncStatusActivity
 * (Phase 5) — no native Dashboard exists yet to host it from. Launch for
 * testing:
 *   adb shell am start -n com.tkbiswas.pilesclinic/.print.PrintCenterActivity
 */
class PrintCenterActivity : AppCompatActivity() {

    // In-memory clinical working copy (populated when a Doctor opens a patient's
    // Checkup/Prescription/Diet from the Doctor Queue). The Rx/Slip/Diet prints
    // read from this, so they are only valid while a patient session is active.
    private val clinicalRepo = ClinicalRepository

    /* 🔴🔒 V786 (২৮.০৮.২০২৬, TK-রিপোর্ট: হেডারে "Patient / - / -") —
       ফোনে কল এলে বা মেমরি কম পড়লে Android অ্যাপের প্রসেস বন্ধ করে দেয়;
       পরে এই পর্দাটা আবার খোলে, কিন্তু মেমরির `RoleSession` ততক্ষণে ফাঁকা।
       তাই রোগীর পরিচয় এই পর্দার নিজের Bundle-এও রাখা হয় — Bundle প্রসেস
       মরলেও বাঁচে, আর V721-এর ৩০ মিনিটের সীমাও এতে লাগে না।
       ⛔ মেমরিতে রোগী থাকলে `restoreFrom()` কিচ্ছু করে না (RoleSession.kt)। */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        com.tkbiswas.pilesclinic.clinical.RoleSession.saveTo(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.tkbiswas.pilesclinic.clinical.RoleSession.restoreFrom(savedInstanceState)   // 🔴🔒 V786 — কল/মেমরির কারণে হারানো রোগী ফেরানো
        setContentView(R.layout.activity_print_center)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        // TK-REPORTED BUG FIX (2026-07-20): the Walk-in / Direct-Print medicine
        // picker (MedicinePickerDialog, opened below) reads/writes remembered
        // Type and Dose via ClinicalRepository's dosePrefs -- but that store is
        // only initialized when PrescriptionActivity/MedicineSlipActivity's own
        // onCreate runs (attachDoseMemory). This screen never called it, so a
        // Walk-in entry's Type/Dose choice silently failed to save (and any
        // earlier-remembered Type/Dose never loaded) whenever Print Center was
        // opened without first visiting Prescription/Medicine Slip in the same
        // app session. Same one-line fix already used in those two screens.
        com.tkbiswas.pilesclinic.clinical.ClinicalRepository.attachDoseMemory(this)

        // 🔴🔴🔴 খাতার সারি B439 (TK-রিপোর্ট, ছবিসহ — জলপাইগুড়ির স্টাফের
        // Print Center-এ "Kishanganj" দেখাচ্ছিল) — গভীরে যাচাই করে আসল
        // কারণ ধরা পড়েছে: `BranchSession.current` (BranchInfo.kt) একটা
        // গ্লোবাল, সারা অ্যাপ-জীবনভর-থাকা ভ্যারিয়েবল, যেটা **সবসময় Kishanganj
        // দিয়ে শুরু হয়** আর **কখনো** লগইন-করা স্টাফের নিজের ব্রাঞ্চ দিয়ে
        // বসানো হতো না — শুধু Doctor নিজে হাতে বদলালেই বদলাত। তাই
        // Kishanganj ছাড়া বাকি ৪টা ব্রাঞ্চের **প্রতিটা স্টাফই** এই পর্দায়
        // ভুল ব্রাঞ্চ (Kishanganj) দেখতেন — এবং সবচেয়ে বড় ঝুঁকি: Prescription/
        // Medicine Slip/Blood Test/Diet Chart/Registration/Payment Receipt/
        // Doctor Checkup — এই সবকটা প্রিন্টে **ভুল ক্লিনিকের নাম-ঠিকানা-ফোন**
        // (Kishanganj-এর) ছাপা হয়ে রোগীর হাতে চলে যাওয়ার ঝুঁকি ছিল।
        // **সমাধান:** স্টাফ (Doctor/Master নয়) হলে, পর্দা খোলার সাথে সাথেই
        // `BranchSession.current` তাঁর নিজের লগইন-ব্রাঞ্চে বসানো হয় — প্রতিবার,
        // তাই ভুল ব্রাঞ্চ থেকে গেলেও (আগের সেশনে অন্য কেউ বদলে থাকলেও) নিজে
        // থেকেই ঠিক হয়ে যায়। ⛔ Doctor/Master-এর নিজের হাতে-বাছা ব্রাঞ্চ
        // ছোঁয়া হয়নি — তাঁদের জন্য এই জোর করে বসানো চলে না।
        try {
            val u = NativeSession.current(this)
            // ⛔ NativeSession.current()-এর `role` ফিল্ড doctor/field-কেও
            // "staff"-এ মিলিয়ে দেয় (permission-চেকের জন্য, NativeSession.kt-
            // এর নিজস্ব নিয়ম) — তাই এখানে আসল `displayRole` দেখা হচ্ছে, যাতে
            // Doctor-এর নিজের হাতে-বাছা ব্রাঞ্চ ভুল করে ওভাররাইড না হয়।
            // 🔴🔴 B613 (10.08.2026, TK-রিপোর্ট, ছবিসহ — জলপাইগুড়ির **Doctor**
            // (Dr. Jay Banik) Print Center-এ Kishanganj দেখছিলেন): B439-এর জোর-করে-
            // ব্রাঞ্চ শুধু staff/field-এ বসত, **Doctor বাদ পড়েছিল** — অথচ Doctor-ও
            // নিজের ব্রাঞ্চেই সীমাবদ্ধ (ব্রাঞ্চ বদলাতে পারেন শুধু Master, canSwitchBranch)।
            // তাই Doctor ডিফল্ট Kishanganj-এ আটকে থাকতেন ও ভুল ক্লিনিকের নাম-ঠিকানা
            // প্রিন্টে যাওয়ার ঝুঁকি ছিল। **সমাধান:** Master ছাড়া **সবাইকে** (staff/
            // field/doctor + যেকোনো non-master) খোলার সাথে সাথে নিজের লগইন-ব্রাঞ্চে
            // বসানো হয়। ⛔ Master-এর নিজের হাতে-বাছা ব্রাঞ্চ আগের মতোই ছোঁয়া হয় না।
            if (u != null && u.displayRole != "master") {
                val ownBranch = com.tkbiswas.pilesclinic.print.BranchCatalog.byName(u.branch)
                com.tkbiswas.pilesclinic.print.BranchSession.current = ownBranch
            }
        } catch (_: Throwable) { }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        setupBranchToggle()

        findViewById<CardView>(R.id.cardRegistration).setOnClickListener { openLatestRegistration() }
        findViewById<CardView>(R.id.cardPrescription).setOnClickListener {
            if (hasActiveClinicalPatient() && clinicalRepo.currentPrescription.isNotEmpty()) {
                PrintDataHolder.pendingModel = PrintMappers.prescription(applicationContext)
                openPreview()
            } else {
                showClinicalPrintFromMobile("Prescription")
            }
        }
        findViewById<CardView>(R.id.cardMedicineSlip).setOnClickListener {
            if (hasActiveClinicalPatient() && clinicalRepo.currentSlip.isNotEmpty()) {
                PrintDataHolder.pendingModel = PrintMappers.medicineSlip(this)
                openPreview()
            } else {
                showClinicalPrintFromMobile("Medicine Slip")
            }
        }
        findViewById<CardView>(R.id.cardDietChart).setOnClickListener {
            if (hasActiveClinicalPatient() && clinicalRepo.currentDiet.any { it.isSelected }) {
                PrintDataHolder.pendingModel = PrintMappers.dietChart()
                openPreview()
            } else {
                showClinicalPrintFromMobile("Diet Chart")
            }
        }
        findViewById<CardView>(R.id.cardPaymentReceipt).setOnClickListener {
            showPaymentReceiptPicker()
        }
        findViewById<CardView>(R.id.cardDoctorVisitPrint).setOnClickListener {
            showDoctorVisitPrintPicker()
        }
        findViewById<CardView>(R.id.cardBloodTest).setOnClickListener {
            showBloodTestPicker()
        }
    }

    /** True only when a real patient is loaded into the clinical session (i.e. a
     *  Doctor opened this patient from the Doctor Queue). Used as the fast path so
     *  a doctor can print the Rx/Diet they just wrote without re-entering the
     *  mobile; otherwise Print Center falls back to a mobile lookup. */
    private fun hasActiveClinicalPatient(): Boolean =
        RoleSession.currentPatientId.isNotBlank() && RoleSession.currentPatientName.isNotBlank()

    /** Web-parity: print any patient's latest saved Prescription / Medicine Slip
     *  / Diet Chart by looking them up on the Supabase "medical" table by mobile
     *  (same pattern as the Payment / Blood Test cards). */
    private fun showClinicalPrintFromMobile(docType: String) {
        val accent = when (docType) {
            "Prescription" -> android.graphics.Color.parseColor("#1557D6")
            "Medicine Slip" -> android.graphics.Color.parseColor("#15913A")
            "Diet Chart" -> android.graphics.Color.parseColor("#EF6C00")
            else -> android.graphics.Color.parseColor("#673AB7")
        }
        showProfessionalMobileDialog(
            title = "$docType Print",
            message = "Enter the patient's mobile number or name to print their latest saved $docType.",
            accentColor = accent,
            primaryLabel = "PRINT",
            allowNameSearch = true,
            onPrimary = { query -> resolveMobileOrName(query) { digits -> fetchAndPrintClinical(digits, docType) } },
            onDirect = if (docType == "Prescription" || docType == "Medicine Slip" || docType == "Diet Chart") {
                { showDirectPrintForm(docType) }
            } else null
        )
    }

    /**
     * Owner-approved Print Center dialog. Only the visual shell is changed:
     * mobile validation and every existing print/direct-print workflow remain intact.
     */
    private fun showProfessionalMobileDialog(
        title: String,
        message: String,
        accentColor: Int,
        primaryLabel: String,
        onPrimary: (String) -> Unit,
        onDirect: (() -> Unit)? = null,
        // TK-REQUESTED ADDITION (2026-07-19): Payment Receipt search now also
        // accepts the patient's NAME, not only their mobile number. Kept as
        // an opt-in flag (default false) so every OTHER Print Center dialog
        // that already uses this shared shell (Prescription, Medicine Slip,
        // Diet Chart, Registration Slip, Doctor Checkup, Blood Test) keeps
        // behaving exactly as before -- nothing about them changes.
        allowNameSearch: Boolean = false
    ) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), accentColor)
            }
        }
        val header = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val icon = android.widget.TextView(this).apply {
            text = when {
                title.startsWith("Prescription") -> "℞"
                title.startsWith("Medicine") -> "💊"
                title.startsWith("Diet") -> "🥗"
                title.startsWith("Blood") -> "🧪"
                else -> "🖨"
            }
            textSize = 30f
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.argb(22, android.graphics.Color.red(accentColor), android.graphics.Color.green(accentColor), android.graphics.Color.blue(accentColor)))
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setStroke(dp(1), accentColor)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(58), dp(58)).also { it.marginEnd = dp(12) }
        }
        val titleView = android.widget.TextView(this).apply {
            text = title
            textSize = 21f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(accentColor)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = android.widget.TextView(this).apply {
            text = "×"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#17304F"))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setStroke(dp(1), accentColor)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(40), dp(40))
        }
        val scrollContent = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        header.addView(icon); header.addView(titleView); header.addView(close)
        scrollContent.addView(header)
        scrollContent.addView(android.widget.TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#334155"))
            setPadding(dp(70), dp(2), dp(4), dp(14))
        })
        val input = EditText(this).apply {
            hint = if (allowNameSearch) "Patient mobile or name" else "Patient mobile (10 digits)"
            if (allowNameSearch) {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            } else {
                inputType = android.text.InputType.TYPE_CLASS_PHONE
                com.tkbiswas.pilesclinic.native.MobileInput.attach(this)
            }
            setBackgroundResource(R.drawable.bg_input_field)
            textSize = 16f
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        scrollContent.addView(input, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(58)
        ).also { it.bottomMargin = dp(14) })
        // TK-REPORTED BUG FIX (2026-07-20): same class of bug as section 88/89
        // (see 00_PROJECT_STATE_MASTER_NOTE.md) -- Cancel/Print buttons below
        // are custom, baked into `root`, not AlertDialog's own button bar, so
        // this needs an actual height cap to ever scroll (see
        // MaxHeightScrollView.kt). Normal-length content still looks and
        // behaves exactly as before.
        root.addView(
            com.tkbiswas.pilesclinic.native.MaxHeightScrollView(this).apply {
                addView(scrollContent)
                maxHeightPx = dp(360)
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        )

        val actionRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        val primary = MaterialButton(this).apply {
            text = primaryLabel
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            this.icon = androidx.core.content.ContextCompat.getDrawable(this@PrintCenterActivity, R.drawable.ic_action_print)
            iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            cornerRadius = dp(12)
        }
        val cancel = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "CANCEL"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#17304F"))
            strokeColor = android.content.res.ColorStateList.valueOf(accentColor)
            cornerRadius = dp(12)
        }
        actionRow.addView(primary, android.widget.LinearLayout.LayoutParams(0, dp(54), 1f).also { it.marginEnd = dp(6) })
        actionRow.addView(cancel, android.widget.LinearLayout.LayoutParams(0, dp(54), 1f).also { it.marginStart = dp(6) })
        root.addView(actionRow)
        val direct = onDirect?.let {
            MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "DIRECT PRINT (WALK-IN)"
                textSize = 13f
                setTextColor(accentColor)
                strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.argb(90, android.graphics.Color.red(accentColor), android.graphics.Color.green(accentColor), android.graphics.Color.blue(accentColor)))
                backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.argb(12, android.graphics.Color.red(accentColor), android.graphics.Color.green(accentColor), android.graphics.Color.blue(accentColor)))
                cornerRadius = dp(12)
            }.also { button ->
                root.addView(button, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
                ).also { lp -> lp.topMargin = dp(10) })
            }
        }
        root.addView(android.widget.TextView(this).apply {
            text = "✓  Patient data remains protected"
            textSize = 11f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#52647A"))
            setPadding(0, dp(12), 0, 0)
        })

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this).setView(root).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            dialog.window?.setDimAmount(0.72f)
            close.setOnClickListener { dialog.dismiss() }
            cancel.setOnClickListener { dialog.dismiss() }
            primary.setOnClickListener {
                if (allowNameSearch) {
                    val text = input.text.toString().trim()
                    val digits = text.filter { ch -> ch.isDigit() }
                    if (digits.length == 10) {
                        dialog.dismiss(); onPrimary(digits)
                    } else if (text.length >= 2) {
                        dialog.dismiss(); onPrimary(text)
                    } else {
                        input.error = "Enter a name or a valid 10-digit mobile"
                    }
                } else {
                    val digits = input.text.toString().filter { ch -> ch.isDigit() }.takeLast(10)
                    if (digits.length != 10) {
                        input.error = "Enter a valid 10-digit mobile"
                    } else {
                        dialog.dismiss()
                        onPrimary(digits)
                    }
                }
            }
            direct?.setOnClickListener {
                dialog.dismiss()
                onDirect?.invoke()
            }
        }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    /** TK APPROVED (2026-07-15): patient snapshot collected on every Direct /
     *  Walk-in form -- matches exactly what the printed slip's "Patient
     *  Details" block shows (Name, Age/Sex, Disease, Address), so the print
     *  is never blank in those fields. Branch stays a Spinner as before. */
    private data class WalkInFields(
        val name: EditText, val mobile: EditText, val age: EditText,
        val sex: EditText, val disease: EditText, val address: EditText,
        val branchSpinner: android.widget.Spinner
    ) {
        // TK-REPORTED FIX (2026-07-21): auto-match found the right patient and
        // filled name/age/sex/disease/address correctly, but never carried the
        // matched patient's real Patient ID through to the printed document --
        // it was always hardcoded blank. This holds that matched ID (if any)
        // so the print model can use it instead of "".
        var matchedPatientId: String = ""
    }

    /** TK APPROVED (2026-07-15): a checked/selected chip (medicine, diet item,
     *  investigation) must be clearly, vividly different from an unchecked one
     *  -- solid colour fill + white text when selected, white/outline when
     *  not. Applied everywhere a selectable Chip is used (Prescription,
     *  Medicine Slip, Diet Chart, Blood Test -- both the walk-in forms and the
     *  found-patient picker), so the whole app behaves the same way. */
    private fun styleSelectableChip(chip: com.google.android.material.chip.Chip, colorHex: String) {
        val color = android.graphics.Color.parseColor(colorHex)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        chip.chipBackgroundColor = android.content.res.ColorStateList(states, intArrayOf(color, android.graphics.Color.WHITE))
        chip.setTextColor(android.content.res.ColorStateList(states, intArrayOf(android.graphics.Color.WHITE, color)))
        chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(color)
        chip.chipStrokeWidth = 1.4f * resources.displayMetrics.density
        chip.isChipIconVisible = false
    }


    /** Shared Name / Mobile / Age / Sex / Disease / Address / Branch header used
     *  by every "Direct / Walk-in" print form -- one place so all of them look
     *  and behave the same. TK APPROVED (2026-07-15): entering a 10-digit
     *  mobile that matches an existing patient now auto-fills every field
     *  below it (name/age/sex/disease/address/branch) so staff don't have to
     *  retype what's already on file; a new/unmatched mobile just leaves the
     *  fields open for manual entry -- nothing is forced or blocked. */
    private fun addWalkInHeader(
        container: android.widget.LinearLayout, prefillMobile: String
    ): WalkInFields {
        val d = resources.displayMetrics.density
        fun styledInput(hintText: String) = EditText(this).apply {
            hint = hintText
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = (14 * d).toInt()
            setPadding(p, p, p, p)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (10 * d).toInt() }
        }
        val nameInput = styledInput("Patient Name / Walk-in")
        container.addView(nameInput)
        val mobileInput = styledInput("Mobile (10 digits — auto-fills if patient found)").apply {
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            com.tkbiswas.pilesclinic.native.MobileInput.attach(this)
            if (prefillMobile.length == 10) setText(prefillMobile)
        }
        container.addView(mobileInput)

        val matchBadge = android.widget.TextView(this).apply {
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            visibility = android.view.View.GONE
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_chip_green)
            val p = (8 * d).toInt(); setPadding(p, p, p, p)
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ); lp.bottomMargin = (10 * d).toInt(); layoutParams = lp
        }
        container.addView(matchBadge)

        val ageSexRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ); lp.bottomMargin = (10 * d).toInt(); layoutParams = lp
        }
        val ageInput = styledInput("Age").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT; keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.marginEnd = (5 * d).toInt() }
        }
        val sexInput = styledInput("Sex (M/F)").apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = (5 * d).toInt() }
        }
        ageSexRow.addView(ageInput); ageSexRow.addView(sexInput)
        container.addView(ageSexRow)

        val diseaseInput = styledInput("Disease")
        container.addView(diseaseInput)
        val addressInput = styledInput("Address")
        container.addView(addressInput)

        val branchLabel = android.widget.TextView(this).apply {
            text = "Branch"; textSize = 13f
            setPadding(0, (8 * d).toInt(), 0, (6 * d).toInt())
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
        }
        container.addView(branchLabel)
        val branchNames = com.tkbiswas.pilesclinic.print.BranchCatalog.all.map { it.displayName }
        val branchSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                this@PrintCenterActivity, android.R.layout.simple_spinner_dropdown_item, branchNames
            )
            setSelection(branchNames.indexOf(BranchSession.current.displayName).coerceAtLeast(0))
        }
        container.addView(branchSpinner)

        // TK-REPORTED FIX (2026-07-21): fields is built here (before
        // tryAutoMatch) so the closure below can set fields.matchedPatientId
        // when a real patient is found -- carrying their actual Patient ID
        // through to the printed document instead of always leaving it blank.
        val fields = WalkInFields(nameInput, mobileInput, ageInput, sexInput, diseaseInput, addressInput, branchSpinner)

        // TK APPROVED (2026-07-15): auto-match by mobile -- fires whenever a
        // full 10-digit number is present (typed OR prefilled); a no-match
        // simply hides the badge and leaves every field exactly as the staff
        // typed it (never wipes manual entry, never blocks the form).
        fun tryAutoMatch(digits: String) {
            if (digits.length != 10) { matchBadge.visibility = android.view.View.GONE; fields.matchedPatientId = ""; return }
            lifecycleScope.launch {
                val patient = withContext(Dispatchers.IO) {
                    val rows = myBranchOnly(SupabaseClient.findByMobile(
                        "patients", "+91$digits", "name,age,sex,disease,address,branch,patientId", 20
                    ))
                    pickPatientForPrint(rows, digits)   // 🔵 V530
                }
                if (patient == null) { matchBadge.visibility = android.view.View.GONE; fields.matchedPatientId = ""; return@launch }
                // 🆕 (03.08.2026, TK-অনুমোদনে) — গভীর অডিটে ধরা পড়া ঝুঁকি: org.json-এর
                // optString(key, fallback)-ও কলাম সত্যিই NULL হলে fallback না দিয়ে
                // "null" শব্দ ফেরত দেয় (B286/B327-এ একই বাগ অন্য ফাইলে ধরা পড়েছিল)।
                // এখানে সেই একই প্রমাণিত নিরাপদ `.s()` হেল্পার (JsonExt.kt) ব্যবহার
                // করা হলো — পুরনো রোগীর (যাদের age/sex/address কখনো ভরা হয়নি) নাম/
                // বয়স/ঠিকানা প্রিন্ট-ফর্মে ভুল করে "null" না বসে যায়।
                if (nameInput.text.isNullOrBlank()) nameInput.setText(patient.s("name"))
                if (ageInput.text.isNullOrBlank()) ageInput.setText(patient.s("age"))
                if (sexInput.text.isNullOrBlank()) sexInput.setText(patient.s("sex"))
                if (diseaseInput.text.isNullOrBlank()) diseaseInput.setText(patient.s("disease"))
                if (addressInput.text.isNullOrBlank()) addressInput.setText(patient.s("address"))
                val branch = patient.s("branch")
                val idx = branchNames.indexOf(branch)
                if (idx >= 0) branchSpinner.setSelection(idx)
                fields.matchedPatientId = patient.s("patientId")
                matchBadge.text = "✓ Patient found — details auto-filled below"
                matchBadge.visibility = android.view.View.VISIBLE
            }
        }
        mobileInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                tryAutoMatch(s?.toString()?.filter { it.isDigit() }?.takeLast(10) ?: "")
            }
        })
        // Prefilled mobile (e.g. from a Print Center mobile search) is set via
        // setText() above, BEFORE this watcher existed -- so it needs its own
        // explicit check here, otherwise a valid 10-digit prefill would
        // silently skip auto-match.
        if (prefillMobile.length == 10) tryAutoMatch(prefillMobile)

        return fields
    }

    private fun sectionLabel(text: String): android.widget.TextView {
        val d = resources.displayMetrics.density
        return android.widget.TextView(this).apply {
            this.text = text
            textSize = 13f
            setPadding(0, (8 * d).toInt(), 0, (6 * d).toInt())
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
        }
    }

    /** TK APPROVED (2026-07-15): shared premium shell for every "Direct /
     *  Walk-in" print form (Prescription / Medicine Slip / Diet Chart / Blood
     *  Test) and the "Select Investigations" picker -- header + scrollable
     *  body + Cancel/primary buttons, same visual language as the mobile-entry
     *  dialogs. `onPrimary` returns true to close the dialog, false to keep it
     *  open (so a validation Toast like "select at least one medicine" still
     *  works, same as the old setPositiveButton { ...; return@setPositiveButton }
     *  pattern -- no logic changed, only the shell). */
    private fun showPremiumFormDialog(
        title: String,
        icon: String,
        accentColor: Int,
        bodyView: android.view.View,
        primaryLabel: String,
        onPrimary: () -> Boolean
    ) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), accentColor)
            }
        }
        val header = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(12))
        }
        header.addView(android.widget.TextView(this).apply {
            text = icon
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.argb(22, android.graphics.Color.red(accentColor), android.graphics.Color.green(accentColor), android.graphics.Color.blue(accentColor)))
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setStroke(dp(1), accentColor)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(46), dp(46)).also { it.marginEnd = dp(12) }
        })
        header.addView(android.widget.TextView(this).apply {
            text = title
            textSize = 16.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(accentColor)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        root.addView(header)

        // Single bounded scroll region for the whole body -- callers pass the
        // raw content container (not pre-wrapped in their own ScrollView), so
        // there is never a nested-ScrollView touch conflict.
        val bodyWrap = android.widget.ScrollView(this).apply {
            addView(bodyView)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(420)
            )
        }
        root.addView(bodyWrap)

        val actionRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(10), dp(16), dp(16))
        }
        val cancel = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "CANCEL"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#17304F"))
            strokeColor = android.content.res.ColorStateList.valueOf(accentColor)
            cornerRadius = dp(12)
        }
        val primary = MaterialButton(this).apply {
            text = primaryLabel
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            cornerRadius = dp(12)
        }
        actionRow.addView(cancel, android.widget.LinearLayout.LayoutParams(0, dp(50), 1f).also { it.marginEnd = dp(6) })
        actionRow.addView(primary, android.widget.LinearLayout.LayoutParams(0, dp(50), 1f).also { it.marginStart = dp(6) })
        root.addView(actionRow)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this).setView(root).setCancelable(true).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            dialog.window?.setDimAmount(0.72f)
            cancel.setOnClickListener { dialog.dismiss() }
            primary.setOnClickListener { if (onPrimary()) dialog.dismiss() }
        }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    /** No patient found (or nothing saved yet) for this mobile -- instead of a
     *  dead-end Toast, offer to continue as a Direct / Walk-in print so the
     *  work never has to stop (TK rule). Same premium shell as the mobile-entry
     *  dialogs (TK APPROVED 2026-07-15) -- logic/behaviour unchanged. */
    /** NOTE (2026-07-15): no longer called anywhere -- TK approved removing the
     *  confirmation-popup step entirely (see fetchAndPrintClinical /
     *  showBloodTestPicker, which now go straight to the walk-in form).
     *  Left in place rather than deleted to keep this change minimal; safe to
     *  remove later if truly unused. */
    private fun offerWalkInFallback(docType: String, mobileDigits: String, reason: String) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val accent = android.graphics.Color.parseColor("#C2410C")
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), accent)
            }
        }
        val header = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        header.addView(android.widget.TextView(this).apply {
            text = "🔍"
            textSize = 26f
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.argb(22, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent)))
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setStroke(dp(1), accent)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(50), dp(50)).also { it.marginEnd = dp(12) }
        })
        header.addView(android.widget.TextView(this).apply {
            text = docType
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(accent)
        })
        root.addView(header)
        val scrollContent2 = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        scrollContent2.addView(android.widget.TextView(this).apply {
            text = reason
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#334155"))
            setPadding(0, dp(12), 0, dp(4))
        })
        scrollContent2.addView(android.widget.TextView(this).apply {
            text = "Print as Direct / Walk-in instead? Nothing will be saved — print only."
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(2), 0, dp(16))
        })
        // TK-REPORTED BUG FIX (2026-07-20): same class of bug as section
        // 88/89 -- Direct Print/Cancel buttons below are custom, baked into
        // `root`, not AlertDialog's own button bar (see MaxHeightScrollView.kt).
        // Normal-length content still looks and behaves exactly as before.
        root.addView(
            com.tkbiswas.pilesclinic.native.MaxHeightScrollView(this).apply {
                addView(scrollContent2)
                maxHeightPx = dp(300)
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        )

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this).setView(root).create()

        val primary = MaterialButton(this).apply {
            text = "DIRECT PRINT (WALK-IN)"
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
            cornerRadius = dp(12)
        }
        val cancel = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "CANCEL"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#17304F"))
            strokeColor = android.content.res.ColorStateList.valueOf(accent)
            cornerRadius = dp(12)
        }
        val actionRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        actionRow.addView(primary, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
        ))
        actionRow.addView(cancel, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
        ).also { it.topMargin = dp(8) })
        root.addView(actionRow)

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            dialog.window?.setDimAmount(0.72f)
            cancel.setOnClickListener { dialog.dismiss() }
            primary.setOnClickListener {
                dialog.dismiss()
                if (docType == "Blood Test / Investigation") showDirectBloodTestForm(mobileDigits)
                else showDirectPrintForm(docType, mobileDigits)
            }
        }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    /**
     * Web-parity openDirectPrintForm()/printDirectRx()/printDirectDiet(): print
     * a Prescription / Medicine Slip / Diet Chart for a walk-in patient with no
     * Registration at all -- name + mobile (optional) typed directly, nothing
     * is saved to the patients/medical tables, print only.
     */
    private fun showDirectPrintForm(docType: String, prefillMobile: String = "") {
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val wf = addWalkInHeader(container, prefillMobile)
        val nameInput = wf.name; val mobileInput = wf.mobile; val branchSpinner = wf.branchSpinner
        val branchNames = com.tkbiswas.pilesclinic.print.BranchCatalog.all.map { it.displayName }

        // ---- Diet Chart: Allowed / Avoid checklist (web parity DIET_ITEMS) ----
        if (docType == "Diet Chart") {
            // TK APPROVED (2026-07-15): each item now shows English/বাংলা/हिन्दी
            // on separate lines, so switched from Material Chip (single-line
            // only, would truncate/clip this much text) to a custom pill row
            // that grows to fit -- same colours/checkmark look as before, just
            // safe for multi-line text. Print/save logic (name string) unchanged.
            fun buildDietRow(name: String, accentHex: String): Pair<android.widget.LinearLayout, BooleanArray> {
                val d = resources.displayMetrics.density
                fun dp(v: Int) = (v * d).toInt()
                val checked = booleanArrayOf(true)
                val row = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(android.graphics.Color.parseColor(accentHex))
                    }
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = dp(8); layoutParams = lp
                    isClickable = true; isFocusable = true
                }
                val circle = android.widget.TextView(this).apply {
                    text = "✓"; textSize = 15f; gravity = android.view.Gravity.CENTER
                    setTextColor(android.graphics.Color.parseColor(accentHex))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(android.graphics.Color.WHITE)
                    }
                    layoutParams = android.widget.LinearLayout.LayoutParams(dp(28), dp(28)).also { it.marginEnd = dp(10) }
                }
                val label = android.widget.TextView(this).apply {
                    text = name; textSize = 13f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setLineSpacing(dp(2).toFloat(), 1f)
                }
                row.addView(circle); row.addView(label)
                row.setOnClickListener {
                    checked[0] = !checked[0]
                    row.alpha = if (checked[0]) 1f else 0.4f
                }
                return row to checked
            }

            container.addView(sectionLabel("Recommended (Allowed)"))
            val allowedChips = mutableListOf<Pair<BooleanArray, String>>()
            com.tkbiswas.pilesclinic.clinical.ClinicalRepository.dietAllowed.forEach { name ->
                val (row, checked) = buildDietRow(name, "#16A36D")
                allowedChips.add(checked to name)
                container.addView(row)
            }

            container.addView(sectionLabel("Avoid"))
            val avoidChips = mutableListOf<Pair<BooleanArray, String>>()
            com.tkbiswas.pilesclinic.clinical.ClinicalRepository.dietAvoid.forEach { name ->
                val (row, checked) = buildDietRow(name, "#D64545")
                avoidChips.add(checked to name)
                container.addView(row)
            }

            container.addView(sectionLabel("Extra Advice"))
            val remarksInput = EditText(this).apply {
                hint = "Extra advice (optional)"
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
                val p = (14 * d).toInt(); setPadding(p, p, p, p)
            }
            container.addView(remarksInput)

            showPremiumFormDialog(
                title = "Direct Diet Chart (Walk-in)",
                icon = "🥗",
                accentColor = android.graphics.Color.parseColor("#EF6C00"),
                bodyView = container,
                primaryLabel = "PREVIEW & PRINT"
            ) {
                val name = nameInput.text.toString().trim().ifBlank { "WALK-IN" }
                val mobileDigits = mobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
                val branch = branchNames.getOrElse(branchSpinner.selectedItemPosition) { BranchSession.current.displayName }
                val allowed = allowedChips.filter { it.first[0] }.map { "✓ ${it.second}" }
                val avoid = avoidChips.filter { it.first[0] }.map { "✗ ${it.second}" }
                if (allowed.isEmpty() && avoid.isEmpty()) {
                    Toast.makeText(this, "Select at least one diet guideline", Toast.LENGTH_SHORT).show()
                    return@showPremiumFormDialog false
                }
                val sections = mutableListOf<PrintSection>()
                if (allowed.isNotEmpty()) sections.add(PrintSection("Allowed", allowed))
                if (avoid.isNotEmpty()) sections.add(PrintSection("Avoid", avoid))
                val remarks = remarksInput.text.toString().trim()
                if (remarks.isNotBlank()) sections.add(PrintSection("Extra Advice", listOf(remarks)))
                val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
                val ageSex = listOf(wf.age.text.toString().trim(), wf.sex.text.toString().trim()).filter { it.isNotBlank() }.joinToString(" / ")
                PrintDataHolder.pendingModel = PrintDocumentModel(
                    documentTitle = "Diet Chart",
                    branchName = branch,
                    patientName = name,
                    patientId = wf.matchedPatientId,
                    dateLabel = today,
                    sections = sections,
                    qrPayload = null,
                    footerNote = "Walk-in print — no registration on file." +
                        (if (mobileDigits.length == 10) " Mobile: $mobileDigits" else ""),
                    patientAddress = wf.address.text.toString().trim(),
                    patientAgeSex = ageSex,
                    patientDisease = wf.disease.text.toString().trim(),
                    patientMobile = mobileDigits
                )
                openPreview()
                true
            }
            return
        }

        // TK FIX (2026-07-15): now uses the EXACT SAME "Add Medicine" screen as
        // the Patient card's Prescription/Medicine Slip (MedicinePickerDialog --
        // same search box, same "Add as new medicine", same Type chip, same
        // premium look). Uses its own private list (walkInMeds) instead of the
        // real patient session lists, so it can never mix into a doctor's
        // in-progress prescription elsewhere in the app.
        container.addView(sectionLabel("Medicine — tick, dose auto-fills (editable)"))
        val listType = if (docType == "Prescription") "ayurvedic" else "allopathic"
        val isPrescription = docType == "Prescription"
        val walkInMeds = mutableListOf<com.tkbiswas.pilesclinic.clinical.MedicineEntry>()

        val addedSummary = android.widget.TextView(this).apply {
            text = "No medicine added yet"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, (4 * d).toInt(), 0, (10 * d).toInt())
        }
        fun refreshSummary() {
            addedSummary.text = if (walkInMeds.isEmpty()) "No medicine added yet"
                else walkInMeds.joinToString("\n") { "• ${it.name}${if (it.medicineType.isNotBlank()) " (${it.medicineType})" else ""} — ${it.dosage.ifBlank { "As advised" }}" }
        }
        val addBtn = android.widget.TextView(this).apply {
            text = "＋  Add Medicine"
            textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = (10 * d)
                setColor(android.graphics.Color.parseColor(if (isPrescription) "#16A36D" else "#1067D8"))
            }
            gravity = android.view.Gravity.CENTER
            val pv = (12 * d).toInt(); setPadding(pv, pv, pv, pv)
            isClickable = true; isFocusable = true
        }
        addBtn.setOnClickListener {
            com.tkbiswas.pilesclinic.clinical.MedicinePickerDialog.showPicker(
                activity = this,
                listType = listType,
                baseList = if (isPrescription) com.tkbiswas.pilesclinic.clinical.ClinicalRepository.commonMedicines
                    else com.tkbiswas.pilesclinic.clinical.ClinicalRepository.slipMedicines,
                title = "Add Medicine",
                subtitle = if (isPrescription) "Ayurvedic medicines · Walk-in Prescription" else "Allopathic medicines · Walk-in Slip",
                accent = if (isPrescription) com.tkbiswas.pilesclinic.clinical.MedicinePickerDialog.GREEN_AYURVEDIC
                    else com.tkbiswas.pilesclinic.clinical.MedicinePickerDialog.BLUE_ALLOPATHIC,
                targetList = walkInMeds
            ) { refreshSummary() }
        }
        // TK APPROVED (2026-07-15): "Apply Common Prescription/Medicine Slip" --
        // same remembered-set pattern as Blood Test's Apply Common. Re-adds
        // whatever medicine names were saved last time (each with its own last
        // dose/type via rxDoseFor/rxTypeFor), skipping duplicates.
        val applyCommonBtn = android.widget.TextView(this).apply {
            text = if (isPrescription) "⭐  Apply Common Prescription" else "⭐  Apply Common Medicine Slip"
            textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1B2432"))
            background = premiumSilverDrawable()
            elevation = 6f * d
            gravity = android.view.Gravity.CENTER
            val pv = (10 * d).toInt(); setPadding(pv, pv, pv, pv)
            isClickable = true; isFocusable = true
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (8 * d).toInt() }
        }
        applyCommonBtn.postDelayed({ animatePulse(applyCommonBtn) }, 400)
        applyCommonBtn.setOnClickListener {
            val remembered = if (isPrescription) com.tkbiswas.pilesclinic.clinical.ClinicalRepository.getCommonPrescription()
                else com.tkbiswas.pilesclinic.clinical.ClinicalRepository.getCommonMedicineSlip()
            if (remembered.isEmpty()) {
                Toast.makeText(this, "No common set saved yet — add medicines and print once first.", Toast.LENGTH_SHORT).show()
            } else {
                var added = 0
                remembered.forEach { medName ->
                    if (walkInMeds.none { it.name == medName }) {
                        walkInMeds.add(
                            com.tkbiswas.pilesclinic.clinical.MedicineEntry(
                                name = medName,
                                dosage = com.tkbiswas.pilesclinic.clinical.ClinicalRepository.rxDoseFor(medName),
                                medicineType = com.tkbiswas.pilesclinic.clinical.ClinicalRepository.rxTypeFor(medName)
                            )
                        )
                        added++
                    }
                }
                refreshSummary()
                Toast.makeText(this, if (added > 0) "$added medicine(s) applied." else "Already all on the list.", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(applyCommonBtn)
        container.addView(addBtn)
        container.addView(addedSummary)

        val docAccent = if (isPrescription) android.graphics.Color.parseColor("#1557D6") else android.graphics.Color.parseColor("#15913A")
        showPremiumFormDialog(
            title = "Direct $docType (Walk-in)",
            icon = if (isPrescription) "📝" else "💊",
            accentColor = docAccent,
            bodyView = container,
            primaryLabel = "PREVIEW & PRINT"
        ) {
            val name = nameInput.text.toString().trim().ifBlank { "WALK-IN" }
            val mobileDigits = mobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
            val branch = branchNames.getOrElse(branchSpinner.selectedItemPosition) { BranchSession.current.displayName }
            if (walkInMeds.isEmpty()) {
                Toast.makeText(this, "Select at least one medicine", Toast.LENGTH_SHORT).show()
                return@showPremiumFormDialog false
            }
            // TK FIX (2026-07-15): Type mandatory before print, same rule as
            // the Patient card Prescription/Medicine Slip screen.
            val missingType = walkInMeds.filter { it.medicineType.isBlank() }.map { it.name }
            if (missingType.isNotEmpty()) {
                Toast.makeText(this, "Please set Type (Tab/Cap/Syp/...) for: ${missingType.joinToString(", ")}", Toast.LENGTH_LONG).show()
                return@showPremiumFormDialog false
            }
            // TK APPROVED (2026-07-15): this exact medicine set becomes the new
            // "Common Prescription"/"Common Medicine Slip" default for next
            // time -- same as the Patient card screens.
            if (isPrescription) com.tkbiswas.pilesclinic.clinical.ClinicalRepository.saveCommonPrescription(walkInMeds.map { it.name }.toSet())
            else com.tkbiswas.pilesclinic.clinical.ClinicalRepository.saveCommonMedicineSlip(walkInMeds.map { it.name }.toSet())
            val lines = walkInMeds.map { m ->
                val parts = listOfNotNull(
                    m.dosage.takeIf { it.isNotBlank() },
                    m.frequency.takeIf { it.isNotBlank() },
                    m.duration.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                val instructionPart = if (m.instructions.isNotBlank()) "  [${m.instructions}]" else ""
                (if (parts.isNotBlank()) parts else "As advised") + instructionPart
            }
            val rxTypes = walkInMeds.map { it.medicineType }
            val rxNames = walkInMeds.map { it.name }
            val rxDosage = walkInMeds.map { it.dosage.ifBlank { "-" } }
            val rxFrequency = walkInMeds.map { it.frequency.ifBlank { "-" } }
            val rxDuration = walkInMeds.map { it.duration.ifBlank { "-" } }
            // 💊 V723 — Walk-in কাগজেও Instruction নামের নিচে ছাপবে।
            val rxInstructions = walkInMeds.map { it.instructions.trim() }
            val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
            val ageSex = listOf(wf.age.text.toString().trim(), wf.sex.text.toString().trim()).filter { it.isNotBlank() }.joinToString(" / ")
            PrintDataHolder.pendingModel = PrintDocumentModel(
                documentTitle = if (isPrescription) "Prescription" else "Medicine Slip",
                branchName = branch,
                patientName = name,
                patientId = wf.matchedPatientId,
                dateLabel = today,
                // TK FIX (2026-07-15): removed literal "Rx" heading (Prescription
                // already shows the ℞ symbol -- was printing "Rx" twice).
                sections = listOf(PrintSection(if (isPrescription) null else "Medicines", lines, rxTypes, rxNames, rxDosage, rxFrequency, rxDuration, rxInstructions)),   // 💊 V723
                qrPayload = null,
                footerNote = "Walk-in print — no registration on file." +
                    (if (mobileDigits.length == 10) " Mobile: $mobileDigits" else ""),
                patientAddress = wf.address.text.toString().trim(),
                patientAgeSex = ageSex,
                patientDisease = wf.disease.text.toString().trim(),
                patientMobile = mobileDigits
            )
            // restore whatever real in-progress patient prescription/slip was
            // there before this Walk-in form opened -- Walk-in never overwrites it.
            // walkInMeds is a private local list -- nothing to restore, the
            // real patient session (if any) was never touched.
            openPreview()
            true
        }
    }

    private fun fetchAndPrintClinical(mobileDigits: String, docType: String) {
        val medicalType = if (docType == "Diet Chart") "Diet Chart" else "Prescription"
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val patients = myBranchOnly(SupabaseClient.findByMobile(
                    "patients", "+91$mobileDigits",
                    "id,name,patientId,mobile,branch,visitDate,registrationDate,date", 20
                ))
                val patient = pickPatientForPrint(patients, mobileDigits)   // 🔵 V530
                    ?: return@withContext null
                val pid = patient.optString("patientId", "")
                val puuid = patient.optString("id", "")
                if (pid.isBlank() && puuid.isBlank()) return@withContext Pair(patient, null as org.json.JSONObject?)
                // Medical may be keyed by the WebView's row id or the native P-xxxx id.
                val medFilter = when {
                    puuid.isNotBlank() && pid.isNotBlank() -> "or=(patientId.eq.$puuid,patientId.eq.$pid)"
                    pid.isNotBlank() -> "patientId=eq.$pid"
                    else -> "patientId=eq.$puuid"
                }
                // 🔴🔒 V794 — ছবি ছাড়া; সীমাও বসানো হলো (আগে সীমাই ছিল না)
                val meds = SupabaseClient.fetchListSlim("medical", medFilter, 500,
                        SupabaseClient.MEDICAL_COLS)
                var latest: org.json.JSONObject? = null
                for (i in 0 until meds.length()) {
                    val row = meds.getJSONObject(i)
                    if (row.optString("type") != medicalType) continue
                    if (latest == null ||
                        row.optString("createdAt") > latest!!.optString("createdAt")) {
                        latest = row
                    }
                }
                Pair(patient, latest)
            }
            // TK APPROVED (2026-07-15): the "No patient found" / "No saved
            // Prescription yet" confirmation popup was confusing staff/doctor
            // (they'd expect the number to just work) -- removed. Whether the
            // mobile matches a patient or not, go straight to the walk-in form;
            // addWalkInHeader's own auto-match already fills every field the
            // instant it recognises the number, exactly like Blood Test already
            // does. Nothing is saved until Preview & Print is tapped either way.
            if (result == null) {
                showDirectPrintForm(docType, mobileDigits)
                return@launch
            }
            val (patient, medical) = result
            if (medical == null) {
                showDirectPrintForm(docType, mobileDigits)
                return@launch
            }
            PrintDataHolder.pendingModel = when (docType) {
                "Diet Chart" -> PrintMappersCloud.dietFromMedical(patient, medical)
                "Medicine Slip" -> PrintMappersCloud.medicineSlipFromMedical(patient, medical)
                else -> PrintMappersCloud.prescriptionFromMedical(patient, medical)
            }
            openPreview()
        }
    }

    private fun setupBranchToggle() {
        val btn = findViewById<MaterialButton>(R.id.btnBranchToggle)
        // 🔒 TK-APPROVED (29.07.2026, খাতার সারি B84): ব্রাঞ্চ বাছার ঘর এখন
        // হেডারের ডান দিকে, বাকি পর্দাগুলোর হুবহু একই পিল। পুরনো বোতামটা
        // মোছা হয়নি — শুধু লুকানো — তাই নিচের সব কোড আগের মতোই কাজ করে।
        val pill = findViewById<android.widget.TextView>(R.id.branchPicker)

        fun show(name: String) {
            btn.text = name
            pill.text = "🏥 $name ▾"
        }
        show(BranchSession.current.displayName)

        val onTap = android.view.View.OnClickListener {
            if (!com.tkbiswas.pilesclinic.security.SecurityGuard.canSwitchBranch(this)) {
                // 🔴 B613 (10.08.2026): আগের বার্তা ভুল ছিল ("Only a Doctor...") —
                // আসলে `canSwitchBranch` = শুধু Master। Doctor/Staff/Field সবাই নিজের
                // ব্রাঞ্চেই থাকেন। বার্তাটা সঠিক করা হলো।
                Toast.makeText(
                    this,
                    "Only Master can switch branches. You stay on your own branch.",
                    Toast.LENGTH_LONG
                ).show()
                return@OnClickListener
            }
            // 🔒 খাতার সারি B84 — পপ-আপটা **Follow-up-এর হুবহু একই** (TK
            // ২৯.০৭.২০২৬-এ ছবি দিয়ে দেখিয়ে দিয়েছেন): শিরোনাম "Branch" · গোল
            // বোতামের তালিকা (এখন যেটা বাছা আছে সেটায় দাগ) · নিচে "Cancel"।
            // ⛔ আগে এখানে চাপ দিলে পরপর এক ব্রাঞ্চ থেকে আরেক ব্রাঞ্চে ঘুরত,
            //    কোনো তালিকাই আসত না — সেটা এখন এই এক নিয়মে এলো।
            // ⛔ এখানে "All" নেই — ছাপার জন্য একটাই ব্রাঞ্চ বাছতে হয়।
            val names = BranchCatalog.all.map { it.displayName }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(
                    names.toTypedArray(),
                    BranchCatalog.all.indexOf(BranchSession.current).coerceAtLeast(0)
                ) { dialog, which ->
                    val next = BranchCatalog.all[which]
                    BranchSession.current = next
                    show(next.displayName)
                    Toast.makeText(this, "Print branch set to ${next.displayName}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
        }
        btn.setOnClickListener(onTap)
        pill.setOnClickListener(onTap)
    }

    private fun openLatestRegistration() {
        showProfessionalMobileDialog(
            title = "Patient Registration Print",
            message = "Enter the patient's mobile number or name to print their Registration Slip.",
            accentColor = android.graphics.Color.parseColor("#0E7C86"),
            primaryLabel = "FIND & PRINT",
            allowNameSearch = true,
            onPrimary = { query -> resolveMobileOrName(query) { digits -> fetchAndPrintRegistration(digits) } }
        )
    }

    private fun fetchAndPrintRegistration(mobileDigits: String) {
        lifecycleScope.launch {
            val patient = withContext(Dispatchers.IO) {
                // 🔒 B601 (TK-নির্দেশ, প্রুফ-হুবহু): পুরনো রোগীর Registration Form পুনঃপ্রিন্টেও
                // ওয়েবের হুবহু ডিজাইন। তাই রেজিস্ট্রেশনের সময় জমা-হওয়া বাড়তি ঘরগুলোও আনা
                // হচ্ছে (occupation/complaint/sinceWhen/previousTreatment/refDoctor…) — এগুলো
                // buildPatientRow-এ **সবসময়** লেখা হয় বলে কলাম নিশ্চিত আছে, 400 হবে না।
                val patients = myBranchOnly(SupabaseClient.findByMobile(
                    "patients", "+91$mobileDigits",
                    "id,patientId,name,mobile,branch,disease,diagnosis,address,age,sex,occupation,refBy,refDoctor,refDoctorMobile,complaint,sinceWhen,previousTreatment,registrationDate,date,visitDate", 20
                ))
                pickPatientForPrint(patients, mobileDigits)   // 🔵 V530
            }
            if (patient == null) {
                Toast.makeText(
                    this@PrintCenterActivity,
                    "No patient found for this mobile. Check the number or connection.",
                    Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                return@launch
            }
            // 🔒 B601 (TK-নির্দেশ, প্রুফ-হুবহু, 10.08.2026): নেটিভ structured প্রিন্টের বদলে
            // ওয়েবের অনুমোদিত ডিজাইনেই (RegistrationHtmlPrint — WebView→PrintManager) ছাপে,
            // যাতে ফোনের প্রিন্ট প্রুফের হুবহু। ⛔ অন্য প্রিন্ট (Payment/Prescription) অক্ষত।
            RegistrationHtmlPrint.print(this@PrintCenterActivity, patient)
        }
    }

    private fun openPreview() {
        startActivity(Intent(this, PrintPreviewActivity::class.java))
    }

    /**
     * Payment Receipt: staff enters the patient's 10-digit mobile; we look the
     * patient up in the live Supabase "patients" table (same as PaymentRepository),
     * then fetch that patient's payments and print the most recent one. Reading
     * from Supabase (not Room) is deliberate -- that is where the Payment screen
     * actually saves, so the receipt reflects real collected money.
     */
    private fun showPaymentReceiptPicker() {
        showProfessionalMobileDialog(
            title = "Payment Receipt",
            message = "Enter the patient's mobile number or name to print their latest Payment Receipt.",
            accentColor = android.graphics.Color.parseColor("#F79009"),
            primaryLabel = "FIND & PRINT",
            allowNameSearch = true,
            onPrimary = { query -> fetchAndPrintLatestPaymentByQuery(query) }
        )
    }

    /** TK-REQUESTED ADDITION (2026-07-19): shared name-or-mobile resolver for
     *  EVERY Print Center search dialog (Prescription/Medicine Slip/Diet
     *  Chart, Registration Slip, Payment Receipt, Doctor Checkup, Blood
     *  Test). A 10-digit query goes straight to `onResolved` unchanged.
     *  Anything else is treated as a name -- partial, case-insensitive
     *  match against the "patients" table. Exactly one match resolves
     *  straight through; more than one shows the tap-to-pick list below;
     *  zero shows a "not found" message. */
    private fun resolveMobileOrName(query: String, onResolved: (String) -> Unit) {
        val digits = query.filter { it.isDigit() }
        if (digits.length == 10) { onResolved(digits); return }
        lifecycleScope.launch {
            val matches = withContext(Dispatchers.IO) {
                try {
                    SupabaseClient.fetchList(
                        "patients",
                        "name=ilike.*${java.net.URLEncoder.encode(query.trim(), "UTF-8")}*",
                        20
                    )
                } catch (_: Throwable) { org.json.JSONArray() }
            }
            when (matches.length()) {
                0 -> Toast.makeText(
                    this@PrintCenterActivity,
                    "No patient found with that name. Check the spelling or try the mobile number.",
                    Toast.LENGTH_LONG
                ).show()
                1 -> {
                    val mobile = matches.getJSONObject(0).optString("mobile", "").filter { it.isDigit() }.takeLast(10)
                    if (mobile.length == 10) onResolved(mobile)
                    else Toast.makeText(this@PrintCenterActivity, "That patient has no mobile number on file.", Toast.LENGTH_LONG).show()
                }
                else -> showNameMatchPicker(matches) { mobile -> onResolved(mobile) }
            }
        }
    }

    private fun fetchAndPrintLatestPaymentByQuery(query: String) {
        resolveMobileOrName(query) { mobile -> fetchAndPrintLatestPayment(mobile) }
    }

    /** Simple tap-to-pick list when a name search matches more than one
     *  patient -- shows Name + masked mobile per row so staff can tell them
     *  apart without exposing the full number in a shared list. */
    private fun showNameMatchPicker(matches: org.json.JSONArray, onPick: (String) -> Unit) {
        val names = (0 until matches.length()).map { i ->
            val row = matches.getJSONObject(i)
            val mobile = row.s("mobile").filter { it.isDigit() }.takeLast(10)
            val masked = if (mobile.length == 10) "${mobile.take(2)}XXXXXX${mobile.takeLast(2)}" else "-"
            "${row.s("name").ifBlank { "-" }}  ·  $masked"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Multiple patients found — select one"))
            .setItems(names) { d, which ->
                d.dismiss()
                val mobile = matches.getJSONObject(which).s("mobile").filter { it.isDigit() }.takeLast(10)
                if (mobile.length == 10) onPick(mobile)
            }
            .setNegativeButton("Cancel", null)
            .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
    }

    private fun fetchAndPrintLatestPayment(mobileDigits: String) {
        lifecycleScope.launch {
            val payment = withContext(Dispatchers.IO) {
                val patients = myBranchOnly(SupabaseClient.findByMobile(
                    "patients", "+91$mobileDigits", "id,name,patientId,branch", 20
                ))
                val patientRow = pickPatientForPrint(patients, mobileDigits)   // 🔵 V530
                    ?: return@withContext null
                val patientId = patientRow.s("id")
                val humanPatientId = patientRow.s("patientId")
                if (patientId.isBlank()) return@withContext null
                val payments = SupabaseClient.fetchList("payments", "patientId=eq.$patientId")
                if (payments.length() == 0) null else payments.getJSONObject(0).apply {
                    put("patientId", humanPatientId)
                    // 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত): রসিদে ছাপা নামটাও
                    // (Advance / 2nd Payment …) এখন **দিন ধরে** হিসাব করা — একই
                    // দিনের সব টাকা একটাই নম্বরে। পুরনো রেকর্ডেও ঠিক ছাপে।
                    // ⛔ ক্লাউডে বাড়তি অনুরোধ নেই — উপরের ওই তালিকাটাই ব্যবহার
                    // হচ্ছে। ⛔ ডেটাবেসে কিছু লেখা হয় না, শুধু ছাপার আগে এই
                    // কপিটাতে নামটা বসানো হয়।
                    val all = (0 until payments.length()).mapNotNull { payments.optJSONObject(it) }
                    val fixed = com.tkbiswas.pilesclinic.native.PaymentModel
                        .dayBasedLabelById(all)[optString("id", "")] ?: ""
                    if (fixed.isNotBlank()) put("payLabel", fixed)
                }
            }
            if (payment == null) {
                Toast.makeText(
                    this@PrintCenterActivity,
                    "No payment found for this mobile. Check the number or connection.",
                    Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                return@launch
            }
            PrintDataHolder.pendingModel = PrintMappersCloud.paymentReceipt(payment)
            openPreview()
        }
    }

    /**
     * Doctor Visit Print: enter mobile, look the patient up in the live Supabase
     * "patients" table, and print the visit sheet built from that row (matching
     * printDoctorVisit()).
     */
    private fun showDoctorVisitPrintPicker() {
        showProfessionalMobileDialog(
            title = "Doctor Checkup Print",
            message = "Enter the patient's mobile number or name to print their Checkup sheet.",
            accentColor = android.graphics.Color.parseColor("#0B4F2A"),
            primaryLabel = "FIND & PRINT",
            allowNameSearch = true,
            onPrimary = { query -> resolveMobileOrName(query) { digits ->
                lifecycleScope.launch {
                    val patient = withContext(Dispatchers.IO) {
                        val rows = myBranchOnly(SupabaseClient.findByMobile(
                            "patients", "+91$digits",
                            "id,name,patientId,mobile,branch,disease,visitDate,registrationDate,date,refBy", 20
                        ))
                        pickPatientForPrint(rows, digits)   // 🔵 V530
                    }
                    if (patient == null) {
                        Toast.makeText(
                            this@PrintCenterActivity,
                            "No patient found for this mobile. Check the number or connection.",
                            Toast.LENGTH_LONG
                        ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                        return@launch
                    }
                    PrintDataHolder.pendingModel = PrintMappersCloud.doctorVisitPrint(patient)
                    openPreview()
                }
            } }
        )
    }

    /**
     * Blood Test: enter mobile -> fetch patient -> tick investigations from the
     * ported BLOOD_TESTS list + optional remarks -> print. Mirrors blood(id)/
     * printBlood(id).
     */
    private fun showBloodTestPicker() {
        showProfessionalMobileDialog(
            title = "Blood Test / Investigation",
            message = "Enter the patient's mobile number or name to continue for Blood Test / Investigation.",
            accentColor = android.graphics.Color.parseColor("#6F42C1"),
            primaryLabel = "NEXT",
            allowNameSearch = true,
            onPrimary = { query -> resolveMobileOrName(query) { digits ->
                lifecycleScope.launch {
                    val patient = withContext(Dispatchers.IO) {
                        val rows = myBranchOnly(SupabaseClient.findByMobile(
                            "patients", "+91$digits",
                            "id,name,patientId,mobile,branch,visitDate,registrationDate,date", 20
                        ))
                        pickPatientForPrint(rows, digits)   // 🔵 V530
                    }
                    if (patient == null) {
                        showDirectBloodTestForm(digits)
                        return@launch
                    }
                    showBloodTestSelectionDialog(patient)
                }
            } },
            onDirect = { showDirectBloodTestForm() }
        )
    }

    // TK FIX (2026-07-15): Blood Test now uses the SAME category grid (Hematology
    // / Bio-Chemistry / Immunology / Special Test / Urine / Stool -- from
    // ClinicalRepository.investigationCategories) and the same "Apply Common
    // Blood Test" remembered-set shortcut as the Patient/Visit card screen, for
    // BOTH the mobile-found-patient and the Walk-in path -- so they are
    // identical, as TK asked. No Doctor-approval step here (TK: staff does
    // everything, no doctor role in this flow) -- just pick tests and print.

    /** One category card in the Blood Test grid. */
    private fun buildBloodTestCategoryGrid(
        grid: android.widget.LinearLayout,
        selectedTests: MutableSet<String>,
        accentHex: String,
        onChanged: () -> Unit
    ) {
        grid.removeAllViews()
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val palette = listOf(
            Triple("#FBD1D1", "#F8ECEC", "#7A1F1F"), Triple("#D6E6FB", "#EAF2FD", "#12407A"),
            Triple("#CFF0DC", "#E9FBF0", "#0C5A2C"), Triple("#FCEFC2", "#FFF8E1", "#7A5B0C"),
            Triple("#E3D6FB", "#F2EAFD", "#4A1F7A"), Triple("#CFF4EE", "#E9FBF8", "#0C5A50")
        )
        val cats = com.tkbiswas.pilesclinic.clinical.ClinicalRepository.investigationCategories
        var row: android.widget.LinearLayout? = null
        cats.forEachIndexed { index, cat ->
            if (index % 2 == 0) {
                row = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = dp(10) }
                }
                grid.addView(row)
            }
            val (c1, c2, textColor) = palette[index % palette.size]
            val selectedCount = cat.tests.count { selectedTests.contains(it) }
            val card = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(dp(10), dp(14), dp(10), dp(14))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    colors = intArrayOf(android.graphics.Color.parseColor(c1), android.graphics.Color.parseColor(c2))
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.setMargins(dp(4), 0, dp(4), 0) }
                isClickable = true; isFocusable = true
            }
            card.addView(android.widget.TextView(this).apply { text = cat.emoji; textSize = 20f; gravity = android.view.Gravity.CENTER })
            card.addView(android.widget.TextView(this).apply {
                text = cat.name; textSize = 11.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(textColor)); gravity = android.view.Gravity.CENTER
                val p = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                p.topMargin = dp(5); layoutParams = p
            })
            card.addView(android.widget.TextView(this).apply {
                text = if (selectedCount > 0) "$selectedCount / ${cat.tests.size} selected" else "${cat.tests.size} tests"
                textSize = 9f; setTextColor(android.graphics.Color.parseColor(textColor)); gravity = android.view.Gravity.CENTER
            })
            card.setOnClickListener {
                showBloodTestCategoryChecklist(cat.name, cat.tests, selectedTests) {
                    buildBloodTestCategoryGrid(grid, selectedTests, accentHex, onChanged)
                    onChanged()
                }
            }
            row?.addView(card)
        }
        if (cats.size % 2 == 1) row?.addView(android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, 0, 1f)
        })
    }

    /** Checkbox list for one category's tests -- tap a category card to open.
     *  🔒 TK-এর নির্দেশ (02.08.2026, স্ক্রিনশটসহ — "একেক জায়গা থেকে একেক
     *  রকম আসে"): এই পপ-আপটা আগে সাধারণ বর্গাকার CheckBox তালিকা ছিল, অথচ
     *  Investigation Advice-এর একই কাজের পর্দা (InvestigationCategoryActivity)
     *  সাদা কার্ড + গোল টিক-বৃত্তের প্রিমিয়াম চেহারায়। এখন **হুবহু সেই
     *  একই সারি-ডিজাইন** (রং/মাপ/ব্যবহার অক্ষরে অক্ষরে নকল) এখানেও বসানো
     *  হলো, প্লাস প্রিমিয়াম হেডার (item 11-এর লক করা নিয়ম অনুযায়ী)।
     *  ⛔ ডেটার হিসাব (`selectedTests`, `onDone`) এক অক্ষরও বদলায়নি। */
    private fun showBloodTestCategoryChecklist(
        categoryName: String,
        tests: List<String>,
        selectedTests: MutableSet<String>,
        onDone: () -> Unit
    ) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun roundedBg(fill: String, stroke: String, radius: Int): android.graphics.drawable.GradientDrawable =
            android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = radius.toFloat()
                setColor(android.graphics.Color.parseColor(fill))
                setStroke(2, android.graphics.Color.parseColor(stroke))
            }
        val listContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(4))
        }
        // TK-এর নিয়ম মেনে "Done" চাপার আগ পর্যন্ত কিছুই চূড়ান্ত সেভ হয় না —
        // তাই আগের CheckBox-দের বদলে এখন `checkedNow` (স্থানীয় mutable set)
        // দিয়ে ট্র্যাক করা হয়, Done-এই selectedTests-এ ফাইনাল হয়।
        val checkedNow = tests.filterTo(mutableSetOf()) { selectedTests.contains(it) }
        tests.forEach { name ->
            var isChecked = checkedNow.contains(name)
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedBg(if (isChecked) "#EAFBF0" else "#FFFFFF", if (isChecked) "#0B4F2A" else "#E1E6ED", dp(12))
                val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(8); layoutParams = lp
                isClickable = true; isFocusable = true
            }
            val circle = android.widget.TextView(this).apply {
                text = if (isChecked) "✓" else ""
                textSize = 13f; gravity = android.view.Gravity.CENTER; setTextColor(android.graphics.Color.WHITE)
                background = roundedBg(if (isChecked) "#0B4F2A" else "#FFFFFF", "#B7C0CE", dp(20))
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(26), dp(26))
            }
            val label = android.widget.TextView(this).apply {
                text = name; textSize = 13.5f
                setTextColor(android.graphics.Color.parseColor(if (isChecked) "#0B4F2A" else "#10223A"))
                val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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
        val scroll = android.widget.ScrollView(this).apply {
            addView(listContainer)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(360)
            )
        }
        UppercaseInputUtil.applyToAll(scroll)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, categoryName))
            .setView(scroll)
            .setPositiveButton("Done") { _, _ ->
                tests.forEach { name ->
                    if (checkedNow.contains(name)) selectedTests.add(name) else selectedTests.remove(name)
                }
                onDone()
            }
            .setNegativeButton("Cancel", null)
            .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
    }

    private fun showBloodTestSelectionDialog(patient: org.json.JSONObject) {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val selectedTests = mutableSetOf<String>()
        val grid = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        val commonBtn = bloodTestCommonButton(selectedTests) { buildBloodTestCategoryGrid(grid, selectedTests, "#6F42C1") {} }
        container.addView(commonBtn)
        container.addView(grid)
        buildBloodTestCategoryGrid(grid, selectedTests, "#6F42C1") {}

        val remarksLabel = android.widget.TextView(this).apply {
            text = "Advice / Remarks"; setPadding(0, 20, 0, 4)
        }
        container.addView(remarksLabel)
        val remarksInput = EditText(this).apply {
            hint = "Advice / Remarks (optional)"
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        container.addView(remarksInput)

        showPremiumFormDialog(
            title = "Select Investigations",
            icon = "🩸",
            accentColor = android.graphics.Color.parseColor("#6F42C1"),
            bodyView = container,
            primaryLabel = "PRINT"
        ) {
            if (selectedTests.isEmpty()) {
                Toast.makeText(this, "Select at least one test", Toast.LENGTH_SHORT).show()
                return@showPremiumFormDialog false
            }
            com.tkbiswas.pilesclinic.clinical.ClinicalRepository.saveCommonBloodTest(selectedTests.toSet())
            PrintDataHolder.pendingModel = PrintMappersCloud.bloodTest(
                patient, selectedTests.toList(), remarksInput.text.toString().trim()
            )
            openPreview()
            true
        }
    }

    /** "Apply Common Blood Test" button -- re-checks whatever set was saved
     *  last time (ClinicalRepository.getCommonBloodTest, same store used by
     *  the Patient/Visit card screen), shared by both Blood Test entry points. */
    private fun bloodTestCommonButton(selectedTests: MutableSet<String>, onApplied: () -> Unit): android.widget.LinearLayout {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val btn = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = premiumSilverDrawable()
            elevation = 6f * d
            isClickable = true; isFocusable = true
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(12) }
        }
        btn.addView(android.widget.TextView(this).apply {
            text = "⭐  Apply Common Blood Test"
            textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1B2432")); gravity = android.view.Gravity.CENTER
        })
        btn.addView(android.widget.TextView(this).apply {
            text = "Re-checks whatever was saved last time"
            textSize = 10.5f; setTextColor(android.graphics.Color.parseColor("#4A5568")); gravity = android.view.Gravity.CENTER
        })
        btn.postDelayed({ animatePulse(btn) }, 400)
        btn.setOnClickListener {
            val remembered = com.tkbiswas.pilesclinic.clinical.ClinicalRepository.getCommonBloodTest()
            if (remembered.isEmpty()) {
                Toast.makeText(this, "No common set saved yet — select tests and Print once first.", Toast.LENGTH_SHORT).show()
            } else {
                selectedTests.addAll(remembered)
                onApplied()
            }
        }
        return btn
    }

    /** TK APPROVED (2026-07-15): shared premium metallic-silver background for
     *  the "Apply Common ..." buttons only (Blood Test / Prescription /
     *  Medicine Slip, Print Center Walk-in). Nothing else uses this. */
    private fun premiumSilverDrawable(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            val d = resources.displayMetrics.density
            cornerRadius = 14f * d
            orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
            colors = intArrayOf(
                android.graphics.Color.parseColor("#F4F6F9"),
                android.graphics.Color.parseColor("#D6DBE2"),
                android.graphics.Color.parseColor("#A7ADB8")
            )
            setStroke((1 * d).toInt(), android.graphics.Color.WHITE)
        }
    }

    /** TK APPROVED (2026-07-15): shared gentle "premium pulse" -- one soft
     *  scale-up-and-back, not a repeating loop, used only on the new Apply
     *  Common buttons. */
    private fun animatePulse(view: android.view.View) {
        val anim = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            view,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.04f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.04f)
        )
        anim.duration = 650
        anim.repeatMode = android.animation.ValueAnimator.REVERSE
        anim.repeatCount = 1
        anim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        anim.start()
    }

    /** Web-parity walk-in Blood Test: same category grid + Apply Common Blood
     *  Test as the mobile-found-patient path above -- only the "patient" is
     *  typed name/mobile/branch instead of looked up. Nothing is saved to the
     *  patient database, print only (TK approved 2026-07-15). */
    private fun showDirectBloodTestForm(prefillMobile: String = "") {
        val d = resources.displayMetrics.density
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), 0)
        }
        val wf = addWalkInHeader(container, prefillMobile)
        val nameInput = wf.name; val mobileInput = wf.mobile; val branchSpinner = wf.branchSpinner
        val branchNames = com.tkbiswas.pilesclinic.print.BranchCatalog.all.map { it.displayName }

        container.addView(sectionLabel("Investigations"))
        val selectedTests = mutableSetOf<String>()
        val grid = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        val commonBtn = bloodTestCommonButton(selectedTests) { buildBloodTestCategoryGrid(grid, selectedTests, "#6F42C1") {} }
        container.addView(commonBtn)
        container.addView(grid)
        buildBloodTestCategoryGrid(grid, selectedTests, "#6F42C1") {}

        container.addView(sectionLabel("Advice / Remarks"))
        val remarksInput = EditText(this).apply {
            hint = "Advice / Remarks (optional)"
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = (14 * d).toInt(); setPadding(p, p, p, p)
        }
        container.addView(remarksInput)

        showPremiumFormDialog(
            title = "Direct Blood Test (Walk-in)",
            icon = "🩸",
            accentColor = android.graphics.Color.parseColor("#6F42C1"),
            bodyView = container,
            primaryLabel = "PREVIEW & PRINT"
        ) {
            if (selectedTests.isEmpty()) {
                Toast.makeText(this, "Select at least one test", Toast.LENGTH_SHORT).show()
                return@showPremiumFormDialog false
            }
            val name = nameInput.text.toString().trim().ifBlank { "WALK-IN" }
            val mobileDigits = mobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
            val branch = branchNames.getOrElse(branchSpinner.selectedItemPosition) { BranchSession.current.displayName }
            val walkInPatient = org.json.JSONObject()
                .put("name", name)
                .put("branch", branch)
                .put("patientId", "")
                .put("id", "")
                .put("age", wf.age.text.toString().trim())
                .put("sex", wf.sex.text.toString().trim())
                .put("disease", wf.disease.text.toString().trim())
                .put("address", wf.address.text.toString().trim())
            val remarks = remarksInput.text.toString().trim() +
                (if (mobileDigits.length == 10) "\nMobile: $mobileDigits (walk-in, no registration)" else "\n(walk-in, no registration)")
            com.tkbiswas.pilesclinic.clinical.ClinicalRepository.saveCommonBloodTest(selectedTests.toSet())
            PrintDataHolder.pendingModel = PrintMappersCloud.bloodTest(walkInPatient, selectedTests.toList(), remarks.trim())
            openPreview()
            true
        }
    }

    /** TK-ORDER (2026-07-25): printing is always for THIS branch's patients,
     *  so a patient looked up here must belong to the logged-in staff's own
     *  branch. Master is unrestricted (they print for every branch). Only
     *  filtering . no query, no layout, no print design is changed. */
    /**
     * 🔵🔒 V530 (২২.০৮.২০২৬, TK-নির্দেশ) — **ভুল রোগীর নামে আর ছাপা হবে না।**
     *
     * **আগে যা হত:** এই পর্দার ছ'টা জায়গায় নম্বর দিয়ে রোগী খুঁজে **প্রথম
     * সারিটাই** নেওয়া হত (`rows.getJSONObject(0)`)। এক নম্বরে স্বামী-স্ত্রী
     * দু'জন আলাদা রোগী থাকলে **অন্যজনের নামে প্রেসক্রিপশন / Patient Card /
     * রসিদ ছাপা হয়ে যেতে পারত** — গোটা প্রজেক্টের সবচেয়ে বিপজ্জনক ফাঁক।
     *
     * **এখন:** ওই নম্বরে সত্যিই একাধিক আলাদা রোগী থাকলে **তখনই** জিজ্ঞাসা।
     *
     * ⛔ একজন থাকলে (রোজকার ৯৯%) **হুবহু আগের সেই লাইনটাই** চলে —
     *    `rows.optJSONObject(0)`। কোনো বাড়তি প্রশ্ন নেই, ছাপার নকশা, ঘর,
     *    ক্রম — কিচ্ছু বদলায়নি।
     * ⛔ **নতুন কোনো ক্লাউড-অনুরোধ নেই** — আগে থেকেই আনা সারিগুলোর উপরেই কাজ।
     * ⛔ স্টাফ *Cancel* করলে `null` — অর্থাৎ "রোগী মেলেনি"-র সেই একই পুরোনো
     *    পথ, ভুল নামে ছাপা হওয়ার চেয়ে যা অনেক নিরাপদ।
     */
    private suspend fun pickPatientForPrint(
        rows: org.json.JSONArray, mobileDigits: String
    ): org.json.JSONObject? {
        if (rows.length() == 0) return null
        val people = com.tkbiswas.pilesclinic.native.PatientIdentity
            .separateIdentities(rows, mobileDigits)
        if (people.size < 2) return rows.optJSONObject(0)   // ⛔ হুবহু আগের আচরণ
        return withContext(Dispatchers.Main) { askWhichPrintPatient(mobileDigits, people) }
    }

    private suspend fun askWhichPrintPatient(
        mobile: String, people: List<org.json.JSONObject>
    ): org.json.JSONObject? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val labels = people.map { r ->
            (r.s("name").ifBlank { "UNKNOWN" }) + "\n" + (r.s("patientId").ifBlank { "-" })
        }.toTypedArray()
        var done = false
        fun finishWith(v: org.json.JSONObject?) {
            if (done) return
            done = true
            if (cont.isActive) cont.resume(v)
        }
        if (isFinishing || isDestroyed) { finishWith(null); return@suspendCancellableCoroutine }
        AlertDialog.Builder(this)
            .setTitle("\uD83D\uDCDE $mobile \u2014 which patient?")
            .setItems(labels) { _, which -> finishWith(people.getOrNull(which)) }
            .setNegativeButton("Cancel") { _, _ -> finishWith(null) }
            .setOnCancelListener { finishWith(null) }
            .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
    }

    private fun myBranchOnly(rows: org.json.JSONArray): org.json.JSONArray {
        val me = com.tkbiswas.pilesclinic.native.NativeSession.current(this) ?: return rows
        if (me.role == "master") return rows
        val mine = me.branch.trim()
        if (mine.isBlank() || mine.equals("All", ignoreCase = true)) return rows
        val out = org.json.JSONArray()
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            if (r.s("branch").trim().equals(mine, ignoreCase = true)) out.put(r)
        }
        return out
    }

}
