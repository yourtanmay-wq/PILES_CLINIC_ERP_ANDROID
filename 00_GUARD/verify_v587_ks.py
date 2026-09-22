# -*- coding: utf-8 -*-
"""
🛡️ V587 (২৩.০৮.২০২৬) — ঘড়ির আঁকা বাদ + ক্ষারসূত্রের ধাপ, দু'জায়গাতেই।

TK: *"ঘড়ি আকানো থাকবে না"* · *"ক্ষার সূত্র দিয়ে বেঁধে দিব, সেটাও যেন
অ্যানিমেশন করে দেখানো যায়"*।

চালানো:  python3 00_GUARD/verify_v587_ks.py
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT   = os.path.join(ROOT, '02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/clinical')
WEB  = os.path.join(ROOT, '03_NETLIFY_READY')

def read(p):
    try: return io.open(p, encoding='utf-8').read()
    except Exception: return ''

KS   = read(os.path.join(KT, 'KsharSutraAnim.kt'))
VIEW = read(os.path.join(KT, 'AnatomyView.kt'))
ACT  = read(os.path.join(KT, 'DoctorCheckupActivity.kt'))
JS   = read(os.path.join(WEB, 'app.js'))
CSS  = read(os.path.join(WEB, 'styles.css'))

def nocomment_kt(t):
    return re.sub(r'//[^\n]*', '', re.sub(r'/\*.*?\*/', '', t, flags=re.S))
def nocomment_js(t):
    return re.sub(r'/\*.*?\*/', '', t, flags=re.S)

VIEWC, JSC = nocomment_kt(VIEW), nocomment_js(JS)

C = []
def ck(t, ok): C.append((t, bool(ok)))

# ── ১. ঘড়ির আঁকা আর নেই (হিসাব আছে) ──
ck('ঘড়ির বলয় আর আঁকা হয় না — ফোনে', 'clockCentre?.let' not in VIEWC)
ck('ঘড়ির বলয় আর আঁকা হয় না — ওয়েবে',
   'function wlv1AnatDrawClock(){' in JS and 'ctx.arc(cx,cy,rr' not in JSC)
ck('ঘড়ির হিসাব অটুট — ফোনে', 'AnatomyClock.hourAt(' in VIEWC)
ck('ঘড়ির হিসাব অটুট — ওয়েবে', 'function wlv1AnatHourOf(' in JS)
ck('কেন্দ্র জমা রাখা অটুট — ফোনে', 'AnatomyClock.centreOf(' in VIEWC)
ck('কেন্দ্র জমা রাখা অটুট — ওয়েবে', 'function wlv1AnatCentreOf(' in JS)

# ── ২. ধাপের ক্রম দু'জায়গায় এক ──
ck('ধাপের সংজ্ঞা — ফোনে', 'object KsharSutraAnim' in KS)
ck('ধাপের সংজ্ঞা — ওয়েবে', 'WLV1_KS_STEP' in JS)
for nm, val in (('LUMP_DRAWN', 1), ('LUMP_INJECT', 2), ('LUMP_SWELL', 3),
                ('LUMP_TIE', 4), ('LUMP_FALL', 5),
                ('TRACT_DRAWN', 11), ('TRACT_LACE', 12), ('TRACT_TIE', 13), ('TRACT_CUT', 14)):
    # ⚠️ ফাঁকা জায়গার সংখ্যা মেলানো হয় না — শুধু নাম ও মান
    ck('ধাপ %s = %d — দু জায়গাতেই' % (nm, val),
       re.search(r'const val %s\s*=\s*%d\b' % (nm, val), KS) is not None
       and ('%s:%d' % (nm, val)) in JS.replace(' ', ''))

# ── ৩. ইনজেকশন বাদ দেওয়া যায় ──
ck('ইনজেকশন বাদ দেওয়া যায় — ফোনে', 'withInjection' in KS and 'ksWithInjection' in ACT)
ck('ইনজেকশন বাদ দেওয়া যায় — ওয়েবে', 'wlv1KsInjToggle' in JS)

# ── ৪. আঁকার অংশ দু'জায়গাতেই ──
for a, b, t in (('drawThread', 'wlv1KsThread', 'সুতো বাঁধা'),
                ('drawNeedle', 'wlv1KsNeedle', 'ইনজেকশনের সুচ'),
                ('halfWidthAt', 'wlv1KsHalfWidth', 'গায়ের চওড়া মাপা')):
    ck('%s — দু জায়গাতেই' % t, ('fun %s' % a) in KS and ('function %s' % b) in JS)
ck('নালী বরাবর সুতো — ফোনে', 'ksTractThread' in VIEWC)
ck('নালী বরাবর সুতো — ওয়েবে', 'function wlv1KsTract(' in JS)

# ── ৫. বাছাই ও পুরো পর্দা ──
ck('ছুঁয়ে বাছাই — ফোনে', 'fun ksNearestAt' in VIEW)
ck('ছুঁয়ে বাছাই — ওয়েবে', 'function wlv1KsNearest(' in JS)
ck('একই দূরত্বের সীমা (18) — দু জায়গাতেই',
   'bd <= 18.0' in VIEWC and 'bd<=18?' in JSC.replace(' ', ''))
ck('পুরো পর্দায় 🧵 বোতাম — ফোনে', 'roundBtn("\U0001F9F5")' in ACT)
ck('পুরো পর্দায় 🧵 বোতাম — ওয়েবে', 'wlv1KsStart()">\U0001F9F5<' in JS)
ck('ধাপের সারির CSS — ওয়েবে', '.wlv1KsBox{' in CSS)

# ── ৬. নিরাপত্তা: KS মোডে আঁকা যায় না, কিছু সেভ হয় না ──
ck('KS মোডে নতুন দাগ পড়ে না — ফোনে', 'if (ksOn) {' in VIEWC)
ck('KS মোডে নতুন দাগ পড়ে না — ওয়েবে', 'if(WLV1_KS.on){' in JSC.replace(' ', ''))
ck('KS-এর কিছু সেভ হয় না — ফোনে', 'ksOn' not in nocomment_kt(read(os.path.join(KT, 'AnatomyModel.kt'))))

print('=' * 66)
print('🛡️  V587 — ঘড়ির আঁকা বাদ + ক্ষারসূত্রের ধাপ')
print('=' * 66)
bad = 0
for t, ok in C:
    print(('  ✅ ' if ok else '  ❌ ') + t)
    if not ok: bad += 1
print('-' * 66)
print('মোট %d টার মধ্যে পাশ %d · ব্যর্থ %d' % (len(C), len(C) - bad, bad))
if bad:
    print('⛔ FAIL'); sys.exit(1)
print('✅ PASS')
