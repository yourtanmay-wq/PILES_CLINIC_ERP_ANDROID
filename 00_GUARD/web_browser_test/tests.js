/* 🔵 V1293 — কম্পিউটার-অ্যাপের ব্রাউজার-পরীক্ষা (run.py থেকে চালান)। নকল সার্ভার: Supabase-এ কিছু যায় না। */
const {chromium}=require('playwright');const http=require('http'),fs=require('fs'),path=require('path');
const ROOT=process.env.TK_WEB_ROOT, SDK=fs.readFileSync(process.env.TK_SDK_PATH,'utf8'), EXE=process.env.TK_CHROME||undefined, UD=process.env.TK_UDATA||'udata';
const PORT=8790, BASE='http://localhost:'+PORT;
const srv=http.createServer((q,r)=>{let p=path.join(ROOT,decodeURIComponent(q.url.split('?')[0]));if(p.endsWith('/'))p+='index.html';fs.readFile(p,(e,d)=>{if(e){r.statusCode=404;return r.end()}r.setHeader('content-type',p.endsWith('.js')?'text/javascript':p.endsWith('.css')?'text/css':'text/html');r.end(d)})}).listen(PORT);
const IDS=[];for(let i=0;i<1500;i++)IDS.push('followups|fu_'+i);
const seen=[]; let failures=0;
function ok(name,cond,detail){ console.log((cond?'  ✅ ':'  ❌ ')+name+(cond?'':'  → '+JSON.stringify(detail))); if(!cond)failures++; }
function handler(r){
  const req=r.request(), u=req.url(), m=req.method(), h=req.headers();
  const fulfill0=r.fulfill.bind(r); r.fulfill=(o)=>fulfill0(Object.assign({},o,{headers:Object.assign({'cache-control':'no-store'},o.headers||{})}));
  if(u.includes('cdn.jsdelivr.net')) return r.fulfill({status:200,headers:{'content-type':'text/javascript'},body:SDK});
  if(u.startsWith(BASE)) return r.continue();
  if(u.includes('/rest/v1/deleted_records')){
    const kind = m==='HEAD' ? 'COUNT' : (u.includes('order=deletedAt') ? 'HEADROW' : (u.includes('order=id') ? 'PAGE' : 'OTHER'));
    seen.push(kind);
    if(kind==='COUNT') return r.fulfill({status:200,headers:{'content-range':'*/'+IDS.length,'access-control-expose-headers':'content-range','content-type':'application/json'},body:''});
    if(kind==='HEADROW') return r.fulfill({status:200,headers:{'content-type':'application/json'},body:JSON.stringify([{id:IDS[IDS.length-1]}])});
    const q=new URL(u).searchParams; let off=Number(q.get('offset')||0), lim=Number(q.get('limit')||1000); if(h['range']){const rr=h['range'].split('-').map(Number); off=rr[0]; lim=rr[1]-rr[0]+1;}
    const slice=IDS.slice(off,off+lim);
    return r.fulfill({status:200,headers:{'content-type':'application/json','content-range':off+'-'+(off+slice.length-1)+'/'+IDS.length,'access-control-expose-headers':'content-range'},body:JSON.stringify(slice.map(id=>({id})))});
  }
  if(u.includes('/rest/v1/patients') && u.includes('server_updated_at')){
    seen.push('POLL');
    if(POLL.noColumn) return r.fulfill({status:400,headers:{'content-type':'application/json'},body:JSON.stringify({code:'42703',message:'column patients.server_updated_at does not exist'})});
    if(u.includes('order=server_updated_at.desc')) return r.fulfill({status:200,headers:{'content-type':'application/json'},body:JSON.stringify([{server_updated_at:POLL.head}])});
    const sel=(new URL(u).searchParams.get('select')||''); if(sel.split(',').includes('photo')) seen.push('POLL_ASKED_PHOTO');
    return r.fulfill({status:200,headers:{'content-type':'application/json'},body:JSON.stringify(POLL.rows)});
  }
  if(u.includes('/rest/v1/payments')){
    /* 🔴 V1304 (তালিকা ৪১৭): নকল Supabase — আসলটার মতোই এক ডাকে সর্বোচ্চ ১০০০ সারি, limit বেশি চাইলেও */
    seen.push('PAY');
    const q=new URL(u).searchParams; let off=Number(q.get('offset')||0), lim=Number(q.get('limit')||1000); if(h['range']){const rr=h['range'].split('-').map(Number); off=rr[0]; lim=rr[1]-rr[0]+1;}
    lim=Math.min(lim,1000);
    const slice=PAYS.slice(off,off+lim);
    return r.fulfill({status:200,headers:{'content-type':'application/json','content-range':off+'-'+(off+slice.length-1)+'/'+PAYS.length,'access-control-expose-headers':'content-range'},body:JSON.stringify(slice)});
  }
  return r.fulfill({status:200,headers:{'content-type':'application/json'},body:'[]'});   // অন্য সব REST: ফাঁকা
}
const PAYS=(()=>{let a=[];for(let i=0;i<2821;i++){a.push({id:'pay'+String(i).padStart(5,'0'),patientId:'p'+(i%600),mobile:'98000'+String(i%600).padStart(5,'0'),branch:'KNE',payType:'treatment',amount:100,date:'2026-08-04',updatedAt:new Date(Date.UTC(2026,7,4,0,0,i)).toISOString()})}return a})();
const POLL={noColumn:false,head:'2026-09-10T10:00:00.000000+00:00',rows:[]};
const mk=n=>{let rows=[];for(let i=0;i<n;i++){rows.push({id:'p'+i,name:'PATIENT NAME '+i,mobile:'98000'+String(i).padStart(5,'0'),branch:'KNE',address:'Some long address text here for size '.repeat(8),createdAt:new Date(Date.now()-i*1000).toISOString(),updatedAt:new Date(Date.now()-i*1000).toISOString()})}return rows};
async function ctxOf(dir,noIdb){ fs.rmSync(dir,{recursive:true,force:true}); const ctx=await chromium.launchPersistentContext(dir,{headless:true,executablePath:EXE,args:['--no-sandbox']}); if(noIdb) await ctx.addInitScript(()=>{Object.defineProperty(window,'indexedDB',{value:undefined})}); return ctx; }
async function open(ctx){ const page=await ctx.newPage(); await page.route('**/*',handler); const errs=[]; page.on('pageerror',e=>errs.push(String(e.message).slice(0,140))); await page.goto(BASE+'/index.html'); await page.waitForTimeout(1200); return {page,errs}; }
const state=p=>p.evaluate(()=>{let ls=null;try{ls=localStorage.getItem('rk_patients')}catch(e){};return {rows:load('patients').length,lsChars:ls==null?null:ls.length,ramOnly:!!RAM_ONLY.patients,idbOff:__bigOff}});
setTimeout(()=>{console.log('❌ TIMEOUT');process.exit(2)},240000);
(async()=>{
 console.log('— জমা-ঘর (V1287) —');
 let ctx=await ctxOf(path.join(UD,'a')); let {page,errs}=await open(ctx);
 await page.evaluate(`localStorage.setItem('rk_patients',JSON.stringify((${mk.toString()})(4000)))`);
 await page.reload(); await page.waitForTimeout(1500); let s=await state(page);
 ok('A localStorage → IndexedDB সরানো (৪০০০ সারি, localStorage খালি)', s.rows===4000&&s.lsChars===null&&!s.idbOff, s);
 await page.evaluate(`save('patients',(${mk.toString()})(14000),{skipCloud:true})`); await page.waitForTimeout(800); await page.reload(); await page.waitForTimeout(1800); s=await state(page);
 ok('B ১৪০০০ সারি reload-এর পরেও আছে (RAM-only নয়)', s.rows===14000&&!s.ramOnly, s);
 let p2=(await open(ctx)).page; await p2.evaluate(`save('patients',(${mk.toString()})(15000),{skipCloud:true})`); await page.waitForTimeout(1500); s=await state(page);
 ok('D অন্য ট্যাবের লেখা এই ট্যাবে (১৫০০০)', s.rows===15000, s);
 await page.evaluate(()=>{localStorage.setItem('rk_session',JSON.stringify({mobile:'9999999999',name:'TEST MASTER',branch:'All',role:'master'}))});
 const e2=[]; page.on('pageerror',e=>e2.push(String(e.message).slice(0,140))); await page.reload({timeout:90000}); /* V1432 — ১৫০০০ সারি নিয়ে master-boot এই মেশিনে ~৩৩ s লাগে; ৩০ s-এ মিথ্যা FAIL হচ্ছিল */ await page.waitForTimeout(2500);
 const txt=await page.evaluate(()=>document.body.innerText.slice(0,300).replace(/\s+/g,' '));
 ok('E মাস্টার-সেশনে boot + ড্যাশবোর্ড + page-error ০', /Master Admin/.test(txt)&&e2.length===0, {txt:txt.slice(0,120),e2});
 await ctx.close();
 ctx=await ctxOf(path.join(UD,'c'),true); ({page,errs}=await open(ctx));
 await page.evaluate(`save('patients',(${mk.toString()})(3000),{skipCloud:true})`); await page.reload(); await page.waitForTimeout(1200); s=await state(page);
 ok('C IndexedDB না থাকলে localStorage-পথ (৩০০০)', s.rows===3000&&s.lsChars>0&&s.idbOff, s);
 await ctx.close();
 console.log('— মোছা-চিহ্ন তালিকা (V1292) —');
 ctx=await ctxOf(path.join(UD,'f')); ({page,errs}=await open(ctx));
 const run=async(pre)=>{ seen.length=0; const r=await page.evaluate(async(pre)=>{ try{await initCloudClientOnly()}catch(e){} if(pre==='reset')wlv1WebDeletedSet=new Set(); await wlv1WebSyncDeleted(); return {sz:wlv1WebDeletedSet.size,hasNew:wlv1WebDeletedSet.has('patients|p_new')} },pre); r.PAGE=seen.filter(k=>k==='PAGE').length; return r; };
 let r=await run(); ok('F1 প্রথমবার পুরো তালিকা (PAGE 2, ১৫০০)', r.PAGE===2&&r.sz===1500, r);
 r=await run('reset'); ok('F2 অপরিবর্তিত ⇒ পাতা-টানা বাদ (PAGE 0, ১৫০০)', r.PAGE===0&&r.sz===1500, r);
 IDS.push('patients|p_new'); r=await run(); ok('F3 নতুন চিহ্ন ⇒ আবার পুরো (PAGE 2, নতুনটা আছে)', r.PAGE===2&&r.hasNew, r);
 await page.reload(); await page.waitForTimeout(1500); r=await run(); ok('F4 reload-এর পরে জমা কপি (PAGE 0, নতুনটা আছে)', r.PAGE===0&&r.hasNew, r);
 IDS.pop(); r=await run(); ok('F5 চিহ্ন তোলা ⇒ আবার পুরো (PAGE 2, বাদ)', r.PAGE===2&&!r.hasNew, r);
 console.log('— ভারী টেবিল: লাইভের বদলে হালকা পড়া (V1295) —');
 await page.evaluate(()=>{localStorage.setItem('rk_session',JSON.stringify({mobile:'9999999999',name:'TEST MASTER',branch:'All',role:'master'}))});
 await page.reload(); await page.waitForTimeout(1500);
 await page.evaluate(async()=>{ try{await initCloudClientOnly()}catch(e){} window.__chanCount=0; const orig=sb.channel.bind(sb); sb.channel=function(n){ window.__chanCount++; return orig(n); }; });
 seen.length=0; let g=await page.evaluate(async()=>{ await wlv1RtPollTick(); return {cursor:localStorage.getItem('rk_rtpoll_cursor_patients')} });
 ok('G1 প্রথম টিক: কার্সার = সার্ভারের সবচেয়ে নতুন (কোনো সারি টানা নয়)', g.cursor==='2026-09-10T10:00:00.000000+00:00'&&seen.filter(k=>k==='POLL').length>=1, {g,seen});
 POLL.rows=[{id:'pz1',name:'POLL PATIENT 1',mobile:'9800000001',branch:'KNE',updatedAt:'2026-09-10T10:01:00.000Z',server_updated_at:'2026-09-10T10:01:00.000000+00:00'},{id:'pz2',name:'POLL PATIENT 2',mobile:'9800000002',branch:'KNE',updatedAt:'2026-09-10T10:02:00.000Z',server_updated_at:'2026-09-10T10:02:00.000000+00:00'}];
 seen.length=0; g=await page.evaluate(async()=>{ await wlv1RtPollTick(); const rows=load('patients'); return {cursor:localStorage.getItem('rk_rtpoll_cursor_patients'),has1:rows.some(r=>r.id==='pz1'),has2:rows.some(r=>r.id==='pz2'&&r.name==='POLL PATIENT 2'),leak:rows.some(r=>r.server_updated_at!==undefined)} });
 ok('G2 বদলানো ২ সারি স্থানীয়ে বসল, ছবি চাওয়া হয়নি, কার্সার এগোল', g.has1&&g.has2&&!g.leak&&g.cursor==='2026-09-10T10:02:00.000000+00:00'&&!seen.includes('POLL_ASKED_PHOTO'), {g,seen});
 POLL.noColumn=true; seen.length=0; g=await page.evaluate(async()=>{ localStorage.setItem('rk_rtpoll_cursor_patients','2026-09-10T10:02:00.000000+00:00'); await wlv1RtPollTick(); const pe=performance.getEntriesByType('resource').filter(e=>e.name.includes('server_updated_at')).map(e=>({u:e.name.slice(-60),size:e.transferSize})); return {fallback:!!__rtPollFallback.patients, chan:window.__chanCount, pe:pe.slice(-3)} }); g.seen=seen.slice();
 ok('G3 ঘর না থাকলে (SQL চলেনি) ⇒ ওই টেবিল আগের লাইভ চ্যানেলে', g.fallback&&g.chan>=1, g);
 console.log('— ১০০০-সারির সীমা: পুরো টেবিল পাতা-ধরে (V1304) —');
 seen.length=0; let hq=await page.evaluate(async()=>{ const r=await wlv1FetchPaged('payments','*',null,true); const ids=new Set((r.rows||[]).map(x=>x.id)); return {err:r.error?String(r.error.message||r.error):null,n:(r.rows||[]).length,uniq:ids.size,has1003:ids.has('pay01002'),last:ids.has('pay02820')}; });
 ok('H1 ২৮২১ সারির টেবিল — সবগুলো আসে (১০০৩ নম্বর ও শেষটা সহ), জোড়া নেই', !hq.err&&hq.n===2821&&hq.uniq===2821&&hq.has1003&&hq.last, hq);
 ok('H2 ঠিক ৩টা ডাক (১০০০+১০০০+৮২১)', seen.filter(k=>k==='PAY').length===3, seen.filter(k=>k==='PAY').length);
 await ctx.close(); srv.close();
 console.log(failures?('❌ '+failures+'টা পরীক্ষা ব্যর্থ'):'সব পরীক্ষা পাশ'); process.exit(failures?1:0);
})().catch(e=>{console.error('❌',e);process.exit(1)});
