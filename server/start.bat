@echo off
rem PETPET 网关一键启动脚本（双击或命令行运行）
rem 首次使用：复制 .env.example 为 .env，填入智谱免费 Key（获取：open.bigmodel.cn 实名后免费创建）
setlocal
if exist "%~dp0.env" (
  echo [i] 已读取 .env 配置（免费模型 glm-4.7-flash）
) else (
  echo [!] 未找到 .env：请复制 .env.example 为 .env 并填入 ZHIPU_API_KEY
  echo     否则对话将全部降级为本地规则。
)
cd /d "%~dp0"
node src/index.js
endlocal
