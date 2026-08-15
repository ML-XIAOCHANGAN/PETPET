"""PETPET 宠物头像抠透明底脚本。

方法：颜色距离法（色度键）
- 从四角采样背景主色
- 与背景色欧氏距离 < 内阈值 → 全透明
- 距离在 [内阈值, 外阈值] → 按距离线性羽化（抗锯齿）
- 白色/浅色毛发的宠物（萨摩耶/比熊/小金毛/纯白英短等）容差调低，避免误抠毛发

用法：python remove_bg.py
"""
from PIL import Image
import os, math

SRC = r'F:\Agent-Program\项目02_(PETPET)\Pet'
DST = r'F:\Agent-Program\项目02_(PETPET)\app\src\main\res\drawable-nodpi'

# 品种名 → 资源名映射（与复制脚本一致）
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

# 白色/浅色毛发品种：需要低容差保护毛发
LIGHT_FUR = {'pet_samoyed', 'pet_bichon', 'pet_golden_retriever', 'pet_white_british',
             'pet_golden_shaded', 'pet_pomeranian', 'pet_ragdoll', 'pet_calico'}


def sample_bg_color(img):
    """从四角+四边中点采样背景色（中位数，抗噪）。"""
    w, h = img.size
    pts = [
        (8, 8), (w // 2, 8), (w - 8, 8),
        (8, h // 2), (w - 8, h // 2),
        (8, h - 8), (w // 2, h - 8), (w - 8, h - 8),
    ]
    rgb = [img.getpixel(p) for p in pts]
    return tuple(sorted(c[i] for c in rgb)[len(rgb) // 2] for i in range(3))


def distance(c1, c2):
    return math.sqrt(sum((a - b) ** 2 for a, b in zip(c1, c2)))


def remove_bg(img, bg, inner_thresh, outer_thresh):
    """返回 RGBA 图：距离 bg < inner 全透明，inner~outer 线性羽化。"""
    w, h = img.size
    rgba = img.convert('RGBA')
    pixels = rgba.load()
    for y in range(h):
        for x in range(w):
            px = pixels[x, y][:3]
            d = distance(px, bg)
            if d < inner_thresh:
                pixels[x, y] = (px[0], px[1], px[2], 0)
            elif d < outer_thresh:
                alpha = int(255 * (1 - (d - inner_thresh) / (outer_thresh - inner_thresh)))
                pixels[x, y] = (px[0], px[1], px[2], alpha)
    return rgba


def main():
    total = 0
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

        img = Image.open(main_file).convert('RGB')
        bg = sample_bg_color(img)

        # 浅色毛发品种用低容差（保护白色毛发），其余正常容差
        if res_name in LIGHT_FUR:
            inner, outer = 18, 55
        else:
            inner, outer = 30, 75

        result = remove_bg(img, bg, inner, outer)

        # 缩到 512 并保存
        if result.size[0] > 512:
            result = result.resize((512, 512), Image.LANCZOS)
        out = os.path.join(DST, res_name + '.png')
        result.save(out, 'PNG', optimize=True)
        print(f'✅ {folder} → {res_name}.png (背景 {bg}, 容差 {inner}/{outer})')
        total += 1

    print(f'\n完成：{total} 张已抠透明底')


if __name__ == '__main__':
    main()
