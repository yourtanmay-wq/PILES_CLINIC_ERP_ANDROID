/* ============================================================================
   🔔🔒 V1186 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — Doctor Note & Reminder
   (কম্পিউটার), ফোনের `DoctorReminderActivity` / `DoctorReminderRepository`-র যমজ।

   TK-এর কথা (হুবহু):
     · "এটা প্রত্যেকের হোম স্ক্রিনে ই থাকবে"
     · "যে কোন staff, যে কোন ডাক্তার এবং মাস্টার এটা ক্রিয়েট করতে পারবে"
     · "যে ব্রাঞ্চের যে Doctor তাকেই করতে পারবে"
     · "পাঠানোর পর KH MANDAL কে Accept করতে হবে"
     · "পরে যেন History তে ও দেখতে পায়, যাতে কেউ অস্বীকার না করতে পারে"

   ⛔ পুরনো `patients.doctorReminder*` ব্যবস্থা এক অক্ষরও ছোঁয়া হয়নি।
   ⛔ টাকার কোনো হিসাব এখানে নেই।
   ========================================================================== */
(function () {
  var TABLE = 'doctor_reminders';
  /* ওই ব্রাঞ্চের নিজের ডাক্তার + Dr. K.H MANDAL + TK BISWAS — TK-র পাঁচটা
     তালিকা মিলিয়ে বের করা একটাই নিয়ম (ফোনেও হুবহু এটাই)। */
  var EVERYWHERE_DOCTOR = '7980993652';   // Dr. K.H MANDAL

  function d10(v){ return String(v||'').replace(/\D/g,'').slice(-10); }
  function me(){ try{ return d10((typeof user!=='undefined'&&user&&user.mobile)||''); }catch(e){ return ''; } }
  function myBranch(){ try{ return String(((typeof user!=='undefined'&&user)||{}).branch||'').trim(); }catch(e){ return ''; } }
  function myRole(){ try{ return String(((typeof user!=='undefined'&&user)||{}).role||'').toLowerCase(); }catch(e){ return ''; } }
  function myName(){ try{ return (typeof codeName==='function' ? (codeName(user&&user.mobile)||'') : '') || String((user&&user.name)||''); }catch(e){ return ''; } }
  function nowIso(){ return new Date().toISOString(); }
  function todayIso(){ try{ return window.MOD.todayIST(); }catch(e){ return new Date().toISOString().slice(0,10); } }
  function esc2(v){ try{ return esc(String(v==null?'':v)); }catch(e){ return String(v==null?'':v); } }

  function dmy(iso){ try{ var p=String(iso).slice(0,10).split('-'); return p[2]+'/'+p[1]+'/'+p[0]; }catch(e){ return iso||''; } }
  /* ⏰🔒 V1241 — ফোনের হুবহু যমজ। সময় ডেটাবেসে UTC-তে জমা (...Z),
     কিন্তু এতদিন ওই ঘণ্টাটাই সোজা ছাপা হত ⇒ ভারতীয় সময়ের চেয়ে ৫ঘ.৩০মি. পিছিয়ে
     দেখাত (১১.১৫ PM → ৫.৪৫ PM)। এখন ভারতীয় সময়ে বদলে দেখানো হয়।
     ⛔ জমা থাকা লেখা এক অক্ষরও বদলায়নি — শুধু পর্দায় পড়াটা ঠিক হলো। */
  function stamp(raw){
    var t=String(raw||''); if(t.length<10) return '';
    if(t.length>=19 && t.slice(-1)==='Z'){
      try{
        var ms=Date.parse(t);
        if(!isNaN(ms)){
          var ist=new Date(ms+(5*60+30)*60000);
          var dd=String(ist.getUTCDate()).padStart(2,'0'),
              mo=String(ist.getUTCMonth()+1).padStart(2,'0'),
              yy=ist.getUTCFullYear(),
              H=ist.getUTCHours(), M=String(ist.getUTCMinutes()).padStart(2,'0');
          var ap2=H>=12?'PM':'AM', h2=(H===0)?12:(H>12?H-12:H);
          return dd+'/'+mo+'/'+yy+'  \u00b7  '+h2+'.'+M+' '+ap2;
        }
      }catch(e){}
    }
    var d=dmy(t); if(t.length<16) return d;
    try{
      var hh=parseInt(t.substr(11,2),10), mm=t.substr(14,2);
      var ap=hh>=12?'PM':'AM', h12=(hh===0)?12:(hh>12?hh-12:hh);
      return d+'  ·  '+h12+'.'+mm+' '+ap;
    }catch(e){ return d; }
  }
  function time12(hm){
    var p=String(hm||'').split(':'); if(p.length<2) return hm||'';
    var h=parseInt(p[0],10); if(isNaN(h)) return hm;
    var ap=h>=12?'PM':'AM', h12=(h===0)?12:(h>12?h-12:h);
    return h12+'.'+p[1]+' '+ap;
  }

  /** ওই ব্রাঞ্চে যাঁদের কাছে পাঠানো যায় (ফোনের `doctorsForBranch`-এর যমজ)। */
  function doctorsForBranch(branch){
    var out=[];
    try{
      var list=(typeof allUsers==='function')?allUsers():[];
      (list||[]).forEach(function(u){
        var role=String(u.role||'').toLowerCase();
        var mob=d10(u.mobile);
        var nm=(typeof codeName==='function'?(codeName(u.mobile)||''):'')||String(u.name||'')||mob;
        if(role==='doctor'){
          var mine=String(u.branch||'').trim().toLowerCase()===String(branch||'').trim().toLowerCase();
          if(mine || mob===EVERYWHERE_DOCTOR) out.push([nm,mob]);
        } else if(role==='master'){ out.push([nm,mob]); }
      });
    }catch(e){}
    var seen={}, uniq=[];
    out.forEach(function(x){ if(!seen[x[1]]){ seen[x[1]]=1; uniq.push(x); } });
    return uniq;
  }

  var DR_ROWS=[];
  async function drRemLoad(historyMode){
    if(typeof sb==='undefined'||!sb) return [];
    var q=sb.from(TABLE).select('*').eq('active',true);
    if(!historyMode) q=q.gte('remindDate',todayIso());
    var r=await q.order('createdAt',{ascending:false}).limit(300);
    var rows=(r&&r.data)||[];
    // 🚫 V1194 — বাতিল হওয়া সারি চলতি তালিকায় আসে না; History-তে আসে।
    if(!historyMode) rows=rows.filter(function(x){ return !String(x.cancelledAt||''); });
    if(myRole()==='master') return rows;
    var m=me(), br=myBranch().toLowerCase();
    return rows.filter(function(x){
      var f=d10(x.forMobile), b=d10(x.byMobile);
      return f===m || b===m || (!f && String(x.branch||'').trim().toLowerCase()===br);
    });
  }

  /* ─────────────────────────────────────────────────────────────────────
     🙈🔒 V1193 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু) — ফোনের `waitingFor` /
     `acceptedNoticesFor` / `hide` / `ack`-এর যমজ।
       · "আমি তো পাঠালাম, তাহলে অল টাইম আমার হোম স্ক্রিনে কেন দেখাবে"
       · "এটা হাইড রাখার ব্যবস্থা তো রাখতে হবে"
       · "উপরের ঘন্টাতে নোটিফিকেশন আসুক"
     ⛔ Doctor Note & Reminder পর্দা ও History-তে সব **আগের মতোই** থাকে।
     ───────────────────────────────────────────────────────────────────── */
  function listHas(raw,m){ if(!m) return false;
    return String(raw||'').split(',').some(function(x){ return x.trim()===m; }); }
  function listAdd(raw,m){ if(!m) return String(raw||'');
    if(listHas(raw,m)) return String(raw||'');
    return String(raw||'')?(String(raw)+','+m):m; }

  /** হোম-কার্ড ও ঘন্টার একটাই উৎস — এই ব্যক্তির জন্য অপেক্ষা করছে এমনগুলো। */
  function drWaiting(rows){
    var m=me(); if(!m) return [];
    var br=myBranch().toLowerCase(), role=myRole();
    var canRole=(role==='master'||role==='doctor');
    return (rows||[]).filter(function(x){
      if(String(x.cancelledAt||'')) return false;      // 🚫 V1194
      if(String(x.acceptedAt||'')) return false;
      if(listHas(x.hiddenBy,m)) return false;
      if(d10(x.byMobile)===m) return false;             // নিজের পাঠানো নয়
      var f=d10(x.forMobile);
      if(f) return f===m;
      return canRole && String(x.branch||'').trim().toLowerCase()===br;
    });
  }
  /** পাঠানো ব্যক্তির ঘন্টার জন্য — তাঁর পাঠানো যেগুলো Accept হয়েছে, দেখা হয়নি। */
  function drAcceptedNotices(rows){
    var m=me(); if(!m) return [];
    return (rows||[]).filter(function(x){
      return d10(x.byMobile)===m && String(x.acceptedAt||'') && !listHas(x.ackBy,m);
    });
  }
  window.__DR_BELL={waiting:[],accepted:[]};
  /** একটাই পড়া — হোম-কার্ড, ঘন্টার সংখ্যা ও ঘন্টার তালিকা তিনটেই এখান থেকেই। */
  async function drRemBellRefresh(){
    /* ⛔ **একটাই পড়া** — দুটো তালিকাই এই একই সারিগুলো থেকে ছেঁকে বার করা হয়,
       তাই ফ্রি প্ল্যানে বাড়তি egress লাগে না (V509-এর শিক্ষা)। */
    var rows=[]; try{ rows=await drRemLoad(true); }catch(e){ return window.__DR_BELL; }
    var t=todayIso();
    var live=rows.filter(function(x){ return String(x.remindDate||'')>=t; });
    window.__DR_BELL={ waiting:drWaiting(live), accepted:drAcceptedNotices(rows) };
    try{ drRemPaintBadge(); }catch(e){}
    return window.__DR_BELL;
  }
  function drRemBellCount(){
    try{ return (window.__DR_BELL.waiting.length + window.__DR_BELL.accepted.length); }catch(e){ return 0; }
  }
  /** ঘন্টার ব্যাজে সংখ্যাটা বসিয়ে দেয় — নতুন করে পুরো হেডার আঁকতে হয় না। */
  function drRemPaintBadge(){
    try{
      var b=document.querySelector('.bellOnly'); if(!b) return;
      var base=0;
      try{ base=mergeFollow(scoped(load('followups'))).filter(wlv1CallDue).length; }catch(e){ base=0; }
      var n=base+drRemBellCount();
      var sp=b.querySelector('.bellBadge');
      if(n>0){ if(!sp){ sp=document.createElement('span'); sp.className='bellBadge'; b.appendChild(sp); } sp.textContent=n; }
      else if(sp){ sp.remove(); }
    }catch(e){}
  }
  async function drRemHide(id){
    var x=(DR_ROWS||[]).filter(function(r){return r.id===id})[0];
    var m=me(); if(!m) return;
    if(typeof sb==='undefined'||!sb) return toast('No internet connection');
    try{
      var next=listAdd(x?x.hiddenBy:'', m);
      var r=await sb.from(TABLE).update({hiddenBy:next}).eq('id',id);
      if(r&&r.error) return toast('করা গেল না — আবার চেষ্টা করুন');
      if(x) x.hiddenBy=next;
      toast('Hidden from your Home & bell');
      await drRemBellRefresh();
      try{ var el=document.getElementById('drRemHomeBox'); if(el&&!drRemBellCount()) el.innerHTML=''; }catch(e){}
      drRemHome();
    }catch(e){ toast('করা গেল না — আবার চেষ্টা করুন'); }
  }
  /* 🚫🔒 V1194 (TK-নির্দেশ ও ফটো-প্রুফ পাশ) — ভুল করে পাঠানো হলে **যিনি
     পাঠিয়েছেন** বাতিল করতে পারেন, **Accept হওয়ার আগে পর্যন্ত**। TK:
     *"ডিলিট লেখা থাকলে তো বিভ্রান্ত হতে পারে"* ⇒ লেখা **Cancel**।
     ⛔ সারিটা মোছে না — History-তে "CANCELLED · কে · কখন" থেকে যায়। */
  function canCancel(x){
    var m=me(); if(!m) return false;
    if(String(x.acceptedAt||'')) return false;
    if(String(x.cancelledAt||'')) return false;
    return d10(x.byMobile)===m;
  }
  async function drRemCancel(id){
    var x=(DR_ROWS||[]).filter(function(r){return r.id===id})[0];
    if(!x||!canCancel(x)) return;
    if(typeof sb==='undefined'||!sb) return toast('No internet connection');
    if(!confirm('Cancel this reminder?\nIt was sent by you and is not accepted yet.\nHistory will still show it as CANCELLED.')) return;
    try{
      var at=nowIso(), nm=myName();
      var r=await sb.from(TABLE).update({cancelledBy:me(),cancelledByName:nm,cancelledAt:at}).eq('id',id);
      if(r&&r.error) return toast('করা গেল না — আবার চেষ্টা করুন');
      x.cancelledBy=me(); x.cancelledByName=nm; x.cancelledAt=at;
      toast('Cancelled');
      await drRemBellRefresh();
      drRemHome();
    }catch(e){ toast('করা গেল না — আবার চেষ্টা করুন'); }
  }

  async function drRemAck(id){
    var m=me(); if(!m||typeof sb==='undefined'||!sb) return;
    var x=((window.__DR_BELL||{}).accepted||[]).filter(function(r){return r.id===id})[0];
    try{
      var next=listAdd(x?x.ackBy:'', m);
      await sb.from(TABLE).update({ackBy:next}).eq('id',id);
      if(x) x.ackBy=next;
      await drRemBellRefresh();
    }catch(e){}
    drRemHome();
  }

  /** এই সারিটা এই ব্যবহারকারী Accept করতে পারেন কিনা — ফোনের হুবহু নিয়ম।
      ⛔ স্টাফ কখনো নয়; নির্দিষ্ট একজনকে পাঠানো হলে কেবল তিনিই। */
  function canAccept(x){
    if(String(x.acceptedAt||'')) return false;
    var m=me(); if(!m) return false;
    var f=d10(x.forMobile);
    if(f) return f===m;
    if(myRole()==='master') return true;
    if(myRole()!=='doctor') return false;
    return String(x.branch||'').trim().toLowerCase()===myBranch().toLowerCase();
  }

  /* 🩺🔒 V1194 (TK: *"রোগের নাম দরকার তো"*) — নামের পাশে ছোট চিপ।
     ⛔ পুরনো সারিতে ঘরটা ফাঁকা; কম্পিউটারে তখন **আগে থেকেই রাখা** রোগী-তালিকা
        থেকে নামটা ভরে নেওয়া হয় (নতুন কোনো ক্লাউড-পড়া নয়)। */
  function drDisease(x){
    var d=String(x.disease||'').trim();
    if(d) return d;
    try{
      var m=d10(x.patientMobile);
      var p=(load('patients')||[]).filter(function(q){ return d10(q.mobile)===m; })[0];
      return p?String(p.disease||'').trim():'';
    }catch(e){ return ''; }
  }
  function drDisChip(x){
    var d=drDisease(x);
    if(!d) return '';
    return ' <span style="background:#EEF4FF;border:1px solid #D6E2FB;color:#123E8C;border-radius:10px;font-size:11.5px;font-weight:700;padding:3px 9px;margin-left:6px">'+esc2(d)+'</span>';
  }

  function drCard(x, withAccept){
    var accepted=!!String(x.acceptedAt||'');
    var rail=accepted?'#0F766E':'#E0A800';
    function cell(l,v,last){
      return '<div style="flex:1;background:#FBFDFC;border:1px solid #E7ECEA;border-radius:13px;padding:6px 12px;margin-top:8px'+(last?'':';margin-right:8px')+'">'+
        '<div style="font-size:10.5px;color:#8B98A9;font-weight:700;letter-spacing:.8px">'+l+'</div>'+
        '<div style="font-size:12.5px;color:#0B2B1C;font-weight:700;margin-top:3px">'+(v||'—')+'</div></div>';
    }
    /* ✂️↔️ V1202 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"not accept yet sent by you
       (এই লেখাটা ওখানে থাকবে না) · cancel hide এই বক্স দুটো পাশাপাশি থাকবে,
       উচ্চতা আরো কম হবে"* ⇒ লেখাগুলো বাদ, বোতাম দুটো **সমান চওড়ায় পাশাপাশি**
       আর পাতলা। ⛔ কে Accept করতে পারে · কী সেভ হয় — কিছুই বদলায়নি।
       (ফোনের DoctorReminderActivity-র হুবহু যমজ।) */
    var BSTY='flex:1;padding:5px 10px;font-size:12.5px;min-width:0';
    var acts='';
    if(withAccept && canAccept(x)){
      acts='<button class="small" style="'+BSTY+'" onclick="drRemAccept(\''+esc2(x.id)+'\')">Accept</button>';
    } else if(accepted){
      acts='<span style="flex:1;color:#0A7C3F;font-weight:700;font-size:12px">Accepted  ·  '+esc2(x.acceptedByName||'')+'  ·  '+esc2(stamp(x.acceptedAt))+'</span>';
    }
    /* 🙈 V1193 — "Hide": শুধু এই ব্যক্তির হোম ও ঘন্টা থেকে সরে; তালিকা ও
       History-তে সারিটা অটুট থাকে, কেউ কিছু হারায় না। */
    if(withAccept && canCancel(x)){
      acts='<button class="small ghost" style="border-color:#C0392B;color:#C0392B;'+BSTY+'" onclick="drRemCancel(\''+esc2(x.id)+'\')">Cancel</button>';
    }
    if(withAccept) acts+='<button class="small ghost" style="'+BSTY+'" onclick="drRemHide(\''+esc2(x.id)+'\')">Hide</button>';
    return '<div style="display:flex;background:#fff;border:1px solid #E7ECEA;border-radius:16px;overflow:hidden;margin-bottom:10px">'+
      '<div style="width:6px;background:'+rail+'"></div>'+
      '<div style="flex:1;padding:14px 16px">'+
        '<div style="font-size:15px;font-weight:700;color:#0B2B1C">'+esc2(x.patientName||'Patient')+
          ' <span style="font-weight:400;color:#7A8794;font-size:13px">'+esc2(x.patientMobile||'')+'</span>'+
          drDisChip(x)+'</div>'+
        '<div style="margin-top:7px;background:#F6FAF7;border:1px solid #E2EDE6;border-radius:11px;padding:10px 12px;font-size:13.5px;color:#17212B">'+esc2(x.note||'')+'</div>'+
        '<div style="display:flex">'+cell('FOR',esc2(x.forName||'All doctors'),false)+
          cell('BY',esc2(x.byName||'')+(x.byBranch?(' · '+esc2(x.byBranch)):''),true)+'</div>'+
        (x.remindDate?('<div style="margin-top:8px;background:#FFFBF0;border:1px solid #F0E0BC;border-radius:14px;padding:10px 12px;font-size:12.5px;color:#B45309;font-weight:700">Remind on  '+esc2(dmy(x.remindDate))+(x.remindTime?('  ·  '+esc2(time12(x.remindTime))):'')+'</div>'):'')+
        '<div style="display:flex;align-items:center;gap:8px;margin-top:9px">'+acts+'</div>'+
      '</div></div>';
  }

  /** হোম পর্দার কার্ড — শুধু **এই ব্যক্তির জন্য অপেক্ষা করছে** এমনগুলো
      (V1193, TK-নির্দেশ)। কিছু না থাকলে কিছুই বসে না, আর উপরে **Hide**। */
  function drRemHomeCard(){
    setTimeout(function(){
      drRemBellRefresh().then(function(b){
        var el=document.getElementById('drRemHomeBox'); if(!el) return;
        var rows=(b&&b.waiting)||[];
        if(!rows.length){ el.innerHTML=''; return; }
        DR_ROWS=rows;
        el.innerHTML='<div class="card" style="padding:0;overflow:hidden">'+
          '<div style="background:#0B4F2A;color:#fff;padding:12px 16px;display:flex;align-items:center">'+
            '<b style="flex:1;font-size:14px;letter-spacing:.5px">DOCTOR NOTE &amp; REMINDER</b>'+
            '<span onclick="drRemHideAll(event)" style="border:1.5px solid #9FD3B6;border-radius:12px;padding:5px 14px;font-size:12px;font-weight:700;margin-right:9px;cursor:pointer">Hide</span>'+
            '<span style="background:#fff;color:#0B4F2A;border-radius:20px;padding:2px 12px;font-size:12.5px;font-weight:700">'+rows.length+'</span></div>'+
          '<div style="padding:12px 14px;cursor:pointer" onclick="drRemHome()">'+rows.slice(0,3).map(function(x){return drCard(x,false)}).join('')+
          '<div style="text-align:center;color:#0A5C33;font-weight:700;font-size:13.5px;padding:6px 0 2px">View all</div></div></div>';
      }).catch(function(){});
    }, 60);
    return '<div id="drRemHomeBox"></div>';
  }

  /** হোম-কার্ডের "Hide" — এই ব্যক্তির হোম ও ঘন্টা থেকেই শুধু সরে যায়। */
  async function drRemHideAll(ev){
    try{ ev.stopPropagation(); }catch(e){}
    var rows=((window.__DR_BELL||{}).waiting)||[];
    var m=me(); if(!m||typeof sb==='undefined'||!sb) return;
    for(var i=0;i<rows.length;i++){
      try{
        var next=listAdd(rows[i].hiddenBy,m);
        await sb.from(TABLE).update({hiddenBy:next}).eq('id',rows[i].id);
        rows[i].hiddenBy=next;
      }catch(e){}
    }
    try{ var el=document.getElementById('drRemHomeBox'); if(el) el.innerHTML=''; }catch(e){}
    await drRemBellRefresh();
    toast('Hidden from your Home & bell');
  }

  async function drRemAccept(id){
    if(typeof sb==='undefined'||!sb) return toast('No internet connection');
    try{
      var r=await sb.from(TABLE).update({acceptedBy:me(),acceptedByName:myName(),acceptedAt:nowIso()}).eq('id',id);
      if(r&&r.error) return toast('করা গেল না — আবার চেষ্টা করুন');
      toast('Accepted');
      drRemHome();
    }catch(e){ toast('করা গেল না — আবার চেষ্টা করুন'); }
  }

  async function drRemHome(){
    var rows=[]; try{ rows=await drRemLoad(false); }catch(e){}
    DR_ROWS=rows;
    /* 🎨 V1193 (TK-র পাশ-করা প্রুফ) — উপরে ছোট "+ New", একটাই কার্ড,
       আর **Back একদম নিচে** (TK-নির্দেশ: *"Back একদম ডিসপ্লের নিচে থাকবে"*)। */
    document.getElementById('app').innerHTML='<div class="wrap">'+
      /* 📏 V1202 (TK-নির্দেশ): *"doctor note and reminders এই লেখাটা আরও ছোট হবে ·
         + New উচ্চতা আরো কম হবে · +New ও ৩-ডটের মধ্যে গ্যাপ থাকবে আরো"*
         📏 V1571 (১৭.০৯.২০২৬, TK-নির্দেশ, Artifact ডেমো "A" পাশ) — ৫ → ৩px। */
      '<div class="topbar"><b style="font-size:15.5px">Doctor Note &amp; Reminder</b>'+
      '<button class="small" style="padding:3px 12px;font-size:12.5px" onclick="drRemNew()">+ New</button>'+
      '<span style="width:18px;display:inline-block"></span>'+
      /* V1194 (TK-নির্দেশ): "রিমাইন্ডার হিস্টরি উপরে ডান সাইডে ৩ ডট থাকবে
         তার মধ্যে থাকতে হবে" — ফোনের PopupMenu-র যমজ। */
      '<button class="ghost" style="min-width:0;padding:6px 12px;font-size:19px;font-weight:700" onclick="drRemMenu()">\u22EE</button></div>'+
      '<div class="page"><div class="card">'+
        '<div style="background:linear-gradient(90deg,#0B4F2A,#0F766E);border-radius:14px;padding:11px 16px;margin-bottom:10px;display:flex;align-items:center">'+
          '<b style="flex:1;color:#fff;font-size:13.5px;letter-spacing:.6px">WAITING NOW'+(rows.length?('   ('+rows.length+')'):'')+'</b></div>'+
        (rows.length? rows.map(function(x){return drCard(x,true)}).join('')
                    : '<div class="mut">No reminder right now.</div>')+
      '</div>'+
      '<button class="ghost" style="width:100%;margin-top:14px" onclick="dashboard()">Back</button>'+
      '</div></div>';
  }

  function drRemMenu(){
    try{ modal('<h2>Doctor Note &amp; Reminder</h2><div class="grid menuGrid">'+
      '<button class="menuBtn" onclick="closeModal();drRemHistory()"><b>Reminder History</b></button></div>'); }
    catch(e){ drRemHistory(); }
  }

  async function drRemHistory(){
    var rows=[]; try{ rows=await drRemLoad(true); }catch(e){}
    function cell(l,v){ return '<div style="flex:1;background:#FBFDFC;border:1px solid #EDF2EF;border-radius:11px;padding:9px 12px;margin:7px 7px 0 0">'+
      '<div style="font-size:10.5px;color:#8B98A9;letter-spacing:.8px">'+l+'</div>'+
      '<div style="font-size:13px;color:#0B2B1C;font-weight:700;margin-top:3px">'+(v||'—')+'</div></div>'; }
    var body=rows.map(function(x){
      var accepted=!!String(x.acceptedAt||'');
      var cancelled=!!String(x.cancelledAt||'');   /* V1194 */
      return '<div style="display:flex;background:#fff;border:1px solid #E7ECEA;border-radius:16px;overflow:hidden;margin-bottom:12px">'+
        '<div style="width:6px;background:'+(cancelled?'#C0392B':(accepted?'#0F766E':'#E0A800'))+'"></div>'+
        '<div style="flex:1;padding:14px 16px">'+
          '<div style="font-size:15px;font-weight:700;color:#0B2B1C">'+esc2(x.patientName||'Patient')+
            ' <span style="font-weight:400;color:#7A8794;font-size:13px">'+esc2(x.patientMobile||'')+'</span>'+drDisChip(x)+'</div>'+
          '<div style="margin-top:7px;background:#F6FAF7;border:1px solid #E2EDE6;border-radius:11px;padding:10px 12px;font-size:13.5px;color:#17212B">'+esc2(x.note||'')+'</div>'+
          '<div style="display:flex">'+cell('SENT BY', esc2(x.byName||'')+(x.byBranch?(' · '+esc2(x.byBranch)):''))+cell('SENT ON', esc2(stamp(x.createdAt)))+'</div>'+
          '<div style="display:flex">'+cell('SENT TO', esc2(x.forName||'All doctors'))+
            (cancelled? cell('CANCELLED BY', esc2(x.cancelledByName||'')+' · '+esc2(stamp(x.cancelledAt)))
                      : cell('ACCEPTED', accepted?(esc2(x.acceptedByName||'')+' · '+esc2(stamp(x.acceptedAt))):'Not yet'))+'</div>'+
          '<div style="margin-top:11px;display:flex;align-items:center;border-top:1px solid #EEF1F5;padding-top:11px">'+
            '<span style="font-size:12.5px;color:#5B6B81">Remind on</span>'+
            '<b style="font-size:13.5px;color:#0B2B1C;margin-left:8px">'+esc2(dmy(x.remindDate))+(x.remindTime?('  ·  '+esc2(time12(x.remindTime))):'')+'</b>'+
            '<span style="margin-left:auto;border-radius:12px;padding:5px 14px;font-size:12px;font-weight:700;'+
              (cancelled?'background:#FDECEA;color:#C0392B;border:1.5px solid #F3C4BE'
                        :(accepted?'background:#E8F6ED;color:#0A7C3F;border:1.5px solid #BFE3CD':'background:#FFF4E5;color:#8A5A00;border:1.5px solid #F0DCA8'))+'">'+
              (cancelled?'CANCELLED':(accepted?'ACCEPTED':'WAITING'))+'</span>'+
          '</div>'+
        '</div></div>';
    }).join('');
    document.getElementById('app').innerHTML='<div class="wrap"><div class="topbar"><b>Reminder History</b></div><div class="page">'+
      (rows.length?body:'<div class="card">Nothing yet.</div>')+
      '<button class="ghost" style="width:100%;margin-top:14px" onclick="drRemHome()">Back</button>'+
      '</div></div>';
  }

  var DR_PICKED=null, DR_FOR=['',''];
  /* 🎨🔒 V1193 (TK-র পাশ-করা ফটো-প্রুফ) — একটাই কার্ড, উপরে সোনালি পট্টি,
     প্রতিটা ঘর নিজের বাক্সে, দিন ও সময় পাশাপাশি, নিচে একটাই Send, আর
     **Back একদম পর্দার নিচে**। ফোনের `renderCreate()`-এর হুবহু যমজ।
     ⛔ যা সেভ হয় (`drRemSend`) তার এক অক্ষরও বদলায়নি — একই ঘর, একই সারি। */
  function drRemNew(){
    DR_PICKED=null; DR_FOR=['',''];
    /* 📏 V1202 (TK-নির্দেশ): *"প্রতিটা ঘর এবং প্রতিটা বক্সের উচ্চতা আরো কম হবে"*
       — ফোনের cellBox()-এর হুবহু যমজ মাপ (১১ → ৬px)। */
    var CELL='background:#FBFDFC;border:1px solid #E7ECEA;border-radius:13px;padding:6px 12px;margin-top:8px';
    var CAP='font-size:10.5px;color:#8B98A9;font-weight:700;letter-spacing:.8px';
    document.getElementById('app').innerHTML='<div class="wrap"><div class="topbar"><b>New Reminder</b></div>'+
      '<div class="page"><div class="card">'+
      '<div style="background:linear-gradient(90deg,#B45309,#E0A800);border-radius:14px;padding:11px 16px;display:flex;align-items:center;margin-bottom:6px">'+
        '<b style="flex:1;color:#fff;font-size:13.5px;letter-spacing:.6px">NEW REMINDER</b>'+
        '<span style="color:#FFF3D6;font-size:12px">'+esc2(dmy(todayIso()))+'</span></div>'+

      '<div style="'+CELL+'"><div style="'+CAP+'">PATIENT</div>'+
        '<div style="display:flex;align-items:center;gap:9px">'+
          '<input id="drPat" class="input" type="text" placeholder="Patient name or mobile" style="flex:1;margin:0" oninput="drRemSuggest()">'+
          '<button class="small ghost" onclick="drRemFind()">Find</button></div></div>'+
      '<div id="drPatSug"></div>'+
      '<div style="'+CELL+'"><div id="drPatOut" style="font-size:13.5px;color:#8A93A0">No patient chosen</div></div>'+

      '<div style="'+CELL+'"><div style="'+CAP+'">NOTE</div>'+
        '<input id="drNote" class="input" type="text" placeholder="What to give / what to do" style="margin:4px 0 0"></div>'+

      '<div style="'+CELL+'"><div style="'+CAP+'">SEND TO</div>'+
        '<select id="drFor" class="input" style="margin:4px 0 0"><option value="">All doctors</option></select></div>'+

      '<div style="display:flex;gap:9px">'+
        /* 📅🔒 V1201 (TK-রিপোর্ট: *"রিমাইন্ডার আবার অতীত কাল কি করে নির্বাচন করা হয়"*)
           — আজকের আগের দিন আর বাছা যায় না (ফোনেও হুবহু একই)। */
        '<div style="flex:1;'+CELL+'"><div style="'+CAP+'">REMIND DAY</div>'+
          '<input id="drDate" class="input" type="date" min="'+esc2(todayIso())+'" style="margin:4px 0 0"></div>'+
        '<div style="flex:1;'+CELL+'"><div style="'+CAP+'">TIME</div>'+
          '<input id="drTime" class="input" type="time" style="margin:4px 0 0"></div>'+
      '</div>'+
      '<div style="font-size:11.5px;color:#8B98A9;padding:8px 2px 0">Reminds the doctor the day before, and again at the chosen time.</div>'+

      '<div style="background:#FFFBF0;border:1px solid #F0E0BC;border-radius:14px;padding:11px 13px;margin-top:9px">'+
        '<div style="'+CAP+'">REMINDER BY</div>'+
        '<div style="font-size:13.5px;color:#8A5A00;font-weight:700;margin-top:3px">'+
          esc2(myName())+(myBranch()?('  ·  '+esc2(myBranch())):'')+'   (you)</div></div>'+

      '<button style="width:100%;margin-top:12px" onclick="drRemSend()">Send Reminder</button>'+
      '</div>'+
      '<button class="ghost" style="width:100%;margin-top:14px" onclick="drRemHome()">Back</button>'+
      '</div></div>';
    drRemFillDoctors(myBranch());
  }

  /* 🔎🔒 V1193 (TK-নির্দেশ, হুবহু): *"পেশেন্ট এর নাম 4 সংখ্যা দিলে সাজেশন কেন
     করে না"* — যাচাই করে দেখা গেল টাইপ করার সঙ্গে সঙ্গে সাজেশন দেখানোর
     ব্যবস্থা **কোনোদিনই ছিল না**, "Find Patient" চাপতেই হত। এখন ৩ অক্ষর
     লিখলেই নিজে থেকে তালিকা নামে।
     ⛔ খোঁজা হয় **ফোনে/ব্রাউজারে আগে থেকেই রাখা** রোগী-তালিকা থেকে —
        নতুন কোনো ক্লাউড-পড়া যোগ হয়নি। */
  var DR_SUG_T=null;
  function drRemSuggest(){
    if(DR_SUG_T) clearTimeout(DR_SUG_T);
    DR_SUG_T=setTimeout(function(){
      var box=document.getElementById('drPatSug'); if(!box) return;
      var q=String((document.getElementById('drPat')||{}).value||'').trim();
      if(q.length<3){ box.innerHTML=''; return; }
      var list=drRemSearch(q).slice(0,8);
      window.__drFound=list;
      if(!list.length){ box.innerHTML=''; return; }
      box.innerHTML=list.map(function(p,i){
        return '<div onclick="drRemPick('+i+')" style="background:#F2FBF5;border:1px solid #D8ECDF;border-radius:14px;padding:10px 13px;margin-top:8px;cursor:pointer">'+
          '<div style="font-size:13.5px;font-weight:700;color:#0B2B1C">'+esc2(p.name||'')+'</div>'+
          '<div style="font-size:11.5px;color:#4A6B58;margin-top:2px">'+esc2(p.mobile||'')+'  ·  '+esc2(p.branch||'')+(p.disease?('  ·  '+esc2(p.disease)):'')+'</div></div>';
      }).join('');
    }, 300);
  }

  /** নাম বা নম্বরের টুকরো ধরে রোগী — "Find" ও সাজেশন দুটোই এই একটাই নিয়মে। */
  function drRemSearch(q){
    var digits=String(q||'').replace(/\D/g,'');
    try{
      return (load('patients')||[]).filter(function(p){
        if(digits.length>=4) return d10(p.mobile).indexOf(digits.slice(-10))>=0 || String(p.mobile||'').indexOf(digits)>=0;
        return String(p.name||'').toLowerCase().indexOf(String(q).toLowerCase())>=0;
      }).slice(0,25);
    }catch(e){ return []; }
  }

  function drRemFillDoctors(branch){
    var sel=document.getElementById('drFor'); if(!sel) return;
    var docs=doctorsForBranch(branch);
    sel.innerHTML='<option value="">All doctors</option>'+
      docs.map(function(x){ return '<option value="'+esc2(x[1])+'">'+esc2(x[0])+'</option>'; }).join('');
  }

  function drRemFind(){
    var q=String((document.getElementById('drPat')||{}).value||'').trim();
    if(q.length<3) return toast('Type at least 3 letters or digits');
    var list=drRemSearch(q);
    if(!list.length) return toast('No patient found');
    var rows=list.map(function(p,i){
      return '<button class="menuBtn" onclick="drRemPick('+i+')"><b>'+esc2(p.name||'')+'</b><small>'+esc2(p.mobile||'')+' · '+esc2(p.branch||'')+(p.disease?(' · '+esc2(p.disease)):'')+'</small></button>';
    }).join('');
    window.__drFound=list;
    try{ modal('<h2>Choose patient</h2><div class="grid menuGrid">'+rows+'</div>'); }catch(e){}
  }
  function drRemPick(i){
    var p=(window.__drFound||[])[i]; if(!p) return;
    DR_PICKED=p;
    var out=document.getElementById('drPatOut');
    if(out){ out.textContent=(p.name||'')+'   '+(p.mobile||'')+(p.branch?('   ·   '+p.branch):'')+(p.disease?('   ·   '+p.disease):''); out.style.color='#0B2B1C'; }
    try{ var sug=document.getElementById('drPatSug'); if(sug) sug.innerHTML=''; }catch(e){}
    drRemFillDoctors(p.branch||myBranch());
    try{ closeModal(); }catch(e){}
  }

  async function drRemSend(){
    if(!DR_PICKED) return toast('Choose a patient first');
    var note=String((document.getElementById('drNote')||{}).value||'').trim();
    if(!note) return toast('Write the note');
    var date=String((document.getElementById('drDate')||{}).value||'').slice(0,10);
    if(!date) return toast('Pick the date');
    if(date < todayIso()) return toast('Past date cannot be chosen');   /* 📅 V1201 */
    var time=String((document.getElementById('drTime')||{}).value||'').slice(0,5);
    var sel=document.getElementById('drFor');
    var forMob=sel?String(sel.value||''):'';
    var forName=sel&&sel.selectedIndex>0?sel.options[sel.selectedIndex].text:'';
    if(typeof sb==='undefined'||!sb) return toast('No internet connection');
    var row={
      id:'drem_'+Date.now()+'_'+Math.floor(Math.random()*1000),
      patientId:String(DR_PICKED.id||''), patientName:String(DR_PICKED.name||''),
      patientMobile:d10(DR_PICKED.mobile), branch:String(DR_PICKED.branch||myBranch()),
      disease:String(DR_PICKED.disease||''),      /* V1194 */
      note:note, forMobile:d10(forMob), forName:forName,
      byMobile:me(), byName:myName(), byBranch:myBranch(),
      remindDate:date, remindTime:time, createdAt:nowIso(),
      acceptedBy:'', acceptedByName:'', acceptedAt:'',
      cancelledBy:'', cancelledByName:'', cancelledAt:'', active:true
    };
    try{
      var r=await sb.from(TABLE).upsert(row);
      if(r&&r.error) return toast('পাঠানো গেল না — আবার চেষ্টা করুন');
      toast('Sent');
      drRemHome();
    }catch(e){ toast('পাঠানো গেল না — আবার চেষ্টা করুন'); }
  }

  window.drRemHome=drRemHome; window.drRemHistory=drRemHistory; window.drRemNew=drRemNew;
  window.drRemAccept=drRemAccept; window.drRemFind=drRemFind; window.drRemPick=drRemPick;
  window.drRemSend=drRemSend; window.drRemHomeCard=drRemHomeCard;
  window.drRemHide=drRemHide; window.drRemHideAll=drRemHideAll; window.drRemAck=drRemAck;
  window.drRemSuggest=drRemSuggest; window.drRemSearch=drRemSearch;
  window.drRemCancel=drRemCancel; window.drRemMenu=drRemMenu; window.drDisease=drDisease;
  window.drRemBellRefresh=drRemBellRefresh; window.drRemBellCount=drRemBellCount;
})();
