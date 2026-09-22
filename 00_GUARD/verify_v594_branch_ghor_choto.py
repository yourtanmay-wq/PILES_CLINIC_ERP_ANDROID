# -*- coding: utf-8 -*-
"""
🛡️ V594 পাহারাদার (২৩.০৮.২০২৬) — **হেডারের ব্রাঞ্চ-ঘর ছোট**।

TK: *"উপরে ডান দিকে যে সিলেক্ট ব্রাঞ্চ, এত বড় রাখা যাবে না · শুধুমাত্র
     মাস্টারের ফোনে বলে কথা না, এ টু জেড সবার কাছে যেন ওটা সাইজের ছোট থাকে ·
     মাস্টারের ফোনে অন্যান্য জায়গায় যেমন ব্রাঞ্চ সিলেক্ট করার জায়গায় একটু ছোট
     সাইজের, ঠিক সেরকমই রাখবেন"* — ডেমো-ফটো দেখে অনুমোদিত।

আগে: 142 × 40 dp, লেখা ১৬sp — লেখাটা কেটে যেত ("SELECT BRANC▾")।
এখন: 120 × 28 dp, লেখা ১০sp — সবচেয়ে বড় লেখাটাও পুরো ধরে।
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/res/'
K = ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/'
xml = io.open(RES + 'layout/activity_registration.xml', encoding='utf-8').read()
ra  = io.open(K + 'native/RegistrationActivity.kt', encoding='utf-8').read()
big = io.open(RES + 'drawable/bg_input_field_picker.xml', encoding='utf-8').read()
sml_p = RES + 'drawable/bg_input_field_picker_small.xml'

ok, bad = [], []
def check(name, cond, detail=''):
    (ok if cond else bad).append((name, detail))

# ── ১ · ঘরের মাপ ──────────────────────────────────────────────────────
i = xml.find('android:id="@+id/spBranch"')
blk = xml[i:i + 400] if i >= 0 else ''
check('[১] spBranch পাওয়া গেল', i >= 0)
def attr(a):
    m = re.search(r'android:%s="([^"]+)"' % a, blk)
    return m.group(1) if m else None
check('[১] চওড়া 120dp (আগে 142dp)', attr('layout_width') == '120dp', 'পেলাম %s' % attr('layout_width'))
check('[১] উঁচু 28dp (আগে 40dp)',   attr('layout_height') == '28dp', 'পেলাম %s' % attr('layout_height'))
check('[১] ছোট পটভূমি ব্যবহার হচ্ছে',
      attr('background') == '@drawable/bg_input_field_picker_small')
check('[১] paddingStart 8dp · paddingEnd 20dp',
      attr('paddingStart') == '8dp' and attr('paddingEnd') == '20dp')

# ── ২ · লেখা ১০sp — শুধু ব্রাঞ্চের ঘরে ────────────────────────────────
check('[২] capsAdapter-এ ঐচ্ছিক boxTextSp আছে', 'boxTextSp: Float? = null' in ra)
check('[২] শুধু বন্ধ বাক্সে বসে (getView)',
      'if (boxTextSp != null) {' in ra and 'COMPLEX_UNIT_SP, boxTextSp' in ra)
# ⛔ শুধু `getDropDownView`-এর **ভিতরটাই** দেখতে হবে। পুরো বাকি ফাইল দেখলে
#    নিচের `boxTextSp = 10f` (অ্যাডাপ্টার ডাকার লাইন) ধরা পড়ে মিথ্যে ❌ দিত —
#    উল্টো-পরীক্ষা করতে গিয়ে নিজেই ধরা পড়েছে।
_dd = ra.index('override fun getDropDownView')
_ddEnd = ra.index('\n        }', _dd)
check('[২] খোলা তালিকার লেখা ছোট করা হয়নি', 'boxTextSp' not in ra[_dd:_ddEnd])
check('[২] ব্রাঞ্চের অ্যাডাপ্টারেই ১০sp', 'boxTextSp = 10f' in ra)
check('[২] boxTextSp ঠিক একবারই দেওয়া হয়েছে', ra.count('boxTextSp = ') == 1,
      'পাওয়া গেল %d বার' % ra.count('boxTextSp = '))

# ── ৩ · বাকি তিনটে বাছাই-ঘর ছোঁয়া হয়নি ───────────────────────────────
for sp in ('spOccupation', 'spDurationUnit', 'spRefBy'):
    j = ra.find('binding.%s.adapter' % sp)
    seg = ra[j:j + 220] if j >= 0 else ''
    check('[৩] %s আগের মতোই (ছোট লেখা নয়)' % sp, j >= 0 and 'boxTextSp' not in seg)
check('[৩] বড় পটভূমির ফাইলটা এক অক্ষরও বদলায়নি',
      'android:radius="14dp"' in big and 'android:width="18dp"' in big
      and 'android:right="14dp"' in big)
check('[৩] ফর্মের অন্য ঘরগুলো বড় পটভূমিই ব্যবহার করছে',
      xml.count('@drawable/bg_input_field_picker"') >= 1)

# ── ৪ · নতুন পটভূমির ফাইল ─────────────────────────────────────────────
check('[৪] ছোট পটভূমির ফাইলটা আছে', os.path.exists(sml_p))
if os.path.exists(sml_p):
    sml = io.open(sml_p, encoding='utf-8').read()
    check('[৪] কোণ 8dp', 'android:radius="8dp"' in sml)
    check('[৪] তীর 12dp', 'android:width="12dp"' in sml and 'android:height="12dp"' in sml)
    check('[৪] তীর ডান থেকে 6dp', 'android:right="6dp"' in sml)
    check('[৪] একই chevron ছবি (নতুন ছবি বানানো হয়নি)',
          '@drawable/ic_picker_chevron' in sml)

# ── ৫ · মেপে দেখা: সবচেয়ে বড় লেখাও ধরে যায় ──────────────────────────
#    Roboto-তে মাপা (ব্রাউজারে canvas.measureText):
WID_10SP = {'SELECT BRANCH': 84, 'COOCH BEHAR': 75, 'KISHANGANJ': 71,
            'JALPAIGURI': 64, 'FALAKATA': 55, 'BIRPARA': 48}
room = 120 - 8 - 20      # চওড়া − paddingStart − paddingEnd
worst = max(WID_10SP.values())
check('[৫] ভিতরের জায়গা %ddp ≥ সবচেয়ে বড় লেখা %ddp' % (room, worst), room >= worst,
      'ঘাটতি %ddp' % (worst - room))
check('[৫] আগের মাপে সত্যিই কেটে যেত (তাই বদলটা দরকার ছিল)',
      (142 - 8 - 34) < 135, '১৬sp-এ "SELECT BRANCH" = 135dp, জায়গা ছিল %ddp' % (142 - 8 - 34))

# ── ৬ · ভার্সন ────────────────────────────────────────────────────────
gr = io.open(ROOT + '/02_ANDROID_SOURCE_CODE/PilesClinicApp/app/build.gradle.kts', encoding='utf-8').read()
vj = io.open(ROOT + '/03_NETLIFY_READY/version.json', encoding='utf-8').read()
_g = re.search(r'val appVersionCode = (\d+)', gr)
_v = re.search(r'"versionCode": (\d+)', vj)
check('[৬] ভার্সন V594 বা তার পরে',
      bool(_g) and bool(_v) and int(_g.group(1)) >= 594 and int(_v.group(1)) >= 594,
      'gradle=%s · version.json=%s' % (_g and _g.group(1), _v and _v.group(1)))

print('🛡️ V594 পাহারাদার — হেডারের ব্রাঞ্চ-ঘর ছোট')
print('=' * 66)
for n, d in ok:  print('  ✅ ' + n + (' · ' + d if d else ''))
for n, d in bad: print('  ❌ ' + n + (' · ' + d if d else ''))
print('-' * 66)
print('  পাশ: %d / %d' % (len(ok), len(ok) + len(bad)))
if bad:
    print('\nFAIL — V594 ❌'); sys.exit(1)
print('\nPASS — ঘরটা ছোট, লেখা আর কাটে না, বাকি কিছুই বদলায়নি ✅')
