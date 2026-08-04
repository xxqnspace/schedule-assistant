@rem Gradle 启动脚本（Windows）。
@rem 说明：gradle-wrapper.jar 默认不包含在仓库中（二进制文件），首次用 Android Studio
@rem 打开本项目会自动生成；或在已安装 Gradle 的环境下执行 `gradle wrapper` 生成。
@echo off
set "APP_HOME=%~dp0"
set "WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"

if exist "%WRAPPER_JAR%" (
  if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -jar "%WRAPPER_JAR%" %*
  ) else (
    java -jar "%WRAPPER_JAR%" %*
  )
  goto :eof
)

where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  echo 未检测到 gradle-wrapper.jar，使用系统已安装的 gradle 构建。
  gradle %*
  goto :eof
)

echo 未找到 gradle-wrapper.jar，也没有安装 gradle。>&2
echo 请用 Android Studio 打开本项目（会自动生成 wrapper），或先安装 Gradle 后执行 `gradle wrapper`。>&2
exit /b 1
