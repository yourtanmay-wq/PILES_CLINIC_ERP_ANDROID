#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🟢 V590 (২৩.০৮.২০২৬) — নতুন পাহারাদার।

TK-এর রিপোর্ট ও নির্দেশ (সবগুলোই ছবিসহ):
  ১. *"ট্রিটমেন্ট প্রোগ্রেসে যা লেখা হয় রিপোর্ট কার্ডে অটোমেটিক উঠে না কেন?"*
  ২. *"এতগুলো ওভারডিউ রয়েছে · স্টাফদের কাছে কি নোটিফিকেশন যায় না?"*
  ৩. *"View-তে চাপার পর আগে যেখানে ছিল সেখানকার মতনই চেহারা দেখতে হতে হবে।"*
  ৪. *"App থেকে অনেকগুলো কল আমার সামনেই করলো, কিন্তু এখন সব 0 কেন দেখাচ্ছে?"*
  ৫. *"কিশানগঞ্জের যেকোনো স্টাফের কাছে যেন বাংলা পর্দা কোথাও না থাকে —
       বাংলার পরিবর্তে হয় হিন্দি অথবা ইংলিশ।"*

⛔ এটা কিছু বদলায় না — শুধু পড়ে ও মিলিয়ে দেখে।
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NAT = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                   "java", "com", "tkbiswas", "pilesclinic", "native")
CLI = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                   "java", "com", "tkbiswas", "pilesclinic", "clinical")
MOD = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                   "java", "com", "tkbiswas", "pilesclinic", "modules")
APPJS = os.path.join(ROOT, "03_NETLIFY_READY", "app.js")
CSS = os.path.join(ROOT, "03_NETLIFY_READY", "styles.css")

fails, oks = [], []


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def check(name, cond):
    (oks if cond else fails).append(name)


chamber = read(os.path.join(NAT, "ChamberAttendanceActivity.kt"))
worker = read(os.path.join(NAT, "CallReminderWorker.kt"))
dash = read(os.path.join(NAT, "DashboardActivity.kt"))
follow = read(os.path.join(NAT, "FollowUpActivity.kt"))
trashact = read(os.path.join(NAT, "TrashBinActivity.kt"))
srccard = read(os.path.join(NAT, "TrashSourceCard.kt"))
nobn = read(os.path.join(NAT, "NoBengali.kt"))
etw = read(os.path.join(NAT, "ExpectedTomorrowReminderWorker.kt"))
wn = read(os.path.join(MOD, "WorkNotebookActivity.kt"))
chk = read(os.path.join(CLI, "DoctorCheckupActivity.kt"))
js = read(APPJS)
css = read(CSS)

# ───────── ১ · Treatment Progress → Report Card ─────────
check("১.১ পাঠানোর কাজটা একটাই জায়গায় (ফোন)",
      "private fun syncProgressToReportCard(row: ChamberAttendanceRow, text: String, dateKey: String)" in chamber)
check("১.২ চারটে বাক্সের তিনটেই এখন ডাকে (ফোন)",
      chamber.count("syncProgressToReportCard(") == 4)   # ঘোষণা + তিনটে ডাক
check("১.৩ তারিখ = বোর্ডে খোলা দিন, 'আজ' নয় (ফোন)",
      "val dayKey = dateKey.ifBlank { FollowUpModel.today() }" in chamber
      and "date=eq.$dayKey" in chamber)
check("১.৪ লেখাটা `progress` ঘরেই (স্টাফের remarks ছোঁয়া হয় না)",
      'JSONObject().put("progress", text)' in chamber)
check("১.৫ ওয়েবের বাক্সও এখন পাঠায়",
      "wlv1SyncProgressToReportCard(m, rowId, txt);" in js
      and "function wlv1SyncProgressToReportCard(m, rowId, txt){" in js)
check("১.৬ ওয়েবে দিন = বোর্ডের দিন, আর bill_edit বাদ",
      "const day = String(wlv1ChamberDate||today()).slice(0,10);" in js
      and "if(t==='bill_edit'||t==='chamber_expected') return false;" in js)
check("১.৭ ওয়েবে ঘোষিত আলাদা রোগীর সারি আলাদাই থাকে",
      "return own ? (owner===own) : !wlv1IsDeclaredSeparateRowId(owner, m);" in js)

# ───────── ২ · ওভারডিউ কল ─────────
check("২.১ নোটিফিকেশনে আজ + বকেয়া (ফোন)",
      "return all.filter { it.nextFollow.isNotBlank() && it.nextFollow <= today }" in worker)
check("২.২ বকেয়া আগে, সবচেয়ে পুরনো আগে (ফোন)",
      ".sortedBy { it.nextFollow }" in worker)
check("২.৩ কতগুলো বকেয়া, নোটিফিকেশনে লেখা থাকে (ফোন)",
      'val overdue = due.count { it.nextFollow.isNotBlank() && it.nextFollow < FollowUpModel.today() }' in worker
      and "overdue" in worker.split("setContentTitle")[1])
check("২.৪ ড্যাশবোর্ডের ব্যানারেও আজ + বকেয়া (ফোন)",
      "fun isDue(f: FollowUpItem): Boolean = f.nextFollow.isNotBlank() && f.nextFollow <= today" in dash
      and ".count { isDue(it) }" in dash)
# 📞🔒 V1115 (০৫.০৯.২০২৬) — **TK নিজে এই নিয়মটা তুলে দিয়েছেন:**
#   *"51 Calls Pending, 33 over Due তার পাশের ট্যাপ টু কল — এটা রাখতে হবে না"*
#   *"Call icon 2 বার কেন? এখানে Over due-ও লেখা থাকবে না"* (ফটো-প্রুফ পাশ)
# ⇒ পট্টিতে এখন শুধু "N calls pending"। তাই পুরনো "বকেয়ার সংখ্যা দেখাতেই হবে"
#   যাচাইটা আর চলে না — বদলে **গোনাটা যেন হারিয়ে না যায়** সেটাই পাহারা দেওয়া
#   হয় (V590-এর আসল কাজ: আজ + বকেয়া দুটোই গোনা)।
check("২.৫ ব্যানারে আজ + বকেয়া দুটোই গোনা হয় (ফোন) · লেখায় শুধু মোট (TK, ০৫.০৯.২০২৬)",
      '"$count calls pending"' in dash and "overdueFrom(" in dash)
check("২.৬ ব্যানার থেকে খুললে তালিকাতেও আজ + বকেয়া (ফোন)",
      '"Today" -> if (bannerCallsOnly) items.filter { it.nextFollow.isNotBlank() && it.nextFollow <= today }' in follow)
check("২.৭ নিজে 'Today' চাপলে আগের মতোই শুধু আজকের (ফোন)",
      "else items.filter { it.nextFollow == today || it.recordDate == today }" in follow)
check("২.৮ ওয়েবেও আজ + বকেয়া, একই নিয়ম",
      "return d!=='' && d<=t;" in js
      and "if(f.callsOnly) return rows.filter(x=>{const d=String(x.nextFollow||'').slice(0,10); return d!=='' && d<=t;});" in js)
# 📞 V1115 — উপরের একই কারণে ওয়েবেও লেখাটা ছোট হলো; গোনাটা অটুট।
check("২.৯ ওয়েবের ব্যানারেও আজ + বকেয়া গোনা হয় (TK, ০৫.০৯.২০২৬)",
      "calls pending`" in js and "overdue" in js)

# ───────── ৩ · Trash Bin-এর View ─────────
check("৩.১ আসল Adapter ডেকে কার্ড বানানো হয় (ফোন)",
      "CollectionAdapter(ctx, listOf(row))" in srccard
      and "FollowUpAdapter(ctx, listOf(fu), {}, {}, {}, {})" in srccard)
check("৩.২ সারির JSON পড়া হয় প্রমাণিত ফাংশনে (ফোন)",
      "PaymentModel.parsePaymentRow(item.record)" in srccard
      and "FollowUpModel.parse(item.record)" in srccard)
check("৩.৩ কার্ডের কোনো বোতাম কাজ করে না (ফোন)",
      "private fun disableTouch(v: View)" in srccard
      and "v.setOnTouchListener { _, _ -> true }" in srccard)
check("৩.৪ View-তে কার্ডটা বসে, না পারলে পুরোনো তালিকা (ফোন)",
      "val card = TrashSourceCard.build(this, item)" in trashact
      and "if (card == null) {" in trashact)
check("৩.৫ কে মুছেছেন · কখন — আলাদা লাইনে (ফোন)",
      '"🗑  Deleted by $line"' in trashact)
check("৩.৬ ওয়েবেও একই কার্ড, নতুন করে আঁকা নয়",
      "function wlv1TrashSourceCardHtml(x){" in js
      and "inner=wlv1PayCardHtml(" in js and "inner=fuCard(" in js)
check("৩.৭ Collection পর্দার কার্ড এখন একটাই জায়গা থেকে (ওয়েব)",
      "function wlv1PayCardHtml(x){" in js and "rows.map(wlv1PayCardHtml)" in js)
check("৩.৮ ওয়েবেও কার্ডের ভিতরে চাপ কাজ করে না",
      ".wlv1TrashSrc *{pointer-events:none !important" in css)

# ───────── ৪ · App Calls ০ হয়ে যাওয়া ─────────
check("৪.১ call_taps এখন যাচাই-সহ পড়া হয়",
      'ModuleAuth.getRowsChecked("wn", "call_taps"' in wn
      and 'val appCalls = appR.rows.length()' in wn)
check("৪.২ পড়া সফল হলো কিনা, সেটা সাথে যায়", '.put("appOk", appR.ok)' in wn)
# 🔧 V593 — শর্তটা `callsOk(s)` নামের ছোট ফাংশনে সরানো হয়েছে (মানে হুবহু
#    এক: `s.optBoolean("appOk", true)`)। তাই দুটো লেখার যেকোনোটাই চলবে —
#    পাহারাদার এখন **আচরণ** দেখে, লেখার ধরন নয়।
check("৪.৩ ব্যর্থ হলে ফোনের গোনা মুছে যায় না",
      ('appCallsNow = if (s.optBoolean("appOk", true))' in wn
       or 'appCallsNow = if (callsOk(s))' in wn)
      and "maxOf(cloudCalls, phoneCalls) else maxOf(phoneCalls, appCallsNow)" in wn)
check("৪.৪ ক্লাউড পিছিয়ে থাকলেও গোনা কমে না",
      "val phoneCalls = try {" in wn and "ModuleAuth.localCallTapCount(" in wn)
check("৪.৫ পুরোনো শর্তহীন লাইনটা আর নেই",
      'appVal.text = s.optInt("appCalls").toString()' not in wn)

# ───────── ৫ · কিশানগঞ্জে বাংলা নেই ─────────
def strip_kt(src):
    out = []
    i, n = 0, len(src)
    in_s = in_ts = False
    while i < n:
        c = src[i]
        if not in_s and not in_ts:
            if src.startswith('"""', i):
                in_ts = True; out.append('"""'); i += 3; continue
            if c == '"':
                in_s = True; out.append(c); i += 1; continue
            if src.startswith('//', i):
                j = src.find('\n', i); i = n if j < 0 else j; continue
            if src.startswith('/*', i):
                j = src.find('*/', i + 2); i = n if j < 0 else j + 2; continue
            out.append(c); i += 1; continue
        if in_ts:
            if src.startswith('"""', i):
                in_ts = False; out.append('"""'); i += 3; continue
            out.append(c); i += 1; continue
        if c == '\\':
            out.append(src[i:i + 2]); i += 2; continue
        if c == '"':
            in_s = False
        out.append(c); i += 1
    return ''.join(out)


BN = re.compile(r'[ঀ-৿]')
raw_toasts = []
for f, code in (("DoctorCheckupActivity.kt", strip_kt(chk)),):
    for m in re.finditer(r'Toast\.makeText\((?:[^()]|\([^()]*\))*\)', code):
        t = m.group(0)
        if BN.search(t) and 'NoBengali.s(' not in t:
            raw_toasts.append((f, t[:60]))
check("৫.১ চেক-আপ পর্দার কোনো Toast-এ আর কাঁচা বাংলা নেই", not raw_toasts)
raw_notif = []
for f, code in (("ExpectedTomorrowReminderWorker.kt", strip_kt(etw)),
                ("CallReminderWorker.kt", strip_kt(worker))):
    for m in re.finditer(r'\.setContent(?:Title|Text)\((?:[^()]|\([^()]*\))*\)', code):
        t = m.group(0)
        if BN.search(t) and 'NoBengali.s(' not in t:
            raw_notif.append((f, t[:60]))
check("৫.২ নোটিফিকেশনেও কাঁচা বাংলা নেই", not raw_notif)
check("৫.৩ ওয়ার্কারে কে লগইন আছে দেখে নেওয়া হয়",
      "NoBengali.refresh(applicationContext)" in etw)
# ওই ১১টা লেখার হিন্দি সত্যিই যোগ হয়েছে কিনা
need = ["ছবি যোগ হলো", "মোছার মত কিছু নেই", "তালিকা থেকে সরানো হলো",
        "ইনজেকশনের ধাপ থাকবে", "ইনজেকশনের ধাপ বাদ",
        "আগে উপরের সারি থেকে একটা ছবি বাছুন", "কাল আসার কথা"]
hindi_part = nobn.split("private val HINDI: Map<String, String> = mapOf(")[1]
check("৫.৪ প্রতিটার হিন্দি অনুবাদ যোগ হয়েছে",
      all(('"%s" to ' % t) in hindi_part for t in need))

# ───────── ৬ · "আবার পাঠান" বোতামের উজ্জ্বলতা ─────────
mui = read(os.path.join(MOD, "ModuleUi.kt"))
check("৬.১ দ্বিতীয় সারির (ফিকে) বোতাম আছে",
      "fun buttonSoft(ctx: Context, text: String, onClick: () -> Unit): Button" in mui)
check("৬.২ IN TIME আবার পাঠানোর বোতামটা ফিকে",
      'ModuleUi.buttonSoft(this, NoBengali.s("📤 IN TIME আবার WhatsApp-এ পাঠান"))' in wn)
check("৬.৩ OUT TIME বোতাম আগের মতোই উজ্জ্বল",
      'ModuleUi.button(this, "OUT TIME")' in wn or 'ModuleUi.button(this, NoBengali.s("OUT TIME"))' in wn
      or 'ModuleUi.button(this, "OUT TIME"' in wn)

print("🛡️ V590 পাহারাদার — Report Card · ওভারডিউ · Trash View · App Calls · বাংলা")
print("=" * 70)
for o in oks:
    print("   ✅ " + o)
for f in fails:
    print("   ❌ " + f)
if raw_toasts:
    for f, t in raw_toasts[:5]:
        print("        ↳ " + f + " : " + t)
if raw_notif:
    for f, t in raw_notif[:5]:
        print("        ↳ " + f + " : " + t)
print("-" * 70)
print("   পাশ: %d / %d" % (len(oks), len(oks) + len(fails)))
if fails:
    print("\nFAIL — উপরের ❌ ঘরগুলো ঠিক করতে হবে")
    sys.exit(1)
print("\nPASS — TK-এর পাঁচটা রিপোর্টই দুই অ্যাপে ঠিক করা আছে ✅")
