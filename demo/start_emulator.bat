@echo off
setlocal enabledelayedexpansion
chcp 936 >nul
title PETPET 模拟器快速启动

REM ============================================================
REM  PETPET 模拟器快速启动器
REM  双击运行即可：启动模拟器 -> 等待系统就绪 -> 可选安装 APK
REM  如需修改，只需改下面"配置区"的路径。
REM ============================================================

REM ====== 配置区（按需修改） ======
set SDK_DIR=F:\Agent-Program\PETPET_SDK
set AVD_HOME=F:\Agent-Program\PETPET_AVD
set AVD_NAME=PETPET_API35
set APK_PATH=F:\Agent-Program\项目02_(PETPET)\app\build\outputs\apk\debug\app-debug.apk
REM ==================================

set ADB=%SDK_DIR%\platform-tools\adb.exe

echo.
echo  ============================================
echo     PETPET 模拟器快速启动器
echo  ============================================
echo.

if not exist "%ADB%" (
    echo  [错误] 找不到 adb：%ADB%
    echo  请检查 SDK_DIR 配置是否正确。
    goto :end
)

REM ---- 检查模拟器是否已在运行 ----
"%ADB%" devices 2>nul | findstr /r "emulator-.*device" >nul
if %errorlevel%==0 (
    echo  [提示] 模拟器已在运行，跳过启动步骤。
    goto :wait_boot
)

REM ---- 启动模拟器 ----
echo  [1/3] 正在启动模拟器 %AVD_NAME% ...
if not exist "%SDK_DIR%\emulator\emulator.exe" (
    echo  [错误] 找不到模拟器程序：%SDK_DIR%\emulator\emulator.exe
    goto :end
)
set ANDROID_AVD_HOME=%AVD_HOME%
start "" "%SDK_DIR%\emulator\emulator.exe" -avd %AVD_NAME% -no-snapshot-load -no-boot-anim -gpu auto -dns-server 223.5.5.5

:wait_boot
echo  [2/3] 等待系统启动完成（首次约 1~2 分钟）...
set /a BOOT_COUNT=0

:loop
"%ADB%" -e shell getprop sys.boot_completed 2>nul | findstr /r "^1" >nul
if %errorlevel%==0 goto :booted
set /a BOOT_COUNT+=1
if !BOOT_COUNT! geq 36 (
    echo  [错误] 等待超时（约 3 分钟）。请手动检查模拟器窗口。
    goto :end
)
ping 127.0.0.1 -n 6 >nul
echo      正在启动中，请稍候...（%BOOT_COUNT%0秒）
goto :loop

:booted
echo  [3/3] 模拟器启动完成！
echo.
"%ADB%" devices
echo.

REM ---- 可选：安装 APK（goto 风格，避免括号块内路径含 () 的解析问题） ----
if not exist "%APK_PATH%" goto :no_apk
set /p DO_INSTALL="  是否安装最新 debug APK？[Y/N]: "
if /i not "!DO_INSTALL!"=="Y" goto :install_done
echo.
echo  正在安装 %APK_PATH% ...
"%ADB%" -e install -r "%APK_PATH%"
if !errorlevel!==0 goto :install_fail
echo  [完成] APK 安装成功！
goto :install_done
:install_fail
echo  [提示] 安装失败。
echo  若提示"签名不匹配"，说明设备上装的是正式版，请先卸载：
echo      adb uninstall cn.petpet.app
goto :install_done
:no_apk
echo  [提示] 未找到 APK 文件，跳过安装。
echo  请先构建：gradlew assembleDebug
:install_done

echo.
echo  ============================================
echo    模拟器已就绪，可以开始验收了！
echo  ============================================
echo.

:end
pause
