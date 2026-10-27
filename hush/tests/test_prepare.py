import importlib.util
from pathlib import Path
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[1]
class HelperTest(unittest.TestCase):
 def test_single_apk_helper_sources(self):
  spec=importlib.util.spec_from_file_location('prepare',ROOT/'prepare.py'); m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
  with tempfile.TemporaryDirectory() as d:
   m.prepare(Path(d)/'generated')
   helper=Path(d)/'generated/dev/aurora/tvpatch/hush'
   self.assertTrue((helper/'Helper.java').exists())
   commands=(helper/'Commands.java').read_text()
   self.assertIn('Only the fixed Spotify TV restart action is supported',commands)
   self.assertIn('com.spotify.tv.android',commands)
   self.assertFalse(list((Path(d)/'generated').rglob('AndroidManifest.xml')))
