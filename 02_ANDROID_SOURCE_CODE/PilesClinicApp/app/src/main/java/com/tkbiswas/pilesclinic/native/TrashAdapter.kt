package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemTrashCardBinding

/**
 * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-অনুমোদিত প্রুফ) — **Trash Bin-এর নতুন ছোট কার্ড।**
 *
 * TK-এর কথা (হুবহু): *"এই জিনিসটা দেখে আমি বুঝতেই পারছি না যে কোন পেশেন্টকে
 * আমি রিস্টোর করবো বা কোন পেশেন্টকে আমি ডিলিট ফরএভার করব... এখানে ভিউ একটা
 * অপশন থাকার দরকার ছিল... এই কার্ডেই পেশেন্টের সম্পূর্ণ ডিটেলস ছোট আকারে
 * দেখানো থাকবে... restore and delete forever এগুলি পাশাপাশি থাকবে... কার্ডগুলি
 * যত ছোট রাখা যায়... অনেকগুলো একসাথে সিলেক্ট করা যাবে তারও ব্যবস্থা রাখতে হবে।"*
 *
 * ⛔ **যা এক অক্ষরও বদলায়নি:** কোন সারি দেখা যাবে (ব্রাঞ্চের নিয়ম), Restore ও
 *    Delete Forever আসলে কী করে, "Sure?" পপ-আপ — সবই আগের মতোই। এখানে শুধু
 *    **দেখার চেহারা** আর **কী কী তথ্য দেখানো হবে** সেটুকু।
 * ⛔ কার্ডের সব লেখা **ইংরেজি** (TK: *"বাংলা থাকবে না"*)।
 */
class TrashAdapter(
    private val context: Context,
    private var items: List<TrashItem>,
    private val onRestore: (TrashItem) -> Unit,
    // TK APPROVED (2026-07-20): permanent delete. onDelete is only wired up
    // when showDelete is true (Master), so a non-master never sees or fires it.
    private val showDelete: Boolean = false,
    private val onDelete: (TrashItem) -> Unit = {},
    // 🔴 V511: নতুন — 👁 View, আর একসাথে বাছার ব্যবস্থা।
    private val onView: (TrashItem) -> Unit = {},
    private val isPicked: (TrashItem) -> Boolean = { false },
    private val onTogglePick: (TrashItem) -> Unit = {}
) : RecyclerView.Adapter<TrashAdapter.ViewHolder>() {

    /** Select মোড চালু আছে কি না — চালু থাকলে প্রতিটা কার্ডে টিক-ঘর ওঠে
     *  আর তিনটে বোতাম লুকিয়ে যায় (কার্ড আরও ছোট হয়)। */
    var selectMode: Boolean = false

    fun updateItems(newItems: List<TrashItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemTrashCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrashCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding
        val rec = item.record

        /* 🔴🔴🔒 V790 (২৮.০৮.২০২৬, TK-রিপোর্ট ছবিসহ: *"ফটোপ্রুফ দেখিয়েছিলেন
           কিন্তু সেই অনুসারে ডিজাইন তো এখানে হয় নাই"*)

           ─── আসল কারণ (কোড ধরে প্রমাণিত, আন্দাজ নয়) ───────────────────────
           V773-এ তিনটে বোতামের নরম রং **XML-এ ঠিকই বসানো হয়েছিল**
           (`bg_trash_btn_view/restore/delete` + `#2F6E80/#2F7A4B/#A4453A`)।
           কিন্তু অ্যাপের থিম `Theme.MaterialComponents.Light.NoActionBar`,
           আর সেই থিমে XML-এর সাদামাটা `<Button>` আপনা-আপনি **MaterialButton**
           হয়ে যায় — যে `android:background` **অগ্রাহ্য করে**, নিজের
           `backgroundTint` (থিমের গাঢ় নীল) বসিয়ে দেয়।
           ⇒ তাই কম্পিউটারে রংটা এসেছিল (CSS), কিন্তু **ফোনে আসেনি** —
             TK-এর ছবিতে গাঢ় নীল বোতামই দেখা যাচ্ছে। ভুলটা আমার।

           ─── সমাধান (প্রকল্পের নিজেরই প্রমাণিত ওষুধ) ───────────────────────
           `DoctorQueueAdapter` (লাইন 150-153) ও `DraftCardAdapter` ঠিক এই
           কাজটাই করে: `backgroundTintList = null` — তখন MaterialButton আর
           নিজের রং চাপায় না, XML-এর drawable-টাই দেখা যায়। Doctor Queue-র
           চার রঙের বোতাম এভাবেই ঠিক দেখাচ্ছে, তাই এটা যাচাই-করা পথ।
           `textAllCaps = false` — কম্পিউটারের হুবহু একই লেখা ("👁 View"),
           MaterialButton নইলে জোর করে বড় হাতের করে দেয়।
           ⛔ layout · মাপ · কী চাপলে কী হয় — কিচ্ছু বদলায়নি। */
        b.btnView.backgroundTintList = null
        b.btnRestore.backgroundTintList = null
        b.btnDeleteForever.backgroundTintList = null
        b.btnView.isAllCaps = false
        b.btnRestore.isAllCaps = false
        b.btnDeleteForever.isAllCaps = false

        b.tvLabel.text = item.label.ifBlank { "(record)" }

        // ── কবে মোছা হয়েছে + কে মুছেছে (একই লাইনে, TK-নির্দেশ V660) ────────
        // 🟢🔒🔒 V660 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — আগে tvWhen
        // (ডান-উপরে) আর tvBy (নিচে আলাদা সারিতে) দুটো আলাদা জায়গায় ছিল,
        // বাড়তি জায়গা নিত। এখন একই একটা লাইনে — tvWhen-এই বসে
        // (whenAndBy(), "🗑 তারিখ, সময় · Deleted by নাম")। tvBy আর ব্যবহার
        // হয় না (নিচে GONE)।
        b.tvWhen.text = TrashCardText.whenAndBy(item)

        // ── চিপ ১ · কোথা থেকে মোছা হয়েছে ─────────────────────────────────
        b.tvChipSrc.text = TrashCardText.sourceLabel(item.table)

        // ── চিপ ২ · ব্রাঞ্চ ───────────────────────────────────────────────
        val branch = rec.s("branch").trim()
        if (branch.isBlank()) {
            b.tvChipBranch.visibility = View.GONE
        } else {
            b.tvChipBranch.visibility = View.VISIBLE
            b.tvChipBranch.text = branch.uppercase()
        }

        // ── চিপ ৩ · টাকা বা রোগ (যেটা প্রযোজ্য) ───────────────────────────
        val extra = TrashCardText.extraChip(item)
        if (extra.isBlank()) {
            b.tvChipExtra.visibility = View.GONE
        } else {
            b.tvChipExtra.visibility = View.VISIBLE
            b.tvChipExtra.text = extra
        }

        // ── লাইন ১ · মোবাইল ও পেশেন্ট আইডি ────────────────────────────────
        val l1 = TrashCardText.line1(item)
        b.tvLine1.visibility = if (l1.isBlank()) View.GONE else View.VISIBLE
        b.tvLine1.text = l1

        // ── লাইন ২ · টেবিল অনুযায়ী বাকি দরকারি ঘর ─────────────────────────
        val l2 = TrashCardText.line2(item)
        b.tvLine2.visibility = if (l2.isBlank()) View.GONE else View.VISIBLE
        b.tvLine2.text = l2

        // 🟢🔒 V660 — "কে মুছেছেন" এখন উপরে tvWhen-এর সাথে একই লাইনে
        // (whenAndBy()) — এই আলাদা তৃতীয় সারিটা আর দরকার নেই, তাই সবসময়
        // লুকানো (id/view মোছা হয়নি — TK-নিয়ম — শুধু GONE)।
        b.tvBy.visibility = View.GONE

        // ── Select মোড ────────────────────────────────────────────────────
        if (selectMode) {
            b.cbPick.visibility = View.VISIBLE
            // ⛔ শোনার কাজটা আগে বন্ধ করে তবেই টিক বসানো হয়, নইলে RecyclerView
            //    কার্ড আবার ব্যবহার করার সময় ভুল সারিতে টিক পড়ে যেতে পারত।
            b.cbPick.setOnCheckedChangeListener(null)
            b.cbPick.isChecked = isPicked(item)
            b.cbPick.setOnCheckedChangeListener { _, _ -> onTogglePick(item) }
            b.rowButtons.visibility = View.GONE
            b.cardBody.setOnClickListener { b.cbPick.isChecked = !b.cbPick.isChecked }
        } else {
            b.cbPick.setOnCheckedChangeListener(null)
            b.cbPick.isChecked = false
            b.cbPick.visibility = View.GONE
            b.rowButtons.visibility = View.VISIBLE
            // ⛔ Select মোড বন্ধ থাকলে কার্ডে চাপ দিলে 👁 View খোলে — TK-এর
            //    *"একবার দেখে নিতে পারি"* কথাটা এখানেও।
            b.cardBody.setOnClickListener { onView(item) }
        }

        b.btnView.setOnClickListener { onView(item) }
        b.btnRestore.setOnClickListener { onRestore(item) }
        b.btnDeleteForever.visibility = if (showDelete) View.VISIBLE else View.GONE
        b.btnDeleteForever.setOnClickListener { onDelete(item) }
    }
}
