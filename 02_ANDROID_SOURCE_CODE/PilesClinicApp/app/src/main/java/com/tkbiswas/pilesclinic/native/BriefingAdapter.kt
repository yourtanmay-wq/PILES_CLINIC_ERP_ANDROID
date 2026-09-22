package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemBriefingCardBinding

class BriefingAdapter(
    private val context: Context,
    private var items: List<Briefing>,
    private val onSeen: (Briefing) -> Unit,
    private val onReply: (Briefing) -> Unit,
    private val onDelete: (Briefing) -> Unit,
    /** 🔒 খাতার সারি B100 (TK, 29.07.2026 রাত ১১.১০): স্টাফের পাঠানো ডিলিটের
     *  অনুরোধ মাস্টার এক চাপে অনুমোদন করবেন। ডিফল্ট ফাঁকা, তাই পুরনো কোনো
     *  ডাক ভাঙে না। */
    private val onApproveDelete: (Briefing) -> Unit = {},
    /* 🟢🔒 V1134 (০৬.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK: *"আমি যদি
       অনুমতি না দেই, তাহলে আমাকে কোনটা চাপতে হবে?"* ⇒ ডিলিট-অনুরোধ কার্ডে
       এখন **Reject** বোতাম। ডিফল্ট ফাঁকা, তাই অন্য কোনো ব্যবহার ভাঙে না। */
    private val onReject: (Briefing) -> Unit = {},
    /** 🔴 B281 (02.08.2026): "Refund request" নোটিশে Approve/Reject বোতাম চাপলে। */
    private val onApproveRefund: (Briefing) -> Unit = {},
    /** 🆕 B419 (04.08.2026): "Chamber reopen request" নোটিশে Approve চাপলে। */
    private val onApproveReopen: (Briefing) -> Unit = {},
    /** 🆕 B467 (05.08.2026, TK-নির্দেশ) — মেসেজে থাকা ফোন-নম্বরে ট্যাপ করলে
     *  ডাকা হয় (Enquiry/Registration/Payment নোটিশের মতোই — নতুন প্যারামিটার,
     *  ডিফল্ট ফাঁকা, তাই পুরনো কোনো caller ভাঙে না)। */
    private val onCallNumber: (String) -> Unit = {},
    /** 🔴🔒 V501 (TK-নির্দেশ ২১.০৮.২০২৬, ফটো-প্রুফ অনুমোদিত) — নোটিশের
     *  "View" বোতাম: ওই নম্বরের রোগীর পাতা খোলে। ডিফল্ট ফাঁকা, তাই পুরনো
     *  কোনো caller ভাঙে না। */
    private val onViewRecord: (Briefing, String) -> Unit = { _, _ -> },
    /* 🟢🔒 V692 (TK, ২৬.০৮.২০২৬) — ⚠️ Overdue Follow-up Alert-এর "View"।
       ডিফল্ট ফাঁকা, তাই এই অ্যাডাপ্টারের পুরনো ব্যবহারকারীরা অক্ষত। */
    private val onViewOverdue: (Briefing) -> Unit = { },
    /* 📋🔒 V1435 (তালিকা ৫৫৪, TK "পাশ") — "Daily/Monthly Report submitted" নোটিশের
       "View": রিপোর্টটাই খোলে। ডিফল্ট ফাঁকা, পুরনো caller অক্ষত। */
    private val onViewReport: (Briefing) -> Unit = { },
    private val isMaster: Boolean = false,
    /** 🆕 TK-নির্দেশ (07.08.2026) — একসাথে অনেক অনুমোদন: কোনো কার্ড বাছাই/
     *  বাছাই-বাতিল হলে Activity-কে জানায় (নিচের "একসাথে অনুমোদন" বার দেখাতে)।
     *  ডিফল্ট ফাঁকা, তাই পুরনো কোনো caller ভাঙে না। */
    private val onSelectChanged: () -> Unit = {}
) : RecyclerView.Adapter<BriefingAdapter.ViewHolder>() {

    /** 🔴 V501 — মোবাইল নম্বরের নিয়ম এক জায়গায় (আগে ফাংশনের ভিতরে ছিল),
     *  যাতে "ক্লিকযোগ্য নম্বর" আর "View বোতাম" কখনো আলাদা নিয়মে না চলে।
     *
     * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — *"এখানে ভিউ একটা অপশন থাকার
     * দরকার ছিল... কিন্তু আপনি সেটা না করে রিপ্লাই রেখেছেন"*)।
     *
     * **সত্যি কথা:** View বোতামটা V501-এ TK-এর আগের নির্দেশেই বানানো হয়েছিল
     * (`btnViewRecord`, নিচে দেখুন)। কিন্তু সেটা **কখনো দেখাই যেত না**, কারণ
     * নিচের নিয়মটা ভুল ছিল —
     *
     *      পুরোনো:  (\+91[\-\s]?)?\b[6-9]\d{9}\b
     *
     * এখানে `\b` (শব্দের সীমানা) `+91`-এর ঠিক পরেই বসানো। `+91 7099468221`
     * (মাঝে ফাঁকা) হলে মেলে, কিন্তু **`+917099468221` (গায়ে গায়ে লাগানো)**
     * হলে `1` আর `7`-এর মাঝে কোনো সীমানা নেই, তাই **মেলেই না**।
     * TK-এর ডেটাবেসে নম্বর ঠিক ঐভাবেই জমা — তাই View চিরকাল লুকানো থাকত, আর
     * নম্বরটাও নীল/ক্লিকযোগ্য হত না (TK-এর ছবিতে নম্বরটা কালো — সেটাই প্রমাণ)।
     * ⇒ আমি `+91` ছাড়া নম্বর দিয়ে পরীক্ষা করেছিলাম, গায়ে-লাগানো `+91` দিয়ে
     *   করিনি। **এটা আমার ভুল ছিল।**
     *
     * **নতুন নিয়ম:** `(?<![0-9])(\+?91[\-\s]?)?([6-9][0-9]{9})(?![0-9])`
     *  · `+917099468221` · `+91 7099468221` · `917099468221` · `7099468221`
     *    — চারটেই ধরা পড়ে।
     *  · সামনে বা পিছনে আর কোনো অঙ্ক থাকলে ধরা পড়ে না, তাই বড় সংখ্যার
     *    (বিলের অঙ্ক, আইডি) মাঝ থেকে ভুল করে ১০ অঙ্ক তুলে নেওয়া হয় না।
     */
    private val MOBILE_PATTERN: java.util.regex.Pattern =
        java.util.regex.Pattern.compile("(?<![0-9])(\\+?91[\\-\\s]?)?([6-9][0-9]{9})(?![0-9])")

    /** 🆕 (07.08.2026) — বাছাই-করা অনুমোদন-নোটিশের id (শুধু মাস্টারের পর্দায়)। */
    val selectedIds = linkedSetOf<String>()

    fun clearSelection() { selectedIds.clear(); notifyDataSetChanged() }

    /** 🆕 (07.08.2026) — বর্তমানে দেখানো তালিকা (একসাথে অনুমোদনের সময় বাছাই-করা
     *  id থেকে আসল নোটিশ বার করতে লাগে)। ⛔ শুধু পড়ার জন্য। */
    fun itemsSnapshot(): List<Briefing> = items

    /** 👉🔒 V1438 (১৩.০৯.২০২৬, TK-নির্দেশ) — ডান দিকে সরিয়ে দেওয়ার জন্য ওই
     *  জায়গার নোটিশটা। ⛔ শুধু পড়ার জন্য, সীমার বাইরে হলে null। */
    fun itemAt(position: Int): Briefing? = items.getOrNull(position)

    /** 👉🔒 V1438 — ডান দিকে সরানো কার্ডটা তালিকা থেকে সঙ্গে সঙ্গে বাদ (শুধু
     *  পর্দায়; ক্লাউডের কাজটা ডাকার জায়গা আলাদা করে করে)। ক্লাউড ব্যর্থ হলে
     *  ডাকার জায়গা পুরো তালিকা আবার লোড করে, তাই কার্ডটা ফিরে আসে। */
    fun removeAt(position: Int) {
        if (position < 0 || position >= items.size) return
        val left = items.toMutableList()
        val gone = left.removeAt(position)
        items = left
        selectedIds.remove(gone.id)
        notifyItemRemoved(position)
    }

    fun updateItems(newItems: List<Briefing>) {
        items = newItems
        // তালিকা বদলালে আর-না-থাকা কার্ডের বাছাই বাদ (নইলে ভুল id থেকে যেত)।
        val alive = newItems.map { it.id }.toSet()
        selectedIds.retainAll(alive)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemBriefingCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBriefingCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    /**
     * V689: "Seen by N"-কে TextView-এর সাধারণ click-এর উপর নির্ভর না রেখে
     * লেখাটির ওই অংশকেই আসল clickable span করা হয়েছে। কিছু কার্ডে message-এর
     * LinkMovementMethod/RecyclerView touch handling-এর কারণে আগের listener
     * বাস্তবে tap পেত না, যদিও সংখ্যা ঠিক দেখা যেত।
     */
    private fun bindSeenBy(view: android.widget.TextView, prefix: String, item: Briefing) {
        val label = "Seen by ${item.seenCount}"
        val fullText = prefix + label
        val text = SpannableString(fullText)
        val openSeenList = {
            // বর্তমান model-list ছাড়াও raw row থেকে আবার পড়া—পুরনো/মিশ্র
            // notice card হলেও নামের তালিকা ফাঁকা হয়ে tap নষ্ট হবে না।
            val rawSeen = item.raw.optJSONArray("seen")
            val mobiles = if (item.seenBy.isNotEmpty()) item.seenBy else
                (0 until (rawSeen?.length() ?: 0))
                    .map { rawSeen?.optString(it, "").orEmpty() }
                    .filter { it.isNotBlank() }
            val names = mobiles.map { mobile ->
                StaffDirectory.findAccount(mobile)?.name ?: mobile
            }.distinct()
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setCustomTitle(PremiumAlert.header(context, "Seen by (${names.size})"))
                .setItems(names.toTypedArray(), null)
                .setPositiveButton("Close", null)
                .show().also { dialog -> PremiumAlert.paint(dialog) }
        }
        val start = fullText.indexOf(label)
        text.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) { openSeenList() }
            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.color = Color.parseColor("#1976B9")
                ds.isUnderlineText = true
            }
        }, start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.text = text
        view.movementMethod = LinkMovementMethod.getInstance()
        view.highlightColor = Color.TRANSPARENT
        view.isClickable = true
        // পুরো লাইন চাপলেও একই ফল—সাধারণ ব্যবহারকারীর জন্য tap target বড় থাকে।
        view.setOnClickListener { openSeenList() }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding

        /* 🟢🔒 V1123 (TK-অনুমোদিত ফটো-প্রুফ) — শিরোনাম থেকে ছবি-অক্ষর (emoji)
           বাদ, যাতে এক লাইনে পরিষ্কার পড়া যায়। ⛔ **শুধু দেখানোর সময়** —
           `item.title` (ডেটাবেসে সেভ করা লেখা, যেটা ধরে Approve/Reply/ছাঁকনি
           সব চলে) এক অক্ষরও বদলায় না, তাই কোনো নিয়ম ভাঙে না। */
        b.tvTitle.text = plainTitle(item.title).ifBlank { "(No title)" }
        // 🔒 TK-নির্দেশ (04.08.2026): এখন তারিখের সাথে সময়ও দেখা যাবে —
        // যাতে নোটিফিকেশনটা আজকের নাকি অনেক পুরনো, তা এক নজরেই বোঝা যায়।
        // 🔒 TK-নির্দেশ (04.08.2026, ছবিসহ — "সময় শো করবে, তাহলে নোটিফিকেশন
        // আজকের নাকি পুরনো তা বোঝার জন্য বারবার জিজ্ঞাসা করতে হবে না"):
        // এখন থেকে তারিখের সাথে সময়ও দেখা যাবে। `date` ঘরে শুধু তারিখ থাকে
        // (সময় নেই — BriefingModel.buildNewBriefing()), তাই আসল সময়ের জন্য
        // `raw` থেকে `createdAt` (পুরো ISO টাইমস্ট্যাম্প, সবসময় থাকে) পড়া হয়।
        // ⛔ ব্যর্থ হলে (createdAt ফাঁকা/পুরনো ফরম্যাট) আগের মতোই শুধু তারিখ।
        // 🔴🆕🔒 V433 (TK-নির্দেশ ১৮.০৮.২০২৬, স্ক্রিনশটসহ — *"সাধারণ নোটিফিকেশন,
        // এত হাইলাইট করে দেখানোর কিছু নেই"* · *"in time submit হয়েছে, তার জন্য
        // আমাকে কেন আবার রিপ্লাই দিতে হবে"* · *"Time ২ বার কেন? Role: master ·
        // Seen by 0 — এর মানে কি? এটা একটা সাধারণ জিনিস, তাহলে এটা নোটিশ কেন হবে"*)।
        // "Staff IN TIME"/"Staff OUT TIME" = শুধু **তথ্য জানানোর** কার্ড, কোনো
        // কাজ করার নেই। তাই এই কার্ডে — NOTICE চিপ নেই · Reply বোতাম নেই ·
        // "Role/Seen by" লাইন নেই · সময় একবারই (উপরে শুধু তারিখ, নিচে 🕐 সময়)।
        // ⛔ বাকি সব ধরনের নোটিশ (Delete/Refund/Reopen অনুরোধ, TK-এর নিজের লেখা
        //    নোটিশ) এক চুলও বদলায়নি — এখানে শুধু এই দুটো শিরোনামের জন্যই আলাদা।
        // ⛔ RecyclerView রিসাইকেল-নিরাপদ: প্রতিটা ঘরেই দুই দিকই (দেখাও/লুকাও)
        //    বসানো হয়েছে, তাই স্ক্রল করলে অন্য কার্ডে ভুল চেহারা যাবে না।
        val isPlainInfo = item.title.equals("Staff IN TIME", ignoreCase = true) ||
            item.title.equals("Staff OUT TIME", ignoreCase = true)
        val createdAtRaw = item.raw.s("createdAt")
        b.tvDate.text = when {
            // সময় দুবার দেখানো হত (উপরে পোস্টের সময়, নিচে 🕐 IN TIME) — এখানে
            // উপরে শুধু তারিখ, নিচে আসল IN TIME-টাই থাকে (কোনো তথ্য হারায় না)।
            isPlainInfo -> DateUtil.display(item.date)
            createdAtRaw.isNotBlank() -> DateUtil.displayWithTime(createdAtRaw)
            else -> DateUtil.display(item.date)
        }
        // 🆕 B467 (05.08.2026, TK-নির্দেশ — "নোটিফিকেশনে যে নাম্বার আসবে, এই
        // নাম্বারের উপর চাপ দিলে যেন কলিং অপশন খুলে যায়") — মেসেজের ভিতরে
        // ১০-অঙ্কের (ঐচ্ছিক +91 সহ) নম্বর খুঁজে সেটাকে ক্লিকযোগ্য করা হয়।
        // ⛔ মেসেজের বাকি লেখা/রং/সাইজ কিছুই বদলায়নি — শুধু নম্বরটুকু নীল ও
        // আন্ডারলাইন করা, বাকি স্বাভাবিক।
        /* 🎨🔒 V1141 (TK-অনুমোদিত ফটো-প্রুফ) — TK: *"Jalpaiguri-র পাশে তারিখ
           এবং সময় রাখুন"* ⇒ তারিখ-সময় এখন উপরের লাইনেই, নিচের আলাদা লাইনটা
           লুকানো — কার্ড অনেক কম উঁচু। ⛔ `tvDate`-এর id ও লেখা অক্ষত, শুধু দেখানো বন্ধ। */
        b.tvDate.visibility = View.GONE
        b.tvMessage.text = buildClickableMessage(item.message)
        b.tvMessage.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        b.tvMessage.highlightColor = android.graphics.Color.TRANSPARENT
        /* 🎨🔒 V1141 (TK: *"নিচে আবার Branch Jalpaiguri থাকবে না"*) — ব্রাঞ্চ
           উপরের লাইনেই আছে, তাই এখানে শুধু "Seen by N"। ⛔ চাপ দিলে কে দেখেছেন
              তার তালিকা আগের মতোই খোলে (V682)। */
        bindSeenBy(b.tvTargets, "", item)
        // 🔴🔒 V682 (২৫.০৮.২০২৬, TK-লাইভ-টেস্ট রিপোর্ট — "Seen by 1-এ চাপ
        // দিলে কে দেখেছে বোঝা যায় না") — এখন চাপলে নামের তালিকা দেখায়
        // (স্টাফ কোড → নাম, প্রমাণিত StaffDirectory.findAccount())। ⛔
        // ফাঁকা থাকলে চাপে কিছুই হয় না, বাকি সব আগের মতোই।
        // 🔴 V433 (TK): "Role: master · Seen by 0 — এর মানে কি?" — সাধারণ
        // তথ্য-কার্ডে লাইনটার কোনো কাজ নেই, তাই লুকানো। বাকি নোটিশে আগের মতোই।
        b.tvTargets.visibility = if (isPlainInfo) View.GONE else View.VISIBLE

        // 🎨 DESIGN #4 (2026-08-06, TK approved): sender avatar + line, a
        // priority chip, and the left colour bar. "Urgent" = action-required
        // request notices (delete / refund / reopen) → red; ordinary notices →
        // green. Derived from the title only, so NO new data is needed and no
        // existing logic changes. Both branches set every field, so a recycled
        // card never keeps a previous card's colour/chip.
        /* 🎨🔒 V1163 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ "খ") — গোল অক্ষরের
           চিহ্নটা (avatar) আর দেখানো হয় না; ওটা কোনো তথ্য দিত না, শুধু জায়গা
           নিত। ⛔ `tvAvatar`-এর id ও নিচের লেখা বসানো হুবহু আগের — শুধু লুকানো,
           তাই ভবিষ্যতে ফেরাতে চাইলে এক লাইনেই ফেরে। */
        b.tvAvatar.visibility = View.GONE
        val who = item.branch.ifBlank { item.targetsSummary }.ifBlank { "Notice" }
        val whenTxt = b.tvDate.text?.toString().orEmpty()   // 🎨 V1141 — একই লাইনে তারিখ-সময়
        fun withWhen(base: String) = if (whenTxt.isBlank()) base else "$base \u00b7 $whenTxt"
        b.tvWho.text = withWhen(who)
        val initialSrc = who.trim().ifEmpty { item.title.trim() }
        b.tvAvatar.text = (initialSrc.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'N').toString()
        // 🔴🆕🔒 TK-নির্দেশ (08.08.2026, ফটো-প্রুফে লক) — স্টাফ-নোটিশের (IN/OUT TIME
        // ইত্যাদি, বার্তায় "Staff : <কোড>" থাকে) হাইলাইটে **ব্রাঞ্চের বদলে স্টাফ কোড +
        // নাম** (কোড দিয়ে জমানো স্টাফ-তালিকা থেকে নাম) — master যেন বিভ্রান্ত না হয়।
        // ⛔ যেসব নোটিশে "Staff :" নেই (Admin briefing/Delete/Refund অনুরোধ) সেগুলো
        // আগের মতোই (ব্রাঞ্চ)। নাম না পেলে শুধু কোড — কিছুই ভাঙে না।
        /* 👤🔒 V1169 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ "খ") — TK:
           *"কোন staff in time দিয়েছে বোঝা যাচ্ছে না"* · *"নিচে স্টাফ নাম আর টাইম থাকবে না"*।

           আগে স্টাফের কোড ও নাম বসত **উপরের ধূসর হেডার-লাইনে**, আর কার্ডের ভিতরে
           লেখা হয়ে যেত শুধু `10.28 AM` — তাই কে দিয়েছে বোঝা যেত না, আর লাইনটা
           লম্বা হলে কেটেও যেত। এখন:
             উপরে  →  ব্রাঞ্চ · তারিখ : সময়
             পিলে  →  `IN TIME — স্টাফের নাম`
             নিচে  →  কিছু না (TK-নির্দেশে কোড ও সময় দুটোই বাদ)

           ⛔ `item.title` এক অক্ষরও বদলায়নি — শুধু **দেখানোর** লেখা। কারণ "দেখা
              হলে নিজে মুছে যাওয়া" ব্যবস্থা (B467) শিরোনামের হুবহু শব্দ মেলায়;
              শিরোনাম বদলালে সেটা ভেঙে যেত।
           ⛔ একই ধরনের সব কার্ডে (IN TIME · OUT TIME · Staff still at chamber)
              একই নিয়ম — বার্তায় `Staff :` ঘরটা থাকলেই।
           ⛔ নাম না পেলে কোডটাই বসে; সময় না পেলে শুধু তারিখ — কিছুই ভাঙে না। */
        val staffCode = extractField(item.message, "Staff")
        val staffNotice = staffCode != null
        var staffExtra = ""
        if (staffCode != null) {
            val nm = staffNameFor(staffCode)
            /* উপরের লাইন — ব্রাঞ্চ · তারিখ : সময়। সময়টা বার্তার ঘর থেকেই নেওয়া
               (পোস্টের সময় নয়) — এটাই আসল IN/OUT TIME। */
            val t = extractField(item.message, "Time")
            /* ⛔ তারিখটা সরাসরি `item.date` থেকে — `tvDate`-এর লেখা কার্ড-ভেদে
               কখনো সময়সহ হয়, তখন সময় দুবার বসে যেত (নিজে মেপে ধরা)। */
            val dateTxt = try { DateUtil.display(item.date) } catch (_: Throwable) { "" }
            val whenFull = when {
                t != null && dateTxt.isNotBlank() -> dateTxt + " : " + t
                t != null -> t
                dateTxt.isNotBlank() -> dateTxt
                else -> b.tvDate.text?.toString().orEmpty().trim()
            }
            b.tvWho.text = if (whenFull.isBlank()) who else who + " \u00b7 " + whenFull
            b.tvAvatar.text = ((nm ?: staffCode).firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'S').toString()
            /* পিলে — `Staff ` উপসর্গটা বাদ দিয়ে নামটা জুড়ে দেওয়া। */
            /* ⛔ `Staff ` উপসর্গটা শুধু IN/OUT TIME-এ বাদ যায় — নইলে
               "Staff still at chamber" হয়ে যেত "still at chamber" (নিজে মেপে ধরা)। */
            val fullTitle = plainTitle(item.title)
            val shortTitle =
                if (Regex("(?i)^Staff\\s+(IN|OUT)\\s+TIME$").matches(fullTitle.trim()))
                    fullTitle.trim().removePrefix("Staff ").removePrefix("staff ").trim()
                else fullTitle
            val whoName = (nm ?: staffCode).trim()
            b.tvTitle.text = if (whoName.isNotBlank()) shortTitle + " \u2014 " + whoName else shortTitle
            /* নিচের লাইন থেকে **Staff · Branch · Time** তিনটেই বাদ (TK:
               *"নিচে স্টাফ নাম আর টাইম থাকবে না"*) — তিনটেই উপরে/পিলে আছে।
               বাকি কিছু থাকলে (যেমন "Staff still at chamber"-এর `Reason :`)
               সেটুকু থেকে যায় — কোনো তথ্য হারায় না।
               ⛔ কিছুই না থাকলে লাইনটাই বসে না; নিচের `!rich` ঘরে
                  `staffExtra` দিয়ে সেটা ঠিক রাখা হয় (রিসাইকেল-নিরাপদ)। */
            staffExtra = item.message.split("\n")
                .map { it.trim() }
                .filter { line ->
                    line.isNotBlank() &&
                        !Regex("(?i)^[^A-Za-z]*(Staff|Branch|Time)\\s*:").containsMatchIn(line)
                }
                .joinToString("\n")
            if (staffExtra.isNotBlank()) b.tvMessage.text = buildClickableMessage(staffExtra)
        }

        // 🟢🔒 V641 (২৪.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ পাশ — "ব্রাঞ্চ দুই
        // জায়গায় কেন? এক লাইনে রাখলেই জায়গা কমে") — Delete/Refund/Reopen-
        // ধরনের গঠিত অনুরোধ-নোটিশে ব্রাঞ্চ + অনুরোধকারীর কোড এক লাইনে
        // (হেডারেই), তাই বার্তায় আর আলাদা "Name :"/"Branch :"/"Requested
        // by :" সারি লাগে না (Name শিরোনামেই আছে) — ডুপ্লিকেট বাদ দিয়ে
        // বার্তাটা ছোট হয়।
        // ⛔ শুধু **দেখানো** টেক্সট বদলায় — item.message (ডেটাবেসে সেভ করা
        //    আসল লেখা, Reply/cloud sync-এ যা ব্যবহার হয়) এক অক্ষরও বদলায় না।
        // ⛔ এই গঠন (Requested by :) নেই এমন বার্তায় (সাধারণ Briefing/
        //    auto-notice/Staff IN-OUT) একচুলও প্রভাব পড়ে না।
        /* 🟢🔒 V1123 (TK-রিপোর্ট: *"কোচবিহারের কোন স্টাফ করেছে তার তো নাম নেই?"*)
           — ঠিক কথা: এতদিন এই ধরনের কার্ডে শুধু **ব্রাঞ্চ** দেখাত। নামটা ডেটাতেই
           ছিল (`createdBy`), শুধু পর্দায় বসানো হয়নি। ⇒ যেসব বার্তায় "Staff :"
           বা "Requested by :" নেই, সেখানে পাঠানো স্টাফের কোড/নাম ব্রাঞ্চের পাশে
           বসে। ⛔ চেনা না গেলে (তালিকায় নেই) লাইনটা **হুবহু আগের মতোই** থাকে —
           বানানো কিছু বসানো হয় না। ⛔ প্রমাণিত সেই একই `staffNameFor()`। */
        /* 🎨🔒 V1163 (০৭.০৯.২০২৬, TK-নির্দেশ: *"staff-এর নাম ২ জায়গায় থাকবে না ·
           উপরে ব্রাঞ্চের পাশে স্টাফের নাম থাকবে না"*) — V1123-এ এখানে ব্রাঞ্চের
           পাশে পাঠানো স্টাফের নামটা বসত, আর V1141-এ ঠিক সেই নামটাই নিচের
           **By …** লাইনেও বসে ⇒ নাম দুবার। ⇒ উপরের লাইনে আর বসে না; নামটা
           নিচের লাইনে একবারই থাকে, তাই কোনো তথ্য হারায় না।
           ⛔ "Staff :" ও "Requested by :" ধরনের নোটিশে ব্রাঞ্চ ও নাম একসাথে
              দেখানো হয় না, তাই সেখানে আগের নিয়মই অটুট (নিচে দেখুন)। */

        val requestedBy = extractField(item.message, "Requested by")
        if (requestedBy != null) {
            val brOnly = item.branch.ifBlank { extractField(item.message, "Branch").orEmpty() }
            /* 🔴🔒 V1165 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — TK: *"এটা কিসের
               রিকোয়েস্ট আমি তো বুঝতেই পারছি না · রিকোয়েস্টটা কোন স্টাফ করেছে
               সেটাও তো বোঝা যাচ্ছে না"*।
               **কোডে মেপে পাওয়া দুটো কারণ:**
               ① কী মোছা হবে সেটা বার্তার `Type :` ঘরে **আছেই**, কিন্তু V1134-এ
                  দেখানো থেকে বাদ দেওয়া হয়েছিল ("শিরোনামেই আছে" ধরে) — অথচ
                  V1134-এর **আগে পাঠানো** নোটিশের শিরোনামে ওটা নেই। তাই পুরনো
                  অনুরোধে TK কিছুই বুঝতে পারতেন না।
               ② উপরের লাইনে অনুরোধকারীর **কোড** বসত (JPE-CRP), নাম নয়।
               ⇒ এখন: উপরে শুধু **ব্রাঞ্চ · তারিখ-সময়**; শিরোনামে **কী মোছা হবে +
                 রোগীর নাম**, পাশে **রোগের নাম** (বার্তার `Disease :` ঘর — এটাও
                 আগে থেকেই জমা ছিল); নিচে নম্বর ও অঙ্ক; তারপর **By কোড · নাম**।
               ⛔ `item.message` (ডেটাবেসে সেভ করা লেখা) এক অক্ষরও বদলায় না —
                  তাই ✔ Approve আগের মতোই `Row ID`/`Type` পড়ে ঠিক সারিটাই মোছে। */
            b.tvWho.text = withWhen(brOnly)
            b.tvAvatar.text = (brOnly.trim().ifEmpty { requestedBy }
                .firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'N').toString()
            val trimmed = item.message.lines().filterNot { line ->
                val l = line.trim()
                l.startsWith("Name :", true) || l.startsWith("Branch :", true) ||
                    l.startsWith("Requested by :", true) ||
                    /* 🟢🔒 V1134 (TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK: *"এখানে পেশেন্ট
                       আইডি, Row Id থাকবে না"* · *"আবার Type : Payment কেন লিখেছেন"*
                       (কী মোছা হবে সেটা এখন শিরোনামেই লেখা)।
                       ⛔ শুধু **দেখানো** লেখা থেকে বাদ — `item.message` (ডেটাবেসে সেভ
                          করা আসল লেখা) এক অক্ষরও বদলায় না, তাই Approve বোতাম
                          ওখান থেকে "Row ID"/"Type" পড়ে ঠিক সারিটাই মোছে। */
                    l.startsWith("Type :", true) || l.startsWith("Patient ID :", true) ||
                    l.startsWith("Row ID :", true) ||
                    (l.endsWith("permission request", true) && !l.contains(":")) ||
                    // 🟢🔒 V641 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "এটা দেখে কি
                    // প্রফেশনাল লুক মনে হচ্ছে?" → "না") — আসল কারণ: "Master:
                    // রেকর্ডটা খুলে Take Action → Delete চাপুন" এই নির্দেশটা
                    // পুরনো, ম্যানুয়াল একটা ফ্লো বোঝাত। "✔ Approve" বোতাম
                    // এখন **এক-চাপেই** সরাসরি ডিলিট করে দেয় (নিচেই
                    // `onApproveDelete(item)` ডাকা হয়) — তাই এই লাইনটা
                    // এখন সত্যিই অপ্রয়োজনীয়/বিভ্রান্তিকর, শুধু "Take
                    // Action"-ধরনের ম্যানুয়াল-নির্দেশের লাইনগুলোই বাদ
                    // (Reopen-এর মতো তথ্য-জানানো Master-নোট অক্ষত থাকে)।
                    (l.contains("Take Action", true) && l.contains("Master", true))
            }.joinToString("\n").trim()
            /* 🔴 V1165 — কী মোছা হবে (`Type`) শিরোনামে, রোগ পাশের চিপে, আর
               নম্বর ও কারণ এক লাইনে — তিনটেই বার্তার ভিতরে আগে থেকেই ছিল। */
            val what = extractField(item.message, "Type").orEmpty().trim()
            val pname = extractField(item.message, "Name").orEmpty().trim()
            val dis = extractField(item.message, "Disease").orEmpty().trim()
            if (what.isNotBlank() && pname.isNotBlank())
                b.tvTitle.text = "Delete $what — $pname"
            b.tvChipDisease.text = dis
            b.tvChipDisease.visibility = if (dis.isNotBlank()) View.VISIBLE else View.GONE
            /* নম্বর ও কারণ এক লাইনে — আলাদা দুই লাইনের বদলে, তাই কার্ড ছোট হয়।
               ⛔ যেটা জানা নেই সেটা বসেই না; দুটোই ফাঁকা হলে আগের পুরো লেখাটাই। */
            val mob = extractField(item.message, "Mobile").orEmpty().trim()
            val why = trimmed.lines().firstOrNull { l ->
                val t = l.trim()
                t.startsWith("Reason :", true) || (what.isNotBlank() && t.startsWith("$what :", true))
            }?.substringAfter(":")?.trim().orEmpty()
            val oneLine = listOf(mob, why).filter { it.isNotBlank() }.joinToString("  ·  ")
            b.tvMessage.text = buildClickableMessage(if (oneLine.isNotBlank()) oneLine else trimmed)
            /* 🔴 V1165 — ধূসর বাক্সটা আর নয়; এক লাইনের লেখায় ওটা শুধু জায়গা নিত।
               ⛔ রিসাইকেল-নিরাপদ: নিচের `else`-এ ব্যাকগ্রাউন্ড/প্যাডিং মোছাই থাকে। */
            b.tvMessage.background = null
            b.tvMessage.setPadding(0, 0, 0, 0)
            /* 🔴 V1165 — **কে চেয়েছে**: কোড ও নাম, একবারই, নিজের লাইনে।
               ⛔ নাম চেনা না গেলে শুধু কোড; কিছুই না জানলে লাইনটাই বসে না। */
            val byNm = staffNameFor(requestedBy)
            val byTxt = when {
                byNm == null -> requestedBy
                byNm.contains(requestedBy, ignoreCase = true) -> byNm
                else -> "$requestedBy \u00b7 $byNm"
            }
            b.tvPatientId.text = if (byTxt.isNotBlank()) "By  $byTxt" else ""
            b.tvPatientId.visibility =
                if (b.tvPatientId.text.isNullOrBlank()) View.GONE else View.VISIBLE
            // 🟢🔒 V641 (২৪.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ পাশ) — এই
            // ধরনের গঠিত অনুরোধ-নোটিশে "Role: master" অংশটা বাদ (কে
            // পাঠিয়েছেন সেটা হেডারেই আছে) — শুধু "Seen by X" থাকে।
            bindSeenBy(b.tvTargets, "", item)
        } else {
            // 🔒 RecyclerView রিসাইকেল-নিরাপদ: আগের কার্ডে বসানো বক্স-
            // ব্যাকগ্রাউন্ড/প্যাডিং যেন সাধারণ নোটিশে ভুল করে থেকে না যায়।
            b.tvMessage.background = null
            b.tvMessage.setPadding(0, 0, 0, 0)
        }
        val titleLc = item.title.lowercase()
        /* 🔴🔒 V1123 (০৫.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — TK: *"Urgent কেন?"*
           কারণ শিরোনামে "request" শব্দটা থাকলেই লাল URGENT বসত — অথচ
           **"Reply on: … "** কার্ডটা তো কাজ **হয়ে যাওয়ার খবর**, অনুরোধ নয়।
           V697-এ বোতামের জন্য এই ছাঁকনিটা এক জায়গায় আনা হয়েছিল
           (`BriefingModel.isReplyNotice`), কিন্তু রং/ব্যাজের এই লাইনটা বাদ
           পড়েছিল — সেটাই এখন মিলিয়ে দেওয়া হলো (নিয়ম ৭ক-এর ২)।
           ⛔ আসল Refund/Delete/Reopen/Leave অনুরোধ আগের মতোই লাল থাকে। */
        val replyNotice = com.tkbiswas.pilesclinic.native.BriefingModel.isReplyNotice(item.title)
        val urgent = !replyNotice &&
            listOf("request", "refund", "reopen", "urgent").any { titleLc.contains(it) }
        // 🔴🎨 TK-নির্দেশ (07.08.2026): "নেভি ব্লু/কালো রঙ ব্যবহার করা যাবে না।
        // ডিলিট একটা নিরাপত্তার জিনিস, তাই সেখানে লাল ধরনের রঙ; বাকিটা সবুজ,
        // যাতে সম্পূর্ণ প্রফেশনাল লাগে।" — তাই অ্যাভাটার/শিরোনাম/অনুমোদন-বোতাম
        // এখন অনুরোধ-নোটিশে লাল, সাধারণ নোটিশে সবুজ (আগে সবগুলোই কালচে
        // নেভি ছিল)। ⛔ শুধু রঙ — কোনো লজিক/ডেটা বদলায়নি।
        val redLine = android.graphics.Color.parseColor("#C0392B")
        val greenLine = android.graphics.Color.parseColor("#0F7A43")
        /* 🎨🔒 V829 (২৯.০৮.২০২৬, TK-অনুমোদিত) — এই দাগটা XML-এ `<Button>`, আর
           অ্যাপের থিমে সেটা MaterialButton হয়ে যায় ⇒ নিচের `setBackgroundColor`
           (লাল/সবুজ) চুপচাপ ফেলে দিয়ে থিমের গাঢ় নীল বসত। `backgroundTintList`
           খালি করলে তবেই আসল রংটা ফোনে দেখা যায় (পাহারা ৯.৩২)।
           ⛔ শুধু চেহারা — নোটিশের লজিক/তথ্য কিচ্ছু বদলায়নি। */
        b.priorityBar.backgroundTintList = null
        if (urgent) {
            b.priorityBar.setBackgroundColor(redLine)
            b.tvChip.text = "URGENT"
            b.tvChip.setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_brief_chip_urgent)
            b.tvChip.setTextColor(android.graphics.Color.parseColor("#B91C1C"))
            b.tvAvatar.backgroundTintList = android.content.res.ColorStateList.valueOf(redLine)
            b.tvTitle.setTextColor(android.graphics.Color.parseColor("#8E2A20"))
            // 🟢🔒 V641 (২৪.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ পাশ — কমপ্যাক্ট
            // ডিজাইন) — শিরোনাম এখন ছোট রঙিন পিল (chip), আগের মতো পুরো-
            // চওড়া প্লেইন টেক্সট নয়। ⛔ শুধু দেখানোর সাজ — id/লজিক অক্ষত।
            b.tvTitle.setBackgroundResource(0)   // 🟢 V1123 — শিরোনাম আর রঙিন পিল নয়, সাদা এক লাইন
        } else {
            b.priorityBar.setBackgroundColor(greenLine)
            b.tvChip.text = "NOTICE"
            b.tvChip.setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_brief_chip_normal)
            b.tvChip.setTextColor(android.graphics.Color.parseColor("#166534"))
            b.tvAvatar.backgroundTintList = android.content.res.ColorStateList.valueOf(greenLine)
            b.tvTitle.setTextColor(android.graphics.Color.parseColor("#14361F"))
            b.tvTitle.setBackgroundResource(0)   // 🟢 V1123
        }
        // 🔴 V433 (TK): "এটা একটা সাধারণ জিনিস, তাহলে এটা নোটিশ কেন হবে" —
        // তথ্য-কার্ডে NOTICE চিপটা তুলে দেওয়া হলো। রং/বার আগের মতোই সবুজ।
        /* 🟢 V1123 — TK: *"এই নোটিফিকেশনে নোটিস-ই বা কেন আসবে?"* — কাজ হয়ে
           যাওয়ার খবরে কোনো ব্যাজেরই দরকার নেই (০৪.০৮-এ সাধারণ তথ্য-কার্ডে
           TK নিজেই একই কথা বলেছিলেন, V433)। */
        /* 🟢🔒 V1134 (TK-প্রশ্ন: *"Urjent লেখা কেন?"*) — অনুমতি-চাওয়া
           কার্ডে বাঁ দিকের লাল দাগ ও রঙিন Approve বোতামই বুঝিয়ে দেয় কাজটা
           TK-এর অনুমতির অপেক্ষায়; উপরে আবার "URGENT" লেখাটা বাড়তি।
           ⛔ দাগ/রং/বোতাম কিচ্ছু বদলায়নি — শুধু ব্যাজটা লুকানো। */
        val hideChip = isPlainInfo || replyNotice ||
            (!replyNotice && titleLc.contains("delete request"))
        b.tvChip.visibility = if (hideChip) View.GONE else View.VISIBLE
        /* 🟢🔒 V1123 (TK-অনুমোদিত ফটো-প্রুফ) — উত্তর-নোটিশের ফলটা ("Approved &
           deleted by …") এখন ছোট সবুজ ব্যাজে, ✅ ছবি-অক্ষরটা ছাড়াই — এক নজরেই
           বোঝা যায় কাজটা হয়ে গেছে।
           ⛔ **শুধু দেখানোর সাজ** — `item.message` (ডেটাবেসে সেভ করা লেখা) এক
              অক্ষরও বদলায় না, তাই Reply/সিঙ্ক/ছাঁকনি কিছুই ভাঙে না।
           ⛔ রিসাইকেল-নিরাপদ: উত্তর-নোটিশ না হলে সাজটা প্রতিবার তুলে নেওয়া হয়। */
        if (replyNotice) {
            val plain = plainTitle(item.message)
            if (plain.isNotBlank()) {
                b.tvMessage.text = plain
                b.tvMessage.setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_brief_chip_normal)
                val padH = (9 * context.resources.displayMetrics.density).toInt()
                val padV = (3 * context.resources.displayMetrics.density).toInt()
                b.tvMessage.setPadding(padH, padV, padH, padV)
                b.tvMessage.setTextColor(android.graphics.Color.parseColor("#0A6C3D"))
                b.tvMessage.setTypeface(b.tvMessage.typeface, android.graphics.Typeface.BOLD)
            }
        } else {
            b.tvMessage.setTextColor(android.graphics.Color.parseColor("#374151"))
            b.tvMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
        // অনুমোদন-বোতামের রংও একই নিয়মে (কালো বাদ)।
        b.btnApproveDelete.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (urgent) redLine else greenLine)

        // 🔴🆕 TK (08.08.2026): কম্প্যাক্ট — রিপ্লাই না থাকলে "No replies yet" বাক্সটাই
        // লুকানো (আগে খালি হলুদ বাক্স জায়গা নিত)। রিপ্লাই থাকলে আগের মতোই।
        if (item.replies.isEmpty()) {
            b.tvReplies.visibility = View.GONE
        } else {
            b.tvReplies.visibility = View.VISIBLE
            b.tvReplies.text = item.replies.joinToString("\n") { r ->
                "• ${r.text}"
            }
        }

        // 🔒 খাতার সারি B100: শুধু **মাস্টার** ও শুধু **ডিলিটের অনুরোধ** নোটিশে
        // "Approve & Delete" দেখা যায়। বাকি সব নোটিশে বোতামটা লুকানো থাকে,
        // তাই পুরনো কার্ডের চেহারা এক চুলও বদলায়নি।
        // 🔴🔒 V697 — "Reply on: …" নামের কার্ড রিপ্লাই, অনুরোধ নয় (নিচে দেখুন)।
        val isReplyNotice = BriefingModel.isReplyNotice(item.title)
        val isDeleteRequest = !isReplyNotice && item.title.contains("Delete request", ignoreCase = true)
        if (isMaster && isDeleteRequest) {
            b.btnApproveDelete.visibility = View.VISIBLE
            b.btnApproveDelete.text = "\u2714 Approve"      // 🔴 এক-লাইনে আঁটাতে ছোট লেখা (আগে "Approve & Delete"); কাজ একই
            b.btnApproveDelete.setOnClickListener { onApproveDelete(item) }
            /* 🟢🔒 V1134 (TK: *"approve গ্রীন কালার করতে হবে তো"*) — অনুমতি
               দেওয়া মানে "হ্যাঁ", তাই সবুজ; না-বলার বোতামটাই লাল।
               ⛔ বাঁ দিকের লাল দাগ আগের মতোই থাকে, তাই কাজটা যে অনুমতির
                  অপেক্ষায় সেটা বোঝা যায়। */
            b.btnApproveDelete.backgroundTintList =
                android.content.res.ColorStateList.valueOf(greenLine)
        } else {
            b.btnApproveDelete.visibility = View.GONE
            b.btnApproveDelete.setOnClickListener(null)
        }

        // 🔴🔴 B281 (02.08.2026, TK-রিপোর্ট — "Refund request" নোটিশ SEEN
        // করেও সরছে না): এই নোটিশটা শুধু তথ্য জানানোর জন্য ছিল, আসল Approve/
        // Reject হতো আলাদা একটা কালেভদ্রে-না-দেখা ড্রপডাউনে (💸 Pending Refund
        // Requests) — যেটা কোনো pending রিফান্ড খুঁজে না পেলে সম্পূর্ণ লুকিয়ে
        // যেত। TK নিজে জানাতে রাজি নন সেটা খুঁজে দেখতে, তাই এখন ঠিক B100-এর
        // Delete-request-এর প্যাটার্নেই এই নোটিশ কার্ডেই সরাসরি Approve/Reject
        // বোতাম বসানো হলো — মাস্টারকে আলাদা কোথাও খুঁজতে হবে না।
        // ⛔ RecyclerView-এর View রিসাইকেল হয় বলে দুটো শাখাতেই (Delete/Refund)
        //    টেক্সট আলাদা করে বসাতে হয়েছে — নইলে স্ক্রল করার সময় ভুল লেখা
        //    রয়ে যেতে পারত (একটা কার্ডের বোতাম অন্য কার্ডে "ভুতুড়ে" দেখাত)।
        val isRefundRequest = !isReplyNotice && item.title.contains("Refund request", ignoreCase = true)
        if (isMaster && isRefundRequest) {
            b.btnApproveDelete.visibility = View.VISIBLE
            b.btnApproveDelete.text = "\u2714 Refund"       // আগে "Approve / Reject Refund" — চাপলে সেই একই Approve/Reject পপ-আপই খোলে
            b.btnApproveDelete.setOnClickListener { onApproveRefund(item) }
        }

        // 🆕 B419 (04.08.2026, TK-নির্দেশ) — "Chamber reopen request" নোটিশে
        // Approve/Reject। ⛔ একই শেয়ার্ড বোতাম (Delete/Refund-এর প্যাটার্নেই),
        // তাই RecyclerView রিসাইকেল হলেও ভুতুড়ে লেখা থাকার ঝুঁকি নেই — প্রতিটা
        // শাখাই নিজের লেখা/ক্লিক আবার বসায়।
        val isReopenRequest = !isReplyNotice && item.title.contains("Chamber reopen request", ignoreCase = true)
        if (isMaster && isReopenRequest) {
            b.btnApproveDelete.visibility = View.VISIBLE
            b.btnApproveDelete.text = "\u2714 Reopen"       // আগে "Approve / Reject Reopen" — চাপলে সেই একই পপ-আপই খোলে
            b.btnApproveDelete.setOnClickListener { onApproveReopen(item) }
        }

        // 🆕🔒 V1625 (২০.০৯.২০২৬, TK-রিপোর্ট — "এখানে তো অনুমতিপত্র নেই,
        // Accept-ও নেই") — "Money handover — please confirm" নোটিশে এতদিন
        // শুধু লেখাই থাকত, কোনো বোতাম ছিল না; যাঁর কাছে টাকা এসেছে তাঁকে
        // আলাদা করে ☰ মেনু থেকে Money Handover পর্দা খুঁজে বার করতে হতো।
        // ⛔ এখানেই সরাসরি "Accept" করে দেওয়া হয়নি — টাকা গ্রহণের আসল
        //    স্বীকৃতি (MoneyHandover.acknowledge) পাসওয়ার্ড দিয়ে যাচাই করে
        //    (MoneyHandoverActivity.kt) — সেই সুরক্ষা এড়িয়ে শর্টকাট বসানো
        //    হয়নি (টাকার কাজে কোনো ঝুঁকি নয়)। বোতাম শুধু সরাসরি সেই
        //    পর্দাতেই নিয়ে যায়, যেখানে পাসওয়ার্ড দিয়ে Accept করা যায় —
        //    নোটিশ পড়েও কোথায় গিয়ে করতে হবে বুঝতে না পারার সমস্যাটাই মেটানো
        //    হলো। ⛔ শুধু যাঁকে টাকা দেওয়া হয়েছে তাঁর নিজের নোটিশে — Master-এর
        //    "waiting for confirmation" কপিতে (role-target, তাঁর নিজের
        //    করণীয় নেই) এই বোতাম দেখানো হয়নি।
        val isMoneyHandoverConfirm = !isReplyNotice && !isMaster &&
            titleLc.contains("money handover") && titleLc.contains("please confirm")
        if (isMoneyHandoverConfirm) {
            b.btnApproveDelete.visibility = View.VISIBLE
            b.btnApproveDelete.text = "💰 Accept"
            b.btnApproveDelete.setOnClickListener {
                try {
                    context.startActivity(android.content.Intent(context, MoneyHandoverActivity::class.java))
                } catch (_: Throwable) { }
            }
        }

        // 🆕 TK-নির্দেশ (07.08.2026) — "অনুমতি অনেকগুলো একসাথে হয়ে গেলে মার্ক
        // করে একবারে অনুমোদন দেওয়া যাবে": শুধু যেসব কার্ডে অনুমোদন-বোতাম
        // দেখা যাচ্ছে (মাস্টারের অনুরোধ-নোটিশ) সেগুলোতেই বাছাই-বক্স।
        // ⛔ RecyclerView রিসাইকেল-নিরাপদ: প্রতিবার listener খুলে, অবস্থা বসিয়ে,
        //    তারপর আবার listener বসানো হয় (নইলে স্ক্রলে ভুল কার্ড বাছা হতো)।
        val canBulk = isMaster && b.btnApproveDelete.visibility == View.VISIBLE
        b.cbSelect.setOnCheckedChangeListener(null)
        b.cbSelect.visibility = if (canBulk) View.VISIBLE else View.GONE
        b.cbSelect.isChecked = canBulk && selectedIds.contains(item.id)
        if (canBulk) {
            b.cbSelect.setOnCheckedChangeListener { _, checked ->
                if (checked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                onSelectChanged()
            }
        }

        /* 🟢🔒 V1134 (TK-নির্দেশ, ফটো-প্রুফ পাশ) — *"হ্যাঁ Reject বসান,
           Close তুলে দিয়ে চারটেই রাখুন"*। ডিলিট-অনুরোধ কার্ডে চারটে বোতাম:
           **Approve · Reject · View · Reply**।
           ⛔ নতুন কোনো id বানানো হয়নি — V433-এ লুকিয়ে রাখা `btnSeen` ঘরটাই
              এখানে Reject হিসেবে কাজে লাগানো হলো, তাই লেআউটে হাত পড়েনি।
           ⛔ **Close শুধু এই অনুমতি-চাওয়া কার্ড থেকেই সরে** — সাধারণ নোটিশে
              ওটা না থাকলে বোর্ড আর পরিষ্কারই করা যেত না। */
        val showReject = isMaster && isDeleteRequest
        if (showReject) {
            b.btnSeen.visibility = View.VISIBLE
            b.btnSeen.text = "\u2716 Reject"
            b.btnSeen.backgroundTintList =
                android.content.res.ColorStateList.valueOf(redLine)
            b.btnSeen.setOnClickListener { onReject(item) }
        } else {
            b.btnSeen.visibility = View.GONE
            b.btnSeen.backgroundTintList = null
            b.btnSeen.setOnClickListener { onSeen(item) }
        }
        b.btnReply.setOnClickListener { onReply(item) }
        // 🔴 V433 (TK): "in time submit হয়েছে, তার জন্য আমাকে কেন আবার রিপ্লাই
        // দিতে হবে" — তথ্য-কার্ডে Reply নেই। Delete থেকেই যায় (চাইলে সরানো যায়)।
        // 🔴🔴 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — TK-এর কথা: *"একটু ভেবেচিন্তে
        //   বলুন তো, যেখানে রিপ্লাই আমি কি দিব? এটা একটা নতুন এনকোয়ারি হয়েছে,
        //   এখন অ্যাকশন কি হতে পারে? আমার হিসাবে তো অ্যাকশন হতে হয় ভিউ।"*
        //   ⇒ স্বয়ংক্রিয় তিন নোটিশে (New Enquiry · New Registration ·
        //     Advance Received) **Reply নেই** — View · Seen · Delete থাকে।
        //   ⛔ হাতে লেখা Briefing ও অনুমোদনের নোটিশে Reply আগের মতোই আছে।
        // 🟢🔒 V692 (২৬.০৮.২০২৬, TK): *"Overdue Call Alert এ Reply কেন আসবে,
        //   সেখানে View থাকতে হবে"* — ⚠️ Overdue Follow-up Alert-এও Reply নেই।
        b.btnReply.visibility =
            if (isPlainInfo || isAutoNotice(item) || BriefingModel.isOverdueAlert(item.title))
                View.GONE else View.VISIBLE
        b.btnDelete.setOnClickListener { onDelete(item) }
        b.btnDelete.visibility = if (showReject) View.GONE else View.VISIBLE   // 🟢 V1134

        /* 🔴🔒 V501 (TK-নির্দেশ) — "View" বোতাম।
           নোটিশের লেখায় রোগীর মোবাইল নম্বর থাকলেই দেখা যায়; না থাকলে
           লুকানো (অকেজো বোতাম দেখানো হয় না)। চাপলে ওই নম্বরের রোগীর পাতা।
           ⛔ নম্বর খোঁজা হয় ঠিক সেই একই নিয়মে যেটা দিয়ে নম্বরটা এতদিন
              ক্লিকযোগ্য করা হচ্ছে (`buildClickableMessage`) — নতুন কিছু নয়। */
        /* 🟢🔒 V1134 (TK: *"View তে নেভি ব্লু কালার থাকবে না"*) — অনুমতি-চাওয়া
           কার্ডে View এখন Reply-র মতোই হালকা সাদা ধাঁচে, তাই চোখ সোজা
           Approve/Reject-এ যায়। ⛔ বাকি সব নোটিশে View আগের মতোই। */
        /* 🎨🔒 V1141 (TK: *"View-এর কালার Normal রাখুন"*) — গাঢ় নেভি বাদ,
           সব কার্ডেই View এখন Reply/Close-এর মতো সাদা ধাঁচে। ⛔ কাজ অপরিবর্তিত। */
        b.btnViewRecord.setBackgroundResource(
            com.tkbiswas.pilesclinic.R.drawable.bg_brief_btn_ghost)
        b.btnViewRecord.setTextColor(android.graphics.Color.parseColor("#0F172A"))
        val mobileInNotice = firstMobileIn(item.message)
        // 🟢🔒 V692 — Overdue সতর্কতায় কোনো একটা নম্বর থাকে না (ব্রাঞ্চ ধরে
        //   গোনা), তাই নম্বর না থাকলেও View দেখাতে হবে। চাপলে ওই ব্রাঞ্চের
        //   **৩+ দিন দেরি হওয়া** কলগুলোই খোলে — নোটিশে যত জন লেখা, ঠিক তত জন।
        val overdueAlert = BriefingModel.isOverdueAlert(item.title)
        if (overdueAlert) {
            b.btnViewRecord.visibility = View.VISIBLE
            b.btnViewRecord.setOnClickListener { onViewOverdue(item) }
        } else if (BriefingModel.isReportNotice(item.title)) {
            // 📋 V1435 — রিপোর্ট-নোটিশে নম্বর থাকে না; View চাপলে রিপোর্টটা খোলে।
            b.btnViewRecord.visibility = View.VISIBLE
            b.btnViewRecord.setOnClickListener { onViewReport(item) }
        } else if (mobileInNotice != null) {
            b.btnViewRecord.visibility = View.VISIBLE
            // 🔴 V511: নোটিশটাও পাঠানো হয় — কারণ গন্তব্য নোটিশের **ধরন**
            //    অনুযায়ী বদলায় (Enquiry/Registration → Follow-up-এর ঠিক
            //    সেকশন, Advance → Payment)। নিচের `isAutoNotice` দেখুন।
            b.btnViewRecord.setOnClickListener { onViewRecord(item, mobileInNotice) }
        } else {
            b.btnViewRecord.visibility = View.GONE
            b.btnViewRecord.setOnClickListener(null)
        }
    
        /* ═══════════════════════════════════════════════════════════════
           🎨🔒 V1102 (০৫.০৯.২০২৬) — TK নিজের নকশার ছবি পাঠিয়ে বললেন
           *"এইরকম করুন"*: রোগের পিল · রোগীর নাম বড় করে · পাশে নম্বর ·
           নিচে Patient ID · নিচে ভরাট **View** ও হালকা দ্বিতীয় বোতাম।
           TK নিশ্চিত করেছেন দ্বিতীয় বোতামের কাজ **আগের মতোই — মুছে যাবে**,
           তাই `btnDelete`-এর id ও কাজ এক অক্ষরও বদলায়নি (শুধু চেহারা)।

           ⛔ শুধু অ্যাপের **নিজের তিন নোটিশে** (New Enquiry · New Registration ·
              Advance Received) চলে, আর তখনই — যখন লেখাটা চেনা ধাঁচে আছে
              (নাম - নম্বর - আইডি - রোগ - ব্রাঞ্চ)। না মিললে **হুবহু আগের কার্ডই**
              দেখা যায়, একটা লাইনও হারায় না।
           ⛔ হাতে লেখা Briefing · অনুমোদন · Overdue Alert — কিচ্ছু বদলায়নি।
           ⛔ নম্বরে চাপলে আগের মতোই কল যায়।
           ═══════════════════════════════════════════════════════════════ */
        run {
            var rich = false
            try {
                if (isAutoNotice(item)) {
                    /* 🚨🔒 V1207 (০৮.০৯.২০২৬, TK-রিপোর্ট: *"এই নম্বরে এন্ট্রি করে কোন
                       Staff করল এটা এখানে কেন দেখা যাচ্ছে না"*) — **আসল দোষ, কোডে মেপে পাওয়া:**
                       এখানে `.filter { it.isNotEmpty() }` ছিল। রোগীর **নামের ঘর ফাঁকা**
                       থাকলে প্রথম টুকরোটা বাদ পড়ে যেত ⇒ টুকরো ৪-এর বদলে ৩ ⇒ নিচের
                       শর্তটাই মিলত না ⇒ **স্টাফের নাম-সহ পুরো সাজটাই বসত না**, শুধু
                       কাঁচা লেখাটা দেখাত। (TK-এর ছবিতে ঠিক তাই — লেখাটা "-" দিয়ে শুরু।)
                       ⇒ এখন ফাঁকা টুকরোও **জায়গায় থাকে**, তাই ঘরগুলো আর সরে যায় না।
                       ⛔ নাম থাকা সারিতে আগে ছাঁকনিটা কিছুই বাদ দিত না — তাই ওদের
                          আচরণ **হুবহু আগের মতোই**। */
                    val parts = item.message.split(" - ").map { it.trim() }
                    val digitsOf = { t: String -> t.filter { c -> c.isDigit() } }
                    if (parts.size >= 4 && digitsOf(parts[1]).length >= 10) {
                        b.tvPatientName.text = parts[0]
                        b.tvPatientPhone.text = parts[1]
                        /* 🔴🔒 V1141 (TK: *"পেশেন্ট id লাগবে না"* · *"এই Registration-টা
                           কে করলো সেটা তো জানা যাচ্ছে না"*) — আইডির বদলে এখানে এখন
                           **কে করেছেন** সেটা বসে। নামটা `createdBy`-তেই ছিল, শুধু
                           খোঁজার চাবি ভুল ছিল (উপরে `staffNameFor` দেখুন)।
                           ⛔ চেনা না গেলে লাইনটাই বসে না — বানানো কিছু দেখানো হয় না। */
                        /* 🎨🔒 V1163 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ "খ") —
                           শিরোনামের পিলটা (যেমন "New Enquiry") আগে **নিজের একটা
                           আলাদা লাইন** নিত। এখন সেটা নিচের ধূসর লাইনেই লেখা হিসেবে
                           বসে, তাই কার্ড থেকে একটা গোটা লাইন কমে গেল।
                           ⛔ লেখাটা `tvTitle`-এর থেকেই নেওয়া — নতুন কিছু বানানো হয়নি।
                           ⛔ যে কার্ডে এই ধরনটা খাটে না, সেখানে পিল-সারি আগের মতোই থাকে। */
                        /* 🏷️🔒 V1207 (TK-অনুমোদিত ফটো-প্রুফ, হুবহু): *"Enquiry Piles
                           Jpe-Crp — এই তিনটা পাশাপাশি Tag হিসাবে থাকবে"*।
                           ⇒ উপরের সারিতে তিনটে ট্যাগ: ধরন · রোগ · স্টাফের কোড।
                           ⛔ কোড জানা না গেলে ট্যাগটা বসেই না — বানানো কিছু নয়। */
                        val titleTxt = b.tvTitle.text?.toString().orEmpty().trim()
                        b.tvTitle.text = shortNoticeTag(titleTxt)
                        /* 🎨🔒 V1444 (১৪.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — "Enquiry বক্সের
                           লম্বা এত বেশি কেন") — আসল কারণ (মেপে ধরা): XML-এ `tvTitle`
                           V1349-এ `layout_weight=1` পেয়েছিল, যাতে **লম্বা** raw
                           শিরোনাম (যেমন "Delete Payment — NAGENDRA SINGH") পাশের
                           Disease/Staff চিপ দুটোকে চেপে অক্ষর-ভাঙা না করে দেয়। কিন্তু
                           এই "rich" শাখায় শিরোনাম সবসময়ই ছোট নির্দিষ্ট শব্দ (Enquiry/
                           Registration/Advance) — তবু সেই একই weight=1 সারির **বাকি
                           সব ফাঁকা জায়গা জোর করে টেনে নিত**, তাই Piles/JPE-CRP-এর
                           মতো নিজের লেখা-জড়ানো (wrap_content) না হয়ে অস্বাভাবিক লম্বা
                           দেখাত। ⇒ এই শাখায় (যেখানে টেক্সট নিশ্চিতভাবেই ছোট) সরাসরি
                           wrap_content/weight=0 বসানো হলো, বাকি দুটো চিপের মতোই।
                           ⛔ V1349-এর আসল সুরক্ষা (লম্বা raw শিরোনামের কার্ড) অটুট —
                              এই `if`-এর বাইরে (নিচের `!rich` শাখায়) XML-এর ডিফল্ট
                              (weight=1) ফিরিয়ে দেওয়া হয়, তাই রিসাইকেল-করা কার্ডে
                              পুরনো বাইন্ডের চেহারা টিকে থাকে না। */
                        (b.tvTitle.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let {
                            it.width = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            it.weight = 0f
                            b.tvTitle.layoutParams = it
                        }
                        /* 🏷️🔒 V1253 (০৯.০৯.২০২৬, TK-নির্দেশ, খাতার সারি ৩৭৪) — TK:
                           *"Enquiry, Piles & Staff Name একই সাইজের ফন্ট হতে হবে"*।
                           মেপে দেখা (লেআউটে ও TK-র নিজের ছবিতে): **ফন্ট আগে থেকেই
                           তিনটেই ১৩sp, অক্ষরের উচ্চতাও হুবহু এক** — তফাতটা ছিল
                           সবুজ পিলের, V1123-এ শিরোনামের পিলটা তোলা হয়েছিল।
                           ⇒ **শুধু এই তিন-ট্যাগের সারিতেই** পিলটা ফিরল, তাই তিনটে
                             ট্যাগ দেখতে হুবহু এক (V1207-এ TK যা চেয়েছিলেন)।
                           ⛔ বাকি সব নোটিশের শিরোনাম (যেমন "IN TIME — LAXMI GUPTA")
                              V1123-এর মতোই **পিল ছাড়া সাদা লাইনেই** থাকে — ওখানে
                              এক অক্ষরও বদলায়নি (উপরের `setBackgroundResource(0)`
                              আগের মতোই চলে, এই লাইনটা শুধু এই শাখাতেই পরে বসে)। */
                        b.tvTitle.setBackgroundResource(
                            com.tkbiswas.pilesclinic.R.drawable.bg_brief_chip_normal)
                        b.tvPatientId.text = ""
                        b.rowTitle.visibility = View.VISIBLE
                        /* 🩺 V1207 — রোগের ট্যাগটা **মেপে** বাছা হয়: নাম ও নম্বরের
                           পরের যে টুকরোটা "branch" দিয়ে শেষ হয় না আর যাতে ১০ অঙ্কের
                           নম্বর নেই — সেটাই রোগ। এতে লেখার ধরন সামান্য আলাদা হলেও
                           (নাম থাকা/না-থাকা) ভুল করে "Cooch Behar branch" বসে না। */
                        val dis = parts.drop(2).firstOrNull { t ->
                            t.isNotBlank() && !t.trim().endsWith("branch", true) &&
                                digitsOf(t).length < 10
                        }.orEmpty()
                        b.tvChipDisease.text = dis
                        b.tvChipDisease.visibility = if (dis.isNotBlank()) View.VISIBLE else View.GONE
                        val staffTag = staffCodeFor(item.createdBy.trim()).orEmpty()
                        b.tvChipStaff.text = staffTag
                        b.tvChipStaff.visibility = if (staffTag.isNotBlank()) View.VISIBLE else View.GONE
                        b.rowPatient.visibility = View.VISIBLE
                        b.tvPatientId.visibility =
                            if (b.tvPatientId.text.isNullOrBlank()) View.GONE else View.VISIBLE
                        b.tvMessage.visibility = View.GONE
                        val dg = digitsOf(parts[1]).takeLast(10)
                        b.tvPatientPhone.setOnClickListener { if (dg.length == 10) onCallNumber(dg) }
                        rich = true
                    }
                }
            } catch (_: Throwable) { rich = false }
            if (!rich) {
                /* 🎨🔒 V1444 — RecyclerView রিসাইকেল-নিরাপত্তা: আগের বাইন্ডে এই
                   সারিটাই "rich" (Enquiry/Registration/Advance) ছিল বলে
                   tvTitle-এর LayoutParams wrap_content/weight=0 করা থাকতে
                   পারে। এই শাখা (V1349-এর আসল লম্বা-শিরোনামের সুরক্ষা প্রযোজ্য
                   যেখানে) XML-এর ডিফল্ট (0dp/weight=1) ফিরিয়ে দেয়, নইলে
                   অন্য কার্ডের রিসাইকেল-করা সারিতে ভুল চেহারা টিকে থাকত। */
                (b.tvTitle.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let {
                    it.width = 0
                    it.weight = 1f
                    b.tvTitle.layoutParams = it
                }
                b.rowPatient.visibility = View.GONE
                /* 👤 V1169 — স্টাফ-নোটিশে নিচের লাইন থাকবে না (TK-নির্দেশ);
                   বাকি সব কার্ডে আগের মতোই দেখায় — দুই দিকই বসানো, তাই সারি
                   পুনর্ব্যবহার হলেও অন্য কার্ডে ভুল চেহারা যায় না। */
                b.tvMessage.visibility =
                    if (staffNotice && staffExtra.isBlank()) View.GONE else View.VISIBLE
                /* 🔴🔒 V1165 — অনুরোধ-কার্ডে (Delete/Refund/Reopen) উপরে **By …**
                   লাইনটা ও রোগের চিপটা ইচ্ছে করে বসানো হয়েছে; এই ঘরটা সেগুলো
                   মুছে দিত। তাই অনুরোধ-কার্ড হলে এখানে হাত দেওয়া হয় না।
                   ⛔ বাকি সব কার্ডে আচরণ হুবহু আগের মতোই (রিসাইকেল-নিরাপদ)। */
                if (requestedBy == null) {
                    b.tvPatientId.visibility = View.GONE
                    b.tvChipDisease.visibility = View.GONE
                }
                /* 🔴🔒 V1349 (১১.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — "এখানে কেন COB-UTTAMA")
                   — `tvChipStaff` শুধু উপরের "rich" (New Enquiry/Registration) শাখাতেই
                   বসে, আর কোথাও (এমনকি অনুরোধ-কার্ডেও) সেট/রিসেট হয় না — তাই
                   RecyclerView রিসাইকেল করা সারিতে আগের কার্ডের staff-চিপ (আর তার
                   দৈর্ঘ্যের কারণে চরিত্র-ভাঙা চেহারাও) টিকে থাকত। ⛔ এটা
                   `requestedBy == null` শর্তের বাইরে — কারণ অনুরোধ-কার্ড সহ কোনো
                   শাখাই এটা নিজে বসায় না, তাই মোছার কিছু নেই, শুধু পুরনো রিসাইকেল-
                   করা মান লুকানো হচ্ছে। */
                b.tvChipStaff.visibility = View.GONE
                /* 🎨🔒 V1163 — RecyclerView সারি **পুনর্ব্যবহার** করে, তাই উপরের
                   ধাপে লুকানো পিল-সারিটা এখানে আবার দেখাতেই হয় — নইলে স্ক্রল
                   করলে অন্য কার্ডের শিরোনাম চুপচাপ হারিয়ে যেত।
                   ⛔ প্রকল্পের চিরকালের নিয়ম: প্রতিটা ঘরে **দুই দিকই** বসানো। */
                b.rowTitle.visibility = View.VISIBLE
            }
        }

        // 🔴🔒 V449 — একই ফিক্স (দেখুন FollowUpAdapter.kt-এর মন্তব্য): তালিকার
        // সারির বাংলা যাতে বাংলা-বন্ধ স্টাফের ফোনে কখনো না দেখা যায়।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }

    // 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — নোটিশ-বার্তা থেকে একটা ঘরের মান (যেমন
    // "Staff", "Time") বার করা। বার্তার ধরন: "👤 Staff : X\n🏥 Branch : Y\n🕐 Time : Z"।
    // না পেলে null। ⛔ মূল বার্তা বদলায় না।
    private fun extractField(message: String, label: String): String? {
        return try {
            Regex(Regex.escape(label) + "\\s*:\\s*([^\\n]+)")
                .find(message)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) { null }
    }

    // মাস্টারের জমানো স্টাফ-তালিকা (staff_profile_cache → "rows") থেকে কোড ধরে নাম।
    // একবার পড়ে map-এ রাখা হয় (বারবার নয়)। না পেলে null (তখন শুধু কোড দেখাবে)।
    private var staffNameMap: Map<String, String>? = null
    /** 🟢 V1123 — দেখানোর জন্য শিরোনাম: ছবি-অক্ষর ও বাড়তি ফাঁক বাদ।
     *  ⛔ অক্ষর · সংখ্যা · সাধারণ যতিচিহ্ন সবই থাকে — শুধু ছবি-অক্ষরগুলো যায়। */
    private fun plainTitle(raw: String): String {
        val sb = StringBuilder()
        for (ch in raw) {
            val t = Character.getType(ch).toByte()
            val drop = t == Character.SURROGATE || t == Character.OTHER_SYMBOL ||
                t == Character.NON_SPACING_MARK || ch == '\uFE0F' || ch == '\u20E3'
            if (!drop) sb.append(ch)
        }
        return sb.toString().replace(Regex("\\s{2,}"), " ").trim()
    }

    /* 🏷️ V1207 — ট্যাগের ছোট লেখা: "New Enquiry" → "Enquiry" ইত্যাদি।
       চেনা না গেলে শিরোনামটাই যেমন আছে তেমনই বসে। */
    private fun shortNoticeTag(title: String): String = when {
        title.equals("New Enquiry", true) -> "Enquiry"
        title.equals("New Registration", true) -> "Registration"
        title.equals("Advance Received", true) -> "Advance"
        else -> title
    }

    /* 🏷️ V1207 — শুধু স্টাফের **কোড** (যেমন COB-4 · JPE-CRP)। `createdBy`-তে
       মোবাইল থাকে, তাই V1141-এর প্রমাণিত পথেই কোড বার করা হয়।
       ⛔ না পেলে null ⇒ ট্যাগটা বসে না। */
    private fun staffCodeFor(raw: String): String? {
        val t = raw.trim()
        if (t.isBlank()) return null
        val ten = t.filter { it.isDigit() }.takeLast(10)
        if (ten.length == 10) {
            val acc = try { StaffDirectory.findAccount(ten) } catch (_: Throwable) { null }
            val c = acc?.name?.trim()
            if (!c.isNullOrBlank()) return c.uppercase()
            return null
        }
        // মোবাইল নয় — তাহলে কোডই বসানো আছে
        return t.uppercase()
    }

    private fun staffNameFor(code: String): String? {
        val map = staffNameMap ?: run {
            val m = HashMap<String, String>()
            try {
                val s = context.getSharedPreferences("staff_profile_cache", Context.MODE_PRIVATE)
                    .getString("rows", null)
                if (s != null) {
                    val arr = org.json.JSONArray(s)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        // 🔴🔒 V458 (20.08.2026, TK-অনুমোদিত ছোট কাজ — একই
                        // "null" টেক্সট প্যাটার্ন যাচাই): raw `.optString(key)`
                        // (fallback ছাড়াই) key অনুপস্থিত/NULL দুটোতেই literal
                        // "null" টেক্সট ফেরাতে পারে, আর `.isNotBlank()` সেটাকে
                        // ভুল করে "আছে" ধরে নিত — staff-এর নামের জায়গায় "null"
                        // বসে যাওয়ার ঝুঁকি ছিল। `.s()` (JsonExt.kt) দিয়ে ঠিক করা হলো।
                        val pc = o.s("person_code")
                        val nm = o.s("full_name")
                        if (pc.isNotBlank() && nm.isNotBlank()) m[pc] = nm
                    }
                }
            } catch (_: Throwable) { }
            staffNameMap = m
            m
        }
        val direct = map[code]?.takeIf { it.isNotBlank() }
        if (direct != null) return direct
        /* 🔴🔒 V1141 (০৬.০৯.২০২৬, TK-রিপোর্ট: *"এই Registration-টা কে করলো
           সেটা তো জানা যাচ্ছে না"*) — 🔴 **কারণ (কোডে মেপে পাওয়া):** নোটিশের
           `createdBy` ঘরে থাকে স্টাফের **মোবাইল নম্বর**, অথচ এই তালিকাটা
           **person_code** ধরে নাম খোঁজে ⇒ কখনো মিলত না, আর নম্বরে অক্ষর নেই
           বলে কাঁচা নম্বরটাও বসত না। ফল: শুধু ব্রাঞ্চই দেখাত।
           ⇒ এখন নম্বর পেলে আগে **কোড** বার করা হয় (প্রমাণিত `StaffDirectory`,
             DoctorVisit পর্দায় ঠিক এভাবেই চলে), তারপর সেই কোডে নাম।
           ⛔ কোড দিয়ে ডাকলে আচরণ হুবহু আগের মতোই। */
        val ten = code.filter { it.isDigit() }.takeLast(10)
        if (ten.length == 10) {
            val acc = try { StaffDirectory.findAccount(ten) } catch (_: Throwable) { null }
            val byCode = acc?.name?.takeIf { it.isNotBlank() }
            if (byCode != null) {
                val nm = map[byCode]?.takeIf { it.isNotBlank() }
                return if (nm != null) "$byCode \u00b7 $nm" else byCode
            }
        }
        return null
    }

    // 🆕 B467 (05.08.2026, TK-নির্দেশ) — মেসেজের লেখায় ১০-অঙ্কের ভারতীয়
    // মোবাইল নম্বর (ঐচ্ছিক +91 প্রিফিক্স-সহ) খুঁজে বার করে ক্লিকযোগ্য করে।
    // ⛔ মূল `item.message` (যা ডেটাবেসে সেভ আছে) এক অক্ষরও বদলায় না —
    // শুধু দেখানোর সময় স্প্যান বসানো হয়।
    /** 🔴 V501 — বার্তায় প্রথম যে মোবাইল নম্বরটা আছে (না থাকলে `null`)।
     *  `buildClickableMessage`-এর হুবহু একই প্যাটার্ন ব্যবহার করা হয়। */
    /** 🔴 V511 — ডেটাবেসের ট্রিগার যে তিন ধরনের নোটিশ নিজে থেকে বানায়
     *  (V490)। নাম তিনটে সার্ভারের `auto_notice_titles()`-এর হুবহু একই। */
    private fun isAutoNotice(item: Briefing): Boolean {
        val t = item.title.trim()
        return t.equals("New Enquiry", ignoreCase = true) ||
            t.equals("New Registration", ignoreCase = true) ||
            t.equals("Advance Received", ignoreCase = true)
    }

    private fun firstMobileIn(message: String): String? {
        return try {
            val m = MOBILE_PATTERN.matcher(message)
            if (m.find()) m.group().filter { it.isDigit() }.takeLast(10) else null
        } catch (_: Throwable) { null }
    }

    private fun buildClickableMessage(message: String): android.text.SpannableString {
        val spannable = android.text.SpannableString(message)
        val matcher = MOBILE_PATTERN.matcher(message)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val digits = matcher.group().filter { it.isDigit() }.takeLast(10)
            val span = object : android.text.style.ClickableSpan() {
                override fun onClick(widget: View) { onCallNumber(digits) }
                override fun updateDrawState(ds: android.text.TextPaint) {
                    ds.color = android.graphics.Color.parseColor("#1167D8")
                    ds.isUnderlineText = true
                }
            }
            spannable.setSpan(span, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }
}
