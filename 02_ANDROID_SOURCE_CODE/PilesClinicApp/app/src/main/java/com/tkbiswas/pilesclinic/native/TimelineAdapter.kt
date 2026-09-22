package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemTimelineBinding

/** TK-REQUESTED REDESIGN (2026-07-16): renders the register-style table row
 *  (Visit# / Date / Progress / Paid / Due) TK approved, replacing the old
 *  spacious card. Edit behaviour is unchanged: 3-tap still only fires for
 *  entries that carry a real payment id (see onBindViewHolder). */
class TimelineAdapter(
    private val context: Context,
    private var items: List<TimelineEntry>,
    // TK APPROVED (2026-07-15): standing rule — 3-tap must edit everything.
    // Only fires for entries that carry a real payment id (see onBindViewHolder).
    private val onPaymentEdit: (TimelineEntry) -> Unit = {}
) : RecyclerView.Adapter<TimelineAdapter.VH>() {

    fun update(newItems: List<TimelineEntry>) {
        /* 🔴🔒 V509 (TK-রিপোর্ট ২১.০৮.২০২৬ — "স্ক্রিন কম্পন দিচ্ছে"):
           এই তালিকা আগে **দুবার** বসত (প্রথমে ফোনে জমানো, তারপর ক্লাউড
           থেকে এসে আবার)। `notifyDataSetChanged()` প্রতিবার দেখা-যাওয়া
           সব সারি নতুন করে আঁকে — বেশিরভাগ সময় তথ্য হুবহু এক হলেও।
           চোখে সেটাই **ঝিলিক**। এখন হুবহু এক হলে আর বসানো হয় না।
           ⛔ এক চুল আলাদা হলেই আগের মতোই পুরো বসে — কিছু চাপা পড়ে না।
           ⛔ `data class` বলে তুলনাটা তথ্যের ভিত্তিতেই হয়, ঠিকানার নয়। */
        if (items.size == newItems.size && items == newItems) return
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemTimelineBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemTimelineBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun getItemCount(): Int = items.size

    private fun displayDate(raw: String): String =
        if (raw.length >= 10) FollowUpModel.displayDate(raw.take(10)) else raw

    private fun money(v: Double): String = "\u20B9" + "%,.0f".format(v)

    override fun onBindViewHolder(holder: VH, position: Int) {
        // CRASH-SAFETY FIX (TK-reported via video, 2026-07-16): RecyclerView
        // renders each card on its own pass, outside the Activity's
        // try-catch -- so a bad entry here could still crash the app even
        // with PatientTimelineActivity's own safety net. A failed row is now
        // just skipped (blank), the rest of the list still shows.
        try {
            val e = items[position]
            val b = holder.binding

            b.rowRoot.setBackgroundColor(Color.parseColor(if (position % 2 == 1) "#F6FAF7" else "#FFFFFF"))

            b.tvVisitNo.text = e.visitNo.toString()
            b.tvDate.text = displayDate(e.date)
            b.tvTitle.text = e.title
            if (e.note.isNotBlank()) {
                b.tvNote.text = e.note
                b.tvNote.visibility = View.VISIBLE
            } else {
                b.tvNote.visibility = View.GONE
            }

            if (e.paymentId != null) {
                // \ud83d\udd34\ud83d\udd12 V1605 (\u09e7\u09ef.\u09e6\u09ef.\u09e8\u09e6\u09e8\u09ec, TK-\u09b0\u09bf\u09aa\u09cb\u09b0\u09cd\u099f, \u099b\u09ac\u09bf\u09b8\u09b9) \u2014 \u098f\u0987 \u0998\u09b0\u09c7\u09b0 \u09b0\u0982 XML-\u098f
                // \u09b8\u09cd\u09a5\u09bf\u09b0 \u09b8\u09ac\u09c1\u099c \u09ac\u09b8\u09be\u09a8\u09cb \u099b\u09bf\u09b2 (item_timeline.xml), \u09a4\u09be\u0987 Refund \u09b8\u09be\u09b0\u09bf\u0993
                // (\u099f\u09be\u0995\u09be \u09ac\u09c7\u09b0\u09cb\u09a8\u09cb) \u09b8\u09be\u09a7\u09be\u09b0\u09a3 \u09aa\u09c7\u09ae\u09c7\u09a8\u09cd\u099f\u09c7\u09b0 \u09ae\u09a4\u09cb\u0987 \u09b8\u09ac\u09c1\u099c \u09a6\u09c7\u0996\u09be\u09a4\u0964 \u098f\u0996\u09a8 Refund
                // \u09b8\u09be\u09b0\u09bf\u09a4\u09c7 \u09b2\u09be\u09b2, \u09b8\u09be\u09ae\u09a8\u09c7 "\u2212" \u2014 PaymentActivity.kt-\u098f\u09b0 \u09aa\u09aa-\u0986\u09aa\u09c7\u09b0 \u09b9\u09c1\u09ac\u09b9\u09c1
                // \u098f\u0995\u0987 \u09a8\u09bf\u09af\u09bc\u09ae/\u09b0\u0982 (#B3261E)\u0964
                val isRefundRow = e.payType.equals("refund", true)
                b.tvPaid.text = (if (isRefundRow) "\u2212" else "") + money(e.paymentAmount)
                b.tvPaid.setTextColor(Color.parseColor(if (isRefundRow) "#B3261E" else "#0EA25F"))
                b.tvDue.text = if (e.runningDue < 0) "\u2014" else money(e.runningDue)
                TripleTapEdit.attach(b.rowRoot) { onPaymentEdit(e) }
            } else {
                // \ud83d\udd34\ud83d\udd12 V1605 \u2014 RecyclerView \u09aa\u09c1\u09a8\u09b0\u09cd\u09ac\u09cd\u09af\u09ac\u09b9\u09be\u09b0\u09c7 \u0986\u0997\u09c7\u09b0 \u09b8\u09be\u09b0\u09bf \u09b2\u09be\u09b2 \u09b0\u09c7\u0996\u09c7
                // \u0997\u09c7\u09b2\u09c7 \u098f\u0987 "\u2014" \u09b8\u09be\u09b0\u09bf\u0993 \u09b2\u09be\u09b2 \u09a6\u09c7\u0996\u09be\u09a4\u0964 \u09aa\u09cd\u09b0\u09a4\u09bf\u09ac\u09be\u09b0\u0987 \u09b0\u0982 \u09ab\u09bf\u09b0\u09bf\u09af\u09bc\u09c7 \u09a6\u09c7\u0993\u09af\u09bc\u09be \u09b9\u09b2\u09cb\u0964
                b.tvPaid.setTextColor(Color.parseColor("#0EA25F"))
                b.tvPaid.text = "\u2014"
                b.tvDue.text = "\u2014"
                b.rowRoot.setOnClickListener(null)
            }
        } catch (_: Throwable) { }
    }
}
