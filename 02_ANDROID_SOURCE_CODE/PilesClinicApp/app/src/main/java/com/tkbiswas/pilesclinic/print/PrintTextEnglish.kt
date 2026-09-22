package com.tkbiswas.pilesclinic.print

/**
 * 🔒 TK-এর স্থায়ী নিয়ম (২৯.০৭.২০২৬ সকাল ১০.৪০ — খাতার সারি B74):
 *
 *   *"App-এর মধ্যে বাংলা থাকলে অসুবিধা নেই। কিন্তু প্রিন্ট আউট হওয়ার পরে কোনো
 *   বাংলা লেখা চলবে না। শুধুমাত্র ডায়েট চার্টে বাংলা থাকবে।"*
 *
 * আর TK-এর সিদ্ধান্ত (১০.৫০): **পর্দার বোতামগুলো বাংলাই থাকবে, শুধু ছাপার
 * সময় ইংরেজি হয়ে যাবে।** স্টাফ **নিজের হাতে** যা লিখবেন সেটা যেমন আছে
 * তেমনই ছাপা হবে — TK: *"নিজের হাতে যা লিখবে সেটা প্রিন্ট আউট হলে অসুবিধা নেই।"*
 *
 * ⛔ এই নিয়ম **এক জায়গাতেই** থাকবে — Chamber Register ও Report Card দুটো
 * ছাপাই এখান থেকেই ডাকে, যাতে দুই কাগজে কখনো দুই রকম না হয়।
 *
 * ⛔ **ডেটাবেসে কিছু বদলানো হয় না।** যা সেভ হয়ে আছে তা বাংলাই থাকে; শুধু
 * কাগজে ছাপার ঠিক আগে লেখাটা বদলে নেওয়া হয়। তাই পুরনো রেকর্ডও নিজে থেকেই
 * ইংরেজিতে ছাপা হবে, আর পর্দার চেহারা এক অক্ষরও বদলায় না।
 */
object PrintTextEnglish {

    /**
     * Chamber Attendance ও Report Card-এর "Treatment Progress" ঘরের **দ্রুত
     * চিপ**-গুলোর হুবহু লেখা (`ChamberAttendanceActivity` ও `ReportCardActivity`
     * -এর `quick` তালিকা) আর তার ইংরেজি রূপ।
     *
     * ⚠️ চিপের তালিকা বদলালে এখানেও যোগ করতে হবে — নইলে ওই লেখাটা কাগজে
     * বাংলাই ছাপা হবে।
     */
    private val chipToEnglish: List<Pair<String, String>> = listOf(
        "KSHAR SUTRA ক্লিয়ার করা হল" to "KSHAR SUTRA CLEARED",
        "TEST করতে পাঠানো হল" to "SENT FOR TEST",
        "MACHINE এর কাজ করা হল" to "MACHINE WORK DONE",
        "KSHAR SUTRA করা হল" to "KSHAR SUTRA DONE",
        "CHECK-UP করা হলো" to "CHECK-UP DONE",
        "DRESSING করা হল" to "DRESSING DONE",
        "MEDICINE দেওয়া হল" to "MEDICINE GIVEN",
        "KTA করা হল" to "KTA DONE",
        "LIS করা হল" to "LIS DONE",
        "Visit Return করা হল" to "VISIT RETURN DONE"   // 🟢🔒 V615 (২৪.০৮.২০২৬, TK-নির্দেশ)
    ).sortedByDescending { it.first.length }

    /**
     * ছাপার ঠিক আগে ডাকতে হবে। জানা চিপের লেখা থাকলে সেটুকু ইংরেজি হয়ে যায়;
     * বাকি সব — স্টাফের নিজের হাতে লেখা কথা সহ — **হুবহু অক্ষত** থাকে।
     *
     * একটা ঘরে একাধিক চিপ থাকতে পারে (যেমন `KTA করা হল · DRESSING করা হল`),
     * তাই প্রতিটা চিপ আলাদা করে খোঁজা হয়। লম্বা লেখা আগে বদলানো হয় যাতে
     * ছোট লেখা তার ভিতরের অংশ কেটে না দেয়।
     *
     * ফাঁকা বা `null` এলে ফাঁকাই ফেরে — কখনো crash করে না, কখনো ঘর হারায় না।
     */
    fun forPrint(text: String?): String {
        val src = text ?: return ""
        if (src.isBlank()) return src
        var out = src
        for ((bn, en) in chipToEnglish) {
            if (out.contains(bn)) out = out.replace(bn, en)
        }
        return out
    }
}
