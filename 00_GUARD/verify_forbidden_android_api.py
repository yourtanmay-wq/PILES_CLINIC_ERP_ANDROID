#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🔴 V491 (২০.০৮.২০২৬) — নতুন পাহারাদার।

**কেন বানানো হলো:**
২০.০৮.২০২৬ রাত ১০:৩১-এ TK-এর বিল্ড ভেঙে গেল:

    Cannot access '<init>': it is package-private in 'LayoutResultCallback'
    Cannot access '<init>': it is package-private in 'WriteResultCallback'

`PrescriptionWhatsAppShare.kt`-এ `PrintDocumentAdapter.LayoutResultCallback` ও
`WriteResultCallback`-এর object বানানো হয়েছিল। ওদের constructor
**package-private** — `android.print` প্যাকেজের বাইরে থেকে বানানোই যায় না।
পুরনো পাহারাদারগুলো এটা ধরতে পারেনি, কারণ **পাহারাদার Kotlin কম্পাইল করে না**।

এই ফাইলটা ওই নির্দিষ্ট ফাঁদগুলো লেখা-দেখেই ধরে ফেলে, তাই একই ভুল আর
কখনো TK-এর কম্পিউটার পর্যন্ত পৌঁছাবে না।

⛔ এটা কিছু বদলায় না — শুধু খুঁজে দেখে ও বলে।

⚠️ **মনে রাখার কথা:** এটা আসল কম্পাইলারের বিকল্প নয়। পাহারাদার পাশ করলেও
   Android Studio-তে বিল্ড করাই একমাত্র চূড়ান্ত প্রমাণ।
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src")

# (খোঁজার নমুনা, কেন নিষিদ্ধ, তার বদলে কী করতে হবে)
FORBIDDEN = [
    (
        r"object\s*:\s*PrintDocumentAdapter\.LayoutResultCallback",
        "PrintDocumentAdapter.LayoutResultCallback-এর constructor package-private",
        "WebView-কে PdfDocument-এর canvas-এ এঁকে PDF বানান (PrescriptionWhatsAppShare দেখুন)",
    ),
    (
        r"object\s*:\s*PrintDocumentAdapter\.WriteResultCallback",
        "PrintDocumentAdapter.WriteResultCallback-এর constructor package-private",
        "WebView-কে PdfDocument-এর canvas-এ এঁকে PDF বানান (PrescriptionWhatsAppShare দেখুন)",
    ),
    (
        r"^\s*package\s+android\.",
        "নিজের ক্লাস `android.*` প্যাকেজে রাখা — কিছু ফোনে চলার সময় IllegalAccessError",
        "নিজের প্যাকেজে রাখুন (com.tkbiswas.pilesclinic.*)",
    ),
]


def main():
    if not os.path.isdir(SRC):
        print("FAIL — source ফোল্ডার পাওয়া গেল না: " + SRC)
        return 1

    problems = []
    checked = 0
    for folder, _dirs, files in os.walk(SRC):
        for name in files:
            if not (name.endswith(".kt") or name.endswith(".java")):
                continue
            path = os.path.join(folder, name)
            checked += 1
            try:
                text = open(path, encoding="utf-8").read()
            except Exception:
                continue
            for line_no, line in enumerate(text.splitlines(), 1):
                stripped = line.strip()
                # মন্তব্যের ভিতরের উদাহরণ ধরা হবে না
                if stripped.startswith("//") or stripped.startswith("*"):
                    continue
                for pattern, why, fix in FORBIDDEN:
                    if re.search(pattern, line):
                        rel = os.path.relpath(path, ROOT)
                        problems.append((rel, line_no, stripped[:90], why, fix))

    print("যাচাই করা ফাইল: %d" % checked)
    if problems:
        print("FAIL — Android-এ যেগুলো তৈরি করাই যায় না, সেগুলো পাওয়া গেছে:")
        for rel, line_no, snippet, why, fix in problems:
            print("  ❌ %s:%d" % (rel, line_no))
            print("     %s" % snippet)
            print("     কারণ  : %s" % why)
            print("     করণীয় : %s" % fix)
        return 1

    print("PASS — Android-এর নিষিদ্ধ/অসম্ভব API কোথাও ব্যবহার হয়নি ✅")
    return 0


if __name__ == "__main__":
    sys.exit(main())
