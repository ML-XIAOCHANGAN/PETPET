# PETPET 对话网关（受控后端）

按 `docs/10_人工智能对话接入与安全建议.md` 实现：安卓客户端不保存模型密钥，
所有对话请求经此网关转发到智谱开放平台，统一执行内容审核、限流、超时与降级。

## 架构

```text
安卓客户端 → PETPET 网关(Node/Express) → 内容审核+限流 → 智谱 GLM 文本模型
                 ↓ 失败 / 超时 / 审核不通过
              返回 { degraded: true } → 客户端回退本地规则回复
```

## 快速开始

```powershell
cd server
npm install
# 配置密钥（只存本机，绝不提交 git）
$env:ZHIPU_API_KEY = "你的智谱Key"
npm start
```

## 配置（环境变量）

| 变量 | 默认 | 说明 |
|---|---|---|
| `ZHIPU_API_KEY` | 无(必填) | 智谱开放平台密钥 |
| `PORT` | 3002 | 监听端口 |
| `RATE_LIMIT_PER_MIN` | 20 | 每用户每分钟最大请求数 |
| `MAX_INPUT_LEN` | 200 | 单条消息最大长度 |
| `MODEL` | `glm-4.5-air` | 智谱文本模型 |

## API

### POST /api/chat

请求体:
```json
{
  "userId": "匿名稳定标识",
  "message": "今天有点累",
  "history": [{"role": "user", "text": "早上好"}, {"role": "pet", "text": "早安！"}]
}
```

成功响应:
```json
{ "reply": "喵……我会安静地陪着你。", "degraded": false }
```

降级响应（模型失败/超时/限流/审核命中）:
```json
{ "reply": "", "degraded": true, "reason": "rate_limited" }
```

## 安全要点

- 密钥只从环境变量读取，绝不写入源码或提交 git（`.env` 已忽略）
- 输入输出都过敏感词审核（黑名单 + 长度限制）
- 每用户内存限流（`userId → 时间戳桶`）
- 超时 15s，模型失败自动重试 1 次
- 客户端只发 `userId`（匿名标识）+ 会话内容，不发手机号/设备硬件标识
