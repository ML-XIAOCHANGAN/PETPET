"""直接替换 18 张主头像：源图已处理（去水印+透明底），只缩到 512px 保持体积。
不做任何去水印/抠图处理。"""
from PIL import Image
import os

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
    img = Image.open(main_file)
    has_alpha = img.mode in ('RGBA', 'LA') or (img.mode == 'P' and 'transparency' in img.info)
    if img.size[0] > 512:
        img = img.resize((512, 512), Image.LANCZOS)
    # 保持原模式（含透明底）；P 模式转 RGBA 保留 alpha
    if img.mode == 'P':
        img = img.convert('RGBA')
    img.save(os.path.join(DST, res_name + '.png'), 'PNG', optimize=True)
    print(f'✅ {folder} → {res_name}.png ({img.size[0]}px, 模式{img.mode}, 原图{has_alpha and "带透明" or "RGB"})')
print('\n完成')
