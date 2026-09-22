# -*- coding: utf-8 -*-
"""
YACHAI_2026-07-30_V173_V174.py  —  🔒 TK-এর জন্য যাচাই-স্ক্রিপ্ট (লক করা)

কী এটা: ৩০.০৭.২০২৬-এর সেশনে (V173 + V174) যা যা করা হয়েছে, তার প্রতিটা
সত্যিই কোডে ও নোটে আছে কিনা — যন্ত্র দিয়ে মিলিয়ে দেখে। ৫৮টা যাচাই।

কীভাবে চালাবেন:   python3 00_GUARD/YACHAI_2026-07-30_V173_V174.py

⛔ কেন রাখা হলো (TK-এর নির্দেশ, 30.07.2026):
   *"অনেক কাজ এক সেশনে বলেছি কিন্তু সেই সেশনে আপনি করেননি... সতর্কবার্তা হিসেবে
    লিখে রেখেছিলেন তারপরও আপনি সেই কাজ করেন নাই।"*
   তাই এই সেশনের কাজগুলো শুধু নোটে লেখা রইল না — **যন্ত্র দিয়ে যাচাই করা যায়**।
   ভবিষ্যতের যে কোনো সেশনে এটা চালালেই বোঝা যাবে কেউ আগের কাজ নষ্ট করেছে কি না।
   একটাও ❌ এলে বুঝতে হবে **TK-এর অনুমোদিত কোনো কাজ ভেঙে গেছে** — তখন TK-কে
   না জানিয়ে এগোনো যাবে না।
"""
import io, os, re

import os as _os
ROOT = _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))
NAT = ROOT + "/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/"
APP = ROOT + "/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/"


def read(p):
    return io.open(p, encoding="utf-8").read()


checks = []


def chk(label, ok, detail=""):
    checks.append((label, ok, detail))


# ── ১ · দুর্বল নেটে সেভ হারানো ────────────────────────────────────
sc = read(NAT + "SupabaseClient.kt")
up = sc[sc.index("fun upsert("):sc.index("/** Finds rows in a table")]
chk("১. upsert-এর catch-এ remember আছে",
    'catch (e: Exception)' in up and 'CloudWriteQueue.remember("UPSERT"' in up.split("catch (e: Exception)")[1])
ub = sc[sc.index("fun updateById("):sc.index("fun deleteById(")]
chk("১. updateById-এর catch-এ remember আছে",
    'CloudWriteQueue.remember("UPDATE"' in ub.split("catch (e: Exception)")[1])

# ── ২ · Synced দেখালেও কাজ বাকি ───────────────────────────────────
ps = read(NAT + "PendingSyncStatus.kt")
chk("২. সতর্কবাতি কেন্দ্রীয় তালিকা গোনে", "CloudWriteQueue.pendingCount(context)" in ps)
chk("২. সতর্কবাতি 'পাঠানো যায়নি' গোনে", "CloudWriteQueue.failedCount(context)" in ps)
chk("২. 'পাঠান' বোতাম কেন্দ্রীয় তালিকা পাঠায়",
    "CloudWriteQueue.flush(context)" in ps and "CloudWriteQueue.retryFailed(context)" in ps)
bw = read(NAT + "BackgroundRefreshWorker.kt")
chk("২. পিছনে পাঠানো MIN_GAP-এর আগেই চলে",
    bw.index("flushEverythingWaiting(ctx)") < bw.index("if (now - last < MIN_GAP_MS)"))
chk("২. পিছনে পাঠানো 'নতুন কিছু হয়েছে?' প্রশ্নের আগেই চলে",
    bw.index("flushEverythingWaiting(ctx)") < bw.index('val since = sp.getString(KEY_SINCE'))
chk("২. flushEverythingWaiting-এ ১১টা তালিকাই আছে",
    all(x in bw for x in ["CloudWriteQueue.flush", "EnquiryRepository(ctx)", "RegistrationRepository(ctx)",
                          "PaymentRepository(ctx)", "FollowUpRepository(ctx)",
                          "ChamberAttendanceRepository.flushPending", "ChamberCloseRepository.flushPending",
                          "ClinicalCloudRepository.flushPending", "BriefingRepository().flushPending",
                          "GenericUpdateQueue.flushPending"]))

# ── ৩ · নীরবে বাদ পড়া ─────────────────────────────────────────────
cw = read(NAT + "CloudWriteQueue.kt")
chk("৩. সীমা ১০০০", "MAX_ENTRIES = 1000" in cw)
chk("৩. মাপের সীমা ২ MB", "MAX_TOTAL_CHARS = 2_000_000" in cw and "MAX_TOTAL_CHARS" in cw.split("while (")[1])
chk("৩. 'যায়নি' ঘর আছে", 'KEY_FAILED = "failed"' in cw and "fun failedCount(" in cw)
chk("৩. ৫০ বার ব্যর্থ হলে ঘরে যায়, মোছে না", "giveUp.add(e)" in cw and 'withFailedAdded(p, giveUp, "tried_50_times")' in cw)
chk("৩. তালিকা ভরে গেলে ঘরে যায়", '"list_full"' in cw)

# ── ৪ · বড় ছবি ────────────────────────────────────────────────────
chk("৪. বড় তথ্য হলে নোট রাখে (নীরব নয়)", '"too_large"' in cw)
chk("৪. 'পাঠান' চাপলে অ-পাঠানোযোগ্য নোট পরিষ্কার হয়",
    "fun retryFailed(" in cw and "stillFailed" in cw)

# ── নিজের যাচাইয়ে পাওয়া ঝুঁকি ─────────────────────────────────────
chk("যাচাই. আইডি ছাড়া UPSERT আর পাঠানো হয় না",
    'if (kind == "UPSERT" && id.isBlank()) return' in cw)

# ── ৫ · ডিলিট ফিরে আসা ────────────────────────────────────────────
dg = read(NAT + "DeletedGuard.kt")
chk("৫. ডিলিট-তালিকা ১ ঘণ্টায় নামে (আগে ৬)", "SYNC_GAP_MS = 1L * 60L * 60L * 1000L" in dg)

# ── ৬ · প্রতি পর্দায় অনেক retry ───────────────────────────────────
bn = read(NAT + "BottomNav.kt")
# 🔒 খাতার সারি B169 (30.07.2026 দুপুর ২.৪০) — TK-কে জানিয়ে বদলানো হলো।
# আগে তালাটা ছিল শুধু `BottomNav.kt`-এর ভিতরে, তাই এখানে ওই ফাইলটাই দেখা হত।
# TK-এর ৬ নম্বর সন্দেহের কাজে তালাটা `SyncGate.kt`-এ সরানো হয়েছে, যাতে চারটে
# জায়গা (পর্দা-খোলা · পিছনের কাজ · WorkManager · "পাঠান" বোতাম) একই দরজা
# ব্যবহার করে। ⛔ **সুরক্ষা কমেনি, বেড়েছে** — তাই যাচাইটা এখন নতুন ঘরটাই দেখে,
# আর সঙ্গে BottomNav সত্যিই ওই দরজা ব্যবহার করছে কি না সেটাও দেখে।
sg = read(NAT + "SyncGate.kt")
chk("৬. তালা আছে", "AtomicBoolean(false)" in sg and "compareAndSet(false, true)" in sg
    and "SyncGate.tryRun" in bn)
chk("৬. ২ মিনিটের বিরতি আছে", "QUIET_GAP_MS = 2L * 60L * 1000L" in bn)
chk("৬. কিছু আটকে থাকলে বিরতি মানা হয় না", "!anythingWaiting(activity)" in bn)
chk("৬. তালা finally-তে খোলে", "busy.set(false)" in sg and sg.count("finally") >= 2)
chk("৬. মেইন থ্রেডে ফাইল পড়া হয় না",
    bn.index("Thread {") < bn.index("anythingWaiting(activity)\n"))

# ── ৭ · অন্য ফোনের তথ্য দেরিতে ─────────────────────────────────────
fc = sc[sc.index("fun fetchCount("):sc.index("fun updateById(")]
chk("৭. fetchCount ব্যর্থ হলে -1 দেয়", fc.count("-1") >= 3 and "return 0" not in fc)
pr = read(NAT + "PaymentRepository.kt")
chk("৭. ঘন্টার দুই সংখ্যা -1 সামলায়", pr.count("coerceAtLeast(0)") >= 2)
chk("৭. worker -1 কে 'জানি না' ধরে", "if (a < 0 || b < 0) -1 else a + b" in bw)

# ── B150 · B151 · নিজে থেকে নতুন হওয়া ────────────────────────────
lr = read(NAT + "LiveRefresh.kt")
chk("B151. LiveRefresh ফাইল আছে ও ৩০ সেকেন্ড", "TICK_MS = 30_000L" in lr)
chk("B151. ঘুম রাত ১০টা – সকাল ৬টা", "WAKE_HOUR = 6" in lr and "SLEEP_HOUR = 22" in lr)
chk("B151. সস্তা প্রশ্ন updatedAt ধরে (নতুন+বদল দুটোই ধরা পড়ে)", 'updatedAt=gt.' in lr)
chk("B151. ব্রাঞ্চের নাম encode করা", "URLEncoder.encode" in lr)
# 🔒 খাতার সারি B171 (30.07.2026, TK-এর ৮ নম্বর সন্দেহ, "ঝুঁকি ন্যূনতম হলে করুন"):
# Chamber ও Follow-up আসলে একাধিক টেবিলের যোগফল দেখায় (কোড মিলিয়ে দেখা হয়েছে),
# তাই `LiveRefresh.Watch(...)` এখন একাধিক টেবিল নেয়। DoctorQueue অপরিবর্তিত
# (ডেটা শুধু patients থেকেই)। ⛔ সুরক্ষা কমেনি, বেড়েছে — যাচাইটা প্রতিটা
# টেবিল আলাদা করে খুঁজে দেখে, তাই কোনোটা বাদ পড়লে ধরা পড়বে।
for f, tables in (("ChamberAttendanceActivity", ("payments", "patients", "enquiries", "followups")),
                 ("FollowUpActivity", ("followups", "enquiries", "patients", "payments")),
                 ("DoctorQueueActivity", ("patients",))):
    s = read(NAT + f + ".kt")
    watch_call = s.split("LiveRefresh.Watch(")[1].split(")")[0] if "LiveRefresh.Watch(" in s else ""
    watch_ok = all(('"%s"' % t) in watch_call for t in tables)
    chk("B151. %s — LiveRefresh ব্যবহার (%s)" % (f, "+".join(tables)),
        watch_ok and "LiveRefresh.TICK_MS" in s)
    chk("B151. %s — রাতে বন্ধ" % f, "LiveRefresh.awake()" in s)
    chk("B151. %s — পপ-আপ খোলা থাকলে বন্ধ" % f,
        "onWindowFocusChanged" in s and "autoScreenFocused" in s)
    chk("B151. %s — পর্দা সামনে না থাকলে বন্ধ" % f, "removeCallbacks(autoTick" in s or "removeCallbacks(autoRefreshRunnable" in s)
chk("B171. পিছিয়ে যাওয়ার মাপ ছোট (একই বদল বারবার ধরবে না)", "SAFETY_BACK_MS = 5_000L" in lr)
chk("B151. পিছনের কাজও রাতে বন্ধ, কিন্তু পাঠানো চালু",
    "if (!LiveRefresh.awake()) return Result.success()" in bw and
    bw.index("flushEverythingWaiting(ctx)") < bw.index("if (!LiveRefresh.awake())"))
fu = read(NAT + "FollowUpActivity.kt")
chk("B151. Follow-up-এর ৩ মিনিটের পুরো-তালিকা নামানো বন্ধ",
    "AUTO_REFRESH_MS" not in fu and "180_000L" not in fu)

# ── ভার্সন এক জায়গায় ─────────────────────────────────────────────
g = read(APP + "build.gradle.kts")
da = read(NAT + "DashboardActivity.kt")
# ⛔ ভার্সনের সংখ্যা প্রতি সেশনে বাড়ে — তাই এখানে নির্দিষ্ট সংখ্যা নয়,
#    gradle · পর্দা · নোট তিন জায়গায় **একই** সংখ্যা কিনা সেটাই দেখা হয়
#    (30.07.2026 সকাল ৯.১০-এ V175-এ যাওয়ার সময় বদলানো হলো, খাতার সারি B156)।
import re as _re
_vc = _re.search(r"versionCode = (\d+)", g)
# ⛔ সংশোধন (30.07.2026 সন্ধ্যা, V200-এ ভার্সন ১৯৯ পেরিয়ে ৩-অঙ্কে যাওয়ার সময়
#    ধরা পড়া): আগের regex শুধু "1.XX" (major সবসময় 1) ধরত, তাই V200-এ
#    versionName="2.00" (major.minor = versionCode÷100 . versionCode%100 —
#    এই একই সাধারণ নিয়ম যা V100-V199-এও versionName="1.00"–"1.99" দিয়ে
#    হুবহু মিলত) হলে মিথ্যা ব্যর্থতা দেখাত। এখন যেকোনো "major.minor" ধরে,
#    আর মিলিয়ে দেখে ঠিক সেই একই সাধারণ সূত্র দিয়ে।
_vn = _re.search(r'versionName = "(\d+)\.(\d+)"', g)
_V = _vc.group(1) if _vc else ""
_vn_ok = False
if _vc and _vn:
    _code = int(_vc.group(1))
    _expected = "%d.%02d" % (_code // 100, _code % 100)
    _actual = "%s.%s" % (_vn.group(1), _vn.group(2))
    _vn_ok = (_actual == _expected)
chk("ভার্সন. gradle-এ versionCode ও versionName মেলে", bool(_vc) and bool(_vn) and _vn_ok)
chk("ভার্সন. পর্দায় একই V%s (পুরনো নেই)" % _V, da.count("· V" + _V) == 2 and _re.search(r"V1[0-7]\d", da.replace("V"+_V, "")) is None)

# ── ওয়েব পাসওয়ার্ড ───────────────────────────────────────────────
js = read(ROOT + "/03_NETLIFY_READY/app.js")
chk("ওয়েব. কাস্টম থাকলে ডিফল্ট বাতিল",
    "let custom=String(cloudPw||(savedRow&&savedRow.password)||'').trim();" in js and
    "if(p!==custom)return toast('Wrong password');" in js)

# ── নোট ───────────────────────────────────────────────────────────
kh = read(ROOT + "/00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md")
for row in ("B145", "B146", "B147", "B148", "B149", "B150", "B151"):
    chk("নোট. খাতায় সারি %s আছে" % row, ("| %s |" % row) in kh)
chk("নোট. খাতার উপরের লাইনে V%s" % _V, ("**V" + _V + "**") in kh[:6000])
log = read(ROOT + "/00_TK_KAJER_TARIKH_SOMOY_LOG.md")
chk("নোট. লগে আজকের ৩টে সারি (তারিখ-সময় সহ)", log.count("## 📅 30.07.2026") >= 3)
chk("নোট. লক নোট V173 আছে", os.path.exists(ROOT + "/00_LOCK_NOTE_SESSION_2026-07-30_V173.md"))
chk("নোট. লক নোট V174 আছে", os.path.exists(ROOT + "/00_LOCK_NOTE_SESSION_2026-07-30_V174.md"))
mn = read(ROOT + "/00_PROJECT_STATE_MASTER_NOTE.md")
chk("নোট. মাস্টার নোটে V%s সবার উপরে" % _V, mn.lstrip().startswith("# 📌 সর্বশেষ অবস্থা — **V" + _V + "**"))

bad = [c for c in checks if not c[1]]
for label, ok, _ in checks:
    print(("  ✅ " if ok else "  ❌ ") + label)
print("=" * 60)
print("মোট %d টি যাচাই · পাশ %d · ব্যর্থ %d" % (len(checks), len(checks) - len(bad), len(bad)))
