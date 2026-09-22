# V227 — Before/After diff (V226 → V227)

সাতটি ফাইল বদলেছে। প্রতিটি বাস্তব diff নিচে।

```diff
--- V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts
+++ V227/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts
@@ -82,8 +82,8 @@
         // §4 Refund per-dialog nonce — retry double নয়, কিন্তু নতুন ফর্মে বৈধ দ্বিতীয়
         // Refund আলাদা (Android+web)। §3 (backup-safe) শুধু re-audit — এই version-এ
         // কোড-এ করা হয়নি। See V220_CHANGED_FILES.md.
-        versionCode = 226
-        versionName = "2.26"
+        versionCode = 227
+        versionName = "2.27"
 
         buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
         buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
```

```diff
--- V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/assets/www/index.html
+++ V227/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/assets/www/index.html
@@ -7,9 +7,9 @@
   <link rel="manifest" href="manifest.json">
   <meta name="theme-color" content="#0f2748">
   <link rel="apple-touch-icon" href="assets/icon-192.png">
-  <link rel="stylesheet" href="styles.css?v=v226" />
-  <script defer src="config.js?v=v226"></script>
-  <script defer src="app.js?v=v226"></script>
+  <link rel="stylesheet" href="styles.css?v=v227" />
+  <script defer src="config.js?v=v227"></script>
+  <script defer src="app.js?v=v227"></script>
 </head>
 <body>
   <div id="app"><div class="loader">PILES CLINIC loading...</div></div>
```

```diff
--- V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/PatientTimelineActivity.kt
+++ V227/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/PatientTimelineActivity.kt
@@ -2643,7 +2643,14 @@
                     binding.progressLoad.visibility = View.GONE
                     binding.tvEmpty.visibility = View.VISIBLE
                 } else {
+                    // V227 (item 26): preserve scroll position on in-place
+                    // refresh (e.g. returning from Payment/Photo), same proven
+                    // pattern as Doctor Queue / Follow-up.
+                    val lm = binding.recyclerView.layoutManager as? LinearLayoutManager
+                    val firstPos = lm?.findFirstVisibleItemPosition() ?: -1
+                    val firstOffset = if (firstPos >= 0) (lm?.findViewByPosition(firstPos)?.top ?: 0) else 0
                     adapter.update(data.entries)
+                    if (firstPos in 0 until data.entries.size) lm?.scrollToPositionWithOffset(firstPos, firstOffset)
                 }
 
                 // TK-REQUESTED ADDITION (2026-07-16): summary chips — Estimated
```

```diff
--- V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/PaymentActivity.kt
+++ V227/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/PaymentActivity.kt
@@ -232,7 +232,16 @@
             binding.tvEmpty.visibility = View.VISIBLE
         } else {
             binding.tvEmpty.visibility = View.GONE
-            adapter.updateItems(rows.sortedByDescending { it.date })
+            // V227 (item 26): keep the same scroll position on an in-place
+            // refresh (add/edit/delete), same proven pattern as Doctor Queue /
+            // Follow-up. Back navigation already retains position; this covers
+            // the reload case so the list no longer jumps to the top.
+            val lm = binding.recyclerView.layoutManager as? LinearLayoutManager
+            val firstPos = lm?.findFirstVisibleItemPosition() ?: -1
+            val firstOffset = if (firstPos >= 0) (lm?.findViewByPosition(firstPos)?.top ?: 0) else 0
+            val sorted = rows.sortedByDescending { it.date }
+            adapter.updateItems(sorted)
+            if (firstPos in 0 until sorted.size) lm?.scrollToPositionWithOffset(firstPos, firstOffset)
         }
     }
 
```

```diff
--- V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/TrashBinActivity.kt
+++ V227/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/TrashBinActivity.kt
@@ -22,6 +22,10 @@
     private lateinit var repository: TrashRepository
     private lateinit var adapter: TrashAdapter
     private lateinit var user: NativeUser
+    // V227 (item 46): Master-only branch filter (default "All" = every branch,
+    // exactly as before). Staff never see this — they are blocked from the
+    // whole screen by the master-only guard in onCreate.
+    private var pickedBranch: String = "All"
 
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
@@ -65,9 +69,33 @@
         }
         binding.btnBack.setOnClickListener { finish() }
 
+        // V227 (item 46): Master picks a branch (or keeps All) — same rule as the
+        // Draft / Reject / Incomplete lists. Cloud query is untouched; the filter
+        // is applied client-side in branchScoped(), so no extra cloud usage.
+        if (user.role == "master") {
+            binding.branchFilter.visibility = View.VISIBLE
+            binding.branchFilter.text = "🏥 $pickedBranch ▾"
+            binding.branchFilter.setOnClickListener { showBranchPicker() }
+        } else {
+            binding.branchFilter.visibility = View.GONE
+        }
+
         loadList()
     }
 
+    // V227 (item 46): branch chooser for Master only.
+    private fun showBranchPicker() {
+        val options = arrayOf("All", "Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
+        AlertDialog.Builder(this)
+            .setCustomTitle(PremiumAlert.header(this, "Branch"))
+            .setItems(options) { _, which ->
+                pickedBranch = options[which]
+                binding.branchFilter.text = "🏥 $pickedBranch ▾"
+                loadList()
+            }
+            .show().also { PremiumAlert.paint(it) }
+    }
+
         private var firstResume = true
     override fun onResume() {
         super.onResume()
@@ -173,7 +201,13 @@
      *  see their own branch's deleted records; Master still sees all. */
     private fun branchScoped(items: List<TrashItem>): List<TrashItem> {
         val me = NativeSession.current(this) ?: return emptyList()
-        if (me.role == "master") return items
+        if (me.role == "master") {
+            // V227 (item 46): Master default "All" = every branch (unchanged);
+            // if a branch is picked, show only that one.
+            val pick = pickedBranch.trim()
+            if (pick.isBlank() || pick.equals("All", ignoreCase = true)) return items
+            return items.filter { it.record.optString("branch", "").trim().equals(pick, ignoreCase = true) }
+        }
         val mine = me.branch.trim()
         if (mine.isBlank() || mine.equals("All", ignoreCase = true)) return items
         return items.filter { it.record.optString("branch", "").trim().equals(mine, ignoreCase = true) }
```

```diff
--- V226/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/res/layout/activity_trash_bin.xml
+++ V227/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/res/layout/activity_trash_bin.xml
@@ -31,6 +31,20 @@
             android:textColor="@color/white"
             android:textSize="15sp"
             android:textStyle="bold" />
+
+        <!-- V227 (item 46): Master-only branch filter, mirrors DraftActivity.
+             Hidden by default; shown only for Master in TrashBinActivity. -->
+        <TextView
+            android:id="@+id/branchFilter"
+            android:layout_width="wrap_content"
+            android:layout_height="wrap_content"
+            android:layout_marginStart="8dp"
+            android:paddingHorizontal="10dp"
+            android:paddingVertical="4dp"
+            android:text="🏥 All ▾"
+            android:textColor="@color/white"
+            android:textSize="13sp"
+            android:visibility="gone" />
     </LinearLayout>
 
     <ProgressBar
```

```diff
--- V226/03_NETLIFY_READY/index.html
+++ V227/03_NETLIFY_READY/index.html
@@ -7,9 +7,9 @@
   <link rel="manifest" href="manifest.json">
   <meta name="theme-color" content="#0f2748">
   <link rel="apple-touch-icon" href="assets/icon-192.png">
-  <link rel="stylesheet" href="styles.css?v=v226" />
-  <script defer src="config.js?v=v226"></script>
-  <script defer src="app.js?v=v226"></script>
+  <link rel="stylesheet" href="styles.css?v=v227" />
+  <script defer src="config.js?v=v227"></script>
+  <script defer src="app.js?v=v227"></script>
 </head>
 <body>
   <div id="app"><div class="loader">PILES CLINIC loading...</div></div>
```

