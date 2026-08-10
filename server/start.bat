@echo off
rem PETPET 网关一键启动脚本（PowerShell 双击或命令行运行）
rem 用法: 先 set ZHIPU_API_KEY=你的Key，再运行本脚本；或直接编辑下面一行
setlocal
if "%ZHIPU_API_KEY%"=="" (
  echo [!] 未设置 ZHIPU_API_KEY，对话将全部降级为本地规则。
  echo     建议: set ZHIPU_API_KEY=你的智谱Key 后再启动
)
cd /d "%~dp0"
node src/index.js
endlocal
