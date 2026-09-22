var window=globalThis; var wlv1EstSheet={lines:[],discount:0,discountPct:false,finding:'',strikeInDiscount:true};
function wlv1EstLoad(){
  try{ var raw=window.__wlv1EstJson||''; if(!raw){ wlv1EstSheet={lines:[],discount:0,discountPct:false,finding:'',strikeInDiscount:true}; return }
    var o=JSON.parse(raw);
    wlv1EstSheet={lines:(o.lines||[]).map(function(l){return{name:l.name||'',measure:l.measure||'',position:l.position||'',rate:Number(l.rate||0),qty:Number(l.qty||1),struck:!!l.struck}}),
      discount:Number(o.discount||0),discountPct:!!o.discountPct,finding:String(o.finding||''),
      strikeInDiscount:!!o.strikeInDiscount};
    /* 💰🔴🔒 V1064 — **পুরনো সেভ করা এস্টিমেটের টাকা যেন এক পয়সাও না বদলায়।**
       পুরনো নিয়মে কাটা লাইনের টাকা নিজে থেকেই বাদ যেত, তাই তখন Discount ঘরে
       কিছু লেখা না-ও থাকতে পারে; V1062-এর পরে ওই কাগজ খুললে Net Payable
       বেড়ে যেত। ⇒ চিহ্নটা না থাকলে (পুরনো কাগজ) কাটা লাইনের টাকা **একবার**
       ছাড়ে যোগ করে নেওয়া হয়, তাই অঙ্ক হুবহু আগের মতোই থাকে।
       ⛔ ফোনের `EstimateModel.parse()`-এর হুবহু একই নিয়ম (নিয়ম ৬.৬)। */
    if(!wlv1EstSheet.strikeInDiscount){
      var st=wlv1EstSheet.lines.filter(function(l){return l.struck})
        .reduce(function(n,l){return n+Number(l.rate||0)*Number(l.qty||0)},0);
      if(st>0){
        if(wlv1EstSheet.discountPct){
          /* 🔴 V1064খ — পুরনো নিয়মে শতাংশটা **কাটা-বাদ-দেওয়া** যোগফলের উপর
             কষা হত; পুরনো অঙ্ক ফেরাতে ওই ভিত্তিই নিতে হবে। */
          var base=wlv1EstSheet.lines.filter(function(l){return !l.struck})
            .reduce(function(n,l){return n+Number(l.rate||0)*Number(l.qty||0)},0);
          wlv1EstSheet.discount=Math.max(0,Math.min(base*Number(wlv1EstSheet.discount||0)/100,base));
          wlv1EstSheet.discountPct=false;
        }
        wlv1EstSheet.discount=Math.max(0,Number(wlv1EstSheet.discount||0)+st);
      }
      wlv1EstSheet.strikeInDiscount=true;
    }
  }catch(e){ wlv1EstSheet={lines:[],discount:0,discountPct:false,finding:'',strikeInDiscount:true} }
}
function wlv1EstSubtotal(){ return wlv1EstSheet.lines.reduce(function(n,l){return n+Number(l.rate||0)*Number(l.qty||0)},0) }
function wlv1EstDiscAmt(){
  var sub=wlv1EstSubtotal(), d=Number(wlv1EstSheet.discount||0);
  var v=wlv1EstSheet.discountPct ? (sub*d/100) : d;
  return Math.max(0, Math.min(v, sub));
}
function wlv1EstNet(){ return Math.max(0, wlv1EstSubtotal()-wlv1EstDiscAmt()) }
function wlv1EstStruckSync(l, nowStruck){
  try{
    if(wlv1EstSheet.discountPct){
      var sub=wlv1EstSubtotal();
      wlv1EstSheet.discount = Math.max(0, Math.min(sub*Number(wlv1EstSheet.discount||0)/100, sub));
      wlv1EstSheet.discountPct = false;
    }
    var amt = Number(l.rate||0)*Number(l.qty||0);
    wlv1EstSheet.discount = Math.max(0, Number(wlv1EstSheet.discount||0) + (nowStruck ? amt : -amt));
  }catch(e){}
}
function wlv1EstStore(){ try{ window.__wlv1EstJson = wlv1EstSheet.lines.length?JSON.stringify(wlv1EstSheet):'' }catch(e){} }
function old(sub){ return sub; }
// ── পুরনো কাগজ: কাটা লাইন আছে, Discount ঘর ফাঁকা (পুরনো নিয়মে ভরসা) ──
var OLD={lines:[{name:'A',rate:8455,qty:1},{name:'B',rate:6233,qty:1},
 {name:'C',rate:5.20,qty:250,struck:true},{name:'D',rate:125,qty:3,struck:true},
 {name:'E',rate:225,qty:3,struck:true}], discount:0, discountPct:false, finding:''};
var oldPayable = 8455+6233;   // পুরনো নিয়মে যা ছাপত
window.__wlv1EstJson=JSON.stringify(OLD); wlv1EstLoad();
console.log('পুরনো কাগজ  Sub',wlv1EstSubtotal(),' Disc',wlv1EstDiscAmt(),' Net',wlv1EstNet(),' (আগে ছাপত',oldPayable,')');
console.log(wlv1EstNet()===oldPayable ? '✅ পুরনো কাগজের টাকা এক পয়সাও বদলায়নি' : '❌ পুরনো কাগজের টাকা বদলে গেছে');
// দুবার খুললেও যেন আবার যোগ না হয়
wlv1EstStore(); wlv1EstLoad();
console.log('আবার খুললে  Net',wlv1EstNet());
console.log(wlv1EstNet()===oldPayable ? '✅ দ্বিতীয়বার খুললেও এক' : '❌ দুবার যোগ হয়ে গেছে');
// ── পুরনো কাগজ, শতাংশ ছাড় ১০% ──
var OLD2=JSON.parse(JSON.stringify(OLD)); OLD2.discount=10; OLD2.discountPct=true;
var oldPayable2 = (8455+6233) - (8455+6233)*0.10;
window.__wlv1EstJson=JSON.stringify(OLD2); wlv1EstLoad();
console.log('পুরনো ১০%   Net',wlv1EstNet().toFixed(2),' (আগে ছাপত',oldPayable2.toFixed(2),')');
console.log(Math.abs(wlv1EstNet()-oldPayable2)<0.01 ? '✅ শতাংশের পুরনো কাগজও এক' : '❌ শতাংশে বদলে গেছে');
// ── নতুন কাগজ: কেটে দিলে ছাড়ে বসে ──
window.__wlv1EstJson=''; wlv1EstLoad();
wlv1EstSheet.lines=[{name:'A',rate:8455,qty:1},{name:'B',rate:6233,qty:1},
 {name:'C',rate:5.20,qty:250},{name:'D',rate:125,qty:3},{name:'E',rate:225,qty:3}];
[2,3,4].forEach(function(i){ var l=wlv1EstSheet.lines[i]; l.struck=true; wlv1EstStruckSync(l,true); });
console.log('নতুন কাগজ   Sub',wlv1EstSubtotal(),' Disc',wlv1EstDiscAmt(),' Net',wlv1EstNet());
console.log(wlv1EstSubtotal()===17038 && wlv1EstNet()===14688 ? '✅ TK-এর সংখ্যা হুবহু' : '❌ মেলেনি');
wlv1EstStore(); wlv1EstLoad();
console.log('সেভ করে আবার খুললে Net',wlv1EstNet());
console.log(wlv1EstNet()===14688 ? '✅ নতুন কাগজও দুবার যোগ হয় না' : '❌ নতুন কাগজে দুবার যোগ');
