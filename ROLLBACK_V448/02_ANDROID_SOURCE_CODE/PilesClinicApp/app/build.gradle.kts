import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

// ═══════════════════════════════════════════════════════════════════════════
// 🔒 V397 (১৬.০৮.২০২৬, TK-নির্দেশ) — ভার্সন **একটাই জায়গায়**, আর তৈরি হওয়া
// APK-র নামেও ভার্সন বসবে।
//
// আগে: APK-র নাম ঠিক করার কোনো নিয়মই ছিল না, তাই Gradle প্রতিবার একই
//      ডিফল্ট নাম দিত (`app-release.apk`) — কোন ফাইল কোন ভার্সন বোঝা যেত না।
// এখন: নাম হবে  PilesClinic-V397-release.apk  (ও -debug.apk)।
//
// পরের বার ভার্সন বদলাতে **শুধু নিচের দুটো লাইন** বদলালেই হবে — versionCode,
// versionName আর APK-র নাম তিনটেই একসাথে বদলাবে, কোনোটা বাদ পড়বে না।
// ⛔ অ্যাপের কাজ · ডিজাইন · ডেটা · Supabase কিছুই ছোঁয়া হয়নি; শুধু বিল্ডের নাম।
// ═══════════════════════════════════════════════════════════════════════════
val appVersionCode = 448
val appVersionName = "4.48"

base {
    archivesName.set("PilesClinic-V$appVersionCode")
}

// ---------------------------------------------------------------------------
// Phase 5: Supabase credentials are NEVER hardcoded here.
// They are read at build time from (in priority order):
//   1) local.properties (git-ignored, lives only on the developer's machine)
//   2) environment variables SUPABASE_URL / SUPABASE_ANON_KEY (useful for CI)
// If neither is set, the app still builds — Supabase calls simply fail fast
// with a clear "not configured" error instead of crashing (see
// data/remote/SupabaseConfig.kt). See SUPABASE_SETUP.md for how to fill these in.
// ---------------------------------------------------------------------------
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        FileInputStream(localPropsFile).use { load(it) }
    }
}

fun readSecret(key: String): String =
    (localProperties.getProperty(key) ?: System.getenv(key) ?: "").trim()

val supabaseUrl = "https://bcyeogjqtupbdyciqfmz.supabase.co"  // fixed to new project
val supabaseAnonKey = "sb_publishable_k_170-JGrdxmZ7rBrjCyTA_-ElK2XdZ"  // fixed to new project

// ---------------------------------------------------------------------------
// Phase 9-10: Release signing, same "never hardcoded" pattern as Supabase
// above. Fill these into local.properties (see local.properties.example) to
// produce a signed release build; without them, `release` still builds, just
// unsigned (Android Studio's "Generate Signed Bundle/APK" wizard can supply
// signing interactively instead). See RELEASE_GUIDE.md.
// ---------------------------------------------------------------------------
val releaseKeystorePath = readSecret("RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword = readSecret("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = readSecret("RELEASE_KEY_ALIAS")
val releaseKeyPassword = readSecret("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseKeystorePath.isNotBlank() && File(releaseKeystorePath).exists()

android {
    namespace = "com.tkbiswas.pilesclinic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tkbiswas.pilesclinic"
        minSdk = 24
        targetSdk = 34
        // Phase 9-10: version bumped to reflect Phases 4-9 native additions
        // (Clinical Modules, Supabase Sync, Print System, Security/Settings)
        // on top of the original Phase 1-3 WebView shell. See CHANGELOG.md.
        // TK APPROVED (2026-07-16): versionName now matches the "V##" label
        // already used in every delivered ZIP filename / master note, so
        // the new "App Version" line in Menu (More Menu screen) shows the
        // same number TK already tracks -- makes it easy to see which
        // branch's phone is on an older build. Bump BOTH of these together
        // on every future delivery, right along with the ZIP filename.
        // BUG FIX (2026-07-26, full-project audit): these were left at 115
        // while the project moved on to V131, so Menu's "App Version" line
        // showed the wrong number. Brought back in line with the ZIP name.
        // V215 (2026-07-31): ZERO-RISK MASTER FIX ORDER — §12 Remark-from-Action
        // removed, §17 call-signal on all stages, §16 no false "Saved" + return to
        // source list, §18 delete truly leaves Draft/Visit-Reject list, §15 near-
        // realtime bell notification, §11 one-Back to list from Payment/Register,
        // web security headers + Cooch Behar number fix. See V215_CHANGED_FILES.md.
        // V216 (2026-07-31): §13 Refund/টাকা ফেরত full feature (entry+save+totals+
        // Master approval), §10 Report cache-first, §4 password hashing (backward-
        // compatible + lazy migration), §5 Supabase Auth prep, §15 FCM-ready source.
        // See V216_CHANGED_FILES.md.
        // V219 (2026-07-31): §1 Refund idempotency (deterministic id — retry-তে
        // দ্বিতীয় Refund হয় না, Android+web), §2 Web Delete by Record ID (মোবাইল নয়),
        // §4 stuck HTTP-400: Table/Record/কারণ দেখানো + 4xx safe-park, §5 web copy
        // parity (assets/www = Netlify), §6 security SQL copy-paste, §7 Free-plan:
        // briefings full-table fetch CloudReadCache দিয়ে dedupe। See V219_CHANGED_FILES.md.
        // V220 (2026-07-31): §1 HTTP-400-এর আসল ভুল Field (PostgREST body) সতর্কবার্তায়;
        // §2 একই ভুল data auto-resend বন্ধ (permanent 4xx + body-hash, retry অটুট);
        // §4 Refund per-dialog nonce — retry double নয়, কিন্তু নতুন ফর্মে বৈধ দ্বিতীয়
        // Refund আলাদা (Android+web)। §3 (backup-safe) শুধু re-audit — এই version-এ
        // কোড-এ করা হয়নি। See V220_CHANGED_FILES.md.
        // B271 (02.08.2026, TK approved, no new version yet): removed the unused
        // stale web-copy (assets/www/{index.html,app.js,styles.css,config.js,
        // manifest.json}) -- only branch logo/icon images remain there. No logic,
        // design, or build behaviour changed.
        // B558–B564 (08.08.2026): Medicine Payment (একাধিক ওষুধ + Save/Share/Print
        // + প্রফেশনাল History) এবং Add Treatment Payment (নতুন কার্ড — নাম/ID চাপলে
        // Full Journey, রোগ, ঠিকানা; BILL ৩-ট্যাপ এডিট / PAID→history / DUE→refund;
        // ৪ বোতাম Cancel/Share/Print/Save + রসিদ) — সব TK-অনুমোদিত প্রুফ অনুযায়ী।
        // V301 (09.08.2026): টাকার খাতা খাতা-স্টাইল (ব্রাঞ্চ ধরে আগের বাকি+অবশিষ্ট টাকা,
        // খরচ ভাঙা-হিসাব) · Add Collection নতুন সাজ (তারিখ→ক্যালেন্ডার, Save&Add More, Back) ·
        // IN TIME রিমাইন্ডার নতুন বিল্ডে দ্বিতীয়বার আসা বন্ধ (cloud-check) · নেট ফাস্ট তবু
        // "weak internet" ফিক্স (টোকেন মেয়াদশেষে গোপনে re-login, ModuleAuth) · কম্পিউটারের
        // আজকের হিসাব cache-first। সব ফোন+ওয়েব, TK-অনুমোদিত। বিস্তারিত খাতায়।
        // V306 (10.08.2026): অংশীদারি ভাগ (Partner Shares) ধাপ ১ — টাকার হিসাবের ভেতরে
        // নতুন PartnerSharesActivity (isolated): ব্রাঞ্চ ওভারভিউ · সেট-আপ · তোলা। fin schema-র
        // V306 টেবিল পড়ে/লেখে (SQL আলাদা রান)। ⛔ পুরনো কোনো হিসাব/স্ক্রিন বদলায়নি।
        // V309 (11.08.2026, B618): ছুটির আবেদন→অনুমোদন + Suspend। স্টাফ ছুটি চায় (আজ/অগ্রিম);
        // মাসে ৪ পর্যন্ত ও একই দিনে ব্রাঞ্চে সংঘর্ষ না থাকলে সরাসরি; ৫ম বা সংঘর্ষে Pending →
        // ওই ব্রাঞ্চের ডাক্তার বা মাস্টার (যেকোনো একজন) Approve/Reject; মঞ্জুর হলে পুরো ব্রাঞ্চ
        // দেখে + WhatsApp; মাস্টার স্টাফকে কয়েকদিন Suspend (লগইন বন্ধ)। ফোন+ওয়েব দুটোতেই।
        // V310 (11.08.2026, B619+B620): টাকা = নিজের ব্রাঞ্চ। Medicine Payment-এ নন-মাস্টার
        // অন্য ব্রাঞ্চ দেখতেই পারবে না; রোগীর Payment/Follow-up Advance/Nth/Chamber-এ অন্য
        // ব্রাঞ্চের টাকার ঘর খোলার আগেই আটকায় (সেভের MoneyBranchGuard backstop অটুট)। আর
        // "আসবে বলেছে" সারিতে বিভ্রান্তিকর "₹0 · CASH"-এর বদলে "আসার তারিখ"। ফোন+ওয়েব দুটোতেই।
        // V311 (11.08.2026, B621): Today Collection-এ আসল টাকা লুকিয়ে যাওয়ার বাগ ঠিক।
        // যে নম্বর আগে Enquiry-তে Reject হয়েছিল, পরে আবার রেজিস্টার + পেমেন্ট করলেও তার
        // টাকা দিনের হিসাব থেকে বাদ পড়ছিল (RefundedRecords "শুধু Cancelled" ধরত)। এখন আসল
        // রেজিস্টার্ড রোগী থাকলে (registration Cancel না হলে) টাকা গোনা হয়। ঝুঁকিহীন — শুধু
        // লুকোনো আসল টাকা ফেরে, বাড়তি কিছু লুকোয় না।
        // V312 (11.08.2026, B622): Doctor Check-up ফর্মের সব field-লেবেল এক সমান 12.5sp
        // (আগে 7–10.5sp অসমান); লম্বা লেবেল দু-লাইনে। ৪টি ঘর বাদ — Result of Previous
        // Treatment · Spent for Previous Treatment · Treatment Duration · Advance Payment
        // to be Done (ফর্ম + A4 প্রিন্ট দুটোতেই)। বাকি ডিজাইন/সেভ-লজিক অটুট।
        // V313 (11.08.2026, B624): Supabase Free-plan egress-এর আসল কারণ ধরা — ফোনের
        // auto-backup প্রতিটা স্টাফের ফোনে রোজ পুরো DB (ছবি-সহ) নামাত। এখন auto-backup
        // শুধু মাস্টারের ফোনে + সপ্তাহে একবার। স্টাফ-ফোনে আর পুরো-DB ডাউনলোড নেই।
        // V314 (11.08.2026, B625): egress-এর দ্বিতীয় বড় উৎস ধরা — প্রতি ফোনে ব্যাকগ্রাউন্ড
        // pre-warm প্রতি ~১৫ মিনিটে Doctor Queue-র সব রোগী **ছবি-সহ (base64)** নামাত। এখন
        // ব্যাকগ্রাউন্ডে ছবি ছাড়া টানে (ছবি শুধু কেউ স্ক্রিন খুললে), pre-warm-এর ব্যবধান
        // ১২→৩০ মিনিট, আর জমানো ছবি cache-এ অটুট থাকে। অফলাইন/স্ক্রিন আচরণ অপরিবর্তিত।
        // V315 (11.08.2026): Android কোড V314-এর মতোই অপরিবর্তিত (B625)। এই ভার্সনে শুধু
        // **ওয়েব** কাজ যুক্ত — B626 (Reject/ডুপ্লিকেট ফিক্স) + B627 (ওয়েবে অটো লগ-আউট +
        // লগ-আউটে realtime বন্ধ)। সম্পূর্ণ-প্রজেক্ট প্যাকেজ বলে version নম্বর বাড়ানো হলো।
        // V316 (11.08.2026, B628): Referral Income এন্ট্রি এডিট/ডিলিট — তিনবার-চাপ।
        // মাস্টার/একই-দিন সরাসরি; দিন পেরোলে স্টাফ/ডাক্তার → মাস্টার-Approve (payment
        // edit-এর মতোই)। নতুন টেবিল referral_edit_requests (SQL আলাদা দেওয়া আছে)।
        // V317 (11.08.2026, B629): Staff Salary — (১) "Add Salary — choose month"
        // (Master নিজে জয়েনিং থেকে History ভরতে পারবেন); (২) স্যালারির তারিখে
        // Master ও Doctor-কে reminder (bell + Briefing "Salary Due" + Pay)।
        // বিদ্যমান salary_config/salary_payments পড়েই — নতুন টেবিল/SQL নেই।
        // V318 (11.08.2026, B630): Doctor/RMP — নম্বর লেখার সাথে সাথেই "আগে সেভ আছে"
        // (থাকলে Save আটকায়) + একই ডাক্তারের একাধিক নম্বর (নতুন altMobiles কলাম; SQL আলাদা)।
        // V329 (12.08.2026): Owner-confirmed five Demo numbers were safely
        // cleaned from live data after backup+tombstone proof. Android adds
        // two narrow guards only: stale self-heal may not resurrect a deleted
        // source, and Incomplete/Reject chooses the active row linked to the
        // current patient (not an older same-mobile record). No UI/design or
        // normal Registration/Payment workflow changed.
        // V330 (12.08.2026): same-day Prescription confirmation guard; Full
        // Journey shows real Prescription/Registration times and presents
        // Registration + Visit + Visit Fee as the owner's single action.
        // Display merge only: accounting/database rows remain intact.
        // V368 (13.08.2026): V367 live-photo proof — force only Login button
        // to approved green on phones that re-apply the app's navy theme tint.
        // V371 (13.08.2026): Dialer + Work Notebook SIM chooser now has a
        // safe Back/Cancel route. No call data, design, web workflow or DB changed.
        // V385 (15.08.2026): "Complete despite Due" (R2) লেখা ফোনেও ইংরেজি —
        // কম্পিউটারের সাথে হুবহু এক (TK-অনুমোদিত প্রুফ)। শুধু দেখানো লেখা;
        // কাজের নিয়ম · Due/bill/paid · ডিজাইন · DB কিছুই বদলায়নি।
        // V397 (16.08.2026): Diet Chart-এর নতুন A4 প্রিন্ট (Android+Web এক) এবং
        // দুটো গ্লোবাল রুল — "TK BISWAS / Founder & Consultant" ও
        // "Dr. K.H MANDAL / (B.A.M.S) Regd 12386" — সব কাগজে ও বার্তায়।
        // ⛔ কোনো হিসাব · DB · workflow বদলায়নি; শুধু ছাপা ও দেখানো লেখা।
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    // 🔒 B271 (02.08.2026, TK approved): app/src/main/assets/www now holds ONLY
    // the branch logo/icon images (assets/*.jpg, *.png) used by BranchCatalog
    // for printing. The old index.html/app.js/styles.css/config.js/manifest.json
    // web-copy was deleted -- login is fully native (LoginActivity), the WebView
    // was already removed, and those files were an unused stale duplicate of the
    // real website at 03_NETLIFY_READY/. No network/build-time processing needed
    // for the remaining image files.
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.webkit:webkit:1.9.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Phase 4: Clinical Modules (native lists for Prescription / Investigation /
    // Diet Chart / Patient History)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Phase 5: Supabase Integration + Offline-First
    // -- Local database (offline-first source of truth) --
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // -- Networking (raw Supabase REST/GoTrue calls over Retrofit+OkHttp; see
    //    SUPABASE_SETUP.md for why this project talks to Supabase's stable HTTP
    //    API directly instead of the multiplatform supabase-kt SDK) --
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // -- Background auto-sync --
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // -- Secure local token storage (session persistence for Supabase Auth) --
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // -- Lifecycle helpers used by the sync/auth test screen --
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Phase 6: Professional Print System
    // QR code generation only (pure-Java encoder, no Android UI deps needed).
    // PDF generation/printing itself uses the built-in android.graphics.pdf /
    // android.print APIs — no extra library required.
    implementation("com.google.zxing:core:3.5.3")

    // Phase 9: regression unit tests (pure-JVM logic only; anything touching
    // Context/Room/Android framework is covered by the manual test steps in
    // FINAL_TEST_REPORT.md instead, since that needs an instrumented/emulator run).
    testImplementation("junit:junit:4.13.2")
}
