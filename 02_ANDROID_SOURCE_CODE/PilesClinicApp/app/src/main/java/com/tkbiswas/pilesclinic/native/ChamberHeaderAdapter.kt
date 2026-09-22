package com.tkbiswas.pilesclinic.native

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.R

/**
 * 🔴🎨🔒 খাতার সারি B448 (TK-নির্দেশ, 05.08.2026 — "উপরের পাঁচটা বক্স
 * ফিক্স থাকছে, রোগীর তালিকার জায়গা কমে যাচ্ছে")।
 *
 * এই ছোট, একক-আইটেম Adapter শুধু Registration/Mark Expected/Patient
 * বোতাম আর Expected/Arrived বাক্স (item_chamber_header.xml) দেখায় —
 * `ChamberAttendanceActivity`-তে `ConcatAdapter(headerAdapter,
 * chamberAdapter)` হিসেবে ব্যবহার হয়, RecyclerView-এর **প্রথম আইটেম**
 * হিসেবে, তাই তালিকার সাথেই স্ক্রল করে উপরে সরে যায়।
 *
 * ⛔ **ঝুঁকিহীন নকশা:** এটা সম্পূর্ণ **আলাদা, নতুন** Adapter — আগে থেকে
 * থাকা `ChamberAttendanceAdapter.kt` (পেমেন্ট/ট্রিটমেন্ট প্রগ্রেস/৩-ট্যাপ
 * এডিটসহ জটিল, ভারী ব্যবহৃত ফাইল) **এক অক্ষরও ছোঁয়া হয়নি** — সেই
 * ফাইলের কোনো ঝুঁকি নেই। এই ছোট Adapter শুধু একটাই View তৈরি করে ধরে
 * রাখে, Activity তার ভেতরের বোতাম/টেক্সট নিজে বাইরে থেকে বসায়
 * ([bindViews]) — ঠিক আগে `binding.btnAddRegistration` ইত্যাদি যেভাবে
 * সরাসরি ব্যবহার হতো, একইভাবে।
 */
class ChamberHeaderAdapter : RecyclerView.Adapter<ChamberHeaderAdapter.HeaderVH>() {

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val btnAddRegistration: Button = view.findViewById(R.id.btnAddRegistration)
        val btnMarkExpected: Button = view.findViewById(R.id.btnMarkExpected)
        val btnSearchPatient: Button = view.findViewById(R.id.btnSearchPatient)
        val branchDateRow: LinearLayout = view.findViewById(R.id.branchDateRow)
        val cardExpected: LinearLayout = view.findViewById(R.id.cardExpected)
        val cardArrived: LinearLayout = view.findViewById(R.id.cardArrived)
        val tvExpectedCount: TextView = view.findViewById(R.id.tvExpectedCount)
        val tvArrivedCount: TextView = view.findViewById(R.id.tvArrivedCount)
        // 🆕 (07.08.2026) — Expected/Arrived-এর মাঝের নতুন "কাল আসার কথা" বোতাম।
        val cardKalAsar: LinearLayout = view.findViewById(R.id.cardKalAsar)
        val tvKalAsarCount: TextView = view.findViewById(R.id.tvKalAsarCount)
    }

    // 🔒 Activity onCreate-এ একবারই সেট হয় (নিচের bindViews-এর মাধ্যমে
    // Activity তার সব ক্লিক-লিসেনার/টেক্সট-আপডেট এখানেই বসায়)।
    private var onBound: ((HeaderVH) -> Unit)? = null
    private var holder: HeaderVH? = null

    fun bindViews(onReady: (HeaderVH) -> Unit) {
        onBound = onReady
        holder?.let { onReady(it) }
    }

    /** ⛔ B448 — শুধু বর্তমানে-তৈরি-হওয়া View থাকলে সরাসরি সেটা ফেরত দেয়
     *  (নতুন করে `onBound` বদলায় না) — Expected/Arrived-এর মতো বারবার
     *  বদলানো ছোট মান-আপডেটের জন্য, যাতে [bindViews]-এ বসানো ক্লিক-
     *  লিসেনারগুলো ভুল করে হারিয়ে/ওভাররাইট না হয়। */
    fun currentHolder(): HeaderVH? = holder

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chamber_header, parent, false)
        val vh = HeaderVH(v)
        holder = vh
        onBound?.invoke(vh)
        return vh
    }

    override fun onBindViewHolder(holder: HeaderVH, position: Int) { /* সব বাইন্ডিং bindViews()-এর মাধ্যমেই, এখানে কিছু করার নেই */ 
        // 🔴🔒 V449 — একই ফিক্স (দেখুন FollowUpAdapter.kt-এর মন্তব্য): তালিকার
        // সারির বাংলা যাতে বাংলা-বন্ধ স্টাফের ফোনে কখনো না দেখা যায়।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }
}
