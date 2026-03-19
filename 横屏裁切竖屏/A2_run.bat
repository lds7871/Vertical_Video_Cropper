@echo off
chcp 65001
setlocal enabledelayedexpansion

REM 快速运行脚本 - 假设项目已编译

set JAVA_HOME=D:\AAAHJPZ\JDK\jdk-25_windows-x64_bin\jdk-25.0.2
set M2_HOME=D:\AAAHJPZ\Maven\apache-maven-3.9.12-bin\apache-maven-3.9.12

echo.
echo ========================================
echo   MP4视频中心9:16比例裁切工具
echo ========================================
echo.

if not exist "target\video-crop-tool-1.0.0-jar-with-dependencies.jar" (
    echo 错误: JAR文件未找到!
    echo.
    echo 请先运行: build_and_run.bat
    pause
    exit /b 1
)

echo 启动程序...
echo.

"%JAVA_HOME%\bin\java" -jar target\video-crop-tool-1.0.0-jar-with-dependencies.jar

echo.
echo 程序已退出。
pause
