#!/usr/bin/env python3
"""Prepare an auditable TV adaptation from the exact upstream gitlink; never edit the submodule."""
from pathlib import Path
import argparse
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
PIN = '4e9ded397338de5c69b1784c636feb3d34a8de01'
ANDROID = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', ANDROID)
ET.register_namespace('tools', 'http://schemas.android.com/tools')

def replace(path, old, new):
    text = path.read_text(encoding='utf-8')
    if text.count(old) != 1:
        raise ValueError(f'Upstream changed: expected one replacement in {path.name}: {old[:60]}')
    path.write_text(text.replace(old, new), encoding='utf-8')

def prepare(destination):
    upstream = ROOT / 'upstream'
    actual = subprocess.check_output(['git', '-C', str(upstream), 'rev-parse', 'HEAD'], text=True).strip()
    if actual != PIN:
        raise ValueError(f'Expected Hush {PIN}, got {actual}')
    subprocess.run(['git', '-C', str(upstream), 'diff', '--exit-code', 'HEAD', '--'], check=True, stdout=subprocess.DEVNULL)
    # Untracked files are excluded too: copy only paths recorded in the pinned source tree.
    if destination.exists():
        raise ValueError('Destination must be new; refusing to overwrite an existing project')
    paths = subprocess.check_output(['git', '-C', str(upstream), 'ls-files', '-z', 'android'], text=True).split('\0')
    for name in filter(None, paths):
        relative = Path(name).relative_to('android')
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(upstream / name, target)

    source = destination / 'app/src/main/java/io/coldmew/hush'
    for path in (destination / 'app/src').rglob('*'):
        if path.suffix not in ('.java', '.xml'):
            continue
        text = path.read_text(encoding='utf-8')
        text = text.replace('com.spotify.music', 'com.spotify.tv.android')
        # This metadata key belongs to Spotify's protocol, not its install package.
        text = text.replace('com.spotify.tv.android.extra.CONTEXT_URI', 'com.spotify.music.extra.CONTEXT_URI')
        text = text.replace('com.spotify.tv.android/.MainActivity', 'com.spotify.tv.android/.SpotifyTVActivity')
        path.write_text(text, encoding='utf-8')

    gradle = destination / 'app/build.gradle.kts'
    replace(gradle, 'applicationId = "io.coldmew.hush"', 'applicationId = "dev.aurora.hushtv"')
    replace(gradle, 'versionName = "2.0.2"', 'versionName = "2.0.2-tv.1"')
    # Keep one stable package for the signature-protected Spotify protocol in every variant.
    replace(gradle, 'applicationIdSuffix = ".debug"', '// Stable companion package for debug builds.')

    watcher = source / 'AdWatcher.java'
    replace(watcher, 'static boolean isAd(MediaController c) { return isAd(c.getMetadata()); }',
            'static boolean isAd(MediaController c) { return TvBridge.isNativeAd(c) || isAd(c.getMetadata()); }')
    replace(watcher, 'AdWatcher w = instance;\n        if (w != null && w.spotify != null) w.check(w.spotify);',
            'TvBridge.recheck();')
    text = watcher.read_text(encoding='utf-8')
    start = text.index('    private void checkLocked(MediaController c) {')
    watcher.write_text(text[:start] + '    private void checkLocked(MediaController c) {\n'
                       '        TvBridge.acceptController(this, c);\n    }\n}\n', encoding='utf-8')

    skipper = source / 'Skipper.java'
    replace(skipper, 'return c.getSystemService(MediaSessionManager.class).getActiveSessions(new ComponentName(c, AdWatcher.class));',
            'return TvBridge.sessions(c);')
    replace(skipper, 'Hush.SPOTIFY + "/.MainActivity"', 'Hush.SPOTIFY + "/.SpotifyTVActivity"')
    replace(skipper, 'Intent i = c.getPackageManager().getLaunchIntentForPackage(Hush.SPOTIFY);',
            'Intent i = c.getPackageManager().getLeanbackLaunchIntentForPackage(Hush.SPOTIFY);\n'
            '        if (i == null) i = c.getPackageManager().getLaunchIntentForPackage(Hush.SPOTIFY);')
    replace(skipper, '''        switch (n % 3) {
            case 0: Shell.run(Commands.MEDIA_PLAY); break;
            case 1: mc.getTransportControls().play(); break;
            default: Shell.run(Commands.KEY_PLAY); break;
        }''', '        mc.getTransportControls().play(); // Only the forwarded Spotify TV session.')
    replace(skipper, 'MediaController mc = latest(c, old);\n            PlaybackState ps = mc == null ? null : mc.getPlaybackState();',
            'MediaController mc = latest(c, old);\n'
            '            if (!TvBridge.mayResume(c)) {\n'
            '                if (mc != null) mc.getTransportControls().pause();\n'
            '                return Result.NOT_PLAYING;\n'
            '            }\n'
            '            PlaybackState ps = mc == null ? null : mc.getPlaybackState();')
    # Repair must not send global media keys that could control another TV app.
    replace(skipper, 'Shell.run(Commands.REPAIR_MUTE);',
            'Shell.run(Commands.ALLOW_AUDIO);\n'
            '                if (mc != null) mc.getTransportControls().play();')

    # Hush's optional advanced timer can also use the native session without notification access.
    timer = source / 'SleepTimer.java'
    text = timer.read_text(encoding='utf-8')
    text = text.replace('c.getSystemService(MediaSessionManager.class)\n                    .getActiveSessions(new ComponentName(c, AdWatcher.class))', 'TvBridge.sessions(c)')
    text = text.replace('''                AudioManager am = c.getSystemService(AudioManager.class);
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));''',
                        '                // No global media keys: another TV app may now be playing.')
    timer.write_text(text, encoding='utf-8')

    hush = source / 'Hush.java'
    replace(hush, 'p.enabled(on);', 'p.enabled(on);\n        TvService.enabled(c, on);')
    replace(hush, 'if (p.enabled()) { applyDns(c); refreshNotification(c); }',
            'if (p.enabled()) { applyDns(c); refreshNotification(c); TvService.enabled(c, true); }')

    for path in (ROOT / 'overlay/java').rglob('*.java'):
        target = destination / 'app/src/main/java' / path.relative_to(ROOT / 'overlay/java')
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)

    manifest_path = destination / 'app/src/main/AndroidManifest.xml'
    tree = ET.parse(manifest_path)
    manifest = tree.getroot()
    key = lambda name: '{' + ANDROID + '}' + name
    permission = 'dev.aurora.hushtv.permission.TV_BRIDGE'
    manifest.insert(0, ET.Element('permission', {key('name'): permission, key('protectionLevel'): 'signature'}))
    manifest.insert(1, ET.Element('uses-permission', {key('name'): permission}))
    manifest.insert(2, ET.Element('uses-feature', {key('name'): 'android.hardware.touchscreen', key('required'): 'false'}))
    manifest.insert(3, ET.Element('uses-feature', {key('name'): 'android.software.leanback', key('required'): 'false'}))
    app = manifest.find('application')
    app.set(key('label'), 'Hush · Spotify+ TV')
    app.set(key('banner'), '@drawable/tv_banner')
    activity = ET.SubElement(app, 'activity', {key('name'): '.TvActivity', key('exported'): 'true'})
    intent = ET.SubElement(activity, 'intent-filter')
    ET.SubElement(intent, 'action', {key('name'): 'android.intent.action.MAIN'})
    ET.SubElement(intent, 'category', {key('name'): 'android.intent.category.LEANBACK_LAUNCHER'})
    ET.SubElement(app, 'receiver', {key('name'): '.TvReceiver', key('exported'): 'true', key('permission'): permission})
    service = ET.SubElement(app, 'service', {key('name'): '.TvService', key('exported'): 'false', key('foregroundServiceType'): 'specialUse'})
    ET.SubElement(service, 'property', {key('name'): 'android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE',
                  key('value'): 'User-enabled Spotify TV ad detection and restart recovery'})
    tree.write(manifest_path, encoding='utf-8', xml_declaration=True)
    assets = destination / 'app/src/main/assets'
    assets.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(upstream / 'LICENSE', assets / 'HUSH-COPYING.txt')
    (assets / 'HUSH-SOURCE.txt').write_text('Hush 2.0.2 by coldMEW, GPL-3.0\n'
        f'Upstream https://github.com/coldMEW/Hush commit {PIN}\n'
        'TV changes and reproducible source: https://github.com/AlphadogRSA/spotifyplus-tv-patch\n', encoding='utf-8')
    drawable = destination / 'app/src/main/res/drawable'
    drawable.mkdir(exist_ok=True)
    (drawable / 'tv_banner.xml').write_text('''<vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="320dp" android:height="180dp" android:viewportWidth="320" android:viewportHeight="180">
        <path android:fillColor="#10191F" android:pathData="M0,0h320v180h-320z"/>
        <path android:fillColor="#91E7CF" android:pathData="M112,42h16v36h64v-36h16v96h-16v-44h-64v44h-16z"/>
        </vector>''', encoding='utf-8')
    return destination

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / 'build/android')
    print(prepare(parser.parse_args().output))
