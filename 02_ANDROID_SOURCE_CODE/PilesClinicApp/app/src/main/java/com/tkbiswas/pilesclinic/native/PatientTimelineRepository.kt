package com.tkbiswas.pilesclinic.native

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/** One entry in the patient's View-All timeline. */
data class TimelineEntry(
    val icon: String,
    val colorHex: String,
    val title: String,
    val date: String,
    val by: String,
    val note: String,
    // TK APPROVED (2026-07-15): standing rule — a 3-tap edit spot must let you
    // edit everything. Only Payment-type entries carry a real "payments" row
    // id + branch (needed to edit them); every other entry type leaves this
    // null and is completely unaffected.
    val paymentId: String? = null,
    val paymentBranch: String = "",
    val paymentAmount: Double = 0.0,
    val paymentMode: String = "CASH",
    // 🔒 V452 (19.08.2026, TK-approved A): one Treatment Payment per calendar
    // day can contain both CASH and ONLINE. Keep the split + real event count
    // so History can show one truthful daily row without losing accounting.
    val paymentCashAmount: Double = 0.0,
    val paymentOnlineAmount: Double = 0.0,
    val paymentEventCount: Int = 1,
    // TK-REQUESTED FIX (2026-07-18): Visit Fee (the one-time fee collected at
    // Registration) must be tracked completely separately from Treatment
    // cost/Advance payments — they are unrelated money flows. This field
    // lets the running-total calculation below tell them apart reliably
    // (checking the exact payType string set by
    // PatientModel.buildVisitFeePaymentRow(), not just the display label).
    val payType: String = "",
    // TK-REQUESTED ADDITION (2026-07-16): register-style table fields —
    // 1-based visit number (oldest = 1) and the running Paid/Due total AT
    // this point in the patient's history, matching TK's paper register.
    // Default 0 so no other existing reader of TimelineEntry is affected.
    val visitNo: Int = 0,
    val runningPaid: Double = 0.0,
    val runningDue: Double = 0.0,
    // TK-REPORTED BUG FIX (2026-07-19, from video): same-day payments (e.g.
    // 2nd and 3rd Payment both taken today) were sorting by "date" alone
    // (a plain yyyy-MM-dd string, no time) -- when two entries share the
    // exact same date string, Kotlin's stable sort just keeps whatever
    // order they happened to arrive from the cloud in (fetchList's own
    // updatedAt-desc order), NOT the order they actually happened, so same-
    // day payments could show reversed / out of sequence. This carries the
    // full createdAt timestamp (always set, has real time-of-day) for a
    // precise chronological sort; blank for entry types that don't set it,
    // which safely falls back to the old "date" behavior below.
    // 🔒 V217 (§B216, 31.07.2026): Paid/Due হিসাবে refund-এর জন্য আলাদা,
    // "সাইন-করা" মান — সাধারণ পেমেন্টে = paymentAmount, approved refund-এ
    // = -amount, pending/rejected refund-এ = 0 (হিসাবে কোনো প্রভাব নেই)।
    // ⛔ `paymentAmount` কখনো বদলায়নি — সেটাই 3-tap Edit বাক্সে দেখানো আসল
    // DB-এর টাকা, negative করে দিলে Edit-এ গিয়ে ভুল করে negative সেভ হয়ে
    // যেতে পারত। তাই হিসাব ও দেখানো — দুটো আলাদা ঘরে।
    val paidEffect: Double = 0.0,
    val sortKey: String = "",
    // TK-REQUESTED ADDITION (2026-07-20): 3-tap edit target for Enquiry-stage
    // rows (Enquiry & Call History table). Optional, default null/-1, so
    // every other reader of TimelineEntry is completely unaffected.
    /* ✏️🔒 V736 (২৭.০৮.২০২৬, TK-অনুমোদিত অপশন ৩) — **"Registration / Visit"
       সারির ভুল লেখা ডাক্তার নিজে ঠিক করতে পারবেন, আর পুরোনো লেখাও জমা থাকবে।**

       ডা. কে. এইচ. মণ্ডলের রিপোর্ট: *"এটা staff ভুল তুলেছে, এটা আমাকে ঠিক
       করার অধিকার দেওয়া হোক"*। TK: *"অপশন ৩ — চারটেই বদলানো যাবে, তবে পুরোনো
       লেখা জমা থাকবে।"*

       ওই সারিটার লেখা আসলে **দুই জায়গার তিন+এক টুকরো জোড়া**:
         · patients.complaint · patients.sinceWhen · patients.previousTreatment
         · payments.progress  (+ টাকার লাইন, যেটা কখনো এডিটে আসে না)
       তাই এডিট করতে হলে প্রতিটা টুকরোর **আসল ঠিকানা** সঙ্গে নিয়ে যেতে হয় —
       নিচের ঘরগুলো ঠিক সেটাই বয়ে নেয়।
       ⛔ যে সারিতে এসব নেই সেখানে সব `null`/ফাঁকা ⇒ আচরণ আগের মতোই। */
    val regPatientRowId: String? = null,   // patients.id — কোন সারি বদলাবে
    val regComplaint: String = "",         // patients.complaint
    val regDuration: String = "",          // patients.sinceWhen
    val regPrevTreatment: String = "",     // patients.previousTreatment
    val payTypedNote: String = "",         // payments.progress-এর মানুষের লেখা অংশ
    val enquiryRowId: String? = null,
    val followUpHistoryId: String? = null,
    val followUpHistoryIndex: Int = -1,
    // TK-REQUESTED ADDITION (2026-07-20): time-of-day (full ISO timestamp),
    // used only by the Enquiry & Call History table's Time column + the
    // Unexpected-hour flag. Blank when not available (older entries).
    val callTime: String = "",
    // 🚨 TK-REPORTED, LIVE (27.07.2026, SADDAM — TK's photo): the Report Card's
    // PROGRESS box was filled with the app's own payment text ("Advance Payment
    // — ₹10,000 · CASH · ..."), three lines tall, instead of staying "—" like the
    // next visit. The Report Card used to work that out by pattern-matching the
    // finished note text, which cannot be reliable -- an amount written slightly
    // differently, or an edit that appends an audit line, and the guess fails.
    // This field carries the answer directly instead: the remark a PERSON really
    // typed, and nothing else (blank when the app filled it in by itself).
    // Default blank, so every other reader of TimelineEntry is unaffected.
    val typedRemark: String = "",
    // 🟢🔒 V676 (২৫.০৮.২০২৬, TK-নির্দেশ — "আজকের Doctor Checkup এডিট করতে
    // পারব") — "medical" সারির id + "selected" ঘর (Doctor Checkup-এ এতদিন
    // ব্যবহারই হতো না, তাই এখানে structured JSON রাখা নিরাপদ — Prescription/
    // Diet Chart-এর "selected" ব্যবহার অক্ষত, ওগুলোর id আলাদা row-এ থাকে)।
    // ⛔ ডিফল্ট ফাঁকা — বাকি সব TimelineEntry পাঠক সম্পূর্ণ অপ্রভাবিত।
    val medicalRecordId: String = "",
    val medicalSelected: String = "",
    /* 🟢🔒 V1090 (০৫.০৯.২০২৬, TK-রিপোর্ট: *"check up done দুইবার কেন"*) —
       এই সারিটা আসলে চিকিৎসার নোট (কেউ ফোন করেননি)। ডিফল্ট false —
       তাই TimelineEntry-এর বাকি সব পাঠক সম্পূর্ণ অপ্রভাবিত। */
    val isTreatmentNote: Boolean = false,
    /* 🔴🔒 V1376 (১২.০৯.২০২৬, TK-রিপোর্ট — SAIF RAZA-র কার্ডে MANZAR ALAM-এর
       Visit Fee ভুল করে দেখাচ্ছিল): এই টাকার সারিটা আসলে **কোন রোগীর**
       (payments.patientId) — যখন এক মোবাইলে একাধিক আলাদা রোগী থাকেন
       ("Different Patient — Same Mobile", V517), Registration/Visit-এর
       সাথে Visit Fee মেশানোর (নিচের merge) সময় এটা দিয়েই যাচাই হয় যে
       টাকাটা সত্যিই **এই** রোগীর কিনা, শুধু একই দিনে হয়েছে বলেই নয়।
       ⛔ ডিফল্ট ফাঁকা — বাকি সব TimelineEntry পাঠক সম্পূর্ণ অপ্রভাবিত। */
    val paymentPatientRowId: String = "",
    // V1653: true only when this payment was taken from Chamber Attendance.
    // Ordinary payment-only dates must not become a new VISIT row.
    val isChamberVisitAnchor: Boolean = false
)

/** Header + all updates for one patient/mobile, built by joining every table. */
data class TimelineData(
    val name: String,
    val patientId: String,
    val mobile: String,
    // 🔒 V235: Alternate/Enquiry Mobile (default ফাঁকা — পুরনো caller অপরিবর্তিত)।
    val altMobile: String = "",
    val branch: String,
    val disease: String,
    val photo: String,
    val entries: List<TimelineEntry>,
    // TK-REQUESTED ADDITION (2026-07-16): total estimated bill, needed for
    // the register table's "Estimated" summary chip. 0 when not yet set.
    val billTotal: Double = 0.0,
    // 🏷️ TK-APPROVED (03.09.2026): the discount already forgiven on this
    // patient's bill. 0.0 means none was ever given -- the Discount box then
    // stays hidden and the three older boxes look exactly as before.
    val discount: Double = 0.0,
    // 🏷️🔒 V1508 (১৬.০৯.২০২৬, TK-নির্দেশ — MD ANARUL HOWK) — সত্যিকারের আসল
    // বিল (ছাড়ের আগে), শুধু "Estimated" চিপ-এ দেখানোর জন্য। 0.0 মানে কখনো
    // ছাড় দেওয়াই হয়নি -- তখন চিপ আগের মতোই `billTotal` দেখায় (কোনো বদল নেই)।
    // ⛔ Due-এর হিসাব এখনো `billTotal` (কমানো বিল) দিয়েই হয় -- এই ঘরটা শুধু
    //    দেখানোর জন্য, কোনো হিসাবে যোগ হয় না।
    val billBeforeDiscount: Double = 0.0,
    // TK-REQUESTED ADDITION (2026-07-18): the "patients" table row id (uuid),
    // needed so the Patient Card header's 3-tap Edit can save name/mobile
    // corrections. Blank when no Registration/patients row exists yet for
    // this mobile (e.g. Enquiry-only) — Edit then safely no-ops with a
    // message instead of crashing. Default "" so nothing else is affected.
    val rowId: String = "",
    // TK-REQUESTED ADDITION (2026-07-18): who referred this patient, if
    // known. Read from the existing "patients" row (set at Registration if
    // "Dr. Visit" was picked as Ref By, or added later via the Patient
    // Card edit below). Blank when unknown — nothing else is affected.
    val refDoctor: String = "",
    // TK-REQUESTED ADDITION (2026-07-24): pre-formatted "Dr. NAME (AREA)"
    // string for the header's own display line -- refDoctor above is left
    // untouched (still used wherever just the plain name is needed, e.g.
    // the 3-tap Edit dialog's prefill).
    val refDoctorDisplay: String = "",
    val refDoctorMobile: String = "",
    // TK-REQUESTED ADDITION (2026-07-18): "Take Action" menu needs these to
    // wire Remark/Next-Follow-up/Reject (act on the followups row) and
    // Delete (act on the enquiries row). Blank when that record doesn't
    // exist yet for this mobile — those specific actions just don't show.
    val followupId: String = "",
    val followupStage: String = "",
    /** 🔒 খাতার সারি B97 (TK, 29.07.2026 রাত ৯.১০): রেকর্ডটা **এখনই Reject/
     *  Incomplete অবস্থায় আছে কি না** — "Take Action" মেনুতে ইতিমধ্যে বাতিল
     *  হয়ে যাওয়া রেকর্ডে আবার "Reject" দেখানো অর্থহীন, TK ঠিক সেটাই ধরেছেন।
     *  ফাঁকা মানে জানা যায়নি — তখন আগের মতোই সব দেখায়, কিছু ভাঙে না। */
    val followupStatus: String = "",
    val enquiryId: String = "",
    // TK-REQUESTED ADDITION (2026-07-18): needed for the same-day
    // Delete-Patient permission check (TrashHelper), same pattern already
    // used for Enquiry delete in Draft.
    val registrationDate: String = "",
    /** 🔴🔒 V505 (TK-নির্দেশ ২১.০৮.২০২৬): *"পেশেন্ট আইডি তার নিচে
     *  রেজিস্ট্রেশনের তারিখ এবং সময় থাকবে।"*
     *  রোগীর সারির `createdAt` — এতে দিনের **আসল সময়** থাকে (উপরে ৫৩ নং
     *  লাইনের নোট দেখুন)। তাই পরে আবার বার্তা পাঠালেও **রেজিস্ট্রেশনের
     *  আসল সময়টাই** যায়, পাঠানোর সময় নয়।
     *  ⛔ বাড়তি কোনো ক্লাউড-কল নেই — এই ঘরটা আগে থেকেই একই লোডে আসে। */
    val registrationCreatedAt: String = "",
    val registeredByMobile: String = "",
    val enquiryDate: String = "",
    val enquiryReceivedBy: String = "",
    // TK-LOCKED DESIGN (2026-07-23): header now shows Age-Sex and Address as
    // dedicated rows. Age/Sex only exist on the "patients" row (captured at
    // Registration) -- blank for Enquiry-only people. Address falls back to
    // the Enquiry's address when no Registration row exists yet.
    val age: String = "",
    val sex: String = "",
    val address: String = "",
    // TK-REQUESTED ADDITION (2026-07-24): "Complete despite Due" workflow --
    // Staff requests (completeRequestedBy set), Master approves
    // (completeApprovedBy set, completeRequestedBy cleared) or rejects
    // (both cleared). Blank/blank = no request in progress. Real Due is
    // NEVER changed by this -- it only lets the Draft "Complete Patient"
    // bucket include this patient anyway, and stops Follow-up call
    // reminders for them (see DraftRepository.kt / FollowUpRepository.kt).
    val completeRequestedBy: String = "",
    val completeApprovedBy: String = "",
    /**
     * 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — এই রোগীর এনকোয়ারি **অসময়ে** এসেছিল
     * কি না (`"Unexpected Time"` / `"Official Time"`)।
     *
     * **কেন দরকার:** স্টাফের Extra Income **শুধু অসময়ের এনকোয়ারিতেই** হয়
     * (নিয়মটা `V418_INCENTIVE_AUTO_2026-08-17.sql`-এ)। কিন্তু রোগীর History
     * খুলে সেটা দেখার কোনো উপায়ই ছিল না — TK বলেছেন *"আমি বুঝবো কী করে যে
     * স্টাফটা কী কারণে টাকা নিচ্ছে।"*
     *
     * ⛔ **বাড়তি কোনো cloud-read নেই** — এই পর্দা `patients`/`enquiries` সারি
     *    আনে `byMobile()` দিয়ে, আর সেটা `fetchList(...)`-এর ডিফল্ট `select=*`
     *    ব্যবহার করে (এই ফাইলের ২৪৯–২৫৫ নম্বর লাইন)। তাই `timeType` ঘরটা
     *    **আগে থেকেই আসত**, শুধু কেউ পড়ত না। এক বাইটও বাড়ছে না।
     * ⛔ ডিফল্ট ফাঁকা — পুরোনো সারিতে ঘরটা না থাকলে কিছুই দেখায় না, আচরণ আগের মতোই।
     */
    val timeType: String = ""
)

/**
 * Builds the View-All timeline exactly like the WebView's viewFollow(): it pulls
 * the person's Enquiry, Follow-up history, Registration/Visit, Payments and
 * Medical records, tags each with a type icon/colour, and sorts newest-first so
 * the last thing that happened is on top.
 */
object PatientTimelineRepository {

    private fun style(type: String): Pair<String, String> {
        val t = type.lowercase()
        return when {
            t.contains("enquiry") -> "📞" to "#1067D8"
            t.contains("registration") || t.contains("visit") -> "👣" to "#16A36D"
            t.contains("discount") -> "🏷️" to "#0B7A34"
            t.contains("payment") || t.contains("advance") -> "💰" to "#F79009"
            t.contains("prescription") || t.contains("medicine") ||
                t.contains("diet") || t.contains("blood") || t.contains("medical") ||
                t.contains("checkup") || t.contains("investigation") -> "🩺" to "#6941C6"
            else -> "📝" to "#667085"
        }
    }

    /**
     * 🟢🔒 B679 (১৫.০৮.২০২৬, TK-অনুমোদিত) — **তালিকা সাজানোর একটাই চাবি**।
     *
     * TK-এর রিপোর্ট (ছবিসহ): *"তারিখ অগোছালো কেন?"* — ২৫.০৭ · ১৮.০৭ · ১১.০৭ · ০৪.০৭-এর
     * চারটে পেমেন্ট পাশাপাশি বসে ছিল, অথচ ১০.০৮ · ০৮.০৮-এর সারি তাদের **নিচে**।
     *
     * **আসল কারণ (কোড ও TK-এর ছবি — দুটোতেই প্রমাণ):** সাজানো হত `sortKey` দিয়ে,
     * আর পেমেন্টের `sortKey = createdAt` — অর্থাৎ **যেদিন এন্ট্রি করা হয়েছে**, ঘটনার
     * দিন নয়। TK-এর ছবিতে ওই চারটের সময় ছিল 4.17 · 4.18 · 4.19 · 4.20 PM — পরপর এক
     * মিনিট, অর্থাৎ পুরনো চারটে পেমেন্ট একদিনে বসে একসঙ্গে তোলা হয়েছিল। তাই ওরা
     * এন্ট্রির দিনে জমাট বেঁধে ছিল, যদিও পর্দায় নিজেদের আসল তারিখ দেখাচ্ছিল।
     *
     * **এখন:** আগে **ঘটনার তারিখ** (যেটা পর্দায় দেখা যায়), তারপর একই দিনের ভিতরে
     * সময় ধরে — তাই তালিকা ঠিক তারিখ-ক্রমে বসে।
     * ⛔ কোনো সারি বাদ যায় না · টাকার অঙ্ক বদলায় না · মোট Paid/Due এক থাকে।
     * ⛔ একই চাবি দিন-ভাগ (byDay) ও Report Card-এও — তাই দুই পর্দা কখনো আলাদা হবে না।
     */
    /**
     * 🔴🔴🔒 V655 (২৫.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "উল্টা পাল্টা তারিখ হিসাবে
     * কেন দেখাচ্ছে") — **আসল কারণ (কোড ধরে যাচাই, Sukanta Roy-র টাইমলাইনে
     * প্রমাণিত):** "আসবে বলেছে" (chamber_expected) সারির `date` ঘরে
     * ইচ্ছাকৃতভাবে **ভবিষ্যতের প্রত্যাশিত তারিখ** বসানো থাকে (স্টাফ যেদিন
     * আসবে বলেছেন — যেমন ২৯.০৮.২০২৬), যদিও এন্ট্রিটা **সত্যিই তৈরি** হয়েছিল
     * অন্য দিনে (২৪.০৮.২০২৬)। আগে `orderKey()` **প্রথমে `date`** দেখত —
     * তাই এই সারিটা ভবিষ্যতের তারিখ ধরে "সবচেয়ে নতুন" সেজে তালিকার একদম
     * উপরে উঠে যেত, যদিও ওই একই সারির দেখানো সময় (Date/Time কলাম, `sortKey`/
     * `callTime` থেকে) ছিল ২৪.০৮.২০২৬ — তাই আজকের (২৫.০৮.২০২৬) আসল পেমেন্টও
     * এর নিচে চলে যেত।
     * **সমাধান:** এখন **প্রথমে `sortKey`** দেখা হয় (যেটা সবসময় সত্যিকারের
     * "কবে ঘটেছে/তৈরি হয়েছে" ধরে, `date`-এর মতো ভবিষ্যতের-লক্ষ্য-তারিখ কখনো
     * বহন করে না) — `date` শুধু fallback, `sortKey` ফাঁকা থাকলেই ব্যবহার হয়।
     * ⛔ যেসব এন্ট্রিতে আলাদা `sortKey` কখনো বসানোই হয়নি (ডিফল্ট ফাঁকা),
     *    তাদের ক্রম **অক্ষরে অক্ষরে আগের মতোই** (date-ই ব্যবহার হয়)।
     */
    private fun orderKey(e: TimelineEntry): String {
        val day = e.sortKey.take(10).ifBlank { e.date.take(10) }.ifBlank { e.callTime.take(10) }
        val stamp = listOf(e.sortKey, e.callTime).firstOrNull { it.length > 10 } ?: ""
        val timePart = if (stamp.length > 10) stamp.substring(10) else ""
        return if (day.isBlank()) "" else day + timePart
    }

    private fun entry(type: String, date: String, by: String, note: String): TimelineEntry {
        val (icon, color) = style(type)
        return TimelineEntry(icon, color, type, date, by, note)
    }

    private fun money(v: Double): String = "₹" + "%,.0f".format(v)

    /** Rows whose mobile ends with these 10 digits (format-agnostic).
     *
     *  🚨 TK-REPORTED (29.07.2026 বিকেল ৪.৪০, ছবিসহ · খাতার সারি B114):
     *  *"লোডিং হতে এত বেশি সময় কেন লাগছে?"* (Report Card ও Patient Timeline —
     *  দুটোতেই অনেকক্ষণ "Loading…")।
     *
     *  **আসল কারণ (কোড ধরে মেপে, আন্দাজ নয়):** রোগীর **ছবিটা সারির ভিতরেই**
     *  লেখা থাকে (text আকারে), আর এই খোঁজাটা প্রতিটা ঘর চাইত (`select=*`)।
     *  `patients`-এ ছবি আছে, **`followups`-এও ঠিক একই `photo` ঘর আছে** — তাই
     *  **একই ছবি দু'বার নামত**। অথচ এই পর্দা ছবিটা নেয় **শুধু `patients` সারি
     *  থেকে** (নিচে `photo = patient.s("photo")`), `followups`-এরটা কোথাও
     *  ব্যবহারই হয় না। TK-এর লাইনে ছবিটাই সবচেয়ে ভারী জিনিস, তাই অপেক্ষাটা
     *  প্রায় দ্বিগুণ হয়ে যেত।
     *
     *  ⛔ **শুধু `followups`-এর ছবি বাদ** — `patients`-এর ছবি আগের মতোই আসে,
     *     তাই পর্দায় ও Report Card-এ ছবি ঠিকঠাক দেখাবে।
     *  ⛔ বাকি প্রতিটা ঘর আগের মতোই আসে (খাতার সারি B105-এর সেই লাইভ-যাচাই করা
     *     তালিকা), তাই কোনো তথ্য হারায় না।
     *  ⛔ সরু পড়া কোনো কারণে না চললে `fetchListSlim` নিজেই আগের মতো সব ঘর
     *     চেয়ে নেয় — সবচেয়ে খারাপ অবস্থাতেও আজকের আচরণই থাকে।
     *  ⛔ অনুরোধের সংখ্যা এক চুলও বাড়েনি — সেই একটাই খোঁজা।
     */
    private fun byMobile(table: String, digits: String) =
        if (table == "followups")
            SupabaseClient.fetchListSlim(
                "followups", "mobile=like.*$digits", 500, SupabaseClient.FOLLOWUP_COLS_NO_PHOTO
            )
        else if (table == "patients")
            /* 🔴🔒 V794 (২৮.০৮.২০২৬, TK-নির্দেশে Egress-যাচাইয়ের পরে) —
               এখানেও ছবিটা সারির সঙ্গে নামত (≈৬০–১২০ KB × যত সারি)। এখন
               ছবি ছাড়া পড়া হয়, আর নিচে (`photo = …`) ছবিটা
               `PatientPhotoCache` থেকে আসে — জমা থাকলে **একটাও বাইট নয়**,
               নইলে একবারই শুধু `id,photo`।
               ⛔ পর্দায় ও Report Card-এ ছবি আগের মতোই দেখাবে। */
            SupabaseClient.fetchListSlim(
                "patients", "mobile=like.*$digits", 500, SupabaseClient.PATIENT_NO_PHOTO_COLS
            )
        else
            SupabaseClient.fetchList(table, "mobile=like.*$digits", 500)

    /** TK APPROVED (2026-07-15): when opened from a specific Follow-up tab, only
     *  that tab's own entries show; opened from Dashboard/Global Search (or any
     *  other place that doesn't pass a section), everything shows — unchanged.
     *  NOTE: FollowUpActivity's internal stage strings are historically named —
     *  the UI "Visit" tab uses stage string "Patient", and the UI "Patient" tab
     *  uses stage string "Treatment". This mapping matches those exact strings. */
    private fun matchesSection(entryTitle: String, section: String): Boolean {
        val t = entryTitle.lowercase()
        return when (section) {
            "Inquiry" -> t.contains("enquiry")
            "Patient" -> t.contains("registration") || t.contains("visit") || t.contains("advance")
            "Treatment" -> (t.contains("payment") && !t.contains("advance")) ||
                t.contains("prescription") || t.contains("medicine") || t.contains("diet") ||
                t.contains("blood") || t.contains("medical") || t.contains("checkup") ||
                t.contains("investigation") || t.contains("treatment complete")
            else -> true
        }
    }

    /** Merges any locally-pending rows (not yet synced) for this mobile into
     *  a cloud result set, avoiding duplicates by id. TK-REQUESTED ADDITION
     *  (2026-07-16): same fix as Follow-up/Doctor Queue/Today's Collection/
     *  Global Search -- Full Journey always read straight from the cloud
     *  with no awareness of a save still syncing in the background. */
    private fun mergeWithPending(cloud: JSONArray, pending: JSONArray, mobileDigits: String): JSONArray {
        val merged = JSONArray()
        val seenIds = HashSet<String>()
        for (i in 0 until cloud.length()) {
            val row = cloud.getJSONObject(i)
            seenIds.add(row.optString("id"))
            merged.put(row)
        }
        for (i in 0 until pending.length()) {
            val row = pending.getJSONObject(i)
            val id = row.optString("id")
            val rowMobile = row.optString("mobile").filter { it.isDigit() }.takeLast(10)
            if (id.isNotBlank() && rowMobile == mobileDigits && seenIds.add(id)) merged.put(row)
        }
        return merged
    }

    // 🔒 B607 (TK-অনুমোদিত, প্রুফ দেখিয়ে 09.08.2026): নতুন প্যারাম
    // separateRowsPerEvent — true হলে একই দিনের ঘটনা আর এক ঘরে মেশে না, প্রতিটা
    // ঘটনা আলাদা সারি হয় (History পর্দার জন্য)। ডিফল্ট false — ⛔ Report Card ও
    // অন্য সব কলার আগের মতোই (day-merge) পায়, এক অক্ষরও বদলায় না। টাকার হিসাব
    // দুই পথেই এক (প্রতি পেমেন্ট আলাদা paidEffect ধরে গোনা হয়, নিচে দেখুন)।
    /**
     * 🟢🔒 V1090 (০৫.০৯.২০২৬, TK: *"check up done দুইবার কেন"*) —
     * **টাকার সারি থেকে সেই পুনরাবৃত্ত বাক্যটা তোলা।**
     *
     * চিকিৎসার নোট দুই জায়গায় জমা হয় (V590): ফলো-আপ খাতায় আর সেদিনের
     * টাকার সারির `progress` ঘরে। History পর্দায় দুটোই দেখাত ⇒ এক কথা
     * দু'বার। এখানে **শুধু দেখানোর জন্য** টাকার সারি থেকে ওই বাক্যটা তোলা
     * হয়; ফলো-আপ সারিটা অক্ষত থাকে, তাই **কে লিখেছেন ও কখন** — কিছুই
     * হারায় না, আর ৩-বার চেপে এডিটও আগের জায়গাতেই থাকে।
     *
     * ⛔ তোলা হয় **শুধু তখনই** যখন ঠিক ওই দিনেই ফলো-আপ সারিতে হুবহু একই
     *    লেখা আছে। ⛔ তুললে নোট একদম ফাঁকা হয়ে গেলে কিছুই তোলা হয় না
     *    (টাকা ০ হলে নোটে টাকার লাইন থাকে না — তখন আগের মতোই থাকবে)।
     * ⛔ `payTypedNote`/`typedRemark` (Report Card ও ৩-বার চাপের এডিট
     *    যেগুলো পড়ে) এক অক্ষরও বদলায় না — শুধু `note`।
     * ⛔ একই দিনে একাধিক পেমেন্ট জোড়া লাগলে লেখাটা " | " দিয়ে জোড়া হয়;
     *    তখন **প্রতিটা টুকরোই** ফলো-আপে থাকলে তবেই পুরোটা তোলা হয়।
     */
    /* 🚨🔒 V1189 — তারিখ ধরে চেম্বারের লেখা। `followups`-এর `history`-তে
       প্রতিটা সারিতে `date` · `time` · `remark` · `staff` জমা থাকে
       (`FollowUpRepository.updateRemark`), তাই কোন দিনে কী লেখা হয়েছিল সেটা
       ঠিকঠাক বের করা যায়।
       ⛔ একই দিনে একাধিক লেখা থাকলে সেগুলো ` · ` দিয়ে জোড়া হয়, একটাও হারায় না।
       ⛔ অ্যাপের নিজের বসানো কথা বাদ যায় (একই `isAutoPaymentRemark` ছাঁকনি)।
       ⛔ নতুন কোনো cloud-read লাগে না — `history` আগের পড়াতেই চলে আসে। */
    private var chamberNotesByDate: Map<String, String> = emptyMap()

    private fun chamberNoteOn(date: String): String = chamberNotesByDate[date].orEmpty()

    private fun followUpSafeForPatient(
        row: org.json.JSONObject,
        mobileDigits: String,
        patientRowId: String,
        patientCode: String,
        patientName: String,
        selectedIsDeclaredSeparate: Boolean,
        otherDeclaredRowIds: Set<String>,
        otherDeclaredCodes: Set<String>
    ): Boolean {
        if (PatientIdentity.provablyOtherPatient(row, mobileDigits, patientRowId, patientCode, patientName)) return false
        val ref = row.s("refId").trim()
        val code = row.s("patientId").trim().uppercase()
        if (ref.isNotBlank() && otherDeclaredRowIds.contains(ref)) return false
        if (code.isNotBlank() && otherDeclaredCodes.contains(code)) return false
        if (selectedIsDeclaredSeparate && !PatientIdentity.rowBelongsTo(row, patientRowId, patientCode)) return false
        return true
    }

    private fun buildChamberNotes(
        followups: org.json.JSONArray,
        mobileDigits: String,
        patientRowId: String,
        patientCode: String,
        patientName: String,
        selectedIsDeclaredSeparate: Boolean,
        otherDeclaredRowIds: Set<String>,
        otherDeclaredCodes: Set<String>
    ) {
        val out = LinkedHashMap<String, MutableList<String>>()
        try {
            for (i in 0 until followups.length()) {
                val f = followups.optJSONObject(i) ?: continue
                if (!followUpSafeForPatient(
                        f, mobileDigits, patientRowId, patientCode, patientName,
                        selectedIsDeclaredSeparate, otherDeclaredRowIds, otherDeclaredCodes
                    )) continue
                val hist = f.optJSONArray("history") ?: continue
                for (j in 0 until hist.length()) {
                    val h = hist.optJSONObject(j) ?: continue
                    val d = h.optString("date", "").take(10)
                    val r = h.optString("remark", "").trim()
                    if (d.isBlank() || r.isBlank()) continue
                    /* 🏷 V1192 — ফোন-কলের রিমার্ক চিকিৎসার জায়গায় কখনো নয়।
                       ⛔ পুরনো সারিতে চিহ্ন নেই ⇒ সেগুলো আগের মতোই দেখানো হয়। */
                    if (h.optString("src", "").equals("call", true)) continue
                    if (PaymentModel.isAutoPaymentRemark(r, "")) continue
                    val human = PaymentModel.typedPartOf(r, "").trim()
                    if (human.isBlank()) continue
                    val list = out.getOrPut(d) { mutableListOf() }
                    if (list.none { it.equals(human, ignoreCase = true) }) list.add(human)
                }
            }
        } catch (_: Throwable) { }
        chamberNotesByDate = out.mapValues { it.value.joinToString("  \u00b7  ") }
    }

    private fun wlv1StripEchoedTreatmentNote(rows: List<TimelineEntry>): List<TimelineEntry> {
        return try {
            val noteKeys = HashSet<String>()
            for (e in rows) {
                if (!e.followUpHistoryId.isNullOrBlank() && e.note.trim().isNotEmpty())
                    noteKeys.add(e.date.take(10) + "|" + e.note.trim().lowercase())
            }
            if (noteKeys.isEmpty()) return rows
            rows.map { e ->
                val typed = listOf(e.payTypedNote.trim(), e.typedRemark.trim())
                    .firstOrNull { it.isNotEmpty() && e.note.trim().startsWith(it) }
                if (e.paymentId == null || typed.isNullOrEmpty()) return@map e
                val day = e.date.take(10)
                val parts = typed.split(" | ").map { it.trim() }.filter { it.isNotEmpty() }
                val allEchoed = parts.isNotEmpty() && parts.all { noteKeys.contains(day + "|" + it.lowercase()) }
                if (!allEchoed) return@map e
                val stripped = e.note.trim()
                    .removePrefix(typed).trim()
                    .removePrefix("\u2014").trim()
                if (stripped.isEmpty()) e else e.copy(note = stripped)
            }
        } catch (_: Throwable) { rows }
    }

    fun build(mobileDigits: String, section: String? = null, context: Context? = null, keepVisitFeeAsOwnRow: Boolean = false, separateRowsPerEvent: Boolean = false, preferRowId: String = "", preferPatientCode: String = ""): TimelineData {
        // Match by the trailing 10 digits (like the global search does) instead of
        // an exact "+91..." match, so a timeline is found regardless of how the
        // mobile was stored (bare 10-digit, +91, spaces, etc.). This is why the
        // View-All screen used to show "No updates yet" for a real patient.
        // PERFORMANCE FIX (2026-07-25, TK-requested proactive sweep): these
        // three reads (and the payments read further down) do not depend on
        // each other, so they are started together instead of one after the
        // other . opening Patient Details / Full Journey used to wait for the
        // SUM of four round trips. Exactly the same queries, same order of
        // use afterwards; only the waiting is shorter.
        val preTimeline = runBlocking {
            val eDef = async(Dispatchers.IO) { byMobile("enquiries", mobileDigits) }
            val fDef = async(Dispatchers.IO) { byMobile("followups", mobileDigits) }
            val pDef = async(Dispatchers.IO) { byMobile("patients", mobileDigits) }
            val payDef = async(Dispatchers.IO) { byMobile("payments", mobileDigits) }
            listOf(eDef.await(), fDef.await(), pDef.await(), payDef.await())
        }
        var enquiries = preTimeline[0]
        var followups = preTimeline[1]
        var patients = preTimeline[2]
        context?.let { ctx ->
            val store = LocalWorkflowStore(ctx)
            try { store.markSyncedWhereCloudCaughtUp("enquiries", enquiries); store.markSyncedWhereCloudCaughtUp("followups", followups); store.markSyncedWhereCloudCaughtUp("patients", patients) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
            enquiries = mergeWithPending(enquiries, store.pendingEnquiries(), mobileDigits)
            followups = mergeWithPending(followups, store.pendingFollowUps(), mobileDigits)
            patients = mergeWithPending(patients, store.pendingPatients(), mobileDigits)
        }
        // V1653: chamber-history fallback is built only AFTER the exact patient row is selected below,
        // so a shared family mobile can never leak another patient's progress.

        // TK-REQUESTED (2026-07-27), ধাপ ৩: this took whichever row the cloud
        // returned first. When a person has a duplicate "patients" row, that
        // could be the empty one while the money screen was using the real
        // (billed) one -- the same patient then showed a different Bill /
        // Patient ID / address here than on the payment screen. Now both use
        // the ONE shared rule. With a single row (the normal case) this is
        // exactly the same row as before.
        // TK-REQUESTED (2026-07-27), ধাপ ৩খ — ১ম পর্দা (রোগীর পাতা): the shared
        // rule is "the row in the branch being worked in -> the row with a real
        // bill -> the first row". The payment screen has always passed its own
        // branch into that rule; this screen passed nothing, so on a duplicate
        // registration the two could still land on DIFFERENT rows -- the money
        // screen on the current branch's row, this screen on the billed one --
        // and show a different Bill / Patient ID / address for the same person.
        // Passing the same branch makes the two identical in every case.
        // With a single row (the normal case) nothing changes at all.
        val viewerBranch = context?.let { NativeSession.current(it)?.branch }.orEmpty()
        /* 🔵🔒 V517 (২২.০৮.২০২৬, TK-অনুমোদিত): এক মোবাইলে একাধিক রোগী থাকতে
           পারেন (স্বামী/স্ত্রী)। ডাকার জায়গা যদি **কোন রোগী** তা জানিয়ে দেয়
           (`preferRowId`), তখন ঠিক সেই সারিটাই নেওয়া হয়।
           ⛔ `preferRowId` ফাঁকা, বা ওই আইডির সারি এই নম্বরে না থাকলে —
              **হুবহু আগের পথ** (`pickPatientRow`)। তাই পুরোনো সব ডাক
              (Report Card · Chamber · Timeline-এর নিজের পথ) এক অক্ষরও বদলায়নি।
           ⛔ শেয়ার-করা `PatientIdentity.pickPatientRow` নিয়মটা ছোঁয়া হয়নি —
              ওটা ৯টা পর্দা ব্যবহার করে, V143-এর সুরক্ষা অটুট। */
        /* 🔵🔒 V522 (২২.০৮.২০২৬, TK-নির্দেশ): `preferPatientCode`-ও চলবে।
           কিছু পর্দা (Chamber Attendance) রোগীর **সারির আইডি** জানে না, কিন্তু
           **Official Patient ID** জানে — সেটাও রোগী-প্রতি অনন্য, তাই সেটা দিয়েও
           ঠিক রোগীটাই বেছে নেওয়া যায়। ঠিক যে নিয়ম V520-এ
           `PaymentRepository`-এর `findPatientByMobile()`-এ পাশ হয়েছে।
           ⛔ একই লুপ, **বাড়তি একটাও cloud-read নেই**।
           ⛔ দুটোই ফাঁকা হলে বা না মিললে — হুবহু আগের পথ (`pickPatientRow`)। */
        val forced = if (preferRowId.isBlank() && preferPatientCode.isBlank()) null else {
            /* 🔴 নিজের যাচাইয়ে ধরা পড়েছে (২২.০৮.২০২৬): প্রথমে দুটো শর্ত **একই
               লুপে** রাখা হয়েছিল। তাতে দুটো আইডি একসাথে এলে ও তারা আলাদা
               রোগীকে দেখালে, তালিকায় যে সারিটা **আগে** আছে সেটাই জিতে যেত —
               অর্থাৎ কোড দিয়ে সারির আইডি হেরে যেত। এখন **দুই ধাপে**:
               ১. আগে সারির আইডি (সবচেয়ে নির্দিষ্ট) — পুরো তালিকা খুঁজে।
               ২. না পেলে তবেই Official Patient ID।
               ⛔ একটাই আইডি এলে (রোজকার সব ক্ষেত্রে) ফল অবিকল একই। */
            var f: JSONObject? = null
            if (preferRowId.isNotBlank()) {
                for (i in 0 until patients.length()) {
                    val r = patients.optJSONObject(i) ?: continue
                    if (r.s("id") == preferRowId) { f = r; break }
                }
            }
            if (f == null && preferPatientCode.isNotBlank()) {
                for (i in 0 until patients.length()) {
                    val r = patients.optJSONObject(i) ?: continue
                    if (r.s("patientId") == preferPatientCode) { f = r; break }
                }
            }
            f
        }
        val patient = forced ?: PatientIdentity.pickPatientRow(patients, viewerBranch) ?: JSONObject()
        val patientId = patient.s("id")
        val patientCode = patient.s("patientId")
        val selectedIsDeclaredSeparate = PatientModel.isDeclaredSeparateRowId(patientId, mobileDigits)
        val otherDeclaredRowIds = HashSet<String>()
        val otherDeclaredCodes = HashSet<String>()
        for (i in 0 until patients.length()) {
            val pr = patients.optJSONObject(i) ?: continue
            val rid = pr.s("id").trim()
            if (rid.isBlank() || rid == patientId || !PatientModel.isDeclaredSeparateRowId(rid, mobileDigits)) continue
            otherDeclaredRowIds.add(rid)
            pr.s("patientId").trim().takeIf { it.isNotBlank() }?.let { otherDeclaredCodes.add(it.uppercase()) }
        }
        buildChamberNotes(
            followups, mobileDigits, patientId, patientCode, patient.s("name"),
            selectedIsDeclaredSeparate, otherDeclaredRowIds, otherDeclaredCodes
        )
        // Clinical records may be keyed by EITHER the patient's row id (the
        // WebView saves medical with patientId:p.id) OR the P-xxxx patientId (the
        // native clinical modules use RoleSession.currentPatientId). Match both so
        // prescriptions/diet/checkups show regardless of which app wrote them.
        val uuid = patient.s("id")
        val pcode = patient.s("patientId")
        val medFilter = when {
            uuid.isNotBlank() && pcode.isNotBlank() -> "or=(patientId.eq.$uuid,patientId.eq.$pcode)"
            pcode.isNotBlank() -> "patientId=eq.$pcode"
            uuid.isNotBlank() -> "patientId=eq.$uuid"
            else -> null
        }

        // TK-REQUESTED (2026-07-27), ধাপ ২: the payments read below now uses
        // the ONE shared identity rule (PatientIdentity), so this screen and
        // the Follow-up payment history can never disagree about which rows
        // belong to this patient.
        val payFilter = PatientIdentity.identityFilter(uuid, pcode)

        // TK-REPORTED BUG FIX (2026-07-25): payments here used to be matched
        // by mobile ONLY. Follow-up's own Paid/Due (FollowUpRepository) has
        // always matched by patientId (the payments row's real foreign key --
        // every payment write already stores it, see PatientModel/PaymentModel
        // buildXPaymentRow). If a payment row's own "mobile" field is ever
        // imperfect (spacing/format), Timeline silently missed that payment
        // -- Paid/Due looked right on the Follow-up card but wrong here,
        // exactly the mismatch TK's photo-proof showed. Now ALSO fetched by
        // patientId and merged in (id-deduped) -- purely additive, can only
        // find MORE of this patient's real payments than before, never fewer.
        var payments = preTimeline[3]
        // 🔴🔒 V1614 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ, TK নিজে নিশ্চিত করেছেন
        // "আলাদা মানুষ") -- এক মোবাইলে সত্যিই দু'জন আলাদা মানুষ থাকতে পারেন
        // (পরিবারে এক ফোন)। স্টাফ "Different Patient — Same Mobile" চাপলে সেই
        // দ্বিতীয় মানুষের patients-সারির id `pat_<mobile>_...` ফরম্যাটে বসে
        // (PatientModel.isDeclaredSeparateRowId) — এটাই একমাত্র প্রমাণ। উপরের
        // মোবাইল-ধরে-আনা payments-এ তাই এমন কোনো সারি থাকলে, যেটার নিজের
        // patientId **প্রমাণিতভাবে অন্য কারো** (declared-separate, আর সেটা আমি
        // নই), সেটা এই রোগীর Paid/Estimated-এ গোনা হবে না। ⛔ প্রমাণ ছাড়া
        // কোনো সারি কখনো বাদ যায় না (PatientIdentity.kt-এর "হাইড করার চেয়ে
        // বাড়তি দেখানো নিরাপদ" নিয়ম অটুট) -- এটা সেই নিয়মেরই নতুন প্রয়োগ,
        // ব্যতিক্রম নয়।
        run {
            val kept = JSONArray()
            for (i in 0 until payments.length()) {
                val row = payments.optJSONObject(i) ?: continue
                val owner = row.s("patientId").trim()
                val exactMine = owner == patientId || (pcode.isNotBlank() && owner.equals(pcode, ignoreCase = true))
                if (exactMine) { kept.put(row); continue }
                // A declared secondary patient never borrows mobile-only legacy money from the primary patient.
                if (selectedIsDeclaredSeparate) continue
                // Primary patient: reject only identities that are proven to belong to a declared secondary patient.
                if (otherDeclaredRowIds.contains(owner) || otherDeclaredCodes.contains(owner.uppercase())) continue
                kept.put(row)
            }
            payments = kept
        }
        // TK-REPORTED (2026-07-27, "ডাটা লোড হতে প্রচুর সময় লাগে" -- the
        // Patient Details header sat on "Loading..."): these two reads do not
        // depend on each other (one is this patient's payments filed under
        // their row id, the other is their clinical records), yet they ran one
        // after the other, so the screen waited for the sum of both. Same two
        // queries, same results, same use below -- only the waiting is
        // shorter.
        val secondRound = runBlocking {
            val payDef = async(Dispatchers.IO) {
                // TK-REQUESTED (2026-07-27), ধাপ ২: this asked only by the row
                // id. The web app's Chamber screen files a payment under the
                // human Patient ID code instead, so those rows were missed
                // here whenever the mobile on them wasn't a clean match --
                // Paid/Due then differed from the Follow-up card. Same single
                // request, now covering both identities (exactly the same
                // "or=" pattern already used for medical just above). Purely
                // additive: it can only find MORE of this patient's payments.
                if (payFilter != null) SupabaseClient.fetchList("payments", payFilter, 500)
                else null
            }
            val medDef = async(Dispatchers.IO) {
                // 🔴🔒 V794 — ছবি ছাড়া (Timeline শুধু type/details/selected/তারিখ পড়ে)
                if (medFilter != null) SupabaseClient.fetchListSlim("medical", medFilter, 500,
                        SupabaseClient.MEDICAL_COLS) else null
            }
            Pair(payDef.await(), medDef.await())
        }
        val byPidOrNull = secondRound.first
        if (byPidOrNull != null) {
            val byPid = byPidOrNull
            val seenIds = HashSet<String>()
            for (i in 0 until payments.length()) seenIds.add(payments.getJSONObject(i).s("id"))
            for (i in 0 until byPid.length()) {
                val row = byPid.getJSONObject(i)
                if (seenIds.add(row.s("id"))) payments.put(row)
            }
        }
        context?.let { ctx -> try { LocalWorkflowStore(ctx).markSyncedWhereCloudCaughtUp("payments", payments) } catch (_: Throwable) { }; payments = mergeWithPending(payments, LocalWorkflowStore(ctx).pendingPayments(), mobileDigits) }   // 🔴 V1311 (তালিকা ৪২৩)
        val medical = secondRound.second

        // TK-REPORTED BUG FIX (2026-07-24): same root cause as section 111
        // (Follow-up tab's "syncing..." bug) and section 113 (Edit Record
        // silently failing) -- a registered patient's "followups" row can
        // be missing here too (dual-write gap, this device or another
        // never finished syncing it), which left followupStage blank
        // ("" -- neither "Inquiry" nor "Patient" nor "Treatment"). The
        // Patient Timeline header/buttons then fell through to the wrong
        // (Patient/Treatment-stage) button layout for a patient who was
        // actually only at Visit stage -- Full Journey stayed visible and
        // Payment never relabelled to "Advance", exactly what TK's photo-
        // proof showed. Self-heals the missing row (same as section 111)
        // AND corrects the value used for THIS response immediately, so
        // the very first render is already right -- not just after a
        // future reload once the self-heal write has landed.
        // TK-REPORTED BUG FIX (2026-07-25, found in deep audit): a patient
        // who has moved through Inquiry -> Patient -> Treatment has a
        // SEPARATE followups row created at each transition (never the
        // same row updated in place -- see buildVisitFollowUpRow /
        // FollowUpRepository's own stage-change writes), so this mobile
        // can have MULTIPLE followups rows. Supabase does not guarantee
        // which one comes back first, so blindly taking index 0 could pick
        // an OLDER, less-progressed stage (e.g. show the Inquiry-only
        // header/table for a patient who is actually already registered)
        // -- exactly the wrong-view bug TK's photo-proof caught. Now picks
        // whichever row has the MOST PROGRESSED real stage among all of
        // them, so the header/table always reflects where this patient
        // truly is today.
        fun stagePriority(s: String): Int = when {
            s.equals("Treatment", true) || s.equals("Treatment Running", true) -> 3
            s.equals("Patient", true) -> 2
            s.equals("Inquiry", true) -> 1
            else -> 0
        }
        val allFollowups = (0 until followups.length())
            .mapNotNull { followups.optJSONObject(it) }
        // 12.08.2026, GST live proof: an old Cancelled Treatment row and a new
        // Active Patient row shared the same demo mobile.  Choosing only the
        // highest stage selected the old row, so "Incomplete" closed the wrong
        // stage and the active Visit card remained.  Prefer the active row;
        // retain the old fallback only when no active row exists (history view).
        // Prefer rows explicitly linked to the patient currently open.  Mobile
        // numbers can be reused (family numbers and old demo data), so a row
        // belonging to another patient must not decide this patient's action.
        val patientRowId = patient.s("id")
        val linkedFollowups = allFollowups.filter {
            (patientId.isNotBlank() && it.s("patientId") == patientId) ||
                (patientRowId.isNotBlank() && it.s("refId") == patientRowId)
        }
        val selectionPool = if (selectedIsDeclaredSeparate) linkedFollowups else linkedFollowups.ifEmpty { allFollowups }
        val activeFollowups = selectionPool.filter {
            val status = it.s("status")
            !status.equals("Cancelled", true) &&
                !status.equals("Incomplete", true) &&
                !status.equals("Rejected", true) &&
                !status.equals("Closed", true)
        }
        val bestFollowup = (activeFollowups.ifEmpty { selectionPool })
            .maxWithOrNull(
                // 🟢🔒🔒 V638 (২৪.০৮.২০২৬, TK-রিপোর্ট — একাধিকবার আসা রোগীর
                // Treatment Progress আবার ফাঁকা দেখানো একই কারণ এখানেও) —
                // সমান stage-এর একাধিক সারি থাকলে (একাধিক ভিজিট) আগে
                // অনির্দিষ্ট ক্রমে প্রথমটাই বাছত। এখন সমান stage-এ সবচেয়ে
                // সাম্প্রতিক (updatedAt/createdAt) সারিটাই জেতে —
                // ChamberAttendanceRepository-র একই টাই-ব্রেকার এখানেও।
                compareBy<org.json.JSONObject> { stagePriority(it.s("stage")) }
                    .thenBy { it.s("updatedAt").ifBlank { it.s("createdAt") } }
            )
        var effectiveFollowupStage = bestFollowup?.s("stage") ?: ""
        if (followups.length() == 0 && patientId.isNotBlank()) {
            // Infer the correct stage from real payment data: a genuine
            // (non visit_fee/attendance_mark) payment means Treatment has
            // already started; otherwise this is just a registered Visit.
            // BUG FIX (2026-07-26, found in full-project audit): the marker
            // rows written by Chamber Attendance's "Marked Expected" button
            // carry payType="chamber_expected" with amount=0.0 (see
            // PaymentModel.buildExpectedMarkRow). They were NOT listed here,
            // so simply marking somebody Expected counted as a real
            // treatment payment and wrote a "Treatment" stage row for a
            // patient who had paid nothing. Excluded now, exactly like the
            // other zero-amount marker rows next to it. Nothing else in this
            // function changes.
            var hasRealTreatmentPayment = false
            for (i in 0 until payments.length()) {
                val pay = payments.optJSONObject(i) ?: continue
                val payType = pay.optString("payType", "")
                if (payType == "visit_fee" || payType == "attendance_mark" ||
                    payType == "bill_edit" || payType == "chamber_expected") continue
                hasRealTreatmentPayment = true
                break
            }
            val inferredStage = if (hasRealTreatmentPayment) "Treatment" else "Patient"
            effectiveFollowupStage = inferredStage
            /* ═══════════════════════════════════════════════════════════════
               🔴 V406 (16.08.2026) — **এটাই ছিল Follow-up-এ এক রোগীর ৬৩/৫৭/৪৩/১৬
                  সারি হয়ে যাওয়ার আসল কারণ।**

               কীভাবে ধরা পড়ল — দুটো চিহ্নই এই কোডে লেখা ছিল:
                 • `history` ফাঁকা  → নিচের `.put("history", JSONArray())`
                 • সব সারির `createdAt` **হুবহু এক** → নিচে `createdAt`-এ এখনকার
                   সময় নয়, **রোগীর সারির createdAt কপি** হত। তাই সারিগুলো আসলে
                   সপ্তাহ ধরে তৈরি হলেও তারিখ-সময় এক দেখাত। "এক মিলিসেকেন্ডে
                   ৬৩টা" আসলে **৬৩ বার heal চলার** ফল।

               কেন বারবার চলত (দুটো দোষ একসাথে):
                 ১) উপরের শর্ত `followups.length() == 0` — কিন্তু `followups` আসে
                    `fetchListSlim` থেকে, যা **পড়া ব্যর্থ হলেও খালি তালিকাই
                    ফেরায়**। নেট একটু দুর্বল হলেই অ্যাপ ভাবত "এই রোগীর কোনো
                    ফলোআপ নেই" — অথচ ছিল। এটাই fail-open ফাঁদ।
                 ২) প্রতিবার **নতুন random UUID** — আগের সারিটার সঙ্গে মিলত না,
                    তাই প্রতিবার একটা করে নতুন সারি যোগ হত।

               সমাধান (ওয়েবের প্রমাণিত নিয়মের হুবহু নকল — `app.js`-এ
               `'fu_pat_'+p.id`, খাতার সারি B626):
                 • **স্থির id** `fu_pat_<রোগীর সারির id>` ⇒ বারবার চললেও একই
                   সারিতেই বসে, নতুন সারি তৈরি হয় না। ওয়েব ও ফোন এখন **একই id**
                   ব্যবহার করে, তাই দুই দিক থেকেও আর ডুপ্লিকেট হবে না।
                 • `updatedAt` আর পিছিয়ে দেওয়া হয় না। (আগে পিছিয়ে দেওয়ায় সারিটা
                   `updatedAt.desc` তালিকার শেষে পড়ত ও delta-sync-ও একে দেখত না —
                   ফলে পরের বার আবার "নেই" মনে হত।)

               🔴🔴 **সবচেয়ে জরুরি — স্থির id একা বসালে হিতে বিপরীত হত।**
               স্থির id মানে upsert; পড়া ব্যর্থ হলে অ্যাপ ভাবত "নেই", আর **আসল
               সারিটার উপরেই ফাঁকা history ও callCount=0 বসিয়ে দিত** — অর্থাৎ
               ডুপ্লিকেটের বদলে **তথ্য মুছে যেত**। তাই লেখার ঠিক আগে একটা
               **fail-closed** যাচাই বসানো হলো (`healAllowed`, নিচে):
                 `fetchListOrNull` পড়া ব্যর্থ হলে `null` ফেরায় (খালি তালিকা নয়)।
                 · null (পড়াই গেল না)  → **কিছুই লেখা হয় না**
                 · সারি পাওয়া গেল      → **কিছুই লেখা হয় না**
                 · সফলভাবে ০টি সারি     → তবেই লেখা হয়
               ⛔ কোনো পুরনো সারি মোছা হয় না — সেটা TK-এর আলাদা সিদ্ধান্ত।
               ⛔ FollowUpHealGuard-এর আগের যাচাইটাও আগের মতোই থাকল।
               ═══════════════════════════════════════════════════════════════ */
            val healRow = JSONObject()
                .put("id", "fu_pat_" + patient.s("id"))
                .put("refId", patient.s("id"))
                .put("patientId", patient.s("patientId"))
                .put("mobile", patient.s("mobile"))
                .put("name", patient.s("name"))
                .put("branch", patient.s("branch"))
                .put("disease", patient.s("disease").ifBlank { patient.s("diagnosis") })
                .put("address", patient.s("address"))
                .put("age", patient.s("age"))
                .put("sex", patient.s("sex"))
                .put("stage", inferredStage)
                .put("date", patient.s("registrationDate").ifBlank { patient.s("date") })
                .put("registrationDate", patient.s("registrationDate").ifBlank { patient.s("date") })
                .put("visitDate", patient.s("visitDate").ifBlank { patient.s("registrationDate").ifBlank { patient.s("date") } })
                .put("lastRemark", if (inferredStage == "Treatment") "Treatment payment / Advance received" else "Registered patient / Visit created")
                .put("nextFollow", "")
                .put("callCount", 0)
                .put("status", "Active")
                .put("history", JSONArray())
                .put("createdBy", patient.s("createdBy"))
                .put("createdAt", patient.s("createdAt"))
                // 🔴 V406: আর পিছিয়ে দেওয়া হয় না — এখনকার সময়।
                .put("updatedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()))
            Thread {
                try {
                    // 🔴 V406 — fail-closed যাচাই। `fetchListOrNull` পড়া ব্যর্থ হলে
                    //    `null` ফেরায়; খালি তালিকা মানে **সত্যিই** কোনো সারি নেই।
                    //    ⛔ শুধু `id` ঘরটা চাওয়া হচ্ছে — সবচেয়ে সস্তা পড়া।
                    val proof = SupabaseClient.fetchListOrNull(
                        "followups", "mobile=like.*$mobileDigits", 500, select = "id"
                    )
                    val healAllowed = (proof != null && proof.length() == 0)
                    if (healAllowed && FollowUpHealGuard.liveSourceStillExists(healRow)) {
                        SupabaseClient.upsert("followups", healRow)
                    }
                } catch (_: Throwable) { }
            }.start()
        }

        val entries = mutableListOf<TimelineEntry>()

        /* 🟢🔒 B680 (১৫.০৮.২০২৬, TK-অনুমোদিত) — **একই কথা আর দু'বার উঠবে না**।
           TK-এর রিপোর্ট (ছবিসহ): "আগামী শনিবার আসবেন" — একই লেখা দুটো সারিতে।
           **কারণ:** কথাটা দুই জায়গায় জমা — Enquiry-র নিজের `remarks` ঘরে একবার, আর
           Follow-up-এর `history`-তে একবার। নিচের `seenHist` শুধু history-র ভিতরে
           ডুপ্লিকেট আটকাত, **Enquiry বনাম history** মেলাত না।
           **সমাধান (নিচে, history পড়ার পরে):** লেখা হুবহু এক হলে **history-র সারিটাই
           রাখা হয়** (কারণ ওটাতে ৩-বার চেপে রিমার্ক ঠিক করার সুবিধা আছে), আর
           Enquiry-র সারিটার **সময়টা** ওতে তুলে দেওয়া হয় — তাই সময়ও হারায় না।
           ⛔ লেখা ফাঁকা হলে কিছুই মেলানো হয় না (নইলে "Enquiry created" সারিটাই হারাত)। */
        var enquiryIdx = -1

        // Enquiry
        if (enquiries.length() > 0) {
            val e = enquiries.getJSONObject(0)
            entries.add(entry("Enquiry created",
                e.s("date").ifBlank { e.s("createdAt") },
                e.s("receivedBy").ifBlank { e.s("createdBy") },
                e.s("remarks")).copy(enquiryRowId = e.s("id"), callTime = e.s("createdAt")))
            enquiryIdx = entries.lastIndex          // 🟢 B680
        }

        // Follow-up history log
        // TK-REPORTED BUG FIX (2026-07-24): a remark saved via the Follow-up
        // card (updateRemark) could go missing from this Timeline entirely
        // -- the card correctly showed the new "Last Remark", but Timeline
        // kept showing only the old/original entry. Root cause: this used
        // to read history from ONLY followups.getJSONObject(0) -- if more
        // than one followups row exists for this mobile (e.g. the
        // self-heal safety net in FollowUpRepository, section 143, creating
        // a second row when the "real" one briefly looked missing),
        // Supabase doesn't guarantee which row comes back first, so the
        // update could land on a DIFFERENT row than the one Timeline was
        // reading. Now merges history from EVERY followups row for this
        // mobile (not just the first), de-duplicated by date+staff+remark,
        // so an update is never invisible here no matter which row it hit.
        if (followups.length() > 0) {
            val seenHist = HashSet<String>()
            for (fi in 0 until followups.length()) {
                val fRow = followups.getJSONObject(fi)
                if (!followUpSafeForPatient(
                        fRow, mobileDigits, patientId, pcode, patient.s("name"),
                        selectedIsDeclaredSeparate, otherDeclaredRowIds, otherDeclaredCodes
                    )) continue
                val hist = fRow.optJSONArray("history")
                val fuId = fRow.s("id")
                if (hist != null) {
                    for (i in 0 until hist.length()) {
                        // CRASH-SAFETY FIX (TK-reported, 2026-07-16): getJSONObject(i)
                        // used to throw and crash the WHOLE APP if a single old/
                        // malformed history entry wasn't a proper object -- Android
                        // then relaunches at Login/Dashboard, which looks exactly
                        // like "View All takes me back to Home". Now a bad entry is
                        // just skipped; every other entry still shows normally.
                        val h = hist.optJSONObject(i) ?: continue
                        val status = h.s("status").ifBlank { "Follow-up" }
                        val dedupeKey = "${h.s("date")}|${h.s("staff")}|${h.s("remark")}"
                        if (!seenHist.add(dedupeKey)) continue
                        entries.add(entry(status, h.s("date"), h.s("staff"), h.s("remark"))
                            .copy(followUpHistoryId = fuId, followUpHistoryIndex = i, callTime = h.s("time")))
                    }
                }
            }
        }

        /* 🟢 B680 — Enquiry-র সারি আর history-র সারি একই কথা বললে একটাই রাখা হয়। */
        if (enquiryIdx >= 0) {
            val enq = entries[enquiryIdx]
            val enqNote = enq.note.trim()
            if (enqNote.isNotEmpty()) {
                val twinIdx = entries.indexOfFirst { other ->
                    !other.followUpHistoryId.isNullOrBlank() &&   // ⛔ ঘরটা nullable
                        other.note.trim().equals(enqNote, ignoreCase = true) &&
                        other.date.take(10) == enq.date.take(10)
                }
                if (twinIdx >= 0) {
                    // history-র সারিটা রাখি; সময় না থাকলে Enquiry-র সময়টা তুলে দিই
                    if (entries[twinIdx].callTime.isBlank() && enq.callTime.isNotBlank()) {
                        entries[twinIdx] = entries[twinIdx].copy(callTime = enq.callTime)
                    }
                    entries.removeAt(enquiryIdx)
                }
            }
        }

        /* 🔴🔒 V399 (16.08.2026, TK-রিপোর্ট ছবিসহ — ETA ORAIN: একই "KAL AAYENGE"
           দু'বার, একটায় 11.59 AM আর একটায় "—")।

           **কেন হয়:** একই কল দু'জায়গায় জমা থাকে — `enquiries.remarks`
           (`EnquiryModel.kt:51`) আর `followups.history[0].remark`
           (`EnquiryModel.kt:68-73`)। উপরের B680 মেলানোর চেষ্টা করে, কিন্তু সেটা
           শুধু **Enquiry-সারির** সঙ্গে মেলায় আর লেখা হুবহু মিললে তবেই।
           দুটো history-সারির `staff` ঘরে দু'রকম মান থাকলে (কোথাও মোবাইল,
           কোথাও নাম — `PatientModel.kt:164` বনাম `EnquiryModel.kt:72`) উপরের
           `date|staff|remark` চাবিও আলাদা হয়ে যায়, তাই দুটোই থেকে যায়।

           **নিরাপদ নিয়ম:** স্টাফের লেখা আসল রিমার্কে **সবসময় সময় থাকে**
           (`FollowUpRepository.kt:1912/2051/2158`); সময়-হীন সারি মানেই অ্যাপের
           নিজের বসানো নকল। তাই — একই দিনে একই লেখার দুটো সারি থাকলে, আর
           তাদের একটায় সময় থাকলে, **সময়-হীনটাই বাদ যায়**।
           ⛔ দুটোতেই সময় থাকলে কিছুই বাদ যায় না (সত্যিই দুটো আলাদা কল)।
           ⛔ দুটোই সময়-হীন হলেও কিছুই বাদ যায় না।
           ⛔ ডেটাবেসে কিচ্ছু বদলায় না — এটি শুধু **দেখানোর** নিয়ম। */
        try {
            val timedKeys = HashSet<String>()
            for (e in entries) {
                if (e.callTime.isNotBlank() && e.note.trim().isNotEmpty()) {
                    timedKeys.add(e.date.take(10) + "|" + e.note.trim().lowercase())
                }
            }
            if (timedKeys.isNotEmpty()) {
                val it2 = entries.iterator()
                while (it2.hasNext()) {
                    val e = it2.next()
                    if (e.callTime.isBlank() && e.note.trim().isNotEmpty() &&
                        timedKeys.contains(e.date.take(10) + "|" + e.note.trim().lowercase())
                    ) it2.remove()
                }
            }
        } catch (_: Throwable) { }

        // Registration / Visit
        if (patientId.isNotBlank()) {
            // 🔴🔴🔒 খাতার সারি B453 (TK-নির্দেশ, 05.08.2026 — "চেকআপ করার
            // আগে আমাকে পেশেন্টের সম্পূর্ণ হিস্ট্রি দেখতে হবে, তারপর চেকআপ
            // করব")। TK স্পষ্ট করেছেন: Duration/Previous Treatment
            // Check-up খোলার আগেই "History" (Full Journey) বোতাম থেকে
            // দেখা যেতে হবে। তাই Registration-এর নোটের সাথেই এখন এই দুটো
            // তথ্যও জোড়া লাগানো হলো। ⛔ এটা শুধু **দেখানোর** জন্য —
            // আসল `patients.sinceWhen`/`previousTreatment` কলাম এখানে
            // বদলায় না, Doctor Checkup-এর নিজস্ব প্রি-ফিল (fill() ফাংশন)
            // আগের মতোই সরাসরি সেই কলাম থেকে কাজ করে।
            // 🔒 TK-এর দ্বিতীয় প্রশ্নের উত্তর (যাচাই করা, ইতিমধ্যেই ঠিক
            // আছে): ডাক্তার Check-up-এ Duration/Previous Treatment বদলে
            // Save করলে সেটা `medical` টেবিলের নিজস্ব রেকর্ডে যায় —
            // `patients.sinceWhen`/`previousTreatment` (Registration-এর
            // আসল, মূল তথ্য) কখনো ওভাররাইট হয় না, তাই এখানে "History"-তে
            // Registration-এর আসল তথ্যই সবসময় থেকে যাবে, ডাক্তার পরে
            // ভুল ধরলেও।
            val regDuration = patient.s("sinceWhen")
            val regPrevTreatment = patient.s("previousTreatment")
            val regNote = listOfNotNull(
                patient.s("complaint").ifBlank { null },
                if (regDuration.isNotBlank()) "Duration: $regDuration" else null,
                if (regPrevTreatment.isNotBlank()) "Previous Treatment: $regPrevTreatment" else null
            ).joinToString(" | ")
            entries.add(entry("Registration / Visit",
                patient.s("registrationDate").ifBlank { patient.s("date") },
                patient.s("registeredBy").ifBlank { patient.s("createdBy") },
                regNote).copy(
                    sortKey = patient.s("createdAt").ifBlank {
                        patient.s("registrationDate").ifBlank { patient.s("date") }
                    },
                    callTime = patient.s("createdAt"),
                    // ✏️🔒 V736 — এডিটের জন্য আসল ঠিকানা ও আসল তিনটে লেখা
                    regPatientRowId = patient.s("id").ifBlank { null },
                    regComplaint = patient.s("complaint"),
                    regDuration = regDuration,
                    regPrevTreatment = regPrevTreatment
                ))
        }

        // Payments
        // TK-REPORTED BUG FIX (2026-07-25): the ordinal label (Advance/2nd
        // Payment/3rd Payment...) used to be trusted exactly as it was
        // stored at save-time -- but that stored label came from a
        // client-side count taken when the Payment screen opened, which can
        // race across two staff/devices taking payments for the same
        // patient close together (whichever hadn't synced yet gets an
        // undercount). Recomputing it HERE, fresh, from the full sorted
        // list of this patient's real treatment payments, means the label
        // shown is always correct once every payment has synced -- self-
        // healing, no change to how/when a payment is actually saved (the
        // race-prone part), so this carries none of that risk.
        val treatmentPaymentsSorted = (0 until payments.length())
            .mapNotNull { payments.optJSONObject(it) }
            .filter { PaymentModel.isOrdinalTreatmentPayment(it.optString("payType", ""), it.s("remarks")) }
            .sortedBy { it.s("createdAt").ifBlank { it.s("date") } }
        // 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত): **নম্বর বাড়ে দিন ধরে, সারি
        // ধরে নয়।** TK: *"একদিনে একটা পেমেন্টই এলাউ হবে... যতবারই ওই একদিনে
        // পেমেন্ট করবে সেটা সেকেন্ড পেমেন্ট হিসাবেই ধরা হবে।"*
        // ইন্টারনেট ধীর থাকায় স্টাফ একই টাকা ৩-৪ বার সেভ করে ফেললে আগে
        // Advance · 2nd · 3rd · 4th হয়ে যেত। এখন **একই দিনের সব সারি একটাই
        // নম্বর** পায়। ⛔ টাকার অঙ্কে হাত পড়ে না — শুধু নামটা ঠিক বসে।
        // ⛔ হিসাবটা এক জায়গাতেই লেখা (`PaymentModel.dayBasedLabelById`), তাই
        //    Payment History · রসিদ · Timeline তিন জায়গায় কখনো আলাদা হতে পারে না।
        val ordinalLabelById = HashMap<String, String>()
        ordinalLabelById.putAll(PaymentModel.dayBasedLabelById(treatmentPaymentsSorted))
        // তারিখ না থাকা খুব পুরনো সারি (উপরের হিসাবে ঢোকে না) — আগের নিয়মেই,
        // যাতে কোথাও নাম উধাও না হয়।
        treatmentPaymentsSorted.forEachIndexed { idx, p ->
            val id = p.s("id")
            if (id.isNotBlank() && !ordinalLabelById.containsKey(id)) {
                ordinalLabelById[id] = PaymentModel.ordinalPaymentLabel(idx + 1)
            }
        }
        var totalPaid = 0.0
        var lastPayDate = ""
        var lastPayBy = ""
        /* 🕐🔒 V1172 (০৭.০৯.২০২৬, TK-অনুমোদিত পরিকল্পনা) — "Treatment Complete"
           সারির **সময়**। আগে বসত না, তাই ঘরটা `—` দেখাত (TK: *"তাছাড়া এখানে
           টাইম নেই কেন?"*)। এখন যে টাকার সারিতে বিল পুরো হলো, তার নিজের
           সময়টাই বসে। ⛔ সময় জমা না থাকলে (খুব পুরনো সারি) আগের মতোই `—` —
           আন্দাজে কোনো সময় বসানো হয় না। */
        var lastPayAt = ""
        /* ⛔ **শেষ-ভরসা:** ছাঁকনির পরে একটাও সারি না মিললে (যেমন সব সারিতেই
           তারিখ ফাঁকা) যেন সারিটা **ফাঁকা তারিখে** না বসে — তখন আগের নিয়মটাই
           (যেকোনো সারি) চলে। অর্থাৎ এই বদলে কোনো অবস্থাতেই আগের চেয়ে খারাপ
           হয় না। */
        var anyPayDate = ""
        var anyPayBy = ""
        var anyPayAt = ""
        for (i in 0 until payments.length()) {
            val p = payments.optJSONObject(i) ?: continue
            /* 🔴🔒 V1376 (১২.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ — SAIF RAZA-র কার্ডে
               MANZAR ALAM-এর ₹400 Visit Fee ভুল করে দেখাচ্ছিল): `payments`
               মোবাইল-ধরে আনা হয় (উপরে `byMobile`), তাই এক মোবাইলে একাধিক
               আলাদা রোগী থাকলে (V517 "Different Patient — Same Mobile")
               সবার টাকাই এখানে চলে আসে। ডাকার জায়গা যখন **নির্দিষ্ট করে
               জানিয়েছে কোন রোগী** (`preferRowId`/`preferPatientCode`
               → `forced`), তখন অন্য রোগীর টাকার সারি এই কার্ডে/Report
               Card-এ দেখানো ঠিক নয় — টাকা ঠিক জায়গাতেই জমা ছিল, শুধু
               ভুল রোগীর তালিকায় উঠে আসছিল (Paid/Due-র যোগফলও প্রভাবিত
               হতে পারত)।
               ⛔ `forced` জানা না থাকলে (সাধারণ মোবাইল-ধরে খোঁজা, কোনো
                  নির্দিষ্ট রোগী বাছা হয়নি) — আচরণ হুবহু আগের মতোই, এই
                  ছাঁকনি ছোঁয়ই না। */
            if (forced != null) {
                val payPatientId = p.s("patientId")
                if (payPatientId.isNotBlank() && payPatientId != forced.s("id")) continue
            }
            val storedLabel = p.s("payLabel").ifBlank { p.s("paymentLabel").ifBlank { "Payment" } }
            val label = ordinalLabelById[p.s("id")] ?: storedLabel
            val amt = p.optDouble("amount", 0.0)
            val pDate = p.s("date")
            val pBy = p.s("receivedBy").ifBlank { p.s("createdBy") }
            val payTypeRaw = p.optString("payType", "")
            val isTreatmentMoney = payTypeRaw.equals("treatment", ignoreCase = true)
            val split = if (isTreatmentMoney) PaymentModel.paymentSplit(p) else 0.0 to 0.0
            val dailyEvents = if (isTreatmentMoney) p.optJSONArray("dailyEvents") else null
            val eventCount = if (isTreatmentMoney) (dailyEvents?.length()?.coerceAtLeast(1) ?: 1) else 1
            // V452: when one daily row contains money taken by more than one staff,
            // show all real takers from dailyEvents. Row-level receivedBy alone would
            // incorrectly credit the whole day to the first staff member.
            val paymentBy = if (isTreatmentMoney && dailyEvents != null && dailyEvents.length() > 1) {
                val names = mutableListOf<String>()
                for (di in 0 until dailyEvents.length()) {
                    val ev = dailyEvents.optJSONObject(di) ?: continue
                    val by = ev.s("receivedBy").ifBlank { ev.s("createdBy") }.trim()   // 🔴 V819 — `optString` SQL NULL-এ আক্ষরিক "null" ফেরায় (V696/V812-এর ফাঁদ); `s()` সেটা ফাঁকা ধরে
                    if (by.isNotBlank() && !names.contains(by)) names.add(by)
                }
                names.joinToString(" / ").ifBlank { pBy }
            } else pBy
            val moneyModeText = if (isTreatmentMoney) PaymentModel.paymentBreakdown(split.first, split.second)
                else money(amt) + " · " + p.s("mode")
            val (icon, color) = style(label)
            // TK APPROVED (2026-07-16): Chamber Attendance's "Search & mark
            // Arrived" writes a ₹0 row with payType="attendance_mark" so it
            // shows up here too (Full Journey), same as TK asked -- but a
            // "₹0 · CASH" line would look like a broken payment. Show a
            // plain note instead; still the SAME row/id, still 3-tap
            // editable like any other entry below.
            val isAttendanceMark = p.optString("payType", "") == "attendance_mark"
            val isVisitFee = p.optString("payType", "") == "visit_fee"
            // 🔒 V217 (§B216, 31.07.2026): Refund row — Full Journey/Report Card-এর
            // Paid/Due হিসাব আগে refund-কে সাধারণ পেমেন্টের মতো **যোগ** করে
            // ফেলত (নিচের `totalPaid +=` ও পরের `runningPaid +=`-এ payType
            // বাদ দেওয়ার তালিকায় "refund" ছিল না) — PaymentRepository-এর
            // paid-হিসাব (যেটা Payment স্ক্রিনে দেখা যায়) ইতিমধ্যে approved
            // refund **বিয়োগ** করত, তাই Timeline/Report Card-এর Paid, আসল
            // Payment স্ক্রিনের চেয়ে বেশি দেখাত (double-error, উল্টো দিকে)।
            // এখন: approved refund → বিয়োগ, pending/rejected → হিসাবে কোনো
            // প্রভাব নেই (শুধু সারিটা history-তে "Pending/Rejected" লেখা
            // অবস্থায় দেখা যায়, টাকা কমে না)।
            val isRefund = p.optString("payType", "") == "refund"
            /* 🔴🔒 V1301 (১০.০৯.২০২৬, তালিকা ৪১৩ — Tinku Bauli, TK: *"৩"*): bill_edit /
               chamber_expected / attendance_mark **চিহ্ন-সারি** — টাকা থাকার কথা নয়। কেউ
               ৩-ট্যাপ এডিটে ওতে টাকা বসিয়ে দিলে (Tinku: ₹3,000) Timeline-এর Paid বাড়ত,
               অথচ Payment পর্দা ওই সারি দেখায়ই না ⇒ দুই পর্দায় দুরকম। এখন চিহ্ন-সারির
               টাকা Paid-এ কখনো যোগ হয় না — সব পর্দা এক নিয়মে। সারিটা দেখানো/মোছা অছোঁয়া। */
            val isMarkerRowMoney = PaymentModel.isMarkerOnlyRow(p.optString("payType", ""))
            val isApprovedRefund = PaymentModel.isApprovedRefund(p)
            val refundStatus = p.optString("refundApprovalStatus", "")
            // TK-REQUESTED CHANGE (2026-07-19): Progress should show the
            // staff-written Treatment remark (what actually happened with
            // the patient), not just "₹amount · MODE" -- that auto text is
            // now only the fallback when no remark was written.
            /* 🔵🔒 V533 (২২.০৮.২০২৬, TK-সিদ্ধান্ত: **"নোট আলাদা ঘরে রাখব"**)
               চিকিৎসার নোট এখন payments-এর **নিজস্ব `progress` ঘরে** — আর
               `remarks`-এর উপর বসে না। ⛔ পুরোনো সব সারিতে `progress` ফাঁকা,
               তাই সেখানে আগের মতোই `remarks`-ই পড়া হয় — **একটা পুরোনো নোটও
               হারায় না, একটাও পর্দা বদলায় না**। */
            val staffRemark = p.s("progress").ifBlank { p.s("remarks") }
            // TK-REPORTED BUG FIX (2026-07-25): remarks defaults to the
            // save-time label when the staff types nothing (see
            // PaymentModel.buildTreatmentPaymentRow). If that save-time
            // label was later found wrong (the race above) and corrected
            // for Type/title, showing the OLD label here as if it were a
            // real typed note would contradict the now-correct Type column
            // right next to it. Treat "remarks == the label it was saved
            // with" as no real remark at all, same as blank.
            // 🚨 TK-REPORTED, LIVE (27.07.2026, SADDAM — TK's photo): this used
            // to compare ONLY against the label the row was saved with, so the
            // Follow-up card's Advance (which saves the words "Advance Payment"
            // while the label is "Advance") slipped through as if a person had
            // typed it, and filled the Report Card's PROGRESS box with the app's
            // own payment text. Now it asks the ONE shared rule instead, which
            // knows every label the app writes by itself. A remark a person
            // really typed is never matched, so nothing real can be hidden.
            val isAutoFilledRemark = PaymentModel.isAutoPaymentRemark(staffRemark, storedLabel)
            // TK-REQUESTED (2026-07-25): when a real Treatment Progress
            // remark was typed for a day that also has a real payment,
            // show BOTH together -- "আজকে অপারেশন করা হলো — ₹5,000 · CASH" --
            // instead of the remark alone (which used to hide the money
            // entirely). This is TK's chosen alternative to adding a
            // separate Amount column: the money stays visible right next
            // to what it was for, in the same Note text.
            // 🚨 TK-REPORTED (29.07.2026, JONEKA BIBI) · খাতার সারি B59: এখানেও
            // এখন **টুকরো ধরে ছাঁকা** লেখাটাই ব্যবহার হয় (`typedPartOf`), তাই
            // অ্যাপের নিজের জোড়া-লাগা কথা (`Advance Payment · Chamber ONLINE
            // payment`) আর মানুষের লেখা সেজে টাইমলাইনেও বসতে পারবে না।
            val humanPart = PaymentModel.typedPartOf(staffRemark, storedLabel)
            val noteText = when {
                isAttendanceMark -> "Attendance confirmed (no payment)"
                // 🔵 B620 (11.08.2026, TK-নির্দেশ): "আসবে বলেছে" (chamber_expected) সারি
                // আসলে টাকা নয় — শুধু আসার তারিখের চিহ্ন (amount=0)। আগে else-শাখায় গিয়ে
                // "₹0 · CASH" দেখাত (ভাঙা পেমেন্টের মতো, বিভ্রান্তিকর)। এখন আসার তারিখ দেখাই।
                // "আসার তারিখ" NoBengali MAP-এ আছে → no-Bengali স্টাফে "Appointment date:"।
                p.optString("payType", "") == "chamber_expected" ->
                    "আসার তারিখ: " + FollowUpModel.displayDate(pDate)
                // 🔒 V217 (§B216): Refund row-এর নিজস্ব, পরিষ্কার লেখা — status-সহ,
                // যাতে pending/rejected refund কখনো "টাকা কমে গেছে" বলে ভুল না
                // বোঝায় (নিচের `noteText`-এর `humanPart`/সাধারণ শাখায় গেলে
                // অন্য পেমেন্টের মতোই দেখাত)।
                isRefund -> {
                    val statusTxt = when (refundStatus.lowercase()) {
                        "approved" -> "Refunded"
                        "rejected" -> "Refund rejected"
                        else -> "Refund pending approval"
                    }
                    val reasonTxt = if (staffRemark.isNotBlank()) " — $staffRemark" else ""
                    "$statusTxt — ${money(amt)} · ${p.s("mode")}$reasonTxt"
                }
                humanPart.isNotBlank() ->
                    if (amt > 0.0) "$humanPart — $moneyModeText" else humanPart
                else -> moneyModeText
            }
            // 🚨 TK-REPORTED (27.07.2026, SADDAM): the ONE value the Report Card's
            // PROGRESS box may print -- what a person actually typed, with the
            // app's own labels and the amount/mode text left out. The audit trail
            // an amount-correction appends ("… | Audit: …") is cut off here too,
            // so a corrected payment shows the real note, not the audit line.
            val explicitProgress = p.s("progress").trim()
            var typedOnly = when {
                explicitProgress.isNotBlank() -> PaymentModel.typedPartOf(explicitProgress, "").trim()
                isAttendanceMark || isAutoFilledRemark -> ""
                else -> PaymentModel.typedPartOf(staffRemark, storedLabel)
            }
            /* 🚨🔒 V1189 (০৭.০৯.২০২৬, TK-রিপোর্ট ও ফটো-প্রুফ পাশ, হুবহু):
               *"Treatment Progress এর ঘরে কিছু নেই কেন? চেম্বার গেটের ট্রিটমেন্টের
               প্রগ্রেসের ঘরে কিছু না লিখলে তো চেম্বার বন্ধই হয় না। তাহলে সেখান থেকে
               এখানে কেন অটোমেটিক আপডেট হচ্ছে না"*

               🔴 **আসল কারণ (কোডে মেপে বের করা, আন্দাজ নয়):** চেম্বার-বোর্ডের
                  TREATMENT PROGRESS ঘরে লেখা কথাটা জমা হয় **রোগীর ফলো-আপ সারিতে**
                  (`FollowUpRepository.updateRemark` → `lastRemark` ও `history`),
                  কিন্তু Report Card-এর PROGRESS ঘর পড়ে **টাকার সারির** নিজের
                  `progress`/`remarks` ঘর থেকে। দুটো সম্পূর্ণ আলাদা জায়গা ⇒ চেম্বারে
                  লেখা কথা এখানে কখনোই আসতে পারত না।

               ⇒ টাকার সারিতে মানুষের লেখা কিছু না থাকলে এখন **ঐ একই তারিখের**
                 চেম্বারের লেখাটা বসে। তারিখ ধরে মেলানো হয় বলে প্রতিটা ভিজিটে
                 **সেই দিনেরই** কথা যায়, শেষ কথাটা সব সারিতে বসে না।
               ⛔ এটা শুধু **দেখানোর** কাজ — ডেটাবেসে কিছু লেখা/বদলানো হয় না, তাই
                  পুরনো ভিজিটগুলোতেও নিজে থেকেই দেখা যাবে।
               ⛔ টাকার সারিতে লেখা থাকলে **সেটাই আগের মতো** থাকে; চেম্বারের কথা
                  কখনো তার উপরে বসে না।
               ⛔ অ্যাপের নিজের বসানো কথা (টাকার লেখা ইত্যাদি) আগের একই ছাঁকনিতেই
                  বাদ যায় (`isAutoPaymentRemark`), তাই PROGRESS ঘরে টাকার কথা ঢোকার
                  সুযোগ নেই। */
            if (typedOnly.isBlank() && pDate.isNotBlank()) {
                typedOnly = chamberNoteOn(pDate)
            }
            // 🔒 V217 (§B216): এই এক জায়গাতেই refund-এর সাইন ঠিক হয় — বাকি
            // সব হিসাব (totalPaid, day-merge, runningPaid) এখান থেকেই নেয়।
            val paidEffect = when {
                isAttendanceMark || isVisitFee || isMarkerRowMoney -> 0.0   // 🔴 V1301
                isRefund && isApprovedRefund -> -amt
                isRefund -> 0.0
                else -> amt
            }
            entries.add(TimelineEntry(
                icon, color, label, pDate, paymentBy, noteText,
                // ✏️🔒 V736 — শুধু মানুষের টাইপ করা অংশ (টাকার লাইন ছাড়া)
                payTypedNote = humanPart,
                paymentId = p.s("id"), paymentBranch = p.s("branch"),
                paymentPatientRowId = p.s("patientId"),   // 🔴🔒 V1376
                paymentAmount = amt,
                paymentMode = if (isTreatmentMoney) PaymentModel.splitMode(split.first, split.second) else p.s("mode").ifBlank { "CASH" },
                paymentCashAmount = split.first, paymentOnlineAmount = split.second,
                paymentEventCount = eventCount,
                payType = p.s("payType"),
                paidEffect = paidEffect,
                sortKey = p.s("createdAt").ifBlank { pDate },
                // TK-REPORTED FIX (2026-07-26): the Date/Time column showed a
                // bare dash for every Payment row, because callTime was only
                // ever filled for Enquiry and Follow-up call entries -- a
                // payment carries its real timestamp in "createdAt" but it was
                // simply never passed through to the table. Display-only: the
                // same value already used for sortKey just above, so nothing
                // about sorting, totals or 3-tap edit changes. Blank createdAt
                // (very old rows) still shows the dash exactly as before.
                callTime = p.s("createdAt"),
                typedRemark = typedOnly,
                isChamberVisitAnchor = Regex("^chamber( .+)? payment$", RegexOption.IGNORE_CASE)
                    .matches(p.s("remarks").trim())
            ))
            // 🔒 V217 (§B216): paidEffect-এই সাইন ঠিক করা আছে (উপরে) — এখানে
            // শুধু যোগ করলেই approved refund বিয়োগ ও pending/rejected বাদ যায়।
            totalPaid += paidEffect
            /* 🕐🔒 V1172 (০৭.০৯.২০২৬, TK-অনুমোদিত পরিকল্পনা) — TK, SAMOTI
               BARMAN-এর ছবিসহ: *"Treatment complete by Dr. kh mandal · bill
               Fully Paid 20000 — এগুলির জন্য বিভ্রান্ত হয়ে যাচ্ছি"*।

               **কারণ (কোডে প্রমাণিত):** "শেষ টাকা কবে, কে নিল" বাছার সময় আগে
               **সব সারি** ধরা হত — `attendance_mark` (Marked Arrived) ·
               `chamber_expected` (আসবে বলেছে) · `bill_edit` (Bill Edited)-এর
               মতো **₹০-র চিহ্ন-সারিগুলোও**। তাই SAMOTI-র ক্ষেত্রে ০৭.০৯-এর
               "Marked Arrived" (Dr. K.H MANDAL, ₹০) সারিটাই শেষ ধরা হয়েছিল ⇒
               নিচের "Treatment Complete" সারিতে **আজকের তারিখ ও ডাক্তারের নাম**
               উঠত, অথচ বিল সত্যিই পুরো হয়েছিল ০২.০৮.২০২৬-এ COB-BRANCH-এর
               নেওয়া ₹২০,০০০ Advance-এ। মনে হত ডাক্তার আজ চিকিৎসা শেষ করলেন।

               **এখন শুধু আসল টাকার সারি** ধরা হয় (চিহ্ন-সারি বাদ, আর অঙ্ক ০-র
               বেশি হতে হবে)। ছাঁকনিটা প্রকল্পের **আগে থেকেই থাকা প্রমাণিত**
               `PaymentModel.isMarkerOnlyRow` — নতুন কোনো নিয়ম বানানো হয়নি
               (V533/V549-এ এই একই ছাঁকনি তালিকা দেখানোয় ব্যবহার হয়)।

               ⛔ **টাকার কোনো অঙ্ক বদলায় না** — উপরের `totalPaid` আগের মতোই
                  প্রতিটা সারি ধরেই হয় (চিহ্ন-সারির টাকা এমনিতেই ০)। Paid ·
                  Due · Total · Report Card — সব অক্ষত।
               ⛔ শুধু "Treatment Complete" ও "Discount" সারির **তারিখ · সময় ·
                  কে** — এই তিনটেই ঠিক হয়। */
            val isMarkerRow = try { PaymentModel.isMarkerOnlyRow(payTypeRaw) } catch (_: Throwable) { false }
            if (!isMarkerRow && amt > 0.0 && pDate.isNotBlank() && pDate >= lastPayDate) {
                lastPayDate = pDate; lastPayBy = pBy; lastPayAt = p.s("createdAt")
            }
            if (pDate.isNotBlank() && pDate >= anyPayDate) {
                anyPayDate = pDate; anyPayBy = pBy; anyPayAt = p.s("createdAt")
            }
        }

        // APPROVED UPDATE #9: Treatment Complete entry when the bill is fully paid
        // (Due = 0). Synthesized from the last payment — no other module changed.
        val billTotal = patient.optDouble("bill", 0.0)
        // 🏷️🔒 V1508 — আসল বিল (ছাড়ের আগে), সরিয়ে আনা হলো যাতে TimelineData-তেও
        // ব্যবহার করা যায় (নিচে discGiven ব্লকও এখন এটাই পড়ে, দ্বিতীয়বার নয়)।
        val billBeforeDiscount = patient.optDouble("billBeforeDiscount", 0.0)
        // 🏷️ TK-APPROVED (03.09.2026): a discount given on this bill shows as
        // its own permanent history row -- how much, what the bill was before
        // and after, who gave it and why. Synthesized from the patients row
        // itself (no new table), exactly the way Treatment Complete below is.
        /* ⛔ V1172-এর শেষ-ভরসা এখানেই বসে — নিচের দুটো সারি (Discount ও
           Treatment Complete) এই তিনটে মান-ই ব্যবহার করে। */
        if (lastPayDate.isBlank()) { lastPayDate = anyPayDate; lastPayBy = anyPayBy; lastPayAt = anyPayAt }
        val discGiven = patient.optDouble("discount", 0.0)
        if (discGiven > 0.0) {
            val why = patient.s("discountReason")
            val note = buildString {
                // শিরোনামেই "Discount" লেখা থাকে, তাই নোটে আর দ্বিতীয়বার নয়।
                append(money(discGiven))
                if (billBeforeDiscount > 0.0) append(" · Bill ").append(money(billBeforeDiscount)).append(" → ").append(money(billTotal))
                if (why.isNotBlank()) append(" · ").append(why)
            }
            entries.add(entry(
                "Discount",
                patient.s("discountAt").take(10).ifBlank { lastPayDate },
                patient.s("discountBy").ifBlank { lastPayBy },
                note
            ).copy(sortKey = patient.s("discountAt"), callTime = patient.s("discountAt")))
        }
        if (billTotal > 0.0 && totalPaid >= billTotal) {
            /* 🕐 V1172 — সময়টাও বসল (আসল টাকার সারির নিজের `createdAt`)।
               ⛔ `sortKey`-ও সেই একই মান, তাই সারিটা এখন **ঠিক জায়গায়** বসে —
                  আগে তারিখ ধরে বসত, একই দিনের অন্য সারির সঙ্গে ক্রম গুলিয়ে যেত। */
            entries.add(entry("Treatment Complete", lastPayDate, lastPayBy, "Bill fully paid — " + money(billTotal))
                .copy(sortKey = lastPayAt.ifBlank { lastPayDate }, callTime = lastPayAt))
        }

        // Medical (prescription / diet / investigation / checkup)
        if (medical != null) {
            for (i in 0 until medical.length()) {
                val m = medical.optJSONObject(i) ?: continue
                val type = m.s("type").ifBlank { "Medical" }
                val note = m.s("details").ifBlank { m.s("selected").ifBlank { m.s("decision") } }
                entries.add(entry(type, m.s("date").ifBlank { m.s("createdAt") }, m.s("createdBy"), note).copy(
                    sortKey = m.s("createdAt").ifBlank { m.s("date") },
                    callTime = m.s("createdAt"),
                    // 🟢🔒 V676 — এই সারিটার আসল id + JSON (থাকলে) ধরে রাখা,
                    // যাতে আজকের নিজের Doctor Checkup এডিট করা যায়।
                    medicalRecordId = m.s("id"),
                    medicalSelected = m.s("selected")
                ))
            }
        }

        /* ╔═══════════════════════════════════════════════════════════
           🟢🔒 V1090 (০৫.০৯.২০২৬) — TK, MD AKBAR ALI-র ছবিসহ:
           *"check up done দুইবার কেন"*

           **কারণ (কোডে প্রমাণিত, আন্দাজে নয়):** স্টাফ একবারই লেখেন, কিন্তু
           V590 (২৩.০৮, TK-অনুমোদিত) থেকে লেখাটা **দুই জায়গায়** জমা হয় —
           ফলো-আপ খাতায় (`followups.history`) আর সেদিনের টাকার সারিতে
           (`payments.progress`, নইলে Report Card-এ উঠত না)। History পর্দা
           দুটোই দেখাত ⇒ এক কথা দু'বার।
           সঙ্গে দ্বিতীয় দোষ: `history`-র সারিতে ধরন লেখা থাকে না, তাই
           চিকিৎসার নোটটাও **"Called By"** সেজে উপরের **Visit Calls**-এ
           গোনা হত — যদিও কেউ ফোন করেননি।

           **এই নিয়মটা করে (শুধু দেখানোর, B680/V399-এর হুবহু একই ধাঁচ):**
           ① একই দিনে ফলো-আপ-নোটের লেখা আর টাকার সারির মানুষের-লেখা অংশ
              হুবহু মিললে ⇒ ওই ফলো-আপ সারিটা "চিকিৎসার নোট" চিহ্নিত হয়।
              টাকার সারি না থাকলেও ধরা পড়ে — লেখাটা পুরোপুরি চিকিৎসার
              চিপ দিয়ে বানানো হলে (`TreatmentQuickNotes.isQuickNoteText`)।
           ② **History পর্দায়** (separateRowsPerEvent) ওই টাকার সারি থেকে
              শুধু ওই পুনরাবৃত্ত বাক্যটা তোলা হয় — টাকা ও মোড থেকে যায়
              (`wlv1StripEchoedTreatmentNote`, নিচে — **একই দিনের একাধিক
              পেমেন্ট জোড়া লাগার পরে** চলে, নইলে জোড়া লাগার সময় লেখাটা
              আবার ফিরে আসত)। ⛔ তুললে লেখা একদম ফাঁকা হয়ে গেলে কিছুই
              তোলা হয় না।

           ⛔ ডেটাবেসে কিচ্ছু বদলায় না · `payTypedNote` (Report Card যেটা
              পড়ে) ছোঁয়াই হয় না · Report Card ও অন্য সব পাঠক অটুট (② শুধু
              History-তে) · ফলো-আপ সারিটা **থেকেই যায়**, তাই কে লিখেছেন ও
              কখন লিখেছেন — কিছুই হারায় না।
           ╚══════════════════════════════════════════════════════════ */
        try {
            val payTypedKeys = HashSet<String>()
            for (e in entries) {
                val typed = e.payTypedNote.trim()
                if (e.paymentId != null && typed.isNotEmpty())
                    payTypedKeys.add(e.date.take(10) + "|" + typed.lowercase())
            }
            for (i in entries.indices) {
                val e = entries[i]
                if (e.followUpHistoryId.isNullOrBlank()) continue
                val txt = e.note.trim()
                if (txt.isEmpty()) continue
                val isTreat = payTypedKeys.contains(e.date.take(10) + "|" + txt.lowercase()) ||
                    TreatmentQuickNotes.isQuickNoteText(txt)
                if (isTreat) entries[i] = e.copy(isTreatmentNote = true)
            }
        } catch (_: Throwable) { /* ⛔ শুধু দেখানোর সাজানো — ব্যর্থ হলে আগের মতোই দেখাবে */ }

        // OWNER CONFIRMED (12.08.2026): Registration, Visit and Visit Fee are
        // ONE user action.  They stay in their existing database rows (the
        // payment row is still required for accounts/audit), but Full Journey
        // must not present that single action as two separate events.  Merge
        // exactly one same-day Visit Fee into Registration for DISPLAY only.
        // Any accidental extra fee is deliberately left visible rather than
        // hidden.  Report Card and every non-History caller retain the old
        // behaviour because this is gated by separateRowsPerEvent.
        /* 🗂️🔒 V1170 (০৭.০৯.২০২৬, TK-অনুমতি: *"হ্যাঁ অনুমতি দিলাম,
           লুকিয়ে দিন সাবধানে, কোনো ভালো কাজ যেন খারাপ না হয়"*)

           TK, AMIT MAJUMDAR-এর History-র ছবিসহ: *"22/08/2026: 1.06 PM-এ ২ রকম
           সারি কেন?"*

           **কারণ (কোডে প্রমাণিত, আন্দাজে নয়):** ১.০৬ PM-এ স্টাফ **একটাই কাজ**
           করেছেন — রেজিস্ট্রেশন। কিন্তু ওই এক কাজে অ্যাপ তিন জায়গায় লেখে:
             ① `patients` সারি ⇒ `Registration / Visit` (অভিযোগ · Duration ·
                আগের চিকিৎসা — **আসল তথ্য, এটাই থাকবে**)
             ② পুরনো Inquiry সারিটা বন্ধ করার সময় তার খাতায়
                `Converted to Patient Registration` (`RegistrationRepository.kt`)
             ③ নতুন Patient সারিটা তৈরির সময় তার খাতায়
                `Registered patient / Visit created` (`PatientModel.kt`)
           ②③ দুটোই **অ্যাপের নিজের বসানো চিহ্ন** — কেউ ফোন করেননি, নতুন কোনো
           তথ্যও দেয় না। তবু History-তে সারি হয়ে বসত, আর ধরন না থাকায়
           **"Called By" সেজে `Enquiry Calls` গোনায়ও ঢুকত** (ছবিতে ৫, আসলে ৩)।

           **এই নিয়মটা করে (V1090/B680-এর হুবহু প্রমাণিত ধাঁচ):** শুধু
           **History পর্দায়** (`separateRowsPerEvent`) ওই দুটো সারি দেখানো হয় না।

           ⛔ ডেটাবেসে কিচ্ছু বদলায় না — `followups.history` অক্ষত, ক্লাউডেও
              হুবহু আগের লেখা; TK চাইলে যেকোনো দিন ফেরানো যায় (এক শর্ত সরালেই)।
           ⛔ Report Card · Chamber · Draft · Follow-up কার্ড — **একটাও ছোঁয়া
              হয়নি** (এটা শুধু History-র শাখায়)।
           ⛔ **সুরক্ষা:** `Registration / Visit` সারিটা যদি কোনো কারণে না থাকে
              (রোগীর সারি মুছে গেছে), তখন কিছুই লুকানো হয় না — নইলে ওই দিনের
              একমাত্র প্রমাণটাই হারিয়ে যেত।
           ⛔ শুধু ফলো-আপের খাতার সারি (`followUpHistoryId`) — কোনো পেমেন্ট বা
              রেজিস্ট্রেশন সারি এই নিয়মে কখনো পড়ে না।
           ⛔ লেখাটা **হুবহু** মিললে তবেই; স্টাফের নিজের লেখা কিছু বাদ যায় না। */
        run {
            try {
                val hasReg = entries.any { it.title.equals("Registration / Visit", ignoreCase = true) }
                if (separateRowsPerEvent && hasReg) {
                    val autoStubs = setOf(
                        "registered patient / visit created",
                        "converted to patient registration"
                    )
                    entries.removeAll { e ->
                        !e.followUpHistoryId.isNullOrBlank() &&
                            e.paymentId == null &&
                            autoStubs.contains(e.note.trim().lowercase())
                    }
                }
            } catch (_: Throwable) { /* ⛔ ব্যর্থ হলে আগের মতোই সব সারি দেখাবে */ }
        }

        val displayEntries = if (separateRowsPerEvent) {
            val regIndex = entries.indexOfFirst { it.title.equals("Registration / Visit", ignoreCase = true) }
            if (regIndex >= 0) {
                val reg = entries[regIndex]
                val regDay = reg.date.take(10)
                /* 🔴🔒 V1376 (১২.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ — SAIF RAZA-র
                   Timeline-এ MANZAR ALAM-এর Visit Fee ভুল করে জুড়ে গিয়েছিল):
                   এতদিন শুধু "একই দিনে visit_fee" দেখেই মেলানো হত। এক
                   মোবাইলে একাধিক আলাদা রোগী থাকলে (V517) ও দুজনেরই
                   registration একই দিনে হলে, একজনের Registration-নোটে
                   অন্যজনের Visit Fee জুড়ে যেতে পারত — টাকা ঠিক জায়গাতেই
                   জমা ছিল, শুধু এই স্ক্রিনে ভুল রোগীর সারিতে দেখাত।
                   ⛔ এখন patientId-ও মেলাতে হয় — **শুধু তখনই** পুরনো
                      (শুধু-দিন-ধরে) নিয়মে ফেরা হয় যদি কোনো একটা সারিতে এই
                      তথ্যটাই না থাকে (পুরনো/অসম্পূর্ণ ডেটা), তাই স্বাভাবিক
                      এক-রোগীর ক্ষেত্রে আচরণ এক অক্ষরও বদলায়নি। */
                val feeIndex = entries.indices
                    .filter { i ->
                        val e = entries[i]
                        e.payType == "visit_fee" && e.date.take(10) == regDay &&
                            (reg.regPatientRowId.isNullOrBlank() || e.paymentPatientRowId.isBlank() ||
                                e.paymentPatientRowId == reg.regPatientRowId)
                    }
                    .minByOrNull { i -> entries[i].sortKey.ifBlank { entries[i].date } }
                if (feeIndex != null) {
                    val fee = entries[feeIndex]
                    val combinedNote = listOf(reg.note, fee.note)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" | ")
                    val merged = reg.copy(
                        note = combinedNote,
                        // ✏️🔒 V736 — reg-এর তিনটে ঘর `copy` নিজেই ধরে রাখে;
                        //    পেমেন্টের মানুষের-লেখা অংশটা এখানে জুড়ে দেওয়া হলো
                        payTypedNote = fee.payTypedNote,
                        paymentId = fee.paymentId,
                        paymentBranch = fee.paymentBranch,
                        paymentAmount = fee.paymentAmount,
                        paymentMode = fee.paymentMode,
                        payType = fee.payType,
                        paidEffect = fee.paidEffect,
                        sortKey = fee.sortKey.ifBlank { reg.sortKey.ifBlank { reg.date } },
                        callTime = fee.callTime.ifBlank { reg.callTime }
                    )
                    entries.mapIndexedNotNull { index, item ->
                        when (index) {
                            feeIndex -> null
                            regIndex -> merged
                            else -> item
                        }
                    }
                } else entries.toList()
            } else entries.toList()
        } else entries.toList()

        // TK-REQUESTED ADDITION (2026-07-16): number visits oldest-first (1,2,3...)
        // and carry a running Paid/Due total forward through the whole history,
        // matching TK's paper register. Computed here (once, in the repository)
        // so the Activity/adapter doesn't need to re-derive it.
        // 🔒 V452 (19.08.2026, TK-approved A): Full Journey/History now follows
        // the locked money rule: one Treatment Payment per patient per calendar
        // day. Legacy duplicate DB rows are NOT deleted; they are grouped for
        // display/accounting here. Visit Fee, Refund, Medicine and every other
        // event remain separate. Cash/Online and staff attribution are preserved.
        val historyDisplayEntries = if (separateRowsPerEvent) {
            val groupedTreatment = LinkedHashMap<String, MutableList<TimelineEntry>>()
            val passthrough = mutableListOf<TimelineEntry>()
            for (e in displayEntries) {
                if (e.payType.equals("treatment", ignoreCase = true) && e.paymentId != null && e.date.take(10).isNotBlank()) {
                    groupedTreatment.getOrPut(e.date.take(10)) { mutableListOf() }.add(e)
                } else passthrough.add(e)
            }
            val mergedTreatment = groupedTreatment.values.map { sameDay ->
                if (sameDay.size == 1 && sameDay[0].paymentEventCount <= 1) sameDay[0]
                else {
                    val ordered = sameDay.sortedBy { orderKey(it) }
                    val latest = ordered.last()
                    val cash = ordered.sumOf { it.paymentCashAmount }
                    val online = ordered.sumOf { it.paymentOnlineAmount }
                    val total = ordered.sumOf { it.paymentAmount }
                    val effect = ordered.sumOf { it.paidEffect }
                    val eventTotal = ordered.sumOf { it.paymentEventCount.coerceAtLeast(1) }
                    val byText = ordered.map { it.by.trim() }.filter { it.isNotBlank() }.distinct().joinToString(" / ")
                    val human = ordered.map { it.typedRemark.trim() }.filter { it.isNotBlank() }.distinct()
                    val breakdown = PaymentModel.paymentBreakdown(cash, online)
                    latest.copy(
                        by = byText.ifBlank { latest.by },
                        note = if (human.isNotEmpty()) "${human.joinToString(" | ")} — $breakdown" else breakdown,
                        paymentAmount = total,
                        paymentMode = PaymentModel.splitMode(cash, online),
                        paymentCashAmount = cash,
                        paymentOnlineAmount = online,
                        paymentEventCount = eventTotal,
                        paidEffect = effect,
                        typedRemark = human.joinToString(" | ")
                    )
                }
            }
            (passthrough + mergedTreatment).sortedBy { orderKey(it) }
        } else displayEntries.sortedBy { orderKey(it) }

        /* 🟢🔒 V1090 ধাপ ② — উপরের টীকা দ্রষ্টব্য। এখানেই চালানো হয়, কারণ
           একই দিনের একাধিক Treatment পেমেন্ট জোড়া লাগার সময় লেখাটা নতুন
           করে বানানো হয় (`note = human — breakdown`) — আগে তুলে দিলে সেটা
           আবার ফিরে আসত। ⛔ History ছাড়া কোনো কলার প্রভাবিত হয় না। */
        /* 🔴🔒 V1097 (০৫.০৯.২০২৬ — নিজে খুঁটিয়ে যাচাই করতে গিয়ে ধরা):
           নিচে (`filtered`) সেকশন বাছা থাকলে ফলো-আপের সারিগুলো বাদ পড়ে যায়।
           তখন টাকার সারি থেকে লেখাটা তুলে দিলে **কথাটা কোথাও আর দেখাই যেত না**।
           ⇒ সেকশন বাছা থাকলে কিছুই তোলা হয় না — হুবহু আগের আচরণ। */
        val historyDisplayFinal =
            if (separateRowsPerEvent && section.isNullOrBlank())
                wlv1StripEchoedTreatmentNote(historyDisplayEntries)
            else historyDisplayEntries

        val chronologicalRaw = historyDisplayFinal

        // TK-REQUESTED CHANGE (2026-07-19): one row per DAY, not one row per
        // event -- if an enquiry call, a follow-up remark, and a payment all
        // happened the same day, that used to show as 3 separate rows. Now
        // they merge into a single day-row. A payment's own staff-written
        // remark (Progress) always wins over a non-payment note for that
        // same day, since TK's explicit priority is "what happened with the
        // patient, especially around payment". Paid amounts for the day are
        // summed for the running total; the day's most recent payment id is
        // kept so 3-tap-edit still opens something real for that day.
        val byDay = LinkedHashMap<String, MutableList<TimelineEntry>>()
        for (e in chronologicalRaw) {
            val dayKey = orderKey(e).take(10).ifBlank { "unknown" }   // 🟢 B679: ঘটনার দিন ধরে ভাগ
            byDay.getOrPut(dayKey) { mutableListOf() }.add(e)
        }
        // TK-REPORTED BUG FIX (2026-07-25): when TWO OR MORE real, distinct
        // payments landed on the same calendar day, this used to collapse
        // them into ONE row -- both amounts were still summed correctly
        // into the running Paid/Due totals below, but only the LATEST
        // payment's own label/note survived on screen, so an earlier
        // same-day payment (its own real transaction, its own audit
        // trail) silently vanished from Full Journey/Report Card -- exactly
        // what TK's photo-proof showed ("3rd Payment" row with no real
        // detail, "2nd Payment" nowhere to be found, even though Paid
        // already correctly included its amount). Every distinct real
        // payment now always keeps its own row, no matter what else
        // happened that same day -- only a NON-payment same-day note (a
        // call, a plain remark) still folds into the day's last payment
        // row, exactly as before.
        val chronologicalMerged = byDay.values.flatMap { dayEntries ->
            // 🚨🚨 TK-REPORTED, LIVE (30.07.2026, ছবিসহ · TAZIM — একই দিনে
            // Registration Fee ও Advance দুটোই হয়েছিল, অথচ Full Journey/
            // Payments টেবিলে শুধু Advance দেখাচ্ছিল, Fee-র সারিটাই ছিল না)।
            //
            // আসল কারণ (কোড ধরে, আন্দাজ নয়): এই ফিল্টারটা `visit_fee`-কে
            // "আসল পেমেন্ট" গোনা থেকে বাদ দিত, তাই একই দিনে Fee + Advance
            // দুটো থাকলেও `paymentEntries.size` হত মাত্র ১ (শুধু Advance) —
            // আর "সাইজ ≤ ১" মানেই উপরের শাখা একটাই সারিতে সব মিশিয়ে দিত,
            // আর সেই মেশানো সারিতে Fee-র কোনো চিহ্নই থাকত না। Kalam Khan/
            // MD Akhtar Ali-এর ক্ষেত্রে সেদিন শুধু Fee-ই ছিল, তাই ঠিক
            // লাগছিল — TAZIM-এর মতো "একই দিনে Fee + আরেকটা আসল পেমেন্ট"
            // থাকলেই Fee-টা হারাত।
            //
            // 🔒 TK-ORDER (30.07.2026 রাত): "Report Card-এ হাত দেবেন না,
            // সেখানে Fees Amount দেখানোর দরকার নেই।" Report Card ও এই
            // ফাংশনই (`build()`) ব্যবহার করে (`ReportCardActivity` ·
            // `ReportCardPrinter`), তাই সমাধানটা **শুধু Patient Timeline-এর
            // Full Journey/Payments টেবিলে** সীমাবদ্ধ রাখা হলো — নতুন
            // প্যারামিটার `keepVisitFeeAsOwnRow` (ডিফল্ট `false` — Report
            // Card ও Chamber-এর কলার আগের মতোই আচরণ পাবে, এক অক্ষরও বদলায়নি)।
            // Patient Timeline-এর নিজের কল-সাইটেই শুধু `true` পাঠানো হয়।
            val paymentEntries = dayEntries.filter {
                it.paymentId != null && it.payType != "attendance_mark" &&
                    (keepVisitFeeAsOwnRow || it.payType != "visit_fee")
            }
            if (paymentEntries.size <= 1) {
                // Unchanged behaviour: at most one real payment this day.
                val bestRemark = dayEntries.filter { it.paymentId != null && it.note.isNotBlank() }
                    .maxByOrNull { orderKey(it) }
                    ?: dayEntries.maxByOrNull { orderKey(it) }
                    ?: dayEntries.first()
                // 🔴🔴🔴 খাতার সারি B451 (TK-রিপোর্ট, ছবিসহ — Doctor
                // Check-up Queue থেকে Full Journey খুলে রোগীর Registration-
                // এর সময় স্টাফের লেখা Complaint/History দেখা যাচ্ছিল না,
                // বদলে শুধু "Marked Arrived — Attendance confirmed"
                // দেখাচ্ছিল)। **আসল কারণ:** যেদিন কোনো আসল পেমেন্ট নেই
                // (paymentEntries খালি/১টা), সেদিনের একাধিক ঘটনা (যেমন
                // "Registration/Visit" + "Marked Arrived") থাকলে আগে শুধু
                // **সবচেয়ে দেরিতে হওয়া** ঘটনার নোটটাই বাঁচত (bestRemark) —
                // তার আগের ঘটনার নোট (যেমন রোগীর Complaint/History)
                // সম্পূর্ণ হারিয়ে যেত পর্দা থেকে (ডেটাবেসে ঠিকই থাকত, শুধু
                // এই তালিকায় দেখাত না)। **সমাধান:** এখন সেই দিনের সবকটা
                // আলাদা, অ-ফাঁকা নোট সময়-ক্রমে জোড়া লাগানো হয় (` | ` দিয়ে,
                // একাধিক পেমেন্টের দিনের জন্য নিচেই যে একই পদ্ধতি আগে থেকে
                // ছিল, ঠিক সেটাই) — কোনো নোটই আর হারায় না।
                val combinedNote = dayEntries
                    .filter { it.note.isNotBlank() }
                    .sortedBy { orderKey(it) }
                    .map { it.note }
                    .distinct()
                    .joinToString(" | ")
                val latestPayment = dayEntries.filter { it.paymentId != null }.maxByOrNull { orderKey(it) }
                val dayPaidSum = paymentEntries.sumOf { it.paymentAmount }
                // 🔒 V217 (§B216): দেখানোর `paymentAmount`/`dayPaidSum` অক্ষত (এক
                // দিনে সর্বোচ্চ একটাই real payment এই branch-এ, তাই আগের মতোই
                // আসল ঘর — 3-tap Edit বাক্সে সবসময় positive দেখাবে)। হিসাবের
                // জন্য আলাদা signed যোগফল, refund-এর সাইন সহ।
                val dayPaidEffect = paymentEntries.sumOf { it.paidEffect }
                listOf(
                    bestRemark.copy(
                        note = combinedNote.ifBlank { bestRemark.note },
                        paymentId = latestPayment?.paymentId,
                        paymentBranch = latestPayment?.paymentBranch ?: bestRemark.paymentBranch,
                        paymentAmount = dayPaidSum,
                        paymentMode = latestPayment?.paymentMode ?: bestRemark.paymentMode,
                        payType = latestPayment?.payType ?: bestRemark.payType,
                        paidEffect = dayPaidEffect
                    )
                )
            } else {
                // 🔒 উপরের paymentEntries-এর সঙ্গে মেলানো — flag true হলে
                // visit_fee নিজের সারি হিসেবে থাকে (তাই এখানে বাদ), নইলে
                // আগের মতোই এখানে থেকে নোট হিসেবে জুড়ে যায় (Report Card/
                // Chamber-এর কলার, যারা flag পাঠায় না)।
                val nonPayment = dayEntries.filter {
                    it.paymentId == null || it.payType == "attendance_mark" ||
                        (!keepVisitFeeAsOwnRow && it.payType == "visit_fee")
                }
                val extraNote = nonPayment.filter { it.note.isNotBlank() }.maxByOrNull { orderKey(it) }
                paymentEntries.sortedBy { orderKey(it) }.mapIndexed { idx, p ->
                    if (idx == paymentEntries.lastIndex && extraNote != null && extraNote.note != p.note)
                        p.copy(note = if (p.note.isBlank()) extraNote.note else "${p.note} | ${extraNote.note}")
                    else p
                }
            }
        }.sortedBy { orderKey(it) }

        // 🔒 V452 supersedes old B607 only for Treatment money: History-তে
        // non-payment/Visit Fee/Refund ঘটনা আলাদা থাকে, কিন্তু একই calendar-day
        // Treatment money এখন এক daily row। Running total একই যোগফল পায়।
        // Report Card/অন্য caller (false) আগের merged পথই পায়।
        val chronological = if (separateRowsPerEvent) chronologicalRaw else chronologicalMerged

        var runningPaid = 0.0
        val numbered = chronological.mapIndexed { idx, e ->
            // 🔒 V217 (§B216): paidEffect ব্যবহার — approved refund এখন সত্যিই
            // বিয়োগ হয়, pending/rejected কিছুই যোগ করে না (আগে সবসময় যোগ হত)।
            if (e.paymentId != null && e.payType != "visit_fee" && e.payType != "attendance_mark") runningPaid += e.paidEffect
            val due = if (billTotal > 0.0) (billTotal - runningPaid).coerceAtLeast(0.0) else -1.0
            e.copy(visitNo = idx + 1, runningPaid = runningPaid, runningDue = due)
        }

        // Newest first
        val newestFirst = numbered.sortedByDescending { it.visitNo }

        val filtered = if (section.isNullOrBlank()) newestFirst else newestFirst.filter { matchesSection(it.title, section) }

        return TimelineData(
            // 🔴🔴🔴 TK-REPORTED (31.07.2026): "Patient Name-এর জায়গায় Mobile
            // দুবার দেখানো"। আসল কারণ এইখানে — নাম সত্যিই না পাওয়া গেলে শেষে
            // mobileDigits বসিয়ে দেওয়া হত, তাই PatientTimelineActivity.kt-এ
            // "UNKNOWN" ফিক্স বসালেও কখনো কাজ করত না (name কখনো
            // ফাঁকা পেত না)। Enquiry-তে নাম খোঁজার নিয়ম অক্ষত রাখা হলো (ওটা
            // সঠিক ফলব্যাক) — শুধু একদম শেষের mobile-ফলব্যাক বাদ দেওয়া হলো।
            //
            // 🟢🔒🔒 V636 (২৪.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ — "Follow-up কার্ডে
            // OSMAN ORAW নাম দেখাচ্ছে, View করলে UNKNOWN কেন?") — আসল কারণ:
            // এই নাম-খোঁজার নিয়ম শুধু `patient` (patients টেবিল) ও
            // `enquiries[0]` দেখত — `followups[0]`-এ কখনো দেখতই না। ঠিক এই
            // একই সমস্যা branch/disease/address-এ আগে (V235) ধরা পড়েছিল ও
            // followups[0]-এ fallback যোগ করে ঠিক হয়েছিল (নিচের/উপরের লাইন
            // দেখুন) — কিন্তু নাম-এর জন্য তখন করা হয়নি। যে রোগীর শুধু
            // `followups` সারিই টিকে আছে (patients/enquiries সারি নেই — যেমন
            // OSMAN ORAW), তার নাম followups টেবিলে থাকা সত্ত্বেও এখানে
            // ফাঁকা থেকে যেত, তাই শেষে "UNKNOWN"। এখন branch/disease-এর
            // প্রমাণিত একই প্যাটার্নে followups[0].name-ও যোগ হলো।
            // ⛔ কোনো অনুমান/ভুয়া নাম নয় — সব ফাঁকা হলে আগের মতোই ""
            //    (তখনই "UNKNOWN")।
            name = patient.s("name").ifBlank {
                (if (enquiries.length() > 0) enquiries.getJSONObject(0).s("name") else "").ifBlank {
                    if (followups.length() > 0) followups.getJSONObject(0).s("name") else ""
                }
            },
            patientId = patient.s("patientId"),
            mobile = mobileDigits,
            // 🔒 V235: Alternate/Enquiry নম্বর (থাকলে) — শেষ ১০ সংখ্যা, Primary-র মতোই।
            altMobile = patient.s("altMobile").filter { it.isDigit() }.takeLast(10),
            branch = patient.s("branch").ifBlank {
                if (followups.length() > 0) followups.getJSONObject(0).s("branch") else ""
            },
            disease = patient.s("disease").ifBlank {
                if (followups.length() > 0) followups.getJSONObject(0).s("disease") else ""
            },
            // 🔴🔒 V794 — ছবিটা জমা থাকলে ফোন থেকেই, নইলে একবারই ক্লাউড থেকে
            photo = patient.s("photo").ifBlank {
                com.tkbiswas.pilesclinic.native.PatientPhotoCache.photoFor(
                    context, patient.s("id"), patient.s("updatedAt"))
            },
            entries = filtered,
            billTotal = billTotal,
            billBeforeDiscount = billBeforeDiscount,
            discount = patient.optDouble("discount", 0.0),
            rowId = uuid,
            // TK-REQUESTED (2026-07-18): refDoctor was a name SNAPSHOT saved
            // at the time it was set -- if that doctor's name is later
            // corrected in RMP, old patients kept showing the outdated
            // name. Now looked up live by refDoctorMobile each time;
            // falls back to the stored snapshot if no live match (e.g. the
            // RMP record was removed), so this never goes blank.
            // TK-REQUESTED (2026-07-24): "By- Dr. NAME (AREA)" format --
            // area is the referring RMP's own location tag (same field RMP
            // cards show as "📍 AREA"), looked up live alongside the name
            // (same reasoning as the 2026-07-18 fix above: never a stale
            // snapshot). refDoctorDisplay is blank whenever there's no
            // referring doctor at all, so the header line hides cleanly.
            // TK-REPORTED (2026-09-16): an Enquiry-only person (not yet
            // Registered) has no `patients` row, so `patient` above is an
            // empty JSONObject() -- the RMP name entered on the Enquiry form
            // itself (saved on the `enquiries` row, per EnquiryModel) was
            // never read here, so "Ref by" never showed on the Enquiry
            // header even though the data was saved all along. Now falls
            // back to enquiries[0]'s own refDoctor/refDoctorMobile whenever
            // the patient row's own fields are blank -- same fallback idiom
            // already used for address/timeType/name above in this file.
            refDoctor = run {
                val refSourceDoctor = patient.s("refDoctor").ifBlank {
                    if (enquiries.length() > 0) enquiries.getJSONObject(0).s("refDoctor") else ""
                }
                val refSourceMobile = patient.s("refDoctorMobile").ifBlank {
                    if (enquiries.length() > 0) enquiries.getJSONObject(0).s("refDoctorMobile") else ""
                }
                val savedMobile = refSourceMobile.filter { it.isDigit() }.takeLast(10)
                if (savedMobile.length == 10) {
                    val liveDoc = SupabaseClient.findByMobile("doctor_visits", savedMobile, "name", 1)
                    if (liveDoc.length() > 0) liveDoc.getJSONObject(0).s("name").ifBlank { refSourceDoctor }
                    else refSourceDoctor
                } else refSourceDoctor
            },
            refDoctorDisplay = run {
                val refSourceDoctor = patient.s("refDoctor").ifBlank {
                    if (enquiries.length() > 0) enquiries.getJSONObject(0).s("refDoctor") else ""
                }
                val refSourceMobile = patient.s("refDoctorMobile").ifBlank {
                    if (enquiries.length() > 0) enquiries.getJSONObject(0).s("refDoctorMobile") else ""
                }
                val name = run {
                    val savedMobile = refSourceMobile.filter { it.isDigit() }.takeLast(10)
                    if (savedMobile.length == 10) {
                        val liveDoc = SupabaseClient.findByMobile("doctor_visits", savedMobile, "name,area", 1)
                        if (liveDoc.length() > 0) liveDoc.getJSONObject(0) else null
                    } else null
                }
                if (name == null) {
                    val plain = refSourceDoctor
                    if (plain.isBlank()) "" else if (plain.startsWith("Dr.", ignoreCase = true)) plain else "Dr. $plain"
                } else {
                    val docName = name.s("name").ifBlank { refSourceDoctor }
                    val area = name.s("area")
                    val withTitle = if (docName.startsWith("Dr.", ignoreCase = true)) docName else "Dr. $docName"
                    if (area.isNotBlank()) "$withTitle ($area)" else withTitle
                }
            },
            refDoctorMobile = patient.s("refDoctorMobile").ifBlank {
                if (enquiries.length() > 0) enquiries.getJSONObject(0).s("refDoctorMobile") else ""
            },
            followupId = bestFollowup?.s("id") ?: "",
            followupStage = effectiveFollowupStage,
            // 🔒 খাতার সারি B97: ওই সারিটার চলতি অবস্থা (Active / Cancelled /
            // Incomplete) — এটা আগেই আনা তালিকার ভিতরেই আছে, **বাড়তি কোনো
            // ক্লাউড-কল নেই**।
            followupStatus = bestFollowup?.s("status") ?: "",
            enquiryId = if (enquiries.length() > 0) enquiries.getJSONObject(0).s("id") else "",
            registrationDate = patient.s("registrationDate").ifBlank { patient.s("date") },
            registrationCreatedAt = patient.s("createdAt"),
            registeredByMobile = patient.s("registeredBy").ifBlank { patient.s("createdBy") },
            enquiryDate = if (enquiries.length() > 0) enquiries.getJSONObject(0).s("date") else "",
            enquiryReceivedBy = if (enquiries.length() > 0) enquiries.getJSONObject(0).s("receivedBy").ifBlank { enquiries.getJSONObject(0).s("createdBy") } else "",
            age = patient.s("age"),
            sex = patient.s("sex"),
            // 🔒 V235 (TK verified 01.08.2026): Address এতদিন patients → enquiries[0]
            // পর্যন্ত fallback করত, কিন্তু **followups[0]-এ করত না** — যদিও Branch/
            // Disease করে (উপরে line 807-812)। ফলে যে পুরোনো record শুধু followups
            // row হিসেবে টিকে আছে (enquiry/patient row নেই) তার address DB-তে
            // থাকা সত্ত্বেও View-তে দেখাত না। এখন followups[0].address-এও fallback।
            // ⛔ কোনো অনুমান/ভুয়া address নয় — সব ফাঁকা হলে আগের মতোই "" (View hide)।
            address = patient.s("address").ifBlank {
                (if (enquiries.length() > 0) enquiries.getJSONObject(0).s("address") else "").ifBlank {
                    if (followups.length() > 0) followups.getJSONObject(0).s("address") else ""
                }
            },
            completeRequestedBy = patient.s("completeRequestedBy"),
            completeApprovedBy = patient.s("completeApprovedBy"),
            // 🔵🔒 V521: রোগীর সারিতে না থাকলে (পুরোনো রেকর্ড) এনকোয়ারির সারি
            // থেকে — Extra Income-এর নিয়ম রোগীর সারিই দেখে, তাই সেটাই আগে।
            timeType = patient.s("timeType").ifBlank {
                if (enquiries.length() > 0) enquiries.getJSONObject(0).s("timeType") else ""
            }
        )
    }
}
