# V356 STAFF PROFILE / PHOTO / SALARY / MASTER — WORK LOG

**তারিখ ও সময়:** 13.08.2026, 02:04 AM IST  
**কাজের কর্তা:** ChatGPT  
**Owner-এর প্রমাণ:** V353 চলমান APK-এর 01–05 Screenshot এবং Salary Screenshot।

## যাচাইয়ে সত্য পাওয়া সমস্যা

- Staff Photos এবং Staff Profiles আলাদা ছিল।
- Profile photo বদলাতে গোপনে তিনবার চাপতে হতো।
- Master-এর Profile field edit-ও গোপনে তিনবার চাপলে চালু হতো।
- Monthly Salary set/edit-ও Salary লেখায় তিনবার চাপলে খুলত; সাধারণ বোতাম ছিল না।

## কার্যকর পরিবর্তন

- More menu থেকে শুধু `Staff Photos` card সরানো হয়েছে।
- Staff Profile-এর ভিতরে সরাসরি `Add / Change Photo` বোতাম দেওয়া হয়েছে।
- Master-এর জন্য সরাসরি `Edit Profile` বোতাম দেওয়া হয়েছে; চাপলে সব profile field edit করা যায় এবং আগের Save দিয়েই সংরক্ষণ হয়।
- Salary screen-এ `Set Monthly Salary` / `Edit Monthly Salary` সরাসরি বোতাম দেওয়া হয়েছে।
- পুরনো আলাদা Staff Photos-এ এই ফোনে রাখা ছবি থাকলে cloud profile photo না থাকার ক্ষেত্রে fallback করে Profile-এ দেখাবে; Profile Save করলে একই `photo_data`-তে থাকবে।

## সুরক্ষা ও Free Plan

- পুরনো photo বা salary data delete করা হয়নি।
- নতুন Supabase table/bucket/SQL যোগ হয়নি।
- বিদ্যমান `hr.staff_profiles.photo_data`, `hr.salary_config`, `hr.salary_payments`-ই ব্যবহার করা হয়েছে।
- List screen-এ সব ছবি download করার পুরনো নিষেধ অক্ষত; তাই Free Plan-এ অপ্রয়োজনীয় ছবি download বাড়েনি।
- অন্য Design, Patient, Payment, Workflow, Role বা Branch rule বদলানো হয়নি।
