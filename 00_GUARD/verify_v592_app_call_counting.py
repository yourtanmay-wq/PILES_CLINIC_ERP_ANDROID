# -*- coding: utf-8 -*-
"""
🛡️ V592 পাহারাদার (২৩.০৮.২০২৬) — **App Calls গোনা**।

TK: *"App এর কল গুলি সঠিক ভাবে কাউন্টিং কেন হচ্ছে না · একই সমস্যার কথা এর আগে
     অনেকবার আপনাকে বলা হয়েছিল · গভীরে গিয়ে সত্যতা যাচাই করুন"*

**প্রমাণিত মূল কারণ (কোড চালিয়ে দেখা):**
  · লেখার দিক (`ModuleAuth.logCallTap`) — `staff_code = expectedCode()`
  · পড়ার দিক (`WorkNotebookActivity`) — `staff_code = user.name`
  দুটো আলাদা ⇒ মাস্টার · ৭ জন ডাক্তার · ফিল্ড অফিসারের গোনা **সবসময় 0**।
  একই ভুল ফোনে-জমা গোনাতেও ছিল, তাই নেট থাকলেও 0-ই থাকত।
  উপরন্তু ফোন থেকে `call_date` পাঠানোই হত না, অথচ গোনার ফিল্টার ওটাই।

এই পাহারাদার কোড শুধু পড়ে না — StaffDirectory-র **সব অ্যাকাউন্ট নিয়ে
দুটো নিয়ম চালিয়ে** মিলিয়ে দেখে।
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
K = ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/'
ma = io.open(K + 'modules/ModuleAuth.kt', encoding='utf-8').read()
wn = io.open(K + 'modules/WorkNotebookActivity.kt', encoding='utf-8').read()
sd = io.open(K + 'native/StaffDirectory.kt', encoding='utf-8').read()
mc = io.open(ROOT + '/03_NETLIFY_READY/module_core.js', encoding='utf-8').read()
nb = io.open(ROOT + '/03_NETLIFY_READY/notebook.js', encoding='utf-8').read()

ok, bad = [], []
def check(name, cond, detail=''):
    (ok if cond else bad).append((name, detail))

# ── ১ · লেখার কোড আর পড়ার কোড এখন একই ফাংশন থেকে ──────────────────────
check('[১] পড়ার দিকে callTapCode() আছে',
      'private fun callTapCode(): String =' in wn)
check('[১] callTapCode() ঠিক expectedCode()-ই ব্যবহার করে',
      'ModuleAuth.expectedCode(this) ?: staffCode' in wn)
check('[১] লেখার দিকে staff_code = personCode (expectedCode থেকে)',
      '.put("staff_code", personCode)' in ma and 'val expected = expectedCode(context)' in ma)

# ── ২ · call_taps-এর তিনটে জায়গাতেই নতুন কোড, পুরোনোটা আর নেই ─────────
for pat in ['"select=id&staff_code=eq.${callTapCode()}&call_date=eq.$key"',
            '"select=id&staff_code=eq.${callTapCode()}&call_date=gte.$key-01&call_date=lt.$monthEnd"']:
    check('[২] ক্লাউড-গোনায় নতুন কোড — ' + pat[-34:], pat in wn)
check('[২] ফোনে-জমা গোনাতেও নতুন কোড',
      'ModuleAuth.localCallTapCount(this@WorkNotebookActivity, callTapCode(), todayIso())' in wn)
check('[২] call_taps-এর কোনো লাইনে আর পুরোনো $staffCode নেই',
      not any('call_taps' in l and '$staffCode' in l for l in wn.split('\n')))

# ── ৩ · বাকি জায়গায় staffCode এক অক্ষরও বদলায়নি ──────────────────────
check('[৩] notebook_days আগের মতোই staffCode দিয়েই পড়ে',
      '"select=*&staff_code=eq.$staffCode&work_date=eq.$date&limit=1"' in wn)
check('[৩] staffCode এখনো বহু জায়গায় ব্যবহার হচ্ছে (কিছু ভাঙা হয়নি)',
      wn.count('staffCode') >= 25, 'পাওয়া গেল %d বার' % wn.count('staffCode'))

# ── ৪ · তারিখ — ফোন এখন ওয়েবের মতোই ভারতীয় সময়ে পাঠায় ──────────────
# ⛔ `str.index()` সরাসরি ডাকলে লেখাটা না থাকলে স্ক্রিপ্ট **ক্র্যাশ** করে,
#    আর ক্র্যাশ মানে ❌ ছাপা হয় না — অর্থাৎ ভুলটা চুপচাপ পার হয়ে যেত।
#    (উল্টো-পরীক্ষা করতে গিয়ে নিজেই ধরা পড়েছে।) তাই `find()` দিয়ে দেখা হয়।
def _before(hay, needle, span):
    at = hay.find(needle)
    return None if at < 0 else hay[max(0, at - span):at]

# 🔵 TK ডেটাবেসে চালিয়ে দেখালেন — `call_date`-এর ডিফল্ট আগে থেকেই ঠিক আছে
#    আর ভারতীয় সময়েই (`now() AT TIME ZONE 'Asia/Kolkata'`), ৭২৩টা সারির
#    একটারও তারিখ ফাঁকা নয়। তাই তারিখটা **সার্ভারই বসাবে** — ফোন পাঠাবে না,
#    কারণ কোনো স্টাফের ফোনের ঘড়ি ভুল থাকলে ভুল দিনে বসে যেত।
check('[৪] ফোন থেকে call_date পাঠানো হয় না (সার্ভারের ডিফল্টই থাকুক)',
      '.put("call_date"' not in ma)
_lc = _before(ma, 'bumpLocalCallTapCount(context, staffCodeNow, today)', 500)
check('[৪] ফোনে-জমা গোনার তারিখ ভারতীয় সময়ের (পড়ার দিকের মতোই)',
      _lc is not None and 'Asia/Kolkata' in _lc)

# ── ৫ · ওয়েবে এই ভুলটা নেই — লেখা ও পড়া একই কোড ──────────────────────
check('[৫] ওয়েবে লেখা — MOD._session.code', 'staff_code: MOD._session.code' in mc)
check('[৫] ওয়েবে পড়া — একই session().code',
      ".eq('staff_code', code)" in nb and "(window.MOD.session() || {}).code" in nb)

# ── ৬ · আসল চালিয়ে দেখা: ২২টা অ্যাকাউন্টেই লেখা-কোড == পড়া-কোড ───────
i = ma.index('fun expectedCode(context: Context): String? {')
body = ma[i:ma.index('\n    }', i)]
mapd = dict(re.findall(r'"(\d{10})" -> "([A-Z\-]+)"', body))
accs = re.findall(r'StaffAccount\("(\d+)",\s*"([^"]*)"', sd)
check('[৬] StaffDirectory পড়া গেছে', len(accs) >= 20, '%d টা অ্যাকাউন্ট' % len(accs))

old_bad, new_bad = [], []
for mob, name in accs:
    write_code = mapd.get(mob, name.strip().upper())      # logCallTap যেটা বসায়
    old_read   = name                                     # আগের পড়া (user.name)
    new_read   = write_code                               # এখনকার পড়া (expectedCode)
    if old_read != write_code: old_bad.append(name)
    if new_read != write_code: new_bad.append(name)

check('[৬] আগে সত্যিই মিলত না — অন্তত ৯টা অ্যাকাউন্টে',
      len(old_bad) >= 9, 'মিলত না: %d টা — %s' % (len(old_bad), ', '.join(old_bad[:4]) + '…'))
check('[৬] এখন একটাও অমিল নেই', len(new_bad) == 0,
      'এখনো অমিল: %s' % (', '.join(new_bad) or 'নেই'))

# ── ৭ · ভার্সন ────────────────────────────────────────────────────────
gr = io.open(ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts', encoding='utf-8').read() \
     if os.path.exists(ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts') else ''
vj = io.open(ROOT + '/03_NETLIFY_READY/version.json', encoding='utf-8').read()
# 🔧 V593 — এখানেও হুবহু "592" বাঁধা ছিল, তাই পরের ভার্সনে মিথ্যে ❌ দিত
#    (ঠিক যে ভুলটা verify_v591-এ ধরা পড়েছিল)। এখন "592 বা তার পরে"।
_g = re.search(r'val appVersionCode = (\d+)', gr)
_v = re.search(r'"versionCode": (\d+)', vj)
check('[৭] ভার্সন V592 বা তার পরে',
      bool(_g) and bool(_v) and int(_g.group(1)) >= 592 and int(_v.group(1)) >= 592,
      'gradle=%s · version.json=%s' % (_g and _g.group(1), _v and _v.group(1)))

print('🛡️ V592 পাহারাদার — App Calls গোনা')
print('=' * 66)
for n, d in ok:  print('  ✅ ' + n + (' · ' + d if d else ''))
for n, d in bad: print('  ❌ ' + n + (' · ' + d if d else ''))
print('-' * 66)
print('  পাশ: %d / %d' % (len(ok), len(ok) + len(bad)))
if bad:
    print('\nFAIL — V592 ❌'); sys.exit(1)
print('\nPASS — কল-গোনার লেখা ও পড়া এখন একই নামে ✅')
