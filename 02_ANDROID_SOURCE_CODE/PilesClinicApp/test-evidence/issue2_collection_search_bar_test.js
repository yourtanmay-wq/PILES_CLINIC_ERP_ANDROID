const { chromium } = require('/home/claude/.npm-global/lib/node_modules/playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('console.error: ' + m.text()); });

  await page.goto('file:///home/claude/work/testharness/test.html', { waitUntil: 'load' });
  await page.waitForTimeout(300);

  const result = await page.evaluate(() => {
    const out = { checks: [] };

    // Seed a master session so boot()/dashboard() render as master (no real login UI needed).
    user = { role: 'master', branch: 'All', name: 'Test Master', mobile: '8001080080' };
    try { localStorage.setItem('rk_session', JSON.stringify(user)); } catch (e) {}

    // Seed a few real payment records for "today" across two branches so the
    // Collection pages have real cash/online/total data to render, exactly the
    // way collectionRows()/overview() expect (see app.js).
    const t = today();
    const pays = [
      { id: uid('pay'), patientId: 'p1', name: 'Test Patient A', mobile: '9812345670', branch: 'Kishanganj', mode: 'Cash', amount: 500, payType: 'treatment', date: t, receivedBy: 'KNE-LAXMI' },
      { id: uid('pay'), patientId: 'p2', name: 'Test Patient B', mobile: '9812345671', branch: 'Jalpaiguri', mode: 'UPI', amount: 800, payType: 'treatment', date: t, receivedBy: 'JPE-CRP' },
    ];
    save('payments', pays);

    // --- 1. Branch-wise Collection (Today) page ---
    branchWiseCollectionPage();
    let html = document.getElementById('app').innerHTML;
    out.checks.push({
      page: 'Branch-wise Collection (Today)',
      searchBarRemoved: !html.includes('globalCapsuleWrap'),
      hasBranchData: html.includes('Kishanganj') && html.includes('Jalpaiguri'),
      hasAmounts: html.includes('500') || html.includes('₹500') || /500/.test(html)
    });

    // --- 2. Branch collection detail (tap into one branch) ---
    branchCollectionDetail('Kishanganj');
    html = document.getElementById('app').innerHTML;
    out.checks.push({
      page: 'Branch Collection Detail (Kishanganj)',
      searchBarRemoved: !html.includes('globalCapsuleWrap'),
      hasCashOnlineTotal: html.includes('Cash') && html.includes('Online') && html.includes('Total'),
      hasPatientRow: html.includes('Test Patient A')
    });

    // --- 3. Payment Collection / Today Collection hub page ---
    paymentHome();
    html = document.getElementById('app').innerHTML;
    out.checks.push({
      page: 'Payment Collection (Today Collection hub)',
      searchBarRemoved: !html.includes('globalCapsuleWrap'),
      hasSummary: html.includes('TODAY COLLECTION SUMMARY'),
      hasTotal: html.includes('Total Collection'),
      hasCashUpi: html.includes('Cash Collection') && html.includes('UPI Collection')
    });

    // --- 4. Collection list (Cash / Online / Monthly / History) ---
    collectionList('Cash');
    html = document.getElementById('app').innerHTML;
    out.checks.push({
      page: 'Collection List - Cash',
      searchBarRemoved: !html.includes('globalCapsuleWrap'),
      hasCorrectRow: html.includes('Test Patient A') && !html.includes('Test Patient B'),
      titleCorrect: html.includes('Cash Collection')
    });

    // --- 5. Sanity: a normal (non-collection) page STILL keeps the global search bar ---
    dashboard();
    html = document.getElementById('app').innerHTML;
    out.checks.push({
      page: 'Dashboard (control - should be unaffected)',
      searchBarStillPresent: html.includes('globalCapsuleWrap')
    });

    return out;
  });

  console.log(JSON.stringify({ result, jsErrors: errors }, null, 2));
  await browser.close();
})();
