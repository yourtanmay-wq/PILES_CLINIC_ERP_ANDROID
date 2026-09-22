/* =====================================================================
   V245 MODULE CORE  (shared by profile.js / notebook.js / finance.js)
   Owner: TK BISWAS.  Additive only — does NOT touch existing app logic.

   WHY A SEPARATE CLIENT: the main app uses the public anon key with NO
   Supabase Auth session. The three new modules need real per-person
   privacy, so they open their OWN authenticated Supabase client (a second,
   module-only login) that never changes the existing app login. Nothing
   here reads/writes any existing table.

   IDENTITY: no personal email is required. The module signs in with a
   synthetic email built from the person's existing Staff Code (or mobile),
   e.g.  kne-laxmi@staff.piles .

   🔴 B305 (03.08.2026, V252): the module login is now SILENT — MOD.autoSignIn()
   derives the module identity from the main app's own session (rk_session)
   and signs in with the SAME four role passwords the main login already
   uses (admin123/staff123/doctor123/field123, see V249 SQL). No second
   password screen is shown to the person; no new password/pattern exists.
   ===================================================================== */
(function () {
  if (window.MOD) return; // load once
  var MOD = {};
  window.MOD = MOD;

  MOD.EMAIL_DOMAIN = 'staff.piles'; // synthetic email domain (not a real inbox)
  MOD._client = null;
  MOD._session = null; // {code, role, is_master}

  // Reuse the same Supabase URL/key the app already uses (from config.js).
  function cfg() { return window.RK_CONFIG || {}; }

  // Ensure supabase-js is present (the app loads it dynamically; reuse it).
  MOD.ensureLib = function () {
    return new Promise(function (resolve) {
      if (window.supabase) return resolve(true);
      var s = document.createElement('script');
      s.src = 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2';
      s.onload = function () { resolve(!!window.supabase); };
      s.onerror = function () { resolve(false); };
      document.head.appendChild(s);
      setTimeout(function () { resolve(!!window.supabase); }, 5000);
    });
  };

  // A dedicated authenticated client, isolated storageKey so it never
  // collides with anything the main app might use.
  MOD.client = async function () {
    if (MOD._client) return MOD._client;
    await MOD.ensureLib();
    if (!window.supabase) return null;
    var c = cfg();
    MOD._client = window.supabase.createClient(c.supabaseUrl, c.supabaseKey, {
      auth: { storageKey: 'rk_module_auth', persistSession: true, autoRefreshToken: true }
    });
    return MOD._client;
  };

  MOD.codeToEmail = function (code) {
    return String(code || '').trim().toLowerCase().replace(/[^a-z0-9]+/g, '-') + '@' + MOD.EMAIL_DOMAIN;
  };

  // Sign in to the module. Returns {ok, error}. Caller passes code + password.
  MOD.signIn = async function (code, password) {
    var sb = await MOD.client();
    if (!sb) return { ok: false, error: 'No internet / library not loaded' };
    var email = MOD.codeToEmail(code);
    var r = await sb.auth.signInWithPassword({ email: email, password: password });
    if (r.error) return { ok: false, error: r.error.message };
    /* 🔴🔴🔴🔒 V1570 (১৭.০৯.২০২৬, ফোনের ভিডিওতে ধরা পড়া বাগ, রুল ৮ — এখানেও
       হুবহু একই ভুল ছিল) — আগে এখানে `uid`-ফিল্টার ছাড়াই `limit(1)` চালানো
       হতো, শুধু RLS-এর "নিজেরটাই দেখা যায়" নিয়মের ভরসায়। মাস্টারের RLS
       সবার সারি দেখতে দেয় বলে এটা মাস্টারের বেলায় এলোমেলো যেকোনো একজন
       স্টাফের পরিচয় ফিরিয়ে দিতে পারত (ORDER BY নেই, ২৩ জনেরই created_at
       হুবহু এক সেকেন্ডে)। এখন নিজের auth uid দিয়ে সরাসরি ছেঁকে নেওয়া হলো। */
    var myUid = (r.data && r.data.user && r.data.user.id) || '';
    var q = sb.schema('hr').from('app_identity').select('person_code,role_kind,is_master');
    if (myUid) q = q.eq('uid', myUid);
    var id = await q.limit(1).maybeSingle();
    MOD._session = id.data
      ? { code: id.data.person_code, role: id.data.role_kind, is_master: !!id.data.is_master }
      : { code: code, role: 'staff', is_master: false };
    return { ok: true };
  };

  MOD.signOut = async function () {
    var sb = await MOD.client();
    if (sb) await sb.auth.signOut();
    MOD._session = null;
  };

  MOD.session = function () { return MOD._session; };
  MOD.isMasterModule = function () { return !!(MOD._session && MOD._session.is_master); };

  // 🔴 B305 (03.08.2026, V252, TK-নির্দেশ): মূল অ্যাপ লগইনের পরে এই মডিউলগুলোর জন্য
  // আলাদা "Module Password" স্ক্রিন আর দেখানো হবে না। এটা Android-এর V247
  // `ModuleAuth.signInCurrentSession()`-এর হুবহু একই, প্রমাণিত প্যাটার্ন —
  // চার-টে পুরনো role-পাসওয়ার্ডই (admin123/staff123/doctor123/field123)
  // পুনর্ব্যবহার হচ্ছে, নতুন কোনো পাসওয়ার্ড/প্যাটার্ন তৈরি হয়নি। মোবাইল-থেকে-কোড
  // ম্যাপিংটাও Android-এরটার সাথে অক্ষরে-অক্ষরে মেলানো (V249 SQL-এর সাথেও মেলে)।
  MOD.SPECIAL_CODE = {
    '8001080080': 'MASTER-TK', '7980993652': 'DR-KH-MANDAL', '8001800148': 'DR-JAY-BANIK',
    '9046366596': 'DR-AMIT-GOLDAR', '6297625447': 'DR-PK-ROY', '9002003540': 'FIELD-OFFICER',
    // 🔵 V308 (১০.০৮, TK-নির্দেশ: সব অংশীদারই ডাক্তার): ৪ জন এখন ডাক্তার — মোবাইল→DR-কোড
    // (V308 SQL-এর dr-…@staff.piles পরিচয়ের হুবহু মিল)। মডিউল-পাসওয়ার্ড doctor role → 'doctor123'।
    '7479173399': 'DR-JH-MANDAL', '9002610352': 'DR-GOKUL',
    '7810907954': 'DR-SAIKAT-ROY', '9242009205': 'DR-PRANAB-BISWAS'
  };
  MOD.ROLE_PASSWORD = { master: 'admin123', doctor: 'doctor123', field: 'field123' };

  // Derive module identity from the EXISTING main-app session (rk_session) —
  // never asks the person anything, never creates a new credential.
  // 🔴 B317 (03.08.2026, TK-নির্দেশ): কোড বার করার অংশটা এখানে আলাদা করা হলো
  // (`MOD.autoSignIn()`-এর ভিতরে যা ছিল) — যাতে `MOD.gate()` সাইন-ইন না করেই
  // আগে থেকে জানতে পারে "এখন আসলে কার Module-এ থাকা উচিত", আর cached সেশনের
  // সাথে মিলিয়ে দেখতে পারে।
  MOD.expectedCode = function () {
    var raw = null;
    try { raw = JSON.parse(localStorage.getItem('rk_session') || 'null'); } catch (e) {}
    if (!raw || !raw.mobile) return null;
    var mobDigits = String(raw.mobile).replace(/\D/g, '').slice(-10);
    // 🔑 V749 (২৭.০৮.২০২৬, TK: "KNE-LAXMI — এত মানুষ") — অ্যাপ থেকে যোগ করা
    //    লোকের **কোড** এখন আলাদা `code` ঘরে আসে, নাম থেকে নয়। তাই পর্দায়
    //    আসল নাম দেখানো যায়, আর auth-ইমেল (`<কোড>@staff.piles`) মিলে যায়।
    //    ⛔ পুরনো ২৩ জনের সেশনে `code` ঘর নেই ⇒ আগের নিয়মই (নাম→কোড) অটুট,
    //       এক অক্ষরও বদল নেই। Android-এ হুবহু একই ব্যবস্থা (ModuleAuth)।
    return MOD.SPECIAL_CODE[mobDigits]
        || String(raw.code || '').trim().toUpperCase()
        || String(raw.name || '').trim().toUpperCase() || null;
  };

  MOD.autoSignIn = async function () {
    var raw = null;
    try { raw = JSON.parse(localStorage.getItem('rk_session') || 'null'); } catch (e) {}
    if (!raw || !raw.mobile) return { ok: false, error: 'Main app login required' };
    var code = MOD.expectedCode();
    if (!code) return { ok: false, error: 'Main app login required' };
    var pw = MOD.ROLE_PASSWORD[String(raw.role || '').toLowerCase()] || 'staff123';
    return await MOD.signIn(code, pw);
  };

  // Is a module session already active (persisted)? Refresh identity if so.
  MOD.restore = async function () {
    var sb = await MOD.client();
    if (!sb) return false;
    var s = await sb.auth.getSession();
    if (!s.data || !s.data.session) return false;
    // 🔴🔒 V1570 — MOD.signIn()-এর হুবহু একই ফিক্স (উপরের বড় মন্তব্য দ্রষ্টব্য)।
    var myUid2 = (s.data.session.user && s.data.session.user.id) || '';
    var q2 = sb.schema('hr').from('app_identity').select('person_code,role_kind,is_master');
    if (myUid2) q2 = q2.eq('uid', myUid2);
    var id = await q2.limit(1).maybeSingle();
    if (id.data) MOD._session = { code: id.data.person_code, role: id.data.role_kind, is_master: !!id.data.is_master };
    return !!MOD._session;
  };

  /* ---- small shared helpers (safe, no dependency on app internals) ---- */
  MOD.esc = function (s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (m) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
  };
  /* 🔴🔴🆕🔒 V437 (নিজের অডিটে ধরা — ফোন-বনাম-ওয়েব তালিকার #১৭): ফোনে টাকা
     সবসময় **পুরো টাকায়** দেখানো হয় (`MoneyFormat.kt:24` — `DecimalFormat("#,##,##0")`,
     পয়সা নেই)। ওয়েবের এই ফাংশনে গোল করা ছিল না, তাই Income-Expense ও
     Partners-এ `1234.5` লেখা উঠত **₹1,234.5** — ফোনে ₹1,235। এখন এক নিয়ম।
     ⛔ শুধু **দেখানোর** চেহারা — কোনো হিসাব/সেভ/মোট ছোঁয়া হয়নি (app.js-এর
        মূল `money()` আগে থেকেই গোল করত, এটা বাদ পড়েছিল)। */
  MOD.money = function (n) { return '₹' + Math.round(Number(n || 0)).toLocaleString('en-IN'); };
  MOD.todayIST = function () {
    var d = new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  };
  MOD.nowTimeIST = function () {
    var d = new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
    return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
  };
  /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — *"নাম্বার এত হাই সিকিউরিটি ভাবে
     দেখানোর কিছু নেই… নাম্বারটা সম্পূর্ণভাবে দেখাবে।"*
     ⛔ নিচের `maskMobile` **মোছা হয়নি** — সরকারি আইডি ও কল-লগের সংরক্ষিত
        মুখোশ (`target_mobile_mask`) ওটাই ব্যবহার করে, তাই ভাঙা চলবে না।
        এটা শুধু **দেখানোর** জন্য আলাদা ফাংশন (ফোনের `ModuleUi.fullMobile`)। */
  MOD.fullMobile = function (m) {
    var raw = String(m == null ? '' : m).trim();
    var d = raw.replace(/\D/g, '');
    if (!d) return raw;
    if (raw.charAt(0) === '+') return raw;
    return d.length > 10 ? ('+' + d) : d;
  };
  MOD.maskMobile = function (m) {
    m = String(m || '').replace(/\D/g, '');
    return m.length >= 4 ? ('••••••' + m.slice(-4)) : '••••';
  };
  MOD.maskId = function (v) {
    v = String(v || '').replace(/\s/g, '');
    return v.length >= 4 ? ('XXXX XXXX ' + v.slice(-4)) : '••••';
  };

  // Offline-safe local cache: mirror of the app's philosophy — write local
  // first, then cloud. Kept under a module-only key so it never collides.
  MOD.localGet = function (key) {
    try { return JSON.parse(localStorage.getItem('rk_mod_' + key) || 'null'); } catch (e) { return null; }
  };
  MOD.localSet = function (key, val) {
    try { localStorage.setItem('rk_mod_' + key, JSON.stringify(val)); } catch (e) {}
  };
  // Pending cloud writes that failed offline — retried on next open (dedupe by id).
  MOD.queueWrite = function (schema, table, row) {
    var q = MOD.localGet('pending') || [];
    q = q.filter(function (x) { return !(x.schema === schema && x.table === table && x.row && x.row.id === row.id); });
    q.push({ schema: schema, table: table, row: row, at: Date.now() });
    MOD.localSet('pending', q.slice(-500));
  };
  MOD.flushQueue = async function () {
    var sb = await MOD.client();
    if (!sb || !MOD._session) return;
    var q = MOD.localGet('pending') || [];
    if (!q.length) return;
    var left = [];
    for (var i = 0; i < q.length; i++) {
      try {
        var r = await sb.schema(q[i].schema).from(q[i].table).upsert(q[i].row);
        if (r.error) left.push(q[i]);
      } catch (e) { left.push(q[i]); }
    }
    MOD.localSet('pending', left);
  };

  /* 🔴🔒 V418 (TK-নির্দেশ: "ভবিষ্যতে ডুপ্লিকেট এন্ট্রির জন্য আটকে দেয়") —
     ডেটাবেস নিজের unique-নিয়মে আটকেছে কিনা চেনা। এটা **নেট-সমস্যা নয়**,
     তাই এমন সারি অপেক্ষার সারিতে বসানো চলবে না (নইলে চিরকাল চেষ্টা করত)। */
  MOD.isDuplicateError = function (err) {
    try {
      var c = String((err && err.code) || '');
      var m = String((err && err.message) || '') + ' ' + String((err && err.details) || '');
      return c === '23505' || /duplicate key/i.test(m) || /already exists/i.test(m);
    } catch (e) { return false; }
  };

  // Upsert by id: local-first, then cloud; never creates a duplicate (id key).
  MOD.save = async function (schema, table, row) {
    MOD.localSet(table + '_' + row.id, row); // immediate local copy
    var sb = await MOD.client();
    if (!sb || !MOD._session) { MOD.queueWrite(schema, table, row); return { ok: false, offline: true }; }
    try {
      var r = await sb.schema(schema).from(table).upsert(row).select().maybeSingle();
      if (r.error) {
        // ⛔ ডুপ্লিকেট হলে অপেক্ষায় বসে না — সৎভাবে ফিরিয়ে দেয়, পর্দা বার্তা দেখাবে।
        if (MOD.isDuplicateError(r.error)) return { ok: false, duplicate: true, error: r.error.message };
        MOD.queueWrite(schema, table, row); return { ok: false, error: r.error.message };
      }
      return { ok: true, data: r.data };
    } catch (e) { MOD.queueWrite(schema, table, row); return { ok: false, error: String(e) }; }
  };

  // uuid v4 (module rows use real uuids so id-upsert is safe across devices)
  MOD.uuid = function () {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      var r = (Math.random() * 16) | 0, v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  };

  // Shared login gate: ensures a module session, then runs render().
  // B305 (V252): no visible second password screen — sign in silently using the
  // existing main-app session (MOD.autoSignIn). If that genuinely fails
  // (e.g. main session missing), show a plain error + Back button — never
  // a password field, so no second credential is ever asked for here.
  // 🔴 B317 (03.08.2026, TK-নির্দেশ — একজনের পর অন্য একজন লগইন করলে আগের
  // Module-পরিচয় যেন কখনো না থেকে যায়): এখন cached সেশন ব্যবহারের আগে সবসময়
  // যাচাই হয় সেটা *এখনকার* main-app ব্যবহারকারীরই কিনা — না মিললে চুপচাপ
  // সাইন-আউট করে বর্তমান ব্যবহারকারী হিসেবেই আবার সাইন-ইন হয়।
  /* 🔴🔴🔒 V1230 (০৮.০৯.২০২৬ — TK, দুবার বলার পরে: *"staff profile · income and
     expense — চাপ করলে কোন কাজ হয় না · কিছু আসে না, ওপেনও হয় না"*)।

     **কোডে মেপে পাওয়া কারণ:** এই দরজাটা আগে **কিচ্ছু না দেখিয়ে** সোজা
     `restore()` / `autoSignIn()`-এর জন্য অপেক্ষা করত। ও দুটো ইন্টারনেটে কথা
     বলে; উত্তর না এলে (নেট ঝুলে থাকা · টোকেন আটকে যাওয়া) **কোনো উত্তরই আসে না**
     ⇒ পর্দা যেমন ছিল তেমনই থাকে, ব্যবহারকারীর মনে হয় বোতামটা মরা।
     (এখানে নেট বন্ধ বলে সঙ্গে সঙ্গে "Could not open" আসত, তাই এতদিন ধরা পড়েনি —
      TK-র ওখানে নেট আছে, তাই ওটা ঝুলে থাকে।)

     ⇒ দুটো সুরক্ষা বসল, দুটোই "কম করে, বেশি নয়" ধরনের:
       ① চাপ দেওয়ামাত্রই **"Opening…" কার্ড** বসে — পর্দা আর কখনো নিঃশব্দ থাকে না।
       ② লগইনের ধাপে **১২ সেকেন্ডের সময়সীমা** — উত্তর না এলে সৎ বার্তা দেখায়,
          অনন্তকাল অপেক্ষা করে না।
     ⛔ সফল হলে আচরণ **হুবহু আগের মতোই** — একই `render()`, একই `flushQueue()`।
     ⛔ কোনো তথ্য · অনুমতি · সেভের নিয়ম ছোঁয়া হয়নি। */
  MOD._withTimeout = function (promise, ms) {
    return Promise.race([
      Promise.resolve(promise).catch(function (e) { return { ok: false, error: String(e && e.message || e) }; }),
      new Promise(function (res) { setTimeout(function () { res({ __timeout: true }); }, ms || 12000); })
    ]);
  };
  MOD.gate = async function (title, render) {
    var host = document.getElementById('app');
    // ① সঙ্গে সঙ্গে কিছু দেখাও — পর্দা কখনো নিঃশব্দ থাকবে না
    try {
      host.innerHTML = '<div class="wrap"><div class="topbar"><b>' + MOD.esc(title) + '</b></div>' +
        '<div class="page"><div class="card"><h2>Opening…</h2>' +
        '<p class="mut">Signing in to this module. Please wait a moment.</p></div></div></div>';
    } catch (e) {}
    var expected = MOD.expectedCode();
    var timedOut = false;
    if (!MOD._session) {
      var rr = await MOD._withTimeout(MOD.restore(), 12000);
      if (rr && rr.__timeout) timedOut = true;
    }
    if (MOD._session && expected && MOD._session.code !== expected) {
      await MOD._withTimeout(MOD.signOut(), 8000);
    }
    if (MOD._session) { MOD.flushQueue(); return render(); }
    var r = timedOut ? { ok: false, error: 'Slow or blocked internet — could not sign in to this module. Please check the connection and try again.' }
                     : await MOD._withTimeout(MOD.autoSignIn(), 12000);
    if (r && r.__timeout) r = { ok: false, error: 'Slow or blocked internet — could not sign in to this module. Please check the connection and try again.' };
    if (r.ok) { MOD.flushQueue(); return render(); }
    host.innerHTML =
      '<div class="wrap"><div class="topbar"><b>' + MOD.esc(title) + '</b></div>' +
      '<div class="page"><div class="card"><h2>Could not open</h2>' +
      '<p class="mut">' + MOD.esc(r.error || 'Please log in again from the main app.') + '</p>' +
      '<div class="actions"><button class="ghost" id="modBackBtn">Back</button></div></div></div></div>';
    document.getElementById('modBackBtn').onclick = function () { try { dashboard(); } catch (e) {} };
  };

  // Log an in-app Call-button press (owner rule 8). Records ONLY the press —
  // never claims the call connected, never a duration. Writes to wn.call_taps
  // and never touches any existing table.
  /* 🔴🔒 V913 (৩১.০৮.২০২৬ — TK: "আপলোড করার পরে সেটা কার্যকরী হবে তো?")
     **নিজের কাজ যাচাই করতে গিয়ে ধরা পড়ল:** V911-এ কল-গোনা বসানো হয়েছিল,
     কিন্তু এই ঘরটা মডিউল-সেশন না থাকলে **চুপচাপ ফিরে যেত** — যে স্টাফ কখনো
     Work Notebook/Staff Profiles খোলেননি, তাঁর একটাও কল গোনা হত না।
     ফোনে `logCallTap()` দরকার হলে **নিজে থেকেই নিঃশব্দে সাইন-ইন** করে নেয়
     (`signInCurrentSession`) — এখানেও ঠিক তাই।
     ⛔ সাইন-ইনটা মডিউলের **নিজের আলাদা** Supabase ক্লায়েন্টে (`rk_module_auth`),
        তাই মূল অ্যাপের লগইনে এক অক্ষরও হাত পড়ে না।
     ⛔ কোনো পর্দা বা পাসওয়ার্ড দেখায় না (V252-এর নিঃশব্দ পথ)।
     ⛔ একবার ব্যর্থ হলে এই পাতায় আর চেষ্টা করা হয় না — অকারণ নেট-ডাক নেই। */
  MOD._callTapSignInFailed = false;
  MOD.logCallTap = async function (mobile) {
    try {
      if (!MOD._session && !MOD._callTapSignInFailed) {
        try { await MOD.restore(); } catch (e) {}
        if (!MOD._session) {
          try { await MOD.autoSignIn(); } catch (e) {}
          if (!MOD._session) MOD._callTapSignInFailed = true;
        }
      }
      if (!MOD._session) return;
      // 🔴 V452 (19.08.2026, TK-অনুমোদিত): ভবিষ্যতের App Call-এ Master
      // Staff Performance থেকে exact dialed number দেখতে পারবেন। পুরনো
      // masked-only call আন্দাজ করে পূরণ করা হবে না। Existing mask field-ও
      // backward compatibility-এর জন্য আগের মতোই রাখা হচ্ছে।
      var full = String(mobile || '').replace(/\D/g, '');
      var row = {
        id: MOD.uuid(), staff_code: MOD._session.code,
        target_mobile_mask: MOD.maskMobile(full || mobile),
        target_mobile: full || null,
        call_date: MOD.todayIST()
      };
      /* 🔴🔒 V1338 (১১.০৯.২০২৬, TK-নির্দেশ, JPE-CRP ১৭-বনাম-৪ Android-এর একই
         সমস্যা এখানেও যাচাই করে পাওয়া) — আগে সরাসরি `sb...insert()`, ব্যর্থ
         হলে (নেট/tel: লিংকে ট্যাবটা সরে যাওয়ার আগেই) সেই কল-লগ চিরকালের
         জন্য হারিয়ে যেত, কোনো retry ছিল না। এখন প্রকল্পের নিজের প্রতিষ্ঠিত
         `MOD.save()` (local-first + id-upsert + ব্যর্থ হলে MOD.queueWrite,
         পরে MOD.gate()-এর flushQueue() আবার পাঠায়) — অন্য সব লেখার মতোই। */
      await MOD.save('wn', 'call_taps', row);
    } catch (e) {}
  };

  // Passive, additive listener: when any element that dials (href/onclick
  // containing tel:) is activated AND a module session exists, count the press.
  // It does NOT intercept or change the call in any way.
  MOD.attachCallLogger = function () {
    if (MOD._callLoggerOn) return; MOD._callLoggerOn = true;
    document.addEventListener('click', function (ev) {
      try {
        if (!MOD._session) return;
        var el = ev.target;
        for (var i = 0; i < 4 && el; i++, el = el.parentElement) {
          var hay = (el.getAttribute && (el.getAttribute('href') || el.getAttribute('onclick'))) || '';
          if (hay.indexOf('tel:') >= 0) {
            var mm = (hay.match(/tel:\+?(\d{6,})/) || [])[1] || '';
            MOD.logCallTap(mm);
            break;
          }
        }
      } catch (e) {}
    }, true);
  };

  // সাধারণ "৩ বার চাপ" লক — app.js-এর wlv1BranchLock-এর হুবহু একই প্যাটার্ন,
  // যেকোনো ছোট মডিউল স্ক্রিনে পুনর্ব্যবহারের জন্য (যেমন Staff Photo change)।
  MOD.tripleTap = function (el, onUnlock) {
    if (!el) return;
    var taps = 0, last = 0;
    el.addEventListener('click', function (e) {
      var now = Date.now();
      if (now - last > 1200) taps = 0;
      last = now; taps++;
      if (taps >= 3) { taps = 0; onUnlock(); }
    });
  };


  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { MOD.restore(); MOD.attachCallLogger(); });
  } else { MOD.restore(); MOD.attachCallLogger(); }

  // WhatsApp share of plain text (reuses the phone/browser share sheet).
  MOD.whatsapp = function (text) {
    var url = 'https://wa.me/?text=' + encodeURIComponent(text || '');
    try { window.open(url, '_blank'); } catch (e) { location.href = url; }
  };

  // Print / Save-as-PDF of an HTML fragment via a clean print window
  // (reuses the browser's native "Save as PDF" — no new library, free).
  MOD.printHtml = function (title, innerHtml) {
    var w = window.open('', '_blank');
    if (!w) return;
    w.document.write('<!doctype html><html><head><meta charset="utf-8"><title>' +
      MOD.esc(title) + '</title><style>body{font-family:system-ui,Arial,sans-serif;padding:18px;color:#111}' +
      'h1{font-size:18px;margin:0 0 6px}h2{font-size:14px;margin:14px 0 4px}' +
      'table{border-collapse:collapse;width:100%;margin:6px 0}td,th{border:1px solid #bbb;padding:5px 7px;font-size:12px;text-align:left}' +
      '.tot{font-weight:700}.muted{color:#666;font-size:11px}</style></head><body>' +
      innerHtml + '<script>window.onload=function(){setTimeout(function(){window.print();},250);}<\/script></body></html>');
    w.document.close();
  };
})();
