#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛡️ V510 Trash-Restore পাহারা (২১.০৮.২০২৬, TK-রিপোর্ট — KAMAL ROY)

লাইভ ডেটাবেসে দুটো ট্রিগার আছে, দুটোই **কোনো ভুল না দেখিয়ে** কাজ বাতিল করে:

  · `tk_block_deleted_return` (INSERT/UPDATE) — `deleted_records`-এ "মুছে ফেলা"
    চিহ্ন থাকলে সারিটা বসতেই দেয় না (`RETURN NULL`)।
    ⇒ **আগে চিহ্ন তুলতে হবে, তারপর বসাতে হবে।**

  · `tk_terminal_no_return` (INSERT/UPDATE, followups) — Cancelled/Incomplete/
    Rejected/Closed সারিকে Active করা যায় শুধু তখনই, যখন `history`-র **শেষ**
    এন্ট্রিতে থাকে status="Active" আর remark শুরু "Restored"/"Continue" দিয়ে।
    ⇒ **status-এর সঙ্গে ঐ history লাইনটাও পাঠাতে হবে।**

এই পাহারা নিশ্চিত করে ফোন ও ওয়েব — দুই দিকেই নিয়ম দুটো মানা হচ্ছে।
⛔ এই ফাইল কিছু বদলায় না — শুধু পড়ে ও মেলায়।
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KT = ROOT / "02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic"
WEB = ROOT / "03_NETLIFY_READY"

fails, oks = [], []


def read(p):
    if not p.exists():
        fails.append("ফাইলটাই নেই: %s" % p.name)
        return ""
    return p.read_text(encoding="utf-8", errors="replace")


def strip_kt_comments(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"^\s*//.*$", "", s, flags=re.M)


def strip_js_comments(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"^\s*//.*$", "", s, flags=re.M)


# ── ১) ফোন — TrashRepository.restore() ───────────────────────────────────
kt = strip_kt_comments(read(KT / "native/TrashRepository.kt"))
if kt:
    m = re.search(r"fun restore\(.*?\n    \}", kt, re.S)
    body = m.group(0) if m else ""
    if not body:
        fails.append("[১] `TrashRepository.restore()` খুঁজে পাওয়া গেল না")
    else:
        # ক) মূল record: unmark আগে, upsert পরে
        i_un = body.find("DeletedGuard.unmark")
        i_up = body.find("upsertRestoreSafe")
        if i_un < 0 or i_up < 0 or i_un > i_up:
            fails.append("[১ক] ফোনে চিহ্ন তোলা (`DeletedGuard.unmark`) upsert-এর আগে নেই — "
                         "`tk_block_deleted_return` সারিটা চুপচাপ বাতিল করবে")
        else:
            oks.append("[১ক] ফোনে আগে চিহ্ন তোলা, তারপর রেকর্ড বসানো ✅")

        # খ) cascaded followups: status-এর সঙ্গে history লাইন
        need = ['fields.put("history"', '"Restored', 'ctable == "followups"']
        miss = [n for n in need if n not in body]
        if miss:
            fails.append("[১খ] ফলো-আপ ফেরানোর সময় history লাইনটা পাঠানো হচ্ছে না "
                         "(নেই: " + " · ".join(miss) + ") — `tk_terminal_no_return` "
                         "সারিটা Cancelled-ই রেখে দেবে")
        else:
            oks.append("[১খ] ফোনে ফলো-আপ ফেরানোর সময় `history`-তে \"Restored\" লাইন যায় ✅")

        # গ) history আগে পড়ে নেওয়া হয় (পুরোনো লেখা যেন না মোছে)
        if "fetchListSlimOrNull" not in body or "id,history" not in body:
            fails.append("[১গ] পুরোনো `history` সার্ভার থেকে পড়া হচ্ছে না — "
                         "ফেরালে আগের ইতিহাস মুছে যেতে পারে")
        else:
            oks.append("[১গ] পুরোনো `history` পড়ে নিয়ে শেষে লাইন যোগ হয় (কিছু মোছে না) ✅")

        # ঘ) cascaded-এও unmark আগে
        i_c_un = body.find("DeletedGuard.unmark(ctable")
        i_c_up = body.find("updateById(ctable")
        if i_c_un < 0 or i_c_up < 0 or i_c_un > i_c_up:
            fails.append("[১ঘ] ফলো-আপের ক্ষেত্রেও চিহ্ন তোলা update-এর আগে নেই")
        else:
            oks.append("[১ঘ] ফলো-আপেও আগে চিহ্ন তোলা, তারপর লেখা ✅")

# ── ২) ওয়েব — wlv1RestoreTrash() ─────────────────────────────────────────
js = strip_js_comments(read(WEB / "app.js"))
if js:
    m = re.search(r"async function wlv1RestoreTrash\([^)]*\)\{.*?\n\}", js, re.S)
    body = m.group(0) if m else ""
    if not body:
        fails.append("[২] ওয়েবের `wlv1RestoreTrash()` খুঁজে পাওয়া গেল না")
    else:
        # ⛔ `rfind` — এই ফাংশনে চিহ্ন-তোলার ডাক **দুবার** আছে ("নবীন কপি রাখা"
        #    শাখাতেও একটা)। `find` দিলে ঐ প্রথমটাই ধরা পড়ত আর ক্রম উল্টে দিলেও
        #    পাহারা মিথ্যে "পাশ" বলত (একবার সত্যিই তাই হয়েছিল)।
        i_un = body.rfind("wlv1UnmarkDeletedCloud(x.table,rid)")
        i_up = body.find("directCloudUpsertRow(x.table,rec)")
        if i_un < 0 or i_up < 0 or i_un > i_up:
            fails.append("[২ক] ওয়েবে চিহ্ন তোলা রেকর্ড বসানোর আগে নেই — "
                         "`tk_block_deleted_return` চুপচাপ বাতিল করবে")
        else:
            oks.append("[২ক] ওয়েবে আগে চিহ্ন তোলা, তারপর রেকর্ড বসানো ✅")

        if "wlv1RestoreCascadedFollowups" not in body:
            fails.append("[২খ] ওয়েবে ফলো-আপ ফেরানোর ডাকটাই নেই — রোগী ফিরবে, "
                         "ফলো-আপ কার্ড লুকানো থেকে যাবে")
        else:
            oks.append("[২খ] ওয়েবেও ফলো-আপ ফেরানো হয় ✅")

    m2 = re.search(r"async function wlv1RestoreCascadedFollowups\(trashRow\)\{.*?\n\}", js, re.S)
    body2 = m2.group(0) if m2 else ""
    if not body2:
        fails.append("[২গ] ওয়েবে `wlv1RestoreCascadedFollowups()` ফাংশনটাই নেই")
    else:
        need = ["wlv1UnmarkDeletedCloud", "Restored from Trash", "history"]
        miss = [n for n in need if n not in body2]
        if miss:
            fails.append("[২গ] ওয়েবের ফলো-আপ ফেরানোয় নেই: " + " · ".join(miss))
        else:
            oks.append("[২গ] ওয়েবেও `history`-তে \"Restored\" লাইন যায়, চিহ্নও আগে ওঠে ✅")

print("🛡️ V510 Trash-Restore পাহারা")
print("=" * 64)
for o in oks:
    print("  " + o)
if fails:
    print()
    for f in fails:
        print("  ❌ " + f)
    print()
    print("FAIL — Restore-এর নিয়ম ভেঙেছে ❌")
    sys.exit(1)
print()
print("PASS — ফোন ও ওয়েব দুই দিকেই Restore-এর নিয়ম মানা আছে ✅")
