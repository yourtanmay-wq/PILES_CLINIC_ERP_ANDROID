package com.tkbiswas.pilesclinic.native

/**
 * 🖼️🔒 V493→V494 (২০–২১.০৮.২০২৬, TK-নির্দেশ ৪ ও ৬) — **শেষ চেষ্টাতেও যেন ছবি না নামে।**
 *
 * TK-এর নির্দেশ:
 *   ৪) *"তালিকা ও Background Refresh-এ রোগীর photo বা অপ্রয়োজনীয় বড় field
 *       নামাবেন না।"*
 *   ৬) *"সরু Read ব্যর্থ হলেই সঙ্গে সঙ্গে select=* দিয়ে সম্পূর্ণ তালিকা
 *       নামানোর ব্যবস্থা কোথায় কোথায় আছে যাচাই করুন... অকারণে ছবিসহ পুরো
 *       table download করবেন না।"*
 *
 * ─── কোড ধরে যা পাওয়া গেল ──────────────────────────────────────────────────
 * `fetchListSlimOrNull` / `fetchListSlim` — এই দুটোই প্রজেক্টের প্রায় সব
 * সরু পড়ার পথ। এদের **শেষ ধাপে** ছিল:
 *
 *     return fetchListOrNull(table, filter, limit, order = order)   // select=*
 *
 * অর্থাৎ সরু পড়া দু'বার ব্যর্থ হলে (বা টেবিলটা এখনো `slimProven` না হলে)
 * **সব ঘর** চাওয়া হত — আর `patients`/`followups`/`medical`-এ "সব ঘর" মানে
 * **রোগীর base64 ছবি**। নেট একটু দুর্বল হলেই তালিকার পর্দাগুলো চুপচাপ
 * ছবি-সহ পুরো টেবিল নামাত, অথচ ওই পর্দায় ছবি দেখানোই হয় না।
 *
 * ─── এখন কী হয় ─────────────────────────────────────────────────────────────
 * `select=*`-এর বদলে এই তালিকা ব্যবহার হয়:
 *
 *     ওই টেবিলের সব ঘর  −  ভারী ঘর  +  ডাকার জায়গা যে **চেনা** ঘর চেয়েছিল
 *
 * **শেষ অংশটাই আসল সুরক্ষা।** ডাকার জায়গা যদি ইচ্ছে করে কোনো ভারী ঘর
 * চেয়ে থাকে (যেমন Prescription-এর `details`, বা RMP-র `callHistory`),
 * সেটা কখনো বাদ পড়ে না — শুধু **যা চাওয়াই হয়নি** সেই ভারী ঘরগুলো বাদ যায়।
 *
 * 🔴 **V494 সংশোধন:** "চেনা" শব্দটাই আসল। V493-এ চাওয়া ঘর **যাচাই না করেই**
 * ফেরানো হত, তাই সরু পড়া যদি ভুল ঘর-নামের কারণে ব্যর্থ হত, সেই ভুল নামটা
 * এই ধাপেও ঢুকে এটাকেও ব্যর্থ করত ⇒ শেষে `select=*` ⇒ ছবি নেমে যেত।
 * এখন শুধু টেবিলের সত্যিকারের ঘরই ফেরানো হয়।
 *
 * ⇒ খাতার সারি **B446**-এর গ্যারান্টি অটুট: এই ধাপ ব্যর্থ হলেও নিচে পুরনো
 *   `select=*` পথ আগের মতোই আছে (V494-এ সেই ফাঁকটাও বন্ধ করা হয়েছে —
 *   `SupabaseClient.fetchListSlimOrNull` দেখুন)।
 * ⇒ ঘরের তালিকা **লাইভ-ডেটাবেসে যাচাই করা ধ্রুবক** (`PATIENT_COLS_NO_PHOTO`,
 *   `FOLLOWUP_COLS_NO_PHOTO`, `PAYMENT_COLS_LIST`, `PATIENT_COLS`) + schema +
 *   ভারী ঘর — সব মিলিয়ে বানানো, তাই কোনো সত্যিকারের ঘর বাদ পড়ে না
 *   (খাতার সারি B105-এর শিক্ষা)।
 *
 * ─── যে ঘরগুলো "ভারী" ধরা হয়েছে (প্রতিটা schema দেখে বাছা) ─────────────────
 *   patients      : photo · medicalHistory
 *   followups     : photo · history
 *   medical       : photos · details
 *   doctor_visits : callHistory · referralPayments
 *   payments      : editHistory
 *
 * ⛔ যে টেবিলের নাম এই তালিকায় নেই, তার আচরণ **হুবহু আগের মতোই** (`select=*`)।
 * ⛔ সফল সরু পড়ার পথ (বেশিরভাগ সময়) এক অক্ষরও বদলায়নি — এটা শুধু
 *    ব্যর্থতার শেষ ধাপ।
 * ⛔ কোনো তথ্য · হিসাব · ডিজাইন কিছুই ছোঁয়া হয়নি।
 */
/* 📱 V1291 (১০.০৯.২০২৬, তালিকা ৪১১-⑧): `server_at` — সার্ভার নিজে বসায় (default now()); এখানে শুধু
   যাতে শেষ-ভরসার পড়ায় ঘরটা বাদ না পড়ে (পাহারা ৯.৩৪)। অ্যাপ এটা কখনো লেখে না, পড়লেও ব্যবহার করে না। */
object SafeWideColumns {

    /** টেবিল → (সব ঘর, ভারী ঘর)। ঘরের নাম `PILES_CLINIC_DB_SETUP.sql` ও
     *  প্রজেক্টে ব্যবহৃত প্রমাণিত তালিকা মিলিয়ে নেওয়া। */
    /* 🔴🔒 V801 (২৮.০৮.২০২৬) — TK: "গভীরে যাচাই করুন / কোন ভালো কাজ যেন খারাপ না হয়"।
       ─── যা ধরা পড়ল ────────────────────────────────────────────────────────
       এই তালিকাগুলো **শেষ-ভরসার** পড়ায় ব্যবহার হয় (সরু পড়া ব্যর্থ হলে,
       `select=*`-এর ঠিক আগে)। কিন্তু নতুন ঘর যোগ হলে এখানে যোগ করা হয়নি,
       তাই SQL-এর সঙ্গে মিলিয়ে দেখে **১৫টা আসল ঘর বাদ পড়ে ছিল**:
         · patients      — doctorReminderDate · doctorReminderNote · doctorReminderTime
                           (V656, V671)
         · payments      — refundReason · refundRequestedBy · refundApprovedBy ·
                           refundOfPaymentId (V215) · backdateRequestedBy ·
                           backdateApprovedBy (PATCH 24.07) · editRequestedBy ·
                           editApprovedBy (PATCH 25.07) · progress (V533)
         · doctor_visits — altMobiles (V318) · expectedPatientDate (PATCH 30.07) ·
                           remarksEditedBy (V458)
       অর্থাৎ শেষ-ভরসার পড়াটা চললে **রিফান্ড কে চেয়েছে/কে অনুমোদন করেছে,
       ব্যাকডেট-অনুমোদন, ডাক্তারের বাড়তি মোবাইল, "কবে রোগী পাঠাবেন" তারিখ**
       — এসব চুপচাপ উধাও হয়ে যেত। টাকার ঘরও ছিল, তাই এটা গুরুতর।
       ─── সারানো ───────────────────────────────────────────────────────────
       প্রতিটা ঘর SQL ফাইল ধরে মিলিয়ে যোগ করা হলো (উপরে ফাইলের নাম লেখা আছে)।
       ⛔ ঝুঁকি নেই: নাম ভুল হলে ওই পড়াটা ব্যর্থ হয়ে নিচের `select=*`-এ নামত —
          অর্থাৎ **আজকের আচরণ**, তার চেয়ে খারাপ কিছু হতে পারত না।
       ⛔ পাহারাদারে নতুন যাচাই ৯.৩৪ বসানো হলো, যাতে ভবিষ্যতে নতুন ঘর যোগ
          হলে এই তালিকা পুরনো হয়ে গেলে সঙ্গে সঙ্গে ধরা পড়ে। */
    private val ALL: Map<String, String> = mapOf(
        "patients" to "id,address,age,altMobile,bill,branch,complaint,completeApprovedBy,completeRequestedBy,createdAt,createdBy,date,decision,diagnosis,discount,billBeforeDiscount,discountReason,discountBy,discountAt,disease,doctorAdvice,doctorComplete,doctorFullNote,doctorReminderDate,doctorReminderNote,doctorReminderTime,editHistory,medicalHistory,mobile,name,occupation,patientId,photo,previousCost,previousResult,previousTreatment,queue,queuedAt,refBy,refDoctor,refDoctorMobile,refundRestoredBy,registeredBy,registrationDate,sex,sinceWhen,stage,timeType,timeSource,treatmentDuration,updatedAt,visitDate,nextVisitPlan,server_at,server_updated_at",
        "followups" to "id,address,age,branch,callCount,convertedPatientId,createdAt,createdBy,date,disease,history,lastCallDate,lastRemark,lastRemarkAt,mobile,name,nextFollow,noMoreCalls,patientId,photo,previousStage,refId,registrationDate,sex,stage,status,timeType,timeSource,updatedAt,visitDate,server_at,server_updated_at",
        "payments" to "id,amount,backdateApprovedBy,backdateRequestedBy,branch,cashAmount,createdAt,createdBy,dailyEvents,date,editApprovedBy,editedAt,editedBy,editHistory,editRequestedBy,mobile,mode,name,onlineAmount,patientCode,patientId,payLabel,paymentLabel,payType,progress,receivedBy,refundApprovalStatus,refundApprovedBy,refundOfPaymentId,refundReason,refundRequestedBy,remarks,updatedAt,server_at",
        "medical" to "id,branch,createdAt,createdBy,date,days,decision,details,diagnosis,doctorFullNote,mobile,name,nextFollow,patientId,photos,selected,type,updatedAt,server_at,server_updated_at",
        "doctor_visits" to "id,altMobiles,area,branch,callHistory,callStatus,createdAt,createdBy,date,expectedPatientDate,lastCallDate,mobile,name,nextCallDate,policeStation,referralDue,referralPaid,referralPayments,remarks,remarksEditedBy,status,updatedAt,server_at"
    )

    private val HEAVY: Map<String, List<String>> = mapOf(
        // ✏️ V736 — editHistory ভারী হতে পারে, তাই তালিকা-পড়ায় বাদ
        //    (egress বাঁচে); সংশোধনের পর্দা খুললে শুধু ওই এক সারির জন্য আনা হয়
        "patients" to listOf("photo", "medicalHistory", "editHistory"),
        "followups" to listOf("photo", "history"),
        "medical" to listOf("photos", "details"),
        "doctor_visits" to listOf("callHistory", "referralPayments"),
        "payments" to listOf("editHistory")
    )

    /**
     * শেষ-চেষ্টার জন্য নিরাপদ ঘরের তালিকা।
     *
     * @param table      কোন টেবিল।
     * @param wantedCols ডাকার জায়গা যে ঘরগুলো চেয়েছিল (সরু তালিকা)।
     * @return ঘরের তালিকা, অথবা টেবিলটা অচেনা হলে `null` (তখন আগের মতোই `select=*`)।
     */
    fun forTable(table: String, wantedCols: String?): String? {
        val all = ALL[table] ?: return null
        val heavy = HEAVY[table] ?: return null
        val known = all.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val knownSet = known.toHashSet()
        val wanted = (wantedCols ?: "")
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }.toHashSet()
        val out = LinkedHashSet<String>()
        for (name in known) {
            // ভারী ঘর শুধু তখনই থাকবে, যখন ডাকার জায়গা নিজে সেটা চেয়েছিল।
            if (name in heavy && name !in wanted) continue
            out.add(name)
        }
        // 🔴🔒 V494 (২১.০৮.২০২৬) — **এখানেই V493-এর আসল বাগটা ছিল।**
        //
        // V493-এ লেখা ছিল `for (w in wanted) out.add(w)` — অর্থাৎ ডাকার জায়গা
        // যা চেয়েছিল **সবই** ফিরিয়ে আনা হত, যাচাই না করেই।
        //
        // ফল: সরু পড়া যদি **ঘরের নাম ভুল** থাকার কারণেই ব্যর্থ হয়ে থাকে,
        // তাহলে সেই ভুল নামটা এই "নিরাপদ" তালিকাতেও ঢুকে যেত ⇒ এই ধাপও
        // ব্যর্থ ⇒ শেষে `select=*` ⇒ **রোগীর ছবি নেমে যেত**। অর্থাৎ যে
        // কারণে ধাপটা বানানো, ঠিক সেই কারণেই সেটা কাজ করত না।
        // (Java পরীক্ষায় হাতেনাতে ধরা পড়েছে — `T3.java` পরিস্থিতি "ক"।)
        //
        // এখন শুধু **চেনা ঘরই** ফেরানো হয়। যে নাম টেবিলে নেই, সেটা রেখে
        // লাভও নেই (পড়াই যাবে না), বরং পুরো ধাপটা নষ্ট হয়।
        // ⛔ চেনা ভারী ঘর (Prescription-এর `details`, RMP-র `callHistory`)
        //    আগের মতোই ফিরে আসে — সেগুলো `known`-এ আছে।
        // ⛔ এই তালিকাগুলো প্রজেক্টের **লাইভ-ডেটাবেসে যাচাই করা** ধ্রুবক থেকে
        //    নেওয়া (খাতার সারি B105), তাই "চেনা নয়" মানে প্রায় নিশ্চিতভাবেই
        //    ভুল নাম। তবু এই ধাপ ব্যর্থ হলে নিচে পুরনো `select=*` পথ আছেই।
        for (w in wanted) if (w != "*" && w in knownSet) out.add(w)
        return if (out.isEmpty()) null else out.joinToString(",")
    }
}
