"""Inject the original TV extension into the user's exact Spotify TV 1.134.2 APK.

Does not download Spotify, sign packages, install, read login data or alter audio/DRM.
Use Android SDK tools to compile src/, dex the classes, align and sign all APKs.
"""
import argparse
import hashlib
import pathlib
import re
import shutil
import subprocess
import zipfile
import xml.etree.ElementTree as ET

BASE_SHA256 = 'e33df37a08eaa8d97e3baf0b25b50904bc3036eb83c9945df1694d9c51128f14'
ACTIVITY = 'Lcom/spotify/tv/android/SpotifyTVActivity;'
HOOK = 'Ldev/aurora/tvpatch/Hooks;'


def method_patch(source, signature, action):
    pattern = re.compile(r'(?m)^\.method ' + re.escape(signature) + r'\n.*?^\.end method', re.S)
    matches = list(pattern.finditer(source))
    if len(matches) != 1:
        raise ValueError(f'Expected one method: {signature}, found {len(matches)}')
    match = matches[0]
    return source[:match.start()] + action(match.group()) + source[match.end():]


def inject(decoded):
    activity = decoded / 'smali/com/spotify/tv/android/SpotifyTVActivity.smali'
    source = activity.read_text(encoding='utf-8')
    if HOOK in source:
        raise ValueError('Input is already patched; decode the original APK again')
    source = method_patch(source, 'public final onCreate(Landroid/os/Bundle;)V',
        lambda body: body.replace('    return-void', f'    invoke-static {{p0}}, {HOOK}->attach(Landroid/app/Activity;)V\n\n    return-void'))
    source = method_patch(source, 'public final onDestroy()V',
        lambda body: body.replace('    .locals 5', f'    .locals 5\n\n    invoke-static {{p0}}, {HOOK}->detach(Landroid/app/Activity;)V', 1))
    prologue = f'''    .locals 8

    invoke-static {{p0, p1}}, {HOOK}->key(Landroid/app/Activity;Landroid/view/KeyEvent;)Z
    move-result v0
    if-eqz v0, :sptv_original_keys
    const/4 v0, 0x1
    return v0
    :sptv_original_keys'''
    source = method_patch(source, 'public final dispatchKeyEvent(Landroid/view/KeyEvent;)Z',
        lambda body: body.replace('    .locals 8', prologue, 1))
    activity.write_text(source, encoding='utf-8')

    client = decoded / 'smali/VC.smali'
    source = client.read_text(encoding='utf-8')
    source = method_patch(source, 'public final onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V',
        lambda body: re.sub(r'(    \.locals \d+)', r'\1\n\n    invoke-static {p1}, ' + HOOK + '->page(Landroid/webkit/WebView;)V', body, count=1))
    client.write_text(source, encoding='utf-8')

    manager = decoded / 'smali/com/spotify/tv/android/audio/PlayerStateManager.smali'
    source = manager.read_text(encoding='utf-8')
    assignment = '    iput-object v2, p0, Lcom/spotify/tv/android/audio/PlayerStateManager;->m:Landroid/support/v4/media/session/MediaSessionCompat;'
    if source.count(assignment) != 1:
        raise ValueError('Media-session hook no longer matches this client')
    source = source.replace(assignment, assignment + f'\n\n    invoke-static {{v2}}, {HOOK}->bindSession(Ljava/lang/Object;)V')
    manager.write_text(source, encoding='utf-8')

    manifest = decoded / 'AndroidManifest.xml'
    android = '{http://schemas.android.com/apk/res/android}'
    ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
    tree = ET.parse(manifest)
    app = tree.getroot().find('application')
    app.set(android + 'debuggable', 'true')  # Personal sideload; local LRC run-as import only.
    app.set(android + 'label', 'Spotify TV +')
    for child in app.findall('activity'):
        if child.get(android + 'name') == 'com.spotify.tv.android.SpotifyTVActivity':
            child.set(android + 'label', 'Spotify TV +')
    tree.write(manifest, encoding='utf-8', xml_declaration=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', required=True, type=pathlib.Path)
    parser.add_argument('--apktool', required=True, type=pathlib.Path)
    parser.add_argument('--java', required=True)
    parser.add_argument('--work', required=True, type=pathlib.Path, help='New directory; never overwritten')
    parser.add_argument('--dex', required=True, type=pathlib.Path, help='D8 output classes.dex for src/')
    parser.add_argument('--framework', required=True, type=pathlib.Path)
    args = parser.parse_args()
    if hashlib.sha256(args.base.read_bytes()).hexdigest() != BASE_SHA256:
        parser.error('Wrong original APK; this patch is pinned to the inspected 1.134.2 base APK')
    args.work.mkdir(parents=True, exist_ok=False)
    decoded = args.work / 'decoded'
    subprocess.run([args.java, '-jar', str(args.apktool), 'd', '-p', str(args.framework), '-o', str(decoded), str(args.base)], check=True)
    inject(decoded)
    here = pathlib.Path(__file__).resolve().parent
    shutil.copytree(here / 'assets', decoded / 'assets', dirs_exist_ok=True)
    unsigned = args.work / 'patched-unsigned.apk'
    subprocess.run([args.java, '-jar', str(args.apktool), 'b', '-p', str(args.framework), '-o', str(unsigned), str(decoded)], check=True)
    with zipfile.ZipFile(unsigned, 'a', zipfile.ZIP_DEFLATED) as z:
        if 'classes2.dex' in z.namelist():
            raise ValueError('Unexpected existing classes2.dex')
        z.write(args.dex, 'classes2.dex')
    with zipfile.ZipFile(unsigned) as z:
        bad = z.testzip()
        if bad:
            raise ValueError('APK ZIP integrity failed: ' + bad)
    print('Prepared unsigned personal patch:', unsigned)


if __name__ == '__main__':
    main()
