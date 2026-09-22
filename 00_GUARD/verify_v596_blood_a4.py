# -*- coding: utf-8 -*-
"""
🛡️ V596 পাহারাদার (২৩.০৮.২০২৬) — **Blood Test / Investigation Advice-এর A4 কাগজ**।

TK ধাপে ধাপে ডেমো-ফটো দেখে অনুমোদন দিয়েছেন:
  · *"প্রফেশনাল লুক · বড় হসপিটালে কেমন দেখতে হয় ঠিক সেরকম"*
  · *"হেডার ও একদম নিচের পাঠ প্রেসক্রিপশনের মতোই থাকবে"*
  · *"জল ছবি টা পুরো আসতে হবে"*
  · *"দাগ গুলি এত ক্লিয়ার থাকবে না, হাল্কা দাগ"*
  · *"এক্সেল এর দাগগুলো খুব উজ্জ্বল লাগছে, একটু হালকা করুন"*
"""
import io, os, re, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
K = ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/'
js  = io.open(ROOT + '/03_NETLIFY_READY/app.js', encoding='utf-8').read()
css = io.open(ROOT + '/03_NETLIFY_READY/styles.css', encoding='utf-8').read()
kt  = io.open(K + 'print/InvestigationHtmlPrint.kt', encoding='utf-8').read()
act = io.open(K + 'clinical/InvestigationAdviceActivity.kt', encoding='utf-8').read()
pdf = io.open(K + 'print/ClinicPdfBuilder.kt', encoding='utf-8').read()

ok, bad = [], []
def check(n, c, d=''): (ok if c else bad).append((n, d))

# ── ১ · ওয়েব: নতুন A4 কাগজ, আর তাতেই যায় ─────────────────────────────
check('[১] wlv1InvestigationA4() আছে', 'function wlv1InvestigationA4(id, tests, remarks){' in js)
check('[১] wlv1InvRowsHtml() আছে', 'function wlv1InvRowsHtml(tests){' in js)
check('[১] printBlood এখন ওটাই ডাকে', 'wlv1InvestigationA4(id, selected.split' in js)
check('[১] পুরোনো সাদামাটা টেবিল আর নেই',
      '<table class="printTable finalPrintTable"><tr><th>Tests</th></tr>' not in js)

# ── ২ · হেডার ও নিচের পাঠ প্রেসক্রিপশনের **সেই ফাংশন থেকেই** ─────────
i = js.find('function wlv1InvestigationA4(')
blk = js[i:js.find('window["wlv1InvestigationA4"]', i)] if i >= 0 else ''
check('[২] লেটারহেড printHead() থেকে', "printHead(p,'PRESCRIPTION')" in blk)
check('[২] রোগীর ঘর rxPrintPatientTwoCol() থেকে', 'rxPrintPatientTwoCol(p)' in blk)
check('[২] বারকোড printVerifyCenter() থেকে', 'printVerifyCenter(p)' in blk)
check('[২] দুই সই — TK BISWAS ও Dr. K.H MANDAL',
      'TK BISWAS' in blk and 'Dr. K.H MANDAL' in blk and 'Regd 12386' in blk)
check('[২] সবুজ পটি আগের লেখাতেই',
      'All treatments are Ayurvedic &amp; Natural' in blk)

# ── ৩ · শেয়ার-করা কিছু ছোঁয়া হয়নি ───────────────────────────────────
for fn in ('function printHead(', 'function printFoot(', 'function printDoctorsHtml(',
           'function rxPrintPatientTwoCol(', 'function printVerifyCenter('):
    check('[৩] %s অক্ষত' % fn.replace('function ', '').rstrip('('), fn in js)
check('[৩] ফোনে ClinicPdfBuilder ছোঁয়া হয়নি (OWNER LOCKED)',
      'OWNER LOCKED' in pdf or 'ClinicPdfBuilder' in pdf)

# ── ৪ · জলছাপ — পুরো গোল, হালকা, ধূসর ────────────────────────────────
check('[৪] ওয়েবে নিজের জলছাপ (.invWm)', '.invBox .invWm{' in css and '<div class="invWm">' in js)
check('[৪] ওয়েবে গোলটা 150mm (কাটে না)', 'width:150mm!important;height:150mm!important' in css)
check('[৪] ওয়েবে ঝাপসা ও ধূসর',
      'opacity:.030!important' in css and 'filter:grayscale(1)!important' in css)
check('[৪] ওয়েবে শেয়ার-করা .wm এই কাগজে লুকানো',
      '.printArea.invPage>.wm{display:none!important}' in css)
check('[৪] ফোনেও ধূসর ও ঝাপসা জলছাপ',
      'opacity:.022' in kt and 'filter:grayscale(1)' in kt)

# ── ৫ · টেবিলের দাগ হালকা (এক্সেলের মতো কালো নয়) ────────────────────
check('[৫] ওয়েবে ঘরের চারদিকের দাগ বন্ধ', '.invTbl td{' in css and 'border:0!important' in css)
check('[৫] ওয়েবে হালকা আনুভূমিক রেখা', 'border-bottom:1px solid #E8EEEA!important' in css)
check('[৫] ফোনেও একই হালকা রেখা', 'border-bottom:1px solid #E8EEEA' in kt)
# ⚠️ মন্তব্য বাদ দিয়ে দেখতে হয় — নইলে ব্যাখ্যার ভিতরে লেখা "#222"-কেই
#    কোড ভেবে মিথ্যে ❌ দেয় (উল্টো-পরীক্ষা করতে গিয়ে ধরা পড়েছে)।
_v596 = css[css.index('/* 🔵🔒 V596'):] if '/* 🔵🔒 V596' in css else ''
_v596_code = re.sub(r'/\*.*?\*/', '', _v596, flags=re.S)
check('[৫] কালো #222 দাগ এই টেবিলে ব্যবহার হয়নি',
      bool(_v596_code) and '#222' not in _v596_code)

# ── ৬ · ফোনের কাগজ — ওয়েবের যমজ ─────────────────────────────────────
check('[৬] InvestigationHtmlPrint আছে', 'object InvestigationHtmlPrint' in kt)
check('[৬] WebView + PrintManager (V390-এর প্রমাণিত পথ)',
      'PrintManager' in kt and 'MediaSize.ISO_A4' in kt)
check('[৬] পর্দা এখন ওটাই ডাকে',
      'InvestigationHtmlPrint.print(' in act)
check('[৬] পুরোনো PrintPreview পথ আর নেই',
      'PrintMappers.investigationAdvice(invRemarks())' not in act)
check('[৬] দুই কাগজেই একই ঘর ও লেখা',
      'TOTAL ' in kt and 'INVESTIGATION(S) ADVISED' in kt
      and 'Report Collection Date' in kt and 'Next Follow-up Date' in kt)
check('[৬] ফাঁকা ছাপা লাইন — দুই দিকেই ১৩',
      'MIN_ROWS = 13' in kt and 'MIN_ROWS=13' in js)

# ── ৭ · কাগজে বাংলা নেই (TK-এর লক করা নিয়ম) ─────────────────────────
bn = re.compile(r'[ঀ-৿]')
i2 = kt.index('return """<!DOCTYPE html>')
check('[৭] ফোনের কাগজের লেখায় বাংলা নেই', not bn.search(kt[i2:kt.index('"""\n    }\n}', i2)]))
check('[৭] ওয়েবের কাগজের লেখায় বাংলা নেই', not bn.search(re.sub(r'/\*.*?\*/', '', blk, flags=re.S)))

# ── ৮ · ভার্সন ───────────────────────────────────────────────────────
gr = io.open(ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts', encoding='utf-8').read()
vj = io.open(ROOT + '/03_NETLIFY_READY/version.json', encoding='utf-8').read()
_g = re.search(r'val appVersionCode = (\d+)', gr); _v = re.search(r'"versionCode": (\d+)', vj)
check('[৮] ভার্সন V596 বা তার পরে',
      _g and _v and int(_g.group(1)) >= 596 and int(_v.group(1)) >= 596,
      'gradle=%s · version.json=%s' % (_g and _g.group(1), _v and _v.group(1)))

print('🛡️ V596 পাহারাদার — Blood Test-এর A4 কাগজ')
print('=' * 66)
for n, d in ok:  print('  ✅ ' + n + (' · ' + d if d else ''))
for n, d in bad: print('  ❌ ' + n + (' · ' + d if d else ''))
print('-' * 66); print('  পাশ: %d / %d' % (len(ok), len(ok) + len(bad)))
if bad: print('\nFAIL — V596 ❌'); sys.exit(1)
print('\nPASS — কাগজ প্রফেশনাল, হেডার-ফুটার প্রেসক্রিপশনের মতোই ✅')
