from pathlib import Path
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont


WIDTH = 900
HEIGHT = 720
OUTPUT_DIR = Path(__file__).resolve().parents[1] / "public" / "assets" / "wines"

WINES = [
    ("chateau-lueur-noire", "CHATEAU", "LUEUR NOIRE", "BORDEAUX 2018", "red", (88, 17, 28), (225, 202, 152)),
    ("monteluna-estate", "MONTELUNA", "ESTATE", "TOSCANA 2019", "red", (112, 28, 25), (231, 216, 186)),
    ("aotearoa-cellars", "AOTEAROA", "CELLARS", "MARLBOROUGH 2021", "white", (203, 183, 91), (238, 241, 220)),
    ("valle-di-sera", "VALLE DI", "SERA", "PIEMONTE 2017", "red", (71, 12, 35), (220, 186, 154)),
    ("moonlight-spark", "MOONLIGHT", "SPARK", "CHAMPAGNE 2020", "sparkling", (196, 158, 65), (248, 236, 207)),
    ("sakura-reserve", "SAKURA", "RESERVE", "YAMANASHI 2020", "white", (211, 185, 105), (247, 226, 230)),
]


def font(size: int, serif: bool = False) -> ImageFont.FreeTypeFont:
    name = "C:/Windows/Fonts/georgia.ttf" if serif else "C:/Windows/Fonts/arial.ttf"
    return ImageFont.truetype(name, size)


def vertical_gradient(top, bottom):
    image = Image.new("RGB", (WIDTH, HEIGHT))
    pixels = image.load()
    for y in range(HEIGHT):
        ratio = y / (HEIGHT - 1)
        color = tuple(int(top[i] * (1 - ratio) + bottom[i] * ratio) for i in range(3))
        for x in range(WIDTH):
            pixels[x, y] = color
    return image


def add_bokeh(image, seed, colors):
    random.seed(seed)
    layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for _ in range(22):
        radius = random.randint(18, 75)
        x = random.randint(-radius, WIDTH + radius)
        y = random.randint(20, 520)
        color = random.choice(colors)
        draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=(*color, random.randint(20, 65)))
    image.alpha_composite(layer.filter(ImageFilter.GaussianBlur(24)))


def draw_bottle(image, wine, index):
    slug, line1, line2, origin, category, liquid, label_color = wine
    bottle = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(bottle)

    center = WIDTH // 2 + (index % 3 - 1) * 8
    body_left, body_right = center - 142, center + 142
    body_top, body_bottom = 195, 675
    neck_left, neck_right = center - 58, center + 58

    draw.ellipse((center - 190, 645, center + 190, 708), fill=(0, 0, 0, 105))
    draw.rounded_rectangle((body_left, body_top, body_right, body_bottom), radius=82, fill=(*liquid, 238))
    draw.rectangle((neck_left, 102, neck_right, 250), fill=(*liquid, 245))
    draw.rounded_rectangle((neck_left - 5, 74, neck_right + 5, 145), radius=18, fill=(44, 28, 18, 255))

    if category == "sparkling":
        draw.rounded_rectangle((neck_left - 8, 69, neck_right + 8, 158), radius=20, fill=(190, 148, 58, 255))
        draw.line((neck_left - 2, 85, neck_right + 2, 142), fill=(240, 215, 143, 180), width=5)

    for offset, alpha, width in [(24, 100, 18), (48, 48, 10)]:
        draw.rounded_rectangle(
            (body_left + offset, body_top + 20, body_left + offset + width, body_bottom - 44),
            radius=width // 2,
            fill=(255, 255, 255, alpha),
        )
    draw.arc((body_left + 15, body_top + 15, body_right - 15, body_bottom - 20), 280, 80, fill=(255, 255, 255, 55), width=5)

    label_top = 326 if index % 2 == 0 else 342
    draw.rounded_rectangle((center - 112, label_top, center + 112, label_top + 210), radius=10, fill=(*label_color, 255))
    draw.rectangle((center - 112, label_top, center + 112, label_top + 11), fill=(*liquid, 255))
    draw.line((center - 87, label_top + 72, center + 87, label_top + 72), fill=(*liquid, 150), width=2)
    draw.line((center - 72, label_top + 161, center + 72, label_top + 161), fill=(*liquid, 120), width=2)

    draw.text((center, label_top + 29), line1, anchor="mm", font=font(25, True), fill=(42, 29, 22))
    draw.text((center, label_top + 106), line2, anchor="mm", font=font(31, True), fill=liquid)
    draw.text((center, label_top + 184), origin, anchor="mm", font=font(15), fill=(70, 54, 43))

    if category == "sparkling":
        random.seed(80 + index)
        for _ in range(34):
            x = random.randint(body_left + 30, body_right - 30)
            y = random.randint(body_top + 50, body_bottom - 35)
            radius = random.randint(2, 5)
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), outline=(255, 250, 210, 125), width=1)

    image.alpha_composite(bottle.filter(ImageFilter.GaussianBlur(0.45)))


def generate():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for index, wine in enumerate(WINES):
        category = wine[4]
        if category == "red":
            top, bottom = (66, 37, 28), (20, 10, 9)
            bokeh = [(150, 72, 40), (217, 155, 83), (92, 22, 30)]
        elif category == "sparkling":
            top, bottom = (94, 74, 38), (28, 21, 13)
            bokeh = [(244, 218, 143), (173, 121, 44), (255, 244, 203)]
        else:
            top, bottom = (74, 83, 64), (17, 24, 21)
            bokeh = [(212, 201, 127), (119, 153, 113), (238, 226, 184)]

        image = vertical_gradient(top, bottom).convert("RGBA")
        add_bokeh(image, index + 20, bokeh)
        draw_bottle(image, wine, index)

        draw = ImageDraw.Draw(image)
        draw.text((42, 42), wine[3], font=font(20), fill=(255, 245, 219, 165))
        draw.text((42, 70), wine[4].upper(), font=font(14), fill=(255, 245, 219, 110))

        output = OUTPUT_DIR / f"{wine[0]}.jpg"
        image.convert("RGB").save(output, "JPEG", quality=94, optimize=True, progressive=True)
        print(f"generated {output.name}")


if __name__ == "__main__":
    generate()