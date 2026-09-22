# V226 — Ready-to-apply patch: Trash Bin Master Branch Filter (item 46)

**কেন এটি এখানে patch হিসেবে, V226 tree-তে সরাসরি নয়:** এটি একটি **নতুন দৃশ্যমান UI control** যোগ করে ও approved layout ছোঁয়। এই cloud-এ build/device নেই বলে চেহারা (placement/overlap) চোখে যাচাই করা যায় না, আর আপনার নিয়মে approved layout আন্দাজে বদলানো নিষেধ। তাই হুবহু কোড দিলাম — আপনি Android Studio-তে paste করে build + device-এ দেখে merge করবেন। Staff behavior **একদম অপরিবর্তিত** (তারা আগের মতোই Trash-এ ঢুকতেই পারবে না); এটি শুধু Master-কে branch বেছে নেওয়ার সুযোগ দেয়, ঠিক Draft/Reject/Incomplete list-এর মতো।

---

## ১) Layout — `res/layout/activity_trash_bin.xml`

হেডারের title TextView-এর **ঠিক পরে** (line 33-এর `</TextView>`-এর পরে, header `LinearLayout` বন্ধ হওয়ার আগে) এই TextView যোগ করুন:

```xml
        <!-- V226 (item 46): Master-only branch filter, mirrors DraftActivity. -->
        <TextView
            android:id="@+id/branchFilter"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:paddingHorizontal="10dp"
            android:paddingVertical="4dp"
            android:text="🏥 All ▾"
            android:textColor="@color/white"
            android:textSize="13sp"
            android:visibility="gone" />
```

(placement: `btnBack` → title(weight=1) → `branchFilter` — ডান দিকে বসবে।)

---

## ২) Kotlin — `native/TrashBinActivity.kt`

**(a)** ক্লাসে একটি ফিল্ড যোগ করুন (`user` ঘোষণার পাশে):

```kotlin
    private var pickedBranch: String = "All"
```

**(b)** `onCreate`-এ, `loadList()` কল করার **আগে** master হলে picker সেট করুন — `binding.btnBack.setOnClickListener { finish() }`-এর ঠিক পরে:

```kotlin
        // V226 (item 46): Master branch filter — same rule as Draft/Reject/Incomplete.
        if (user.role == "master") {
            binding.branchFilter.visibility = View.VISIBLE
            binding.branchFilter.text = "🏥 $pickedBranch ▾"
            binding.branchFilter.setOnClickListener { showBranchPicker() }
        } else {
            binding.branchFilter.visibility = View.GONE
        }
```

**(c)** একটি নতুন function যোগ করুন (`branchScoped`-এর পাশে):

```kotlin
    // V226 (item 46): Master picks a branch (or "All"); staff never see this.
    private fun showBranchPicker() {
        val options = arrayOf("All", "Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Branch"))
            .setItems(options) { _, which ->
                pickedBranch = options[which]
                binding.branchFilter.text = "🏥 $pickedBranch ▾"
                loadList()
            }
            .show().also { PremiumAlert.paint(it) }
    }
```

**(d)** `branchScoped()`-এ master-এর নির্বাচিত branch প্রয়োগ করুন। বর্তমান function-টি এভাবে বদলান:

```kotlin
    private fun branchScoped(items: List<TrashItem>): List<TrashItem> {
        val me = NativeSession.current(this) ?: return emptyList()
        if (me.role == "master") {
            // V226 (item 46): Master default "All" = সব branch (আগের মতোই);
            // একটি branch বাছলে শুধু সেটিই।
            val pick = pickedBranch.trim()
            if (pick.isBlank() || pick.equals("All", ignoreCase = true)) return items
            return items.filter { it.record.optString("branch", "").trim().equals(pick, ignoreCase = true) }
        }
        val mine = me.branch.trim()
        if (mine.isBlank() || mine.equals("All", ignoreCase = true)) return items
        return items.filter { it.record.optString("branch", "").trim().equals(mine, ignoreCase = true) }
    }
```

---

## নিরাপত্তা নোট
- Cloud query (`TrashRepository.fetchTrashRaw()`) **অপরিবর্তিত** — filter শুধু client-side `branchScoped()`-এ, তাই কোনো নতুন read/write বা Free-Plan খরচ বাড়ে না।
- Staff path সম্পূর্ণ অপরিবর্তিত (তারা `onCreate`-এর master-only guard-এ আগেই আটকে যায়)।
- `AlertDialog`, `PremiumAlert`, `View`, `NativeSession` — সবই ফাইলে আগে থেকেই import করা, নতুন import লাগে না।
- Branch তালিকা `ReportsRepository.kt:180`-এর সঙ্গে হুবহু এক (Kishanganj, Jalpaiguri, Cooch Behar, Falakata, Birpara)।
- Rollback: এই তিন-চার টুকরো সরিয়ে দিলেই আগের অবস্থা; বা `ROLLBACK_V226`-এর মূল ফাইল ফেরত।

**⛔ build না করে production-এ দেবেন না — Android Studio-তে compile + device-এ চেহারা দেখে তবেই merge করুন।**
