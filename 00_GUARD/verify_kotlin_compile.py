#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛡️🔴 V497 (২১.০৮.২০২৬) — **আসল Kotlin কম্পাইলার দিয়ে পাহারা।**

─── কেন এটা বানাতে হলো (TK-এর অভিযোগ, ২১.০৮.২০২৬ সকাল ৯:৩৬) ───────────────
V496-এর ZIP পাঠানোর পরে TK Android Studio-তে Build দিলে **১৮টা ভুল** এসে
বিল্ড ভেঙে যায়:

    Classifier 'Reason' does not have a companion object…
    Comparison of incompatible enums 'BiometricGate.Reason' and …
    'when' expression must be exhaustive, add necessary 'SAVED'…

কারণ: `WorkNotebookActivity.kt`-এ লেখা হয়েছিল

    val G = BiometricGate.Reason        // ⛔ Kotlin-এ enum **ক্লাসটাকে**
    val canRetry = r == G.FAILED        //    চলকে রাখা যায় না

পুরনো পাহারাদারগুলো শুধু **লেখা খুঁজত** (regex), কোড **কম্পাইল করত না**।
তাই এই ধরনের ভুল ধরা পড়ার কোনো উপায়ই ছিল না।

TK-এর নির্দেশ: *"পাহারাদার কে আরো শক্ত করবেন, যাতে ভবিষ্যতে এই ধরনের
ভুল ভ্রান্তি ফাইল আমার কাছে না আসে।"*

⇒ এই পাহারাদার **সত্যিকারের `kotlinc` চালায়** — অনুমান নয়।

─── ⚠️ সীমাবদ্ধতা (সৎভাবে, লুকানো হয়নি) ───────────────────────────────────
এই পরিবেশে **Android SDK ও androidx লাইব্রেরি নামানো যায় না**
(`dl.google.com` ও `maven` বন্ধ)। তাই `android.*` · `androidx.*` ·
`org.json` · `kotlinx.*` — এগুলো কম্পাইলার চেনে না, আর সেজন্য হাজার হাজার
"unresolved reference" ভুল দেখায়। সেগুলো **আসল ভুল নয়**, শুধু গোলমাল।

─── 🔴 V497-এ যে ফাঁক ধরা পড়ল ও বন্ধ করা হলো ──────────────────────────────
প্রথম চেষ্টায় **সব ভুলকেই** বেসলাইনে রেখে দেওয়া হয়েছিল। কিন্তু সেই
বেসলাইন বানানো হয়েছিল এমন অবস্থা থেকে **যেটা নিজেই বিল্ড হত না** —
ফলে আসল দুটো ভুলও "জানা গোলমাল" হয়ে চাপা পড়ে গিয়েছিল:

    NoBengali.kt: unresolved reference: distance     ← `"…$distance…"`
    NoBengali.kt: function invocation 'until(...)'   ← `"…$until…"`

TK-এর দ্বিতীয় বিল্ডে ঠিক এগুলোই বেরোয়। তাই এখন **দুই ধাপ**:

  ধাপ ১ — **বাছাই**: প্রতিটা ভুল "বাইরের লাইব্রেরি নেই বলে" নাকি "আসল"।
     বাইরের বলে ধরা হয় শুধু তখনই, যখন —
       • না-চেনা নামটা কোনো `import android/androidx/org.json/…`-এর, বা
       • নামটা প্রকল্পে অন্য কোথাও `.নাম` হিসেবে ব্যবহার হয় (অর্থাৎ
         কোনো ক্লাসের সদস্য, শুধু ক্লাসটাই চেনা যাচ্ছে না), বা
       • বার্তাটা স্পষ্টতই ধাক্কার ফল ("overrides nothing", "type mismatch",
         "cannot infer a type" ইত্যাদি)।
     ⇒ এতে ২৮,৬০০ ভুল থেকে **~২৯০**-এ নামে।

  ধাপ ২ — সেই ~২৯০টাই বেসলাইনে (`kotlin_noise_baseline.txt`), এবং
     **নতুন একটাও এলে FAIL**।

✅ প্রমাণ: `$distance`-এর ভুলটা এই বাছাইয়ে **আসল** হিসেবে ধরা পড়ে।

✅ প্রমাণ: ইচ্ছে করে উপরের বাগটা আবার বসিয়ে চালানো হয়েছিল —
   বেসলাইনের বাইরে ঠিক **২টা নতুন ভুল** ধরা পড়ে, একটাও মিথ্যা সতর্কতা নয়।

⛔ এটা Android Studio-র বিল্ডের **বিকল্প নয়** — resource (R.java), manifest,
   dexing, ProGuard কিছুই এখানে যাচাই হয় না। কিন্তু Kotlin কোডের ভুল আর
   TK-এর কাছে পৌঁছাবে না।
⛔ যাচাই করা না গেলে (কম্পাইলার নেই / নামানো গেল না) এটা **কখনো PASS বলে না** —
   "SKIPPED (যাচাই করা যায়নি)" বলে এবং exit code 1 দেয়।

─── চালানোর নিয়ম ──────────────────────────────────────────────────────────
    python3 00_GUARD/verify_kotlin_compile.py                # যাচাই
    python3 00_GUARD/verify_kotlin_compile.py --update-baseline
                                                            # নতুন বেসলাইন
"""
import os
import re
import shutil
import subprocess
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app",
                   "src", "main", "java")
BASELINE = os.path.join(ROOT, "00_GUARD", "kotlin_noise_baseline.txt")

KOTLIN_VERSION = "1.9.24"
KOTLIN_URL = ("https://github.com/JetBrains/kotlin/releases/download/"
              "v%s/kotlin-compiler-%s.zip" % (KOTLIN_VERSION, KOTLIN_VERSION))

# কম্পাইলার কোথায় খুঁজব (ক্রম অনুসারে)
CANDIDATES = [
    os.environ.get("KOTLINC", ""),
    "/tmp/kotlinc_dist/kotlinc/bin/kotlinc",
    os.path.join(ROOT, "00_GUARD", ".kotlinc", "kotlinc", "bin", "kotlinc"),
    shutil.which("kotlinc") or "",
]


def find_kotlinc(allow_download=True):
    for c in CANDIDATES:
        if c and os.path.exists(c):
            return c
    if not allow_download:
        return None
    # নেই — একবার নামানোর চেষ্টা (ইন্টারনেট না থাকলে চুপচাপ ব্যর্থ)
    dest_dir = os.path.join(ROOT, "00_GUARD", ".kotlinc")
    zip_path = os.path.join(dest_dir, "kotlin-compiler.zip")
    try:
        os.makedirs(dest_dir, exist_ok=True)
        print("   kotlinc পাওয়া যায়নি — নামানোর চেষ্টা করছি (~৯১ MB)…")
        urllib.request.urlretrieve(KOTLIN_URL, zip_path)
        import zipfile
        with zipfile.ZipFile(zip_path) as z:
            z.extractall(dest_dir)
        os.remove(zip_path)
        exe = os.path.join(dest_dir, "kotlinc", "bin", "kotlinc")
        if os.path.exists(exe):
            os.chmod(exe, 0o755)
            return exe
    except Exception as e:
        print("   নামানো গেল না: %s" % e)
    return None


# ══════════════════════════════════════════════════════════════════════
# 🔴🔴 V597 — **আসল android.jar দিয়ে টাইপ-যাচাই** (TK-এর বিল্ড ভাঙা, ২৩.০৮.২০২৬)
# ----------------------------------------------------------------------
# কী হয়েছিল: TK V596-এর ফাইল Android Studio-তে বিল্ড দিলে ১২টা ভুল এসে
# বিল্ড ভেঙে যায় —
#     AnatomyView.kt: Type mismatch: inferred type is Double but Float was expected
# কারণ `ksHealLump()`-এ `val r` Double হয়ে যাচ্ছিল, অথচ Canvas-এর ঘরগুলো
# Float চায়।
#
# 🔴 এই পাহারাদার সেটা ধরেনি কেন — সৎ উত্তর:
#   (ক) ক্লাসপাথে **android.jar ছিল না**, তাই `Canvas`/`RectF` কম্পাইলার
#       চিনতই না — টাইপ মিলছে কি না দেখার প্রশ্নই ওঠেনি।
#   (খ) উপরের `CASCADE` তালিকায় **"type mismatch"** লেখা ছিল, অর্থাৎ ওই
#       বার্তাকে ধরেই নেওয়া হত "লাইব্রেরি নেই বলে গোলমাল"। ঠিক এই
#       বার্তাটাই TK-র বিল্ড ভেঙেছে।
#
# ⇒ সমাধান (দুটোই):
#   ১) compileSdk 34-এর সমান একটা আসল `android.jar` ক্লাসপাথে দেওয়া হয়
#      (Maven Central-এর robolectric `android-all` — Google-এর সাইট এই
#      পরিবেশ থেকে বন্ধ, Maven Central খোলা)।
#   ২) নতুন নিয়ম `real_type_errors()`: **যে ফাইলে একটাও
#      "unresolved reference" নেই, অথচ অন্য ভুল আছে** — সেটা ধাক্কার ফল
#      হতেই পারে না, ওটা আসল ভুল ⇒ সরাসরি FAIL।
#      (AnatomyView `android.view.View` থেকে আসে, androidx থেকে নয় — তাই
#       ওই ফাইলে unresolved শূন্য, আর নিয়মটা ঠিক ওখানেই ধরে ফেলে।)
#
# ⛔ সীমা, লুকানো হয়নি: androidx / retrofit / okhttp / org.json এই পরিবেশে
#    নামানো যায় না (maven.google.com বন্ধ)। তাই যেসব ফাইল androidx-এর
#    ক্লাস থেকে আসে, তাদের ভিতরের টাইপ এখনো পুরোপুরি মেলানো যায় না।
# ══════════════════════════════════════════════════════════════════════
ANDROID_JAR = os.path.join(ROOT, "00_GUARD", ".kotlinc", "android34.jar")
ANDROID_JAR_URL = ("https://repo1.maven.org/maven2/org/robolectric/android-all/"
                   "14-robolectric-10818077/android-all-14-robolectric-10818077.jar")


def find_android_jar(allow_download=True):
    """compileSdk 34-এর android.jar — না থাকলে একবার নামায় (~132 MB)।"""
    env = os.environ.get("ANDROID_JAR", "")
    if env and os.path.exists(env):
        return env
    if os.path.exists(ANDROID_JAR) and os.path.getsize(ANDROID_JAR) > 50_000_000:
        return ANDROID_JAR
    if not allow_download:
        return None
    try:
        os.makedirs(os.path.dirname(ANDROID_JAR), exist_ok=True)
        print("   android.jar নামাচ্ছি (~১৩২ MB, একবারই)…")
        urllib.request.urlretrieve(ANDROID_JAR_URL, ANDROID_JAR + ".part")
        os.replace(ANDROID_JAR + ".part", ANDROID_JAR)
        return ANDROID_JAR
    except Exception as e:
        print("   android.jar নামানো গেল না: %s" % e)
        try:
            os.remove(ANDROID_JAR + ".part")
        except Exception:
            pass
    return None


def real_type_errors(log_text):
    """যে ফাইলে **একটাও** "unresolved reference" নেই অথচ ভুল আছে —
       সেগুলো ফেরত দেয়। ধাক্কার ফল হওয়া অসম্ভব, তাই আসল ভুল।"""
    unres, other = {}, {}
    for ln in log_text.splitlines():
        m = re.match(r"^(.*\.kt):(\d+):(\d+): error: (.*)$", ln.rstrip())
        if not m:
            continue
        path = m.group(1).replace("\\", "/")
        path = path[path.find("com/"):] if "com/" in path else path
        if "unresolved reference" in m.group(4).lower():
            unres[path] = unres.get(path, 0) + 1
        else:
            other.setdefault(path, []).append(
                "%s:%s — %s" % (path.split("/")[-1], m.group(2), m.group(4)))
    return [(f, v) for f, v in sorted(other.items()) if unres.get(f, 0) == 0]


# ══════════════════════════════════════════════════════════════════════════
# 🔴🔴🔴 V840 — **TK-এর বিল্ড ভাঙা থেকে ধরা পড়া ফাঁক** (২৯.০৮.২০২৬)
#
# কী হয়েছিল: V833-এ `HELPLINE` ধ্রুবকটা `object BranchCatalog`-এর ভিতরে
# লেখা হয়েছিল, কিন্তু ১২ জায়গায় ডাকা হয়েছিল `BranchInfo.HELPLINE` বলে —
# ভুল নাম। Android Studio সাথে সাথে ধরল:
#     Unresolved reference: HELPLINE :381
# অথচ **এই পাহারাদার পাশ করিয়ে দিয়েছিল।**
#
# কেন ধরেনি: উপরের `real_type_errors()` শুধু তখনই FAIL করে যখন কোনো ফাইলে
# "unresolved reference" **একটাও নেই** অথচ অন্য ভুল আছে। কিন্তু এই ভুলটা
# নিজেই "unresolved reference" — আর ওই ফাইলগুলো androidx-এর হাজারো
# একই ধরনের গোলমালে ভরা ⇒ **আসল ভুলটা গোলমালের ভিড়ে হারিয়ে গেল।**
#
# ✅ নতুন নিয়ম: "Unresolved reference: X" — যেখানে **X নামটা এই প্রজেক্টেরই
#    কোনো ফাইলে ঘোষণা করা আছে** (object · class · const val · val · fun) —
#    সেটা কখনোই বাইরের লাইব্রেরি না-থাকার ফল হতে পারে না। ওটা **আসল ভুল**।
#    ⇒ সরাসরি FAIL, বেসলাইনে চাপা পড়বে না।
#
# ⛔ ভুল করে ধরার (false positive) আশঙ্কা কম: নামটা প্রজেক্টে ঘোষিত হতে হবে,
#    আর androidx/android-এর নামের সাথে মেলে এমন সাধারণ নাম বাদ রাখা হয়েছে।
# ══════════════════════════════════════════════════════════════════════════

# প্রজেক্টের নাম হলেও এগুলো Android/androidx-এও আছে — তাই বাদ (নইলে ভুয়া ধরা)
OWN_NAME_SKIP = {
    "View", "Context", "Intent", "Bundle", "Activity", "Fragment", "Adapter",
    "Builder", "Companion", "Type", "Item", "Entry", "Options", "Info",
    "Model", "Repository", "State", "Result", "Error", "Log", "Color",
}


def project_declared_names(src_root):
    """প্রজেক্টের নিজের **ধ্রুবক-জাতীয়** নামগুলো, যেগুলোর
       "unresolved reference" কখনোই বাইরের লাইব্রেরি না-থাকার ফল হতে পারে না:

         · `object NAME`            — যেমন `object BranchCatalog`
         · `const val NAME`         — যেমন `const val HELPLINE`
         · একদম বাঁ-ধার (indent 0) থেকে লেখা `val/var/fun NAME`

       ⛔ **ইচ্ছে করে বাদ:** ভিতরের (indented) সাধারণ `val`/`fun` ও
          `override` — কারণ `onCreate` · `intent` · `resources`-এর মতো নাম
          AppCompatActivity থেকেও আসে, আর androidx এখানে নেই বলে সেগুলোর
          "unresolved reference" **স্বাভাবিক গোলমাল**। ওগুলো ধরলে দিনে
          ৬৮০টা ভুয়া ভুল আসত ⇒ আসল ভুল আবার ভিড়ে হারাত।
    """
    names = set()
    re_obj = re.compile(r"^\s*(?:(?:public|internal|private|sealed)\s+)*object\s+([A-Za-z_][A-Za-z0-9_]*)")
    re_const = re.compile(r"^\s*(?:(?:public|internal|private|protected)\s+)*const\s+val\s+([A-Za-z_][A-Za-z0-9_]*)")
    re_top = re.compile(r"^(?:(?:public|internal|private|inline|suspend)\s+)*(?:val|var|fun)\s+([A-Za-z_][A-Za-z0-9_]*)")
    for root, _dirs, files in os.walk(src_root):
        for f in files:
            if not f.endswith(".kt"):
                continue
            try:
                with open(os.path.join(root, f), encoding="utf-8") as fh:
                    for ln in fh:
                        if "override" in ln:
                            continue
                        for rx in (re_obj, re_const, re_top):
                            m = rx.match(ln)
                            if m:
                                names.add(m.group(1))
                                break
            except Exception:
                pass
    return names - OWN_NAME_SKIP


def own_unresolved_errors(log_text, src_root):
    """প্রজেক্টের নিজের নামের "unresolved reference" — সবগুলো আসল ভুল।"""
    own = project_declared_names(src_root)
    out = []
    seen = set()
    for ln in log_text.splitlines():
        m = re.match(r"^(.*\.kt):(\d+):(\d+): error: (.*)$", ln.rstrip())
        if not m:
            continue
        msg = m.group(4)
        m2 = re.search(r"[Uu]nresolved reference:?\s+'?([A-Za-z_][A-Za-z0-9_]*)", msg)
        if not m2:
            continue
        name = m2.group(1)
        if name not in own:
            continue
        path = m.group(1).replace("\\", "/")
        path = path[path.find("com/"):] if "com/" in path else path
        key = (path, m.group(2), name)
        if key in seen:
            continue
        seen.add(key)
        out.append("%s:%s — Unresolved reference: %s  (এই নামটা প্রজেক্টেই ঘোষিত ⇒ আসল ভুল)"
                   % (path.split("/")[-1], m.group(2), name))
    return out


# ── বাছাই: কোনটা "বাইরের লাইব্রেরি নেই বলে", কোনটা আসল ──────────────────────
EXT_PKG = ("android", "androidx", "org.json", "kotlinx", "okhttp3", "okio",
           "java", "javax", "kotlin", "com.google")

# স্পষ্টতই ধাক্কার ফল — এগুলো আসল ভুলের প্রমাণ নয়
CASCADE = (
    "overrides nothing", "cannot infer a type", "overload resolution ambiguity",
    "variable expected", "[error type", "no value passed for parameter",
    "cannot access", "not enough information",
    # 🔴 V597: "type mismatch" এখান থেকে **তুলে দেওয়া হলো** — ঠিক এই
    #    বার্তাটাই TK-র Android Studio-র বিল্ড ভেঙেছিল, অথচ এখানে চাপা
    #    পড়ে যাচ্ছিল। এখন এটা আসল সন্দেহভাজন হিসেবেই গোনা হয়।
    "none of the following candidates", "cannot be applied",
    "is not a function", "smart cast",
)


def project_vocabulary():
    """প্রকল্পের সব ফাইল পড়ে তালিকা বানায় —
       ১) বাইরের import-এর নাম, ২) `.নাম` হিসেবে ব্যবহৃত সব সদস্যের নাম,
       ৩) 🔴 V575: **প্রতিটা ফাইলের নিজের** import ও `.নাম` আলাদা করে।

    🔴🔴 V575-এ ধরা পড়া ফাঁক (TK-এর Android Studio-র স্ক্রিনশট, ২৩.০৮.২০২৬):
    `DoctorCheckupActivity.kt`-এ খালি `View(this)` লেখা ছিল, অথচ ওই ফাইলে
    `android.view.View` import করা **ছিল না** — Android Studio-তে
    "Unresolved reference: View" এসে বিল্ড ভেঙেছিল। কিন্তু এই পাহারাদার
    সেটা ধরতে পারেনি, কারণ `View` **অন্য ফাইলে** import করা আছে বলে নামটা
    গোটা-প্রকল্পের তালিকায় ছিল, তাই "বাইরের লাইব্রেরির গোলমাল" ধরে নিত।
    ⇒ এখন যাচাইটা **ফাইল-ধরে-ফাইল** হয়: যে ফাইলে ভুলটা, সেই ফাইলে নামটা
      import করা আছে কি না বা `.নাম` হিসেবে ব্যবহার হয়েছে কি না — তবেই ছাড়।"""
    ext = {"android", "androidx", "org", "kotlinx", "okhttp3", "okio",
           "java", "javax", "com", "it", "this", "field", "R", "BuildConfig"}
    members = set()
    per_file = {}
    for folder, _d, files in os.walk(SRC):
        for f in files:
            if not f.endswith(".kt"):
                continue
            full = os.path.join(folder, f)
            t = open(full, encoding="utf-8", errors="replace").read()
            imports = set()
            for ln in t.splitlines():
                m = re.match(r"\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?", ln)
                if m:
                    name = m.group(2) or m.group(1).split(".")[-1]
                    imports.add(name)
                    if m.group(1).startswith(EXT_PKG):
                        ext.add(name)
            members.update(re.findall(r"\.(\w+)", t))
            key = full.replace("\\", "/")
            key = key[key.find("com/"):] if "com/" in key else key
            # শুধু **import**-এর নাম রাখা হয় — `.View`-এর মতো ব্যবহার নয়।
            # কারণ পুরো নাম লিখে (`android.view.View(...)`) ব্যবহার করলে
            # কম্পাইলার গোড়ার `android` নিয়ে অভিযোগ করে, `View` নিয়ে নয়।
            per_file[key] = imports
    return ext, members, per_file


# গোটা-প্রকল্পে ছাড় পাওয়া নাম — এগুলো প্যাকেজের গোড়া বা কম্পাইলারের নিজের
# শব্দ, কোনো ফাইলেই import লাগে না।
ROOT_NAMES = {"android", "androidx", "org", "kotlinx", "okhttp3", "okio",
              "java", "javax", "com", "it", "this", "field", "R", "BuildConfig"}


def is_noise(msg, ext, members, mine=None):
    u = re.match(r"unresolved reference: (\w+)$", msg)
    if u:
        name = u.group(1)
        if name in ROOT_NAMES:
            return True
        # 🔴🔴🔒 V775 (২৮.০৮.২০২৬) — **TK-এর Android Studio-তে বিল্ড ভাঙল,
        #    অথচ এই পাহারাদার "PASS" বলেছিল।** TK: *"পাহারাদার কি করে
        #    আপনাকে আটকালো না?"*
        #
        #    **আসল ফাঁক:** নিচের লাইনটা `members`-কেও ছাড় দিত। `members` মানে
        #    প্রকল্পের **যেকোনো** ফাইলে `.নাম` হিসেবে লেখা সব নাম। তাই
        #    `user` নামটা অন্য কোথাও `.user` হিসেবে থাকায়, DashboardActivity-র
        #    আসল ভুল ("Unresolved reference: user" — ওই ফাংশনে `user` বলে
        #    কিছুই নেই) **গোলমাল ধরে বাদ** পড়ে গিয়েছিল।
        #
        #    **সমাধান:** ছোট হাতের অক্ষরে শুরু হওয়া নাম = ভেরিয়েবল/ফাংশন,
        #    ক্লাস নয়। এদের জন্য `members`-এর ঢালাও ছাড় আর নেই — শুধু
        #    সত্যিকারের import করা বাইরের নাম (`ext`) হলে ছাড়। বাকি সব
        #    **সন্দেহভাজন** হিসেবে যায়, আর বেসলাইনে না থাকলে **FAIL**।
        #    ⛔ বড় হাতের নাম (ক্লাস) আগের নিয়মেই চলে — androidx/material
        #       এই পরিবেশে নেই বলে ওই ছাড়টা না রাখলে শত শত মিথ্যা ভুল আসত।
        if name[:1].islower() and name not in ext:
            return False
        # 🔵 V575 টীকা: "ওই ফাইলে import আছে কি না" ধরে বাছাই করে দেখা হয়েছিল,
        #    কিন্তু তাতে উত্তরাধিকারে পাওয়া ধ্রুবক (MODE_PRIVATE,
        #    LAYER_TYPE_SOFTWARE) ও ভিতরের ক্লাস (ActivityLifecycleCallbacks,
        #    LayoutResultCallback) মিথ্যে ভুল হিসেবে ধরা পড়ছিল — অথচ
        #    Android Studio-তে ওগুলো ঠিকঠাক বিল্ড হয়। তাই এই বাছাইটা আগের
        #    মতোই রইল, আর "import ছাড়া ক্লাস ব্যবহার"-এর আসল ভুল ধরার জন্য
        #    নিচে আলাদা, নিখুঁত পাহারা `missing_import_errors()` বসানো হলো।
        return name in ext or name in members
    low = msg.lower()
    if low.startswith("unresolved reference. "):
        return False          # ⚠️ আসল ধরা হয় — `$until`-এর ভুল এভাবেই আসে
    return any(k in low for k in CASCADE)


def normalise(msg):
    """লাইন নম্বর বদলালেও যেন বেসলাইন না ভাঙে — সংখ্যা সরিয়ে দেওয়া হয়।"""
    return re.sub(r"\d+", "N", msg).strip()


def collect_errors(log_text, ext, members, per_file=None):
    """(ফাইল, বার্তা) জোড়ার সেট — শুধু **আসল সন্দেহভাজন** ভুল।
       লাইন/কলাম বাদ, তাই কোড উপরে-নিচে সরালেও বেসলাইন ভাঙে না।"""
    pairs, total = set(), 0
    for ln in log_text.splitlines():
        m = re.match(r"^(.*\.kt):(\d+):(\d+): error: (.*)$", ln.rstrip())
        if not m:
            continue
        total += 1
        msg = m.group(4)
        path = m.group(1).replace("\\", "/")
        path = path[path.find("com/"):] if "com/" in path else path
        mine = (per_file or {}).get(path)
        if is_noise(msg, ext, members, mine):
            continue
        pairs.add((path, normalise(msg)))
    return pairs, total


# ══════════════════════════════════════════════════════════════════════
# 🔴🔴 V575 — **"import না করে Android-এর ক্লাস ব্যবহার" ধরার পাহারা**
# ----------------------------------------------------------------------
# কেন (TK-এর Android Studio-র স্ক্রিনশট, ২৩.০৮.২০২৬ সকাল ৮:০৩):
#     DoctorCheckupActivity.kt  →  Unresolved reference: View :1598
#                                  Unresolved reference: setBackgroundColor :1599
#                                  Unresolved reference: layoutParams :1600
# `buildToolBar()`-এ খালি `View(this)` লেখা ছিল, অথচ ওই ফাইলে
# `android.view.View` import করা ছিল না। উপরের কম্পাইলার-পাহারা এটা ধরতে
# পারেনি, কারণ এই পরিবেশে Android SDK নেই বলে `View`-এর ভুলটাও "বাইরের
# লাইব্রেরির গোলমাল" হিসেবে বাদ পড়ে যেত।
#
# এই পাহারাটা কম্পাইলার ছাড়াই, **শুধু লেখা পড়ে** কাজ করে:
#   ১) প্রকল্পের সব ফাইল থেকে `import android…/androidx…`-এর ক্লাসের নাম জমা হয়
#   ২) প্রতিটা ফাইলে দেখা হয় — ওই নামগুলোর কোনোটা `Name(` বা `Name.` ভাবে
#      ব্যবহার হয়েছে কি না, অথচ **ওই ফাইলে** import নেই
#   ৩) শুধু **বড় হাতের অক্ষরে শুরু** নাম দেখা হয় (ক্লাস), ছোট হাতের নয় —
#      নইলে `EditText.addTextChangedListener()`-এর মতো নিজের সদস্য-ফাংশনও
#      মিথ্যে ভুল হয়ে ধরা পড়ত (পরীক্ষা করে দেখা হয়েছে)
#   ৪) কমেন্ট ও উদ্ধৃতির ভিতরের লেখা বাদ, পুরো নাম লিখে ব্যবহার
#      (`android.view.View(...)`) বাদ, ওই ফাইলেই ঘোষিত ক্লাস বাদ
#
# ✅ প্রমাণ: ভুলটা ইচ্ছে করে ফেরত বসিয়ে চালানো হয়েছে — ধরা পড়ে ঠিক একটাই
#    সারি (DoctorCheckupActivity.kt → View); সারানোর পরে **শূন্য**।
# ══════════════════════════════════════════════════════════════════════
IMP_SKIP = {"R", "BuildConfig"}


def _strip_code(t):
    """কমেন্ট ও উদ্ধৃতির ভিতরের লেখা সরিয়ে দেয় — নইলে বাংলা টীকা বা
       বার্তার ভিতরের শব্দও কোড বলে ভুল করত।"""
    t = re.sub(r"/\*.*?\*/", " ", t, flags=re.S)
    t = re.sub(r"//[^\n]*", " ", t)
    t = re.sub(r'"""(?:.|\n)*?"""', '""', t)
    t = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', t)
    return t


def missing_import_errors():
    """(ফাইল, ক্লাসের নাম, কোন প্যাকেজের) — import ছাড়া ব্যবহারের তালিকা।"""
    files = {}
    for folder, _d, fs in os.walk(SRC):
        for f in fs:
            if f.endswith(".kt"):
                p = os.path.join(folder, f)
                files[p] = open(p, encoding="utf-8", errors="replace").read()
    known = {}
    for t in files.values():
        for m in re.finditer(
                r"^\s*import\s+((?:android|androidx)[\w.]*)(?:\s+as\s+(\w+))?",
                t, re.M):
            name = m.group(2) or m.group(1).split(".")[-1]
            if name[:1].isupper():
                known.setdefault(name, m.group(1))
    # 🔴🔴🔒 V807 (২৮.০৮.২০২৬) — **TK-এর Android Studio-তে আবার বিল্ড ভাঙল**
    #   ("Unresolved reference: PilesClinicApplication", PatientPhotoCache.kt:41),
    #   অথচ এই পাহারাদার PASS বলেছিল। TK: *"কি ধরনের ফালতু পাহারাদার রেখেছেন …
    #   ইন্সপেক্টর রাখুন"*।
    #   **আসল গর্ত:** উপরের লুপটা শুধু `android`/`androidx` ক্লাস শিখত —
    #   **প্রকল্পের নিজের ক্লাস কখনো দেখত না**। তাই উপরের প্যাকেজের
    #   (`com.tkbiswas.pilesclinic`) ক্লাস নিচের প্যাকেজে (`…native`) import
    #   ছাড়া লিখলে কেউ ধরত না — অথচ Kotlin-এ ওটা লিখতেই হয়।
    #   এখন প্রকল্প নিজে যত ক্লাস/object/interface ঘোষণা করে, সবগুলোই শেখা হয়।
    #   ⛔ একই প্যাকেজ হলে ছাড় (নিচে `pkg == mypkg` যাচাই) — মিথ্যে ভুল আসবে না।
    for t in files.values():
        pm = re.search(r"^\s*package\s+([\w.]+)", t, re.M)
        if not pm:
            continue
        pkg = pm.group(1)
        for m in re.finditer(
                r"^\s*(?:@\w+\s+)*(?:public\s+|internal\s+|private\s+|open\s+|abstract\s+"
                r"|sealed\s+|data\s+|enum\s+|annotation\s+|value\s+)*"
                r"(?:class|object|interface)\s+(\w+)", t, re.M):
            known.setdefault(m.group(1), pkg)
    hits = []
    for p in sorted(files):
        code = _strip_code(files[p])
        mine = {(m.group(2) or m.group(1).split(".")[-1]) for m in
                re.finditer(r"^\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?",
                            code, re.M)}
        declared = set(re.findall(
            r"\b(?:class|object|interface|enum class|annotation class)\s+(\w+)",
            code))
        mypkg_m = re.search(r"^\s*package\s+([\w.]+)", code, re.M)
        mypkg = mypkg_m.group(1) if mypkg_m else ""
        for name, pkg in known.items():
            if name in mine or name in IMP_SKIP or name in declared:
                continue
            if pkg == mypkg:      # 🔴 V807 — একই প্যাকেজে import লাগে না
                continue
            if re.search(r"(?<![\w.])" + re.escape(name) + r"\s*[.(]", code):
                short = p.replace("\\", "/")
                short = short[short.find("com/"):] if "com/" in short else short
                hits.append((short, name, pkg))
    return hits


def run_compiler(kotlinc, android_jar=None):
    out_dir = os.path.join("/tmp", "kt_guard_out")
    shutil.rmtree(out_dir, ignore_errors=True)
    env = dict(os.environ)
    env["JAVA_TOOL_OPTIONS"] = ""          # লগ পরিষ্কার রাখতে
    cmd = [kotlinc, "-J-Xmx8g", "-nowarn"]
    if android_jar:                        # 🔴 V597
        cmd += ["-cp", android_jar]
    cmd += ["com", "-d", out_dir]
    try:
        p = subprocess.run(
            cmd,
            cwd=SRC, env=env, capture_output=True, text=True, timeout=2400)
        return (p.stderr or "") + (p.stdout or "")
    except subprocess.TimeoutExpired:
        return None
    except Exception as e:
        print("   কম্পাইলার চালানো গেল না: %s" % e)
        return None


def load_baseline():
    if not os.path.exists(BASELINE):
        return None
    pairs = set()
    for ln in open(BASELINE, encoding="utf-8"):
        ln = ln.rstrip("\n")
        if not ln or ln.startswith("#"):
            continue
        if "\t" not in ln:
            continue
        f, m = ln.split("\t", 1)
        pairs.add((f, m))
    return pairs


def save_baseline(pairs):
    with open(BASELINE, "w", encoding="utf-8") as fh:
        fh.write("# 🛡️ Kotlin কম্পাইলারের **জানা গোলমাল** (Android SDK নেই বলে)।\n")
        fh.write("# ⛔ হাতে লিখবেন না — `verify_kotlin_compile.py --update-baseline`\n")
        fh.write("# প্রতিটা সারি:  ফাইল <TAB> ভুলের বার্তা (সংখ্যা N দিয়ে ঢাকা)\n")
        fh.write("# মোট: %d\n" % len(pairs))
        for f, m in sorted(pairs):
            fh.write("%s\t%s\n" % (f, m))


def main():
    update = "--update-baseline" in sys.argv
    print("🛡️ Kotlin কম্পাইল-পাহারা (V497 · V597-এ android.jar যোগ)")
    print("=" * 64)

    if not os.path.isdir(SRC):
        print("❌ FAIL — Kotlin source ফোল্ডার পাওয়া গেল না:\n   %s" % SRC)
        return 1

    kotlinc = find_kotlinc()
    if not kotlinc:
        print()
        print("⚠️ SKIPPED — kotlinc পাওয়া যায়নি, তাই **কম্পাইল যাচাই হয়নি**।")
        print("   ⛔ এটাকে PASS ধরা যাবে না। কম্পাইলার বসিয়ে আবার চালান:")
        print("   %s" % KOTLIN_URL)
        print()
        print("ফল: SKIPPED — যাচাই করা যায়নি। ⛔ PASS নয়।")
        return 1

    print("   কম্পাইলার: %s" % kotlinc)
    android_jar = find_android_jar()       # 🔴 V597
    if android_jar:
        print("   android.jar: %s" % android_jar)
    else:
        print()
        print("⚠️ SKIPPED — android.jar পাওয়া গেল না, তাই **টাইপ যাচাই হয়নি**।")
        print("   ⛔ এটাকে PASS ধরা যাবে না। নামিয়ে আবার চালান:")
        print("   %s" % ANDROID_JAR_URL)
        print()
        print("ফল: SKIPPED — যাচাই করা যায়নি। ⛔ PASS নয়।")
        return 1
    print("   কম্পাইল হচ্ছে… (কয়েক মিনিট লাগে)")
    log = run_compiler(kotlinc, android_jar)
    if log is None:
        print()
        print("⚠️ SKIPPED — কম্পাইলার শেষ করতে পারল না (সময়/মেমরি)।")
        print("ফল: SKIPPED — যাচাই করা যায়নি। ⛔ PASS নয়।")
        return 1

    # 🔴 V575 — কম্পাইলারের আগেই "import ছাড়া ক্লাস ব্যবহার" দেখে নেওয়া হয়
    imp = missing_import_errors()
    if imp:
        print()
        print("❌ FAIL — import না করেই Android-এর ক্লাস ব্যবহার করা হয়েছে")
        print("   (Android Studio-তে ঠিক এখানেই 'Unresolved reference' এসে")
        print("    বিল্ড ভেঙে যাবে):")
        for f, name, pkg in imp:
            print("  ❌ %s" % f)
            print("       %s — `import %s` লেখা নেই" % (name, pkg))
        print()
        print("ফল: FAIL — TK-কে এই ফাইল পাঠানো যাবে না. ⛔ PASS নয়।")
        return 1
    print("   ✅ import ছাড়া কোনো Android ক্লাস ব্যবহার হয়নি")

    # 🔴🔴 V597 — বেসলাইনের **আগেই**, যাতে কোনোভাবেই চাপা না পড়ে
    hard = real_type_errors(log)
    if hard:
        print()
        print("❌ FAIL — এই ফাইলগুলোতে **আসল ভুল** আছে (একটাও unresolved নেই,")
        print("   তাই লাইব্রেরি না-থাকার গোলমাল হতেই পারে না):")
        for f, msgs in hard:
            print("  ❌ %s  — %d টা ভুল" % (f.split("/")[-1], len(msgs)))
            for m in msgs[:6]:
                print("       %s" % m[:150])
            if len(msgs) > 6:
                print("       … আরো %d টা" % (len(msgs) - 6))
        print()
        print("ফল: FAIL — Android Studio-তে এই ফাইল বিল্ড হবে না। ⛔ PASS নয়।")
        return 1
    # 🔴🔴🔴 V840 — প্রজেক্টের **নিজের নামে** unresolved reference = আসল ভুল।
    #   (V833-এ ঠিক এভাবেই `BranchInfo.HELPLINE` পার হয়ে গিয়ে TK-এর
    #    Android Studio বিল্ড ভেঙেছিল — উপরের ব্যাখ্যা দেখুন।)
    own = own_unresolved_errors(log, SRC)
    if own:
        print()
        print("❌ FAIL — প্রজেক্টের **নিজের নামে** unresolved reference (%d টা)।" % len(own))
        print("   এই নামগুলো প্রজেক্টেই ঘোষিত (object · const val · top-level),")
        print("   তাই বাইরের লাইব্রেরি না-থাকার ফল হতেই পারে না — **আসল ভুল**।")
        for m in own[:20]:
            print("  ❌ %s" % m[:170])
        if len(own) > 20:
            print("       … আরো %d টা" % (len(own) - 20))
        print()
        print("ফল: FAIL — Android Studio-তে বিল্ড হবে না। ⛔ ফাইল পাঠানো যাবে না।")
        return 1

    print("   ✅ android.jar দিয়ে টাইপ যাচাই — আসল ভুল ০")

    ext, members, per_file = project_vocabulary()
    found, total = collect_errors(log, ext, members, per_file)
    print("   কম্পাইলারের মোট ভুল      : %d" % total)
    print("   বাইরের লাইব্রেরির গোলমাল : %d (বাদ)" % (total - len(found)))
    print("   আসল সন্দেহভাজন           : %d" % len(found))

    if update:
        save_baseline(found)
        print()
        print("✅ নতুন বেসলাইন লেখা হলো: %d সারি" % len(found))
        print("   (এটা শুধু **ভালো, বিল্ড-হওয়া** অবস্থায় চালাবেন)")
        return 0

    base = load_baseline()
    if base is None:
        print()
        print("❌ FAIL — বেসলাইন ফাইল নেই (kotlin_noise_baseline.txt)।")
        print("   ভালো অবস্থায় একবার চালান: --update-baseline")
        print()
        print("ফল: FAIL — যাচাই করা যায়নি। ⛔ PASS নয়।")
        return 1

    new = sorted(found - base)
    gone = len(base - found)

    print("   বেসলাইনে জানা গোলমাল : %d" % len(base))
    print("   নতুন ভুল             : %d" % len(new))
    if gone:
        print("   (বেসলাইনের %d টা আর নেই — ভালো খবর)" % gone)
    print()

    if new:
        print("❌ FAIL — Kotlin কোডে **নতুন ভুল** আছে, এই ফাইল বিল্ড হবে না:")
        for f, m in new[:40]:
            print("  ❌ %s" % f.split("/")[-1])
            print("       %s" % m[:150])
        if len(new) > 40:
            print("  … আরও %d টা" % (len(new) - 40))
        print()
        print("ফল: FAIL — TK-কে এই ফাইল পাঠানো যাবে না। ⛔ PASS নয়।")
        return 1

    print("PASS — Kotlin কোডে নতুন কোনো ভুল নেই ✅")
    print("⚠️ মনে রাখুন: এটা Android Studio-র পুরো বিল্ড নয় (resource · manifest ·")
    print("   dex এখানে যাচাই হয় না)। কিন্তু Kotlin-এর ভুল আর পার হতে পারবে না।")
    return 0


if __name__ == "__main__":
    sys.exit(main())
