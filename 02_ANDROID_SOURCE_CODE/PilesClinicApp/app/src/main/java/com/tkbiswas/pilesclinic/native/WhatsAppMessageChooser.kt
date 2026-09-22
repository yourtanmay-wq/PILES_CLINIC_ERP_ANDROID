package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast

// 🔒 TK-ORDER (31.07.2026, V235-এ আরও কঠোর): "যেকোনো বার্তা যখন পাঠানো হয়, তখন
// যেখানে WhatsApp আসে সেখানে যেন choose করা যায় কোন WhatsApp দিয়ে পাঠানো হবে —
// Personal নাকি Business।" এবং V235: **"একটি WhatsApp installed থাকলেও সেটিও
// chooser/selection-এর মাধ্যমে খুলবে — silent default নয়। কোনো WhatsApp app
// default হবে না বা সরাসরি খুলবে না।"**
//
// তাই এখন — Personal (com.whatsapp) ও Business (com.whatsapp.w4b)-এর মধ্যে **যেগুলো
// সত্যিই installed** শুধু সেগুলো নিয়ে একটা ছোট selection dialog দেখানো হয় (এক বা
// দুই — যা-ই থাক, কখনো নিঃশব্দে খোলে না)। ব্যবহারকারী বেছে দিলে তবেই ওই নির্দিষ্ট
// package-এ prepared message-সহ WhatsApp খোলে। কোনোটাই installed না থাকলে crash
// নয় — পরিষ্কার safe message।
//
// ⛔ এই একটা জায়গায় বদলালেই সব "WhatsApp" জায়গায় একই আচরণ আসে — Patient Message,
// RMP/Doctor Message, আর Follow-up/Timeline/Search/Draft/Briefing-এর chat বোতামও
// এখন এই একই ফাংশন ডাকে (পুরনো আলাদা আলাদা `wa.me` intent বাদ)।
// ⛔ prepared message text (`text`) অপরিবর্তিত থাকে; শুধু বার্তা-হীন chat বোতাম
// থেকে ডাকলে `text` ফাঁকা যায় (তখন URL-এ `?text=` যোগ হয় না)।
object WhatsAppMessageChooser {

    private const val PKG_PERSONAL = "com.whatsapp"
    private const val PKG_BUSINESS = "com.whatsapp.w4b"

    private fun isInstalled(activity: Activity, pkg: String): Boolean =
        try { activity.packageManager.getPackageInfo(pkg, 0); true } catch (_: Throwable) { false }

    // 🔴 B343 (03.08.2026, TK-রিপোর্ট — "WhatsApp Business-এ চাপলে কিছুই হয়
    // না, সবসময়ই হয়") — আসল কারণ: `https://wa.me/...` লিংক Android-এর
    // "App Link" যাচাই-ব্যবস্থার উপর নির্ভর করে (Settings → Apps → WhatsApp
    // Business → Set as default → এই ডোমেইন অনুমোদিত কিনা) — বেশিরভাগ ফোনেই
    // WhatsApp Business এই যাচাই নিজে থেকে পায় না (Personal WhatsApp সাধারণত
    // পায়), তাই মিলে যাওয়া কোনো অ্যাপ না পেয়ে চুপচাপ কিছুই হয় না। **সমাধান:**
    // `https://` এর বদলে WhatsApp-এর নিজস্ব `whatsapp://` স্কিম — এটা কোনো
    // App-Link যাচাই লাগে না (কাস্টম স্কিম, সরাসরি WhatsApp/Business-এর
    // নিজের রেজিস্টার করা হ্যান্ডলারে যায়), তাই দুটো অ্যাপেই সমান নির্ভরযোগ্য।
    // এটাই বছরের পর বছর ধরে সবচেয়ে প্রমাণিত/প্রচলিত পদ্ধতি। ⛔ setPackage()
    // দিয়ে কোন অ্যাপ (Personal/Business) খুলবে তার সিদ্ধান্ত আগের মতোই অক্ষত।
    private fun urlFor(mobile: String, text: String): Uri {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        val textPart = if (text.isBlank()) "" else "&text=" + Uri.encode(text)
        return Uri.parse("whatsapp://send?phone=91$digits$textPart")
    }

    private fun openInPackage(activity: Activity, pkg: String, uri: Uri) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { setPackage(pkg) })
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, "WhatsApp is not installed on this phone", Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {
            Toast.makeText(activity, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔴 B344 (03.08.2026, TK-নির্দেশ — "শুধু Business না, WhatsApp-এও যেন
    // যায়, প্রতিবার দুটোই মনে করিয়ে দেবে") — TK নিশ্চিত করেছেন ওই ফোনে
    // **দুটো অ্যাপই** ইনস্টল করা আছে, তবু `isInstalled()` (getPackageInfo)
    // শুধু Business-কেই ধরেছিল, Personal WhatsApp-কে না — কিছু ফোনে
    // (বিশেষত কিছু ব্র্যান্ডের কাস্টম Android-এ) এই পাহারা নিজেই নির্ভরযোগ্য
    // না। **সমাধান:** আগে-থেকে-যাচাই (isInstalled) তুলে দেওয়া হলো — এখন
    // থেকে **সবসময় দুটো অপশনই** (WhatsApp + WhatsApp Business) দেখানো হয়,
    // ঠিক TK যা চেয়েছেন। কোনোটা সত্যিই ইনস্টল না থাকলে `openInPackage()`-এর
    // আগে-থেকে-থাকা নিরাপদ ব্যবস্থাই কাজ করবে (চাপলে "WhatsApp is not
    // installed on this phone" — crash নয়)। `isInstalled()` ফাংশনটা কোডে
    // অক্ষত রাখা হলো (মোছা হয়নি), শুধু এখানে আর ডাকা হয় না।
    // 🔴 B359 (03.08.2026, TK-রিপোর্ট — "WhatsApp চাপলেই popup বন্ধ হয়ে
    // Dashboard-এ ফিরে যাচ্ছে, Personal/Business বাক্স আসছেই না") — আসল কারণ:
    // এই chooser dialog নিজে অ্যাসিঙ্ক্রোনাস (.show() সাথে সাথেই ফিরে আসে, আসল
    // বাছাই পরে ব্যবহারকারী চাপলে হয়) — কিন্তু ডাকা হতো এমন জায়গা থেকে (দেখুন
    // PatientMessage.presentSendBox()) যেখানে এই ফাংশন কল করার ঠিক পরের লাইনেই
    // বর্তমান Activity finish() হয়ে যেত, ব্যবহারকারী কিছু বাছার আগেই — dialog-টা
    // তার নিজের Activity-র সাথেই তখন ভেঙে (dismiss) যেত। **সমাধান:** নতুন
    // ঐচ্ছিক `onDone` — ব্যবহারকারী সত্যিই একটা অপশন বাছলে বা Cancel করলে
    // (দুটো ক্ষেত্রেই) তবেই ডাকা হয়, কখনো `.send()` কল করার সাথে সাথে না।
    // ⛔ **ডিফল্ট null** — যেসব জায়গায় (Follow-up/Timeline/Search/Draft/
    // Briefing-এর প্লেইন 💬 চ্যাট বোতাম) এই ফাংশন `onDone` ছাড়াই ডাকা হয়,
    // তাদের আচরণ এক অক্ষরও বদলায়নি।
    // 🔴🆕🔒 খাতার সারি B438 (TK-নির্দেশ, 05.08.2026 — Work Notebook IN TIME
    // মার্ক করার পরে স্বয়ংক্রিয়ভাবে WhatsApp খুলবে, কিন্তু **নির্দিষ্ট কোনো
    // নম্বরে না** — স্টাফ নিজে বেছে নেবেন কাকে পাঠাবেন, ঠিক যেভাবে ফোনের
    // সাধারণ "Share" শীট কাজ করে)। উপরের `send()`-এর থেকে আলাদা — ওটা
    // সবসময় একটা নির্দিষ্ট `mobile`-এ সরাসরি চ্যাট খোলে (রোগী/ডাক্তারের
    // বার্তার জন্য ঠিক), কিন্তু এখানে কোনো নির্দিষ্ট নম্বরই নেই। তাই
    // `whatsapp://send?phone=...` ব্যবহার করা যায় না — বদলে Android-এর
    // নিজস্ব `ACTION_SEND` (টেক্সট শেয়ার), যেটা WhatsApp-এর ভিতরের
    // "কাকে পাঠাবেন" তালিকা খোলে। ⛔ Personal/Business বাছাই এখানেও
    // বাধ্যতামূলক (TK: "শুধু Personal হলে চলবে না, Business-ও চুজ করতে
    // হবে") — উপরের মতোই একই ধরনের ছোট নির্বাচন-পপআপ।
    fun sendGeneric(activity: Activity, text: String, onDone: (() -> Unit)? = null) {
        val options = listOf("WhatsApp" to PKG_PERSONAL, "WhatsApp Business" to PKG_BUSINESS)
        fun openShareInPackage(pkg: String) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    setPackage(pkg)
                }
                activity.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(activity, "WhatsApp is not installed on this phone", Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
                Toast.makeText(activity, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
        try {
            val labels = options.map { it.first }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setCustomTitle(PremiumAlert.header(activity, "💬 Send message with"))
                .setItems(labels) { _, which -> openShareInPackage(options[which].second); onDone?.invoke() }
                .setNegativeButton("Cancel") { _, _ -> onDone?.invoke() }
                .setOnCancelListener { onDone?.invoke() }
                .show().also { PremiumAlert.paint(it) }
        } catch (_: Throwable) {
            openShareInPackage(options[0].second)
            onDone?.invoke()
        }
    }

    fun send(activity: Activity, mobile: String, text: String = "", onDone: (() -> Unit)? = null) {
        val uri = urlFor(mobile, text)
        // 🔒 V235 + B344: detection-নির্ভর ফিল্টার ছাড়াই সবসময় দুটো অপশনই।
        val options = listOf("WhatsApp" to PKG_PERSONAL, "WhatsApp Business" to PKG_BUSINESS)

        // 🔒 V235: সবসময় selection দেখাবে (silent default নয়)।
        try {
            val labels = options.map { it.first }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setCustomTitle(PremiumAlert.header(activity, "💬 Send message with"))
                .setItems(labels) { _, which -> openInPackage(activity, options[which].second, uri); onDone?.invoke() }
                .setNegativeButton("Cancel") { _, _ -> onDone?.invoke() }
                .setOnCancelListener { onDone?.invoke() }
                .show().also { PremiumAlert.paint(it) }
        } catch (_: Throwable) {
            // dialog দেখাতে না পারলেও (অসম্ভব প্রায়) — অন্তত প্রথমটাতে চেষ্টা করে,
            // কখনো crash নয় (openInPackage নিজেই "not installed" সামলে নেয়)।
            openInPackage(activity, options[0].second, uri)
            onDone?.invoke()
        }
    }
}
