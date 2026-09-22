package com.tkbiswas.pilesclinic.native

import java.util.concurrent.Executors

/**
 * 🔒 TK-এর নিয়ম (28.07.2026): **স্টাফকে কখনো ক্লাউডের জন্য অপেক্ষা করানো যাবে না।**
 * TK-এর কথা: "আমি আমার ফোনে যখন কাজ করবো, মসৃণভাবে কাজ করতে হবে।"
 *
 * স্টাফ Save চাপলে পরের কাজটা (ক্যালেন্ডার · প্রিন্ট পর্দা · পরের রোগী) **সঙ্গে
 * সঙ্গে** খুলবে, আর ক্লাউডে পাঠানোর কাজটা চুপচাপ পিছনে চলবে।
 *
 * এটা নিরাপদ কেন: প্রতিটা সেভ **আগে ফোনেই লেখা হয়** (LocalWorkflowStore), তারপর
 * ক্লাউডে যায়; ক্লাউডে না গেলে অপেক্ষমাণ তালিকায় জমা থাকে আর নিজে থেকেই আবার
 * চেষ্টা হয় (অ্যাপ খুললে + ঘণ্টায় একবার)। কিছু আটকে থাকলে Dashboard-এর লাল
 * সতর্কবাতি দেখায়। তাই অপেক্ষা না করেও কোনো লেখা কখনো হারায় না।
 *
 * পর্দা বন্ধ হয়ে গেলেও কাজ থামে না — এই থ্রেডগুলো অ্যাপের, কোনো একটা পর্দার নয়।
 * (lifecycleScope ব্যবহার করলে পর্দা বন্ধ হওয়ামাত্র সেভ বাতিল হয়ে যেত।)
 */
object BackgroundWork {

    private val pool = Executors.newFixedThreadPool(3) { r ->
        Thread(r, "piles-bg").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    /** Runs [task] off the main thread. Never throws, never blocks the caller. */
    fun run(task: () -> Unit) {
        try {
            pool.execute {
                try { task() } catch (_: Throwable) { }
            }
        } catch (_: Throwable) {
            // Pool refused (shutting down) -- still must not run on the main
            // thread, so fall back to a plain thread rather than the caller.
            try {
                Thread { try { task() } catch (_: Throwable) { } }.apply { isDaemon = true }.start()
            } catch (_: Throwable) { }
        }
    }
}
