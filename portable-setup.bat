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

set JRE_DIR=%~dp0jre
set JRE_BIN=%JRE_DIR%\bin

if exist "%JRE_BIN%\java.exe" (
    echo ✓ JRE 已安装，位置: %JRE_DIR%
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
curl -L -o "%~dp0jre-download.zip" "https://release-assets.githubusercontent.com/github-production-release-asset/901810329/8516459b-4c0e-49df-a7b5-d060f4cb3e8e?sp=r&sv=2018-11-09&sr=b&spr=https&se=2026-03-12T05%3A02%3A08Z&rscd=attachment%3B+filename%3DOpenJDK25U-jre_x64_windows_hotspot_25.0.2_10.zip&rsct=application%2Foctet-stream&skoid=96c2d410-5711-43a1-aedd-ab1947aa7ab0&sktid=398a6654-997b-47e9-b12b-9515b896b4de&skt=2026-03-12T04%3A02%3A03Z&ske=2026-03-12T05%3A02%3A08Z&sks=b&skv=2018-11-09&sig=73DDCzfQV1Wt8Djf8KQNMwdY2WIE2k%2Br13yTyBpIZds%3D&jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmVsZWFzZS1hc3NldHMuZ2l0aHVidXNlcmNvbnRlbnQuY29tIiwia2V5Ijoia2V5MSIsImV4cCI6MTc3MzI5MDcwNywibmJmIjoxNzczMjg4OTA3LCJwYXRoIjoicmVsZWFzZWFzc2V0cHJvZHVjdGlvbi5ibG9iLmNvcmUud2luZG93cy5uZXQifQ.TE8S2A20Avkn34ctvqDVE8CQiWwveobl4iDR-C6tS4o&response-content-disposition=attachment%3B%20filename%3DOpenJDK25U-jre_x64_windows_hotspot_25.0.2_10.zip&response-content-type=application%2Foctet-stream"

if errorlevel 1 (
    echo.
    echo × 下载失败！请检查网络连接
    pause
    exit /b 1
)

echo 下载完成，正在解压...
REM 等待文件完全释放
timeout /t 2 /nobreak >nul 2>&1

REM 解压到临时目录，然后移动到 jre
powershell -NoProfile -Command "Expand-Archive -Path '%~dp0jre-download.zip' -DestinationPath '%~dp0jre-temp' -Force -ErrorAction Stop"
if errorlevel 1 (
    echo.
    echo × 解压失败！
    del /q "%~dp0jre-download.zip" >nul 2>&1
    rmdir /s /q "%~dp0jre-temp" >nul 2>&1
    pause
    exit /b 1
)

echo 解压成功，正在整理文件...
timeout /t 1 /nobreak >nul 2>&1

REM 处理解压后的目录结构
REM 可能是: jdk*/, jre*/, 或直接的 bin/lib 结构
echo 整理文件结构...

REM 检查是否已有正确的结构
if exist "%~dp0jre-temp\bin\java.exe" (
    echo 检测到标准 JRE 结构
    if not exist "%JRE_DIR%" mkdir "%JRE_DIR%"
    move "%~dp0jre-temp\*" "%JRE_DIR%\" >nul 2>&1
    if errorlevel 1 (
        powershell -NoProfile -Command "Copy-Item '%~dp0jre-temp\*' '%JRE_DIR%\' -Recurse -Force -ErrorAction Stop"
    )
    goto :cleanup
)

REM 检查 jdk* 目录
for /d %%X in ("%~dp0jre-temp\jdk*") do (
    echo 检测到 JDK 目录: %%~nX
    if not exist "%JRE_DIR%" mkdir "%JRE_DIR%"
    move "%%X\*" "%JRE_DIR%\" >nul 2>&1
    if errorlevel 1 (
        powershell -NoProfile -Command "Copy-Item '%%X\*' '%JRE_DIR%\' -Recurse -Force -ErrorAction Stop"
    )
    goto :cleanup
)

REM 检查 jre* 目录
for /d %%X in ("%~dp0jre-temp\jre*") do (
    echo 检测到 JRE 目录: %%~nX
    if not exist "%JRE_DIR%" mkdir "%JRE_DIR%"
    move "%%X\*" "%JRE_DIR%\" >nul 2>&1
    if errorlevel 1 (
        powershell -NoProfile -Command "Copy-Item '%%X\*' '%JRE_DIR%\' -Recurse -Force -ErrorAction Stop"
    )
    goto :cleanup
)

REM 如果没找到标准结构，尝试直接移动所有内容
echo 使用标准结构复制...
if not exist "%JRE_DIR%" mkdir "%JRE_DIR%"
powershell -NoProfile -Command "Copy-Item '%~dp0jre-temp\*' '%JRE_DIR%\' -Recurse -Force -ErrorAction SilentlyContinue"

:cleanup
REM 清理临时文件
echo 清理临时文件...
timeout /t 1 /nobreak >nul 2>&1
del /q "%~dp0jre-download.zip" >nul 2>&1
rmdir /s /q "%~dp0jre-temp" >nul 2>&1

echo 安装完成！
exit /b 0

:downloadJREPowerShell
REM PowerShell 下载和解压方式
powershell -NoProfile -Command ^
    "$ErrorActionPreference = 'Stop'; " ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "$url = 'https://release-assets.githubusercontent.com/github-production-release-asset/901810329/83d18b47-94b4-4f8f-bd6b-5de9bcefc1ed?sp=r&sv=2018-11-09&sr=b&spr=https&se=2026-03-12T04:36:03Z&rscd=attachment;+filename=OpenJDK25U-jdk_x64_windows_hotspot_25.0.2_10.zip&rsct=application/octet-stream&skoid=96c2d410-5711-43a1-aedd-ab1947aa7ab0&sktid=398a6654-997b-47e9-b12b-9515b896b4de&skt=2026-03-12T03:35:52Z&ske=2026-03-12T04:36:03Z&sks=b&skv=2018-11-09&sig=fHPRqTPz7/D0WRsuffeZX901RtPWjWVZ6y0ljsgJ0Wc=&jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmVsZWFzZS1hc3NldHMuZ2l0aHVidXNlcmNvbnRlbnQuY29tIiwia2V5Ijoia2V5MSIsImV4cCI6MTc3MzI5MDE5MCwibmJmIjoxNzczMjg2NTkwLCJwYXRoIjoicmVsZWFzZWFzc2V0cHJvZHVjdGlvbi5ibG9iLmNvcmUud2luZG93cy5uZXQifQ.jV5mnDYDcB1ecEAk1prEjxybK5DK6nitvt2j-95Xr3w&response-content-disposition=attachment;+filename=OpenJDK25U-jdk_x64_windows_hotspot_25.0.2_10.zip&response-content-type=application/octet-stream'; " ^
    "$zip = '%~dp0jre-download.zip'; " ^
    "$jreDir = '%~dp0jre'; " ^
    "Write-Host '正在下载 JRE (约 200MB，请稍候)...'; " ^
    "try { " ^
        "Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing; " ^
        "Write-Host '下载完成，正在解压...'; " ^
        "if (Test-Path '$jreDir') { Remove-Item '$jreDir' -Recurse -Force }; " ^
        "Expand-Archive -Path $zip -DestinationPath '$jreDir-temp' -Force; " ^
        "Get-ChildItem '$jreDir-temp' -Filter 'jdk*' -Directory | ForEach-Object { Move-Item $_.FullName\* $jreDir -Force }; " ^
        "Remove-Item '$jreDir-temp' -Recurse -Force; " ^
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
    echo.
    echo 现在可以运行: portable-run.bat
    pause
    exit /b 0
) else (
    echo.
    echo × 安装失败，请按如下步骤手动安装：
    echo.
    echo 1. 访问: https://adoptium.net/temurin/releases/?version=25
    echo 2. 下载 "JRE x64 Windows" 版本
    echo 3. 解压到项目根目录的 jre 文件夹
    echo 4. 运行 portable-run.bat
    echo.
    pause
    exit /b 1
)
