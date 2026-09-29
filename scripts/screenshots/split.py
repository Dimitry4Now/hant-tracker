"""Combines each light/dark screenshot pair into one image, split diagonally:
light on the left, dark on the right, along a line ~20 degrees off vertical.

    python3 split.py [src-dir] [out-dir]

Reads <name>-light.png / <name>-dark.png from src-dir (default docs/screenshots)
and writes <name>.png to out-dir (default docs/screenshots/split). Needs Pillow.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ANGLE = 20  # degrees off vertical
SS = 4      # supersampling factor, for an anti-aliased edge

ROOT = Path(__file__).resolve().parents[2]


def line_mask(size, top, bottom, width):
    """A supersampled line from (top, 0) to (bottom, h), scaled back down."""
    w, h = size
    big = Image.new('L', (w * SS, h * SS), 0)
    ImageDraw.Draw(big).line([(top * SS, 0), (bottom * SS, h * SS)], fill=255, width=width * SS)
    return big.resize(size, Image.LANCZOS)


def split(light_path, dark_path, out_path):
    light = Image.open(light_path).convert('RGB')
    dark = Image.open(dark_path).convert('RGB')
    if light.size != dark.size:
        sys.exit(f'{light_path} and {dark_path} differ in size')
    w, h = light.size

    # The split runs through the centre, leaning like "/".
    t = math.tan(math.radians(ANGLE))
    top = w / 2 + (h / 2) * t
    bottom = w / 2 - (h / 2) * t

    # 255 where the dark half shows
    big = Image.new('L', (w * SS, h * SS), 0)
    ImageDraw.Draw(big).polygon(
        [(top * SS, 0), (w * SS * 2, 0), (w * SS * 2, h * SS), (bottom * SS, h * SS)], fill=255)
    mask = big.resize((w, h), Image.LANCZOS)
    out = Image.composite(dark, light, mask)

    # A soft shadow the dark half casts onto the light one, then a thin seam.
    lw = max(2, round(w / 900))
    shadow = line_mask((w, h), top, bottom, lw * 6).filter(ImageFilter.GaussianBlur(lw * 6))
    shadow = Image.composite(Image.new('L', (w, h), 0), shadow, mask).point(lambda v: int(v * 0.35))
    out = Image.composite(Image.new('RGB', (w, h), (0, 0, 0)), out, shadow)

    seam = line_mask((w, h), top, bottom, lw).point(lambda v: int(v * 0.85))
    out = Image.composite(Image.new('RGB', (w, h), (255, 255, 255)), out, seam)

    out.save(out_path, optimize=True)
    print(out_path)


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'docs/screenshots'
    dst = Path(sys.argv[2]) if len(sys.argv) > 2 else src / 'split'
    dst.mkdir(parents=True, exist_ok=True)
    for light in sorted(src.glob('*-light.png')):
        name = light.name[:-len('-light.png')]
        split(light, src / f'{name}-dark.png', dst / f'{name}.png')


if __name__ == '__main__':
    main()
