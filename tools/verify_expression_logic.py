# -*- coding: utf-8 -*-
"""验证 Petpet 表情情绪联动逻辑（源码断言，无需运行 App）。
检查：1) analyzeReplyEmotion 关键词规则存在且完整；2) 情绪→品种资源可解析。
"""
import re
import os
import sys

ROOT = r"F:\Agent-Program\项目02_(PETPET)\app\src\main"

vm = open(os.path.join(ROOT, "java", "cn", "petpet", "app", "ui", "PetPetViewModel.kt"), encoding="utf-8").read()
lib = open(os.path.join(ROOT, "java", "cn", "petpet", "app", "ui", "components", "PetExpressionLibrary.kt"), encoding="utf-8").read()
avatar = open(os.path.join(ROOT, "java", "cn", "petpet", "app", "ui", "components", "InteractiveBlackCatAvatar.kt"), encoding="utf-8").read()

ok = True

def check(name, cond):
    global ok
    print(("PASS" if cond else "FAIL"), name)
    if not cond:
        ok = False

# 1. analyzeReplyEmotion 存在
check("analyzeReplyEmotion 函数存在", "private fun analyzeReplyEmotion" in vm)
check("happy 关键词", 'text.contains("开心")' in vm and 'text.contains("哈哈")' in vm and 'text.contains("加油")' in vm)
check("surprised 关键词", 'text.contains("哇")' in vm and 'text.contains("没想到")' in vm)
check("sad 关键词", 'text.contains("难过")' in vm and 'text.contains("抱抱")' in vm and 'text.contains("别担心")' in vm)

# 2. petExpression 状态写入与清除
check("回复后写 petExpression", "petExpression = analyzeReplyEmotion(replyText)" in vm)
check("2.8 秒后清除", "delay(2800)" in vm and "petExpression = null" in vm)

# 3. 组件三态优先级
check("Avatar 有 expression 参数", "expression: String? = null" in avatar)
check("思考 > 情绪 > 主头像 优先级", "thinking && thinkingRes != null -> currentImage = thinkingRes" in avatar)
check("情绪分支", "PetExpressionLibrary.expressionRes(breedId, expression)" in avatar)

# 4. 情绪→品种资源可解析（抽查所有情绪名）
expr_map = dict(re.findall(r'"(\w+)" to R\.drawable\.(expr_\w+)', lib))
print(f"\n表情映射条目: {len(expr_map)}")
nodpi = os.path.join(ROOT, "res", "drawable-nodpi")
existing = {f[:-4] for f in os.listdir(nodpi) if f.endswith(".png")}
missing = [res for res in expr_map.values() if res not in existing]
check(f"全部 {len(expr_map)} 个资源文件存在", not missing)
if missing:
    print("  缺失:", missing[:10])

# 5. 18 品种都有表情
breeds = set(re.findall(r'"(\w+)" to mapOf', lib))
check(f"品种数=18", len(breeds) == 18)

# 6. 情绪名合法性（表情后缀）
valid_emotions = {"wink", "happy", "joy", "surprised", "shocked", "panic", "sad", "sleeping", "speechless", "thinking"}
bad = [e for e in expr_map.keys() if e not in valid_emotions]
check("情绪名均在合法集合内", not bad)
if bad:
    print("  非法情绪:", bad)

print("\n" + ("✅ 全部通过" if ok else "❌ 存在失败项"))
sys.exit(0 if ok else 1)
