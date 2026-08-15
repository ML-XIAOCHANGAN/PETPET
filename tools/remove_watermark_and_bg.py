"""PETPET 宠物头像去水印 + 抠透明底（一体脚本）。

水印：豆包生成图右下角"豆包AI生成"浅灰/深灰小字，位于 x≈0.78-1.0, y≈0.85-1.0。
方法：
1. 在原图右下角贴边区，找出与背景色差异明显的小文字像素（浅灰/深灰）
2. 用背景色（从边缘采样）填充水印像素，羽化过渡
3. 再做颜色距离抠图 → 透明底

用法：python remove_watermark_and_bg.py
"""
from PIL import Image, ImageFilter
import os, math

SRC = r'F:\Agent-Program\项目02_(PETPET)\Pet'
DST = r'F:\Agent-Program\项目02_(PETPET)\app\src\main\res\drawable-nodpi'

NAMES = {
    'PET-比熊小狗': 'pet_bichon', 'PET-边牧小狗': 'pet_border_collie',
    'PET-伯恩山小狗': 'pet_berner', 'PET-博美小狗': 'pet_pomeranian',
    'PET-布偶小猫': 'pet_ragdoll', 'PET-纯白英短小猫': 'pet_white_british',
    'PET-德牧小狗': 'pet_german_shepherd', 'PET-黑色小猫': 'pet_black_cat',
    'PET-灰色小猫': 'pet_gray_cat', 'PET-金渐层长毛小猫': 'pet_golden_shaded',
    'PET-缅因小猫': 'pet_maine_coon', 'PET-萨摩耶小狗': 'pet_samoyed',
    'PET-三花小猫': 'pet_calico', 'PET-苏格兰折耳小猫': 'pet_scottish_fold',
    'PET-暹罗小猫': 'pet_siamese', 'PET-小柴犬': 'pet_shiba',
    'PET-小金毛': 'pet_golden_retriever', 'PET-小橘猫': 'pet_orange_cat',
}

LIGHT_FUR = {'pet_samoyed', 'pet_bichon', 'pet_golden_retriever', 'pet_white_british',
             'pet_golden_shaded', 'pet_pomeranian', 'pet_ragdoll', 'pet_calico'}

# 水印区域（右下角贴边）——豆包生成图水印精确定位：x 0.81-0.98, y 0.90-0.98
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
    """水印贴右下角且该区域是纯背景：直接整格填背景色（安全，不伤宠物）。"""
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
    for folder, res_name in NAMES.items():
        folder_path = os.path.join(SRC, folder)
        main_file = None
        for f in os.listdir(folder_path):
            if '主头像' in f:
                main_file = os.path.join(folder_path, f)
                break
        if not main_file:
            print(f'❌ {folder}: 未找到主头像')
            continue

        img = Image.open(main_file).convert('RGBA')
        bg = sample_bg_color(img)
        remove_watermark(img, bg)

        # 抠图
        if res_name in LIGHT_FUR:
            inner, outer = 18, 55
        else:
            inner, outer = 30, 75
        result = remove_bg(img, bg, inner, outer)

        if result.size[0] > 512:
            result = result.resize((512, 512), Image.LANCZOS)
        result.save(os.path.join(DST, res_name + '.png'), 'PNG', optimize=True)
        print(f'✅ {folder} → {res_name}.png (水印已去除, 背景 {bg})')
    print('\n完成')


if __name__ == '__main__':
    main()
