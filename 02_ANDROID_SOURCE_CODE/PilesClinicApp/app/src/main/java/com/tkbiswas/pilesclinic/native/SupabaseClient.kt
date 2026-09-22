package com.tkbiswas.pilesclinic.native

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Shared Supabase REST helper for the native screens. Same project/keys the
 * WebView app.js (config.js) and CloudPasswordCheck.kt already use.
 */
object SupabaseClient {
    const val URL = "https://bcyeogjqtupbdyciqfmz.supabase.co"
    const val KEY = "sb_publishable_k_170-JGrdxmZ7rBrjCyTA_-ElK2XdZ"

    /* 🟢🔒 V1519 — payment/edit refresh correctness-এর জন্য নতুন লেখার একই
       timestamp helper। প্রজেক্টের লক-করা পুরনো নিয়ম অনুযায়ী **ফোনের local
       clock**-ই লেখা হয় এবং শেষে literal Z থাকে; timezone semantics বদলানো হয়নি। */
    fun localWriteStamp(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .format(java.util.Date())

    /* 🔴🔒 V1584 (১৭.০৯.২০২৬, TK-রিপোর্ট — AMIT GOLDAR-এর ব্যাকডেট-পেমেন্ট-
       অনুরোধ প্রতিবারই "Failed — check your connection" দেখাচ্ছিল, অথচ Home
       পর্দায় কখনো "to sync" আসেনি — মানে লেখাটা নেট পর্যন্ত পৌঁছানোরই আগে
       কোথাও আটকে যাচ্ছিল, কিন্তু কেন সেটা বলার কোনো উপায় ছিল না, কারণ
       `upsert()` ব্যর্থ হলে শুধু `false` ফেরত দেয় — আসল কারণটা (blocked/
       HTTP কোড/exception) ভিতরেই থেকে যেত।
       ⛔ এই একটা ভাগের ভেরিয়েবল যোগ করা হলো — শেষ ব্যর্থতার আসল কারণ এখানে
       লেখা থাকবে, `upsert()`-এর ফেরত-মান/আচরণ এক অক্ষরও বদলায়নি। একাধিক
       লেখা একসাথে চললে এটা অন্য কারো কারণে চাপা পড়তে পারে (সততার সাথে
       জানানো) — তাই এটা শুধু নির্ণয়ের সহায়ক, নিশ্চিত প্রমাণ নয়। */
    @Volatile var lastWriteFailReason: String = ""

    // 🔴🔒 V1563 (RLS-প্রস্তুতি ধাপ ৪, ১৭.০৯.২০২৬, TK-অনুমোদিত) — এই ফাইলের
    // প্রতিটা কল (patients/payments-সহ এই ফাইল যে ২১টা টেবিলে ব্যবহার হয় সবগুলোই
    // — কোড খুঁজে বার করা তালিকা, আন্দাজ নয়) এখন থেকে, সাইন-ইন করা থাকলে,
    // ব্যক্তির নিজের আসল Supabase টোকেন পাঠাবে (আগে সবসময় শুধু ভাগের anon KEY
    // যেত)। SQL-এ যাচাই করা হয়েছে (V1562 patients/payments, V1567 বাকি ১৯টা
    // টেবিলসহ সবগুলো) — `anon` আর `authenticated` role-এর অনুমতি **সব
    // ২১টাতেই** হুবহু এক, আর এই টেবিলগুলোতে RLS এখনো বন্ধই আছে — তাই এই বদলে
    // **আজ কার কী দেখা/লেখা যায় তার এক অক্ষরও বদলায় না**, শুধু ভবিষ্যতের
    // RLS-এর জন্য প্রস্তুতি। সাইন-ইন করা না থাকলে (বা টোকেনের মেয়াদ ফুরিয়ে
    // থাকলে) আগের মতোই anon KEY যায়, তাই ব্যর্থ হলে/সাইন-ইন না থাকলে কারো
    // কাজ কখনো আটকায় না।
    private fun authToken(): String {
        val m = com.tkbiswas.pilesclinic.modules.ModuleAuth
        return if (m.isSignedIn) (m.accessToken ?: KEY) else KEY
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        // TK-REPORTED (2026-07-20): on a very slow connection (e.g. 0.9 KB/s)
        // the response body trickles in continuously, so readTimeout never
        // fires and a large fetch could keep downloading for minutes -- the
        // loading spinner just span forever. callTimeout caps the TOTAL time
        // of EVERY request, so any call that can't finish in 25s is cancelled
        // and surfaces as a normal failure -- the spinner then always stops
        // (and cache-first shows the previous data instantly next time).
        .callTimeout(25, TimeUnit.SECONDS)
        // TK-REPORTED (2026-07-27, "slow internet" audit): OkHttp allows only
        // FIVE requests to the same server at once by default and silently
        // queues the rest. Several screens deliberately send their reads
        // together (Draft, Reports, Patient Timeline, and now the chamber
        // print's visit-number lookups) -- with the default cap those
        // "together" reads were still partly standing in a queue. Raised to a
        // modest 10 so those groups actually travel together.
        // This does NOT change how many cloud calls the app makes, so
        // Supabase's free-plan quota is completely unaffected -- only how many
        // may be in the air at the same moment. Kept deliberately small: a
        // phone on a weak signal does worse, not better, when flooded.
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 10
        })
        .build()

    /**
     * 🚨 TK'S POINT (2026-07-28): "একই ইন্টারনেট স্পিডেই তো আমরা UPI লেনদেন করি,
     * সেখানে কোনো অসুবিধা হচ্ছে না — তাহলে আমাদের অ্যাপে কেন?"
     *
     * READS are capped at 25 seconds on purpose (a huge list must never keep a
     * screen waiting). But that same 25-second cap was also cancelling WRITES.
     * On a 0.16-2 KB/s line a save can genuinely need longer, and when it was
     * cancelled the row was often ALREADY saved on the server -- only the reply
     * had not come back yet. The app then said "Failed", the staff did it
     * again, and money/remarks looked lost.
     *
     * Writes now get a full minute. Nothing waits on screen because of this:
     * every save is written on the phone first and shown at once; this client
     * is only for the quiet cloud copy that happens behind it.
     */
    private val writeHttp: OkHttpClient = http.newBuilder()
        .callTimeout(60, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * 🚨 TK'S POINT (2026-07-28): "আমি একজন সাধারণ ব্যবহারকারী হতে চাই... UPI-তে,
     * ফ্লিপকার্টে এই একই ইন্টারনেটে কোনো অসুবিধা হয় না, তাহলে আমাদের অ্যাপে কেন?"
     *
     * The patients and followups rows each carry the patient's PHOTO inside
     * them. Several screens were asking for "every column" of up to 5,000 of
     * those rows just to show a name, a number and an amount -- so every photo
     * in the clinic came down the line as well, every time. That, far more than
     * the connection itself, is what made lists crawl.
     *
     * These two lists name every column those screens actually read -- the
     * photo is simply not asked for. Nothing else changes: same table, same
     * rows, same order, same figures. Screens that DO show a photo (patient
     * photo, timeline) keep asking for it as before.
     */
    const val PATIENT_COLS_NO_PHOTO = "address,age,bill,branch,complaint,completeApprovedBy,completeRequestedBy,createdAt,createdBy,date,decision,diagnosis,discount,disease,doctorAdvice,doctorComplete,doctorFullNote,id,medicalHistory,mobile,name,occupation,patientId,previousCost,previousResult,previousTreatment,queue,queuedAt,refBy,refDoctor,refDoctorMobile,refundRestoredBy,registeredBy,registrationDate,sex,sinceWhen,stage,timeType,treatmentDuration,updatedAt,visitDate"

    // 🔒 সংশোধন (29.07.2026 দুপুর, খাতার সারি B105): এই তালিকাটা বানানো হয়েছিল
    // `PILES_CLINIC_DB_SETUP.sql` দেখে, আর ওই ফাইলটা **আসল ডেটাবেসের চেয়ে পুরনো**
    // (সারি B26-এ সেটা লেখাও আছে)। ফলে ছ'টা সত্যিকারের ঘর বাদ পড়ে গিয়েছিল —
    // `age · convertedPatientId · lastCallDate · patientId · sex · timeType`।
    // ⛔ `patientId` বাদ পড়া সবচেয়ে বিপজ্জনক ছিল: ব্রাঞ্চ যাচাইয়ে ফাঁকা patientId
    //    দেখলে সদ্য রেজিস্টার হওয়া রোগী তালিকা থেকে হারিয়ে যেতে পারত
    //    (২৭.০৭.২০২৬-এ ঠিক এই বিপদেই একবার কাজ ফিরিয়ে নিতে হয়েছিল)।
    // এখন এটা **TK-এর নিজের হাতে লাইভ ডেটাবেসে যাচাই করা** তালিকাটাই
    // (`FollowUpRepository.FOLLOWUP_COLS`) — অর্থাৎ **শুধু `photo` ছাড়া
    // followups টেবিলের প্রতিটা ঘর**।
    /* 🔴🔒 V820 (২৯.০৮.২০২৬, TK-নির্দেশে Supabase লগ **মেপে** পাওয়া সবচেয়ে বড় ফুটো) —
       Enquiry ট্যাব `enquiries` টেবিল থেকে `select=*` দিয়ে **৫০০০ সারি** টানত
       (লগে গত এক ঘণ্টায় ২৪ বার, chunked)। কোড পড়ে যাচাই করা হয়েছে — ওই
       সারিগুলো থেকে সত্যিই পড়া হয় **শুধু নিচের ঘরগুলো**।
       ⛔ বাদ পড়া ঘর: `stage` (কখনো পড়া হয় না — কোডে সবসময় "Inquiry" **লেখা**
          হয়), `updatedAt` · `appointmentDate` · `convertedPatientId` ·
          `convertedAt` (একবারও পড়া হয় না)।
       ⛔ `patientId` ইচ্ছে করে নেই — ঘরটা `enquiries` টেবিলে **নেই-ই**
          (schema-তে `convertedPatientId`)। আজও `row.s("patientId")` ফাঁকাই
          ফেরে, তাই আচরণ এক চুলও বদলায় না; বরং তালিকায় রাখলে পড়াটাই ব্যর্থ হত।
       ⛔ `order=updatedAt.desc.nullslast` আগের মতোই চলে — সাজানোর জন্য ঘরটা
          select-এ থাকা লাগে না। */
    const val ENQUIRY_COLS_INQUIRY_TAB =
        "address,branch,callCount,createdAt,createdBy,date,disease,id,mobile,name,nextFollow,receivedBy,remarks,status,timeType"

    const val FOLLOWUP_COLS_NO_PHOTO = "address,age,branch,callCount,convertedPatientId,createdAt,createdBy,date,disease,history,id,lastCallDate,lastRemark,mobile,name,nextFollow,patientId,refId,registrationDate,sex,stage,status,timeType,updatedAt,visitDate"

    /* 💸🔒 V1281 (০৯.০৯.২০২৬, TK-র অনুমতি — তালিকা সারি ৪০৫, ধাপ ১): Draft পর্দার
       followups-পড়া। `FOLLOWUP_COLS_NO_PHOTO`-র হুবহু তালিকা, শুধু **`history`
       বাদ** — ওই লম্বা লেখাটা Draft-এর কোনো হিসাবে লাগে না (কার্ডের LAST CALL-এর
       তারিখ `lastCallDate` ঘর থেকেই আসে)। TK-কে আগে জানানো সৎ সীমা: history না
       নামালে ওই লাইনে "কে করেছিলেন · কটায়" অংশটা ফাঁকা যাবে।
       ⛔ `history` ঘরটা ডেটাবেসে অটুট (TK-র লক করা নিয়ম) — শুধু এই একটা পড়ায় নামে না;
          Draft থেকে history-তে লেখার দুটো পথই আগে সেই একটা সারি আলাদা করে নামায়। */
    const val FOLLOWUP_COLS_DRAFT = "address,age,branch,callCount,convertedPatientId,createdAt,createdBy,date,disease,id,lastCallDate,lastRemark,mobile,name,nextFollow,patientId,refId,registrationDate,sex,stage,status,timeType,updatedAt,visitDate"

    /** 🔵🔒 V441 (19.08.2026, TK-অনুমোদিত — Draft egress): Draft-এর enquiry
     *  bucket বানাতে কোডে যাচাই করে শুধু এই ঘরগুলোই পড়া হয়। সব নাম active
     *  enquiries schema-তে আছে; filter/order/limit একদম আগের মতো। Narrow read
     *  ব্যর্থ হলে fetchListSlimOrNull-এর পুরনো full-row fallback অটুট। */
    const val ENQUIRY_COLS_DRAFT = "id,date,branch,name,mobile,disease,remarks,timeType,receivedBy,stage,nextFollow,createdBy,updatedAt,convertedPatientId"

    /**
     * 🟢🔒 B661 (15.08.2026, TK-অনুমোদিত · Egress-৩) — **শুধু চেম্বার হাজিরা বোর্ডের জন্য**।
     *
     * ওই বোর্ডে `followups`-এর পুরো ইতিহাস নামে (তারিখের সীমা নেই, limit 5000), আর
     * **২৫টা ঘর**সহ নামত — অথচ কোডে ওই তালিকা থেকে মাত্র **৭টা ঘর** পড়া হয়:
     *   `status` (ChamberAttendanceRepository:482) · `id` (:520, :797, :817) ·
     *   `nextFollow` (:539) · `mobile` (:790, :810) · `stage` (:791, :812) ·
     *   `lastRemark` (:794, :815) · `branch` (স্থানীয় pending মেলানোয়)।
     * বাকি ১৮টা ঘর — `history` · `address` · `disease` · `name` · `age` · `sex` ·
     * `callCount` · `date` · `visitDate` ইত্যাদি — কোথাও পড়াই হয় না, শুধু নেটে নামত।
     * (`history` ঘরটাই সবচেয়ে লম্বা লেখা।)
     *
     * ⛔ তারিখের সীমা **ইচ্ছে করে বসানো হয়নি** — আজ এসেছেন কিন্তু আজকের ফলোআপ সারি নেই,
     *    এমন রোগীর **Last Remark** ওই পুরনো সারি থেকেই আসে (:809-820)। সীমা বসালে
     *    চালু একটা সুবিধা নষ্ট হত।
     * ⛔ সারির সংখ্যা · ছাঁকনি · সাজানো · limit — কিচ্ছু বদলায়নি, শুধু ঘর কমল।
     * ⛔ সরু পড়া ব্যর্থ হলে অ্যাপ নিজেই সব ঘর চেয়ে নেয় (fetchListSlimOrNull-এর B446 নিয়ম)।
     */
    /* 🔴🔒 V814 — `lastRemarkAt` যোগ হলো: রিমার্কের কথাটা **কবে লেখা হলো**।
       চেম্বার বোর্ডের "আজকের Treatment Progress" পাহারা এই ঘরটাই দেখে,
       কারণ `updatedAt` রিমার্ক ছাড়া অন্য কাজেও আজকের হয়ে যায়।
       ⛔ একটা ছোট সময় (~৩০ বাইট) — Egress-এ প্রভাব নগণ্য। */
    const val FOLLOWUP_COLS_CHAMBER_BOARD = "branch,id,lastRemark,lastRemarkAt,mobile,nextFollow,stage,status,updatedAt"

    /** Everything the money lists actually read from a payment row.
     *  🔒 সংশোধন (29.07.2026, খাতার সারি B114): এই তালিকায় **`patientCode` ছিল না**,
     *  অথচ `PaymentModel.parsePaymentRow()` ওই ঘরটাই পড়ে Patient ID দেখানোর জন্য।
     *  কেউ এই তালিকা দিয়ে টাকার তালিকা নামালে **Patient ID ফাঁকা হয়ে যেত** —
     *  ঠিক খাতার সারি B109-এর সেই দোষটাই আবার হত। তাই ঘরটা যোগ করা হলো।
     *  ⛔ ঘর যোগ করায় কোনো তথ্য হারায় না, শুধু ফাঁকা হওয়ার ফাঁদটা বন্ধ হয়।
     *  🔴🔴🔒 V688 (২৫.০৮.২০২৬, নিজের যাচাইয়ে ধরা পড়া গুরুতর বাগ — V687-এর
     *  Chamber বোর্ড ফিক্স আসলে কখনোই কাজ করত না) — V687-এ Chamber বোর্ডের
     *  Treatment Progress-এর উৎস `payments.progress`-এ বদলানো হয়েছিল, কিন্তু
     *  Chamber বোর্ড ঠিক **এই তালিকা** (`PAYMENT_COLS_LIST`) দিয়েই payments
     *  আনে — আর তাতে `progress` ঘরটাই ছিল না! তাই `row.optString("progress")`
     *  সবসময় ফাঁকা ফিরত, ফিক্সটা নীরবে কিছুই করত না। এখন `progress` ঘরও
     *  এই তালিকায় যোগ করা হলো — খুবই ছোট লেখা (remarks-এর মতোই), Egress-এ
     *  চাপ পড়ে না। */
    const val PAYMENT_COLS_LIST = "id,patientId,patientCode,mobile,branch,name,amount,mode,cashAmount,onlineAmount,dailyEvents,payType,payLabel,paymentLabel,date,remarks,progress,receivedBy,createdBy,createdAt,updatedAt,refundApprovalStatus"

    private val jsonMedia = "application/json".toMediaType()

    /**
     * 🚨 TK'S ORDER (2026-07-28): "যে কোনো জায়গায় কাজ করতে গেলে অনেক দেরিতে
     * ওপেন হয়... একবারে গোড়া থেকে সমাধান করুন।"
     *
     * WHAT THE AUDIT FOUND (counted, not guessed): more than twenty screens ask
     * the cloud for EVERY COLUMN of up to 5,000 rows just to add up a few
     * numbers -- so the patient photos, the full doctor notes, the whole call
     * history all come down TK's 0.16-2.00 KB/s line every single time. That is
     * the root of the slowness, not the connection.
     *
     * WHAT THESE TWO DO: exactly the same read, from the same table, with the
     * same filter, limit and order -- only asking for the columns that screen
     * actually reads. Same rows, same order, same figures.
     *
     * 🔒 THE SAFETY NET (same proven pattern FollowUpRepository has used since
     * 27.07.2026): if a narrowed read ever fails while the column list is still
     * unproven for that table, the SAME read is done again asking for every
     * column. So a wrong column name can never empty a screen or a total -- the
     * worst case is exactly the old behaviour. Once a narrowed read has worked
     * even once, the list is proven for this run and a later failure can only be
     * the network, so no second request is spent on a slow line.
     */
    private val slimProven = java.util.Collections.synchronizedSet(HashSet<String>())

    /** Narrowed read. Returns null on a genuine failure, exactly like
     *  fetchListOrNull -- callers keep their own failure handling unchanged. */
    // 🔴🔴🔴 খাতার সারি B446 (TK-রিপোর্ট, ছবিসহ — একই তারিখে দুই ফোনে দুই
    // রকম পেমেন্ট তালিকা, Collection Summary ₹0 অথচ নিচে আসল পেমেন্ট
    // দেখা যাচ্ছে) — গভীরে খুঁজে একটা মূল, ব্যাপক বাগ পাওয়া গেছে, যা এই
    // প্রজেক্টের **১৯টা ফাইলে ব্যবহৃত এই একই শেয়ার্ড ফাংশনে** ছিল।
    // **আসল কারণ:** `slimProven`-এ একবার কোনো টেবিলের "slim" (কম কলাম)
    // পড়া সফল হয়ে গেলে, তারপর থেকে সেই টেবিলের **যেকোনো** slim-পড়া
    // ব্যর্থ হলে (নেট একটু ধীর/অস্থির হলেও) — এই ফাংশন **চুপচাপ `null`**
    // ফিরিয়ে দিত, পুরো-কলাম দিয়ে আবার চেষ্টা **করতই না**। কল করা কোড
    // (যেমন `PaymentRepository.fetchCollectionRange`) `null`-কে "খালি
    // তালিকা" (`?: JSONArray()`) ধরে নিত — তাই **নেট একটু আটকে গেলেই
    // "কোনো পেমেন্ট নেই" দেখাত, প্রকৃতপক্ষে ডেটা ছিল**। এটাই Collection
    // Summary ₹0 দেখানোর ও দুই ফোনে দুই রকম তালিকা দেখানোর (একজনের নেট
    // ভালো ছিল, অন্যজনের একটু আটকে গিয়েছিল) আসল কারণ।
    // **সমাধান:** slim-পড়া ব্যর্থ হলে এখন **সবসময়** পুরো-কলাম দিয়ে আবার
    // চেষ্টা হয় (আগের মতো "একবার প্রমাণিত হলে আর চেষ্টা না করা" শর্টকাট
    // সরানো হলো) — ব্যর্থতা সত্যিই নেট-এর সমস্যা হলে দ্বিতীয় চেষ্টাও
    // ব্যর্থ হতে পারে (তখনও honest `null`/খালি), কিন্তু slim-syntax-এর
    // কারণে ব্যর্থ হলে পুরো-কলাম চেষ্টায় ঠিকই ডেটা পাওয়া যাবে। ⛔ সফল
    // slim-পড়ার পথ (বেশিরভাগ সময়) এক অক্ষরও বদলায়নি — শুধু ব্যর্থতার
    // পথে আর চুপচাপ হাল ছাড়া হয় না।
    // 🔵 V405 (16.08.2026, TK-অনুমোদিত — Egress) — **B446 এক চুলও দুর্বল হয়নি।**
    //
    // সমস্যা যেটা ছিল: slim-পড়া ব্যর্থ হলেই সঙ্গে সঙ্গে **সব ঘর** (`select=*`)
    // চাওয়া হত — আর `patients`/`medical`/`followups`-এ "সব ঘর" মানে **রোগীর
    // base64 ছবি**। নেট একটু দুর্বল হলেই এই ~১৯টা জায়গায় চুপচাপ ছবি-সহ পুরো
    // সারি নামত, অথচ কোনো পর্দায় ওই ছবি দেখানোই হত না।
    //
    // যেটা বোঝা গেল: টেবিলটা একবার `slimProven`-এ উঠে গেলে **slim-syntax যে চলে
    // তা প্রমাণিত** — তাই পরের ব্যর্থতা প্রায় নিশ্চিতভাবে **নেটের** সমস্যা,
    // syntax-এর নয়। ওই অবস্থায় "সব ঘর" চাওয়ার কোনো কারণই নেই; একই সরু
    // অনুরোধটাই আরেকবার করলে হয়।
    //
    // ⛔ B446-এর গ্যারান্টি অটুট: সরু দ্বিতীয় চেষ্টাও ব্যর্থ হলে **আগের মতোই
    //    পুরো-কলাম চেষ্টা হয়** — অর্থাৎ "নেট আটকালে খালি তালিকা দেখানো"
    //    (Collection Summary ₹0) কখনো ফিরে আসতে পারে না।
    // ⛔ যে টেবিল এখনো প্রমাণিত নয়, তার পথ **হুবহু আগের মতোই** (সরু → পুরো)।
    // ⛔ সফল slim-পড়ার পথ (বেশিরভাগ সময়) এক অক্ষরও বদলায়নি।
    /* ═══════════════════════════════════════════════════════════════════════
       🔴🔴🔒 V794 (২৮.০৮.২০২৬, TK-নির্দেশে পূর্ণ Egress-যাচাইয়ের পরে) —
       **যে ঘরগুলো কেউ পড়েই না, সেগুলো আর নামানো হবে না।**

       TK: *"Supabase egress এর ঝুঁকি আর কোথায় কোথায় আছে … আন্দাজে কিছু করবেন
       না, যাচাই করে কাজ করবেন।"*

       ─── প্রমাণ (আন্দাজ নয়) ────────────────────────────────────────────────
       `medical.photos` ঘরে চেক-আপের before + during + after তিনটে ছবিই
       base64 হিসেবে জমা হয় (`DoctorCheckupActivity.kt:1392-1402`,
       ছবি ≈ ৫৫–১২০ KB করে ⇒ এক সারি ≈ ৩৬০ KB পর্যন্ত)।
       কিন্তু পুরো প্রকল্পে খুঁজে দেখা গেছে — **এই ঘরটা কেউ কখনো পড়েই না**
       (ফোনে `optString("photos")`/`s("photos")` একটাও নেই; ওয়েবেও `.photos`
       পড়া নেই)। শুধু লেখা হয়, পড়া হয় না।
       ⇒ অথচ পাঁচ জায়গায় ৫০০ সারি পর্যন্ত **ছবিসহ** নামত।

       ─── এখন ─────────────────────────────────────────────────────────────
       এই তালিকাটা `photos` **বাদ দিয়ে** বাকি সব ঘর চায় — তাই যারা এই সারি
       ব্যবহার করে (Checkup History · Timeline · Print Center) তাদের একটাও
       দরকারি ঘর হারায় না, শুধু না-পড়া ছবিগুলো আর নামে না।
       ⛔ `photos` লেখার কোড এক অক্ষরও বদলায়নি — ডেটাবেসে ছবি আগের মতোই জমা
          থাকে, ভবিষ্যতে দরকার হলে আলাদা করে ওই এক সারিটা পড়া যাবে।
       ⛔ সরু পড়া ব্যর্থ হলে আগের মতোই তিন-ধাপের fallback চলে
          (`fetchListSlim*`), তাই পর্দা কখনো ফাঁকা হবে না।
       ═══════════════════════════════════════════════════════════════════ */
    const val MEDICAL_COLS =
        "id,patientId,type,date,selected,days,details,nextFollow,diagnosis," +
        "decision,doctorFullNote,name,mobile,branch,createdBy,createdAt,updatedAt"

    /** 🔴🔒 V794 — রোগীর সারির **সব ঘর, শুধু `photo` বাদ**।
     *  যেসব জায়গায় ছবিটা পর্দায় দেখানো হয় **না** (যাচাই করে বার করা ৭টা
     *  জায়গা), সেখানে এটাই ব্যবহার হয় — একটাও দরকারি ঘর হারায় না, শুধু
     *  ৬০–১২০ KB-র base64 ছবিটা আর নামে না।
     *  ⛔ যেখানে ছবি সত্যিই দেখানো হয় (Check-up হেডার · Report Card) সেখানে
     *     এটা ব্যবহার হয় না — সেগুলোর জন্য `PatientPhotoCache`।
     *  ⛔ V796 — `photo`-র সঙ্গে `editHistory`-ও বাদ। কারণ দুটো:
     *     (১) খাতার নিয়ম — "editHistory তালিকা-পড়ায় টানা হয় না (egress বাঁচাতে)";
     *     (২) যাচাই করে দেখা গেছে এই ১২টা জায়গার একটাও ওটা পড়ে না —
     *         একমাত্র PatientTimelineActivity নিজে আলাদা করে `id,editHistory`
     *         টানে, তাই কোনো কাজ নষ্ট হয়নি। */
    // 🏷️🔒 V1508 (১৬.০৯.২০২৬, TK-নির্দেশ — MD ANARUL HOWK) — `billBeforeDiscount`
    // যোগ হলো। এতদিন এই তালিকায় ছিলই না (V1014-এর সময় বাদ পড়ে গিয়েছিল),
    // তাই "Estimated" চিপে আসল বিল কখনো দেখানো যায়নি। ⛔ একটামাত্র ছোট
    // সংখ্যার ঘর — ছবির মতো ভারী কিছু নয়, egress-এ প্রভাব নগণ্য।
    const val PATIENT_NO_PHOTO_COLS = "id,address,age,altMobile,bill,branch,complaint,completeApprovedBy,completeRequestedBy,createdAt,createdBy,date,decision,diagnosis,discount,billBeforeDiscount,disease,doctorAdvice,doctorComplete,doctorFullNote,medicalHistory,mobile,name,occupation,patientId,previousCost,previousResult,previousTreatment,queue,queuedAt,refBy,refDoctor,refDoctorMobile,refundRestoredBy,registeredBy,registrationDate,sex,sinceWhen,stage,timeType,treatmentDuration,updatedAt,visitDate"

    /** 🔴🔒 V794 — Follow-up সারিতে `photo` ও `history` দুটোই ভারী
     *  (`SafeWideColumns`)। যেসব জায়গায় শুধু id/মিল দেখা হয়, সেখানে এই
     *  ছোট্ট তালিকাটাই যথেষ্ট — প্রমাণ করে দেখা হয়েছে ওরা আর কিছু পড়ে না। */
    const val FOLLOWUP_ID_COLS = "id,mobile,stage,patientId,name,updatedAt"

    fun fetchListSlimOrNull(table: String, filter: String?, limit: Int, cols: String, order: String = "updatedAt.desc.nullslast", offset: Int = 0): JSONArray? {
        val narrow = fetchListOrNull(table, filter, limit, order = order, select = cols, offset = offset)
        if (narrow != null) { slimProven.add(table); return narrow }
        if (slimProven.contains(table)) {
            val retry = fetchListOrNull(table, filter, limit, order = order, select = cols, offset = offset)
            if (retry != null) return retry
        }
        // 🖼️🔒 V493 (TK-নির্দেশ ৪ ও ৬) — শেষ চেষ্টায় আর `select=*` নয়।
        // ছবি/বড় লেখার ঘরগুলো বাদ, কিন্তু **ডাকার জায়গা যা চেয়েছিল তার
        // সবটাই থাকে** (SafeWideColumns দেখুন)। B446-এর গ্যারান্টি অটুট:
        // সারিগুলো ঠিকই আসে, তাই খালি তালিকা/₹0 ফিরতে পারে না।
        // 🔴🔒 V494 (২১.০৮.২০২৬, TK-যাচাই ৩) — **B446-এর ফাঁক বন্ধ।**
        // V493-এ এখানে ছিল `if (safe != null) return fetchListOrNull(... safe ...)`
        // — অর্থাৎ ছবি-ছাড়া পড়াটাও ব্যর্থ হলে সরাসরি `null` ফিরত, পুরনো
        // `select=*` চেষ্টাটাই আর হত না। দুর্বল নেটে সেটা আবার "খালি তালিকা /
        // Collection ₹0" দেখানোর পথ খুলে দিত — ঠিক যে বাগ খাতার সারি B446-এ
        // ঠিক করা হয়েছিল। এখন তিন ধাপই পুরোপুরি চলে।
        val safe = SafeWideColumns.forTable(table, cols)
        if (safe != null) {
            val safeRead = fetchListOrNull(table, filter, limit, order = order, select = safe, offset = offset)
            if (safeRead != null) return safeRead
        }
        return fetchListOrNull(table, filter, limit, order = order, offset = offset)
    }

    /** Narrowed read. Returns an empty array on failure, exactly like
     *  fetchList -- callers keep their own behaviour unchanged. */
    fun fetchListSlim(table: String, filter: String?, limit: Int, cols: String, order: String = "updatedAt.desc.nullslast"): JSONArray {
        val narrow = fetchListOrNull(table, filter, limit, order = order, select = cols)
        if (narrow != null) { slimProven.add(table); return narrow }
        if (slimProven.contains(table)) {           // 🔵 V405 — উপরের একই যুক্তি
            val retry = fetchListOrNull(table, filter, limit, order = order, select = cols)
            if (retry != null) return retry
        }
        // 🖼️🔒 V493 — উপরের একই নিয়ম (ছবি বাদ, চাওয়া ঘর অটুট)।
        val safe = SafeWideColumns.forTable(table, cols)
        if (safe != null) {
            val safeRead = fetchListOrNull(table, filter, limit, order = order, select = safe)
            if (safeRead != null) return safeRead
        }
        return fetchList(table, filter, limit, order = order)
    }

    /** Upserts (insert-or-update by id) a single row into the given table.
     * Returns true on success, false on any failure (network, server error,
     * etc.) -- callers are responsible for queueing a retry on failure. */
    /**
     * 🔒 V220 (§1, 31.07.2026): ব্যর্থ (4xx) লেখার PostgREST উত্তরের body থেকে
     * **আসল কারণ/ভুল Field** সংক্ষেপে বের করে (code · message · details)। এতে
     * আটকে থাকা সারির Table+Record-এর সঙ্গে "কোন ঘরটা ভুল" তা দেখা যায় (§4-এর
     * সতর্কবার্তায়)। ⛔ শুধু পড়া/সংক্ষেপ — কোনো নতুন request নেই, কোনো data বদলায় না।
     * ⛔ রোগীর তথ্য নয় — PostgREST শুধু column/constraint-এর নাম দেয়; তবু নিরাপত্তার
     *    জন্য ~200 অক্ষরে কাটা হয়।
     */
    private fun errSummary(body: String): String {
        return try {
            val b = body.trim()
            if (b.isBlank()) return ""
            val obj = try { JSONObject(b) } catch (_: Throwable) { null }
            val text = if (obj != null) {
                listOf("code", "message", "details", "hint")
                    .mapNotNull { k -> obj.optString(k, "").takeIf { it.isNotBlank() }?.let { "$k=$it" } }
                    .joinToString(" · ")
            } else b
            val clean = text.replace("\n", " ").trim()
            if (clean.length > 200) clean.substring(0, 200) + "…" else clean
        } catch (_: Throwable) { "" }
    }

    /**
     * V452 (19.08.2026): one-day-one-treatment-payment cloud write.
     *
     * This is ONE write replacing the old payments upsert. The database returns
     * the canonical daily row: if this is a genuine second collection on the
     * same day, the new money event is merged there (Cash/Online kept separate).
     * Returning the canonical row is important on a fresh/reinstalled phone,
     * where local cache may not yet know that today's payment already exists.
     */
    /* ═══════════════════════════════════════════════════════════════════
       🔴🔒 V1122 (০৫.০৯.২০২৬, TK-অনুমোদিত — "হ্যাঁ করুন, সাবধানে")
       TK: *"ভিজিট ফি যখন বাধ্যতামূলক, তখন আমি এই ধরনের নোটিফিকেশন কেন মেনে নেব"*

       🔴 প্রমাণিত কারণ: রেজিস্ট্রেশনের তিনটে সারি (followups · patients ·
       payments) **আলাদা আলাদা তিনটে অনুরোধে** যেত, payments সবার শেষে। মাঝপথে
       লাইন কাটলে প্রথম দুটো বসে যেত আর ফি-র সারিটা ফোনের জমা-ঘরে পড়ে থাকত —
       ওই ফোন পরে ফ্লাশ না করলে (অ্যাপ আবার বসানো · ডেটা মুছে যাওয়া · ফোন বদল)
       সারিটা চিরতরে হারাত, অথচ রোগী ক্লাউডে বসেই থাকত।

       ⇒ এখন **একটাই অনুরোধ** — সার্ভারের নিজের লেনদেনে তিনটে সারি একসাথে বসে।
         একটায় গোলমাল হলে তিনটেরই কিছু বসে না, তাই "রোগী আছে, ফি নেই" অবস্থাটা
         আর তৈরিই হতে পারে না।

       ⛔ **ব্যর্থ হলে কিছুই ভাঙে না** — `false` ফেরে, আর ডাকনেওয়ালা তার পুরনো
          প্রমাণিত পথেই (তিনটে আলাদা সারি + জমা-ঘর) সেভ করে। SQL ফাংশনটা
          চালানোর আগেও অ্যাপ হুবহু আগের মতোই চলবে।
       ⛔ নেট না থাকলে আগের মতোই ফোনে জমা থাকে — কিছুই হারায় না।
       ⛔ Egress: তিনটে অনুরোধের বদলে একটা ⇒ খরচ বাড়ে না, কমে।
       ⛔ চালু `recordTreatmentPayment()`-এর হুবহু একই ধাঁচ — নতুন কিছু বানানো হয়নি। */
    fun registerAtomic(patient: JSONObject, followUp: JSONObject, payment: JSONObject?): Boolean {
        return try {
            val body = JSONObject()
                .put("p_patient", patient)
                .put("p_followup", followUp)
                .put("p_payment", payment ?: JSONObject.NULL)
            val request = Request.Builder()
                .url("$URL/rest/v1/rpc/tk_register_patient")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
            writeHttp.newCall(request).execute().use { resp -> resp.isSuccessful }
        } catch (_: Throwable) { false }
    }

    /** 🔴🔒 V1361 (১১.০৯.২০২৬, পুরো-প্রজেক্ট দেরি-অডিট, TK-বাছাই "ক") — `RefundedRecords`-এর
     *  নিয়ম (কে "বাতিল হিসেবে টাকা লুকানো হবে") এখন সার্ভারে (`tk_refunded_mobiles`),
     *  তাই এই ফোনে ব্রাঞ্চের **পুরো** followups+patients (৫০০০+৫০০০ পর্যন্ত) আর নামাতে
     *  হয় না — শুধু মোবাইল নম্বরের ছোট তালিকা আসে। ব্যর্থ হলে `null` — ডাকা জায়গায়
     *  আগের ভারী-কিন্তু-প্রমাণিত পথে ফিরে যাওয়া হয়, তাই টাকার হিসাব কখনো ভুল হয় না। */
    fun refundedMobilesRpc(branch: String?): HashSet<String>? {
        return try {
            val body = JSONObject().put("p_branch", branch?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            val request = Request.Builder()
                .url("$URL/rest/v1/rpc/tk_refunded_mobiles")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = JSONArray(resp.body?.string().orEmpty())
                val out = HashSet<String>()
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i)?.optString("mobile", "").orEmpty()
                    if (m.length == 10) out.add(m)
                }
                out
            }
        } catch (_: Throwable) { null }
    }

    /** 🔴🔒 V1372 (১২.০৯.২০২৬, তালিকা ৪৬২-চ — TK-নির্দেশে গভীরে যাচাই করে):
     *  `followups.history`-তে একটা নতুন এন্ট্রি **সরাসরি সার্ভারেই** জুড়ে দেয়
     *  (single atomic UPDATE, jsonb ||) — ফোন আর নিজে পুরো array পড়ে-জুড়ে-
     *  ফিরিয়ে লেখে না। দুই ফোন কাছাকাছি সময়ে একই রোগীর রিমার্ক লিখলেও
     *  PostgreSQL-এর সারি-লক নিজেই দুজনের এন্ট্রি ধরে রাখে — কেউ কারো লেখা
     *  হারায় না (নকল ডেটাবেসে overlapping লেখা দিয়ে যাচাই করা)।
     *  ব্যর্থ হলে (পুরনো ডেটাবেসে ফাংশন না থাকলেও) `null` — ডাকা জায়গা আগের
     *  read-modify-write পথেই ফিরে যায়, তাই আচরণ কখনো আগের চেয়ে খারাপ হয় না। */
    fun appendFollowupHistory(id: String, entry: JSONObject): JSONArray? {
        if (blockedFromWriting()) return null
        return try {
            val body = JSONObject().put("p_id", id).put("p_entry", entry)
            val request = Request.Builder()
                .url("$URL/rest/v1/rpc/tk_append_followup_history")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
            writeHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank() || raw == "null") null else JSONArray(raw)
            }
        } catch (_: Throwable) { null }
    }

    fun recordTreatmentPayment(row: JSONObject): JSONObject? {
        return try {
            val body = JSONObject().put("p_row", row)
            val request = Request.Builder()
                .url("$URL/rest/v1/rpc/tk_record_treatment_payment")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
            writeHttp.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    // 🔴🔴🔴 V509 (২১.০৮.২০২৬, TK-রিপোর্ট, দুই ফোনের ছবিসহ —
                    // KNE-LAXMI "24 to sync (11 other work, 13 Payment)" ও
                    // COB-4 "২টি তথ্য এখনো ক্লাউডে যায়নি"): **payments_pkey
                    // duplicate দেখিয়ে চিরকাল আটকে থাকা পেমেন্ট।**
                    //
                    // ─── আসল কারণ (কোড ধরে প্রমাণিত) ────────────────────────
                    // এই অ্যাপে পেমেন্টের জন্য **দুটো আলাদা অপেক্ষমাণ তালিকা** আছে:
                    //   ক) `CloudWriteQueue` — এখানে V479-এ নিয়ম বসানো আছে:
                    //      "নিজের id-তে pkey duplicate মানে সারিটা ইতিমধ্যেই
                    //       ক্লাউডে বসে আছে → সফল ধরে বাদ দাও"। **এটা কাজ করে।**
                    //   খ) `PaymentRepository`-র নিজের তালিকা
                    //      (`piles_clinic_payment_pending`) — এখানে ওরকম কোনো
                    //      নিয়ম **নেই**, ছেড়ে দেওয়ারও কোনো নিয়ম নেই।
                    //
                    // ফল: (খ) প্রতিবার এই ফাংশনটা ডাকে → ৪০৯ duplicate আসে →
                    // `null` ফেরে → পেমেন্টটা (খ)-তেই থেকে যায়, **আর এখানকার
                    // `remember()` (ক)-তে একটা নতুন ভূতুড়ে এন্ট্রি বানিয়ে দেয়**।
                    // (ক) সেটা পরের বারই বাদ দেয়, কিন্তু (খ) আবার বানায় —
                    // অর্থাৎ **"11 other work + 13 Payment" চিরকাল ঘুরতে থাকে**,
                    // "send" চাপলেও কোনোদিন শূন্য হয় না।
                    //
                    // ─── ✅ সমাধান (V479-এর হুবহু প্রমাণিত নিয়মটাই এখানেও) ───
                    // `_pkey`-এর উপর duplicate মানে **নিজের এই একই id-র সারিটা
                    // সত্যিই ইতিমধ্যে ওখানে আছে** — টাকা ক্লাউডে পৌঁছে গেছে।
                    // তাই ক্লাউড থেকে ঐ আসল সারিটা পড়ে সেটাই ফেরত দেওয়া হয়,
                    // অর্থাৎ **সফল**। তখন পেমেন্টটা (খ) থেকেও উঠে যায়, আর
                    // ভূতুড়ে এন্ট্রিও তৈরি হয় না।
                    //
                    // ⛔ কোনো টাকা দুবার বসতে পারে না — এখানে **কিছুই লেখা হয় না**,
                    //    শুধু ইতিমধ্যে-বসে-থাকা সারিটা **পড়া** হয়।
                    // ⛔ পড়াও যদি ব্যর্থ হয়, তবে আগের মতোই `null` — পেমেন্ট
                    //    তালিকায় থেকে যায়, কিছুই হারায় না।
                    // ⛔ অন্য যে কোনো ব্যর্থতায় (নেট/অন্য কোড) আচরণ অবিকল আগের মতোই।
                    val payId = row.optString("id", "")
                    /* 🔴🔒 V903 — একই কারণে (উপরে CloudWriteQueue দেখুন) এখানেও
                       "already exists" ও আইডি-মিল আর চাওয়া হয় না; সার্ভার ওই
                       বিস্তারিত অংশটা না পাঠালে এই পথটাও কাজ করত না।
                       ⛔ নিচে সারিটা **পড়ে** নিশ্চিত হওয়া হয় — না পেলে আগের
                          মতোই ব্যর্থ ধরা হয়, তাই ভুল করে "হয়ে গেছে" বলার পথ নেই। */
                    val dup = payId.isNotBlank() &&
                        raw.contains("23505") &&
                        raw.contains("payments_pkey")
                    if (dup) {
                        val existing = try {
                            val enc = java.net.URLEncoder.encode(payId, "UTF-8")
                            val rows = fetchListOrNull("payments", "id=eq.$enc", 1)
                            if (rows != null && rows.length() > 0) rows.optJSONObject(0) else null
                        } catch (_: Throwable) { null }
                        if (existing != null && existing.optString("id", "").isNotBlank()) {
                            // ইতিমধ্যেই ক্লাউডে আছে — এটাই সফল।
                            try { CloudWriteQueue.forget("UPSERT", "payments", payId) } catch (_: Throwable) { }
                            try { CloudReadCache.clear() } catch (_: Throwable) { }
                            try { CloudReadDedupe.clear("payments") } catch (_: Throwable) { }
                            try { CloudListRevalidate.clear("payments") } catch (_: Throwable) { }
                            return@use existing
                        }
                        // পড়া গেল না — আগের মতোই retry-র জন্য রেখে দেওয়া হয়।
                    }
                    try {
                        CloudWriteQueue.remember(
                            "UPSERT", "payments", row.optString("id", ""), row,
                            "HTTP ${resp.code}" + errSummary(raw).let { if (it.isBlank()) "" else " · $it" }
                        )
                    } catch (_: Throwable) { }
                    return null
                }
                val trimmed = raw.trim()
                val obj = try { JSONObject(trimmed) } catch (_: Throwable) {
                    try { JSONArray(trimmed).optJSONObject(0) } catch (_: Throwable) { null }
                }
                if (obj == null || obj.optString("id", "").isBlank()) {
                    try { CloudWriteQueue.remember("UPSERT", "payments", row.optString("id", ""), row, "canonical payment not returned") } catch (_: Throwable) { }
                    null
                } else {
                    try { CloudReadCache.clear() } catch (_: Throwable) { }
                    try { CloudReadDedupe.clear("payments") } catch (_: Throwable) { }
                    try { CloudListRevalidate.clear("payments") } catch (_: Throwable) { }
                    obj
                }
            }
        } catch (e: Throwable) {
            try { CloudWriteQueue.remember("UPSERT", "payments", row.optString("id", ""), row, e.javaClass.simpleName) } catch (_: Throwable) { }
            null
        }
    }

    /**
     * 🔐🔒 V496 (২১.০৮.২০২৬, TK §১১) — **গুরুত্বপূর্ণ কিছু লেখার আগে যাচাই।**
     *
     * মাস্টার কাউকে Suspend/Remove করলে ওই ফোন যেন আর নতুন তথ্য লিখতে না পারে।
     * সত্যের উৎস আগের মতোই `hr.staff_profiles.active` ও `suspended_until`
     * (V311/V404) — **নতুন কোনো ব্যবস্থা বানানো হয়নি**।
     *
     * খরচের নিয়ম (TK §১১): শেষ যাচাইয়ের পরে **১৫ মিনিট** না পেরোলে নতুন ডাক
     * যায় না। পেরোলে তখনই টাটকা যাচাই — অর্থাৎ ১৫ মিনিটের পুরনো ফল দিয়ে
     * কখনো লেখা অনুমোদন করা হয় না।
     * ⇒ দিনে জনপ্রতি সর্বোচ্চ ~৩০টা ছোট ডাক।
     *
     * ⛔ **fail-open** — নেট না থাকলে বা যাচাই করা না গেলে লেখা আটকানো হয় না।
     *    নইলে ইন্টারনেট একটু দুর্বল হলেই সবার কাজ বন্ধ হয়ে যেত, আর সেটা
     *    খাতার সারি B446-এর মতোই বিপজ্জনক। অফলাইনের এই সীমা নথিতে লেখা আছে।
     * ⛔ মাস্টারের লেখা কখনো আটকানো হয় না।
     * ⛔ Application-এর Context না থাকলে (যেমন পরীক্ষায়) কিছুই করা হয় না।
     */
    private fun blockedFromWriting(): Boolean {
        return try {
            val ctx = com.tkbiswas.pilesclinic.PilesClinicApplication.appContext ?: return false
            val check = com.tkbiswas.pilesclinic.native.SessionGuard.ensureFreshForWrite(ctx)
            val isBlocked = check.verdict == com.tkbiswas.pilesclinic.native.SessionGuard.Verdict.BLOCKED
            if (isBlocked) lastWriteFailReason = "Blocked: " + check.message.ifBlank { "account suspended/removed" }   // 🔴🔒 V1584
            isBlocked
        } catch (_: Throwable) {
            false      // যাচাই করা না গেলে কাজ আটকানো হয় না
        }
    }

    fun upsert(table: String, row: JSONObject): Boolean {
        if (blockedFromWriting()) return false
        return try {
            // 🔒 V223 (§C2, 01.08.2026): row-এ `updatedAt` থাকলে (মূল ডেটা-টেবিল) লেখার
            // পরে **আসলে আমাদের data-ই বসল কিনা** যাচাই করা হয় — server representation-এ
            // ফেরা `updatedAt` আমাদের পাঠানোটার সঙ্গে মিললে LANDED, না মিললে (DB trigger
            // পুরোনো লেখা আটকে cloud-এর নবীন row রেখেছে) SUPERSEDED। `updatedAt`-হীন
            // row-এ (deleted_records/activity_logs/trash) আগের মতোই return=minimal।
            val verify = row.has("updatedAt")
            val sentUpdatedAt = row.s("updatedAt")
            val request = Request.Builder()
                .url(if (verify) "$URL/rest/v1/$table?select=id,updatedAt" else "$URL/rest/v1/$table")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", if (verify) "resolution=merge-duplicates,return=representation" else "resolution=merge-duplicates,return=minimal")
                .post(JSONArray().put(row).toString().toRequestBody(jsonMedia))
                .build()
            // 🔒 খাতার সারি B194 (TK, 30.07.2026 রাত): ব্যর্থ হলে HTTP কোডটাও
            // ধরে রাখা হয় (শুধু দেখানোর জন্য — নিচের `ok` নির্ধারণ একই আছে)।
            var httpReason = ""
            // 🔒 V222 (§1): নেট-কল **শুরুর আগে** সময় ধরা — `clearConfirmed` যাতে এই
            // লেখা চলাকালীন/পরে জমা নতুন Pending কাজ (Remark/Date/Payment/Follow-up)
            // কখনো না মোছে; শুধু এর আগের পুরোনো/সম্পন্ন কাজই পরিষ্কার হয়।
            val writeStart = System.currentTimeMillis()
            // outcome: 0 = FAILED (নেট/সার্ভার) · 1 = LANDED (আমাদের data সত্যিই বসেছে) ·
            //          2 = SUPERSEDED (trigger পুরোনো লেখা আটকেছে; cloud-এ নবীন row আছে)
            val outcome = writeHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    httpReason = "HTTP ${resp.code}"
                    // 🔒 V220 (§1): আসল কারণ/ভুল Field body থেকে (থাকলে) জুড়ে দেওয়া।
                    try { val b = resp.body?.string().orEmpty(); val s = errSummary(b); if (s.isNotBlank()) httpReason += " · $s" } catch (_: Throwable) { }
                    0
                } else if (!verify) {
                    1   // updatedAt-হীন টেবিল — আগের মতোই 2xx = বসেছে (এদের trigger নেই)
                } else {
                    // 🔒 V223 (§C2): representation পড়ে দেখা — আমাদের updatedAt-ই বসল কিনা।
                    val body = resp.body?.string().orEmpty()
                    val ret = try { JSONArray(body) } catch (_: Throwable) { JSONArray() }
                    if (ret.length() == 0) {
                        1   // প্রতিনিধিত্ব ফাঁকা (অস্বাভাবিক) → আগের আচরণ (LANDED ধরা)
                    } else {
                        val retUpd = ret.optJSONObject(0)?.s("updatedAt") ?: ""
                        if (retUpd.isBlank() || sentUpdatedAt.isBlank()) 1  // তুলনা অসম্ভব → LANDED (আগের আচরণ)
                        else if (retUpd == sentUpdatedAt) 1                  // আমাদের data বসেছে → LANDED
                        else 2                                              // cloud নবীন রয়ে গেছে → SUPERSEDED
                    }
                }
            }
            // TK-REQUESTED SAFETY (2026-07-26): a few rows use a DETERMINISTIC
            // id that can legitimately come back later . e.g. the "আসার কথা"
            // (chamber_expected) row is always "exp_<last 10 digits>". If such
            // a row was once cancelled, its id sits in the deleted list, and a
            // brand new mark for the same person would then be dropped by the
            // queues forever. Any row that actually reaches the cloud is alive
            // again by definition, so its id is cleared from that list here.
            // This cannot weaken the delete guard: a queue never reaches this
            // function for a row it has already skipped as deleted.
            // 🚨 TK'S ORDER (2026-07-28): a failed write must never just vanish.
            // Every write in the app comes through here, so one safety net here
            // covers all of them. It only runs when the write has ALREADY
            // failed, so nothing that works today changes.
            when (outcome) {
                0 -> {
                    // FAILED — কিছু বসেনি; পরে retry-র জন্য মনে রাখা (আগের মতোই)।
                    lastWriteFailReason = httpReason.ifBlank { "Unknown server error" }   // 🔴🔒 V1584
                    try { CloudWriteQueue.remember("UPSERT", table, row.optString("id", ""), row, httpReason) } catch (_: Throwable) { }
                }
                2 -> {
                    // 🔒 V223 (§C2): SUPERSEDED — DB trigger আমাদের **পুরোনো** লেখা আটকেছে,
                    // cloud-এ **নবীন** row রয়ে গেছে। ⛔ আমাদের data বসেনি, তাই
                    //   · `clearConfirmed` **নয়** — নইলে না-বসা লেখাকে "success" ধরে ঐ row-এর
                    //     অন্য Pending কাজ (নতুন Remark/Date) মুছে যেত (concern 2)।
                    //   · `remember`/retry **নয়** — এটা obsolete (cloud নবীন), retry-তে আবারই আটকাবে।
                    // row cloud-এ জীবিত, তাই tombstone সাফ ও cache fresh রাখা হয়।
                    try { DeletedGuard.unmark(table, row.optString("id", "")) } catch (_: Throwable) { }
                    try { CloudReadCache.clear() } catch (_: Throwable) { }
                    try { CloudReadDedupe.clear(table) } catch (_: Throwable) { }
                    try { CloudListRevalidate.clear(table) } catch (_: Throwable) { }
                }
                else -> {
                    // 1 = LANDED — আমাদের data সত্যিই cloud-এ বসেছে।
                    try { DeletedGuard.unmark(table, row.optString("id", "")) } catch (_: Throwable) { }
                    // 🔒 V221 (§2): ঐ একই Table+Record-এর আটকে থাকা পুরোনো UPSERT/UPDATE
                    // (pending ও failed HTTP 400) পরিষ্কার — **শুধু সত্যিকারের LANDED-এর পরে**
                    // (concern 2)। অন্য record/DELETE ছোঁয়া হয় না; সারি ফাঁকা হলে ফাইল পড়াও নয়।
                    try { CloudWriteQueue.clearConfirmed("UPSERT", table, row.optString("id", ""), null, writeStart) } catch (_: Throwable) { }
                    // TK'S DATA-CONSISTENCY RULE (2026-07-25): every write passes through here,
                    // so clearing the short-lived read cache guarantees the next read is fresh.
                    try { CloudReadCache.clear() } catch (_: Throwable) { }
                    try { CloudReadDedupe.clear(table) } catch (_: Throwable) { }
                    try { CloudListRevalidate.clear(table) } catch (_: Throwable) { }
                }
            }
            // LANDED বা SUPERSEDED — দুটোতেই cloud-এ (আমাদের বা নবীন) data আছে, তাই
            // caller-এর কাছে true; শুধু FAILED-এ false (retry হবে)।
            outcome != 0
        } catch (e: Exception) {
            // 🚨 খাতার সারি B145 (TK, 30.07.2026): দুর্বল নেট / timeout হলে সার্ভার
            // কোনো উত্তরই দেয় না — তখন উপরের `if (!ok)` অংশটা পর্যন্ত পৌঁছানোই
            // হয় না, সোজা এখানে এসে পড়ে। আগে এখানে কিছুই মনে রাখা হত না, তাই
            // ঠিক যে অবস্থার জন্য এই জালটা বানানো হয়েছিল (নেট খারাপ) সেই
            // অবস্থাতেই কাজটা নীরবে হারিয়ে যেত।
            // ⛔ একই id ধরে আবার পাঠানো হয়, তাই নকল সারি বা দুবার টাকা হতে পারে না।
            // 🔒 খাতার সারি B194: এই এক্সেপশনটাই আসল কারণ (যেমন টাইমআউট,
            // ঠিকানা খুঁজে না পাওয়া) — শুধু দেখানোর জন্য ধরে রাখা হলো।
            val reason = (e.javaClass.simpleName + ": " + (e.message ?: "")).trim()
            lastWriteFailReason = reason   // 🔴🔒 V1584
            try { CloudWriteQueue.remember("UPSERT", table, row.optString("id", ""), row, reason) } catch (_: Throwable) { }
            false
        }
    }

    /**
     * 🔒 V222 (§3, 31.07.2026): একটা row-এর "কতটা নতুন" — updatedAt (নইলে createdAt/
     * তারিখ) থেকে millisecond। পার্স না হলে 0 (= অজানা)। ISO-8601 ("…SSS'Z'" ও
     * ms ছাড়া রূপ) দুটোই ধরা হয়। ⛔ শুধু তুলনার জন্য — কিছুই লেখে না।
     */
    fun rowStampMs(row: JSONObject): Long {
        val s = listOf("updatedAt", "createdAt", "registrationDate", "visitDate", "date")
            .map { row.optString(it, "") }.firstOrNull { it.isNotBlank() } ?: return 0L
        return parseIsoMs(s)
    }

    private fun parseIsoMs(s: String): Long {
        if (s.isBlank()) return 0L
        val fmts = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        for (f in fmts) {
            try {
                val sdf = java.text.SimpleDateFormat(f, java.util.Locale.US)
                sdf.isLenient = false
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val d = sdf.parse(s) ?: continue
                return d.time
            } catch (_: Throwable) { }
        }
        return 0L
    }

    /** 🔒 V223 (§C1): Restore-লেখার ফল — WRITTEN (আমাদের data বসল) · KEPT_NEWER
     *  (cloud নবীন, তাই রাখা হলো — ব্যর্থ নয়) · BLOCKED (cloud-এর নতুনত্ব **নিশ্চিত
     *  করা যায়নি** — পড়া ব্যর্থ/তুলনা অসম্ভব/লেখা ব্যর্থ — তাই পুরোনো data আন্দাজে
     *  লেখা হয়নি, Restore থেমেছে)। */
    enum class RestoreOutcome { WRITTEN, KEPT_NEWER, BLOCKED }

    /**
     * 🔒🔒 V223 (§C1, 01.08.2026): **Restore-নিরাপদ upsert (কড়া)।** পুরোনো snapshot
     * বসানোর আগে cloud-এর `updatedAt` **অবশ্যই** পড়ে মেলানো হয়:
     *   · cloud পড়া **ব্যর্থ** (network) → `BLOCKED` — পুরোনো data **কখনো আন্দাজে লেখা হয় না**।
     *   · cloud-এ ঐ id **নেই** → নিরাপদ নতুন insert → `WRITTEN`/`BLOCKED`(লেখা ব্যর্থ)।
     *   · দুই পাশে stamp আছে, cloud **কড়া নবীন** → `KEPT_NEWER` (overwrite নয়)।
     *   · দুই পাশে stamp আছে, incoming ≥ cloud → লেখা → `WRITTEN`/`BLOCKED`।
     *   · কোনো পাশে stamp **অজানা** (তুলনা অসম্ভব) → `BLOCKED` (আন্দাজে লেখা নয়)।
     * ⛔ শুধু **Restore-পথে** — রোজকার সেভে নয় (Free-plan অপরিবর্তিত)। একটাই সারির
     *    `id,updatedAt` পড়া। DB `updatedAt`-trigger দ্বিতীয় স্তরের সর্বজনীন পাহারা।
     */
    fun upsertRestoreSafe(table: String, row: JSONObject): RestoreOutcome {
        return try {
            val id = row.optString("id", "")
            if (id.isBlank()) {
                // id নেই — কোনো বিদ্যমান row overwrite-এর ঝুঁকি নেই; সোজা insert।
                return if (upsert(table, row)) RestoreOutcome.WRITTEN else RestoreOutcome.BLOCKED
            }
            // ⚠️ fetchListOrNull: **পড়া ব্যর্থ = null** (খালি array = সত্যিই row নেই) —
            // এই পার্থক্যই "নতুনত্ব নিশ্চিত করা যায়নি → Restore বন্ধ" সম্ভব করে।
            val cur = fetchListOrNull(table, "id=eq.$id", 1, select = "id,updatedAt")
                ?: return RestoreOutcome.BLOCKED   // cloud পড়া ব্যর্থ → আন্দাজে লেখা নয়
            if (cur.length() == 0) {
                // cloud-এ ঐ id নেই → overwrite নয়, নিরাপদ নতুন insert।
                return if (upsert(table, row)) RestoreOutcome.WRITTEN else RestoreOutcome.BLOCKED
            }
            val incoming = rowStampMs(row)
            val cloudStamp = rowStampMs(cur.optJSONObject(0) ?: JSONObject())
            if (incoming <= 0L || cloudStamp <= 0L) return RestoreOutcome.BLOCKED  // তুলনা অসম্ভব → বন্ধ
            if (cloudStamp > incoming) return RestoreOutcome.KEPT_NEWER            // cloud নবীন → রাখা
            return if (upsert(table, row)) RestoreOutcome.WRITTEN else RestoreOutcome.BLOCKED
        } catch (_: Throwable) { RestoreOutcome.BLOCKED }   // যেকোনো সন্দেহে আন্দাজে লেখা নয়
    }

    /** Finds rows in a table matching an exact mobile value (used for
     * duplicate-number checks before saving a new Enquiry). Returns an
     * empty array on any failure so a network hiccup never blocks a
     * legitimate save entirely -- matches the WebView's own
     * "duplicate check is best-effort" behavior. */
    /** Finds rows in a table matching an exact mobile value (used for
     * duplicate-number checks before saving a new Enquiry). Returns an
     * empty array on any failure so a network hiccup never blocks a
     * legitimate save entirely -- matches the WebView's own
     * "duplicate check is best-effort" behavior.
     * ROOT-CAUSE FIX (2026-07-15): this always capped results to 1 row --
     * fine for patients/enquiries/followups (one active record per mobile),
     * but WRONG for "payments", where one mobile can have many transactions:
     * tapping any payment row was silently showing only whichever single
     * payment Supabase returned first, instead of that patient's full
     * payment list. Added an optional `limit` (default 1, so every existing
     * caller keeps behaving exactly as before) that PaymentActivity now
     * overrides to fetch all of a patient's payments. */
    /**
     * 🚨 TK'S RULE (28.07.2026, খাতার সারি B30): *"কোন প্রকার রোগীর যেন ডুপ্লিকেট
     * না হয়। সিস্টেমে যদি আগে থেকে থাকে অবশ্যই ওয়ার্নিং দিতে হবে।"*
     *
     * `findByMobile` উপরে **ব্যর্থ হলেও খালি তালিকা** ফেরত দেয় — অর্থাৎ "এই নম্বর
     * নতুন" আর "দেখতেই পারলাম না" — দুটো একই রকম দেখায়। ঠিক এই কারণেই ধীর লাইনে
     * আগে থেকে থাকা রোগীর **দ্বিতীয় সারি** তৈরি হয়ে যেত।
     *
     * এটা একই অনুরোধ, একই ছাঁকনি — শুধু **সত্যিকারের ব্যর্থতায় `null`** ফেরত দেয়,
     * যাতে ডাকা জায়গাটা দুটোর পার্থক্য বুঝে ওয়ার্নিং দিতে পারে।
     * ⛔ `findByMobile` এক অক্ষরও বদলানো হয়নি — পুরনো সব ডাক আগের মতোই চলবে।
     */
    /* 🟢🔒 V600 (২৩.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — "Add Payment"
       খুললেই প্রতিবার নতুন করে patients/payments টানত, যদিও রোগী আগে থেকেই
       চেনা (Follow-up কার্ড থেকে খোলা)। খুঁজে পাওয়া গেছে: `fetchListOrNull()`
       আগে থেকেই `CloudReadDedupe` (V493, ৬০ সেকেন্ড TTL, প্রতিটা সেভের পরে
       নিজে থেকে খালি হয়ে যায়) দিয়ে সুরক্ষিত — কিন্তু `findByMobile()` ও
       `findByMobileOrNull()` (৪০+ জায়গায় ব্যবহৃত — Payment, Doctor Visit,
       Chamber Attendance, Registration, Print Center, Enquiry, Follow-up...)
       এই সুরক্ষার **বাইরে** ছিল, প্রতিবারই কাঁচা নেট-কল করত।
       ⛔ এখন এই দুটোও ঠিক সেই একই প্রমাণিত পথে (URL/filter/limit এক অক্ষরও
       বদলায়নি, শুধু কাঁচা fetch-টা `CloudReadDedupe.body()`-এর ভিতর দিয়ে
       যায়) — তাই নিজে কিছু সেভ করলে সঙ্গে সঙ্গে পুরনো তথ্য মুছে যায় (আগে
       থেকেই প্রতিটা upsert/update/delete-এর পরে `CloudReadDedupe.clear()`
       ডাকা হয়), কখনো বাসি টাকা/তথ্য দেখানোর ঝুঁকি নেই। */
    fun findByMobileOrNull(table: String, normalizedMobile: String, selectCols: String = "*", limit: Int = 1): JSONArray? {
        return try {
            val digits = normalizedMobile.filter { it.isDigit() }.takeLast(10)
            val filter = if (digits.length == 10) "mobile=like.*$digits" else "mobile=eq.$normalizedMobile"
            val url = "$URL/rest/v1/$table?$filter&select=$selectCols&limit=$limit"
            val body = CloudReadDedupe.body(url) { fetchBodyOrNull(url) } ?: return null
            JSONArray(body)
        } catch (e: Exception) {
            null
        }
    }

    /* 🟣🔒 V961 (০১.০৯.২০২৬, TK-নির্দেশ) — `order` ঘরটা যোগ হলো (ডিফল্ট ফাঁকা,
       তাই **পুরনো প্রতিটা ডাক অবিকল আগের মতোই** চলে)। কারণ: এক নম্বরে একাধিক
       সারি থাকলে `limit=1`-এ সাজানো ছাড়া **যেকোনো একটা** ফিরত — কোনটা, তার
       নিশ্চয়তা নেই। রেজিস্ট্রেশন ফর্ম এখান থেকেই এনকোয়ারির Timing নেয়, আর
       সার্ভারের নিয়ম (V418 SQL) **সবচেয়ে নতুন** এনকোয়ারি ধরে — দুই দিক দুই
       রকম হয়ে যেত। */
    fun findByMobile(table: String, normalizedMobile: String, selectCols: String = "*", limit: Int = 1, order: String = ""): JSONArray {
        return try {
            // Match by the trailing 10 digits, not an exact "+91..." string. The
            // WebView stores mobiles as bare 10 digits (mob() = slice(-10)) while
            // the native app stores "+91...", so an exact match would miss records
            // created by the other front-end. `like.*<digits>` matches both.
            val digits = normalizedMobile.filter { it.isDigit() }.takeLast(10)
            val filter = if (digits.length == 10) "mobile=like.*$digits" else "mobile=eq.$normalizedMobile"
            val orderPart = if (order.isBlank()) "" else "&order=$order"
            val url = "$URL/rest/v1/$table?$filter&select=$selectCols$orderPart&limit=$limit"
            val body = CloudReadDedupe.body(url) { fetchBodyOrNull(url) } ?: return JSONArray()
            JSONArray(body)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    /** Finds rows where the given column starts with prefix (Postgres `like`),
     * used by PatientIdGenerator to find existing patient IDs for the same
     * branch+date to compute the next serial number. Empty array on failure. */
    fun findByPrefix(table: String, column: String, prefix: String, selectCols: String = "*"): JSONArray {
        return try {
            val encodedPrefix = java.net.URLEncoder.encode("$prefix*", "UTF-8")
            val request = Request.Builder()
                .url("$URL/rest/v1/$table?$column=like.$encodedPrefix&select=$selectCols")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return JSONArray()
                val body = response.body?.string() ?: return JSONArray()
                JSONArray(body)
            }
        } catch (e: Exception) {
            JSONArray()
        }
    }

    /* 🟢🔒 V600 (২৩.০৮.২০২৬, TK-নির্দেশ — Egress অডিট #২) — আজকের Supabase
       কোটা শেষ হওয়ার পর (Egress 6.264/5 GB, 125%) পুরো প্রজেক্ট খুঁটিয়ে
       যাচাই করে পাওয়া গেছে: `fetchList()` (এই ফাংশন) ২১টা ফাইলে ~৫৫ জায়গায়
       ব্যবহৃত, কিন্তু `fetchListOrNull()`-এর মতো কখনোই `CloudReadDedupe`
       (V493) বা `CloudListRevalidate` (V513) দিয়ে যায়নি — V515-এর নিজের
       কমেন্টেই এটা লেখা ছিল, কিন্তু তখন শুধু হাতে-গোনা কয়েকটা জায়গা
       (`fetchListGuarded`) সরানো হয়েছিল, বাকি ৫৫টা জায়গা আগের মতোই ছিল।
       ⇒ এখন **এই একটা জায়গায়** বদলে সবকটা একসাথে সুরক্ষিত হলো —
       ২১টা ফাইলের একটা লাইনও ছোঁয়া হয়নি।
       ⛔ আচরণ (contract) হুবহু আগের মতোই: ব্যর্থ হলে **খালি তালিকা** (`[]`),
          `fetchListOrNull`-এর মতো `null` নয় — তাই কোনো ডাকার জায়গার
          `if (rows == null)` বা `.length()` কোনো কোডে ক্র্যাশ/আচরণ-বদল নেই।
       ⛔ URL/filter/limit/order/select — এক অক্ষরও বদলায়নি।
       ⛔ `trash`-এর মতো বড়-রেকর্ড টেবিলে ঝুঁকি নেই: `CloudListRevalidate`-এর
          নিজস্ব ২MB/৮MB/১২MB সীমা (V515) বড় উত্তর কখনো জমা রাখে না — জমা
          না হলেও ক্ষতি নেই, শুধু আগের মতোই প্রতিবার সরাসরি নেটে যায়। */
    fun fetchList(table: String, filter: String? = null, limit: Int = 500, order: String = "updatedAt.desc.nullslast", select: String = "*"): JSONArray {
        return fetchListOrNull(table, filter, limit, order = order, select = select) ?: JSONArray()
    }

    // TK-REQUESTED ADDITION (2026-07-23): same request as fetchList() above,
    // byte-for-byte identical query, but returns null on a genuine failure
    // (bad connection, non-200 response, unreadable body) instead of an
    // empty JSONArray -- so a caller can tell "the server truly has zero
    // rows right now" (empty array, a normal legitimate result e.g. a quiet
    // day with no payments yet) apart from "this fetch didn't actually
    // work" (null). fetchList() itself is completely untouched -- every
    // existing caller keeps its exact current behavior; this is a new,
    // separate function only used where that failure/empty distinction
    // specifically matters (Chamber Attendance / Draft / Payment
    // Collection's cache-fallback fixes).
    // 🔵🔒 V493 (20.08.2026, TK-নির্দেশ — Supabase Egress) — **একই অনুরোধ দুবার নয়।**
    //
    // TK-এর প্রমাণ (Supabase লাইভ লগ): একই বড় তালিকা কাছাকাছি সময়ে বারবার,
    // কখনো **হুবহু একই অনুরোধ দুবার** নামছিল। কোড ধরে খুঁজে কারণ পাওয়া গেল —
    // `onCreate` · `onResume` · `LiveRefresh` · `BackgroundRefreshWorker` ও
    // পাশাপাশি চলা দুটো Repository একে অপরের কথা জানে না, তাই সবাই আলাদা
    // আলাদা করে একই URL চাইত।
    //
    // এখন প্রতিটা পড়া `CloudReadDedupe`-এর ভিতর দিয়ে যায়: হুবহু একই URL
    // এখনই নেটে থাকলে দ্বিতীয়জন সেটারই উত্তরের জন্য অপেক্ষা করে, আর সদ্য
    // (৬০ সেকেন্ডের মধ্যে) পাওয়া উত্তর থাকলে সেটাই দেওয়া হয়।
    //
    // ⛔ **খাতার সারি B446 অটুট** — ব্যর্থ পড়া কখনো জমা হয় না, তাই "নেট
    //    আটকালে খালি তালিকা/₹0" ফিরে আসার পথ নেই।
    // ⛔ প্রতিটা upsert/update/delete-এর পরে সব ভুলে যাওয়া হয়, তাই নিজের
    //    সেভ করা তথ্য কখনো পুরনো দেখাবে না।
    // ⛔ জমা থাকে সার্ভারের **কাঁচা লেখা**, আর প্রত্যেক ডাকার জায়গা নিজের
    //    আলাদা `JSONArray` বানায় — তাই কেউ কারো তালিকা ছুঁতে পারে না,
    //    আচরণ হুবহু আগের মতোই।
    // ⛔ URL এক অক্ষরও বদলায়নি — সার্ভারের দিকে কিছুই আলাদা নয়।
    fun fetchListOrNull(table: String, filter: String? = null, limit: Int = 500, order: String = "updatedAt.desc.nullslast", select: String = "*", offset: Int = 0): JSONArray? {
        return try {
            val filterPart = if (filter != null) "&$filter" else ""
            // 🔵🔒 V442 (19.08.2026, TK-approved — Android DeletedGuard only):
            // offset=0 keeps every old caller's URL byte-for-byte unchanged. A positive
            // offset is used only by DeletedGuard to read deleted_records in safe pages.
            val offsetPart = if (offset > 0) "&offset=$offset" else ""
            val url = "$URL/rest/v1/$table?select=$select&order=$order&limit=$limit$offsetPart$filterPart"
            /* 🔵🔒 V513 (২২.০৮.২০২৬, TK-নির্দেশ — Egress): দুটো স্তর, একটার পরে একটা।
               ১. `CloudReadDedupe` (V493) — ৬০ সেকেন্ডে হুবহু একই URL দুবার নয়।
               ২. `CloudListRevalidate` (নতুন) — ৬০ সেকেন্ড পেরোলে বড় তালিকা
                  নামানোর **আগে** জিজ্ঞেস করা হয় "বদলেছে কি?"; কিছু না বদলালে
                  গতবারের উত্তরটাই দেওয়া হয়, একটাও সারি নামে না।
               ⛔ `offset > 0` (শুধু DeletedGuard-এর পাতা-ধরে পড়া) এই স্তরে ঢোকে
                  না — ওখানে প্রতিটা পাতার URL আলাদা, সই মেলানোর মানে হয় না।
               ⛔ URL এক অক্ষরও বদলায়নি; সার্ভারের দিকে তালিকার অনুরোধ হুবহু আগেরটাই। */
            /* 🔴🔴🔒 V1303 (১০.০৯.২০২৬, তালিকা ৪১৭ — REHANA BAGUM, TK-র মাপা CSV):
               Supabase একটা অনুরোধে **সর্বোচ্চ ১০০০ সারি** দেয় — `limit=5000`
               চাইলেও চুপচাপ ১০০০-ই আসে (কোনো error নয়)। Kishanganj-এর payments
               ১১০৭ সারি, Rehana-র ₹1,500 সারির ক্রম ১০০৩ ⇒ Follow-up কার্ডে Paid ০;
               "All"-এ ২৮২১-র মধ্যে ১৮২১ সারি কখনো আসতই না। এখন ১০০০-র বেশি চাইলে
               **পাতা-ধরে-পাতা** (offset) নামানো হয়, শেষ পাতা ছোট হলে থামে —
               ওয়েবের `wlv1FetchPaged`-এর একই নিয়ম। id-ধরে জোড়া বাদ (পড়ার মাঝে
               নতুন সারি ঢুকলে পাতার সীমানায় একই সারি দুবার আসতে পারে — টাকা
               দুবার গোনা যাবে না)। কোনো পাতা ব্যর্থ ⇒ null (আগের মতোই "ব্যর্থ"),
               আধখানা তালিকা কখনো ফেরে না।
               ⛔ ১০০০ বা কম চাইলে URL · dedupe · revalidate — **হুবহু আগের মতো**। */
            if (limit > SERVER_PAGE_MAX) return fetchListPagedOrNull(table, filter, limit, order, select, offset)
            val body = CloudReadDedupe.body(url) {
                if (offset > 0) fetchBodyOrNull(url)
                else CloudListRevalidate.body(table, filter, url) { fetchBodyOrNull(url) }
            } ?: return null
            JSONArray(body)
        } catch (e: Exception) {
            null
        }
    }

    /** Supabase (PostgREST) প্রতি অনুরোধে সর্বোচ্চ যতগুলো সারি দেয় (project-এর "Max rows")। */
    private const val SERVER_PAGE_MAX = 1000

    /** 🔴 V1303 — ১০০০-র বেশি সারির তালিকা পাতা-ধরে নামানো (উপরে দেখুন)। */
    private fun fetchListPagedOrNull(table: String, filter: String?, limit: Int, order: String, select: String, offset: Int, direct: Boolean = false): JSONArray? {
        return try {
            val filterPart = if (filter != null) "&$filter" else ""
            val start = if (offset > 0) offset else 0
            val end = start + limit

            /* 🟢🔒 V1519 (১৭.০৯.২০২৬, TK-র Loading audit — live DB + source দুটো মেপে):
               Supabase প্রতি HTTP উত্তরে সর্বোচ্চ 1000 row দেয়। V1303 correctness ঠিক
               করেছিল, কিন্তু 5000 row চাইলে page-1 শেষ → page-2 → page-3... **একটার
               পরে একটা** Tokyo region-এ যেত। DB execution কয়েক ms হলেও প্রতিটা page-এ
               আলাদা network round-trip যোগ হয়ে screen loading দীর্ঘ হচ্ছিল।

               এখন exact count সফল হলে যতগুলো page সত্যিই দরকার, `ParallelCloud`
               দিয়ে সর্বোচ্চ ৬টা করে **একসাথে** পড়া হয়। ফল offset-এর একই ক্রমে
               জোড়া হয়; id duplicate বাদ দেওয়া, count-completeness guard, এবং কোনো
               page ব্যর্থ হলে পুরো read=null — V1303/V1304-এর সব নিরাপত্তা অটুট।
               count ব্যর্থ হলে পুরনো one-by-one loop-ই fallback।

               🔴 V1522 egress audit-এ এখানেই একটি ফাঁক ধরা পড়ে: V1519-এ
               offset=0 page `CloudListRevalidate` পেত, কিন্তু পরের pageগুলো শুধু
               `CloudReadDedupe` পেত। নিচের `readPage()` এখন non-direct **সব page**-কে
               একই revalidate guard দেয়। direct=true হলে আগের মতোই bypass থাকে।
               ⛔ filter/order/select/limit/offset ও data/hisab কিছুই বদলানো হয়নি। */
            fun readPage(pageOff: Int, pageLimit: Int): String? {
                val pageOffsetPart = if (pageOff > 0) "&offset=$pageOff" else ""
                val pageUrl = "$URL/rest/v1/$table?select=$select&order=$order&limit=$pageLimit$pageOffsetPart$filterPart"
                return if (direct) {
                    fetchBodyOrNull(pageUrl)
                } else {
                    CloudReadDedupe.body(pageUrl) {
                        /* 🔴🔒 V1522 (18.09.2026, Supabase Free-plan egress audit):
                           V1519-এর parallel pagination-এ page-1 (offset=0) শুধু
                           CloudListRevalidate-এর ভিতর দিয়ে যেত, কিন্তু page-2/3/...
                           প্রতি 60 সেকেন্ডের dedupe শেষে আবার পুরো নেট থেকে নামত।
                           1000+ row-এর followups/patients/payments-এ এটাই বড় egress
                           ফুটো ছিল। এখন প্রতিটা page একই proven fingerprint guard-এর
                           ভিতর দিয়ে যায়। Fingerprint table+filter-এর জন্য একটাই,
                           তাই একই batch-এর সব page একই 15s probe share করে; কিছু
                           না বদলালে page body cache থেকেই ফেরে।
                           ⛔ filter/order/select/offset/rows একটাও বদলায়নি।
                           ⛔ add/delete হলে count বদলায়, edit হলে updatedAt বদলায় —
                              তখন সব page আগের মতোই fresh network read করে। */
                        CloudListRevalidate.body(table, filter, pageUrl) { fetchBodyOrNull(pageUrl) }
                    }
                }
            }

            val expectedTotal = fetchCount(table, filter)
            val all = JSONArray()
            val seenIds = HashSet<String>()

            fun appendPage(body: String): Int {
                val page = JSONArray(body)
                for (i in 0 until page.length()) {
                    val row = page.opt(i)
                    val id = (row as? JSONObject)?.optString("id", "") ?: ""
                    if (id.isNotBlank() && !seenIds.add(id)) continue
                    all.put(row)
                }
                return page.length()
            }

            if (expectedTotal >= 0) {
                val remaining = (expectedTotal - start).coerceAtLeast(0)
                if (remaining == 0 || limit <= 0) return JSONArray()
                val planned = minOf(remaining, limit)
                val offsets = ArrayList<Int>()
                var off = start
                val plannedEnd = start + planned
                while (off < plannedEnd) {
                    offsets.add(off)
                    off += SERVER_PAGE_MAX
                }

                val bodies = ParallelCloud.map(offsets) { pageOff ->
                    val pageLimit = minOf(SERVER_PAGE_MAX, end - pageOff)
                    readPage(pageOff, pageLimit)
                }
                if (bodies.any { it == null }) return null

                var lastLen = 0
                for (body in bodies) lastLen = appendPage(body!!)

                /* count নেওয়ার পর নতুন row ঢুকে page-boundary পেরিয়ে গেলে planned
                   শেষ page 1000 পূর্ণ হতে পারে। তখন পরের page-ও পড়ি — পুরনো
                   "ছোট শেষ page পর্যন্ত" completeness আচরণ অটুট। */
                var nextOff = if (offsets.isEmpty()) start else offsets.last() + SERVER_PAGE_MAX
                while (lastLen == SERVER_PAGE_MAX && nextOff < end) {
                    val pageLimit = minOf(SERVER_PAGE_MAX, end - nextOff)
                    val body = readPage(nextOff, pageLimit) ?: return null
                    lastLen = appendPage(body)
                    nextOff += SERVER_PAGE_MAX
                }

                val want = minOf((expectedTotal - start).coerceAtLeast(0), limit)
                if (want > 0 && all.length() < want) return null   // 🔴 V1304 — কাটা তালিকা নিষেধ
                return all
            }

            // count জানা যায়নি ⇒ V1303-এর পুরনো sequential fallback হুবহু।
            var off = start
            while (off < end) {
                val pageLimit = minOf(SERVER_PAGE_MAX, end - off)
                val body = readPage(off, pageLimit) ?: return null
                val got = appendPage(body)
                if (got < pageLimit) break
                off += got
            }
            all
        } catch (e: Exception) {
            null
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔴🔴🔒 V512 (২১.০৮.২০২৬) — `fetchListOrNull`-এর হুবহু যমজ, **শুধু
    //    `CloudReadDedupe` ছাড়া**।
    //
    // কেন দরকার হলো: `trash` টেবিলের সারিতে মুছে ফেলা রেকর্ডের **পুরোটা**
    // থাকে (`record` ঘরে), রোগীর base64 ছবিসহ। ওটা `CloudReadDedupe`-এ ৬০
    // সেকেন্ড ধরে রাখলে ওর ৮ MB জায়গা ভরে গিয়ে **অন্য পর্দার** জমানো
    // উত্তরগুলো ছিটকে যেত — ফলে সেগুলো আবার নতুন করে নামত, অর্থাৎ Egress
    // **বাড়ত**। (V509-এ ঠিক এই Egress কমাতেই এত কাজ হয়েছে।)
    //
    // ⛔ পুরোনো `fetchList()` কখনোই dedupe ব্যবহার করত না — তাই এই ফাংশনটা
    //    সফল অবস্থায় **হুবহু আগের মতোই** কাজ করে; একমাত্র পার্থক্য, সত্যিকারের
    //    ব্যর্থতায় ফাঁকা তালিকার বদলে `null` ফেরায়।
    // ⛔ `fetchListOrNull` ও `fetchList` — দুটোর একটাও বদলানো হয়নি।
    // ══════════════════════════════════════════════════════════════════════
    /**
     * 🔵🔒 V515 (২২.০৮.২০২৬, TK-নির্দেশ — Egress অডিট) — `fetchList()`-এর হুবহু
     * যমজ, **শুধু V513/V514-এর পাহারার ভিতর দিয়ে**।
     *
     * কেন দরকার হলো: `fetchList()` নিজের অনুরোধ নিজেই পাঠায় — তাই সেটা
     * `CloudReadDedupe` (৬০ সে.) বা `CloudListRevalidate` (সই মিলিয়ে দেখা)
     * কোনোটার ভিতর দিয়েই যায় না। অ্যাপের বেশিরভাগ `fetchList()` ডাকই ছোট
     * (`id=eq.…`, limit 1) — ওদের এতে কিছু যায়-আসে না। কিন্তু হাতে গোনা
     * কয়েকটা জায়গায় ওটা দিয়ে **পুরো টেবিল** (limit 5000) নামে, আর পর্দা
     * খুললেই বারবার নামে।
     *
     * ⛔ অনুরোধের URL `fetchList()`-এর সঙ্গে **হুবহু এক** — একই `select`,
     *    একই `order`, একই `limit`, একই ছাঁকনি। সার্ভারের দিকে কিছুই আলাদা নয়,
     *    ফিরে আসা সারিগুলোও হুবহু একই।
     * ⛔ ব্যর্থতার আচরণও `fetchList()`-এর মতোই — **খালি তালিকা**, `null` নয়।
     *    (`fetchListOrNull` ব্যর্থে `null` দেয়; এখানে সেটাকে খালি তালিকা করা
     *    হয়, ঠিক যেমন `fetchList()` করত।) ⇒ ডাকার জায়গার কোড বদলাতে হয় না।
     * ⛔ পুরোনো `fetchList()` **এক অক্ষরও বদলায়নি** — তার ৬০+ ডাকের একটাও
     *    ছোঁয়া হয়নি। শুধু যে কয়েকটা জায়গা সত্যিই বড় ও বারবার, সেগুলোকে
     *    এই যমজটায় সরানো হয়েছে।
     * ⛔ ভারী সারির টেবিল (যেমন `trash` — সারিতে মুছে ফেলা পুরো রেকর্ড ও ছবি)
     *    ইচ্ছে করে এখানে আনা হয়নি; V512-এর কারণটা অটুট।
     */
    /* ⚠️🔒 V997 (০৩.০৯.২০২৬, TK-এর Egress অডিটে নিজে ধরা) — **সাবধান:**
       নামে "Guarded" থাকলেও এই ফাংশনে **কোনো বদল-যাচাই নেই** — নিচের এক
       লাইনই সব: প্রতিবার পুরো তালিকা নামে। উপরের লম্বা মন্তব্যটা যে
       পাহারার কথা বলে, সেটা কোডে কখনো বসেনি।
       ⇒ Egress বাঁচাতে হলে ডাকার জায়গায় `fetchListFingerprintOrNull()` দিয়ে
         আগে মিলিয়ে নিতে হবে (যেমন `DoctorVisitRepository.fetchListRawSmartOrNull`
         ও V997-এর `BriefingRepository.fetchRawSmart`)।
       ⛔ এখানে আচরণ ইচ্ছে করেই বদলানো হয়নি; শুধু ভুল বোঝাটা যেন আর না হয়
          সেজন্য এই সতর্কবার্তা। (গুনে দেখা: ডাকার জায়গা মাত্র ৬টা —
          DoctorVisitRepository ২ · BriefingRepository ২ · মন্তব্যে ২। উপরের
          পুরনো মন্তব্যের "৬০+" সংখ্যাটাও ভুল ছিল।) */
    fun fetchListGuarded(table: String, filter: String? = null, limit: Int = 500, order: String = "updatedAt.desc.nullslast", select: String = "*"): JSONArray {
        return fetchListOrNull(table, filter, limit, order = order, select = select) ?: JSONArray()
    }

    fun fetchListOrNullDirect(table: String, filter: String? = null, limit: Int = 500, order: String = "updatedAt.desc.nullslast", select: String = "*"): JSONArray? {
        return try {
            // 🔴 V1303 — ১০০০-র বেশি চাইলে পাতা-ধরে (উপরের একই নিয়ম, শুধু dedupe/revalidate ছাড়া)।
            if (limit > SERVER_PAGE_MAX) return fetchListPagedOrNull(table, filter, limit, order, select, 0, direct = true)
            val filterPart = if (filter != null) "&$filter" else ""
            val url = "$URL/rest/v1/$table?select=$select&order=$order&limit=$limit$filterPart"
            val body = fetchBodyOrNull(url) ?: return null
            JSONArray(body)
        } catch (e: Exception) {
            null
        }
    }

    /** V493 — শুধু নেট থেকে কাঁচা লেখা আনে। আগে এই কাজটা
     *  `fetchListOrNull`-এর ভিতরেই ছিল; হুবহু একই অনুরোধ, একই হেডার। */
    private fun fetchBodyOrNull(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** TK-REQUESTED ADDITION (2026-07-23): count-only query -- used for the
     *  Follow-up screen's Enquiry/Visit/Patient tab numbers. Uses Postgrest's
     *  "count=exact" + a HEAD request so NO row data is transferred at all,
     *  just the total count in the Content-Range response header -- far
     *  cheaper on Supabase quota than fetching all rows just to count them.
     *
     *  🚨 খাতার সারি B145 (TK, 30.07.2026): আগে ব্যর্থ হলে **0** ফেরত দিত, অর্থাৎ
     *  "লাইন কেটে গেছে" আর "সত্যিই একটাও সারি নেই" — দুটো একই দেখাত। পিছনের
     *  কাজ (`BackgroundRefreshWorker`) সেটা দেখে "নতুন কিছু নেই" ধরে ফিরে যেত,
     *  তাই অন্য স্টাফের নতুন তথ্য আসত না এবং আটকে থাকা সেভ পাঠানোও হত না।
     *  এখন ব্যর্থ হলে **-1 = জানি না** ফেরত দেয়, তখন ডাকার জায়গা আগের মতোই
     *  পুরো তালিকা নামিয়ে নেয়।
     *  ⛔ যেসব জায়গায় সংখ্যাটা সোজা পর্দায় বসে (ঘন্টার badge) সেখানে -1 কে 0
     *     ধরা হয়েছে, নইলে পর্দায় উল্টোপাল্টা সংখ্যা দেখাত। */
    /**
     * 🔵🔒 V513 (২২.০৮.২০২৬, TK-নির্দেশ — Egress): টেবিলের **"সই"** —
     * (সারির সংখ্যা, সবচেয়ে নতুন `updatedAt`) — **একটাই ছোট অনুরোধে**।
     *
     * `CloudListRevalidate` এই সই দিয়ে ঠিক করে, বড় তালিকাটা আদৌ আবার নামানোর
     * দরকার আছে কি না। সই না বদলালে একটাও সারি নামে না।
     *
     * কেন একটাই অনুরোধ: PostgREST-কে `Prefer: count=exact` বললে সে
     * `Content-Range` হেডারে **মোট সংখ্যা** পাঠায়, আর body-তে চাওয়া সারিগুলো।
     * তাই `limit=1` + `order=updatedAt.desc` দিলে **একই উত্তরে** দুটোই পাওয়া
     * যায় — সংখ্যা (হেডারে) আর সবচেয়ে নতুন সময় (এক সারি, এক ঘর)।
     * ⇒ প্রতি টেবিলে দুটো নয়, **একটাই** ছোট অনুরোধ।
     *
     * ⛔ ইচ্ছে করে `fetchListOrNull()` দিয়ে নয় — সেটা আবার ঘুরে
     *    `CloudListRevalidate`-এ ঢুকে পড়ত। এটা সরাসরি নেটে যায়।
     * ⛔ ব্যর্থ হলে / `Content-Range` না এলে / `updatedAt` ঘর না থাকলে `null` —
     *    ডাকার জায়গা তখন আগের মতোই পুরো তালিকা নামায়। কখনো আন্দাজে
     *    "সব ঠিক আছে" বলা হয় না।
     * ⛔ টেবিল সত্যিই ফাঁকা হলে সংখ্যা 0 ও সময় `""` — এটাও একটা বৈধ সই।
     * ⛔ `fetchCount()` ও অন্য কোনো ফাংশন ছোঁয়া হয়নি।
     */
    fun fetchListFingerprintOrNull(table: String, filter: String? = null): Pair<Int, String>? {
        return try {
            val filterPart = if (filter != null) "&$filter" else ""
            val url = "$URL/rest/v1/$table?select=updatedAt&order=updatedAt.desc.nullslast&limit=1$filterPart"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Prefer", "count=exact")
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val range = response.header("Content-Range") ?: return null
                val count = range.substringAfter("/").toIntOrNull() ?: return null
                if (count < 0) return null
                val body = response.body?.string() ?: return null
                val arr = JSONArray(body)
                if (arr.length() == 0) return if (count == 0) Pair(0, "") else null
                val v = arr.optJSONObject(0)?.optString("updatedAt", "") ?: return null
                if (v.isBlank()) null else Pair(count, v)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun fetchCount(table: String, filter: String? = null): Int {
        return try {
            val filterPart = if (filter != null) "&$filter" else ""
            val request = Request.Builder()
                .url("$URL/rest/v1/$table?select=id$filterPart")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Prefer", "count=exact")
                .head()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return -1
                val range = response.header("Content-Range") ?: return -1
                range.substringAfter("/").toIntOrNull() ?: -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    /** Updates specific fields of a single row by id (PATCH), used for
     * Remark / Next Follow-up Date edits. Returns true on success. */
    /**
     * 🚨 TK-REPORTED (2026-07-28): "remarks change করলাম, দেখাল হয়ে গেছে, কিন্তু
     * তারপরও remark আগেরটাই থেকে যাচ্ছে।"
     *
     * WHAT WAS WRONG (this one function, used by 62 places in the app):
     * the request said "return=minimal", so the server replied with an EMPTY
     * body. An update that matched NO ROW AT ALL also answers "200 OK" with an
     * empty body -- there was no way to tell the two apart. So a bill
     * correction, an approval, a status change, a treatment-progress note or a
     * remark could all report "saved" while nothing whatsoever had been
     * written, and the retry queue was never told either.
     *
     * NOW: the server is asked to send back the id of each row it actually
     * changed (`select=id` keeps that reply tiny -- no photos, no patient
     * data). If nothing comes back, this returns FALSE, so the caller queues a
     * real retry instead of believing a save that never happened.
     */
    fun updateById(table: String, id: String, fields: JSONObject): Boolean {
        if (blockedFromWriting()) return false
        return try {
            // 🔒 V223 (§C2): patch-এ `updatedAt` থাকলে (এটাই trigger-সংবেদনশীল) representation-এ
            // ফেরা updatedAt দেখে LANDED/SUPERSEDED ঠিক করা হয়। patch-এ updatedAt না থাকলে
            // (subset PATCH) trigger আটকায় না — আগের আচরণ (row মিললেই বসেছে)।
            val verify = fields.has("updatedAt")
            val sentUpdatedAt = fields.s("updatedAt")
            val request = Request.Builder()
                .url("$URL/rest/v1/$table?id=eq.$id&select=" + if (verify) "id,updatedAt" else "id")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(fields.toString().toRequestBody(jsonMedia))
                .build()
            // 🔒 খাতার সারি B194 (TK, 30.07.2026 রাত): ব্যর্থতার আসল কারণ ধরে
            // রাখা হয় (শুধু দেখানোর জন্য) — নিচের `changed` নির্ধারণ একই আছে।
            var httpReason = ""
            // 🔒 V222 (§1): নেট-কল শুরুর আগে সময় ধরা (upsert-এর মতোই)।
            val writeStart = System.currentTimeMillis()
            // outcome: 0 = FAILED/row-not-matched · 1 = LANDED · 2 = SUPERSEDED (trigger আটকেছে)
            val outcome = writeHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    httpReason = "HTTP ${resp.code}"
                    // 🔒 V220 (§1): 4xx হলে আসল কারণ/ভুল Field body থেকে।
                    try { val b = resp.body?.string().orEmpty(); val s = errSummary(b); if (s.isNotBlank()) httpReason += " · $s" } catch (_: Throwable) { }
                    0
                } else {
                    val body = resp.body?.string().orEmpty()
                    val ret = try { JSONArray(body) } catch (_: Exception) { JSONArray() }
                    if (ret.length() == 0) {
                        // 🔒🔒 B593 (10.08.2026, TK-অনুমোদিত — "সার্ভারে-নেই কাজ চুপচাপ
                        // পাকাপাকি বাদ দাও, লাল বার্তা আর ফিরবে না, আসল তথ্য মুছবে না"):
                        // `row_not_matched` = সার্ভারে ঐ id-র সারিটাই নেই (মুছে গেছে/কখনো
                        // ইনসার্ট হয়নি)। একই PATCH আবার পাঠালে **প্রতিবারই** ০ সারি ফিরবে —
                        // কোনোদিন সফল হবে না, শুধু "পাঠানো বাকি"/"যায়নি" ঘরে চিরকাল আটকে
                        // থেকে TK-কে লাল সতর্কবার্তা দেখাত (বারবার রিপোর্ট করা বাগ)। তাই
                        // এটাকে **terminal (outcome 3)** ধরা হয়: retry-তে মনে রাখা হয় **না**,
                        // আর ঐ id-র আটকে-থাকা পুরোনো UPDATE কপি সরিয়ে দেওয়া হয়।
                        // ⛔ তথ্য হারায় না: (ক) নেই-থাকা সারিতে UPDATE এমনিতেও কিছুই করত না;
                        // (খ) followup-এর পুরো সারি আলাদা upsert-heal পথে ক্লাউডে নিরাপদে বসে;
                        // (গ) ইচ্ছাকৃত-মোছা সারি DeletedGuard-এর নিয়মে আবার তৈরিও হওয়া উচিত নয়।
                        httpReason = "row_not_matched"
                        3
                    } else if (!verify) {
                        1   // subset PATCH (updatedAt নেই) — row মিলেছে, বসেছে → LANDED
                    } else {
                        // 🔒 V223 (§C2): আমাদের updatedAt-ই বসল কিনা।
                        val retUpd = ret.optJSONObject(0)?.s("updatedAt") ?: ""
                        if (retUpd.isBlank() || sentUpdatedAt.isBlank()) 1
                        else if (retUpd == sentUpdatedAt) 1   // LANDED
                        else 2                                // SUPERSEDED (cloud নবীন রয়ে গেছে)
                    }
                }
            }
            when (outcome) {
                0 -> {
                    // FAILED/row-not-matched — retry-র জন্য মনে রাখা (আগের মতোই)।
                    try { CloudWriteQueue.remember("UPDATE", table, id, fields, httpReason) } catch (_: Throwable) { }
                }
                2 -> {
                    // 🔒 V223 (§C2): SUPERSEDED — trigger পুরোনো patch আটকেছে, cloud নবীন।
                    // clearConfirmed **নয়** (concern 2), remember **নয়** (obsolete)। cache fresh।
                    try { CloudReadCache.clear() } catch (_: Throwable) { }
                    try { CloudReadDedupe.clear(table) } catch (_: Throwable) { }
                    try { CloudListRevalidate.clear(table) } catch (_: Throwable) { }
                }
                3 -> {
                    // 🔒🔒 B593: row_not_matched (terminal) — remember **নয়** (কোনোদিন
                    // যাবে না)। ঐ id-র আটকে-থাকা পুরোনো একই UPDATE কপি "পাঠানো বাকি"
                    // তালিকা থেকে সরিয়ে দেওয়া হয়, যাতে লাল সতর্কবার্তা পাকাপাকি যায়।
                    // 📱 V1291 (⑨ খ): সারিটা হয়তো এখনো "পাঠানো বাকি" UPSERT হিসেবে এই ফোনেই জমা —
                    // তাহলে এডিটটা তার ভিতরে জুড়ে দেওয়া হয় (হারায় না)। না থাকলে B593 আগের মতোই।
                    try { CloudWriteQueue.mergeIntoPendingUpsert(table, id, fields) } catch (_: Throwable) { }
                    try { CloudWriteQueue.clearConfirmed("UPDATE", table, id, fields, writeStart) } catch (_: Throwable) { }
                }
                else -> {
                    // 1 = LANDED — আমাদের update সত্যিই বসেছে।
                    try { CloudReadCache.clear() } catch (_: Throwable) { }
                    try { CloudReadDedupe.clear(table) } catch (_: Throwable) { }
                    try { CloudListRevalidate.clear(table) } catch (_: Throwable) { }
                    try { CloudWriteQueue.clearConfirmed("UPDATE", table, id, fields, writeStart) } catch (_: Throwable) { }
                }
            }
            outcome != 0
        } catch (e: Exception) {
            // 🚨 খাতার সারি B145 — upsert()-এর মতোই একই কারণ ও একই সুরক্ষা।
            // 🔒 খাতার সারি B194: এক্সেপশনের আসল কারণও ধরে রাখা হলো।
            val reason = (e.javaClass.simpleName + ": " + (e.message ?: "")).trim()
            try { CloudWriteQueue.remember("UPDATE", table, id, fields, reason) } catch (_: Throwable) { }
            false
        }
    }

    /** Deletes a single row by id (DELETE), used by Trash Bin restore to remove
     * the trash entry after re-inserting the record. Returns true on success. */
    fun deleteById(table: String, id: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$URL/rest/v1/$table?id=eq.$id")
                .addHeader("apikey", KEY)
                .addHeader("Authorization", "Bearer ${authToken()}")
                .addHeader("Prefer", "return=minimal")
                .delete()
                .build()
            // 🔒 খাতার সারি B194 (TK, 30.07.2026 রাত): ব্যর্থ হলে HTTP কোডটাও
            // ধরে রাখা হয় (শুধু দেখানোর জন্য)।
            var httpReason = ""
            val ok = writeHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    httpReason = "HTTP ${resp.code}"
                    try { val b = resp.body?.string().orEmpty(); val s = errSummary(b); if (s.isNotBlank()) httpReason += " · $s" } catch (_: Throwable) { }  // V220 (§1)
                }
                resp.isSuccessful
            }
            // TK-REQUESTED (2026-07-26): every delete in the app goes through
            // this one function, so remembering the id here covers Trash Bin,
            // 3 tap deletes and payment deletes at once. A retry queue that
            // still holds this same row will now drop it instead of pushing
            // the record back into the cloud. Bookkeeping only . it can never
            // change whether the delete itself succeeded.
            if (ok) {
                try { DeletedGuard.markDeleted(table, id) } catch (_: Throwable) { }
                // Same data-consistency rule as upsert() above.
                try { CloudReadCache.clear() } catch (_: Throwable) { }
                    try { CloudReadDedupe.clear(table) } catch (_: Throwable) { }
                    try { CloudListRevalidate.clear(table) } catch (_: Throwable) { }
            } else {
                // 🚨🚨 খাতার সারি B166 (TK, 30.07.2026 — TK-এর ৩ নম্বর সন্দেহ):
                // *"ব্যর্থ Delete-এর স্থায়ী Retry নেই। Save এবং Update ব্যর্থ হলে
                //   Retry ব্যবস্থা আছে, কিন্তু মূল Delete ব্যর্থ হলে সেটি কেন্দ্রীয়ভাবে
                //   আবার পাঠানোর তালিকায় ঢোকে না।"* — **সত্যি ছিল।**
                // `upsert` ও `updateById` — দুটোই ব্যর্থ হলে কাজটা মনে রাখত, শুধু
                // এই একটাই রাখত না। ফলে দুর্বল নেটে Delete চাপার পরে কাজটা
                // **অসম্পূর্ণ থেকে যেত** (Trash তৈরি, সারি লুকানো, কিন্তু মূল
                // রেকর্ড রয়ে গেছে — খাতার সারি B165-এর সেই আধা-কাজ)।
                // এখন নেট ফিরলে বা অ্যাপ খুললে **নিজে থেকেই শেষ হয়ে যাবে**।
                // ⛔ আইডি ধরে মোছা হয়, তাই দুবার পাঠালেও ভুল সারি কখনো মুছবে না।
                // ⛔ Restore করলে এই অপেক্ষমাণ কাজটা **মুছে ফেলা হয়**
                //    (`CloudWriteQueue.forget`, ডাকা হয় `DeletedGuard.unmark`
                //    থেকে) — নইলে ফেরানো রেকর্ড আবার মুছে যেত।
                try { CloudWriteQueue.remember("DELETE", table, id, null, httpReason) } catch (_: Throwable) { }
            }
            ok
        } catch (e: Exception) {
            // 🔒🔒 খাতার সারি B195 (TK, 30.07.2026 রাত — "সাবধানে কাজ করুন, সঠিক
            // হয় যেন, কোনো ক্ষতি যেন না হয়"): B166-এ (উপরে) শুধু "HTTP উত্তর
            // এসেছে কিন্তু সফল হয়নি" পথটাই ঠিক হয়েছিল — **এই catch ব্লকটা
            // (সত্যিকারের এক্সেপশন — টাইমআউট, DNS ব্যর্থতা, সংযোগ হঠাৎ কেটে
            // যাওয়া) তখনও বাদ পড়ে গিয়েছিল।** ঠিক দুর্বল নেটে Delete চাপলে যে
            // পরিস্থিতির জন্য B166 বসানো হয়েছিল, সেটাই এই একটা পথে এখনও ঘটতে
            // পারত — Delete কাজটা কোথাও মনে রাখা হত না, তাই আর কখনো নিজে থেকে
            // আবার চেষ্টা হত না।
            // ✅ ওষুধ: upsert()/updateById()-এর catch ব্লকের **হুবহু একই**
            // প্যাটার্ন — এক্সেপশনের নাম+বার্তা ধরে `remember()`-এ পাঠানো।
            // ⛔ **সাবধানে যাচাই করা হয়েছে যাতে কোনো ক্ষতি না হয়:**
            //   · `remember()`-এর ভিতরেই আইডি ফাঁকা হলে কিছুই করে না (DELETE-এ
            //     `id.isBlank()` হলে সরাসরি ফেরত যায়) — তাই এখানে বাড়তি কোনো
            //     পাহারা লাগে না।
            //   · আইডি ধরে মোছা হয় (deterministic), তাই দুবার retry হলেও
            //     ভুল সারি কখনো মুছবে না — ঠিক যেমন B166-এর অন্য পথে আগে থেকেই
            //     ব্যাখ্যা করা আছে।
            //   · Restore করলে এই অপেক্ষমাণ কাজটাও মুছে যায় (`DeletedGuard.
            //     unmark` → `CloudWriteQueue.forget`) — তাই ফেরানো রেকর্ড
            //     আবার মুছে যাওয়ার কোনো ঝুঁকি নেই।
            //   · `ok`/return-type/retry-এর ক্রম কিছুই বদলায়নি — শুধু এই একটা
            //     পথে আগে যা হারিয়ে যেত তা এখন মনে রাখা হয়।
            val reason = (e.javaClass.simpleName + ": " + (e.message ?: "")).trim()
            try { CloudWriteQueue.remember("DELETE", table, id, null, reason) } catch (_: Throwable) { }
            false
        }
    }
}
