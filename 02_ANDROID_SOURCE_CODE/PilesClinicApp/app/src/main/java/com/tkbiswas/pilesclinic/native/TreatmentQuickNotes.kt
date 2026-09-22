package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 🟢🔒 V588 (23.08.2026, TK-নির্দেশ, ছবিসহ) — *"এখানে চাপ দিলে আজকে পেশেন্ট কে
 * কি করা হল সাজেশন বক্স এবং হাতে লেখা থাকবে · যদিও প্রজেক্টে অন্য সেকশনে আছে
 * সেখান থেকে তুলে আনবেন এখানের জন্য"*
 *
 * **আসল সমস্যা:** চিকিৎসার কথা লেখার বাক্স অ্যাপে **চারটে** জায়গায় খোলে —
 *   ১. চেম্বার বোর্ডের TREATMENT PROGRESS ঘরে চাপ (`writeTreatment`)
 *   ২. চেম্বার বন্ধ করার সময় "ফাঁকা আছে" বলে যেটা খোলে (`showRemarkDialog`)
 *   ৩. Review পর্দার ঘরে তিন-চাপ (`editRemarkInReview`)
 *   ৪. Report Card-এর Progress (`ReportCardActivity.editProgress`)
 * এর মধ্যে **১ আর ৪-এ ৯টা সাজেশন-চিপ ছিল, ২ আর ৩-এ ছিল না** — তাই TK যখন
 * চেম্বার বন্ধ করতে গিয়ে বাক্সটা পেলেন, শুধু ফাঁকা ঘর দেখলেন।
 *
 * **সমাধান:** তালিকা আর চিপ বানানোর কাজটা এখন **একটাই জায়গায়** (এই ফাইল),
 * চারটে বাক্সই এখান থেকে নেয়। ⛔ লেখাগুলো হুবহু আগের সেই ৯টাই — একটা অক্ষরও
 * বদলায়নি, তাই পুরনো লেখা/ছাপা (`PrintTextEnglish`-এর বাংলা→ইংরেজি তালিকা)
 * আগের মতোই মেলে। ⛔ হাতে লেখার ঘরটা আগের মতোই থাকে — চিপ শুধু **যোগ** করে।
 */
object TreatmentQuickNotes {

    /** 🔒 B137 অক্ষত: সাধারণ সব স্টাফের চিপ বাংলাতেই। */
    private val QUICK_BN = listOf(
        "CHECK-UP করা হলো", "KTA করা হল", "DRESSING করা হল",
        "KSHAR SUTRA করা হল", "KSHAR SUTRA ক্লিয়ার করা হল", "MEDICINE দেওয়া হল",
        "TEST করতে পাঠানো হল", "MACHINE এর কাজ করা হল", "LIS করা হল",
        "Visit Return করা হল"   // 🟢🔒 V615 (২৪.০৮.২০২৬, TK-নির্দেশ)
    )

    /** 🔓 TK-এর অনুমতি (31.07.2026): শুধু বাংলা-বন্ধ স্টাফের (KNE-KISHAN5) জন্য। */
    private val QUICK_EN_HI = listOf(
        "CHECK-UP done / जाँच-अप हो गया",
        "KTA done / KTA हो गया",
        "DRESSING done / ड्रेसिंग हो गई",
        "KSHAR SUTRA done / क्षार सूत्र हो गया",
        "KSHAR SUTRA CLEAR done / क्षार सूत्र क्लियर हो गया",
        "MEDICINE given / दवा दे दी गई",
        "TEST sent / टेस्ट के लिए भेजा गया",
        "MACHINE work done / मशीन का काम हो गया",
        "LIS done / LIS हो गया",
        "Visit Return done / विज़िट रिटर्न हो गया"   // 🟢🔒 V615
    )

    /** যে তালিকাটা এই স্টাফ দেখবেন। */
    fun labels(): List<String> = if (NoBengali.active()) QUICK_EN_HI else QUICK_BN

    /**
     * 🟢🔒 V1090 (০৫.০৯.২০২৬, TK-রিপোর্ট *"check up done দুইবার কেন"*) —
     * লেখাটা কি **পুরোপুরি** এই চিপগুলো দিয়েই বানানো?
     *
     * History পর্দায় চিকিৎসার নোট আর ফোনের রিমার্ক আলাদা করতে লাগে
     * (`followups.history`-তে ধরন লেখা থাকে না)। চিপ চাপলে লেখাগুলো
     * " · " দিয়ে জোড়া হয় (উপরের `attach`), তাই সেভাবেই ভেঙে মেলানো হয়।
     *
     * ⛔ বড়-ছোট হাতের অক্ষর ধরা হয় না (`UppercaseInputUtil` ইংরেজি লেখা
     *    বড় হাতে করে দেয়)। ⛔ দুটো তালিকাই (বাংলা ও ইংরেজি-হিন্দি) দেখা হয়,
     *    কারণ এক ক্লিনিকে দু'রকম স্টাফ থাকতে পারেন।
     * ⛔ একটা টুকরোও তালিকার বাইরে হলে `false` — অর্থাৎ মানুষের নিজের হাতে
     *    লেখা কথা কখনো "চিকিৎসার নোট" ধরে নেওয়া হয় না।
     */
    fun isQuickNoteText(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        val all = (QUICK_BN + QUICK_EN_HI).map { it.trim().lowercase() }
        val parts = t.split(" · ").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return parts.isNotEmpty() && parts.all { all.contains(it) }
    }

    /** হাতে লেখার ঘরের নিচে বসার নির্দেশ-লাইন। */
    fun hint(): String = NoBengali.s("আজ কী হলো — নিজে লিখুন বা নিচের চিপ চাপুন")

    /**
     * `container`-এ শিরোনাম + ৯টা চিপ বসায়। চিপে চাপলে লেখাটা `input`-এ
     * যোগ হয় (আগে কিছু লেখা থাকলে " · " দিয়ে জোড়া) — হুবহু আগের আচরণ।
     */
    fun attach(ctx: Context, container: LinearLayout, input: EditText) {
        val d = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        container.addView(TextView(ctx).apply {
            text = NoBengali.s("দ্রুত (চাপলে লেখায় বসবে):")
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#7A1F3D"))
            setPadding(0, dp(10), 0, dp(4))
        })
        labels().forEach { label ->
            container.addView(TextView(ctx).apply {
                text = "＋ $label"
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#7A1F3D"))
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
                val p = dp(10); setPadding(p, dp(9), p, dp(9))
                isClickable = true
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ); lp.topMargin = dp(6); layoutParams = lp
                setOnClickListener {
                    val cur = input.text.toString().trim()
                    input.setText(if (cur.isBlank()) label else "$cur · $label")
                    input.setSelection(input.text.length)
                }
            })
        }
    }
}
