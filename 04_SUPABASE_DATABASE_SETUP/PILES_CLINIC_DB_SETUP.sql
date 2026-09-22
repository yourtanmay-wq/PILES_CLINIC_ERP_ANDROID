-- PILES CLINIC — create all tables in a fresh Supabase project
-- Safe to run more than once (uses IF NOT EXISTS).

create table if not exists public.enquiries (
  "id" text primary key,
  "date" text,
  "branch" text,
  "name" text,
  "mobile" text,
  "disease" text,
  "address" text,
  "remarks" text,
  "timeType" text,
  "receivedBy" text,
  "status" text,
  "stage" text,
  "callCount" text,
  "nextFollow" text,
  "appointmentDate" text,
  "convertedPatientId" text,
  "convertedAt" text,
  "createdBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.enquiries disable row level security;

create table if not exists public.patients (
  "id" text primary key,
  "patientId" text,
  "date" text,
  "registrationDate" text,
  "visitDate" text,
  "name" text,
  "mobile" text,
  "altMobile" text,
  "branch" text,
  "age" text,
  "sex" text,
  "address" text,
  "occupation" text,
  "refBy" text,
  "refDoctor" text,
  "refDoctorMobile" text,
  "disease" text,
  "complaint" text,
  "diagnosis" text,
  "sinceWhen" text,
  "medicalHistory" text,
  "previousTreatment" text,
  "previousResult" text,
  "previousCost" text,
  "treatmentDuration" text,
  "doctorAdvice" text,
  "doctorFullNote" text,
  "decision" text,
  "stage" text,
  "queue" text,
  "doctorComplete" text,
  "bill" text,
  "discount" text,
  "photo" text,
  "createdBy" text,
  "registeredBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.patients disable row level security;

create table if not exists public.payments (
  "id" text primary key,
  "payType" text,
  "payLabel" text,
  "paymentLabel" text,
  "patientId" text,
  "mobile" text,
  "branch" text,
  "name" text,
  "date" text,
  "amount" text,
  "mode" text,
  "remarks" text,
  "editHistory" jsonb,
  "editedAt" text,
  "editedBy" text,
  "receivedBy" text,
  "createdBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.payments disable row level security;

create table if not exists public.followups (
  "id" text primary key,
  "refId" text,
  "mobile" text,
  "name" text,
  "branch" text,
  "disease" text,
  "address" text,
  "stage" text,
  "date" text,
  "registrationDate" text,
  "visitDate" text,
  "lastRemark" text,
  "nextFollow" text,
  "callCount" text,
  "status" text,
  "history" jsonb,
  "photo" text,
  "createdBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.followups disable row level security;

create table if not exists public.medical (
  "id" text primary key,
  "patientId" text,
  "type" text,
  "date" text,
  "selected" jsonb,
  "days" jsonb,
  "photos" text,
  "details" text,
  "nextFollow" text,
  "diagnosis" text,
  "decision" text,
  "doctorFullNote" text,
  "name" text,
  "mobile" text,
  "branch" text,
  "createdBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.medical disable row level security;

create table if not exists public.products (
  "id" text primary key,
  "kind" text,
  "product" text,
  "customer" text,
  "mobile" text,
  "qty" text,
  "price" text,
  "bill" text,
  "total" text,
  "deposit" text,
  "due" text,
  "mode" text,
  "remarks" text,
  "date" text,
  "branch" text,
  "receivedBy" text,
  "createdBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.products disable row level security;

create table if not exists public.doctor_visits (
  "id" text primary key,
  "name" text,
  "mobile" text,
  "area" text,
  "remarks" text,
  "date" text,
  "branch" text,
  "lastCallDate" text,
  "nextCallDate" text,
  "callStatus" text,
  "status" text,
  "callHistory" jsonb,
  "referralPayments" jsonb,
  "referralPaid" text,
  "referralDue" text,
  "createdBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.doctor_visits disable row level security;

create table if not exists public.briefings (
  "id" text primary key,
  "date" text,
  "title" text,
  "message" text,
  "targets" jsonb,
  "seen" jsonb,
  "replies" jsonb,
  "hiddenFor" jsonb,
  "deletedAt" text,
  "deletedBy" text,
  "branch" text,
  "createdBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.briefings disable row level security;

create table if not exists public.trash (
  "id" text primary key,
  "table" text,
  "record" jsonb,
  "deletedAt" text,
  "deletedBy" text
);
alter table public.trash disable row level security;

create table if not exists public.backuprecords (
  "id" text primary key,
  "date" text,
  "reason" text,
  "status" text,
  "size" text,
  "payload" jsonb,
  "by" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.backuprecords disable row level security;

create table if not exists public.usercredentials (
  "id" text primary key,
  "mobile" text,
  "role" text,
  "name" text,
  "branch" text,
  "password" text,
  "changedBy" text,
  "createdAt" text,
  "updatedAt" text
);
alter table public.usercredentials disable row level security;

create table if not exists public.activity_logs (
  "id" text primary key,
  "module" text,
  "action" text,
  "recordId" text,
  "oldValue" jsonb,
  "newValue" jsonb,
  "userMobile" text,
  "userName" text,
  "userRole" text,
  "userBranch" text,
  "createdAt" text
);
alter table public.activity_logs disable row level security;
