#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛡️ TK-এর পাহারাদার  (tk_guard.py)
====================================
মালিক: TK Biswas · Maa Ayurved Piles Clinic
তৈরি: ২৮.০৭.২০২৬ · অবস্থা: 🔒 লক করা

কেন এটা আছে
------------
মালিকের নিয়ম ১১৫টার বেশি, ছয়টা ফাইলে ছড়ানো ছিল। নিয়ম লেখা থাকলেও
কেউ যাচাই করত না — তাই ভুল হত, আর মালিককে একই কথা বারবার বলতে হত।

এই পাহারাদার নিয়মগুলো **মনে রাখার** বদলে **যাচাই করে**।
একটাও যাচাই ব্যর্থ হলে ফাইল বানানো যাবে না।

কীভাবে চালাতে হয়
------------------
    python3 00_GUARD/tk_guard.py            → শুধু যাচাই
    python3 00_GUARD/tk_guard.py --release  → যাচাই + নতুন নাম ঠিক করা

⛔ মালিকের অনুমতি ছাড়া এই ফাইল বদলানো, দুর্বল করা বা কোনো যাচাই বাদ
   দেওয়া যাবে না। যাচাই বাদ দেওয়া মানে মালিকের কাছে ভাঙা ফাইল যাওয়া।
"""

import os
import subprocess, re, io, sys, json, glob, datetime
import xml.dom.minidom as md

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP  = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp")
JAVA = os.path.join(APP, "app", "src", "main", "java")
RES  = os.path.join(APP, "app", "src", "main", "res")
WEB  = os.path.join(ROOT, "03_NETLIFY_READY")
SQLD = os.path.join(ROOT, "04_SUPABASE_DATABASE_SETUP")
SENT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "pathano_filer_talika.json")

problems = []   # ❌ এগুলো থাকলে ফাইল বানানো যাবে না
notices  = []   # ⚠️ মানুষকে নিজে দেখতে হবে


def fail(rule, what):
    problems.append((rule, what))


def note(rule, what):
    notices.append((rule, what))


# ────────────────────────────────────────────────────────────────
#  Kotlin-সচেতন ব্র্যাকেট গণনা
#  সাধারণ গণনা মিথ্যা ফল দেয়, কারণ Kotlin-এ "${...}" লেখার ভিতরে
#  আবার উদ্ধৃতি ও বন্ধনী থাকতে পারে। এটা সেগুলো আলাদা করে সামলায়।
# ────────────────────────────────────────────────────────────────
def kotlin_balance(src):
    i, n = 0, len(src)
    brace = paren = 0
    mode = 'code'
    tmpl = []
    while i < n:
        c = src[i]
        if mode == 'code':
            if src.startswith('"""', i):
                mode = 'raw'; i += 3; continue
            if c == '"':
                mode = 'str'; i += 1; continue
            if c == "'":                      # char literal, যেমন '{'
                i += 1
                while i < n and src[i] != "'":
                    if src[i] == '\\':
                        i += 1
                    i += 1
                i += 1; continue
            if src.startswith('//', i):
                j = src.find('\n', i); i = j if j != -1 else n; continue
            if src.startswith('/*', i):
                d = 1; i += 2
                while i < n and d:
                    if src.startswith('/*', i): d += 1; i += 2; continue
                    if src.startswith('*/', i): d -= 1; i += 2; continue
                    i += 1
                continue
            if c == '{':
                brace += 1
                if tmpl: tmpl[-1][0] += 1
            elif c == '}':
                brace -= 1
                if tmpl:
                    tmpl[-1][0] -= 1
                    if tmpl[-1][0] == 0:
                        mode = tmpl.pop()[1]; i += 1; continue
            elif c == '(':
                paren += 1
            elif c == ')':
                paren -= 1
            i += 1; continue
        if mode == 'str':
            if c == '\\': i += 2; continue
            if c == '"': mode = 'code'; i += 1; continue
            if src.startswith('${', i):
                brace += 1; tmpl.append([1, 'str']); mode = 'code'; i += 2; continue
            if c == '\n': mode = 'code'; i += 1; continue
            i += 1; continue
        if mode == 'raw':
            if src.startswith('"""', i): mode = 'code'; i += 3; continue
            if src.startswith('${', i):
                brace += 1; tmpl.append([1, 'raw']); mode = 'code'; i += 2; continue
            i += 1; continue
    return brace, paren


def comment_start(line):
    """লাইনে // কমেন্ট কোথায় শুরু (স্ট্রিং-এর ভিতরেরটা বাদ)। না থাকলে -1"""
    j, n, instr = 0, len(line), False
    while j < n:
        c = line[j]
        if instr:
            if c == '\\': j += 2; continue
            if c == '"': instr = False
            j += 1; continue
        if c == '"': instr = True; j += 1; continue
        if line.startswith('//', j): return j
        j += 1
    return -1


def kt_files():
    out = []
    for dp, _, fs in os.walk(JAVA):
        for f in fs:
            if f.endswith(".kt"):
                out.append(os.path.join(dp, f))
    return out


def read(p):
    return io.open(p, encoding="utf-8").read()


# ═══════════════════════════════════════════════════════════════
#  যাচাই ১ — ব্র্যাকেট (সার্কুলার ৯.১)
# ═══════════════════════════════════════════════════════════════
def check_brackets():
    KNOWN_OK = {"ReportCardPrinter.kt", "TrashAdapter.kt"}   # raw string-এ CSS, মূল ফাইল থেকেই এমন
    bad = []
    for f in kt_files():
        b, p = kotlin_balance(read(f))
        if (b, p) != (0, 0) and os.path.basename(f) not in KNOWN_OK:
            bad.append(f"{os.path.basename(f)} (brace {b:+d}, paren {p:+d})")
    if bad:
        fail("৯.১", "ব্র্যাকেট মিলছে না → " + " · ".join(bad))
    return len(kt_files())


# ═══════════════════════════════════════════════════════════════
#  যাচাই ২ — কমেন্ট কোড গিলে ফেলেছে কিনা (সার্কুলার ৯.২)
#  ২৮.০৭.২০২৬-এ ঠিক এই ভুলে বিল্ড ভেঙেছিল।
# ═══════════════════════════════════════════════════════════════
def check_comment_swallow():
    BENG = re.compile(r'[\u0980-\u09FF]')
    bad = []
    for f in kt_files():
        for i, l in enumerate(read(f).split("\n"), 1):
            p = comment_start(l)
            if p < 0 or not l[:p].strip():
                continue                       # পুরো লাইনই কমেন্ট → নিরাপদ
            cm = l[p + 2:]
            if not BENG.search(cm):
                continue
            tail = cm[max(m.end() for m in BENG.finditer(cm)):].strip()
            if re.search(r'[{}]|else\s+null|;\s*\w', tail):
                bad.append(f"{os.path.basename(f)}:{i}")
    if bad:
        fail("৯.২", "কমেন্টের পরে কোড পড়ে আছে → " + " · ".join(bad))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৩ — if expression-এ else (সার্কুলার ৯.৪)
# ═══════════════════════════════════════════════════════════════
def check_if_else():
    bad = []
    for f in kt_files():
        for i, l in enumerate(read(f).split("\n"), 1):
            p = comment_start(l)
            code = l[:p] if p >= 0 else l
            if re.search(r'\bval\s+\w+\s*:\s*[\w.<>?()\s\-]+\?\s*=\s*if\s*\(', code):
                if " else " not in code and not code.rstrip().endswith(("{", "->")):
                    bad.append(f"{os.path.basename(f)}:{i}")
    if bad:
        fail("৯.৪", "`if` expression-এ `else` নেই → " + " · ".join(bad))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৪ — binding.<view> লেআউটে আছে কিনা (সার্কুলার ৯.৫)
# ═══════════════════════════════════════════════════════════════
def check_bindings():
    ids = set()
    for f in glob.glob(os.path.join(RES, "layout", "*.xml")):
        ids |= set(re.findall(r'@\+id/(\w+)', read(f)))
    miss = set()
    for f in kt_files():
        for m in re.finditer(r'binding\.(\w+)', read(f)):
            v = m.group(1)
            if v == "root" or v[0].isupper() or v in ids:
                continue
            miss.add(f"{os.path.basename(f)} → {v}")
    if miss:
        fail("৯.৫", "লেআউটে এই view নেই → " + " · ".join(sorted(miss)))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৫ — drawable রেফারেন্স (সার্কুলার ৯.৫)
# ═══════════════════════════════════════════════════════════════
def check_drawables():
    have = set()
    for d in ("drawable", "drawable-v24", "drawable-night"):
        p = os.path.join(RES, d)
        if os.path.isdir(p):
            have |= {os.path.splitext(x)[0] for x in os.listdir(p)}
    used = set()
    for f in glob.glob(os.path.join(RES, "layout", "*.xml")):
        used |= set(re.findall(r'@drawable/(\w+)', read(f)))
    for f in kt_files():
        used |= set(re.findall(r'R\.drawable\.(\w+)', read(f)))
    miss = sorted(used - have)
    if miss:
        fail("৯.৫", "এই drawable নেই → " + " · ".join(miss))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৬ — XML (সার্কুলার ৯.৬)
# ═══════════════════════════════════════════════════════════════
def check_xml():
    files = []
    for dp, _, fs in os.walk(os.path.join(APP, "app", "src", "main")):
        for f in fs:
            if f.endswith(".xml"):
                files.append(os.path.join(dp, f))
    broken, dashes = [], []
    for f in files:
        try:
            md.parse(f)
        except Exception:
            broken.append(os.path.basename(f))
        if re.search(r'<!--(?:(?!-->).)*--(?:(?!>).)', read(f), re.S):
            dashes.append(os.path.basename(f))
    if broken:
        fail("৯.৬", "XML ভাঙা → " + " · ".join(broken))
    if dashes:
        fail("৯.৬", "XML কমেন্টে '--' আছে → " + " · ".join(dashes))
    return len(files)


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৭ — প্রতি ক্লাসে একটাই companion object (সার্কুলার ৯.৬)
# ═══════════════════════════════════════════════════════════════
def check_companion():
    bad = []
    for f in kt_files():
        src = read(f)
        if len(re.findall(r'^\s*(?:private\s+)?companion\s+object', src, re.M)) > 1:
            # একাধিক ক্লাস থাকলে সত্যি হতে পারে — তাই শুধু জানানো
            if len(re.findall(r'^\s*(?:internal\s+|private\s+|open\s+|abstract\s+)*class\s+\w', src, re.M)) < 2:
                bad.append(os.path.basename(f))
    if bad:
        fail("৯.৬", "এক ক্লাসে একাধিক companion object → " + " · ".join(bad))


# ═══════════════════════════════════════════════════════════════
#  🔴🔒 V801 — ডেটাবেসের আসল ঘরগুলো এক জায়গা থেকে (আগে যাচাই ৮-এর ভিতরে
#  আটকে ছিল, তাই অন্য যাচাই ওটা কাজে লাগাতে পারত না)।
#  ⛔ সঙ্গে একটা গর্তও বন্ধ হলো: আগে শুধু `04_SUPABASE_DATABASE_SETUP/`
#     দেখা হত, অথচ `00_SQL/`-এও আসল migration আছে (যেমন V736 — patients-এর
#     `editHistory`)। ফলে সত্যিকারের ঘরকেও "নেই" বলে মিথ্যে ভুল দেখাত।
# ═══════════════════════════════════════════════════════════════
_DB_COLS_CACHE = {}


def _db_columns():
    if _DB_COLS_CACHE:
        return _DB_COLS_CACHE
    cols = {}
    setup = os.path.join(SQLD, "PILES_CLINIC_DB_SETUP.sql")
    if os.path.exists(setup):
        s = read(setup)
        for m in re.finditer(r'create table if not exists public\.(\w+)\s*\((.*?)\n\);', s, re.S):
            cols[m.group(1)] = set(re.findall(r'"(\w+)"\s+\w', m.group(2)))
    folders = [SQLD, os.path.join(ROOT, "00_SQL")]
    for d in folders:
        if not os.path.isdir(d):
            continue
        for f in glob.glob(os.path.join(d, "*.sql")):
            if os.path.basename(f) == "PILES_CLINIC_DB_SETUP.sql":
                continue
            s = read(f)
            for m in re.finditer(
                r'alter table\s+(?:if exists\s+)?(?:public\.)?"?(\w+)"?\s+add column\s+(?:if not exists\s+)?"?(\w+)"?',
                s, re.I
            ):
                cols.setdefault(m.group(1), set()).add(m.group(2))
            for m in re.finditer(r'create table if not exists (?:public\.)?"?(\w+)"?\s*\((.*?)\n\);', s, re.S | re.I):
                cols.setdefault(m.group(1), set()).update(re.findall(r'"(\w+)"\s+\w', m.group(2)))
    cols.setdefault('patients', set()).add('timeType')   # setup.sql-এর চেয়ে আসল DB নতুন (সার্কুলার ১০)
    _DB_COLS_CACHE.update(cols)
    return _DB_COLS_CACHE


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩৭ — Supabase-এ প্রতিটা ডাকে দুটো হেডারই আছে তো?
#  🔴🔴🔴 TK-REPORTED, LIVE (২৮.০৮.২০২৬ — "Staff Profile খুলছেই না",
#  সাদা পর্দা / "Could not open — timeout")। **আসল কারণ:** মডিউল-লগইনের
#  ডাকটা পাঠাত শুধু `apikey`, কিন্তু `Authorization: Bearer <key>` হেডারটা
#  ছিল না। এই অ্যাপের বাকি **সব** ডাক (SupabaseClient) ও ওয়েবের Supabase
#  SDK — দুটোই পাঠায়। নতুন ধরনের চাবিতে (`sb_publishable_…`) একটা না পেলে
#  Supabase সাড়া দেয় না ⇒ অ্যাপ অপেক্ষা করতেই থাকে।
#  ⇒ এখন কোনো ডাকে হেডারটা বাদ পড়লে ফাইলই বানানো যাবে না।
# ═══════════════════════════════════════════════════════════════
def check_supabase_auth_header():
    bad = []
    for f in kt_files():
        # ⚠️ এখানে `_blank_comments()` **ব্যবহার করা যাবে না** — ওটা স্ট্রিং-ও
        #    ফাঁকা করে দেয়, ফলে `"apikey"` লেখাটাই আর খুঁজে পাওয়া যায় না
        #    (নিজের ফাঁদ-পরীক্ষায় ধরা পড়েছে, ২৮.০৮.২০২৬)। তাই আসল লেখাই পড়া হয়;
        #    কমেন্টে লেখা উদাহরণ যেন না ধরা পড়ে, সেজন্য নিচে `.url(` বাধ্যতামূলক।
        s = read(f)
        if "supabase.co" not in s and "baseUrl()" not in s and "SupabaseConfig.url" not in s:
            continue
        for m in re.finditer(r'Request\.Builder\(\)(.*?)\.build\(\)', s, re.S):
            blk = m.group(1)
            if 'addHeader("apikey"' not in blk:
                continue
            if 'addHeader("Authorization"' in blk:
                continue
            url = re.search(r'\.url\(([^\n]{0,90})', blk)
            if not url:
                continue
            ln = s[:m.start()].count("\n") + 1
            bad.append("%s:%d — %s" % (os.path.basename(f), ln, url.group(1).strip()[:70]))
    if bad:
        for b in bad[:8]:
            fail("৯.৩৭", "Supabase-এর ডাকে `apikey` আছে কিন্তু `Authorization: Bearer` নেই "
                         "⇒ সার্ভার সাড়া না দিলে পর্দা আটকে যাবে — " + b)


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩৬ — 🕵️ **ইন্সপেক্টর**: প্রকল্পের নিজের ক্লাস import ছাড়া ব্যবহার
#  🔴🔴🔴 TK-REPORTED, LIVE (২৮.০৮.২০২৬, ফটো সহ — Android Studio:
#  "Unresolved reference: PilesClinicApplication", PatientPhotoCache.kt:41,
#  Gradle build failed)। TK: *"কি ধরনের ফালতু পাহারাদার রেখেছেন … ইন্সপেক্টর
#  রাখুন"* — একদম ঠিক কথা, দুটো পাহারাদারই এটা ছেড়ে দিয়েছিল:
#    (ক) `verify_kotlin_compile.py`-র `is_noise()` — যে নাম প্রকল্পের **অন্য
#        কোনো** ফাইলে import করা আছে, সেটা সব ফাইলেই "ঠিক আছে" ধরে নিত
#        (`ext` তালিকাটা ফাইল-ধরে নয়, গোটা প্রকল্প ধরে বানানো)।
#    (খ) `missing_import_errors()` — শুধু `android`/`androidx` ক্লাস দেখত,
#        **প্রকল্পের নিজের ক্লাস কখনো দেখত না**।
#  ─── এই ইন্সপেক্টর যা করে ─────────────────────────────────────────────
#  ১) প্রকল্পের প্রতিটা `class/object/interface` কোন প্যাকেজে ঘোষিত — তালিকা।
#  ২) প্রতিটা ফাইলে কমেন্ট ও স্ট্রিং ফাঁকা করে নিয়ে খোঁজে: এমন কোনো নাম
#     `Name.` বা `Name(` হিসেবে ব্যবহার হয়েছে কিনা, যেটা **অন্য প্যাকেজে**
#     ঘোষিত অথচ এই ফাইলে import করা নেই।
#  ৩) পেলে ফাইল · লাইন · **যে import লাইনটা লিখতে হবে** — সব দেখিয়ে FAIL।
#  ⛔ কম্পাইলার লাগে না, তাই সবসময় চলে ও নিখুঁত।
# ═══════════════════════════════════════════════════════════════
def check_project_class_imports():
    files = {}
    for f in kt_files():
        if "/src/test/" in f.replace("\\", "/"):
            continue
        files[f] = read(f)

    decl = {}
    DECL_RE = (r'^\s*(?:@\w+\s+)*(?:public\s+|internal\s+|private\s+|open\s+|abstract\s+'
               r'|sealed\s+|data\s+|enum\s+|annotation\s+|value\s+)*'
               r'(?:class|object|interface)\s+(\w+)')
    for f, t in files.items():
        pm = re.search(r'^\s*package\s+([\w.]+)', t, re.M)
        if not pm:
            continue
        code = _blank_comments(t)
        for m in re.finditer(DECL_RE, code, re.M):
            decl.setdefault(m.group(1), set()).add(pm.group(1))

    bad = []
    for f in sorted(files):
        t = files[f]
        pm = re.search(r'^\s*package\s+([\w.]+)', t, re.M)
        if not pm:
            continue
        mypkg = pm.group(1)
        code = _blank_comments(t)
        imported = {(m.group(2) or m.group(1).split(".")[-1]) for m in
                    re.finditer(r'^\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?', code, re.M)}
        star = {m.group(1) for m in re.finditer(r'^\s*import\s+([\w.]+)\.\*', code, re.M)}
        local = {m.group(1) for m in re.finditer(r'\b(?:class|object|interface)\s+(\w+)', code)}
        for name, pkgs in decl.items():
            if name in imported or name in local or mypkg in pkgs or (pkgs & star):
                continue
            m = re.search(r'(?<![\w.])' + re.escape(name) + r'\s*[.(]', code)
            if m:
                ln = code[:m.start()].count("\n") + 1
                rel = f.replace("\\", "/")
                rel = rel[rel.find("com/"):] if "com/" in rel else os.path.basename(rel)
                bad.append((rel, ln, name, sorted(pkgs)[0]))
    if bad:
        for rel, ln, name, pkg in bad[:8]:
            fail("৯.৩৬", "Android Studio-তে `Unresolved reference: %s` হবে — %s:%d। "
                         "এই লাইনটা যোগ করুন:  import %s.%s" % (name, rel, ln, pkg, name))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩৫ — প্রতিটা OkHttpClient-এ callTimeout আছে তো?
#  🔴🔴 TK-REPORTED, LIVE (২৮.০৮.২০২৬, ফটো সহ — "Staff Profile তো খুলছেই
#  না?", সাদা ফাঁকা পর্দা): `ModuleAuth.kt`-এ ছিল খালি `OkHttpClient()` —
#  একটাও timeout নেই। OkHttp-র ডিফল্টে `callTimeout = 0` (সময়সীমা নেই), আর
#  নেট আধমরা হয়ে উত্তর ফোঁটা-ফোঁটা এলে `readTimeout` বারবার নতুন করে শুরু
#  হয় ⇒ ডাক কোনোদিন শেষ হয় না ⇒ পর্দা চিরকাল সাদা।
#  প্রজেক্টে এটা আগেই একবার ধরা পড়ে সারানো হয়েছিল (SupabaseClient.kt:19-29),
#  কিন্তু বাকি ফাইলে বসানো হয়নি। এখন আর কোনো নতুন ক্লায়েন্ট পার পাবে না।
# ═══════════════════════════════════════════════════════════════
def check_http_call_timeout():
    bad = []
    for f in kt_files():
        s = _blank_comments(read(f))
        if "OkHttpClient" not in s:
            continue
        name = os.path.basename(f)
        # খালি OkHttpClient() — একটাও timeout নেই
        if re.search(r'OkHttpClient\s*\(\s*\)', s):
            bad.append(name + " — খালি `OkHttpClient()`, একটাও timeout নেই")
            continue
        for m in re.finditer(r'OkHttpClient\s*\.\s*Builder\s*\(\s*\)(.*?)\.build\s*\(\s*\)', s, re.S):
            if "callTimeout" not in m.group(1):
                bad.append(name + " — Builder-এ `callTimeout` বসানো নেই")
    if bad:
        for b in sorted(set(bad))[:8]:
            fail("৯.৩৫", "নেট-ডাকে সময়সীমা নেই ⇒ পর্দা চিরকাল সাদা হয়ে বসে থাকতে পারে — " + b)


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩৪ — SafeWideColumns পুরনো হয়ে যায়নি তো?
#  🔴🔒 V801 (২৮.০৮.২০২৬), TK-নির্দেশ: "গভীরে যাচাই করুন / কোন ভালো কাজ
#  যেন খারাপ না হয়"। SafeWideColumns-এর তালিকাগুলো **শেষ-ভরসার** পড়ায়
#  ব্যবহার হয় (সরু পড়া ব্যর্থ হলে, `select=*`-এর ঠিক আগে)। নতুন ঘর যোগ
#  হলে ওখানে যোগ করা হত না — মিলিয়ে দেখে **১৫টা আসল ঘর** বাদ পড়ে ছিল,
#  তার মধ্যে রিফান্ড/ব্যাকডেট-অনুমোদনের মতো **টাকার ঘরও**। ওই পড়াটা
#  চললে সেগুলো চুপচাপ উধাও হয়ে যেত। এখন আর কখনো পুরনো হতে পারবে না।
# ═══════════════════════════════════════════════════════════════
def check_safe_wide_columns():
    path = None
    for f in kt_files():
        if os.path.basename(f) == "SafeWideColumns.kt":
            path = f
            break
    if not path:
        return                     # ফাইলই নেই — যাচাইয়ের কিছু নেই
    s = read(path)
    all_m = re.search(r'private val ALL[^=]*=\s*mapOf\((.*?)\n    \)', s, re.S)
    heavy_m = re.search(r'private val HEAVY[^=]*=\s*mapOf\((.*?)\n    \)', s, re.S)
    if not all_m or not heavy_m:
        fail("৯.৩৪", "SafeWideColumns.kt-এর ALL / HEAVY তালিকা পড়া গেল না")
        return
    heavy = {}
    for m in re.finditer(r'"(\w+)"\s*to\s*listOf\(([^)]*)\)', heavy_m.group(1)):
        heavy[m.group(1)] = set(re.findall(r'"(\w+)"', m.group(2)))
    db = _db_columns()
    bad = []
    for m in re.finditer(r'"(\w+)"\s*to\s*"([^"]+)"', all_m.group(1)):
        tb = m.group(1)
        listed = set(x.strip() for x in m.group(2).split(",") if x.strip())
        real = db.get(tb)
        if not real:
            continue                      # এই টেবিলের SQL জানা নেই — চুপ থাকি
        missing = sorted(real - listed - heavy.get(tb, set()))
        ghost = sorted(listed - real - heavy.get(tb, set()))
        if missing:
            bad.append(f"{tb}: SafeWideColumns-এ নেই → {', '.join(missing)}")
        if ghost:
            bad.append(f"{tb}: ডেটাবেসে নেই এমন নাম → {', '.join(ghost)}")
    if bad:
        for b in bad[:8]:
            fail("৯.৩৪", "SafeWideColumns পুরনো হয়ে গেছে — " + b +
                 "  (শেষ-ভরসার পড়ায় ঘরটা চুপচাপ উধাও হবে)")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৮ — Supabase কলাম (সার্কুলার ৯.৭)
# ═══════════════════════════════════════════════════════════════
def check_columns():
    cols = {}
    setup = os.path.join(SQLD, "PILES_CLINIC_DB_SETUP.sql")
    if os.path.exists(setup):
        s = read(setup)
        for m in re.finditer(r'create table if not exists public\.(\w+)\s*\((.*?)\n\);', s, re.S):
            cols[m.group(1)] = set(re.findall(r'"(\w+)"\s+\w', m.group(2)))
    # 🔴🔴 TK-অডিট-অনুরোধ (01.08.2026, Refund-হিসাব অডিটের সময় ধরা পড়ল): এই
    # scanner শুধু "*PATCH*.sql" নামের ফাইল খুঁজত, তাই "V215_SAFE_MIGRATION...",
    # "V216_AUTH_PREP...", "V219_SECURITY..." ইত্যাদি নামের বাস্তব, TK নিজে
    # Run-করা migration ফাইলগুলো এতদিন এই পাহারাদারের চোখেই পড়েনি — যে কলাম
    # ওই ফাইলগুলোয় যোগ হয়েছে (যেমন refundApprovalStatus, password_hash,
    # auth_user_id) সেগুলো ভুলভাবে "নেই" বলে ধরা পড়ত। এখন PILES_CLINIC_DB_SETUP.sql
    # বাদে ফোল্ডারের **সব** .sql ফাইল স্ক্যান হয়।
    # সঙ্গে regex-ও নরম করা হলো — বাস্তব ফাইলে "alter table if exists <name>"
    # (কোনো "public." প্রিফিক্স ছাড়া) প্যাটার্নও আছে, আগের regex শুধু
    # "alter table public.<name>" ধরত।
    # 🔴🔒 V814 (২৮.০৮.২০২৬, নিজের যাচাইয়ে ধরা পড়া পাহারার ফাঁক) — এই যাচাই
    # এতদিন **শুধু `04_SUPABASE_DATABASE_SETUP/`** ফোল্ডার দেখত। কিন্তু আসল
    # migration ফাইল অনেকগুলোই **`00_SQL/`**-এ থাকে (যেমন V770, V814) — তাই
    # ওখানে যোগ-করা সত্যিকারের ঘরকেও "ডেটাবেসে নেই" বলে ধরা পড়ত। §৯.৩৪-এর
    # `_db_columns()` দুটো ফোল্ডারই দেখত, এটা দেখত না — দুটো এখন এক নিয়মে।
    for _d in (SQLD, os.path.join(ROOT, "00_SQL")):
        if not os.path.isdir(_d):
            continue
        for f in glob.glob(os.path.join(_d, "*.sql")):
            if os.path.basename(f) == "PILES_CLINIC_DB_SETUP.sql":
                continue
            s = read(f)
            for m in re.finditer(
                r'alter table\s+(?:if exists\s+)?(?:public\.)?"?(\w+)"?\s+add column\s+(?:if not exists\s+)?"?(\w+)"?',
                s, re.I
            ):
                cols.setdefault(m.group(1), set()).add(m.group(2))
            for m in re.finditer(r'create table if not exists (?:public\.)?"?(\w+)"?\s*\((.*?)\n\);', s, re.S | re.I):
                cols.setdefault(m.group(1), set()).update(re.findall(r'"(\w+)"\s+\w', m.group(2)))
    # setup.sql আসল ডেটাবেসের চেয়ে পুরনো — এই ঘরটা সত্যিই আছে (সার্কুলার ১০)
    cols.setdefault('patients', set()).add('timeType')
    bad = []
    # 🔒 নতুন (29.07.2026, খাতার সারি B107): কলামের তালিকা যখন সরাসরি লেখা না
    # হয়ে **নামে** পাঠানো হয় (যেমন `SupabaseClient.FOLLOWUP_COLS_NO_PHOTO`),
    # পাহারাদার আগে সেটা দেখতেই পেত না — ঠিক ওখানেই ছ'টা আসল ঘর বাদ পড়ে
    # ছিল (সারি B105)। এখন const-গুলোর লেখা আগে জোগাড় করে নেওয়া হয়।
    consts = {}
    for f in kt_files():
        for m in re.finditer(r'(?:const\s+)?val\s+([A-Z][A-Z0-9_]*)\s*=\s*"([a-zA-Z,]+)"', read(f)):
            consts[m.group(1)] = m.group(2)
    for f in kt_files():
        s = read(f)
        for m in re.finditer(r'fetchListSlim(?:OrNull)?\(\s*"(\w+)",[^)]*?"([a-zA-Z,]+)"', s, re.S):
            miss = [c for c in m.group(2).split(',') if c and c not in cols.get(m.group(1), set())]
            if miss:
                bad.append(f"{os.path.basename(f)} → {m.group(1)}: {','.join(miss)}")
        for m in re.finditer(r'fetchListSlim(?:OrNull)?\(\s*"(\w+)",[^)]*?([A-Z][A-Z0-9_]{4,})\s*[,)]', s, re.S):
            lst = consts.get(m.group(2))
            if not lst:
                continue
            miss = [c for c in lst.split(',') if c and c not in cols.get(m.group(1), set())]
            if miss:
                bad.append(f"{os.path.basename(f)} → {m.group(1)} ({m.group(2)}): {','.join(miss)}")
    if bad:
        fail("৯.৭", "ডেটাবেসে এই কলাম নেই → " + " · ".join(bad))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩৮ — `R.drawable`/`R.id`… ব্যবহার করলে `R`-এর import থাকতেই হবে
#
#  🔴🔴🔴 TK-এর Android Studio-তে ধরা পড়া বিল্ড-এরর (৩০.০৮.২০২৬ সকাল ৯.৫৬):
#      DoctorQueueAdapter.kt — `Unresolved reference: R` :118 :122 :126
#
#  **আসল কারণ:** ফাইলটা `com.tkbiswas.pilesclinic.native` প্যাকেজে, কিন্তু
#  `R` ক্লাসটা তৈরি হয় মূল প্যাকেজে (`com.tkbiswas.pilesclinic.R`)। তাই
#  উপ-প্যাকেজের ফাইলে খালি `R` লিখলে কম্পাইলার খুঁজে পায় না — import লাগে।
#
#  আগের কোনো পাহারা এটা ধরত না: §৯.১৮/§৯.৩৬ শুধু **প্রকল্পে লেখা** ক্লাসের
#  নাম মেলায়, আর `R` কোডে লেখা নয় — বিল্ডের সময় তৈরি হয়। এখন এই ফাঁকটা বন্ধ।
# ═══════════════════════════════════════════════════════════════
def check_r_import():
    bad = []
    for f in kt_files():
        s = read(f)
        m = re.search(r'^package\s+([\w.]+)', s, re.M)
        pkg = m.group(1) if m else ''
        # মূল প্যাকেজের ফাইলে import লাগে না
        if pkg == 'com.tkbiswas.pilesclinic':
            continue
        code = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
        code = '\n'.join(l.split('//', 1)[0] for l in code.split('\n'))
        if not re.search(r'(?<![\w.])R\.(drawable|layout|id|string|color|style|mipmap|raw|anim|array|dimen)\b', code):
            continue
        if not re.search(r'^import\s+com\.tkbiswas\.pilesclinic\.R\s*$', s, re.M):
            bad.append(os.path.relpath(f, ROOT))
    if bad:
        for b in bad[:8]:
            fail("৯.৩৮", f"{b} — `R.` ব্যবহার হয়েছে কিন্তু `import com.tkbiswas.pilesclinic.R` নেই ⇒ Android Studio-তে `Unresolved reference: R` (৩০.০৮.২০২৬-এ ধরা পড়া বিল্ড-এরর)")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩৯ — 🧑‍💼 আসল রেজিস্ট্রারের নাম কখনো বদলাবে না
#  (TK-রিপোর্ট ৩০.০৮.২০২৬, ছবিসহ — RAJA MANDAL কার্ডে JPE-CRP-এর নাম
#   মুছে TK BISWAS হয়ে গিয়েছিল, কারণ সারি আবার সেভ হলে `registeredBy`
#   /`createdBy`/`createdAt`-এ তখনকার লগইনের নাম-সময় বসে যেত।)
#  ⛔ এই তিনটে ঘর যেন আর কখনো সরাসরি বসানো না হয় — `keep…ifBlank` হয়েই
#     যেতে হবে। ফোন ও কম্পিউটার — দুটোতেই যাচাই হয়।
# ═══════════════════════════════════════════════════════════════
# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৪০ — 🧱 প্রতিটা Kotlin ফাইলে ব্রেস ও বন্ধনী মেলে
#  (TK-নির্দেশ ৩০.০৮.২০২৬: *"Android studio তে Build করার সময় যেন
#   কোন প্রকারে error না আসে"*)
#  ⛔ একটা ব্রেস কম/বেশি হলে Android Studio-তে parse-error — অথচ
#     kotlinc-এর বেসলাইন-ব্যবস্থায় সেটা চাপা পড়ে যেতে পারত।
#  ⚠️ স্ট্রিং · ক্যারেক্টার · মন্তব্য · `${...}` — সব বাদ দিয়ে গোনা হয়,
#     তাই ভুল সংকেত (false alarm) আসে না।
# ═══════════════════════════════════════════════════════════════
def _scan_balance(s):
    i = 0; n = len(s); depth = 0; par = 0
    while i < n:
        c = s[i]
        if c == '/' and i + 1 < n and s[i+1] == '/':
            while i < n and s[i] != '\n': i += 1
        elif c == '/' and i + 1 < n and s[i+1] == '*':
            i += 2
            while i + 1 < n and not (s[i] == '*' and s[i+1] == '/'): i += 1
            i += 2
        elif c == '"':
            if s[i:i+3] == '"""':
                i += 3
                while i + 2 < n and s[i:i+3] != '"""': i += 1
                i += 3
            else:
                i += 1
                while i < n and s[i] != '"':
                    if s[i] == '\\': i += 1
                    elif s[i] == '$' and i + 1 < n and s[i+1] == '{':
                        d = 1; i += 2
                        while i < n and d > 0:
                            if s[i] == '{': d += 1
                            elif s[i] == '}': d -= 1
                            elif s[i] == '"':
                                i += 1
                                while i < n and s[i] != '"':
                                    if s[i] == '\\': i += 1
                                    i += 1
                            i += 1
                        continue
                    i += 1
                i += 1
        elif c == "'":
            i += 1
            while i < n and s[i] != "'":
                if s[i] == '\\': i += 1
                i += 1
            i += 1
        else:
            if c == '{': depth += 1
            elif c == '}': depth -= 1
            elif c == '(': par += 1
            elif c == ')': par -= 1
            i += 1
    return depth, par


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৪১ — ⏰ history-তে লেখা প্রতিটা সারিতে date **ও** time
#  (TK-নির্দেশ ৩০.০৮.২০২৬: *"তারিখ এবং সময় সমস্ত জায়গায় লাগবে"*)
#  ⛔ V827-এর আগে কিছু জায়গায় শুধু তারিখ বসত, সময় বসত না — তাই
#     টাইমলাইনে সময়ের ঘর ফাঁকা দেখাত (TK ছবি দিয়ে ধরেছিলেন)।
#  ⚠️ শুধু সেই সারিগুলোই দেখা হয় যেগুলো সত্যিই `history`-তে যায়
#     (JSONObject-এ `remark` **ও** (`staff`/`status`) দুটোই আছে) —
#     তাই call_remarks/বেতন/চেম্বারের সারিতে ভুল সংকেত আসে না।
# ═══════════════════════════════════════════════════════════════
def check_history_time():
    pat = re.compile(r'JSONObject\(\)((?:\s*\.put\([^\n]*\n?){1,12})')
    for f in kt_files():
        s = read(f)
        for m in pat.finditer(s):
            blk = m.group(1)
            if '.put("remark"' not in blk:
                continue
            if not re.search(r'\.put\("staff"\s*,', blk):
                continue
            if '.put("time"' in blk and '.put("date"' in blk:
                continue
            line = s[:m.start()].count('\n') + 1
            fail("৯.৪১", f"{os.path.relpath(f, ROOT)}:{line} — history-র সারিতে "
                        f"`date`/`time` দুটোই নেই ⇒ টাইমলাইনে সময় ফাঁকা দেখাবে")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৪২ — 🔤 হ্যাঁ/না-এর ঘর কখনো সোজাসুজি পড়া যাবে না
#  (TK-এর লাইভ রিপোর্ট ৩১.০৮.২০২৬: *"মোবাইলে পেশেন্ট আছে কিন্তু
#   কম্পিউটারে নেই কেন?"* — V906-এ ধরা পড়া আসল দোষের পাহারা)
#
#  ডেটাবেসে `patients.queue` ও `patients.doctorComplete` জমা থাকে
#  **লেখা** হিসেবে — `"true"` / `"false"` — সত্যি-মিথ্যা হিসেবে নয়
#  (TK-এর আসল Backup ফাইলে ৩৯৭টার মধ্যে ৩৯৪টাই এমন)।
#    · ফোন — `optBoolean()` ওই লেখাটা বুঝে নেয়, তাই ঠিক দেখায়।
#    · কম্পিউটার — `!x.doctorComplete` লিখলে `"false"` একটা **ভরা লেখা**,
#      তাই "সত্যি" ধরা হত ⇒ প্রত্যেক রোগীকেই "চেকআপ শেষ" ভেবে CHECK-UP
#      তালিকা থেকে বাদ দিত ⇒ কম্পিউটারে তালিকা সবসময়ই ফাঁকা থাকত।
#
#  ⇒ এখন থেকে ওয়েবের কোনো ফাইলে এই দুটো ঘর `!` / `&&` / `||` /
#     `===true` — এভাবে পড়া যাবে না; সবসময় `wlv1Flag(...)` দিয়ে।
#  ⛔ শুধু **পড়া** আটকানো হয় — লেখা (`queue:true`), ঘর আছে কিনা দেখা
#     (`!==undefined` / `!==null`), আর বয়ে নিয়ে যাওয়া (`?:`) আগের মতোই।
# ═══════════════════════════════════════════════════════════════
FLAG_FIELDS = ("queue", "doctorComplete")


def _js_code_mask(src):
    """মন্তব্যের ঘরগুলো বাদ দিয়ে শুধু আসল কোডের জায়গা চিহ্নিত করে।
       ⛔ পাহারা যেন মন্তব্যের ভিতরের উদাহরণ দেখে ভুল করে না ধরে।
       ⚠️ লেখার (string) ভিতরটা আলাদা করে দেখা হয় না — ইচ্ছে করেই।
          `x.queue` ধাঁচটা লেখার ভিতরে থাকে না, আর string-খোঁজা করতে গেলে
          regex-literal (`/['"]/`)-এ হিসাব গুলিয়ে যায় ⇒ আসল দোষ ফসকে যেত
          (৩১.০৮.২০২৬-এ নিজে পরীক্ষা করে ধরা পড়েছে)। """
    n = len(src)
    m = bytearray(b"\x01") * n
    i = 0
    while True:                                   # /* ... */ মন্তব্য
        i = src.find("/*", i)
        if i < 0:
            break
        j = src.find("*/", i + 2)
        j = n if j < 0 else j + 2
        for k in range(i, j):
            m[k] = 0
        i = j
    pos = 0
    for line in src.split("\n"):                  # // দিয়ে শুরু হওয়া লাইন
        t = line.lstrip()
        if t.startswith("//") or t.startswith("*"):
            for k in range(pos, pos + len(line)):
                m[k] = 0
        pos += len(line) + 1
    return m


def check_web_flag_fields():
    import glob as _glob
    obj = re.compile(r'(?<![\w$.])[A-Za-z_$][\w$]*(?:\??\.[A-Za-z_$][\w$]*)*'
                     r'\.(?:' + '|'.join(FLAG_FIELDS) + r')\b')
    cmp_re = re.compile(r'^\s*(?:===|!==|==|!=)\s*([A-Za-z0-9_$\'"]+)')
    for js in sorted(_glob.glob(os.path.join(WEB, "*.js"))):
        if os.path.basename(js).startswith("V"):
            continue                      # পুরোনো ডেমো-সোর্স, অ্যাপে চলে না
        s = read(js)
        mask = _js_code_mask(s)
        for m in obj.finditer(s):
            if not mask[m.start()]:
                continue                  # মন্তব্য বা লেখার ভিতরে
            before, after = s[:m.start()], s[m.end():]
            if before.rstrip().endswith("wlv1Flag("):
                continue                  # ঠিকভাবেই পড়া হচ্ছে
            c = cmp_re.match(after)
            if c:                         # তুলনা — শুধু true/false হলেই দোষ
                if c.group(1) in ("true", "false"):
                    bad = "`" + c.group(0).strip() + "` — লেখা `\"true\"`/`\"false\"`-এর সঙ্গে মিলবে না"
                else:
                    continue
            elif before.rstrip().endswith("!"):
                bad = "`!` দিয়ে উল্টো করা হয়েছে"
            elif before.rstrip().endswith(("&&", "||")) or after.lstrip().startswith(("&&", "||")):
                bad = "`&&`/`||`-এ সরাসরি ব্যবহার"
            else:
                continue
            line = before.count("\n") + 1
            fail("৯.৪২", f"{os.path.basename(js)}:{line} — `{m.group(0)}` {bad} "
                        f"⇒ `wlv1Flag(...)` দিয়ে পড়তে হবে (V906)")
    app = os.path.join(WEB, "app.js")
    if os.path.exists(app) and "function wlv1Flag(" not in read(app):
        fail("৯.৪২", "app.js — `wlv1Flag()` ঘরটাই নেই ⇒ হ্যাঁ/না পড়ার এক নিয়ম ভেঙে গেছে (V906)")


def check_kotlin_balance():
    for f in kt_files():
        d, p = _scan_balance(read(f))
        if d != 0 or p != 0:
            fail("৯.৪০", f"{os.path.relpath(f, ROOT)} — ব্রেস/বন্ধনী মেলেনি "
                        f"(brace {d:+d} · paren {p:+d}) ⇒ Android Studio-তে parse-error")


def check_owner_preserved():
    pm = os.path.join(APP, "app", "src", "main", "java", "com", "tkbiswas",
                      "pilesclinic", "native", "PatientModel.kt")
    rr = os.path.join(APP, "app", "src", "main", "java", "com", "tkbiswas",
                      "pilesclinic", "native", "RegistrationRepository.kt")
    if os.path.exists(pm):
        s = read(pm)
        for needle, what in (
            ('.put("createdBy", keepCreatedBy.ifBlank { createdByMobile })',
             "রোগীর সারিতে `createdBy` আসল রেজিস্ট্রারের নামই রাখতে হবে"),
            ('.put("registeredBy", keepRegisteredBy.ifBlank { createdByMobile })',
             "রোগীর সারিতে `registeredBy` আসল রেজিস্ট্রারের নামই রাখতে হবে"),
            ('.put("createdBy", keepCreatedBy.ifBlank { staffMobile })',
             "Follow-up (Visit) সারিতে `createdBy` আসল স্টাফের নামই রাখতে হবে"),
        ):
            if needle not in s:
                fail("৯.৩৯", f"PatientModel.kt — {what} (V868-এর পাহারা)")
        if s.count('.put("createdAt", keepCreatedAt.ifBlank { now })') < 2:
            fail("৯.৩৯", "PatientModel.kt — রোগী ও Follow-up দুটো সারিতেই `createdAt` আসল সময়ই রাখতে হবে (V868)")
    if os.path.exists(rr):
        s = read(rr)
        for needle in ("keepCreatedBy", "keepRegisteredBy", "keepCreatedAt",
                       "keepFuCreatedBy", "keepFuCreatedAt"):
            if needle not in s:
                fail("৯.৩৯", f"RegistrationRepository.kt — `{needle}` নেই ⇒ আবার সেভ করলে রেজিস্ট্রারের নাম বদলে যাবে (V868)")
    js = os.path.join(ROOT, "03_NETLIFY_READY", "app.js")
    if os.path.exists(js):
        s = read(js)
        if "registeredBy:old.registeredBy||old.createdBy||p.registeredBy" not in s:
            fail("৯.৩৯", "app.js — আবার রেজিস্ট্রেশনে `registeredBy` পুরোনোটাই রাখতে হবে (V868)")
        if not re.search(r"\.select\('id,createdBy,registeredBy,createdAt[^']*'\)", s):
            fail("৯.৩৯", "app.js — ক্লাউডে সারি থাকলে আসল নাম-সময় ফিরিয়ে আনতে হবে (V868)")



# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯ — ভার্সন তিন জায়গায় এক (সার্কুলার ৯.৮)
# ═══════════════════════════════════════════════════════════════
def check_version():
    g = os.path.join(APP, "app", "build.gradle.kts")
    if not os.path.exists(g):
        fail("৯.৮", "build.gradle.kts পাওয়া যায়নি")
        return None
    s = read(g)
    m = re.search(r'versionCode\s*=\s*(\d+)', s)
    if not m:
        # 🔴🆕 V438 (১৮.০৮.২০২৬) — পাহারাদারের নিজের অন্ধ-জায়গা ঠিক করা হলো।
        #    build.gradle.kts-এ এখন লেখা থাকে  versionCode = appVersionCode
        #    আর উপরে  val appVersionCode = 438 । আগের নিয়মটা শুধু সরাসরি
        #    সংখ্যা খুঁজত, তাই **ভার্সন মেলানোর পুরো পাহারাটাই চুপচাপ বন্ধ ছিল**
        #    (VNone দেখাত)। এখন ওই ভেরিয়েবলটাও পড়া হয়।
        mv = re.search(r'versionCode\s*=\s*([A-Za-z_][A-Za-z0-9_]*)', s)
        if mv:
            m = re.search(r'val\s+' + re.escape(mv.group(1)) + r'\s*=\s*(\d+)', s)
    if not m:
        fail("৯.৮", "versionCode পাওয়া যায়নি")
        return None
    code = int(m.group(1))
    dash = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "DashboardActivity.kt")
    if os.path.exists(dash):
        d = read(dash)
        # 🔴 TK-ORDER (31.07.2026, Section E — "Guard যদি পুরনো Button Design
        # খুঁজে Fail করে, Guard-কেই বর্তমান approved Design অনুযায়ী আপডেট
        # করবেন")। খাতার সারি B225: এখানে আগে "V210" হার্ডকোড ছিল (তাই
        # প্রতিটা নতুন ভার্সনে হাতে বদলাতে ভুলে যাওয়ার ঝুঁকি ছিল) — এখন
        # সরাসরি BuildConfig.VERSION_CODE থেকে ডায়নামিকভাবে বসে, তাই এই
        # ফাইলে আর কখনো literal "V<সংখ্যা>" থাকবে না, versionCode বদলালেও
        # আপনা থেকেই ঠিক দেখাবে। এই ডায়নামিক প্যাটার্নটাও এখন বৈধ ধরা হয় —
        # শুধু hardcoded ভুল সংখ্যা থাকলেই আসল সমস্যা ধরা পড়বে।
        uses_dynamic = "BuildConfig.VERSION_CODE" in d and 'val vLabel' in d
        if f"· V{code}" not in d and not uses_dynamic:
            fail("৯.৮", f"Dashboard-এ V{code} লেখা নেই")
        old = re.findall(r'· V(\d+)', d)
        if any(int(x) != code for x in old):
            fail("৯.৮", f"Dashboard-এ পুরনো ভার্সন রয়ে গেছে → V{[x for x in old if int(x)!=code]}")

    # 🔴🔒 TK-নির্দেশ (20.08.2026) — "ফাইলের নাম, ভার্সনের নাম, ভেতরের সমস্ত
    # জায়গার নাম যেন সঠিকভাবে থাকে, তবেই পাহারাদার অনুমতি দেবে।" আগে
    # `03_NETLIFY_READY/version.json` (Web-এর নিজের ভার্সন-ঘোষণা) শুধু একটা
    # **আলাদা** স্ক্রিপ্টে (`verify_version_json.py`) চেক হত, মূল পাহারাদারের
    # ভেতরে না — তাই ভুলে সেটা না চালালেও মূল ছাড়পত্র মিলে যেত। এখন সেটাও
    # এখানেই, একই বাধ্যতামূলক গেটে।
    vj = os.path.join(WEB, "version.json")
    if os.path.exists(vj):
        try:
            data = json.loads(read(vj))
            vc = data.get("versionCode")
            if vc != code:
                fail("৯.৮", f"03_NETLIFY_READY/version.json-এ versionCode {vc} — build.gradle.kts-এর V{code}-এর সাথে মেলেনি")
            vn = data.get("versionName", "")
            m2 = re.search(r'val\s+appVersionName\s*=\s*"([\d.]+)"', s)
            if m2 and vn and m2.group(1) != vn:
                fail("৯.৮", f"03_NETLIFY_READY/version.json-এর versionName \"{vn}\" — build.gradle.kts-এর \"{m2.group(1)}\"-এর সাথে মেলেনি")
        except Exception:
            fail("৯.৮", "03_NETLIFY_READY/version.json পড়া যায়নি (ভাঙা JSON?)")
    else:
        fail("৯.৮", "03_NETLIFY_READY/version.json পাওয়া যায়নি")

    return code



# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১০ — ক্লাসের নাম দিয়ে সরাসরি ফাংশন ডাকা হয়নি তো?
#  🚨 TK-REPORTED, LIVE (29.07.2026 দুপুর ১.০০ · খাতার সারি B85):
#  Android Studio-তে `Unresolved reference: post` — কারণ
#  `BriefingRepository` একটা **class**, `object` নয়; তবু
#  `BriefingRepository.post(...)` লেখা ছিল। Kotlin-এ ক্লাসের নাম দিয়ে
#  instance-ফাংশন ডাকা যায় না, আগে `BriefingRepository()` বানাতে হয়।
#  পাহারাদার এতদিন এটা ধরতে পারত না — এখন পারবে।
# ═══════════════════════════════════════════════════════════════
def _blank_comments(s: str) -> str:
    """কমেন্ট ও স্ট্রিং-এর ভিতরের সব অক্ষর ফাঁকা করে দেয়, কিন্তু **লাইন-সংখ্যা ও
    দৈর্ঘ্য অটুট** রাখে (নতুন লাইন যেমন ছিল তেমনই)। ফলে লাইন নম্বর ধরে যাচাই
    করা কোড আগের মতোই চলে, শুধু কমেন্টের লেখা আর কোড বলে ভুল হয় না।
    🔴🔒 V800 — TK-এর যাচাইয়ে ধরা পড়ল: RoleSession.kt-এর বাংলা `/** … */`
    মন্তব্যের ভিতরে `PilesClinicApplication.onCreate()` লেখা ছিল, আর সেই
    লাইনটা `*` দিয়ে শুরু হয় না বলে পুরনো ছাঁকনি ওটাকে **কোড** ভেবে
    মিথ্যে ভুল দেখাচ্ছিল।"""
    out = list(s)
    n = len(s)
    i = 0
    def blank(a, b):
        for k in range(a, min(b, n)):
            if out[k] != "\n":
                out[k] = " "
    while i < n:
        c = s[i]
        if c == "/" and i + 1 < n and s[i + 1] == "/":
            j = s.find("\n", i)
            j = n if j < 0 else j
            blank(i, j); i = j; continue
        if c == "/" and i + 1 < n and s[i + 1] == "*":
            j = s.find("*/", i + 2)
            j = n if j < 0 else j + 2
            blank(i, j); i = j; continue
        if s.startswith('\"\"\"', i):
            j = s.find('\"\"\"', i + 3)
            j = n if j < 0 else j + 3
            blank(i, j); i = j; continue
        if c in "\"'":
            j = i + 1
            while j < n and s[j] != c and s[j] != "\n":
                if s[j] == "\\":
                    j += 1
                j += 1
            j = min(j + 1, n)
            blank(i, j); i = j; continue
        i += 1
    return "".join(out)


def _brace_block(s: str, start: int) -> str:
    """`start` থেকে শুরু করে প্রথম `{` খুঁজে তার মিল-করা `}` পর্যন্ত অংশটা ফেরায়।
    কমেন্ট (`//`, `/* */`) ও স্ট্রিং (`"`, `\'`, `\"\"\"`) -এর ভিতরের বন্ধনী গোনা হয় না,
    তাই বাংলা মন্তব্যে `{` থাকলেও ভুল হবে না। মিল না পেলে ফাইলের শেষ পর্যন্ত।"""
    i = s.find("{", start)
    if i < 0:
        return s[start:]
    n = len(s)
    depth = 0
    while i < n:
        c = s[i]
        if c == "/" and i + 1 < n and s[i + 1] == "/":
            j = s.find("\n", i)
            i = n if j < 0 else j + 1
            continue
        if c == "/" and i + 1 < n and s[i + 1] == "*":
            j = s.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        if s.startswith('\"\"\"', i):
            j = s.find('\"\"\"', i + 3)
            i = n if j < 0 else j + 3
            continue
        if c in "\"'":
            j = i + 1
            while j < n and s[j] != c:
                if s[j] == "\\":
                    j += 1
                j += 1
            i = j + 1
            continue
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return s[start:i + 1]
        i += 1
    return s[start:]


def check_static_calls():
    files = {}
    for root, _, fs in os.walk(JAVA):
        for f in fs:
            if f.endswith(".kt"):
                q = os.path.join(root, f)
                files[q] = read(q)

    objects = set()
    classes = {}
    for q, s in files.items():
        for m in re.finditer(r'^\s*(?:internal\s+|private\s+|public\s+)?object\s+([A-Z]\w*)', s, re.M):
            objects.add(m.group(1))
        for m in re.finditer(r'^\s*(?:internal\s+|private\s+|public\s+|open\s+|abstract\s+|data\s+|sealed\s+)*class\s+([A-Z]\w*)', s, re.M):
            classes.setdefault(m.group(1), q)

    # companion object-এর ভিতরের নামগুলো (ওগুলো ক্লাসের নামেই ডাকা যায়)
    comp = {}
    for cname, q in classes.items():
        s = files[q]
        i = s.find("class " + cname)
        if i < 0:
            continue
        j = s.find("companion object", i)
        if j < 0:
            continue
        # 🔴🔒 V800 (২৮.০৮.২০২৬) — আগে এখানে লেখা ছিল `s[j:j+8000]`, অর্থাৎ
        # companion object-এর প্রথম ৮০০০ অক্ষরই দেখা হত। বড় কমেন্ট বা বড়
        # companion থাকলে পরের `fun`-গুলো জানালার বাইরে পড়ে যেত, আর
        # পাহারাদার **মিথ্যে ভুল** দেখাত (FollowUpRepository-তে ঠিক এটাই হলো:
        # `inquiryHistoryEndsTerminal` ১৬০ নম্বর লাইনে, কিন্তু ৮০০০ অক্ষরের
        # বাইরে)। এখন সত্যিকারের `{`…`}` গুনে companion-এর শেষ বার করা হয় —
        # কমেন্ট ও স্ট্রিং-এর ভিতরের বন্ধনী গোনা হয় না।
        blk = _brace_block(s, j)
        comp[cname] = set(re.findall(r'\bfun\s+(\w+)', blk)) | set(re.findall(r'\b(?:val|var|const val)\s+(\w+)', blk))

    bad = []
    for q, s in files.items():
        own = os.path.basename(q)[:-3]
        # 🔴🔒 V800 — কমেন্ট/স্ট্রিং ফাঁকা করে নিয়ে তবেই খোঁজা হয়
        for ln, line in enumerate(_blank_comments(s).split("\n"), 1):
            t = line.strip()
            if t.startswith("//") or t.startswith("*") or t.startswith("/*"):
                continue
            code = line.split("//")[0]
            for m in re.finditer(r'(?<![\w.])([A-Z]\w*)\.([a-z]\w*)\s*\(', code):
                cn, mn = m.group(1), m.group(2)
                if cn in objects or cn not in classes:
                    continue
                if mn in comp.get(cn, set()):
                    continue
                if cn == own:                       # নিজের ফাইলের ভিতরে (nested/companion)
                    continue
                if re.search(r'\bfun\s+' + cn + r'\.', code):   # extension function ঘোষণা
                    continue
                bad.append((q, ln, cn, mn))
    if bad:
        for q, ln, cn, mn in bad[:8]:
            fail("৯.১০", f"{os.path.relpath(q, ROOT)}:{ln} — `{cn}` একটা class, তবু `{cn}.{mn}()` ডাকা হয়েছে (আগে `{cn}()` বানাতে হবে)")
    return len(files)


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১৮ — bare ক্লাস-নাম ব্যবহার হয়েছে অথচ import নেই (Unresolved
#  reference)।
#  🔴🔴🔴🔴 TK-REPORTED, LIVE (20.08.2026, Android Studio Build Output
#  ছবিসহ — "Unresolved reference: JSONArray/JSONObject/optString",
#  APK তৈরিই হয়নি): `ChamberAttendanceRepository.kt`-এ নতুন কোড
#  `JSONArray(`/`JSONObject(` (সংক্ষিপ্ত নাম) ব্যবহার করেছিল, কিন্তু
#  ফাইলের উপরে `import org.json.JSONArray`/`JSONObject` ছিল না — পুরনো
#  কোড সবসময় `org.json.JSONArray` পুরো-নাম লিখত। brace/paren গোনার
#  পুরনো চেক (৯.১) এটা ধরতে পারেনি (bracket ঠিকই মিলছিল), ঠিক
#  check_qualified_extension_fn()-এর (৯.১৫) মতোই একটা নাম-ভিত্তিক
#  ফাঁক।
#
#  🚨 TK-এর স্পষ্ট নির্দেশ: এই ধরনের ভুল থাকলে পাহারাদার যেন **কখনোই**
#  ফাইল পাঠানোর অনুমতি না দেয় — অক্ষরে অক্ষরে পালন করা হচ্ছে।
#
#  পদ্ধতি: এই তালিকার ("সংক্ষিপ্ত নামে ডাকা যায়, কিন্তু import ছাড়া
#  Kotlin/Android নিজে থেকে চেনে না" এমন) প্রতিটা ক্লাসের জন্য —
#  ফাইলে bare ব্যবহার (constructor call/type annotation/generic-এর
#  ভিতরে) থাকলে, কিন্তু ওই একই ফাইলে সংশ্লিষ্ট `import` না থাকলে,
#  ব্যর্থ ধরা হয়। ভবিষ্যতে একই ধরনের নতুন bug পেলে এই তালিকায় নতুন
#  ক্লাস যোগ করলেই যথেষ্ট।
#  ⛔ যেসব ফাইলে সব ব্যবহারই পুরো-নাম (`org.json.JSONArray`) দিয়ে করা,
#     সেখানে bare ব্যবহারই নেই, তাই ধরাও পড়ে না (মিথ্যা-এলার্ম নেই)।
# ═══════════════════════════════════════════════════════════════
NEEDS_IMPORT = {
    "JSONArray": "org.json.JSONArray",
    "JSONObject": "org.json.JSONObject",
}

def check_unresolved_imports():
    for root, _, fs in os.walk(JAVA):
        for f in fs:
            if not f.endswith(".kt"):
                continue
            q = os.path.join(root, f)
            s = read(q)
            imported = set(re.findall(r'^\s*import\s+([\w.]+)\s*$', s, re.M))
            for short_name, full_name in NEEDS_IMPORT.items():
                if full_name in imported:
                    continue   # import আছে, নিরাপদ
                # bare ব্যবহার খোঁজা — আগে কোনো '.' না থাকলেই bare
                # (org.json.JSONArray(...) এই প্যাটার্নে ম্যাচ করবে না,
                #  কারণ তার আগে '.' আছে)
                pattern = re.compile(r'(?<![\w.])' + short_name + r'(?:\s*\(|\s*\?|\s*>|\s*,)')
                lines = s.split("\n")
                bad_lines = []
                for ln, line in enumerate(lines, 1):
                    t = line.strip()
                    if t.startswith("//") or t.startswith("*") or t.startswith("/*"):
                        continue
                    code = line.split("//")[0]
                    # পুরো-নামে ব্যবহার (org.json.JSONArray) হলে বাদ
                    code_no_qualified = re.sub(r'org\.json\.' + short_name, '', code)
                    if pattern.search(code_no_qualified):
                        bad_lines.append(ln)
                if bad_lines:
                    fail("৯.১৮", f"{os.path.relpath(q, ROOT)}:{bad_lines[0]} — `{short_name}` ব্যবহার হয়েছে কিন্তু `import {full_name}` নেই (Unresolved reference — build ব্যর্থ হবে)")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১৫ — kotlinx.coroutines-এর extension function প্যাকেজ-নাম
#  দিয়ে সরাসরি ডাকা হয়নি তো?
#  🔴🔴🔴 TK-REPORTED, LIVE (30.07.2026 রাত, ছবিসহ — Android Studio-তে সত্যিকারের
#  বিল্ড ব্যর্থ, "app:compileDebugKotlin 2 errors" · খাতার সারি B203):
#  `async` ও `launch` — এই দুটো Kotlin-এ `CoroutineScope`-এর **extension
#  function**, সাধারণ টপ-লেভেল ফাংশন নয়। `kotlinx.coroutines.async(...)`-এর
#  মতো সরাসরি প্যাকেজ-নাম জুড়ে ডাকলে Kotlin কম্পাইলার "Unresolved reference"
#  বলে আটকে যায় — এটাই এই সেশনে সত্যিকারের বিল্ড ভেঙেছিল, আর ব্র্যাকেট/প্যারেন
#  গোনার পুরনো চেক (৯.১) এটা ধরতে পারেনি কারণ ব্র্যাকেট ঠিকই মিলছিল।
#  ⛔ `coroutineScope`/`withContext`/`runBlocking` টপ-লেভেল ফাংশন — প্যাকেজ-নাম
#     জুড়ে ডাকলেও ঠিকই আছে, তাই এগুলো এই চেকে ধরা হয় না।
# ═══════════════════════════════════════════════════════════════
def check_qualified_extension_fn():
    bad = []
    for f in kt_files():
        s = read(f)
        for i, line in enumerate(s.split("\n")):
            # ⛔ // লাইন-কমেন্টের ভিতরে উদাহরণ হিসেবে এই প্যাটার্ন লেখা থাকতে
            # পারে (যেমন এই বাগ ব্যাখ্যা করা কমেন্টেই) — তাই কোড-অংশটুকুই
            # (// এর আগে পর্যন্ত) দেখা হয়, পুরো লাইন নয়।
            code_part = line.split("//", 1)[0]
            m = re.search(r'kotlinx\.coroutines\.(async|launch)\s*\(', code_part)
            if m:
                bad.append((f, i + 1, m.group(1)))
    if bad:
        for f, ln, name in bad[:8]:
            fail("৯.১৫", f"{os.path.relpath(f, ROOT)}:{ln} — `kotlinx.coroutines.{name}(...)` সরাসরি প্যাকেজ-নাম দিয়ে ডাকা হয়েছে ({name} একটা extension function, import করে unqualified ডাকতে হবে — নইলে বিল্ড ভাঙবে)")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১৬ — এমন View ক্লাসের নিজের `.LayoutParams(` ডাকা হয়নি তো,
#  যার আসলে নিজস্ব LayoutParams ক্লাসই নেই?
#  🔴🔴🔴 TK-REPORTED, LIVE (02.08.2026 সকাল, Android Studio build error
#  ছবিসহ — "Unresolved reference: LayoutParams" · খাতার সারি B266-সংশোধন):
#  `ScrollView`/`HorizontalScrollView` নিজেদের কোনো `LayoutParams` ক্লাস
#  ঘোষণা করে না (এরা `FrameLayout`-এর সাব-ক্লাস) — তাই
#  `android.widget.ScrollView.LayoutParams(...)` লিখলে Kotlin কম্পাইলার
#  "Unresolved reference" বলে আটকে যায়। সঠিক লেখা:
#  `FrameLayout.LayoutParams(...)`। ব্র্যাকেট/প্যারেন গোনার পুরনো চেক (৯.১)
#  এটা ধরতে পারেনি কারণ ব্র্যাকেট ঠিকই মিলছিল।
# ═══════════════════════════════════════════════════════════════
_NO_OWN_LAYOUTPARAMS = ("ScrollView", "HorizontalScrollView")


def check_fake_layoutparams_class():
    bad = []
    for f in kt_files():
        s = read(f)
        for i, line in enumerate(s.split("\n")):
            code_part = line.split("//", 1)[0]
            for cls in _NO_OWN_LAYOUTPARAMS:
                if re.search(rf'\b{cls}\.LayoutParams\s*[.(]', code_part):
                    bad.append((f, i + 1, cls))
    if bad:
        for f, ln, cls in bad[:8]:
            fail("৯.১৬", f"{os.path.relpath(f, ROOT)}:{ln} — `{cls}.LayoutParams` লেখা হয়েছে, কিন্তু `{cls}`-এর নিজস্ব কোনো LayoutParams ক্লাস নেই (এটা `FrameLayout`-এর সাব-ক্লাস) — `FrameLayout.LayoutParams` ব্যবহার করুন, নইলে বিল্ড ভাঙবে")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১৭ — একা `InputType.TYPE_CLASS_NUMBER` (কোনো DigitsKeyListener
#  ছাড়া) কোনো প্রোগ্রাম্যাটিক EditText-এ বসানো হয়নি তো?
#  🔴🔴🔴 TK-REPORTED (04.08.2026, Laxmi/Kishanganj-এর "সংখ্যা-ঘরে চাপ দিলে
#  কীবোর্ড আসে না" — খাতার সারি B408→B409→B411): কিছু ফোনের (Xiaomi/Vivo/
#  Oppo-ঘরানার) কাস্টম কীবোর্ড একা-`TYPE_CLASS_NUMBER`-এ tap করলে numeric
#  keypad খুলতেই ব্যর্থ হয় — ১৭ জায়গায় (টাকার ঘরসহ) এই বাগ পাওয়া গিয়েছিল।
#  সমাধান: `TYPE_CLASS_TEXT` + `DigitsKeyListener` (একই লাইনে বা তার
#  খুব কাছাকাছি)। এই চেক একা `TYPE_CLASS_NUMBER` (ORর সাথেও, যেমন পুরনো
#  `TYPE_NUMBER_FLAG_DECIMAL`-সহ কম্বিনেশনেও) থাকা প্রতিটা লাইন ধরে —
#  `DigitsKeyListener` ব্যবহার হলে সেটাকে নিরাপদ ধরা হয়, `TYPE_CLASS_TEXT`
#  দিয়ে কল হলেও নিরাপদ (already-fixed pattern)।
# ═══════════════════════════════════════════════════════════════
def check_bare_number_input():
    bad = []
    for f in kt_files():
        s = read(f)
        for i, line in enumerate(s.split("\n")):
            code_part = line.split("//", 1)[0]
            if "TYPE_CLASS_NUMBER" in code_part and "DigitsKeyListener" not in code_part:
                bad.append((f, i + 1))
    if bad:
        for f, ln in bad[:8]:
            fail("৯.১৭", f"{os.path.relpath(f, ROOT)}:{ln} — একা `TYPE_CLASS_NUMBER` (কোনো `DigitsKeyListener` ছাড়া) — কিছু ফোনে কীবোর্ড খুলবে না, `TYPE_CLASS_TEXT` + `DigitsKeyListener` ব্যবহার করুন (B411)")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১৯ — Follow-up কার্ডের ট্যাগ জমানো তালিকাতেও থাকতে হবে
#  🔴🔴🔴 TK-REPORTED (২৬.০৮.২০২৬, ছবিসহ — *"Tag এ Unexpected লেখা নেই,
#  কিন্তু View All-এ ক্লিক করলে আছে"*)। TK-এর কথা: *"একই ধরনের সমস্যা
#  আপনাকে প্রত্যেকবার কেন বলতে হয়, একবারে কেন ঠিক করতে পারেন না"*।
#
#  **আসল কারণ ছিল:** `FollowUpRepository.saveCachedTab()`-এ `timeType` ·
#  `refDoctor` · `addressTag` — এই তিনটে ঘর লেখাই হত না। তাই লাইভ তালিকা
#  আসার আগে (বা লাইন খারাপ থাকলে চিরকাল) কার্ডে ঠিকানার ট্যাগ · UNEXPECTED ·
#  RMP উধাও থাকত, অথচ View All-এ ঠিকই দেখা যেত।
#
#  এই পাহারা দুটো জিনিস দেখে, যাতে দোষটা আর কখনো ফিরতে না পারে:
#   ১) কার্ডে দেখানো প্রতিটা দরকারি ঘর জমানো তালিকায় **লেখা হয়** কি না
#   ২) জমানো তালিকা **পড়ার** সময় যে ঘরগুলো চাওয়া হয়, লেখার সময়ও সেগুলো
#      বসে কি না (লেখা-পড়া কখনো আলাদা হয়ে যেতে পারবে না)
# ═══════════════════════════════════════════════════════════════
MUST_CACHE_FIELDS = [
    "timeType", "refDoctor", "addressTag", "lastCallDate", "lastCallBy",
    "nextFollow", "callCount", "patientId", "bill", "paid", "address",
]


def check_followup_cache_fields():
    f = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                     "java", "com", "tkbiswas", "pilesclinic", "native", "FollowUpRepository.kt")
    s = read(f)
    if not s:
        return
    i = s.find("fun saveCachedTab")
    j = s.find("fun loadCachedTab")
    if i < 0 or j < 0:
        fail("৯.১৯", "FollowUpRepository.kt-এ saveCachedTab/loadCachedTab খুঁজে পাওয়া গেল না")
        return
    save_body = s[i:s.find("\n    fun ", i + 10)]
    # ⛔ শুধু **জমানো তালিকা পড়ার** অংশটুকু — নিচের `mergeOwnPhoneRows()` অন্য
    #    উৎস (ফোনের নিজের সেভ করা সারি), তার ঘরের নাম আলাদা হতেই পারে।
    load_all = s[j:s.find("\n    fun ", j + 10)]
    a = load_all.find("val arr = JSONArray(json)")
    load_body = load_all[a:load_all.find("catch", a)] if a >= 0 else ""
    # ⛔ মন্তব্য করে দেওয়া লাইন গোনা চলবে না — নইলে কেউ `//` দিয়ে ঢেকে দিলেই
    #    পাহারাদার ঠকে যেত (নিজের নেগেটিভ-টেস্টেই এটা ধরা পড়েছে)।
    def no_comments(txt):
        return "\n".join(ln.split("//", 1)[0] for ln in txt.split("\n"))
    save_body = no_comments(save_body)
    load_body = no_comments(load_body)
    written = set(re.findall(r'\.put\("([A-Za-z]+)"', save_body))
    for name in MUST_CACHE_FIELDS:
        if name not in written:
            fail("৯.১৯", f"FollowUpRepository.saveCachedTab()-এ `{name}` লেখা হচ্ছে না — "
                         f"জমানো তালিকা দেখানোর সময় কার্ডে ওটা উধাও থাকবে (TK, ২৬.০৮.২০২৬)")
    read_names = set(re.findall(r'r\.optString\("([A-Za-z]+)"', load_body)) | \
                 set(re.findall(r'r\.s\("([A-Za-z]+)"\)', load_body))
    for name in sorted(read_names - written):
        fail("৯.১৯", f"FollowUpRepository — জমানো তালিকা পড়ার সময় `{name}` চাওয়া হয়, "
                     f"কিন্তু লেখার সময় বসানো হয় না (লেখা-পড়া আলাদা হয়ে গেছে)")

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২৪ — মেঘ থেকে আসা লোকের `name` ঘরে **কোডই** আছে তো?
#  ─────────────────────────────────────────────────────────────
#  🔴🔴🔴 TK-রিপোর্ট (২৭.০৮.২০২৬, ছবিসহ): ROHINI-র নম্বর দিয়ে ঢোকা গেল,
#  কিন্তু মডিউল খুলতে গিয়ে *"Could not open — Sign-in failed"*।
#
#  **আসল কারণ:** মডিউলের পরিচয় বার হয় `ModuleAuth.expectedCode()` থেকে,
#  আর সেটা পড়ে **`user.name.uppercase()`** — অর্থাৎ প্রজেক্টে `name` ঘরে
#  চিরকাল **কোড** থাকে (KNE-LAXMI · KNE-BRANCH …), মানুষের নাম নয়।
#  সার্ভারের auth-ইমেলও তৈরি হয় **কোড** থেকে (`kne-laxmi@staff.piles`)।
#  তাই মেঘের তালিকা পার্স করার সময় ভুল করে `full_name` বসালে ইমেল হত
#  `raju-das@staff.piles` — যা নেই ⇒ **প্রতিটা নতুন লোকের মডিউল বন্ধ**।
#
#  TK-এর নিয়ম ৬.২ (*"একবারে কেন ঠিক করতে পারেন না"*) মেনে ফোন **ও**
#  কম্পিউটার — দুই জায়গাতেই পাহারা বসানো হলো।
# ═══════════════════════════════════════════════════════════════
def check_cloud_login_name_is_code():
    """মেঘের লোকের **কোড** কোথা থেকে আসে — নাম থেকে নয়, `person_code` থেকে।"""
    kt = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic",
                      "native", "CloudStaffDirectory.kt")
    ma = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic",
                      "modules", "ModuleAuth.kt")

    # ── ফোন, ধাপ ১: জমানো তালিকা থেকে কোড বার করার পথটা আছে তো? ──────────
    if not os.path.exists(kt):
        fail("৯.২৪", "CloudStaffDirectory.kt খুঁজে পাওয়া গেল না")
    else:
        s_kt = read(kt)
        if "fun cachedCodeFor(" not in s_kt:
            fail("৯.২৪", "CloudStaffDirectory-তে `cachedCodeFor()` নেই — তাহলে মডিউলের "
                         "কোড নাম থেকে বার হবে ⇒ নতুন লোকের Sign-in failed হবে")
        elif "person_code" not in s_kt.split("fun cachedCodeFor(")[1][:1200]:
            fail("৯.২৪", "`cachedCodeFor()` `person_code` পড়ছে না")

    # ── ফোন, ধাপ ২: `expectedCode()` সত্যিই ওটা ব্যবহার করছে তো? ─────────
    if not os.path.exists(ma):
        fail("৯.২৪", "ModuleAuth.kt খুঁজে পাওয়া গেল না")
    else:
        s_ma = read(ma)
        if "fun expectedCode(" not in s_ma:
            fail("৯.২৪", "ModuleAuth-এ `expectedCode()` খুঁজে পাওয়া গেল না")
        else:
            seg = s_ma.split("fun expectedCode(")[1][:2600]
            if "cachedCodeFor" not in seg:
                fail("৯.২৪", "ModuleAuth.expectedCode() মেঘের `cachedCodeFor()` ব্যবহার করছে না "
                             "⇒ অ্যাপ থেকে যোগ করা লোক কোনো মডিউল খুলতে পারবেন না")
            # ⚠️ পুরনো ২৩ জন যেন কখনো এই নতুন পথে না ঢোকেন — শর্তটা থাকতেই হবে।
            elif "StaffDirectory.findAccount(mobile) == null" not in seg:
                fail("৯.২৪", "ModuleAuth.expectedCode() — মেঘের কোড নেওয়ার আগে "
                             "`StaffDirectory.findAccount(mobile) == null` শর্তটা নেই; "
                             "শর্ত ছাড়া বাঁধা তালিকার ২৩ জনের পরিচয়ও বদলে যেতে পারে")

    # ── কম্পিউটার: `code` ঘরে person_code, আর expectedCode সেটা পড়ে তো? ──
    js = os.path.join(WEB, "app.js")
    mc = os.path.join(WEB, "module_core.js")
    if not os.path.exists(js) or not os.path.exists(mc):
        fail("৯.২৪", "app.js বা module_core.js খুঁজে পাওয়া গেল না")
        return
    s_js, s_mc = read(js), read(mc)
    if "staff_login_list" not in s_js:
        return          # ওয়েবে মেঘ-লগইন নেই — পাহারার কিছু নেই
    # ⚠️ `staff_login_list` ফাইলে দুবার আছে (একটা কমেন্টে, একটা আসল কলে) —
    #    তাই `split(...)[1]` ভুল টুকরো নিত। নিজের ফাঁদ-পরীক্ষাতেই ধরা পড়েছে।
    push = s_js.split("cfg2.users[rk].push(")
    if len(push) < 2:
        fail("৯.২৪", "app.js — মেঘ-লগইনে ব্যবহারকারী যোগ করার লাইন খুঁজে পাওয়া গেল না")
        return
    seg = push[1].split("});")[0]
    if "code:" not in seg:
        fail("৯.২৪", "app.js — মেঘ-লগইনে `code:` ঘরটাই নেই ⇒ নতুন লোকের মডিউল খুলবে না")
    else:
        val = seg.split("code:")[1].split(",")[0]
        if "person_code" not in val:
            fail("৯.২৪", "app.js — মেঘ-লগইনে `code:` ঘরে `person_code` বসানো হয়নি")
    # ⚠️ `MOD.expectedCode` ফাইলে কয়েকবার আছে (সংজ্ঞা + কল)। `[-1]` নিলে
    #    শেষ **কল**-টা আসত, সংজ্ঞা নয় — নিজের পরীক্ষাতেই ধরা পড়েছে।
    if "MOD.expectedCode = function" not in s_mc:
        fail("৯.২৪", "module_core.js — `MOD.expectedCode` সংজ্ঞাটাই খুঁজে পাওয়া গেল না")
    elif "raw.code" not in s_mc.split("MOD.expectedCode = function")[1][:900]:
        fail("৯.২৪", "module_core.js — `MOD.expectedCode()` `raw.code` পড়ছে না "
                     "⇒ নতুন লোকের মডিউলে Sign-in failed হবে")

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২৫ — কম্পিউটারের ফাইল বদলেছে, অথচ cache-নম্বর বদলায়নি?
#  ─────────────────────────────────────────────────────────────
#  🔴🔴🔴 ২৭.০৮.২০২৬-এ ধরা পড়া আসল ভুল: V746-এ `app.js` বদলানো হয়েছিল,
#  কিন্তু `index.html`-এর `app.js?v=v712` অপরিবর্তিত ছিল। ফলে ব্রাউজার
#  **পুরনো ফাইলটাই** ধরে রাখত ⇒ Netlify-তে তুললেও TK কোনো বদল দেখতেন না,
#  আর "কাজ করছে না" বলে ভুল খোঁজাখুঁজি হত।
#
#  **পাহারাটা কীভাবে কাজ করে**
#   · `index.html`-এ `xxx.js?v=NNN` ধাঁচের প্রতিটা ফাইলের বিষয়বস্তুর
#     আঙুলছাপ (sha) `00_GUARD/web_cache_hash.json`-এ জমা থাকে।
#   · আঙুলছাপ বদলেছে **কিন্তু** `v=` একই ⇒ **আটকায়**।
#   · দুটোই বদলেছে ⇒ ঠিক আছে, নতুন আঙুলছাপ জমা হয়।
#   ⇒ তাই ভবিষ্যতে ওয়েবের কোনো ফাইল বদলে cache-নম্বর ভুলে গেলে guard ধরবে।
# ═══════════════════════════════════════════════════════════════
def check_web_cache_busters():
    import hashlib, json as _json
    idx = os.path.join(WEB, "index.html")
    ledger = os.path.join(os.path.dirname(os.path.abspath(__file__)), "web_cache_hash.json")
    if not os.path.exists(idx):
        fail("৯.২৫", "index.html খুঁজে পাওয়া গেল না")
        return
    html = read(idx)
    found = re.findall(r'(?:src|href)="([A-Za-z0-9_.-]+\.(?:js|css))\?v=([A-Za-z0-9_.-]+)"', html)
    if not found:
        fail("৯.২৫", "index.html-এ `?v=` সহ কোনো js/css পাওয়া গেল না")
        return
    try:
        old = _json.load(io.open(ledger, encoding="utf-8")) if os.path.exists(ledger) else {}
    except Exception:
        old = {}
    new = {}
    before = len(problems)          # ⛔ শুধু **এই** যাচাইয়ের অভিযোগ গোনা হয়
    for fname, ver in found:
        fpath = os.path.join(WEB, fname)
        if not os.path.exists(fpath):
            fail("৯.২৫", f"index.html `{fname}` চাইছে, কিন্তু ফাইলটা নেই")
            continue
        sha = hashlib.sha256(io.open(fpath, "rb").read()).hexdigest()
        new[fname] = {"v": ver, "sha": sha}
        prev = old.get(fname)
        if prev and prev.get("sha") != sha and prev.get("v") == ver:
            fail("৯.২৫", f"`{fname}` বদলেছে, কিন্তু index.html-এ cache-নম্বর `?v={ver}` "
                         f"একই রয়ে গেছে ⇒ ব্রাউজার পুরনো ফাইলই ধরে রাখবে, "
                         f"TK কোনো বদল দেখবেন না। নম্বরটা বাড়ান।")
    # ⛔ অভিযোগ থাকলে খাতা লেখা হয় না — নইলে ভুলটা এক দৌড়েই চাপা পড়ে যেত।
    if len(problems) == before:
        try:
            io.open(ledger, "w", encoding="utf-8").write(
                _json.dumps(new, indent=2, ensure_ascii=False) + "\n")
        except Exception:
            pass

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২৬ — ফোনের নিজের সাজেশন বন্ধ রাখার পাহারা
#  ─────────────────────────────────────────────────────────────
#  🔴 TK-রিপোর্ট (২৭.০৮.২০২৬, ৪টা ছবিসহ): *"এরকম যেন সাজেস্ট না করে"* —
#  নাম · মোবাইল · টাকার ঘরে ফোনের পুরনো লেখা ভেসে উঠছিল।
#
#  V418-এ শুধু পর্দার মূল বাক্সে `importantForAutofill` বসেছিল — সেটা
#  Google-এর Autofill থামায়, **কীবোর্ডের নিজের সাজেশন থামায় না**। আর
#  পপ-আপের আলাদা উইন্ডোতে সেটা পৌঁছাতই না।
#
#  V752-এ ৩টে জিনিস বসেছে; এই পাহারা তিনটেই টিকিয়ে রাখে:
#    ১. `NoAutofill.scrub()` — প্রতিটা লেখার ঘরে NO_PERSONALIZED_LEARNING
#    ২. `apply()` থেকে `scrub()` ডাকা হয় (নইলে পর্দায় খাটবে না)
#    ৩. `PremiumAlert.paint()`-এর **দুটোতেই** `scrubDialogWindow()`
#       (নইলে পপ-আপে আবার সাজেশন ফিরে আসবে)
# ═══════════════════════════════════════════════════════════════
def check_no_autofill_kept():
    na = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "NoAutofill.kt")
    pa = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "PremiumAlert.kt")
    if not os.path.exists(na):
        fail("৯.২৬", "NoAutofill.kt খুঁজে পাওয়া গেল না")
    else:
        s_na = read(na)
        if "fun scrub(" not in s_na and "private fun scrub(" not in s_na:
            fail("৯.২৬", "NoAutofill-এ `scrub()` নেই ⇒ কীবোর্ডের সাজেশন আবার আসবে")
        if "IME_FLAG_NO_PERSONALIZED_LEARNING" not in s_na:
            fail("৯.২৬", "NoAutofill-এ NO_PERSONALIZED_LEARNING পতাকাটা নেই")
        if "fun scrubDialogWindow(" not in s_na:
            fail("৯.২৬", "NoAutofill-এ `scrubDialogWindow()` নেই ⇒ পপ-আপে সাজেশন ফিরবে")
        if "fun apply(" in s_na and "scrub(root)" not in s_na:
            fail("৯.২৬", "NoAutofill.apply() থেকে `scrub(root)` ডাকা হচ্ছে না")
    if not os.path.exists(pa):
        fail("৯.২৬", "PremiumAlert.kt খুঁজে পাওয়া গেল না")
        return
    s_pa = read(pa)
    n_paint = s_pa.count("fun paint(")
    n_hook = s_pa.count("NoAutofill.scrubDialogWindow(")
    if n_hook < n_paint:
        fail("৯.২৬", f"PremiumAlert-এ {n_paint} টা `paint()` আছে কিন্তু "
                     f"`NoAutofill.scrubDialogWindow()` ডাকা হয়েছে {n_hook} বার — "
                     f"যে পপ-আপগুলো বাদ পড়ল সেখানে ফোনের সাজেশন ফিরে আসবে")

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২৯ — কপি করা নম্বর যেন কীবোর্ডের সাজেশনে না ওঠে
#  ─────────────────────────────────────────────────────────────
#  🔴 TK-রিপোর্ট (২৮.০৮.২০২৬): *"যেকোনো ঘরে চাপ দিয়ে দেখলাম — নম্বরের
#  সাজেশন আসে, সম্পূর্ণ প্রজেক্টের সমস্ত জায়গায় আসতেছে।"*
#
#  **আসল কারণ (V772-এ ধরা):** সাজেশনটা Autofill-এর নয় — **কীবোর্ডের
#  ক্লিপবোর্ড-চিপ**। অ্যাপ ১৭টা জায়গায় রোগীর মোবাইল ক্লিপবোর্ডে রাখে
#  (Dialer · Chamber · Follow-up · Timeline …), আর Gboard সদ্য-কপি করা
#  লেখা ~১ ঘণ্টা ধরে **প্রত্যেক ঘরের** সাজেশন-পট্টিতে দেখায়।
#
#  ⇒ তাই কপি এখন একটাই দরজা দিয়ে যায় — `Clip.copy()` — যা লেখাটাকে
#    "গোপন" (IS_SENSITIVE) চিহ্ন দেয়। এই পাহারা নিশ্চিত করে যে
#    ভবিষ্যতে কেউ আবার সরাসরি `setPrimaryClip` লিখে ফাঁক তৈরি না করে।
# ═══════════════════════════════════════════════════════════════
def check_clip_sensitive():
    util = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "ClipboardUtil.kt")
    if not os.path.exists(util):
        fail("৯.২৯", "ClipboardUtil.kt খুঁজে পাওয়া গেল না")
        return
    s_u = read(util)
    if "object Clip" not in s_u or "fun copy(" not in s_u:
        fail("৯.২৯", "ClipboardUtil.kt-এ `object Clip` / `copy()` নেই ⇒ কপি করা নম্বর আবার সাজেশনে উঠবে")
    if "android.content.extra.IS_SENSITIVE" not in s_u:
        fail("৯.২৯", "Clip.copy()-তে IS_SENSITIVE পতাকাটা নেই ⇒ কীবোর্ড আবার নম্বর দেখাবে")
    bad = []
    for f in kt_files():
        if os.path.basename(f) == "ClipboardUtil.kt":
            continue
        txt = read(f)
        if "setPrimaryClip(" in txt:
            bad.append(os.path.basename(f))
    if bad:
        fail("৯.২৯", "সরাসরি `setPrimaryClip(` লেখা আছে — `Clip.copy()` দিয়ে যেতে হবে: "
                     + ", ".join(sorted(set(bad))))

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩০ — প্রতিটা পপ-আপেও কীবোর্ডের সাজেশন বন্ধ থাকতেই হবে
#  ─────────────────────────────────────────────────────────────
#  🔴 TK বহুবার বলেছেন: *"যেকোনো পর্দাতে মোবাইল নাম্বার সাজেস্ট"*।
#
#  **আসল কারণ (V774-এ প্রমাণসহ ধরা):** কীবোর্ড থামানোর পতাকা
#  `IME_FLAG_NO_PERSONALIZED_LEARNING` **প্রতিটা ঘরে আলাদা করে** বসাতে হয়,
#  উপরের বাক্সে বসালে ভিতরে নামে না। আর পপ-আপের **নিজের আলাদা উইন্ডো**,
#  তাই পর্দার পাহারা ওখানে পৌঁছায় না।
#
#  ⇒ নিয়ম: যে পপ-আপ `PremiumAlert.paint()` দিয়ে যায় না, তাকে নিজে
#    `NoAutofill.scrubAnyDialog(...)` ডাকতেই হবে — নইলে ওই পপ-আপের ঘরে
#    আবার পুরনো নম্বর ভেসে উঠবে।
# ═══════════════════════════════════════════════════════════════
def check_dialog_suggestion_guard():
    import re as _re
    na = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "NoAutofill.kt")
    if os.path.exists(na):
        s_na = read(na)
        # V774-এর তিনটে খুঁটি — একটাও সরলে সাজেশন ফিরে আসবে
        if "fun scrubAnyDialog(" not in s_na:
            fail("৯.৩০", "NoAutofill-এ `scrubAnyDialog()` নেই ⇒ পপ-আপে সাজেশন ফিরবে")
        if "keepScrubbing(" not in s_na or "addOnGlobalLayoutListener" not in s_na:
            fail("৯.৩০", "NoAutofill-এ layout-এর পরে বারবার মেলানো (`keepScrubbing`) নেই ⇒ "
                         "পরে তৈরি হওয়া ঘরে পতাকা বসবে না — এটাই ছিল আসল ফাঁক (V774)")
        # 🟢 V1505 (১৫.০৯.২০২৬, TK-নির্দেশ, স্টাফ-রিপোর্ট) — এই `restartInput`
        # চেকটা ছিল শুধু `IME_FLAG_NO_PERSONALIZED_LEARNING`-এর জন্য (খোলা
        # ঘরে পতাকা বসলে কীবোর্ডকে নতুন করে জানানো লাগত)। TK-নির্দেশে এই
        # পতাকাটাই `harden()` থেকে সরানো হয়েছে (এটাই Gboard-এর ভয়েস-টাইপিং
        # বন্ধ করে দিচ্ছিল — "অন্য অ্যাপে ভয়েস কাজ করে, এখানে করে না")।
        # ⛔ আসল সুরক্ষা (Android Autofill Framework — `importantForAutofill`
        # · `scrubAnyDialog` · `keepScrubbing` · প্রতি-উইন্ডো জাল) সব অক্ষত;
        # `importantForAutofill` বদলাতে `restartInput` লাগে না। মোবাইল-নম্বরের
        # ঘরে (`MobileInput.attach()`) পতাকাটা আলাদাভাবে এখনো বসানো আছে —
        # ওখানে ভয়েস বোতামই থাকে না বলে হারানোর কিছু নেই।
    bad = []
    for f in kt_files():
        lines = read(f).split("\n")
        for i, l in enumerate(lines):
            st = l.strip()
            if ".show()" not in l or st.startswith("//") or st.startswith("*"):
                continue
            # 🟢 V1538 (১৮.০৯.২০২৬) — আগে শুধু `.show()`-এর নিজের লাইনে "Toast"/
            # "Snackbar" খুঁজত। কিন্তু `Toast.makeText(...)` প্রায়ই কয়েক লাইনে
            # ভাঙা থাকে (আর্গুমেন্ট আলাদা লাইনে), তখন `.show()`-এর লাইনে একা
            # "Toast" শব্দটাই থাকে না — মিথ্যা রিপোর্ট হত (যেমন
            # PatientTimelineActivity.kt-এর "Duplicate blocked" Toast)।
            # এখন উপরের ৬ লাইন পর্যন্ত দেখা হয় — আসল Dialog.show()-এর ঠিক
            # উপরে এত কাছে "Toast("/"Snackbar." থাকার সুযোগ নেই, তাই ঝুঁকি নেই।
            near = "\n".join(lines[max(0, i - 6):i + 1])
            if "Toast(" in near or "Toast.makeText(" in near or "Snackbar." in near:
                continue
            ctx = "\n".join(lines[i:i + 6])
            if ("PremiumAlert.paint" in ctx or "scrubAnyDialog" in ctx
                    or "scrubDialogWindow" in ctx):
                continue
            back = "\n".join(lines[max(0, i - 90):i + 1])
            if not _re.search(r"AlertDialog\.Builder|BottomSheetDialog|= *Dialog\(|Dialog\(this", back):
                continue
            if _re.search(r"DatePickerDialog|TimePickerDialog", back):
                continue
            bad.append(os.path.basename(f) + ":" + str(i + 1))
    if bad:
        fail("৯.৩০", "এই পপ-আপগুলোতে সাজেশন-পাহারা নেই (PremiumAlert.paint বা "
                     "NoAutofill.scrubAnyDialog দুটোর একটাও ডাকা হয়নি): " + ", ".join(bad[:12])
                     + (" …আরও " + str(len(bad) - 12) if len(bad) > 12 else ""))

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২৭ — মেঘের সারি পড়ার সময় "null" লেখা যেন পর্দায় না আসে
#  ─────────────────────────────────────────────────────────────
#  🔴 TK-রিপোর্ট (২৭.০৮.২০২৬, ছবিসহ): Staff & Doctors তালিকায় নামের জায়গায়
#  **"null"**, আর বোতামে **"Remove null"**।
#
#  **আসল কারণ:** ডেটাবেসে ঘরটা ভরা না থাকলে JSON-এ `null` আসে, আর
#  `optString("full_name","")` তখন **"null" লেখাটাই** ফেরত দেয় — ফাঁকা নয়।
#  তাই `ifBlank { ... }` পাহারা কখনো চলত না।
#
#  **সমাধান:** প্রজেক্টের নিজের `JsonExt.s()` — `if (isNull(key)) "" else …`।
#  এই পাহারা নিশ্চিত করে, মেঘের সারি পড়া এই ফাইলদুটোয় আর কখনো কাঁচা
#  `optString(` দিয়ে **লেখা** পড়া না হয় (সংখ্যা/সত্য-মিথ্যা বাদ)।
# ═══════════════════════════════════════════════════════════════
def check_cloud_row_null_text():
    files = [
        os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "PeopleAdminRepository.kt"),
        os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "CloudStaffDirectory.kt"),
    ]
    for f in files:
        if not os.path.exists(f):
            fail("৯.২৭", f"{os.path.basename(f)} খুঁজে পাওয়া গেল না")
            continue
        # ⚠️ `/* ... */` কমেন্টও বাদ দিতে হয় — নইলে ব্যাখ্যার ভিতরে লেখা
        #    `optString(` -ও ভুল বলে ধরা পড়ে (নিজের পরীক্ষাতেই ধরা পড়েছে)।
        raw = read(f)
        out, i, n_ = [], 0, len(raw)
        while i < n_:
            if raw[i] == "/" and i + 1 < n_ and raw[i + 1] == "*":
                i += 2
                while i + 1 < n_ and not (raw[i] == "*" and raw[i + 1] == "/"):
                    if raw[i] == "\n":
                        out.append("\n")
                    i += 1
                i += 2
                continue
            out.append(raw[i]); i += 1
        for n, line in enumerate("".join(out).split("\n"), 1):
            code = line.split("//")[0]
            if "optString(" not in code:
                continue
            # ⛔ `message` ঘরটা সার্ভারের নিজের লেখা — কখনো null আসে না, তাই ছাড়।
            if "optString(\"message\"" in code:
                continue
            fail("৯.২৭", f"{os.path.basename(f)}:{n} — মেঘের সারি থেকে লেখা পড়তে "
                         f"`optString(` ব্যবহার হয়েছে; ঘরটা null হলে পর্দায় **\"null\"** "
                         f"দেখাবে। বদলে প্রজেক্টের `JsonExt.s()` ব্যবহার করুন।")

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২৮ — `পুরো-নাম.অবজেক্ট.ফাংশন(` সত্যিই আছে তো?
#  ─────────────────────────────────────────────────────────────
#  🔴🔴 TK-এর Android Studio-র ছবি (২৮.০৮.২০২৬, রাত ১২:৫৯):
#      InvestigationAdviceActivity.kt → Unresolved reference: build :109
#  আমি লিখেছিলাম `InvestigationHtmlPrint.build(...)`, কিন্তু `build()`
#  আছে **`InvestigationHtml`**-এ (দুটোই একই ফাইলে)। ফাইলের নাম ধরে
#  লিখে ফেলাই ছিল ভুল।
#
#  **কেন কম্পাইল-পাহারা ধরেনি (সৎ কথা):** `verify_kotlin_compile.py`-এর
#  `is_noise()` "unresolved reference: X"-কে **নীরব** ধরে যদি `X` নামটা
#  প্রজেক্টের কোথাও থাকে (Android SDK নেই বলে ওই ছাড়টা দরকার)। `build`
#  নামটা প্রজেক্টে আছে ⇒ আসল ভুলটাও চাপা পড়ে যায়।
#
#  এই পাহারা কম্পাইলার ছাড়াই, শুধু লেখা পড়ে কাজ করে:
#    ১) প্রজেক্টের সব `object X {` / `class X {`-এর নাম ও তাদের `fun` জমা হয়
#    ২) কোডে `com.tkbiswas.pilesclinic.….X.y(` লেখা খুঁজে দেখা হয় —
#       `X` চেনা হলে, `y` সত্যিই `X`-এ আছে কি না
#    ⛔ অচেনা `X` (বাইরের লাইব্রেরি) বাদ — মিথ্যা সতর্কতা হয় না।
# ═══════════════════════════════════════════════════════════════
def check_qualified_calls():
    root = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic")
    if not os.path.isdir(root):
        return
    files = []
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if fn.endswith(".kt"):
                files.append(os.path.join(dp, fn))

    # ধাপ ১ — কোন object/class-এ কোন কোন সদস্য আছে
    # ⚠️ **ব্রেস গুনে** ঠিক করা হয় কোন সদস্য কার — নইলে ভিতরের
    #    `data class Person` শুরু হলে বাইরের object-এর বাকি ফাংশনগুলোও
    #    ভুল করে Person-এর বলে ধরা হত (প্রথম চেষ্টায় ২১টা ভুয়া অভিযোগ
    #    এসেছিল, নিজের পরীক্ষাতেই ধরা পড়ে)।
    owns = {}
    top_level = set()          # 🔴 V975 — একদম বাইরে ঘোষণা করা টাইপ
    decl = re.compile(r"^\s*(?:internal\s+|private\s+|public\s+|sealed\s+|abstract\s+|open\s+)*"
                      r"(?:data\s+|enum\s+|annotation\s+)?(?:object|class|interface)\s+([A-Z]\w*)")
    mem = re.compile(r"^\s*(?:@\w+\s+)*(?:override\s+|public\s+|internal\s+|private\s+|protected\s+"
                     r"|suspend\s+|inline\s+|lateinit\s+|open\s+|const\s+)*"
                     r"(?:fun|val|var)\s+(?:<[^>]*>\s*)?([A-Za-z_]\w*)")
    for f in files:
        stack = []          # (নাম, যে গভীরতায় শুরু)
        depth = 0
        for line in read(f).splitlines():
            code = line.split("//")[0]
            d = decl.match(line)
            if d:
                # 🔴 V975 — ভিতরে ঘোষণা করা class/object-ও বাইরেরটার **সদস্য**
                #    (যেমন `EstimatePrices.Item`), নইলে ঠিক লেখাও ভুল বলে ধরা হত।
                if stack and depth == stack[-1][1] + 1:
                    owns.setdefault(stack[-1][0], set()).add(d.group(1))
                if depth == 0:
                    top_level.add(d.group(1))
                stack.append([d.group(1), depth])
                owns.setdefault(d.group(1), set())
            elif stack:
                m = mem.match(line)
                # সরাসরি ভিতরের সদস্যই গোনা হয় (আরও গভীরে নয়)
                if m and depth == stack[-1][1] + 1:
                    owns[stack[-1][0]].add(m.group(1))
            depth += code.count("{") - code.count("}")
            while stack and depth <= stack[-1][1]:
                stack.pop()

    # ধাপ ২ — পুরো-নাম দিয়ে ডাকা প্রতিটা জায়গা যাচাই
    call = re.compile(r"com\.tkbiswas\.pilesclinic(?:\.\w+)*\.([A-Z]\w*)\.([a-z]\w*)\s*\(")
    for f in files:
        txt = read(f)
        for m in call.finditer(txt):
            obj, member = m.group(1), m.group(2)
            if obj not in owns:
                continue                      # অচেনা — বাইরের কিছু, ছোঁয়া হয় না
            if member in owns[obj]:
                continue
            # অন্য কোন object-এ আছে? থাকলে নামটা বলে দিই — সারানো সহজ হয়
            where = [k for k, v in owns.items() if member in v]
            hint = (" — এটা আছে `" + "`, `".join(sorted(where)[:3]) + "`-এ") if where else ""
            fail("৯.২৮", f"{os.path.basename(f)} — `{obj}.{member}(` ডাকা হয়েছে, "
                         f"কিন্তু `{obj}`-এ `{member}` নেই{hint}")

    # ═══════════════════════════════════════════════════════════════
    #  🔴🔴 যাচাই ৯.৪১ — **টাইপের নামেও একই ভুল** (V975, ০২.০৯.২০২৬)
    #  ─────────────────────────────────────────────────────────────
    #  TK-এর Android Studio-র ছবি: `Unresolved reference: QueuePatient`।
    #  লেখা হয়েছিল `DoctorQueueModel.QueuePatient`, অথচ `QueuePatient`
    #  **object-এর বাইরে** আলাদা ক্লাস। উপরের ৯.২৮ শুধু **ফাংশন-ডাক**
    #  (`X.y(`) দেখত, তাই টাইপের এই ভুলটা পার হয়ে গিয়েছিল, আর
    #  কম্পাইল-পাহারা androidx-এর গোলমালে সেটা চাপা দিয়েছিল।
    #  ⇒ এখন `X.Y` (দুটোই বড় হাতের) — যেখানে `X` প্রজেক্টের চেনা
    #    object/class আর `Y` প্রজেক্টেরই **বাইরে ঘোষণা করা** টাইপ, অথচ
    #    `X`-এর ভিতরে নেই — সোজা FAIL।
    #  ⛔ ভুয়া ধরার ভয় কম: `Y` প্রজেক্টের top-level টাইপ হতে হবে, আর
    #     `X`-এর ভিতরে ঘোষণা করা টাইপগুলো উপরে সদস্য হিসেবেই গোনা হয়েছে।
    # ═══════════════════════════════════════════════════════════════
    tref = re.compile(r"\b([A-Z]\w*)\.([A-Z]\w*)\b")
    for f in files:
        for raw in read(f).splitlines():
            line = raw.split("//")[0]
            if line.lstrip().startswith("*"):
                continue                      # টীকা — ছোঁয়া হয় না
            for m in tref.finditer(line):
                obj, typ = m.group(1), m.group(2)
                if obj not in owns or typ not in top_level:
                    continue
                if typ in owns[obj] or typ == obj:
                    continue
                fail("৯.৪১", f"{os.path.basename(f)} — `{obj}.{typ}` লেখা হয়েছে, "
                             f"কিন্তু `{typ}` আলাদা টাইপ, `{obj}`-এর ভিতরে নয় "
                             f"(শুধু `{typ}` লিখুন)")

# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২৩ — Draft-এর জমানো তালিকায় **একটা ঘরও** বাদ পড়েনি তো?
#  ─────────────────────────────────────────────────────────────
#  🔴🔴🔴 TK-রিপোর্ট (২৭.০৮.২০২৬, ছবিসহ): *"এইসব পেশেন্টের তো বিল ক্লিয়ার
#  হয়ে গেছে, তাহলে এখানে 0% কেন দেখাচ্ছে? বিলও লেখা নাই।"*
#
#  **আসল কারণ:** `DraftEntry`-তে V646-এ কার্ডের জন্য ৯টা নতুন ঘর যোগ হয়েছিল
#  (bill · paid · refId ইত্যাদি), কিন্তু `serializeEntries()`-এ সেগুলো যোগ
#  করা হয়নি। ফলে ফোনে **জমানো** তালিকা থেকে দেখালে টাকার ঘর ০ আসত ⇒
#  Bill ₹0 · Due ₹0 · 0%, আর ➡️ বোতামের রোগী-আইডিও (`refId`) হারাত।
#
#  §৯.১৯ ঠিক এই শ্রেণির পাহারা, কিন্তু সেটা শুধু Follow-up-এর জমানো তালিকায়
#  ছিল। TK-এর নিয়ম ৬.২ (*"একবারে কেন ঠিক করতে পারেন না"*) মেনে এখানে
#  Draft-এর তালিকাটাও একই পাহারায় আনা হলো — **নাম ধরে নয়, `DraftEntry`-র
#  ঘরগুলো নিজে গুনে**, তাই ভবিষ্যতে নতুন ঘর যোগ হলেও পাহারা নিজেই ধরবে।
# ═══════════════════════════════════════════════════════════════
def check_draft_cache_fields():
    f = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "DraftRepository.kt")
    if not os.path.exists(f):
        fail("৯.২৩", "DraftRepository.kt খুঁজে পাওয়া গেল না")
        return
    s = read(f)

    m = re.search(r'data class DraftEntry\((.*?)\n\)\s*:\s*java\.io\.Serializable', s, re.S)
    if not m:
        fail("৯.২৩", "DraftRepository.kt-এ `data class DraftEntry` খুঁজে পাওয়া গেল না")
        return
    fields = re.findall(r'^\s*val (\w+)\s*:', m.group(1), re.M)
    if not fields:
        fail("৯.২৩", "DraftEntry-র ঘরগুলো পড়া গেল না")
        return

    # ⚠️ ফাংশনের শেষ **ব্রেস গুনে** বের করা হয়। প্রথম চেষ্টায় "পরের `fun`
    #    পর্যন্ত" ধরেছিলাম — তাতে `private fun` মেলেনি, দুটো ফাংশন এক হয়ে
    #    গিয়েছিল, আর ফাঁদ পেতে দেখে ধরা পড়ল পাহারা ফাঁকি খাচ্ছে।
    def body(fn_name):
        i = s.find("fun " + fn_name)
        if i < 0:
            return None
        k = s.find("{", i)
        if k < 0:
            return None
        d, j = 0, k
        while j < len(s):
            if s[j] == "{":
                d += 1
            elif s[j] == "}":
                d -= 1
                if d == 0:
                    return s[i:j + 1]
            j += 1
        return None

    save = body("serializeEntries")
    load = body("deserializeEntries")
    if save is None or load is None:
        fail("৯.২৩", "serializeEntries/deserializeEntries খুঁজে পাওয়া গেল না")
        return

    # ⛔ মন্তব্যে ঢাকা লেখা গোনা চলবে না — `//` **আর** `/* */` দুটোই বাদ,
    #    নইলে ঢেকে দিলেই পাহারা ঠকত (ফাঁদ পেতে যাচাই করা)।
    def no_comments(t):
        t = re.sub(r"/\*.*?\*/", " ", t, flags=re.S)
        return "\n".join(ln.split("//", 1)[0] for ln in t.split("\n"))
    save_c, load_c = no_comments(save), no_comments(load)

    for name in fields:
        if '"%s"' % name not in save_c:
            fail("৯.২৩", f"DraftRepository.serializeEntries()-এ `{name}` জমা হচ্ছে না — "
                         f"জমানো তালিকা দেখানোর সময় কার্ডে ওটা উধাও থাকবে "
                         f"(TK-রিপোর্ট ২৭.০৮.২০২৬: বিল/০% হারিয়ে যাওয়া)")
        if '"%s"' % name not in load_c:
            fail("৯.২৩", f"DraftRepository.deserializeEntries()-এ `{name}` পড়া হচ্ছে না — "
                         f"জমা হলেও কার্ডে বসবে না")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২০ — ছবিওয়ালা টেবিলে "সব ঘর" (select=*) তালিকা পড়া নিষেধ
#  🔴🔴🔴 V715 (২৬.০৮.২০২৬) — Supabase-এর লগ থেকে **মেপে** পাওয়া দোষ:
#  `PaymentRepository.promoteFollowUpToTreatment()` প্রতিবার `followups`
#  থেকে **`select=*`** দিয়ে ১০০ সারি পর্যন্ত টানত — অর্থাৎ **রোগীর base64
#  ছবিসহ** (একটা ছবি ~৫৫–১২০ KB)। একটা আটকে থাকা পেমেন্টের জন্য সেটা
#  দিনে ~৪,০০০ বার চলত ⇒ দিনের egress-এর সিংহভাগ।
#
#  TK-এর নিয়ম ৬.২: *"একই ধরনের সমস্যা প্রত্যেকবার কেন বলতে হবে, একবারে
#  কেন ঠিক করতে পারেন না"* — তাই এই পাহারা গোটা শ্রেণিটাই আটকায়:
#   ক) ওই ফাংশনে আর কখনো `fetchList(` (= select=*) ব্যবহার করা যাবে না
#   খ) `followups/patients/medical`-এ **মোবাইল ধরে খোঁজা** (`like.`) তালিকা
#      কোথাও `select=*` হতে পারবে না — সরু ঘর (`fetchListSlim`) লাগবেই
#   গ) `findByMobile*`-এ `"*"` চাইলে limit ছোট (<৫০) হতে হবে; বড় হলে
#      শুধু অনুমোদিত ফাইলেই (Trash-এর পুরো সারি সত্যিই দরকার)
#  ⛔ `id=eq.…` ধরে এক সারি পড়া — আগের মতোই, কিছুই বদলায়নি।
# ═══════════════════════════════════════════════════════════════
PHOTO_TABLES = ("followups", "patients", "medical")
#  পুরো সারি সত্যিই দরকার (ডিলিটের স্ন্যাপশট — ছবি বাদ দিলে Restore-এ
#  রোগীর ছবি চিরতরে হারাবে; ২৩.০৮.২০২৬-এর অডিটে TK-অনুমোদিত)।
#  ⚠️ **পুরোনো ভিত্তি (baseline)** — V715-এ পাহারাটা বসানোর দিন এই ফাইলগুলোয়
#  আগে থেকেই এই ধরনের চওড়া পড়া ছিল। প্রতিটার কারণ কোডে গিয়ে দেখা হয়েছে
#  (২৩.০৮.২০২৬-এর egress অডিটেও এদের কয়েকটা "ছোঁয়া যাবে না" বলা আছে —
#  ওখানে **পুরো সারিটাই আবার লেখা/জমা হয়**, ছবি বাদ দিলে Restore/ব্রাঞ্চ-বদলে
#  রোগীর ছবি চিরতরে হারাত)। এগুলো **মাপা হয়নি এখনো** — তাই এখন ছোঁয়া হচ্ছে
#  না, শুধু নতুন কোনো জায়গা যোগ হলে পাহারা আটকাবে।
#  🔴 TK-কে জানানো হয়েছে; তাঁর অনুমতি পেলে একটা একটা করে মেপে ঠিক করা হবে।
WIDE_MOBILE_READ_ALLOWED = (
    "TrashHelper.kt",              # ডিলিটের স্ন্যাপশট — পুরো সারি সত্যিই দরকার
    "ReturnVisitRepository.kt",    # ভিত্তি (V715), মাপা হয়নি
    "ChamberAttendanceActivity.kt",# ভিত্তি (V715), মাপা হয়নি
    "PatientTimelineActivity.kt",  # ভিত্তি (V715), মাপা হয়নি
    "EnquiryRepository.kt",        # ব্রাঞ্চ বদল — পুরো সারি আবার লেখা হয়
    "ReportCardActivity.kt",       # ভিত্তি (V715), মাপা হয়নি
    "ReportCardPrinter.kt",        # ভিত্তি (V715), মাপা হয়নি
    "FollowUpActivity.kt",         # ভিত্তি (V715), মাপা হয়নি
)


def check_no_wide_photo_reads():
    base = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                        "java", "com", "tkbiswas", "pilesclinic")
    if not os.path.isdir(base):
        return

    # ── (ক) promoteFollowUpToTreatment-এ select=* ফিরে আসেনি তো ──
    pr = os.path.join(base, "native", "PaymentRepository.kt")
    s = read(pr)
    if s:
        i = s.find("fun promoteFollowUpToTreatment")
        if i < 0:
            fail("৯.২০", "PaymentRepository.kt-এ promoteFollowUpToTreatment() খুঁজে পাওয়া গেল না")
        else:
            j = s.find("\n    private fun ", i + 10)
            body = s[i:j if j > 0 else len(s)]
            body = "\n".join(ln for ln in body.split("\n") if not ln.strip().startswith("//"))
            if "SupabaseClient.fetchList(" in body:
                fail("৯.২০", "PaymentRepository.promoteFollowUpToTreatment()-এ `fetchList(` "
                             "(= select=*, রোগীর ছবিসহ) ফিরে এসেছে — `fetchListSlim(` ব্যবহার করুন")

    # ── (খ) ও (গ) গোটা প্রজেক্ট ──
    for root, _dirs, files in os.walk(base):
        for fn in files:
            if not fn.endswith(".kt"):
                continue
            path = os.path.join(root, fn)
            txt = read(path)
            if not txt:
                continue
            for n, line in enumerate(txt.split("\n"), 1):
                bare = line.strip()
                if bare.startswith("//") or bare.startswith("*"):
                    continue
                for t in PHOTO_TABLES:
                    if fn in WIDE_MOBILE_READ_ALLOWED:
                        continue
                    if 'SupabaseClient.fetchList("%s"' % t in line and "like." in line:
                        fail("৯.২০", "%s:%d — `%s` টেবিলে মোবাইল ধরে (`like.`) তালিকা পড়া হচ্ছে "
                                     "`select=*` দিয়ে (ছবিসহ)। `fetchListSlim(` + সরু ঘর ব্যবহার করুন"
                                     % (fn, n, t))
                if "findByMobile" in line and '"*"' in line and fn not in WIDE_MOBILE_READ_ALLOWED:
                    m = re.search(r'"\*"\s*,\s*(\d+)', line)
                    if m and int(m.group(1)) >= 50:
                        fail("৯.২০", "%s:%d — মোবাইল ধরে `\"*\"` (সব ঘর, ছবিসহ) %s সারি পর্যন্ত "
                                     "পড়া হচ্ছে। সরু ঘরের তালিকা দিন" % (fn, n, m.group(1)))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১১ — সংখ্যা সবসময় ইংরেজিতে
#  🔒 TK-এর গ্লোবাল রুল (29.07.2026 সন্ধ্যা ৬.১০, খাতার সারি B93):
#  *"সংখ্যা সব সময় ইংলিশেই হতে হবে। বাংলা অথবা হিন্দিতে হবে না।"*
#  ব্যবহারকারী যে লেখা দেখেন (স্ট্রিং) তাতে ০-৯ (বাংলা) বা ०-९ (হিন্দি)
#  থাকলে ফাইল বানানো আটকে যাবে। কমেন্টে থাকলে সমস্যা নেই।
# ═══════════════════════════════════════════════════════════════
# ══════════════════════════════════════════════════════════════════════
#  যাচাই ৯.২১ — ডাক্তার/RMP-এর ৪টে বার্তা ফোনে ও ওয়েবে **হুবহু এক**
#  🔒 V733 (২৭.০৮.২০২৬, TK-অনুমোদিত)। TK-নির্দেশ: *"ফোনের লেখা থেকে যন্ত্র
#  দিয়ে ওয়েবেরটা বানাবেন"* এবং *"দুটো আলাদা হলে যেন ধরা পড়ে"*।
#
#  লেখার **একমাত্র উৎস** `DoctorMessage.kt`। `03_NETLIFY_READY/app.js`-এর
#  WLV1_DOCMSG অংশটা `00_GUARD/gen_web_doctor_messages.py` যন্ত্রে বানায়।
#  কেউ ওয়েবে হাতে লেখা বদলালে — বা ফোনে বদলে ওয়েবে না বসালে — এই যাচাই
#  আটকে দেবে, তাই ডাক্তারের কাছে দুই জায়গা থেকে দু-রকম বার্তা যেতে পারে না।
#
#  ঠিক করার উপায়:  python3 00_GUARD/gen_web_doctor_messages.py --write
# ══════════════════════════════════════════════════════════════════════
def check_doctor_message_twin():
    gen = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "gen_web_doctor_messages.py")
    if not os.path.exists(gen):
        fail("৯.২১", "gen_web_doctor_messages.py ফাইলটাই নেই — ফোন ও ওয়েবের "
                     "ডাক্তার-বার্তা মিলিয়ে দেখার যন্ত্র হারিয়ে গেছে")
        return
    try:
        r = subprocess.run([sys.executable, gen, "--check"], cwd=ROOT,
                           capture_output=True, text=True, timeout=120)
    except Exception as e:
        fail("৯.২১", "ফোন ও ওয়েবের ডাক্তার-বার্তা মেলানো গেল না: %s" % e)
        return
    if r.returncode != 0:
        msg = (r.stdout + r.stderr).strip().replace("\n", " · ")[:400]
        fail("৯.২১", "ডাক্তার/RMP-এর বার্তা ফোনে ও ওয়েবে এক নয় → " + msg)


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.২২ — 🩹 পপ-আপে WebView বসানোর ফাঁদ (কম্পনের দোষ)
#  ─────────────────────────────────────────────────────────────
#  🔴🔴🔴 TK-রিপোর্ট (২৭.০৮.২০২৬, ভিডিও সহ): *"কম্পন হচ্ছে কেন?"* — Note
#  পপ-আপ সেকেন্ডে কয়েকবার ছোট-বড় হচ্ছিল, দুটো Close বোতাম দেখা যাচ্ছিল।
#
#  **দোষটা কী:** পপ-আপে (`AlertDialog.setView`) সরাসরি WebView বসালে তার
#  উচ্চতা বাঁধা থাকে না, আর তাতে একটা গোল চক্র তৈরি হয় —
#      পপ-আপের মাপ → WebView-এর চওড়া → লেখার উচ্চতা → পপ-আপ আবার মাপে
#  একবার এদিকে একবার ওদিকে ⇒ চোখে **কম্পন**।
#  V737-এ তিনটে পপ-আপেই (Note · Check-up Record · Prescription Details)
#  এটা ঠিক করা হয়েছে — সবকটা এখন `steadyWebView()` দিয়ে যায়, যেখানে
#  উচ্চতা **একবার মেপে বসে, তারপর আর বদলায় না**।
#
#  TK-এর নির্দেশ (২৭.০৮.২০২৬): *"নিরাপদে করতে হবে... যাতে কোনো ভালো কাজ
#  খারাপ না হয় আর আপনাকে কোন সমস্যার কথা আমাকে বলতে না হয়।"*
#  ⇒ এই পাহারাটা যাতে দোষটা **আর কোনোদিন ফিরতে না পারে**।
#
#  তিনটে ভাগে যাচাই:
#    (ক) পপ-আপে (`setView`) কোনো WebView বসানো হয়নি তো?
#    (খ) Kotlin-এ নতুন কোনো WebView গজায়নি তো? (অনুমোদিত তালিকার বাইরে)
#    (গ) `steadyWebView()`-এর তিনটে খুঁটি অক্ষত আছে তো?
#
#  ⛔ কমেন্ট ও লেখার ভিতরের "WebView(" ধরা হয় না — নইলে ভুয়া ভুল দেখাত
#     (FollowUpActivity ও UserPhotoActivity-র ডক-কমেন্টে ওই শব্দটা আছে)।
# ═══════════════════════════════════════════════════════════════

# 🔒 অনুমোদিত WebView — ফাইল : কতগুলো। সবকটাই যাচাই করা (২৭.০৮.২০২৬):
#    হয় ছাপার/শেয়ারের (পর্দায় বসে না), নয় মাপ বাঁধা।
#    ⛔ নতুন WebView লাগলে **আগে TK-কে জানাতে হবে**, তারপর এখানে যোগ।
_WEBVIEW_ALLOWED = {
    "modules/IncomeExpenseActivity.kt":  (1, "ছাপা — Statement PDF"),
    "modules/PartnerSharesActivity.kt":  (1, "ছাপা — Partner Shares"),
    "clinical/DoctorCheckupActivity.kt": (3, "১টা পূর্ণ-পর্দা (weight=1f, মাপ বাঁধা) + ২টা ছাপার"),
    # V982 (TK-নির্দেশ: *"ফুল স্ক্রিন পর্দা খুলবে, আলাদা পপ-আপ লাগবে না"*)
    "clinical/EstimatePaperActivity.kt": (2, "১টা পূর্ণ-পর্দা (weight=1f, মাপ বাঁধা) + ১টা ছাপার"),
    "native/MedicinePaymentActivity.kt": (1, "ছাপা — Medicine receipt"),
    "native/PatientTimelineActivity.kt": (2, "১টা `steadyWebView()`-এর ভিতরে + ১টা ছাপার"),
    "native/PaymentActivity.kt":         (1, "ছাপা — Treatment receipt"),
    "native/ReportCardPrinter.kt":       (1, "পর্দার বাইরে, আসল ছাপার মাপে"),
    "print/DietChartHtmlPrint.kt":       (1, "ছাপা"),
    # 🧾 V1055 (০৪.০৯.২০২৬) — TK-কে জানিয়ে যোগ করা হলো। এটা **ছাপার** ফাইল
    #    (DietChartHtmlPrint-এর হুবহু একই পথ), পপ-আপে বসে না, তাই কম্পনের ফাঁদ নেই।
    "print/SalaryStatementHtmlPrint.kt": (1, "ছাপা"),
    # 🗓️ V1199 (০৮.০৯.২০২৬) — TK-কে জানিয়ে যোগ করা হলো। এটা **ছাপার** ফাইল
    #    (SalaryStatementHtmlPrint-এর হুবহু একই পথ), পপ-আপে বসে না, তাই কম্পনের ফাঁদ নেই।
    "print/AttendanceSheetHtmlPrint.kt": (1, "ছাপা — Staff Attendance Sheet"),
    # 📒 V1252 (০৯.০৯.২০২৬) — TK-কে জানিয়ে যোগ করা হলো। এটা **ছাপার**
    #    WebView (EstimatePaperActivity-র হুবহু একই পথ) — পপ-আপে বসে না,
    #    পর্দায় দেখানোও হয় না, তাই কম্পনের ফাঁদ নেই।
    "native/RmpCommissionSheetActivity.kt": (1, "ছাপা — RMP Commission Sheet"),
    "print/InvestigationHtmlPrint.kt":   (1, "ছাপা"),
    "print/PrescriptionHtmlPrint.kt":    (1, "ছাপা"),
    "print/PrescriptionWhatsAppShare.kt":(1, "WhatsApp-এ PDF"),
    "print/RegistrationHtmlPrint.kt":    (1, "ছাপা"),
}

# 🔒 `steadyWebView()`-এর খুঁটি — একটাও সরালে কম্পন ফিরে আসতে পারে।
_STEADY_PILLARS = [
    ("var applied = false",
     "একবারই মাপ বসানোর পাহারা (`applied`) — এটাই চক্র ভাঙে"),
    ("if (applied) return",
     "দ্বিতীয়বার মাপ বসানো আটকানো"),
    ("if (px <= 0) fallbackH else px.coerceIn(minH, maxH)",
     "মাপা উচ্চতা ১৪০dp আর পর্দার ৭০%-এর মধ্যে বাঁধা (মাপ না পেলে নিরাপদ মাপ)"),
    ("android.widget.LinearLayout.LayoutParams.MATCH_PARENT, fallbackH",
     "শুরুতেই একটা নিরাপদ মাপ, তাই প্রথম মাপাতেই পপ-আপ স্থির"),
]


def _blank_comments(src):
    """কমেন্ট ও লেখার ভিতরটা ফাঁকা করে দেয়, **লাইন-সংখ্যা অটুট রেখে**।
       তাই ভুলের বার্তায় লাইন নম্বর ঠিক থাকে।"""
    out = list(src)
    i, n = 0, len(src)
    def blank(a, b):
        for k in range(a, min(b, n)):
            if out[k] != '\n':
                out[k] = ' '
    while i < n:
        c = src[i]
        if src.startswith('"""', i):                       # raw string
            j = src.find('"""', i + 3)
            j = (j + 3) if j >= 0 else n
            blank(i, j); i = j; continue
        if c == '"':                                       # সাধারণ লেখা
            j = i + 1
            while j < n and src[j] != '"':
                if src[j] == '\\': j += 2
                elif src[j] == '\n': break
                else: j += 1
            j = min(j + 1, n)
            blank(i, j); i = j; continue
        if c == "'":                                       # অক্ষর-লেখা ('"' সহ)
            j = i + 1
            while j < n and src[j] != "'":
                if src[j] == '\\': j += 2
                elif src[j] == '\n': break
                else: j += 1
            j = min(j + 1, n)
            blank(i, j); i = j; continue
        if src.startswith('//', i):
            j = src.find('\n', i); j = j if j >= 0 else n
            blank(i, j); i = j; continue
        if src.startswith('/*', i):                        # Kotlin-এ নেস্টেডও হয়
            d, j = 1, i + 2
            while j < n and d > 0:
                if src.startswith('/*', j): d += 1; j += 2; continue
                if src.startswith('*/', j): d -= 1; j += 2; continue
                j += 1
            blank(i, j); i = j; continue
        i += 1
    return ''.join(out)


def _arg_of(src, open_paren):
    """`(` থেকে শুরু করে মিলে যাওয়া `)` পর্যন্ত ভিতরের লেখা।"""
    d, j, n = 0, open_paren, len(src)
    while j < n:
        if src[j] == '(': d += 1
        elif src[j] == ')':
            d -= 1
            if d == 0: return src[open_paren + 1:j]
        j += 1
    return ""


_WV_NEW = re.compile(r'\bWebView\s*\(')


def check_webview_popup():
    pkg = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic")
    JAVA_REL = lambda p: os.path.relpath(p, pkg).replace(os.sep, "/")

    # ── (ক) পপ-আপে (`setView`) WebView বসানো হয়নি তো? ──────────────
    for f in kt_files():
        src = _blank_comments(read(f))
        for m in re.finditer(r'\.setView\s*\(', src):
            op = src.index('(', m.start())
            arg = _arg_of(src, op)
            ln = src.count('\n', 0, m.start()) + 1
            hit = None
            if _WV_NEW.search(arg):
                hit = "সরাসরি WebView বসানো হয়েছে"
            else:
                ident = arg.strip()
                if re.fullmatch(r'[A-Za-z_]\w*', ident):
                    if re.search(
                        r'\b(?:val|var)\s+' + re.escape(ident) +
                        r'\s*(?::[^=\n]*)?=\s*[^\n]*\bWebView\s*\(', src):
                        hit = f"`{ident}` একটা WebView, সেটাই বসানো হয়েছে"
            if hit:
                fail("৯.২২",
                     f"{JAVA_REL(f)}:{ln} — পপ-আপে ({'.setView'}) {hit}। "
                     "এতে পপ-আপ **কাঁপে** (TK-রিপোর্ট ২৭.০৮.২০২৬, V737)। "
                     "`steadyWebView(html)` ব্যবহার করুন — ওতে উচ্চতা একবার "
                     "বসে, তারপর আর বদলায় না")

    # ── (খ) অনুমোদিত তালিকার বাইরে নতুন WebView গজায়নি তো? ─────────
    for f in kt_files():
        rel = JAVA_REL(f)
        src = _blank_comments(read(f))
        n = len(_WV_NEW.findall(src))
        if n == 0:
            continue
        allowed, why = _WEBVIEW_ALLOWED.get(rel, (0, ""))
        if n > allowed:
            fail("৯.২২",
                 f"{rel} — {n}টা WebView পাওয়া গেল, অনুমোদিত {allowed}টা"
                 + (f" ({why})" if why else " (এই ফাইলে কোনো WebView অনুমোদিত নয়)")
                 + "। পপ-আপে দেখাতে হলে `steadyWebView()` ব্যবহার করুন; ছাপার "
                   "হলে TK-কে জানিয়ে 00_GUARD/tk_guard.py-র `_WEBVIEW_ALLOWED`-এ যোগ করুন")

    # ── (গ) `steadyWebView()`-এর তিনটে খুঁটি অক্ষত আছে তো? ──────────
    ptl = os.path.join(pkg, "native", "PatientTimelineActivity.kt")
    if os.path.exists(ptl):
        s = read(ptl)
        if "private fun steadyWebView(" not in s:
            fail("৯.২২",
                 "native/PatientTimelineActivity.kt — `steadyWebView()` ফাংশনটাই নেই। "
                 "এটা কম্পন থামায় (V737) — সরানো যাবে না")
        else:
            for needle, what in _STEADY_PILLARS:
                if needle not in s:
                    fail("৯.২২",
                         f"native/PatientTimelineActivity.kt — `steadyWebView()`-এর খুঁটি "
                         f"হারিয়েছে: {what} (`{needle}`)। এটা ছাড়া পপ-আপ আবার কাঁপতে পারে")


def check_digits():
    DIG = set(chr(0x09E6 + i) for i in range(10)) | set(chr(0x0966 + i) for i in range(10))
    STR = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"|\'([^\'\\]*(?:\\.[^\'\\]*)*)\'')
    bad = []
    for base in (JAVA, RES, WEB):
        for dp, _, fs in os.walk(base):
            if os.sep + "build" + os.sep in dp:
                continue
            for f in fs:
                if not f.endswith((".kt", ".js", ".xml")):
                    continue
                q = os.path.join(dp, f)
                inblk = False
                for ln, line in enumerate(read(q).split("\n"), 1):
                    t = line.strip()
                    if inblk:
                        if "*/" in t: inblk = False
                        continue
                    if t.startswith("/*"):
                        if "*/" not in t: inblk = True
                        continue
                    if t.startswith("//") or t.startswith("*") or t.startswith("<!--"):
                        continue
                    for m in STR.finditer(line):
                        if any(ch in DIG for ch in m.group(0)):
                            bad.append((q, ln, m.group(0)[:40]))
    for q, ln, txt in bad[:8]:
        fail("৯.১১", f"{os.path.relpath(q, ROOT)}:{ln} — লেখায় বাংলা/হিন্দি সংখ্যা: {txt}")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১২ — 🚔 লক করা নিয়মের পাহারা ("দারোগা")
#
#  🚨 TK-এর নির্দেশ (29.07.2026 সন্ধ্যা ৬.৩৫ · খাতার সারি B125):
#  *"আগের সেশনে যখন এই কথা বলা হয়েছে, এখন আবার কেন আপনাকে বলতে হচ্ছে...
#    পাহারাদারকে আরো স্ট্রং করুন বা পুলিশ বসান, দারোগা বসান, এসপি বসান...
#    আমি চাই না দ্বিতীয়বার আপনার সাথে এই নিয়ে আমার কোনো কথা হোক।"*
#
#  **কেন এটা দরকার হলো:** আগের যাচাইগুলো ধরত **বিল্ড ভাঙবে কিনা**। কিন্তু
#  29.07.2026-এ যে ভুলটা হলো (খাতার সারি B124) তাতে বিল্ড ভাঙেনি — কোড
#  চলছিল, শুধু **নিয়মটা ভুল শর্তে বাঁধা ছিল**, তাই লাইভ ক্লিনিকে TK-এর
#  আগে বলে দেওয়া নিয়মটা আবার ভেঙে গেল আর তাঁকে দ্বিতীয়বার বলতে হলো।
#
#  **এই পাহারাদার সেটাই ধরে:** নিচের তালিকায় TK-এর লক করা নিয়মগুলো
#  কোডের **নির্দিষ্ট লাইনের সঙ্গে বাঁধা**। কেউ (ভবিষ্যতের যে কোনো সেশন,
#  AI বা ডেভেলপার) ওই লাইন সরালে বা নিয়মটা অন্য শর্তে বাঁধলে **ফাইল
#  বানানোই আটকে যাবে**।
#
#  ⛔ এই তালিকা থেকে কোনো সারি TK-এর অনুমতি ছাড়া মোছা বা দুর্বল করা
#     যাবে না। নতুন নিয়ম লক হলে **নতুন সারি যোগ** করতে হবে, তারিখ সহ।
#
# ═══════════════════════════════════════════════════════════════
#  📋 TK-এর স্থায়ী নির্দেশ (02.08.2026 রাত ~১.১০ am · খাতার সারি B268) —
#  মেশিনে চেক করা যায় না, কিন্তু TK-এর স্পষ্ট নির্দেশে এখানে লিখে রাখা হলো:
#
#  *"এটাতো একটা সমস্যা, ঠিক করার জন্য এরকম পারমিশন নেয়ার তো কোন দরকার
#    নেই। আমি টেকনিক্যাল ভাষা বুঝি না, আমি কোনো ডেভেলপার নই, আমি একজন
#    সাধারণ ব্যবহারকারী। ভবিষ্যতে এই ধরনের কথা বলার আগে এটা বোঝার চেষ্টা
#    করবেন। যদি কখনো কাজ করতে সন্দেহ থাকে ডাইরেক্ট এইভাবেই বোঝাবেন।"*
#
#  ⛔ সত্যিকারের bug (design change নয়) ধরা পড়লে "ঠিক করব?" জিজ্ঞাসা না
#     করে সরাসরি ঠিক করে ফেলতে হবে। শুধু ডিজাইন বদলের ক্ষেত্রেই আগে
#     ফটো-প্রুফ/অনুমতি লাগে (আগের স্থায়ী নিয়ম অক্ষত)। সন্দেহ/জিজ্ঞাসা
#     থাকলে **সহজ/শর্টকাট ভাষায়** — টেকনিক্যাল ভাষায় নয়।
#
#  ⚠️ এই নিয়মের প্রসঙ্গেই একটা স্বীকারোক্তি: item 89 ("ব্রাঞ্চের আজকের
#     হিসাব বনাম পেমেন্ট মেলা")-কে ভুল করে "বাগ আছে" বলা হয়েছিল — শুধু
#     `RefundedRecords` নামটা খুঁজে, `ReportsRepository.kt`-এর আসল কোড
#     (একই নিয়ম `PaymentModel.isApprovedRefund/isRefundRow` দিয়ে, আগেই
#     B254-এ ঠিক করা) না দেখেই। **শিক্ষা:** নাম/ক্লাস খুঁজে "বাগ আছে/নেই"
#     সিদ্ধান্ত নেওয়া যাবে না — আসল লজিক পড়ে নিশ্চিত হতে হবে।
# ═══════════════════════════════════════════════════════════════
NATIVE = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native")
# 🔒 খাতার সারি B258 (01.08.2026): Blood Test/Investigation Advice পর্দা
# `native` প্যাকেজে নয়, `clinical`-এ — তাই লক করা নিয়ম যাচাইয়ে এই পথটাও লাগবে।
CLINICAL = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "clinical")
# 🔒 খাতার সারি B269 (02.08.2026): PrintCenterActivity.kt `print` প্যাকেজে —
# লক করা নিয়ম যাচাইয়ে এই পথটাও লাগবে।
PRINT = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "print")
MODULES = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "modules")

LOCKED_RULES = [
    {
        "row": "B283",
        "when": "02.08.2026 05.47.55 pm IST (TK-এর স্থায়ী Password/Login Lock + SQL success screenshot)",
        "rule": "Password শুধু মূল App Login-এ একবার; পরে কোথাও Module Sign-in নয়; পুরোনো চারটি role password-ই থাকবে",
        "files": {
            "ModuleUi.kt": {
                "must": ["fun ensureSignedIn(", "ModuleAuth.signInCurrentSession(activity.applicationContext)"],
                "must_not": ["Module Sign-in", "Module password", "Secure module sign-in"],
            },
            "ModuleAuth.kt": {
                "must": ['"master" -> "admin123"', '"doctor" -> "doctor123"', '"field" -> "field123"', 'else -> "staff123"'],
                "must_not": ["mobile@$branchCode"],
            },
            "StaffDirectory.kt": {
                "must": ['"master" to "admin123"', '"staff" to "staff123"', '"doctor" to "doctor123"', '"field" to "field123"'],
                "must_not": [],
            },
        },
    },
    {
        "row": "B124",
        "when": "29.07.2026 6.25 pm",
        "rule": "চালু কার্ডে ডিলিট কখনো দেখাবে না — ডিলিট শুধু Draft-এর বাতিল তালিকা থেকে",
        "files": {
            "PatientTimelineActivity.kt": {
                "must": [
                    'val fromDraftClosed = intent.getBooleanExtra("fromDraftClosed"',
                    'if (!fromDraftClosed) {',
                    'if (currentFollowupId.isNotBlank() && !fromDraftClosed) {',
                ],
                "must_not": [
                    'val alreadyClosed = currentFollowupStatus',
                ],
            },
            "DraftListActivity.kt": {
                "must": ['putExtra("fromDraftClosed"'],
                "must_not": [],
            },
        },
    },
    {
        "row": "B277",
        "when": "02.08.2026 সকাল ১১.০৫ am (TK-এর ছবি+নির্দেশ — Cancel/Apply বদলে Share/Cancel/Print)",
        "rule": "\"Common Blood Test\" পপ-আপে Cancel/Apply নয় — Share/Cancel/Print তিনটে বোতাম, প্রতিটা সত্যিকারের কাজ করবে",
        "files": {
            "InvestigationAdviceActivity.kt": {
                "must": ['.setPositiveButton("Print")', '.setNeutralButton("Share")', "fun shareInvestigations()"],
                "must_not": ['.setPositiveButton("Apply")'],
            },
        },
    },
    {
        "row": "B275",
        "when": "02.08.2026 সকাল ~১০.৩৮ am (TK — 'তারা চিহ্ন আমরাও যেন না দেখি, যাকে পাঠানো হবে তারাও যেন না দেখে')",
        "rule": "Enquiry বার্তায় (প্রিভিউ/WhatsApp/SMS তিন জায়গাতেই) কোনো তারা-চিহ্ন (*) থাকবে না",
        "files": {
            "PatientMessage.kt": {
                "must": ['buildEnquiryLockedTemplate(lang, branch, disease).replace("*", "")'],
                "must_not": [],
            },
        },
    },
    {
        "row": "B279",
        "when": "02.08.2026 দুপুর ১১.৪৫ am (TK নিজে Enquiry ভরে→Reject করে→Draft Reject List-এ Delete খুঁজে পাননি)",
        "rule": "Draft-এর Reject/Incomplete তালিকা থেকে খোলা অ-রেজিস্টার্ড রেকর্ডে Delete বোতাম সবসময় দেখাবে — currentEnquiryId ফাঁকা থাকলেও (আসল ডিলিট-কাজ নিজে থেকেই মোবাইল ধরে খুঁজে নেয়)",
        "files": {
            "PatientTimelineActivity.kt": {
                "must": ["} else if (!isRegistered) {"],
                "must_not": ["} else if (!isRegistered && currentEnquiryId.isNotBlank()) {"],
            },
        },
    },
    {
        "row": "B138",
        "when": "29.07.2026 10.30 pm",
        "rule": "ডিলিটের চিহ্ন ক্লাউডেও থাকবে — মুছে ফেলা রেকর্ড কোনো ফোন থেকেই আর ফিরতে পারবে না",
        "files": {
            "DeletedGuard.kt": {"must": ['fun pushDeletedToCloud(', 'fun syncFromCloud(', 'deleted_records'], "must_not": []},
            "BottomNav.kt": {"must": ['DeletedGuard.syncFromCloud('], "must_not": []},
        },
    },
    {
        "row": "B134",
        "when": "29.07.2026 9.20 pm",
        "rule": "কম্পিউটারেও বাতিল (Cancelled) রোগীর টাকা দিনের হিসাবে ধরা যাবে না — ফোনের সঙ্গে এক হিসাব",
        "files": {
            "app.js": {"must": ['function wlv1RefundedMobiles(', 'function collectionRowsAll(', 'wlv1NotRefunded'], "must_not": []},
        },
    },
    {
        "row": "B133",
        "when": "29.07.2026 8.50 pm",
        "rule": "Registration-এর ডুপ্লিকেট বাক্সেও সতর্কবার্তা — রেকর্ডটা আগে বাতিল/Incomplete করা ছিল কিনা",
        "files": {
            "RegistrationActivity.kt": {"must": ['closed: EnquiryRepository.ClosedInfo', 'if (closed.closed) {', 'R.id.boxDupClosed'], "must_not": []},
        },
    },
    {
        "row": "B132",
        "when": "29.07.2026 8.20 pm",
        "rule": "ডুপ্লিকেট নম্বরের বাক্সে সতর্কবার্তা — নম্বরটা আগে Reject/Incomplete করা ছিল কিনা স্টাফকে জানাতেই হবে",
        "files": {
            "EnquiryActivity.kt": {"must": ['closed: EnquiryRepository.ClosedInfo', 'if (closed.closed) {', 'THIS NUMBER WAS '], "must_not": []},
            "EnquiryRepository.kt": {"must": ['fun closedInfo(', 'data class ClosedInfo'], "must_not": []},
        },
    },
    {
        "row": "B113",
        "when": "29.07.2026 6.40 pm",
        "rule": "চালু কার্ডের মেনুতে এক শব্দ — Reject List / Incomplete Patient (দুই পর্দায় দুই নাম নয়)",
        "files": {
            "PatientTimelineActivity.kt": {"must": ['"Reject List"', '"Incomplete Patient"'], "must_not": []},
            "FollowUpActivity.kt":        {"must": ['"Reject List"', '"Incomplete Patient"'], "must_not": []},
        },
    },
    {
        "row": "B108",
        "when": "29.07.2026 3.34 pm",
        "rule": "Reject/Delete করা এনকোয়ারি আর কখনো তালিকায় ফিরবে না — সারির নিজের status দেখতেই হবে",
        "files": {
            "FollowUpRepository.kt": {
                "must": ['val eStatus = row.s("status").trim()', 'fun markEnquiryClosedByMobile'],
                "must_not": [],
            },
        },
    },
    {
        "row": "B98 · B111 · B112",
        "when": "29.07.2026 10.10 pm",
        "rule": "ডিলিট শুধু Master — স্টাফ চাপলে কিছুই মোছে না, অনুরোধ মাস্টারের ঘন্টায় যায়",
        "files": {
            "PatientTimelineActivity.kt": {"must": ['DeletePermission.canDeleteNow'], "must_not": []},
            "DraftListActivity.kt":       {"must": ['DeletePermission.canDeleteNow'], "must_not": []},
        },
    },
    {
        "row": "B240 (Master Fix Order §14)",
        "when": "31.07.2026 রাত, V217",
        "rule": "🔓 B98-এর উপরের নিয়ম এখন TK-এর স্পষ্ট অনুমতিতে (\"এই নিয়ম B98-এর সাধারণ Delete "
                "Rule-এর পরিবর্তে শুধু Same-Day বিশেষ নিয়ম হিসেবে কাজ করবে\") আংশিক বদলেছে — "
                "Master সবসময় পারবেন (অপরিবর্তিত); Staff এখন আজ/গতকালের নিজের ব্রাঞ্চের এন্ট্রি "
                "নিজেই Reject/Delete করতে পারবেন (চেম্বার বন্ধ না থাকলে) — Trash-এ যায়, Permanent "
                "নয়, Master Restore করতে পারবেন। পুরনো এন্ট্রিতে আগের B98 নিয়মই (Master-only "
                "request) বহাল। ⚠️ এই সারিটা মোছা যাবে না — B98 আর এটা একসাথে পড়তে হবে।",
        "files": {
            "PatientTimelineActivity.kt": {"must": ['DeletePermission.canDeleteEntryNow'], "must_not": []},
        },
    },
    {
        "row": "B258",
        "when": "01.08.2026 10.33 pm",
        "rule": "Blood Test পাতায় 'Previous Patient Blood Test' (dynamic) ও 'Common Blood Test' (fixed 7 tests) — দুটো আলাদা বক্স, একই শেয়ার্ড ডায়ালগ",
        "files": {
            "ClinicalRepository.kt": {"must": ["val commonBloodTestFixed: List<String>"], "must_not": []},
            "InvestigationAdviceActivity.kt": {
                "must": [
                    "fun showBloodTestChecklistDialog(",
                    "fun applyFixedCommonBloodTest()",
                    '"⏰  Previous Patient Blood Test"',
                    '"⭐  Common Blood Test"',
                ],
                "must_not": [],
            },
        },
    },
    {
        "row": "B258.1",
        "when": "01.08.2026 10.33 pm এর পরে (আনুমানিক)",
        "rule": "Blood Test পাতার নিচের বোতাম — SAVE · SHARE · PRINT এই ক্রমে, 'Save & Print' নামে কোনো বোতাম নয়",
        "files": {
            "activity_investigation_advice.xml": {
                "must": ['android:text="Print"'],
                "must_not": ["Save &amp; Print"],
            },
        },
    },
    {
        "row": "B270",
        "when": "02.08.2026 রাত ১.৩৫ am (TK — 'সম্পূর্ণ প্রজেক্টে এই চেহারা এক থাকতে হবে', আগের ছবিগুলোর কথা বলে)",
        "rule": "InvestigationAdviceActivity-এর নিজের 'Previous Patient'/'Common Blood Test' চেকলিস্টও গোল টিক-বৃত্ত কার্ড ডিজাইনে হবে, প্লেইন CheckBox নয়",
        "files": {
            "InvestigationAdviceActivity.kt": {
                "must": ["fun roundedBg(fill: String, stroke: String, radius: Int)"],
                "must_not": ["val cb = android.widget.CheckBox(this).apply {\n                text = name\n                isChecked = true"],
            },
        },
    },
    {
        "row": "B269",
        "when": "02.08.2026 রাত ১.১৫ am (TK-এর স্ক্রিনশট — Print Center-এর Blood Test ক্যাটাগরি পপ-আপ Investigation Advice-এর থেকে আলাদা দেখাচ্ছিল)",
        "rule": "Print Center-এর Blood Test ক্যাটাগরি-চেকলিস্ট (Hematology/Immunology/...) InvestigationCategoryActivity-এর হুবহু একই গোল-টিক কার্ড ডিজাইনে হবে, সাধারণ CheckBox তালিকা নয়",
        "files": {
            "PrintCenterActivity.kt": {
                "must": ["fun roundedBg(fill: String, stroke: String, radius: Int)", "com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, categoryName)"],
                "must_not": ["android.widget.CheckBox(this).apply {\n                text = name\n                isChecked = selectedTests.contains(name)"],
            },
        },
    },
    {
        "row": "B267",
        "when": "02.08.2026 রাত ১২.৫৫ am (TK নিজে পাঁচটা ব্রাঞ্চের বাংলা ক্লিনিক-তথ্য হুবহু লিখে নিশ্চিত করেছেন — 'এগুলোই হবে')",
        "rule": "পাঁচটা ব্রাঞ্চের বাংলা ক্লিনিক-নাম/ঠিকানা TK-এর নিজের চূড়ান্ত লেখার সাথে হুবহু মিলবে",
        "files": {
            "PatientMessage.kt": {
                "must": [
                    '"kishanganj" to BnBranchText("বিশ্বাস পাইলস ক্লিনিক", "কিষানগঞ্জ, ক্যালটেক্স চক, মোদি গোলা")',
                    '"falakata" to BnBranchText("মা আয়ুর্বেদ পাইলস ক্লিনিক", "ফালাকাটা, বিডিও অফিস রোড,\\n হোটেল নন্দনিকের কাছে।")',
                    '"birpara" to BnBranchText("মা আয়ুর্বেদ পাইলস ক্লিনিক", "বীরপাড়া, এমজি রোড, \\nঅ্যাক্সিস ব্যাংকের কাছে।")',
                ],
                "must_not": [],
            },
        },
    },
    {
        "row": "B266",
        "when": "02.08.2026 রাত ১২.৪৫ am-এর কাছাকাছি (TK-এর স্ক্রিনশট — বার্তার প্রিভিউ ডানে কেটে যাচ্ছিল); সংশোধন 02.08.2026 সকাল ~৯.১৫ am (TK-এর Android Studio build error screenshot — 'Unresolved reference: LayoutParams', B268-এর আওতায় সরাসরি ঠিক করা হলো)",
        "rule": "বার্তা পাঠানোর পপ-আপে প্রিভিউ TextView-এর MATCH_PARENT চওড়া থাকবে — লম্বা লাইন ডানে কাটবে না, পরের লাইনে নামবে। (ScrollView-এর নিজের কোনো LayoutParams ক্লাস নেই, তাই FrameLayout.LayoutParams ব্যবহার হবে -- আচরণ/চেহারা এক অক্ষরও বদলায়নি, শুধু কম্পাইল-ভাঙা টাইপের নাম ঠিক হয়েছে)",
        "files": {
            "PatientMessage.kt": {"must": ["android.widget.FrameLayout.LayoutParams(\n                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT"], "must_not": ["android.widget.ScrollView.LayoutParams"]},
            "DoctorVisitActivity.kt": {"must": ["LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT\n            )"], "must_not": []},
        },
    },
    {
        "row": "B265",
        "when": "02.08.2026 রাত ~১২.২৫ am (TK নিজে হুবহু পুরো বাংলা বার্তা লিখে পাঠিয়েছেন, বলেছেন 'এটাই ফাইনাল হবে') · হালনাগাদ B587 (08.08.2026) ও TK-এর সরাসরি নির্দেশ (15.08.2026, 'এটাই ফাইনাল Founder & Consultant') অনুযায়ী — সব ভাষায় এক সই",
        "rule": "Bill বার্তার বাংলা সংস্করণে 'ধন্যবাদান্তে' + TK BISWAS/Founder & Consultant সই + বাংলা ক্লিনিক-তথ্য",
        "files": {
            "PatientMessage.kt": {
                "must": [
                    'sb.append("প্রিয়\\n").append(who).append(",\\n")',
                    'sb.append("সম্পূর্ণ হয়েছে\\n")',
                    '"ধন্যবাদান্তে\\nTK BISWAS\\nFounder & Consultant\\n\\n"',
                ],
                "must_not": [],
            },
        },
    },
    {
        "row": "B263",
        "when": "02.08.2026 রাত ১২.০০–১২.১৫ am (TK দুই দফায় নমুনা দিয়ে চূড়ান্ত করেছেন) · হালনাগাদ B587 (08.08.2026) ও TK-এর সরাসরি নির্দেশ (15.08.2026, 'এটাই ফাইনাল Founder & Consultant')",
        "rule": "সব রোগী-বার্তার নিচে TK BISWAS/Founder & Consultant + ক্লিনিকের নাম/ঠিকানা/Helpline থাকবে, উপরে নয়",
        "files": {
            "PatientMessage.kt": {
                "must": ['"TK BISWAS\\nFounder & Consultant\\n\\n"', 'sb.append("*TK BISWAS*\\nFounder & Consultant\\n\\n")'],
                "must_not": [],
            },
        },
    },
    {
        "row": "item — Bill বার্তায় Paid Today (সংশোধিত)",
        "when": "01.08.2026 রাত ~১১.৩০–১১.৪৫ pm (TK প্রথমে 'আজকের পেমেন্ট দেখান' বলেন, পরে স্পষ্ট করেন তারিখ+বার+সময় লাগবে, আর সর্বমোট বিল শুধু প্রথমবার দেখাবে)",
        "rule": "Bill বার্তায় আজকের পেমেন্ট থাকলে তারিখ+বার+সময়সহ আলাদা ব্লকে বসবে (paidTodayAtIso দিয়ে), আর সর্বমোট বিল (Total Treatment Cost) শুধু showBillTotal=true হলে (রোগীর প্রথম পেমেন্ট-দিনে) দেখাবে",
        "files": {
            "PatientMessage.kt": {"must": ["fun dayName(d: java.util.Date, lang: String)", "if (showBillTotal) {"], "must_not": []},
            "PatientTimelineActivity.kt": {"must": ["val isFirstPaymentDay = treatmentDays.firstOrNull() == todayIso", "showBillTotal = isFirstPaymentDay"], "must_not": []},
        },
    },
    {
        "row": "item 12 (92-item তালিকা)",
        "when": "01.08.2026 রাত ১১.২৩ pm (TK-এর স্ক্রিনশট — Full Journey/Report Card/Payment বোতামের লেখা কাটা যাচ্ছিল); সংশোধন 02.08.2026 সকাল ~১১.১৯ am (TK আবার একই কাটা ধরেছেন — B260-এর `setAutoSizeTextTypeUniformWithConfiguration` আসলে `MaterialButton`-এ কাজ করে না বলে ধরা পড়ল, হাতে-মাপা শ্রিংক-টু-ফিট দিয়ে বদলানো হলো)",
        "rule": "PatientTimelineActivity-এর হেডারের চার বোতামে (Full Journey · Report Card · Payment · Action) লেখা কেটে যাওয়া (...) চলবে না — প্রয়োজনে টেক্সট-সাইজ নিজে থেকে ছোট হয়ে পুরো লেখা এক লাইনে দেখাবে",
        "files": {
            "PatientTimelineActivity.kt": {"must": ["btn.doOnLayout {"], "must_not": ["TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration"]},
        },
    },
    {
        "row": "item 11 (92-item তালিকা)",
        "when": "01.08.2026 রাত ১১.০৭ pm (TK-এর স্ক্রিনশট — \"Update Remark\" পপ-আপ পছন্দ হয়নি)",
        "rule": "যে কোনো নতুন/পুরনো পপ-আপে .setTitle() নয় — PremiumAlert.header() ব্যবহার করতেই হবে (আগে থেকে অনুমোদিত premium চেহারা, TK 25.07.2026)",
        "files": {
            "FollowUpActivity.kt": {"must": ['PremiumAlert.header(this, "📝 Update Remark'], "must_not": []},
            "PatientTimelineActivity.kt": {"must": ['PremiumAlert.header(this, "✏️ Edit Patient"'], "must_not": []},
            "InvestigationAdviceActivity.kt": {"must": ['com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title)'], "must_not": []},
        },
    },
]


def check_hidden_spinner():
    """🚔 খাতার সারি B126 (TK, 29.07.2026 সন্ধ্যা ৬.২৩ — *"Branch Open কেন হচ্ছে না?"*)

    লেআউটে `visibility="gone"` করা Spinner **কখনো layout হয় না**, আর Spinner-এর
    `onItemSelected` **layout-এর সময়েই** ডাকা হয় — তাই `setSelection(...)` লিখে
    তার ভরসায় বসে থাকলে বাছাইটা নিঃশব্দে হারিয়ে যায়। বিল্ড ভাঙে না, কোড চলে,
    শুধু কাজটা হয় না।

    নিয়ম: `setSelection(which)`-এর পরে **ওই একই জায়গাতেই** কাজটা করতে হবে
    (পিলের লেখা বসানো + তালিকা নতুন করে আনা)। পাহারাদার সেটাই মিলিয়ে দেখে।
    """
    for dp, _, fs in os.walk(NATIVE):
        for f in sorted(fs):
            if not f.endswith(".kt"):
                continue
            lines = read(os.path.join(dp, f)).split("\n")
            for i, line in enumerate(lines):
                if "setSelection(which)" not in line:
                    continue
                window = "\n".join(lines[i:i + 26])
                if "branchPicker.text" not in window:
                    fail("৯.১২",
                         f"[B126] {f}:{i+1} — লুকানো Spinner-এ setSelection(which) করে ছেড়ে দেওয়া হয়েছে। "
                         f"GONE Spinner কখনো layout হয় না, তাই onItemSelected চলবে না ও ব্রাঞ্চ বাছাই হারিয়ে যাবে। "
                         f"পপ-আপের ভিতরেই পিলের লেখা বসিয়ে তালিকা নতুন করে আনতে হবে "
                         f"(TK লক করেছেন 29.07.2026 6.45 pm)")


def check_locked_rules():
    for r in LOCKED_RULES:
        for fname, want in r["files"].items():
            p = os.path.join(NATIVE, fname)
            if not os.path.exists(p):
                # 🔒 খাতার সারি B134: কিছু লক করা নিয়ম কম্পিউটারের অ্যাপে
                # (`03_NETLIFY_READY/app.js`) — সেগুলোও এখানেই যাচাই হয়।
                alt = os.path.join(WEB, fname)
                if os.path.exists(alt):
                    p = alt
            if not os.path.exists(p):
                # 🔒 খাতার সারি B258: কিছু লক করা নিয়ম `clinical` প্যাকেজে
                # (যেমন Blood Test/Investigation Advice পর্দা) — সেগুলোও এখানেই।
                alt2 = os.path.join(CLINICAL, fname)
                if os.path.exists(alt2):
                    p = alt2
            if not os.path.exists(p):
                # 🔒 খাতার সারি B258.1: কিছু লক করা নিয়ম XML লেআউটে (যেমন
                # বোতামের ক্রম/লেখা) — res/layout-এও খুঁজতে হবে।
                alt3 = os.path.join(RES, "layout", fname)
                if os.path.exists(alt3):
                    p = alt3
            if not os.path.exists(p):
                # 🔒 খাতার সারি B269: কিছু লক করা নিয়ম `print` প্যাকেজে
                # (যেমন PrintCenterActivity.kt) — সেগুলোও এখানেই।
                alt4 = os.path.join(PRINT, fname)
                if os.path.exists(alt4):
                    p = alt4
            if not os.path.exists(p):
                # 🔒 খাতার সারি B283: স্থায়ী Password/Login Lock-এর নতুন
                # private module files `modules` প্যাকেজে থাকে।
                alt5 = os.path.join(MODULES, fname)
                if os.path.exists(alt5):
                    p = alt5
            if not os.path.exists(p):
                fail("৯.১২", f'[{r["row"]}] {fname} ফাইলটাই নেই — লক করা নিয়ম যাচাই করা গেল না')
                continue
            s = read(p)
            for m in want.get("must", []):
                if m not in s:
                    fail("৯.১২",
                         f'[{r["row"]}] নিয়ম ভাঙা → "{r["rule"]}" · {fname}-এ এই লাইনটা আর নেই: {m}'
                         f'  (TK লক করেছেন {r["when"]} — তাঁর অনুমতি ছাড়া বদলানো যাবে না)')
            for m in want.get("must_not", []):
                if m in s:
                    fail("৯.১২",
                         f'[{r["row"]}] নিয়ম ভাঙা → "{r["rule"]}" · {fname}-এ নিষিদ্ধ লেখাটা ফিরে এসেছে: {m}'
                         f'  (TK লক করেছেন {r["when"]})')


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১৩ — 🧾 কাজের নিয়মের পাহারা  (খাতার সারি B147)
#
#  TK-এর কথা (30.07.2026): *"tk_guard.py সব গুরুত্বপূর্ণ Working rule
#  পরীক্ষা করে না — অনেক নিয়ম শুধু Note-এ লেখা।"*
#
#  উপরের ৯.১২ পাহারা দেয় **ডিজাইন ও কোড ভাঙা**। এটা পাহারা দেয়
#  **কাজের নিয়ম (business rule)** — যেগুলো ভাঙলে বিল্ড ঠিকই হয়,
#  অ্যাপ চলে, শুধু **ক্লিনিকের হিসাব ভুল হয়ে যায়**। এই ভুল সবচেয়ে
#  বিপজ্জনক, কারণ চোখে পড়ে না।
#
#  ⛔ TK-এর অনুমতি ছাড়া এই তালিকার কোনো সারি মোছা বা দুর্বল করা যাবে না।
#  ⚠️ নতুন নিয়ম **এক এক করে** যোগ করতে হবে (TK-কে আগেই জানানো হয়েছে) —
#     ভুল যাচাই লিখলে ভালো ফাইলও আটকে যাবে।
# ═══════════════════════════════════════════════════════════════
WORK_RULES = [
    {
        "row": "B147-১",
        "when": "27.07.2026 5.02 pm",
        "rule": "টাকা শুধু রোগীর নিজের ব্রাঞ্চের স্টাফ · সেই ব্রাঞ্চের ডাক্তার · মাস্টার নিতে পারবে",
        "why": "অন্য ব্রাঞ্চের স্টাফ টাকা নিলে ওই ব্রাঞ্চের Today's Collection আর ড্রয়ারের ক্যাশ কোনোদিন মিলবে না",
        "files": {
            "MoneyBranchGuard.kt":  {"must": ["fun canTakeMoney(", "fun blockMessage("]},
            "PaymentActivity.kt":   {"must": ["MoneyBranchGuard.canTakeMoney(", "MoneyBranchGuard.blockMessage("]},
            "PaymentRepository.kt": {"must": ["MoneyBranchGuard.canTakeMoney("]},
            "FollowUpActivity.kt":  {"must": ["MoneyBranchGuard.canTakeMoney("]},
        },
    },
    {
        "row": "B147-২",
        "when": "26.07.2026",
        "rule": "একই রোগীর একই দিনে দ্বিতীয়বার টাকা বসানোর আগে সতর্কবার্তা দিতেই হবে",
        "why": "একই টাকা দুবার বসে গেলে দিনের হিসাব বাড়ে আর রোগীর Due ভুল দেখায়",
        # 🕒 V1106 (০৫.০৯.২০২৬) — নিয়মটা এক চুলও শিথিল হয়নি, শুধু ডাকার নামটা
        # বদলেছে: চারটে পর্দাই এখন `PaymentDayGuard.confirmBeforeSave(` ডাকে, যেটা
        # আগে ক্লাউডে "হুবহু এই অঙ্ক আজ বসেছে কিনা" দেখে, তারপর **সবসময়**
        # `confirmIfAlreadyPaidToday`-তেই গিয়ে ঠেকে। তাই পাহারাটা এখন `confirm`
        # পর্যন্ত দেখে — দুটোর যেকোনোটা থাকলেই নিয়ম মানা, একটাও না থাকলে ধরা পড়বে।
        "files": {
            "PaymentDayGuard.kt":            {"must": ["fun confirmIfAlreadyPaidToday(", "fun confirmBeforeSave("]},
            "PaymentActivity.kt":            {"must": ["PaymentDayGuard.confirm"]},
            "FollowUpActivity.kt":           {"must": ["PaymentDayGuard.confirm"]},
            "ChamberAttendanceActivity.kt":  {"must": ["PaymentDayGuard.confirm"]},
        },
    },
    {
        "row": "B147-৩",
        "when": "29.07.2026 10.30 pm",
        "rule": "মুছে ফেলা রেকর্ড কোনো ফোনের তালিকায় আর কখনো ফিরে আসবে না",
        "why": "ক্লাউড থেকে পুরনো সারি আবার নামলে ডিলিট করা রোগী/টাকা তালিকায় ফিরে আসত",
        "files": {
            "DeletedGuard.kt":             {"must": ["fun markDeleted(", "fun isDeleted("]},
            "FollowUpRepository.kt":       {"must": ["DeletedGuard.isDeleted("]},
            "EnquiryRepository.kt":        {"must": ["DeletedGuard.isDeleted("]},
            "RegistrationRepository.kt":   {"must": ["DeletedGuard.isDeleted("]},
            "PaymentRepository.kt":        {"must": ["DeletedGuard.isDeleted(", "DeletedGuard.markDeleted("]},
            "SupabaseClient.kt":           {"must": ["DeletedGuard.markDeleted("]},
        },
    },
    {
        "row": "B147-৪",
        "when": "26.07.2026",
        "rule": "টাকার পরিচয় — রোগীর আসল সারিটা এক জায়গার নিয়মেই বাছা হবে, আর কে নিয়েছে তার নাম বসবে",
        "why": "একই মোবাইলে একাধিক সারি থাকলে ভুল সারিতে টাকা বসে যেত (ব্রাঞ্চ বদলে যাওয়ার আসল কারণ)",
        "files": {
            "PatientIdentity.kt":   {"must": ["fun identityFilter(", "fun pickPatientRow("]},
            "PaymentRepository.kt": {"must": ["PatientIdentity.pickPatientRow(",
                                             "PatientIdentity.identityFilter(",
                                             "StaffDirectory.findAccount("]},
        },
    },
    {
        "row": "B147-৫",
        "when": "লক করা নিয়ম — পেমেন্টের ধরন",
        "rule": "পেমেন্টের ধরন শুধু দুটো — CASH আর ONLINE",
        "why": "তৃতীয় কোনো ধরন ঢুকলে চেম্বার রেজিস্টারের CASH/ONLINE ঘর আর দিনের হিসাবে মিলবে না",
        "files": {
            "PaymentActivity.kt": {"must": ['listOf("CASH", "ONLINE")'],
                                   "must_not": ['"CHEQUE"', '"DEBIT"', '"CREDIT"']},
        },
    },
    {
        "row": "B147-৬",
        "when": "27.07.2026 6.02 pm",
        "rule": "পেমেন্ট নেওয়ার ফরমে Remarks ঘর কখনো ফেরানো যাবে না",
        "why": "টাকা আগে নেওয়া হয়, চিকিৎসা পরে — তাই তখন Treatment Progress জানাই যায় না",
        "files": {
            "activity_payment.xml":   {"must_not": ["Remark"]},
            "dialog_nth_payment.xml": {"must_not": ["Remark"]},
        },
    },
    {
        "row": "B154 · B156",
        "when": "30.07.2026 সকাল ৮.৪৫",
        "rule": "Doctor Detail-এর তিনটে বাক্স হুবহু সমান · হেডারে ডিলিট নেই · View All পুরো পর্দা · Referred Patient-এ আসল তালিকা · Action-এ ধন্যবাদ ও ডিটেইলস",
        "why": "TK-কে একই কথা তৃতীয়বার বলতে হয়েছে — MaterialButton নিজের inset যোগ করে বলে মাপ মিলত না, আর AlertDialog-এর কনটেনার WRAP_CONTENT বলে পর্দা অর্ধেক আসত",
        "files": {
            "DoctorVisitActivity.kt": {
                "must": [
                    "fun actionButton(label: String, colorHex: String, onClick: () -> Unit) = TextView(this@DoctorVisitActivity)",
                    "android.widget.LinearLayout.LayoutParams(0, dgpx(38), 1f)",
                    # 🔴 TK-ORDER (31.07.2026, Section E — "Guard যদি পুরনো
                    # Button Design খুঁজে Fail করে, Guard-কেই বর্তমান
                    # approved Design অনুযায়ী আপডেট করবেন"): TK-এর B208
                    # ফটো-প্রুফ ("ওকে পছন্দ হয়েছে") অনুযায়ী এই দুই বাটনের
                    # আইকন ও লাইন-ব্রেক বাদ দেওয়া হয়েছিল, এক লাইনে ফিক্সড
                    # 10sp। Guard-এর পুরনো প্রত্যাশা এখানে আপডেট হলো, Design
                    # নিজে পুরনো নিয়মে ফেরানো হয়নি (নিয়ম অনুযায়ী সঠিক দিক)।
                    # 🔴 V1541 (১৯.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — উচ্চতা
                    # 52dp → 38dp। একই নিয়মে (Section E) Guard-এর প্রত্যাশাও
                    # এখানে নতুন approved মাপে আপডেট হলো।
                    '"Referred Patient"',
                    '"Referral Income"',
                    "fun forceDialogFullScreen(",
                    "forceDialogFullScreen(fsDialog, root)",
                    "fun openReferredList()",
                    "fun openAttachPatient(r: RefIncomeLine)",
                    "PATIENTS SENT BY THIS DOCTOR",
                    "REFERRAL INCOME WITH NO PATIENT ATTACHED",
                    '"\U0001F4E9 Msg 1 \u00b7 Intro & Request"',
                    '"\U0001F4E9 Msg 2 \u00b7 Patient Arrived"',
                    '"\U0001F4E9 Msg 3 \u00b7 Patient Details"',
                    '"\U0001F4E9 Msg 4 \u00b7 Referral Income Sent"',
                    "fun withLanguage(",
                    '"Select Language"',
                    "fun pickReferredPatient(",
                    "fun openDetailsMessageForm(",
                    "sendDoctorMessage(item.mobile, item.name, DoctorMessage.intro(",
                    "fun sendDoctorMessage(mobile: String, doctorName: String, text: String, logKind: String = \"\")",
                    'premiumDialogShell("\U0001F4E9", "Send Message")',
                ],
                "must_not": [
                    "MaterialButton(this@DoctorVisitActivity)",
                    'text = "\U0001F5D1\uFE0F"; textSize = 13f',
                ],
            },
            "DoctorVisitRepository.kt": {
                "must": ["fun attachPatientToReferralEntry(", "fun linkReferringDoctorIfBlank("],
            },
            "PatientMessage.kt": {
                "must": [
                    '"\U0001F4E9   Send Message"',
                    '"\U0001F4AC  WhatsApp"',
                    '"Later"',
                    "NoBengali.installDialog(dlg)",
                ],
                "must_not": ['.setTitle("Send to patient")'],
            },
            "DoctorMessage.kt": {
                "must": [
                    "fun intro(", "fun arrived(", "fun details(", "fun referralPaid(",
                    "fun isKishanganj(",
                    "private fun introHi(", "private fun arrivedHi(",
                    "private fun detailsHi(", "private fun referralPaidHi(",
                    "private fun introBn(", "private fun arrivedBn(",
                    "private fun detailsBn(", "private fun referralPaidBn(",
                    "\u0986\u09ae\u09be\u09a6\u09c7\u09b0 \u09ac\u09bf\u09b6\u09c7\u09b7\u09a4\u09cd\u09ac",
                    "Piles \u00b7 Fissure \u00b7 Fistula \u00b7 Hydrocele \u00b7 Gupt Rog",
                    "TK BISWAS",
                    "Founder & Consultant",
                ],
                "must_not": [
                    "\u09aa\u09cd\u09b0\u09a4\u09bf\u09a6\u09bf\u09a8 \u099a\u09c7\u09ae\u09cd\u09ac\u09be\u09b0\u09c7",
                ],
            },
        },
    },
    {
        "row": "B159 \u00b7 B160",
        "when": "30.07.2026 \u09a6\u09c1\u09aa\u09c1\u09b0 \u09e7\u09e8.\u09e9\u09eb",
        "rule": "\u0995\u09bf\u09b6\u09be\u09a8\u0997\u099e\u09cd\u099c\u09c7 \u09b9\u09bf\u09a8\u09cd\u09a6\u09bf/\u09ac\u09be\u0982\u09b2\u09be \u09ac\u09be\u099b\u09be\u0987 \u00b7 \u0993\u09df\u09c7\u09ac\u09c7\u0993 \u09ac\u09be\u0982\u09b2\u09be-\u09ac\u09a8\u09cd\u09a7 \u00b7 \u0993\u09df\u09c7\u09ac\u09c7 \u099c\u09be\u0995\u09cd\u09a4\u09be\u09b0\u09c7\u09b0 \u099b\u09be\u09b0\u099f\u09c7 \u09ac\u09be\u09b0\u09cd\u09a4\u09be \u00b7 Referred Patient \u0995\u09be\u09b0\u09cd\u09a1\u09c7 \u099a\u09be\u09aa\u09b2\u09c7 \u09aa\u09cd\u09b0\u09cb\u09ab\u09be\u0987\u09b2",
        "why": "TK: \u0995\u09bf\u09b6\u09be\u09a8\u0997\u099e\u09cd\u099c\u09c7 \u09a6\u09c1\u0987 \u09ad\u09be\u09b7\u09be\u09a4\u09c7\u0987 \u09a5\u09be\u0995\u09ac\u09c7, \u0986\u09b0 \u0995\u09ae\u09cd\u09aa\u09bf\u0989\u099f\u09be\u09b0\u09c7\u0993 \u0986\u099c\u0995\u09c7\u09b0 \u0995\u09be\u099c\u0997\u09c1\u09b2\u09cb \u099a\u09be\u0987",
        "files": {
            "app.js": {
                "must": [
                    "wlv1NoBnFix", "wlv1NoBnSweep", "wlv1NoBnStart",
                    "WLV1_NOBN_MOBILES", "6207841890",
                    "wlv1DocMsgBar(x.id)", "function wlv1DocMsg(",
                    "wlv1DocMsgPick", "Select Language",
                    "wlv1FullJourney('${esc(normMob(p.mobile))}')",
                ],
                "must_not": ["nodeName==='INPUT'"],
            },
        },
    },
    {
        "row": "B147-৭",
        "when": "লক করা নিয়ম — তারিখ",
        # 🔴 V1158 (৩১.১২.২০২৬ ৩.১৫ PM) — TK নিজে নিয়মটা বদলেছেন:
        #    *"31/12/2026 : 3.15 PM — সম্পূর্ণ প্রজেক্ট এরকম হতে হবে, প্রিন্ট আউট
        #    থেকে শুরু করে অ্যাপের মধ্যে থাকা সমস্ত জায়গায়"*।
        #    তাই পাহারার লক করা নমুনাও এখন স্ল্যাশ + " : " + বিন্দুর ঘড়ি।
        "rule": "তারিখ সবসময় স্ল্যাশ দিয়ে — 31/12/2026 · 31/12/2026 : 3.15 PM",
        "why": "রোগীর কাগজে ও পর্দায় দুই রকম তারিখ দেখালে কোনটা আসল বোঝা যায় না",
        "files": {
            "DateUtil.kt": {"must": ['"dd/MM/yyyy"', '"dd/MM/yyyy : h.mm a"']},
        },
    },
]


def work_rule_path(fname):
    """ফাইলটা কোথায় আছে খুঁজে দেয় — Kotlin · লেআউট · কম্পিউটারের অ্যাপ।"""
    for p in (os.path.join(NATIVE, fname),
              os.path.join(RES, "layout", fname),
              os.path.join(WEB, fname)):
        if os.path.exists(p):
            return p
    return None


def check_work_rules():
    for r in WORK_RULES:
        for fname, want in r["files"].items():
            p = work_rule_path(fname)
            if p is None:
                fail("৯.১৩", f'[{r["row"]}] {fname} ফাইলটাই নেই — কাজের নিয়ম যাচাই করা গেল না')
                continue
            s = read(p)
            for m in want.get("must", []):
                if m not in s:
                    fail("৯.১৩",
                         f'[{r["row"]}] কাজের নিয়ম ভাঙা → "{r["rule"]}" · {fname}-এ এই লেখাটা আর নেই: {m}'
                         f'  · কেন দরকার: {r["why"]}  (TK, {r["when"]})')
            for m in want.get("must_not", []):
                if m in s:
                    fail("৯.১৩",
                         f'[{r["row"]}] কাজের নিয়ম ভাঙা → "{r["rule"]}" · {fname}-এ নিষিদ্ধ লেখাটা ফিরে এসেছে: {m}'
                         f'  · কেন নিষেধ: {r["why"]}  (TK, {r["when"]})')


# ── B147-৮ ──────────────────────────────────────────────────────
#  followups টেবিলে বদল পাঠানোর আগে অবশ্যই resolveFollowUpId()
#
#  কেন: Supabase-এ `followups?id=eq.<ভুল আইডি>`-তে PATCH পাঠালেও
#  HTTP 200 আসে — একটাও সারি না বদলালেও। তাই ভুলটা নীরবে ঘটে,
#  স্টাফ ভাবেন লেখা হয়ে গেছে, আসলে কিছুই হয়নি।
# ────────────────────────────────────────────────────────────────
FOLLOWUP_WRITES = (".updateRemark(", ".updateNextFollow(", ".updateStatus(", ".resetCallCount(")
FOLLOWUP_SCREENS = ("FollowUpActivity.kt", "FollowCalendarActivity.kt", "PatientTimelineActivity.kt")


def check_followup_id():
    for fname in FOLLOWUP_SCREENS:
        p = os.path.join(NATIVE, fname)
        if not os.path.exists(p):
            fail("৯.১৩", f"[B147-৮] {fname} নেই — followups-এর নিয়ম যাচাই করা গেল না")
            continue
        lines = read(p).split("\n")
        for i, line in enumerate(lines):
            t = line.strip()
            if t.startswith("//") or t.startswith("*") or t.startswith("/*"):
                continue
            if not any(w in line for w in FOLLOWUP_WRITES):
                continue
            window = "\n".join(lines[i:i + 3])
            if "resolveFollowUpId" not in window:
                fail("৯.১৩",
                     f"[B147-৮] {fname}:{i+1} — followups-এ বদল পাঠানো হচ্ছে কিন্তু resolveFollowUpId() "
                     f"দিয়ে আসল সারিটা খোঁজা হয়নি। ভুল আইডিতে PATCH পাঠালেও Supabase 200 বলে, "
                     f"তাই স্টাফের লেখা নীরবে হারিয়ে যাবে (TK-এর লক করা নিয়ম)")


# ── B147-৯ ──────────────────────────────────────────────────────
#  ব্রাঞ্চের ছাঁকনিতে নাম সবসময় encode করা
#
#  কেন: "Cooch Behar"-এ ফাঁকা জায়গা আছে। encode না করলে
#  `branch=eq.Cooch Behar` অনুরোধটাই ভেঙে যায় — তালিকা ফাঁকা আসে
#  অথবা অন্য ব্রাঞ্চের রোগী দেখা যায়। দুটোই চালু ক্লিনিকে মারাত্মক।
# ────────────────────────────────────────────────────────────────
def check_branch_encoded():
    for dp, _, fs in os.walk(NATIVE):
        for f in sorted(fs):
            if not f.endswith(".kt"):
                continue
            lines = read(os.path.join(dp, f)).split("\n")
            for i, line in enumerate(lines):
                if "branch=eq." not in line:
                    continue
                t = line.strip()
                if t.startswith("//") or t.startswith("*") or t.startswith("/*"):
                    continue
                window = "\n".join(lines[i:i + 2])
                if ("URLEncoder.encode" not in window) and ("enc(" not in window):
                    fail("৯.১৩",
                         f"[B147-৯] {f}:{i+1} — branch=eq. ছাঁকনিতে ব্রাঞ্চের নাম encode করা হয়নি। "
                         f'"Cooch Behar"-এর মতো ফাঁকা জায়গাওয়ালা নামে অনুরোধ ভেঙে যাবে — '
                         f"তালিকা ফাঁকা আসবে বা অন্য ব্রাঞ্চের রোগী দেখাবে")



# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.১৪ — 🚫 বাংলা-বন্ধ স্টাফের পর্দায় একটাও বাংলা অক্ষর নেই
#
#  খাতার সারি B158 · V451 (19.08.2026) TK-এর চূড়ান্ত branch-rule:
#  Kishanganj-এর সব Staff account এবং KNE-BRANCH (branch-number login)-এ
#  বাংলা থাকবে না। পুরনো KNE-KISHAN5 fallback-ও অটুট থাকবে।
#  TK বলেছেন: *"পরবর্তী সেশনে যেন আবার বলতে না হয়।"*
#
#  এই যাচাই তাই **প্রমাণ করে দেখায়** —
#   ১. `NoBengali.kt` আছে, তালিকায় ওই স্টাফ আছে।
#   ২. পুরো অ্যাপে পাহারা বসানো আছে (Application) ও পপ-আপে (PremiumAlert,
#      দুই overload) ও Toast-এ (বাংলা লেখা প্রতিটা Toast ঢাকা)।
#   ৩. **প্রজেক্টের প্রতিটা আসল বাংলা লেখা** (কোড ও XML) ওই নিয়মে চালিয়ে
#      দেখা হয় — একটাও বাংলা অক্ষর টিকে থাকলে ফাইল বানানো আটকে যায়।
#      ⛔ অর্থাৎ ভবিষ্যতে কেউ নতুন বাংলা লেখা যোগ করলে **সঙ্গে সঙ্গে ধরা পড়বে**।
#
#  ⛔ রোগীর কাছে যাওয়া লেখা (PatientMessage · DoctorMessage · ClinicalRepository
#     · ছাপার লেখা) ইচ্ছে করে বাদ — ওগুলো রোগীর নিজের ভাষা, TK-এর লক করা
#     তিন-ভাষার নিয়ম (খাতার সারি B17)।
# ═══════════════════════════════════════════════════════════════
BN_RE = re.compile(r"[\u0980-\u09FF]")
BN_SKIP_FILES = {"PatientMessage.kt", "DoctorMessage.kt", "ClinicalRepository.kt",
                 "PrintTextEnglish.kt", "NoBengali.kt"}


def _bn_strip_comments(src):
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith('"""', i):
            j = src.find('"""', i + 3); j = n if j == -1 else j + 3
            out.append(src[i:j]); i = j; continue
        if c == '"':
            j = i + 1
            while j < n:
                if src[j] == '\\': j += 2; continue
                if src[j] == '"': j += 1; break
                if src[j] == '\n': break
                j += 1
            out.append(src[i:j]); i = j; continue
        if c == "'":
            j = i + 1
            while j < n and src[j] != "'":
                if src[j] == '\\': j += 1
                j += 1
            out.append(src[i:j + 1]); i = j + 1; continue
        if src.startswith('//', i):
            j = src.find('\n', i); i = n if j == -1 else j
            out.append('\n'); continue
        if src.startswith('/*', i):
            d, j = 1, i + 2
            while j < n and d:
                if src.startswith('/*', j): d += 1; j += 2; continue
                if src.startswith('*/', j): d -= 1; j += 2; continue
                j += 1
            i = j; out.append(' '); continue
        out.append(c); i += 1
    return "".join(out)


def _bn_read_maps(src):
    def one(name):
        try:
            body = src.split("private val %s: Map<String, String> = mapOf(" % name)[1].split("\n    )")[0]
        except IndexError:
            return None
        pairs = re.findall(r'"((?:[^"\\]|\\.)*)"\s+to\s+"((?:[^"\\]|\\.)*)"', body)
        un = lambda t: t.replace('\\$', '$').replace('\\"', '"').replace('\\u0964', '\u0964').replace('\\\\', '\\')
        return {un(a): un(b) for a, b in pairs}
    return one("WHOLE"), one("MAP")


# ══════════════════════════════════════════════════════════════════════
#  যাচাই ৯.১৫ — 📅⛔ তারিখ/সময় আঁকা ইমোজি পর্দায় কোথাও নয়
#
#  TK (০৫.০৯.২০২৬): *"July 17 Emoji থাকবে না · আর কতবার আপনাকে বলবো ·
#  ভবিষ্যতের জন্য এই ইমোজি ব্লক করে রাখুন"*
#
#  🔴 কেন: 📅 ইমোজিটা ফোনভেদে **"JULY 17"** লেখা একটা ক্যালেন্ডার আঁকে,
#     আর 🕐-🕧 ঘড়ির মুখগুলো **একটা নির্দিষ্ট সময়** আঁকে। ফলে পর্দায়
#     ভুল তারিখ/সময় লেখা থাকে — রোগীর কাগজে বা ডাক্তারের পর্দায় সেটা
#     সরাসরি বিভ্রান্তি। TK এটা আগেও বলেছিলেন (V699-এ
#     `ic_pd_calendar_green`/`ic_pd_clock` বসানো হয়েছিল), তবু আবার ফিরে এসেছে।
#  ⇒ এখন থেকে পাহারাদারই আটকাবে — **ব্যবহারকারী যা দেখেন** সেই লেখায়
#     (`android:text` · `android:hint` · Kotlin/JS-এর স্ট্রিং) এই ইমোজিগুলো
#     থাকলে ফাইল বানানো যাবে না।
#  ⛔ **কমেন্টে (ব্যাখ্যায়) থাকলে কিছু হয় না** — সেটা কেউ পর্দায় দেখে না।
#  ⛔ বদলে ব্যবহার করুন: `ic_pd_calendar_green` · `ic_pd_clock` (আঁকা নেই)।
# ══════════════════════════════════════════════════════════════════════
DATE_TIME_EMOJI = ["\U0001F4C5", "\U0001F4C6", "\U0001F5D3"] + [chr(c) for c in range(0x1F550, 0x1F568)]

def _dte_strip_comments(text, kind):
    """কমেন্ট বাদ দেওয়া — ব্যাখ্যায় ইমোজি থাকলে কেউ পর্দায় দেখে না।"""
    import re as _re
    if kind == "xml":
        return _re.sub(r"<!--.*?-->", " ", text, flags=_re.S)
    # kt / js — ব্লক ও এক-লাইনের কমেন্ট
    text = _re.sub(r"/\*.*?\*/", " ", text, flags=_re.S)
    out = []
    for line in text.split("\n"):
        i = line.find("//")
        out.append(line if i < 0 else line[:i])
    return "\n".join(out)

def check_no_date_emoji():
    bad = []
    scans = [(JAVA, ".kt", "kt"), (os.path.join(RES, "layout"), ".xml", "xml"),
             (WEB, ".js", "js")]
    for root, ext, kind in scans:
        if not os.path.isdir(root):
            continue
        for dp, _, fs in os.walk(root):
            for f in sorted(fs):
                if not f.endswith(ext):
                    continue
                body = _dte_strip_comments(read(os.path.join(dp, f)), kind)
                for em in DATE_TIME_EMOJI:
                    if em not in body:
                        continue
                    for n, line in enumerate(body.split("\n"), 1):
                        if em in line:
                            bad.append("%s:%d  %s" % (f, n, em))
                            break
    for b in bad[:12]:
        fail("৯.৪৩",
             "[TK \u09e6\u09eb.\u09e6\u09ef.\u09e8\u09e6\u09e8\u09ec] "
             "\u09a4\u09be\u09b0\u09bf\u0996/\u09b8\u09ae\u09df \u0986\u0981\u0995\u09be "
             "\u0987\u09ae\u09cb\u099c\u09bf \u09aa\u09b0\u09cd\u09a6\u09be\u09b0 "
             "\u09b2\u09c7\u0996\u09be\u09df \u2014 " + b +
             "  \u00b7  TK: \"July 17 Emoji \u09a5\u09be\u0995\u09ac\u09c7 \u09a8\u09be\" "
             "\u00b7 \u09ac\u09a6\u09b2\u09c7 ic_pd_calendar_green / ic_pd_clock")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৪৪ — Java-র সংরক্ষিত শব্দ `native` কোনো ঘোষণায় নয়
#  (TK-এর Studio-তে V1119 বিল্ড ভেঙেছিল, ০৫.০৯.২০২৬ — তাঁর ছবি থেকে ধরা)
#
#  বিল্ডের সময় kapt প্রতিটা Kotlin ক্লাসের একটা **Java নকল** (stub) বানায়।
#  প্রকল্পের একটা প্যাকেজের নাম `...pilesclinic.native`, আর Java-তে `native`
#  একটা **সংরক্ষিত শব্দ** — তাই ওই নামটা কোনো **ঘোষণায়** (ফাংশনের ফেরত-ধরন ·
#  প্যারামিটার · property-র ধরন) থাকলে নকল ফাইলটাই ভেঙে যায়
#  (`<identifier> expected` · `illegal start of type`) আর বিল্ড থামে।
#  ⛔ ফাংশনের **ভিতরে** (শরীরে) ওই নাম থাকলে কোনো সমস্যা নেই — kapt শরীরটা
#     ফেলে দেয়। তাই এই পাহারা শুধু ঘোষণার লাইনগুলোই দেখে।
#  ⛔ `native` প্যাকেজের নিজের ফাইলগুলো এই যাচাইয়ের বাইরে — ওগুলো এতদিন
#     ঠিকঠাক বিল্ড হয়েছে, নতুন করে কিছু বদলানোর দরকার নেই।
# ═══════════════════════════════════════════════════════════════
NATIVE_FQN = "com.tkbiswas.pilesclinic.native."
_NATIVE_SIG_PATS = [
    re.compile(r"\bfun\s+\w+[^=]*:\s*[^=]*" + re.escape(NATIVE_FQN)),
    re.compile(r"\b(?:val|var)\s+\w+\s*:\s*[^=]*" + re.escape(NATIVE_FQN)),
    re.compile(r"\bfun\s+\w+\s*\([^)]*" + re.escape(NATIVE_FQN)),
]

# 🔴 V1220 (০৮.০৯.২০২৬) — এই পাহারার **ফাঁক**, TK-র Studio-তে আবার বিল্ড ভাঙায় ধরা।
#   উপরের নিয়মগুলো শুধু **কোলন দিয়ে লেখা** ধরন দেখত (`val x: com...native.Foo`)।
#   কিন্তু ঘরের ধরন **নিজে থেকে বুঝে নেওয়া** হলেও (কোলন ছাড়া) kapt-এর Java নকলে
#   পুরো নামটাই বসে — যেমন V1215-এর
#   `private val collRowsByDate = HashMap<String, MutableList<com...native.CollectionRow>>()`
#   ⇒ নকল ভেঙে `<identifier> expected`।
#   ⇒ তাই এখন **ক্লাসের ঘর** (ঠিক ৪ ফাঁকা দিয়ে শুরু) হলে ইনিশিয়ালাইজারও দেখা হয়,
#     কিন্তু **শুধু দুটো সত্যিকারের বিপদে** — (১) `<…>`-এর ভিতরে native টাইপ,
#     (২) সরাসরি `native.ClassName(` নতুন বস্তু বানানো।
#   ⛔ `= com...native.MoneyFormat.inr(n)`-এর মতো ডাক ধরা হয় না — ওর ধরন String,
#      ওতে বিল্ড ভাঙে না; মিথ্যা সতর্কতা দিয়ে TK-র সময় নষ্ট করা হবে না।
_NATIVE_FIELD_PATS = [
    re.compile(r"<[^<>]*" + re.escape(NATIVE_FQN)),
    re.compile(re.escape(NATIVE_FQN) + r"[A-Z]\w*\s*\("),
]


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৪৫ — 'use strict' ফাইলে **ঘোষণা ছাড়া** ঘর বসানো নিষেধ
#  (TK-রিপোর্ট ০৮.০৯.২০২৬: *"CHECK-UP QUEUE-তে Check-up এ কাজ করে না"*)
#
#  `app.js`-এর প্রথম লাইনেই `'use strict'`। ওই অবস্থায় `var/let/const` ছাড়া
#  কোনো নাম-ঘরে মান বসালে JavaScript **ReferenceError** ছোড়ে, আর ওই লাইনেই
#  পুরো ফাংশনটা থেমে যায়। V1225-এ ঠিক এটাই হয়েছিল — `wlv1ChkFistula=(arr)=>…`
#  ঘোষণা ছাড়া বসানো ছিল, তাই `doctorCheck()` ওখানেই থেমে যেত ⇒ **চেকআপ ফর্ম
#  কখনো খুলত না**, আর ডাকার জায়গার try/catch ভুলটা গিলে ফেলায় পর্দায় কোনো
#  বার্তাও আসত না — বোতামটা মরা মনে হত।
#  ⛔ `node --check` এটা ধরতে পারে না (লেখা ঠিকই আছে, ভুলটা চলার সময়ের)।
#  ⛔ এক লাইনে একাধিক ঘোষণা (`let a=1, b=2`) ও ফাংশনের ঘর ধরা হয়, তাই
#     মিথ্যা সতর্কতা দিয়ে TK-র সময় নষ্ট হয় না (নিয়ম ৫ক)।
# ═══════════════════════════════════════════════════════════════
def check_strict_undeclared():
    import glob as _g
    for js in sorted(_g.glob(os.path.join(WEB, "*.js"))):
        if os.path.basename(js).startswith("V"):
            continue
        s = read(js)
        if "'use strict'" not in s and '"use strict"' not in s:
            continue
        declared = set(re.findall(r"\b(?:var|let|const|function)\s+([A-Za-z_$][\w$]*)", s))
        declared |= set(re.findall(r"\bcatch\s*\(\s*([A-Za-z_$][\w$]*)", s))
        # এক লাইনে একাধিক ঘোষণা:  let a=1, b=2, c=3
        # ⚠️ একাধিক লাইনে ছড়ানো ঘোষণাও ধরা হয় (`var a=1,\n    b=2;`) — নইলে
        #    ভুল করে "ঘোষণা নেই" বলে মিথ্যা সতর্কতা দিত (নিয়ম ৫ক)।
        for m in re.finditer(r"\b(?:var|let|const)\s+([^;]{0,800})", s):
            for nm in re.findall(r"(?:^|,)\s*([A-Za-z_$][\w$]*)\s*(?==|,|$)", m.group(1)):
                declared.add(nm)
        # ফাংশনের ঘর (parameters)
        for m in re.finditer(r"function\s*\*?\s*[A-Za-z_$\w]*\s*\(([^)]{0,300})\)", s):
            for nm in re.findall(r"[A-Za-z_$][\w$]*", m.group(1)):
                declared.add(nm)
        # ⚠️ লেখা/টেমপ্লেটের ভিতরের অংশ কোড নয় — মিথ্যা সতর্কতা ঠেকাতে মুখোশ।
        mask = _js_code_mask(s)
        for m in re.finditer(r"(?m)^[ \t]*([A-Za-z_$][\w$]*)\s*=(?!=)", s):
            n = m.group(1)
            if n in declared or n == "window":
                continue
            if not mask[m.start()]:
                continue                  # মন্তব্য বা লেখার ভিতরে
            # ব্যাকটিক-লেখার (template) ভিতরেও কোড নয় — আগে কতগুলো ব্যাকটিক
            # আছে গুনে দেখা হয় (বিজোড় হলে ভিতরে আছি)।
            if s.count("`", 0, m.start()) % 2 == 1:
                continue
            # আগের লাইন কমা দিয়ে শেষ হলে এটা একই ঘোষণার পরের অংশ
            head = s[:m.start()].rstrip()
            prev = head.rsplit("\n", 1)[-1].strip() if "\n" in head else head.strip()
            if prev.endswith(","):
                continue
            line = s[:m.start()].count("\n") + 1
            fail("৯.৪৫",
                 f"{os.path.basename(js)}:{line} — `{n}` **ঘোষণা ছাড়া** বসানো হয়েছে, "
                 f"অথচ ফাইলটা `'use strict'` ⇒ চালানোর সময় ReferenceError, ওই "
                 f"ফাংশনটা ওখানেই থেমে যাবে (বোতাম মরা মনে হবে)। "
                 f"সমাধান: সামনে `var` বসান।")


def check_no_native_in_signature():
    if not os.path.isdir(JAVA):
        return
    bad = []
    for dp, _, fs in os.walk(JAVA):
        if os.path.basename(dp) == "native":
            continue
        for f in sorted(fs):
            if not f.endswith(".kt"):
                continue
            for n, line in enumerate(read(os.path.join(dp, f)).split("\n"), 1):
                t = line.strip()
                if NATIVE_FQN not in t or t.startswith(("//", "*", "/*")):
                    continue
                hit = any(p.search(t) for p in _NATIVE_SIG_PATS)
                # 🔴 V1220 — ক্লাসের ঘর (ঠিক ৪ ফাঁকা), ধরন নিজে থেকে বোঝা।
                if not hit and line.startswith("    ") and not line.startswith("     "):
                    if re.match(r"\s*(?:private |internal |protected |public )?(?:val|var)\s+\w+\s*=", line):
                        hit = any(p.search(t) for p in _NATIVE_FIELD_PATS)
                if hit:
                    bad.append("%s:%d  %s" % (f, n, t[:110]))
    for b in bad[:8]:
        fail("৯.৪৪",
             "ঘোষণার ভিতরে `" + NATIVE_FQN + "` — kapt-এর Java নকলে `native` "
             "সংরক্ষিত শব্দ, তাই Android Studio-তে বিল্ড ভাঙবে → " + b +
             "  ·  সমাধান: ধরনটা ঘোষণা থেকে সরান (ফাংশনের ভিতরে রাখলে সমস্যা নেই)")


def check_no_bengali():
    nb = os.path.join(NATIVE, "NoBengali.kt")
    if not os.path.exists(nb):
        fail("৯.১৪", "[B158] NoBengali.kt ফাইলটাই নেই — বাংলা-বন্ধ ব্যবস্থাটা মুছে গেছে")
        return
    src = read(nb)
    # 🔴 সবচেয়ে জরুরি: লেখার ঘরে (EditText) হাত দেওয়া চলবে না — নইলে পুরনো
    #    রেকর্ডের বাংলা Remark/নাম সেভ করার সময় নষ্ট হয়ে যাবে (30.07.2026-এ
    #    নিজের কাজ আবার যাচাই করতে গিয়ে এই বিপদটা ধরা পড়েছিল)।
    if "v is android.widget.EditText" not in src:
        fail("৯.১৪", "[B158] NoBengali.kt-এ EditText-এর সুরক্ষা নেই — ইনপুট ঘরের লেখা বদলে গেলে "
                     "পুরনো রেকর্ডের বাংলা তথ্য সেভ করার সময় চিরতরে নষ্ট হয়ে যাবে")
    for must in ('"6207841890"', '"KNE-KISHAN5"', "fun hookApp(", "fun installDialog(",
                 "fun install(", "fun fix(", "fun s("):
        if must not in src:
            fail("৯.১৪", f"[B158] NoBengali.kt-এ এই অংশটা আর নেই: {must}")
    # 🔒 V451: ব্যক্তি-নির্ভর rule-এ আবার ফিরে যাওয়া যাবে না।
    for must in ('role.equals("staff", ignoreCase = true)',
                 'branch.equals("Kishanganj", ignoreCase = true)',
                 'val isKishanganjStaff'):
        if must not in src:
            fail("৯.১৪", f"[V451] Kishanganj Staff branch-wide No-Bengali guard নেই: {must}")
    web_app = os.path.join(WEB, "app.js")
    if os.path.exists(web_app):
        web_src = read(web_app)
        for must in ("role==='staff'&&br==='kishanganj'", "WLV1_NOBN_MOBILES", "WLV1_NOBN_CODES"):
            if must not in web_src:
                fail("৯.১৪", f"[V451] Web Kishanganj Staff branch-wide No-Bengali guard নেই: {must}")
    else:
        fail("৯.১৪", "[V451] 03_NETLIFY_READY/app.js নেই")
    app = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "PilesClinicApplication.kt")
    if os.path.exists(app):
        if "NoBengali.hookApp(this)" not in read(app):
            fail("৯.১৪", "[B158] PilesClinicApplication-এ NoBengali.hookApp(this) নেই — তাহলে কোনো পর্দাতেই পাহারা বসবে না")
    else:
        fail("৯.১৪", "[B158] PilesClinicApplication.kt নেই")
    pa = os.path.join(NATIVE, "PremiumAlert.kt")
    if os.path.exists(pa):
        if read(pa).count("NoBengali.installDialog(dialog)") < 2:
            fail("৯.১৪", "[B158] PremiumAlert.paint-এর দুটো overload-এ NoBengali.installDialog(dialog) থাকতে হবে — পপ-আপের লেখা তখনই ঢাকা পড়ে")
    else:
        fail("৯.১৪", "[B158] PremiumAlert.kt নেই")

    WHOLE, MAP = _bn_read_maps(src)
    if not MAP:
        fail("৯.১৪", "[B158] NoBengali.kt-এর অনুবাদের তালিকা (MAP) পড়া গেল না")
        return
    WHOLE = WHOLE or {}
    ordered = sorted(MAP.items(), key=lambda kv: -len(kv[0]))

    def fix(t):
        if not BN_RE.search(t):
            return t
        if t.strip() in WHOLE:
            return WHOLE[t.strip()]
        s2 = t
        for bn, en in ordered:
            if bn in s2:
                s2 = s2.replace(bn, en)
        # ⛔ এখানে ইচ্ছে করেই "শেষ জাল" (বাংলা অক্ষর মুছে ফেলা) চালানো হয় না —
        #    নইলে অনুবাদ ছাড়া নতুন বাংলা লেখাও "ঠিক আছে" বলে পাশ হয়ে যেত,
        #    অথচ ফোনে ওই লেখাটা **উবে যেত** (স্টাফ কিছুই বুঝতেন না)।
        #    তাই অনুবাদ না থাকলে এখানেই ধরা পড়বে।
        return s2.replace('\u0964', '.')

    lit = re.compile(r'"((?:[^"\\\n]|\\.)*)"')
    bad = 0
    # 🔴 B347 (03.08.2026, TK-নির্দেশ — "এখনই ঠিক করতে হবে, ঝুঁকিহীনভাবে"):
    # আগে শুধু NATIVE ফোল্ডার স্ক্যান হতো — MODULES (Staff Profile/Work
    # Notebook/Income-Expense) স্ক্যানের বাইরে থেকে যেত। IncomeExpenseActivity.kt-এর
    # ১০টা পুরনো বাংলা লাইন ইংরেজি করে দেওয়ার পরেই (একই দিনে) এই স্ক্যান
    # স্থায়ীভাবে দুটো ফোল্ডার কভার করছে — এখন থেকে ভবিষ্যতেও এই ফাঁক ফিরে
    # আসবে না।
    for scan_dir in (NATIVE, MODULES):
      for dp, _, fs in os.walk(scan_dir):
        for f in sorted(fs):
            if not f.endswith(".kt") or f in BN_SKIP_FILES:
                continue
            clean = _bn_strip_comments(read(os.path.join(dp, f)))
            for m in lit.finditer(clean):
                t = m.group(1).replace('\\"', '"')
                if not BN_RE.search(t):
                    continue
                if BN_RE.search(fix(t)) and bad < 12:
                    bad += 1
                    fail("৯.১৪", f"[B158] {f} — এই বাংলা লেখাটার অনুবাদ NoBengali.kt-এ নেই, তাই "
                                 f"বাংলা-বন্ধ স্টাফের পর্দায় বাংলা থেকে যাবে: {t[:70]}")
            # 🔴 বাংলা লেখাওয়ালা Toast অবশ্যই NoBengali দিয়ে ঢাকা থাকতে হবে
            for m in re.finditer(r"Toast\.makeText\(", clean):
                seg = clean[m.end():m.end() + 400]
                cut = seg.find(").show()")
                seg = seg if cut == -1 else seg[:cut]
                if BN_RE.search(seg) and "NoBengali." not in seg:
                    fail("৯.১৪", f"[B158] {f} — বাংলা লেখাওয়ালা একটা Toast NoBengali দিয়ে ঢাকা নেই "
                                 f"(Toast-এর নিজের উইন্ডো, তাই পর্দার পাহারা ওখানে পৌঁছায় না)")
                    break
    for dp, _, fs in os.walk(os.path.join(RES, "layout")):
        for f in sorted(fs):
            if not f.endswith(".xml"):
                continue
            src2 = re.sub(r"<!--.*?-->", " ", read(os.path.join(dp, f)), flags=re.S)
            for m in re.finditer(r'android:(?:text|hint)="([^"]*)"', src2):
                t = m.group(1)
                if BN_RE.search(t) and BN_RE.search(fix(t)):
                    fail("৯.১৪", f"[B158] XML {f} — এই বাংলা লেখাটার অনুবাদ NoBengali.kt-এ নেই: {t[:60]}")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ১০ — কম্পিউটারের অ্যাপ (সার্কুলার ৯.৯)
# ═══════════════════════════════════════════════════════════════
def check_web():
    import subprocess
    for f in ("app.js", "config.js"):
        p = os.path.join(WEB, f)
        if not os.path.exists(p):
            fail("৯.৯", f"{f} নেই"); continue
        try:
            r = subprocess.run(["node", "--check", p], capture_output=True, text=True)
            if r.returncode != 0:
                fail("৯.৯", f"{f} ভাঙা → {r.stderr.strip()[:120]}")
        except FileNotFoundError:
            note("৯.৯", "node নেই — কম্পিউটারের অ্যাপ যাচাই করা যায়নি, হাতে দেখতে হবে")
    # ⛔ সার্কুলার ১০: select('*') কখনো ছোট করা যাবে না
    p = os.path.join(WEB, "app.js")
    if os.path.exists(p) and "select('*')" not in read(p):
        fail("১০", "app.js-এ select('*') নেই — কম ঘর নামালে ক্লাউডে তথ্য মুছে যাবে")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ১১ — বাধ্যতামূলক নোট ফাইল (সার্কুলার ৪.৬)
# ═══════════════════════════════════════════════════════════════
def check_notes():
    must = [
        "00_TK_SARKULAR_LOCK.md",
        "00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md",
        "00_TK_KAJER_TARIKH_SOMOY_LOG.md",
        "00_PROJECT_STATE_MASTER_NOTE.md",
        "00_TK_PORER_SESSION_SOBAR_AGE_PORUN.md",
    ]
    miss = [f for f in must if not os.path.exists(os.path.join(ROOT, f))]
    if miss:
        fail("৪.৬", "এই নোট ফাইল নেই → " + " · ".join(miss))
    if not glob.glob(os.path.join(ROOT, "00_LOCK_NOTE_SESSION_*.md")):
        fail("৪.৬", "সেশনের LOCK NOTE নেই")
    # 🔒 V691 (TK, ২৬.০৮.২০২৬) — TK-এর নতুন স্থায়ী নিয়ম (অগ্রগতি % + বাকি
    # সময়, ২-৩ লাইন, আন্দাজ নিষেধ, Web+Android দুটোতেই, নিজে ডিসিশন নয়,
    # Supabase free-র ঝুঁকি আগে জানানো, Fast mode) যেন কোনো সেশনে চুপচাপ
    # মুছে না যায়। TK বলেছেন "স্থায়ী নিয়ম অনুযায়ী লক করে রাখুন" — কাগজে
    # লেখা যথেষ্ট নয়, পাহারাদারেই ধরা থাকল।
    for _f, _mark in (
        ("00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md", "(TK, ২৬.০৮.২০২৬ — স্থায়ী, লক করা)"),
        ("00_TK_SOB_NIYOM_EK_JAYGAY_LOCKED.md", "TK-এর নতুন স্থায়ী নিয়ম — ২৬.০৮.২০২৬"),
        ("00_TK_SESSION_NIYOM_STHAYI_PORUN.md", "TK-এর নতুন স্থায়ী নিয়ম — ২৬.০৮.২০২৬"),
    ):
        _p = os.path.join(ROOT, _f)
        if not os.path.exists(_p) or _mark not in read(_p):
            fail("৪.৬", "TK-এর ২৬.০৮.২০২৬-এর লক করা নিয়ম %s-এ নেই" % _f)
    # খাতায় 🔴 বাকি আছে কিনা
    k = os.path.join(ROOT, "00_TK_KAJER_KHATA_SOBAR_AGE_PORUN.md")
    if os.path.exists(k):
        left = []
        for name, body in re.findall(r'\| (B\d+) \|(.*)', read(k)):
            seg = body.split("|")[-1]
            if "🔴" in seg and "🟢" not in seg:
                left.append(name)
        if left:
            note("৬.১", "খাতায় এখনো বাকি → " + " · ".join(left))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ১২ — সম্পূর্ণ প্রজেক্ট (সার্কুলার ৪.৫)
# ═══════════════════════════════════════════════════════════════
# 🔴 নিয়ম বদল (03.08.2026, TK-এর স্পষ্ট অনুমতি): ".git ফোল্ডার বাদ দেওয়া
# যাবে না" — এই পুরনো নিয়মটা (27.07.2026) TK নিজে শিথিল করেছেন, কারণ ফাইলের
# সাইজ কমানোর জন্য .git (গিট-এর নিজস্ব ইতিহাস-ফোল্ডার, শুধু programmer-দের
# টুলের জন্য, Android Studio build-এ কোনো ভূমিকা নেই) সরানো নিরাপদ কিনা
# জিজ্ঞাসা করেছিলেন। Claude প্রস্তাব দিয়েছিল ".git সরালে কাজ/ডিজাইনের কোনো
# ক্ষতি হয় না, আসল ভার্সন-ইতিহাস তো নাম্বার দেওয়া ZIP-গুলোতেই (V221...
# FINAL_60...) থাকে, যেগুলো TK নিজে সেভ করে রাখেন" — TK নিজের কথায় সম্মত
# হয়ে বলেছেন: *"আপনি যেটা ভালো বোঝেন সেটা করে দেন, তবে ভবিষ্যতে কোনো
# অসুবিধা থাকলে দায়বদ্ধতা আপনার — আপনি সেভাবে কোডে লিখে রাখুন।"*
# ⛔ **দায়বদ্ধতার নোট (Claude-এর নিজের সিদ্ধান্তের রেকর্ড):** .git ফোল্ডার
# সরানোর সিদ্ধান্ত ও সুপারিশ সম্পূর্ণ Claude-এর — TK শুধু অনুমতি দিয়েছেন।
# এতে অ্যাপের কোনো ফাংশনালিটি/ডিজাইন/ডেটা ছোঁয়া হয় না (git শুধু
# programmer-এর কমিট-ইতিহাস রাখে, অ্যাপ চালাতে/build করতে ব্যবহৃতই হয় না)।
# যদি ভবিষ্যতে কোনো কারণে .git-এর পুরনো কমিট-ইতিহাসের প্রয়োজন পড়ে
# (যেমন কোনো নির্দিষ্ট পুরনো মুহূর্তের সাথে ফাইল-বাই-ফাইল তুলনা), সেটা আর
# সম্ভব হবে না — সেই সীমাবদ্ধতার দায় Claude-এর, TK-কে আগেই স্পষ্ট করে
# জানানো হয়েছিল।
def check_complete():
    for d in ("02_ANDROID_SOURCE_CODE", "03_NETLIFY_READY", "04_SUPABASE_DATABASE_SETUP"):
        if not os.path.isdir(os.path.join(ROOT, d)):
            fail("৪.৫", f"{d} ফোল্ডার নেই")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ১৩ — রোগীর সময় ১১-৪ (সার্কুলার ১১)
# ═══════════════════════════════════════════════════════════════
def check_clinic_time():
    bad = []
    pm = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "native", "PatientMessage.kt")
    for p in (pm, os.path.join(WEB, "app.js")):
        if not os.path.exists(p):
            continue
        s = read(p)
        if "৯টা" in s or "9 am to 6 pm" in s or "९ बजे" in s:
            bad.append(os.path.basename(p))
    if bad:
        fail("১১", "রোগীর বার্তায় ৯টা–৬টা রয়ে গেছে (হওয়ার কথা ১১টা–৪টা) → " + " · ".join(bad))


# ═══════════════════════════════════════════════════════════════
#  যাচাই ১৪ — ফাইলের নাম আগে ব্যবহার হয়নি (সার্কুলার ৪.১)
# ═══════════════════════════════════════════════════════════════
def sent_list():
    if os.path.exists(SENT):
        try:
            return json.load(io.open(SENT, encoding="utf-8"))
        except Exception:
            return []
    return []


def next_name(code):
    used = {r["file"] for r in sent_list()}
    base = f"PILES_CLINIC_APP_V{code}_FINAL"
    if f"{base}.zip" not in used:
        return f"{base}.zip"
    n = 2
    while f"{base}_{n}.zip" in used:
        n += 1
    return f"{base}_{n}.zip"


def record(name, code):
    rows = sent_list()
    rows.insert(0, {
        "file": name,
        "versionCode": code,
        "when": datetime.datetime.now().strftime("%d.%m.%Y %I.%M %p").lower(),
    })
    io.open(SENT, "w", encoding="utf-8").write(json.dumps(rows, ensure_ascii=False, indent=2))


# ═══════════════════════════════════════════════════════════════
#  মানুষকে যা নিজে দেখতে হবে (মেশিন পারে না)
# ═══════════════════════════════════════════════════════════════
HUMAN = [
    ("৭.২", "ডিজাইনে হাত পড়েছে? তাহলে মালিককে ফুল-স্ক্রিন প্রুফ দেখানো হয়েছে ও তিনি পাশ করেছেন?"),
    ("৭.১", "মালিকের অনুমতি ছাড়া কিছু বদলানো/মোছা/যোগ করা হয়নি তো?"),
    ("৮.১", "কোনো ঝুঁকি ছিল? থাকলে কাজ শুরুর আগেই মালিককে জানানো হয়েছে?"),
    ("৬.১", "মালিক এই সেশনে যা যা বলেছেন — সবকটা শেষ হয়েছে?"),
    ("৬.২", "একটা দোষ পেলে পুরো প্রজেক্টে একই ধরনের সব জায়গা খুঁজে ঠিক করা হয়েছে?"),
    ("৭.৫", "আগে লক করা সব ডিজাইন এই ফাইলেও হুবহু আছে?"),
    ("৬.৬", "ফোন ও কম্পিউটার — দুটোতেই কাজটা করা হয়েছে?"),
    ("৪.৪", "সব নোটে তারিখ-সময় সহ লেখা শেষ?"),
    ("২", "পাঁচটা প্রতিশ্রুতি ঠিক আছে? (তথ্য হারাবে না · থমকাবে না · সঙ্গে সঙ্গে দেখাবে · ফ্রি প্ল্যানে চলবে · বোতাম কাজ করবে)"),
]


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩১ — রোগীর পরিচয় হারালে ফাঁকা আইডিতে সেভ হতে পারবে না
#  ─────────────────────────────────────────────────────────────
#  🔴 TK-রিপোর্ট (২৮.০৮.২০২৬, ছবিসহ): Doctor Check-up পর্দার হেডারে
#  নাম "Patient", ID "-", ব্রাঞ্চ "-" — অথচ ঘরে "BLEEDING" লেখা।
#
#  **আসল কারণ:** এই পর্দাগুলো রোগীকে চেনে শুধু মেমরির `RoleSession` থেকে;
#  খোলার Intent-এ রোগীর আইডি পাঠানোই হয় না। ফোনে কল এলে/মেমরি কম পড়লে
#  Android প্রসেস বন্ধ করে দেয়, পর্দা আবার খোলে — টাইপ করা লেখা ফেরে,
#  কিন্তু `RoleSession` ফাঁকা। V721-এর ফোনে-জমা ব্যবস্থার ৩০ মিনিটের সীমা
#  পেরোলে সেটাও ফেরাতে পারে না। তখনও **Save চাপা যেত** — "saved" লেখা
#  উঠত, অথচ সারি যেত ফাঁকা আইডিতে ⇒ ডাক্তারের লেখা চুপচাপ হারাত।
#
#  **সমাধান (V786):** প্রতিটা রোগী-নির্ভর পর্দা (ক) নিজের Bundle-এ পরিচয়
#  রাখে (`RoleSession.saveTo` — Bundle-এ সময়সীমা নেই), (খ) খুলে ফাঁকা
#  পেলে সেখান থেকেই ফেরায় (`restoreFrom`), আর (গ) তবু না পেলে
#  `blockIfNoPatient()` দিয়ে সেভ থামিয়ে দেয়।
#  এই পাহারা নিশ্চিত করে তিনটেই জায়গামতো থাকে।
# ═══════════════════════════════════════════════════════════════
def check_patient_session_survives():
    import re
    rs = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "clinical", "RoleSession.kt")
    if not os.path.exists(rs):
        fail("৯.৩১", "RoleSession.kt খুঁজে পাওয়া গেল না")
        return
    src = read(rs)
    for need, why in (
        ("fun saveTo(", "পর্দার Bundle-এ রোগী জমানোর ব্যবস্থা নেই"),
        ("fun restoreFrom(", "Bundle থেকে রোগী ফেরানোর ব্যবস্থা নেই"),
        ("fun blockIfNoPatient(", "রোগী না চিনলে সেভ থামানোর পাহারা নেই"),
    ):
        if need not in src:
            fail("৯.৩১", "RoleSession-এ `%s` নেই ⇒ %s" % (need.replace("fun ", "").rstrip("("), why))
    # ফাঁকা রোগীর উপর কখনো লেখা যাবে না — মেমরিতে রোগী থাকলে ফেরানো বন্ধ
    if "if (currentPatientId.isNotBlank()) return" not in src:
        fail("৯.৩১", "restoreFrom/restoreIfEmpty-এ 'মেমরিতে রোগী থাকলে কিছু নয়' পাহারা নেই "
                     "⇒ পুরোনো পর্দা থেকে অন্য রোগী ফিরে আসতে পারে")

    # রোগী-নির্ভর প্রতিটা পর্দায় Bundle-এ জমা + ফেরানো — দুটোই থাকতেই হবে
    missing = []
    for f in kt_files():
        base = os.path.basename(f)
        if not base.endswith("Activity.kt"):
            continue
        src2 = read(f)
        # ⚠️ শুধু **পড়া**-র জায়গা ধরা হয়। Dashboard-এর Print টাইল ঘরটা
        #    ইচ্ছে করে **ফাঁকা করে** (`currentPatientId = ""`) — সেটা রোগী
        #    ব্যবহার করা নয়, তাই ওটা এই পাহারার আওতায় নয়।
        reads = [ln for ln in src2.split("\n")
                 if "RoleSession.currentPatientId" in ln
                 and not re.search(r"currentPatientId\s*=[^=]", ln)
                 and not ln.strip().startswith(("//", "*", "/*"))]
        if not reads:
            continue
        if ".saveTo(outState)" not in src2 or ".restoreFrom(savedInstanceState)" not in src2:
            missing.append(base)
    if missing:
        fail("৯.৩১", "এই পর্দাগুলো রোগীর পরিচয় Bundle-এ রাখে/ফেরায় না ⇒ কল এলে "
                     "রোগী হারিয়ে ফাঁকা আইডিতে সেভ হবে: " + ", ".join(sorted(missing)))

    # যে পর্দাগুলো সত্যিই ক্লাউডে সেভ করে, সেখানে সেভ-থামানোর পাহারা লাগবেই
    for base in ("DoctorCheckupActivity.kt", "PrescriptionActivity.kt",
                 "DietChartActivity.kt", "InvestigationAdviceActivity.kt",
                 "MedicineSlipActivity.kt"):
        f = os.path.join(JAVA, "com", "tkbiswas", "pilesclinic", "clinical", base)
        if not os.path.exists(f):
            fail("৯.৩১", base + " খুঁজে পাওয়া গেল না")
            continue
        if "blockIfNoPatient(this)" not in read(f):
            fail("৯.৩১", base + "-এ `blockIfNoPatient()` নেই ⇒ রোগী না চিনেও সেভ "
                                "হয়ে যাবে আর মিথ্যা \"saved\" দেখাবে")


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩২ — XML-এ বসানো বোতামের রং ফোনে চুপচাপ হারাবে না
#  ─────────────────────────────────────────────────────────────
#  🔴 TK-রিপোর্ট (২৮.০৮.২০২৬, Trash Bin-এর ছবিসহ):
#  *"ফটোপ্রুফ দেখিয়েছিলেন কিন্তু সেই অনুসারে ডিজাইন তো এখানে হয় নাই"*
#
#  **আসল কারণ:** অ্যাপের থিম `Theme.MaterialComponents.Light.NoActionBar`।
#  সেই থিমে XML-এর সাদামাটা `<Button>` আপনা-আপনি **MaterialButton** হয়ে যায়,
#  আর MaterialButton `android:background` **অগ্রাহ্য করে** — নিজের
#  `backgroundTint` (থিমের গাঢ় নীল) বসিয়ে দেয়। ⇒ V773-এ Trash-এর নরম রং
#  XML-এ লেখা থাকা সত্ত্বেও ফোনে কখনো দেখা যায়নি; কম্পিউটারে (CSS) দেখা গেছে।
#  একই ভুল ধরা না পড়ায় "হয়ে গেছে" বলা হয়েছিল — সেটাই ছিল অসততা।
#
#  **প্রমাণিত ওষুধ (প্রকল্পেরই নিজের):** `DoctorQueueAdapter` ও
#  `DraftCardAdapter` কোডে `backgroundTintList = null` বসায় — তখন XML-এর
#  drawable-টাই দেখা যায়। Doctor Queue-র চার রঙের বোতাম এভাবেই ঠিক আছে।
#
#  এই পাহারা: XML-এ নিজের রং (`android:background="@drawable/…"` বা `"#…"`)
#  দেওয়া প্রতিটা `<Button>`-এর জন্য কোডে হয় `backgroundTintList` বসাতে হবে,
#  নয়তো XML-এ `backgroundTint` থাকতে হবে। নইলে রংটা ফোনে হারাবে।
#  ⚠️ যেগুলো এখনো সারানো হয়নি সেগুলো `MATERIAL_TINT_KNOWN`-এ লেখা আছে —
#     TK-কে ডিজাইন-প্রুফ দেখিয়ে অনুমতি নেওয়ার পরে সারানো হবে; তালিকা
#     **বাড়ানো যাবে না**, শুধু কমবে।
# ═══════════════════════════════════════════════════════════════
# ✅ V829 (২৯.০৮.২০২৬) — TK ফটো-প্রুফ দেখে অনুমতি দিয়েছেন ("হ্যাঁ করুন, তবে
#    সাবধানে"), আর **তেরোটাই সারানো হয়েছে** (`backgroundTintList = null`)।
#    তাই তালিকাটা এখন ফাঁকা। ⛔ নতুন নাম এখানে যোগ করা যাবে না — বোতামের
#    রং XML-এ দিলে কোডেও tint খালি করতে হবে, নইলে ফোনে রং হারাবে।
MATERIAL_TINT_KNOWN = set()


# ═══════════════════════════════════════════════════════════════
#  যাচাই ৯.৩৩ — "কোডে যা লেখা" আর "ফোনে যা দেখায়" আলাদা হলে জানাবে
#  ─────────────────────────────────────────────────────────────
#  🔴 TK-এর প্রশ্ন (২৮.০৮.২০২৬): *"কতবারই তো এরকম করেন কিন্তু কার্যকরী
#  কেন হয় না"*
#
#  **সৎ উত্তর:** কম্পিউটারের কাজ এখানে সত্যিই **চালিয়ে চোখে দেখা যায়**
#  (ব্রাউজার আছে)। ফোনের অ্যাপ এখানে **চালানো যায় না** — শুধু কোড পড়া যায়।
#  তাই ফোনের বেলায় যাচাই হচ্ছিল *"আমি কি ঠিক জিনিসটা লিখেছি?"*, অথচ আসল
#  প্রশ্ন *"ফোন কি সেটা দেখাবে?"* — দুটো এক নয়। Android যেখানে নিজে থেকে
#  লেখাটা বদলে দেয় (যেমন MaterialButton), সেখানেই "হয়ে গেছে" বলা হয়ে
#  যাচ্ছিল, অথচ হয়নি। এটাই বারবার হওয়ার আসল কারণ।
#
#  **এই পাহারা সেই ফাঁকটাই বন্ধ করে** — XML যা বলে আর ফোন যা দেখাবে, তার
#  মধ্যে জানা প্রতিটা অমিল এখানে ধরা পড়ে, তাই আর "চুপচাপ" থাকে না।
#
#  এখনকার জানা অমিল: `<Button>`-এর লেখা ছোট হাতে লেখা থাকলেও Material
#  থিমে ফোন সেটা **বড় হাতে** দেখায় (`textAllCaps` ডিফল্ট true) — অথচ
#  কম্পিউটারে ছোট হাতেই থাকে। এটা ভুল নয়, কিন্তু **ফোন ও কম্পিউটার এক নয়**,
#  তাই তালিকাটা চোখের সামনে থাকা দরকার। TK-এর অনুমতি ছাড়া বদলানো হবে না;
#  তালিকা শুধু **ছোট** হতে পারে, বড় নয়।
# ═══════════════════════════════════════════════════════════════
# ✅ V829 (২৯.০৮.২০২৬) — TK-এর অনুমতিতে **৯টা সারানো হয়েছে**
#    (`android:textAllCaps="false"` — ফোনেও এখন কম্পিউটারের মতো ছোট হাতে)।
#    ⚠️ নিচের **৫টা রয়ে গেল** — ওগুলোর XML-এ **নিজে হাতে `textAllCaps="true"`
#    লেখা আছে**, অর্থাৎ বড় হাতে দেখানোটা কেউ ইচ্ছে করে বেছেছিলেন। TK-কে
#    আলাদা করে জিজ্ঞেস না করে সেই স্পষ্ট নির্দেশ উল্টে দেওয়া হয়নি।
ALLCAPS_KNOWN = {
    ("activity_registration.xml", "btnSexMale"),
    ("activity_registration.xml", "btnSexFemale"),
    ("activity_registration.xml", "btnSexOther"),
    ("activity_registration.xml", "btnRegTimingOfficial"),
    ("activity_registration.xml", "btnRegTimingUnexpected"),
}


def check_phone_shows_what_code_says():
    import re
    lay = os.path.join(RES, "layout")
    if not os.path.isdir(lay):
        return
    kt = ""
    for f in kt_files():
        kt += read(f)
    new, healed = [], []
    for fn in sorted(os.listdir(lay)):
        if not fn.endswith(".xml"):
            continue
        src = read(os.path.join(lay, fn))
        for m in re.finditer(r"<Button\b(.*?)/>", src, re.S):
            blk = m.group(1)
            tm = re.search(r'android:text="([^"]*)"', blk)
            if not tm:
                continue
            letters = [c for c in tm.group(1) if c.isalpha()]
            if not letters or not any(c.islower() for c in letters):
                continue
            idm = re.search(r'android:id="@\+id/(\w+)"', blk)
            bid = idm.group(1) if idm else "?"
            key = (fn, bid)
            off = ('textAllCaps="false"' in blk
                   or re.search(r"\b" + re.escape(bid) + r"\.isAllCaps", kt) is not None)
            if off:
                if key in ALLCAPS_KNOWN:
                    healed.append(fn + " · " + bid)
                continue
            if key in ALLCAPS_KNOWN:
                continue
            new.append(fn + " · " + bid)
    if new:
        fail("৯.৩৩", "এই বোতামের লেখা কোডে ছোট হাতে, কিন্তু ফোনে বড় হাতে দেখাবে "
                     "(কম্পিউটারের সঙ্গে মিলবে না)। ইচ্ছাকৃত হলে `ALLCAPS_KNOWN`-এ "
                     "লিখুন, নইলে `isAllCaps = false` বসান: " + ", ".join(new[:10]))
    if healed:
        fail("৯.৩৩", "এগুলো ঠিক হয়ে গেছে — `ALLCAPS_KNOWN` থেকে নামগুলো তুলে দিন: "
                     + ", ".join(healed[:10]))


def check_material_button_background():
    import re
    lay = os.path.join(RES, "layout")
    if not os.path.isdir(lay):
        fail("৯.৩২", "res/layout ফোল্ডার খুঁজে পাওয়া গেল না")
        return
    kt = ""
    for f in kt_files():
        kt += read(f)
    bad, healed = [], []
    for fn in sorted(os.listdir(lay)):
        if not fn.endswith(".xml"):
            continue
        src = read(os.path.join(lay, fn))
        for m in re.finditer(r"<Button\b(.*?)/>", src, re.S):
            blk = m.group(1)
            if not ('android:background="@drawable/' in blk or 'android:background="#' in blk):
                continue
            if "backgroundTint" in blk:
                continue
            idm = re.search(r'android:id="@\+id/(\w+)"', blk)
            if not idm:
                continue
            bid = idm.group(1)
            fixed = re.search(r"\b" + re.escape(bid) + r"\.backgroundTintList", kt) is not None
            key = (fn, bid)
            if fixed:
                if key in MATERIAL_TINT_KNOWN:
                    healed.append(fn + " · " + bid)
                continue
            if key in MATERIAL_TINT_KNOWN:
                continue
            bad.append(fn + " · " + bid)
    if bad:
        fail("৯.৩২", "এই বোতামগুলোর XML-এ নিজের রং আছে, কিন্তু কোডে "
                     "`backgroundTintList = null` নেই ⇒ MaterialButton রংটা "
                     "চুপচাপ ফেলে দেবে, ফোনে ডিজাইন দেখা যাবে না: "
                     + ", ".join(bad[:10])
                     + (" …আরও " + str(len(bad) - 10) if len(bad) > 10 else ""))
    if healed:
        fail("৯.৩২", "এগুলো সারানো হয়ে গেছে — `MATERIAL_TINT_KNOWN` তালিকা থেকে "
                     "নামগুলো তুলে দিন (তালিকা শুধু ছোট হবে): " + ", ".join(healed[:10]))


def main():
    release = "--release" in sys.argv
    print("=" * 66)
    print("🛡️  TK-এর পাহারাদার — নিয়ম মিলিয়ে দেখা হচ্ছে")
    print("=" * 66)

    nkt = check_brackets()
    check_comment_swallow()
    check_if_else()
    check_bindings()
    check_drawables()
    nxml = check_xml()
    check_companion()
    check_columns()
    check_supabase_auth_header()    # 🔑 V811 — দুটো হেডারই আছে তো
    check_project_class_imports()   # 🕵️ V807 — ইন্সপেক্টর: import ছাড়া প্রকল্পের ক্লাস
    check_r_import()                # 🅰️ V855 — R-এর import (TK-এর বিল্ড-এরর ৩০.০৮.২০২৬)
    check_owner_preserved()         # 🧑‍💼 V868 — আসল রেজিস্ট্রারের নাম কখনো বদলাবে না
    check_kotlin_balance()          # 🧱 V877 — ব্রেস/বন্ধনী মেলে (বিল্ড-এরর ঠেকানো)
    check_history_time()            # ⏰ V888 — history-র প্রতিটা সারিতে তারিখ ও সময়
    check_web_flag_fields()         # 🔤 V906 — হ্যাঁ/না-এর ঘর wlv1Flag() ছাড়া পড়া নিষেধ
    check_http_call_timeout()   # ⏱️ V803 — প্রতিটা নেট-ডাকে সময়সীমা
    check_safe_wide_columns()   # 🛟 V801 — শেষ-ভরসার কলাম-তালিকা পুরনো হয়নি তো
    check_static_calls()
    check_unresolved_imports()   # 🔴🔴🔴🔴 TK-নির্দেশ (20.08.2026) — Unresolved reference কখনো ফাইল পাঠাতে দেবে না
    check_qualified_extension_fn()   # 🔴🔴🔴 খাতার সারি B203 — async/launch fully-qualified কল
    check_fake_layoutparams_class()   # 🔴🔴🔴 খাতার সারি B266-সংশোধন — ScrollView-এর নিজের LayoutParams নেই
    check_bare_number_input()         # 🔴🔴🔴 খাতার সারি B411 — একা TYPE_CLASS_NUMBER-এ কীবোর্ড না খোলার বাগ
    check_followup_cache_fields()     # 🏷️ V712 — জমানো তালিকায় কার্ডের ট্যাগের ঘর বাদ পড়েনি তো
    check_no_wide_photo_reads()       # 📉 V715 — ছবিওয়ালা টেবিলে select=* তালিকা পড়া নিষেধ
    check_digits()
    check_doctor_message_twin()
    check_draft_cache_fields()        # 💰 V741 — Draft-এর জমানো তালিকায় টাকার ঘর
    check_cloud_login_name_is_code()  # 👥 V748 — মেঘের লোকের name ঘরে কোড
    check_web_cache_busters()         # 🌐 V750 — ওয়েব ফাইল বদলে cache-নম্বর
    check_no_autofill_kept()          # ⌨️ V752 — ফোনের নিজের সাজেশন বন্ধ
    check_clip_sensitive()            # 📋 V772 — কপি করা নম্বর সাজেশনে উঠবে না
    check_dialog_suggestion_guard()   # ⌨️ V774 — পপ-আপেও সাজেশন বন্ধ
    check_patient_session_survives()  # 👤 V786 — কল এলে রোগীর পরিচয় হারাবে না
    check_material_button_background()  # 🎨 V790 — XML-এর বোতামের রং ফোনে হারাবে না
    check_phone_shows_what_code_says()  # 📱 V791 — কোডে যা লেখা, ফোনে তাই দেখায় তো?
    check_qualified_calls()           # 🎯 V769 — ভুল object-এর নামে ডাকা
    check_cloud_row_null_text()       # 🚫 V760 — পর্দায় "null" লেখা
    check_webview_popup()             # 🩹 V738 — পপ-আপে WebView বসানোর ফাঁদ (কম্পন)
    check_locked_rules()
    check_hidden_spinner()
    check_work_rules()          # 🧾 খাতার সারি B147 — কাজের নিয়ম
    check_followup_id()         # 🧾 খাতার সারি B147
    check_branch_encoded()      # 🧾 খাতার সারি B147
    check_no_bengali()          # 🚫 খাতার সারি B158
    check_no_date_emoji()       # 📅 TK ০৫.০৯.২০২৬ — "July 17 Emoji থাকবে না"
    check_no_native_in_signature()   # 🔴 TK-এর Studio-তে V1119 বিল্ড ভাঙা (০৫.০৯.২০২৬)
    check_strict_undeclared()        # 🔴 V1225 — Check-up বোতাম মরা হয়ে যাওয়া
    code = check_version()
    check_web()
    check_notes()
    check_complete()
    check_clinic_time()

    rows = [
        ("৯.১", f"ব্র্যাকেট (Kotlin-সচেতন) — {nkt} ফাইল"),
        ("৯.২", "কমেন্ট কোড গিলে ফেলেনি"),
        ("৯.৪", "`if` expression-এ `else` আছে"),
        ("৯.৫", "binding.<view> ও drawable সব আছে"),
        ("৯.৬", f"XML ঠিক আছে — {nxml} ফাইল"),
        ("৯.৭", "Supabase কলাম মেলে"),
        ("৯.১০", "ক্লাসের নামে instance-ফাংশন ডাকা হয়নি"),
        ("৯.১৮", "সব ক্লাস-ব্যবহারের import ঠিক আছে (Unresolved reference নেই)"),
        ("৯.১৫", "kotlinx.coroutines async/launch প্যাকেজ-নাম দিয়ে ডাকা হয়নি"),
        ("৯.১৬", "ScrollView/HorizontalScrollView-এর ভুয়া .LayoutParams ডাকা হয়নি"),
        ("৯.১৭", "সংখ্যা-ঘরে একা TYPE_CLASS_NUMBER (কীবোর্ড না-খোলার ঝুঁকি) নেই"),
        ("৯.১৯", "Follow-up কার্ডের ট্যাগ জমানো তালিকাতেও লেখা হয় (Unexpected · RMP · ঠিকানা)"),
        ("৯.২০", "ছবিওয়ালা টেবিলে (followups/patients/medical) select=* তালিকা পড়া নেই"),
        ("৯.২২", "পপ-আপে সরাসরি WebView বসানো নেই (কম্পনের ফাঁদ) + steadyWebView-এর খুঁটি অক্ষত"),
        ("৯.২৩", "Draft-এর জমানো তালিকায় DraftEntry-র একটাও ঘর বাদ পড়েনি"),
        ("৯.১১", "সংখ্যা সবসময় ইংরেজিতে"),
        ("৯.১২", f"🚔 লক করা নিয়ম অক্ষত — {len(LOCKED_RULES)}টি + লুকানো Spinner-এর ফাঁদ"),
        ("৯.১৩", f"🧾 কাজের নিয়ম অক্ষত — {len(WORK_RULES)}টি + followups-এর আইডি + ব্রাঞ্চের encode"),
        ("৯.১৪", "🚫 বাংলা-বন্ধ স্টাফের পর্দায় একটাও বাংলা নেই (কোড · XML · Toast · পপ-আপ)"),
        ("৯.৮", f"ভার্সন এক — V{code}"),
        ("৯.৯", "কম্পিউটারের অ্যাপ ঠিক আছে"),
        ("৪.৫", "সম্পূর্ণ প্রজেক্ট (মূল ফোল্ডার সব আছে)"),
        ("৪.৬", "সব বাধ্যতামূলক নোট আছে"),
        ("১১",  "রোগীর সময় ১১টা–৪টা"),
        ("৯.৩৭", "🔑 Supabase-এর প্রতিটা ডাকে apikey + Authorization দুটোই আছে"),
        ("৯.৩৬", "🕵️ ইন্সপেক্টর — প্রকল্পের প্রতিটা ক্লাসের import আছে"),
        ("৯.৪৫", "🚦 strict ফাইলে ঘোষণা ছাড়া কোনো ঘর বসানো নেই"),
        ("৯.৪২", "🔤 হ্যাঁ/না-এর ঘর সবসময় wlv1Flag() দিয়েই পড়া হয়"),
        ("৯.৪১", "⏰ history-র প্রতিটা সারিতে তারিখ ও সময় দুটোই বসে"),
        ("৯.৪০", "🧱 প্রতিটা Kotlin ফাইলে ব্রেস ও বন্ধনী মেলে"),
        ("৯.৩৯", "🧑\u200d💼 আসল রেজিস্ট্রারের নাম ও সময় কখনো বদলায় না"),
        ("৯.৩৮", "🅰️ `R.` ব্যবহারকারী প্রতিটা ফাইলে R-এর import আছে"),
        ("৯.৩৫", "⏱️ প্রতিটা OkHttpClient-এ callTimeout বসানো আছে"),
        ("৯.৩৪", "🛟 SafeWideColumns (শেষ-ভরসার পড়া) ডেটাবেসের সঙ্গে মেলে"),
        ("১০",  "মাইন-পোঁতা জায়গা অক্ষত"),
    ]
    broken_rules = {r for r, _ in problems}
    for rule, label in rows:
        print(f"  {'❌' if rule in broken_rules else '✅'} [{rule}] {label}")

    if problems:
        print("\n" + "─" * 66)
        print("❌ যা ঠিক করতে হবে:")
        for rule, what in problems:
            print(f"   [{rule}] {what}")
    if notices:
        print("\n" + "─" * 66)
        print("⚠️  খেয়াল রাখুন:")
        for rule, what in notices:
            print(f"   [{rule}] {what}")

    print("\n" + "─" * 66)
    print("👁️  মেশিন যা যাচাই করতে পারে না — নিজে দেখে নিন:")
    for rule, q in HUMAN:
        print(f"   [ ] [{rule}] {q}")

    print("=" * 66)
    if problems:
        print("⛔ ফাইল বানানো যাবে না — উপরের সমস্যাগুলো আগে ঠিক করুন।")
        sys.exit(1)

    print("✅ মেশিনের সব যাচাই পাশ।")
    if release and code:
        name = next_name(code)
        print(f"\n📦 এই নামে ফাইল বানান:  {name}")
        prev = sent_list()
        if prev:
            print(f"   (আগে পাঠানো হয়েছে: {prev[0]['file']} — {prev[0]['when']})")
        record(name, code)
        print("   ✅ নামটা তালিকায় লিখে রাখা হলো — এই নাম আর কখনো ব্যবহার হবে না।")
    else:
        print("   ফাইল বানাতে চাইলে চালান:  python3 00_GUARD/tk_guard.py --release")
    print("=" * 66)


if __name__ == "__main__":
    main()
