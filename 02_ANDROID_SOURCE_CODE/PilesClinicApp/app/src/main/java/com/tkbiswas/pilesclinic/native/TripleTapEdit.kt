package com.tkbiswas.pilesclinic.native

import android.view.View
import android.widget.Toast

/**
 * Global "3 taps to edit" gesture — the app-wide rule that any saved value
 * (form field, list row, payment, etc.) can be corrected by tapping it three
 * times quickly. Mirrors the WebView's triple-tap-to-unlock behaviour and keeps
 * every screen consistent: attach this, and the 3rd quick tap fires [onEdit],
 * silently — no visible hint text is shown.
 *
 * Usage:  TripleTapEdit.attach(view) { openEditDialog(record) }
 *
 * Note: this sets an OnClickListener on the view, so attach it to a view that
 * doesn't already need its own single-tap action (e.g. a name/label/row).
 */
object TripleTapEdit {

    private const val WINDOW_MS = 1200L

    fun attach(view: View, onEdit: () -> Unit) {
        var count = 0
        var last = 0L
        view.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - last > WINDOW_MS) count = 0
            last = now
            count++
            if (count >= 3) { count = 0; onEdit() }
        }
    }
}
