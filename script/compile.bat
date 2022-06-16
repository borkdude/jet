@echo off

Rem set GRAALVM_HOME=C:\Users\IEUser\Downloads\graalvm\graalvm-ce-19.2.1
Rem set PATH=%PATH%;C:\Users\IEUser\bin

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b
)
set JAVA_HOME=%GRAALVM_HOME%\bin
set PATH=%GRAALVM_HOME%\bin;%PATH%

set /P JET_VERSION=< resources\JET_VERSION
echo Building jet %JET_VERSION%

java -version
call lein with-profiles +native-image do clean, uberjar
if %errorlevel% neq 0 exit /b %errorlevel%

call %GRAALVM_HOME%\bin\gu.cmd install native-image

Rem the --no-server option is not supported in GraalVM Windows.
call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "target/jet-%JET_VERSION%-standalone.jar" ^
  "-H:+ReportExceptionStackTraces" ^
  "-H:Log=registerResource:" ^
  "--no-fallback" ^
  "--verbose" ^
  "-J-Xmx3g"

if %errorlevel% neq 0 exit /b %errorlevel%

echo Creating zip archive
jar -cMf jet-%JET_VERSION%-windows-amd64.zip jet.exe
