const assert=require('node:assert/strict');
const fs=require('node:fs');const {chromium}=require('playwright');
(async()=>{const browser=await chromium.launch(process.env.SPTV_BROWSER?{executablePath:process.env.SPTV_BROWSER}:{headless:true});try{const page=await browser.newPage({viewport:{width:1920,height:1080}});
await page.setContent('<style>html{font-size:8px}body{margin:0}.layer{height:1080px;overflow:hidden}.header{height:200px}.profile__menu{width:900px}.profile-menu-item{height:120px}button{height:100px}</style><div class="layer"><nav class="header">Spotify header</nav><div class="profile__menu"><div class="profile-menu-item"><button class="list-button--focused">About</button></div></div></div>');
await page.evaluate(source=>(new Function('return ('+source+')'))()({theme:1}),fs.readFileSync('assets/spotifyplus-settings.js','utf8'));
const before=await page.locator('.header').boundingBox();
await page.evaluate(()=>{for(let i=0;i<11;i++)window.__sptvSettings.remote('ArrowDown')});
const after=await page.locator('.header').boundingBox();assert.equal(after.y,before.y,'Settings navigation must not scroll the outer Spotify header');
const state=await page.evaluate(()=>({outer:document.querySelector('.layer').scrollTop,inner:document.querySelector('.profile__menu').scrollTop,top:document.activeElement.getBoundingClientRect().top,bottom:document.activeElement.getBoundingClientRect().bottom,height:innerHeight}));
assert.equal(state.outer,0);assert.ok(state.inner>0);assert.ok(state.top>=0 && state.bottom<=state.height,'Selected bottom row stays visible inside bounded list');
await page.evaluate(()=>{for(let i=0;i<11;i++)window.__sptvSettings.remote('ArrowUp')});assert.equal((await page.locator('.header').boundingBox()).y,before.y);
console.log('Settings scroll: header fixed, inner list scrolls, bottom row visible, upward return stable');
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exit(1)});

