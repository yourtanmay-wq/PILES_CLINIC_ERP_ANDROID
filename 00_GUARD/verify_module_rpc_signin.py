#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛑 পাহারাদার — ক্লাউডে ডাক পাঠানোর আগে লগইন আছে কিনা (V429, ১৭.০৮.২০২৬)

কেন বানানো হলো
--------------
TK-এর চেম্বার পর্দায় RMP কমিশন কিছুই দেখাচ্ছিল না। কারণ: ওই পর্দা ক্লাউডে ডাক
পাঠাত `RmpCommissionRepository` হয়ে, কিন্তু পর্দাটায় **লগইন করানো হয়নি** —
তাই ডাক ব্যর্থ হয়ে চুপচাপ খালি ফল দিত। ব্যবহারকারী কোনো এররও দেখতেন না,
জিনিসটা শুধু "নেই" মনে হতো। এই পাহারাদার সেই ভুল আর ঢুকতে দেবে না।

কীভাবে কাজ করে
--------------
১) আগে খুঁজে বার করে **কোন কোন Repository/Helper** ফাইল `ModuleAuth`-এর
   মাধ্যমে ক্লাউডে ডাক পাঠায়।
২) তারপর দেখে — `native/` ফোল্ডারের কোনো **Activity** ওই ফাইলগুলোর কোনোটা
   (বা সরাসরি `ModuleAuth`) ব্যবহার করছে কিনা।
৩) করলে সেই Activity-তে লগইনের লাইন (`signInCurrentSession` বা `isSignedIn`)
   থাকতেই হবে। না থাকলে **FAIL**।

⛔ `modules/` ফোল্ডারের পর্দাগুলো বাদ — ওগুলো `ModuleUi.ensureSignedIn()` গেট
   দিয়ে খোলে, লগইন সেখানেই হয়ে যায়।

চালানো:  python3 00_GUARD/verify_module_rpc_signin.py <প্রজেক্ট-ফোল্ডার>
"""
import os
import re
import sys

CALLS = (
    "ModuleAuth.rpc(",
    "ModuleAuth.getRowsChecked(",
    "ModuleAuth.upsert(",
    "ModuleAuth.insertChecked(",
    "ModuleAuth.insert(",
)
SIGNIN = ("ModuleAuth.signInCurrentSession(", "ModuleAuth.isSignedIn")


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"^\s*//.*$", "", src, flags=re.M)
    return src


def main() -> int:
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    files = {}
    for dirpath, _dirs, fns in os.walk(root):
        for fn in fns:
            if fn.endswith(".kt"):
                p = os.path.join(dirpath, fn)
                try:
                    files[p] = strip_comments(open(p, encoding="utf-8").read())
                except Exception:
                    pass

    # ১) কোন Repository/Helper ক্লাউডে ডাক পাঠায়
    carriers = set()
    for p, src in files.items():
        if any(c in src for c in CALLS):
            # যে সাহায্যকারী **নিজেই** লগইন করিয়ে নেয় (যেমন IePermit), তার
            # ডাকা পর্দাকে আলাদা করে লগইন করাতে হয় না — তাই সে তালিকায় নয়।
            if any(sg in src for sg in SIGNIN):
                continue
            carriers.add(os.path.basename(p)[:-3])   # ফাইলের নাম = object/class-এর নাম
    carriers.discard("ModuleAuth")

    # ২) native/ ফোল্ডারের Activity-গুলো যাচাই
    bad = []
    checked = 0
    for p, src in sorted(files.items()):
        rel = p.replace(root, "").lstrip("/\\").replace("\\", "/")
        if "/native/" not in rel or not rel.endswith("Activity.kt"):
            continue
        uses = [c for c in carriers if re.search(r"\b" + re.escape(c) + r"\s*\.", src)]
        direct = any(c in src for c in CALLS)
        if not uses and not direct:
            continue
        checked += 1
        if any(s in src for s in SIGNIN):
            continue
        bad.append((rel, sorted(uses) if uses else ["ModuleAuth"]))

    print("ক্লাউডে ডাক পাঠায় এমন সাহায্যকারী ফাইল:", len(carriers))
    print("যাচাই করা native পর্দা:", checked)
    # V429: অ্যাপ চালু হওয়ার সময় ModuleAuth.attachContext(...) বসানো থাকলে
    # গোপন লগইন (reAuth) **সব পর্দাতেই** কাজ করে — তখন এটা আর ভাঙা নয়।
    app_guard = any(
        "ModuleAuth.attachContext(" in src
        for p2, src in files.items()
        if p2.endswith("PilesClinicApplication.kt")
    )
    if bad and app_guard:
        print("\n⚠️  নিচের পর্দায় আলাদা লগইনের লাইন নেই, তবে অ্যাপ চালু হওয়ার সময়")
        print("    ModuleAuth.attachContext(...) বসানো আছে — তাই গোপন লগইন কাজ করবে:")
        for rel, uses in bad:
            print("   ·", rel, "→", ", ".join(uses))
        print("PASS (সতর্কতা সহ) ✅")
        return 0
    if bad:
        print("\n🛑 FAIL — নিচের পর্দা ক্লাউডে ডাক পাঠায় কিন্তু লগইন করায় না:")
        for rel, uses in bad:
            print("   ✗", rel, "→", ", ".join(uses))
        print("\n   ঠিক করার নিয়ম — ডাকের আগে এই লাইনটা বসান:")
        print("   if (!ModuleAuth.isSignedIn) ModuleAuth.signInCurrentSession(applicationContext)")
        return 1
    print("PASS — প্রতিটা পর্দাতেই ডাকের আগে লগইন আছে ✅")
    return 0


if __name__ == "__main__":
    sys.exit(main())
