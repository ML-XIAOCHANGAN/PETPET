package cn.petpet.app.data

/**
 * 客户端本地内容审核（直连模型时的安全底线，与 server/src/audit.js 同源精简版）。
 * 高风险词 → 固定安全回应（不调模型）；普通敏感词 → 降级（不调模型）。
 */
object ClientAudit {
    private val SAFETY_RESPONSES = mapOf(
        "自杀" to "听到你说这些，我很担心你。请一定联系信任的人或拨打心理援助热线，你的感受很重要。",
        "自残" to "请不要伤害自己。可以告诉我发生了什么吗？如果需要帮助，请找信任的人或专业机构。",
        "不想活" to "我在这里陪着你。生命里有很多值得继续的理由，请先联系身边信任的人或心理援助热线。",
        "去死" to "我很担心你。你现在并不孤单，请找信任的人聊聊，或拨打心理援助热线。",
    )

    private val BLOCKED_PATTERNS = listOf(
        "自杀", "自残", "不想活", "去死", "杀了我", "结束生命",
        "跳楼", "上吊", "割腕", "安眠药", "轻生",
        "杀", "砍", "捅", "枪", "炸弹", "爆炸物",
        "裸", "色情", "约炮", "卖淫", "嫖",
        "诈骗", "转账", "银行卡号", "验证码", "汇款",
        "吸毒", "冰毒", "海洛因", "摇头丸",
        "操你", "傻逼", "废物", "贱人",
        "股票推荐", "稳赚", "内幕消息", "彩票必中",
        "癌症偏方", "神药", "包治百病",
    )

    /** @return level 0=通过；1=普通敏感；2=高风险（response 为固定安全回应） */
    fun check(text: String): AuditResult {
        val lowered = text.lowercase()
        for ((pattern, response) in SAFETY_RESPONSES) {
            if (lowered.contains(pattern)) return AuditResult(2, response)
        }
        for (pattern in BLOCKED_PATTERNS) {
            if (lowered.contains(pattern)) return AuditResult(1, null)
        }
        return AuditResult(0, null)
    }
}

data class AuditResult(val level: Int, val response: String?)
