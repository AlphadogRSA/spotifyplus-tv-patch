function(config) {
  'use strict';
  var old = window.__sptvSettings;
  if (old) return old.configure(config);
  var stats={syncs:0,focusReports:0};
  var reportedMode=null,reportedProfile=null;
  var cfg = config, section = null, host = null, lastNav = null, watchedNav = null, scheduled = false;
  var rows = [
    ['theme','Appearance','theme'], ['blur','Artwork blur','blurArtwork'],
    ['motion','Cinematic motion','motion'], ['promotions','Hide promotions','hideUpgrade'],
    ['mute','Mute detected ads','muteAds'], ['hush','Hush ad-break skipping','hushEnabled'],
    ['hushSetup','Hush helper setup','hushStatus'], ['audio','Graphic equalizer','audioStatus'],
    ['sleep','Sleep timer','sleep'],
    ['about','About Spotify+ TV','version']
  ];
  var style = document.createElement('style'); style.id = 'sptv-native-settings-style';
  style.textContent = '.sptv-settings-host{max-height:calc(100vh - 20rem);overflow-y:auto;overscroll-behavior:contain;padding-right:1rem;scrollbar-width:thin}' +
    '.menu--focused .sptv-nav-focus{outline:.4rem solid var(--sptv-accent,#1ed760);outline-offset:.5rem}' +
    '#sptv-native-settings{color:inherit;font:inherit;padding-bottom:3rem}' +
    '#sptv-native-settings h2{font:inherit;font-weight:700;margin:3rem 0 2rem}' +
    '.sptv-settings-host.sptv-dom-navigation .list-button--focused:not(:focus),.sptv-settings-host.sptv-settings-suspended .list-button--focused{box-shadow:none!important}';
  (document.head || document.documentElement).appendChild(style);
  function editing() {
    var active=document.activeElement,nav=navElement();
    if(nav && (nav.classList.contains('menu--focused') || nav.contains(active)))return false;
    return !!(active && (active.matches('input,textarea,select') || active.isContentEditable)) || /^#\/search(?:[/?]|$)/.test(location.hash);
  }
  function reportFocus() {
    var nav=navElement(), profile=profileVisible() && !!document.querySelector('[data-testid="profile-menu"],.profile__menu');
    var mode=nav && (nav.classList.contains('menu--focused') || nav.contains(document.activeElement)) ? 'menu' : editing() ? 'text' : profile ? 'profile' : 'native';
    if(mode===reportedMode && profile===reportedProfile)return;
    reportedMode=mode;reportedProfile=profile;
    if(window.SpotifyPlusSettings && typeof window.SpotifyPlusSettings.focus==='function'){stats.focusReports++;window.SpotifyPlusSettings.focus(mode,profile);}
  }
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
  function navElement() { return document.querySelector('[data-testid="top-menu"]'); }
  function profileVisible() { return !location.hash || /^#\/profile(?:[/?]|$)/.test(location.hash); }
  function clearNavFocus() {
    var nav=navElement();if(!nav)return;
    Array.prototype.forEach.call(nav.querySelectorAll('.sptv-nav-focus'),function(e){e.classList.remove('sptv-nav-focus');});
    if(nav.contains(document.activeElement))document.activeElement.blur();
  }
  function releaseSettingsFocus() {
    if(!host)return;
    if(host.contains(document.activeElement))document.activeElement.blur();
    host.classList.remove('sptv-dom-navigation');
  }
  function repairNavFocus(nav) {
    var selector='button,[data-testid="now-playing-bar"],[role="button"]';
    var marked=nav.querySelector('[class*="--focused"]');
    var current=nav.contains(document.activeElement) ? document.activeElement : marked && (marked.closest(selector)||marked.querySelector(selector));
    if(current && !visibleControl(current,nav))navigation('ArrowRight',true);
  }
  function syncFocus() {
    var nav=navElement(), top=!!nav && nav.classList.contains('menu--focused');
    if(host)host.classList.toggle('sptv-settings-suspended',top);
    if(top){releaseSettingsFocus();repairNavFocus(nav);}else clearNavFocus();
    reportFocus();
  }
  var navObserver=new MutationObserver(syncFocus);
  function watchNav() {
    var nav=navElement();if(nav===watchedNav)return;
    navObserver.disconnect();watchedNav=nav;
    if(nav)navObserver.observe(nav,{attributes:true,attributeFilter:['class']});
    syncFocus();
  }
  function focusChanged(event) {
    reportFocus();
    var nav=navElement();
    if(nav && nav.contains(event.target))releaseSettingsFocus();
    else if(host && host.contains(event.target)) {
      host.classList.add('sptv-dom-navigation');host.classList.remove('sptv-settings-suspended');clearNavFocus();
    }
  }
  function cleanHost() {
    releaseSettingsFocus();
    if(host){host.classList.remove('sptv-settings-host','sptv-dom-navigation','sptv-settings-suspended');host.style.removeProperty('max-height');}
    if(section)section.remove();section=null;host=null;
  }
  function sync() {
    stats.syncs++;reportFocus();
    scheduled = false;
    watchNav();disableUpgrade();
    var topNav=navElement();if(topNav && topNav.classList.contains('menu--focused'))repairNavFocus(topNav);
    var next = profileVisible() ? document.querySelector('[data-testid="profile-menu"],.profile__menu') : null;
    if (!next) { cleanHost();return; }
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
        button.addEventListener('focus',function(){button.classList.add('list-button--focused');reveal(button);});
        button.addEventListener('blur',function(){button.classList.remove('list-button--focused');});
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
    var selector='[data-testid="profile-menu"],.profile__menu,[data-testid="top-menu"],[data-testid="upsell-menu-item"]';
    var relevant=records.some(function(record){
      if(record.target.nodeType===1 && record.target.closest('#sptv-native-settings'))return false;
      if(host && (record.target===host || host.contains(record.target)))return true;
      return Array.prototype.some.call(record.addedNodes,function(node){return node.nodeType===1 && (node.matches(selector)||!!node.querySelector(selector));}) ||
        Array.prototype.some.call(record.removedNodes,function(node){return node.nodeType===1 && (node.matches(selector)||!!node.querySelector(selector));});
    });
    if(relevant && !scheduled){scheduled=true;Promise.resolve().then(sync);}
  }
  var observer=new MutationObserver(defer);observer.observe(document.documentElement,{childList:true,subtree:true});
  function remote(key) {
    var nav=navElement();
    if(!profileVisible() || !section || !host || !section.isConnected || (nav && (nav.classList.contains('menu--focused') || nav.contains(document.activeElement))))return false;
    var controls=Array.prototype.slice.call(host.querySelectorAll('button'));
    var logout=document.querySelector('[data-testid="switch-user-button"]');
    if(logout && controls.indexOf(logout)<0)controls.push(logout);
    var current=document.activeElement;
    if(controls.indexOf(current)<0)current=controls.filter(function(control){return /(?:^|\s)\S*--focused(?:\s|$)/.test(control.className);})[0];
    var index=controls.indexOf(current), next=null;
    if(key==='ArrowDown') {
      next=current===logout ? section.querySelector('button') : controls[Math.min(controls.length-1,index+1)];
    } else if(key==='ArrowUp') {
      if(index===0){releaseSettingsFocus();return false;}
      next=controls[Math.max(0,index-1)];
    }
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
    var nav=navElement();
    if(nav && nav.contains(event.target) && !(event.target.closest && event.target.closest(upgradeSelector)))releaseSettingsFocus();
    if(event.target.closest && event.target.closest(upgradeSelector)) {
      event.preventDefault();event.stopImmediatePropagation();
    }
  }
  function disableUpgrade() {
    var nav=navElement();if(!nav)return;
    Array.prototype.forEach.call(nav.querySelectorAll('[data-testid="upsell-menu-item"]'),function(e){
      if(!e.disabled)e.disabled=true;
      if(e.tabIndex!==-1)e.tabIndex=-1;
      if(!e.hidden)e.hidden=true;
      if(e.getAttribute('aria-hidden')!=='true')e.setAttribute('aria-hidden','true');
      if(!e.hasAttribute('inert'))e.setAttribute('inert','');
    });
  }
  function visibleControl(e,nav) {
    if(e.matches(upgradeSelector) || e.disabled || e.getAttribute('aria-disabled')==='true')return false;
    for(var node=e;node;node=node.parentElement) {
      if(node.hidden || node.hasAttribute('inert') || node.getAttribute('aria-hidden')==='true')return false;
      var style=getComputedStyle(node);
      if(style.display==='none' || style.visibility==='hidden' || style.visibility==='collapse' || Number(style.opacity)===0)return false;
      if(node===nav)break;
    }
    var rect=e.getBoundingClientRect();
    return rect.width>0 && rect.height>0 && rect.left>=-1 && rect.top>=-1 && rect.right<=innerWidth+1 && rect.bottom<=innerHeight+1;
  }
  function navigation(key,down) {
    if(upgradeRoute())return true;
    var nav=navElement();
    if(!nav)return false;
    if(key==='ArrowUp' || key==='ArrowDown') {
      clearNavFocus();
      return false;
    }
    if(!nav.classList.contains('menu--focused') && !nav.contains(document.activeElement))return false;
    releaseSettingsFocus();disableUpgrade();
    if(key!=='ArrowLeft' && key!=='ArrowRight' && key!=='Enter')return false;
    if(!down)return true;
    var navControls='button,[data-testid="now-playing-bar"],[role="button"]';
    var all=Array.prototype.slice.call(nav.querySelectorAll(navControls));
    var eligible=all.filter(function(e){return visibleControl(e,nav);});
    var controls=eligible.filter(function(e){return !eligible.some(function(parent){return parent!==e && parent.contains(e);});});
    if(!controls.length){lastNav=null;clearNavFocus();return true;}
    var current=document.activeElement;
    if(all.indexOf(current)<0) {
      var marked=nav.querySelector('[class*="--focused"]');current=marked && (marked.closest(navControls) || marked.querySelector(navControls));
    }
    if(!current)current=controls.indexOf(lastNav)>=0 ? lastNav : controls.filter(function(e){return e.dataset.testid==='top-menu--home';})[0] || controls[0];
    var index=controls.indexOf(current), next;
    if(current && controls.indexOf(current)<0) {
      var origin=all.indexOf(current), direction=key==='ArrowLeft' ? -1 : 1;
      for(var i=origin+direction;i>=0 && i<all.length;i+=direction)if(controls.indexOf(all[i])>=0){next=all[i];break;}
      if(!next)for(var back=origin-direction;back>=0 && back<all.length;back-=direction)if(controls.indexOf(all[back])>=0){next=all[back];break;}
      next=next || controls[0];
    } else if(key==='Enter') {
      if(current && controls.indexOf(current)>=0){lastNav=current;current.click();}
      return true;
    } else next=controls[Math.max(0,Math.min(controls.length-1,index+(key==='ArrowRight'?1:-1)))];
    if(next) {
      all.forEach(function(e){e.classList.remove('sptv-nav-focus');e.className=String(e.className).replace(/\S*--focused/g,'').trim();});
      lastNav=next;next.classList.add('sptv-nav-focus');if(next.tabIndex<0)next.tabIndex=0;next.focus({preventScroll:true});
    }
    return true;
  }
  function routeChanged() {releaseSettingsFocus();clearNavFocus();lastNav=null;upgradeRoute();sync();}
  var handledKeys=Object.create(null);
  function dispatch(key,down) {
    if(!down){var handled=handledKeys[key]===true;delete handledKeys[key];return handled;}
    if(editing()) {handledKeys[key]=false;return false;}
    var handled=navigation(key,true) || (profileVisible() && remote(key));
    handledKeys[key]=!!handled;return !!handled;
  }
  document.addEventListener('click',blockUpgrade,true);
  document.addEventListener('focusin',focusChanged,true);
  document.addEventListener('focusout',reportFocus,true);
  window.addEventListener('hashchange',routeChanged);upgradeRoute();
  function key(event) {
    if(dispatch(event.key,true)){event.preventDefault();event.stopImmediatePropagation();}
  }
  function keyUp(event) {if(dispatch(event.key,false)){event.preventDefault();event.stopImmediatePropagation();}}
  document.addEventListener('keydown',key,true);
  document.addEventListener('keyup',keyUp,true);
  window.addEventListener('resize',fitHost);
  var api={stats:stats,dispatch:dispatch,navigation:navigation,remote:remote,configure:function(next){cfg=next;sync();return !!section;},open:function(){
    var nativeButton=document.querySelector('[data-testid="top-menu--settings"]');
    if(nativeButton){nativeButton.click();return true;}
    return !!document.querySelector('[data-testid="profile-menu"],.profile__menu');
  },dispose:function(){navObserver.disconnect();document.removeEventListener('focusin',focusChanged,true);document.removeEventListener('focusout',reportFocus,true);document.removeEventListener('click',blockUpgrade,true);window.removeEventListener('hashchange',routeChanged);window.removeEventListener('resize',fitHost);observer.disconnect();document.removeEventListener('keydown',key,true);document.removeEventListener('keyup',keyUp,true);clearNavFocus();cleanHost();style.remove();}};
  window.__sptvSettings=api;sync();return !!section;
}
