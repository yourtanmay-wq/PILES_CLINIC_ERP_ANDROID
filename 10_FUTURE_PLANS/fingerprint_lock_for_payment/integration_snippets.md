# Fingerprint চালু করার সময় ঠিক কোথায়, কী যোগ করতে হবে

এই ফাইলে ৩টা জায়গার জন্য **হুবহু** কোড দেওয়া আছে। যখন TK বলবেন
"এখন চালু করো", তখন `PaymentActivity.kt`-এর এই ৩টা জায়গায় নিচের
বদলগুলো করতে হবে — অন্য কিছু ছোঁয়া লাগবে না।

---

## জায়গা ১ — Total Bill Amount, ৩-ট্যাপ আনলক

**ফাইল:** `PaymentActivity.kt`, প্রায় লাইন ৫০০ (`billTapCount >= 3` লেখা আছে)

**এখন যা আছে:**
```kotlin
if (billTapCount >= 3) {
    isFocusableInTouchMode = true; isFocusable = true; isCursorVisible = true
    setTextIsSelectable(true)
    setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
    setTextColor(android.graphics.Color.parseColor("#0B2B59"))
    requestFocus()
    Toast.makeText(this@PaymentActivity, "Bill unlocked for editing", Toast.LENGTH_SHORT).show()
}
```

**এভাবে বদলাতে হবে** (৩-ট্যাপের পর, Fingerprint থাকলে সেটাও চাইবে):
```kotlin
if (billTapCount >= 3) {
    fun doUnlock() {
        isFocusableInTouchMode = true; isFocusable = true; isCursorVisible = true
        setTextIsSelectable(true)
        setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
        setTextColor(android.graphics.Color.parseColor("#0B2B59"))
        requestFocus()
        Toast.makeText(this@PaymentActivity, "Bill unlocked for editing", Toast.LENGTH_SHORT).show()
    }
    if (BiometricGate.isAvailable(this@PaymentActivity)) {
        BiometricGate.prompt(
            this@PaymentActivity, "Confirm it's you", "Unlock Total Bill Amount",
            onSuccess = { doUnlock() },
            onFailOrCancel = { billTapCount = 0 }
        )
    } else {
        doUnlock() // no fingerprint hardware/enrollment -- old 3-tap-only behavior
    }
}
```

---

## জায়গা ২ — Advance/2nd Payment Amount, ৩-ট্যাপ আনলক

**ফাইল:** `PaymentActivity.kt`, প্রায় লাইন ৫৩৫ (`amtTapCount >= 3` লেখা আছে)

**এখন যা আছে:**
```kotlin
if (amtTapCount >= 3) {
    amtInput.isFocusableInTouchMode = true; amtInput.isFocusable = true; amtInput.isCursorVisible = true
    amtInput.requestFocus()
    Toast.makeText(this, "Advance unlocked for editing", Toast.LENGTH_SHORT).show()
}
```

**এভাবে বদলাতে হবে:**
```kotlin
if (amtTapCount >= 3) {
    fun doUnlock() {
        amtInput.isFocusableInTouchMode = true; amtInput.isFocusable = true; amtInput.isCursorVisible = true
        amtInput.requestFocus()
        Toast.makeText(this, "Advance unlocked for editing", Toast.LENGTH_SHORT).show()
    }
    if (BiometricGate.isAvailable(this@PaymentActivity)) {
        BiometricGate.prompt(
            this@PaymentActivity, "Confirm it's you", "Unlock Advance Amount",
            onSuccess = { doUnlock() },
            onFailOrCancel = { amtTapCount = 0 }
        )
    } else {
        doUnlock()
    }
}
```

---

## জায়গা ৩ — "Marked Arrived" ৩-ট্যাপ Delete

**ফাইল:** `PaymentActivity.kt`, `tryEditPayment()`-এর ভেতরে, `deleteBtn.setOnClickListener` অংশ

**এখন যা আছে:**
```kotlin
deleteBtn.setOnClickListener {
    lifecycleScope.launch {
        val ok = withContext(Dispatchers.IO) { ChamberAttendanceRepository.undoAttendanceMark(id) }
        Toast.makeText(this@PaymentActivity, if (ok) "Entry deleted" else "Failed — check connection", Toast.LENGTH_SHORT).show()
        if (ok) { loadSummary(); dialog.dismiss() }
    }
}
```

**এভাবে বদলাতে হবে:**
```kotlin
deleteBtn.setOnClickListener {
    fun doDelete() {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { ChamberAttendanceRepository.undoAttendanceMark(id) }
            Toast.makeText(this@PaymentActivity, if (ok) "Entry deleted" else "Failed — check connection", Toast.LENGTH_SHORT).show()
            if (ok) { loadSummary(); dialog.dismiss() }
        }
    }
    if (BiometricGate.isAvailable(this@PaymentActivity)) {
        BiometricGate.prompt(
            this@PaymentActivity, "Confirm it's you", "Delete this entry",
            onSuccess = { doDelete() },
            onFailOrCancel = { }
        )
    } else {
        doDelete()
    }
}
```

---

## build.gradle.kts-এ যোগ করতে হবে (একবার)

`app/build.gradle.kts` ফাইলে, `dependencies { ... }` অংশের ভেতরে,
আর যেকোনো একটা `implementation(...)` লাইনের নিচে এই লাইনটা যোগ করুন:

```kotlin
implementation("androidx.biometric:biometric:1.1.0")
```

---

## এই ফোল্ডারের `BiometricGate.kt` ফাইলটা কোথায় বসাতে হবে

কপি করে এখানে রাখতে হবে:
```
app/src/main/java/com/tkbiswas/pilesclinic/native/BiometricGate.kt
```
(অন্য সব ফাইল যেখানে আছে, ঠিক সেই একই ফোল্ডারে)

---

## সংক্ষেপে, চালু করার ধাপ (৪টা কাজ)

1. `BiometricGate.kt` কপি করে আসল কোড-ফোল্ডারে বসান
2. `build.gradle.kts`-এ একটা লাইন যোগ করুন (উপরে দেখানো)
3. `PaymentActivity.kt`-এর ৩টা জায়গায় উপরের বদল করুন
4. Android Studio-তে Clean Project → Rebuild Project → APK বানান → লাইভ টেস্ট
