@echo off
chcp 65001 >nul
REM 此脚本用于运行图像处理程序

setlocal enabledelayedexpansion

echo.
echo ========================================
echo 图像宽荧幕转换工具 - JDK25版本
echo ========================================
echo.

REM 检查JAR文件是否存在
if not exist "target\image-aspect-ratio-processor-1.0.0.jar" (
    echo ✗ 错误: JAR文件不存在
    echo.
    echo 请先运行 build.cmd 进行构建
    echo.
    pause
    exit /b 1
)

echo 检查输入目录...
if not exist "ImgIn_133" (
    echo ✗ 错误: 输入目录 ImgIn_133 不存在
    echo.
    echo 请创建 ImgIn_133 目录并放入图像文件
    echo.
    pause
    exit /b 1
)

echo ✓ 准备就绪
echo.
echo 开始处理图像...
echo.

REM 运行程序
java -cp target/image-aspect-ratio-processor-1.0.0.jar com.image.processor.ImageAspectRatioProcessor

echo.
echo ========================================
echo 处理完成！
echo =======================================.
echo.
echo 输出目录: ImgOut_133
echo.
echo 验证输出DPI...
echo.

REM 检查输出DPI
for %%F in (ImgOut_133\*.jpg) do (
    echo 检查: %%~nF
    java -cp target/image-aspect-ratio-processor-1.0.0.jar com.image.processor.CheckImageDPIv2 "%%F" 2>nul
    echo.
)

pause
