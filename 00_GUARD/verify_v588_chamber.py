#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🟢 V588 (২৩.০৮.২০২৬) — নতুন পাহারাদার।

TK-এর ছয়টা নির্দেশ (২৩.০৮.২০২৬, তিনটে ছবিসহ) যেন পরে কেউ ভুল করে ফিরিয়ে
না দেয়, তাই প্রতিটার জন্য আলাদা যাচাই — **ফোনে আর কম্পিউটারে দুই জায়গাতেই**:

  ১. চিকিৎসার কথা লেখার চারটে বাক্সেই ৯টা সাজেশন-চিপ (একটাই উৎস)
  ২. বোর্ডের সারিতে Patient ID-র জায়গায় তারিখ ও সময়
  ৩. বোর্ডের ক্রম — যিনি আগে এসেছেন তিনি আগে
  ৪. Review পর্দায় 📞 ও 🆔 চিহ্ন নেই, তারিখ ও সময় আছে
  ৫. ছাপা কাগজে "CASH ₹400" এক লাইনে, আর UPI নয় — ONLINE
  ৬. ছাপা কাগজে সারির/শিরোনামের ব্যাকগ্রাউন্ড রং নেই (প্রিন্টারের কালি বাঁচে)

⛔ এটা কিছু বদলায় না — শুধু পড়ে ও মিলিয়ে দেখে।
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KROOT = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main", "java", "com", "tkbiswas", "pilesclinic")
NAT = os.path.join(KROOT, "native")
PRT = os.path.join(KROOT, "print")
WEB = os.path.join(ROOT, "03_NETLIFY_READY")

fails = []
oks = []


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def check(name, cond):
    (oks if cond else fails).append(name)


QUICK = os.path.join(NAT, "TreatmentQuickNotes.kt")
ADAPTER = os.path.join(NAT, "ChamberAttendanceAdapter.kt")
ACT = os.path.join(NAT, "ChamberAttendanceActivity.kt")
REPO = os.path.join(NAT, "ChamberAttendanceRepository.kt")
CARD = os.path.join(NAT, "ReportCardActivity.kt")
PDF = os.path.join(PRT, "ChamberRegisterPdfBuilder.kt")
APPJS = os.path.join(WEB, "app.js")
CSS = os.path.join(WEB, "styles.css")

for p in (QUICK, ADAPTER, ACT, REPO, CARD, PDF, APPJS, CSS):
    if not os.path.exists(p):
        print("FAIL — ফাইল পাওয়া গেল না: " + p)
        sys.exit(1)

quick = read(QUICK)
adapter = read(ADAPTER)
act = read(ACT)
repo = read(REPO)
card = read(CARD)
pdf = read(PDF)
appjs = read(APPJS)
css = read(CSS)

# ---------------------------------------------------------------- ১ · সাজেশন
BN = re.findall(r'"([^"]+)"', quick.split("val QUICK_BN = listOf(")[1].split(")")[0])
ENHI = re.findall(r'"([^"]+)"', quick.split("val QUICK_EN_HI = listOf(")[1].split(")")[0])
check("১.১ ফোনে বাংলা সাজেশন ৯টা", len(BN) == 9)
check("১.২ ফোনে ইংরেজি/হিন্দি সাজেশন ৯টা", len(ENHI) == 9)
# চারটে বাক্সই একটাই উৎস থেকে নেয়
check("১.৩ চেম্বার বোর্ডের বাক্সে সাজেশন", act.count("TreatmentQuickNotes.attach(this, container, input)") >= 1)
check("১.৪ চেম্বার বন্ধের বাক্সেও সাজেশন (আগে ছিল না)",
      "TreatmentQuickNotes.attach(this, container, input)" in act and act.count("TreatmentQuickNotes.attach") >= 3)
check("১.৫ Review-র বাক্সেও সাজেশন (আগে ছিল না)", "TreatmentQuickNotes.attach(this, box, input)" in act)
check("১.৬ Report Card-এর বাক্সও একই উৎস থেকে", "TreatmentQuickNotes.attach(this, container, input)" in card)
check("১.৭ পুরনো নকল তালিকা আর কোথাও নেই",
      "val quickBn = listOf(" not in act and "val quickBn = listOf(" not in card)
# ওয়েবের তালিকা ফোনের সঙ্গে হুবহু এক
def js_list(name):
    blk = appjs.split("const %s = [" % name)[1].split("];")[0]
    out = []
    for raw in re.findall(r"'([^']*)'", blk):
        out.append(raw.encode().decode("unicode_escape"))
    return out
WBN = js_list("WLV1_TREAT_QUICK_BN")
WEN = js_list("WLV1_TREAT_QUICK_ENHI")
check("১.৮ ওয়েবের বাংলা তালিকা ফোনের হুবহু এক", WBN == BN)
check("১.৯ ওয়েবের ইংরেজি/হিন্দি তালিকা ফোনের হুবহু এক", WEN == ENHI)
check("১.১০ ওয়েবের বাক্সে চিপ বসে", "${wlv1TreatChipsHtml('cbTrIn')}" in appjs)
check("১.১১ চিপের রং/সাজ CSS-এ আছে", ".wlv1TrChip{" in css)
# ছাপার বাংলা→ইংরেজি তালিকা প্রতিটা চিপ চেনে (নইলে কাগজে বাংলা যাবে)
prt_en = read(os.path.join(PRT, "PrintTextEnglish.kt"))
check("১.১২ ৯টা চিপই ছাপার সময় ইংরেজি হয় (ফোন)", all(('"%s"' % c) in prt_en for c in BN))
# ওয়েবের বাংলা→ইংরেজি ছাপার তালিকা (WLV1_PRINT_EN) প্রতিটা চিপ চেনে কিনা
_pe_blk = appjs.split("const WLV1_PRINT_EN = [")[1].split("];")[0]
_pe_keys = [m.encode().decode("unicode_escape")
            for m in re.findall(r"\['([^']*)',", _pe_blk)]
check("১.১৩ ৯টা চিপই ছাপার সময় ইংরেজি হয় (ওয়েব)", all(c in _pe_keys for c in BN))

# ------------------------------------------------------- ২ · সারিতে তারিখ+সময়
check("২.১ বোর্ডের সারিতে আসার সময় বসে (ফোন)", "DateUtil.displayWithTime(row.arrivedAt" in adapter)
check("২.২ সরু ও চওড়া দুই লেআউটেই বসে (ফোন)", adapter.count("whenV.ifBlank { null }") == 2)
check("২.৩ সারিতে আর Patient ID দেখানো হয় না (ফোন)",
      "row.patientId.ifBlank { null }" not in adapter)
check("২.৪ Patient ID মোছা হয়নি — কপি করলে আগের মতোই যায়",
      'newPlainText("patient id", row.patientId)' in adapter)
check("২.৫ বোর্ডের সারিতে আসার সময় বসে (ওয়েব)", "const whenTxt = String(r.arrivedAt||'').trim() ? fmtDateTime(r.arrivedAt) : '';" in appjs)
check("২.৬ সারিতে আর Patient ID দেখানো হয় না (ওয়েব)", "const idLine = whenTxt ?" in appjs)

# ----------------------------------------------------------------- ৩ · ক্রম
check("৩.১ বোর্ড আসার সময় ধরে সাজে (ফোন)",
      ".thenBy { it.arrivedAt.ifBlank { \"9999\" } }" in repo)
check("৩.২ আগে 'এসেছেন', তারপর 'আসার কথা' (ফোন)",
      "compareByDescending<ChamberAttendanceRow> { it.arrived }" in repo)
check("৩.৩ বোর্ড আসার সময় ধরে সাজে (ওয়েব)",
      "if(a.arrived!==b.arrived) return a.arrived ? -1 : 1;" in appjs
      and "const ka = String(a.arrivedAt||'').trim() || '9999';" in appjs)
check("৩.৪ ছাপা কাগজ ও Review আগের মতোই আসার ক্রমে",
      act.count('sortedBy { it.arrivedAt.ifBlank { "9999" } }') == 2
      and "function wlv1ByArrivalOrder(rows){" in appjs)

# --------------------------------------------------------------- ৪ · Review
check("৪.১ Review-তে 📞 চিহ্ন নেই (ফোন)", '"📞 " + r.mobile' not in act)
check("৪.২ Review-তে 🆔 চিহ্ন নেই (ফোন)", '"🆔 " + r.patientId' not in act)
check("৪.৩ Review-তে তারিখ ও সময় আছে (ফোন)", "DateUtil.displayWithTime(r.arrivedAt" in act)
check("৪.৪ Review-তে 📞 চিহ্ন নেই (ওয়েব)", "&#128222; ${esc(shownMob(r.mobile))}" not in appjs)
check("৪.৫ Review-তে 🆔 চিহ্ন নেই (ওয়েব)", "&#127380; ${esc(r.patientId)}" not in appjs)
check("৪.৬ Review-তে তারিখ ও সময় আছে (ওয়েব)", "${esc(fmtDateTime(r.arrivedAt))}" in appjs)
check("৪.৭ ওয়েবের Review-র ঘরে চাপ কাজ করে (বন্ধনী ঠিক)",
      "wlv1ChamberPatientChoices('${esc(r.mobile)}','${esc(String(r.patientRowId||''))}')\">" in appjs
      and "'${esc(String(r.patientRowId||''))}'\"" not in appjs)

# ------------------------------------------------- ৫ · CASH ₹400 এক লাইনে
check("৫.১ ছাপায় উপায়+টাকা এক লাইনে (ফোন)",
      'val oneLine = fitText("$modeText $feeText", newOldPaint, colVisitW - 6f)' in pdf)
check("৫.২ পুরনো দুই-লাইনের ছাপা আর নেই (ফোন)",
      "drawMoney(canvas, r.fees, colVisit, colVisitW, rowTop + rowHeight / 2f + 9f, moneyPaint)" not in pdf)
check("৫.৩ অনলাইনের শব্দ এখন ONLINE (ফোন)",
      'if (r.feesCash > 0.0) "CASH" else if (r.feesOnline > 0.0) "ONLINE" else ""' in act)
check("৫.৪ ছাপায় উপায়+টাকা এক লাইনে (ওয়েব)",
      "? `${Number(r.feeOnline||0)>0 && !(Number(r.feeCash||0)>0) ? 'ONLINE' : 'CASH'} ${rupee(r.fee)}`" in appjs)
check("৫.৫ ওয়েবে ঘরটা এক লাইনেই থাকে", "white-space:nowrap" in css.split(".wlv1CbRegTable .cbVisit{")[1].split("}")[0])

# ------------------------------------------------- ৬ · ছাপায় ব্যাকগ্রাউন্ড রং নেই
check("৬.১ সারির রঙের ছোপ তুলে দেওয়া হয়েছে (ফোন)",
      "canvas.drawRect(RectF(colSl, rowTop, tableRight, rowBottom), fill)" not in pdf
      and 'color = Color.parseColor("#EAF7EE")' not in pdf
      and 'color = Color.parseColor("#EDEFF2")' not in pdf)
check("৬.২ শিরোনামের সবুজ ব্যান্ড তুলে দেওয়া হয়েছে (ফোন)",
      'color = Color.parseColor("#0F8A6E")' not in pdf
      and "canvas.drawRect(RectF(colSl, TABLE_TOP, tableRight, TABLE_TOP + HEADER_ROW_HEIGHT), headerBg)" not in pdf)
check("৬.৩ শিরোনামের লেখা এখনো স্পষ্ট (সবুজ কালি + দাগ)",
      "val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(GREEN)" in pdf
      and "canvas.drawLine(colSl, TABLE_TOP + HEADER_ROW_HEIGHT, tableRight, TABLE_TOP + HEADER_ROW_HEIGHT," in pdf)
check("৬.৪ সারির রঙের ছোপ তুলে দেওয়া হয়েছে (ওয়েব)",
      ".wlv1CbRegTable tbody tr.cbRowNew td{background:#fff}" in css
      and ".wlv1CbRegTable tbody tr.cbRowOld td{background:#fff}" in css)
check("৬.৫ শিরোনামের সবুজ ব্যান্ড তুলে দেওয়া হয়েছে (ওয়েব)",
      ".wlv1CbRegTable th{background:#fff;color:#0A5428" in css)
check("৬.৬ ঘরের রেখা আগের মতোই আছে (ছাপা পড়া যাবে)",
      "border:1px solid #B9C2CC" in css and "colTreat, rowTop, colTreat, rowBottom, gridPaint" in pdf)

# --------------------------------------------------------------------- ফল
print("🛡️ V588 পাহারাদার — চেম্বারের সারি · সাজেশন · ক্রম · ছাপা")
print("=" * 64)
for o in oks:
    print("   ✅ " + o)
for f in fails:
    print("   ❌ " + f)
print("-" * 64)
print("   পাশ: %d / %d" % (len(oks), len(oks) + len(fails)))
if fails:
    print("\nFAIL — উপরের ❌ ঘরগুলো ঠিক করতে হবে")
    sys.exit(1)
print("\nPASS — TK-এর ছয়টা নির্দেশই দুই অ্যাপে বসে আছে ✅")
