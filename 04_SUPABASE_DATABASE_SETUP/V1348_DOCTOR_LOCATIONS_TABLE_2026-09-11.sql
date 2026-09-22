-- V1348 (১১.০৯.২০২৬, TK-নির্দেশ — "staff/branch/doctor সব"-এর লোকেশন, ডাক্তারের অংশ)
-- ডাক্তারের ফোনে কোনো নতুন বোতাম/নোটিফিকেশন নেই (TK-র স্পষ্ট নিষেধ) — শুধু
-- ফোনের নিজের অনুমতি থাকলে আর লোকেশন এমনিতেই অন থাকলে, শেষবার-দেখা
-- অবস্থানটা এই একটামাত্র সারিতে (প্রতি ডাক্তার একটা সারি, mobile ধরে) জমা
-- হবে। মাস্টার এটা দেখবেন Partner Shares-এর ডাক্তার-কার্ডে "Location" বোতামে।
--
-- ⛔ কোনো history/track নয় — শুধু "শেষবার কোথায় দেখা গেছে, কখন"। IN/OUT TIME
--    বা কিমি-হিসাবের সাথে এর কোনো সম্পর্ক নেই (সেটা staff/branch-এর জন্য
--    আলাদা wn.field_visit_days টেবিলে, এটা তার সাথে গোলায়নি)।
-- ⛔ RLS এই প্রকল্পে এখনো বসানো হয়নি (TK-র নিজের সিদ্ধান্ত, "যখন করতে হবে
--    আমি বলব") — তাই এখানেও নতুন করে বসানো হয়নি, বাকি wn schema-র টেবিলের
--    সাথে সামঞ্জস্য রেখে।

-- ⛔ আসল ডেটাবেসে wn schema আগে থেকেই আছে (call_taps/field_visit_days-এর
--    ঘর) — IF NOT EXISTS থাকায় সেখানে এটা নিঃশব্দে কিছুই বদলাবে না, শুধু
--    এই SQL-টাকেই স্বনির্ভর করল (নকল/টেস্ট ডেটাবেসেও একাই চালানো যায়)।
CREATE SCHEMA IF NOT EXISTS wn;

CREATE TABLE IF NOT EXISTS wn.doctor_locations (
    mobile      text PRIMARY KEY,
    lat         double precision NOT NULL,
    lng         double precision NOT NULL,
    accuracy_m  double precision,
    updated_at  timestamptz NOT NULL DEFAULT now()
);
