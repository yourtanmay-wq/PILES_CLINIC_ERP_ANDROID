package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemFollowupCardBinding

class FollowUpAdapter(
    private val context: Context,
    private var items: List<FollowUpItem>,
    private val onCall: (FollowUpItem) -> Unit,
    private val onWhatsApp: (FollowUpItem) -> Unit,
    private val onRemark: (FollowUpItem) -> Unit,
    private val onNextFollow: (FollowUpItem) -> Unit,
    private val onPayment: (FollowUpItem) -> Unit = {},
    private val onPrescription: (FollowUpItem) -> Unit = {},
    private val onView: (FollowUpItem) -> Unit = {},
    private val onStatusMenu: (FollowUpItem) -> Unit = {},
    private val onCallSignal: (FollowUpItem) -> Unit = {},
    private val onEdit: (FollowUpItem) -> Unit = {},
    private val onPhotoEdit: (FollowUpItem) -> Unit = {}
) : RecyclerView.Adapter<FollowUpAdapter.ViewHolder>() {

    private var sigTapTime = 0L
    private var sigTapCount = 0
    private var sigTapItemId = ""

    private val density = context.resources.displayMetrics.density

    fun updateItems(newItems: List<FollowUpItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemFollowupCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFollowupCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    /** Rounded signal bar, matching the WebView .signalBars pills. */
    private fun barDrawable(on: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = 3f * density
        setColor(Color.parseColor(if (on) "#0FA83B" else "#D8E4F2"))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding
        try {

        // 🟢🔒 V648 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) — Follow-up-এর আসল
        // কার্ডের সাথে মেলানো সিরিয়াল নম্বর ব্যাজ।
        b.tvFuSerial.text = (position + 1).toString()

        // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে মোবাইল দুইবার দেখাত।
        // 🟢🔒 V694 (২৬.০৮.২০২৬, TK-নির্দেশ ছবিসহ) — নামের আগে আর 👤 নয়:
        //   বাঁ পাশে এখন লাল সিরিয়াল ব্যাজটাই থাকে, ঠিক আসল Follow-up
        //   কার্ডের মতো (`buildFollowCard`: সিরিয়াল থাকলে 👤 বসে না)।
        b.tvName.text = item.name.ifBlank { "UNKNOWN" }
        b.tvMobile.text = "📞 ${formatMobileForDisplay(item.mobile)}"
        /* 📋🔒 V827 (২৯.০৮.২০২৬, TK-নির্দেশ) — নামেও লম্বা চাপে কপি, ঠিক
           নিচের নম্বরটার মতোই (Draft-এর কার্ডও একই দেখতে, তাই একই আচরণ)।
           ⛔ নম্বর কপির পুরনো কোডে এক অক্ষরও হাত পড়েনি। */
        b.tvName.setOnLongClickListener {
            if (item.name.isBlank()) false
            else {
                com.tkbiswas.pilesclinic.native.Clip.copy(it.context, "Name", item.name.trim())
                android.widget.Toast.makeText(it.context, "Name copied", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }
        b.tvMobile.setOnLongClickListener {
            com.tkbiswas.pilesclinic.native.Clip.copy(it.context, "mobile", item.mobile)   // 🤫 V772
            android.widget.Toast.makeText(it.context, "Mobile copied", android.widget.Toast.LENGTH_SHORT).show()
            true
        }
        b.tvBranch.text = item.branch.ifBlank { "-" }.uppercase()
        b.tvDisease.text = item.disease.ifBlank { "-" }.uppercase()
        // TK-APPROVED (2026-07-25, via photo proof): label removed -- the
        // box's own light-green color now makes it recognizable at a
        // glance, applied consistently project-wide.
        // TK-REPORTED (2026-07-26): Visit/Patient cards were still showing the
        // internal placeholder "Registered (syncing…)". That text was only ever
        // meant to be a few-seconds transient state, but rows saved with it in
        // an older build keep showing it forever. It is now replaced at DISPLAY
        // time with the plain sentence the app itself writes for such a row, so
        // nothing in the database is touched and no other remark is affected.
        val shownRemark = item.lastRemark.trim()
        val syncingPlaceholder = shownRemark.startsWith("Registered (syncing", ignoreCase = true)
        b.tvRemark.text = when {
            syncingPlaceholder -> "Registered patient / Visit created"
            shownRemark.isBlank() -> "No remark"
            else -> shownRemark
        }

        val regDate = if (item.recordDate.isNotBlank()) FollowUpModel.displayDate(item.recordDate) else ""

        // Left side + right slot, mapped exactly like the WebView fuCard:
        //   Enquiry (Inquiry)  -> LEFT call-signal      | RIGHT Today/Overdue/Xd badge
        //   Visit   (Patient)  -> LEFT avatar+VISITED   | RIGHT 👣 Visit / Advance pill
        //   Patient (Treatment)-> LEFT avatar+PATIENT   | RIGHT Prescription + payment ring
        when (item.stage) {
            "Inquiry" -> {
                // 🟢🔒 V694 — Enquiry-তে বাঁদিকের কলাম **থাকে** (কল-সিগন্যাল
                //   ওখানেই বসে) — আসল কার্ডেও তাই। আর পিল/আইডির সারি এখানে
                //   নেই। ⚠️ দুটোই এখানে **স্পষ্ট করে** বসানো হলো, নইলে
                //   RecyclerView পুরনো সারি আবার ব্যবহার করলে Patient কার্ডের
                //   লুকানো/দেখানো অবস্থাটা Enquiry-তে থেকে যেত।
                b.llLeft.visibility = View.VISIBLE
                b.llIdRow.visibility = View.GONE
                b.llCallSignal.visibility = View.VISIBLE
                b.llPhoto.visibility = View.GONE
                b.tvVisitAdvance.visibility = View.GONE
                b.llPayment.visibility = View.GONE

                val n = item.callCount.coerceIn(0, 5)
                val wifiDrawable = when (n) {
                    0 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_0
                    1 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_1
                    2 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_2
                    3 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_3
                    4 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_4
                    else -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_5
                }
                b.ivWifiSignal.setImageResource(wifiDrawable)
                b.ivWifiSignal.contentDescription = "Enquiry calls: $n of 5"
                b.tvSignalDate.text = regDate
                // TK-REQUESTED ADDITION (2026-07-23): small, clear time-type
                // badge on the Enquiry card -- green "Official Time" or purple
                // "Unexpected Time". Hidden when the row has no timeType (older
                // rows not yet backfilled), so nothing looks broken.
                val tt = item.timeType.trim()
                when {
                    tt.equals("Unexpected Time", ignoreCase = true) -> {
                        b.tvTimeType.text = "🌙 Unexpected Time"
                        b.tvTimeType.setBackgroundColor(android.graphics.Color.parseColor("#6A4C93"))
                        b.tvTimeType.visibility = View.VISIBLE
                    }
                    tt.equals("Official Time", ignoreCase = true) -> {
                        b.tvTimeType.text = "Official Time"
                        b.tvTimeType.setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                        b.tvTimeType.visibility = View.VISIBLE
                    }
                    else -> b.tvTimeType.visibility = View.GONE
                }
                // Final lock: triple-tap the "Enquiry" status text for Continue / Reject.
                TripleTapEdit.attach(b.tvSignalLabel) { onStatusMenu(item) }
            }
            "Patient" -> { // Visit tab
                b.llCallSignal.visibility = View.GONE
                // 🟢🔒 V694 — আসল কার্ডে Visit/Patient-এ বাঁদিকের ৬২dp কলামটা
                //   পুরোপুরি বাদ (TK-APPROVED 27.07.2026), তাই নাম-মোবাইল
                //   একদম বাঁ প্রান্ত থেকে শুরু হয়। এখানেও তাই।
                b.llPhoto.visibility = View.GONE
                b.llLeft.visibility = View.GONE
                b.tvVisitAdvance.visibility = View.VISIBLE
                b.llPayment.visibility = View.GONE
                // TK-REQUESTED (2026-07-23): time-type badge is Enquiry-only;
                // hide it here so a recycled view can't carry it over.
                b.tvTimeType.visibility = View.GONE

                // 🟢🔒 V694 — পিল ও আইডি হারায়নি: ট্যাগের নিচে নিজের সারিতে।
                b.llIdRow.visibility = View.VISIBLE
                b.tvStatusPill.text = "VISITED"
                b.tvIdOnRow.text = item.patientId.ifBlank { regDate }
                b.tvIdOnRow.visibility = if (b.tvIdOnRow.text.isNullOrBlank()) View.GONE else View.VISIBLE
                b.tvVisitAdvance.setOnClickListener { onPayment(item) }
                TripleTapEdit.attach(b.tvStatusPill) { onStatusMenu(item) }
                TripleTapEdit.attach(b.tvIdOnRow) { onEdit(item) }
            }
            else -> { // Treatment -> Patient tab
                b.llCallSignal.visibility = View.GONE
                // 🟢🔒 V694 — উপরের Visit কার্ডের একই কথা।
                b.llPhoto.visibility = View.GONE
                b.llLeft.visibility = View.GONE
                b.tvVisitAdvance.visibility = View.GONE
                b.llPayment.visibility = View.VISIBLE
                // TK-REQUESTED (2026-07-23): time-type badge is Enquiry-only.
                b.tvTimeType.visibility = View.GONE

                // 🟢🔒 V694 — পিল ও আইডি ট্যাগের নিচে নিজের সারিতে।
                b.llIdRow.visibility = View.VISIBLE
                b.tvStatusPill.text = "PATIENT"
                b.tvIdOnRow.text = item.patientId.ifBlank { regDate }
                b.tvIdOnRow.visibility = if (b.tvIdOnRow.text.isNullOrBlank()) View.GONE else View.VISIBLE
                TripleTapEdit.attach(b.tvStatusPill) { onStatusMenu(item) }
                TripleTapEdit.attach(b.tvIdOnRow) { onEdit(item) }

                val pct = if (item.bill > 0) Math.min(100.0, Math.round(item.paid / item.bill * 100.0).toDouble()).toInt() else 0
                // 🔴🔒 V683 (২৫.০৮.২০২৬) — সলিড চাকতির বদলে আসল কার্ডের
                // PaymentRingView + আলাদা রঙিন Bill/Due পিল (FollowUpActivity.
                // buildFollowCard()-এর হুবহু একই মান/রং)।
                b.paymentRing.percent = pct
                val due = Math.max(0.0, item.bill - item.paid)
                /* 🏷️🔒 V1508 (১৬.০৯.২০২৬, TK-নির্দেশ — MD ANARUL HOWK): "Bill"
                   চিপ ছাড় দেওয়া থাকলে আসল (ছাড়ের আগের) বিল দেখায়। ⛔ Due/%
                   হিসাব উপরের/নিচের লাইনে `item.bill`-ই ব্যবহার করে, অপরিবর্তিত। */
                val displayBillChip = if (item.billBeforeDiscount > 0.0 && item.billBeforeDiscount != item.bill)
                    item.billBeforeDiscount else item.bill
                b.tvBillPill.text = "Bill\n₹${"%,.0f".format(displayBillChip)}"
                b.tvDuePill.text = "Due\n₹${"%,.0f".format(due)}"
                b.paymentRing.setOnClickListener { onPayment(item) }
                b.tvPrescription.setOnClickListener { onPrescription(item) }
                TripleTapEdit.attach(b.tvBillPill) { onPayment(item) }
                TripleTapEdit.attach(b.tvDuePill) { onPayment(item) }
            }
        }

        // Badge: only on the Enquiry (Inquiry) tab — Today Due / Overdue / X-Days,
        // matching the WebView badge logic.
        val days = FollowUpModel.daysUntil(item.nextFollow)
        if (item.stage != "Inquiry" || days == null) {
            b.tvBadge.visibility = View.GONE
        } else {
            val d: Int = days
            when {
                d < 0 -> setBadge(b, "⏰ Overdue", "#E5484D")
                // 🔒 খাতার সারি B202 (30.07.2026 রাত): FollowUpActivity.kt-এর
                // হুবহু একই ফিক্স — ⏰ ইমোজির ভিতরে ফোনের ফন্ট নিজে থেকে "১৭"
                // এঁকে রাখে (আসল তথ্য নয়), তাই ⏰-তে বদলানো হলো। এই ফাইলটা এই
                // মুহূর্তে প্রজেক্টের কোথাও ব্যবহার হয় না (dead code), তবু
                // ভবিষ্যতের নিরাপত্তার জন্য একইসঙ্গে ঠিক করা হলো।
                d == 0 -> setBadge(b, "⏰ Today Due", "#E5484D")
                else -> setBadge(b, "⏰ ${d}d Due", "#F79009")
            }
        }

        // 🟢🔒🔒 V649 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবিসহ — "আরো ভালোভাবে যাচাই
        // করুন") — আসল কারণ: এই আলাদা FollowUpAdapter "LAST CALL" লাইনটাই
        // কখনো দেখাত না, আর "NEXT CALL"-এর বদলে ভুল লেখা ("Next Follow up
        // Call:"/"Next Follow-up:") দেখাত — আসল অ্যাপে (TK-এর ছবিতে) দুটোই
        // ছোট, নির্দিষ্ট লেখা "LAST CALL ..." / "NEXT CALL ...", পাশাপাশি,
        // রিমার্ক-বাক্সের ভেতরে উপরে। FollowUpItem-এ lastCallDate/lastCallBy/
        // lastCallTime আগে থেকেই আছে (আগের V543/B39-এ প্রমাণিত ঘর, নতুন কিছু
        // যোগ করা হয়নি) — শুধু এখানে পড়া হচ্ছিল না।
        val lastDt = item.lastCallDate
        val lastBy = item.lastCallBy
        val lastTm = item.lastCallTime
        val lastWhen = if (lastDt.isNotBlank()) {
            val dateTxt = FollowUpModel.displayDate(lastDt)
            val tm = formatTime12(lastTm)
            dateTxt + (if (tm.isNotBlank()) " : $tm" else "")
        } else ""
        /* 🆕🔒 V850 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — TK: "যেগুলো এনকোয়ারি
           কার্ড সেগুলোতে লাস্ট কল থাকবে; যেগুলো রেজিস্ট্রেশন করা হয়েছে সেখানে
           লিখতে হবে কত তারিখে রেজিস্ট্রেশন হয়েছে এবং কে রেজিস্ট্রেশন করেছিল"।
           ⛔ `regDate` ফাঁকা হলে নিচের লাইনটা **হুবহু আগের মতোই** চলে — তাই
              Follow-up ও Trash-প্রিভিউর কার্ড এক অক্ষরও বদলায়নি। */
        val regDt = item.regDate
        /* 🟢🔒 V874 (৩০.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত) — Follow-up
           কার্ডের হুবহু একই বদল এখানেও (খাতার নিয়ম ৭): REGISTERED-এর লাইনটা
           আগের মতোই, তার **নিচের লাইনে** LAST CALL-এর তারিখ · সময় · কে।
           ⛔ কল না হয়ে থাকলে দ্বিতীয় লাইনটা বসেই না — কার্ড আগের মতোই।
           ⛔ TK: *"NEXT CALL এ time থাকবে না"* ⇒ ডান দিকের লেখা অপরিবর্তিত। */
        val regLastLine = if (lastDt.isNotBlank())
            "\nLAST CALL $lastWhen" + (if (lastBy.isNotBlank()) " (${lastBy})" else "") else ""
        /* 🔒 V931 (৩১.০৮.২০২৬, TK ডেমো প্রুফ দেখে "হ্যাঁ পাশ, দুটোই বসিয়ে দিন") —
           TK: *"Registered এর তারিখ এবং কে করেছে এখানে থাকবে না — View All এ
           ক্লিক করলে দেখা যায় শুধুমাত্র সেটাই থাকবে"*।
           ⚠️ ৩০.০৮.২০২৬-এ TK এই লাইনটা *"যা আছে তাই থাকবে"* বলে বন্ধ করেছিলেন;
              ৩১.০৮-এ তিনি নিজে উল্টো নির্দেশ দিয়েছেন — আজকেরটাই চলবে।
           ⛔ তথ্য কোথাও মোছা হয়নি — REGISTERED-এর তারিখ ও কে করেছে, দুটোই
              View All-এ আগের মতোই আছে; শুধু কার্ড থেকে লাইনটা উঠল। */
        b.tvLastCall.text = if (lastDt.isNotBlank())
            "LAST CALL $lastWhen" + (if (lastBy.isNotBlank()) " (${lastBy})" else "")
        else "LAST CALL —"
        b.tvNextFollow.text = if (item.nextFollow.isNotBlank())
            "NEXT CALL ${FollowUpModel.displayDate(item.nextFollow)}" else "NEXT CALL —"

        b.btnCall.setOnClickListener { onCall(item) }
        b.btnWhatsApp.setOnClickListener { onWhatsApp(item) }
        b.btnRemark.setOnClickListener { onView(item) }
        b.tvRemark.setOnClickListener { onRemark(item) }
        b.btnNextFollow.setOnClickListener { onNextFollow(item) }
        // Final correction rule: any displayed patient detail can be corrected by 3 quick taps.
        TripleTapEdit.attach(b.tvName) { onEdit(item) }
        TripleTapEdit.attach(b.tvMobile) { onEdit(item) }
        TripleTapEdit.attach(b.tvBranch) { onEdit(item) }
        TripleTapEdit.attach(b.tvDisease) { onEdit(item) }
        } catch (e: Exception) { }
    
        // 🔴🔴🔒 V449 (TK-রিপোর্ট ১৮.০৮.২০২৬ — "লগইন হয়ে যায় কিন্তু কার্যকরী কিছু হয় না")।
        // আসল কারণ: NoBengali-এর অটো-সুইপ শুধু পর্দার layout-পাসে চলে; RecyclerView
        // rebind/scroll নতুন layout-পাস তৈরি না করেই লেখা বসিয়ে দেয়, তাই তালিকার
        // সারির বাংলা কখনো ঢাকাই পড়ত না। এখন প্রতিটা বাইন্ডের শেষেই সরাসরি সুইপ —
        // বাংলা-বন্ধ না থাকলে কিছুই করে না (activeCache false ⇒ সাথে সাথে ফেরত)।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }

    /** 🟢🔒 V649 (২৫.০৮.২০২৬) — "HH:mm" (24-ঘণ্টা, ডেটাবেসে যেমন সেভ থাকে)
     *  কে "h:mm a" (১২-ঘণ্টা, AM/PM) দেখানোর জন্য। ⛔ সেভ করা আসল সময় এক
     *  অক্ষরও বদলায় না — শুধু দেখানোর সময় রূপ বদলায়। ফাঁকা/ভুল ফরম্যাট
     *  হলে চুপচাপ ফাঁকা ফেরত যায় (কার্ড ভাঙে না)। */
    private fun formatTime12(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            /* 🔴🔒 V850 (নিজের যাচাইয়ে ধরা পড়া দোষ) — `history`-র `time` ঘরে
               কোথাও "HH:mm" জমা হয়, আবার কোথাও **পুরো ISO** ("2026-08-29T
               11:20:05.000Z" — যেমন `EnquiryModel.buildFollowUpRow()`)। আগে
               সরাসরি `:` দিয়ে ভাঙা হত, তাই ISO হলে প্রথম টুকরো "…T11" হয়ে
               `toInt()` ভেঙে যেত ⇒ সময়টা চুপচাপ উধাও হয়ে যেত।
               এখন 'T'-এর পরের অংশটাই নেওয়া হয়। ⛔ "HH:mm" আগের মতোই চলে। */
            val body = raw.trim().substringAfter('T', raw.trim())
            val parts = body.split(":")
            if (parts.size < 2) return ""
            var h = parts[0].trim().toInt()
            val m = parts[1].take(2).toInt()
            val ampm = if (h < 12) "AM" else "PM"
            if (h == 0) h = 12 else if (h > 12) h -= 12
            /* ⏰ V835 (TK-নির্দেশ): `3:15 PM` → `3.15 PM`। এই ফাংশনটা
               প্রজেক্টে **শুধু এখানেই** (LAST CALL লাইনে) চলে — খুঁজে দেখা। */
            "%d.%02d %s".format(h, m, ampm)
        } catch (_: Throwable) { "" }
    }

    /** Display-only formatting. Stored number and Call/WhatsApp logic remain unchanged. */
    private fun formatMobileForDisplay(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            raw.trim().startsWith("+") -> raw.trim()
            else -> raw.trim()
        }
    }

    private fun setBadge(b: ItemFollowupCardBinding, text: String, colorHex: String) {
        b.tvBadge.visibility = View.VISIBLE
        b.tvBadge.text = text
        val drawable = GradientDrawable().apply {
            cornerRadius = 8f * density
            setColor(Color.parseColor(colorHex))
        }
        b.tvBadge.background = drawable
    }
}
