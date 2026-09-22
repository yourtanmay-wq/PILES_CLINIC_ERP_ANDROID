package com.tkbiswas.pilesclinic.native

import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast

/**
 * Locks a Spinner so it cannot be changed by accident. It stays locked and a
 * single tap only shows a hint; three quick taps unlock it and open the list.
 * After a new value is picked it re-locks automatically. Used for Branch (all
 * forms) and "Call Received By" (Enquiry) — locked with TK.
 */
object SpinnerLock {

    private const val WINDOW_MS = 1200L

    fun attach(sp: Spinner, label: String) {
        var taps = 0
        var last = 0L
        var unlocked = false

        sp.setOnTouchListener { v, e ->
            if (unlocked) return@setOnTouchListener false
            if (e.actionMasked == MotionEvent.ACTION_UP) {
                val now = System.currentTimeMillis()
                if (now - last > WINDOW_MS) taps = 0
                last = now
                taps++
                if (taps >= 3) {
                    taps = 0
                    unlocked = true
                    v.performClick()
                }
            }
            true // swallow touches while locked so it can't open on a single tap
        }

        // Re-lock as soon as a value is chosen (or on the initial/default selection).
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                unlocked = false
                taps = 0
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
