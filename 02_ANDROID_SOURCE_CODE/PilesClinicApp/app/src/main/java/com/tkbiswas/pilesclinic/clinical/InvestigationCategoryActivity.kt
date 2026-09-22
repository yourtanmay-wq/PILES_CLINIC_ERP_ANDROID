package com.tkbiswas.pilesclinic.clinical

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/** TK APPROVED (2026-07-15): shows the checklist for exactly one Blood Test
 *  category (Hematology / Bio-Chemistry / etc.), opened by tapping a category
 *  card on InvestigationAdviceActivity. Reads/writes the same shared
 *  ClinicalRepository.currentInvestigations list, so nothing else (Approve,
 *  Save, Print, PrintMappers.investigationAdvice) needs to change. */
class InvestigationCategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val categoryName = intent.getStringExtra("category_name").orEmpty()
        val category = ClinicalRepository.investigationCategories.find { it.name == categoryName }

        // ROOT-CAUSE FIX (2026-07-15): dp() must be declared BEFORE it's used —
        // it was declared further down before, so every earlier use of dp()
        // failed to compile ("Unresolved reference: dp"). Moved to the top.
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.bg_app_gradient)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_login_hero)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val backArrow = TextView(this).apply {
            text = "←"; textSize = 20f; setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, dp(14), 0)
            isClickable = true; isFocusable = true
        }
        backArrow.setOnClickListener { finish() }
        header.addView(backArrow)
        header.addView(TextView(this).apply {
            text = category?.let { "${it.emoji}  ${it.name}" } ?: "Blood Test"
            textSize = 17f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(android.graphics.Color.WHITE)
        })
        root.addView(header)

        val editable = RoleSession.canEditClinical()

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        scroll.addView(list)
        root.addView(scroll)

        category?.tests?.forEach { testName ->
            val existing = ClinicalRepository.currentInvestigations.find { it.name == testName }
            var isChecked = existing?.isSelected == true

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedBg(if (isChecked) "#EAFBF0" else "#FFFFFF", if (isChecked) "#0B4F2A" else "#E1E6ED", dp(12))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(8); layoutParams = lp
                isClickable = editable; isFocusable = editable
            }
            val circle = TextView(this).apply {
                text = if (isChecked) "✓" else ""
                textSize = 13f; gravity = android.view.Gravity.CENTER; setTextColor(android.graphics.Color.WHITE)
                background = roundedBg(if (isChecked) "#0B4F2A" else "#FFFFFF", "#B7C0CE", dp(20))
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            }
            val label = TextView(this).apply {
                text = testName; textSize = 13.5f
                setTextColor(android.graphics.Color.parseColor(if (isChecked) "#0B4F2A" else "#10223A"))
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginStart = dp(12); layoutParams = lp
            }
            row.addView(circle); row.addView(label)
            if (editable) {
                row.setOnClickListener {
                    isChecked = !isChecked
                    if (existing != null) {
                        existing.isSelected = isChecked
                    } else {
                        ClinicalRepository.currentInvestigations.add(InvestigationEntry(name = testName, isSelected = isChecked))
                    }
                    circle.text = if (isChecked) "✓" else ""
                    circle.background = roundedBg(if (isChecked) "#0B4F2A" else "#FFFFFF", "#B7C0CE", dp(20))
                    label.setTextColor(android.graphics.Color.parseColor(if (isChecked) "#0B4F2A" else "#10223A"))
                    row.background = roundedBg(if (isChecked) "#EAFBF0" else "#FFFFFF", if (isChecked) "#0B4F2A" else "#E1E6ED", dp(12))
                }
            }
            list.addView(row)
        }

        // ══════════════════════════════════════════════════════════════════
        // 🟢🔒 V624 (২৪.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ অনুমোদিত) — "Add
        // Optional Test": তালিকায় নেই এমন টেস্ট এই সেকশনেই টাইপ করে যোগ
        // করা যায়। Parent screen (InvestigationAdviceActivity.
        // showBloodTestChecklistDialog-এর "+ Add" বাক্স)-এর হুবহু একই
        // প্রমাণিত যুক্তি — dedup, সরাসরি isSelected=true, একই
        // ClinicalRepository.currentInvestigations-এই যোগ। শুধু প্রতিটা
        // ক্যাটাগরি-পর্দায় আলাদাভাবে বসানো হলো, যাতে Common Blood Test/
        // Previous Patient পপ-আপে ফিরে না গিয়েই যোগ করা যায়।
        // ⛔ Save/Share/Print এক অক্ষরও বদলায়নি — তারা `isSelected` দেখেই
        //    কাজ করে, custom না predefined তা জানার দরকার নেই।
        // ══════════════════════════════════════════════════════════════════
        if (editable) {
            val chipWrap = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(4); layoutParams = lp
            }
            fun renderChip(entry: InvestigationEntry) {
                val chip = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(9), dp(10), dp(9))
                    background = roundedBg("#EAFBF0", "#0B4F2A", dp(20))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = dp(8); layoutParams = lp
                }
                chip.addView(TextView(this).apply {
                    text = entry.name; textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                })
                chip.addView(TextView(this).apply {
                    text = "  ✕"; textSize = 13f
                    setTextColor(android.graphics.Color.parseColor("#B42318"))
                    setPadding(dp(8), 0, 0, 0)
                    isClickable = true; isFocusable = true
                    setOnClickListener {
                        ClinicalRepository.currentInvestigations.remove(entry)
                        chipWrap.removeView(chip)
                    }
                })
                chipWrap.addView(chip)
            }
            ClinicalRepository.currentInvestigations
                .filter { it.isCustom && it.customCategory == categoryName }
                .forEach { renderChip(it) }
            list.addView(chipWrap)

            val addWrap = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg("#F4FAFF", "#93C5FD", dp(14))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.topMargin = dp(6); layoutParams = lp
            }
            addWrap.addView(TextView(this).apply {
                text = "➕ Add Optional Test"
                textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0B2B59"))
            })
            val addRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.topMargin = dp(8); layoutParams = lp
            }
            val addInput = android.widget.EditText(this).apply {
                hint = "Type a test name…"
                background = roundedBg("#FFFFFF", "#C7D6E8", dp(10))
                textSize = 13.5f
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.marginEnd = dp(8) }
            }
            val addBtn = android.widget.Button(this).apply {
                text = "ADD"
                setBackgroundResource(R.drawable.bg_btn_navy)
                setTextColor(android.graphics.Color.WHITE)
            }
            addRow.addView(addInput); addRow.addView(addBtn)
            addWrap.addView(addRow)
            list.addView(addWrap)

            addBtn.setOnClickListener {
                val name = addInput.text.toString().trim()
                if (name.isBlank()) return@setOnClickListener
                val already = (category?.tests.orEmpty().any { it.equals(name, ignoreCase = true) }) ||
                    ClinicalRepository.currentInvestigations.any { it.name.equals(name, ignoreCase = true) }
                if (already) {
                    android.widget.Toast.makeText(this, "Already in the list.", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val entry = InvestigationEntry(name = name, isSelected = true, isCustom = true, customCategory = categoryName)
                ClinicalRepository.currentInvestigations.add(entry)
                renderChip(entry)
                addInput.setText("")
            }
        }

        setContentView(root)
        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
    }

    private fun roundedBg(fill: String, stroke: String, radius: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = radius.toFloat()
            setColor(android.graphics.Color.parseColor(fill))
            setStroke(2, android.graphics.Color.parseColor(stroke))
        }
}
