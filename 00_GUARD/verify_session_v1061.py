import json
from playwright.sync_api import sync_playwright
S='/tmp/claude-0/-home-user-piles-clinic-erp/168d9891-ef27-5321-92f8-972fd3726ef8/scratchpad'
GEN=open(S+'/gen.js').read()
session={"mobile":"9999999999","name":"TK BISWAS","branch":"All","role":"master"}
PROOF={"rows":[
 {"person_code":"KNE-LAXMI","full_name":"LAXMI GUPTA","role_kind":"staff","designation":"Staff","branch":"Kishanganj","link_mobile":"9883605917","active":True}],
 "cfgs":{"KNE-LAXMI":{"salary_enabled":True,"salary_amount":8000,"salary_date":"3"}}}
seed=("localStorage.setItem('rk_session', %s); window.__PROOF = %s;"
      % (json.dumps(json.dumps(session)), json.dumps(PROOF)))
SEED2="""(function(){
 var p=JSON.parse(localStorage.getItem('rk_patients')||'[]');
 if(p.length>1){
   p[0].patientId='KNE-22082026-001'; p[0].name='MOHSINA KHATOON'; p[0].mobile='7384872527';
   p[0].timeType='Unexpected Time'; p[0].timeSource='auto'; p[0].disease='PILES';
   p[0].createdAt='2026-08-22T21:14:00';
   p[1].patientId='KNE-19082026-002'; p[1].name='SATEBUL'; p[1].mobile='9832145670';
   p[1].timeType='Unexpected Time'; p[1].timeSource='hand'; p[1].disease='FISTULA';
   p[1].createdAt='2026-08-19T22:41:00';
   localStorage.setItem('rk_patients',JSON.stringify(p));
   var eq=JSON.parse(localStorage.getItem('rk_enquiries')||'[]');
   eq.unshift({id:'q1',mobile:'7384872527',createdAt:'2026-08-21T20:36:00',date:'2026-08-21'});
   localStorage.setItem('rk_enquiries',JSON.stringify(eq));
   var py=JSON.parse(localStorage.getItem('rk_payments')||'[]');
   py=py.filter(function(r){return String(r.patientId)!==String(p[0].id)&&String(r.patientId)!==String(p[1].id)});
   py.unshift({id:'y1',patientId:p[1].id,payType:'treatment',amount:'5000',createdAt:'2026-09-01T11:20:00'});
   localStorage.setItem('rk_payments',JSON.stringify(py));
 }
})();"""
PAYS=[{'id':'e1','kind':'EXTRA','status':'DUE','amount':100,'paid_on':'2026-09-02','extra_reason':'Registration · KNE-22082026-001 · Manually approved by TK'},
      {'id':'e2','kind':'EXTRA','status':'DUE','amount':100,'paid_on':'2026-09-02','extra_reason':'Registration · KNE-22082026-001'},
      {'id':'e3','kind':'EXTRA','status':'PAID','mode':'Cash','amount':400,'paid_on':'2026-09-01','extra_reason':'Treatment · KNE-19082026-002'}]
STMT=[{'id':'s1','kind':'SALARY','status':'PAID','amount':8000,'mode':'Cash','paid_on':'2026-08-03','for_month':'2026-08'},
      {'id':'s2','kind':'EXTRA','status':'DUE','amount':300,'paid_on':'2026-08-21','extra_reason':'Registration · X'}]
fails=[]
def ck(n,c,d=''):
    print(('  ✅ ' if c else '  ❌ ')+n+('' if c else '   << '+str(d)[:160]))
    if not c: fails.append(n)
with sync_playwright() as pw:
    b=pw.chromium.launch(executable_path='/opt/pw-browsers/chromium-1194/chrome-linux/chrome',args=['--no-sandbox'])
    ctx=b.new_context(viewport={'width':430,'height':1100},device_scale_factor=1)
    ctx.route('**supabase.co**', lambda r: r.abort()); ctx.add_init_script(seed)
    pg=ctx.new_page(); je=[]; pg.on('pageerror', lambda e: je.append(str(e)[:200]))
    pg.goto('http://127.0.0.1:8939/index.html', wait_until='domcontentloaded')
    pg.evaluate(GEN); pg.evaluate(SEED2); pg.reload(wait_until='domcontentloaded')
    pg.wait_for_function('typeof window.__salTable==="function"', timeout=25000); pg.wait_for_timeout(1200)
    pg.evaluate("()=>{ window.MOD.gate=function(t,fn){return fn();}; window.MOD.isMasterModule=function(){return true;}; }")

    # A) Salary Statement — এক রোগী এক বাক্স, Total = উপরের যোগফল
    pg.evaluate("""(p)=>{document.getElementById('app').innerHTML='<div class="wrap anMod anModPf"><div class="page">'+window.__salTable(p)+'</div></div>';}""", PAYS)
    pg.wait_for_timeout(900)
    t=pg.evaluate("()=>document.querySelector('.page').innerText")
    ck('এক রোগী = এক বাক্স (MOHSINA একবারই)', t.count('MOHSINA KHATOON')==1, t.count('MOHSINA KHATOON'))
    ck('দুটো ₹১০০ যোগ হয়ে ₹২০০', '₹200' in t, t[:200])
    ck('Total ₹২০০-ও মিলছে', 'Total ₹200' in t)
    ck('"Added by hand" আর নেই', 'Added by hand' not in t)
    ck('তারিখ-সময় বসেছে', '21.08.2026' in t and '8:36 PM' in t)
    ck('ট্রিটমেন্টের টাকা না থাকলে ওই লাইন নেই', t.count('Treatment paid')==1, t.count('Treatment paid'))

    # B) নামে চাপ → ওই রোগী খোলে
    pg.eval_on_selector('.pfXWho',"e=>e.click()"); pg.wait_for_timeout(1500)
    ck('নামে চাপ দিলে ঠিক রোগী খোলে', 'MOHSINA KHATOON' in pg.evaluate("()=>document.body.innerText"))

    # C) Statement — তারিখ থেকে তারিখ
    pg.evaluate("async(r)=>{ await window.profStatement('KNE-LAXMI','2026-08-01','2026-08-31', r); }", STMT)
    pg.wait_for_timeout(900)
    st=pg.evaluate("()=>document.querySelector('.pfStmTable').innerText")
    ck('মাস ছোট করে Aug-26', 'Aug-26' in st, st[:120])
    ck('স্যালারি ₹৮,০০০ · বাকি ₹৩০০', '₹8,000' in st and '₹300' in st)
    ck('TOTAL সারি আছে', 'TOTAL' in st)

    # D) স্টাফ কার্ড
    pg.evaluate("async()=>{ await window.staffProfiles(); }"); pg.wait_for_timeout(1400)
    c=pg.evaluate("()=>document.querySelector('.pfStaffCard').innerText")
    ck('কার্ডে তিনটে বোতাম', all(x in c for x in ['Salary','Performance','Extra Income']))
    ck('কার্ডে Suspend/Remove নেই', ('Suspend' not in c) and ('Remove' not in c))
    ck('নামের short (LG) নেই', 'LG' not in c.split('\\n')[0])
    ck('Salary day ধাঁচ', '• Salary day: 3' in c, c[:160])
    pg.eval_on_selector('.pfMore',"e=>e.click()"); pg.wait_for_timeout(800)
    mn=pg.evaluate("()=>{const d=document.querySelector('.pfDotsMenu');return d?d.innerText:'';}")
    ck('⋮ মেনুতে Suspend ও Remove', 'Suspend' in mn and 'Remove' in mn, mn)
    ck('কোনো JS ভুল নেই', not je, je[:2])
    b.close()
print('\\nমোট ব্যর্থ:', len(fails), fails)
