# FCM Instant Push — চালু করার ধাপ (§15)

⛔ **সততা:** V216-এ notification ইতিমধ্যে **near-realtime** (background worker ~১৫ মিনিট cadence, সাউন্ড+ভাইব্রেশন+background, de-dup সহ — `BackgroundRefreshWorker`)। এটা **তাৎক্ষণিক (instant) push নয়**। সত্যিকারের instant চাইলে নিচের FCM setup লাগবে — এতে google-services.json ও Firebase console দরকার, যা এই সেশনে করা **সম্ভব নয়**, তাই "instant push চালু" **দাবি করা হয়নি**।

এই ফোল্ডারে drop-in source আছে: `PilesFirebaseMessagingService.kt.txt`।

## এক-বারের manual ধাপ

1. **Firebase project + app** তৈরি করুন (package `com.tkbiswas.pilesclinic`), `google-services.json` নামিয়ে `app/` ফোল্ডারে রাখুন।

2. **build.gradle (project-level)** — plugins ব্লকে:
   ```
   id("com.google.gms.google-services") version "4.4.2" apply false
   ```

3. **app/build.gradle.kts** —
   ```
   plugins { id("com.google.gms.google-services") }
   dependencies {
     implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
     implementation("com.google.firebase:firebase-messaging-ktx")
   }
   ```

4. **PilesFirebaseMessagingService.kt.txt** → নাম বদলে `PilesFirebaseMessagingService.kt`, সরান
   `app/src/main/java/com/tkbiswas/pilesclinic/native/`-এ।

5. **AndroidManifest.xml** — `<application>`-এর ভিতরে:
   ```
   <service
     android:name=".native.PilesFirebaseMessagingService"
     android:exported="false">
     <intent-filter>
       <action android:name="com.google.firebase.MESSAGING_EVENT" />
     </intent-filter>
   </service>
   ```

6. **Supabase (SQL):** একটা `device_tokens` টেবিল (id, mobile, role, branch, token, updatedAt) — service class-এর `onNewToken` এতে token রাখে।

7. **Supabase Edge Function:** নতুন `briefings` row (role=master target) insert হলে সংশ্লিষ্ট master-দের `device_tokens.token`-এ FCM data-message পাঠায় (Firebase server key দিয়ে)। এটাই instant।

## যাচাই (setup-এর পর)
- App বন্ধ রেখে অন্য ফোন থেকে একটা staff request পাঠান → master-এর ফোনে কয়েক সেকেন্ডে সাউন্ডসহ notification আসা উচিত।
- একই request দুবার সাউন্ড করবে না (BellNotifier de-dup)।

⛔ উপরের ৭টা ধাপ সম্পূর্ণ না করে "Notification Fully Working (instant)" বলা যাবে না — V216-এ শুধু near-realtime অংশ কোড-এ চালু আছে।
