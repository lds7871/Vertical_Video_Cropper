@echo off
chcp 65001 >nul
REM 此脚本用于构建项目并生成JAR文件

echo.
echo ========================================
echo 开始构建项目...
echo ========================================
echo.

REM 执行Maven清理和打包
mvn clean package -q

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✓ 构建成功！
    echo ========================================
    echo.
    echo JAR文件位置:
    echo   - target\image-aspect-ratio-processor-1.0.0.jar
    echo   - target\image-aspect-ratio-processor-1.0.0-jar-with-dependencies.jar
    echo.
    echo 下一步: 运行 run.cmd 执行程序
    echo.
    pause
) else (
    echo.
    echo ========================================
    echo ✗ 构建失败！
    echo ========================================
    echo.
    pause
)
