package com.tkbiswas.pilesclinic.clinical

/**
 * In-memory store for Phase 4 clinical modules.
 *
 * NOTE (pending item, not Phase 4 scope): this is intentionally session-only /
 * in-memory, mirroring how the existing ERP's medicine/test/diet reference lists
 * are used as templates that the doctor edits per patient. Real persistence
 * (Supabase table + linking to an actual patient record) is pending backend
 * wiring once native Login/Dashboard/Patient records are connected here.
 */
object ClinicalRepository {

    // ---- Reference lists (exact web ERP data: MEDICINES / SLIP_MEDICINES /
    //      BLOOD_TESTS / DIET, with the RX_DOSE_MAP standard doses) ----

    val commonMedicines: List<String> = listOf(
        "Arshakuthar Rasa", "Kankayan Vati Arsha", "Bolbadha Rasa",
        "Raktasthambhak Vati", "Rasamanikya", "Sameerpanag Rasa",
        "Arogyadhadi kashayam", "Septilin", "Abhyaristram",
        "Jatyadi Ghritam", "Abhayadi Modak", "Qurs Alkali",
        "Habb-e-Kabid Naushadri", "Nityam", "Trivrilehan"
    )

    val slipMedicines: List<String> = listOf(
        "Omez DSR", "Rabekind DSR", "Pantop DSR", "Aciloc 300", "P-40",
        "Voveran SR 100", "Voveran 50", "Zerodol SP", "Clavam 625",
        "Monocef O CV 200", "Taxim O CV 200", "Taxim 200", "Roclav 625 LB",
        "Bicosul", "Zincovit", "RB Tone 200 ml", "Celin 500",
        "Metrogyl P 2% 20 gm", "Betadine 5% 25 gm"
    )

    /** Standard dose per medicine (web RX_DOSE_MAP). rxDoseFor() falls back to "As advised". */
    private val rxDoseMap: Map<String, String> = mapOf(
        "Arshakuthar Rasa" to "2-0-2 After Food",
        "Kankayan Vati Arsha" to "2-0-2 After Food",
        "Bolbadha Rasa" to "1-0-1 After Food",
        "Raktasthambhak Vati" to "1-0-1 After Food",
        "Rasamanikya" to "1-0-1 After Food",
        "Sameerpanag Rasa" to "1-0-1 After Food",
        "Arogyadhadi kashayam" to "15 ml with equal water twice daily after food",
        "Septilin" to "1-0-1 After Food",
        "Abhyaristram" to "15 ml with equal water twice daily after food",
        "Jatyadi Ghritam" to "Local application Morning & Evening",
        "Abhayadi Modak" to "0-0-1 After Food",
        "Qurs Alkali" to "2-0-2 Before Food",
        "Habb-e-Kabid Naushadri" to "1-0-1 Before Food",
        "Nityam" to "1 at bedtime",
        "Trivrilehan" to "1 tsp at bedtime with warm water",
        "Omez DSR" to "1-0-0 Before Food",
        "Rabekind DSR" to "1-0-0 Before Food",
        "Pantop DSR" to "1-0-0 Before Food",
        "Aciloc 300" to "0-0-1 After Food",
        "P-40" to "1-0-0 Before Food",
        "Voveran SR 100" to "0-0-1 After Food",
        "Voveran 50" to "1-0-1 After Food",
        "Zerodol SP" to "1-0-1 After Food",
        "Clavam 625" to "1-0-1 After Food",
        "Monocef O CV 200" to "1-0-1 After Food",
        "Taxim O CV 200" to "1-0-1 After Food",
        "Taxim 200" to "1-0-1 After Food",
        "Roclav 625 LB" to "1-0-1 After Food",
        "Bicosul" to "1-0-0 After Food",
        "Zincovit" to "1-0-0 After Food",
        "RB Tone 200 ml" to "2 tsp twice daily after food",
        "Celin 500" to "1-0-0 After Food",
        "Metrogyl P 2% 20 gm" to "Local application twice daily",
        "Betadine 5% 25 gm" to "Local application as advised"
    )

    /** TK-REPORTED BUG FIX (2026-07-16): RX_DOSE_MAP values like "1-0-1 After
     *  Food" were going entirely into the single `dosage` field, leaving the
     *  print table's separate WHEN column blank ("-") while DOSE showed the
     *  whole combined text. This splits a "<numbers> Before/After Food" style
     *  dose into (dosePart, whenPart). Anything that doesn't match this exact
     *  pattern (free-text doses like "Local application as advised") is
     *  returned unchanged with an empty second part -- same as before. */
    fun splitDoseAndFrequency(dose: String): Pair<String, String> {
        val trimmed = dose.trim()
        val numeric = Regex("^([\\d\\-]+)\\s+(After Food|Before Food)$", RegexOption.IGNORE_CASE)
            .find(trimmed)
        if (numeric != null) return Pair(numeric.groupValues[1], numeric.groupValues[2])
        // TK APPROVED (2026-07-16): "Local application ..." doses don't fit
        // the narrow DOSE column either -- shorten to "L/A" and move the
        // rest ("as advised" / "twice daily" / etc.) to WHEN, capitalized.
        val localApp = Regex("^Local application\\s+(.+)$", RegexOption.IGNORE_CASE).find(trimmed)
        if (localApp != null) {
            val rest = localApp.groupValues[1].trim()
            return Pair("L/A", rest.replaceFirstChar { it.uppercase() })
        }
        return Pair(dose, "")
    }

    /** Web rxDoseFor(): remembered (edited) dose first, else standard RX_DOSE_MAP, else "As advised".
     *  Mirrors web rxDoseMemory: once TK edits a medicine's dose it stays the default
     *  for that medicine everywhere until changed again. */
    private var dosePrefs: android.content.SharedPreferences? = null

    fun attachDoseMemory(context: android.content.Context) {
        if (dosePrefs == null) {
            dosePrefs = context.applicationContext
                .getSharedPreferences("rxDoseMemory", android.content.Context.MODE_PRIVATE)
        }
    }

    /* 🔵 V548 (২২.০৮.২০২৬, TK-নির্দেশ ছাপা কাগজসহ: *"মেডিসিন When-এর ঘর ফাঁকা কেন থাকবে"*)।
       **আসল কারণ (কোড ধরে):** ওষুধের মনে-রাখা ডোজ একটাই লেখায় জমা থাকে
       ("2-0-2 After Food")। কিন্তু রিভিউ-তালিকার Dose ঘরে হাত পড়লে
       `rememberRxDose` **শুধু ডোজটুকুই** ("2-0-2") জমা করে — "After Food" অংশটা
       মুছে যায়। তার পর থেকে ওই ওষুধের When চিরকাল ফাঁকা ছাপা হত।
       **এই ঘরটা** প্রজেক্টের নিজের প্রমাণিত তালিকা (`rxDoseMap`) থেকে ওষুধটার
       **আদত When** ফিরিয়ে দেয় — শুধু যখন মনে-রাখা লেখায় When নেই।
       ⛔ তালিকায় নেই এমন ওষুধে ফাঁকাই থাকে — **কিছুই বানানো হয় না**।
       ⛔ জমা রাখার নিয়ম/ঘর একটুও বদলায়নি, তাই পুরোনো কিছু নষ্ট হওয়ার পথ নেই। */
    fun rxWhenFor(name: String): String =
        splitDoseAndFrequency(rxDoseMap[name.trim()].orEmpty()).second

    fun rxDoseFor(name: String): String {
        val key = name.trim()
        dosePrefs?.getString(key, null)?.let { if (it.isNotBlank()) return it }
        return rxDoseMap[key] ?: "As advised"
    }

    /** Web rememberRxDose(): save the edited dose as the new default for this medicine. */
    fun rememberRxDose(name: String, dose: String) {
        val key = name.trim()
        if (key.isEmpty() || dose.isBlank()) return
        // 🟢 V956: the dose also goes into its own box, so the When box beside it
        // is never wiped by a dose edit (see rxDoseWhenFor below).
        dosePrefs?.edit()?.putString(key, dose)?.putString("dose_$key", dose.trim())?.apply()
    }

    /* ══════════════════════════════════════════════════════════════════════
       🍯🔒 V956 (০১.০৯.২০২৬, TK-নির্দেশ) — TK: *"অনেক সময় কোন মেডিসিন মধু দিয়ে
       খেতে বলতে হয়, মাখন দিয়ে খেতে বলতে হয় … একবার সেট করে থাকলে সারা জীবন
       যেন একই রকম থাকে যতক্ষণ না ভবিষ্যতে আমি পরিবর্তন না করি"*।

       ─── আসল কারণ (কোডে মেপে দেখা, আন্দাজ নয়) ──────────────────────────────
       ওষুধের ডোজ ও When **একটাই লেখায়** জমা হত ("2-0-2 After Food")। ফেরত
       পড়ার সময় `splitDoseAndFrequency` শুধু হুবহু "After Food"/"Before Food"
       চিনত। তাই ডাক্তার "After Food with Honey" লিখলে পরের বার সেটা ভেঙে
       পড়ত না — পুরো লেখাটা Dose-এর ঘরে ঢুকে যেত আর When আবার "After Food"
       হয়ে ফিরে আসত। অর্থাৎ মধু/মাখনের কথা **কখনোই টিকত না**।

       ─── স্থায়ী সমাধান ────────────────────────────────────────────────────
       Dose ও When এখন **নিজের নিজের আলাদা ঘরে** জমা থাকে (`dose_<নাম>` ও
       `when_<নাম>`)। যা খুশি লেখা যায় — "After Food with Honey", "With Butter",
       "খালি পেটে" — হুবহু সেটাই ফিরে আসে, TK না বদলানো পর্যন্ত।
       ⛔ পুরোনো জোড়া-লেখা ঘরটা অক্ষত রাখা হয়েছে; নতুন ঘর ফাঁকা থাকলে আগের
          নিয়মেই কাজ চলে — তাই এখনকার কোনো ডিফল্ট নষ্ট হয় না।
       ☁️ ক্লাউডের `medicine_defaults` টেবিলে `whenText` কলামটা **আগে থেকেই**
          আলাদা ছিল, তাই নতুন কোনো টেবিল/কলাম লাগেনি।
       ══════════════════════════════════════════════════════════════════════ */
    fun rememberRxWhen(name: String, whenText: String) {
        val key = name.trim()
        if (key.isEmpty() || whenText.isBlank()) return
        dosePrefs?.edit()?.putString("when_$key", whenText.trim())?.apply()
    }

    /** V956: the medicine's remembered Dose + When, each from its own box.
     *  Falls back to the old glued-together value (and then to the project's
     *  own list) whenever the new boxes are still empty. */
    fun rxDoseWhenFor(name: String): Pair<String, String> {
        val key = name.trim()
        val savedDose = dosePrefs?.getString("dose_$key", null).orEmpty().trim()
        val savedWhen = dosePrefs?.getString("when_$key", null).orEmpty().trim()
        val fallback = splitDoseAndFrequency(rxDoseFor(key))
        return Pair(
            savedDose.ifBlank { fallback.first },
            savedWhen.ifBlank { fallback.second.ifBlank { rxWhenFor(key) } }
        )
    }

    /** TK APPROVED (2026-07-15): medicine "Type" (Tab/Cap/Syp/Oint/Inj/Other) shown
     *  as a small box before the name on printed Prescription/Medicine Slip. TK sets
     *  this once per medicine and it is remembered forever after (same pattern as
     *  Dose above) so it never needs to be picked again unless TK changes it. Claude
     *  never guesses a medicine's type — it starts blank until TK picks one. */
    val MEDICINE_TYPES = listOf("Tab", "Cap", "Syp", "Oint", "Cream", "Drops", "Inj", "Powder", "Churna", "Taila", "Vati", "Other")

    fun rxTypeFor(name: String): String {
        val key = name.trim()
        dosePrefs?.getString("type_$key", null)?.let { if (it.isNotBlank()) return it }
        return when (key) {
            "Arshakuthar Rasa", "Kankayan Vati Arsha", "Abhayadi Modak",
            "Qurs Alkali", "Habb-e-Kabid Naushadri" -> "Tab"
            "Jatyadi Ghritam" -> "Oint"
            else -> ""
        }
    }

    val fixedCommonPrescription = linkedSetOf(
        "Arshakuthar Rasa", "Kankayan Vati Arsha", "Abhayadi Modak",
        "Jatyadi Ghritam", "Qurs Alkali", "Habb-e-Kabid Naushadri"
    )

    fun rememberRxType(name: String, type: String) {
        val key = name.trim()
        if (key.isEmpty()) return
        dosePrefs?.edit()?.putString("type_$key", type)?.apply()
    }

    /** TK-REQUESTED (2026-07-19): "Days" (how many days a medicine is taken)
     *  now remembered per medicine forever, exactly like Dose and Type above —
     *  once TK types e.g. "10 days" for a medicine, that becomes the default
     *  for that medicine everywhere until TK changes it again. Starts at the
     *  standard DEFAULT_DURATION ("5 days") until first edited. */
    fun rxDaysFor(name: String): String {
        val key = name.trim()
        dosePrefs?.getString("days_$key", null)?.let { if (it.isNotBlank()) return it }
        return DEFAULT_DURATION
    }

    fun rememberRxDays(name: String, days: String) {
        val key = name.trim()
        if (key.isEmpty() || days.isBlank()) return
        dosePrefs?.edit()?.putString("days_$key", days)?.apply()
    }

    /** V331: one permanent default = Type + Dose + When + Days.  The picker
     * still uses the same local keys/design; these helpers only make the four
     * values atomic and allow a safe cloud refresh after reinstall/new phone. */
    fun rememberPermanentDefault(
        context: android.content.Context,
        name: String,
        type: String,
        dose: String,
        whenText: String,
        days: String
    ) {
        MedicineDefaultsCloudRepository.rememberAndSync(context, name, type, dose, whenText, days)
    }

    fun applyPermanentDefault(
        name: String,
        type: String,
        dose: String,
        whenText: String,
        days: String,
        updatedAt: String
    ): Boolean {
        val key = name.trim()
        val prefs = dosePrefs ?: return false
        if (key.isBlank()) return false
        val stampKey = "cloud_updated_$key"
        val localStamp = prefs.getString(stampKey, "").orEmpty()
        // ISO UTC timestamps sort in time order. Never let an older delayed
        // cloud write replace a newer change already made on this phone.
        if (updatedAt.isNotBlank() && localStamp.isNotBlank() && updatedAt < localStamp) return false
        val combinedDose = listOf(dose.trim(), whenText.trim()).filter { it.isNotBlank() }.joinToString(" ")
        val before = listOf(
            prefs.getString("type_$key", "").orEmpty(),
            prefs.getString(key, "").orEmpty(),
            prefs.getString("days_$key", "").orEmpty(),
            prefs.getString("when_$key", "").orEmpty()   // 🟢 V956
        )
        val editor = prefs.edit()
        if (type.isNotBlank()) editor.putString("type_$key", type.trim())
        if (combinedDose.isNotBlank()) editor.putString(key, combinedDose)
        // 🟢 V956: Dose ও When আলাদা ঘরেও বসে — তাই "with Honey"-এর মতো লেখা টেকে।
        if (dose.isNotBlank()) editor.putString("dose_$key", dose.trim())
        if (whenText.isNotBlank()) editor.putString("when_$key", whenText.trim())
        if (days.isNotBlank()) editor.putString("days_$key", days.trim())
        if (updatedAt.isNotBlank()) editor.putString(stampKey, updatedAt)
        editor.apply()
        val after = listOf(
            rxTypeFor(key), rxDoseFor(key), rxDaysFor(key),
            prefs.getString("when_$key", "").orEmpty()   // 🟢 V956
        )
        return before != after
    }

    // ---- TK APPROVED (2026-07-15): "learned" medicine names ----
    // A name typed into the Prescription/Medicine Slip search box (or added via
    // "Add Outside") is remembered here, per list type, so it shows up next time
    // someone searches for it — but it is NEVER added to the default/base list
    // that is shown before any search. Claude does not invent medicine names;
    // only names TK/staff actually typed and saved are ever remembered.
    private fun learnedKey(listType: String) = "learned_medicines_$listType"

    fun learnMedicine(name: String, listType: String) {
        val key = name.trim()
        if (key.isEmpty()) return
        val prefKey = learnedKey(listType)
        val existing = dosePrefs?.getStringSet(prefKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (existing.add(key)) {
            // getStringSet's returned set must not be mutated in place per SharedPreferences
            // contract, so write back a fresh copy.
            dosePrefs?.edit()?.putStringSet(prefKey, existing)?.apply()
        }
    }

    fun learnedMedicines(listType: String): List<String> =
        (dosePrefs?.getStringSet(learnedKey(listType), emptySet()) ?: emptySet()).sorted()

    // TK APPROVED (2026-07-15): "Common Prescription" / "Common Medicine Slip"
    // -- same remembered-SET pattern as Common Blood Test above. Saving the
    // whole medicine list given to one patient as the new default combo, so
    // the next patient can get the exact same set with one tap. Kept as two
    // separate stores (prescription vs slip) -- same root-cause fix reasoning
    // as currentPrescription/currentSlip being kept separate. Only remembers
    // medicine NAMES; each name's own dose/type is already remembered
    // separately via rememberRxDose/rememberRxType, so applying the common set
    // always uses whatever that medicine's own last dose/type was.
    fun saveCommonPrescription(names: Set<String>) {
        dosePrefs?.edit()?.putStringSet("common_prescription", names)?.apply()
    }

    fun getCommonPrescription(): Set<String> =
        dosePrefs?.getStringSet("common_prescription", emptySet()) ?: emptySet()

    fun saveCommonMedicineSlip(names: Set<String>) {
        dosePrefs?.edit()?.putStringSet("common_medicine_slip", names)?.apply()
    }

    fun getCommonMedicineSlip(): Set<String> =
        dosePrefs?.getStringSet("common_medicine_slip", emptySet()) ?: emptySet()

    /** Base list (unchanged) + anything learned for that list type — used only
     *  when the person searches; the base list alone is what shows by default. */
    fun searchableMedicines(listType: String): List<String> {
        val base = if (listType == "ayurvedic") commonMedicines else slipMedicines
        return base + learnedMedicines(listType).filter { it !in base }
    }

    /** Web default prescription duration when none is chosen. */
    const val DEFAULT_DURATION = "5 days"

    val commonInvestigations: List<String> = listOf(
        "CBC", "ESR", "HB", "HIV", "VDRL", "Sugar", "LFT", "Lipid Profile",
        "S. Creatinine", "Semen Analysis", "USG Scrotal", "Whole Abdomen",
        "Lower Abdomen", "MRI Fistulogram", "FNAC", "Biopsy"
    )

    /** TK APPROVED (2026-07-15): full categorised Blood Test / Investigation
     *  catalogue, transcribed test-by-test from TK's physical lab reference
     *  slip (verified count per category against the photo — Hematology 12,
     *  Bio-Chemistry 30, Immunology 14, Special Test 29, Urine 5, Stool 2,
     *  Semen 1 = 93 from the photo). CBC and the 4 Imaging tests were already
     *  in the app before this photo and are kept as their own items so
     *  nothing already in use is lost. English only, no items invented. */
    data class InvestigationCategory(val name: String, val emoji: String, val tests: List<String>)

    val investigationCategories: List<InvestigationCategory> = listOf(
        InvestigationCategory("Hematology", "🩸", listOf(
            "CBC", "Hemoglobin (HB)", "TLC", "DLC", "ESR", "RBC Count",
            "Reticulocyte Count", "Platelets Count", "BT-CT, PT, PTT",
            "Malaria Parasites", "Band Cell", "Toxic Granules",
            "Complete Hemogram (PCV, MCV, MCH, MCHC)"
        )),
        InvestigationCategory("Bio-Chemistry", "🧪", listOf(
            "Blood Sugar", "Urea", "Creatinine", "Uric Acid", "Calcium",
            "Amylase", "Lipase", "BT-CT, PT, PTT", "Testosterone", "SGPT",
            "SGOT", "Bilirubin", "Alkaline P. Tase", "Cholestrol",
            "Lipid Profile", "Sodium", "Potassium", "Chloride & Bicarbonate",
            "Lithium", "Total Protein", "Albumin", "Ag Ration", "LFT", "KFT",
            "Creatinence", "Clearance", "Micro Albumin", "Cratine Ratio",
            "CPKMB", "Tripomine I"
        )),
        InvestigationCategory("Immunology", "🛡️", listOf(
            "Blood Group ABO", "HBs AG (Slide/Elisa)", "HCV", "HIV", "MP KIT",
            "Widal", "VDRL (R, P, R)", "A.S.O. Titre", "RA Test", "C.R.P.",
            "Aldehyde", "Pregnancy Test", "Mantoux Test (5/10 TU)",
            "K-39 for kala-ajar (PF+PV Antigen)"
        )),
        InvestigationCategory("Special Test", "⭐", listOf(
            "T3, T4, TSH, Free T3, T4", "Free T3, LH, FSH", "Prolactin",
            "Torch Test", "TB for Elisa IgG/IgmIgA", "Anti Cardiolipin",
            "ADA", "PAP Smear", "Allergy Profile",
            "CSF, Synovia Fluid (Pleural, Ascitic Fluid)", "Fluid Analysis",
            "Histopathology (Biopsy)", "FNAC", "Blood Culture",
            "Prepheral Blood Smear", "Toxoplasma, IgG, Igm", "Electrophoresis",
            "Pus Culture", "Indirect Coomb Test", "Micro Albumin (Tubi)",
            "CRP (Turbi)", "G-6 Pd", "Alpha Feto Protein",
            "B-HCG (Quantitative)", "PSA", "ANF", "DS DNA", "ANA", "CCP"
        )),
        InvestigationCategory("Urine", "🚻", listOf(
            "R.E./M.E.", "Bile Salt/Pigment/Acetone", "Benz-Zone's Protein",
            "Culture & Sensitivity", "Detection"
        )),
        InvestigationCategory("Stool", "💩", listOf(
            "R.E./M.E./Occult Blood", "Culture & Sensitivity (C/S)"
        )),
        InvestigationCategory("Semen", "🧬", listOf(
            "Semen Analysis"
        )),
        InvestigationCategory("Imaging", "🖥️", listOf(
            "USG Scrotal", "Whole Abdomen", "Lower Abdomen", "MRI Fistulogram"
        ))
    )

    // ---- TK APPROVED (2026-07-15): "Common Blood Test" — remembers the exact
    // set of tests last saved for a patient. Tapping "Apply Common Blood Test"
    // re-checks that same set. Blank until TK saves a selection for the first
    // time; every later Save overwrites it with the new selection. ----
    private var investPrefs: android.content.SharedPreferences? = null

    fun attachInvestMemory(context: android.content.Context) {
        if (investPrefs == null) {
            investPrefs = context.applicationContext
                .getSharedPreferences("commonBloodTestMemory", android.content.Context.MODE_PRIVATE)
        }
    }

    fun saveCommonBloodTest(names: Set<String>) {
        investPrefs?.edit()?.putStringSet("names", names)?.apply()
    }

    fun getCommonBloodTest(): Set<String> =
        investPrefs?.getStringSet("names", emptySet()) ?: emptySet()

    // 🔒 TK-এর নির্দেশ (01.08.2026): "Previous Patient Blood Test" (উপরের
    // getCommonBloodTest/saveCommonBloodTest, শেষ সেভ করা রোগীর তালিকা)
    // থেকে সম্পূর্ণ আলাদা — এটা একটা স্থায়ী/ফিক্সড তালিকা, রোগী বদলালেও
    // বদলায় না। TK নিজে ৭টা নাম দিয়েছেন (01.08.2026)।
    val commonBloodTestFixed: List<String> = listOf(
        "CBC", "ESR", "HB", "SUGAR", "HIV", "VDRL", "LIPID PROFILE"
    )

    // TK APPROVED (2026-07-15): expanded with more common piles-care advice, and
    // each item now shows in three languages (English / বাংলা / हिन्दी) on
    // separate lines so any patient can read it regardless of language --
    // requested to look more complete and professional. Same list is shared by
    // the Patient/Visit card Diet Chart screen and the Print Center Walk-in
    // form, so both get this update identically.
    val dietAllowed: List<String> = listOf(
        "Drink sufficient water\nপর্যাপ্ত জল পান করুন\nपर्याप्त पानी पिएं",
        "High fiber food\nআঁশযুক্ত খাবার খান\nफाइबरयुक्त भोजन करें",
        "Regular walking\nনিয়মিত হাঁটাহাঁটি করুন\nनियमित सैर करें",
        "Warm sitz bath\nগরম জলে সিটজ বাথ নিন\nगर्म पानी से सिट्ज़ बाथ लें",
        "Fresh fruits & vegetables\nটাটকা ফল ও সবজি খান\nताज़े फल और सब्जियां खाएं",
        "Curd / Buttermilk\nদই বা ঘোল খান\nदही या छाछ लें",
        "Whole grains\nগোটা শস্য জাতীয় খাবার খান\nसाबुत अनाज खाएं",
        "Adequate rest & sleep\nপর্যাপ্ত বিশ্রাম ও ঘুম\nपर्याप्त आराम और नींद लें"
    )

    val dietAvoid: List<String> = listOf(
        "Avoid spicy/oily food\nঝাল ও তেলযুক্ত খাবার এড়িয়ে চলুন\nमसालेदार और तैलीय भोजन से बचें",
        "Avoid constipation\nকোষ্ঠকাঠিন্য এড়িয়ে চলুন\nकब्ज़ से बचें",
        "Avoid alcohol & smoking\nমদ্যপান ও ধূমপান এড়িয়ে চলুন\nशराब और धूम्रपान से बचें",
        "Avoid prolonged sitting\nদীর্ঘক্ষণ বসে থাকা এড়িয়ে চলুন\nलंबे समय तक बैठने से बचें",
        "Avoid straining during bowel movement\nমলত্যাগের সময় জোর করবেন না\nमल त्याग के समय ज़ोर न लगाएं",
        "Avoid red meat / heavy non-veg\nলাল মাংস/ভারী আমিষ এড়িয়ে চলুন\nलाल मांस/भारी मांसाहार से बचें"
    )

    // ---- Session working data (per current patient, cleared when app restarts) ----

    var lastCheckup: CheckupRecord? = null
    /** Prescription (Rx) screen — Ayurvedic medicines only. */
    val currentPrescription: MutableList<MedicineEntry> = mutableListOf()
    /** Medicine Slip screen — Allopathic medicines only.
     *  ROOT-CAUSE FIX (2026-07-15): previously Medicine Slip reused
     *  currentPrescription, so medicines added on one screen silently
     *  appeared on the other. Now fully separate lists. */
    val currentSlip: MutableList<MedicineEntry> = mutableListOf()
    val currentInvestigations: MutableList<InvestigationEntry> = mutableListOf()
    val currentDiet: MutableList<DietEntry> = mutableListOf()
    val visitHistory: MutableList<ClinicalVisit> = mutableListOf()

    fun addVisit(type: String, summary: String, role: UserRole) {
        visitHistory.add(
            0,
            ClinicalVisit(
                patientName = RoleSession.currentPatientName,
                type = type,
                summary = summary,
                timestamp = System.currentTimeMillis(),
                doneByRole = role
            )
        )
    }

    /**
     * 🔴🔵🔒 V537 (২২.০৮.২০২৬, TK-রিপোর্ট — **রোগী-নিরাপত্তার গুরুতর বাগ**):
     * *"আমি শুধু দুটো মেডিসিন দিয়েছি, কিন্তু প্রিন্টে আগের রোগীর মেডিসিনগুলো
     * অটোমেটিক চলে এল।"*
     *
     * **আসল কারণ:** নিচের `currentPrescription` একটাই **অ্যাপ-জোড়া তালিকা**
     * (`object` = পুরো অ্যাপে একটাই)। রোগী বদলালে এটা **খালি হত না**।
     * `resetForNewPatient()` লেখা ছিল, কিন্তু ডাকা হত **একমাত্র Dashboard-এর
     * Print টাইল থেকে** (`DashboardActivity.kt:149`) — CHECK-UP / Prescription
     * খোলার সময় নয়। ⇒ এক রোগীর প্রেসক্রিপশনের পর অন্য রোগী খুললে
     * **আগের রোগীর ওষুধগুলো তালিকাতেই থেকে যেত**, আর নতুন রোগীর নামে সেভ ও
     * প্রিন্ট হয়ে যেত।
     *
     * ⚠️ ঠিক এই শ্রেণির বাগ **০৫.০৮.২০২৬-এ `lastCheckup`-এ ধরা পড়েছিল**
     *    (খাতার সারি B437) — সেখানে লেখাই আছে *"`resetForNewPatient()` ফাংশন
     *    লেখা ছিল কিন্তু কোথাও ডাকাই হত না"*। তখন Checkup-এর দিকটা সারানো
     *    হয়েছিল, **কিন্তু ওষুধের তালিকাটা একই দোষ নিয়ে রয়ে গিয়েছিল।**
     *
     * **সমাধান (দুটো আলাদা পাহারা, একটা ব্যর্থ হলেও অন্যটা ধরবে):**
     *  ১) `RoleSession.applyFrom()` — রোগী বদলালেই তালিকা খালি (কেন্দ্রীয়,
     *     রোগী খোলার **প্রতিটা** পথ ওখান দিয়েই যায়)।
     *  ২) নিচের `ownerPatientId` — তালিকাটা **কার**, সেটা মনে রাখা হয়; অন্য
     *     রোগীর পর্দায় সেটা নিজে থেকেই খালি হয়ে যায়।
     */
    /** তালিকাটা এই মুহূর্তে কোন রোগীর — ফাঁকা মানে কারও নয়। */
    var ownerPatientId: String = ""

    /** 🔵 V537: তালিকা এই রোগীর না হলে খালি করে দেয়। ⛔ একই রোগী হলে
     *  কিচ্ছু হয় না — চলতি কাজ কখনো মুছবে না। */
    fun ensureListsBelongTo(patientId: String) {
        val want = patientId.trim()
        if (want.isEmpty()) return
        if (ownerPatientId == want) return
        resetForNewPatient()
        ownerPatientId = want
    }

    fun resetForNewPatient() {
        ownerPatientId = ""
        lastCheckup = null
        currentPrescription.clear()
        currentSlip.clear()
        currentInvestigations.clear()
        currentDiet.clear()
        // visitHistory intentionally preserved across a session for the demo view
    }
}
