const { chromium } = require('/home/claude/.npm-global/lib/node_modules/playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));

  await page.goto('file:///home/claude/work/duptest/test.html', { waitUntil: 'load' });
  await page.waitForFunction(() => document.title === 'READY', { timeout: 5000 });

  const results = {};

  // ===== SETUP: seed one existing Enquiry + one existing registered Patient =====
  await page.evaluate(() => {
    let e = { id: uid('enq'), name: 'Existing Enquiry Person', mobile: '9876500001', branch: 'Kishanganj', disease: 'Piles', remarks: '', status: 'Active', stage: 'Inquiry', callCount: 0, receivedBy: 'TEST', createdBy: 'TEST', date: today(), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
    add('enquiries', e);
    ensureFollow(e, 'Inquiry', today(), '');

    let p = { id: uid('pat'), patientId: patientId('Kishanganj', today()), date: today(), registrationDate: today(), visitDate: today(), name: 'Existing Registered Patient', mobile: normMob('9876500002'), branch: 'Kishanganj', age: '30', sex: 'Male', address: 'Test', disease: 'Fissure', diagnosis: 'Fissure', complaint: '', createdBy: 'TEST', registeredBy: 'TEST', stage: 'Doctor Queue', queue: true, doctorComplete: false, bill: 0, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
    let rows = load('patients'); rows.unshift(p); save('patients', rows);
    ensureFollow({ ...p, refId: p.id }, 'Patient', '', 'Registered patient / Visit created');
  });

  // ===== TEST 1: Enquiry Form — duplicate warning while typing an EXISTING PATIENT's number =====
  await page.evaluate(() => { enquiryForm(); });
  await page.waitForSelector('#eMob');
  await page.click('#eMob');
  await page.keyboard.type('9876500002', { delay: 10 }); // matches the existing PATIENT seeded above
  await page.waitForTimeout(200);
  results.enquiryForm_duplicatePopupShown = await page.$eval('#modalRoot', el => el.innerHTML.length > 0);
  results.enquiryForm_popupMentionsExisting = await page.$eval('#modalRoot', el => el.innerText.includes('আগে থেকেই সিস্টেমে আছে'));
  await page.evaluate(() => { try { closeModal(); } catch (e) {} });

  // ===== TEST 2: Registration Form — duplicate warning + Continue Registration button works =====
  await page.evaluate(() => { registration(); });
  await page.waitForSelector('#pMob');
  await page.click('#pMob');
  await page.keyboard.type('9876500002', { delay: 10 }); // same existing patient
  await page.waitForTimeout(200);
  results.registrationForm_duplicatePopupShown = await page.$eval('#modalRoot', el => el.innerHTML.length > 0);
  results.registrationForm_hasContinueButton = await page.$eval('#modalRoot', el => el.innerHTML.includes('v280ContinueRegistration'));
  // Click "Continue Registration" and verify it actually opens the registration form pre-filled (was broken before fix)
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('#modalRoot button'));
    const btn = btns.find(b => /Continue Registration/i.test(b.textContent));
    if (btn) btn.click();
  });
  await page.waitForTimeout(300);
  results.continueRegistration_opensForm = await page.$('#pMob') !== null;
  results.continueRegistration_prefilledMobile = await page.$eval('#pMob', el => el.value).catch(() => 'FIELD_NOT_FOUND');

  // ===== TEST 3: +91 vs 10-digit treated as the same number =====
  results.sameNumberDifferentFormats = await page.evaluate(() => {
    const a = mob('9876500002');
    const b = mob('+919876500002');
    const c = mob('919876500002');
    return { raw: a, plus91: b, noPlus: c, allEqual: (a === b && b === c) };
  });

  // ===== TEST 4: second Registration for an ALREADY-REGISTERED mobile must NOT create a new patient =====
  const beforeCount = await page.evaluate(() => load('patients').filter(p => mob(p.mobile) === '9876500002').length);
  await page.evaluate(() => { registration(); });
  await page.waitForSelector('#pMob');
  await page.fill('#pMob', '9876500002');
  await page.fill('#pName', 'Existing Registered Patient (re-registered)');
  await page.selectOption('#pBranch', { index: 0 }).catch(() => {});
  await page.fill('#regFee', '200');
  // dismiss any duplicate popup that may appear on blur/typing before save
  await page.evaluate(() => { try { closeModal(); } catch (e) {} });
  await page.click('button.fullSave');
  await page.waitForTimeout(400);
  const afterCount = await page.evaluate(() => load('patients').filter(p => mob(p.mobile) === '9876500002').length);
  results.patientCount_beforeSecondRegistration = beforeCount;
  results.patientCount_afterSecondRegistration = afterCount;
  results.noDuplicatePatientCreated = (afterCount === 1);

  // Confirm it's the SAME patient id (updated, not replaced by a new id) and same registeredBy/createdAt preserved
  results.patientRecordAfterMerge = await page.evaluate(() => {
    const rows = load('patients').filter(p => mob(p.mobile) === '9876500002');
    return rows.map(r => ({ id: r.id, name: r.name, createdAt: r.createdAt }));
  });

  // ===== TEST 5: Follow-up — no duplicate 'Patient' stage row created for the same mobile =====
  results.followupPatientStageRowCount = await page.evaluate(() => {
    return load('followups').filter(f => mob(f.mobile) === '9876500002' && f.stage === 'Patient').length;
  });

  // ===== TEST 6: Global Search — de-duplicated, single result for that mobile =====
  await page.evaluate(() => { searchPage(); });
  await page.waitForTimeout(100);
  await page.fill('.capsuleGlobalSearch', '9876500002');
  await page.evaluate(() => { searchDo(document.querySelector('.capsuleGlobalSearch').value); });
  await page.waitForTimeout(150);
  results.globalSearch_cardCount = await page.$$eval('#sres .card', els => els.length);

  results.jsErrors = errors;
  require('fs').writeFileSync('/home/claude/work/duptest/results.json', JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results, null, 2));

  await browser.close();
})();
