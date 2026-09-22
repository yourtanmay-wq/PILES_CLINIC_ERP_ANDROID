package com.tkbiswas.pilesclinic.clinical

/**
 * Placeholder role/session holder for Phase 4.
 *
 * The existing Login/Dashboard (Phase 1-2) live inside the WebView JS app, so there
 * is currently no native session object to read the real logged-in role/patient from.
 * Until that wiring is done, screens read the role from:
 *   1) an Intent extra (EXTRA_ROLE / EXTRA_PATIENT_NAME / EXTRA_PATIENT_ID) if the
 *      caller (e.g. a future native Dashboard) supplied one, otherwise
 *   2) this in-memory default (DOCTOR), so the modules are always usable/testable
 *      standalone and never show a blank/broken screen.
 *
 * When Phase 1-2 native login exists, replace `currentRole` assignment below with
 * the real authenticated role and delete the "default" fallback.
 */
object RoleSession {

    const val EXTRA_ROLE = "extra_role"
    const val EXTRA_PATIENT_NAME = "extra_patient_name"
    const val EXTRA_PATIENT_ID = "extra_patient_id"
    // 🔒🔒 খাতার সারি B175 (TK, 30.07.2026 — "Patient ID তো প্রজেক্টের শুরু
    // থেকেই ফাইনাল ছিল, তাহলে প্রেসক্রিপশনে নেই কেন?")। **আসল কারণ (কোড ধরে,
    // আন্দাজ নয়):** `EXTRA_PATIENT_ID`/`currentPatientId` — এই একটাই ঘর **দুটো
    // আলাদা কাজে** ব্যবহার হত: (১) ছাপার কাগজে দেখানো মানুষ-পড়া-যায় এমন আইডি
    // (যেমন "KNE-30072026-001"), (২) `medical` টেবিলে প্রেসক্রিপশন সেভ/খোঁজার
    // **আসল চাবি** (রোগীর সারির ভিতরের raw আইডি, যেমন "pat_9711468691")।
    // এই দুটো মান **আলাদা**, কিন্তু একই ঘরে রাখা হত — তাই যেখান থেকেই raw আইডি
    // আগে বসত (বেশিরভাগ জায়গায়), ছাপায় সেটাই "Patient ID" হিসেবে দেখাত।
    // **সমাধান:** এই নতুন ঘরটা **শুধু দেখানোর জন্য** — `currentPatientId`
    // (raw আইডি) এক অক্ষরও বদলায়নি, তাই `medical` টেবিলের সেভ/খোঁজা আগের
    // মতোই ঠিক থাকে (এটাই সবচেয়ে জরুরি — এটা ভাঙলে প্রেসক্রিপশন হারিয়ে যেত)।
    const val EXTRA_PATIENT_DISPLAY_ID = "extra_patient_display_id"
    const val EXTRA_PATIENT_BRANCH = "extra_patient_branch"
    const val EXTRA_PATIENT_MOBILE = "extra_patient_mobile"
    const val EXTRA_PATIENT_ADDRESS = "extra_patient_address"
    const val EXTRA_PATIENT_AGE = "extra_patient_age"
    const val EXTRA_PATIENT_SEX = "extra_patient_sex"
    const val EXTRA_PATIENT_DISEASE = "extra_patient_disease"

    var currentRole: UserRole = UserRole.STAFF
    var currentPatientName: String = ""
    // ⛔ এটা `medical` টেবিলের সত্যিকারের চাবি (raw row id) — শুধু ছাপার
    //    জন্য এখান থেকে সরাসরি না পড়ে, নিচের `displayId()` ব্যবহার করা হয়।
    var currentPatientId: String = ""
    // 🔒 খাতার সারি B175 — মানুষ-পড়া-যায় Patient ID (যেমন "KNE-30072026-001"),
    // শুধু ছাপার কাগজ/স্ক্রিনে দেখানোর জন্য। ফাঁকা থাকলে `displayId()`
    // নিজে থেকেই raw আইডিতে ফিরে যায় — তাই পুরনো কোনো পথ কখনো ভাঙে না।
    var currentPatientDisplayId: String = ""
    var currentPatientBranch: String = ""
    var currentPatientMobile: String = ""
    var currentPatientAddress: String = ""
    var currentPatientAge: String = ""
    var currentPatientSex: String = ""
    var currentPatientDisease: String = ""

    /** ছাপা/পর্দায় দেখানোর জন্য — মানুষ-পড়া-যায় আইডি থাকলে সেটাই, নইলে
     *  raw আইডি (আগের আচরণ, কিছু ভাঙে না)। ⛔ `medical` টেবিলের সেভ/খোঁজায়
     *  এটা ব্যবহার করা যাবে না — সেখানে সবসময় `currentPatientId`। */
    fun displayId(): String = currentPatientDisplayId.ifBlank { currentPatientId }

    fun isDoctor(): Boolean = currentRole == UserRole.DOCTOR

    /**
     * Whether the current user may fill and save clinical forms (Checkup,
     * Prescription, Medicine Slip, Investigation, Diet Chart). The web ERP's
     * saveMedicalRecord() had NO doctor-only lock — staff and the master could
     * both fill and save these — so native matches that: clinical editing is
     * allowed for any logged-in user.
     */
    fun canEditClinical(): Boolean = true

    fun applyFrom(
        roleExtra: String?, patientName: String?, patientId: String?, patientBranch: String? = null,
        patientMobile: String? = null, patientAddress: String? = null, patientAge: String? = null,
        patientSex: String? = null, patientDisease: String? = null, patientDisplayId: String? = null
    ) {
        if (!roleExtra.isNullOrBlank()) {
            currentRole = try {
                UserRole.valueOf(roleExtra.uppercase())
            } catch (e: IllegalArgumentException) {
                currentRole
            }
        }
        /* 🔴🔵🔒 V537 (২২.০৮.২০২৬, TK-রিপোর্ট) — **আগের রোগীর ওষুধ আর সঙ্গে
           যাবে না।** রোগী খোলার প্রতিটা পথ (Doctor Queue · Chamber · Global
           Search · Timeline · Follow-up) এই ফাংশনটা দিয়েই যায় — তাই পাহারাটা
           এখানেই, এক জায়গায়।
           ⛔ **একই রোগী হলে কিচ্ছু হয় না** — চলতি প্রেসক্রিপশন কখনো মুছবে না।
           ⛔ `patientId` ফাঁকা এলেও কিছু হয় না (আগের মতোই)। */
        val incomingPatient = patientId?.trim().orEmpty()
        if (incomingPatient.isNotEmpty() && incomingPatient != currentPatientId) {
            ClinicalRepository.resetForNewPatient()
            ClinicalRepository.ownerPatientId = incomingPatient

            /* 🔴🔵🔒 V538 (২২.০৮.২০২৬, TK-নির্দেশ) — **আগের রোগীর ঠিকানা/বয়স/
               লিঙ্গ/রোগ আর নতুন রোগীর কাগজে যাবে না।**

               নিচের প্রতিটা ঘর বসে `if (!…isNullOrBlank())` শর্তে — অর্থাৎ
               **নতুন রোগীর কোনো ঘর ফাঁকা এলে আগের রোগীরটাই থেকে যেত**।
               এই ঝুঁকিটা কোডেই লেখা ছিল (খাতার সারি B174, ৩০.০৭.২০২৬):
               *"`applyFrom()` `null` পেলে পুরনো (হয়তো **অন্য রোগীর**, বা
               ফাঁকা) মান ধরেই রাখে"* — তখন শুধু Timeline-এর পথটা সারানো
               হয়েছিল, বাকি পথগুলো নয়।

               যাচাই করে পাওয়া বাকি পথগুলো:
                 • Chamber — রোগীর সারি না পেলে ঠিকানা/বয়স/লিঙ্গ `""` যায়
                 • Global Search — ওগুলো লোড না হলে `""`, আর **রোগের নাম
                   সবসময়** `""`
                 • Doctor Queue → Clinical — intent-এ ঘর না থাকলে `null`

               ⇒ **রোগী বদলালে** এই ঘরগুলো আগে খালি করে দেওয়া হয়, তাই ফাঁকা
                 মান আর আগের রোগীর তথ্য টেনে আনতে পারে না।

               ⛔ **একই রোগী হলে এই ব্লক চলেই না** — B174-এর ভাল কাজটা
                  (ফাঁকা এলে আগের মান ধরে রাখা) সেখানে **অটুট**।
               ⛔ `currentRole` **ছোঁয়া হয় না** — ওটা স্টাফের ভূমিকা, রোগীর নয়।
               ⛔ নিচের বসানোর লাইনগুলো এক অক্ষরও বদলায়নি। */
            currentPatientName = ""
            currentPatientDisplayId = ""
            currentPatientBranch = ""
            currentPatientMobile = ""
            currentPatientAddress = ""
            currentPatientAge = ""
            currentPatientSex = ""
            currentPatientDisease = ""
        }
        if (!patientName.isNullOrBlank()) currentPatientName = patientName
        if (!patientId.isNullOrBlank()) currentPatientId = patientId
        // 🔒 খাতার সারি B175 — মানুষ-পড়া-যায় আইডি আলাদা করে রাখা হচ্ছে।
        if (!patientDisplayId.isNullOrBlank()) currentPatientDisplayId = patientDisplayId
        if (!patientBranch.isNullOrBlank()) currentPatientBranch = patientBranch
        if (!patientMobile.isNullOrBlank()) currentPatientMobile = patientMobile
        if (!patientAddress.isNullOrBlank()) currentPatientAddress = patientAddress
        if (!patientAge.isNullOrBlank()) currentPatientAge = patientAge
        if (!patientSex.isNullOrBlank()) currentPatientSex = patientSex
        if (!patientDisease.isNullOrBlank()) currentPatientDisease = patientDisease
        persist()   // 🔴🔒 V721
    }

    /* ═══════════════════════════════════════════════════════════════════════
       🔴🔴🔒 V721 (২৭.০৮.২০২৬, ডা. কে. এইচ. মণ্ডলের রিপোর্ট, TK-অনুমোদিত) —
       **কল এলে বা অ্যাপ বন্ধ হয়ে গেলে রোগীর তথ্য আর হারাবে না।**

       ─── আসল সমস্যা (কোড ধরে প্রমাণিত, আন্দাজ নয়) ───────────────────────
       ডাক্তার Check-up সেভ করে A4 কাগজ বানালেন, কিন্তু কাগজে —
         · নাম · Patient ID · বয়স/লিঙ্গ · মোবাইল · ঠিকানা — সব `"-"`
         · রোগ = "Piles" (কোডে বসানো fallback)
         · হেডার = **Kishanganj**, অথচ রোগী **Cooch Behar**-এর

       কারণ: উপরের ঘরগুলো **শুধু মেমরিতে** থাকত — এই ফাইলে SharedPreferences-এর
       একটাও ব্যবহার ছিল না। ফোনে কল এলে / মেমরি কম পড়লে Android অ্যাপের
       প্রসেসটাই বন্ধ করে দেয়, পরে পর্দাটা আবার খোলে — **কিন্তু এই ঘরগুলো
       ততক্ষণে ফাঁকা**। তখন —
         · `pid` ফাঁকা ⇒ `bindPatientHeader()`-এর `if (pid.isBlank()) return`
           ⇒ ডেটাবেস থেকে রোগীর তথ্য **আনার চেষ্টাই হয় না**
         · ব্রাঞ্চ ফাঁকা ⇒ BranchInfo-র byName ফাংশন চুপচাপ **Kishanganj** ধরে
           (`?: KISHANGANJ`) ⇒ ভুল ক্লিনিকের হেডার
       ⛔ রোগীর ব্রাঞ্চ ডেটাবেসে ঠিকই আছে (রেজিস্ট্রেশনে বাধ্যতামূলক) — সেটা
          নিয়ে কোনো সন্দেহ নেই; হারাত শুধু **মেমরির** কপিটা।
       প্রমাণ: TK-এর পাঠানো ছবিতেই ফোনের উপরে **"On call"** লেখা ছিল।

       ─── এখন কী হয় ───────────────────────────────────────────────────────
       তথ্যগুলো ফোনেও লেখা থাকে; প্রসেস আবার চালু হলে ফিরিয়ে আনা হয়
       (`PilesClinicApplication.onCreate()` থেকে একবার)।

       ─── 🔒 নিরাপত্তা (প্রতিটা ইচ্ছে করে বসানো) ──────────────────────────
        • **রোগী বদলালে মুছে যাওয়ার নিয়ম (V537/V538) এক অক্ষরও বদলায়নি** —
          উপরের সেই ব্লকটাই আগের মতো চলে, তারপর নতুন মান বসে, তারপর জমা হয়।
          ⇒ এক রোগীর তথ্য অন্য রোগীর কাগজে যাওয়ার পথ তৈরি হয়নি।
        • **৩০ মিনিটের সীমা** — এর চেয়ে পুরোনো জমা তথ্য কখনো ফেরানো হয় না।
          কল/সাময়িক বন্ধ কয়েক মিনিটের ব্যাপার; পরের দিন অ্যাপ খুললে পুরোনো
          রোগী **কিছুতেই** ফিরে আসবে না।
        • **মেমরি ফাঁকা না থাকলে ফেরানো হয় না** — চালু কাজের উপর কখনো লেখে না।
        • **যেকোনো গোলমালে চুপচাপ সরে দাঁড়ায়** (try/catch) — এই ফাইল কোনো
          পর্দা আটকাতে বা ভাঙতে পারে না।
        • কোনো ক্লাউড-কল নয়, কোনো নতুন কলাম নয়, Egress-এ প্রভাব শূন্য।
       ═══════════════════════════════════════════════════════════════════ */
    private const val SESSION_PREFS = "piles_clinic_role_session"
    private const val MAX_RESTORE_AGE_MS = 30L * 60L * 1000L   // ৩০ মিনিট

    private fun prefs(): android.content.SharedPreferences? = try {
        com.tkbiswas.pilesclinic.PilesClinicApplication.appContext
            ?.getSharedPreferences(SESSION_PREFS, android.content.Context.MODE_PRIVATE)
    } catch (_: Throwable) { null }

    private fun persist() {
        try {
            val e = prefs()?.edit() ?: return
            e.putString("name", currentPatientName)
                .putString("id", currentPatientId)
                .putString("displayId", currentPatientDisplayId)
                .putString("branch", currentPatientBranch)
                .putString("mobile", currentPatientMobile)
                .putString("address", currentPatientAddress)
                .putString("age", currentPatientAge)
                .putString("sex", currentPatientSex)
                .putString("disease", currentPatientDisease)
                .putLong("savedAt", System.currentTimeMillis())
                .apply()
        } catch (_: Throwable) { }
    }

    /**
     * প্রসেস আবার চালু হলে (কল এসে অ্যাপ বন্ধ হয়ে যাওয়ার পরে) একবার ডাকা হয় —
     * `PilesClinicApplication.onCreate()` থেকে।
     * ⛔ মেমরিতে ইতিমধ্যে রোগী থাকলে **কিছুই করে না**।
     */
    fun restoreIfEmpty() {
        try {
            if (currentPatientId.isNotBlank()) return
            val p = prefs() ?: return
            val savedAt = p.getLong("savedAt", 0L)
            if (savedAt <= 0L) return
            val age = System.currentTimeMillis() - savedAt
            if (age < 0L || age > MAX_RESTORE_AGE_MS) return   // পুরোনো/ঘড়ি নড়া — ফেরানো হবে না
            val id = p.getString("id", "").orEmpty()
            if (id.isBlank()) return
            currentPatientId = id
            currentPatientName = p.getString("name", "").orEmpty()
            currentPatientDisplayId = p.getString("displayId", "").orEmpty()
            currentPatientBranch = p.getString("branch", "").orEmpty()
            currentPatientMobile = p.getString("mobile", "").orEmpty()
            currentPatientAddress = p.getString("address", "").orEmpty()
            currentPatientAge = p.getString("age", "").orEmpty()
            currentPatientSex = p.getString("sex", "").orEmpty()
            currentPatientDisease = p.getString("disease", "").orEmpty()
        } catch (_: Throwable) { }
    }

    /**
     * 🔵🔒 V722 (২৭.০৮.২০২৬, ডা. কে. এইচ. মণ্ডলের রিপোর্ট, TK-অনুমোদিত) —
     * **ডাক্তার রোগের নাম বদলালে সঙ্গে সঙ্গে এখানেও বসে।**
     *
     * আগে কী হত: ডাক্তার "Probable Disease" বদলে সেভ করলে ডেটাবেসে ঠিকই
     * লেখা হত (`DoctorCheckupActivity`), কিন্তু **মেমরির এই ঘরটা পুরোনোই
     * থাকত** — আর প্রেসক্রিপশন ছাপা হয় ঠিক এই ঘর থেকে
     * (`PrescriptionOptionsStore.printLines()`)। তাই ওই একই সেশনে ছাপলে
     * **পুরোনো রোগের নামই** ছাপত (ডাক্তারের ছবিতে "PILES", অথচ তিনি
     * Fissure লিখেছিলেন)।
     *
     * ⛔ ফাঁকা নাম কখনো বসে না — তাহলে আগেরটাই থাকে।
     * ⛔ শুধু রোগের ঘরটাই; রোগীর আর কিছু ছোঁয়া হয় না।
     */
    fun updateDisease(disease: String?) {
        val d = disease?.trim().orEmpty()
        if (d.isBlank()) return
        currentPatientDisease = d
        persist()
    }

    /* ═══════════════════════════════════════════════════════════════════════
       🔴🔴🔒 V786 (২৮.০৮.২০২৬, TK-রিপোর্ট + ফটো-প্রমাণ) —
       **"Patient / - / -" — রোগীর নাম-আইডি ফাঁকা হয়ে যাওয়া।**

       ─── TK কী দেখেছেন ────────────────────────────────────────────────────
       Doctor Check-up পর্দা খোলা, Chief Complaint-এ "BLEEDING" লেখা আছে,
       অথচ উপরের কার্ডে নাম = "Patient", ID = "-", ব্রাঞ্চ = "-"।

       ─── কারণ (কোড ধরে প্রমাণিত, আন্দাজ নয়) ───────────────────────────────
       এই পর্দাগুলো রোগীকে চেনে **শুধু মেমরির** `RoleSession` থেকে — খোলার
       Intent-এ রোগীর আইডি **পাঠানোই হয় না** (৫টা খোলার জায়গাতেই যাচাই
       করা হয়েছে)। ফোনে কল এলে / মেমরি কম পড়লে Android অ্যাপের প্রসেস বন্ধ
       করে দেয়, পরে পর্দাটা **আবার নিজে থেকে খোলে** —
         · ঘরে টাইপ করা লেখা (BLEEDING) Android নিজেই ফিরিয়ে দেয়
         · কিন্তু `RoleSession` মেমরির জিনিস ⇒ **ফাঁকা**
       V721-এ ফোনে জমা রাখার ব্যবস্থা হয়েছিল, কিন্তু সেখানে **৩০ মিনিটের
       সীমা** — এর বেশি সময় পরে ফিরলে (পর্দা খোলা রেখে ফোন রেখে দিলে) আর
       ফেরে না। TK-এর ছবিটা ঠিক সেই অবস্থার।

       ─── কেন এটা বিপজ্জনক ────────────────────────────────────────────────
       আইডি ফাঁকা থাকলেও **Save চাপা যেত** — "Check-up saved." লেখা উঠত,
       অথচ সারিটা যেত ফাঁকা আইডিতে, অর্থাৎ **ডাক্তারের লেখা হারিয়ে যেত**
       আর কেউ টেরও পেত না।

       ─── এখন কী হয় ───────────────────────────────────────────────────────
       ১. রোগীর পরিচয় পর্দার নিজের **Bundle**-এও রাখা হয় (`saveTo`)। Bundle
          প্রসেস মরলেও বাঁচে এবং **কোনো সময়সীমা নেই** — তাই ৩০ মিনিটের
          গণ্ডি আর সমস্যা নয়।
       ২. পর্দা আবার খুললে মেমরি ফাঁকা থাকলে সেখান থেকেই ফেরানো হয়
          (`restoreFrom`)।
       ৩. তবুও আইডি না পেলে ওই পর্দায় **Save বন্ধ** — মিথ্যা "saved" আর নয়।

       ─── 🔒 নিরাপত্তা ────────────────────────────────────────────────────
        • **মেমরিতে রোগী থাকলে কিচ্ছু ফেরানো হয় না** — চালু রোগীর উপর কখনো
          লেখা হয় না, তাই পুরোনো পর্দা থেকে অন্য রোগী ফিরে আসার পথ নেই।
        • ফেরানো হয় `applyFrom()` দিয়েই — V537/V538-এর রোগী-বদল পাহারা
          এক অক্ষরও এড়ানো হয়নি।
        • যেকোনো গোলমালে চুপচাপ সরে দাঁড়ায় (try/catch)।
        • কোনো ক্লাউড-কল নয়, Egress-এ প্রভাব শূন্য।
       ═══════════════════════════════════════════════════════════════════ */
    private const val BKEY = "rolesession_patient_"

    /** পর্দার নিজের Bundle-এ রোগীর পরিচয় রেখে দেওয়া (onSaveInstanceState)। */
    fun saveTo(out: android.os.Bundle?) {
        try {
            val b = out ?: return
            if (currentPatientId.isBlank()) return
            b.putString(BKEY + "id", currentPatientId)
            b.putString(BKEY + "name", currentPatientName)
            b.putString(BKEY + "displayId", currentPatientDisplayId)
            b.putString(BKEY + "branch", currentPatientBranch)
            b.putString(BKEY + "mobile", currentPatientMobile)
            b.putString(BKEY + "address", currentPatientAddress)
            b.putString(BKEY + "age", currentPatientAge)
            b.putString(BKEY + "sex", currentPatientSex)
            b.putString(BKEY + "disease", currentPatientDisease)
            b.putString(BKEY + "role", currentRole.name)
        } catch (_: Throwable) { }
    }

    /** প্রসেস মরে পর্দা আবার খুললে ফিরিয়ে আনা (onCreate)। ⛔ মেমরিতে রোগী
     *  থাকলে **কিছুই করে না**। */
    fun restoreFrom(b: android.os.Bundle?) {
        try {
            if (currentPatientId.isNotBlank()) return
            val s = b ?: return
            val id = s.getString(BKEY + "id").orEmpty()
            if (id.isBlank()) return
            applyFrom(
                roleExtra = s.getString(BKEY + "role"),
                patientName = s.getString(BKEY + "name"),
                patientId = id,
                patientBranch = s.getString(BKEY + "branch"),
                patientMobile = s.getString(BKEY + "mobile"),
                patientAddress = s.getString(BKEY + "address"),
                patientAge = s.getString(BKEY + "age"),
                patientSex = s.getString(BKEY + "sex"),
                patientDisease = s.getString(BKEY + "disease"),
                patientDisplayId = s.getString(BKEY + "displayId")
            )
        } catch (_: Throwable) { }
    }

    /**
     * 🔴🔒 V786 — রোগী চেনা না গেলে সেভ করতে দেওয়া যাবে না।
     * `true` ফিরলে ডাকা জায়গাটা **থেমে যাবে** (রোগীকে বার্তা দেখিয়ে)।
     */
    fun blockIfNoPatient(ctx: android.content.Context): Boolean {
        if (currentPatientId.isNotBlank()) return false
        try {
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("⚠️ Patient not loaded")
                .setMessage(
                    "This screen lost the patient (the phone closed the app in the " +
                    "background — a call, or low memory).\n\nNothing was saved. Please " +
                    "open the patient again from the list and re-enter."
                )
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show().also {
                    com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it)
                    com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it)
                }
        } catch (_: Throwable) { }
        return true
    }

    /** লগ-আউটে জমা তথ্য মুছে ফেলা — অন্য কেউ লগইন করলে যেন কিছু না থাকে। */
    fun clearPersisted() {
        try { prefs()?.edit()?.clear()?.apply() } catch (_: Throwable) { }
    }
}
