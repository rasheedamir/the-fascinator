#!/bin/bash
#
# this script sets the environment for other fascinator scripts
#
# work out proxy info
TMP=${http_proxy#*//}
HOST=${TMP%:*}
TMP=${http_proxy##*:}
PORT=${TMP%/}

# set environment
OS=`uname`
if [ "$OS" == "Darwin" ] ; then
	export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home
else
	# assume ubuntu with sun-java6-jdk installed
	export JAVA_HOME=/usr/lib/jvm/java-6-sun
fi
if [ "$FASCINATOR_HOME" == "" ]; then
	export FASCINATOR_HOME=$HOME/.fascinator
fi
if [ "$SOLR_BASE_DIR" == "" ]; then
	export SOLR_BASE_DIR=$FASCINATOR_HOME
fi
export MAVEN_OPTS="-XX:MaxPermSize=256m -Xmx512m -Dhttp.proxyHost=$HOST -Dhttp.proxyPort=$PORT -Dhttp.nonProxyHosts=localhost -Dfascinator.home=$FASCINATOR_HOME -Dsolr.base.dir=$SOLR_BASE_DIR $MAVEN_OPTS"
