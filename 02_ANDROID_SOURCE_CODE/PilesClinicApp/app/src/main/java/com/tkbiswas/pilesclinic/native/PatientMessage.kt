package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.tkbiswas.pilesclinic.print.BranchCatalog

/**
 * 🔒 TK-APPROVED (28.07.2026, প্রুফ ৯–১৪) — রোগীর ফোনে খবর পাঠানো।
 *
 * নিয়ম (TK-এর নির্দেশ):
 *  · প্রতিটা বার্তা **তিন ভাষায়** — বাংলা · হিন্দি · English।
 *  · **কিশনগঞ্জ ব্রাঞ্চে হিন্দি আগে**, বাকি সব ব্রাঞ্চে **বাংলা আগে**। ক্রম রোগীর
 *    ব্রাঞ্চ দেখে নিজে থেকেই বসে — স্টাফকে কিছু বাছতে হয় না।
 *  · ক্লিনিকের নাম · ঠিকানা · ফোন **কখনো হাতে লেখা হয় না** — BranchCatalog
 *    থেকেই আসে (প্রিন্টে যেখান থেকে আসে, ঠিক সেখান থেকেই)। তাই কিশনগঞ্জে
 *    আপনা থেকেই "TK BISWAS PILES CLINIC", বাকি ব্রাঞ্চে "MAA AYURVED PILES CLINIC"।
 *  · **রোগের নাম কোনো বার্তায় থাকে না** — রোগীর ফোন অন্য কেউ দেখলেও যেন
 *    গোপনীয়তা নষ্ট না হয়। শুধু নাম, Patient ID ও টাকার হিসাব।
 *  · খরচ শূন্য: ফোনের নিজের Message অ্যাপ বা WhatsApp খোলে, নম্বর ও লেখা বসানো
 *    থাকে, স্টাফ শুধু পাঠান চাপেন। কোনো গেটওয়ে/নিবন্ধন লাগে না।
 *  · "পরে পাঠাব" চাপলে কিছুই যায় না — সেভ হয়ে যাওয়া কাজ কখনো আটকায় না।
 */
object PatientMessage {

    // 🔒 TK-এর নির্দেশ (01.08.2026): "আজকের জমা"-য় তারিখ, বার (দিনের নাম),
    // আর সময় — তিনটেই থাকতে হবে, ভুল করা যাবে না। ISO টাইমস্ট্যাম্প থেকে এই
    // তিনটে বার করার জন্য এক জায়গায় হেল্পার ফাংশন — প্রজেক্টের অন্য জায়গায়
    // (PatientTimelineActivity.displayDate/displayTime) যে একই ধাঁচ ব্যবহার
    // হয় তার হুবহু নকল, শুধু এখানে বার (day name)-ও যোগ হলো তিন ভাষায়।
    private val dayNamesBn = arrayOf("রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার")
    private val dayNamesHi = arrayOf("रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार")

    private fun parseIso(iso: String): java.util.Date? = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).parse(iso)
    } catch (_: Exception) { null }

    /** GLOBAL RULE (dot date, never slash) — same ধাঁচ প্রজেক্টের বাকি সব জায়গায়। */
    private fun dotDate(d: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(d)
    }

    /* ═══════════════════════════════════════════════════════════════════
       🔴🔒 V505 — TK-এর নির্দেশ (২১.০৮.২০২৬), হুবহু তাঁর উদাহরণে:
           DATE: 31/12/2026 TIME: 12.34Pm
       TK: *"শুধু রেজিস্ট্রেশন বলে কথা নয়, যে কোন বার্তায় যেখানে তারিখ
            থাকবে তার পাশে সময় থাকবে।"*

       ⚠️ সৎ কথা: প্রকল্পের বাকি সব জায়গায় (রসিদ · ছাপা · রিপোর্ট) নিয়ম হলো
          **বিন্দু-তারিখ** (31.12.2026) ও **বড় হাতের PM**। TK-কে দুটো নমুনাই
          ফটো-প্রুফে দেখানো হয়েছে; তিনি **তাঁর নিজের ধরনটাই** বেছেছেন।
          তাই শুধু **রোগীর বার্তায়** এই ধরন — অ্যাপের বাকি কিছুই বদলানো হয়নি।
       ═══════════════════════════════════════════════════════════════════ */

    /** `31.12.2026` — 🔴🔒 V936 (TK-নির্দেশ ৩১.০৮.২০২৬: *"সম্পূর্ণ প্রজেক্টে
     *  তারিখ একই ফরমেটে থাকতে হবে"*)। আগে স্ল্যাশ ছিল। */
    private fun tkDate(d: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(d)
    }

    /** `12.34Pm` — TK-এর উদাহরণ হুবহু (প্রথম অক্ষর বড়, পরেরটা ছোট, ফাঁক নেই)। */
    private fun tkTime(d: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US)   // 🔴 V1158
        sdf.timeZone = java.util.TimeZone.getDefault()
        val t = sdf.format(d)          // যেমন "12.34PM"
        return if (t.length >= 2)
            t.dropLast(2) + t.takeLast(2).first().uppercase() + t.takeLast(1).lowercase()
        else t
    }

    /**
     * বার্তার তারিখ-লাইন। সময় জানা থাকলে পাশে বসে, না জানলে **শুধু তারিখ**
     * (ভবিষ্যতের ভিজিট-তারিখে সময় বলে কিছু নেই — সেখানে মিথ্যা সময় বসানো হয় না)।
     */
    private fun dateTimeLine(dateText: String, timeText: String): String =
        if (timeText.isBlank()) "DATE: " + dateText
        else "DATE: " + dateText + " TIME: " + timeText

    /** GLOBAL RULE (h.mm AM/PM, বড় হাতের অক্ষরে) — খাতার সারি B76। */
    private fun clockTime(d: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(d)
    }

    private fun dayName(d: java.util.Date, lang: String): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeZone = java.util.TimeZone.getDefault()
        cal.time = d
        val idx = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1  // Calendar.SUNDAY == 1
        return when (lang) {
            "bn" -> dayNamesBn.getOrElse(idx) { "" }
            "hi" -> dayNamesHi.getOrElse(idx) { "" }
            else -> java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(d)
        }
    }

    // 🔒🔒 TK-এর চূড়ান্ত নির্দেশ (02.08.2026 রাত ~১২.২৫–১২.৫৫ am): বাংলা বার্তার
    // নিচে ক্লিনিকের নাম ও ঠিকানা **বাংলাতেই** — ইংরেজি BranchCatalog থেকে
    // সরাসরি নয়। ✅ **TK নিজে পাঁচটা ব্রাঞ্চের জন্যই এখন হুবহু লিখে
    // দিয়েছেন ও বলেছেন "এগুলোই হবে"** — নিচের পাঁচটাই তাঁর নিজের লেখা,
    // অক্ষরে অক্ষরে বসানো (Falakata/Birpara-এ তাঁর নিজের দেওয়া লাইন-ব্রেক ও
    // বাংলা দাঁড়ি "।" সহ)। ⛔ TK-এর অনুমতি ছাড়া এক অক্ষরও বদলানো যাবে না।
    private data class BnBranchText(val clinicNameBn: String, val addressBn: String)
    private val bnBranchText: Map<String, BnBranchText> = mapOf(
        "kishanganj" to BnBranchText("বিশ্বাস পাইলস ক্লিনিক", "কিষানগঞ্জ, ক্যালটেক্স চক, মোদি গোলা"),
        "jalpaiguri" to BnBranchText("মা আয়ুর্বেদ পাইলস ক্লিনিক", "জলপাইগুড়ি, রায়কতপাড়া, স্পোর্টস কমপ্লেক্সের সামনে-"),
        "cooch_behar" to BnBranchText("মা আয়ুর্বেদ পাইলস ক্লিনিক", "কোচবিহার, মিনি বাস স্ট্যান্ডের বিপরীতে, সেনগুপ্ত কমপ্লেক্স"),
        "falakata" to BnBranchText("মা আয়ুর্বেদ পাইলস ক্লিনিক", "ফালাকাটা, বিডিও অফিস রোড,\n হোটেল নন্দনিকের কাছে।"),
        "birpara" to BnBranchText("মা আয়ুর্বেদ পাইলস ক্লিনিক", "বীরপাড়া, এমজি রোড, \nঅ্যাক্সিস ব্যাংকের কাছে।")
    )

    enum class Kind {
        REGISTRATION, ADVANCE, BILL, PAYMENT, ENQUIRY, VISIT_DATE,
        // TK-FINAL (28.07.2026, প্রুফ ১৪): বাকি পাঁচটা বার্তা। পুরনো ছ'টার
        // একটা অক্ষরও বদলানো হয়নি — এগুলো শুধু নতুন করে যোগ হলো।
        DUE_REMINDER, RECEIPT, VISIT_REMINDER, DOCUMENT, TREATMENT_DONE
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔒🔒 ENQUIRY_WHATSAPP_MESSAGE_FINAL_LOCK_2026-07-31_0950_IST
    // TK BISWAS-এর FINAL OWNER LOCK নোট অনুযায়ী — শুধু Enquiry-এর প্রথম
    // WhatsApp বার্তার জন্য। এই লেখা/ফরম্যাট/লিংক-অর্ডার TK-এর স্পষ্ট
    // অনুমতি ছাড়া কোনোদিন বদলানো যাবে না। পুরনো তিন-ভাষা-একসাথে
    // ENQUIRY বার্তা (`block()`/`build()`/`buildWhatsApp()`) অক্ষত আছে —
    // ওগুলো অন্য কোনো ডাক এখনও ব্যবহার করলে আগের মতোই কাজ করবে।
    // ══════════════════════════════════════════════════════════════════════
    private data class EnquiryBranchExtra(
        val bnName: String, val hiName: String,
        val bnDays: String, val hiDays: String, val enDays: String,
        val mapLink: String, val facebookLink: String
    )

    // ⚠️ ধরে নেওয়া হয়েছে (TK-কে জানানো হলো, ফাইনাল ছবি-প্রুফে দেখানো হবে):
    // হিন্দি ব্রাঞ্চ-নাম সাধারণ প্রচলিত বানানে লেখা হলো (কোনো সরকারি নথি
    // থেকে যাচাই করা হয়নি) — TK ভুল মনে করলে বদলে দেওয়া হবে।
    private val enquiryBranchExtra: Map<String, EnquiryBranchExtra> = mapOf(
        "kishanganj" to EnquiryBranchExtra(
            bnName = "কিষানগঞ্জ", hiName = "किशनगंज",
            bnDays = "বুধবার থেকে শনিবার", hiDays = "बुधवार से शनिवार", enDays = "Wednesday to Saturday",
            mapLink = "https://maps.app.goo.gl/3WoQv658CdzMhtRA6",
            facebookLink = "https://www.facebook.com/share/1CuUNwf48e/"
        ),
        "jalpaiguri" to EnquiryBranchExtra(
            bnName = "জলপাইগুড়ি", hiName = "जलपाईगुड़ी",
            bnDays = "মঙ্গলবার ও শনিবার", hiDays = "मंगलवार एवं शनिवार", enDays = "Tuesday and Saturday",
            mapLink = "https://maps.app.goo.gl/mWQPDUJfepYnXhgy8",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        ),
        "cooch_behar" to EnquiryBranchExtra(
            bnName = "কোচবিহার", hiName = "कूचबिहार",
            bnDays = "সোমবার ও শুক্রবার", hiDays = "सोमवार एवं शुक्रवार", enDays = "Monday and Friday",
            mapLink = "https://maps.app.goo.gl/mnVFJJ436Rwx1Pff6",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        ),
        "falakata" to EnquiryBranchExtra(
            bnName = "ফালাকাটা", hiName = "फालाकाटा",
            bnDays = "মঙ্গলবার ও বৃহস্পতিবার", hiDays = "मंगलवार एवं गुरुवार", enDays = "Tuesday and Thursday",
            mapLink = "https://maps.app.goo.gl/FdwYxUukwK9kTMUcA",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        ),
        "birpara" to EnquiryBranchExtra(
            bnName = "বীরপাড়া", hiName = "बीरपाड़ा",
            bnDays = "রবিবার ও বুধবার", hiDays = "रविवार एवं बुधवार", enDays = "Sunday and Wednesday",
            mapLink = "https://maps.app.goo.gl/euEW22kdnE21Fove6",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        )
    )

    // ⚠️ ধরে নেওয়া হয়েছে: disease "Other" বা তালিকার বাইরে হলে ওয়েবসাইটের
    // মূল পাতার লিংক যাবে (কোনো নির্দিষ্ট রোগ-পাতার URL আন্দাজ করা হয়নি)।
    private fun diseaseEduLink(disease: String): String {
        val base = "https://maaayurvedpilesclinic.netlify.app"
        return when (disease.trim().lowercase()) {
            "piles" -> "$base/?disease=piles"
            "fissure" -> "$base/?disease=fissure"
            "fistula" -> "$base/?disease=fistula"
            "hydrocele" -> "$base/?disease=hydrocele"
            "gupt rog" -> "$base/?disease=gupt-rog"
            else -> base
        }
    }

    private fun money(v: Double): String = "Rs " + "%,.0f".format(v)

    private fun tenDigits(mobile: String): String =
        mobile.filter { it.isDigit() }.takeLast(10)

    /**
     * 🔒 B526 (07.08.2026, TK-এর লাইভ রিপোর্ট): রোগীর WhatsApp/SMS Bill-এ
     * টাকা নেওয়া Staff-এর personal mobile কখনো প্রকাশ করা যাবে না। DB/audit-এ
     * আগের `receivedBy` যেমন আছে তেমনই থাকে; শুধু রোগীর জন্য বানানো বার্তায়
     * configured Staff Code/Name দেখানো হয়। অচেনা 10-digit number হলে নিরাপত্তার
     * জন্য সেটি পুরোপুরি লুকানো হয় — কোনো personal number leak নয়।
     */
    private fun patientSafeReceiverCode(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return ""
        val digits = value.filter { it.isDigit() }
        val normalized = StaffDirectory.normalizeMobile(value)
        if (digits.length >= 10 && normalized.length == 10) {
            return StaffDirectory.findAccount(value)?.name.orEmpty()
        }
        return value
    }

    /** এক ভাষার অংশটুকু। bn / hi / en — তিনটেই একই তথ্য বলে। */
    /** 🔒 খাতার সারি B92 (TK, 29.07.2026 বিকেল ৫.১০ — TK নমুনা দেখে
     *  "যা লিখেছেন ঠিকই আছে" বলেছেন):
     *   • রেজিস্ট্রেশনের বার্তায় **রেজিস্ট্রেশন ফি** কত ও কীভাবে দিলেন তা থাকবে
     *   • বিলের বার্তায় **মোট খরচ · জমা · বাকি** তিনটেই আলাদা লাইনে
     *   • পেমেন্টের বার্তায় **কততম পেমেন্ট** তা লেখা থাকবে (২য় · ৩য় · ৪র্থ …)
     *  ⛔ রোগের নাম কোনো বার্তায় যায় না — TK-এর পুরনো গোপনীয়তার নিয়ম বহাল।
     *  ⛔ নতুন ঘরগুলোর ডিফল্ট ফাঁকা/০, তাই **পুরনো কোনো ডাক ভাঙে না** — যেখানে
     *     তথ্য দেওয়া হয়নি সেখানে লাইনটাই ওঠে না, বার্তা আগের মতোই থাকে। */
    private fun ordinalWord(lang: String, payLabel: String): String {
        val n = Regex("\\d+").find(payLabel)?.value?.toIntOrNull() ?: return ""
        return when (lang) {
            "bn" -> when (n) {
                2 -> "2য়"; 3 -> "3য়"; 4 -> "4র্থ"; 5 -> "5ম"; 6 -> "6ষ্ঠ"
                7 -> "7ম"; 8 -> "8ম"; 9 -> "9ম"; 10 -> "10ম"; else -> "$n তম"
            }
            "hi" -> when (n) {
                2 -> "दूसरा"; 3 -> "तीसरा"; 4 -> "चौथा"; 5 -> "पाँचवाँ"; 6 -> "छठा"
                7 -> "सातवाँ"; 8 -> "आठवाँ"; 9 -> "नौवाँ"; 10 -> "दसवाँ"; else -> "$n वाँ"
            }
            else -> payLabel.trim()
        }
    }

    private fun block(
        lang: String, clinic: String, place: String, name: String, patientId: String,
        kind: Kind, amount: Double, mode: String, bill: Double, paid: Double, dateText: String,
        /** 🔴 V505 — তারিখের পাশের সময় (ফাঁকা = সময় জানা নেই, তখন শুধু তারিখ)। */
        timeText: String = "",
        regFee: Double = 0.0, regFeeMode: String = "", payLabel: String = "",
        // TK-APPROVED ADDITION (31.07.2026): optional, defaults to "" so
        // every existing call (SMS/WhatsApp for all other Kinds) is
        // untouched. Only Kind.RECEIPT reads it.
        receiptNumber: String = "",
        // 🔒 TK-এর নির্দেশ (01.08.2026): "সর্বমোট বিল" শুধু প্রথমবার (বিল
        // তৈরি/প্রথম Advance) দেখাবে, পরের পেমেন্ট বার্তায় নয়। ডিফল্ট true —
        // তাই Kind.BILL-এর পুরনো সব ডাক (যেমন FollowUpActivity.kt-এর বিল
        // তৈরির বার্তা) আগের মতোই সবসময় সর্বমোট দেখায়। শুধু Kind.PAYMENT ও
        // Kind.BILL-এর নতুন "২য়+ পেমেন্ট" পথ এটা false করে ডাকে।
        showBillTotal: Boolean = true,
        // 🔒 TK-এর নির্দেশ (01.08.2026): সম্পূর্ণ বার্তায় মোবাইল নম্বর ও
        // "টাকা কে জমা নিয়েছে" থাকতে হবে — ডিফল্ট "" তাই পুরনো কোনো ডাক
        // ভাঙে না, শুধু Kind.BILL এদুটো পড়ে।
        paidTodayAtIso: String = "", mobile: String = "", receivedBy: String = "",
        // 🔒 B597 (TK-অনুমোদিত, 09.08.2026 — প্রুফ দেখিয়ে): রেজিস্ট্রেশন বার্তায়
        // স্টাফের ভরা সম্পূর্ণ বিবরণ (বয়স/লিঙ্গ, রোগ, মোবাইল, ঠিকানা)। পেশা নয়।
        // ⛔ ডিফল্ট "" — পুরনো কোনো ডাক ভাঙে না; শুধু Kind.REGISTRATION পড়ে।
        ageSex: String = "", disease: String = "", address: String = ""
    ): String {
        val who = name.ifBlank { "Patient" }
        val due = kotlin.math.max(0.0, bill - paid)
        val receivedByCode = patientSafeReceiverCode(receivedBy)
        val sb = StringBuilder()
        when (lang) {
            "bn" -> {
                when (kind) {
                    Kind.REGISTRATION -> {
                        sb.append("REGISTRATION CONFIRMED\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার রেজিস্ট্রেশন সফলভাবে সম্পন্ন হয়েছে।\n\n")
                        sb.append("Patient ID: ").append(patientId).append("\n")
                        if (ageSex.isNotBlank()) sb.append("Age / Sex: ").append(ageSex).append("\n")
                        if (disease.isNotBlank()) sb.append("Disease: ").append(disease).append("\n")
                        if (mobile.isNotBlank()) sb.append("Mobile: ").append(mobile).append("\n")
                        if (address.isNotBlank()) sb.append("Address: ").append(address).append("\n")
                        if (regFee > 0.0) {
                            sb.append("Registration Fee: ").append(money(regFee)).append("\n")
                            if (regFeeMode.isNotBlank()) sb.append("Payment Mode: ").append(regFeeMode).append("\n")
                        }
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nভবিষ্যৎ যোগাযোগ ও পরবর্তী ভিজিটের জন্য Patient ID-টি সংরক্ষণ করুন।")
                    }
                    Kind.ADVANCE -> {
                        sb.append("ADVANCE PAYMENT RECEIVED\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার অ্যাডভান্স পেমেন্ট সফলভাবে গ্রহণ করা হয়েছে।\n\n")
                        if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                        sb.append("Advance Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nএই পেমেন্টটি আপনার চিকিৎসার হিসাবের সঙ্গে সংযুক্ত করা হয়েছে।")
                    }
                    Kind.BILL -> {
                        // 🔒🔒 TK-এর চূড়ান্ত লক (02.08.2026 রাত ~১২.২৫ am) — TK নিজে
                        // পুরো বাংলা বার্তাটা হুবহু লিখে পাঠিয়েছেন ও বলেছেন
                        // *"এটাই ফাইনাল হবে... লাইভ টেস্টে যেন পরিবর্তন না হয়।"*
                        // ⛔ এই bn ব্লক তাঁর অনুমতি ছাড়া এক অক্ষরও বদলানো যাবে না।
                        sb.append("প্রিয়\n").append(who).append(",\n")
                        // ID-র কোনো লেবেল নেই (TK-এর নমুনায় সরাসরি আইডিটাই) —
                        // ⛔ আন্দাজে লেবেল বসানো হয়নি।
                        if (patientId.isNotBlank()) sb.append(patientId).append("\n")
                        if (mobile.isNotBlank()) sb.append("মোবাইল: ").append(mobile).append("\n")
                        sb.append("\n")
                        if (amount > 0.0) {
                            val d = parseIso(paidTodayAtIso)
                            if (d != null) {
                                sb.append("জমার পরিমাণ: ").append(money(amount)).append("\n")
                                sb.append("সম্পূর্ণ হয়েছে\n")
                                sb.append("তারিখ: ").append(dotDate(d)).append(" (").append(dayName(d, "bn")).append(")\n")
                                sb.append("সময়: ").append(clockTime(d)).append("\n\n")
                                if (receivedByCode.isNotBlank()) sb.append("গ্রহণ করেছেন: ").append(receivedByCode).append("\n\n")
                            }
                        }
                        sb.append("আপনার চিকিৎসার আর্থিক বিবরণ —\n")
                        if (showBillTotal) sb.append("সর্বমোট চিকিৎসা খরচ: ").append(money(bill)).append("\n")
                        sb.append("মোট জমা: ").append(money(paid)).append("\n")
                        sb.append("বাকি আছে: ").append(money(due)).append("\n\n")
                        sb.append("এই হিসাব বা পেমেন্ট নিয়ে কোনো প্রশ্ন বা অভিযোগ থাকলে আমাদের সঙ্গে যোগাযোগ করুন।")
                    }
                    Kind.PAYMENT -> {
                        sb.append("PAYMENT RECEIVED\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার পেমেন্ট সফলভাবে গ্রহণ করা হয়েছে।\n\n")
                        sb.append("Payment Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nTotal Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("এই পেমেন্টটি আপনার চিকিৎসার হিসাবের সঙ্গে যুক্ত হয়েছে।")
                    }
                    Kind.ENQUIRY -> {
                        sb.append("প্রিয় ").append(who).append(",\n")
                        sb.append("আমাদের সঙ্গে যোগাযোগ করার জন্য ধন্যবাদ।\n")
                        sb.append("চেম্বার খোলা থাকে সকাল 11টা থেকে বিকেল 4টা।\n")
                        sb.append("যে কোনো প্রয়োজনে ফোন করুন।")
                    }
                    Kind.VISIT_DATE -> {
                        sb.append("NEXT VISIT SCHEDULED\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার পরবর্তী ভিজিটের তারিখ নির্ধারণ করা হয়েছে।\n\n")
                        sb.append("⏰ ").append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("Visiting Time: সকাল 11টা থেকে বিকেল 4টা\n\n")
                        sb.append("নির্ধারিত তারিখে ক্লিনিকে উপস্থিত হবেন।")
                    }
                    Kind.DUE_REMINDER -> {
                        sb.append("PAYMENT DUE REMINDER\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার চিকিৎসার বকেয়া পেমেন্ট মনে করিয়ে দেওয়া হচ্ছে।\n\n")
                        sb.append("Total Treatment Cost: ").append(money(bill)).append("\n")
                        sb.append("Total Paid: ").append(money(paid)).append("\n")
                        sb.append("🔴 Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("সুবিধামতো অথবা পরবর্তী ভিজিটের সময় বকেয়া পরিশোধ করবেন।")
                    }
                    Kind.RECEIPT -> {
                        sb.append("PAYMENT RECEIPT\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার পেমেন্টের রসিদ প্রস্তুত হয়েছে।\n\n")
                        if (amount > 0.0) sb.append("Payment Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        if (receiptNumber.isNotBlank()) sb.append("Receipt Number: ").append(receiptNumber).append("\n")
                        sb.append("\nTotal Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("ভবিষ্যৎ প্রয়োজনে রসিদটি সংরক্ষণ করুন।")
                    }
                    Kind.VISIT_REMINDER -> {
                        sb.append("VISIT REMINDER\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার নির্ধারিত ভিজিটের তারিখ মনে করিয়ে দেওয়া হচ্ছে।\n\n")
                        sb.append("⏰ ").append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("Visiting Time: সকাল 11টা থেকে বিকেল 4টা\n\n")
                        sb.append("নির্ধারিত তারিখে ক্লিনিকে উপস্থিত হবেন।")
                    }
                    Kind.DOCUMENT -> {
                        sb.append("MEDICAL DOCUMENT\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার চিকিৎসাসংক্রান্ত Prescription / Medicine Slip / Diet Chart PDF পাঠানো হলো।\n\n")
                        sb.append("অনুগ্রহ করে Documentটি ডাউনলোড করে সংরক্ষণ করুন এবং চিকিৎসকের নির্দেশ অনুসরণ করুন।")
                    }
                    Kind.TREATMENT_DONE -> {
                        sb.append("TREATMENT COMPLETED\n\n")
                        sb.append("প্রিয় ").append(who).append(",\n\n")
                        sb.append("আপনার নির্ধারিত চিকিৎসা সফলভাবে সম্পন্ন হয়েছে।\n\n")
                        sb.append("চিকিৎসকের পরামর্শ ও নিয়ম মেনে চলবেন। ভবিষ্যতে কোনো সমস্যা হলে আমাদের সঙ্গে যোগাযোগ করবেন।\n\n")
                        sb.append("আপনার সুস্বাস্থ্য কামনা করি।")
                    }
                }
            }
            "hi" -> {
                when (kind) {
                    Kind.REGISTRATION -> {
                        sb.append("REGISTRATION CONFIRMED\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपका रजिस्ट्रेशन सफलतापूर्वक पूरा हो गया है।\n\n")
                        sb.append("Patient ID: ").append(patientId).append("\n")
                        if (ageSex.isNotBlank()) sb.append("Age / Sex: ").append(ageSex).append("\n")
                        if (disease.isNotBlank()) sb.append("Disease: ").append(disease).append("\n")
                        if (mobile.isNotBlank()) sb.append("Mobile: ").append(mobile).append("\n")
                        if (address.isNotBlank()) sb.append("Address: ").append(address).append("\n")
                        if (regFee > 0.0) {
                            sb.append("Registration Fee: ").append(money(regFee)).append("\n")
                            if (regFeeMode.isNotBlank()) sb.append("Payment Mode: ").append(regFeeMode).append("\n")
                        }
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nभविष्य के संपर्क एवं अगली विज़िट के लिए Patient ID सुरक्षित रखें।")
                    }
                    Kind.ADVANCE -> {
                        sb.append("ADVANCE PAYMENT RECEIVED\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपका एडवांस पेमेंट सफलतापूर्वक प्राप्त हो गया है।\n\n")
                        if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                        sb.append("Advance Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nयह पेमेंट आपके इलाज के हिसाब से जोड़ दिया गया है।")
                    }
                    Kind.BILL -> {
                        sb.append("Dear\n").append(who).append(",\n\n")
                        if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                        if (mobile.isNotBlank()) sb.append("Mobile: ").append(mobile).append("\n")
                        sb.append("\n")
                        if (amount > 0.0) {
                            val d = parseIso(paidTodayAtIso)
                            if (d != null) {
                                sb.append("Amount: ").append(money(amount)).append("\n")
                                sb.append("Successfully / सफलतापूर्वक हुआ\n")
                                sb.append("Date: ").append(dotDate(d)).append(" (").append(dayName(d, "hi")).append(")\n")
                                sb.append("Time: ").append(clockTime(d)).append("\n\n")
                                if (receivedByCode.isNotBlank()) sb.append("Received By: ").append(receivedByCode).append("\n\n")
                            }
                        }
                        if (showBillTotal) {
                            sb.append("आपके इलाज का आर्थिक विवरण —\n")
                            sb.append("Total Treatment Cost: ").append(money(bill)).append("\n")
                        }
                        sb.append("Total Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("इस हिसाब / भुगतान के बारे में कोई सवाल या शिकायत हो तो हमसे संपर्क करें।")
                    }
                    Kind.PAYMENT -> {
                        sb.append("PAYMENT RECEIVED\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपका भुगतान सफलतापूर्वक प्राप्त हो गया है।\n\n")
                        sb.append("Payment Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nTotal Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("यह भुगतान आपके इलाज के हिसाब से जुड़ गया है।")
                    }
                    Kind.ENQUIRY -> {
                        sb.append("प्रिय ").append(who).append(",\n")
                        sb.append("हमसे संपर्क करने के लिए धन्यवाद।\n")
                        sb.append("चेंबर सुबह 11 बजे से शाम 4 बजे तक खुला रहता है।\n")
                        sb.append("किसी भी ज़रूरत पर फ़ोन करें।")
                    }
                    Kind.VISIT_DATE -> {
                        sb.append("NEXT VISIT SCHEDULED\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपकी अगली विज़िट की तारीख तय कर दी गई है।\n\n")
                        sb.append("⏰ ").append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("Visiting Time: सुबह 11 बजे से शाम 4 बजे तक\n\n")
                        sb.append("तय तारीख पर क्लिनिक में उपस्थित रहें।")
                    }
                    Kind.DUE_REMINDER -> {
                        sb.append("PAYMENT DUE REMINDER\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपके इलाज का बकाया भुगतान याद दिलाया जा रहा है।\n\n")
                        sb.append("Total Treatment Cost: ").append(money(bill)).append("\n")
                        sb.append("Total Paid: ").append(money(paid)).append("\n")
                        sb.append("🔴 Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("सुविधा अनुसार अथवा अगली विज़िट के समय बकाया चुका दें।")
                    }
                    Kind.RECEIPT -> {
                        sb.append("PAYMENT RECEIPT\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपकी भुगतान की रसीद तैयार है।\n\n")
                        if (amount > 0.0) sb.append("Payment Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        if (receiptNumber.isNotBlank()) sb.append("Receipt Number: ").append(receiptNumber).append("\n")
                        sb.append("\nTotal Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("भविष्य के लिए रसीद सुरक्षित रखें।")
                    }
                    Kind.VISIT_REMINDER -> {
                        sb.append("VISIT REMINDER\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपकी तय विज़िट की तारीख याद दिलाई जा रही है।\n\n")
                        sb.append("⏰ ").append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("Visiting Time: सुबह 11 बजे से शाम 4 बजे तक\n\n")
                        sb.append("तय तारीख पर क्लिनिक में उपस्थित रहें।")
                    }
                    Kind.DOCUMENT -> {
                        sb.append("MEDICAL DOCUMENT\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपका Prescription / Medicine Slip / Diet Chart PDF भेज दिया गया है।\n\n")
                        sb.append("कृपया Document डाउनलोड करके सुरक्षित रखें और डॉक्टर की सलाह का पालन करें।")
                    }
                    Kind.TREATMENT_DONE -> {
                        sb.append("TREATMENT COMPLETED\n\n")
                        sb.append("प्रिय ").append(who).append(",\n\n")
                        sb.append("आपका निर्धारित इलाज सफलतापूर्वक पूरा हो गया है।\n\n")
                        sb.append("डॉक्टर की सलाह एवं नियमों का पालन करें। भविष्य में कोई समस्या हो तो हमसे संपर्क करें।\n\n")
                        sb.append("आपके अच्छे स्वास्थ्य की कामना करते हैं।")
                    }
                }
            }
            else -> {
                when (kind) {
                    Kind.REGISTRATION -> {
                        sb.append("REGISTRATION CONFIRMED\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("Your registration has been completed successfully.\n\n")
                        sb.append("Patient ID: ").append(patientId).append("\n")
                        if (ageSex.isNotBlank()) sb.append("Age / Sex: ").append(ageSex).append("\n")
                        if (disease.isNotBlank()) sb.append("Disease: ").append(disease).append("\n")
                        if (mobile.isNotBlank()) sb.append("Mobile: ").append(mobile).append("\n")
                        if (address.isNotBlank()) sb.append("Address: ").append(address).append("\n")
                        if (regFee > 0.0) {
                            sb.append("Registration Fee: ").append(money(regFee)).append("\n")
                            if (regFeeMode.isNotBlank()) sb.append("Payment Mode: ").append(regFeeMode).append("\n")
                        }
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nPlease keep the Patient ID safe for future contact and your next visit.")
                    }
                    Kind.ADVANCE -> {
                        sb.append("ADVANCE PAYMENT RECEIVED\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("Your advance payment has been received successfully.\n\n")
                        if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                        sb.append("Advance Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nThis payment has been linked to your treatment account.")
                    }
                    Kind.BILL -> {
                        sb.append("Dear\n").append(who).append(",\n\n")
                        if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                        if (mobile.isNotBlank()) sb.append("Mobile: ").append(mobile).append("\n")
                        sb.append("\n")
                        if (amount > 0.0) {
                            val d = parseIso(paidTodayAtIso)
                            if (d != null) {
                                sb.append("Amount: ").append(money(amount)).append("\n")
                                sb.append("Successfully Received\n")
                                sb.append("Date: ").append(dotDate(d)).append(" (").append(dayName(d, "en")).append(")\n")
                                sb.append("Time: ").append(clockTime(d)).append("\n\n")
                                if (receivedByCode.isNotBlank()) sb.append("Received By: ").append(receivedByCode).append("\n\n")
                            }
                        }
                        if (showBillTotal) {
                            sb.append("Here is the financial summary of your treatment —\n")
                            sb.append("Total Treatment Cost: ").append(money(bill)).append("\n")
                        }
                        sb.append("Total Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("Please contact us if you have any questions or complaints about this payment.")
                    }
                    Kind.PAYMENT -> {
                        sb.append("PAYMENT RECEIVED\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("Your payment has been received successfully.\n\n")
                        sb.append("Payment Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("\nTotal Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("This payment has been added to your treatment account.")
                    }
                    Kind.ENQUIRY -> {
                        sb.append("Dear ").append(who).append(",\n")
                        sb.append("Thank you for contacting us.\n")
                        sb.append("Chamber is open 11 am to 4 pm.\n")
                        sb.append("Call us any time you need help.")
                    }
                    Kind.VISIT_DATE -> {
                        sb.append("NEXT VISIT SCHEDULED\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("Your next visit date has been scheduled.\n\n")
                        sb.append("⏰ ").append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("Visiting Time: 11:00 AM to 4:00 PM\n\n")
                        sb.append("Please be present at the clinic on the scheduled date.")
                    }
                    Kind.DUE_REMINDER -> {
                        sb.append("PAYMENT DUE REMINDER\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("This is a reminder about the pending payment for your treatment.\n\n")
                        sb.append("Total Treatment Cost: ").append(money(bill)).append("\n")
                        sb.append("Total Paid: ").append(money(paid)).append("\n")
                        sb.append("🔴 Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("Please clear the due amount at your convenience or during your next visit.")
                    }
                    Kind.RECEIPT -> {
                        sb.append("PAYMENT RECEIPT\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("Your payment receipt is ready.\n\n")
                        if (amount > 0.0) sb.append("Payment Amount: ").append(money(amount)).append("\n")
                        if (mode.isNotBlank()) sb.append("Payment Mode: ").append(mode).append("\n")
                        if (dateText.isNotBlank()) sb.append(dateTimeLine(dateText, timeText)).append("\n")
                        if (receiptNumber.isNotBlank()) sb.append("Receipt Number: ").append(receiptNumber).append("\n")
                        sb.append("\nTotal Paid: ").append(money(paid)).append("\n")
                        sb.append("Amount Due: ").append(money(due)).append("\n\n")
                        sb.append("Please keep this receipt safe for future reference.")
                    }
                    Kind.VISIT_REMINDER -> {
                        sb.append("VISIT REMINDER\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("This is a reminder about your scheduled visit date.\n\n")
                        sb.append("⏰ ").append(dateTimeLine(dateText, timeText)).append("\n")
                        sb.append("Visiting Time: 11:00 AM to 4:00 PM\n\n")
                        sb.append("Please be present at the clinic on the scheduled date.")
                    }
                    Kind.DOCUMENT -> {
                        sb.append("MEDICAL DOCUMENT\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("Your Prescription / Medicine Slip / Diet Chart PDF has been sent.\n\n")
                        sb.append("Please download and keep the document safe, and follow the doctor's advice.")
                    }
                    Kind.TREATMENT_DONE -> {
                        sb.append("TREATMENT COMPLETED\n\n")
                        sb.append("Dear ").append(who).append(",\n\n")
                        sb.append("Your scheduled treatment has been completed successfully.\n\n")
                        sb.append("Please follow the doctor's advice and instructions. Contact us if you face any problem in future.\n\n")
                        sb.append("We wish you good health.")
                    }
                }
            }
        }
        return sb.toString()
    }

    /** পুরো বার্তা — তিন ভাষা, ব্রাঞ্চ অনুযায়ী ক্রমে, শেষে হেল্পলাইন। */
    fun build(
        branch: String, name: String, patientId: String, kind: Kind,
        amount: Double = 0.0, mode: String = "", bill: Double = 0.0,
        paid: Double = 0.0, dateText: String = "", timeText: String = "",
        // 🔒 খাতার সারি B92 — নতুন তিনটে ঘর, তিনটেরই ডিফল্ট আছে, তাই পুরনো
        // কোনো ডাক বদলাতে হয়নি।
        regFee: Double = 0.0, regFeeMode: String = "", payLabel: String = "",
        // TK-APPROVED ADDITION (31.07.2026): default "" — পুরনো কোনো ডাক ভাঙে না।
        receiptNumber: String = "",
        showBillTotal: Boolean = true, paidTodayAtIso: String = ""
    ): String {
        val info = BranchCatalog.byName(branch)
        val clinic = info.clinicName
        val place = info.displayName
        // 🔒 কিশনগঞ্জে হিন্দি আগে, বাকি সব ব্রাঞ্চে বাংলা আগে।
        val order = if (place.equals("Kishanganj", ignoreCase = true))
            listOf("hi", "bn", "en") else listOf("bn", "hi", "en")
        // ক্লিনিকের নাম-ঠিকানা একবারই উপরে, তারপর তিন ভাষার অংশ, শেষে হেল্পলাইন।
        val head = clinic + "\n" + info.addressLine
        val parts = order.map {
            block(it, clinic, place, name, patientId, kind, amount, mode, bill, paid, dateText, timeText,
                regFee, regFeeMode, payLabel, receiptNumber, showBillTotal, paidTodayAtIso)
        }
        return head + "\n\n" + parts.joinToString("\n\n") + "\n\nHelpline : " + info.phoneLine
    }

    private fun sendSms(activity: Activity, mobile: String, text: String) {
        val digits = tenDigits(mobile)
        try {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$digits"))
            i.putExtra("sms_body", text)
            activity.startActivity(i)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, "No Message app found on this phone", Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {
            Toast.makeText(activity, "Could not open Message app", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🔒 খাতার সারি B91 (TK, 29.07.2026 বিকেল ৪.১০): *"হোয়াটসঅ্যাপ চুজ করলে
     * ডাইরেক্ট সেই পেশেন্টের হোয়াটসঅ্যাপে যায় না, নাম্বার সিলেক্ট করতে হয়...
     * প্রফেশনাল লুক মেসেজ যাবে, নাম্বার বাছাই করাও লাগবে না।"*
     *
     * **কারণ (কোড দেখে):** আগে ছবিওয়ালা কার্ড পাঠানোর চেষ্টা হত। **WhatsApp-এ
     * ছবি পাঠানোর সময় নম্বর বলে দেওয়ার কোনো উপায় নেই** — এটা WhatsApp-এর
     * নিজের সীমা — তাই কনট্যাক্ট বাছার পর্দা আসত।
     *
     * **এখন:** ছবির বদলে **WhatsApp-এর নিজের সাজসজ্জা দেওয়া লেখা** যায়
     * (মোটা অক্ষর `*...*`, লাইন টানা, ইমোজি)। লেখা পাঠানোর সময় নম্বর বলে
     * দেওয়া যায়, তাই **সরাসরি ওই রোগীর চ্যাটই খোলে** — স্টাফ শুধু Send চাপবেন।
     *
     * ⛔ **SMS-এ এক অক্ষরও হাত পড়েনি** — ওখানে আগের সেই সাদামাটা লেখাই যায়
     *    (SMS-এ `*` বা `_` সাজসজ্জা হিসেবে কাজ করে না, অক্ষর হয়েই দেখাত)।
     * ⛔ **বার্তার কথাগুলো এক অক্ষরও বদলানো হয়নি** — `block()` ছোঁয়াই হয়নি;
     *    শুধু উপরে-নিচে সাজ ও মোটা অক্ষর যোগ হয়েছে।
     * ⛔ তিন ভাষা · ভাষার ক্রম · ব্রাঞ্চের নাম-ঠিকানা-হেল্পলাইন — সব আগের মতোই।
     */
    private fun bold(line: String): String {
        val t = line.trim()
        if (t.isBlank()) return line
        val i = t.indexOf(" : ")
        if (i > 0 && i + 3 < t.length) {
            val head = t.substring(0, i + 3)
            val value = t.substring(i + 3).trim()
            if (value.isNotBlank() && !value.contains("*")) return head + "*" + value + "*"
        }
        return line
    }

    fun buildWhatsApp(
        branch: String, name: String, patientId: String, kind: Kind,
        amount: Double = 0.0, mode: String = "", bill: Double = 0.0,
        paid: Double = 0.0, dateText: String = "", timeText: String = "",
        regFee: Double = 0.0, regFeeMode: String = "", payLabel: String = "",
        receiptNumber: String = ""
    ): String {
        val info = BranchCatalog.byName(branch)
        val order = if (info.displayName.equals("Kishanganj", ignoreCase = true))
            listOf("hi", "bn", "en") else listOf("bn", "hi", "en")
        val line = "━━━━━━━━━━━━━━━"
        val sb = StringBuilder()
        sb.append("🌿 *").append(info.clinicName).append("*\n")
        sb.append("_").append(info.addressLine).append("_\n")
        for (lang in order) {
            sb.append(line).append("\n")
            val raw = block(lang, info.clinicName, info.displayName, name, patientId,
                kind, amount, mode, bill, paid, dateText, timeText, regFee, regFeeMode, payLabel, receiptNumber)
            val rows = raw.split("\n")
            rows.forEachIndexed { idx, r ->
                // প্রথম লাইনটা সম্বোধন — সেটা মোটা অক্ষরে; বাকি লাইনে শুধু
                // ": "-এর পরের মানটুকু মোটা (Patient ID · তারিখ ইত্যাদি)।
                if (idx == 0 && r.isNotBlank()) sb.append("*").append(r.trim()).append("*\n")
                else sb.append(bold(r)).append("\n")
            }
        }
        sb.append(line).append("\n")
        sb.append("📞 Helpline : *").append(info.phoneLine).append("*")
        return sb.toString()
    }

    // 🔒 TK-ORDER (31.07.2026): এখন Personal/Business WhatsApp দুটোই ফোনে
    // থাকলে চুস করার জন্য Android chooser দেখায় — বিস্তারিত
    // `WhatsAppMessageChooser.kt`-এ। ⛔ বার্তার লেখা (`text`)/URL-বানানো
    // (`wa.me`) একটুও বদলায়নি, শুধু কোন প্যাকেজে পাঠানো হবে সেই সিদ্ধান্তটা।
    private fun sendWhatsApp(activity: Activity, mobile: String, text: String, branch: String, onDone: (() -> Unit)? = null) {
        WhatsAppMessageChooser.send(activity, mobile, text, onDone)
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔴🔴 TK-ORDER (01.08.2026): Enquiry/First Visit-এর মতোই বাকি ১০টা
    // বার্তাও (Registration/Advance/Bill/Payment/Visit Date/Due Reminder/
    // Receipt/Visit Reminder/Document/Treatment Done) এখন **এক ভাষা বেছে**
    // পাঠাতে হবে — তিন ভাষা একসাথে আর নয়। `block()`-এর লেখা (bn/hi/en
    // তিনটেই) এক অক্ষরও বদলানো হয়নি, শুধু একবারে একটা ভাষাই জোড়া হচ্ছে
    // (আগে `build()`/`buildWhatsApp()` তিনটে ভাষা লুপ করে জোড়ত)।
    // ⛔ পুরনো `build()`/`buildWhatsApp()` (তিন-ভাষা-একসাথে) অক্ষত রইল,
    //    কিন্তু `show()` আর এদের ডাকে না — প্রজেক্টের অন্য কোথাও এই দুটো
    //    পাবলিক ফাংশন ডাকা হয় না বলে নিরাপদে রাখা গেল।
    // ══════════════════════════════════════════════════════════════════════
    // 🔒 TK-এর নির্দেশ (02.08.2026, দ্বিতীয় দফায় নমুনা দিয়ে চূড়ান্ত করেছেন):
    // 🔒 TK-এর নির্দেশ (02.08.2026, দ্বিতীয় দফায় নমুনা দিয়ে চূড়ান্ত করেছেন):
    // স্বাক্ষর সরাসরি "TK BISWAS" দিয়ে শুরু (আলাদা "Regards,"/"সবিনয়ে,"
    // লাইন ছাড়াই), আর লেবেল "Helpline" (আগে "Contact" লেখা হয়েছিল,
    // TK-এর নমুনায় "Helpline" — সেটাই রইল)।
    private fun buildSingleLang(
        lang: String, branch: String, name: String, patientId: String, kind: Kind,
        amount: Double, mode: String, bill: Double, paid: Double, dateText: String,
        /** 🔴 V505 — তারিখের পাশের সময় (ফাঁকা = সময় জানা নেই)। */
        timeText: String,
        regFee: Double, regFeeMode: String, payLabel: String, receiptNumber: String,
        showBillTotal: Boolean = true, paidTodayAtIso: String = "",
        mobile: String = "", receivedBy: String = "",
        ageSex: String = "", disease: String = "", address: String = ""
    ): String {
        val info = BranchCatalog.byName(branch)
        val body = block(lang, info.clinicName, info.displayName, name, patientId, kind,
            amount, mode, bill, paid, dateText, timeText, regFee, regFeeMode, payLabel, receiptNumber,
            showBillTotal, paidTodayAtIso, mobile, receivedBy, ageSex, disease, address)
        // 🔒🔒 TK-এর চূড়ান্ত নির্দেশ (02.08.2026): বাংলা বার্তায় "ধন্যবাদান্তে" +
        // বাংলা ক্লিনিক-তথ্য (স্বাক্ষর/Founder-এর নাম নেই); হিন্দি/ইংরেজিতে
        // আগের TK BISWAS/Founder & Consultant ফরম্যাটই থাকল (TK শুধু বাংলার
        // কথা বলেছেন — "যখন বাংলা ভাষায় বার্তা পাঠাবো")।
        // 🔒 B587 (TK-নির্দেশ, 08.08.2026): সইয়ে ব্যক্তির নাম "TK BISWAS"
        // (বড় হাতে) ও নিচে "Founder & Consultant" (ছোট হাতে) — সব ভাষায় এক সই।
        // ব্রাঞ্চের নাম/ঠিকানা/হেল্পলাইন অপরিবর্তিত।
        val footer = if (lang == "bn") {
            val bn = bnBranchText[info.id] ?: BnBranchText(info.clinicName, info.addressLine)
            "ধন্যবাদান্তে\nTK BISWAS\nFounder & Consultant\n\n" +
                bn.clinicNameBn + "\n" + bn.addressBn + "\nহেল্পলাইন: " + info.phoneLine
        } else {
            "TK BISWAS\nFounder & Consultant\n\n" +
                info.clinicName + "\n" + info.addressLine + "\nHelpline : " + info.phoneLine
        }
        return body + "\n\n" + footer
    }

    private fun buildSingleLangWhatsApp(
        lang: String, branch: String, name: String, patientId: String, kind: Kind,
        amount: Double, mode: String, bill: Double, paid: Double, dateText: String,
        /** 🔴 V505 — তারিখের পাশের সময় (ফাঁকা = সময় জানা নেই)। */
        timeText: String,
        regFee: Double, regFeeMode: String, payLabel: String, receiptNumber: String,
        showBillTotal: Boolean = true, paidTodayAtIso: String = "",
        mobile: String = "", receivedBy: String = "",
        ageSex: String = "", disease: String = "", address: String = ""
    ): String {
        val info = BranchCatalog.byName(branch)
        val line = "━━━━━━━━━━━━━━━"
        val sb = StringBuilder()
        val raw = block(lang, info.clinicName, info.displayName, name, patientId, kind,
            amount, mode, bill, paid, dateText, timeText, regFee, regFeeMode, payLabel, receiptNumber,
            showBillTotal, paidTodayAtIso, mobile, receivedBy, ageSex, disease, address)
        raw.split("\n").forEachIndexed { idx, r ->
            if (idx == 0 && r.isNotBlank()) sb.append("*").append(r.trim()).append("*\n")
            else sb.append(bold(r)).append("\n")
        }
        sb.append(line).append("\n")
        // 🔒 B587 (TK-নির্দেশ): সইয়ে "TK BISWAS" (বড় হাতে) + "Founder & Consultant"
        // (ছোট হাতে) — সব ভাষায় এক। ব্রাঞ্চ নাম/ঠিকানা/হেল্পলাইন অপরিবর্তিত।
        if (lang == "bn") {
            val bn = bnBranchText[info.id] ?: BnBranchText(info.clinicName, info.addressLine)
            sb.append("*ধন্যবাদান্তে*\n")
            sb.append("*TK BISWAS*\nFounder & Consultant\n\n")
            sb.append("*").append(bn.clinicNameBn).append("*\n")
            sb.append(bn.addressBn).append("\n")
            sb.append("📞 হেল্পলাইন: *").append(info.phoneLine).append("*")
        } else {
            sb.append("*TK BISWAS*\nFounder & Consultant\n\n")
            sb.append("*").append(info.clinicName).append("*\n")
            sb.append(info.addressLine).append("\n")
            sb.append("📞 Helpline : *").append(info.phoneLine).append("*")
        }
        return sb.toString()
    }

    /**
     * সেভ হওয়ার পরে এই বাক্সটা ওঠে। SMS / WhatsApp / পরে পাঠাব।
     * [onClosed] সবসময় ডাকা হয় (পাঠান বা না পাঠান) — তাই আগের ফ্লো
     * (যেমন রেজিস্ট্রেশনের পরে পর্দা বন্ধ হওয়া) কখনো আটকায় না।
     * 🔴 TK-ORDER (01.08.2026): এখন আগে ভাষা বাছাই (bn/hi/en), তারপর
     * সেই এক ভাষার বার্তা — Enquiry/First Visit-এর মতোই একই প্যাটার্ন।
     */
    fun show(
        activity: Activity, branch: String, name: String, mobile: String,
        patientId: String, kind: Kind, amount: Double = 0.0, mode: String = "",
        bill: Double = 0.0, paid: Double = 0.0, dateText: String = "",
        /** 🔴🔒 V505 (TK-নির্দেশ ২১.০৮.২০২৬) — তারিখের পাশে সময়।
         *  ডিফল্ট "" — তাই পুরনো কোনো ডাক ভাঙে না, শুধু সময় দেখাবে না। */
        timeText: String = "",
        // TK-FINAL (28.07.2026): রসিদ ও কাগজপত্র শুধু WhatsApp-এ যায় (SMS-এ
        // এত লম্বা লেখা ভেঙে যায়)। ডিফল্ট false, তাই পুরনো ছ'টা ডাক অপরিবর্তিত।
        whatsAppOnly: Boolean = false,
        // 🔒 খাতার সারি B92 — ডিফল্ট আছে, তাই পুরনো ১২টা ডাক অপরিবর্তিত।
        regFee: Double = 0.0, regFeeMode: String = "", payLabel: String = "",
        // TK-APPROVED ADDITION (31.07.2026): শুধু Kind.RECEIPT ব্যবহার করে,
        // ডিফল্ট "" তাই পুরনো কোনো ডাক ভাঙে না।
        receiptNumber: String = "",
        // 🔒 TK-এর নির্দেশ (01.08.2026): Bill বার্তায় "সর্বমোট বিল" শুধু
        // প্রথমবার (বিল তৈরি/প্রথম Advance) দেখাবে, আর আজকের জমা তারিখ-বার-
        // সময়সহ দেখাবে। দুটোরই ডিফল্ট আছে (true / ""), তাই পুরনো সব ডাক
        // (Registration, Advance, Payment, ইত্যাদি) এক অক্ষরও বদলায় না।
        showBillTotal: Boolean = true, paidTodayAtIso: String = "",
        // 🔒 TK-এর নির্দেশ (01.08.2026): "টাকা কে জমা নিয়েছে" — বার্তায়
        // থাকতে হবে, যাতে পরে সমস্যা হলে বার্তা দেখিয়ে বোঝা যায় কোন স্টাফ
        // টাকা নিয়েছিলেন। ডিফল্ট "" — শুধু Kind.BILL পড়ে, পুরনো ডাক অক্ষত।
        receivedBy: String = "",
        // 🔒 B597 (TK-অনুমোদিত): রেজিস্ট্রেশন বার্তার সম্পূর্ণ বিবরণ। ডিফল্ট "" —
        // পুরনো সব show() ডাক অপরিবর্তিত; শুধু RegistrationActivity এগুলো পাঠায়।
        ageSex: String = "", disease: String = "", address: String = "",
        // 🔒 B598 (TK-অনুমোদিত): A4 রসিদ বোতামের জন্য রোগীর তথ্য (JSON)। null হলে
        // বোতাম দেখানো হয় না — তাই পুরনো সব ডাক অপরিবর্তিত।
        a4Patient: org.json.JSONObject? = null,
        onClosed: (() -> Unit)? = null
    ) {
        val digits = tenDigits(mobile)
        if (activity.isFinishing || activity.isDestroyed || digits.length != 10) {
            onClosed?.invoke(); return
        }
        // 🔒 B600 (TK-অনুমোদিত, প্রুফ দেখিয়ে): বার্তা-বাক্সের A4 বোতামের মডেল —
        // রেজিস্ট্রেশন হলে registration(), টাকা-জমার বার্তা (Advance/Payment/Receipt)
        // হলে paymentReceipt() — দুটোই বিদ্যমান PrintMappersCloud থেকে। ⛔ নতুন
        // PDF কোড লেখা হয়নি; কেবল ওই স্ক্রিনে পৌঁছানোর মডেল বানাই।
        val a4Model: com.tkbiswas.pilesclinic.print.PrintDocumentModel? = try {
            when {
                a4Patient != null -> com.tkbiswas.pilesclinic.print.PrintMappersCloud.registration(a4Patient)
                kind == Kind.PAYMENT || kind == Kind.ADVANCE || kind == Kind.RECEIPT -> {
                    val pj = org.json.JSONObject().apply {
                        put("name", name); put("patientId", patientId); put("mobile", digits)
                        put("branch", branch); put("amount", amount); put("mode", mode)
                        put("payLabel", payLabel); put("receivedBy", receivedBy)
                        put("disease", disease); put("address", address)
                        put("date", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
                    }
                    com.tkbiswas.pilesclinic.print.PrintMappersCloud.paymentReceipt(pj)
                }
                else -> null
            }
        } catch (_: Throwable) { null }
        val a4Label = if (a4Patient != null) "📄  A4 Registration — Send / Print" else "📄  A4 Receipt — Send / Print"
        showLanguagePicker(
            activity = activity,
            onPicked = { lang ->
                val text = try {
                    buildSingleLang(lang, branch, name, patientId, kind, amount, mode, bill, paid, dateText, timeText,
                        regFee, regFeeMode, payLabel, receiptNumber, showBillTotal, paidTodayAtIso, digits, receivedBy,
                        ageSex, disease, address)
                } catch (_: Throwable) {
                    onClosed?.invoke(); return@showLanguagePicker
                }
                // 🔒 খাতার সারি B91: SMS-এ সাদামাটা লেখাই যায় (উপরের `text`), আর
                // WhatsApp-এ যায় সাজানো লেখা (মোটা অক্ষর · লাইন · ইমোজি)। বানাতে
                // না পারলে আগের সেই সাদামাটা লেখাই যাবে — কাজ কখনো আটকাবে না।
                val waText = try {
                    buildSingleLangWhatsApp(lang, branch, name, patientId, kind, amount, mode, bill, paid, dateText, timeText,
                        regFee, regFeeMode, payLabel, receiptNumber, showBillTotal, paidTodayAtIso, digits, receivedBy,
                        ageSex, disease, address)
                } catch (_: Throwable) { text }
                /* 🔴🔒 V503 (TK-রিপোর্ট ২১.০৮.২০২৬, TK-এর বাছাই অনুযায়ী):
                   *"এখানে ম্যাসেজ যেটা আছে কমপ্লিট হয় নাই — ক্লিনিকের নাম নেই, ঠিকানা নেই।"*

                   কারণ (প্রমাণ নিচের `previewEn`-এর মন্তব্যে): বাংলা-বন্ধ ফোনে
                   (Kishanganj স্টাফ) `NoBengali` পর্দার বাংলা লেখাকে শব্দে-শব্দে
                   বদলে দেয় ও বাকিটা মুছে দেয় — তাই নমুনাটা ভেঙে যেত এবং
                   ক্লিনিকের বাংলা নাম-ঠিকানা উধাও হয়ে যেত।

                   এখন ওই ফোনে পর্দায় **সম্পূর্ণ ইংরেজি নমুনা** দেখানো হয়।
                   ⛔ রোগীর কাছে যা যায় (`text` / `waText`) এক অক্ষরও বদলায় না —
                      তিনি বেছে নেওয়া ভাষাতেই পান। */
                val previewEn = try {
                    buildSingleLang("en", branch, name, patientId, kind, amount, mode, bill, paid, dateText, timeText,
                        regFee, regFeeMode, payLabel, receiptNumber, showBillTotal, paidTodayAtIso, digits, receivedBy,
                        ageSex, disease, address)
                } catch (_: Throwable) { null }
                presentSendBox(activity, branch, name, digits, text, waText, whatsAppOnly, onClosed, logKind = kind.name, a4Model = a4Model, a4Label = a4Label, a4RegJson = a4Patient, previewEn = previewEn)
            },
            onCancelled = { onClosed?.invoke() }
        )
    }

    // 🔒 ENQUIRY_WHATSAPP_MESSAGE_FINAL_LOCK_2026-07-31_0950_IST — এই ফাংশনটা
    // `show()`-এর ভিতরের পুরনো পপ-আপ-তৈরির কোড হুবহু বার করে আনা হলো (এক
    // অক্ষরও বদলানো হয়নি), যাতে নতুন Enquiry ভাষা-বাছাই ফ্লো একই প্রফেশনাল
    // বাক্স পুনর্ব্যবহার করতে পারে। ⛔ পুরনো ১২টা `show()` ডাক অক্ষত।
    private fun presentSendBox(
        activity: Activity, branch: String, name: String, digits: String,
        text: String, waText: String, whatsAppOnly: Boolean, onClosed: (() -> Unit)?,
        // 🔴🟢 খাতার সারি B433 (TK-নির্দেশ, 05.08.2026) — কোন ধরনের বার্তা
        // (Kind.name, বা Enquiry/First-Visit-এর জন্য নিজস্ব ছোট নাম) তা
        // চিহ্নিত করার জন্য। ডিফল্ট "" — যেখানে না পাঠানো হয় (থাকার কথা
        // না), সেখানে শুধু আগে-পাঠানো-হয়েছে-কিনা যাচাইটা বাদ পড়ে, বাকি সব
        // আগের মতোই কাজ করে (পুরনো কোনো ডাক ভাঙে না)।
        logKind: String = "",
        // 🔒 B598/B600 (TK-অনুমোদিত): A4 রসিদ বোতামের রেডি মডেল (null হলে বোতাম নেই)।
        a4Model: com.tkbiswas.pilesclinic.print.PrintDocumentModel? = null,
        a4Label: String = "📄  A4 — Send / Print",
        // 🔒 B601 (TK-নির্দেশ, প্রুফ-হুবহু): রেজিস্ট্রেশন হলে রোগীর JSON — থাকলে A4
        // বোতাম নেটিভ মডেলের বদলে ওয়েব-হুবহু HTML প্রিন্ট খোলে। null = পুরনো পথ।
        a4RegJson: org.json.JSONObject? = null,
        /** 🔴 V503 — বাংলা-বন্ধ ফোনে **শুধু পর্দায়** দেখানোর ইংরেজি নমুনা।
         *  `null` = আগের আচরণ (পুরনো কোনো ডাক ভাঙে না)। */
        previewEn: String? = null
    ) {
        var done = false
        fun finishOnce() {
            if (!done) { done = true; onClosed?.invoke() }
        }
        // 🔴🟢 খাতার সারি B433 — পপ-আপ খোলার আগে ক্লাউডে (ব্যাকগ্রাউন্ডে)
        // যাচাই করা হয় এই নম্বরে এই ধরনের বার্তা আগে কেউ পাঠিয়েছেন কিনা।
        // ⛔ এই চেকের জন্য পপ-আপ খোলা কখনো আটকে থাকে না — ব্যর্থ/ধীর হলে
        // চুপচাপ সতর্কতা ছাড়াই খোলে।
        if (logKind.isBlank() || digits.length != 10) {
            buildAndShowSendBox(activity, branch, name, digits, text, waText, whatsAppOnly, logKind, null, ::finishOnce, a4Model, a4Label, a4RegJson, previewEn)
            return
        }
        BackgroundWork.run {
            val prior = try { MessageSentLog.checkPrior(digits, logKind) } catch (_: Throwable) { null }
            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.runOnUiThread {
                        buildAndShowSendBox(activity, branch, name, digits, text, waText, whatsAppOnly, logKind, prior, ::finishOnce, a4Model, a4Label, a4RegJson, previewEn)
                    }
                } else finishOnce()
            } catch (_: Throwable) { finishOnce() }
        }
    }

    private fun buildAndShowSendBox(
        activity: Activity, branch: String, name: String, digits: String,
        text: String, waText: String, whatsAppOnly: Boolean, logKind: String,
        prior: MessageSentLog.PriorSend?, finishOnce: () -> Unit,
        // 🔒 B598/B600 (TK-অনুমোদিত): A4 রসিদ বোতামের রেডি মডেল (null হলে বোতাম নেই)।
        a4Model: com.tkbiswas.pilesclinic.print.PrintDocumentModel? = null,
        a4Label: String = "📄  A4 — Send / Print",
        // 🔒 B601 (TK-নির্দেশ, প্রুফ-হুবহু): রেজিস্ট্রেশন JSON — থাকলে A4 বোতাম
        // ওয়েব-হুবহু HTML প্রিন্ট খোলে (RegistrationHtmlPrint)। null = পুরনো পথ।
        a4RegJson: org.json.JSONObject? = null,
        /** 🔴 V503 — দেখুন `presentSendBox`-এর একই নামের প্যারামিটার। */
        previewEn: String? = null
    ) {
        try {
            // 🔒 TK-APPROVED (30.07.2026 দুপুর ৩.১০, ফটো-প্রুফে "ওকে পছন্দ হয়েছে"
            //    · খাতার সারি B163): *"ফলোয়াপ থেকে যে সমস্ত বার্তা যাবে, ডাক্তার
            //    ভিজিট থেকে যে সমস্ত বার্তা যাবে — উক্ত Pop up গুলি প্রফেশনাল লুক
            //    আসতে হবে... নিচে যেখানে লেখা SMS · পরে পাঠাবো · WhatsApp।"*
            //
            //    আগে এটা ডিফল্ট AlertDialog ছিল (ছোট শিরোনাম + সাদা টেক্সট-বোতাম)।
            //    এখন প্রজেক্টের নিজের প্রিমিয়াম ধাঁচ — সবুজ হেডার · বার্তার প্রিভিউ
            //    · নিচে **তিনটে সমান মাপের পিল বোতাম**।
            // ⛔ বার্তার লেখা (`text` / `waText`) এক অক্ষরও বদলায়নি — শুধু চেহারা।
            // ⛔ কাজের নিয়মও অপরিবর্তিত: WhatsApp · SMS · Later, `whatsAppOnly`
            //    হলে SMS দেখানো হয় না, আর `finishOnce()` সব পথেই ডাকা হয়।
            val d = activity.resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()

            val root = android.widget.LinearLayout(activity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE)
                    cornerRadius = 20f * d
                }
            }
            root.addView(android.widget.TextView(activity).apply {
                this.text = "📩   Send Message"
                textSize = 17f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#145A32"))
                setPadding(dp(18), dp(16), dp(18), dp(16))
            })
            root.addView(android.widget.TextView(activity).apply {
                this.text = "To patient : " + name.trim().uppercase() + "  ·  " + digits
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#10223A"))
                setPadding(dp(18), dp(14), dp(18), dp(8))
            })
            // 🔴🟢 খাতার সারি B433 (TK-নির্দেশ, 05.08.2026) — আগে এই একই
            // ধরনের বার্তা এই নম্বরে পাঠানো হয়ে থাকলে সতর্কতা। ⛔ শুধু
            // মনে করিয়ে দেয়, বোতাম বন্ধ করে না — স্টাফ চাইলে আবার
            // পাঠাতে পারবেন।
            if (prior != null) {
                root.addView(android.widget.TextView(activity).apply {
                    this.text = MessageSentLog.warningText(prior)
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#8A5A00"))
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#FFF6E0"))
                        cornerRadius = 10f * d
                    }
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(dp(18), dp(2), dp(18), dp(4))
                    layoutParams = lp
                })
            }
            val preview = android.widget.TextView(activity).apply {
                /* 🔴🔒 V503 — বাংলা-বন্ধ ফোনে (KNE-KISHAN5 / Kishanganj স্টাফ)
                   বাংলা নমুনা দেখালে `NoBengali` সেটাকে শব্দে-শব্দে ভেঙে দেয়
                   ("রেজিস্ট্রেশন হয়ে গেছে।" → "Registration being.") আর
                   ক্লিনিকের বাংলা নাম-ঠিকানা একেবারে মুছে ফেলে। তাই ওই ফোনে
                   **সম্পূর্ণ ইংরেজি নমুনা** দেখানো হয় — ভাঙা বাংলা নয়।
                   ⛔ রোগীর কাছে যাওয়া বার্তা এতে বদলায় না (নিচে WhatsApp
                      `waText` ও SMS `text` সরাসরি পাঠায়)। */
                val showBengaliSafe = try {
                    if (NoBengali.active() && previewEn != null) previewEn else text
                } catch (_: Throwable) { text }
                this.text = showBengaliSafe
                textSize = 12.5f
                setTextColor(android.graphics.Color.parseColor("#1E2A3A"))
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#F7FAFC"))
                    cornerRadius = 12f * d
                    setStroke(dp(1), android.graphics.Color.parseColor("#D8E1EC"))
                }
                // 🔒 TK-এর নির্দেশ (02.08.2026, স্ক্রিনশটসহ — "ডান দেখে কেটে
                // যাচ্ছে, উপরে নিচে হতে হবে"): আগে এই TextView-এর নিজের কোনো
                // চওড়া বাঁধা ছিল না (ScrollView-এর ভিতরে ডিফল্ট WRAP_CONTENT),
                // তাই লম্বা লাইন পর্দার বাইরে বেরিয়ে **ডানে কেটে যেত**, নিচের
                // লাইনে নামত না। এখন MATCH_PARENT চওড়া — লেখা পর্দার মধ্যেই
                // ভেঙে **পরের লাইনে নামবে**, কিছুই কাটবে না। ⛔ বার্তার লেখা
                // (`text`) এক অক্ষরও বদলায়নি — শুধু দেখানোর চেহারা।
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            root.addView(android.widget.ScrollView(activity).apply {
                addView(preview)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(300)
                ).apply { setMargins(dp(18), 0, dp(18), dp(4)) }
            })

            val row = android.widget.LinearLayout(activity).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(10), dp(12), dp(14))
            }
            fun pill(label: String, bg: String, textColor: Int, onTap: () -> Unit) =
                android.widget.TextView(activity).apply {
                    this.text = label
                    textSize = 13f
                    gravity = android.view.Gravity.CENTER
                    maxLines = 1
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(textColor)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor(bg))
                        cornerRadius = 12f * d
                    }
                    val lp = android.widget.LinearLayout.LayoutParams(0, dp(46), 1f)
                    lp.setMargins(dp(5), 0, dp(5), 0)
                    layoutParams = lp
                    isClickable = true
                    setOnClickListener { onTap() }
                }

            // 🔴🟢 খাতার সারি B433 — সত্যিই পাঠানো হলে (WhatsApp/SMS,
            // "Later" নয়) নিঃশব্দে ব্যাকগ্রাউন্ডে রেকর্ড হয়, যাতে পরের
            // স্টাফ একই নম্বরে একই বার্তা পাঠাতে গেলে সতর্কতা পান।
            fun logSent(channel: String) {
                if (logKind.isBlank()) return
                BackgroundWork.run {
                    try {
                        val staff = NativeSession.current(activity)
                        MessageSentLog.record(
                            digits, logKind, branch, name,
                            staff?.mobile ?: "", staff?.name ?: "", channel
                        )
                    } catch (_: Throwable) { }
                }
            }

            lateinit var dlg: AlertDialog
            // 🔒🔒 V234 (TK verified live-test + demo-approved, 01.08.2026): সব Send
            // Message popup-এ **একই সারিতে একই ক্রম — WhatsApp → Later → SMS**।
            // আগে SMS বোতামটা `whatsAppOnly` হলে লুকানো ছিল, তাই কিছু popup-এ
            // "শুধু WhatsApp ও Later" দেখাত। এখন **তিনটি বোতামই সব popup-এ** থাকে।
            // ⛔ বার্তার লেখা/ভাষা, header, preview, রঙ (সবুজ/ধূসর/নীল), size/shape —
            //    কিছুই বদলায়নি; WhatsApp (`waText`)/SMS (`text`)/Later-এর flow ও
            //    আগের মতোই। শুধু বোতামের ক্রম ও SMS-এর দৃশ্যমানতা।
            row.addView(pill("💬  WhatsApp", "#0C9E33", android.graphics.Color.WHITE) {
                dlg.dismiss(); logSent("whatsapp"); sendWhatsApp(activity, digits, waText, branch, onDone = { finishOnce() })
            })
            row.addView(pill("Later", "#E4E8EE", android.graphics.Color.parseColor("#3C4859")) {
                dlg.dismiss(); finishOnce()
            })
            row.addView(pill("✉  SMS", "#1E5AB4", android.graphics.Color.WHITE) {
                dlg.dismiss(); logSent("sms"); sendSms(activity, digits, text); finishOnce()
            })
            root.addView(row)

            // 🔒 B598/B600 (TK-অনুমোদিত, প্রুফ দেখিয়ে): বার্তা-বাক্স থেকেই A4 রসিদ পর্দা
            // (বিদ্যমান PrintPreviewActivity — Save / Share / Print)। রেজিস্ট্রেশন ও
            // টাকা-জমার (Advance/Payment/Receipt) — দুটোতেই। ⛔ নতুন PDF কোড নেই;
            // রেডি PrintDocumentModel রি-ইউজ। বার্তা-বাক্স বন্ধ হয় না — স্টাফ A4 দেখে
            // ফিরে এসে বার্তাও পাঠাতে পারে।
            if (a4Model != null) {
                root.addView(android.widget.TextView(activity).apply {
                    this.text = a4Label
                    textSize = 13.5f
                    gravity = android.view.Gravity.CENTER
                    maxLines = 1
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#0F5132"))
                        cornerRadius = 12f * d
                    }
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
                    )
                    lp.setMargins(dp(17), 0, dp(17), dp(14))
                    layoutParams = lp
                    isClickable = true
                    setOnClickListener {
                        try {
                            // 🔒 B601 (TK-নির্দেশ, প্রুফ-হুবহু): রেজিস্ট্রেশন হলে ওয়েবের
                            // হুবহু একই ডিজাইনে HTML প্রিন্ট (WebView→PrintManager)। বাকি
                            // (টাকা-জমা) আগের নেটিভ PrintPreviewActivity পথেই যায়।
                            if (a4RegJson != null) {
                                /* 🔴🔒 V502 (TK-রিপোর্ট ২১.০৮.২০২৬):
                                   *"A4 Registration শুধুমাত্র Print হচ্ছে, কিন্তু
                                   WhatsApp / Business WhatsApp-এ শেয়ার হচ্ছে না।"*

                                   কারণ: এই বোতামটা সরাসরি `RegistrationHtmlPrint.print()`
                                   ডাকত, যেটা শুধুই Android-এর PrintManager খোলে —
                                   শেয়ারের কোনো পথই ছিল না। (টাকা-জমার রসিদে ছিল,
                                   কারণ সেটা `PrintPreviewActivity` খোলে।)

                                   এখন হুবহু **একই A4 ডিজাইন** (`RegistrationHtml.build`)
                                   PDF হয়ে তিনটে পথ দেখায়: WhatsApp · WhatsApp Business ·
                                   Print/PDF — TK-এর অনুমোদিত সেই একই বাক্স।
                                   ⛔ নতুন কিছু বানানো হয়নি; V501-এ বসানো প্রমাণিত
                                      `PrescriptionWhatsAppShare.shareHtml()`-ই ডাকা হয়।
                                   ⛔ ছাপার কাগজ এক অক্ষরও বদলায়নি। */
                                com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
                                    activity = activity,
                                    html = com.tkbiswas.pilesclinic.print.RegistrationHtml.build(a4RegJson),
                                    documentTitle = "Registration",
                                    patientName = name,
                                    allowPrint = true
                                )
                            } else {
                                com.tkbiswas.pilesclinic.print.PrintDataHolder.pendingModel = a4Model
                                activity.startActivity(
                                    Intent(activity, com.tkbiswas.pilesclinic.print.PrintPreviewActivity::class.java)
                                )
                            }
                        } catch (_: Throwable) {
                            Toast.makeText(activity, "Could not open A4 receipt", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }

            dlg = AlertDialog.Builder(activity).setView(root).create()
            dlg.setOnCancelListener { finishOnce() }
            dlg.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
            // 🔒 খাতার সারি B158: বাংলা বন্ধ থাকা স্টাফের জন্য এই পপ-আপের
            // **দেখানো লেখাটা** বাংলা-মুক্ত হয়। ⛔ রোগীর কাছে যাওয়া আসল
            // বার্তা (`text` / `waText`) এতে **এক অক্ষরও বদলায় না** — ওটা
            // আলাদা স্ট্রিং থেকে পাঠানো হয়, তাই রোগী তিন ভাষাতেই পান।
            try { NoBengali.installDialog(dlg) } catch (_: Throwable) { }
        } catch (_: Throwable) {
            finishOnce()
        }
    }

    // 🔒 ENQUIRY_WHATSAPP_MESSAGE_FINAL_LOCK_2026-07-31_0950_IST — TK-এর
    // FINAL BENGALI/HINDI/ENGLISH TEMPLATE হুবহু, শুধু ব্রাঞ্চ ও রোগ অনুযায়ী
    // বসানো জায়গাগুলো ([BRANCH_NAME], [BRANCH_ADDRESS] ইত্যাদি) বদলায়।
    // ⛔ TK-এর লিখিত অনুমতি ছাড়া এই লেখা এক অক্ষরও বদলানো যাবে না।
    private fun buildEnquiryLockedTemplate(lang: String, branch: String, disease: String): String {
        val info = BranchCatalog.byName(branch)
        val extra = enquiryBranchExtra[info.id] ?: enquiryBranchExtra.getValue("kishanganj")
        val eduLink = diseaseEduLink(disease)
        val sb = StringBuilder()
        when (lang) {
            "bn" -> {
                sb.append("*THANK YOU FOR CONTACTING US*\n\n")
                sb.append("আমাদের চিকিৎসা পরিষেবা সম্পর্কে জানতে যোগাযোগ করার জন্য আপনাকে আন্তরিক ধন্যবাদ।\n\n")
                sb.append("আমাদের ক্লিনিকে রোগীর অবস্থা অনুযায়ী নিম্নলিখিত রোগের চিকিৎসা করা হয়—\n\n")
                sb.append("*Piles — অর্শ*\n*Fissure — ফিশার*\n*Fistula — ভগন্দর*\n*Hydrocele — একশিরা*\n*Gupt Rog — গুপ্তরোগ*\n\n")
                sb.append("প্রয়োজন অনুযায়ী আয়ুর্বেদিক ও ক্ষারসূত্র পদ্ধতিতে চিকিৎসার সুবিধা রয়েছে।\n\n")
                sb.append("*").append(extra.bnName).append(" চেম্বারের দিন:* ").append(extra.bnDays).append("\n")
                sb.append("*সময়:* সকাল 11টা থেকে বিকেল 4টা\n\n")
                sb.append("চেম্বারে আসার আগে অনুগ্রহ করে যোগাযোগ করে সময় নিশ্চিত করুন।\n\n")
                sb.append("*Regards,*\n*TK BISWAS*\nFounder & Consultant\n\n")
                sb.append("*").append(info.clinicName).append("*\n")
                sb.append("*").append(info.displayName).append(" Branch*\n")
                sb.append("📍 ").append(info.addressLine).append("\n")
                sb.append("*Contact Us:* ").append(info.phoneLine).append("\n\n")
                sb.append("*আপনার রোগ সম্পর্কে বিস্তারিত জানুন:*\n").append(eduLink).append("\n\n")
                sb.append("*Google Map:*\n").append(extra.mapLink).append("\n\n")
                sb.append("🔵 *Facebook Page:*\n").append(extra.facebookLink)
            }
            "hi" -> {
                sb.append("*THANK YOU FOR CONTACTING US*\n\n")
                sb.append("हमारी चिकित्सा सेवाओं के बारे में जानकारी लेने के लिए संपर्क करने हेतु आपका हार्दिक धन्यवाद।\n\n")
                sb.append("हमारे क्लिनिक में रोगी की स्थिति के अनुसार निम्नलिखित रोगों का उपचार किया जाता है—\n\n")
                sb.append("*Piles — बवासीर*\n*Fissure — फिशर*\n*Fistula — भगंदर*\n*Hydrocele — हाइड्रोसील*\n*Gupt Rog — गुप्त रोग*\n\n")
                sb.append("आवश्यकतानुसार आयुर्वेदिक एवं क्षारसूत्र पद्धति से उपचार की सुविधा उपलब्ध है।\n\n")
                sb.append("*").append(extra.hiName).append(" चैंबर के दिन:* ").append(extra.hiDays).append("\n")
                sb.append("*समय:* सुबह 11 बजे से शाम 4 बजे तक\n\n")
                sb.append("चैंबर आने से पहले कृपया संपर्क करके समय सुनिश्चित कर लें।\n\n")
                sb.append("*Regards,*\n*TK BISWAS*\nFounder & Consultant\n\n")
                sb.append("*").append(info.clinicName).append("*\n")
                sb.append("*").append(info.displayName).append(" Branch*\n")
                sb.append("📍 ").append(info.addressLine).append("\n")
                sb.append("*Contact Us:* ").append(info.phoneLine).append("\n\n")
                sb.append("*अपनी बीमारी के बारे में विस्तार से जानें:*\n").append(eduLink).append("\n\n")
                sb.append("*Google Map:*\n").append(extra.mapLink).append("\n\n")
                sb.append("🔵 *Facebook Page:*\n").append(extra.facebookLink)
            }
            else -> {
                sb.append("*THANK YOU FOR CONTACTING US*\n\n")
                sb.append("Thank you for contacting us to learn more about our medical services.\n\n")
                sb.append("Depending on the patient's condition, treatment is available at our clinic for the following conditions—\n\n")
                sb.append("*Piles — Haemorrhoids*\n*Fissure — Anal Fissure*\n*Fistula — Anal Fistula*\n*Hydrocele — Hydrocele*\n*Gupt Rog — Private Health Conditions*\n\n")
                sb.append("Ayurvedic treatment and Kshar Sutra therapy are available where appropriate.\n\n")
                sb.append("*").append(info.displayName).append(" Chamber Days:* ").append(extra.enDays).append("\n")
                sb.append("*Time:* 11:00 AM to 4:00 PM\n\n")
                sb.append("Please contact us before visiting to confirm your appointment time.\n\n")
                sb.append("*Regards,*\n*TK BISWAS*\nFounder & Consultant\n\n")
                sb.append("*").append(info.clinicName).append("*\n")
                sb.append("*").append(info.displayName).append(" Branch*\n")
                sb.append("📍 ").append(info.addressLine).append("\n")
                sb.append("*Contact Us:* ").append(info.phoneLine).append("\n\n")
                sb.append("*Learn more about your condition:*\n").append(eduLink).append("\n\n")
                sb.append("*Google Map:*\n").append(extra.mapLink).append("\n\n")
                sb.append("🔵 *Facebook Page:*\n").append(extra.facebookLink)
            }
        }
        return sb.toString()
    }

    // 🔒 TK-নির্দেশ (31.07.2026): "স্টাফ যখন চুজ করবে, আগে তাকে দেখাবে বার্তা
    // সে বাংলাতে/হিন্দিতে/ইংরাজীতে পাঠাতে চাইছে — পপ-আপ প্রফেশনাল লুক হতে
    // হবে।" ⛔ শুধুমাত্র Enquiry বার্তায় ব্যবহার হয় — বাকি ১১ ধরনের বার্তা
    // (Registration/Bill/Payment ইত্যাদি) এই ফাংশন ছোঁয় না, `show()` আগের
    // মতোই তিন-ভাষা-একসাথে পাঠায়।
    private fun showLanguagePicker(activity: Activity, onPicked: (String) -> Unit, onCancelled: () -> Unit) {
        val d = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        var picked = false
        val root = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = 20f * d
            }
        }
        root.addView(android.widget.TextView(activity).apply {
            text = "🌐  Choose Message Language"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#145A32"))
            setPadding(dp(18), dp(16), dp(18), dp(16))
        })
        root.addView(android.widget.TextView(activity).apply {
            text = "Which language should this patient's message be sent in?"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#3C4859"))
            setPadding(dp(18), dp(14), dp(18), dp(6))
        })
        lateinit var dlg: AlertDialog
        fun optionRow(label: String, sub: String, lang: String) =
            android.widget.LinearLayout(activity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                isClickable = true
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#F7FAFC"))
                    cornerRadius = 12f * d
                    setStroke(dp(1), android.graphics.Color.parseColor("#D8E1EC"))
                }
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dp(18), dp(6), dp(18), 0)
                layoutParams = lp
                setPadding(dp(14), dp(12), dp(14), dp(12))
                addView(android.widget.TextView(activity).apply {
                    text = label
                    textSize = 14.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                })
                addView(android.widget.TextView(activity).apply {
                    text = sub
                    textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#6B7A90"))
                })
                setOnClickListener {
                    picked = true
                    dlg.dismiss()
                    onPicked(lang)
                }
            }
        root.addView(optionRow("Bengali", "বাংলায় বার্তা পাঠান", "bn"))
        root.addView(optionRow("Hindi", "हिंदी में संदेश भेजें", "hi"))
        root.addView(optionRow("English", "Send message in English", "en"))
        root.addView(android.widget.TextView(activity).apply {
            text = "Cancel"
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#3C4859"))
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
            )
            lp.setMargins(dp(18), dp(14), dp(18), dp(16))
            layoutParams = lp
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E4E8EE"))
                cornerRadius = 12f * d
            }
            isClickable = true
            // 🔴🔴🔒 B514 (06.08.2026, TK-রিপোর্ট — "সেভের পরে ফর্ম ফাঁকা
            // হচ্ছে না") — আসল কারণ পাওয়া গেছে: এই "Cancel" বোতাম আগে
            // শুধু `dlg.dismiss()` ডাকত, `onCancelled()` (যেটা শেষে
            // `clearForm()` চালায়) কখনো ডাকা হতো না — Android-এ
            // `dismiss()` আর সিস্টেম "cancel" ইভেন্ট (`setOnCancelListener`)
            // আলাদা, dismiss() করলে cancel-listener চলে না। তাই এই
            // পপ-আপে "Cancel" চাপলেই ফর্ম চিরকাল ভরা থেকে যেত। এখন
            // "picked" true করে সরাসরি `onCancelled()` ডাকা হয়, তারপর
            // dismiss — ভাষা বাছা/Cancel দুটো পথেই ফর্ম নিশ্চিতভাবে
            // ফাঁকা হবে।
            setOnClickListener {
                if (!picked) { picked = true; onCancelled() }
                dlg.dismiss()
            }
        })
        dlg = AlertDialog.Builder(activity).setView(root).create()
        dlg.setOnCancelListener { if (!picked) onCancelled() }
        dlg.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
        try { NoBengali.installDialog(dlg) } catch (_: Throwable) { }
    }

    /**
     * শুধু Enquiry-এর প্রথম বার্তার জন্য — নতুন FINAL LOCK টেমপ্লেট।
     * আগে ভাষা-বাছাই পপ-আপ, তারপর সেই ভাষার লকড টেমপ্লেট প্রফেশনাল
     * Send-বাক্সে (একই `presentSendBox`, WhatsApp-only) দেখানো হয়।
     * [onClosed] সবসময় ডাকা হয় (পাঠান/Later/Cancel — যাই হোক না কেন),
     * তাই আগের ফ্লো (স্ক্রিন বন্ধ হওয়া) কখনো আটকায় না।
     */
    fun showEnquiryMessage(
        activity: Activity, branch: String, name: String, mobile: String,
        disease: String, onClosed: (() -> Unit)? = null
    ) {
        val digits = tenDigits(mobile)
        if (activity.isFinishing || activity.isDestroyed || digits.length != 10) {
            onClosed?.invoke(); return
        }
        showLanguagePicker(
            activity = activity,
            onPicked = { lang ->
                val text = try {
                    // 🔒 B275 (02.08.2026, TK-এর স্পষ্ট নির্দেশ — "তারা চিহ্ন
                    // আমরাও যেন না দেখি, যাকে পাঠানো হবে তারাও যেন না দেখে"):
                    // এই বার্তার লেখায় WhatsApp বোল্ডের জন্য তারা-চিহ্ন (*টেক্সট*)
                    // ছিল — WhatsApp-এ এগুলো মোটা অক্ষরে দেখা যেত, তাই তারা-চিহ্ন
                    // চোখে পড়ত না; কিন্তু SMS-এ (আর প্রিভিউতেও) হুবহু তারা-চিহ্নসহ
                    // দেখাত। TK চেয়েছেন কোথাও কেউ তারা-চিহ্ন না দেখুক — তাই এখানে,
                    // যে একটাই জায়গা থেকে প্রিভিউ+WhatsApp+SMS তিনটেই এই টেক্সট
                    // পায়, তারা-চিহ্ন সরিয়ে দেওয়া হলো। ⛔ বাকি কোনো লেখা/শব্দ এক
                    // অক্ষরও বদলায়নি — শুধু "*" অক্ষরটা বাদ।
                    buildEnquiryLockedTemplate(lang, branch, disease).replace("*", "")
                } catch (_: Throwable) {
                    onClosed?.invoke(); return@showLanguagePicker
                }
                // 🔴🔒 V504 (TK-এর অনুমোদন ২১.০৮.২০২৬) — বাংলা-বন্ধ ফোনে
                //    নমুনায় ভাঙা বাংলার বদলে সম্পূর্ণ ইংরেজি লেখা দেখানো হয়।
                //    ⛔ রোগীর কাছে যা যায় তা এক অক্ষরও বদলায় না।
                val previewEn = try {
                    buildEnquiryLockedTemplate("en", branch, disease).replace("*", "")
                } catch (_: Throwable) { null }
                presentSendBox(activity, branch, name, digits, text, text, whatsAppOnly = true, onClosed = onClosed, logKind = "ENQUIRY", previewEn = previewEn)
            },
            onCancelled = { onClosed?.invoke() }
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔒🔒 FIRST_VISIT_APPOINTMENT_MESSAGE_FINAL_LOCK_2026-08-01_1218_IST
    // TK BISWAS-এর VERIFIED LIVE-TEST নির্দেশ (01.08.2026): Enquiry ব্যক্তি
    // (এখনো ক্লিনিকে একবারও আসেননি) "আসার তারিখ মনে করিয়ে দিন" চাপলে
    // কখনোই "NEXT VISIT SCHEDULED" যাবে না — যাবে এই First Visit বার্তা।
    // Heading: "FIRST VISIT APPOINTMENT CONFIRMED"।
    // মূল বক্তব্য: "আপনার অ্যাপয়েন্টমেন্ট সফলভাবে নিশ্চিত করা হয়েছে।"
    // ⛔ TK-এর লিখিত অনুমতি ছাড়া এই লেখা/heading এক অক্ষরও বদলানো যাবে না।
    // ⛔ Visit/Patient-এর `Kind.VISIT_DATE` ("NEXT VISIT SCHEDULED") এই কোড
    //    এক অক্ষরও ছোঁয় না — সেটা আগের মতোই আলাদা পথে যায়।
    // ⛔ ভাষা-বাছাই (`showLanguagePicker`) ও Send-বাক্স (`presentSendBox`)
    //    আগে থেকে থাকা ফাংশন — শুধু পুনর্ব্যবহার, নতুন করে কিছু বানানো হয়নি।
    // ══════════════════════════════════════════════════════════════════════
    private fun buildFirstVisitAppointment(
        lang: String, branch: String, name: String, patientId: String, dateText: String
    ): String {
        val info = BranchCatalog.byName(branch)
        val who = name.ifBlank { "Patient" }
        val sb = StringBuilder()
        // ক্লিনিকের নাম-ঠিকানা একবারই উপরে (build()-এর মতোই একই উৎস BranchCatalog)।
        sb.append(info.clinicName).append("\n").append(info.addressLine).append("\n\n")
        when (lang) {
            "bn" -> {
                sb.append("FIRST VISIT APPOINTMENT CONFIRMED\n\n")
                sb.append("প্রিয় ").append(who).append(",\n\n")
                sb.append("আপনার অ্যাপয়েন্টমেন্ট সফলভাবে নিশ্চিত করা হয়েছে।\n\n")
                if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                if (dateText.isNotBlank()) sb.append("⏰ Appointment Date: ").append(dateText).append("\n")
                sb.append("Visiting Time: সকাল 11টা থেকে বিকেল 4টা\n\n")
                sb.append("নির্ধারিত তারিখে ক্লিনিকে উপস্থিত হবেন।")
            }
            "hi" -> {
                sb.append("FIRST VISIT APPOINTMENT CONFIRMED\n\n")
                sb.append("प्रिय ").append(who).append(",\n\n")
                sb.append("आपका अपॉइंटमेंट सफलतापूर्वक निश्चित कर दिया गया है।\n\n")
                if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                if (dateText.isNotBlank()) sb.append("⏰ Appointment Date: ").append(dateText).append("\n")
                sb.append("Visiting Time: सुबह 11 बजे से शाम 4 बजे तक\n\n")
                sb.append("तय तारीख पर क्लिनिक में उपस्थित रहें।")
            }
            else -> {
                sb.append("FIRST VISIT APPOINTMENT CONFIRMED\n\n")
                sb.append("Dear ").append(who).append(",\n\n")
                sb.append("Your appointment has been confirmed successfully.\n\n")
                if (patientId.isNotBlank()) sb.append("Patient ID: ").append(patientId).append("\n")
                if (dateText.isNotBlank()) sb.append("⏰ Appointment Date: ").append(dateText).append("\n")
                sb.append("Visiting Time: 11:00 AM to 4:00 PM\n\n")
                sb.append("Please be present at the clinic on the scheduled date.")
            }
        }
        sb.append("\n\nHelpline : ").append(info.phoneLine)
        return sb.toString()
    }

    /**
     * 🔒 FIRST_VISIT_APPOINTMENT_MESSAGE_FINAL_LOCK_2026-08-01_1218_IST
     * শুধু Enquiry-এর (এখনো ক্লিনিকে আসেননি) "আসার তারিখ মনে করিয়ে দিন" থেকে
     * ডাকা হয়। আগে ভাষা-বাছাই পপ-আপ (বাংলা/হিন্দি/English), তারপর **শুধু
     * নির্বাচিত একটি ভাষার** বার্তা প্রফেশনাল Send-বাক্সে (SMS/WhatsApp/Later)।
     * তিনটি ভাষা একসাথে যুক্ত হয় না। [onClosed] সব পথেই (Send/Later/Cancel)
     * ডাকা হয়, তাই আগের ফ্লো কখনো আটকায় না।
     */
    fun showFirstVisitAppointment(
        activity: Activity, branch: String, name: String, mobile: String,
        patientId: String = "", dateText: String = "", onClosed: (() -> Unit)? = null
    ) {
        val digits = tenDigits(mobile)
        if (activity.isFinishing || activity.isDestroyed || digits.length != 10) {
            onClosed?.invoke(); return
        }
        showLanguagePicker(
            activity = activity,
            onPicked = { lang ->
                val text = try {
                    buildFirstVisitAppointment(lang, branch, name, patientId, dateText)
                } catch (_: Throwable) {
                    onClosed?.invoke(); return@showLanguagePicker
                }
                // একই একক-ভাষার লেখা SMS ও WhatsApp দুটোতেই (আগের remind-date
                // আচরণের মতো দুটোই খোলা রইল); তিন ভাষা একসাথে নয়।
                // 🔴🔒 V504 — উপরের Enquiry-র মতোই একই কারণে ও একই নিয়মে।
                val previewEn = try {
                    buildFirstVisitAppointment("en", branch, name, patientId, dateText)
                } catch (_: Throwable) { null }
                presentSendBox(activity, branch, name, digits, text, text, whatsAppOnly = false, onClosed = onClosed, logKind = "FIRST_VISIT_APPOINTMENT", previewEn = previewEn)
            },
            onCancelled = { onClosed?.invoke() }
        )
    }
}
