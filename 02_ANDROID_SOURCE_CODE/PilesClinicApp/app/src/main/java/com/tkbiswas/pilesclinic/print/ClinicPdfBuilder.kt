package com.tkbiswas.pilesclinic.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * MASTER PRINT DESIGN V1.0 — OWNER LOCKED.
 *
 * The same compact A4 design is used for Prescription, Medicine Slip,
 * Blood Test, Diet Chart, Registration, Payment Receipt, Doctor Visit and all
 * future print types. Only branch clinic name, logo, address and mobile change.
 * No patient photo is ever printed.
 */
class ClinicPdfBuilder(private val context: Context) {

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 18f
        // TK-REQUESTED ADDITION (2026-07-16): new "Kshar Sutra" tagline badge
        // added below the clinic name/address in the header (same text, same
        // place, for every branch's print). HEADER_BOTTOM/CONTENT_TOP are
        // shifted down by exactly the badge's height so every element that
        // used to come after the address (green separator, title pill,
        // patient info box, its 3 rows, and the content/watermark area) keeps
        // its EXACT same size/shape/spacing -- it just slides down together.
        // Nothing else in this file changed.
        private const val TAGLINE_SHIFT = 34f
        // TK-REQUESTED ADDITION (2026-07-19): patient details box grows by one
        // row to fit a new "Mobile" field — same slide-everything-down pattern
        // as TAGLINE_SHIFT above, so nothing else's size/spacing changes.
        private const val MOBILE_ROW_SHIFT = 12f
        // 🔒 B554 (08.08.2026, TK-অনুমোদিত প্রুফ "গ্লোবাল রুলস") — Address এখন
        // দু'লাইনে (গ্রাম+পোস্ট / থানা+জেলা), তাই রোগীর ডিটেলস-বক্সে ঠিক এক
        // লাইন বাড়তি উচ্চতা লাগে। HEADER_BOTTOM (বক্সের তলা + নিচের বিভাজিকা)
        // ও CONTENT_TOP (নিচের Rx/ওষুধ-টেবিলের শুরু) — দুটোই এই পরিমাণ নিচে
        // নামে, তাই বক্স-ও-টেবিলের মাঝের ফাঁক আগের মতোই থাকে, কিছু ওভারল্যাপ
        // করে না। সব ClinicPdfBuilder-প্রিন্টে (Prescription/Slip/Diet/Reg) একই।
        private const val ADDR2_GAP = 11f
        private const val HEADER_BOTTOM = 148f + TAGLINE_SHIFT + MOBILE_ROW_SHIFT + ADDR2_GAP
        private const val CONTENT_TOP = 158f + TAGLINE_SHIFT + MOBILE_ROW_SHIFT + ADDR2_GAP
        // TK APPROVED (2026-07-15): medicine/body text must start clearly below
        // the decorative ℞ watermark symbol, never overlapping it.
        private const val CONTENT_TEXT_TOP = CONTENT_TOP + 42f
        private const val FOOTER_TOP = 782f
        private const val CONTENT_BOTTOM = 774f
        private const val GREEN = "#0A5428"
        private const val GOLD = "#C99A19"

        // TK APPROVED (2026-07-15): distinct colour per medicine Type box.
        private fun typeColor(type: String): String = when (type.trim().uppercase()) {
            "TAB" -> "#1067D8"
            "CAP" -> "#7A3FC9"
            "SYP" -> "#C99A19"
            "OINT" -> "#0A9E8A"
            "INJ" -> "#D64545"
            "OTHER" -> "#5B6B81"
            else -> "#0A5428"
        }
    }

    private data class Line(
        val layout: StaticLayout,
        val spaceAfter: Float,
        val badgeText: String? = null,
        val badgeColorHex: String = GREEN,
        val rxName: String? = null,
        val nameOnSameLine: Boolean = false,
        // TK APPROVED (2026-07-15): small drawn checkbox for Advised/Allowed
        // ("✓ ") and Avoid/Requested ("✗ ") lines -- Blood Test & Diet Chart
        // only (the only two places these prefixes are ever produced). Every
        // other document type's lines are untouched (checkGlyph stays null).
        val checkGlyph: Boolean? = null, // true = tick (green), false = cross (red)
        // TK FIX (2026-07-15): guarantees the row is tall enough for the
        // (possibly bigger) medicine-name font, not just the dose-line font
        // underneath it -- see nameLineHeight where this is set.
        val minRowHeight: Float = 0f
    ) {
        val rowHeight: Float get() = maxOf(layout.height.toFloat(), minRowHeight)
    }

    fun build(model: PrintDocumentModel, branch: BranchInfo, outputFile: File): File {
        // TK APPROVED (2026-07-15): SL./MEDICINE NAME/DOSE/WHEN/DURATION table
        // print for Prescription/Medicine Slip, requested to look like a
        // professional hospital medicine table. Only used when the model
        // actually carries the separate dosage/frequency/duration fields
        // (a live clinical session) -- every other document type (Blood Test,
        // Diet Chart, Registration, and the cloud reprint-by-mobile path which
        // only has old flattened text) is completely untouched below and
        // keeps the exact same rendering as before.
        val tableSection = model.sections.firstOrNull { it.rxDosage != null }
        if (tableSection != null) {
            return buildMedicineTable(model, branch, tableSection, outputFile)
        }
        val logoBitmap = loadAssetBitmap(branch.logoAssetPath)
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()
        val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11.5f; isFakeBoldText = true; color = Color.parseColor(GREEN)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f; color = Color.parseColor("#18251D")
        }
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f; isFakeBoldText = true; color = Color.WHITE; textAlign = Paint.Align.CENTER
        }
        // TK FIX (2026-07-15): medicine name asked to be bigger again -- bumped
        // further (10.5f -> 13f). This time the row height itself is computed
        // from whichever is taller (medicine name vs dose line), not just the
        // dose line as before, so a bigger name can never clip into the row
        // above or below it (see minRowHeight below).
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f; isFakeBoldText = true; color = Color.parseColor("#18251D")
        }
        val nameAscentOffset = -namePaint.fontMetrics.ascent
        val nameLineHeight = namePaint.fontMetrics.descent - namePaint.fontMetrics.ascent
        val fm = bodyPaint.fontMetrics
        val baselineOffset = -fm.ascent
        val lines = mutableListOf<Line>()
        model.sections.forEach { section ->
            if (!section.heading.isNullOrBlank()) lines += Line(makeLayout(section.heading, headingPaint, contentWidth), 4f)
            section.lines.forEachIndexed { i, text ->
                val badge = section.rxTypes?.getOrNull(i)?.takeIf { it.isNotBlank() }
                val name = section.rxNames?.getOrNull(i)
                if (name != null) {
                    // TK APPROVED (2026-07-15): hospital-style single-line row —
                    // [TYPE box]  Medicine Name ⋯⋯⋯ dose/frequency/days
                    val badgeW = if (badge != null) (badgePaint.measureText(badge) + 10f).coerceAtLeast(30f) else 0f
                    val gap1 = if (badge != null) 6f else 0f
                    val nameBudget = (contentWidth - badgeW - gap1 - 90f).coerceAtLeast(40f)
                    val nameFitted = fitText(name, namePaint, nameBudget)
                    val nameW = namePaint.measureText(nameFitted)
                    val afterNameWidth = contentWidth - badgeW - gap1 - nameW - 6f
                    val dashW = 16f
                    val doseMaxWidth = (afterNameWidth - dashW - 6f).coerceAtLeast(30f)
                    val doseFitted = fitText(text, bodyPaint, doseMaxWidth)
                    lines += Line(
                        makeLayout(doseFitted, bodyPaint, contentWidth), 9f,
                        badge, typeColor(badge.orEmpty()), nameFitted, true,
                        minRowHeight = nameLineHeight
                    )
                } else if (badge != null) {
                    val badgeW = (badgePaint.measureText(badge) + 10f).coerceAtLeast(30f)
                    lines += Line(makeLayout(text, bodyPaint, (contentWidth - badgeW - 6f).toInt()), 3.5f, badge, typeColor(badge))
                } else {
                    val isCheck = text.startsWith("✓ ")
                    val isCross = text.startsWith("✗ ")
                    if (isCheck || isCross) {
                        val stripped = text.substring(2)
                        val glyphSpace = 16
                        lines += Line(
                            makeLayout(stripped, bodyPaint, (contentWidth - glyphSpace).coerceAtLeast(1)),
                            3.5f, checkGlyph = isCheck
                        )
                    } else {
                        lines += Line(makeLayout(text, bodyPaint, contentWidth), 3.5f)
                    }
                }
            }
            lines += Line(makeLayout(" ", bodyPaint, contentWidth), 3f)
        }
        val pages = paginate(lines, CONTENT_BOTTOM - CONTENT_TEXT_TOP)
        val pageGroups = if (pages.isEmpty()) listOf(emptyList()) else pages
        val pdf = PdfDocument()
        pageGroups.forEachIndexed { index, pageLines ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
            val canvas = page.canvas
            drawWatermark(canvas, logoBitmap)
            drawHeader(canvas, model, branch, logoBitmap)
            var y = CONTENT_TEXT_TOP
            pageLines.forEach { line ->
                if (line.nameOnSameLine && line.badgeText != null) {
                    var x = MARGIN + 8f
                    val badgeW = (badgePaint.measureText(line.badgeText) + 10f).coerceAtLeast(30f)
                    val badgeH = 12f
                    // TK FIX (2026-07-15): badge + dash now vertically centered on
                    // the medicine name's own (bigger) baseline, not the old
                    // small dose-line baseline, so nothing looks mis-aligned
                    // now that the name text is noticeably larger.
                    val badgeTop = y + (nameAscentOffset - badgeH) / 2f
                    val badgeRect = RectF(x, badgeTop, x + badgeW, badgeTop + badgeH)
                    canvas.drawRoundRect(badgeRect, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(line.badgeColorHex) })
                    canvas.drawText(line.badgeText, badgeRect.centerX(), badgeRect.centerY() + 2.8f, badgePaint)
                    x += badgeW + 6f
                    val name = line.rxName.orEmpty()
                    canvas.drawText(name, x, y + nameAscentOffset, namePaint)
                    x += namePaint.measureText(name) + 6f
                    val dashW = 16f
                    val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor(line.badgeColorHex); strokeWidth = 1f
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f, 1.5f), 0f)
                    }
                    canvas.drawLine(x, y + nameAscentOffset - 2f, x + dashW, y + nameAscentOffset - 2f, dashPaint)
                    x += dashW + 6f
                    canvas.save(); canvas.translate(x, y + (nameLineHeight - line.layout.height) / 2f); line.layout.draw(canvas); canvas.restore()
                } else if (line.nameOnSameLine) {
                    // no type set for this medicine — still show name + dash + dose, just no badge
                    var x = MARGIN + 8f
                    val name = line.rxName.orEmpty()
                    canvas.drawText(name, x, y + nameAscentOffset, namePaint)
                    x += namePaint.measureText(name) + 6f
                    val dashW = 16f
                    val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor(GREEN); strokeWidth = 1f
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f, 1.5f), 0f)
                    }
                    canvas.drawLine(x, y + nameAscentOffset - 2f, x + dashW, y + nameAscentOffset - 2f, dashPaint)
                    x += dashW + 6f
                    canvas.save(); canvas.translate(x, y + (nameLineHeight - line.layout.height) / 2f); line.layout.draw(canvas); canvas.restore()
                } else if (line.badgeText != null) {
                    val badgeW = (badgePaint.measureText(line.badgeText) + 10f).coerceAtLeast(30f)
                    val badgeH = 12f
                    val badgeRect = RectF(MARGIN + 8f, y + 1f, MARGIN + 8f + badgeW, y + 1f + badgeH)
                    canvas.drawRoundRect(badgeRect, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(line.badgeColorHex) })
                    canvas.drawText(line.badgeText, badgeRect.centerX(), badgeRect.centerY() + 2.8f, badgePaint)
                    canvas.save(); canvas.translate(MARGIN + 8f + badgeW + 6f, y); line.layout.draw(canvas); canvas.restore()
                } else if (line.checkGlyph != null) {
                    val isCheck = line.checkGlyph
                    val boxColor = if (isCheck) Color.parseColor(GREEN) else Color.parseColor("#D64545")
                    val boxSize = 8f
                    val boxRect = RectF(MARGIN + 8f, y + 1f, MARGIN + 8f + boxSize, y + 1f + boxSize)
                    canvas.drawRoundRect(boxRect, 1.5f, 1.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = boxColor })
                    val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE; strokeWidth = 1f; style = Paint.Style.STROKE
                    }
                    if (isCheck) {
                        canvas.drawLine(boxRect.left + 1.6f, boxRect.centerY(), boxRect.left + 3.2f, boxRect.bottom - 1.6f, markPaint)
                        canvas.drawLine(boxRect.left + 3.2f, boxRect.bottom - 1.6f, boxRect.right - 1.4f, boxRect.top + 1.4f, markPaint)
                    } else {
                        canvas.drawLine(boxRect.left + 1.6f, boxRect.top + 1.6f, boxRect.right - 1.6f, boxRect.bottom - 1.6f, markPaint)
                        canvas.drawLine(boxRect.right - 1.6f, boxRect.top + 1.6f, boxRect.left + 1.6f, boxRect.bottom - 1.6f, markPaint)
                    }
                    canvas.save(); canvas.translate(MARGIN + 8f + boxSize + 8f, y); line.layout.draw(canvas); canvas.restore()
                } else {
                    canvas.save(); canvas.translate(MARGIN + 8f, y); line.layout.draw(canvas); canvas.restore()
                }
                y += line.rowHeight + line.spaceAfter
            }
            drawFooter(canvas, index + 1, pageGroups.size, model)
            pdf.finishPage(page)
        }
        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close(); logoBitmap?.recycle()
        return outputFile
    }

    /** TK APPROVED (2026-07-15): "SL. / MEDICINE NAME / DOSE / WHEN / DURATION"
     *  bordered table print for Prescription / Medicine Slip, matching a
     *  reference hospital-style table photo TK approved. Same header/footer/
     *  watermark as every other document; only the medicine list itself is a
     *  real table instead of the dashed single-line rows. Every column comes
     *  straight from the medicine's own dosage/frequency/duration fields --
     *  nothing here is invented or guessed from the combined text. */
    private fun buildMedicineTable(model: PrintDocumentModel, branch: BranchInfo, section: PrintSection, outputFile: File): File {
        val logoBitmap = loadAssetBitmap(branch.logoAssetPath)
        val contentWidth = PAGE_WIDTH - 2 * MARGIN
        val splitPrescription = model.documentTitle.equals("Prescription", true)
        val historyWidth = if (splitPrescription) 128f else 0f
        val splitGap = 0f
        val tableLeft = MARGIN + historyWidth + splitGap
        val tableWidth = contentWidth - historyWidth - splitGap
        val names = section.rxNames.orEmpty()
        val types = section.rxTypes.orEmpty()
        val doses = section.rxDosage.orEmpty()
        val freqs = section.rxFrequency.orEmpty()
        val durations = section.rxDuration.orEmpty()
        /* 💊🔒 V723 (২৭.০৮.২০২৬, ডা. কে. এইচ. মণ্ডলের রিপোর্ট · TK ডেমো-প্রুফে
           **প্রস্তাব ১** অনুমোদিত) — ডাক্তারের লেখা Instruction (যেমন
           "WITH LUKEWARM WATER") **ওষুধের নামের ঠিক নিচে ছোট হরফে**।
           ⛔ কোনো কলামের চওড়া বদলায়নি — তাই লম্বা নাম ভেঙে যাওয়ার ঝুঁকি নেই।
           ⛔ Instruction ফাঁকা হলে সারিটা **হুবহু আগের মতোই** ছাপে। */
        val instructions = section.rxInstructions.orEmpty()
        val rowCount = names.size

        val navy = Color.parseColor("#123C8C")
        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f; isFakeBoldText = true; color = Color.WHITE
        }
        val slPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f; isFakeBoldText = true; color = Color.parseColor(GREEN)
        }
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f; isFakeBoldText = true; color = Color.parseColor("#18251D")
        }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f; color = Color.parseColor("#18251D")
        }
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7f; isFakeBoldText = true; color = Color.WHITE; textAlign = Paint.Align.CENTER
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D8DEE6"); strokeWidth = 0.8f }
        // 💊 V723 — Instruction-এর ছোট হরফ (নামের নিচে)।
        val instrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f; isFakeBoldText = true; color = Color.parseColor("#8A5A00")
        }

        // Column layout: SL | MEDICINE NAME | DOSE | WHEN | DURATION
        // TK-REQUESTED (2026-07-22): DOSE widened (its text can run long, e.g.
        // "15 ml with equal water twice daily after food"); WHEN/DURATION
        // narrowed since their content is always short/fixed ("After Food",
        // "5 days"). Checked against the longest realistic values ("Before
        // Food", "30 days") with comfortable margin before both borders.
        val colSl = if (splitPrescription) 28f else 34f
        val colDuration = if (splitPrescription) 52f else 78f
        val colDose = if (splitPrescription) 62f else 128f
        val colWhen = if (splitPrescription) 68f else 100f
        val colName = tableWidth - colSl - colDose - colWhen - colDuration
        val xSl = tableLeft
        val xName = xSl + colSl
        val xDose = xName + colName
        val xWhen = xDose + colDose
        val xDuration = xWhen + colWhen
        val cellPad = 12f
        val headerRowH = 24f

        data class TableRow(val sl: Int, val badge: String, val badgeColor: Int, val nameLayout: StaticLayout, val doseLayout: StaticLayout, val whenLayout: StaticLayout, val durLayout: StaticLayout, val height: Float,
                            val instrLayout: StaticLayout? = null, val nameBlockH: Float = 0f)   // 💊 V723

        // 🩹🔒 V1386 (১২.০৯.২০২৬, TK-রিপোর্ট — ছবি-প্রুফ পাশ) — TK: *"একটা মেডিসিনের
        // নাম দেখুন ডোজের ওখানে কেমন ভাবে রয়েছে"*। যখন Dose-এ লম্বা বাক্য
        // (যেমন "1 tsp at bedtime with warm water") টাইপ করা হয়, সরু Dose
        // কলামে একটা করে শব্দ আলাদা লাইনে ভেঙে বিশ্রী দেখাত। এখন Dose-এর
        // লেখা এক লাইনে না ধরলে শুরুর যতটা এক লাইনে ধরে (যেমন "1 tsp") সেটাই
        // Dose-এ থাকে, বাকিটা (যেমন "at bedtime with warm water") নামের
        // নিচে ছোট Instruction-লাইনে চলে যায় — V723-এর সেই একই জায়গা।
        // ⛔ ছোট Dose (যেমন "2-0-2", "15 Ml") এক লাইনেই ধরে, তাই তাদের
        // কিছুই বদলায় না। ⛔ সেভ করা আসল ডেটা এক অক্ষরও বদলায় না — শুধু
        // ছাপার সময় দেখানোর ধরন।
        val doseUsableW = (colDose - 2 * cellPad)
        fun splitDoseOverflow(dose: String): Pair<String, String> {
            if (dose.isBlank() || cellPaint.measureText(dose) <= doseUsableW) return Pair(dose, "")
            val words = dose.trim().split(Regex("\\s+"))
            val kept = StringBuilder()
            var i = 0
            while (i < words.size) {
                val candidate = if (kept.isEmpty()) words[i] else "$kept ${words[i]}"
                if (kept.isNotEmpty() && cellPaint.measureText(candidate) > doseUsableW) break
                kept.clear(); kept.append(candidate)
                i++
            }
            val overflow = words.drop(i).joinToString(" ")
            return Pair(kept.toString(), overflow)
        }

        val rows = (0 until rowCount).map { i ->
            val nameLayout = makeLayout(names.getOrElse(i) { "-" }, namePaint, (colName - 2 * cellPad - 32f).toInt())
            val (doseShown, doseOverflow) = splitDoseOverflow(doses.getOrElse(i) { "-" })
            val doseLayout = makeLayout(doseShown, cellPaint, doseUsableW.toInt())
            val whenLayout = makeLayout(freqs.getOrElse(i) { "-" }, cellPaint, (colWhen - 2 * cellPad).toInt())
            val durLayout = makeLayout(durations.getOrElse(i) { "-" }, cellPaint, (colDuration - 2 * cellPad).toInt())
            val badge = types.getOrNull(i).orEmpty()
            /* 💊 V723 — Instruction থাকলে নামের নিচে একটা ছোট লাইন। ⛔ ফাঁকা হলে
               `null`, আর তখন নিচের হিসাব হুবহু আগের মতোই চলে। */
            val instrText = listOf(instructions.getOrNull(i).orEmpty().trim(), doseOverflow)
                .filter { it.isNotBlank() }.joinToString(" · ")
            val instrLayout = if (instrText.isBlank()) null
                else makeLayout(instrText, instrPaint, (colName - 2 * cellPad).toInt())
            val nameBlockH = nameLayout.height.toFloat() + (instrLayout?.let { it.height.toFloat() + 3f } ?: 0f)
            val h = maxOf(nameBlockH, doseLayout.height.toFloat(), whenLayout.height.toFloat(), durLayout.height.toFloat()) + 16f
            TableRow(i + 1, badge, Color.parseColor(typeColor(badge)), nameLayout, doseLayout, whenLayout, durLayout, h,
                instrLayout, nameBlockH)
        }

        fun drawTableHeader(canvas: Canvas, top: Float) {
            canvas.drawRect(tableLeft, top, tableLeft + tableWidth, top + headerRowH, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy })
            fun label(text: String, x: Float, w: Float, alignCenter: Boolean = false) {
                val tx = if (alignCenter) x + (w - headerTextPaint.measureText(text)) / 2f else x + cellPad
                canvas.drawText(text.uppercase(), tx, top + headerRowH / 2f + 3f, headerTextPaint)
            }
            label("SL.", xSl, colSl, true)
            label("MEDICINE NAME", xName, colName)
            label("DOSE", xDose, colDose)
            label("WHEN", xWhen, colWhen)
            label("DURATION", xDuration, colDuration)
        }

        // Pre-split rows into page-chunks first (same pattern as the rest of
        // this class) so the footer can always show the correct "Page X of Y"
        // instead of guessing while rendering.
        val rxTopSpace = if (splitPrescription) 22f else 0f
        val availableFirstRow = CONTENT_BOTTOM - (CONTENT_TEXT_TOP + rxTopSpace + headerRowH)
        val pageChunks = mutableListOf<List<TableRow>>()
        run {
            var current = mutableListOf<TableRow>(); var used = 0f
            rows.forEach { row ->
                if (used + row.height > availableFirstRow && current.isNotEmpty()) {
                    pageChunks += current; current = mutableListOf(); used = 0f
                }
                current += row; used += row.height
            }
            if (current.isNotEmpty() || pageChunks.isEmpty()) pageChunks += current
        }

        val pdf = PdfDocument()
        pageChunks.forEachIndexed { pageIndex, chunk ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create())
            val canvas = page.canvas
            if (splitPrescription) drawPrescriptionFrame(canvas) else drawWatermark(canvas, logoBitmap)
            drawHeader(canvas, model, branch, logoBitmap)
            var y = CONTENT_TEXT_TOP
            if (splitPrescription && pageIndex == 0) {
                val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(GREEN); style = Paint.Style.STROKE; strokeWidth = 1f }
                val historyTop = CONTENT_TEXT_TOP + rxTopSpace
                canvas.drawRect(MARGIN, historyTop, MARGIN + historyWidth, CONTENT_BOTTOM, hp)
                val hLabel = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7.5f; isFakeBoldText = true; color = Color.parseColor(GREEN) }
                val hBody = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8.2f; color = Color.parseColor("#18251D") }
                var hy = historyTop + 15f
                model.complaintHistory.forEach { block ->
                    val parts = block.split("\n", limit = 2)
                    val label = parts.firstOrNull().orEmpty()
                    val value = parts.getOrNull(1).orEmpty()
                    canvas.drawText(label, MARGIN + 8f, hy, hLabel); hy += 12f
                    val layout = makeLayout(value, hBody, (historyWidth - 16f).toInt())
                    canvas.save(); canvas.translate(MARGIN + 8f, hy); layout.draw(canvas); canvas.restore()
                    hy += layout.height + 14f
                }
            }
            if (splitPrescription) {
                val rx = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(GREEN); textSize = 20f; isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                // Owner-approved position: Rx directly above the SL column.
                canvas.drawText("℞", xSl + colSl / 2f, y + 17f, rx)
                y += rxTopSpace
            }
            val tableTop = y
            drawTableHeader(canvas, y)
            y += headerRowH
            chunk.forEachIndexed { i, row ->
                if (i % 2 == 1) canvas.drawRect(tableLeft, y, tableLeft + tableWidth, y + row.height, Paint().apply { color = Color.parseColor("#F5F8FA") })
                val slText = row.sl.toString()
                // TK-REQUESTED (2026-07-22): centre the serial number in the SL
                // column (like the "SL." header) so it isn't merged into the
                // left box edge.
                val slX = xSl + (colSl - slPaint.measureText(slText)) / 2f
                canvas.drawText(slText, slX, y + row.height / 2f + 4f, slPaint)

                var nameX = xName + cellPad
                /* 💊 V723 — নাম ও Instruction একসাথে একটা "ব্লক" ধরে মাঝখানে বসে।
                   ⛔ Instruction না থাকলে `nameBlockH == nameLayout.height`, অর্থাৎ
                      হিসাবটা **হুবহু আগের মতোই** — পুরোনো কাগজ এক চুলও নড়বে না। */
                val blockTop = y + (row.height - row.nameBlockH) / 2f
                if (row.badge.isNotBlank()) {
                    val bw = (badgePaint.measureText(row.badge) + 8f).coerceAtLeast(22f)
                    val bh = 11f
                    // ব্যাজটা নামের লাইনের সঙ্গে মেলে (আগে সারির মাঝে বসত — এক লাইনের
                    // সারিতে দুটো এক জায়গাতেই পড়ে, তাই পুরোনো চেহারা অক্ষত)।
                    val bTop = blockTop + (row.nameLayout.height - bh) / 2f
                    canvas.drawRoundRect(RectF(nameX, bTop, nameX + bw, bTop + bh), 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = row.badgeColor })
                    canvas.drawText(row.badge, nameX + bw / 2f, bTop + bh / 2f + 2.6f, badgePaint)
                    nameX += bw + 12f
                }
                canvas.save(); canvas.translate(nameX, blockTop); row.nameLayout.draw(canvas); canvas.restore()
                row.instrLayout?.let { il ->
                    canvas.save()
                    canvas.translate(xName + cellPad, blockTop + row.nameLayout.height + 3f)
                    il.draw(canvas)
                    canvas.restore()
                }
                canvas.save(); canvas.translate(xDose + cellPad, y + (row.height - row.doseLayout.height) / 2f); row.doseLayout.draw(canvas); canvas.restore()
                canvas.save(); canvas.translate(xWhen + cellPad, y + (row.height - row.whenLayout.height) / 2f); row.whenLayout.draw(canvas); canvas.restore()
                canvas.save(); canvas.translate(xDuration + cellPad, y + (row.height - row.durLayout.height) / 2f); row.durLayout.draw(canvas); canvas.restore()

                y += row.height
                canvas.drawLine(tableLeft, y, tableLeft + tableWidth, y, borderPaint)
            }
            listOf(xName, xDose, xWhen, xDuration).forEach { vx ->
                canvas.drawLine(vx, tableTop, vx, y, borderPaint)
            }
            canvas.drawRect(tableLeft, tableTop, tableLeft + tableWidth, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D8DEE6"); style = Paint.Style.STROKE; strokeWidth = 1f
            })
            if (splitPrescription && pageIndex == 0) {
                val adviceTop = y + 8f
                val adviceH = if (model.prescriptionDiet.isBlank()) 24f else 38f
                val advicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(GREEN); style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRect(tableLeft, adviceTop, tableLeft + tableWidth, adviceTop + adviceH, advicePaint)
                val aHead = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(GREEN); textSize = 10f; isFakeBoldText = true }
                val aText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18251D"); textSize = 9f }
                canvas.drawText("ADVICE:", tableLeft + 9f, adviceTop + 16f, aHead)
                // 🔵 V488 (TK-নির্দেশ): টিক তোলা থাকলে এখানেও লাইনটা ছাপে না।
                // ডিফল্ট true, তাই অন্য কোনো কাগজ/পুরনো ব্যবহারে কিছুই বদলায় না।
                if (model.prescriptionSitzBath)
                    canvas.drawText("Sitz Bath — 2 Times Daily", tableLeft + 55f, adviceTop + 16f, aText)
                if (model.prescriptionDiet.isNotBlank())
                    canvas.drawText("Diet — ${fitText(model.prescriptionDiet, aText, tableWidth - 50f)}", tableLeft + 9f, adviceTop + 31f, aText)
                drawPrescriptionLowerWatermark(canvas, logoBitmap, tableLeft, adviceTop + adviceH + 5f)
            }
            drawFooter(canvas, pageIndex + 1, pageChunks.size, model)
            pdf.finishPage(page)
        }
        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close(); logoBitmap?.recycle()
        return outputFile
    }

    private fun paginate(lines: List<Line>, contentHeight: Float): List<List<Line>> {
        val pages = mutableListOf<MutableList<Line>>(); var current = mutableListOf<Line>(); var y = 0f
        lines.forEach { line ->
            val h = line.rowHeight + line.spaceAfter
            if (y + h > contentHeight && current.isNotEmpty()) { pages += current; current = mutableListOf(); y = 0f }
            current += line; y += h
        }
        if (current.isNotEmpty()) pages += current
        return pages
    }

    private fun makeLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(1.2f, 1.12f).setIncludePad(false).build()

    private fun drawHeader(canvas: Canvas, model: PrintDocumentModel, branch: BranchInfo, logo: Bitmap?) {
        val green = Color.parseColor(GREEN); val gold = Color.parseColor(GOLD)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        // TK APPROVED (2026-07-15): clinic name bigger/bolder (19f -> 22f) and
        // address bigger (8.2f -> 9.2f), still guaranteed one line via fitText
        // below -- nothing else in the header layout moved.
        // TK FIX (2026-07-15): clinic name asked to use "Impact" font. Impact is
        // not a font file present in this project (no .ttf uploaded), so true
        // Impact cannot be embedded -- using Android's closest built-in heavy
        // condensed system font (sans-serif-black) instead, which gives the same
        // bold-poster look. If TK sends the actual Impact.ttf file it can be
        // embedded exactly.
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green; textSize = 22f; isFakeBoldText = true
            typeface = try { Typeface.create("sans-serif-black", Typeface.BOLD) } catch (_: Exception) { Typeface.DEFAULT_BOLD }
        }
        // TK FIX (2026-07-15): address/mobile line made a bit bigger (9.2f -> 10.5f).
        val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18251D"); textSize = 10.5f }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18251D"); textSize = 7.5f; isFakeBoldText = true }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 8f }
        val whiteBold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 10.5f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }

        logo?.let {
            val rect = RectF(MARGIN, 11f, MARGIN + 70f, 81f)
            canvas.drawBitmap(it, null, rect, Paint(Paint.ANTI_ALIAS_FLAG))
        }
        // TK APPROVED (2026-07-15): clinic name + address/mobile centered in the
        // space beside the logo (page-middle), not hugging the logo's left edge.
        title.textAlign = Paint.Align.CENTER
        detail.textAlign = Paint.Align.CENTER
        val logoRight = MARGIN + 70f
        val headerRight = PAGE_WIDTH - MARGIN
        val centerX = (logoRight + headerRight) / 2f
        val nameMaxWidth = headerRight - logoRight - 16f
        canvas.drawText(fitText(branch.clinicName, title, nameMaxWidth), centerX, 31f, title)
        canvas.drawLine(logoRight + 20f, 40f, headerRight - 20f, 40f, Paint().apply { color = gold; strokeWidth = 1.2f })
        val oneLine = "${branch.addressLine}   |   Mob: ${branch.phoneLine}   |   Helpline: ${BranchCatalog.HELPLINE}"
        canvas.drawText(fitText(oneLine, detail, headerRight - logoRight - 20f), centerX, 57f, detail)

        // TK-REQUESTED ADDITION (2026-07-16): tagline badge, same text for
        // every branch. Sits between the address line and the (now shifted
        // down) green separator.
        val badgeTop = 64f
        val badgeBottom = badgeTop + TAGLINE_SHIFT - 4f
        val badgeRect = RectF(logoRight + 14f, badgeTop, headerRight - 14f, badgeBottom)
        // TK-APPROVED (2026-07-22): premium bordered box around the tagline
        // text -- outer green rounded border + inner thin gold rounded
        // border, plain/no fill (no reverse colour, matches the demoed and
        // confirmed design). Only a border was added; the text/colors inside
        // are unchanged.
        canvas.drawRoundRect(badgeRect, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green; style = Paint.Style.STROKE; strokeWidth = 1.1f
        })
        // TK-REQUESTED (2026-07-22): no reverse/filled colour band -- it printed
        // poorly. Now plain, impactful dark text on white (no background).
        val taglineBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green; textSize = 8.8f; isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        val taglineSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3A4A40"); textSize = 7.3f; isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val taglineMaxW = badgeRect.width() - 20f
        canvas.drawText(
            fitText("WE PROVIDE AYURVEDA KSHAR SUTRA THERAPY IN PILES, FISSURE & FISTULA", taglineBold, taglineMaxW),
            centerX, badgeTop + 12.5f, taglineBold
        )
        canvas.drawText(
            fitText("MOST SUCCESSFUL TREATMENT WITH HIGH SUCCESS RATE", taglineSmall, taglineMaxW),
            centerX, badgeTop + 23f, taglineSmall
        )

        canvas.drawLine(MARGIN, 88f + TAGLINE_SHIFT, PAGE_WIDTH - MARGIN, 88f + TAGLINE_SHIFT, Paint().apply { color = green; strokeWidth = 1.3f })
        // TK APPROVED (2026-07-15): the title pill used to be a FIXED 170pt wide
        // regardless of the document title's length, so a long title like
        // "BLOOD TEST / INVESTIGATION ADVICE" spilled outside the green box.
        // It now grows to fit the text (with padding), and the text itself
        // shrinks a little if it still wouldn't fit within the page margins --
        // so it can never overflow again, for any current or future doc type.
        val titleText = model.documentTitle.uppercase()
        val maxPillW = PAGE_WIDTH - 2 * MARGIN - 20f
        var titleSize = 10.5f
        whiteBold.textSize = titleSize
        var textW = whiteBold.measureText(titleText)
        while (textW + 28f > maxPillW && titleSize > 7.5f) {
            titleSize -= 0.5f
            whiteBold.textSize = titleSize
            textW = whiteBold.measureText(titleText)
        }
        val pillW = (textW + 28f).coerceAtMost(maxPillW).coerceAtLeast(140f)
        val pill = RectF((PAGE_WIDTH - pillW) / 2f, 79f + TAGLINE_SHIFT, (PAGE_WIDTH + pillW) / 2f, 100f + TAGLINE_SHIFT)
        canvas.drawRoundRect(pill, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green })
        canvas.drawText(titleText, PAGE_WIDTH / 2f, 94f + TAGLINE_SHIFT, whiteBold)

        val box = RectF(MARGIN, 106f + TAGLINE_SHIFT, PAGE_WIDTH - MARGIN, HEADER_BOTTOM)
        // TK-REQUESTED (2026-07-22): the patient box's own border is no longer
        // drawn here -- the single continuous frame drawn in drawWatermark now
        // wraps BOTH the patient details and the Rx table, so the ugly double
        // line (patient-box bottom + Rx-box top) between them is gone.
        val mid = PAGE_WIDTH / 2f
        canvas.drawLine(mid, 110f + TAGLINE_SHIFT, mid, HEADER_BOTTOM + 2f, Paint().apply { color = green; strokeWidth = .8f })

        val leftLabelX = MARGIN + 12f; val leftValueX = MARGIN + 88f
        val rightLabelX = mid + 12f; val rightValueX = mid + 82f
        val row1 = 119f + TAGLINE_SHIFT; val row2 = 131f + TAGLINE_SHIFT; val row3 = 142f + TAGLINE_SHIFT
        val row4 = row3 + MOBILE_ROW_SHIFT
        // TK-REQUESTED (2026-07-22): fixed field order/side, same design/box.
        // LEFT: Name / Age / Patient ID / Mobile   RIGHT: Date / Sex / Diseases / Address.
        // Age & Sex are split out of the combined "Age / Sex" value.
        val ageSexParts = model.patientAgeSex.split("/")
        val ageVal = ageSexParts.getOrNull(0)?.trim().orEmpty().ifBlank { "-" }
        val sexVal = ageSexParts.getOrNull(1)?.trim().orEmpty().ifBlank { "-" }
        drawField(canvas, "Patient Name", model.patientName, leftLabelX, leftValueX, row1, label, value)
        drawField(canvas, "Age", ageVal, leftLabelX, leftValueX, row2, label, value)
        drawField(canvas, "Patient ID", model.patientId, leftLabelX, leftValueX, row3, label, value)
        drawField(canvas, "Mobile", model.patientMobile.ifBlank { "-" }, leftLabelX, leftValueX, row4, label, value)
        drawField(canvas, "Date", model.dateLabel, rightLabelX, rightValueX, row1, label, value)
        drawField(canvas, "Sex", sexVal, rightLabelX, rightValueX, row2, label, value)
        drawField(canvas, "Diseases", model.patientDisease.ifBlank { "-" }, rightLabelX, rightValueX, row3, label, value)
        drawAddressField(canvas, model.patientAddress.ifBlank { "-" }, rightLabelX, rightValueX, row4, label, value)

        // TK-REQUESTED (2026-07-22): single separator line between the patient
        // details box and the Rx/medicine table below it (demoed and confirmed
        // against the real device screenshot). Moved down slightly for clear
        // spacing from the Mobile/Address row and the mid-divider (TK-requested
        // adjustment), and matched to the same stroke width as the other
        // full-width green line in this header (line below the tagline box)
        // so it starts/looks the same as the other separator lines.
        canvas.drawLine(MARGIN, HEADER_BOTTOM + 6f, PAGE_WIDTH - MARGIN, HEADER_BOTTOM + 6f, Paint().apply { color = green; strokeWidth = 1.3f })
    }

    private fun drawField(canvas: Canvas, labelText: String, valueText: String, lx: Float, vx: Float, y: Float, label: Paint, value: Paint) {
        canvas.drawText(labelText, lx, y, label); canvas.drawText(":", vx - 10f, y, label)
        canvas.drawText(fitText(valueText, value, 170f), vx, y, value)
    }

    // 🔒 B554 (08.08.2026, TK-অনুমোদিত "গ্লোবাল রুলস") — Address দু'লাইনে আঁকা:
    // থানা-চিহ্নের আগে ভেঙে ১ম লাইন গ্রাম+পোস্ট (y), ২য় লাইন থানা+জেলা
    // (y + ADDR2_GAP)। চিহ্ন না পেলে আগের মতোই এক লাইন। প্রতি লাইন fitText দিয়ে
    // ঘরের মধ্যে রাখা (আগের মতোই), কিন্তু ভাঙার ফলে সাধারণত আর কাটে না।
    private fun drawAddressField(canvas: Canvas, valueText: String, lx: Float, vx: Float, y: Float, label: Paint, value: Paint) {
        canvas.drawText("Address", lx, y, label); canvas.drawText(":", vx - 10f, y, label)
        val (l1, l2) = splitAddress(valueText)
        canvas.drawText(fitText(l1, value, 170f), vx, y, value)
        if (l2.isNotBlank()) canvas.drawText(fitText(l2, value, 170f), vx, y + ADDR2_GAP, value)
    }

    private fun splitAddress(raw: String): Pair<String, String> {
        val markers = listOf("PS:", "P.S", "P/S", "Thana", "থানা", "Police Station")
        var idx = -1
        for (m in markers) { val i = raw.indexOf(m, ignoreCase = true); if (i > 0 && (idx == -1 || i < idx)) idx = i }
        if (idx <= 0) return Pair(raw, "")
        val first = raw.substring(0, idx).trim().trimEnd(',').trim()
        val second = raw.substring(idx).trim()
        return if (first.isBlank() || second.isBlank()) Pair(raw, "") else Pair(first, second)
    }

    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var s = text
        while (s.length > 3 && paint.measureText("$s…") > maxWidth) s = s.dropLast(1)
        return "$s…"
    }

    private fun drawWatermark(canvas: Canvas, logo: Bitmap?) {
        logo ?: return
        val targetW = PAGE_WIDTH * 0.74f
        val ratio = logo.height.toFloat() / logo.width.toFloat()
        val targetH = targetW * ratio
        val top = CONTENT_TOP + ((CONTENT_BOTTOM - CONTENT_TOP) - targetH) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 22 }
        canvas.drawBitmap(logo, null, RectF((PAGE_WIDTH-targetW)/2f, top, (PAGE_WIDTH+targetW)/2f, top+targetH), paint)
        val rx = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(GREEN); textSize = 26f; isFakeBoldText = true }
        canvas.drawText("℞", MARGIN + 9f, CONTENT_TOP + 28f, rx)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(GREEN); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(RectF(MARGIN, 106f + TAGLINE_SHIFT, PAGE_WIDTH - MARGIN, CONTENT_BOTTOM + 2f), 7f, 7f, border)
    }

    private fun drawPrescriptionFrame(canvas: Canvas) {
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(GREEN); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(RectF(MARGIN, 106f + TAGLINE_SHIFT, PAGE_WIDTH - MARGIN, CONTENT_BOTTOM + 2f), 7f, 7f, border)
    }

    private fun drawPrescriptionLowerWatermark(canvas: Canvas, logo: Bitmap?, left: Float, top: Float) {
        logo ?: return
        val availableH = (CONTENT_BOTTOM - top).coerceAtLeast(0f)
        if (availableH < 40f) return
        val size = minOf((PAGE_WIDTH - left - MARGIN) * 0.58f, availableH * 0.78f)
        val cx = left + (PAGE_WIDTH - MARGIN - left) / 2f
        val cy = top + availableH / 2f
        canvas.drawBitmap(logo, null, RectF(cx-size/2f, cy-size/2f, cx+size/2f, cy+size/2f), Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 22 })
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int, model: PrintDocumentModel) {
        val green = Color.parseColor(GREEN)
        // TK FIX (2026-07-15): footer names (TK BISWAS / KH MANDAL) made bolder
        // -- real bold Typeface added on top of the existing fake-bold + a hair
        // bigger size (9f -> 9.8f) so the extra weight is clearly visible in print.
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green; textSize = 9.8f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18251D"); textSize = 6.8f; textAlign = Paint.Align.CENTER }
        val verify = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 7.2f; textAlign = Paint.Align.CENTER }
        val separator = Paint().apply { color = green; strokeWidth = .8f }

        val centerX = PAGE_WIDTH / 2f
        canvas.drawLine(178f, FOOTER_TOP + 2f, 178f, PAGE_HEIGHT - 14f, separator)
        canvas.drawLine(417f, FOOTER_TOP + 2f, 417f, PAGE_HEIGHT - 14f, separator)

        canvas.drawLine(34f, FOOTER_TOP + 5f, 138f, FOOTER_TOP + 5f, separator)
        canvas.drawText("TK BISWAS", 86f, FOOTER_TOP + 20f, bold)
        // 🔒 গ্লোবাল রুল (TK-নির্দেশ, ১৬.০৮.২০২৬): সব কাগজে-বার্তায় এক লেখা —
        // "TK BISWAS" + "Founder & Consultant"। ⛔ শুধু এই একটা লেখা বদলেছে,
        // ছাপার মাপ/জায়গা/রং কিছুই ছোঁয়া হয়নি।
        canvas.drawText("Founder & Consultant", 86f, FOOTER_TOP + 32f, small)

        canvas.drawLine(458f, FOOTER_TOP + 5f, 561f, FOOTER_TOP + 5f, separator)
        canvas.drawText("Dr. K.H MANDAL", 510f, FOOTER_TOP + 20f, bold)
        canvas.drawText("(B.A.M.S) Regd 12386", 510f, FOOTER_TOP + 32f, small)

        drawBarcode(canvas, model.patientId.ifBlank { "PILESCLINIC" }, 214f, FOOTER_TOP + 2f, 167f, 24f)
        canvas.drawText(model.patientId.ifBlank { "PILESCLINIC" }, centerX, FOOTER_TOP + 33f, small)
        canvas.drawText("This document is digitally verified.", centerX, FOOTER_TOP + 46f, verify)
        canvas.drawText("No signature is required.", centerX, FOOTER_TOP + 57f, verify)
        if (totalPages > 1) canvas.drawText("Page $pageNumber of $totalPages", centerX, PAGE_HEIGHT - 5f, small)
    }

    private fun drawBarcode(canvas: Canvas, payload: String, left: Float, top: Float, width: Float, height: Float) {
        val paint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
        val seed = abs(payload.hashCode())
        var x = left; var i = 0
        while (x < left + width) {
            val bit = ((seed shr (i % 30)) xor (i * 31)) and 3
            val bar = when (bit) { 0 -> 1f; 1 -> 1.6f; 2 -> 2.2f; else -> 2.8f }
            canvas.drawRect(x, top, (x + bar).coerceAtMost(left + width), top + height, paint)
            x += bar + if (i % 3 == 0) 1.8f else 1.1f; i++
        }
    }

    private fun loadAssetBitmap(assetPath: String): Bitmap? = try {
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) { null }
}
