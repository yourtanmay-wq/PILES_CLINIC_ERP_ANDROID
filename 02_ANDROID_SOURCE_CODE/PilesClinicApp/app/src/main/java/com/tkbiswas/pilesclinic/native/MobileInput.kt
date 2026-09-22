package com.tkbiswas.pilesclinic.native

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText

/**
 * Shared mobile-number input for every phone field (Enquiry, Registration,
 * Payment search, Print pickers, etc.).
 *
 * Rules (locked with TK):
 *  - Field always holds ONLY plain digits, max 10. "+91" is never inside the
 *    field — it is added on save via normalized() -> "+91XXXXXXXXXX".
 *  - PASTE anything ("+918001080080", "91 80010 80080", "8001080080") -> the
 *    last 10 digits (country code stripped).
 *  - TYPING an 11th digit is blocked: the number STAYS at the first 10, it does
 *    not shift. So a correct 10-digit number can't be silently changed.
 *  - BACKSPACE deletes digits cleanly (no stray "+91"/prefix left behind).
 */
object MobileInput {

    fun attach(et: EditText) {
        et.inputType = InputType.TYPE_CLASS_PHONE
        // No number/contact suggestions or autofill on any mobile field (TK rule):
        // stop the keyboard's personalized/clipboard number chips and the system
        // autofill dropdown from suggesting phone numbers.
        // 🔴🔒 V1505 (15.09.2026) — this flag was removed from NoAutofill.harden()
        // (it was silently blocking Gboard voice typing app-wide) but is kept
        // HERE deliberately: this is a phone-number keyboard, which never shows
        // a voice/mic button anyway, and this is the exact field type TK's
        // original complaint (V758) was about.
        et.imeOptions = et.imeOptions or 0x1000000 // EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            et.importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            et.setAutofillHints(null as String?)
        }
        et.addTextChangedListener(object : TextWatcher {
            private var editing = false
            private var added = 0
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // How many characters were just inserted (1 = typing, many = paste).
                added = count
            }
            override fun afterTextChanged(s: Editable) {
                if (editing) return
                val allDigits = s.toString().filter { it.isDigit() }
                val fixed = when {
                    allDigits.length <= 10 -> allDigits
                    // Bulk change (paste) -> treat extra leading digits as a country
                    // code and keep the LAST 10.
                    added > 1 -> allDigits.takeLast(10)
                    // Single-char over-type -> block it: keep the FIRST 10 (stay).
                    else -> allDigits.take(10)
                }
                if (s.toString() != fixed) {
                    editing = true
                    et.setText(fixed)
                    et.setSelection(fixed.length)
                    editing = false
                }
            }
        })
    }

    /** Last-10-digit form, matching the WebView's mob(). */
    fun digits(et: EditText): String = et.text.toString().filter { it.isDigit() }.takeLast(10)

    fun digits(raw: String): String = raw.filter { it.isDigit() }.takeLast(10)

    fun isValid(et: EditText): Boolean = digits(et).length == 10

    /** "+91XXXXXXXXXX" when valid, else the raw digits -- matching normMob(). */
    fun normalized(et: EditText): String {
        val d = digits(et)
        return if (d.length == 10) "+91$d" else d
    }
}
