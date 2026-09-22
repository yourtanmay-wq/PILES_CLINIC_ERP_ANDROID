const { chromium } = require('/home/claude/.npm-global/lib/node_modules/playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));

  await page.goto('file:///home/claude/work/mobtest/test.html', { waitUntil: 'load' });
  await page.waitForSelector('#eMob', { timeout: 5000 });
  await page.waitForTimeout(200);

  const results = {};
  await page.click('#eMob');
  await page.keyboard.type('9876543210', { delay: 20 });
  results.afterTyping10Digits = await page.$eval('#eMob', el => el.value);
  results.badgeVisibleAfter10 = await page.$eval('#eMobPrefix', el => !el.classList.contains('hidden'));

  await page.keyboard.press('Backspace');
  results.afterOneBackspace = await page.$eval('#eMob', el => el.value);
  results.badgeHiddenAfterBackspace = await page.$eval('#eMobPrefix', el => el.classList.contains('hidden'));

  for (let i = 0; i < 9; i++) await page.keyboard.press('Backspace');
  results.afterFullBackspace = await page.$eval('#eMob', el => el.value);

  await page.keyboard.type('9123456789', { delay: 10 });
  await page.evaluate(() => { const el = document.getElementById('eMob'); el.setSelectionRange(3, 3); });
  await page.keyboard.press('Delete');
  results.afterMiddleDelete = await page.$eval('#eMob', el => el.value);
  results.cursorPosAfterMiddleDelete = await page.$eval('#eMob', el => el.selectionStart);

  results.jsErrors = errors;
  console.log(JSON.stringify(results, null, 2));
  await browser.close();
})();
