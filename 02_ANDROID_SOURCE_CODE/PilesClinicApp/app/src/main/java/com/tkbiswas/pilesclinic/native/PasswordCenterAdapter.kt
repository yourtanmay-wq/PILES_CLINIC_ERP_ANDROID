package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemCredentialCardBinding

class PasswordCenterAdapter(
    private val context: Context,
    private var items: List<UserCredential>,
    private val onChange: (UserCredential) -> Unit
) : RecyclerView.Adapter<PasswordCenterAdapter.ViewHolder>() {

    fun updateItems(newItems: List<UserCredential>) {
        /* 🔴🔒 V509 (TK-রিপোর্ট ২১.০৮.২০২৬ — "স্ক্রিন কম্পন দিচ্ছে"):
           তালিকা আগে দুবার বসত (ফোনে জমানো, তারপর ক্লাউড)। হুবহু এক হলেও
           `notifyDataSetChanged()` প্রতিবার সব সারি নতুন করে আঁকত — চোখে
           সেটাই ঝিলিক। এখন হুবহু এক হলে আর বসানো হয় না।
           ⛔ এক চুল আলাদা হলেই আগের মতোই পুরো বসে। */
        if (items.size == newItems.size && items == newItems) return
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemCredentialCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCredentialCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding
        val acc = item.account

        // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে মোবাইল দুইবার দেখাত।
        // 🔴🔒 V1510 (TK-রিপোর্ট ১৬.০৯.২০২৬) — আসল নাম থাকলে "নাম (কোড)",
        // না থাকলে (যেমন শেয়ার-করা ব্রাঞ্চ লগইন) আগের মতোই শুধু কোড।
        b.tvName.text = if (item.fullName.isNotBlank()) "${item.fullName} (${acc.name})" else acc.name.ifBlank { "UNKNOWN" }
        b.tvMeta.text = "${acc.role} · ${acc.branch.ifBlank { "-" }} · ${PasswordCenterModel.mob(acc.mobile)}"
        b.tvPassword.text = "Password: ${item.password}"
        /* 🎨🔒 V829 (২৯.০৮.২০২৬, TK-অনুমোদিত ফটো-প্রুফ: *"হ্যাঁ করুন, তবে সাবধানে"*)
           — অ্যাপের থিমে XML-এর সাদামাটা `<Button>` আপনা-আপনি **MaterialButton**
           হয়ে যায়, আর সেটা `android:background` **অগ্রাহ্য করে** নিজের গাঢ় নীল
           `backgroundTint` বসিয়ে দেয়। ফলে XML-এ লেখা রংটা ফোনে কখনো দেখা যেত না
           (কম্পিউটারে ঠিকই দেখা যেত)। `backgroundTintList = null` বসালে তবেই
           XML-এর drawable-টা দেখা যায় — প্রজেক্টের নিজেরই প্রমাণিত ওষুধ
           (`DoctorQueueAdapter` · `DraftCardAdapter`-এ আগে থেকেই চলছে, পাহারা ৯.৩২)।
           ⛔ শুধু চেহারা — বোতামের কাজ · জায়গা · লেখা কিচ্ছু বদলায়নি। */
        b.btnChange.backgroundTintList = null
        b.btnChange.setOnClickListener { onChange(item) }
    
        // 🔴🔒 V449 — একই ফিক্স (দেখুন FollowUpAdapter.kt-এর মন্তব্য): তালিকার
        // সারির বাংলা যাতে বাংলা-বন্ধ স্টাফের ফোনে কখনো না দেখা যায়।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }
}
