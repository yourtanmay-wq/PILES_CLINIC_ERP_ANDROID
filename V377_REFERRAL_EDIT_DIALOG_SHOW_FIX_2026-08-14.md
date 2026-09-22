# V377 — Referral Income Edit/Delete জানালা কার্যকর

তারিখ: 14.08.2026 (IST)

- V376 live proof: চাপের action চললেও Edit/Delete জানালা দেখা যায়নি।
- সত্যিকারের কারণ: `openReferralEdit()` সব controls তৈরি করলেও শেষে `parts.dialog.show()` অনুপস্থিত ছিল।
- একই Activity-এর অন্যান্য কার্যকর premium dialog-এর প্রমাণিত পদ্ধতি মিলিয়ে ঠিক সেই এক লাইন যোগ হয়েছে।
- V376-এর দৃশ্যমান তিন বক্সের one-tap action, পুরনো entry নিরাপদে মিলানো এবং Ref. Due পুনর্গণনা অক্ষত।
- অন্য ডিজাইন, হিসাব, Web, Database বা workflow বদলানো হয়নি।
