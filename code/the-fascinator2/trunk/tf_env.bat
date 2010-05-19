@echo off

REM this script sets the environment for other fascinator scripts

REM find java installation
if not defined JAVA_HOME (
  echo Detecting Java...
  set KeyName=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit
  set Cmd=reg query "%KeyName%" /s
  for /f "tokens=2*" %%i in ('%Cmd% ^| findstr "JavaHome"') do set JAVA_HOME=%%j
)

REM find proxy server
set KeyName=HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Internet Settings
set Cmd=reg query "%KeyName%" /s
for /f "tokens=2*" %%i in ('%Cmd% ^| findstr "ProxyServer" 2^> NUL') do set http_proxy=%%j
for /f "tokens=1,2 delims=:" %%i in ("%http_proxy%") do (
  set PROXY_HOST=%%i
  set PROXY_PORT=%%j
)

REM set environment
if not defined FASCINATOR_HOME set FASCINATOR_HOME=%USERPROFILE%\.fascinator
if not defined SOLR_BASE_DIR set SOLR_BASE_DIR=%FASCINATOR_HOME%
set MAVEN_OPTS=-XX:MaxPermSize=128m -Xmx512m -Dhttp.proxyHost=%PROXY_HOST% -Dhttp.proxyPort=%PROXY_PORT% -Dhttp.nonProxyHosts=localhost -Dfascinator.home="%FASCINATOR_HOME%" -Dsolr.base.dir="%SOLR_BASE_DIR%" %MAVEN_OPTS%
