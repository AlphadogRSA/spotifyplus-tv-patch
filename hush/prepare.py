#!/usr/bin/env python3
"""Extract the pinned Hush helper sources for embedding in Spotify's single APK."""
from pathlib import Path
import argparse
import shutil
import subprocess
ROOT=Path(__file__).resolve().parent
PIN='4e9ded397338de5c69b1784c636feb3d34a8de01'
FILES=('Actions','Commands','Exec','Helper','HelperProtocol','HelperProvider','HushHelper','Providers')
def prepare(destination):
 upstream=ROOT/'upstream'
 if subprocess.check_output(['git','-C',str(upstream),'rev-parse','HEAD'],text=True).strip()!=PIN:
  raise ValueError('Hush gitlink differs from the pinned source')
 subprocess.run(['git','-C',str(upstream),'diff','--exit-code','HEAD','--'],check=True,stdout=subprocess.DEVNULL)
 if destination.exists():raise ValueError('Destination must be new')
 target=destination/'dev/aurora/tvpatch/hush';target.mkdir(parents=True)
 for name in FILES:
  text=(upstream/f'android/app/src/main/java/io/coldmew/hush/helper/{name}.java').read_text()
  text=text.replace('io.coldmew.hush.helper','dev.aurora.tvpatch.hush')
  text=text.replace('com.spotify.music','com.spotify.tv.android')
  if name=='HelperProtocol':
   text=text.replace('VERSION = 3','VERSION = 4').replace('".helper"','".sptv_hush_helper"')
  if name=='Commands':
   start=text.index('        switch (action) {');end=text.index('\n    private static String open(',start)
   text=text[:start]+'''        if (action != RESTART || !"com.spotify.tv.android/.SpotifyTVActivity".equals(a))
            throw new IllegalArgumentException("Only the fixed Spotify TV restart action is supported");
        return STOP + "; " + ALLOWLIST + "; " + open(a) + "; " + ALLOW;
    }
''' +text[end:]
  (target/f'{name}.java').write_text('// Adapted from coldMEW/Hush 2.0.2. SPDX-License-Identifier: GPL-3.0-only\n'+text)
 return destination
if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,default=ROOT/'build/generated')
 print(prepare(parser.parse_args().output))
