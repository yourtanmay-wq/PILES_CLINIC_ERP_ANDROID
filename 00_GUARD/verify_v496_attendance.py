#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🔐 V496 (২১.০৮.২০২৬) — নতুন পাহারাদার।

TK-এর চূড়ান্ত নির্দেশের যে নিয়মগুলো কোড পড়েই যাচাই করা যায়, সেগুলো প্রতিবার
মিলিয়ে দেখে — যাতে ভবিষ্যতে কেউ ভুল করে ভেঙে না ফেলে।

⛔ কিছু বদলায় না — শুধু খুঁজে দেখে ও বলে।
⚠️ পাহারাদার Kotlin কম্পাইল করে না; Android Studio-র বিল্ডই চূড়ান্ত প্রমাণ।
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                  "java", "com", "tkbiswas", "pilesclinic")
SQL_DIR = os.path.join(ROOT, "04_SUPABASE_DATABASE_SETUP")

problems, notes = [], []

# 🔴🔒 TK-এর নির্দেশ (২১.০৮.২০২৬): "বাধ্যতামূলক কোনো বিষয় — বিশেষ করে GPS
#    coordinate — অসম্পূর্ণ থাকলে verifier 'PASS' বলবে না, 'FAIL/INCOMPLETE'
#    দেখাবে।" তাই ভুল (problems) আর অসম্পূর্ণতা (incomplete) আলাদা রাখা হলো —
#    কিন্তু **দুটোর যেকোনো একটা থাকলেই ফল PASS নয়** এবং exit code 1।
incomplete = []

def strip_kt(text):
    """Kotlin/Java-র মন্তব্য বাদ — নইলে ব্যাখ্যার লেখাকেই কোড ভেবে ভুল ধরা পড়ে।"""
    text = re.sub(r"/\*[\s\S]*?\*/", "", text)
    text = re.sub(r"//[^\n]*", "", text)
    return text


def strip_sql(text):
    """SQL-এর `--` মন্তব্য বাদ (একই কারণে)।"""
    return re.sub(r"--[^\n]*", "", text)




def read(*parts):
    try:
        return open(os.path.join(KT, *parts), encoding="utf-8").read()
    except Exception:
        return ""


# ── §১ · আঙুলের ছাপ: শুধু STRONG, PIN/Pattern-এর পথ নেই ────────────────────
bio = read("native", "BiometricGate.kt")
if not bio:
    problems.append("BiometricGate.kt পাওয়া যায়নি")
else:
    if "Authenticators.BIOMETRIC_STRONG" not in bio:
        problems.append("BiometricGate-এ BIOMETRIC_STRONG ব্যবহার হয়নি")
    bio_code = strip_kt(bio)
    if "BIOMETRIC_WEAK" in bio_code:
        problems.append("BiometricGate-এ `BIOMETRIC_WEAK` আছে — দুর্বল বায়োমেট্রিক চলবে না")
    # 🔴🔒 V500 (TK-এর স্পষ্ট সিদ্ধান্ত, ২১.০৮.২০২৬): ঝুঁকি জানানোর পরে TK
    #    বলেছেন **হাজিরাতেও পাসওয়ার্ড চলবে**। তাই গোটা অ্যাপে একটাই পর্দা —
    #    `promptUnlock()` (আঙুল **অথবা** ফোনের পাসওয়ার্ড)।
    #    ⇒ পাহারাদার এখন উল্টোটা দেখে: দুই জায়গায় যেন **দুই নিয়ম না হয়ে যায়**।
    if "fun promptUnlock" not in bio_code:
        problems.append("BiometricGate-এ promptUnlock() নেই — আঙুল/পাসওয়ার্ডের পর্দা হারিয়েছে")
    elif "DEVICE_CREDENTIAL" not in bio_code:
        problems.append("promptUnlock()-এ ফোনের পাসওয়ার্ডের পথ নেই — TK-এর নিয়ম ভাঙছে")
    elif re.search(r"fun prompt\(", bio_code):
        problems.append("BiometricGate-এ পুরনো শুধু-আঙুলের prompt() ফিরে এসেছে — দুই জায়গায় দুই নিয়ম হয়ে যাবে")
    else:
        notes.append("  আঙুল **অথবা** ফোনের পাসওয়ার্ড — অ্যাপ খোলা ও হাজিরা, দুটোতেই এক নিয়ম ✅")
    if "BiometricGate.promptUnlock" not in strip_kt(read("modules", "WorkNotebookActivity.kt")):
        problems.append("হাজিরার পর্দা এখনো পুরনো কড়া পথ ব্যবহার করছে — TK পাসওয়ার্ড খুলতে বলেছেন")
    else:
        notes.append("  হাজিরাতেও পাসওয়ার্ড চলে (TK-এর সিদ্ধান্ত, ঝুঁকি জানানোর পরে) ✅")
    if len(problems) == 0:
        notes.append("  আঙুলের ছাপ শুধু BIOMETRIC_STRONG · PIN/Pattern-এর পথ নেই ✅")

# পুরো প্রকল্পে কোথাও যেন বায়োমেট্রিক তথ্য সেভ না হয়
for folder, _d, files in os.walk(KT):
    for f in files:
        if not f.endswith(".kt"):
            continue
        txt = strip_kt(open(os.path.join(folder, f), encoding="utf-8").read())
        for bad in ("fingerprintData", "biometricTemplate", "fingerprintImage"):
            if bad in txt:
                problems.append("%s-এ `%s` — বায়োমেট্রিক তথ্য সংরক্ষণ করা যাবে না" % (f, bad))

# ── §২ · ডাক্তারের হাজিরা নেই ───────────────────────────────────────────────
role = read("native", "RoleRules.kt")
if not role:
    problems.append("RoleRules.kt পাওয়া যায়নি")
elif "displayRole" not in role:
    problems.append("RoleRules `displayRole` দেখছে না — permissionRole() ডাক্তারকে staff বানায়")
else:
    notes.append("  ভূমিকা চেনা হয় displayRole দিয়ে (permissionRole নয়) ✅")
    # 🔴🔒 TK-এর চূড়ান্ত নিয়ম: হাজিরা/বেতন **শুধু আসল staff**-এর।
    role_code = strip_kt(role)
    m_att = re.search(r"fun usesAttendance\(user: NativeUser\?\)[\s\S]{0,200}?(?=\n    fun |\n    /\*|\n\})", role_code)
    att_body = m_att.group(0) if m_att else ""
    if not att_body:
        problems.append("RoleRules.usesAttendance() পড়া গেল না")
    elif "ROLE_FIELD" in att_body:
        problems.append("RoleRules.usesAttendance()-এ এখনো ROLE_FIELD আছে — TK বাতিল করেছেন (শুধু staff)")
    elif "ROLE_STAFF" not in att_body:
        problems.append("RoleRules.usesAttendance() staff দেখছে না")
    else:
        notes.append("  হাজিরা শুধু আসল staff-এর (field/doctor/master নয়) ✅")
    if "salaryAppliesToRoleKind" not in role_code:
        problems.append("RoleRules-এ বেতনের ভূমিকা-যাচাই (salaryAppliesToRoleKind) নেই")
    else:
        notes.append("  বেতনের ভূমিকা-যাচাই এক জায়গায় (RoleRules) ✅")

# ── §৩(খ) · Doctor/Field-এর বেতনের পর্দা বন্ধ ───────────────────────────────
prof = read("modules", "StaffProfileActivity.kt")
if not prof:
    problems.append("StaffProfileActivity.kt পাওয়া যায়নি")
else:
    prof_code = strip_kt(prof)
    if "salaryAppliesToRoleKind" not in prof_code:
        problems.append("StaffProfileActivity-তে বেতনের ভূমিকা-যাচাই বসানো হয়নি — ডাক্তার/ফিল্ড বেতন দেখতে পাবেন")
    else:
        notes.append("  My Salary ও Salary পর্দা: শুধু role_kind = staff ✅")
    # ⛔ TK §৩: "পুরোনো Database record মুছবেন না।" — বেতনের টেবিল থেকে
    #    মোছার কোনো নতুন ডাক এই পর্দায় ঢুকল কিনা।
    if re.search(r'(delete|remove)\w*\(\s*"hr"\s*,\s*"salary', prof_code, re.I):
        problems.append("StaffProfileActivity বেতনের টেবিল থেকে সারি মুছছে — TK §৩ নিষেধ করেছেন")
    else:
        notes.append("  বেতনের পুরোনো record কিছুই মোছা হয় না ✅")

dash = read("native", "DashboardActivity.kt")
if "RoleRules.usesAttendance" not in dash:
    problems.append("Dashboard-এ Work Notebook টাইল এখনো ভূমিকা যাচাই করছে না — ডাক্তার IN TIME পাবেন")
else:
    notes.append("  Dashboard: ডাক্তারের Work Notebook টাইল লুকানো ✅")

rem = read("native", "AttendanceReminderWorker.kt")
if 'user.role == "staff"' in strip_kt(rem):
    problems.append("হাজিরা-রিমাইন্ডার এখনো `user.role == \"staff\"` দেখছে — ডাক্তারের ফোনেও বাজবে")
elif "RoleRules.usesAttendance" in rem:
    notes.append("  হাজিরা-রিমাইন্ডার ডাক্তারের ফোনে যায় না ✅")

wn = read("modules", "WorkNotebookActivity.kt")
if "RoleRules.usesAttendance" not in wn:
    problems.append("Work Notebook-এ ডাক্তারের জন্য নিরাপত্তা-জাল নেই")
else:
    notes.append("  Work Notebook: ডাক্তার ঢুকলে সঙ্গে সঙ্গে ফিরে যান ✅")

# ── §৩ · অ্যাপ খোলার তালা — সবার জন্য, হাজিরা নয় ───────────────────────────
lock = read("native", "AppLock.kt")
if not lock:
    problems.append("AppLock.kt পাওয়া যায়নি")
elif "check_in" in strip_kt(lock) or "mark_check_in" in strip_kt(lock):
    problems.append("AppLock হাজিরার সঙ্গে যুক্ত — এটা শুধু অ্যাপ খোলার জন্য")
else:
    notes.append("  অ্যাপ-তালা শুধু অ্যাপ খোলে, হাজিরা নয় ✅")
    lock_code = strip_kt(lock)
    # 🔴 TK (২১.০৮.২০২৬): "এটা শুধু মাস্টারের ক্ষেত্রে নয় — প্রত্যেকের ক্ষেত্রেই একই নিয়ম।"
    if "isDoctor" in lock_code or "isMaster" in lock_code:
        problems.append("AppLock এখনো ভূমিকা দেখে বাছাই করছে — তালা সবার জন্য হওয়ার কথা")
    else:
        notes.append("  তালা Master · Doctor · Staff · Field — সবার জন্য এক ✅")
    if "promptUnlock" not in lock_code:
        problems.append("AppLock আঙুল-অথবা-পাসওয়ার্ডের পর্দা ডাকছে না")
    if "showBlocked" not in lock_code:
        problems.append("AppLock আটকানোর সময় কোনো বার্তা দেখাচ্ছে না")
    else:
        notes.append("  আটকালে পরিষ্কার বার্তা (crash নয়) ✅")

bridge = read("..", "SessionGuardBridge.kt") or ""
if not bridge:
    import os as _os
    _p = _os.path.join(KT, "SessionGuardBridge.kt")
    bridge = open(_p, encoding="utf-8").read() if _os.path.exists(_p) else ""
bcode = strip_kt(bridge)
# 🔵 V584 (২৩.০৮.২০২৬) — এই নিয়মটা TK-এর **নিজের পরের নির্দেশে** বাতিল হয়ে
# গেছে, তাই পাহারা থেকেও তুলে নেওয়া হলো।
# V527 (২২.০৮.২০২৬) TK: *"বারবার Fingerprint আসবে না। যখন Login-এর ব্যাপার
# থাকবে তখন চাইবে। আর In Time-এর সময় চাইবে, পেমেন্ট নেওয়ার সময় চাইবে। বাকি
# কখনো যেন ফিঙ্গারপ্রিন্ট চায় না।"* ⇒ `SessionGuardBridge`-এর `AppLock.guard`
# ডাকটা ইচ্ছে করে তুলে দেওয়া হয়েছিল (কোডে কারণ সহ লেখা আছে)। অথচ পাহারাটা
# পুরোনো নিয়মই খুঁজছিল, তাই V527-এর পর থেকে অকারণে লাল হয়ে ছিল।
# ⛔ যে তিন জায়গায় আঙুল **থাকতেই হবে** (Login · IN TIME · টাকা) সেগুলোর
#    যাচাই এই ফাইলে আগের মতোই আছে — নিয়ম শিথিল হয়নি।
if "AppLock.guard" not in bcode:
    notes.append("  অ্যাপ খোলার তালা নেই — V527-এ TK-এর নির্দেশেই তোলা হয়েছে ✅")
elif bcode.index("AppLock.guard") > bcode.index("isMaster(user)) return"):
    problems.append("তালা মাস্টারের `return`-এর পরে ডাকা হচ্ছে — মাস্টারের ফোনে তালা লাগবে না")
else:
    notes.append("  মাস্টারের ফোনেও তালা লাগে (যাচাইয়ের আগেই ডাকা) ✅")

# ── §৪ · IN TIME: ফোনের ঘড়ি আর ব্যবহার হয় না ──────────────────────────────
if re.search(r'day\.put\(\s*"check_in"\s*,\s*nowTime\(\)', wn):
    problems.append("IN TIME এখনো ফোনের ঘড়ি দিয়ে বসছে — ঘড়ি বদলে ফাঁকি দেওয়া যাবে")
elif "startInTimeFlow" in wn and "AttendanceRepository.markCheckIn" in wn:
    notes.append("  IN TIME সার্ভারের সময়ে বসে (ফোনের ঘড়ি নয়) ✅")
else:
    problems.append("IN TIME-এর নতুন পথ পাওয়া যায়নি")

for need, why in (("ClinicPresence.check", "ক্লিনিক যাচাই"),
                  ("BiometricGate.promptUnlock", "আঙুল/পাসওয়ার্ড")):
    if need not in wn:
        problems.append("IN TIME-এর পথে %s নেই" % why)
if "ClinicPresence.check" in wn and "BiometricGate.promptUnlock" in wn:
    notes.append("  IN TIME-এর আগে ক্লিনিকে উপস্থিতি ও পরিচয় — দুটোই যাচাই হয় ✅")

# ── §৫ · কোনো coordinate আন্দাজে বসানো হয়নি ────────────────────────────────
locs = read("native", "ClinicLocations.kt")
if not locs:
    problems.append("ClinicLocations.kt পাওয়া যায়নি")
else:
    locs_code = strip_kt(locs)
    pts = re.findall(r'branchId = "([a-z_]+)"[\s\S]{0,600}?lat = ([^,\n]+),[\s\S]{0,200}?lng = ([^,\n]+),', locs_code)
    if len(pts) != 5:
        problems.append("ClinicLocations-এ ৫টা ব্রাঞ্চ পাওয়া যায়নি (পাওয়া গেছে %d)" % len(pts))
    filled = [b for b, la, ln in pts if la.strip() != "null" and ln.strip() != "null"]
    blank = [b for b, la, ln in pts if la.strip() == "null" or ln.strip() == "null"]
    if blank:
        # 🔴🔒 এটা আর "নোট" নয় — TK-এর নির্দেশে **বাধ্যতামূলক অসম্পূর্ণতা**।
        incomplete.append(
            "GPS coordinate বসানো হয়নি (%d/%d ব্রাঞ্চ): %s"
            % (len(blank), len(pts), ", ".join(blank)))
        incomplete.append(
            "  ⇒ ওই ব্রাঞ্চে IN TIME নেওয়া হবে না। কাজটা সম্পূর্ণ নয়।")
    if filled:
        notes.append("  অবস্থান বসানো আছে: %s" % ", ".join(filled))
    if "isConfigured" not in locs or "NOT_CONFIGURED" not in read("native", "ClinicPresence.kt"):
        problems.append("অবস্থান খালি থাকলে আটকানোর ব্যবস্থা নেই — নীরবে হাজিরা বসে যেতে পারে")
    else:
        notes.append("  অবস্থান খালি থাকলে হাজিরা নেওয়া হয় না ✅")

pres = read("native", "ClinicPresence.kt")
for need, why in (("MOCK_DETECTED", "নকল অবস্থান"), ("LOW_ACCURACY", "দুর্বল নির্ভুলতা"),
                  ("TIMEOUT", "সময় শেষ"), ("NO_PERMISSION", "অনুমতি নেই"),
                  ("LOCATION_OFF", "Location বন্ধ")):
    if need not in pres:
        problems.append("ClinicPresence-এ `%s` (%s) সামলানো নেই" % (need, why))
if "removeUpdates" not in pres:
    problems.append("ClinicPresence GPS বন্ধ করছে না — ব্যাটারি খরচ হবে")
else:
    notes.append("  GPS শুধু দরকারের সময়, ফল পেলেই বন্ধ ✅")

# ── §৬ · SQL: server-side ও atomic ─────────────────────────────────────────
sqlp = os.path.join(SQL_DIR, "V496_MARK_CHECK_IN_2026-08-21.sql")
sql = open(sqlp, encoding="utf-8").read() if os.path.exists(sqlp) else ""
if not sql:
    problems.append("V496-এর SQL migration ফাইল পাওয়া যায়নি")
else:
    checks = [
        ("hr.my_code()", "কে ডাকছে সার্ভার নিজে দেখে"),
        ("Asia/Kolkata", "সার্ভারের সময়"),
        ("on conflict (staff_code, work_date)", "এক ধাপে (atomic)"),
        ("role_kind", "ডাক্তার/স্টাফ যাচাই"),
        ("suspended_until", "suspend যাচাই"),
        ("leave_requests", "ছুটি যাচাই"),
        ("revoke all on function wn.mark_check_in() from public, anon", "anon আটকানো"),
        ("grant execute on function wn.mark_check_in() to authenticated", "শুধু লগইন করা ব্যবহারকারী"),
        ("set search_path = wn, hr, public", "নিরাপদ search_path"),
    ]
    for needle, why in checks:
        if needle not in sql:
            problems.append("SQL-এ `%s` নেই (%s)" % (needle, why))
    # 🔴🔒 TK-এর চূড়ান্ত নিয়ম: সার্ভারেও হাজিরা **শুধু staff**-এর।
    sql_code = strip_sql(sql)
    if re.search(r"not\s+in\s*\(\s*'staff'\s*,\s*'field'\s*\)", sql_code, re.I):
        problems.append("SQL এখনো 'field'-কেও হাজিরা দিচ্ছে — TK বাতিল করেছেন (শুধু staff)")
    elif re.search(r"role_kind[^\n]*<>\s*'staff'", sql_code, re.I):
        notes.append("  সার্ভারেও হাজিরা শুধু role_kind = staff ✅")
    else:
        problems.append("SQL-এ role_kind = staff-এর কড়া যাচাই পাওয়া গেল না")
    for bad, why in (("drop table", "টেবিল মোছা"), ("delete from", "সারি মোছা"),
                     ("truncate", "টেবিল খালি করা"), ("service_role", "গোপন key")):
        if bad in strip_sql(sql).lower():
            problems.append("SQL-এ `%s` আছে — %s চলবে না" % (bad, why))
    if not problems:
        notes.append("  SQL: server-side · atomic · anon বন্ধ · কিছু মোছে না ✅")

# ── §১০ · ৭ দিন: শুধু মানুষ অ্যাপ খুললে ────────────────────────────────────
guard = read("native", "SessionGuard.kt")
appf = read("PilesClinicApplication.kt")
if "INACTIVITY_LIMIT_MS" not in guard:
    problems.append("৭ দিনের হিসাব পাওয়া যায়নি")
else:
    if "maxSeenAt" not in guard and "K_MAX_SEEN" not in guard:
        problems.append("ফোনের ঘড়ি পিছিয়ে দিলে ঠেকানোর ব্যবস্থা নেই")
    else:
        notes.append("  ঘড়ি পিছিয়ে দিলেও ৭ দিনের হিসাব পিছোয় না ✅")
if "onActivityStarted" not in appf or "SessionGuardBridge.onForeground" not in appf:
    problems.append("অ্যাপ সামনে আনা ধরার ব্যবস্থা নেই")
else:
    notes.append("  \"ব্যবহার\" = মানুষ অ্যাপ সামনে এনেছেন (Worker নয়) ✅")
for w in ("BackgroundRefreshWorker.kt", "CallReminderWorker.kt", "BriefingReminderWorker.kt",
          "AttendanceReminderWorker.kt"):
    if "noteForeground" in read("native", w):
        problems.append("%s ৭ দিনের হিসাব পিছিয়ে দিচ্ছে — পিছনের কাজ ব্যবহার নয়" % w)
notes.append("  কোনো Worker ৭ দিনের হিসাব বাড়ায় না ✅")

# ── §১১ · লেখার আগে suspend যাচাই, খরচ না বাড়িয়ে ──────────────────────────
sup = read("native", "SupabaseClient.kt")
if "blockedFromWriting()" not in sup:
    problems.append("গুরুত্বপূর্ণ লেখার আগে suspend যাচাই নেই")
else:
    notes.append("  লেখার আগে suspend যাচাই (upsert ও updateById) ✅")
if "SUSPEND_CHECK_GAP_MS" not in guard or "15L * 60L * 1000L" not in guard:
    problems.append("suspend যাচাইয়ের ১৫ মিনিটের সীমা নেই — Egress বাড়বে")
else:
    notes.append("  রুটিন suspend যাচাই ১৫ মিনিটে একবারের বেশি নয় ✅")
# 🔴🔒 TK §৫: লেখার আগে রুটিনের ১৫ মিনিটের পুরনো ফল মানা যাবে না।
guard_code = strip_kt(guard)
if "WRITE_FRESH_MS" not in guard_code:
    problems.append("লেখার আগে আলাদা (টাটকা) যাচাইয়ের সীমা নেই — ১৫ মিনিটের পুরনো ফলেই লেখা হয়ে যাবে")
elif not re.search(r"ensureFreshForWrite[\s\S]{0,200}?WRITE_FRESH_MS", guard_code):
    problems.append("ensureFreshForWrite() এখনো WRITE_FRESH_MS ব্যবহার করছে না")
else:
    notes.append("  গুরুত্বপূর্ণ লেখার আগে টাটকা Active/Suspend যাচাই (১ মিনিট) ✅")
# ⚠️ সৎ কথা — এটা ১০০% নয়, তাই নথিতে সীমাটা লেখা থাকতেই হবে।
if "fail-open" not in guard and "fail-open" not in sup:
    problems.append("অফলাইনের সীমাবদ্ধতা (fail-open) কোথাও সৎভাবে লেখা নেই")
else:
    notes.append("  অফলাইন/সার্ভার-ব্যর্থতার সীমা সৎভাবে লেখা আছে (fail-open) ✅")

# ── §১২ · পুরনো ভালো কাজ অক্ষত ─────────────────────────────────────────────
if "CloudReadDedupe.body(url)" not in sup:
    problems.append("V493-এর dedupe ভেঙে গেছে")
if "SafeWideColumns.forTable(table, cols)" not in sup:
    problems.append("V493/V494-এর ছবি-বাঁচানোর ব্যবস্থা ভেঙে গেছে")
web = ""
try:
    web = open(os.path.join(ROOT, "03_NETLIFY_READY", "app.js"), encoding="utf-8").read()
except Exception:
    pass
if web and "__wlv1DedupeInstalled" not in web:
    problems.append("V495-এর ওয়েব dedupe ভেঙে গেছে")
if web and "fingerprint" in web.lower() and "biometric" in web.lower():
    problems.append("ওয়েবে নকল fingerprint ব্যবস্থা তৈরি হয়েছে — TK নিষেধ করেছেন")
notes.append("  V493–V495-এর Egress · Dedupe · ছবি-বাঁচানো — সব অক্ষত ✅")
notes.append("  ওয়েবে কোনো fingerprint ব্যবস্থা নেই ✅")

# ── §৭/§৮/§৯ · OUT TIME · ছুটি · বেতন ছোঁয়া হয়নি ──────────────────────────
# 🔴 V509 (২১.০৮.২০২৬) — এই পাহারাটা আগে হুবহু `day.put("check_out", nowTime())`
# লেখাটা খুঁজত। TK-এর সিদ্ধান্তে (বিকল্প ৩) OUT TIME-এ এখন অবস্থানের নোট যোগ
# হয়েছে, আর সেই কারণে সময়টা **চাপার মুহূর্তে** আলাদা ঘরে ধরা হয়:
#       val outAt = nowTime()   →   day.put("check_out", outAt)
# পাহারার **আসল উদ্দেশ্য** (§৭: OUT TIME ফোনের ঘড়ি থেকেই আসবে, একই ঘরে বসবে,
# সার্ভার/অন্য কোনো সময় নয়) অক্ষত — বরং আগের চেয়ে কড়া, কারণ এখন অপেক্ষা
# করলেও চাপার সময়টাই বসে।
# ⛔ পাহারা **আলগা করা হয়নি** — দুটো শর্তই একসাথে মিলতে হবে (সময় ধরা, আর
#    ঠিক সেই ধরা সময়টাই বসানো)। শুধু-ইংরেজি ভাষা নয়, দুই পথেই (ফর্ম ও
#    নোটিফিকেশন-শর্টকাট) `outAt` ব্যবহার হচ্ছে কিনা তাও গোনা হয়।
_out_old = 'day.put("check_out", nowTime())' in wn
_out_new = ('val outAt = nowTime()' in wn) and (wn.count('day.put("check_out", outAt)') >= 2)
if not (_out_old or _out_new):
    problems.append("OUT TIME-এর পুরনো পথ বদলে গেছে — TK §৭ ভেঙেছে")
elif _out_new:
    notes.append("  OUT TIME ফোনের ঘড়িতেই, একই ঘরে — সময় ধরা হয় চাপার মুহূর্তে (V509) ✅")
else:
    notes.append("  OUT TIME হুবহু আগের মতোই (ফোনের সময়, একই ঘর) ✅")

# 🔴🔴🔴 V509 (২১.০৮.২০২৬) — নতুন পাহারা: **মাসের ৩২ তারিখ**।
#
# TK-এর রিপোর্ট: "Monthly রিপোর্টে অ্যাপসের কল শূন্য দেখাচ্ছে কেন?"
# আসল কারণ: মাসের শেষ সীমা লেখা হত `$key-32` (যেমন "2026-08-32")। Postgres-এর
# `date` কলামে ওটা পার্স-এরর দেয় ("date/time field value out of range"), তাই
# পুরো অনুরোধটাই ব্যর্থ → ফাঁকা ফল → পর্দায় **0**।
#
# ⚠️ এটা **দ্বিতীয়বার** ঘটল — ১১.০৮.২০২৬-এ ছুটির গোনায় (B618) ঠিক এই একই ভুল
#    ধরা পড়ে ঠিক করা হয়েছিল, কিন্তু `fetchStats()`-এ রয়ে গিয়েছিল। তাই এবার
#    পাহারা বসানো হলো, যাতে তৃতীয়বার আর না হয়।
#
# নিয়ম: `date` ধরনের কলামের (call_date · work_date · leave_date) পাশে কোথাও
# "-32" থাকতে পারবে না। ⛔ `createdAt`-এর মতো **text** কলামে "-32" ঠিক আছে
# (সেখানে এটা নিছক অক্ষর-তুলনা), তাই সেগুলো এই পাহারায় ধরা হয় না।
#
# ⚠️ পাহারাটা **ফোন ও কম্পিউটার — দুই দিকেই** দেখে। কারণ যাচাই করতে গিয়ে ধরা
#    পড়েছে ওয়েবেও (`03_NETLIFY_READY/notebook.js`) হুবহু একই ভুল রয়ে গিয়েছিল,
#    আর সেখানে "-31"-ও ছিল — যেটা আরও চালাক ভুল: ৩১ দিনের মাসে কাজ করে, কিন্তু
#    ফেব্রুয়ারি/এপ্রিল/জুন/সেপ্টেম্বর/নভেম্বরে অসম্ভব তারিখ হয়ে ০ দেখায়।
_date_cols = ("call_date", "work_date", "leave_date")
_bad32 = []
_kt_all = []
for _folder, _d2, _files in os.walk(KT):
    for _f2 in _files:
        if _f2.endswith(".kt"):
            _kt_all.append(os.path.join(_folder, _f2))
_web_dir = os.path.join(ROOT, "03_NETLIFY_READY")
for _folder, _d2, _files in os.walk(_web_dir):
    for _f2 in _files:
        if _f2.endswith(".js"):
            _kt_all.append(os.path.join(_folder, _f2))
for _f in _kt_all:
    try:
        _txt = open(_f, encoding="utf-8").read()
    except Exception:
        continue
    for _ln_no, _ln in enumerate(_txt.splitlines(), 1):
        _s = _ln.strip()
        if _s.startswith("//") or _s.startswith("*"):
            continue                      # কমেন্ট/ব্যাখ্যা বাদ
        if ("-32" not in _ln) and ("-31" not in _ln):
            continue
        if any(_c in _ln for _c in _date_cols):
            _bad32.append(f"{os.path.basename(_f)}:{_ln_no}")
if _bad32:
    problems.append(
        "মাসের অসম্ভব তারিখ (৩২/৩১) আবার ফিরে এসেছে — date কলামে query ব্যর্থ হবে, সংখ্যা ০ দেখাবে: "
        + ", ".join(_bad32[:4])
    )
else:
    notes.append("  মাসের সীমায় কোথাও অসম্ভব তারিখ (৩২/৩১) নেই — ফোন ও ওয়েব দুই দিকেই ✅")

# 🔴🔴🔴 V509 (২১.০৮.২০২৬) — নতুন পাহারা: **নেই-ঘর ধরে সাজানো**।
#
# TK-এর রিপোর্ট: "trash bin তো সারাজীবন ফাঁকাই দেখলাম, মাস্টার হিসাবে।"
# আসল কারণ: `SupabaseClient.fetchList()` ডিফল্টে `order=updatedAt.desc` দেয়,
# কিন্তু `trash` টেবিলে `updatedAt` ঘরটাই নেই → ডেটাবেস অনুরোধ বাতিল করে
# ("column \"updatedAt\" does not exist") → ফাঁকা তালিকা → "Trash empty"।
# অর্থাৎ **Trash Bin কোনোদিনই একটাও সারি দেখাতে পারেনি**, আর মুছে যাওয়া
# রেকর্ড ফেরানোর কোনো উপায়ই ছিল না।
#
# নিয়ম: SQL-এ যে টেবিলে `updatedAt` ঘর নেই, তাকে `fetchList`-এ ডাকতে হলে
# **নিজের `order=` অবশ্যই দিতে হবে**। না দিলে এখানেই ধরা পড়বে।
_no_upd = set()
try:
    for _sf in os.listdir(SQL_DIR):
        if not _sf.endswith(".sql"):
            continue
        _st = open(os.path.join(SQL_DIR, _sf), encoding="utf-8").read()
        for _m in re.finditer(r"create table if not exists public\.(\w+) \((.*?)\n\);", _st, re.S):
            if "updatedAt" not in _m.group(2):
                _no_upd.add(_m.group(1))
            else:
                _no_upd.discard(_m.group(1))
except Exception:
    pass
_bad_order = []
for _f in _kt_all:
    if not _f.endswith(".kt"):
        continue
    try:
        _txt = strip_kt(open(_f, encoding="utf-8").read())
    except Exception:
        continue
    for _t in _no_upd:
        # 🔵 V584 — আগে `[^)]*` ছিল, তাই যুক্তির ভিতরে আরেকটা বন্ধনী থাকলেই
        # (যেমন `URLEncoder.encode(item.id, "UTF-8")`) খোঁজা ওখানেই থেমে যেত
        # আর পরের `order = ...` চোখে পড়ত না — TrashRepository-তে ঠিক সেটাই
        # হচ্ছিল, ফলে সঠিক কোডকেও ভুল বলে ধরা হত। এখন এক ধাপ ভিতরের বন্ধনীও
        # ধরা পড়ে। ⛔ নিয়ম শিথিল হয়নি — সত্যিই `order` না দিলে আগের মতোই ধরা
        #    পড়ে (নমুনা দিয়ে যাচাই করা হয়েছে)।
        for _m in re.finditer(r'fetchList\w*\(\s*"%s"\s*,((?:[^()]|\([^()]*\))*)\)' % re.escape(_t), _txt):
            if "order" not in _m.group(1):
                _bad_order.append("%s → \"%s\"" % (os.path.basename(_f), _t))
if _bad_order:
    problems.append(
        "যে টেবিলে `updatedAt` ঘর নেই তাকে নিজের order ছাড়াই পড়া হচ্ছে — "
        "অনুরোধ বাতিল হবে, তালিকা চিরকাল ফাঁকা দেখাবে: " + ", ".join(sorted(set(_bad_order)))
    )
else:
    notes.append("  `updatedAt`-হীন টেবিল (যেমন trash) নিজের order দিয়েই পড়া হয় ✅")

# 🔴 V509 — নতুন পাহারা: OUT TIME যেন **কখনো আটকে না যায়**। TK-এর নিয়ম হলো
# "আটকানো হবে না, শুধু লেখা থাকবে"। তাই অবস্থান না পাওয়া গেলেও OUT TIME বসতেই
# হবে — অর্থাৎ অপেক্ষার একটা সময়সীমা ও শেষ-ভরসার পথ থাকতেই হবে।
if 'startPlaceProbe' in wn or 'withPlaceNote' in wn:
    if 'Location not verified' not in wn:
        problems.append("OUT TIME-এ অবস্থান না পেলে কী হবে লেখা নেই — আটকে যেতে পারে")
    elif '6_000L' not in wn:
        problems.append("OUT TIME-এর অপেক্ষার সময়সীমা নেই — চিরকাল ঝুলে থাকতে পারে")
    else:
        notes.append("  OUT TIME কখনো আটকায় না — অবস্থান না পেলেও বসে যায় (সময়সীমাসহ) ✅")

print("V496 — হাজিরা · ভূমিকা · সেশন পাহারা")
print("=" * 64)
for n in notes:
    print(n)
print()
if problems:
    print("❌ FAIL — %d টি সমস্যা:" % len(problems))
    for p in problems:
        print("  ❌ " + p)
    print()
if incomplete:
    print("⚠️ INCOMPLETE — বাধ্যতামূলক কাজ এখনো বাকি:")
    for p in incomplete:
        print("  ⚠️ " + p)
    print()

# 🔴🔒 TK-এর নির্দেশ: ভুল **অথবা** অসম্পূর্ণতা — যেকোনো একটা থাকলেই PASS নয়।
if problems or incomplete:
    if problems and incomplete:
        print("ফল: FAIL / INCOMPLETE — কাজ শেষ হয়নি। ⛔ PASS নয়।")
    elif problems:
        print("ফল: FAIL — কাজ শেষ হয়নি। ⛔ PASS নয়।")
    else:
        print("ফল: INCOMPLETE — বাকি কাজ শেষ না হলে এটাকে সম্পূর্ণ বলা যাবে না। ⛔ PASS নয়।")
    sys.exit(1)

print("PASS — TK-এর নিয়মগুলো কোডে বসানো আছে ✅")
sys.exit(0)
