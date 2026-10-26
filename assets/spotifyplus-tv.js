function(config) {
  'use strict';
  // Original presentation extension. Existing artwork only; no media or account requests.
  var id = 'spotifyplus-tv-style', state = window.__spotifyPlusTV;
  if (state) {
    state.destroyed = true;
    state.observer.disconnect();
    state.marked.forEach(function(node) { node.classList.remove('sptv-background', 'sptv-art-blur', 'sptv-static', 'sptv-upgrade-hidden'); node.style.removeProperty('--sptv-art'); });
  }
  var existing = document.getElementById(id);
  if (existing) existing.remove();
  // Preserve artwork-derived colors; custom accents use Spotify's semantic tokens.
  var accents = [null, '#91e7cf', '#bbb3ff', '#ffbf98'];
  var accent = accents[config.theme] || null;
  var css = '.sptv-upgrade-hidden{display:none!important;visibility:hidden!important;pointer-events:none!important}'
    + '.sptv-static{position:relative!important}'
    + '.sptv-background{isolation:isolate!important;background-image:none!important;overflow:hidden!important}'
    + '.sptv-background:before{content:"";position:absolute;inset:-40px;z-index:-1;pointer-events:none;background-image:var(--sptv-art);background-size:cover;background-position:center;filter:blur(32px) brightness(.58);transform:scale(1.08);'
    + (config.motion ? 'animation:sptv-drift 30s ease-in-out infinite alternate;' : '') + '}'
    + '.sptv-art-blur{filter:blur(32px) brightness(.58)!important;transform:scale(1.08)!important}'
    + '@keyframes sptv-drift{from{transform:scale(1.08) translateX(-.5%)}to{transform:scale(1.12) translateX(.5%)}}'
    + '@media(prefers-reduced-motion:reduce){.sptv-background:before{animation:none!important}}';
  if (accent) css += ':root{--text-bright-accent:' + accent + '!important;--essential-bright-accent:' + accent + '!important;--background-tinted-highlight:' + accent + '26!important}'
    + 'button:focus,a:focus,[role="button"]:focus,[data-focused="true"]{outline-color:' + accent + '!important;border-color:' + accent + '!important}';
  if (config.largeLyrics) css += '[data-testid*="lyrics"] p,[data-testid*="lyrics"] [role="text"]{font-size:32px!important;line-height:1.55!important}';
  existing = document.createElement('style'); existing.id = id; existing.textContent = css;
  (document.head || document.documentElement).appendChild(existing);
  state = { marked: [], pending: false, backgrounds: 0, upgrades: 0 };
  function mark(node, name) {
    if (!node.classList.contains(name)) node.classList.add(name);
    if (state.marked.indexOf(node) < 0) state.marked.push(node);
  }
  state.scan = function() {
    if (state.destroyed) return;
    state.pending = false;
    var candidates = document.querySelectorAll('body,div,main,section,img,button,a,[role="button"]');
    var album = '', albumArea = 0, backdrop = null, nativeBackdrop = false;
    if (config.blurArtwork) {
      var images = document.querySelectorAll('img');
      for (var a = 0; a < images.length; a++) {
        var box = images[a].getBoundingClientRect(), source = images[a].currentSrc || images[a].src || '';
        var imageStyle = getComputedStyle(images[a]);
        if (box.width >= innerWidth * .9 && box.height >= innerHeight * .9 && imageStyle.position === 'absolute' && imageStyle.objectFit === 'cover') nativeBackdrop = true;
        if (source.indexOf('ab67616d') >= 0 && /^https:\/\//.test(source) && box.width >= 120 && box.height >= 120 && box.width * box.height > albumArea) {
          album = 'url(' + JSON.stringify(source) + ')'; albumArea = box.width * box.height;
        }
      }
    }
    for (var i = 0; i < Math.min(candidates.length, 2000); i++) {
      var node = candidates[i], tag = node.tagName.toLowerCase();
      var control = tag === 'button' || tag === 'a' || node.getAttribute('role') === 'button';
      if (config.hideUpgrade && control) {
        var label = (node.getAttribute('aria-label') || node.textContent || '').replace(/\s+/g, ' ').trim();
        var testid = node.getAttribute('data-testid') || '';
        var href = node.getAttribute('href') || '';
        if (/^(upgrade(?: to premium)?|(?:get|explore|try) premium)$/i.test(label)
            || /(?:^|[-_])(?:upgrade|premium-upsell)(?:$|[-_])/i.test(testid)
            || /^(?:https:\/\/(?:www\.)?spotify\.com)?\/(?:[a-z]{2}(?:-[a-z]{2})?\/)?premium(?:[/?#]|$)/i.test(href)) {
          if (!node.classList.contains('sptv-upgrade-hidden')) state.upgrades++;
          mark(node, 'sptv-upgrade-hidden');
        }
      }
      if (!config.blurArtwork || control) continue;
      var rect = node.getBoundingClientRect();
      if (rect.width < innerWidth * .6 || rect.height < innerHeight * .25) continue;
      if (tag === 'img') {
        // Only decorative images, preserving album-cover controls.
        var imageStyle = getComputedStyle(node);
        if (node.getAttribute('aria-hidden') === 'true' || node.getAttribute('role') === 'presentation'
            || (rect.width >= innerWidth * .9 && rect.height >= innerHeight * .9 && imageStyle.position === 'absolute' && imageStyle.objectFit === 'cover' && !node.closest('button,a,[role="button"]'))) {
          if (!node.classList.contains('sptv-art-blur')) state.backgrounds++;
          mark(node, 'sptv-art-blur');
        }
      } else {
        var style = getComputedStyle(node), image = node.style.backgroundImage || style.backgroundImage;
        var color = style.backgroundColor.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/);
        if (!nativeBackdrop && album && rect.width >= innerWidth * .8 && rect.height >= innerHeight * .8 && color && (color[4] === undefined || Number(color[4]) > .95) && Math.max(+color[1], +color[2], +color[3]) < 70) backdrop = node;
        if (image && image.indexOf('url(') >= 0) {
          if (node.style.getPropertyValue('--sptv-art') !== image) node.style.setProperty('--sptv-art', image);
          if (!node.classList.contains('sptv-background')) state.backgrounds++;
          if (style.position === 'static') mark(node, 'sptv-static');
          mark(node, 'sptv-background');
        }
      }
    }
    if (backdrop && album) {
      if (backdrop.style.getPropertyValue('--sptv-art') !== album) backdrop.style.setProperty('--sptv-art', album);
      if (!backdrop.classList.contains('sptv-background')) state.backgrounds++;
      if (getComputedStyle(backdrop).position === 'static') mark(backdrop, 'sptv-static');
      mark(backdrop, 'sptv-background');
    }
    state.marked = state.marked.filter(function(node) { return node.isConnected; });
  };
  state.observer = new MutationObserver(function() {
    if (!state.pending) { state.pending = true; setTimeout(state.scan, 200); }
  });
  state.observer.observe(document.documentElement, {childList: true, subtree: true, attributes: true, attributeFilter: ['src', 'style', 'aria-label', 'data-testid', 'href']});
  window.__spotifyPlusTV = state;
  state.scan();
  return 'native-palette;blur=' + state.backgrounds + ';upgrade=' + state.upgrades;
}
