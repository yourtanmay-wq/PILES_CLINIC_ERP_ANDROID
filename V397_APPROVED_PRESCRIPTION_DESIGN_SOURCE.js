/* ═══════════════════════════════════════════════════════════════════════════
   🔒 V397 — TK-অনুমোদিত প্রেসক্রিপশন ডিজাইনের **আসল উৎস** (১৬.০৮.২০২৬)

   ⚠️ এই ফাইলটা অ্যাপ চালায় না। এটা **রেফারেন্স** — TK যে ডিজাইনটা এক-এক করে
      দেখে অনুমোদন করেছেন, তার হুবহু CSS ও গঠন এখানে ধরা আছে।

   কেন রাখা হলো: প্রুফগুলো বানানো হয়েছিল সেশনের অস্থায়ী জায়গায়। সেশন শেষ
   হলে ওটা মুছে যেত, আর পরের সেশনে ডিজাইনটা **আন্দাজে** বানাতে হত — যেটা
   TK স্পষ্ট নিষেধ করেছেন। তাই পুরোটা প্রজেক্টের ভিতরেই তুলে রাখা হলো।

   TK-র অনুমোদিত ১০টি বিন্দু (এই ফাইলের CSS-এ সব আছে):
     ১) রোগীর ঘর — দু'কলাম: বাঁয়ে Name/Age/Patient ID/Mobile,
        ডানে Date/Sex/Diseases/Address; মাঝে সবুজ খাড়া দাগ
     ২) Disease & Complaint ঘরের বাঁ ও উপরের দাগ থাকবে না
     ৩) ঐ ঘরের **ডানে পাতলা সবুজ খাড়া দাগ** (0.8px), নিচ পর্যন্ত টানা
     ৪) ওষুধের তালিকা ℞-এর ঠিক নিচ থেকে শুরু
     ৫) ADVICE · DIET · NEXT FOLLOW-UP DATE — বাঁ ঘরের একদম তলায়
     ৬) Rx বাক্সের তলার আড়াআড়ি দাগ থাকবে না
     ৭) TK BISWAS · বারকোড · Dr. K.H MANDAL — পাতার তলা থেকে ৭ মিমি,
        তিনটে **এক সমান্তরালে**
     ৮) বারকোড ছোট (৫মিমি×৩০মিমি), নিচে ৬pt-এ "Document Digitally Verified";
        বারকোডের নিচে **পেশেন্ট ID থাকবে না**
     ৯) জলছাপ — **একটাই**, বড় (১৮০মিমি), হালকা (opacity .035), উপরে ৫৫মিমি
        ফাঁক দিয়ে নিচের দিকে
     ১০) নিচের সবুজ পটি — "All treatments are Ayurvedic & Natural |
        Bring this prescription on your next visit |
        **In an emergency, visit your nearest hospital immediately**"

   ⛔ পরের সেশনে করণীয়: এই CSS ও গঠন **হুবহু** প্রজেক্টে বসানো —
      Prescription **ও** Medicine Slip দুটোতেই, Web (mobile+desktop) ও Android-এ।
      ধাপ ও ক্রম `V396_STATUS_READ_FIRST.md`-এ লেখা আছে।
   ═══════════════════════════════════════════════════════════════════════════ */

const {chromium}=require('playwright');const fs=require('fs');
const ROOT='/tmp/app/PILES_CLINIC_APP_V386_FINAL_1/03_NETLIFY_READY/';
const CSS=fs.readFileSync(ROOT+'styles.css','utf8');
const MAA='data:image/jpeg;base64,'+fs.readFileSync(ROOT+'assets/maa-ayurved-final-logo.jpg').toString('base64');
// ⛔ ব্রাঞ্চ config.js থেকে হুবহু · ওষুধ RX_FIXED_COMMON থেকে হুবহু · শুধু রোগী ডেমো
const MEDS=[['Arshakuthar Rasa','Tab','2-0-2','After Food','5 days'],
 ['Kankayan Vati Arsha','Tab','2-0-2','After Food','5 days'],
 ['Abhayadi Modak','Tab','0-0-1','After Food','5 days'],
 ['Jatyadi Ghritam','Oint','Local App.','Morning & Evening','5 days'],
 ['Qurs Alkali','Tab','2-0-2','Before Food','5 days'],
 ['Habb-e-Kabid Naushadri','Tab','1-0-1','Before Food','5 days']];
const rows=MEDS.map((m,i)=>`<tr><td>${i+1}</td><td><span class="rxPrintType">${m[1]}</span>${m[0]}</td><td>${m[2]}</td><td>${m[3]}</td><td>${m[4]}</td></tr>`).join('');
// RX_HISTORY_FIELDS-এর ডিফল্ট ৪টি
const HIST=[['Disease','PILES'],['Symptoms','Bleeding, Pain, Swelling'],
 ['Duration','2 MONTHS'],['Chief Complaint','BLEEDING WHILE PASSING STOOL']]
 .map(h=>`<div><b>${h[0]}</b><span>${h[1]}</span></div>`).join('');
const html=`<div class="printArea prescriptionLock rxPrescriptionPrint">
<div class="wm"><img class="printLogoImg" src="${MAA}"></div>
<div class="printHead compact"><div class="logoCell"><img class="printLogoImg" src="${MAA}"></div>
<div class="clinicCell"><h2>MAA AYURVED PILES CLINIC</h2><div class="brSub">JALPAIGURI BRANCH</div>
<p>Raikatpara, Opp. Sports Complex, Jalpaiguri · Mobile: +91 8436002200</p></div></div>
<div class="rxTagline"><b>WE PROVIDE AYURVEDA KSHAR SUTRA THERAPY IN PILES, FISSURE &amp; FISTULA</b><span>MOST SUCCESSFUL TREATMENT WITH HIGH SUCCESS RATE</span></div>
<div class="printTitle">PRESCRIPTION</div>
<div class="rxPatientInfo anRxPt"><div class="c"><div><b>Patient Name</b><i>:</i><span>SUJATA ROY</span></div><div><b>Age</b><i>:</i><span>38</span></div><div><b>Patient ID</b><i>:</i><span>JPE-16082026-007</span></div><div><b>Mobile</b><i>:</i><span>+91 9832114455</span></div></div><div class="c"><div><b>Date</b><i>:</i><span>16.08.2026</span></div><div><b>Sex</b><i>:</i><span>FEMALE</span></div><div><b>Diseases</b><i>:</i><span>PILES</span></div><div class="ad"><b>Address</b><i>:</i><span>Pandapara Colony, PO: Jalpaiguri<br>PS: Kotwali, Jalpaiguri, 735101</span></div></div></div>
<div class="rxBox clinicPadRx finalMedicalPrint"><div class="rxMark">Rx</div>
<h3 class="medicalPrintTitle">PRESCRIPTION</h3>
<div class="rxApprovedGrid" style="--clinic-watermark:url('${MAA}')">
<aside class="rxComplaintHistory">${HIST}<div class="anRxBot"><div class="rxPrintAdvice"><span><b>ADVICE:</b> Sitz Bath — 2 Times Daily</span><span class="rxDietLine"><b>Diet:</b> Light &amp; fibre rich</span></div><div class="anRxNext"><b>Next Follow-up Date</b><span>_______________</span></div></div></aside>
<section><div class="rxGridMark">℞</div>
<table class="printTable finalPrintTable"><tr><th>SL</th><th>Medicine Name</th><th>Dose</th><th>When</th><th>Duration</th></tr>${rows}</table>

</section></div></div>
<div class="doctorLine"><div class="docLeft"><b>TK BISWAS</b><small>Founder &amp; Consultant</small></div>
<div class="verifyCenter"><b>Document Digitally Verified</b><small>No Physical Signature Required</small><div class="vbar"></div><div class="vid">JPE-16082026-007</div></div>
<div class="docRight"><b>Dr. K.H MANDAL</b><small>(B.A.M.S) Regd 12386</small></div></div>

<div class="thanksStrip">All treatments are Ayurvedic &amp; Natural &nbsp;|&nbsp; Bring this prescription on your next visit &nbsp;|&nbsp; <b>In an emergency, visit your nearest hospital immediately</b></div></div>`;
(async()=>{const b=await chromium.launch();
const p=await b.newPage({viewport:{width:794,height:1123},deviceScaleFactor:3});
await p.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${CSS}</style><style>
.rxPatientInfo.anRxPt{display:grid!important;grid-template-columns:1fr 1fr!important;gap:0 0!important;position:relative!important;padding:2.5mm 0 2mm!important;border:0!important;border-bottom:1.3px solid #0A5428!important;margin:1.5mm 6mm 0!important;background:transparent!important}
.anRxPt .c{padding:0 4mm}
.anRxPt .c:first-child{border-right:.8px solid #0A5428}
.anRxPt .c>div{display:grid;grid-template-columns:22mm 3mm 1fr;align-items:start;padding:.55mm 0}
.anRxPt b{font-size:7.5pt!important;font-weight:bold!important;color:#18251D!important}
.anRxPt i{font-style:normal;font-size:7.5pt;color:#18251D}
.anRxPt span{font-size:8pt!important;color:#000!important;line-height:1.25}
/* ⬆️ TK-নির্দেশ: ওষুধের নাম যেখান থেকে শুরু, সেটা আরও উপরে তোলা।
   ℞ চিহ্নটা বড় ফন্টে অনেকটা জায়গা নিচ্ছিল, আর Rx বাক্সের উপরে বাড়তি প্যাডিং ছিল। */
.rxPrescriptionPrint .rxBox{padding-top:2mm!important}
.rxPrescriptionPrint .medicalPrintTitle{display:none!important}
.rxPrescriptionPrint .rxGridMark{font-size:15pt!important;line-height:1!important;margin:0 0 1.5mm!important;padding:0!important;height:auto!important}
.rxPrescriptionPrint .rxApprovedGrid section{padding-top:0!important}
.rxPrescriptionPrint .rxComplaintHistory{padding-top:0!important}
.rxPrescriptionPrint .rxApprovedGrid .printTable{margin-top:0!important}
.rxPrescriptionPrint .rxApprovedGrid section{display:flex!important;flex-direction:column!important}
.rxPrescriptionPrint .rxGridMark{position:static!important;display:block!important;flex:0 0 auto!important;margin:0 0 1mm!important}
/* TK: Diseases / Complaint history — বাঁ ও উপরের দাগ থাকবে না, আরও উপর থেকে শুরু */
.rxPrescriptionPrint .rxComplaintHistory{border:0!important;border-left:0!important;border-top:0!important;
  padding:0 3mm 0 0!important;margin:0!important;background:transparent!important}
.rxPrescriptionPrint .rxComplaintHistory>div:first-child{margin-top:0!important;padding-top:0!important}
/* দুই কলামই আরও উপরে */
.rxPrescriptionPrint .rxBox{padding-top:1mm!important}
/* TK: সই-সারি + বারকোড + নিচের সব একদম পাতার তলায় ফিক্স */
.printArea.rxPrescriptionPrint{display:flex!important;flex-direction:column!important;height:297mm!important;min-height:297mm!important}
.printArea.rxPrescriptionPrint .rxBox{flex:1 1 auto!important;min-height:0!important}
/* TK: একদম তলায় ফিক্স — পাতার নিচ থেকে মেপে বসানো */
.printArea.rxPrescriptionPrint{position:relative!important}
.printArea.rxPrescriptionPrint .doctorLine{position:absolute!important;left:6mm!important;right:6mm!important;bottom:23mm!important;margin:0!important}
.printArea.rxPrescriptionPrint .doctorLine{bottom:7mm!important;
  display:grid!important;grid-template-columns:1fr auto 1fr!important;
  align-items:center!important;gap:6mm!important}
/* TK: তিনটেই এক সমান্তরালে — মাঝের ব্লকের বাড়তি উপরের অংশ সরিয়ে সমান করা */
.printArea.rxPrescriptionPrint .verifyCenter{align-self:center!important;margin:0!important}
.printArea.rxPrescriptionPrint .verifyCenter{display:flex!important;flex-direction:column!important;align-items:center!important}
.printArea.rxPrescriptionPrint .verifyCenter small{display:none!important}
/* TK: বারকোড ছোট, আর তার নিচে ছোট করে 'Document Digitally Verified' */
.printArea.rxPrescriptionPrint .verifyCenter .vbar{order:1!important;height:5mm!important;width:30mm!important;margin:0 auto .8mm!important}
.printArea.rxPrescriptionPrint .verifyCenter b{order:2!important;display:block!important;font-size:6pt!important;color:#0A5428!important;font-weight:bold!important}
.printArea.rxPrescriptionPrint .docLeft,
.printArea.rxPrescriptionPrint .docRight{align-self:center!important}
/* TK: নিচের দিকের দাগটা রাখতে হবে না */
.printArea.rxPrescriptionPrint .rxBox{border-bottom:0!important}
/* TK: বারকোডের নিচে পেশেন্ট ID থাকবে না */
.printArea.rxPrescriptionPrint .verifyCenter .vid{display:none!important}
/* TK: ADVICE · Diet · Next Follow-up — Disease & Complaint ঘরের একদম নিচে */
.rxPrescriptionPrint .rxComplaintHistory{display:flex!important;flex-direction:column!important;
  border-right:.8px solid #0A5428!important;padding-right:4mm!important;min-height:184mm!important}
.rxPrescriptionPrint .anRxBot{margin-top:auto!important;padding-top:3mm!important}
.rxPrescriptionPrint .rxPrintAdvice{border:0!important;background:transparent!important;padding:0!important;margin:0 0 3mm!important;display:block!important}
.rxPrescriptionPrint .rxPrintAdvice span{display:block!important;font-size:8.5pt!important;color:#15231C!important;line-height:1.35!important}
.rxPrescriptionPrint .rxPrintAdvice b{color:#0A5428!important}
.rxPrescriptionPrint .anRxNext{border-top:.8px solid #C7D3CB;padding-top:2mm}
.rxPrescriptionPrint .anRxNext b{display:block;font-size:7.5pt;color:#0A5428}
.rxPrescriptionPrint .anRxNext span{display:block;font-size:9pt;letter-spacing:1px;color:#15231C;margin-top:1mm}
.printArea.rxPrescriptionPrint .thanksStrip{position:absolute!important;left:0!important;right:0!important;bottom:0!important;margin:0!important}
.printArea.rxPrescriptionPrint .rxBox{margin-bottom:40mm!important}
.rxPrescriptionPrint .rxApprovedGrid{align-items:start!important}

</style><style>html,body{margin:0;background:#fff}</style></head><body class="printModeActive">${html}</body></html>`);
await p.emulateMedia({media:'print'});await p.waitForTimeout(700);
const r=await p.evaluate(()=>{const mm=v=>+(v/(96/25.4)).toFixed(1);
 const a=document.querySelector('.printArea').getBoundingClientRect();
 const g=s=>{const e=document.querySelector(s);return e?mm(e.getBoundingClientRect().bottom-a.top):null};
 return {w:mm(a.width),h:mm(a.height),head:g('.printHead.compact'),tag:g('.rxTagline'),
  title:g('.printTitle'),pt:g('.rxPatientInfo'),box:g('.rxBox'),foot:g('.thanksStrip'),
  rows:document.querySelectorAll('.finalPrintTable tr').length-1,
  doc:(document.querySelector('.docRight')||{}).textContent,
  left:(document.querySelector('.docLeft')||{}).textContent};});
console.log('কাগজ:',r.w+'mm × '+r.h+'mm', r.w===210&&r.h>=297?'= A4 ✅':'❌');
console.log('হেডার শেষ',r.head+'mm | ট্যাগলাইন',r.tag+'mm | শিরোনাম',r.title+'mm | রোগী-বাক্স',r.pt+'mm');
console.log('ওষুধের সারি:',r.rows,'| Rx বাক্স শেষ',r.box+'mm | পাতার শেষ',r.foot+'mm', r.foot<=297?'✅ এক পাতায়':'❌ উপচে পড়েছে');
console.log('সই — বাঁয়ে:',r.left,'| ডানে:',r.doc);
await p.screenshot({path:'/tmp/out/PRESCRIPTION_A4_PROOF.png',fullPage:true});
await b.close();})();
