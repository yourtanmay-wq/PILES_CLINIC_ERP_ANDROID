#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🔴🔴🔴 পাহারাদার — **পাঠানো ZIP-এর ভিতরে যা থাকার কথা, সত্যিই আছে তো?**

TK-রিপোর্ট (২৮.০৮.২০২৬, Android Studio-র ছবিসহ):
    ERROR: …\\PILES_CLINIC_APP_V791_FINAL\\… Android resource linking failed :83
    AndroidManifest.xml — 2 errors (line 83 = <application …>)

─── আসল কারণ (প্রমাণ করে দেখা, আন্দাজ নয়) ────────────────────────────────
ZIP বানানোর সময় গোড়ার ডেমো-ছবিগুলো বাদ দিতে চালানো হত:
    zip -d …zip "PILES_CLINIC_APP_Vxxx_FINAL/*.png"
কিন্তু `zip`-এর প্যাটার্নে `*` **`/`-ও মিলিয়ে ফেলে**। তাই ওই এক লাইনেই
**পুরো প্রকল্পের সব `.png`** মুছে যেত — `res/mipmap-*/ic_launcher.png` ও
`ic_launcher_round.png` সহ। তখন AndroidManifest-এর
`android:icon="@mipmap/ic_launcher"` ও `android:roundIcon=…` আর খুঁজে
পাওয়া যেত না ⇒ ঠিক ওই **দুটো** resource-linking error, ঠিক ৮৩ নম্বর লাইনে।

যাচাই: V791-এর ZIP-এ `.png` ছিল **শূন্যটা**, অথচ প্রকল্পে ৭২টা
(গোড়ায় ৫৫টা ডেমো-ছবি + res/-এ ১১টা + বাকিগুলো)।

─── কেন আগের পাহারাদার ধরেনি ──────────────────────────────────────────────
`tk_guard.py` · `verify_kotlin_compile.py` · `verify_android_resources.py` —
তিনটেই **প্রকল্পের কোড** দেখে, কেউই **পাঠানো ZIP** খুলে দেখে না।
ZIP বানানোর ধাপটাই পাহারার বাইরে ছিল। এই ফাইলটা সেই ফাঁক বন্ধ করে।

চালানোর নিয়ম:  python3 00_GUARD/verify_zip_contents.py <zip ফাইল>
"""
import os
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

# ইচ্ছে করে বাদ দেওয়া হয় (জায়গা বাঁচাতে) — শুধু এই দুটোই
def is_deliberately_dropped(rel: str) -> bool:
    # (ক) প্রকল্পের **গোড়ার** ডেমো-ছবি — ভিতরের ফোল্ডারের নয়
    if "/" not in rel and rel.lower().endswith(".png"):
        return True
    # (খ) অ্যানাটমি ছবির আসল (বিশাল) কপি
    if rel.startswith("08_ASSETS_BACKUP/ANATOMY_PICTURES/ORIGINAL/"):
        return True
    return False


def main():
    if len(sys.argv) < 2:
        print("ব্যবহার: python3 00_GUARD/verify_zip_contents.py <zip ফাইল>")
        return 2
    zpath = sys.argv[1]
    if not os.path.exists(zpath):
        print("❌ ZIP ফাইলটাই নেই:", zpath)
        return 1

    tracked = subprocess.check_output(
        ["git", "ls-files"], cwd=ROOT).decode("utf-8").split("\n")
    tracked = [t for t in tracked if t.strip()]

    with zipfile.ZipFile(zpath) as z:
        names = z.namelist()
    if not names:
        print("❌ ZIP ফাঁকা")
        return 1
    prefix = names[0].split("/")[0] + "/"
    inside = set(n[len(prefix):] for n in names if n.startswith(prefix))

    missing, dropped = [], 0
    for t in tracked:
        if is_deliberately_dropped(t):
            if t not in inside:
                dropped += 1
            continue
        if t not in inside:
            missing.append(t)

    print("=" * 66)
    print("🛡️  পাঠানো ZIP-এর ভিতরের যাচাই")
    print("=" * 66)
    print("   ZIP                     :", os.path.basename(zpath))
    print("   প্রকল্পে ফাইল           :", len(tracked))
    print("   ইচ্ছে করে বাদ           :", dropped)
    print("   ZIP-এ পাওয়া গেল        :", len(inside))

    # 🔴 সবচেয়ে জরুরি — অ্যাপের আইকন (এটাই V791-এ হারিয়ে বিল্ড ভেঙেছিল)
    must = [t for t in tracked if "/res/mipmap-" in t]
    lost_icons = [m for m in must if m not in inside]

    print("-" * 66)
    if lost_icons:
        print("❌ FAIL — অ্যাপের আইকন ZIP-এ নেই ⇒ Android Studio-তে")
        print("   'Android resource linking failed' (AndroidManifest-এর")
        print("   android:icon / android:roundIcon খুঁজে পাবে না):")
        for m in lost_icons[:12]:
            print("     ·", m)
    if missing:
        print("❌ FAIL — এই ফাইলগুলো প্রকল্পে আছে কিন্তু ZIP-এ নেই:", len(missing), "টা")
        for m in missing[:15]:
            print("     ·", m)
        if len(missing) > 15:
            print("     … আরও", len(missing) - 15)
    if lost_icons or missing:
        print("=" * 66)
        print("⛔ এই ফাইল TK-কে পাঠানো যাবে না — আগে ZIP আবার বানান।")
        print("   ⚠️ `zip -d`-এর প্যাটার্নে `*` স্ল্যাশও মেলায় — তাই")
        print("      \"…/*.png\" লিখলে **সব** png মুছে যায়। গোড়ার ছবি বাদ")
        print("      দিতে হলে প্রতিটা নাম আলাদা করে দিন (নিচের নিয়মে)।")
        return 1
    print("✅ PASS — প্রকল্পের প্রতিটা দরকারি ফাইল ZIP-এ আছে")
    print("   (অ্যাপের", len(must), "টা আইকনও ঠিক আছে)")
    print("=" * 66)
    return 0


if __name__ == "__main__":
    sys.exit(main())
