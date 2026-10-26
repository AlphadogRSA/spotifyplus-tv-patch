function(config) {
  'use strict';
  // Original CSS-only UI enhancement: no network, token access or playback interception.
  var id = 'spotifyplus-tv-style';
  var existing = document.getElementById(id);
  var state = window.__spotifyPlusTV;
  if (config.theme === 0) {
    if (state) {
      state.observer.disconnect();
      state.marked.forEach(function(node) { node.classList.remove('sptv-background'); });
      delete window.__spotifyPlusTV;
    }
    if (existing) existing.remove();
    return 'default';
  }
  var palettes = [null,
    ['#102d35', '#20213e', '#0a1723', '#91e7cf'],
    ['#151b42', '#281a3d', '#090c1d', '#bbb3ff'],
    ['#3b1c21', '#2c203e', '#160f1a', '#ffbf98']];
  var p = palettes[config.theme] || palettes[1];
  var css = ':root{--background-base:' + p[2] + '!important;--background-highlight:' + p[0] + '!important;--text-bright-accent:' + p[3] + '!important;--essential-bright-accent:' + p[3] + '!important}'
    + 'html,body{background-color:' + p[2] + '!important;color:#fff!important}'
    + 'body{background-image:linear-gradient(125deg,' + p[0] + ',' + p[1] + ',' + p[2] + ')!important;background-size:240% 240%!important;'
    + (config.motion ? 'animation:sptv-drift 28s ease-in-out infinite alternate!important' : 'animation:none!important') + '}'
    + '#root,.sptv-background{background-color:transparent!important;background-image:none!important}'
    + '@keyframes sptv-drift{0%{background-position:0% 35%}100%{background-position:100% 65%}}';
  if (config.largeLyrics) css += '[data-testid*="lyrics"] p,[data-testid*="lyrics"] [role="text"]{font-size:32px!important;line-height:1.55!important}';
  if (!existing) { existing = document.createElement('style'); existing.id = id; (document.head || document.documentElement).appendChild(existing); }
  existing.textContent = css;
  if (!state) {
    state = {marked: [], pending: false};
    state.scan = function() {
      state.pending = false;
      if (!document.getElementById(id)) return;
      var candidates = document.querySelectorAll('div,main,section');
      for (var i = 0; i < Math.min(candidates.length, 1000); i++) {
        var node = candidates[i];
        if (node.classList.contains('sptv-background')) continue;
        var rect = node.getBoundingClientRect();
        if (rect.width < innerWidth * .6 || rect.height < innerHeight * .6) continue;
        var rgb = getComputedStyle(node).backgroundColor.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/);
        if (rgb && (rgb[4] === undefined || Number(rgb[4]) > .2) && Math.max(+rgb[1],+rgb[2],+rgb[3]) < 70) {
          node.classList.add('sptv-background');
          state.marked.push(node);
        }
      }
      state.marked = state.marked.filter(function(node) { return node.isConnected; });
    };
    state.observer = new MutationObserver(function() {
      if (!state.pending) { state.pending = true; setTimeout(state.scan, 200); }
    });
    state.observer.observe(document.documentElement, {childList: true, subtree: true});
    window.__spotifyPlusTV = state;
  }
  state.scan();
  return 'spotifyplus-tv:' + config.theme;
}
