function(config) {
  'use strict';
  var old = window.__sptvSettings;
  if (old) return old.configure(config);
  var cfg = config, section = null, host = null, previous = null, scheduled = false;
  var rows = [
    ['theme','Appearance','theme'], ['blur','Artwork blur','blurArtwork'],
    ['motion','Cinematic motion','motion'], ['promotions','Hide promotions','hideUpgrade'],
    ['mute','Mute detected ads','muteAds'], ['hush','Hush ad-break skipping','hushEnabled'],
    ['hushSetup','Hush helper setup','hushStatus'], ['audio','Graphic equalizer','audioStatus'],
    ['lyrics','Lyrics','Local files / LRCLIB'], ['sleep','Sleep timer','sleep'],
    ['about','About Spotify+ TV','version']
  ];
  var style = document.createElement('style'); style.id = 'sptv-native-settings-style';
  style.textContent = '.sptv-settings-host{max-height:calc(100vh - 20rem);overflow-y:auto;overscroll-behavior:contain;padding-right:1rem;scrollbar-width:thin}' +
    '.menu--focused .sptv-nav-focus{outline:.4rem solid var(--sptv-accent,#1ed760);outline-offset:.5rem}' +
    '#sptv-native-settings{color:inherit;font:inherit;padding-bottom:3rem}' +
    '#sptv-native-settings h2{font:inherit;font-weight:700;margin:3rem 0 2rem}' +
    '#sptv-native-settings .profile-menu-item{min-height:8rem;box-sizing:border-box}' +
    '#sptv-native-settings button{display:block;width:100%;background:transparent;color:inherit;border:0;text-align:left;font:inherit;padding:1rem;border-radius:.5rem}' +
    '#sptv-native-settings button:focus{outline:.4rem solid var(--sptv-settings-accent,#1ed760);outline-offset:.5rem}' +
    '#sptv-native-settings .profile-menu-item__subtitle{font-size:.75em;opacity:.7;margin-top:1rem}';
  (document.head || document.documentElement).appendChild(style);
  function invoke(action) {
    if (window.SpotifyPlusSettings && window.SpotifyPlusSettings.action) window.SpotifyPlusSettings.action(action);
  }
  function label(row) {
    if (row[0] === 'theme') return ['Spotify original','Aurora','Midnight','Ember','Ocean','Rose','Solar'][Math.abs(Number(cfg.theme)||0)%7];
    if (row[0] === 'sleep') return cfg.sleep || 'Off';
    if (Object.prototype.hasOwnProperty.call(cfg,row[2])) {
      var value = cfg[row[2]]; return typeof value === 'boolean' ? value ? 'On' : 'Off' : String(value);
    }
    return row[2];
  }
  function fitHost() {
    if(!host)return;
    var top=host.getBoundingClientRect().top;
    host.style.maxHeight=Math.max(120,window.innerHeight-Math.max(0,top)-32)+'px';
  }
  function reveal(button) {
    if(!host || !host.contains(button))return;
    var viewport=host.getBoundingClientRect(), row=button.getBoundingClientRect();
    if(row.top<viewport.top+12)host.scrollTop-=viewport.top+12-row.top;
    else if(row.bottom>viewport.bottom-12)host.scrollTop+=row.bottom-viewport.bottom+12;
  }
  function sync() {
    scheduled = false;
    var next = document.querySelector('[data-testid="profile-menu"],.profile__menu');
    if (!next) { if(section && section.parentNode)section.remove(); section=null;host=null;return; }
    if (!section || section.parentNode !== next) {
      if(section)section.remove();
      host=next; host.classList.add('sptv-settings-host');
      section=document.createElement('section'); section.id='sptv-native-settings'; section.setAttribute('aria-label','Spotify+ TV settings');
      var title=document.createElement('h2'); title.textContent='Spotify+ TV'; section.appendChild(title);
      var nativeControl=host.querySelector('.profile-menu-item > :first-child');
      rows.forEach(function(row) {
        var item=document.createElement('div'); item.className='profile-menu-item sptv-setting-row';
        var button=document.createElement('button'); button.type='button';button.tabIndex=0;button.dataset.sptvAction=row[0];
        if(nativeControl)button.className=String(nativeControl.className||'').replace(/\S*--focused\S*/g,'').trim();
        var name=document.createElement('span');name.textContent=row[1];button.appendChild(name);
        var value=document.createElement('p');value.className='profile-menu-item__subtitle';button.appendChild(value);
        button.addEventListener('click',function(event){event.preventDefault();event.stopPropagation();invoke(row[0]);});
        button.addEventListener('focus',function(){reveal(button);});
        item.appendChild(button);section.appendChild(item);
      });
      host.appendChild(section);
    }
    fitHost();
    section.style.setProperty('--sptv-settings-accent',['#ffffff','#91e7cf','#bbb3ff','#ffbf98','#70c8ff','#ff96b7','#ffd166'][Math.abs(Number(cfg.theme)||0)%7]);
    rows.forEach(function(row) {
      var button=section.querySelector('[data-sptv-action="'+row[0]+'"]'), value=label(row);
      var subtitle=button.querySelector('p'); if(subtitle.textContent!==value)subtitle.textContent=value;
      button.setAttribute('aria-label',row[1]+': '+value);
    });
  }
  function defer(records) {
    if(records.every(function(record){return record.target.nodeType===1 && record.target.closest('#sptv-native-settings');}))return;
    if(!scheduled){scheduled=true;Promise.resolve().then(sync);}
  }
  var observer=new MutationObserver(defer);observer.observe(document.documentElement,{childList:true,subtree:true});
  function remote(key) {
    if(!section || !host || !section.isConnected)return false;
    var controls=Array.prototype.slice.call(host.querySelectorAll('button'));
    var logout=document.querySelector('[data-testid="switch-user-button"]');
    if(logout && controls.indexOf(logout)<0)controls.push(logout);
    var current=document.activeElement;
    if(controls.indexOf(current)<0)current=controls.filter(function(control){return /(?:^|\s)\S*--focused(?:\s|$)/.test(control.className);})[0];
    var index=controls.indexOf(current), next=null;
    if(key==='ArrowDown') {
      next=current===logout ? section.querySelector('button') : controls[Math.min(controls.length-1,index+1)];
    } else if(key==='ArrowUp') next=controls[Math.max(0,index-1)];
    else if(key==='Enter' || key===' ') {
      if(!current)return false;
      current.click();return true;
    } else return false;
    if(!next)return false;
    next.focus({preventScroll:true});reveal(next);return true;
  }
  var upgradeSelector='[data-testid="upsell-menu-item"],.upsell-menu-item';
  function upgradeRoute() {
    if(/^#\/(?:upsell|upgrade|premium)(?:[/?]|$)/i.test(location.hash)) {
      location.replace('#/view/home');return true;
    }
    return false;
  }
  function blockUpgrade(event) {
    if(event.target.closest && event.target.closest(upgradeSelector)) {
      event.preventDefault();event.stopImmediatePropagation();
    }
  }
  function navigation(key,down) {
    if(upgradeRoute())return true;
    var nav=document.querySelector('[data-testid="top-menu"]');
    if(!nav)return false;
    if(key==='ArrowUp' || key==='ArrowDown') {
      Array.prototype.forEach.call(nav.querySelectorAll('.sptv-nav-focus'),function(e){e.classList.remove('sptv-nav-focus');if(document.activeElement===e)e.blur();});
      return false;
    }
    if(!nav.classList.contains('menu--focused'))return false;
    if(key!=='ArrowLeft' && key!=='ArrowRight' && key!=='Enter')return false;
    if(!down)return true;
    var navControls='button,[data-testid="now-playing-bar"],[role="button"]';
    var all=Array.prototype.slice.call(nav.querySelectorAll(navControls));
    var controls=all.filter(function(e){return !e.matches(upgradeSelector) && !e.disabled;});
    var current=document.activeElement;
    if(all.indexOf(current)<0) {
      var marked=nav.querySelector('[class*="--focused"]');current=marked && (marked.closest(navControls) || marked.querySelector(navControls));
    }
    var index=controls.indexOf(current), next;
    if(current && current.matches(upgradeSelector)) {
      var origin=all.indexOf(current), direction=key==='ArrowLeft' ? -1 : 1;
      for(var i=origin+direction;i>=0 && i<all.length;i+=direction)if(controls.indexOf(all[i])>=0){next=all[i];break;}
      next=next || controls[0];
    } else if(key==='Enter') {
      if(current && controls.indexOf(current)>=0)current.click();
      return true;
    } else next=controls[Math.max(0,Math.min(controls.length-1,index+(key==='ArrowRight'?1:-1)))];
    if(next) {
      all.forEach(function(e){e.classList.remove('sptv-nav-focus');e.className=String(e.className).replace(/\S*--focused/g,'').trim();});
      next.classList.add('sptv-nav-focus');if(next.tabIndex<0)next.tabIndex=0;next.focus({preventScroll:true});
    }
    return true;
  }
  document.addEventListener('click',blockUpgrade,true);
  window.addEventListener('hashchange',upgradeRoute);upgradeRoute();
  function key(event) {
    if(remote(event.key)){event.preventDefault();event.stopImmediatePropagation();}
  }
  document.addEventListener('keydown',key,true);
  window.addEventListener('resize',fitHost);
  var api={navigation:navigation,remote:remote,configure:function(next){cfg=next;sync();return !!section;},open:function(){
    var nativeButton=document.querySelector('[data-testid="top-menu--settings"]');
    if(nativeButton){nativeButton.click();return true;}
    return !!document.querySelector('[data-testid="profile-menu"],.profile__menu');
  },dispose:function(){document.removeEventListener('click',blockUpgrade,true);window.removeEventListener('hashchange',upgradeRoute);window.removeEventListener('resize',fitHost);observer.disconnect();document.removeEventListener('keydown',key,true);if(section)section.remove();if(host){host.classList.remove('sptv-settings-host');host.style.removeProperty('max-height');}style.remove();}};
  window.__sptvSettings=api;sync();return !!section;
}
