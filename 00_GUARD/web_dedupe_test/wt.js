// ওয়েবের dedupe পরীক্ষা — app.js থেকে সরাসরি বের করা কোডই চালানো হচ্ছে
const fs=require('fs');
let pass=0,fail=0;
const ck=(n,ok,got)=>{ if(ok){pass++;console.log('  ✅ '+n+'  →  '+got);} else {fail++;console.log('  ❌ '+n+'  →  '+got);} };

global.window = globalThis;
window.RK_CONFIG = { supabaseUrl: 'https://demo.supabase.co' };
const REST = 'https://demo.supabase.co/rest/v1/';

let hits = 0, failNext = false, serverBody = '[{"amount":100}]';
window.fetch = async function(input, init){
  hits++;
  const url = typeof input==='string'?input:input.url;
  await new Promise(r=>setTimeout(r,30));
  if(failNext) return new Response('err',{status:500,statusText:'Server Error'});
  if(url.indexOf(REST)!==0) return new Response('OUTSIDE',{status:200});
  return new Response(serverBody,{status:200,headers:{'content-range':'0-1/2','content-type':'application/json'}});
};
const realFetchRef = window.fetch;
window.user = { mobile:'+919800000001' };

eval(fs.readFileSync(require('path').join(__dirname,'hook_extracted.js'),'utf8'));

(async ()=>{
  console.log('── ১. একসঙ্গে ৮টা একই অনুরোধ ──');
  hits=0;
  const rs = await Promise.all(Array.from({length:8},()=>window.fetch(REST+'payments?select=*&limit=5000')));
  ck('নেটে গেল ১ বার', hits===1, 'network hit = '+hits+' (আগে ৮)');
  const bodies = await Promise.all(rs.map(r=>r.text()));
  ck('আটজনই নিজের উত্তর পড়তে পারল', bodies.every(b=>b===serverBody), 'সবাই = '+bodies[0]);
  ck('হেডারও অটুট (পাতা গোনার জন্য জরুরি)', rs[0].headers.get('content-range')==='0-1/2', rs[0].headers.get('content-range'));

  console.log('\n── ২. ২০ সেকেন্ডের ভিতরে আবার ──');
  hits=0;
  await window.fetch(REST+'patients?select=id');
  await window.fetch(REST+'patients?select=id');
  await window.fetch(REST+'patients?select=id');
  ck('৩ বার চাওয়া, নেটে ১ বার', hits===1, 'network hit = '+hits);

  console.log('\n── ৩. লেখা (POST/PATCH) ──');
  hits=0;
  await window.fetch(REST+'payments', {method:'POST', body:'{}'});
  ck('লেখা সরাসরি যায় (ছোঁয়া হয়নি)', hits===1, 'network hit = '+hits);
  hits=0;
  await window.fetch(REST+'patients?select=id');
  ck('লেখার পরে জমানো মুছে গেছে', hits===1, 'network hit = '+hits+' (টাটকা আনল)');

  console.log('\n── ৪. Supabase ছাড়া অন্য ঠিকানা ──');
  hits=0;
  await window.fetch('https://other-site.com/data.json');
  await window.fetch('https://other-site.com/data.json');
  ck('একেবারেই ছোঁয়া হয় না', hits===2, 'network hit = '+hits);

  console.log('\n── ৫. ব্যর্থ উত্তর (নেট/সার্ভার সমস্যা) ──');
  hits=0; failNext=true;
  const bad = await window.fetch(REST+'followups?select=id');
  ck('ব্যর্থ উত্তর ঠিকই ফেরে', bad.status===500, 'status = '+bad.status);
  failNext=false;
  const good = await window.fetch(REST+'followups?select=id');
  ck('ব্যর্থতা জমা হয়নি — পরের বার নেটে যায়', hits>=2 && good.status===200, 'network hit = '+hits+', status = '+good.status);

  console.log('\n── ৬. আলাদা schema / পাতা আলাদাই থাকে ──');
  hits=0;
  await window.fetch(REST+'x?select=id', {headers:{'accept-profile':'hr'}});
  await window.fetch(REST+'x?select=id', {headers:{'accept-profile':'fin'}});
  ck('hr আর fin আলাদা অনুরোধ', hits===2, 'network hit = '+hits);
  hits=0;
  await window.fetch(REST+'y?select=id', {headers:{'range':'0-999'}});
  await window.fetch(REST+'y?select=id', {headers:{'range':'1000-1999'}});
  ck('আলাদা পাতা আলাদা অনুরোধ', hits===2, 'network hit = '+hits);

  console.log('\n── ৭. ব্যবহারকারী বদল (লগআউট → নতুন লগইন) ──');
  hits=0; serverBody='[{"branch":"Kishanganj"}]';
  const a = await (await window.fetch(REST+'payments?select=*')).text();
  window.user = null;                                   // লগআউট
  serverBody='[{"branch":"Falakata"}]';
  window.user = { mobile:'+919800000002' };             // অন্যজন লগইন
  const b = await (await window.fetch(REST+'payments?select=*')).text();
  ck('দ্বিতীয় জন নতুন করে নেট থেকে পায়', hits===2, 'network hit = '+hits);
  ck('আগের জনের তথ্য পায়নি', b.includes('Falakata') && !b.includes('Kishanganj'), b);

  console.log('\n── ৮. পরিসংখ্যান ──');
  const st = window.__wlv1DedupeStats();
  ck('চলমান-অনুরোধ তালিকা খালি (লিক নেই)', st.inFlight===0, 'inFlight = '+st.inFlight);
  ck('জমানো আছে, মেমরি সীমার ভিতরে', st.cached>0 && st.bytes<6*1024*1024, 'জমা = '+st.cached+', bytes = '+st.bytes);

  console.log('\n═══════════════════════════════');
  console.log('পাশ: '+pass+'   ব্যর্থ: '+fail);
  process.exit(fail>0?1:0);
})();
