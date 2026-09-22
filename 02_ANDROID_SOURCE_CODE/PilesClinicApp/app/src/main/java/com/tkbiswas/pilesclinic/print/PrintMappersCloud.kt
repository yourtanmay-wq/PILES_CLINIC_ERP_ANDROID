package com.tkbiswas.pilesclinic.print

import com.tkbiswas.pilesclinic.native.s
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Print mappers that build documents from LIVE Supabase rows (not Room).
 *
 * Why separate from PrintMappers: the existing PrintMappers.registration()
 * reads a Room RegistrationEntity, but the native data flow (Registration /
 * Payment / Doctor Queue screens) reads and writes the Supabase tables via
 * SupabaseClient. A Payment Receipt must therefore be built from the same
 * Supabase "payments" row the Payment screen actually saved, or it would print
 * blank. This object bridges that gap for cloud-sourced print types.
 *
 * NOTE for live verification: the older Print Center cards (Registration, etc.)
 * still read Room. Those may need the same Room->Supabase switch once the app is
 * live and we can confirm which store holds the real data on the device.
 */
object PrintMappersCloud {

    /** Ported verbatim from BLOOD_TESTS in app.js -- do not edit the wording or
     * order without matching the WebView, so both front-ends offer the same
     * investigations. */
    val BLOOD_TESTS = listOf(
        "CBC", "ESR", "HB", "HIV", "VDRL", "Sugar", "LFT", "Lipid Profile",
        "S. Creatinine", "Semen Analysis", "USG Scrotal", "Whole Abdomen",
        "Lower Abdomen", "MRI Fistulogram", "FNAC", "Biopsy"
    )

    private fun money(amount: Double): String {
        val whole = if (amount == amount.toLong().toDouble()) amount.toLong().toString()
        else String.format(Locale.US, "%.2f", amount)
        return "₹$whole"
    }

    /** Formats an ISO/yyyy-MM-dd date string for display; falls back to raw. */
    private fun displayDate(raw: String): String {
        if (raw.isBlank()) return "-"
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw.take(10))
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date ?: Date())
        } catch (e: Exception) { raw }
    }

    /**
     * Builds a Payment Receipt from a single Supabase "payments" row. The row
     * already carries name/patientId/amount/mode/date/label (see
     * PatientModel.buildVisitFeePaymentRow / PaymentRepository's saved rows), so
     * no extra patient lookup is required for the receipt body.
     */
    fun paymentReceipt(payment: JSONObject): PrintDocumentModel {
        val name = payment.s("name")
        val patientId = payment.s("patientId")
        val amount = payment.optDouble("amount", 0.0)
        val mode = payment.s("mode").ifBlank { "Cash" }
        val date = payment.s("date")
        val label = payment.s("payLabel").ifBlank { payment.s("paymentLabel").ifBlank { "Payment" } }
        val branch = payment.s("branch")
        val receivedBy = payment.s("receivedBy")
        val mobile = payment.s("mobile")

        val lines = mutableListOf(
            "Purpose: ${label.ifBlank { "Payment" }}",
            "Amount: ${money(amount)}",
            "Mode: ${mode.ifBlank { "Cash" }}",
            "Branch: ${branch.ifBlank { "-" }}"
        )
        // TK-REQUESTED (2026-07-22): staff CODE (e.g. "JPE-CRP"), not the
        // raw mobile number, on the printed receipt too.
        if (receivedBy.isNotBlank()) lines.add("Received By: ${com.tkbiswas.pilesclinic.native.StaffDirectory.findAccount(receivedBy)?.name ?: receivedBy}")

        return PrintDocumentModel(
            documentTitle = "Payment Receipt",
            patientName = name.ifBlank { "-" },
            patientId = patientId,
            dateLabel = displayDate(date),
            sections = listOf(PrintSection(heading = "Payment Details", lines = lines)),
            qrPayload = if (patientId.isNotBlank()) "PID:$patientId|AMT:${money(amount)}" else null,
            footerNote = "This is a computer-generated receipt.",
            branchName = branch,
            patientAddress = payment.s("address"),
            patientAgeSex = listOf(payment.s("age"), payment.s("sex").ifBlank { payment.s("gender") }).filter { it.isNotBlank() }.joinToString(" / "),
            patientDisease = payment.s("disease"),
            patientMobile = mobile
        )
    }

    /**
     * Doctor Visit Print, mirroring printDoctorVisit(id) in app.js: a compact
     * sheet with Patient block (name/mobile/branch) and Visit Details block
     * (disease / visit date / ref by). Built from a live Supabase "patients" row.
     */
    fun doctorVisitPrint(patient: JSONObject): PrintDocumentModel {
        val name = patient.s("name")
        val patientId = patient.s("patientId")
        val mobile = patient.s("mobile")
        val branch = patient.s("branch")
        val disease = patient.s("disease")
        val visitDate = patient.s("visitDate").ifBlank { patient.s("registrationDate").ifBlank { patient.s("date") } }
        val refBy = patient.s("refBy")

        val patientBlock = PrintSection(
            heading = "Patient",
            lines = listOf(
                "Name: ${name.ifBlank { "-" }}",
                "Mobile: ${mobile.ifBlank { "-" }}",
                "Branch: ${branch.ifBlank { "-" }}"
            )
        )
        val visitBlock = PrintSection(
            heading = "Visit Details",
            lines = listOf(
                "Disease: ${disease.ifBlank { "-" }}",
                "Visit Date: ${displayDate(visitDate)}",
                "Ref By: ${refBy.ifBlank { "-" }}"
            )
        )
        return PrintDocumentModel(
            documentTitle = "Doctor Visit Print",
            branchName = branch,
            patientName = name.ifBlank { "-" },
            patientId = patientId,
            dateLabel = displayDate(visitDate),
            sections = listOf(patientBlock, visitBlock),
            qrPayload = if (patientId.isNotBlank()) "PID:$patientId" else null,
            footerNote = null,
            patientAddress = patient.s("address"),
            patientAgeSex = listOf(patient.s("age"), patient.s("sex").ifBlank { patient.s("gender") }).filter { it.isNotBlank() }.joinToString(" / "),
            patientDisease = disease,
            patientMobile = mobile
        )
    }

    /**
     * Patient Registration print, built from a live Supabase "patients" row
     * (not Room). Shows the same details the WebView registration slip shows.
     */
    fun registration(patient: JSONObject): PrintDocumentModel {
        val name = patient.s("name")
        val patientId = patient.s("patientId")
        val mobile = patient.s("mobile")
        val branch = patient.s("branch")
        val disease = patient.s("disease")
        val address = patient.s("address")
        val age = patient.s("age")
        val sex = patient.s("sex").ifBlank { patient.s("gender") }
        val refBy = patient.s("refBy")
        val regDate = patient.s("registrationDate").ifBlank { patient.s("date").ifBlank { patient.s("visitDate") } }
        // 🔵🔒 TK-অনুমোদিত প্রুফ (09.08.2026): Registration A4 = পূর্ণ ফর্ম (সব ঘর) +
        // নিচে ফাঁকা "Doctor's Check-up" (ডাক্তার হাতে লেখেন)। ভেতরে Registration Date
        // বাদ (উপরে dateLabel-এ আছে — TK: "দুবার কেন")। Advance/Patient Decision শুধু
        // প্রিন্ট থেকে বাদ, সিস্টেম/চেকআপ-ফর্মে অটুট। ⛔ প্রিন্ট-রেন্ডারার বদলায়নি —
        // শুধু বাড়তি Section/lines (native-safe, বিল্ড-ঝুঁকি নেই)।
        val altMobile = patient.s("altMobile")
        val occupation = patient.s("occupation")
        val complaint = patient.s("complaint").ifBlank { patient.s("symptoms") }
        val sinceWhen = patient.s("sinceWhen").ifBlank { patient.s("durationNote") }
        val prevTreat = patient.s("previousTreatment").ifBlank { patient.s("prevTreatmentNote") }
        val refDoctor = patient.s("refDoctor")
        val refDoctorMobile = patient.s("refDoctorMobile")
        val regFee = patient.s("regFee")
        val regMode = patient.s("regMode").ifBlank { patient.s("payMode") }

        val details = listOf(
            "Patient ID: ${patientId.ifBlank { "-" }}",
            "Name: ${name.ifBlank { "-" }}",
            "Age / Sex: ${age.ifBlank { "-" }} / ${sex.ifBlank { "-" }}",
            "Occupation: ${occupation.ifBlank { "-" }}",
            "Mobile: ${mobile.ifBlank { "-" }}",
            "Alt Mobile: ${altMobile.ifBlank { "-" }}",
            "Branch: ${branch.ifBlank { "-" }}",
            "Disease: ${disease.ifBlank { "-" }}",
            "Address: ${address.ifBlank { "-" }}"
        )
        val sections = mutableListOf(PrintSection("Patient Details", details))
        val refLines = mutableListOf<String>()
        if (refBy.isNotBlank()) refLines.add("Referred By: $refBy")
        if (refDoctor.isNotBlank()) refLines.add("Doctor Name: $refDoctor")
        if (refDoctorMobile.isNotBlank()) refLines.add("Doctor Mobile: $refDoctorMobile")
        if (refLines.isNotEmpty()) sections.add(PrintSection("Referred By", refLines))
        val medLines = mutableListOf<String>()
        if (complaint.isNotBlank()) medLines.add("Complaint: $complaint")
        if (sinceWhen.isNotBlank()) medLines.add("Since When: $sinceWhen")
        if (prevTreat.isNotBlank()) medLines.add("Previous Treatment: $prevTreat")
        if (medLines.isNotEmpty()) sections.add(PrintSection("Medical Details (at Registration)", medLines))
        val feeLines = mutableListOf<String>()
        val regFeeClean = regFee.removeSuffix(".0")
        if (regFeeClean.isNotBlank() && regFeeClean != "0") feeLines.add("Registration Fee: ₹$regFeeClean")
        if (regMode.isNotBlank()) feeLines.add("Payment Mode: $regMode")
        if (feeLines.isNotEmpty()) sections.add(PrintSection("Registration Fee", feeLines))
        val blank = "______________________________"
        sections.add(PrintSection("Doctor's Check-up (to be filled by Doctor)", listOf(
            "Examination / DRE: $blank",
            "Grade: $blank",
            "On Probing: $blank",
            "Other Findings: $blank",
            "Investigation Advised: $blank",
            "Treatment Plan: $blank",
            "Estimated Cost: $blank",
            "Recovery Time: $blank",
            "Doctor's Remarks / Advice: $blank"
        )))
        return PrintDocumentModel(
            documentTitle = "Patient Registration Form",
            branchName = branch,
            patientName = name.ifBlank { "-" },
            patientId = patientId,
            dateLabel = displayDate(regDate),
            sections = sections,
            qrPayload = if (patientId.isNotBlank()) "PID:$patientId" else null,
            footerNote = null,
            patientAddress = address,
            patientAgeSex = listOf(age, sex).filter { it.isNotBlank() }.joinToString(" / "),
            patientDisease = disease,
            patientMobile = mobile
        )
    }
    /**
     * Blood Test / Investigation print, mirroring printBlood(id): lists the
     * selected investigations (checkbox ticks) plus an optional advice/remarks
     * block. Built from a live Supabase "patients" row plus the staff's on-screen
     * test selection.
     */
    fun bloodTest(patient: JSONObject, selectedTests: List<String>, remarks: String): PrintDocumentModel {
        val branchBT = patient.s("branch")
        val name = patient.s("name")
        val patientId = patient.s("patientId")
        val mobileBT = patient.s("mobile")

        val sections = mutableListOf(
            PrintSection(
                heading = "Selected Investigation",
                lines = selectedTests.map { "✓ $it" }
            )
        )
        if (remarks.isNotBlank()) {
            sections.add(PrintSection(heading = "Advice / Remarks", lines = listOf(remarks)))
        }

        return PrintDocumentModel(
            documentTitle = "Blood Test / Investigation",
            branchName = branchBT,
            patientName = name.ifBlank { "-" },
            patientId = patientId,
            dateLabel = displayDate(visitDateOf(patient)),
            sections = sections,
            qrPayload = if (patientId.isNotBlank()) "PID:$patientId" else null,
            footerNote = null,
            patientAddress = patient.s("address"),
            patientAgeSex = listOf(patient.s("age"), patient.s("sex").ifBlank { patient.s("gender") }).filter { it.isNotBlank() }.joinToString(" / "),
            patientDisease = patient.s("disease"),
            patientMobile = mobileBT
        )
    }

    private fun visitDateOf(patient: JSONObject): String =
        patient.s("visitDate").ifBlank { patient.s("registrationDate").ifBlank { patient.s("date") } }

    /**
     * Builds a Prescription (Rx) print from a saved "medical" row (type =
     * "Prescription"). The clinical module saves each medicine's
     * name/dosage/frequency/duration into `details` (semicolon-separated) and
     * the plain names into `selected`, so a past prescription can be reprinted
     * from Print Center by mobile lookup — matching the WebView's printRx().
     */
    fun prescriptionFromMedical(patient: JSONObject, medical: JSONObject): PrintDocumentModel {
        val branch = patient.s("branch")
        val name = patient.s("name")
        val patientId = patient.s("patientId")
        val mobileRx = patient.s("mobile")
        val details = medical.s("details")
        val selected = medical.s("selected")
        val raw = when {
            details.isNotBlank() -> details.split(";")
            selected.isNotBlank() -> selected.split(",")
            else -> emptyList()
        }
        val lines = raw.map { it.trim() }.filter { it.isNotBlank() }
            .mapIndexed { i, l -> "${i + 1}. $l" }
            .ifEmpty { listOf("No medicines recorded.") }
        return PrintDocumentModel(
            documentTitle = "Prescription",
            branchName = branch,
            patientName = name.ifBlank { "-" },
            patientId = patientId,
            dateLabel = displayDate(medical.s("date")),
            sections = listOf(PrintSection("Rx", lines)),
            qrPayload = if (patientId.isNotBlank()) "PILESCLINIC|RX|$patientId" else null,
            footerNote = null,
            patientAddress = patient.s("address"),
            patientAgeSex = listOf(patient.s("age"), patient.s("sex").ifBlank { patient.s("gender") }).filter { it.isNotBlank() }.joinToString(" / "),
            patientDisease = patient.s("disease"),
            patientMobile = mobileRx
        )
    }

    /** Medicine Slip from a saved "medical" (Prescription) row — same data as the
     *  Rx, presented as a dispensing slip. */
    fun medicineSlipFromMedical(patient: JSONObject, medical: JSONObject): PrintDocumentModel {
        val base = prescriptionFromMedical(patient, medical)
        return base.copy(
            documentTitle = "Medicine Slip",
            sections = listOf(PrintSection("Medicines", base.sections.flatMap { it.lines })),
            qrPayload = if (base.patientId.isNotBlank()) "PILESCLINIC|SLIP|${base.patientId}" else null
        )
    }

    /**
     * Builds a Diet Chart print from a saved "medical" row (type = "Diet Chart").
     * The clinical module saves "category: name" items into `selected`.
     */
    fun dietFromMedical(patient: JSONObject, medical: JSONObject): PrintDocumentModel {
        val branch = patient.s("branch")
        val name = patient.s("name")
        val patientId = patient.s("patientId")
        val mobileDiet = patient.s("mobile")
        val selected = medical.s("selected")
        val lines = selected.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .map { "• $it" }
            .ifEmpty { listOf("No diet guidelines recorded.") }
        return PrintDocumentModel(
            documentTitle = "Diet Chart",
            branchName = branch,
            patientName = name.ifBlank { "-" },
            patientId = patientId,
            dateLabel = displayDate(medical.s("date")),
            sections = listOf(PrintSection("Diet", lines)),
            qrPayload = if (patientId.isNotBlank()) "PILESCLINIC|DIET|$patientId" else null,
            footerNote = null,
            patientAddress = patient.s("address"),
            patientAgeSex = listOf(patient.s("age"), patient.s("sex").ifBlank { patient.s("gender") }).filter { it.isNotBlank() }.joinToString(" / "),
            patientDisease = patient.s("disease"),
            patientMobile = mobileDiet
        )
    }
}
