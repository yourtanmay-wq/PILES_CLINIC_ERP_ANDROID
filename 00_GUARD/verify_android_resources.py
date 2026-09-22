# -*- coding: utf-8 -*-
"""
🛡️ Android রিসোর্স-পাহারা (V584, ২৩.০৮.২০২৬)

TK-নির্দেশ (বারবার): *"অ্যান্ড্রয়েড স্টুডিওতে বিল্ড করার সময় কোন প্রকার যেন
এরার না আসে"*।

`verify_kotlin_compile.py` শুধু **Kotlin**-এর ভুল ধরে — resource ধরে না।
অথচ layout থেকে একটা `@+id` সরালে বা drawable-এর নাম ভুল লিখলে Android
Studio-তে বিল্ড **তখনই ভাঙে** ("Unresolved reference: btnX")। এই পাহারা ঠিক
সেই শ্রেণির ভুলটাই আগেই ধরে ফেলে।

কী মেলানো হয়:
  ১) কোডের প্রতিটা `R.id.X` — কোনো layout/menu-তে `@+id/X` আছে কি না
  ২) কোডের প্রতিটা `R.layout.X` · `R.drawable.X` · `R.menu.X` · `R.raw.X` — ফাইল আছে কি না
  ৩) কোডের প্রতিটা `R.string.X` · `R.color.X` · `R.style.X` · `R.array.X` — values-এ আছে কি না
  ৪) XML-এর প্রতিটা `@drawable/X` · `@layout/X` · `@color/X` · `@style/X` · `@string/X`
     · `@menu/X` · `@anim/X` — আছে কি না  (`@android:...` বাদ)

চালানো:  python3 00_GUARD/verify_android_resources.py
"""
import io, os, re, sys, glob, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP  = os.path.join(ROOT, '02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main')
RES  = os.path.join(APP, 'res')
SRC  = os.path.join(APP, 'java')

def read(p):
    try: return io.open(p, encoding='utf-8', errors='ignore').read()
    except Exception: return ''

def lib_style(name, local_styles):
    """লাইব্রেরির style (Material/AppCompat) — res-এ থাকে না, থাকার কথাও নয়।
    নিয়ম: নামে ডট থাকলে তার **প্রথম টুকরোটা** প্রকল্পে সংজ্ঞায়িত কি না দেখা হয়।
    `AppTheme.NoBar` → 'AppTheme' প্রকল্পের ⇒ যাচাই হয়।
    `Widget.MaterialComponents.Button` → 'Widget' প্রকল্পের নয় ⇒ লাইব্রেরির, বাদ।
    ⚠️ সৎ সীমা: লাইব্রেরির কোনো style-এর নাম ভুল লিখলে এই পাহারা ধরবে না
       (res-এ ওটা নেই বলে বোঝার উপায়ও নেই) — kotlinc/aapt2-ই ধরবে।"""
    if '.' not in name:
        return False
    root = name.split('.')[0]
    return not any(st == root or st.startswith(root + '.') for st in local_styles)


def walk(base, exts):
    for d, _, fs in os.walk(base):
        for f in fs:
            if f.lower().endswith(exts):
                yield os.path.join(d, f)

# ── যা যা আছে ──────────────────────────────────────────────────────────
have = collections.defaultdict(set)

for p in walk(RES, ('.xml',)):
    rel = os.path.relpath(p, RES).replace('\\', '/')
    folder, fname = rel.split('/', 1)
    kind = folder.split('-')[0]           # layout-land → layout
    stem = fname[:-4]
    if kind in ('layout', 'menu', 'anim', 'animator', 'xml', 'navigation', 'transition'):
        have[kind].add(stem)
    if kind == 'drawable':
        have['drawable'].add(stem)
    if kind == 'mipmap':
        have['mipmap'].add(stem)
    # 🔵🔒 V946 (০১.০৯.২০২৬ — নিজের অডিটে ধরা): `res/color/xxx.xml` (রং-সিলেক্টর)
    #    গোনা হত না, তাই `R.color.chip_bg_selector` "নেই" বলে ভুল FAIL দিত।
    #    Android-এ এগুলো সম্পূর্ণ বৈধ রং-রিসোর্স। ⛔ শুধু যোগ, কিছু বাদ যায়নি।
    if kind == 'color':
        have['color'].add(stem)
    txt = read(p)
    # layout/menu-র ভিতরের সব `@+id/...`
    for m in re.finditer(r'@\+id/([A-Za-z_][A-Za-z0-9_]*)', txt):
        have['id'].add(m.group(1))
    # values/*.xml-এর সব সংজ্ঞা
    if kind == 'values':
        for m in re.finditer(r'<(string|color|style|dimen|integer|bool|string-array|integer-array|array|declare-styleable)\s[^>]*name="([^"]+)"', txt):
            tag, nm = m.group(1), m.group(2)
            key = {'string-array': 'array', 'integer-array': 'array'}.get(tag, tag)
            have[key].add(nm)
        for m in re.finditer(r'<item\s[^>]*type="id"[^>]*name="([^"]+)"', txt):
            have['id'].add(m.group(1))

# ছবি/অডিও ইত্যাদি — যেগুলো xml নয়
for p in walk(RES, ('.png', '.jpg', '.jpeg', '.webp', '.gif', '.9.png', '.mp3', '.ogg', '.wav', '.ttf', '.otf', '.json')):
    rel = os.path.relpath(p, RES).replace('\\', '/')
    folder, fname = rel.split('/', 1)
    kind = folder.split('-')[0]
    stem = re.sub(r'\.(9\.png|png|jpg|jpeg|webp|gif|mp3|ogg|wav|ttf|otf|json)$', '', fname, flags=re.I)
    have[kind].add(stem)

# ── কোড যা চায় ─────────────────────────────────────────────────────────
def strip_comments_keep_lines(src):
    """কমেন্ট মুছে দেয়, কিন্তু **লাইন-সংখ্যা অটুট রাখে** (নিউলাইন রেখে দেয়),
    যাতে ভুল ধরা পড়লে লাইন নম্বরটা সত্যি থাকে।

    🔴 কেন দরকার (২৭.০৮.২০২৬-এ ধরা): `DoctorCheckupActivity.kt`-এ V600-এর
    ব্যাখ্যা-কমেন্টে `R.id.etHistoryNote` লেখা ছিল — আসল কোডে নয়। এই যন্ত্র
    কমেন্টও পড়ত, তাই **সবসময় FAIL** দেখাত ⇒ যন্ত্রটাই অকেজো হয়ে পড়েছিল,
    আর সত্যিকারের কোনো ভুল ধরা পড়লেও আলাদা করা যেত না।
    ⛔ স্ট্রিং-এর ভিতরের `//` যেন কমেন্ট না ধরা হয়, সেটাও দেখা হয়।
    """
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':                      # স্ট্রিং — হুবহু রেখে দিই
            out.append(c); i += 1
            while i < n:
                if src[i] == '\\' and i + 1 < n:
                    out.append(src[i]); out.append(src[i + 1]); i += 2; continue
                out.append(src[i])
                if src[i] == '"' or src[i] == '\n':
                    i += 1; break
                i += 1
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            while i < n and src[i] != '\n':
                i += 1
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '*':
            i += 2
            while i + 1 < n and not (src[i] == '*' and src[i + 1] == '/'):
                if src[i] == '\n':
                    out.append('\n')
                i += 1
            i += 2
            continue
        out.append(c); i += 1
    return ''.join(out)


BAD = []
KINDS = ('id', 'layout', 'drawable', 'mipmap', 'menu', 'string', 'color',
         'style', 'dimen', 'array', 'raw', 'anim', 'font', 'integer', 'bool')

for p in walk(SRC, ('.kt', '.java')):
    # ⛔ কমেন্ট বাদ দিয়ে তবেই খোঁজা — নইলে ব্যাখ্যা-কমেন্টে লেখা `R.id.…`-ও
    #    ভুল বলে ধরা পড়ত (আসল ঘটনা: V600-এর কমেন্ট, ২২.০৮.২০২৬ থেকে)।
    txt = strip_comments_keep_lines(read(p))
    # `android.R.id.x` বাদ — ওটা সিস্টেমের
    for m in re.finditer(r'(?<![\w.])(android\.)?R\.(\w+)\.([A-Za-z_][A-Za-z0-9_]*)', txt):
        if m.group(1):        # android.R.* — সিস্টেমের, বাদ
            continue
        kind, name = m.group(2), m.group(3)
        if kind not in KINDS:
            continue
        pool = have[kind] | (have['mipmap'] if kind == 'drawable' else set())
        if name not in pool:
            if kind == 'style' and lib_style(name, have['style']):
                continue
            line = txt[:m.start()].count('\n') + 1
            BAD.append('%s:%d — R.%s.%s নেই res-এ' % (os.path.relpath(p, ROOT), line, kind, name))

XMLKINDS = ('drawable', 'layout', 'color', 'style', 'string', 'menu', 'anim',
            'dimen', 'array', 'integer', 'bool', 'mipmap', 'font')
for p in walk(RES, ('.xml',)):
    txt = read(p)
    for m in re.finditer(r'"@(?!\+|android:)(\w+)/([A-Za-z_][A-Za-z0-9_.]*)"', txt):
        kind, name = m.group(1), m.group(2)
        if kind not in XMLKINDS:
            continue
        pool = have[kind] | (have['mipmap'] if kind == 'drawable' else set())
        if name not in pool:
            if kind == 'style' and lib_style(name, have['style']):
                continue
            line = txt[:m.start()].count('\n') + 1
            BAD.append('%s:%d — @%s/%s নেই res-এ' % (os.path.relpath(p, ROOT), line, kind, name))

# ── 🐞🔒 V1240 (০৮.০৯.২০২৬) — layout XML-এর ভাঁজ ঠিক আছে তো? ──────────
#
# TK ভিডিও পাঠিয়ে দেখালেন: ডাক্তারের ফোনে ☰ চাপলেই অ্যাপ বন্ধ হয়ে হোম
# স্ক্রিনে ফিরে আসে। **আসল কারণ (আমারই ভুল, V1212):** একটা নতুন সারি
# বসানোর সময় বন্ধ-ট্যাগ ভুল জায়গায় পড়ায় `ScrollView`-এর ভিতরে **দুটো
# সরাসরি ঘর** হয়ে গিয়েছিল। Android-এ ScrollView-এ একটাই ঘর থাকতে পারে —
# তাই পর্দা তৈরি হওয়ার মুহূর্তেই অ্যাপ ভেঙে পড়ত।
#
# ⚠️ এই দোষ **কোনো পাহারাতেই ধরা পড়েনি** — XML ভাঙা ছিল না (ট্যাগ মিলে
#    যাচ্ছিল), রিসোর্সও সব ঠিক ছিল। তাই এখন থেকে এই দুটোও দেখা হবে:
#      ① প্রতিটা layout ফাইল সত্যিই পড়া যায় কিনা (ট্যাগের ভাঁজ ঠিক আছে কিনা)
#      ② ScrollView / HorizontalScrollView / NestedScrollView-এর ভিতরে
#         একটাই সরাসরি ঘর আছে কিনা
# ⛔ শুধু পড়া — কোনো ফাইল বদলায় না।
try:
    import xml.etree.ElementTree as _ET
    _SINGLE = ('ScrollView', 'HorizontalScrollView', 'NestedScrollView')
    for _p in sorted(glob.glob(os.path.join(RES, 'layout*', '*.xml'))):
        _rel = os.path.relpath(_p, ROOT)
        try:
            _root = _ET.parse(_p).getroot()
        except Exception as _e:
            BAD.append('%s — XML-এর ভাঁজ ভাঙা: %s' % (_rel, _e))
            continue
        _stack = [_root]
        while _stack:
            _n = _stack.pop()
            _t = _n.tag.split('.')[-1]
            _kids = list(_n)
            if _t in _SINGLE and len(_kids) > 1:
                BAD.append('%s — %s-এর ভিতরে %d টা সরাসরি ঘর; Android-এ একটাই '
                           'থাকতে পারে (পর্দা খুললেই অ্যাপ ভাঙবে)'
                           % (_rel, _t, len(_kids)))
            _stack.extend(_kids)
except Exception:
    pass

# ── ছাপা ───────────────────────────────────────────────────────────────
print('=' * 66)
print('🛡️  Android রিসোর্স-পাহারা — বিল্ড ভাঙার আগেই ধরা')
print('=' * 66)
print('   res-এ পাওয়া গেল: id=%d · layout=%d · drawable=%d · string=%d · color=%d'
      % (len(have['id']), len(have['layout']), len(have['drawable']),
         len(have['string']), len(have['color'])))
if BAD:
    print('-' * 66)
    for b in BAD[:60]:
        print('  ❌ ' + b)
    if len(BAD) > 60:
        print('  … আরও %d টা' % (len(BAD) - 60))
    print('-' * 66)
    print('⛔ FAIL — এই %d টা রিসোর্স নেই, Android Studio-তে বিল্ড ভাঙবে।' % len(BAD))
    sys.exit(1)
print('-' * 66)
print('✅ PASS — কোড ও XML যত রিসোর্স চায়, সবগুলোই res-এ আছে।')
