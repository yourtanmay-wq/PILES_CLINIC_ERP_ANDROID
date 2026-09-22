# -*- coding: utf-8 -*-
"""এই সেশনের প্রতিটা কাজ — ফোনে ও কম্পিউটারে এক কি না, যন্ত্র দিয়ে মেলানো।"""
import io, re, json
R='/home/user/piles-clinic-erp/'
KT=R+'02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/'
JS=io.open(R+'03_NETLIFY_READY/app.js',encoding='utf-8').read()
def kt(p): return io.open(KT+p,encoding='utf-8').read()
ok=[]; bad=[]
def check(name, cond, detail=''):
    (ok if cond else bad).append((name, detail))

# ── ১. হিন্দি তালিকা এক কি না ────────────────────────────────────────
nb=kt('native/NoBengali.kt')
i=nb.index('private val HINDI'); j=nb.index('\n    )', i)
# 🔧 V591 (২৩.০৮.২০২৬) — আগে শুধু **এক লাইনে লেখা** সারিগুলোই ধরা পড়ত।
# Kotlin-এ লম্বা সারি দু লাইনে লেখা যায় (`"…" to\n    "…"`), আর তখন এই
# পাহারাদার ওই সারিগুলো **গুনতই না** — অর্থাৎ ফোনে থাকা সত্ত্বেও "নেই" ধরত।
# এখন `to`-এর দু পাশে নতুন লাইন থাকলেও ধরা পড়ে।
ktpairs=set(re.findall(r'"((?:[^"\\]|\\.)*)"\s+to\s+"((?:[^"\\]|\\.)*)"', nb[i:j]))
a=JS.index('var WLV1_NOBN_HI=['); b=JS.index('\n];', a)
jspairs=set()
for m in re.finditer(r"\['((?:[^'\\]|\\.)*)','((?:[^'\\]|\\.)*)'\]", JS[a:b]):
    jspairs.add((m.group(1).replace("\\'","'"), m.group(2).replace("\\'","'")))
def norm(s): return s.replace('\\n','\n').replace('\\"','"').replace('\\\\','\\')
K={(norm(x),norm(y)) for x,y in ktpairs}
J={(norm(x),norm(y)) for x,y in jspairs}
check('V575 হিন্দি তালিকা — ফোন ও ওয়েবে হুবহু এক', K==J,
      'ফোনে %d · ওয়েবে %d · শুধু ফোনে %d · শুধু ওয়েবে %d'%(len(K),len(J),len(K-J),len(J-K)))

# ── ২. ফোলানোর রং এক কি না ──────────────────────────────────────────
av=kt('clinical/AnatomyView.kt')
i=av.index('private fun drawLump'); j=av.index('private fun drawMarks')
kthex=re.findall(r'parseColor\("#([0-9A-Fa-f]{6,8})"\)', av[i:j])
def rgb8(h):
    h=h.upper()
    return h[2:] if len(h)==8 else h
i2=JS.index('function bulge('); j2=JS.index('function bulgeFromDrag(', i2)
jshex=[x.upper() for x in re.findall(r'#([0-9A-Fa-f]{6})', JS[i2:j2])]
jsrgba=re.findall(r'rgba\((\d+),(\d+),(\d+),([\d.]+)\)', JS[i2:j2])
jsall=set(jshex)|{('%02X%02X%02X'%(int(r),int(g),int(bl))) for r,g,bl,_ in jsrgba}
ktall={rgb8(h) for h in kthex}
check('V581–583 ফোলানোর রং — দুই দিকে একই রং ব্যবহার', ktall==jsall,
      'ফোনে %d রং · ওয়েবে %d রং · অমিল %s'%(len(ktall),len(jsall), sorted(ktall^jsall) or 'নেই'))

# ── ৩. ফিস্টুলার দাগের মাপ ──────────────────────────────────────────
check('V583 ফিস্টুলার দাগ ১.৩৫ — ফোনে', 'Tool.TRACT) 1.35f' in av and '"#F0A400", 1.35f' in av)
check('V583 ফিস্টুলার দাগ ১.৩৫ — ওয়েবে', "'tract' ? 1.35 :" in JS)
check('V583 কালো ছায়া +০.৭ — ফোনে', 'if (dashed) 0.7f else 1.6f' in av)
check('V583 কালো ছায়া +০.৭ — ওয়েবে', "'tract' ? 0.7 : 1.6" in JS)
check('V583 কাটা-কাটা ২.৮/২.০ — ফোনে', '2.8f * s, 2.0f * s' in av)
check('V583 কাটা-কাটা ২.৮/২.০ — ওয়েবে', '[2.8 * s, 2.0 * s]' in JS)

# ── ৪. শিরার মাপ ────────────────────────────────────────────────────
check('V583 শিরা চওড়া ০.০৩২ — ফোনে', 'wide * 0.032f' in av)
check('V583 শিরা চওড়া ০.০৩২ — ওয়েবে', 'LW * 0.032' in JS)
check('V583 শিরা ৩টে — ফোনে', 'for (q in 0 until 3)' in av)
check('V583 শিরা ৩টে — ওয়েবে', 'for (var q = 0; q < 3; q++)' in JS)
check('V582 ছোপ ৪টে — ফোনে', 'for (k in 0 until 4)' in av)
check('V582 ছোপ ৪টে — ওয়েবে', 'for (var k = 0; k < 4; k++)' in JS)
check('V582 ছিটের মাপ ০.০১৪+০.০৫০ — ফোনে', '0.014 + AnatomyModel.lumpNext(state) * 0.050' in av)
check('V582 ছিটের মাপ ০.০১৪+০.০৫০ — ওয়েবে', '0.014 + lumpNext(st) * 0.050' in JS)

# ── ৫. ভাঁজ (fold) ─────────────────────────────────────────────────
xml=io.open(R+'02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/res/layout/activity_doctor_checkup.xml',encoding='utf-8').read()
dc=kt('clinical/DoctorCheckupActivity.kt')
check('V574 ভাগ ২ ভাঁজ — ফোনে', 'symptomFoldHead' in xml and 'wireSymptomFold()' in dc)
check('V574 ভাগ ২ ভাঁজ — ওয়েবে', "wlv1FoldHead('dnSymFold'" in JS)
check('V574 চারটে ইতিহাস ভাঁজ — ফোনে', 'attachFold(foldKey' in dc)
check('V574 চারটে ইতিহাস ভাঁজ — ওয়েবে', "wlv1FoldHead(fid" in JS)
check('V578 রোগ ও অভ্যাস ভাঁজ — ফোনে', 'lifeFoldHead' in xml and 'wireLifeFold()' in dc)
check('V578 রোগ ও অভ্যাস ভাঁজ — ওয়েবে', "wlv1FoldHead('dnLifeFold'" in JS)
check('V578 রোগ ও অভ্যাস শুরুতে বন্ধ — ফোনে', re.search(r'lifeFoldBody"[\s\S]{0,300}?visibility="gone"', xml) is not None)
check('V574 ভাগ ২ শুরুতে বন্ধ — ফোনে', re.search(r'symptomFoldBody"[\s\S]{0,300}?visibility="gone"', xml) is not None)

# ── ৬. দুটো বাক্স এক মাপ ───────────────────────────────────────────
css=io.open(R+'03_NETLIFY_READY/styles.css',encoding='utf-8').read()
check('V578 বাক্স ৮২×৪০ — ওয়েবে', 'width:82px!important' in css and 'height:40px!important' in css)
check('V578 বাক্স ৮২×৪০ — ফোনে', 'symDp(82), symDp(40)' in dc)
check('V574 টিক দিলে তবেই বাক্স — ফোনে', 'syncSymptomBoxes' in dc)
check('V574 টিক দিলে তবেই বাক্স — ওয়েবে', '.wlv1SymRow.on .wlv1SymWhen' in css)

# ── ৭. ঘড়ির কাঁটার তালিকা ও ফেরার বোতাম ────────────────────────────
check('V583 শুধু ঘড়ির কাঁটা — ফোনে', 'সামনে-ডান' not in dc)
check('V583 ফেরার বোতাম — ফোনে', '← ফিরে যান' in dc)
check('V583 ঘড়ির কাঁটার লেখা — ওয়েবে', 'ঘড়ির কাঁটা অনুযায়ী জায়গা' in JS)

# ── ৮. Egress ─────────────────────────────────────────────────────
ch=kt('native/ChamberAttendanceRepository.kt')
check('V577 চেম্বারে ছবি ছাড়া followups — ফোনে',
      ch.count('FOLLOWUP_COLS_CHAMBER_BOARD')>=3)
check('V579 "বদলেছে কি?" প্রশ্ন — ওয়েবে', 'wlv1CloudUnchanged' in JS and JS.count('wlv1CloudUnchanged(')>=4)
check('V513 "বদলেছে কি?" প্রশ্ন — ফোনে আগে থেকেই', 'CloudListRevalidate' in kt('native/SupabaseClient.kt'))
check('V580 Doctor Visit শুধু-বদল — ফোনে', 'fetchListRawSmartOrNull' in kt('native/DoctorVisitRepository.kt'))
check('V576 ছবির তালিকা দু-ধাপে — ফোনে', 'id=in.(' in kt('clinical/AnatomyPictureRepository.kt'))
check('V576 ছবির তালিকা দু-ধাপে — ওয়েবে', ".in('id',need)" in JS)

print('══ যাচাইয়ের ফল ══')
for n,d in ok: print('  ✅', n, ('· '+d) if d else '')
if bad:
    print('  ── অমিল ──')
    for n,d in bad: print('  ❌', n, ('· '+d) if d else '')
print('মোট %d টার মধ্যে পাশ %d · ব্যর্থ %d' % (len(ok)+len(bad), len(ok), len(bad)))
