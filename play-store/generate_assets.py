"""Render Play Store art from the app's code-native clock symbol."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent
NAVY = (14, 17, 27)
PANEL = (34, 39, 55)
LAVENDER = (185, 182, 255)
WHITE = (233, 235, 250)
BLUE = (123, 204, 239)


def clock(draw, cx, cy, radius, width):
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), outline=LAVENDER, width=width)
    draw.line((cx, cy - radius * 0.58, cx, cy), fill=WHITE, width=width)
    draw.line((cx, cy, cx + radius * 0.5, cy + radius * 0.3), fill=WHITE, width=width)
    draw.arc((cx - radius * 1.25, cy - radius * 1.3, cx - radius * 0.15, cy - radius * 0.4), 195, 320, fill=LAVENDER, width=width)
    draw.arc((cx + radius * 0.15, cy - radius * 1.3, cx + radius * 1.25, cy - radius * 0.4), 220, 345, fill=LAVENDER, width=width)


icon = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
d = ImageDraw.Draw(icon)
d.rounded_rectangle((16, 16, 496, 496), radius=112, fill=PANEL)
clock(d, 256, 272, 132, 24)
icon.save(OUT / "icon-512.png", optimize=True)

graphic = Image.new("RGB", (1024, 500), NAVY)
g = ImageDraw.Draw(graphic)
for x in range(1024):
    t = x / 1023
    g.line((x, 0, x, 499), fill=(int(14 + 22 * t), int(17 + 18 * t), int(27 + 34 * t)))
g.rounded_rectangle((55, 84, 345, 374), radius=65, fill=PANEL)
clock(g, 200, 240, 76, 14)
latin = ImageFont.truetype('/System/Library/Fonts/Avenir Next.ttc', 68)
korean = ImageFont.truetype('/System/Library/Fonts/AppleSDGothicNeo.ttc', 34)
g.text((390, 155), 'Minute Voice', font=latin, fill=WHITE)
g.text((397, 262), '매분, 시간을 말해주는 알람', font=korean, fill=BLUE)
graphic.save(OUT / "feature-1024x500.png", optimize=True)
