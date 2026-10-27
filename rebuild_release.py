"""Rebuild the exact previous personal TV release as one unsigned 1.3.0.1 APK.

Requires compiled extension DEX and the checksum-pinned APK tooling. No signing keys are read.
"""
import argparse
import hashlib
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT=Path(__file__).resolve().parent
INPUT_SHA='fd14f65b1a282621c2f4873d3323370046527dbf2c22384ba14514203ea7d17d'
TOOL_SHA={
 'apktool':'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
 'apkeditor':'a9cd40df818845456be6d696de6110c89edf4b0a0580cb83438ed6b25a366e67',
}
VERSION='1.3.0.1'
def verified(path,sha):
 if hashlib.sha256(path.read_bytes()).hexdigest()!=sha:raise ValueError(f'Checksum mismatch: {path.name}')
def run(*args):subprocess.run(list(map(str,args)),check=True)
def rebuild(source,dex,apktool,apkeditor,output):
 verified(source,INPUT_SHA);verified(apktool,TOOL_SHA['apktool']);verified(apkeditor,TOOL_SHA['apkeditor'])
 if output.exists():raise ValueError('Output directory must be new')
 if not dex.is_file():raise ValueError('Compiled extension DEX is required')
 output.mkdir(parents=True);parts=output/'splits';parts.mkdir()
 names=['base.apk','split_config.armeabi_v7a.apk','split_config.en.apk','split_config.tvdpi.apk']
 with zipfile.ZipFile(source) as archive:
  for name in names:(parts/name).write_bytes(archive.read(name))
 decoded=output/'decoded'
 run('java','-jar',apktool,'d',parts/'base.apk','-o',decoded)
 removed=0
 for smali in decoded.glob('smali*'):
  old=smali/'dev/aurora/tvpatch'
  if old.exists():shutil.rmtree(old);removed+=1
 if not removed:raise ValueError('Expected the previous TV extension; refusing to patch an unknown client')
 manifest=decoded/'AndroidManifest.xml';tree=ET.parse(manifest);root=tree.getroot()
 ns='http://schemas.android.com/apk/res/android';ET.register_namespace('android',ns)
 attr=lambda k:'{'+ns+'}'+k
 app=root.find('application')
 if root.get('package')!='com.spotify.tv.android':raise ValueError('Unexpected Android package')
 if any('sptv_hush_helper' in x.get(attr('authorities'),'') for x in app.findall('provider')):
  raise ValueError('This input already includes the integrated helper')
 ET.SubElement(app,'provider',{attr('name'):'dev.aurora.tvpatch.hush.HelperProvider',
     attr('authorities'):'com.spotify.tv.android.sptv_hush_helper',attr('exported'):'true',
     attr('permission'):'android.permission.INTERACT_ACROSS_USERS_FULL'})
 tree.write(manifest,encoding='utf-8',xml_declaration=True)
 metadata=decoded/'apktool.yml';text=metadata.read_text()
 text,count=re.subn(r'(?m)^  versionName: 1\.134\.2$',f'  versionName: {VERSION}',text)
 if count!=1:raise ValueError('Unexpected source version')
 metadata.write_text(text)
 for path in (ROOT/'assets').iterdir():
  if path.is_file():shutil.copyfile(path,decoded/'assets'/path.name)
 unsigned_base=output/'rebuilt-base.apk'
 run('java','-jar',apktool,'b',decoded,'-o',unsigned_base)
 # Hook calls in the original app remain; the previous runtime definitions were removed above.
 # Append exactly one new DEX without copying any stale compiled extension classes.
 with zipfile.ZipFile(unsigned_base) as archive:
  files=[(info,archive.read(info.filename)) for info in archive.infolist()]
  used=set(archive.namelist())
 number=2
 while f'classes{number}.dex' in used:number+=1
 with zipfile.ZipFile(parts/'base.apk','w') as archive:
  for info,data in files:
   if not info.filename.upper().startswith('META-INF/'):
    archive.writestr(info,data)
  archive.writestr(f'classes{number}.dex',dex.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
 final=output/f'SpotifyPlus-TV-{VERSION}-unsigned.apk'
 run('java','-jar',apkeditor,'m','-i',parts,'-o',final,'-validate-modules')
 (output/'SHA256.txt').write_text(hashlib.sha256(final.read_bytes()).hexdigest()+'  '+final.name+'\n')
 print(f'Unsigned single-app APK: {final}')
 return final
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--input',type=Path,required=True);p.add_argument('--dex',type=Path,required=True)
 p.add_argument('--apktool',type=Path,required=True);p.add_argument('--apkeditor',type=Path,required=True)
 p.add_argument('--output',type=Path,required=True)
 a=p.parse_args();rebuild(a.input,a.dex,a.apktool,a.apkeditor,a.output)
