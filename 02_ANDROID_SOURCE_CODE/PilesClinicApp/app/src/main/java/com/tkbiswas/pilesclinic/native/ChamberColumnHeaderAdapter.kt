package com.tkbiswas.pilesclinic.native

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.R

/**
 * 🔒🎨 খাতার সারি B473 (05.08.2026, TK-নির্দেশ — "PATIENT | TREATMENT
 * PROGRESS | FEES | CASH | ONLINE হেডার ৫টা বক্সের নিচেই থাকবে, স্ক্রল
 * করলে শুধু বক্স হাইড হবে")।
 *
 * খাতার সারি B448-এ ৫টা বক্স (`ChamberHeaderAdapter`) RecyclerView-এর
 * প্রথম আইটেম হিসেবে বসানো হয়েছিল, যাতে স্ক্রল করলে সরে যায় — কিন্তু
 * এই কলাম-হেডার লাইনটা তখনো পুরনো জায়গায় (RecyclerView-এর **বাইরে**,
 * স্থির) থেকে গিয়েছিল। ফলে চেহারার ক্রম উল্টে গিয়েছিল — হেডার বক্সের
 * **উপরে** উঠে এসেছিল, যেটা TK-এর অনুমতি ছাড়াই হয়েছিল (নিজের ভুল)।
 *
 * এখন এই ছোট, একক-আইটেম Adapter কলাম-হেডারকে **৫টা বক্সের ঠিক পরের
 * (দ্বিতীয়) আইটেম** বানায় — `ConcatAdapter(headerAdapter,
 * columnHeaderAdapter, adapter)` — তাই বিশ্রাম অবস্থায় (স্ক্রল করার
 * আগে) ক্রম আবার ঠিক: বক্স → হেডার → রোগীর তালিকা।
 *
 * স্ক্রল করলে বক্স ও এই আইটেম দুটোই স্বাভাবিকভাবে সরে যায় — TK-এর নতুন
 * নির্দেশ (05.08.2026): "শুধু বক্স হাইড হবে, হেডার আটকে থাকবে।" তাই
 * `ChamberAttendanceActivity.kt`-এ একটা আলাদা, পিন-করা ডুপ্লিকেট (XML-এর
 * `pinnedTableHeaderRow`, প্রথমে লুকানো) এই আইটেমটা স্ক্রল করে সরে
 * গেলেই দেখা যায় ও স্ক্রিনের উপরে আটকে থাকে — স্ক্রল-লিসেনার দেখুন।
 *
 * ⛔ **ঝুঁকিহীন নকশা:** `ChamberHeaderAdapter.kt`-এর হুবহু একই প্যাটার্ন
 * (আলাদা, নতুন, ছোট Adapter) — `ChamberAttendanceAdapter.kt` এক অক্ষরও
 * ছোঁয়া হয়নি।
 */
class ChamberColumnHeaderAdapter : RecyclerView.Adapter<ChamberColumnHeaderAdapter.ColumnHeaderVH>() {

    class ColumnHeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeaderPayment: TextView = view.findViewById(R.id.tvHeaderPayment)
        val tvHeaderCash: TextView = view.findViewById(R.id.tvHeaderCash)
        val tvHeaderOnline: TextView = view.findViewById(R.id.tvHeaderOnline)
    }

    private var holder: ColumnHeaderVH? = null
    private var pendingMoneyVisibility: Int? = null

    fun currentHolder(): ColumnHeaderVH? = holder

    /** ⛔ B473 — money-কলাম (FEES/CASH/ONLINE) লুকানো/দেখানোর সিদ্ধান্ত
     *  (Expected ফিল্টারে লুকায়, বাকিতে দেখায় — পুরনো নিয়ম B... অক্ষত)
     *  View তৈরি হওয়ার আগেই আসতে পারে, তাই মনে রাখা হয় ও View তৈরি
     *  হলে সঙ্গে সঙ্গে বসানো হয় (headerAdapter-এর `onBound` ঠিক একই কৌশল)। */
    fun setMoneyColumnsVisibility(visibility: Int) {
        pendingMoneyVisibility = visibility
        holder?.let { applyMoneyVisibility(it, visibility) }
    }

    private fun applyMoneyVisibility(vh: ColumnHeaderVH, visibility: Int) {
        vh.tvHeaderPayment.visibility = visibility
        vh.tvHeaderCash.visibility = visibility
        vh.tvHeaderOnline.visibility = visibility
    }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnHeaderVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chamber_column_header, parent, false)
        val vh = ColumnHeaderVH(v)
        holder = vh
        pendingMoneyVisibility?.let { applyMoneyVisibility(vh, it) }
        return vh
    }

    override fun onBindViewHolder(holder: ColumnHeaderVH, position: Int) { /* সব বাইন্ডিং setMoneyColumnsVisibility()-এর মাধ্যমেই */ 
        // 🔴🔒 V449 — একই ফিক্স (দেখুন FollowUpAdapter.kt-এর মন্তব্য): তালিকার
        // সারির বাংলা যাতে বাংলা-বন্ধ স্টাফের ফোনে কখনো না দেখা যায়।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }
}
