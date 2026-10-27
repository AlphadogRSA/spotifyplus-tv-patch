"""Import your own/licensed UTF-8 LRC into the installed TV patch."""
import argparse
import pathlib
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('file', type=pathlib.Path)
    parser.add_argument('--name', required=True, help='Exact .lrc filename displayed by Local synchronized lyrics')
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial')
    args = parser.parse_args()
    if not re.fullmatch(r'(?:[A-Za-z0-9]{22}|track-[0-9a-f]{1,8})\.lrc', args.name):
        parser.error('Use the exact safe .lrc filename displayed in the lyrics overlay')
    data = args.file.read_bytes()
    data.decode('utf-8-sig')
    if len(data) > 524000:
        parser.error('LRC must be smaller than 524 KB')
    adb = [args.adb] + (['-s', args.serial] if args.serial else [])
    path = 'files/sptv-lyrics/' + args.name
    command = f'"umask 077; mkdir -p files/sptv-lyrics; cat > {path}.tmp && mv {path}.tmp {path}"'
    subprocess.run(adb + ['shell', 'run-as', 'com.spotify.tv.android', 'sh', '-c', command], input=data, check=True)
    print('Imported', args.name, '- open Lyrics · local / LRCLIB in Spotify TV +.')


if __name__ == '__main__':
    main()
