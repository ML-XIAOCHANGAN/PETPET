package cn.petpet.app.data

import cn.petpet.app.model.ChatProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * PETPET 对话网关客户端。
 *
 * 按 docs/10_人工智能对话接入与安全建议.md 实现：
 * - 客户端只发送匿名 userId + 会话内容，不保存模型密钥；
 * - 网关负责内容审核、限流、转发模型与降级；
 * - 网络失败 / 超时 / 网关返回 degraded 时，调用方回退本地规则回复。
 */
class ChatGatewayClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val timeoutMs: Int = 8_000,
) {
    data class GatewayResponse(
        val reply: String,
        val degraded: Boolean,
        val reason: String? = null,
    )

    /**
     * 直连模型 API（OpenAI 兼容），不经过网关。
     * 用户设置页填写了 API Key 时走此路径——手机只要有网即可用，不依赖电脑网关。
     * 输入输出都过本地安全审核（ClientAudit）。
     * @return null 表示网络/鉴权/解析失败，调用方回退本地规则。
     */
    suspend fun directChat(
        provider: ChatProvider,
        apiKey: String,
        message: String,
        history: List<Pair<String, String>>,
    ): GatewayResponse? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // 输入审核：高风险 → 固定安全回应；普通敏感 → 降级
                val inputAudit = ClientAudit.check(message)
                if (inputAudit.level == 2) {
                    return@withContext GatewayResponse(
                        reply = inputAudit.response ?: "",
                        degraded = true,
                        reason = "safety",
                    )
                }
                if (inputAudit.level == 1) {
                    return@withContext GatewayResponse(reply = "", degraded = true, reason = "blocked")
                }

                val messages = JSONArray()
                messages.put(
                    JSONObject().apply {
                        put("role", "system")
                        put(
                            "content",
                            "你是 PETPET 里的一只小黑猫。你温柔、简短、有点俏皮，像真正的宠物一样陪伴用户。" +
                                "回复控制在 60 字以内，多用'喵'，不要提及你是 AI 或模型。用户可能心情低落，先回应情绪再回应内容。",
                        )
                    },
                )
                history.takeLast(6).forEach { (role, text) ->
                    messages.put(
                        JSONObject().apply {
                            put("role", if (role == "user") "user" else "assistant")
                            put("content", text)
                        },
                    )
                }
                messages.put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", message)
                    },
                )

                val payload = JSONObject().apply {
                    put("model", provider.defaultModel)
                    put("messages", messages)
                    put("temperature", 0.8)
                    put("max_tokens", 200)
                }

                connection = URL(provider.baseUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")

                connection.outputStream.use { stream ->
                    stream.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val body = when {
                    code in 200..299 -> connection.inputStream
                    else -> connection.errorStream
                }?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText) ?: ""

                if (code !in 200..299) {
                    android.util.Log.w(
                        "PETPET-GW",
                        "直连 ${provider.displayName} 返回 $code: ${body.take(200)}",
                    )
                    return@withContext null
                }

                val json = JSONObject(body)
                val reply = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content", "")
                    ?.trim()
                    ?: ""

                // 输出审核：模型回复也过一遍敏感词
                val outAudit = ClientAudit.check(reply)
                if (outAudit.level >= 1) {
                    return@withContext GatewayResponse(reply = "", degraded = true, reason = "output_blocked")
                }
                GatewayResponse(reply = reply, degraded = reply.isEmpty(), reason = null)
            } catch (exception: Exception) {
                android.util.Log.w("PETPET-GW", "直连失败: ${exception.message}", exception)
                null
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * 发送一条消息到网关。
     * @param provider 模型 Provider id（openai/deepseek/zhipu/kimi），见 ChatProvider
     * @param apiKey 用户在该平台填写的 API Key；为空时网关回退 .env 的智谱 Key（仅 zhipu）
     * @return null 表示网络层失败（超时/断网/非预期状态码），调用方应回退本地规则。
     */
    suspend fun sendMessage(
        userId: String,
        message: String,
        history: List<Pair<String, String>>,
        provider: String = "zhipu",
        apiKey: String = "",
    ): GatewayResponse? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val payload = JSONObject().apply {
                    put("userId", userId)
                    put("message", message)
                    put("provider", provider)
                    put("apiKey", apiKey)
                    put("history", JSONArray().apply {
                        history.takeLast(6).forEach { (role, text) ->
                            put(JSONObject().apply {
                                put("role", role)
                                put("text", text)
                            })
                        }
                    })
                }

                connection = URL("$baseUrl/api/chat").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                connection.outputStream.use { stream ->
                    stream.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val body = when {
                    code in 200..299 -> connection.inputStream
                    else -> connection.errorStream
                }?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText) ?: ""

                if (code !in 200..299) {
                    android.util.Log.w("PETPET-GW", "网关返回 $code: ${body.take(200)}")
                    return@withContext null
                }

                val json = JSONObject(body)
                GatewayResponse(
                    reply = json.optString("reply", ""),
                    degraded = json.optBoolean("degraded", true),
                    reason = json.optString("reason", "").takeIf { it.isNotEmpty() },
                )
            } catch (exception: Exception) {
                // 超时、断网、解析失败等一律视为网关不可用 → 降级
                android.util.Log.w("PETPET-GW", "网关请求失败: ${exception.message}", exception)
                null
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * 拉取社区动态（在线）。失败返回 null，调用方回退本地。
     */
    suspend fun fetchCommunityPosts(): List<RemotePost>? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL("$baseUrl/api/community/posts").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                if (connection.responseCode !in 200..299) return@withContext null
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(body)
                val array = json.optJSONArray("posts") ?: return@withContext emptyList()
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        add(
                            RemotePost(
                                id = item.optString("id", ""),
                                author = item.optString("author", "小猫朋友"),
                                text = item.optString("text", ""),
                                time = item.optString("time", ""),
                                likeCount = item.optInt("likeCount", 0),
                            ),
                        )
                    }
                }
            } catch (exception: Exception) {
                null
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * 同步发布动态到线上。成功返回 true；失败返回 false（本地已保存，不阻塞）。
     */
    suspend fun publishCommunityPost(authorId: String, author: String, text: String): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val payload = JSONObject().apply {
                    put("authorId", authorId)
                    put("author", author)
                    put("text", text)
                }
                connection = URL("$baseUrl/api/community/posts").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { stream ->
                    stream.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                connection.responseCode in 200..299
            } catch (exception: Exception) {
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    data class RemotePost(
        val id: String,
        val author: String,
        val text: String,
        val time: String,
        val likeCount: Int,
    )

    companion object {
        // 本地开发默认地址；真机调试需改为电脑局域网 IP（如 http://192.168.x.x:3002）
        const val DEFAULT_BASE_URL = "http://10.0.2.2:3002"
    }
}
