# -*- coding: utf-8 -*-
"""
🛡️ V585 (২৩.০৮.২০২৬) — ঘড়ির কাঁটা নিজে হিসাবের পাহারা।

TK-নির্দেশ: *"যেখানেই থাকবে চারটা কেন বাঁচবে — এটাতো অটোমেটিক্যালি হওয়ার কথা"*।

চালানো:  python3 00_GUARD/verify_v585_clock.py
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT   = os.path.join(ROOT, '02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/clinical')
WEB  = os.path.join(ROOT, '03_NETLIFY_READY')

def read(p):
    try: return io.open(p, encoding='utf-8').read()
    except Exception: return ''

CLOCK = read(os.path.join(KT, 'AnatomyClock.kt'))
VIEW  = read(os.path.join(KT, 'AnatomyView.kt'))
ICON  = read(os.path.join(KT, 'AnatToolIcon.kt'))
ACT   = read(os.path.join(KT, 'DoctorCheckupActivity.kt'))
JS    = read(os.path.join(WEB, 'app.js'))

def nocomment_kt(t):
    t = re.sub(r'/\*.*?\*/', '', t, flags=re.S)
    return re.sub(r'//[^\n]*', '', t)
def nocomment_js(t):
    t = re.sub(r'/\*.*?\*/', '', t, flags=re.S)
    return t

ACTC, VIEWC = nocomment_kt(ACT), nocomment_kt(VIEW)
JSC = nocomment_js(JS)

C = []
def ck(t, ok): C.append((t, bool(ok)))

# ── ১. হিসাব দুই জায়গাতেই আছে ──
ck('ঘড়ির হিসাব — ফোনে (AnatomyClock.hourAt)', 'fun hourAt(' in CLOCK)
ck('ঘড়ির হিসাব — ওয়েবে (wlv1AnatHourOf)', 'function wlv1AnatHourOf(' in JS)
ck('12 = সোজা উপর, ঘড়ির দিকে — ফোনে', 'atan2(dx, -dy)' in CLOCK)
ck('12 = সোজা উপর, ঘড়ির দিকে — ওয়েবে', 'Math.atan2(dx,-dy)' in JSC.replace(' ', ''))

# ── ২. মাপা কেন্দ্র দুই জায়গাতেই এক ──
for key, x, y in (('anat26', '49.8', '28.7'), ('anat27', '50.0', '52.0')):
    ck('মাপা কেন্দ্র %s — ফোনে' % key, ('"%s" to Pair(%s, %s)' % (key, x, y)) in CLOCK)
    ck('মাপা কেন্দ্র %s — ওয়েবে' % key,
       ('%s:[%s,%s]' % (key, x, y)) in JSC.replace(' ', '') or
       ('%s:[%s,%s]' % (key, x.rstrip('0').rstrip('.'), y)) in JSC.replace(' ', ''))

# ── ৩. নতুন "কেন্দ্র" হাতিয়ার ──
ck('কেন্দ্র হাতিয়ার — ফোনে (Tool.CENTRE)', 'CENTRE' in VIEWC and 'Tool.CENTRE' in ACTC)
ck('কেন্দ্র হাতিয়ার — ওয়েবে', "['centre','কেন্দ্র']" in JS)
ck('কেন্দ্র আইকন — ফোনে', '"centre" ->' in ICON)
ck('কেন্দ্র আইকন — ওয়েবে', 'centre:' in JS and 'WLV1_ANAT_ICONS' in JS)
ck('কেন্দ্রের এক-লাইনের লেখা — ফোনে', 'Tool.CENTRE ->' in ACT)
ck('কেন্দ্রের এক-লাইনের লেখা — ওয়েবে', 'centre:' in JS and 'WLV1_ANAT_TIPS' in JS)

# ── ৪. পুরনো পপ-আপ আর ডাকা হয় না ──
ck('পুরনো তালিকা-পপআপ আর ডাকা হয় না — ফোনে', 'askPileLabel(view)' not in ACTC)
ck('পুরনো prompt আর নেই — ওয়েবে', 'ঘড়ির কাঁটা অনুযায়ী জায়গা — 1 থেকে 12' not in JSC)
ck('askPileLabel ফাংশনটা মোছা হয়নি (TK-এর নিয়ম)', 'private fun askPileLabel' in ACT)

# ── ৫. লেখাটা চিহ্ন বসানোর সময়ই হিসাব হয় (তাই A4/History ছুঁতে হয়নি) ──
ck('চিহ্ন বসানোর সময় হিসাব — ফোনে', 'AnatomyClock.hourAt(' in VIEWC)
ck('চিহ্ন বসানোর সময় হিসাব — ওয়েবে', 'wlv1AnatClockLabel(s.down[0],s.down[1])' in JSC)
ck('পুরনো pileLabel আর ব্যবহার হয় না — ফোনে', 'label = pileLabel' not in VIEWC)
ck('পুরনো s.label আর ব্যবহার হয় না — ওয়েবে', "label:s.label" not in JSC.replace(' ', ''))

# ── ৬. পর্দায় ঘড়ির বলয় ──
# 🔵 V587 (২৩.০৮.২০২৬) — TK-এর পরের নির্দেশে (*"ঘড়ি আকানো থাকবে না"*) ছবিতে
# ঘড়ি আঁকাটা তুলে দেওয়া হয়েছে, তাই এই দুটো যাচাইও তুলে নেওয়া হলো — নইলে
# পাহারাটা TK-এর নিজের সিদ্ধান্তের জন্যই চিরকাল লাল থাকত।
# ⛔ **হিসাবের যাচাই উপরে আগের মতোই আছে** — নিয়ম শিথিল হয়নি; কেন্দ্র জমা
#    রাখা ও o'clock বার করা দুটোই এখনো মিলিয়ে দেখা হয়।
ck('ঘড়ির আঁকা তোলা হয়েছে (V587, TK-নির্দেশ) — ফোনে', 'clockCentre?.let' not in VIEW)
ck('ঘড়ির আঁকা তোলা হয়েছে (V587, TK-নির্দেশ) — ওয়েবে',
   'function wlv1AnatDrawClock(){' in JS and 'ctx.arc(cx,cy,rr' not in JS)

# ── ৭. ফ্রি-প্লান — কেন্দ্র জমায় কোনো ক্লাউড কল নেই ──
ck('কেন্দ্র ফোনেই জমা (ক্লাউড কল নেই)', 'SharedPreferences' not in CLOCK.split('fun setCentre')[0] or 'SupabaseClient' not in CLOCK)
ck('কেন্দ্র ব্রাউজারেই জমা (ক্লাউড কল নেই)',
   'localStorage' in JS[JS.find('function wlv1AnatCentreSet'):JS.find('function wlv1AnatHourOf')])

print('=' * 66)
print('🛡️  V585 — ঘড়ির কাঁটা নিজে হিসাব (ফোন ↔ ওয়েব)')
print('=' * 66)
bad = 0
for t, ok in C:
    print(('  ✅ ' if ok else '  ❌ ') + t)
    if not ok: bad += 1
print('-' * 66)
print('মোট %d টার মধ্যে পাশ %d · ব্যর্থ %d' % (len(C), len(C) - bad, bad))
if bad:
    print('⛔ FAIL'); sys.exit(1)
print('✅ PASS — দুই জায়গাতেই সমানভাবে বসানো আছে।')
