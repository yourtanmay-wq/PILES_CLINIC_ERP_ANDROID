# V494 — Dedupe · সেশন · B446 যুক্তির পরীক্ষা

Kotlin কম্পাইলার এই পরিবেশে আনা যায়নি (dl.google.com ও Maven Central ব্লকড),
তাই `CloudReadDedupe.kt` ও `SafeWideColumns.kt`-এর যুক্তি **হুবহু Java-তে** লিখে
সত্যিকারের থ্রেড দিয়ে চালানো হয়েছে। `SafeWide.java`-র ঘর-তালিকা সরাসরি
Kotlin ফাইল থেকে বানানো, তাই দুটো কখনো আলাদা হতে পারে না।

    javac -d . Dedupe.java SafeWide.java T.java T2.java T3.java
    java -Dfile.encoding=UTF-8 -cp . T    # V493-এর ১৬টি
    java -Dfile.encoding=UTF-8 -cp . T2   # V494: মেমরি লিক · সেশন · expiry (১৫টি)
    java -Dfile.encoding=UTF-8 -cp . T3   # V494: B446 চার-ধাপের চেইন (৯টি)

ফল: **৪০টি পরীক্ষার সবগুলো পাশ।**

⚠️ এটা যুক্তির প্রমাণ — Kotlin বিল্ডের বিকল্প নয়। Android Studio-র বিল্ডই চূড়ান্ত।
