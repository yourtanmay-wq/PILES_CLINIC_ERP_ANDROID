/* 🔵 V1294 (১০.০৯.২০২৬) — তালিকা ৪১১-⑬ (ক): সাপ্তাহিক স্বয়ংক্রিয় কপি ডেটাবেসের বাইরে।
   GitHub Actions (.github/workflows/weekly_backup.yml) থেকে চলে। Supabase-এ শুধু **পড়া** — কিছু লেখা হয় না।
   ফল: backups/<বছর>/PILES_CLINIC_backup_<তারিখ>.json.gz — কম্পিউটার-অ্যাপের Backup Center → "Restore JSON"
   যে ছাঁচ বোঝে ({meta,data}) ঠিক সেই ছাঁচে, তাই দরকারে সরাসরি ফিরিয়ে আনা যায়।
   · প্রতি সপ্তাহ: ছবি ছাড়া (~৮ MB নামে, ~১.৫ MB জমে) · মাসের প্রথম রান: ছবিসহ (~২০ MB নামে)।
   · usercredentials/message_log ইচ্ছে করে বাদ (পাসওয়ার্ড-জাতীয়; ক্লিনিকের তথ্য নয়)। */
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const URL_ = process.env.SUPABASE_URL, KEY = process.env.SUPABASE_KEY;
if (!URL_ || !KEY) { console.error('SUPABASE_URL/SUPABASE_KEY নেই'); process.exit(1); }
const WITH_PHOTOS = process.env.WITH_PHOTOS === '1';
const OUT_DIR = process.env.OUT_DIR || 'backups';
// কম্পিউটার-অ্যাপের TABLES (Restore JSON-এর জন্য সবগুলো থাকতেই হবে) + বাড়তি টেবিল
const APP_TABLES = ['enquiries','patients','payments','followups','medical','products','doctor_visits','briefings','trash','address_tags'];
const EXTRA_TABLES = ['deleted_records','chamber_close','payment_backdate_requests','payment_edit_requests','backdate_payment_grants','registration_count_excluded','referral_edit_requests','rmp_commission_payments','rmp_advance_payments','leave_requests','medicine_defaults'];
const PHOTO_COLS = { patients: ['photo'], followups: ['photo'], medical: ['photos'] };
const PAGE = 1000;
async function rest(table, from, to) {
  const r = await fetch(`${URL_}/rest/v1/${table}?select=*&order=id.asc`, {
    headers: { apikey: KEY, Authorization: 'Bearer ' + KEY, Range: `${from}-${to}`, 'Range-Unit': 'items', Prefer: 'count=exact' }
  });
  if (r.status === 404) return { missing: true };
  if (!r.ok && r.status !== 206 && r.status !== 416) throw new Error(`${table} HTTP ${r.status}: ${(await r.text()).slice(0, 200)}`);
  const total = Number(((r.headers.get('content-range') || '').split('/')[1]) || -1);
  const rows = r.status === 416 ? [] : await r.json();
  return { rows: Array.isArray(rows) ? rows : [], total };
}
async function dump(table) {
  const all = []; let total = -1;
  for (let from = 0; ; from += PAGE) {
    const p = await rest(table, from, from + PAGE - 1);
    if (p.missing) { console.log(`  ${table}: টেবিল নেই — বাদ`); return null; }
    total = p.total; all.push(...p.rows);
    if (p.rows.length < PAGE) break;
  }
  if (total >= 0 && all.length !== total) throw new Error(`${table}: ${all.length} সারি পড়া, কিন্তু সার্ভার বলে ${total} — অসম্পূর্ণ, ব্যাকআপ বাতিল`);
  if (!WITH_PHOTOS && PHOTO_COLS[table]) for (const r of all) for (const c of PHOTO_COLS[table]) if (r && typeof r[c] === 'string' && r[c].length > 100) r[c] = '';
  console.log(`  ${table}: ${all.length} সারি`);
  return all;
}
(async () => {
  const now = new Date(), ist = new Date(now.getTime() + 5.5 * 3600 * 1000);
  const dateIST = ist.toISOString().slice(0, 10);
  const data = {}, extra = {};
  console.log(WITH_PHOTOS ? 'ছবিসহ (মাসিক)' : 'ছবি ছাড়া (সাপ্তাহিক)');
  for (const t of APP_TABLES) { const rows = await dump(t); data[t] = rows || []; }
  for (const t of EXTRA_TABLES) { const rows = await dump(t); if (rows) extra[t] = rows; }
  const payload = { meta: { app: 'PILES CLINIC ERP', version: 'github-weekly', reason: WITH_PHOTOS ? 'monthly_full' : 'weekly', date: dateIST, createdAt: now.toISOString(), createdBy: 'github-actions', tables: APP_TABLES, withPhotos: WITH_PHOTOS }, data, extra };
  const dir = path.join(OUT_DIR, dateIST.slice(0, 4)); fs.mkdirSync(dir, { recursive: true });
  const name = `PILES_CLINIC_backup_${dateIST}${WITH_PHOTOS ? '_with_photos' : ''}.json.gz`;
  const text = JSON.stringify(payload), gz = zlib.gzipSync(Buffer.from(text), { level: 9 });
  fs.writeFileSync(path.join(dir, name), gz);
  const counts = Object.fromEntries(Object.entries(data).map(([k, v]) => [k, v.length]));
  fs.writeFileSync(path.join(OUT_DIR, 'LAST_BACKUP.json'), JSON.stringify({ date: dateIST, file: `${dateIST.slice(0, 4)}/${name}`, withPhotos: WITH_PHOTOS, jsonMB: +(text.length / 1048576).toFixed(2), gzMB: +(gz.length / 1048576).toFixed(2), counts, extra: Object.fromEntries(Object.entries(extra).map(([k, v]) => [k, v.length])) }, null, 1));
  console.log(`লেখা হলো ${dir}/${name}: JSON ${(text.length / 1048576).toFixed(2)} MB → gz ${(gz.length / 1048576).toFixed(2)} MB`);
  if (!counts.patients || !counts.payments) throw new Error('patients/payments ফাঁকা — ব্যাকআপ সন্দেহজনক, বাতিল');
})().catch(e => { console.error('❌', e.message || e); process.exit(1); });
