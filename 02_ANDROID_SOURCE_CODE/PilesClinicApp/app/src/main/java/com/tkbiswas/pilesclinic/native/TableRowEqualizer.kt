package com.tkbiswas.pilesclinic.native

import android.view.View
import android.view.ViewGroup

/**
 * 🔒 TK-LOCKED RULE — "সব সারির উচ্চতা এক" (কাজের খাতার (ঞ) অংশ, সারি ৮ ·
 * লক নোট V142 · আবার নিশ্চিত করা 28.07.2026)।
 *
 * The "Full Journey" table (Date/Time | Type / By | Note) must show every row
 * AND every box inside every row at exactly the same height — as tall as the
 * tallest row. TK has had to report this twice, so the logic now lives in ONE
 * place and every screen that draws that table calls this. Nobody should ever
 * copy the loop into a screen again.
 *
 * WHY IT KEPT LOOKING WRONG: making a row taller is not enough. Each of the
 * three cells is WRAP_CONTENT, so a taller row just left blank space under the
 * shorter cell and its border stopped short — the row was equal, the BOXES were
 * not. This raises the cells too.
 *
 * WHY THE CELLS CANNOT SIMPLY BE MATCH_PARENT: that was tried on 2026-07-25 and
 * it clipped the staff name ("COB-UTTAMA" was cut off in TK's photo proof).
 * Measuring with WRAP_CONTENT first and only RAISING the height afterwards
 * keeps both promises — equal boxes and nothing ever cut off.
 *
 * SAFETY: heights are only ever increased (the tallest row is by definition at
 * least as tall as every cell), column widths are never touched, and if
 * anything at all goes wrong the table is simply left as it was.
 */
object TableRowEqualizer {

    fun equalize(anchor: View, rows: List<ViewGroup>) {
        if (rows.isEmpty()) return
        anchor.post {
            try {
                var tallest = 0
                for (r in rows) if (r.height > tallest) tallest = r.height
                if (tallest <= 0) return@post
                for (r in rows) {
                    if (r.minimumHeight != tallest) r.minimumHeight = tallest
                    for (c in 0 until r.childCount) {
                        val cell = r.getChildAt(c) ?: continue
                        val lp = cell.layoutParams ?: continue
                        if (lp.height != tallest) {
                            lp.height = tallest
                            cell.layoutParams = lp
                        }
                    }
                    r.requestLayout()
                }
            } catch (_: Throwable) { }
        }
    }
}
