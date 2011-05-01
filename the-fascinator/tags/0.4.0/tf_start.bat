echo off

REM This script starts SOLR and The Fascinator's portal
REM Make sure you check to make sure tf_env.bat reflects
REM your config.


call tf_env.bat

start /D%TF_HOME%\solr java -jar start.jar

start /D%TF_HOME%\code\portal mvn -Dhttp.nonProxyHosts=localhost -P test -Djetty.port=9997 jetty:run

cd %TF_HOME%\code
