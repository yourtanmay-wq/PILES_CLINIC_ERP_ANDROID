#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛡️ V511 পাহারা (২১.০৮.২০২৬, TK লাইভ টেস্টে ধরা তিনটে জিনিস)

  ১) **ব্যানারের সংখ্যা = ভিতরের সংখ্যা।** হোম পেজের "N calls pending today"
     চাপলে শুধু *আজ কল করার কথা* যাঁদের, তাঁরাই দেখাবে (`bannerCallsOnly` /
     `callsOnly`)। অন্য ভাবে Today খুললে ০৫.০৮.২০২৬-এর নিয়মই বহাল
     (কল-ডিউ **অথবা** আজকের নতুন Visit/Registration)।

  ২) **Staff Performance-এর তালিকা থেকে Back কাজ করবে।** ফেরার ঠিকানা
     "তখন যা ছিল" থেকে আন্দাজ করা চলবে না (`perfListBack`) — নইলে ডিটেল
     থেকে ফিরলে Back একই তালিকাই আবার আঁকে, পর্দা আটকে যায়।

  ৩) **Trash Bin-এর নতুন ছোট কার্ড** — 👁 View · Restore ও Delete পাশাপাশি ·
     একসাথে অনেকগুলো বাছা · কার্ডেই সম্পূর্ণ ডিটেলস · **নেভি ব্লু নয়**।
     ফোন ও ওয়েব — দুই দিকেই।

⛔ এই ফাইল কিছু বদলায় না — শুধু পড়ে ও মেলায়।
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KT = ROOT / "02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic"
RES = ROOT / "02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/res"
WEB = ROOT / "03_NETLIFY_READY"

fails, oks = [], []


def read(p):
    if not p.exists():
        fails.append("ফাইলটাই নেই: %s" % p.name)
        return ""
    return p.read_text(encoding="utf-8", errors="replace")


def nocomment_kt(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"^\s*//.*$", "", s, flags=re.M)


def nocomment_js(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"^\s*//.*$", "", s, flags=re.M)


# ══ ১) ব্যানার = ভিতরের তালিকা ═══════════════════════════════════════════
fu = nocomment_kt(read(KT / "native/FollowUpActivity.kt"))
if fu:
    if "bannerCallsOnly = intent.getBooleanExtra(\"todayOnly\", false)" not in fu:
        fails.append("[১] ফোনে `bannerCallsOnly` ব্যানার থেকে আসা কিনা দেখে বসানো হচ্ছে না")
    elif not re.search(r'"Today"\s*->\s*if \(bannerCallsOnly\)', fu):
        fails.append("[১] ফোনের \"Today\" ছাঁকনিতে `bannerCallsOnly` মানা হচ্ছে না — "
                     "ব্যানারে এক সংখ্যা, ভিতরে আরেক হবে")
    elif "it.nextFollow == today || it.recordDate == today" not in fu:
        fails.append("[১] ০৫.০৮.২০২৬-এর নিয়ম (আজকের নতুন Visit/Registration) মুছে গেছে")
    else:
        oks.append("[১] ফোনে — ব্যানার থেকে এলে শুধু আজকের কল, নইলে আগের নিয়ম ✅")

js = nocomment_js(read(WEB / "app.js"))
if js:
    if "callsOnly:true" not in js:
        fails.append("[১] ওয়েবে ব্যানার-তালিকায় `callsOnly` চিহ্ন বসানো হচ্ছে না")
    elif "if(f.callsOnly) return rows.filter" not in js:
        fails.append("[১] ওয়েবের Today ছাঁকনিতে `callsOnly` মানা হচ্ছে না")
    else:
        oks.append("[১] ওয়েবে — একই নিয়ম মানা হচ্ছে ✅")

# ══ ২) Performance তালিকা থেকে Back ══════════════════════════════════════
sp = nocomment_kt(read(KT / "modules/StaffProfileActivity.kt"))
if sp:
    if "private var perfListBack" not in sp:
        fails.append("[২] `perfListBack` নেই — তালিকা থেকে Back আবার আটকে যাবে")
    elif "perfListBack = { performanceOne(" not in sp:
        fails.append("[২] `performanceOne()` ফেরার ঠিকানা লিখে রাখছে না")
    elif "val prevBack = perfListBack ?: backAction" not in sp:
        fails.append("[২] `perfListScreen()` আবার \"তখন যা ছিল\" ধরে নিচ্ছে — "
                     "ডিটেল থেকে ফিরলে Back একই পর্দাই আঁকবে")
    else:
        oks.append("[২] Performance-এর তালিকা থেকে Back-এর ঠিকানা আগেই ঠিক করা ✅")

# ══ ৩ক) Trash Bin — ফোন ════════════════════════════════════════════════
tb = nocomment_kt(read(KT / "native/TrashBinActivity.kt"))
ta = nocomment_kt(read(KT / "native/TrashAdapter.kt"))
tc = read(KT / "native/TrashCardText.kt")
if tb and ta:
    need_tb = {
        "onView = { showRecord(it) }": "👁 View",
        "private fun showRecord(": "View পপ-আপ",
        "private fun runBulk(": "একসাথে Restore/Delete",
        "picked": "বাছার তালিকা",
        "binding.etSearch": "খোঁজার ঘর",
    }
    miss = [d for k, d in need_tb.items() if k not in tb]
    if miss:
        fails.append("[৩ক] Trash Bin (ফোন)-এ নেই: " + " · ".join(miss))
    else:
        oks.append("[৩ক] Trash Bin (ফোন) — View · খোঁজা · একসাথে বাছা সবই আছে ✅")

    if "TrashCardText" not in ta:
        fails.append("[৩ক] কার্ডে ডিটেলস বসানোর নিয়ম (`TrashCardText`) ব্যবহার হচ্ছে না")
    elif not tc:
        fails.append("[৩ক] `TrashCardText.kt` ফাইলটাই নেই")
    else:
        oks.append("[৩ক] কার্ড ও View — একই নিয়ম থেকে লেখা আসে (আলাদা হতে পারে না) ✅")

    # ⛔ "Sure?" পাহারা একসাথে মোছার সময়েও থাকতে হবে
    if "Delete these ${list.size} records forever?" not in tb:
        fails.append("[৩ক] একসাথে মোছার আগে \"Sure?\" পাহারা নেই — বিপজ্জনক")
    else:
        oks.append("[৩ক] একসাথে মোছার আগেও \"Sure?\" পাহারা আছে ✅")

lay_card = read(RES / "layout/item_trash_card.xml")
if lay_card:
    ids = set(re.findall(r'android:id="@\+id/(\w+)"', lay_card))
    want = {"btnView", "btnRestore", "btnDeleteForever", "cbPick", "rowButtons",
            "tvLine1", "tvLine2", "tvChipSrc"}
    if not want.issubset(ids):
        fails.append("[৩ক] কার্ডের লেআউটে নেই: " + ", ".join(sorted(want - ids)))
    else:
        oks.append("[৩ক] কার্ডের লেআউটে তিনটে বোতাম · টিক-ঘর · ডিটেলসের ঘর আছে ✅")
    # তিনটে বোতাম **পাশাপাশি** — অর্থাৎ প্রত্যেকের চওড়া 0dp + weight
    row = re.search(r'<LinearLayout[^>]*android:id="@\+id/rowButtons".*?</LinearLayout>', lay_card, re.S)
    if not row or row.group(0).count('android:layout_weight="1"') < 3:
        fails.append("[৩ক] Restore/Delete/View পাশাপাশি নেই (TK-এর স্পষ্ট নির্দেশ)")
    else:
        oks.append("[৩ক] View · Restore · Delete — তিনটেই পাশাপাশি ✅")
    # ⛔ নেভি ব্লু নিষিদ্ধ (TK বারবার বারণ করেছেন)
    navy = re.findall(r"(?i)(#0B2B59|#0F2748|#081A33|brand_navy|bg_btn_navy|clinic_primary)", lay_card)
    if navy:
        fails.append("[৩ক] কার্ডে নেভি ব্লু ফিরে এসেছে: " + ", ".join(sorted(set(navy))))
    else:
        oks.append("[৩ক] কার্ডে নেভি ব্লু কোথাও নেই ✅")

# ══ ৩খ) Trash Bin — ওয়েব ═══════════════════════════════════════════════
if js:
    need_js = {
        "function wlv1TrashCardHtml(": "ছোট কার্ড",
        "function wlv1TrashView(": "👁 View",
        "function wlv1TrashBulk(": "একসাথে Restore/Delete",
        "wlv1TrashToggleSelectMode": "Select মোড",
        "wlv1TrashSearch": "খোঁজার ঘর",
    }
    miss = [d for k, d in need_js.items() if k not in js]
    if miss:
        fails.append("[৩খ] Trash Bin (ওয়েব)-এ নেই: " + " · ".join(miss))
    else:
        oks.append("[৩খ] Trash Bin (ওয়েব) — View · খোঁজা · একসাথে বাছা সবই আছে ✅")

    # একসাথে করার সময় প্রতিটার জন্য আলাদা পপ-আপ/রি-লোড নয় (quiet)
    if "wlv1RestoreTrash(id, true)" not in js or "wlv1TrashDeleteForeverSafe(id, true)" not in js:
        fails.append("[৩খ] একসাথে করার সময় `quiet` ব্যবহার হচ্ছে না — প্রতিটার জন্য "
                     "আলাদা পপ-আপ ও পুরো Trash আবার নামানো হবে (Egress নষ্ট)")
    else:
        oks.append("[৩খ] একসাথে করার সময় প্রতিটার জন্য আলাদা পপ-আপ/ডাউনলোড হয় না ✅")

css = read(WEB / "styles.css")
if css:
    # 🔵 V584 (২৩.০৮.২০২৬) — ভুয়া অ্যালার্ম সারানো হলো।
    # আগে "V511" শব্দটার **শেষ উল্লেখের পরের সব CSS** খোঁজা হত। কিন্তু ওই
    # শব্দটা মন্তব্যেও থাকে, তাই ফাইলের শেষ দিকের অন্য পর্দার CSS-ও জালে
    # পড়ত — `.wlv1HistTtl` (V555, History-র শিরোনামের লেখা) আর `.wlv1FoldT`
    # (V574, ভাঁজের শিরোনামের লেখা) — দুটোই নেভি রঙের **লেখা**, Trash-এর
    # বোতাম নয়। ফলে পাহারাটা অনেক দিন ধরে অকারণে লাল হয়ে ছিল
    # (git-এ মিলিয়ে দেখা হয়েছে — V584-এর অনেক আগে থেকেই)।
    # এখন শুধু **Trash-এর নিজের ঘরগুলোই** (`.wlv1Trash…`) দেখা হয়, অর্থাৎ
    # নিয়মটা যা বোঝাতে চেয়েছিল ঠিক সেটাই। ⛔ নিয়ম শিথিল হয়নি — Trash-এ
    # নেভি ফিরলে আগের মতোই ধরা পড়বে (নিচে নমুনা দিয়ে যাচাই করা হয়েছে)।
    trash_css = "\n".join(l for l in css.split("\n") if "wlv1Trash" in l)
    navy = re.findall(r"(?i)(#0B2B59|#0F2748|#081A33)", trash_css)
    if navy:
        fails.append("[৩খ] ওয়েবের Trash কার্ডে নেভি ব্লু ফিরে এসেছে: " + ", ".join(sorted(set(navy))))
    elif ".wlv1TrashB.r{background:#16A34A}" not in css.replace(" ", ""):
        fails.append("[৩খ] ওয়েবে Restore সবুজ নয়")
    else:
        oks.append("[৩খ] ওয়েবেও View নীল · Restore সবুজ · Delete লাল, নেভি ব্লু নেই ✅")


# ══ ৪) Briefing-এর স্বয়ংক্রিয় নোটিশে 👁 View ════════════════════════════
ba = nocomment_kt(read(KT / "native/BriefingAdapter.kt"))
bact = nocomment_kt(read(KT / "native/BriefingActivity.kt"))
if ba:
    # ⛔ নম্বরের নিয়মে `\b` ফিরে এলে `+917099468221` আবার ধরা পড়বে না —
    #    View চিরকাল লুকিয়ে যাবে (ঠিক এই বাগটাই TK ২১.০৮.২০২৬-এ ধরেছেন)।
    m = re.search(r'Pattern\.compile\("([^"]+)"\)', ba)
    pat = m.group(1) if m else ""
    if not pat:
        fails.append("[৪] `MOBILE_PATTERN` খুঁজে পাওয়া গেল না")
    else:
        real = pat.replace("\\\\", "\\")
        try:
            rx = re.compile(real)
        except Exception as e:
            rx = None
            fails.append("[৪] `MOBILE_PATTERN` অচল: %s" % e)
        if rx is not None:
            cases = [
                (" - +917099468221 - Piles - Cooch Behar branch", "7099468221"),
                ("RAM - 7099468221 - Piles", "7099468221"),
                ("RAM - +91 7099468221 - Piles", "7099468221"),
                ("RAM - 917099468221 - Piles", "7099468221"),
                ("SOMA - Rs.5000 - CASH - Cooch Behar branch", None),
            ]
            bad = []
            for msg, exp in cases:
                mm = rx.search(msg)
                got = re.sub(r"[^0-9]", "", mm.group())[-10:] if mm else None
                if got != exp:
                    bad.append(msg.strip()[:28])
            if bad:
                fails.append("[৪] নম্বরের নিয়ম এই ধরনগুলোয় ভুল: " + " · ".join(bad) +
                             " — View আবার লুকিয়ে যাবে")
            else:
                oks.append("[৪] নম্বরের নিয়ম `+91` গায়ে-লাগানো সহ চারটে ধরনেই কাজ করে ✅")

    if "isAutoNotice(item)" not in ba:
        fails.append("[৪] স্বয়ংক্রিয় নোটিশে Reply এখনো দেখানো হচ্ছে (TK: \"রিপ্লাই কী দেব?\")")
    else:
        oks.append("[৪] স্বয়ংক্রিয় নোটিশে Reply নেই, View আছে ✅")

if bact:
    # ⛔ শুধু নাম আছে কিনা দেখলে হবে না — `openRecordForNumber()`-এর **ভিতরে**
    #    সত্যিই ডাকা হচ্ছে কিনা মেলানো হয় (একবার এই ফাঁকেই তামারি ফসকে গিয়েছিল)।
    mo = re.search(r"private fun openRecordForNumber\(.*?\n    \}", bact, re.S)
    obody = mo.group(0) if mo else ""
    if not obody:
        fails.append("[৪] `openRecordForNumber()` খুঁজে পাওয়া গেল না")
    else:
        need = ["switchBranchForNotice(item)", "focusCardMobile", "PaymentActivity"]
        miss = [n for n in need if n not in obody]
        if miss:
            fails.append("[৪] View-এর গন্তব্যে নেই: " + " · ".join(miss))
        else:
            oks.append("[৪] View → Enquiry/Visit হলে Follow-up, Advance হলে Payment; ব্রাঞ্চও বদলায় ✅")

if js:
    need = ["function wlv1NoticeView(", "function wlv1NoticeMobile(", "wlv1NoticeViewBtn"]
    miss = [n for n in need if n not in js]
    if miss:
        fails.append("[৪] ওয়েবে নেই: " + " · ".join(miss))
    else:
        oks.append("[৪] ওয়েবেও একই View ব্যবস্থা আছে ✅")


# ══ ৫) OUT TIME হারিয়ে যাওয়া + রাত ৯টায় মাস্টারের নোটিশ ═════════════════
wn = nocomment_kt(read(KT / "modules/WorkNotebookActivity.kt"))
if wn:
    need = {
        "saveDayCache()": "আজকের সারি ফোনে জমা রাখা",
        "loadDayCache()": "নেট না পেলে জমানো কপি দেখানো",
        "dayLoadFailed": "তথ্য না এলে ভুল অবস্থা না দেখানো",
    }
    miss = [d for k, d in need.items() if k not in wn]
    if miss:
        fails.append("[৫] Work Notebook-এ নেই: " + " · ".join(miss) +
                     " — নেট এক মুহূর্ত দুর্বল হলেই OUT TIME বোতাম আবার হারিয়ে যাবে")
    else:
        oks.append("[৫] নেট না পেলেও OUT TIME বোতাম হারায় না (জমানো কপি) ✅")

    # ⛔ ব্যর্থ অবস্থায় IN/OUT-এর `when` ব্লকে ঢোকাই যাবে না — নইলে ভুল
    #    অবস্থা দেখিয়ে স্টাফ আবার IN চেপে দিতে পারেন।
    mr = re.search(r"private fun render\(\) \{.*?\n        val isLeave", wn, re.S)
    rbody = mr.group(0) if mr else ""
    if not rbody or "if (dayLoadFailed) {" not in rbody or "return" not in rbody:
        fails.append("[৫] তথ্য না এলে `render()` এখনো IN/OUT অবস্থা আঁকছে — বিপজ্জনক")
    else:
        oks.append("[৫] তথ্য না এলে পরিষ্কার ভুল-বার্তা ও 🔄 Try again, ভুল বোতাম নয় ✅")

mw = nocomment_kt(read(KT / "native/MasterOutTimeWorker.kt"))
msch = nocomment_kt(read(KT / "native/MasterOutTimeScheduler.kt"))
app_kt = nocomment_kt(read(KT / "PilesClinicApplication.kt"))
if not mw or not msch:
    fails.append("[৫] রাত ৯টার মাস্টার-নোটিশের ফাইল নেই")
else:
    if 'user.role == "master"' not in mw:
        fails.append("[৫] মাস্টার-নোটিশ স্টাফ/ডাক্তারের ফোনেও চলবে — শুধু মাস্টার হওয়া উচিত")
    elif "if (missing.isNotEmpty())" not in mw:
        fails.append("[৫] কেউ বাকি না থাকলেও নোটিফিকেশন যাবে — অকারণ জ্বালাতন")
    elif "HOUR = 21" not in msch:
        fails.append("[৫] মাস্টার-নোটিশের সময় রাত ৯টা নয়")
    elif "MasterOutTimeScheduler.scheduleNext" not in app_kt:
        fails.append("[৫] মাস্টার-নোটিশের সময়সূচি অ্যাপ চালুর সময় বসানো হয় না — কখনো চলবে না")
    else:
        oks.append("[৫] রাত ৯টায় মাস্টারের নোটিশ — শুধু মাস্টার, বাকি থাকলে তবেই ✅")

print("🛡️ V511 পাহারা")
print("=" * 64)
for o in oks:
    print("  " + o)
if fails:
    print()
    for f in fails:
        print("  ❌ " + f)
    print()
    print("FAIL — V511-এর নিয়ম ভেঙেছে ❌")
    sys.exit(1)
print()
print("PASS — V511-এর সব নিয়ম কোডে বসানো আছে ✅")
