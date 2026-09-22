/* ============================================================================
 * V223 AUTOMATED LOGIC TESTS  (run:  node 11_V223_TESTS/V223_logic_tests.js)
 * ----------------------------------------------------------------------------
 * এই ফাইলটা V223-এর প্রতিটি নতুন নিয়মের সিদ্ধান্ত-লজিক (decision logic) হুবহু
 * পোর্ট করে ও সব দৃশ্য (scenario) পরীক্ষা করে — পুরোনো/newer data, cloud-read
 * failure, simultaneous save, pending preservation, false success, দুই রোগীর
 * একই mobile, crash-retry। এ ছাড়া **আসল web app.js**-এর refund id/nonce ফাংশন
 * সরাসরি লোড করে চালিয়ে Android↔Web parity প্রমাণ করে।
 * Kotlin runtime এই পরিবেশে নেই, তাই Kotlin-এর সিদ্ধান্ত-লজিক এখানে হুবহু
 * mirror করা হয়েছে (SupabaseClient.kt / CloudWriteQueue.kt-এর সঙ্গে লাইন-বাই-লাইন)।
 * ========================================================================== */

const fs = require('fs');
const path = require('path');

let PASS = 0, FAIL = 0;
const fails = [];
function ok(name, cond) { if (cond) { PASS++; } else { FAIL++; fails.push(name); console.log('  ❌ ' + name); } }
function eq(name, a, b) { ok(name + ' (got ' + JSON.stringify(a) + ', want ' + JSON.stringify(b) + ')', a === b); }

/* ===========================================================================
 * PART A — LANDED detection (mirror of SupabaseClient.upsert/updateById)
 *   outcome: 0 = FAILED · 1 = LANDED · 2 = SUPERSEDED
 * ======================================================================== */
function upsertOutcome({httpSuccessful, verify, repr /* array of {updatedAt} */, sentUpdatedAt}) {
  if (!httpSuccessful) return 0;                 // FAILED
  if (!verify) return 1;                          // updatedAt-less table → LANDED (as before)
  const arr = Array.isArray(repr) ? repr : [];
  if (arr.length === 0) return 1;                 // empty representation → LANDED (as before)
  const retUpd = (arr[0] && arr[0].updatedAt) || '';
  if (!retUpd || !sentUpdatedAt) return 1;        // cannot compare → LANDED (as before)
  if (retUpd === sentUpdatedAt) return 1;         // our data won → LANDED
  return 2;                                        // cloud kept newer → SUPERSEDED
}
// Mirror of the when(outcome) side-effects we care about:
function upsertEffects(outcome) {
  return {
    returned: outcome !== 0,                       // caller-visible boolean
    clearConfirmed: outcome === 1,                 // only on LANDED
    remember: outcome === 0,                        // only on FAILED (retry)
  };
}

// no trigger applied yet: merge-duplicates always writes our row → returned updatedAt == sent
eq('A1 no-trigger conflict → LANDED', upsertOutcome({httpSuccessful:true, verify:true, repr:[{updatedAt:'2026-08-01T10:00:00.000Z'}], sentUpdatedAt:'2026-08-01T10:00:00.000Z'}), 1);
// trigger blocks our OLDER write → cloud keeps newer → returned != sent → SUPERSEDED
eq('A2 trigger blocks old write → SUPERSEDED', upsertOutcome({httpSuccessful:true, verify:true, repr:[{updatedAt:'2026-08-01T12:00:00.000Z'}], sentUpdatedAt:'2026-08-01T09:00:00.000Z'}), 2);
// our NEWER write wins even with trigger → LANDED
eq('A3 newer write wins → LANDED', upsertOutcome({httpSuccessful:true, verify:true, repr:[{updatedAt:'2026-08-01T15:00:00.000Z'}], sentUpdatedAt:'2026-08-01T15:00:00.000Z'}), 1);
// network/server failure → FAILED
eq('A4 http fail → FAILED', upsertOutcome({httpSuccessful:false, verify:true, repr:[], sentUpdatedAt:'x'}), 0);
// updatedAt-less table (deleted_records) → LANDED, minimal
eq('A5 updatedAt-less table → LANDED', upsertOutcome({httpSuccessful:true, verify:false, repr:[], sentUpdatedAt:''}), 1);

// Effects: only LANDED clears pending; SUPERSEDED must NOT clear pending or retry
ok('A6 LANDED clears pending', upsertEffects(1).clearConfirmed === true && upsertEffects(1).returned === true);
ok('A7 SUPERSEDED does NOT clear pending, does NOT retry, returns true', (() => { const e = upsertEffects(2); return e.clearConfirmed === false && e.remember === false && e.returned === true; })());
ok('A8 FAILED retries, returns false, no clear', (() => { const e = upsertEffects(0); return e.remember === true && e.returned === false && e.clearConfirmed === false; })());

/* ===========================================================================
 * PART B — clearConfirmed decision (mirror of CloudWriteQueue.clearConfirmed)
 *   Decide per queue entry: remove or keep.
 * ======================================================================== */
function clearDecision(entry, confirmedKind, confKeys, table, id, writeStart) {
  const k = entry.kind;
  const sameRow = (k === 'UPSERT' || k === 'UPDATE') && entry.table === table && entry.id === id;
  if (!sameRow) return 'keep';                      // other id / DELETE → keep
  const at = entry.at || 0;
  if (at > writeStart) return 'keep';               // queued during/after the write → keep (NEW)
  if (confirmedKind === 'UPSERT') return 'remove';  // full row supersedes UPSERT+UPDATE
  // confirmedKind === 'UPDATE': only subset UPDATE
  if (k !== 'UPDATE' || !confKeys) return 'keep';
  const storedKeys = Object.keys(entry.bodyObj || {});
  if (storedKeys.length === 0) return 'keep';
  const subset = storedKeys.every(x => confKeys.includes(x));
  return subset ? 'remove' : 'keep';
}

const WS = 1000; // writeStart
// UPSERT confirmed:
eq('B1 UPSERT confirmed clears older same-id UPSERT', clearDecision({kind:'UPSERT',table:'payments',id:'p1',at:500}, 'UPSERT', null, 'payments','p1', WS), 'remove');
eq('B2 UPSERT confirmed clears older same-id UPDATE', clearDecision({kind:'UPDATE',table:'payments',id:'p1',at:500,bodyObj:{remarks:'x'}}, 'UPSERT', null, 'payments','p1', WS), 'remove');
eq('B3 keeps NEWER pending (at>writeStart)', clearDecision({kind:'UPDATE',table:'payments',id:'p1',at:1500,bodyObj:{remarks:'new'}}, 'UPSERT', null, 'payments','p1', WS), 'keep');
eq('B4 never touches OTHER id', clearDecision({kind:'UPSERT',table:'payments',id:'p2',at:500}, 'UPSERT', null, 'payments','p1', WS), 'keep');
eq('B5 never touches DELETE', clearDecision({kind:'DELETE',table:'payments',id:'p1',at:500}, 'UPSERT', null, 'payments','p1', WS), 'keep');
// UPDATE confirmed:
eq('B6 UPDATE confirmed clears same-field older UPDATE (subset)', clearDecision({kind:'UPDATE',table:'followups',id:'f1',at:500,bodyObj:{remarks:'old'}}, 'UPDATE', ['remarks'], 'followups','f1', WS), 'remove');
eq('B7 UPDATE confirmed KEEPS disjoint-field older UPDATE', clearDecision({kind:'UPDATE',table:'followups',id:'f1',at:500,bodyObj:{nextFollow:'2026-08-05'}}, 'UPDATE', ['remarks'], 'followups','f1', WS), 'keep');
eq('B8 UPDATE confirmed does NOT clear an UPSERT of same id', clearDecision({kind:'UPSERT',table:'followups',id:'f1',at:500,bodyObj:{}}, 'UPDATE', ['remarks'], 'followups','f1', WS), 'keep');
eq('B9 UPDATE confirmed keeps NEWER same-field update (at>ws)', clearDecision({kind:'UPDATE',table:'followups',id:'f1',at:1500,bodyObj:{remarks:'newer'}}, 'UPDATE', ['remarks'], 'followups','f1', WS), 'keep');
// concern-2 wiring: a SUPERSEDED write never calls clearConfirmed at all (Part A A7) → so no pending removed.

/* ===========================================================================
 * PART C — Restore outcome (mirror of SupabaseClient.upsertRestoreSafe)
 *   cloudRead: 'FAIL' | 'NONE' | array[{updatedAt}]   ·  writeOk: bool
 * ======================================================================== */
function stampMs(s){ if(!s) return 0; const n = Date.parse(s); return Number.isFinite(n)?n:0; }
function restoreOutcome(row, cloudRead, writeOk) {
  const id = row.id || '';
  if (!id) return writeOk ? 'WRITTEN' : 'BLOCKED';
  if (cloudRead === 'FAIL') return 'BLOCKED';                 // read failed → never guess-write
  if (cloudRead === 'NONE' || (Array.isArray(cloudRead) && cloudRead.length === 0))
    return writeOk ? 'WRITTEN' : 'BLOCKED';                   // no cloud row → safe insert
  const incoming = stampMs(row.updatedAt);
  const cloud = stampMs(cloudRead[0] && cloudRead[0].updatedAt);
  if (incoming <= 0 || cloud <= 0) return 'BLOCKED';          // cannot compare → never guess
  if (cloud > incoming) return 'KEPT_NEWER';                  // cloud newer → keep
  return writeOk ? 'WRITTEN' : 'BLOCKED';                     // incoming >= cloud → restore
}
eq('C1 read FAIL → BLOCKED (no guess-write)', restoreOutcome({id:'p1',updatedAt:'2026-01-01T00:00:00.000Z'}, 'FAIL', true), 'BLOCKED');
eq('C2 cloud has NO row → WRITTEN (safe insert)', restoreOutcome({id:'p1',updatedAt:'2026-01-01T00:00:00.000Z'}, 'NONE', true), 'WRITTEN');
eq('C3 cloud NEWER → KEPT_NEWER (no overwrite)', restoreOutcome({id:'p1',updatedAt:'2026-01-01T00:00:00.000Z'}, [{updatedAt:'2026-08-01T00:00:00.000Z'}], true), 'KEPT_NEWER');
eq('C4 incoming NEWER → WRITTEN', restoreOutcome({id:'p1',updatedAt:'2026-08-01T00:00:00.000Z'}, [{updatedAt:'2026-01-01T00:00:00.000Z'}], true), 'WRITTEN');
eq('C5 cannot compare (no incoming stamp) → BLOCKED', restoreOutcome({id:'p1',updatedAt:''}, [{updatedAt:'2026-08-01T00:00:00.000Z'}], true), 'BLOCKED');
eq('C6 cannot compare (cloud stamp junk) → BLOCKED', restoreOutcome({id:'p1',updatedAt:'2026-08-01T00:00:00.000Z'}, [{updatedAt:'not-a-date'}], true), 'BLOCKED');
eq('C7 write itself fails → BLOCKED (not false success)', restoreOutcome({id:'p1',updatedAt:'2026-08-01T00:00:00.000Z'}, [{updatedAt:'2026-01-01T00:00:00.000Z'}], false), 'BLOCKED');

/* ===========================================================================
 * PART D — web wlv1CloudStampMs semantics (mirror) + wlv1RestoreTrash decision
 *   stamp: -2 read-fail(BLOCK) · -1 no-row(safe) · >=0 stamp (0 unknown)
 * ======================================================================== */
function webRestoreDecision(incMs, cloudMs) {
  if (cloudMs === -2) return 'BLOCK';
  if (cloudMs >= 0) {
    if (incMs <= 0 || cloudMs === 0) return 'BLOCK';
    if (cloudMs > incMs) return 'KEEP_NEWER';
  }
  return 'WRITE'; // cloudMs === -1 (no row) OR incMs >= cloudMs
}
eq('D1 web read-fail(-2) → BLOCK', webRestoreDecision(100, -2), 'BLOCK');
eq('D2 web no-row(-1) → WRITE', webRestoreDecision(100, -1), 'WRITE');
eq('D3 web cloud newer → KEEP_NEWER', webRestoreDecision(100, 200), 'KEEP_NEWER');
eq('D4 web incoming newer → WRITE', webRestoreDecision(200, 100), 'WRITE');
eq('D5 web cannot compare (inc 0) → BLOCK', webRestoreDecision(0, 200), 'BLOCK');
eq('D6 web cannot compare (cloud 0/unknown) → BLOCK', webRestoreDecision(200, 0), 'BLOCK');

/* ===========================================================================
 * PART E — REAL web refund functions loaded from app.js (Android↔Web parity)
 * ======================================================================== */
function extractFn(src, name) {
  const start = src.indexOf('function ' + name + '(');
  if (start < 0) throw new Error('fn not found: ' + name);
  let i = src.indexOf('{', start), depth = 0, j = i;
  for (; j < src.length; j++) { const c = src[j]; if (c === '{') depth++; else if (c === '}') { depth--; if (depth === 0) { j++; break; } } }
  return src.slice(start, j);
}
const appjs = fs.readFileSync(path.join(__dirname, '..', '03_NETLIFY_READY', 'app.js'), 'utf8');
const sandbox = { today: () => '2026-08-01', String, Math, Number };
const vm = require('vm');
vm.createContext(sandbox);
vm.runInContext(extractFn(appjs, 'wlv1JavaHash') + '\n' + extractFn(appjs, 'wlv1RefundIdFor') + '\n' + extractFn(appjs, 'wlv1RefundDraftKey') + '\n' +
  'this.wlv1RefundIdFor = wlv1RefundIdFor; this.wlv1RefundDraftKey = wlv1RefundDraftKey; this.wlv1JavaHash = wlv1JavaHash;', sandbox);
const webRefundId = sandbox.wlv1RefundIdFor;
const webDraftKey = sandbox.wlv1RefundDraftKey;
const javaHash = sandbox.wlv1JavaHash;

// Java String.hashCode reference (to model Android refundIdFor / refundNonceKey identically)
function jhash(s){ let h=0; for(let i=0;i<s.length;i++){ h=(Math.imul(31,h)+s.charCodeAt(i))|0; } return h; }
ok('E0 web wlv1JavaHash == Java String.hashCode', javaHash('rfnd_test|123|hello|2026-08-01') === jhash('rfnd_test|123|hello|2026-08-01'));

// Android refundIdFor mirror (must match web byte-for-byte): raw = patient.id|mob|amtCents|reason|today|req|nonce
function androidRefundId(p, amt, reason, req, nonce) {
  const mob = String(p.mobile||'').replace(/\D/g,'').slice(-10);
  const amtCents = Math.round(Number(amt||0)*100);
  const d = '2026-08-01';
  const reqD = String(req||'').replace(/\D/g,'').slice(-10);
  const raw = String(p.id||'')+'|'+mob+'|'+amtCents+'|'+String(reason||'').trim().toLowerCase()+'|'+d+'|'+reqD+'|'+String(nonce||'');
  const hex = (jhash(raw) >>> 0).toString(16);
  return 'rfnd_'+mob+'_'+amtCents+'_'+d.replace(/-/g,'')+'_'+hex;
}

// §5: two DIFFERENT patients sharing the SAME mobile, same amount/reason/day/staff/nonce
const pA = { id: 'PAT-A', mobile: '+919876543210' };
const pB = { id: 'PAT-B', mobile: '9876543210' };   // same last-10 digits, different record id
const webIdA = webRefundId(pA, 500, 'cancel', '+911111111111', 'NONCE1');
const webIdB = webRefundId(pB, 500, 'cancel', '+911111111111', 'NONCE1');
ok('E1 same-mobile two patients → DIFFERENT refund id (web)', webIdA !== webIdB);
eq('E2 Android refundId == Web refundId (parity, patient A)', androidRefundId(pA,500,'cancel','+911111111111','NONCE1'), webIdA);
eq('E3 Android refundId == Web refundId (parity, patient B)', androidRefundId(pB,500,'cancel','+911111111111','NONCE1'), webIdB);

// draft-key includes patient.id → two same-mobile patients never share a nonce slot
const keyA = webDraftKey(pA, 500, 'cancel');
const keyB = webDraftKey(pB, 500, 'cancel');
ok('E4 same-mobile two patients → DIFFERENT nonce draft-key', keyA !== keyB);

// crash-retry: SAME patient + same amount/reason/day → SAME draft-key → same persisted nonce → same id
eq('E5 same patient same refund → SAME draft-key (crash-retry no dup)', webDraftKey(pA,500,'cancel'), webDraftKey(pA,500,'cancel'));
ok('E6 same patient, persisted nonce reused → SAME id (no duplicate)', webRefundId(pA,500,'cancel','+911111111111','KEEP') === webRefundId(pA,500,'cancel','+911111111111','KEEP'));
// two VALID different refunds (different nonce after confirm-clear) → different id
ok('E7 two valid refunds (different nonce) → DIFFERENT id', webRefundId(pA,500,'cancel','+911111111111','N1') !== webRefundId(pA,500,'cancel','+911111111111','N2'));
// different amount/reason → different id regardless
ok('E8 different amount → different id', webRefundId(pA,500,'cancel','+911111111111','N1') !== webRefundId(pA,700,'cancel','+911111111111','N1'));

/* ===========================================================================
 * PART F — simultaneous-save / pending-preservation end-to-end scenario
 *   Stale UPSERT replay is trigger-blocked (SUPERSEDED). It must NOT clear a
 *   concurrent NEW pending edit for the same row.
 * ======================================================================== */
(function(){
  // Queue holds a NEW pending UPDATE (remark) for row p1, queued AT 1500 (after a write started at 1000)
  const queue = [{kind:'UPDATE',table:'payments',id:'p1',at:1500,bodyObj:{remarks:'brand new'}}];
  // A stale UPSERT replay for p1 gets SUPERSEDED by trigger → upsert returns true but clearConfirmed is NOT called.
  const superseded = upsertEffects(2);
  ok('F1 SUPERSEDED replay does not invoke clearConfirmed', superseded.clearConfirmed === false);
  // Even if clearConfirmed WERE (wrongly) called with writeStart=1000, the new pending (at 1500) is preserved:
  const decision = clearDecision(queue[0], 'UPSERT', null, 'payments','p1', 1000);
  eq('F2 new pending survives even a same-id UPSERT clear (time-guard)', decision, 'keep');
})();

/* ===========================================================================
 * RESULT
 * ======================================================================== */
console.log('\n================ V223 LOGIC TESTS ================');
console.log('PASS: ' + PASS + '   FAIL: ' + FAIL);
if (FAIL > 0) { console.log('FAILED: ' + fails.join(' | ')); process.exit(1); }
console.log('ALL PASS ✅');
