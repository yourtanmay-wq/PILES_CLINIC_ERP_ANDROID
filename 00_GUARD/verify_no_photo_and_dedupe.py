#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🔵 V493 (২০.০৮.২০২৬) — নতুন পাহারাদার (TK-নির্দেশ ৪ · ৫ · ৬)।

তিনটে জিনিস পাহারা দেয়, যাতে ভবিষ্যতে কেউ ভুল করে আবার Egress বাড়িয়ে না ফেলে:

  ৪) তালিকা ও ব্যাকগ্রাউন্ডের পড়ায় রোগীর `photo` যেন না নামে।
  ৫) প্রতিটা পড়া যেন `CloudReadDedupe`-এর ভিতর দিয়ে যায় (একই অনুরোধ দুবার নয়)।
  ৬) সরু পড়া ব্যর্থ হলে যেন সঙ্গে সঙ্গে `select=*` (ছবি-সহ) না চাওয়া হয়।

⛔ এটা কিছু বদলায় না — শুধু খুঁজে দেখে ও বলে।
⚠️ পাহারাদার Kotlin কম্পাইল করে না; Android Studio-র বিল্ডই চূড়ান্ত প্রমাণ।
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                  "java", "com", "tkbiswas", "pilesclinic")
WEB = os.path.join(ROOT, "03_NETLIFY_READY", "app.js")

problems = []
notes = []


def read(path):
    try:
        return open(path, encoding="utf-8").read()
    except Exception:
        return ""


# ── ৪ · তালিকার ঘর-তালিকায় photo থাকা চলবে না ────────────────────────────
LIST_CONSTS = [
    "PATIENT_COLS_NO_PHOTO", "FOLLOWUP_COLS_NO_PHOTO", "ENQUIRY_COLS_DRAFT",
    "FOLLOWUP_COLS_CHAMBER_BOARD", "PAYMENT_COLS_LIST",
]
sup = read(os.path.join(KT, "native", "SupabaseClient.kt"))
for name in LIST_CONSTS:
    m = re.search(r'const val %s\s*=\s*"([^"]*)"' % name, sup)
    if not m:
        continue
    cols = [c.strip() for c in m.group(1).split(",")]
    for bad in ("photo", "photos"):
        if bad in cols:
            problems.append("তালিকার ঘর-তালিকা `%s`-এ `%s` আছে — তালিকায় ছবি নামবে" % (name, bad))
    notes.append("  %-30s %d টি ঘর, ছবি নেই ✅" % (name, len(cols)))

# ── ৬ · সরু পড়া ব্যর্থ হলে select=* নয় ───────────────────────────────────
if "SafeWideColumns.forTable(table, cols)" not in sup:
    problems.append("SupabaseClient-এর সরু-পড়া ব্যর্থতার পথে SafeWideColumns বসানো নেই")
else:
    n = sup.count("SafeWideColumns.forTable(table, cols)")
    notes.append("  সরু-পড়া ব্যর্থতার পথ SafeWideColumns দিয়ে ঢাকা: %d টি ✅" % n)

safe = read(os.path.join(KT, "native", "SafeWideColumns.kt"))
if not safe:
    problems.append("SafeWideColumns.kt ফাইলটাই নেই")
else:
    for t, heavy in (("patients", "photo"), ("followups", "photo"), ("medical", "photos")):
        if '"%s" to listOf(' % t not in safe or heavy not in safe:
            problems.append("SafeWideColumns-এ `%s`-এর ভারী ঘর `%s` তালিকাভুক্ত নয়" % (t, heavy))

# ── ৫ · প্রতিটা পড়া CloudReadDedupe-এর ভিতর দিয়ে ─────────────────────────
if "CloudReadDedupe.body(url)" not in sup:
    problems.append("fetchListOrNull এখনো CloudReadDedupe-এর ভিতর দিয়ে যাচ্ছে না")
else:
    notes.append("  fetchListOrNull → CloudReadDedupe ✅")

clears = sup.count("CloudReadDedupe.clear()")
cache_clears = sup.count("CloudReadCache.clear()")
if clears < cache_clears:
    problems.append("লেখার পরে ভুলে যাওয়া অসম্পূর্ণ — CloudReadCache %d বার, CloudReadDedupe %d বার"
                    % (cache_clears, clears))
else:
    notes.append("  প্রতিটা লেখার পরে দুটো cache-ই খালি হয়: %d জায়গা ✅" % clears)

ded = read(os.path.join(KT, "native", "CloudReadDedupe.kt"))
if ded and "if (fresh != null) put(key, fresh)" not in ded:
    problems.append("CloudReadDedupe ব্যর্থ পড়াও জমিয়ে রাখছে — খাতার সারি B446-এর ঝুঁকি")

# ── ওয়েবেও একই নিয়ম ──────────────────────────────────────────────────────
web = read(WEB)
if web:
    if re.search(r"sb\.from\(table\)\.select\('\*'\)\.gte\('updatedAt'", web):
        problems.append("ওয়েবের ৪৫-সেকেন্ডের pull এখনো select('*') — ছবি নামবে")
    else:
        notes.append("  ওয়েবের ৪৫-সেকেন্ডের pull ছবি ছাড়া ✅")
    if "RT_NO_PHOTO_COLS" not in web:
        problems.append("ওয়েবে RT_NO_PHOTO_COLS পাওয়া যায়নি")


# ── V494 · ১ · মেমরি লিক ফিরে আসেনি ──────────────────────────────────────
if ded:
    if "releaseGate(key, g)" not in ded or "finally {" not in ded:
        problems.append("CloudReadDedupe-এ finally দিয়ে চাবি ছাড়ার ব্যবস্থা নেই — মেমরি লিক")
    elif "inFlight.remove(key)" not in ded:
        problems.append("CloudReadDedupe চলমান-অনুরোধের চাবি কখনো মুছছে না — মেমরি লিক")
    else:
        notes.append("  চলমান-অনুরোধের চাবি finally-তে মুছে যায় ✅")
    if "debugInFlightSize" not in ded:
        problems.append("debugInFlightSize() নেই — পরীক্ষা করা যাবে না")

# ── V494 · ২ · লগইন/লগআউট নিরাপত্তা ──────────────────────────────────────
if ded and "sessionTag + \"|\" + rawKey" not in ded:
    problems.append("Dedupe চাবিতে ব্যবহারকারীর পরিচয় যুক্ত নেই — লগআউটের পরে ফাঁস হতে পারে")
sess = read(os.path.join(KT, "native", "NativeSession.kt"))
for what, needle in (("লগইন/বদল", "setSession(user.mobile)"),
                     ("লগআউট", "setSession(null)"),
                     ("প্রতি পর্দায় যাচাই", "setSession(mobile)")):
    if needle not in sess:
        problems.append("NativeSession-এ %s পথে CloudReadDedupe.setSession বসানো নেই" % what)
if all(x in sess for x in ("setSession(user.mobile)", "setSession(null)", "setSession(mobile)")):
    notes.append("  লগইন · লগআউট · প্রতি পর্দা — তিন পথেই পরিচয় বসে ✅")
auth = read(os.path.join(KT, "modules", "ModuleAuth.kt"))
if "CloudReadDedupe.clear()" not in auth:
    problems.append("ModuleAuth.signOut()-এ CloudReadDedupe.clear() নেই")

# ── V494 · ৩ · SafeWide ব্যর্থ হলেও select=* পথ আছে ──────────────────────
if "if (safeRead != null) return safeRead" not in sup:
    problems.append("SafeWide পড়া ব্যর্থ হলে select=* পথে যাচ্ছে না — B446-এর ঝুঁকি")
else:
    notes.append("  SafeWide ব্যর্থ হলেও পুরনো select=* পথ আছে (B446) ✅")
if safe and "w in knownSet" not in safe:
    problems.append("SafeWideColumns এখনো অচেনা ঘর ফেরত আনছে — ভুল ঘর থাকলে ধাপটা নষ্ট হবে")

# ── V494 · ALL তালিকা লাইভ-যাচাই ধ্রুবক ঢাকে কিনা ────────────────────────
import re as _re
def _cols(txt, name):
    m = _re.search(r'(?:const val|val)\s+%s\s*=\s*"([^"]*)"' % name, txt)
    return set(c.strip() for c in m.group(1).split(",")) if m else set()
def _all(t):
    m = _re.search(r'"%s" to "([^"]*)"' % t, safe)
    return set(c.strip() for c in m.group(1).split(",")) if m else set()
fu = read(os.path.join(KT, "native", "FollowUpRepository.kt"))
for table, names in (("patients", ("PATIENT_COLS_NO_PHOTO",)),
                     ("followups", ("FOLLOWUP_COLS_NO_PHOTO", "FOLLOWUP_COLS_CHAMBER_BOARD")),
                     ("payments", ("PAYMENT_COLS_LIST",))):
    proven = set()
    for nm in names:
        proven |= _cols(sup, nm)
    if table == "patients":
        proven |= _cols(fu, "PATIENT_COLS")
    miss = sorted(proven - _all(table))
    if miss:
        problems.append("SafeWideColumns.ALL['%s']-এ লাইভ-যাচাই করা ঘর নেই: %s" % (table, miss))
    else:
        notes.append("  ALL['%s'] লাইভ-যাচাই ধ্রুবকের সব ঘর ঢাকে ✅" % table)

# ── V494 · ৪ · ওয়েবে অর্থহীন ডুপ্লিকেট retry নেই ─────────────────────────
if web:
    if "wlv1IsColumnError" not in web:
        problems.append("ওয়েবে wlv1IsColumnError() নেই — ব্যর্থতার কারণ আলাদা করা হচ্ছে না")
    else:
        notes.append("  ওয়েবে ব্যর্থতার কারণ (ঘর-ভুল বনাম নেট) আলাদা করা হয় ✅")
    if "select(__ph).in('id',chunk)" in web or "select(__ph).limit(2000)" in web:
        problems.append("ওয়েবে এখনো হুবহু একই query দ্বিতীয়বার পাঠানো হচ্ছে")


# ── V495 · ওয়েবেও ডুপ্লিকেট ঠেকানোর ব্যবস্থা ────────────────────────────
if web:
    if "__wlv1DedupeInstalled" not in web:
        problems.append("ওয়েবে ডুপ্লিকেট ঠেকানোর ব্যবস্থা (fetch dedupe) বসানো নেই")
    else:
        notes.append("  ওয়েবে fetch-স্তরে ডুপ্লিকেট ঠেকানো আছে ✅")
        if "if(method !== 'GET'){ clearAll();" not in web:
            problems.append("ওয়েবের dedupe লেখাকেও ছুঁচ্ছে — শুধু GET হওয়ার কথা")
        if "url.indexOf(REST) !== 0" not in web:
            problems.append("ওয়েবের dedupe শুধু Supabase REST-এ সীমাবদ্ধ নয়")
        if "if(!res || !res.ok) return null" not in web:
            problems.append("ওয়েবের dedupe ব্যর্থ উত্তরও জমাচ্ছে")
        if "wlv1DedupeClear()" not in web:
            problems.append("Sync Now-তে জমানো মোছার ব্যবস্থা নেই")
        else:
            notes.append("  Sync Now চাপলে জমানো মুছে টাটকা আনে ✅")

print("V495 — ছবি · ডুপ্লিকেট (অ্যাপ+ওয়েব) · মেমরি লিক · সেশন পাহারা")
print("=" * 62)
for n in notes:
    print(n)
print()
if problems:
    print("FAIL — %d টি সমস্যা:" % len(problems))
    for p in problems:
        print("  ❌ " + p)
    sys.exit(1)
print("PASS — তালিকায় ছবি নামে না · একই অনুরোধ দুবার যায় না ✅")
sys.exit(0)
