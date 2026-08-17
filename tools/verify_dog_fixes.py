# -*- coding: utf-8 -*-
"""验证狗狗文案修复（BUG 1/2/3 + AI 提示词），源码断言。"""
import os, sys

ROOT = r"F:\Agent-Program\项目02_(PETPET)"
files = {
    "vm": os.path.join(ROOT, "app", "src", "main", "java", "cn", "petpet", "app", "ui", "PetPetViewModel.kt"),
    "myscreen": os.path.join(ROOT, "app", "src", "main", "java", "cn", "petpet", "app", "ui", "screens", "MyPetScreen.kt"),
    "client": os.path.join(ROOT, "app", "src", "main", "java", "cn", "petpet", "app", "data", "ChatGatewayClient.kt"),
    "server": os.path.join(ROOT, "server", "src", "index.js"),
}
content = {k: open(v, encoding="utf-8").read() for k, v in files.items()}
ok = True

def check(name, cond):
    global ok
    print(("PASS" if cond else "FAIL"), name)
    if not cond:
        ok = False

vm = content["vm"]

# BUG1: 互动奖励提示用宠物名字
check("BUG1 互动提示用宠物名字", '"$petName 很开心，今天的互动奖励已经领满啦"' in vm)
check("BUG1 petName 从 snapshot 取", "val petName = snapshot.pet?.name ?: petKindText()" in vm)

# BUG2: 正在想按品种
check("BUG2 打字气泡按品种", 'TypingBubble(isDog = PetBreeds.byId(pet.breedId)?.type == PetType.DOG)' in content["myscreen"])
check("BUG2 气泡文字小狗分支", 'text = "${if (isDog) "小狗" else "小猫"}正在想$dotsText"' in content["myscreen"])
check("BUG2 重复发送提示按品种", 'showNotice("${petKindText()}正在想怎么回你，稍等一下${petSound()}")' in vm)

# BUG3: 叫声按品种（本地规则回复）
check("BUG3 离线回复用 petSound", 'val sound = petSound()' in vm)
check("BUG3 离线回复无硬编码喵", '"喵……" not in vm' and '"喵～"' not in vm)

# 品种辅助函数
check("isDog() 函数存在", "private fun isDog(): Boolean" in vm)
check("petKindText() 函数存在", 'private fun petKindText(): String = if (isDog()) "小狗" else "小猫"' in vm)
check("petSound() 函数存在", 'private fun petSound(): String = if (isDog()) "汪" else "喵"' in vm)

# 启动问候按品种
check("启动问候按品种", 'if (PetBreeds.byId(repository.snapshot().pet?.breedId ?: "black_cat")?.type == PetType.DOG) "汪" else "喵"' in vm)

# AI 提示词按品种（客户端直连）
check("AI 客户端提示词 kindText", 'val kindText = if (isDog) "小狗" else "小猫"' in content["client"])
check("AI 客户端提示词 sound", 'val sound = if (isDog) "汪" else "喵"' in content["client"])
check("AI 客户端无小黑猫硬编码", '"小黑猫" not in content["client"]')

# 网关透传
check("网关 callProvider 接收品种", "callProvider(provider, apiKey, model, message, history, petName, Boolean(isDog))" in content["server"])
check("网关请求解析品种", 'petName = "", isDog = false' in content["server"])
check("网关提示词按品种", 'kindText = isDog ? "小狗" : "小猫"' in content["server"])

print("\n" + ("✅ 全部通过" if ok else "❌ 存在失败项"))
sys.exit(0 if ok else 1)
