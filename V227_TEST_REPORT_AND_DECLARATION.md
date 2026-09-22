# V227 — Test report + Declaration (সৎ)

## এই পরিবেশে যা যাচাই করা হয়েছে
- Kotlin edit-গুলোর brace/paren balance যাচাই — নতুন কোডে কোনো imbalance আসেনি (edit-এর আগে-পরে একই)।
- Trash filter: DraftActivity-র প্রমাণিত pattern হুবহু অনুসরণ; ব্যবহৃত সব API (AlertDialog, PremiumAlert, View, NativeSession, LinearLayoutManager) ফাইলে আগে থেকেই import করা।
- Scroll preserve: Doctor Queue/Follow-up-এ চলমান একই code-pattern পুনরায় ব্যবহার।
- File-diff/hash: V226→V227-এ ঠিক ৭টি ফাইল বদলেছে; rollback কার্যকর (rollback==V226, rollback≠V227)।
- Web parity: দুই index.html byte-identical।

## যা এই পরিবেশে করা যায়নি (সৎভাবে দাবি করা হচ্ছে না)
- **Android `assembleDebug` build:** ❌ এই cloud-এ Android SDK নেই + Google blocked। কোনো APK/BUILD SUCCESSFUL দাবি করা হচ্ছে না। **owner Android Studio-তে build করবেন।**
- Physical-device UI ও live Supabase test: ❌ এখানে সম্ভব নয়।

## Declaration
“অনুমোদিত তালিকার বাইরে কোনো Design, Workflow, Permission, Branch Rule, Payment Logic, Print Logic, Login, Database Rule বা Feature পরিবর্তন করা হয়নি। কোনো অসম্পূর্ণ বা পরীক্ষা না-করা কাজকে Done/Passed বলা হয়নি। কোনো মিথ্যা Build/Test দাবি নেই।”

ZIP: `PILES_CLINIC_APP_V227_BUILD_PENDING.zip` · Root: `PILES_CLINIC_APP_V227_BUILD_PENDING` · `versionCode=227`, `versionName="2.27"`.
