@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM MP4视频裁切工具便携包 - FFmpeg 配置脚本
REM 此脚本会自动下载 FFmpeg，无需用户手动安装

echo.
echo ========================================
echo   MP4视频裁切工具 - FFmpeg 配置脚本
echo ========================================
echo.

set FFMPEG_DIR=%~dp0ffmpeg-full_build
set FFMPEG_BIN=%FFMPEG_DIR%\bin

if exist "%FFMPEG_BIN%\ffmpeg.exe" (
    echo ✓ FFmpeg 已安装，位置: %FFMPEG_DIR%
    echo.
    echo 可以运行: portable-run.bat
    pause
    exit /b 0
)

echo FFmpeg 未找到，开始自动下载和安装...
echo.

REM 检查是否有 curl
where curl >nul 2>&1
if errorlevel 1 (
    echo 使用 PowerShell 下载 FFmpeg...
    call :downloadFFmpegPowerShell
    goto :checkDownload
)

echo 使用 curl 下载 FFmpeg...
call :downloadFFmpegCurl
goto :checkDownload

:downloadFFmpegCurl
REM 下载 FFmpeg
echo 正在下载 FFmpeg (约 200MB，请稍候)...
curl -L -o "%~dp0ffmpeg-download.7z" "https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-2026-03-09-git-9b7439c31b-full_build.7z"

if errorlevel 1 (
    echo.
    echo × 下载失败！请检查网络连接
    pause
    exit /b 1
)

echo 下载完成！
timeout /t 1 /nobreak >nul 2>&1

goto :checkDownload

:downloadFFmpegPowerShell
REM PowerShell 下载方式
powershell -NoProfile -Command ^
    "$ErrorActionPreference = 'Stop'; " ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "$url = 'https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-2026-03-09-git-9b7439c31b-full_build.7z'; " ^
    "$file = '%~dp0ffmpeg-download.7z'; " ^
    "Write-Host '正在下载 FFmpeg (约 200MB，请稍候)...'; " ^
    "try { " ^
        "Invoke-WebRequest -Uri $url -OutFile $file -UseBasicParsing; " ^
        "Write-Host '下载完成！'; " ^
    "} " ^
    "catch { " ^
        "Write-Host '下载失败！请检查网络连接'; " ^
        "if (Test-Path $file) { Remove-Item $file }; " ^
        "exit 1 " ^
    "}"

if errorlevel 1 exit /b 1

goto :checkDownload

:checkDownload
if exist "%~dp0ffmpeg-download.7z" (
    echo.
    echo ✓ FFmpeg 下载成功！
    echo 文件位置: %~dp0ffmpeg-download.7z
    echo.
    echo 请按以下步骤手动解压:
    echo.
    echo 1. 使用 7-Zip 打开 ffmpeg-download.7z
    echo    （如未安装，访问: https://www.7-zip.org/download.html）
    echo.
    echo 2. 解压到项目根目录
    echo.
    echo 3. 将解压出的文件夹重命名为: ffmpeg-full_build
    echo.
    echo 4. 确保目录结构为: ffmpeg-full_build\bin\ffmpeg.exe
    echo.
    echo 完成后，可以运行: portable-run.bat
    echo.
    pause
    exit /b 0
) else (
    echo.
    echo × 下载失败！
    echo.
    echo 请检查:
    echo   - 网络连接是否正常
    echo   - 是否有足够的磁盘空间
    echo.
    pause
    exit /b 1
)
