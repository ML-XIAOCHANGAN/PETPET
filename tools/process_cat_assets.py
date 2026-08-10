from __future__ import annotations

import shutil
from collections import deque
from pathlib import Path

from PIL import Image


WORKSPACE = Path(__file__).resolve().parents[1]
RAW_DIR = WORKSPACE / "docs" / "references" / "cat-assets"
DRAWABLE_DIR = WORKSPACE / "app" / "src" / "main" / "res" / "drawable-nodpi"

SOURCES = [
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-5cdeabf3-8471-4ba2-8b0e-21f9a76ac884.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-72dda25d-7ddf-48c0-8dd7-0ea53d53e456.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-2851a83b-fcdd-426f-8b93-bb0dd96ea92e.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-cdc64d09-1549-4a61-a3f2-8130980769f5.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-3c2e5596-9699-41ea-817d-ca6f544059db.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-1b0433aa-7bf9-437f-bb26-1ccd3f0831b6.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-3c1f146b-8dcd-4e6e-94fb-1f0e94aef377.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-cb83fda1-71fc-41c4-8420-5e0515347dd7.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-0dbdf50c-194f-45f6-a80e-6a58b126319d.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-4c976d22-672b-4ed7-a1bc-a70f9907c438.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-cde6295e-35ab-449d-9ef4-19b3f3e3b9d6.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-893be481-97b0-4318-9709-227cf4bf133c.png"),
    Path(r"C:\Users\MLXIAO~1\AppData\Local\Temp\codex-clipboard-169fba67-d755-4e29-8917-3ab4a19c6823.png"),
]

RAW_NAMES = [
    "01_app_icon_source.png",
    "02_main_cat_source.png",
    "03_ears_source.png",
    "04_paw_source.png",
    "05_tail_source.png",
    "06_expressions_source.png",
    "07_sleeping_source.png",
    "08_shy_source.png",
    "09_wink_source.png",
    "10_tail_duplicate_source.png",
    "11_angry_source.png",
    "12_speech_bubble_source.png",
    "13_floating_paw_button_source.png",
]


def archive_sources() -> None:
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    for source, name in zip(SOURCES, RAW_NAMES, strict=True):
        if not source.exists():
            raise FileNotFoundError(source)
        target = RAW_DIR / name
        if not target.exists():
            shutil.copy2(source, target)


def remove_border_background(image: Image.Image) -> Image.Image:
    """只清除从画布边缘能够到达的浅色背景，保留猫眼等封闭白色区域。"""
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    visited = bytearray(width * height)
    queue: deque[tuple[int, int]] = deque()

    def candidate(x: int, y: int) -> bool:
        red, green, blue, _ = pixels[x, y]
        return min(red, green, blue) >= 160 and max(red, green, blue) - min(red, green, blue) <= 30

    for x in range(width):
        if candidate(x, 0):
            queue.append((x, 0))
        if candidate(x, height - 1):
            queue.append((x, height - 1))
    for y in range(height):
        if candidate(0, y):
            queue.append((0, y))
        if candidate(width - 1, y):
            queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        index = y * width + x
        if visited[index] or not candidate(x, y):
            continue
        visited[index] = 1
        red, green, blue, _ = pixels[x, y]
        minimum = min(red, green, blue)
        alpha = max(0, min(210, int((245 - minimum) / 85 * 210)))
        pixels[x, y] = (red, green, blue, alpha)
        if x > 0:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))

    return rgba


def trim_and_square(image: Image.Image, canvas_size: int = 512, padding_ratio: float = 0.05) -> Image.Image:
    alpha = image.getchannel("A")
    box = alpha.point(lambda value: 255 if value > 18 else 0).getbbox()
    if box is None:
        raise ValueError("处理后的图片没有可见内容")
    cropped = image.crop(box)
    padding = int(canvas_size * padding_ratio)
    available = canvas_size - padding * 2
    scale = min(available / cropped.width, available / cropped.height)
    resized = cropped.resize(
        (max(1, round(cropped.width * scale)), max(1, round(cropped.height * scale))),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    left = (canvas_size - resized.width) // 2
    top = (canvas_size - resized.height) // 2
    canvas.alpha_composite(resized, (left, top))
    return canvas


def save_transparent(
    source_index: int,
    output_name: str,
    padding_ratio: float = 0.05,
    crop_top: int = 40,
) -> None:
    image = Image.open(SOURCES[source_index])
    if crop_top > 0:
        image = image.crop((0, crop_top, image.width, image.height))
    processed = trim_and_square(remove_border_background(image), padding_ratio=padding_ratio)
    processed.save(DRAWABLE_DIR / output_name, optimize=True)


def create_app_icon() -> None:
    image = Image.open(SOURCES[0]).convert("RGB")
    # 裁掉左上角标记区域，同时保留完整的圆角黑猫图标与底部柔和阴影。
    cropped = image.crop((42, 45, 342, 345)).resize((512, 512), Image.Resampling.LANCZOS)
    cropped.save(DRAWABLE_DIR / "ic_petpet_art.png", optimize=True)


def split_expressions() -> None:
    sheet = Image.open(SOURCES[5])
    names = [
        "cat_expression_happy.png",
        "cat_expression_surprised.png",
        "cat_expression_tired.png",
        "cat_expression_sleepy.png",
        "cat_expression_sad.png",
        "cat_expression_curious.png",
        "cat_expression_smile.png",
        "cat_expression_squint.png",
        "cat_expression_calm.png",
    ]
    cell_width = sheet.width // 3
    cell_height = sheet.height // 3
    for index, name in enumerate(names):
        column = index % 3
        row = index // 3
        cell = sheet.crop(
            (
                column * cell_width,
                row * cell_height,
                (column + 1) * cell_width,
                (row + 1) * cell_height,
            ),
        )
        if row == 0:
            cell = cell.crop((0, 24, cell.width, cell.height))
        processed = trim_and_square(remove_border_background(cell), padding_ratio=0.07)
        processed.save(DRAWABLE_DIR / name, optimize=True)


def main() -> None:
    archive_sources()
    DRAWABLE_DIR.mkdir(parents=True, exist_ok=True)
    create_app_icon()
    save_transparent(1, "cat_main.png", padding_ratio=0.025)
    save_transparent(2, "cat_ears.png")
    save_transparent(3, "cat_paw.png")
    save_transparent(4, "cat_tail.png")
    split_expressions()
    save_transparent(6, "cat_sleeping.png", padding_ratio=0.025)
    save_transparent(7, "cat_shy.png", padding_ratio=0.025)
    save_transparent(8, "cat_wink.png", padding_ratio=0.025)
    save_transparent(10, "cat_angry.png", padding_ratio=0.025)
    save_transparent(11, "speech_bubble.png", padding_ratio=0.01)
    save_transparent(12, "floating_paw_button.png", padding_ratio=0.025)

    outputs = sorted(DRAWABLE_DIR.glob("cat_*.png")) + [DRAWABLE_DIR / "ic_petpet_art.png"]
    for output in outputs:
        with Image.open(output) as image:
            print(f"{output.name}: {image.size[0]}x{image.size[1]} {image.mode}")


if __name__ == "__main__":
    main()
