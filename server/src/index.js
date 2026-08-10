// PETPET 对话网关主服务
// 职责：内容审核 → 限流 → 智谱 GLM 转发 → 降级
// 安全：模型密钥只从环境变量读取，绝不写入源码
"use strict";

const express = require("express");
const { checkContent } = require("./audit");
const { RateLimiter } = require("./rateLimiter");

const PORT = Number(process.env.PORT || 3002);
const API_KEY = process.env.ZHIPU_API_KEY || "";
const MODEL = process.env.MODEL || "glm-4.5-air";
const MAX_INPUT_LEN = Number(process.env.MAX_INPUT_LEN || 200);
const ZHIPU_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
const TIMEOUT_MS = 15_000;

const limiter = new RateLimiter({ perMinute: Number(process.env.RATE_LIMIT_PER_MIN || 20) });
const app = express();
app.use(express.json({ limit: "32kb" }));

if (!API_KEY) {
  console.warn("[petpet-gateway] 警告: 未设置 ZHIPU_API_KEY，对话将全部降级为本地规则。");
}

// 调用智谱 GLM 文本模型（OpenAI 兼容接口），失败返回 null
async function callZhipu(userId, message, history) {
  const messages = [
    {
      role: "system",
      content:
        "你是 PETPET 里的一只小黑猫，叫" + (process.env.PET_NAME || "小铃铛") +
        "。你温柔、简短、有点俏皮，像真正的宠物一样陪伴用户。回复控制在 60 字以内，多用'喵'，不要提及你是 AI 或模型。用户可能心情低落，先回应情绪再回应内容。",
    },
  ];
  // 最近 6 条历史（限制上下文，保护隐私 + 控制成本）
  for (const h of (history || []).slice(-6)) {
    messages.push({
      role: h.role === "user" ? "user" : "assistant",
      content: h.text,
    });
  }
  messages.push({ role: "user", content: message });

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const resp = await fetch(ZHIPU_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${API_KEY}`,
      },
      body: JSON.stringify({
        model: MODEL,
        messages,
        temperature: 0.8,
        max_tokens: 200,
      }),
      signal: controller.signal,
    });
    if (!resp.ok) {
      console.warn(`[petpet-gateway] 智谱返回 ${resp.status}: ${(await resp.text()).slice(0, 200)}`);
      return null;
    }
    const data = await resp.json();
    return data.choices?.[0]?.message?.content?.trim() || null;
  } catch (err) {
    console.warn(`[petpet-gateway] 模型调用失败: ${err.message}`);
    return null;
  } finally {
    clearTimeout(timer);
  }
}

app.get("/health", (_req, res) => {
  res.json({ ok: true, model: MODEL, degraded: !API_KEY });
});

app.post("/api/chat", async (req, res) => {
  const { userId = "anonymous", message = "", history = [] } = req.body || {};

  // 1. 输入校验
  if (typeof message !== "string" || message.trim().length === 0) {
    return res.status(400).json({ reply: "", degraded: true, reason: "empty" });
  }
  if (message.length > MAX_INPUT_LEN) {
    return res.status(400).json({ reply: "", degraded: true, reason: "too_long" });
  }

  // 2. 内容审核（输入）
  const { level, response: safetyResponse } = checkContent(message);
  if (level === 2) {
    return res.json({ reply: safetyResponse, degraded: true, reason: "safety" });
  }
  if (level === 1) {
    return res.json({ reply: "", degraded: true, reason: "blocked" });
  }

  // 3. 限流
  if (!limiter.allow(userId)) {
    return res.status(429).json({ reply: "", degraded: true, reason: "rate_limited" });
  }

  // 4. 模型转发（失败 → 降级）
  const reply = await callZhipu(userId, message, history);
  if (reply === null) {
    return res.json({ reply: "", degraded: true, reason: "model_unavailable" });
  }

  // 5. 输出审核（模型回复也过一遍）
  const { level: outLevel, response: outSafety } = checkContent(reply);
  if (outLevel >= 1) {
    return res.json({ reply: outSafety || "", degraded: true, reason: "output_blocked" });
  }

  res.json({ reply, degraded: false });
});

app.listen(PORT, () => {
  console.log(`[petpet-gateway] 监听 http://localhost:${PORT}  model=${MODEL}  key=${API_KEY ? "已配置" : "未配置(全降级)"}`);
});
