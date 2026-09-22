package com.tkbiswas.pilesclinic.native

/**
 * 🔒🔒 খাতার সারি B169 (TK, 30.07.2026 — TK-এর ৬ নম্বর সন্দেহ)
 * ══════════════════════════════════════════════════════════════════
 *
 * TK-এর কথা (হুবহু):
 *   *"একাধিক Retry ব্যবস্থা একসঙ্গে চলতে পারে। Background Retry, Screen-open
 *    Retry, Manual 'পাঠান' এবং WorkManager একই shared lock ব্যবহার করছে না।
 *    ফল: একই কাজ একসঙ্গে একাধিকবার পাঠানোর চেষ্টা, অপ্রয়োজনীয় Internet call
 *    এবং App ভারী হওয়ার ঝুঁকি থাকে। Fixed ID থাকার কারণে সাধারণত টাকা দ্বিগুণ
 *    হবে না, কিন্তু ব্যবস্থাটি যথেষ্ট পরিষ্কার ও শক্ত নয়।"*
 *
 * **TK-এর কথা ঠিক ছিল** (কোড ধরে মিলিয়ে দেখা): তালা ছিল **শুধু একটাতেই** —
 * `BottomNav.retryStuckSaves`-এর নিজের ভিতরে। বাকি তিনটে জায়গা সেই তালার কথা
 * জানতই না:
 *   · `BackgroundRefreshWorker` — পিছনের ১৫ মিনিটের কাজ
 *   · `SyncWorker` — WorkManager-এর নিজের কাজ
 *   · `PendingSyncStatus.retryAll` — হোম পেজের লাল বারে "পাঠান" বোতাম
 * তাই তিন-চারটে দফা **একই সময়ে** চলতে পারত: একই সারি একসঙ্গে কয়েকবার
 * পাঠানোর চেষ্টা, অকারণ ইন্টারনেট খরচ, ব্যাটারি ক্ষয়, দুর্বল লাইনে অ্যাপ ভারী।
 *
 * ⛔ **টাকা কখনো দ্বিগুণ হত না** (TK নিজেও সেটা লিখেছেন) — প্রতিটা সারির নিজের
 *    আইডি আছে, আর প্রতিটা তালিকার ভিতরে আলাদা তালা আছে। ক্ষতি ছিল শুধু অপচয়।
 *
 * **এখন চারটে জায়গাই এই একটাই দরজা দিয়ে ঢোকে** — একজন ভিতরে থাকলে বাকিরা
 * ঢোকে না।
 *
 * ⛔ পাঠানোর কাজ · তার ক্রম · কোনো নিয়ম — এক অক্ষরও বদলানো হয়নি।
 * ⛔ যা-ই ঘটুক (ব্যর্থতা · exception) দরজা `finally`-তে খুলে যায়, তাই একবার
 *    আটকে গেলে পাঠানো চিরকাল বন্ধ হয়ে যেতে পারে না।
 */
object SyncGate {

    private val busy = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * ভিতরে কেউ না থাকলে কাজটা চালায়। কেউ থাকলে **চুপচাপ ফিরে যায়** —
     * কারণ ওই দফাটাই এই কাজগুলো করছে, দ্বিতীয়বার করার দরকার নেই।
     * @return সত্যিই চলল কি না।
     */
    fun tryRun(block: () -> Unit): Boolean {
        if (!busy.compareAndSet(false, true)) return false
        try {
            block()
        } finally {
            busy.set(false)
        }
        return true
    }

    /**
     * TK নিজে "পাঠান" বোতাম চাপলে এটা ব্যবহার হয়। তখন চুপচাপ ফিরে যাওয়া
     * ঠিক নয় — TK ভাববেন বোতামটা কাজ করেনি। তাই একটু **অপেক্ষা** করা হয়;
     * তার মধ্যে দরজা খালি হলে কাজটা চলে।
     * ⛔ মেইন থ্রেডে কখনো ডাকা যাবে না (ডাকা হয় শুধু পিছনের থ্রেড থেকে)।
     * @return সত্যিই চলল কি না।
     */
    fun runWaiting(maxWaitMs: Long, block: () -> Unit): Boolean {
        val until = System.currentTimeMillis() + maxWaitMs
        while (!busy.compareAndSet(false, true)) {
            if (System.currentTimeMillis() >= until) return false
            try {
                Thread.sleep(200L)
            } catch (_: InterruptedException) {
                return false
            }
        }
        try {
            block()
        } finally {
            busy.set(false)
        }
        return true
    }
}
