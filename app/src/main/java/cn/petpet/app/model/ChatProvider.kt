package cn.petpet.app.model

/**
 * 可选的对话模型 Provider（与 server/src/index.js 的 PROVIDERS 表 id 一致）。
 * 全部为 OpenAI 兼容接口，网关统一转发；apiKey 由用户在设置页填写，
 * 为空时网关回退 .env 中的 ZHIPU_API_KEY（仅 zhipu 有效）。
 */
enum class ChatProvider(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val baseUrl: String,
    val hint: String,
) {
    OPENAI(
        "openai", "ChatGPT", "gpt-4o-mini",
        "https://api.openai.com/v1/chat/completions",
        "OpenAI 平台 API Key",
    ),
    DEEPSEEK(
        "deepseek", "DeepSeek", "deepseek-chat",
        "https://api.deepseek.com/v1/chat/completions",
        "DeepSeek 开放平台 API Key",
    ),
    ZHIPU(
        "zhipu", "GLM", "glm-4-flash-250414",
        "https://open.bigmodel.cn/api/paas/v4/chat/completions",
        "智谱开放平台 API Key（免费）",
    ),
    KIMI(
        "kimi", "Kimi", "moonshot-v1-8k",
        "https://api.moonshot.cn/v1/chat/completions",
        "Moonshot 开放平台 API Key",
    ),
    ;

    companion object {
        fun byId(id: String?): ChatProvider =
            entries.firstOrNull { it.id == id } ?: ZHIPU
    }
}
