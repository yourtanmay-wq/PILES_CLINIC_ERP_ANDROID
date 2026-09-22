#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🔴 V436 (১৮.০৮.২০২৬) — নতুন পাহারাদার।

**কেন:** TK-এর রিপোর্ট — Dr. K.H MANDAL-এর ফোনে নতুন APK বসানোর পরেও পুরনো
V259-ই চলছিল, কেউ টেরই পাননি। এখন অ্যাপ নিজে `03_NETLIFY_READY/version.json`
দেখে বুঝে নেয় সে পুরনো কিনা। কিন্তু ওই ফাইলের সংখ্যাটা `build.gradle.kts`-এর
সংখ্যার সঙ্গে না মিললে পুরো ব্যবস্থাটাই ভুল হয়ে যেত —
  · কম থাকলে: পুরনো ফোন কোনো সতর্কবার্তা পেত না (আগের সেই বিপদ ফিরে আসত)
  · বেশি থাকলে: নতুন ফোনেও মিথ্যে "আপনার অ্যাপ পুরনো" উঠত

তাই প্রতিবার প্যাকেজ করার আগে এই পাহারাদারটা চালানো হয়।
⛔ এটা কিছু বদলায় না — শুধু মিলিয়ে দেখে ও বলে।
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRADLE = os.path.join(
    ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "build.gradle.kts"
)
VJSON = os.path.join(ROOT, "03_NETLIFY_READY", "version.json")


def fail(msg):
    print("FAIL — " + msg)
    sys.exit(1)


def main():
    if not os.path.exists(GRADLE):
        fail("build.gradle.kts পাওয়া গেল না: " + GRADLE)
    if not os.path.exists(VJSON):
        fail("version.json পাওয়া গেল না: " + VJSON)

    src = open(GRADLE, encoding="utf-8").read()
    m_code = re.search(r"^val appVersionCode = (\d+)\s*$", src, re.M)
    m_name = re.search(r'^val appVersionName = "([^"]+)"\s*$', src, re.M)
    if not m_code or not m_name:
        fail("build.gradle.kts-এ appVersionCode / appVersionName খুঁজে পাওয়া গেল না")
    code = int(m_code.group(1))
    name = m_name.group(1)

    try:
        data = json.load(open(VJSON, encoding="utf-8"))
    except Exception as e:
        fail("version.json পড়া গেল না (লেখা ভুল?): %s" % e)

    j_code = data.get("versionCode")
    j_name = data.get("versionName")

    if not isinstance(j_code, int):
        fail('version.json-এ "versionCode" একটা সংখ্যা হতে হবে, পাওয়া গেল: %r' % (j_code,))
    if j_code != code:
        fail(
            "মিলছে না — build.gradle.kts: versionCode=%d, কিন্তু version.json: %d\n"
            "        ⇒ দুটোই %d করুন।" % (code, j_code, code)
        )
    if j_name != name:
        fail(
            'মিলছে না — build.gradle.kts: versionName="%s", কিন্তু version.json: "%s"\n'
            "        ⇒ দুটোই \"%s\" করুন।" % (name, j_name, name)
        )

    print("PASS — version.json ও build.gradle.kts মিলেছে (V%d / %s) ✅" % (code, name))


if __name__ == "__main__":
    main()
