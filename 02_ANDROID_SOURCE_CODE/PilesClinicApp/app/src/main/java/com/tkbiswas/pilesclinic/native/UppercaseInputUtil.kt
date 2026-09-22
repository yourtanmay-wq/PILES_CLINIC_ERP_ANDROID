package com.tkbiswas.pilesclinic.native

import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

/** TK-REQUESTED GLOBAL RULE (2026-07-24, Locked): every English text field
 *  anywhere in the app must auto-save as CAPITAL LETTERS, even if the
 *  person types lowercase -- EXCEPT fields where that would be risky
 *  (Password, where case matters for the credential itself).
 *
 *  Usage: call UppercaseInputUtil.applyToAll(binding.root) once, right
 *  after setContentView/binding in an Activity's onCreate. It walks the
 *  whole screen and attaches the filter to every EditText it finds --
 *  nothing else about that field (hint, validation, listeners, mandatory-
 *  star, etc.) is touched. Numeric-only fields (mobile, amount) are
 *  harmless no-ops since there are no letters to capitalize.
 */
object UppercaseInputUtil {

    fun applyToAll(root: View) {
        if (root is EditText) {
            val type = root.inputType
            val isPassword =
                (type and InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                (type and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                (type and InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            if (!isPassword) {
                root.filters = root.filters + InputFilter.AllCaps()
            }
        } else if (root is ViewGroup) {
            for (i in 0 until root.childCount) applyToAll(root.getChildAt(i))
        }
    }
}
