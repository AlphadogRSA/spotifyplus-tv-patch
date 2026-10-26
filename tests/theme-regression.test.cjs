const {chromium}=require('playwright');const fs=require('node:fs');const assert=require('node:assert/strict');const path=require('node:path');
(async()=>{
 const browser=await chromium.launch(process.env.SPTV_BROWSER?{executablePath:process.env.SPTV_BROWSER}:{});let failed=0,passed=0;
 try{
  const page=await browser.newPage({viewport:{width:1280,height:720}});await page.route('**/*',r=>r.abort());
  const script=fs.readFileSync(path.join(__dirname,'../assets/spotifyplus-tv.js'),'utf8');
  const config={theme:1,blurArtwork:true,hideUpgrade:true,motion:false};
  const run=c=>page.evaluate(({script,c})=>(0,eval)('('+script+')')(c),{script,c});
  async function check(name,test){try{await test();passed++;console.log('PASS '+name)}catch(e){failed++;console.error('FAIL '+name+': '+e.message)}}
  await page.setContent(`<style>
    body{background:rgb(18,18,18)} .icon-button--focused{border:4px solid white}.progress-bar__indicator{background:white}.entity-list-row__title--playing{color:rgb(29,184,84)}
    .tile__cover-art{position:relative}.tile--focused .tile__cover-art:before{content:'';border:4px solid white}
    .icon-button--primary.bg-green .icon-button__content{background:rgb(29,184,84)}
  </style><main><button id="focused" class="icon-button icon-button--focused">Play</button><div id="progress" class="progress-bar__indicator"></div><p id="playing" class="entity-list-row__title--playing">Current song</p><div class="tile tile--focused"><div id="tile" class="tile__cover-art"></div></div><button class="icon-button icon-button--primary bg-green"><span id="primary" class="icon-button__content">Play</span></button>
    <div id="banner" class="promotion"><h2>Upgrade for more</h2><button>Upgrade now</button></div><p id="plain">Upgrade your listening experience</p><div role="option" id="option">Upgrade</div><div id="accessible" aria-label="Upgrade to Premium"><svg></svg></div><button id="reused">Upgrade</button><button id="safe">Home</button>
    <img id="background" style="position:absolute;width:1280px;height:720px;object-fit:cover" role="img">
  </main>`);
  await run(config);
  await check('TV class-based focus uses selected accent',async()=>assert.equal(await page.locator('#focused').evaluate(n=>getComputedStyle(n).borderColor),'rgb(145, 231, 207)'));
  await check('Playback progress uses selected accent',async()=>assert.equal(await page.locator('#progress').evaluate(n=>getComputedStyle(n).backgroundColor),'rgb(145, 231, 207)'));
  await check('Playing title uses selected accent',async()=>assert.equal(await page.locator('#playing').evaluate(n=>getComputedStyle(n).color),'rgb(145, 231, 207)'));
  await check('Tile focus outline uses selected accent',async()=>assert.equal(await page.locator('#tile').evaluate(n=>getComputedStyle(n,':before').borderColor),'rgb(145, 231, 207)'));
  await check('Primary action uses selected accent',async()=>assert.equal(await page.locator('#primary').evaluate(n=>getComputedStyle(n).backgroundColor),'rgb(145, 231, 207)'));
  await check('Upgrade banners and plain text are hidden',async()=>{assert.equal(await page.locator('#banner').isVisible(),false);assert.equal(await page.locator('#plain').isVisible(),false)});
  await check('Upgrade options and accessible controls are hidden',async()=>{assert.equal(await page.locator('#option').isVisible(),false);assert.equal(await page.locator('#accessible').isVisible(),false)});
  await check('Unrelated navigation remains visible',async()=>assert.equal(await page.locator('#safe').isVisible(),true));
  await page.evaluate(()=>{const n=document.createElement('section');n.id='library';n.className='promotion';n.setAttribute('aria-label','Library');n.innerHTML='<button id="nested-offer">Upgrade now</button><button id="unrelated-play">Play</button>';document.querySelector('main').appendChild(n)});await page.waitForTimeout(450);
  await check('Labeled containers keep unrelated playback controls',async()=>{assert.equal(await page.locator('#unrelated-play').isVisible(),true);assert.equal(await page.locator('#nested-offer').isVisible(),false)});
  await page.evaluate(()=>{const n=document.createElement('span');n.textContent='Upgrade now';document.body.appendChild(n);n.textContent='Upgrade today';n.remove()});await page.waitForTimeout(450);
  await check('Detached promotional nodes are released',async()=>assert.equal(await page.evaluate(()=>Array.from(window.__spotifyPlusTV.hidden||[]).some(n=>!n.isConnected)),false));
  await page.locator('#reused').evaluate(n=>n.textContent='Play');await page.waitForTimeout(450);
  await check('Reused controls are restored when no longer promotional',async()=>assert.equal(await page.locator('#reused').isVisible(),true));
  await page.locator('#background').evaluate(n=>{n.style.width='200px';n.style.height='200px'});await page.waitForTimeout(450);
  await check('Reused background becoming a cover stays clear',async()=>assert.equal(await page.locator('#background').evaluate(n=>getComputedStyle(n).filter),'none'));
  await page.evaluate(()=>window.previousStyle=document.getElementById('spotifyplus-tv-style'));await run(config);
  await check('Repeated identical configuration keeps existing style',async()=>assert.equal(await page.evaluate(()=>window.previousStyle===document.getElementById('spotifyplus-tv-style')),true));
  await run({...config,theme:3});
  await check('Theme switching updates TV UI immediately',async()=>assert.equal(await page.locator('#focused').evaluate(n=>getComputedStyle(n).borderColor),'rgb(255, 191, 152)'));
  await run({...config,theme:0,hideUpgrade:false,blurArtwork:false});
  await check('Spotify original restores native TV focus colors',async()=>assert.equal(await page.locator('#focused').evaluate(n=>getComputedStyle(n).borderColor),'rgb(255, 255, 255)'));
  await check('Promotions can be restored through the setting',async()=>assert.equal(await page.locator('#plain').isVisible(),true));
  console.log(`Theme/Upgrade regressions: ${passed} passed, ${failed} failed`);if(failed)process.exitCode=1;
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
