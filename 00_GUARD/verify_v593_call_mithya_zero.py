# -*- coding: utf-8 -*-
"""
🛡️ V593 পাহারাদার (২৩.০৮.২০২৬) — **কলের মিথ্যা "0"**।

TK: *"আরো যাচাই করুন · কারণ এই একই ধরনের সমস্যা বেশ কয়েকবার আপনাকে বলা
     হয়েছে · প্রতিবার আপনি বলেছেন হ্যাঁ ঠিক হয়ে গেছে · স্টাফদের কাছে আমার
     কথা শুনতে হয় শুধুমাত্র আপনার কারণে"*

**V590 কাজটা অর্ধেক করেছিল।** New Enquiry · Registration · Collection —
এই তিনটে পড়া ব্যর্থ হলে "…" দেখায় (B496 থেকেই)। কিন্তু App Calls ·
Outside Calls · Total call ব্যর্থ হলেও **`0`** ছাপত — পর্দাতেও, আর
**স্টাফের পাঠানো WhatsApp রিপোর্টেও**। TK-এর কাছে যে রিপোর্ট যেত, সেটাই
মিথ্যা 0 বয়ে নিয়ে যেত।

⇒ এখন কল-সংখ্যাও অন্য তিনটের মতোই: পড়া না গেলে "…"।
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
K = ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/'
wn = io.open(K + 'modules/WorkNotebookActivity.kt', encoding='utf-8').read()
nb = io.open(ROOT + '/03_NETLIFY_READY/notebook.js', encoding='utf-8').read()

ok, bad = [], []
def check(name, cond, detail=''):
    (ok if cond else bad).append((name, detail))

# ── ১ · ফোনে একটাই জায়গা থেকে কল-সংখ্যা লেখা হয় ───────────────────────
check('[১] callsOk() আছে', 'private fun callsOk(s: JSONObject): Boolean' in wn)
check('[১] callTxt() আছে', 'private fun callTxt(s: JSONObject, key: String): String' in wn)
check('[১] callTxt ব্যর্থ হলে "…" দেয়',
      'if (callsOk(s)) s.optInt(key).toString() else "…"' in wn)

# ── ২ · কাঁচা optInt আর কোথাও ছাপা হয় না ──────────────────────────────
raw = [l.strip() for l in wn.split('\n')
       if re.search(r'optInt\("(appCalls|outsideCalls|totalCalls)"\)', l)
       and 'val cloudCalls' not in l]
check('[২] কল-সংখ্যা আর কাঁচা ছাপা হয় না', not raw,
      'এখনো কাঁচা: %d — %s' % (len(raw), (raw[0][:60] if raw else '')))

# ── ৩ · সাতটা রিপোর্ট-লাইনই callTxt দিয়ে ──────────────────────────────
check('[৩] রিপোর্টে App Calls — callTxt',
      wn.count('.append("\\nApp Calls: ").append(callTxt(s, "appCalls"))') >= 4,
      'পাওয়া গেল %d' % wn.count('.append("\\nApp Calls: ").append(callTxt(s, "appCalls"))'))
check('[৩] রিপোর্টে Outside Calls — callTxt',
      wn.count('.append(callTxt(s, "outsideCalls"))') >= 4)
check('[৩] রিপোর্টে Total call — callTxt',
      wn.count('.append(callTxt(s, "totalCalls"))') >= 4)
check('[৩] গ্রিড-কার্ডেও callTxt', wn.count('"App Calls" to callTxt(s, "appCalls")') == 2)

# ── ৪ · পর্দার ঘরে ব্যর্থ পড়া আর "0" লিখবে না ─────────────────────────
check('[৪] appVal.text এখন শর্তের ভিতরে',
      'if (callsOk(s) || appCallsNow > 0) {' in wn and 'appVal.text = appCallsNow.toString()' in wn)
i = wn.find('appVal.text = appCallsNow.toString()')
check('[৪] শর্তটা ঠিক ওই লাইনের আগেই',
      i > 0 and 'if (callsOk(s) || appCallsNow > 0) {' in wn[max(0, i - 400):i])

# ── ৫ · "weak internet" সতর্কবার্তা এখন কল ব্যর্থ হলেও ওঠে ─────────────
check('[৫] anyFailed-এ কলও ধরা হয়', '|| !callsOk(stats)' in wn)
check('[৫] সারাংশে তিনটেই callTxt',
      'val appTxt = callTxt(stats, "appCalls")' in wn
      and 'val outTxt = callTxt(stats, "outsideCalls")' in wn
      and 'val totTxt = callTxt(stats, "totalCalls")' in wn)

# ── ৬ · সাথে-সাথে দেখানো স্থানীয় গোনাও একই কোডে ──────────────────────
check('[৬] স্থানীয় গোনা callTapCode() দিয়ে (দুই জায়গাতেই)',
      wn.count('localCallTapCount(this, callTapCode(), todayIso())') == 1
      and wn.count('localCallTapCount(this@WorkNotebookActivity, callTapCode(), todayIso())') == 1)
check('[৬] staffCode দিয়ে আর কোনো স্থানীয় গোনা নেই',
      'localCallTapCount(this, staffCode' not in wn
      and 'localCallTapCount(this@WorkNotebookActivity, staffCode' not in wn)

# ── ৭ · ওয়েবেও হুবহু একই নিয়ম ─────────────────────────────────────────
check('[৭] ওয়েবে callTxt()', "function callTxt(v) { return (v === null || v === undefined) ? '…' : String(v); }" in nb)
check('[৭] ওয়েবে callSum()', 'function callSum(a, b)' in nb)
i = nb.find('async function appCallCount(range)')
blk = nb[i:nb.find('\n  }', i)] if i >= 0 else ''
check('[৭] ওয়েবে ব্যর্থ পড়া null ফেরায় (0 নয়)',
      'return null;' in blk and 'return 0;' not in blk)
check('[৭] ওয়েবের পর্দায় callTxt', "autoRow('App Calls (auto)', callTxt(apc))" in nb)
check('[৭] ওয়েবের রিপোর্টেও callTxt', nb.count("'\\nApp Calls: ' + callTxt(apc)") == 2)
check('[৭] ওয়েবে null যোগ করে মিথ্যা সংখ্যা বানায় না',
      'totalCalls: apc + occ' not in nb and nb.count('totalCalls: callSum(apc, occ)') == 2)

# ── ৮ · সত্যিকারের ০ যেন লুকিয়ে না যায় (উল্টোদিকের ঝুঁকি) ────────────
check('[৮] পড়া সফল হলে সংখ্যা যাই হোক ছাপা হয় (০-ও)',
      'if (callsOk(s)) s.optInt(key).toString()' in wn)

# ── ৯ · ভার্সন ────────────────────────────────────────────────────────
gr = io.open(ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts', encoding='utf-8').read()
vj = io.open(ROOT + '/03_NETLIFY_READY/version.json', encoding='utf-8').read()
_g = re.search(r'val appVersionCode = (\d+)', gr)
_v = re.search(r'"versionCode": (\d+)', vj)
check('[৯] ভার্সন V593 বা তার পরে',
      bool(_g) and bool(_v) and int(_g.group(1)) >= 593 and int(_v.group(1)) >= 593,
      'gradle=%s · version.json=%s' % (_g and _g.group(1), _v and _v.group(1)))

print('🛡️ V593 পাহারাদার — কলের মিথ্যা "0"')
print('=' * 66)
for n, d in ok:  print('  ✅ ' + n + (' · ' + d if d else ''))
for n, d in bad: print('  ❌ ' + n + (' · ' + d if d else ''))
print('-' * 66)
print('  পাশ: %d / %d' % (len(ok), len(ok) + len(bad)))
if bad:
    print('\nFAIL — V593 ❌'); sys.exit(1)
print('\nPASS — পড়া না গেলে আর মিথ্যা 0 নয়, সৎ "…" ✅')
