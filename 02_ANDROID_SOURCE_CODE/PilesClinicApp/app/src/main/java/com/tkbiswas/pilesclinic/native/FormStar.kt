package com.tkbiswas.pilesclinic.native

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Makes the "*" on every mandatory-field label RED, on any form — without
 * changing the label text or any logic. Call once after setContentView with the
 * screen's root view; it walks the tree and recolors only the "*" characters.
 * Purely visual: no field, validation, or flow is touched.
 */
object FormStar {
    private val RED = Color.parseColor("#E53935")

    fun paint(root: View?) {
        root ?: return
        when (root) {
            is TextView -> colorStars(root)
            is ViewGroup -> for (i in 0 until root.childCount) paint(root.getChildAt(i))
        }
    }

    private fun colorStars(tv: TextView) {
        val text = tv.text?.toString() ?: return
        if (!text.contains('*')) return
        val sp = SpannableString(text)
        var i = text.indexOf('*')
        while (i >= 0) {
            sp.setSpan(ForegroundColorSpan(RED), i, i + 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            i = text.indexOf('*', i + 1)
        }
        tv.text = sp
    }
}
