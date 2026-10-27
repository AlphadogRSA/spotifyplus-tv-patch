function(config) {
  'use strict';
  // Original TV presentation code. Existing artwork only; no media/account requests.
  var VERSION = 8, ID = 'spotifyplus-tv-style';
  var previous = window.__spotifyPlusTV;
  if (previous && previous.version === VERSION) return previous.configure(config);
  if (previous) {
    previous.destroyed = true;
    if (previous.dispose) previous.dispose();
    else {
      previous.observer.disconnect();
      previous.marked.forEach(function(node) {
        node.classList.remove('sptv-background', 'sptv-art-blur', 'sptv-static', 'sptv-upgrade-hidden');
        node.style.removeProperty('--sptv-art');
      });
    }
  }
  var style = document.getElementById(ID);
  if (!style) { style = document.createElement('style'); style.id = ID; (document.head || document.documentElement).appendChild(style); }
  var state = { version: VERSION, hidden: new Set(), images: new Set(), backgrounds: new Set(),
    roots: new Set(), writes: new WeakMap(), timer: null, artPending: false,
    stats: { scans: 0, artworkPasses: 0, layoutReads: 0 } };
  var cfg, key, artKey, destroy = false;
  var ART = 'img,main,section,[style*="background-image"],[class*="background"],[class*="npv-"],[class*="view"],body';
  var LABELS = '[aria-label],[title],[href],[data-testid],[class*="upsell"],[class*="upgrade"]';
  var PROMOTION = '[data-testid*="upsell"],[data-testid*="upgrade"],[class*="upsell"],[class*="promotion"],[class*="banner"],[class*="promo"]';
  var CONTROL = 'button,a,[role="button"],[role="link"],[role="menuitem"],[role="option"]';
  var UPGRADE = /\bupgrade\b/i;
  function ownWrite(node, attribute) {
    var snapshot = state.writes.get(node) || {};
    snapshot[attribute] = node.getAttribute(attribute);
    state.writes.set(node, snapshot);
  }
  function classToggle(node, name, value) {
    if (node.classList.contains(name) === value) return;
    node.classList.toggle(name, value); ownWrite(node, 'class');
  }
  function artworkProperty(node, value) {
    if (value === node.style.getPropertyValue('--sptv-art')) return;
    if (value) node.style.setProperty('--sptv-art', value); else node.style.removeProperty('--sptv-art');
    ownWrite(node, 'style');
  }
  function summary() { return 'tv-theme=' + cfg.theme + ';blur=' + (state.images.size + state.backgrounds.size) + ';hidden=' + state.hidden.size; }
  function css(c) {
    var accents = [null, '#91e7cf', '#bbb3ff', '#ffbf98', '#70c8ff', '#ff96b7', '#ffd166'], accent = accents[c.theme];
    var sheet = '.sptv-upgrade-hidden{display:none!important;visibility:hidden!important;pointer-events:none!important}'
      + '.sptv-static{position:relative}.sptv-background{isolation:isolate!important;overflow:hidden!important}'
      + '.sptv-background:before{content:"";position:absolute;inset:-40px;z-index:-1;pointer-events:none;background-image:var(--sptv-art);background-size:cover;background-position:center;filter:blur(32px) brightness(.58);transform:scale(1.08);'
      + (c.motion ? 'animation:sptv-drift 30s ease-in-out infinite alternate;' : '') + '}'
      + '.sptv-art-blur{filter:blur(32px) brightness(.58)!important;transform:scale(1.08)!important}'
      + '@keyframes sptv-drift{from{transform:scale(1.08) translateX(-.5%)}to{transform:scale(1.12) translateX(.5%)}}'
      + '@media(prefers-reduced-motion:reduce){.sptv-background:before{animation:none!important}}';
    sheet += '[data-testid="upsell-menu-item"],.upsell-menu-item{display:none!important;visibility:hidden!important;pointer-events:none!important}';
    if (accent) {
      sheet += ':root{--sptv-accent:' + accent + ';--text-bright-accent:' + accent + '!important;--essential-bright-accent:' + accent + '!important}'
        // TV uses class-based focus and hard-coded colors, not desktop Spotify tokens.
        + '.icon-button--focused,.icon-text-button--focused,.button--focused,.avatar--focused,.chip--focused,.toggle--focused,.select-box--focused,.context-bar-menu-item__context-bar--focused{border-color:' + accent + '!important}'
        + '.tile--focused .tile__cover-art:before,.tile__more-content--focused:before,.media-tile-image--focused .media-tile-image__background:before,.entity-list-row__cover-art--focused:before,.genre-card--focused:before,.tile-shortcut--focused:before,.now-playing-bar--focused:before,.checkbox--focused:before,.button-play-pause-mixed.icon-button--focused:after,.entity-metadata__description--focused .entity-metadata__description__button:before{border-color:' + accent + '!important}'
        + '.list-button--focused,.search-bar--focused,.playback-queue-item__cover-art--focused{box-shadow:0 0 0 .5rem ' + accent + ',0 1.5rem 5rem .25rem rgba(0,0,0,.3)!important}'
        + '.entity-list-row--focused{background-color:' + accent + '20!important}'
        + '.progress-bar__indicator,.progress-bar__scrubber__dot,.progress-indicator__bar,.equalizer--colored .equalizer__bar,.bars--green .bar,.checkbox--checked,.icon-button--primary.bg-green .icon-button__content,.button:not(.button--secondary):not(.button--tertiary) .button__content{background-color:' + accent + '!important}'
        + '.entity-list-row__title--playing,.playback-queue-item__title--current-track,.playback-queue-item__title--current-track .bars,.now-playing-bar__subtitle--green,.context-menu__icon--green,.icon-green svg.tv-icon,svg.tv-icon--green,svg.tv-icon.icon-check-green{color:' + accent + '!important}'
        + '.context-menu{background-color:' + ['','#152927','#211f33','#30231e','#122738','#321a2a','#302719'][c.theme] + '!important}'
        + 'button:focus-visible,a:focus-visible,[role="button"]:focus-visible{outline-color:' + accent + '!important}';
    }
    if (c.largeLyrics) sheet += '[data-testid*="lyrics"] p,[data-testid*="lyrics"] [role="text"]{font-size:32px!important;line-height:1.55!important}';
    return sheet;
  }
  function directPromo(node) {
    if (!node || node.nodeType !== 1 || /^(HTML|BODY|SCRIPT|STYLE|NOSCRIPT|TEMPLATE)$/.test(node.tagName)) return false;
    var classes = (node.getAttribute('class') || '').replace(/sptv-[\w-]+/g, '');
    var label = (node.getAttribute('aria-label') || '') + ' ' + (node.getAttribute('title') || '');
    var text = node.textContent || '', testid = node.getAttribute('data-testid') || '', href = node.getAttribute('href') || '';
    var textUnit = node.matches(CONTROL) || (node.children.length === 0 && text.length < 500);
    return UPGRADE.test(label) || (textUnit && UPGRADE.test(text)) || /(?:upgrade|upsell)/i.test(testid + ' ' + classes)
      || (textUnit && /^(?:get|try|explore)\s+premium\b/i.test(text.trim()))
      || /^(?:https:\/\/(?:www\.)?spotify\.com)?\/(?:[a-z]{2}(?:-[a-z]{2})?\/)?premium(?:[/?#]|$)/i.test(href);
  }
  function boundedPromotion(node) {
    if (!node || /^(HTML|BODY|MAIN|NAV)$/.test(node.tagName) || /^(main|navigation)$/.test(node.getAttribute('role') || '')) return false;
    var controls = node.querySelectorAll(CONTROL);
    for (var i = 0; i < controls.length; i++) {
      if (!directPromo(controls[i]) && !/^(close|dismiss|not now)$/i.test((controls[i].getAttribute('aria-label') || controls[i].textContent || '').trim())) return false;
    }
    return true;
  }
  function promo(node) {
    return directPromo(node) || (!!node && node.matches(PROMOTION) && boundedPromotion(node) && UPGRADE.test(node.textContent || ''));
  }
  function hide(node) {
    if (!node || !cfg.hideUpgrade) return;
    var container = node.closest(PROMOTION);
    var target = container && boundedPromotion(container) ? container : node.closest(CONTROL) || node;
    if (/^(HTML|BODY|MAIN)$/.test(target.tagName)) target = node;
    classToggle(target, 'sptv-upgrade-hidden', true); state.hidden.add(target);
  }
  function promotions(root) {
    if (!cfg.hideUpgrade || !root || !root.isConnected || root.nodeType !== 1 || root.closest('style,script,noscript,template')) return;
    if (root.matches(LABELS) && promo(root)) hide(root);
    var labels = root.querySelectorAll(LABELS);
    for (var i = 0; i < labels.length; i++) if (promo(labels[i])) hide(labels[i]);
    var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null), text;
    while ((text = walker.nextNode())) {
      var parent = text.parentElement;
      if (parent && !parent.closest('style,script,noscript,template') && (UPGRADE.test(text.nodeValue) || /^(?:get|try|explore)\s+premium\b/i.test(text.nodeValue.trim()))) hide(parent);
    }
  }
  function geometry(node) { state.stats.layoutReads++; return node.getBoundingClientRect(); }
  function artwork() {
    state.stats.artworkPasses++;
    var nodes = document.querySelectorAll(ART), reads = [], nextImages = new Set(), nextBackgrounds = new Map();
    var album = '', area = 0, nativeBackdrop = false, backdrop = null;
    // Read phase: geometry/computed styles first, no interleaved DOM writes.
    for (var i = 0; i < nodes.length; i++) {
      var node = nodes[i], rect = geometry(node);
      if (!rect.width || !rect.height || rect.bottom < 0 || rect.right < 0 || rect.top > innerHeight || rect.left > innerWidth) continue;
      var info = { node: node, rect: rect, style: getComputedStyle(node) }; reads.push(info);
      if (node.tagName !== 'IMG') continue;
      var source = node.currentSrc || node.src || '';
      var full = rect.width >= innerWidth * .9 && rect.height >= innerHeight * .9 && info.style.position === 'absolute' && info.style.objectFit === 'cover' && !node.closest(CONTROL);
      if (full) nativeBackdrop = true;
      if (rect.width >= innerWidth * .6 && rect.height >= innerHeight * .25 && (full || node.getAttribute('aria-hidden') === 'true' || node.getAttribute('role') === 'presentation')) nextImages.add(node);
      if (source.indexOf('ab67616d') >= 0 && /^https:\/\//.test(source) && rect.width >= 120 && rect.height >= 120 && rect.width * rect.height > area) {
        album = 'url(' + JSON.stringify(source) + ')'; area = rect.width * rect.height;
      }
    }
    for (var j = 0; j < reads.length; j++) {
      var item = reads[j], el = item.node, box = item.rect, computed = item.style;
      if (el.tagName === 'IMG' || el.matches(CONTROL) || box.width < innerWidth * .6 || box.height < innerHeight * .25) continue;
      var image = el.style.backgroundImage || computed.backgroundImage;
      if (image && image.indexOf('url(') >= 0) nextBackgrounds.set(el, { image: image, relative: computed.position === 'static' || el.classList.contains('sptv-static') });
      var color = computed.backgroundColor.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/);
      if (!nativeBackdrop && album && box.width >= innerWidth * .8 && box.height >= innerHeight * .8 && color && (color[4] === undefined || Number(color[4]) > .95) && Math.max(+color[1], +color[2], +color[3]) < 70) backdrop = item;
    }
    if (backdrop && album) nextBackgrounds.set(backdrop.node, { image: album, relative: backdrop.style.position === 'static' || backdrop.node.classList.contains('sptv-static') });
    // Write phase: only changed classes/properties; removed/reused art is restored.
    state.images.forEach(function(el) { if (!nextImages.has(el)) classToggle(el, 'sptv-art-blur', false); });
    nextImages.forEach(function(el) { classToggle(el, 'sptv-art-blur', true); });
    state.backgrounds.forEach(function(el) {
      if (!nextBackgrounds.has(el)) { classToggle(el, 'sptv-background', false); classToggle(el, 'sptv-static', false); artworkProperty(el, ''); }
    });
    nextBackgrounds.forEach(function(value, el) {
      artworkProperty(el, value.image); classToggle(el, 'sptv-static', value.relative); classToggle(el, 'sptv-background', true);
    });
    state.images = nextImages; state.backgrounds = new Set(nextBackgrounds.keys());
  }
  function clearArt() {
    state.images.forEach(function(el) { classToggle(el, 'sptv-art-blur', false); });
    state.backgrounds.forEach(function(el) { classToggle(el, 'sptv-background', false); classToggle(el, 'sptv-static', false); artworkProperty(el, ''); });
    state.images.clear(); state.backgrounds.clear();
  }
  function flush() {
    state.timer = null;
    if (destroy) return;
    if (document.hidden) return; // Resume once on visibilitychange; no hidden-page scanning.
    state.stats.scans++;
    state.hidden.forEach(function(el) {
      if (!cfg.hideUpgrade || !el.isConnected || !promo(el)) { classToggle(el, 'sptv-upgrade-hidden', false); state.hidden.delete(el); }
    });
    state.roots.forEach(promotions); state.roots.clear();
    if (state.artPending) { state.artPending = false; if (cfg.blurArtwork) artwork(); else clearArt(); }
  }
  function schedule(root, art) {
    if (destroy) return;
    if (root && !root.isConnected) root = null;
    if (!root && !art) return;
    if (root && root.nodeType === 1) {
      var covered = false;
      state.roots.forEach(function(saved) { if (saved.contains(root)) covered = true; else if (root.contains(saved)) state.roots.delete(saved); });
      if (!covered) state.roots.add(root);
      if (state.roots.size > 24) { state.roots.clear(); state.roots.add(document.body || document.documentElement); }
    }
    if (art) state.artPending = true;
    if (state.timer === null && !document.hidden) state.timer = setTimeout(flush, 150);
  }
  function relevantArt(node) {
    return node && node.nodeType === 1 && (node.matches(ART) || !!node.querySelector('img,[style*="background-image"]'));
  }
  state.observer = new MutationObserver(function(records) {
    for (var i = 0; i < records.length; i++) {
      var record = records[i], node = record.target.nodeType === 1 ? record.target : record.target.parentElement;
      if (!node || node.closest('style,script,noscript,template')) continue;
      if (record.type === 'attributes') {
        var attr = record.attributeName, snapshot = state.writes.get(node);
        if (snapshot && Object.prototype.hasOwnProperty.call(snapshot, attr) && node.getAttribute(attr) === snapshot[attr]) continue;
        if (attr === 'class') {
          // TV focus colors are pure CSS; focus moves must not trigger document/layout scans.
          var clean = function(v) { return (v || '').replace(/\bsptv-[\w-]+\b|\b[\w-]+--focused\b/g, '').replace(/\s+/g, ' ').trim(); };
          if (clean(record.oldValue) === clean(node.className)) continue;
          if (node.matches('img,[class*="npv-"],[class*="background"]')) schedule(null, true);
          if (/(?:upgrade|upsell|promo|banner)/i.test((record.oldValue || '') + ' ' + node.className) || state.hidden.has(node)) schedule(node, false);
        } else if (attr === 'style') {
          var layoutStyle = function(value) { return ((value || '').match(/(?:^|;)\s*(?:width|height|display|position|object-fit|background(?:-image)?)\s*:[^;]*/g) || []).sort().join(';'); };
          if (layoutStyle(record.oldValue) !== layoutStyle(node.getAttribute('style')) && relevantArt(node)) schedule(null, true);
        } else if (attr === 'src' || attr === 'srcset') schedule(null, true);
        else schedule(node, false);
      } else if (record.type === 'characterData') schedule(node, false);
      else {
        schedule(node, false);
        for (var a = 0; a < record.addedNodes.length; a++) if (relevantArt(record.addedNodes[a])) state.artPending = true;
        for (var r = 0; r < record.removedNodes.length; r++) if (relevantArt(record.removedNodes[r])) state.artPending = true;
      }
    }
  });
  state.observer.observe(document.documentElement, { childList: true, subtree: true, characterData: true, attributes: true, attributeOldValue: true,
    attributeFilter: ['src', 'srcset', 'style', 'class', 'aria-label', 'title', 'data-testid', 'href'] });
  function visible() { if (!document.hidden) schedule(document.body, true); }
  function resized() { schedule(null, true); }
  document.addEventListener('visibilitychange', visible); window.addEventListener('resize', resized);
  state.dispose = function() {
    destroy = true; if (state.timer !== null) clearTimeout(state.timer);
    state.observer.disconnect(); document.removeEventListener('visibilitychange', visible); window.removeEventListener('resize', resized);
    state.hidden.forEach(function(el) { el.classList.remove('sptv-upgrade-hidden'); }); clearArt();
  };
  state.configure = function(c) {
    var next = { theme: Math.abs(Number(c.theme) || 0) % 7, blurArtwork: !!c.blurArtwork, hideUpgrade: !!c.hideUpgrade, motion: !!c.motion, largeLyrics: !!c.largeLyrics };
    var signature = JSON.stringify(next), nextArt = next.blurArtwork + ':' + next.hideUpgrade;
    if (key === signature) return summary();
    var first = !cfg, scan = first || nextArt !== artKey; cfg = next; key = signature; artKey = nextArt;
    style.textContent = css(cfg);
    if (scan) { state.roots.add(document.body || document.documentElement); state.artPending = true; flush(); }
    return summary();
  };
  window.__spotifyPlusTV = state;
  return state.configure(config);
}
