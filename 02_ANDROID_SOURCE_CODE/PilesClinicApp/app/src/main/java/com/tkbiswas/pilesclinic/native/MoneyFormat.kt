package com.tkbiswas.pilesclinic.native

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * 🔴 V430 (TK-সিদ্ধান্ত ১৮.০৮.২০২৬) — টাকার অঙ্কে কমা বসার **একটাই নিয়ম**।
 *
 * কেন দরকার হলো
 * -------------
 * অ্যাপের ভিতরেই দুরকম ছিল: বেশিরভাগ পর্দা ভারতীয় ভাগে (২,১০,৮৫০) দেখাত,
 * কিন্তু টাকার খাতা · বেতন · খাতা-নোট · RMP-র একটা জায়গা `Locale.US` ধরে
 * বিদেশি ভাগে (২১০,৮৫০) দেখাত। TK বললেন সব জায়গায় **ভারতীয় ভাগ** থাকবে।
 *
 * ⛔ এটা শুধু **দেখানোর** নিয়ম — কোনো অঙ্ক, সেভ বা হিসাব ছোঁয়া হয়নি।
 * ⛔ ফোনের ভাষা/দেশ যাই থাকুক, ফল সবসময় একই (নিয়মটা এখানে হাতে লেখা,
 *    তাই ফোনের সেটিং বদলালেও কাগজ ও পর্দা বদলাবে না)।
 */
object MoneyFormat {

    private val fmt: DecimalFormat by lazy {
        // #,##,##0 = শেষ তিন অঙ্কের পরে প্রতি দুই অঙ্কে কমা — ভারতীয় নিয়ম।
        DecimalFormat("#,##,##0", DecimalFormatSymbols(Locale.US)).apply {
            // 🔒 V1433 (১৩.০৯.২০২৬, তালিকা ৫৫২): আধা টাকা উপরে গোল (HALF_UP) — অ্যাপের
            // বাকি সব জায়গার "%,.0f" ও ওয়েবের Math.round-এর সাথে এক নিয়ম। আগে
            // DecimalFormat-এর নিজস্ব HALF_EVEN ছিল: ₹0.50 এখানে "0", অন্য পর্দায় "1" —
            // RMP Due List-এ JH MANDAL "Due ₹0" অথচ patient-wise "₹1" এই কারণেই।
            roundingMode = java.math.RoundingMode.HALF_UP
        }
    }

    /** 210850.0 → "2,10,850" ; -1500.0 → "-1,500" */
    @JvmStatic
    fun inr(value: Double): String = try {
        synchronized(fmt) { fmt.format(value) }
    } catch (_: Throwable) {
        String.format(Locale.US, "%,.0f", value)
    }
}
