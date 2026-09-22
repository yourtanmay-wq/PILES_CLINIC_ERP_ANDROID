/* =====================================================================
   V245 MODULE 1 — PROFESSIONAL PROFILE & SALARY  · additive, isolated
   Master: view/edit every profile + salary + record payments.
   Staff/Doctor/Field: view ONLY their own profile + salary history.
   Data lives in schema `hr` (RLS enforced). Numbers displayed masked.
   ===================================================================== */
(function () {
  function sb() { return window.MOD.client(); }
  // 🔴 V404 (16.08.2026): বার্তা দেখানোর নিরাপদ মোড়ক। `toast` আসে app.js থেকে
  //    (গ্লোবাল) — MOD-এ toast নেই, তাই `MOD.toast(...)` ডাকলে পর্দা ভেঙে যেত।
  //    এখানে ধরা আছে; না পাওয়া গেলেও কিছু ভাঙবে না।
  function pfToast(msg) { try { toast(msg); } catch (e) { try { console.log(msg); } catch (_e) {} } }

  // ছবি (V252, TK-অনুরোধে): existing app.js-এর fileData() পুনর্ব্যবহার (resize+base64),
  // নতুন কোনো Storage bucket/লাইব্রেরি লাগেনি। ৩-বার চাপে তবেই বদলানো যায় (MOD.tripleTap)।
  var __currentPhoto = null;  // edit খোলার সময় আগে থেকে সেভ করা ছবি
  var __pendingPhoto = null;  // এই সেশনে নতুন বাছা ছবি (এখনো সেভ হয়নি)

  // Master dashboard card entry point
  async function staffProfiles() { window.MOD.gate('Staff Profiles', renderMaster); }
  // Staff More-Menu entry point
  async function myProfile() { window.MOD.gate('My Profile', renderSelf); }

  async function renderMaster() {
    var m = window.MOD;
    if (!m.isMasterModule()) { return renderSelf(); }
    var host = document.getElementById('app');
    host.innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>🧑‍💼 Staff Profiles</b>' +
      '<button class="ghost" title="More" onclick="pfMoreMenu()">⋮</button>' +
      '<button class="ghost" onclick="dashboard()">Home</button></div>' +
      '<div class="page"><div id="spList" class="mut">Loading...</div></div></div>';
    var client = await sb();
    var rows = [];
    // 🔴 বাগ-ফিক্স (02.08.2026, TK-রিপোর্ট Supabase খরচ বেশি দেখে ধরা পড়েছে):
    // এই তালিকায় ছবি দেখানো হয় না, অথচ select('*') সবার photo_data (৫০-১০০ KB
    // প্রতিটা) সহ পুরো রেকর্ড টানত। এখন শুধু দরকারি কলামই টানা হচ্ছে।
    // 🔵 TK-ORDER (07.08.2026): পড়া ব্যর্থ হলে "No profiles." নয় (আগে দেখাত) — সৎ
    // "লোড করা গেল না" বার্তা, যাতে সত্যিই তালিকা খালি নাকি নেট-সমস্যা বোঝা যায়।
    var listFailed = false;
    try { var __rp = await client.schema('hr').from('staff_profiles')
      .select('person_code,designation,role_kind,branch,full_name,link_mobile,active,join_date')   /* 🔴 V1140 — join_date-ও লাগে */
      .order('person_code'); if (__rp && __rp.error) listFailed = true; rows = __rp.data || []; } catch (e) { listFailed = true; }
    /* ⛔🔒 V890 (৩০.০৮.২০২৬, TK-নির্দেশ) — বাদ দেওয়া স্টাফের একটাও তথ্য
       কোথাও দেখাবে না। ফোনের `BlockedStaff`-এর হুবহু একই তালিকা।
       ⛔ রোগীর তথ্য/টাকা কিছুই মোছে না — শুধু ওই ব্যক্তি দেখা যায় না। */
    var WLV1_BLOCKED_MOB = ['9339139852'];          // BIR-5 · RESAM KHATUN
    var WLV1_BLOCKED_CODE = ['BIR-5'];
    rows = (rows || []).filter(function (r) {
      var m = String((r && r.link_mobile) || '').replace(/[^0-9]/g, '').slice(-10);
      var c = String((r && r.person_code) || '').trim().toUpperCase();
      return WLV1_BLOCKED_MOB.indexOf(m) < 0 && WLV1_BLOCKED_CODE.indexOf(c) < 0;
    });
    // 🔴 V404 (16.08.2026, TK-নির্দেশ): বাদ-দেওয়া কর্মী (active = false) মূল
    //    তালিকায় ও "Salary Due"-তে আর আসবে না। আগে আসত — SWAPNA ADHIKARI
    //    কাজ ছেড়ে দেওয়ার পরেও তাঁর নাম উঠত।
    //    ⛔ চুপচাপ লুকোনো হচ্ছে না — নিচে আলাদা "Removed Staff" বাক্সে গোনা
    //       থাকবে, ভুল হলে Restore করা যাবে।
    //    ⚠️ `active` ঘরটা না থাকলে (পুরনো সারি) coalesce-এর মতোই সচল ধরা হয়।
    /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬) — ফোনে এই তালিকায় **শুধু কর্মী**
       দেখানো হয় (StaffProfileActivity.kt:188 — role_kind ≠ staff হলে বাদ)।
       ওয়েবে ডাক্তার ও মাস্টারের সারিও উঠে আসত, তাই দুই জায়গায় তালিকা আলাদা
       দেখাত। ⛔ কারও তথ্য মোছা হয় না — শুধু এই এক পর্দায় দেখানো হয় না। */
    var __nameFallback = {'DR-JH-MANDAL':'J.H. MANDAL','DR-GOKUL':'GOKUL','DR-PRANAB-BISWAS':'PRANAB BISWAS','DR-SAIKAT-ROY':'SAIKAT ROY','DR-JAY-BANIK':'JAY BANIK','DR-KH-MANDAL':'J.H. MANDAL','DR-PK-ROY':'SAIKAT ROY'};
    rows.forEach(function(p){ if (!String(p.full_name||'').trim()) p.full_name=__nameFallback[String(p.person_code||'').toUpperCase()]||p.person_code; });
    /* 🔵🔒🔒 V828 (২৯.০৮.২০২৬, TK-অনুমোদিত: *"ঠিক আছে খুব সাবধানে করুন"*) —
       **Staff Profiles-এ ফোন ও কম্পিউটার এখন হুবহু একই ক্রমে সাজায়।**

       ─── আগে কী ভুল ছিল (কোড ধরে যাচাই করা) ─────────────────────────────
       এখানে ব্রাঞ্চের ক্রমটা **হাতে লেখা একটা আলাদা তালিকা** ছিল —
       ['Jalpaiguri','Cooch Behar','Falakata','Kishanganj'] — আর তাতে
       **Birpara ছিলই না**। অথচ ফোনে (`StaffProfileActivity.kt:469`) ক্রমটা
       আসে প্রজেক্টের একটাই আসল তালিকা `BranchFilterStore.BRANCHES` থেকে
       (Kishanganj · Jalpaiguri · Cooch Behar · Falakata · Birpara)।
       ⇒ একই পর্দা দুই জায়গায় দুই রকম দেখাত, আর Birpara-র স্টাফ
         "অচেনা ব্রাঞ্চ" (৯৯) হিসেবে সবার শেষে পড়তেন।

       ─── এখন কী হলো ────────────────────────────────────────────────────
       ক্রমটা আর হাতে লেখা নয় — ওয়েবের **নিজের আসল তালিকা**
       `RK_CONFIG.branches` (config.js) থেকেই আসে, যার ক্রম ফোনের
       `BranchFilterStore.BRANCHES`-এর সঙ্গে হুবহু মেলে (দুটোই যাচাই করা)।
       ⇒ ভবিষ্যতে নতুন ব্রাঞ্চ যোগ হলে **এখানে আর হাত দিতেই হবে না** —
         config.js-এ বসালেই এই পর্দাও নিজে থেকে ঠিক ক্রমে সাজাবে।

       ⛔ শুধু **সাজানোর ক্রম** — কে তালিকায় থাকবেন, কার কী তথ্য দেখাবে,
          ডাক্তার-ছাঁকনি (V430) — কিচ্ছু বদলায়নি।
       ⛔ `RK_CONFIG` কোনো কারণে না পাওয়া গেলে (বা ফাঁকা হলে) আগের হাতে-লেখা
          তালিকাটাই ব্যবহার হয় — তাই পর্দা কখনো ভাঙবে না। */
    var __branchOrder = (function(){
      try{
        var c = (window.RK_CONFIG && window.RK_CONFIG.branches) || [];
        var names = c.map(function(b){ return String((b && b.name) || '').trim(); })
                     .filter(function(n){ return n; });
        if (names.length) return names;
      }catch(e){}
      return ['Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara'];
    })();
    rows.sort(function(a,b){
      var ar=String(a.role_kind||'').toLowerCase(), br=String(b.role_kind||'').toLowerCase();
      var ag=ar==='staff'?0:(ar==='doctor'?1:2), bg=br==='staff'?0:(br==='doctor'?1:2);
      if(ag!==bg)return ag-bg;
      if(ag===0){var ai=__branchOrder.indexOf(a.branch),bi=__branchOrder.indexOf(b.branch);ai=ai<0?99:ai;bi=bi<0?99:bi;if(ai!==bi)return ai-bi;}
      return String(a.full_name||a.person_code).localeCompare(String(b.full_name||b.person_code));
    });
    var removedRows = rows.filter(function (p) { return p.active === false; });
    rows = rows.filter(function (p) { return p.active !== false; });
    var cfgs = {};
    try {
      (await client.schema('hr').from('salary_config').select('*')).data.forEach(function (c) { cfgs[c.person_code] = c; });
    } catch (e) {}
    // 🟢 B629: স্যালারির তারিখ পেরিয়েছে অথচ এ মাসে দেওয়া হয়নি — এমন স্টাফ থাকলে উপরে "Salary Due"
    var paysByCode = {};
    try {
      /* 🔵🔒 V818 (২৯.০৮.২০২৬, TK-নির্দেশে Egress-এর পূর্ণ যাচাই) — আগে এখানে
         **সব কর্মীর জীবনের সব বেতন-লেনদেন** নামত, কোনো সীমা ছাড়া; প্রতি মাসে
         তালিকাটা বাড়তেই থাকত। অথচ নিচের `salaryDueThisMonth()` শুধু
         **চলতি মাসের** সারিই দেখে (`salPayMonth(p)===cur`)।
         ⇒ শেষ ৬ মাসে ছেঁকে নেওয়া সম্পূর্ণ নিরাপদ — হিসাব এক পয়সাও বদলায় না। */
      var __since = new Date(); __since.setMonth(__since.getMonth() - 6);
      var __sinceIso = __since.toISOString().slice(0, 10);
      var __pa = (await client.schema('hr').from('salary_payments')
        .select('person_code,amount,paid_on,for_month')
        .gte('paid_on', __sinceIso)).data || [];
      __pa.forEach(function (p) { (paysByCode[p.person_code] = paysByCode[p.person_code] || []).push(p); });
    } catch (e) {}
    var dueRows = [];
    rows.forEach(function (p) { var sc = cfgs[p.person_code] || {}; var d = salaryDueThisMonth(sc, paysByCode[p.person_code] || [], p.join_date);   /* 🔴 V1140 */ if (d > 0) dueRows.push({ code: p.person_code, name: p.full_name || p.person_code, branch: p.branch || '', sd: sc.salary_date, amt: d }); });
    var dueHtml = dueRows.length ? ('<div class="card" style="border:1px solid #ffd58a;background:#fff7e6"><b>💰 Salary Due (' + dueRows.length + ')</b>' +
      dueRows.map(function (d) { return '<div style="padding:8px 0;border-top:1px solid #f0e2c0"><b>' + m.esc(d.name) + '</b> · ' + m.esc(d.branch) +
        /* 🔴 V430 — ফোনের লেখা: "Salary day 5 · due this month · ₹5,000"
           (BriefingActivity.kt:945-976)। ওয়েবের পুরনো লেখাটা নির্দেশ-ধাঁচের ছিল। */
        /* 🐞🔒 V1031 (TK-রিপোর্ট: *"একটা একটার গায় ঘেসে যাচ্ছে"*) — লেখাটা আর
           বোতামটা পাশাপাশি বসত মাত্র একটা ফাঁকা-অক্ষরের দূরত্বে, তাই ₹8,000-এর
           গায়ে "Pay Salary" লেগে থাকত। এখন নিজের সারিতে, মাঝে সত্যিকারের ফাঁক,
           আর জায়গা না হলে বোতামটা নিচে নামে। ⛔ লেখা/কাজ কিছুই বদলায়নি। */
        '<div class="pfDueLine"><span class="tiny mut">Salary day ' + m.esc(d.sd || '-') + ' · ' + m.esc(salMonthLabel(salDueMonth())) + ' due · ' + m.money(d.amt) + '</span>' +
        '<button class="small" onclick="profSalary(\'' + m.esc(d.code) + '\')">➕ Pay Salary</button></div></div>'; }).join('') + '</div>') : '';
    /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬: "সব কিছু Android এর মত হোক") — কর্মীর
       কার্ডটা হুবহু ফোনের মতো করা হলো (StaffProfileActivity.kt:251-327):
         · প্রথম লাইনে **নাম** (মোটা) আর পাশে **পদবির রঙিন চিপ**
           (স্টাফ সবুজ #EAF6EE/#0B8A3E · ডাক্তার বেগুনি #EFEAFB/#6A3FCB)
         · দ্বিতীয় লাইনে কোড · ব্রাঞ্চ · ঢাকা মোবাইল
         · তৃতীয় লাইনে বেতন — চালু থাকলে সবুজ, বন্ধ থাকলে ধূসর
         · বোতাম: View (ফাঁপা) · Salary (ভরাট সবুজ) · Performance · Suspend · Remove
       আগে ওয়েবে কোড আগে ছিল, নাম পরে; পদবির চিপ ছিলই না; ভরাট বোতাম ছিল
       "Edit"; আর বেতনের রং সবসময় এক ছিল। ⛔ কোনো বোতামের কাজ বদলায়নি। */
    /* 🔵🔒 V521: কার্ডে চাপ = View। কিন্তু ভিতরের কোনো বোতামে (Salary /
       Performance / Suspend / Remove) চাপ পড়লে কার্ডের চাপটা **চলবে না** —
       নইলে Salary চাপলে ভুল করে View খুলে যেত। */
    /* \u{1F3C6}\u{1F512} V1091 (\u09e6\u09eb.\u09e6\u09ef.\u09e8\u09e6\u09e8\u09ec, TK-\u09a8\u09bf\u09b0\u09cd\u09a6\u09c7\u09b6: *"\u0995\u09be\u09b0\u09cd\u09a1\u09c7 \u099a\u09be\u09aa \u09a6\u09bf\u09b2\u09c7
       \u09aa\u09be\u09b0\u09ab\u09b0\u09ae\u09c7\u09a8\u09cd\u09b8 \u0996\u09c1\u09b2\u09ac\u09c7… \u09b8\u09be\u09ae\u09a8\u09c7 Performance \u09b2\u09c7\u0996\u09be \u09a8\u09be \u09a5\u09be\u0995\u09b2\u09c7\u0993 \u099a\u09b2\u09ac\u09c7,
       \u09a1\u09be\u09a8\u09a6\u09bf\u0995\u09c7 \u09a5\u09cd\u09b0\u09bf \u09a1\u099f\u09c7\u09b0 \u09ae\u09a7\u09cd\u09af\u09c7 \u09a5\u09be\u0995\u09ac\u09c7"*) \u2014 \u09ab\u09cb\u09a8\u09c7\u09b0 \u09b9\u09c1\u09ac\u09b9\u09c1 \u098f\u0995\u0987 \u09ac\u09a6\u09b2\u0964
       \u26d4 \u09aa\u09cd\u09b0\u09cb\u09ab\u09be\u0987\u09b2 \u09b9\u09be\u09b0\u09be\u09df\u09a8\u09bf \u2014 \u22ee \u09ae\u09c7\u09a8\u09c1\u09a4\u09c7 "View profile" \u0986\u0997\u09c7 \u09a5\u09c7\u0995\u09c7\u0987 \u0986\u099b\u09c7\u0964
       \u26d4 \u09ad\u09bf\u09a4\u09b0\u09c7\u09b0 \u09ac\u09cb\u09a4\u09be\u09ae\u09c7 \u099a\u09be\u09aa \u09aa\u09dc\u09b2\u09c7 \u0995\u09be\u09b0\u09cd\u09a1\u09c7\u09b0 \u099a\u09be\u09aa\u099f\u09be \u0986\u0997\u09c7\u09b0 \u09ae\u09a4\u09cb\u0987 \u099a\u09b2\u09c7 \u09a8\u09be\u0964 */
    function pfCardTap(ev, code) {
      try {
        var t = ev && ev.target;
        if (t && t.closest && t.closest('button')) return;
        if (typeof staffPerformanceOne === 'function') staffPerformanceOne(code);
        else profEdit(code);
      } catch (e) {}
    }
    window["pfCardTap"] = pfCardTap;

    var listHtml = rows.map(function (p) {
      var sc = cfgs[p.person_code] || {};
      var isDoc = String(p.role_kind || '').toLowerCase() === 'doctor';
      var desig = p.designation || p.role_kind || (isDoc ? 'Doctor' : 'Staff');
      var salOn = !!sc.salary_enabled;
      /* 🔒👁️ V1321 (TK-নির্দেশ ১০.০৯.২০২৬, ডেমো-প্রুফ পাশ) — টাকার অঙ্কটা
         ডিফল্টে ডট দিয়ে ঢাকা, পাশের 👁 (বোতাম, তাই কার্ডের onclick-এ চাপে না)
         চাপলে দেখা যায়। "Salary day: N" আগের মতোই খোলা। */
      var salInner;
      if (!salOn) {
        salInner = m.esc('Salary: disabled');
      } else {
        var __salId = 'pfSalAmt_' + m.esc(p.person_code);
        var __realAmt = m.esc(m.money(sc.salary_amount));
        salInner = '<span>Salary: </span><span id="' + __salId + '" data-real="' + __realAmt + '" data-shown="0">••••••</span>' +
          '<button class="pfEyeBtn" onclick="pfToggleSalary(\'' + __salId + '\')">👁</button>' +
          '<span>  •  Salary day: ' + m.esc(sc.salary_date || '-') + '</span>';
      }
      /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — *"এই কার্ডের মধ্যে ভিউ থাকবে না,
         কিন্তু কার্ডে চাপ দিলে ভিউ হবে।"* ⇒ "View" বোতাম সরানো; কাজটা এখন
         পুরো কার্ডে চাপ দিলেই হয় (ফোনের অ্যাপে হুবহু একই বদল)।
         ⛔ ভিতরের বোতামে চাপ দিলে যেন কার্ডের চাপটা **না** চলে — তাই
            `event.stopPropagation()` ছাড়াই কাজ হয়, কারণ কার্ডের হ্যান্ডলার
            নিজেই দেখে নেয় চাপটা কোনো বোতামের উপরে পড়েছে কি না। */
      return '<div class="card pfStaffCard" style="cursor:pointer"' +
        ' onclick="pfCardTap(event,\'' + m.esc(p.person_code) + '\')">' +
        /* 🗑️ V1059 (TK: *"নামের short থাকবে না"*) — গোল আদ্যক্ষর-ব্যাজটা তোলা হলো। */
        '<div class="pfStaffInfo">' +
        '<div class="pfNameRow"><b class="pfName">' + m.esc(p.full_name || '(name not set)') + '</b>' +
        '<span class="pfPill' + (isDoc ? ' pfPillDoc' : '') + '">' + m.esc(desig) + '</span></div>' +
        '<div class="pfMeta">' + m.esc(p.person_code) + ' · ' + m.esc(p.branch || '') + ' · ' + m.esc(m.fullMobile(p.link_mobile)) + '</div>' +
        '<div class="pfSal' + (salOn ? '' : ' pfSalOff') + '">' + salInner + '</div></div>' +
        /* 🎨 V1057 — TK-এর ছবির ⋮; চাপলে কার্ডে চাপ দিলে যা হয় ঠিক তাই। */
        /* ⋮🔒 V1058 (TK-নির্দেশ) — ⋮-এ Suspend · Remove (ও View); কার্ডে থাকে শুধু
           Salary · Performance · Extra Income। ⛔ ফোনে এই মেনুতে **Fix Attendance**-ও
           আছে; কম্পিউটারে ওই পর্দাটা কোনোদিনই ছিল না, তাই এখানে নেই — সৎভাবে বলা। */
        '<button class="pfMore" title="More" onclick="event.stopPropagation();profDots(\'' + m.esc(p.person_code) + '\',\'' + m.esc(String(p.full_name||'')) + '\')">\u22EE</button>' +
        '<div class="pfStaffBtns">' +
        '<button class="small pfBtn pfBtnFill" onclick="profSalary(\'' + m.esc(p.person_code) + '\')">Salary</button>' +
        /* \u{1F3C6} V1091 \u2014 "Performance" \u09ac\u09cb\u09a4\u09be\u09ae\u099f\u09be \u0995\u09be\u09b0\u09cd\u09a1 \u09a5\u09c7\u0995\u09c7 \u09a4\u09c1\u09b2\u09c7 \u09a6\u09c7\u0993\u09df\u09be \u09b9\u09b2\u09cb \u2014
           \u098f\u0996\u09a8 \u0995\u09be\u09b0\u09cd\u09a1\u09c7 \u099a\u09be\u09aa \u09a6\u09bf\u09b2\u09c7\u0987 \u0996\u09cb\u09b2\u09c7, \u0986\u09b0 \u22ee \u09ae\u09c7\u09a8\u09c1\u09a4\u09c7\u0993 \u0986\u099b\u09c7\u0964 */
        /* 🏍️🔒 V978 (০২.০৯.২০২৬, TK-নির্দেশ: *"ওই সারিতেই বসিয়ে দিন"*) — বাইরে
           ঘোরা স্টাফের কার্ডেই Field Visit বোতাম, ফোনের হুবহু জোড়া।
           ⛔ অন্য কারো কার্ডে ওঠে না; বাকি বোতাম অপরিবর্তিত। */
        (WLV1_FIELD_STAFF_CODES.indexOf(String(p.person_code||'').toUpperCase()) >= 0
          ? '<button class="small ghost pfBtn" onclick="profFieldVisit(\'' + m.esc(p.person_code) + '\')">Field Visit</button>' : '') +
        /* 💰🔒 V1029 (TK-নির্দেশ: *"salary সহ যে পাঁচটা বটম আছে সেখানেই এক্সট্রা
           ইনকামটা রাখতে বলা হয়েছিল"*) — এই সারিতেই বোতাম; চাপলে সেই স্টাফের
           বেতন-পর্দা খোলে, যেখানে Extra Income-এর বাক্সটাই আছে।
           ⛔ টাকার কোনো অঙ্ক/নিয়ম ছোঁয়া হয়নি — শুধু পৌঁছনোর পথ। */
        '<button class="small ghost pfBtn" onclick="profSalary(\'' + m.esc(p.person_code) + '\')">Extra Income</button>' +
        '</div></div>';
    }).join('');
    // ⛔ V404: আগের `... || 'No profiles.'` লেখাটা এখানেই রাখা হলো, কিন্তু শুধু
    //    সচল তালিকার উপরে — নইলে সবাই বাদ হয়ে গেলে "No profiles." আর
    //    Removed-বাক্স একসাথে গুলিয়ে যেত।
    if (!listHtml) listHtml = '<div class="card mut">' + (listFailed ? 'Could not load. Please try again.' : 'No profiles.') + '</div>';
    /* 📋🔒 V1321 (TK-নির্দেশ ১০.০৯.২০২৬, ডেমো-প্রুফ পাশ) — আগে এখানে দুটো
       বড় বোতাম ("🏆 Staff Performance" ও "➕ Add Staff or Doctor") আলাদা
       কার্ড হয়ে থাকত; এখন দুটোই উপরের টপবারের ⋮ (pfMoreMenu) চাপলে খোলে।
       ⛔ ফোনে যেমন "📱 Mobile Version" আলাদা একটা বোতাম ছিল, ওয়েবে সেটা
          কোনোদিনই আলাদা বোতাম ছিল না (V813 অনুযায়ী ভার্সন এখানে Staff
          Performance-এর ভিতরেই নামের নিচে ছোট ট্যাগ হয়ে দেখায়) — তাই ওয়েবে
          মেনুতে নতুন করে "Mobile Version" যোগ করার কিছু নেই, সৎভাবে জানানো
          হলো। ⛔ কোনো তথ্য/পাহারা/SQL বদলায়নি — শুধু বোতাম দুটোর জায়গা। */
    var listWrap = rows.length ? ('<div class="pfGrid">' + listHtml + '</div>') : listHtml;
    document.getElementById('spList').innerHTML = dueHtml + listWrap + removedHtml();

    // 🔴 V404: বাদ-দেওয়া কর্মীদের ছোট তালিকা — গোনা থাকে, ভুল হলে Restore।
    function removedHtml() {
      if (!removedRows.length) return '';
      return '<details class="card" style="border:1px solid #e5e5e5;background:#fafafa"><summary style="cursor:pointer;font-weight:700">Removed Staff (' + removedRows.length + ')</summary>' +
                removedRows.map(function (p) {
          /* 🔴 V430 — ফোনে বাদ-দেওয়া কর্মীও **পুরো কার্ড** হিসেবেই দেখায়
             (নাম + পদবির চিপ + কোড·ব্রাঞ্চ·মোবাইল + Restore)। */
          var __isDoc = String(p.role_kind || '').toLowerCase() === 'doctor';
          return '<div class="pfStaffCard pfRemovedRow"><div class="pfStaffInfo">' +
            '<div class="pfNameRow"><b class="pfName">' + m.esc(p.full_name || '(name not set)') + '</b>' +
            '<span class="pfPill' + (__isDoc ? ' pfPillDoc' : '') + '">' + m.esc(p.designation || p.role_kind || 'Staff') + '</span></div>' +
            '<div class="pfMeta">' + m.esc(p.person_code) + ' · ' + m.esc(p.branch || '') + ' · ' + m.esc(m.fullMobile(p.link_mobile)) + '</div>' +
            '<div class="pfSal pfSalOff">Salary: disabled</div></div>' +
            '<div class="pfStaffBtns"><button class="small ghost pfBtn" onclick="profRestore(\'' + m.esc(p.person_code) + '\')">Restore</button></div></div>';
        }).join('') + '</details>';
    }
  }

  /* 📋🔒 V1321 (TK-নির্দেশ ১০.০৯.২০২৬) — Staff Profiles-এর উপরের ⋮। */
  function pfMoreMenu() {
    var m = window.MOD;
    modal('<h2>Staff Profiles</h2><div class="card pfDotsMenu">'
      + '<button class="ghost" onclick="closeModal();staffPerformance()">🏆 Staff Performance</button>'
      + '<button class="ghost" onclick="closeModal();pfAddPerson()">➕ Add Staff / Doctor</button>'
      + '</div><div class="actions"><button class="ghost" onclick="closeModal()">Close</button></div>');
  }
  window.pfMoreMenu = pfMoreMenu;

  /* 🔒👁️ V1321 — কার্ডের Salary-ডট চাপলে দেখা/ঢাকা টগল। */
  function pfToggleSalary(id) {
    try {
      var el = document.getElementById(id);
      if (!el) return;
      if (el.getAttribute('data-shown') === '1') {
        el.textContent = '••••••';
        el.setAttribute('data-shown', '0');
      } else {
        el.textContent = el.getAttribute('data-real') || '';
        el.setAttribute('data-shown', '1');
      }
    } catch (e) {}
  }
  window.pfToggleSalary = pfToggleSalary;

  /* 🔒👁️ V1321 — Salary Statement-এর Salary paid (total) ও Grand total
     paid একসাথে টগল হয় (Android-এর মতোই)। */
  function pfStmtToggleMoney() {
    try {
      var ids = ['pfStmtSalVal', 'pfStmtGrandVal'];
      var first = document.getElementById(ids[0]);
      var shown = !!(first && first.getAttribute('data-shown') === '1');
      ids.forEach(function (id) {
        var el = document.getElementById(id); if (!el) return;
        if (shown) { el.textContent = '••••••'; el.setAttribute('data-shown', '0'); }
        else { el.textContent = el.getAttribute('data-real') || ''; el.setAttribute('data-shown', '1'); }
      });
    } catch (e) {}
  }
  window.pfStmtToggleMoney = pfStmtToggleMoney;

  /* 👥🔒 V750 — নতুন স্টাফ বা ডাক্তার (ফোনের addPersonDialog-এর হুবহু সঙ্গী)।
     ⛔ এই ফাংশন নিজে **কোনো নিয়ম যাচাই করে না** — সব পাহারা সার্ভারে
        (`hr.admin_create_person`): শুধু মাস্টার · master ভূমিকা বানানো যায় না ·
        মোবাইল ১০ অঙ্ক · একই মোবাইল অন্য কারও নয় · কোড আগে থেকে অন্য কারও নয়।
     ⛔ ব্রাঞ্চ **হাতে লেখা যায় না** — config.js-এর তালিকা থেকেই বাছতে হয়
        (ফোনেও ঠিক একই, V747; বানান ভুল হলে ভুল ব্রাঞ্চে বসে যেত)। */
  async function pfAddPerson() {
    var m = window.MOD;
    if (!m.isMasterModule()) return pfToast('Only Master');
    var brs = [];
    try { brs = ((window.RK_CONFIG || C || {}).branches || []).map(function (b) { return String(b.name || ''); }); } catch (e) { brs = []; }
    brs = brs.filter(function (x) { return x; });
    if (!brs.length) brs = ['Kishanganj', 'Jalpaiguri', 'Cooch Behar', 'Falakata', 'Birpara'];
    var host = document.getElementById('app');
    host.innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>👥 Add Staff or Doctor</b>' +
      '<button class="ghost" onclick="staffProfiles()">Back</button></div><div class="page">' +
      '<div class="card">' +
      '<label class="tiny mut">Type</label>' +
      '<select id="apRole" class="input"><option value="staff">Staff</option><option value="doctor">Doctor</option></select>' +
      '<label class="tiny mut">Full Name</label><input id="apName" class="input" placeholder="Full name">' +
      '<label class="tiny mut">Mobile (10 digits)</label><input id="apMobile" class="input" inputmode="numeric" placeholder="10-digit mobile">' +
      '<label class="tiny mut">Staff Code</label><input id="apCode" class="input" placeholder="e.g. KNE-KISHAN9">' +
      '<label class="tiny mut">Branch</label><select id="apBranch" class="input">' +
      brs.map(function (b) { return '<option value="' + m.esc(b) + '">' + m.esc(b) + '</option>'; }).join('') + '</select>' +
      '<div style="margin-top:14px"><button id="apSave" onclick="pfSavePerson()" style="width:100%;background:#1457B8;color:#fff;border:0;font-weight:800;padding:12px;border-radius:10px">Save</button></div>' +
      '<div id="apMsg" class="tiny mut" style="margin-top:10px"></div>' +
      '</div></div></div>';
  }

  async function pfSavePerson() {
    var m = window.MOD;
    if (!m.isMasterModule()) return pfToast('Only Master');
    function v(id) { var e = document.getElementById(id); return e ? String(e.value || '') : ''; }
    var name = v('apName').trim();
    var mobile = v('apMobile').replace(/\D/g, '').slice(-10);
    var code = v('apCode').trim().toUpperCase();
    var branch = v('apBranch').trim();
    var role = v('apRole').trim().toLowerCase();
    var msg = document.getElementById('apMsg');
    // ⛔ এটুকু শুধু বাঁচাতে — আসল পাহারা সার্ভারেই।
    if (!name || !code || !branch || mobile.length !== 10) {
      if (msg) msg.textContent = 'Please fill name, mobile and code (mobile must be 10 digits).';
      return;
    }
    var btn = document.getElementById('apSave');
    if (btn) { btn.disabled = true; btn.textContent = 'Saving...'; }
    if (msg) msg.textContent = 'Saving...';
    var out = null;
    try {
      var client = await sb();
      var r = await client.schema('hr').rpc('admin_create_person', {
        p_code: code, p_mobile: mobile, p_name: name, p_branch: branch, p_role: role
      });
      out = (r && !r.error) ? r.data : null;
      if (r && r.error && msg) msg.textContent = 'Could not reach the server. Please try again.';
    } catch (e) { out = null; }
    if (btn) { btn.disabled = false; btn.textContent = 'Save'; }
    if (!out) { if (msg && !msg.textContent) msg.textContent = 'Could not reach the server. Please try again.'; return; }
    if (out.ok) {
      pfToast(String(out.message || 'Added'));
      staffProfiles();
    } else if (msg) {
      msg.textContent = String(out.message || 'Could not do it');
    }
  }
  window["pfAddPerson"] = pfAddPerson;
  window["pfSavePerson"] = pfSavePerson;

  // 🔴 V404 (16.08.2026, TK-নির্দেশ: "কর্মী বাদ দিন বোতাম বসান")
  //    বাদ দিলে যা যা হয় — একটাই বোতামে:
  //      ১) hr.staff_profiles.active = false ⇒ লগইন বন্ধ (V404-এর
  //         `suspended_until_for` 2999-12-31 ফেরায়, ওয়েব ও ফোন দুটোতেই)
  //      ২) মাইনে বন্ধ ⇒ আর কখনো "Salary Due"-তে নাম উঠবে না
  //      ৩) আয়-খরচের চাবি বন্ধ ⇒ V403-এর নিয়মও আপনা থেকে খাটে
  //    ⛔ রোগী · ফলোআপ · মাইনের রসিদ · ছুটি — একটাও সারি ছোঁয়া হয় না।
  async function profRemove(code) {
    if (!window.MOD.isMasterModule()) return pfToast('Only Master');
    if (!confirm(code + ' — remove this staff?\n\n• Login stops\n• Removed from the salary list\n• Income/Expense key turned off\n\nNo past record is deleted. Can be restored later.')) return;
    var client = await sb();
    var failed = [];
    try {
      var r1 = await client.schema('hr').from('staff_profiles')
        .update({ active: false, updated_at: new Date().toISOString() }).eq('person_code', code).select('person_code');
      if ((r1 && r1.error) || !r1 || !(r1.data||[]).length) failed.push('profile');
    } catch (e) { failed.push('profile'); }
    try {
      var r2 = await client.schema('hr').from('salary_config')
        .update({ salary_enabled: false }).eq('person_code', code);
      if (r2 && r2.error) failed.push('salary');
    } catch (e) { failed.push('salary'); }
    try {
      var r3 = await client.schema('fin').from('entry_permits')
        .update({ can_entry: false }).eq('person_code', code);
      if (r3 && r3.error) failed.push('permit');
    } catch (e) { failed.push('permit'); }
    // ⛔ সৎ বার্তা — অর্ধেক হলে "হয়ে গেছে" বলা হয় না (খাতার ফাঁদ ১ দ্রষ্টব্য)।
    if (failed.length) pfToast('Partly done. Left: ' + failed.join(', ') + '. Please try again.');
    else pfToast(code + ' removed');
    renderMaster();
  }

  // 🔴 V404: ভুল করে বাদ দিলে ফিরিয়ে আনা। ⛔ মাইনে নিজে থেকে চালু হয় না —
  //    মাস্টারকে Salary পর্দায় গিয়ে নিজে চালু করতে হবে (টাকার ব্যাপার, আন্দাজে নয়)।
  async function profRestore(code) {
    if (!window.MOD.isMasterModule()) return pfToast('Only Master');
    if (!confirm(code + ' — restore this staff?\n\nLogin will work again.\nSalary must be turned on separately from the Salary screen.')) return;
    var client = await sb();
    try {
      var r = await client.schema('hr').from('staff_profiles')
        .update({ active: true, updated_at: new Date().toISOString() }).eq('person_code', code);
      if (r && r.error) return pfToast('Did not work. Please try again.');
    } catch (e) { return pfToast('Did not work. Please try again.'); }
    pfToast(code + ' restored');
    renderMaster();
  }

  // 🔵🔒 B618 (11.08.2026, TK-নির্দেশ): master স্টাফকে X দিন Suspend — hr.staff_profiles-এ
  // suspended_until বসে; ওই সময় স্টাফ লগইন করতে পারবে না (login-গেট)। 0/ফাঁকা = Remove।
  async function profSuspend(code) {
    var opt = prompt('Suspend how many days? (0 or blank = remove suspend)', '3');
    if (opt === null) return;
    var days = parseInt(opt, 10);
    var until = null;
    var reason = '';
    if (days > 0) {
      var t = new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
      t.setDate(t.getDate() + days);
      until = t.getFullYear() + '-' + String(t.getMonth() + 1).padStart(2, '0') + '-' + String(t.getDate()).padStart(2, '0');
      // 🔴🔒 V1649 (২১.০৯.২০২৬, TK-নির্দেশ) — "সাসপেন্ড কি কারনে করা হয়েছে
      // সেটার জন্য সে বুঝতে পারে" · ফাঁকা রাখলেও চলবে (Android doSuspend-এর
      // হুবহু একই নিয়ম)।
      reason = String(prompt('Reason (optional) — staff will see this', '') || '').trim();
    }
    // 🔴🔒 V1645 (TK-রিপোর্ট ২০.০৯.২০২৬ — "সাসপেন্ড করলেও বাস্তবে কাজ করে
    // না") — চার V308 Doctor-এর (J.H MANDAL/GOKUL/SAIKAT ROY/PRANAB BISWAS)
    // কোনো staff_profiles সারিই DB-তে নেই (ফোনের doSuspend-এ একই ফাঁক ধরা
    // পড়েছে, কমেন্ট দ্রষ্টব্য) — আগে .select() ও length-check ছাড়া PATCH
    // ০ সারিতেও নীরবে "Suspended till …" দেখাত, আসলে কিছুই সেভ হতো না।
    var missingDoctorSeed = {
      'DR-JH-MANDAL': { link_mobile: '7479173399', full_name: 'J.H MANDAL', role_kind: 'doctor', branch: 'Cooch Behar', active: true },
      'DR-GOKUL': { link_mobile: '9002610352', full_name: 'GOKUL', role_kind: 'doctor', branch: 'Cooch Behar', active: true },
      'DR-SAIKAT-ROY': { link_mobile: '7810907954', full_name: 'Dr. SAIKAT ROY', role_kind: 'doctor', branch: 'Falakata', active: true },
      'DR-PRANAB-BISWAS': { link_mobile: '9242009205', full_name: 'Dr. PRANAB BISWAS', role_kind: 'doctor', branch: 'Birpara', active: true }
    };
    try {
      var client = await sb();
      var r = await client.schema('hr').from('staff_profiles')
        .update({ suspended_until: until, suspend_reason: reason || null }).eq('person_code', code).select('person_code');
      var ok = !!(r && !r.error && (r.data || []).length > 0);
      if (!ok && missingDoctorSeed[code]) {
        var seed = Object.assign({ person_code: code, suspended_until: until, suspend_reason: reason || null }, missingDoctorSeed[code]);
        var r2 = await client.schema('hr').from('staff_profiles').upsert(seed, { onConflict: 'person_code' });
        ok = !(r2 && r2.error);
      }
      try { toast(ok ? (until ? ('Suspended till ' + until) : 'Suspend removed') : 'Failed — check net'); } catch (e) {}
    } catch (e) { try { toast('Failed — check net'); } catch (_e) {} }
  }
  window.profSuspend = profSuspend;
  window.profRemove = profRemove;     // 🔴 V404
  window.profRestore = profRestore;   // 🔴 V404

  async function profEdit(code) {
    var m = window.MOD, client = await sb();
    var p = ((await client.schema('hr').from('staff_profiles').select('*').eq('person_code', code).maybeSingle()).data) || { person_code: code };
    var __oldProfilePhoto = null;
    try { __oldProfilePhoto = (userPhotoMap() || {})[mob(p.link_mobile || '')] || null; } catch (e) {}
    __currentPhoto = p.photo_data || __oldProfilePhoto || null; __pendingPhoto = null;
    // 🎨 (03.08.2026, TK-অনুমোদিত ফটো-প্রুফ পাশ করার পরে) — বড় হাসপাতাল/
    // নার্সিংহোমের HR সিস্টেমের ধাঁচে: উপরে প্রোফাইল-স্ট্রিপ + নিচে বিষয়-
    // ভিত্তিক প্যানেলে (Personal/Employment/Contact/Emergency/Identification/
    // Notes) ফিল্ড আলাদা করে সাজানো, ২-কলাম গ্রিডে, স্পষ্ট ফাঁক সহ।
    // ⛔ সম্পূর্ণ ইনলাইন স্টাইল — কোনো নতুন global CSS ক্লাস তৈরি হয়নি,
    // styles.css একটুও ছোঁয়া হয়নি, তাই বাকি কোনো স্ক্রিনে প্রভাব নেই।
    // ⛔ প্রতিটা ফিল্ডের id (pName, pBranch, ... pNotes, pPhotoImg,
    // pPhotoChange, pPhotoFile) আর profSave()/triple-tap লজিক অক্ষত —
    // শুধু চারপাশের HTML/CSS বদলেছে, ডেটা সেভ হওয়ার পথ এক অক্ষরও বদলায়নি।
    // ⛔ মকআপে দেখানো 🔒 আইকন এখানে ইচ্ছাকৃতভাবে বাদ — এই পাতায় Master
    // সত্যিই ফিল্ড এডিট করেন (input বাক্স), তাই লক-আইকন থাকলে বিভ্রান্তিকর
    // হতো (ফোনের সেই আইকনটা staff-এর নিজের-প্রোফাইল-triple-tap-lock এর
    // জন্য প্রাসঙ্গিক, ওয়েবে Master-only এই স্ক্রিনে সেই আচরণ নেই)।
    function f(id, label, val, type) {
      return '<div style="padding:11px 20px;border-bottom:1px solid #F5F6F8">' +
        '<label style="display:block;font-size:11px;color:#667085;margin-bottom:5px;font-weight:600;text-transform:uppercase;letter-spacing:.3px">' + label + '</label>' +
        '<input id="' + id + '" class="input" type="' + (type || 'text') + '" value="' + m.esc(val || '') + '" style="margin:0;padding:8px 10px;font-size:13.5px;border-radius:8px">' +
        '</div>';
    }
    function panel(title, fieldsHtml) {
      return '<div style="background:#fff;border:1px solid #E4E8EE;border-radius:12px;box-shadow:0 1px 3px rgba(16,24,40,0.04);overflow:hidden">' +
        '<div style="padding:13px 20px;border-bottom:1px solid #EEF1F4;font-size:13px;font-weight:700;color:#101828;display:flex;align-items:center;gap:8px">' +
        '<span style="width:7px;height:7px;border-radius:50%;background:#0B6B3A;display:inline-block"></span>' + title + '</div>' +
        fieldsHtml + '</div>';
    }
    var photoHtml = '<img id="pPhotoImg" src="' + m.esc(__currentPhoto || '') + '" style="width:64px;height:64px;object-fit:cover;border-radius:50%;border:2px solid #CFE9D8;background:#E6F4EA"' + (__currentPhoto ? '' : ' hidden') + '>' +
      (__currentPhoto ? '' : '<div id="pPhotoBlank" style="width:64px;height:64px;border-radius:50%;background:#E6F4EA;color:#0B6B3A;display:flex;align-items:center;justify-content:center;font-size:26px;border:2px solid #CFE9D8">👤</div>');
    document.getElementById('app').innerHTML =
      '<div class="wrap anMod anModPf" style="max-width:1180px">' +
      '<div class="topbar" style="padding:14px 24px"><b>🧑\u200d💼 Staff Profiles &nbsp;›&nbsp; <span style="color:#0B6B3A">View — ' + m.esc(code) + '</span></b>' +
      '<span><button class="ghost" onclick="staffProfiles()" style="margin-right:8px">Back</button>' +
      '<button onclick="profSave(\'' + m.esc(code) + '\')" style="background:#0B6B3A;box-shadow:none">Save</button></span></div>' +
      '<div class="page" style="padding:20px 24px 40px">' +

      '<div style="background:#fff;border:1px solid #E4E8EE;border-radius:12px;padding:20px 24px;display:flex;align-items:center;gap:18px;box-shadow:0 1px 3px rgba(16,24,40,0.04);flex-wrap:wrap">' +
      photoHtml +
      '<div style="flex:1;min-width:180px">' +
      '<div style="font-size:19px;font-weight:700;color:#101828">' + m.esc(p.full_name || '(name not set)') + '</div>' +
      '<div style="font-size:13px;color:#667085;margin-top:2px">' + m.esc(p.person_code) + ' · ' + m.esc(p.designation || p.role_kind || '-') + '</div>' +
      '<div style="margin-top:9px;display:flex;gap:8px;flex-wrap:wrap">' +
      '<span style="font-size:12px;font-weight:600;padding:4px 11px;border-radius:999px;background:#E6F4EA;color:#0B6B3A">' + m.esc(p.designation || p.role_kind || 'Staff') + '</span>' +
      '<span style="font-size:12px;font-weight:600;padding:4px 11px;border-radius:999px;background:#EAF1FB;color:#1D4E89">' + m.esc(p.branch || '-') + '</span>' +
      '</div></div>' +
      '<div style="text-align:right;font-size:12.5px;color:#98A2B3">Profile photo' +
      '<div id="pPhotoChange" style="cursor:pointer;color:#0B6B3A;font-weight:600;margin-top:2px">📷 Add / Change Photo</div>' +
      '<input type="file" id="pPhotoFile" accept="image/*" style="display:none"></div>' +
      '</div>' +

      '<div style="font-size:12.5px;font-weight:700;color:#667085;text-transform:uppercase;letter-spacing:.5px;margin:22px 2px 10px">Profile Details</div>' +
      '<div class="wlv1SafeTwoCol">' +

      panel('Personal Details', f('pName', 'Full Name', p.full_name) + f('pDob', 'Date of Birth', p.dob, 'date') + f('pGender', 'Gender', p.gender) + f('pBlood', 'Blood Group', p.blood_group) + f('pQual', 'Qualification', p.qualification)) +
      panel('Employment', f('pDesig', 'Designation (e.g. Staff/Receptionist)', p.designation) + f('pBranch', 'Branch', p.branch) + f('pJoin', 'Join Date', p.join_date, 'date')) +
      panel('Contact', f('pAddr', 'Address', p.address) + f('pAltMobile', 'Alternate Mobile', p.alt_mobile)) +
      panel('Emergency Contact', f('pEmg', 'Emergency Contact (Name + Mobile)', p.emergency_contact) + f('pEmgRel', 'Emergency Relationship', p.emergency_relationship)) +
      panel('Identification', '<div style="padding:11px 20px;border-bottom:1px solid #F5F6F8"><label style="display:block;font-size:11px;color:#667085;margin-bottom:5px;font-weight:600;text-transform:uppercase;letter-spacing:.3px">ID Type</label><input id="pIdType" class="input" value="' + m.esc(p.gov_id_type || '') + '" placeholder="Aadhaar / PAN / Voter" style="margin:0;padding:8px 10px;font-size:13.5px;border-radius:8px"></div>' +
        '<div style="padding:11px 20px"><label style="display:block;font-size:11px;color:#667085;margin-bottom:5px;font-weight:600;text-transform:uppercase;letter-spacing:.3px">ID Number (stored masked; last 4 kept)</label><input id="pIdNum" class="input" placeholder="enter full number" style="margin:0;padding:8px 10px;font-size:13.5px;border-radius:8px"></div>') +
      panel('Notes', f('pNotes', 'Notes', p.notes)) +

      '</div></div></div>';
    document.getElementById('pPhotoChange').onclick = function () { document.getElementById('pPhotoFile').click(); };
    document.getElementById('pPhotoFile').onchange = async function (e) {
      var url = await fileData(e.target);
      if (url) {
        __pendingPhoto = url;
        var img = document.getElementById('pPhotoImg');
        img.src = url; img.hidden = false;
        var blank = document.getElementById('pPhotoBlank'); if (blank) blank.hidden = true;
      }
    };
  }

  async function profSave(code) {
    var m = window.MOD, client = await sb();
    var idNum = document.getElementById('pIdNum').value.trim();
    var row = {
      person_code: code,
      full_name: document.getElementById('pName').value,
      branch: document.getElementById('pBranch').value,
      designation: document.getElementById('pDesig').value,
      join_date: document.getElementById('pJoin').value,
      dob: document.getElementById('pDob').value,
      gender: document.getElementById('pGender').value,
      blood_group: document.getElementById('pBlood').value,
      qualification: document.getElementById('pQual').value,
      address: document.getElementById('pAddr').value,
      alt_mobile: document.getElementById('pAltMobile').value,
      emergency_contact: document.getElementById('pEmg').value,
      emergency_relationship: document.getElementById('pEmgRel').value,
      gov_id_type: document.getElementById('pIdType').value,
      notes: document.getElementById('pNotes').value,
      updated_at: new Date().toISOString()
    };
    var photoToSave = __pendingPhoto || __currentPhoto;
    if (photoToSave) row.photo_data = photoToSave;
    if (idNum) { row.gov_id_last4 = idNum.replace(/\s/g, '').slice(-4); }
    // 🔵 TK-ORDER (07.08.2026): supabase upsert error-এ throw করে না ({error} ফেরায়)।
    // আগে সেটা যাচাই না করে সবসময় staffProfiles()-এ ফিরে যেত — মনে হত সেভ হয়েছে,
    // অথচ হয়নি → staff পরিবর্তন দেখত না। এখন {error}/throw হলে সৎ বার্তা + পাতায়
    // থাকা (আবার Save করা যায়); সফল হলে আগের মতোই তালিকায় ফেরা।
    // ⛔ upsert/onConflict/সফল-পথ এক অক্ষরও বদলায়নি — শুধু ব্যর্থতা এখন সৎ।
    try {
      // upsert by person_code (unique). Fetch id first to keep same row.
      var ex = (await client.schema('hr').from('staff_profiles').select('id').eq('person_code', code).maybeSingle()).data;
      if (ex) row.id = ex.id;
      var __up = await client.schema('hr').from('staff_profiles').upsert(row, { onConflict: 'person_code' });
      if (__up && __up.error) throw __up.error;
      try { toast('Saved'); } catch (e) {}
      staffProfiles();
    } catch (e) {
      try { toast('Not saved. Press Save again.'); } catch (e2) {}
    }
  }

  /* 🔵🔒 V418 (TK-অনুমোদিত নিয়ম, ১৭.০৮.২০২৬) — Extra Income আপনা থেকে।
     Master স্যালারি পর্দা খুললেই একবার হিসাবটা মিলিয়ে নেওয়া হয়:
       • Unexpected Time-এ রেজিস্ট্রেশন + Fee জমা ⇒ ₹১০০ (৫০-৫০)
       • ওই রোগীর প্রথম Advance জমা ⇒ আরও ₹৪০০ ⇒ মোট ₹৫০০
     ⛔ পুরো হিসাবটা ডেটাবেসের ভিতরে (`hr.incentive_sync`) — অ্যাপ শুধু ডাক দেয়,
        তাই ফোন আর ওয়েবে নিয়ম আলাদা হয়ে যাওয়ার সুযোগ নেই।
     ⛔ রোগী/এনকোয়ারি/পেমেন্ট — একটাও টেবিলে লেখা হয় না, শুধু পড়া হয়।
     ⛔ একবার "দেওয়া হয়েছে" হয়ে গেলে সেই সারি আর কখনো বদলায় না।
     ⛔ ডাকটা ব্যর্থ হলেও পর্দা আগের মতোই খোলে — কিছুই ভাঙে না। */
  async function profIncentiveSync(client) {
    try {
      if (!window.MOD.isMasterModule()) return;
      await client.schema('hr').rpc('incentive_sync', {});
    } catch (e) {}
  }

  /* ⏱️🔒 V1166 (০৭.০৯.২০২৬) — **ঘণ্টা হিসাবে বেতন** — ফোনের
     `native/HourSalary.kt`-এর হুবহু যমজ (TK-নির্দেশ ৮: দুটো জায়গাতেই)।
     নিয়ম (খাতার সারি ২৭০): দিন ১০টা–৫টা = ৭ ঘণ্টা · মাসের ঘণ্টা =
     ওই মাসের আসল দিন × ৭ · দর = সেট করা বেতন ÷ মাসের ঘণ্টা · মঞ্জুর
     ছুটি = ৭ ঘণ্টা · IN বা OUT একটাও না থাকলে ওই দিন ০ ঘণ্টা।
     ⛔ TK-এর স্পষ্ট নির্দেশ: *"প্রথমে শুধু দেখানো, টাকা কাটা নয়"* ⇒ এই
        লাইনটা **এক পয়সাও বদলায় না** — Due · Paid · Total সব আগের নিয়মেই।
     ⛔ একটাই ছোট পড়া (ওই স্টাফের ওই মাসের হাজিরা, তিনটে ঘর), পর্দা আঁকা
        এর জন্য থামে না — ব্যর্থ হলে লাইনটা শুধু বসে না। */
  var SAL_DAY_HOURS = 7, SAL_DAY_MINUTES = 7 * 60;
  /* যে মাস থেকে নিয়মটা চালু — TK: *"বিগত দিনের হিসাব ধরবেন না"*। */
  /* 📅🔒 V1178 (০৭.০৯.২০২৬, TK-নির্দেশ) — TK: *"হ্যাঁ, সেপ্টেম্বর থেকেই চালু হবে"*।
     আগে "আসছে মাস" হিসাব হত; TK নিজে সেপ্টেম্বরের সংখ্যা মিলিয়ে দেখে সিদ্ধান্ত
     বদলেছেন। এখন বাঁধা `2026-09` (ফোনের `HourSalary.STARTS_FROM`-এর হুবহু একই)। */
  var SAL_HOUR_STARTS_FROM = '2026-09';
  function salHourStartsFrom(){ return SAL_HOUR_STARTS_FROM; }
  function salHourDaysInMonth(ym){
    try{ var p = String(ym).split('-'); var y = parseInt(p[0],10), mo = parseInt(p[1],10);
      if(!y || !mo) return 0; return new Date(Date.UTC(y, mo, 0)).getUTCDate(); }catch(e){ return 0 }
  }
  /* `"09:15"` ও `"09:15:00"` — দুটো ধাঁচই জমা থাকে (Fix Attendance সেকেন্ড লেখে)। */
  function salHourMinutes(raw){
    var t = String(raw==null?'':raw).trim(); if(!t) return null;
    var p = t.split(':'); if(p.length !== 2 && p.length !== 3) return null;
    var h = parseInt(p[0],10), mi = parseInt(p[1],10);
    if(isNaN(h) || isNaN(mi) || h<0 || h>23 || mi<0 || mi>59) return null;
    return h*60 + mi;
  }
  function salHourText(mins){
    var h = Math.floor(mins/60), mm = mins%60; return h + 'h ' + (mm<10?'0'+mm:''+mm) + 'm';
  }
  function salHourCompute(days, amount, ym){
    var mh = salHourDaysInMonth(ym) * SAL_DAY_HOURS;
    var rate = mh > 0 ? (amount / mh) : 0;
    var worked = 0, leaves = 0, missing = 0;
    (days||[]).forEach(function(d){
      /* মঞ্জুর হওয়া ছুটি ⇒ পুরো দিনের ৭ ঘণ্টা (`is_leave` মঞ্জুর হলেই বসে)। */
      if(d && d.is_leave){ leaves++; worked += SAL_DAY_MINUTES; return; }
      /* 🏠🔒 V1180 (০৭.০৯.২০২৬, TK-নির্দেশ): মাস্টারের অনুমোদিত Work From Home
         দিন = সকাল ১০টা–বিকেল ৫টা, অর্থাৎ **সবসময় ৭ ঘণ্টা** (কম-বেশি নয়)।
         ⛔ ফোনের `HourSalary.compute`-এর হুবহু একই নিয়ম, তাই দুই পর্দা মেলে। */
      if(d && d.is_wfh){ worked += SAL_DAY_MINUTES; return; }
      var a = salHourMinutes(d && d.check_in), b = salHourMinutes(d && d.check_out);
      /* 🐞🔒 V1455 (১৪.০৯.২০২৬) — এই ফাংশনের মন্তব্যে (উপরে) দাবি ছিল ফোনের
         `HourSalary.compute()`-এর "হুবহু একই নিয়ম", কিন্তু V1200-এর "IN আছে
         অথচ OUT নেই ⇒ ৭ ঘণ্টা" শাখাটাই এখানে বসানো হয়নি — শুধু "IN বা OUT
         যেকোনো একটা না থাকলে ০" ধরত, তাই সত্যিই OUT ভুলে যাওয়া দিনে ফোন ৭ ঘণ্টা
         দেখাত, কম্পিউটার ০। এখন ফোনের নিয়মই বসানো হলো — আজকের এখনো-চলতে-থাকা
         দিনে (TK-রিপোর্ট, তালিকা ৫৭৬) ফ্ল্যাট ৭ ঘণ্টা নয়, এখন পর্যন্ত IN থেকে
         সত্যিকারের সময়; আগের কোনো দিনে সত্যিই OUT ভুলে গেলে ৭ ঘণ্টাই। */
      if (a !== null && b === null) {
        var wdSal = String((d && d.work_date) || '').slice(0, 10);
        var todayIsoSal = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Kolkata' });
        if (wdSal === todayIsoSal) {
          var nowMinSal = (function(){ var n = new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Kolkata'})); return n.getHours()*60+n.getMinutes(); })();
          if (nowMinSal > a) worked += (nowMinSal - a);
          return;
        }
        missing++; worked += SAL_DAY_MINUTES; return;
      }
      if(a === null || b === null || b <= a){ missing++; return; }
      worked += (b - a);
    });
    return { monthHours: mh, ratePerHour: rate, workedMinutes: worked,
             leaveDays: leaves, missingDays: missing, payable: (worked/60) * rate };
  }
  async function salHourFill(code, amount, ym, paidThisMonth){
    var el = document.getElementById('salHourVal'), lab = document.getElementById('salHourLab');
    if(!el) return;
    try{
      var client = await sb();
      var p = String(ym).split('-'), y = parseInt(p[0],10), mo = parseInt(p[1],10);
      var from = ym + '-01';
      var end = (mo >= 12) ? ((y+1) + '-01-01') : (y + '-' + ((mo+1)<10?'0'+(mo+1):''+(mo+1)) + '-01');
      var rows = ((await client.schema('wn').from('notebook_days')
        .select('work_date,check_in,check_out,is_leave,is_wfh')
        .eq('staff_code', code).gte('work_date', from).lt('work_date', end)).data) || [];
      var r = salHourCompute(rows, amount, ym);
      /* ⛔ নিজে মেপে ধরা: নিয়মটা চালু **আগামী মাস থেকে**, অথচ দেখানো হচ্ছে
         **এই মাসের** হিসাব (TK যেন আগে থেকে মিলিয়ে দেখতে পারেন) — তাই চালু
         হওয়ার আগে লেবেলেই স্পষ্ট লেখা থাকে যে এটা শুধু আগাম দেখা। */
      var started = String(ym) >= salHourStartsFrom();
      if(lab) lab.textContent = started
        ? ('By hours \u00b7 ' + salHourMonthName(ym))
        : ('By hours \u00b7 ' + salHourMonthName(ym) +
           ' (preview \u2014 counts from ' + salHourMonthName(salHourStartsFrom()) + ')');
      el.textContent = window.MOD.money(r.payable) + '  \u00b7  ' + salHourText(r.workedMinutes) +
        ' of ' + Math.round(r.monthHours) + 'h';
      el.style.color = '#0E6E8C';
      /* 💰🔒 V1178 (TK-অনুমোদিত ধাপ ২) — নিয়ম চালু হওয়া মাস থেকে "এই মাসে বাকি"-ও
         ঘণ্টা হিসাবেই। ⛔ হাজিরা আনা না গেলে এই ঘরটা ছোঁয়াই হয় না (উপরের catch),
         তখন আগের নিয়মেই সেট করা বেতন ধরে বাকি দেখায়। */
      if (started) {
        var dueNow = Math.max(0, r.payable - Number(paidThisMonth || 0));
        var dEl = document.getElementById('salDueVal');
        if (dEl) {
          dEl.textContent = dueNow <= 0 ? 'Paid' : ('Due ' + window.MOD.money(dueNow));
          dEl.style.color = dueNow <= 0 ? '#0A7C3F' : '#B42318';
        }
      }
    }catch(e){
      var row = document.getElementById('salHourRow'); if(row) row.style.display = 'none';
    }
  }
  function salHourMonthName(ym){
    try{ var q = String(ym||'').split('-');
      var n = ['January','February','March','April','May','June','July','August','September','October','November','December'];
      return n[parseInt(q[1],10)-1] + ' ' + q[0]; }catch(e){ return ym||'' }
  }

  async function profSalary(code) {
    var m = window.MOD, client = await sb();
    await profIncentiveSync(client);
    var sc = ((await client.schema('hr').from('salary_config').select('*').eq('person_code', code).maybeSingle()).data) || { person_code: code };
    /* 🔵 V818 — একজনেরই তালিকা, তবু সীমা বসানো (ফোনের সঙ্গে এক নিয়ম)। */
    var pays = ((await client.schema('hr').from('salary_payments').select('*').eq('person_code', code).order('paid_on', { ascending: false }).limit(300)).data) || [];
    /* 🔒 V1641 — Salary/Extra history reuses these already-loaded rows. */
    window.__pfSalaryHistoryCache = { code: String(code||''), pays: pays };
    var prof = ((await client.schema('hr').from('staff_profiles').select('join_date').eq('person_code', code).maybeSingle()).data) || {};
    // 🔴🆕🔒 TK-নির্দেশ (08.08.2026, ফটো-প্রুফে লক) — ফোনের মতোই সহজ: উপরে মাসিক
    // বেতন + "কোন মাস পর্যন্ত দেওয়া / এই মাসে বাকি", নিচে এই-মাসের-বেতন দিন, পুরো
    // History, একদম নিচে বেতন-সেটিংস। মাসের নাম ইংরেজিতে। ⛔ সেভ/পেমেন্টের কল
    // আগেরটাই (profSalaryCfgSave/profSalaryPay), শুধু পেমেন্টে for_month যোগ।
    // হিসাব: পেমেন্টের মাস = for_month থাকলে সেটা, নইলে paid_on-এর মাস (পুরনো পেমেন্টও
    // ধরা পড়ে)। "Paid up to / due" শুধু পড়ার হিসাব — ভুল হলেও টাকা/রেকর্ড ভাঙে না।
    var names = ['January','February','March','April','May','June','July','August','September','October','November','December'];
    function monthLabel(ym){ try{ var pp=String(ym).split('-'); return names[Number(pp[1])-1]+' '+pp[0]; }catch(e){ return ym; } }
    function payMonth(p){ if(p.for_month) return String(p.for_month); var d=String(p.paid_on||''); return d.length>=7?d.slice(0,7):''; }
    var cur = m.todayIST().slice(0,7);
    var amount = Number(sc.salary_amount||0);
    var active = !!sc.salary_enabled && amount>0;
    var latest='', paidThis=0;
    var extraTotal=0, extraDue=0, salaryTotal=0;
    var salaryCount = pays.filter(function(p){ return !salIsExtra(p); }).length;
    var extraCount = pays.filter(function(p){ return salIsExtra(p); }).length;
    pays.forEach(function(p){
      if(salIsExtra(p)){
        /* 🔵 V417: "বাকি" টাকা এখনো দেওয়া হয়নি ⇒ "দেওয়া হয়েছে"-তে ধরা যাবে না। */
        if(salStatus(p)==='DUE') extraDue+=Number(p.amount||0); else extraTotal+=Number(p.amount||0);
        return;
      }
      salaryTotal+=Number(p.amount||0);
      var mm=payMonth(p); if(mm>latest)latest=mm; if(mm===cur)paidThis+=Number(p.amount||0); });
    var due = active ? Math.max(0, amount-paidThis) : 0;
    /* 🎨 V416 (TK-অনুমোদিত মডেল ২): জয়েনিং ডেট ছোট করে উপরে, তারপর তিনটে মোট-বাক্স।
       ⛔ শুধু সাজ — কোনো হিসাব বদলায়নি। ⛔ নেভি ব্লু কোথাও নেই। */
    /* 🔵 V417খ: পাশাপাশি বসা সমান-মাপের বোতাম (দুটোতেই flex:1 ⇒ সমান চওড়া)।
       ⛔ রং/মোটা লেখা/গোল কোণ আগের মডেল ৩-এর মতোই — TK-অনুমোদিত। */
    function salPairBtn(label, textHex, borderHex, call) {
      return '<button style="flex:1;min-width:0;background:#fff;color:' + textHex +
        ';border:2px solid ' + borderHex + ';font-weight:800;font-size:14px;line-height:1.25' +
        ';padding:13px 8px;border-radius:12px" onclick="' + call + '">' + label + '</button>';
    }
    /* 🎨 V417গ (TK-অনুমোদিত "মডেল ৩", ১৭.০৮.২০২৬) — *"৩ নম্বর বসান · সব ইংরেজিতে
       হবে বাংলা লেখা থাকবে না"*। পর্দাটা এখন দুই ভাগে:
         বাক্স ১ = Salary   (মাসিক · এই মাস · মোট দেওয়া · দুটো বোতাম)
         বাক্স ২ = Extra Income (দেওয়া · বাকি · দুটো বোতাম)
       ⛔ একটাও হিসাব বদলায়নি — উপরের গণনা হুবহু আগেরটাই।
       ⛔ পর্দায় কোনো বাংলা লেখা নেই, কোনো নির্দেশ/সাহায্য-লাইনও নেই। */
    function salRow(label, value, valueHex, top) {
      return '<div style="display:flex;justify-content:space-between;align-items:center;padding:9px 0' +
        (top ? ';border-top:1px solid #F0F4F1' : '') + '">' +
        '<span style="color:#3B5A49;font-size:13.5px">' + label + '</span>' +
        '<b style="color:' + (valueHex || '#123A26') + ';font-size:14.5px">' + value + '</b></div>';
    }
    var joinRow = salRow('Joining date', m.esc(prof.join_date ? salDmy(prof.join_date) : 'Not recorded'), '#5B6B81', true);

    /* বাক্স ১ — Salary */
    var salaryCard = '<div class="card">' +
      '<div style="font-weight:800;color:#0A5C33;font-size:16px;padding-bottom:4px">Salary</div>' +
      (active
        ? (salRow('Monthly', m.money(amount) + (sc.salary_date ? (' · day ' + m.esc(sc.salary_date)) : ''), '#0A5C33', true) +
           /* 💰 V1178 — এই সারিটাই পরে ঘণ্টার হিসাবে বদলে যায় (`salHourFill`)।
              ⛔ প্রথমে আগের নিয়মেই বসে, তাই হাজিরা না এলে কিছু খারাপ হয় না। */
           ('<div style="display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-top:1px solid #F0F4F1">' +
             '<span style="color:#3B5A49;font-size:13.5px">' + m.esc(monthLabel(cur)) + '</span>' +
             '<b id="salDueVal" style="color:' + (due <= 0 ? '#0A7C3F' : '#B42318') + ';font-size:14.5px">' +
             (due <= 0 ? 'Paid' : 'Due ' + m.money(due)) + '</b></div>') +
           salRow('Paid up to', (latest ? monthLabel(latest) : '—'), '#0A7C3F', true) +
           /* ⏱️🔒 V1166 — ঘণ্টা হিসাবে কত হত (শুধু দেখানো; টাকা বদলায় না)। */
           '<div id="salHourRow" style="display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-top:1px solid #F0F4F1">' +
             '<span id="salHourLab" style="color:#3B5A49;font-size:13.5px">By hours</span>' +
             '<b id="salHourVal" style="color:#5B6B81;font-size:14.5px">…</b></div>')
        : salRow('Monthly', 'Not set', '#B42318', true)) +
      /* 🎨 V1181 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — "Total paid" ও
         "Joining date" এখন উপরের ⋮-এর ভিতরে (TK: *"না থাকলেও চলবে এখানে"*)।
         ⛔ মুছে ফেলা হয়নি, শুধু সরানো — ফোনেও হুবহু একই। */
      /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬) — ফোনে "Add Salary" বোতামটা **শুধু
         বেতন চালু থাকলেই** আসে (StaffProfileActivity.kt:913-919); বন্ধ থাকলে
         "Payment History" পুরো সারিটা নেয়। ওয়েবে বেতন "Not set" হলেও বোতামটা
         পড়ে থাকত — চাপলে কোনো মাসই বাছা যেত না। */
      /* 🎨 V1181 — "Add Salary" একাই পুরো লাইন; History ⋮-এ গেছে। */
      (active ? ('<div style="display:flex;gap:9px;margin-top:11px">' +
        salPairBtn('Add Salary', '#0A5C33', '#0A5C33', 'profSalaryAddMonth(\'' + m.esc(code) + '\')') +
      '</div>') : '') +
      /* 🟣🔒 V1641 — Android parity: Salary History ও Extra Income History
         আলাদা screen; hidden combined history আর render করা হয় না। */
      '</div>';

    
    /* 🗑️🔒 V1051 (TK-নির্দেশ, ০৪.০৯.২০২৬: *"Add salary আর Pay September 2026
       salary — ২টা একই জিনিস, তাহলে এটা বাদ দিন"*) — "Add Salary" দিয়েই যেকোনো
       মাসের বেতন দেওয়া যায়, তাই এই ফর্মটা বাড়তি ছিল। ফোনেও একই সাথে তোলা হলো।
       ⛔ `profSalaryPay()` মোছা হয়নি — টাকার হিসাব ছোঁয়া হয়নি। */
    var payHtml = '';

    /* বাক্স ২ — Extra Income। ⛔ "Pay" বোতাম কেবল বাকি থাকলেই আসে; না থাকলে
       "Add Extra" নিজেই পুরো লাইন নেয় (ফাঁকা বাক্স বসে না)। */
    /* 💰🔒 V991 (০৩.০৯.২০২৬, TK-নির্দেশ: *"ডিজাইনটা আরো প্রফেশনাল লুক বানাতে
       হবে"*, ফটো-প্রুফ পাশ) — সোনালি পট্টি ও দুটো রঙিন টালি (ফোনের যমজ)।
       ⛔ শুধু সাজ — টাকার অঙ্ক ও হিসাব এক অক্ষরও বদলায়নি। */
    function unxMonthName(ym){
      try{ var q=String(ym||'').split('-');
        var n=['January','February','March','April','May','June','July','August','September','October','November','December'];
        return n[parseInt(q[1],10)-1]+' '+q[0]; }catch(e){ return ym||'' }
    }
    function salTile(cap,val,fill,ink){
      return '<div style="flex:1;background:'+fill+';border-radius:12px;padding:11px 14px">'+
        '<div style="font-size:10px;font-weight:800;letter-spacing:1.2px;color:#6B7A83">'+cap+'</div>'+
        '<div style="font-size:19px;font-weight:800;color:'+ink+';margin-top:3px">'+val+'</div></div>';
    }
    var extraCard = '<div class="card">' +
      '<div style="background:linear-gradient(90deg,#B45309,#E0A800);color:#fff;border-radius:12px;padding:10px 14px;display:flex;margin-bottom:10px">' +
        '<b style="font-size:14px;letter-spacing:.6px;flex:1">EXTRA INCOME</b>' +
        '<span style="font-size:12px;color:#FFF3D6">' + m.esc(unxMonthName(cur)) + '</span></div>' +
      '<div style="display:flex;gap:8px;margin-bottom:6px">' +
        salTile('PAID', m.money(extraTotal), '#EAF7F0', '#0B5B2F') +
        salTile('DUE', m.money(extraDue), (extraDue>0?'#FDEDEC':'#F3F5F7'), (extraDue>0?'#B42318':'#5B6B81')) +
      '</div>' +
      '<div style="display:flex;gap:9px;margin-top:11px">' +
        salPairBtn('Add Extra', '#B45309', '#E0A800', 'profExtraIncome(\'' + m.esc(code) + '\')') +
        (extraDue>0 ? salPairBtn('Pay ' + m.money(extraDue), '#0A5C33', '#0A5C33', 'profPayExtraDue(\'' + m.esc(code) + '\')') : '') +
      '</div>' +
      /* 🎨 V1181 — Statement এখন উপরের ⋮-এর ভিতরে (ফোনের মতোই)। */
      /* ⏰🔒 V990 (০৩.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK: *"তারা যদি নাই
         জানতে পারে যে সেই পেশেন্টটা ট্রিটমেন্ট চালু করেছে কিনা, তাহলে তারা
         হিসাবটা পাবে কি করে"*। ফোনের হুবহু জোড়া বোতাম।
         ⛔ টাকার কোনো অঙ্ক এখান থেকে বদলায় না — শুধু দেখা। */
      /* 🎨🔒 V1181 (TK-নির্দেশ, হুবহু): *"Extra income History, My Unexpected
         Enquiry এগুলি পাশাপাশি থাকতে হবে ( তাছাড়া My Unexpected Enquiry এটা
         staff এর তাহলে মাস্টারের ডিসপ্লে তে এরকম নাম কেন থাকবে)"*।
         ⇒ দুটো এক লাইনে, আর মাস্টারের পর্দায় "My" থাকে না — তিনি তখন **অন্য
           একজন স্টাফের** এনকোয়ারি দেখছেন। ⛔ ফোনের হুবহু একই নিয়ম। */
      /* ⚠️ কম্পিউটারে **আলাদা "Extra Income History" বোতাম নেই** — এখানে
         বেতন ও এক্সট্রার হিস্ট্রি একটাই মিলিত তালিকা ("Payment History",
         এখন ⋮-এর ভিতরে)। তাই পাশে বসানোর মতো দ্বিতীয় বোতামই নেই; এই
         বোতামটা একাই পুরো লাইন নেয়। ⛔ TK-কে এটা সৎভাবে জানানো হয়েছে। */
      '<div style="display:flex;gap:9px;margin-top:9px">' +
        salPairBtn('Extra Income History (' + extraCount + ')', '#B45309', '#E0A800', 'profSalaryHistory(\'' + m.esc(code) + '\',\'EXTRA\')') +
        salPairBtn(((typeof isMaster==='function'&&isMaster()) ? 'Unexpected Enquiries' : 'My Unexpected Enquiries'), '#123E8C', '#123E8C', 'profUnexpected(\'' + m.esc(code) + '\')') +
      '</div></div>';

    /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬) — ফোনে এটা **আলাদা পর্দা**
       ("Edit Salary — CODE", StaffProfileActivity.kt:1501-1517): লেবেল
       Salary (disabled/enabled বাছাই) · Amount · Salary Date, নিচে
       Cancel ও Save। ওয়েবে একটা গুটোনো লাইনের ভিতরে চেকবক্স ছিল।
       ⛔ কী সেভ হয় (salary_enabled · salary_amount · salary_date) — একই। */
    /* 🎨 V1181 — এই কার্ডটা আর পর্দায় বসে না ("Salary Settings" এখন ⋮-এ)।
       ⛔ কোড মোছা হয়নি (প্রকল্প-নিয়ম) — ভবিষ্যতে দরকার হলে এক লাইনেই ফেরে। */
    var settingsCard = '<div class="card">' +
      '<div onclick="profSalaryEdit(\'' + m.esc(code) + '\')" style="display:flex;justify-content:space-between;align-items:center;cursor:pointer">' +
        '<b style="color:#0A5C33;font-size:15px">Salary Settings</b><span style="color:#9AA8B5">›</span></div></div>';

    /* 🏍️🔒 V968 (০২.০৯.২০২৬, TK-নির্দেশ) — **শুধু বাইরে ঘোরা স্টাফের** কার্ডে
       ফিল্ড ভিজিটের বোতাম (এখন RUPAM)। ফোনের StaffProfileActivity-র হুবহু জোড়া।
       ⛔ GPS গোনা শুধু ফোনেই হয় (ব্রাউজারে পর্দা বন্ধ হলেই থেমে যায়) — এখানে
          শুধু **দেখা** যায়, TK-কে সেটা কাজ শুরুর আগেই জানানো হয়েছে। */
    /* 🏍️ V978 (TK-নির্দেশ) — বোতামটা এখন স্টাফ-কার্ডের সারিতেই; বেতন-পর্দার
       ভিতরের কার্ডটা আর বসে না (একই জিনিস দুই জায়গায় থাকলে বিভ্রান্তি)। */
    var fieldCard = true ? '' :
      '<div class="card">' +
      '<div onclick="profFieldVisit(\'' + m.esc(code) + '\')" style="display:flex;justify-content:space-between;align-items:center;cursor:pointer">' +
        '<b style="color:#0369A1;font-size:15px">Field Visit Tracking</b><span style="color:#9AA8B5">›</span></div></div>';

    /* 🎨🔒 V1181 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — উপরের ⋮।
       ভিতরে: Payment History · Statement · Salary Settings · Total paid ·
       Joining date। ⛔ প্রতিটা সারি ঠিক আগের বোতামটাই ডাকে, কাজ বদলায়নি।
       ⛔ শেষ দুটো শুধু দেখার — চাপলে কিছু হয় না। */
    var salMenu = '<div id="salMenu" style="display:none;position:absolute;right:12px;top:44px;z-index:40;' +
      'min-width:240px;background:#fff;border:1px solid #E4E8EE;border-radius:12px;box-shadow:0 8px 26px rgba(16,24,40,.18);overflow:hidden">' +
      '<div class="salMi" onclick="profSalMenuHide();profSalaryHistory(\'' + m.esc(code) + '\',\'SALARY\')" style="padding:12px 16px;font-size:14px;color:#1C2B3A;border-bottom:1px solid #EEF1F5;cursor:pointer">Salary History (' + salaryCount + ')</div>' +
      '<div class="salMi" onclick="profSalMenuHide();profStatement(\'' + m.esc(code) + '\')" style="padding:12px 16px;font-size:14px;color:#1C2B3A;border-bottom:1px solid #EEF1F5;cursor:pointer">🧾 Statement (date to date)</div>' +
      '<div class="salMi" onclick="profSalMenuHide();profSalaryEdit(\'' + m.esc(code) + '\')" style="padding:12px 16px;font-size:14px;color:#1C2B3A;border-bottom:1px solid #EEF1F5;cursor:pointer">Salary Settings</div>' +
      /* 🗓️ V1199 (TK-নির্দেশ) — কত তারিখে কত ঘণ্টা, আলাদা পর্দায় (ফোনের যমজ)। */
      '<div class="salMi" onclick="profSalMenuHide();attendanceSheet(\'' + m.esc(code) + '\')" style="padding:12px 16px;font-size:14px;color:#1C2B3A;border-bottom:1px solid #EEF1F5;cursor:pointer">Performance Sheet</div>' +
      '<div style="padding:12px 16px;font-size:14px;color:#1C2B3A;border-bottom:1px solid #EEF1F5;display:flex;justify-content:space-between;gap:14px">Total paid<span style="color:#5B6B81">' + m.money(salaryTotal) + '</span></div>' +
      '<div style="padding:12px 16px;font-size:14px;color:#1C2B3A;display:flex;justify-content:space-between;gap:14px">Joining date<span style="color:#5B6B81">' + m.esc(prof.join_date ? salDmy(prof.join_date) : 'Not recorded') + '</span></div>' +
      '</div>';
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar" style="position:relative"><b>Salary — ' + m.esc(code) + '</b>' +
      '<span onclick="profSalMenuToggle()" style="font-size:20px;font-weight:800;color:#0B4F2A;padding:0 10px;cursor:pointer;user-select:none">⋮</span>' +
      '<button class="ghost" onclick="staffProfiles()">Back</button>' + salMenu + '</div><div class="page">' +
      salaryCard + payHtml + extraCard + fieldCard +
      '</div></div>';
    /* ⛔ পর্দা আগে আঁকা হয়, তারপর ঘণ্টার লাইনটা ভরে — ফোনের মতোই। */
    if(active) salHourFill(code, amount, cur, paidThis);   /* 💰 V1178 */
  }

  /* ⏰🔒 V990 (০৩.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) —
     **MY UNEXPECTED ENQUIRIES** (ফোনের `UnexpectedEnquiryActivity`-র যমজ)।
     TK: *"তারা যদি নাই জানতে পারে যে সেই পেশেন্টটা ট্রিটমেন্ট চালু করেছে কিনা,
     তাহলে তারা হিসাবটা পাবে কি করে"*।
     ⛔ টাকার নিয়ম নতুন করে বানানো হয়নি — ডেটাবেসের চালু নিয়মই দেখানো হয়
        (ভিজিট ₹১০০ · চিকিৎসা শুরু হলে আরও ₹৪০০ ⇒ ₹৫০০)।
     ⛔ উপরের লাল লাইনে **কল কখন এসেছিল** — TK-এর কথায় এটাই টাকার শর্ত।
     ⛔ একটাও সারি লেখা হয় না, শুধু পড়া। */
  function unxDigits(v){ return String(v||'').replace(/[^0-9]/g,'').slice(-10) }
  function unxDateTime(raw){
    var t=String(raw||'').trim(); if(t.length<10) return '';
    var p=t.slice(0,10).split('-'); if(p.length<3) return '';
    var d=p[2]+'/'+p[1]+'/'+p[0];   /* 🔴 V1158 */
    if(t.length<16) return d;
    var hh=parseInt(t.slice(11,13),10), mm=t.slice(14,16);
    if(isNaN(hh)) return d;
    var ap=hh>=12?'PM':'AM', h12=(hh===0)?12:(hh>12?hh-12:hh);
    return d+' : '+h12+'.'+mm+' '+ap;   /* 🔴 V1158 */
  }
  var UNX_NOT_TREATMENT = ['visit_fee','attendance_mark','bill_edit','chamber_expected','refund'];
  /* 💰 V1029 — রেজিস্ট্রেশনের ফি যে যে নামে জমা হয় (SQL-এর হুবহু তালিকা)। */
  var UNX_FEE_TYPES = ['visit_fee','visitfee','registration'];

  /* 💰🔒 V1184 — নতুন নিয়ম কবে থেকে ও কত (ফোনের `UnexpectedIncentive`-এর যমজ)। */
  var UNX_NEW_RULE_FROM='2026-09-08';
  var UNX_NEW_REG=100, UNX_NEW_TRT=350;
  /* রেজিস্ট্রেশন-কারী সত্যিই স্টাফ কিনা — প্রকল্পের নিজের `staffCodeMap()`
     (মোবাইল → স্টাফ-কোড) থেকেই। ⛔ নতুন কোনো পড়া নয়। ⛔ চেনা না গেলে `false`,
     অর্থাৎ তখন ভাগ হয় না — ডেটাবেসের নিয়মেও ঠিক তাই। */
  function unxKnownStaff(mob10){
    try{
      if(typeof staffCodeMap!=='function') return false;
      var map=staffCodeMap()||{};
      for(var k in map){ if(unxDigits(k)===mob10) return true; }
      return false;
    }catch(e){ return false; }
  }
  async function profUnexpected(code){
    var m = window.MOD;
    var mob='';
    try{
      (((window.C&&C.users)||{}).staff||[]).concat(((window.C&&C.users)||{}).doctor||[],((window.C&&C.users)||{}).master||[])
        .forEach(function(u){ if(String(u.name||'').trim().toLowerCase()===String(code||'').trim().toLowerCase()) mob=unxDigits(u.mobile) });
    }catch(e){}
    if(!mob){ try{ mob=unxDigits(user&&user.mobile) }catch(e){} }
    document.getElementById('app').innerHTML='<div class="wrap anMod anModPf"><div class="topbar"><b>'+m.esc(code)+' · UNEXPECTED</b>'+
      '<button class="ghost" onclick="profSalary(\''+m.esc(code)+'\')">Back</button></div><div class="page"><div class="card mut">Loading…</div></div></div>';

    var enq=[], pays=[];
    try{
      var ok=await initCloudClientOnly();
      if(ok&&sb){
        var r1=await sb.from('enquiries').select('id,name,mobile,branch,date,timeType,receivedBy,createdAt')
          .eq('receivedBy',mob).eq('timeType','Unexpected Time').limit(500);
        enq=(r1&&r1.data)||[];
        var mobs=[]; enq.forEach(function(e){ var x=unxDigits(e.mobile); if(x.length===10&&mobs.indexOf(x)<0) mobs.push(x) });
        if(mobs.length){
          var r2=await sb.from('payments').select('id,mobile,amount,payType,date,createdAt').in('mobile',mobs).limit(2000);
          pays=(r2&&r2.data)||[];
        }
      }
    }catch(e){}

    /* 💰🔒 V1184 (০৭.০৯.২০২৬, TK-নির্দেশ) — কে রেজিস্ট্রেশন করেছেন ও কোন
       তারিখে; নতুন নিয়মটা তারিখ ধরে চলে বলে এটা না জানলে প্রাপ্য অঙ্ক ঠিক
       বলা যায় না। ⛔ একটাই সরু পড়া; ব্যর্থ হলে পুরনো নিয়মেই দেখানো হয়। */
    var unxRegBy={}, unxRegOn={};
    try{
      if(mobs && mobs.length && typeof sb!=='undefined' && sb){
        var r3=await sb.from('patients').select('id,mobile,registeredBy,registrationDate,date').in('mobile',mobs).limit(2000);
        ((r3&&r3.data)||[]).forEach(function(q){
          var mm=unxDigits(q.mobile); if(mm.length!==10) return;
          unxRegBy[mm]=unxDigits(q.registeredBy||'');
          unxRegOn[mm]=String(q.registrationDate||q.date||'').slice(0,10);
        });
      }
    }catch(e){}

    var firstVisit={}, firstTreat={};
    pays.forEach(function(p){
      var mm=unxDigits(p.mobile); if(mm.length!==10) return;
      var t=String(p.payType||'').toLowerCase();
      var at=String(p.createdAt||p.date||''); if(!at) return;
      /* 🐞🔒 V1029 — টাকা যে নিয়মে দেওয়া হয় সেখানে রেজিস্ট্রেশনের ফি তিন
         নামে ধরা হয় (visit_fee · visitfee · registration); এখানে শুধু
         প্রথমটাই দেখা হত, তাই বেতনে বাকি দেখালেও এখানে ₹০ উঠত। */
      if(UNX_FEE_TYPES.indexOf(t)>=0){ if(!firstVisit[mm]||at<firstVisit[mm]) firstVisit[mm]=at }
      else if(UNX_NOT_TREATMENT.indexOf(t)<0 && Number(p.amount||0)>0){ if(!firstTreat[mm]||at<firstTreat[mm]) firstTreat[mm]=at }
    });

    var seen={}, rows=[], monthTotal=0;
    var ym=(new Date()).toISOString().slice(0,7);
    enq.sort(function(a,b){ return String(b.createdAt||'').localeCompare(String(a.createdAt||'')) });
    enq.forEach(function(e){
      var mm=unxDigits(e.mobile); if(mm.length!==10||seen[mm]) return; seen[mm]=1;
      var treat=firstTreat[mm], visit=firstVisit[mm];
      var stage=treat?'treatment':(visit?'visit':'none');
      /* 💰🔒 V1184 (TK-নির্দেশ ০৭.০৯.২০২৬) — ফোনের `UnexpectedIncentive`-এর
         হুবহু একই নিয়ম, তাই দুই পর্দা কখনো দুরকম বলবে না (নিয়ম ৬.৬ · ৭ক-২):
           • রেজিস্ট্রেশন ০৮.০৯.২০২৬ বা তার পরে ⇒ এনকোয়ারি-ফর্মকারী ₹১০০,
             ট্রিটমেন্ট শুরু হলে আরও ₹৩৫০ (রেজিস্ট্রেশন-কারী আলাদা ₹৫০ পান)।
           • তার আগের রেজিস্ট্রেশনে ⇒ পুরনো নিয়মে ₹১০০ ও ₹৪০০ **সমান ভাগ**।
         ⛔ তারিখ জানা না গেলে পুরনো নিয়মই ধরা হয় — আন্দাজে বেশি দেখানো নয়। */
      var rOn=String(unxRegOn[mm]||''), rBy=String(unxRegBy[mm]||'');
      var newEra=(rOn.length===10 && rOn>=UNX_NEW_RULE_FROM);
      var otherReg=(rBy.length===10 && rBy!==mob && !!unxKnownStaff(rBy));
      var earned;
      if(newEra){
        earned=(stage==='treatment')?(UNX_NEW_REG+UNX_NEW_TRT):(stage==='visit'?UNX_NEW_REG:0);
      }else{
        var share=otherReg?2:1;
        earned=(stage==='treatment')?(500/share):(stage==='visit'?(100/share):0);
      }
      var at=treat||visit||'';
      if(at.slice(0,7)===ym) monthTotal+=earned;
      rows.push({name:e.name||'(no name)',mobile:mm,branch:e.branch||'',callAt:e.createdAt||e.date||'',stage:stage,at:at,earned:earned});
    });

    var body='<div class="card" style="background:#0B4F2A;color:#fff;display:flex;justify-content:space-between;font-weight:800">'+
      '<span>THIS MONTH · EARNED</span><span>₹'+monthTotal.toLocaleString('en-IN')+'</span></div>';
    if(!rows.length) body+='<div class="card mut">No unexpected-time enquiry found yet.</div>';
    rows.forEach(function(r){
      var line,ink,fill;
      if(r.stage==='treatment'){ line='✓ Treatment started  ·  '+unxDateTime(r.at); ink='#0B5B2F'; fill='#EAF7F0'; }
      else if(r.stage==='visit'){ line='⌛ Visit given  ·  '+unxDateTime(r.at); ink='#8A5A00'; fill='#FFF6E6'; }
      else { line='— Not come to the branch yet'; ink='#5B6B81'; fill='#F3F5F7'; }
      /* 👆 V1029 — কার্ডে চাপ দিলে ওই রোগীর পুরো ইতিহাস খোলে (ফোনের হুবহু)। */
      body+='<div class="card" style="cursor:pointer" onclick="wlv1FullJourney(\''+m.esc(r.mobile)+'\')"><div style="display:flex;align-items:baseline;gap:10px">'+
        '<b style="font-size:15px">'+m.esc(r.name)+'</b>'+
        '<span style="color:#1667D8;flex:1">'+m.esc(r.mobile)+'</span>'+
        '<span class="mut">'+m.esc(r.branch)+'</span></div>'+
        '<div style="margin-top:6px;color:#8A1810;font-weight:700;font-size:12px">⏰ Call: '+unxDateTime(r.callAt)+'  ·  UNEXPECTED</div>'+
        '<div style="margin-top:8px;border-radius:8px;padding:9px 12px;display:flex;color:'+ink+';background:'+fill+'">'+
        '<span style="flex:1">'+line+'</span><b>₹'+r.earned.toLocaleString('en-IN')+'</b></div></div>';
    });
    document.getElementById('app').innerHTML='<div class="wrap anMod anModPf"><div class="topbar"><b>'+m.esc(code)+' · UNEXPECTED</b>'+
      '<button class="ghost" onclick="profSalary(\''+m.esc(code)+'\')">Back</button></div><div class="page">'+body+'</div></div>';
  }
  window.profUnexpected = profUnexpected;

  /* 🏍️🔒 V968 — বাইরে ঘোরা স্টাফের কোড। ফোনের `FieldVisit.FIELD_STAFF_MOBILES`-এর
     জোড়া; নতুন কেউ যোগ হলে TK বলবেন, তখন দুই জায়গাতেই এক লাইন। */
  var WLV1_FIELD_STAFF_CODES = ['JPE-RUPAM'];

  /* 🏍️ V968 — TK-এর দেখার পর্দা: কোন দিন কত ঘণ্টা · কত কিমি · এখন কোথায়। */
  async function profFieldVisit(code) {
    var host = document.getElementById('app');
    host.innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>Field Visit — ' + m.esc(code) + '</b>' +
      '<button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Back</button></div>' +
      '<div class="page"><div class="card mut">Loading...</div></div></div>';
    var days = [], visits = [];
    try {
      var client = await sb();
      var r = await client.schema('wn').from('field_visit_days')
        .select('*').eq('staff_code', code).order('work_date', { ascending: false }).limit(30);
      days = (r && r.data) ? r.data : [];
      var v = await client.schema('wn').from('doctor_visits')
        .select('work_date,doctor_name,visited_at').eq('staff_code', code)
        .order('visited_at', { ascending: false }).limit(300);
      visits = (v && v.data) ? v.data : [];
    } catch (_e) { }
    var byDate = {};
    visits.forEach(function (x) {
      var d = String(x.work_date || '').slice(0, 10);
      if (!byDate[d]) byDate[d] = [];
      byDate[d].push(x);
    });
    /* 🔴🔒 V1076 (০৪.০৯.২০২৬, TK: *"Rupam যে আজ ডাক্তার রেফারে গেল, কই আমি
       দেখতে পাচ্ছি না"* — খাতার সারি ১৩৯ ও ১৭৭, দুবার বলা)।
       আগে দিনের তালিকা বানানো হত **শুধু GPS-এর সারি থেকে**; ডাক্তারের সারি
       আনা হত ঠিকই, কিন্তু GPS-সারি না থাকলে দিনটাই উঠত না, তাই ওগুলো ফেলে
       দেওয়া হত। ⇒ এখন দুটো মিলিয়ে তালিকা — ফোনের সঙ্গে হুবহু এক নিয়ম। */
    var dayByDate = {};
    days.forEach(function (r) {
      var d = String(r.work_date || '').slice(0, 10);
      if (d && !dayByDate[d]) dayByDate[d] = r;
    });
    Object.keys(byDate).forEach(function (d) {
      if (d && !dayByDate[d]) dayByDate[d] = { work_date: d };
    });
    days = Object.keys(dayByDate).sort().reverse().slice(0, 30).map(function (d) { return dayByDate[d] });

    var body = '';
    if (!days.length) body = '<div class="card mut">No field visit recorded yet.</div>';
    days.forEach(function (r) {
      var date = String(r.work_date || '').slice(0, 10);
      var started = String(r.started_at || '');
      var ended = String(r.ended_at || '');
      var auto = !!r.auto_closed;
      /* 🕐🔒 V1333 (১১.০৯.২০২৬, TK-নির্দেশ ও গভীর যাচাই — তালিকা সারি ৪৩৪) —
         `toISOString()` সবসময় UTC তারিখ দেয়, ভারতের নয় (ফোনের কোড আগে থেকেই
         Asia/Kolkata ব্যবহার করে — এখানে বাদ পড়েছিল)। রাত ১২টা থেকে ভোর
         ৫.৩০-এর মধ্যে UTC তারিখ এখনো "গতকাল" থাকে, তাই আজকের চলতি ভিজিটও
         ভুল করে "NOT CLOSED" (লাল) দেখাতে পারত, "RUNNING"-এর বদলে। */
      var today = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Kolkata' });
      /* ⛔ V1076 — GPS-সারি নেই এমন দিন ভুল করে লাল "NOT CLOSED" দেখানো যাবে না। */
      var noGps = !started && !ended;
      /* 🩺🔒 V1333 — TK-রিপোর্ট: "COMPLETE" লেখা ছিল, অথচ "Hours -" — আগে এই
         লেবেল শুধু OUT চাপা হয়েছে কিনা দেখত, IN-এর তথ্য সত্যিই এল কিনা মেলাত
         না। এখন না মিললে সৎভাবে "INCOMPLETE DATA" (ফোনের হুবহু যমজ)। */
      var startedValid = !!started && isFinite(new Date(started).getTime());
      var endedValid = !!ended && isFinite(new Date(ended).getTime());
      var status = noGps ? 'NO GPS'
        : ((!ended && date === today) ? 'RUNNING'
          : (!ended ? 'NOT CLOSED'
            : (!startedValid ? 'INCOMPLETE DATA' : (auto ? 'AUTO CLOSED' : 'COMPLETE'))));
      var colour = status === 'NOT CLOSED' ? '#B42318'
        : ((status === 'AUTO CLOSED' || status === 'NO GPS' || status === 'INCOMPLETE DATA') ? '#8A5A00' : '#0B7A4B');
      var km = (Number(r.distance_m || 0) / 1000).toFixed(1) + ' km';
      var hrs = wlv1FvHours(r.started_at, r.ended_at);
      var docs = (byDate[date] || []).length;
      var map = '';
      if (r.last_lat && r.last_lng) {
        /* 🗺️🔒 V1452 (১৪.০৯.২০২৬, TK-নির্দেশ) — route_points থাকলে (নতুন
           দিনের সারি) পুরো দিনের রুট Google Maps-এর "dir" লিংক দিয়ে দেখানো
           হয় (প্রথম বিন্দু origin, শেষটা destination, মাঝেরগুলো waypoints);
           পুরনো দিনের সারিতে এই কলাম না থাকলে আগের মতোই শুধু শেষ বিন্দু। */
        var pts = Array.isArray(r.route_points) ? r.route_points : null;
        var mapUrl;
        if (pts && pts.length >= 2) {
          var ll = function (i) { return pts[i][0] + ',' + pts[i][1]; };
          var mid = pts.slice(1, -1).map(function (_, i) { return ll(i + 1); }).join('|');
          mapUrl = 'https://www.google.com/maps/dir/?api=1&origin=' + encodeURIComponent(ll(0)) +
            '&destination=' + encodeURIComponent(ll(pts.length - 1)) + '&travelmode=driving' +
            (mid ? '&waypoints=' + encodeURIComponent(mid) : '');
        } else {
          mapUrl = 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(r.last_lat + ',' + r.last_lng);
        }
        map = '<a class="pill blueP" style="text-decoration:none" target="_blank" rel="noopener" href="' + mapUrl + '">OPEN IN GOOGLE MAPS</a>';
      }
      /* 📍🔒 V1333 — আসল কারণ (V1156-এ কোডে সারানো): লোকেশন-অনুমতি না থাকলে
         GPS একটাও অবস্থান দেয় না, তাই দূরত্ব চিরকাল ০.০। এই ফিক্সের আগের
         পুরনো দিনগুলোতে এখনো এটাই দেখা যাবে, তাই এখানে সৎভাবে কারণ বলা হলো। */
      var zeroDistanceUnexplained = !noGps && endedValid && startedValid && Number(r.distance_m || 0) <= 0;
      body += '<div class="card"><div style="display:flex;justify-content:space-between;align-items:center">' +
        '<b>' + m.esc(wlv1FvDmy(date)) + '</b>' +
        '<span class="pill" style="color:' + colour + ';background:#F4F7FB">' + status + '</span></div>' +
        (noGps ? '' : '<div class="tiny mut" style="margin-top:6px">IN ' + m.esc(startedValid ? wlv1FvTime(started) : '-') +
          '  ·  OUT ' + m.esc(endedValid ? wlv1FvTime(ended) : '-') + '</div>') +
        '<div class="tiny mut" style="margin-top:2px">Hours ' + m.esc(hrs) + '  ·  Distance ' + m.esc(km) +
        '  ·  Doctors ' + docs + '</div>' +
        (auto ? '<div class="tiny mut">OUT TIME not marked - closed by app at 12:00 AM</div>' : '') +
        (noGps ? '<div class="tiny mut">Location was off on the phone - only the doctor visits were recorded</div>' : '') +
        (zeroDistanceUnexplained ? ('<div class="tiny mut">' + ((r.last_lat||r.last_lng) ? 'No accurate GPS fix (indoors?) - distance not counted' : '⚠ Location permission may have been off - distance not recorded') + '</div>') : '') +   // 📍 V1431 — অবস্থান থাকলে অনুমতির দোষ নয়
        (r.last_seen_at ? '<div class="tiny mut">Last seen ' + m.esc(wlv1FvTime(r.last_seen_at)) +
          '  ·  accuracy ±' + (r.last_acc_m || 0) + ' m' + ((Number(r.last_acc_m)||0) > 60 ? ' (approx.)' : '') + '</div>' : '') +   // 📍 V1431 — আনুমানিক অবস্থান চিহ্নিত
        (map ? '<div style="margin-top:8px">' + map + '</div>' : '') +
        '</div>';
    });
    host.innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>Field Visit — ' + m.esc(code) + '</b>' +
      '<button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Back</button></div>' +
      '<div class="page">' + body + '</div></div>';
  }

  function wlv1FvDmy(iso) {
    var p = String(iso || '').slice(0, 10).split('-');
    return p.length === 3 ? p[2] + '.' + p[1] + '.' + p[0] : String(iso || '');
  }
  function wlv1FvTime(iso) {
    try {
      var d = new Date(iso);
      if (isNaN(d.getTime())) return '-';
      /* 🔴 V1158 */
      return d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', timeZone: 'Asia/Kolkata' }).replace(':', '.');
    } catch (_e) { return '-' }
  }
  function wlv1FvHours(a, b) {
    try {
      if (!a) return '-';
      var s = new Date(a).getTime();
      var e = b ? new Date(b).getTime() : Date.now();
      if (!isFinite(s) || !isFinite(e) || e <= s) return '0h 00m';
      var mins = Math.round((e - s) / 60000);
      return Math.floor(mins / 60) + 'h ' + String(mins % 60).padStart(2, '0') + 'm';
    } catch (_e) { return '-' }
  }
  window.profFieldVisit = profFieldVisit;
  /* 🎨 V417গ: Salary Settings খোলা/গোটানো — ক্লাউড থেকে নতুন কিছু আনা হয় না। */
  function profToggleSalCfg() {
    try {
      var box = document.getElementById('salCfgBox');
      if (box) box.style.display = (box.style.display === 'none') ? 'block' : 'none';
    } catch (e) {}
  }
  window.profToggleSalCfg = profToggleSalCfg;
  /* 🔴 V430 — "Edit Salary — CODE" পর্দা, হুবহু ফোনের মতো। */
  async function profSalaryEdit(code) {
    var m = window.MOD, client = await sb();
    var sc = ((await client.schema('hr').from('salary_config').select('*').eq('person_code', code).maybeSingle()).data) || {};
    var on = !!sc.salary_enabled;
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>Edit Salary — ' + m.esc(code) + '</b>' +
      '<button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Back</button></div><div class="page">' +
      '<div class="card">' +
      '<label>Salary</label><select id="sEn" class="input"><option value="disabled"' + (on ? '' : ' selected') + '>disabled</option>' +
        '<option value="enabled"' + (on ? ' selected' : '') + '>enabled</option></select>' +
      '<label>Amount</label><input id="sAmt" class="input" type="number" value="' + m.esc(sc.salary_amount || '') + '">' +
      '<label>Salary Date</label><input id="sDate" class="input" value="' + m.esc(sc.salary_date || '') + '">' +
      '<div class="actions"><button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Cancel</button>' +
      '<button onclick="profSalaryCfgSave(\'' + m.esc(code) + '\')">Save</button></div></div>' +
      '</div></div>';
  }
  window.profSalaryEdit = profSalaryEdit;
  /* 🔵🔒 V416 (TK-নির্দেশ, ১৭.০৮.২০২৬) — বেতন আর "বাড়তি টাকা" আলাদা।
     ⛔ পুরনো সারিতে `kind` ঘরটা নেই ⇒ সেগুলো বেতনই ধরা হয়, তাই পুরনো হিসাব
        এক পয়সাও বদলায় না। ⛔ ফোনের `payKind()`/`dmy()`-র হুবহু একই নিয়ম। */
  /* 🔵 V417: এখনো "বাকি" না "দেওয়া হয়েছে"। পুরনো সারিতে ঘরটা নেই ⇒ PAID। */
  function salStatus(p){ return String((p && p.status) || 'PAID').trim().toUpperCase() || 'PAID'; }
  function salIsDue(p){ return salKind(p) === 'EXTRA' && salStatus(p) === 'DUE'; }
  function salKind(p){ return String((p && p.kind) || 'SALARY').trim().toUpperCase() || 'SALARY'; }
  /* ══════════════════════════════════════════════════════════════════════
     🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — Extra টাকা কোন রোগীর জন্য।
     TK: *"যেখানে ডিউ লেখা রয়েছে সেখানে চাপ দিলে যেন আমি বুঝতে পারি, এটা কোন
     পেশেন্টের জন্য... তার হিস্টরি যেন একবারেই ক্লিক করলে রিডাইরেক্ট হতে পারি।"*
     ⛔ ফোনের `StaffProfileActivity`-র হুবহু একই নিয়ম।
     ⛔ রোগীর আইডি `src_key`-তে আগে থেকেই আছে — আন্দাজ করা হয় না।
     ══════════════════════════════════════════════════════════════════════ */
  var SAL_PAT_CACHE = {};     /* patients.id → {name, mobile} */
  var SAL_LAST_PAYS = [];     /* শেষবার যে সারিগুলো আঁকা হয়েছে */
  /* 🐞🔒 V1029 — যাচাইয়ে ধরা: চাপ দিলে পপ-আপ উঠত না, কারণ সারিটা
     `SAL_LAST_PAYS` থেকে খুঁজে পাওয়া যেত না। এখন যে সারিটা আঁকা হচ্ছে ঠিক
     সেখানেই তার নিজের নকল রেখে দেওয়া হয় — খুঁজে না পাওয়ার পথ আর নেই। */
  var SAL_PAY_BY_ID = {};

  /* 🐞🔒 V1029 — সূত্র (`src_key`) ফাঁকা হলে কারণের লেখা থেকেই রোগীর কোড। */
  function salExtraPatientCode(x){
    try{
      var why=String((x&&x.extra_reason)||'').trim(); if(!why)return '';
      var parts=why.split('·'), i, t;
      for(i=0;i<parts.length;i++){
        t=parts[i].trim();
        if(/^[A-Za-z]{2,4}-\d{6,8}-\d{2,4}$/.test(t)) return t;
      }
      return '';
    }catch(e){ return ''; }
  }
  function salExtraPatientId(x){
    try{
      var k=String((x&&x.src_key)||'').trim();
      if(k.indexOf('INC:')===0){
        var rest=k.slice(4);
        var a=rest.indexOf(':'), b=rest.lastIndexOf(':');
        if(a>=0&&b>a) return rest.slice(a+1,b).trim();
      }
      /* 🐞🔒 V1029 — সূত্র ফাঁকা (হাতে বসানো সারি): কারণের লেখার কোড ধরে
         জমা তালিকা থেকেই রোগীটা বার করা হয়, তাই চাপ দিলে আর কিছু-না-হওয়া নয়।
         ⛔ নতুন কোনো cloud-read নেই — কম্পিউটারে জমা তালিকা থেকেই। */
      var cd=salExtraPatientCode(x); if(!cd) return '';
      var f=(load('patients')||[]).filter(function(r){return String((r&&r.patientId)||'')===cd})[0];
      return f?String(f.id||''):'';
    }catch(e){ return ''; }
  }

  /* নামগুলো একবারেই এনে বসানো — ব্যর্থ হলে আগের মতোই শুধু কোড থাকে। */
  async function salFillPatientNames(pays, redraw){
    try{
      var ids=[];
      (pays||[]).forEach(function(x){
        var id=salExtraPatientId(x);
        if(id && !SAL_PAT_CACHE[id] && ids.indexOf(id)<0) ids.push(id);
      });
      if(!ids.length)return;
      var rows=null;
      try{ rows=(load('patients')||[]).filter(function(r){return ids.indexOf(String(r.id))>=0}); }catch(e){}
      if(rows && rows.length){
        /* 🔵🔒 V521: `timeType`-ও জমা রাখা হয় — এটাই বলে দেয় টাকাটা কেন পাওনা।
           ⛔ নতুন কোনো cloud-read নয়; এটা ফোনের/ব্রাউজারের জমা তালিকা থেকেই। */
        rows.forEach(function(r){ SAL_PAT_CACHE[String(r.id)]={name:String(r.name||''),mobile:String(r.mobile||''),timeType:String(r.timeType||''),timeSource:String(r.timeSource||''),disease:String(r.disease||r.diagnosis||'')}; });
      }
      if(typeof redraw==='function' && rows && rows.length) redraw();
    }catch(e){}
  }

  /* 🧹🔒 V1041 (TK: *"Manually approved by TK এর মানেটা আগে আমাকে একটু বোঝান তো"*)।
     ⚠️ **দোষ আমার** — ওই লেখাটা আমারই দেওয়া SQL থেকে ডেটাবেসে বসেছিল, TK-এর
     কাছে ওটার কোনো মানে ছিল না। ⇒ পর্দায় দেখানোর সময় ওটা সোজা ইংরেজিতে
     বদলে যায়: `Added by hand`।
     ⛔ ডেটাবেসের একটা অক্ষরও বদলানো হয় না (TK-কে কোনো SQL চালাতে হবে না) —
        শুধু **দেখানোর সময়** লেখাটা পরিষ্কার করা হয়।
     ⛔ অন্য কোনো লেখা ছোঁয়া হয় না; নিজে টাইপ করা কারণ আগের মতোই থাকে। */
  /* 🕐🔒 V1042 (TK-নির্দেশ) — সময়ের ব্যাজ। কলটা চেম্বারের ফোনে এলে অ্যাপ
     কল-তালিকা দেখে নিজেই বোঝে ⇒ **AUTO UNEXPECTED**। স্টাফের নিজের ফোনে
     এলে অ্যাপ কিছুই জানে না, স্টাফ হাতে বেছে দেন ⇒ **UNEXPECTED (BY HAND)**।
     ⛔ পুরনো সারিতে ঘরটা ফাঁকা — সেখানে ব্যাজ হুবহু আগের মতোই থাকে। */
  /* 📅🔒 V1047 (TK-নির্দেশ: *"patient ID লাগবে না · কোন তারিখে কোন সময় Enquiry
     করা হয়েছে · কত তারিখে কোন সময় Registration হয়েছে · ট্রিটমেন্টের জন্য টাকা
     জমা করলে কত তারিখে কোন সময়"*) — তিনটে ধাপের তারিখ ও সময়।
     ⛔ সবই আগে থেকেই জমা আছে: এনকোয়ারির সময় `enquiries.createdAt`, রেজিস্ট্রেশনের
        `patients.createdAt`, আর ট্রিটমেন্টের টাকা `payments`-এ `payType='treatment'`
        (V418-এর SQL ঠিক এই শর্তেই ₹৪০০ দেয়, তাই দুই জায়গার নিয়ম মেলে)।
     ⛔ নতুন কোনো cloud-read নেই — ব্রাউজারের জমা তালিকা থেকেই।
     ⛔ যেটা পাওয়া যায়নি সেই লাইনটা বসেই না, আন্দাজে কিছু লেখা হয় না। */
  function salWhenText(iso){
    try{
      var t=String(iso||'').trim(); if(!t) return '';
      var mm=/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/.exec(t);
      if(mm) return mm[3]+'/'+mm[2]+'/'+mm[1]+' : '+salClock(mm[4],mm[5]);   /* 🔴 V1158 */
      var d=/^(\d{4})-(\d{2})-(\d{2})/.exec(t);
      return d ? (d[3]+'/'+d[2]+'/'+d[1]) : '';   /* 🔴 V1158 */
    }catch(e){ return ''; }
  }
  function salClock(hh,mi){
    var h=Number(hh), ap=h<12?'AM':'PM', h12=h%12; if(h12===0) h12=12;
    return h12+'.'+mi+' '+ap;   /* 🔴 V1158 */
  }
  /** ঐ রোগীর তিনটে ধাপ — এনকোয়ারি · রেজিস্ট্রেশন · ট্রিটমেন্টের টাকা। */
  function salSteps(pid){
    var out={enq:'',reg:'',trt:''};
    try{
      var pt=(load('patients')||[]).filter(function(r){return String(r.id)===String(pid)})[0];
      if(!pt) return out;
      out.reg = salWhenText(pt.createdAt || pt.registrationDate || pt.date || '');
      var mob=String(pt.mobile||'').replace(/\D/g,'').slice(-10);
      if(mob){
        var eq=(load('enquiries')||[]).filter(function(r){
          return String(r.mobile||'').replace(/\D/g,'').slice(-10)===mob; });
        eq.sort(function(a,b){ return String(a.createdAt||a.date||'') < String(b.createdAt||b.date||'') ? -1 : 1; });
        if(eq[0]) out.enq = salWhenText(eq[0].createdAt || eq[0].date || '');
      }
      var py=(load('payments')||[]).filter(function(r){
        return String(r.patientId||'')===String(pid)
          && /^treatment$/i.test(String(r.payType||''))
          && Number(String(r.amount||'').replace(/[^0-9.]/g,'')||0) > 0; });
      py.sort(function(a,b){ return String(a.createdAt||a.date||'') < String(b.createdAt||b.date||'') ? -1 : 1; });
      if(py[0]) out.trt = salWhenText(py[0].createdAt || py[0].date || '');
    }catch(e){}
    return out;
  }
  window.salSteps = salSteps;

  /* 👤🔒 V1045 — নামের সারিতে চাপ ⇒ ঐ রোগীর পুরো ডিটেলস (প্রকল্পের প্রমাণিত
     `summaryByMobile`)। ⛔ সারির নিজের চাপ (ছোট পপ-আপ) আগের মতোই আছে —
     এখানে `event.stopPropagation()` করা হয় বলে দুটো একসাথে খোলে না। */
  function salOpenPatient(mob){
    try{ if(!mob) return; summaryByMobile(String(mob)); }catch(e){}
  }
  window.salOpenPatient = salOpenPatient;

  function salTimeBadge(tt, src){
    var t=String(tt||'').trim(), sc=String(src||'').trim().toLowerCase();
    if(!/^unexpected time$/i.test(t)) return t?t.toUpperCase():'';
    if(sc==='auto') return '⏰ AUTO UNEXPECTED';
    if(sc==='hand') return '✍️ UNEXPECTED (BY HAND)';
    return '⏰ UNEXPECTED';
  }
  window.salTimeBadge = salTimeBadge;

  function salCleanWhy(t){
    try{
      var s=String(t||'');
      /* 🔴 V1043 (TK: *"auto unexpected লেখা আছে এক জায়গায় আবার লেখা added by hand"*)
         — TK ঠিক ধরেছেন, দুটো এক লাইনে এসে উল্টো মানে দিচ্ছিল। তাই লেখাটা আর
         বদলানো নয়, **একদম তুলে** দেওয়া হয় — সারিটা অ্যাপের নিজের সারির মতোই। */
      s = s.replace(/\s*[·|-]\s*Manually approved by TK\s*/gi, ' ');
      s = s.replace(/\s*Manually approved by TK\s*/gi, ' ');
      return s.replace(/\s{2,}/g,' ').replace(/^[\s·]+|[\s·]+$/g,'');
    }catch(e){ return String(t||''); }
  }
  window.salCleanWhy = salCleanWhy;

  /* চাপ দিলে ছোট পপ-আপ — নাম · মোবাইল · কেন · কত · কবে · অবস্থা,
     নিচে "Open History" (TK-এর বাছা পথ: আগে দেখে নেওয়া, তারপর যাওয়া)। */
  function salExtraWhy(payId){
    try{
      var x=SAL_PAY_BY_ID[String(payId)] ||
              (SAL_LAST_PAYS||[]).filter(function(a){return String(a.id)===String(payId)})[0];
      if(!x)return;
      var pid=salExtraPatientId(x);
      var c=SAL_PAT_CACHE[pid]||{name:'',mobile:'',timeType:''};
      /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — *"কী কারণে দিচ্ছি সেটা তো বোঝা
         যাচ্ছে না।"* Timing · কোন ধাপ · নিয়ম — তিনটেই এখানে।
         ফোনের `StaffProfileActivity.showExtraPatientPopup()`-এর হুবহু একই লেখা। */
      var tt = String(c.timeType||'').trim();
      var isUnexp = /^unexpected time$/i.test(tt);
      var stage = String(x.extra_reason||'').split('·')[0].trim();
      var stepTxt = /^registration$/i.test(stage) ? 'Registration Fee received  →  ₹100'
                  : /^treatment$/i.test(stage)    ? 'First Advance / Treatment payment received  →  ₹400'
                  : '';
      var rows=''
        + (c.name?  '<div class="pfStmtWhyRow"><span>Patient</span><b>'+m.esc(c.name)+'</b></div>':'')
        + (c.mobile?'<div class="pfStmtWhyRow"><span>Mobile</span><b>'+m.esc(c.mobile)+'</b></div>':'')
        + (tt? '<div class="pfStmtWhyRow"><span>Timing</span><b>'+m.esc(salTimeBadge(tt, c&&c.timeSource))+'</b></div>':'')
        + '<div class="pfStmtWhyRow"><span>For</span><b>'+m.esc(salCleanWhy(String(x.extra_reason||'-')))+'</b></div>'
        + (stepTxt? '<div class="pfStmtWhyRow"><span>Step</span><b>'+m.esc(stepTxt)+'</b></div>':'')
        + '<div class="pfStmtWhyRow"><span>Amount</span><b>'+m.money(x.amount)+'</b></div>'
        + '<div class="pfStmtWhyRow"><span>Date</span><b>'+m.esc(salDmy(x.paid_on))+'</b></div>'
        + '<div class="pfStmtWhyRow"><span>Status</span><b>'+(salIsDue(x)?'DUE (not paid yet)':'PAID')+'</b></div>'
        + (isUnexp
            ? '<div class="mut" style="margin-top:10px;font-size:12px;line-height:1.6">'
              + 'Rule: only an <b>UNEXPECTED TIME</b> enquiry earns extra.<br>'
              + '₹100 when that number registers and pays the fee,<br>'
              + '₹400 more when the same patient pays an advance.<br>'
              + salExtraShareLine(stage, x.amount) + '</div>'
            : (tt ? '<div style="margin-top:10px;font-size:12px;color:#B0392B;line-height:1.6">'
                    + '⚠️ This patient is not marked UNEXPECTED TIME.<br>'
                    + 'Extra income is only for unexpected-time enquiries — please check this entry.</div>'
                  : ''));
      var go = c.mobile
        ? '<button onclick="closeModal();summaryByMobile(\''+m.esc(c.mobile)+'\')">Open History</button>'
        : '';
      modal('<h2>Extra income - why?</h2><div class="card">'+rows+'</div>'
           +'<div class="actions">'+go+'<button class="ghost" onclick="closeModal()">Close</button></div>');
    }catch(e){ try{ console.warn('salExtraWhy', e && e.message); }catch(_){} }
  }
  window["salExtraWhy"]=salExtraWhy;

  function salIsExtra(p){ return salKind(p) === 'EXTRA'; }
  /* 🔴🔒 V936 (TK ৩১.০৮.২০২৬ — সম্পূর্ণ প্রজেক্টে এক ফরম্যাট): 2026-12-31 → 31.12.2026। ডেটাবেসে তারিখ আগের মতোই থাকে। */

  /* 🔵🔒 V532 (২২.০৮.২০২৬, TK-নির্দেশ) — **ভাগের হিসাবটা এখন সত্যি।**
     এতদিন সবসময় লেখা থাকত "Shared 50-50", কিন্তু ডেটাবেসের আসল নিয়ম
     (`hr.incentive_wanted()`, V418 SQL: `round(st.amt / s.n, 2)`) তা নয় —
     এনকোয়ারি ও রেজিস্ট্রেশন দুজন আলাদা হলে দু'ভাগ, একই লোক হলে পুরোটাই।
     ⛔ ফোনের `showExtraPatientPopup()`-এর হুবহু একই লেখা ও একই হিসাব।
     ⛔ নতুন কোনো cloud-read নেই — অঙ্কটা আগে থেকেই হাতে। */
  function salExtraShareLine(stage, amount){
    /* ⛔ `m` এখানে নেই — ওটা প্রতিটা async ফাংশনের **ভিতরের** নিজস্ব ঘর
       (`var m = window.MOD`)। তাই এখানে সরাসরি `window.MOD` ধরা হলো, আর
       না পেলে সাদামাটা "₹" — কোনো অবস্থাতেই পর্দা ভাঙে না। */
    var MM = (typeof window !== 'undefined' && window.MOD) ? window.MOD : null;
    var mny = function(v){ try{ return MM && MM.money ? MM.money(v) : ('₹' + v); }catch(e){ return '₹' + v; } };
    var full = /^registration$/i.test(String(stage||'')) ? 100
             : /^treatment$/i.test(String(stage||''))    ? 400 : 0;
    var got = Number(String(amount||'').replace(/[^0-9.]/g,'')) || 0;
    if(!full || !got) return 'Shared between the enquiry staff and the registering staff<br>when they are two different people.';
    if(got >= full - 0.01)
      return 'This entry: the <b>FULL ' + mny(full) + '</b> — enquiry and registration<br>by the same staff (or only one of the two could be identified).';
    return 'This entry: <b>' + mny(got) + '</b> of ' + mny(full) + ' — the rest goes to the<br>other staff (enquiry and registration by two different people).';
  }

  /* 🔴 V1162 — "09:15" বা "09:15:00" → "9.15 AM"। চেনা না গেলে যা এসেছে তাই। */
  function salAttTime12(raw){
    var t = String(raw || '').trim(); if(!t) return '';
    var p = t.split(':'); if(p.length !== 2 && p.length !== 3) return t;
    var h24 = parseInt(p[0], 10); if(isNaN(h24)) return t;
    var ap = h24 < 12 ? 'AM' : 'PM';
    var h12 = h24 === 0 ? 12 : (h24 > 12 ? h24 - 12 : h24);
    return h12 + '.' + p[1] + ' ' + ap;
  }
  function salDmy(v){
    var t = String(v || '').trim();
    var mm = /^(\d{4})-(\d{2})-(\d{2})/.exec(t);
    return mm ? (mm[3] + '/' + mm[2] + '/' + mm[1]) : t;   /* 🔴🔒 V1158 — এক ফরম্যাট */
  }
  /* 🔵 V417: Payment History খোলা/গোটানো — ক্লাউড থেকে নতুন কিছু আনা হয় না। */
  function profTogglePayHistory() {
    try {
      var box = document.getElementById('phBox');
      if (!box) return;
      box.style.display = (box.style.display === 'none') ? 'block' : 'none';
    } catch (e) {}
  }
  window.profTogglePayHistory = profTogglePayHistory;
  /* 🎨 V1181 — উপরের ⋮ খোলা/বন্ধ। ⛔ শুধু দেখানো, কোনো তথ্য ছোঁয় না। */
  function profSalMenuToggle(){ try{ var e=document.getElementById('salMenu'); if(e) e.style.display = (e.style.display==='block'?'none':'block'); }catch(_e){} }
  function profSalMenuHide(){ try{ var e=document.getElementById('salMenu'); if(e) e.style.display='none'; }catch(_e){} }
  window.profSalMenuToggle = profSalMenuToggle; window.profSalMenuHide = profSalMenuHide;

  /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬: "সব কিছু Android এর মত হোক") — Payment
     History এখন হুবহু ফোনের মতো (StaffProfileActivity.kt:1548-1573):
       · উপরে **চারটে মোট** — Salary paid (total) · Extra income paid ·
         Extra income due (থাকলে তবেই, লাল) · Grand total paid
       · তারপর শিরোনাম "All Entries (n)"
       · প্রতিটা সারি **এক লাইনে**: মাস · টাকা · উপায় · তারিখ · মন্তব্য
     আগে ওয়েবে কোনো মোটই ছিল না, শিরোনামও ছিল না, আর **মন্তব্যটা কোথাও
     দেখাত না** — তার বদলে "Paid By" দেখাত, যেটা ফোনে নেই।
     ⛔ কোনো অঙ্ক নতুন করে হিসাব করা হয়নি — একই সারিগুলো থেকেই যোগ হয়। */
  /* 🎨🔒 V443 (TK-approved 19.08.2026) — Payment History / Salary Statement
     professional presentation. Android V443-এর একই visual hierarchy:
       Summary → All Entries → aligned Mode/HISTORICAL + Date → footer totals.
     ⛔ হিসাব, sort order, database query, salary/extra/due rule একটুও বদলায়নি। */
  function salaryTable(pays, only) {
    var m = window.MOD;
    only = String(only || '').toUpperCase();
    var allPays = Array.isArray(pays) ? pays : [];
    var shownPays = only ? allPays.filter(function(x){ return only === 'EXTRA' ? salIsExtra(x) : !salIsExtra(x); }) : allPays.slice();
    var tS = 0, tE = 0, tD = 0, latest = '', newestYm = '', oldestYm = '';
    allPays.forEach(function (x) {
      var po = String(x.paid_on || '').slice(0, 10);
      if (salIsExtra(x)) {
        if (salIsDue(x)) tD += Number(x.amount || 0); else tE += Number(x.amount || 0);
      } else {
        tS += Number(x.amount || 0);
        var ym = String(x.for_month || String(x.paid_on || '').slice(0, 7));
        if (!newestYm || ym > newestYm) newestYm = ym;
        if (!oldestYm || ym < oldestYm) oldestYm = ym;
      }
    });
    /* 📅🔒 V1641 — header date comes only from the filtered history being shown. */
    shownPays.forEach(function(x){
      var po = String(x.paid_on || '').slice(0,10);
      if (po > latest) latest = po;
    });
    var names = ['January','February','March','April','May','June','July','August','September','October','November','December'];
    var shorts = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    function monthLabel(ym){
      try { var q=String(ym||'').split('-'); return names[Number(q[1])-1]+' '+q[0]; } catch(e) { return String(ym||''); }
    }
    function shortMonth(ym){
      try { var q=String(ym||'').split('-'); return shorts[Number(q[1])-1]+' '+q[0]; } catch(e) { return String(ym||''); }
    }
    function friendlyDate(iso){
      var mm = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(iso||''));
      if (!mm) return salDmy(iso);
      var mon = shorts[Number(mm[2])-1] || mm[2];
      return mm[3] + ' ' + mon + ' ' + mm[1];
    }
    function metric(label, value, cls){
      return '<div class="pfStmtMetric"><span>'+label+'</span><b class="'+(cls||'')+'">'+value+'</b></div>';
    }

    /* 🔒👁️ V1321 (TK-নির্দেশ ১০.০৯.২০২৬, ডেমো-প্রুফ "B" পাশ) — Salary paid
       (total) ও Grand total paid ডিফল্টে ডট দিয়ে ঢাকা, 👁 চাপলে দুটোই
       একসাথে দেখা যায় (pfStmtToggleMoney)। Extra income paid/due খোলাই
       থাকে। পর্দা নতুন করে খুললে আবার ঢাকা অবস্থা থেকেই শুরু হয়। */
    var lockPill = '<span style="display:inline-block;font-size:8.5px;font-weight:700;color:#9A6B00;background:#FFF4E5;border:1px solid #F0D6A6;padding:2px 6px;border-radius:20px;margin-top:3px">🔒 hidden</span>';
    var eyeBtn = '<button class="pfEyeBtn" onclick="pfStmtToggleMoney()">👁</button>';
    var summary = '';
    if (only === 'EXTRA') {
      /* 🔒 V1641 — Extra Income History summary contains Paid + Due only. */
      summary = '<div class="pfStmtSummary">' +
        '<div class="pfStmtSummaryHead"><b>Summary</b><span>' + (latest ? ('Up to '+m.esc(friendlyDate(latest))) : 'Current statement') + '</span></div>' +
        '<div class="pfStmtMetrics pfStmtMetrics2">' +
          metric('Extra income paid', m.money(tE), 'pfStmtGreen') +
          metric('Extra income due', m.money(tD), tD > 0 ? 'pfStmtRed' : 'pfStmtMuted') +
        '</div></div>';
    } else {
      summary = '<div class="pfStmtSummary">' +
        '<div class="pfStmtSummaryHead"><b>Summary</b><span>' + (latest ? ('Up to '+m.esc(friendlyDate(latest))) : 'Current statement') + '</span></div>' +
        '<div class="pfStmtMetrics">' +
          '<div class="pfStmtMetric"><span>Salary paid (total)</span>' + lockPill +
            '<b class="pfStmtGreen" id="pfStmtSalVal" data-real="'+m.esc(m.money(tS))+'" data-shown="0" style="display:inline-block">••••••</b>' + eyeBtn +
          '</div>' +
          metric('Extra income paid', m.money(tE), 'pfStmtGreen') +
          metric('Extra income due', m.money(tD), tD > 0 ? 'pfStmtRed' : 'pfStmtMuted') +
        '</div>' +
        '<div class="pfStmtGrand"><span>Grand total paid ' + lockPill + '</span>' +
          '<b id="pfStmtGrandVal" data-real="'+m.esc(m.money(tS+tE))+'" data-shown="0">••••••</b>' + eyeBtn +
        '</div>' +
      '</div>';
    }

    SAL_LAST_PAYS = allPays;
    /* 🔴 V511 — Extra সারির রোগীর নাম জমানো তালিকা থেকেই নেওয়া হয় (কোনো নতুন
       ক্লাউড-অনুরোধ নেই)। পাওয়া গেলে লাইনে নামটা বসে। */
    try{ salFillPatientNames(shownPays); }catch(e){}
    var headTitle = only === 'SALARY' ? 'Salary History' : (only === 'EXTRA' ? 'Extra Income History' : 'All Entries');
    var head = '<div class="pfStmtListHead"><b>'+headTitle+' ('+shownPays.length+')</b><span>Most recent</span></div>';
    /* 🧾 V1049 — এক রোগীর বাক্স একবারই আঁকা হয় (প্রতিবার নতুন করে শুরু)। */
    var SAL_SEEN_PID = {};
    var lines = shownPays.map(function (x) {
      var isExtra = salIsExtra(x), isDue = salIsDue(x);
      var ym = String(x.for_month || String(x.paid_on || '').slice(0, 7));
      var title = isExtra ? 'Extra' : monthLabel(ym);
      var why = isExtra ? salCleanWhy(String(x.extra_reason || '')) : String(x.remark || '');
      var mode = isDue ? 'DUE' : String(x.mode || '—');
      var modeCls = isDue ? ' due' : (/^(cash|online)$/i.test(mode) ? ' paid' : ' hist');
      var detail = why;
      if (!detail && !isExtra && /^historical$/i.test(mode)) detail = 'Salary paid - confirmed by Master';
      /* 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — **কোন রোগীর জন্য এই Extra টাকা।**
         রোগীর আসল আইডি `src_key`-তে আগে থেকেই আছে (V418: INC:REG:<patients.id>:<code>)।
         নাম জানা থাকলে লাইনেই দেখানো হয়; চাপ দিলে ছোট পপ-আপ, তারপর Open History।
         ⛔ ফোনের `StaffProfileActivity`-র হুবহু একই নিয়ম। */
      var vPid = isExtra ? salExtraPatientId(x) : '';
      var vNm  = vPid ? (SAL_PAT_CACHE[vPid] && SAL_PAT_CACHE[vPid].name) : '';
      /* 👤🔒 V1045 (TK: *"নাম মোবাইল নাম্বার এবং রোগের নাম থাকবে · নামের উপর চাপ
         দিলে যেন পেশেন্ট ডিটেলস ওপেন হয়"*) — নামের সারিতে এখন তিনটেই।
         ⛔ যেটা জানা নেই সেটা বসেই না (আগের মতোই), আন্দাজে কিছু লেখা হয় না। */
      var vMob = vPid ? String((SAL_PAT_CACHE[vPid] && SAL_PAT_CACHE[vPid].mobile) || '') : '';
      var vDis = vPid ? String((SAL_PAT_CACHE[vPid] && SAL_PAT_CACHE[vPid].disease) || '') : '';
      /* 🔵🔒 V521: লাইনের **সামনে** Timing চিহ্ন — পপ-আপ না খুলেও TK বুঝবেন
         টাকাটা অসময়ের এনকোয়ারির জন্য। ⛔ ঘরটা ফাঁকা হলে আগের মতোই কিছু নয়। */
      var vTt  = vPid ? String((SAL_PAT_CACHE[vPid] && SAL_PAT_CACHE[vPid].timeType) || '') : '';
      var mark = '';
      if (vTt) {
        mark = salTimeBadge(vTt, (vPid && SAL_PAT_CACHE[vPid]) ? SAL_PAT_CACHE[vPid].timeSource : '');
        /* 🧾 V1046 (TK: *"Registration  UNEXPECTED"* — এই ক্রমেই) — Extra সারিতে
           আগে কী কারণে, তারপর সময়ের ব্যাজ। ⛔ স্যালারির সারিতে আগের ক্রমই। */
        detail = detail ? (isExtra ? (detail + '  ·  ' + mark) : (mark + '  ·  ' + detail)) : mark;
        /* 🧾 V1047 (TK: *"patient ID লাগবে না"*) — Extra সারিতে রোগীর কোডটা আর
           দেখানো হয় না (নাম-মোবাইল-রোগ তো উপরেই আছে)। ⛔ ডেটাবেসে কোডটা
           আগের মতোই থাকে, রোগী খোঁজার কাজেও ওটাই ব্যবহার হয়। */
        /* 🧾 V1048 (TK: *"unexpected এর আগে আবার রেজিস্ট্রেশন কেন থাকবে?"*) —
           TK ঠিক ধরেছেন: ধাপের নামটা নিচের তালিকাতেই আছে, তাই উপরে দুবার হত।
           ⇒ Extra সারিতে এই লাইনে এখন **শুধু সময়ের ব্যাজ**; কোন ধাপের জন্য
             টাকাটা, সেটা নিচে ঐ ধাপের পাশেই `→ ₹১০০` হয়ে বসে। */
        if (isExtra) detail = mark;
      }
      /* 👤🔒 V1044 (TK: *"আমার মনে হয় পেশেন্ট এর নাম দরকার এখানে"*) — নামটা
         এতদিন লাইনের একদম শেষে কোডের পরে বসত, চোখেই পড়ত না। এখন **নিজের
         সারিতে, মোটা করে** — বাকি লেখাটা (সময়ের ব্যাজ · কী কারণে · কোড)
         আগের মতোই নিচে থাকে। ⛔ নাম না জানা থাকলে আগের মতোই কিছুই বসে না। */
      if (isExtra) { try { SAL_PAY_BY_ID[String(x.id||'')] = x; } catch(e){} }
      var vClick = vPid ? (' onclick="salExtraWhy(\''+m.esc(String(x.id||''))+'\')" style="cursor:pointer"') : '';

      /* 🧾🔒 V1046 (TK-নির্দেশ: *"আগে নাম, মোবাইল, রোগ · তারপর এর লাইনে
         Registration UNEXPECTED · তারপর কত টাকা পাবে"*) — Extra সারিটা এখন
         ঠিক এই ক্রমেই সাজে। ⛔ স্যালারির সারি এক অক্ষরও বদলায়নি — সেটা
         আগের গ্রিডেই আঁকা হয়, নিচের `else`-এ। */
      /* 🧾🔒 V1049 (TK ডেমো-"ক" পাশ করেছেন, ০৪.০৯.২০২৬) — **এক রোগী = এক বাক্স**।
         উপরে নাম·মোবাইল·রোগ, তারপর সময়ের ব্যাজ, তারপর ধাপগুলো তারিখ-সময় সহ আর
         যে ধাপের জন্য টাকা তার পাশেই অঙ্ক, সবার নিচে **Total**।
         ⛔ **Total সবসময় উপরে দেখানো অঙ্কগুলোর যোগফলই** — ডেমোতে একই রোগীর দুটো
            Registration সারি থাকায় "Total ₹২০০" কিন্তু পাশে ₹১০০ দেখাচ্ছিল; সেটা
            নিজে ধরে ঠিক করা হলো, এখন এক ধাপের সব সারি যোগ হয়ে একটাই অঙ্ক বসে।
         ⛔ যে সারির ধাপ Registration/Treatment কোনোটাই নয় (হাতে লেখা কারণ), সেটাও
            নিজের লাইনে দেখানো হয় — নইলে Total-এ থাকত কিন্তু চোখে পড়ত না।
         ⛔ রোগী চেনা না গেলে সারিটা **আগের মতোই একা** আঁকা হয় (নিচে), কিছু হারায় না।
         ⛔ উপরের Summary ও নিচের footer-এর সংখ্যাগুলো ছোঁয়া হয়নি — সেগুলো গোটা
            তালিকা থেকেই গোনা হয়, তাই টাকার হিসাব এক অক্ষরও বদলায়নি। */
      if (isExtra && vPid) {
        if (SAL_SEEN_PID[vPid]) return '';                 // একই রোগীর বাকি সারি
        SAL_SEEN_PID[vPid] = 1;
        var mine = shownPays.filter(function(q){
          return salIsExtra(q) && salExtraPatientId(q) === vPid; });
        function sumOf(re){
          return mine.filter(function(q){ return re.test(String(q.extra_reason||'').trim()); })
                     .reduce(function(a,q){ return a + Number(q.amount||0); }, 0);
        }
        var regAmt = sumOf(/^registration/i), trtAmt = sumOf(/^treatment/i);
        var others = mine.filter(function(q){
          var r=String(q.extra_reason||'').trim();
          return !/^registration/i.test(r) && !/^treatment/i.test(r); });
        var tot = mine.reduce(function(a,q){ return a + Number(q.amount||0); }, 0);
        var dueN = mine.filter(function(q){ return salIsDue(q); }).length;
        var payState = dueN === 0 ? 'PAID' : (dueN === mine.length ? 'DUE' : 'PART DUE');
        var stateCls = dueN === 0 ? ' paid' : ' due';

        var st = salSteps(vPid);
        function amtTag(v){ return v > 0 ? '<em class="pfXEarn">'+m.money(v)+'</em>' : ''; }
        var steps = '';
        if (st.enq) steps += '<div class="pfXStep"><span>Enquiry</span><b>'+m.esc(st.enq)+'</b></div>';
        if (st.reg || regAmt > 0)
          steps += '<div class="pfXStep"><span>Registration</span><b>'+m.esc(st.reg||'—')+'</b>'+amtTag(regAmt)+'</div>';
        if (st.trt || trtAmt > 0)
          steps += '<div class="pfXStep"><span>Treatment paid</span><b>'+m.esc(st.trt||'—')+'</b>'+amtTag(trtAmt)+'</div>';
        others.forEach(function(q){
          steps += '<div class="pfXStep"><span>Other</span><b>'+m.esc(salCleanWhy(String(q.extra_reason||'-')))+'</b>'
                 + amtTag(Number(q.amount||0)) + '</div>';
        });

        /* 🎨🔒 V1183 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"রোগের নামের
           পাশে থাকবে Unexpected Time"* ⇒ সময়ের ব্যাজটা এখন রোগের ঠিক পাশে,
           নিজের আলাদা লাইনটা আর নেই (একই কথা দুবার লেখা হত)।
           ⛔ ফোনেও হুবহু এই নিয়ম। ⛔ নতুন কোনো পড়া লাগেনি। */
        var whoRow = vNm
          ? ('<div class="pfXWho"'+(vMob?' onclick="event.stopPropagation();salOpenPatient(\''+m.esc(vMob)+'\')"':'')+'>'
             +'\uD83D\uDC64 '+m.esc(vNm)
             +(vMob?'<span class="pfXSub">\uD83D\uDCDE '+m.esc(vMob)+'</span>':'')
             +(vDis?'<span class="pfXSub">\uD83E\uDE7A '+m.esc(vDis)+'</span>':'')
             +(mark?'<span class="pfXSub">\u00b7 '+m.esc(mark)+'</span>':'')
             +'</div>')
          : '';
        return '<div class="pfStmtEntry pfXCard'+(dueN?' isDue':'')+'"'+vClick+'>' +
          whoRow +
          (steps ? ('<div class="pfXSteps">'+steps+'</div>') : '') +
          '<div class="pfXFoot"><b class="pfXAmt">Total '+m.money(tot)+'</b>' +
            '<span class="pfStmtBadge'+stateCls+'">'+payState+'</span></div>' +
        '</div>';
      }

      /* 🗑️🔒 V1195 (TK-নির্দেশ ও ফটো-প্রুফ পাশ) — ভুল করে বসে যাওয়া স্যালারির
         সারি **মাস্টার** মুছতে পারেন (নিশ্চিত করার প্রশ্নের পরেই; মোছার আগে
         hr.salary_deleted_log-এ কে-কখন-কী মুছল লেখা হয়)। ফোনের হুবহু একই নিয়ম।
         ⛔ স্টাফ/ডাক্তারের পর্দায় বোতামটা বসেই না। ⛔ Extra সারিতে নয়। */
      var delBtn = '';
      try{
        if(!isExtra && String(x.id||'') && (window.MOD && MOD.isMasterModule && MOD.isMasterModule()))
          delBtn = '<div class="pfStmtDel"><button class="ghost small" style="border-color:#E8B4B4;color:#C62828" '
                 + 'onclick="event.stopPropagation();salDeletePay(\''+m.esc(String(x.id))+'\')">Delete</button></div>';
      }catch(e){}
      try{ SAL_PAY_BY_ID[String(x.id||'')] = x; }catch(e){}
      return '<div class="pfStmtEntry'+(isDue?' isDue':'')+'"'+vClick+'>' +
        '<span class="pfStmtAccent"></span>' +
        '<div class="pfStmtMain"><b>'+m.esc(title)+'</b><span>'+m.money(x.amount)+'</span></div>' +
        '<div class="pfStmtMode"><span class="pfStmtBadge'+modeCls+'">'+m.esc(mode)+'</span></div>' +
        '<div class="pfStmtDate">'+m.esc(salDmy(x.paid_on))+'</div>' +
        (detail ? ('<div class="pfStmtDetail">'+m.esc(detail)+'</div>') : '') +
        delBtn +
      '</div>';
    }).join('');
    if (!shownPays.length) lines = '<div class="pfStmtEmpty">No payments.</div>';

    var period = (oldestYm && newestYm) ? (shortMonth(oldestYm)+' – '+shortMonth(newestYm)) : '—';
    var netPaid = only === 'SALARY' ? tS : (only === 'EXTRA' ? tE : (tS+tE));
    var footer = '<div class="pfStmtFooter">' +
      '<div><span>Total Entries</span><b>'+shownPays.length+'</b></div>' +
      '<div><span>Period</span><b>'+m.esc(period)+'</b></div>' +
      '<div><span>Net Paid</span><b>'+m.money(netPaid)+'</b></div>' +
    '</div>';
    return '<div class="pfStatement">'+summary+head+lines+footer+'</div>';
  }


  /* 🟣🔒 V1641 — Web Android parity: Salary History / Extra Income History
     are separate screens. Already-loaded rows are reused whenever possible. */
  async function profSalaryHistory(code, only) {
    var m = window.MOD;
    only = String(only || '').toUpperCase();
    var cache = window.__pfSalaryHistoryCache || null, pays = null;
    if (cache && String(cache.code||'') === String(code||'') && Array.isArray(cache.pays)) pays = cache.pays;
    if (!pays) {
      var client = await sb();
      pays = ((await client.schema('hr').from('salary_payments').select('*').eq('person_code', code).order('paid_on', { ascending: false }).limit(300)).data) || [];
      window.__pfSalaryHistoryCache = { code: String(code||''), pays: pays };
    }
    var title = only === 'EXTRA' ? 'Extra Income History' : (only === 'SALARY' ? 'Salary History' : 'Payment History');
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>'+m.esc(title)+' — '+m.esc(code)+'</b>' +
      '<button class="ghost" onclick="profSalary(\''+m.esc(code)+'\')">Back</button></div><div class="page">' +
      salaryTable(pays, only) + '</div></div>';
  }
  window.profSalaryHistory = profSalaryHistory;

  async function profSalaryCfgSave(code) {
    var m = window.MOD, client = await sb();
    var row = { person_code: code, salary_enabled: String((document.getElementById('sEn') || {}).value || '') === 'enabled', salary_amount: Number(document.getElementById('sAmt').value || 0), salary_date: document.getElementById('sDate').value, updated_by: (m.session() || {}).code, updated_at: new Date().toISOString() };
    // 🔵 TK-ORDER (07.08.2026): উপরের profSave-এর মতোই — upsert {error}/throw যাচাই,
    // সফল হলে তবেই ফেরা, ব্যর্থে সৎ বার্তা। ⛔ upsert/onConflict অপরিবর্তিত।
    try {
      var ex = (await client.schema('hr').from('salary_config').select('id').eq('person_code', code).maybeSingle()).data;
      if (ex) row.id = ex.id;
      var __up = await client.schema('hr').from('salary_config').upsert(row, { onConflict: 'person_code' });
      if (__up && __up.error) throw __up.error;
      try { toast('Saved'); } catch (e) {}
      profSalary(code);
    } catch (e) {
      try { toast('Not saved. Press Save Settings again.'); } catch (e2) {}
    }
  }
  async function profSalaryPay(code) {
    var m = window.MOD, client = await sb();
    // 🔴🆕🔒 TK-নির্দেশ (08.08.2026) — পেমেন্টে `for_month` যোগ (কোন মাসের বেতন) =
    // পেমেন্ট-তারিখের মাস। ⛔ বাকি সব ঘর ও `m.save`-এর কল আগের মতোই।
    var pd = document.getElementById('spDate').value;
    var forMonth = (String(pd).length >= 7) ? String(pd).slice(0, 7) : m.todayIST().slice(0, 7);
    var row = { id: m.uuid(), person_code: code, paid_on: pd, amount: Number(document.getElementById('spAmt').value || 0), mode: document.getElementById('spMode').value, paid_by: (m.session() || {}).code || 'master', remark: (document.getElementById('spRem') || {}).value || '', for_month: forMonth };
    await m.save('hr', 'salary_payments', row);
    profSalary(code);
  }

  /* 🔵🔒 V416 (TK-নির্দেশ): বেতনের বাইরে দেওয়া বাড়তি টাকা।
     ⛔ `kind='EXTRA'` ও `for_month` ফাঁকা হয়ে জমা হয় ⇒ বেতনের "বাকি কত"
        হিসাবে কখনো ঢোকে না। ⛔ ফোনের `addExtraIncome()`-এর হুবহু একই নিয়ম। */
  /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬: "সব কিছু Android এর মত হোক") — ফোনে এটা
     একটা **পূর্ণ পর্দা** (StaffProfileActivity.kt:1584-1596): Amount · Reason ·
     When (Paying now / Pay later (Due)) · Mode · "Save Extra Income" বোতাম।
     ওয়েবে চারটে ব্রাউজার-prompt বাক্স আসত, আর তার একটাতে নির্দেশ-লেখাও ছিল
     ("OK = Paying now / Cancel = Pay later") — TK-এর নিয়মে ওটা থাকার কথা নয়।
     ⛔ কী সেভ হয় (kind='EXTRA', status, for_month ফাঁকা) — এক অক্ষরও বদলায়নি। */
  function profExtraIncome(code) {
    var m = window.MOD;
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>Extra Income — ' + m.esc(code) + '</b>' +
      '<button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Back</button></div><div class="page">' +
      /* 🎨🔒 V1182 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — সোনালি পট্টি,
         সবকিছু একটাই কার্ডে, "When" ও "Mode" পাশাপাশি (ফোনের হুবহু যমজ)।
         ⛔ ঘরগুলোর `id` · কী সেভ হয় · নিয়ম — এক অক্ষরও বদলায়নি। */
      '<div class="card">' +
      '<div style="background:linear-gradient(90deg,#B45309,#E0A800);color:#fff;border-radius:12px;padding:10px 14px;margin-bottom:10px">' +
        '<b style="font-size:14px;letter-spacing:.6px">EXTRA INCOME</b></div>' +
      '<label>Amount</label><input id="exAmt" class="input" type="number" inputmode="numeric">' +
      '<label>Reason</label><input id="exWhy" class="input" type="text" placeholder="Bonus / Festival / Overtime">' +
      /* 🗓️🔒 V1273 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — খাতার সারি ৩৯৫):
         TK: *"Extra income কত তারিখে দেয়া হলো … প্রকৃত টাকা দেয়ার তারিখ"*।
         আগে তারিখের কোনো ঘরই ছিল না, সেভের সময় সবসময় `paid_on = আজ` বসত।
         এখন **Paid on** ঘর — ডিফল্ট আজ, `max` আজ ⇒ ভবিষ্যতের তারিখ বাছা যায় না।
         🎨 ফোনের হুবহু যমজ — "When" উপরে পুরো চওড়ায়, নিচে "Paid on | Mode"।
         ⛔ "Pay later (Due)" বাছলে তারিখের ঘরটা লুকোয় (তখনো টাকা দেওয়া হয়নি)।
         ⛔ ঘরগুলোর পুরনো `id` · কী সেভ হয় — এক অক্ষরও বদলায়নি। */
      '<label>When</label><select id="exWhen" class="input" onchange="profExtraWhenChange()"><option value="now">Paying now</option><option value="due">Pay later (Due)</option></select>' +
      '<div style="display:flex;gap:9px">' +
        '<div style="flex:1" id="exPaidOnBox"><label>Paid on</label><input id="exPaidOn" class="input" type="date"></div>' +
        '<div style="flex:1"><label>Mode</label><select id="exMode" class="input"><option>Cash</option><option>Online</option></select></div>' +
      '</div>' +
      '<div class="actions"><button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Cancel</button>' +
      '<button onclick="profExtraIncomeSave(\'' + m.esc(code) + '\')">Save Extra Income</button></div></div>' +
      '</div></div>';
    /* 🗓️ V1273 — ডিফল্ট আজ, আর আজকের পরের দিন বাছা যায় না। */
    try {
      var __t = m.todayIST(), __d = document.getElementById('exPaidOn');
      if (__d) { __d.value = __t; __d.max = __t; }
    } catch (e) {}
  }
  window.profExtraIncome = profExtraIncome;
  /* 🗓️ V1273 — "Pay later (Due)" হলে তারিখের ঘরটা লুকোয়, নইলে দেখায়। */
  function profExtraWhenChange() {
    try {
      var w = String((document.getElementById('exWhen') || {}).value || 'now');
      var b = document.getElementById('exPaidOnBox');
      if (b) b.style.display = (w === 'now') ? '' : 'none';
    } catch (e) {}
  }
  window.profExtraWhenChange = profExtraWhenChange;
  async function profExtraIncomeSave(code) {
    var m = window.MOD;
    var v = Number((document.getElementById('exAmt') || {}).value || 0);
    if (!(v > 0)) { try { toast('Enter an amount'); } catch (e) {} return; }
    var why = String((document.getElementById('exWhy') || {}).value || '').trim();
    if (!why) { try { toast('Enter a reason'); } catch (e) {} return; }
    var payingNow = String((document.getElementById('exWhen') || {}).value || 'now') === 'now';
    var mode = payingNow ? String((document.getElementById('exMode') || {}).value || 'Cash') : '';
    /* 🗓️ V1273 — "এখনই দিচ্ছি" হলে মাস্টারের বাছা তারিখ; "পরে দেব" হলে
       আগের মতোই আজ (তখনো টাকা দেওয়াই হয়নি)। ঘর ফাঁকা/ভুল হলেও আজ। */
    var exOn = String((document.getElementById('exPaidOn') || {}).value || '').slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(exOn) || exOn > m.todayIST()) exOn = m.todayIST();
    var row = {
      id: m.uuid(), person_code: code, paid_on: (payingNow ? exOn : m.todayIST()), amount: v, mode: mode,
      paid_by: (m.session() || {}).code || 'master', remark: '',
      for_month: '', kind: 'EXTRA', extra_reason: why,
      status: (payingNow ? 'PAID' : 'DUE')
    };
    await m.save('hr', 'salary_payments', row);
    try { toast('Extra income added'); } catch (e) {}
    profSalary(code);
  }
  window.profExtraIncomeSave = profExtraIncomeSave;

  /* 🔵 V417: ঠিক করে রাখা বাড়তি টাকা এখন দেওয়া হলো।
     ⛔ নতুন সারি বানানো হয় না — "বাকি" সারিটাই "দেওয়া হয়েছে" হয়, তাই একই টাকা
        দুবার গোনার সুযোগ নেই। ⛔ শুধু status · mode · paid_on বদলায়। */
  /* 🔴 V430 — ফোনে এটাও একটা **পর্দা** (StaffProfileActivity.kt:1295-1314):
     উপরে "Total to pay now" (লাল), তারপর প্রতিটা বাকি টাকার ₹ ও কারণ ধরে
     ধরে তালিকা, নিচে Mode ও "✅ Mark as Paid"। ওয়েবে শুধু একটা confirm বাক্স
     ছিল — কোন কোন টাকা মেটানো হচ্ছে তা দেখাই যেত না।
     ⛔ নতুন সারি বানানো হয় না; "বাকি" সারিটাই "দেওয়া হয়েছে" হয় — আগের নিয়মই। */
  async function profPayExtraDue(code) {
    var m = window.MOD, client = await sb();
    var rows = ((await client.schema('hr').from('salary_payments').select('*').eq('person_code', code)).data) || [];
    var due = rows.filter(salIsDue);
    if (!due.length) { try { toast('Nothing due'); } catch (e) {} return; }
    var sum = due.reduce(function (a, x) { return a + Number(x.amount || 0); }, 0);
    /* 👤🔒 V1040 (TK: "extra income আমি কোন পেশেন্ট এর জন্য দিচ্ছি সেটা বুঝতেই তো
       পারছি না" → "ওখানে চাপ দিলে পেশেন্টের ভিউ ওয়াল খুলতে হবে")।
       ⇒ প্রতিটা সারিতে রোগীর নাম ও মোবাইল, আর সারিতে চাপ দিলে ঐ রোগীর পুরো
         History খোলে — ফোনের হুবহু একই আচরণ (নিয়ম ৬.৬)।
       ⛔ নতুন কোনো cloud-read নেই; নাম জমা তালিকা থেকেই আসে। নাম আসতে দেরি হলে
         কোডটাই দেখায়, এসে গেলে নিজে থেকেই বসে যায়। */
    due.forEach(function (x) { try { SAL_PAY_BY_ID[String(x.id || '')] = x; } catch (e) {} });
    function whoOf(x) {
      var pid = salExtraPatientId(x), c = pid ? (SAL_PAT_CACHE[pid] || null) : null;
      return {
        name: c ? String(c.name || '').trim() : '',
        mobile: c ? String(c.mobile || '').trim() : '',
        code: salExtraPatientCode(x)
      };
    }
    /* 🎨🔒 V1183 (০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — এই পর্দাটা এখন
       Extra Income History-র **হুবহু একই বাক্স** আঁকে (এক রোগী = এক বাক্স:
       নাম · মোবাইল · রোগ · Unexpected Time, নিচে ধাপগুলো তারিখ-সময় সহ,
       তারপর Total ও অবস্থা)। প্রতিটা বাক্সে নিজের **Pay** বোতাম —
       TK: *"একটা একটা আলদা আলদা ও Paid করা যায় তার ব্যাবস্থা রাখতে হবে"*।
       ⛔ নিচের "Mark as Paid" আগের মতোই সবগুলো একসাথে মেটায়।
       ⛔ টাকার কোনো অঙ্ক/নিয়ম বদলায়নি; যা সেভ হয় তা হুবহু আগের মতোই।
       ⛔ ফোনের `payExtraDue()`-এর যমজ (নিয়ম ৬.৬)। */
    function draw() {
      var seen = {}, cards = '';
      due.forEach(function (x) {
        var pid = salExtraPatientId(x);
        if (!pid) return;
        if (seen[pid]) return;
        seen[pid] = 1;
        var mine = due.filter(function (q) { return salExtraPatientId(q) === pid; });
        var c = SAL_PAT_CACHE[pid] || null;
        var nm = c ? String(c.name || '').trim() : '';
        var mob = c ? String(c.mobile || '').trim() : '';
        var dis = c ? String(c.disease || '').trim() : '';
        var tt = c ? String(c.timeType || '').trim() : '';
        var mk = tt ? salTimeBadge(tt, c ? c.timeSource : '') : '';
        function sumOf(re) {
          return mine.filter(function (q) { return re.test(String(q.extra_reason || '').trim()); })
                     .reduce(function (a, q) { return a + Number(q.amount || 0); }, 0);
        }
        var regAmt = sumOf(/^registration/i), trtAmt = sumOf(/^treatment/i);
        var others = mine.filter(function (q) {
          var r = String(q.extra_reason || '').trim();
          return !/^registration/i.test(r) && !/^treatment/i.test(r); });
        var tot = mine.reduce(function (a, q) { return a + Number(q.amount || 0); }, 0);
        var st = salSteps(pid);
        function amtTag(v) { return v > 0 ? '<em class="pfXEarn">' + m.money(v) + '</em>' : ''; }
        var steps = '';
        if (st.enq) steps += '<div class="pfXStep"><span>Enquiry</span><b>' + m.esc(st.enq) + '</b></div>';
        if (st.reg || regAmt > 0)
          steps += '<div class="pfXStep"><span>Registration</span><b>' + m.esc(st.reg || '—') + '</b>' + amtTag(regAmt) + '</div>';
        if (st.trt || trtAmt > 0)
          steps += '<div class="pfXStep"><span>Treatment paid</span><b>' + m.esc(st.trt || '—') + '</b>' + amtTag(trtAmt) + '</div>';
        others.forEach(function (q) {
          steps += '<div class="pfXStep"><span>Other</span><b>' + m.esc(salCleanWhy(String(q.extra_reason || '-'))) + '</b>'
                 + amtTag(Number(q.amount || 0)) + '</div>';
        });
        var whoRow = nm
          ? ('<div class="pfXWho"' + (mob ? ' onclick="event.stopPropagation();salOpenPatient(\'' + m.esc(mob) + '\')"' : '') + '>'
             + '\uD83D\uDC64 ' + m.esc(nm)
             + (mob ? '<span class="pfXSub">\uD83D\uDCDE ' + m.esc(mob) + '</span>' : '')
             + (dis ? '<span class="pfXSub">\uD83E\uDE7A ' + m.esc(dis) + '</span>' : '')
             + (mk ? '<span class="pfXSub">\u00b7 ' + m.esc(mk) + '</span>' : '')
             + '</div>')
          : '';
        cards += '<div class="pfStmtEntry pfXCard isDue">' + whoRow +
          (steps ? ('<div class="pfXSteps">' + steps + '</div>') : '') +
          '<div class="pfXFoot"><b class="pfXAmt">Total ' + m.money(tot) + '</b>' +
            '<span class="pfStmtBadge due">DUE</span>' +
            '<button class="ghost" style="margin-left:auto;padding:6px 16px" ' +
              'onclick="event.stopPropagation();profPayOne(\'' + m.esc(code) + '\',\'' + m.esc(pid) + '\')">Pay ' + m.money(tot) + '</button>' +
          '</div></div>';
      });
      /* ⛔ রোগী চেনা যায়নি এমন সারি (পুরনো তথ্য) — আগের মতোই সাদামাটা লাইন,
         যাতে একটাও টাকা পর্দা থেকে হারিয়ে না যায়। */
      due.forEach(function (x) {
        if (salExtraPatientId(x)) return;
        cards += '<div class="pfPayLine"><div class="pfPayTop"><b>' + m.money(x.amount) + '</b>' +
                 '<span>' + m.esc(salCleanWhy(x.extra_reason || '')) + '</span></div></div>';
      });
      document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>Pay Extra Income — ' + m.esc(code) + '</b>' +
        '<button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Back</button></div><div class="page">' +
        '<div class="card"><div class="pfTotRow"><span>Total to pay now</span><b style="color:#B42318">' + m.money(sum) + '</b></div>' +
        cards +
        '<label>Mode</label><select id="exdMode" class="input"><option>Cash</option><option>Online</option></select>' +
        '<div class="actions"><button class="ghost" onclick="profSalary(\'' + m.esc(code) + '\')">Cancel</button>' +
        '<button onclick="profPayExtraDueSave(\'' + m.esc(code) + '\')">✅ Mark as Paid</button></div></div>' +
        '</div></div>';
    }
    draw();
    try { salFillPatientNames(due, draw); } catch (e) {}
  }
  /* সারিতে চাপ — ঐ রোগীর পুরো History (প্রকল্পের প্রমাণিত `summaryByMobile`)। */
  function profPayExtraOpen(payId) {
    try {
      var x = SAL_PAY_BY_ID[String(payId)];
      if (!x) return;
      var pid = salExtraPatientId(x), c = pid ? (SAL_PAT_CACHE[pid] || null) : null;
      var mob = c ? String(c.mobile || '').trim() : '';
      if (!mob) { try { toast('Patient mobile not found'); } catch (e) {} return; }
      summaryByMobile(mob);
    } catch (e) {}
  }
  window.profPayExtraOpen = profPayExtraOpen;
  window.profPayExtraDue = profPayExtraDue;
  async function profPayExtraDueSave(code) {
    var m = window.MOD, client = await sb();
    var rows = ((await client.schema('hr').from('salary_payments').select('*').eq('person_code', code)).data) || [];
    var due = rows.filter(salIsDue);
    if (!due.length) { try { toast('Nothing due'); } catch (e) {} return; }
    var mode = String((document.getElementById('exdMode') || {}).value || 'Cash');
    for (var i = 0; i < due.length; i++) {
      var r = due[i];
      r.status = 'PAID'; r.mode = mode; r.paid_on = m.todayIST();
      await m.save('hr', 'salary_payments', r);
    }
    try { toast('Paid'); } catch (e) {}
    profSalary(code);
  }
  window.profPayExtraDueSave = profPayExtraDueSave;
  /* 💸🔒 V1183 (TK-নির্দেশ) — **শুধু একজন রোগীর** বাকি টাকা মেটানো।
     ⛔ যা লেখা হয় তা "Mark as Paid"-এর হুবহু একই (status/mode/paid_on),
        তাই দুই বোতামে কখনো দুরকম কিছু ঘটতে পারে না। */
  async function profPayOne(code, pid) {
    var m = window.MOD, client = await sb();
    var rows = ((await client.schema('hr').from('salary_payments').select('*').eq('person_code', code)).data) || [];
    var mine = rows.filter(function (r) { return salIsDue(r) && salExtraPatientId(r) === String(pid); });
    if (!mine.length) { try { toast('Nothing due'); } catch (e) {} return; }
    var mode = String((document.getElementById('exdMode') || {}).value || 'Cash');
    for (var i = 0; i < mine.length; i++) {
      var r = mine[i];
      r.status = 'PAID'; r.mode = mode; r.paid_on = m.todayIST();
      await m.save('hr', 'salary_payments', r);
    }
    try { toast('Paid'); } catch (e) {}
    profPayExtraDue(code);
  }
  window.profPayOne = profPayOne;

  /* ===================================================================
     🟢 B629 (11.08.2026) — ওয়েব parity: (১) "Add Salary — choose month"
     (জয়েনিং ডেট থেকে যেকোনো মাস বেছে History নিজে ভরা যায়), (২) স্যালারির
     তারিখে Salary-Due reminder (Master: Staff Profiles তালিকার উপরে;
     Doctor/Staff: My Profile-এ নিজের বাকি)। ⛔ salary_config/salary_payments
     টেবিলেই বসে — নতুন কোনো টেবিল/SQL লাগে না; বিদ্যমান পর্দা অটুট।
     =================================================================== */
  var SAL_NAMES = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  function salMonthLabel(ym){ try{ var pp=String(ym).split('-'); return SAL_NAMES[Number(pp[1])-1]+' '+pp[0]; }catch(e){ return ym; } }
  function salPayMonth(p){ if(p&&p.for_month) return String(p.for_month); var d=String((p&&p.paid_on)||''); return d.length>=7?d.slice(0,7):''; }
  /** জয়েনিং মাস → চলতি মাস পর্যন্ত YYYY-MM (নতুন-আগে)। join_date না থাকলে শেষ ২৪ মাস। */
  function monthsFromJoin(joinIso){
    var out=[];
    try{
      var now=new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Kolkata'}));
      var curIdx=now.getFullYear()*12+now.getMonth();
      var startIdx;
      try{
        var jy=parseInt(String(joinIso).slice(0,4),10), jm=parseInt(String(joinIso).slice(5,7),10);
        var joinIdx=jy*12+(jm-1);
        startIdx=Math.max(curIdx-240, Math.min(joinIdx, curIdx));
      }catch(e){ startIdx=curIdx-23; }
      if(!(startIdx>=0)||startIdx>curIdx) startIdx=curIdx-23;
      for(var idx=curIdx; idx>=startIdx; idx--){ var y=Math.floor(idx/12), mo=(idx%12)+1; out.push((''+y).padStart(4,'0')+'-'+(''+mo).padStart(2,'0')); }
    }catch(e){}
    return out;
  }
  /** ফোনের SalaryReminder-এর হুবহু হিসাব: enabled + amount>0 + salary_date দেওয়া +
   *  আজকের দিন >= salary_date + এই মাসে এখনো পুরো দেওয়া হয়নি → বাকি টাকা ফেরত (নইলে 0)। */
  /* 🔴🔒 V1140 (০৬.০৯.২০২৬, TK-রিপোর্ট: *"আজকের থেকে জয়েন করেছে, তাহলে
     স্যালারি ডিউ কেন দেখাচ্ছে? ১ মাস যাবে তারপর তো স্যালারি দেবো"*) —
     এই মাসে (বা পরে) জয়েন করা কর্মীর নাম এ মাসে আর ওঠে না; প্রথম বেতন
     পরের মাস থেকে। ⛔ তারিখ ফাঁকা/অচেনা হলে আগের মতোই ধরা হয়।
     ⛔ ফোনের `SalaryReminder.kt`-এর হুবহু একই নিয়ম। */
  /* 💰🔒 V1198 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"আগস্টের স্যালারি সেপ্টেম্বরে
     দেয়া হয় … তাহলে নোটিফিকেশনে কেন বারবার স্যালারি ডিউ লেখা আসে"*।
     🔴 নিয়ম বসানো ছিল "চলতি মাসের বেতন চলতি মাসেই বাকি" ⇒ এখন বেতনের দিন এলে
        **আগের মাসের** বেতন বাকি কিনা দেখে। ফোনের `SalaryReminder`-এর হুবহু যমজ।
     ⛔ টাকার অঙ্ক কিছুই বদলায়নি, শুধু কোন মাসটা খোঁজা হবে। */
  function salDueMonth(){
    var now=new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Kolkata'}));
    now.setMonth(now.getMonth()-1);
    return (''+now.getFullYear())+'-'+(''+(now.getMonth()+1)).toString().padStart(2,'0');
  }
  function salaryDueThisMonth(sc, pays, joinDate){
    try{
      if(!sc || !sc.salary_enabled) return 0;
      var amount=Number(sc.salary_amount||0); if(!(amount>0)) return 0;
      var sd=parseInt(sc.salary_date||'0',10); if(!(sd>=1)) return 0;
      var now=new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Kolkata'}));
      if(now.getDate()<sd) return 0;
      var due=salDueMonth();
      var jm=String(joinDate||'').trim().slice(0,7);
      if(jm.length===7 && jm>due) return 0;   /* 🔴 V1140 + V1198 — বাকি মাসের পরে জয়েন */
      var paidThis=0;
      (pays||[]).forEach(function(p){ if(salIsExtra(p)) return; if(salPayMonth(p)===due) paidThis+=Number(p.amount||0); });
      return Math.max(0, amount-paidThis);
    }catch(e){ return 0; }
  }
  /** "Add Salary — choose month" পূর্ণ ফর্ম (join_date থেকে মাসের তালিকা; Paid/Due ট্যাগ)। */
  async function profSalaryAddMonth(code){
    var m=window.MOD, client=await sb();
    var sc=((await client.schema('hr').from('salary_config').select('*').eq('person_code',code).maybeSingle()).data)||{};
    var prof=((await client.schema('hr').from('staff_profiles').select('join_date').eq('person_code',code).maybeSingle()).data)||{};
    var pays=((await client.schema('hr').from('salary_payments').select('*').eq('person_code',code).limit(300)).data)||[];   /* 🔵 V818 — সীমা */
    var months=monthsFromJoin(prof.join_date||'');
    var paidSet={}; pays.forEach(function(p){ paidSet[salPayMonth(p)]=1; });
    var amount=Number(sc.salary_amount||0);
    var opts=months.map(function(ym){ return '<option value="'+ym+'">'+salMonthLabel(ym)+(paidSet[ym]?'  (Paid)':'  (Due)')+'</option>'; }).join('');
    document.getElementById('app').innerHTML='<div class="wrap anMod anModPf"><div class="topbar"><b>Add Salary — '+m.esc(code)+'</b>'+
      '<button class="ghost" onclick="profSalary(\''+m.esc(code)+'\')">Back</button></div><div class="page">'+
      /* 🔴 V430 — ফোনে ভিতরে দ্বিতীয় শিরোনাম নেই, উপরের নামটাই যথেষ্ট। */
      /* 🗓️🔒 V1274 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — খাতার সারি ৩৯৬):
         **আমারই ভুল সংশোধন।** V1248-এ (খাতার সারি ৩৬৯) "Paid on" ঘরটা
         **শুধু ফোনে** বসেছিল, কম্পিউটারে বসেনি ⇒ ওয়েবে সেভ করলে সবসময়
         আজকের তারিখ বসত (`paid_on: m.todayIST()`) — TK-এর নিয়ম ৮ ভাঙা।
         ⇒ এখন ওয়েবেও **Paid on** ঘর — ডিফল্ট আজ, `max` আজ ⇒ ভবিষ্যতের
           তারিখ বাছা যায় না (ফোনের `maxDate` নিয়মের হুবহু জোড়া)।
         🎨 সাজও ফোনের মতো — সোনালি পট্টি · "Paid on | Mode" পাশাপাশি ·
            বোতাম "Cancel · Salary Payment" (ফোনের V521 নিয়ম)।
         ⛔ কোন মাস বাছা যাবে · প্রস্তাবিত অঙ্ক · ডুপ্লিকেট-মাসের প্রশ্ন ·
            `for_month` — কিচ্ছু বদলায়নি; শুধু তারিখটা এখন হাতে বাছা যায়। */
      '<div class="card">'+
      '<div style="background:linear-gradient(90deg,#B45309,#E0A800);color:#fff;border-radius:12px;padding:10px 14px;margin-bottom:10px">'+
        '<b style="font-size:14px;letter-spacing:.6px">SALARY PAYMENT</b></div>'+
      '<label>Month</label><select id="amMonth" class="input">'+opts+'</select>'+
      /* 💰🔒 V1195 (TK: "September আমি 2044 দেই নাই একবারও") — অঙ্ক আর নিজে
         থেকে বসে না; ঘরটা ফাঁকাই থাকে, প্রস্তাবটা শুধু নিচে হালকা লেখায়। */
      '<label>Amount</label><input id="amAmt" class="input" type="number" placeholder="Enter amount">'+
      (amount>0?('<div style="font-size:12px;color:#8B98A9;padding:6px 2px 0">Suggested : \u20B9'+amount+'</div>'):'')+
      '<div style="display:flex;gap:9px">'+
        '<div style="flex:1"><label>Paid on</label><input id="amPaidOn" class="input" type="date"></div>'+
        '<div style="flex:1"><label>Mode</label><select id="amMode" class="input"><option>Cash</option><option>Online</option></select></div>'+
      '</div>'+
      '<div class="actions"><button class="ghost" onclick="profSalary(\''+m.esc(code)+'\')">Cancel</button>'+
      '<button onclick="profSalaryPayMonth(\''+m.esc(code)+'\')">Salary Payment</button></div></div>'+
            '</div></div>';
    /* 🗓️ V1274 — ডিফল্ট আজ, আর আজকের পরের দিন বাছা যায় না। */
    try {
      var __t=m.todayIST(), __d=document.getElementById('amPaidOn');
      if(__d){ __d.value=__t; __d.max=__t; }
    } catch(e){}
  }
  async function profSalaryPayMonth(code){
    var m=window.MOD;
    var mn=document.getElementById('amMonth'); var ym=mn?mn.value:'';
    var amt=Number((document.getElementById('amAmt')||{}).value||0);
    if(!ym){ try{ toast('Choose a month'); }catch(e){} return; }
    if(!(amt>0)){ try{ toast('Enter amount'); }catch(e){} return; }
    /* 🛡️🔒 V1195 (TK: "একই পেমেন্ট তিনবার দিলে আটকাবে না কেন") — ওই মাস আগে
       দেওয়া থাকলে **আগে জিজ্ঞাসা**, তারপরই সেভ। ফোনের হুবহু একই নিয়ম। */
    try{
      var client0=await sb();
      var old=((await client0.schema('hr').from('salary_payments').select('*').eq('person_code',code).limit(300)).data)||[];
      var prev=old.filter(function(p){ return !salIsExtra(p) && salPayMonth(p)===ym; })[0];
      if(prev){
        var okGo=confirm(salMonthLabel(ym)+' already paid\n\n\u20B9'+Number(prev.amount||0)+'  ·  '+(prev.mode||'—')+'  ·  '+String(prev.paid_on||'').slice(0,10)+
          '\nAlready recorded for this month.\n\nAdd one more payment for '+salMonthLabel(ym)+'?');
        if(!okGo) return;
      }
    }catch(e){}
    /* 🗓️ V1274 — আজকের বদলে মাস্টারের বাছা তারিখ। ঘর ফাঁকা/ভুল/ভবিষ্যতের
       হলে আগের মতোই আজ ⇒ কখনো ফাঁকা বা ভুল তারিখ জমা হয় না।
       ⛔ `for_month` আগের মতোই বাছা মাস — তারিখ থেকে **কখনো** ধরা হয় না। */
    var amOn=String((document.getElementById('amPaidOn')||{}).value||'').slice(0,10);
    if(!/^\d{4}-\d{2}-\d{2}$/.test(amOn) || amOn>m.todayIST()) amOn=m.todayIST();
    var row={ id:m.uuid(), person_code:code, paid_on:amOn, amount:amt, mode:(document.getElementById('amMode')||{}).value||'Cash', paid_by:(m.session()||{}).code||'master', remark:'', for_month:ym };
    try{ await m.save('hr','salary_payments',row); try{ toast('Payment added'); }catch(e){} }
    catch(e){ try{ toast('Retry'); }catch(_e){} }
    profSalary(code);
  }

  /* 🗑️🔒 V1195 (TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"মনে করুন আমি ভুল করে
     দিয়ে ফেলেছি, তাহলে সেটা ডিলিট করতে পারছি না কেন?"*
     ⛔ শুধু মাস্টার · নিশ্চিত করার প্রশ্নের পরেই · মোছার **আগে** খাতায়
        (hr.salary_deleted_log) কে-কখন-কী মুছল লেখা হয়; খাতায় লেখা না গেলে
        কিছুই মোছে না (নইলে টাকা হারাত, প্রমাণও থাকত না)। ফোনের হুবহু একই নিয়ম। */
  async function salDeletePay(id){
    var m=window.MOD;
    if(!(m && m.isMasterModule && m.isMasterModule())) return;
    var x=SAL_PAY_BY_ID[String(id||'')]; if(!x) return;
    var ym=String(x.for_month||String(x.paid_on||'').slice(0,7));
    var ok=confirm('Delete this salary payment?\n\n'+salMonthLabel(ym)+'  ·  \u20B9'+Number(x.amount||0)+
      '  ·  '+(x.mode||'—')+'  ·  '+String(x.paid_on||'').slice(0,10)+
      '\nIt will be removed from the total, and who deleted it will be recorded.');
    if(!ok) return;
    try{
      var client=await sb();
      var log={ id:m.uuid(), original_id:String(x.id||''), person_code:String(x.person_code||''),
        for_month:ym, amount:Number(x.amount||0), mode:String(x.mode||''), kind:String(x.kind||''),
        paid_on:String(x.paid_on||''), paid_by:String(x.paid_by||''), remark:String(x.remark||''),
        deleted_by:(m.session()||{}).code||'master', deleted_at:new Date().toISOString() };
      var ins=await client.schema('hr').from('salary_deleted_log').insert(log);
      if(ins && ins.error){ try{ toast('Not deleted — run the V1195 SQL patch first'); }catch(e){} return; }
      var del=await client.schema('hr').from('salary_payments').delete().eq('id', String(x.id||''));
      if(del && del.error){ try{ toast('Could not delete — try again'); }catch(e){} return; }
      try{ toast('Deleted'); }catch(e){}
      profSalary(String(x.person_code||''));
    }catch(e){ try{ toast('Could not delete — try again'); }catch(_e){} }
  }
  window.salDeletePay=salDeletePay;

  async function renderSelf() {
    var m = window.MOD, client = await sb();
    var code = (m.session() || {}).code;
    var host = document.getElementById('app');
    var p = null, sc = null, pays = [];
    /* 🔴🔴🔴🔒 V1570 (১৭.০৯.২০২৬, ফোনের ভিডিওতে ধরা পড়া বাগ, রুল ৮ — এখানে
       হুবহু একই ভুল, আরও বড় আকারে) — আগে এই তিনটে পড়াই কোনো `.eq(...)`
       ফিল্টার ছাড়া চলত, শুধু RLS-এর "নিজেরটাই দেখা যায়" নিয়মের ভরসায়।
       মাস্টারের RLS সবার সারি দেখতে দেয় বলে (ভুল করে এই পথে এলে) `staff_profiles`/
       `salary_config`-এ এলোমেলো যেকোনো একজন স্টাফের তথ্য, আর `salary_payments`-এ
       (কোনো filter/limit-ই ছিল না) **সবার সব বেতনের সারি** নেমে আসতে পারত।
       এখন তিনটেই নিজের `person_code` দিয়ে সরাসরি ছেঁকে নেওয়া হলো। */
    try {
      var __selfQ = client.schema('hr').from('staff_profiles').select('*');
      if (code) __selfQ = __selfQ.eq('person_code', code);
      p = (await __selfQ.maybeSingle()).data;
      var __scQ = client.schema('hr').from('salary_config').select('*');
      if (code) __scQ = __scQ.eq('person_code', code);
      sc = (await __scQ.maybeSingle()).data;
      var __payQ = client.schema('hr').from('salary_payments').select('*');
      if (code) __payQ = __payQ.eq('person_code', code);
      pays = (await __payQ.order('paid_on', { ascending: false })).data || [];
    } catch (e) {}
    p = p || { person_code: code };
    // 🎨 (03.08.2026, TK-অনুমোদিত ফটো-প্রুফ পাশ করার পরে) — B370-এর "View"
    // পাতার প্যানেল-ডিজাইনের শুধু-দেখা সংস্করণ (এখানে এডিট করা যায় না,
    // তাই কোনো input বাক্স নেই)। ⛔ সম্পূর্ণ ইনলাইন স্টাইল, styles.css
    // ছোঁয়া হয়নি। ⛔ salaryTable(pays) ফাংশন এক অক্ষরও বদলায়নি —
    // শুধু চারপাশের HTML রিস্টাইল হয়েছে।
    function row(label, val) {
      return '<div style="display:flex;justify-content:space-between;padding:11px 20px;border-bottom:1px solid #F5F6F8;font-size:13.5px">' +
        '<span style="color:#667085">' + label + '</span><span style="color:#1D2939;font-weight:500">' + val + '</span></div>';
    }
    function panel(title, rowsHtml) {
      return '<div style="background:#fff;border:1px solid #E4E8EE;border-radius:12px;box-shadow:0 1px 3px rgba(16,24,40,0.04);overflow:hidden">' +
        '<div style="padding:13px 20px;border-bottom:1px solid #EEF1F4;font-size:13px;font-weight:700;color:#101828;display:flex;align-items:center;gap:8px">' +
        '<span style="width:7px;height:7px;border-radius:50%;background:#0B6B3A;display:inline-block"></span>' + title + '</div>' + rowsHtml + '</div>';
    }
    var avatarHtml = p.photo_data ?
      '<img src="' + m.esc(p.photo_data) + '" style="width:64px;height:64px;object-fit:cover;border-radius:50%;border:2px solid #CFE9D8">' :
      '<div style="width:64px;height:64px;border-radius:50%;background:#E6F4EA;color:#0B6B3A;display:flex;align-items:center;justify-content:center;font-size:26px;border:2px solid #CFE9D8">👤</div>';
    host.innerHTML = '<div class="wrap anMod anModPf" style="max-width:1180px">' +
      '<div class="topbar" style="padding:14px 24px"><b>My Profile</b>' +
      '<button class="ghost" onclick="dashboard()">Home</button></div>' +
      '<div class="page" style="padding:20px 24px 40px">' +

      '<div style="background:#fff;border:1px solid #E4E8EE;border-radius:12px;padding:20px 26px;display:flex;align-items:center;gap:18px;box-shadow:0 1px 3px rgba(16,24,40,0.04);flex-wrap:wrap">' +
      avatarHtml +
      '<div>' +
      '<div style="font-size:20px;font-weight:700;color:#101828">' + m.esc(p.full_name || '(name not set by Master yet)') + '</div>' +
      '<div style="font-size:13.5px;color:#667085;margin-top:3px">' + m.esc(p.person_code) + ' · Mobile: ' + m.esc(m.fullMobile(p.link_mobile)) + '</div>' +
      '<div style="margin-top:10px;display:flex;gap:8px;flex-wrap:wrap">' +
      '<span style="font-size:12px;font-weight:600;padding:4px 11px;border-radius:999px;background:#E6F4EA;color:#0B6B3A">' + m.esc(p.designation || p.role_kind || 'Staff') + '</span>' +
      '<span style="font-size:12px;font-weight:600;padding:4px 11px;border-radius:999px;background:#EAF1FB;color:#1D4E89">' + m.esc(p.branch || '-') + '</span>' +
      '</div></div></div>' +

      '<div style="font-size:12.5px;font-weight:700;color:#667085;text-transform:uppercase;letter-spacing:.5px;margin:22px 2px 10px">Profile Details</div>' +
      '<div class="wlv1SafeTwoCol">' +
      panel('Personal Details', row('Date of Birth', m.esc(p.dob || '—')) + row('Gender', m.esc(p.gender || '—')) + row('Blood Group', m.esc(p.blood_group || '—')) + row('Qualification', m.esc(p.qualification || '—'))) +
      panel('Contact', row('Alternate Mobile', p.alt_mobile ? m.esc(m.fullMobile(p.alt_mobile)) : '—') + row('ID Type', p.gov_id_type ? (m.esc(p.gov_id_type) + ': ' + m.maskId((p.gov_id_last4 ? '0000' + p.gov_id_last4 : ''))) : '—')) +
      '</div>' +

      '<div style="font-size:12.5px;font-weight:700;color:#667085;text-transform:uppercase;letter-spacing:.5px;margin:22px 2px 10px">My Salary</div>' +
      '<div style="background:linear-gradient(120deg,#0B4F2A,#0B8A3E 70%);color:#fff;border-radius:12px;padding:16px 22px;display:flex;align-items:center;justify-content:space-between">' +
      (sc && sc.salary_enabled ? '<span>Monthly Salary</span><b style="font-size:22px">' + m.money(sc.salary_amount) + ' <span style="font-size:13px;font-weight:400;opacity:0.85">(day ' + m.esc(sc.salary_date || '-') + ')</span></b>' : '<span>Salary not enabled.</span>') +
      '</div>' +
      // 🟢 B629: নিজের এই মাসের বেতন বাকি থাকলে (salary date পেরিয়ে গেলে) মনে করিয়ে দেওয়া
      (salaryDueThisMonth(sc, pays) > 0 ? '<div style="border:1px solid #ffd58a;background:#fff7e6;border-radius:10px;padding:11px 16px;margin-top:8px"><b style="color:#B42318">' + m.esc(salMonthLabel(salDueMonth())) + ' salary due: ' + m.money(salaryDueThisMonth(sc, pays)) + /* 🔴 V430 — বন্ধনীর ভিতরের ছোট ব্যাখ্যা-লাইনটা তুলে দেওয়া হলো (TK-এর স্থায়ী
   নিয়ম: পর্দায় নির্দেশ/ব্যাখ্যা-লাইন থাকবে না)। বেতনের দিনটা উপরের সবুজ
   কার্ডেই লেখা আছে, তাই কোনো তথ্য হারায়নি। */
'</b></div>' : '') +

      /* 🗓️ V509 (TK-নির্দেশ): স্টাফের নিজের মাসিক হাজিরা-খাতা — DATE · IN · OUT · LEAVE */
      '<div style="margin:18px 0 4px"><button class="ghost" style="width:100%;text-align:center" onclick="myAttendanceSheet()">My Attendance Sheet</button></div>' +

      '<div style="font-size:12.5px;font-weight:700;color:#667085;text-transform:uppercase;letter-spacing:.5px;margin:22px 2px 10px">Payment History</div>' +
      '<div style="background:#fff;border:1px solid #E4E8EE;border-radius:12px;box-shadow:0 1px 3px rgba(16,24,40,0.04);overflow:hidden;padding:2px 0">' +
      salaryTable(pays) +
      '</div>' +

      '</div></div>';
  }

  window.staffProfiles = staffProfiles;
  window.myProfile = myProfile;
  window.myAttendanceSheet = myAttendanceSheet;   // 🗓️ V509
  window.profEdit = profEdit;
  window.profSave = profSave;

  /* 🧾🔒 V1052 (TK-নির্দেশ ০৪.০৯.২০২৬: *"বছরের শেষে যেন আমি স্টেটমেন্ট তুলতে পারি
     … স্টাফ কোন মাসে কত স্যালারি পেয়েছে এক্সট্রা ইনকাম কত পেয়েছে … পিডিএফ ফরমে"*
     এবং *"শুধু সারা বছর কেন, আমি যতদিন থেকে যতদিন খুশি … ব্যাংকের স্টেটমেন্টের
     মতো তারিখ থেকে তারিখ"*) — **তারিখ-থেকে-তারিখ স্টেটমেন্ট**।
     ⛔ কোনো নতুন হিসাব বানানো হয়নি — যে সারিগুলো এখন দেখানো হয় সেগুলোই তারিখ
        দিয়ে ছেঁকে মাস ধরে যোগ করা হয়। তাই এই পাতার সংখ্যা আর অন্য পর্দার সংখ্যা
        কখনো আলাদা হবে না (নিয়ম ৭ক-এর ২)।
     ⛔ টাকার তারিখ ধরা হয় `paid_on`; ফাঁকা হলে সারিটা বাদ যায় না, "No date"-এ বসে।
     ⛔ PDF আলাদা করে বানানো হয়নি — Print চাপলে ব্রাউজারের "Save as PDF"-ই
        যথেষ্ট (প্রকল্পের বাকি প্রিন্টও ঠিক এই পথেই যায়)। */
  function salYmd(v){ return String(v||'').slice(0,10); }
  /* 🧾🔒 V1054 (TK-নির্দেশ ০৪.০৯.২০২৬: *"September 2026 কে Sep 26 করুন"* —
     TK নিজেই তালিকা দিয়েছেন: Jan-26 · Feb-26 … Dec-26)। সরু পর্দায় মাসের ঘরটা
     আর দু'লাইনে ভাঙে না, তাই সারিগুলোও এক উচ্চতার হয়।
     ⛔ শুধু এই স্টেটমেন্টের টেবিলে — অন্য পর্দার মাসের লেখা ছোঁয়া হয়নি। */
  function salMonthName(ym){
    var N=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    try{
      var q=String(ym||'').split('-');
      return (N[Number(q[1])-1]||q[1])+'-'+String(q[0]).slice(-2);
    }catch(e){ return String(ym||''); }
  }
  async function profStatement(code, from, to){
    var m = window.MOD, client = await sb();
    var rows = ((await client.schema('hr').from('salary_payments').select('*').eq('person_code', code)).data) || [];
    var today = m.todayIST();
    if (!from) { var d = new Date(today); d.setMonth(d.getMonth()-11); d.setDate(1);
                 from = d.toISOString().slice(0,10); }
    if (!to) to = today;
    var inRange = rows.filter(function(x){
      var d = salYmd(x.paid_on); return d && d >= from && d <= to; });
    var noDate = rows.filter(function(x){ return !salYmd(x.paid_on); });

    var byMonth = {};
    inRange.forEach(function(x){
      /* 🔴🔒 V1432 (১৩.০৯.২০২৬, TK: "২ পর্দায় দুই রকম কেন — LAXMI-র স্যালারি", তালিকা ৫৪৯) — বেতন আগে
         "যেদিন দেওয়া" সেই মাসে বসত, অথচ Salary History "কোন মাসের বেতন" (for_month) দেখায় ⇒ দুই
         পর্দায় দুরকম। এখন Statement-ও History-র একই নিয়ম salPayMonth() (for_month, নইলে paid_on-এর মাস);
         বাড়তি টাকা আগের মতোই যেদিন দেওয়া সেই মাসে (for_month ফাঁকা)। From/To ছাঁকনি paid_on-এ, আগের মতোই। */
      var ym = salIsExtra(x) ? salYmd(x.paid_on).slice(0,7) : String(salPayMonth(x)||'').slice(0,7);
      var b = byMonth[ym] || (byMonth[ym] = {sal:0, exPaid:0, exDue:0});
      if (salIsExtra(x)) { if (salIsDue(x)) b.exDue += Number(x.amount||0); else b.exPaid += Number(x.amount||0); }
      else b.sal += Number(x.amount||0);
    });
    var yms = Object.keys(byMonth).sort();
    var tS=0, tP=0, tD=0;
    var body = yms.map(function(ym){
      var b = byMonth[ym]; tS+=b.sal; tP+=b.exPaid; tD+=b.exDue;
      return '<tr><td>'+m.esc(salMonthName(ym))+'</td><td class="num">'+m.money(b.sal)+'</td>'
           + '<td class="num">'+m.money(b.exPaid)+'</td>'
           + '<td class="num'+(b.exDue>0?' due':'')+'">'+m.money(b.exDue)+'</td>'
           + '<td class="num"><b>'+m.money(b.sal+b.exPaid)+'</b></td></tr>';
    }).join('');
    if (!yms.length) body = '<tr><td colspan="5" class="mut">No payments in this period.</td></tr>';

    document.getElementById('app').innerHTML =
      '<div class="wrap anMod anModPf"><div class="topbar noPrint"><b>Statement — '+m.esc(code)+'</b>'
      + '<button class="ghost" onclick="profSalary(\''+m.esc(code)+'\')">Back</button></div>'
      + '<div class="page"><div class="card noPrint pfStmRange">'
      +   '<label>From</label><input id="stFrom" class="input" type="date" value="'+m.esc(from)+'">'
      +   '<label>To</label><input id="stTo" class="input" type="date" value="'+m.esc(to)+'">'
      +   '<div class="actions"><button onclick="profStatementGo(\''+m.esc(code)+'\')">Show</button>'
      +   '<button class="ghost" onclick="window.print()">🖨 Print / PDF</button></div></div>'
      + '<div class="card pfStmSheet">'
      +   '<div class="pfStmTitle">SALARY &amp; EXTRA INCOME STATEMENT</div>'
      +   '<div class="pfStmSub">'+m.esc(code)+'  ·  '+m.esc(salDmy(from))+'  to  '+m.esc(salDmy(to))+'</div>'
      +   '<div class="pfStmScroll"><table class="pfStmTable"><thead><tr><th>Month</th><th class="num">Salary</th>'
      +     '<th class="num">Extra</th><th class="num">Due</th><th class="num">Total</th></tr></thead>'
      +   '<tbody>'+body+'</tbody>'
      +   '<tfoot><tr><td>TOTAL</td><td class="num">'+m.money(tS)+'</td><td class="num">'+m.money(tP)+'</td>'
      +     '<td class="num'+(tD>0?' due':'')+'">'+m.money(tD)+'</td><td class="num"><b>'+m.money(tS+tP)+'</b></td></tr></tfoot>'
      +   '</table></div>'
      +   (noDate.length ? '<div class="pfStmNote">'+noDate.length+' entry(ies) have no date and are not counted here.</div>' : '')
      + '</div></div></div>';
  }
  window.profStatement = profStatement;
  function profStatementGo(code){
    var f=(document.getElementById('stFrom')||{}).value||'';
    var t=(document.getElementById('stTo')||{}).value||'';
    if (f && t && f > t) { try{ toast('From date is after To date'); }catch(e){} return; }
    profStatement(code, f, t);
  }
  window.profStatementGo = profStatementGo;

  /* ⋮🔒 V1058 (TK: *"এই থ্রি ডটে চাপ দিলে … আসবে এবং সেটা কার্যকারী হতে হবে"*)
     — তিনটেই আসল কাজ করে, প্রকল্পের সেই একই ফাংশনগুলোই ডাকা হয়। */
  function profDots(code, name){
    var m = window.MOD;
    modal('<h2>' + m.esc(name || code) + '</h2><div class="card pfDotsMenu">'
      + '<button class="ghost" onclick="closeModal();profEdit(\'' + m.esc(code) + '\')">View profile</button>'
      /* \u{1F3C6} V1091 (TK: *"\u09a1\u09be\u09a8\u09a6\u09bf\u0995\u09c7 \u09a5\u09cd\u09b0\u09bf \u09a1\u099f\u09c7\u09b0 \u09ae\u09a7\u09cd\u09af\u09c7 \u09a5\u09be\u0995\u09ac\u09c7"*) \u2014 \u0995\u09be\u099c \u098f\u0995 \u0985\u0995\u09cd\u09b7\u09b0\u0993 \u09ac\u09a6\u09b2\u09be\u09df\u09a8\u09bf\u0964 */
      + '<button class="ghost" onclick="closeModal();staffPerformanceOne(\'' + m.esc(code) + '\')">Performance</button>'
      + '<button class="ghost pfDanger" onclick="closeModal();profSuspend(\'' + m.esc(code) + '\')">Suspend</button>'
      + '<button class="ghost pfDanger" onclick="closeModal();profRemove(\'' + m.esc(code) + '\')">Remove</button>'
      + '</div><div class="actions"><button class="ghost" onclick="closeModal()">Close</button></div>');
  }
  window.profDots = profDots;

  window.profSalary = profSalary;
  window.profSalaryCfgSave = profSalaryCfgSave;
  window.profSalaryPay = profSalaryPay;
  window.profSalaryAddMonth = profSalaryAddMonth;
  window.profSalaryPayMonth = profSalaryPayMonth;
  /* =====================================================================
     🏆🔒 V419 — STAFF PERFORMANCE (TK-নির্দেশ, ১৭.০৮.২০২৬)
     TK চারটেই দেখতে চেয়েছেন: রোগী আনার কাজ · ফলোআপ ও কল · টাকা আদায় ·
     হাজিরা ও রিপোর্ট। আর পর্দা "দুটোই" — সবার তালিকা, নামে চাপলে একজনের পুরো হিসাব।
     ⛔ পুরো গণনা ডেটাবেসের ভিতরে (`hr.staff_performance`) ⇒ ফোন ও ওয়েবে সংখ্যা
        আলাদা হওয়ার সুযোগ নেই, আর এক ডাকে ছোট্ট উত্তর আসে (Egress-এ সস্তা)।
     ⛔ একটাও সারি লেখা/বদলানো হয় না — কেবল পড়া।
     ⛔ পর্দায় বাংলা লেখা নেই, নির্দেশ/সাহায্য-লাইনও নেই (TK-এর স্থায়ী নিয়ম)।
     ===================================================================== */
  var PERF_NAMES = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  /* 🔵 V420 (TK-নির্দেশ: "daily performance দেখার ব্যবস্থা রাখতে হবে") —
     একই পর্দায় দুটোই: `2026-08` = গোটা মাস · `2026-08-17` = শুধু ওই দিন। */
  function perfIsDay(k){ return /^\d{4}-\d{2}-\d{2}$/.test(String(k || '')); }
  function perfLabel(k){
    try {
      var p = String(k).split('-');
      if (perfIsDay(k)) return p[2] + '.' + p[1] + '.' + p[0];   /* 🔴🔒 V936 — এক ফরম্যাট */
      return PERF_NAMES[Number(p[1]) - 1] + ' ' + p[0];
    } catch (e) { return k; }
  }
  function perfNum(v){ var n=Number(v||0); return isFinite(n)?n:0; }

  /* 🔧 V421খ (TK-নির্দেশ, ১৭.০৮.২০২৬): *"Day month আবার ক্যালেন্ডার তিনটে রাখার
     দরকার নেই · Month & calendar থাকবে · ক্যালেন্ডারে চাপ দিলে pop up ক্যালেন্ডার
     খুলবে · তারিখ পছন্দ করলে অটোমেটিক সেই তারিখের পারফরম্যান্স · অন্যথায় ডিফল্ট
     আজকের · আর এগুলো হেডারে থাকবে"*
     ⇒ হেডারে **দুটোই** — [Month] আর তারিখের বাক্স। বাক্সে চাপলে ফোনের নিজের
       পপ-আপ ক্যালেন্ডার খোলে (`input type=date`), তারিখ বাছলেই সঙ্গে সঙ্গে
       ওই দিনের হিসাব। ⛔ ক্যালেন্ডার-ইমোজি কোথাও নেই (TK-এর স্থায়ী নিয়ম)।
     ⛔ ডিফল্ট = আজকের দিন। */
  function perfChips(curKey, mode, code, viaList) {
    var m = window.MOD;
    var isDay = perfIsDay(curKey);
    var today = m.todayIST();
    var vl = viaList ? 'true' : 'false';
    var monthCall = (mode === 'one')
      ? "staffPerformanceOne('" + m.esc(code) + "','" + today.slice(0, 7) + "'," + vl + ")"
      : "staffPerformance('" + today.slice(0, 7) + "')";
    var dayCall = (mode === 'one')
      ? "staffPerformanceOne('" + m.esc(code) + "',this.value," + vl + ")"
      : "staffPerformance(this.value)";
    /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬) — ফোনের চিপ দুটো হুবহু
       (StaffProfileActivity.kt:1046-1063): বাছা না থাকলে **সাদা জমিন, সবুজ
       লেখা ও সবুজ পাড়**; বাছা থাকলে **ভরাট সবুজ (#0A5C33), সাদা লেখা**।
       ওয়েবে Month কখনো সবুজ ভরাট হত না, আর তারিখের বাক্সটা ছোট পর্দায়
       CSS-এর জোরে সাদা-কালো হয়ে যেত — বাছা কোনটা বোঝাই যেত না। */
    return '<span class="pfChipRow">' +
      '<button class="pfChip' + (isDay ? '' : ' pfChipOn') + '" onclick="' + monthCall + '">Month</button>' +
      '<span class="wlv1DateBox pfChip' + (isDay ? ' pfChipOn' : '') + '">' + m.esc(perfLabel(curKey)) +
        '<input type="date" value="' + m.esc(isDay ? curKey : today) + '" onchange="' + dayCall + '"></span>' +
    '</span>';
  }

  async function perfRows(month) {
    var client = await sb();
    try {
      var r = await client.schema('hr').rpc('staff_performance', { p_month: month });
      if (r && r.error) return null;
      /* 🔴 TK-নির্দেশ (১৭.০৮.২০২৬): "ডাক্তারদের বাদ দিয়ে দিন" — এই তালিকা শুধু
         কর্মীদের। ডেটাবেসেও একই ছাঁকনি বসানো আছে; এটা দ্বিতীয় স্তর, যাতে পুরনো
         ডেটাবেসেও (নতুন SQL না চালালেও) ডাক্তার আর তালিকায় না ওঠেন। */
      /* 🔴 V427 (TK-নির্দেশ ১৭.০৮.২০২৬): *"কোচবিহার এর সমস্ত staff & Branch এর
         নম্বর একের পর এক থাকতে হবে"* ⇒ তালিকা এখন **ব্রাঞ্চ ধরে সাজানো**, এক
         ব্রাঞ্চের সবাই পরপর; ভিতরে নাম অনুসারে। ⛔ শুধু সাজানোর ক্রম — একটাও
         সংখ্যা বদলায় না। ফোনের তালিকাতেও হুবহু একই ক্রম। */
      var __staff = (Array.isArray(r && r.data) ? r.data : []).filter(function (x) {
        return !/^DR-/i.test(String((x && x.person_code) || ''));
      });
      /* 🔴🔒 V428 (TK-নির্দেশ ১৭.০৮.২০২৬: *"8514002200 — ওটা ব্রাঞ্চ হিসাবেই গন্য
         হোক"*) — চেম্বারের সাধারণ নম্বরে করা কাজ কোনো staff-এর নামে ওঠে না, তাই
         Collection কম দেখাত। এখন সেগুলো **ব্রাঞ্চের নিজের সারি** হয়ে তালিকায় আসে।
         ⛔ পুরনো `staff_performance`-এ হাত দেওয়া হয়নি — আলাদা ফাংশন।
         ⛔ ডাক ব্যর্থ হলে বা সব শূন্য হলে কিছুই যোগ হয় না; ফোনের হুবহু একই নিয়ম। */
      try {
        var rb = await client.schema('hr').rpc('branch_performance', { p_month: month });
        if (rb && !rb.error && Array.isArray(rb.data)) {
          rb.data.forEach(function (b) {
            var busy = Number(b.enquiry_count || 0) + Number(b.registration_count || 0) +
                       Number(b.treatment_count || 0) + Number(b.rmp_added || 0);
            var money = Number(b.cash_collected || 0) + Number(b.online_collected || 0);
            if (busy > 0 || money > 0) __staff.push(b);
          });
        }
      } catch (e) {}
      /* ক্রম: আগে ব্রাঞ্চ, ভিতরে staff-রা নাম অনুসারে, আর ওই ব্রাঞ্চের নিজের
         সারিটা সবার **শেষে** — তাহলে যোগফল চোখে পড়ে সহজে। */
      function __isBr(x) { return /^BRANCH-/i.test(String((x && x.person_code) || '')) ? 1 : 0; }
      return __staff.sort(function (a, b) {
        var ab = String((a && a.branch) || '').toUpperCase(), bb = String((b && b.branch) || '').toUpperCase();
        if (ab !== bb) return ab < bb ? -1 : 1;
        if (__isBr(a) !== __isBr(b)) return __isBr(a) - __isBr(b);
        var an = String((a && a.full_name) || (a && a.person_code) || '').toUpperCase();
        var bn = String((b && b.full_name) || (b && b.person_code) || '').toUpperCase();
        return an < bn ? -1 : (an > bn ? 1 : 0);
      });
    } catch (e) { return null; }
  }

  /* ---- ১) সবার তালিকা এক পর্দায় ---- */
  async function staffPerformance(month) {
    var m = window.MOD;
    if (!m.isMasterModule()) return pfToast('Only Master');
    /* 🔧 V421খ: ডিফল্ট **আজকের দিন** (আগে মাস ছিল)। Month চাপলে চলতি মাস। */
    var ym = month || m.todayIST();
    /* 🔧 V422খ (TK-নির্দেশ: *"উপরে ডান সাইডে Back বটম রাখার দরকার কি?"*) —
       হেডারে এখন শুধু শিরোনাম + Month + তারিখ। Back একদম **নিচে** (ফোনের মতোই,
       আর TK-এর পুরনো নিয়ম *"Back বটম নিচে বসবে"* মেনে)।
       ⛔ ওয়েবে ফোনের নিজের Back বোতাম নেই, তাই বোতামটা তুলে দেওয়া হয়নি —
          শুধু জায়গা বদলেছে, নইলে ফেরার পথ বন্ধ হয়ে যেত। */
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf">' +
      '<div class="topbar"><b>Performance</b>' + perfChips(ym, 'list', '', false) + '</div><div class="page">' +
      '<div id="perfOut" class="card mut">Loading...</div>' +
      '<div class="actions" style="margin-top:12px"><button class="ghost" onclick="staffProfiles()">Back</button></div>' +
      '</div></div>';
    var rows = await perfRows(ym);
    /* 📱🔒 V813 — ভার্সনের তালিকাও আনা হয় (person_code → version)।
       ⛔ ব্যর্থ হলে ম্যাপ ফাঁকা থাকে — পারফরম্যান্সের পর্দা আগের মতোই
          পুরোপুরি চলে, একটাও সংখ্যা আটকায় না।
       ⛔ "সর্বশেষ ভার্সন" আসে `version.json` থেকে (ফোনের অ্যাপও ঠিক ওটাই
          পড়ে) — নইলে তালিকার সবচেয়ে বড় সংখ্যাটাই ধরা হয়। */
    var verMap = {}, verLatest = 0;
    try {
      var vclient = await sb();
      var vres = await vclient.schema('hr').rpc('app_devices_list', {});
      if (!vres.error && vres.data && vres.data.length) {
        vres.data.forEach(function (d) {
          var c = String(d.person_code || '').trim().toUpperCase();
          if (c) verMap[c] = parseInt(d.app_version_code, 10) || 0;
          var dv = parseInt(d.app_version_code, 10) || 0;
          if (dv > verLatest) verLatest = dv;
        });
        try {
          var vjr = await fetch('version.json?t=' + Date.now(), { cache: 'no-store' });
          if (vjr.ok) { var vj = await vjr.json(); var jc = parseInt(vj.versionCode, 10) || 0; if (jc > verLatest) verLatest = jc; }
        } catch (e2) { }
      }
    } catch (e) { verMap = {}; verLatest = 0; }
    var out = document.getElementById('perfOut'); if (!out) return;
    if (rows === null) { out.className = 'card mut'; out.textContent = 'Could not load. Please try again.'; return; }
    if (!rows.length) { out.className = 'card mut'; out.textContent = 'No staff yet.'; return; }
    out.className = '';
    out.innerHTML = rows.map(function (x) {
      var reg = perfNum(x.registration_count), trt = perfNum(x.treatment_count);
      var money = perfNum(x.cash_collected) + perfNum(x.online_collected);
      return '<div class="card" style="cursor:pointer" onclick="staffPerformanceOne(\'' + m.esc(x.person_code) + '\',\'' + m.esc(ym) + '\',true)">' +
        '<div style="display:flex;justify-content:space-between;align-items:center">' +
          /* 🔴 V430 — ফোনে নাম ফাঁকা থাকলে কোডটাই দেখায় (kt:1115), তাই সারি
             কখনো নামহীন হয় না। ওয়েবে ফাঁকা বোল্ড লাইন পড়ে থাকত। */
          '<b style="color:#0A5C33;font-size:15px">' + m.esc(x.full_name || x.person_code) + '</b>' +
          '<span style="color:#9AA8B5">&rsaquo;</span></div>' +
        /* 📱🔒 V813 — কোড · ব্রাঞ্চ-এর পাশেই ফোনের ভার্সনের ট্যাগ (ফোনের হুবহু জোড়া)।
           ⛔ পুরনো লাইনটা একটুও বদলায়নি, শুধু পাশে একটা ট্যাগ যোগ হলো। */
        '<div style="font-size:12px;color:#3B5A49;margin-top:2px">' + m.esc(x.person_code) + ' · ' + m.esc(x.branch) +
          perfVerChip(x.person_code, verMap, verLatest) + '</div>' +
        '<div style="display:flex;gap:8px;margin-top:9px">' +
          /* 🔴 V429 (TK-নির্দেশ: ওয়েব হুবহু অ্যান্ড্রয়েডের মতো) — ফোনের কার্ডে
             লেখা আছে "Enquiry · Regist. · Treat. · Collected"; ওয়েবে ভুল করে
             "Registration"/"Treatment" পুরো লেখা ছিল। মিলিয়ে দেওয়া হলো। */
          perfTile('Enquiry', perfNum(x.enquiry_count)) +
          perfTile('Regist.', reg) +
          perfTile('Treat.', trt) +
          /* 🟠🔒 V1077 (০৪.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) — TK: *"আজকের
             সারাদিনে কে কতগুলো RMP-র নাম এন্ট্রি করল সেটাও যেন বোঝা যায়,
             বিশেষ করে নতুন নাম"*। ⛔ নতুন গোনা বানানো হয়নি — `hr.staff_performance`-এর
             আগে থেকে থাকা `rmp_added` (ওই সময়ে ওই স্টাফের `createdBy`-তে বসানো
             নতুন `doctor_visits` সারি)। ভিতরের পর্দার "RMP added"-এ ঠিক এই
             সংখ্যাটাই ওঠে, তাই দুই পর্দায় দুরকম উত্তর হবে না। ফোনের সঙ্গেও এক। */
          perfTile('New RMP', perfNum(x.rmp_added), true) +
          perfTile('Collected', m.money(money)) +
        '</div></div>';
    }).join('');
  }

  /* মডেল ৩-এর মতো এক লাইন: বাঁয়ে নাম, ডানে সংখ্যা। */
  function perfRow(label, value, hex) {
    return '<div style="display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-top:1px solid #F0F4F1">' +
      '<span style="color:#3B5A49;font-size:13.5px">' + label + '</span>' +
      '<b style="color:' + (hex || '#123A26') + ';font-size:14.5px">' + value + '</b></div>';
  }

  /* 📱🔒 V813 — একজনের ভার্সন-ট্যাগ। জানা না থাকলে কিছুই ফেরে না (ফাঁকা)। */
  function perfVerChip(code, verMap, latest) {
    if (!latest) return '';
    var c = String(code || '').trim().toUpperCase();
    if (!Object.prototype.hasOwnProperty.call(verMap, c)) return '';
    var v = verMap[c] || 0;
    if (v <= 0) return phvChip('No app yet', '#B3261E', '#FDECEA');
    if (v < latest) return phvChip('V' + v + ' · old', '#B3261E', '#FDECEA');
    return phvChip('V' + v, '#0A5C33', '#E9F7EE');
  }

  /* 🟠 V1077 — `warm` শুধু "New RMP" ঘরের জন্য (হলুদ), ফোনের হুবহু একই রং।
     ডিফল্ট মিথ্যা, তাই পুরনো চারটে ডাক আগের মতোই সবুজ। */
  function perfTile(cap, val, warm) {
    var fill = warm ? '#FFF4E5' : '#F2FBF5', edge = warm ? '#F3D9AE' : '#D8ECDF';
    var capCol = warm ? '#8A5A00' : '#3B5A49', valCol = warm ? '#B45309' : '#0A5C33';
    return '<div style="flex:1;min-width:0;background:' + fill + ';border:1px solid ' + edge +
      ';border-radius:10px;padding:8px 3px;text-align:center">' +
      '<span style="display:block;font-size:10px;color:' + capCol + ';white-space:nowrap">' + cap + '</span>' +
      '<b style="display:block;font-size:13.5px;color:' + valCol + ';margin-top:2px;white-space:nowrap">' + val + '</b></div>';
  }

  /* 🔴 V452 (19.08.2026, TK-অনুমোদিত): Android-এর মতো Web Staff Performance-এও
     count → list, আর Enquiry / Calls / Collection list-row → exact read-only
     detail। Detail খুলতে নতুন Cloud read লাগে না; already-loaded row cache হয়। */
  function perfRowLink(label, value, hex, call) {
    return '<div onclick="' + call + '" style="display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-top:1px solid #F0F4F1;cursor:pointer">' +
      '<span style="color:#3B5A49;font-size:13.5px">' + label + '</span>' +
      '<span><b style="color:' + (hex || '#123A26') + ';font-size:14.5px">' + value + '</b><span style="color:#9AA8B5;margin-left:8px">&rsaquo;</span></span></div>';
  }

  var __perfDrillCtx = null;

  function perfDrillBackCall(ctx) {
    return 'staffPerformanceOne(\'' + window.MOD.esc(ctx.code) + '\',\'' + window.MOD.esc(ctx.month) + '\',' + (ctx.viaList ? 'true' : 'false') + ')';
  }

  function perfWebDrillRender(ctx) {
    var m = window.MOD, rows = ctx.rows || [];
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf">' +
      '<div class="topbar"><b>' + m.esc(ctx.title) + '</b></div><div class="page">' +
      '<div style="font-size:12px;color:#5B6B81;margin:0 2px 10px">' + m.esc(perfLabel(ctx.month)) + '</div>' +
      '<div id="perfDrillRows"></div>' +
      '<div class="actions" style="margin-top:12px"><button class="ghost" onclick="' + perfDrillBackCall(ctx) + '">Back</button></div>' +
      '</div></div>';
    var host = document.getElementById('perfDrillRows'); if (!host) return;
    if (!rows.length) { host.innerHTML = '<div class="card mut">' + m.esc(ctx.emptyMsg || 'No records this period.') + '</div>'; return; }
    host.innerHTML = '<div class="card">' + rows.map(function (r, i) {
      var main = '', sub = '', third = '', clickable = false;
      if (ctx.kind === 'enquiry') {
        main = r.name || 'Unknown'; sub = (r.enq_date || '') + ' · ' + (r.mobile || '') + ' · ' + (r.branch || ''); third = r.disease || ''; clickable = true;
      } else if (ctx.kind === 'registration' || ctx.kind === 'treatment') {
        main = r.name || 'Unknown'; sub = (r.reg_date || '') + ' · ' + (r.mobile || '') + ' · ' + (r.branch || ''); third = r.patient_id || '';
      } else if (ctx.kind === 'rmp') {
        main = r.name || 'Unknown'; sub = (String(r.added_date || '').slice(0,10)) + ' · ' + (r.mobile || '') + ' · ' + (r.area || '');
      } else if (ctx.kind === 'calls_app' || ctx.kind === 'calls_outside') {
        main = r.target || '—'; sub = (r.call_date || '') + ' · ' + (r.call_time || ''); third = r.remark || ''; clickable = true;
      } else if (ctx.kind === 'cash' || ctx.kind === 'online') {
        main = (r.name || 'Unknown') + ' — ' + m.money(Number(r.amount || 0)); sub = (r.pay_date || '') + ' · ' + (r.mobile || '') + ' · ' + (r.branch || ''); third = r.pay_label || r.remarks || ''; clickable = true;
      } else if (ctx.kind === 'reports') {
        main = r.report_date || ''; sub = (r.status || 'sent') + (r.accepted ? ' · seen' : '');
      } else if (ctx.kind === 'attendance') {
        /* 🔴🔒 V1162 (০৭.০৯.২০২৬, TK-নির্দেশ · ফোনের হুবহু যমজ) — তারিখ ও সময়
           এখানে **কাঁচা** বসত (`2026-09-07` · `09:15:00`)। এখন প্রকল্পের এক
           চেহারায়: `07/09/2026` ও `9.15 AM`। ⛔ জমা লেখায় হাত পড়েনি। */
        main = salDmy(r.work_date || ''); sub = 'IN ' + (salAttTime12(r.check_in) || '—') + ' · OUT ' + (salAttTime12(r.check_out) || '—') + (r.is_leave ? ' · Leave' : '');
      }
      return '<div ' + (clickable ? 'onclick="perfWebDrillDetail(' + i + ')" ' : '') + 'style="padding:11px 2px;' + (i ? 'border-top:1px solid #E3ECE7;' : '') + (clickable ? 'cursor:pointer;' : '') + '">' +
        '<div style="display:flex;justify-content:space-between;gap:8px"><b style="color:#123A26;font-size:14px">' + m.esc(main) + '</b>' + (clickable ? '<span style="color:#9AA8B5">&rsaquo;</span>' : '') + '</div>' +
        (sub ? '<div style="font-size:12.5px;color:#5B6B81;margin-top:3px">' + m.esc(sub) + '</div>' : '') +
        (third ? '<div style="font-size:12.5px;color:#5B6B81;margin-top:3px">' + m.esc(third) + '</div>' : '') + '</div>';
    }).join('') + '</div>';
  }

  async function perfWebDrillList(kind, code, month, viaList) {
    var m = window.MOD, client = await sb(), fn = '', params = { p_month: month, p_code: code }, title = '', emptyMsg = 'No records this period.';
    if (kind === 'enquiry') { fn = 'perf_enquiry_list_v2'; title = 'Enquiry Forms'; emptyMsg = 'No enquiry forms this period.'; }
    else if (kind === 'registration') { fn = 'perf_registration_list'; title = 'Registrations'; }
    else if (kind === 'treatment') { fn = 'perf_treatment_list'; title = 'Started Treatment'; }
    else if (kind === 'rmp') { fn = 'perf_rmp_list'; title = 'RMP Added'; }
    else if (kind === 'calls_app') { fn = 'perf_calls_list_v2'; params.p_kind = 'app'; title = 'Calls From App'; emptyMsg = 'No calls this period.'; }
    else if (kind === 'calls_outside') { fn = 'perf_calls_list_v2'; params.p_kind = 'outside'; title = 'Outside Calls'; emptyMsg = 'No calls this period.'; }
    else if (kind === 'cash') { fn = 'perf_payment_list_v2'; params.p_mode = 'cash'; title = 'Cash Collection'; emptyMsg = 'No payments this period.'; }
    else if (kind === 'online') { fn = 'perf_payment_list_v2'; params.p_mode = 'online'; title = 'Online Collection'; emptyMsg = 'No payments this period.'; }
    else if (kind === 'reports') { fn = 'perf_reports_list'; title = 'Daily Reports Sent'; emptyMsg = 'No reports sent this period.'; }
    else if (kind === 'attendance') { fn = 'perf_attendance_sheet'; title = 'Attendance Sheet'; emptyMsg = 'No attendance this period.'; }
    else return;

    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>' + m.esc(title) + '</b></div><div class="page"><div class="card mut">Loading...</div></div></div>';
    var rr;
    try { rr = await client.schema('hr').rpc(fn, params); } catch (e) { rr = { error: e }; }
    if (rr && rr.error) {
      document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>' + m.esc(title) + '</b></div><div class="page"><div class="card mut">Could not load. Please try again.</div><div class="actions"><button class="ghost" onclick="staffPerformanceOne(\'' + m.esc(code) + '\',\'' + m.esc(month) + '\',' + (viaList ? 'true' : 'false') + ')">Back</button></div></div></div>'; return;
    }
    __perfDrillCtx = { kind: kind, code: code, month: month, viaList: !!viaList, title: title, emptyMsg: emptyMsg, rows: (rr && Array.isArray(rr.data)) ? rr.data : [] };
    perfWebDrillRender(__perfDrillCtx);
  }

  function perfWebDrillDetail(index) {
    var m = window.MOD, ctx = __perfDrillCtx, r = ctx && ctx.rows ? ctx.rows[index] : null; if (!ctx || !r) return;
    var title = 'Detail', fields = [];
    function add(label, value) { if (value !== undefined && value !== null && String(value).trim() !== '') fields.push([label, String(value)]); }
    if (ctx.kind === 'enquiry') {
      title = 'Enquiry Detail'; add('Name', r.name || 'Unknown'); add('Mobile', r.mobile); add('Date', r.enq_date); add('Branch', r.branch); add('Disease', r.disease); add('Address', r.address); add('Remarks', r.remarks); add('Status', r.status); add('Stage', r.stage); add('Received By', r.received_by); add('Created By', r.created_by); add('Created At', r.created_at); add('Record ID', r.id);
    } else if (ctx.kind === 'calls_app' || ctx.kind === 'calls_outside') {
      title = ctx.kind === 'calls_app' ? 'App Call Detail' : 'Outside Call Detail'; add('Number', r.target || '—'); add('Date', r.call_date); add('Time', r.call_time); add('Call Type', ctx.kind === 'calls_app' ? 'Call from app' : 'Outside call'); add('Remark', r.remark); if (ctx.kind === 'calls_app' && !r.full_number_available) add('Note', 'This is an older call. The full number was not stored at that time.'); add('Record ID', r.id);
    } else if (ctx.kind === 'cash' || ctx.kind === 'online') {
      title = ctx.kind === 'cash' ? 'Cash Collection Detail' : 'Online Collection Detail'; add('Patient', r.name || 'Unknown'); add('Mobile', r.mobile); add('Amount', m.money(Number(r.amount || 0))); add('Date', r.pay_date); add('Mode', r.mode || (ctx.kind === 'cash' ? 'Cash' : 'Online')); add('Payment', r.pay_label); add('Payment Type', r.pay_type); add('Branch', r.branch); add('Remarks', r.remarks); add('Patient ID', r.patient_id); add('Patient Code', r.patient_code); add('Received By', r.received_by); add('Created By', r.created_by); add('Created At', r.created_at); add('Status', r.status); add('Record ID', r.id);
    } else return;
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf"><div class="topbar"><b>' + m.esc(title) + '</b></div><div class="page">' +
      '<div style="font-size:12px;color:#5B6B81;margin:0 2px 10px">' + m.esc(perfLabel(ctx.month)) + '</div>' +
      '<div class="card">' + fields.map(function (f, i) { return '<div style="padding:10px 2px;' + (i ? 'border-top:1px solid #E3ECE7;' : '') + '"><div style="font-size:11.5px;font-weight:700;color:#5B6B81">' + m.esc(f[0]) + '</div><div style="font-size:14px;color:#123A26;margin-top:3px;overflow-wrap:anywhere">' + m.esc(f[1]) + '</div></div>'; }).join('') + '</div>' +
      '<div class="actions" style="margin-top:12px"><button class="ghost" onclick="perfWebDrillCached()">Back</button></div></div></div>';
  }

  function perfWebDrillCached() { if (__perfDrillCtx) perfWebDrillRender(__perfDrillCtx); }

  /* 🗓️🔴 V509 (২১.০৮.২০২৬, TK-নির্দেশ — কাগজের হাজিরা-খাতার ছবিসহ):
   *   *"staff এর এখানে attendance sheet এরকম থাকবে, যাতে সে দেখতে পারে সারা
   *    মাসে কোন সময় এসেছে এবং কোন সময় ক্লিনিক থেকে গেছে, কবে সে ছুটি নিয়েছিল।"*
   *
   * ⛔ নতুন কিছু বানানো হয়নি — হাজিরা-খাতার পর্দাটা (`perfWebDrillList`-এর
   *    'attendance') আগে থেকেই তৈরি ছিল, শুধু **মাস্টারের** Staff Performance
   *    পথ থেকে খুলত (`staffPerformanceOne`-এ "Only Master")। স্টাফের নিজের
   *    পাতায় ঢোকার দরজাই ছিল না। এখানে শুধু সেই দরজাটা বসানো হলো —
   *    ফোনের অ্যাপে হুবহু একই ("My Attendance Sheet")।
   * ⛔ স্টাফ **শুধু নিজের** খাতা দেখেন: নিজের কোড ছাড়া কিছু পাঠানোই হয় না, আর
   *    সার্ভারের নিয়মও (V509_MY_ATTENDANCE_SHEET SQL) নিজের কোড ছাড়া অন্য কারও
   *    সারি ফেরত দেয় না। মাস্টারের ক্ষমতা অপরিবর্তিত।
   * ⚠️ ভিতরের সাধারণ "Back" মাস্টারের পাতায় ফেরে — স্টাফের জন্য সেটা "Only
   *    Master" দেখাত। তাই এখানে নিজের `myProfile()`-এ ফেরার আলাদা Back বসানো হয়।
   * ⚡ Egress: চাপ দিলে তবেই একটাই ছোট RPC (সর্বোচ্চ ৩১ সারি, ৪টে ঘর)।
   */
  /* ═══════════════════════════════════════════════════════════════════
     🗓️🔒 V1199 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **ATTENDANCE SHEET**
     (কত তারিখে কত ঘণ্টা), ফোনের `attendanceSheet()`-এর হুবহু যমজ।
     ⛔ ঘণ্টার নিয়ম হুবহু ফোনের `HourSalary`-রই: ছুটি ও Work From Home = ৭ ঘণ্টা;
        IN/OUT-এর একটা না থাকলে ০; মাসের ঘণ্টা = ওই মাসের দিন × ৭।
     ⛔ পুরনো `myAttendanceSheet()` (RPC-ভিত্তিক) ছোঁয়া হয়নি — এটা নতুন পর্দা।
     ═══════════════════════════════════════════════════════════════════ */
  var ATT_ROWS = [], ATT_INFO = {}, ATT_PERF = null;   /* 📊 V1204 */
  function attT12(raw){
    var t = String(raw || '').trim();
    if (t.length < 4) return '—';
    try{
      var hh = parseInt(t.slice(0,2),10), mm = t.slice(3,5);
      var ap = hh >= 12 ? 'PM' : 'AM', h12 = (hh===0) ? 12 : (hh>12 ? hh-12 : hh);
      return h12 + '.' + mm + ' ' + ap;
    }catch(e){ return t; }
  }
  function attMins(raw){
    var t = String(raw || '').trim();
    if (t.length < 4) return null;
    try{
      var h = parseInt(t.slice(0,2),10), mi = parseInt(t.slice(3,5),10);
      if (isNaN(h) || isNaN(mi)) return null;
      return h*60 + mi;
    }catch(e){ return null; }
  }
  function attHours(mins){ return Math.floor(mins/60) + 'h ' + String(mins%60).padStart(2,'0') + 'm'; }
  function attDmy(iso){ try{ var p=String(iso).slice(0,10).split('-'); return p[2]+'/'+p[1]+'/'+p[0]; }catch(e){ return iso||''; } }
  function attDaysInMonth(ym){ try{ var p=ym.split('-'); return new Date(Number(p[0]), Number(p[1]), 0).getDate(); }catch(e){ return 30; } }

  async function attendanceSheet(code, ym){
    var m = window.MOD, client = await sb();
    if (!ym) { var d=new Date(); ym = d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0'); }
    var from = ym + '-01';
    var p = ym.split('-'), y = Number(p[0]), mo = Number(p[1]);
    var end = (mo >= 12) ? ((y+1) + '-01-01') : (y + '-' + String(mo+1).padStart(2,'0') + '-01');
    var days = [], cfg = {}, prof = {};
    try{
      days = ((await client.schema('wn').from('notebook_days')
        .select('work_date,check_in,check_out,is_leave,leave_reason,is_wfh,is_other_branch,branch,outside_calls_manual')   /* 📊 V1204 */
        .eq('staff_code', code).gte('work_date', from).lt('work_date', end)
        .order('work_date', {ascending:true})).data) || [];
    }catch(e){}
    try{ cfg = ((await client.schema('hr').from('salary_config').select('salary_amount').eq('person_code',code).maybeSingle()).data) || {}; }catch(e){}
    try{ prof = ((await client.schema('hr').from('staff_profiles').select('full_name,branch,link_mobile,address').eq('person_code',code).maybeSingle()).data) || {}; }catch(e){}

    var home = String(prof.branch||'').trim().toLowerCase();
    var DAYM = 7*60, worked = 0, rows = [];
    days.forEach(function(d){
      var iso = String(d.work_date||'').slice(0,10), tag='', kind='', mins=0, outMissing=false;
      if (d.is_leave){ mins = DAYM; tag = 'LEAVE' + (d.leave_reason ? (' ('+d.leave_reason+')') : ''); kind='lv'; }
      else if (d.is_wfh){ mins = DAYM; tag = 'WORK FROM HOME'; kind='wf'; }
      /* 🚌 V1200 — আগে থেকে জানানো "অন্য ব্রাঞ্চে ডিউটি" ⇒ ৭ ঘণ্টা (ফোনের নিয়মই)। */
      else if (d.is_other_branch){ mins = DAYM; tag = 'DUTY · ' + String(d.branch||'').trim().toUpperCase(); kind='br'; }
      else {
        var a = attMins(d.check_in), b = attMins(d.check_out);
        /* ⏰ V1200 (TK-সিদ্ধান্ত) — IN আছে অথচ OUT নেই ⇒ ৭ ঘণ্টা (আগে ০ ছিল)।
           🐞🔒 V1455 (১৪.০৯.২০২৬, TK-রিপোর্ট — "আজকের তারিখে এখনও staff চেম্বারে
           আছে, Missing 7hr এটা তো ঠিক না") — TK-র কথা ছিল "আউট টাইম চাপতে
           **ভুলে যায়**" (দিন শেষ হওয়ার পরে) — আজকের এখনো-চলতে-থাকা দিনের জন্য
           নয়। আজকের তারিখে OUT না থাকলে এখন পর্যন্ত IN থেকে সত্যিকারের সময়ই
           দেখায় (লাল MISSING/আন্দাজি ৭ ঘণ্টা নয়); আগের দিনগুলোয় নিয়ম অটুট। */
        var todayIsoAtt = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Kolkata' });
        if (a!==null && b===null && iso === todayIsoAtt){
          var nowMinAtt = (function(){ var n = new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Kolkata'})); return n.getHours()*60+n.getMinutes(); })();
          mins = nowMinAtt > a ? (nowMinAtt - a) : 0;
        }
        else if (a!==null && b===null){ mins = DAYM; outMissing = true; }
        else if (a===null || b===null || b<=a){ mins = 0; }
        else mins = b - a;
        var br = String(d.branch||'').trim();
        if (!tag && br && home && br.toLowerCase() !== home){ tag = br.toUpperCase(); kind='br'; }
      }
      worked += mins;
      var plain = (d.is_leave || d.is_wfh);
      rows.push({ iso:iso, date:attDmy(iso),
        inTime: plain ? '—' : attT12(d.check_in),
        outTime: plain ? '—' : attT12(d.check_out),
        outMissing: outMissing, minutes: mins, hours: attHours(mins), tag: tag, kind: kind });
    });
    var amount = Number(cfg.salary_amount||0);
    var monthHours = attDaysInMonth(ym) * 7;
    var rate = monthHours > 0 ? amount/monthHours : 0;
    var payable = (worked/60) * rate;
    /* 📊🔒 V1204 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — মাসের পারফরম্যান্স।
       ⛔ গোনার নিয়ম হুবহু notebook.js-এর মাসিক হিসাবেরই (শেষ ১০ অঙ্ক ধরে মেলানো),
         তাই স্টাফের পর্দা ও এই কাগজ কখনো আলাদা হবে না।
       ⛔ পড়া ব্যর্থ হলে "…" — মিথ্যা ০ নয়। তিনটে ছোট count-কল, সারি টানা হয় না। */
    var perf = { enq:'…', reg:'…', app:'…', out:'0', tot:'…', lv:'0' };
    try {
      var mob10 = String(prof.link_mobile||'').replace(/\D/g,'').slice(-10);
      async function cnt(tbl, col1, col2){
        try{
          var q = client.from(tbl).select('id', { count:'exact', head:true })
            .or(col1+'.like.%'+mob10+','+col2+'.like.%'+mob10)
            .or('createdAt.gte.'+from+',date.gte.'+from+',registrationDate.gte.'+from);
          var r = await q;
          if (r && r.error) return '…';
          return String(r.count == null ? '…' : r.count);
        }catch(e){ return '…'; }
      }
      if (mob10.length === 10) {
        perf.enq = await cnt('enquiries','createdBy','receivedBy');
        perf.reg = await cnt('patients','registeredBy','createdBy');
      }
      try{
        var ct = await client.schema('wn').from('call_taps').select('id', { count:'exact', head:true })
          .eq('staff_code', code).gte('call_date', from).lt('call_date', end);
        perf.app = (ct && !ct.error && ct.count != null) ? String(ct.count) : '…';
      }catch(e){}
      var oSum = 0, lvN = 0;
      days.forEach(function(d){ oSum += Number(d.outside_calls_manual||0); if (d.is_leave) lvN++; });
      perf.out = String(oSum); perf.lv = String(lvN);
      perf.tot = (perf.app === '…') ? '…' : String(Number(perf.app) + oSum);
    } catch(e) {}
    ATT_PERF = perf;
    ATT_ROWS = rows;
    ATT_INFO = { code:code, ym:ym, monthLabel:salMonthLabel(ym), name:String(prof.full_name||code),
      branch:String(prof.branch||''), mobile:String(prof.link_mobile||''), address:String(prof.address||''),
      worked:worked, monthHours:monthHours, amount:amount, rate:rate, payable:payable };

    function cell(t, cls){ return '<td'+(cls?(' class="'+cls+'"'):'')+'>'+t+'</td>'; }
    var body = rows.map(function(r,i){
      var out = r.outMissing ? '<td class="attMs">MISSING</td>' : cell(m.esc(r.outTime));
      var hcell = '<td class="att-'+(r.kind||'x')+'">'+m.esc(r.hours)+(r.tag?('<small> · '+m.esc(r.tag)+'</small>'):'')+'</td>';
      var click = (m.isMasterModule()) ? (' onclick="attEditDay('+i+')" style="cursor:pointer"') : '';
      return '<tr'+click+'>'+cell(m.esc(r.date),'attD')+cell(m.esc(r.inTime))+out+hcell+'</tr>';
    }).join('') || '<tr><td colspan="4" class="mut">No attendance in this month.</td></tr>';

    /* 📊 V1204 — কাগজে যে ছয়টা ঘর, পর্দাতেও হুবহু সেগুলোই (এক নিয়ম, এক সংখ্যা)।
       সংখ্যা ০-র বেশি হলে গাঢ়, ০ হলে ফিকে (TK-নির্দেশ)। */
    function pfCell(lbl, v){
      var bright = (Number(v) || 0) > 0;
      return '<div style="flex:1;padding:8px 10px"><div class="tiny mut" style="font-weight:800">'+lbl+'</div>'
        + '<div style="font-size:17px;font-weight:'+(bright?'800':'600')+';margin-top:2px;color:'
        + (bright ? '#0B2B59' : '#B9C0C8') + '">'+m.esc(String(v||'-'))+'</div></div>';
    }
    var html = '<div class="card" style="padding:6px 4px"><div class="tiny mut" style="font-weight:800;padding:4px 10px;color:#0B4F2A">MONTHLY PERFORMANCE</div>'
      + '<div style="display:flex">'+pfCell('NEW ENQUIRY',perf.enq)+pfCell('REGISTRATION',perf.reg)+pfCell('APP CALLS',perf.app)+'</div>'
      + '<div style="display:flex">'+pfCell('OUTSIDE CALLS',perf.out)+pfCell('TOTAL CALLS',perf.tot)+pfCell('LEAVE DAYS',perf.lv)+'</div></div>'
      + '<div class="card" style="display:flex;padding:0;overflow:hidden">'
      + '<div style="flex:1;padding:12px 14px"><div class="tiny mut" style="font-weight:800">WORKED</div>'
      +   '<div style="font-size:19px;font-weight:800;color:#0A7C3F;margin-top:3px">'+attHours(worked)+'</div></div>'
      + '<div style="width:1px;background:#EDF2EF;margin:10px 0"></div>'
      + '<div style="flex:1;padding:12px 14px"><div class="tiny mut" style="font-weight:800">MONTH HOURS</div>'
      +   '<div style="font-size:19px;font-weight:800;margin-top:3px">'+monthHours+'h</div></div>'
      + '<div style="width:1px;background:#EDF2EF;margin:10px 0"></div>'
      + '<div style="flex:1;padding:12px 14px"><div class="tiny mut" style="font-weight:800">SALARY</div>'
      +   '<div style="font-size:19px;font-weight:800;color:#0E6E8C;margin-top:3px">'+m.money(payable)+'</div></div></div>'
      + '<div class="card" style="padding:8px"><table class="attTbl">'
      + '<thead><tr><th>DATE</th><th>IN TIME</th><th>OUT TIME</th><th>HOURS</th></tr></thead>'
      + '<tbody>'+body+'</tbody></table></div>'
      + '<div class="actions"><button onclick="attPrint()">Print / PDF</button>'
      + '<button class="ghost" onclick="attWhatsApp()">WhatsApp</button>'
      + '<button class="ghost" onclick="attPickMonth(\''+m.esc(code)+'\')">Change month</button></div>';

    page('Performance Sheet · ' + m.esc(code) + ' · ' + salMonthLabel(ym), html, true);   /* 📊 V1204 */
  }
  window.attendanceSheet = attendanceSheet;

  function attPickMonth(code){
    var m = window.MOD, out = [], d = new Date();
    for (var i=0;i<12;i++){
      var ym = d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
      out.push('<button class="menuBtn" onclick="closeModal();attendanceSheet(\''+m.esc(code)+'\',\''+ym+'\')"><b>'+salMonthLabel(ym)+'</b></button>');
      d.setMonth(d.getMonth()-1);
    }
    try{ modal('<h2>Choose month</h2><div class="grid menuGrid">'+out.join('')+'</div>'); }catch(e){}
  }
  window.attPickMonth = attPickMonth;

  /* ✏️ V1199 — মাস্টার ওই দিনের IN / OUT বদলান; ঘণ্টা নিজে থেকেই ঠিক হয়।
     ⛔ ফোনের মতোই `wn.notebook_days`-এ (staff_code, work_date) upsert। */
  async function attEditDay(i){
    var m = window.MOD;
    if (!m.isMasterModule()) return;
    var r = ATT_ROWS[i]; if (!r) return;
    function ask(label, cur){
      var v = prompt(label + ' (hh:mm AM/PM) — blank = no change', (cur==='—'?'':cur));
      return v;
    }
    var vi = ask('IN TIME', r.inTime); if (vi === null) return;
    var vo = ask('OUT TIME', r.outTime); if (vo === null) return;
    function to24(v){
      var t = String(v||'').trim().toUpperCase(); if (!t) return null;
      try{
        var pm = t.indexOf('PM') >= 0;
        var core = t.replace('AM','').replace('PM','').trim().replace('.',':');
        var pp = core.split(':'); var h = parseInt(pp[0],10), mi = parseInt(pp[1],10);
        if (isNaN(h) || isNaN(mi)) return null;
        if (pm && h < 12) h += 12;
        if (!pm && h === 12) h = 0;
        return String(h).padStart(2,'0')+':'+String(mi).padStart(2,'0')+':00';
      }catch(e){ return null; }
    }
    var in24 = to24(vi), out24 = to24(vo);
    if (!in24 && !out24) return;
    try{
      var client = await sb();
      var row = { staff_code: ATT_INFO.code, work_date: r.iso, updated_at: new Date().toISOString() };
      if (in24) row.check_in = in24;
      if (out24) row.check_out = out24;
      var up = await client.schema('wn').from('notebook_days').upsert(row, { onConflict: 'staff_code,work_date' });
      if (up && up.error) { try{ toast('Could not save — try again'); }catch(e){} return; }
      try{ toast('Saved'); }catch(e){}
      attendanceSheet(ATT_INFO.code, ATT_INFO.ym);
    }catch(e){ try{ toast('Could not save — try again'); }catch(_e){} }
  }
  window.attEditDay = attEditDay;

  /* 📊 V1204 — কাগজের "MONTHLY PERFORMANCE" বাক্স। না পড়া গেলে বাক্সটাই বসে না,
     তাই কাগজ আগের মতোই ছাপা হয় (কিছু ভাঙে না)। */
  function attPerfBlockHtml(){
    var m = window.MOD, P = ATT_PERF, I = ATT_INFO;
    if (!P) return '';
    function c(l, v){ return '<td><span class="pl">'+l+'</span>'+m.esc(String(v||'-'))+'</td>'; }
    return '<div class="blk"><div class="h">MONTHLY PERFORMANCE &middot; '+m.esc(String(I.monthLabel).toUpperCase())+'</div>'
      + '<table class="perf"><tr>'+c('NEW ENQUIRY',P.enq)+c('REGISTRATION',P.reg)+c('APP CALLS',P.app)
      + c('OUTSIDE CALLS',P.out)+c('TOTAL CALLS',P.tot)+c('LEAVE DAYS',P.lv)+'</tr></table></div>';
  }

  /** A4 এক পাতার কাগজ — ফোনের `AttendanceSheetHtmlPrint`-এর হুবহু একই সাজ। */
  function attSheetHtml(){
    var m = window.MOD, I = ATT_INFO;
    var body = ATT_ROWS.map(function(r){
      var out = r.outMissing ? '<td class="ms">MISSING</td>' : '<td>'+m.esc(r.outTime)+'</td>';
      return '<tr><td class="d">'+m.esc(r.date)+'</td><td>'+m.esc(r.inTime)+'</td>'+out
        + '<td class="'+(r.kind||'')+'">'+m.esc(r.hours)+(r.tag?('<small> · '+m.esc(r.tag)+'</small>'):'')+'</td></tr>';
    }).join('') || '<tr><td colspan="4">No attendance in this month.</td></tr>';
    return '<!doctype html><html><head><meta charset="utf-8"><title>Performance Sheet</title><style>'
      + '@page{size:A4;margin:8mm} body{font-family:sans-serif;color:#1C2A33;margin:0}'
      + '.tb{background:#0f5132;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:6px 10px}'
      + '.tb .t{font-size:12px;font-weight:800;letter-spacing:2px}.tb .r{font-size:9px;color:#cfe6d8;text-align:right}'
      + '.cn{font-size:16px;font-weight:800;color:#0f5132;padding:8px 10px 2px}'
      + '.addr{font-size:9px;color:#3b4650;padding:0 10px 6px}'
      + '.pi{display:flex;gap:16px;padding:6px 10px 4px;font-size:9.5px}.pi .c{flex:1}.pi b{color:#0f5132}'
      + 'table{width:100%;border-collapse:collapse;font-size:9.5px}'
      + 'th{background:#0B4F2A;color:#fff;padding:4px 6px;font-size:8.5px;border:1px solid #0B4F2A}'
      + 'td{padding:2.5px 6px;border:1px solid #D6DEE6;text-align:center}'
      + 'td.d{text-align:left;font-weight:700} td small{font-size:8px;font-weight:700}'
      + '.lv small{color:#123E8C}.wf small{color:#8A5A00}.br small{color:#0A7C3F}.ms{color:#C62828;font-weight:700}'
      + 'tfoot td{border-top:2px solid #0B4F2A;font-weight:800;background:#F4F9F6}'
      + '.calc{margin-top:7px;border:1px solid #D6DEE6;border-radius:5px;overflow:hidden}'
      + '.calc .h{background:#0B4F2A;color:#fff;padding:4px 9px;font-size:8.5px;font-weight:800;letter-spacing:1px}'
      /* 📊 V1204 — পারফরম্যান্সের বাক্স (ফোনের কাগজের হুবহু যমজ) */
      + '.blk{margin:0 0 7px;border:1px solid #D6DEE6;border-radius:5px;overflow:hidden}'
      + '.blk .h{background:#0B4F2A;color:#fff;padding:4px 9px;font-size:8.5px;font-weight:800;letter-spacing:1px}'
      + '.perf td{text-align:center;font-weight:800;font-size:13px;color:#0B2B59;padding:5px 4px}'
      + '.perf td .pl{display:block;font-size:8px;font-weight:700;color:#6B7280;letter-spacing:.6px}'
      + '.wrap{padding:2px 10px 0}'
      + '</style></head><body>'
      + '<div class="cn">' + m.esc(I.branch ? (I.branch.toUpperCase()==='KISHANGANJ'?'TK BISWAS PILES CLINIC':'MAA AYURVED PILES CLINIC') : 'MAA AYURVED PILES CLINIC') + '</div>'
      + '<div class="addr">' + m.esc(I.branch) + '</div>'
      + '<div class="tb"><span class="t">STAFF PERFORMANCE SHEET</span><span class="r">'+m.esc(I.monthLabel)+'</span></div>'
      + '<div class="pi"><div class="c"><div><b>Staff Name</b> : '+m.esc(I.name)+'</div>'
      +   '<div><b>Staff Code</b> : '+m.esc(I.code)+'</div><div><b>Branch</b> : '+m.esc(I.branch)+'</div></div>'
      + '<div class="c"><div><b>Mobile</b> : '+m.esc(I.mobile||'-')+'</div>'
      +   '<div><b>Address</b> : '+m.esc(I.address||'-')+'</div></div></div>'
      + '<div class="wrap">' + attPerfBlockHtml() + '<table><thead><tr><th>DATE</th><th>IN TIME</th><th>OUT TIME</th><th>HOURS</th></tr></thead>'
      + '<tbody>'+body+'</tbody>'
      + '<tfoot><tr><td colspan="3">TOTAL HOURS WORKED</td><td>'+attHours(I.worked)+'</td></tr></tfoot></table>'
      + '<div class="calc"><div class="h">SALARY CALCULATION · '+m.esc(String(I.monthLabel).toUpperCase())+'</div><table>'
      + '<tr><td class="d">Month hours</td><td>'+I.monthHours+'h 00m</td></tr>'
      + '<tr><td class="d">Monthly salary (set)</td><td>'+m.money(I.amount)+'</td></tr>'
      + '<tr><td class="d">Rate per hour</td><td>'+m.money(I.rate)+'</td></tr>'
      + '<tr><td class="d">Hours worked</td><td>'+attHours(I.worked)+'</td></tr>'
      + '<tr style="background:#F4F9F6"><td class="d"><b>SALARY FOR THIS MONTH</b></td>'
      +   '<td style="font-weight:800;color:#0F5132">'+m.money(I.payable)+'</td></tr></table></div></div>'
      + '</body></html>';
  }
  function attPrint(){
    try{
      var w = window.open('', '_blank');
      if (!w) { toast('Allow pop-ups to print'); return; }
      w.document.write(attSheetHtml()); w.document.close();
      setTimeout(function(){ try{ w.focus(); w.print(); }catch(e){} }, 400);
    }catch(e){ try{ toast('Could not open print'); }catch(_e){} }
  }
  window.attPrint = attPrint;

  function attWhatsApp(){
    var I = ATT_INFO;
    var lines = ['*STAFF PERFORMANCE SHEET*', I.name + '  ·  ' + I.code, I.branch + '  ·  ' + I.monthLabel,
      '--------------------------------'];
    if (ATT_PERF) {   /* 📊 V1204 — কাগজ ও WhatsApp একই সংখ্যা */
      lines.push('New Enquiry: ' + ATT_PERF.enq + '  |  Registration: ' + ATT_PERF.reg);
      lines.push('App Calls: ' + ATT_PERF.app + '  |  Outside Calls: ' + ATT_PERF.out + '  |  Total: ' + ATT_PERF.tot);
      lines.push('Leave Days: ' + ATT_PERF.lv);
      lines.push('--------------------------------');
    }
    ATT_ROWS.forEach(function(r){
      lines.push(String(r.date).slice(0,5) + '  '
        + (r.outMissing ? (r.inTime + ' → MISSING') : (r.inTime + ' → ' + r.outTime))
        + '  ' + r.hours + (r.tag ? ('  (' + r.tag + ')') : ''));
    });
    lines.push('--------------------------------');
    lines.push('*Total hours* : ' + attHours(I.worked));
    lines.push('*Salary this month* : ' + window.MOD.money(I.payable));
    try{ window.open('https://wa.me/?text=' + encodeURIComponent(lines.join('\n')), '_blank'); }
    catch(e){ try{ toast('Could not open WhatsApp'); }catch(_e){} }
  }
  window.attWhatsApp = attWhatsApp;

  async function myAttendanceSheet() {
    var m = window.MOD;
    var code = (m.session() || {}).code || '';
    if (!code) return pfToast('Not signed in');
    var d = new Date();
    var ym = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
    await perfWebDrillList('attendance', code, ym, false);
    try {
      var page = document.querySelector('.anModPf .page');
      if (page) {
        var bar = document.createElement('div');
        bar.className = 'actions';
        bar.style.marginTop = '12px';
        bar.innerHTML = '<button class="ghost" onclick="myProfile()">Back to My Profile</button>';
        page.appendChild(bar);
      }
    } catch (_e) {}
  }

  /* ---- ২) একজনের পুরো হিসাব ---- */
  async function staffPerformanceOne(code, month, fromList) {
    var m = window.MOD;
    if (!m.isMasterModule()) return pfToast('Only Master');
    /* 🔧 V421খ: ডিফল্ট **আজকের দিন**। */
    var ym = month || m.todayIST();
    /* 🔧 V421 (TK-রিপোর্ট: "staff এর Daily Performance কেন দেখা যাচ্ছে না") —
       Day/Month বোতাম দুটো আগে **শুধু তালিকার** পর্দায় ছিল। Staff Profiles থেকে
       সরাসরি কারও Performance-এ ঢুকলে দিনের হিসাব দেখার উপায়ই ছিল না।
       এখন এই পর্দাতেও একই দুটো বোতাম। ⛔ গোনার নিয়ম কিছুই বদলায়নি।
       ⛔ Back কোথায় ফিরবে সেটাও ঠিক রাখা হলো — তালিকা থেকে এলে তালিকায়,
          Staff Profiles থেকে এলে Staff Profiles-এ। */
    var viaList = (fromList === true) || (fromList === undefined && !!month);
    var backCall = viaList ? ('staffPerformance(\'' + m.esc(ym) + '\')') : 'staffProfiles()';
    /* 🔧 V422খ: Back একদম নিচে (উপরের নোট দেখুন)। */
    document.getElementById('app').innerHTML = '<div class="wrap anMod anModPf">' +
      '<div class="topbar"><b>' + m.esc(code) + '</b>' + perfChips(ym, 'one', code, viaList) + '</div>' +
      '<div class="page"><div id="perf1" class="card mut">Loading...</div>' +
      '<div class="actions" style="margin-top:12px"><button class="ghost" onclick="' + backCall + '">Back</button></div>' +
      '</div></div>';
    var rows = await perfRows(ym);
    var box = document.getElementById('perf1'); if (!box) return;
    if (rows === null) { box.textContent = 'Could not load. Please try again.'; return; }
    var x = (rows || []).filter(function (r) { return String(r.person_code) === String(code); })[0];
    if (!x) { box.textContent = 'No record for this month.'; return; }
    box.className = '';
    function sec(title, hex, lines) {
      return '<div class="card"><div style="font-weight:800;color:' + hex + ';font-size:16px;padding-bottom:4px">' + title + '</div>' + lines + '</div>';
    }
    box.outerHTML =
      /* 🔴 V437 #20 (নিজের অডিটে ধরা) — ফোনে (`StaffProfileActivity.kt:1218`)
         `full_name` ফাঁকা হলে কোডটা দেখানো হয় (`ifBlank { code }`); ওয়েবে
         এখানে কোনো বিকল্প ছিল না, তাই শিরোনামটা **ফাঁকা** পড়ে থাকত।
         (তালিকার কার্ডে বিকল্পটা আগেই ছিল, শুধু এই পর্দাটা বাদ পড়েছিল।) */
      '<div class="card"><b style="color:#0A5C33;font-size:17px">' + m.esc(x.full_name || x.person_code || '') + '</b>' +
        '<div style="font-size:12px;color:#3B5A49;margin-top:2px">' + m.esc(x.person_code) + ' · ' + m.esc(x.branch) + ' · ' + m.esc(perfLabel(ym)) + '</div></div>' +
      sec('Patient Work', '#0A5C33',
        perfRowLink('Enquiry forms', perfNum(x.enquiry_count), '#123A26', "perfWebDrillList('enquiry','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRowLink('Registrations', perfNum(x.registration_count), '#123A26', "perfWebDrillList('registration','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRowLink('Started treatment', perfNum(x.treatment_count), '#0A7C3F', "perfWebDrillList('treatment','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")")) +
      sec('Calls', '#0A5C33',
        perfRowLink('Calls from app', perfNum(x.app_calls), '#123A26', "perfWebDrillList('calls_app','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRowLink('Outside calls', perfNum(x.outside_calls), '#123A26', "perfWebDrillList('calls_outside','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRowLink('RMP added', perfNum(x.rmp_added), '#123A26', "perfWebDrillList('rmp','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")")) +
      sec('Money Collected', '#0A5C33',
        perfRowLink('Cash', m.money(perfNum(x.cash_collected)), '#123A26', "perfWebDrillList('cash','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRowLink('Online', m.money(perfNum(x.online_collected)), '#123A26', "perfWebDrillList('online','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRow('Total', m.money(perfNum(x.cash_collected) + perfNum(x.online_collected)), '#0A7C3F')) +
      sec('Attendance &amp; Reports', '#B45309',
        perfRowLink('Days present', perfNum(x.present_days), '#123A26', "perfWebDrillList('attendance','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRowLink('Daily reports sent', perfNum(x.reports_sent), '#123A26', "perfWebDrillList('reports','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")") +
        perfRowLink('Leave days', perfNum(x.leave_days), (perfNum(x.leave_days) > 0 ? '#B42318' : '#5B6B81'), "perfWebDrillList('attendance','" + m.esc(code) + "','" + m.esc(ym) + "'," + (viaList ? 'true' : 'false') + ")"));
  }

  /* 📱🔒 V813 — ছোট রঙিন ট্যাগ (শুধু দেখানোর)। V771-এ এটা আলাদা
     "Phone Versions" পর্দায় ছিল; TK-র নির্দেশে সেই পর্দা উঠে গেছে,
     তাই ট্যাগটাই এখন Performance-এর সারিতে বসে (ফোনের `pvChip`-এর জোড়া)। */
  function phvChip(text, fg, bg) {
    return '<span style="display:inline-block;font-size:11.5px;font-weight:800;color:' + fg +
      ';background:' + bg + ';border:1px solid ' + fg + ';border-radius:20px;padding:2px 8px;margin-left:7px">' +
      window.MOD.esc(text) + '</span>';
  }

  window.staffPerformance = staffPerformance;
  window.staffPerformanceOne = staffPerformanceOne;
  window.perfWebDrillList = perfWebDrillList;
  window.perfWebDrillDetail = perfWebDrillDetail;
  window.perfWebDrillCached = perfWebDrillCached;
})();
