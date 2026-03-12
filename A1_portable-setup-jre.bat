@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM MP4视频裁切工具便携包 - 首次配置脚本
REM 此脚本会自动下载 JRE，无需用户手动安装

echo.
echo ========================================
echo   MP4视频裁切工具 - 便携包首次配置
echo ========================================
echo.

set JRE_TEMP=%~dp0jre-temp
set JRE_BIN=%JRE_TEMP%\bin

if exist "%JRE_BIN%\java.exe" (
    echo ✓ JRE 已安装，位置: %JRE_TEMP%
    echo.
    echo 可以运行: portable-run.bat
    pause
    exit /b 0
)

echo JRE 未找到，开始自动下载和安装...
echo.

REM 检查是否有 curl 或 powershell
where curl >nul 2>&1
if errorlevel 1 (
    echo 使用 PowerShell 下载 JRE...
    call :downloadJREPowerShell
    goto :checkDownload
)

echo 使用 curl 下载 JRE...
call :downloadJRECurl
goto :checkDownload

:downloadJRECurl
REM 下载 JRE 25（OpenJDK 25）
echo 正在下载 JRE 25 (约 200MB，请稍候)...
curl -L -o "%~dp0jre-download.zip" "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/25/jre/x64/windows/OpenJDK25U-jre_x64_windows_hotspot_25.0.2_10.zip"

if errorlevel 1 (
    echo.
    echo × 下载失败！请检查网络连接
    pause
    exit /b 1
)

echo 下载完成，正在解压...
REM 等待文件完全释放
timeout /t 2 /nobreak >nul 2>&1

REM 解压到 jre-temp 目录
if exist "%JRE_TEMP%" rmdir /s /q "%JRE_TEMP%" >nul 2>&1
powershell -NoProfile -Command "Expand-Archive -Path '%~dp0jre-download.zip' -DestinationPath '%~dp0jre-temp' -Force -ErrorAction Stop"
if errorlevel 1 (
    echo.
    echo × 解压失败！
    del /q "%~dp0jre-download.zip" >nul 2>&1
    pause
    exit /b 1
)

echo 解压成功，正在整理文件...
timeout /t 1 /nobreak >nul 2>&1

REM 处理解压后的目录结构，展平到 jre-temp
echo 整理文件结构...

REM 检查是否已有正确的结构（bin 直接在 jre-temp）
if exist "%JRE_TEMP%\bin\java.exe" (
    echo ✓ 检测到标准 JRE 结构
    goto :cleanup
)

REM 检查并展平内嵌的目录（如 OpenJDK25U-jre_x64_windows...、jdk*、jre*）
for /d %%X in ("%JRE_TEMP%\*") do (
    if exist "%%X\bin\java.exe" (
        echo 检测到内嵌 JRE 目录: %%~nX，正在展平...
        powershell -NoProfile -Command "Move-Item '%%X\*' '%JRE_TEMP%\' -Force -ErrorAction Stop"
        if errorlevel 1 (
            echo 展平失败，尝试复制方式...
            powershell -NoProfile -Command "Copy-Item '%%X\*' '%JRE_TEMP%\' -Recurse -Force -ErrorAction Stop"
            rmdir /s /q "%%X" >nul 2>&1
        )
        goto :cleanup
    )
)

echo × 未找到有效的 JRE 结构
goto :cleanup

:cleanup
REM 清理 zip 文件，保留 jre-temp
echo 清理下载文件...
del /q "%~dp0jre-download.zip" >nul 2>&1
echo ✓ 保留解压文件: jre-temp

echo 安装完成！
exit /b 0

:downloadJREPowerShell
REM PowerShell 下载和解压方式
powershell -NoProfile -Command ^
    "$ErrorActionPreference = 'Stop'; " ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "$url = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/25/jre/x64/windows/OpenJDK25U-jre_x64_windows_hotspot_25.0.2_10.zip'; " ^
    "$zip = '%~dp0jre-download.zip'; " ^
    "$jreTemp = '%~dp0jre-temp'; " ^
    "Write-Host '正在下载 JRE (约 200MB，请稍候)...'; " ^
    "try { " ^
        "Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing; " ^
        "Write-Host '下载完成，正在解压...'; " ^
        "if (Test-Path '$jreTemp') { Remove-Item '$jreTemp' -Recurse -Force }; " ^
        "Expand-Archive -Path $zip -DestinationPath '$jreTemp' -Force; " ^
        "Write-Host '整理文件结构...'; " ^
        "Get-ChildItem '$jreTemp' -Directory | ForEach-Object { if (Test-Path (Join-Path $_.FullName 'bin\\java.exe')) { Move-Item (Join-Path $_.FullName '*') $jreTemp -Force -ErrorAction SilentlyContinue; Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue } }; " ^
        "Remove-Item $zip; " ^
        "Write-Host '安装完成！'; " ^
    "} " ^
    "catch { " ^
        "Write-Host '下载或解压失败！请检查网络连接'; " ^
        "if (Test-Path $zip) { Remove-Item $zip }; " ^
        "exit 1 " ^
    "}"

exit /b %ERRORLEVEL%

:checkDownload
if exist "%JRE_BIN%\java.exe" (
    echo.
    echo ✓ JRE 安装成功！
    echo 位置: %JRE_TEMP%
    echo.
    echo 现在可以继续下一步
    pause
    exit /b 0
) else (
    echo.
    echo × 安装失败！未找到 JRE
    echo.
    echo 请检查:
    echo   - 网络连接是否正常
    echo   - %JRE_TEMP% 文件夹是否存在
    echo   - 是否有足够的磁盘空间
    echo.
    pause
    exit /b 1
)
