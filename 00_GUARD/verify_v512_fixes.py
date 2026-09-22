#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🛡️ V512 পাহারা (২১.০৮.২০২৬ — TK-এর "বাকি পুরোনো কাজগুলো ধরুন")

  ১) **Trash Bin নেট খারাপ হলে "Trash empty" দেখাবে না।** পড়া ব্যর্থ হলে
     `null` আসতেই হবে (`fetchTrashRawOrNull` → `fetchListOrNull`), জমানো
     কপির উপরে `"[]"` লেখা চলবে না, আর পর্দায় সৎ বার্তা বসবে।

  ২) **Note পপ-আপ থেকে Remark সংশোধন করা যাবে** — ফোনে ✏️ Edit বোতাম, যেটা
     আগের `editEnquiryHistoryNote()`-ই ডাকে (নতুন সেভের পথ নয়)। ওয়েবেও
     Timeline সারিতে একই ✏️ Edit, হুবহু একই নিয়মে সেভ।

  ৩) **ব্যানারের সংখ্যা = ভিতরের তালিকার সংখ্যা (ব্রাঞ্চ)।** V511-এ তারিখের
     নিয়ম মেলানো হয়েছিল; এখন ব্রাঞ্চের নিয়মও এক — দুই জায়গাতেই
     `BranchFilterStore` / `CrossBranchStaffAccess`।

  ৪) **Doctor Visit-এর তালিকা আর লাফ দেবে না** — "কতজন রেফার" সংখ্যাটাও
     ফোনে জমা থাকে, তাই প্রথম ও দ্বিতীয় আঁকা একরকম।

  ৫) **ভাষা-বোতামের নাম** — "বাংলা" → "Bengali" (আগে ভুল করে "English"
     হয়ে যেত, একই নামে দুটো বোতাম বসত)। ফোন ও ওয়েব দুই দিকেই। সঙ্গে
     Prescription পপ-আপ দুটোও বাংলা-বন্ধ স্টাফের জন্য ঢাকা পড়ল।

  ৬) **ওয়েবে OUT TIME আর হারাবে না** — supabase-js ভুল হলে throw করে না,
     তাই `r.error` স্পষ্ট করে দেখতে হবে; আজকের সারি ফোনেও জমা থাকবে; কিছু
     না পেলে ফাঁকা দিন নয়, সৎ বার্তা + কাজ করা 🔄 Try again।

  ৭) **ওয়েবে ব্যানারে চাপ দিলে তিন ভাগ এক পর্দায়** — আর ব্যানারের সংখ্যা
     ঠিক সেই একই তালিকা থেকেই (তারিখ-ছাঁকনি নিরপেক্ষ, ব্রাঞ্চ-গেটসহ)।

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


def nocomment_kt(s):
    """মন্তব্য বাদ — নইলে শুধু মন্তব্যে লিখে রাখলেই পাহারা ফাঁকি দেওয়া যেত।"""
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"^\s*//.*$", "", s, flags=re.M)


def nocomment_js(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"^\s*//.*$", "", s, flags=re.M)


def body_of(src, header, end_marker):
    """একটা ফাংশনের ভিতরটা — নামটা কোথাও লেখা আছে কিনা নয়, ভিতরে **কাজটা**
    আছে কিনা সেটাই মেলানোর জন্য।"""
    i = src.find(header)
    if i < 0:
        return ""
    j = src.find(end_marker, i + len(header))
    return src[i:j if j > 0 else len(src)]


# ══ ১) Trash Bin — নেট খারাপ হলে ফাঁকা নয় ════════════════════════════════
trepo = nocomment_kt(read(KT / "native/TrashRepository.kt"))
if trepo:
    fn = body_of(trepo, "fun fetchTrashRawOrNull(", "\n    fun ")
    if not fn:
        fails.append("[১] `fetchTrashRawOrNull()` নেই — ব্যর্থ পড়া আর ফাঁকা তালিকা "
                     "আলাদা করা যাবে না")
    elif "SupabaseClient.fetchListOrNullDirect(" not in fn:
        fails.append("[১] `fetchTrashRawOrNull()` ভিতরে `fetchListOrNullDirect` ব্যবহার করছে না। "
                     "সাধারণ `fetchList` হলে ব্যর্থ পড়াতেও ফাঁকা তালিকা ফিরবে (আগের বিপদ), "
                     "আর `fetchListOrNull` হলে ছবিসহ বিশাল trash সারি CloudReadDedupe ভরে "
                     "অন্য পর্দার জমানো উত্তর ছিটকে দেবে ⇒ Egress বাড়বে")
    elif 'order = "deletedAt.desc.nullslast"' not in fn:
        fails.append("[১] V509-এর প্রমাণিত সাজানোর নিয়ম (`deletedAt`) হারিয়ে গেছে — "
                     "`updatedAt` ঘরটা `trash` টেবিলে নেই, অনুরোধ বাতিল হবে")
    else:
        oks.append("[১] `fetchTrashRawOrNull()` ঠিক আছে ✅")

tact = nocomment_kt(read(KT / "native/TrashBinActivity.kt"))
if tact:
    if "repository.fetchTrashRawOrNull()" not in tact:
        fails.append("[১] TrashBinActivity এখনো `fetchTrashRawOrNull()` ডাকছে না")
    elif "repository.fetchTrashRaw()" in tact:
        fails.append("[১] TrashBinActivity এখনো পুরোনো `fetchTrashRaw()` ডাকছে — "
                     "নেট খারাপ হলে আবার 'Trash empty' দেখাবে")
    elif re.search(r'binding\.tvEmpty\.text\s*=\s*"Trash empty"\s*\n\s*binding\.tvEmpty\.visibility\s*=\s*View\.VISIBLE\s*\n\s*\}\s*\n\s*return@launch', tact):
        fails.append("[১] পড়া ব্যর্থ হলে এখনো 'Trash empty' লেখা হচ্ছে — "
                     "সত্যি কী আছে সেটা জানা নেই, তাই এই বার্তা মিথ্যে")
    elif "Could not load Trash" not in tact:
        fails.append("[১] পড়া ব্যর্থ হলে সৎ বার্তা ('Could not load Trash …') বসছে না")
    else:
        oks.append("[১] নেট খারাপ হলে জমানো তালিকা থাকে, ভুল বার্তা ওঠে না ✅")

    swipe = body_of(tact, "binding.swipeRefresh.setOnRefreshListener {", "binding.btnBack")
    if not swipe:
        fails.append("[১] Trash Bin-এ টেনে-রিফ্রেশ করার ব্যবস্থাটাই নেই")
    elif "CloudReadCache.clear()" in swipe or "CloudReadDedupe.clear()" in swipe:
        fails.append("[১] টেনে-রিফ্রেশে অ্যাপ-জুড়ে জমানো উত্তর মোছা হচ্ছে — এতে **অন্য সব "
                     "পর্দার** তথ্যও আবার নামবে ⇒ Egress বাড়বে (V509-এর কাজ নষ্ট)। "
                     "এই পর্দার পড়া এমনিতেই dedupe-এর বাইরে দিয়ে যায়, তাই দরকার নেই")
    else:
        oks.append("[১] টেনে-রিফ্রেশ সরাসরি সার্ভার থেকে আনে, বাকি পর্দার ক্যাশ ছোঁয় না ✅")

    # 🔒 ব্যর্থ পড়ার পরে Restore/Delete Forever অবশ্যই আটকাতে হবে —
    #    নইলে ইতিমধ্যে চিরতরে মোছা সারি Restore করলে রেকর্ড **ফিরে আসত**।
    if "private var cloudVerified" not in tact:
        fails.append("[১] `cloudVerified` পাহারাটাই নেই — জমানো তালিকা থেকে Restore চাপলে "
                     "চিরতরে মোছা রেকর্ড আবার ফিরে আসতে পারে")
    elif tact.count("if (blockedUnverified()) return") < 4:
        fails.append("[১] Restore · Delete Forever · একসাথে Restore · একসাথে Delete — "
                     "চারটে জায়গাতেই `blockedUnverified()` পাহারা লাগবে")
    elif "cloudVerified = true" not in tact:
        fails.append("[১] সফল পড়ার পরেও `cloudVerified` চালু হচ্ছে না — Restore/Delete "
                     "চিরকাল আটকে থাকবে")
    elif "cloudVerified = false" not in body_of(
            tact, "if (rawRows == null) {", "return@launch"):
        # ⛔ শুধু ঘোষণার লাইনটা (`private var cloudVerified = false`) থাকলেই হবে না —
        #    **পড়া ব্যর্থ হওয়ার ঠিক ঐ জায়গাতেই** আবার বন্ধ করতে হবে, নইলে একবার
        #    সফল হওয়ার পরে নেট চলে গেলেও তালা খোলাই থেকে যেত।
        fails.append("[১] পড়া ব্যর্থ হলে `cloudVerified` আবার বন্ধ করা হচ্ছে না — "
                     "একবার সফল হওয়ার পরে নেট গেলেও Restore/Delete খোলা থাকবে")
    elif "picked.clear()" not in body_of(tact, "if (rawRows == null) {", "return@launch"):
        fails.append("[১] পড়া ব্যর্থ হলে আগের বাছাগুলো ছাড়া হচ্ছে না — "
                     "'৩টা বাছা' লেখা থেকে যাবে অথচ সারিগুলো সত্যি আছে কিনা জানা নেই")
    else:
        oks.append("[১] ক্লাউড যাচাই না হলে Restore / Delete Forever আটকে থাকে ✅")

# ══ ২) Note পপ-আপে ✏️ Edit ═══════════════════════════════════════════════
tl = nocomment_kt(read(KT / "native/PatientTimelineActivity.kt"))
if tl:
    if "onEdit: (() -> Unit)? = null" not in tl:
        fails.append("[২] `showNoteCardsDialog()`-এ `onEdit` প্যারামিটারই নেই")
    elif "showNoteCardsDialog(fullNote, e.date, e.title, noteEditAction)" not in tl:
        fails.append("[২] Note পপ-আপ খোলার সময় সংশোধনের কাজটা পাঠানো হচ্ছে না")
    elif not re.search(r"val canEditNote\s*=\s*e\.paymentId == null", tl):
        fails.append("[২] সংশোধনের শর্তে `e.paymentId == null` নেই — পেমেন্টের সারিতেও "
                     "ভুল সম্পাদক খুলে যেতে পারে (পেমেন্টের নিজস্ব সম্পাদক আছে)")
    elif "editEnquiryHistoryNote(e)" not in tl:
        fails.append("[২] সংশোধনের কাজটা পুরোনো `editEnquiryHistoryNote()` ডাকছে না — "
                     "নতুন সেভের পথ তৈরি করা চলবে না")
    elif 'setNeutralButton("✏️ Edit")' not in tl:
        fails.append("[২] পপ-আপে ✏️ Edit বোতামটাই বসছে না")
    elif "TripleTapEdit.attach(row) { editEnquiryHistoryNote(e) }" not in tl:
        fails.append("[২] আগের ৩-ট্যাপের ব্যবস্থাটা মুছে গেছে — পুরোনো পথ বন্ধ করা চলবে না")
    else:
        oks.append("[২] ফোনে — Note পপ-আপে ✏️ Edit, কাজ করে পুরোনো ফাংশনই ✅")

    # প্রেসক্রিপশন "শুধু দেখার" (V327) — সেখানে Edit যেন না ঢোকে
    rx = body_of(tl, "private fun showPrescriptionDetailsDialog(", "\n    private fun ")
    if rx and "setNeutralButton" in rx:
        fails.append("[২] Prescription Details পপ-আপে সম্পাদনার বোতাম ঢুকেছে — "
                     "V327-এ ওটা 'শুধু দেখার' বলে লক করা আছে")
    elif rx:
        oks.append("[২] Prescription এখনো শুধু-দেখার ✅")

js = nocomment_js(read(WEB / "app.js"))
if js:
    if "_fid:x.id" not in js or "_hidx:hi" not in js or "_eid:e.id" not in js:
        fails.append("[২] ওয়েবের Timeline সারিতে চেনার চাবি (`_eid`/`_fid`/`_hidx`) বসছে না")
    elif "function wlv1SaveTlNote(" not in js:
        fails.append("[২] ওয়েবে সংশোধন সেভ করার ফাংশনই নেই")
    else:
        sv = body_of(js, "function wlv1SaveTlNote(", "\nwindow[\"wlv1SaveTlNote\"]")
        if "upd('enquiries'" not in sv or "upd('followups'" not in sv:
            fails.append("[২] ওয়েবের সেভ অ্যাপের নিজস্ব `upd()` পথে হচ্ছে না। হাতে `save()` "
                         "করলে `updatedAt` পুরোনোই থাকে, আর `mergeForCloudPush()` ক্লাউডের "
                         "পুরোনো লেখাটাকেই জিতিয়ে দেয় — সংশোধন নিঃশব্দে ফিরে যায়")
        elif "if(k===hist.length-1)patch.lastRemark=text" not in sv:
            fails.append("[২] ওয়েবে `lastRemark` শুধু শেষ রিমার্কের বেলায় বদলানোর নিয়ম নেই — "
                         "ফোনের অ্যাপের নিয়মের সাথে মিলছে না")
        elif "wlv1EditTlNote(" not in js:
            fails.append("[২] ওয়েবের সারিতে ✏️ Edit বোতামটাই নেই")
        else:
            oks.append("[২] ওয়েবে — একই নিয়মে Timeline Note সংশোধন ✅")

# ══ ৩) ব্যানারের ব্রাঞ্চ = তালিকার ব্রাঞ্চ ═══════════════════════════════
dash = nocomment_kt(read(KT / "native/DashboardActivity.kt"))
if dash:
    banner = body_of(dash, "private fun refreshCallBanner(", "\n    /**")
    if not banner:
        fails.append("[৩] `refreshCallBanner()` খুঁজে পাওয়া গেল না")
    elif "val bannerBranch" not in banner:
        fails.append("[৩] ব্যানার এখনো নিজের আলাদা ব্রাঞ্চ-নিয়মে গুনছে")
    elif "BranchFilterStore.get(" not in banner:
        fails.append("[৩] মাস্টারের বেছে রাখা ব্রাঞ্চ (`BranchFilterStore`) মানা হচ্ছে না — "
                     "ব্যানারে পাঁচ ব্রাঞ্চের সংখ্যা, ভিতরে এক ব্রাঞ্চের নাম")
    elif "CrossBranchStaffAccess.effectiveViewBranch(session)" not in banner:
        fails.append("[৩] JPE-CRP-এর অতিরিক্ত ব্রাঞ্চ (V453) ব্যানারে ধরা হচ্ছে না — "
                     "ভিতরে বেশি নাম দেখাবে")
    elif "repo.fetchTab(stage, bannerBranch," not in banner:
        fails.append("[৩] ক্লাউড-গোনা এখনো `bannerBranch` ধরে হচ্ছে না")
    elif "repo.loadCachedTab(stage, bannerBranch)" not in banner:
        fails.append("[৩] জমানো-কপির গোনা এখনো `bannerBranch` ধরে হচ্ছে না")
    elif re.search(r"(fetchTab|loadCachedTab)\(stage,\s*session\.branch", banner):
        fails.append("[৩] গোনা এখনো `session.branch` ধরে হচ্ছে — মাস্টারের ওটা "
                     "আক্ষরিক 'All', তাই সংখ্যা আবার মিলবে না")
    elif "session.branch" in banner.replace(
            "} catch (_: Throwable) { session.branch }", ""):
        # ⛔ একমাত্র অনুমোদিত ব্যবহার: উপরের হিসাব কোনো কারণে ভেঙে গেলে স্টাফ যেন
        #    অন্তত নিজের ব্রাঞ্চের সংখ্যাই দেখেন (আগের আচরণ)। এর বাইরে কোথাও
        #    `session.branch` থাকলে পুরোনো গোলমাল ফিরে আসার ঝুঁকি।
        fails.append("[৩] ব্যানারে অন্য কোথাও এখনো `session.branch` ব্যবহার হচ্ছে")
    elif '"dashbanner|" + session.mobile + "|" + bannerBranch' not in banner:
        fails.append("[৩] ফাঁক-হিসাবের চাবিতে `bannerBranch` নেই — ব্রাঞ্চ বদলালে "
                     "পুরোনো সংখ্যাই বসে থাকবে")
    else:
        oks.append("[৩] ব্যানার ও ভিতরের তালিকা — একই ব্রাঞ্চ-নিয়ম ✅")

# ══ ৪) Doctor Visit — লাফ (flicker) ══════════════════════════════════════
dv = nocomment_kt(read(KT / "native/DoctorVisitActivity.kt"))
if dv:
    lc = body_of(dv, "private fun loadCachedDoctors(", "\n    private fun saveCachedDoctors(")
    if "private fun dvCountsKey(" not in dv:
        fails.append("[৪] 'কতজন রেফার' সংখ্যা জমা রাখার চাবিই নেই")
    elif "copy(referredCount = counts.optInt(" not in lc:
        fails.append("[৪] জমানো তালিকা দেখানোর সময় সংখ্যাটা বসানো হচ্ছে না — "
                     "প্রথম আঁকায় ০ থেকে যাবে, তারপর লাইন যোগ হয়ে কার্ড লাফাবে")
    elif "saveCachedCounts(itemsWithCounts, countsKeyAtStart)" not in dv:
        fails.append("[৪] ক্লাউড থেকে গোনা হওয়ার পরে সংখ্যাগুলো জমা রাখা হচ্ছে না "
                     "(অথবা কোন ব্রাঞ্চের জন্য গোনা হয়েছিল সেই চাবিটা পাঠানো হচ্ছে না)")
    elif "if (items.none { it.referredCount > 0 }) return" not in dv:
        fails.append("[৪] সব সংখ্যা ০ হলেও জমা হচ্ছে — নেট খারাপ হলে গোনার ধাপটা চুপচাপ "
                     "ফাঁকা ফেরায়, তখন ভুল ০ জমা হয়ে পরের বার ভুল দেখাবে")
    elif "if (keyAtStart != dvCountsKey()) return" not in dv:
        fails.append("[৪] মাঝপথে ব্রাঞ্চ বদলে গেলে আগের ব্রাঞ্চের সংখ্যা নতুন ব্রাঞ্চের ঘরে "
                     "বসে যাবে — `keyAtStart` মেলানো হচ্ছে না")
    # 🔵 V584 (২৩.০৮.২০২৬) — নামটা হালনাগাদ। V576-এর egress-কাজে এই স্ক্রিনের
    # ডাক `fetchListRawOrNull()` → `fetchListRawSmartOrNull()` হয়েছিল (probe →
    # unchanged / delta / full)। নিয়মটা **এক অক্ষরও শিথিল হয়নি**: নতুন
    # ফাংশনের প্রতিটা ব্যর্থ-পথও `full()` = `SupabaseClient.fetchListOrNull()`
    # ডাকে, অর্থাৎ নেট খারাপ হলে আগের মতোই `null` ফেরে — কখনো চুপচাপ ফাঁকা নয়
    # (DoctorVisitRepository.kt-এ মিলিয়ে দেখা হয়েছে)। পাহারাটা শুধু পুরোনো
    # নামটাই খুঁজছিল বলে V576-এর পর থেকে অকারণে লাল হয়ে ছিল।
    elif not any(f in dv for f in ("fetchListRawOrNull(", "fetchListRawSmartOrNull(")):
        fails.append("[৪] Doctor Visit-এর ব্যর্থ-পড়া পাহারা (`fetchListRaw…OrNull`) মুছে গেছে")
    else:
        oks.append("[৪] Doctor Visit-এর তালিকা দুইবারেই একরকম দেখাবে ✅")

# ══ ৫) ভাষার নাম ও বাকি বাংলা ════════════════════════════════════════════
nb = read(KT / "native/NoBengali.kt")          # ⚠️ এখানে মন্তব্য রাখাই দরকার নেই,
nbc = nocomment_kt(nb)                          #    তাই কোড-অংশেই মেলানো হয়
if nbc:
    if '"বাংলা" to "English"' in nbc:
        fails.append("[৫] ফোনে এখনো `\"বাংলা\" to \"English\"` আছে — ভাষা বাছার পর্দায় "
                     "একই নামে দুটো বোতাম বসবে (Bengali-র বোতামে লেখা থাকবে English)")
    elif nbc.count('"বাংলা" to "Bengali"') < 2:
        fails.append("[৫] WHOLE ও MAP — দুই জায়গাতেই `\"বাংলা\" to \"Bengali\"` থাকতে হবে")
    elif '"বাংলা  (Bengali)" to "Bengali"' not in nbc:
        fails.append("[৫] `বাংলা  (Bengali)` হুবহু-মিলের সারি নেই — লেখা হবে "
                     "'Bengali  (Bengali)'; ওয়েবে ঐ সারি আগে থেকেই আছে")
    elif '"Prescription যাচাই করা যায়নি" to' not in nbc:
        fails.append("[৫] Prescription পপ-আপের লেখাগুলোর ইংরেজি যোগ করা হয়নি — "
                     "বাংলা-বন্ধ স্টাফের পর্দায় অক্ষর মুছে গিয়ে ভাঙা লেখা উঠত")
    else:
        oks.append("[৫] ফোনে — ভাষার নাম ঠিক, Prescription-এর লেখাও ঢাকা পড়ল ✅")

rxa = nocomment_kt(read(KT / "clinical/PrescriptionActivity.kt"))
if rxa:
    if rxa.count("NoBengali.installDialog(") < 2:
        fails.append("[৫] Prescription-এর দুটো পপ-আপে `NoBengali.installDialog()` বসেনি — "
                     "ঐ পপ-আপ PremiumAlert দিয়ে আঁকা হয় না, তাই নিজে থেকে ঢাকা পড়ে না")
    else:
        oks.append("[৫] Prescription-এর দুটো পপ-আপ বাংলা-বন্ধে ঢাকা ✅")

if js:
    if "'বাংলা':'English'" in js:
        fails.append("[৫] ওয়েবে এখনো `'বাংলা':'English'` আছে — publicEdu পাতায় "
                     "পরপর দুটো ভাগেরই নাম English হয়ে যাবে")
    elif "'বাংলা':'Bengali'" not in js:
        fails.append("[৫] ওয়েবের হুবহু-মিলের তালিকায় `'বাংলা':'Bengali'` নেই")
    else:
        oks.append("[৫] ওয়েবে — ভাষার নাম ফোনের সাথে এক ✅")

# ══ ৬) ওয়েবে OUT TIME-এর জমানো কপি ═══════════════════════════════════════
nb = nocomment_js(read(WEB / "notebook.js"))
if nb:
    ld = body_of(nb, "async function loadDay(date)", "\n  function workEntriesSummary")
    if not ld:
        fails.append("[৬] `loadDay()` খুঁজে পাওয়া গেল না")
    elif "r.error" not in ld:
        fails.append("[৬] `loadDay()` supabase-এর `error` দেখে না — supabase-js ভুল হলে "
                     "throw করে না, `{data:null,error:{...}}` ফেরায়; তাই `catch` কখনো চলত "
                     "না আর OUT TIME বোতাম হারিয়ে যেত")
    elif "nbSaveDayCache(row)" not in ld:
        fails.append("[৬] ক্লাউড থেকে আসা আজকের সারিটা ফোনে জমা রাখা হচ্ছে না — "
                     "পরের বার নেট না পেলে দেখানোর কিছুই থাকবে না")
    elif "nbLoadDayCache(code, date)" not in ld:
        fails.append("[৬] জমানো কপি স্টাফের কোড ধরে খোঁজা হচ্ছে না — একই কম্পিউটারে "
                     "একজনের পরে আরেকজন লগইন করলে আগেরজনের IN/OUT TIME দেখাবে")
    elif "nbHasPendingWrite(cached.id)" not in ld:
        fails.append("[৬] ক্লাউডে সারি নেই অথচ জমানো কপি দেখানো হচ্ছে **প্রমাণ ছাড়াই** — "
                     "ইচ্ছে করে মুছে ফেলা সারি এতে আবার ফিরে আসতে পারে; সেভ এখনো "
                     "অপেক্ষায় আছে কিনা (`MOD.queueWrite`) সেটা দেখে নিতে হবে")
    elif "nbDayFromCache = true" not in ld or "nbDayLoadFailed = true" not in ld:
        fails.append("[৬] জমানো-কপি / পাওয়া-যায়নি — দুটো অবস্থা আলাদা করে চিহ্নিত হচ্ছে না")
    else:
        oks.append("[৬] ওয়েবে — OUT TIME আর হারাবে না, আর অন্য স্টাফের কপি কখনো নয় ✅")

    if "function nbCacheOk(" not in nb:
        fails.append("[৬] জমানো কপির ভিতরের `staff_code`/`work_date` মিলিয়ে দেখা হচ্ছে না")
    elif "nbSaveDayCache(d, true)" not in nb:
        fails.append("[৬] `nbSaveDay()` এখনো পুরোনো চাবিতে লিখছে (কোড ছাড়া) — "
                     "অথবা `force` ছাড়া, ফলে নিজের নতুন কপিই বসত না")
    elif "nbDayLoadFailed ?" not in nb:
        fails.append("[৬] তথ্য আনা না গেলে পর্দায় সৎ সতর্কবার্তা বসছে না")
    elif "workNotebook()" not in nb.split("nbDayLoadFailed ?")[1][:600]:
        fails.append("[৬] সতর্কবার্তার 🔄 Try again কাজ করে না — `renderToday` window-এ "
                     "expose করা নেই, `workNotebook()` ডাকতে হবে")
    else:
        oks.append("[৬] ওয়েবে — না পেলে সৎ সতর্কবার্তা, তবু হাজিরা দেওয়া আটকায় না ✅")

# ══ ৭) ওয়েবে ব্যানারে চাপ দিলে তিন ভাগ একসাথে ════════════════════════════
if js:
    if "function wlv1TodayCallRows(" not in js:
        fails.append("[৭] `wlv1TodayCallRows()` নেই — গোনা আর তালিকা আলাদা উৎস থেকেই থাকবে")
    else:
        tcr = body_of(js, "function wlv1TodayCallRows(", '\nwindow["wlv1TodayCallRows"]')
        if "__followDateFilter={mode:'',from:'',to:''}" not in tcr or "finally" not in tcr:
            fails.append("[৭] গোনার সময় আগের তারিখ-ছাঁকনি সরিয়ে (ও শেষে হুবহু ফেরত দিয়ে) "
                         "হিসাব হচ্ছে না — কেউ 'Overdue' বেছে রাখলে ব্যানার ০ দেখাত")
        elif "wlv1BranchGate(rows)" not in tcr:
            fails.append("[৭] গোনায় মাস্টারের বেছে রাখা ব্রাঞ্চের গেট বসছে না")
        elif "g===null?rows:g" not in tcr:
            fails.append("[৭] ব্রাঞ্চ একবারও না বাছলে (গেট `null`) আগের মতো সব ব্রাঞ্চ গোনার "
                         "নিয়মটা নেই — পুরোনো আচরণ হারিয়ে যাবে")
        elif tcr.count("try{") < 2 or "}catch(e){" not in tcr:
            fails.append("[৭] প্রতিটা ভাগের **ভিতরে** আলাদা পাহারা নেই — একটা ভাগে একটামাত্র "
                         "গোলমেলে সারি থাকলেই পুরো গোনা ০ হয়ে ব্যানারই উঠত না")
        elif "wlv1TodayCallRows().length" not in js:
            fails.append("[৭] ব্যানারের সংখ্যা এখনো ঐ একই উৎস থেকে আসছে না")
        elif "all=wlv1TodayCallRows();" not in js:
            fails.append("[৭] মিশ্র পর্দার তালিকা ঐ একই উৎস থেকে আসছে না — গোনা ও তালিকা "
                         "আলাদা হিসাবে চললে সংখ্যা আবার মিলবে না")
        elif "__wlv1FuAllSections ? wlv1TodayCallRows() : followStats(tab).rows" not in js:
            fails.append("[৭] ৩০ সেকেন্ডের নিজে-নিজে রিফ্রেশ (`wlv1FuRedraw`) মিশ্র-মোড মানছে না — "
                         "কিছুক্ষণ পরেই তালিকা এক ভাগে নেমে যেত")
        elif "__wlv1FuAllSections = true" not in js:
            fails.append("[৭] ব্যানারে চাপ দিলে তিন ভাগ একসাথে দেখানোর চিহ্নটা বসছে না")
        elif "function wlv1FuTab(tab){ __wlv1FuAllSections=false; followup(tab); }" not in js:
            fails.append("[৭] ট্যাবে চাপলে মিশ্র-মোড নেভানোর আলাদা পথ (`wlv1FuTab`) নেই")
        elif "function followup(tab='Inquiry'){__wlv1FuAllSections=false;" in js:
            fails.append("[৭] `followup()`-এর ভিতরেই চিহ্নটা নেভানো হচ্ছে — একটা কল লগ করলেই "
                         "`updateFollowAction()` ওটাই ডাকে, তাই প্রথম কলের পরেই বাকি রোগীরা "
                         "পর্দা থেকে হারিয়ে যেতেন")
        elif js.count('onclick="wlv1FuTab(') < 2:
            # ⛔ দুটো জায়গাতেই (`tabs` ও পর্দায় বসা `tabs2`) — একটাতে থাকলেই যথেষ্ট নয়,
            #    কারণ পর্দায় আসলে `tabs2`-টাই বসে।
            fails.append("[৭] ট্যাবের বোতাম (দুই জায়গার একটা) এখনো `followup()` ডাকছে — "
                         "মিশ্র-মোড নিভবে না, এক ভাগ দেখতে চাইলেও তিন ভাগই দেখাবে")
        elif "__wlv1FuAllSections=false" not in body_of(
                js, "function wlv1FollowFilter(stage, mode){", '\nwindow["wlv1FollowFilter"]'):
            fails.append("[৭] ছাঁকনির বোতামে মিশ্র-মোড নেভানো হচ্ছে না")
        elif "resetFollowDateFilter();__wlv1FuAllSections=false;" not in js:
            fails.append("[৭] Follow-up ছেড়ে অন্য পাতায় গেলে চিহ্নটা নিভছে না — ফিরে এসে "
                         "খোঁজার ঘরে টাইপ করলে ছাঁকনি-ছাড়া তিন ভাগের পুরো তালিকা বসে যেত")
        elif "__wlv1FuAllSections?'__ALL__':tab" not in js:
            fails.append("[৭] মিশ্র পর্দায় ⬇ Sheet এখনো শুধু এক ভাগ নামাবে")
        else:
            oks.append("[৭] ওয়েবে — ব্যানারের সংখ্যা = পর্দার তালিকা, তিন ভাগ একসাথে ✅")

# ══ ফল ═══════════════════════════════════════════════════════════════════
for o in oks:
    print("  " + o)
if fails:
    print("\nFAIL — V512:")
    for f in fails:
        print("  ✗ " + f)
    sys.exit(1)
print("\nPASS — V512-এর সাতটা সংশোধনই কোডে আছে ✅")
