"""Builds the RuneRealm login-screen assets packed into the client cache.

The client draws the login background from `title.jpg` (archive 10) as the left half of the
765x503 screen and a mirrored copy of it as the right half, then draws the `logo` sprite
(archive 8) centred at x=382, y=18. To show a full-width, non-mirrored image:
- `title.jpg` holds the left half of the image.
- `logo` holds the right half (x 382..764, y 18..502) and is offset so it lands at x=382.

Usage: python3 "Branding Resources/build_client_branding.py"
"""

import io
import struct
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "Branding Resources" / "RuneRealm Background.png"
OUT = ROOT / "server/app/src/main/resources/org/runerealm/server/app/branding"

WIDTH, HEIGHT = 765, 503
HALF = 383
LOGO_Y = 18


def main() -> None:
    image = Image.open(SOURCE).convert("RGB")
    scaled_height = round(image.height * WIDTH / image.width)
    image = image.resize((WIDTH, scaled_height), Image.LANCZOS).crop((0, 0, WIDTH, HEIGHT))

    left = image.crop((0, 0, HALF, HEIGHT))
    jpg = io.BytesIO()
    left.save(jpg, "JPEG", quality=92, optimize=True, progressive=False)
    (OUT / "title.jpg").write_bytes(jpg.getvalue())

    right = image.crop((WIDTH - HALF, LOGO_Y, WIDTH, HEIGHT))
    (OUT / "logo.dat").write_bytes(encode_sprite(right))


def encode_sprite(image: Image.Image) -> bytes:
    """Encodes [image] as a single-frame, palette-indexed cache sprite."""
    quantized = image.quantize(colors=255, method=Image.Quantize.MEDIANCUT,
                               dither=Image.Dither.FLOYDSTEINBERG)
    raw_palette = quantized.getpalette()[: 255 * 3]
    colours = len(raw_palette) // 3
    width, height = quantized.size
    # Palette index 0 is transparent in the client, so shift every pixel up by one.
    pixels = bytes(p + 1 for p in quantized.tobytes())

    out = bytearray()
    out.append(0)  # Row-major pixels, no alpha.
    out += pixels
    for i in range(colours):
        r, g, b = raw_palette[i * 3: i * 3 + 3]
        out += bytes((r, g, b))
    # Offsets centre the frame on x=382 whether the client centres on the frame or max width.
    out += struct.pack(">HHB", width, height, colours)
    out += struct.pack(">H", width // 2)  # x offset
    out += struct.pack(">H", 0)  # y offset
    out += struct.pack(">H", width)
    out += struct.pack(">H", height)
    out += struct.pack(">H", 1)  # Frame count.
    return bytes(out)


if __name__ == "__main__":
    main()
