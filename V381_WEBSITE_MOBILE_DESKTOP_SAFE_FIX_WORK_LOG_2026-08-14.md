# V381 — Website Mobile/Desktop Safe Fix

**তারিখ ও সময়:** 14.08.2026 · 03.17 PM IST  
**Owner অনুমতি:** নিশ্চিত সন্দেহগুলো নিরাপদে ঠিক করার অনুমতি।

## সম্পন্ন
- Profile ও Work Notebook: 520px পর্যন্ত 1 column; 521px+ আগের 2 column।
- Doctor/RMP Previous Records: পুরোনো manual new-entry Paid/Unpaid form বন্ধ।
- পুরোনো Referral Income history এবং অনুমোদিত Edit/Delete অক্ষত।
- নতুন commission/payment-এর V380 Cloud workflow অক্ষত।
- বদলানো Web file-এর cache link V381; Android identity V381 / 3.81।

## অপরিবর্তিত
- SQL/Database data, commission calculation, role permission, approved design, Android workflow এবং অন্য module।

## যাচাই
- সব Web JavaScript syntax: PASS
- `index.html` local asset references: PASS
- পুরোনো entry form আর reachable নয়: PASS
- বাস্তব login/Supabase live test: Owner-এর live environment-এ বাকি।

## 03.23 PM IST — RMP live-photo correction
- Android Details এখন history-এর Paid/Unpaid rows থেকেই Ref. Paid/Ref. Due যোগ করে; stale scalar আর নয়।
- Referral Income child flow খুললে Details আর forced-close হয় না; শেষ হলে একই Details-এ ফেরে।
- Web একই history total ও modal-return আগে থেকেই করছিল; যাচাই করে অপ্রয়োজনীয় code change করা হয়নি।
## 03:53 PM IST — RMP Unallocated Advance

- Confirmed ₹20,000 Online for RMP TK BISWAS is stored separately until older patients are matched.
- Android and Web show Paid / Adjusted / Available and allow only Master to allocate to a verified patient of the same RMP.
- Duplicate opening entry, over-allocation, wrong-RMP allocation, and unverified success are blocked.
