# সেশন সারসংক্ষেপ — V121 (2026-07-24)

বিস্তারিত সব `00_PROJECT_STATE_MASTER_NOTE.md`-এর সেকশন ১৩৮-১৬৩-এ। সংক্ষিপ্ত তালিকা নিচে।

## এই সেশনে সম্পন্ন কাজ (V116 → V121)
স্লো-নেট লোডিং ফিক্স · Date Format Global Rule (dot) · Background Sync ফিক্স · Visit Fee Missing ট্যাপ · Enquiry Branch নিয়ম · Follow-up badge/count/remark-sync bug ফিক্স · Registration বোতাম রিডিজাইন · "syncing" স্থায়ী bug root-cause ফিক্স · Payment সিস্টেম ইউনিফাই (Chamber সহ) · Bill-without-Advance · CAPITAL LETTER Global Rule (সম্পূর্ণ প্রজেক্ট) · missing-import বাগ ফিক্স + নতুন যাচাই-পদ্ধতি · Note ট্যাপ-করে-পুরোটা-দেখা · টেবিলের সব বক্স সমান-উচ্চতা Global Rule · Chamber Payment→একটা বক্স · Expected/waiting সারি রিডিজাইন · RMP Referral Income→Report Card ট্যাপ + নাম হাইলাইট।

## যাচাই (ফাইল পাঠানোর আগে, বাধ্যতামূলক)
- প্রজেক্টের ১৪৪টা Kotlin ফাইল brace/paren-balanced
- নতুন শেয়ার্ড ইউটিলিটি (DateUtil, UppercaseInputUtil) — প্রতিটা ব্যবহারের জায়গায় import ক্রস-চেক করা
- সব পরিবর্তিত XML well-formed, কোনো "--" কমেন্ট-ভুল নেই
- পুরনো সরানো view-এর কোনো অবশিষ্ট রেফারেন্স নেই
- আপলোড করা মূল ZIP-এর সব ফাইল অক্ষত (কিছু বাদ পড়েনি)

## পরবর্তী সেশনের জন্য বাকি কাজ
কিছুই বাকি নেই এই মুহূর্তে — সব অনুরোধ সম্পন্ন ও যাচাই করা।

## এই ফাইলে যা আছে তা ধ্বংস করা যাবে না
বাস্তব চলমান ক্লিনিকের ডেটা, ডেমো প্রজেক্ট না। TK-এর অনুমতি ছাড়া কিছু বদলানো/ধ্বংস করা যাবে না। TK শুধু ফটো-প্রুফ (সম্পূর্ণ স্ক্রিন, ক্রপ না করে) দেখে ডিজাইন ফাইনাল করেন — কোড সঠিকতার সম্পূর্ণ দায় Claude-এর।
