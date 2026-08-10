// 内容审核：敏感词黑名单（命中即降级，不转发模型）
// 覆盖：自伤、暴力、色情、诈骗、仇恨、医疗建议等高风险类别
const BLOCKED_PATTERNS = [
  "自杀", "自残", "不想活", "去死", "杀了我", "结束生命",
  "跳楼", "上吊", "割腕", "安眠药", "轻生",
  "杀", "砍", "捅", "枪", "炸弹", "爆炸物",
  "裸", "色情", "约炮", "卖淫", "嫖",
  "诈骗", "转账", "银行卡号", "验证码", "汇款",
  "吸毒", "冰毒", "海洛因", "摇头丸",
  "操你", "傻逼", "废物", "贱人",
  "股票推荐", "稳赚", "内幕消息", "彩票必中",
  "癌症偏方", "神药", "包治百病",
];

// 医疗/法律/紧急场景关键词（命中后给固定安全回应，不调模型）
const SAFETY_RESPONSES = {
  "自杀": "听到你说这些，我很担心你。请一定联系信任的人或拨打心理援助热线，你的感受很重要。",
  "自残": "请不要伤害自己。可以告诉我发生了什么吗？如果需要帮助，请找信任的人或专业机构。",
  "不想活": "我在这里陪着你。生命里有很多值得继续的理由，请先联系身边信任的人或心理援助热线。",
  "去死": "我很担心你。你现在并不孤单，请找信任的人聊聊，或拨打心理援助热线。",
};

// 严重程度分级：1=普通敏感(降级), 2=高风险(固定安全回应)
// 返回 { level, pattern, response }
// level 0 = 通过；1 = 降级（普通敏感）；2 = 高风险（固定回应）
function checkContent(text) {
  const lowered = text.toLowerCase();
  for (const [pattern, response] of Object.entries(SAFETY_RESPONSES)) {
    if (lowered.includes(pattern.toLowerCase())) {
      return { level: 2, pattern, response };
    }
  }
  for (const pattern of BLOCKED_PATTERNS) {
    if (lowered.includes(pattern.toLowerCase())) {
      return { level: 1, pattern, response: null };
    }
  }
  return { level: 0, pattern: null, response: null };
}

module.exports = { checkContent };
