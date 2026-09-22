#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛡️⚡ V497 (২১.০৮.২০২৬) — **দ্রুত পাহারা: Kotlin-এ যা করাই যায় না।**

`verify_kotlin_compile.py` আসল কম্পাইলার চালায় — সেটাই প্রধান পাহারা।
কিন্তু কম্পাইলার নামাতে ~৯১ MB লাগে ও কয়েক মিনিট সময় নেয়। তাই যে
ভুলগুলো TK-এর বিল্ড ভেঙেছিল, সেগুলো **এক সেকেন্ডেই** ধরার জন্য এই ছোট
পাহারাদার — কম্পাইলার ছাড়াই চলে।

⛔ এটা কম্পাইলারের **বিকল্প নয়**, বাড়তি একটা জাল মাত্র।

─── কী কী ধরে ─────────────────────────────────────────────────────────────
১. **enum ক্লাসকে চলকে রাখা** (TK-এর বিল্ড এখানেই ভেঙেছিল):
       val G = BiometricGate.Reason          ⛔ Kotlin-এ অসম্ভব
       val S = AttendanceRepository.Status   ⛔ একই ভুল
   ⇒ "Classifier 'Reason' does not have a companion object"

২. **নেই এমন enum ধ্রুবক** ব্যবহার:
       BiometricGate.Reason.FAILEDD          ⛔ বানান ভুল

৩. **`<include>` টাইলে `.root` ভুলে যাওয়া** (V497-এ TK-এর দ্বিতীয় বিল্ড
   এখানেই ভেঙেছিল):
       binding.tileWorkNotebook.visibility = …       ⛔ এটা View নয়
       binding.tileWorkNotebook.root.visibility = …  ✅ ঠিক
   layout XML-এ `<include>` থাকলে ViewBinding ওখানে **আরেকটা binding** দেয়,
   View নয়। তাই `.root` ছাড়া কিছু ছোঁয়া যায় না।
   ⇒ "Unresolved reference: visibility"

`when`-এ সব অবস্থা আছে কিনা — সেটা এখানে দেখা হয় না (কম্পাইলার ছাড়া
নির্ভুল নয়); সেটা `verify_kotlin_compile.py` ধরে।

সব যাচাই প্রকল্পের নিজের ফাইল পড়েই হয় — কোনো অনুমান নেই।
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app",
                   "src", "main", "java")

problems, notes = [], []


def strip_kt(text):
    """মন্তব্য ও লেখার টুকরো বাদ — নইলে ব্যাখ্যাকেই কোড ভেবে ভুল ধরা পড়ে।"""
    text = re.sub(r"/\*[\s\S]*?\*/", "", text)
    text = re.sub(r"//[^\n]*", "", text)
    text = re.sub(r'"""[\s\S]*?"""', '""', text)
    text = re.sub(r'"(\\.|[^"\\\n])*"', '""', text)
    return text


def kt_files():
    for folder, _d, files in os.walk(SRC):
        for f in sorted(files):
            if f.endswith(".kt"):
                yield os.path.join(folder, f)


# ── ধাপ ১ · প্রকল্পের সব enum ও তাদের ধ্রুবক খুঁজে বের করা ──────────────────
# {enum-এর নাম: set(ধ্রুবক)}   যেমন {"Reason": {"SUCCESS","NO_HARDWARE",…}}
#
# ⚠️ একই নামের enum একাধিক জায়গায় থাকতে পারে — যেমন `BiometricGate.Reason`
#    আর `ClinicPresence.Reason`। কম্পাইলার ছাড়া কোনটা কোথায় ব্যবহার হচ্ছে
#    নিশ্চিত করে বলা যায় না। তাই এমন নামের ক্ষেত্রে ধ্রুবকের বানান-যাচাই
#    **করা হয় না** (মিথ্যা সতর্কতা দেওয়ার চেয়ে চুপ থাকা ভালো) — ওটা
#    `verify_kotlin_compile.py`-ই ধরবে।
enums = {}
declared_in = {}
for path in kt_files():
    code = strip_kt(open(path, encoding="utf-8").read())
    for m in re.finditer(r"\benum\s+class\s+(\w+)\s*(?:\([^)]*\))?\s*\{", code):
        name = m.group(1)
        # `{` থেকে মিলিয়ে-যাওয়া `}` পর্যন্ত শরীরটা কেটে নেওয়া
        i, depth = m.end() - 1, 0
        while i < len(code):
            if code[i] == "{":
                depth += 1
            elif code[i] == "}":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        body = code[m.end():i]
        head = re.split(r";", body)[0]          # ধ্রুবকগুলো প্রথম `;`-এর আগে
        consts = set(re.findall(r"\b([A-Z][A-Z0-9_]{1,40})\b", head))
        if consts:
            enums.setdefault(name, set()).update(consts)
            declared_in.setdefault(name, set()).add(os.path.basename(path))

if not enums:
    problems.append("প্রকল্পে একটাও enum পাওয়া গেল না — পাহারাদার ঠিকমতো পড়তে পারেনি")
else:
    notes.append("  প্রকল্পের %d টা enum চেনা গেছে (%d টা ধ্রুবক) ✅"
                 % (len(enums), sum(len(v) for v in enums.values())))

enum_names = set(enums)

# ── ধাপ ২ · তিন রকম ভুল খোঁজা ────────────────────────────────────────────────
for path in kt_files():
    short = os.path.basename(path)
    raw = open(path, encoding="utf-8").read()
    code = strip_kt(raw)

    # ভুল ১ — enum ক্লাসটাকেই চলকে রাখা:  val G = কিছু.Reason   (পরে `.` নেই)
    for m in re.finditer(r"\b(?:val|var)\s+(\w+)\s*=\s*"
                         r"((?:[A-Za-z_]\w*\.)*)([A-Z]\w*)\s*(?![\w.\(])", code):
        cls = m.group(3)
        if cls in enum_names:
            problems.append(
                "%s — `val %s = ….%s` : Kotlin-এ enum **ক্লাসটাকে** চলকে রাখা যায় না। "
                "সরাসরি `%s.%s` লিখুন।"
                % (short, m.group(1), cls, cls, sorted(enums[cls])[0]))

    # ভুল ২ — নেই এমন ধ্রুবক:  Reason.FAILEDD
    #
    # 🟢🔒 V589 (২৩.০৮.২০২৬) — **পাহারাদারের নিজের একটা ফাঁক সারানো হলো।**
    # `android.graphics.PorterDuff.Mode.DST_IN` লেখা থাকলে এই খোঁজাটা শুধু
    # `Mode.DST_IN` টুকু দেখত, আর প্রজেক্টের নিজের `Mode` নামের enum-এর সাথে
    # মিলিয়ে মিথ্যে "এই নামের কিছু নেই" বলত (kotlinc কিন্তু দিব্যি কম্পাইল করে)।
    # ⛔ নিয়ম **এক চুলও আলগা হয়নি**: শুধু তখনই ছেড়ে দেওয়া হয় যখন নামটার আগে
    #    **ছোট হাতের অক্ষরে শুরু হওয়া প্যাকেজ** আছে (`android.` · `java.` …) —
    #    অর্থাৎ ওটা বাইরের লাইব্রেরির নাম, প্রজেক্টের enum নয়।
    #    `BiometricGate.Reason.FAILEDD`-এর মতো প্রজেক্টের ভিতরের ভুল আগের
    #    মতোই ধরা পড়ে (সব অংশই বড় হাতের অক্ষরে শুরু)।
    def _is_library_qualified(text, at):
        """`at` অবস্থানের ঠিক আগে ছোট-হাতের প্যাকেজ-নাম আছে কি না।"""
        j = at
        while j > 0 and text[j - 1] == '.':
            k = j - 1
            while k > 0 and (text[k - 1].isalnum() or text[k - 1] == '_'):
                k -= 1
            seg = text[k:j - 1]
            if not seg:
                return False
            if seg[0].islower():
                return True
            j = k
        return False

    for m in re.finditer(r"\b([A-Z]\w*)\.([A-Z][A-Z0-9_]{1,40})\b", code):
        cls, const = m.group(1), m.group(2)
        if _is_library_qualified(code, m.start()):
            continue
        if (cls in enums and const not in enums[cls]
                and len(declared_in.get(cls, ())) == 1):
            problems.append("%s — `%s.%s` : `%s`-এ এই নামের কিছু নেই।"
                            % (short, cls, const, cls))

    # ⛔ `when`-এ সব অবস্থা আছে কিনা — এখানে যাচাই করা হয় **না**।
    #    কম্পাইলার ছাড়া `when (x)`-এর `x` কোন enum তা নিশ্চিত জানা যায় না,
    #    ফলে মিথ্যা সতর্কতা আসত। ওটা `verify_kotlin_compile.py` নিখুঁতভাবে ধরে
    #    ("'when' expression must be exhaustive") — পরীক্ষায় প্রমাণিত।


# ── ধাপ ৩ · `<include>` টাইলে `.root` লাগে কিনা ─────────────────────────────
#  ViewBinding-এর নিয়ম: layout-এ `<include android:id="@+id/x" layout="@layout/y">`
#  থাকলে `binding.x` একটা **YBinding**, View নয়। তাই `binding.x`-এর পরে
#  শুধু `.root` অথবা `y` layout-এর ভিতরের কোনো id-ই বসতে পারে।
LAYOUT_DIR = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp",
                          "app", "src", "main", "res", "layout")


def camel(snake):
    bits = snake.split("_")
    return bits[0] + "".join(b[:1].upper() + b[1:] for b in bits[1:])


layout_ids = {}          # layout ফাইলের নাম -> ভিতরের সব id (camelCase)
includes = {}            # include-এর id (camelCase) -> কোন layout
if os.path.isdir(LAYOUT_DIR):
    for f in sorted(os.listdir(LAYOUT_DIR)):
        if not f.endswith(".xml"):
            continue
        xml = open(os.path.join(LAYOUT_DIR, f), encoding="utf-8").read()
        ids = set(camel(i) for i in re.findall(r'android:id="@\+?id/(\w+)"', xml))
        layout_ids[f[:-4]] = ids
        for m in re.finditer(r"<include\b[^>]*>", xml):
            tag = m.group(0)
            i = re.search(r'android:id="@\+?id/(\w+)"', tag)
            l = re.search(r'layout="@layout/(\w+)"', tag)
            if i and l:
                includes[camel(i.group(1))] = l.group(1)

if includes:
    notes.append("  layout-এ %d টা `<include>` টাইল চেনা গেছে ✅" % len(includes))
    for path in kt_files():
        short = os.path.basename(path)
        code = strip_kt(open(path, encoding="utf-8").read())
        for m in re.finditer(r"\bbinding\.(\w+)\.(\w+)", code):
            fid, member = m.group(1), m.group(2)
            if fid not in includes:
                continue
            if member == "root":
                continue
            inner = layout_ids.get(includes[fid], set())
            if member not in inner:
                problems.append(
                    "%s — `binding.%s.%s` : `%s` layout-এ `<include>`, তাই এটা View নয়। "
                    "`binding.%s.root.%s` লিখুন।"
                    % (short, fid, member, fid, fid, member))

# ⛔ লেখার ভিতরে `$নাম` — এখানে যাচাই করা হয় **না**।
#    চেষ্টা করা হয়েছিল, কিন্তু বহু জায়গায় `$packageName` · `$it` · loop-এর
#    চলক ইত্যাদি বৈধভাবেই থাকে; কম্পাইলার ছাড়া "ওই নামে কিছু আছে কিনা"
#    নিশ্চিত জানা যায় না, ফলে ১৬টা **মিথ্যা সতর্কতা** আসছিল।
#    ⇒ এটা `verify_kotlin_compile.py` নিখুঁতভাবে ধরে — NoBengali.kt-এর
#      `$distance` ভুলটা সেখানেই ধরা পড়েছে (পরীক্ষায় প্রমাণিত)।

# ── ফল ──────────────────────────────────────────────────────────────────────
print("⚡ Kotlin-এর অসম্ভব লেখা (দ্রুত পাহারা)")
print("=" * 64)
for n in notes:
    print(n)
print()
if problems:
    seen, uniq = set(), []
    for p in problems:
        if p not in seen:
            seen.add(p)
            uniq.append(p)
    print("❌ FAIL — %d টি সমস্যা (এই কোড বিল্ড হবে না):" % len(uniq))
    for p in uniq[:40]:
        print("  ❌ " + p)
    if len(uniq) > 40:
        print("  … আরও %d টা" % (len(uniq) - 40))
    print()
    print("ফল: FAIL — TK-কে এই ফাইল পাঠানো যাবে না। ⛔ PASS নয়।")
    sys.exit(1)

print("PASS — enum নিয়ে অসম্ভব কোনো লেখা কোথাও নেই ✅")
print("⚠️ এটা ছোট জাল; আসল প্রমাণ `verify_kotlin_compile.py` ও Android Studio-র বিল্ড।")
sys.exit(0)
