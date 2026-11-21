from __future__ import annotations

import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES_DIR = ROOT / "app" / "src" / "main" / "res"
PREVIEW_PATH = ROOT / "scripts" / "icon_preview.png"

SS = 4
ADAPTIVE_SIZE = 432
WORK_SIZE = ADAPTIVE_SIZE * SS  # 1728x1728


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def lerp_color(c1: tuple[int, int, int], c2: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    t = max(0.0, min(1.0, t))
    return (
        int(round(lerp(c1[0], c2[0], t))),
        int(round(lerp(c1[1], c2[1], t))),
        int(round(lerp(c1[2], c2[2], t))),
    )


def build_background(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (11, 13, 19, 255))
    draw = ImageDraw.Draw(img, "RGBA")

    # Subtle ZZZ diagonal industrial hazard stripes
    stripe_w = int(size * 0.048)
    step = stripe_w * 2
    for offset in range(-size, size * 2, step):
        pts = [
            (offset, size),
            (offset + stripe_w, size),
            (offset + stripe_w + size, 0),
            (offset + size, 0),
        ]
        draw.polygon(pts, fill=(19, 23, 33, 255))

    # Ambient golden-amber core glow + faint cyan edge accent
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow, "RGBA")
    cx, cy = size // 2, size // 2
    max_r = int(size * 0.44)
    for step_i in range(28, 0, -1):
        frac = step_i / 28.0
        r = int(max_r * frac)
        alpha = int(52 * (1.0 - frac) ** 1.9)
        gdraw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(245, 166, 35, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=size * 0.04))
    return Image.alpha_composite(img, glow)


def draw_four_point_star(
    draw: ImageDraw.ImageDraw,
    cx: float,
    cy: float,
    outer_r: float,
    inner_r: float,
    fill: tuple[int, int, int, int],
    outline: tuple[int, int, int, int] | None = None,
    width: int = 1,
) -> None:
    pts = []
    for i in range(8):
        angle = -math.pi / 2 + i * (math.pi / 4)
        r = outer_r if i % 2 == 0 else inner_r
        pts.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    draw.polygon(pts, fill=fill)
    if outline is not None:
        draw.line(pts + [pts[0]], fill=outline, width=width, joint="curve")


def draw_arc_band(
    img: Image.Image,
    cx: float,
    cy: float,
    radius: float,
    thickness: float,
    start_deg: float,
    end_deg: float,
    c_start: tuple[int, int, int],
    c_end: tuple[int, int, int],
    alpha: int = 255,
) -> None:
    draw = ImageDraw.Draw(img, "RGBA")
    segments = max(32, int(abs(end_deg - start_deg) * 1.8))
    r_in = radius - thickness / 2
    r_out = radius + thickness / 2
    for i in range(segments):
        t0 = i / segments
        t1 = (i + 1) / segments
        a0 = math.radians(lerp(start_deg, end_deg, t0))
        a1 = math.radians(lerp(start_deg, end_deg, t1))
        col = lerp_color(c_start, c_end, (t0 + t1) * 0.5)
        pts = [
            (cx + r_out * math.cos(a0), cy + r_out * math.sin(a0)),
            (cx + r_out * math.cos(a1), cy + r_out * math.sin(a1)),
            (cx + r_in * math.cos(a1), cy + r_in * math.sin(a1)),
            (cx + r_in * math.cos(a0), cy + r_in * math.sin(a0)),
        ]
        draw.polygon(pts, fill=(*col, alpha))


def build_foreground(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cx, cy = size / 2.0, size / 2.0

    # Safe radius inside 66% adaptive icon zone with generous breathing room
    outer_r = size * 0.272

    # 1. Drop shadow + subtle warm glow behind the Master Tape reel
    aura = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    adraw = ImageDraw.Draw(aura, "RGBA")
    adraw.ellipse(
        (cx - outer_r * 1.07, cy - outer_r * 1.07, cx + outer_r * 1.07, cy + outer_r * 1.07),
        fill=(0, 0, 0, 190),
    )
    adraw.ellipse(
        (cx - outer_r * 1.02, cy - outer_r * 1.02, cx + outer_r * 1.02, cy + outer_r * 1.02),
        outline=(255, 195, 65, 120),
        width=int(size * 0.018),
    )
    aura = aura.filter(ImageFilter.GaussianBlur(radius=size * 0.018))
    img = Image.alpha_composite(img, aura)

    draw = ImageDraw.Draw(img, "RGBA")

    # 2. Outer metallic chassis disc + chamfer highlight
    draw.ellipse(
        (cx - outer_r, cy - outer_r, cx + outer_r, cy + outer_r),
        fill=(30, 35, 46, 255),
        outline=(95, 106, 128, 255),
        width=int(size * 0.007),
    )

    track_r = outer_r * 0.855
    draw.ellipse(
        (cx - track_r, cy - track_r, cx + track_r, cy + track_r),
        fill=(13, 16, 22, 255),
        outline=(48, 55, 70, 255),
        width=int(size * 0.004),
    )

    # 3. Three symmetrical holographic Master Tape windows strictly BETWEEN the 3 spokes
    # Spokes sit at -90, 30, 150 degrees; windows sit cleanly in (-73..13), (47..133), (167..253)
    ring_r = outer_r * 0.725
    ring_thick = outer_r * 0.165
    draw_arc_band(img, cx, cy, ring_r, ring_thick, -72, 12, (255, 190, 58), (255, 86, 166))
    draw_arc_band(img, cx, cy, ring_r, ring_thick, 48, 132, (0, 229, 255), (170, 255, 85))
    draw_arc_band(img, cx, cy, ring_r, ring_thick, 168, 252, (245, 166, 35), (255, 228, 130))

    # Glossy inner highlights on each tape arc
    draw_arc_band(img, cx, cy, ring_r + ring_thick * 0.28, ring_thick * 0.16, -70, 10, (255, 245, 225), (255, 210, 235), alpha=155)
    draw_arc_band(img, cx, cy, ring_r + ring_thick * 0.28, ring_thick * 0.16, 50, 130, (215, 252, 255), (235, 255, 210), alpha=155)
    draw_arc_band(img, cx, cy, ring_r + ring_thick * 0.28, ring_thick * 0.16, 170, 250, (255, 236, 180), (255, 250, 225), alpha=155)

    # 4. Three armored spokes aligned cleanly between windows (-90, 30, 150)
    draw = ImageDraw.Draw(img, "RGBA")
    hub_r = outer_r * 0.54
    for spoke_deg in (-90, 30, 150):
        rad = math.radians(spoke_deg)
        nx, ny = -math.sin(rad), math.cos(rad)
        half_w = outer_r * 0.068
        r_start = hub_r * 0.72
        r_end = outer_r * 0.96
        pts = [
            (cx + r_start * math.cos(rad) + half_w * nx, cy + r_start * math.sin(rad) + half_w * ny),
            (cx + r_end * math.cos(rad) + half_w * 0.75 * nx, cy + r_end * math.sin(rad) + half_w * 0.75 * ny),
            (cx + r_end * math.cos(rad) - half_w * 0.75 * nx, cy + r_end * math.sin(rad) - half_w * 0.75 * ny),
            (cx + r_start * math.cos(rad) - half_w * nx, cy + r_start * math.sin(rad) - half_w * ny),
        ]
        draw.polygon(pts, fill=(32, 37, 49, 255), outline=(92, 102, 124, 255), width=int(size * 0.003))

        # Gold rivet pin on each spoke outer end
        pin_r = outer_r * 0.032
        px = cx + outer_r * 0.91 * math.cos(rad)
        py = cy + outer_r * 0.91 * math.sin(rad)
        draw.ellipse((px - pin_r, py - pin_r, px + pin_r, py + pin_r), fill=(255, 209, 92, 245))

    # 5. Central Dark Hub + Golden Chamfer Ring
    draw.ellipse(
        (cx - hub_r, cy - hub_r, cx + hub_r, cy + hub_r),
        fill=(22, 26, 35, 255),
        outline=(245, 166, 35, 255),
        width=int(size * 0.011),
    )
    inner_hub_r = hub_r * 0.82
    draw.ellipse(
        (cx - inner_hub_r, cy - inner_hub_r, cx + inner_hub_r, cy + inner_hub_r),
        fill=(13, 15, 21, 255),
        outline=(58, 65, 82, 255),
        width=int(size * 0.004),
    )

    # 6. Hero S-Rank Four-Point Signal Star in center
    star_glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sg_draw = ImageDraw.Draw(star_glow, "RGBA")
    draw_four_point_star(
        sg_draw,
        cx,
        cy,
        outer_r=hub_r * 0.78,
        inner_r=hub_r * 0.23,
        fill=(255, 198, 60, 175),
    )
    star_glow = star_glow.filter(ImageFilter.GaussianBlur(radius=size * 0.014))
    img = Image.alpha_composite(img, star_glow)

    draw = ImageDraw.Draw(img, "RGBA")
    draw_four_point_star(
        draw,
        cx,
        cy,
        outer_r=hub_r * 0.70,
        inner_r=hub_r * 0.21,
        fill=(245, 166, 35, 255),
        outline=(255, 234, 158, 255),
        width=int(size * 0.0045),
    )
    draw_four_point_star(
        draw,
        cx,
        cy,
        outer_r=hub_r * 0.48,
        inner_r=hub_r * 0.13,
        fill=(255, 240, 178, 255),
    )

    return img


def make_rounded_mask(size: int, radius: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    return mask


def make_circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.ellipse((0, 0, size - 1, size - 1), fill=255)
    return mask


def main() -> None:
    bg_hi = build_background(WORK_SIZE)
    fg_hi = build_foreground(WORK_SIZE)

    bg_out = bg_hi.resize((ADAPTIVE_SIZE, ADAPTIVE_SIZE), Image.Resampling.LANCZOS)
    fg_out = fg_hi.resize((ADAPTIVE_SIZE, ADAPTIVE_SIZE), Image.Resampling.LANCZOS)

    nodpi_dir = RES_DIR / "drawable-nodpi"
    nodpi_dir.mkdir(parents=True, exist_ok=True)
    bg_out.save(nodpi_dir / "ic_launcher_bg.png")
    fg_out.save(nodpi_dir / "ic_launcher_fg.png")

    crop_margin = int(WORK_SIZE * (18 / 108))
    comp_hi = Image.alpha_composite(bg_hi, fg_hi).crop(
        (crop_margin, crop_margin, WORK_SIZE - crop_margin, WORK_SIZE - crop_margin)
    )

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, px in densities.items():
        target_dir = RES_DIR / folder
        target_dir.mkdir(parents=True, exist_ok=True)
        resized = comp_hi.resize((px, px), Image.Resampling.LANCZOS)

        sq = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        sq.paste(resized, (0, 0), make_rounded_mask(px, int(px * 0.22)))
        sq.save(target_dir / "ic_launcher.png")

        rnd = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        rnd.paste(resized, (0, 0), make_circle_mask(px))
        rnd.save(target_dir / "ic_launcher_round.png")

    preview = Image.new("RGBA", (620, 320), (24, 27, 34, 255))
    p_sq = comp_hi.resize((240, 240), Image.Resampling.LANCZOS)
    p_rnd = comp_hi.resize((240, 240), Image.Resampling.LANCZOS)
    preview.paste(p_sq, (45, 40), make_rounded_mask(240, 52))
    preview.paste(p_rnd, (335, 40), make_circle_mask(240))
    preview.save(PREVIEW_PATH)
    print(f"Generated polished adaptive icon & mipmaps. Preview: {PREVIEW_PATH}")


if __name__ == "__main__":
    main()
