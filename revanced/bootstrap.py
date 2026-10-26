"""Fetch the pinned official build-host API/runtime; never fetch Spotify APKs."""
import hashlib
import pathlib
import urllib.request

VERSION = '6.0.0'
SHA256 = 'c25549bc17d59d2eb94fa5f86e60e9b77a02772ca88f7050f8f1276f923a9958'
TARGET = pathlib.Path(__file__).resolve().parent / '.tools' / f'revanced-cli-{VERSION}-all.jar'


def main():
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    if TARGET.is_file() and hashlib.sha256(TARGET.read_bytes()).hexdigest() == SHA256:
        print('Pinned ReVanced CLI/API verified')
        return
    url = f'https://github.com/ReVanced/revanced-cli/releases/download/v{VERSION}/revanced-cli-{VERSION}-all.jar'
    temporary = TARGET.with_suffix('.download')
    request = urllib.request.Request(url, headers={'User-Agent': 'SpotifyPlusTV-Build/0.3'})
    with urllib.request.urlopen(request, timeout=60) as response, temporary.open('wb') as output:
        while chunk := response.read(1024 * 1024):
            output.write(chunk)
    if hashlib.sha256(temporary.read_bytes()).hexdigest() != SHA256:
        temporary.unlink()
        raise SystemExit('Official build-host checksum mismatch; refusing to execute it')
    temporary.replace(TARGET)
    print('Pinned ReVanced CLI/API downloaded and verified')


if __name__ == '__main__':
    main()
