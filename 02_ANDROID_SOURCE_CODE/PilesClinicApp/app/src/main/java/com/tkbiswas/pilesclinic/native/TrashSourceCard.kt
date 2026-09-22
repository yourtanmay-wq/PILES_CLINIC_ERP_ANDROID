package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.view.View
import android.widget.FrameLayout

/**
 * 🟢🔒 V590 (২৩.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) — **Trash Bin-এর 👁 View।**
 *
 * TK-এর কথা: *"View-তে চাপার পর আগে যেখানে ছিল সেখানকার মতনই চেহারা দেখতে
 * হতে হবে।"*
 *
 * আগে View-তে শুধু `Label: value` ধাঁচের একটা লম্বা তালিকা দেখাত — মুছে ফেলা
 * সারিটা কোন পর্দার, দেখে বোঝার উপায় ছিল না।
 *
 * ─── কীভাবে করা হলো (এটাই সবচেয়ে জরুরি) ─────────────────────────────────
 * কার্ডটা এখানে **নতুন করে আঁকা হয়নি**। যে পর্দায় সারিটা থাকত, সেই পর্দার
 * **আসল Adapter-কেই** একটা সারি দিয়ে ডেকে তার তৈরি করা View বসানো হয় —
 *   · payments  → `CollectionAdapter`  (Collection পর্দার সারি)
 *   · followups · enquiries → `FollowUpAdapter` (Follow-up পর্দার কার্ড)
 * তাই চেহারা "মতন" নয়, **হুবহু এক**; আর ভবিষ্যতে ওই কার্ড বদলালে এটাও নিজে
 * থেকেই বদলে যাবে (দুই জায়গায় আলাদা করে রাখতে হবে না)।
 *
 * সারির JSON → কার্ডের ঘর — এই বদলটাও নিজে লেখা হয়নি; প্রজেক্টের আগে থেকেই
 * থাকা ও প্রমাণিত দুটো ফাংশন ব্যবহার হয়েছে:
 *   · `PaymentModel.parsePaymentRow()` · `FollowUpModel.parse()`
 * (Trash সারির ভিতরে **আসল সারির হুবহু JSON**-ই জমা থাকে, তাই এরা ঠিক সেভাবেই
 *  পড়তে পারে — এটা কোড দেখে মিলিয়ে নেওয়া, আন্দাজ নয়।)
 *
 * ⛔ **কোনো বোতাম কাজ করে না** — সব callback ফাঁকা। Trash থেকে ভুল করে কল ·
 *    WhatsApp · টাকা নেওয়া — কিছুই হতে পারে না।
 * ⛔ কিছু পড়া/লেখা হয় না, ক্লাউডে যায় না — Trash সারির ভিতরে যা আছে শুধু তাই।
 * ⛔ চেনা না গেলে (patients · doctor_visits · অন্য টেবিল) `null` ফেরে, তখন
 *    View আগের মতোই লেখার তালিকা দেখায় — পুরোনো আচরণ অক্ষত।
 */
object TrashSourceCard {

    /**
     * এই মুছে ফেলা সারিটা যে পর্দায় ছিল, সেখানকার কার্ড।
     * না বানাতে পারলে `null` (তখন ডাকার জায়গা আগের তালিকাই দেখায়)।
     */
    fun build(ctx: Context, item: TrashItem): View? = try {
        val parent = FrameLayout(ctx)
        when (item.table.trim().lowercase()) {
            "payments" -> {
                val row = PaymentModel.parsePaymentRow(item.record)
                val ad = CollectionAdapter(ctx, listOf(row))
                val vh = ad.onCreateViewHolder(parent, 0)
                ad.onBindViewHolder(vh, 0)
                vh.itemView
            }
            "followups", "enquiries" -> {
                /* enquiries সারিতে ফলো-আপের কিছু ঘর থাকে না (stage · nextFollow),
                   কিন্তু `FollowUpModel.parse()` না-থাকা ঘরকে ফাঁকা ধরে — তাই
                   কার্ডটা ভাঙে না, শুধু ওই ঘরগুলো দেখায় না। */
                val fu = FollowUpModel.parse(item.record)
                val ad = FollowUpAdapter(ctx, listOf(fu), {}, {}, {}, {})
                val vh = ad.onCreateViewHolder(parent, 0)
                ad.onBindViewHolder(vh, 0)
                vh.itemView
            }
            else -> null
        }?.also { v ->
            // সারিটা RecyclerView-র ভিতরে নেই, তাই নিজের চওড়া নিজেকেই বলতে হয়
            (v.layoutParams as? android.view.ViewGroup.LayoutParams)?.width =
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            /* ⛔ পুরো কার্ডের ছোঁয়া বন্ধ — ভিতরের কোনো বোতামে চাপ পড়তে পারে না।
               (callback-গুলো এমনিতেই ফাঁকা; এটা দ্বিতীয় জাল।) */
            disableTouch(v)
        }
    } catch (_: Throwable) {
        // কার্ড বানাতে না পারলে View যেন কখনো ফাঁকা না খোলে — তালিকায় ফিরে যায়
        null
    }

    private fun disableTouch(v: View) {
        v.isClickable = false
        v.isLongClickable = false
        v.isFocusable = false
        v.setOnClickListener(null)
        v.setOnTouchListener { _, _ -> true }
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) disableTouch(v.getChildAt(i))
        }
    }
}
