# -*- coding: utf-8 -*-
"""
🛡️ V595 পাহারাদার (২৩.০৮.২০২৬) — **ডায়েট চার্টে সবগুলো আগে থেকেই টিক**।

TK: *"সবগুলো ঠিক মারা থাকবে · আমি চাইলে untick করতে পারি · তারপর ফাইনাল
     প্রিন্ট আউট হবে"*
"""
import io, os, re, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
K = ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/'
dc = io.open(K + 'clinical/DietChartActivity.kt', encoding='utf-8').read()
cm = io.open(K + 'clinical/ClinicalModels.kt', encoding='utf-8').read()
cr = io.open(K + 'clinical/ClinicalRepository.kt', encoding='utf-8').read()
js = io.open(ROOT + '/03_NETLIFY_READY/app.js', encoding='utf-8').read()

ok, bad = [], []
def check(n, c, d=''): (ok if c else bad).append((n, d))

# ── ১ · ফোনে দুই তালিকাতেই টিক দেওয়া অবস্থায় বসে ─────────────────────
check('[১] Allowed তালিকায় আগে থেকেই টিক',
      'DietEntry(name = it, category = "Allowed", isSelected = true)' in dc)
check('[১] Avoid তালিকাতেও আগে থেকেই টিক',
      'DietEntry(name = it, category = "Avoid", isSelected = true)' in dc)
check('[১] পর্দার ঘরে ওই মানটাই বসে', 'cb.isChecked = entry.isSelected' in dc)
check('[১] তুলে দিলে সঙ্গে সঙ্গে মনে রাখে',
      'cb.setOnCheckedChangeListener { _, isChecked -> entry.isSelected = isChecked }' in dc)

# ── ২ · ছাপা/সেভ/শেয়ার — শুধু টিক-দেওয়াগুলোই ────────────────────────
check('[২] সেভ ও ছাপা শুধু টিক-দেওয়া নেয়',
      dc.count('ClinicalRepository.currentDiet.filter { it.isSelected }') == 2,
      'পাওয়া গেল %d বার' % dc.count('ClinicalRepository.currentDiet.filter { it.isSelected }'))
for f in ('print/DietChartHtmlPrint.kt', 'print/PrintMappers.kt'):
    t = io.open(K + f, encoding='utf-8').read()
    check('[২] %s-ও শুধু টিক-দেওয়া নেয়' % f.split('/')[-1],
          'filter { it.isSelected }' in t)

# ── ৩ · ঝুঁকি বন্ধ — অন্য কিছু বদলায়নি ───────────────────────────────
check('[৩] DietEntry-র নিজের ডিফল্ট আগের মতোই false',
      'var isSelected: Boolean = false' in cm)
check('[৩] রোগী বদলালে তালিকা মুছে যায় (আগের বাছাই যায় না)',
      'currentDiet.clear()' in cr)
check('[৩] "Print without Patient"-এর আলাদা তালিকাটা ছোঁয়া হয়নি',
      '<label class="dietOpt"><input type="checkbox" class="dt" value=' in js)

# ── ৪ · ওয়েবেও একই ─────────────────────────────────────────────────
check('[৪] ওয়েবে আগে থেকেই টিক',
      '<label class="wlv1DietItem"><input type="checkbox" class="dt" checked value=' in js)
check('[৪] ওয়েবে সেভ/ছাপা শুধু টিক-দেওয়া পড়ে',
      ".dt:checked" in js)

# ── ৫ · ভার্সন ─────────────────────────────────────────────────────
gr = io.open(ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts', encoding='utf-8').read()
vj = io.open(ROOT + '/03_NETLIFY_READY/version.json', encoding='utf-8').read()
_g = re.search(r'val appVersionCode = (\d+)', gr); _v = re.search(r'"versionCode": (\d+)', vj)
check('[৫] ভার্সন V595 বা তার পরে',
      _g and _v and int(_g.group(1)) >= 595 and int(_v.group(1)) >= 595,
      'gradle=%s · version.json=%s' % (_g and _g.group(1), _v and _v.group(1)))

print('🛡️ V595 পাহারাদার — ডায়েট চার্টে সবগুলো আগে থেকেই টিক')
print('=' * 66)
for n, d in ok:  print('  ✅ ' + n + (' · ' + d if d else ''))
for n, d in bad: print('  ❌ ' + n + (' · ' + d if d else ''))
print('-' * 66); print('  পাশ: %d / %d' % (len(ok), len(ok) + len(bad)))
if bad: print('\nFAIL — V595 ❌'); sys.exit(1)
print('\nPASS — সবগুলো টিক দেওয়া, তুলে দিলে কাগজে যায় না ✅')
