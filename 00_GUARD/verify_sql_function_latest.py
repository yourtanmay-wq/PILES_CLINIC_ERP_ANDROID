#!/usr/bin/env python3
"""
🔴 V1082 (০৪.০৯.২০২৬) — TK: *"আরো সঠিকভাবে গভীরে যাচাই করুন যাতে এই ধরনের
সমস্যা ভবিষ্যতে আর কোনদিনও না হয়"*

যে ভুলটা এটা ধরে:
  একটা SQL ফাংশন আগে অনেকবার লেখা হয়ে থাকতে পারে (V325 → V380 → V470 → V488)।
  নতুন ফাইলে সেটা আবার লেখার সময় ভুল করে **পুরনো** সংস্করণ নকল করলে মাঝের
  সব ভালো কাজ চুপচাপ মুছে যায় — ঠিক যেটা V1080-এ হয়েছিল।

কীভাবে ধরে (আন্দাজে নয়):
  প্রতিটা ফাংশনের সবচেয়ে নতুন সংজ্ঞাটা বার করে দেখা হয় — তার আগের সংস্করণে
  থাকা কোনো **চিহ্ন-শব্দ** (যেমন `rmp_auto_default_percent`,
  `rmp_commission_branch_defaults`) নতুনটায় হারিয়ে গেছে কিনা।
  হারালে থামিয়ে দেয়।
"""
import glob, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FILES = sorted(glob.glob(os.path.join(ROOT, "04_SUPABASE_DATABASE_SETUP", "*.sql"))) + \
        sorted(glob.glob(os.path.join(ROOT, "00_SQL", "*.sql")))

# যে শব্দগুলো হারালে সত্যিকারের ক্ষতি — প্রতিটার সঙ্গে কেন, সেটাও লেখা
MARKERS = {
    "rmp_auto_default_percent":        "V488 — হার বসানো না থাকলে স্বয়ংক্রিয় ১০%",
    "rmp_commission_branch_defaults":  "V470 — RMP-র ব্রাঞ্চ-নির্দিষ্ট হার",
    "rate_changed_on":                 "V941 — তারিখ থেকে হার বদলের নিয়ম",
    "rmp_earned_for":                  "V941 — কমিশন গোনার একটাই নিয়ম",
    "rmp_guard_refby":                 "V1080 — Ref By না মিললে কমিশন আটকানো",
}

def vnum(path):
    m = re.search(r"/V(\d+)", path)
    return int(m.group(1)) if m else 0

def bodies(text):
    """ফাইলের প্রতিটা `create or replace function fin.X` -> **শুধু তার শরীর**।

    ⚠️ শরীরটা `$$;`-এ শেষ হয় — এর পরের আলাদা statement-গুলো ওই ফাংশনের অংশ
    নয়। আগে পুরো বাকি অংশটা ধরা হত, তাই একই ফাইলের অন্য কোনো UPDATE-এ থাকা
    শব্দ ভুল করে ফাংশনের ভিতরের বলে গোনা হত (মিথ্যা সতর্কতা)।
    """
    out = {}
    parts = re.split(r"(?=create or replace function\s+fin\.)", text)
    for p in parts:
        m = re.match(r"create or replace function\s+(fin\.\w+)", p)
        if not m:
            continue
        end = p.find("$$;")
        body = p[: end + 3] if end != -1 else p
        out.setdefault(m.group(1), []).append(body)
    return out

seen = {}   # fn -> list of (vnum, filename, body)
for f in FILES:
    try:
        txt = open(f, encoding="utf-8").read()
    except Exception:
        continue
    for fn, bs in bodies(txt).items():
        for b in bs:
            seen.setdefault(fn, []).append((vnum(f), os.path.basename(f), b))

problems = []
for fn, lst in seen.items():
    if len(lst) < 2:
        continue
    lst.sort(key=lambda x: x[0])
    newest_v, newest_file, newest_body = lst[-1]
    for v, fname, body in lst[:-1]:
        for marker, why in MARKERS.items():
            if marker in body and marker not in newest_body:
                problems.append(
                    f"   {fn}\n"
                    f"      পুরনো {fname} (V{v})-এ ছিল «{marker}» — {why}\n"
                    f"      কিন্তু সবচেয়ে নতুন {newest_file} (V{newest_v})-এ নেই।")

print("=" * 66)
print("🛡️  SQL ফাংশন-পাহারা — পুরনো সংস্করণ নকল করে ভালো কাজ মোছা হয়নি তো?")
print("=" * 66)
print(f"   দেখা হলো: {len(FILES)} ফাইল · {len(seen)} ফাংশন")
if problems:
    print("-" * 66)
    print("❌ BLOCKED — নতুন সংজ্ঞায় আগের কাজ হারিয়ে গেছে:\n")
    for p in sorted(set(problems)):
        print(p)
    print("\n   ⇒ সবচেয়ে নতুন সংস্করণটা নকল করে তার উপরে বদল করুন।")
    sys.exit(1)
print("✅ PASS — প্রতিটা ফাংশনের নতুন সংজ্ঞায় আগের কাজগুলো অটুট।")
