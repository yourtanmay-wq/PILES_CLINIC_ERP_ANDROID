const { chromium } = require('/home/claude/.npm-global/lib/node_modules/playwright');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  await page.goto('file:///home/claude/work/mobtest/test.html', { waitUntil: 'load' });
  await page.waitForFunction(() => document.title === 'ENQ_FORM_READY', { timeout: 5000 }).catch(()=>{});
  await page.waitForTimeout(200);

  const r = await page.evaluate(() => {
    const out = {};
    // Seed 2 patients in different branches, created by different staff
    let pA = { id: uid('pat'), patientId: patientId('Kishanganj', today()), name: 'Branch A Patient', mobile: normMob('9111111111'), branch: 'Kishanganj', createdBy: '9883605917', receivedBy: '9883605917', stage: 'Doctor Queue', queue: true, doctorComplete: false, bill: 0, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
    let pB = { id: uid('pat'), patientId: patientId('Jalpaiguri', today()), name: 'Branch B Patient', mobile: normMob('9222222222'), branch: 'Jalpaiguri', createdBy: '9647840067', receivedBy: '9647840067', stage: 'Doctor Queue', queue: true, doctorComplete: false, bill: 0, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
    let rows = load('patients'); rows.unshift(pA); rows.unshift(pB); save('patients', rows);

    // Test as MASTER (branch='All') — should see both
    user = { mobile: '8001080080', name: 'MASTER', branch: 'All', role: 'master' };
    out.master_seesBoth = scoped(load('patients')).some(x=>x.id===pA.id) && scoped(load('patients')).some(x=>x.id===pB.id);

    // Test as Kishanganj staff — should see ONLY Branch A (their own branch), NOT Branch B
    user = { mobile: '9883605917', name: 'KNE-LAXMI', branch: 'Kishanganj', role: 'staff' };
    let staffAView = scoped(load('patients'));
    out.staffA_seesOwnBranch = staffAView.some(x=>x.id===pA.id);
    out.staffA_hidesOtherBranch = !staffAView.some(x=>x.id===pB.id);

    // Test as Jalpaiguri staff — should see ONLY Branch B, NOT Branch A
    user = { mobile: '9647840067', name: 'JPE-CRP', branch: 'Jalpaiguri', role: 'staff' };
    let staffBView = scoped(load('patients'));
    out.staffB_seesOwnBranch = staffBView.some(x=>x.id===pB.id);
    out.staffB_hidesOtherBranch = !staffBView.some(x=>x.id===pA.id);

    // Test cross-branch creator override: a Kishanganj staff who happens to be logged in
    // under a DIFFERENT branch setting but created the Branch B record should still see it
    // (per the createdBy/receivedBy override in canSeeFinal/inScope)
    user = { mobile: '9647840067', name: 'JPE-CRP', branch: 'Kishanganj', role: 'staff' }; // branch mismatch on purpose
    let creatorOverrideView = scoped(load('patients'));
    out.creatorOverride_stillSeesOwnCreatedRecord = creatorOverrideView.some(x=>x.id===pB.id);

    return out;
  });

  console.log(JSON.stringify(r, null, 2));
  await browser.close();
})();
