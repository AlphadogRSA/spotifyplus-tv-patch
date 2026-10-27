"""Generate the offline developer QR asset. Install qrcode==8.2 and pypng==0.20220715.0."""
from pathlib import Path
import qrcode
from qrcode.image.pure import PyPNGImage
URL='https://github.com/AlphadogRSA'
ROOT=Path(__file__).resolve().parents[1]
qr=qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_Q,box_size=12,border=4)
qr.add_data(URL);qr.make(fit=True)
qr.make_image(image_factory=PyPNGImage).save(str(ROOT/'assets/developer-github-qr.png'))
print('Generated developer QR:',URL)

