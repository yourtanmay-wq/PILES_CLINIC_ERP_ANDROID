package com.tkbiswas.pilesclinic.print

import com.tkbiswas.pilesclinic.clinical.ClinicalRepository
import com.tkbiswas.pilesclinic.clinical.RoleSession
import com.tkbiswas.pilesclinic.clinical.PrescriptionOptionsStore
import com.tkbiswas.pilesclinic.data.local.RegistrationEntity
import com.tkbiswas.pilesclinic.native.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintMappers {

    // 🔒 খাতার সারি B75 (TK, 29.07.2026): আগে এখানে `Locale.getDefault()` ছিল —
    // অর্থাৎ ফোনের ভাষা যা, তারিখ সেই নিয়মে। কারো ফোনের ভাষা বাংলা/হিন্দি হলে
    // ছাপা কাগজে তারিখ বাংলা অঙ্কে (২৯.০৭.২০২৬) উঠতে পারত। TK-এর স্থায়ী নিয়ম:
    // ⛔ ছাপায় কখনো বাংলা নয় (একমাত্র Diet Chart ছাড়া), আর তারিখ সবসময়
    // 31.12.2026 ধাঁচে। তাই `Locale.US` — অ্যাপের বাকি সব জায়গার মতোই।
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private fun now() = DateUtil.displayWithTime(Date())

    private fun patientAgeSex(): String = listOf(
        RoleSession.currentPatientAge.trim(),
        RoleSession.currentPatientSex.trim()
    ).filter { it.isNotBlank() }.joinToString(" / ")


    fun registration(entity: RegistrationEntity): PrintDocumentModel {
        val details = listOfNotNull(
            "Registration No: ${entity.regNo}",
            "Age / Gender: ${entity.age?.toString() ?: "-"} / ${entity.gender ?: "-"}",
            "Phone: ${entity.phone}",
            entity.address?.takeIf { it.isNotBlank() }?.let { "Address: $it" },
            "Registration Date: ${dateFormat.format(Date(entity.registrationDate))}",
            entity.referredBy?.takeIf { it.isNotBlank() }?.let { "Referred By: $it" }
        )
        val notes = entity.notes?.takeIf { it.isNotBlank() }

        return PrintDocumentModel(
            documentTitle = "Patient Registration Slip",
            patientName = entity.patientName,
            patientId = entity.id.take(8).uppercase(),
            dateLabel = dateFormat.format(Date(entity.registrationDate)),
            sections = buildList {
                add(PrintSection("Patient Details", details))
                if (notes != null) add(PrintSection("Notes", listOf(notes)))
            },
            qrPayload = "PILESCLINIC|REG|${entity.id}|${entity.regNo}",
            patientAddress = entity.address.orEmpty(),
            patientAgeSex = "${entity.age?.toString() ?: "-"} / ${entity.gender ?: "-"}",
            patientDisease = "-",
            patientMobile = entity.phone
        )
    }

    fun prescription(context: android.content.Context): PrintDocumentModel {
        val medicines = ClinicalRepository.currentPrescription
        val lines = if (medicines.isEmpty()) {
            listOf("No medicines in the current prescription.")
        } else {
            medicines.map { m ->
                val parts = listOfNotNull(
                    m.dosage.takeIf { it.isNotBlank() },
                    m.frequency.takeIf { it.isNotBlank() },
                    m.duration.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                val instructionPart = if (m.instructions.isNotBlank()) "  [${m.instructions}]" else ""
                (if (parts.isNotBlank()) parts else "As advised") + instructionPart
            }
        }
        val rxTypes = if (medicines.isEmpty()) null else medicines.map { it.medicineType }
        val rxNames = if (medicines.isEmpty()) null else medicines.map { it.name.ifBlank { "(unnamed medicine)" } }
        // TK APPROVED (2026-07-15): SL./MEDICINE NAME/DOSE/WHEN/DURATION table
        // print -- real separate fields exist here (live session), so pass them
        // through instead of only the combined `lines` string.
        val rxDosage = if (medicines.isEmpty()) null else medicines.map { it.dosage.ifBlank { "-" } }
        /* 🔵 V548 (২২.০৮.২০২৬, TK: *"মেডিসিন When-এর ঘর ফাঁকা কেন থাকবে"*):
           সেভ করা লেখায় When না থাকলে ছাপার সময় প্রজেক্টের নিজের আদত When বসে।
           ⛔ তালিকায় নেই এমন ওষুধে আগের মতোই "-" — কিছুই বানানো হয় না। */
        val rxFrequency = if (medicines.isEmpty()) null else medicines.map {
            it.frequency.ifBlank { com.tkbiswas.pilesclinic.clinical.ClinicalRepository.rxWhenFor(it.name) }.ifBlank { "-" }
        }
        val rxDuration = if (medicines.isEmpty()) null else medicines.map { it.duration.ifBlank { "-" } }
        /* 💊🔒 V723 — ডাক্তারের লেখা Instruction (যেমন "WITH LUKEWARM WATER")
           এখন ছাপাতেও যায়; নামের নিচে ছোট হরফে বসে। ⛔ ফাঁকা হলে কিছুই বসে না। */
        val rxInstructions = if (medicines.isEmpty()) null else medicines.map { it.instructions.trim() }
        return PrintDocumentModel(
            documentTitle = "Prescription",
            branchName = RoleSession.currentPatientBranch,
            patientName = RoleSession.currentPatientName,
            // 🔒 খাতার সারি B175: ছাপায় দেখানো Patient ID এখন মানুষ-পড়া-যায়
            // কোডটাই (থাকলে) — QR-এ raw আইডিই থাকছে (কিছু ভাঙে না)।
            patientId = RoleSession.displayId(),
            dateLabel = now(),
            // TK FIX (2026-07-15): section heading was "Rx" while the PDF already
            // draws a large "℞" rx-symbol at the same spot -> looked like "Rx" printed
            // twice. Heading removed; the ℞ symbol alone marks the medicine list.
            sections = listOf(PrintSection(null, lines, rxTypes, rxNames, rxDosage, rxFrequency, rxDuration, rxInstructions)),   // 💊 V723
            qrPayload = "PILESCLINIC|RX|${RoleSession.currentPatientId}|${System.currentTimeMillis()}",
            patientAddress = RoleSession.currentPatientAddress,
            patientAgeSex = patientAgeSex(),
            patientDisease = RoleSession.currentPatientDisease,
            complaintHistory = PrescriptionOptionsStore.printLines(context),
            prescriptionDiet = PrescriptionOptionsStore.diet(context),
            // 🔵 V488: Sitz Bath-এর টিক তোলা থাকলে ছাপাতেও থাকবে না।
            prescriptionSitzBath = PrescriptionOptionsStore.sitzBath(context),
            patientMobile = RoleSession.currentPatientMobile
        )
    }

    /** 🖨️🔒 V833 — `context` **ঐচ্ছিক** (ডিফল্ট null) ⇒ পুরনো কোনো ডাক ভাঙে না;
     *  না দিলে বাঁ কলাম আগের মতোই ফাঁকা যায়। */
    fun medicineSlip(context: android.content.Context? = null): PrintDocumentModel {
        val medicines = ClinicalRepository.currentSlip
        val lines = if (medicines.isEmpty()) {
            listOf("No medicines in the current slip.")
        } else {
            medicines.map { m ->
                val parts = listOfNotNull(
                    m.dosage.takeIf { it.isNotBlank() },
                    m.frequency.takeIf { it.isNotBlank() },
                    m.duration.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (parts.isNotBlank()) parts else "As advised"
            }
        }
        val rxTypes = if (medicines.isEmpty()) null else medicines.map { it.medicineType }
        val rxNames = if (medicines.isEmpty()) null else medicines.map { it.name.ifBlank { "(unnamed medicine)" } }
        val rxDosage = if (medicines.isEmpty()) null else medicines.map { it.dosage.ifBlank { "-" } }
        /* 🔵 V548 (২২.০৮.২০২৬, TK: *"মেডিসিন When-এর ঘর ফাঁকা কেন থাকবে"*):
           সেভ করা লেখায় When না থাকলে ছাপার সময় প্রজেক্টের নিজের আদত When বসে।
           ⛔ তালিকায় নেই এমন ওষুধে আগের মতোই "-" — কিছুই বানানো হয় না। */
        val rxFrequency = if (medicines.isEmpty()) null else medicines.map {
            it.frequency.ifBlank { com.tkbiswas.pilesclinic.clinical.ClinicalRepository.rxWhenFor(it.name) }.ifBlank { "-" }
        }
        val rxDuration = if (medicines.isEmpty()) null else medicines.map { it.duration.ifBlank { "-" } }
        /* 💊🔒 V723 — ডাক্তারের লেখা Instruction (যেমন "WITH LUKEWARM WATER")
           এখন ছাপাতেও যায়; নামের নিচে ছোট হরফে বসে। ⛔ ফাঁকা হলে কিছুই বসে না। */
        val rxInstructions = if (medicines.isEmpty()) null else medicines.map { it.instructions.trim() }
        return PrintDocumentModel(
            documentTitle = "Medicine Slip",
            branchName = RoleSession.currentPatientBranch,
            patientName = RoleSession.currentPatientName,
            // 🔒 খাতার সারি B175: ছাপায় দেখানো Patient ID এখন মানুষ-পড়া-যায়
            // কোডটাই (থাকলে) — QR-এ raw আইডিই থাকছে (কিছু ভাঙে না)।
            patientId = RoleSession.displayId(),
            dateLabel = now(),
            sections = listOf(PrintSection(null, lines, rxTypes, rxNames, rxDosage, rxFrequency, rxDuration, rxInstructions)),   // 💊 V723
            qrPayload = "PILESCLINIC|SLIP|${RoleSession.currentPatientId}|${System.currentTimeMillis()}",
            footerNote = "Please follow dosage exactly as advised by the doctor.",
            patientAddress = RoleSession.currentPatientAddress,
            patientAgeSex = patientAgeSex(),
            patientDisease = RoleSession.currentPatientDisease,
            /* 🖨️🔒 V833 (TK-নির্দেশ) — Medicine Slip-এও এখন বাঁ কলামে
               DISEASE · SYMPTOMS · DURATION · CHIEF COMPLAINT, ঠিক
               Prescription-এর মতোই। ⛔ ঘরগুলোর তথ্য **আগে থেকেই জমা**
               (ডাক্তারের চেক-আপ থেকে `syncFromCloud`), তাই নতুন কোনো
               ক্লাউড-পড়া লাগেনি — Egress বাড়ে না। */
            complaintHistory = if (context != null)
                com.tkbiswas.pilesclinic.clinical.PrescriptionOptionsStore.printLinesForSlip(context)
            else emptyList(),
            patientMobile = RoleSession.currentPatientMobile
        )
    }

    /** 🔴 V430 (TK-সিদ্ধান্ত ১৮.০৮.২০২৬) — নতুন ঐচ্ছিক `remarks`। খালি রাখলে
     *  কাগজ **হুবহু আগের মতোই** ছাপে (ডিফল্ট ""), তাই পুরনো কোনো কল ভাঙে না। */
    fun investigationAdvice(remarks: String = ""): PrintDocumentModel {
        // TK-REQUESTED CHANGE (2026-07-16): the Doctor-approval step was
        // removed from the screen (staff can do this without it), so the
        // print no longer splits into Advised/Requested or mentions
        // "pending doctor approval" — just a plain list of selected tests.
        val tests = ClinicalRepository.currentInvestigations.filter { it.isSelected }
        val lines = tests.map { "• ${it.name}" }
        val sections = mutableListOf<PrintSection>()
        if (lines.isNotEmpty()) sections.add(PrintSection("Tests", lines))
        if (sections.isEmpty()) sections.add(PrintSection(null, listOf("No tests selected yet.")))
        // 🔴 V430 — ডাক্তারের লেখা বাড়তি পরামর্শ, লেখা থাকলে তবেই।
        if (remarks.isNotBlank()) sections.add(PrintSection("Advice / Remarks", listOf(remarks.trim())))

        return PrintDocumentModel(
            documentTitle = "Blood Test / Investigation Advice",
            branchName = RoleSession.currentPatientBranch,
            patientName = RoleSession.currentPatientName,
            // 🔒 খাতার সারি B175: ছাপায় দেখানো Patient ID এখন মানুষ-পড়া-যায়
            // কোডটাই (থাকলে) — QR-এ raw আইডিই থাকছে (কিছু ভাঙে না)।
            patientId = RoleSession.displayId(),
            dateLabel = now(),
            sections = sections,
            qrPayload = "PILESCLINIC|INV|${RoleSession.currentPatientId}|${System.currentTimeMillis()}",
            patientAddress = RoleSession.currentPatientAddress,
            patientAgeSex = patientAgeSex(),
            patientDisease = RoleSession.currentPatientDisease,
            patientMobile = RoleSession.currentPatientMobile
        )
    }

    fun dietChart(): PrintDocumentModel {
        val selected = ClinicalRepository.currentDiet.filter { it.isSelected }
        val allowed = selected.filter { it.category == "Allowed" }.map { "✓ ${it.name}" }
        val avoid = selected.filter { it.category == "Avoid" }.map { "✗ ${it.name}" }
        val sections = mutableListOf<PrintSection>()
        if (allowed.isNotEmpty()) sections.add(PrintSection("Allowed", allowed))
        if (avoid.isNotEmpty()) sections.add(PrintSection("Avoid", avoid))
        if (sections.isEmpty()) sections.add(PrintSection(null, listOf("No diet guidelines selected yet.")))

        return PrintDocumentModel(
            documentTitle = "Diet Chart",
            branchName = RoleSession.currentPatientBranch,
            patientName = RoleSession.currentPatientName,
            // 🔒 খাতার সারি B175: ছাপায় দেখানো Patient ID এখন মানুষ-পড়া-যায়
            // কোডটাই (থাকলে) — QR-এ raw আইডিই থাকছে (কিছু ভাঙে না)।
            patientId = RoleSession.displayId(),
            dateLabel = now(),
            sections = sections,
            qrPayload = "PILESCLINIC|DIET|${RoleSession.currentPatientId}|${System.currentTimeMillis()}",
            patientAddress = RoleSession.currentPatientAddress,
            patientAgeSex = patientAgeSex(),
            patientDisease = RoleSession.currentPatientDisease,
            patientMobile = RoleSession.currentPatientMobile
        )
    }
}
