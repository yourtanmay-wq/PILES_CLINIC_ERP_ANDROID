package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.print.BranchCatalog

/**
 * 🔒🔒 ডাক্তার / RMP-কে পাঠানো বার্তা — TK-এর নিজের হাতে ফাইনাল করা লেখা।
 * ==========================================================================
 * খাতার সারি **B157** · পুরো লেখা ও সব নিয়ম `00_TK_DOCTOR_BARTA_LOCKED.md`-এ।
 * TK ফাইনাল করেছেন 30.07.2026 সকাল ১০.১০ – ১০.২৫, এবং বলেছেন:
 * *"আপাতত লক করে রাখুন। যদি ভবিষ্যতে কখনো পরিবর্তন করার প্রয়োজন হয় আপনাকে বলব।"*
 *
 * ⛔ **TK-কে না জানিয়ে এই লেখার একটা শব্দও বদলানো যাবে না।**
 *
 * TK-এর ঠিক করে দেওয়া নিয়ম (প্রতিটা বার্তায় মানা হয়েছে):
 *  ১. **সহজ চলিত বাংলা** — ভারী/সাধু বাংলা নয়।
 *  ২. কিছু শব্দ ইংরেজিতেই থাকবে — Thank you · inform · update · follow-up ·
 *     referral income · registration · Kshar Sutra।
 *  ৩. **ডাক্তারের নাম ও পেশেন্টের নাম সবসময় ইংরেজিতে।**
 *  ৪. **সংখ্যা সবসময় ইংরেজিতে** (খাতার সারি B93) — মোবাইল · তারিখ · সময় · টাকা।
 *     তারিখ ডট দিয়ে: 30.07.2026।
 *  ৫. ⛔ **বার্তায় চিকিৎসকের নাম লেখা হবে না** — TK: *"ডাক্তার বিভিন্ন সময়
 *     চেঞ্জ হতে পারে, ক্লিনিক তো আর চেঞ্জ হবে না।"*
 *  ৬. ক্লিনিকের নাম · ঠিকানা · ফোন **ব্রাঞ্চ অনুযায়ী `BranchCatalog` থেকেই**
 *     বসে — কখনো হাতে বা আন্দাজে লেখা হয় না (খাতার গ্লোবাল রুল, 28.07.2026)।
 *  ৭. স্বাক্ষর সবসময় **TK BISWAS · Founder & Consultant**।
 *  ৮. ⛔ **কোনো মিথ্যা প্রতিশ্রুতি নয়** — TK: *"আমাদের ক্লিনিকে প্রতিদিন ডাক্তার
 *     বসে না তো, তাহলে মিথ্যা প্রতিশ্রুতি কেন দিচ্ছেন?"* তাই চেম্বারের দিন বা
 *     সময় নিয়ে কোনো দাবি কোনো বার্তায় নেই।
 *
 * 🔴 **কিশনগঞ্জ ব্রাঞ্চ বাদ** — TK: *"কিশানগঞ্জ ছাড়া সমস্ত ব্রাঞ্চের জন্য।"*
 *    কিশনগঞ্জের বার্তা TK পরে ঠিক করে দেবেন (হিন্দি হবে কিনা **নিজে থেকে ধরে
 *    নেওয়া যাবে না**), তাই ততদিন ওই ব্রাঞ্চে বার্তা পাঠানো বন্ধ থাকে।
 */
object DoctorMessage {

    /** কোন বার্তা — Action মেনুর চারটে অপশন। */
    enum class Kind { INTRO, ARRIVED, DETAILS, REFERRAL_PAID }

    /** কিশনগঞ্জে এখনো বার্তা ঠিক হয়নি, তাই আলাদা করে চেনা দরকার। */
    fun isKishanganj(branch: String): Boolean =
        BranchCatalog.byName(branch).id == "kishanganj"

    /** পর্দায় দেখানোর জন্য (ইংরেজি — পর্দার লেখা ইংরেজি রাখার নিয়ম)।
     *  ⛔ ব্রাঞ্চের নাম নিজে থেকে বসে — কোনো ব্রাঞ্চের বার্তা ঠিক না থাকলে
     *     স্টাফ যেন পরিষ্কার বোঝেন কোন ব্রাঞ্চের কথা বলা হচ্ছে। */
    fun blockedNote(branch: String = ""): String {
        val name = if (branch.isBlank()) "this" else BranchCatalog.byName(branch).displayName
        return "Message for $name branch is not finalised yet. Nothing has been sent."
    }


    // ── 🔒 TK-ORDER (30.07.2026 দুপুর ১২.৩৫ · খাতার সারি B159) ──────────
    // *"হিন্দি এবং বাংলা দুইরকম ভাষাতেই থাকবে, তবে স্টাফ সেখান থেকে পছন্দ
    //  করে নেবে যে কোন ভাষায় পাঠাতে চাইছে তারা।"*
    //
    // ⛔ **বাংলা লেখা এক অক্ষরও বদলানো হয়নি** — TK যেটা ফাইনাল করেছিলেন
    //    সেটাই `...Bn()` ফাংশনে অক্ষত আছে। হিন্দিটা ওরই হুবহু অনুবাদ।
    // ⛔ ভাষা বাছাই **শুধু কিশানগঞ্জে** দেখানো হয় (TK যেভাবে বলেছেন);
    //    বাকি ব্রাঞ্চে আগের মতোই বাংলা যায়।
    // ⛔ সংখ্যা (তারিখ · টাকা · মোবাইল) সবসময় ইংরেজিতে — হিন্দিতেও
    //    দেবনাগরী অঙ্ক ব্যবহার করা হয়নি (খাতার সারি B93)।

    /** কোন ভাষা — "hi" হলে হিন্দি, বাকি সব ক্ষেত্রে বাংলা। */
    private fun isHi(lang: String) = lang.trim().lowercase() == "hi"

    // ══════════════════════════════════════════════════════════════════════
    // 🔒🔒 RMP_POST_CALL_WHATSAPP_INTRO_FINAL_LOCK_2026-07-31_1054_IST
    // শুধুমাত্র Msg 1 (Intro)-এর জন্য। TK BISWAS-এর FINAL OWNER LOCK নোট
    // অনুযায়ী — এই লেখা/ফরম্যাট/লিংক-অর্ডার TK-এর স্পষ্ট অনুমতি ছাড়া কোনোদিন
    // বদলানো যাবে না। Msg 2/3/4 (arrived/details/referralPaid) ও তাদের
    // head()/foot()/headHi()/footHi() একটুও ছোঁয়া হয়নি।
    // ══════════════════════════════════════════════════════════════════════
    private data class IntroBranchExtra(
        val bnName: String, val hiName: String,
        val bnDays: String, val hiDays: String, val enDays: String,
        val mapLink: String, val facebookLink: String
    )

    // ⚠️ ধরে নেওয়া হয়েছে (TK-কে জানানো হলো): [ACTIVE_BRANCH] হিসেবে RMP
    // রেকর্ডের নিজস্ব `branch` ব্যবহার করা হলো (প্রজেক্টের লক করা branch-
    // isolation নিয়ম অনুযায়ী স্টাফ শুধু নিজের ব্রাঞ্চের RMP-ই দেখেন, তাই এটা
    // Logged-in Staff-এর Active Branch-এর সমতুল্য) — Doctor-এর Area কখনো
    // Branch হিসেবে ব্যবহার হয়নি।
    private val introBranchExtra: Map<String, IntroBranchExtra> = mapOf(
        "kishanganj" to IntroBranchExtra(
            bnName = "কিশানগঞ্জ", hiName = "किशनगंज",
            bnDays = "বুধবার থেকে শনিবার", hiDays = "बुधवार से शनिवार", enDays = "Wednesday to Saturday",
            mapLink = "https://maps.app.goo.gl/NWxduLdY6NKae1aj8",
            facebookLink = "https://www.facebook.com/share/1CuUNwf48e/"
        ),
        "jalpaiguri" to IntroBranchExtra(
            bnName = "জলপাইগুড়ি", hiName = "जलपाईगुड़ी",
            bnDays = "মঙ্গলবার ও শনিবার", hiDays = "मंगलवार एवं शनिवार", enDays = "Tuesday and Saturday",
            mapLink = "https://maps.app.goo.gl/YS6k4B8XD1NYaUDK7?g_st=ac",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        ),
        "cooch_behar" to IntroBranchExtra(
            bnName = "কোচবিহার", hiName = "कूचबिहार",
            bnDays = "সোমবার ও শুক্রবার", hiDays = "सोमवार एवं शुक्रवार", enDays = "Monday and Friday",
            mapLink = "https://maps.app.goo.gl/hR5YK4jpELetmQDQ8?g_st=ac",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        ),
        // 🔴 Falakata/Birpara: এই RMP লক-নোটে ভেরিফায়েড লিংক দেওয়া হয়নি
        // ("CURRENT VERIFIED ... LINK REQUIRED"), নোটের নিজের ১১ নং নিয়ম
        // অনুযায়ী কোডে আগে থেকে থাকা TK-verified লিংক পুনর্ব্যবহার হলো —
        // এই একই লিংক TK নিজে 31.07.2026-এ Enquiry ফিচারের জন্য পাঠিয়েছিলেন।
        "falakata" to IntroBranchExtra(
            bnName = "ফালাকাটা", hiName = "फालाकाटा",
            bnDays = "মঙ্গলবার ও বৃহস্পতিবার", hiDays = "मंगलवार एवं गुरुवार", enDays = "Tuesday and Thursday",
            mapLink = "https://maps.app.goo.gl/FdwYxUukwK9kTMUcA",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        ),
        "birpara" to IntroBranchExtra(
            bnName = "বীরপাড়া", hiName = "बीरपाड़ा",
            bnDays = "রবিবার ও বুধবার", hiDays = "रविवार एवं बुधवार", enDays = "Sunday and Wednesday",
            mapLink = "https://maps.app.goo.gl/euEW22kdnE21Fove6",
            facebookLink = "https://www.facebook.com/share/19HV5QLgnW/"
        )
    )

    private const val INTRO_WEBSITE = "https://maaayurvedpilesclinic.netlify.app"

    /** [DOCTOR_NAME] খালি হলে বার্তা তৈরিই হয় না — ডাক্তারের আসল নাম ছাড়া
     *  কোনো fallback (মোবাইল ইত্যাদি) এই বার্তায় বসে না, বাকি তিনটে বার্তার
     *  থেকে এইটা আলাদা (TK-এর 31.07.2026-এর FINAL LOCK নিয়ম)। */
    fun introDoctorNameMissing(doctorName: String): Boolean = doctorName.trim().isBlank()

    private fun introLockedTemplate(lang: String, branch: String, doctorName: String, doctorArea: String): String {
        val info = BranchCatalog.byName(branch)
        val extra = introBranchExtra[info.id] ?: introBranchExtra.getValue("kishanganj")
        val dr = doctorName.trim()
        val areaLine = if (doctorArea.trim().isBlank()) "" else doctorArea.trim() + "\n"
        val sb = StringBuilder()
        when (lang) {
            "bn" -> {
                sb.append("THANK YOU FOR YOUR VALUABLE TIME\n\n")
                sb.append("সম্মানীয় ডা. ").append(dr).append(" মহাশয়\n").append(areaLine).append("\n")
                sb.append("আজকের আলোচনার পর আমাদের ক্লিনিকের চিকিৎসা পরিষেবা সম্পর্কে সংক্ষিপ্ত তথ্য পাঠানো হলো।\n\n")
                sb.append("আমাদের ক্লিনিকে Piles (অর্শ), Fissure (ফিশার), Fistula (ভগন্দর), Hydrocele (একশিরা) ও Gupt Rog (গুপ্তরোগ)-এর বিশেষায়িত মূল্যায়ন, পরামর্শ ও চিকিৎসা প্রদান করা হয়।\n\n")
                sb.append("বিশেষভাবে যেসব রোগী দীর্ঘদিন ধরে মলদ্বারের সমস্যায় ভুগছেন, বিভিন্ন স্থানে চিকিৎসা নিয়েও কাঙ্ক্ষিত ফলাফল পাননি, বারবার একই সমস্যা ফিরে আসছে অথবা অপারেশন নিয়ে ভয় ও দ্বিধায় আছেন—তাঁদের রোগের অবস্থা বিস্তারিতভাবে মূল্যায়ন করে উপযুক্ত চিকিৎসার ব্যবস্থা করা হয়। উপযুক্ত ক্ষেত্রে বিনা অপারেশনে চিকিৎসার সুযোগ রয়েছে।\n\n")
                sb.append("আপনার চেম্বারে এ ধরনের কোনো রোগী এলে প্রয়োজনীয় মূল্যায়ন ও চিকিৎসার জন্য আমাদের ").append(extra.bnName).append(" শাখায় রেফার করতে পারেন। রোগী পাঠানোর আগে যোগাযোগ করলে অ্যাপয়েন্টমেন্ট ও প্রয়োজনীয় সমন্বয় করা হবে।\n\n")
                sb.append(extra.bnName).append(" চেম্বার: ").append(extra.bnDays).append("\n")
                sb.append("সময়: সকাল 11টা থেকে বিকেল 4টা\n\n")
                sb.append("Regards,\nTK BISWAS\nFounder & Consultant\n\n")
                sb.append(info.clinicName).append("\n")
                sb.append(info.displayName).append(" Branch\n")
                sb.append("📍 ").append(info.addressLine).append("\n")
                sb.append("Contact Us: +91 ").append(info.phoneLine).append("\n\n")
                sb.append("রোগ ও চিকিৎসা পরিষেবা সম্পর্কে বিস্তারিত জানুন:\n").append(INTRO_WEBSITE).append("\n\n")
                sb.append("Google Map:\n").append(extra.mapLink).append("\n\n")
                sb.append("🔵 Facebook Page:\n").append(extra.facebookLink)
            }
            "hi" -> {
                sb.append("THANK YOU FOR YOUR VALUABLE TIME\n\n")
                sb.append("सम्माननीय डॉ. ").append(dr).append(" जी\n").append(areaLine).append("\n")
                sb.append("आज की बातचीत के बाद हमारे क्लिनिक की चिकित्सा सेवाओं के बारे में संक्षिप्त जानकारी साझा की जा रही है।\n\n")
                sb.append("हमारे क्लिनिक में Piles (बवासीर), Fissure (फिशर), Fistula (भगंदर), Hydrocele (हाइड्रोसील) एवं Gupt Rog (गुप्त रोग) का विशेष मूल्यांकन, परामर्श एवं उपचार किया जाता है।\n\n")
                sb.append("विशेष रूप से ऐसे रोगी, जो लंबे समय से गुदा संबंधी समस्याओं से पीड़ित हैं, विभिन्न स्थानों पर उपचार कराने के बाद भी अपेक्षित परिणाम प्राप्त नहीं कर पाए हैं, जिनकी समस्या बार-बार लौट आती है अथवा जो ऑपरेशन को लेकर भय या दुविधा में हैं—उनकी स्थिति का विस्तार से मूल्यांकन करके उपयुक्त उपचार किया जाता है। उपयुक्त मामलों में बिना ऑपरेशन उपचार की सुविधा उपलब्ध है।\n\n")
                sb.append("आपके क्लिनिक में ऐसा कोई रोगी आए तो आवश्यक मूल्यांकन एवं उपचार के लिए उसे हमारी ").append(extra.hiName).append(" शाखा में रेफर कर सकते हैं। रोगी भेजने से पहले संपर्क करने पर अपॉइंटमेंट एवं आवश्यक समन्वय किया जाएगा।\n\n")
                sb.append(extra.hiName).append(" चैंबर: ").append(extra.hiDays).append("\n")
                sb.append("समय: सुबह 11 बजे से शाम 4 बजे तक\n\n")
                sb.append("Regards,\nTK BISWAS\nFounder & Consultant\n\n")
                sb.append(info.clinicName).append("\n")
                sb.append(info.displayName).append(" Branch\n")
                sb.append("📍 ").append(info.addressLine).append("\n")
                sb.append("Contact Us: +91 ").append(info.phoneLine).append("\n\n")
                sb.append("रोगों एवं हमारी चिकित्सा सेवाओं के बारे में विस्तार से जानें:\n").append(INTRO_WEBSITE).append("\n\n")
                sb.append("Google Map:\n").append(extra.mapLink).append("\n\n")
                sb.append("🔵 Facebook Page:\n").append(extra.facebookLink)
            }
            else -> {
                sb.append("THANK YOU FOR YOUR VALUABLE TIME\n\n")
                sb.append("Dear Dr. ").append(dr).append(",\n").append(areaLine).append("\n")
                sb.append("Following our discussion today, we are sharing a brief overview of our clinic's medical services.\n\n")
                sb.append("Our clinic provides specialized evaluation, consultation and treatment for Piles (Haemorrhoids), Anal Fissure, Anal Fistula, Hydrocele and Gupt Rog (Private Health Conditions).\n\n")
                sb.append("Patients who have been suffering from anorectal problems for a long time, have not achieved the expected results after receiving treatment elsewhere, experience recurrent symptoms, or feel anxious about surgery are thoroughly evaluated and offered treatment according to their clinical condition. Non-surgical treatment options are available in suitable cases.\n\n")
                sb.append("When such a patient visits your clinic, you may refer them to our ").append(info.displayName).append(" Branch for further evaluation and treatment. Please contact us before sending the patient so that the appointment and necessary coordination can be arranged.\n\n")
                sb.append(info.displayName).append(" Branch Chamber: ").append(extra.enDays).append("\n")
                sb.append("Time: 11:00 AM to 4:00 PM\n\n")
                sb.append("Regards,\nTK BISWAS\nFounder & Consultant\n\n")
                sb.append(info.clinicName).append("\n")
                sb.append(info.displayName).append(" Branch\n")
                sb.append("📍 ").append(info.addressLine).append("\n")
                sb.append("Contact Us: +91 ").append(info.phoneLine).append("\n\n")
                sb.append("Learn more about the conditions and our medical services:\n").append(INTRO_WEBSITE).append("\n\n")
                sb.append("Google Map:\n").append(extra.mapLink).append("\n\n")
                sb.append("🔵 Facebook Page:\n").append(extra.facebookLink)
            }
        }
        return sb.toString()
    }

    // 🔒 RMP_POST_CALL_WHATSAPP_INTRO_FINAL_LOCK_2026-07-31_1054_IST — Msg 1
    // এখন নতুন লকড টেমপ্লেট ব্যবহার করে (bn/hi/en তিন ভাষা, ডাক্তারের নাম +
    // এলাকা)। ⛔ পুরনো `introBn()`/`introHi()` ফাংশন দুটো মোছা হয়নি (রেকর্ডের
    // জন্য রাখা হলো) কিন্তু আর ডাকা হয় না — Msg 2/3/4 এই দুটো ছোঁয় না।
    // ⛔ signature বদলেছে (doctorMobile সরানো হয়েছে, doctorArea যোগ হয়েছে) —
    // কারণ নতুন নিয়মে ডাক্তারের নাম না থাকলে মোবাইল/কোনো fallback বসে না,
    // কল-সাইটে আগে থেকেই `introDoctorNameMissing()` দিয়ে আটকানো হয়।
    // ══════════════════════════════════════════════════════════════════════
    // 🔒🔒 STRICT_MESSAGE_ONLY_UPDATE — 31.07.2026 (TK-এর নতুন FINAL LOCK,
    // Msg 1 লকের উপরে) — Msg 2/3/4 এখন এই নতুন Common-Footer টেমপ্লেট
    // ব্যবহার করে। ⛔ **RMP Message 2–4 শুধু Bengali** (TK-এর স্পষ্ট নিয়ম) —
    // তাই এই তিনটে ফাংশনে আর `lang`/Hindi ডাকা হয় না। পুরনো
    // `arrivedHi()`/`detailsHi()`/`referralPaidHi()`/`head()`/`foot()`/
    // `headHi()`/`footHi()` মোছা হয়নি (রেকর্ডের জন্য, dead code) কিন্তু আর
    // ডাকা হয় না।
    // ══════════════════════════════════════════════════════════════════════
    private fun commonFooterBlock(branch: String): String {
        val info = BranchCatalog.byName(branch)
        return "Regards,\nTK BISWAS\nFounder & Consultant\n\n" +
            info.clinicName + "\n" + info.displayName + " Branch\n" +
            "📍 " + info.addressLine + "\n" +
            "Contact Us: " + info.phoneLine
    }

    fun intro(branch: String, doctorName: String, doctorArea: String, lang: String = "bn"): String =
        introLockedTemplate(lang, branch, doctorName, doctorArea)

    /**
     * Msg 2 — PATIENT ARRIVAL CONFIRMATION।
     * 🔴🔴 TK-FIX (01.08.2026): এই ফাংশনটা আগে TK-এর লক করা লেখা (উপরে
     * `00_TK_DOCTOR_BARTA_LOCKED.md`) ব্যবহার করত না — নিজস্ব আলাদা (লক
     * না-করা) লেখা পাঠাচ্ছিল, আর ভাষা বাছাই করলেও সবসময় বাংলাই যেত।
     * এখন লক করা `arrivedBn/arrivedHi/arrivedEn` (patientId param রাখা
     * হয়েছে সিগনেচার না ভাঙার জন্য, লক করা লেখায় Patient ID নেই বলে
     * ব্যবহার হয় না)।
     */
    fun arrived(branch: String, doctorName: String, doctorArea: String,
                patientName: String, patientId: String, visitDate: String, lang: String = "bn"): String {
        return when {
            isHi(lang) -> arrivedHi(branch, doctorName, doctorArea, patientName, patientId, visitDate)
            lang.trim().lowercase() == "en" -> arrivedEn(branch, doctorName, doctorArea, patientName, patientId, visitDate)
            else -> arrivedBn(branch, doctorName, doctorArea, patientName, patientId, visitDate)
        }
    }

    /**
     * Msg 3 — PATIENT TREATMENT UPDATE।
     * 🔴🔴 TK-FIX (01.08.2026): আগে TK-এর লক করা লেখার বদলে নিজস্ব আলাদা
     * (Treatment Status/Blood Test Advice শব্দে) লেখা যেত, ভাষা বাছাই কাজ
     * করত না। এখন লক করা `detailsBn/detailsHi/detailsEn` — treatment/
     * bloodTest/nextVisit এখানে স্টাফের বেছে দেওয়া টেক্সট হিসেবেই আসে
     * (DoctorVisitActivity-তে `DoctorMessage.wordDone/wordGiven` দিয়ে
     * আগে থেকেই ভাষা-উপযোগী শব্দ বসানো হয়, তাই এই ফাংশন বদলাতে হয়নি)।
     */
    fun details(branch: String, doctorName: String, doctorArea: String,
                patientName: String, patientId: String, visitDate: String,
                treatmentStarted: Boolean, bloodTestGiven: Boolean, nextVisitDate: String, lang: String = "bn"): String {
        val nv = nextVisitDate.trim().ifBlank { if (isHi(lang)) "तय नहीं" else if (lang.trim().lowercase() == "en") "NOT SCHEDULED" else "নির্ধারিত হয়নি" }
        val treatment = if (treatmentStarted) wordDone(lang, true) else wordDone(lang, false)
        val bloodTest = if (bloodTestGiven) wordGiven(lang, true) else wordGiven(lang, false)
        return when {
            isHi(lang) -> detailsHi(branch, doctorName, doctorArea, patientName, patientId, visitDate, treatment, bloodTest, nv)
            lang.trim().lowercase() == "en" -> detailsEn(branch, doctorName, doctorArea, patientName, patientId, visitDate,
                if (treatmentStarted) "Done" else "Not done", if (bloodTestGiven) "Given" else "Not given",
                nextVisitDate.trim().ifBlank { "NOT SCHEDULED" })
            else -> detailsBn(branch, doctorName, doctorArea, patientName, patientId, visitDate, treatment, bloodTest, nv)
        }
    }

    /** Msg 4 — REFERRAL PAYMENT CONFIRMATION। শুধু বাংলা।
     *  ✅ TK-এর অনুমতিতে (31.07.2026) — এখন Payment Mode ও Transaction/
     *  Reference No.-ও Saved রেকর্ড থেকে আসে (Add Referral Income ফর্মে
     *  নতুন দুটো ঘর যোগ হয়েছে)। পুরনো এন্ট্রি যেগুলোতে এই দুটো ঘর সেভ করার
     *  আগে থেকেই ছিল, সেগুলোর জন্য এখনও ফাঁকা-ঘর প্যাটার্ন (______) থাকে —
     *  Placeholder নয়, শুধু পুরনো ডেটা সত্যিই ফাঁকা তাই। */
    /**
     * Msg 4 — referral income পাঠানো হলো।
     * 🔴🔴 TK-FIX (01.08.2026): আগের মতোই — লক করা লেখার বদলে আলাদা লেখা
     * যেত, ভাষা বাছাই কাজ করত না। এখন লক করা `referralPaidBn/Hi/En`।
     * ⛔ Amount/Mode/Date/Reference No. — এই ঘরগুলো লক করা টেমপ্লেটে
     * সবসময় ফাঁকা-ঘর (______) থাকে (TK-এর নির্দেশ, স্টাফ নিজে ভরবেন),
     * তাই এখানে `amount/paymentDate/mode/referenceNo` প্যারামিটার নেওয়া
     * হলেও লক করা ফাংশনে পাঠানো হয় না — সিগনেচার শুধু পুরনো কল-সাইট
     * (যেখান থেকে Saved রেকর্ড থেকে মান আসে) না ভাঙার জন্য রাখা হয়েছে।
     */
    fun referralPaid(branch: String, doctorName: String, doctorArea: String,
                     patientName: String, patientId: String, visitDate: String,
                     amount: Double, paymentDate: String, mode: String, referenceNo: String, lang: String = "bn"): String {
        return when {
            isHi(lang) -> referralPaidHi(branch, doctorName, doctorArea, patientName, patientId, visitDate, amount, paymentDate, mode, referenceNo)
            lang.trim().lowercase() == "en" -> referralPaidEn(branch, doctorName, doctorArea, patientName, patientId, visitDate, amount, paymentDate, mode, referenceNo)
            else -> referralPaidBn(branch, doctorName, doctorArea, patientName, patientId, visitDate, amount, paymentDate, mode, referenceNo)
        }
    }

    /** বার্তা ৩-এর তিনটে ঘরের উত্তর ভাষা অনুযায়ী। */
    fun wordDone(lang: String, yes: Boolean) =
        if (isHi(lang)) (if (yes) "\u0915\u0930\u093e\u092f\u093e \u0917\u092f\u093e" else "\u0928\u0939\u0940\u0902 \u0915\u0930\u093e\u092f\u093e \u0917\u092f\u093e")
        else (if (yes) "\u0995\u09b0\u09be\u09a8\u09cb \u09b9\u09af\u09bc\u09c7\u099b\u09c7" else "\u0995\u09b0\u09be\u09a8\u09cb \u09b9\u09af\u09bc\u09a8\u09bf")

    fun wordGiven(lang: String, yes: Boolean) =
        if (isHi(lang)) (if (yes) "\u0926\u093f\u092f\u093e \u0917\u092f\u093e" else "\u0928\u0939\u0940\u0902 \u0926\u093f\u092f\u093e \u0917\u092f\u093e")
        else (if (yes) "\u09a6\u09c7\u0993\u09af\u09bc\u09be \u09b9\u09af\u09bc\u09c7\u099b\u09c7" else "\u09a6\u09c7\u0993\u09af\u09bc\u09be \u09b9\u09af\u09bc\u09a8\u09bf")

    fun wordNotGiven(lang: String) =
        if (isHi(lang)) "\u0928\u0939\u0940\u0902 \u0926\u093f\u092f\u093e \u0917\u092f\u093e" else "\u09a6\u09c7\u0993\u09af\u09bc\u09be \u09b9\u09af\u09bc\u09a8\u09bf"

    private fun head(branch: String): String {
        val b = BranchCatalog.byName(branch)
        return b.clinicName + "\n" + b.addressLine + "\nফোন: " + b.phoneLine
    }

    private fun foot(branch: String): String {
        val b = BranchCatalog.byName(branch)
        return "সবিনয়ে,\nTK BISWAS\nFounder & Consultant\n" + b.clinicName + " · " + b.displayName
    }

    /** নাম সবসময় ইংরেজিতে ও বড় হাতের অক্ষরে (প্রজেক্টের নিয়ম)। */
    private fun nameOf(raw: String, fallback: String): String {
        val t = raw.trim()
        return (if (t.isBlank()) fallback.trim() else t).uppercase()
    }

    /**
     * ✅ বার্তা ১ — পরিচয় ও অনুরোধ (স্টাফ ফোন করার পরে)।
     * TK ফাইনাল: 30.07.2026 সকাল ১০.১০।
     */
    private fun introBn(branch: String, doctorName: String, doctorMobile: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val sb = StringBuilder()
        sb.append(head(branch)).append("\n\n")
        sb.append("শ্রদ্ধেয় Dr. ").append(dr).append(",\n\n")
        sb.append("আজ আমাদের ক্লিনিকের পক্ষ থেকে আপনার সঙ্গে কথা হলো। ")
        sb.append("সময় দেওয়ার জন্য Thank you স্যার। আমাদের ক্লিনিকের পরিচয় আপনাকে জানাচ্ছি।\n\n")
        sb.append("আমাদের চিকিৎসা\n")
        sb.append("Piles · Fissure · Fistula · Hydrocele · Gupt Rog\n\n")
        sb.append("আমাদের বিশেষত্ব\n")
        sb.append("আয়ুর্বেদের Kshar Sutra পদ্ধতিতে বিনা অপারেশনে চিকিৎসা। ")
        sb.append("পেশেন্টকে ভর্তি রাখতে হয় না, খরচ সাধ্যের মধ্যে। ")
        sb.append("অভিজ্ঞ ডাক্তারের তত্ত্বাবধানে চিকিৎসা হয় এবং প্রতিটি পেশেন্টের সুস্থ হওয়া পর্যন্ত নিয়মিত follow-up রাখা হয় — ")
        sb.append("এই রোগের চিকিৎসায় এই অঞ্চলে আমরাই সবচেয়ে ভরসার নাম।\n\n")
        sb.append("Referral\n")
        sb.append("আপনার পাঠানো প্রতিটি পেশেন্টের জন্য আমরা প্রকৃত referral income সম্পূর্ণ হিসাব সহ, যথাসময়ে দিয়ে থাকি।\n\n")
        sb.append("অনুরোধ\n")
        sb.append("স্যার, আপনার কাছে সেই ধরনের পেশেন্ট থাকলে আমাদের ক্লিনিকে পাঠানোর ব্যবস্থা করলে আমরা কৃতজ্ঞ থাকব। ")
        sb.append("আপনার পাঠানো পেশেন্টের সম্পূর্ণ দায়িত্ব আমাদের।\n\n")
        sb.append(foot(branch))
        return sb.toString()
    }

    /**
     * ✅ বার্তা ২ — পেশেন্ট ক্লিনিকে এসেছেন, ডাক্তারকে inform করা।
     * TK ফাইনাল: 30.07.2026 সকাল ১০.১০।
     * ⛔ এখানে "চিকিৎসা শুরু হয়েছে" লেখা যাবে না — TK: *"পেশেন্ট এসেছে মানেই
     *    সে চিকিৎসা শুরু করেছে তা নয়।"* ওটা বার্তা ৩-এর কাজ।
     */
    private fun arrivedBn(branch: String, doctorName: String, doctorMobile: String,
                patientName: String, patientMobile: String, dateText: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val sb = StringBuilder()
        sb.append(head(branch)).append("\n\n")
        sb.append("শ্রদ্ধেয় Dr. ").append(dr).append(",\n\n")
        sb.append("আপনার পাঠানো পেশেন্ট আজ আমাদের ক্লিনিকে এসেছেন। আপনাকে সেটা inform করছি।\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Date — ").append(dateText).append("\n\n")
        sb.append("পেশেন্টের রেজিস্ট্রেশন কমপ্লিট হয়েছে। ")
        sb.append("ডাক্তারের চেকআপের পর চিকিৎসার update আপনাকে যথাসময়ে জানানো হবে।\n\n")
        sb.append("আপনার ভরসার জন্য Thank you স্যার।\n\n")
        sb.append(foot(branch))
        return sb.toString()
    }

    /**
     * ✅ বার্তা ৩ — পেশেন্টের ডিটেইলস (চিকিৎসা · ব্লাড টেস্ট · পরের তারিখ)।
     * TK ফাইনাল: 30.07.2026 সকাল ১০.২০।
     * ⛔ তিনটে ঘরের উত্তর **স্টাফ নিজে বেছে দেন** — অ্যাপ আন্দাজে কিছু বসায় না।
     */
    private fun detailsBn(branch: String, doctorName: String, doctorMobile: String,
                patientName: String, patientMobile: String, visitDate: String,
                treatment: String, bloodTest: String, nextVisit: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val sb = StringBuilder()
        sb.append(head(branch)).append("\n\n")
        sb.append("শ্রদ্ধেয় Dr. ").append(dr).append(",\n\n")
        sb.append("আপনার পাঠানো পেশেন্টের update আপনাকে জানানো হচ্ছে।\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Visit Date — ").append(visitDate).append("\n")
        sb.append("Treatment — ").append(treatment).append("\n")
        sb.append("Blood Test — ").append(bloodTest).append("\n")
        sb.append("Next Visit Date — ").append(nextVisit).append("\n\n")
        sb.append("পেশেন্টের প্রতি সম্পূর্ণ যত্ন নেওয়া হচ্ছে। ")
        sb.append("পরবর্তী follow-up-এর খবরও আপনাকে যথাসময়ে জানানো হবে।\n\n")
        sb.append("আপনার ভরসার জন্য Thank you স্যার।\n\n")
        sb.append(foot(branch))
        return sb.toString()
    }

    /**
     * ✅ বার্তা ৪ — referral income পাঠানো হলো।
     * TK ফাইনাল: 30.07.2026 সকাল ১০.২৫।
     * 🔒 **ফাঁকা ঘর ইচ্ছে করে রাখা** — TK: *"কিছু জায়গায় ব্লাং থাকবে যেগুলো
     *    স্টাফকে ফিলাপ করতে হবে।"* ⛔ অ্যাপ নিজে থেকে টাকার অঙ্ক বা তারিখ
     *    বসায় না।
     */
    /** 🔒 TK-APPROVED (31.07.2026): Saved রেকর্ড থাকলে আসল Amount/Mode/
     *  Date/Reference বসে, না থাকলে ফাঁকা-ঘর (______) — Cash হলে
     *  Reference "Not Applicable"। এই তিনটে সাহায্যকারী ফাংশন (bn/hi/en
     *  তিনটেতেই একই নিয়ম) দিয়ে ব্লাংক-বা-আসল-মান ঠিক হয়। */
    private fun referralAmountLine(amount: Double, blank: String) =
        if (amount > 0.0) "%,.0f".format(amount) else blank
    private fun referralRefLine(mode: String, referenceNo: String, blank: String): String {
        val isOnline = mode.trim().equals("Online", ignoreCase = true)
        return if (!isOnline) "Not Applicable" else referenceNo.trim().ifBlank { blank }
    }

    private fun referralPaidBn(branch: String, doctorName: String, doctorMobile: String,
                     patientName: String, patientMobile: String, visitDate: String,
                     amount: Double = 0.0, paymentDate: String = "", mode: String = "", referenceNo: String = ""): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val blank = "______"
        val sb = StringBuilder()
        sb.append(head(branch)).append("\n\n")
        sb.append("শ্রদ্ধেয় Dr. ").append(dr).append(",\n\n")
        sb.append("আপনি যে পেশেন্ট পাঠিয়েছিলেন, সেই পেশেন্টের referral income আপনাকে পাঠানো হলো।\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Visit Date — ").append(visitDate).append("\n")
        sb.append("Referral Income — Rs ").append(referralAmountLine(amount, blank)).append("\n")
        sb.append("Payment Mode — ").append(mode.trim().ifBlank { blank }).append(" (Cash / Online)\n")
        sb.append("Payment Date — ").append(paymentDate.trim().ifBlank { blank }).append("\n")
        sb.append("Reference No. — ").append(referralRefLine(mode, referenceNo, blank)).append("\n\n")
        sb.append("টাকা পেয়ে একবার জানিয়ে দিলে ভালো হয় স্যার। কোনো অসুবিধা হলে উপরের নম্বরে জানাবেন।\n\n")
        sb.append("আপনার সহযোগিতার জন্য Thank you স্যার।\n\n")
        sb.append(foot(branch))
        return sb.toString()
    }

    // ══════════════════════════════════════════════════════════════
    //  🇮🇳 হিন্দি রূপ — উপরের বাংলা লেখারই হুবহু অনুবাদ (খাতার সারি B159)
    //  ⛔ TK-এর অনুমতি ছাড়া বদলানো যাবে না। অঙ্ক সবসময় ইংরেজিতে।
    // ══════════════════════════════════════════════════════════════

    private fun headHi(branch: String): String {
        val b = BranchCatalog.byName(branch)
        return b.clinicName + "\n" + b.addressLine + "\nफ़ोन: " + b.phoneLine
    }

    private fun footHi(branch: String): String {
        val b = BranchCatalog.byName(branch)
        return "सादर,\nTK BISWAS\nFounder & Consultant\n" + b.clinicName + " · " + b.displayName
    }

    private fun introHi(branch: String, doctorName: String, doctorMobile: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val sb = StringBuilder()
        sb.append(headHi(branch)).append("\n\n")
        sb.append("आदरणीय Dr. ").append(dr).append(",\n\n")
        sb.append("आज हमारे क्लिनिक की ओर से आपसे बात हुई। ")
        sb.append("समय देने के लिए Thank you सर। हमारे क्लिनिक का परिचय आपको दे रहे हैं।\n\n")
        sb.append("हमारा इलाज\n")
        sb.append("Piles · Fissure · Fistula · Hydrocele · Gupt Rog\n\n")
        sb.append("हमारी विशेषता\n")
        sb.append("आयुर्वेद की Kshar Sutra पद्धति से बिना ऑपरेशन इलाज। ")
        sb.append("मरीज़ को भर्ती रखने की ज़रूरत नहीं, खर्च आपकी पहुँच में। ")
        sb.append("अनुभवी डॉक्टर की निगरानी में इलाज होता है और हर मरीज़ के ठीक होने तक नियमित follow-up रखा जाता है — ")
        sb.append("इस रोग के इलाज में इस क्षेत्र में हम ही सबसे भरोसेमंद नाम हैं।\n\n")
        sb.append("Referral\n")
        sb.append("आपके भेजे हर मरीज़ के लिए हम सही referral income पूरे हिसाब के साथ, समय पर देते हैं।\n\n")
        sb.append("निवेदन\n")
        sb.append("सर, आपके पास इस तरह के मरीज़ हों तो हमारे क्लिनिक में भेजने की व्यवस्था करें, हम आपके आभारी रहेंगे। ")
        sb.append("आपके भेजे मरीज़ की पूरी ज़िम्मेदारी हमारी है।\n\n")
        sb.append(footHi(branch))
        return sb.toString()
    }

    private fun arrivedHi(branch: String, doctorName: String, doctorMobile: String,
                          patientName: String, patientMobile: String, dateText: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val sb = StringBuilder()
        sb.append(headHi(branch)).append("\n\n")
        sb.append("आदरणीय Dr. ").append(dr).append(",\n\n")
        sb.append("आपके भेजे मरीज़ आज हमारे क्लिनिक में आए हैं। आपको यह inform कर रहे हैं।\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Date — ").append(dateText).append("\n\n")
        sb.append("मरीज़ का रजिस्ट्रेशन पूरा हो गया है। ")
        sb.append("डॉक्टर के चेकअप के बाद इलाज का update आपको समय पर बता दिया जाएगा।\n\n")
        sb.append("आपके भरोसे के लिए Thank you सर।\n\n")
        sb.append(footHi(branch))
        return sb.toString()
    }

    private fun detailsHi(branch: String, doctorName: String, doctorMobile: String,
                          patientName: String, patientMobile: String, visitDate: String,
                          treatment: String, bloodTest: String, nextVisit: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val sb = StringBuilder()
        sb.append(headHi(branch)).append("\n\n")
        sb.append("आदरणीय Dr. ").append(dr).append(",\n\n")
        sb.append("आपके भेजे मरीज़ का update आपको बताया जा रहा है।\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Visit Date — ").append(visitDate).append("\n")
        sb.append("Treatment — ").append(treatment).append("\n")
        sb.append("Blood Test — ").append(bloodTest).append("\n")
        sb.append("Next Visit Date — ").append(nextVisit).append("\n\n")
        sb.append("मरीज़ का पूरा ध्यान रखा जा रहा है। ")
        sb.append("अगले follow-up की जानकारी भी आपको समय पर दे दी जाएगी।\n\n")
        sb.append("आपके भरोसे के लिए Thank you सर।\n\n")
        sb.append(footHi(branch))
        return sb.toString()
    }

    private fun referralPaidHi(branch: String, doctorName: String, doctorMobile: String,
                               patientName: String, patientMobile: String, visitDate: String,
                               amount: Double = 0.0, paymentDate: String = "", mode: String = "", referenceNo: String = ""): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val blank = "______"
        val sb = StringBuilder()
        sb.append(headHi(branch)).append("\n\n")
        sb.append("आदरणीय Dr. ").append(dr).append(",\n\n")
        sb.append("आपने जो मरीज़ भेजा था, उस मरीज़ की referral income आपको भेज दी गई है।\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Visit Date — ").append(visitDate).append("\n")
        sb.append("Referral Income — Rs ").append(referralAmountLine(amount, blank)).append("\n")
        sb.append("Payment Mode — ").append(mode.trim().ifBlank { blank }).append(" (Cash / Online)\n")
        sb.append("Payment Date — ").append(paymentDate.trim().ifBlank { blank }).append("\n")
        sb.append("Reference No. — ").append(referralRefLine(mode, referenceNo, blank)).append("\n\n")
        sb.append("पैसा मिलने पर एक बार बता दें तो अच्छा रहेगा सर। कोई दिक्कत हो तो ऊपर दिए नंबर पर बताएँ।\n\n")
        sb.append("आपके सहयोग के लिए Thank you सर।\n\n")
        sb.append(footHi(branch))
        return sb.toString()
    }

    // ══════════════════════════════════════════════════════════════
    //  🇬🇧 ইংরেজি রূপ — TK-ORDER (01.08.2026): বার্তা ২·৩·৪-এও এখন
    //  বাংলা/হিন্দির মতোই ইংরেজি বাছাইয়ের সুযোগ। বাংলা/হিন্দি টেমপ্লেটের
    //  একই অর্থ, একই তথ্যের সারি (Patient Name/ID/Date ইত্যাদি) — কোনো
    //  নতুন তথ্য যোগ করা হয়নি, কোনো বাক্য বাদ দেওয়া হয়নি।
    // ══════════════════════════════════════════════════════════════

    private fun headEn(branch: String): String {
        val b = BranchCatalog.byName(branch)
        return b.clinicName + "\n" + b.addressLine + "\nPhone: " + b.phoneLine
    }

    private fun footEn(branch: String): String {
        val b = BranchCatalog.byName(branch)
        return "Regards,\nTK BISWAS\nFounder & Consultant\n" + b.clinicName + " · " + b.displayName
    }

    private fun arrivedEn(branch: String, doctorName: String, doctorMobile: String,
                          patientName: String, patientMobile: String, dateText: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val sb = StringBuilder()
        sb.append(headEn(branch)).append("\n\n")
        sb.append("Dear Dr. ").append(dr).append(",\n\n")
        sb.append("The patient you referred has visited our clinic today. Informing you accordingly.\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Date — ").append(dateText).append("\n\n")
        sb.append("The patient's registration has been completed. ")
        sb.append("Further treatment update will be shared with you after the doctor's checkup.\n\n")
        sb.append("Thank you for your trust, Sir.\n\n")
        sb.append(footEn(branch))
        return sb.toString()
    }

    private fun detailsEn(branch: String, doctorName: String, doctorMobile: String,
                          patientName: String, patientMobile: String, visitDate: String,
                          treatment: String, bloodTest: String, nextVisit: String): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val sb = StringBuilder()
        sb.append(headEn(branch)).append("\n\n")
        sb.append("Dear Dr. ").append(dr).append(",\n\n")
        sb.append("Here is the latest update on the patient you referred.\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Visit Date — ").append(visitDate).append("\n")
        sb.append("Treatment — ").append(treatment).append("\n")
        sb.append("Blood Test — ").append(bloodTest).append("\n")
        sb.append("Next Visit Date — ").append(nextVisit).append("\n\n")
        sb.append("The patient is receiving complete care. ")
        sb.append("The next follow-up update will also be shared with you on time.\n\n")
        sb.append("Thank you for your trust, Sir.\n\n")
        sb.append(footEn(branch))
        return sb.toString()
    }

    private fun referralPaidEn(branch: String, doctorName: String, doctorMobile: String,
                               patientName: String, patientMobile: String, visitDate: String,
                               amount: Double = 0.0, paymentDate: String = "", mode: String = "", referenceNo: String = ""): String {
        val dr = nameOf(doctorName, doctorMobile)
        val pt = nameOf(patientName, patientMobile)
        val blank = "______"
        val sb = StringBuilder()
        sb.append(headEn(branch)).append("\n\n")
        sb.append("Dear Dr. ").append(dr).append(",\n\n")
        sb.append("The referral income for the patient you sent has been paid to you.\n\n")
        sb.append("Patient Name — ").append(pt).append("\n")
        sb.append("Visit Date — ").append(visitDate).append("\n")
        sb.append("Referral Income — Rs ").append(referralAmountLine(amount, blank)).append("\n")
        sb.append("Payment Mode — ").append(mode.trim().ifBlank { blank }).append(" (Cash / Online)\n")
        sb.append("Payment Date — ").append(paymentDate.trim().ifBlank { blank }).append("\n")
        sb.append("Reference No. — ").append(referralRefLine(mode, referenceNo, blank)).append("\n\n")
        sb.append("Kindly confirm once you receive the payment, Sir. Contact us on the above number for any issue.\n\n")
        sb.append("Thank you for your cooperation, Sir.\n\n")
        sb.append(footEn(branch))
        return sb.toString()
    }
}
