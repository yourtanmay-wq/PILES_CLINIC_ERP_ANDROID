# -*- coding: utf-8 -*-
"""
V584 (২৩.০৮.২০২৬) — ফোন ও কম্পিউটার মিলিয়ে দেখার পাহারা।

TK-নির্দেশ: *"অ্যান্ড্রয়েড এবং ওয়েব দু জায়গাতেই সমানভাবে করেছেন কিনা একবার
মিলিয়ে দেখুন"* — তাই এই সেশনের প্রতিটা কাজ দু'দিকেই আছে কি না, মেশিন দিয়ে
মিলিয়ে দেখা হয় (চোখে নয়)।

চালানো:  python3 00_GUARD/verify_v584_parity.py
"""
import io, os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT   = os.path.join(ROOT, '02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic')
RES  = os.path.join(ROOT, '02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/res')
WEB  = os.path.join(ROOT, '03_NETLIFY_READY')

def read(p):
    try: return io.open(p, encoding='utf-8').read()
    except Exception: return ''

A4LANG  = read(os.path.join(KT, 'clinical/CheckupA4Lang.kt'))
A4REP   = read(os.path.join(KT, 'clinical/CheckupA4Report.kt'))
A4IMG   = read(os.path.join(KT, 'clinical/CheckupAnatomyImage.kt'))
DOCACT  = read(os.path.join(KT, 'clinical/DoctorCheckupActivity.kt'))
LAYOUT  = read(os.path.join(RES, 'layout/activity_doctor_checkup.xml'))
APPJS   = read(os.path.join(WEB, 'app.js'))
CSS     = read(os.path.join(WEB, 'styles.css'))

CHECKS = []
def ck(title, ok):
    CHECKS.append((title, bool(ok)))

# ── ১. হেডার কার্ড — ID/Ref By ভিতরে, কল আইকন নেই ──
ck('হেডার মোড়ক (ক্রিম ব্যাকগ্রাউন্ড ID-সারি সহ) — ফোনে',
   'ptlHeaderCard' in LAYOUT and 'bg_ptl_header_visit' in LAYOUT)
ck('হেডার মোড়ক (ID-সারি কার্ডের ভিতরে) — ওয়েবে',
   'dnPatTop' in APPJS and 'dnPatTop' in CSS)
# ⚠️ শুধু আসল View খোঁজা হয় — ব্যাখ্যার মন্তব্যে নামটা থেকে যায় (থাকাই উচিত,
#    নইলে পরে কেউ জানবে না বোতামটা কেন সরানো হয়েছিল)।
ck('কল আইকন সরানো — ফোনে (layout)', '@+id/btnPatientCall' not in LAYOUT)
ck('কল আইকন সরানো — ফোনে (কোড)', 'R.id.btnPatientCall' not in DOCACT)

# ── ২. Check-up History পপ-আপ ──
ck('Check-up History — ফোনে', 'openCheckupHistory' in DOCACT and 'showCheckupHistoryDialog' in DOCACT)
ck('Check-up History — ওয়েবে', 'wlv1CheckupHistory' in APPJS and 'dnHistBtn' in APPJS)
ck('চেকআপ না হলে ওয়ার্নিং — ফোনে', 'চেক-আপ এখনো সম্পূর্ণ হয়নি' in DOCACT)
ck('চেকআপ না হলে ওয়ার্নিং — ওয়েবে', 'চেক-আপ এখনো সম্পূর্ণ হয়নি' in APPJS)
for label in ('View', 'A4 Print', 'Send on WhatsApp'):
    ck('বোতাম ইংরেজিতে "%s" — ফোনে' % label, '"%s"' % label in DOCACT)

# ── ৩. দুই ভাষার অভিধান ──
ck('অভিধান ফাইল আছে — ফোনে', 'object CheckupA4Lang' in A4LANG)
ck('অভিধান আছে — ওয়েবে', 'WLV1_A4_EN_MAP' in APPJS)
SAMPLE = ['পায়ুপথে রক্তপাত', 'টকটকে লাল', 'দপদপ করা', 'ডায়াবেটিস',
          'দৈনিক জল পানের পরিমাণ', 'সারাক্ষণ একটানা থাকে', 'দেখা যায় না']
for w in SAMPLE:
    ck('অভিধানে "%s" — দু জায়গাতেই' % w, (w in A4LANG) and (w in APPJS))
for key in ('sec1','sec2','sec3','sec4','sec5','sec6','sec7','sec8'):
    ck('সেকশন শিরোনাম %s — দু জায়গাতেই' % key,
       ('"%s" to ' % key) in A4LANG and (key + ':') in APPJS)
ck('ভাষা মনে রাখা — ফোনে', 'v584_a4' in DOCACT)
ck('ভাষা মনে রাখা — ওয়েবে', 'wlv1A4Lang' in APPJS)

# ── ৪. A4 রিপোর্ট — এক পাতা, নতুন সেকশন, ক্লিনিক্যাল ফটো বাদ ──
ck('ক্লিনিক্যাল ফটোগ্রাফ বাদ — ফোনে', 'CLINICAL PHOTOGRAPHS' not in A4REP)
ck('ক্লিনিক্যাল ফটোগ্রাফ বাদ — ওয়েবে', 'CLINICAL PHOTOGRAPHS' not in APPJS)
ck('পাশাপাশি সাজ (.two.btm) — ফোনে', '.two.btm{align-items:stretch' in A4REP)
ck('পাশাপাশি সাজ (.two.btm) — ওয়েবে', '.two.btm{align-items:stretch' in APPJS)
for css in ('.sec{margin-top:5px', '.cell{width:50%;padding:4.5px 12px',
            '.pbox{flex:1;min-height:170px'):
    ck('একই CSS "%s" — দু জায়গাতেই' % css.split('{')[0], (css in A4REP) and (css in APPJS))
ck('ভাগ ৬-এর ছবি — ফোনে', 'CheckupAnatomyImage' in A4IMG and 'anatomyImage' in A4REP)
ck('ভাগ ৬-এর ছবি — ওয়েবে', 'wlv1A4AnatImage' in APPJS)
ck('ছবি JPEG-এ (হালকা) — ফোনে', 'CompressFormat.JPEG' in A4IMG)
ck("ছবি JPEG-এ (হালকা) — ওয়েবে", "toDataURL('image/jpeg'" in APPJS)
ck('হেডার সবসময় ইংরেজি — ফোনে', 'DOCTOR CHECK-UP RECORD' in A4REP and '<b>Name</b>' in A4REP)
ck('হেডার সবসময় ইংরেজি — ওয়েবে', 'DOCTOR CHECK-UP RECORD' in APPJS and '<b>Name</b>' in APPJS)
ck('ডিফল্ট ভাষা English (পুরোনো ডাক ভাঙে না) — ফোনে',
   'lang: String = CheckupA4Lang.EN' in A4REP)
ck('ডিফল্ট ভাষা English (পুরোনো ডাক ভাঙে না) — ওয়েবে',
   'WLV1_A4_EN;' in APPJS or ':WLV1_A4_EN;' in APPJS.replace(' ', ''))

# ── ৫. ফ্রি-প্লান — নতুন কোনো ক্লাউড কল যেন না ঢোকে ──
ck('Check-up History-তে নতুন Supabase কল নেই — ফোনে',
   'SupabaseClient' not in DOCACT[DOCACT.find('private fun openCheckupHistory'):
                                  DOCACT.find('private fun showCheckupReportView')])
ck('Check-up History-তে নতুন ক্লাউড কল নেই — ওয়েবে',
   'supa' not in APPJS[APPJS.find('function wlv1CheckupHistory'):
                       APPJS.find('window["wlv1CheckupHistory"]')].lower())

# ── ছাপা ──
print('=' * 66)
print('🛡️  V584 — ফোন ও কম্পিউটার মিলিয়ে দেখার পাহারা')
print('=' * 66)
bad = 0
for t, ok in CHECKS:
    print(('  ✅ ' if ok else '  ❌ ') + t)
    if not ok: bad += 1
print('-' * 66)
print('মোট %d টার মধ্যে পাশ %d · ব্যর্থ %d' % (len(CHECKS), len(CHECKS) - bad, bad))
if bad:
    print('⛔ FAIL — উপরের ❌ গুলো আগে ঠিক করুন।')
    sys.exit(1)
print('✅ PASS — এই সেশনের সব কাজ দু জায়গাতেই সমানভাবে আছে।')
