const assert=require('node:assert/strict');const fs=require('node:fs');const {chromium}=require('playwright');
(async()=>{const browser=await chromium.launch(process.env.SPTV_BROWSER?{executablePath:process.env.SPTV_BROWSER}:{headless:true});try{const page=await browser.newPage();await page.setContent('<nav><button id="home">Home</button></nav>');
await page.evaluate(source=>(new Function('return ('+source+')'))()({theme:0,hideUpgrade:true,blurArtwork:false,motion:false}),fs.readFileSync('assets/spotifyplus-tv.js','utf8'));
const display=await page.evaluate(()=>{const b=document.createElement('button');b.dataset.testid='upsell-menu-item';b.innerHTML='<div class="upsell-menu-item"><span>Upgrade</span></div>';document.querySelector('nav').appendChild(b);return getComputedStyle(b).display});
assert.equal(display,'none','Main Upgrade entry must be hidden immediately, before the observer runs');assert.ok(await page.locator('#home').isVisible());console.log('Upgrade entry: immediate full-control hiding with Home retained');
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exit(1)});
