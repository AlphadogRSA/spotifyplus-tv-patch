// Optional browser regression: NODE_PATH must include Playwright; no network is used.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch(process.env.SPTV_BROWSER ? { executablePath: process.env.SPTV_BROWSER } : {});
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
    await page.route('**/*', route => route.abort());
    await page.setContent(`<style>body{background:rgb(18,18,18)}#hero{width:1000px;height:400px;background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg'/%3E")}</style>
      <section id="hero"><h1 id="title">Original title</h1></section>
      <img id="cover" alt="Album artwork" style="width:800px;height:500px">
      <img id="backdrop" style="position:absolute;width:1280px;height:720px;object-fit:cover;z-index:-1">
      <button id="upgrade">Upgrade</button><button id="play">Play</button><button id="song">Premium song</button>`);
    const apply = fs.readFileSync(require('node:path').join(__dirname, '../assets/spotifyplus-tv.js'), 'utf8');
    const run = config => page.evaluate(({ apply, config }) => (0, eval)('(' + apply + ')')(config), { apply, config });
    const config = { theme: 1, blurArtwork: true, hideUpgrade: true, motion: false };
    await run(config);
    assert.equal(await page.locator('#upgrade').isVisible(), false);
    assert.equal(await page.locator('#play').isVisible(), true);
    assert.equal(await page.locator('#song').isVisible(), true);
    assert.equal(await page.locator('#hero').evaluate(n => getComputedStyle(n, ':before').filter.includes('blur(32px)')), true);
    assert.equal(await page.locator('#title').evaluate(n => getComputedStyle(n).filter), 'none');
    assert.equal(await page.locator('#cover').evaluate(n => getComputedStyle(n).filter), 'none');
    assert.equal(await page.locator('#backdrop').evaluate(n => getComputedStyle(n).filter.includes('blur(32px)')), true);
    assert.equal(await page.locator('body').evaluate(n => getComputedStyle(n).backgroundColor), 'rgb(18, 18, 18)');
    await page.evaluate(() => { const b = document.createElement('button'); b.id = 'late'; b.textContent = 'Get Premium'; document.body.appendChild(b); });
    await page.waitForFunction(() => document.getElementById('late').classList.contains('sptv-upgrade-hidden'));
    assert.equal(await page.locator('#late').isVisible(), false);
    await run({ theme: 0, blurArtwork: false, hideUpgrade: false, motion: false });
    assert.equal(await page.locator('#upgrade').isVisible(), true);
    assert.equal(await page.locator('#hero').evaluate(n => n.classList.contains('sptv-background')), false);
    assert.equal(await page.locator('#backdrop').evaluate(n => getComputedStyle(n).filter), 'none');
    console.log('Presentation: 12 browser checks passed');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
