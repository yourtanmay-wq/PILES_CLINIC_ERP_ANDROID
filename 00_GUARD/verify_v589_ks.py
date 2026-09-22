#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🟢 V589 (২৩.০৮.২০২৬) — নতুন পাহারাদার।

TK-এর কথা (হুবহু, ছবিসহ):
  ১. *"একই Form-এ দুই জায়গাতে রয়েছে কতদিন সময় চাওয়া হইলো · দ্বিতীয় ফটোর
     সময়ের ওখানটা থাকবে না · সময় চাওয়া তো এক জায়গায় থাকবে · যেটা অটোমেটিক
     ফর্মে রয়েছে পাশাপাশি দুটো বক্স, ওটাই থাকবে"*
  ২. *"প্রথমত ইনজেকশন — ডাক্তার যেখানে চাইবে সেখানে অ্যানিমেশনটা দেখাবে"*
  ৩. *"ক্ষার সূত্র যেখানে থাকবে সেখানে বেঁধে রাখবে, যদিও পাইলসের মাংসের
     গোড়ায় বাঁধতে হয়"*
  ৪. *"মাংসটা যখন কেটে পড়ে যাবে তখন রিয়েল ফটোতেও যেন পরিষ্কার করে দেয়,
     যাতে পেশেন্ট সম্পূর্ণভাবে বুঝতে পারে যে তার পাইলসের মাংসটা বেরিয়ে গেছে"*

⛔ এটা কিছু বদলায় না — শুধু পড়ে ও মিলিয়ে দেখে।
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CLI = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                   "java", "com", "tkbiswas", "pilesclinic", "clinical")
NAT = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                   "java", "com", "tkbiswas", "pilesclinic", "native")
LAY = os.path.join(ROOT, "02_ANDROID_SOURCE_CODE", "PilesClinicApp", "app", "src", "main",
                   "res", "layout", "activity_doctor_checkup.xml")
APPJS = os.path.join(ROOT, "03_NETLIFY_READY", "app.js")

fails, oks = [], []


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def check(name, cond):
    (oks if cond else fails).append(name)


for p in (LAY, APPJS):
    if not os.path.exists(p):
        print("FAIL — ফাইল পাওয়া গেল না: " + p)
        sys.exit(1)

anim = read(os.path.join(CLI, "KsharSutraAnim.kt"))
view = read(os.path.join(CLI, "AnatomyView.kt"))
act = read(os.path.join(CLI, "DoctorCheckupActivity.kt"))
rep = read(os.path.join(CLI, "CheckupA4Report.kt"))
lang = read(os.path.join(CLI, "CheckupA4Lang.kt"))
lay = read(LAY)
js = read(APPJS)

# ───────────────────── ১ · সময়ের ঘর এক জায়গায় ─────────────────────
check("১.১ ভাগ ৪-এর Recovery Time ঘরটা পর্দা থেকে বাদ (ফোন)",
      'android:id="@+id/etRecoveryTime"' not in lay)
check("১.২ ভাগ ৩-এর দুটো বক্স আগের মতোই আছে (ফোন)",
      'android:id="@+id/etTimeAsked"' in lay and 'android:id="@+id/spTimeAskedUnit"' in lay)
check("১.৩ পুরনো লেখা মুছে যায় না — যা ছিল তাই ফিরে বসে (ফোন)",
      "recoveryTime = keptRecoveryTime," in act and "keptRecoveryTime = r.recoveryTime" in act)
check("১.৪ কাগজে এখন ভাগ ৩-এর সময়টাই ছাপে (ফোন)",
      "recovery = r.timeAsked.ifBlank { r.recoveryTime }," in act)
check("১.৫ পুরনো রেকর্ডের লেখাও পড়া যায় (ফোন)",
      'recovery = field("Time Asked", "Recovery")' in rep)
check("১.৬ কাগজের নাম মানের সাথে মেলে (ফোন)",
      '"recovery" to "Time Asked"' in lang and '"recovery" to "কতদিন সময় চাওয়া হল"' in lang)
check("১.৭ ভাগ ৪-এর ঘরটা ওয়েবেও বাদ", 'id="dnRecoveryTime"' not in js)
check("১.৮ পুরনো লেখা মুছে যায় না (ওয়েব)",
      "recoveryTime:wlv1KeptRecoveryTime," in js
      and "wlv1KeptRecoveryTime = String(note.recoveryTime||'');" in js)
check("১.৯ কাগজে ভাগ ৩-এর সময়, না থাকলে পুরনোটা (ওয়েব)",
      "recovery:(n.timeAsked||'')||(n.recoveryTime||'')," in js)
check("১.১০ কাগজের নাম দুই ভাষায় ঠিক (ওয়েব)",
      "recovery:'Time Asked'" in js and "recovery:'কতদিন সময় চাওয়া হল'" in js)
check("১.১১ মডেলের ঘরটা মোছা হয়নি (পুরনো তথ্য নিরাপদ)",
      "var recoveryTime: String" in read(os.path.join(CLI, "ClinicalModels.kt"))
      and '"recoveryTime" to r.recoveryTime' in read(os.path.join(CLI, "CheckupNoteJson.kt")))

# ───────────────────── ২ · ইনজেকশন যেখানে চাইবেন ─────────────────────
check("২.১ সুচের ডগা বেছে দেওয়া জায়গায় (ফোন)",
      "fun drawNeedle(canvas: Canvas, p: Paint, len: Float, wide: Float, prog: Float," in anim
      and "along: Float = 0.55f, across: Float = 0.10f" in anim
      and "val tipX = len * clampInjAlong(along); val tipY = wide * clampInjAcross(across)" in anim)
check("২.২ ছোঁয়ার জায়গা মাংসের নিজের মাপে বদলানো হয় (ফোন)",
      "private fun ksSetSpot(xPct: Double, yPct: Double): Boolean {" in view
      and "ksInjAlong = KsharSutraAnim.clampInjAlong((along / g.len).toFloat())" in view)
check("২.৩ ইনজেকশনের ধাপে ছুঁলে চিহ্ন বদলায় না (ফোন)",
      "if (!ksSetSpot(p[0].toDouble(), p[1].toDouble())) {" in view)
check("২.৪ সুচের ডগা বেছে দেওয়া জায়গায় (ওয়েব)",
      "function wlv1KsNeedle(ctx,L,LW,p,along,across){" in js
      and "wlv1KsNeedle(ctx,L,LW,t,WLV1_KS.injAlong,WLV1_KS.injAcross)" in js)
check("২.৫ ছোঁয়ার হিসাব ফোনের হুবহু (ওয়েব)",
      "function wlv1KsSetSpot(x,y){" in js
      and "WLV1_KS.injAlong=wlv1KsClampAlong(along/g.L);" in js)
check("২.৬ লেখাটা ডাক্তারকে বলে দেয় (দুই অ্যাপে)",
      "ছুঁয়ে দেখান কোথায়" in anim and "ছুঁয়ে দেখান কোথায়" in js)

# ───────────────────── ৩ · সুতো যেখানে চাইবেন ─────────────────────
check("৩.১ সুতোর জায়গা বেছে দেওয়া যায় (ফোন)",
      "at: Float = TIE_AT_BASE" in anim and "val d = len * clampTie(at)" in anim)
check("৩.২ ডিফল্টে গোড়াতেই (TK-এর চিকিৎসার নিয়ম)",
      "const val TIE_AT_BASE = 0.20f" in anim
      and "big.ksTieAt = KsharSutraAnim.TIE_AT_BASE" in act)
check("৩.৩ সীমার বাইরে যেতে পারে না (ফোন)",
      "fun clampTie(v: Float): Float = Math.max(0.06f, Math.min(0.78f, v))" in anim)
check("৩.৪ সুতোর জায়গা বেছে দেওয়া যায় (ওয়েব)",
      "function wlv1KsThread(ctx,L,LW,tight,at){" in js
      and "wlv1KsThread(ctx,L,LW,t,WLV1_KS.tieAt)" in js)
check("৩.৫ ডিফল্ট ও সীমা ফোনের হুবহু (ওয়েব)",
      "function wlv1KsClampTie(v){return Math.max(0.06,Math.min(0.78,v))}" in js
      and "WLV1_KS.injAlong=0.55; WLV1_KS.injAcross=0.10; WLV1_KS.tieAt=0.20;" in js)
check("৩.৬ নতুন মাংস বাছলে ডিফল্টে ফেরে (ফোন)",
      "big.ksInjAlong = 0.55f" in act and "big.ksInjAcross = 0.10f" in act)

# ─────────── ৪ · কেটে পড়লে আসল ছবিও পরিষ্কার ───────────
check("৪.১ আসল ছবির জায়গাটা ঢাকা পড়ে (ফোন)",
      "private fun ksHealLump(canvas: Canvas, m: AnatomyModel.Mark, t: Float) {" in view
      and "ksHealLump(canvas, m, ksT)" in view)
check("৪.২ পাশের ভালো চামড়ার আসল টুকরো বসে, শুধু রঙের ছোপ নয় (ফোন)",
      "canvas.drawBitmap(b, src, RectF(-r, -r, r, r), null)" in view
      and "PorterDuff.Mode.DST_IN" in view)
check("৪.৩ সবচেয়ে মসৃণ জায়গা বেছে নেওয়া হয় (ফোন)",
      "private fun ksDonor(" in view and "if (v < bestVar) { bestVar = v; best = Pair(dx, dy) }" in view)
check("৪.৪ রং পড়া না গেলেও ভাঙে না (ফোন)",
      "val donor = if (b != null) ksDonor(cxP, cyP, g, halfPct) else null" in view
      and "ksSkinColour(cxP, cyP, Math.max(g.len, g.wide) * 0.85)" in view)
check("৪.৫ নালী কেটে গেলেও জায়গাটা পরিষ্কার (ফোন)",
      "ksHealTract(canvas, pts, s, ksT)" in view)
check("৪.৬ আসল ছবি বদলায় না — শুধু উপরে আঁকা (ফোন)",
      "আসল ছবিটা এক পিক্সেলও বদলায় না" in view)
check("৪.৭ আসল ছবির জায়গাটা ঢাকা পড়ে (ওয়েব)",
      "function wlv1KsHealLump(ctx,w,h,m,t){" in js and "wlv1KsHealLump(ctx,w,h,m,t);" in js)
check("৪.৮ পাশের চামড়ার আসল টুকরো বসে (ওয়েব)",
      "function wlv1KsPatch(" in js and "oc.globalCompositeOperation='destination-in';" in js)
check("৪.৯ সবচেয়ে মসৃণ জায়গা বেছে নেওয়া হয় (ওয়েব)",
      "function wlv1KsDonor(cx,cy,g,halfPct){" in js and "if(v<bestVar){ bestVar=v; best=[dx,dy] }" in js)
check("৪.১০ নালী কেটে গেলেও জায়গাটা পরিষ্কার (ওয়েব)",
      "wlv1KsHealTract(ctx,w,h,pts,t);" in js)

# ─────────── ৫ · ওয়েবের পুরো পর্দার ফাঁকটা সারানো ───────────
check("৫.১ পুরো পর্দাতেও ক্ষারসূত্রের ধাপ আঁকা হয় (ওয়েব)",
      "wlv1KsPaintMark(ctxF,rw,rh,wlv1AnatState.marks[WLV1_KS.idx]);" in js)
check("৫.২ ছোট বোর্ডেও আগের মতোই",
      "wlv1KsPaintMark(ctx,w,h,wlv1AnatState.marks[WLV1_KS.idx]);" in js)

# ─────────── ৬ · কিছু সেভ হয় না, ছাপাতেও যায় না ───────────
check("৬.১ ক্ষারসূত্রের মোড কোনো দাগ যোগ করে না",
      "marks` তালিকা ছোঁয়া হয় না" in view or "marks তালিকা ছোঁয়া হয় না" in view)
check("৬.২ ছোঁয়ার জায়গা কোথাও সেভ হয় না",
      "ksInjAlong" not in read(os.path.join(CLI, "CheckupNoteJson.kt"))
      and "ksTieAt" not in read(os.path.join(CLI, "ClinicalModels.kt")))

# ─────────── ৭ · আঁকার সারি (টুলবার) ───────────
check("৭.১ 📍 সবার আগে, তারপর 〰️ দাগ (ফোন)",
      'Triple("pile",  "চিহ্ন",  AnatomyView.Tool.PILE),\n            Triple("tract", "নালী",   AnatomyView.Tool.TRACT),' in act)
check("৭.২ তীরের বোতাম সারিতে নেই (ফোন)",
      'Triple("arrow", "তীর",    AnatomyView.Tool.ARROW),' not in act)
check("৭.৩ তীর আঁকার কোড মোছা হয়নি — পুরোনো ছবি অক্ষত (ফোন)",
      "ARROW" in read(os.path.join(CLI, "AnatomyView.kt"))
      and "ARROW" in read(os.path.join(CLI, "AnatomyModel.kt")))
check("৭.৪ নিচের নির্দেশ-লেখা আর বসে না (ফোন)",
      "tip.visibility = android.view.View.GONE" in act and "tip.text = toolTip(chosen)" not in act)
check("৭.৫ 📍 সবার আগে, তারপর 〰️ দাগ (ওয়েব)",
      "var WLV1_ANAT_TOOLS=[['pile','চিহ্ন'],['tract','নালী'],['bulge','ফোলান']," in js)
check("৭.৬ তীরের বোতাম সারিতে নেই (ওয়েব)", "['arrow','তীর']" not in js)
check("৭.৭ তীর আঁকার কোড মোছা হয়নি (ওয়েব)", "'arrow'" in js)
check("৭.৮ নিচের নির্দেশ-লেখা আর বসে না (ওয়েব)",
      """id="'+(full?'dnAnatTipFull':'dnAnatTip')+'" style="display:none">""" in js)

print("🛡️ V589 পাহারাদার — সময়ের ঘর · ইনজেকশন · সুতো · আসল ছবি পরিষ্কার")
print("=" * 66)
for o in oks:
    print("   ✅ " + o)
for f in fails:
    print("   ❌ " + f)
print("-" * 66)
print("   পাশ: %d / %d" % (len(oks), len(oks) + len(fails)))
if fails:
    print("\nFAIL — উপরের ❌ ঘরগুলো ঠিক করতে হবে")
    sys.exit(1)
print("\nPASS — TK-এর চারটে নির্দেশই দুই অ্যাপে বসে আছে ✅")
