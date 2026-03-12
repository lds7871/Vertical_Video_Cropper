@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM 创建便携包脚本 - 简化版本

set PROJECT_DIR=%~dp0
set PACKAGE_NAME=video-crop-tool-portable
set PACKAGE_DIR=%PROJECT_DIR%%PACKAGE_NAME%

echo.
echo ========================================
echo   MP4视频裁切工具 - 便携包打包脚本
echo ========================================
echo.

REM 检查是否已编译
if not exist "%PROJECT_DIR%target\video-crop-tool-1.0.0-jar-with-dependencies.jar" (
    echo 错误: JAR 文件不存在，需要先编译项目！
    echo.
    echo 请运行: build_and_run.bat 或 mvn clean package
    echo.
    pause
    exit /b 1
)

echo 开始创建便携包...
echo.

REM 删除旧的打包目录
if exist "%PACKAGE_DIR%" (
    echo 删除旧打包目录...
    rmdir /s /q "%PACKAGE_DIR%"
)

REM 创建新目录结构
echo 创建目录结构...
mkdir "%PACKAGE_DIR%"
mkdir "%PACKAGE_DIR%\target"
mkdir "%PACKAGE_DIR%\VideoIn"
mkdir "%PACKAGE_DIR%\VideoOut"

REM 复制必要文件
echo 复制文件...
copy "%PROJECT_DIR%target\video-crop-tool-1.0.0-jar-with-dependencies.jar" "%PACKAGE_DIR%\target\" /y
copy "%PROJECT_DIR%A1_portable-setup-jre.bat" "%PACKAGE_DIR%\" /y
copy "%PROJECT_DIR%A1_portable-setup-ffmpeg.bat" "%PACKAGE_DIR%\" /y
copy "%PROJECT_DIR%A1_portable-run.bat" "%PACKAGE_DIR%\" /y

echo.
echo 便携包已生成！
echo.
echo 位置: %PACKAGE_DIR%
echo.
echo 文件夹包含:
echo   - target\           编译好的 JAR 文件
echo   - jre\              Java 运行环境（首次配置时自动下载）
echo   - VideoIn\          输入视频文件夹
echo   - VideoOut\         输出视频文件夹
echo   - portable-setup.bat 配置脚本（首次使用）
echo   - portable-run.bat   运行脚本（日常使用）
echo.
echo 现在可以:
echo   1. 直接分发 %PACKAGE_NAME% 文件夹
echo   2. 或压缩为 ZIP 后分发
echo.

pause
