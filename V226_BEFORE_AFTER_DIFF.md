# V226 — Before/After diff (V225 → V226)

Generated from real file contents. Four application files changed. Nothing else.

```diff
--- V225/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts
+++ V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts
@@ -82,8 +82,8 @@
         // §4 Refund per-dialog nonce — retry double নয়, কিন্তু নতুন ফর্মে বৈধ দ্বিতীয়
         // Refund আলাদা (Android+web)। §3 (backup-safe) শুধু re-audit — এই version-এ
         // কোড-এ করা হয়নি। See V220_CHANGED_FILES.md.
-        versionCode = 225
-        versionName = "2.25"
+        versionCode = 226
+        versionName = "2.26"
 
         buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
         buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
```

```diff
--- V225/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/ReportsRepository.kt
+++ V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/ReportsRepository.kt
@@ -60,7 +60,32 @@
         return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
     }
 
-    private fun monthOf(raw: String): String = raw.take(7)
+    /** 🔒 V226 (item 86, 01.08.2026): Current-month গণনা কখনো ভুল করে ০ দেখাতে
+     *  পারত যদি কোনো টেবিলে তারিখ `dd.MM.yyyy` / `dd/MM/yyyy` ধাঁচে জমা থাকত —
+     *  কারণ আগের `raw.take(7)` শুধু `yyyy-MM-...` ধাঁচকে ঠিকভাবে ধরত (`01.08.2026`
+     *  থেকে পেত "01.08.2", যা কোনো মাসের সঙ্গে মেলে না)। এখন তিন ধাঁচই সঠিক
+     *  `yyyy-MM` বানায়; অজানা ধাঁচে আগের আচরণ (take(7)) অটুট।
+     *  ⛔ `yyyy-MM-dd`/ISO সারির ফল হুবহু অপরিবর্তিত — শুধু ভুলভাবে বাদ পড়া
+     *  `dd.MM.yyyy` সারি এখন সঠিক মাসে গোনা হয়। কোনো টাকা/সংখ্যার নিয়ম বদলায়নি,
+     *  শুধু তারিখ-পড়া দৃঢ় করা হলো। (Owner: live data-তে যাচাই করবেন।) */
+    private fun monthOf(raw: String): String {
+        val s = raw.trim()
+        // yyyy-MM-dd বা ISO (yyyy-MM-ddTHH:...) → আগের মতোই (ফল অপরিবর্তিত)
+        if (s.length >= 7 && s[4] == '-' &&
+            s[0].isDigit() && s[1].isDigit() && s[2].isDigit() && s[3].isDigit() &&
+            s[5].isDigit() && s[6].isDigit()) {
+            return s.take(7)
+        }
+        // dd.MM.yyyy বা dd/MM/yyyy → yyyy-MM
+        if (s.length >= 10 && (s[2] == '.' || s[2] == '/') && (s[5] == '.' || s[5] == '/') &&
+            s[0].isDigit() && s[1].isDigit() && s[3].isDigit() && s[4].isDigit() &&
+            s[6].isDigit() && s[7].isDigit() && s[8].isDigit() && s[9].isDigit()) {
+            val year = s.substring(6, 10)
+            val month = s.substring(3, 5)
+            return "$year-$month"
+        }
+        return s.take(7)
+    }
 
     /** TK-ORDER (2026-07-25, branch-leak sweep): a staff must see numbers for
      *  THEIR OWN BRANCH only. Master (branchFilter = null or "All") still sees
```

```diff
--- V225/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/assets/www/index.html
+++ V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/assets/www/index.html
@@ -7,9 +7,9 @@
   <link rel="manifest" href="manifest.json">
   <meta name="theme-color" content="#0f2748">
   <link rel="apple-touch-icon" href="assets/icon-192.png">
-  <link rel="stylesheet" href="styles.css?v=v223" />
-  <script defer src="config.js?v=v223"></script>
-  <script defer src="app.js?v=v223"></script>
+  <link rel="stylesheet" href="styles.css?v=v226" />
+  <script defer src="config.js?v=v226"></script>
+  <script defer src="app.js?v=v226"></script>
 </head>
 <body>
   <div id="app"><div class="loader">PILES CLINIC loading...</div></div>
```

```diff
--- V225/03_NETLIFY_READY/index.html
+++ V226/03_NETLIFY_READY/index.html
@@ -7,9 +7,9 @@
   <link rel="manifest" href="manifest.json">
   <meta name="theme-color" content="#0f2748">
   <link rel="apple-touch-icon" href="assets/icon-192.png">
-  <link rel="stylesheet" href="styles.css?v=v223" />
-  <script defer src="config.js?v=v223"></script>
-  <script defer src="app.js?v=v223"></script>
+  <link rel="stylesheet" href="styles.css?v=v226" />
+  <script defer src="config.js?v=v226"></script>
+  <script defer src="app.js?v=v226"></script>
 </head>
 <body>
   <div id="app"><div class="loader">PILES CLINIC loading...</div></div>
```

