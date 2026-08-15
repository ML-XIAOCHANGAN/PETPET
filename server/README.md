# PETPET 对话网关（受控后端）

按 `docs/10_人工智能对话接入与安全建议.md` 实现：安卓客户端不保存模型密钥，
所有对话请求经此网关转发到智谱开放平台，统一执行内容审核、限流、超时与降级。

**对话模型 = 智谱官方免费模型 `glm-4-flash-250414`（2026-08 确认免费，零成本）。**

## 架构

```text
安卓客户端 → PETPET 网关(Node/Express) → 内容审核+限流 → 智谱 GLM-4-Flash（免费）
                 ↓ 失败 / 超时 / 审核不通过
              返回 { degraded: true } → 客户端回退本地规则回复
```

## 快速开始

```powershell
cd server
npm install
# 1) 复制模板并填入 Key（推荐，Key 只存本机，绝不提交 git）
copy .env.example .env
#    打开 .env，把 ZHIPU_API_KEY 换成你的 Key
# 2) 启动（自动读取 .env）
npm start
```

### 免费 Key 获取（2 分钟）

1. 打开 https://open.bigmodel.cn ，注册 / 登录智谱开放平台
2. 完成实名认证（手机号即可）
3. 控制台 →「API Keys」→ 创建新 Key，复制到 `.env` 的 `ZHIPU_API_KEY=`
4. 对话模型用免费的 `glm-4-flash-250414`，**不需要充值**

## 配置（环境变量 / .env）

| 变量 | 默认 | 说明 |
|---|---|---|
| `ZHIPU_API_KEY` | 无(必填) | 智谱开放平台密钥（免费创建） |
| `PORT` | 3002 | 监听端口 |
| `RATE_LIMIT_PER_MIN` | 20 | 每用户每分钟最大请求数 |
| `MAX_INPUT_LEN` | 200 | 单条消息最大长度 |
| `MODEL` | `glm-4-flash-250414` | 智谱文本模型（免费） |

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
