@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM MP4视频裁切工具便携版 - 运行脚本
REM 使用项目内嵌的 JRE 运行，无需系统安装 Java

set JRE_TEMP=%~dp0jre-temp
set JAR_FILE=%~dp0target\video-crop-tool-1.0.0-jar-with-dependencies.jar

echo.
echo ========================================
echo   MP4视频中心9:16比例裁切工具（便携版）
echo ========================================
echo.

REM 查找 JRE bin 目录（可能在 jre-temp 或其子目录中）
set JRE_BIN=
if exist "%JRE_TEMP%\bin\java.exe" (
    set JRE_BIN=%JRE_TEMP%\bin
) else (
    for /d %%X in ("%JRE_TEMP%\*") do (
        if exist "%%X\bin\java.exe" (
            set JRE_BIN=%%X\bin
            goto :found_jre
        )
    )
)

:found_jre
REM 检查 JRE 是否存在
if not exist "%JRE_BIN%\java.exe" (
    echo 错误: JRE 未找到！
    echo.
    echo 请先运行: A1_portable-setup.bat
    echo.
    pause
    exit /b 1
)

REM 检查 JAR 文件是否存在
if not exist "%JAR_FILE%" (
    echo 错误: JAR 文件未找到！
    echo.
    echo 请确保以下文件存在:
    echo   %JAR_FILE%
    echo.
    pause
    exit /b 1
)

REM 显示 Java 版本
echo 使用 JRE: %JRE_BIN%
"%JRE_BIN%\java.exe" -version
echo.

REM 查找并运行 FFmpeg（优先查找 ffmpeg-download\bin, ffmpeg-full_build\bin）
set FFMPEG_BIN=
if exist "%~dp0ffmpeg-download\bin\ffmpeg.exe" (
    set FFMPEG_BIN=%~dp0ffmpeg-download\bin
) else if exist "%~dp0ffmpeg-full_build\bin\ffmpeg.exe" (
    set FFMPEG_BIN=%~dp0ffmpeg-full_build\bin
) else (
    for /d %%Y in ("%~dp0*") do (
        if exist "%%Y\bin\ffmpeg.exe" (
            set FFMPEG_BIN=%%Y\bin
            goto :found_ffmpeg
        )
    )
)

:found_ffmpeg
if defined FFMPEG_BIN (
    echo 使用 FFmpeg: %FFMPEG_BIN%
    REM 临时将 FFmpeg 的 bin 目录加入当前会话的 PATH
    set "PATH=%FFMPEG_BIN%;%PATH%"
    echo 已将 FFmpeg 临时加入 PATH
    @REM "%FFMPEG_BIN%\ffmpeg.exe" -version
) else (
    echo 警告: 未找到 FFmpeg，部分功能可能不可用
)

REM 运行程序
"%JRE_BIN%\java.exe" -jar "%JAR_FILE%"

echo.
echo 程序已退出。
pause
