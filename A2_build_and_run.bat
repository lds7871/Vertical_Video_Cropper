@echo off
chcp 65001

REM MP4视频裁切工具 - 编译和运行脚本

setlocal enabledelayedexpansion

REM 设置JDK和Maven路径
set JAVA_HOME=D:\AAAHJPZ\JDK\jdk-25_windows-x64_bin\jdk-25.0.2
set M2_HOME=D:\AAAHJPZ\Maven\apache-maven-3.9.12-bin\apache-maven-3.9.12
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

echo.
echo ========================================
echo   MP4视频中心9:16比例裁切工具
echo ========================================
echo.
echo JDK路径: %JAVA_HOME%
echo Maven路径: %M2_HOME%
echo.

REM 验证JDK和Maven
echo 验证环境...
java -version 2>&1 | findstr /R "version" >nul
if errorlevel 1 (
    echo 错误: 找不到Java，请检查JAVA_HOME配置
    pause
    exit /b 1
)

mvn -v 2>&1 | findstr /R "Apache Maven" >nul
if errorlevel 1 (
    echo 错误: 找不到Maven，请检查M2_HOME配置
    pause
    exit /b 1
)

echo JDK和Maven验证成功
echo.

REM 清理并编译
echo 正在清理和编译项目...
call mvn clean compile -DskipTests

if errorlevel 1 (
    echo.
    echo 错误: 编译失败!
    pause
    exit /b 1
)

echo.
echo 编译成功！
echo 正在打包项目...
echo.

REM 打包项目
call mvn package -DskipTests

if errorlevel 1 (
    echo.
    echo 错误: 打包失败!
    pause
    exit /b 1
)

echo.
echo ========================================
echo   编译和打包完成！
echo ========================================
echo.
echo 打包后的JAR文件:
echo   target\video-crop-tool-1.0.0-jar-with-dependencies.jar
echo.
echo 按任意键运行程序...
pause

REM 运行程序
echo.
echo 启动程序...
echo.
java -jar target\video-crop-tool-1.0.0-jar-with-dependencies.jar

pause
