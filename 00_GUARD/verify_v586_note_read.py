# -*- coding: utf-8 -*-
"""
🛡️ V586 (২৩.০৮.২০২৬) — চেক-আপের লেখা দু'জায়গাতেই পড়া যায় কি না, তার পাহারা।

TK-এর সিদ্ধান্ত: *"পড়ার কোড দু'রকম বুঝুক"*।

চালানো:  python3 00_GUARD/verify_v586_note_read.py
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT   = os.path.join(ROOT, '02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/clinical')
WEB  = os.path.join(ROOT, '03_NETLIFY_READY')
SQLD = os.path.join(ROOT, '04_SUPABASE_DATABASE_SETUP')

def read(p):
    try: return io.open(p, encoding='utf-8').read()
    except Exception: return ''

REP = read(os.path.join(KT, 'CheckupA4Report.kt'))
JS  = read(os.path.join(WEB, 'app.js'))
RT  = read(os.path.join(SQLD, 'V586_REALTIME_CHOBI_CHARA_CHALU_2026-08-23.sql'))

C = []
def ck(t, ok): C.append((t, bool(ok)))

# ── ১. দুই বিভাজকই চেনা হয় ──
ck('দুই বিভাজক ("; " ও " | ") চেনা হয়', 'private val SEPS = listOf("; ", " | ")' in REP)
ck('ঘরের শেষ চেনা হয় লেবেল দেখে (ভিতরের ";" ভুল বোঝায় না)', 'KNOWN_LABELS' in REP and 'note.startsWith("$it: ", after)' in REP)

# ── ২. লেবেলের বিকল্প নাম (ফোন বনাম ওয়েব) ──
for a, b in (('Internal Piles Grade', 'Grade'), ('Investigation', 'Investigations'), ('Est Cost', 'Financial')):
    ck('লেবেলের বিকল্প "%s" / "%s"' % (a, b), ('field("%s", "%s")' % (a, b)) in REP)

# ── ৩. ওয়েবের সেভে ভাগ ২ · ৩ · ৪ ──
for lab, fn in (('Patient Reported', 'wlv1SymReadable'), ('History Detail', 'wlv1HistReadable'), ('Habits', 'wlv1LifeReadable')):
    ck('ওয়েবের সেভে "%s" যায়' % lab, ('`%s: ${%s(' % (lab, fn)) in JS)
ck('ফোনের লেবেলের সাথে হুবহু মিল', all(('"%s"' % l) in REP for l in ('Patient Reported', 'History Detail', 'Habits')))

# ── ৪. Realtime — ছবি বাদ ──
ck('Realtime SQL আছে', bool(RT.strip()))
ck('ছবির ঘর তিনটে বাদ দেওয়া আছে',
   "when 'patients'  then array['photo']" in RT and
   "when 'followups' then array['photo']" in RT and
   "when 'medical'   then array['photos']" in RT)
ck('trash স্ট্রিম হয় না', "'trash'" not in RT.split('tabs  text[] :=')[1].split('];')[0] if 'tabs  text[] :=' in RT else False)
ck('REPLICA IDENTITY ছোঁয়া হয়নি (পুরনো সারি ছবিসহ যেত)', 'replica identity' not in RT.lower().replace('replica identity ছোঁয়া', ''))
ck('ঘরের নাম ডেটাবেস থেকে পড়া (আন্দাজে লেখা নয়)', 'information_schema.columns' in RT)
ck('ওয়েবের realtime কোড এক সারিই বসায় (নতুন ডাউনলোড নয়)',
   'postgres_changes' in JS and 'mergeById([].concat(protectedRows(t),one)' in JS)

print('=' * 66)
print('🛡️  V586 — চেক-আপের লেখা পড়া + Realtime (ছবি বাদ)')
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
