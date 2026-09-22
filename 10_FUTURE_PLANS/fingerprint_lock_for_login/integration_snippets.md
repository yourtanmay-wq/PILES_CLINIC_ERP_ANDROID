# Fingerprint চালু করার সময় — Login/App-open-এর জন্য

## এটা কী কাজ করবে

আপনার অ্যাপ এমনিতেই একবার লগইন করলে সেশন মনে রাখে — পরেরবার অ্যাপ
খুললে সরাসরি Dashboard-এ চলে যায় (আবার মোবাইল/পাসওয়ার্ড লিখতে হয় না)।
এই ফিচারটা যোগ হলে: সেই "সরাসরি Dashboard-এ চলে যাওয়া"-র ঠিক আগে
একবার Fingerprint চাইবে — যাতে ফোনটা অন্য কারো হাতে গেলেও, সেশন
থাকলেও, Fingerprint ছাড়া অ্যাপের ভেতরের ডেটা দেখা না যায়।

**Fingerprint না থাকলে/ব্যর্থ হলে:** পুরনো নিয়মে ফিরে যাবে — লগইন
স্ক্রিনেই থাকবে, মোবাইল-পাসওয়ার্ড দিয়ে আবার লগইন করা যাবে (এটাই
fallback, TK-এর নিয়ম অনুযায়ী)।

---

## ফাইল — `BiometricGate.kt`

এই একই ফাইল ব্যবহার হবে যেটা `fingerprint_lock_for_payment` ফোল্ডারে
আছে — নতুন করে বানাতে হবে না, দুই জায়গাতেই (Login আর Payment) একই
`BiometricGate.kt` কাজ করবে। শুধু একবার
`app/src/main/java/com/tkbiswas/pilesclinic/native/BiometricGate.kt`-এ
কপি করে রাখলেই দুই জায়গার জন্যই যথেষ্ট।

---

## `LoginActivity.kt`-এ ঠিক কোথায় বদল করতে হবে

**ফাইল:** `LoginActivity.kt`, `onCreate()`-এর শুরুর দিকে

**এখন যা আছে:**
```kotlin
// Already logged in from a previous app open -- skip straight to Dashboard.
val existing = NativeSession.current(this)
if (existing != null) {
    openDashboard()
    return
}
```

**এভাবে বদলাতে হবে:**
```kotlin
// Already logged in from a previous app open -- confirm with Fingerprint
// first (if available), THEN go to Dashboard.
val existing = NativeSession.current(this)
if (existing != null) {
    if (BiometricGate.isAvailable(this)) {
        BiometricGate.prompt(
            this, "Confirm it's you", "Unlock Piles Clinic App",
            onSuccess = { openDashboard() },
            onFailOrCancel = {
                // Stay on the Login screen (already set up below) --
                // staff can log in again with mobile/password as normal.
            }
        )
        return
    } else {
        openDashboard() // no fingerprint hardware/enrollment -- old behavior
        return
    }
}
```

**একটা কথা মনে রাখতে হবে:** এই বদলের পর, `onFailOrCancel`-এর সময় কিছু
না করলেও চলবে — কারণ `setContentView(binding.root)` আগেই হয়ে গেছে
(এই লাইনের ঠিক উপরে), তাই Login ফর্মটা এমনিতেই স্ক্রিনে দেখা যাবে,
স্টাফ সরাসরি মোবাইল-পাসওয়ার্ড দিয়ে লগইন করতে পারবেন।

---

## build.gradle.kts

`fingerprint_lock_for_payment`-এ যে লাইন যোগ করার কথা বলা হয়েছে
(`implementation("androidx.biometric:biometric:1.1.0")`) — সেটা একবার
যোগ করলেই Login আর Payment দুটোর জন্যই যথেষ্ট, দুইবার যোগ করার দরকার
নেই।

---

## এই ফিচারটা আলাদাভাবে চালু/বন্ধ করা যাবে

TK চাইলে শুধু Login-এর ফিঙ্গারপ্রিন্ট চালু করতে পারেন, Payment-এরটা
ছাড়াই (বা উল্টোটা) — দুটো সম্পূর্ণ আলাদা জায়গায় বসে, একটা আরেকটার
উপর নির্ভর করে না। শুধু `BiometricGate.kt` ফাইলটা দুটোরই জন্য
দরকার হবে (একবারই বসাতে হবে)।
