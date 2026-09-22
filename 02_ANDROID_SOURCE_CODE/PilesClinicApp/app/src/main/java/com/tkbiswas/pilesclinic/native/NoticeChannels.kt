package com.tkbiswas.pilesclinic.native

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * 🚨 TK-REPORTED (28.07.2026, স্টাফদের রিপোর্ট · খাতার সারি B28):
 * **"Notification sound আসে না।"**
 *
 * আসল কারণ (দুটো, দুটোই একসঙ্গে ঠিক করতে হয়):
 *
 * ১) নতুন Android-এ (৮ ও তার পরে) নোটিফিকেশনের **শব্দ ঠিক হয় "চ্যানেল" থেকে**,
 *    নোটিফিকেশন বানানোর সময়কার `setDefaults(DEFAULT_SOUND)` থেকে নয় — ওই লাইনটা
 *    নতুন ফোনে Android একেবারেই দেখে না। অ্যাপের তিনটে চ্যানেলের একটাতেও শব্দ
 *    **স্পষ্ট করে বসানো ছিল না**।
 *
 * ২) আরও বড় কথা: **একটা চ্যানেল একবার তৈরি হয়ে গেলে তার সেটিং আর বদলানো যায় না।**
 *    পুরনো বিল্ড ফোনে চ্যানেলটা একবার বানিয়ে ফেলেছে, তাই এখন কোডে শব্দ বসালেও
 *    ওই পুরনো (নিঃশব্দ) চ্যানেলটাই থেকে যেত। তাই **নতুন নামে চ্যানেল** বানাতে হয়।
 *
 * এখানে দুটোই করা হয়েছে: নতুন নাম (`..._v2`) + শব্দ স্পষ্ট করে বসানো, আর পুরনো
 * নিঃশব্দ চ্যানেলটা মুছে দেওয়া হয় যাতে ফোনের সেটিংসে দুটো নাম না দেখায়।
 *
 * ⛔ এতে শুধু শব্দ ফেরে — কোনো তথ্য, টাকা, নিয়ম বা ডিজাইন ছোঁয়া হয়নি।
 * ⚠️ স্টাফ নিজে ফোনের সেটিংস থেকে অ্যাপের নোটিফিকেশন বন্ধ করে রাখলে বা "Silent"
 *    করে রাখলে অ্যাপ থেকে সেটা জোর করে চালু করা যায় না — সেটা ফোনের নিজের নিয়ম।
 */
object NoticeChannels {

    /**
     * চ্যানেলটা তৈরি (বা নিশ্চিত) করে এবং **যে নামটা ব্যবহার করতে হবে** সেটা ফেরত দেয়।
     * পুরনো Android-এ (৮-এর আগে) চ্যানেল বলে কিছু নেই, তখন নামটা শুধু ফেরত যায়।
     */
    fun ensure(ctx: Context, oldId: String, name: String, description: String): String {
        val id = oldId + "_v2"
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return id
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // পুরনো নিঃশব্দ চ্যানেলটা সরিয়ে দেওয়া হয়, নইলে ফোনের সেটিংসে দুটো দেখাত।
            try { nm.deleteNotificationChannel(oldId) } catch (_: Throwable) { }
            val ch = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // 🔊 এই তিনটে লাইনই আসল — ফোনের নিজের নোটিফিকেশন শব্দটা বাজবে।
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            nm.createNotificationChannel(ch)
        } catch (_: Throwable) {
            // কিছু ভুল হলেও নোটিফিকেশন আগের মতোই যাবে, শুধু শব্দ না-ও থাকতে পারে।
        }
        return id
    }
}
