package com.tkbiswas.pilesclinic.clinical

import com.tkbiswas.pilesclinic.print.BranchCatalog

/**
 * 💰🔒 V971 (০২.০৯.২০২৬, TK-এর PDF নমুনা ও পাশ-করা প্রুফ অনুযায়ী) —
 * **এস্টিমেটের A4 কাগজ।**
 *
 * ⛔ প্রকল্পের **মাস্টার প্রিন্ট ডিজাইনেই** — সোনালি দাগ · লোগো-সারি · সবুজ
 *    শিরোনাম-বার · রোগীর ঘর · নিচে TK BISWAS ও ডাক্তারের সই-সারি
 *    (`CheckupA4Report`-এর হুবহু একই ধাঁচ)। নতুন কোনো সাজ বানানো হয়নি।
 * ⛔ **কাটা লাইনের টাকা কাটা অবস্থাতেই ছাপে**। 💰 V1062 (০৪.০৯.২০২৬, TK-নির্দেশ):
 *    Subtotal-এ এখন **সব লাইনের পুরো দাম** যায় (কাটা লাইনসুদ্ধ), ছাড় একবারই
 *    বাদ যায়। আগে কাটা লাইন বাদ **আর** ছাড়ও বাদ হত ⇒ একই ছাড় দুবার, কাগজে
 *    Net Payable কম ছাপা হত। এখানে কোনো হিসাব কষা হয় না — `sheet.subtotal`
 *    পর্দায় যা দেখায় কাগজেও হুবহু তাই।
 *    TK-এর নমুনা PDF-এ ঠিক তাই।
 * ⛔ কাগজে বাড়তি কোনো লেখা নেই (TK: *"Free লিখতে কে বলেছে আপনাকে"*)।
 */
object EstimateHtmlPrint {

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /* 🏠🔒 V1008 (০৩.০৯.২০২৬, TK-নির্দেশ) — TK: *"গ্রাম পোস্ট এক লাইনে থাকবে,
       থানা এবং জেলা আরেক লাইনে থাকবে"*। এস্টিমেটের কাগজে ঠিকানা এক লাইনেই
       বসত, তাই লম্বা ঠিকানা যেখানে খুশি ভেঙে যেত।
       প্রকল্পে এই নিয়মটা **আগে থেকেই আছে ও প্রমাণিত** — রেজিস্ট্রেশন ·
       ইনভেস্টিগেশন · ডায়েট চার্ট · চেকআপ রিপোর্টের কাগজে (`addr2` /
       `addrTwoLines`)। শুধু এই একটা কাগজেই বসানো বাকি ছিল।
       ⛔ থানার চিহ্ন না পেলে আগের মতোই এক লাইনে থাকে। */
    /* 🔠 V1008 — TK: *"VILL capital লেটার হবে"* ⇒ এস্টিমেটের কাগজে পুরো
       ঠিকানাটাই বড় হাতের অক্ষরে (নাম ও ব্রাঞ্চ এই কাগজে আগে থেকেই বড় হাতে)। */
    private fun addr2(aRaw: String): String {
        val a = aRaw.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ: "সমস্ত জায়গায় ক্যাপিটাল লেটারই করবেন") — শুধু **দেখানোর** সময় বড় হাতে; ডেটাবেসে যা লেখা আছে তা এক অক্ষরও বদলায় না।
        if (a.isBlank()) return "-"
        val u = a.uppercase()
        // 🟢🔒 V1135 (০৬.০৯.২০২৬ — TK: *"অ্যান্ড্রয়েড থেকে যত ধরনের প্রিন্ট
        // আউট হয়, একই জিনিস একই মডেল কম্পিউটার থেকেও হতে হবে"* — মিলিয়ে দেখতে
        // গিয়ে ধরা পড়ল): ওয়েবের `wlv1AddrTwo` বাংলা **"থানা"** শব্দেও লাইন ভাঙে,
        // অথচ এই চারটে ছাপার কাগজে সেটা বাদ ছিল ⇒ একই ঠিকানা কম্পিউটারে দুই
        // লাইনে, ফোনে এক লাইনে ছাপত। অ্যাপের বাকি আটটা জায়গায় শব্দটা আগে থেকেই আছে।
        // ⛔ ইংরেজি চিহ্নগুলো বড় হাতেই থাকল (উপরে `u` বড় হাতে করা), তাই আগের
        //    আচরণ এক অক্ষরও বদলায়নি — শুধু বাংলা শব্দটা যোগ হলো।
        val markers = listOf("PS:", "P.S", "P/S", "THANA", "POLICE STATION", "থানা")
        var idx = -1
        for (m in markers) { val k = u.indexOf(m); if (k > 0 && (idx == -1 || k < idx)) idx = k }
        if (idx <= 0) return esc(a)
        val first = a.substring(0, idx).trimEnd(',', ' ').trim()
        val second = a.substring(idx).trim()
        if (first.isBlank() || second.isBlank()) return esc(a)
        return esc(first) + "<br>" + esc(second)
    }

    fun build(
        sheet: EstimateModel.Sheet,
        branch: String,
        name: String,
        patientId: String,
        ageSex: String,
        mobile: String,
        address: String,
        date: String,
        /* 🖥️🔒 V982 (TK-নির্দেশ) — `true` হলে এই কাগজটাই পর্দায়
           দেখায় ও হলুদ ঘরগুলো চাপ দিয়ে বদলানো যায়। ছাপা ও শেয়ারের
           সময় `false` — তখন হুবহু আগের কাগজ, একটি দাগও বাড়তি নয়। */
        editable: Boolean = false
    ): String {
        val b = BranchCatalog.byName(branch)
        /* 📍🔒 V1114 (০৫.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ): *"পজিশন যদি সব
           ব্ল্যাঙ্ক থাকে তাহলে ওই কলমটা রাখারই দরকার নেই। যেখানে অবস্থান থাকবে
           তখন সেই কলমে কিছু লেখা থাকবে, কিন্তু ফাঁকা দাঁত থাকবে না।"*
           ⇒ একটাও লাইনে অবস্থান না থাকলে কলমটাই বসে না; থাকলে কলম থাকে, আর
             যে লাইনে নেই সেখানে **ফাঁকা** (আগের "—" দাঁতটা আর নয়)।
           ⛔ টাকার কিছুই বদলায় না — শুধু কাগজের চেহারা। */
        val showPos = sheet.lines.any { it.position.isNotBlank() }
        val rows = StringBuilder()
        for ((i, l) in sheet.lines.withIndex()) {
            val cls = if (l.struck) " class=\"free\"" else ""
            val amtCls = if (l.struck) " class=\"r amt\"" else " class=\"r\""
            /* 🏷️🔒 V986 (TK-রিপোর্ট ছবিসহ — "Grade II Haemorrhoid Treatment
               (Grade II)", একই কথা দুবার)। নামেই মাপটা থাকলে আর বন্ধনীতে লেখা
               হয় না। ⛔ ফিস্টুলার "(2 inch)" আগের মতোই থাকে। */
            val label = if (l.measure.isBlank() || l.name.contains(l.measure, ignoreCase = true))
                            esc(l.name)
                        else esc(l.name) + " (" + esc(EstimateModel.unitTxt(l.measure)) + ")"   // 📏 V1278
            /* ছাপার সময় `editable=false` ⇒ নিচের তিনটে স্ট্রিং ফাঁকা থাকে,
               তাই কাগজ হুবহু আগের মতোই ছাপে। */
            val aOpen = if (editable) "<a class=\"tap\" href=\"est://line/$i\">" else ""
            val eOpen = if (editable) "<a class=\"ed\" href=\"est://line/$i\">" else ""
            val aEnd = if (editable) "</a>" else ""
            rows.append("<tr").append(cls).append(">")
                .append("<td>").append(aOpen).append(label).append(aEnd).append("</td>")
            if (showPos) rows.append("<td class=\"k\">").append(aOpen)
                .append(esc(l.position)).append(aEnd).append("</td>")
            rows
                .append("<td class=\"r\">").append(eOpen).append(EstimateModel.money(l.rate)).append(aEnd).append("</td>")
                .append("<td class=\"r\">").append(eOpen).append(EstimateModel.moneyShort(l.qty)).append(aEnd).append("</td>")
                .append("<td").append(amtCls).append(">").append(EstimateModel.money(l.total)).append("</td>")
                .append("</tr>")
        }
        if (editable && sheet.lines.isEmpty()) {
            rows.append("<tr><td colspan=\"").append(if (showPos) 5 else 4)
                .append("\" class=\"k\" style=\"text-align:center;padding:16px 0\">")
                .append("No item added yet &mdash; use the buttons at the top.</td></tr>")
        }
        val findingBlock = if (sheet.finding.isBlank()) "" else
            """<div class="sec"><div class="sh">CLINICAL FINDING</div>
<div class="note">${esc(sheet.finding)}</div></div>"""
        /* 💰 V980 (TK-নির্দেশ) — শতাংশে দিলে কাগজেও "(20%)" লেখা থাকে। */
        val discLabel = if (sheet.discountPct && sheet.discount > 0.0)
            "Extra Discount (" + EstimateModel.moneyShort(sheet.discount) + "%)" else "Extra Discount"
        val discOpen = if (editable) "<a class=\"ed\" href=\"est://discount\">" else ""
        val discEnd = if (editable) "</a>" else ""
        /* 💸🔒 V986 (TK-রিপোর্ট: ছাড় শূন্য হলেও "Total Discount − 0.00" বসত)।
           ⇒ ছাড় না থাকলে ছাপার কাগজে লাইনটাই নেই; পর্দায় "Add discount" লেখা
             একটা ছোট জায়গা থাকে, যাতে চাপ দিয়ে ছাড় বসানো যায়। */
        val discountRow = if (sheet.discountAmount <= 0.0 && !editable) "" else
            if (sheet.discountAmount <= 0.0)
                """<div><span class="lbl">Extra Discount</span><span class="disc">${discOpen}Add discount$discEnd</span></div>"""
            else
                """<div><span class="lbl">$discLabel</span><span class="disc">&minus; $discOpen${EstimateModel.money(sheet.discountAmount)}$discEnd</span></div>"""
        /* 💰🔒 V1114 (TK-এর নিজের ছক, ফটো-প্রুফ পাশ) — কাটা আইটেমের টাকা এখন
           **নিজের আলাদা সারিতে** বাদ যায়, আর তার নিচে "কত রইল" সেটাও দেখানো
           হয়। তারপর TK-এর বাড়তি ছাড়। ⛔ কাটা আইটেম না থাকলে এই দুটো সারি
           বসেই না — তখন কাগজ হুবহু আগের মতোই দেখায়। */
        val cancelledRows = if (sheet.cancelledAmount <= 0.0) "" else
            """<div><span class="lbl">Cancelled Items Discount</span><span class="disc">&minus; ${EstimateModel.money(sheet.cancelledAmount)}</span></div>
<div class="mid"><span>Amount After Item Discount</span><span>${EstimateModel.money(sheet.afterItems)}</span></div>"""

        return """<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=794">
<style>
/* 🖨️🔒 V1105 (০৫.০৯.২০২৬, TK-রিপোর্ট: *"print তো ১ পেজে হওয়ার কথা ছিল"*)।
   মেপে দেখা: এই একটা কাগজেই `@page` নিয়মটা লেখাই হয়নি ⇒ ছাপার সময় প্রিন্টার
   নিজের ফাঁক (~১০mm) বসিয়ে দিত, অথচ নকশা পুরো A4 লম্বা ⇒ উপচে **২ পাতা**
   (২য় পাতায় শুধু সই-সারি)। V425-এ প্রেসক্রিপশনে ঠিক এই দোষটাই ধরা পড়েছিল
   আর ফাঁক ০ করেই সারানো হয়েছিল — Registration · Investigation · Diet Chart
   কাগজেও আগে থেকেই `@page{size:A4;margin:0}` আছে। এখানেও সেটাই বসল।
   ⛔ কাগজের ভিতরের সব ফাঁক (.lh · .wrap · .foot-এর padding) আগের মতোই,
      তাই লেখা কাগজের ধারে গিয়ে ঠেকে না — একটা দাগও নড়েনি।
   ⛔ উচ্চতা px-এর বদলে mm-এ (২৯৬mm) — ১১২৩px ঠিক ২৯৭mm-এর সমান হওয়ায়
      গোল করতে গিয়ে ১ পাতার বদলে ২ পাতা হয়ে যেতে পারত। */
@page{size:A4;margin:0;}
*{margin:0;padding:0;box-sizing:border-box;font-family:Georgia,'Noto Serif',serif;-webkit-print-color-adjust:exact;print-color-adjust:exact;}
body{background:#fff;color:#111;min-height:296mm;display:flex;flex-direction:column;}
.gold{height:5px;background:linear-gradient(90deg,#b8912f,#e6c65c,#b8912f);}
.gbar{height:3px;background:#0f5132;}
.lh{display:flex;align-items:center;gap:16px;padding:10px 20px 8px;}
.lh img{width:78px;height:78px;object-fit:contain;flex:0 0 auto;}
.cn{font-size:23px;font-weight:800;color:#0f5132;line-height:1;}
.tag{font-size:11px;font-weight:700;color:#b8912f;letter-spacing:2px;margin-top:2px;text-transform:uppercase;font-family:Arial;}
.addr{font-size:11.5px;color:#3b4650;margin-top:3px;font-family:Arial;}
.addr b{color:#0f5132;}
.tb{background:#0f5132;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:7px 20px;font-family:Arial;}
.tb .t{font-size:13px;font-weight:800;letter-spacing:2px;}
.tb .r{font-size:10.5px;color:#cfe6d8;}
.pi{display:flex;gap:18px;padding:9px 20px;font-size:12px;font-family:Arial;background:#f7faf8;border-bottom:1.5px solid #e4ebe6;}
.pi .c{flex:1;}
.pi .r{padding:2.5px 0;}
.pi .r b{color:#0f5132;display:inline-block;min-width:74px;}
.wrap{padding:8px 20px 10px;font-family:Arial;flex:1;}
.sec{margin-top:7px;border:1px solid #d5ddd7;border-radius:4px;overflow:hidden;}
.sh{background:#eef5f0;color:#0f5132;font-size:11px;font-weight:800;letter-spacing:1px;padding:5.5px 12px;border-left:4px solid #b8912f;}
.note{padding:7px 12px;font-size:11.3px;line-height:1.5;color:#222;}
table{width:100%;border-collapse:collapse;font-size:11.3px;}
thead th{background:#eef5f0;color:#0f5132;text-align:left;padding:6px 10px;font-size:10px;letter-spacing:.8px;border-bottom:1px solid #d5ddd7;}
td{padding:6px 10px;border-bottom:1px solid #f0f3f1;color:#111;}
td.k{color:#6b7680;}
td.r,th.r{text-align:right;}
tbody tr:last-child td{border-bottom:0;}
.free td{color:#3B434B;}
.free .amt{text-decoration:line-through;color:#5B6570;font-weight:600;}
.sum{margin-top:8px;display:flex;justify-content:flex-end;}
.sumbox{width:300px;border:1px solid #d5ddd7;border-radius:4px;overflow:hidden;}
.sumbox div{display:flex;justify-content:space-between;padding:6px 12px;font-size:11.5px;font-family:Arial;border-bottom:1px solid #f0f3f1;}
.sumbox div:last-child{border-bottom:0;}
.sumbox .lbl{color:#6b7680;}
.sumbox .mid{background:#f7faf8;font-weight:700;color:#22303f;}   /* 💰 V1114 */
.sumbox .net{background:#0f5132;color:#fff;font-weight:800;font-size:13px;padding:9px 12px;}
.disc{color:#B42318;font-weight:700;}
.small{font-size:10px;color:#6b7680;font-style:italic;padding:8px 2px 0;font-family:Arial;}
.foot{margin-top:auto;padding:9px 20px 4px;font-family:Arial;}
.sign{display:grid;grid-template-columns:1fr auto 1fr;align-items:start;gap:22px;}
.sign .ln{border-top:.9px solid #15231C;text-align:center;padding-top:5px;}
.sign .ln b{display:block;font-size:11.2px;font-weight:900;color:#15231C;}
.sign .ln small{display:block;font-size:9px;color:#54615A;margin-top:1px;}
.vfy{text-align:center;border-top:.9px solid #15231C;padding-top:5px;}
.vfy .bar{height:24px;width:121px;margin:0 auto 1.5px;background:repeating-linear-gradient(90deg,#15231C 0 1.9px,#fff 1.9px 4px);}
.vfy .vl{margin-top:2px;white-space:nowrap;}
.vfy b{font-size:8.6px;color:#0A5428;}.vfy small{font-size:8.2px;color:#54615A;}
.fn{border-top:1px solid #e4ebe6;text-align:center;font-size:9.5px;color:#8a949e;padding:5px 0 6px;font-family:Arial;}
${if (!editable) "" else """
a{color:inherit;text-decoration:none;}
.ed{background:#FFF4CE;border-bottom:1.4px dashed #C9A227;border-radius:2px;padding:0 3px;}
.free .amt .ed{text-decoration:line-through;}
"""}
</style></head><body>
<div class="gold"></div>
<div class="lh"><img src="${b.logoAssetPath}">
<div><div class="cn">${esc(b.clinicName)}</div><div class="tag">Ayurveda &amp; Anorectal Diseases</div>
<div class="addr"><b>${esc(b.displayName)}:</b> ${esc(b.addressLine)} &nbsp;|&nbsp; <b>&#9742;</b> ${esc(b.phoneLine)} &nbsp;|&nbsp; <b>&#9742;</b> ${esc(BranchCatalog.HELPLINE)}</div></div></div>
<div class="gbar"></div>
<div class="tb"><span class="t">TREATMENT COST ESTIMATE</span><span class="r">Rec. No: ${esc(patientId)} &nbsp;&middot;&nbsp; Date: ${esc(date)}</span></div>
<div class="pi">
<div class="c"><div class="r"><b>Name</b> : ${esc(name)}</div><div class="r"><b>Patient ID</b> : ${esc(patientId)}</div><div class="r"><b>Age / Sex</b> : ${esc(ageSex)}</div></div>
<div class="c"><div class="r"><b>Mobile</b> : ${esc(mobile)}</div><div class="r"><b>Branch</b> : ${esc(b.displayName)}</div><div class="r"><b>Address</b> : <span style="display:inline-block;vertical-align:top">${addr2(address)}</span></div></div>
</div>
<div class="wrap">
$findingBlock
<div class="sec"><div class="sh">COST BREAKDOWN</div>
<table><thead><tr><th>Treatment / Item</th>${if (showPos) "<th>Position</th>" else ""}<th class="r">Rate (&#8377;)</th><th class="r">Qty</th><th class="r">Total (&#8377;)</th></tr></thead>
<tbody>$rows</tbody></table></div>
<div class="sum"><div class="sumbox">
<div><span class="lbl">Grand Total</span><span>${EstimateModel.money(sheet.grandTotal)}</span></div>
$cancelledRows
$discountRow
<div class="net"><span>Net Payable Amount</span><span>${EstimateModel.money(sheet.netPayable)}</span></div>
${if (sheet.timeAsked.isBlank()) "" else """<div><span class="lbl">Time Asked</span><span>${esc(sheet.timeAsked)}</span></div>"""}
</div></div>
<div class="small">* This estimate is indicative and based on the initial clinical presentation. The net payable amount may vary.</div>
</div>
<div class="foot"><div class="sign">
<div class="ln"><b>TK BISWAS</b><small>Founder &amp; Consultant</small></div>
<div class="vfy"><div class="bar"></div>
<div class="vl"><b>Document Digitally Verified</b> &middot; <small>No Physical Signature Required</small></div></div>
<div class="ln"><b>Dr. K.H MANDAL</b><small>(B.A.M.S) Regd 12386</small></div>
</div></div>
<div class="fn">Computer-generated cost estimate &middot; ${esc(b.clinicName)} &middot; Ayurveda &amp; Anorectal Diseases</div>
</body></html>"""
    }
}
