-- ============================================================================
-- V316 (11.08.2026, B628) — Referral Income এন্ট্রি এডিট/ডিলিটের মাস্টার-অনুমোদন
--
-- কী: DoctorVisit → View All → Referral Income এন্ট্রিতে **তিনবার চাপ** দিলে এডিট
--     খোলে (টাকা + Paid/Unpaid বদলানো যায় + Delete)। মাস্টার ও একই-দিনের
--     স্টাফ/ডাক্তার সরাসরি বদলায়; **দিন পেরিয়ে গেলে** স্টাফ/ডাক্তার শুধু একটা
--     অনুরোধ পাঠায় → মাস্টার Approve করলে তবেই বদলায় (payment_edit_requests-এর
--     হুবহু একই প্রমাণিত প্যাটার্ন)। এই টেবিলটা সেই pending অনুরোধ রাখে।
--
-- ⛔ কোনো পুরনো টেবিল/ডেটা ছোঁয় না — শুধু নতুন একটা টেবিল যোগ।
-- ⛔ RLS চালু করা হয় না (অন্য টেবিলগুলোর মতোই), তাই অ্যাপ আগের মতোই পড়তে/লিখতে
--    পারে; কোনো লগইন/রোল/অনুমতি বদলায় না।
-- একবার চালালেই হবে; বারবার চালালেও ক্ষতি নেই (IF NOT EXISTS)।
-- ============================================================================

create table if not exists public.referral_edit_requests (
  id              text primary key,
  "docId"         text,
  "entryId"       text,
  "docName"       text,
  "docMobile"     text,
  branch          text,
  patient         text,
  "patientMobile" text,
  "oldAmount"     double precision,
  "newAmount"     double precision,
  "oldStatus"     text,
  "newStatus"     text,
  "isDelete"      boolean default false,
  reason          text,
  "requestedBy"   text,
  "requestedByName" text,
  "requestedAt"   text,
  status          text default 'pending',
  "approvedBy"    text,
  "approvedAt"    text,
  "createdAt"     text,
  "updatedAt"     text
);

-- PostgREST-কে নতুন টেবিলটা চিনতে বলা (সাধারণত নিজে থেকেই হয়):
notify pgrst, 'reload schema';
