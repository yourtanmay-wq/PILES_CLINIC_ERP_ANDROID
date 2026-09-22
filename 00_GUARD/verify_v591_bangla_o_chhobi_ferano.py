# -*- coding: utf-8 -*-
"""
🛡️ V591 পাহারাদার (২৩.০৮.২০২৬) — TK-এর দুটো অনুমোদিত কাজ।

  কাজ ১ · ভাঙা বাংলা: *"কিশানগঞ্জ এর যে কোন স্টাফ এর কাছে যেন বাংলা পর্দা
          কোথাও না থাকে … বাংলার পরিবর্তে হয় হিন্দি অথবা ইংলিশ"*
  কাজ ২ · সরানো ছবি: *"এখান থেকে যে ফটো গুলি বাদ দেওয়া হয়েছে — সে গুলি পরে
          আবার কিভাবে এবং কোথায় পাবো"*

⛔ এই পাহারাদার কোড **পড়ে** যাচাই করে না — কাজ ১-এর জন্য ফোনের নিজের
   অনুবাদ-নিয়মটা এখানে হুবহু চালিয়ে দেখা হয় (verification by execution)।
"""
import io, re, sys, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT   = ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/'
NB   = KT + 'native/NoBengali.kt'
DCA  = KT + 'clinical/DoctorCheckupActivity.kt'
APR  = KT + 'clinical/AnatomyPictureRepository.kt'
JS   = io.open(ROOT + '/03_NETLIFY_READY/app.js', encoding='utf-8').read()
CSS  = io.open(ROOT + '/03_NETLIFY_READY/styles.css', encoding='utf-8').read()
nb   = io.open(NB,  encoding='utf-8').read()
dca  = io.open(DCA, encoding='utf-8').read()
apr  = io.open(APR, encoding='utf-8').read()

ok, bad = [], []
def check(name, cond, detail=''):
    (ok if cond else bad).append((name, detail))

# ───────────────────────── ফোনের নিয়মটা এখানে চালানো ─────────────────────
def _block(name):
    marks = {n: nb.index('private val %s: Map<String, String> = mapOf(' % n)
             for n in ('WHOLE', 'MAP', 'HINDI')}
    st = sorted(marks.values()) + [len(nb)]
    i = marks[name]
    return nb[i:st[st.index(i) + 1]]

def _pairs(txt):
    out = {}
    txt = re.sub(r'//[^\n]*', '', txt)
    txt = re.sub(r'/\*.*?\*/', '', txt, flags=re.S)
    def un(x):
        return x.replace('\\n', '\n').replace('\\"', '"').replace('\\\\', '\\').replace('\\t', '\t')
    for m in re.finditer(r'"((?:[^"\\]|\\.)*)"\s+to\s+"((?:[^"\\]|\\.)*)"', txt):
        out[un(m.group(1))] = un(m.group(2))
    return out

WHOLE, MAP, HINDI = _pairs(_block('WHOLE')), _pairs(_block('MAP')), _pairs(_block('HINDI'))
BN = re.compile(r'[ঀ-৿]')
_m = sorted(MAP.items(),   key=lambda kv: -len(kv[0]))
_h = sorted(HINDI.items(), key=lambda kv: -len(kv[0]))

def fix(text):
    """NoBengali.fix()-এর হুবহু নকল — ক্রমটাও এক (WHOLE → HINDI → MAP → সংখ্যা → জাল)।"""
    if not BN.search(text):
        return text, 'ok'
    w = WHOLE.get(text.strip())
    if w is not None:
        return w, 'whole'
    lead = text[:len(text) - len(text.lstrip())]
    tail = text[len(text.rstrip()):]
    s = text.strip()
    for bn, hi in _h:
        if bn in s: s = s.replace(bn, hi)
    for bn, en in _m:
        if bn in s: s = s.replace(bn, en)
    if 'টা' in s:
        s = re.sub(r'([0-9]+) টা(?![ঀ-৿])', lambda m: m.group(1) + ' नग', s)
        s = re.sub(r'([0-9]+)টা(?![ঀ-৿])',  lambda m: m.group(1) + ' बजे', s)
    stripped = False
    if BN.search(s):
        s = ''.join(c for c in s if not ('ঀ' <= c <= '৿'))
        stripped = True
    s = s.replace('।', '.')
    while '  ' in s: s = s.replace('  ', ' ')
    s = s.replace(' ·  ', ' · ').replace('· ·', '·').replace(' .', '.').strip()
    while s.endswith('—') or s.endswith('·') or s.endswith(':'):
        s = s[:-1].strip()
    return lead + s + tail, ('BROKEN' if stripped else 'translated')

# ── ১ · TK-এর নিজের ধরা লেখাগুলো এখন সত্যিই অনুবাদ হয় ────────────────────
MUST = {
    'মুছে ফেলা হচ্ছে': 'Deleting',
    'ফোলা 3 টা': 'सूजन 3 नग',
    'মাংস ফোলানো 2 টা': 'माँस की सूजन 2 नग',
    'নালীর দাগ 1 টা': 'नली का निशान 1 नग',
    '7টা': '7 बजे',
    '12টা': '12 बजे',
    'ফোলা: 3টা, 7টা': 'सूजन: 3 बजे, 7 बजे',
    '₹6000 / ক্ষার সূত্র': '₹6000 / क्षार सूत्र',
}
for src, want in MUST.items():
    got, kind = fix(src)
    check('[১] অনুবাদ চলে — %r' % src, got == want, 'পেলাম %r' % got)

# ── ২ · সংখ্যার নিয়ম যেন ভালো লেখা নষ্ট না করে ───────────────────────────
SAFE = ['অবশিষ্ট টাকা', '💵 টাকার হিসাব', 'অফিস টাইম শেষ',
        'ফোলান — মাংসের উপরে আঙুল টানুন', 'টাটকা ফল ও সবজি খান', 'মোট 4টাকা দিন', '5 টাকা']
for t in SAFE:
    s = t
    if 'টা' in s:
        s = re.sub(r'([0-9]+) টা(?![ঀ-৿])', lambda m: m.group(1) + ' नग', s)
        s = re.sub(r'([0-9]+)টা(?![ঀ-৿])',  lambda m: m.group(1) + ' बजे', s)
    check('[২] সংখ্যার নিয়ম ছোঁয় না — %r' % t, s == t, 'বদলে গেছে: %r' % s)

# ── ৩ · সংখ্যার নিয়ম দুই জায়গাতেই বসানো ─────────────────────────────────
# ⛔ শুধু নামটা আছে কিনা দেখলে যথেষ্ট নয় (নাম বদলে দিলেও "আছে" ধরত) —
#    তাই নিয়মটার **হুবহু লেখাটাই** মেলানো হয়।
_KT_COUNT  = 'private val NUM_COUNT  = Regex("([0-9]+) টা(?![\\\\u0980-\\\\u09FF])")'
_KT_OCLOCK = 'private val NUM_OCLOCK = Regex("([0-9]+)টা(?![\\\\u0980-\\\\u09FF])")'
check('[৩] ফোনে NUM_COUNT আছে',  _KT_COUNT in nb)
check('[৩] ফোনে NUM_OCLOCK আছে', _KT_OCLOCK in nb)
check('[৩] ফোনে দুটো নিয়মই fix()-এ চলে',
      'NUM_COUNT.replace(s)' in nb and 'NUM_OCLOCK.replace(s)' in nb)
# ⚠️ app.js-এ বাংলা অক্ষরটা `\u099F\u09BE` লেখায় বসানো (টা), তাই এখানে
#    ঠিক ওই লেখাটাই খোঁজা হয় — অক্ষর খুঁজলে "নেই" বলে ভুল ধরত।
_TA = r"\u099F\u09BE"
check('[৩] ওয়েবে গোনার নিয়ম',      (r"([0-9]+) " + _TA + r"(?![\u0980-\u09FF])") in JS)
check('[৩] ওয়েবে ঘড়ির কাঁটার নিয়ম', (r"([0-9]+)" + _TA + r"(?![\u0980-\u09FF])") in JS)

# ── ৪ · তিনটে তালিকা ফোন ও ওয়েবে হুবহু এক ───────────────────────────────
def js_pairs_arr(name):
    i = JS.index('var %s=[' % name); j = JS.index('\n];', i)
    out = {}
    for m in re.finditer(r"\['((?:[^'\\]|\\.)*)','((?:[^'\\]|\\.)*)'\]", JS[i:j]):
        def un(x):
            return x.replace("\\n", "\n").replace("\\'", "'").replace('\\\\', '\\').replace('\\t', '\t')
        out[un(m.group(1))] = un(m.group(2))
    return out
def js_pairs_obj(name):
    i = JS.index('var %s={' % name); j = JS.index('};', i)
    out = {}
    for m in re.finditer(r"'((?:[^'\\]|\\.)*)':'((?:[^'\\]|\\.)*)'", JS[i:j]):
        def un(x):
            return x.replace("\\n", "\n").replace("\\'", "'").replace('\\\\', '\\').replace('\\t', '\t')
        out[un(m.group(1))] = un(m.group(2))
    return out
for nm, kmap, jmap in (('WHOLE', WHOLE, js_pairs_obj('WLV1_NOBN_WHOLE')),
                       ('MAP',   MAP,   js_pairs_arr('WLV1_NOBN_MAP')),
                       ('HINDI', HINDI, js_pairs_arr('WLV1_NOBN_HI'))):
    check('[৪] %s তালিকা ফোন = ওয়েব' % nm, kmap == jmap,
          'ফোনে %d · ওয়েবে %d · শুধু ফোনে %d · শুধু ওয়েবে %d'
          % (len(kmap), len(jmap), len(set(kmap) - set(jmap)), len(set(jmap) - set(kmap))))

# ── ৫ · ওয়েবের লম্বা টুকরো আগে (নইলে ছোট শব্দে লম্বা বাক্য ভেঙে যায়) ──────
for name in ('WLV1_NOBN_MAP', 'WLV1_NOBN_HI'):
    i = JS.index('var %s=[' % name); j = JS.index('\n];', i)
    ks = re.findall(r"\['((?:[^'\\]|\\.)*)',", JS[i:j])
    # ⚠️ মাপটা **আসল লেখার** উপর, JS-লেখার উপর নয়। `\n` উৎসে দুই অক্ষর কিন্তু
    #    চলার সময়ে এক — উৎস ধরে মাপলে ঠিক ক্রমকেও "উল্টো" বলে ধরত।
    def _un(x):
        return x.replace('\\n', '\n').replace("\\'", "'").replace('\\\\', '\\').replace('\\t', '\t')
    L = [len(_un(k)) for k in ks]
    check('[৫] %s — সবচেয়ে লম্বা আগে' % name,
          all(L[x] >= L[x + 1] for x in range(len(L) - 1)))

# ── ৬ · সরানো ছবি ফেরানো · ফোন ────────────────────────────────────────────
check('[৬] ফোনে hiddenRows() আছে',  'fun hiddenRows(ctx: Context)' in apr)
check('[৬] ফোনে restoreRow() আছে',  'fun restoreRow(ctx: Context, id: String): JSONObject?' in apr)
check('[৬] ফেরানোর সারিতে hidden ফাঁকা হয', 'put("hidden", "")' in apr)
check('[৬] যোগ-করা ছবির ছবিটুকু ক্লাউড থেকে', 'private fun fetchPhoto(id: String)' in apr)
check('[৬] ছবি না পেলে কিছুই জমা হয় না',
      'if (!isBuiltIn && photo.isBlank()) return null' in apr)
check('[৬] সারিতে ♻ ঘর — সরানো থাকলে তবেই',
      'val dropped = AnatomyPictureRepository.hiddenRows(this)' in dca
      and 'if (dropped.isNotEmpty()) {' in dca)
check('[৬] ফোনে ফেরানোর তালিকা খোলে', 'private fun askRestorePicture(' in dca)
check('[৬] ফোনে ফেরানোর কাজ আলাদা থ্রেডে',
      'private fun doRestorePicture(' in dca and 'BackgroundWork.run {' in dca)

# ── ৭ · সরানো ছবি ফেরানো · ওয়েব ──────────────────────────────────────────
check('[৭] ওয়েবে wlv1AnatHiddenRows()', 'function wlv1AnatHiddenRows(){' in JS)
check('[৭] ওয়েবে wlv1AnatRestoreOpen()', 'function wlv1AnatRestoreOpen(){' in JS)
check('[৭] ওয়েবে wlv1AnatRestorePic()', 'async function wlv1AnatRestorePic(id){' in JS)
check('[৭] ওয়েবেও hidden ফাঁকা হয়ে ফেরে', "{hidden:'',photo:photo," in JS)
check('[৭] ওয়েবেও ছবি না পেলে কিছুই জমা হয় না',
      "if(!isBuiltIn && !photo){" in JS)
check('[৭] ওয়েবের সারিতে ♻ ঘর', 'wlv1AnatBack' in JS and "onclick=\"wlv1AnatRestoreOpen()\"" in JS)
check('[৭] ♻ ঘরের রং styles.css-এ', '.wlv1AnatAdd.wlv1AnatBack{' in CSS)
check('[৭] ব্রাউজারের prompt-ও বাংলা-মুক্ত হয়',
      "prompt(wlv1NoBnFixSafe(" in JS)

# ── ৮ · ফেরানোর নিজস্ব লেখাগুলোও অনুবাদ হয় ───────────────────────────────
for t in ['সরানো ছবি ফেরান', 'ফেরানো হচ্ছে…', 'ফিরে এসেছে',
          'ফেরানো গেল না — ইন্টারনেট দেখে আবার চেষ্টা করুন',
          'কোন ছবিটা ফেরাবেন? নম্বর লিখুন', 'সরানো কোনো ছবি নেই',
          'ছবিটা পাওয়া গেল না', 'নম্বরটা মিলল না',
          'এই ফোনে ফিরল — ক্লাউডে পরে যাবে',
          'এই ব্রাউজারে ফিরল — ক্লাউডে পরে যাবে']:
    got, kind = fix(t)
    check('[৮] ফেরানোর লেখা অনুবাদ হয় — %r' % t, kind != 'BROKEN', 'পেলাম %r' % got)

# ── ৯ · ভার্সন ────────────────────────────────────────────────────────────
gr = io.open(ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts', encoding='utf-8').read()
vj = io.open(ROOT + '/03_NETLIFY_READY/version.json', encoding='utf-8').read()
# 🔧 V592-এ ধরা পড়ল: এখানে হুবহু "591" বাঁধা ছিল, তাই পরের ভার্সনে গিয়ে
#    এই পাহারাদার মিথ্যে করে ❌ দিত। এখন **V591 বা তার পরে** হলেই চলে —
#    দুই ফাইলের মিল আছে কিনা সেটা `verify_version_json.py` আলাদা করে দেখে।
import re as _re
_gc = _re.search(r'val appVersionCode = (\d+)', gr)
_vc = _re.search(r'"versionCode": (\d+)', vj)
check('[৯] ভার্সন V591 বা তার পরে',
      bool(_gc) and bool(_vc) and int(_gc.group(1)) >= 591 and int(_vc.group(1)) >= 591,
      'gradle=%s · version.json=%s' % (_gc and _gc.group(1), _vc and _vc.group(1)))

# ───────────────────────────── ফলাফল ─────────────────────────────────────
print('🛡️ V591 পাহারাদার — ভাঙা বাংলা ও সরানো ছবি ফেরানো')
print('=' * 66)
for n, d in ok:  print('  ✅ ' + n + (' · ' + d if d else ''))
for n, d in bad: print('  ❌ ' + n + (' · ' + d if d else ''))
print('-' * 66)
print('  পাশ: %d / %d' % (len(ok), len(ok) + len(bad)))
if bad:
    print('\nFAIL — V591 ❌')
    sys.exit(1)
print('\nPASS — TK-এর দুটো কাজই দুই অ্যাপে বসে আছে ✅')
