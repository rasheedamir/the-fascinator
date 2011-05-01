@echo off

REM this script starts a fascinator harvest using maven
REM only usable when installed in development mode

REM get fascinator home dir
for %%F in ("%0") do set TF_HOME=%%~dpF

set SAMPLE_DIR=%TF_HOME%core\src\test\resources
if "%1" == "" goto usage
set JSON_FILE=%1

REM set environment
call "%TF_HOME%tf_env.bat"

set Cmd=tasklist /fi "windowtitle eq The Fascinator - mvn*" /fo csv /nh
for /f "tokens=1*" %%i in ('%Cmd% ^| find "cmd.exe"') do goto harvest
echo Please start The Fascinator before harvesting.
goto end

:harvest
if exist "%JSON_FILE%" (set BASE_FILE=%JSON_FILE%) else (set BASE_FILE=%SAMPLE_DIR%\%JSON_FILE%)
if not exist "%BASE_FILE%.json" copy "%BASE_FILE%.json.sample" "%BASE_FILE%.json"
if not exist "%BASE_FILE%.py" copy "%BASE_FILE%.py.sample" "%BASE_FILE%.py"
pushd "%TF_HOME%\core"
call mvn -P dev -DjsonFile="%BASE_FILE%.json" exec:java
popd
goto end

:usage
echo Usage: %0 jsonFile
echo Where jsonFile is a JSON configuration file
echo If jsonFile is not an absolute path, the file is assumed to be in:
echo     %SAMPLE_DIR%
echo Available files:
for /f "tokens=1,2* delims=." %%i in ('dir /b "%SAMPLE_DIR%\*.json.sample"') do @echo     %%~ni

:end
