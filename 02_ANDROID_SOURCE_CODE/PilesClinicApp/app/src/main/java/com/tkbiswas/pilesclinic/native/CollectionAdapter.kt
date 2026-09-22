package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemCollectionRowBinding

class CollectionAdapter(
    private val context: Context,
    private var items: List<CollectionRow>,
    private val onRowClick: (CollectionRow) -> Unit = {},
    // 🔒 B572 (08.08.2026, TK-নির্দেশ): নামে ট্যাপ করলে ওই রোগীর History (Full
    // Journey) খোলে। পুরো সারির onRowClick (পেমেন্ট-ডিটেলস) অপরিবর্তিত।
    private val onNameClick: (CollectionRow) -> Unit = {}
) :
    RecyclerView.Adapter<CollectionAdapter.ViewHolder>() {

    fun updateItems(newItems: List<CollectionRow>) {
        /* 🔴🔒 V509 (TK-রিপোর্ট ২১.০৮.২০২৬ — "স্ক্রিন কম্পন দিচ্ছে"):
           তালিকা আগে দুবার বসত (ফোনে জমানো, তারপর ক্লাউড)। হুবহু এক হলেও
           `notifyDataSetChanged()` প্রতিবার সব সারি নতুন করে আঁকত — চোখে
           সেটাই ঝিলিক। এখন হুবহু এক হলে আর বসানো হয় না।
           ⛔ এক চুল আলাদা হলেই আগের মতোই পুরো বসে। */
        if (items.size == newItems.size && items == newItems) return
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemCollectionRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        // 🟢🔒 V640 (২৪.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ পাশ) — নামের আগে
        // সিরিয়াল নম্বর (এই তালিকায় কততম, position+1)। ⛔ ডেটা/সাজানোর
        // নিয়ম কিছু বদলায়নি — শুধু দেখানোর একটা বাড়তি ঘর।
        holder.binding.tvSerial.text = (position + 1).toString()
        holder.binding.tvName.text = item.name.ifBlank { "Walk-in" }
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)
        // 🆔 TK-এর নিয়ম (28.07.2026): নাম ও মোবাইলের সঙ্গে Patient ID-ও।
        // ID না থাকলে (ওয়াক-ইন / মেডিসিন) আগের মতোই শুধু নম্বর দেখায়।
        // 🔒 B578 (TK-অনুমোদিত প্রুফ + গ্লোবাল রুল ১৬): মোবাইল সবুজ+bold (ট্যাপ →
        // auto-call), ID ধূসর — এক TextView-এ SpannableString দিয়ে দু'রঙ।
        val pid = item.patientId.trim()
        if (digits.length == 10 || pid.isNotBlank()) {
            // 🔵 B611 (10.08.2026, TK-অনুমোদিত প্রুফ): মোবাইল লাইনে আর 🆔+ID জোড়া
            // হয় না (জায়গা কম বলে ভেঙে ID পরের লাইনে নেমে যেত)। এখন মোবাইল শুধু
            // সবুজ+bold (ট্যাপ→কল), আর ID নিচের আলাদা `idRow`-এ ছোট চিপসহ। ⛔ ডেটা
            // অপরিবর্তিত — শুধু দেখানোর সাজানো।
            // 🔵 B612 (10.08.2026, TK-অনুমোদিত): 📞 আইকন বাদ, নম্বর +91 দিয়ে শুরু।
            val phonePart = if (digits.length == 10) "+91 $digits" else digits
            val sb = android.text.SpannableStringBuilder(phonePart)
            sb.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#2F7D4E")), 0, phonePart.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, phonePart.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.binding.tvMobile.text = sb
            holder.binding.tvMobile.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvMobile.visibility = android.view.View.GONE
        }
        // 🔵 B611: Patient ID নিজের লাইনে — থাকলে দেখাও (ছোট বেগুনি "ID" চিপ + নম্বর),
        // না থাকলে (walk-in / medicine) পুরো সারি লুকানো।
        if (pid.isNotBlank()) {
            holder.binding.tvPatientId.text = pid
            holder.binding.idRow.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.idRow.visibility = android.view.View.GONE
        }
        // TK-REQUESTED (2026-07-18): long-press to copy name/mobile. আলাদা জেসচার,
        // তাই সারির অন্য ট্যাপ অক্ষত।
        holder.binding.tvName.copyOnLongPress("Name", item.name)
        if (digits.length == 10) {
            // 🔵 B612: long-press করলে "+91 নম্বর" copy হয় (এক-চাপ আগের মতোই কল)।
            holder.binding.tvMobile.copyOnLongPress("Mobile", "+91 $digits")
            // 🔒 B578 (গ্লোবাল রুল ১৬): মোবাইলে ট্যাপ → auto-call (সব পর্দার এক CallChooser)।
            holder.binding.tvMobile.setOnClickListener {
                (context as? android.app.Activity)?.let { act -> CallChooser.open(act, digits) }
            }
        } else {
            holder.binding.tvMobile.setOnClickListener(null)
        }
        // 🔒 B572: নামে ট্যাপ → History (Full Journey)।
        holder.binding.tvName.setOnClickListener { onNameClick(item) }
        // 🔒 B575/B576 (TK-অনুমোদিত প্রুফ): নামের পাশে রোগ (chip, ইমোজি ছাড়া) +
        // নিচে ঠিকানা (দু'লাইন)।
        val dens = holder.binding.root.resources.displayMetrics.density
        if (item.disease.isNotBlank()) {
            holder.binding.tvDisease.text = item.disease.uppercase()
            holder.binding.tvDisease.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 8f * dens; setColor(android.graphics.Color.parseColor("#FDE7EA"))
            }
            holder.binding.tvDisease.visibility = android.view.View.VISIBLE
        } else holder.binding.tvDisease.visibility = android.view.View.GONE

        /* 🔴🔒 V566 (TK, ২২.০৮.২০২৬): *"এখানেও যদি কোন আরএমপির পেশেন্ট হয়ে
           থাকে অথবা আনএক্সপেক্টেড টাইমের পেশেন্ট হয়ে থাকে সেটাও যেন শো করে"*
           ⛔ যেটা নেই সেটার চিপ বসে না — খালি চিপ কখনো দেখানো হয় না। */
        val rmpTag = PaymentModel.rmpTagOf(item.refBy, item.refDoctor)
        if (rmpTag.isNotBlank()) {
            holder.binding.tvRmpTag.text = rmpTag
            holder.binding.tvRmpTag.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 20f
                setColor(android.graphics.Color.parseColor("#E8F6EC"))
                setStroke(1, android.graphics.Color.parseColor("#BFE9CE"))
            }
            holder.binding.tvRmpTag.visibility = android.view.View.VISIBLE
        } else holder.binding.tvRmpTag.visibility = android.view.View.GONE

        val timeTag = PaymentModel.unexpectedTagOf(item.timeType)
        if (timeTag.isNotBlank()) {
            holder.binding.tvTimeTag.text = timeTag
            holder.binding.tvTimeTag.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 20f
                setColor(android.graphics.Color.parseColor("#FFF6E5"))
                setStroke(1, android.graphics.Color.parseColor("#F0D9A8"))
            }
            holder.binding.tvTimeTag.visibility = android.view.View.VISIBLE
        } else holder.binding.tvTimeTag.visibility = android.view.View.GONE
        if (item.address.isNotBlank()) {
            holder.binding.tvAddress.text = "📍 " + addrTwoLines(item.address)
            holder.binding.tvAddress.visibility = android.view.View.VISIBLE
        } else holder.binding.tvAddress.visibility = android.view.View.GONE
        // source (Treatment/Medicine) — B575-এ লুকানো, তবু text সেট থাকে (ক্ষতি নেই)।
        holder.binding.tvSource.text = item.source
        holder.binding.tvAmount.text = "₹${"%,.0f".format(item.amount)}"
        holder.binding.tvMode.text = if (item.cashAmount > 0.0 && item.onlineAmount > 0.0)
            "CASH + ONLINE" else item.mode
        // 🔒 B576 (TK-অনুমোদিত প্রুফ): টাকা+মোড গোল রাউন্ড বক্সে, মোড অনুযায়ী রঙ —
        // ক্যাশ সবুজ (#E8F7EE/#BFE6CD/#0B7A34), অনলাইন/UPI নীল (#E7F0FD/#C3DCFA/#1457B8)।
        // Mixed row-এর accounting এই রঙের উপর নির্ভর করে না; split fields-ই source of truth.
        /* 🔴🔴 V487 (20.08.2026, TK-রিপোর্ট ছবিসহ): *"আজ টাকা ফেরত দেওয়া হয়েছে,
           সংখ্যাটা মাইনাস — তবু রঙটা লাল হল না কেন?"*

           আসল কারণ: রঙ ঠিক হত **শুধু মোড দেখে** (ক্যাশ ⇒ সবুজ, অনলাইন ⇒ নীল)।
           টাকাটা জমা না ফেরত — সেটা রঙ ঠিক করার সময় দেখাই হত না। তাই
           ₹-14,000 (ফেরত) আর ₹14,000 (জমা) দুটোই একই সবুজ দেখাত।

           এখন: টাকা মাইনাস হলে (ফেরত) পুরো বাক্স লাল। জমা হলে আগের মতোই
           ক্যাশ সবুজ · অনলাইন নীল — এক চুলও বদলায়নি।
           ⛔ কোনো হিসাব/ডেটা নয় — শুধু রঙ। */
        val isRefund = item.amount < 0.0
        val isOnline = item.onlineAmount != 0.0 && item.cashAmount == 0.0
        val boxFill = if (isRefund) "#FDECEC" else if (isOnline) "#E7F0FD" else "#E8F7EE"
        val boxStroke = if (isRefund) "#F3C2C2" else if (isOnline) "#C3DCFA" else "#BFE6CD"
        val boxText = if (isRefund) "#B3261E" else if (isOnline) "#1457B8" else "#0B7A34"
        holder.binding.amountBox.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 12f * dens
            setColor(android.graphics.Color.parseColor(boxFill))
            setStroke((1f * dens).toInt(), android.graphics.Color.parseColor(boxStroke))
        }
        holder.binding.tvAmount.setTextColor(android.graphics.Color.parseColor(boxText))
        holder.binding.tvMode.setTextColor(android.graphics.Color.parseColor(boxText))
        // 🔵 B612 (10.08.2026, TK-অনুমোদিত): পেমেন্টের সময় (থাকলে) — টাকা+মোডের
        // নিচে ছোট ধূসর (ঘড়ি-আইকন ছাড়া)। না থাকলে (পুরনো সারি/ক্যাশ) লুকানো।
        if (item.time.isNotBlank()) {
            holder.binding.tvTime.text = item.time
            holder.binding.tvTime.visibility = android.view.View.VISIBLE
        } else holder.binding.tvTime.visibility = android.view.View.GONE
        holder.binding.root.setOnClickListener { onRowClick(item) }
    
        // 🔴🔴🔒 V449 (TK-রিপোর্ট ১৮.০৮.২০২৬ — "লগইন হয়ে যায় কিন্তু কার্যকরী কিছু হয় না")।
        // আসল কারণ: NoBengali-এর অটো-সুইপ শুধু পর্দার layout-পাসে চলে; RecyclerView
        // rebind/scroll নতুন layout-পাস তৈরি না করেই লেখা বসিয়ে দেয়, তাই তালিকার
        // সারির বাংলা কখনো ঢাকাই পড়ত না। এখন প্রতিটা বাইন্ডের শেষেই সরাসরি সুইপ —
        // বাংলা-বন্ধ না থাকলে কিছুই করে না (activeCache false ⇒ সাথে সাথে ফেরত)।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }

    // 🔒 B575: গ্লোবাল দু'লাইন ঠিকানা নিয়ম (থানা-চিহ্নের আগে ভাঙা) — PaymentActivity-র
    // addressTwoLines-এর হুবহু একই। চিহ্ন না পেলে এক লাইন (কিছু ভাঙে না)।
    private fun addrTwoLines(addressIn: String): String {
        val address = addressIn.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ: "সমস্ত জায়গায় ক্যাপিটাল লেটারই করবেন") — শুধু **দেখানোর** সময় বড় হাতে; ডেটাবেসে যা লেখা আছে তা এক অক্ষরও বদলায় না।
        if (address.isBlank()) return address
        fun strip(s: String) = s.replace(Regex("(?i)Vill:?|PO:?|PS:?|Dist:?|PIN:?"), "").replace(Regex("\\s*,\\s*"), ", ").trim().trim(',').trim()
        val markers = listOf("PS:", "P.S", "P/S", "Thana", "থানা", "Police Station")
        var idx = -1
        for (m in markers) { val i = address.indexOf(m, ignoreCase = true); if (i > 0 && (idx == -1 || i < idx)) idx = i }
        if (idx <= 0) return strip(address)
        val first = strip(address.substring(0, idx).trim().trimEnd(',').trim())
        val second = strip(address.substring(idx).trim())
        return if (first.isBlank() || second.isBlank()) strip(address) else "$first\n$second"
    }
}
