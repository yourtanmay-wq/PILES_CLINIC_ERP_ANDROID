package com.tkbiswas.pilesclinic.native

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast

/**
 * 📋🔒 V772 (২৮.০৮.২০২৬, TK-লাইভ রিপোর্ট) — **কপি করা নম্বর যেন কীবোর্ডের
 * সাজেশনে ভেসে না ওঠে।**
 *
 * TK: *"যেকোনো ঘরে চাপ দিয়ে দেখলাম — নম্বরের সাজেশন আসে, সম্পূর্ণ প্রজেক্টের
 * সমস্ত জায়গায় আসতেছে। আর কতবার আপনাকে বলতে হবে?"*
 *
 * ═══════════════════════════════════════════════════════════════════════
 * 🔴 **আসল কারণ (কোড ধরে খুঁজে পাওয়া — এবার আর Autofill নয়)**
 *
 * এতদিন V418 · V752 · V758 · V761 — চারবারই আমি ধরে নিয়েছিলাম সাজেশনটা
 * **Android-এর Autofill**। সেগুলো বন্ধ করার পরেও TK-এর ফোনে সাজেশন থেকে
 * গেছে। তাই এবার উল্টো দিক থেকে খোঁজা হলো — **অ্যাপ নিজে কী কী ক্লিপবোর্ডে
 * রাখে?** ফল:
 *
 *   অ্যাপের **১৭টা জায়গায়** রোগীর মোবাইল নম্বর ক্লিপবোর্ডে কপি হয় —
 *   Dialer · Chamber-এর সারি (একবার চাপলেই, V364) · Follow-up · Doctor
 *   Visit · Patient Timeline · Expected Tomorrow · লম্বা-চাপে কপি …
 *
 *   Gboard (আর প্রায় সব কীবোর্ড) **সদ্য কপি করা লেখাটা ~১ ঘণ্টা ধরে
 *   প্রত্যেকটা ঘরের সাজেশন-পট্টিতে চিপ হিসেবে দেখায়** — কোন অ্যাপ, কোন
 *   ঘর, তাতে কিছু যায় আসে না।
 *
 * ⇒ **এই কারণেই "সম্পূর্ণ প্রজেক্টের সমস্ত জায়গায়" আসছিল।** Autofill-এর
 *   কোনো পতাকা দিয়ে এটা কখনোই বন্ধ হতো না — ওটা অন্য জিনিস।
 * ═══════════════════════════════════════════════════════════════════════
 *
 * ✅ **সমাধান:** কপি করার সময় লেখাটাকে **"গোপন" (sensitive)** বলে দেওয়া হয়।
 *    তখন কীবোর্ড ওটা নিজের ইতিহাসে রাখে না, সাজেশনেও দেখায় না।
 *
 * ⛔ **পেস্ট করা একটুও বদলায় না** — WhatsApp/ডায়ালারে আগের মতোই পেস্ট হবে।
 * ⛔ অ্যাপের নিজের "copied" টোস্ট আগের মতোই থাকে।
 * ⛔ কোনো তথ্য হারায় না, কিছু মোছে না।
 *
 * ⚠️ **সৎ কথা (আন্দাজ নয়):** `IS_SENSITIVE` পতাকাটা Android 13 (API 33)
 *    থেকে **সরকারিভাবে** আছে — সেখানে নিশ্চিত কাজ করে। তার আগের Android-এ
 *    Gboard এই একই নামের পতাকা দেখে বলে জানা আছে, কিন্তু ওটা Google-এর
 *    নিজের সিদ্ধান্ত — **১০০% নিশ্চিত নয়**। কোনো ফোনে তবু থেকে গেলে ওই
 *    ফোনে একবার: Gboard → Settings → Clipboard → *"Show recently copied
 *    text in the suggestion strip"* **বন্ধ** — এটা নিশ্চিত কাজ করে।
 *
 * ⛔ **নিয়ম (পাহারা ৯.২৯ দিয়ে বাঁধা):** প্রজেক্টের আর কোথাও সরাসরি
 *    `setPrimaryClip(...)` লেখা যাবে না — সবাইকে `Clip.copy(...)` দিয়ে
 *    যেতে হবে, নইলে আবার একই ফাঁক তৈরি হবে।
 */
object Clip {

    /** `ClipDescription.EXTRA_IS_SENSITIVE` — API 33-এ যোগ হয়েছে, কিন্তু
     *  নামটা (string) পুরনো Android-এও একই, আর Gboard সেটাই দেখে। তাই
     *  সরাসরি নামটাই লেখা হলো — পুরনো ফোনেও চেষ্টা করা যায়। */
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    /**
     * এক জায়গা দিয়ে কপি — সবসময় "গোপন" চিহ্ন দিয়ে।
     * ⛔ কখনো ব্যতিক্রম ছোড়ে না; ফাঁকা লেখা হলে কিছুই করে না।
     * @return সত্যিই কপি হলো কি না (টোস্ট দেখানোর সিদ্ধান্তের জন্য)।
     */
    fun copy(context: Context, label: String, value: String?): Boolean {
        val v = value.orEmpty()
        if (v.isBlank()) return false
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val clip = ClipData.newPlainText(label, v)
            // 🤫 কীবোর্ডকে বলা — "এটা মনে রেখো না, সাজেশনে দেখিও না"।
            try {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(EXTRA_IS_SENSITIVE, true)
                }
            } catch (_: Throwable) { /* পতাকা না বসলেও কপি হবেই */ }
            cm.setPrimaryClip(clip)
            true
        } catch (_: Throwable) { false }
    }

    /** কপি + চেনা টোস্ট — আগের সব জায়গায় ঠিক এই লেখাটাই দেখাত। */
    fun copyWithToast(context: Context, label: String, value: String?) {
        if (copy(context, label, value)) {
            try { Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
        }
    }
}

/**
 * TK-REQUESTED (2026-07-18): "long-press to copy" for name/mobile text shown
 * anywhere in the app — Enquiry/Visit/Patient cards, popups, Chamber
 * Attendance, etc. Centralised here so every screen copies the exact same
 * way (same toast wording, same clipboard label) instead of each file
 * re-implementing it slightly differently.
 *
 * Usage: someTextView.copyOnLongPress("Name", item.name)
 *
 * 🔴 V772 — এখন `Clip.copy()` দিয়ে যায়, তাই কপি করা নম্বর আর কীবোর্ডের
 *    সাজেশন-পট্টিতে ভেসে ওঠে না। বাকি আচরণ (টোস্ট, লেবেল) হুবহু আগের মতো।
 */
fun View.copyOnLongPress(label: String, value: String) {
    setOnLongClickListener {
        if (value.isBlank()) return@setOnLongClickListener true
        Clip.copyWithToast(context, label, value)
        true
    }
}
