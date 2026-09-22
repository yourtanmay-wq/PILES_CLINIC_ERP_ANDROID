# V374 — Referral Income Edit খোলার আসল বাধা সংশোধন

তারিখ: 14.08.2026 (IST)

- V373 ফোন-প্রমাণে তিন চাপ ধরা পড়লেও Edit খোলেনি।
- কারণ: পুরনো ID-বিহীন এন্ট্রিতে Edit খোলার আগেই Cloud write করা হচ্ছিল।
- এখন তৃতীয় চাপেই Edit/Delete সঙ্গে সঙ্গে খুলবে।
- Master Save/Delete করলে তারিখ+পুরনো অঙ্ক+Paid/Unpaid+রোগীর নাম মিলিয়ে একটিমাত্র নিশ্চিত সারি একই Cloud write-এ বদলাবে; আলাদা preliminary write নেই।
- একটিমাত্র নিশ্চিত মিল না হলে কিছু বদলাবে না। Staff/Doctor-এর আগের approval নিয়ম অক্ষত।
- অন্য ডিজাইন, টাকা, Paid/Due হিসাব, Web এবং Database অপরিবর্তিত।
