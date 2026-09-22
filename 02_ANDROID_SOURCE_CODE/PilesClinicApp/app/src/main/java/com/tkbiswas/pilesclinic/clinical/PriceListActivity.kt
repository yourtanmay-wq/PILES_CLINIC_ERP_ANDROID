package com.tkbiswas.pilesclinic.clinical

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.modules.ModuleUi
import com.tkbiswas.pilesclinic.native.PremiumAlert

/**
 * 💰🔒 V971 (০২.০৯.২০২৬, TK-নির্দেশ) — **দরের তালিকা, যে কেউ বদলাতে পারবে।**
 *
 * TK: *"একটা রেডিমেড থাকবে, সেটা যেন বদলাতে পারে"* ·
 *     *"হ্যাঁ আমি এবং যে কেউ বদলাতে পারবে"*।
 *
 * ⛔ বদল **এই ফোনেই** জমা থাকে (TK-এর নিজের সিদ্ধান্ত) — কোনো SQL লাগে না।
 * ⛔ "Reset to default" চাপলে অ্যাপের রেডিমেড দরই ফিরে আসে।
 */
class PriceListActivity : AppCompatActivity() {

    private var group = EstimatePrices.G_PILES
    private var items: MutableList<EstimatePrices.Item> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = EstimatePrices.list(this).toMutableList()
        render()
    }

    private fun dp(v: Int) = ModuleUi.dp(this, v)

    private fun box(fill: String, stroke: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(fill))
        setStroke(dp(1), Color.parseColor(stroke))
        cornerRadius = dp(radius).toFloat()
    }

    private fun render() {
        val col = ModuleUi.screen(this, "")
        col.addView(ModuleUi.heading(this, "Price List"))
        col.addView(ModuleUi.body(this, "Anyone can change  ·  saved on this phone"))

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        for (g in EstimatePrices.ALL_GROUPS) {
            val on = g == group
            tabs.addView(TextView(this).apply {
                text = g
                textSize = 11f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (on) Color.WHITE else Color.parseColor("#63748C"))
                background = box(if (on) "#0F3D6B" else "#EFF3F8", if (on) "#0F3D6B" else "#E2E9F2", 10)
                setPadding(dp(2), dp(9), dp(2), dp(9))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { leftMargin = dp(2); rightMargin = dp(2) }
                setOnClickListener { group = g; render() }
            })
        }
        col.addView(tabs)

        val card = ModuleUi.card(this)
        val mine = items.filter { it.group == group }
        if (mine.isEmpty()) card.addView(ModuleUi.body(this, "Nothing here yet."))
        for (item in mine) card.addView(rowFor(item))
        col.addView(card)

        col.addView(ModuleUi.button(this, "+ Add new item") { addOrEdit(null) }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })
        col.addView(ModuleUi.buttonSoft(this, "Reset to default") {
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "⚠️ Are you sure?"))
                .setMessage("Bring back the app's own price list? Your changes will be lost.")
                .setPositiveButton("Yes") { _, _ ->
                    EstimatePrices.reset(this)
                    items = EstimatePrices.list(this).toMutableList()
                    ModuleUi.toast(this, "Price list reset")
                    render()
                }
                .setNegativeButton("No", null)
                .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        })
    }

    private fun rowFor(item: EstimatePrices.Item): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
            setOnClickListener { addOrEdit(item) }
        }
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = item.name
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#101C2E"))
        })
        info.addView(TextView(this).apply {
            /* 🔢 V1251 (TK-নির্দেশ, ফটো-প্রুফ পাশ) — নিজে থেকে বসা সংখ্যাটা
               নামের নিচেই দেখা যায়, তাই খুলে না দেখেও বোঝা যায়।
               ⛔ ১ হলে লেখা হয় না — আগের সারিগুলো হুবহু আগের মতোই থাকে। */
            val q = EstimatePrices.qtyOf(item)
            // 📏 V1278 — একক CM-এ দেখানো (পুরনো "inch" লেখা সারিতেও)
            text = EstimateModel.unitTxt(item.unit) + (if (item.measure.isBlank()) "" else "  ·  " + EstimateModel.unitTxt(item.measure)) +
                (if (q == 1.0) "" else "  ·  Qty " + EstimateModel.moneyShort(q))
            textSize = 11f
            setTextColor(Color.parseColor("#8B98A9"))
        })
        row.addView(info)
        row.addView(TextView(this).apply {
            text = EstimateModel.moneyShort(item.rate)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B4F2A"))
            setPadding(dp(8), 0, dp(10), 0)
        })
        row.addView(TextView(this).apply {
            text = "🗑"
            textSize = 15f
            setOnClickListener {
                AlertDialog.Builder(this@PriceListActivity)
                    .setCustomTitle(PremiumAlert.header(this@PriceListActivity, "⚠️ Are you sure?"))
                    .setMessage("Remove " + item.name + " from the price list?")
                    .setPositiveButton("Yes") { _, _ ->
                        items.remove(item)
                        EstimatePrices.save(this@PriceListActivity, items)
                        render()
                    }
                    .setNegativeButton("No", null)
                    .show().also { d -> try { PremiumAlert.paint(d) } catch (_: Throwable) { } }
            }
        })
        return row
    }

    /** নতুন সারি বা পুরনোটার বদল — নাম · দর · একক · মাপ। */
    private fun addOrEdit(existing: EstimatePrices.Item?) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(4))
        }
        fun field(hint: String, value: String, number: Boolean): EditText =
            EditText(this).apply {
                setText(value)
                this.hint = hint
                textSize = 14f
                setTextColor(Color.parseColor("#101C2E"))
                background = box("#F8FBFE", "#D6E1EE", 10)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                /* 🔒 খাতার সারি B411 — উপরের একই কারণ। */
                if (number) {
                    inputType = InputType.TYPE_CLASS_TEXT
                    keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
                }
            }
        val name = field("Name", existing?.name.orEmpty(), false)
        val rate = field("Rate", if (existing == null) "" else EstimateModel.moneyShort(existing.rate), true)
        val unit = field("Unit (per position / per inch / per day)", existing?.unit.orEmpty(), false)
        val measure = field("Grade or measure (optional)", existing?.measure.orEmpty(), false)
        /* 🔢🔒 V1251 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK: *"কোয়ান্টিটি
           আমি যা সেট করে রাখবো সেটাই থাকার কথা ছিল … আমি যেটা সেট করে রাখবো
           সেটাই যেন অটোমেটিক ডিফল্ট হিসেবে থাকে"* (খাতার সারি ৩৭২)।
           **আগে এই ঘরটাই ছিল না** — তাই এস্টিমেটে সবসময় ১ বসত।
           ⛔ ফাঁকা রাখলে আগের মতোই ১, তাই পুরনো কোনো জিনিস বদলায় না। */
        val qty = field("Default quantity", 
            if (existing == null || EstimatePrices.qtyOf(existing) == 1.0) ""
            else EstimateModel.moneyShort(existing.qty), true)
        for (f in listOf(name, rate, unit, measure, qty)) {
            root.addView(f.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(7) }
            })
        }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, if (existing == null) "➕ New item" else "✏️ Edit item"))
            .setView(root)
            .setPositiveButton("Save") { _, _ ->
                val n = name.text?.toString()?.trim().orEmpty()
                if (n.isBlank()) { ModuleUi.toast(this, "Name is required"); return@setPositiveButton }
                val fresh = EstimatePrices.Item(
                    group = group,
                    name = n,
                    rate = EstimateModel.num(rate.text?.toString()),
                    unit = unit.text?.toString()?.trim().orEmpty(),
                    measure = measure.text?.toString()?.trim().orEmpty(),
                    // 🔢 V1251 — ফাঁকা বা ০ হলে ১ (আগের আচরণ)।
                    qty = EstimateModel.num(qty.text?.toString()).let { q -> if (q > 0.0) q else 1.0 }
                )
                if (existing == null) items.add(fresh)
                else {
                    val at = items.indexOf(existing)
                    if (at >= 0) items[at] = fresh else items.add(fresh)
                }
                EstimatePrices.save(this, items)
                render()
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }
}
