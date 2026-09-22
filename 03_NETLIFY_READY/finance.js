/* =====================================================================
   V245 MODULE 3 — INCOME & EXPENSE  (MASTER ONLY)  · additive, isolated
   Manual entries only. Never reads Patient Payment/Refund/Collection.
   All data lives in schema `fin` (RLS: Master only). Free-plan light.
   ===================================================================== */
(function () {
  var CATS = ['RMP Commission', 'Staff unexpected time Commission', 'Staff Salary', 'Chamber Rent',
    'Bills — Electricity / Water / Internet', 'Medicine / Surgical', 'Advertisement',
    'Office — Printing / Cleaning / Repair / Equipment', 'Transport / Parcel', 'Food',
    'License / Govt Fee', 'Other Expense'];
  function branches() { return ((window.RK_CONFIG || {}).branches || []).map(function (b) { return b.name; }); }
  function sb() { return window.MOD.client(); }

  // 🟢🆕 TK-অনুমোদিত (10.08.2026): হোম "আজকের হিসাব" কোন ব্রাঞ্চের দেখাবে (ডিফল্ট সব)।
  // ⛔ অন্য পর্দা/হিসাব ছোঁয়া হয়নি — শুধু আজকের কার্ডটা এই ব্রাঞ্চে ফিল্টার হয়।
  var finHomeBranch = 'All Branches';
  /* 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): টাকার হিসাবও এখন **একই মনে-রাখা ব্রাঞ্চ**
     মানবে (app.js-এর wlv1BranchGet/Set)। এখানে নামটা 'All Branches', app.js-এ 'All' —
     তাই এই দুটো ছোট অনুবাদক। ⛔ অংশীদার-ডাক্তারের ব্রাঞ্চ-লক (B617) আগের মতোই অটুট। */
  function finGlobalBranch() {
    try {
      if (typeof window.wlv1BranchGet === 'function') {
        var v = window.wlv1BranchGet();
        if (v === 'All') return 'All Branches';
        if (v) return v;
        return '';                       // এখনো বাছা হয়নি
      }
    } catch (e) {}
    return 'All Branches';
  }
  function finSetGlobalBranch(v) {
    try {
      if (typeof window.wlv1BranchSet === 'function')
        window.wlv1BranchSet(v === 'All Branches' ? 'All' : (v || ''));
    } catch (e) {}
  }
  /* লক থাকলে লক-ব্রাঞ্চ; নইলে মনে-রাখা ব্রাঞ্চ; মাস্টার ছাড়া কেউ '' পেলে আগের
     ডিফল্ট 'All Branches'-ই থাকে (অংশীদার-ডাক্তারের আচরণ একটুও বদলায়নি)। */
  function finCurBranch() {
    var l = finLockedBranch(); if (l) return l;
    var g = finGlobalBranch(); if (g) return g;
    try { if (typeof window.isMaster === 'function' && window.isMaster()) return ''; } catch (e) {}
    return 'All Branches';
  }
  function finSetHomeBranch(v) { finSetGlobalBranch(v); finHomeBranch = v || ''; finLoadToday(); }

  // 🔵🔒 B617 (11.08.2026, TK-নির্দেশ, প্রুফ-অনুমোদিত): অংশীদার-ডাক্তার নিজের ব্রাঞ্চের
  // আয়-ব্যয় লিখতে/দেখতে পারবেন (master সবখানে, ডাক্তার নিজ ব্রাঞ্চে-লক)। Amit Goldar ·
  // P.K Roy অংশীদার নন (শুধু রোগী দেখেন) — বাদ। আসল সুরক্ষা DB-র RLS (V307/V310)।
  function finU() { return (typeof window !== 'undefined' && window.user) ? window.user : null; }
  function finMob10() { var u = finU(); return u && u.mobile ? String(u.mobile).replace(/\D/g, '').slice(-10) : ''; }
  function finIsPartnerDoctor() {
    var u = finU(); if (!u || String(u.role || '').toLowerCase() !== 'doctor') return false;
    var m10 = finMob10(); return m10 !== '9046366596' && m10 !== '6297625447';   // Amit Goldar, P.K Roy বাদ
  }
  /* 🔴🔒 V415 (TK-নির্দেশ): "Paid To"-তে শুধু সংখ্যা বসানো আটকানো — ওখানে নাম
     থাকার কথা। ফাঁকা, বা একটাও অক্ষর নেই (শুধু অঙ্ক/চিহ্ন) হলে সেভ হবে না।
     ⛔ টাকার অঙ্ক · ক্যাটেগরি · তারিখ · মোড — কোনো নিয়ম বদলায়নি।
     ⛔ ফোনের `ieBadPaidTo()`-র হুবহু একই নিয়ম। */
  function finBadPaidTo(v) {
    var t = String(v || '').trim();
    if (!t) return true;
    return !/[A-Za-z\u0980-\u09E5]/.test(t);   /* বাংলা অক্ষর; বাংলা অঙ্ক ও ৳ বাদ */
  }
  function finLockedBranch() { return finIsPartnerDoctor() ? ((finU() && finU().branch) || null) : null; }
  // এন্ট্রির created_by: ডাক্তারের ক্ষেত্রে DB-র RLS created_by-র শেষ ১০ অঙ্ক = নিজের
  // মোবাইল মেলায়, তাই মোবাইল বসাই (নইলে লেখা RLS আটকাবে)। master-এ আগের কোড।
  function finCreatedBy() { return finIsPartnerDoctor() ? finMob10() : ((window.MOD.session() || {}).code || 'master'); }
  window.finIsPartnerDoctor = finIsPartnerDoctor;

  /* =====================================================================
     🟢🆕🔒 V401/V403 (16.08.2026, TK-নির্দেশ) — "দিনের দিন" নিয়ম ওয়েবেও।

     TK: *"তবে সেটা দিনের দিন হতে হবে / পুরাতন কোন হিসাব তুলতে গেলে অথবা
     Edit করতে গেলে Master এর অনুমতি লাগবে"*

     🔴 কেন এটা না করলে বিপদ: ডেটাবেস পুরনো তারিখের লেখা আটকায় ঠিকই, কিন্তু
        **কোনো এরর দেয় না** — চুপচাপ ০টি সারিতে কাজ করে। ফলে ওয়েবে ডাক্তার
        "Save" চেপে ভাবতেন হয়ে গেছে, অথচ কিছুই হয়নি। তাই আগেই এখানে ধরা হয়।
     ⛔ মাস্টারের কিছু বদলায়নি — তিনি আগের মতোই সরাসরি সব পারেন।
     ===================================================================== */
  function finIsMaster() { try { return !!window.MOD.isMasterModule(); } catch (e) { return false; } }

  /* 🔵 V406 (16.08.2026) — V401-এর বাকি তিনটে কাজ ওয়েবে।
     ফোনের `IncomeExpenseActivity.entryPermission()` · `IeRequests.kt` ও
     `BriefingActivity`-র ঘণ্টার অংশের হুবহু নকল — একই RPC, একই নিয়ম।
     ⛔ কোনো নতুন SQL লাগেনি; V401/V403-এর ফাংশনগুলোই ডাকা হয়।
     ⛔ আসল নিরাপত্তা ডেটাবেসেই (master-only RPC) — এটা শুধু পর্দা। */
  function finIsStaffOnly() {
    try {
      if (finIsMaster()) return false;
      var r = String((window.MOD.session() || {}).role_kind || '').toLowerCase();
      return r === 'staff';
    } catch (e) { return false; }
  }

  // ---- ১) Entry Permission পর্দা (শুধু মাস্টার) ----
  async function finEntryPermission() {
    if (!finIsMaster()) return finToast('Only Master');
    var m = window.MOD;
    var bs = branches();
    var sel = window.__finPermBranch || bs[0] || '';
    window.__finPermBranch = sel;
    document.getElementById('app').innerHTML =
      '<div class="wrap anMod anModFin"><div class="topbar"><b style="color:#6A5320">🔑 Entry Permission</b>' +
      '<button class="ghost" onclick="incomeExpense()">Back</button></div><div class="page">' +
      '<div class="card"><label>Branch</label><select id="finPermBr" class="input" onchange="finPermBranchChange()">' +
      bs.map(function (b) { return '<option value="' + m.esc(b) + '"' + (b === sel ? ' selected' : '') + '>' + m.esc(b) + '</option>'; }).join('') +
      '</select></div>' +
      '<div class="tiny mut" style="margin:2px 4px 8px">চাবি চালু করলে ওই কর্মী/ডাক্তার <b>ওই ব্রাঞ্চের</b> আজকের আয়-খরচ তুলতে পারবেন। পুরনো তারিখ হলে মাস্টারের অনুমোদন লাগবে।</div>' +
      '<div id="finPermList" class="mut">Loading…</div></div></div>';
    finPermLoad();
  }
  function finPermBranchChange() {
    window.__finPermBranch = document.getElementById('finPermBr').value;
    finPermLoad();
  }
  async function finPermLoad() {
    var m = window.MOD, box = document.getElementById('finPermList');
    if (!box) return;
    box.innerHTML = 'Loading…';
    var rows = null;
    try {
      var client = await sb();
      var r = await client.schema('fin').rpc('ie_permit_candidates', { p_branch: window.__finPermBranch });
      if (!r || r.error || !Array.isArray(r.data)) rows = null; else rows = r.data;
    } catch (e) { rows = null; }
    // ⛔ সৎ বার্তা — পড়া ব্যর্থ আর "কেউ নেই" আলাদা।
    if (rows === null) { box.innerHTML = '<div class="card mut">লোড করা গেল না — একটু পরে আবার দেখুন</div>'; return; }
    if (!rows.length) { box.innerHTML = '<div class="card mut">এই ব্রাঞ্চে কোনো কর্মী বা ডাক্তার পাওয়া গেল না।</div>'; return; }
    box.innerHTML = '<div class="card">' + rows.map(function (p) {
      var on = !!p.can_entry;
      var sub = [p.role_kind || '', window.__finPermBranch, p.is_partner ? 'Partner' : ''].filter(Boolean).join(' · ');
      return '<div style="display:flex;align-items:center;gap:10px;padding:10px 2px;border-top:1px solid #eef2ef">' +
        '<div style="flex:1"><b>' + m.esc(p.full_name || p.person_code) + '</b>' +
        '<br><span class="tiny mut">' + m.esc(sub) + '</span></div>' +
        '<button class="small ' + (on ? '' : 'ghost') + '" onclick="finPermToggle(\'' + m.esc(p.person_code) + '\',' + (on ? 'false' : 'true') + ')">' +
        (on ? 'ON' : 'OFF') + '</button></div>';
    }).join('') + '</div>';
  }
  async function finPermToggle(code, turnOn) {
    if (!finIsMaster()) return finToast('Only Master');
    try {
      var client = await sb();
      // 🔒 PK এখানে (person_code, branch) — ফোনের মতোই on_conflict স্পষ্ট করে।
      var r = await client.schema('fin').from('entry_permits').upsert(
        { person_code: code, branch: window.__finPermBranch, can_entry: !!turnOn,
          updated_by: String((window.MOD.session() || {}).code || ''),
          updated_at: new Date().toISOString() },
        { onConflict: 'person_code,branch' });
      if (r && r.error) return finToast('Could not save — try again');
      finToast(turnOn ? 'Turned ON' : 'Turned OFF');
      finPermLoad();
    } catch (e) { finToast('Could not save — try again'); }
  }

  // ---- ২) মাস্টারের ঘণ্টায় আয়-খরচের অনুরোধ — Approve / Reject ----
  async function finPendingRequests() {
    if (!finIsMaster()) return [];
    try {
      var client = await sb();
      var r = await client.schema('fin').from('ie_requests')
        .select('*').eq('status', 'PENDING').order('requested_at', { ascending: true }).limit(100);
      return (r && !r.error && Array.isArray(r.data)) ? r.data : [];
    } catch (e) { return []; }
  }
  // ফোনের `IeRequests.describe()`-এর হুবহু একই বাক্য (ইংরেজি, TK-নির্দেশ)।
  function finDescribeRequest(r) {
    var m = window.MOD, p = r.payload || {};
    var what;
    switch (r.kind) {
      case 'ADD_COLLECTION':  what = 'Add Collection — Cash ' + m.money(p.cash || 0) + ' · Online ' + m.money(p.online || 0); break;
      case 'EDIT_COLLECTION': what = 'Edit Collection — Cash ' + m.money(p.cash || 0) + ' · Online ' + m.money(p.online || 0); break;
      case 'ADD_EXPENSE':     what = 'Add Expense — ' + (p.category || '') + (p.paid_to ? ' · ' + p.paid_to : '') + ' · ' + m.money(p.amount || 0); break;
      case 'EDIT_EXPENSE':    what = 'Edit Expense — ' + (p.category || '') + (p.paid_to ? ' · ' + p.paid_to : '') + ' · ' + m.money(p.amount || 0); break;
      default:                what = r.kind || '';
    }
    var who = r.requested_by_name || r.requested_by || '';
    var why = (r.reason && r.reason !== 'null') ? ('\nReason: ' + r.reason) : '';
    return finSlash(r.entry_date || '') + ' · ' + (r.branch || '') + '\n' + what + '\nBy: ' + who + why;
  }
  async function finApprovalsHtml() {
    var m = window.MOD, rows = await finPendingRequests();
    if (!rows.length) return '';
    return '<div class="card" style="border:1px solid #ffd58a;background:#fff7e6">' +
      '<b>💰 Income / Expense approval (' + rows.length + ')</b>' +
      rows.map(function (r) {
        return '<div style="padding:9px 0;border-top:1px solid #f0e2c0">' +
          '<div style="white-space:pre-line">' + m.esc(finDescribeRequest(r)) + '</div>' +
          '<div class="actions" style="margin-top:6px">' +
          '<button class="small" onclick="finDecide(\'' + m.esc(r.id) + '\',true)">Approve</button>' +
          '<button class="small ghost" onclick="finDecide(\'' + m.esc(r.id) + '\',false)">Reject</button>' +
          '</div></div>';
      }).join('') + '</div>';
  }
  async function finDecide(id, approve) {
    if (!finIsMaster()) return finToast('Only Master');
    try {
      var client = await sb();
      var r = await client.schema('fin').rpc('ie_decide_request', { p_id: id, p_approve: !!approve, p_note: null });
      if (r && r.error) return finToast('Could not save — try again');
      finToast(approve ? 'Approved' : 'Rejected');
      try { if (typeof window.finRenderApprovals === 'function') window.finRenderApprovals(); } catch (e) {}
    } catch (e) { finToast('Could not save — try again'); }
  }
  // ড্যাশবোর্ডের ঘণ্টার পাতায় বসানোর জন্য — app.js থেকে ডাকা হয়।
  async function finRenderApprovals() {
    var host = document.getElementById('finIeApprovals');
    if (!host) return;
    try { host.innerHTML = await finApprovalsHtml(); } catch (e) { host.innerHTML = ''; }
  }
  function finToast(msg) { try { toast(msg); } catch (e) {} }
  function finToday() { return window.MOD.todayIST(); }
  function finIsToday(d) { return String(d || '') === finToday(); }
  function finSlash(iso) {
    try { var p = String(iso || '').split('-'); return p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : iso; }
    catch (e) { return iso; }
  }
  /** পুরনো তারিখে কিছু করতে গেলে — কারণ চেয়ে মাস্টারের কাছে অনুরোধ পাঠায়।
   *  সফল হলে true ফেরে (তখন ডাকা জায়গাটা আর সরাসরি সেভ করবে না)। */
  async function finAskApproval(kind, branch, entryDate, targetId, payload) {
    var why = window.prompt(
      finSlash(entryDate) + ' is not today, so it cannot be changed directly.\n\n' +
      'Send a request to Master. Write what you want to change and why.', '');
    if (why === null) return true;                  // বাতিল — সেভও নয়
    try {
      var client = await sb();
      var r = await client.schema('fin').rpc('ie_request', {
        p_kind: kind, p_branch: branch, p_entry_date: entryDate,
        p_target_id: targetId || null, p_payload: payload || {}, p_reason: why
      });
      if (r.error) {
        if (typeof toast === 'function') toast(r.error.message || 'Could not send the request');
        else alert(r.error.message || 'Could not send the request');
      } else if (typeof toast === 'function') toast('Request sent to Master.');
      else alert('Request sent to Master.');
    } catch (e) {
      if (typeof toast === 'function') toast('Could not send the request — try again');
    }
    return true;
  }

  async function incomeExpense() {
    window.MOD.gate('Income & Expense', render);
  }
  async function render() {
    if (!window.MOD.isMasterModule() && !finIsPartnerDoctor()) {
      document.getElementById('app').innerHTML =
        '<div class="wrap anMod anModFin"><div class="page"><div class="card">This module is for Master only.' +
        ' <button class="ghost" onclick="dashboard()">Back</button></div></div></div>';
      return;
    }
    // 🔵 B617: ডাক্তার হলে নিজের ব্রাঞ্চেই লক (master হলে আগের মতোই)।
    var finLockBr = finLockedBranch();
    if (finLockBr) finHomeBranch = finLockBr;
    else finHomeBranch = finCurBranch();             // 🟢🔒 V398: মনে-রাখা ব্রাঞ্চ
    var host = document.getElementById('app');
    // 🎨 TK-অনুমোদিত সবুজ থিম (02.08.2026, মকআপ দেখিয়ে পাশ, নেভি/কালো বাদ) —
    // সম্পূর্ণ নতুন ইনলাইন স্টাইল, পুরনো .card/.ghost/.grid ক্লাস একটুও ছোঁয়া হয়নি,
    // তাই বাকি কোনো স্ক্রিনের ডিজাইনে প্রভাব পড়ে না।
    function finPill(icon, title, sub, onClick, bg, iconBg, fg, border) {
      return '<div onclick="' + onClick + '" style="display:flex;align-items:center;gap:12px;' +
        'padding:14px 15px;border-radius:14px;margin-bottom:10px;cursor:pointer;' +
        'background:' + bg + ';' + (border ? 'border:1px solid ' + border + ';' : '') + '">' +
        '<div style="width:34px;height:34px;border-radius:9px;background:' + iconBg + ';' +
        'display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0;color:#fff">' + icon + '</div>' +
        '<div style="flex:1;min-width:0"><div style="font-weight:700;font-size:15px;color:' + fg + '">' + title + '</div>' +
        (sub ? '<div style="font-size:11px;color:' + fg + ';opacity:.75;margin-top:1px">' + sub + '</div>' : '') +
        '</div></div>';
    }
    // 🔴🆕🔒 TK-নির্দেশ (08.08.2026, ফটো-প্রুফে লক) — ফোনের মতোই সহজ সাজ: উপরে
    // "আজকের হিসাব" কার্ড নিজে থেকেই দেখায়, নিচে ঠিক ৪টা সমান বক্স (২×২)। স্থির
    // ক্যালেন্ডার-ইমোজি বাদ, লাইভ তারিখ দেখানো হয়। ⛔ finAddCollection/finAddExpense/
    // finMonthly/finLedgerSheet/finDailyLedger ফাংশন ও হিসাব একটুও বদলায়নি।
    var _td = window.MOD.todayIST().split('-');
    var _mon = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][Number(_td[1]) - 1];
    var _day = String(Number(_td[2]));
    var liveCal = '<span style="display:inline-flex;flex-direction:column;align-items:center;justify-content:center;' +
      'width:34px;height:34px;border-radius:8px;background:#fff;border:1px solid #BFDCC9;line-height:1">' +
      '<span style="font-size:8px;font-weight:800;color:#B42318">' + _mon + '</span>' +
      '<span style="font-size:12px;font-weight:800;color:#0A5C33">' + _day + '</span></span>';
    // 🔵🔒 TK-প্রুফ (09.08.2026): বক্সে কোনো আইকন/ইমোজি নেই — শুধু লেখা; বক্স ছোট।
    function finBox(icon, label, onClick, grad, bg, fg) {
      return '<div onclick="' + onClick + '" style="cursor:pointer;flex:1;border-radius:14px;padding:14px 8px;' +
        'min-height:46px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;' +
        (grad ? 'background:linear-gradient(135deg,' + bg + ');color:#fff;' : 'background:#EEF7F1;border:1px solid #CFE4D6;color:' + fg + ';') +
        'box-shadow:0 3px 9px rgba(0,0,0,.08)">' +
        (icon ? '<div style="font-size:20px;line-height:1">' + icon + '</div>' : '') +
        '<div style="font-weight:800;font-size:14px;text-align:center">' + label + '</div></div>';
    }
    // 🟢🆕 হোম ব্রাঞ্চ-সিলেক্টর (কম্প্যাক্ট চিপ, content-মাপ — কোথাও উপচে পড়ে না)।
    // width:auto দিয়ে গ্লোবাল select{width:100%} ওভাররাইড — মোবাইল/ডেস্কটপ দুটোতেই ঠিক বসে।
    var branchSel = finLockBr
      ? '<span style="display:inline-block;border:1px solid #B7E3C5;background:#E7F6EC;color:#0A5C33;' +
        'font-weight:800;font-size:12.5px;border-radius:10px;padding:7px 12px">🏥 ' + window.MOD.esc(finHomeBranch) + '</span>'
      : '<select onchange="finSetHomeBranch(this.value)" style="width:auto;max-width:100%;' +
      'border:1px solid #B7E3C5;background:#E7F6EC;color:#0A5C33;font-weight:800;font-size:12.5px;' +
      'border-radius:10px;padding:7px 10px;margin:0">' +
      (finHomeBranch ? [] : ['']).concat(['All Branches']).concat(branches()).map(function (b) {
        return '<option value="' + window.MOD.esc(b) + '"' + (b === finHomeBranch ? ' selected' : '') +
          '>🏥 ' + window.MOD.esc(b || 'Select Branch') + '</option>';
      }).join('') + '</select>';
    host.innerHTML = '<div class="wrap anMod anModFin"><div class="topbar">' +
      /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬) — শিরোনামগুলো ফোনের হুবহু
         (IncomeExpenseActivity.kt:1194, 251, 1326, 2614)। ⛔ ফোনে এগুলো
         বাংলাতেই আছে ও TK-এর অনুমোদিত — তাই ওয়েবেও সেই একই লেখা। */
      '<b style="color:#0A5C33">💵 টাকার হিসাব</b>' +
      /* 🟢🔒 V920 (৩১.০৮.২০২৬, TK ডেমো প্রুফ দেখে "হ্যাঁ পাশ, বসিয়ে দিন") —
         TK: *"ব্রাঞ্চ সিলেক্ট উপরে হেডারে ডান পাশে, ক্যালেন্ডারের বাঁ পাশে"*।
         নিচের আলাদা সারিটা উঠে গেল, তাই টেবিল আরও উপরে ওঠে — বড় পর্দায়
         স্ক্রল না করেই সব দেখা যায়। ⛔ বাছাইয়ের কাজ ও নিয়ম অপরিবর্তিত। */
      '<span style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;justify-content:flex-end">' +
      branchSel +
      '<span onclick="finPickDay()" style="cursor:pointer">' + liveCal + '</span>' +
      /* ⋮🔒 V1216 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — পাঁচটা বোতামই এখন
         ⋮ মেনুতে; ব্রাঞ্চ ও ক্যালেন্ডার বাঁয়ে, ⋮ তাদের ডানে। ফোনের হুবহু যমজ।
         ⛔ কে কোনটা দেখবেন সেই নিয়ম এক অক্ষরও বদলায়নি। */
      '<span onclick="finHomeMenu()" style="cursor:pointer;font-size:20px;font-weight:800;color:#0A5C33;padding:0 6px">&#8942;</span>' +
      '<button class="ghost" onclick="dashboard()">Home</button></span></div><div class="page">' +
      '<div id="finToday" class="card" style="padding:14px">Loading...</div>' +
      /* 🟢🔒 V630 (২৪.০৮.২০২৬, TK-নির্দেশ) — "আয় এবং ব্যয় এটা দুই রকম ভাবে
         আলাদা কলম থাকবে না।" আলাদা "Add Collection"/"Add Expense" বোতাম দুটো
         সরানো হলো — এখন "পুরো খাতা" (Sheet)-ই একমাত্র পথ: Cash/Online ঘরে
         ৩-চাপে বা খালি খরচ ঘরে চাপ দিয়েই টাকা ঢোকানো যায়। ⛔ `finAddCollection`/
         `finAddExpense` ফাংশন মোছা হয়নি — `finAddExpense` এখনো Sheet-এর খালি
         খরচ-ঘর থেকে ডাকা হয়। */
      /* 🔵 V406 (16.08.2026) — V401-এর নিয়ম ওয়েবেও (আগে শুধু ফোনে ছিল)।
         TK-লক করা ছক: **staff টাকার খাতা ও মাসের হিসাব দেখতেই পাবে না**;
         doctor দেখবে কিন্তু বদলাতে পারবে না; master সব।
         ⛔ ডেটাবেস আগে থেকেই সব দিক আটকাচ্ছে (RLS) — এটা শুধু পর্দা থেকেও
            সরিয়ে দেওয়া, যাতে ফাঁকা পাতা দেখে কেউ বিভ্রান্ত না হয়।
         ⛔ master ও doctor-এর কিছুই বদলায়নি। */
      /* 🔴 V437 #18 (নিজের অডিটে ধরা) — ফোনে staff-only অবস্থাতেও সারিটা থাকে,
         শুধু একটাই ঘর: **"Today's Entries"** → dailyLedger()
         (`IncomeExpenseActivity.kt:1448,1455-1456`)। ওয়েবে পুরো সারিটাই লুকানো
         ছিল, ফলে স্টাফ `finDailyLedger()`-এ পৌঁছানোরই কোনো পথ পেতেন না
         (ফাংশনটা আছে, কিন্তু কোনো বোতাম ছিল না)।
         ⛔ master/doctor-এর জন্য আগের দুটো ঘরই অপরিবর্তিত। */
      /* 🟢🔒 V920 — বোতামগুলো এখন **একটাই সারিতে** (আগে প্রতিটা আলাদা সারিতে
         পুরো চওড়া জুড়ে বসত, তাই বড় পর্দায় চারটে সারি জায়গা খেয়ে টেবিলকে
         নিচে ঠেলে দিত)। ছোট পর্দায় `flex-wrap` নিজে থেকেই আগের মতো একটার
         নিচে একটা সাজায় (`.finTileRow` — styles.css)।
         ⛔ কোন বোতাম কার জন্য, কী কাজ করে — কিছুই বদলায়নি। */
      /* ⋮ V1216 — সারিটা আর বসে না, সবগুলো ⋮ মেনুতে (নিচের finBox-গুলো
         মোছা হয়নি; TK-নিয়ম: নিজে থেকে কোড মোছা হয় না)। */
      '' + (false ?
      '<div class="finTileRow">' +
      (finIsStaffOnly() ?
        finBox('', "Today's Entries", 'finDailyLedger()', false, '', '#0A5C33') :
        finBox('', 'এই মাসের হিসাব', 'finMonthly()', false, '', '#0A5C33') +
        finBox('', 'পুরো খাতা', 'finLedgerSheet()', false, '', '#0A5C33')) +
      /* 🟢🔒 V629 (২৪.০৮.২০২৬, TK-নির্দেশ) — "ব্যাংকে যেমন স্টেটমেন্ট বের করা
         যায়, আমার অ্যাপেও সেরকম চাই।" Ledger Sheet/Monthly-র মতোই বিধিনিষেধ
         (staff-only দেখবেন না)। */
      (!finIsStaffOnly() ? finBox('📄', 'Statement', 'finStatement()', false, '', '#0A5C33') : '') +
      /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬) — ফোনে এই সারিতে **আগে "🤝 অংশীদারি ভাগ",
         তারপর "Entry Permission"** (IncomeExpenseActivity.kt:1468-1478), আর
         Entry Permission-এ কোনো চাবি-আইকন নেই। ওয়েবে ক্রম উল্টো ছিল ও
         🔑 আইকন বসানো ছিল। ⛔ বোতামের কাজ ও অনুমতির নিয়ম অপরিবর্তিত। */
      finBox('🤝', 'অংশীদারি ভাগ', 'finPartners()', false, '', '#0A5C33') +
      (finIsMaster() ? finBox('', 'Entry Permission', 'finEntryPermission()', false, '', '#6A5320') : '') +
      '</div>' : '') +
      '<div id="finBody"></div></div></div>';
    finLoadToday();
  }

  /* ⋮ V1216 — ⋮ মেনু। শর্তগুলো উপরের বোতামগুলোর হুবহু নকল, তাই কে কী দেখবেন
     তা কোথাও বদলায় না। ⛔ 📅 ইমোজি কোথাও নয় (TK-র চিরস্থায়ী নিষেধ)। */
  function finHomeMenu(){
    var out = [];
    if (finIsStaffOnly()) {
      out.push(['Today\'s Entries', 'finDailyLedger()']);
    } else {
      out.push(['এই মাসের হিসাব', 'finMonthly()']);
      out.push(['পুরো খাতা', 'finLedgerSheet()']);
      out.push(['📄 Statement', 'finStatement()']);
    }
    out.push(['🤝 অংশীদারি ভাগ', 'finPartners()']);
    if (finIsMaster()) out.push(['Entry Permission', 'finEntryPermission()']);
    var html = out.map(function (it) {
      return '<button class="menuBtn" onclick="closeModal();' + it[1] + '"><b>' +
        window.MOD.esc(it[0]) + '</b></button>';
    }).join('');
    try { modal('<h2>💵 টাকার হিসাব</h2><div class="grid menuGrid">' + html + '</div>'); }
    catch (e) { try { finMonthly(); } catch (e2) {} }
  }
  window.finHomeMenu = finHomeMenu;

  // 🔵🔒 B617 (11.08.2026, TK-অনুমোদিত প্রুফ "সাজ ক"): প্রফেশনাল টেবিল-কার্ড — সবুজ
  // হেডার, কলাম Cash·Online·মোট, সারি আয়(সবুজ)·ব্যয়(লাল)·অবশিষ্ট(নীল=আয়−ব্যয়)।
  // ব্যয়ের Cash/Online আসে fin.expenses.mode থেকে। ⛔ মোট ব্যয় আগের মতোই অটুট।
  function finTodayCardHtml(dotted, data, stale) {
    var m = window.MOD;
    function bare(n) { return String(m.money(n)).replace('₹', ''); }
    var note = data ? (stale ? ' (updating…)' : '') : ' (not loaded)';
    function dcell(v, w, col, bold) {
      return '<div style="flex:' + w + ';text-align:right;font-size:13.5px;color:' + col + (bold ? ';font-weight:800' : '') + '">' + v + '</div>';
    }
    function drow(label, lcol, cash, online, tot, vcol, tcol, bandStyle, tbold) {
      return '<div style="display:flex;align-items:center;padding:10px 14px;' + (bandStyle || '') + '">' +
        '<div style="flex:1.1;font-weight:800;font-size:13.5px;color:' + lcol + '">' + label + '</div>' +
        dcell(cash, 1, vcol, false) + dcell(online, 1, vcol, false) + dcell(tot, 1.05, tcol, true) + '</div>';
    }
    var incCash = data ? bare(data.cash) : '—', incOnline = data ? bare(data.online) : '—', incTot = data ? m.money(data.tot) : '—';
    var exCash = data ? bare(data.expCash) : '—', exOnline = data ? bare(data.expOnline) : '—', exTot = data ? m.money(data.exp) : '—';
    var reCash = data ? bare(data.remCash) : '—', reOnline = data ? bare(data.remOnline) : '—', reTot = data ? m.money(data.remTot) : '—';
    return '<div style="border:1px solid #E2ECE6;border-radius:16px;overflow:hidden;box-shadow:0 6px 18px rgba(16,40,28,.06)">' +
      // header
      '<div style="display:flex;justify-content:space-between;align-items:center;padding:12px 15px;' +
        'background:linear-gradient(135deg,#0B4F2A,#16A34A)">' +
        '<b style="color:#fff;font-size:14px">💵 আজকের হিসাব' + note + '</b>' +
        /* 🔴 V414 (TK-রিপোর্ট, ছবিসহ): স্টাফ/ডাক্তারের ব্রাঞ্চ বাঁধা — উপরে একবার
           নাম লেখা থাকে, তাই এখানে দ্বিতীয়বার লেখা হয় না। Master-এর ক্ষেত্রে
           আগের মতোই থাকে (তিনি ব্রাঞ্চ বদলাতে পারেন)। ফোনেও হুবহু একই নিয়ম। */
        /* 🔴 V415 (TK-নির্দেশ): ব্রাঞ্চের নাম উপরে একবারই থাকবে। */
        '</div>' +
      // column header
      '<div style="display:flex;align-items:center;padding:8px 14px;background:#F3F7F4;border-bottom:1px solid #EAF0EC">' +
        '<div style="flex:1.1"></div>' +
        '<div style="flex:1;text-align:right;font-size:11px;font-weight:800;color:#6A7D72">Cash</div>' +
        '<div style="flex:1;text-align:right;font-size:11px;font-weight:800;color:#6A7D72">Online</div>' +
        '<div style="flex:1.05;text-align:right;font-size:11px;font-weight:800;color:#6A7D72">মোট</div></div>' +
      drow('আয়', '#0A7C3F', '<span onclick="finTodayIncomeEditor(\'cash\')" style="display:block;cursor:pointer">'+incCash+'</span>', '<span onclick="finTodayIncomeEditor(\'online\')" style="display:block;cursor:pointer">'+incOnline+'</span>', incTot, '#12704A', '#0A7C3F', 'border-bottom:1px solid #F1F5F2;background:#fff') +
      drow('<span onclick="finAddExpense(window.MOD.todayIST(),finHomeBranch)" style="display:block;cursor:pointer">ব্যয়</span>', '#B42318', exCash, exOnline, exTot, '#B0392B', '#B42318', 'background:#fff') +
      drow('অবশিষ্ট', '#FFFFFF', reCash, reOnline, reTot, '#DBE9FF', '#FFFFFF', 'background:linear-gradient(90deg,#0B2B59,#155EAE)') +
      '</div>';
  }

  async function finTodayIncomeEditor(field) {
    var m = window.MOD, br = finHomeBranch;
    if (!br || br === 'All Branches') { finToast('আগে একটি Branch বাছুন'); return; }
    var label = field === 'online' ? 'Online' : 'Cash', date = m.todayIST();
    var value = prompt(label + ' — ' + date, '');
    if (value === null) return;
    var amount = Number(String(value).replace(/,/g, ''));
    if (!isFinite(amount) || amount < 0) { finToast('সঠিক Amount লিখুন'); return; }
    var client = await sb(), res;
    try { res = await client.schema('fin').from('collections').select('*').eq('entry_date', date).eq('branch', br).eq('ignored', false).order('created_at', {ascending:true}); }
    catch (e) { finToast('লোড করা গেল না'); return; }
    if (res && res.error) { finToast('লোড করা গেল না'); return; }
    var rows = (res && res.data) || [], target = rows[0], other = rows.slice(1).reduce(function(s,x){return s+Number(x[field]||0);},0);
    if (amount < other) { finToast('অন্য এন্ট্রিতে ইতিমধ্যে ' + m.money(other) + ' আছে—পুরো খাতা থেকে ঠিক করুন'); return; }
    var row = target ? Object.assign({}, target) : {id:m.uuid(),entry_date:date,branch:br,cash:0,online:0,expense_notes:'',expense_total:0,created_by:finCreatedBy()};
    row[field] = amount - other;
    var saved = await m.save('fin','collections',row);
    if (saved && saved.error) { finToast('Save হয়নি'); return; }
    finToast('Saved'); finLoadToday();
  }
  window.finTodayIncomeEditor = finTodayIncomeEditor;

  // পর্দা খুললেই আজকের Collection/Expense একবার টেনে "আজকের হিসাব" কার্ডে বসায়
  // (আগে Daily Ledger-এ চাপ দিলে ঠিক এই একটাই কল হতো — বাড়তি কিছু নয়)।
  async function finLoadToday() {
    var m = window.MOD, host = document.getElementById('finToday');
    if (!host) return;
    /* 🟢🔒 V398: ব্রাঞ্চ না-বাছা থাকলে **একটাও ক্লাউড-অনুরোধ যাবে না** — শুধু বার্তা। */
    if (!finHomeBranch) {
      host.innerHTML = (typeof window.wlv1BranchAskCard === 'function')
        ? window.wlv1BranchAskCard()
        : '<div class="mut">🏥 উপরের বাক্স থেকে একটি Branch বাছুন।</div>';
      return;
    }
    var d = m.todayIST();
    var dp = d.split('-'); var dotted = dp[2] + '/' + dp[1] + '/' + dp[0];
    // 🔵🔒 cache-first (09.08.2026, TK-অনুমোদিত — ফোনের income_expense_cache-এর মতোই):
    // শেষ-জানা আজকের হিসাব থাকলে **সাথে সাথেই** দেখাই ("হালনাগাদ হচ্ছে…" লেখা-সহ),
    // তারপর নেট থেকে আসল সংখ্যা এলে বসাই। ⛔ হিসাবের নিয়ম বদলায়নি — শুধু আগে দেখায়।
    var cacheKey = 'fin_today_' + d + '_' + finHomeBranch;   // 🟢 ব্রাঞ্চ-ভিত্তিক ক্যাশ (মিশে যায় না)
    var cached = m.localGet(cacheKey);
    if (cached) host.innerHTML = finTodayCardHtml(dotted, cached, true);
    var coll = [], exp = [], ok = true, client = null;
    try { client = await sb(); } catch (e) { ok = false; }
    if (client) {
      try {
        // 🟢🆕 নির্বাচিত ব্রাঞ্চ হলে ওই ব্রাঞ্চেই ফিল্টার; "All Branches" হলে আগের মতোই সব।
        var cQ = client.schema('fin').from('collections').select('*').eq('entry_date', d).eq('ignored', false);
        var eQ = client.schema('fin').from('expenses').select('*').eq('entry_date', d).eq('ignored', false);
        if (finHomeBranch !== 'All Branches') { cQ = cQ.eq('branch', finHomeBranch); eQ = eQ.eq('branch', finHomeBranch); }
        coll = (await cQ).data || [];
        exp = (await eQ).data || [];
      } catch (e) { ok = false; }
    } else { ok = false; }
    // পড়া ব্যর্থ: ক্যাশ থাকলে সেটাই থাক (ভুল "—" দিয়ে ঢাকব না); না থাকলে জানাই।
    if (!ok) { if (!cached) host.innerHTML = finTodayCardHtml(dotted, null); return; }
    var cash = coll.reduce(function (s, x) { return s + Number(x.cash || 0); }, 0);
    var online = coll.reduce(function (s, x) { return s + Number(x.online || 0); }, 0);
    // 🔵 B617: ব্যয়ও Cash/Online-এ ভাগ — fin.expenses-এর `mode` ধরে। collection-এর
    // ভেতরের inline খরচ (mode নেই) Cash-এ ধরা হয় → মোট ব্যয় আগের মতোই অটুট।
    var expCash = 0, expOnline = 0;
    exp.forEach(function (x) { var a = Number(x.amount || 0); if (String(x.mode || 'Cash').toLowerCase() === 'online') expOnline += a; else expCash += a; });
    coll.forEach(function (x) { var note = x.expense_notes || ''; if (note) expCash += (x.expense_total != null && x.expense_total >= 0) ? Number(x.expense_total) : finSumNumbers(note); });
    var expTotal = expCash + expOnline;
    var data = { cash: cash, online: online, tot: cash + online, exp: expTotal,
      expCash: expCash, expOnline: expOnline,
      remCash: cash - expCash, remOnline: online - expOnline, remTot: (cash + online) - expTotal };
    m.localSet(cacheKey, data);
    host.innerHTML = finTodayCardHtml(dotted, data);
  }

  // 🆕 ক্যালেন্ডার-আইকন দিয়ে যেকোনো তারিখের মোট Collection/Expense (সব ব্রাঞ্চ) —
  // ফোনের pickDateForSummary()-র সাথে মিলিয়ে (B290)।
  /* 🔴 V430 (TK-নির্দেশ ১৮.০৮.২০২৬) — ফোনে এখানে **ক্যালেন্ডার** খোলে
     (IncomeExpenseActivity.kt:1766-1772)। ওয়েবে একটা লেখা-বাক্স আসত যেখানে
     হাতে "YYYY-MM-DD" লিখতে হত — সেটা নির্দেশ-লেখাও বটে, TK-এর নিয়মে
     থাকার কথা নয়। এখন ব্রাউজারের নিজের ক্যালেন্ডারই খোলে। */
  function finPickDay() {
    var m = window.MOD;
    var host = document.getElementById('finDayPick');
    if (!host) {
      host = document.createElement('input');
      host.type = 'date'; host.id = 'finDayPick';
      host.style.cssText = 'position:fixed;left:-9999px;top:0;opacity:0';
      document.body.appendChild(host);
      host.addEventListener('change', function () { if (host.value) finShowDay(host.value); });
    }
    host.value = m.todayIST();
    try { if (host.showPicker) host.showPicker(); else host.click(); } catch (e) { host.click(); }
  }
  // 🔴🔒 V459 (TK-নির্দেশ ১৮.০৮.২০২৬, Android-এ V452 হিসেবে করা হয়েছিল —
  // এখানে ওয়েবেও একই ফিক্স আনা হলো: "All branch কেন দেখাবে, cash/online
  // আলাদা কেন দেখাবে না")। এখন finHomeBranch (হেডারের ব্রাঞ্চ-চিপ) মেনে
  // ফিল্টার হয়, আর Cash/Online দুটোই আলাদা লাইনে দেখায়।
  async function finShowDay(d) {
    if (!d) return;
    var m = window.MOD, client = await sb();
    var coll = [], exp = [];
    var brSel = finHomeBranch || 'All Branches';
    try {
      var cQ = client.schema('fin').from('collections').select('*').eq('entry_date', d).eq('ignored', false);
      var eQ = client.schema('fin').from('expenses').select('*').eq('entry_date', d).eq('ignored', false);
      if (brSel !== 'All Branches') { cQ = cQ.eq('branch', brSel); eQ = eQ.eq('branch', brSel); }
      coll = (await cQ).data || [];
      exp = (await eQ).data || [];
    } catch (e) { try { toast('লোড করা গেল না — একটু পরে আবার দেখুন'); } catch (e2) { alert('লোড করা গেল না — একটু পরে আবার দেখুন'); } return; }
    var cashColl = coll.reduce(function (s, x) { return s + Number(x.cash || 0); }, 0);
    var onlineColl = coll.reduce(function (s, x) { return s + Number(x.online || 0); }, 0);
    /* 🐞🔒 V1322 (TK-রিপোর্ট ছবিসহ, ১১.০৯.২০২৬: "দুই পর্দায় দুই রকম কেন") —
       এই পপ-আপ শুধু `fin.collections`-এর হাতে-লেখা সারি পড়ত। যেদিন কেউ এখনও
       হাতে "নতুন দিন" এন্ট্রি বসাননি, ওই টেবিলে সারিই নেই ⇒ সৎভাবে ₹0
       দেখাত — অথচ Payment স্ক্রিন আসল টাকা ঠিকই দেখাত। খাতা ও Monthly
       Summary অনেক আগে থেকেই এই ফাঁক ভরে দেয় wlv1AutoIncomeForDay() দিয়ে
       (V928/V1100) — এখানেই সেটা ডাকা হতো না। এখন শুধু তখনই ডাকা হচ্ছে যখন
       fin.collections-এ সত্যিই কোনো হাতে-লেখা সারি নেই। */
    if (!coll.length) {
      try {
        if (typeof window.wlv1AutoIncomeForDay === 'function') {
          var __auto = window.wlv1AutoIncomeForDay(d, brSel);
          if (__auto) { cashColl = __auto.cash; onlineColl = __auto.online; }
        }
      } catch (e) {}
    }
    // 🟢🔒 V1440 (১৩.০৯.২০২৬, TK ছবি-প্রুফে "Option C" পাশ — খাতার সারি ৫৬৪)
    // ব্যয়ও Cash/Online-এ ভাগ (fin.expenses.mode) — ফোনের showDaySummary()-র
    // হুবহু একই নিয়ম (আগে ওয়েবে এই পপ-আপ শুধু একটা মোট দেখাত, ভাগ করত না)।
    // ⛔ মোট ব্যয়ের অঙ্ক এক অক্ষরও বদলায়নি।
    var cashExp = 0, onlineExp = 0;
    exp.forEach(function (x) {
      var a = Number(x.amount || 0);
      if (String(x.mode || 'Cash').toLowerCase() === 'online') onlineExp += a; else cashExp += a;
    });
    coll.forEach(function (x) {
      var note = x.expense_notes || '';
      if (note) cashExp += (x.expense_total != null && x.expense_total >= 0) ? Number(x.expense_total) : finSumNumbers(note);
    });
    var expTotal = cashExp + onlineExp;
    var dp = d.split('-'); var dotted = dp[2] + '/' + dp[1] + '/' + dp[0];
    var collTotal = cashColl + onlineColl;
    var net = collTotal - expTotal;
    // Expense-বার Collection-এর তুলনায় (০–১০০% বাঁধা); Collection ০ হলে ও
    // খরচ থাকলে পুরো লাল বার — Collection নিজে সবসময় পূর্ণ (নিজের ভিত্তি)।
    var ratio = collTotal > 0 ? Math.min(1, expTotal / collTotal) : (expTotal > 0 ? 1 : 0);
    modal(
      '<h2>' + m.esc(dotted + ' · ' + brSel) + '</h2>' +
      '<div style="text-align:center;margin:6px 0 18px">' +
        '<div class="tiny mut" style="text-transform:uppercase;letter-spacing:.04em;font-weight:800">NET (Collection − Expense)</div>' +
        '<div style="font-size:28px;font-weight:900;color:#0b2b59;margin-top:2px">' + m.money(net) + '</div>' +
      '</div>' +
      '<div class="row" style="margin-bottom:2px"><b class="tiny">Collection</b><b style="color:#0f7a3d">' + m.money(collTotal) + '</b></div>' +
      '<div class="progress"><div class="bar" style="width:100%;background:#16a36d"></div></div>' +
      '<div class="tiny mut" style="margin:4px 0 14px">Cash ' + m.money(cashColl) + '  ·  Online ' + m.money(onlineColl) + '</div>' +
      '<div class="row" style="margin-bottom:2px"><b class="tiny">Expense</b><b style="color:#b42318">' + m.money(expTotal) + '</b></div>' +
      '<div class="progress"><div class="bar" style="width:' + Math.round(ratio * 100) + '%;background:#d92d20"></div></div>' +
      '<div class="tiny mut" style="margin-top:4px">Cash ' + m.money(cashExp) + '  ·  Online ' + m.money(onlineExp) + '</div>'
    );
  }

  function branchOptions(sel) {
    return branches().map(function (b) {
      return '<option value="' + window.MOD.esc(b) + '"' + (b === sel ? ' selected' : '') + '>' + window.MOD.esc(b) + '</option>';
    }).join('');
  }

  // =====================================================================
  // 🆕 Ledger Sheet (02.08.2026, TK-নির্দেশে, ফোনের সাথে মিলিয়ে) — Google
  // Sheet-এর মতো গ্রিড। Expense-এর বাক্সে যা লিখুন সব সংখ্যা (কমা-সহ, লাইনে
  // এক বা একাধিক) খুঁজে যোগ করে। fin.collections-এর নতুন কলামে সেভ হয়;
  // পুরনো Add Collection/Add Expense/fin.expenses একদম ছোঁয়া হয়নি।
  // =====================================================================
  function finSumNumbers(text) {
    var re = /[0-9][0-9,]*/g, mt, total = 0;
    while ((mt = re.exec(text || '')) !== null) {
      var n = Number(mt[0].replace(/,/g, ''));
      if (!isNaN(n)) total += n;
    }
    return total;
  }

  // 🆕 (03.08.2026, TK-অনুমোদনে) — ফোনের Daily Ledger/Monthly Summary-র
  // গুগল-শিট-স্টাইল বক্স-টেবিল এখন কম্পিউটারেও, ঠিক ফোনের gridRow()/
  // renderGridTable()-এর (IncomeExpenseActivity.kt) একই রং/সারি-সাজানো
  // পুনর্ব্যবহার করে — নতুন CSS ক্লাস তৈরি হয়নি, সব ইনলাইন স্টাইল (ঠিক
  // finPill()/finLedgerLoad()-এর মতোই), তাই বাকি কোনো স্ক্রিনের ডিজাইন
  // ছোঁয়া হয়নি। ⛔ টাকার কোনো হিসাব/লজিক বদলায়নি — শুধু দেখানোর ধরন।
  function finGridRow(label, value, bold) {
    var bg = bold ? '#EAF6EE' : '#FFFFFF', fg = bold ? '#0A5C33' : '#222222';
    var fw = bold ? '700' : '400';
    return '<tr style="background:' + bg + '">' +
      '<td style="padding:8px 10px;border:1px solid #CFE9D8;color:' + fg + ';font-weight:' + fw + '">' +
      window.MOD.esc(label) + '</td>' +
      '<td style="padding:8px 10px;border:1px solid #CFE9D8;color:' + fg + ';font-weight:' + fw +
      ';text-align:right;white-space:nowrap">' + window.MOD.esc(value) + '</td></tr>';
  }
  function finGridTable(headTitle, headValue, rows) {
    if (!rows.length) return '<div class="mut">No data.</div>';
    var body = rows.map(function (r) { return finGridRow(r[0], r[1], r[2]); }).join('');
    return '<div style="overflow-x:auto"><table style="width:100%;min-width:320px;border-collapse:collapse;font-size:13px">' +
      '<tr style="background:#0A7C3F"><th style="padding:8px 10px;border:1px solid #CFE9D8;color:#FFFFFF;text-align:left">' +
      window.MOD.esc(headTitle) + '</th><th style="padding:8px 10px;border:1px solid #CFE9D8;color:#FFFFFF;text-align:right">' +
      window.MOD.esc(headValue) + '</th></tr>' + body + '</table></div>';
  }

  // 🔵🔒 খাতা-স্টাইল (TK-প্রুফ অনুমোদিত + TK-সিদ্ধান্ত 09.08.2026 "ব্রাঞ্চ ধরে আলাদা"):
  // ফোনের (IncomeExpenseActivity.kt) হুবহু একই সাজ কম্পিউটারেও — শিরোনাম "টাকার খাতা",
  // ব্রাঞ্চ বাছাই (All Branches = আগের মতো সব একসাথে), আগের বাকি (উপরে) ও অবশিষ্ট টাকা
  // (নিচে), খরচের সংখ্যায় চাপ → ভাঙা-হিসাব। ⛔ টাকার হিসাব/লজিক বদলায়নি — শুধু দেখানোর সাজ।
  async function finLedgerSheet() {
    var m = window.MOD;
    var month = m.todayIST().slice(0, 7);
    var finLockBr = finLockedBranch();   // 🔵 B617: ডাক্তার হলে ব্রাঞ্চ লক
    /* 🟢🔒 V398: মনে-রাখা ব্রাঞ্চ আগে থেকেই বসানো থাকে — বারবার বাছতে হয় না। */
    var __lsCur = finCurBranch();
    /* 🟢🔒 V628 (২৪.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট) — "ওটা তো হিসাবের খাতা...
       প্রতিটা ব্রাঞ্চের হিসাব থাকবে আলাদা, সমস্ত ব্রাঞ্চ একসাথে দেখানো যাবে
       না"। "All Branches" অপশন বাদ — সবসময় একটা নির্দিষ্ট ব্রাঞ্চ বাছতে হবে।
       পুরনো মনে-রাখা মান "All Branches" হলে এখানে ফাঁকা (বাছাই করতে বলা) ধরা হয়। */
    var brOpts = finLockBr ? branchOptions(finLockBr)
      : ((!__lsCur || __lsCur === 'All Branches') ? '<option value="" selected>Select Branch</option>' + branchOptions('')
         : branchOptions(__lsCur));
    var monthName = new Date(Number(month.slice(0,4)), Number(month.slice(5,7))-1, 1).toLocaleString('en-US',{month:'long',year:'numeric'});
    document.getElementById('finBody').innerHTML =
      '<div class="card" style="padding:10px 12px 68px"><div style="font-size:18px;font-weight:700;margin:0 0 8px">' + monthName + '</div>' +
      '<input id="lsMonth" type="hidden" value="' + month + '"><select id="lsBranchSel" style="display:none">' + brOpts + '</select>' +
      /* ➕🔒 V1174 (০৭.০৯.২০২৬, TK-নির্দেশ — ফোনের হুবহু যমজ) — TK, হাতের খাতার
         ছবিসহ: *"26/08/2026 এর হিসাব কি ভাবে তুলিবো"*।
         খাতায় শুধু সেই দিনগুলোই ওঠে যেদিনে ক্লাউডে কিছু আছে; একদম বাদ পড়া দিন
         যোগ করার কোনো পথই ছিল না। এখন এই বোতামে **ফাঁকা Ledger Entry** খোলে —
         ক্যালেন্ডার থেকে দিন বেছে Cash/Online/খরচ লিখে Save।
         ⛔ `finLedgerRowEdit('v399exp_<তারিখ>')` — প্রকল্পের **আগে থেকেই থাকা**
            পথ (V928); নতুন কোনো সেভ-নিয়ম বানানো হয়নি। id ক্লাউডে নেই বলে
            সবসময় **নতুন** সারি বসে, পুরনো কোনো দিন ওভাররাইট হয় না।
         ⛔ পুরনো-তারিখের অনুমতির নিয়ম আগেরটাই। ⛔ টাকার কোনো হিসাব ছোঁয়া হয়নি। */
      '<button class="ghost" style="width:100%;margin-bottom:8px;font-weight:700;color:#0A5C33;border:1px solid #CFE2D5;background:#EEF7F1" ' +
        'onclick="finAddNewDay()">\u2795 নতুন দিন</button>' +
      '<div id="lsOut" class="mut">Loading...</div>' +
      '<div style="position:fixed;left:0;right:0;bottom:0;z-index:20;background:#fff;padding:7px 14px;display:flex;gap:10px;border-top:1px solid #ddd">' +
      '<button class="ghost" style="flex:1;padding:9px" onclick="incomeExpense()">Back</button><button style="flex:1;padding:9px" onclick="finLedgerLoad()">Show</button></div></div>';
    finLedgerLoad();
  }

  /* ➕🔒 V1174 — "নতুন দিন": দেখানো মাসের ১ তারিখ থেকে ফাঁকা এন্ট্রি খোলে
     (ক্যালেন্ডার ওই মাসেই খোলে, অন্য মাস থেকে ঘুরে আসতে হয় না)। */
  function finAddNewDay() {
    var mEl = document.getElementById('lsMonth');
    var ym = (mEl && mEl.value) ? mEl.value : window.MOD.todayIST().slice(0, 7);
    finLedgerRowEdit('v399exp_' + ym + '-01');
  }
  window.finAddNewDay = finAddNewDay;

  // 🔵 TK-নির্দেশ (09.08.2026): এটা শুধু পুরনো এন্ট্রি দেখার খাতা — এক-চাপে ভুল করে
  // এডিটে ঢোকা ঠেকাতে কোনো দিন এডিট করতে সেই সারিতে **তিনবার** চাপতে হবে (১.২ সেকেন্ডে)।
  var __finRowTap = { id: null, n: 0, t: 0 };
  /* 🔴 V437 #19 — ফোনের ওই বার্তাটাই। ⛔ শুধু জানানো, কিছু বদলায় না। */
function finExpenseOnlyNote(){ finToast('এই দিনে শুধু খরচ আছে — Add Expense পর্দা থেকে দেখুন'); }
window.finExpenseOnlyNote = finExpenseOnlyNote;
function finRowTap(id) {
    var now = Date.now();
    if (__finRowTap.id !== id || (now - __finRowTap.t) > 1200) { __finRowTap.id = id; __finRowTap.n = 0; }
    __finRowTap.n++; __finRowTap.t = now;
    if (__finRowTap.n >= 3) { __finRowTap.n = 0; finLedgerRowEdit(id); }
  }

  /* =====================================================================
     🟢🔒 V630 (২৪.০৮.২০২৬, TK-নির্দেশ) — Sheet-এর Cash/Online ঘরে নিজস্ব
     ৩-চাপ, শুধু সেই একটা সংখ্যার জন্য ছোট quick-editor — পুরো সারির ফর্ম
     (Date/Branch/Cash/Online/Expense) খোলার দরকার নেই। ফোনের
     `IncomeExpenseActivity.quickFieldEditor()`-এর হুবহু যমজ।
     ⛔ একই পুরনো-তারিখ অনুমতি-নিয়ম (finIsMaster/finAskApproval) অক্ষত।
     ===================================================================== */
  var __finFieldTap = { id: null, field: null, n: 0, t: 0 };
  function finFieldTap(id, field) {
    var now = Date.now();
    if (__finFieldTap.id !== id || __finFieldTap.field !== field || (now - __finFieldTap.t) > 1200) {
      __finFieldTap = { id: id, field: field, n: 0, t: 0 };
    }
    __finFieldTap.n++; __finFieldTap.t = now;
    if (__finFieldTap.n >= 3) { __finFieldTap.n = 0; finQuickFieldEditor(id, field); }
  }
  window.finFieldTap = finFieldTap;

  function finQuickFieldEditor(id, field) {
    var m = window.MOD;
    var row = (window.__finRowMap || {})[id] || {};
    var label = field === 'cash' ? 'Cash' : 'Online';
    var current = Number(row[field] || 0);
    var esc = m.esc;
    var ov = document.createElement('div');
    ov.id = 'v630FieldOverlay';
    ov.setAttribute('style', 'position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:99999;display:flex;align-items:center;justify-content:center;padding:16px');
    ov.innerHTML = '<div style="background:#fff;border-radius:16px;max-width:380px;width:100%;box-shadow:0 10px 40px rgba(0,0,0,.3)">' +
      '<div style="background:#0B4F2A;color:#fff;padding:14px 18px;font-size:16px;font-weight:700;border-radius:16px 16px 0 0">' + label + ' — ' + esc(finSlash(row.entry_date || '')) + '</div>' +
      '<div style="padding:16px 18px"><input id="v630FieldAmt" class="input" type="number" value="' + (current > 0 ? current : '') + '" placeholder="Amount"></div>' +
      '<div style="padding:0 18px 16px;display:flex;gap:8px">' +
      '<button type="button" id="v630FieldCancel" class="ghost" style="flex:1">Cancel</button>' +
      '<button type="button" id="v630FieldSave" style="flex:1;background:#0A7C3F;color:#fff;border:none;border-radius:12px;padding:11px;font-size:15px;font-weight:700;cursor:pointer">Save</button>' +
      '</div></div>';
    document.body.appendChild(ov);
    function closeOv() { try { ov.remove(); } catch (e) { } }
    ov.addEventListener('click', function (ev) { if (ev.target === ov) closeOv(); });
    var cancelBtn = ov.querySelector('#v630FieldCancel'); if (cancelBtn) cancelBtn.addEventListener('click', closeOv);
    var saveBtn = ov.querySelector('#v630FieldSave');
    if (saveBtn) saveBtn.addEventListener('click', async function () {
      var v = Number((document.getElementById('v630FieldAmt') || {}).value || 0);
      var newRow = {
        id: row.id || m.uuid(),
        entry_date: row.entry_date || m.todayIST(),
        branch: row.branch || finCurBranch(),
        cash: field === 'cash' ? v : Number(row.cash || 0),
        online: field === 'online' ? v : Number(row.online || 0),
        expense_notes: row.expense_notes || '',
        expense_total: (row.expense_total != null && row.expense_total >= 0) ? row.expense_total : finSumNumbers(row.expense_notes || ''),
        created_by: finCreatedBy()
      };
      if (!finIsMaster() && !finIsToday(newRow.entry_date)) {
        closeOv();
        await finAskApproval(row.id ? 'EDIT_COLLECTION' : 'ADD_COLLECTION', newRow.branch, newRow.entry_date, row.id || null,
          { cash: newRow.cash, online: newRow.online });
        finLedgerLoad();
        return;
      }
      closeOv();
      await m.save('fin', 'collections', newRow);
      finLedgerLoad();
    });
  }
  window.finQuickFieldEditor = finQuickFieldEditor;

  async function finLedgerLoad() {
    var m = window.MOD, client = await sb();
    var monthEl = document.getElementById('lsMonth');
    var month = monthEl ? monthEl.value : m.todayIST().slice(0, 7);
    var brEl = document.getElementById('lsBranchSel');
    var branchSel = brEl ? brEl.value : 'All Branches';
    /* 🟢🔒 V398: এখানে যেটা বাছা হবে সেটাই সব সেকশনে মনে থাকবে। */
    if (!finLockedBranch()) finSetGlobalBranch(branchSel);
    if (!branchSel) {
      var __o0 = document.getElementById('lsOut');
      if (__o0) __o0.innerHTML = (typeof window.wlv1BranchAskCard === 'function')
        ? window.wlv1BranchAskCard() : '🏥 একটি Branch বাছুন।';
      return;
    }
    var start = month + '-01';
    var endD = new Date(Number(month.slice(0, 4)), Number(month.slice(5, 7)), 1);
    var end = endD.getFullYear() + '-' + String(endD.getMonth() + 1).padStart(2, '0') + '-01';
    var out = document.getElementById('lsOut');
    out.innerHTML = 'Loading...';
    var rows = [], prevRows = [], prevOk = true;
    try {
      var q = client.schema('fin').from('collections').select('*')
        .gte('entry_date', start).lt('entry_date', end).eq('ignored', false);
      if (branchSel !== 'All Branches') q = q.eq('branch', branchSel);
      var r = await q.order('entry_date', { ascending: true });
      rows = r.data || [];
    } catch (e) { out.innerHTML = 'Could not load (offline?).'; return; }
    /* 🔴🔒 V399 (16.08.2026, TK-রিপোর্ট ছবিসহ): টাকার খাতা এতদিন খরচ পড়ত **শুধু**
       `collections`-এর `expense_total` থেকে — `fin.expenses` টেবিল কখনো পড়ত না।
       কিন্তু "Add Expense"-এর খরচ ওই টেবিলেই জমা হয়। ফলে খরচের ঘর ফাঁকা দেখাত
       আর "অবশিষ্ট টাকা" আসলের চেয়ে বেশি দেখাত।
       ⛔ নতুন নিয়ম নয় — Monthly Summary (`finRunMonthly`) ও আজকের হিসাব
          (`finLoadToday`) আগে থেকেই দুটো উৎসই পড়ে; সেই প্রমাণিত নিয়মই এখানে।
       ⛔ ব্রাঞ্চ-ছাঁকনি ও `ignored=false` হুবহু একই। পড়া ব্যর্থ হলে বাড়তি খরচ ০। */
    var v399ExpByDate = {};
    /* 🟢🔒 V400 (16.08.2026, TK-অনুমোদিত মকআপ — "আমি যেন Edit করতে পারি / কখনো কমও
       হতে পারে কখনো বেশিও হতে পারে"): আগে শুধু `entry_date,amount` টানা হতো (যোগ
       করার জন্য)। এখন প্রতিটা খরচ আলাদা লাইনে দেখাতে ও বদলাতে হবে, তাই ওই একই
       সারিগুলোরই আরো কয়েকটা ঘর টানা হয়। ⛔ সারির **সংখ্যা** এক — একই ফিল্টার। */
    var v400ExpItems = {};
    try {
      var eq2 = client.schema('fin').from('expenses').select('id,entry_date,branch,category,paid_to,amount,mode')
        .gte('entry_date', start).lt('entry_date', end).eq('ignored', false);
      if (branchSel !== 'All Branches') eq2 = eq2.eq('branch', branchSel);
      var er2 = await eq2;
      (er2.data || []).forEach(function (x) {
        var d = String(x.entry_date || ''); if (!d) return;
        v399ExpByDate[d] = (v399ExpByDate[d] || 0) + Number(x.amount || 0);
        (v400ExpItems[d] = v400ExpItems[d] || []).push(x);
      });
    } catch (e) { }
    /* যে দিনে শুধু খরচ আছে অথচ collection সারি নেই, সেই দিনও দেখাতে হবে —
       নইলে ওই খরচ কোথাও দেখাত না। ⛔ ওই সারিতে id নেই, তাই ৩-চাপে এডিট খোলে না। */
    try {
      var seen = {}; rows.forEach(function (x) { seen[String(x.entry_date || '')] = 1; });
      Object.keys(v399ExpByDate).forEach(function (d) {
        if (!seen[d] && v399ExpByDate[d] > 0) {
          rows.push({ entry_date: d, cash: 0, online: 0, expense_notes: '', __v399ExpenseOnly: 1 });
          /* 🐞🔒 V1322 (TK-রিপোর্ট ছবিসহ, ১১.০৯.২০২৬) — আগে এখানে `seen[d]=1`
             বসানো হতো না, তাই নিচের V927 অটো-আয় লুপ একই তারিখের জন্য
             **দ্বিতীয়** একটা সারি বসিয়ে দিত (একই cash/online দুবার) — খাতার
             Total ও "অবশিষ্ট টাকা"-তে দ্বিগুণ যোগ হতো। ডেটাবেসের টাকা
             ছোঁয়া হয়নি, শুধু এই পর্দার যোগফল। */
          seen[d] = 1;
        }
      });
      /* 🟢🔒 V630 (২৪.০৮.২০২৬, TK-নির্দেশ) — চলতি মাস দেখলে, আজকের সারি সবসময়
         সবার নিচে (এখনো কোনো এন্ট্রি না থাকলেও) — নতুন দিন শুরু করতে আলাদা
         "আয়" পর্দায় যেতে হবে না, এই খালি সারিতেই সরাসরি Cash/Online বসানো
         যায় (৩-চাপে quick-edit)। ⛔ ফোনের `IncomeExpenseActivity.
         loadSheet()`-এর হুবহু একই নিয়ম। */
      var todayNow = m.todayIST();
      if (month === todayNow.slice(0, 7) && !seen[todayNow]) {
        rows.push({ entry_date: todayNow, cash: 0, online: 0, expense_notes: '', branch: branchSel });
      }
      /* 🟢🔒 V927 — অটো-আয়ের দিনগুলোও খাতায় দেখাতে হবে, নইলে যে দিনে কেউ
         হাতে কিছু লেখেনি সেই দিনটা খাতায় আসতই না। ⛔ ০১/০৯/২০২৬-এর আগে নয়,
         আর হাতে লেখা সারি থাকলে সেটাই থাকে। */
      try {
        if (typeof window.wlv1AutoIncomeForMonth === 'function') {
          var __am2 = window.wlv1AutoIncomeForMonth(month, branchSel) || {};
          Object.keys(__am2).forEach(function (d) {
            if (seen[d]) return;
            rows.push({ entry_date: d, cash: 0, online: 0, expense_notes: '', branch: branchSel, __v927Auto: 1 });
            seen[d] = 1;
          });
        }
      } catch (e) { }
      rows.sort(function (a2, b2) { return String(a2.entry_date || '') < String(b2.entry_date || '') ? -1 : 1; });
    } catch (e) { }
    /* 🟢🔒 V927 — খাতার সারিতে অটো-আয় বসানো। ⛔ যে সারিতে ক্লাউডের আসল id
       আছে (মানে কেউ হাতে লিখেছেন) সেটা কখনো ছোঁয়া হয় না। */
    var v927Auto = {};
    var v927ShowTag = false;
    try { v927ShowTag = (typeof window.isMaster === 'function') && window.isMaster(); } catch (e) { v927ShowTag = false; }
    try {
      if (typeof window.wlv1AutoIncomeForDay === 'function') {
        rows.forEach(function (r) {
          var d = String(r.entry_date || '');
          if (!d || r.id) return;                                  // হাতে লেখা সারি → ছোঁয়া হবে না
          if (Number(r.cash || 0) !== 0 || Number(r.online || 0) !== 0) return;
          var v = window.wlv1AutoIncomeForDay(d, branchSel);
          if (!v || (!v.cash && !v.online)) return;
          r.cash = v.cash; r.online = v.online; v927Auto[d] = 1;
        });
      }
    } catch (e) { }
    // 🔵 আগের বাকি (Previous Balance) = এই মাসের আগের সব দিনের (একই ব্রাঞ্চের)
    // নগদ+অনলাইন − খরচ। শুধু দরকারি ৪টা কলাম টানা (egress কম)। ফোনের হুবহু হিসাব।
    try {
      var pq = client.schema('fin').from('collections').select('cash,online,expense_total,expense_notes')
        .lt('entry_date', start).eq('ignored', false);
      if (branchSel !== 'All Branches') pq = pq.eq('branch', branchSel);
      var pr = await pq;
      prevRows = pr.data || [];
    } catch (e) { prevOk = false; }
    /* 🔴🔒 V399: আগের-বাকিতেও "Add Expense"-এর খরচ বাদ যাবে। */
    var v399PrevExp = 0;
    try {
      var pe = client.schema('fin').from('expenses').select('amount')
        .lt('entry_date', start).eq('ignored', false);
      if (branchSel !== 'All Branches') pe = pe.eq('branch', branchSel);
      var per = await pe;
      (per.data || []).forEach(function (x) { v399PrevExp += Number(x.amount || 0); });
    } catch (e) { prevOk = false; }
    var prevBal = 0;
    prevRows.forEach(function (row) {
      var note = row.expense_notes || '';
      var exp = (row.expense_total != null && row.expense_total >= 0) ? Number(row.expense_total) : finSumNumbers(note);
      prevBal += Number(row.cash || 0) + Number(row.online || 0) - exp;
    });
    prevBal -= v399PrevExp;
    window.__finExpMap = {};
    var cashTot = 0, onlineTot = 0, expTot = 0;
    var trs = rows.map(function (row) {
      var cash = Number(row.cash || 0), online = Number(row.online || 0);
      var note = row.expense_notes || '';
      /* 🔴🔒 V399: খাতার নিজের খরচ + "Add Expense"-এর খরচ — দুটোই। */
      var expSum = ((row.expense_total != null && row.expense_total >= 0) ? Number(row.expense_total) : finSumNumbers(note))
        + Number(v399ExpByDate[String(row.entry_date || '')] || 0);
      cashTot += cash; onlineTot += online; expTot += expSum;
      var dp = String(row.entry_date).split('-'); var dotted = dp[2] + '/' + dp[1] + '/' + dp[0];
      /* 🟢🔒 V927 — ট্যাগ শুধু মাস্টারের পর্দায় (TK: "অটো না হাতে ঠিক করা এটা
         মাস্টার ছাড়া কেউ দেখতে পাবে না")। `dotted` নিজে অপরিবর্তিত, তাই
         খরচের পপ-আপের শিরোনামে ট্যাগ যায় না। */
      var dottedCell = dotted;
      if (v927ShowTag && String(row.entry_date || '') >= (window.WLV1_AUTO_INCOME_FROM || '2026-09-01')) {
        if (v927Auto[String(row.entry_date || '')]) dottedCell += '<span style="display:inline-block;margin-left:6px;background:#EAF6EE;color:#0B8A3E;border:1px solid #CFE9D8;border-radius:6px;padding:1px 5px;font-size:8.5px;font-weight:800;vertical-align:middle">AUTO</span>';
        else if (row.id) dottedCell += '<span style="display:inline-block;margin-left:6px;background:#EAF2FE;color:#0B4FA8;border:1px solid #C9DDF7;border-radius:6px;padding:1px 5px;font-size:8.5px;font-weight:800;vertical-align:middle">হাতে ঠিক করা</span>';
      }
      /* 🔒 V399: শুধু-খরচের দিনে ক্লাউডে collection সারি নেই — তাই আলাদা একটা কী,
         আর ৩-চাপে এডিট খোলে না (নিচে দেখুন)। */
      var v399Only = !!row.__v399ExpenseOnly;
      var rid = m.esc(row.id || ('v399exp_' + String(row.entry_date || '')));
      // 🟢🔒 V630 (২৪.০৮.২০২৬) — কাঁচা row-টা মনে রাখা, যাতে finQuickFieldEditor
      // Cash/Online বদলানোর সময় বাকি ঘর (branch/expense_notes ইত্যাদি) না হারায়।
      window.__finRowMap = window.__finRowMap || {};
      if (!v399Only) window.__finRowMap[row.id || rid] = row;
      var expCell;
      if (expSum > 0 || note) {
        window.__finExpMap[row.id || ('v399exp_' + String(row.entry_date || ''))] = {
          dotted: dotted, note: note, total: expSum,
          /* 🟢 V400: খাতার নিজের খরচ আলাদা, আর ওই দিনের "Add Expense" খরচগুলো আলাদা —
             পপ-আপে প্রতিটা আলাদা লাইনে দেখাতে ও বদলাতে। */
          own: ((row.expense_total != null && row.expense_total >= 0) ? Number(row.expense_total) : finSumNumbers(note)),
          items: (v400ExpItems[String(row.entry_date || '')] || []),
          /* ➕ V1203 — "এই দিনে আরও খরচ" বোতামের জন্য দিন ও ব্রাঞ্চ। */
          addDate: String(row.entry_date || ''), addBranch: String(row.branch || branchSel || '')
        };
        expCell = '<td onclick="event.stopPropagation();finExpenseBreakdown(\'' + rid + '\')" style="padding:6px;text-align:right;color:#B42318;font-weight:700;cursor:pointer;border:1px solid #CFE9D8">' +
          (expSum > 0 ? m.money(expSum).replace('₹', '') : '-') + '</td>';
      } else if (v399Only) {
        expCell = '<td style="padding:6px;text-align:right;color:#B42318;border:1px solid #CFE9D8">-</td>';
      } else {
        // 🟢🔒 V630 (TK-নির্দেশ, "হ্যাঁ চাই") — খালি খরচ ঘরে (single) চাপলে নতুন
        // খরচ যোগ করার ফর্ম খোলে (date/branch প্রি-ফিল) — কিছু ওভাররাইট হয় না
        // (নতুন যোগ), তাই এখানে ৩-চাপের দরকার নেই।
        expCell = '<td onclick="event.stopPropagation();finAddExpense(\'' + m.esc(String(row.entry_date || '')) + '\',\'' + m.esc(String(row.branch || branchSel)) + '\')" style="padding:6px;text-align:right;color:#B42318;cursor:pointer;border:1px solid #CFE9D8">-</td>';
      }
      // 🟢🔒 V630 — Cash/Online ঘরে নিজস্ব ৩-চাপ (event.stopPropagation() দিয়ে
      // সারির নিজের finRowTap-এ পৌঁছাতে দেয় না), শুধু সেই একটা সংখ্যার জন্য
      // ছোট quick-editor। শুধু-খরচের সারিতে (v399Only) এটা চালু হয় না —
      // ক্লাউডে ওই সারির কোনো collections id-ই নেই।
      var cashCell = v399Only
        ? '<td style="padding:6px;text-align:right;color:#0A7C3F;border:1px solid #CFE9D8">-</td>'
        : '<td onclick="event.stopPropagation();finFieldTap(\'' + (row.id || rid) + '\',\'cash\')" style="padding:6px;text-align:right;color:#0A7C3F;cursor:pointer;border:1px solid #CFE9D8">' + m.money(cash).replace('₹', '') + '</td>';
      var onlineCell = v399Only
        ? '<td style="padding:6px;text-align:right;color:#0A7C3F;border:1px solid #CFE9D8">-</td>'
        : '<td onclick="event.stopPropagation();finFieldTap(\'' + (row.id || rid) + '\',\'online\')" style="padding:6px;text-align:right;color:#0A7C3F;cursor:pointer;border:1px solid #CFE9D8">' + m.money(online).replace('₹', '') + '</td>';
      return '<tr style="cursor:pointer;user-select:none;-webkit-user-select:none"' +
        /* 🔴 V437 #19 (নিজের অডিটে ধরা) — ফোনে শুধু-খরচের দিনে চাপলে একটা
           বার্তা ওঠে ("এই দিনে শুধু খরচ আছে — Add Expense পর্দা থেকে দেখুন");
           ওয়েবে চাপ **নিঃশব্দে হারিয়ে যেত**, কেউ বুঝতেন না কেন কিছু হচ্ছে না। */
        /* 💰🔒 V1173 (০৭.০৯.২০২৬, TK-নির্দেশ — ফোনের হুবহু যমজ) — TK: *"একটা
           পেমেন্ট ভুল করে ওঠেনি, আমি এখান থেকে করতে পারছি না কেন? আমি মাস্টার"*।
           শুধু-খরচের দিনে আগে শুধু বার্তা দেখাত; এখন ৩-চাপে **ফাঁকা নতুন এন্ট্রি**
           খোলে (তারিখ ও ব্রাঞ্চ বসানো — `finLedgerRowEdit`-এর V928 পথটাই এটা
           আগে থেকেই পারত, শুধু ৩-চাপের দরজাটা বন্ধ ছিল)।
           ⛔ খরচের সারি ছোঁয়া হয় না, তাই দুবার গোনার ঝুঁকি নেই।
           ⛔ পুরনো-তারিখের অনুমতির নিয়ম আগেরটাই। */
        ' onclick="finRowTap(\'' + rid + '\')"' + '>' +
        '<td style="padding:6px;font-weight:700;border:1px solid #CFE9D8">' + dottedCell + '</td>' +
        cashCell + onlineCell +
        expCell + '</tr>';
    }).join('');
    var headHtml = '<tr style="background:#0A7C3F;color:#fff">' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">Date</th>' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">Cash</th>' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">Online</th>' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">খরচ</th></tr>';
    var totHtml = '<tr style="background:#EAF6EE;font-weight:700">' +
      '<td style="padding:6px;border:1px solid #CFE9D8;color:#0A5C33">Total</td>' +
      '<td style="padding:6px;text-align:right;border:1px solid #CFE9D8;color:#0A5C33">' + m.money(cashTot).replace('₹', '') + '</td>' +
      '<td style="padding:6px;text-align:right;border:1px solid #CFE9D8;color:#0A5C33">' + m.money(onlineTot).replace('₹', '') + '</td>' +
      '<td style="padding:6px;text-align:right;border:1px solid #CFE9D8;color:#B42318">' + m.money(expTot).replace('₹', '') + '</td></tr>';
    var brLabel = branchSel === 'All Branches' ? '' : ' · ' + m.esc(branchSel);
    var remaining = prevBal + cashTot + onlineTot - expTot;
    // 🔴🔒 V460 (TK-নির্দেশ ১৮.০৮.২০২৬, Android-এ V453 হিসেবে করা হয়েছিল —
    // এখানে ওয়েবেও একই ফিক্স: "ব্রাঞ্চের নাম বারবার কেন, দুটো বড়-খোলা বার কেন
    // পাশাপাশি রাখা হচ্ছে না")। হেডারেই ব্রাঞ্চ-চিপ থাকে বলে brLabel আর বসে না;
    // দুটো সংখ্যা এখন একটাই ছোট পাশাপাশি সারিতে, টেবিলের নিচে একবারই।
    var balPair = '<div style="margin:8px 0 4px;display:flex;gap:8px">' +
      '<div style="flex:1;background:#EEFAF0;border:1px solid #CDE9D5;border-radius:10px;padding:10px 8px;text-align:center">' +
      '<div style="font-size:11.5px;font-weight:700;color:#0A5C33">Previous Balance</div>' +
      '<div style="font-size:15px;font-weight:800;color:#0A5C33;padding-top:2px">' + (prevOk ? m.money(prevBal) : '—') + '</div></div>' +
      '<div style="flex:1;background:#0B4F2A;border-radius:10px;padding:10px 8px;text-align:center">' +
      '<div style="font-size:11.5px;font-weight:700;color:#fff">অবশিষ্ট টাকা</div>' +
      '<div style="font-size:15px;font-weight:800;color:#fff;padding-top:2px">' + (prevOk ? m.money(remaining) : '—') + '</div></div>' +
      '</div>';
    // 🔴 B602 (TK-নির্দেশ, ১০.০৮): টেবিলের উপরের নির্দেশ-লাইন বাদ — প্রফেশনাল ভিউতে
    // ডেমি-লেখা থাকবে না। ⛔ তিনবার-চাপে এডিট আচরণ অটুট, শুধু দেখানো লেখাটা সরানো।
    out.innerHTML =
      '<div style="overflow-x:auto"><table style="width:100%;min-width:600px;border-collapse:collapse;font-size:12px">' +
      headHtml + trs + totHtml + '</table></div>' +
      /* 🔴 V430 — টাকার খাতার খালি-লেখা ফোনের হুবহু (IncomeExpenseActivity.kt:678) */
      (rows.length ? '' : '<div class="mut">No entries yet this month.</div>') +
      balPair;
  }

  // 🔵🔒 খরচের ভাঙা-হিসাব পপ-আপ (ফোনের showExpenseBreakdown-এর হুবহু নকল): TK যা লেখেন
  // নাম-টাকা কমা দিয়ে (Rupam-1348, CRP-2759) — কমা/লাইন ধরে ভেঙে দেখায়। কোনো টাকা
  // যোগ/বিয়োগ/সংরক্ষণ নয় — শুধু TK-র লেখা সাজিয়ে দেখানো।
  function finExpenseBreakdown(id) {
    var m = window.MOD;
    var info = (window.__finExpMap || {})[id];
    if (!info) return;
    var lines = [];
    (info.note || '').split(/[,\n;]/).forEach(function (seg) {
      seg = (seg || '').trim(); if (!seg) return;
      var mm = seg.match(/[0-9][0-9,]*(?:\.[0-9]+)?/g);
      if (!mm) return;
      var last = mm[mm.length - 1];
      var amt = Number(last.replace(/,/g, '')); if (isNaN(amt)) return;
      var idx = seg.lastIndexOf(last);
      var name = (seg.slice(0, idx) + seg.slice(idx + last.length)).replace(/[-:=·./()]/g, ' ').trim();
      if (!name) name = 'খরচ';
      lines.push({ name: name, amt: amt });
    });

    /* 🟢🔒 V400 (TK-অনুমোদিত মকআপ, 16.08.2026): আগে এটা একটা সাদামাটা `alert()` ছিল,
       তাই ভুল খরচ চোখে দেখা যেত কিন্তু ছোঁয়া যেত না। এখন প্রতিটা "Add Expense" খরচ
       আলাদা লাইনে — চাপলে বদলানো/মোছার পর্দা খোলে।
       ⛔ TK-নির্দেশ: খরচের **সব** সংখ্যা লাল (#B42318), মোট-ও।
       ⛔ কোনো টাকার হিসাব বদলায়নি — মোট = সারিতে যা দেখাচ্ছে সেই একই সংখ্যা। */
    var items = info.items || [];
    var own = (info.own != null) ? Number(info.own) : lines.reduce(function (a, b) { return a + b.amt; }, 0);
    var esc = m.esc;
    var h = '';
    /* 🔴 V413 (TK-নির্দেশ): নির্দেশ-লাইনটা তুলে দেওয়া হলো। */
    items.forEach(function (x) {
      var label = [x.category, x.paid_to, x.mode].filter(function (t) { return t; }).join(' · ') || 'খরচ';
      h += '<div class="v400ExpLine" data-id="' + esc(String(x.id || '')) + '" ' +
        'style="display:flex;align-items:center;gap:10px;border:1px solid #9FD3B4;background:#F2FAF5;border-radius:10px;padding:11px 12px;margin-bottom:8px;cursor:pointer">' +
        '<span style="flex:1;font-size:14.5px;color:#22312A">' + esc(label) + '   ✏️</span>' +
        '<span style="font-weight:700;color:#B42318;font-size:15.5px">' + m.money(Number(x.amount || 0)) + '</span></div>';
    });
    if (own > 0 || (info.note || '')) {
      if (items.length) {
        h += '<div style="font-size:12.5px;color:#6A5320;background:#FFF7E6;border:1px solid #F0D9A0;border-radius:10px;padding:10px 12px;margin-bottom:8px">' +
          'নিচের ' + m.money(own) + ' খাতার সারিতেই লেখা — বদলাতে হলে ওই সারিতে 3 বার চাপুন</div>';
      }
      var plain = lines.length ? lines : [{ name: (info.note || 'খাতার সারিতে লেখা খরচ'), amt: own }];
      plain.forEach(function (l) {
        h += '<div style="display:flex;align-items:center;gap:10px;border:1px solid #D9E2EC;background:#FCFEFD;border-radius:10px;padding:11px 12px;margin-bottom:8px">' +
          '<span style="flex:1;font-size:14.5px;color:#22312A">' + esc(l.name) + '</span>' +
          '<span style="font-weight:700;color:#B42318;font-size:15.5px">' + m.money(l.amt) + '</span></div>';
      });
    } else if (!items.length) {
      h += '<div style="font-size:15px;color:#22312A">এই দিনের খরচের কোনো বিবরণ লেখা নেই।</div>';
    }
    h += '<div style="border-top:1px dashed #E2B3AD;margin-top:6px;padding-top:10px;display:flex;font-weight:700;color:#B42318;font-size:16.5px">' +
      '<span style="flex:1">মোট খরচ</span><span>' + m.money(Number(info.total || 0)) + '</span></div>';

    /* 🟢🔒 V628 (২৪.০৮.২০২৬, TK-নির্দেশ) — Monthly Summary থেকে এখানে এলে
       (info.editRowId থাকলে) আর Master হলে — সরাসরি ওই দিনের Ledger এডিটর
       খোলার বোতাম। ⛔ নতুন কোনো এডিট-লজিক নয় — Ledger Sheet-এর নিজের
       `finLedgerRowEdit()` পুনর্ব্যবহার হচ্ছে। ব্রাঞ্চ এখন সবসময় নির্দিষ্ট
       (V628-এর "All Branches" অপসারণ) বলে কোন সারি এডিট হবে তা নিয়ে কোনো
       দ্বিধা নেই। */
    /* ➕🔒 V1203 (০৮.০৯.২০২৬, TK-রিপোর্ট, ফটো-প্রুফ পাশ) — TK: *"উক্ত দিনে আরও
       ব্যয় ছিল, সেই ব্যয়টা আমি এখন কেন লিখতে পারছি না এখানে … এই পর্দাতেই যেন
       আরো খরচা হলে আমি সেটা যোগ করতে পারি"*। কোডে মেপে দেখা গেল খরচের ঘর
       **ফাঁকা থাকলেই** কেবল নতুন খরচের ফর্ম খুলত (V630)। ⇒ এখন এখান থেকেও
       `finAddExpense()`-এর সেই একই প্রমাণিত পথ খোলে (দিন+ব্রাঞ্চ প্রি-ফিল)।
       ⛔ নতুন কোনো সেভ-লজিক নয়, কিছু ওভাররাইট হয় না — শুধু নতুন সারি যোগ।
       (ফোনের showExpenseBreakdown-এর হুবহু যমজ।) */
    if (info.addDate && info.addBranch) {
      h += '<button type="button" id="v400ExpAdd" style="width:100%;margin-top:12px;background:#fff;color:#0B4F2A;' +
        'border:2px solid #0B4F2A;border-radius:10px;padding:11px;font-size:14.5px;font-weight:700;cursor:pointer">➕ Add Expense on this day</button>';
    }
    if (info.editRowId && finIsMaster()) {
      h += '<button type="button" id="v400ExpEdit" style="width:100%;margin-top:12px;background:#0B4F2A;color:#fff;border:none;' +
        'border-radius:10px;padding:12px;font-size:14.5px;font-weight:700;cursor:pointer">✏️ Edit This Day</button>';
    }

    var ov = document.createElement('div');
    ov.id = 'v400ExpOverlay';
    ov.setAttribute('style', 'position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:99999;display:flex;align-items:center;justify-content:center;padding:16px');
    ov.innerHTML = '<div style="background:#fff;border-radius:16px;max-width:520px;width:100%;max-height:86vh;overflow:auto;box-shadow:0 10px 40px rgba(0,0,0,.3)">' +
      '<div style="background:#12A150;color:#fff;padding:14px 18px;font-size:18px;font-weight:700;border-radius:16px 16px 0 0">' + esc(info.dotted) + ' — খরচের বিবরণ</div>' +
      '<div style="padding:16px 18px">' + h + '</div>' +
      '<div style="padding:0 18px 16px;text-align:right">' +
      '<button type="button" id="v400ExpOk" style="background:#0A7C3F;color:#fff;border:none;border-radius:12px;padding:11px 30px;font-size:16px;font-weight:700;cursor:pointer">OK</button>' +
      '</div></div>';
    document.body.appendChild(ov);
    function closeOv() { try { ov.remove(); } catch (e) { } }
    ov.addEventListener('click', function (ev) { if (ev.target === ov) closeOv(); });
    var okBtn = ov.querySelector('#v400ExpOk');
    if (okBtn) okBtn.addEventListener('click', closeOv);
    // 🟢🔒 V628 — "✏️ Edit This Day" — পপ-আপ বন্ধ করে সরাসরি Ledger এডিটর।
    var addBtn = ov.querySelector('#v400ExpAdd');
    if (addBtn) addBtn.addEventListener('click', function () {
      closeOv();
      finAddExpense(info.addDate, info.addBranch);
    });
    var editBtn = ov.querySelector('#v400ExpEdit');
    if (editBtn) editBtn.addEventListener('click', function () {
      closeOv();
      finLedgerRowEdit(info.editRowId);
    });
    Array.prototype.forEach.call(ov.querySelectorAll('.v400ExpLine'), function (el) {
      el.addEventListener('click', function () {
        var eid = el.getAttribute('data-id');
        closeOv();
        if (eid) finExpenseEdit(eid);
      });
    });
  }

  /* 🟢🔒🆕 V400 — "Add Expense" দিয়ে লেখা খরচ বদলানো/মোছার পর্দা (ফোনের
     `openExpenseEditor`-এর হুবহু নকল)। এতদিন ওয়েবে ও ফোনে দুই জায়গাতেই এই খরচ
     শুধু **যোগ** করা যেত, বদলানো যেত না।
     ⛔ সেভ হয় সেই একই প্রমাণিত পথে (`MOD.save`, আসল `id` সমেত) — অফলাইনে সারিতে
        জমা থাকে, নেট এলে চলে যায়; নতুন `id` বানায় না বলে সারি দুইবার হয় না।
     ⛔ Delete = `ignored: true` — খাতার সারির Delete-এর (B675) হুবহু একই নিয়ম;
        হিসাব থেকে বাদ যায়, কিছুই চিরতরে মোছে না। */
  async function finExpenseEdit(id) {
    var m = window.MOD, client = await sb();
    var row = null;
    try { row = (await client.schema('fin').from('expenses').select('*').eq('id', id).maybeSingle()).data; } catch (e) { }
    if (!row) { if (typeof toast === 'function') toast('খরচটি পাওয়া গেল না — আবার দেখুন'); return; }
    __v400ExpRow = row;   // 🔒 আসল সারি ধরে রাখা (উপরের ব্যাখ্যা দেখুন)
    /* 📝🔒 V1203 — Add Expense-এর মতোই এখানেও একটাই লেখার ঘর। আগে Category
       তালিকা-ঘর ছিল; টাইপ-করা নতুন লেখা তালিকায় না থাকায় বদলাতে গেলে ঘরটা
       ফাঁকা দেখাত ও সেভ আটকে যেত — সেই ফাঁকটাও এতে বন্ধ। পুরনো সারিতে দুটো
       আলাদা লেখা থাকলে " · " দিয়ে জুড়ে দেখানো হয়, কিছু হারায় না। */
    var xJoin = [String(row.category || ''), String(row.paid_to || '')]
      .filter(function (t) { return t && t !== 'Other Expense'; })
      .filter(function (t, i, a) { return a.indexOf(t) === i; }).join(' · ');
    var modeOpts = ['Cash', 'Online'].map(function (x) {
      return '<option' + (x === row.mode ? ' selected' : '') + '>' + x + '</option>';
    }).join('');
    document.getElementById('finBody').innerHTML =
      '<div style="background:linear-gradient(135deg,#0B4F2A,#0B8A3E);color:#fff;border-radius:14px;padding:14px 16px;font-size:18px;font-weight:800;margin-bottom:12px">✏️ খরচ বদলান</div>' +
      '<div class="card">' +
      '<div class="finTwo"><div><label>Date</label><input id="xDate" class="input" type="date" value="' + m.esc(String(row.entry_date || '')) + '"></div>' +
      '<div><label>Branch</label><select id="xBranch" class="input">' + branchOptions(row.branch) + '</select></div></div>' +

      /* 🔴 TK-নির্দেশ: খরচের সব সংখ্যা লাল — তাই Amount ঘরের লেখাও লাল।
         `!important` লাগে, কারণ মোবাইল-ভিউয়ের `.input` নিয়মে (styles.css §৪)
         `color:#1A1A1A!important` বসানো আছে — নইলে লাল রংটা বসত না। */
      '<label>Amount</label><input id="xAmt" class="input" type="number" value="' + Number(row.amount || 0) + '" style="color:#B42318!important;font-weight:700!important">' +
      '<label>Mode</label><select id="xMode" class="input">' + modeOpts + '</select>' +
      '<label>Spent On (optional)</label><input id="xPaidTo" class="input" placeholder="Type here…" value="' + m.esc(xJoin) + '">' +
      '<div class="actions finActs">' +
      '<button onclick="finExpenseSave(\'' + m.esc(String(row.id)) + '\')">Save</button>' +
      '<button class="ghost" onclick="finLedgerSheet()">← Back</button>' +
      (finIsMaster() ? '<button class="ghost finDel" onclick="finExpenseDelete(\'' + m.esc(String(row.id)) + '\')">🗑️  Delete</button>' : '') +
      '</div>' +
      /* 🔴 V413 (TK-নির্দেশ): নির্দেশ-লাইনটা তুলে দেওয়া হলো। */
      '</div>';
  }

  /* 🔴🔒 V400 — গুরুত্বপূর্ণ নিরাপত্তা: ওয়েবের `MOD.save` **পুরো সারিটাই** upsert করে।
     তাই শুধু বদলানো ঘরগুলো পাঠালে বাকি ঘর (note · created_by · created_at) ফাঁকা
     হয়ে যেত। সেজন্য আসল সারিটা (`__v400ExpRow`) আগে ধরে রাখা হয়, তার উপরেই শুধু
     বদলানো ঘরগুলো বসানো হয় — বাকি সব অক্ষত থাকে। */
  var __v400ExpRow = null;

  function v400ExpFormRow(existingId) {
    var base = {};
    if (__v400ExpRow && String(__v400ExpRow.id) === String(existingId)) {
      Object.keys(__v400ExpRow).forEach(function (k) { base[k] = __v400ExpRow[k]; });
    }
    base.id = existingId;
    base.entry_date = document.getElementById('xDate').value;
    base.branch = document.getElementById('xBranch').value;
    /* 📝 V1203 — একটাই ঘর; ফাঁকা হলে "Other Expense"। */
    base.category = String(document.getElementById('xPaidTo').value || '').trim() || 'Other Expense';
    base.paid_to = '';
    base.amount = Number(document.getElementById('xAmt').value || 0);
    base.mode = document.getElementById('xMode').value;
    try { base.updated_at = new Date().toISOString(); } catch (e) { }
    return base;
  }

  async function finExpenseSave(existingId) {
    var m = window.MOD;
    if (!existingId) return;
    var row = v400ExpFormRow(existingId);
    if (!(row.amount > 0)) { if (typeof toast === 'function') toast('Amount লিখুন'); else alert('Amount লিখুন'); return; }
    /* 🟢🔒 V401: পুরনো তারিখের খরচ — মাস্টারের অনুমতি লাগবে। */
    if (!finIsMaster() && !finIsToday(row.entry_date)) {
      await finAskApproval('EDIT_EXPENSE', row.branch, row.entry_date, existingId,
        { category: row.category, paid_to: row.paid_to, amount: row.amount, mode: row.mode });
      finLedgerSheet(); return;
    }
    await m.save('fin', 'expenses', row);
    if (typeof toast === 'function') toast('খরচ বদলানো হয়েছে।');
    finLedgerSheet();
  }

  async function finExpenseDelete(existingId) {
    var m = window.MOD;
    if (!existingId) return;
    var row = v400ExpFormRow(existingId);
    if (!confirm('এই খরচটি মুছবেন?\n\n' + row.entry_date + ' · ' + row.branch + '\n' +
                 row.category + ' · ' + row.paid_to + '\nটাকা: ' + m.money(row.amount) +
                 '\n\nএটি হিসাব থেকে বাদ যাবে। চিরতরে মুছবে না — দরকারে ফেরানো যাবে।')) return;
    row.ignored = true;
    await m.save('fin', 'expenses', row);
    if (typeof toast === 'function') toast('খরচটি মুছে ফেলা হয়েছে।');
    finLedgerSheet();
  }

  async function finLedgerRowEdit(id) {
    var m = window.MOD, client = await sb();
    var row = null;
    if (id) { try { row = (await client.schema('fin').from('collections').select('*').eq('id', id).maybeSingle()).data; } catch (e) {} }
    /* 🔴🔒 V928 (৩১.০৮.২০২৬, নিজের যাচাইয়ে ধরা পড়া দোষ) — **ভুল তারিখ খুলত।**
       খাতায় যে দিনে ক্লাউডে কোনো সারি নেই (শুধু খরচের দিন · আজকের খালি সারি ·
       V927-এর অটো-আয়ের দিন), সেখানে id হয় `v399exp_<তারিখ>`। ক্লাউডে ওই id
       পাওয়া যায় না বলে নিচের fallback-টা **সবসময় আজকের তারিখ** বসিয়ে দিত —
       ফলে ০১/০৯-এর সারিতে ৩-চাপ দিলে ফর্মে আজকের তারিখ উঠত, আর Save করলে ভুল
       দিনে হিসাব বসত। এখন id-র ভিতরের তারিখটাই ধরা হয়।
       ⛔ আগে যেখানে id সত্যিই ক্লাউডের (হাতে লেখা সারি), সেখানে কিছুই বদলায়নি।
       ⛔ V927-এর অটো-আয়ের দিন হলে Cash/Online-ও আগে থেকে ভরা থাকে, যাতে TK
          শুধু দরকারি সংখ্যাটা বদলে Save করতে পারেন। */
    var v928Date = '', v928Branch = '';
    try {
      var mm = String(id || '').match(/^v399exp_(\d{4}-\d{2}-\d{2})$/);
      if (mm) {
        v928Date = mm[1];
        try { v928Branch = finLockedBranch() || finGlobalBranch() || ''; } catch (e) { v928Branch = ''; }
      }
    } catch (e) {}
    if (!row && v928Date) {
      var v928Auto = null;
      try {
        if (typeof window.wlv1AutoIncomeForDay === 'function')
          v928Auto = window.wlv1AutoIncomeForDay(v928Date, v928Branch || null);
      } catch (e) { v928Auto = null; }
      var v928Br = (v928Branch && v928Branch !== 'All Branches') ? v928Branch : (branches()[0] || '');
      row = { entry_date: v928Date, branch: v928Br,
              cash: (v928Auto ? Number(v928Auto.cash || 0) : 0),
              online: (v928Auto ? Number(v928Auto.online || 0) : 0),
              expense_notes: '' };
    }
    row = row || { entry_date: m.todayIST(), branch: branches()[0], cash: 0, online: 0, expense_notes: '' };
    document.getElementById('finBody').innerHTML = '<div class="card"><h2>Ledger Entry</h2>' +
      /* 🖥️🔵 B677 (১৫.০৮.২০২৬, TK-নির্দেশ): *"এটা তো ডেক্সটপের ভিউ — তাহলে সবগুলো
         উপর নিচে রাখার কি দরকার, পাশাপাশি রেখে তো জায়গাটা কমানো যায়।"*
         বড় পর্দায় Date+Branch এক সারিতে, Cash+Online এক সারিতে — উচ্চতা প্রায় অর্ধেক।
         ⛔ ছোট পর্দায় (৫৬০px-এর নিচে) নিজে থেকেই আগের মতো উপর-নিচে নেমে যায়। */
      '<div class="finTwo"><div><label>Date</label><input id="lsDate" class="input" type="date" value="' + m.esc(row.entry_date) + '"></div>' +
      '<div><label>Branch</label><select id="lsBranch" class="input">' + branchOptions(row.branch) + '</select></div></div>' +
      '<div class="finTwo"><div><label>Cash</label><input id="lsCash" class="input" type="number" value="' + Number(row.cash || 0) + '"></div>' +
      '<div><label>Online</label><input id="lsOnline" class="input" type="number" value="' + Number(row.online || 0) + '"></div></div>' +
      /* 💰🔒 V1211 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — একটাই বড় ঘরের বদলে
         **জোড়া-জোড়া ঘর** (আগে "কত টাকা", তারপর "কীসে"), সঙ্গে ＋ বোতাম।
         ⛔ সেভ হয় হুবহু আগের ধাঁচেই ("নাম টাকা, নাম টাকা") — তাই খরচের বিবরণ ·
            মাসের হিসাব · ছাপা সব আগের মতোই চলে। ফোনের হুবহু যমজ। */
      '<label>Expense / ব্যয়</label>' +
      '<div id="lsExpRows"></div>' +
      '<div id="lsExpAdd" style="margin-top:8px;border:2px dashed #D9A8A1;background:#FFF3F1;color:#B42318;' +
        'border-radius:11px;text-align:center;padding:10px;font-weight:800;cursor:pointer">＋  আরও একটা খরচ</div>' +
      '<div id="lsTotal" class="mut" style="margin-top:6px">Total Expense: ' + m.money(finSumNumbers(row.expense_notes || '')) + '</div>' +
      /* 🖥️🔵 B678 (১৫.০৮.২০২৬, TK-নির্দেশ): *"save Cancel Delete এগুলি তো পাশাপাশি
         রাখা যায় না কি?"* — তিনটেই এক সারিতে। ⛔ প্রতিটার কাজ · রং · নিয়ম অপরিবর্তিত;
         Delete আগের মতোই **শুধু আগে-সেভ-করা** সারিতে ওঠে। */
      '<div class="actions finActs"><button onclick="finLedgerSave(\'' + (row.id ? m.esc(row.id) : '') + '\')">Save</button>' +
      '<button class="ghost" onclick="finLedgerSheet()">Cancel</button>' +
      /* 🟢🔒 B675 (১৫.০৮.২০২৬, TK-অনুমোদিত): ফোনে আগে থেকেই ছিল
         (`IncomeExpenseActivity.kt:658-686`), কম্পিউটারে ছিল না — তাই ভুল হিসাব বসে
         গেলে কম্পিউটার থেকে শোধরানোর উপায় ছিল না।
         ⛔ ফোনের হুবহু একই নিয়ম: **মোছা হয় না**, শুধু `ignored = true` বসে (আড়াল) —
            দরকারে ফেরানো যায়, কোনো তথ্য নষ্ট হয় না।
         ⛔ শুধু **আগে-সেভ-করা** সারিতেই দেখা যায় (নতুন এন্ট্রিতে নয়)। */
      /* 🟢🔒 V401 (TK-নির্দেশ "মোছা শুধু মাস্টার"): মাস্টার ছাড়া এই বোতাম দেখানোই হয় না। */
      ((row.id && finIsMaster()) ? '<button class="ghost finDel" onclick="finLedgerDelete(\'' + m.esc(row.id) + '\')">' +
        '\uD83D\uDDD1\uFE0F  Delete</button>' : '') +
      '</div></div>';
    /* 💰 V1211 — ঘরগুলো জুড়ে সেই পুরনো ধাঁচের লেখা (ফোনের `expText()`-এর যমজ)। */
    window.finExpText = function () {
      var out = [];
      var cards = document.querySelectorAll('#lsExpRows .lsExpCard');
      Array.prototype.forEach.call(cards, function (c) {
        var a0 = String((c.querySelector('.lsExpAmt') || {}).value || '').trim().replace(/,/g, '');
        var w0 = String((c.querySelector('.lsExpWhat') || {}).value || '').trim().replace(/,/g, ' ');
        var n = Number(a0) || 0;
        if (!n && !w0) return;
        out.push((w0 || 'খরচ') + ' ' + Math.round(n));
      });
      return out.join(', ');
    };
    function lsRefreshTotal() {
      var t = document.getElementById('lsTotal');
      if (t) t.textContent = 'Total Expense: ' + m.money(finSumNumbers(window.finExpText()));
    }
    window.lsAddExpPair = function (amount, what) {
      var host = document.getElementById('lsExpRows'); if (!host) return;
      var d = document.createElement('div');
      d.className = 'lsExpCard';
      d.setAttribute('style', 'background:#fff;border:1px solid #EBD6D2;border-radius:12px;padding:4px 12px 12px;margin-top:8px');
      d.innerHTML = '<label>কত টাকা খরচ করলাম</label>' +
        '<input class="input lsExpAmt" type="number" value="' + (amount || '') + '" style="color:#B42318!important;font-weight:700!important">' +
        '<label>কীসে খরচ করলাম</label>' +
        '<input class="input lsExpWhat" value="' + m.esc(what || '') + '">';
      host.appendChild(d);
      Array.prototype.forEach.call(d.querySelectorAll('input'), function (el) {
        el.addEventListener('input', lsRefreshTotal);
      });
    };
    /* 💰 V1211 — পুরনো লেখা ভেঙে ঘরে বসানো (খরচের বিবরণ পপ-আপের হুবহু নিয়ম)। */
    (function () {
      var old = String(row.expense_notes || '').trim();
      var re = /[0-9][0-9,]*(?:\.[0-9]+)?/g;
      old.split(/[,\n;]/).forEach(function (seg) {
        seg = String(seg || '').trim(); if (!seg) return;
        var last = null, mm2;
        re.lastIndex = 0;
        while ((mm2 = re.exec(seg)) !== null) last = mm2;
        if (!last) return;
        var amt = Number(String(last[0]).replace(/,/g, '')); if (!isFinite(amt)) return;
        var name = (seg.slice(0, last.index) + seg.slice(last.index + last[0].length))
          .trim().replace(/^[-:=·./()\s]+|[-:=·./()\s]+$/g, '').trim();
        window.lsAddExpPair(String(Math.round(amt)), name || 'খরচ');
      });
      if (!document.querySelectorAll('#lsExpRows .lsExpCard').length) window.lsAddExpPair('', '');
      lsRefreshTotal();
    })();
    var addBtn = document.getElementById('lsExpAdd');
    if (addBtn) addBtn.addEventListener('click', function () { window.lsAddExpPair('', ''); lsRefreshTotal(); });
  }

  /* 🟢🔒 B675 — ফোনের `IncomeExpenseActivity` যা করে হুবহু তাই:
     লাল নিশ্চিতকরণে তারিখ · ব্রাঞ্চ · Cash · Online দেখিয়ে জিজ্ঞাসা, তারপর
     সারিটায় `ignored = true` বসে। ⛔ সারি মোছা হয় না — Ledger তালিকা এমনিতেই
     `ignored=false` ছাঁকনি দিয়ে পড়ে (finLedgerLoad), তাই ওটা আর দেখাবে না।
     ⛔ সেভের সেই একই প্রমাণিত পথ (`MOD.save`) ব্যবহার হয় — অফলাইন হলে নিজে থেকেই
        সারিতে জমা থাকে, নেট এলে চলে যায়। ⛔ নতুন কোনো ক্লাউড-পড়া নেই। */
  async function finLedgerDelete(existingId) {
    var m = window.MOD;
    if (!existingId) return;
    var d = document.getElementById('lsDate').value;
    var br = document.getElementById('lsBranch').value;
    var cash = Number(document.getElementById('lsCash').value || 0);
    var online = Number(document.getElementById('lsOnline').value || 0);
    if (!confirm('Delete this entry?\n\n' + d + ' · ' + br + '\nCash ' + m.money(cash) + ' · Online ' + m.money(online) +
                 '\n\nIt will be removed from the ledger. Nothing is permanently erased.')) return;
    var note = (typeof window.finExpText === 'function') ? window.finExpText() : '';   // 💰 V1211
    await m.save('fin', 'collections', {
      id: existingId,
      entry_date: d,
      branch: br,
      cash: cash,
      online: online,
      expense_notes: note,
      expense_total: finSumNumbers(note),
      created_by: finCreatedBy(),
      ignored: true
    });
    finLedgerSheet();
  }

  async function finLedgerSave(existingId) {
    var m = window.MOD;
    var note = (typeof window.finExpText === 'function') ? window.finExpText() : '';   // 💰 V1211
    // 🔴 বাগ-প্রতিরোধ (ফোনের সংস্করণে ধরা পড়া একই ঝুঁকি এখানেও এড়ানো হলো):
    // existing দিন এডিট করলে তার আসল id-ই পাঠাতে হবে, নইলে upsert নতুন সারি
    // বানিয়ে ফেলত — একই দিনের টাকা দুইবার গোনা হয়ে যেত।
    var row = {
      id: existingId || m.uuid(),
      entry_date: document.getElementById('lsDate').value,
      branch: document.getElementById('lsBranch').value,
      cash: Number(document.getElementById('lsCash').value || 0),
      online: Number(document.getElementById('lsOnline').value || 0),
      expense_notes: note,
      expense_total: finSumNumbers(note),
      created_by: finCreatedBy()
    };
    /* 🟢🔒 V401: পুরনো তারিখের খাতার সারি — মাস্টারের অনুমতি লাগবে। */
    if (!finIsMaster() && !finIsToday(row.entry_date)) {
      await finAskApproval('EDIT_COLLECTION', row.branch, row.entry_date, existingId || null,
        { cash: row.cash, online: row.online, expense_notes: row.expense_notes, expense_total: row.expense_total });
      finLedgerSheet(); return;
    }
    await m.save('fin', 'collections', row);
    finLedgerSheet();
  }

  // 🔵🔒 Add Collection নতুন সাজ (09.08.2026, TK-প্রুফ অনুমোদিত — ফোনের সাথে এক, নিয়ম ৯):
  // Date = type=date (নিজেই ক্যালেন্ডার খোলে, ISO জমা রাখে) · Branch একবার বাছলে থাকে ·
  // "Save & Add More" চাপলে জমা হয় কিন্তু পর্দাতেই থাকে (নগদ/অনলাইন/নোট ফাঁকা, তারিখ+ব্রাঞ্চ
  // থাকে) · "← Back" চাপলে তবেই ফেরে। ⛔ সেভ/হিসাব বদলায়নি — একই fin.collections। খালি
  // টাকায় ভুল-সারি ঠেকাতে ছোট সুরক্ষা (নগদ+অনলাইন দুটোই ০ হলে সেভ হয় না)।
  // 🔵🔒 (09.08.2026, TK-প্রুফ অনুমোদিত — ফোনের সাথে এক): উপরে গ্রেডিয়েন্ট হেডার (←+শিরোনাম বাঁয়ে,
  // ডানে ব্রাঞ্চ-চিপ 🏥 বাছুন + ↻)। ফর্মে আলাদা Branch/Note ঘর নেই। ব্রাঞ্চ প্রতিবার নিজে বাছতে হবে
  // (না বাছলে Save নয়); একবার বাছলে থেকে যায়। ↻ = পর্দা নতুন করে।
  async function finAddCollection() {
    var m = window.MOD;
    var finLockBr = finLockedBranch();   // 🔵 B617: ডাক্তার হলে নিজের ব্রাঞ্চ প্রি-সেট + লক
    var brOpts = finLockBr ? branchOptions(finLockBr) : ('<option value="">🏥 Select</option>' + branchOptions(''));
    var html =
      '<div style="background:linear-gradient(90deg,#123F86,#16A34A);color:#fff;border-radius:14px;padding:11px 14px;display:flex;align-items:center;gap:10px;margin-bottom:12px">' +
        '<span onclick="finDailyLedger()" style="cursor:pointer;font-weight:800;font-size:17px;flex:1">←  Add Collection</span>' +
        '<select id="cBranchSel"' + (finLockBr ? ' disabled' : '') + ' style="width:auto;margin:0;padding:6px 8px;border-radius:10px;background:rgba(255,255,255,.16);color:#fff;border:1px solid rgba(255,255,255,.55);font-weight:700">' + brOpts + '</select>' +
        '<span onclick="finAddCollection()" style="cursor:pointer;font-size:20px">↻</span>' +
      '</div>' +
      '<div class="card">' +
      '<label>Date</label><input id="cDate" class="input" type="date" value="' + m.todayIST() + '">' +
      '<label>Cash Collection</label><input id="cCash" class="input" type="number" value="">' +
      '<label>Online Collection</label><input id="cOnline" class="input" type="number" value="">' +
      /* 🔵 V388 (TK-নিয়ম): উপরের পটিতেই "←  Add …" তীর আছে — নিচে দ্বিতীয় তীর নয়। */
      '<div class="actions">' +
      '<button onclick="finSaveCollection()">Save &amp; Add More</button></div></div>';
    document.getElementById('finBody').innerHTML = html;
  }

  /* 🔴🔒 V418 (TK-রিপোর্ট, ১৭.০৮.২০২৬ — "এত ডুপ্লিকেট কেন হবে"):
     ফোনে ১৩/০৩/২০২৬-এর একই এন্ট্রি তিনবার বসে গিয়েছিল, তিনটেই এক সেকেন্ডের
     মধ্যে — Save-এ পরপর চাপ পড়েছিল। ওয়েবেও ঠিক একই ফাঁক ছিল, তাই এখানেও
     একই তালা বসানো হলো: একটা সেভ শেষ না হওয়া পর্যন্ত দ্বিতীয়টা শুরু হয় না।
     ⛔ প্রতিটা বেরোনোর পথে তালা খোলে (finally), তাই বোতাম কখনো মরে থাকে না।
     ⛔ সেভ/হিসাবের নিয়ম এক অক্ষরও বদলায়নি। */
  var finSaveBusy = false;

  async function finSaveCollection() {
    if (finSaveBusy) return;
    finSaveBusy = true;
    try { await finSaveCollectionInner(); } finally { finSaveBusy = false; }
  }
  async function finSaveCollectionInner() {
    var m = window.MOD;
    var br = document.getElementById('cBranchSel').value;
    if (!br) {
      if (typeof toast === 'function') toast('উপরে ডানে ব্রাঞ্চ বাছুন');
      else alert('ব্রাঞ্চ বাছুন');
      return;
    }
    var cashV = Number(document.getElementById('cCash').value || 0);
    var onlineV = Number(document.getElementById('cOnline').value || 0);
    if (cashV <= 0 && onlineV <= 0) {
      if (typeof toast === 'function') toast('নগদ বা অনলাইন — অন্তত একটা লিখুন');
      else alert('নগদ বা অনলাইন — অন্তত একটা লিখুন');
      return;
    }
    var row = {
      id: m.uuid(),
      entry_date: document.getElementById('cDate').value,
      branch: br,
      cash: cashV,
      online: onlineV,
      note: '',
      created_by: finCreatedBy()
    };
    /* 🟢🔒 V401: পুরনো তারিখে নতুন আয় — মাস্টারের অনুমতি লাগবে। */
    if (!finIsMaster() && !finIsToday(row.entry_date)) {
      await finAskApproval('ADD_COLLECTION', row.branch, row.entry_date, null,
        { cash: row.cash, online: row.online });
      document.getElementById('cCash').value = ''; document.getElementById('cOnline').value = '';
      return;
    }
    var __r = await m.save('fin', 'collections', row);
    /* 🔴🔒 V418 (TK-নির্দেশ): ডেটাবেস নিজেই এখন একই দিনের · একই ব্রাঞ্চের · হুবহু
       একই Cash+Online দ্বিতীয়বার বসতে দেয় না। আটকালে **সৎ কথা** বলা হয় —
       "Saved" বলা হয় না, আর ঘরের অঙ্কও মুছে যায় না (দেখে শুধরে নিতে পারবেন)। */
    if (__r && __r.duplicate) {
      if (typeof toast === 'function') toast('এই দিনে এই অঙ্ক আগেই জমা আছে — আর যোগ হয়নি');
      return;
    }
    // 🔵 পর্দাতেই থাকে; টাকার ঘর ফাঁকা (তারিখ+ব্রাঞ্চ থাকে) — আরেকটা যোগ করা যায়।
    document.getElementById('cCash').value = '';
    document.getElementById('cOnline').value = '';
    if (typeof toast === 'function') toast('Saved — আরেকটা যোগ করতে পারেন');
  }

  // 🔵🔒 (09.08.2026, TK-প্রুফ অনুমোদিত — ফোনের সাথে এক): উপরে লাল হেডার (←+শিরোনাম বাঁয়ে, ডানে
  // ব্রাঞ্চ-চিপ 🏥 Select + ↻)। ফর্মে Branch/Note ঘর নেই। ব্রাঞ্চ ও Category প্রতিবার বাছতে হবে।
  async function finAddExpense(prefillDate, prefillBranch) {
    var m = window.MOD;
    var finLockBr = finLockedBranch();   // 🔵 B617: ডাক্তার হলে নিজের ব্রাঞ্চ প্রি-সেট + লক
    // 🟢🔒 V630 (২৪.০৮.২০২৬) — Sheet-এর খালি খরচ-ঘর থেকে এলে date/branch প্রি-ফিল।
    var fixedBr = finLockBr || prefillBranch || '';
    var brOpts = fixedBr ? branchOptions(fixedBr) : ('<option value="">🏥 Select</option>' + branchOptions(''));
    var backFn = prefillDate ? "finLedgerSheet()" : "finDailyLedger()";
    var html =
      '<div style="background:linear-gradient(90deg,#7A1212,#C0271B);color:#fff;border-radius:14px;padding:11px 14px;display:flex;align-items:center;gap:10px;margin-bottom:12px">' +
        '<span onclick="' + backFn + '" style="cursor:pointer;font-weight:800;font-size:17px;flex:1">←  Add Expense — ব্যয়</span>' +
        '<select id="eBranchSel"' + (fixedBr ? ' disabled' : '') + ' style="width:auto;margin:0;padding:6px 8px;border-radius:10px;background:rgba(255,255,255,.16);color:#fff;border:1px solid rgba(255,255,255,.55);font-weight:700">' + brOpts + '</select>' +
        '<span onclick="finAddExpense()" style="cursor:pointer;font-size:20px">↻</span>' +
      '</div>' +
      '<div class="card">' +
      /* 📝🔒 V1203 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — Category-র তালিকা-ঘর ও
         Paid To মিলে **একটাই লেখার ঘর** ("Spent On", ঐচ্ছিক), আর ক্রম:
         Date → Amount → Mode → Spent On। ফোনের addExpense()-এর হুবহু যমজ।
         🔬 `category` শুধু লেখা দেখানোর কাজে লাগে — কোনো যোগফল ওটা ধরে হয় না। */
      '<label>Date</label><input id="eDate" class="input" type="date" value="' + (prefillDate || m.todayIST()) + '">' +
      '<label>Amount</label><input id="eAmt" class="input" type="number" value="">' +
      '<label>Mode</label><select id="eMode" class="input"><option>Cash</option><option>Online</option></select>' +
      '<label>Spent On (optional)</label><input id="ePaidTo" class="input" placeholder="Type here…">' +
      /* 🔵 V388 (TK-নিয়ম): উপরের পটিতেই "←  Add …" তীর আছে — নিচে দ্বিতীয় তীর নয়। */
      '<div class="actions">' +
      '<button onclick="finSaveExpense()">Save</button></div></div>';
    document.getElementById('finBody').innerHTML = html;
  }

  async function finSaveExpense() {
    if (finSaveBusy) return;                 /* 🔴🔒 V418 — উপরের একই তালা */
    finSaveBusy = true;
    try { await finSaveExpenseInner(); } finally { finSaveBusy = false; }
  }
  async function finSaveExpenseInner() {
    var m = window.MOD;
    var br = document.getElementById('eBranchSel').value;
    if (!br) { if (typeof toast === 'function') toast('উপরে ডানে ব্রাঞ্চ বাছুন'); else alert('ব্রাঞ্চ বাছুন'); return; }
    // 🔵 খালি (০) Amount-এ সেভ নয় (Add More-এ ভুল ফাঁকা সারি ঠেকাতে) — ফোনের সাথে এক।
    var amtV = Number(document.getElementById('eAmt').value || 0);
    if (amtV <= 0) {
      if (typeof toast === 'function') toast('Amount লিখুন'); else alert('Amount লিখুন');
      return;
    }
    /* 📝 V1203 — ঘরটা **ঐচ্ছিক**, ফাঁকা থাকলেও সেভ আটকায় না; ফাঁকা হলে আগের
       মতোই "Other Expense" লেখা হয়, যাতে পুরনো সারির সঙ্গে চেহারা মেলে। */
    var typed = String(document.getElementById('ePaidTo').value || '').trim();
    var row = {
      id: m.uuid(),
      entry_date: document.getElementById('eDate').value,
      branch: br,
      category: typed || 'Other Expense',
      paid_to: '',
      amount: amtV,
      mode: document.getElementById('eMode').value,
      note: '',
      created_by: finCreatedBy()
    };
    /* 🟢🔒 V401: পুরনো তারিখে নতুন খরচ — মাস্টারের অনুমতি লাগবে। */
    if (!finIsMaster() && !finIsToday(row.entry_date)) {
      await finAskApproval('ADD_EXPENSE', row.branch, row.entry_date, null,
        { category: row.category, paid_to: row.paid_to, amount: row.amount, mode: row.mode });
      document.getElementById('eAmt').value = ''; document.getElementById('ePaidTo').value = '';
      return;
    }
    // Soft duplicate guard: warn on identical recent entry, never hard-block.
    var client = await sb();
    try {
      var dup = await client.schema('fin').from('expenses').select('id')
        .eq('entry_date', row.entry_date).eq('branch', row.branch).eq('category', row.category)
        .eq('paid_to', row.paid_to).eq('amount', row.amount).eq('mode', row.mode).limit(1);
      if (dup.data && dup.data.length) {
        if (!confirm('This looks identical to an entry already saved — add anyway?')) return;
      }
    } catch (e) {}
    await m.save('fin', 'expenses', row);
    if (typeof toast === 'function') toast('Saved');
    incomeExpense();
  }

  async function finDailyLedger() {
    var m = window.MOD, client = await sb();
    var date = m.todayIST();
    var body = document.getElementById('finBody');
    if (body) body.innerHTML = '<div class="card mut">Loading ' + date + ' ...</div>';
    var coll = [], exp = [], loadFailed = false;
    try {
      var rc = await client.schema('fin').from('collections').select('*').eq('entry_date', date).eq('ignored', false);
      var re = await client.schema('fin').from('expenses').select('*').eq('entry_date', date).eq('ignored', false);
      if ((rc && rc.error) || (re && re.error)) loadFailed = true;   // 🔵 DB error = failure (throw করে না)
      coll = (rc.data || []); exp = (re.data || []);
    } catch (e) { loadFailed = true; }
    // 🔵 TK-ORDER (07.08.2026): পড়া ব্যর্থ হলে আর ₹0 টোটাল দেখাব না (আগে দেখাত →
    // মনে হত দিনে কোনো টাকা/খরচ নেই)। ব্যর্থে স্পষ্ট বার্তা, ₹0 নয়।
    if (loadFailed) { if (body) body.innerHTML = '<div class="card mut">Could not load — please try again shortly</div>'; return; }
    var cash = coll.reduce(function (s, x) { return s + Number(x.cash || 0); }, 0);
    var online = coll.reduce(function (s, x) { return s + Number(x.online || 0); }, 0);
    // 🎨 (03.08.2026) — এখন ঠিক ফোনের dailyLedger()-এর একই সারি-ক্রম:
    // Cash/Online/Total Collection, তারপর প্রতিটা Expense এন্ট্রি, তারপর
    // Ledger Sheet-এ লেখা প্রতিটা [Sheet] এন্ট্রি, শেষে Total Expense —
    // টাকার হিসাব এক অক্ষরও বদলায়নি, শুধু বক্স-টেবিলে দেখানো হচ্ছে।
    var rows = [];
    rows.push(['Cash Collection', m.money(cash), false]);
    rows.push(['Online Collection', m.money(online), false]);
    rows.push(['Total Collection', m.money(cash + online), true]);
    var expTotal = 0;
    exp.forEach(function (x) {
      var amt = Number(x.amount || 0); expTotal += amt;
      rows.push([(x.category || '') + ' · ' + (x.paid_to || '') + ' · ' + (x.mode || ''), m.money(amt), false]);
    });
    var sheetExp = 0;
    coll.forEach(function (x) {
      var note = x.expense_notes || '';
      if (note) {
        var et = (x.expense_total != null && x.expense_total >= 0) ? Number(x.expense_total) : finSumNumbers(note);
        sheetExp += et; expTotal += et;
        rows.push(['[Sheet] ' + note.replace(/\n/g, ', '), m.money(et), false]);
      }
    });
    rows.push(['Total Expense', m.money(expTotal), true]);
    var html = '<div class="card"><h2>⏰ Daily Ledger</h2><div class="mut" style="margin-bottom:8px">Date: ' +
      m.esc(date) + '</div>' + finGridTable('Item', 'Amount', rows) + '</div>';
    if (body) body.innerHTML = html;
  }

  /* 📈🔒 V1217 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"Option
     থাকবে না — উপরে ডান দিকে LG থ্রি ডট, তার মধ্যে WhatsApp share · pdf Download ·
     print"* · *"Show থাকবে না — মাস সিলেক্ট করলে Show হতে হবে"* · *"2026-09-এর বদলে
     মাসের নাম, চাপ দিলে মাসের লিস্ট"*। ফোনের `monthly()`-র হুবহু যমজ।
     ⛔ ভিতরে মাসটা আগের মতোই `yyyy-MM` — লুকোনো `#mMonth` ঘরেই থাকে, তাই
        `finRunMonthly()` এক অক্ষরও বদলায়নি; কোনো হিসাব/ছাঁকনি/টাকার অঙ্ক নয়।
     ⛔ V412-এর সুরক্ষা অক্ষত — মাস/ব্রাঞ্চ বদলালেই সঙ্গে সঙ্গে নতুন হিসাব বসে,
        পুরনো ব্রাঞ্চের টাকা পর্দায় বসে থাকে না।
     ⛔ 📅 ইমোজি কোথাও বসানো হয়নি (TK-র চিরস্থায়ী নিষেধ, সারি ২১৩)। */
  var FIN_MONTH_NAMES = ['January','February','March','April','May','June',
    'July','August','September','October','November','December'];
  function finMonthLabel(ym) {
    try { var pp = String(ym).split('-'); return FIN_MONTH_NAMES[Number(pp[1]) - 1] + ' ' + pp[0]; }
    catch (e) { return ym; }
  }

  async function finMonthly() {
    var m = window.MOD;
    var month = m.todayIST().slice(0, 7);
    /* 🟢🔒 V628 (২৪.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট) — "All Branches" অপশন বাদ,
       হিসাবের খাতায় ব্রাঞ্চ মিশবে না। সবসময় একটা নির্দিষ্ট ব্রাঞ্চ বাছতে হবে। */
    /* 📏 V1217 — হেডার দু-সারির, ফোনের হুবহু যমজ: উপরে শিরোনাম ও ডানে ⋮,
       নিচে ডানদিকে মাস ও ব্রাঞ্চ। মাসের পুরো নাম বসায় এক সারিতে আর আঁটে না। */
    var html = '<div class="card">' +
      '<div style="display:flex;gap:8px;align-items:center">' +
      '<h2 style="flex:1;margin:0">📈 Monthly Summary</h2>' +
      '<span onclick="finMonthlyOptions()" style="cursor:pointer;font-size:20px;font-weight:800;color:#0A5C33;padding:0 6px">&#8942;</span>' +
      '</div>' +
      '<div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;justify-content:flex-end;margin-top:6px">' +
      '<span id="mMonthBtn" onclick="finMonthPick()" style="cursor:pointer;font-weight:800;' +
      'color:#0A5C33;background:#F1F6F3;border:1px solid #D6E4DC;border-radius:12px;padding:6px 10px">' +
      m.esc(finMonthLabel(month)) + '  &#9662;</span>' +
      /* 🟢🔒 V398: মনে-রাখা ব্রাঞ্চ আগে থেকেই বসানো। */
      '<select id="mBranch" class="input" style="width:auto;flex:0 1 auto">' +
      ((!finCurBranch() || finCurBranch() === 'All Branches') ? '<option value="" selected>Select Branch</option>' + branchOptions('')
       : branchOptions(finCurBranch())) + '</select>' +
      '</div>' +
      '<input id="mMonth" type="hidden" value="' + month + '">' +
      '<div style="position:relative">' +
      '<div id="finMonthlyMenu" style="display:none;position:absolute;top:4px;right:0;z-index:40;' +
      'background:#fff;border:1px solid #E3ECE6;border-radius:12px;box-shadow:0 6px 24px rgba(0,0,0,.14);min-width:210px;overflow:hidden">' +
      '<div onclick="finMonthlyMenuPick(1)" style="padding:12px 14px;cursor:pointer;border-bottom:1px solid #EEF3F0">📤 WhatsApp-এ শেয়ার</div>' +
      '<div onclick="finMonthlyMenuPick(2)" style="padding:12px 14px;cursor:pointer;border-bottom:1px solid #EEF3F0">📄 PDF Download</div>' +
      '<div onclick="finMonthlyMenuPick(3)" style="padding:12px 14px;cursor:pointer">🖨️ Print</div>' +
      '</div></div>' +
      '<div id="mOut" class="mut"></div></div>';
    document.getElementById('finBody').innerHTML = html;
    try {
      var __b = document.getElementById('mBranch');
      if (__b) __b.onchange = function () { try { finRunMonthly(); } catch (_e) {} };
    } catch (_e) {}
    try { finRunMonthly(); } catch (_e) {}
  }

  /* মাসের তালিকা — শেষ ২৪ মাস, ফোনের `monthly()`-র হুবহু একই। */
  function finMonthPick() {
    var m = window.MOD, out = [], d = new Date();
    for (var k = 0; k < 24; k++) {
      var ym = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
      out.push(ym);
      d.setMonth(d.getMonth() - 1);
    }
    var html = out.map(function (ym) {
      return '<button class="menuBtn" onclick="closeModal();finMonthSet(\'' + ym + '\')"><b>' +
        m.esc(finMonthLabel(ym)) + '</b></button>';
    }).join('');
    modal('<h2>Select Month</h2><div class="grid menuGrid">' + html + '</div>');
  }
  function finMonthSet(ym) {
    var inp = document.getElementById('mMonth'); if (inp) inp.value = ym;
    var btn = document.getElementById('mMonthBtn');
    if (btn) btn.innerHTML = window.MOD.esc(finMonthLabel(ym)) + '  &#9662;';
    try { finRunMonthly(); } catch (_e) {}
  }
  window.finMonthPick = finMonthPick;
  window.finMonthSet = finMonthSet;

  // 🔵🔒 Monthly Summary — TK-অনুমোদিত প্রুফ (09.08.2026): টাকার খাতার হুবহু একই
  // খাতা-ডিজাইন (finLedgerLoad-এর মতোই)। উপরে Previous Balance, দিন-ধরে
  // Date/Cash/Online/খরচ বক্স-টেবিল, খরচে চাপ দিলে ভাঙা-হিসাব, নিচে অবশিষ্ট টাকা।
  // ⛔ টাকার হিসাব আগের summarize()-এর সাথে হুবহু এক — আয় = নগদ+অনলাইন; খরচ =
  // Add-Expense এন্ট্রি + খাতায় লেখা খরচ; ব্যালেন্স = আয় − খরচ।
  async function finRunMonthly() {
    var m = window.MOD, client = await sb();
    var month = document.getElementById('mMonth').value;   // YYYY-MM
    var branch = document.getElementById('mBranch').value;
    /* 🟢🔒 V398: এখানে যেটা বাছা হবে সেটাই সব সেকশনে মনে থাকবে। */
    if (!finLockedBranch()) finSetGlobalBranch(branch === '__all' ? 'All Branches' : branch);
    if (!branch) {
      var __m0 = document.getElementById('mOut');
      if (__m0) __m0.innerHTML = (typeof window.wlv1BranchAskCard === 'function')
        ? window.wlv1BranchAskCard() : '🏥 একটি Branch বাছুন।';
      return;
    }
    var start = month + '-01';
    var endD = new Date(Number(month.slice(0, 4)), Number(month.slice(5, 7)), 1); // first of next month
    var end = endD.getFullYear() + '-' + String(endD.getMonth() + 1).padStart(2, '0') + '-01';
    var out = document.getElementById('mOut');
    out.innerHTML = 'Loading...';
    var coll = [], exp = [], prevColl = [], prevExp = [], prevOk = true;
    try {
      var qc = client.schema('fin').from('collections').select('*').gte('entry_date', start).lt('entry_date', end).eq('ignored', false);
      var qe = client.schema('fin').from('expenses').select('*').gte('entry_date', start).lt('entry_date', end).eq('ignored', false);
      if (branch !== '__all') { qc = qc.eq('branch', branch); qe = qe.eq('branch', branch); }
      var __rc = await qc.order('entry_date', { ascending: true }), __re = await qe.order('entry_date', { ascending: true });
      // 🔵 TK-ORDER (07.08.2026): DB error-এও throw হয় না — {error} যাচাই না করলে
      // ব্যর্থ পড়া খালি হয়ে মাসের আয়/ব্যয় ₹0/কম দেখাত। এখন error থাকলে সৎ বার্তা।
      if ((__rc && __rc.error) || (__re && __re.error)) { out.innerHTML = 'Could not load — একটু পরে আবার দেখুন'; return; }
      coll = __rc.data || []; exp = __re.data || [];
    } catch (e) { out.innerHTML = 'Could not load (offline?).'; return; }
    // 🔵 আগের বাকি (Previous Balance) = এই মাসের আগের সব দিনের আয় − খরচ (দুই উৎস)।
    try {
      var pqc = client.schema('fin').from('collections').select('cash,online,expense_total,expense_notes').lt('entry_date', start).eq('ignored', false);
      var pqe = client.schema('fin').from('expenses').select('amount').lt('entry_date', start).eq('ignored', false);
      if (branch !== '__all') { pqc = pqc.eq('branch', branch); pqe = pqe.eq('branch', branch); }
      var __pc = await pqc, __pe = await pqe;
      if ((__pc && __pc.error) || (__pe && __pe.error)) prevOk = false;
      prevColl = (__pc && __pc.data) || []; prevExp = (__pe && __pe.data) || [];
    } catch (e) { prevOk = false; }
    var prevBal = 0;
    prevColl.forEach(function (row) {
      var note = row.expense_notes || '';
      var e = (row.expense_total != null && row.expense_total >= 0) ? Number(row.expense_total) : finSumNumbers(note);
      prevBal += Number(row.cash || 0) + Number(row.online || 0) - e;
    });
    prevExp.forEach(function (row) { prevBal -= Number(row.amount || 0); });

    // দিন-ধরে জড়ো: প্রতিটি দিনের নগদ/অনলাইন + খরচ (খাতার খরচ + Add-Expense এন্ট্রি)।
    var days = {};
    /* 🟠🔒 V960 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ — ফোনের যমজ) — TK:
       *"এখানে এডিট বা ডিলিটের কোন অপশনই বা নেই কেন?"* এই পর্দার খরচ-পপ-আপে
       খরচের **আসল সারিগুলো পাঠানোই হত না**, তাই ✏️ বসত না। এখন পাঠানো হয়
       (`items`), আর "লেখা" হিসেবে যায় শুধু খাতার নিজের লেখাটুকু (`own`/`ownSeg`)
       — তাই একই খরচ দুবার দেখায় না। ⛔ মোট (`exp`) আগের মতোই অপরিবর্তিত।
       ⛔ এডিটরের কোড নতুন কিছু নয় — V400-এর `finExpenseEdit`-ই খোলে। */
    function ensure(d) { if (!days[d]) days[d] = { cash: 0, online: 0, exp: 0, seg: [], ownSeg: [], own: 0, items: [] }; return days[d]; }
    /* 🟢🔒 V628 (২৪.০৮.২০২৬) — তারিখ ধরে আসল collections সারি মনে রাখা, যাতে
       "✏️ Edit This Day" বোতাম সঠিক সারিতে পৌঁছাতে পারে। ব্রাঞ্চ এখন সবসময়
       একটাই নির্দিষ্ট (V628-এর "All Branches" অপসারণ) — তাই প্রতি তারিখে
       বড়জোর একটাই সারি। */
    var rowByDate = {};
    coll.forEach(function (row) {
      var d = row.entry_date; if (!d) return;
      var o = ensure(d);
      o.cash += Number(row.cash || 0); o.online += Number(row.online || 0);
      rowByDate[d] = row;
      var note = row.expense_notes || '';
      var se = (row.expense_total != null && row.expense_total >= 0) ? Number(row.expense_total) : finSumNumbers(note);
      if (se !== 0 || note) { o.exp += se; o.own += se; if (note) { o.seg.push(note); o.ownSeg.push(note); } }   /* 🟠 V960 */
    });
    exp.forEach(function (row) {
      var d = row.entry_date; if (!d) return;
      var o = ensure(d);
      var a = Number(row.amount || 0);
      o.exp += a;
      var cat = row.category || '', pt = row.paid_to || '';
      o.seg.push((pt ? (cat + ' — ' + pt) : cat) + '-' + a);
      o.items.push(row);   /* 🟠 V960 — ✏️ চেপে বদলানো/মোছার জন্য আসল সারিটাই */
    });
    /* 🟢🔒 V927 (৩১.০৮.২০২৬, TK ডেমো প্রুফ দেখে "আমার উত্তর পাশ") —
       ০১/০৯/২০২৬ থেকে যে দিনগুলোয় **হাতে লেখা কোনো সারি নেই**, সেই দিনের আয়
       অ্যাপ নিজেই Visit Fee · Treatment · Medicine/Saline পেমেন্ট থেকে গুনে
       দেখায়। ⛔ ডেটাবেসে কিচ্ছু লেখা হয় না — শুধু দেখানো; হাতে লেখা সারি
       থাকলে সেটাই জেতে; ০১/০৯-এর আগের দিন কখনো ছোঁয়া হয় না। */
    var autoDays = {};
    try {
      if (typeof window.wlv1AutoIncomeForMonth === 'function') {
        var __am = window.wlv1AutoIncomeForMonth(month, branch) || {};
        Object.keys(__am).forEach(function (d) {
          if (rowByDate[d]) return;                 // হাতে লেখা আছে → অ্যাপ ছোঁবে না
          var o = ensure(d);
          if (Number(o.cash || 0) !== 0 || Number(o.online || 0) !== 0) return;
          o.cash = Number(__am[d].cash || 0);
          o.online = Number(__am[d].online || 0);
          autoDays[d] = true;
        });
      }
    } catch (e) {}
    var showTag = false;
    try { showTag = (typeof window.isMaster === 'function') && window.isMaster(); } catch (e) { showTag = false; }
    var dates = Object.keys(days).sort();
    window.__finExpMap = window.__finExpMap || {};
    var cashTot = 0, onlineTot = 0, expTot = 0;
    var trs = dates.map(function (d) {
      var o = days[d];
      cashTot += o.cash; onlineTot += o.online; expTot += o.exp;
      var dp = String(d).split('-'); var dotted = dp[2] + '/' + dp[1] + '/' + dp[0];
      /* ট্যাগ শুধু মাস্টারের পর্দায় — TK: "অটো না হাতে ঠিক করা এটা মাস্টার
         ছাড়া কেউ দেখতে পাবে না"। স্টাফ/ডাক্তার শুধু সংখ্যাটাই দেখেন। */
      var tag = '';
      if (showTag && d >= (window.WLV1_AUTO_INCOME_FROM || '2026-09-01')) {
        tag = autoDays[d]
          ? '<span style="display:inline-block;margin-left:6px;background:#EAF6EE;color:#0B8A3E;border:1px solid #CFE9D8;border-radius:6px;padding:1px 5px;font-size:8.5px;font-weight:800;vertical-align:middle">AUTO</span>'
          : (rowByDate[d] ? '<span style="display:inline-block;margin-left:6px;background:#EAF2FE;color:#0B4FA8;border:1px solid #C9DDF7;border-radius:6px;padding:1px 5px;font-size:8.5px;font-weight:800;vertical-align:middle">হাতে ঠিক করা</span>' : '');
      }
      var dottedCell = dotted + tag;   /* ⛔ `dotted` নিজে অপরিবর্তিত — খরচের
             পপ-আপের শিরোনামে (`__finExpMap[key].dotted`) ট্যাগ যাবে না */
      var key = 'M' + d;
      var expCell;
      if (o.exp > 0 || o.seg.length) {
        window.__finExpMap[key] = { dotted: dotted, note: (o.ownSeg || []).join(', '), total: o.exp, own: Number(o.own || 0), items: (o.items || []), editRowId: rowByDate[d] ? rowByDate[d].id : null,
          addDate: d, addBranch: (branch === '__all' ? '' : branch) };   /* 🟠 V960 · ➕ V1203 */
        expCell = '<td onclick="finExpenseBreakdown(\'' + key + '\')" style="padding:6px;text-align:right;color:#B42318;font-weight:700;cursor:pointer;border:1px solid #CFE9D8">' +
          (o.exp > 0 ? m.money(o.exp).replace('₹', '') : '-') + '</td>';
      } else {
        expCell = '<td style="padding:6px;text-align:right;color:#B42318;border:1px solid #CFE9D8">-</td>';
      }
      return '<tr><td style="padding:6px;font-weight:700;border:1px solid #CFE9D8">' + dottedCell + '</td>' +
        '<td style="padding:6px;text-align:right;color:#0A7C3F;border:1px solid #CFE9D8">' + m.money(o.cash).replace('₹', '') + '</td>' +
        '<td style="padding:6px;text-align:right;color:#0A7C3F;border:1px solid #CFE9D8">' + m.money(o.online).replace('₹', '') + '</td>' +
        expCell + '</tr>';
    }).join('');
    var headHtml = '<tr style="background:#0A7C3F;color:#fff">' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">Date</th>' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">Cash</th>' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">Online</th>' +
      '<th style="padding:8px 6px;text-align:center;border:1px solid #0d6a31">খরচ</th></tr>';
    var totHtml = '<tr style="background:#EAF6EE;font-weight:700">' +
      '<td style="padding:6px;border:1px solid #CFE9D8;color:#0A5C33">Total</td>' +
      '<td style="padding:6px;text-align:right;border:1px solid #CFE9D8;color:#0A5C33">' + m.money(cashTot).replace('₹', '') + '</td>' +
      '<td style="padding:6px;text-align:right;border:1px solid #CFE9D8;color:#0A5C33">' + m.money(onlineTot).replace('₹', '') + '</td>' +
      '<td style="padding:6px;text-align:right;border:1px solid #CFE9D8;color:#B42318">' + m.money(expTot).replace('₹', '') + '</td></tr>';
    /* 🟢🔒 V693 (২৬.০৮.২০২৬, TK-নির্দেশ ছবিসহ, তাঁর "হ্যাঁ" নিয়ে) — নিচের
       বাক্সটা এখন TK-এর ছবির মতো: **মোট আয় · মোট ব্যয় · অবশিষ্ট**।
       আগে ছিল "Previous Balance | অবশিষ্ট টাকা" (V460 / Android V453)।
       ⚠️ টাকার হিসাবেও বদল — অবশিষ্ট = মোট আয় − মোট ব্যয়; **গত মাসের বাকি
          আর যোগ হয় না**। TK-এর ছবির সংখ্যাও ঠিক এই হিসাবেই মেলে।
       ⛔ ফোনের `IncomeExpenseActivity.monthTotalsBox()`-এর হুবহু একই (§৬.৬)।
       ⛔ Daily Ledger-এর নিচের বার (এই ফাইলের ~৮৪১ লাইন) এক অক্ষরও বদলায়নি —
          সেখানে গত মাসের বাকি আগের মতোই ধরা হয়। */
    var incomeTot = cashTot + onlineTot;
    var remaining = incomeTot - expTot;
    function finSumLine(label, value, color, line){
      return '<div style="display:flex;align-items:center;padding:7px 0' +
        (line ? ';border-bottom:1px solid #EEF3F0' : '') + '">' +
        '<span style="font-size:14px;font-weight:700;color:' + color + '">' + label + '</span>' +
        '<span style="font-size:14px;color:' + color + ';padding:0 8px">=</span>' +
        '<span style="flex:1;text-align:right;font-size:15px;font-weight:800;color:' + color + '">' + value + '</span></div>';
    }
    var balPair = '<div style="margin:10px 0 4px;background:#fff;border:1px solid #E3ECE6;border-radius:12px;padding:12px 14px">' +
      finSumLine('মোট আয়', m.money(incomeTot), '#0A7C3F', true) +
      finSumLine('মোট ব্যয়', m.money(expTot), '#B42318', true) +
      finSumLine('অবশিষ্ট', m.money(remaining), '#1B4E9B', false) +
      '</div>';
    // 🔵 Date ঘর সরু (colgroup width), Cash/Online/খরচ বাকি জায়গা ভাগ করে নেয়।
    var tableHtml = '<div style="overflow-x:auto"><table style="width:100%;min-width:560px;border-collapse:collapse;font-size:12px">' +
      '<colgroup><col style="width:92px"><col><col><col></colgroup>' +
      headHtml + trs + totHtml + '</table></div>';
    out.innerHTML = tableHtml +
      /* 🔴 V430 — Monthly Summary-র খালি-লেখা ফোনের হুবহু (kt:2618) */
      (dates.length ? '' : '<div class="mut">এই মাসে এখনো কোনো এন্ট্রি নেই।</div>') +
      balPair +
      /* 🟢🔒 V693 → 📈🔒 V1217 — আগে এখানে নিচে "••• Options" বোতাম ও
         তার মেনু বসত। TK-নির্দেশে (০৮.০৯.২০২৬) সেটা উপরের হেডারের ⋮-এ উঠে
         গেছে। ⛔ কাজ তিনটেই আগের প্রমাণিত ফাংশন (finMonthlyShare /
         finMonthlyPdf) — এক অক্ষরও বদলায়নি, শুধু জায়গা বদলেছে। */
      '';
    var shareText = 'Income & Expense — ' + month + (branch === '__all' ? ' (All Branches)' : ' (' + branch + ')');
    window._finMonthlyHtml = '<h1>' + m.esc(shareText) + '</h1>' + tableHtml + balPair;
    window._finMonthlyText = shareText + '\n' +
      'Collection: ' + m.money(cashTot + onlineTot) + ' (Cash ' + m.money(cashTot) + ' / Online ' + m.money(onlineTot) + ')\n' +
      'Expense: ' + m.money(expTot) + '\n' +
      /* 🟢🔒 V693 — পর্দায় যা দেখা যায়, শেয়ারের লেখাতেও ঠিক তাই।
         "Previous Balance" পর্দা থেকে উঠে যাওয়ায় লেখাতেও রাখা হলো না —
         নইলে পর্দা আর লেখা দুরকম বলত, সেটাই নতুন একটা ভুল হত। */
      'মোট আয়: ' + m.money(incomeTot) + '\n' +
      'মোট ব্যয়: ' + m.money(expTot) + '\n' +
      'অবশিষ্ট: ' + m.money(remaining);
  }

  /* 🟢🔒 V693 — "••• Options" খোলা/বন্ধ, আর ভিতরের তিনটে কাজ। */
  function finMonthlyOptions() {
    var el = document.getElementById('finMonthlyMenu');
    if (!el) return;
    el.style.display = (el.style.display === 'block') ? 'none' : 'block';
  }
  function finMonthlyMenuPick(which) {
    var el = document.getElementById('finMonthlyMenu');
    if (el) el.style.display = 'none';
    /* 📈🔒 V1217 — Show বোতাম আর নেই, হিসাব নিজে থেকেই আসে; কিন্তু আসার
       আগেই ⋮ চাপলে ফাঁকা লেখা শেয়ার হয়ে যেত। ফোনেও হুবহু একই সুরক্ষা। */
    if (!window._finMonthlyText || !window._finMonthlyHtml) {
      alert('হিসাব আসছে — এক মুহূর্ত পরে আবার চাপুন।');
      return;
    }
    if (which === 1) finMonthlyShare();
    /* PDF ও Print — একই ব্রাউজার-পর্দা; সেখানে গন্তব্যে "Save as PDF"
       বাছলে পিডিএফ, প্রিন্টার বাছলে ছাপা (ফোনেও ঠিক একই নিয়ম)। */
    else finMonthlyPdf();
  }
  function finMonthlyPdf() { window.MOD.printHtml('Monthly Summary', window._finMonthlyHtml || ''); }
  function finMonthlyShare() { window.MOD.whatsapp(window._finMonthlyText || 'Monthly Summary'); }

  /* =====================================================================
     🟢🔒 V629 (২৪.০৮.২০২৬, TK-নির্দেশ) — "Statement": ব্যাংক-স্টেটমেন্টের
     মতো, যেকোনো From–To তারিখের মধ্যে প্রতিদিনের **পরে চলতি ব্যালেন্স
     (running balance)** দেখায়। ফোনের `IncomeExpenseActivity.statement()`-এর
     হুবহু যমজ — একই দুই-উৎস হিসাব (collections.expense_notes + expenses),
     একই ব্রাঞ্চ-নিয়ম (V628-এর "All Branches" বাদ, সবসময় একটা নির্দিষ্ট
     ব্রাঞ্চ)। কোনো নতুন হিসাব-সূত্র নেই — Ledger Sheet/Monthly-র প্রমাণিত
     সূত্রই পুনর্ব্যবহার।
     ===================================================================== */
  async function finStatement() {
    var m = window.MOD;
    var today = m.todayIST();
    var fromD = new Date(today); fromD.setDate(fromD.getDate() - 30);
    var fromIso = fromD.getFullYear() + '-' + String(fromD.getMonth() + 1).padStart(2, '0') + '-' + String(fromD.getDate()).padStart(2, '0');
    var finLockBr = finLockedBranch();
    var __stCur = finCurBranch();
    var brOpts = finLockBr ? branchOptions(finLockBr)
      : ((!__stCur || __stCur === 'All Branches') ? '<option value="" selected>Select Branch</option>' + branchOptions('')
         : branchOptions(__stCur));
    document.getElementById('finBody').innerHTML =
      '<div class="card"><h2>📄 Statement</h2>' +
      '<label>Branch</label><select id="stBranchSel"' + (finLockBr ? ' disabled' : '') + ' class="input">' + brOpts + '</select>' +
      '<div class="finTwo"><div><label>From</label><input id="stFrom" class="input" type="date" value="' + fromIso + '"></div>' +
      '<div><label>To</label><input id="stTo" class="input" type="date" value="' + today + '"></div></div>' +
      '<div class="actions"><button onclick="finStatementLoad()">Show</button></div>' +
      '<div id="stOut" class="mut">Loading...</div></div>';
    finStatementLoad();
  }

  async function finStatementLoad() {
    var m = window.MOD, client = await sb();
    var brEl = document.getElementById('stBranchSel');
    var branchSel = brEl ? brEl.value : '';
    if (!finLockedBranch()) finSetGlobalBranch(branchSel || 'All Branches');
    var out = document.getElementById('stOut');
    if (!branchSel) {
      if (out) out.innerHTML = (typeof window.wlv1BranchAskCard === 'function')
        ? window.wlv1BranchAskCard() : '🏥 একটি Branch বাছুন।';
      return;
    }
    var fromIso = (document.getElementById('stFrom') || {}).value;
    var toIso = (document.getElementById('stTo') || {}).value;
    if (!fromIso || !toIso) { out.innerHTML = 'তারিখ বাছুন।'; return; }
    if (fromIso > toIso) { out.innerHTML = '"From" তারিখ "To"-এর পরে হতে পারে না।'; return; }
    out.innerHTML = 'Loading...';
    var toNext = new Date(toIso); toNext.setDate(toNext.getDate() + 1);
    var toNextIso = toNext.getFullYear() + '-' + String(toNext.getMonth() + 1).padStart(2, '0') + '-' + String(toNext.getDate()).padStart(2, '0');
    var coll = [], exp = [], prevColl = [], prevExp = [], openingOk = true;
    try {
      var qc = client.schema('fin').from('collections').select('*').gte('entry_date', fromIso).lt('entry_date', toNextIso).eq('ignored', false).eq('branch', branchSel);
      var qe = client.schema('fin').from('expenses').select('*').gte('entry_date', fromIso).lt('entry_date', toNextIso).eq('ignored', false).eq('branch', branchSel);
      var __rc = await qc.order('entry_date', { ascending: true }), __re = await qe.order('entry_date', { ascending: true });
      if ((__rc && __rc.error) || (__re && __re.error)) { out.innerHTML = 'Could not load — একটু পরে আবার দেখুন'; return; }
      coll = __rc.data || []; exp = __re.data || [];
    } catch (e) { out.innerHTML = 'Could not load (offline?).'; return; }
    // ওপেনিং ব্যালেন্স — "From"-এর আগের সব দিনের (এই ব্রাঞ্চের) আয়−খরচ। Ledger
    // Sheet/Monthly-র prevBal-এর হুবহু একই দুই-উৎস হিসাব।
    try {
      var pqc = client.schema('fin').from('collections').select('cash,online,expense_total,expense_notes').lt('entry_date', fromIso).eq('ignored', false).eq('branch', branchSel);
      var pqe = client.schema('fin').from('expenses').select('amount').lt('entry_date', fromIso).eq('ignored', false).eq('branch', branchSel);
      var __pc = await pqc, __pe = await pqe;
      if ((__pc && __pc.error) || (__pe && __pe.error)) openingOk = false;
      prevColl = (__pc && __pc.data) || []; prevExp = (__pe && __pe.data) || [];
    } catch (e) { openingOk = false; }
    var opening = 0;
    prevColl.forEach(function (row) {
      var note = row.expense_notes || '';
      var e2 = (row.expense_total != null && row.expense_total >= 0) ? Number(row.expense_total) : finSumNumbers(note);
      opening += Number(row.cash || 0) + Number(row.online || 0) - e2;
    });
    prevExp.forEach(function (row) { opening -= Number(row.amount || 0); });
    finStatementRender(coll, exp, opening, openingOk, fromIso, toIso, branchSel, out);
  }

  function finStatementRender(coll, exp, opening, openingOk, fromIso, toIso, branchSel, out) {
    var m = window.MOD;
    var days = {};
    function ensure(d) { if (!days[d]) days[d] = { cash: 0, online: 0, exp: 0 }; return days[d]; }
    coll.forEach(function (row) {
      var d = row.entry_date; if (!d) return;
      var o = ensure(d);
      o.cash += Number(row.cash || 0); o.online += Number(row.online || 0);
      var note = row.expense_notes || '';
      var se = (row.expense_total != null && row.expense_total >= 0) ? Number(row.expense_total) : finSumNumbers(note);
      if (se !== 0 || note) o.exp += se;
    });
    exp.forEach(function (row) {
      var d = row.entry_date; if (!d) return;
      ensure(d).exp += Number(row.amount || 0);
    });
    var dates = Object.keys(days).sort();
    var rows = [];
    rows.push(['Opening', '—', '—', '—', openingOk ? m.money(opening) : '—', true]);
    var cashTot = 0, onlineTot = 0, expTot = 0, running = opening;
    dates.forEach(function (d) {
      var o = days[d];
      cashTot += o.cash; onlineTot += o.online; expTot += o.exp;
      running += o.cash + o.online - o.exp;
      var dp = String(d).split('-'); var dotted = dp[2] + '/' + dp[1] + '/' + dp[0];
      rows.push([dotted, o.cash > 0 ? m.money(o.cash).replace('₹', '') : '-', o.online > 0 ? m.money(o.online).replace('₹', '') : '-',
        o.exp > 0 ? m.money(o.exp).replace('₹', '') : '-', m.money(running).replace('₹', ''), false]);
    });
    rows.push(['Total', m.money(cashTot).replace('₹', ''), m.money(onlineTot).replace('₹', ''), m.money(expTot).replace('₹', ''),
      openingOk ? m.money(running).replace('₹', '') : '—', true]);

    /* 🎨🔒 V1610 (১৯.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ — "আরো প্রফেশনাল
       বানাতে হবে", "খরচেরগুলো সম্পূর্ণ লাল থাকতে হবে") — ফোনের
       IncomeExpenseActivity.kt-এর statementPdfHtml-এর হুবহু জোড়া: লেটারহেড
       (ক্লিনিকের নাম + তৈরির তারিখ) + Opening/Total In/Total Expense/Closing
       চারটে সারাংশ বাক্স + Expense কলাম (সংখ্যা ও "-" দুটোই) বোল্ড লাল।
       ⛔ টাকার হিসাব এক অক্ষরও বদলায়নি। */
    var genDate = (function () { var d = new Date(); return String(d.getDate()).padStart(2, '0') + '/' + String(d.getMonth() + 1).padStart(2, '0') + '/' + d.getFullYear(); })();
    var fromDot = fromIso.split('-').reverse().join('/'), toDot = toIso.split('-').reverse().join('/');
    var html = '<div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #0A5C33;padding-bottom:10px;margin-bottom:14px">' +
      '<div><div style="font-size:15px;font-weight:bold;color:#0A5C33">MAA AYURVED PILES CLINIC</div>' +
      '<div style="font-size:12px;color:#667085;margin-top:2px">Statement — ' + m.esc(branchSel) + '</div></div>' +
      '<div style="text-align:right;font-size:11px;color:#667085;line-height:1.6">' + fromDot + ' – ' + toDot +
      '<br>Generated: <b style="color:#101C2E">' + genDate + '</b></div></div>';
    html += '<div style="display:flex;gap:8px;margin-bottom:14px">' +
      '<div style="flex:1;border-radius:9px;padding:8px 10px;border:1px solid #CDEAD9;background:#FAFCFB">' +
      '<div style="font-size:9px;font-weight:bold;letter-spacing:.06em;color:#667085;text-transform:uppercase">Opening</div>' +
      '<div style="font-size:14px;font-weight:bold;margin-top:3px;color:#0A5C33">' + (openingOk ? m.money(opening) : '—') + '</div></div>' +
      '<div style="flex:1;border-radius:9px;padding:8px 10px;border:1px solid #CDEAD9;background:#FAFCFB">' +
      '<div style="font-size:9px;font-weight:bold;letter-spacing:.06em;color:#667085;text-transform:uppercase">Total In</div>' +
      '<div style="font-size:14px;font-weight:bold;margin-top:3px;color:#0A7C3F">' + m.money(cashTot + onlineTot) + '</div></div>' +
      '<div style="flex:1;border-radius:9px;padding:8px 10px;border:1px solid #F3C6BE;background:#FDEDEB">' +
      '<div style="font-size:9px;font-weight:bold;letter-spacing:.06em;color:#667085;text-transform:uppercase">Total Expense</div>' +
      '<div style="font-size:14px;font-weight:bold;margin-top:3px;color:#B42318">' + m.money(expTot) + '</div></div>' +
      '<div style="flex:1;border-radius:9px;padding:8px 10px;border:1px solid #C7D7EA;background:#FAFCFB">' +
      '<div style="font-size:9px;font-weight:bold;letter-spacing:.06em;color:#667085;text-transform:uppercase">Closing</div>' +
      '<div style="font-size:14px;font-weight:bold;margin-top:3px;color:#0F3A66">' + (openingOk ? m.money(running) : '—') + '</div></div>' +
      '</div>';
    html += '<table style="width:100%;border-collapse:collapse;font-size:12px">' +
      '<tr style="background:#0A7C3F;color:#fff">' +
      '<th style="padding:6px;border:1px solid #CFE9D8">Date</th><th style="padding:6px;border:1px solid #CFE9D8">Cash</th>' +
      '<th style="padding:6px;border:1px solid #CFE9D8">Online</th><th style="padding:6px;border:1px solid #CFE9D8">খরচ</th>' +
      '<th style="padding:6px;border:1px solid #CFE9D8">Running Balance</th></tr>';
    rows.forEach(function (r, i) {
      var bg = r[5] ? '#EAF6EE' : (i % 2 === 0 ? '#FFFFFF' : '#F7FBF8');
      var fg = r[5] ? '#0A5C33' : '#41506A';
      html += '<tr style="background:' + bg + '">' +
        '<td style="padding:6px;border:1px solid #CFE9D8;font-weight:' + (r[5] ? '700' : '400') + ';color:' + fg + '">' + m.esc(r[0]) + '</td>' +
        '<td style="padding:6px;border:1px solid #CFE9D8;text-align:right;color:#0A7C3F">' + m.esc(r[1]) + '</td>' +
        '<td style="padding:6px;border:1px solid #CFE9D8;text-align:right;color:#0A7C3F">' + m.esc(r[2]) + '</td>' +
        '<td style="padding:6px;border:1px solid #CFE9D8;text-align:right;font-weight:700;color:#B42318">' + m.esc(r[3]) + '</td>' +
        '<td style="padding:6px;border:1px solid #CFE9D8;text-align:right;font-weight:700;color:#0F3A66">' + m.esc(r[4]) + '</td></tr>';
    });
    html += '</table>';
    if (!dates.length) html += '<div class="mut" style="margin-top:8px">এই সময়ের মধ্যে এখনো কোনো এন্ট্রি নেই।</div>';

    // 🔵 R6-এর হুবহু একই প্যাটার্নে WhatsApp শেয়ার।
    var fromDotted = fromIso.split('-').reverse().join('/'), toDotted = toIso.split('-').reverse().join('/');
    var sbx = '📄 স্টেটমেন্ট — ' + fromDotted + ' থেকে ' + toDotted + '\n' + branchSel + '\n————————————\n' +
      'Opening Balance: ' + (openingOk ? m.money(opening) : '—') + '\n';
    var run2 = opening;
    dates.forEach(function (d) {
      var o = days[d]; run2 += o.cash + o.online - o.exp;
      var dp = String(d).split('-'); var dotted = dp[2] + '/' + dp[1] + '/' + dp[0];
      sbx += dotted + ' — Cash ' + m.money(o.cash) + ' · Online ' + m.money(o.online) + ' · খরচ ' + m.money(o.exp) + ' · Balance ' + m.money(run2) + '\n';
    });
    sbx += '————————————\nমোট: Cash ' + m.money(cashTot) + ' · Online ' + m.money(onlineTot) + ' · খরচ ' + m.money(expTot) + '\n';
    sbx += 'Closing Balance: ' + (openingOk ? m.money(running) : '—');
    window._finStatementText = sbx;
    html += '<div class="actions" style="margin-top:10px"><button onclick="finStatementShare()">📤 WhatsApp-এ শেয়ার</button></div>';
    out.innerHTML = html;
  }

  function finStatementShare() { window.MOD.whatsapp(window._finStatementText || 'Statement'); }

  // expose
  window.incomeExpense = incomeExpense;
  window.finSetHomeBranch = finSetHomeBranch;
  window.finPickDay = finPickDay;
  window.finShowDay = finShowDay;
  window.finIsStaffOnly = finIsStaffOnly;           // 🔵 V406
  window.finEntryPermission = finEntryPermission;   // 🔵 V406
  window.finPermBranchChange = finPermBranchChange; // 🔵 V406
  window.finPermLoad = finPermLoad;                 // 🔵 V406
  window.finPermToggle = finPermToggle;             // 🔵 V406
  window.finDecide = finDecide;                     // 🔵 V406
  window.finRenderApprovals = finRenderApprovals;   // 🔵 V406
  window.finApprovalsHtml = finApprovalsHtml;       // 🔵 V406
  window.finLedgerSheet = finLedgerSheet;
  window.finLedgerLoad = finLedgerLoad;
  window.finLedgerRowEdit = finLedgerRowEdit;
  window.finRowTap = finRowTap;
  window.finExpenseBreakdown = finExpenseBreakdown;
  window.finExpenseEdit = finExpenseEdit;       // 🟢 V400
  window.finExpenseSave = finExpenseSave;       // 🟢 V400
  window.finExpenseDelete = finExpenseDelete;   // 🟢 V400
  window.finLedgerSave = finLedgerSave;
  window.finLedgerDelete = finLedgerDelete;   // 🟢 B675
  window.finAddCollection = finAddCollection;
  window.finSaveCollection = finSaveCollection;
  window.finAddExpense = finAddExpense;
  window.finSaveExpense = finSaveExpense;
  window.finDailyLedger = finDailyLedger;
  window.finMonthly = finMonthly;
  window.finRunMonthly = finRunMonthly;
  window.finMonthlyPdf = finMonthlyPdf;
  window.finMonthlyShare = finMonthlyShare;
  window.finMonthlyOptions = finMonthlyOptions;
  window.finMonthlyMenuPick = finMonthlyMenuPick;
  window.finStatement = finStatement;
  window.finStatementLoad = finStatementLoad;
  window.finStatementShare = finStatementShare;
})();
