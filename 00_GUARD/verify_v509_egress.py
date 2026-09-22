#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛡️ V509 Egress-পাহারা (২১.০৮.২০২৬, TK-নির্দেশ — Supabase Egress ১০০% ছোঁয়ার পরে)

TK-এর কথা: *"সততার সাথে কাজ করবেন কোন ভাল কাজ যেন খারাপ না হয়... যার মোবাইল
চালু থাকবে তাকে যেন সাথে সাথে দেখায়।"*

এই পাহারা **চারটে** জিনিস আটকে রাখে, যাতে ভবিষ্যতে কেউ (আমি নিজেও) না জেনে
আবার ভাঙতে না পারে:

  ১) হোম পর্দার "calls pending" ব্যানার আর **প্রতিবার ফিরলেই** তিনটে পূর্ণ
     `fetchTab()` চালাবে না — আগে একটা সস্তা HEAD-পাহারা (`bannerWatch`) ও
     সবচেয়ে-কম-ফাঁক (`BANNER_MIN_GAP_MS`) পার হতে হবে। **এটাই ছিল দিনে
     ~৪০০ MB-র মূল ফুটো।**

  ২) হোম পর্দায় ৩০-সেকেন্ডের লাইভ-টিক **ফিরে আসবে না**, আর ঐ সস্তা পাহারার
     নিয়ম তিনটে অটুট থাকবে: সময়-সীমা `changed()`-এর **আগে**; গোনার আগে
     `CloudReadCache` মোছা; একবারে একটাই ভারী গোনা (`bannerBusy`)।
     (`Watch.changed()` `true` ফেরার সঙ্গে সঙ্গেই ঘড়ি এগিয়ে দেয় — তাই এই
     তিনটের একটাও ভাঙলে সত্যিকারের একটা পেমেন্ট/এনকোয়ারি চিরকাল চাপা পড়ে
     যেতে পারে।)

  ৩) পিছনের worker-এর ফাঁক **১৫ মিনিটই** থাকবে। বাড়ালে Egress প্রায় কিছুই
     বাঁচে না (ভারী কাজ এমনিতেই `MIN_GAP_MS`=৬০ মিনিটে আটকানো), অথচ
     `flushEverythingWaiting()` — আটকে থাকা পেমেন্ট পাঠানোর একমাত্র নিয়মিত
     রাস্তা — ৪ গুণ ধীর হয়ে যায়।

  ৪) ঘন্টা গোনার সময় `activity_logs`-এর ৫০০০ সারি **শুধু দরকার হলেই** পড়া হবে।

⛔ এই ফাইল কিছু বদলায় না — শুধু পড়ে ও মেলায়।
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KT = ROOT / "02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic"

fails = []
oks = []


def read(rel):
    p = KT / rel
    if not p.exists():
        fails.append("ফাইলটাই নেই: %s" % rel)
        return ""
    return p.read_text(encoding="utf-8", errors="replace")


# ── ১) ব্যানারের সস্তা পাহারা ────────────────────────────────────────────
dash = read("native/DashboardActivity.kt")
if dash:
    m = re.search(r"private fun refreshCallBanner\(.*?\n    \}", dash, re.S)
    # ⛔ মন্তব্য বাদ দিয়েই মেলানো হয় — নইলে কেউ কোড তুলে দিলেও মন্তব্যে নামটা
    #    থেকে যাওয়ায় পাহারা মিথ্যে "পাশ" বলত (একবার সত্যিই তাই হয়েছিল)।
    body = re.sub(r"^\s*//.*$", "", m.group(0), flags=re.M) if m else ""
    if not body:
        fails.append("[১] `refreshCallBanner()` খুঁজে পাওয়া গেল না")
    else:
        need = [
            ("bannerWatch.changed", "সস্তা HEAD-পাহারা (`bannerWatch.changed`)"),
            ("BANNER_MIN_GAP_MS", "সবচেয়ে-কম-ফাঁক (`BANNER_MIN_GAP_MS`)"),
            ("bannerCloudDone", "প্রথমবার সবসময় সত্যিকারের গোনা (`bannerCloudDone`)"),
        ]
        miss = [d for k, d in need if k not in body]
        if miss:
            fails.append("[১] ব্যানারে নেই: " + " · ".join(miss))
        else:
            oks.append("[১] হোম-ব্যানার কিছু না বদলালে তিনটে পূর্ণ `fetchTab()` চালায় না ✅")

    # পাহারাটা যেন চারটে টেবিলই দেখে (একটাও বাদ দিলে বদল ধরা পড়বে না)
    mw = re.search(r"private val bannerWatch = LiveRefresh\.Watch\(([^)]*)\)", dash)
    tables = set(re.findall(r'"([a-z_]+)"', mw.group(1))) if mw else set()
    want = {"followups", "enquiries", "patients", "payments"}
    if not want.issubset(tables):
        fails.append("[১] `bannerWatch` এই টেবিলগুলো দেখছে না: %s" % ", ".join(sorted(want - tables)))
    else:
        oks.append("[১] `bannerWatch` চারটে টেবিলই দেখে (followups·enquiries·patients·payments) ✅")

    # ── ২) হোম পর্দায় ৩০-সেকেন্ডের লাইভ-টিক যেন **ফিরে না আসে** ────────
    #    কারণ (কোড থেকে প্রমাণিত): `Watch.changed()` `true` ফেরার সঙ্গে সঙ্গেই
    #    নিজের ঘড়ি এগিয়ে দেয় (LiveRefresh.kt:173)। হোম পর্দায় টিক বসিয়ে তার
    #    উপরে কোনো সময়-সীমা দিলে সত্যিকারের একটা পেমেন্ট/এনকোয়ারি চিরকালের
    #    জন্য চাপা পড়ে যেতে পারে। তার উপরে ৫ টেবিল × ৩০ সেকেন্ড = ঘণ্টায়
    #    ৬০০ প্রশ্ন — Egress কমার বদলে বাড়ে।
    for bad in ("liveCheckForChanges", "liveRunnable", "LIVE_MIN_GAP_MS"):
        if bad in dash:
            fails.append("[২] হোম পর্দায় লাইভ-টিক আবার ঢুকেছে (`%s`) — "
                         "খবর হারানোর ঝুঁকি ও বাড়তি খরচ, দুটোই ফিরে আসবে" % bad)
            break
    else:
        oks.append("[২] হোম পর্দায় ৩০-সেকেন্ডের লাইভ-টিক নেই (ইচ্ছাকৃত) ✅")

    # সময়-সীমা `changed()`-এর **আগে** থাকতে হবে, পরে নয়
    if body:
        i_gap = body.find("BANNER_MIN_GAP_MS")
        i_chg = body.find("bannerWatch.changed")
        if i_gap < 0 or i_chg < 0 or i_gap > i_chg:
            fails.append("[২] সময়-সীমা `changed()`-এর পরে চলে গেছে — "
                         "তাহলে ধরা-পড়া বদল হারিয়ে যাবে (LiveRefresh.kt:173)")
        else:
            oks.append("[২] সময়-সীমা `changed()`-এর আগে — কোনো বদল হারায় না ✅")
        # ⛔ দুটোই লাগবে। শুধু CloudReadCache মুছলে নিচের CloudReadDedupe (৬০
        #    সেকেন্ড) থেকে পুরনো উত্তরই ফিরত — নতুন সারিটা চিরকাল বাদ পড়ত।
        miss_clear = [c for c in ("CloudReadCache.clear()", "CloudReadDedupe.clear()") if c not in body]
        if miss_clear:
            fails.append("[২] গোনার আগে এগুলো মোছা হয় না: " + " · ".join(miss_clear) +
                         " — জমানো পুরনো উত্তর ফিরলে নতুন সারি চিরকাল বাদ পড়বে")
        else:
            oks.append("[২] গোনার আগে জমানো উত্তর **দুই স্তরেই** মোছা হয় (Cache + Dedupe) ✅")

        # ⛔ ৩০ মিনিটে একবার জোর করে পূর্ণ গোনা — নইলে সত্যিকারের ডিলিট
        #    (`changed()` শুধু updatedAt দেখে) কোনোদিন ধরা পড়বে না।
        if "BANNER_FULL_GAP_MS" not in body or "forced" not in body:
            fails.append("[২] জোর-করা পূর্ণ গোনা (`BANNER_FULL_GAP_MS`) নেই — "
                         "সত্যিকারের ডিলিট হলে ব্যানারের সংখ্যা চিরকাল বেশি দেখাবে")
        else:
            oks.append("[২] ৩০ মিনিটে একবার জোর করে পূর্ণ গোনা — ডিলিটও ধরা পড়ে ✅")

        # ⛔ জমানো সংখ্যা দেখানো (`render(instant)`) `bannerBusy`-র **আগে** থাকতে
        #    হবে, নইলে ব্যস্ত থাকলে ফোনের নিজের হিসাবটাও দেখানো বাদ যেত।
        i_inst = body.find("render(instant)")
        i_busy = body.find("if (bannerBusy) return@launch")
        if i_inst < 0 or i_busy < 0 or i_inst > i_busy:
            fails.append("[২] `bannerBusy`-র পাহারা জমানো সংখ্যা দেখানোর আগে চলে গেছে — "
                         "ব্যস্ত থাকলে ব্যানার পুরনো সংখ্যাতেই আটকে থাকবে")
        else:
            oks.append("[২] ব্যস্ত থাকলেও ফোনের জমানো সংখ্যাটা আগে দেখানো হয় ✅")
        if "bannerBusy" not in dash:
            fails.append("[২] `bannerBusy` নেই — দুটো ভারী গোনা একসঙ্গে চলে যেতে পারে")
        else:
            oks.append("[২] একবারে একটাই ভারী গোনা (`bannerBusy`) ✅")

    mc = re.search(r"BANNER_MIN_GAP_MS\s*=\s*(\d+)L\s*\*\s*(\d+)L\s*\*\s*(\d+)L", dash)
    if not mc:
        fails.append("[২] `BANNER_MIN_GAP_MS` ধ্রুবকটাই নেই")
    else:
        secs = int(mc.group(1)) * int(mc.group(2)) * int(mc.group(3)) / 1000.0
        if secs < 60 or secs > 600:
            fails.append("[২] `BANNER_MIN_GAP_MS` = %ds — ৬০ থেকে ৬০০ সেকেন্ডের মধ্যে থাকা উচিত" % secs)


# ── ৩) worker-এর ফাঁক ১৫ মিনিটই ─────────────────────────────────────────
app = read("PilesClinicApplication.kt")
if app:
    mp = re.search(
        r"PeriodicWorkRequestBuilder<com\.tkbiswas\.pilesclinic\.native\.BackgroundRefreshWorker>\(\s*(\d+),"
        r"\s*java\.util\.concurrent\.TimeUnit\.MINUTES",
        app,
    )
    if not mp:
        fails.append("[৩] background worker-এর সময়সীমা খুঁজে পাওয়া গেল না")
    elif int(mp.group(1)) != 15:
        fails.append(
            "[৩] worker-এর ফাঁক %s মিনিট — ১৫ থাকতে হবে। বাড়ালে আটকে থাকা পেমেন্ট "
            "পাঠানো (`flushEverythingWaiting`) ধীর হয়ে যায়, অথচ Egress বাঁচে না "
            "(ভারী কাজ এমনিতেই MIN_GAP_MS=৬০ মিনিটে আটকানো)।" % mp.group(1)
        )
    else:
        oks.append("[৩] background worker ১৫ মিনিটেই আছে — আটকে থাকা পেমেন্ট দ্রুত যায় ✅")

wk = read("native/BackgroundRefreshWorker.kt")
if wk:
    mg = re.search(r"MIN_GAP_MS\s*=\s*(\d+)L\s*\*\s*(\d+)L\s*\*\s*(\d+)L", wk)
    if not mg:
        fails.append("[৩] `MIN_GAP_MS` খুঁজে পাওয়া গেল না")
    else:
        mins = int(mg.group(1)) * int(mg.group(2)) * int(mg.group(3)) / 60000.0
        if mins < 60:
            fails.append("[৩] `MIN_GAP_MS` %d মিনিট — ভারী কাজ ঘণ্টায় একবারের বেশি চলে যাবে" % mins)
        else:
            oks.append("[৩] ভারী prewarm ঘণ্টায় একবারের বেশি চলে না (MIN_GAP_MS ≥ ৬০ মিনিট) ✅")
    # ⛔ শুধু "লাইনটা আছে কি না" দেখলে হবে না — কেউ ওটাকে `if (দিন != আজ)`-এর
    #    ভিতরে মুড়ে দিলে prewarm দিনে একবার হয়ে যাবে আর Work Notebook-এর
    #    'Today Patient' সকাল ৬টার (প্রায় শূন্য) সংখ্যা দেখাবে। তাই লাইনটা
    #    **হুবহু শর্তহীন** কিনা মিলিয়ে দেখা হয়।
    chamber_line = re.compile(
        r"^\s*try \{ ChamberAttendanceRepository\.loadBoard\(today, user\.branch, ctx\) \}"
        r" catch \(_: Throwable\) \{\}\s*$",
        re.M,
    )
    if not chamber_line.search(wk):
        fails.append("[৩] Chamber বোর্ডের prewarm শর্তহীন নেই (সরানো/শর্তে মোড়া) — "
                     "Work Notebook-এর 'Today Patient' ভুল/শূন্য হয়ে যাবে")
    elif re.search(r"chamber_prewarm|KEY_CHAMBER_DAY", wk):
        fails.append("[৩] Chamber prewarm-এ আবার 'দিনে একবার' চিহ্ন ঢুকেছে (KEY_CHAMBER_DAY) — "
                     "'Today Patient' ভুল হয়ে যাবে")
    else:
        oks.append("[৩] Chamber বোর্ড আগের মতোই (শর্তহীন) prewarm হয় — 'Today Patient' ঠিক থাকে ✅")


# ── ৪) ঘন্টা গোনায় ৫০০০-সারির পড়া শুধু দরকার হলে ────────────────────────
bell = read("native/BellCounter.kt")
if bell:
    mb = re.search(r"if \(all\.isNotEmpty\(\)\) \{[^}]*fetchFeeMissingSeenKeys", bell, re.S)
    if not mb:
        fails.append("[৪] `fetchFeeMissingSeenKeys()` আর `all.isNotEmpty()`-র ভিতরে নেই — "
                     "প্রতিবার ঘন্টা গুনতে `activity_logs`-এর ৫০০০ সারি নামবে")
    else:
        oks.append("[৪] `activity_logs`-এর বড় পড়া শুধু দরকার হলেই চলে ✅")


print("🛡️ V509 Egress-পাহারা")
print("=" * 64)
for o in oks:
    print("  " + o)
if fails:
    print()
    for f in fails:
        print("  ❌ " + f)
    print()
    print("FAIL — Egress-এর নিয়ম ভেঙেছে ❌")
    sys.exit(1)
print()
print("PASS — Egress-এর সব নিয়ম কোডে বসানো আছে ✅")
