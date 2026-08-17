# -*- coding: utf-8 -*-
"""把 18 品种的表情图复制到 drawable-nodpi，统一命名为 expr_<breedId>_<emotion>.png。
表情语义映射：wink→wink, 高兴→happy, 开心→joy, 惊讶→surprised, 震惊→shocked,
慌张→panic, 伤心→sad, 睡觉→sleeping, 思考→thinking, 无语→speechless
"""
import os
import shutil

ROOT = r"F:\Agent-Program\项目02_(PETPET)\Pet"
DEST = r"F:\Agent-Program\项目02_(PETPET)\app\src\main\res\drawable-nodpi"

# 品种目录名 -> breedId
DIR_TO_ID = {
    "PET-黑色小猫": "black_cat",
    "PET-灰色小猫": "gray_cat",
    "PET-小橘猫": "orange_cat",
    "PET-三花小猫": "calico",
    "PET-布偶小猫": "ragdoll",
    "PET-纯白英短小猫": "white_british",
    "PET-金渐层长毛小猫": "golden_shaded",
    "PET-缅因小猫": "maine_coon",
    "PET-苏格兰折耳小猫": "scottish_fold",
    "PET-暹罗小猫": "siamese",
    "PET-小柴犬": "shiba",
    "PET-小金毛": "golden_retriever",
    "PET-萨摩耶小狗": "samoyed",
    "PET-比熊小狗": "bichon",
    "PET-博美小狗": "pomeranian",
    "PET-边牧小狗": "border_collie",
    "PET-德牧小狗": "german_shepherd",
    "PET-伯恩山小狗": "berner",
}

# 中文表情名 -> 英文语义（按出现顺序追加数字后缀去重）
EMO_MAP = {
    "wink": "wink",
    "高兴": "happy",
    "开心": "joy",
    "惊讶": "surprised",
    "震惊": "shocked",
    "慌张": "panic",
    "伤心": "sad",
    "睡觉": "sleeping",
    "思考": "thinking",
    "无语": "speechless",
}

SKIP_SUBSTR = ("主头像", "生成类似宠物头像")

copied = []
for dirname, breed_id in DIR_TO_ID.items():
    srcdir = os.path.join(ROOT, dirname)
    if not os.path.isdir(srcdir):
        print(f"!! 目录不存在: {dirname}")
        continue
    seen = {}
    for fname in sorted(os.listdir(srcdir)):
        if not fname.lower().endswith(".png"):
            continue
        if any(s in fname for s in SKIP_SUBSTR):
            continue
        base = os.path.splitext(fname)[0]
        # 去掉数字后缀（高兴01.png → 高兴）
        emo_key = base.rstrip("0123456789").rstrip(")")
        emo = EMO_MAP.get(emo_key)
        if emo is None:
            # 未识别：打印但跳过
            print(f"?? 未识别表情: {dirname}/{fname}")
            continue
        count = seen.get(emo, 0) + 1
        seen[emo] = count
        suffix = "" if count == 1 else str(count)
        dest_name = f"expr_{breed_id}_{emo}{suffix}.png"
        shutil.copy2(os.path.join(srcdir, fname), os.path.join(DEST, dest_name))
        copied.append((breed_id, dest_name, fname))

print(f"\n共复制 {len(copied)} 个表情文件")
by_breed = {}
for bid, dest, src in copied:
    by_breed.setdefault(bid, []).append(dest)
for bid in sorted(by_breed):
    print(f"{bid}: {len(by_breed[bid])} 个 -> {', '.join(sorted(by_breed[bid]))}")
