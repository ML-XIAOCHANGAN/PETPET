"""黑猫表情包处理：去水印 + 抠透明底 + 覆盖原资源。

新表情在 PET-黑色小猫/，覆盖到 drawable-nodpi/ 下的 cat_*.png。
水印同前：右下角 x 0.81-0.98, y 0.90-0.98 填背景色 → 抠图时透明化。
猫占画面 85-90%（几乎撑满），保留原构图直接处理，不裁剪中心。
"""
from PIL import Image
import os, math

SRC = r'F:\Agent-Program\项目02_(PETPET)\Pet\PET-黑色小猫'
DST = r'F:\Agent-Program\项目02_(PETPET)\app\src\main\res\drawable-nodpi'

# 新表情 → 覆盖的原资源名
MAPPING = {
    '黑色小猫主头像.png': 'cat_main',
    'wink01.png': 'cat_wink',
    '高兴01.png': 'cat_expression_happy',
    '高兴03.png': 'cat_expression_smile',
    '慌张.png': 'cat_expression_curious',
    '犯困01.png': 'cat_sleeping',
    'wink02.png': 'cat_expression_squint',
    '高兴02.png': 'cat_shy',
    # 备用（处理成透明底，不覆盖引用资源，供后续对话情绪接入）
    '伤心01.png': 'cat_expression_sad',
    '无语01.png': 'cat_expression_tired',
    '睡觉01.png': 'cat_expression_sleepy',
    '睡觉02.png': 'cat_expression_calm',
}

WM_X0, WM_Y0, WM_X1, WM_Y1 = 0.81, 0.90, 0.98, 0.98


def sample_bg_color(img):
    w, h = img.size
    pts = [(8, 8), (w // 2, 8), (w - 8, 8), (8, h // 2), (w - 8, h // 2),
           (8, h - 8), (w // 2, h - 8), (w - 8, h - 8)]
    rgb = [img.getpixel(p) for p in pts]
    return tuple(sorted(c[i] for c in rgb)[len(rgb) // 2] for i in range(3))


def distance(c1, c2):
    return math.sqrt(sum((a - b) ** 2 for a, b in zip(c1, c2)))


def remove_watermark(img, bg):
    w, h = img.size
    px = img.load()
    x0, y0 = int(w * WM_X0), int(h * WM_Y0)
    x1, y1 = int(w * WM_X1), int(h * WM_Y1)
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = bg + (255,)
    return img


def remove_bg(img, bg, inner, outer):
    w, h = img.size
    rgba = img.convert('RGBA')
    pixels = rgba.load()
    for y in range(h):
        for x in range(w):
            px = pixels[x, y][:3]
            d = distance(px, bg)
            if d < inner:
                pixels[x, y] = (px[0], px[1], px[2], 0)
            elif d < outer:
                a = int(255 * (1 - (d - inner) / (outer - inner)))
                pixels[x, y] = (px[0], px[1], px[2], a)
    return rgba


def main():
    for src_name, res_name in MAPPING.items():
        src = os.path.join(SRC, src_name)
        if not os.path.exists(src):
            print(f'❌ {src_name}: 不存在')
            continue
        img = Image.open(src).convert('RGBA')
        bg = sample_bg_color(img)
        remove_watermark(img, bg)
        result = remove_bg(img, bg, 30, 75)
        if result.size[0] > 512:
            result = result.resize((512, 512), Image.LANCZOS)
        out = os.path.join(DST, res_name + '.png')
        result.save(out, 'PNG', optimize=True)
        print(f'✅ {src_name} → {res_name}.png (透明底+去水印)')
    print('\n完成')


if __name__ == '__main__':
    main()
