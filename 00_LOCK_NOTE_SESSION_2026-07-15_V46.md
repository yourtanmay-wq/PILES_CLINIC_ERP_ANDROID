# PILES CLINIC APP — V46 (Visit Card: TEST HERE + প্রিমিয়াম চিপ)

🚫 এই ফাইলে যা আছে তা ধ্বংস করা যাবে না। কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না।
কোনো working flow খারাপ করা যাবে না।

## V46 — Visit Card-এ "TEST HERE" (Blood Test সরাসরি খোলে)
Visit ট্যাবের কার্ডে "💰 ADVANCE HERE"-এর ঠিক নিচে এখন "🩸 TEST HERE" — দুটোই এখন
ক্যাপিটাল লেটারে ও প্রিমিয়াম গ্র্যাডিয়েন্ট চিপ (অ্যাপের "Apply Common Blood Test"
বোতামের মতো একই গ্র্যাডিয়েন্ট স্টাইল — নতুন কিছু আবিষ্কার করা হয়নি, প্রমাণিত ডিজাইন
পুনর্ব্যবহার করা হয়েছে)। TEST HERE-এ ট্যাপ করলে শুধু Blood Test / Investigation
Advice স্ক্রিনই সরাসরি খোলে (৪-অপশনের মেনু নয়)। সেই স্ক্রিনে Share as Text ও Save &
Print দুটোই আগে থেকেই আছে।

## গুরুত্বপূর্ণ — কার্ডের বাকি কিছুই বদলানো হয়নি
- নাম, ট্যাগ (branch/disease), ছবি, Payment Ring, Prescription চিপ (Patient ট্যাবে),
  Enquiry/Inquiry-এর Due badge — এসবের একটাও ছোঁয়া হয়নি।
- পুরনো `bg_visit_advance.xml` ড্রয়েবল (যেটা Draft card / nth-payment dialog / অন্য
  followup card-এও ব্যবহার হয়) **একদম অক্ষত রাখা হয়েছে** — এই দুই নতুন চিপের জন্য
  আলাদা দুটো নতুন ড্রয়েবল বানানো হয়েছে (bg_advance_premium.xml, bg_test_premium.xml),
  যাতে অন্য কোথাও প্রভাব না পড়ে।

## যাচাই করা হয়েছে
- FollowUpActivity.kt brace/paren গোনা মিলেছে (373/373, 1287/1287)
- দুটো নতুন drawable XML ভ্যালিড (xmllint pass)
- bg_visit_advance ব্যবহারকারী অন্য ৩টা ফাইল (item_draft_card.xml, dialog_nth_payment.xml,
  item_followup_card.xml) — কোনোটাই ছোঁয়া হয়নি
