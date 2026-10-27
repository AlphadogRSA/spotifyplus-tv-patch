import importlib.util
import tempfile
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

class PreparationTest(unittest.TestCase):
    def test_tv_project_is_separate_and_bridge_is_signature_protected(self):
        self.assertTrue((ROOT / 'prepare.py').exists(), 'TV preparation feature is missing')
        spec = importlib.util.spec_from_file_location('prepare', ROOT / 'prepare.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory) / 'android'
            module.prepare(out)
            manifest = ET.parse(out / 'app/src/main/AndroidManifest.xml').getroot()
            android = '{http://schemas.android.com/apk/res/android}'
            self.assertEqual(manifest.find('permission').get(android + 'protectionLevel'), 'signature')
            receiver = next(x for x in manifest.findall('application/receiver') if x.get(android + 'name') == '.TvReceiver')
            self.assertEqual(receiver.get(android + 'permission'), 'dev.aurora.hushtv.permission.TV_BRIDGE')
            self.assertTrue(any(x.get(android+'name') == 'android.intent.category.LEANBACK_LAUNCHER' for x in manifest.findall('.//category')))
            self.assertIn('applicationId = "dev.aurora.hushtv"', (out / 'app/build.gradle.kts').read_text())
            source = out / 'app/src/main/java/io/coldmew/hush'
            self.assertIn('com.spotify.tv.android', (source / 'Hush.java').read_text())
            self.assertIn('TvBridge.sessions(c)', (source / 'Skipper.java').read_text())
            self.assertNotIn('am.adjustStreamVolume', (source / 'AdWatcher.java').read_text())
            self.assertTrue((out / 'app/src/main/assets/HUSH-COPYING.txt').exists())
            service = next(x for x in manifest.findall('application/service') if x.get(android+'name') == '.TvService')
            self.assertEqual(service.get(android+'exported'), 'false')
            self.assertEqual(service.get(android+'foregroundServiceType'), 'specialUse')

if __name__ == '__main__':
    unittest.main()
