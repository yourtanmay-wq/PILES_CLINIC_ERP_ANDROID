package com.tkbiswas.pilesclinic.native

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FollowUpItem(
    val id: String,
    val name: String,
    val mobile: String,
    val branch: String,
    val disease: String,
    val stage: String,
    val lastRemark: String,
    val nextFollow: String,
    val recordDate: String,
    // 🔒 খাতার সারি B65 (TK, 29.07.2026): কার্ডের সিরিয়াল নম্বর কোন ক্রমে বসবে
    // তা ঠিক করতে **সারিটা কবে তৈরি হয়েছিল** সেটাই ধরা হয় — কারণ `date` ঘরটা
    // প্রতিবার টাকা নিলে আজকের তারিখে বদলে যায় (কার্ড উপরে তোলার জন্য), তাই
    // ওটা ধরলে রোগীর নম্বর বারবার লাফাত। এটা না থাকলে (পুরনো সারি) আগের
    // মতোই `date` ধরা হয়। ⛔ ডিফল্ট "" — তাই কোথাও কিছু ভাঙে না।
    val createdAt: String = "",
    val callCount: Int = 0,
    // Patient-tab only (payment progress), blank/zero for Enquiry/Visit.
    val bill: Double,
    val paid: Double,
    // 🏷️🔒 V1508 (১৬.০৯.২০২৬, TK-নির্দেশ — MD ANARUL HOWK) — আসল বিল (ছাড়ের
    // আগে), শুধু কার্ডের "Bill" চিপে দেখানোর জন্য। ডিফল্ট 0.0 ⇒ যে সব caller
    // (যেমন FollowUpRepository.kt-এর নিজস্ব তালিকা) এটা এখনো পাঠায় না, তাদের
    // চিপ আগের মতোই `bill` দেখায় — কোনো বদল/ঝুঁকি নেই।
    val billBeforeDiscount: Double = 0.0,
    val patientId: String = "",
    val address: String = "",
    val age: String = "",
    val sex: String = "",
    // TK-REQUESTED PERFORMANCE FIX (2026-07-16): the followups row's mirrored
    // photo (written by PatientPhotoRepository.savePhoto) was already being
    // downloaded as part of the normal list fetch (select=*) but never read
    // client-side. Reading it here means the card can show the photo with
    // zero extra network calls, instead of a per-card live lookup.
    val photo: String = "",
    // TK-REQUESTED (2026-07-17): Follow-up Calendar day-popup shows when the
    // last remark was written, so "আগামীকাল আসবেন" (tomorrow) etc. isn't
    // ambiguous. This was already being saved on every remark update
    // (FollowUpRepository.updateRemark sets "updatedAt") but never read back
    // into this model before -- reading it here adds zero extra network
    // calls, same pattern as the "photo" field fix above.
    val updatedAt: String = "",
    // TK-REQUESTED ADDITION (2026-07-23): Official Time / Unexpected Time flag,
    // carried from the enquiry so the Enquiry-tab card can show a small badge.
    // Blank for older rows saved before this field existed (backfilled where
    // possible from the linked enquiry). Default "" -> no badge, nothing else
    // is affected.
    val timeType: String = "",
    // TK APPROVED (2026-07-28, proof 6): the card's single status line shows
    // "Last Call <date> <STAFF>" on the left. Both already arrive with the
    // normal list fetch -- "lastCallDate" is an existing followups column and
    // the staff name is the last entry of the existing "history" column -- so
    // reading them here costs ZERO extra network calls, the same pattern used
    // earlier for "photo" and "updatedAt".
    val lastCallDate: String = "",
    val lastCallBy: String = "",
    /* 🔵🔒 V543 (২২.০৮.২০২৬, TK-নির্দেশ: *"তারিখের সাথে সময় থাকবে"*)
       শেষ কলের **সময়**। `history`-র সবচেয়ে নতুন সারির `time` ঘর থেকে আসে —
       ওই ঘরটা ২০.০৭.২০২৬ থেকে প্রতিটা কলে জমা হচ্ছে (কোডে যাচাই করা)।
       ⛔ **নতুন কোনো ক্লাউড-অনুরোধ বা কলাম লাগেনি** — `history` এমনিতেই
          স্বাভাবিক তালিকার সাথে নামে।
       ⛔ পুরোনো (২০.০৭-এর আগের) সারিতে সময় নেই ⇒ ফাঁকা থাকে, তখন কার্ডে
          **আগের মতোই শুধু তারিখ** দেখায় — কিছু ভাঙে না। */
    val lastCallTime: String = "",
    // 🔒 TK-ORDER (30.07.2026): Branch/Disease-এর পাশের তৃতীয় ট্যাগে "RMP"
    // দেখাতে হবে যদি কোনো RMP/ডাক্তার এই রোগীকে রেফার করে থাকেন। এই ঘরটা
    // patients টেবিলে (PATIENT_COLS-এ) আগে থেকেই আনা হত, শুধু এই মডেলে
    // পড়া হত না। Enquiry-stage-এ (যেখানে এখনো রেফারেন্স জানা যায় না) খালিই
    // থাকে — কিছু ভাঙে না।
    val refDoctor: String = "",
    // 🔒 খাতার সারি B172 (TK, 30.07.2026): কার্ডের ট্যাগ-সারিতে রোগীর ঠিকানার
    // ছোট ট্যাগ (গ্রাম/পোস্ট/থানা/জেলা থেকে বেছে নেওয়া, বা স্টাফের নিজের
    // লেখা) — মোবাইল ধরে, তাই Enquiry/Visit/Patient তিন কার্ডেই এক থাকে।
    // `FollowUpRepository.fetchTab()`-এ বসে (নিজের ছোট `address_tags`
    // টেবিল থেকে, অথবা স্টাফ কিছু না বসালে `address` থেকে auto বেছে) —
    // এই মডেলের raw JSON parse-এ এটা নেই, তাই ডিফল্ট "" রাখা হলো।
    val addressTag: String = "",
    // 🔴 TK-নির্দেশ (02.08.2026): Refund Approved থাকলে ও নেট জমা ঠিক ₹0 হলে
    // এই রোগীকে Patient (Treatment) কার্ডে আর দেখানো হয় না, Draft-এর নতুন
    // "Refunded" ঘরে যায় (রেকর্ড অক্ষত, শুধু কার্ড সরে)। ডিফল্ট false — তাই
    // অন্য কোনো পুরনো constructor-call ভাঙে না।
    val hasApprovedRefund: Boolean = false,
    // 🔴 TK-নির্দেশ (02.08.2026, B302.1): Refund হওয়া রোগীকে "Refunded" ঘর
    // থেকে হাতে করে Patient কার্ডে ফেরত আনা হলে (নতুন টাকা জমা ছাড়াই) —
    // এটা true হয়, তখন উপরের hasApprovedRefund থাকলেও Patient ট্যাব থেকে
    // আর বাদ পড়ে না। ডিফল্ট false — পুরনো কোনো constructor-call ভাঙে না।
    val refundManuallyRestored: Boolean = false,
    /**
     * 🔵🔒 V518 (২২.০৮.২০২৬, TK-অনুমোদিত): followups সারির `refId` — অর্থাৎ
     * এই সারিটা **কোন রোগীর** (Patient/Visit/Treatment-এ এটা রোগীর row id;
     * Enquiry-তে এনকোয়ারির id)।
     * এক মোবাইলে একাধিক রোগী থাকলে (স্বামী/স্ত্রী) এটা দিয়েই কার তালিকা,
     * কার বিল, কার Patient ID — সব আলাদা রাখা হয়।
     * ⛔ ডিফল্ট ফাঁকা — তাই এই ক্লাস তৈরি করা পুরোনো কোনো জায়গা বদলাতে
     *    হয়নি, আর ফাঁকা থাকলে আচরণ **হুবহু আগের মতোই** (মোবাইল ধরে)।
     */
    val refId: String = "",
    /* 🆕🔒 V850 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — TK: "যেগুলো রেজিস্ট্রেশন
       করা হয়েছে সেখানে লিখতে হবে কত তারিখে রেজিস্ট্রেশন হয়েছে এবং কে
       রেজিস্ট্রেশন করেছিল"। ⛔ ডিফল্ট ফাঁকা ⇒ যে পর্দা এই দুটো ঘর ভরে না
       (Follow-up · Trash-প্রিভিউ) সেখানে কার্ড **হুবহু আগের মতোই** থাকে। */
    val regDate: String = "",
    val regBy: String = "",
    /* 📵🔒 V1206 (০৮.০৯.২০২৬, TK-রিপোর্ট, হুবহু): *"বার বার নো মোর কল দাবার পরেও
       আবার এই পেশেন্টের নাম কেনো শো করছে"*।
       🔬 **আসল কারণ (কোডে মেপে পাওয়া):** "No more calls" এতদিন শুধু `nextFollow`
          **ফাঁকা** করত। কিন্তু V1065-এর নিয়ম বলে — কল/রিমার্ক/টাকা বসলেই
          `nextFollow` ফাঁকা বা পুরনো হলে **আজকের দিন বসিয়ে দাও**। অর্থাৎ
          "ইচ্ছে করে থামানো" আর "কখনো বসানোই হয়নি" — দুটো **একরকম** ধরা হত,
          তাই পরের বার টাকা/রিমার্ক বসলেই নামটা আবার আজকের তালিকায় ফিরে আসত।
       ⇒ এখন থামানোটা আলাদা করে **মনে রাখা হয়** (`noMoreCalls`), তাই কোনো
         নিয়মই আর নিজে থেকে তারিখ বসাবে না।
       ⛔ ডিফল্ট false ⇒ পুরোনো সব সারি ও পুরোনো সব কোড হুবহু আগের মতোই। */
    val noMoreCalls: Boolean = false
)

object FollowUpModel {
    private fun s(row: JSONObject, key: String): String =
        if (row.isNull(key)) "" else row.optString(key, "")

    fun parse(row: JSONObject): FollowUpItem = FollowUpItem(
        refId = s(row, "refId"),
        id = s(row, "id"),
        name = s(row, "name"),
        mobile = s(row, "mobile"),
        branch = s(row, "branch"),
        disease = s(row, "disease"),
        stage = s(row, "stage"),
        lastRemark = s(row, "lastRemark"),
        nextFollow = s(row, "nextFollow"),
        recordDate = s(row, "date"),
        createdAt = s(row, "createdAt"),   // 🔒 খাতার সারি B65 — সিরিয়ালের স্থির ক্রম
        /* 📶🔴🔒 V1268 (০৯.০৯.২০২৬, TK-রিপোর্ট ছবিসহ: *"কল করেছে তাও ওয়াইফাই
           সিগনাল কেন ওঠেনি?"* — +918651838395, খাতায় "Enquiry Calls: 2",
           অথচ কার্ডের সিগন্যাল ফাঁকা)।

           🔬 **আসল কারণ (কোডে মেপে, আন্দাজ নয়):** সিগন্যাল-আইকনটা পড়ে
              `followups.callCount` ঘরটা, আর ওই ঘরটা **সব সময় বাড়ত না** —
              · V1149-এর (০৬.০৯.২০২৬) আগে গোনা বাড়ত শুধু কয়েকটা পর্দা থেকে;
                TK-র নিজের SQL-এ মেপে পাওয়া গিয়েছিল **১৫০২টার মধ্যে ১৩৯৩টা**
                সারিতে গোনা আর ইতিহাস দুই রকম, প্রায় সবগুলোতেই **গোনা কম**।
                V1149 নিয়মটা সারিয়েছে, কিন্তু **পুরনো সারিগুলো ০-ই রয়ে গেছে**।
              · আর এনকোয়ারি থেকে বানানো/মেরামত করা সারিতে `enquiries.callCount`
                (সব সময় ০) কপি হত ⇒ সেখানেও সিগন্যাল ফাঁকা।

           ⇒ **সমাধান — এই ফাইলেরই প্রমাণিত নিয়মে:** ঠিক নিচেই `lastCallDate`
             ঘরটা ফাঁকা হলে **ইতিহাস থেকে** নেওয়া হয় (আগের একই ধরনের TK-রিপোর্টে
             বসানো)। কল-গোনাতেও এখন সেটাই — জমানো সংখ্যা আর **ইতিহাসে গোনা
             কলের** মধ্যে যেটা **বেশি**, সেটাই দেখানো হয়।
           ⛔ ডেটাবেসে কিচ্ছু লেখা হয় না — শুধু **দেখানোর** সময় মিলিয়ে নেওয়া
              (কোনো বাড়তি ক্লাউড-কল নেই, `history` ঘরটা আগে থেকেই আসে)।
           ⛔ গোনার নিয়ম TK-র নিজের নিয়মেই: **দিনে একটাই** (খাতার সারি B53) ও
              সর্বোচ্চ ৫ — তাই সংখ্যা কখনো লাইভ নিয়মের চেয়ে বেশি হতে পারে না।
           ⛔ চিকিৎসার নোট (`src=treat`) ও অ্যাপের নিজের বসানো লেখা কল হিসেবে
              গোনা হয় না — টাইমলাইনের "Enquiry Calls" গোনার একই দর্শন। */
        callCount = maxOf(row.optInt("callCount", 0), callsFromHistory(row)).coerceIn(0, 5),
        noMoreCalls = row.optBoolean("noMoreCalls", false),   // 📵 V1206
        bill = row.optDouble("bill", 0.0),
        paid = row.optDouble("paid", 0.0),
        patientId = s(row, "patientId"),
        address = s(row, "address"),
        age = s(row, "age"),
        sex = s(row, "sex"),
        photo = s(row, "photo"),
        updatedAt = s(row, "updatedAt"),
        timeType = s(row, "timeType"),
        // 🚨 TK-REPORTED আবার (28.07.2026 ৮.০২ pm, ফটো-প্রুফসহ · খাতার সারি B50):
        // *"LAST CALL 19.07.2026 (JPE-CRP) — এখনও কেন এই সমস্যা রয়েছে?"*
        //
        // **আসল কারণ (কোড দেখে, আন্দাজ নয়):** `lastCallDate` ঘরটা **শুধু তখনই**
        // লেখা হত যখন স্টাফ ফোনের বোতামে চাপ দিয়ে কল লগ করতেন
        // (`logEnquiryCall`, বা `updateRemark(incrementCall = true)`)।
        // কিন্তু স্টাফ সাধারণত **রিমার্ক লেখেন** — তখন `history`-তে তারিখ ও
        // স্টাফের নাম দুটোই জমা হত, অথচ `lastCallDate` ফাঁকাই থেকে যেত।
        // ফলে কার্ডে `LAST CALL —` দেখাত, যদিও কল/রিমার্ক দুটোই হয়েছে।
        //
        // **সমাধান:** ঘরটা ফাঁকা থাকলে `history`-র **শেষ এন্ট্রির তারিখ** নেওয়া
        // হয়, আর স্টাফও ওই একই এন্ট্রি থেকে। `history` আগে থেকেই তালিকার
        // সঙ্গে আসে, তাই **এক পয়সাও বাড়তি নেট খরচ নেই**, আর পুরনো সব
        // রেকর্ডেও লাইনটা নিজে থেকেই ঠিক হয়ে যায়।
        // ⛔ ডেটাবেসে কিছুই লেখা হয় না — শুধু দেখানোর সময় হিসাব।
        lastCallDate = s(row, "lastCallDate").ifBlank { lastCallDateFromHistory(row) },
        lastCallBy = lastStaffFromHistory(row),
        // ⏰🔒 V931 — সময় এখন **ঠিক ওই তারিখের** সারি থেকে (উপরের বড় টীকা দেখুন)
        lastCallTime = lastCallTimeFromHistory(row, s(row, "lastCallDate").ifBlank { lastCallDateFromHistory(row) }),
        refDoctor = s(row, "refDoctor")
    )

    /** `history`-র শেষ এন্ট্রির তারিখ (না পেলে ফাঁকা)। */
    /** 🔵 V543: শেষ কলের সময় — `history`-র সবচেয়ে নতুন সারির `time` থেকে।
     *  ⛔ না পেলে ফাঁকা ⇒ কার্ডে আগের মতোই শুধু তারিখ। */
    /* ⏰🔒 V931 (৩১.০৮.২০২৬, TK ডেমো প্রুফ দেখে "হ্যাঁ পাশ, দুটোই বসিয়ে দিন") —
       TK-এর প্রশ্ন: *"LAST CALL এর তারিখ এবং সময় নেই কেন?"*
       **আসল কারণ (কোড ধরে যাচাই):** তারিখ আসত `lastCallDate` ঘর থেকে, কিন্তু
       সময় আসত `history`-র **সবচেয়ে নতুন** সারি থেকে — দুটো আলাদা জায়গা।
       তাই (ক) ওই তারিখের সারিতে সময় থাকলেও দেখাত না, আর (খ) অন্য দিনের সময়
       ভুল করে ওই তারিখের পাশে বসে যেত।
       ⇒ এখন সময়টা **ঠিক ওই তারিখের** সারি থেকেই নেওয়া হয়; কল করার পরে বসা
         সময়হীন সারি ("Next follow-up date updated") থাকলেও ওই দিনের আসল কলের
         সময়টা হারায় না।
       ⛔ ওই দিনে সময় কোথাও জমা না থাকলে ফাঁকা — বানিয়ে কিছু দেখানো হয় না।
       ⛔ তারিখ ফাঁকা হলে আগের মতোই সবচেয়ে নতুন সারির সময়।
       ⛔ ওয়েবের `wlv1LastCallTime()`-এর হুবহু যমজ (নিয়ম ৬.৬)। */
    private fun lastCallTimeFromHistory(row: JSONObject, forDate: String = ""): String = try {
        val arr = row.optJSONArray("history")
        if (arr == null || arr.length() == 0) "" else {
            val want = forDate.trim().take(10)
            var found = ""
            if (want.isNotBlank()) {
                for (i in arr.length() - 1 downTo 0) {
                    val entry = arr.optJSONObject(i) ?: continue
                    val d = if (entry.isNull("date")) "" else entry.optString("date", "")
                    if (d.trim().take(10) != want) continue
                    val t = if (entry.isNull("time")) "" else entry.optString("time", "").trim()
                    if (t.isNotBlank()) { found = t; break }
                }
            } else {
                for (i in arr.length() - 1 downTo 0) {
                    val entry = arr.optJSONObject(i) ?: continue
                    val d = if (entry.isNull("date")) "" else entry.optString("date", "")
                    if (d.isNotBlank()) {
                        found = if (entry.isNull("time")) "" else entry.optString("time", "")
                        break
                    }
                }
            }
            found
        }
    } catch (e: Exception) { "" }

    /** 📶 V1268 — `history`-তে কতগুলো **আলাদা দিনের** কল/রিমার্ক আছে।
     *  ⛔ TK-র "দিনে একবার" নিয়ম (B53) এখানেও, তাই এই সংখ্যা কখনো লাইভ
     *     গোনার চেয়ে বেশি হয় না। ⛔ কিছু গোলমাল হলে ০ ফেরে — তখন আগের
     *     জমানো সংখ্যাটাই দেখায়, অর্থাৎ আচরণ হুবহু আগের মতো। */
    private val AUTO_STUB_REMARKS = setOf(
        "registered patient / visit created",
        "treatment payment / advance received",
        "enquiry (syncing…)"
    )

    private fun callsFromHistory(row: JSONObject): Int = try {
        val arr = row.optJSONArray("history")
        if (arr == null || arr.length() == 0) 0 else {
            val days = HashSet<String>()
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val src = if (e.isNull("src")) "" else e.optString("src", "")
                if (src.equals("treat", ignoreCase = true)) continue
                val remark = (if (e.isNull("remark")) "" else e.optString("remark", "")).trim()
                if (remark.isBlank()) continue
                if (remark.lowercase() in AUTO_STUB_REMARKS) continue
                val d = (if (e.isNull("date")) "" else e.optString("date", "")).take(10)
                if (d.isNotBlank()) days.add(d)
            }
            days.size
        }
    } catch (_: Throwable) { 0 }

    private fun lastCallDateFromHistory(row: JSONObject): String = try {
        val arr = row.optJSONArray("history")
        if (arr == null || arr.length() == 0) "" else {
            var found = ""
            for (i in arr.length() - 1 downTo 0) {
                val entry = arr.optJSONObject(i) ?: continue
                val d = if (entry.isNull("date")) "" else entry.optString("date", "")
                if (d.isNotBlank()) { found = d.take(10); break }
            }
            found
        }
    } catch (e: Exception) { "" }

    /** Name of whoever wrote the most recent call/remark entry. The "history"
     *  column is already part of the normal fetch, so nothing extra is
     *  downloaded. Anything unexpected in there simply gives "" -- the line
     *  then shows the date alone and nothing breaks. */
    /**
     * 🚨 TK-REPORTED (28.07.2026, খাতার সারি B39): *"staff code নেই কেন? যে staff
     * লাস্ট কল করেছে সেই স্টাফের নাম হতে হবে।"*
     *
     * কলের সঙ্গে স্টাফের পরিচয় `history`-তে জমা হয়। সাধারণত ওখানে স্টাফের
     * **কোড** থাকে (যেমন `JPE-CRP`) — কিন্তু পুরনো কিছু সারিতে বা কম্পিউটার থেকে
     * করা এন্ট্রিতে **মোবাইল নম্বর** জমা থাকতে পারত, আর তখন কার্ডে নম্বরটাই
     * দেখাত (বা কিছুই দেখাত না)।
     *
     * 🔒 প্রজেক্টের লক করা নিয়ম: *"'By:' ঘরে সব সময় স্টাফের নাম দেখাবে,
     * কাঁচা মোবাইল নম্বর কখনো নয়"* — এখানেও সেটাই বসানো হলো। নম্বর পেলে
     * স্টাফ-তালিকা থেকে নামটা বের করে নেওয়া হয়।
     */
    private fun lastStaffFromHistory(row: JSONObject): String = try {
        val arr = row.optJSONArray("history")
        if (arr == null || arr.length() == 0) "" else {
            var found = ""
            for (i in arr.length() - 1 downTo 0) {
                val entry = arr.optJSONObject(i) ?: continue
                val staff = if (entry.isNull("staff")) "" else entry.optString("staff", "")
                if (staff.isNotBlank()) { found = staff; break }
            }
            prettyStaff(found)
        }
    } catch (e: Exception) { "" }

    /** নম্বর হলে স্টাফের নাম, নইলে যা আছে তাই। */
    fun prettyStaff(raw: String): String {
        val v = raw.trim()
        if (v.isBlank()) return ""
        val digits = v.filter { it.isDigit() }
        if (digits.length < 10) return v          // আগে থেকেই নাম/কোড — অক্ষত
        return try { StaffDirectory.findAccount(v)?.name?.takeIf { it.isNotBlank() } ?: v }
        catch (_: Throwable) { v }
    }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /* 📞🔒 V1403 (১২.০৯.২০২৬ রাত, TK-নির্দেশ ও অনুমোদন — তালিকা সারি ৫০৮, একই কথা
       ৮ বার) — **"যে তারিখে কল বাকি, সেই তারিখে বা তার পরে কল হয়ে গেলে সেটা আর
       বাকি নয়।"** TK-র CSV-তে মাপা: পেন্ডিং ১২টার ১২টাতেই `lastCallDate` ≥
       `nextFollow` — অর্থাৎ কল হয়ে গেছে, তবু "আজ বাকি"/"বকেয়া" দেখাত (V1065 কলের
       দিনেই `nextFollow`=আজ বসায়, আর গোনা শুধু তারিখ দেখত, কল হয়েছে কিনা দেখত না)।
       এখন কল-তালিকা · Home-এর ব্যানার · মনে-করানো নোটিফিকেশন — সবাই এই একটাই
       নিয়ম ডাকে (ওয়েবে `wlv1AlreadyCalled`, হুবহু যমজ)।
       ⛔ স্টাফ ভবিষ্যতের তারিখ বাছলে সেটা আগের মতোই আসবে (ওই তারিখে কল হয়নি বলে)।
       ⛔ `nextFollow` ফাঁকা হলে আগের মতোই গোনা হয় না; `noMoreCalls` আগের মতোই বাদ। */
    fun alreadyCalled(nextFollow: String, lastCallDate: String): Boolean {
        val nf = nextFollow.trim().take(10)
        val lc = lastCallDate.trim().take(10)
        return nf.isNotBlank() && lc.isNotBlank() && lc >= nf
    }

    /** আজ/বকেয়া কল সত্যিই বাকি কিনা — সব গোনা ও তালিকার একটাই নিয়ম (V1403)। */
    fun callPending(item: FollowUpItem, today: String = today()): Boolean =
        !item.noMoreCalls && item.nextFollow.isNotBlank() && item.nextFollow <= today &&
            !alreadyCalled(item.nextFollow, item.lastCallDate)

    /** Days between nextFollow and today: negative = overdue, 0 = today,
     * positive = days ahead. Null if no next-follow date is set. */
    fun daysUntil(nextFollow: String): Int? {
        if (nextFollow.isBlank()) return null
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val next = fmt.parse(nextFollow) ?: return null
            val now = fmt.parse(today()) ?: return null
            ((next.time - now.time) / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            null
        }
    }

    fun displayDate(iso: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(parsed!!)
    } catch (e: Exception) { iso }

    /** TK-REQUESTED (2026-07-17): the date a remark was written, shown next to
     * the remark text in the Follow-up Calendar day-popup. Blank in = blank
     * out (no crash, no fake date shown).
     *
     * 🔒 TK-এর স্থায়ী নিয়ম (29.07.2026 সকাল ১০.০০, খাতার সারি B76):
     * **তারিখ সবসময় `31.12.2026` (বা `31/12/2026`) ধাঁচে।** আগে এখানে
     * সংক্ষিপ্ত `dd MMM` (যেমন `16 Jul`) দেখানো হত — সেটা নিয়মের বাইরে ছিল,
     * তাই পুরো অ্যাপের এক নিয়মে আনা হলো। ⛔ আর কখনো সংক্ষিপ্ত ধাঁচে ফেরানো
     * যাবে না। */
    fun displayShort(isoDateTime: String): String {
        if (isoDateTime.isBlank()) return ""
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val parsed = fmt.parse(isoDateTime) ?: return ""
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(parsed)
        } catch (e: Exception) { "" }
    }
}
