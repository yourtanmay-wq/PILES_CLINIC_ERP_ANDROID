package com.tkbiswas.pilesclinic.native

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemDraftCardBinding

/**
 * Renders each Draft entry as a follow-up-style card: name, mobile,
 * branch · disease, an optional status/remark line, and the four actions
 * Call / WhatsApp / View / Restore. "Restore" is hidden for the plain
 * "My Enquiry" list (tab "received"), matching the web draft cards where only
 * reject/incomplete/complete lists are restorable.
 *
 * TK-REQUESTED ADDITION (2026-07-18), Phase 1: on the "My Enquiry" list only,
 * that same (otherwise-hidden) button slot shows "🗑️ Delete" instead —
 * ONLY when TrashHelper.canDelete() allows it for the current user (same-day
 * + same staff who received it, or Master anytime). No layout/view was
 * added; the existing btnRestore view is reused, so nothing else changes.
 */
class DraftCardAdapter(
    private var items: List<DraftEntry>,
    private val currentUser: NativeUser,
    private val onCall: (DraftEntry) -> Unit,
    private val onWhatsApp: (DraftEntry) -> Unit,
    private val onView: (DraftEntry) -> Unit,
    private val onRestore: (DraftEntry) -> Unit,
    private val onDelete: (DraftEntry) -> Unit = {},
    /** 🆕 (07.08.2026) — টিক পড়লে/উঠলে Activity-কে জানায় (নিচের বার দেখাতে)।
     *  ডিফল্ট ফাঁকা, তাই পুরনো কোনো caller ভাঙে না। */
    private val onPickChanged: () -> Unit = {}
) : RecyclerView.Adapter<DraftCardAdapter.VH>() {

    /** 🆕 (07.08.2026) — "একসাথে মার্ক" করা কার্ডের id। */
    val pickedIds = linkedSetOf<String>()

    fun clearPicks() { pickedIds.clear(); notifyDataSetChanged() }

    fun pickedEntries(): List<DraftEntry> = items.filter { pickedIds.contains(it.id) }

    /** 🔴 TK-রিপোর্ট (07.08.2026): "নিচে স্ক্রল করে কোনো অ্যাকশন নিলে আবার
     *  একদম প্রথমে চলে আসে কেন? আমি চাই যেখানে ছিলাম সেখানেই থাকব।"
     *  **আসল কারণ:** `DraftListActivity.renderList()` প্রতিবার **নতুন Adapter**
     *  বানিয়ে RecyclerView-তে বসাত — নতুন Adapter মানেই স্ক্রল আবার শূন্য থেকে।
     *  **সমাধান:** Adapter একবারই তৈরি হয়, তালিকা বদলালে শুধু এই ফাংশন —
     *  তাই স্ক্রলের জায়গা অক্ষত থাকে। ⛔ কোনো ডেটা/লজিক বদলায়নি। */
    fun updateItems(newItems: List<DraftEntry>) {
        items = newItems
        val alive = newItems.map { it.id }.toSet()
        pickedIds.retainAll(alive)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemDraftCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemDraftCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        val b = holder.b
        // 🔒 TK-এর নির্দেশ (29.07.2026 রাত ১০.১০ · খাতার সারি B99):
        // *"তাছাড়া এখানেও আলাদা সিরিয়াল নম্বর হবে।"*
        // প্রতিটা তালিকায় **উপর থেকে 1, 2, 3 …** — Follow-up কার্ডের মতোই
        // নামের আগে। ⛔ ডেটাবেসে কিছু লেখা হয় না, শুধু দেখানোর নম্বর।
        // ⛔ নাম/মোবাইল/আইডি — কিছুই বদলায়নি, শুধু আগে নম্বরটা বসে।
        val serial = position + 1
        // 🆕 B420 (05.08.2026, TK-নির্দেশে ফটো-প্রুফ পাশ করার পরে) — মোবাইল
        // নম্বর এখন নামের **আগে**, একই সারিতে (আগে নাম উপরে, মোবাইল নিচে
        // আলাদা সারিতে ছিল)। সিরিয়াল নম্বর এখন নামের বদলে দ্বিতীয় সারির
        // (tvMeta) শুরুতে বসে, যেহেতু উপরের সারিতে (মোবাইল+নাম) জায়গা কম।
        // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে মোবাইল দুইবার দেখাত।
        b.tvName.text = e.name.ifBlank { "UNKNOWN" }
        b.tvMobile.text = PatientIdText.mobileWithId(e.mobile, "", phoneIcon = true)
        b.tvMobile.copyOnLongPress("Mobile", e.mobile)
        // 🆕 B420 — Patient ID (থাকলে) ও কোন সেকশন থেকে (e.extra) — দুটোই
        // এখন এই একই দ্বিতীয় সারিতে, ব্রাঞ্চ/ডিজিজ-এর সাথে। ⛔ tvExtra আর
        // ব্যবহার হয় না (নিচে সবসময় GONE) — কোনো তথ্যই হারায়নি, শুধু একই
        // লাইনে চলে এসেছে।
        val metaParts = mutableListOf("$serial.")
        listOf(e.branch, e.disease).filter { it.isNotBlank() }.let { if (it.isNotEmpty()) metaParts.add(it.joinToString(" · ").uppercase()) }
        if (e.patientId.isNotBlank()) metaParts.add("\uD83C\uDD94 " + e.patientId)
        if (e.extra.isNotBlank()) metaParts.add(e.extra)
        b.tvMeta.text = metaParts.joinToString("  ·  ")
        b.tvMeta.visibility = View.VISIBLE
        b.tvExtra.visibility = View.GONE

        // TK-REPORTED (2026-07-26): same cleanup as the Follow-up card . the
        // internal "Registered (syncing…)" placeholder is never shown to
        // anyone; it is replaced at DISPLAY time only, nothing is rewritten.
        val remarkRaw = e.lastRemark.ifBlank { "" }
        val remark = if (remarkRaw.trim().startsWith("Registered (syncing", ignoreCase = true))
            "Registered patient / Visit created" else remarkRaw
        if (remark.isBlank()) b.tvRemark.visibility = View.GONE
        // TK-APPROVED (2026-07-25, via photo proof): label removed -- the
        // box's own light-green color now makes it recognizable at a
        // glance, applied consistently project-wide.
        else { b.tvRemark.visibility = View.VISIBLE; b.tvRemark.text = remark }

        // "My Enquiry (All Branch)" is a plain list — no Restore there.
        // TK-REQUESTED (2026-07-18): on this same list, that slot instead
        // shows Delete when this user is allowed to delete this entry
        // (same-day self-delete, or Master anytime). "unexpected" (a
        // read-only report list) still never shows this slot at all.
        if (e.tab == "received") {
            // 🔒 খাতার সারি B98 (TK, 29.07.2026): এখন থেকে **সবাই** বোতামটা
            // দেখেন — মাস্টার চাপলে সোজা ডিলিট, অন্য কেউ চাপলে মাস্টারের
            // ঘন্টায় অনুরোধ যায় (`DraftListActivity.confirmDelete`)।
            // ⛔ আগে স্টাফ শুধু একই দিনে নিজেরটা মুছতে পারতেন — সেটা বন্ধ।
            b.btnDelete2.visibility = View.GONE // 🔒 RecyclerView পুনর্ব্যবহার-নিরাপত্তা — শুধু "refunded"-এই VISIBLE হয়
            val canDelete = true
            if (canDelete) {
                b.btnRestore.visibility = View.VISIBLE
                b.btnRestore.text = if (currentUser.role == "master") "🗑️ Delete"
                    else "🗑️ Delete (অনুমতি)"
                b.btnRestore.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FBE1E3"))
                b.btnRestore.setTextColor(android.graphics.Color.parseColor("#B8324A"))
                b.btnRestore.setOnClickListener { onDelete(e) }
            } else {
                b.btnRestore.visibility = View.GONE
            }
        } else if (e.tab == "unexpected") {
            b.btnRestore.visibility = View.GONE
            b.btnDelete2.visibility = View.GONE
        } else if (e.tab == "runningtreatment") {
            // 🟢🔒 V644 (২৫.০৮.২০২৬, TK-নির্দেশ — "কোনো খারাপ কিছু যেন না
            // হয়") — এই তালিকা শুধু তথ্য দেখানোর জন্য (কে এখন চিকিৎসাধীন),
            // Restore/Delete কোনো অ্যাকশনই দরকার নেই — তাই দুটোই GONE
            // (নইলে `DraftRepository.restore()`-এর অচেনা tab-এ চাপলে
            // "Restore failed" টোস্ট দেখাত, বিভ্রান্তিকর)।
            b.btnRestore.visibility = View.GONE
            b.btnDelete2.visibility = View.GONE
        } else if (e.tab == "refunded") {
            // 🔴 TK-নির্দেশ (02.08.2026, B302.1): এখানে Restore সক্রিয় —
            // চাপলে হাতে করে Patient কার্ডে ফিরিয়ে আনে (নতুন টাকা জমা ছাড়াই,
            // Complete-despite-Due-এর হুবহু একই প্যাটার্ন)। সাথে Delete
            // বোতামও (দ্বিতীয় স্লট) — বাকি Delete-এর মতোই Trash-এ যায়,
            // Master-এর অনুমতি লাগে (DraftListActivity-তে যাচাই হয়)।
            b.btnRestore.visibility = View.VISIBLE
            b.btnRestore.isEnabled = true
            b.btnRestore.text = "↩️ Restore"
            b.btnRestore.backgroundTintList = null
            b.btnRestore.setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            b.btnRestore.setOnClickListener { onRestore(e) }
            b.btnDelete2.visibility = View.VISIBLE
            b.btnDelete2.setOnClickListener { onDelete(e) }
        } else {
            b.btnRestore.visibility = View.VISIBLE
            b.btnRestore.text = "↩️ Restore"
            b.btnRestore.backgroundTintList = null                                   // 🔒 রিসাইকেল-নিরাপত্তা: "received"-এর লাল আভা যেন এখানে না আসে
            b.btnRestore.setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            b.btnRestore.setOnClickListener { onRestore(e) }
            // 🆕🔴 TK-নির্দেশ (07.08.2026, ফটো-প্রুফ অনুমোদিত) — Reject তালিকাগুলোয়
            // (Enquiry Reject / Visit Reject) আগে শুধু Restore ছিল, Delete বোতামই
            // দেখা যেত না। এখন পাশেই 🗑️ Delete — চাপলে আগের সেই প্রমাণিত পথেই
            // (`DraftListActivity.confirmDelete` → Trash Bin) মোছে।
            // 🆕🔴 TK-নির্দেশ (২১.০৯.২০২৬) — Return Visit List-এও এখন 🗑️ Delete
            // দেখা যাবে (এখানেও ডেমো/ভুল রেকর্ড থাকতে পারে)। পারমিশন-নিয়ম
            // বদলায়নি — চাপলে যায় `DraftListActivity.confirmDeleteRefundedPatient`
            // → `deletePatientRecord` → `DeletePermission.canDeleteEntryNow`
            // (Master সবসময়; নইলে শুধু আজকের নিজের তৈরি করা রেকর্ড, চেম্বার
            // খোলা ও নিজের OUT TIME সেভ না হওয়া পর্যন্ত; নইলে Master-এর
            // অনুমতির অনুরোধ)। ⛔ Incomplete Patient / Complete Patient এই
            // adapter-এ আসেই না (ALIVE_BUCKETS-এর জন্য FollowUpAdapter ব্যবহার
            // হয়, DraftListActivity.kt দেখুন) — তাই এখানে যোগ করা অর্থহীন হতো।
            val showDelete = e.tab == "enqreject" || e.tab == "visitreject" || e.tab == "returnvisit"
            b.btnDelete2.visibility = if (showDelete) View.VISIBLE else View.GONE
            if (showDelete) b.btnDelete2.setOnClickListener { onDelete(e) }
        }

        // 🆕 TK-নির্দেশ (07.08.2026) — "অনেকগুলো একসাথে মার্ক করেও ডিলিট/Restore
        // করতে পারি"। টিক-বক্স শুধু সেই তালিকাগুলোয় (My Enquiry · Enquiry Reject ·
        // Visit Reject · Unexpected Time); Refunded/Incomplete/Complete-এ কখনো নয়।
        // ⛔ রিসাইকেল-নিরাপদ: listener খুলে → অবস্থা বসিয়ে → আবার listener।
        val canPick = pickableTabs.contains(e.tab)
        b.cbPick.setOnCheckedChangeListener(null)
        b.cbPick.visibility = if (canPick) View.VISIBLE else View.GONE
        b.cbPick.isChecked = canPick && pickedIds.contains(e.id)
        if (canPick) {
            b.cbPick.setOnCheckedChangeListener { _, checked ->
                if (checked) pickedIds.add(e.id) else pickedIds.remove(e.id)
                onPickChanged()
            }
        }

        b.btnCall.setOnClickListener { onCall(e) }
        b.btnWhatsapp.setOnClickListener { onWhatsApp(e) }
        b.btnView.setOnClickListener { onView(e) }
    
        // 🔴🔴🔒 V449 (TK-রিপোর্ট ১৮.০৮.২০২৬ — "লগইন হয়ে যায় কিন্তু কার্যকরী কিছু হয় না")।
        // আসল কারণ: NoBengali-এর অটো-সুইপ শুধু পর্দার layout-পাসে চলে; RecyclerView
        // rebind/scroll নতুন layout-পাস তৈরি না করেই লেখা বসিয়ে দেয়, তাই তালিকার
        // সারির বাংলা কখনো ঢাকাই পড়ত না। এখন প্রতিটা বাইন্ডের শেষেই সরাসরি সুইপ —
        // বাংলা-বন্ধ না থাকলে কিছুই করে না (activeCache false ⇒ সাথে সাথে ফেরত)।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }

    companion object {
        /** 🆕 (07.08.2026, TK নিজে বেছে দিয়েছেন) — যেসব তালিকায় "একসাথে মার্ক"
         *  চলবে। ⛔ refunded / notcomplete / complete ইচ্ছাকৃতভাবে বাদ। */
        val pickableTabs = setOf("received", "enqreject", "visitreject", "unexpected")
    }
}
