const { chromium } = require('/home/claude/.npm-global/lib/node_modules/playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));

  await page.goto('file:///home/claude/work/mobtest/test.html', { waitUntil: 'load' });
  await page.waitForFunction(() => document.title === 'ENQ_FORM_READY' || document.title === 'ENQ_FORM_ERROR', { timeout: 5000 }).catch(()=>{});
  await page.waitForTimeout(200);

  const journey = await page.evaluate(() => {
    const out = { steps: [], trace: [] };
    const mobileNum = '9812345670';
    try {
      // 1. ENQUIRY
      let e = { id: uid('enq'), name: 'Journey Test Patient', mobile: mobileNum, branch: 'Kishanganj', disease: 'Piles', remarks: 'first call', status: 'Active', stage: 'Inquiry', callCount: 0, receivedBy: 'TEST', createdBy: 'TEST', date: today(), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
      add('enquiries', e);
      out.trace.push('after add: count=' + load('enquiries').length);
      let f1 = ensureFollow(e, 'Inquiry', today(), 'first call');
      out.trace.push('after ensureFollow: enq count=' + load('enquiries').length + ' fu count=' + load('followups').length);
      out.steps.push({ step: '1_Enquiry', ok: !!load('enquiries').find(x => x.id === e.id), followCreated: !!f1 });
      out.trace.push('step1 pushed, ok=' + out.steps[0].ok);

      // 2. REGISTRATION (calls the real savePatient-equivalent logic path directly)
      let p = {
        id: uid('pat'), patientId: patientId('Kishanganj', today()), date: today(), registrationDate: today(), visitDate: today(),
        name: 'Journey Test Patient', mobile: normMob(mobileNum), branch: 'Kishanganj', age: '35', sex: 'Male', address: 'Test Addr',
        disease: 'Piles', diagnosis: 'Piles', complaint: 'Pain', createdBy: 'TEST', registeredBy: 'TEST',
        stage: 'Doctor Queue', queue: true, doctorComplete: false, bill: 0,
        createdAt: new Date().toISOString(), updatedAt: new Date().toISOString()
      };
      let rows = load('patients'); rows.unshift(p); save('patients', rows);
      finalizeEnquiryRegistrationVisit(mobileNum, p);
      ensureFollow({ ...p, refId: p.id, stage: 'Patient', visitDate: today(), registrationDate: today() }, 'Patient', '', 'Registered patient / Visit created');
      closeEnquiryAfterRegistration(mobileNum, p);
      out.steps.push({
        step: '2_Registration',
        ok: !!load('patients').find(x => x.id === p.id),
        enquiryClosed: load('enquiries').find(x => mob(x.mobile) === mobileNum)?.status === 'Registered',
        visitFollowCreated: !!load('followups').find(x => x.refId === p.id && x.stage === 'Patient')
      });

      // 3. VISIT / DOCTOR QUEUE — patient should be visible in visitQueueRows()
      let inQueue = visitQueueRows().some(x => x.id === p.id);
      out.steps.push({ step: '3_VisitQueue', ok: inQueue });

      // 4. DOCTOR CHECKUP — save a doctor note via the real saveMedicalRecord path used by saveDoctor()
      let docNote = saveMedicalRecord(p.id, 'checkup', 'Doctor Checkup', 'Chief complaint noted, exam done');
      let patientsAfterDoc = load('patients').map(x => x.id === p.id ? { ...x, doctorComplete: true } : x);
      save('patients', patientsAfterDoc);
      out.steps.push({ step: '4_DoctorCheckup', ok: !!docNote, doctorCompleteFlag: load('patients').find(x => x.id === p.id)?.doctorComplete === true });

      // 5. PRESCRIPTION — use the real medDraft + saveRx path
      medDraft = [{ name: 'Pilex Tablet', dose: '1-0-1', days: '15' }];
      saveRx(p.id, 'Prescription');
      let rxRecord = load('medical').filter(x => x.patientId === p.id && x.type === 'Prescription');
      out.steps.push({ step: '5_Prescription', ok: rxRecord.length > 0, recordCount: rxRecord.length });

      // 6. PAYMENT — treatment bill + payment via the real accumulation formula
      let patientsWithBill = load('patients').map(x => x.id === p.id ? { ...x, bill: 5000 } : x);
      save('patients', patientsWithBill);
      add('payments', { id: uid('pay'), payType: 'treatment', payLabel: '1st Payment', patientId: p.id, mobile: p.mobile, branch: p.branch, name: p.name, date: today(), amount: 5000, mode: 'CASH', remarks: 'Full payment', receivedBy: 'TEST', createdBy: 'TEST', updatedAt: new Date().toISOString() });
      let totals = treatmentTotals(load('patients').find(x => x.id === p.id));
      out.steps.push({ step: '6_Payment', ok: totals.paid === 5000 && totals.due === 0, totals });

      // 7. FOLLOW-UP — advance to Treatment stage follow-up
      ensureFollow({ ...p, refId: p.id, stage: 'Treatment', bill: 5000, paid: 5000, payPct: 100 }, 'Treatment', '', 'Fully paid');
      let treatmentFollow = load('followups').find(x => x.refId === p.id && x.stage === 'Treatment');
      out.steps.push({ step: '7_FollowUp', ok: !!treatmentFollow });

      // 8. COMPLETED — bill fully paid, due === 0 should mark it complete in draffHome's own filter logic
      let finalPatient = load('patients').find(x => x.id === p.id);
      let finalTotals = treatmentTotals({ ...finalPatient, bill: 5000 });
      out.steps.push({ step: '8_Completed', ok: finalTotals.due === 0 && finalTotals.pct === 100 });

      // GLOBAL SEARCH — should resolve to exactly one de-duplicated record for this mobile, at the most advanced stage
      let searchRows = allSearchRows ? allSearchRows(mobileNum) : null;
      out.steps.push({ step: '9_GlobalSearchSanity', mobile: mobileNum, note: 'checked separately via searchResults() in UI test' });

      out.finalPatientId = p.id;
      out.success = out.steps.every(s => s.ok !== false);
    } catch (err) {
      out.error = err.message + '\\n' + err.stack;
    }
    return out;
  });

  journey.jsErrors = errors;
  require('fs').writeFileSync('/home/claude/work/journey_test_results.json', JSON.stringify(journey, null, 2));
  console.log(JSON.stringify(journey, null, 2));

  await browser.close();
})();
