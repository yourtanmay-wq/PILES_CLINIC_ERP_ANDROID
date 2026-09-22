package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.databinding.ItemDoctorCardBinding

class DoctorVisitAdapter(
    private val context: Context,
    private var items: List<DoctorVisitItem>,
    private val currentUser: NativeUser,
    private val onCall: (DoctorVisitItem) -> Unit,
    private val onWhatsApp: (DoctorVisitItem) -> Unit,
    private val onLogCall: (DoctorVisitItem) -> Unit,
    private val onViewAll: (DoctorVisitItem) -> Unit = {},
    private val onEdit: (DoctorVisitItem) -> Unit = {},
    // TK-REQUESTED ADDITION (2026-07-20): triple-tap directly on the Last
    // Remark text opens a small remark-ONLY edit (no name/mobile/branch/
    // area fields involved) -- separate from onEdit (full Edit Doctor/RMP)
    // above, which triple-tapping the name/mobile still opens unchanged.
    private val onEditRemark: (DoctorVisitItem) -> Unit = {},
    // TK-REQUESTED ADDITION (2026-07-23): Delete-Approval for Doctor/RMP.
    // onDeleteTap fires for anyone tapping the delete action -- the
    // Activity decides whether that means "delete now" (Master) or "send a
    // request" (everyone else). onApprove/onReject only ever fire from
    // Master's Approve/Reject buttons on a pending request.
    private val onDeleteTap: (DoctorVisitItem) -> Unit = {},
    private val onApproveDelete: (DoctorVisitItem) -> Unit = {},
    private val onRejectDelete: (DoctorVisitItem) -> Unit = {}
) : RecyclerView.Adapter<DoctorVisitAdapter.ViewHolder>() {

    // V1660: remote-branch calling mode is intentionally call + remark only.
    private var callOnlyMode: Boolean = false

    fun setCallOnlyMode(enabled: Boolean) {
        if (callOnlyMode == enabled) return
        callOnlyMode = enabled
        notifyDataSetChanged()
    }

    // TK-REQUESTED (2026-07-18): RMP/doctor names on this list card should
    // always show a "Dr." prefix, even for entries saved without it. Display
    // only — the underlying name value (used for search/edit/Supabase) is
    // untouched, so no other screen or feature is affected.
    private fun formatDoctorDisplayName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return trimmed
        return if (trimmed.startsWith("dr", ignoreCase = true)) trimmed else "Dr. $trimmed"
    }

    /**
     * 🔴🔒 B685 (15.08.2026, TK-অনুমোদিত): একটা "yyyy-MM-dd" তারিখ আজকের থেকে
     * কত দিন **আগে** পড়ে — শুধু কার্ডে "Call was due N days ago" লেখার জন্য।
     * ⛔ কিছু সেভ/বদলায় না, কোনো গোনা বা ফিল্টারে ব্যবহার হয় না।
     * ⛔ তারিখ পড়তে না পারলে 0 ফেরত — তখন সংখ্যা ছাড়া সাধারণ বার্তা যায়,
     *   কোনোদিন ভুল সংখ্যা দেখানো হয় না।
     */
    private fun daysBeforeToday(dateStr: String): Int {
        return try {
            val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val then = f.parse(dateStr.take(10)) ?: return 0
            val now = f.parse(DoctorVisitModel.today()) ?: return 0
            val diff = (now.time - then.time) / (1000L * 60L * 60L * 24L)
            if (diff > 0) diff.toInt() else 0
        } catch (_: Throwable) { 0 }
    }

    fun updateItems(newItems: List<DoctorVisitItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemDoctorCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoctorCardBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding

        // TK-APPROVED (2026-07-25, via photo proof): fixed serial number in a
        // small red badge at the left of the name. It is display-only and
        // follows the order the list is already shown in (the RMP list is
        // sorted alphabetically by name, so the numbering stays stable), so
        // no stored data, search, edit or Supabase field is affected.
        b.tvSerial.text = (position + 1).toString()
        // TK-APPROVED (2026-07-26): the badge grows with the number, so at
        // four digits and beyond it would start eating the doctor name's
        // space. Shrink the digits instead once the number gets that long.
        // 1 to 3 digits keep the original 12sp exactly, so every badge TK
        // has already seen looks unchanged; this only kicks in past 999.
        // Set on every bind (never only in an if) because RecyclerView
        // reuses these views -- otherwise a shrunk badge would stay shrunk
        // on a later, shorter number.
        b.tvSerial.textSize = when (b.tvSerial.text.length) {
            in 0..3 -> 12f
            4 -> 10f
            5 -> 8.5f
            else -> 7.5f
        }
        // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে মোবাইল দুইবার দেখাত।
        // "Dr." প্রিফিক্স শুধু আসল নাম থাকলেই বসে।
        b.tvName.text = if (item.name.isBlank()) "UNKNOWN" else formatDoctorDisplayName(item.name)
        b.tvMeta.text = "📞 ${item.mobile}"
        // TK-APPROVED REDESIGN (2026-07-23): branch and area are now two
        // side-by-side coloured pill tags (was: area inside tvMeta, branch a
        // plain line). Shown in UPPERCASE per TK. Area tag hides itself when
        // there's no area, so a doctor with no area doesn't get an empty tag.
        b.tvBranch.text = item.branch.ifBlank { "-" }.uppercase()
        /* 🚓🔒 V1034 (০৪.০৯.২০২৬, TK-নির্দেশ) — ঠিকানার সঙ্গেই থানাটাও, একই
           ট্যাগে (`PS ·` লিখে), তাই কার্ডে নতুন কোনো সারি বাড়ে না।
           ⛔ থানা না লেখা থাকলে ট্যাগটা হুবহু আগের মতোই — শুধু ঠিকানা।
           ⛔ ঠিকানা না থাকলেও থানাটা থাকলে ট্যাগটা দেখায়, নইলে আগের মতোই লুকায়। */
        val psTxt = item.policeStation.trim()
        val areaTxt = item.area.trim()
        if (areaTxt.isBlank() && psTxt.isBlank()) {
            b.tvAreaTag.visibility = android.view.View.GONE
        } else {
            b.tvAreaTag.visibility = android.view.View.VISIBLE
            b.tvAreaTag.text = when {
                areaTxt.isBlank() -> "📍 PS · ${psTxt.uppercase()}"
                psTxt.isBlank() -> "📍 ${areaTxt.uppercase()}"
                else -> "📍 ${areaTxt.uppercase()}  ·  PS · ${psTxt.uppercase()}"
            }
        }
        // TK-APPROVED (2026-07-25, via photo proof): "LAST REMARK:" label
        // removed -- the box's own light-green color (bg_remark_dashed)
        // now makes it recognizable at a glance without a text label.
        // 🔴🔒 V458 (TK-নির্দেশ ১৯.০৮.২০২৬) — "Edit Doctor / RMP" পপ-আপ দিয়ে
        // Remarks সরাসরি বদলালে (callHistory-তে যায় না বলে LAST CALL-এ ধরা
        // পড়ে না) সেটা কে/কবে বদলেছেন তা এই বাক্সেই ছোট করে দেখানো হয়।
        // ⛔ raw-তে ঘর না থাকলে (পুরনো রেকর্ড/সেভ SQL চালানোর আগেরটা)
        //    আগের মতোই শুধু রিমার্কই দেখায়, কোনো বাড়তি লাইন নয়।
        // 🔴🔒 V456 (20.08.2026, TK-রিপোর্ট — "EDITED BY null · null" দেখাচ্ছিল):
        // আসল কারণ — এখানে প্রজেক্টের নিজস্ব null-safe `.s()` (JsonExt.kt)
        // ব্যবহার না করে raw `.optString(key, "")` ব্যবহার হচ্ছিল। org.json-এ
        // Supabase-এর ফাঁকা (NULL) কলাম JSON `null` হিসেবে আসে, আর `optString`
        // key **থাকলে** (মান null হলেও) সেটাকে literal "null" টেক্সট হিসেবে
        // ফেরত দেয় (fallback ব্যবহার হয় না) — পুরনো সারিতে (April-এর আগের,
        // এই ঘরদুটো তৈরি হওয়ার আগের) ঠিক এটাই হচ্ছিল। `.s()` এই সমস্যাটা
        // ইতিমধ্যেই সমাধান করা প্রমাণিত helper (JsonExt.kt-এর নিজের কমেন্টেই
        // লেখা "That caused 'null' to show on screen") — এখানে সেটাই ব্যবহার
        // করা হলো। ⛔ শুধু এই দুই লাইন বদলেছে, বাকি সব যুক্তি অক্ষত।
        val editedBy = item.raw.s("remarksEditedBy").trim()
        val editedAt = item.raw.s("remarksEditedAt").trim()
        val remarkMain = item.remarks.ifBlank { "NO REMARK YET" }.uppercase()
        b.tvRemark.text = if (editedBy.isNotBlank() || editedAt.isNotBlank()) {
            val whoEdit = StaffDirectory.findAccount(editedBy)?.name ?: editedBy
            val whenEdit = if (editedAt.isNotBlank()) FollowUpModel.displayDate(editedAt.take(10)) else ""
            val tag = listOfNotNull(whoEdit.ifBlank { null }, whenEdit.ifBlank { null }).joinToString(" · ")
            if (tag.isNotBlank()) "$remarkMain\nEDITED BY $tag" else remarkMain
        } else remarkMain

        val lastCallText = if (item.lastCallDate.isNotBlank()) FollowUpModel.displayDate(item.lastCallDate) else "—"
        val nextCallText = if (item.nextCallDate.isNotBlank()) FollowUpModel.displayDate(item.nextCallDate) else "—"
        // TK-APPROVED REDESIGN (2026-07-23): call count moved out of this
        // line up to the top-right (tvStatus); this line is now just the two
        // dates, bigger/bolder.
        // 🔒 খাতার সারি B123 (TK, 29.07.2026): *"Follow-up সেকশনে LAST CALL ·
        // STAFF NAME · NEXT CALL, তার নিচে remarks — যেমন মডেল রেখেছেন, একটা
        // বক্সের মধ্যে; এখানেও ঠিক সেরকমই রাখবেন।"*
        // তাই লেখাটা এখন **Follow-up কার্ডের হুবহু একই ধাঁচে** —
        //   `LAST CALL 21.07.2026 (JPE-CRP)` ..... `NEXT CALL 29.07.2026`
        // স্টাফের কোডটা আলাদা হালকা সোনালি রঙে (`#B8860B`), ঠিক Follow-up-এর মতো।
        // ⛔ তারিখের ধাঁচ (`FollowUpModel.displayDate`) আগেরটাই — বদলায়নি।
        // ⛔ কোনো তথ্য বাদ যায়নি; না থাকলে আগের মতোই `—`।
        run {
            val whoRaw = StaffDirectory.findAccount(item.lastCallBy)?.name ?: item.lastCallBy.trim()
            /* 🔵🔒 V543 (২২.০৮.২০২৬, TK-নির্দেশ) — *"তারিখের সাথে সময় থাকবে…
               LAST CALL 31/12/2026 : 12.30 PM তারপর staff Name, তারপর
               NEXT CALL 30/01/2026 — কিন্তু নেক্সট কলে সময় যেন না থাকে,
               যেহেতু ভবিষ্যতে কোন সময় কল করবে সেটা তো কেউ জানে না।"*
               ⛔ তারিখের ধাঁচ (ডট) TK-এর নিজের পছন্দে **অপরিবর্তিত** —
                  অ্যাপের বাকি সব জায়গার সাথে এক থাকে।
               ⛔ সময় না থাকলে (পুরোনো কল) লাইনটা **হুবহু আগের মতোই**। */
            val lastWhen = if (item.lastCallTime.isNotBlank())
                "$lastCallText : " + PaymentModel.displayTime12Dot(item.lastCallTime)   /* ⏰ V835 — 3.15 PM */
            else lastCallText
            val lastText = if (item.lastCallDate.isNotBlank()) {
                if (whoRaw.isNotBlank()) "LAST CALL $lastWhen ($whoRaw)" else "LAST CALL $lastWhen"
            } else "LAST CALL \u2014"
            val nextText = if (item.nextCallDate.isNotBlank()) "NEXT CALL $nextCallText" else "NEXT CALL \u2014"
            // 🔴🔒 B685 (15.08.2026, TK-অনুমোদিত · TK-ধরা): TK স্ক্রিনশটে দেখান
            //   `NEXT CALL 30.07.2026` — আজ ১৫.০৮, অর্থাৎ তারিখ ১৬ দিন আগে পার
            //   হয়ে গেছে, তবু কার্ড দেখে কিচ্ছু বোঝা যাচ্ছিল না (একই ধূসর লেখা)।
            //   এটা হুবহু সেই ফাঁক, যেটা "Expected patient"-এর জন্য TK B423-এ
            //   ঠিক করিয়েছিলেন — সেখানকার **প্রমাণিত সমাধানই** এখানে বসানো হলো:
            //   তারিখ লুকানো হয় না, শুধু পার হয়ে গেলে **লাল রং + স্পষ্ট বার্তা**।
            //   ⛔ PENDING / CALLED / EXPECTED / ALL RMP — কোনো বাক্সের গোনা,
            //     ফিল্টার বা সাজানো এক অক্ষরও বদলায়নি (B193 অক্ষত)।
            //   ⛔ তারিখের ধাঁচ, স্টাফের সোনালি নাম, LAST CALL — সব আগের মতোই।
            //   ⛔ `isOverdue()` আগে থেকেই ছিল (কড়া "আজকের আগে"), নতুন নিয়ম নয় —
            //     ফাঁকা তারিখ কখনোই overdue নয়, তাই `—` লেখা কার্ড লাল হবে না।
            val overdueCall = DoctorVisitModel.isOverdue(item.nextCallDate)
            val lateDays = if (overdueCall) daysBeforeToday(item.nextCallDate) else 0
            val warnLine = when {
                !overdueCall -> ""
                // 🔵 V543 (TK-এর পছন্দ): ছোট ও পরিষ্কার — "⚠️ 3 DAYS LATE"।
                lateDays > 0 -> "\n⚠️ " + lateDays + (if (lateDays == 1) " DAY LATE" else " DAYS LATE")
                else -> "\n⚠️ LATE"
            }
            val full = "$lastText  ·  $nextText$warnLine"
            val open = lastText.lastIndexOf("(")
            val span = android.text.SpannableString(full)
            if (whoRaw.isNotBlank() && item.lastCallDate.isNotBlank() && open >= 0) {
                span.setSpan(
                    android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#B8860B")),
                    open, lastText.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (overdueCall) {
                val red = android.graphics.Color.parseColor("#B91C1C")
                val nextStart = lastText.length + 5
                val nextEnd = nextStart + nextText.length
                if (nextStart in 0..full.length && nextEnd <= full.length) {
                    span.setSpan(
                        android.text.style.ForegroundColorSpan(red),
                        nextStart, nextEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    span.setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        nextStart, nextEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (warnLine.isNotEmpty()) {
                    val ws = full.length - warnLine.length + 1
                    if (ws in 0..full.length) {
                        span.setSpan(
                            android.text.style.ForegroundColorSpan(red),
                            ws, full.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        span.setSpan(
                            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            ws, full.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
            b.tvCallDates.text = span
        }
        // 🔒 খাতার সারি B193 (TK, 30.07.2026 রাত): "Expected patient" ব্যাজ —
        // expectedPatientDate ফাঁকা হলে সম্পূর্ণ লুকানো (tvReferralSummary-র
        // ঠিক উপরের নিয়মের মতোই), তারিখ থাকলে দেখা যায়।
        // 🔴 B423 (05.08.2026, TK-নির্দেশ — "তারিখ পার হয়ে গেলেও এখনো
        // পেশেন্ট আসেনি, এটা বোঝা যাচ্ছে না") — TK নিজে সিদ্ধান্ত নিয়েছেন
        // (আলোচনা করে): পুরনো তারিখ **লুকানো হবে না** (B193-এর "নিজে না
        // সরানো পর্যন্ত থাকবে" নিয়ম অক্ষত) — বরং তারিখ পার হয়ে গেলে
        // **আলাদা, স্পষ্ট সতর্কতা-রঙে** দেখাতে হবে, যাতে বোঝা যায় প্রতিশ্রুতি
        // অনুযায়ী রোগী আসেননি, আবার কল করা দরকার। ⛔ EXPECTED বক্সের গোনা/
        // ফিল্টার/সাজানো — কিছুই বদলায়নি (TK-এর আগের B193 সিদ্ধান্তই থাকল),
        // শুধু কার্ডে দেখানোর রং/লেখা বদলেছে।
        if (item.expectedPatientDate.isBlank()) {
            b.tvExpected.visibility = android.view.View.GONE
        } else {
            b.tvExpected.visibility = android.view.View.VISIBLE
            val overdue = item.expectedPatientDate < DoctorVisitModel.today()
            if (overdue) {
                b.tvExpected.text = "⚠️ Patient hasn't come yet (expected " + FollowUpModel.displayDate(item.expectedPatientDate) + ") — call again"
                b.tvExpected.setTextColor(android.graphics.Color.parseColor("#B91C1C"))
            } else {
                b.tvExpected.text = "🤞 Expected patient: " + FollowUpModel.displayDate(item.expectedPatientDate)
                b.tvExpected.setTextColor(android.graphics.Color.parseColor("#BE185D"))
            }
        }
        // TK-REQUESTED ADDITION (2026-07-23): referred-patient count +
        // referral income given, shown directly on the card (previously
        // only visible inside "View All" for one doctor at a time).
        // TK-REPORTED FIX (2026-07-23): hide this line completely when
        // BOTH are zero -- showing "0 patients referred · ₹0 income" for
        // every doctor with no referral activity yet was just noise.
        // TK-APPROVED (2026-07-23): shown in UPPERCASE.
        if (item.referredCount == 0 && item.referralPaid == 0.0) {
            b.tvReferralSummary.visibility = android.view.View.GONE
        } else {
            b.tvReferralSummary.visibility = android.view.View.VISIBLE
            val paidText = if (item.referralPaid == item.referralPaid.toLong().toDouble())
                item.referralPaid.toLong().toString() else "%.2f".format(item.referralPaid)
            b.tvReferralSummary.text = "👥 ${item.referredCount} PATIENTS REFERRED · 💰 ₹$paidText INCOME"
        }

        // CRASH/COLOR-SAFETY FIX (TK-reported, 2026-07-16): the XML-only
        // android:backgroundTint="@null" on these buttons was not reliably
        // taking effect on TK's device -- Android's default Button styling
        // was still painting them navy, hiding their real green/silver
        // background and squashing the "Log Call" text into the icon.
        // Clearing the tint here in code is the reliable fix; it does not
        // touch any color value already defined in the XML drawables.
        b.btnCall.backgroundTintList = null
        b.btnWhatsApp.backgroundTintList = null
        b.btnLogCall.backgroundTintList = null
        b.btnViewAll.backgroundTintList = null

        // TK-APPROVED REDESIGN (2026-07-23): the coloured "Call Pending /
        // Called" badge is removed; the top-right now shows only the call
        // count as plain text. (callStatus/callCount are still stored and
        // used elsewhere -- only this card's display changed.)
        b.tvStatus.text = "📞 ${item.callCount} calls"

        b.btnCall.setOnClickListener { onCall(item) }
        b.btnLogCall.setOnClickListener { onLogCall(item) }
        if (callOnlyMode) {
            // Remote branch: only Phone Call + mandatory Call Remark are available.
            b.btnWhatsApp.visibility = android.view.View.GONE
            b.btnViewAll.visibility = android.view.View.GONE
            b.btnDelete.visibility = android.view.View.GONE
            b.requestBanner.visibility = android.view.View.GONE
            // No triple-tap edit path in call-only mode.
            b.tvName.setOnClickListener(null)
            b.tvMeta.setOnClickListener(null)
            b.tvRemark.setOnClickListener(null)
            b.tvReferralSummary.visibility = android.view.View.GONE
        } else {
            b.btnWhatsApp.visibility = android.view.View.VISIBLE
            b.btnViewAll.visibility = android.view.View.VISIBLE
            b.btnDelete.visibility = android.view.View.VISIBLE
            b.btnWhatsApp.setOnClickListener { onWhatsApp(item) }
            b.btnViewAll.setOnClickListener { onViewAll(item) }
            TripleTapEdit.attach(b.tvName) { onEdit(item) }
            TripleTapEdit.attach(b.tvMeta) { onEdit(item) }
            TripleTapEdit.attach(b.tvRemark) { onEditRemark(item) }
        }
        // TK-REQUESTED (2026-07-18): long-press to copy, matching the same
        // pattern already used on the Follow-up card's mobile field.
        b.tvName.setOnLongClickListener {
            com.tkbiswas.pilesclinic.native.Clip.copy(it.context, "name", item.name)   // 🤫 V772
            android.widget.Toast.makeText(it.context, "Name copied", android.widget.Toast.LENGTH_SHORT).show()
            true
        }
        b.tvMeta.setOnLongClickListener {
            com.tkbiswas.pilesclinic.native.Clip.copy(it.context, "mobile", item.mobile)   // 🤫 V772
            android.widget.Toast.makeText(it.context, "Mobile copied", android.widget.Toast.LENGTH_SHORT).show()
            true
        }

        // TK-REQUESTED ADDITION (2026-07-23): Delete-Approval for Doctor/RMP.
        b.btnDelete.setOnClickListener { onDeleteTap(item) }
        if (!callOnlyMode && item.deleteRequestedBy.isNotBlank()) {
            val requesterName = StaffDirectory.findAccount(item.deleteRequestedBy)?.name ?: item.deleteRequestedBy
            val whenText = if (item.deleteRequestedAt.isNotBlank()) FollowUpModel.displayDate(item.deleteRequestedAt.take(10)) else ""
            b.tvRequestInfo.text = "⚠️ Delete Requested by $requesterName" + (if (whenText.isNotBlank()) " · $whenText" else "")
            b.requestBanner.visibility = android.view.View.VISIBLE
            // Only Master sees Approve/Reject -- everyone else just sees the
            // "pending" text above, so they know their request went through.
            b.requestActionRow.visibility = if (currentUser.role == "master") android.view.View.VISIBLE else android.view.View.GONE
            b.btnApproveDelete.backgroundTintList = null
            b.btnRejectDelete.backgroundTintList = null
            b.btnApproveDelete.setOnClickListener { onApproveDelete(item) }
            b.btnRejectDelete.setOnClickListener { onRejectDelete(item) }
        } else {
            b.requestBanner.visibility = android.view.View.GONE
        }
    
        // 🔴🔴🔒 V449 (TK-রিপোর্ট ১৮.০৮.২০২৬ — "লগইন হয়ে যায় কিন্তু কার্যকরী কিছু হয় না")।
        // আসল কারণ: NoBengali-এর অটো-সুইপ শুধু পর্দার layout-পাসে চলে; RecyclerView
        // rebind/scroll নতুন layout-পাস তৈরি না করেই লেখা বসিয়ে দেয়, তাই তালিকার
        // সারির বাংলা কখনো ঢাকাই পড়ত না। এখন প্রতিটা বাইন্ডের শেষেই সরাসরি সুইপ —
        // বাংলা-বন্ধ না থাকলে কিছুই করে না (activeCache false ⇒ সাথে সাথে ফেরত)।
        try { NoBengali.sweep(holder.itemView) } catch (_: Throwable) { }
    }
}
